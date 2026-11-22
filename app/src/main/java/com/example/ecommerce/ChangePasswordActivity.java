package com.example.ecommerce;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChangePasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText oldPassword = findViewById(R.id.edtOldPassword);
        EditText newPassword = findViewById(R.id.edtNewPassword);
        EditText confirmPassword = findViewById(R.id.edtConfirmPassword);
        findViewById(R.id.btnSavePassword).setOnClickListener(v -> {
            String oldValue = oldPassword.getText().toString();
            String newValue = newPassword.getText().toString();
            String confirmValue = confirmPassword.getText().toString();
            if (oldValue.isEmpty()) {
                oldPassword.setError("Vui lòng nhập mật khẩu cũ");
                oldPassword.requestFocus();
                return;
            }
            if (newValue.length() < 6) {
                newPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
                newPassword.requestFocus();
                return;
            }
            if (!newValue.equals(confirmValue)) {
                confirmPassword.setError("Mật khẩu xác nhận không khớp");
                confirmPassword.requestFocus();
                return;
            }
            boolean changed = new DatabaseHelper(this)
                    .changeCurrentUserPassword(oldValue, newValue);
            if (!changed) {
                oldPassword.setError("Mật khẩu cũ không đúng");
                oldPassword.requestFocus();
                return;
            }
            Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
