package com.somen.hisab.core;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Decides a category from the merchant name. User rules win over the built-in ones. */
public final class Categorizer {

    private Categorizer() {}

    public static final String FOOD = "Food & dining";
    public static final String GROCERIES = "Groceries";
    public static final String SHOPPING = "Shopping";
    public static final String TRANSPORT = "Transport & fuel";
    public static final String TRAVEL = "Travel";
    public static final String BILLS = "Bills & recharge";
    public static final String SUBSCRIPTIONS = "Subscriptions";
    public static final String HEALTH = "Health";
    public static final String RENT = "Rent & home";
    public static final String ENTERTAINMENT = "Entertainment";
    public static final String EDUCATION = "Education";
    public static final String PEOPLE = "Sent to people";
    public static final String CASH = "Cash withdrawal";
    public static final String OTHERS = "Others";
    public static final String INVESTMENTS = "Investments";
    public static final String CARD_BILL = "Credit card bill";
    public static final String SELF = "Self transfer";
    public static final String INCOME = "Income & refunds";

    /** name, counted as spending (1/0), colour. */
    public static final String[][] DEFAULT_CATEGORIES = {
            {FOOD, "1", "#E8590C"},
            {GROCERIES, "1", "#2F9E44"},
            {SHOPPING, "1", "#AE3EC9"},
            {TRANSPORT, "1", "#1971C2"},
            {TRAVEL, "1", "#0C8599"},
            {BILLS, "1", "#F08C00"},
            {SUBSCRIPTIONS, "1", "#C2255C"},
            {HEALTH, "1", "#E03131"},
            {RENT, "1", "#5F3DC4"},
            {ENTERTAINMENT, "1", "#D6336C"},
            {EDUCATION, "1", "#3B5BDB"},
            {PEOPLE, "1", "#66A80F"},
            {CASH, "1", "#868E96"},
            {OTHERS, "1", "#495057"},
            {INVESTMENTS, "0", "#087F5B"},
            {CARD_BILL, "0", "#ADB5BD"},
            {SELF, "0", "#ADB5BD"},
            {INCOME, "0", "#37B24D"},
    };

