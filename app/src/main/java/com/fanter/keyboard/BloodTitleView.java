package com.fanter.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * نمای متحرک عنوان FANTER با افکت قطره‌های خون
 */
public class BloodTitleView extends View {

    private final Paint textPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dripPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // قطره‌های خون
    private static final int DRIP_COUNT = 5;
    private final float[] dripY    = new float[DRIP_COUNT];
    private final float[] dripX    = new float[DRIP_COUNT];
    private final float[] dripSize = new float[DRIP_COUNT];
    private final float[] dripSpd  = new float[DRIP_COUNT];

    private float glowAlpha = 0f;
    private boolean glowUp  = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            tick();
            handler.postDelayed(this, 16); // ~60fps
        }
    };

    public BloodTitleView(Context ctx) {
        super(ctx);
        textPaint.setTextSize(dp(14));
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        dripPaint.setColor(0xFFcc0000);
        glowPaint.setMaskFilter(
            new android.graphics.BlurMaskFilter(dp(6), android.graphics.BlurMaskFilter.Blur.NORMAL));
        glowPaint.setColor(0xFFff0000);

        initDrips();
        handler.post(ticker);
    }

    private void initDrips() {
        for (int i = 0; i < DRIP_COUNT; i++) {
            dripX[i]    = (i + 1) * (1f / (DRIP_COUNT + 1));
            dripY[i]    = (float) Math.random() * 0.4f;
            dripSize[i] = 0.04f + (float) Math.random() * 0.06f;
            dripSpd[i]  = 0.003f + (float) Math.random() * 0.005f;
        }
    }

    private void tick() {
        for (int i = 0; i < DRIP_COUNT; i++) {
            dripY[i] += dripSpd[i];
            if (dripY[i] > 1.3f) {
                dripY[i]    = -0.1f;
                dripSpd[i]  = 0.003f + (float) Math.random() * 0.005f;
                dripSize[i] = 0.04f + (float) Math.random() * 0.06f;
            }
        }
        // glow pulse
        glowAlpha += glowUp ? 0.04f : -0.04f;
        if (glowAlpha >= 1f) { glowAlpha = 1f; glowUp = false; }
        if (glowAlpha <= 0f) { glowAlpha = 0f; glowUp = true; }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        canvas.drawColor(0xFF0a0005);

        // گلو
        glowPaint.setAlpha((int)(glowAlpha * 120));
        canvas.drawText("FANTER", w / 2f, h * 0.62f, glowPaint);

        // متن با گرادیانت خونی
        LinearGradient grad = new LinearGradient(
            0, 0, 0, h,
            new int[]{0xFFff2222, 0xFFcc0000, 0xFF880000},
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP);
        textPaint.setShader(grad);
        canvas.drawText("FANTER", w / 2f, h * 0.62f, textPaint);

        // قطره‌های خون
        for (int i = 0; i < DRIP_COUNT; i++) {
            float cx = dripX[i] * w;
            float cy = dripY[i] * h;
            float r  = dripSize[i] * Math.min(w, h);

            // بدنه قطره
            Path drop = new Path();
            drop.addCircle(cx, cy, r, Path.Direction.CW);
            drop.moveTo(cx - r * 0.4f, cy);
            drop.lineTo(cx, cy - r * 2.5f);
            drop.lineTo(cx + r * 0.4f, cy);
            drop.close();

            dripPaint.setAlpha(200);
            canvas.drawPath(drop, dripPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
