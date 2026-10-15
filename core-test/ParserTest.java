import com.somen.hisab.core.*;
import java.util.*;

public class ParserTest {
    static int fail = 0;
    static void check(String sender, String body, String type, Double amt, String merchant, String cat) {
        ParsedTxn t = SmsParser.parse(sender, body);
        String got = t == null ? "null" : t.type + "|" + t.amount + "|" + t.merchant + "|" + Categorizer.categorize(t, body, (java.util.List<String[]>) null) + "|" + t.mode + "|" + t.account;
        boolean ok;
        if (type == null) ok = t == null;
        else ok = t != null && t.type.equals(type) && Math.abs(t.amount - amt) < 0.001
                && (merchant == null || t.merchant.equals(merchant))
                && (cat == null || Categorizer.categorize(t, body, (java.util.List<String[]>) null).equals(cat));
        if (!ok) fail++;
        System.out.println((ok ? "ok   " : "FAIL ") + got + "   <= " + body.substring(0, Math.min(70, body.length())));
    }
    public static void main(String[] a) {
        String D = ParsedTxn.DEBIT, C = ParsedTxn.CREDIT, B = ParsedTxn.CARD_BILL;
        check("VM-SBIUPI", "Dear UPI user A/C X1234 debited by 250.0 on date 12Oct26 trf to SWIGGY Refno 628512345678. If not u? call 1800111109. -SBI", D, 250.0, "SWIGGY", "Food & dining");
        check("AX-HDFCBK", "Sent Rs.1,499.00\nFrom HDFC Bank A/C *1234\nTo ZEPTO MARKETPLACE PRIVATE LIMITED\nOn 12/10/26\nRef 628598765432\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808", D, 1499.0, null, "Groceries");
        check("JD-ICICIT", "ICICI Bank Acct XX123 debited for Rs 349.00 on 12-Oct-26; Zomato Ltd credited. UPI:628512345678. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.", D, 349.0, "ZOMATO LTD", "Food & dining");
        check("AD-ICICIT", "INR 2,450.00 spent using ICICI Bank Card XX4321 on 12-Oct-26 on AMAZON PAY IN E COM. Avl Limit: INR 1,45,320.50. If not you, call 1800 2662/SMS BLOCK 4321 to 9215676766", D, 2450.0, null, "Shopping");
        check("VM-HDFCBK", "Spent Rs.890 On HDFC Bank Card 5678 At UBER INDIA SYSTEMS On 2026-10-12:21:14:05.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 5678 to 7308080808", D, 890.0, "UBER INDIA SYSTEMS", "Transport & fuel");
        check("BZ-SBICRD", "Rs.1,200.00 spent on your SBI Credit Card ending 9876 at NETFLIX COM on 11/10/26. Trxn. not done by you? Report at https://sbicard.com/Dispute", D, 1200.0, "NETFLIX COM", "Subscriptions");
        check("AX-HDFCBK", "Payment of Rs 15,000.00 has been received on your HDFC Bank Credit Card ending 5678 on 10-OCT-26. Thank you.", B, 15000.0, null, "Credit card bill");
        check("JM-ICICIT", "Dear Customer, Payment of INR 22,500.00 has been received towards your ICICI Bank Credit Card Account XX4321 on 05-Oct-26. Thank you.", B, 22500.0, null, "Credit card bill");
        check("VM-SBIUPI", "Dear UPI user A/C X1234 debited by 15000.0 on date 05Oct26 trf to CRED Club Refno 628511112222. If not u? call 1800111109. -SBI", D, 15000.0, null, "Credit card bill");
        check("AX-HDFCBK", "Your OTP for transaction of INR 2,450.00 at AMAZON on HDFC Bank Card XX5678 is 482913. Valid for 5 mins. Do not share OTP with anyone.", null, null, null, null);
        check("VK-HDFCBK", "Update! INR 1,25,000.00 deposited in HDFC Bank A/c XX1234 on 01-OCT-26 for NEFT Cr-CITI0000001-INFINEON TECHNOLOGIES INDIA PVT LTD.Avl bal INR 1,80,000.00. Cheque deposits in A/C are subject to clearing", C, 125000.0, null, "Income & refunds");
        check("AD-KOTAKB", "Sent Rs.120.00 from Kotak Bank AC X5555 to 9876543210@ybl on 12-10-26.UPI Ref 628533334444. Not you, https://kotak.com/KBANKT/Fraud", D, 120.0, "PHONE ••3210", "Sent to people");
        check("AX-AXISBK", "INR 600.00 debited\nA/c no. XX7788\n12-10-26, 18:22:10\nUPI/P2M/628599990000/BLINKIT\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank", D, 600.0, "BLINKIT", "Groceries");
        check("VM-PNBSMS", "Your a/c no. XXXXXXXX1234 is debited for Rs.2000.00 on 12-10-2026 and credited to a/c no. XXXXXXXX9876 (UPI Ref no 628500001111)", D, 2000.0, null, null);
        check("AX-HDFCBK", "Rs.5000 withdrawn at ATM HDFC BANK MG ROAD from A/c XX1234 on 12-10-26. Avl bal Rs.42,000", D, 5000.0, null, "Cash withdrawal");
        check("AX-HDFCBK", "Your HDFC Bank Credit Card XX5678 statement has been generated. Total amount due Rs 32,450. Minimum amount due Rs 1,630. Due date 01-Nov-26.", null, null, null, null);
        check("TM-PHONPE", "Rahul has requested money of Rs 500 from you on PhonePe. Pay now.", null, null, null, null);
        check("AX-HDFCBK", "Pre-approved personal loan of Rs 5,00,000 is waiting for you! Apply now.", null, null, null, null);
        check("VM-SBIUPI", "Your UPI transaction of Rs 300 to swiggy@icici has failed. Amount if debited will be refunded.", null, null, null, null);
        check("+919812345678", "Sent Rs.500 to you bro", null, null, null, null);
        check("AX-ICICIT", "Dear Customer, Rs.199.00 has been debited from your account XX123 towards NETFLIX on 12-Oct-26 via e-mandate. Info: UPI Mandate", D, 199.0, null, "Subscriptions");
        check("BV-AMAZON", "Refund of Rs 899 for your order has been credited to your Amazon Pay ICICI Credit Card.", C, 899.0, null, "Income & refunds");
        check("AX-HDFCBK", "Rs.450.00 debited from HDFC Bank Debit Card XX9911 at DMART AVENUE SUPERMARTS on 12-10-26", D, 450.0, null, "Groceries");
        check("JD-SBIINB", "Dear Customer, INR 25,000.00 credited to your A/c No XX1234 on 15/10/2026 through IMPS by RAHUL KUMAR. Avl bal INR 60,000.00 -SBI", C, 25000.0, "RAHUL KUMAR", null);
        check("AD-HDFCBK", "Txn Rs.799.00\nOn HDFC Bank Card 5678\nAt bookmyshow\nby UPI 628512312312\nOn 12-10\nNot You?\nCall 18002586161", D, 799.0, "BOOKMYSHOW", "Entertainment");
        // ---- formats seen in the owner's real SMS (numbers changed) ----
        check("AX-AXISBK-S", "Spent\nINR 450.00\nAxis Bank Card no. XX1111\n08-10-26 20:02:48 IST\nSwiggy Limi\nAvl Limit: INR 4,21,157.32\nNot you? SMS BLOCK 1111 to 919999999999", D, 450.0, "SWIGGY LIMI", "Food & dining");
        check("AD-AXISBK-S", "INR 300.00 debited\nA/c no. XX2222\n28-09-26, 08:22:22\nUPI/P2M/088274757482/SRI LAKSHMI RAMANA\nNot you? SMS BLOCKUPI Cust ID to 919999999999\nAxis Bank", D, 300.0, "SRI LAKSHMI RAMANA", null);
        check("AD-AXISBK-S", "INR 60000.00 debited\nA/c no. XX2222\n27-01-26, 09:25:02\nUPI/P2A/832794669782/SUMEN  DAS\nNot you? SMS BLOCKUPI Cust ID to 919999999999\nAxis Bank", D, 60000.0, "SUMEN DAS", null);
        check("AD-AXISBK-S", "INR 150000.00 debited\nA/c no. XX2222\n12-03-26, 16:51:50\nUPI/P2M/643718648612/MUTUAL FUNDS ICCL\nNot you? SMS BLOCKUPI Cust ID to 919999999999\nAxis Bank", D, 150000.0, "MUTUAL FUNDS ICCL", "Investments");
        check("JD-YESBNK-S", "SGD 12.50 spent on your Card 5XXXXXXXX1 on 01-05-26 Your SGD wallet debited by 12.50 Available Bal is SGD 100", null, null, null, null);
        check("VK-AXISBK-S", "INR 149.00 for Google Play will be auto-debited via Axis Bank Card no. XX1111 by 08-10-26. Please ensure sufficient limit", null, null, null, null);
        check("AX-AXISBK-S", "Your NEFT txn with ref. no. AXOMB123 for INR 20000 is credited to beneficiary Sadhan Das on 01-09-26 at 10:00:00 IST - Axis Bank", D, 20000.0, "SADHAN DAS", "Sent to people");
        check("AD-AIRBIL-S", "Hi Somen Das, we have received a payment of Rs. 1178 for your Airtel Wi-Fi ID 123_dsl. To download the payment receipt, click https://x", null, null, null, null);
        check("AD-INDUSB-S", "Service Alert: Manage this purchase better and split your INR 11321 spend at NARAYANA HEALTH INS Bengaluru into EMIs.", null, null, null, null);
        check("JM-KOTAKB-S", "INR 40,604.00 is debited from your Account XXXXXX3333 on 07/10/2026 towards NACH-10-HDFC BANK LIMITED Kotak Bank", D, 40604.0, null, "Loan EMI");
        check("AD-AXISBK-S", "INR 160000.00 debited from A/c no. XX444444 on GARAI GOLD  14-05-2026 16:23:05 IST. Avl bal: INR 139292.00.", D, 160000.0, "GARAI GOLD", "Shopping");
        System.out.println(fail == 0 ? "\nALL PASSED" : "\nFAILURES: " + fail);
    }
}
