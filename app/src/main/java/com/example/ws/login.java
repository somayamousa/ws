package com.example.ws;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class login extends AppCompatActivity {
    private EditText editTextUsername, editTextPassword;
    private TextView textViewlog;

    private static final String LOGIN_URL = "http://10.0.2.2/WorkSync/loginapi.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط العناصر من الواجهة
        editTextUsername = findViewById(R.id.inputEmail);
        editTextPassword = findViewById(R.id.inputPassword);
        textViewlog = findViewById(R.id.btnLogin);


        textViewlog .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userLogin();
            }
        });
    }

    private void userLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(login.this, "يرجى إدخال اسم المستخدم وكلمة المرور", Toast.LENGTH_SHORT).show();
            return;
        }




        JSONObject requestData = new JSONObject();
        try {
            requestData.put("username", username);
            requestData.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // إرسال الطلب باستخدام Volley
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, LOGIN_URL, requestData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            String status = response.getString("status");
                            if (status.equals("success")) {
                                JSONObject user = response.getJSONObject("user");
                                String role = user.getString("role");

                                // توجيه المستخدم حسب الدور
                                if (role.equals("Admin")) {
//                                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                                    Log.d("Res","Admin");
                                } else if (role.equals("Employee")) {
//                                    startActivity(new Intent(LoginActivity.this, EmployeeDashboardActivity.class));
                                    Log.d("Res","Employee");
                                }

                                Toast.makeText(login.this, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show();
                            } else {
                                String message = response.getString("message");
                                Toast.makeText(login.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(login.this, "خطأ في تحليل البيانات", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Toast.makeText(login.this, "خطأ في الاتصال بالخادم", Toast.LENGTH_SHORT).show();
                        Log.e("LoginError", error.toString());
                    }
                });

        // إضافة الطلب إلى قائمة الانتظار
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
