package com.example.ecommerce;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class StatusBarUtils {
    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        if (result <= 0) {
            result = dpToPx(context, 24);
        }
        return result;
    }

    public static void setupHeader(Activity activity, View headerView) {
        if (activity == null || headerView == null) {
            return;
        }

        WindowCompat.enableEdgeToEdge(activity.getWindow());
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }

        int viewId = headerView.getId();
        boolean isHomeHeader = viewId == R.id.layoutHomeHeader;
        boolean isAccountHeader = viewId == R.id.layoutAccountHeader;

        int defaultContentHeight = dpToPx(activity, 50);
        int homeTopExtra = dpToPx(activity, 8);
        int homeBottomPadding = dpToPx(activity, 10);

        applyHeaderInsets(headerView, getStatusBarHeight(activity),
                isHomeHeader, isAccountHeader, defaultContentHeight, homeTopExtra, homeBottomPadding);

        ViewCompat.setOnApplyWindowInsetsListener(headerView, (view, insets) -> {
            Insets statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int top = statusInsets.top > 0 ? statusInsets.top : getStatusBarHeight(activity);
            applyHeaderInsets(view, top,
                    isHomeHeader, isAccountHeader, defaultContentHeight, homeTopExtra, homeBottomPadding);
            return insets;
        });

        ViewCompat.requestApplyInsets(headerView);
    }

    private static void applyHeaderInsets(View headerView, int statusBarHeight,
                                          boolean isHomeHeader, boolean isAccountHeader,
                                          int defaultContentHeight, int homeTopExtra, int homeBottomPadding) {
        if (isHomeHeader) {
            headerView.setPadding(
                    headerView.getPaddingLeft(),
                    statusBarHeight + homeTopExtra,
                    headerView.getPaddingRight(),
                    homeBottomPadding
            );
        } else if (isAccountHeader) {
            headerView.setPadding(
                    headerView.getPaddingLeft(),
                    statusBarHeight,
                    headerView.getPaddingRight(),
                    headerView.getPaddingBottom()
            );
        } else {
            ViewGroup.LayoutParams lp = headerView.getLayoutParams();
            if (lp != null) {
                lp.height = statusBarHeight + defaultContentHeight;
                headerView.setLayoutParams(lp);
            }
            headerView.setPadding(
                    headerView.getPaddingLeft(),
                    statusBarHeight,
                    headerView.getPaddingRight(),
                    0
            );
        }
    }

    public static void setupBottomNav(Activity activity, View bottomView) {
        if (bottomView == null) {
            return;
        }

        int defaultBottomHeight = dpToPx(activity, 56);

        ViewCompat.setOnApplyWindowInsetsListener(bottomView, (view, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int bottom = navInsets.bottom;
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.height = defaultBottomHeight + bottom;
                view.setLayoutParams(lp);
            }
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    bottom
            );
            return insets;
        });

        ViewCompat.requestApplyInsets(bottomView);
    }
}
