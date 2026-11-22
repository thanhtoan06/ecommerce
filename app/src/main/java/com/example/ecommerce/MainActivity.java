package com.example.ecommerce;

import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int SORT_DEFAULT = 0;
    private static final int SORT_PRICE_ASC = 1;
    private static final int SORT_PRICE_DESC = 2;
    private static final int SORT_SOLD_DESC = 3;
    private static final int SORT_RATING_DESC = 4;
    private static final int SORT_STOCK_DESC = 5;
    private static final int SORT_NAME_ASC = 6;

    private View[] menuItems;
    private ImageView[] menuIcons;
    private TextView[] menuLabels;
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
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        databaseHelper = new DatabaseHelper(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupHeader();
        setupHomeContent();
        menuItems = new View[]{
                findViewById(R.id.menuHome),
                findViewById(R.id.menuCart),
                findViewById(R.id.menuOrders),
                findViewById(R.id.menuAccount)
        };
        menuIcons = new ImageView[]{
                findViewById(R.id.ivMenuHome),
                findViewById(R.id.ivMenuCart),
                findViewById(R.id.ivMenuOrders),
                findViewById(R.id.ivMenuAccount)
        };
        menuLabels = new TextView[]{
                findViewById(R.id.tvMenuHome),
                findViewById(R.id.tvMenuCart),
                findViewById(R.id.tvMenuOrders),
                findViewById(R.id.tvMenuAccount)
        };

        for (int index = 0; index < menuItems.length; index++) {
            final int tabIndex = index;
            menuItems[index].setOnClickListener(v -> selectTab(tabIndex));
        }
        selectTab(0);
    }

    private void selectTab(int selectedIndex) {
        int primary = ContextCompat.getColor(this, R.color.primary);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);

        for (int index = 0; index < menuItems.length; index++) {
            boolean selected = index == selectedIndex;
            menuIcons[index].setColorFilter(selected ? primary : secondary);
            menuLabels[index].setTextColor(selected ? primary : secondary);
            menuLabels[index].setTypeface(
                    Typeface.create(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL));
        }
        if (selectedIndex == 0) {
            findViewById(R.id.mainContent).setVisibility(View.VISIBLE);
        } else {
            if (databaseHelper.getSessionUserId() < 0) {
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            String[] names = {"", "Giỏ hàng", "Đơn hàng", "Trang cá nhân"};
            Toast.makeText(this, names[selectedIndex] + " sẽ được triển khai tiếp", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (databaseHelper != null) {
            setupHeader();
        }
    }

    private void setupHeader() {
        Cursor cursor = databaseHelper.getSessionUser();
        TextView tvGreeting = findViewById(R.id.tvUserGreeting);
        TextView tvEmail = findViewById(R.id.tvUserEmail);
        ImageView ivAvatar = findViewById(R.id.ivHeaderAvatar);
        View layoutUserInfo = findViewById(R.id.layoutUserInfo);
        View layoutGuestActions = findViewById(R.id.layoutGuestActions);

        findViewById(R.id.btnHomeLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.btnHomeRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        if (cursor != null && cursor.moveToFirst()) {
            String displayName = cursor.getString(cursor.getColumnIndexOrThrow("display_name"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            String avatarPath = cursor.getString(cursor.getColumnIndexOrThrow("avatar_path"));
            tvGreeting.setText("Xin chào, " + displayName);
            tvEmail.setText(email);
            loadAvatar(ivAvatar, avatarPath);
            layoutUserInfo.setVisibility(View.VISIBLE);
            layoutGuestActions.setVisibility(View.GONE);
        } else {
            layoutUserInfo.setVisibility(View.GONE);
            layoutGuestActions.setVisibility(View.VISIBLE);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private void loadAvatar(ImageView imageView, String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()
                || DatabaseHelper.DEFAULT_AVATAR_PATH.equals(avatarPath)) {
            imageView.setImageResource(R.drawable.ic_avatar_default);
            return;
        }
        if (avatarPath.startsWith("avatars/")) {
            File file = new File(getFilesDir(), avatarPath);
            if (file.exists()) {
                imageView.setImageURI(Uri.fromFile(file));
                return;
            }
        }
        imageView.setImageResource(R.drawable.ic_avatar_default);
    }

    private void setupHomeContent() {
        gridProducts = findViewById(R.id.gridProducts);
        edtSearch = findViewById(R.id.edtSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        layoutCategoryTabs = findViewById(R.id.layoutCategoryTabs);
        layoutSortPanel = findViewById(R.id.layoutSortPanel);
        layoutSortOptions = findViewById(R.id.layoutSortOptions);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvSortLabel = findViewById(R.id.tvSortLabel);
        productAdapter = new ProductAdapter(this, shownProducts);
        gridProducts.setAdapter(productAdapter);

        loadProducts();
        setupCategories();
        setupSortOptions();

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        btnClearSearch.setOnClickListener(v -> edtSearch.setText(""));
        findViewById(R.id.btnResetFilter).setOnClickListener(v -> resetFilters());
        findViewById(R.id.btnSort).setOnClickListener(v -> {
            layoutSortPanel.setVisibility(layoutSortPanel.getVisibility() == View.VISIBLE
                    ? View.GONE : View.VISIBLE);
        });
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

    private void addCategoryTab(String categoryName, boolean selected) {
        TextView tab = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (layoutCategoryTabs.getChildCount() > 0) {
            params.leftMargin = (int) (8 * getResources().getDisplayMetrics().density);
        }
        tab.setLayoutParams(params);
        tab.setText(categoryName);
        tab.setTextSize(13);
        tab.setPadding(dp(14), dp(6), dp(14), dp(6));
        updateCategoryTab(tab, selected);
        tab.setOnClickListener(v -> {
            selectedCategory = categoryName;
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
                ? R.drawable.bg_tab_active_white
                : R.drawable.bg_tab_inactive_translucent);
        tab.setTextColor(ContextCompat.getColor(this,
                active ? R.color.primary : R.color.white));
        tab.setTypeface(Typeface.create(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL));
    }

    private void resetFilters() {
        edtSearch.setText("");
        selectedCategory = "Tất cả";
        selectedSort = SORT_DEFAULT;
        tvSortLabel.setText("Sắp xếp");
        layoutSortPanel.setVisibility(View.GONE);
        updateSortOptions();
        for (int i = 0; i < layoutCategoryTabs.getChildCount(); i++) {
            updateCategoryTab((TextView) layoutCategoryTabs.getChildAt(i), i == 0);
        }
        applyFilters();
    }

    private void resetSort() {
        selectedSort = SORT_DEFAULT;
        tvSortLabel.setText("Sắp xếp");
        updateSortOptions();
        applyFilters();
    }

    private void applyFilters() {
        String keyword = edtSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        shownProducts.clear();
        for (ProductItem product : allProducts) {
            boolean matchesName = keyword.isEmpty()
                    || product.name.toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesCategory = "Tất cả".equals(selectedCategory)
                    || product.categoryName.equals(selectedCategory);
            if (matchesName && matchesCategory) {
                shownProducts.add(product);
            }
        }

        switch (selectedSort) {
            case SORT_PRICE_ASC:
                Collections.sort(shownProducts, Comparator.comparingDouble(p -> p.price));
                break;
            case SORT_PRICE_DESC:
                Collections.sort(shownProducts,
                        (a, b) -> Double.compare(b.price, a.price));
                break;
            case SORT_SOLD_DESC:
                Collections.sort(shownProducts,
                        (a, b) -> Integer.compare(b.soldQuantity, a.soldQuantity));
                break;
            case SORT_RATING_DESC:
                Collections.sort(shownProducts,
                        (a, b) -> Double.compare(b.averageRating, a.averageRating));
                break;
            case SORT_STOCK_DESC:
                Collections.sort(shownProducts,
                        (a, b) -> Integer.compare(b.stockQuantity, a.stockQuantity));
                break;
            case SORT_NAME_ASC:
                Collections.sort(shownProducts,
                        (a, b) -> a.name.compareToIgnoreCase(b.name));
                break;
            default:
                Collections.sort(shownProducts,
                        (a, b) -> Long.compare(a.id, b.id));
                break;
        }

        productAdapter.notifyDataSetChanged();
        gridProducts.setVisibility(shownProducts.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(shownProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSortOptions() {
        layoutSortOptions.removeAllViews();
        updateSortOptions();
    }

    private void updateSortOptions() {
        layoutSortOptions.removeAllViews();
        String[] options = {
                "Mặc định",
                "Giá tăng dần",
                "Giá giảm dần",
                "Lượt bán giảm dần",
                "Số sao giảm dần",
                "Tồn kho nhiều nhất",
                "Tên A-Z"
        };
        for (int index = 0; index < options.length; index++) {
            final int sortIndex = index;
            TextView option = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / 2, 1f),
                    GridLayout.spec(index % 2, 1f));
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(
                    index % 2 == 0 ? 0 : dp(6),
                    index >= 2 ? dp(6) : 0,
                    0,
                    0);
            option.setLayoutParams(params);
            option.setText(options[index]);
            option.setTextSize(12);
            option.setPadding(dp(10), dp(9), dp(10), dp(9));
            option.setGravity(android.view.Gravity.CENTER);
            option.setMaxLines(1);
            boolean selected = selectedSort == sortIndex;
            option.setBackgroundResource(selected
                    ? R.drawable.bg_tab_active_white
                    : R.drawable.bg_sort_reset);
            option.setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.primary : R.color.text_secondary));
            option.setTypeface(Typeface.create(Typeface.DEFAULT,
                    selected ? Typeface.BOLD : Typeface.NORMAL));
            option.setOnClickListener(v -> {
                selectedSort = sortIndex;
                tvSortLabel.setText(sortIndex == SORT_DEFAULT ? "Sắp xếp" : options[sortIndex]);
                layoutSortPanel.setVisibility(View.GONE);
                updateSortOptions();
                applyFilters();
            });
            layoutSortOptions.addView(option);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}