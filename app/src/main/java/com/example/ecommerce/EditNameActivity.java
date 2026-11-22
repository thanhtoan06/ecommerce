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
        databaseHelper = new DatabaseHelper(this);
        edtName = findViewById(R.id.edtDisplayName);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor != null && cursor.moveToFirst()) {
            edtName.setText(cursor.getString(cursor.getColumnIndexOrThrow("display_name")));
            cursor.close();
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
        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor == null || !cursor.moveToFirst()) return;
        String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        String avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
        cursor.close();
        if (databaseHelper.updateCurrentUserProfile(name, username, phone, address, avatar)) {
            Toast.makeText(this, "Đã lưu tên người dùng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
