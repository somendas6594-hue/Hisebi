package com.somen.hisab;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.somen.hisab.core.Categorizer;
import com.somen.hisab.core.ParsedTxn;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** All storage lives on the phone in one SQLite file. Nothing is sent anywhere. */
public final class Db extends SQLiteOpenHelper {

    private static Db instance;

    public static synchronized Db get(Context c) {
        if (instance == null) instance = new Db(c.getApplicationContext());
        return instance;
    }

    private Db(Context c) {
        super(c, "hisab.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE txn(_id INTEGER PRIMARY KEY AUTOINCREMENT, sms_key TEXT UNIQUE, time INTEGER, "
                + "amount REAL, type TEXT, mode TEXT, merchant TEXT, account TEXT, sender TEXT, body TEXT, "
                + "category TEXT, manual_cat INTEGER DEFAULT 0, excluded INTEGER DEFAULT 0, "
                + "duplicate INTEGER DEFAULT 0, note TEXT, person INTEGER DEFAULT 0, self INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX txn_time ON txn(time)");
        db.execSQL("CREATE TABLE category(name TEXT PRIMARY KEY, counted INTEGER, color TEXT, sort INTEGER, builtin INTEGER)");
        db.execSQL("CREATE TABLE rule(_id INTEGER PRIMARY KEY AUTOINCREMENT, keyword TEXT, category TEXT)");
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)");
        seedCategories(db);
        seedNames(db);
    }

    private static void seedCategories(SQLiteDatabase db) {
        int i = 0;
        for (String[] c : Categorizer.DEFAULT_CATEGORIES) {
            ContentValues v = new ContentValues();
            v.put("name", c[0]);
            v.put("counted", Integer.parseInt(c[1]));
            v.put("color", c[2]);
            v.put("sort", i++);
            v.put("builtin", 1);
            db.insertWithOnConflict("category", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            // keep the built-in order even for categories that already existed
            ContentValues o = new ContentValues();
            o.put("sort", i - 1);
            db.update("category", o, "name=?", new String[]{c[0]});
        }
    }

    private static void seedNames(SQLiteDatabase db) {
        ContentValues v = new ContentValues();
        v.put("k", KEY_MY_NAMES);
        v.put("v", "SOMEN DAS, SUMEN DAS");
        db.insertWithOnConflict("meta", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE txn ADD COLUMN person INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE txn ADD COLUMN self INTEGER DEFAULT 0");
            seedCategories(db);
            seedNames(db);
            // version 1 could end up counting these; start them off again as "not counted"
            ContentValues v = new ContentValues();
            v.put("counted", 0);
            db.update("category", v, "name IN (?,?,?,?)", new String[]{
                    Categorizer.INVESTMENTS, Categorizer.CARD_BILL, Categorizer.SELF, Categorizer.INCOME});
        }
    }

    /** Bump when SmsParser / Categorizer change, so stored SMS are read again with the new rules. */
    public static final int PARSER_VERSION = 3;
    public static final String KEY_PARSER_VERSION = "parser_version";
    public static final String KEY_MY_NAMES = "my_names";

    // ------------------------------------------------------------------ meta

    public String meta(String k, String def) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT v FROM meta WHERE k=?", new String[]{k})) {
            return c.moveToFirst() ? c.getString(0) : def;
        }
    }

    public long metaLong(String k, long def) {
        try {
            return Long.parseLong(meta(k, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void setMeta(String k, String v) {
        ContentValues cv = new ContentValues();
        cv.put("k", k);
        cv.put("v", v);
        getWritableDatabase().insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ------------------------------------------------------------------ who you are

    public List<String> myNames() {
        List<String> out = new ArrayList<>();
        for (String n : meta(KEY_MY_NAMES, "").split(",")) {
            String t = n.trim();
            if (!t.isEmpty()) out.add(t.toUpperCase(java.util.Locale.ROOT));
        }
        return out;
    }

    /** Last 4 digits of your bank accounts, learnt from the SMS themselves. */
    public Set<String> ownAccounts() {
        Set<String> s = new HashSet<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT account FROM txn WHERE account<>'' AND LOWER(mode) NOT LIKE '%card%'", null)) {
            while (c.moveToNext()) {
                String a = c.getString(0);
                if (a != null && a.length() >= 3) s.add(a.length() > 4 ? a.substring(a.length() - 4) : a);
            }
        }
        return s;
    }

    public Categorizer.Ctx ctx() {
        Categorizer.Ctx c = new Categorizer.Ctx();
        c.userRules = userRules();
        c.ownAccounts = ownAccounts();
        c.myNames = myNames();
        return c;
    }

    // ------------------------------------------------------------------ categories

    public static class Category {
        public String name;
        public boolean counted;
        public String color;
        public boolean builtin;
    }

    public List<Category> categories() {
        List<Category> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, counted, color, builtin FROM category ORDER BY sort, name", null)) {
            while (c.moveToNext()) {
                Category k = new Category();
                k.name = c.getString(0);
                k.counted = c.getInt(1) == 1;
                k.color = c.getString(2);
                k.builtin = c.getInt(3) == 1;
                out.add(k);
            }
        }
        return out;
    }

    public List<String> categoryNames() {
        List<String> out = new ArrayList<>();
        for (Category c : categories()) out.add(c.name);
        return out;
    }

    public Map<String, Category> categoryMap() {
        Map<String, Category> m = new HashMap<>();
        for (Category c : categories()) m.put(c.name, c);
        return m;
    }

    public boolean addCategory(String name, boolean counted, String color) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("counted", counted ? 1 : 0);
        v.put("color", color);
        v.put("sort", 100);
        v.put("builtin", 0);
        return getWritableDatabase().insert("category", null, v) != -1;
    }

    public void setCounted(String name, boolean counted) {
        ContentValues v = new ContentValues();
        v.put("counted", counted ? 1 : 0);
        getWritableDatabase().update("category", v, "name=?", new String[]{name});
    }

    /** Removes a category; its transactions move to Others and its rules are dropped. */
    public void deleteCategory(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("category", Categorizer.OTHERS);
        v.put("manual_cat", 0);
        db.update("txn", v, "category=?", new String[]{name});
        db.delete("rule", "category=?", new String[]{name});
        db.delete("category", "name=?", new String[]{name});
    }

    // ------------------------------------------------------------------ rules

    /** {id, keyword, category} */
    public List<String[]> rules() {
        List<String[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id, keyword, category FROM rule ORDER BY _id DESC", null)) {
            while (c.moveToNext()) out.add(new String[]{String.valueOf(c.getLong(0)), c.getString(1), c.getString(2)});
        }
        return out;
    }

    /** {keyword, category} for the categorizer. */
    public List<String[]> userRules() {
        List<String[]> out = new ArrayList<>();
        for (String[] r : rules()) out.add(new String[]{r[1], r[2]});
        return out;
    }

    public void addRule(String keyword, String category) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("rule", "keyword=? COLLATE NOCASE", new String[]{keyword});
        ContentValues v = new ContentValues();
        v.put("keyword", keyword);
        v.put("category", category);
        db.insert("rule", null, v);
    }

    public void deleteRule(long id) {
        getWritableDatabase().delete("rule", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Re-runs categorisation on every transaction the user has not fixed by hand. Returns how many changed. */
    public int reapplyRules() {
        Categorizer.Ctx ctx = ctx();
        SQLiteDatabase db = getWritableDatabase();
        int changed = 0;
        db.beginTransaction();
        try (Cursor c = db.rawQuery("SELECT _id, type, mode, merchant, body, category, person, self, sender "
                + "FROM txn WHERE manual_cat=0", null)) {
            while (c.moveToNext()) {
                ParsedTxn p = new ParsedTxn();
                p.type = c.getString(1);
                p.mode = c.getString(2);
                p.merchant = c.getString(3);
                p.toPerson = c.getInt(6) == 1;
                p.selfTransfer = c.getInt(7) == 1;
                ctx.sender = c.getString(8);
                String cat = Categorizer.categorize(p, c.getString(4), ctx);
                if (!cat.equals(c.getString(5))) {
                    ContentValues v = new ContentValues();
                    v.put("category", cat);
                    db.update("txn", v, "_id=?", new String[]{String.valueOf(c.getLong(0))});
                    changed++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return changed;
    }

    // ------------------------------------------------------------------ transactions

    public static String smsKey(String sender, String body, long time) {
        String day = String.valueOf(time / 86_400_000L);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest((sender + "|" + body.trim() + "|" + day).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder("sms:");
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "sms:" + sender + "|" + body.trim().hashCode() + "|" + day;
        }
    }

    /** @return the new row id, or -1 if this SMS was already stored. */
    public long insertParsed(ParsedTxn p, String sender, String body, long time, Categorizer.Ctx ctx) {
        String key = smsKey(sender, body, time);
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM txn WHERE sms_key=?", new String[]{key})) {
            if (c.moveToFirst()) return -1;
        }
        // The same payment often gives two SMS (e.g. bank + card issuer). Same amount and direction,
        // from a different sender, within 3 minutes -> keep both but don't count the second.
        boolean dup = false;
        try (Cursor c = db.rawQuery(
                "SELECT 1 FROM txn WHERE amount=? AND type=? AND sender<>? AND duplicate=0 AND ABS(time-?)<=180000",
                new String[]{String.valueOf(p.amount), p.type, sender, String.valueOf(time)})) {
            dup = c.moveToFirst();
        }
        ContentValues v = new ContentValues();
        v.put("sms_key", key);
        v.put("time", time);
        v.put("amount", p.amount);
        v.put("type", p.type);
        v.put("mode", p.mode);
        v.put("merchant", p.merchant);
        v.put("account", p.account);
        v.put("sender", sender);
        v.put("body", body);
        ctx.sender = sender;
        v.put("category", Categorizer.categorize(p, body, ctx));
        v.put("duplicate", dup ? 1 : 0);
        v.put("person", p.toPerson ? 1 : 0);
        v.put("self", p.selfTransfer ? 1 : 0);
        return db.insertWithOnConflict("txn", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long insertManual(double amount, String category, String description, long time) {
        ContentValues v = new ContentValues();
        v.put("sms_key", "manual:" + UUID.randomUUID());
        v.put("time", time);
        v.put("amount", amount);
        v.put("type", ParsedTxn.DEBIT);
        v.put("mode", ParsedTxn.MODE_CASH);
        v.put("merchant", description == null ? "" : description.trim().toUpperCase());
        v.put("account", "");
        v.put("sender", "You");
        v.put("body", "Added by hand");
        v.put("category", category);
        v.put("manual_cat", 1);
        return getWritableDatabase().insert("txn", null, v);
    }

    private static final String COLS = "_id, sms_key, time, amount, type, mode, merchant, account, sender, body, "
            + "category, manual_cat, excluded, duplicate, note";

    private static Txn read(Cursor c) {
        Txn t = new Txn();
        t.id = c.getLong(0);
        t.smsKey = c.getString(1);
        t.time = c.getLong(2);
        t.amount = c.getDouble(3);
        t.type = c.getString(4);
        t.mode = c.getString(5);
        t.merchant = c.getString(6);
        t.account = c.getString(7);
        t.sender = c.getString(8);
        t.body = c.getString(9);
        t.category = c.getString(10);
        t.manualCategory = c.getInt(11) == 1;
        t.excluded = c.getInt(12) == 1;
        t.duplicate = c.getInt(13) == 1;
        t.note = c.getString(14);
        return t;
    }

    public Txn txn(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM txn WHERE _id=?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    public void update(Txn t) {
        ContentValues v = new ContentValues();
        v.put("merchant", t.merchant);
        v.put("category", t.category);
        v.put("manual_cat", t.manualCategory ? 1 : 0);
        v.put("excluded", t.excluded ? 1 : 0);
        v.put("duplicate", t.duplicate ? 1 : 0);
        v.put("note", t.note);
        getWritableDatabase().update("txn", v, "_id=?", new String[]{String.valueOf(t.id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("txn", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Sets the category for every not-hand-edited transaction of this merchant. */
    public void recategorizeMerchant(String merchant, String category) {
        ContentValues v = new ContentValues();
        v.put("category", category);
        getWritableDatabase().update("txn", v, "merchant=? AND manual_cat=0 AND type<>?",
                new String[]{merchant, ParsedTxn.CARD_BILL});
    }

    public List<Txn> list(long start, long end, String category, String mode, String merchant, String search) {
        StringBuilder where = new StringBuilder("time>=? AND time<?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(start));
        args.add(String.valueOf(end));
        if (category != null) {
            where.append(" AND category=?");
            args.add(category);
        }
        if (mode != null) {
            where.append(" AND mode=? AND type=?");
            args.add(mode);
            args.add(ParsedTxn.DEBIT);
        }
        if (merchant != null) {
            where.append(" AND merchant=?");
            args.add(merchant);
        }
        if (search != null && !search.trim().isEmpty()) {
            where.append(" AND (merchant LIKE ? OR body LIKE ? OR note LIKE ? OR category LIKE ?)");
            String q = "%" + search.trim() + "%";
            Collections.addAll(args, q, q, q, q);
        }
        List<Txn> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM txn WHERE " + where
                + " ORDER BY time DESC", args.toArray(new String[0]))) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    /** Your edits on SMS transactions, so they survive re-reading the SMS. */
    public static class Edit {
        String category, merchant, note;
        boolean manualCat, excluded, notDuplicate;
    }

    public Map<String, Edit> snapshotEdits() {
        Map<String, Edit> m = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT sms_key, category, merchant, note, manual_cat, excluded "
                + "FROM txn WHERE sms_key LIKE 'sms:%' AND (manual_cat=1 OR excluded=1 OR (note IS NOT NULL AND note<>''))",
                null)) {
            while (c.moveToNext()) {
                Edit e = new Edit();
                e.category = c.getString(1);
                e.merchant = c.getString(2);
                e.note = c.getString(3);
                e.manualCat = c.getInt(4) == 1;
                e.excluded = c.getInt(5) == 1;
                m.put(c.getString(0), e);
            }
        }
        return m;
    }

    public void deleteSmsTransactions() {
        getWritableDatabase().delete("txn", "sms_key LIKE 'sms:%'", null);
    }

    public void restoreEdits(Map<String, Edit> edits) {
        SQLiteDatabase db = getWritableDatabase();
        for (Map.Entry<String, Edit> e : edits.entrySet()) {
            Edit x = e.getValue();
            ContentValues v = new ContentValues();
            if (x.manualCat) {
                v.put("category", x.category);
                v.put("manual_cat", 1);
                v.put("merchant", x.merchant);
            }
            v.put("excluded", x.excluded ? 1 : 0);
            v.put("note", x.note);
            db.update("txn", v, "sms_key=?", new String[]{e.getKey()});
        }
    }

    public long countAll() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM txn", null)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    // ------------------------------------------------------------------ summaries

    public static class Summary {
        public double spent;          // what really left your pocket (counted categories, minus refunds tagged to them)
        public double income;         // salary, transfers in, refunds left as income
        public double notCounted;     // card bills, investments, self transfers, excluded
        public int count;
        public final Map<String, Double> byCategory = new LinkedHashMap<>();
        public final Map<String, Double> byMode = new LinkedHashMap<>();
        public final Map<String, Double> byMerchant = new HashMap<>();
    }

    /** Does this transaction count towards spending (positive) or reduce it (refund in a spending category)? */
    public static double spendValue(Txn t, Set<String> countedCats) {
        if (t.excluded || t.duplicate || t.category == null || !countedCats.contains(t.category)) return 0;
        if (ParsedTxn.DEBIT.equals(t.type)) return t.amount;
        if (ParsedTxn.CREDIT.equals(t.type)) return -t.amount;
        return 0;
    }

    public Set<String> countedCategories() {
        Set<String> s = new HashSet<>();
        for (Category c : categories()) if (c.counted && !Categorizer.neverCounted(c.name)) s.add(c.name);
        return s;
    }

    public Summary summary(long start, long end) {
        Set<String> counted = countedCategories();
        Summary s = new Summary();
        for (Txn t : list(start, end, null, null, null, null)) {
            s.count++;
            double v = spendValue(t, counted);
            if (v != 0) {
                s.spent += v;
                s.byCategory.merge(t.category, v, Double::sum);
                if (v > 0) {
                    s.byMode.merge(t.mode == null ? ParsedTxn.MODE_OTHER : t.mode, v, Double::sum);
                    s.byMerchant.merge(t.displayName(), v, Double::sum);
                }
            } else if (t.excluded || t.duplicate) {
                // ignored entirely
            } else if (ParsedTxn.CREDIT.equals(t.type)) {
                s.income += t.amount;
            } else {
                s.notCounted += t.amount;
            }
        }
        return s;
    }

    public double spentBetween(long start, long end) {
        return summary(start, end).spent;
    }
}
