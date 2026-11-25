package com.example.ecommerce;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private final List<OrderAdapter.OrderItem> orders = new ArrayList<>();
    private OrderAdapter adapter;

    private ListView lvOrderHistory;
    private TextView tvHistoryEmpty;
    private TextView tvFilterAll;
    private TextView tvFilterDelivering;
    private TextView tvFilterReceived;
    private TextView tvFilterCancelled;

    /** null = Tất cả, còn lại là STATUS_DELIVERING / STATUS_RECEIVED / STATUS_CANCELLED */
    private String currentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        databaseHelper = new DatabaseHelper(this);
        lvOrderHistory = findViewById(R.id.lvOrderHistory);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        tvFilterAll = findViewById(R.id.tvFilterAll);
        tvFilterDelivering = findViewById(R.id.tvFilterDelivering);
        tvFilterReceived = findViewById(R.id.tvFilterReceived);
        tvFilterCancelled = findViewById(R.id.tvFilterCancelled);

        // Lịch sử chỉ để tra cứu: không hiện nút Đã nhận hàng / Hủy đơn
        adapter = new OrderAdapter(this, orders, false, null);
        lvOrderHistory.setAdapter(adapter);

        findViewById(R.id.tvBackHistory).setOnClickListener(v -> finish());
        tvFilterAll.setOnClickListener(v -> setFilter(null));
        tvFilterDelivering.setOnClickListener(v -> setFilter(DatabaseHelper.STATUS_DELIVERING));
        tvFilterReceived.setOnClickListener(v -> setFilter(DatabaseHelper.STATUS_RECEIVED));
        tvFilterCancelled.setOnClickListener(v -> setFilter(DatabaseHelper.STATUS_CANCELLED));

        updateFilterUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void setFilter(String status) {
        currentFilter = status;
        updateFilterUi();
        loadOrders();
    }

    private void updateFilterUi() {
        styleChip(tvFilterAll, currentFilter == null);
        styleChip(tvFilterDelivering, DatabaseHelper.STATUS_DELIVERING.equals(currentFilter));
        styleChip(tvFilterReceived, DatabaseHelper.STATUS_RECEIVED.equals(currentFilter));
        styleChip(tvFilterCancelled, DatabaseHelper.STATUS_CANCELLED.equals(currentFilter));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_chip_blue : android.R.color.transparent);
        chip.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.primary : R.color.text_secondary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void loadOrders() {
        orders.clear();
        Cursor cursor = databaseHelper.getOrdersByStatus(currentFilter);
        while (cursor.moveToNext()) {
            OrderAdapter.OrderItem order = new OrderAdapter.OrderItem();
            order.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            order.orderDate = cursor.getString(cursor.getColumnIndexOrThrow("order_date"));
            order.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            order.total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
            orders.add(order);
        }
        cursor.close();

        for (OrderAdapter.OrderItem order : orders) {
            order.itemsSummary = databaseHelper.getOrderItemsSummary(order.id);
        }
        adapter.notifyDataSetChanged();

        boolean empty = orders.isEmpty();
        tvHistoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvOrderHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
