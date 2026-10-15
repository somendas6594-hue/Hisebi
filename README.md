# Hisab — your own spending tracker

Android app that reads bank, credit card and UPI SMS on your phone, works out where your money goes, and keeps tracking every new SMS automatically. All data stays on the phone.

## What it does
- **First open:** reads the last 12 months of SMS and builds your history.
- **After that:** every new bank/card/UPI SMS is read the moment it arrives (even with the app closed), with an optional notification.
- **Dashboard per month:** total spent, change vs last month, budget bar, 6-month trend, spend by category, by payment mode (UPI / credit card / debit card / cash), and top places.
- **Credit cards done right:** card swipes count on the day you spend. The bill payment later (to the card or via CRED) is *not counted*, so nothing is counted twice.
- **Duplicates:** if two SMS report the same payment (same amount, different sender, within 3 minutes), the second is marked duplicate and not counted.
- **Ignored:** OTPs, offers, bill reminders, statements, failed payments, money requests, and SMS from personal phone numbers.

## Customise
- Tap any transaction → rename it, change category, tick **Always put … here** to create a rule, mark it **Don't count**, or add a note.
- Menu → **Categories, rules & budget**: add your own categories, choose which ones count as spending, add/remove keyword rules, set a monthly budget (alerts at 80% and 100%), turn per-spend notifications on/off.
- Refunds: put a refund in the category it belongs to (e.g. Shopping) and it reduces that category's total.
- **+ Cash spend** for things you paid in cash.
- Export a month or everything to CSV (saved in Downloads) for Excel/Sheets.

## Where to change things in code
- `core/SmsParser.java` — how SMS are read (amount, merchant, debit/credit, card bill).
- `core/Categorizer.java` — built-in categories and keyword list.
- `core/` has no Android code, so it can be tested with plain `javac` (see `core-test/ParserTest.java`).

## Build
Push to GitHub → the **Build APK** action builds `Hisab.apk` and publishes it under **Releases**. Every build is signed with the same key (`app/hisab.keystore`), so a new APK installs over the old one and keeps your data. Keep this repository **private**.

## Install on your phone
1. Open the latest Release on your phone, download `Hisab.apk`, open it, allow "Install unknown apps" for your browser/Files when asked. If Play Protect warns, tap *More details → Install anyway*.
2. Open Hisab and tap **Allow** for SMS.
3. On Android 13+ the SMS permission may be greyed out for apps installed from a file. Go to Settings → Apps → Hisab → ⋮ (top right) → **Allow restricted settings**, then open Hisab again and tap Allow.
4. Battery: if new SMS stop being tracked on some phones (Xiaomi, Oppo, Vivo, Realme), set Hisab's battery usage to *Unrestricted* / allow *Autostart*. Anything missed is picked up next time you open the app anyway.
