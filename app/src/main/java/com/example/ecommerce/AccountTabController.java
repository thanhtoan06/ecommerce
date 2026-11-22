package com.example.ecommerce;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class AccountTabController {
    private final AppCompatActivity activity;
    private final View root;
    private final DatabaseHelper databaseHelper;
    private String currentDisplayName = "";
    private String currentUsername = "";
    private String currentPhone = "";
    private String currentAddress = "";
    private String currentAvatarPath = "";

    public AccountTabController(AppCompatActivity activity, View root,
                                DatabaseHelper databaseHelper) {
        this.activity = activity;
        this.root = root;
        this.databaseHelper = databaseHelper;
    }

    public void refresh() {
        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor == null || !cursor.moveToFirst()) {
            hide();
            return;
        }

        currentDisplayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        currentUsername = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        currentAvatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
        ((TextView) root.findViewById(R.id.tvAccountName)).setText(currentDisplayName);
        TextView tvDisplayName = root.findViewById(R.id.tvAccountDisplayName);
        if (tvDisplayName != null) {
            tvDisplayName.setText(currentDisplayName);
        }
        String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
        ((TextView) root.findViewById(R.id.tvAccountEmail)).setText(email);
        TextView emailDetail = root.findViewById(R.id.tvAccountEmailDetail);
        if (emailDetail != null) emailDetail.setText(email);
        TextView usernameDetail = root.findViewById(R.id.tvAccountUsername);
        if (usernameDetail != null) usernameDetail.setText(currentUsername);

        String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        currentPhone = phone == null ? "" : phone;
        currentAddress = address == null ? "" : address;
        ((TextView) root.findViewById(R.id.tvAccountPhone)).setText(
                currentPhone.trim().isEmpty() ? "Chưa cập nhật" : currentPhone);
        ((TextView) root.findViewById(R.id.tvAccountAddress)).setText(
                currentAddress.trim().isEmpty() ? "Chưa cập nhật" : currentAddress);
        loadAvatar((ImageView) root.findViewById(R.id.ivAccountAvatar), currentAvatarPath);
        cursor.close();

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
        ((MaterialButton) root.findViewById(R.id.btnLogout)).setOnClickListener(v -> {
            databaseHelper.logout();
            activity.startActivity(new Intent(activity, LoginActivity.class));
            activity.finish();
        });
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


    public void updateProfileField(String field, String value) {
        if ("avatar".equals(field)) {
            boolean saved = databaseHelper.updateCurrentUserProfile(
                    currentDisplayName, currentUsername, currentPhone,
                    currentAddress, value);
            if (saved) refresh();
        }
    }

    public void show() {
        refresh();
        root.setVisibility(View.VISIBLE);
    }

    public void hide() {
        root.setVisibility(View.GONE);
    }
}
