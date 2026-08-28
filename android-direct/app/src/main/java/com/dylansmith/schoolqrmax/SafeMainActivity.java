package com.dylansmith.schoolqrmax;

import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

public class SafeMainActivity extends MainActivity {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        final View content = findViewById(android.R.id.content);
        if (content == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            content.setOnApplyWindowInsetsListener((v, insets) -> {
                int left = insets.getSystemWindowInsetLeft();
                int top = insets.getSystemWindowInsetTop();
                int right = insets.getSystemWindowInsetRight();
                int bottom = insets.getSystemWindowInsetBottom();
                v.setPadding(left, top, right, bottom);
                return insets;
            });
            content.requestApplyInsets();
        }
    }
}
