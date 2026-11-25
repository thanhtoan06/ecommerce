package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeScreenController {
    private static final int SORT_DEFAULT = 0;
    private static final int SORT_PRICE_ASC = 1;
    private static final int SORT_PRICE_DESC = 2;
    private static final int SORT_SOLD_DESC = 3;
    private static final int SORT_RATING_DESC = 4;
    private static final int SORT_NAME_ASC = 5;
    private final AppCompatActivity activity;
    private final View root;
    private final DatabaseHelper databaseHelper;
    private final List<ProductItem> allProducts = new ArrayList<>();
    private final List<ProductItem> shownProducts = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private GridView gridProducts;
    private EditText edtSearch;
    private ImageView btnClearSearch;
    private LinearLayout layoutEmpty;
    private TextView tvCategoryLabel;
    private TextView tvSortLabel;
    private View viewCategoryIndicator;
    private View viewSortIndicator;
    private ProductAdapter productAdapter;
    private String selectedCategory = "Tất cả";
    private int selectedSort = SORT_DEFAULT;

    public HomeScreenController(AppCompatActivity activity, View root) {
        this.activity = activity;
        this.root = root;
        selectedCategory = activity.getString(R.string.home_category_all);
        databaseHelper = new DatabaseHelper(activity);
        bindViews();
        refreshHeader();
        setupHome();
    }

    private void bindViews() {
        gridProducts = root.findViewById(R.id.gridProducts);
        edtSearch = root.findViewById(R.id.edtSearch);
        btnClearSearch = root.findViewById(R.id.btnClearSearch);
        layoutEmpty = root.findViewById(R.id.layoutEmpty);
        tvCategoryLabel = root.findViewById(R.id.tvCategoryLabel);
        tvSortLabel = root.findViewById(R.id.tvSortLabel);
        viewCategoryIndicator = root.findViewById(R.id.viewCategoryIndicator);
        viewSortIndicator = root.findViewById(R.id.viewSortIndicator);
    }

    private void setupHome() {
        productAdapter = new ProductAdapter(activity, shownProducts);
        gridProducts.setAdapter(productAdapter);
        loadProducts();
        setupCategories();
        updateCategoryIndicator();
        updateSortIndicator();

        edtSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilters();
            }
            public void afterTextChanged(Editable s) {}
        });
        btnClearSearch.setOnClickListener(v -> edtSearch.setText(""));
        root.findViewById(R.id.btnResetFilter).setOnClickListener(v -> resetFilters());
        root.findViewById(R.id.btnCategory).setOnClickListener(v -> showCategoryBottomSheet());
        root.findViewById(R.id.btnSort).setOnClickListener(v -> showSortBottomSheet());
    }

    public void refreshHeader() {
        Cursor cursor = databaseHelper.getSessionUser();
        TextView greeting = root.findViewById(R.id.tvUserGreeting);
        TextView email = root.findViewById(R.id.tvUserEmail);
        ImageView avatar = root.findViewById(R.id.ivHeaderAvatar);
        View userInfo = root.findViewById(R.id.layoutUserInfo);
        View guestActions = root.findViewById(R.id.layoutGuestActions);
        root.findViewById(R.id.btnHomeLogin).setOnClickListener(v ->
                activity.startActivity(new Intent(activity, LoginActivity.class)));
        root.findViewById(R.id.btnHomeRegister).setOnClickListener(v ->
                activity.startActivity(new Intent(activity, RegisterActivity.class)));

        if (cursor != null && cursor.moveToFirst()) {
            greeting.setText("Xin chào, " +
                    cursor.getString(cursor.getColumnIndexOrThrow("display_name")));
            email.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            loadAvatar(avatar, cursor.getString(cursor.getColumnIndexOrThrow("avatar_path")));
            userInfo.setVisibility(View.VISIBLE);
            guestActions.setVisibility(View.GONE);
        } else {
            userInfo.setVisibility(View.GONE);
            guestActions.setVisibility(View.VISIBLE);
        }
        if (cursor != null) cursor.close();
    }

    private void loadAvatar(ImageView imageView, String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()
                || DatabaseHelper.DEFAULT_AVATAR_PATH.equals(avatarPath)) {
            imageView.setImageResource(R.drawable.ic_avatar_default);
            return;
        }
        if (avatarPath.startsWith("avatars/")) {
            File file = new File(activity.getFilesDir(), avatarPath);
            if (file.exists()) {
                imageView.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_avatar_default);
    }

    private void loadProducts() {
        allProducts.clear();
        Cursor cursor = databaseHelper.getAllProducts();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                allProducts.add(new ProductItem(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        cursor.getString(cursor.getColumnIndexOrThrow("image_path")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("sold_quantity")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("average_rating")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("review_count")),
                        cursor.getString(cursor.getColumnIndexOrThrow("category_name"))));
            }
            cursor.close();
        }
        applyFilters();
    }

    private void setupCategories() {
        categories.clear();
        categories.add(activity.getString(R.string.home_category_all));
        Cursor cursor = databaseHelper.getAllCategories();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                categories.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        }
    }

    private void showCategoryBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View sheetView = activity.getLayoutInflater().inflate(R.layout.layout_bottom_sheet_category, null);
        dialog.setContentView(sheetView);

        if (sheetView.getParent() != null) {
            ((View) sheetView.getParent()).setBackgroundResource(android.R.color.transparent);
        }

        ImageView ivSheetIcon = sheetView.findViewById(R.id.ivSheetIcon);
        TextView tvSheetTitle = sheetView.findViewById(R.id.tvSheetTitle);
        ImageView btnCloseSheet = sheetView.findViewById(R.id.btnCloseSheet);
        LinearLayout optionsContainer = sheetView.findViewById(R.id.layoutOptionsContainer);

        ivSheetIcon.setImageResource(R.drawable.ic_category);
        tvSheetTitle.setText(R.string.home_filter_category_title);
        btnCloseSheet.setOnClickListener(v -> dialog.dismiss());
        optionsContainer.removeAllViews();

        int primaryColor = ContextCompat.getColor(activity, R.color.primary);
        int textPrimaryColor = ContextCompat.getColor(activity, R.color.text_primary);

        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            boolean isSelected = category.equals(selectedCategory);

            View itemView = activity.getLayoutInflater().inflate(R.layout.item_filter_option, optionsContainer, false);
            TextView tvOptionName = itemView.findViewById(R.id.tvOptionName);
            ImageView ivOptionCheck = itemView.findViewById(R.id.ivOptionCheck);

            tvOptionName.setText(category);
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_filter_item_selected);
                tvOptionName.setTextColor(primaryColor);
                tvOptionName.setTypeface(null, Typeface.BOLD);
                ivOptionCheck.setVisibility(View.VISIBLE);
            } else {
                itemView.setBackgroundResource(R.drawable.bg_filter_item_unselected);
                tvOptionName.setTextColor(textPrimaryColor);
                tvOptionName.setTypeface(null, Typeface.NORMAL);
                ivOptionCheck.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                selectedCategory = category;
                tvCategoryLabel.setText(selectedCategory);
                updateCategoryIndicator();
                applyFilters();
                dialog.dismiss();
            });

            optionsContainer.addView(itemView);
        }

        dialog.show();
    }

    private void showSortBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View sheetView = activity.getLayoutInflater().inflate(R.layout.layout_bottom_sheet_sort, null);
        dialog.setContentView(sheetView);

        if (sheetView.getParent() != null) {
            ((View) sheetView.getParent()).setBackgroundResource(android.R.color.transparent);
        }

        ImageView ivSheetIcon = sheetView.findViewById(R.id.ivSheetIcon);
        TextView tvSheetTitle = sheetView.findViewById(R.id.tvSheetTitle);
        ImageView btnCloseSheet = sheetView.findViewById(R.id.btnCloseSheet);
        LinearLayout optionsContainer = sheetView.findViewById(R.id.layoutOptionsContainer);

        ivSheetIcon.setImageResource(R.drawable.ic_sort);
        tvSheetTitle.setText(R.string.home_filter_sort_title);
        btnCloseSheet.setOnClickListener(v -> dialog.dismiss());
        optionsContainer.removeAllViews();

        String[] sortOptions = activity.getResources().getStringArray(R.array.home_sort_options);
        int primaryColor = ContextCompat.getColor(activity, R.color.primary);
        int textPrimaryColor = ContextCompat.getColor(activity, R.color.text_primary);

        for (int i = 0; i < sortOptions.length; i++) {
            final int index = i;
            String sortName = sortOptions[i];
            boolean isSelected = (selectedSort == index);

            View itemView = activity.getLayoutInflater().inflate(R.layout.item_filter_option, optionsContainer, false);
            TextView tvOptionName = itemView.findViewById(R.id.tvOptionName);
            ImageView ivOptionCheck = itemView.findViewById(R.id.ivOptionCheck);

            tvOptionName.setText(sortName);
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_filter_item_selected);
                tvOptionName.setTextColor(primaryColor);
                tvOptionName.setTypeface(null, Typeface.BOLD);
                ivOptionCheck.setVisibility(View.VISIBLE);
            } else {
                itemView.setBackgroundResource(R.drawable.bg_filter_item_unselected);
                tvOptionName.setTextColor(textPrimaryColor);
                tvOptionName.setTypeface(null, Typeface.NORMAL);
                ivOptionCheck.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                selectedSort = index;
                if (selectedSort == SORT_DEFAULT) {
                    tvSortLabel.setText(R.string.home_sort_label);
                } else {
                    tvSortLabel.setText(sortOptions[selectedSort]);
                }
                updateSortIndicator();
                applyFilters();
                dialog.dismiss();
            });

            optionsContainer.addView(itemView);
        }

        dialog.show();
    }

    private void updateCategoryIndicator() {
        if (viewCategoryIndicator == null) return;
        boolean isFiltered = !activity.getString(R.string.home_category_all).equals(selectedCategory);
        viewCategoryIndicator.setVisibility(isFiltered ? View.VISIBLE : View.GONE);
    }

    private void updateSortIndicator() {
        if (viewSortIndicator == null) return;
        boolean isSorted = (selectedSort != SORT_DEFAULT);
        viewSortIndicator.setVisibility(isSorted ? View.VISIBLE : View.GONE);
    }

    private void resetFilters() {
        edtSearch.setText("");
        selectedCategory = activity.getString(R.string.home_category_all);
        selectedSort = SORT_DEFAULT;
        tvCategoryLabel.setText(selectedCategory);
        tvSortLabel.setText(R.string.home_sort_label);
        updateCategoryIndicator();
        updateSortIndicator();
        applyFilters();
    }

    private void applyFilters() {
        String keyword = edtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        shownProducts.clear();
        for (ProductItem product : allProducts) {
            boolean nameMatch = keyword.isEmpty()
                    || product.name.toLowerCase(Locale.ROOT).contains(keyword);
            boolean categoryMatch = activity.getString(R.string.home_category_all)
                    .equals(selectedCategory)
                    || product.categoryName.equals(selectedCategory);
            if (nameMatch && categoryMatch) shownProducts.add(product);
        }
        switch (selectedSort) {
            case SORT_PRICE_ASC:
                Collections.sort(shownProducts, Comparator.comparingDouble(p -> p.price)); break;
            case SORT_PRICE_DESC:
                Collections.sort(shownProducts, (a, b) -> Double.compare(b.price, a.price)); break;
            case SORT_SOLD_DESC:
                Collections.sort(shownProducts, (a, b) -> Integer.compare(b.soldQuantity, a.soldQuantity)); break;
            case SORT_RATING_DESC:
                Collections.sort(shownProducts, (a, b) -> Double.compare(b.averageRating, a.averageRating)); break;
            case SORT_NAME_ASC:
                Collections.sort(shownProducts, (a, b) -> a.name.compareToIgnoreCase(b.name)); break;
            default:
                Collections.sort(shownProducts, (a, b) -> Long.compare(a.id, b.id)); break;
        }
        productAdapter.notifyDataSetChanged();
        gridProducts.setVisibility(shownProducts.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(shownProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
