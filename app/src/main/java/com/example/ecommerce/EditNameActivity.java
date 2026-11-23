package com.example.ecommerce;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditNameActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private EditText edtName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);
        StatusBarUtils.setupHeader(this, findViewById(R.id.topBar));
        databaseHelper = new DatabaseHelper(this);
        edtName = findViewById(R.id.edtDisplayName);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        try (Cursor cursor = databaseHelper.getSessionUser()) {
            if (cursor == null || !cursor.moveToFirst()) {
                finish();
                return;
            }
            edtName.setText(cursor.getString(cursor.getColumnIndexOrThrow("display_name")));
        }
        edtName.setSelection(edtName.getText().length());

        findViewById(R.id.btnSaveName).setOnClickListener(v -> save());
    }

    private void save() {
        String name = edtName.getText().toString().trim();
        if (name.isEmpty()) {
            edtName.setError("Vui lòng nhập tên người dùng");
            edtName.requestFocus();
            return;
        }
        String phone;
        String address;
        String avatar;
        try (Cursor cursor = databaseHelper.getSessionUser()) {
            if (cursor == null || !cursor.moveToFirst()) return;
            phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
        }
        if (databaseHelper.updateCurrentUserProfile(name, phone, address, avatar)) {
            Toast.makeText(this, "Đã lưu tên người dùng", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Không thể cập nhật tên người dùng",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
