package com.example.ecommerce;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private TextView tvPlaceholder;
    private View[] menuItems;
    private ImageView[] menuIcons;
    private TextView[] menuLabels;
    private final String[] tabNames = {"Trang chủ", "Giỏ hàng", "Đơn hàng", "Tôi"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        if (databaseHelper.getSessionUserId() < 0) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvPlaceholder = findViewById(R.id.tvMainPlaceholder);
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
        tvPlaceholder.setText(tabNames[selectedIndex]);
    }
}