package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends BaseAdapter {

    public static class OrderItem {
        public long id;
        public String orderDate;
        public String status;
        public double total;
        public String itemsSummary;
        public String cancelReason;
        public List<OrderProduct> products = new ArrayList<>();
    }

    public static class OrderProduct {
        public long productId;
        public long orderId;
        public int quantity;
        public String name;
        public String imagePath;
        public int rating;
        public String comment;
    }

    public interface Listener {
        void onReceived(OrderItem order);

        void onCancel(OrderItem order);
    }

    public interface DetailsListener {
        void onViewDetails(OrderItem order);
    }

    public interface ProductActionsListener {
        void onReviewProduct(OrderProduct product);

        void onViewProduct(OrderProduct product);
    }

    private final Context context;
    private final List<OrderItem> orders;
    private final boolean showActions;
    private final Listener listener;
    private final DetailsListener detailsListener;
    private final ProductActionsListener productActionsListener;
    private final LayoutInflater inflater;
    private final DecimalFormat priceFormat;

    public OrderAdapter(Context context, List<OrderItem> orders,
                        boolean showActions, Listener listener,
                        DetailsListener detailsListener) {
        this(context, orders, showActions, listener, detailsListener, null);
    }

    public OrderAdapter(Context context, List<OrderItem> orders,
                        boolean showActions, Listener listener,
                        DetailsListener detailsListener,
                        ProductActionsListener productActionsListener) {
        this.context = context;
        this.orders = orders;
        this.showActions = showActions;
        this.listener = listener;
        this.detailsListener = detailsListener;
        this.productActionsListener = productActionsListener;
        this.inflater = LayoutInflater.from(context);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        priceFormat = new DecimalFormat("#,##0", symbols);
    }

    @Override
    public int getCount() {
        return orders.size();
    }

    @Override
    public OrderItem getItem(int position) {
        return orders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return orders.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_order, parent, false);
            holder = new ViewHolder();
            holder.orderId = convertView.findViewById(R.id.tvOrderId);
            holder.status = convertView.findViewById(R.id.tvOrderStatus);
            holder.date = convertView.findViewById(R.id.tvOrderDate);
            holder.cancelReason = convertView.findViewById(R.id.tvOrderCancelReason);
            holder.items = convertView.findViewById(R.id.tvOrderItems);
            holder.productsBlock = convertView.findViewById(R.id.llOrderProductsBlock);
            holder.products = convertView.findViewById(R.id.llOrderProducts);
            holder.total = convertView.findViewById(R.id.tvOrderTotal);
            holder.actions = convertView.findViewById(R.id.llOrderActions);
            holder.btnReceived = convertView.findViewById(R.id.btnOrderReceived);
            holder.btnCancel = convertView.findViewById(R.id.btnOrderCancel);
            holder.btnDetails = convertView.findViewById(R.id.btnOrderDetails);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        OrderItem order = getItem(position);
        holder.orderId.setText("Đơn hàng #" + order.id);
        holder.status.setText(order.status);
        holder.status.setTextColor(statusColor(order.status));
        holder.status.setBackgroundResource(
                DatabaseHelper.STATUS_CANCELLED.equals(order.status)
                        ? R.drawable.bg_chip_red : R.drawable.bg_chip_blue);
        holder.date.setText("Ngày đặt: " + formatDate(order.orderDate));
        boolean cancelled = DatabaseHelper.STATUS_CANCELLED.equals(order.status)
                && order.cancelReason != null && !order.cancelReason.trim().isEmpty();
        holder.cancelReason.setVisibility(cancelled ? View.VISIBLE : View.GONE);
        if (cancelled) {
            holder.cancelReason.setText(context.getString(
                    R.string.order_cancel_reason, order.cancelReason));
        }
        holder.items.setVisibility(order.products.isEmpty() ? View.VISIBLE : View.GONE);
        holder.items.setText(order.itemsSummary);
        holder.total.setText(priceFormat.format(order.total) + " ₫");
        bindProducts(holder, order);

        if (detailsListener == null) {
            holder.btnDetails.setVisibility(View.GONE);
            holder.btnDetails.setOnClickListener(null);
        } else {
            holder.btnDetails.setVisibility(View.VISIBLE);
            holder.btnDetails.setOnClickListener(
                    v -> detailsListener.onViewDetails(order));
        }

        boolean canAct = showActions && listener != null
                && DatabaseHelper.STATUS_DELIVERING.equals(order.status);
        holder.actions.setVisibility(canAct ? View.VISIBLE : View.GONE);
        if (canAct) {
            holder.btnReceived.setOnClickListener(v -> listener.onReceived(order));
            holder.btnCancel.setOnClickListener(v -> listener.onCancel(order));
        }
        return convertView;
    }

    private void bindProducts(ViewHolder holder, OrderItem order) {
        holder.products.removeAllViews();
        boolean hasProducts = !order.products.isEmpty();
        holder.productsBlock.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        if (!hasProducts) {
            return;
        }

        boolean received = DatabaseHelper.STATUS_RECEIVED.equals(order.status);
        boolean showReviewActions = received && productActionsListener != null;
        for (OrderProduct product : order.products) {
            View row = inflater.inflate(
                    R.layout.item_order_product, holder.products, false);
            TextView name = row.findViewById(R.id.tvOrderProductName);
            TextView reviewSummary = row.findViewById(R.id.tvOrderProductReview);
            TextView reviewButton = row.findViewById(R.id.btnOrderProductReview);
            ImageView productImage = row.findViewById(R.id.ivOrderProductImage);
            View productButton = row.findViewById(R.id.btnOrderProductDetails);
            View productActions = row.findViewById(R.id.llOrderProductActions);
            View divider = row.findViewById(R.id.viewOrderProductDivider);
            divider.setVisibility(holder.products.getChildCount() == 0
                    ? View.GONE : View.VISIBLE);
            Bitmap bitmap = AssetImageLoader.load(context, product.imagePath);
            if (bitmap == null) {
                productImage.setImageResource(R.drawable.ic_product_image_placeholder);
            } else {
                productImage.setImageBitmap(bitmap);
            }
            name.setText(context.getString(R.string.order_review_product_quantity,
                    product.name, product.quantity));

            boolean reviewed = product.rating > 0;
            if (showReviewActions && reviewed) {
                reviewSummary.setText(context.getString(R.string.order_product_reviewed_status,
                        buildStars(product.rating)));
                reviewSummary.setTextColor(Color.parseColor("#B45309"));
            } else if (showReviewActions) {
                reviewSummary.setText(R.string.review_status_pending);
                reviewSummary.setTextColor(
                        ContextCompat.getColor(context, R.color.text_secondary));
            }
            reviewSummary.setVisibility(showReviewActions ? View.VISIBLE : View.GONE);
            productActions.setVisibility(showReviewActions ? View.VISIBLE : View.GONE);
            if (showReviewActions) {
                reviewButton.setText(reviewed
                        ? R.string.review_view_edit : R.string.review_write);
                reviewButton.setOnClickListener(v ->
                        productActionsListener.onReviewProduct(product));
                productButton.setOnClickListener(v ->
                        productActionsListener.onViewProduct(product));
            }
            holder.products.addView(row);
        }
    }

    private String buildStars(int rating) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            builder.append(i <= rating ? "★" : "☆");
        }
        return builder.toString();
    }

    private int statusColor(String status) {
        if (DatabaseHelper.STATUS_RECEIVED.equals(status)) {
            return Color.parseColor("#0D9488");
        }
        if (DatabaseHelper.STATUS_CANCELLED.equals(status)) {
            return Color.parseColor("#EF4444");
        }
        return ContextCompat.getColor(context, R.color.primary);
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
        TextView orderId;
        TextView status;
        TextView date;
        TextView cancelReason;
        TextView items;
        View productsBlock;
        LinearLayout products;
        TextView total;
        View actions;
        View btnReceived;
        View btnCancel;
        View btnDetails;
    }
}
