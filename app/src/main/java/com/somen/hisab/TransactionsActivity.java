package com.somen.hisab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.somen.hisab.core.ParsedTxn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** List of transactions for a month, optionally filtered by category, payment mode or merchant. */
public class TransactionsActivity extends Activity {

    private long start, end;
    private String category, mode, merchant, kind;
    private String search = "";
    private final List<Txn> items = new ArrayList<>();
    private Adapter adapter;
    private TextView summary;
    private Set<String> counted;
    private Map<String, Db.Category> cats;

    public static void open(Context c, int monthOffset, String category, String mode, String merchant) {
        Intent i = new Intent(c, TransactionsActivity.class);
        i.putExtra("start", Ui.monthStart(monthOffset));
        i.putExtra("end", Ui.monthEnd(monthOffset));
        i.putExtra("month", Ui.monthLabel(monthOffset));
        if (category != null) i.putExtra("category", category);
        if (mode != null) i.putExtra("mode", mode);
        if (merchant != null) i.putExtra("merchant", merchant);
        c.startActivity(i);
    }

    public static void openKind(Context c, int monthOffset, String kind) {
        Intent i = new Intent(c, TransactionsActivity.class);
        i.putExtra("start", Ui.monthStart(monthOffset));
        i.putExtra("end", Ui.monthEnd(monthOffset));
        i.putExtra("month", Ui.monthLabel(monthOffset));
        i.putExtra("kind", kind);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        start = in.getLongExtra("start", 0);
        end = in.getLongExtra("end", Long.MAX_VALUE);
        category = in.getStringExtra("category");
        mode = in.getStringExtra("mode");
        merchant = in.getStringExtra("merchant");
        kind = in.getStringExtra("kind");
        String month = in.getStringExtra("month");
        String what = category != null ? category : mode != null ? mode : merchant != null ? merchant
                : "income".equals(kind) ? "Money in" : "notcounted".equals(kind) ? "Not counted" : "All transactions";
        setTitle(what);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 16);

        LinearLayout head = Ui.vbox(this);
        head.setPadding(p, Ui.dp(this, 12), p, Ui.dp(this, 8));
        head.addView(Ui.text(this, month == null ? "" : month, 13, Ui.MUTED, true));
        summary = Ui.text(this, "", 15, Ui.TEXT, true);
        head.addView(summary);
        EditText searchBox = new EditText(this);
        searchBox.setHint("Search name, SMS or note");
        searchBox.setSingleLine(true);
        searchBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int c, int d) {}
            public void afterTextChanged(Editable e) {
                search = e.toString();
                load();
            }
        });
        head.addView(searchBox);
        root.addView(head);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setPadding(p, 0, p, p);
        list.setClipToPadding(false);
        adapter = new Adapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> TxnEditor.edit(this, items.get(pos).id, this::load));
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Db db = Db.get(this);
        counted = db.countedCategories();
        cats = db.categoryMap();
        items.clear();
        for (Txn t : db.list(start, end, category, mode, merchant, search)) {
            if ("income".equals(kind) && !Db.isIncome(t, counted)) continue;
            if ("notcounted".equals(kind) && !Db.isNotCounted(t, counted)) continue;
            items.add(t);
        }
        double spent = 0, in = 0;
        for (Txn t : items) {
            double v = Db.spendValue(t, counted);
            spent += v;
            if (Db.isIncome(t, counted)) in += t.amount;
        }
        String s = items.size() + (items.size() == 1 ? " transaction" : " transactions") + " · spent " + Ui.money(spent);
        if (in > 0) s += " · in " + Ui.money(in);
        summary.setText(s);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Add cash spend");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            TxnEditor.addCash(this, this::load);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class Adapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int i) { return items.get(i); }
        @Override public long getItemId(int i) { return items.get(i).id; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Context c = TransactionsActivity.this;
            Txn t = items.get(pos);

            LinearLayout row = Ui.hbox(c);
            row.setBackground(Ui.round(Ui.CARD, Ui.dp(c, 12)));
            int p = Ui.dp(c, 12);
            row.setPadding(p, p, p, p);
            Db.Category cat = cats.get(t.category);
            row.addView(Ui.dot(c, cat == null ? Ui.MUTED : Ui.color(cat.color, Ui.MUTED), 10));

            LinearLayout mid = Ui.vbox(c);
            TextView name = Ui.text(c, t.displayName(), 15, Ui.TEXT, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            mid.addView(name);
            String flag = t.duplicate ? " · duplicate" : t.excluded ? " · not counted" : "";
            String acct = t.account == null || t.account.isEmpty() ? "" : " ••" + t.account;
            TextView sub = Ui.text(c, t.category + " · " + t.mode + acct + "\n" + Ui.date(t.time) + flag, 12, Ui.MUTED, false);
            mid.addView(sub);
            row.addView(mid, Ui.weight(1));

            boolean credit = ParsedTxn.CREDIT.equals(t.type);
            boolean counts = Db.spendValue(t, counted) != 0;
            String sign = credit ? "+" : ParsedTxn.DEBIT.equals(t.type) ? "−" : "";
            TextView amt = Ui.text(c, sign + Ui.moneyExact(t.amount), 15,
                    credit ? Ui.GREEN : counts ? Ui.TEXT : Ui.MUTED, true);
            if (t.duplicate || t.excluded) amt.setPaintFlags(amt.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            amt.setPadding(Ui.dp(c, 8), 0, 0, 0);
            row.addView(amt);

            LinearLayout wrap = Ui.vbox(c);
            wrap.setPadding(0, 0, 0, Ui.dp(c, 8));
            wrap.addView(row);
            return wrap;
        }
    }
}
