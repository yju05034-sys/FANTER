package com.fanter.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0005"));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("⚔ FANTER Keyboard");
        title.setTextColor(Color.parseColor("#cc0000"));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.bottomMargin = dp(40);
        title.setLayoutParams(tp);
        root.addView(title);

        addBtn(root, "۱. فعال‌سازی کیبورد",
            v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        space(root);
        addBtn(root, "۲. انتخاب پیش‌فرض",
            v -> ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        space(root);
        addBtn(root, "۳. مجوز نمایش روی صفحه", v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        });
        setContentView(root);
    }

    private void addBtn(LinearLayout p, String txt, android.view.View.OnClickListener cl) {
        Button b = new Button(this);
        b.setText(txt);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor("#ffcccc"));
        b.setTextSize(14);
        b.setBackgroundColor(Color.parseColor("#1a000a"));
        b.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        b.setOnClickListener(cl);
        p.addView(b);
    }

    private void space(LinearLayout p) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(12)));
        p.addView(v);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
