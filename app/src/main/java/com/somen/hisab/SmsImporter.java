package com.somen.hisab;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Telephony;

import com.somen.hisab.core.ParsedTxn;
import com.somen.hisab.core.SmsParser;

import java.util.List;

/** Reads the SMS inbox on the phone and stores every transaction it finds. */
public final class SmsImporter {

    private SmsImporter() {}

    public static final String KEY_LAST_SCAN = "last_scan";
    public static final String KEY_FIRST_DONE = "first_import_done";
    public static final long DAY = 86_400_000L;

    public interface Progress {
        void onProgress(int done, int total);
    }

    public static boolean hasSmsPermission(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Scans inbox messages received after {@code since} (ms). Already-stored SMS are skipped, so it is
     * safe to call repeatedly. Returns the number of new transactions.
     */
    public static int scan(Context c, long since, Progress progress) {
        if (!hasSmsPermission(c)) return 0;
        Db db = Db.get(c);
        List<String[]> rules = db.userRules();
        int added = 0;
        long newest = db.metaLong(KEY_LAST_SCAN, 0);
        String[] cols = {Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE};
        try (Cursor cur = c.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, cols,
                Telephony.Sms.DATE + ">?", new String[]{String.valueOf(since)}, Telephony.Sms.DATE + " ASC")) {
            if (cur == null) return 0;
            int total = cur.getCount();
            int i = 0;
            while (cur.moveToNext()) {
                i++;
                String sender = cur.getString(0);
                String body = cur.getString(1);
                long time = cur.getLong(2);
                if (time > newest) newest = time;
                if (sender == null || body == null) continue;
                ParsedTxn p = SmsParser.parse(sender, body);
                if (p != null && db.insertParsed(p, sender, body, time, rules) > 0) added++;
                if (progress != null && (i % 50 == 0 || i == total)) progress.onProgress(i, total);
            }
        } catch (SecurityException e) {
            return added;
        }
        db.setMeta(KEY_LAST_SCAN, String.valueOf(newest));
        return added;
    }

    /** Catch up on anything that arrived while the app was closed (with 2 days overlap, duplicates are skipped). */
    public static int catchUp(Context c) {
        Db db = Db.get(c);
        long last = db.metaLong(KEY_LAST_SCAN, 0);
        long since = last == 0 ? System.currentTimeMillis() - 365 * DAY : last - 2 * DAY;
        return scan(c, since, null);
    }
}
