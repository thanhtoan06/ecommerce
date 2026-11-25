package com.example.ecommerce.data.repository;

import android.database.Cursor;

import com.example.ecommerce.data.DatabaseHelper;

public final class ProductRepository {
    private final DatabaseHelper databaseHelper;

    public ProductRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public Cursor getAllCategories() {
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM categories ORDER BY name ASC", null);
    }

    public Cursor getAllProducts() {
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT p.*, c.name AS category_name FROM products p " +
                        "JOIN categories c ON c.id = p.category_id ORDER BY p.id ASC",
                null);
    }

    public Cursor getProductById(long productId) {
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT p.*, c.name AS category_name FROM products p " +
                        "JOIN categories c ON c.id = p.category_id WHERE p.id = ?",
                new String[]{String.valueOf(productId)});
    }
}
