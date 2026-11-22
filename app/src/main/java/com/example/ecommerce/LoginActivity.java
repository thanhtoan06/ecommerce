package com.example.ecommerce;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText edtIdentity;
    private EditText edtPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private boolean passwordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtIdentity = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBarLogin);

        findViewById(R.id.btnBackToHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btnRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.tvForgotPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        ImageView togglePassword = findViewById(R.id.btnTogglePassword);
        togglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> login());
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        edtPassword.setTransformationMethod(passwordVisible
                ? null
                : PasswordTransformationMethod.getInstance());
        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void login() {
        String identity = edtIdentity.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (identity.isEmpty()) {
            edtIdentity.setError("Vui lòng nhập email, tên tài khoản hoặc số điện thoại");
            edtIdentity.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        long userId = new DatabaseHelper(this).loginUser(identity, password);
        if (userId >= 0) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            Toast.makeText(this, "Thông tin đăng nhập không đúng", Toast.LENGTH_SHORT).show();
        }
    }
}
