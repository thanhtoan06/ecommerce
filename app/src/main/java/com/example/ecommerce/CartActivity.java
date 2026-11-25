package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartActivity extends AppCompatActivity implements CartAdapter.Listener {
    public static final String EXTRA_PRESELECT_CART_ID = "preselect_cart_id";

    private DatabaseHelper databaseHelper;
    private final List<CartAdapter.CartItem> cartItems = new ArrayList<>();
    private final Set<Long> selectedCartIds = new HashSet<>();
    private CartAdapter adapter;

    private ListView lvCart;
    private View layoutEmptyCart;
    private TextView tvCartTotal;
    private TextView tvSelectedCount;
    private TextView btnClearAllCart;
    private CheckBox cbSelectAll;
    private Button btnCheckout;
    private boolean updatingSelectAll;
    private long preselectedCartId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        databaseHelper = new DatabaseHelper(this);
        preselectedCartId = getIntent().getLongExtra(EXTRA_PRESELECT_CART_ID, -1);
        lvCart = findViewById(R.id.lvCart);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnClearAllCart = findViewById(R.id.btnClearAllCart);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        btnCheckout = findViewById(R.id.btnCheckout);

        adapter = new CartAdapter(this, cartItems, selectedCartIds, this);
        lvCart.setAdapter(adapter);

        cbSelectAll.setOnCheckedChangeListener((button, checked) -> {
            if (updatingSelectAll) {
                return;
            }
            selectedCartIds.clear();
            if (checked) {
                for (CartAdapter.CartItem item : cartItems) {
                    selectedCartIds.add(item.cartId);
                }
            }
            adapter.notifyDataSetChanged();
            updateCheckoutBar();
        });
        btnClearAllCart.setOnClickListener(v -> confirmClearAll());
        findViewById(R.id.btnShopNow).setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));
        btnCheckout.setOnClickListener(v -> {
            if (selectedCartIds.isEmpty()) {
                return;
            }
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra(CheckoutActivity.EXTRA_SELECTED_CART_IDS, toSelectedCartIds());
            startActivity(intent);
        });

        BottomNavigationController.setupForActivity(this, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        cartItems.clear();
        Set<Long> existingCartIds = new HashSet<>();
        Cursor cursor = databaseHelper.getCartItems();
        while (cursor.moveToNext()) {
            CartAdapter.CartItem item = new CartAdapter.CartItem();
            item.cartId = cursor.getLong(cursor.getColumnIndexOrThrow("cart_id"));
            item.productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id"));
            item.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            item.categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
            item.imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            item.price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
            item.quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
            item.stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity"));
            cartItems.add(item);
            existingCartIds.add(item.cartId);
        }
        cursor.close();
        selectedCartIds.retainAll(existingCartIds);
        if (preselectedCartId > 0) {
            selectedCartIds.clear();
            if (existingCartIds.contains(preselectedCartId)) {
                selectedCartIds.add(preselectedCartId);
            } else {
                Toast.makeText(this, R.string.cart_selected_items_missing,
                        Toast.LENGTH_SHORT).show();
            }
            preselectedCartId = -1;
        }
        adapter.notifyDataSetChanged();

        boolean empty = cartItems.isEmpty();
        layoutEmptyCart.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvCart.setVisibility(empty ? View.GONE : View.VISIBLE);
        btnClearAllCart.setVisibility(empty ? View.GONE : View.VISIBLE);
        updateCheckoutBar();
        BottomNavigationController.refreshBadges(this);
    }

    private void updateCheckoutBar() {
        updatingSelectAll = true;
        cbSelectAll.setChecked(!cartItems.isEmpty()
                && selectedCartIds.size() == cartItems.size());
        cbSelectAll.setEnabled(!cartItems.isEmpty());
        updatingSelectAll = false;

        tvSelectedCount.setText(getString(R.string.cart_selected_count,
                selectedCartIds.size(), cartItems.size()));
        tvCartTotal.setText(adapter.formatPrice(
                databaseHelper.getCartTotal(toSelectedCartIds())));
        btnCheckout.setText(getString(R.string.cart_checkout_count, selectedCartIds.size()));
        btnCheckout.setEnabled(!selectedCartIds.isEmpty());
    }

    private long[] toSelectedCartIds() {
        long[] ids = new long[selectedCartIds.size()];
        int index = 0;
        for (Long cartId : selectedCartIds) {
            ids[index++] = cartId;
        }
        return ids;
    }

    @Override
    public void onIncrease(CartAdapter.CartItem item) {
        if (!databaseHelper.updateCartQuantity(item.cartId, item.quantity + 1)) {
            Toast.makeText(this, getString(R.string.cart_stock_limit, item.stock),
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
            confirmRemove(item);
        }
    }

    @Override
    public void onRemove(CartAdapter.CartItem item) {
        if (!databaseHelper.removeCartItem(item.cartId)) {
            Toast.makeText(this, R.string.cart_remove_failed, Toast.LENGTH_SHORT).show();
        }
        loadCart();
    }

    @Override
    public void onSelectionChanged(CartAdapter.CartItem item, boolean selected) {
        if (selected) {
            selectedCartIds.add(item.cartId);
        } else {
            selectedCartIds.remove(item.cartId);
        }
        updateCheckoutBar();
    }

    private void confirmClearAll() {
        if (cartItems.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.cart_clear_all_title)
                .setMessage(R.string.cart_clear_all_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.cart_clear_all, (dialog, which) -> {
                    if (databaseHelper.clearCart()) {
                        selectedCartIds.clear();
                        Toast.makeText(this, R.string.cart_clear_all_success,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.cart_clear_all_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                    loadCart();
                })
                .show();
    }

    private void confirmRemove(CartAdapter.CartItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cart_remove_title)
                .setMessage(getString(R.string.cart_remove_message, item.name))
                .setPositiveButton(R.string.cart_remove_item,
                        (dialog, which) -> onRemove(item))
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }
}
