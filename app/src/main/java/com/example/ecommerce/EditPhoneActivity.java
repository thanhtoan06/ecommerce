package com.example.ecommerce;

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
        databaseHelper = new DatabaseHelper(this);
        edtPhone = findViewById(R.id.edtPhone);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor != null && cursor.moveToFirst()) {
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            edtPhone.setText(phone == null ? "" : phone);
            cursor.close();
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
        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor == null || !cursor.moveToFirst()) return;
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        String avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
        cursor.close();
        if (databaseHelper.updateCurrentUserProfile(displayName, username, phone, address, avatar)) {
            Toast.makeText(this, "Đã lưu số điện thoại", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
