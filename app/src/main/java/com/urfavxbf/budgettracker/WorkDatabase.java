package com.urfavxbf.budgettracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorkDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "budget_tracker.db";
    private static final int DATABASE_VERSION = 2;

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
                "overtime_minutes INTEGER NOT NULL," +
                "hourly_rate TEXT NOT NULL," +
                "ot_multiplier TEXT NOT NULL," +
                "allowance TEXT NOT NULL," +
                "deduction TEXT NOT NULL," +
                "gross_pay TEXT NOT NULL," +
                "estimated_net_pay TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX index_work_entries_date ON work_entries(work_date)");
        createExpensesTable(db);
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
    }

    public boolean hasEntryForShift(String date, String timeIn, String timeOut) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM work_entries WHERE work_date = ? AND time_in = ? AND time_out = ? LIMIT 1",
                new String[]{date, timeIn, timeOut})) {
            return cursor.moveToFirst();
        }
    }

    public long insertEntry(String date, String timeIn, String timeOut, String breaks,
                            SalaryCalculator.Result result, BigDecimal hourlyRate,
                            BigDecimal overtimeMultiplier) {
        ContentValues values = new ContentValues();
        values.put("work_date", date);
        values.put("time_in", timeIn);
        values.put("time_out", timeOut);
        values.put("breaks", breaks);
        values.put("shift_minutes", result.shiftMinutes);
        values.put("break_minutes", result.breakMinutes);
        values.put("net_minutes", result.netWorkMinutes);
        values.put("regular_minutes", result.regularMinutes);
        values.put("overtime_minutes", result.overtimeMinutes);
        values.put("hourly_rate", hourlyRate.toPlainString());
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
                               SalaryCalculator.Result result, BigDecimal hourlyRate,
                               BigDecimal overtimeMultiplier) {
        ContentValues values = new ContentValues();
        values.put("work_date", date);
        values.put("time_in", timeIn);
        values.put("time_out", timeOut);
        values.put("breaks", breaks);
        values.put("shift_minutes", result.shiftMinutes);
        values.put("break_minutes", result.breakMinutes);
        values.put("net_minutes", result.netWorkMinutes);
        values.put("regular_minutes", result.regularMinutes);
        values.put("overtime_minutes", result.overtimeMinutes);
        values.put("hourly_rate", hourlyRate.toPlainString());
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
                new String[]{"id", "work_date", "time_in", "time_out", "breaks", "regular_minutes",
                        "break_minutes", "net_minutes", "overtime_minutes", "hourly_rate", "ot_multiplier",
                        "allowance", "deduction", "estimated_net_pay"},
                null, null, null, null, "work_date DESC, id DESC",
                Integer.toString(Math.max(1, limit)))) {
            while (cursor.moveToNext()) {
                entries.add(new WorkEntry(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getInt(6),
                        cursor.getInt(7), cursor.getInt(8), cursor.getString(9), cursor.getString(10),
                        cursor.getString(11), cursor.getString(12), cursor.getString(13)));
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
        public final String date, timeIn, timeOut, breaks, hourlyRate, overtimeMultiplier, allowance, deduction, netPay;
        public final int regularMinutes, breakMinutes, netMinutes, overtimeMinutes;

        WorkEntry(long id, String date, String timeIn, String timeOut, String breaks, int regularMinutes,
                  int breakMinutes, int netMinutes, int overtimeMinutes, String hourlyRate,
                  String overtimeMultiplier, String allowance, String deduction, String netPay) {
            this.id = id;
            this.date = date;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
            this.breaks = breaks;
            this.regularMinutes = regularMinutes;
            this.breakMinutes = breakMinutes;
            this.netMinutes = netMinutes;
            this.overtimeMinutes = overtimeMinutes;
            this.hourlyRate = hourlyRate;
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
