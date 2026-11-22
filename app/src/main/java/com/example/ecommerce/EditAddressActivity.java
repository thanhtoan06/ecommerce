package com.example.ecommerce;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditAddressActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private EditText edtAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);
        databaseHelper = new DatabaseHelper(this);
        edtAddress = findViewById(R.id.edtAddress);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor != null && cursor.moveToFirst()) {
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            edtAddress.setText(address == null ? "" : address);
            cursor.close();
        }
        edtAddress.setSelection(edtAddress.getText().length());
        findViewById(R.id.btnSaveAddress).setOnClickListener(v -> save());
    }

    private void save() {
        String address = edtAddress.getText().toString().trim();
        if (address.isEmpty()) {
            edtAddress.setError("Vui lòng nhập địa chỉ");
            edtAddress.requestFocus();
            return;
        }
        Cursor cursor = databaseHelper.getSessionUser();
        if (cursor == null || !cursor.moveToFirst()) return;
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
        String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
        String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        String avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
        cursor.close();
        if (databaseHelper.updateCurrentUserProfile(displayName, username, phone, address, avatar)) {
            Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
