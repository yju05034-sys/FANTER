package com.fanter.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class IME extends InputMethodService {

    // ── تنظیمات سرعت ──────────────────────────────────────────────
    // هر ۸ میلی‌ثانیه چک میکنه = بالای ۱۲۰ بار در ثانیه
    private static final long POLL = 8L;

    private static final String PREF = "fp";
    private static final String KEY  = "t";

    // ── وضعیت ─────────────────────────────────────────────────────
    private boolean         on      = false;
    private final Handler   h       = new Handler(Looper.getMainLooper());
    private Runnable        loop;
    private SharedPreferences prefs;

    // ── overlay ───────────────────────────────────────────────────
    private WindowManager wm;
    private View          overlay;
    private TextView      btnToggle;
    private LinearLayout  ctrlPanel;
    private TextView      ghostBtn;

    // ── رنگ‌ها ────────────────────────────────────────────────────
    private static final int C_BG     = 0xFF0a0005;
    private static final int C_SURF   = 0xFF1a000a;
    private static final int C_KEY    = 0xFF220010;
    private static final int C_RED    = 0xFFcc0000;
    private static final int C_RED_L  = 0xFFff2222;
    private static final int C_TEXT   = 0xFFffcccc;
    private static final int C_DIM    = 0xFF550020;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildOverlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        try { if (overlay != null) wm.removeView(overlay); } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════════════
    //  کیبورد
    // ════════════════════════════════════════════════════════════════
    @Override
    public View onCreateInputView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(C_BG);

        // ── نوار بالا (ذخیره متن) ──────────────────────────────
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(C_SURF);
        bar.setPadding(px(6), px(3), px(6), px(3));
        bar.setGravity(Gravity.CENTER_VERTICAL);

        // لوگو
        TextView logo = new TextView(this);
        logo.setText("⚔FANTER");
        logo.setTextColor(C_RED_L);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setTextSize(13);
        logo.setPadding(0, 0, px(8), 0);
        bar.addView(logo);

        // فیلد متن
        final EditText et = new EditText(this);
        et.setHint("متن برای ارسال خودکار...");
        et.setHintTextColor(0xFF550020);
        et.setTextColor(C_TEXT);
        et.setTextSize(12);
        et.setSingleLine(false);
        et.setMaxLines(2);
        et.setBackgroundColor(0xFF180010);
        et.setPadding(px(6), px(3), px(6), px(3));
        et.setText(prefs.getString(KEY, ""));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, px(40), 1f);
        ep.setMargins(0, 0, px(5), 0);
        et.setLayoutParams(ep);
        bar.addView(et);

        // دکمه ذخیره
        TextView save = btn("💾", 0);
        save.setTextSize(18);
        save.setLayoutParams(new LinearLayout.LayoutParams(px(44), px(40)));
        save.setOnClickListener(v -> {
            prefs.edit().putString(KEY, et.getText().toString()).apply();
            Toast.makeText(this, "✓ ذخیره شد", Toast.LENGTH_SHORT).show();
        });
        bar.addView(save);
        root.addView(bar);

        // ── ردیف‌های کیبورد ────────────────────────────────────
        addRow(root, new String[]{"۱","۲","۳","۴","۵","۶","۷","۸","۹","۰","⌫"});
        addRow(root, new String[]{"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"});
        addRow(root, new String[]{"ش","س","ی","ب","ل","ا","ت","ن","م","ک","↵"});
        addRow(root, new String[]{"ظ","ط","ز","ر","ذ","د","پ","و","چ","گ"});
        addRow(root, new String[]{"،",".","فاصله","!"});

        return root;
    }

    private void addRow(LinearLayout root, String[] keys) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(42));
        rp.setMargins(px(2), px(1), px(2), px(1));
        row.setLayoutParams(rp);
        for (String k : keys) row.addView(makeKey(k));
        root.addView(row);
    }

    private View makeKey(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setAllCaps(false);

        boolean isEnter = "↵".equals(label);
        boolean isBs    = "⌫".equals(label);
        boolean isSpace = "فاصله".equals(label);

        tv.setTextSize(isSpace ? 10 : 14);
        tv.setTextColor(isEnter ? C_RED_L : isBs ? 0xFFff6666 : C_TEXT);
        tv.setTypeface(null, isEnter ? Typeface.BOLD : Typeface.NORMAL);

        // پس‌زمینه
        android.graphics.drawable.GradientDrawable bg = roundRect(
                isEnter ? 0xFF3a0010 : C_KEY, isEnter ? C_RED : C_DIM, px(5));
        tv.setBackground(bg);

        // سایز
        float w = isSpace ? 3f : (isEnter || isBs) ? 1.7f : 1f;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(38), w);
        lp.setMargins(px(2), px(1), px(2), px(1));
        tv.setLayoutParams(lp);

        // کلیک
        tv.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if (isBs)         ic.deleteSurroundingText(1, 0);
            else if (isEnter) ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
            else if (isSpace) ic.commitText(" ", 1);
            else              ic.commitText(label, 1);
        });

        // افکت لمس
        tv.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
                v.setBackground(roundRect(isEnter ? 0xFF660020 : 0xFF330018, C_RED_L, px(5)));
            else if (ev.getAction() == MotionEvent.ACTION_UP
                  || ev.getAction() == MotionEvent.ACTION_CANCEL)
                v.setBackground(bg);
            return false;
        });

        return tv;
    }

    // ════════════════════════════════════════════════════════════════
    //  دکمه شناور (Overlay)
    // ════════════════════════════════════════════════════════════════
    private void buildOverlay() {
        FrameLayout root = new FrameLayout(this);

        // ── پنل کنترل‌ها ──────────────────────────────────────
        ctrlPanel = new LinearLayout(this);
        ((LinearLayout) ctrlPanel).setOrientation(LinearLayout.HORIZONTAL);
        ctrlPanel.setBackgroundColor(0xDD1a000a);
        ctrlPanel.setPadding(px(5), px(4), px(5), px(4));
        ((LinearLayout) ctrlPanel).setGravity(Gravity.CENTER_VERTICAL);

        // ── دکمه روشن/خاموش ───────────────────────────────────
        btnToggle = btn("▶", 0xFF3a0010);
        btnToggle.setTextSize(16);
        btnToggle.setLayoutParams(new LinearLayout.LayoutParams(px(56), px(48)));
        btnToggle.setOnClickListener(v -> toggle());
        ((LinearLayout) ctrlPanel).addView(btnToggle);

        ((LinearLayout) ctrlPanel).addView(divider());

        // ── دکمه تعویض کیبورد ─────────────────────────────────
        TextView sw = btn("⇄", 0xFF2a0010);
        sw.setLayoutParams(new LinearLayout.LayoutParams(px(46), px(48)));
        sw.setOnClickListener(v ->
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showInputMethodPicker());
        ((LinearLayout) ctrlPanel).addView(sw);

        ((LinearLayout) ctrlPanel).addView(divider());

        // ── دکمه چشم ──────────────────────────────────────────
        TextView eyeBtn = btn("👁", 0xFF200008);
        eyeBtn.setLayoutParams(new LinearLayout.LayoutParams(px(46), px(48)));
        eyeBtn.setOnClickListener(v -> {
            ctrlPanel.setVisibility(View.GONE);
            ghostBtn.setVisibility(View.VISIBLE);
        });
        ((LinearLayout) ctrlPanel).addView(eyeBtn);

        root.addView(ctrlPanel);

        // ── چشم کم‌رنگ (ghost) ────────────────────────────────
        ghostBtn = new TextView(this);
        ghostBtn.setText("👁");
        ghostBtn.setTextSize(22);
        ghostBtn.setTextColor(0x44ff2222);
        ghostBtn.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(px(38), px(38));
        ghostBtn.setLayoutParams(gp);
        ghostBtn.setVisibility(View.GONE);
        ghostBtn.setOnClickListener(v -> {
            ghostBtn.setVisibility(View.GONE);
            ctrlPanel.setVisibility(View.VISIBLE);
        });
        root.addView(ghostBtn);

        overlay = root;

        // ── WindowManager params ───────────────────────────────
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = px(8);
        p.y = px(56);

        // درگ
        final int[] last = {0, 0};
        final boolean[] moved = {false};
        root.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    last[0] = (int) ev.getRawX();
                    last[1] = (int) ev.getRawY();
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) ev.getRawX() - last[0];
                    int dy = (int) ev.getRawY() - last[1];
                    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) moved[0] = true;
                    p.x -= dx;
                    p.y += dy;
                    last[0] = (int) ev.getRawX();
                    last[1] = (int) ev.getRawY();
                    try { wm.updateViewLayout(overlay, p); } catch (Exception ignored) {}
                    return true;
                case MotionEvent.ACTION_UP:
                    return moved[0];
            }
            return false;
        });

        try { wm.addView(overlay, p); } catch (Exception ignored) {}
    }

    // ════════════════════════════════════════════════════════════════
    //  منطق ارسال خودکار
    // ════════════════════════════════════════════════════════════════
    private void toggle() {
        if (on) stop(); else start();
    }

    private void start() {
        String txt = prefs.getString(KEY, "");
        if (txt.isEmpty()) {
            Toast.makeText(this, "⚠ ابتدا متن ذخیره کنید", Toast.LENGTH_SHORT).show();
            return;
        }
        on = true;
        setToggleUI(true);

        loop = new Runnable() {
            @Override
            public void run() {
                if (!on) return;
                try {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
                        String cur = (et != null && et.text != null)
                                ? et.text.toString().trim() : "";
                        if (cur.isEmpty()) {
                            ic.commitText(prefs.getString(KEY, ""), 1);
                        }
                    }
                } catch (Exception ignored) {}
                h.postDelayed(this, POLL);
            }
        };
        h.post(loop);
    }

    private void stop() {
        on = false;
        if (loop != null) h.removeCallbacks(loop);
        setToggleUI(false);
    }

    private void setToggleUI(boolean active) {
        h.post(() -> {
            if (btnToggle == null) return;
            btnToggle.setText(active ? "⏹" : "▶");
            btnToggle.setTextColor(active ? 0xFFff4444 : C_RED_L);
            btnToggle.setBackground(roundRect(
                    active ? 0xFF660000 : 0xFF3a0010,
                    active ? 0xFFff0000 : C_RED, px(6)));
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  ابزار
    // ════════════════════════════════════════════════════════════════
    private TextView btn(String text, int bg) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(C_RED_L);
        tv.setBackground(roundRect(bg, C_RED, px(6)));
        return tv;
    }

    private View divider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(1), px(32));
        lp.setMargins(px(4), 0, px(4), 0);
        v.setLayoutParams(lp);
        v.setBackgroundColor(C_DIM);
        return v;
    }

    private android.graphics.drawable.GradientDrawable roundRect(int color, int stroke, int r) {
        android.graphics.drawable.GradientDrawable d =
                new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(r);
        d.setStroke(2, stroke);
        return d;
    }

    private int px(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
