package com.urfavxbf.budgettracker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class CrashActivity extends Activity {
    private String report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        report = getSharedPreferences(BudgetTrackerApp.CRASH_PREFS, MODE_PRIVATE)
                .getString(BudgetTrackerApp.CRASH_REPORT, "No crash report was saved.");

        int background = Color.rgb(18, 20, 28);
        int surface = Color.rgb(30, 33, 43);
        int foreground = Color.rgb(240, 242, 248);
        int muted = Color.rgb(180, 187, 202);
        int accent = Color.rgb(255, 94, 94);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(background);

        TextView title = new TextView(this);
        title.setText("App Runtime Error");
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(accent);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Nag-crash ang Budget Tracker. Kopyahin ang report sa ibaba at ipadala ito para matukoy ang eksaktong sanhi.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(muted);
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        Button copyButton = new Button(this);
        copyButton.setText("Copy Error Report");
        copyButton.setAllCaps(false);
        copyButton.setTextSize(15);
        copyButton.setTextColor(Color.WHITE);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(accent);
        buttonBackground.setCornerRadius(dp(14));
        copyButton.setBackground(buttonBackground);
        copyButton.setOnClickListener(v -> copyReport());
        root.addView(copyButton, new LinearLayout.LayoutParams(-1, dp(52)));

        Button restartButton = new Button(this);
        restartButton.setText("Try Open App Again");
        restartButton.setAllCaps(false);
        restartButton.setOnClickListener(v -> {
            getSharedPreferences(BudgetTrackerApp.CRASH_PREFS, MODE_PRIVATE)
                    .edit().remove(BudgetTrackerApp.CRASH_REPORT).apply();
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        root.addView(restartButton, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView logTitle = new TextView(this);
        logTitle.setText("CRASH LOG");
        logTitle.setTextColor(muted);
        logTitle.setTextSize(12);
        logTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logTitle.setPadding(0, dp(16), 0, dp(8));
        root.addView(logTitle);

        ScrollView scroll = new ScrollView(this);
        TextView log = new TextView(this);
        log.setText(report);
        log.setTextIsSelectable(true);
        log.setTextSize(12);
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextColor(foreground);
        log.setPadding(dp(12), dp(12), dp(12), dp(12));
        log.setBackgroundColor(surface);
        scroll.addView(log, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
    }

    private void copyReport() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Clipboard is unavailable.", Toast.LENGTH_LONG).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Budget Tracker crash report", report));
        Toast.makeText(this, "Crash report copied. Ipadala rito ang kinopyang text.", Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