    /** keyword (matched against the merchant name), category. */
    public static final String[][] DEFAULT_RULES = {
            // food
            {"swiggy", FOOD}, {"zomato", FOOD}, {"eatsure", FOOD}, {"dominos", FOOD}, {"domino", FOOD},
            {"mcdonald", FOOD}, {"kfc", FOOD}, {"pizza", FOOD}, {"burger", FOOD}, {"restaurant", FOOD},
            {"cafe", FOOD}, {"coffee", FOOD}, {"starbucks", FOOD}, {"bakery", FOOD}, {"biryani", FOOD},
            {"chai", FOOD}, {"food", FOOD}, {"kitchen", FOOD}, {"dhaba", FOOD}, {"subway", FOOD},
            {"haldiram", FOOD}, {"barbeque", FOOD}, {"sweets", FOOD}, {"juice", FOOD},
            // groceries
            {"instamart", GROCERIES}, {"blinkit", GROCERIES}, {"zepto", GROCERIES}, {"bigbasket", GROCERIES},
            {"bbnow", GROCERIES}, {"dmart", GROCERIES}, {"avenue supermart", GROCERIES}, {"jiomart", GROCERIES},
            {"reliance fresh", GROCERIES}, {"reliance retail", GROCERIES}, {"more retail", GROCERIES},
            {"ratnadeep", GROCERIES}, {"supermarket", GROCERIES}, {"kirana", GROCERIES}, {"grocery", GROCERIES},
            {"vegetable", GROCERIES}, {"fruits", GROCERIES}, {"milk", GROCERIES}, {"dairy", GROCERIES},
            {"nandini", GROCERIES}, {"country delight", GROCERIES}, {"milkbasket", GROCERIES}, {"spar", GROCERIES},
            // shopping
            {"amazon", SHOPPING}, {"amzn", SHOPPING}, {"flipkart", SHOPPING}, {"myntra", SHOPPING},
            {"ajio", SHOPPING}, {"meesho", SHOPPING}, {"nykaa", SHOPPING}, {"tatacliq", SHOPPING},
            {"decathlon", SHOPPING}, {"lifestyle", SHOPPING}, {"westside", SHOPPING}, {"croma", SHOPPING},
            {"reliance digital", SHOPPING}, {"ikea", SHOPPING}, {"pantaloons", SHOPPING}, {"trends", SHOPPING},
            {"zudio", SHOPPING}, {"max fashion", SHOPPING}, {"lenskart", SHOPPING}, {"firstcry", SHOPPING},
            // transport & fuel
            {"uber", TRANSPORT}, {"ola", TRANSPORT}, {"olacabs", TRANSPORT}, {"rapido", TRANSPORT},
            {"namma yatri", TRANSPORT}, {"bmtc", TRANSPORT}, {"bmrcl", TRANSPORT}, {"metro", TRANSPORT},
            {"petrol", TRANSPORT}, {"fuel", TRANSPORT}, {"hpcl", TRANSPORT}, {"bpcl", TRANSPORT},
            {"iocl", TRANSPORT}, {"indian oil", TRANSPORT}, {"bharat petroleum", TRANSPORT}, {"hindustan petroleum", TRANSPORT},
            {"shell", TRANSPORT}, {"fastag", TRANSPORT}, {"parking", TRANSPORT}, {"service station", TRANSPORT},
            // travel
            {"irctc", TRAVEL}, {"redbus", TRAVEL}, {"makemytrip", TRAVEL}, {"goibibo", TRAVEL},
            {"cleartrip", TRAVEL}, {"yatra", TRAVEL}, {"ixigo", TRAVEL}, {"indigo", TRAVEL},
            {"air india", TRAVEL}, {"akasa", TRAVEL}, {"spicejet", TRAVEL}, {"oyo", TRAVEL},
            {"booking.com", TRAVEL}, {"airbnb", TRAVEL}, {"hotel", TRAVEL}, {"abhibus", TRAVEL},
            // bills
            {"airtel", BILLS}, {"jio", BILLS}, {"vodafone", BILLS}, {"vi", BILLS}, {"bsnl", BILLS},
            {"bescom", BILLS}, {"wbsedcl", BILLS}, {"cesc", BILLS}, {"electricity", BILLS}, {"bbps", BILLS},
            {"billdesk", BILLS}, {"broadband", BILLS}, {"act fibernet", BILLS}, {"hathway", BILLS},
            {"recharge", BILLS}, {"tata play", BILLS}, {"dth", BILLS}, {"indane", BILLS}, {"bharat gas", BILLS},
            {"hp gas", BILLS}, {"gas", BILLS}, {"water", BILLS}, {"bwssb", BILLS}, {"insurance", BILLS},
            {"lic", BILLS},
            // subscriptions
            {"netflix", SUBSCRIPTIONS}, {"spotify", SUBSCRIPTIONS}, {"hotstar", SUBSCRIPTIONS},
            {"jiocinema", SUBSCRIPTIONS}, {"jiohotstar", SUBSCRIPTIONS}, {"prime video", SUBSCRIPTIONS},
            {"youtube", SUBSCRIPTIONS}, {"google play", SUBSCRIPTIONS}, {"google", SUBSCRIPTIONS},
            {"apple", SUBSCRIPTIONS}, {"openai", SUBSCRIPTIONS}, {"chatgpt", SUBSCRIPTIONS},
            {"anthropic", SUBSCRIPTIONS}, {"claude", SUBSCRIPTIONS}, {"sonyliv", SUBSCRIPTIONS},
            {"zee5", SUBSCRIPTIONS}, {"higgsfield", SUBSCRIPTIONS}, {"microsoft", SUBSCRIPTIONS},
            // health
            {"pharmacy", HEALTH}, {"pharma", HEALTH}, {"medical", HEALTH}, {"apollo", HEALTH},
            {"medplus", HEALTH}, {"1mg", HEALTH}, {"pharmeasy", HEALTH}, {"netmeds", HEALTH},
            {"hospital", HEALTH}, {"clinic", HEALTH}, {"diagnostic", HEALTH}, {"labs", HEALTH},
            {"cult", HEALTH}, {"gym", HEALTH}, {"chemist", HEALTH}, {"dental", HEALTH},
            // rent & home
            {"rent", RENT}, {"nobroker", RENT}, {"maintenance", RENT}, {"society", RENT},
            {"apartment", RENT}, {"urban company", RENT}, {"urbanclap", RENT}, {"housing", RENT},
            // entertainment
            {"bookmyshow", ENTERTAINMENT}, {"pvr", ENTERTAINMENT}, {"inox", ENTERTAINMENT},
            {"cinepolis", ENTERTAINMENT}, {"district", ENTERTAINMENT}, {"paytm insider", ENTERTAINMENT},
            {"steam", ENTERTAINMENT}, {"playstation", ENTERTAINMENT},
            // education
            {"school", EDUCATION}, {"college", EDUCATION}, {"university", EDUCATION}, {"bits pilani", EDUCATION},
            {"coursera", EDUCATION}, {"udemy", EDUCATION}, {"byju", EDUCATION}, {"unacademy", EDUCATION},
            {"tuition", EDUCATION}, {"books", EDUCATION},
            // investments (not counted as spending by default)
            {"zerodha", INVESTMENTS}, {"groww", INVESTMENTS}, {"upstox", INVESTMENTS}, {"kuvera", INVESTMENTS},
            {"coin by", INVESTMENTS}, {"indmoney", INVESTMENTS}, {"mutual fund", INVESTMENTS},
            {"bse ltd", INVESTMENTS}, {"nse clearing", INVESTMENTS}, {"iccl", INVESTMENTS},
            {"indian clearing", INVESTMENTS}, {"nps", INVESTMENTS}, {"ppf", INVESTMENTS}, {"sip", INVESTMENTS},
            {"smallcase", INVESTMENTS}, {"paytm money", INVESTMENTS},
            // credit card bill payments (not counted: the swipes already were)
            {"cred", CARD_BILL}, {"credit card", CARD_BILL}, {"cc payment", CARD_BILL}, {"ccpayment", CARD_BILL},
            {"card bill", CARD_BILL}, {"cardbill", CARD_BILL}, {"sbi card", CARD_BILL}, {"cc bill", CARD_BILL},
            {"billpay cc", CARD_BILL},
    };

