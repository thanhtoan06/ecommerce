package com.example.ecommerce;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    private HomeScreenController homeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        StatusBarUtils.setupHeader(this, findViewById(R.id.layoutHomeHeader));
        StatusBarUtils.setupBottomNav(this, findViewById(R.id.bottomNavigation));

        homeController = new HomeScreenController(this, findViewById(android.R.id.content));
        BottomNavigationController.setupForActivity(this, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (homeController != null) {
            homeController.refreshHeader();
        }
    }
}
