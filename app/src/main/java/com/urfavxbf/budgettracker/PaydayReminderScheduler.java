package com.urfavxbf.budgettracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PaydayReminderScheduler {
    static final String ACTION_PAYDAY_REMINDER = "com.urfavxbf.budgettracker.ACTION_PAYDAY_REMINDER";
    static final String EXTRA_CUTOFF_START = "cutoff_start";
    static final String EXTRA_CUTOFF_END = "cutoff_end";
    static final String EXTRA_PAYDAY_DATE = "payday_date";

    private static final String PREFS = "salary_preferences";
    private static final String DELIVERY_DATE_KEY = "payday_reminder_delivery_date";
    private static final String DELIVERED_CUTOFFS_KEY = "payday_reminder_delivered_cutoffs";

    private PaydayReminderScheduler() {}

    public static void schedule(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        LocalDate today = LocalDate.now();
        int firstPayday = readDay(app, "first_cutoff_payday", 22);
        int secondPayday = readDay(app, "second_cutoff_payday", 7);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTrigger = null;
        CutoffPeriod nextCutoff = null;
        LocalDate nextPayday = null;

        for (int monthOffset = -1; monthOffset <= 14; monthOffset++) {
            LocalDate month = today.withDayOfMonth(1).plusMonths(monthOffset);
            CutoffPeriod[] cutoffs = {
                    CutoffPeriod.forDate(month.withDayOfMonth(1)),
                    CutoffPeriod.forDate(month.withDayOfMonth(16))
            };
            for (CutoffPeriod cutoff : cutoffs) {
                LocalDate payday = cutoff.payday(firstPayday, secondPayday);
                LocalDateTime trigger = payday.atTime(9, 0);
                boolean delivered = wasDelivered(app, payday, cutoff);
                if (!PaydayReminderPolicy.shouldSchedule(payday, today, trigger, now, delivered)) {
                    continue;
                }
                if (trigger.isBefore(now)) {
                    // A missed alarm may be delivered once today; never rearm it after delivery.
                    trigger = now.plusSeconds(5);
                }
                if (nextTrigger == null || trigger.isBefore(nextTrigger)) {
                    nextTrigger = trigger;
                    nextCutoff = cutoff;
                    nextPayday = payday;
                }
            }
        }

        if (nextTrigger == null || nextCutoff == null || nextPayday == null) return;
        Intent intent = new Intent(app, PaydayReminderReceiver.class)
                .setAction(ACTION_PAYDAY_REMINDER)
                .putExtra(EXTRA_CUTOFF_START, nextCutoff.startDate.toString())
                .putExtra(EXTRA_CUTOFF_END, nextCutoff.endDate.toString())
                .putExtra(EXTRA_PAYDAY_DATE, nextPayday.toString());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(app, 7124, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerMillis = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent);
        }
    }

    static void markDelivered(Context context, String cutoffStart, String cutoffEnd, String payday) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = cutoffKey(cutoffStart, cutoffEnd);
        String savedDate = preferences.getString(DELIVERY_DATE_KEY, "");
        String savedCutoffs = payday.equals(savedDate)
                ? preferences.getString(DELIVERED_CUTOFFS_KEY, "") : "";

        Set<String> delivered = new LinkedHashSet<>();
        if (!savedCutoffs.isEmpty()) delivered.addAll(Arrays.asList(savedCutoffs.split(",")));
        delivered.add(key);

        StringBuilder serialized = new StringBuilder();
        for (String deliveredKey : delivered) {
            if (serialized.length() > 0) serialized.append(',');
            serialized.append(deliveredKey);
        }
        preferences.edit()
                .putString(DELIVERY_DATE_KEY, payday)
                .putString(DELIVERED_CUTOFFS_KEY, serialized.toString())
                .apply();
    }

    private static boolean wasDelivered(Context context, LocalDate payday, CutoffPeriod cutoff) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!payday.toString().equals(preferences.getString(DELIVERY_DATE_KEY, ""))) return false;
        String delivered = preferences.getString(DELIVERED_CUTOFFS_KEY, "");
        return Arrays.asList(delivered.split(",")).contains(cutoffKey(
                cutoff.startDate.toString(), cutoff.endDate.toString()));
    }

    private static String cutoffKey(String start, String end) {
        return start + "_" + end;
    }

    private static int readDay(Context context, String key, int fallback) {
        try {
            int day = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getInt(key, fallback);
            return Math.max(1, Math.min(31, day));
        } catch (ClassCastException ignored) {
            try {
                String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString(key, Integer.toString(fallback));
                return Math.max(1, Math.min(31, Integer.parseInt(raw)));
            } catch (RuntimeException ignoredAgain) {
                return fallback;
            }
        }
    }
}
