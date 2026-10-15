package com.somen.hisab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Small helpers so screens can be built in code without layout XML. */
public final class Ui {

    private Ui() {}

    public static final int BG = Color.parseColor("#F3F4F6");
    public static final int CARD = Color.WHITE;
    public static final int TEXT = Color.parseColor("#1F2328");
    public static final int MUTED = Color.parseColor("#6B7280");
    public static final int LINE = Color.parseColor("#E5E7EB");
    public static final int ACCENT = Color.parseColor("#0F766E");
    public static final int ACCENT_SOFT = Color.parseColor("#CCEBE7");
    public static final int RED = Color.parseColor("#C92A2A");
    public static final int GREEN = Color.parseColor("#2B8A3E");

    public static final String[] PALETTE = {"#E8590C", "#2F9E44", "#AE3EC9", "#1971C2", "#0C8599", "#F08C00",
            "#C2255C", "#5F3DC4", "#3B5BDB", "#66A80F", "#D6336C", "#087F5B"};

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static int color(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    /** ₹1,23,456 — Indian digit grouping, whole rupees. */
    public static String money(double v) {
        long r = Math.round(Math.abs(v));
        String s = String.valueOf(r);
        StringBuilder out = new StringBuilder();
        if (s.length() > 3) {
            String head = s.substring(0, s.length() - 3);
            String tail = s.substring(s.length() - 3);
            StringBuilder h = new StringBuilder();
            int n = head.length();
            for (int i = 0; i < n; i++) {
                h.append(head.charAt(i));
                int left = n - i - 1;
                if (left > 0 && left % 2 == 0) h.append(',');
            }
            out.append(h).append(',').append(tail);
        } else {
            out.append(s);
        }
        return (v < 0 && r != 0 ? "−₹" : "₹") + out;
    }

    public static String moneyExact(double v) {
        String whole = money(Math.floor(Math.abs(v)) * Math.signum(v));
        long paise = Math.round((Math.abs(v) - Math.floor(Math.abs(v))) * 100);
        if (paise == 0) return whole;
        if (paise == 100) return money(v);
        return whole + "." + (paise < 10 ? "0" : "") + paise;
    }

    public static String date(long t) {
        return new SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(new Date(t));
    }

    public static String dayLabel(long t) {
        return new SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(new Date(t));
    }

    // ---------- months: offset 0 = this month, -1 = last month ...

    public static long monthStart(int offset) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, offset);
        return c.getTimeInMillis();
    }

    public static long monthEnd(int offset) {
        return monthStart(offset + 1);
    }

    public static String monthLabel(int offset) {
        return new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(new Date(monthStart(offset)));
    }

    public static String monthShort(int offset) {
        return new SimpleDateFormat("MMM", Locale.ENGLISH).format(new Date(monthStart(offset)));
    }

    public static String monthKey(int offset) {
        return new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(new Date(monthStart(offset)));
    }

    // ---------- views

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    /** White rounded card with padding; caller adds children. */
    public static LinearLayout card(Context c) {
        LinearLayout l = vbox(c);
        l.setBackground(round(CARD, dp(c, 14)));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView sectionTitle(Context c, String s) {
        TextView t = text(c, s, 15, TEXT, true);
        t.setPadding(0, 0, 0, dp(c, 10));
        return t;
    }

    public static Button button(Context c, String label, boolean primary) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(primary ? Color.WHITE : ACCENT);
        b.setBackground(round(primary ? ACCENT : ACCENT_SOFT, dp(c, 10)));
        b.setStateListAnimator(null);
        b.setMinHeight(dp(c, 44));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        return b;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    public static View dot(Context c, int color, int sizeDp) {
        View v = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        v.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
        lp.setMargins(0, 0, dp(c, 10), 0);
        v.setLayoutParams(lp);
        return v;
    }

    /** A thin horizontal bar filled to {@code fraction}. */
    public static View bar(Context c, float fraction, int color) {
        LinearLayout track = hbox(c);
        track.setBackground(round(LINE, dp(c, 3)));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 6));
        tlp.setMargins(0, dp(c, 6), 0, 0);
        track.setLayoutParams(tlp);
        float f = Math.max(0f, Math.min(1f, fraction));
        View fill = new View(c);
        fill.setBackground(round(color, dp(c, 3)));
        fill.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, f));
        View rest = new View(c);
        rest.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - f));
        track.addView(fill);
        track.addView(rest);
        return track;
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
