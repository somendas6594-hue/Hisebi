package com.somen.hisab;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.somen.hisab.core.ParsedTxn;

import java.util.Set;

/** Posts a small note for each new spend, and a warning when the monthly budget is crossed. */
public final class Notifier {

    private Notifier() {}

    static final String CH_SPENDS = "spends";
    static final String CH_BUDGET = "budget";
    public static final String KEY_NOTIFY = "notify_each";
    public static final String KEY_BUDGET = "budget";
    private static final String KEY_BUDGET_ALERT = "budget_alert";

    public static void createChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel a = new NotificationChannel(CH_SPENDS, "New spends", NotificationManager.IMPORTANCE_LOW);
        a.setDescription("A quiet note each time money is spent");
        NotificationChannel b = new NotificationChannel(CH_BUDGET, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT);
        b.setDescription("When this month's spending crosses 80% and 100% of your budget");
        nm.createNotificationChannel(a);
        nm.createNotificationChannel(b);
    }

    private static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 33
                || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static PendingIntent openApp(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void onNewTransaction(Context c, long id) {
        if (!allowed(c)) return;
        Db db = Db.get(c);
        Txn t = db.txn(id);
        if (t == null || t.duplicate) return;
        createChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;

        double spent = db.spentBetween(Ui.monthStart(0), Ui.monthEnd(0));

        if (!"0".equals(db.meta(KEY_NOTIFY, "1"))) {
            String title;
            if (ParsedTxn.DEBIT.equals(t.type)) title = Ui.moneyExact(t.amount) + " spent · " + t.displayName();
            else if (ParsedTxn.CREDIT.equals(t.type)) title = Ui.moneyExact(t.amount) + " received · " + t.displayName();
            else title = Ui.moneyExact(t.amount) + " card bill paid";
            Set<String> counted = db.countedCategories();
            String line = t.category + (counted.contains(t.category) ? "" : " (not counted)")
                    + " · This month: " + Ui.money(spent);
            Notification n = new Notification.Builder(c, CH_SPENDS)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(title)
                    .setContentText(line)
                    .setContentIntent(openApp(c))
                    .setAutoCancel(true)
                    .build();
            nm.notify(1000 + (int) (id % 1_000_000), n);
        }

        double budget = budget(db);
        if (budget > 0) {
            int level = spent >= budget ? 100 : spent >= 0.8 * budget ? 80 : 0;
            String key = Ui.monthKey(0) + ":" + level;
            String last = db.meta(KEY_BUDGET_ALERT, "");
            if (level > 0 && !last.equals(key) && !(last.startsWith(Ui.monthKey(0)) && last.endsWith(":100"))) {
                db.setMeta(KEY_BUDGET_ALERT, key);
                String title = level == 100 ? "Budget crossed" : "80% of budget used";
                Notification n = new Notification.Builder(c, CH_BUDGET)
                        .setSmallIcon(R.drawable.ic_stat)
                        .setContentTitle(title)
                        .setContentText(Ui.money(spent) + " of " + Ui.money(budget) + " spent in " + Ui.monthLabel(0))
                        .setContentIntent(openApp(c))
                        .setAutoCancel(true)
                        .build();
                nm.notify(1, n);
            }
        }
    }

    public static double budget(Db db) {
        try {
            return Double.parseDouble(db.meta(KEY_BUDGET, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
