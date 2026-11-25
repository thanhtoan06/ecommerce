package com.example.ecommerce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends BaseAdapter {

    /** Một dòng: sản phẩm chưa đánh giá HOẶC đánh giá đã gửi. */
    public static class ReviewRow {
        public long productId;
        public long orderId;
        public String name;
        public String imagePath;
        public String date;     // ngày đặt (chưa đánh giá) hoặc ngày đánh giá (đã đánh giá)
        public int rating;      // chỉ dùng ở mục Đã đánh giá
        public String comment;  // chỉ dùng ở mục Đã đánh giá
    }

    public interface Listener {
        void onReview(ReviewRow row);
    }

    private final Context context;
    private final List<ReviewRow> rows;
    private final Listener listener;
    private final LayoutInflater inflater;
    private boolean pendingMode = true;

    public ReviewAdapter(Context context, List<ReviewRow> rows, Listener listener) {
        this.context = context;
        this.rows = rows;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    /** true: mục Chưa đánh giá (hiện nút Đánh giá). false: mục Đã đánh giá (hiện sao + nhận xét). */
    public void setPendingMode(boolean pendingMode) {
        this.pendingMode = pendingMode;
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public ReviewRow getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_review_row, parent, false);
            holder = new ViewHolder();
            holder.image = convertView.findViewById(R.id.ivReviewImage);
            holder.name = convertView.findViewById(R.id.tvReviewName);
            holder.meta = convertView.findViewById(R.id.tvReviewMeta);
            holder.button = convertView.findViewById(R.id.btnReviewNow);
            holder.doneBox = convertView.findViewById(R.id.llReviewDone);
            holder.stars = convertView.findViewById(R.id.tvReviewStars);
            holder.comment = convertView.findViewById(R.id.tvReviewComment);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ReviewRow row = getItem(position);
        holder.name.setText(row.name);

        holder.image.setImageResource(R.drawable.ic_product_image_placeholder);
        Bitmap bitmap = loadAssetImage(row.imagePath);
        if (bitmap != null) {
            holder.image.setImageBitmap(bitmap);
        }

        if (pendingMode) {
            holder.meta.setText("Đơn hàng #" + row.orderId + " · " + formatDate(row.date));
            holder.button.setVisibility(View.VISIBLE);
            holder.doneBox.setVisibility(View.GONE);
            holder.button.setOnClickListener(v -> listener.onReview(row));
        } else {
            holder.meta.setText("Đánh giá ngày " + formatDate(row.date));
            holder.button.setVisibility(View.GONE);
            holder.doneBox.setVisibility(View.VISIBLE);
            holder.stars.setText(buildStars(row.rating));
            boolean hasComment = row.comment != null && !row.comment.trim().isEmpty();
            holder.comment.setVisibility(hasComment ? View.VISIBLE : View.GONE);
            holder.comment.setText(hasComment ? row.comment : "");
        }
        return convertView;
    }

    private String buildStars(int rating) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            builder.append(i <= rating ? "★" : "☆");
        }
        return builder.toString();
    }

    /** "2026-11-17 09:30:00" -> "17/11/2026 09:30" */
    private String formatDate(String raw) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(date);
        } catch (Exception e) {
            return raw;
        }
    }

    // Giống hàm loadAssetImage trong ProductAdapter của Người 1
    private Bitmap loadAssetImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        String normalizedPath = imagePath
                .replace('\\', '/')
                .replaceFirst("^/+", "")
                .replaceFirst("^assets/", "");
        try (InputStream input = context.getAssets().open(normalizedPath)) {
            return BitmapFactory.decodeStream(input);
        } catch (Exception ignored) {
            try (InputStream input = context.getAssets().open(imagePath)) {
                return BitmapFactory.decodeStream(input);
            } catch (Exception finalError) {
                return null;
            }
        }
    }

    private static class ViewHolder {
        ImageView image;
        TextView name;
        TextView meta;
        View button;
        View doneBox;
        TextView stars;
        TextView comment;
    }
}
