package com.somen.hisab;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.somen.hisab.core.Categorizer;

import java.util.List;
import java.util.Locale;

/** Customise: categories (and whether each counts as spending), your own rules, budget, alerts. */
public class SettingsActivity extends Activity {

    private LinearLayout content;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Categories, rules & budget");
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        content = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        content.setPadding(p, p, p, p);
        sv.addView(content);
        setContentView(sv);
        render();
    }

    private void render() {
        content.removeAllViews();
        Db db = Db.get(this);

        // ---- budget & alerts
        LinearLayout bc = Ui.card(this);
        bc.addView(Ui.sectionTitle(this, "Monthly budget"));
        double budget = Notifier.budget(db);
        bc.addView(Ui.text(this, budget > 0 ? Ui.money(budget) + " a month. You get an alert at 80% and 100%."
                : "Not set. Set one to get an alert at 80% and 100%.", 13, Ui.MUTED, false));
        Button setBudget = Ui.button(this, budget > 0 ? "Change budget" : "Set budget", false);
        setBudget.setOnClickListener(v -> editBudget());
        bc.addView(spaced(setBudget));
        Switch notify = new Switch(this);
        notify.setText("Notify me for every new spend");
        notify.setTextColor(Ui.TEXT);
        notify.setChecked(!"0".equals(db.meta(Notifier.KEY_NOTIFY, "1")));
        notify.setOnCheckedChangeListener((s, on) -> db.setMeta(Notifier.KEY_NOTIFY, on ? "1" : "0"));
        notify.setPadding(0, Ui.dp(this, 12), 0, 0);
        bc.addView(notify);
        content.addView(bc);

        // ---- your names (to spot transfers between your own accounts)
        LinearLayout nc = Ui.card(this);
        nc.addView(Ui.sectionTitle(this, "Your names"));
        nc.addView(Ui.text(this, "Money sent to or received from these names is a self transfer and is not counted. "
                + "Add every spelling your banks use.", 12, Ui.MUTED, false));
        TextView names = Ui.text(this, db.meta(Db.KEY_MY_NAMES, "").isEmpty() ? "None yet" : db.meta(Db.KEY_MY_NAMES, ""),
                15, Ui.TEXT, true);
        names.setPadding(0, Ui.dp(this, 10), 0, 0);
        nc.addView(names);
        Button editNames = Ui.button(this, "Edit names", false);
        editNames.setOnClickListener(v -> editNames());
        nc.addView(spaced(editNames));
        content.addView(nc);

        // ---- categories
        LinearLayout cc = Ui.card(this);
        cc.addView(Ui.sectionTitle(this, "Categories"));
        cc.addView(Ui.text(this, "Switch off to keep a category out of your spending total (like card bills or investments).",
                12, Ui.MUTED, false));
        for (Db.Category cat : db.categories()) {
            LinearLayout r = Ui.hbox(this);
            r.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            r.addView(Ui.dot(this, Ui.color(cat.color, Ui.MUTED), 12));
            if (Categorizer.neverCounted(cat.name)) {
                // money moving between your own pockets or coming in: never part of spending
                LinearLayout col = Ui.vbox(this);
                col.addView(Ui.text(this, cat.name, 14, Ui.TEXT, false));
                col.addView(Ui.text(this, "Never counted as spending", 11, Ui.MUTED, false));
                r.addView(col, Ui.weight(1));
            } else {
                Switch s = new Switch(this);
                s.setText(cat.name);
                s.setTextColor(Ui.TEXT);
                s.setChecked(cat.counted);
                s.setOnCheckedChangeListener((sw, on) -> db.setCounted(cat.name, on));
                r.addView(s, Ui.weight(1));
            }
            if (!cat.builtin) {
                TextView del = Ui.text(this, "  ✕", 18, Ui.MUTED, false);
                del.setOnClickListener(v -> new AlertDialog.Builder(this)
                        .setMessage("Delete \"" + cat.name + "\"? Its transactions move to Others.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (d, w) -> { db.deleteCategory(cat.name); render(); })
                        .show());
                r.addView(del);
            }
            cc.addView(r);
        }
        Button addCat = Ui.button(this, "+ New category", false);
        addCat.setOnClickListener(v -> addCategory());
        cc.addView(spaced(addCat));
        content.addView(cc);

        // ---- rules
        LinearLayout rc = Ui.card(this);
        rc.addView(Ui.sectionTitle(this, "Your rules"));
        rc.addView(Ui.text(this, "If a payee's name contains the word, it goes to that category. Your rules beat the built-in ones. "
                + "Tip: tap any transaction and tick \"Always put … here\".", 12, Ui.MUTED, false));
        List<String[]> rules = db.rules();
        if (rules.isEmpty()) {
            TextView none = Ui.text(this, "No rules yet.", 14, Ui.MUTED, false);
            none.setPadding(0, Ui.dp(this, 10), 0, 0);
            rc.addView(none);
        }
        for (String[] r : rules) {
            LinearLayout row = Ui.hbox(this);
            row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            row.addView(Ui.text(this, r[1] + "  →  " + r[2], 14, Ui.TEXT, false), Ui.weight(1));
            TextView del = Ui.text(this, "  ✕", 18, Ui.MUTED, false);
            del.setOnClickListener(v -> {
                db.deleteRule(Long.parseLong(r[0]));
                db.reapplyRules();
                render();
            });
            row.addView(del);
            rc.addView(row);
            rc.addView(Ui.divider(this));
        }
        Button addRule = Ui.button(this, "+ New rule", false);
        addRule.setOnClickListener(v -> addRule());
        rc.addView(spaced(addRule));
        content.addView(rc);

        // ---- maintenance
        LinearLayout mc = Ui.card(this);
        mc.addView(Ui.sectionTitle(this, "Data"));
        mc.addView(Ui.text(this, "Everything is stored only on this phone. Nothing is uploaded.", 12, Ui.MUTED, false));
        Button reapply = Ui.button(this, "Re-sort all transactions with current rules", false);
        reapply.setOnClickListener(v -> {
            int n = db.reapplyRules();
            Toast.makeText(this, n + " transactions changed category", Toast.LENGTH_SHORT).show();
        });
        mc.addView(spaced(reapply));
        content.addView(mc);
    }

    private android.view.View spaced(Button b) {
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.setMargins(0, Ui.dp(this, 12), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = Ui.vbox(this);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        return box;
    }

    private void editBudget() {
        Db db = Db.get(this);
        LinearLayout box = dialogBox();
        EditText amount = new EditText(this);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER);
        amount.setHint("e.g. 40000 (0 to turn off)");
        double cur = Notifier.budget(db);
        if (cur > 0) amount.setText(String.valueOf(Math.round(cur)));
        box.addView(amount);
        new AlertDialog.Builder(this)
                .setTitle("Monthly budget (₹)")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String s = amount.getText().toString().trim();
                    db.setMeta(Notifier.KEY_BUDGET, s.isEmpty() ? "0" : s);
                    render();
                })
                .show();
    }

    private void editNames() {
        Db db = Db.get(this);
        LinearLayout box = dialogBox();
        EditText names = new EditText(this);
        names.setText(db.meta(Db.KEY_MY_NAMES, ""));
        names.setHint("e.g. RAHUL KUMAR, R KUMAR");
        box.addView(names);
        box.addView(Ui.text(this, "Separate names with commas.", 11, Ui.MUTED, false));
        new AlertDialog.Builder(this)
                .setTitle("Your names")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    db.setMeta(Db.KEY_MY_NAMES, names.getText().toString().trim().toUpperCase(Locale.ROOT));
                    int n = db.reapplyRules();
                    Toast.makeText(this, n + " transactions updated", Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
    }

    private void addCategory() {
        Db db = Db.get(this);
        LinearLayout box = dialogBox();
        EditText name = new EditText(this);
        name.setHint("e.g. Kids, Pets, Office lunch");
        name.setSingleLine(true);
        box.addView(name);
        Switch counted = new Switch(this);
        counted.setText("Counts as spending");
        counted.setChecked(true);
        counted.setPadding(0, Ui.dp(this, 12), 0, 0);
        box.addView(counted);
        new AlertDialog.Builder(this)
                .setTitle("New category")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    String color = Ui.PALETTE[Math.floorMod(n.hashCode(), Ui.PALETTE.length)];
                    if (!db.addCategory(n, counted.isChecked(), color)) {
                        Toast.makeText(this, "That category already exists", Toast.LENGTH_SHORT).show();
                    }
                    render();
                })
                .show();
    }

    private void addRule() {
        Db db = Db.get(this);
        LinearLayout box = dialogBox();
        box.addView(TxnEditor.label(this, "If the payee name contains"));
        EditText kw = new EditText(this);
        kw.setHint("e.g. RAMESH or CHAI POINT");
        kw.setSingleLine(true);
        box.addView(kw);
        box.addView(TxnEditor.label(this, "Put it in"));
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, db.categoryNames()));
        box.addView(sp);
        TextView hint = Ui.text(this, "Match ignores upper/lower case.", 11, Ui.MUTED, false);
        hint.setGravity(Gravity.START);
        hint.setPadding(0, Ui.dp(this, 8), 0, 0);
        box.addView(hint);
        new AlertDialog.Builder(this)
                .setTitle("New rule")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String k = kw.getText().toString().trim().toUpperCase(Locale.ROOT);
                    String cat = (String) sp.getSelectedItem();
                    if (k.isEmpty() || cat == null) return;
                    db.addRule(k, cat);
                    int n = db.reapplyRules();
                    Toast.makeText(this, "Rule saved · " + n + " transactions updated", Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
    }
}
