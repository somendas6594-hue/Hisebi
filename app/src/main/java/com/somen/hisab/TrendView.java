package com.somen.hisab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

/** Simple bar chart: spending for the last few months, selected month highlighted. */
public class TrendView extends View {

    private double[] values = new double[0];
    private String[] labels = new String[0];
    private int highlight = -1;
    private double budget = 0;

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint budgetLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public TrendView(Context c) {
        super(c);
        label.setColor(Ui.MUTED);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(sp(11));
        value.setColor(Ui.TEXT);
        value.setTextAlign(Paint.Align.CENTER);
        value.setTextSize(sp(10));
        budgetLine.setColor(Ui.RED);
        budgetLine.setStrokeWidth(Ui.dp(c, 1));
        budgetLine.setPathEffect(new android.graphics.DashPathEffect(new float[]{Ui.dp(c, 4), Ui.dp(c, 4)}, 0));
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    public void setData(double[] values, String[] labels, int highlight, double budget) {
        this.values = values;
        this.labels = labels;
        this.highlight = highlight;
        this.budget = budget;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(getContext(), 150));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = values.length;
        if (n == 0) return;
        float w = getWidth();
        float h = getHeight();
        float top = sp(16);
        float bottom = h - sp(18);
        double max = budget;
        for (double v : values) max = Math.max(max, v);
        if (max <= 0) max = 1;
        float slot = w / n;
        float bw = Math.min(slot * 0.55f, Ui.dp(getContext(), 34));
        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2;
            float bh = (float) ((bottom - top) * Math.max(0, values[i]) / max);
            r.set(cx - bw / 2, bottom - bh, cx + bw / 2, bottom);
            bar.setColor(i == highlight ? Ui.ACCENT : Ui.ACCENT_SOFT);
            canvas.drawRoundRect(r, Ui.dp(getContext(), 4), Ui.dp(getContext(), 4), bar);
            canvas.drawText(labels[i], cx, h - sp(4), label);
            if (values[i] > 0) canvas.drawText(shortMoney(values[i]), cx, bottom - bh - sp(4), value);
        }
        if (budget > 0) {
            float y = (float) (bottom - (bottom - top) * budget / max);
            canvas.drawLine(0, y, w, y, budgetLine);
        }
    }

    static String shortMoney(double v) {
        if (v >= 100000) return String.format(java.util.Locale.ENGLISH, "%.1fL", v / 100000);
        if (v >= 1000) return String.format(java.util.Locale.ENGLISH, "%.1fk", v / 1000);
        return String.valueOf(Math.round(v));
    }
}
