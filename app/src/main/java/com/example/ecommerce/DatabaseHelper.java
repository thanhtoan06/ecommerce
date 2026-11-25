package com.example.ecommerce;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    public enum PasswordResetResult {
        SUCCESS,
        INVALID_INPUT,
        ACCOUNT_NOT_FOUND,
        SAME_AS_CURRENT,
        UPDATE_FAILED
    }

    public static final String DATABASE_NAME = "ecommerce.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_CART_ITEMS = "cart_items";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_ORDER_DETAILS = "order_details";
    public static final String TABLE_REVIEWS = "reviews";
    public static final String TABLE_APP_SESSION = "app_session";

    public static final String STATUS_DELIVERING = "Đang giao";
    public static final String STATUS_RECEIVED = "Đã nhận hàng";
    public static final String STATUS_CANCELLED = "Đã hủy";
    public static final String DEFAULT_AVATAR_PATH = "ic_avatar_default";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "display_name TEXT NOT NULL, " +
                "username TEXT NOT NULL UNIQUE, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "phone TEXT, " +
                "address TEXT, " +
                "avatar_path TEXT NOT NULL)");

        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_id INTEGER NOT NULL, " +
                "name TEXT NOT NULL, " +
                "price REAL NOT NULL CHECK(price >= 0), " +
                "description TEXT NOT NULL, " +
                "image_path TEXT NOT NULL, " +
                "stock_quantity INTEGER NOT NULL CHECK(stock_quantity >= 0), " +
                "sold_quantity INTEGER NOT NULL DEFAULT 0 CHECK(sold_quantity >= 0), " +
                "average_rating REAL NOT NULL DEFAULT 0, " +
                "review_count INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE RESTRICT)");

        db.execSQL("CREATE TABLE cart_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL CHECK(quantity > 0), " +
                "UNIQUE(user_id, product_id), " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "order_date TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "total_amount REAL NOT NULL CHECK(total_amount >= 0), " +
                "receiver_name TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "address TEXT NOT NULL, " +
                "note TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE order_details (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "order_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL CHECK(quantity > 0), " +
                "unit_price REAL NOT NULL CHECK(unit_price >= 0), " +
                "subtotal REAL NOT NULL CHECK(subtotal >= 0), " +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE RESTRICT)");

        db.execSQL("CREATE TABLE reviews (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, " +
                "order_id INTEGER NOT NULL, " +
                "rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5), " +
                "comment TEXT, " +
                "created_at TEXT NOT NULL, " +
                "UNIQUE(user_id, product_id, order_id), " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE app_session (" +
                "id INTEGER PRIMARY KEY CHECK(id = 1), " +
                "user_id INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL)");

        db.execSQL("CREATE INDEX idx_products_category ON products(category_id)");
        db.execSQL("CREATE INDEX idx_cart_user ON cart_items(user_id)");
        db.execSQL("CREATE INDEX idx_orders_user_status ON orders(user_id, status)");
        db.execSQL("CREATE INDEX idx_reviews_product ON reviews(product_id)");

        seedDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS reviews");
        db.execSQL("DROP TABLE IF EXISTS order_details");
        db.execSQL("DROP TABLE IF EXISTS orders");
        db.execSQL("DROP TABLE IF EXISTS cart_items");
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS categories");
        db.execSQL("DROP TABLE IF EXISTS app_session");
        onCreate(db);
    }

    private void seedDatabase(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String now = now();

            long electronics = insertCategory(db, "Điện tử");
            long phones = insertCategory(db, "Điện thoại");
            long accessories = insertCategory(db, "Phụ kiện");
            long fashion = insertCategory(db, "Thời trang");
            long home = insertCategory(db, "Nhà cửa");

            long demo = insertUser(db, "UserDemo", "demo", "demo@shop.vn",
                    "demo123", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM",
                    DEFAULT_AVATAR_PATH);
            long p1 = insertProduct(db, phones, "iPhone 15 128GB", 22990000,
                    "iPhone 15 với chip A16 Bionic, màn hình OLED 6.1 inch và camera 48MP.", "assets/products/iphone_15.png", 25, 18);
            long p2 = insertProduct(db, electronics, "Laptop Dell Inspiron 14", 18990000,
                    "Laptop Dell Inspiron 14, Intel Core i5 thế hệ 13, RAM 16GB và SSD 512GB.", "assets/products/dell_inspiron_14.png", 12, 9);
            long p3 = insertProduct(db, accessories, "Tai nghe Sony WH-1000XM5", 8490000,
                    "Tai nghe chống ồn chủ động, pin đến 30 giờ và kết nối Bluetooth 5.2.", "assets/products/sony_wh1000xm5.png", 30, 24);
            long p4 = insertProduct(db, accessories, "Apple Watch Series 9", 10500000,
                    "Apple Watch GPS 45mm, theo dõi sức khỏe, thể thao và thông báo thông minh.", "assets/products/apple_watch_series_9.png", 18, 11);
            long p5 = insertProduct(db, electronics, "Bàn phím cơ Keychron K2", 2450000,
                    "Bàn phím cơ không dây 75%, switch Red, hỗ trợ Mac và Windows.", "assets/products/keychron_k2.png", 40, 16);
            long p6 = insertProduct(db, fashion, "Giày sneaker Urban Runner", 1590000,
                    "Giày sneaker nam nữ đế cao su nhẹ, thoáng khí và phù hợp đi hàng ngày.", "assets/products/urban_runner.png", 35, 27);
            long p7 = insertProduct(db, fashion, "Balo laptop Nomad 15", 690000,
                    "Balo chống nước, ngăn laptop 15.6 inch và nhiều ngăn phụ kiện.", "assets/products/nomad_backpack.png", 50, 32);
            long p8 = insertProduct(db, electronics, "Máy ảnh Fujifilm X-T30 II", 24990000,
                    "Máy ảnh mirrorless APS-C, quay 4K và khả năng bắt nét nhanh.", "assets/products/fujifilm_xt30ii.png", 8, 5);
            long p9 = insertProduct(db, accessories, "Chuột Logitech MX Master 3S", 2650000,
                    "Chuột không dây cao cấp, cuộn MagSpeed, kết nối 3 thiết bị.", "assets/products/logitech_mx_master_3s.png", 22, 20);
            long p10 = insertProduct(db, home, "Đèn bàn Lumos LED", 780000,
                    "Đèn bàn LED chống cận, 3 chế độ ánh sáng và điều chỉnh độ sáng.", "assets/products/lumos_led_lamp.png", 45, 14);

            insertCartItem(db, demo, p3, 1);
            insertCartItem(db, demo, p5, 2);
            insertCartItem(db, demo, p7, 1);

            long orderDelivering = insertOrder(db, demo, now, STATUS_DELIVERING, 3230000,
                    "UserDemo", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM", "Giao giờ hành chính");
            insertOrderDetail(db, orderDelivering, p5, 1, 2450000);
            insertOrderDetail(db, orderDelivering, p10, 1, 780000);

            long orderReceived1 = insertOrder(db, demo, "2026-11-17 09:30:00", STATUS_RECEIVED, 50470000,
                    "UserDemo", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM", "Giao tại nhà");
            insertOrderDetail(db, orderReceived1, p1, 1, 22990000);
            insertOrderDetail(db, orderReceived1, p2, 1, 18990000);
            insertOrderDetail(db, orderReceived1, p3, 1, 8490000);

            long orderReceived2 = insertOrder(db, demo, "2026-11-15 10:30:00", STATUS_RECEIVED, 21140000,
                    "UserDemo", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM", "Để tại sảnh");
            insertOrderDetail(db, orderReceived2, p4, 1, 10500000);
            insertOrderDetail(db, orderReceived2, p5, 1, 2450000);
            insertOrderDetail(db, orderReceived2, p6, 1, 1590000);
            insertOrderDetail(db, orderReceived2, p7, 1, 690000);

            long orderReceived3 = insertOrder(db, demo, "2026-11-10 14:15:00", STATUS_RECEIVED, 28420000,
                    "UserDemo", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM", "Giao buoi toi");
            insertOrderDetail(db, orderReceived3, p8, 1, 24990000);
            insertOrderDetail(db, orderReceived3, p9, 1, 2650000);
            insertOrderDetail(db, orderReceived3, p10, 1, 780000);

            long orderCancelled = insertOrder(db, demo, "2026-11-05 09:00:00", STATUS_CANCELLED, 1590000,
                    "UserDemo", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM", "Hủy do đổi địa chỉ");
            insertOrderDetail(db, orderCancelled, p6, 1, 1590000);

            insertReview(db, demo, p1, orderReceived1, 5,
                    "iPhone chạy mượt, màn hình đẹp và pin dùng được cả ngày.", "2026-11-19 08:30:00");
            insertReview(db, demo, p2, orderReceived1, 4,
                    "Laptop Dell cấu hình ổn, phù hợp học tập và làm việc văn phòng.", "2026-11-19 09:10:00");
            insertReview(db, demo, p3, orderReceived1, 5,
                    "Chống ồn tốt, âm thanh rõ và đeo lâu không bị đau tai.", "2026-11-19 09:40:00");
            insertReview(db, demo, p4, orderReceived2, 4,
                    "Apple Watch theo dõi thể thao và thông báo tiện lợi, pin khá ổn.", "2026-11-16 10:20:00");
            insertReview(db, demo, p5, orderReceived2, 3,
                    "Bàn phím gõ sướng, kết nối ổn nhưng phần đèn nền có thể sáng hơn.", "2026-11-16 11:05:00");
            insertReview(db, demo, p6, orderReceived2, 5,
                    "Giày nhẹ và vừa chân, đế êm nên đi cả ngày vẫn thoải mái.", "2026-11-16 11:35:00");
            insertReview(db, demo, p7, orderReceived2, 4,
                    "Balo chắc chắn, nhiều ngăn và chống nước khá tốt.", "2026-11-16 12:10:00");
            insertReview(db, demo, p8, orderReceived3, 5,
                    "Máy ảnh nhỏ gọn, hình ảnh sắc nét và thao tác bắt nét nhanh.", "2026-11-12 08:20:00");
            insertReview(db, demo, p9, orderReceived3, 4,
                    "Chuột lướt mượt, kết nối ổn định và sử dụng pin lâu.", "2026-11-12 09:05:00");
            insertReview(db, demo, p10, orderReceived3, 3,
                    "Đèn sáng đều, có nhiều mức ánh sáng nhưng chân đế cần chắc hơn.", "2026-11-12 09:35:00");

            recalculateProductRating(db, p1);
            recalculateProductRating(db, p2);
            recalculateProductRating(db, p3);
            recalculateProductRating(db, p4);
            recalculateProductRating(db, p5);
            recalculateProductRating(db, p6);
            recalculateProductRating(db, p7);
            recalculateProductRating(db, p8);
            recalculateProductRating(db, p9);
            recalculateProductRating(db, p10);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private long insertCategory(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        return db.insert(TABLE_CATEGORIES, null, values);
    }

    private long insertUser(SQLiteDatabase db, String displayName, String username, String email,
                            String password, String phone, String address, String avatarPath) {
        ContentValues values = new ContentValues();
        values.put("display_name", displayName);
        values.put("username", username);
        values.put("email", email);
        values.put("password_hash", hashPassword(password));
        values.put("phone", phone);
        values.put("address", address);
        values.put("avatar_path", avatarPath);
        return db.insert(TABLE_USERS, null, values);
    }

    private long insertProduct(SQLiteDatabase db, long categoryId, String name, double price,
                               String description, String imagePath, int stock, int sold) {
        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("name", name);
        values.put("price", price);
        values.put("description", description);
        values.put("image_path", imagePath);
        values.put("stock_quantity", stock);
        values.put("sold_quantity", sold);
        return db.insert(TABLE_PRODUCTS, null, values);
    }

    private void insertCartItem(SQLiteDatabase db, long userId, long productId, int quantity) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("product_id", productId);
        values.put("quantity", quantity);
        db.insert(TABLE_CART_ITEMS, null, values);
    }

    private long insertOrder(SQLiteDatabase db, long userId, String orderDate, String status,
                             double total, String receiverName, String phone, String address,
                             String note) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("order_date", orderDate);
        values.put("status", status);
        values.put("total_amount", total);
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

    private void insertReview(SQLiteDatabase db, long userId, long productId, long orderId,
                              int rating, String comment, String createdAt) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("product_id", productId);
        values.put("order_id", orderId);
        values.put("rating", rating);
        values.put("comment", comment);
        values.put("created_at", createdAt);
        db.insert(TABLE_REVIEWS, null, values);
    }

    private void recalculateProductRating(SQLiteDatabase db, long productId) {
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

    public boolean registerUser(String displayName, String username, String email, String password,
                                String phone, String address, String avatarPath) {
        if (displayName == null || displayName.trim().isEmpty()
                || username == null || username.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
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

        SQLiteDatabase db = getReadableDatabase();
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

        SQLiteDatabase db = getWritableDatabase();
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

        SQLiteDatabase db = getWritableDatabase();
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

        SQLiteDatabase db = getWritableDatabase();
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
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("user_id", userId);
        db.insertWithOnConflict(TABLE_APP_SESSION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long getSessionUserId() {
        SQLiteDatabase db = getReadableDatabase();
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
        return getReadableDatabase().rawQuery(
                "SELECT * FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)});
    }

    public void logout() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.putNull("user_id");
        db.insertWithOnConflict(TABLE_APP_SESSION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getAllCategories() {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM categories ORDER BY name ASC", null);
    }

    public Cursor getAllProducts() {
        return getReadableDatabase().rawQuery(
                "SELECT p.*, c.name AS category_name FROM products p " +
                        "JOIN categories c ON c.id = p.category_id ORDER BY p.id ASC",
                null);
    }

    public Cursor getProductById(long productId) {
        return getReadableDatabase().rawQuery(
                "SELECT p.*, c.name AS category_name FROM products p " +
                        "JOIN categories c ON c.id = p.category_id WHERE p.id = ?",
                new String[]{String.valueOf(productId)});
    }

    public Cursor getProductReviews(long productId) {
        return getProductReviews(productId, 0);
    }

    public Cursor getProductReviews(long productId, int starFilter) {
        if (starFilter <= 0) {
            return getReadableDatabase().rawQuery(
                    "SELECT r.*, u.display_name, u.avatar_path " +
                            "FROM reviews r JOIN users u ON u.id = r.user_id " +
                            "WHERE r.product_id = ? ORDER BY r.created_at DESC",
                    new String[]{String.valueOf(productId)});
        }
        return getReadableDatabase().rawQuery(
                "SELECT r.*, u.display_name, u.avatar_path " +
                        "FROM reviews r JOIN users u ON u.id = r.user_id " +
                        "WHERE r.product_id = ? AND r.rating = ? ORDER BY r.created_at DESC",
                new String[]{String.valueOf(productId), String.valueOf(starFilter)});
    }

    public int[] getProductRatingCounts(long productId) {
        int[] counts = new int[6];
        Cursor cursor = getReadableDatabase().rawQuery(
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

    public boolean addToCart(long productId, int quantity) {
        long userId = getSessionUserId();
        if (userId < 0 || productId <= 0 || quantity <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        Cursor product = db.rawQuery(
                "SELECT stock_quantity FROM products WHERE id = ?",
                new String[]{String.valueOf(productId)});
        if (!product.moveToFirst()) {
            product.close();
            return false;
        }
        int stock = product.getInt(0);
        product.close();

        if (stock <= 0) {
            return false;
        }

        Cursor cartItem = db.rawQuery(
                "SELECT id, quantity FROM cart_items WHERE user_id = ? AND product_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(productId)});
        int existingQuantity = cartItem.moveToFirst() ? cartItem.getInt(1) : 0;
        cartItem.close();

        int finalQuantity = existingQuantity + quantity;
        if (finalQuantity > stock) {
            finalQuantity = stock;
        }

        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("product_id", productId);
        values.put("quantity", finalQuantity);
        if (existingQuantity > 0) {
            return db.update(TABLE_CART_ITEMS, values,
                    "user_id = ? AND product_id = ?",
                    new String[]{String.valueOf(userId), String.valueOf(productId)}) == 1;
        }
        return db.insert(TABLE_CART_ITEMS, null, values) != -1;
    }

    public int getCartItemCount() {
        long userId = getSessionUserId();
        if (userId < 0) {
            return 0;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    public static final long ORDER_FAILED = -1;       // giỏ trống / chưa đăng nhập / lỗi
    public static final long ORDER_OUT_OF_STOCK = -2; // có sản phẩm vượt quá tồn kho

    // ---------- GIỎ HÀNG ----------

    /** Danh sách sản phẩm trong giỏ của user hiện tại.
     *  Cột: cart_id, product_id, quantity, name, price, image_path, stock_quantity, subtotal */
    public Cursor getCartItems() {
        long userId = getSessionUserId();
        return getReadableDatabase().rawQuery(
                "SELECT c.id AS cart_id, c.product_id, c.quantity, p.name, p.price, " +
                        "p.image_path, p.stock_quantity, (c.quantity * p.price) AS subtotal " +
                        "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                        "WHERE c.user_id = ? ORDER BY c.id ASC",
                new String[]{String.valueOf(userId)});
    }

    /** Tổng tiền toàn bộ giỏ hàng. */
    public double getCartTotal() {
        long userId = getSessionUserId();
        if (userId < 0) {
            return 0;
        }
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(c.quantity * p.price), 0) " +
                        "FROM cart_items c JOIN products p ON p.id = c.product_id " +
                        "WHERE c.user_id = ?",
                new String[]{String.valueOf(userId)});
        double total = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return total;
    }

    /** Đặt số lượng mới cho 1 dòng trong giỏ. Trả về false nếu <= 0 hoặc vượt tồn kho. */
    public boolean updateCartQuantity(long cartId, int newQuantity) {
        long userId = getSessionUserId();
        if (userId < 0 || newQuantity <= 0) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
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

    /** Xóa 1 sản phẩm khỏi giỏ. */
    public boolean removeCartItem(long cartId) {
        long userId = getSessionUserId();
        if (userId < 0) {
            return false;
        }
        return getWritableDatabase().delete(TABLE_CART_ITEMS, "id = ? AND user_id = ?",
                new String[]{String.valueOf(cartId), String.valueOf(userId)}) == 1;
    }

    // ---------- ĐẶT HÀNG ----------

    /**
     * Đặt toàn bộ giỏ hàng. Trong 1 transaction:
     * tạo đơn (Đang giao) + chi tiết đơn, trừ tồn kho, xóa giỏ.
     * Trả về id đơn mới, hoặc ORDER_FAILED / ORDER_OUT_OF_STOCK.
     */
    public long placeOrder(String receiverName, String phone, String address, String note) {
        long userId = getSessionUserId();
        if (userId < 0
                || receiverName == null || receiverName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()
                || address == null || address.trim().isEmpty()) {
            return ORDER_FAILED;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<double[]> items = new ArrayList<>(); // {productId, quantity, price}
            double total = 0;
            boolean enoughStock = true;

            Cursor cursor = db.rawQuery(
                    "SELECT ci.product_id, ci.quantity, p.price, p.stock_quantity " +
                            "FROM cart_items ci JOIN products p ON p.id = ci.product_id " +
                            "WHERE ci.user_id = ?",
                    new String[]{String.valueOf(userId)});
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

            if (items.isEmpty()) {
                return ORDER_FAILED;
            }
            if (!enoughStock) {
                return ORDER_OUT_OF_STOCK;
            }

            long orderId = insertOrder(db, userId, now(), STATUS_DELIVERING, total,
                    receiverName.trim(), phone.trim(), address.trim(),
                    note == null ? "" : note.trim());
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

            db.delete(TABLE_CART_ITEMS, "user_id = ?", new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
            return orderId;
        } finally {
            db.endTransaction();
        }
    }

    // ---------- ĐƠN HÀNG ----------

    /** Danh sách đơn của user hiện tại. status = null thì lấy tất cả.
     *  Dùng STATUS_DELIVERING / STATUS_RECEIVED / STATUS_CANCELLED để lọc.
     *  Cột: id, order_date, status, total_amount, receiver_name, phone, address, note */
    public Cursor getOrdersByStatus(String status) {
        long userId = getSessionUserId();
        if (status == null) {
            return getReadableDatabase().rawQuery(
                    "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC, id DESC",
                    new String[]{String.valueOf(userId)});
        }
        return getReadableDatabase().rawQuery(
                "SELECT * FROM orders WHERE user_id = ? AND status = ? " +
                        "ORDER BY order_date DESC, id DESC",
                new String[]{String.valueOf(userId), status});
    }

    /** Chi tiết các món của 1 đơn.
     *  Cột: id, order_id, product_id, quantity, unit_price, subtotal, name, image_path */
    public Cursor getOrderDetails(long orderId) {
        return getReadableDatabase().rawQuery(
                "SELECT od.*, p.name, p.image_path FROM order_details od " +
                        "JOIN products p ON p.id = od.product_id " +
                        "WHERE od.order_id = ? ORDER BY od.id ASC",
                new String[]{String.valueOf(orderId)});
    }

    /** Chuỗi tóm tắt món của đơn, mỗi món 1 dòng: "• Tên x2". */
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

    /** Nút "Đã nhận hàng": Đang giao -> Đã nhận hàng. */
    public boolean markOrderReceived(long orderId) {
        long userId = getSessionUserId();
        if (userId < 0) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("status", STATUS_RECEIVED);
        return getWritableDatabase().update(TABLE_ORDERS, values,
                "id = ? AND user_id = ? AND status = ?",
                new String[]{String.valueOf(orderId), String.valueOf(userId),
                        STATUS_DELIVERING}) == 1;
    }

    /** Nút "Hủy đơn": Đang giao -> Đã hủy và cộng hoàn tồn kho. */
    public boolean cancelOrder(long orderId) {
        long userId = getSessionUserId();
        if (userId < 0) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("status", STATUS_CANCELLED);
            int updated = db.update(TABLE_ORDERS, values,
                    "id = ? AND user_id = ? AND status = ?",
                    new String[]{String.valueOf(orderId), String.valueOf(userId),
                            STATUS_DELIVERING});
            if (updated != 1) {
                return false;
            }

            List<long[]> items = new ArrayList<>(); // {productId, quantity}
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

    // ---------- ĐÁNH GIÁ ----------

    /** Sản phẩm thuộc đơn "Đã nhận hàng" mà user chưa đánh giá.
     *  Cột: product_id, order_id, name, image_path, order_date */
    public Cursor getUnreviewedItems() {
        long userId = getSessionUserId();
        return getReadableDatabase().rawQuery(
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

    /** Các đánh giá user đã gửi.
     *  Cột: id, product_id, order_id, rating, comment, created_at, name, image_path */
    public Cursor getMyReviews() {
        long userId = getSessionUserId();
        return getReadableDatabase().rawQuery(
                "SELECT r.*, p.name, p.image_path FROM reviews r " +
                        "JOIN products p ON p.id = r.product_id " +
                        "WHERE r.user_id = ? ORDER BY r.created_at DESC",
                new String[]{String.valueOf(userId)});
    }

    /** Gửi đánh giá (1-5 sao). Chỉ cho phép với sản phẩm thuộc đơn "Đã nhận hàng" của user. */
    public boolean addReview(long productId, long orderId, int rating, String comment) {
        long userId = getSessionUserId();
        if (userId < 0 || rating < 1 || rating > 5) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
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
            values.put("created_at", now());
            if (db.insert(TABLE_REVIEWS, null, values) == -1) {
                return false; // trùng (đã đánh giá rồi)
            }

            recalculateProductRating(db, productId);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private static String hashPassword(String password) {
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

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
}
