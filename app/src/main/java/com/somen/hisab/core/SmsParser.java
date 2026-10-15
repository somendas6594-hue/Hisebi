package com.somen.hisab.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Indian bank / UPI / credit card transaction SMS and pulls out amount, direction,
 * payment mode, merchant and account. Returns null for anything that is not a completed
 * transaction (OTPs, offers, bill reminders, failed payments, money requests...).
 */
public final class SmsParser {

    private SmsParser() {}

    private static final int CI = Pattern.CASE_INSENSITIVE;

    private static final String NUM = "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)";
    private static final Pattern AMOUNT = Pattern.compile("(?:\\brs\\.?|\\binr|₹)\\s*:?\\s*" + NUM, CI);
    // SBI style: "A/C X1234 debited by 250.0 on date ..."
    private static final Pattern AMOUNT_BY = Pattern.compile(
            "(?:debited|credited|deducted)\\s+(?:by|for|with)\\s+(?:rs\\.?|inr|₹)?\\s*" + NUM, CI);

    private static final Pattern OTP = Pattern.compile(
            "\\botp\\b|one[ -]time password|verification code|security code", CI);
    private static final Pattern STRONG_TXN = Pattern.compile(
            "debited|deducted|spent|withdrawn|\\bsent\\b|credited|\\bpaid\\b", CI);

    /** Never a completed transaction. */
    private static final Pattern HARD_SKIP = Pattern.compile(
            "\\bfailed\\b|declined|unsuccessful|could not be processed|not been processed"
            + "|has requested|requested money|collect request|is requesting|request of"
            + "|will be (?:auto[- ]?)?debited|to be debited|will be credited|would be debited|shall be debited"
            + "|withheld|on hold|into emis?\\b|emi option|convert(?:ed)? (?:in)?to emi|split your"
            + "|\\bfolio\\b|challan|units? allotted|nav of|passbook balance|\\bepf\\b|bit\\.ly|tradeconf|auto-?debit (?:is )?(?:set|registered|activated)|to deactivate|auto-?renew|you'll be charged|will be charged"
            + "|minimum amount due|min\\.? amt\\.? due|total amount due|amount due|amt due|\\bis due\\b|due on|due date|due by"
            + "|statement (?:has been |is )?generated|bill (?:has been |is )?generated|e-?statement"
            + "|mandate (?:has been |is )?(?:registered|created|successfully set)",
            CI);

    /** Promotional / informational, unless the SMS also clearly says money moved. */
    private static final Pattern SOFT_SKIP = Pattern.compile(
            "\\boffer|eligible|apply now|pre-?approved|\\bloan\\b|reward points|mandate|autopay|auto-pay|reminder|cashback|\\bwin\\b|\\bwon\\b|congratulations",
            CI);

    private static final Pattern DEBIT_WORDS = Pattern.compile(
            "debited|debit of|\\btxn (?:rs|inr|of)|\\bspent\\b|\\bsent\\b|\\bpaid\\b|deducted|withdrawn|withdrawal|purchase|\\btxn of|transaction of|used at|used for|charged|\\bdr\\.? (?:rs|inr)",
            CI);
    private static final Pattern CREDIT_WORDS = Pattern.compile(
            "credited|\\breceived\\b|deposited|refund|reversed|reversal|cashback|\\bcr\\.? (?:rs|inr)", CI);

    private static final Pattern CARD_BILL = Pattern.compile(
            "(?:payment|repayment)(?: of)?\\s*(?:rs\\.?|inr|₹)?[^a-z]{0,20}(?:has been |is |was )?(?:received|credited|successful|towards)"
            + "|received (?:your |a |the )?payment|thank you for (?:the |your )?payment|payment received|payment of .{0,30} received"
            + "|payment .{0,60}(?:was |has been |is )?credited to your (?:credit )?card",
            CI);

