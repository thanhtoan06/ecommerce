package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.database.Cursor;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity
        implements OrderAdapter.Listener, OrderAdapter.DetailsListener,
        OrderAdapter.ProductActionsListener {
    private DatabaseHelper databaseHelper;
    private final List<OrderAdapter.OrderItem> orders = new ArrayList<>();
    private OrderAdapter adapter;

    private ListView lvOrderHistory;
    private TextView tvHistoryEmpty;
    private TextView tvFilterAll;
    private TextView tvFilterDelivering;
    private TextView tvFilterReceived;
    private TextView tvFilterCancelled;
    private TextView tvHistoryCount;

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
        tvHistoryCount = findViewById(R.id.tvHistoryCount);

        adapter = new OrderAdapter(this, orders, true, this, this, this);
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
            order.cancelReason = cursor.getString(
                    cursor.getColumnIndexOrThrow("cancel_reason"));
            orders.add(order);
        }
        cursor.close();

        for (OrderAdapter.OrderItem order : orders) {
            order.itemsSummary = databaseHelper.getOrderItemsSummary(order.id);
            try (Cursor products = databaseHelper.getOrderProductsWithReviews(order.id)) {
                while (products.moveToNext()) {
                    OrderAdapter.OrderProduct product = new OrderAdapter.OrderProduct();
                    product.productId = products.getLong(
                            products.getColumnIndexOrThrow("product_id"));
                    product.orderId = products.getLong(
                            products.getColumnIndexOrThrow("order_id"));
                    product.quantity = products.getInt(
                            products.getColumnIndexOrThrow("quantity"));
                    product.name = products.getString(
                            products.getColumnIndexOrThrow("name"));
                    product.imagePath = products.getString(
                            products.getColumnIndexOrThrow("image_path"));
                    product.rating = products.getInt(
                            products.getColumnIndexOrThrow("rating"));
                    product.comment = products.getString(
                            products.getColumnIndexOrThrow("comment"));
                    order.products.add(product);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateHistoryCount(orders.size());

        boolean empty = orders.isEmpty();
        tvHistoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvOrderHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateHistoryCount(int count) {
        int labelResource = R.string.order_history_count_all;
        if (DatabaseHelper.STATUS_DELIVERING.equals(currentFilter)) {
            labelResource = R.string.order_history_count_delivering;
        } else if (DatabaseHelper.STATUS_RECEIVED.equals(currentFilter)) {
            labelResource = R.string.order_history_count_received;
        } else if (DatabaseHelper.STATUS_CANCELLED.equals(currentFilter)) {
            labelResource = R.string.order_history_count_cancelled;
        }
        String countText = getString(R.string.order_history_count_value, count);
        tvHistoryCount.setText(countText);
        tvHistoryCount.setContentDescription(getString(labelResource) + ": " + countText);
    }

    @Override
    public void onReceived(OrderAdapter.OrderItem order) {
        if (databaseHelper.markOrderReceived(order.id)) {
            Toast.makeText(this, R.string.order_received_success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Không thể cập nhật đơn hàng", Toast.LENGTH_SHORT).show();
        }
        loadOrders();
    }

    @Override
    public void onCancel(OrderAdapter.OrderItem order) {
        int horizontalPadding = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        TextView message = new TextView(this);
        message.setText(R.string.order_cancel_dialog_message);
        message.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        content.addView(message);

        EditText reasonInput = new EditText(this);
        reasonInput.setHint(R.string.order_cancel_reason_hint);
        reasonInput.setGravity(Gravity.TOP | Gravity.START);
        reasonInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        reasonInput.setMinLines(2);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        content.addView(reasonInput, inputParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.order_cancel_dialog_title, order.id))
                .setView(content)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.order_cancel_confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = reasonInput.getText().toString().trim();
            if (reason.isEmpty()) {
                reasonInput.setError(getString(R.string.order_cancel_reason_required));
                return;
            }
            if (databaseHelper.cancelOrder(order.id, reason)) {
                Toast.makeText(this, R.string.order_cancel_success, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadOrders();
            } else {
                Toast.makeText(this, R.string.order_cancel_failed, Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    @Override
    public void onViewDetails(OrderAdapter.OrderItem order) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id);
        startActivity(intent);
    }

    @Override
    public void onReviewProduct(OrderAdapter.OrderProduct product) {
        ReviewEditorDialog.show(this, databaseHelper, product.productId, product.orderId,
                product.name, product.rating, product.comment, this::loadOrders);
    }

    @Override
    public void onViewProduct(OrderAdapter.OrderProduct product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.productId);
        startActivity(intent);
    }
}
