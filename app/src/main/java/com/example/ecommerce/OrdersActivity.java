package com.example.ecommerce;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity implements OrderAdapter.Listener {
    private DatabaseHelper databaseHelper;
    private final List<OrderAdapter.OrderItem> orders = new ArrayList<>();
    private OrderAdapter adapter;

    private ListView lvOrders;
    private TextView tvOrdersEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        databaseHelper = new DatabaseHelper(this);
        lvOrders = findViewById(R.id.lvOrders);
        tvOrdersEmpty = findViewById(R.id.tvOrdersEmpty);

        adapter = new OrderAdapter(this, orders, true, this);
        lvOrders.setAdapter(adapter);

        // Tab Đơn hàng có chỉ số 2
        BottomNavigationController.setupForActivity(this, 2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    /** Chỉ lấy các đơn đang ở trạng thái "Đang giao". */
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

        // Lấy danh sách món sau khi đã đóng cursor ở trên
        for (OrderAdapter.OrderItem order : orders) {
            order.itemsSummary = databaseHelper.getOrderItemsSummary(order.id);
        }
        adapter.notifyDataSetChanged();

        boolean empty = orders.isEmpty();
        tvOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onReceived(OrderAdapter.OrderItem order) {
        if (databaseHelper.markOrderReceived(order.id)) {
            Toast.makeText(this,
                    "Đã nhận hàng. Bạn có thể đánh giá sản phẩm trong mục Đánh giá của tôi",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Không thể cập nhật đơn hàng", Toast.LENGTH_SHORT).show();
        }
        loadOrders();
    }

    @Override
    public void onCancel(OrderAdapter.OrderItem order) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc muốn hủy đơn #" + order.id + " không?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    if (databaseHelper.cancelOrder(order.id)) {
                        Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không thể hủy đơn hàng",
                                Toast.LENGTH_SHORT).show();
                    }
                    loadOrders();
                })
                .show();
    }
}