    /** A shop's own receipt ("we have received a payment of Rs 499 for your Airtel ...") duplicates the bank SMS. */
    private static final Pattern MERCHANT_RECEIPT = Pattern.compile(
            "(?:we have |we've )?received (?:a |your )?payment of|thank you for (?:the |your )?payment"
            + "|thank you for (?:your )?(?:purchase|shopping|order)|download your (?:bill|invoice)|you're covered", CI);
    private static final Pattern BANK_MOVE = Pattern.compile(
            "debited|credited to (?:your )?(?:a/c|ac|account)|deposited|\\bsent\\b|\\bspent\\b", CI);
    private static final Pattern FOREIGN = Pattern.compile(
            "\\b(?:USD|SGD|EUR|GBP|AED|THB|MYR|JPY|AUD|CAD|CHF|HKD|CNY|SAR|QAR|IDR|VND|LKR|NPR|KWD|OMR|BHD|NZD)\\s*[0-9]");
    private static final Pattern HAS_INR = Pattern.compile("\\brs\\.?|\\binr\\b|₹", CI);
    private static final Pattern BENEFICIARY = Pattern.compile(
            "credited to (?:the )?beneficiary\\s+([A-Za-z][A-Za-z .]{1,40}?)\\s+(?:on|at|\\.|-)", CI);

    // ---- merchant patterns (tried in order) ----
    private static final String NAME = "([A-Za-z0-9.+][A-Za-z0-9&'._\\-*@/+ ]{1,45}?)";
    private static final String END = "\\s*(?:\\bon\\b|\\bvia\\b|\\busing\\b|\\bfor\\b|\\bfrom\\b|\\bref|\\bavl|\\btxn|\\bupi\\b|\\bimps\\b|\\bdate\\b|\\bthru\\b|\\bby\\b|\\bif\\b|\\bnot\\b|\\bbal|\\(|\\.\\s|\\.$|,|;|-\\s|$)";
    private static final Pattern M_AT = Pattern.compile("\\bat\\s+" + NAME + END, CI);
    private static final Pattern M_ICICI = Pattern.compile(";\\s*" + NAME + "\\s+credited", CI);
    private static final Pattern M_TO = Pattern.compile(
            "\\b(?:to|towards|trf to|transfer to)\\s+(?:vpa\\s+|a/c\\s+|ac\\s+)?" + NAME + END, CI);
    private static final Pattern M_PATH = Pattern.compile(
            "\\b(?:UPI|IMPS|NEFT|RTGS|MOB|POS|ECOM)/.{3,90}?(?=\\.?\\s+(?:not you|not done|avl|chk bal|view bal|for any|call|click|ref\\b|-\\s)|\\.(?:view|chk) bal|\\.\\s|\\s-\\s|\\.?$)",
            CI);
    // Axis card: "Spent INR 250 Axis Bank Card no. XX1234 12-10-26 10:11:12 IST Swiggy Limi Avl Limit: ..."
    private static final Pattern M_IST = Pattern.compile(
            "\\bIST\\s+([A-Za-z0-9*][A-Za-z0-9&'._\\-* ]{1,40}?)\\s*(?:\\bavl\\b|\\bnot you|\\.\\s|$)", CI);
    private static final Pattern M_INFO = Pattern.compile("\\binfo[:\\-\\s]+([A-Za-z0-9/\\-@._* ]{3,60})", CI);
    private static final Pattern M_VPA = Pattern.compile("([A-Za-z0-9.\\-_]{2,}@[A-Za-z]{2,})");
    private static final Pattern M_ON = Pattern.compile(
            "\\bon\\s+\\+?([A-Za-z][A-Za-z0-9&'._\\- ]{1,40}?)\\s*(?:\\.\\s|\\.$|,|;|\\bavl|\\bon\\b|\\bif\\b|\\s\\d{1,2}[-/]|$)", CI);
    private static final Pattern M_FROM = Pattern.compile(
            "\\b(?:from|by)\\s+(?:vpa\\s+)?" + NAME + END, CI);

    private static final Pattern ACCOUNT = Pattern.compile(
            "(?:[xX*]{1,14}\\s?|ending(?:\\s+with)?\\s*|card\\s+(?:no\\.?\\s*)?)([0-9]{3,6})\\b", CI);

