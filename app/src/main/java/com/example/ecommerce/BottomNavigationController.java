package com.example.ecommerce;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

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
            activity.startActivity(new Intent(activity, LoginActivity.class));
            return;
        }
        if (index == 3) {
            if (!(activity instanceof AccountActivity)) {
                activity.startActivity(new Intent(activity, AccountActivity.class));
            }
            return;
        }
        String[] names = {"", "Giỏ hàng", "Đơn hàng"};
        Toast.makeText(activity, names[index] + " sẽ được triển khai tiếp",
                Toast.LENGTH_SHORT).show();
    }
    private final MainActivity activity;
    private final DatabaseHelper databaseHelper;
    private final HomeTabController homeController;
    private final AccountTabController accountController;
    private View[] items;
    private ImageView[] icons;
    private TextView[] labels;

    public BottomNavigationController(MainActivity activity,
                                      DatabaseHelper databaseHelper,
                                      HomeTabController homeController,
                                      AccountTabController accountController) {
        this.activity = activity;
        this.databaseHelper = databaseHelper;
        this.homeController = homeController;
        this.accountController = accountController;
    }

    public void setup() {
        items = new View[]{
                activity.findViewById(R.id.menuHome),
                activity.findViewById(R.id.menuCart),
                activity.findViewById(R.id.menuOrders),
                activity.findViewById(R.id.menuAccount)
        };
        icons = new ImageView[]{
                activity.findViewById(R.id.ivMenuHome),
                activity.findViewById(R.id.ivMenuCart),
                activity.findViewById(R.id.ivMenuOrders),
                activity.findViewById(R.id.ivMenuAccount)
        };
        labels = new TextView[]{
                activity.findViewById(R.id.tvMenuHome),
                activity.findViewById(R.id.tvMenuCart),
                activity.findViewById(R.id.tvMenuOrders),
                activity.findViewById(R.id.tvMenuAccount)
        };

        for (int index = 0; index < items.length; index++) {
            final int selectedIndex = index;
            items[index].setOnClickListener(v -> selectTab(selectedIndex));
        }
        selectTab(0);
    }

    private void selectTab(int selectedIndex) {
        int primary = ContextCompat.getColor(activity, R.color.primary);
        int secondary = ContextCompat.getColor(activity, R.color.text_secondary);
        for (int index = 0; index < items.length; index++) {
            boolean selected = index == selectedIndex;
            icons[index].setColorFilter(selected ? primary : secondary);
            labels[index].setTextColor(selected ? primary : secondary);
            labels[index].setTypeface(Typeface.create(
                    Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL));
        }

        if (selectedIndex == 0) {
            homeController.show();
            accountController.hide();
            return;
        }
        if (databaseHelper.getSessionUserId() < 0) {
            activity.startActivity(new Intent(activity, LoginActivity.class));
            return;
        }
        if (selectedIndex == 3) {
            homeController.hide();
            accountController.show();
            return;
        }

        String[] names = {"", "Giỏ hàng", "Đơn hàng", "Trang cá nhân"};
        Toast.makeText(activity, names[selectedIndex] + " sẽ được triển khai tiếp",
                Toast.LENGTH_SHORT).show();
    }
}
