package com.example.ecommerce;

import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class BottomNavigationController {
    public static void setupForActivity(AppCompatActivity activity, int selectedIndex) {
        DatabaseHelper databaseHelper = new DatabaseHelper(activity);
        View[] items = new View[]{
                activity.findViewById(R.id.menuHome),
                activity.findViewById(R.id.menuCart),
                activity.findViewById(R.id.menuOrders),
                activity.findViewById(R.id.menuAccount)
        };
        ImageView[] icons = new ImageView[]{
                activity.findViewById(R.id.ivMenuHome),
                activity.findViewById(R.id.ivMenuCart),
                activity.findViewById(R.id.ivMenuOrders),
                activity.findViewById(R.id.ivMenuAccount)
        };
        TextView[] labels = new TextView[]{
                activity.findViewById(R.id.tvMenuHome),
                activity.findViewById(R.id.tvMenuCart),
                activity.findViewById(R.id.tvMenuOrders),
                activity.findViewById(R.id.tvMenuAccount)
        };

        int primary = ContextCompat.getColor(activity, R.color.primary);
        int secondary = ContextCompat.getColor(activity, R.color.text_secondary);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            boolean selected = i == selectedIndex;
            icons[i].setColorFilter(selected ? primary : secondary);
            labels[i].setTextColor(selected ? primary : secondary);
            labels[i].setTypeface(Typeface.create(
                    Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL));
            items[i].setOnClickListener(v -> navigate(activity, databaseHelper, index));
        }
    }

    private static void navigate(AppCompatActivity activity,
                                 DatabaseHelper databaseHelper, int index) {
        if (index == 0) {
            if (!(activity instanceof HomeActivity)) {
                activity.startActivity(new Intent(activity, HomeActivity.class));
            }
            return;
        }
        if (databaseHelper.getSessionUserId() < 0) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_LOGIN_REQUIRED, true);
            activity.startActivity(intent);
            return;
        }
        if (index == 3) {
            if (!(activity instanceof AccountActivity)) {
                activity.startActivity(new Intent(activity, AccountActivity.class));
            }
            return;
        }
        if (index == 1) {
            if (!(activity instanceof CartActivity)) {
                activity.startActivity(new Intent(activity, CartActivity.class));
            }
            return;
        }
        if (index == 2) {
            if (!(activity instanceof OrdersActivity)) {
                activity.startActivity(new Intent(activity, OrdersActivity.class));
            }
            return;
        }
    }
}
