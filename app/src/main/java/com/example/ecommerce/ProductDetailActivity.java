package com.example.ecommerce;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private int selectedStarFilter = 0;

    private TextView tvQuantity;
    private TextView tvHeaderCartCount;

    private TextView tvRatingScore;
    private TextView tvRatingStars;
    private TextView tvRatingTotalCount;
    private ProgressBar progressStar5;
    private ProgressBar progressStar4;
    private ProgressBar progressStar3;
    private ProgressBar progressStar2;
    private ProgressBar progressStar1;
    private TextView tvCountStar5;
    private TextView tvCountStar4;
    private TextView tvCountStar3;
    private TextView tvCountStar2;
    private TextView tvCountStar1;

    private TextView btnFilterAll;
    private TextView btnFilter5Star;
    private TextView btnFilter4Star;
    private TextView btnFilter3Star;
    private TextView btnFilter2Star;
    private TextView btnFilter1Star;

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
        findViewById(R.id.btnHeaderCart).setOnClickListener(v -> openCartOrLogin());

        initReviewViews();
        setupReviewFilterButtons();

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

    private void initReviewViews() {
        tvRatingScore = findViewById(R.id.tvRatingScore);
        tvRatingStars = findViewById(R.id.tvRatingStars);
        tvRatingTotalCount = findViewById(R.id.tvRatingTotalCount);

        progressStar5 = findViewById(R.id.progressStar5);
        progressStar4 = findViewById(R.id.progressStar4);
        progressStar3 = findViewById(R.id.progressStar3);
        progressStar2 = findViewById(R.id.progressStar2);
        progressStar1 = findViewById(R.id.progressStar1);

        tvCountStar5 = findViewById(R.id.tvCountStar5);
        tvCountStar4 = findViewById(R.id.tvCountStar4);
        tvCountStar3 = findViewById(R.id.tvCountStar3);
        tvCountStar2 = findViewById(R.id.tvCountStar2);
        tvCountStar1 = findViewById(R.id.tvCountStar1);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilter5Star = findViewById(R.id.btnFilter5Star);
        btnFilter4Star = findViewById(R.id.btnFilter4Star);
        btnFilter3Star = findViewById(R.id.btnFilter3Star);
        btnFilter2Star = findViewById(R.id.btnFilter2Star);
        btnFilter1Star = findViewById(R.id.btnFilter1Star);
    }

    private void setupReviewFilterButtons() {
        btnFilterAll.setOnClickListener(v -> onFilterStarClicked(0));
        btnFilter5Star.setOnClickListener(v -> onFilterStarClicked(5));
        btnFilter4Star.setOnClickListener(v -> onFilterStarClicked(4));
        btnFilter3Star.setOnClickListener(v -> onFilterStarClicked(3));
        btnFilter2Star.setOnClickListener(v -> onFilterStarClicked(2));
        btnFilter1Star.setOnClickListener(v -> onFilterStarClicked(1));
    }

    private void onFilterStarClicked(int star) {
        if (selectedStarFilter == star) {
            return;
        }
        selectedStarFilter = star;
        updateFilterButtonStates();
        loadReviews();
    }

    private void updateFilterButtonStates() {
        TextView[] buttons = new TextView[]{btnFilterAll, btnFilter5Star, btnFilter4Star, btnFilter3Star, btnFilter2Star, btnFilter1Star};
        int[] stars = new int[]{0, 5, 4, 3, 2, 1};
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        int secondaryColor = ContextCompat.getColor(this, R.color.text_secondary);

        for (int i = 0; i < buttons.length; i++) {
            boolean isSelected = (stars[i] == selectedStarFilter);
            buttons[i].setBackgroundResource(isSelected ? R.drawable.bg_review_chip_selected : R.drawable.bg_review_chip_unselected);
            buttons[i].setTextColor(isSelected ? primaryColor : secondaryColor);
            buttons[i].setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void updateReviewStatistics(double averageRating) {
        int[] counts = databaseHelper.getProductRatingCounts(productId);
        int total = counts[1] + counts[2] + counts[3] + counts[4] + counts[5];

        tvRatingScore.setText(String.format(Locale.forLanguageTag("vi-VN"), "%.1f", averageRating));
        int starCount = (int) Math.round(averageRating);
        if (starCount < 0) starCount = 0;
        if (starCount > 5) starCount = 5;
        tvRatingStars.setText(stars(starCount));
        tvRatingTotalCount.setText(total + " đánh giá");

        tvCountStar5.setText(String.valueOf(counts[5]));
        tvCountStar4.setText(String.valueOf(counts[4]));
        tvCountStar3.setText(String.valueOf(counts[3]));
        tvCountStar2.setText(String.valueOf(counts[2]));
        tvCountStar1.setText(String.valueOf(counts[1]));

        progressStar5.setProgress(total > 0 ? (counts[5] * 100 / total) : 0);
        progressStar4.setProgress(total > 0 ? (counts[4] * 100 / total) : 0);
        progressStar3.setProgress(total > 0 ? (counts[3] * 100 / total) : 0);
        progressStar2.setProgress(total > 0 ? (counts[2] * 100 / total) : 0);
        progressStar1.setProgress(total > 0 ? (counts[1] * 100 / total) : 0);

        btnFilterAll.setText("Tất cả (" + total + ")");
        btnFilter5Star.setText("5 sao (" + counts[5] + ")");
        btnFilter4Star.setText("4 sao (" + counts[4] + ")");
        btnFilter3Star.setText("3 sao (" + counts[3] + ")");
        btnFilter2Star.setText("2 sao (" + counts[2] + ")");
        btnFilter1Star.setText("1 sao (" + counts[1] + ")");
        updateFilterButtonStates();
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

        updateReviewStatistics(rating);
    }

    private void loadReviews() {
        LinearLayout reviewContainer = findViewById(R.id.layoutProductReviewItems);
        TextView emptyView = findViewById(R.id.tvProductReviewListEmpty);
        reviewContainer.removeAllViews();

        Cursor cursor = databaseHelper.getProductReviews(productId, selectedStarFilter);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            if (selectedStarFilter > 0) {
                emptyView.setText("Chưa có đánh giá " + selectedStarFilter + " sao nào cho sản phẩm này.");
            } else {
                emptyView.setText("Chưa có đánh giá nào cho sản phẩm này.");
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
