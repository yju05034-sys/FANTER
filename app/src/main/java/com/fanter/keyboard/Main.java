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

public class Main extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0005"));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(px(24), px(56), px(24), px(24));

        TextView t = new TextView(this);
        t.setText("⚔ FANTER");
        t.setTextColor(Color.parseColor("#cc0000"));
        t.setTextSize(28);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.bottomMargin = px(8);
        t.setLayoutParams(tp);
        root.addView(t);

        TextView sub = new TextView(this);
        sub.setText("Clash of Clans Keyboard");
        sub.setTextColor(Color.parseColor("#660020"));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.bottomMargin = px(44);
        sub.setLayoutParams(sp);
        root.addView(sub);

        addBtn(root, "۱. فعال‌سازی کیبورد",
                v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        space(root, 10);
        addBtn(root, "۲. انتخاب به‌عنوان پیش‌فرض",
                v -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                        .showInputMethodPicker());
        space(root, 10);
        addBtn(root, "۳. مجوز نمایش روی صفحه (الزامی)",
                v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())));
                });

        space(root, 36);

        TextView note = new TextView(this);
        note.setText("بعد از ۳ مرحله بالا:\n" +
                "• وارد کلش شوید\n" +
                "• روی چت کلیک کنید\n" +
                "• متن را ذخیره کنید\n" +
                "• دکمه ▶ شناور را بزنید");
        note.setTextColor(Color.parseColor("#550020"));
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        note.setLineSpacing(px(3), 1f);
        root.addView(note);

        setContentView(root);
    }

    private void addBtn(LinearLayout p, String txt, android.view.View.OnClickListener cl) {
        Button b = new Button(this);
        b.setText(txt);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor("#ffcccc"));
        b.setTextSize(14);
        b.setPadding(px(16), px(14), px(16), px(14));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#1a000a"));
        bg.setCornerRadius(px(10));
        bg.setStroke(px(1), Color.parseColor("#cc0000"));
        b.setBackground(bg);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        b.setOnClickListener(cl);
        p.addView(b);
    }

    private void space(LinearLayout p, int dp) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, px(dp)));
        p.addView(v);
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
