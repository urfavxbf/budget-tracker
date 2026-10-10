package com.urfavxbf.budgettracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.time.LocalDate;

public final class PaydayReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "payday_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            PaydayReminderScheduler.schedule(context);
            return;
        }

        String start = intent.getStringExtra(PaydayReminderScheduler.EXTRA_CUTOFF_START);
        String end = intent.getStringExtra(PaydayReminderScheduler.EXTRA_CUTOFF_END);
        String payday = intent.getStringExtra(PaydayReminderScheduler.EXTRA_PAYDAY_DATE);
        if (start == null || end == null || payday == null) {
            PaydayReminderScheduler.schedule(context);
            return;
        }

        WorkDatabase database = new WorkDatabase(context);
        try {
            if (database.getPaydayPayment(start, end) == null) {
                showNotification(context, start, end, payday);
            }
        } finally {
            database.close();
        }
        PaydayReminderScheduler.schedule(context);
    }

    private void showNotification(Context context, String start, String end, String payday) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Payday reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminds you to record salary received on payday.");
            manager.createNotificationChannel(channel);
        }

        Intent openApp = new Intent(context, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(PaydayReminderScheduler.EXTRA_CUTOFF_START, start)
                .putExtra(PaydayReminderScheduler.EXTRA_CUTOFF_END, end);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 7125, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Payday reminder")
                .setContentText("Record the salary you received for " + start + " to " + end + ".")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        "Today is your scheduled payday (" + payday + "). Open Budget Tracker to record your actual salary received."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        try {
            NotificationManagerCompat.from(context).notify(7126, notification.build());
        } catch (SecurityException ignored) {
            // Notifications may be disabled or permission may not have been granted.
        }
    }
}
