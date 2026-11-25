package com.example.ecommerce.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.ecommerce.data.DatabaseHelper;
import com.example.ecommerce.data.DatabaseUtils;

import static com.example.ecommerce.data.DatabaseHelper.*;

public final class ReviewRepository {
    private final DatabaseHelper databaseHelper;
    private final AccountRepository accountRepository;

    public ReviewRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.accountRepository = new AccountRepository(databaseHelper);
    }

    private long getSessionUserId() {
        return accountRepository.getSessionUserId();
    }

    public Cursor getProductReviews(long productId) {
        return getProductReviews(productId, 0);
    }

    public Cursor getProductReviews(long productId, int starFilter) {
        if (starFilter <= 0) {
            return databaseHelper.getReadableDatabase().rawQuery(
                    "SELECT r.*, u.display_name, u.avatar_path " +
                            "FROM reviews r JOIN users u ON u.id = r.user_id " +
                            "WHERE r.product_id = ? ORDER BY r.created_at DESC",
                    new String[]{String.valueOf(productId)});
        }
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT r.*, u.display_name, u.avatar_path " +
                        "FROM reviews r JOIN users u ON u.id = r.user_id " +
                        "WHERE r.product_id = ? AND r.rating = ? ORDER BY r.created_at DESC",
                new String[]{String.valueOf(productId), String.valueOf(starFilter)});
    }

    public int[] getProductRatingCounts(long productId) {
        int[] counts = new int[6];
        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT rating, COUNT(*) FROM reviews WHERE product_id = ? GROUP BY rating",
                new String[]{String.valueOf(productId)});
        while (cursor.moveToNext()) {
            int rating = cursor.getInt(0);
            int count = cursor.getInt(1);
            if (rating >= 1 && rating <= 5) {
                counts[rating] = count;
            }
        }
        cursor.close();
        return counts;
    }

    public Cursor getUnreviewedItems() {
        long userId = getSessionUserId();
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT od.product_id, od.order_id, p.name, p.image_path, o.order_date " +
                        "FROM order_details od " +
                        "JOIN orders o ON o.id = od.order_id " +
                        "JOIN products p ON p.id = od.product_id " +
                        "WHERE o.user_id = ? AND o.status = ? " +
                        "AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.user_id = o.user_id " +
                        "AND r.product_id = od.product_id AND r.order_id = od.order_id) " +
                        "ORDER BY o.order_date DESC, od.id ASC",
                new String[]{String.valueOf(userId), STATUS_RECEIVED});
    }

    public Cursor getMyReviews() {
        long userId = getSessionUserId();
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT r.*, p.name, p.image_path FROM reviews r " +
                        "JOIN products p ON p.id = r.product_id " +
                        "WHERE r.user_id = ? ORDER BY r.created_at DESC",
                new String[]{String.valueOf(userId)});
    }

    public boolean saveReview(long productId, long orderId, int rating, String comment) {
        long userId = getSessionUserId();
        if (userId < 0 || rating < 1 || rating > 5) {
            return false;
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT 1 FROM order_details od JOIN orders o ON o.id = od.order_id " +
                            "WHERE o.id = ? AND o.user_id = ? AND o.status = ? " +
                            "AND od.product_id = ?",
                    new String[]{String.valueOf(orderId), String.valueOf(userId),
                            STATUS_RECEIVED, String.valueOf(productId)});
            boolean allowed = cursor.moveToFirst();
            cursor.close();
            if (!allowed) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("user_id", userId);
            values.put("product_id", productId);
            values.put("order_id", orderId);
            values.put("rating", rating);
            values.put("comment", comment == null ? "" : comment.trim());
            values.put("created_at", DatabaseUtils.now());
            int updated = db.update(TABLE_REVIEWS, values,
                    "user_id = ? AND product_id = ? AND order_id = ?",
                    new String[]{String.valueOf(userId), String.valueOf(productId),
                            String.valueOf(orderId)});
            if (updated == 0 && db.insert(TABLE_REVIEWS, null, values) == -1) {
                return false;
            }

            recalculateProductRating(db, productId);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public static void recalculateProductRating(SQLiteDatabase db, long productId) {
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*), COALESCE(AVG(rating), 0) FROM reviews WHERE product_id = ?",
                new String[]{String.valueOf(productId)});
        if (cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put("review_count", cursor.getInt(0));
            values.put("average_rating", cursor.getDouble(1));
            db.update(TABLE_PRODUCTS, values, "id = ?", new String[]{String.valueOf(productId)});
        }
        cursor.close();
    }
}
