package com.example.ecommerce.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.ecommerce.data.DatabaseHelper;
import com.example.ecommerce.data.DatabaseHelper.PasswordResetResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import static com.example.ecommerce.data.DatabaseHelper.*;

public final class AccountRepository {
    public static final String DEFAULT_AVATAR_PATH = DatabaseHelper.DEFAULT_AVATAR_PATH;
    private final DatabaseHelper databaseHelper;

    public AccountRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public boolean registerUser(String displayName, String username, String email, String password,
                                String phone, String address, String avatarPath) {
        if (displayName == null || displayName.trim().isEmpty()
                || username == null || username.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Cursor duplicate = db.rawQuery(
                "SELECT id FROM users WHERE username = ? OR email = ?",
                new String[]{username.trim(), email.trim()});
        boolean exists = duplicate.moveToFirst();
        duplicate.close();
        if (exists) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("display_name", displayName.trim());
        values.put("username", username.trim());
        values.put("email", email.trim());
        values.put("password_hash", hashPassword(password));
        values.put("phone", phone == null ? "" : phone.trim());
        values.put("address", address == null ? "" : address.trim());
        values.put("avatar_path", avatarPath == null || avatarPath.trim().isEmpty()
                ? DEFAULT_AVATAR_PATH : avatarPath.trim());
        return db.insert(TABLE_USERS, null, values) != -1;
    }

    public long loginUser(String identity, String password) {
        if (identity == null || password == null) {
            return -1;
        }

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, password_hash FROM users WHERE username = ? OR email = ? OR phone = ?",
                new String[]{identity.trim(), identity.trim(), identity.trim()});
        if (cursor.moveToFirst()) {
            long userId = cursor.getLong(0);
            String storedHash = cursor.getString(1);
            cursor.close();
            if (!hashPassword(password).equals(storedHash)) {
                return -1;
            }

            setSessionUser(userId);
            return userId;
        }
        cursor.close();
        return -1;
    }

    public PasswordResetResult resetPassword(
            String identity, String phone, String newPassword) {
        if (identity == null || identity.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()
                || newPassword == null || newPassword.length() < 6) {
            return PasswordResetResult.INVALID_INPUT;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        String value = identity.trim();
        long userId;
        String currentPasswordHash;
        try (Cursor cursor = db.rawQuery(
                "SELECT id, password_hash FROM users " +
                        "WHERE phone = ? AND (username = ? OR email = ?)",
                new String[]{phone.trim(), value, value})) {
            if (!cursor.moveToFirst()) {
                return PasswordResetResult.ACCOUNT_NOT_FOUND;
            }
            userId = cursor.getLong(0);
            currentPasswordHash = cursor.getString(1);
        }

        String newPasswordHash = hashPassword(newPassword);
        if (newPasswordHash.equals(currentPasswordHash)) {
            return PasswordResetResult.SAME_AS_CURRENT;
        }
        ContentValues values = new ContentValues();
        values.put("password_hash", newPasswordHash);
        return db.update(TABLE_USERS, values, "id = ?",
                new String[]{String.valueOf(userId)}) == 1
                ? PasswordResetResult.SUCCESS
                : PasswordResetResult.UPDATE_FAILED;
    }

    public boolean updateCurrentUserProfile(String displayName, String phone,
                                            String address, String avatarPath) {
        long userId = getSessionUserId();
        if (userId < 0 || displayName == null || displayName.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("display_name", displayName.trim());
        values.put("phone", phone == null ? "" : phone.trim());
        values.put("address", address == null ? "" : address.trim());
        if (avatarPath != null && !avatarPath.trim().isEmpty()) {
            values.put("avatar_path", avatarPath.trim());
        }
        return db.update(TABLE_USERS, values, "id = ?",
                new String[]{String.valueOf(userId)}) == 1;
    }

    public boolean changeCurrentUserPassword(String oldPassword,
                                             String newPassword) {
        long userId = getSessionUserId();
        if (userId < 0
                || oldPassword == null
                || newPassword == null
                || newPassword.length() < 6) {
            return false;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT password_hash FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        String storedHash = cursor.getString(0);
        cursor.close();
        if (!hashPassword(oldPassword).equals(storedHash)) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("password_hash", hashPassword(newPassword));
        return db.update(TABLE_USERS, values, "id = ?",
                new String[]{String.valueOf(userId)}) == 1;
    }

    public void setSessionUser(long userId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("user_id", userId);
        db.insertWithOnConflict(TABLE_APP_SESSION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long getSessionUserId() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT user_id FROM app_session WHERE id = 1", null);
        long userId = cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getLong(0) : -1;
        cursor.close();
        return userId;
    }

    public Cursor getSessionUser() {
        long userId = getSessionUserId();
        if (userId < 0) {
            return null;
        }
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)});
    }

    public void logout() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.putNull("user_id");
        db.insertWithOnConflict(TABLE_APP_SESSION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format(Locale.US, "%02x", value));
            }
            return hex.toString();
        } catch (Exception error) {
            throw new RuntimeException("Cannot hash password", error);
        }
    }
}
