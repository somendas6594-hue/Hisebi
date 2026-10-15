package com.somen.hisab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.somen.hisab.core.Categorizer;
import com.somen.hisab.core.ParsedTxn;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Dialogs to fix a transaction and to add a cash spend by hand. */
public final class TxnEditor {

    private TxnEditor() {}

    public static void edit(Activity a, long id, Runnable onChanged) {
        Db db = Db.get(a);
        Txn t = db.txn(id);
        if (t == null) return;

        LinearLayout box = Ui.vbox(a);
        int p = Ui.dp(a, 20);
        box.setPadding(p, Ui.dp(a, 8), p, 0);

        String kind = ParsedTxn.DEBIT.equals(t.type) ? "Spent" : ParsedTxn.CREDIT.equals(t.type) ? "Received" : "Card bill paid";
        TextView amount = Ui.text(a, Ui.moneyExact(t.amount), 26, ParsedTxn.CREDIT.equals(t.type) ? Ui.GREEN : Ui.TEXT, true);
        box.addView(amount);
        String acct = t.account == null || t.account.isEmpty() ? "" : " · ••" + t.account;
        box.addView(Ui.text(a, kind + " · " + t.mode + acct + "\n" + Ui.dayLabel(t.time) + ", "
                + Ui.date(t.time).replaceAll("^.*, ", ""), 13, Ui.MUTED, false));

        box.addView(label(a, "Name"));
        EditText name = new EditText(a);
        name.setText(t.merchant == null ? "" : t.merchant);
        name.setHint("Who you paid");
        name.setSingleLine(true);
        box.addView(name);

        box.addView(label(a, "Category"));
        List<String> cats = db.categoryNames();
        Spinner spinner = new Spinner(a);
        ArrayAdapter<String> ad = new ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item, cats);
        spinner.setAdapter(ad);
        int idx = cats.indexOf(t.category);
        if (idx >= 0) spinner.setSelection(idx);
        box.addView(spinner);

        CheckBox always = new CheckBox(a);
        box.addView(always);
        Runnable updateAlways = () -> {
            String n = name.getText().toString().trim();
            always.setText(n.isEmpty() ? "Always use this category for this name"
                    : "Always put \"" + n.toUpperCase(Locale.ROOT) + "\" here");
            always.setEnabled(!n.isEmpty());
        };
        updateAlways.run();
        name.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable e) { updateAlways.run(); }
        });

        CheckBox exclude = new CheckBox(a);
        exclude.setText("Don't count this one");
        exclude.setChecked(t.excluded);
        box.addView(exclude);

        CheckBox dup = new CheckBox(a);
        dup.setText("Duplicate of another SMS (not counted)");
        dup.setChecked(t.duplicate);
        if (t.duplicate) box.addView(dup);

        box.addView(label(a, "Note"));
        EditText note = new EditText(a);
        note.setText(t.note == null ? "" : t.note);
        note.setHint("Optional");
        box.addView(note);

        if (!t.isManualEntry()) {
            box.addView(label(a, "Original SMS from " + t.sender));
            TextView sms = Ui.text(a, t.body, 12, Ui.MUTED, false);
            sms.setTextIsSelectable(true);
            sms.setPadding(0, 0, 0, Ui.dp(a, 12));
            box.addView(sms);
        }

        ScrollView sv = new ScrollView(a);
        sv.addView(box);

        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setView(sv)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = name.getText().toString().trim().toUpperCase(Locale.ROOT);
                    String cat = (String) spinner.getSelectedItem();
                    boolean catChanged = cat != null && !cat.equals(t.category);
                    t.merchant = newName;
                    if (catChanged) {
                        t.category = cat;
                        t.manualCategory = true;
                    }
                    t.excluded = exclude.isChecked();
                    if (t.duplicate) t.duplicate = dup.isChecked();
                    t.note = note.getText().toString().trim();
                    db.update(t);
                    if (always.isChecked() && !newName.isEmpty() && cat != null) {
                        db.addRule(newName, cat);
                        db.recategorizeMerchant(newName, cat);
                        int n = db.reapplyRules();
                        Toast.makeText(a, "Rule saved" + (n > 0 ? " · " + n + " more updated" : ""), Toast.LENGTH_SHORT).show();
                    }
                    if (onChanged != null) onChanged.run();
                });
        if (t.isManualEntry()) {
            b.setNeutralButton("Delete", (d, w) -> {
                db.delete(t.id);
                if (onChanged != null) onChanged.run();
            });
        }
        b.show();
    }

    public static void addCash(Activity a, Runnable onChanged) {
        Db db = Db.get(a);
        LinearLayout box = Ui.vbox(a);
        int p = Ui.dp(a, 20);
        box.setPadding(p, Ui.dp(a, 8), p, 0);

        box.addView(label(a, "Amount (₹)"));
        EditText amount = new EditText(a);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        box.addView(amount);

        box.addView(label(a, "What for"));
        EditText desc = new EditText(a);
        desc.setHint("e.g. vegetables, auto");
        desc.setSingleLine(true);
        box.addView(desc);

        box.addView(label(a, "Category"));
        List<String> cats = db.categoryNames();
        Spinner spinner = new Spinner(a);
        spinner.setAdapter(new ArrayAdapter<>(a, android.R.layout.simple_spinner_dropdown_item, cats));
        int others = cats.indexOf(Categorizer.OTHERS);
        if (others >= 0) spinner.setSelection(others);
        box.addView(spinner);

        final Calendar when = Calendar.getInstance();
        Button date = Ui.button(a, "Date: " + Ui.dayLabel(when.getTimeInMillis()), false);
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.setMargins(0, Ui.dp(a, 12), 0, Ui.dp(a, 8));
        date.setLayoutParams(lp);
        date.setOnClickListener(v -> new DatePickerDialog(a, (dp, y, m, d) -> {
            when.set(y, m, d);
            date.setText("Date: " + Ui.dayLabel(when.getTimeInMillis()));
        }, when.get(Calendar.YEAR), when.get(Calendar.MONTH), when.get(Calendar.DAY_OF_MONTH)).show());
        box.addView(date);

        new AlertDialog.Builder(a)
                .setTitle("Add cash spend")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    double v;
                    try {
                        v = Double.parseDouble(amount.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(a, "Enter an amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (v <= 0) return;
                    db.insertManual(v, (String) spinner.getSelectedItem(), desc.getText().toString(), when.getTimeInMillis());
                    if (onChanged != null) onChanged.run();
                })
                .show();
    }

    static TextView label(Activity a, String s) {
        TextView t = Ui.text(a, s, 12, Ui.MUTED, true);
        t.setPadding(0, Ui.dp(a, 14), 0, 0);
        return t;
    }
}
