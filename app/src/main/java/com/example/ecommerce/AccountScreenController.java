package com.example.ecommerce;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;

public class AccountScreenController {
    private final AppCompatActivity activity;
    private final View root;
    private final DatabaseHelper databaseHelper;
    private String currentDisplayName = "";
    private String currentPhone = "";
    private String currentAddress = "";

    public AccountScreenController(AppCompatActivity activity, View root,
                                   DatabaseHelper databaseHelper) {
        this.activity = activity;
        this.root = root;
        this.databaseHelper = databaseHelper;
    }

    public void refresh() {
        String displayName;
        String username;
        String avatarPath;
        String email;
        String phone;
        String address;
        try (Cursor cursor = databaseHelper.getSessionUser()) {
            if (cursor == null || !cursor.moveToFirst()) {
                hide();
                return;
            }
            displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
            username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            avatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
            email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        }

        currentDisplayName = displayName;
        currentPhone = phone == null ? "" : phone;
        currentAddress = address == null ? "" : address;
        ((TextView) root.findViewById(R.id.tvAccountName)).setText(displayName);
        TextView tvDisplayName = root.findViewById(R.id.tvAccountDisplayName);
        if (tvDisplayName != null) {
            tvDisplayName.setText(displayName);
        }
        ((TextView) root.findViewById(R.id.tvAccountEmail)).setText(email);
        TextView emailDetail = root.findViewById(R.id.tvAccountEmailDetail);
        if (emailDetail != null) emailDetail.setText(email);
        TextView usernameDetail = root.findViewById(R.id.tvAccountUsername);
        if (usernameDetail != null) usernameDetail.setText(username);

        ((TextView) root.findViewById(R.id.tvAccountPhone)).setText(
                currentPhone.trim().isEmpty() ? "Chưa cập nhật" : currentPhone);
        ((TextView) root.findViewById(R.id.tvAccountAddress)).setText(
                currentAddress.trim().isEmpty() ? "Chưa cập nhật" : currentAddress);
        loadAvatar((ImageView) root.findViewById(R.id.ivAccountAvatar), avatarPath);

        View rowDisplayName = root.findViewById(R.id.rowDisplayName);
        View rowPhone = root.findViewById(R.id.rowPhone);
        View rowAddress = root.findViewById(R.id.rowAddress);
        if (rowDisplayName != null) {
            rowDisplayName.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, EditNameActivity.class)));
        }
        if (rowPhone != null) {
            rowPhone.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, EditPhoneActivity.class)));
        }
        if (rowAddress != null) {
            rowAddress.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, EditAddressActivity.class)));
        }

        root.findViewById(R.id.btnOrderHistory).setOnClickListener(v ->
                Toast.makeText(activity, "Lịch sử đơn hàng sẽ được làm sau",
                        Toast.LENGTH_SHORT).show());
        root.findViewById(R.id.btnMyReviews).setOnClickListener(v ->
                Toast.makeText(activity, "Đánh giá của tôi sẽ được làm sau",
                        Toast.LENGTH_SHORT).show());
        View changePassword = root.findViewById(R.id.btnChangePassword);
        if (changePassword != null) {
            changePassword.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, ChangePasswordActivity.class)));
        }
        ((MaterialButton) root.findViewById(R.id.btnLogout)).setOnClickListener(v ->
                new AlertDialog.Builder(activity)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc muốn đăng xuất không?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Đăng xuất", (dialog, which) -> {
                            databaseHelper.logout();
                            Toast.makeText(activity, "Bạn đã đăng xuất",
                                    Toast.LENGTH_SHORT).show();
                            activity.startActivity(new Intent(activity, LoginActivity.class));
                            activity.finish();
                        })
                        .show());
    }

    private void loadAvatar(ImageView imageView, String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()
                || DatabaseHelper.DEFAULT_AVATAR_PATH.equals(avatarPath)) {
            imageView.setImageResource(R.drawable.ic_avatar_default);
            return;
        }
        if (avatarPath.startsWith("avatars/")) {
            File file = new File(activity.getFilesDir(), avatarPath);
            if (file.exists()) {
                imageView.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_avatar_default);
    }


    public boolean updateAvatar(String avatarPath) {
        boolean saved = databaseHelper.updateCurrentUserProfile(
                currentDisplayName, currentPhone, currentAddress, avatarPath);
        if (saved) refresh();
        return saved;
    }

    public void hide() {
        root.setVisibility(View.GONE);
    }
}
