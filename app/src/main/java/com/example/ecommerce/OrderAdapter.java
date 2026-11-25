package com.example.ecommerce;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends BaseAdapter {

    /** Một đơn hàng. */
    public static class OrderItem {
        public long id;
        public String orderDate;
        public String status;
        public double total;
        public String itemsSummary; // mỗi món 1 dòng
    }

    /** Dùng cho Tab Đơn hàng. Ở Lịch sử truyền null. */
    public interface Listener {
        void onReceived(OrderItem order);

        void onCancel(OrderItem order);
    }

    private final Context context;
    private final List<OrderItem> orders;
    private final boolean showActions;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final DecimalFormat priceFormat;

    /**
     * @param showActions true: hiện 2 nút Đã nhận hàng / Hủy đơn cho đơn "Đang giao"
     * @param listener    có thể null nếu showActions = false
     */
    public OrderAdapter(Context context, List<OrderItem> orders,
                        boolean showActions, Listener listener) {
        this.context = context;
        this.orders = orders;
        this.showActions = showActions;
        this.listener = listener;
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
            holder.items = convertView.findViewById(R.id.tvOrderItems);
            holder.total = convertView.findViewById(R.id.tvOrderTotal);
            holder.actions = convertView.findViewById(R.id.llOrderActions);
            holder.btnReceived = convertView.findViewById(R.id.btnOrderReceived);
            holder.btnCancel = convertView.findViewById(R.id.btnOrderCancel);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        OrderItem order = getItem(position);
        holder.orderId.setText("Đơn hàng #" + order.id);
        holder.status.setText(order.status);
        holder.status.setTextColor(statusColor(order.status));
        holder.date.setText("Ngày đặt: " + formatDate(order.orderDate));
        holder.items.setText(order.itemsSummary);
        holder.total.setText(priceFormat.format(order.total) + " ₫");

        boolean canAct = showActions && listener != null
                && DatabaseHelper.STATUS_DELIVERING.equals(order.status);
        holder.actions.setVisibility(canAct ? View.VISIBLE : View.GONE);
        if (canAct) {
            holder.btnReceived.setOnClickListener(v -> listener.onReceived(order));
            holder.btnCancel.setOnClickListener(v -> listener.onCancel(order));
        }
        return convertView;
    }

    private int statusColor(String status) {
        if (DatabaseHelper.STATUS_RECEIVED.equals(status)) {
            return Color.parseColor("#0D9488"); // xanh lục
        }
        if (DatabaseHelper.STATUS_CANCELLED.equals(status)) {
            return Color.parseColor("#EF4444"); // đỏ
        }
        return ContextCompat.getColor(context, R.color.primary); // Đang giao
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

    private static class ViewHolder {
        TextView orderId;
        TextView status;
        TextView date;
        TextView items;
        TextView total;
        View actions;
        View btnReceived;
        View btnCancel;
    }
}
