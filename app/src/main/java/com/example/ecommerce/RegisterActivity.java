package com.example.ecommerce;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private ImageView ivAvatar;
    private String selectedAvatarPath = DatabaseHelper.DEFAULT_AVATAR_PATH;
    private boolean passwordVisible;
    private boolean confirmPasswordVisible;

    private final ActivityResultLauncher<String> avatarPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::saveAvatar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtRegisterEmail);
        edtPhone = findViewById(R.id.edtRegisterPhone);
        edtPassword = findViewById(R.id.edtRegisterPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmitRegister);
        progressBar = findViewById(R.id.progressBarRegister);
        ivAvatar = findViewById(R.id.ivRegisterAvatar);

        findViewById(R.id.btnBackToHome).setOnClickListener(v -> finish());
        findViewById(R.id.btnBackToLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        ImageButton chooseAvatar = findViewById(R.id.btnChooseAvatar);
        chooseAvatar.setOnClickListener(v -> avatarPicker.launch("image/*"));
        ivAvatar.setOnClickListener(v -> avatarPicker.launch("image/*"));

        findViewById(R.id.btnToggleRegisterPassword).setOnClickListener(v -> togglePassword());
        findViewById(R.id.btnToggleConfirmPassword).setOnClickListener(v -> toggleConfirmPassword());
        btnSubmit.setOnClickListener(v -> register());
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        edtPassword.setTransformationMethod(passwordVisible
                ? null
                : PasswordTransformationMethod.getInstance());
        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void toggleConfirmPassword() {
        confirmPasswordVisible = !confirmPasswordVisible;
        edtConfirmPassword.setTransformationMethod(confirmPasswordVisible
                ? null
                : PasswordTransformationMethod.getInstance());
        edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
    }

    private void saveAvatar(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            File avatarDir = new File(getFilesDir(), "avatars");
            if (!avatarDir.exists() && !avatarDir.mkdirs()) {
                throw new java.io.IOException("Không thể tạo thư mục ảnh");
            }
            String fileName = UUID.randomUUID().toString() + ".jpg";
            File destination = new File(avatarDir, fileName);
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(destination)) {
                if (input == null) {
                    throw new java.io.IOException("Không thể đọc ảnh");
                }
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }
            selectedAvatarPath = "avatars/" + fileName;
            ivAvatar.setImageURI(Uri.fromFile(destination));
        } catch (Exception error) {
            Toast.makeText(this, "Không thể lưu ảnh đại diện", Toast.LENGTH_SHORT).show();
        }
    }

    private void register() {
        String name = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (name.isEmpty()) {
            edtFullName.setError("Vui lòng nhập họ và tên");
            edtFullName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
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

        btnSubmit.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        boolean created = new DatabaseHelper(this).registerUser(
                name, email, email, password, phone, "", selectedAvatarPath);
        if (created) {
            Toast.makeText(this, "Đăng ký tài khoản thành công", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "Email hoặc tên tài khoản đã tồn tại", Toast.LENGTH_SHORT).show();
        }
    }
}
