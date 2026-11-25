package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditPhoneActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private EditText edtPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_phone);
        StatusBarUtils.setupHeader(this, findViewById(R.id.topBar));
        databaseHelper = new DatabaseHelper(this);
        edtPhone = findViewById(R.id.edtPhone);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        try (Cursor cursor = databaseHelper.getSessionUser()) {
            if (cursor == null || !cursor.moveToFirst()) {
                finish();
                return;
            }
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            edtPhone.setText(phone == null ? "" : phone);
        }
        edtPhone.setSelection(edtPhone.getText().length());
        findViewById(R.id.btnSavePhone).setOnClickListener(v -> save());
    }

    private void save() {
        String phone = edtPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            edtPhone.requestFocus();
            return;
        }
        String displayName;
        String address;
        String avatar;
        try (Cursor cursor = databaseHelper.getSessionUser()) {
            if (cursor == null || !cursor.moveToFirst()) return;
            displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
            address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
        }
        if (databaseHelper.updateCurrentUserProfile(displayName, phone, address, avatar)) {
            Toast.makeText(this, "Đã lưu số điện thoại", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Không thể cập nhật số điện thoại",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
