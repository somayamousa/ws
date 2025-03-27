package com.example.ws;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class attendance extends AppCompatActivity {
    private static final String TAG = "MyApp";
    private Button startButton, stopButton;
    private TextView timerText;
    private long startTime = 0;
    private boolean isCounting = false;
    private Handler handler = new Handler();
    private RequestQueue requestQueue;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        startButton = findViewById(R.id.btn_check_in);
        stopButton = findViewById(R.id.btn_check_out);
        timerText = findViewById(R.id.tv_timer);

        requestQueue = Volley.newRequestQueue(this);

        startButton.setOnClickListener(v -> startButtonEvent());
        stopButton.setOnClickListener(v -> stopTimer());
    }

    private void startButtonEvent() {
        if (checkBiometricSupport()) {
            setupBiometricPrompt();
            authenticateUser();
        } else {
            Toast.makeText(this, "Biometric authentication not supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        if (!isCounting) {
            startTime = System.currentTimeMillis();
            isCounting = true;
            handler.postDelayed(updateTimerRunnable, 1000);
        }
    }

    private void stopTimer() {
        if (isCounting) {
            long endTime = System.currentTimeMillis();
            float hoursWorked = (endTime - startTime) / 3600000.0f;

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            sendWorkHoursToServer(currentDate, hoursWorked);

            isCounting = false;
            handler.removeCallbacks(updateTimerRunnable);
            timerText.setText("00:00:00");
        }
    }

    private boolean checkBiometricSupport() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Toast.makeText(this, "Biometric authentication is available", Toast.LENGTH_SHORT).show();
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "No biometric hardware available", Toast.LENGTH_LONG).show();
                return false;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "Biometric hardware unavailable", Toast.LENGTH_LONG).show();
                return false;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No biometric data enrolled", Toast.LENGTH_LONG).show();
                promptUserToEnrollBiometrics();
                return false;
            default:
                return false;
        }
    }

    private void promptUserToEnrollBiometrics() {
        Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
        startActivity(enrollIntent);
    }

    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(attendance.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                String userId = "user123"; // Replace with actual user ID from your app
                String capturedBiometricTemplate = "capturedBiometricData"; // Replace with actual biometric data captured
                // sendBiometricDataToServer(userId, capturedBiometricTemplate);
                startTimer(); // Start the timer after successful authentication
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(attendance.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(attendance.this, "Error : " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Use your fingerprint or face to unlock")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void authenticateUser() {
        biometricPrompt.authenticate(promptInfo);
    }

    private void sendWorkHoursToServer(String date, float hours) {
        String url = "http://10.0.2.2/worksync/work_hours.php"; // Replace with the actual API endpoint

        Map<String, String> params = new HashMap<>();
        params.put("date", date);
        params.put("hours", String.valueOf(hours));

        JSONObject jsonRequest = new JSONObject(params);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            Toast.makeText(attendance.this, "Data sent successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(attendance.this, "Failed to send data: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        Toast.makeText(attendance.this, "Error with the data received", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error: " + error.getMessage());
                    Toast.makeText(attendance.this, "Failed to send: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void sendBiometricDataToServer(String userId, String biometricTemplate) {
        String secretKey = "yourSecretKeyHere";  // Use a secure key
        try {
            String encryptedTemplate = EncryptionUtils.encrypt(biometricTemplate, secretKey);

            String url = "http://10.0.2.2/worksync/upload_biometric_data.php"; // Replace with actual API endpoint

            Map<String, String> params = new HashMap<>();
            params.put("user_id", userId);
            params.put("biometric_template", encryptedTemplate);

            JSONObject jsonRequest = new JSONObject(params);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest,
                    response -> {
                        try {
                            boolean success = response.getBoolean("success");
                            String message = response.getString("message");

                            if (success) {
                                Toast.makeText(attendance.this, "Biometric data uploaded successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(attendance.this, "Failed to upload biometric data: " + message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                            Toast.makeText(attendance.this, "Error with the data received", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error: " + error.getMessage());
                        Toast.makeText(attendance.this, "Failed to send: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(attendance.this, "Error encrypting biometric data", Toast.LENGTH_SHORT).show();
        }
    }

    private final Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCounting) {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) ((elapsedTime / (1000 * 60)) % 60);
                int hours = (int) (elapsedTime / (1000 * 60 * 60));
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };
}
