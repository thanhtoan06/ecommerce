package com.example.ecommerce;

import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class AccountActivity extends AppCompatActivity {
    private AccountTabController accountController;
    private String selectedAvatarPath;

    private final ActivityResultLauncher<String> avatarPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::saveAvatar);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_account);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(systemBars.left, systemBars.top,
                            systemBars.right, systemBars.bottom);
                    return insets;
                });

        accountController = new AccountTabController(this,
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
            selectedAvatarPath = "avatars/" + fileName;
            ImageView avatar = findViewById(R.id.ivAccountAvatar);
            avatar.setImageURI(Uri.fromFile(destination));
            accountController.updateProfileField("avatar", selectedAvatarPath);
        } catch (Exception error) {
            Toast.makeText(this, "Không thể lưu ảnh đại diện", Toast.LENGTH_SHORT).show();
        }
    }
}
