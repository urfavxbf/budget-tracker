package com.urfavxbf.budgettracker;

import androidx.fragment.app.FragmentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class MainActivity extends FragmentActivity {
    private static final String PREFS = "salary_preferences";
    private static final String[] TAB_NAMES = {"Home", "Activity", "Budget", "Settings"};
    private final List<BreakInput> breakInputs = new ArrayList<>();
    private LinearLayout page;
    private BottomNavigationView bottomNav;
    private static final int NAV_HOME_ID = 1;
    private static final int NAV_HISTORY_ID = 2;
    private static final int NAV_SETTINGS_ID = 3;
    private static final int NAV_BUDGET_ID = 4;
    private int currentTab = 0;
    private EditText dateInput, timeInInput, timeOutInput;
    private EditText totalHoursInput;
    private CheckBox hoursOnlyMode;
    private EditText hourlyRateInput, regularHoursInput, overtimeMultiplierInput, allowanceInput, deductionInput;
    private EditText firstPaydayInput, secondPaydayInput;
    private MaterialAutoCompleteTextView payRateTypeInput;
    private EditText expenseDateInput, expenseAmountInput, expenseNoteInput;
    private MaterialAutoCompleteTextView expenseCategoryInput;
    private LinearLayout breakContainer, historyContainer, expenseHistoryContainer;
    private TextView resultView, budgetSummaryView;
    private EditText spendingBudgetInput;
    private EditText openingBalanceInput;
    private EditText openingSavingsInput;
    private ProgressBar budgetProgressBar;
    private WorkDatabase database;
    private long editingWorkEntryId = -1;
    private long editingExpenseId = -1;
    private Button saveWorkButton;
    private Button saveExpenseButton;
    private boolean lastSaveSucceeded;
    private boolean paydayDialogShowing;
    private boolean updatingNavigation;
    private View openSwipeContent;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSharedPreferences(BudgetTrackerApp.CRASH_PREFS, MODE_PRIVATE)
                .contains(BudgetTrackerApp.CRASH_REPORT)) {
            startActivity(new android.content.Intent(this, CrashActivity.class));
            finish();
            return;
        }
        database = new WorkDatabase(this);
        getWindow().setStatusBarColor(resolveColor(com.google.android.material.R.attr.colorSurface));
        getWindow().setNavigationBarColor(resolveColor(com.google.android.material.R.attr.colorSurface));
        buildShell();
        applyImmersiveMode();
        if (isFirstTimeSetupRequired()) {
            bottomNav.setVisibility(View.GONE);
            showFirstTimeSetup();
        } else {
            showTab(currentTab);
            requestNotificationPermissionIfNeeded();
        }
        PaydayReminderScheduler.schedule(this);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private void applyImmersiveMode() {
        android.view.Window window = getWindow();
        if (window == null) return;

        android.view.View decorView = window.getDecorView();
        if (decorView == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }

    private void buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface));

        // Android 15 enforces edge-to-edge for apps targeting SDK 35. Keep the app shell
        // inside the visible system-bar insets so the header and bottom navigation aren't obscured.
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            ViewCompat.setOnApplyWindowInsetsListener(shell, (view, windowInsets) -> {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return windowInsets;
            });
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        bottomNav = new BottomNavigationView(this);
        bottomNav.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface));
        bottomNav.setElevation(dp(4));
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setItemActiveIndicatorEnabled(true);
        bottomNav.setItemActiveIndicatorColor(ColorStateList.valueOf(
                resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)));
        bottomNav.setItemRippleColor(ColorStateList.valueOf(
                withAlpha(resolveColor(androidx.appcompat.R.attr.colorPrimary), 0x33)));
        shell.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(80)));
        setContentView(shell);
        buildBottomNav();
    }

    private void buildBottomNav() {
        bottomNav.getMenu().clear();
        bottomNav.getMenu().add(0, NAV_HOME_ID, 0, "Home").setIcon(R.drawable.ic_home);
        bottomNav.getMenu().add(0, NAV_HISTORY_ID, 1, "Activity").setIcon(R.drawable.ic_history);
        bottomNav.getMenu().add(0, NAV_BUDGET_ID, 2, "Budget").setIcon(R.drawable.ic_budget);
        bottomNav.getMenu().add(0, NAV_SETTINGS_ID, 3, "Settings").setIcon(R.drawable.ic_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            if (updatingNavigation) return true;
            int tab;
            if (item.getItemId() == NAV_HOME_ID) {
                tab = 0;
            } else if (item.getItemId() == NAV_HISTORY_ID) {
                tab = 1;
            } else if (item.getItemId() == NAV_BUDGET_ID) {
                tab = 2;
            } else if (item.getItemId() == NAV_SETTINGS_ID) {
                tab = 3;
            } else {
                return false;
            }
            if (tab != currentTab) {
                editingWorkEntryId = -1;
                editingExpenseId = -1;
            }
            currentTab = tab;
            showTab(tab);
            return true;
        });
        bottomNav.setSelectedItemId(currentTab == 0 ? NAV_HOME_ID
                : currentTab == 1 ? NAV_HISTORY_ID
                : currentTab == 2 ? NAV_BUDGET_ID : NAV_SETTINGS_ID);
    }

    private void showTab(int tab) {
        if (tab < 0 || tab > 3) tab = 0;
        currentTab = tab;
        if (bottomNav != null) {
            int selectedId = tab == 0 ? NAV_HOME_ID
                    : tab == 1 ? NAV_HISTORY_ID
                    : tab == 2 ? NAV_BUDGET_ID : NAV_SETTINGS_ID;
            if (bottomNav.getSelectedItemId() != selectedId) {
                updatingNavigation = true;
                bottomNav.setSelectedItemId(selectedId);
                updatingNavigation = false;
            }
        }
        page.removeAllViews();
        breakInputs.clear();
        dateInput = timeInInput = timeOutInput = null;
        totalHoursInput = null;
        hoursOnlyMode = null;
        hourlyRateInput = regularHoursInput = overtimeMultiplierInput = allowanceInput = deductionInput = null;
        expenseDateInput = null;
        expenseCategoryInput = null;
        expenseAmountInput = expenseNoteInput = null;
        breakContainer = historyContainer = expenseHistoryContainer = null;
        resultView = budgetSummaryView = null;
        spendingBudgetInput = null;
        openingBalanceInput = null;
        openingSavingsInput = null;
        budgetProgressBar = null;
        saveWorkButton = null;
        saveExpenseButton = null;

        switch (tab) {
            case 0: buildDashboard(); break;
            case 1: buildHistoryScreen(); break;
            case 2: buildBudgetScreen(); break;
            case 3: buildSettingsScreen(); break;
            default: buildDashboard();
        }
    }

    private boolean isFirstTimeSetupRequired() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean("onboarding_complete", false)) return false;

        // Do not show onboarding to existing users upgrading the app.
        boolean hasSavedPreferences = prefs.contains("pay_rate")
                || prefs.contains("opening_cash_balance")
                || prefs.contains("spending_budget")
                || prefs.contains("first_cutoff_payday");
        boolean hasSavedActivity = !database.getRecentWorkEntries(1).isEmpty()
                || !database.getRecentExpenseEntries(1).isEmpty()
                || !database.getPaydayPayments(1).isEmpty();
        if (hasSavedPreferences || hasSavedActivity) {
            prefs.edit().putBoolean("onboarding_complete", true).apply();
            return false;
        }
        return true;
    }

    private void requestNotificationPermissionIfNeeded() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("notifications_enabled", false)) return;
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 7127);
        }
    }

    private void showFirstTimeSetup() {
        page.removeAllViews();
        header("Welcome to Budget Tracker", "Let's set up your pay, payday, cash, and savings. You can change these later in Settings.");
        TextView note = text("Currency: Philippine peso (₱). Enter your actual details. The app's estimated salary is based on your saved work shifts; it is not a confirmed payment.", 14, false);
        note.setPadding(0, 0, 0, dp(12));
        page.addView(note);

        LinearLayout card = cardContainer();
        page.addView(card);
        addSection(card, "Salary defaults");
        payRateTypeInput = createRateTypeSpinner();
        hourlyRateInput = field("Rate amount (₱)", pref("pay_rate", ""),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        regularHoursInput = field("Regular hours per day", pref("regular_hours", "8"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        overtimeMultiplierInput = field("Overtime multiplier", pref("ot_multiplier", "1.25"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        allowanceInput = field("Daily allowance", pref("allowance", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        deductionInput = field("Daily deduction", pref("deduction", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(card, "Pay rate type", payRateTypeInput);
        addField(card, "Rate amount (₱)", hourlyRateInput);
        addField(card, "Regular hours per day", regularHoursInput);
        addField(card, "Overtime multiplier", overtimeMultiplierInput);
        addField(card, "Daily allowance (₱, optional)", allowanceInput);
        addField(card, "Daily deduction (₱, optional)", deductionInput);

        addSection(card, "Payday schedule");
        firstPaydayInput = field("Day 1–31", pref("first_cutoff_payday", "22"), InputType.TYPE_CLASS_NUMBER);
        secondPaydayInput = field("Day 1–31", pref("second_cutoff_payday", "7"), InputType.TYPE_CLASS_NUMBER);
        addField(card, "Payday for 1st–15th cutoff (same month)", firstPaydayInput);
        addField(card, "Payday for 16th–month-end cutoff (following month)", secondPaydayInput);

        addSection(card, "Starting balances and budget");
        openingBalanceInput = field("0.00", pref("opening_cash_balance", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        openingSavingsInput = field("0.00", pref("opening_savings_balance", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        spendingBudgetInput = field("0.00", pref("spending_budget", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(card, "Opening cash balance (₱)", openingBalanceInput);
        addField(card, "Opening savings balance (₱)", openingSavingsInput);
        addField(card, "Spending limit per salary cycle (₱, 0 to disable)", spendingBudgetInput);
        TextView balancesNote = text("Optional balances may be 0. Opening cash is your total tracked cash before subtracting the amount reserved as savings. Opening savings cannot exceed opening cash. The spending limit is a guide, not a bank balance.", 12, false);
        balancesNote.setPadding(0, 0, 0, dp(8));
        card.addView(balancesNote);

        CheckBox notifications = new CheckBox(this);
        notifications.setText("Enable payday reminder notifications");
        notifications.setChecked(getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("notifications_enabled", false));
        card.addView(notifications);

        Button finish = button("Save setup and continue", true);
        finish.setOnClickListener(v -> completeFirstTimeSetup(notifications));
        card.addView(finish);
    }

    private void completeFirstTimeSetup(CheckBox notifications) {
        try {
            BigDecimal rate = decimal(hourlyRateInput, "Pay rate");
            String rateType = payRateTypeInput == null ? "Hourly" : payRateTypeInput.getText().toString();
            if (!"Hourly".equals(rateType) && !"Daily".equals(rateType)) {
                throw new IllegalArgumentException("Choose Hourly or Daily for pay rate type.");
            }
            BigDecimal hours = decimal(regularHoursInput, "Regular hours");
            BigDecimal multiplier = decimal(overtimeMultiplierInput, "OT multiplier");
            BigDecimal allowance = decimal(allowanceInput, "Allowance");
            BigDecimal deduction = decimal(deductionInput, "Deduction");
            BigDecimal openingCash = decimal(openingBalanceInput, "Starting cash balance");
            BigDecimal openingSavings = decimal(openingSavingsInput, "Starting savings balance");
            BigDecimal budget = decimal(spendingBudgetInput, "Spending limit");
            int firstPayday = Integer.parseInt(value(firstPaydayInput));
            int secondPayday = Integer.parseInt(value(secondPaydayInput));

            if (rate.signum() <= 0) throw new IllegalArgumentException("Pay rate must be greater than zero.");
            if (hours.signum() <= 0 || hours.multiply(BigDecimal.valueOf(60)).stripTrailingZeros().scale() > 0)
                throw new IllegalArgumentException("Regular hours must be positive and convert to whole minutes.");
            if (multiplier.signum() <= 0 || allowance.signum() < 0 || deduction.signum() < 0)
                throw new IllegalArgumentException("OT multiplier must be positive; allowance and deduction cannot be negative.");
            if (firstPayday < 1 || firstPayday > 31 || secondPayday < 1 || secondPayday > 31)
                throw new IllegalArgumentException("Payday must be a day from 1 to 31.");
            if (openingCash.signum() < 0 || openingSavings.signum() < 0 || budget.signum() < 0)
                throw new IllegalArgumentException("Starting balances and spending limit cannot be negative.");
            if (openingSavings.compareTo(openingCash) > 0)
                throw new IllegalArgumentException("Opening savings cannot exceed your opening cash balance.");

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Confirm your setup")
                    .setMessage("Pay rate: " + money(rate) + " (" + rateType + ")"
                            + "\nPaydays: " + firstPayday + " and " + secondPayday
                            + "\nOpening cash: " + money(openingCash)
                            + "\nOpening savings: " + money(openingSavings)
                            + "\nSpending limit: " + (budget.signum() == 0 ? "Disabled" : money(budget))
                            + "\n\nYou can edit these details later in Settings.")
                    .setNegativeButton("Go back", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm and finish", (dialog, which) -> {
                        saveSalaryPreferences(rate, rateType, hours, multiplier, allowance, deduction);
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString("first_cutoff_payday", Integer.toString(firstPayday))
                                .putString("second_cutoff_payday", Integer.toString(secondPayday))
                                .putString("opening_cash_balance", openingCash.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .putString("opening_savings_balance", openingSavings.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .putString("spending_budget", budget.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .putBoolean("notifications_enabled", notifications.isChecked())
                                .putBoolean("onboarding_complete", true)
                                .apply();
                        bottomNav.setVisibility(View.VISIBLE);
                        currentTab = 0;
                        showTab(0);
                        bottomNav.setSelectedItemId(NAV_HOME_ID);
                        PaydayReminderScheduler.schedule(this);
                        requestNotificationPermissionIfNeeded();
                        Toast.makeText(this, "Setup complete. Welcome!", Toast.LENGTH_LONG).show();
                    }).show();
        } catch (NumberFormatException ex) {
            toast("Enter valid payday days from 1 to 31.");
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    private void buildDashboard() {
        LocalDate today = LocalDate.now();
        int firstPayday = parsePaydayPreference("first_cutoff_payday", 22);
        int secondPayday = parsePaydayPreference("second_cutoff_payday", 7);
        CutoffPeriod currentPaydayCutoff = CutoffPeriod.forLatestPaydayOnOrBefore(
                today, firstPayday, secondPayday);
        LocalDate cycleStart = currentPaydayCutoff.payday(firstPayday, secondPayday);
        LocalDate payday = CutoffPeriod.forNextPayday(today, firstPayday, secondPayday)
                .payday(firstPayday, secondPayday);
        WorkDatabase.PaydayPayment latestPayment = database.getLatestPaydayPaymentOnOrBefore(today.toString());
        WorkDatabase.PaydayPayment activePayment = latestPayment != null
                && !LocalDate.parse(latestPayment.paydayDate).isBefore(cycleStart) ? latestPayment : null;
        BigDecimal earned = activePayment == null
                ? database.getRecordedNetPayTotal(currentPaydayCutoff.startDate.toString(),
                        currentPaydayCutoff.endDate.toString())
                : new BigDecimal(activePayment.receivedAmount);
        BigDecimal spent = database.getExpenseTotal(cycleStart.toString(), today.toString());
        BigDecimal availableCash = getAvailableCashBalance();
        CutoffPeriod upcomingPayCutoff = getLatestPaydayCutoff(today);
        LocalDate upcomingPayday = getScheduledPayday(upcomingPayCutoff);
        BigDecimal expectedUpcomingSalary = database.getRecordedNetPayTotal(
                upcomingPayCutoff.startDate.toString(), upcomingPayCutoff.endDate.toString());
        long daysLeft = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, payday));

        final int ink = resolveColor(com.google.android.material.R.attr.colorOnSurface);
        final int muted = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        final int accent = resolveColor(androidx.appcompat.R.attr.colorPrimary);
        final int green = resolveColor(androidx.appcompat.R.attr.colorPrimary);
        final int navy = 0xFF1A2647;
        page.setPadding(dp(20), dp(24), dp(20), dp(24));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, dp(18));
        LinearLayout greeting = new LinearLayout(this);
        greeting.setOrientation(LinearLayout.VERTICAL);
        TextView hello = text("Good day", 12, false);
        hello.setTextColor(muted);
        greeting.addView(hello);
        TextView title = text("My finances", 25, true);
        title.setTextColor(ink);
        greeting.addView(title);
        top.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView avatar = text("₱", 19, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(accent);
        avatar.setBackground(roundedBackground(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant), dp(21)));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));
        page.addView(top);

        LinearLayout balance = cardContainer();
        balance.setOrientation(LinearLayout.VERTICAL);
        balance.setPadding(dp(20), dp(18), dp(20), dp(18));
        balance.setBackground(roundedBackground(navy, dp(22)));
        LinearLayout balanceHeader = new LinearLayout(this);
        balanceHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView balanceLabel = text("AVAILABLE CASH", 11, true);
        balanceLabel.setTextColor(0xFFB8C7E3);
        balanceHeader.addView(balanceLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialButton editCashButton = new MaterialButton(this);
        editCashButton.setText("Edit");
        editCashButton.setAllCaps(false);
        editCashButton.setTextSize(12);
        editCashButton.setTypeface(editCashButton.getTypeface(), Typeface.BOLD);
        editCashButton.setIconResource(R.drawable.ic_edit);
        editCashButton.setIconTint(ColorStateList.valueOf(0xFFFFFFFF));
        editCashButton.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        editCashButton.setIconPadding(dp(5));
        editCashButton.setTextColor(0xFFFFFFFF);
        editCashButton.setBackgroundTintList(ColorStateList.valueOf(0xFF2B3B5E));
        editCashButton.setCornerRadius(dp(10));
        editCashButton.setInsetTop(0);
        editCashButton.setInsetBottom(0);
        editCashButton.setMinWidth(0);
        editCashButton.setMinimumWidth(0);
        editCashButton.setMinHeight(dp(36));
        editCashButton.setMinimumHeight(dp(36));
        editCashButton.setPadding(dp(10), 0, dp(10), 0);
        editCashButton.setElevation(0);
        editCashButton.setOnClickListener(v -> showReconcileCashDialog(getAvailableCashBalance()));
        LinearLayout.LayoutParams editCashParams = new LinearLayout.LayoutParams(-2, dp(36));
        editCashParams.rightMargin = dp(8);
        balanceHeader.addView(editCashButton, editCashParams);
        TextView paydayLabel = text("PAYDAY  ·  " + payday.format(DateTimeFormatter.ofPattern("MMM d")), 10, true);
        paydayLabel.setTextColor(0xFFFFFFFF);
        paydayLabel.setPadding(dp(9), dp(6), dp(9), dp(6));
        paydayLabel.setBackground(roundedBackground(0xFF2B3B5E, dp(14)));
        balanceHeader.addView(paydayLabel);
        balance.addView(balanceHeader);
        TextView balanceValue = text(money(availableCash), 30, true);
        balanceValue.setTextColor(0xFFFFFFFF);
        balanceValue.setPadding(0, dp(10), 0, dp(5));
        balance.addView(balanceValue);
        TextView balanceTrend = text((availableCash.signum() >= 0 ? "↗  " : "↘  ")
                + (daysLeft == 0 ? "Payday is here" : daysLeft + (daysLeft == 1 ? " day" : " days") + " until payday"), 12, true);
        balanceTrend.setTextColor(0xFF87E8B8);
        balance.addView(balanceTrend);
        View balanceGap = space(dp(14));
        balance.addView(balanceGap, new LinearLayout.LayoutParams(1, dp(14)));
        LinearLayout split = new LinearLayout(this);
        split.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout incomeColumn = new LinearLayout(this);
        incomeColumn.setOrientation(LinearLayout.VERTICAL);
        TextView incomeLabel = text("Income", 11, false);
        incomeLabel.setTextColor(0xFFB8C7E3);
        incomeColumn.addView(incomeLabel);
        TextView incomeValue = text(money(earned), 14, true);
        incomeValue.setTextColor(0xFFFFFFFF);
        incomeColumn.addView(incomeValue);
        split.addView(incomeColumn, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout expenseColumn = new LinearLayout(this);
        expenseColumn.setOrientation(LinearLayout.VERTICAL);
        TextView expenseLabel = text("Expenses", 11, false);
        expenseLabel.setTextColor(0xFFB8C7E3);
        expenseColumn.addView(expenseLabel);
        TextView expenseValue = text(money(spent), 14, true);
        expenseValue.setTextColor(0xFFFFFFFF);
        expenseColumn.addView(expenseValue);
        split.addView(expenseColumn, new LinearLayout.LayoutParams(0, -2, 1f));
        balance.addView(split);
        page.addView(balance);
        rowCards("Expected salary", money(expectedUpcomingSalary), "Next payday",
                upcomingPayday.format(DateTimeFormatter.ofPattern("MMM d")));

        View actionSpacer = space(dp(18));
        page.addView(actionSpacer, new LinearLayout.LayoutParams(1, dp(18)));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        int actionBackground = resolveColor(com.google.android.material.R.attr.colorSurfaceVariant);
        int actionAccent = resolveColor(androidx.appcompat.R.attr.colorPrimary);
        actions.addView(actionTile("＋", "Income", actionBackground, actionAccent,
                () -> showWorkDialog(null)), new LinearLayout.LayoutParams(0, dp(76), 1f));
        actions.addView(actionTile("−", "Expense", actionBackground, actionAccent,
                () -> showExpenseDialog(null)), new LinearLayout.LayoutParams(0, dp(76), 1f));
        actions.addView(actionTile("◉", "Budgets", actionBackground, actionAccent,
                () -> showTab(2)), new LinearLayout.LayoutParams(0, dp(76), 1f));
        actions.addView(actionTile("▦", "Calendar", actionBackground, actionAccent,
                this::buildCalendarScreen), new LinearLayout.LayoutParams(0, dp(76), 1f));
        page.addView(actions);

        LinearLayout snapshot = cardContainer();
        snapshot.setPadding(dp(16), dp(16), dp(16), dp(16));
        snapshot.setBackground(roundedBackground(resolveColor(com.google.android.material.R.attr.colorSurface), dp(18)));
        LinearLayout snapshotHeader = new LinearLayout(this);
        snapshotHeader.setGravity(Gravity.CENTER_VERTICAL);
        snapshotHeader.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout snapshotTitles = new LinearLayout(this);
        snapshotTitles.setOrientation(LinearLayout.VERTICAL);
        TextView snapshotTitle = text("Budget snapshot", 16, true);
        snapshotTitle.setTextColor(ink);
        android.graphics.drawable.Drawable snapshotIcon = getDrawable(R.drawable.ic_budget).mutate();
        snapshotIcon.setTint(accent);
        snapshotTitle.setCompoundDrawablesWithIntrinsicBounds(snapshotIcon, null, null, null);
        snapshotTitle.setCompoundDrawablePadding(dp(8));
        snapshotTitles.addView(snapshotTitle);
        TextView cutoffCaption = text("Spending in your current salary cycle", 12, false);
        cutoffCaption.setTextColor(muted);
        cutoffCaption.setPadding(0, dp(3), 0, 0);
        snapshotTitles.addView(cutoffCaption);
        snapshotHeader.addView(snapshotTitles, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialButton viewBudget = compactButton("View budget", false);
        viewBudget.setIconResource(R.drawable.ic_budget);
        viewBudget.setOnClickListener(v -> showTab(2));
        LinearLayout.LayoutParams viewBudgetParams = new LinearLayout.LayoutParams(-2, dp(36));
        viewBudgetParams.leftMargin = dp(8);
        snapshotHeader.addView(viewBudget, viewBudgetParams);
        snapshot.addView(snapshotHeader);

        BigDecimal limit;
        try {
            limit = new BigDecimal(pref("spending_budget", "0.00"));
            if (limit.signum() < 0) limit = BigDecimal.ZERO;
        } catch (NumberFormatException ignored) {
            limit = BigDecimal.ZERO;
        }
        BigDecimal cycleBudget = activePayment == null ? BigDecimal.ZERO
                : new BigDecimal(activePayment.receivedAmount).add(getCarryoverIntoPayment(activePayment));
        BigDecimal budgetBase = BudgetInsights.resolveBudgetBase(cycleBudget, limit);
        String spentLabel = budgetBase.signum() > 0
                ? money(spent) + " spent  ·  " + money(budgetBase) + " budget"
                : money(spent) + " spent  ·  No budget set";
        TextView limitText = text(spentLabel, 13, true);
        limitText.setTextColor(ink);
        limitText.setPadding(0, dp(16), 0, dp(10));
        snapshot.addView(limitText);
        budgetProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        budgetProgressBar.setMax(100);
        budgetProgressBar.setProgressTintList(ColorStateList.valueOf(green));
        budgetProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(resolveColor(com.google.android.material.R.attr.colorOutlineVariant)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
        progressParams.bottomMargin = dp(12);
        snapshot.addView(budgetProgressBar, progressParams);
        budgetSummaryView = text("", 12, false);
        budgetSummaryView.setTextColor(muted);
        budgetSummaryView.setLineSpacing(dp(5), 1.0f);
        snapshot.addView(budgetSummaryView);
        page.addView(snapshot);
        LinearLayout.LayoutParams snapshotParams = (LinearLayout.LayoutParams) snapshot.getLayoutParams();
        snapshotParams.topMargin = dp(18);
        snapshot.setLayoutParams(snapshotParams);
        refreshBudget();

        CutoffPeriod paymentCutoff = getCutoffAwaitingPayment(today);
        boolean paymentAlreadyRecorded = database.getPaydayPayment(paymentCutoff.startDate.toString(),
                paymentCutoff.endDate.toString()) != null;
        MaterialButton recordPayday = button("＋  Record payday salary", true);
        recordPayday.setOnClickListener(v -> showPaydayPaymentDialog());
        boolean paydayArrived = !getScheduledPayday(paymentCutoff).isAfter(today);
        recordPayday.setVisibility(paymentAlreadyRecorded || !paydayArrived ? View.GONE : View.VISIBLE);
        page.addView(recordPayday);

        LinearLayout recentHeader = new LinearLayout(this);
        recentHeader.setGravity(Gravity.CENTER_VERTICAL);
        recentHeader.setPadding(0, dp(20), 0, dp(8));
        TextView recentTitle = text("Recent transactions", 16, true);
        recentTitle.setTextColor(ink);
        recentHeader.addView(recentTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView seeAll = text("See all  ›", 11, true);
        seeAll.setTextColor(accent);
        seeAll.setOnClickListener(v -> showTab(1));
        recentHeader.addView(seeAll);
        page.addView(recentHeader);
        TextView recentFilter = text("This cutoff", 11, false);
        recentFilter.setTextColor(muted);
        page.addView(recentFilter);
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(historyContainer);
        refreshDashboardActivity();
        maybePromptForSavingsAllocation();

        TextView disclaimer = text("Salary amounts are estimates until you record a confirmed payday payment. Available-to-spend cash excludes reserved savings.", 11, false);
        disclaimer.setTextColor(muted);
        disclaimer.setPadding(dp(2), dp(10), dp(2), dp(12));
        page.addView(disclaimer);
    }

    private void buildCalendarScreen() {
        page.removeAllViews();
        Button back = button("←  Back to dashboard", false);
        back.setOnClickListener(v -> showTab(0));
        page.addView(back);
        header("Calendar", "Browse your work shifts and expenses by date.");
        CalendarView calendar = new CalendarView(this);
        calendar.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        LinearLayout calendarCard = cardContainer();
        calendarCard.addView(calendar, new LinearLayout.LayoutParams(-1, -2));
        page.addView(calendarCard);

        TextView selectedDateTitle = text("", 18, true);
        selectedDateTitle.setPadding(0, dp(12), 0, dp(8));
        page.addView(selectedDateTitle);
        LinearLayout selectedDateEntries = new LinearLayout(this);
        selectedDateEntries.setOrientation(LinearLayout.VERTICAL);
        page.addView(selectedDateEntries);

        Runnable[] refreshSelectedDate = new Runnable[1];
        refreshSelectedDate[0] = () -> {
            LocalDate selected = java.time.Instant.ofEpochMilli(calendar.getDate())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            String date = selected.toString();
            selectedDateTitle.setText(selected.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));
            selectedDateEntries.removeAllViews();
            boolean found = false;
            for (WorkDatabase.WorkEntry entry : database.getRecentWorkEntries(1000)) {
                if (!date.equals(entry.date)) continue;
                found = true;
                BigDecimal pay;
                try { pay = new BigDecimal(entry.netPay); } catch (NumberFormatException ex) { pay = BigDecimal.ZERO; }
                selectedDateEntries.addView(emptyState("Work shift · " + entry.timeIn + "–" + entry.timeOut,
                        "Estimated net pay: " + money(pay) + "  •  " + duration(entry.netMinutes)));
            }
            for (WorkDatabase.ExpenseEntry entry : database.getRecentExpenseEntries(1000)) {
                if (!date.equals(entry.date)) continue;
                found = true;
                BigDecimal amount;
                try { amount = new BigDecimal(entry.amount); } catch (NumberFormatException ex) { amount = BigDecimal.ZERO; }
                selectedDateEntries.addView(emptyState("Expense · " + entry.category,
                        (entry.note == null || entry.note.trim().isEmpty() ? "Recorded expense" : entry.note)
                                + "  •  " + money(amount)));
            }
            if (!found) selectedDateEntries.addView(emptyState("Nothing recorded", "No work shifts or expenses on this date."));
        };
        calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> refreshSelectedDate[0].run());
        refreshSelectedDate[0].run();
    }

    private LinearLayout metricRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(100));
        params.bottomMargin = dp(10);
        row.setLayoutParams(params);
        return row;
    }

    private View colorMetric(String label, String amount, String detail, int background, int accent) {
        LinearLayout metric = new LinearLayout(this);
        metric.setOrientation(LinearLayout.VERTICAL);
        metric.setGravity(Gravity.CENTER_VERTICAL);
        metric.setPadding(dp(14), dp(12), dp(14), dp(12));
        metric.setBackground(roundedBackground(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant), dp(20)));
        metric.addView(text(label, 12, false));
        TextView value = text(amount, 17, true);
        value.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        value.setPadding(0, dp(5), 0, dp(3));
        metric.addView(value);
        TextView caption = text(detail, 10, false);
        caption.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        metric.addView(caption);
        return metric;
    }

    private View actionTile(String symbol, String label, int background, int accent, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2), dp(5), dp(2), dp(5));

        View icon = actionIcon(symbol, accent);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(27), dp(27));
        iconParams.bottomMargin = dp(5);
        tile.addView(icon, iconParams);

        TextView title = text(label, 10, true);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(resolveColor(android.R.attr.textColorPrimary));
        title.setMaxLines(2);
        title.setIncludeFontPadding(false);
        tile.addView(title);
        tile.setClickable(true);
        tile.setFocusable(true);
        android.util.TypedValue outValue = new android.util.TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            tile.setForeground(getDrawable(outValue.resourceId));
        }
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private View actionIcon(String symbol, int accent) {
        android.widget.ImageView icon = new android.widget.ImageView(this);
        int iconRes = "＋".equals(symbol) ? R.drawable.ic_income
                : "−".equals(symbol) ? R.drawable.ic_expense
                : "◉".equals(symbol) ? R.drawable.ic_budget
                : "▦".equals(symbol) ? R.drawable.ic_calendar : R.drawable.ic_trending;
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        return icon;
    }

    private View metricCard(String label, String amount, int accent) {
        LinearLayout metric = cardContainer();
        metric.setPadding(dp(14), dp(14), dp(10), dp(14));
        TextView labelView = text(label, 11, true);
        labelView.setAlpha(0.75f);
        metric.addView(labelView);
        TextView amountView = text(amount, 18, true);
        amountView.setTextColor(accent);
        amountView.setPadding(0, dp(8), 0, 0);
        metric.addView(amountView);
        return metric;
    }

    private android.graphics.drawable.GradientDrawable roundedBackground(int color, int radius) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void refreshDashboardActivity() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews();

        Map<String, DayHistory> days = new TreeMap<>(java.util.Collections.reverseOrder());
        List<WorkDatabase.WorkEntry> workEntries = database.getRecentWorkEntries(100);
        List<WorkDatabase.ExpenseEntry> expenseEntries = database.getRecentExpenseEntries(100);

        for (WorkDatabase.WorkEntry entry : workEntries) {
            DayHistory day = days.get(entry.date);
            if (day == null) {
                day = new DayHistory(entry.date);
                days.put(entry.date, day);
            }
            day.workEntries.add(entry);
        }
        for (WorkDatabase.ExpenseEntry entry : expenseEntries) {
            DayHistory day = days.get(entry.date);
            if (day == null) {
                day = new DayHistory(entry.date);
                days.put(entry.date, day);
            }
            day.expenses.add(entry);
        }

        if (days.isEmpty()) {
            historyContainer.addView(emptyState("No activity yet", "Your saved shifts and expenses will show here."));
            return;
        }

        int shownDays = 0;
        for (DayHistory day : days.values()) {
            if (shownDays++ >= 4) break;

            LinearLayout dayCard = cardContainer();
            dayCard.setPadding(dp(14), dp(12), dp(14), dp(8));

            LinearLayout heading = new LinearLayout(this);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(Gravity.CENTER_VERTICAL);

            int count = day.workEntries.size() + day.expenses.size();
            TextView dateLabel = text(day.date + "  ·  " + count + (count == 1 ? " item" : " items"), 14, true);
            heading.addView(dateLabel, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView arrow = text("⌄", 22, true);
            arrow.setAlpha(0.75f);
            heading.addView(arrow);
            dayCard.addView(heading);

            LinearLayout records = new LinearLayout(this);
            records.setOrientation(LinearLayout.VERTICAL);
            records.setVisibility(View.GONE);

            for (WorkDatabase.WorkEntry entry : day.workEntries) {
                LinearLayout recordContent = new LinearLayout(this);
                recordContent.setOrientation(LinearLayout.VERTICAL);
                recordContent.addView(text("Work shift  ·  " + entry.timeIn + "–" + entry.timeOut, 13, true));
                recordContent.addView(text("Net pay: " + money(new BigDecimal(entry.netPay))
                        + "  ·  Work: " + duration(entry.netMinutes), 12, false));
                addSwipeReveal(records, recordContent, "Edit", "Delete",
                        v -> editWorkEntry(entry), v -> confirmDeleteWorkEntry(entry));
            }

            for (WorkDatabase.ExpenseEntry entry : day.expenses) {
                LinearLayout recordContent = new LinearLayout(this);
                recordContent.setOrientation(LinearLayout.VERTICAL);
                recordContent.addView(text("Expense  ·  " + entry.category, 13, true));
                recordContent.addView(text(money(new BigDecimal(entry.amount))
                        + (entry.note == null || entry.note.trim().isEmpty() ? "" : "  ·  " + entry.note), 12, false));
                addSwipeReveal(records, recordContent, "Edit", "Delete",
                        v -> editExpense(entry), v -> confirmDeleteExpense(entry));
            }

            dayCard.addView(records);
            heading.setOnClickListener(v -> {
                boolean expanded = records.getVisibility() == View.VISIBLE;
                records.setVisibility(expanded ? View.GONE : View.VISIBLE);
                arrow.setText(expanded ? "⌄" : "⌃");
            });
            dayCard.setOnClickListener(v -> heading.performClick());
            records.setOnClickListener(v -> { });
            historyContainer.addView(dayCard);
        }

        TextView seeAll = text("See all activity  →", 13, true);
        seeAll.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        seeAll.setPadding(dp(4), dp(10), 0, dp(4));
        seeAll.setOnClickListener(v -> showTab(1));
        historyContainer.addView(seeAll);
    }

    private void buildHistoryScreen() {
        header("Activity", "Expected salary, work shifts, and expenses in one place.");
        
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(historyContainer);
        refreshHistory();
    }

    private void showWorkDialog(WorkDatabase.WorkEntry entry) {
        editingWorkEntryId = entry == null ? -1 : entry.id;
        breakInputs.clear();
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(4), dp(4), dp(4));

        dateInput = field("Select work date", entry == null ? LocalDate.now().toString() : entry.date, InputType.TYPE_NULL);
        configureDatePicker(dateInput);
        addField(form, "Work date", dateInput);

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        timeInInput = field("Choose time", entry == null ? "08:00" : entry.timeIn, InputType.TYPE_NULL);
        timeOutInput = field("Choose time", entry == null ? "17:00" : entry.timeOut, InputType.TYPE_NULL);
        configureTimePicker(timeInInput);
        configureTimePicker(timeOutInput);
        TextInputLayout timeInLayout = inputLayout("Time in", timeInInput);
        TextInputLayout timeOutLayout = inputLayout("Time out", timeOutInput);
        times.addView(timeInLayout, new LinearLayout.LayoutParams(0, -2, 1f));
        times.addView(space(dp(8)), new LinearLayout.LayoutParams(dp(8), 1));
        times.addView(timeOutLayout, new LinearLayout.LayoutParams(0, -2, 1f));
        form.addView(times);

        // Suggest the most frequently saved time-in/time-out pair.
        Map<String, Integer> shiftCounts = new TreeMap<>();
        for (WorkDatabase.WorkEntry saved : database.getRecentWorkEntries(1000)) {
            String key = saved.timeIn + "|" + saved.timeOut;
            shiftCounts.put(key, shiftCounts.getOrDefault(key, 0) + 1);
        }
        String usualShift = null;
        int usualCount = 0;
        for (Map.Entry<String, Integer> candidate : shiftCounts.entrySet()) {
            if (candidate.getValue() > usualCount) { usualShift = candidate.getKey(); usualCount = candidate.getValue(); }
        }
        if (usualShift != null) {
            final String[] usualTimes = usualShift.split("\\|", -1);
            Button quickShift = button("Quick Shift - " + usualTimes[0] + " to " + usualTimes[1] + " (" + usualCount + " saved)", false);
            quickShift.setOnClickListener(v -> { timeInInput.setText(usualTimes[0]); timeOutInput.setText(usualTimes[1]); previewWorkEstimate(); });
            form.addView(quickShift);
        }

        hoursOnlyMode = new CheckBox(this);
        hoursOnlyMode.setText("Enter total worked hours instead of time out");
        hoursOnlyMode.setTextSize(14);
        form.addView(hoursOnlyMode);
        totalHoursInput = field("Total worked hours, excluding breaks (e.g. 10)", "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputLayout totalHoursLayout = inputLayout("Total worked hours", totalHoursInput);
        totalHoursLayout.setVisibility(View.GONE);
        form.addView(totalHoursLayout);
        hoursOnlyMode.setOnCheckedChangeListener((buttonView, checked) -> {
            totalHoursLayout.setVisibility(checked ? View.VISIBLE : View.GONE);
            timeOutInput.setEnabled(!checked);
            previewWorkEstimate();
        });

        addSection(form, "Break duration");
        breakContainer = new LinearLayout(this);
        breakContainer.setOrientation(LinearLayout.VERTICAL);
        form.addView(breakContainer);
        if (entry == null) {
            addBreakRow("1");
        } else {
            String[] parts = entry.breaks.split(",");
            for (String part : parts) {
                String value = part.trim().replace("h", "").trim();
                if (!value.isEmpty() && !"0".equals(value)) addBreakRow(value);
            }
            if (breakInputs.isEmpty()) addBreakRow("0");
        }
        Button addBreak = button("＋ Add break duration", false);
        addBreak.setOnClickListener(v -> addBreakRow(""));
        form.addView(addBreak);

        payRateTypeInput = createRateTypeSpinner();
        if (entry != null) payRateTypeInput.setText(entry.rateType, false);
        hourlyRateInput = field("Rate amount", entry == null ? pref("pay_rate", pref("hourly_rate", "100.00")) : entry.enteredRate,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        regularHoursInput = field("Regular hours per day",
                entry == null ? pref("regular_hours", "8")
                        : BigDecimal.valueOf(entry.configuredRegularMinutes).divide(BigDecimal.valueOf(60), 2,
                                java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        overtimeMultiplierInput = field("OT multiplier", entry == null ? pref("ot_multiplier", "1.25") : entry.overtimeMultiplier,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        allowanceInput = field("Daily allowance", entry == null ? pref("allowance", "0.00") : entry.allowance,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        deductionInput = field("Daily deduction", entry == null ? pref("deduction", "0.00") : entry.deduction,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        resultView = text(entry == null ? "Your pay breakdown will appear here after saving."
                : "Editing saved shift. Saving will recalculate this record using the values above.", 14, false);
        resultView.setPadding(0, dp(12), 0, dp(4));
        form.addView(resultView);
        android.text.TextWatcher previewWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { previewWorkEstimate(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        timeInInput.addTextChangedListener(previewWatcher);
        timeOutInput.addTextChangedListener(previewWatcher);
        hourlyRateInput.addTextChangedListener(previewWatcher);
        regularHoursInput.addTextChangedListener(previewWatcher);
        overtimeMultiplierInput.addTextChangedListener(previewWatcher);
        allowanceInput.addTextChangedListener(previewWatcher);
        deductionInput.addTextChangedListener(previewWatcher);
        totalHoursInput.addTextChangedListener(previewWatcher);
        previewWorkEstimate();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(form, new ScrollView.LayoutParams(-1, -2));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(entry == null ? "Add work shift" : "Edit work shift")
                .setView(scroll)
                .setNegativeButton("Cancel", (d, which) -> {
                    editingWorkEntryId = -1;
                    d.dismiss();
                })
                .setPositiveButton(entry == null ? "Calculate and save" : "Update shift", null)
                .create();
        dialog.setOnCancelListener(d -> editingWorkEntryId = -1);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f), -2);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            lastSaveSucceeded = false;
            calculateAndSave();
            if (lastSaveSucceeded) {
                dialog.dismiss();
                showTab(currentTab);
            }
        });
    }

    private void showExpenseDialog(WorkDatabase.ExpenseEntry entry) {
        editingExpenseId = entry == null ? -1 : entry.id;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(4), dp(4), dp(4));

        expenseDateInput = field("Select expense date", entry == null ? LocalDate.now().toString() : entry.date, InputType.TYPE_NULL);
        configureDatePicker(expenseDateInput);
        expenseCategoryInput = new MaterialAutoCompleteTextView(this);
        expenseCategoryInput.setSingleLine(true);
        expenseCategoryInput.setInputType(InputType.TYPE_CLASS_TEXT);
        expenseCategoryInput.setText(entry == null ? "" : entry.category, false);
        expenseCategoryInput.setThreshold(1);
        expenseCategoryInput.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, getExpenseCategorySuggestions()));
        expenseAmountInput = field("0.00", entry == null ? "" : entry.amount,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        expenseNoteInput = field("Optional note", entry == null ? "" : entry.note, InputType.TYPE_CLASS_TEXT);
        addField(form, "Date", expenseDateInput);
        addField(form, "Category", expenseCategoryInput);
        addField(form, "Amount (₱)", expenseAmountInput);
        addField(form, "Note", expenseNoteInput);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(form, new ScrollView.LayoutParams(-1, -2));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(entry == null ? "Add an expense" : "Edit expense")
                .setView(scroll)
                .setNegativeButton("Cancel", (d, which) -> {
                    editingExpenseId = -1;
                    d.dismiss();
                })
                .setPositiveButton(entry == null ? "Save expense" : "Update expense", null)
                .create();
        dialog.setOnCancelListener(d -> editingExpenseId = -1);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f), -2);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> attemptExpenseSave(dialog));
    }

    private void attemptExpenseSave(AlertDialog expenseDialog) {
        try {
            LocalDate.parse(value(expenseDateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            String category = value(expenseCategoryInput);
            if (category.isEmpty()) throw new IllegalArgumentException("Expense category is required.");
            BigDecimal amount = decimal(expenseAmountInput, "Expense amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");

            boolean updatingExpense = editingExpenseId >= 0;
            BigDecimal availableBefore = getAvailableCashBalance();
            BigDecimal savingsBefore = database.getTotalSavingsBalance();
            if (updatingExpense) {
                BigDecimal oldAmount = database.getExpenseAmount(editingExpenseId);
                BigDecimal oldDraw = database.getExpenseSavingsDraw(editingExpenseId);
                availableBefore = availableBefore.add(oldAmount).subtract(oldDraw);
                savingsBefore = savingsBefore.add(oldDraw);
            }
            BigDecimal savingsDraw = BudgetInsights.savingsDrawForExpense(availableBefore, savingsBefore, amount);
            BigDecimal availableAfter = availableBefore.subtract(amount).add(savingsDraw);
            if (savingsDraw.signum() > 0 || availableAfter.signum() <= 0) {
                String message;
                if (savingsDraw.signum() > 0) {
                    message = "This expense will use " + money(savingsDraw) + " from your savings."
                            + "\nSavings remaining after this expense: " + money(savingsBefore.subtract(savingsDraw))
                            + "\nAvailable to Spend after this expense: " + money(availableAfter)
                            + "\n\nContinue saving this expense?";
                } else {
                    message = "This expense will exhaust or exceed your Available to Spend."
                            + "\nAvailable to Spend after this expense: " + money(availableAfter.max(BigDecimal.ZERO))
                            + "\nThere is not enough savings to cover the extra amount."
                            + "\n\nContinue anyway?";
                }
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Budget warning")
                        .setMessage(message)
                        .setNegativeButton("Review expense", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Continue", (dialog, which) -> {
                            addExpense();
                            if (lastSaveSucceeded) {
                                expenseDialog.dismiss();
                                showTab(currentTab);
                            }
                        }).show();
                return;
            }
            addExpense();
            if (lastSaveSucceeded) {
                expenseDialog.dismiss();
                showTab(currentTab);
            }
        } catch (DateTimeParseException ex) {
            toast("Use YYYY-MM-DD for the expense date.");
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    private List<String> getExpenseCategorySuggestions() {
        List<String> categories = new ArrayList<>();
        String[] commonCategories = {
                "Food", "Transport", "Bills", "Groceries", "Health",
                "Shopping", "Entertainment", "Rent", "Utilities",
                "Education", "Personal care", "Other"
        };
        for (String category : commonCategories) {
            addCategorySuggestion(categories, category);
        }
        for (String category : database.getDistinctExpenseCategories()) {
            addCategorySuggestion(categories, category);
        }
        for (String category : database.getCategoryBudgetLimits().keySet()) {
            addCategorySuggestion(categories, category);
        }
        return categories;
    }

    private void addCategorySuggestion(List<String> categories, String category) {
        if (category == null) return;
        String trimmed = category.trim();
        if (trimmed.isEmpty()) return;
        for (String existing : categories) {
            if (existing.equalsIgnoreCase(trimmed)) return;
        }
        categories.add(trimmed);
    }

    private void buildBudgetScreen() {
        LocalDate today = LocalDate.now();
        int firstPayday = parsePaydayPreference("first_cutoff_payday", 22);
        int secondPayday = parsePaydayPreference("second_cutoff_payday", 7);
        CutoffPeriod currentPaydayCutoff = CutoffPeriod.forLatestPaydayOnOrBefore(
                today, firstPayday, secondPayday);
        LocalDate cycleStart = currentPaydayCutoff.payday(firstPayday, secondPayday);
        WorkDatabase.PaydayPayment latestPayment =
                database.getLatestPaydayPaymentOnOrBefore(today.toString());
        WorkDatabase.PaydayPayment activePayment = latestPayment != null
                && !LocalDate.parse(latestPayment.paydayDate).isBefore(cycleStart) ? latestPayment : null;

        CutoffPeriod nextCutoff = CutoffPeriod.forNextPayday(today, firstPayday, secondPayday);
        LocalDate nextPayday = nextCutoff.payday(firstPayday, secondPayday);
        long daysUntilPayday = Math.max(0,
                java.time.temporal.ChronoUnit.DAYS.between(today, nextPayday));

        BigDecimal spent = database.getExpenseTotal(cycleStart.toString(), today.toString());
        BigDecimal availableCash = getAvailableCashBalance();
        BigDecimal cycleBudget = activePayment == null ? BigDecimal.ZERO
                : new BigDecimal(activePayment.receivedAmount).add(getCarryoverIntoPayment(activePayment));
        BigDecimal spendingLimit;
        try {
            spendingLimit = new BigDecimal(pref("spending_budget", "0.00")).max(BigDecimal.ZERO);
        } catch (NumberFormatException ignored) {
            spendingLimit = BigDecimal.ZERO;
        }
        BigDecimal budgetBase = BudgetInsights.resolveBudgetBase(cycleBudget, spendingLimit);
        BigDecimal budgetRemaining = budgetBase.subtract(spent);
        BigDecimal dailyGuide = BudgetInsights.dailySpendingGuide(
                budgetBase, spent, availableCash, daysUntilPayday);

        header("Budget", "Manage your salary-cycle budget, category limits, and spending.");
        LinearLayout overview = cardContainer();
        overview.setPadding(dp(20), dp(18), dp(20), dp(20));
        LinearLayout overviewHeader = new LinearLayout(this);
        overviewHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView remainingLabel = text("REMAINING BUDGET", 11, true);
        remainingLabel.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        overviewHeader.addView(remainingLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialButton editBudget = compactButton("Edit", false);
        editBudget.setOnClickListener(v -> showSpendingBudgetDialog());
        overviewHeader.addView(editBudget);
        overview.addView(overviewHeader);
        TextView remainingValue = text(budgetBase.signum() > 0
                ? money(budgetRemaining.max(BigDecimal.ZERO)) : "Not set", 28, true);
        remainingValue.setPadding(0, dp(6), 0, dp(5));
        overview.addView(remainingValue);
        String budgetDetail = budgetBase.signum() > 0
                ? money(spent) + " spent of " + money(budgetBase)
                : "Set a salary-cycle limit in Settings to track budget usage.";
        TextView budgetDetailView = text(budgetDetail, 13, false);
        budgetDetailView.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        overview.addView(budgetDetailView);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        int usedPercent = budgetBase.signum() > 0
                ? spent.multiply(BigDecimal.valueOf(100))
                    .divide(budgetBase, 0, java.math.RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).intValue() : 0;
        progress.setProgress(usedPercent);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
        progressParams.topMargin = dp(12);
        overview.addView(progress, progressParams);
        page.addView(overview);

        rowCards("Spent this cycle", money(spent), "Available cash", money(availableCash));
        LinearLayout cycleCard = cardContainer();
        cycleCard.addView(text("SALARY CYCLE", 11, true));
        cycleCard.addView(text("From " + cycleStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                + " to today", 14, true));
        cycleCard.addView(text("Next payday: " + nextPayday.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                + " · " + daysUntilPayday + (daysUntilPayday == 1 ? " day" : " days") + " left",
                13, false));
        BigDecimal dailySpendable = dailyGuide;
        cycleCard.addView(text("Daily spending guide: " + money(dailySpendable),
                15, true));
        TextView dailyNote = text("Guide is capped by both remaining budget and available cash. Set aside bills due before payday.", 12, false);
        dailyNote.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        cycleCard.addView(dailyNote);
        page.addView(cycleCard);

        LinearLayout categoryHeader = new LinearLayout(this);
        categoryHeader.setGravity(Gravity.CENTER_VERTICAL);
        categoryHeader.setPadding(0, dp(8), 0, dp(8));
        TextView categoryTitle = text("Category budgets", 20, true);
        categoryHeader.addView(categoryTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialButton addCategory = compactButton("＋ Add limit", true);
        addCategory.setOnClickListener(v -> showCategoryBudgetDialog(null));
        categoryHeader.addView(addCategory);
        LinearLayout.LayoutParams categoryHeaderParams = new LinearLayout.LayoutParams(-1, -2);
        categoryHeaderParams.topMargin = dp(8);
        categoryHeaderParams.bottomMargin = dp(4);
        page.addView(categoryHeader, categoryHeaderParams);

        Map<String, BigDecimal> categoryTotals =
                database.getExpenseTotalsByCategory(cycleStart.toString(), today.toString());
        Map<String, BigDecimal> categoryLimits = database.getCategoryBudgetLimits();
        Map<String, String> categories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : new String[]{"Food & Groceries", "Transportation", "Bills", "Savings", "Personal", "Shopping", "Others"}) {
            categories.put(name, name);
        }
        for (String name : database.getDistinctExpenseCategories()) categories.put(name, name);
        for (String name : categoryLimits.keySet()) categories.put(name, name);
        if (categories.isEmpty()) {
            page.addView(emptyState("No categories yet", "Add an expense or create a category budget limit."));
        } else {
            List<String> categoryOrder = new ArrayList<>(categories.values());
            categoryOrder.sort((left, right) -> {
                boolean leftHasLimit = categoryLimits.containsKey(left)
                        && categoryLimits.get(left) != null
                        && categoryLimits.get(left).signum() > 0;
                boolean rightHasLimit = categoryLimits.containsKey(right)
                        && categoryLimits.get(right) != null
                        && categoryLimits.get(right).signum() > 0;
                if (leftHasLimit != rightHasLimit) return leftHasLimit ? -1 : 1;
                return String.CASE_INSENSITIVE_ORDER.compare(left, right);
            });
            for (String category : categoryOrder) {
                BigDecimal categorySpent = categoryTotals.get(category);
                if (categorySpent == null) categorySpent = BigDecimal.ZERO.setScale(2);
                BigDecimal categoryLimit = categoryLimits.get(category);
                addCategoryBudgetRow(category, categorySpent, categoryLimit);
            }
        }

        MaterialButton addExpense = button("＋ Add expense", true);
        addExpense.setOnClickListener(v -> showExpenseDialog(null));
        LinearLayout.LayoutParams addExpenseParams = new LinearLayout.LayoutParams(-1, -2);
        addExpenseParams.topMargin = dp(4);
        addExpenseParams.bottomMargin = dp(8);
        page.addView(addExpense, addExpenseParams);


        section("Savings overview");
        LinearLayout savingsSummary = cardContainer();
        savingsSummary.addView(text("Total savings", 13, false));
        savingsSummary.addView(text(money(database.getTotalSavingsBalance()), 25, true));
        savingsSummary.addView(text("Available to Spend: " + money(getAvailableCashBalance()), 13, false));
        savingsSummary.addView(text("Contributions transfer money from Available to Spend into savings. Withdrawals return money to Available to Spend.", 12, false));
        MaterialButton withdrawSavings = button("Withdraw from savings", false);
        withdrawSavings.setOnClickListener(v -> {
            List<WorkDatabase.SavingsGoal> goals = database.getSavingsGoals();
            if (goals.isEmpty()) {
                toast("Create a savings goal first to track a withdrawal.");
                return;
            }
            showSavingsWithdrawalGoalPicker(goals);
        });
        savingsSummary.addView(withdrawSavings);
        page.addView(savingsSummary);

        section("Savings goals");
        TextView savingsGoalsInfo = text("Create a target, track contributions, and see how close you are to your goal. Contributions increase total savings and reduce Available to Spend.", 13, false);
        savingsGoalsInfo.setPadding(0, 0, 0, dp(8));
        page.addView(savingsGoalsInfo);
        Button addSavingsGoal = button("＋ Create savings goal", true);
        addSavingsGoal.setOnClickListener(v -> showSavingsGoalDialog(null));
        page.addView(addSavingsGoal);
        List<WorkDatabase.SavingsGoal> savingsGoals = database.getSavingsGoals();
        if (savingsGoals.isEmpty()) {
            page.addView(emptyState("No savings goals yet", "Try an emergency fund, a gadget, travel, or another personal target."));
        } else {
            for (WorkDatabase.SavingsGoal goal : savingsGoals) {
                LinearLayout goalCard = cardContainer();
                goalCard.addView(text(goal.name, 16, true));
                goalCard.addView(text(money(goal.savedAmount) + " saved of " + money(goal.targetAmount), 14, true));
                int goalProgressPercent = goal.targetAmount.signum() <= 0 ? 0
                        : goal.savedAmount.multiply(BigDecimal.valueOf(100))
                        .divide(goal.targetAmount, 0, java.math.RoundingMode.DOWN).intValue();
                ProgressBar goalProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                goalProgress.setMax(100);
                goalProgress.setProgress(Math.min(100, Math.max(0, goalProgressPercent)));
                LinearLayout.LayoutParams goalProgressParams = new LinearLayout.LayoutParams(-1, dp(8));
                goalProgressParams.topMargin = dp(8);
                goalProgressParams.bottomMargin = dp(6);
                goalCard.addView(goalProgress, goalProgressParams);
                String detail = goal.targetDate.isEmpty() ? "No target date"
                        : "Target date: " + goal.targetDate;
                BigDecimal remainingForGoal = goal.targetAmount.subtract(goal.savedAmount).max(BigDecimal.ZERO);
                if (goal.savedAmount.compareTo(goal.targetAmount) >= 0) {
                    detail += " · Goal reached!";
                } else {
                    detail += " · " + money(remainingForGoal) + " to go";
                    if (!goal.targetDate.isEmpty()) {
                        try {
                            LocalDate deadline = LocalDate.parse(goal.targetDate);
                            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), deadline);
                            if (daysRemaining < 0) {
                                detail += " · Overdue by " + Math.abs(daysRemaining)
                                        + (Math.abs(daysRemaining) == 1 ? " day" : " days");
                            } else if (daysRemaining == 0) {
                                detail += " · Due today";
                            } else {
                                detail += " · " + daysRemaining + (daysRemaining == 1 ? " day" : " days") + " left";
                                BigDecimal dailyTarget = remainingForGoal.divide(BigDecimal.valueOf(daysRemaining),
                                        2, java.math.RoundingMode.CEILING);
                                detail += " · Save about " + money(dailyTarget) + "/day";
                            }
                        } catch (DateTimeParseException ignored) {
                            // Ignore a legacy malformed date; editing the goal lets the user correct it.
                        }
                    }
                }
                TextView detailView = text(detail, 12, false);
                detailView.setTextColor(resolveColor(android.R.attr.textColorSecondary));
                goalCard.addView(detailView);
                LinearLayout goalActions = new LinearLayout(this);
                goalActions.setOrientation(LinearLayout.HORIZONTAL);
                MaterialButton contribute = compactButton("＋ Add savings", true);
                contribute.setOnClickListener(v -> showSavingsContributionDialog(goal));
                MaterialButton editGoal = compactButton("Edit", false);
                editGoal.setOnClickListener(v -> showSavingsGoalDialog(goal));
                MaterialButton deleteGoal = compactButton("Delete", false);
                deleteGoal.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                        .setTitle("Delete savings goal?")
                        .setMessage("This removes the goal and its tracked progress, but does not change your overall savings balance.")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (database.deleteSavingsGoal(goal.id)) {
                                toast("Savings goal deleted.");
                                showTab(2);
                            } else toast("Could not delete savings goal.");
                        }).show());
                goalActions.addView(contribute);
                goalActions.addView(editGoal);
                goalActions.addView(deleteGoal);
                goalCard.addView(goalActions);
                page.addView(goalCard);
            }
        }

        section("Savings activity");
        List<String> savingsActivity = database.getRecentSavingsActivity(20);
        if (savingsActivity.isEmpty()) {
            page.addView(emptyState("No savings transfers yet", "Contributions and withdrawals will appear here."));
        } else {
            LinearLayout savingsActivityList = new LinearLayout(this);
            savingsActivityList.setOrientation(LinearLayout.VERTICAL);
            page.addView(savingsActivityList);
            for (String activity : savingsActivity) addListItem(savingsActivityList, activity);
        }

        section("Expenses");
        MaterialAutoCompleteTextView categoryFilter = new MaterialAutoCompleteTextView(this);
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All categories");
        filterOptions.addAll(database.getDistinctExpenseCategories());
        categoryFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filterOptions));
        categoryFilter.setText("All categories", false);
        categoryFilter.setInputType(InputType.TYPE_NULL);
        addField(page, "Filter by category", categoryFilter);
        LinearLayout budgetExpenseList = new LinearLayout(this);
        budgetExpenseList.setOrientation(LinearLayout.VERTICAL);
        page.addView(budgetExpenseList);
        renderBudgetExpenses(budgetExpenseList, "All categories");
        categoryFilter.setOnItemClickListener((parent, view, position, id) ->
                renderBudgetExpenses(budgetExpenseList, categoryFilter.getText().toString()));
        Button allActivity = button("View all activity", false);
        allActivity.setOnClickListener(v -> showTab(1));
        page.addView(allActivity);
    }


    private void showSavingsGoalDialog(WorkDatabase.SavingsGoal goal) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText nameInput = field("Goal name", goal == null ? "" : goal.name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText targetInput = field("Target amount (₱)", goal == null ? "" : goal.targetAmount.toPlainString(),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText dateInput = field("Target date (optional, YYYY-MM-DD)", goal == null ? "" : goal.targetDate,
                InputType.TYPE_CLASS_TEXT);
        addField(form, "Goal name", nameInput);
        addField(form, "Target amount (₱)", targetInput);
        addField(form, "Target date (optional)", dateInput);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(goal == null ? "Create savings goal" : "Edit savings goal")
                .setView(form)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton(goal == null ? "Create goal" : "Save changes", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String name = value(nameInput);
                BigDecimal target = decimal(targetInput, "Target amount");
                String targetDate = value(dateInput);
                if (name.isEmpty()) throw new IllegalArgumentException("Goal name is required.");
                if (target.signum() <= 0) throw new IllegalArgumentException("Target amount must be greater than zero.");
                if (!targetDate.isEmpty()) {
                    try { LocalDate.parse(targetDate); }
                    catch (DateTimeParseException ex) { throw new IllegalArgumentException("Target date must use YYYY-MM-DD."); }
                }
                boolean saved = goal == null
                        ? database.addSavingsGoal(name, target, targetDate) > 0
                        : database.updateSavingsGoal(goal.id, name, target, targetDate);
                if (!saved) throw new IllegalArgumentException("Could not save this savings goal.");
                dialog.dismiss();
                showTab(2);
            } catch (IllegalArgumentException ex) {
                toast(ex.getMessage());
            }
        }));
        dialog.show();
    }

    private void showSavingsWithdrawalGoalPicker(List<WorkDatabase.SavingsGoal> goals) {
        List<WorkDatabase.SavingsGoal> fundedGoals = new ArrayList<>();
        for (WorkDatabase.SavingsGoal goal : goals) {
            if (goal.savedAmount.signum() > 0) fundedGoals.add(goal);
        }
        if (fundedGoals.isEmpty()) {
            toast("There is no goal savings balance available to withdraw.");
            return;
        }
        String[] names = new String[fundedGoals.size()];
        for (int i = 0; i < fundedGoals.size(); i++) names[i] = fundedGoals.get(i).name +
                " · " + money(fundedGoals.get(i).savedAmount);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose savings goal")
                .setItems(names, (dialog, which) -> showSavingsWithdrawalDialog(fundedGoals.get(which)))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSavingsWithdrawalDialog(WorkDatabase.SavingsGoal goal) {
        EditText amountInput = field("Amount to withdraw (₱)", "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        BigDecimal savings = database.getTotalSavingsBalance();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Withdraw from " + goal.name)
                .setMessage("Goal balance: " + money(goal.savedAmount) +
                        "\nTotal savings: " + money(savings) +
                        "\nAvailable to Spend now: " + money(getAvailableCashBalance()) +
                        "\n\nThe withdrawal reduces this goal and total savings, and returns the amount to Available to Spend.")
                .setView(amountInput)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Withdraw", (dialog, which) -> {
                    try {
                        BigDecimal amount = decimal(amountInput, "Withdrawal")
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        if (amount.signum() <= 0) throw new IllegalArgumentException("Withdrawal must be greater than zero.");
                        if (amount.compareTo(goal.savedAmount) > 0) {
                            throw new IllegalArgumentException("You can withdraw up to " + money(goal.savedAmount) +
                                    " from this goal.");
                        }
                        if (!database.withdrawFromSavingsGoal(goal.id, amount)) {
                            toast("Could not withdraw. Refresh the goal balance and try again.");
                            return;
                        }
                        toast("Savings withdrawn to Available to Spend.");
                        showTab(2);
                    } catch (IllegalArgumentException ex) {
                        toast(ex.getMessage());
                    }
                })
                .show();
    }

    private void showSavingsContributionDialog(WorkDatabase.SavingsGoal goal) {
        EditText amountInput = field("Amount to add (₱)", "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        BigDecimal available = getAvailableCashBalance().max(BigDecimal.ZERO);
        BigDecimal remaining = goal.targetAmount.subtract(goal.savedAmount).max(BigDecimal.ZERO);

        LinearLayout contributionForm = new LinearLayout(this);
        contributionForm.setOrientation(LinearLayout.VERTICAL);
        contributionForm.setPadding(dp(20), dp(8), dp(20), 0);
        addField(contributionForm, "Contribution amount (₱)", amountInput);

        TextView presetHint = text(
                remaining.signum() > 0
                        ? "Quick amounts are based on the remaining goal and capped by your available cash. You can edit the amount before transferring."
                        : "This goal has reached its target. You can still record an extra contribution if you choose.",
                12, false);
        presetHint.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        presetHint.setPadding(0, dp(2), 0, dp(8));
        contributionForm.addView(presetHint);

        if (remaining.signum() > 0 && available.signum() > 0) {
            LinearLayout presets = new LinearLayout(this);
            presets.setOrientation(LinearLayout.HORIZONTAL);
            BigDecimal[] portions = {
                    new BigDecimal("0.25"), new BigDecimal("0.50"), BigDecimal.ONE
            };
            String[] labels = {"25%", "50%", "Finish goal"};
            for (int i = 0; i < portions.length; i++) {
                BigDecimal suggested = remaining.multiply(portions[i])
                        .setScale(2, java.math.RoundingMode.CEILING)
                        .min(available);
                MaterialButton preset = compactButton(labels[i] + "\n" + money(suggested), false);
                preset.setOnClickListener(v -> amountInput.setText(suggested.toPlainString()));
                LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(0, -2, 1f);
                presetParams.setMargins(0, 0, dp(4), dp(4));
                presets.addView(preset, presetParams);
            }
            contributionForm.addView(presets);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Transfer to " + goal.name)
                .setMessage("Current goal progress: " + money(goal.savedAmount) + " of " + money(goal.targetAmount)
                        + "\nStill needed: " + money(remaining)
                        + "\nAvailable to Spend: " + money(available)
                        + "\n\nThe contribution will be added to your savings balance and deducted from Available to Spend.")
                .setView(contributionForm)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Transfer savings", (dialog, which) -> {
                    try {
                        BigDecimal amount = decimal(amountInput, "Contribution")
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        if (amount.signum() <= 0) throw new IllegalArgumentException("Contribution must be greater than zero.");
                        BigDecimal availableNow = getAvailableCashBalance();
                        if (amount.compareTo(availableNow) > 0) {
                            throw new IllegalArgumentException("Not enough Available to Spend. Current available amount: "
                                    + money(availableNow.max(BigDecimal.ZERO)) + ".");
                        }
                        if (!database.addToSavingsGoal(goal.id, amount)) {
                            toast("Could not transfer savings. No balance was changed.");
                            return;
                        }
                        toast("Savings transferred successfully.");
                        showTab(2);
                    } catch (IllegalArgumentException ex) {
                        toast(ex.getMessage());
                    }
                }).show();
    }

    private void renderBudgetExpenses(LinearLayout container, String selectedCategory) {
        container.removeAllViews();
        List<WorkDatabase.ExpenseEntry> entries = database.getRecentExpenseEntries(1000);
        int shown = 0;
        for (WorkDatabase.ExpenseEntry entry : entries) {
            if (selectedCategory != null && !"All categories".equalsIgnoreCase(selectedCategory)
                    && !selectedCategory.equalsIgnoreCase(entry.category)) continue;

            LinearLayout item = cardContainer();
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(16), dp(14), dp(16), dp(12));

            LinearLayout summary = new LinearLayout(this);
            summary.setOrientation(LinearLayout.HORIZONTAL);
            summary.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            TextView categoryName = text(entry.category, 15, true);
            categoryName.setMaxLines(1);
            categoryName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(categoryName);

            String note = entry.note == null || entry.note.trim().isEmpty()
                    ? entry.date : entry.date + " · " + entry.note.trim();
            TextView noteView = text(note, 12, false);
            noteView.setTextColor(resolveColor(android.R.attr.textColorSecondary));
            noteView.setMaxLines(2);
            noteView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
            noteParams.topMargin = dp(3);
            details.addView(noteView, noteParams);
            summary.addView(details, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView amount = text(money(new BigDecimal(entry.amount)), 16, true);
            amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            amount.setTextColor(resolveColor(android.R.attr.colorError));
            LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(-2, -2);
            amountParams.leftMargin = dp(12);
            summary.addView(amount, amountParams);
            item.addView(summary);

            View separator = new View(this);
            separator.setBackgroundColor(withAlpha(
                    resolveColor(android.R.attr.textColorSecondary), 0x28));
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(-1, dp(1));
            separatorParams.topMargin = dp(12);
            separatorParams.bottomMargin = dp(8);
            item.addView(separator, separatorParams);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            MaterialButton edit = compactButton("Edit", false);
            edit.setOnClickListener(v -> showExpenseDialog(entry));
            actions.addView(edit);

            MaterialButton delete = compactButton("Delete", false);
            delete.setTextColor(resolveColor(androidx.appcompat.R.attr.colorError));
            delete.setBackgroundTintList(ColorStateList.valueOf(
                    withAlpha(resolveColor(androidx.appcompat.R.attr.colorError), 0x12)));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, -2);
            deleteParams.leftMargin = dp(8);
            actions.addView(delete, deleteParams);
            delete.setOnClickListener(v -> confirmDeleteExpense(entry));

            item.addView(actions);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(-1, -2);
            itemParams.bottomMargin = dp(8);
            container.addView(item, itemParams);
            shown++;
        }
        if (shown == 0) {
            container.addView(emptyState("No matching expenses",
                    "Try another category or add a new expense."));
        }
    }

    private int parsePaydayPreference(String key, int fallback) {
        try {
            int value = Integer.parseInt(pref(key, Integer.toString(fallback)));
            return Math.min(31, Math.max(1, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void addCategoryBudgetRow(String category, BigDecimal spent, BigDecimal limit) {
        LinearLayout card = cardContainer();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.ImageView categoryIcon = new android.widget.ImageView(this);
        categoryIcon.setImageResource(R.drawable.ic_category);
        categoryIcon.setColorFilter(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        categoryIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams categoryIconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        categoryIconParams.rightMargin = dp(10);
        heading.addView(categoryIcon, categoryIconParams);
        TextView title = text(category, 15, true);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView amount = text(limit == null || limit.signum() <= 0
                ? money(spent) + " spent" : money(spent) + " / " + money(limit), 12, true);
        amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        amount.setMaxLines(2);
        heading.addView(amount);
        card.addView(heading);
        if (limit != null && limit.signum() > 0) {
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            int pct = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 0, java.math.RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).intValue();
            bar.setProgress(pct);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(6));
            barParams.topMargin = dp(8);
            card.addView(bar, barParams);
            BigDecimal remaining = limit.subtract(spent);
            TextView remainingText = text(remaining.signum() < 0
                    ? "Over limit by " + money(remaining.abs())
                    : money(remaining) + " remaining", 12, false);
            remainingText.setTextColor(remaining.signum() < 0
                    ? resolveColor(android.R.attr.colorError)
                    : resolveColor(android.R.attr.textColorSecondary));
            card.addView(remainingText);
        } else {
            TextView unset = text("No category limit set", 12, false);
            unset.setTextColor(resolveColor(android.R.attr.textColorSecondary));
            card.addView(unset);
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(8), 0, 0);
        MaterialButton edit = compactButton(limit == null ? "Set limit" : "Edit", false);
        edit.setOnClickListener(v -> showCategoryBudgetDialog(category));
        actions.addView(edit);
        if (limit != null) {
            MaterialButton delete = compactButton("Delete", false);
            delete.setTextColor(resolveColor(androidx.appcompat.R.attr.colorError));
            delete.setBackgroundTintList(ColorStateList.valueOf(
                    withAlpha(resolveColor(androidx.appcompat.R.attr.colorError), 0x12)));
            LinearLayout.LayoutParams deleteParams = (LinearLayout.LayoutParams) delete.getLayoutParams();
            deleteParams.leftMargin = dp(8);
            delete.setLayoutParams(deleteParams);
            delete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete category budget?")
                    .setMessage("This deletes the saved spending limit for “" + category
                            + "”. Existing expenses and their category labels will remain unchanged.")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Delete limit", (dialog, which) -> {
                        if (database.deleteCategoryBudgetLimit(category)) {
                            showTab(2);
                            toast("Category budget limit deleted.");
                        } else {
                            toast("Could not delete this category limit.");
                        }
                    }).show());
            actions.addView(delete);
        }
        card.addView(actions);
        page.addView(card);
    }

    private void showCategoryBudgetDialog(String existingCategory) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(8), dp(4), dp(4));
        EditText categoryInput = field("Category name", existingCategory == null ? "" : existingCategory,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText limitInput = field("0.00", existingCategory == null ? "" :
                database.getCategoryBudgetLimits().get(existingCategory) == null ? "" :
                        database.getCategoryBudgetLimits().get(existingCategory).toPlainString(),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existingCategory != null) categoryInput.setEnabled(false);
        addField(form, "Category", categoryInput);
        addField(form, "Limit per salary cycle (₱)", limitInput);
        new MaterialAlertDialogBuilder(this)
                .setTitle(existingCategory == null ? "Add category budget" : "Edit category budget")
                .setView(form)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Save", (dialog, which) -> {
                    String category = categoryInput.getText() == null ? "" : categoryInput.getText().toString().trim();
                    String rawLimit = limitInput.getText() == null ? "" : limitInput.getText().toString().trim();
                    try {
                        if (category.isEmpty()) throw new IllegalArgumentException("Category name is required.");
                        BigDecimal limit = new BigDecimal(rawLimit);
                        if (limit.signum() <= 0) throw new IllegalArgumentException("Limit must be greater than zero.");
                        if (!database.saveCategoryBudgetLimit(category, limit)) {
                            throw new IllegalArgumentException("Could not save this category limit.");
                        }
                        showTab(2);
                        toast("Category budget saved.");
                    } catch (NumberFormatException ex) {
                        toast("Enter a valid category limit.");
                    } catch (IllegalArgumentException ex) {
                        toast(ex.getMessage());
                    }
                }).show();
    }

    private void buildSettingsScreen() {
        header("Settings", "Set your usual pay rates and cutoff schedule.");
        LinearLayout card = cardContainer();
        page.addView(card);
        addSection(card, "Salary defaults");
        hourlyRateInput = field("Rate amount", pref("pay_rate", pref("hourly_rate", "100.00")), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        regularHoursInput = field("Regular hours per day", pref("regular_hours", "8"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        overtimeMultiplierInput = field("OT multiplier", pref("ot_multiplier", "1.25"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        allowanceInput = field("Daily allowance", pref("allowance", "0.00"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        deductionInput = field("Daily deduction", pref("deduction", "0.00"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        payRateTypeInput = createRateTypeSpinner();
        addField(card, "Pay rate type", payRateTypeInput);
        addField(card, "Rate amount (₱)", hourlyRateInput);
        updateRateLabel(payRateTypeInput, hourlyRateInput);
        addField(card, "Regular hours", regularHoursInput);
        addField(card, "Ordinary OT multiplier", overtimeMultiplierInput);
        addField(card, "Daily allowance (₱)", allowanceInput);
        addField(card, "Daily deduction (₱)", deductionInput);
        Button save = button("Save salary defaults", true);
        save.setOnClickListener(v -> saveSalaryDefaults());
        card.addView(save);

        section("Cutoff and payday schedule");
        TextView scheduleInfo = text("Cutoff period and payday are different settings. The default cutoff periods stay 1st–15th and 16th–month-end; choose the separate payday for each period.", 13, false);
        scheduleInfo.setPadding(0, 0, 0, dp(8));
        page.addView(scheduleInfo);
        firstPaydayInput = field("Payday day (1–31)", pref("first_cutoff_payday", "22"), InputType.TYPE_CLASS_NUMBER);
        secondPaydayInput = field("Payday day (1–31)", pref("second_cutoff_payday", "7"), InputType.TYPE_CLASS_NUMBER);
        addField(page, "Payday for 1st–15th cutoff (same month)", firstPaydayInput);
        addField(page, "Payday for 16th–month-end cutoff (following month)", secondPaydayInput);
        Button saveSchedule = button("Save cutoff and payday settings", true);
        saveSchedule.setOnClickListener(v -> savePaydaySettings());
        page.addView(saveSchedule);
        card("1st cutoff", "1st–15th of each month", "Payday: " + pref("first_cutoff_payday", "22") + "th of the month");
        card("2nd cutoff", "16th–last day of each month", "Payday: " + pref("second_cutoff_payday", "7") + "th of the following month");

        section("Saved salary payments");
        TextView paymentsInfo = text("Edit a salary payment here if you need to correct the amount or date. Editing updates the existing cutoff record, so it will not add the salary to your balance twice.", 13, false);
        paymentsInfo.setPadding(0, 0, 0, dp(8));
        page.addView(paymentsInfo);
        List<WorkDatabase.PaydayPayment> savedPayments = database.getPaydayPayments(100);
        if (savedPayments.isEmpty()) {
            page.addView(emptyState("No salary payments saved", "Recorded payday salaries will appear here for editing."));
        } else {
            for (WorkDatabase.PaydayPayment payment : savedPayments) {
                LinearLayout paymentCard = cardContainer();
                paymentCard.addView(text("Received: " + money(new BigDecimal(payment.receivedAmount)), 16, true));
                paymentCard.addView(text("Date received: " + payment.paydayDate, 13, false));
                paymentCard.addView(text("Cutoff: " + payment.cutoffStart + " to " + payment.cutoffEnd, 13, false));
                paymentCard.addView(text("Expected: " + money(new BigDecimal(payment.expectedAmount)), 13, false));
                Button editPayment = button("Edit saved salary", false);
                editPayment.setOnClickListener(v -> {
                    try {
                        showPaydayPaymentDialog(CutoffPeriod.forDate(LocalDate.parse(payment.cutoffStart)));
                    } catch (DateTimeParseException ex) {
                        toast("Could not open this saved salary payment.");
                    }
                });
                paymentCard.addView(editPayment);
                Button deletePayment = button("Delete saved salary", false);
                deletePayment.setOnClickListener(v -> confirmDeletePaydayPayment(payment));
                paymentCard.addView(deletePayment);
                page.addView(paymentCard);
            }
        }

        section("Starting cash balance");
        TextView openingInfo = text("Enter the cash you had when you began tracking. All expenses saved in this app are subtracted from this amount, and confirmed payday payments are added.", 13, false);
        openingInfo.setPadding(0, 0, 0, dp(8));
        page.addView(openingInfo);
        openingBalanceInput = field("0.00", pref("opening_cash_balance", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(page, "Opening cash balance (₱)", openingBalanceInput);
        Button saveOpening = button("Save starting balance", true);
        saveOpening.setOnClickListener(v -> saveOpeningBalance());
        page.addView(saveOpening);

        section("Starting savings balance");
        TextView savingsInfo = text("Enter savings already included in your opening cash balance. This amount is excluded from Available to Spend and can cover overspending when needed.", 13, false);
        savingsInfo.setPadding(0, 0, 0, dp(8));
        page.addView(savingsInfo);
        openingSavingsInput = field("0.00", pref("opening_savings_balance", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(page, "Opening savings balance (₱)", openingSavingsInput);
        Button saveSavings = button("Save starting savings", true);
        saveSavings.setOnClickListener(v -> saveOpeningSavingsBalance());
        page.addView(saveSavings);

        section("Spending budget");
        TextView budgetInfo = text("Set a spending cap for each salary cycle. The dashboard tracks expenses since the most recently recorded payday against this limit. Enter 0 to disable the cap.", 13, false);
        budgetInfo.setPadding(0, 0, 0, dp(8));
        page.addView(budgetInfo);
        spendingBudgetInput = field("0.00", pref("spending_budget", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(page, "Spending limit per salary cycle (₱)", spendingBudgetInput);
        Button saveBudget = button("Save spending budget", true);
        saveBudget.setOnClickListener(v -> saveSpendingBudget());
        page.addView(saveBudget);

        section("About your data");
        TextView info = text("Your entries are stored locally on this device. Expected pay is estimated from saved shifts. Record the amount actually received on payday to update total cash. Savings are tracked separately and excluded from available-to-spend cash. Old salary and expense history is preserved when a new salary cycle begins.", 14, false);
        info.setPadding(dp(4), dp(4), dp(4), dp(12));
        page.addView(info);
    }

    private void addBreakRow(String hours) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        EditText breakHours = field("Total break hours (e.g. 0.5)", hours, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        row.addView(breakHours, new LinearLayout.LayoutParams(0, dp(56), 1f));
        Button remove = button("×", false);
        row.addView(remove, new LinearLayout.LayoutParams(dp(48), dp(52)));
        BreakInput input = new BreakInput(row, breakHours);
        remove.setOnClickListener(v -> {
            if (breakInputs.size() == 1) breakHours.setText("");
            else { breakInputs.remove(input); breakContainer.removeView(row); }
        });
        breakInputs.add(input);
        breakHours.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { previewWorkEstimate(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        breakContainer.addView(row);
    }

    /** Updates the estimate while editing. This method never writes to the database. */
    private void previewWorkEstimate() {
        if (resultView == null || timeInInput == null || timeOutInput == null || hourlyRateInput == null
                || regularHoursInput == null || overtimeMultiplierInput == null || allowanceInput == null || deductionInput == null) return;
        try {
            LocalTime start = LocalTime.parse(value(timeInInput), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime end = LocalTime.parse(value(timeOutInput), DateTimeFormatter.ofPattern("HH:mm"));
            int breaks = 0;
            for (BreakInput item : breakInputs) {
                String raw = value(item.hours);
                if (!raw.isEmpty()) breaks = Math.addExact(breaks, new BigDecimal(raw).multiply(BigDecimal.valueOf(60)).intValueExact());
            }
            if (hoursOnlyMode != null && hoursOnlyMode.isChecked()) {
                String raw = value(totalHoursInput);
                if (raw.isEmpty()) { resultView.setText("Enter total worked hours. Break duration is added to time out."); return; }
                int netMinutes = new BigDecimal(raw).multiply(BigDecimal.valueOf(60)).intValueExact();
                end = start.plusMinutes((long) netMinutes + breaks);
                if (!value(timeOutInput).equals(end.toString())) timeOutInput.setText(end.toString());
            }
            BigDecimal rate = decimal(hourlyRateInput, "Pay rate");
            BigDecimal regularHours = decimal(regularHoursInput, "Regular hours");
            if (regularHours.signum() <= 0 || regularHours.multiply(BigDecimal.valueOf(60)).stripTrailingZeros().scale() > 0) return;
            String rateType = payRateTypeInput == null ? "Hourly" : payRateTypeInput.getText().toString();
            BigDecimal hourly = "Daily".equals(rateType) ? rate.divide(regularHours, 8, java.math.RoundingMode.HALF_UP) : rate;
            SalaryCalculator.Result result = SalaryCalculator.calculateWithBreakMinutes(start, end, breaks,
                    regularHours.multiply(BigDecimal.valueOf(60)).intValueExact(), hourly,
                    decimal(overtimeMultiplierInput, "OT multiplier"), decimal(allowanceInput, "Allowance"), decimal(deductionInput, "Deduction"));
            resultView.setText("LIVE PAY PREVIEW (not saved)\n\nShift     " + duration(result.shiftMinutes)
                    + "\nBreak     " + duration(result.breakMinutes) + "\nNet work  " + duration(result.netWorkMinutes)
                    + "\nRegular   " + duration(result.regularMinutes) + "\nOvertime  " + duration(result.overtimeMinutes)
                    + "\n\nRegular pay   " + money(result.regularPay) + "\nOT pay        " + money(result.overtimePay)
                    + "\nAllowance     " + money(result.allowance) + "\nDeduction     " + money(result.deduction)
                    + "\nGross pay     " + money(result.grossPay) + "\nEstimated net " + money(result.estimatedNetPay));
        } catch (Exception ignored) {
            // Partial values are normal while the user types; save performs strict validation.
        }
    }

    private void calculateAndSave() {
        lastSaveSucceeded = false;
        try {
            LocalDate date = LocalDate.parse(value(dateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime timeIn = LocalTime.parse(value(timeInInput), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime timeOut = LocalTime.parse(value(timeOutInput), DateTimeFormatter.ofPattern("HH:mm"));
            BigDecimal enteredRate = decimal(hourlyRateInput, "Pay rate");
            String rateType = payRateTypeInput == null ? pref("pay_rate_type", "Hourly") : payRateTypeInput.getText().toString();
            BigDecimal regularHours = decimal(regularHoursInput, "Regular hours");
            BigDecimal multiplier = decimal(overtimeMultiplierInput, "OT multiplier");
            BigDecimal allowance = decimal(allowanceInput, "Allowance");
            BigDecimal deduction = decimal(deductionInput, "Deduction");
            if (regularHours.signum() <= 0 || regularHours.multiply(BigDecimal.valueOf(60)).stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Regular hours must be positive and convert to whole minutes.");
            }
            int regularMinutes = regularHours.multiply(BigDecimal.valueOf(60)).intValueExact();
            int totalBreakMinutes = 0;
            List<String> serializedBreaks = new ArrayList<>();
            for (BreakInput item : breakInputs) {
                String rawHours = value(item.hours);
                if (rawHours.isEmpty()) continue;
                BigDecimal breakHours = new BigDecimal(rawHours);
                if (breakHours.signum() < 0) throw new IllegalArgumentException("Break duration cannot be negative.");
                int minutes = breakHours.multiply(BigDecimal.valueOf(60)).intValueExact();
                totalBreakMinutes = Math.addExact(totalBreakMinutes, minutes);
                if (minutes > 0) serializedBreaks.add(breakHours.stripTrailingZeros().toPlainString() + "h");
            }
            if (hoursOnlyMode != null && hoursOnlyMode.isChecked()) {
                BigDecimal totalHours = decimal(totalHoursInput, "Total worked hours");
                if (totalHours.signum() <= 0 || totalHours.multiply(BigDecimal.valueOf(60)).stripTrailingZeros().scale() > 0) {
                    throw new IllegalArgumentException("Total worked hours must be positive and convert to whole minutes.");
                }
                int netMinutes = totalHours.multiply(BigDecimal.valueOf(60)).intValueExact();
                if (netMinutes + totalBreakMinutes > 24 * 60) {
                    throw new IllegalArgumentException("Worked hours plus breaks cannot exceed 24 hours.");
                }
                timeOut = timeIn.plusMinutes((long) netMinutes + totalBreakMinutes);
                timeOutInput.setText(timeOut.toString());
            }
            BigDecimal effectiveHourlyRate = "Daily".equals(rateType)
                    ? enteredRate.divide(regularHours, 8, java.math.RoundingMode.HALF_UP) : enteredRate;
            SalaryCalculator.Result result = SalaryCalculator.calculateWithBreakMinutes(timeIn, timeOut,
                    totalBreakMinutes, regularMinutes, effectiveHourlyRate, multiplier, allowance, deduction);
            boolean updatingEntry = editingWorkEntryId >= 0;
            boolean duplicate = updatingEntry
                    ? database.hasEntryForShiftExceptId(date.toString(), timeIn.toString(), timeOut.toString(), editingWorkEntryId)
                    : database.hasEntryForShift(date.toString(), timeIn.toString(), timeOut.toString());
            if (duplicate) throw new IllegalArgumentException("A shift with this date, time in, and time out is already saved.");
            String savedBreaks = serializedBreaks.isEmpty() ? "0h" : android.text.TextUtils.join(", ", serializedBreaks);
            if (updatingEntry) {
                if (!database.updateEntry(editingWorkEntryId, date.toString(), timeIn.toString(), timeOut.toString(),
                        savedBreaks, result, effectiveHourlyRate, rateType, enteredRate, multiplier, regularMinutes)) {
                    throw new IllegalArgumentException("This saved shift no longer exists.");
                }
                editingWorkEntryId = -1;
                if (saveWorkButton != null) saveWorkButton.setText("Calculate and save shift");
            } else {
                saveSalaryPreferences(enteredRate, rateType, regularHours, multiplier, allowance, deduction);
                database.insertEntry(date.toString(), timeIn.toString(), timeOut.toString(),
                        savedBreaks, result, effectiveHourlyRate, rateType, enteredRate, multiplier, regularMinutes);
            }
            resultView.setText("PAY BREAKDOWN (" + rateType.toUpperCase(Locale.ROOT) + " RATE)\n\nShift     " + duration(result.shiftMinutes)
                    + "\nBreak     " + duration(result.breakMinutes)
                    + "\nNet work  " + duration(result.netWorkMinutes)
                    + "\nRegular   " + duration(result.regularMinutes)
                    + "\nOvertime  " + duration(result.overtimeMinutes)
                    + "\n\nRegular pay   " + money(result.regularPay)
                    + "\nOT pay        " + money(result.overtimePay)
                    + "\nAllowance     " + money(result.allowance)
                    + "\nDeduction     " + money(result.deduction)
                    + "\nGross pay     " + money(result.grossPay)
                    + "\nNet pay      " + money(result.estimatedNetPay));
            refreshHistory();
            refreshBudget();
            Toast.makeText(this, updatingEntry ? "Shift updated" : "Shift saved", Toast.LENGTH_SHORT).show();
            lastSaveSucceeded = true;
        } catch (DateTimeParseException ex) {
            toast("Check date format (YYYY-MM-DD) and time format (HH:mm).");
        } catch (ArithmeticException ex) {
            toast("Regular hours and break durations must convert to whole minutes.");
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        } catch (Exception ex) {
            toast("Could not save shift: " + ex.getMessage());
        }
    }

    private void addExpense() {
        lastSaveSucceeded = false;
        try {
            LocalDate date = LocalDate.parse(value(expenseDateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            String category = value(expenseCategoryInput);
            if (category.isEmpty()) throw new IllegalArgumentException("Expense category is required.");
            BigDecimal amount = decimal(expenseAmountInput, "Expense amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
            boolean updatingExpense = editingExpenseId >= 0;
            BigDecimal availableBefore = getAvailableCashBalance();
            BigDecimal savingsBefore = database.getTotalSavingsBalance();
            if (updatingExpense) {
                BigDecimal oldAmount = database.getExpenseAmount(editingExpenseId);
                BigDecimal oldDraw = database.getExpenseSavingsDraw(editingExpenseId);
                availableBefore = availableBefore.add(oldAmount).subtract(oldDraw);
                savingsBefore = savingsBefore.add(oldDraw);
            }
            BigDecimal savingsDraw = BudgetInsights.savingsDrawForExpense(availableBefore, savingsBefore, amount);
            if (updatingExpense) {
                if (!database.updateExpense(editingExpenseId, date.toString(), category, value(expenseNoteInput), amount, savingsDraw)) {
                    throw new IllegalArgumentException("This saved expense no longer exists.");
                }
                editingExpenseId = -1;
                if (saveExpenseButton != null) saveExpenseButton.setText("Save expense");
            } else {
                database.insertExpense(date.toString(), category, value(expenseNoteInput), amount, savingsDraw);
            }
            if (!updatingExpense) {
                expenseAmountInput.setText("");
                expenseNoteInput.setText("");
            }
            refreshBudget();
            refreshExpenseHistory();
            BigDecimal availableAfter = availableBefore.subtract(amount).add(savingsDraw);
            String saveMessage = updatingExpense ? "Expense updated" : "Expense saved";
            if (availableAfter.signum() <= 0) {
                saveMessage = savingsDraw.signum() > 0
                        ? "Warning: savings used " + money(savingsDraw) + " to cover overspending."
                        : "Warning: available to spend is exhausted; spending exceeds available cash.";
            }
            Toast.makeText(this, saveMessage, Toast.LENGTH_LONG).show();
            lastSaveSucceeded = true;
        } catch (DateTimeParseException ex) {
            toast("Use YYYY-MM-DD for the expense date.");
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        } catch (Exception ex) {
            toast("Could not save expense: " + ex.getMessage());
        }
    }

    private void refreshHistory() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews();
        openSwipeContent = null;

        Map<String, DayHistory> days = new TreeMap<>(java.util.Collections.reverseOrder());
        List<WorkDatabase.WorkEntry> workEntries = database.getRecentWorkEntries(100);
        List<WorkDatabase.ExpenseEntry> expenseEntries = database.getRecentExpenseEntries(100);

        BigDecimal expenseTotal = BigDecimal.ZERO;
        for (WorkDatabase.WorkEntry entry : workEntries) {
            DayHistory day = days.get(entry.date);
            if (day == null) {
                day = new DayHistory(entry.date);
                days.put(entry.date, day);
            }
            day.workEntries.add(entry);
        }
        for (WorkDatabase.ExpenseEntry entry : expenseEntries) {
            DayHistory day = days.get(entry.date);
            if (day == null) {
                day = new DayHistory(entry.date);
                days.put(entry.date, day);
            }
            day.expenses.add(entry);
            expenseTotal = expenseTotal.add(new BigDecimal(entry.amount));
        }

        LocalDate today = LocalDate.now();
        CutoffPeriod displayCutoff = getLatestPaydayCutoff(today);
        LocalDate cutoffStart = displayCutoff.startDate;
        LocalDate cutoffEnd = displayCutoff.endDate;
        BigDecimal expectedSalary = database.getRecordedNetPayTotal(cutoffStart.toString(), cutoffEnd.toString());

        LinearLayout salaryCard = cardContainer();
        salaryCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView salaryLabel = text("EXPECTED SALARY · UPCOMING PAYDAY CUTOFF", 11, true);
        salaryLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        salaryCard.addView(salaryLabel);
        TextView salaryAmount = text(money(expectedSalary), 27, true);
        salaryAmount.setPadding(0, dp(5), 0, dp(3));
        salaryAmount.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        salaryCard.addView(salaryAmount);
        TextView salaryDates = text(cutoffStart + " to " + cutoffEnd
                + " · Based on saved work shifts · Advances on configured payday", 12, false);
        salaryDates.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        salaryCard.addView(salaryDates);
        historyContainer.addView(salaryCard);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button addShiftButton = button("＋ Add work shift", true);
        addShiftButton.setTextSize(13);
        addShiftButton.setOnClickListener(v -> showWorkDialog(null));
        Button addExpenseButton = button("＋ Add expenses", false);
        addExpenseButton.setTextSize(13);
        addExpenseButton.setOnClickListener(v -> showExpenseDialog(null));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        actionParams.rightMargin = dp(6);
        actions.addView(addShiftButton, actionParams);
        LinearLayout.LayoutParams expenseParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        expenseParams.leftMargin = dp(6);
        actions.addView(addExpenseButton, expenseParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
        actionsParams.bottomMargin = dp(10);
        historyContainer.addView(actions, actionsParams);

        List<WorkDatabase.CashBalanceAdjustment> cashAdjustments =
                database.getRecentCashBalanceAdjustments(10);
        if (!cashAdjustments.isEmpty()) {
            addSection(historyContainer, "Cash balance adjustments");
            TextView adjustmentHint = text("Reconciliation records are kept separately and do not change past income or expense entries.", 12, false);
            adjustmentHint.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            historyContainer.addView(adjustmentHint);
            for (WorkDatabase.CashBalanceAdjustment adjustment : cashAdjustments) {
                LinearLayout adjustmentCard = cardContainer();
                adjustmentCard.setPadding(dp(14), dp(12), dp(14), dp(12));
                TextView adjustmentTitle = text(adjustment.date + " · " + money(new BigDecimal(adjustment.targetAvailableBalance))
                        + " actual cash", 14, true);
                adjustmentCard.addView(adjustmentTitle);
                BigDecimal delta = new BigDecimal(adjustment.amount);
                TextView adjustmentDelta = text("Adjustment: " + (delta.signum() > 0 ? "+" : "") + money(delta), 13, true);
                adjustmentDelta.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
                adjustmentCard.addView(adjustmentDelta);
                if (adjustment.note != null && !adjustment.note.trim().isEmpty()) {
                    TextView adjustmentNote = text(adjustment.note, 12, false);
                    adjustmentNote.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
                    adjustmentCard.addView(adjustmentNote);
                }
                historyContainer.addView(adjustmentCard);
            }
        }

        if (days.isEmpty()) {
            if (cashAdjustments.isEmpty()) {
                historyContainer.addView(emptyState("No activity yet",
                        "Add a work shift or expense to start tracking your activity."));
            }
            return;
        }

        LinearLayout overview = cardContainer();
        overview.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView overviewTitle = text("ACTIVITY OVERVIEW", 11, true);
        overviewTitle.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        overview.addView(overviewTitle);
        LinearLayout overviewValues = new LinearLayout(this);
        overviewValues.setOrientation(LinearLayout.HORIZONTAL);
        overviewValues.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout shiftMetric = new LinearLayout(this);
        shiftMetric.setOrientation(LinearLayout.VERTICAL);
        TextView shiftCount = text(String.valueOf(workEntries.size()), 22, true);
        shiftCount.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        shiftMetric.addView(shiftCount);
        TextView shiftCaption = text("Work shifts", 12, false);
        shiftCaption.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        shiftMetric.addView(shiftCaption);
        overviewValues.addView(shiftMetric, new LinearLayout.LayoutParams(0, -2, 1f));
        View overviewDivider = new View(this);
        overviewDivider.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorOutlineVariant));
        overviewValues.addView(overviewDivider, new LinearLayout.LayoutParams(dp(1), dp(34)));
        LinearLayout expenseMetric = new LinearLayout(this);
        expenseMetric.setOrientation(LinearLayout.VERTICAL);
        expenseMetric.setPadding(dp(16), 0, 0, 0);
        TextView expenseValue = text(money(expenseTotal), 22, true);
        expenseValue.setTextColor(resolveColor(android.R.attr.colorError));
        expenseMetric.addView(expenseValue);
        TextView expenseCaption = text("Total from " + expenseEntries.size() + " expenses", 12, false);
        expenseCaption.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        expenseMetric.addView(expenseCaption);
        overviewValues.addView(expenseMetric, new LinearLayout.LayoutParams(0, -2, 1f));
        View overviewSpacer = new View(this);
        overview.addView(overviewSpacer, new LinearLayout.LayoutParams(1, dp(10)));
        overview.addView(overviewValues);
        historyContainer.addView(overview);

        TextView recentHeading = text("ACTIVITY BY DATE", 18, true);
        recentHeading.setPadding(0, dp(8), 0, dp(10));
        historyContainer.addView(recentHeading);

        int shownDays = 0;
        for (DayHistory day : days.values()) {
            if (shownDays++ >= 30) break;
            LinearLayout dayCard = cardContainer();
            dayCard.setPadding(dp(14), dp(12), dp(14), dp(12));

            String prettyDate;
            try {
                prettyDate = LocalDate.parse(day.date).format(
                        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", java.util.Locale.getDefault()));
            } catch (Exception ignored) {
                prettyDate = day.date;
            }
            LinearLayout dateHeader = new LinearLayout(this);
            dateHeader.setOrientation(LinearLayout.HORIZONTAL);
            dateHeader.setGravity(Gravity.CENTER_VERTICAL);
            TextView dateTitle = text(prettyDate, 14, true);
            dateTitle.setTextColor(resolveColor(android.R.attr.textColorPrimary));
            dateHeader.addView(dateTitle, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView itemCount = text((day.workEntries.size() + day.expenses.size()) + " items", 11, true);
            itemCount.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            dateHeader.addView(itemCount);
            dayCard.addView(dateHeader);
            LinearLayout dayActivityList = new LinearLayout(this);
            dayActivityList.setOrientation(LinearLayout.VERTICAL);
            dayActivityList.setVisibility(View.GONE);
            dayActivityList.setTag(false);
            dayCard.addView(dayActivityList);
            dateHeader.setOnClickListener(v -> {
                boolean expanded = Boolean.TRUE.equals(dayActivityList.getTag());
                dayActivityList.setTag(!expanded);
                dayActivityList.setVisibility(expanded ? View.GONE : View.VISIBLE);
                itemCount.setText((day.workEntries.size() + day.expenses.size()) + " ITEMS  " + (expanded ? "⌄" : "⌃"));
            });
            itemCount.setText((day.workEntries.size() + day.expenses.size()) + " ITEMS  ⌄");

            View dateDivider = new View(this);
            dateDivider.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorOutlineVariant));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.topMargin = dp(10);
            dividerParams.bottomMargin = dp(5);
            dayCard.addView(dateDivider, dividerParams);

            for (WorkDatabase.WorkEntry entry : day.workEntries) {
                LinearLayout recordContent = new LinearLayout(this);
                recordContent.setOrientation(LinearLayout.VERTICAL);
                LinearLayout summary = new LinearLayout(this);
                summary.setOrientation(LinearLayout.HORIZONTAL);
                summary.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout summaryText = new LinearLayout(this);
                summaryText.setOrientation(LinearLayout.VERTICAL);
                TextView type = text("WORK SHIFT", 10, true);
                type.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
                summaryText.addView(type);
                TextView shiftTime = text(entry.timeIn + " – " + entry.timeOut, 15, true);
                shiftTime.setPadding(0, dp(2), 0, 0);
                summaryText.addView(shiftTime);
                summary.addView(summaryText, new LinearLayout.LayoutParams(0, -2, 1f));
                TextView pay = text(money(new BigDecimal(entry.netPay)), 14, true);
                pay.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
                summary.addView(pay);
                recordContent.addView(summary);

                LinearLayout details = new LinearLayout(this);
                details.setOrientation(LinearLayout.VERTICAL);
                details.setVisibility(View.VISIBLE);
                details.setPadding(0, dp(8), 0, 0);
                TextView durationLine = text("Worked: " + duration(entry.netMinutes)
                        + "  ·  Break: " + duration(entry.breakMinutes), 12, false);
                durationLine.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
                details.addView(durationLine);
                if (entry.overtimeMinutes > 0) {
                    TextView overtime = text("Overtime: " + duration(entry.overtimeMinutes), 12, true);
                    overtime.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
                    details.addView(overtime);
                }
                details.addView(text("Estimated net pay: " + money(new BigDecimal(entry.netPay)), 13, true));
                recordContent.addView(details);
                recordContent.setContentDescription("Work shift " + entry.timeIn + " to " + entry.timeOut + ".");
                addSwipeReveal(dayActivityList, recordContent, "Edit shift", "Delete shift",
                        v -> editWorkEntry(entry), v -> confirmDeleteWorkEntry(entry));
            }

            if (!day.expenses.isEmpty()) {
                TextView expensesHeading = text("EXPENSES  ·  " + day.expenses.size() + " entries", 11, true);
                expensesHeading.setTextColor(resolveColor(android.R.attr.colorError));
                expensesHeading.setPadding(dp(4), dp(12), 0, dp(4));
                dayActivityList.addView(expensesHeading);
            }

            for (WorkDatabase.ExpenseEntry entry : day.expenses) {
                LinearLayout recordContent = new LinearLayout(this);
                recordContent.setOrientation(LinearLayout.VERTICAL);
                LinearLayout summary = new LinearLayout(this);
                summary.setOrientation(LinearLayout.HORIZONTAL);
                summary.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout summaryText = new LinearLayout(this);
                summaryText.setOrientation(LinearLayout.VERTICAL);
                TextView expenseLine = text(entry.category + "  ·  " + money(new BigDecimal(entry.amount)), 14, true);
                expenseLine.setPadding(0, dp(2), 0, 0);
                summaryText.addView(expenseLine);
                summary.addView(summaryText, new LinearLayout.LayoutParams(0, -2, 1f));
                recordContent.addView(summary);

                LinearLayout details = new LinearLayout(this);
                details.setOrientation(LinearLayout.VERTICAL);
                details.setVisibility(View.GONE);
                details.setPadding(0, dp(8), 0, 0);
                TextView dateDetail = text("Date: " + entry.date, 12, false);
                dateDetail.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
                details.addView(dateDetail);
                if (!entry.note.isEmpty()) {
                    TextView note = text("Note: " + entry.note, 12, false);
                    note.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
                    details.addView(note);
                }
                recordContent.addView(details);
                recordContent.setContentDescription("Expense " + entry.category + ".");
                addSwipeReveal(dayActivityList, recordContent, "Edit expense", "Delete expense",
                        v -> editExpense(entry), v -> confirmDeleteExpense(entry));
            }
            historyContainer.addView(dayCard);
        }
    }

    private void addSwipeReveal(LinearLayout parent, LinearLayout recordContent, String editLabel,
                                String deleteLabel, View.OnClickListener editAction,
                                View.OnClickListener deleteAction) {
        int revealWidth = dp(152);
        FrameLayout row = new FrameLayout(this);
        row.setClipChildren(true);
        row.setClipToPadding(true);
        row.setMinimumHeight(dp(64));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        actions.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(
                revealWidth, -1, Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(actions, actionParams);

        MaterialButton edit = button("Edit", false);
        edit.setTextSize(12);
        edit.setAllCaps(true);
        edit.setTypeface(edit.getTypeface(), android.graphics.Typeface.BOLD);
        edit.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        edit.setPadding(0, 0, 0, 0);
        edit.setMinWidth(0);
        edit.setMinimumWidth(0);
        edit.setMinimumHeight(0);
        edit.setCornerRadius(dp(12));
        edit.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        edit.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        edit.setRippleColor(ColorStateList.valueOf(
                withAlpha(resolveColor(androidx.appcompat.R.attr.colorPrimary), 0x33)));
        edit.setInsetTop(0);
        edit.setInsetBottom(0);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(revealWidth / 2 - dp(8), dp(44));
        editParams.setMargins(dp(4), 0, dp(2), 0);
        edit.setLayoutParams(editParams);
        edit.setOnClickListener(v -> {
            closeSwipeContent();
            editAction.onClick(v);
        });

        MaterialButton delete = button("Delete", false);
        delete.setTextSize(12);
        delete.setAllCaps(true);
        delete.setTypeface(delete.getTypeface(), android.graphics.Typeface.BOLD);
        delete.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnErrorContainer));
        delete.setPadding(0, 0, 0, 0);
        delete.setMinWidth(0);
        delete.setMinimumWidth(0);
        delete.setMinimumHeight(0);
        delete.setCornerRadius(dp(12));
        delete.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        delete.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        delete.setRippleColor(ColorStateList.valueOf(
                withAlpha(resolveColor(android.R.attr.colorError), 0x33)));
        delete.setInsetTop(0);
        delete.setInsetBottom(0);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(revealWidth / 2 - dp(8), dp(44));
        deleteParams.setMargins(dp(2), 0, dp(4), 0);
        delete.setLayoutParams(deleteParams);
        delete.setOnClickListener(v -> {
            closeSwipeContent();
            deleteAction.onClick(v);
        });
        actions.addView(edit);
        actions.addView(delete);

        recordContent.setPadding(dp(10), dp(8), dp(10), dp(8));
        recordContent.setMinimumHeight(dp(56));
        GradientDrawable recordBackground = new GradientDrawable();
        recordBackground.setColor(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant));
        recordBackground.setCornerRadius(dp(10));
        recordContent.setBackground(recordBackground);
        recordContent.setClickable(true);
        recordContent.setElevation(dp(2));
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(-1, -2, Gravity.START | Gravity.TOP);
        row.addView(recordContent, contentParams);

        int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        recordContent.setOnTouchListener(new View.OnTouchListener() {
            float downX;
            float downY;
            float startTranslation;
            boolean horizontalGesture;
            boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        startTranslation = recordContent.getTranslationX();
                        horizontalGesture = false;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (!horizontalGesture && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                            horizontalGesture = true;
                            moved = true;
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                            if (openSwipeContent != null && openSwipeContent != recordContent) {
                                openSwipeContent.setTranslationX(0f);
                            }
                            openSwipeContent = recordContent;
                        }
                        if (horizontalGesture) {
                            float translation = Math.max(-revealWidth, Math.min(0f, startTranslation + dx));
                            recordContent.setTranslationX(translation);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (horizontalGesture) {
                            view.getParent().requestDisallowInterceptTouchEvent(false);
                            if (recordContent.getTranslationX() <= -revealWidth / 2f) {
                                recordContent.animate().translationX(-revealWidth).setDuration(160).start();
                                openSwipeContent = recordContent;
                            } else {
                                recordContent.animate().translationX(0f).setDuration(160).start();
                                if (openSwipeContent == recordContent) openSwipeContent = null;
                            }
                        } else if (!moved && recordContent.getTranslationX() < 0f) {
                            closeSwipeContent();
                        } else if (!moved) {
                            View details = recordContent.getTag() instanceof View
                                    ? (View) recordContent.getTag() : null;
                            if (details != null) {
                                boolean expanding = details.getVisibility() != View.VISIBLE;
                                details.setVisibility(expanding ? View.VISIBLE : View.GONE);
                                if (recordContent.getChildAt(0) instanceof LinearLayout) {
                                    LinearLayout summary = (LinearLayout) recordContent.getChildAt(0);
                                    View last = summary.getChildAt(summary.getChildCount() - 1);
                                    if (last instanceof TextView) ((TextView) last).setText(expanding ? "⌃" : "⌄");
                                }
                                recordContent.requestLayout();
                            }
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(2);
        rowParams.bottomMargin = dp(6);
        parent.addView(row, rowParams);
    }

    private void closeSwipeContent() {
        if (openSwipeContent != null) {
            openSwipeContent.animate().translationX(0f).setDuration(160).start();
            openSwipeContent = null;
        }
    }

    private void refreshExpenseHistory() {
        refreshHistory();
    }

    private static final class DayHistory {
        final String date;
        final List<WorkDatabase.WorkEntry> workEntries = new ArrayList<>();
        final List<WorkDatabase.ExpenseEntry> expenses = new ArrayList<>();

        DayHistory(String date) {
            this.date = date;
        }
    }

    private void editWorkEntry(WorkDatabase.WorkEntry entry) {
        showWorkDialog(entry);
    }

    private void confirmDeleteWorkEntry(WorkDatabase.WorkEntry entry) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete work shift?")
                .setMessage(entry.date + " • " + entry.timeIn + "–" + entry.timeOut
                        + "\nRecorded pay: ₱" + entry.netPay + "\nThis cannot be undone.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (database.deleteEntry(entry.id)) {
                        Toast.makeText(this, "Shift deleted", Toast.LENGTH_SHORT).show();
                        showTab(currentTab);
                    } else toast("Could not delete this shift.");
                }).show();
    }

    private void editExpense(WorkDatabase.ExpenseEntry entry) {
        showExpenseDialog(entry);
    }

    private void confirmDeleteExpense(WorkDatabase.ExpenseEntry entry) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete expense?")
                .setMessage(entry.date + " • " + entry.category + " • ₱" + entry.amount
                        + "\nThis cannot be undone.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (database.deleteExpense(entry.id)) {
                        Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                        showTab(currentTab);
                    } else toast("Could not delete this expense.");
                }).show();
    }

    private void refreshBudget() {
        if (budgetSummaryView == null) return;

        LocalDate today = LocalDate.now();
        int firstPayday = parsePaydayPreference("first_cutoff_payday", 22);
        int secondPayday = parsePaydayPreference("second_cutoff_payday", 7);
        CutoffPeriod cutoff = CutoffPeriod.forLatestPaydayOnOrBefore(
                today, firstPayday, secondPayday);
        LocalDate cycleStart = cutoff.payday(firstPayday, secondPayday);
        LocalDate payday = CutoffPeriod.forNextPayday(today, firstPayday, secondPayday)
                .payday(firstPayday, secondPayday);
        WorkDatabase.PaydayPayment latestPayment =
                database.getLatestPaydayPaymentOnOrBefore(today.toString());
        WorkDatabase.PaydayPayment activePayment = latestPayment != null
                && !LocalDate.parse(latestPayment.paydayDate).isBefore(cycleStart) ? latestPayment : null;
        long daysUntilPayday = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, payday));
        BigDecimal spent = database.getExpenseTotal(cycleStart.toString(), today.toString());
        BigDecimal totalCash = getTotalCashBalance();
        BigDecimal savings = database.getTotalSavingsBalance();
        BigDecimal remaining = totalCash.subtract(savings).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal cycleBudget = activePayment == null ? BigDecimal.ZERO
                : new BigDecimal(activePayment.receivedAmount).add(getCarryoverIntoPayment(activePayment));
        BigDecimal budgetBase;

        BigDecimal spendingLimit;
        try {
            spendingLimit = new BigDecimal(pref("spending_budget", "0.00"));
            if (spendingLimit.signum() < 0) spendingLimit = BigDecimal.ZERO;
        } catch (NumberFormatException ignored) {
            spendingLimit = BigDecimal.ZERO;
        }

        budgetBase = BudgetInsights.resolveBudgetBase(cycleBudget, spendingLimit);
        StringBuilder summary = new StringBuilder();
        summary.append("Cycle started: ").append(cycleStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                .append("\nPayday in ").append(daysUntilPayday)
                .append(daysUntilPayday == 1 ? " day" : " days")
                .append("\nAvailable to spend: ").append(money(remaining))
                .append("\nSavings balance: ").append(money(savings));
        if (remaining.signum() <= 0) {
            summary.append("\nWARNING: Available to spend is exhausted.");
            if (savings.signum() <= 0) summary.append(" No savings remain to cover extra expenses.");
        }

        BigDecimal dailyGuide = BudgetInsights.dailySpendingGuide(
                budgetBase, spent, remaining, daysUntilPayday);
        boolean hasDailyGuide = BudgetInsights.hasDailyGuide(
                budgetBase, remaining, daysUntilPayday);

        summary.append("\n");
        if (hasDailyGuide) {
            summary.append("Daily spending guide: ").append(money(dailyGuide))
                    .append("\nUse this as a guide, not a guarantee; account for bills due before payday.");
        } else if (daysUntilPayday == 0) {
            summary.append("Payday is today. Review any expenses due before your next pay.");
        } else {
            summary.append("No daily guide yet. Add your spending limit in Settings, or record your income.");
        }

        if (budgetBase.signum() > 0) {
            int usedPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(budgetBase, 0, java.math.RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO).intValue();
            budgetProgressBar.setVisibility(View.VISIBLE);
            budgetProgressBar.setProgress(Math.min(100, usedPercent));

            BigDecimal budgetRemaining = budgetBase.subtract(spent);
            BigDecimal actualPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(budgetBase, 1, java.math.RoundingMode.HALF_UP);
            summary.append("\nSpent this cycle: ").append(money(spent))
                    .append(" of ").append(money(budgetBase))
                    .append(" (").append(actualPercent.toPlainString()).append("% used)")
                    .append("\nBudget left: ").append(money(budgetRemaining.max(BigDecimal.ZERO)));

            BudgetInsights.Status status = BudgetInsights.classify(spent, budgetBase);
            if (status == BudgetInsights.Status.OVER_BUDGET) {
                summary.append("\nOver budget by ").append(money(spent.subtract(budgetBase)))
                        .append(". Review non-essential spending and upcoming needs.");
                budgetSummaryView.setTextColor(resolveColor(android.R.attr.colorError));
                budgetProgressBar.setProgressTintList(ColorStateList.valueOf(
                        resolveColor(android.R.attr.colorError)));
            } else if (status == BudgetInsights.Status.WARNING) {
                summary.append("\nMost of this cycle’s budget is used. Check upcoming bills.");
                budgetSummaryView.setTextColor(resolveColor(android.R.attr.colorError));
                budgetProgressBar.setProgressTintList(ColorStateList.valueOf(
                        resolveColor(android.R.attr.colorError)));
            } else {
                summary.append("\nYour spending is within the cycle budget so far.");
                budgetSummaryView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
                budgetProgressBar.setProgressTintList(ColorStateList.valueOf(
                        resolveColor(androidx.appcompat.R.attr.colorPrimary)));
            }
        } else {
            budgetProgressBar.setVisibility(View.GONE);
            summary.append("\nRecord a salary to track a salary-cycle budget, or set a spending limit in Settings.");
            budgetSummaryView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        }

        Map<String, BigDecimal> categoryTotals =
                database.getExpenseTotalsByCategory(cycleStart.toString(), today.toString());
        if (categoryTotals.isEmpty()) {
            summary.append("\nNo expenses recorded for this cutoff yet.");
        } else {
            Map.Entry<String, BigDecimal> topCategory = categoryTotals.entrySet().iterator().next();
            summary.append("\nTop category: ").append(topCategory.getKey())
                    .append(" · ").append(money(topCategory.getValue()));
        }
        budgetSummaryView.setText(summary.toString());
    }


    private void showReconcileCashDialog(BigDecimal currentAvailableCash) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView current = text("Calculated available cash: " + money(currentAvailableCash), 14, true);
        current.setPadding(0, 0, 0, dp(8));
        form.addView(current);
        TextView explanation = text(
                "Enter the actual cash you currently have. The app will save the difference as a separate adjustment record; existing income, expenses, and savings records will remain unchanged.",
                13, false);
        explanation.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        explanation.setPadding(0, 0, 0, dp(10));
        form.addView(explanation);

        EditText actualCashInput = field("Actual available cash", currentAvailableCash.max(BigDecimal.ZERO)
                        .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(form, "Actual available cash (₱)", actualCashInput);

        EditText noteInput = field("Optional reason (e.g. cash not recorded)", "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        addField(form, "Note (optional)", noteInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reconcile available cash")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save adjustment", (dialog, which) -> {
                    try {
                        BigDecimal actual = decimal(actualCashInput, "Actual available cash")
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        if (actual.signum() < 0) {
                            toast("Actual available cash cannot be negative.");
                            return;
                        }
                        BigDecimal currentBalance = getAvailableCashBalance();
                        BigDecimal difference = actual.subtract(currentBalance)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        boolean saved = database.addCashBalanceAdjustment(LocalDate.now().toString(),
                                difference, actual, value(noteInput));
                        if (!saved) {
                            toast("Could not save the cash adjustment. Please try again.");
                            return;
                        }
                        toast("Cash reconciled. Adjustment: " + (difference.signum() > 0 ? "+" : "")
                                + money(difference));
                        showTab(0);
                    } catch (IllegalArgumentException ex) {
                        toast(ex.getMessage());
                    }
                })
                .show();
    }

    private BigDecimal getTotalCashBalance() {
        BigDecimal opening;
        try {
            opening = new BigDecimal(pref("opening_cash_balance", "0.00"));
        } catch (NumberFormatException ignored) {
            opening = BigDecimal.ZERO;
        }
        String today = LocalDate.now().toString();
        return opening.add(database.getTotalReceivedPayOnOrBefore(today))
                .subtract(database.getAllExpensesTotalOnOrBefore(today))
                .add(database.getCashBalanceAdjustmentTotalOnOrBefore(today))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal getAvailableCashBalance() {
        return getTotalCashBalance().subtract(database.getTotalSavingsBalance())
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal getCarryoverIntoPayment(WorkDatabase.PaydayPayment payment) {
        WorkDatabase.PaydayPayment previous = database.getPreviousPaydayPayment(
                payment.paydayDate, payment.cutoffStart, payment.cutoffEnd);
        if (previous == null) return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        WorkDatabase.BudgetCycleAllocation allocation =
                database.getBudgetCycleAllocation(previous.cutoffStart, previous.cutoffEnd);
        return allocation == null ? BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP)
                : new BigDecimal(allocation.carryoverAmount).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void maybePromptForSavingsAllocation() {
        WorkDatabase.BudgetCyclePending pending =
                database.getPendingBudgetCycleAllocation(LocalDate.now().toString());
        if (pending == null || pending.nextPaydayDate == null) return;
        BigDecimal cycleBudget = new BigDecimal(pending.payment.receivedAmount)
                .add(getCarryoverIntoPayment(pending.payment));
        BigDecimal expenses = database.getExpenseTotalBetweenPaydays(
                pending.payment.paydayDate, pending.nextPaydayDate);
        BigDecimal leftover = cycleBudget.subtract(expenses)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        if (leftover.signum() <= 0) {
            WorkDatabase.BudgetCycleAllocation existing =
                    database.getBudgetCycleAllocation(pending.payment.cutoffStart, pending.payment.cutoffEnd);
            BigDecimal preservedSavings = existing == null
                    ? BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP)
                    : new BigDecimal(existing.savingsAmount).setScale(2, java.math.RoundingMode.HALF_UP);
            database.saveBudgetCycleAllocation(pending.payment.cutoffStart, pending.payment.cutoffEnd,
                    preservedSavings, BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP));
            return;
        }
        showSavingsAllocationDialog(pending, leftover);
    }

    private void showSavingsAllocationDialog(WorkDatabase.BudgetCyclePending pending, BigDecimal leftover) {
        String amount = money(leftover);
        new MaterialAlertDialogBuilder(this)
                .setTitle("What should happen to your leftover budget?")
                .setMessage("Your previous salary cycle has " + amount + " left. Choose how to allocate it. Your salary and expense history will stay saved.")
                .setPositiveButton("Move to savings", (dialog, which) ->
                        saveBudgetAllocation(pending, leftover, BigDecimal.ZERO))
                .setNegativeButton("Carry over", (dialog, which) ->
                        saveBudgetAllocation(pending, BigDecimal.ZERO, leftover))
                .setNeutralButton("Split", (dialog, which) -> {
                    EditText savingsInput = field("Amount to save (₱)", leftover.toPlainString(),
                            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    AlertDialog splitDialog = new MaterialAlertDialogBuilder(this)
                            .setTitle("Split leftover budget")
                            .setMessage("Enter the amount for savings. The remainder will carry over.")
                            .setView(savingsInput)
                            .setNegativeButton("Cancel", (splitDialogInterface, splitWhich) ->
                                    maybePromptForSavingsAllocation())
                            .setPositiveButton("Save allocation", null)
                            .create();
                    splitDialog.setOnShowListener(ignored -> splitDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(saveView -> {
                                try {
                                    BigDecimal savings = decimal(savingsInput, "Savings amount");
                                    if (savings.signum() < 0 || savings.compareTo(leftover) > 0) {
                                        throw new IllegalArgumentException("Savings must be between ₱0 and " + amount + ".");
                                    }
                                    BigDecimal carryover = leftover.subtract(savings)
                                            .setScale(2, java.math.RoundingMode.HALF_UP);
                                    saveBudgetAllocation(pending, savings, carryover);
                                    splitDialog.dismiss();
                                } catch (IllegalArgumentException ex) {
                                    toast(ex.getMessage());
                                }
                            }));
                    splitDialog.show();
                })
                .setCancelable(false)
                .show();
    }

    private void saveBudgetAllocation(WorkDatabase.BudgetCyclePending pending,
                                      BigDecimal savings, BigDecimal carryover) {
        if (!database.saveBudgetCycleAllocation(pending.payment.cutoffStart, pending.payment.cutoffEnd,
                savings, carryover)) {
            toast("Could not save the leftover budget decision.");
            return;
        }
        Toast.makeText(this, "Leftover budget allocation saved", Toast.LENGTH_SHORT).show();
        showTab(0);
    }

    private void saveOpeningBalance() {
        try {
            BigDecimal opening = decimal(openingBalanceInput, "Starting cash balance");
            if (opening.signum() < 0) {
                throw new IllegalArgumentException("Starting cash balance cannot be negative.");
            }
            BigDecimal openingSavings = new BigDecimal(pref("opening_savings_balance", "0.00"));
            if (opening.compareTo(openingSavings) < 0) {
                throw new IllegalArgumentException("Starting cash balance cannot be less than your opening savings balance.");
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Save starting cash?")
                    .setMessage("Set your opening cash balance to " + money(opening) + "? This value affects Available to Spend.")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString("opening_cash_balance",
                                        opening.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .apply();
                        Toast.makeText(this, "Starting cash balance saved", Toast.LENGTH_SHORT).show();
                        showTab(3);
                    }).show();
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    private void saveOpeningSavingsBalance() {
        try {
            BigDecimal opening = decimal(openingSavingsInput, "Starting savings balance");
            if (opening.signum() < 0) {
                throw new IllegalArgumentException("Starting savings balance cannot be negative.");
            }
            BigDecimal openingCash = new BigDecimal(pref("opening_cash_balance", "0.00"));
            if (opening.compareTo(openingCash) > 0) {
                throw new IllegalArgumentException("Starting savings cannot exceed your opening cash balance.");
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Save starting savings?")
                    .setMessage("Set your opening savings balance to " + money(opening) + "? This amount will be included in Savings Balance.")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putString("opening_savings_balance",
                                        opening.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .apply();
                        Toast.makeText(this, "Starting savings balance saved", Toast.LENGTH_SHORT).show();
                        showTab(3);
                    }).show();
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    private CutoffPeriod getCutoffAwaitingPayment(LocalDate today) {
        // Find the oldest unpaid cutoff whose scheduled payday has arrived. This
        // avoids skipping a missed payday when the app was not opened for a while.
        LocalDate firstMonth = today.minusMonths(12).withDayOfMonth(1);
        LocalDate month = firstMonth;
        CutoffPeriod fallback = null;

        while (!month.isAfter(today)) {
            CutoffPeriod firstCutoff = CutoffPeriod.forDate(month.withDayOfMonth(1));
            CutoffPeriod secondCutoff = CutoffPeriod.forDate(month.withDayOfMonth(16));

            CutoffPeriod[] candidates = {firstCutoff, secondCutoff};
            for (CutoffPeriod candidate : candidates) {
                if (candidate.startDate.isAfter(today)) continue;
                LocalDate scheduledPayday = getScheduledPayday(candidate);
                if (scheduledPayday.isAfter(today)) continue;
                fallback = candidate;
                if (database.getPaydayPayment(candidate.startDate.toString(),
                        candidate.endDate.toString()) == null) {
                    return candidate;
                }
            }
            month = month.plusMonths(1);
        }

        if (fallback != null) return fallback;
        LocalDate referenceDate = today.getDayOfMonth() <= 15
                ? today.minusMonths(1).withDayOfMonth(16)
                : today.withDayOfMonth(1);
        return CutoffPeriod.forDate(referenceDate);
    }

    private LocalDate getScheduledPayday(CutoffPeriod cutoff) {
        try {
            int first = Integer.parseInt(pref("first_cutoff_payday", "22"));
            int second = Integer.parseInt(pref("second_cutoff_payday", "7"));
            return cutoff.payday(first, second);
        } catch (NumberFormatException ignored) {
            return cutoff.payday(22, 7);
        }
    }

    private void showPaydayPaymentDialog() {
        showPaydayPaymentDialog(getCutoffAwaitingPayment(LocalDate.now()));
    }

    private void showPaydayPaymentDialog(CutoffPeriod paidCutoff) {
        if (paydayDialogShowing || isFinishing() || isDestroyed()) return;
        paydayDialogShowing = true;
        LocalDate today = LocalDate.now();
        LocalDate suggestedPayday = getScheduledPayday(paidCutoff);

        String start = paidCutoff.startDate.toString();
        String end = paidCutoff.endDate.toString();
        BigDecimal expected = database.getRecordedNetPayTotal(start, end);
        BigDecimal existing = database.getPaydayPayment(start, end);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(4), dp(4), dp(4), dp(4));
        TextView cutoffText = text("Cutoff: " + start + " to " + end, 14, true);
        cutoffText.setPadding(0, 0, 0, dp(4));
        form.addView(cutoffText);
        TextView expectedText = text("Expected salary from saved shifts: " + money(expected), 13, false);
        expectedText.setPadding(0, 0, 0, dp(10));
        form.addView(expectedText);
        EditText receivedInput = field("Actual amount received (₱)",
                existing == null ? expected.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                        : existing.toPlainString(),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(form, "Actual salary received (₱)", receivedInput);
        WorkDatabase.PaydayPayment existingPayment = null;
        for (WorkDatabase.PaydayPayment payment : database.getPaydayPayments(1000)) {
            if (payment.cutoffStart.equals(start) && payment.cutoffEnd.equals(end)) {
                existingPayment = payment;
                break;
            }
        }
        EditText paydayInput = field("Payday date",
                existingPayment == null ? suggestedPayday.toString() : existingPayment.paydayDate,
                InputType.TYPE_NULL);
        configureDatePicker(paydayInput);
        addField(form, "Date received", paydayInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Record salary received" : "Update recorded salary")
                .setView(form)
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialog -> paydayDialogShowing = false)
                .setPositiveButton("Save salary", (dialog, which) -> {
                    try {
                        BigDecimal received = decimal(receivedInput, "Actual salary received");
                        if (received.signum() < 0) {
                            throw new IllegalArgumentException("Received salary cannot be negative.");
                        }
                        LocalDate paidDate = LocalDate.parse(value(paydayInput), DateTimeFormatter.ISO_LOCAL_DATE);
                        if (database.hasPaydayDateConflict(paidDate.toString(), start, end)) {
                            throw new IllegalArgumentException(
                                    "Another salary payment already uses this date. Choose a different payday date so budget cycles and expenses stay unambiguous.");
                        }
                        boolean saved = database.savePaydayPayment(start, end, paidDate.toString(), expected, received);
                        if (!saved) {
                            toast("Could not save salary payment.");
                            return;
                        }
                        Toast.makeText(this, "Salary payment saved", Toast.LENGTH_SHORT).show();
                        showTab(0);
                    } catch (DateTimeParseException ex) {
                        toast("Select a valid payday date.");
                    } catch (IllegalArgumentException ex) {
                        toast(ex.getMessage());
                    }
                }).show();
    }

    private void confirmDeletePaydayPayment(WorkDatabase.PaydayPayment payment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete saved salary?")
                .setMessage("Received: " + money(new BigDecimal(payment.receivedAmount))
                        + "\nCutoff: " + payment.cutoffStart + " to " + payment.cutoffEnd
                        + "\nThis salary will be removed from your cash balance. Any carryover already assigned to this cycle will be moved into savings so it is not lost. This cannot be undone.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (database.deletePaydayPayment(payment.cutoffStart, payment.cutoffEnd)) {
                        Toast.makeText(this, "Salary payment deleted", Toast.LENGTH_SHORT).show();
                        showTab(3);
                    } else {
                        toast("Could not delete this salary payment.");
                    }
                }).show();
    }

    private void showSpendingBudgetDialog() {
        EditText limitInput = field("0.00", pref("spending_budget", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        addField(form, "Spending limit per salary cycle (₱, 0 to disable)", limitInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit spending budget")
                .setView(form)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                BigDecimal limit = decimal(limitInput, "Spending limit");
                if (limit.signum() < 0) {
                    throw new IllegalArgumentException("Spending limit cannot be negative. Enter 0 to disable it.");
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString("spending_budget",
                                limit.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                        .apply();
                dialog.dismiss();
                showTab(2);
                toast(limit.signum() == 0 ? "Spending limit disabled" : "Spending budget saved");
            } catch (IllegalArgumentException ex) {
                toast(ex.getMessage());
            }
        }));
        dialog.show();
    }

    private void saveSpendingBudget() {
        try {
            BigDecimal limit = decimal(spendingBudgetInput, "Spending limit");
            if (limit.signum() < 0) {
                throw new IllegalArgumentException("Spending limit cannot be negative. Enter 0 to disable it.");
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("spending_budget", limit.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                    .apply();
            Toast.makeText(this, limit.signum() == 0
                    ? "Spending limit disabled" : "Spending budget saved", Toast.LENGTH_SHORT).show();
            showTab(3);
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    /**
     * Shows the cutoff for the next scheduled payday. It advances only when that
     * configured payday date has been reached, using the user's saved Settings.
     */
    private CutoffPeriod getLatestPaydayCutoff(LocalDate today) {
        int firstPayday;
        int secondPayday;
        try {
            firstPayday = Integer.parseInt(pref("first_cutoff_payday", "22"));
        } catch (NumberFormatException ignored) {
            firstPayday = 22;
        }
        try {
            secondPayday = Integer.parseInt(pref("second_cutoff_payday", "7"));
        } catch (NumberFormatException ignored) {
            secondPayday = 7;
        }
        return CutoffPeriod.forNextPayday(today, firstPayday, secondPayday);
    }

    private LocalDate getPaydayForCurrentCutoff(LocalDate today) {
        boolean firstCutoff = today.getDayOfMonth() <= 15;
        int day;
        try {
            day = Integer.parseInt(pref(firstCutoff ? "first_cutoff_payday" : "second_cutoff_payday", firstCutoff ? "22" : "7"));
        } catch (NumberFormatException ignored) {
            day = firstCutoff ? 22 : 7;
        }
        return CutoffPeriod.forDate(today).payday(firstCutoff ? day : 22, firstCutoff ? 7 : day);
    }
    private void savePaydaySettings() {
        try {
            int first = Integer.parseInt(value(firstPaydayInput));
            int second = Integer.parseInt(value(secondPaydayInput));
            if (first < 1 || first > 31 || second < 1 || second > 31) {
                throw new IllegalArgumentException("Payday must be a day from 1 to 31.");
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("first_cutoff_payday", Integer.toString(first))
                    .putString("second_cutoff_payday", Integer.toString(second))
                    .apply();
            PaydayReminderScheduler.schedule(this);
            Toast.makeText(this, "Cutoff and payday settings saved", Toast.LENGTH_SHORT).show();
            showTab(3);
        } catch (NumberFormatException ex) {
            toast("Enter a valid payday day from 1 to 31.");
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    private void saveSalaryDefaults() {
        try {
            BigDecimal rate = decimal(hourlyRateInput, "Pay rate");
            String rateType = payRateTypeInput == null ? pref("pay_rate_type", "Hourly") : String.valueOf(payRateTypeInput.getText().toString());
            BigDecimal hours = decimal(regularHoursInput, "Regular hours");
            BigDecimal multiplier = decimal(overtimeMultiplierInput, "OT multiplier");
            BigDecimal allowance = decimal(allowanceInput, "Allowance");
            BigDecimal deduction = decimal(deductionInput, "Deduction");
            if (hours.signum() <= 0 || hours.multiply(BigDecimal.valueOf(60)).stripTrailingZeros().scale() > 0)
                throw new IllegalArgumentException("Regular hours must be positive and convert to whole minutes.");
            if (rate.signum() < 0 || allowance.signum() < 0 || deduction.signum() < 0 || multiplier.signum() <= 0)
                throw new IllegalArgumentException("Rates and allowances must be non-negative; OT multiplier must be positive.");
            saveSalaryPreferences(rate, rateType, hours, multiplier, allowance, deduction);
            Toast.makeText(this, "Salary defaults saved", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
    }

    private void saveSalaryPreferences(BigDecimal rate, String rateType, BigDecimal hours, BigDecimal multiplier,
                                       BigDecimal allowance, BigDecimal deduction) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("pay_rate", rate.toPlainString())
                .putString("pay_rate_type", rateType)
                .putString("hourly_rate", ("Daily".equals(rateType) ? rate.divide(hours, 8, java.math.RoundingMode.HALF_UP) : rate).toPlainString())
                .putString("regular_hours", hours.toPlainString())
                .putString("ot_multiplier", multiplier.toPlainString())
                .putString("allowance", allowance.toPlainString())
                .putString("deduction", deduction.toPlainString())
                .apply();
    }

    private String pref(String key, String fallback) {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, fallback);
    }

    private MaterialAutoCompleteTextView createRateTypeSpinner() {
        MaterialAutoCompleteTextView dropdown = new MaterialAutoCompleteTextView(this);
        dropdown.setInputType(InputType.TYPE_NULL);
        dropdown.setKeyListener(null);
        dropdown.setThreshold(0);
        dropdown.setSimpleItems(new String[]{"Hourly", "Daily"});
        dropdown.setText("Daily".equals(pref("pay_rate_type", "Hourly")) ? "Daily" : "Hourly", false);
        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (hourlyRateInput != null) updateRateLabel(dropdown, hourlyRateInput);
        });
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
        dropdown.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(56)));
        return dropdown;
    }

    private void updateRateLabel(MaterialAutoCompleteTextView dropdown, EditText input) {
        if (dropdown != null && input != null) {
            input.setHint("Daily".equals(dropdown.getText().toString())
                    ? "Daily rate amount" : "Hourly rate amount");
        }
    }

    private void configureDatePicker(EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setOnClickListener(v -> {
            LocalDate current;
            try {
                current = LocalDate.parse(value(input));
            } catch (Exception ignored) {
                current = LocalDate.now();
            }
            long initialSelection = current.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select date")
                    .setSelection(initialSelection)
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                LocalDate selected = java.time.Instant.ofEpochMilli(selection)
                        .atZone(java.time.ZoneOffset.UTC).toLocalDate();
                input.setText(selected.toString());
            });
            picker.show(getSupportFragmentManager(), "budget_tracker_date_picker");
        });
    }

    private void configureTimePicker(EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setOnClickListener(v -> {
            LocalTime current;
            try {
                current = LocalTime.parse(value(input));
            } catch (Exception ignored) {
                current = LocalTime.of(8, 0);
            }
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(current.getHour())
                    .setMinute(current.getMinute())
                    .setTitleText("Select time")
                    .build();
            picker.addOnPositiveButtonClickListener(view ->
                    input.setText(String.format(Locale.ROOT, "%02d:%02d",
                            picker.getHour(), picker.getMinute())));
            picker.show(getSupportFragmentManager(), "budget_tracker_time_picker");
        });
    }

    private void header(String title, String subtitle) {
        TextView titleView = text(title, 28, true);
        int headerIcon = iconForLabel(title);
        if (headerIcon != 0) {
            android.graphics.drawable.Drawable icon = getDrawable(headerIcon).mutate();
            icon.setTint(resolveColor(androidx.appcompat.R.attr.colorPrimary));
            titleView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            titleView.setCompoundDrawablePadding(dp(10));
        }
        titleView.setTextColor(resolveColor(android.R.attr.textColorPrimary));
        page.addView(titleView);
        TextView sub = text(subtitle, 14, false);
        sub.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        sub.setPadding(0, dp(5), 0, dp(16));
        page.addView(sub);
    }

    private void section(String label) {
        addSection(page, label);
    }

    private void addSection(LinearLayout parent, String label) {
        TextView heading = text(label, 18, true);
        int sectionIcon = iconForLabel(label);
        if (sectionIcon != 0) {
            android.graphics.drawable.Drawable icon = getDrawable(sectionIcon).mutate();
            icon.setTint(resolveColor(androidx.appcompat.R.attr.colorPrimary));
            heading.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            heading.setCompoundDrawablePadding(dp(8));
        }
        heading.setPadding(0, dp(12), 0, dp(8));
        parent.addView(heading);
    }

    private void card(String title, String value, String detail) {
        LinearLayout card = cardContainer();
        TextView top = text(title, 12, true);
        top.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        card.addView(top);
        TextView main = text(value, 20, true);
        main.setPadding(0, dp(5), 0, dp(3));
        card.addView(main);
        TextView bottom = text(detail, 13, false);
        bottom.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        card.addView(bottom);
        page.addView(card);
    }

    private void rowCards(String label1, String value1, String label2, String value2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout first = cardContainer();
        LinearLayout second = cardContainer();
        for (LinearLayout target : new LinearLayout[]{first, second}) {
            target.setPadding(dp(14), dp(14), dp(14), dp(14));
        }
        TextView a = text(label1, 11, true);
        a.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        first.addView(a);
        first.addView(text(value1, 19, true));
        TextView b = text(label2, 11, true);
        b.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        second.addView(b);
        second.addView(text(value2, 19, true));
        row.addView(first, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(space(dp(10)), new LinearLayout.LayoutParams(dp(10), 1));
        row.addView(second, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        page.addView(row, params);
    }

    private void actionButton(String title, String detail, Runnable action) {
        LinearLayout item = cardContainer();
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable actionBackground = new GradientDrawable();
        actionBackground.setColor(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant));
        actionBackground.setCornerRadius(dp(24));
        actionBackground.setStroke(dp(1), resolveColor(com.google.android.material.R.attr.colorOutlineVariant));
        item.setBackground(new RippleDrawable(
                ColorStateList.valueOf(withAlpha(resolveColor(androidx.appcompat.R.attr.colorPrimary), 0x1F)), actionBackground, null));
        item.setClickable(true);
        item.setFocusable(true);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(text(title, 16, true));
        TextView desc = text(detail, 12, false);
        desc.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        words.addView(desc);
        item.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 26, false);
        arrow.setTextColor(resolveColor(androidx.appcompat.R.attr.colorPrimary));
        item.addView(arrow);
        item.setOnClickListener(v -> action.run());
        page.addView(item);
    }

    private LinearLayout cardContainer() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant));
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), resolveColor(com.google.android.material.R.attr.colorOutlineVariant));
        card.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private void addField(LinearLayout parent, String label, View input) {
        if (input instanceof MaterialAutoCompleteTextView) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(8);
            TextInputLayout dropdownLayout = new TextInputLayout(this);
            dropdownLayout.setHint(label);
            dropdownLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
            dropdownLayout.setBoxBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant));
            dropdownLayout.setBoxCornerRadii(dp(12), dp(12), dp(12), dp(12));
            dropdownLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
            dropdownLayout.addView(input);
            parent.addView(dropdownLayout, params);
            return;
        }
        if (input instanceof EditText) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(8);
            parent.addView(inputLayout(label, (EditText) input), params);
            return;
        }
        TextView caption = text(label, 12, true);
        caption.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        caption.setPadding(0, dp(8), 0, dp(4));
        parent.addView(caption);
        parent.addView(input);
    }

    private TextInputLayout inputLayout(String label, EditText input) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(label);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        layout.setBoxBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurfaceVariant));
        layout.setBoxCornerRadii(dp(12), dp(12), dp(12), dp(12));
        layout.setBoxStrokeColor(resolveColor(com.google.android.material.R.attr.colorOutline));
        input.setHint(null);
        input.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(56)));
        layout.addView(input);
        return layout;
    }

    private EditText field(String hint, String initial, int inputType) {
        EditText edit = new TextInputEditText(this);
        edit.setSingleLine(true);
        edit.setHint(hint);
        edit.setText(initial);
        edit.setInputType(inputType);
        edit.setTextSize(16);
        edit.setPadding(dp(12), 0, dp(12), 0);
        edit.setSelectAllOnFocus(false);
        edit.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        return edit;
    }


    private int iconForLabel(String label) {
        if (label == null) return 0;
        String value = label.toLowerCase(Locale.ROOT);
        if (value.contains("delete") || value.contains("remove")) return R.drawable.ic_delete;
        if (value.contains("edit") || value.contains("correct")) return R.drawable.ic_edit;
        if (value.contains("expense") || value.contains("spending") || value.contains("spent")) return R.drawable.ic_expense;
        if (value.contains("category")) return R.drawable.ic_category;
        if (value.contains("budget") || value.contains("limit")) return R.drawable.ic_budget;
        if (value.contains("calendar") || value.contains("date")) return R.drawable.ic_calendar;
        if (value.contains("payday") || value.contains("cutoff") || value.contains("salary")) return R.drawable.ic_payday;
        if (value.contains("saving")) return R.drawable.ic_savings;
        if (value.contains("cash") || value.contains("balance") || value.contains("payment")) return R.drawable.ic_cash;
        if (value.contains("work shift") || value.contains("income") || value.contains("quick shift")) return R.drawable.ic_income;
        if (value.contains("add") || value.contains("new")) return R.drawable.ic_add;
        if (value.contains("activity") || value.contains("history")) return R.drawable.ic_history;
        if (value.contains("settings")) return R.drawable.ic_settings;
        if (value.contains("welcome") || value.contains("finances")) return R.drawable.ic_cash;
        return 0;
    }

    private MaterialButton compactButton(String label, boolean primary) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(40));
        button.setMinimumHeight(dp(40));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(dp(12));
        button.setIconPadding(dp(4));
        int iconRes = iconForLabel(label);
        if (iconRes != 0) {
            button.setIconResource(iconRes);
            button.setIconTint(ColorStateList.valueOf(resolveColor(
                    primary ? com.google.android.material.R.attr.colorOnPrimary
                            : com.google.android.material.R.attr.colorOnSecondaryContainer)));
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        }
        button.setPadding(dp(12), dp(4), dp(12), dp(4));
        button.setElevation(0);
        if (primary) {
            button.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnPrimary));
            button.setBackgroundTintList(ColorStateList.valueOf(resolveColor(androidx.appcompat.R.attr.colorPrimary)));
        } else {
            button.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
            button.setBackgroundTintList(ColorStateList.valueOf(resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(40));
        button.setLayoutParams(params);
        return button;
    }

    private MaterialButton button(String label, boolean primary) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        int iconRes = iconForLabel(label);
        if (iconRes != 0) {
            button.setIconResource(iconRes);
            button.setIconTint(ColorStateList.valueOf(resolveColor(
                    primary ? com.google.android.material.R.attr.colorOnPrimary
                            : com.google.android.material.R.attr.colorOnSecondaryContainer)));
            button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            button.setIconPadding(dp(8));
        }
        button.setCornerRadius(dp(16));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setElevation(dp(1));
        if (primary) {
            button.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnPrimary));
            button.setBackgroundTintList(ColorStateList.valueOf(
                    resolveColor(androidx.appcompat.R.attr.colorPrimary)));
        } else {
            button.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
            button.setBackgroundTintList(ColorStateList.valueOf(
                    resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)));
        }
        button.setRippleColor(ColorStateList.valueOf(
                withAlpha(resolveColor(androidx.appcompat.R.attr.colorPrimary), 0x33)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.topMargin = dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private void addListItem(LinearLayout parent, String value) {
        TextView item = text(value, 14, false);
        item.setPadding(dp(14), dp(13), dp(14), dp(13));
        item.setBackgroundColor(resolveColor(android.R.attr.colorBackground));
        parent.addView(item);
        View divider = new View(this);
        divider.setBackgroundColor(withAlpha(resolveColor(com.google.android.material.R.attr.colorOutlineVariant), 0x66));
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private View emptyState(String title, String detail) {
        LinearLayout box = cardContainer();
        box.addView(text(title, 15, true));
        TextView sub = text(detail, 13, false);
        sub.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        box.addView(sub);
        return box;
    }

    private View space(int width) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return view;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(resolveColor(android.R.attr.textColorPrimary));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private int resolveColor(int attribute) {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(attribute, value, true);
        if (value.resourceId != 0) return getResources().getColorStateList(value.resourceId, getTheme()).getDefaultColor();
        return value.data;
    }

    private static String value(EditText field) {
        return field.getText().toString().trim();
    }

    private static BigDecimal decimal(EditText field, String label) {
        String raw = value(field);
        if (raw.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.getDefault(), "₱%,.2f", amount);
    }

    private static String duration(int minutes) {
        return String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60);
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "Something went wrong." : message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class BreakInput {
        final View row;
        final EditText hours;
        BreakInput(View row, EditText hours) {
            this.row = row;
            this.hours = hours;
        }
    }
}