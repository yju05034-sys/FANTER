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

public class FanterIME extends InputMethodService {

    private static final String PREF = "fanter";
    private static final String KEY  = "txt";
    private static final long   DELAY = 10L;

    private boolean  running = false;
    private Handler  handler = new Handler(Looper.getMainLooper());
    private Runnable loop;

    private WindowManager wm;
    private View overlay;
    private TextView btnPlay;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showOverlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception e) {}
        }
    }

    @Override
    public View onCreateInputView() {
        return buildUI();
    }

    private View buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0005"));

        // نوار ذخیره
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(Color.parseColor("#1a000a"));
        bar.setPadding(dp(6), dp(4), dp(6), dp(4));

        TextView lbl = new TextView(this);
        lbl.setText("FANTER");
        lbl.setTextColor(Color.parseColor("#cc0000"));
        lbl.setTypeface(null, Typeface.BOLD);
        lbl.setTextSize(14);
        lbl.setPadding(dp(4), 0, dp(8), 0);
        bar.addView(lbl);

        EditText et = new EditText(this);
        et.setHint("متن برای ارسال...");
        et.setHintTextColor(Color.parseColor("#660020"));
        et.setTextColor(Color.parseColor("#ffcccc"));
        et.setTextSize(12);
        et.setBackgroundColor(Color.parseColor("#200010"));
        et.setPadding(dp(6), dp(4), dp(6), dp(4));
        et.setText(prefs.getString(KEY, ""));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dp(38), 1f);
        ep.setMargins(0, 0, dp(6), 0);
        et.setLayoutParams(ep);
        bar.addView(et);

        TextView save = makeBtn("ذخیره");
        save.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(38)));
        save.setOnClickListener(v -> {
            prefs.edit().putString(KEY, et.getText().toString()).apply();
            Toast.makeText(this, "ذخیره شد", Toast.LENGTH_SHORT).show();
        });
        bar.addView(save);
        root.addView(bar);

        // کیبورد
        String[][] rows = {
            {"۱","۲","۳","۴","۵","۶","۷","۸","۹","۰","⌫"},
            {"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"},
            {"ش","س","ی","ب","ل","ا","ت","ن","م","ک","↵"},
            {"ظ","ط","ز","ر","ذ","د","پ","و","چ","گ"},
            {" "," "," ","فاصله"," "," ","،","."}
        };

        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
            rp.setMargins(dp(2), dp(1), dp(2), dp(1));
            r.setLayoutParams(rp);
            for (String k : row) {
                if (k.equals(" ")) continue;
                r.addView(key(k));
            }
            root.addView(r);
        }
        return root;
    }

    private View key(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setAllCaps(false);
        boolean enter = label.equals("↵");
        boolean bs    = label.equals("⌫");
        boolean space = label.equals("فاصله");
        tv.setTextSize(space ? 10 : 14);
        tv.setTextColor(enter ? Color.parseColor("#ff2222")
                      : bs    ? Color.parseColor("#ff6666")
                      :         Color.parseColor("#ffcccc"));
        tv.setBackgroundColor(enter ? Color.parseColor("#440010")
                                    : Color.parseColor("#1a000a"));
        LinearLayout.LayoutParams lp = space
            ? new LinearLayout.LayoutParams(0, dp(38), 3f)
            : enter || bs
                ? new LinearLayout.LayoutParams(0, dp(38), 1.6f)
                : new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(2), dp(1), dp(2), dp(1));
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            if (bs)    ic.deleteSurroundingText(1, 0);
            else if (enter) ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
            else if (space) ic.commitText(" ", 1);
            else            ic.commitText(label, 1);
        });
        return tv;
    }

    private void showOverlay() {
        FrameLayout fl = new FrameLayout(this);

        LinearLayout ctl = new LinearLayout(this);
        ctl.setOrientation(LinearLayout.HORIZONTAL);
        ctl.setBackgroundColor(Color.parseColor("#CC200010"));
        ctl.setPadding(dp(6), dp(4), dp(6), dp(4));

        btnPlay = makeBtn("▶");
        btnPlay.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(44)));
        btnPlay.setOnClickListener(v -> toggle());
        ctl.addView(btnPlay);

        TextView sw = makeBtn("⇄");
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        sw.setOnClickListener(v ->
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        ctl.addView(sw);

        TextView eye = makeBtn("👁");
        eye.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        eye.setOnClickListener(v -> {
            ctl.setVisibility(View.GONE);
            fl.getChildAt(1).setVisibility(View.VISIBLE);
        });
        ctl.addView(eye);
        fl.addView(ctl);

        TextView ghost = new TextView(this);
        ghost.setText("👁");
        ghost.setTextSize(20);
        ghost.setTextColor(0x33ff3333);
        ghost.setGravity(Gravity.CENTER);
        ghost.setVisibility(View.GONE);
        ghost.setLayoutParams(new FrameLayout.LayoutParams(dp(40), dp(40)));
        ghost.setOnClickListener(v -> {
            ctl.setVisibility(View.VISIBLE);
            ghost.setVisibility(View.GONE);
        });
        fl.addView(ghost);

        overlay = fl;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END;
        p.x = dp(8); p.y = dp(60);

        final int[] last = {0,0};
        fl.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                last[0] = (int)ev.getRawX(); last[1] = (int)ev.getRawY();
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                p.x -= (int)ev.getRawX() - last[0];
                p.y += (int)ev.getRawY() - last[1];
                last[0]=(int)ev.getRawX(); last[1]=(int)ev.getRawY();
                try { wm.updateViewLayout(fl, p); } catch (Exception ignored){}
            }
            return false;
        });

        try { wm.addView(overlay, p); } catch (Exception ignored) {}
    }

    private void toggle() {
        if (running) stop();
        else start();
    }

    private void start() {
        String txt = prefs.getString(KEY, "");
        if (txt.isEmpty()) {
            Toast.makeText(this, "ابتدا متن ذخیره کنید", Toast.LENGTH_SHORT).show();
            return;
        }
        running = true;
        btnPlay.setText("⏹");
        btnPlay.setTextColor(Color.parseColor("#ff4444"));
        loop = new Runnable() {
            @Override public void run() {
                if (!running) return;
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
                    if (et == null || et.text == null || et.text.toString().trim().isEmpty()) {
                        ic.commitText(prefs.getString(KEY, ""), 1);
                    }
                }
                handler.postDelayed(this, DELAY);
            }
        };
        handler.post(loop);
    }

    private void stop() {
        running = false;
        if (loop != null) handler.removeCallbacks(loop);
        if (btnPlay != null) {
            btnPlay.setText("▶");
            btnPlay.setTextColor(Color.parseColor("#ff2222"));
        }
    }

    private TextView makeBtn(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(15);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#ff2222"));
        tv.setBackgroundColor(Color.parseColor("#330010"));
        return tv;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
