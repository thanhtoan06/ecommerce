package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChangePasswordActivity extends AppCompatActivity {
    private boolean oldPasswordVisible;
    private boolean newPasswordVisible;
    private boolean confirmPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        StatusBarUtils.setupHeader(this, findViewById(R.id.topBar));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText oldPassword = findViewById(R.id.edtOldPassword);
        EditText newPassword = findViewById(R.id.edtNewPassword);
        EditText confirmPassword = findViewById(R.id.edtConfirmPassword);
        ImageView oldPasswordToggle = findViewById(R.id.btnToggleOldPassword);
        ImageView newPasswordToggle = findViewById(R.id.btnToggleNewPassword);
        ImageView confirmPasswordToggle = findViewById(R.id.btnToggleConfirmPassword);
        oldPasswordToggle.setOnClickListener(v -> oldPasswordVisible =
                togglePasswordVisibility(oldPassword, oldPasswordToggle, oldPasswordVisible));
        newPasswordToggle.setOnClickListener(v -> newPasswordVisible =
                togglePasswordVisibility(newPassword, newPasswordToggle, newPasswordVisible));
        confirmPasswordToggle.setOnClickListener(v -> confirmPasswordVisible =
                togglePasswordVisibility(confirmPassword, confirmPasswordToggle, confirmPasswordVisible));

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
            if (newValue.equals(oldValue)) {
                newPassword.setError("Mật khẩu mới không được trùng mật khẩu hiện tại");
                newPassword.requestFocus();
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

    private boolean togglePasswordVisibility(
            EditText passwordField,
            ImageView toggle,
            boolean currentlyVisible) {
        boolean visible = !currentlyVisible;
        passwordField.setTransformationMethod(visible
                ? null
                : PasswordTransformationMethod.getInstance());
        passwordField.setSelection(passwordField.getText().length());
        toggle.setImageResource(visible
                ? R.drawable.ic_visibility
                : R.drawable.ic_visibility_off);
        toggle.setContentDescription(visible
                ? "Ẩn mật khẩu"
                : "Hiện mật khẩu");
        return visible;
    }
}
