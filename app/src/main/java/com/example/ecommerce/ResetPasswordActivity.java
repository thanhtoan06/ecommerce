package com.example.ecommerce;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {
    private EditText edtIdentity;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private Button btnReset;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        edtIdentity = findViewById(R.id.edtResetIdentity);
        edtPhone = findViewById(R.id.edtResetPhone);
        edtPassword = findViewById(R.id.edtResetPassword);
        edtConfirmPassword = findViewById(R.id.edtResetConfirmPassword);
        btnReset = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBarReset);

        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> {
            finish();
        });
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String identity = edtIdentity.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (identity.isEmpty()) {
            edtIdentity.setError("Vui lòng nhập email hoặc tên tài khoản");
            edtIdentity.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            edtPhone.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        btnReset.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        boolean reset = new DatabaseHelper(this).resetPassword(identity, phone, password);
        if (reset) {
            Toast.makeText(this, "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            progressBar.setVisibility(View.GONE);
            btnReset.setEnabled(true);
            Toast.makeText(this,
                    "Email/tên tài khoản và số điện thoại không khớp",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
