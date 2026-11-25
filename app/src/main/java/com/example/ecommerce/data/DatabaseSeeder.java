package com.example.ecommerce.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.ecommerce.data.repository.AccountRepository;
import com.example.ecommerce.data.repository.ReviewRepository;

import static com.example.ecommerce.data.DatabaseHelper.*;

public final class DatabaseSeeder {
    public void seed(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String now = DatabaseUtils.now();

            long electronics = insertCategory(db, "Điện tử");
            long phones = insertCategory(db, "Điện thoại");
            long accessories = insertCategory(db, "Phụ kiện");
            long fashion = insertCategory(db, "Thời trang");
            long home = insertCategory(db, "Nhà cửa");

            long demo = insertUser(db, "UserDemo", "demo", "demo@shop.vn",
                    "demo123", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM",
                    DEFAULT_AVATAR_PATH);
            ContentValues initialSession = new ContentValues();
            initialSession.put("id", 1);
            initialSession.putNull("user_id");
            db.insert(TABLE_APP_SESSION, null, initialSession);

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

            long orderDelivering = insertOrder(db, demo, now, STATUS_DELIVERING, 3260000,
                    "UserDemo", "0901234567", "123 Nguyen Hue, Quan 1, TP.HCM",
                    "Giao giờ hành chính", SHIPPING_FEE, PAYMENT_METHOD_COD);
            insertOrderDetail(db, orderDelivering, p5, 1, 2450000);
            insertOrderDetail(db, orderDelivering, p10, 1, 780000);

            long orderReceived1 = insertOrder(db, demo, "2026-11-17 09:30:00",
                    STATUS_RECEIVED, 50500000, "UserDemo", "0901234567",
                    "123 Nguyen Hue, Quan 1, TP.HCM", "Giao tại nhà",
                    SHIPPING_FEE, PAYMENT_METHOD_COD);
            insertOrderDetail(db, orderReceived1, p1, 1, 22990000);
            insertOrderDetail(db, orderReceived1, p2, 1, 18990000);
            insertOrderDetail(db, orderReceived1, p3, 1, 8490000);

            long orderReceived2 = insertOrder(db, demo, "2026-11-15 10:30:00",
                    STATUS_RECEIVED, 15260000, "UserDemo", "0901234567",
                    "123 Nguyen Hue, Quan 1, TP.HCM", "Để tại sảnh",
                    SHIPPING_FEE, PAYMENT_METHOD_COD);
            insertOrderDetail(db, orderReceived2, p4, 1, 10500000);
            insertOrderDetail(db, orderReceived2, p5, 1, 2450000);
            insertOrderDetail(db, orderReceived2, p6, 1, 1590000);
            insertOrderDetail(db, orderReceived2, p7, 1, 690000);

            long orderReceived3 = insertOrder(db, demo, "2026-11-10 14:15:00",
                    STATUS_RECEIVED, 28450000, "UserDemo", "0901234567",
                    "123 Nguyen Hue, Quan 1, TP.HCM", "Giao buoi toi",
                    SHIPPING_FEE, PAYMENT_METHOD_COD);
            insertOrderDetail(db, orderReceived3, p8, 1, 24990000);
            insertOrderDetail(db, orderReceived3, p9, 1, 2650000);
            insertOrderDetail(db, orderReceived3, p10, 1, 780000);

            long orderCancelled = insertOrder(db, demo, "2026-11-05 09:00:00",
                    STATUS_CANCELLED, 1620000, "UserDemo", "0901234567",
                    "123 Nguyen Hue, Quan 1, TP.HCM", "",
                    SHIPPING_FEE, PAYMENT_METHOD_COD);
            ContentValues cancelReason = new ContentValues();
            cancelReason.put("cancel_reason", "Đổi ý, không còn nhu cầu mua sản phẩm");
            db.update(TABLE_ORDERS, cancelReason, "id = ?",
                    new String[]{String.valueOf(orderCancelled)});
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

            ReviewRepository.recalculateProductRating(db, p1);
            ReviewRepository.recalculateProductRating(db, p2);
            ReviewRepository.recalculateProductRating(db, p3);
            ReviewRepository.recalculateProductRating(db, p4);
            ReviewRepository.recalculateProductRating(db, p5);
            ReviewRepository.recalculateProductRating(db, p6);
            ReviewRepository.recalculateProductRating(db, p7);
            ReviewRepository.recalculateProductRating(db, p8);
            ReviewRepository.recalculateProductRating(db, p9);
            ReviewRepository.recalculateProductRating(db, p10);

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
        values.put("password_hash", AccountRepository.hashPassword(password));
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
}
