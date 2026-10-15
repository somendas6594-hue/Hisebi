package com.somen.hisab;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.somen.hisab.core.ParsedTxn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dashboard: where the money went this month. */
public class MainActivity extends Activity {

    private static final int REQ_SMS = 1;
    private static final int REQ_NOTIF = 2;
    private static final int MONTHS_IN_TREND = 6;

    private int offset = 0;              // 0 = this month, -1 = last month...
    private LinearLayout content;
    private boolean importing = false;
    private TextView progressText;
    private boolean askedOnce = false;
    private String importMessage;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Notifier.createChannels(this);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFillViewport(true);
        content = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        content.setPadding(p, p, p, p);
        sv.addView(content);
        setContentView(sv);
        if (b != null) offset = b.getInt("offset", 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("offset", offset);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        if (!SmsImporter.hasSmsPermission(this)) {
            if (!askedOnce) {
                askedOnce = true;
                askSms();
            }
            return;
        }
        Db db = Db.get(this);
        if (!"1".equals(db.meta(SmsImporter.KEY_FIRST_DONE, "0"))) {
            firstImport();
        } else if (SmsImporter.needsRebuild(this)) {
            rebuild("Improved SMS reading: going through your SMS again…");
        } else {
            new Thread(() -> {
                int n = SmsImporter.catchUp(this);
                if (n > 0) runOnUiThread(this::render);
            }).start();
        }
    }

    // ------------------------------------------------------------------ permissions & import

