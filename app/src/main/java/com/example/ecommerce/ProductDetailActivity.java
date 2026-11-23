package com.example.ecommerce;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private DatabaseHelper databaseHelper;
    private long productId;
    private int stockQuantity;
    private int quantity = 1;
    private TextView tvQuantity;
    private TextView tvHeaderCartCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        StatusBarUtils.setupHeader(this, findViewById(R.id.topBar));
        StatusBarUtils.setupBottomNav(this, findViewById(R.id.bottomActionBar));

        databaseHelper = new DatabaseHelper(this);
        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        if (productId < 0) {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvQuantity = findViewById(R.id.tvQuantity);
        tvHeaderCartCount = findViewById(R.id.tvHeaderCartCount);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnHeaderCart).setOnClickListener(v ->
                openCartOrLogin());

        loadProduct();
        loadReviews();
        updateCartCount();

        findViewById(R.id.btnDecrease).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });
        findViewById(R.id.btnIncrease).setOnClickListener(v -> {
            if (quantity < stockQuantity) {
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(this, "Đã đạt số lượng tồn kho tối đa",
                        Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> addToCart());
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            if (databaseHelper.getSessionUserId() < 0) {
                openLogin();
            } else {
                addToCart();
            }
        });
    }

    private void loadProduct() {
        Cursor cursor = databaseHelper.getProductById(productId);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
        String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
        stockQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity"));
        int soldQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("sold_quantity"));
        double rating = cursor.getDouble(cursor.getColumnIndexOrThrow("average_rating"));
        int reviewCount = cursor.getInt(cursor.getColumnIndexOrThrow("review_count"));
        String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
        cursor.close();

        TextView tvName = findViewById(R.id.tvName);
        TextView tvCategory = findViewById(R.id.tvCategory);
        TextView tvStock = findViewById(R.id.tvStock);
        TextView tvPrice = findViewById(R.id.tvPrice);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvAvgStar = findViewById(R.id.tvAvgStar);
        TextView tvReviewSummary = findViewById(R.id.tvProductReviewSummary);
        ImageView ivProduct = findViewById(R.id.ivProductImage);

        tvName.setText(name);
        tvCategory.setText(categoryName);
        tvStock.setText("Còn " + stockQuantity + " sp");
        tvPrice.setText(formatPrice(price) + " ₫");
        tvDescription.setText(description);
        tvAvgStar.setText(String.format(Locale.forLanguageTag("vi-VN"), "★ %.1f", rating));
        tvReviewSummary.setText("(" + reviewCount + " đánh giá) · Đã bán " + soldQuantity);

        Bitmap bitmap = loadAssetImage(imagePath);
        if (bitmap != null) {
            ivProduct.setImageBitmap(bitmap);
        } else {
            ivProduct.setImageResource(R.drawable.ic_product_image_placeholder);
        }

        if (stockQuantity <= 0) {
            quantity = 0;
            tvQuantity.setText("0");
            findViewById(R.id.btnDecrease).setEnabled(false);
            findViewById(R.id.btnIncrease).setEnabled(false);
            findViewById(R.id.btnAddToCart).setEnabled(false);
            findViewById(R.id.btnBuyNow).setEnabled(false);
        }
    }

    private void loadReviews() {
        LinearLayout reviewContainer = findViewById(R.id.layoutProductReviewItems);
        TextView emptyView = findViewById(R.id.tvProductReviewListEmpty);
        reviewContainer.removeAllViews();

        Cursor cursor = databaseHelper.getProductReviews(productId);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        do {
            View reviewView = inflater.inflate(R.layout.item_review_preview, reviewContainer, false);
            TextView tvName = reviewView.findViewById(R.id.tvReviewName);
            TextView tvRating = reviewView.findViewById(R.id.tvReviewRating);
            TextView tvComment = reviewView.findViewById(R.id.tvReviewComment);
            ImageView ivAvatar = reviewView.findViewById(R.id.ivReviewAvatar);
            String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
            String avatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
            int rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating"));
            tvName.setText(displayName + " · " + formatReviewDate(createdAt));
            tvRating.setText(stars(rating));
            String comment = cursor.getString(cursor.getColumnIndexOrThrow("comment"));
            tvComment.setText(comment == null || comment.trim().isEmpty()
                    ? "Người mua chưa để lại nhận xét."
                    : comment);
            loadReviewAvatar(ivAvatar, avatarPath);
            reviewContainer.addView(reviewView);
        } while (cursor.moveToNext());
        cursor.close();
    }

    private void addToCart() {
        if (databaseHelper.getSessionUserId() < 0) {
            openLogin();
            return;
        }
        if (quantity <= 0) {
            Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean added = databaseHelper.addToCart(productId, quantity);
        if (added) {
            updateCartCount();
            Toast.makeText(this, "Đã thêm " + quantity + " sản phẩm vào giỏ",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Không thể thêm sản phẩm vào giỏ",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCartCount() {
        tvHeaderCartCount.setText(String.valueOf(databaseHelper.getCartItemCount()));
    }

    private void loadReviewAvatar(ImageView imageView, String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()
                || DatabaseHelper.DEFAULT_AVATAR_PATH.equals(avatarPath)) {
            imageView.setImageResource(R.drawable.ic_avatar_default);
            return;
        }
        if (avatarPath.startsWith("avatars/")) {
            File file = new File(getFilesDir(), avatarPath);
            if (file.exists()) {
                imageView.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_avatar_default);
    }

    private void openCartOrLogin() {
        if (databaseHelper.getSessionUserId() < 0) {
            openLogin();
        } else {
            Toast.makeText(this,
                    "Giỏ hàng hiện có " + databaseHelper.getCartItemCount() + " sản phẩm",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_LOGIN_REQUIRED, true);
        startActivity(intent);
    }

    private Bitmap loadAssetImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        String normalized = imagePath.replace('\\', '/')
                .replaceFirst("^/+", "")
                .replaceFirst("^assets/", "");
        try {
            AssetManager assets = getAssets();
            try (InputStream input = assets.open(normalized)) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception firstError) {
            try (InputStream input = getAssets().open(imagePath)) {
                return BitmapFactory.decodeStream(input);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(price);
    }

    private String stars(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stars.append(i < rating ? "★" : "☆");
        }
        return stars.toString();
    }

    private String formatReviewDate(String createdAt) {
        if (createdAt == null || createdAt.length() < 10) {
            return "";
        }
        try {
            SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat target = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            return target.format(source.parse(createdAt.substring(0, 10)));
        } catch (Exception error) {
            return createdAt.substring(0, 10);
        }
    }
}
