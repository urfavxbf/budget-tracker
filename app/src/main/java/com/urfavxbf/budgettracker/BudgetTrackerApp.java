package com.urfavxbf.budgettracker;

import android.app.Application;
import android.os.Build;
import android.os.Process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BudgetTrackerApp extends Application {
    public static final String CRASH_PREFS = "runtime_debug";
    public static final String CRASH_REPORT = "last_crash_report";
    private Thread.UncaughtExceptionHandler previousHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter writer = new StringWriter();
                PrintWriter printer = new PrintWriter(writer);
                throwable.printStackTrace(printer);
                printer.flush();

                String report = "Budget Tracker runtime crash\n"
                        + "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()) + "\n"
                        + "Package: " + getPackageName() + "\n"
                        + "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
                        + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                        + "Thread: " + thread.getName() + "\n\n"
                        + writer;

                getSharedPreferences(CRASH_PREFS, MODE_PRIVATE)
                        .edit()
                        .putString(CRASH_REPORT, report)
                        .commit();
            } catch (Throwable ignored) {
                // Preserve the original crash even if report persistence fails.
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        });
    }
}
