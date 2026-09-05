package com.fanter.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FanterIME extends InputMethodService {

    private static final int    CLR_BLOOD      = 0xFFcc0000;
    private static final int    CLR_BLOOD_LITE = 0xFFff2222;
    private static final int    CLR_DARK       = 0xFF0a0005;
    private static final int    CLR_SURFACE    = 0xFF1a000a;
    private static final int    CLR_TEXT       = 0xFFffcccc;
    private static final long   POLL_MS        = 10L;   // هر ۱۰ میلی‌ثانیه چک
    private static final String PREF_NAME      = "fanter";
    private static final String KEY_TEXT       = "saved_text";

    private volatile boolean    running  = false;
    private String              savedText = "";
    private final Handler       mainHandler = new Handler(Looper.getMainLooper());
    private Runnable            pollRunnable;

    // Overlay
    private WindowManager                wm;
    private View                         overlayRoot;
    private TextView                     btnOnOff;
    private boolean                      controlsVisible = true;
    private WindowManager.LayoutParams   overlayParams;
    private SharedPreferences            prefs;

    // ─── چرخه سرویس ────────────────────────────────────────────────
    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        wm    = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildOverlay();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        stopSending();
        removeOverlay();
    }

    @Override public View onCreateInputView() {
        return buildKeyboardView();
    }

    // ─── کیبورد ────────────────────────────────────────────────────
    private View buildKeyboardView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CLR_DARK);
        root.setPadding(0, dp(2), 0, dp(2));

        // نوار ذخیره
        LinearLayout saveBar = new LinearLayout(this);
        saveBar.setOrientation(LinearLayout.HORIZONTAL);
        saveBar.setBackgroundColor(CLR_SURFACE);
        saveBar.setPadding(dp(8), dp(4), dp(8), dp(4));
        saveBar.setGravity(Gravity.CENTER_VERTICAL);

        BloodTitleView title = new BloodTitleView(this);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(90), dp(36));
        title.setLayoutParams(tLp);
        saveBar.addView(title);

        EditText et = new EditText(this);
        et.setTextColor(CLR_TEXT);
        et.setHintTextColor(0xFF6b0020);
        et.setHint("متن برای ارسال...");
        et.setTextSize(12f);
        et.setSingleLine(false);
        et.setMaxLines(2);
        et.setBackgroundColor(0xFF200010);
        et.setPadding(dp(6), dp(4), dp(6), dp(4));
        savedText = prefs.getString(KEY_TEXT, "");
        et.setText(savedText);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        etLp.setMargins(dp(6), 0, dp(6), 0);
        et.setLayoutParams(etLp);
        saveBar.addView(et);

        TextView saveBtn = makeLabel("ذخیره", CLR_BLOOD, 0xFF330010);
        saveBtn.setTextSize(11f);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(dp(54), dp(36));
        saveBtn.setLayoutParams(saveLp);
        saveBtn.setOnClickListener(v -> {
            savedText = et.getText().toString();
            prefs.edit().putString(KEY_TEXT, savedText).apply();
            Toast.makeText(this, "✓ ذخیره شد", Toast.LENGTH_SHORT).show();
        });
        saveBar.addView(saveBtn);
        root.addView(saveBar);

        // ردیف‌های کیبورد
        String[][] rows = {
            {"۱","۲","۳","۴","۵","۶","۷","۸","۹","۰","⌫"},
            {"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"},
            {"ش","س","ی","ب","ل","ا","ت","ن","م","ک","↵"},
            {"ظ","ط","ز","ر","ذ","د","پ","و","چ","گ"},
            {"فاصله","،","."}
        };
        for (String[] row : rows) {
            LinearLayout rowL = new LinearLayout(this);
            rowL.setOrientation(LinearLayout.HORIZONTAL);
            rowL.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
            rowLp.setMargins(dp(3), dp(1), dp(3), dp(1));
            rowL.setLayoutParams(rowLp);
            for (String k : row) rowL.addView(makeKey(k));
            root.addView(rowL);
        }
        return root;
    }

    private View makeKey(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setAllCaps(false);
        boolean isEnter = label.equals("↵");
        boolean isBS    = label.equals("⌫");
        boolean isSpace = label.equals("فاصله");
        tv.setTextSize(isSpace ? 10f : 14f);
        tv.setTextColor(isEnter ? CLR_BLOOD_LITE : isBS ? 0xFFff6666 : CLR_TEXT);

        android.graphics.drawable.GradientDrawable bg = keyBg(
            isEnter ? 0xFF440010 : CLR_SURFACE,
            isEnter ? CLR_BLOOD  : 0xFF440015);
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = isSpace
            ? new LinearLayout.LayoutParams(0, dp(38), 3f)
            : (isEnter || isBS)
                ? new LinearLayout.LayoutParams(0, dp(38), 1.6f)
                : new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(2), dp(1), dp(2), dp(1));
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            vibrate(15);
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if (isBS)    ic.deleteSurroundingText(1, 0);
            else if (isEnter) ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
            else if (isSpace) ic.commitText(" ", 1);
            else ic.commitText(label, 1);
        });
        tv.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
                v.setBackground(keyBg(isEnter ? 0xFF880020 : 0xFF330015, CLR_BLOOD));
            else if (ev.getAction() == MotionEvent.ACTION_UP ||
                     ev.getAction() == MotionEvent.ACTION_CANCEL)
                v.setBackground(bg);
            return false;
        });
        return tv;
    }

    // ─── Overlay شناور ─────────────────────────────────────────────
    private void buildOverlay() {
        overlayRoot = new FrameLayout(this);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(8), dp(6), dp(8), dp(6));
        android.graphics.drawable.GradientDrawable ctrlBg =
            new android.graphics.drawable.GradientDrawable();
        ctrlBg.setColor(0xCC200010);
        ctrlBg.setCornerRadius(dp(20));
        ctrlBg.setStroke(dp(1), CLR_BLOOD);
        controls.setBackground(ctrlBg);

        // دکمه روشن/خاموش
        btnOnOff = makeLabel("▶", CLR_BLOOD_LITE, 0xFF440010);
        btnOnOff.setTextSize(14f);
        btnOnOff.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(40)));
        btnOnOff.setOnClickListener(v -> toggleSend());
        controls.addView(btnOnOff);
        controls.addView(divider());

        // دکمه تعویض کیبورد
        TextView btnSwitch = makeLabel("⇄", 0xFFffaaaa, 0xFF330010);
        btnSwitch.setTextSize(14f);
        btnSwitch.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnSwitch.setOnClickListener(v ->
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .showInputMethodPicker());
        controls.addView(btnSwitch);
        controls.addView(divider());

        // دکمه چشم
        TextView btnEye = makeLabel("👁", 0xFFffaaaa, 0xFF330010);
        btnEye.setTextSize(14f);
        btnEye.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnEye.setOnClickListener(v -> hideControls());
        controls.addView(btnEye);

        // چشم کم‌رنگ
        TextView ghostEye = makeLabel("👁", 0x33ff3333, 0x00000000);
        ghostEye.setTextSize(20f);
        ghostEye.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        ghostEye.setVisibility(View.GONE);
        ghostEye.setOnClickListener(v -> showControls(controls, ghostEye));

        ((FrameLayout) overlayRoot).addView(controls);
        ((FrameLayout) overlayRoot).addView(ghostEye);

        overlayParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.END;
        overlayParams.x = dp(8);
        overlayParams.y = dp(60);
        makeDraggable(overlayRoot, overlayParams);

        try { wm.addView(overlayRoot, overlayParams); }
        catch (Exception ignored) {}
    }

    private void hideControls() {
        overlayRoot.getChildAt(0).setVisibility(View.GONE);
        overlayRoot.getChildAt(1).setVisibility(View.VISIBLE);
    }

    private void showControls(View controls, View ghost) {
        controls.setVisibility(View.VISIBLE);
        ghost.setVisibility(View.GONE);
    }

    private void removeOverlay() {
        try { if (overlayRoot != null) wm.removeView(overlayRoot); }
        catch (Exception ignored) {}
    }

    // ─── ارسال خودکار ──────────────────────────────────────────────
    private void toggleSend() {
        if (running) stopSending(); else startSending();
    }

    private void startSending() {
        savedText = prefs.getString(KEY_TEXT, "");
        if (savedText.isEmpty()) {
            Toast.makeText(this, "ابتدا متن را ذخیره کنید!", Toast.LENGTH_SHORT).show();
            return;
        }
        running = true;
        updateBtn(true);
        pollRunnable = new Runnable() {
            @Override public void run() {
                if (!running) return;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    android.view.inputmethod.ExtractedText et =
                        ic.getExtractedText(new ExtractedTextRequest(), 0);
                    boolean empty = (et == null || et.text == null ||
                                     et.text.toString().trim().isEmpty());
                    if (empty) ic.commitText(savedText, 1);
                }
                mainHandler.postDelayed(this, POLL_MS);
            }
        };
        mainHandler.post(pollRunnable);
    }

    private void stopSending() {
        running = false;
        if (pollRunnable != null) mainHandler.removeCallbacks(pollRunnable);
        updateBtn(false);
    }

    private void updateBtn(boolean on) {
        mainHandler.post(() -> {
            if (btnOnOff == null) return;
            btnOnOff.setText(on ? "⏹" : "▶");
            btnOnOff.setTextColor(on ? 0xFFff4444 : CLR_BLOOD_LITE);
            btnOnOff.setBackground(keyBg(on ? 0xFF550000 : 0xFF440010,
                                         on ? 0xFFff0000 : CLR_BLOOD));
        });
    }

    // ─── کمکی ──────────────────────────────────────────────────────
    private void vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        } catch (Exception ignored) {}
    }

    private TextView makeLabel(String text, int textColor, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(textColor);
        tv.setTextSize(14f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setBackground(keyBg(bgColor, textColor));
        return tv;
    }

    private android.graphics.drawable.GradientDrawable keyBg(int color, int stroke) {
        android.graphics.drawable.GradientDrawable bg =
            new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(5));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private View divider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(28));
        lp.setMargins(dp(4), 0, dp(4), 0);
        v.setLayoutParams(lp);
        v.setBackgroundColor(0xFF550020);
        return v;
    }

    private void makeDraggable(View view, WindowManager.LayoutParams params) {
        final int[] last = {0, 0};
        final boolean[] moved = {false};
        view.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    last[0] = (int) ev.getRawX();
                    last[1] = (int) ev.getRawY();
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) ev.getRawX() - last[0];
                    int dy = (int) ev.getRawY() - last[1];
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) moved[0] = true;
                    params.x -= dx; params.y += dy;
                    last[0] = (int) ev.getRawX();
                    last[1] = (int) ev.getRawY();
                    try { wm.updateViewLayout(view, params); } catch (Exception ignored) {}
                    return true;
                case MotionEvent.ACTION_UP:
                    return moved[0];
            }
            return false;
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
