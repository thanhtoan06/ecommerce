package com.example.ecommerce.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ecommerce.data.repository.AccountRepository;
import com.example.ecommerce.data.repository.CartRepository;
import com.example.ecommerce.data.repository.OrderRepository;
import com.example.ecommerce.data.repository.ProductRepository;
import com.example.ecommerce.data.repository.ReviewRepository;

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
    public static final String PAYMENT_METHOD_COD = "COD";
    public static final double SHIPPING_FEE = 30000;
    public static final String DEFAULT_AVATAR_PATH = "ic_avatar_default";

    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        accountRepository = new AccountRepository(this);
        productRepository = new ProductRepository(this);
        cartRepository = new CartRepository(this);
        orderRepository = new OrderRepository(this);
        reviewRepository = new ReviewRepository(this);
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
                "shipping_fee REAL NOT NULL DEFAULT 0 CHECK(shipping_fee >= 0), " +
                "payment_method TEXT NOT NULL DEFAULT 'COD', " +
                "receiver_name TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "address TEXT NOT NULL, " +
                "note TEXT, " +
                "cancel_reason TEXT, " +
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

        new DatabaseSeeder().seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            if (!hasColumn(db, TABLE_ORDERS, "shipping_fee")) {
                db.execSQL("ALTER TABLE orders ADD COLUMN shipping_fee " +
                        "REAL NOT NULL DEFAULT 0");
            }
            if (!hasColumn(db, TABLE_ORDERS, "payment_method")) {
                db.execSQL("ALTER TABLE orders ADD COLUMN payment_method " +
                        "TEXT NOT NULL DEFAULT 'COD'");
            }
        }
        if (oldVersion < 3 && !hasColumn(db, TABLE_ORDERS, "cancel_reason")) {
            db.execSQL("ALTER TABLE orders ADD COLUMN cancel_reason TEXT");
        }
    }

    private boolean hasColumn(SQLiteDatabase db, String tableName, String columnName) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(1))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final long ORDER_FAILED = -1;
    public static final long ORDER_OUT_OF_STOCK = -2;

    public boolean registerUser(String displayName, String username, String email, String password,
                                String phone, String address, String avatarPath) {
        return accountRepository.registerUser(displayName, username, email, password,
                phone, address, avatarPath);
    }

    public long loginUser(String identity, String password) {
        return accountRepository.loginUser(identity, password);
    }

    public PasswordResetResult resetPassword(
            String identity, String phone, String newPassword) {
        return accountRepository.resetPassword(identity, phone, newPassword);
    }

    public boolean updateCurrentUserProfile(String displayName, String phone,
                                            String address, String avatarPath) {
        return accountRepository.updateCurrentUserProfile(
                displayName, phone, address, avatarPath);
    }

    public boolean changeCurrentUserPassword(String oldPassword,
                                             String newPassword) {
        return accountRepository.changeCurrentUserPassword(oldPassword, newPassword);
    }

    public void setSessionUser(long userId) {
        accountRepository.setSessionUser(userId);
    }

    public long getSessionUserId() {
        return accountRepository.getSessionUserId();
    }

    public Cursor getSessionUser() {
        return accountRepository.getSessionUser();
    }

    public void logout() {
        accountRepository.logout();
    }

    public Cursor getAllCategories() {
        return productRepository.getAllCategories();
    }

    public Cursor getAllProducts() {
        return productRepository.getAllProducts();
    }

    public Cursor getProductById(long productId) {
        return productRepository.getProductById(productId);
    }

    public int getAvailableCartQuantity(long productId) {
        return cartRepository.getAvailableCartQuantity(productId);
    }

    public boolean addToCart(long productId, int quantity) {
        return cartRepository.addToCart(productId, quantity);
    }

    public long addToCartAndGetCartId(long productId, int quantity) {
        return cartRepository.addToCartAndGetCartId(productId, quantity);
    }

    public int getCartItemCount() {
        return cartRepository.getCartItemCount();
    }

    public Cursor getCartItems() {
        return cartRepository.getCartItems();
    }

    public Cursor getCartItems(long[] cartIds) {
        return cartRepository.getCartItems(cartIds);
    }

    public double getCartTotal() {
        return cartRepository.getCartTotal();
    }

    public double getCartTotal(long[] cartIds) {
        return cartRepository.getCartTotal(cartIds);
    }

    public boolean updateCartQuantity(long cartId, int newQuantity) {
        return cartRepository.updateCartQuantity(cartId, newQuantity);
    }

    public boolean removeCartItem(long cartId) {
        return cartRepository.removeCartItem(cartId);
    }

    public boolean clearCart() {
        return cartRepository.clearCart();
    }

    public long placeOrder(String receiverName, String phone, String address, String note) {
        return orderRepository.placeOrder(receiverName, phone, address, note);
    }

    public long placeOrder(String receiverName, String phone, String address, String note,
                           long[] selectedCartIds) {
        return orderRepository.placeOrder(receiverName, phone, address, note, selectedCartIds);
    }

    public long placeOrder(String receiverName, String phone, String address, String note,
                           long[] selectedCartIds, double shippingFee, String paymentMethod) {
        return orderRepository.placeOrder(receiverName, phone, address, note,
                selectedCartIds, shippingFee, paymentMethod);
    }

    public Cursor getOrdersByStatus(String status) {
        return orderRepository.getOrdersByStatus(status);
    }

    public int getOrderCountByStatus(String status) {
        return orderRepository.getOrderCountByStatus(status);
    }

    public Cursor getOrderById(long orderId) {
        return orderRepository.getOrderById(orderId);
    }

    public Cursor getOrderDetails(long orderId) {
        return orderRepository.getOrderDetails(orderId);
    }

    public String getOrderItemsSummary(long orderId) {
        return orderRepository.getOrderItemsSummary(orderId);
    }

    public boolean markOrderReceived(long orderId) {
        return orderRepository.markOrderReceived(orderId);
    }

    public boolean cancelOrder(long orderId, String reason) {
        return orderRepository.cancelOrder(orderId, reason);
    }

    public Cursor getOrderProductsWithReviews(long orderId) {
        return orderRepository.getOrderProductsWithReviews(orderId);
    }

    public Cursor getProductReviews(long productId) {
        return reviewRepository.getProductReviews(productId);
    }

    public Cursor getProductReviews(long productId, int starFilter) {
        return reviewRepository.getProductReviews(productId, starFilter);
    }

    public int[] getProductRatingCounts(long productId) {
        return reviewRepository.getProductRatingCounts(productId);
    }

    public Cursor getUnreviewedItems() {
        return reviewRepository.getUnreviewedItems();
    }

    public Cursor getMyReviews() {
        return reviewRepository.getMyReviews();
    }

    public boolean saveReview(long productId, long orderId, int rating, String comment) {
        return reviewRepository.saveReview(productId, orderId, rating, comment);
    }
}
