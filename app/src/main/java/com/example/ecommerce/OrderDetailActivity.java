package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER_ID = "order_id";

    private DatabaseHelper databaseHelper;
    private long orderId;
    private DecimalFormat priceFormat;

    private TextView tvOrderDetailStatus;
    private TextView tvOrderDetailTotal;
    private TextView tvOrderDetailItemsEmpty;
    private LinearLayout layoutOrderDetailItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        databaseHelper = new DatabaseHelper(this);
        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);
        DecimalFormatSymbols symbols =
                new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        priceFormat = new DecimalFormat("#,##0", symbols);

        findViewById(R.id.btnOrderDetailBack).setOnClickListener(v -> finish());
        tvOrderDetailStatus = findViewById(R.id.tvOrderDetailStatus);
        tvOrderDetailTotal = findViewById(R.id.tvOrderDetailTotal);
        tvOrderDetailItemsEmpty = findViewById(R.id.tvOrderDetailItemsEmpty);
        layoutOrderDetailItems = findViewById(R.id.layoutOrderDetailItems);

        if (orderId <= 0 || !loadOrder()) {
            Toast.makeText(this, R.string.order_detail_missing, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean loadOrder() {
        try (Cursor cursor = databaseHelper.getOrderById(orderId)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }

            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String orderDate = cursor.getString(cursor.getColumnIndexOrThrow("order_date"));
            String receiverName = cursor.getString(
                    cursor.getColumnIndexOrThrow("receiver_name"));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            String note = cursor.getString(cursor.getColumnIndexOrThrow("note"));
            String cancelReason = cursor.getString(
                    cursor.getColumnIndexOrThrow("cancel_reason"));
            double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
            double shippingFee = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("shipping_fee"));
            String paymentMethod = cursor.getString(
                    cursor.getColumnIndexOrThrow("payment_method"));

            ((TextView) findViewById(R.id.tvOrderDetailCode)).setText(
                    getString(R.string.order_detail_code, orderId));
            ((TextView) findViewById(R.id.tvOrderDetailDate)).setText(
                    getString(R.string.order_detail_date, formatDate(orderDate)));
            ((TextView) findViewById(R.id.tvOrderDetailReceiver)).setText(receiverName);
            ((TextView) findViewById(R.id.tvOrderDetailPhone)).setText(phone);
            ((TextView) findViewById(R.id.tvOrderDetailAddress)).setText(address);
            ((TextView) findViewById(R.id.tvOrderDetailItemsSubtotal)).setText(
                    formatPrice(total - shippingFee));
            ((TextView) findViewById(R.id.tvOrderDetailShippingFee)).setText(
                    formatPrice(shippingFee));
            TextView paymentMethodView = findViewById(R.id.tvOrderDetailPaymentMethod);
            if (DatabaseHelper.PAYMENT_METHOD_COD.equals(paymentMethod)) {
                paymentMethodView.setText(R.string.order_detail_payment_cod);
            } else {
                paymentMethodView.setText(paymentMethod);
            }
            tvOrderDetailTotal.setText(formatPrice(total));

            View cancelReasonLayout = findViewById(R.id.layoutOrderDetailCancelReason);
            if (DatabaseHelper.STATUS_CANCELLED.equals(status)
                    && cancelReason != null && !cancelReason.trim().isEmpty()) {
                ((TextView) findViewById(R.id.tvOrderDetailCancelReason))
                        .setText(cancelReason);
                cancelReasonLayout.setVisibility(View.VISIBLE);
            } else {
                cancelReasonLayout.setVisibility(View.GONE);
            }

            if (note == null || note.trim().isEmpty()) {
                findViewById(R.id.layoutOrderDetailNote).setVisibility(View.GONE);
            } else {
                ((TextView) findViewById(R.id.tvOrderDetailNote)).setText(note);
                findViewById(R.id.layoutOrderDetailNote).setVisibility(View.VISIBLE);
            }
            showStatus(status);
        }

        loadOrderItems();
        return true;
    }

    private void showStatus(String status) {
        tvOrderDetailStatus.setText(status);
        boolean cancelled = DatabaseHelper.STATUS_CANCELLED.equals(status);
        tvOrderDetailStatus.setBackgroundResource(
                cancelled ? R.drawable.bg_chip_red : R.drawable.bg_chip_blue);
        tvOrderDetailStatus.setTextColor(ContextCompat.getColor(this,
                cancelled ? R.color.danger : R.color.primary));
    }

    private void loadOrderItems() {
        layoutOrderDetailItems.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int itemCount = 0;
        try (Cursor cursor = databaseHelper.getOrderDetails(orderId)) {
            while (cursor.moveToNext()) {
                View row = inflater.inflate(
                        R.layout.item_order_detail_product, layoutOrderDetailItems, false);
                String imagePath = cursor.getString(
                        cursor.getColumnIndexOrThrow("image_path"));
                Bitmap bitmap = loadAssetImage(imagePath);
                ImageView image = row.findViewById(R.id.ivOrderDetailProduct);
                if (bitmap == null) {
                    image.setImageResource(R.drawable.ic_product_image_placeholder);
                } else {
                    image.setImageBitmap(bitmap);
                }

                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                long productId = cursor.getLong(cursor.getColumnIndexOrThrow("product_id"));
                double unitPrice = cursor.getDouble(
                        cursor.getColumnIndexOrThrow("unit_price"));
                double subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal"));
                ((TextView) row.findViewById(R.id.tvOrderDetailProductName)).setText(
                        cursor.getString(cursor.getColumnIndexOrThrow("name")));
                ((TextView) row.findViewById(R.id.tvOrderDetailProductPrice)).setText(
                        getString(R.string.order_detail_unit_price_quantity,
                                formatPrice(unitPrice), quantity));
                ((TextView) row.findViewById(R.id.tvOrderDetailProductSubtotal)).setText(
                        formatPrice(subtotal));
                row.findViewById(R.id.btnOrderDetailViewProduct).setOnClickListener(v -> {
                    Intent intent = new Intent(this, ProductDetailActivity.class);
                    intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
                    startActivity(intent);
                });
                layoutOrderDetailItems.addView(row);
                itemCount++;
            }
        }

        layoutOrderDetailItems.setVisibility(itemCount == 0 ? View.GONE : View.VISIBLE);
        tvOrderDetailItemsEmpty.setVisibility(itemCount == 0 ? View.VISIBLE : View.GONE);
    }

    private Bitmap loadAssetImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        String normalizedPath = imagePath
                .replace('\\', '/')
                .replaceFirst("^/+", "")
                .replaceFirst("^assets/", "");
        try (InputStream input = getAssets().open(normalizedPath)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException error) {
            if (normalizedPath.equals(imagePath)) {
                return null;
            }
            try (InputStream input = getAssets().open(imagePath)) {
                return BitmapFactory.decodeStream(input);
            } catch (IOException fallbackError) {
                return null;
            }
        }
    }

    private String formatPrice(double value) {
        return priceFormat.format(value) + " ₫";
    }

    private String formatDate(String raw) {
        try {
            Date date = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw);
            if (date == null) {
                return raw;
            }
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"))
                    .format(date);
        } catch (ParseException error) {
            return raw;
        }
    }
}
