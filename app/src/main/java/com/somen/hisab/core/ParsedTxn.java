package com.somen.hisab.core;

/** Result of reading one bank / UPI / card SMS. Pure Java so it can be unit tested anywhere. */
public class ParsedTxn {
    /** Money left your account or card: a real spend (or a transfer, decided by category). */
    public static final String DEBIT = "DEBIT";
    /** Money came in: salary, refund, cashback, someone paid you. */
    public static final String CREDIT = "CREDIT";
    /** You paid your credit card bill. Never counted as spending (the card swipes already were). */
    public static final String CARD_BILL = "CARD_BILL";

    public static final String MODE_UPI = "UPI";
    public static final String MODE_CREDIT_CARD = "Credit card";
    public static final String MODE_DEBIT_CARD = "Debit card";
    public static final String MODE_CARD = "Card";
    public static final String MODE_ATM = "Cash (ATM)";
    public static final String MODE_BANK = "Bank transfer";
    public static final String MODE_CASH = "Cash";
    public static final String MODE_OTHER = "Other";

    public double amount;
    public String type;
    public String mode;
    public String merchant;   // cleaned, upper case, may be "" if unknown
    public String account;    // last digits of account / card, may be ""

    @Override
    public String toString() {
        return type + " " + amount + " [" + mode + "] " + merchant + " acct=" + account;
    }
}