    private void askSms() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.READ_SMS);
        perms.add(Manifest.permission.RECEIVE_SMS);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        requestPermissions(perms.toArray(new String[0]), REQ_SMS);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_SMS && SmsImporter.hasSmsPermission(this)) firstImport();
        else render();
    }

    private void firstImport() {
        if (importing) return;
        importing = true;
        render();
        final long since = System.currentTimeMillis() - 365 * SmsImporter.DAY;
        new Thread(() -> {
            int n = SmsImporter.scan(this, since, (done, total) -> runOnUiThread(() -> {
                if (progressText != null) progressText.setText("Reading your SMS… " + done + " of " + total);
            }));
            Db.get(this).setMeta(SmsImporter.KEY_FIRST_DONE, "1");
            Db.get(this).setMeta(Db.KEY_PARSER_VERSION, String.valueOf(Db.PARSER_VERSION));
            runOnUiThread(() -> {
                importing = false;
                Toast.makeText(this, "Found " + n + " transactions in the last 12 months", Toast.LENGTH_LONG).show();
                render();
            });
        }).start();
    }

    /** Reads all SMS of the last 12 months again with the current rules. Your edits are kept. */
    private void rebuild(String message) {
        if (importing) return;
        importing = true;
        importMessage = message;
        render();
        new Thread(() -> {
            int n = SmsImporter.rebuild(this, (done, total) -> runOnUiThread(() -> {
                if (progressText != null) progressText.setText(importMessage + " " + done + " of " + total);
            }));
            runOnUiThread(() -> {
                importing = false;
                importMessage = null;
                Toast.makeText(this, n + " transactions found", Toast.LENGTH_SHORT).show();
                render();
            });
        }).start();
    }

    // ------------------------------------------------------------------ dashboard

    private void render() {
        content.removeAllViews();
        progressText = null;

        if (!SmsImporter.hasSmsPermission(this)) {
            content.addView(permissionCard());
        }
        if (importing) {
            LinearLayout c = Ui.card(this);
            progressText = Ui.text(this, importMessage != null ? importMessage : "Reading your SMS…", 15, Ui.TEXT, true);
            c.addView(progressText);
            c.addView(Ui.text(this, "This happens once. Only bank, card and UPI messages are kept, on this phone.",
                    13, Ui.MUTED, false));
            content.addView(c);
        }

        Db db = Db.get(this);
        content.addView(monthNav());

        long start = Ui.monthStart(offset), end = Ui.monthEnd(offset);
        Db.Summary s = db.summary(start, end);
        Db.Summary prev = db.summary(Ui.monthStart(offset - 1), Ui.monthEnd(offset - 1));
        double budget = Notifier.budget(db);
        Map<String, Db.Category> cats = db.categoryMap();

        // ---- total
        LinearLayout total = Ui.card(this);
        total.addView(Ui.text(this, "Spent", 13, Ui.MUTED, true));
        total.addView(Ui.text(this, Ui.money(s.spent), 34, Ui.TEXT, true));
        if (prev.spent > 0) {
            double pct = (s.spent - prev.spent) * 100 / prev.spent;
            String arrow = pct >= 0 ? "▲ " : "▼ ";
            TextView cmp = Ui.text(this, arrow + Math.round(Math.abs(pct)) + "% vs " + Ui.monthShort(offset - 1)
                    + " (" + Ui.money(prev.spent) + ")", 13, pct > 0 ? Ui.RED : Ui.GREEN, false);
            total.addView(cmp);
        }
        if (budget > 0) {
            float f = (float) (s.spent / budget);
            total.addView(Ui.bar(this, f, f >= 1 ? Ui.RED : f >= 0.8f ? Ui.color("#F08C00", Ui.RED) : Ui.ACCENT));
            String left = s.spent <= budget ? Ui.money(budget - s.spent) + " left of " + Ui.money(budget)
                    : Ui.money(s.spent - budget) + " over your " + Ui.money(budget) + " budget";
            total.addView(Ui.text(this, left, 12, Ui.MUTED, false));
        }
        LinearLayout row = Ui.hbox(this);
        row.setPadding(0, Ui.dp(this, 12), 0, 0);
        row.addView(miniStat("Money in", Ui.money(s.income), Ui.GREEN), Ui.weight(1));
        row.addView(miniStat("Not counted", Ui.money(s.notCounted), Ui.MUTED), Ui.weight(1));
        total.addView(row);
        TextView why = Ui.text(this, "Not counted = credit card bill payments, investments and self transfers. "
                + "Card swipes are counted on the day you spend.", 11, Ui.MUTED, false);
        why.setPadding(0, Ui.dp(this, 8), 0, 0);
        total.addView(why);
        content.addView(total);

        // ---- trend
        LinearLayout trendCard = Ui.card(this);
        trendCard.addView(Ui.sectionTitle(this, "Last " + MONTHS_IN_TREND + " months"));
        double[] vals = new double[MONTHS_IN_TREND];
        String[] labels = new String[MONTHS_IN_TREND];
        for (int i = 0; i < MONTHS_IN_TREND; i++) {
            int o = offset - (MONTHS_IN_TREND - 1 - i);
            vals[i] = db.spentBetween(Ui.monthStart(o), Ui.monthEnd(o));
            labels[i] = Ui.monthShort(o);
        }
        TrendView trend = new TrendView(this);
        trend.setData(vals, labels, MONTHS_IN_TREND - 1, budget);
        trendCard.addView(trend);
        content.addView(trendCard);

        // ---- categories
        LinearLayout catCard = Ui.card(this);
        catCard.addView(Ui.sectionTitle(this, "Where it went"));
        List<Map.Entry<String, Double>> byCat = sorted(s.byCategory);
        if (byCat.isEmpty()) {
            catCard.addView(Ui.text(this, importing ? "Wait a moment…" : "No spending found for this month yet.",
                    14, Ui.MUTED, false));
        }
        double max = byCat.isEmpty() ? 1 : Math.max(1, byCat.get(0).getValue());
        for (Map.Entry<String, Double> e : byCat) {
            Db.Category cat = cats.get(e.getKey());
            int color = cat == null ? Ui.MUTED : Ui.color(cat.color, Ui.MUTED);
            int pct = s.spent > 0 ? (int) Math.round(e.getValue() * 100 / s.spent) : 0;
            catCard.addView(barRow(e.getKey(), Ui.money(e.getValue()), pct + "%", (float) (e.getValue() / max), color,
                    v -> TransactionsActivity.open(this, offset, e.getKey(), null, null)));
        }
        content.addView(catCard);

        // ---- payment modes
        if (!s.byMode.isEmpty()) {
            LinearLayout modeCard = Ui.card(this);
            modeCard.addView(Ui.sectionTitle(this, "How you paid"));
            List<Map.Entry<String, Double>> byMode = sorted(s.byMode);
            double mmax = Math.max(1, byMode.get(0).getValue());
            for (Map.Entry<String, Double> e : byMode) {
                modeCard.addView(barRow(e.getKey(), Ui.money(e.getValue()), "", (float) (e.getValue() / mmax), Ui.ACCENT,
                        v -> TransactionsActivity.open(this, offset, null, e.getKey(), null)));
            }
            content.addView(modeCard);
        }

        // ---- top merchants
        if (!s.byMerchant.isEmpty()) {
            LinearLayout mc = Ui.card(this);
            mc.addView(Ui.sectionTitle(this, "Top places"));
            List<Map.Entry<String, Double>> top = sorted(s.byMerchant);
            for (int i = 0; i < Math.min(8, top.size()); i++) {
                Map.Entry<String, Double> e = top.get(i);
                LinearLayout r = Ui.hbox(this);
                r.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
                TextView n = Ui.text(this, (i + 1) + ".  " + e.getKey(), 14, Ui.TEXT, false);
                n.setSingleLine(true);
                n.setEllipsize(android.text.TextUtils.TruncateAt.END);
                r.addView(n, Ui.weight(1));
                r.addView(Ui.text(this, Ui.money(e.getValue()), 14, Ui.TEXT, true));
                r.setOnClickListener(v -> TransactionsActivity.open(this, offset, null, null, e.getKey()));
                mc.addView(r);
            }
            content.addView(mc);
        }

        // ---- actions
        LinearLayout actions = Ui.hbox(this);
        Button all = Ui.button(this, "All transactions", true);
        all.setOnClickListener(v -> TransactionsActivity.open(this, offset, null, null, null));
        Button cash = Ui.button(this, "+ Cash spend", false);
        cash.setOnClickListener(v -> TxnEditor.addCash(this, this::render));
        LinearLayout.LayoutParams l1 = Ui.weight(1);
        l1.setMargins(0, 0, Ui.dp(this, 8), 0);
        actions.addView(all, l1);
        actions.addView(cash, Ui.weight(1));
        content.addView(actions);

        TextView foot = Ui.text(this, SmsImporter.hasSmsPermission(this)
                ? "Watching new SMS automatically · " + db.countAll() + " transactions stored on this phone"
                : "SMS access is off, so nothing new is tracked", 11, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 8));
        content.addView(foot);
    }

    private View monthNav() {
        LinearLayout nav = Ui.hbox(this);
        nav.setPadding(0, 0, 0, Ui.dp(this, 12));
        Button prev = Ui.button(this, "‹", false);
        Button next = Ui.button(this, "›", false);
        TextView label = Ui.text(this, Ui.monthLabel(offset), 18, Ui.TEXT, true);
        label.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> { offset--; render(); });
        next.setOnClickListener(v -> { if (offset < 0) { offset++; render(); } });
        next.setEnabled(offset < 0);
        next.setAlpha(offset < 0 ? 1f : 0.35f);
        int w = Ui.dp(this, 48);
        nav.addView(prev, new LinearLayout.LayoutParams(w, w));
        nav.addView(label, Ui.weight(1));
        nav.addView(next, new LinearLayout.LayoutParams(w, w));
        return nav;
    }

    private View permissionCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.sectionTitle(this, "Allow SMS access"));
        c.addView(Ui.text(this, "Hisab reads bank, card and UPI SMS to work out your spending. "
                + "Everything stays on this phone.\n\nIf Android won't let you allow it: open App info → ⋮ (top right) → "
                + "\"Allow restricted settings\", then come back and tap Allow.", 13, Ui.MUTED, false));
        LinearLayout r = Ui.hbox(this);
        r.setPadding(0, Ui.dp(this, 12), 0, 0);
        Button allow = Ui.button(this, "Allow", true);
        allow.setOnClickListener(v -> askSms());
        Button info = Ui.button(this, "Open App info", false);
        info.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))));
        LinearLayout.LayoutParams l1 = Ui.weight(1);
        l1.setMargins(0, 0, Ui.dp(this, 8), 0);
        r.addView(allow, l1);
        r.addView(info, Ui.weight(1));
        c.addView(r);
        return c;
    }

    private LinearLayout miniStat(String label, String value, int color) {
        LinearLayout l = Ui.vbox(this);
        l.addView(Ui.text(this, label, 12, Ui.MUTED, false));
        l.addView(Ui.text(this, value, 16, color, true));
        return l;
    }

    private View barRow(String name, String amount, String pct, float fraction, int color, View.OnClickListener click) {
        LinearLayout box = Ui.vbox(this);
        box.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        LinearLayout top = Ui.hbox(this);
        top.addView(Ui.dot(this, color, 10));
        top.addView(Ui.text(this, name, 14, Ui.TEXT, false), Ui.weight(1));
        if (!pct.isEmpty()) {
            TextView p = Ui.text(this, pct, 12, Ui.MUTED, false);
            p.setPadding(0, 0, Ui.dp(this, 10), 0);
            top.addView(p);
        }
        top.addView(Ui.text(this, amount, 14, Ui.TEXT, true));
        box.addView(top);
        box.addView(Ui.bar(this, fraction, color));
        box.setOnClickListener(click);
        return box;
    }

    private static List<Map.Entry<String, Double>> sorted(Map<String, Double> m) {
        List<Map.Entry<String, Double>> l = new ArrayList<>();
        for (Map.Entry<String, Double> e : m.entrySet()) if (e.getValue() > 0.5) l.add(e);
        l.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return l;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Transactions");
        menu.add(0, 2, 0, "Categories, rules & budget");
        menu.add(0, 3, 0, "Add cash spend");
        menu.add(0, 4, 0, "Export this month (CSV)");
        menu.add(0, 5, 0, "Export everything (CSV)");
        menu.add(0, 6, 0, "Re-read all SMS (keeps your edits)");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: TransactionsActivity.open(this, offset, null, null, null); return true;
            case 2: startActivity(new Intent(this, SettingsActivity.class)); return true;
            case 3: TxnEditor.addCash(this, this::render); return true;
            case 4: CsvExport.export(this, Ui.monthStart(offset), Ui.monthEnd(offset), "hisab-" + Ui.monthKey(offset)); return true;
            case 5: CsvExport.export(this, 0, Long.MAX_VALUE, "hisab-all"); return true;
            case 6: rebuild("Reading your SMS again…"); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    static boolean isSpend(Txn t) {
        return ParsedTxn.DEBIT.equals(t.type);
    }
}
