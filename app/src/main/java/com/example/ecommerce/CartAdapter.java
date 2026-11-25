package com.example.ecommerce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends BaseAdapter {

    /** Một dòng trong giỏ hàng. */
    public static class CartItem {
        public long cartId;
        public long productId;
        public String name;
        public String imagePath;
        public double price;
        public int quantity;
        public int stock;
    }

    /** CartActivity lắng nghe các nút bấm trên từng dòng. */
    public interface Listener {
        void onIncrease(CartItem item);

        void onDecrease(CartItem item);

        void onRemove(CartItem item);
    }

    private final Context context;
    private final List<CartItem> items;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final DecimalFormat priceFormat;

    public CartAdapter(Context context, List<CartItem> items, Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        priceFormat = new DecimalFormat("#,##0", symbols);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public CartItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).cartId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_cart, parent, false);
            holder = new ViewHolder();
            holder.image = convertView.findViewById(R.id.ivCartImage);
            holder.name = convertView.findViewById(R.id.tvCartName);
            holder.price = convertView.findViewById(R.id.tvCartPrice);
            holder.quantity = convertView.findViewById(R.id.tvCartQuantity);
            holder.subtotal = convertView.findViewById(R.id.tvCartSubtotal);
            holder.minus = convertView.findViewById(R.id.btnCartMinus);
            holder.plus = convertView.findViewById(R.id.btnCartPlus);
            holder.remove = convertView.findViewById(R.id.btnCartRemove);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CartItem item = getItem(position);
        holder.name.setText(item.name);
        holder.price.setText(formatPrice(item.price));
        holder.quantity.setText(String.valueOf(item.quantity));
        holder.subtotal.setText(formatPrice(item.price * item.quantity));

        holder.image.setImageResource(R.drawable.ic_product_image_placeholder);
        Bitmap bitmap = loadAssetImage(item.imagePath);
        if (bitmap != null) {
            holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.image.setImageBitmap(bitmap);
        } else {
            holder.image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        holder.minus.setOnClickListener(v -> listener.onDecrease(item));
        holder.plus.setOnClickListener(v -> listener.onIncrease(item));
        holder.remove.setOnClickListener(v -> listener.onRemove(item));
        return convertView;
    }

    public String formatPrice(double value) {
        return priceFormat.format(value) + " ₫";
    }

    // Giống hàm loadAssetImage trong ProductAdapter của Người 1
    private Bitmap loadAssetImage(String imagePath) {
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

    private static class ViewHolder {
        ImageView image;
        TextView name;
        TextView price;
        TextView quantity;
        TextView subtotal;
        TextView minus;
        TextView plus;
        TextView remove;
    }
}