    /** Transaction SMS in India come from DLT headers like "AX-HDFCBK", never from a phone number. */
    public static boolean isLikelyBankSender(String sender) {
        if (sender == null) return false;
        String s = sender.trim();
        if (s.startsWith("+")) return false;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) return true;
        }
        return false;
    }

    public static ParsedTxn parse(String sender, String body) {
        if (body == null || body.length() < 12) return null;
        if (!isLikelyBankSender(sender)) return null;

        String text = body.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        String lower = text.toLowerCase(Locale.ROOT);

        boolean strong = STRONG_TXN.matcher(text).find();
        if (OTP.matcher(text).find() && !strong) return null;
        if (HARD_SKIP.matcher(text).find()) return null;
        if (SOFT_SKIP.matcher(text).find() && !strong) return null;

        if (!HAS_INR.matcher(text).find() && FOREIGN.matcher(text).find()) return null;  // forex card, not rupees

        Double amount = findAmount(text);
        if (amount == null || amount <= 0) return null;

        ParsedTxn t = new ParsedTxn();
        t.amount = amount;

        boolean mentionsCard = lower.contains("card");
        if (mentionsCard && !lower.contains("debit card") && CARD_BILL.matcher(text).find()
                && !Pattern.compile("\\bspent\\b|used at|purchase", CI).matcher(text).find()) {
            t.type = ParsedTxn.CARD_BILL;
        } else if (MERCHANT_RECEIPT.matcher(text).find() && !BANK_MOVE.matcher(text).find()) {
            return null;
        } else if (BENEFICIARY.matcher(text).find()) {
            // "Your NEFT txn ... is credited to beneficiary Ravi Kumar": money went OUT
            t.type = ParsedTxn.DEBIT;
        } else {
            int d = firstIndex(DEBIT_WORDS, text);
            int c = firstIndex(CREDIT_WORDS, text);
            if (d >= 0 && (c < 0 || d < c)) t.type = ParsedTxn.DEBIT;
            else if (c >= 0) t.type = ParsedTxn.CREDIT;
            else return null;
        }

        t.mode = detectMode(lower);
        t.account = findAccount(text);
        t.toPerson = Pattern.compile("/P2A/|\\bP2A\\b", CI).matcher(text).find();
        t.selfTransfer = Pattern.compile("SELFFT|self transfer|own account", CI).matcher(text).find();
        Matcher mob = Pattern.compile("a/c linked to mobile (?:no\\.? )?[x*]*([0-9]{3,4})", CI).matcher(text);
        Matcher ben = BENEFICIARY.matcher(text);
        if (ben.find()) {
            t.merchant = cleanMerchant(ben.group(1));
            t.toPerson = true;
        } else if (mob.find()) {
            t.merchant = "MOBILE ••" + mob.group(1);
        } else if (ParsedTxn.CREDIT.equals(t.type)) {
            t.merchant = firstGood(text, M_PATH, M_FROM, M_VPA, M_INFO);
        } else {
            t.merchant = firstGood(text, M_AT, M_IST, M_ICICI, M_PATH, M_TO, M_INFO, M_VPA, M_ON);
        }
        if (t.merchant == null) t.merchant = "";
        if (ParsedTxn.MODE_ATM.equals(t.mode) && t.merchant.isEmpty()) t.merchant = "ATM";
        return t;
    }

    static Double findAmount(String text) {
        Matcher m = AMOUNT.matcher(text);
        while (m.find()) {
            int from = Math.max(0, m.start() - 22);
            String before = text.substring(from, m.start()).toLowerCase(Locale.ROOT);
            if (before.contains("bal") || before.contains("limit") || before.contains("due")) continue;
            Double v = toNumber(m.group(1));
            if (v != null && v > 0) return v;
        }
        Matcher b = AMOUNT_BY.matcher(text);
        if (b.find()) return toNumber(b.group(1));
        return null;
    }

    private static Double toNumber(String s) {
        try {
            String clean = s.replace(",", "");
            if (clean.endsWith(".")) clean = clean.substring(0, clean.length() - 1);
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int firstIndex(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.start() : -1;
    }

    static String detectMode(String lower) {
        if (lower.contains("credit card") || lower.contains("creditcard") || lower.contains("sbi card")
                || lower.contains("amex") || lower.contains("cc ending") || lower.contains("credit crd")) {
            return ParsedTxn.MODE_CREDIT_CARD;
        }
        if (lower.contains("debit card") || lower.contains("debitcard")) return ParsedTxn.MODE_DEBIT_CARD;
        if (lower.contains("upi") || lower.contains("vpa") || Pattern.compile("[a-z0-9.\\-_]{2,}@[a-z]{2,}").matcher(lower).find()) {
            return ParsedTxn.MODE_UPI;
        }
        if (lower.contains("atm") || lower.contains("cash withdrawal") || lower.contains("withdrawn")) return ParsedTxn.MODE_ATM;
        if (lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs") || lower.contains("net banking")
                || lower.contains("netbanking")) {
            return ParsedTxn.MODE_BANK;
        }
        if (lower.contains("card")) return ParsedTxn.MODE_CARD;
        return ParsedTxn.MODE_OTHER;
    }

    static String findAccount(String text) {
        Matcher m = ACCOUNT.matcher(text);
        while (m.find()) {
            String digits = m.group(1);
            // skip things that look like dates or amounts glued to an x (e.g. "2x500")
            if (digits.length() >= 3) {
                return digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
            }
        }
        return "";
    }

    private static String firstGood(String text, Pattern... patterns) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                String cleaned = cleanMerchant(m.groupCount() >= 1 ? m.group(1) : m.group());
                if (cleaned != null) return cleaned;
            }
        }
        return null;
    }

    private static final String[] REJECT_EXACT = {
            "YOUR", "YOU", "BANK", "US", "A/C", "AC", "ACCOUNT", "NEFT", "IMPS", "RTGS", "UPI", "THE", "SELF",
            "YOUR A/C", "YOUR ACCOUNT", "YOUR AC", "CARD", "DATE", "MOBILE", "CUSTOMER CARE", "CALL", "SMS",
            "BENEFICIARY", "TRANSFER", "NO", "NO.", "A/C NO", "NET BANKING", "NETBANKING", "BRANCH", "THIS", "IT", "ATM CARD"
    };

    private static final java.util.Set<String> PATH_WORDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "UPI", "P2M", "P2A", "IMPS", "NEFT", "RTGS", "MOB", "SELFFT", "POS", "ECOM", "PAYM", "PAYMENT", "DR", "CR",
            "HDFC", "ICIC", "KKBK", "SBIN", "UTIB", "YESB", "INDB", "BARB", "PUNB", "CNRB", "IDFB"));

    static String cleanMerchant(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.toLowerCase(Locale.ROOT).startsWith("vpa ")) s = s.substring(4).trim();

        // UPI / bank paths: "UPI/P2M/123456/SWIGGY", "UPI/P2A/123/ANITA /HDFC/Paym",
        // "MOB/SELFFT/RAHUL KUMAR/9", "NEFT/CITIN26123/ACME" -> the name part
        if (s.contains("/") && !s.toUpperCase(Locale.ROOT).startsWith("A/C")) {
            String[] parts = s.split("/");
            String afterRef = null, firstName = null;
            boolean sawRef = false;
            for (String part : parts) {
                String p = part.trim();
                String up = p.toUpperCase(Locale.ROOT);
                boolean isRef = p.replaceAll("[^0-9]", "").length() >= 5;
                if (isRef) { sawRef = true; continue; }
                if (p.length() < 3 || !p.matches(".*[A-Za-z].*")) continue;
                if (PATH_WORDS.contains(up)) continue;
                if (firstName == null) firstName = p;
                if (sawRef && afterRef == null) afterRef = p;
            }
            String best = afterRef != null ? afterRef : firstName;
            if (best != null) s = best;
        }

        boolean wasPhoneVpa = false;
        int at = s.indexOf('@');
        if (at > 0) {
            String handle = s.substring(0, at);
            if (handle.replaceAll("[^0-9]", "").length() >= 8) wasPhoneVpa = true;
            s = handle;
        }

        s = s.replaceAll("[*_]+", " ").replaceAll("\\s+", " ").trim();
        s = s.replaceAll("^[\\-.,'+ ]+|[\\-.,'_ ]+$", "");
        if (s.isEmpty()) return null;

        String up = s.toUpperCase(Locale.ROOT);
        if (wasPhoneVpa) {
            String digits = up.replaceAll("[^0-9]", "");
            return "PHONE ••" + digits.substring(Math.max(0, digits.length() - 4));
        }
        if (!up.matches(".*[A-Z].*")) return null;              // only digits / time
        if (up.matches("^[0-9]{6,}.*")) return null;             // phone / ref numbers, not a merchant
        for (String r : REJECT_EXACT) if (up.equals(r)) return null;
        if (up.startsWith("YOUR ") || up.contains("A/C") || up.contains("ACCOUNT") || up.matches(".*\\bCARD\\b.*")
                || up.matches(".*\\bBANK\\b.*X+[0-9]*.*") || up.startsWith("XX")) {
            return null;
        }
        if (up.length() > 30) up = up.substring(0, 30).trim();
        return up;
    }
}
