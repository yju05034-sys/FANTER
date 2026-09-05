package com.fanter.keyboard;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FanterIME extends InputMethodService {

    // ═══════════════════════════════════════════════════════════
    //  ثابت‌ها
    // ═══════════════════════════════════════════════════════════
    private static final int CLR_BLOOD      = 0xFFcc0000;
    private static final int CLR_BLOOD_DARK = 0xFF880000;
    private static final int CLR_BLOOD_LITE = 0xFFff2222;
    private static final int CLR_DARK       = 0xFF0a0005;
    private static final int CLR_SURFACE    = 0xFF1a000a;
    private static final int CLR_TEXT       = 0xFFffcccc;

    // تأخیر ارسال — هر ۱۰ میلی‌ثانیه (۱۰۰ بار در ثانیه)
    private static final long CHAR_DELAY_MS  = 10L;
    // تأخیر بعد از اینتر — تقریباً صفر
    private static final long ENTER_DELAY_MS = 2L;

    private static final String PREF_NAME = "fanter";
    private static final String KEY_TEXT  = "saved_text";

    // ═══════════════════════════════════════════════════════════
    //  وضعیت
    // ═══════════════════════════════════════════════════════════
    private volatile boolean running = false;
    private String           savedText = "";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable sendRunnable;

    // ═══════════════════════════════════════════════════════════
    //  دکمه شناور روی صفحه
    // ═══════════════════════════════════════════════════════════
    private WindowManager wm;
    private View          overlayRoot;
    private TextView      btnOnOff;
    private View          btnSwitch;
    private View          btnEye;
    private View          ghostEye;       // چشم کم‌رنگ وقتی مخفی‌ان
    private boolean       controlsVisible = true;

    private SharedPreferences prefs;

    // ═══════════════════════════════════════════════════════════
    //  چرخه زندگی سرویس
    // ═══════════════════════════════════════════════════════════
    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        wm    = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildOverlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSending();
        removeOverlay();
    }

    @Override
    public View onCreateInputView() {
        return buildKeyboardView();
    }

    // ═══════════════════════════════════════════════════════════
    //  ساخت کیبورد (بدون سایز — مطابق تصویر نالز کلش)
    // ═══════════════════════════════════════════════════════════
    private View buildKeyboardView() {
        // پس‌زمینه اصلی
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CLR_DARK);
        root.setPadding(0, dp(2), 0, dp(2));

        // ── نوار ذخیره متن ──────────────────────────────────
        LinearLayout saveBar = new LinearLayout(this);
        saveBar.setOrientation(LinearLayout.HORIZONTAL);
        saveBar.setBackgroundColor(CLR_SURFACE);
        saveBar.setPadding(dp(8), dp(4), dp(8), dp(4));
        saveBar.setGravity(Gravity.CENTER_VERTICAL);

        // عنوان خونی FANTER
        BloodTitleView title = new BloodTitleView(this);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(dp(90), dp(36));
        title.setLayoutParams(titleLp);
        saveBar.addView(title);

        // فیلد متن
        EditText et = new EditText(this);
        et.setTextColor(CLR_TEXT);
        et.setHintTextColor(0xFF6b0020);
        et.setHint("متن برای ارسال...");
        et.setTextSize(12f);
        et.setSingleLine(false);
        et.setMaxLines(2);
        et.setBackgroundColor(0xFF200010);
        et.setPadding(dp(6), dp(4), dp(6), dp(4));
        et.setTypeface(Typeface.DEFAULT);
        savedText = prefs.getString(KEY_TEXT, "");
        et.setText(savedText);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        etLp.setMargins(dp(6), 0, dp(6), 0);
        et.setLayoutParams(etLp);
        saveBar.addView(et);

        // دکمه ذخیره
        TextView saveBtn = makeTextBtn("ذخیره", CLR_BLOOD, 0xFF330010);
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

        // ── ردیف‌های کیبورد ─────────────────────────────────
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

            for (String k : row) {
                rowL.addView(makeKey(k, row.length));
            }
            root.addView(rowL);
        }

        return root;
    }

    private View makeKey(String label, int rowLen) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(CLR_TEXT);
        tv.setAllCaps(false);

        boolean isEnter = label.equals("↵");
        boolean isBS    = label.equals("⌫");
        boolean isSpace = label.equals("فاصله");

        tv.setTextSize(isSpace ? 10f : 14f);
        tv.setTextColor(isEnter ? CLR_BLOOD_LITE : isBS ? 0xFFff6666 : CLR_TEXT);

        // پس‌زمینه گرد
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(5));
        if (isEnter) {
            bg.setColor(0xFF440010);
            bg.setStroke(dp(1), CLR_BLOOD);
        } else {
            bg.setColor(CLR_SURFACE);
            bg.setStroke(dp(1), 0xFF440015);
        }
        tv.setBackground(bg);

        // سایز
        LinearLayout.LayoutParams lp;
        if (isSpace) {
            lp = new LinearLayout.LayoutParams(0, dp(38), 3f);
        } else if (isEnter || isBS) {
            lp = new LinearLayout.LayoutParams(0, dp(38), 1.6f);
        } else {
            lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        }
        lp.setMargins(dp(2), dp(1), dp(2), dp(1));
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            vibrate(18);
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if (isBS) {
                ic.deleteSurroundingText(1, 0);
            } else if (isEnter) {
                ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
            } else if (isSpace) {
                ic.commitText(" ", 1);
            } else {
                ic.commitText(label, 1);
            }
        });

        tv.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                android.graphics.drawable.GradientDrawable press =
                    new android.graphics.drawable.GradientDrawable();
                press.setCornerRadius(dp(5));
                press.setColor(isEnter ? 0xFF880020 : 0xFF330015);
                press.setStroke(dp(1), CLR_BLOOD);
                v.setBackground(press);
            } else if (ev.getAction() == MotionEvent.ACTION_UP ||
                       ev.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setBackground(bg);
            }
            return false;
        });

        return tv;
    }

    // ═══════════════════════════════════════════════════════════
    //  Overlay شناور — روشن/خاموش + تعویض + چشم
    // ═══════════════════════════════════════════════════════════
    private WindowManager.LayoutParams overlayParams;

    private void buildOverlay() {
        // کانتینر اصلی
        overlayRoot = new FrameLayout(this);

        // کانتینر دکمه‌های اصلی (روشن/خاموش + تعویض + چشم)
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(8), dp(6), dp(8), dp(6));

        // پس‌زمینه شیشه‌ای خونی
        android.graphics.drawable.GradientDrawable ctrlBg =
            new android.graphics.drawable.GradientDrawable();
        ctrlBg.setColor(0xCC200010);
        ctrlBg.setCornerRadius(dp(20));
        ctrlBg.setStroke(dp(1), CLR_BLOOD);
        controls.setBackground(ctrlBg);

        // ── دکمه روشن/خاموش ─────────────────────────────────
        btnOnOff = makeTextBtn("▶", CLR_BLOOD_LITE, 0xFF440010);
        btnOnOff.setTextSize(14f);
        LinearLayout.LayoutParams ooLp = new LinearLayout.LayoutParams(dp(48), dp(40));
        btnOnOff.setLayoutParams(ooLp);
        btnOnOff.setOnClickListener(v -> toggleSend());
        controls.addView(btnOnOff);

        // جداکننده
        controls.addView(makeDivider());

        // ── دکمه تعویض کیبورد ───────────────────────────────
        btnSwitch = makeTextBtn("⇄", 0xFFffaaaa, 0xFF330010);
        ((TextView)btnSwitch).setTextSize(14f);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        btnSwitch.setLayoutParams(swLp);
        btnSwitch.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showInputMethodPicker();
        });
        controls.addView(btnSwitch);

        controls.addView(makeDivider());

        // ── دکمه چشم (مخفی/نمایان) ──────────────────────────
        btnEye = makeTextBtn("👁", 0xFFffaaaa, 0xFF330010);
        ((TextView)btnEye).setTextSize(14f);
        LinearLayout.LayoutParams eyeLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        btnEye.setLayoutParams(eyeLp);
        btnEye.setOnClickListener(v -> hideControls());
        controls.addView(btnEye);

        // ── چشم کم‌رنگ (وقتی کنترل‌ها مخفی‌اند) ────────────
        ghostEye = makeTextBtn("👁", 0x33ff3333, 0x00000000);
        ((TextView)ghostEye).setTextSize(18f);
        LinearLayout.LayoutParams geLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        ghostEye.setLayoutParams(geLp);
        ghostEye.setVisibility(View.GONE);
        ghostEye.setOnClickListener(v -> showControls());

        FrameLayout.LayoutParams ctrlFLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        ((FrameLayout) overlayRoot).addView(controls, ctrlFLp);
        ((FrameLayout) overlayRoot).addView(ghostEye, ctrlFLp);

        // ── WindowManager params ─────────────────────────────
        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.END;
        overlayParams.x = dp(8);
        overlayParams.y = dp(60);

        // درگ
        makeDraggable(overlayRoot, overlayParams);

        try {
            wm.addView(overlayRoot, overlayParams);
        } catch (Exception e) {
            // مجوز overlay نداده — توی MainActivity می‌گیریم
        }
    }

    private void hideControls() {
        controlsVisible = false;
        // کانتینر کنترل‌ها (فرزند اول) مخفی
        overlayRoot.getChildAt(0).setVisibility(View.GONE);
        ghostEye.setVisibility(View.VISIBLE);
    }

    private void showControls() {
        controlsVisible = true;
        overlayRoot.getChildAt(0).setVisibility(View.VISIBLE);
        ghostEye.setVisibility(View.GONE);
    }

    private void removeOverlay() {
        try {
            if (overlayRoot != null) wm.removeView(overlayRoot);
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════
    //  ارسال خودکار — قلب سیستم
    // ═══════════════════════════════════════════════════════════
    private void toggleSend() {
        if (running) stopSending();
        else         startSending();
    }

    private void startSending() {
        savedText = prefs.getString(KEY_TEXT, "");
        if (savedText.isEmpty()) {
            Toast.makeText(this, "ابتدا متن را ذخیره کنید!", Toast.LENGTH_SHORT).show();
            return;
        }
        running = true;
        updateBtnState(true);
        scheduleTextInsert();
    }

    private void stopSending() {
        running = false;
        mainHandler.removeCallbacks(sendRunnable);
        updateBtnState(false);
    }

    /**
     * هر بار که این تابع صدا زده میشه:
     * 1) متن کامل رو یه‌باره commit میکنه
     * 2) منتظر میمونه کاربر Enter بزنه
     * 3) بعد از Enter (که کاربر میزنه) — خودش فوری دوباره متن میاره
     *
     * ولی چون کاربر Enter رو میزنه ما نمیتونیم دقیق لحظه‌ش رو بگیریم،
     * پس از روش polling استفاده میکنیم:
     * هر CHAR_DELAY_MS چک میکنیم اگه چت باز بود و متن خالی بود دوباره بریز.
     */
    private void scheduleTextInsert() {
        sendRunnable = new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    // وضعیت متن فعلی بررسی کن
                    CharSequence cur = ic.getExtractedText(
                        new android.view.inputmethod.ExtractedTextRequest(), 0) != null
                        ? ic.getExtractedText(new android.view.inputmethod.ExtractedTextRequest(), 0).text
                        : null;

                    boolean isEmpty = (cur == null || cur.toString().trim().isEmpty());
                    if (isEmpty) {
                        // باکس خالیه — متن رو بریز
                        ic.commitText(prefs.getString(KEY_TEXT, savedText), 1);
                    }
                }
                // هر ۱۰ میلی‌ثانیه چک کن
                mainHandler.postDelayed(this, CHAR_DELAY_MS);
            }
        };
        mainHandler.post(sendRunnable);
    }

    private void updateBtnState(boolean on) {
        mainHandler.post(() -> {
            if (btnOnOff == null) return;
            btnOnOff.setText(on ? "⏹" : "▶");
            btnOnOff.setTextColor(on ? 0xFFff4444 : CLR_BLOOD_LITE);
            android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(on ? 0xFF550000 : 0xFF440010);
            bg.setStroke(dp(1), on ? 0xFFff0000 : CLR_BLOOD);
            btnOnOff.setBackground(bg);
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  کمکی‌ها
    // ═══════════════════════════════════════════════════════════
    private void vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(ms);
        } catch (Exception ignored) {}
    }

    private TextView makeTextBtn(String text, int textColor, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(textColor);
        tv.setTextSize(14f);
        tv.setTypeface(null, Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg =
            new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(8));
        tv.setBackground(bg);
        return tv;
    }

    private View makeDivider() {
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
                    params.x -= dx;
                    params.y += dy;
                    last[0] = (int) ev.getRawX();
                    last[1] = (int) ev.getRawY();
                    try { wm.updateViewLayout(view, params); } catch (Exception ignored) {}
                    return true;
                case MotionEvent.ACTION_UP:
                    return moved[0]; // اگه حرکت نکرد، کلیک به فرزند میرسه
            }
            return false;
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
