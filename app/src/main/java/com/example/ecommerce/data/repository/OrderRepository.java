package com.example.ecommerce.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.ecommerce.data.DatabaseHelper;
import com.example.ecommerce.data.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import static com.example.ecommerce.data.DatabaseHelper.*;

public final class OrderRepository {
    private final DatabaseHelper databaseHelper;
    private final AccountRepository accountRepository;

    public OrderRepository(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.accountRepository = new AccountRepository(databaseHelper);
    }

    private long getSessionUserId() {
        return accountRepository.getSessionUserId();
    }

    private long insertOrder(SQLiteDatabase db, long userId, String orderDate, String status,
                             double total, String receiverName, String phone, String address,
                             String note, double shippingFee, String paymentMethod) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("order_date", orderDate);
        values.put("status", status);
        values.put("total_amount", total);
        values.put("shipping_fee", shippingFee);
        values.put("payment_method", paymentMethod);
        values.put("receiver_name", receiverName);
        values.put("phone", phone);
        values.put("address", address);
        values.put("note", note);
        return db.insert(TABLE_ORDERS, null, values);
    }

    private void insertOrderDetail(SQLiteDatabase db, long orderId, long productId,
                                   int quantity, double unitPrice) {
        ContentValues values = new ContentValues();
        values.put("order_id", orderId);
        values.put("product_id", productId);
        values.put("quantity", quantity);
        values.put("unit_price", unitPrice);
        values.put("subtotal", unitPrice * quantity);
        db.insert(TABLE_ORDER_DETAILS, null, values);
    }

    public long placeOrder(String receiverName, String phone, String address, String note) {
        return placeOrder(receiverName, phone, address, note, null);
    }

    public long placeOrder(String receiverName, String phone, String address, String note,
                           long[] selectedCartIds) {
        return placeOrder(receiverName, phone, address, note, selectedCartIds,
                SHIPPING_FEE, PAYMENT_METHOD_COD);
    }

    public long placeOrder(String receiverName, String phone, String address, String note,
                           long[] selectedCartIds, double shippingFee, String paymentMethod) {
        long userId = getSessionUserId();
        if (userId < 0
                || receiverName == null || receiverName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()
                || address == null || address.trim().isEmpty()
                || (selectedCartIds != null && selectedCartIds.length == 0)
                || shippingFee < 0
                || paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return ORDER_FAILED;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            List<double[]> items = new ArrayList<>();
            double total = shippingFee;
            boolean enoughStock = true;
            StringBuilder query = new StringBuilder(
                    "SELECT ci.product_id, ci.quantity, p.price, p.stock_quantity " +
                            "FROM cart_items ci JOIN products p ON p.id = ci.product_id " +
                            "WHERE ci.user_id = ?");
            List<String> queryArgs = new ArrayList<>();
            queryArgs.add(String.valueOf(userId));
            if (selectedCartIds != null) {
                query.append(" AND ci.id IN (").append(DatabaseUtils.placeholders(selectedCartIds.length)).append(")");
                for (long cartId : selectedCartIds) {
                    queryArgs.add(String.valueOf(cartId));
                }
            }

            Cursor cursor = db.rawQuery(query.toString(), queryArgs.toArray(new String[0]));
            while (cursor.moveToNext()) {
                long productId = cursor.getLong(0);
                int quantity = cursor.getInt(1);
                double price = cursor.getDouble(2);
                int stock = cursor.getInt(3);
                if (quantity > stock) {
                    enoughStock = false;
                }
                items.add(new double[]{productId, quantity, price});
                total += quantity * price;
            }
            cursor.close();

            if (items.isEmpty()
                    || (selectedCartIds != null && items.size() != selectedCartIds.length)) {
                return ORDER_FAILED;
            }
            if (!enoughStock) {
                return ORDER_OUT_OF_STOCK;
            }

            long orderId = insertOrder(db, userId, DatabaseUtils.now(), STATUS_DELIVERING, total,
                    receiverName.trim(), phone.trim(), address.trim(),
                    note == null ? "" : note.trim(), shippingFee, paymentMethod.trim());
            if (orderId == -1) {
                return ORDER_FAILED;
            }

            for (double[] item : items) {
                long productId = (long) item[0];
                int quantity = (int) item[1];
                insertOrderDetail(db, orderId, productId, quantity, item[2]);
                db.execSQL("UPDATE products SET stock_quantity = stock_quantity - ?, " +
                                "sold_quantity = sold_quantity + ? WHERE id = ?",
                        new Object[]{quantity, quantity, productId});
            }

            String deleteWhere = "user_id = ?";
            List<String> deleteArgs = new ArrayList<>();
            deleteArgs.add(String.valueOf(userId));
            if (selectedCartIds != null) {
                deleteWhere += " AND id IN (" + DatabaseUtils.placeholders(selectedCartIds.length) + ")";
                for (long cartId : selectedCartIds) {
                    deleteArgs.add(String.valueOf(cartId));
                }
            }
            int deleted = db.delete(TABLE_CART_ITEMS, deleteWhere,
                    deleteArgs.toArray(new String[0]));
            if (selectedCartIds != null && deleted != selectedCartIds.length) {
                return ORDER_FAILED;
            }
            db.setTransactionSuccessful();
            return orderId;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getOrdersByStatus(String status) {
        long userId = getSessionUserId();
        if (status == null) {
            return databaseHelper.getReadableDatabase().rawQuery(
                    "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC, id DESC",
                    new String[]{String.valueOf(userId)});
        }
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM orders WHERE user_id = ? AND status = ? " +
                        "ORDER BY order_date DESC, id DESC",
                new String[]{String.valueOf(userId), status});
    }

    public int getOrderCountByStatus(String status) {
        long userId = getSessionUserId();
        if (userId < 0) {
            return 0;
        }

        String query = status == null
                ? "SELECT COUNT(*) FROM orders WHERE user_id = ?"
                : "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = ?";
        String[] selectionArgs = status == null
                ? new String[]{String.valueOf(userId)}
                : new String[]{String.valueOf(userId), status};
        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, selectionArgs);
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public Cursor getOrderById(long orderId) {
        long userId = getSessionUserId();
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM orders WHERE id = ? AND user_id = ?",
                new String[]{String.valueOf(orderId), String.valueOf(userId)});
    }

    public Cursor getOrderDetails(long orderId) {
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT od.*, p.name, p.image_path FROM order_details od " +
                        "JOIN products p ON p.id = od.product_id " +
                        "WHERE od.order_id = ? ORDER BY od.id ASC",
                new String[]{String.valueOf(orderId)});
    }

    public String getOrderItemsSummary(long orderId) {
        StringBuilder builder = new StringBuilder();
        Cursor cursor = getOrderDetails(orderId);
        while (cursor.moveToNext()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("• ")
                    .append(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    .append(" x")
                    .append(cursor.getInt(cursor.getColumnIndexOrThrow("quantity")));
        }
        cursor.close();
        return builder.toString();
    }

    public boolean markOrderReceived(long orderId) {
        long userId = getSessionUserId();
        if (userId < 0) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("status", STATUS_RECEIVED);
        return databaseHelper.getWritableDatabase().update(TABLE_ORDERS, values,
                "id = ? AND user_id = ? AND status = ?",
                new String[]{String.valueOf(orderId), String.valueOf(userId),
                        STATUS_DELIVERING}) == 1;
    }

    public boolean cancelOrder(long orderId, String reason) {
        long userId = getSessionUserId();
        if (userId < 0 || reason == null || reason.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("status", STATUS_CANCELLED);
            values.put("cancel_reason", reason.trim());
            int updated = db.update(TABLE_ORDERS, values,
                    "id = ? AND user_id = ? AND status = ?",
                    new String[]{String.valueOf(orderId), String.valueOf(userId),
                            STATUS_DELIVERING});
            if (updated != 1) {
                return false;
            }

            List<long[]> items = new ArrayList<>();
            Cursor cursor = db.rawQuery(
                    "SELECT product_id, quantity FROM order_details WHERE order_id = ?",
                    new String[]{String.valueOf(orderId)});
            while (cursor.moveToNext()) {
                items.add(new long[]{cursor.getLong(0), cursor.getLong(1)});
            }
            cursor.close();

            for (long[] item : items) {
                db.execSQL("UPDATE products SET stock_quantity = stock_quantity + ?, " +
                                "sold_quantity = MAX(sold_quantity - ?, 0) WHERE id = ?",
                        new Object[]{item[1], item[1], item[0]});
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getOrderProductsWithReviews(long orderId) {
        long userId = getSessionUserId();
        return databaseHelper.getReadableDatabase().rawQuery(
                "SELECT od.product_id, od.order_id, od.quantity, p.name, p.image_path, " +
                        "r.rating, r.comment " +
                        "FROM order_details od " +
                        "JOIN orders o ON o.id = od.order_id " +
                        "JOIN products p ON p.id = od.product_id " +
                        "LEFT JOIN reviews r ON r.user_id = o.user_id " +
                        "AND r.product_id = od.product_id AND r.order_id = od.order_id " +
                        "WHERE o.id = ? AND o.user_id = ? " +
                        "ORDER BY od.id ASC",
                new String[]{String.valueOf(orderId), String.valueOf(userId)});
    }
}
