package com.example.ecommerce;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private static final int SORT_STOCK_DESC = 5;
    private static final int SORT_NAME_ASC = 6;

    private final AppCompatActivity activity;
    private final View root;
    private final DatabaseHelper databaseHelper;
    private final List<ProductItem> allProducts = new ArrayList<>();
    private final List<ProductItem> shownProducts = new ArrayList<>();
    private GridView gridProducts;
    private EditText edtSearch;
    private ImageView btnClearSearch;
    private LinearLayout layoutCategoryTabs;
    private LinearLayout layoutSortPanel;
    private GridLayout layoutSortOptions;
    private LinearLayout layoutEmpty;
    private TextView tvSortLabel;
    private ProductAdapter productAdapter;
    private String selectedCategory = "Tất cả";
    private int selectedSort = SORT_DEFAULT;

    public HomeScreenController(AppCompatActivity activity, View root) {
        this.activity = activity;
        this.root = root;
        databaseHelper = new DatabaseHelper(activity);
        bindViews();
        refreshHeader();
        setupHome();
    }

    private void bindViews() {
        gridProducts = root.findViewById(R.id.gridProducts);
        edtSearch = root.findViewById(R.id.edtSearch);
        btnClearSearch = root.findViewById(R.id.btnClearSearch);
        layoutCategoryTabs = root.findViewById(R.id.layoutCategoryTabs);
        layoutSortPanel = root.findViewById(R.id.layoutSortPanel);
        layoutSortOptions = root.findViewById(R.id.layoutSortOptions);
        layoutEmpty = root.findViewById(R.id.layoutEmpty);
        tvSortLabel = root.findViewById(R.id.tvSortLabel);
    }

    private void setupHome() {
        productAdapter = new ProductAdapter(activity, shownProducts);
        gridProducts.setAdapter(productAdapter);
        loadProducts();
        setupCategories();
        setupSortOptions();

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
        root.findViewById(R.id.btnSort).setOnClickListener(v ->
                layoutSortPanel.setVisibility(layoutSortPanel.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE));
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
        layoutCategoryTabs.removeAllViews();
        addCategoryTab("Tất cả", true);
        Cursor cursor = databaseHelper.getAllCategories();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                addCategoryTab(cursor.getString(cursor.getColumnIndexOrThrow("name")), false);
            }
            cursor.close();
        }
    }

    private void addCategoryTab(String name, boolean selected) {
        TextView tab = new TextView(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (layoutCategoryTabs.getChildCount() > 0) params.leftMargin = dp(8);
        tab.setLayoutParams(params);
        tab.setText(name);
        tab.setTextSize(13);
        tab.setPadding(dp(14), dp(6), dp(14), dp(6));
        updateCategoryTab(tab, selected);
        tab.setOnClickListener(v -> {
            selectedCategory = name;
            for (int i = 0; i < layoutCategoryTabs.getChildCount(); i++) {
                updateCategoryTab((TextView) layoutCategoryTabs.getChildAt(i),
                        layoutCategoryTabs.getChildAt(i) == tab);
            }
            applyFilters();
        });
        layoutCategoryTabs.addView(tab);
    }

    private void updateCategoryTab(TextView tab, boolean active) {
        tab.setBackgroundResource(active
                ? R.drawable.bg_tab_active_white : R.drawable.bg_tab_inactive_translucent);
        tab.setTextColor(ContextCompat.getColor(activity,
                active ? R.color.primary : R.color.white));
        tab.setTypeface(Typeface.create(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL));
    }

    private void resetFilters() {
        edtSearch.setText("");
        selectedCategory = "Tất cả";
        selectedSort = SORT_DEFAULT;
        tvSortLabel.setText("Sắp xếp");
        layoutSortPanel.setVisibility(View.GONE);
        setupSortOptions();
        for (int i = 0; i < layoutCategoryTabs.getChildCount(); i++) {
            updateCategoryTab((TextView) layoutCategoryTabs.getChildAt(i), i == 0);
        }
        applyFilters();
    }

    private void setupSortOptions() {
        layoutSortOptions.removeAllViews();
        String[] options = {"Mặc định", "Giá tăng dần", "Giá giảm dần",
                "Lượt bán giảm dần", "Số sao giảm dần", "Tồn kho nhiều nhất", "Tên A-Z"};
        for (int index = 0; index < options.length; index++) {
            final int sortIndex = index;
            TextView option = new TextView(activity);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / 2, 1f),
                    GridLayout.spec(index % 2, 1f));
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(index % 2 == 0 ? 0 : dp(6),
                    index >= 2 ? dp(6) : 0, 0, 0);
            option.setLayoutParams(params);
            option.setText(options[index]);
            option.setTextSize(12);
            option.setPadding(dp(10), dp(9), dp(10), dp(9));
            option.setGravity(android.view.Gravity.CENTER);
            boolean selected = selectedSort == sortIndex;
            option.setBackgroundResource(selected
                    ? R.drawable.bg_tab_active_white : R.drawable.bg_sort_reset);
            option.setTextColor(ContextCompat.getColor(activity,
                    selected ? R.color.primary : R.color.text_secondary));
            option.setOnClickListener(v -> {
                selectedSort = sortIndex;
                tvSortLabel.setText(sortIndex == SORT_DEFAULT ? "Sắp xếp" : options[sortIndex]);
                layoutSortPanel.setVisibility(View.GONE);
                setupSortOptions();
                applyFilters();
            });
            layoutSortOptions.addView(option);
        }
    }

    private void applyFilters() {
        String keyword = edtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        shownProducts.clear();
        for (ProductItem product : allProducts) {
            boolean nameMatch = keyword.isEmpty()
                    || product.name.toLowerCase(Locale.ROOT).contains(keyword);
            boolean categoryMatch = "Tất cả".equals(selectedCategory)
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
            case SORT_STOCK_DESC:
                Collections.sort(shownProducts, (a, b) -> Integer.compare(b.stockQuantity, a.stockQuantity)); break;
            case SORT_NAME_ASC:
                Collections.sort(shownProducts, (a, b) -> a.name.compareToIgnoreCase(b.name)); break;
            default:
                Collections.sort(shownProducts, (a, b) -> Long.compare(a.id, b.id)); break;
        }
        productAdapter.notifyDataSetChanged();
        gridProducts.setVisibility(shownProducts.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(shownProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }
}
