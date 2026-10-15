package com.somen.hisab;

/** One row of the txn table. */
public class Txn {
    public long id;
    public String smsKey;
    public long time;
    public double amount;
    public String type;      // ParsedTxn.DEBIT / CREDIT / CARD_BILL
    public String mode;
    public String merchant;
    public String account;
    public String sender;
    public String body;
    public String category;
    public boolean manualCategory;
    public boolean excluded;
    public boolean duplicate;
    public String note;

    public boolean isManualEntry() {
        return smsKey != null && smsKey.startsWith("manual:");
    }

    public String displayName() {
        if (merchant != null && !merchant.isEmpty()) return merchant;
        if (note != null && !note.isEmpty()) return note;
        if (mode != null && !mode.isEmpty()) return mode + " payment";
        return "Unknown";
    }
}
