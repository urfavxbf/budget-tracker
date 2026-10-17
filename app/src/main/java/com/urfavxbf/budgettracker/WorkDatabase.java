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
    private static final int DATABASE_VERSION = 11;
    private final Context context;

    public WorkDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
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
        createBudgetCycleAllocationsTable(db);
        createExpenseSavingsDrawsTable(db);
        createCategoryBudgetLimitsTable(db);
        createCashBalanceAdjustmentsTable(db);
        createSavingsGoalsTable(db);
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
        if (oldVersion < 6) {
            createBudgetCycleAllocationsTable(db);
        }
        if (oldVersion == 6) {
            db.execSQL("ALTER TABLE budget_cycle_allocations ADD COLUMN needs_review INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 8) createExpenseSavingsDrawsTable(db);
        if (oldVersion < 9) createCategoryBudgetLimitsTable(db);
        if (oldVersion < 10) createCashBalanceAdjustmentsTable(db);
        if (oldVersion < 11) createSavingsGoalsTable(db);
    }

    private static void createCategoryBudgetLimitsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS category_budget_limits (" +
                "category TEXT PRIMARY KEY COLLATE NOCASE," +
                "limit_amount TEXT NOT NULL," +
                "updated_at INTEGER NOT NULL)");
    }

    /** Returns saved category spending limits keyed by category name. */
    public Map<String, BigDecimal> getCategoryBudgetLimits() {
        Map<String, BigDecimal> limits = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT category, limit_amount FROM category_budget_limits ORDER BY category COLLATE NOCASE", null)) {
            while (cursor.moveToNext()) {
                try {
                    limits.put(cursor.getString(0), new BigDecimal(cursor.getString(1))
                            .setScale(2, java.math.RoundingMode.HALF_UP));
                } catch (NumberFormatException ignored) {
                    // Skip a malformed limit without hiding other valid saved categories.
                }
            }
        }
        return limits;
    }

    public boolean saveCategoryBudgetLimit(String category, BigDecimal limit) {
        if (category == null || category.trim().isEmpty() || limit == null || limit.signum() < 0) return false;
        ContentValues values = new ContentValues();
        values.put("category", category.trim());
        values.put("limit_amount", limit.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insertWithOnConflict("category_budget_limits", null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public boolean deleteCategoryBudgetLimit(String category) {
        if (category == null || category.trim().isEmpty()) return false;
        return getWritableDatabase().delete("category_budget_limits", "category = ?",
                new String[]{category.trim()}) > 0;
    }

    private static void createExpenseSavingsDrawsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS expense_savings_draws (" +
                "expense_id INTEGER PRIMARY KEY," +
                "amount TEXT NOT NULL," +
                "FOREIGN KEY(expense_id) REFERENCES expenses(id) ON DELETE CASCADE)");
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

    private static void createBudgetCycleAllocationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS budget_cycle_allocations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "cutoff_start TEXT NOT NULL," +
                "cutoff_end TEXT NOT NULL," +
                "savings_amount TEXT NOT NULL," +
                "carryover_amount TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "needs_review INTEGER NOT NULL DEFAULT 0," +
                "UNIQUE(cutoff_start, cutoff_end))");
    }

    private static void invalidateAllocationForExpenseDate(SQLiteDatabase db, String expenseDate) {
        if (expenseDate == null || expenseDate.isEmpty()) return;
        db.execSQL("UPDATE budget_cycle_allocations SET needs_review = 1 " +
                "WHERE EXISTS (SELECT 1 FROM payday_payments p " +
                        "WHERE p.cutoff_start = budget_cycle_allocations.cutoff_start " +
                        "AND p.cutoff_end = budget_cycle_allocations.cutoff_end " +
                        "AND p.payday_date <= ? " +
                        "AND NOT EXISTS (SELECT 1 FROM payday_payments q " +
                        "WHERE q.payday_date > p.payday_date AND q.payday_date <= ?))",
                new Object[]{expenseDate, expenseDate});
    }

    private static void invalidateAllocationsForPaydayChange(SQLiteDatabase db,
                                                               String cutoffStart, String cutoffEnd,
                                                               String affectedDate) {
        if (affectedDate == null || affectedDate.isEmpty()) return;
        db.execSQL("UPDATE budget_cycle_allocations SET needs_review = 1 " +
                        "WHERE EXISTS (SELECT 1 FROM payday_payments p " +
                        "WHERE p.cutoff_start = budget_cycle_allocations.cutoff_start " +
                        "AND p.cutoff_end = budget_cycle_allocations.cutoff_end " +
                        "AND ( (p.cutoff_start = ? AND p.cutoff_end = ?) " +
                        "OR p.payday_date = (SELECT MAX(q.payday_date) FROM payday_payments q " +
                        "WHERE q.payday_date < ?) " +
                        "OR p.payday_date = (SELECT MIN(r.payday_date) FROM payday_payments r " +
                        "WHERE r.payday_date > ?) ))",
                new Object[]{cutoffStart, cutoffEnd, affectedDate, affectedDate});
    }

    private static void invalidateNextAllocationAfterPayday(SQLiteDatabase db, String paydayDate) {
        if (paydayDate == null || paydayDate.isEmpty()) return;
        db.execSQL("UPDATE budget_cycle_allocations SET needs_review = 1 " +
                        "WHERE EXISTS (SELECT 1 FROM payday_payments p " +
                        "WHERE p.cutoff_start = budget_cycle_allocations.cutoff_start " +
                        "AND p.cutoff_end = budget_cycle_allocations.cutoff_end " +
                        "AND p.payday_date = (SELECT MIN(q.payday_date) FROM payday_payments q " +
                        "WHERE q.payday_date > ?))",
                new Object[]{paydayDate});
    }

    public boolean saveBudgetCycleAllocation(String cutoffStart, String cutoffEnd,
                                             BigDecimal savingsAmount, BigDecimal carryoverAmount) {
        ContentValues values = new ContentValues();
        values.put("cutoff_start", cutoffStart);
        values.put("cutoff_end", cutoffEnd);
        values.put("savings_amount", savingsAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("carryover_amount", carryoverAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("created_at", System.currentTimeMillis());
        values.put("needs_review", 0);
        return getWritableDatabase().insertWithOnConflict("budget_cycle_allocations", null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public BudgetCycleAllocation getBudgetCycleAllocation(String cutoffStart, String cutoffEnd) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT savings_amount, carryover_amount FROM budget_cycle_allocations " +
                        "WHERE cutoff_start = ? AND cutoff_end = ? LIMIT 1",
                new String[]{cutoffStart, cutoffEnd})) {
            if (!cursor.moveToFirst()) return null;
            return new BudgetCycleAllocation(cursor.getString(0), cursor.getString(1));
        }
    }

    public BigDecimal getTotalSavingsBalance() {
        String openingSavings = context.getSharedPreferences("salary_preferences", Context.MODE_PRIVATE)
                .getString("opening_savings_balance", "0.00");
        BigDecimal total;
        try {
            total = new BigDecimal(openingSavings);
        } catch (NumberFormatException ignored) {
            total = BigDecimal.ZERO;
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT savings_amount FROM budget_cycle_allocations", null)) {
            while (cursor.moveToNext()) total = total.add(new BigDecimal(cursor.getString(0)));
            try (Cursor draws = getReadableDatabase().rawQuery("SELECT amount FROM expense_savings_draws", null)) {
                while (draws.moveToNext()) total = total.subtract(new BigDecimal(draws.getString(0)));
            }
            return total.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public PaydayPayment getLatestPaydayPaymentOnOrBefore(String date) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT cutoff_start, cutoff_end, payday_date, expected_amount, received_amount " +
                        "FROM payday_payments WHERE payday_date <= ? ORDER BY payday_date DESC, id DESC LIMIT 1",
                new String[]{date})) {
            if (!cursor.moveToFirst()) return null;
            return new PaydayPayment(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4));
        }
    }

    public PaydayPayment getPreviousPaydayPayment(String paydayDate, String excludedCutoffStart,
                                                   String excludedCutoffEnd) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT cutoff_start, cutoff_end, payday_date, expected_amount, received_amount " +
                        "FROM payday_payments WHERE payday_date < ? " +
                        "AND NOT (cutoff_start = ? AND cutoff_end = ?) " +
                        "ORDER BY payday_date DESC, id DESC LIMIT 1",
                new String[]{paydayDate, excludedCutoffStart, excludedCutoffEnd})) {
            if (!cursor.moveToFirst()) return null;
            return new PaydayPayment(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4));
        }
    }

    public BudgetCyclePending getPendingBudgetCycleAllocation() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT p.cutoff_start, p.cutoff_end, p.payday_date, p.expected_amount, p.received_amount, " +
                        "(SELECT q.payday_date FROM payday_payments q WHERE q.payday_date > p.payday_date " +
                        "ORDER BY q.payday_date ASC, q.id ASC LIMIT 1) " +
                        "FROM payday_payments p " +
                        "WHERE EXISTS (SELECT 1 FROM payday_payments q WHERE q.payday_date > p.payday_date) " +
                        "AND NOT EXISTS (SELECT 1 FROM budget_cycle_allocations a " +
                        "WHERE a.cutoff_start = p.cutoff_start AND a.cutoff_end = p.cutoff_end " +
                        "AND a.needs_review = 0) " +
                        "ORDER BY p.payday_date ASC, p.id ASC LIMIT 1", null)) {
            if (!cursor.moveToFirst()) return null;
            PaydayPayment payment = new PaydayPayment(cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4));
            return new BudgetCyclePending(payment, cursor.getString(5));
        }
    }

    public BudgetCyclePending getPendingBudgetCycleAllocation(String throughDateInclusive) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT p.cutoff_start, p.cutoff_end, p.payday_date, p.expected_amount, p.received_amount, " +
                        "(SELECT q.payday_date FROM payday_payments q " +
                        "WHERE q.payday_date > p.payday_date AND q.payday_date <= ? " +
                        "ORDER BY q.payday_date ASC, q.id ASC LIMIT 1) " +
                        "FROM payday_payments p " +
                        "WHERE EXISTS (SELECT 1 FROM payday_payments q " +
                        "WHERE q.payday_date > p.payday_date AND q.payday_date <= ?) " +
                        "AND NOT EXISTS (SELECT 1 FROM budget_cycle_allocations a " +
                        "WHERE a.cutoff_start = p.cutoff_start AND a.cutoff_end = p.cutoff_end " +
                        "AND a.needs_review = 0) " +
                        "ORDER BY p.payday_date ASC, p.id ASC LIMIT 1",
                new String[]{throughDateInclusive, throughDateInclusive})) {
            if (!cursor.moveToFirst()) return null;
            PaydayPayment payment = new PaydayPayment(cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4));
            return new BudgetCyclePending(payment, cursor.getString(5));
        }
    }


    private static void createCashBalanceAdjustmentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS cash_balance_adjustments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "adjustment_date TEXT NOT NULL," +
                "adjustment_amount TEXT NOT NULL," +
                "target_available_balance TEXT NOT NULL," +
                "note TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_balance_adjustments_date " +
                "ON cash_balance_adjustments(adjustment_date, id)");
    }

    /** Stores the signed difference needed to reconcile calculated available cash to the user's actual cash. */
    public boolean addCashBalanceAdjustment(String date, BigDecimal adjustmentAmount,
                                            BigDecimal targetAvailableBalance, String note) {
        if (date == null || date.trim().isEmpty() || adjustmentAmount == null
                || targetAvailableBalance == null || targetAvailableBalance.signum() < 0) return false;
        ContentValues values = new ContentValues();
        values.put("adjustment_date", date);
        values.put("adjustment_amount", adjustmentAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("target_available_balance", targetAvailableBalance.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("note", note == null ? "" : note.trim());
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("cash_balance_adjustments", null, values) != -1;
    }

    public BigDecimal getCashBalanceAdjustmentTotalOnOrBefore(String date) {
        BigDecimal total = BigDecimal.ZERO;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT adjustment_amount FROM cash_balance_adjustments WHERE adjustment_date <= ?",
                new String[]{date})) {
            while (cursor.moveToNext()) total = total.add(new BigDecimal(cursor.getString(0)));
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public List<CashBalanceAdjustment> getRecentCashBalanceAdjustments(int limit) {
        List<CashBalanceAdjustment> adjustments = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, adjustment_date, adjustment_amount, target_available_balance, note " +
                        "FROM cash_balance_adjustments ORDER BY adjustment_date DESC, id DESC LIMIT ?",
                new String[]{Integer.toString(Math.max(1, limit))})) {
            while (cursor.moveToNext()) {
                adjustments.add(new CashBalanceAdjustment(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            }
        }
        return adjustments;
    }

    public static final class CashBalanceAdjustment {
        public final long id;
        public final String date, amount, targetAvailableBalance, note;

        CashBalanceAdjustment(long id, String date, String amount, String targetAvailableBalance, String note) {
            this.id = id;
            this.date = date;
            this.amount = amount;
            this.targetAvailableBalance = targetAvailableBalance;
            this.note = note;
        }
    }

    public BigDecimal getExpenseTotalBetweenPaydays(String startInclusive, String endExclusive) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT amount FROM expenses WHERE expense_date >= ? AND expense_date < ?",
                new String[]{startInclusive, endExclusive})) {
            BigDecimal total = BigDecimal.ZERO;
            while (cursor.moveToNext()) total = total.add(new BigDecimal(cursor.getString(0)));
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public static final class BudgetCycleAllocation {
        public final String savingsAmount, carryoverAmount;

        BudgetCycleAllocation(String savingsAmount, String carryoverAmount) {
            this.savingsAmount = savingsAmount;
            this.carryoverAmount = carryoverAmount;
        }
    }

    public static final class BudgetCyclePending {
        public final PaydayPayment payment;
        public final String nextPaydayDate;

        BudgetCyclePending(PaydayPayment payment, String nextPaydayDate) {
            this.payment = payment;
            this.nextPaydayDate = nextPaydayDate;
        }
    }

    public boolean hasPaydayDateConflict(String paydayDate, String excludedCutoffStart,
                                         String excludedCutoffEnd) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM payday_payments WHERE payday_date = ? " +
                        "AND NOT (cutoff_start = ? AND cutoff_end = ?) LIMIT 1",
                new String[]{paydayDate, excludedCutoffStart, excludedCutoffEnd})) {
            return cursor.moveToFirst();
        }
    }

    public boolean savePaydayPayment(String cutoffStart, String cutoffEnd, String paydayDate,
                                     BigDecimal expectedAmount, BigDecimal receivedAmount) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Expense records are date-only, so two salary cycles cannot be split reliably
            // when their paydays share the same date. Reject ambiguity before replacing a row.
            try (Cursor cursor = db.rawQuery(
                    "SELECT 1 FROM payday_payments WHERE payday_date = ? " +
                            "AND NOT (cutoff_start = ? AND cutoff_end = ?) LIMIT 1",
                    new String[]{paydayDate, cutoffStart, cutoffEnd})) {
                if (cursor.moveToFirst()) return false;
            }
            String oldDate = null;
            String oldReceived = null;
            try (Cursor cursor = db.rawQuery(
                    "SELECT payday_date, received_amount FROM payday_payments WHERE cutoff_start = ? AND cutoff_end = ? LIMIT 1",
                    new String[]{cutoffStart, cutoffEnd})) {
                if (cursor.moveToFirst()) {
                    oldDate = cursor.getString(0);
                    oldReceived = cursor.getString(1);
                }
            }
            boolean paydayDateChanged = oldDate != null && !oldDate.equals(paydayDate);
            boolean receivedAmountChanged = oldReceived != null
                    && new BigDecimal(oldReceived).compareTo(receivedAmount) != 0;
            if (oldDate != null && (paydayDateChanged || receivedAmountChanged)) {
                db.execSQL("UPDATE budget_cycle_allocations SET needs_review = 1 " +
                                "WHERE cutoff_start = ? AND cutoff_end = ?",
                        new Object[]{cutoffStart, cutoffEnd});
            }
            ContentValues values = new ContentValues();
            values.put("cutoff_start", cutoffStart);
            values.put("cutoff_end", cutoffEnd);
            values.put("payday_date", paydayDate);
            values.put("expected_amount", expectedAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            values.put("received_amount", receivedAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            values.put("created_at", System.currentTimeMillis());
            long saved = db.insertWithOnConflict("payday_payments", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (saved == -1) return false;
            if (paydayDateChanged) {
                invalidateAllocationsForPaydayChange(db, cutoffStart, cutoffEnd, oldDate);
                invalidateAllocationsForPaydayChange(db, cutoffStart, cutoffEnd, paydayDate);
            } else if (receivedAmountChanged) {
                invalidateNextAllocationAfterPayday(db, paydayDate);
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }
    public boolean deletePaydayPayment(String cutoffStart, String cutoffEnd) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String deletedPaydayDate = null;
            try (Cursor cursor = db.rawQuery(
                    "SELECT payday_date FROM payday_payments " +
                            "WHERE cutoff_start = ? AND cutoff_end = ? LIMIT 1",
                    new String[]{cutoffStart, cutoffEnd})) {
                if (cursor.moveToFirst()) deletedPaydayDate = cursor.getString(0);
            }
            if (deletedPaydayDate != null) {
                invalidateAllocationsForPaydayChange(db, cutoffStart, cutoffEnd, deletedPaydayDate);
            }
            // A deleted salary must not silently erase money already reserved as savings.
            // Convert any carryover from this cycle into savings before removing the payment,
            // so the reserved amount remains accounted for and cannot be spent twice.
            try (Cursor cursor = db.rawQuery(
                    "SELECT savings_amount, carryover_amount FROM budget_cycle_allocations " +
                            "WHERE cutoff_start = ? AND cutoff_end = ? LIMIT 1",
                    new String[]{cutoffStart, cutoffEnd})) {
                if (cursor.moveToFirst()) {
                    BigDecimal preservedSavings = new BigDecimal(cursor.getString(0))
                            .add(new BigDecimal(cursor.getString(1)))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    ContentValues allocation = new ContentValues();
                    allocation.put("savings_amount", preservedSavings.toPlainString());
                    allocation.put("carryover_amount", "0.00");
                    allocation.put("needs_review", 0);
                    db.update("budget_cycle_allocations", allocation,
                            "cutoff_start = ? AND cutoff_end = ?",
                            new String[]{cutoffStart, cutoffEnd});
                }
            }
            int deleted = db.delete("payday_payments", "cutoff_start = ? AND cutoff_end = ?",
                    new String[]{cutoffStart, cutoffEnd});
            db.setTransactionSuccessful();
            return deleted > 0;
        } finally {
            db.endTransaction();
        }
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

    public BigDecimal getTotalReceivedPayOnOrBefore(String dateInclusive) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT received_amount FROM payday_payments WHERE payday_date <= ?",
                new String[]{dateInclusive})) {
            BigDecimal total = BigDecimal.ZERO;
            while (cursor.moveToNext()) total = total.add(new BigDecimal(cursor.getString(0)));
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getAllExpensesTotalOnOrBefore(String dateInclusive) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT amount FROM expenses WHERE expense_date <= ?",
                new String[]{dateInclusive})) {
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
        return insertExpense(date, category, note, amount, BigDecimal.ZERO);
    }

    public long insertExpense(String date, String category, String note, BigDecimal amount, BigDecimal savingsDraw) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("expense_date", date);
            values.put("category", category);
            values.put("note", note == null ? "" : note);
            values.put("amount", amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            values.put("created_at", System.currentTimeMillis());
            long id = db.insertOrThrow("expenses", null, values);
            saveExpenseSavingsDraw(db, id, savingsDraw);
            invalidateAllocationForExpenseDate(db, date);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    private static void saveExpenseSavingsDraw(SQLiteDatabase db, long expenseId, BigDecimal amount) {
        BigDecimal normalized = (amount == null ? BigDecimal.ZERO : amount).max(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        if (normalized.signum() == 0) {
            db.delete("expense_savings_draws", "expense_id = ?", new String[]{Long.toString(expenseId)});
            return;
        }
        ContentValues values = new ContentValues();
        values.put("expense_id", expenseId);
        values.put("amount", normalized.toPlainString());
        db.insertWithOnConflict("expense_savings_draws", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public BigDecimal getExpenseSavingsDraw(long expenseId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT amount FROM expense_savings_draws WHERE expense_id = ? LIMIT 1",
                new String[]{Long.toString(expenseId)})) {
            return cursor.moveToFirst() ? new BigDecimal(cursor.getString(0)) : BigDecimal.ZERO.setScale(2);
        }
    }

    public BigDecimal getExpenseAmount(long expenseId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT amount FROM expenses WHERE id = ? LIMIT 1",
                new String[]{Long.toString(expenseId)})) {
            return cursor.moveToFirst() ? new BigDecimal(cursor.getString(0)) : BigDecimal.ZERO.setScale(2);
        }
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
        return updateExpense(id, date, category, note, amount, getExpenseSavingsDraw(id));
    }

    public boolean updateExpense(long id, String date, String category, String note, BigDecimal amount, BigDecimal savingsDraw) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String oldDate = null;
            try (Cursor cursor = db.rawQuery(
                    "SELECT expense_date FROM expenses WHERE id = ? LIMIT 1",
                    new String[]{Long.toString(id)})) {
                if (cursor.moveToFirst()) oldDate = cursor.getString(0);
            }
            ContentValues values = new ContentValues();
            values.put("expense_date", date);
            values.put("category", category);
            values.put("note", note == null ? "" : note);
            values.put("amount", amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            int updated = db.update("expenses", values, "id = ?", new String[]{Long.toString(id)});
            if (updated == 1) {
                saveExpenseSavingsDraw(db, id, savingsDraw);
                invalidateAllocationForExpenseDate(db, oldDate);
                if (!date.equals(oldDate)) invalidateAllocationForExpenseDate(db, date);
            }
            db.setTransactionSuccessful();
            return updated == 1;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteExpense(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String oldDate = null;
            try (Cursor cursor = db.rawQuery(
                    "SELECT expense_date FROM expenses WHERE id = ? LIMIT 1",
                    new String[]{Long.toString(id)})) {
                if (cursor.moveToFirst()) oldDate = cursor.getString(0);
            }
            int deleted = db.delete("expenses", "id = ?", new String[]{Long.toString(id)});
            if (deleted == 1) {
                db.delete("expense_savings_draws", "expense_id = ?", new String[]{Long.toString(id)});
                invalidateAllocationForExpenseDate(db, oldDate);
            }
            db.setTransactionSuccessful();
            return deleted == 1;
        } finally {
            db.endTransaction();
        }
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


    private static void createSavingsGoalsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS savings_goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "target_amount TEXT NOT NULL," +
                "saved_amount TEXT NOT NULL DEFAULT '0.00'," +
                "target_date TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL)");
    }

    /** Adds a savings goal without changing any existing cash or savings records. */
    public long addSavingsGoal(String name, BigDecimal targetAmount, String targetDate) {
        if (name == null || name.trim().isEmpty() || targetAmount == null || targetAmount.signum() <= 0) return -1;
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("target_amount", targetAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("saved_amount", "0.00");
        values.put("target_date", targetDate == null ? "" : targetDate.trim());
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("savings_goals", null, values);
    }

    public List<SavingsGoal> getSavingsGoals() {
        List<SavingsGoal> goals = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, name, target_amount, saved_amount, target_date FROM savings_goals ORDER BY created_at DESC, id DESC", null)) {
            while (cursor.moveToNext()) {
                try {
                    goals.add(new SavingsGoal(cursor.getLong(0), cursor.getString(1),
                            new BigDecimal(cursor.getString(2)), new BigDecimal(cursor.getString(3)),
                            cursor.getString(4)));
                } catch (NumberFormatException ignored) {
                    // Skip only malformed goal rows; preserve the rest of the list.
                }
            }
        }
        return goals;
    }

    public boolean addToSavingsGoal(long goalId, BigDecimal amount) {
        if (goalId <= 0 || amount == null || amount.signum() <= 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String current = null;
            try (Cursor cursor = db.rawQuery("SELECT saved_amount FROM savings_goals WHERE id = ?",
                    new String[]{Long.toString(goalId)})) {
                if (cursor.moveToFirst()) current = cursor.getString(0);
            }
            if (current == null) return false;
            BigDecimal next = new BigDecimal(current).add(amount).setScale(2, java.math.RoundingMode.HALF_UP);
            ContentValues update = new ContentValues();
            update.put("saved_amount", next.toPlainString());
            boolean success = db.update("savings_goals", update, "id = ?", new String[]{Long.toString(goalId)}) > 0;
            if (success) db.setTransactionSuccessful();
            return success;
        } catch (NumberFormatException ignored) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean updateSavingsGoal(long goalId, String name, BigDecimal targetAmount, String targetDate) {
        if (goalId <= 0 || name == null || name.trim().isEmpty()
                || targetAmount == null || targetAmount.signum() <= 0) return false;
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("target_amount", targetAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        values.put("target_date", targetDate == null ? "" : targetDate.trim());
        return getWritableDatabase().update("savings_goals", values, "id = ?",
                new String[]{Long.toString(goalId)}) > 0;
    }

    public boolean deleteSavingsGoal(long goalId) {
        return goalId > 0 && getWritableDatabase().delete("savings_goals", "id = ?",
                new String[]{Long.toString(goalId)}) > 0;
    }

    public static final class SavingsGoal {
        public final long id;
        public final String name;
        public final BigDecimal targetAmount;
        public final BigDecimal savedAmount;
        public final String targetDate;

        SavingsGoal(long id, String name, BigDecimal targetAmount, BigDecimal savedAmount, String targetDate) {
            this.id = id;
            this.name = name;
            this.targetAmount = targetAmount;
            this.savedAmount = savedAmount;
            this.targetDate = targetDate == null ? "" : targetDate;
        }
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