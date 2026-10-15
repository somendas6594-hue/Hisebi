package com.somen.hisab;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Saves transactions as a CSV file in Downloads (opens in Excel / Google Sheets). */
public final class CsvExport {

    private CsvExport() {}

    public static void export(Activity a, long start, long end, String baseName) {
        Db db = Db.get(a);
        List<Txn> rows = db.list(start, end, null, null, null, null);
        Set<String> counted = db.countedCategories();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        StringBuilder sb = new StringBuilder("﻿");   // BOM so Excel shows ₹ correctly
        sb.append("Date,Type,Amount,Counted as spend,Category,Name,Mode,Account,Note,Sender,SMS\n");
        for (Txn t : rows) {
            sb.append(f.format(new Date(t.time))).append(',')
                    .append(t.type).append(',')
                    .append(String.format(Locale.ENGLISH, "%.2f", t.amount)).append(',')
                    .append(String.format(Locale.ENGLISH, "%.2f", Db.spendValue(t, counted))).append(',')
                    .append(q(t.category)).append(',')
                    .append(q(t.merchant)).append(',')
                    .append(q(t.mode)).append(',')
                    .append(q(t.account)).append(',')
                    .append(q(t.note)).append(',')
                    .append(q(t.sender)).append(',')
                    .append(q(t.body)).append('\n');
        }
        String name = baseName + ".csv";
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            ContentResolver cr = a.getContentResolver();
            Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IllegalStateException("no uri");
            try (OutputStream out = cr.openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("no stream");
                out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(a, "Saved " + rows.size() + " rows to Downloads/" + name, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(a, "Couldn't save the file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String q(String s) {
        if (s == null) return "";
        String clean = s.replace("\r", " ").replace("\n", " ");
        if (clean.contains(",") || clean.contains("\"")) return "\"" + clean.replace("\"", "\"\"") + "\"";
        return clean;
    }
}
