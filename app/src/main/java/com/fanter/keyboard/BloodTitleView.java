package com.fanter.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class BloodTitleView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public BloodTitleView(Context ctx) {
        super(ctx);
        p.setTextSize(42f);
        p.setColor(Color.RED);
        p.setFakeBoldText(true);
        p.setTextAlign(Paint.Align.CENTER);
    }
    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(Color.parseColor("#0a0005"));
        c.drawText("FANTER", getWidth()/2f, getHeight()*0.7f, p);
    }
}
