package com.example.ecommerce;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends BaseAdapter {
    private final Context context;
    private final List<ProductItem> products;
    private final LayoutInflater inflater;
    private final DecimalFormat priceFormat;

    public ProductAdapter(Context context, List<ProductItem> products) {
        this.context = context;
        this.products = products;
        this.inflater = LayoutInflater.from(context);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        priceFormat = new DecimalFormat("#,##0", symbols);
    }

    @Override
    public int getCount() {
        return products.size();
    }

    @Override
    public ProductItem getItem(int position) {
        return products.get(position);
    }

    @Override
    public long getItemId(int position) {
        return products.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_product, parent, false);
            holder = new ViewHolder();
            holder.image = convertView.findViewById(R.id.ivProductImage);
            holder.category = convertView.findViewById(R.id.tvProductCategory);
            holder.name = convertView.findViewById(R.id.tvProductName);
            holder.price = convertView.findViewById(R.id.tvProductPrice);
            holder.rating = convertView.findViewById(R.id.tvProductRating);
            holder.stock = convertView.findViewById(R.id.tvProductStock);
            holder.sold = convertView.findViewById(R.id.tvProductSold);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ProductItem product = getItem(position);
        holder.category.setText(product.categoryName);
        holder.name.setText(product.name);
        holder.price.setText(priceFormat.format(product.price) + " ₫");
        holder.rating.setText(String.format(Locale.forLanguageTag("vi-VN"),
                "★ %.1f", product.averageRating));
        holder.stock.setText("Còn " + product.stockQuantity + " sp");
        holder.sold.setText("(" + product.reviewCount + ") · Đã bán " + product.soldQuantity);
        holder.image.setImageResource(R.drawable.ic_product_image_placeholder);

        Bitmap bitmap = loadAssetImage(product.imagePath);
        if (bitmap != null) {
            holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.image.setImageBitmap(bitmap);
        } else {
            holder.image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id);
            context.startActivity(intent);
        });
        return convertView;
    }

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
        TextView category;
        TextView name;
        TextView price;
        TextView rating;
        TextView stock;
        TextView sold;
    }
}
