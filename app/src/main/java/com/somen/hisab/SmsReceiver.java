package com.somen.hisab;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.somen.hisab.core.ParsedTxn;
import com.somen.hisab.core.SmsParser;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runs every time an SMS arrives, even when the app is closed. Keeps tracking going on its own. */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;

        // long SMS arrive in pieces: join them per sender
        Map<String, StringBuilder> bySender = new LinkedHashMap<>();
        for (SmsMessage m : parts) {
            if (m == null) continue;
            String from = m.getDisplayOriginatingAddress();
            if (from == null) from = m.getOriginatingAddress();
            if (from == null) continue;
            StringBuilder sb = bySender.get(from);
            if (sb == null) {
                sb = new StringBuilder();
                bySender.put(from, sb);
            }
            String part = m.getMessageBody();
            if (part != null) sb.append(part);
        }

        final Context app = context.getApplicationContext();
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                Db db = Db.get(app);
                long now = System.currentTimeMillis();
                for (Map.Entry<String, StringBuilder> e : bySender.entrySet()) {
                    String sender = e.getKey();
                    String body = e.getValue().toString();
                    ParsedTxn p = SmsParser.parse(sender, body);
                    if (p == null) continue;
                    long id = db.insertParsed(p, sender, body, now, db.userRules());
                    if (id > 0) Notifier.onNewTransaction(app, id);
                }
            } catch (Exception ignored) {
                // never crash on a strange SMS
            } finally {
                pending.finish();
            }
        }).start();
    }
}
