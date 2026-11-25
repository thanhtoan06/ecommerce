package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_CART_IDS = "selected_cart_ids";

    private DatabaseHelper databaseHelper;
    private long[] selectedCartIds;
    private DecimalFormat priceFormat;

    private LinearLayout llCheckoutItems;
    private TextView tvCheckoutTotal;
    private TextView tvCheckoutItemsSubtotal;
    private TextView tvCheckoutShippingFee;
    private EditText etReceiverName;
    private EditText etReceiverPhone;
    private EditText etReceiverAddress;
    private EditText etOrderNote;
    private Button btnConfirmOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        StatusBarUtils.setupHeader(this, findViewById(R.id.topBar));
        View bottomCheckoutBar = findViewById(R.id.bottomCheckoutBar);
        int bottomBarPadding = bottomCheckoutBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bottomCheckoutBar, (view, insets) -> {
            Insets navigationInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bottomBarPadding + navigationInsets.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(bottomCheckoutBar);

        databaseHelper = new DatabaseHelper(this);
        selectedCartIds = getIntent().getLongArrayExtra(EXTRA_SELECTED_CART_IDS);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        priceFormat = new DecimalFormat("#,##0", symbols);

        llCheckoutItems = findViewById(R.id.llCheckoutItems);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        tvCheckoutItemsSubtotal = findViewById(R.id.tvCheckoutItemsSubtotal);
        tvCheckoutShippingFee = findViewById(R.id.tvCheckoutShippingFee);
        etReceiverName = findViewById(R.id.etReceiverName);
        etReceiverPhone = findViewById(R.id.etReceiverPhone);
        etReceiverAddress = findViewById(R.id.etReceiverAddress);
        etOrderNote = findViewById(R.id.etOrderNote);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        findViewById(R.id.btnCheckoutBack).setOnClickListener(v -> finish());

        if (selectedCartIds == null || selectedCartIds.length == 0) {
            Toast.makeText(this, R.string.cart_select_items_first, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!loadItems()) {
            Toast.makeText(this, R.string.cart_selected_items_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        prefillForm();
        btnConfirmOrder.setOnClickListener(v -> confirmOrder());
    }

    private boolean loadItems() {
        llCheckoutItems.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = 0;

        Cursor cursor = databaseHelper.getCartItems(selectedCartIds);
        while (cursor.moveToNext()) {
            View row = inflater.inflate(R.layout.item_checkout, llCheckoutItems, false);
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));

            ImageView image = row.findViewById(R.id.ivCheckoutImage);
            image.setImageResource(R.drawable.ic_product_image_placeholder);
            Bitmap bitmap = loadAssetImage(this, imagePath);
            if (bitmap != null) {
                image.setImageBitmap(bitmap);
            }
            ((TextView) row.findViewById(R.id.tvCheckoutName)).setText(name);
            ((TextView) row.findViewById(R.id.tvCheckoutQty)).setText(
                    formatPrice(price) + "  x" + quantity);
            ((TextView) row.findViewById(R.id.tvCheckoutSubtotal)).setText(
                    formatPrice(price * quantity));
            llCheckoutItems.addView(row);
            count++;
        }
        cursor.close();

        double itemsSubtotal = databaseHelper.getCartTotal(selectedCartIds);
        tvCheckoutItemsSubtotal.setText(formatPrice(itemsSubtotal));
        tvCheckoutShippingFee.setText(formatPrice(DatabaseHelper.SHIPPING_FEE));
        tvCheckoutTotal.setText(formatPrice(itemsSubtotal + DatabaseHelper.SHIPPING_FEE));
        return count == selectedCartIds.length;
    }

    private void prefillForm() {
        try (Cursor cursor = databaseHelper.getSessionUser()) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                etReceiverName.setText(name == null ? "" : name);
                etReceiverPhone.setText(phone == null ? "" : phone);
                etReceiverAddress.setText(address == null ? "" : address);
            }
        }
    }

    private void confirmOrder() {
        String name = etReceiverName.getText().toString().trim();
        String phone = etReceiverPhone.getText().toString().trim();
        String address = etReceiverAddress.getText().toString().trim();
        String note = etOrderNote.getText().toString().trim();

        boolean valid = true;
        if (name.isEmpty()) {
            etReceiverName.setError("Vui lòng nhập tên người nhận");
            valid = false;
        }
        if (phone.isEmpty()) {
            etReceiverPhone.setError("Vui lòng nhập số điện thoại");
            valid = false;
        }
        if (address.isEmpty()) {
            etReceiverAddress.setError("Vui lòng nhập địa chỉ nhận hàng");
            valid = false;
        }
        if (!valid) {
            return;
        }

        btnConfirmOrder.setEnabled(false);
        long result = databaseHelper.placeOrder(name, phone, address, note, selectedCartIds);
        if (result > 0) {
            Toast.makeText(this, "Đặt hàng thành công", Toast.LENGTH_SHORT).show();
            finish();
        } else if (result == DatabaseHelper.ORDER_OUT_OF_STOCK) {
            Toast.makeText(this,
                    "Có sản phẩm vượt quá số lượng tồn kho, vui lòng kiểm tra lại giỏ hàng",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            btnConfirmOrder.setEnabled(true);
            Toast.makeText(this, "Đặt hàng thất bại, vui lòng thử lại",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String formatPrice(double value) {
        return priceFormat.format(value) + " ₫";
    }

    private static Bitmap loadAssetImage(Context context, String imagePath) {
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
}
