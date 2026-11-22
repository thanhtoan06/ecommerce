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

public class DatabaseHelper extends SQLiteOpenHelper {
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
        values.put("username", username.trim().isEmpty() ? email.trim() : username.trim());
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

    public boolean resetPassword(String identity, String phone, String newPassword) {
        if (identity == null || identity.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()
                || newPassword == null || newPassword.length() < 6) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        String value = identity.trim();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE phone = ? AND (username = ? OR email = ?)",
                new String[]{phone.trim(), value, value});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }

        long userId = cursor.getLong(0);
        cursor.close();
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

    public Cursor searchProducts(String keyword) {
        if (keyword == null) {
            keyword = "";
        }
        String pattern = "%" + keyword.trim() + "%";
        return getReadableDatabase().rawQuery(
                "SELECT p.*, c.name AS category_name FROM products p " +
                        "JOIN categories c ON c.id = p.category_id " +
                        "WHERE p.name LIKE ? ESCAPE '\\' OR c.name LIKE ? ESCAPE '\\' " +
                        "ORDER BY p.name ASC",
                new String[]{pattern, pattern});
    }

    public Cursor getProductById(long productId) {
        return getReadableDatabase().rawQuery(
                "SELECT p.*, c.name AS category_name FROM products p " +
                        "JOIN categories c ON c.id = p.category_id WHERE p.id = ?",
                new String[]{String.valueOf(productId)});
    }

    public Cursor getProductReviews(long productId) {
        return getReadableDatabase().rawQuery(
                "SELECT r.*, u.display_name, u.avatar_path " +
                        "FROM reviews r JOIN users u ON u.id = r.user_id " +
                        "WHERE r.product_id = ? ORDER BY r.created_at DESC",
                new String[]{String.valueOf(productId)});
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
