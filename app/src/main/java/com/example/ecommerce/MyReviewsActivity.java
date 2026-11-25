package com.example.ecommerce;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MyReviewsActivity extends AppCompatActivity implements ReviewAdapter.Listener {
    private DatabaseHelper databaseHelper;
    private final List<ReviewAdapter.ReviewRow> rows = new ArrayList<>();
    private ReviewAdapter adapter;

    private ListView lvReviews;
    private TextView tvReviewsEmpty;
    private TextView tvTabPending;
    private TextView tvTabDone;

    private boolean showingPending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        databaseHelper = new DatabaseHelper(this);
        lvReviews = findViewById(R.id.lvReviews);
        tvReviewsEmpty = findViewById(R.id.tvReviewsEmpty);
        tvTabPending = findViewById(R.id.tvTabPending);
        tvTabDone = findViewById(R.id.tvTabDone);

        adapter = new ReviewAdapter(this, rows, this);
        lvReviews.setAdapter(adapter);

        findViewById(R.id.tvBackReviews).setOnClickListener(v -> finish());
        tvTabPending.setOnClickListener(v -> switchTab(true));
        tvTabDone.setOnClickListener(v -> switchTab(false));

        updateTabUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRows();
    }

    private void switchTab(boolean pending) {
        showingPending = pending;
        updateTabUi();
        loadRows();
    }

    private void updateTabUi() {
        styleChip(tvTabPending, showingPending);
        styleChip(tvTabDone, !showingPending);
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_chip_blue : android.R.color.transparent);
        chip.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.primary : R.color.text_secondary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void loadRows() {
        rows.clear();
        adapter.setPendingMode(showingPending);

        if (showingPending) {
            Cursor cursor = databaseHelper.getUnreviewedItems();
            while (cursor.moveToNext()) {
                ReviewAdapter.ReviewRow row = new ReviewAdapter.ReviewRow();
                row.productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id"));
                row.orderId = cursor.getLong(cursor.getColumnIndexOrThrow("order_id"));
                row.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                row.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
                row.date = cursor.getString(cursor.getColumnIndexOrThrow("order_date"));
                rows.add(row);
            }
            cursor.close();
        } else {
            Cursor cursor = databaseHelper.getMyReviews();
            while (cursor.moveToNext()) {
                ReviewAdapter.ReviewRow row = new ReviewAdapter.ReviewRow();
                row.productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id"));
                row.orderId = cursor.getLong(cursor.getColumnIndexOrThrow("order_id"));
                row.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                row.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
                row.date = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
                row.rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating"));
                row.comment = cursor.getString(cursor.getColumnIndexOrThrow("comment"));
                rows.add(row);
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();

        boolean empty = rows.isEmpty();
        tvReviewsEmpty.setText(showingPending
                ? "Bạn không có sản phẩm nào cần đánh giá"
                : "Bạn chưa gửi đánh giá nào");
        tvReviewsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvReviews.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** Bấm nút "Đánh giá": hiện hộp thoại chọn sao + nhận xét. */
    @Override
    public void onReview(ReviewAdapter.ReviewRow row) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rate_product, null);
        ((TextView) view.findViewById(R.id.tvRateProductName)).setText(row.name);
        RatingBar ratingBar = view.findViewById(R.id.rbRateStars);
        EditText etComment = view.findViewById(R.id.etReviewComment);

        new AlertDialog.Builder(this)
                .setTitle("Đánh giá sản phẩm")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi đánh giá", (dialog, which) -> {
                    int stars = Math.max(1, Math.round(ratingBar.getRating()));
                    boolean saved = databaseHelper.addReview(
                            row.productId, row.orderId, stars,
                            etComment.getText().toString());
                    Toast.makeText(this,
                            saved ? "Cảm ơn bạn đã đánh giá" : "Không thể gửi đánh giá",
                            Toast.LENGTH_SHORT).show();
                    loadRows();
                })
                .show();
    }
}