    private static final Pattern CARD_BILL_BODY = Pattern.compile(
            "credit card.{0,40}(?:payment|bill|billpay|dues)|(?:payment|bill|billpay|dues).{0,40}credit card|\\bcc (?:payment|bill)",
            Pattern.CASE_INSENSITIVE);

    /**
     * @param userRules list of {keyword, category} the user created; these win over the built-in ones.
     */
    public static String categorize(ParsedTxn t, String body, List<String[]> userRules) {
        if (ParsedTxn.CARD_BILL.equals(t.type)) return CARD_BILL;
        String merchant = t.merchant == null ? "" : t.merchant;

        String user = bestMatch(merchant, userRules);
        if (user != null) return user;

        if (ParsedTxn.CREDIT.equals(t.type)) return INCOME;

        if (body != null && CARD_BILL_BODY.matcher(body).find()
                && !Pattern.compile("\\bspent\\b|used at|purchase", Pattern.CASE_INSENSITIVE).matcher(body).find()) {
            return CARD_BILL;
        }

        String builtIn = bestMatch(merchant, DEFAULT_RULES);
        if (builtIn != null) return builtIn;

        if (ParsedTxn.MODE_ATM.equals(t.mode)) return CASH;
        if (merchant.startsWith("PHONE ")) return PEOPLE;
        return OTHERS;
    }

    private static String bestMatch(String merchant, List<String[]> rules) {
        if (rules == null || rules.isEmpty()) return null;
        return bestMatch(merchant, rules.toArray(new String[0][]));
    }

    /** Longest matching keyword wins, so "swiggy instamart" goes to Groceries, not Food. */
    private static String bestMatch(String merchant, String[][] rules) {
        if (merchant == null || merchant.isEmpty()) return null;
        String norm = normalize(merchant);
        String squashed = norm.replace(" ", "");
        String best = null;
        int bestLen = 0;
        for (String[] r : rules) {
            String k = normalize(r[0]);
            if (k.isEmpty() || k.length() <= bestLen) continue;
            if (matches(norm, squashed, k)) {
                best = r[1];
                bestLen = k.length();
            }
        }
        return best;
    }

    public static boolean keywordMatches(String merchant, String keyword) {
        String norm = normalize(merchant);
        String k = normalize(keyword);
        return !k.isEmpty() && matches(norm, norm.replace(" ", ""), k);
    }

    private static boolean matches(String norm, String squashed, String k) {
        if (k.length() >= 5) return squashed.contains(k.replace(" ", ""));
        return (" " + norm + " ").contains(" " + k + " ");
    }

    static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
