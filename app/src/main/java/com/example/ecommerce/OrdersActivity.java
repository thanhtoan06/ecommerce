package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.database.Cursor;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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

public class OrdersActivity extends AppCompatActivity implements
        OrderAdapter.Listener, OrderAdapter.DetailsListener {
    private DatabaseHelper databaseHelper;
    private final List<OrderAdapter.OrderItem> orders = new ArrayList<>();
    private OrderAdapter adapter;

    private ListView lvOrders;
    private TextView tvOrdersEmpty;
    private TextView tvOrdersCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        databaseHelper = new DatabaseHelper(this);
        lvOrders = findViewById(R.id.lvOrders);
        tvOrdersEmpty = findViewById(R.id.tvOrdersEmpty);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);

        adapter = new OrderAdapter(this, orders, true, this, this);
        lvOrders.setAdapter(adapter);

        BottomNavigationController.setupForActivity(this, 2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        orders.clear();
        Cursor cursor = databaseHelper.getOrdersByStatus(DatabaseHelper.STATUS_DELIVERING);
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

        String countText = getString(R.string.order_history_count_value, orders.size());
        tvOrdersCount.setText(countText);
        tvOrdersCount.setContentDescription(getString(
                R.string.order_history_count_delivering) + ": " + countText);

        adapter.notifyDataSetChanged();

        boolean empty = orders.isEmpty();
        tvOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
        BottomNavigationController.refreshBadges(this);
    }

    @Override
    public void onReceived(OrderAdapter.OrderItem order) {
        if (databaseHelper.markOrderReceived(order.id)) {
            Toast.makeText(this,
                    R.string.order_received_success,
                    Toast.LENGTH_LONG).show();
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
}
