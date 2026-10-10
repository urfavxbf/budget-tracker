package com.urfavxbf.budgettracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class WorkDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "budget_tracker.db";
    private static final int DATABASE_VERSION = 5;

    public WorkDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE work_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "work_date TEXT NOT NULL," +
                "time_in TEXT NOT NULL," +
                "time_out TEXT NOT NULL," +
                "breaks TEXT NOT NULL," +
                "shift_minutes INTEGER NOT NULL," +
                "break_minutes INTEGER NOT NULL," +
                "net_minutes INTEGER NOT NULL," +
                "regular_minutes INTEGER NOT NULL," +
                "configured_regular_minutes INTEGER NOT NULL DEFAULT 480," +
                "overtime_minutes INTEGER NOT NULL," +
                "hourly_rate TEXT NOT NULL," +
                "rate_type TEXT NOT NULL DEFAULT 'Hourly'," +
                "entered_rate TEXT NOT NULL DEFAULT '0'," +
                "ot_multiplier TEXT NOT NULL," +
                "allowance TEXT NOT NULL," +
                "deduction TEXT NOT NULL," +
                "gross_pay TEXT NOT NULL," +
                "estimated_net_pay TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX index_work_entries_date ON work_entries(work_date)");
        createExpensesTable(db);
        createPaydayPaymentsTable(db);
    }

    private static void createExpensesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "expense_date TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "note TEXT NOT NULL," +
                "amount TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date ON expenses(expense_date)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createExpensesTable(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE work_entries ADD COLUMN configured_regular_minutes INTEGER NOT NULL DEFAULT 480");
            db.execSQL("UPDATE work_entries SET configured_regular_minutes = regular_minutes WHERE overtime_minutes > 0");
        }
        if (oldVersion < 4) {
            createPaydayPaymentsTable(db);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE work_entries ADD COLUMN rate_type TEXT NOT NULL DEFAULT 'Hourly'");
            db.execSQL("ALTER TABLE work_entries ADD COLUMN entered_rate TEXT NOT NULL DEFAULT '0'");
            db.execSQL("UPDATE work_entries SET entered_rate = hourly_rate WHERE entered_rate = '0'");
        }
    }

    private static void createPaydayPaymentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS payday_payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cutoff_start TEXT NOT NULL," +
                "cutoff_end TEXT NOT NULL," +
                "payday_date TEXT NOT NULL," +
                "expected_amount TEXT NOT NULL," +
                "received_amount TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "UNIQUE(cutoff_start, cutoff_end))");
    }

    public boolean savePaydayPayment(String cutoffStart, String cutoffEnd, String paydayDate,
                                     BigDecimal expectedAmount, BigDecimal receivedAmount) {
        ContentValues values = new ContentValues();
        values.put("cutoff_start", cutoffStart);
        values.put("cutoff_end", cutoffEnd);
        values.put("payday_date", paydayDate);
        values.put("expected_amount", expectedAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("received_amount", receivedAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertWithOnConflict("payday_payments", null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public BigDecimal getPaydayPayment(String cutoffStart, String cutoffEnd) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT received_amount FROM payday_payments WHERE cutoff_start = ? AND cutoff_end = ? LIMIT 1",
                new String[]{cutoffStart, cutoffEnd})) {
            return cursor.moveToFirst() ? new BigDecimal(cursor.getString(0)).setScale(2, java.math.RoundingMode.HALF_UP) : null;
        }
    }

    public List<PaydayPayment> getPaydayPayments(int limit) {
        List<PaydayPayment> payments = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "payday_payments",
                new String[]{"cutoff_start", "cutoff_end", "payday_date", "expected_amount", "received_amount"},
                null, null, null, null, "payday_date DESC, id DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                payments.add(new PaydayPayment(cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            }
        }
        return payments;
    }

    public static final class PaydayPayment {
        public final String cutoffStart, cutoffEnd, paydayDate, expectedAmount, receivedAmount;

        PaydayPayment(String cutoffStart, String cutoffEnd, String paydayDate,
                      String expectedAmount, String receivedAmount) {
            this.cutoffStart = cutoffStart;
            this.cutoffEnd = cutoffEnd;
            this.paydayDate = paydayDate;
            this.expectedAmount = expectedAmount;
            this.receivedAmount = receivedAmount;
        }
    }

    public BigDecimal getTotalReceivedPay() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT received_amount FROM payday_payments", null)) {
            BigDecimal total = BigDecimal.ZERO;
            while (cursor.moveToNext()) total = total.add(new BigDecimal(cursor.getString(0)));
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getAllExpensesTotal() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT amount FROM expenses", null)) {
            BigDecimal total = BigDecimal.ZERO;
            while (cursor.moveToNext()) total = total.add(new BigDecimal(cursor.getString(0)));
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public boolean hasEntryForShift(String date, String timeIn, String timeOut) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM work_entries WHERE work_date = ? AND time_in = ? AND time_out = ? LIMIT 1",
                new String[]{date, timeIn, timeOut})) {
            return cursor.moveToFirst();
        }
    }

    public long insertEntry(String date, String timeIn, String timeOut, String breaks,
                            SalaryCalculator.Result result, BigDecimal hourlyRate, String rateType, BigDecimal enteredRate,
                            BigDecimal overtimeMultiplier, int configuredRegularMinutes) {
        ContentValues values = new ContentValues();
        values.put("work_date", date);
        values.put("time_in", timeIn);
        values.put("time_out", timeOut);
        values.put("breaks", breaks);
        values.put("shift_minutes", result.shiftMinutes);
        values.put("break_minutes", result.breakMinutes);
        values.put("net_minutes", result.netWorkMinutes);
        values.put("regular_minutes", result.regularMinutes);
        values.put("configured_regular_minutes", configuredRegularMinutes);
        values.put("overtime_minutes", result.overtimeMinutes);
        values.put("hourly_rate", hourlyRate.toPlainString());
        values.put("rate_type", "Daily".equals(rateType) ? "Daily" : "Hourly");
        values.put("entered_rate", enteredRate.toPlainString());
        values.put("ot_multiplier", overtimeMultiplier.toPlainString());
        values.put("allowance", result.allowance.toPlainString());
        values.put("deduction", result.deduction.toPlainString());
        values.put("gross_pay", result.grossPay.toPlainString());
        values.put("estimated_net_pay", result.estimatedNetPay.toPlainString());
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("work_entries", null, values);
    }

    public long insertExpense(String date, String category, String note, BigDecimal amount) {
        ContentValues values = new ContentValues();
        values.put("expense_date", date);
        values.put("category", category);
        values.put("note", note == null ? "" : note);
        values.put("amount", amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("expenses", null, values);
    }

    public boolean hasEntryForShiftExceptId(String date, String timeIn, String timeOut, long excludedId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM work_entries WHERE work_date = ? AND time_in = ? AND time_out = ? AND id != ? LIMIT 1",
                new String[]{date, timeIn, timeOut, Long.toString(excludedId)})) {
            return cursor.moveToFirst();
        }
    }

    public boolean updateEntry(long id, String date, String timeIn, String timeOut, String breaks,
                               SalaryCalculator.Result result, BigDecimal hourlyRate, String rateType, BigDecimal enteredRate,
                               BigDecimal overtimeMultiplier, int configuredRegularMinutes) {
        ContentValues values = new ContentValues();
        values.put("work_date", date);
        values.put("time_in", timeIn);
        values.put("time_out", timeOut);
        values.put("breaks", breaks);
        values.put("shift_minutes", result.shiftMinutes);
        values.put("break_minutes", result.breakMinutes);
        values.put("net_minutes", result.netWorkMinutes);
        values.put("regular_minutes", result.regularMinutes);
        values.put("configured_regular_minutes", configuredRegularMinutes);
        values.put("overtime_minutes", result.overtimeMinutes);
        values.put("hourly_rate", hourlyRate.toPlainString());
        values.put("rate_type", "Daily".equals(rateType) ? "Daily" : "Hourly");
        values.put("entered_rate", enteredRate.toPlainString());
        values.put("ot_multiplier", overtimeMultiplier.toPlainString());
        values.put("allowance", result.allowance.toPlainString());
        values.put("deduction", result.deduction.toPlainString());
        values.put("gross_pay", result.grossPay.toPlainString());
        values.put("estimated_net_pay", result.estimatedNetPay.toPlainString());
        return getWritableDatabase().update("work_entries", values, "id = ?",
                new String[]{Long.toString(id)}) == 1;
    }

    public boolean deleteEntry(long id) {
        return getWritableDatabase().delete("work_entries", "id = ?",
                new String[]{Long.toString(id)}) == 1;
    }

    public boolean updateExpense(long id, String date, String category, String note, BigDecimal amount) {
        ContentValues values = new ContentValues();
        values.put("expense_date", date);
        values.put("category", category);
        values.put("note", note == null ? "" : note);
        values.put("amount", amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        return getWritableDatabase().update("expenses", values, "id = ?",
                new String[]{Long.toString(id)}) == 1;
    }

    public boolean deleteExpense(long id) {
        return getWritableDatabase().delete("expenses", "id = ?",
                new String[]{Long.toString(id)}) == 1;
    }

    public List<WorkEntry> getRecentWorkEntries(int limit) {
        List<WorkEntry> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "work_entries",
                new String[]{"id", "work_date", "time_in", "time_out", "breaks", "regular_minutes", "configured_regular_minutes",
                        "break_minutes", "net_minutes", "overtime_minutes", "hourly_rate", "rate_type", "entered_rate", "ot_multiplier",
                        "allowance", "deduction", "estimated_net_pay"},
                null, null, null, null, "work_date DESC, id DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                entries.add(new WorkEntry(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7),
                        cursor.getInt(8), cursor.getInt(9), cursor.getString(10), cursor.getString(11),
                        cursor.getString(12), cursor.getString(13), cursor.getString(14), cursor.getString(15), cursor.getString(16)));
            }
        }
        return entries;
    }

    public List<ExpenseEntry> getRecentExpenseEntries(int limit) {
        List<ExpenseEntry> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "expenses", new String[]{"id", "expense_date", "category", "note", "amount"},
                null, null, null, null, "expense_date DESC, id DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                entries.add(new ExpenseEntry(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4)));
            }
        }
        return entries;
    }

    public static final class WorkEntry {
        public final long id;
        public final String date, timeIn, timeOut, breaks, hourlyRate, rateType, enteredRate, overtimeMultiplier, allowance, deduction, netPay;
        public final int regularMinutes, configuredRegularMinutes, breakMinutes, netMinutes, overtimeMinutes;

        WorkEntry(long id, String date, String timeIn, String timeOut, String breaks, int regularMinutes, int configuredRegularMinutes,
                  int breakMinutes, int netMinutes, int overtimeMinutes, String hourlyRate, String rateType,
                  String enteredRate, String overtimeMultiplier, String allowance, String deduction, String netPay) {
            this.id = id;
            this.date = date;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
            this.breaks = breaks;
            this.regularMinutes = regularMinutes;
            this.configuredRegularMinutes = configuredRegularMinutes;
            this.breakMinutes = breakMinutes;
            this.netMinutes = netMinutes;
            this.overtimeMinutes = overtimeMinutes;
            this.hourlyRate = hourlyRate;
            this.rateType = rateType;
            this.enteredRate = enteredRate;
            this.overtimeMultiplier = overtimeMultiplier;
            this.allowance = allowance;
            this.deduction = deduction;
            this.netPay = netPay;
        }
    }

    public static final class ExpenseEntry {
        public final long id;
        public final String date, category, note, amount;

        ExpenseEntry(long id, String date, String category, String note, String amount) {
            this.id = id;
            this.date = date;
            this.category = category;
            this.note = note;
            this.amount = amount;
        }
    }

    public BigDecimal getRecordedNetPayTotal(String startDate, String endDate) {
        return getEstimatedNetPayTotal(startDate, endDate);
    }

    public BigDecimal getEstimatedNetPayTotal(String startDate, String endDate) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT estimated_net_pay FROM work_entries WHERE work_date BETWEEN ? AND ?",
                new String[]{startDate, endDate})) {
            BigDecimal total = BigDecimal.ZERO;
            while (cursor.moveToNext()) {
                total = total.add(new BigDecimal(cursor.getString(0)));
            }
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getExpenseTotal(String startDate, String endDate) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT amount FROM expenses WHERE expense_date BETWEEN ? AND ?",
                new String[]{startDate, endDate})) {
            BigDecimal total = BigDecimal.ZERO;
            while (cursor.moveToNext()) {
                total = total.add(new BigDecimal(cursor.getString(0)));
            }
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public List<String> getDistinctExpenseCategories() {
        List<String> categories = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT DISTINCT TRIM(category) FROM expenses WHERE TRIM(category) != '' ORDER BY TRIM(category) COLLATE NOCASE",
                null)) {
            while (cursor.moveToNext()) {
                String category = cursor.getString(0);
                if (category != null && !category.trim().isEmpty()) {
                    categories.add(category.trim());
                }
            }
        }
        return categories;
    }

    public Map<String, BigDecimal> getExpenseTotalsByCategory(String startDate, String endDate) {
        Map<String, BigDecimal> totals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category, amount FROM expenses WHERE expense_date BETWEEN ? AND ?",
                new String[]{startDate, endDate})) {
            while (cursor.moveToNext()) {
                String category = cursor.getString(0).trim();
                if (category.isEmpty()) category = "Uncategorized";
                BigDecimal amount = new BigDecimal(cursor.getString(1));
                BigDecimal current = totals.get(category);
                totals.put(category, (current == null ? BigDecimal.ZERO : current).add(amount));
            }
        }

        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(totals.entrySet());
        Collections.sort(entries, (left, right) -> {
            int byAmount = right.getValue().compareTo(left.getValue());
            return byAmount != 0 ? byAmount : left.getKey().compareToIgnoreCase(right.getKey());
        });
        Map<String, BigDecimal> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue().setScale(2, java.math.RoundingMode.HALF_UP));
        }
        return sorted;
    }

    public List<String> getRecentExpenses(int limit) {
        List<String> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "expenses", new String[]{"expense_date", "category", "note", "amount"},
                null, null, null, null, "expense_date DESC, id DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                entries.add(String.format(Locale.getDefault(), "%s • %s • ₱%s%s",
                        cursor.getString(0), cursor.getString(1), cursor.getString(3),
                        cursor.getString(2).isEmpty() ? "" : "\n" + cursor.getString(2)));
            }
        }
        return entries;
    }

    public List<String> getRecentEntries(int limit) {
        List<String> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "work_entries",
                new String[]{"work_date", "time_in", "time_out", "break_minutes",
                        "net_minutes", "overtime_minutes", "estimated_net_pay"},
                null, null, null, null, "work_date DESC, id DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                String date = cursor.getString(0);
                String timeIn = cursor.getString(1);
                String timeOut = cursor.getString(2);
                int breakMinutes = cursor.getInt(3);
                int netMinutes = cursor.getInt(4);
                int overtimeMinutes = cursor.getInt(5);
                String pay = cursor.getString(6);
                entries.add(String.format(Locale.getDefault(),
                        "%s  •  %s–%s\nWork %s  •  Break %s  •  OT %s\nRecorded net pay: ₱%s",
                        date, timeIn, timeOut, formatMinutes(netMinutes),
                        formatMinutes(breakMinutes), formatMinutes(overtimeMinutes), pay));
            }
        }
        return entries;
    }

    private static String formatMinutes(int minutes) {
        return String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60);
    }
}
