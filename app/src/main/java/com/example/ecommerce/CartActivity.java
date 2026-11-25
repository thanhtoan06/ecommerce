package com.example.ecommerce;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.Listener {
    private DatabaseHelper databaseHelper;
    private final List<CartAdapter.CartItem> cartItems = new ArrayList<>();
    private CartAdapter adapter;

    private ListView lvCart;
    private TextView tvCartEmpty;
    private TextView tvCartTotal;
    private Button btnCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        databaseHelper = new DatabaseHelper(this);
        lvCart = findViewById(R.id.lvCart);
        tvCartEmpty = findViewById(R.id.tvCartEmpty);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        btnCheckout = findViewById(R.id.btnCheckout);

        adapter = new CartAdapter(this, cartItems, this);
        lvCart.setAdapter(adapter);

        btnCheckout.setOnClickListener(v ->
                startActivity(new Intent(this, CheckoutActivity.class)));

        // Tab Giỏ hàng có chỉ số 1
        BottomNavigationController.setupForActivity(this, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart(); // tải lại mỗi khi quay về màn hình này
    }

    private void loadCart() {
        cartItems.clear();
        Cursor cursor = databaseHelper.getCartItems();
        while (cursor.moveToNext()) {
            CartAdapter.CartItem item = new CartAdapter.CartItem();
            item.cartId = cursor.getLong(cursor.getColumnIndexOrThrow("cart_id"));
            item.productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id"));
            item.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            item.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            item.price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
            item.quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
            item.stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity"));
            cartItems.add(item);
        }
        cursor.close();
        adapter.notifyDataSetChanged();

        boolean empty = cartItems.isEmpty();
        tvCartEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvCart.setVisibility(empty ? View.GONE : View.VISIBLE);
        btnCheckout.setEnabled(!empty);
        tvCartTotal.setText(adapter.formatPrice(databaseHelper.getCartTotal()));
    }

    @Override
    public void onIncrease(CartAdapter.CartItem item) {
        if (!databaseHelper.updateCartQuantity(item.cartId, item.quantity + 1)) {
            Toast.makeText(this, "Chỉ còn " + item.stock + " sản phẩm trong kho",
                    Toast.LENGTH_SHORT).show();
        }
        loadCart();
    }

    @Override
    public void onDecrease(CartAdapter.CartItem item) {
        if (item.quantity > 1) {
            databaseHelper.updateCartQuantity(item.cartId, item.quantity - 1);
            loadCart();
        } else {
            confirmRemove(item); // đang là 1 mà giảm nữa thì hỏi xóa
        }
    }

    @Override
    public void onRemove(CartAdapter.CartItem item) {
        databaseHelper.removeCartItem(item.cartId);
        loadCart();
    }

    private void confirmRemove(CartAdapter.CartItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa sản phẩm")
                .setMessage("Xóa \"" + item.name + "\" khỏi giỏ hàng?")
                .setPositiveButton("Xóa", (dialog, which) -> onRemove(item))
                .setNegativeButton("Hủy", null)
                .show();
    }
}
