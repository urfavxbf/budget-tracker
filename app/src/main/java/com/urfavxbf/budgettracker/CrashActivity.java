package com.urfavxbf.budgettracker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
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

        int background = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurface, Color.rgb(18, 20, 28));
        int surface = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceVariant, Color.rgb(30, 33, 43));
        int foreground = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface, Color.rgb(240, 242, 248));
        int muted = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant, Color.rgb(180, 187, 202));
        int accent = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorError, Color.rgb(255, 94, 94));

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

        MaterialButton copyButton = new MaterialButton(this);
        copyButton.setText("Copy Error Report");
        copyButton.setAllCaps(false);
        copyButton.setTextSize(14);
        copyButton.setCornerRadius(dp(16));
        copyButton.setInsetTop(0);
        copyButton.setInsetBottom(0);
        copyButton.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnError, Color.WHITE));
        copyButton.setBackgroundTintList(ColorStateList.valueOf(accent));
        copyButton.setRippleColor(ColorStateList.valueOf(0x33FFFFFF));
        copyButton.setOnClickListener(v -> copyReport());
        root.addView(copyButton, new LinearLayout.LayoutParams(-1, dp(52)));

        MaterialButton restartButton = new MaterialButton(this);
        restartButton.setText("Try Open App Again");
        restartButton.setAllCaps(false);
        restartButton.setCornerRadius(dp(16));
        restartButton.setInsetTop(0);
        restartButton.setInsetBottom(0);
        restartButton.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSecondaryContainer, surface)));
        restartButton.setTextColor(MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSecondaryContainer, foreground));
        restartButton.setRippleColor(ColorStateList.valueOf(0x335267D8));
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
        log.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable logBackground = new GradientDrawable();
        logBackground.setColor(surface);
        logBackground.setCornerRadius(dp(16));
        log.setBackground(logBackground);
        scroll.setClipToPadding(false);
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
