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

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0a0005);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        // عنوان متحرک
        BloodTitleView title = new BloodTitleView(this);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(180), dp(60));
        tLp.gravity = Gravity.CENTER_HORIZONTAL;
        tLp.bottomMargin = dp(8);
        title.setLayoutParams(tLp);
        root.addView(title);

        // زیرنویس
        TextView sub = new TextView(this);
        sub.setText("Clash of Clans Edition");
        sub.setTextColor(0xFF880000);
        sub.setTextSize(12f);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.bottomMargin = dp(40);
        sub.setLayoutParams(subLp);
        root.addView(sub);

        // دکمه‌های راه‌اندازی
        addStep(root, "۱. فعال‌سازی کیبورد",
            v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        space(root, 12);

        addStep(root, "۲. انتخاب به‌عنوان پیش‌فرض",
            v -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showInputMethodPicker());

        space(root, 12);

        addStep(root, "۳. مجوز نمایش روی صفحه (OVERLAY)",
            v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                }
            });

        space(root, 36);

        TextView note = new TextView(this);
        note.setText(
            "پس از انجام ۳ مرحله بالا:\n" +
            "۱) وارد کلش اف کلنز شوید\n" +
            "۲) روی چت‌باکس بزنید — کیبورد FANTER ظاهر میشه\n" +
            "۳) متن را ذخیره کنید\n" +
            "۴) دکمه ▶ شناور را بزنید — شروع خودکار!\n" +
            "۵) هر بار که ارسال میزنید چت‌باکس فوری پر میشه"
        );
        note.setTextColor(0xFF6b0020);
        note.setTextSize(12f);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(dp(2), 1f);
        root.addView(note);

        setContentView(root);
    }

    private void addStep(LinearLayout parent, String text, android.view.View.OnClickListener cl) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(0xFFffcccc);
        b.setTextSize(14f);
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF1a000a);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0xFFcc0000);
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(lp);
        b.setOnClickListener(cl);
        parent.addView(b);
    }

    private void space(LinearLayout p, int dp) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        p.addView(v);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
