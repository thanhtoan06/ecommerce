package com.example.ecommerce;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends BaseAdapter {

    public static class ReviewRow {
        public long productId;
        public long orderId;
        public String name;
        public String imagePath;
        public String date;
        public int rating;
        public String comment;
    }

    public interface Listener {
        void onReview(ReviewRow row);

        void onViewOrder(ReviewRow row);

        void onViewProduct(ReviewRow row);
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
            holder.btnOrderDetails = convertView.findViewById(R.id.btnReviewOrderDetails);
            holder.btnProductDetails = convertView.findViewById(R.id.btnReviewProductDetails);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ReviewRow row = getItem(position);
        holder.name.setText(row.name);

        holder.image.setImageResource(R.drawable.ic_product_image_placeholder);
        Bitmap bitmap = AssetImageLoader.load(context, row.imagePath);
        if (bitmap != null) {
            holder.image.setImageBitmap(bitmap);
        }

        if (pendingMode) {
            holder.meta.setText("Đơn hàng #" + row.orderId + " · " + formatDate(row.date));
            holder.doneBox.setVisibility(View.GONE);
        } else {
            holder.meta.setText("Đơn hàng #" + row.orderId
                    + " · Đánh giá ngày " + formatDate(row.date));
            holder.doneBox.setVisibility(View.VISIBLE);
            holder.stars.setText(buildStars(row.rating));
            boolean hasComment = row.comment != null && !row.comment.trim().isEmpty();
            holder.comment.setVisibility(hasComment ? View.VISIBLE : View.GONE);
            holder.comment.setText(hasComment ? row.comment : "");
        }
        holder.button.setVisibility(View.VISIBLE);
        holder.button.setText(pendingMode
                ? R.string.review_write : R.string.review_view_edit);
        holder.button.setOnClickListener(v -> listener.onReview(row));
        holder.btnOrderDetails.setOnClickListener(v -> listener.onViewOrder(row));
        holder.btnProductDetails.setOnClickListener(v -> listener.onViewProduct(row));
        return convertView;
    }

    private String buildStars(int rating) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            builder.append(i <= rating ? "★" : "☆");
        }
        return builder.toString();
    }

    private String formatDate(String raw) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(date);
        } catch (Exception e) {
            return raw;
        }
    }

    private static class ViewHolder {
        ImageView image;
        TextView name;
        TextView meta;
        TextView button;
        View doneBox;
        TextView stars;
        TextView comment;
        View btnOrderDetails;
        View btnProductDetails;
    }
}
