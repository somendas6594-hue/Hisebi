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
    public static final String LOANS = "Loan EMI";
    public static final String TAXES = "Taxes";
    public static final String PERSONAL = "Personal care";
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
            {LOANS, "1", "#862E9C"},
            {TAXES, "1", "#A61E4D"},
            {PERSONAL, "1", "#F783AC"},
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
            {"bundl", FOOD}, {"udupi", FOOD}, {"chicken", FOOD}, {"darshini", FOOD}, {"meghana", FOOD},
            {"empire", FOOD}, {"bistro", FOOD}, {"eats", FOOD}, {"canteen", FOOD}, {"tiffin", FOOD},
            // groceries
            {"instamart", GROCERIES}, {"blinkit", GROCERIES}, {"zepto", GROCERIES}, {"bigbasket", GROCERIES},
            {"bbnow", GROCERIES}, {"dmart", GROCERIES}, {"avenue supermart", GROCERIES}, {"jiomart", GROCERIES},
            {"reliance fresh", GROCERIES}, {"reliance retail", GROCERIES}, {"more retail", GROCERIES},
            {"ratnadeep", GROCERIES}, {"supermarket", GROCERIES}, {"kirana", GROCERIES}, {"grocery", GROCERIES},
            {"vegetable", GROCERIES}, {"fruits", GROCERIES}, {"milk", GROCERIES}, {"dairy", GROCERIES},
            {"nandini", GROCERIES}, {"country delight", GROCERIES}, {"milkbasket", GROCERIES}, {"spar", GROCERIES},
            {"grofers", GROCERIES}, {"blink comme", GROCERIES}, {"delightful gourmet", GROCERIES}, {"licious", GROCERIES},
            {"bazaar", GROCERIES}, {"star bazaar", GROCERIES}, {"provision", GROCERIES}, {"general store", GROCERIES},
            {"meat", GROCERIES}, {"fresh", GROCERIES}, {"swiggy grocery", GROCERIES}, {"insta mart", GROCERIES},
            // shopping
            {"amazon", SHOPPING}, {"amzn", SHOPPING}, {"flipkart", SHOPPING}, {"myntra", SHOPPING},
            {"ajio", SHOPPING}, {"meesho", SHOPPING}, {"nykaa", SHOPPING}, {"tatacliq", SHOPPING},
            {"decathlon", SHOPPING}, {"lifestyle", SHOPPING}, {"westside", SHOPPING}, {"croma", SHOPPING},
            {"reliance digital", SHOPPING}, {"ikea", SHOPPING}, {"pantaloons", SHOPPING}, {"trends", SHOPPING},
            {"zudio", SHOPPING}, {"max fashion", SHOPPING}, {"lenskart", SHOPPING}, {"firstcry", SHOPPING},
            {"gold", SHOPPING}, {"jewel", SHOPPING}, {"diamond", SHOPPING}, {"tanishq", SHOPPING}, {"malabar", SHOPPING},
            // transport & fuel
            {"uber", TRANSPORT}, {"ola", TRANSPORT}, {"olacabs", TRANSPORT}, {"rapido", TRANSPORT},
            {"namma yatri", TRANSPORT}, {"bmtc", TRANSPORT}, {"bmrcl", TRANSPORT}, {"metro", TRANSPORT},
            {"petrol", TRANSPORT}, {"fuel", TRANSPORT}, {"hpcl", TRANSPORT}, {"bpcl", TRANSPORT},
            {"iocl", TRANSPORT}, {"indian oil", TRANSPORT}, {"bharat petroleum", TRANSPORT}, {"hindustan petroleum", TRANSPORT},
            {"shell", TRANSPORT}, {"fastag", TRANSPORT}, {"parking", TRANSPORT}, {"service station", TRANSPORT},
            // travel
            {"irctc", TRAVEL}, {"indian railways", TRAVEL}, {"railway", TRAVEL}, {"redbus", TRAVEL}, {"makemytrip", TRAVEL}, {"goibibo", TRAVEL},
            {"cleartrip", TRAVEL}, {"yatra", TRAVEL}, {"ixigo", TRAVEL}, {"indigo", TRAVEL},
            {"air india", TRAVEL}, {"akasa", TRAVEL}, {"spicejet", TRAVEL}, {"oyo", TRAVEL},
            {"booking.com", TRAVEL}, {"airbnb", TRAVEL}, {"hotel", TRAVEL}, {"abhibus", TRAVEL},
            // bills
            {"airtel", BILLS}, {"jio", BILLS}, {"vodafone", BILLS}, {"vi", BILLS}, {"bsnl", BILLS},
            {"bescom", BILLS}, {"wbsedcl", BILLS}, {"cesc", BILLS}, {"electricity", BILLS}, {"bbps", BILLS},
            {"billdesk", BILLS}, {"broadband", BILLS}, {"act fibernet", BILLS}, {"hathway", BILLS},
            {"recharge", BILLS}, {"tata play", BILLS}, {"dth", BILLS}, {"indane", BILLS}, {"bharat gas", BILLS},
            {"hp gas", BILLS}, {"gas", BILLS}, {"water", BILLS}, {"bwssb", BILLS}, {"insurance", BILLS},
            {"lic", BILLS}, {"bharti", BILLS}, {"reliance jio", BILLS}, {"jio infoco", BILLS},
            // subscriptions
            {"netflix", SUBSCRIPTIONS}, {"spotify", SUBSCRIPTIONS}, {"hotstar", SUBSCRIPTIONS},
            {"jiocinema", SUBSCRIPTIONS}, {"jiohotstar", SUBSCRIPTIONS}, {"prime video", SUBSCRIPTIONS},
            {"youtube", SUBSCRIPTIONS}, {"google play", SUBSCRIPTIONS}, {"google", SUBSCRIPTIONS},
            {"apple", SUBSCRIPTIONS}, {"openai", SUBSCRIPTIONS}, {"chatgpt", SUBSCRIPTIONS},
            {"anthropic", SUBSCRIPTIONS}, {"claude", SUBSCRIPTIONS}, {"sonyliv", SUBSCRIPTIONS},
            {"zee5", SUBSCRIPTIONS}, {"higgsfield", SUBSCRIPTIONS}, {"microsoft", SUBSCRIPTIONS},
            {"kukufm", SUBSCRIPTIONS}, {"kuku fm", SUBSCRIPTIONS}, {"googleplay", SUBSCRIPTIONS}, {"audible", SUBSCRIPTIONS},
            // health
            {"pharmacy", HEALTH}, {"pharma", HEALTH}, {"medical", HEALTH}, {"apollo", HEALTH},
            {"medplus", HEALTH}, {"1mg", HEALTH}, {"pharmeasy", HEALTH}, {"netmeds", HEALTH},
            {"hospital", HEALTH}, {"clinic", HEALTH}, {"diagnostic", HEALTH}, {"labs", HEALTH},
            {"cult", HEALTH}, {"gym", HEALTH}, {"chemist", HEALTH}, {"dental", HEALTH},
            {"narayana", HEALTH}, {"manipal", HEALTH}, {"health", HEALTH}, {"medic", HEALTH},
            // personal care
            {"salon", PERSONAL}, {"saloon", PERSONAL}, {"spa", PERSONAL}, {"parlour", PERSONAL},
            {"parlor", PERSONAL}, {"barber", PERSONAL}, {"beauty", PERSONAL}, {"naturals", PERSONAL},
            // loans
            {"nach", LOANS}, {"emi", LOANS}, {"bajaj finance", LOANS}, {"home loan", LOANS}, {"loan", LOANS},
            {"hdfc bank limited", LOANS}, {"idfc first bank", LOANS},
            // taxes
            {"incometax", TAXES}, {"income tax", TAXES}, {"eportal", TAXES}, {"gst", TAXES}, {"tds", TAXES},
            // rent & home
            {"rent", RENT}, {"nobroker", RENT}, {"maintenance", RENT}, {"society", RENT},
            {"apartment", RENT}, {"urban company", RENT}, {"urbancompany", RENT}, {"urbanclap", RENT}, {"housing", RENT},
            // entertainment
            {"bookmyshow", ENTERTAINMENT}, {"pvr", ENTERTAINMENT}, {"inox", ENTERTAINMENT},
            {"cinepolis", ENTERTAINMENT}, {"district", ENTERTAINMENT}, {"paytm insider", ENTERTAINMENT},
            {"steam", ENTERTAINMENT}, {"playstation", ENTERTAINMENT},
            // education
            {"school", EDUCATION}, {"college", EDUCATION}, {"university", EDUCATION}, {"bits pilani", EDUCATION},
            {"coursera", EDUCATION}, {"udemy", EDUCATION}, {"byju", EDUCATION}, {"unacademy", EDUCATION},
            {"tuition", EDUCATION}, {"books", EDUCATION},
            {"birla insti", EDUCATION}, {"bits pilani", EDUCATION}, {"institute", EDUCATION},
            // investments (not counted as spending by default)
            {"zerodha", INVESTMENTS}, {"groww", INVESTMENTS}, {"upstox", INVESTMENTS}, {"kuvera", INVESTMENTS},
            {"coin by", INVESTMENTS}, {"indmoney", INVESTMENTS}, {"mutual fund", INVESTMENTS},
            {"bse ltd", INVESTMENTS}, {"nse clearing", INVESTMENTS}, {"iccl", INVESTMENTS},
            {"indian clearing", INVESTMENTS}, {"nps", INVESTMENTS}, {"ppf", INVESTMENTS}, {"sip", INVESTMENTS},
            {"smallcase", INVESTMENTS}, {"paytm money", INVESTMENTS}, {"growwinv", INVESTMENTS}, {"groww inv", INVESTMENTS},
            {"mutual funds", INVESTMENTS}, {"zerodha", INVESTMENTS}, {"angel one", INVESTMENTS},
            // CRED sending credit card money to your own bank account (rent pay / send money)
            {"dreamplug", SELF}, {"dreampur", SELF},
            // credit card bill payments (not counted: the swipes already were)
            {"cred", CARD_BILL}, {"credit card", CARD_BILL}, {"cc payment", CARD_BILL}, {"ccpayment", CARD_BILL},
            {"card bill", CARD_BILL}, {"cardbill", CARD_BILL}, {"sbi card", CARD_BILL}, {"cc bill", CARD_BILL},
            {"billpay cc", CARD_BILL},
    };

    private static final Pattern CARD_BILL_BODY = Pattern.compile(
            "credit card.{0,40}(?:payment|bill|billpay|dues)|(?:payment|bill|billpay|dues).{0,40}credit card|\\bcc (?:payment|bill)",
            Pattern.CASE_INSENSITIVE);

    /** Things about the phone's owner that help spot transfers between their own accounts. */
    public static class Ctx {
        public List<String[]> userRules = java.util.Collections.emptyList();
        public java.util.Set<String> ownAccounts = java.util.Collections.emptySet(); // last 4 digits
        public List<String> myNames = java.util.Collections.emptyList();            // e.g. "RAHUL KUMAR"
        public String sender = "";
    }

    /** Categories that are never counted as spending, whatever the switches say. */
    public static boolean neverCounted(String category) {
        return CARD_BILL.equals(category) || SELF.equals(category) || INCOME.equals(category);
    }

    public static String categorize(ParsedTxn t, String body, List<String[]> userRules) {
        Ctx c = new Ctx();
        if (userRules != null) c.userRules = userRules;
        return categorize(t, body, c);
    }

    public static String categorize(ParsedTxn t, String body, Ctx ctx) {
        if (ParsedTxn.CARD_BILL.equals(t.type)) return CARD_BILL;
        String merchant = t.merchant == null ? "" : t.merchant;

        if (isSelf(t, merchant, ctx)) return SELF;

        String user = bestMatch(merchant, ctx.userRules);
        if (user != null) return user;

        if (ParsedTxn.CREDIT.equals(t.type)) {
            if (ctx.sender != null && ctx.sender.toUpperCase(Locale.ROOT).contains("CREDWL")) return SELF; // wallet top-up
            String b = bestMatch(merchant, DEFAULT_RULES);
            if (INVESTMENTS.equals(b)) return INVESTMENTS;   // redemption: your own money coming back
            if (SELF.equals(b)) return SELF;
            return INCOME;
        }

        if (body != null && CARD_BILL_BODY.matcher(body).find()
                && !Pattern.compile("\\bspent\\b|used at|purchase", Pattern.CASE_INSENSITIVE).matcher(body).find()) {
            return CARD_BILL;
        }

        String builtIn = bestMatch(merchant, DEFAULT_RULES);
        if (builtIn != null) return builtIn;

        if (ParsedTxn.MODE_ATM.equals(t.mode)) return CASH;
        if (merchant.startsWith("PHONE ") || t.toPerson || merchant.matches("X+[0-9]{3,6}")) return PEOPLE;
        return OTHERS;
    }

    private static boolean isSelf(ParsedTxn t, String merchant, Ctx ctx) {
        if (t.selfTransfer) return true;
        if (merchant.isEmpty()) return false;
        String m = normalize(merchant);
        for (String n : ctx.myNames) {
            String k = normalize(n);
            if (!k.isEmpty() && (m.equals(k) || m.startsWith(k + " ") || (" " + m + " ").contains(" " + k + " "))) return true;
        }
        if (merchant.matches("X+[0-9]{3,6}")) {
            String d = merchant.replaceAll("[^0-9]", "");
            String last4 = d.length() > 4 ? d.substring(d.length() - 4) : d;
            return ctx.ownAccounts.contains(last4);
        }
        return false;
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
