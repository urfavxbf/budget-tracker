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
    private static final int DATABASE_VERSION = 1;

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Database migrations must preserve existing user data.
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
                        "%s  •  %s–%s\nWork %s  •  Break %s  •  OT %s\nEstimated net pay: ₱%s",
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
