package com.example.ecommerce.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.ecommerce.data.DatabaseHelper;
import com.example.ecommerce.data.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import static com.example.ecommerce.data.DatabaseHelper.*;

public final class CartRepository {
    private final DatabaseHelper databaseHelper;
    private final AccountRepository accountRepository;

    public CartRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.accountRepository = new AccountRepository(databaseHelper);
    }

    private long getSessionUserId() {
        return accountRepository.getSessionUserId();
    }

    public int getAvailableCartQuantity(long productId) {
        long userId = getSessionUserId();
        if (userId < 0 || productId <= 0) {
            return 0;
        }

        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT p.stock_quantity - COALESCE(ci.quantity, 0) " +
                        "FROM products p LEFT JOIN cart_items ci " +
                        "ON ci.product_id = p.id AND ci.user_id = ? WHERE p.id = ?",
                new String[]{String.valueOf(userId), String.valueOf(productId)});
        int availableQuantity = 0;
        if (cursor.moveToFirst()) {
            availableQuantity = Math.max(0, cursor.getInt(0));
        }
        cursor.close();
        return availableQuantity;
    }

    public boolean addToCart(long productId, int quantity) {
        return addToCartAndGetCartId(productId, quantity) > 0;
    }

    public long addToCartAndGetCartId(long productId, int quantity) {
        long userId = getSessionUserId();
        if (userId < 0 || productId <= 0 || quantity <= 0) {
            return -1;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int stock;
            try (Cursor product = db.rawQuery(
                    "SELECT stock_quantity FROM products WHERE id = ?",
                    new String[]{String.valueOf(productId)})) {
                if (!product.moveToFirst()) {
                    return -1;
                }
                stock = product.getInt(0);
            }

            long cartItemId = -1;
            int existingQuantity = 0;
            try (Cursor cartItem = db.rawQuery(
                    "SELECT id, quantity FROM cart_items WHERE user_id = ? AND product_id = ?",
                    new String[]{String.valueOf(userId), String.valueOf(productId)})) {
                if (cartItem.moveToFirst()) {
                    cartItemId = cartItem.getLong(0);
                    existingQuantity = cartItem.getInt(1);
                }
            }
            if (existingQuantity + quantity > stock) {
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("product_id", productId);
            values.put("quantity", existingQuantity + quantity);
            if (cartItemId > 0) {
                if (db.update(TABLE_CART_ITEMS, values, "id = ? AND user_id = ?",
                        new String[]{String.valueOf(cartItemId), String.valueOf(userId)}) != 1) {
                    return -1;
                }
            } else {
                cartItemId = db.insert(TABLE_CART_ITEMS, null, values);
                if (cartItemId == -1) {
                    return -1;
                }
            }

            db.setTransactionSuccessful();
            return cartItemId;
        } finally {
            db.endTransaction();
        }
    }

    public int getCartItemCount() {
        long userId = getSessionUserId();
        if (userId < 0) {
            return 0;
        }
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public Cursor getCartItems() {
        long userId = getSessionUserId();
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT ci.id AS cart_id, ci.product_id, ci.quantity, p.name, p.price, " +
                        "p.image_path, p.stock_quantity, cat.name AS category_name, " +
                        "(ci.quantity * p.price) AS subtotal " +
                        "FROM cart_items ci JOIN products p ON p.id = ci.product_id " +
                        "JOIN categories cat ON cat.id = p.category_id " +
                        "WHERE ci.user_id = ? ORDER BY ci.id ASC",
                new String[]{String.valueOf(userId)});
    }

    public Cursor getCartItems(long[] cartIds) {
        long userId = getSessionUserId();
        if (cartIds == null || cartIds.length == 0) {
            return databaseHelper.getReadableDatabase().rawQuery(
                    "SELECT c.id AS cart_id, c.product_id, c.quantity, p.name, p.price, " +
                            "p.image_path, p.stock_quantity, (c.quantity * p.price) AS subtotal " +
                            "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                            "WHERE c.user_id = ? AND 0 ORDER BY c.id ASC",
                    new String[]{String.valueOf(userId)});
        }
        String selection = "c.user_id = ? AND c.id IN (" + DatabaseUtils.placeholders(cartIds.length) + ")";
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT c.id AS cart_id, c.product_id, c.quantity, p.name, p.price, " +
                        "p.image_path, p.stock_quantity, (c.quantity * p.price) AS subtotal " +
                        "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                        "WHERE " + selection + " ORDER BY c.id ASC",
                DatabaseUtils.cartSelectionArgs(userId, cartIds));
    }

    public double getCartTotal() {
        long userId = getSessionUserId();
        if (userId < 0) {
            return 0;
        }
        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(c.quantity * p.price), 0) " +
                        "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                        "WHERE c.user_id = ?",
                new String[]{String.valueOf(userId)});
        double total = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return total;
    }

    public double getCartTotal(long[] cartIds) {
        long userId = getSessionUserId();
        if (userId < 0 || cartIds == null || cartIds.length == 0) {
            return 0;
        }
        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(c.quantity * p.price), 0) " +
                        "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                        "WHERE c.user_id = ? AND c.id IN (" + DatabaseUtils.placeholders(cartIds.length) + ")",
                DatabaseUtils.cartSelectionArgs(userId, cartIds));
        double total = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return total;
    }

    public boolean updateCartQuantity(long cartId, int newQuantity) {
        long userId = getSessionUserId();
        if (userId < 0 || newQuantity <= 0) {
            return false;
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT p.stock_quantity FROM cart_items ci " +
                        "JOIN products p ON p.id = ci.product_id " +
                        "WHERE ci.id = ? AND ci.user_id = ?",
                new String[]{String.valueOf(cartId), String.valueOf(userId)});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        int stock = cursor.getInt(0);
        cursor.close();
        if (newQuantity > stock) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("quantity", newQuantity);
        return db.update(TABLE_CART_ITEMS, values, "id = ? AND user_id = ?",
                new String[]{String.valueOf(cartId), String.valueOf(userId)}) == 1;
    }

    public boolean removeCartItem(long cartId) {
        long userId = getSessionUserId();
        if (userId < 0) {
            return false;
        }
        return databaseHelper.getWritableDatabase().delete(TABLE_CART_ITEMS, "id = ? AND user_id = ?",
                new String[]{String.valueOf(cartId), String.valueOf(userId)}) == 1;
    }

    public boolean clearCart() {
        long userId = getSessionUserId();
        if (userId < 0) {
            return false;
        }
        return databaseHelper.getWritableDatabase().delete(TABLE_CART_ITEMS, "user_id = ?",
                new String[]{String.valueOf(userId)}) > 0;
    }
}
