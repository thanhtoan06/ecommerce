package com.example.ecommerce;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class AccountActivity extends AppCompatActivity {
    private AccountScreenController accountController;

    private final ActivityResultLauncher<String> avatarPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::saveAvatar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        StatusBarUtils.setupHeader(this, findViewById(R.id.layoutAccountHeader));
        StatusBarUtils.setupBottomNav(this, findViewById(R.id.bottomNavigation));

        accountController = new AccountScreenController(this,
                findViewById(android.R.id.content),
                new DatabaseHelper(this));
        findViewById(R.id.ivAccountAvatar).setOnClickListener(v ->
                avatarPicker.launch("image/*"));
        findViewById(R.id.btnEditAccountAvatar).setOnClickListener(v ->
                avatarPicker.launch("image/*"));
        BottomNavigationController.setupForActivity(this, 3);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accountController != null) {
            accountController.refresh();
        }
    }

    private void saveAvatar(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            File avatarDir = new File(getFilesDir(), "avatars");
            if (!avatarDir.exists() && !avatarDir.mkdirs()) {
                throw new java.io.IOException("Không thể tạo thư mục avatar");
            }
            String fileName = UUID.randomUUID() + ".jpg";
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
            String avatarPath = "avatars/" + fileName;
            if (accountController.updateAvatar(avatarPath)) {
                Toast.makeText(this, "Đã cập nhật ảnh đại diện",
                        Toast.LENGTH_SHORT).show();
            } else {
                accountController.refresh();
                Toast.makeText(this, "Không thể cập nhật ảnh đại diện",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception error) {
            Toast.makeText(this, "Không thể lưu ảnh đại diện", Toast.LENGTH_SHORT).show();
        }
    }
}
