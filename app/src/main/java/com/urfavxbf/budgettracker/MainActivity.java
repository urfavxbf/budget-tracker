package com.urfavxbf.budgettracker;

import androidx.fragment.app.FragmentActivity;
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
    private static final String[] TAB_NAMES = {"Home", "History", "Settings"};
    private final List<BreakInput> breakInputs = new ArrayList<>();
    private LinearLayout page;
    private BottomNavigationView bottomNav;
    private static final int NAV_HOME_ID = 1;
    private static final int NAV_HISTORY_ID = 2;
    private static final int NAV_SETTINGS_ID = 3;
    private int currentTab = 0;
    private EditText dateInput, timeInInput, timeOutInput;
    private EditText hourlyRateInput, regularHoursInput, overtimeMultiplierInput, allowanceInput, deductionInput;
    private EditText firstPaydayInput, secondPaydayInput;
    private MaterialAutoCompleteTextView payRateTypeInput;
    private EditText expenseDateInput, expenseAmountInput, expenseNoteInput;
    private MaterialAutoCompleteTextView expenseCategoryInput;
    private LinearLayout breakContainer, historyContainer, expenseHistoryContainer;
    private TextView resultView, budgetSummaryView;
    private EditText spendingBudgetInput;
    private EditText openingBalanceInput;
    private ProgressBar budgetProgressBar;
    private WorkDatabase database;
    private long editingWorkEntryId = -1;
    private long editingExpenseId = -1;
    private Button saveWorkButton;
    private Button saveExpenseButton;
    private boolean lastSaveSucceeded;
    private boolean paydayDialogShowing;
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
        showTab(currentTab);
        PaydayReminderScheduler.schedule(this);
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 7127);
        }
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

    @Override protected void onResume() {
        super.onResume();
        maybePromptForPaydaySalary();
    }

    @Override protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }

    private void buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface));

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
        bottomNav.getMenu().add(0, NAV_HISTORY_ID, 1, "History").setIcon(R.drawable.ic_history);
        bottomNav.getMenu().add(0, NAV_SETTINGS_ID, 2, "Settings").setIcon(R.drawable.ic_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int tab;
            if (item.getItemId() == NAV_HOME_ID) {
                tab = 0;
            } else if (item.getItemId() == NAV_HISTORY_ID) {
                tab = 1;
            } else if (item.getItemId() == NAV_SETTINGS_ID) {
                tab = 2;
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
                : currentTab == 1 ? NAV_HISTORY_ID : NAV_SETTINGS_ID);
    }

    private void showTab(int tab) {
        page.removeAllViews();
        breakInputs.clear();
        dateInput = timeInInput = timeOutInput = null;
        hourlyRateInput = regularHoursInput = overtimeMultiplierInput = allowanceInput = deductionInput = null;
        expenseDateInput = null;
        expenseCategoryInput = null;
        expenseAmountInput = expenseNoteInput = null;
        breakContainer = historyContainer = expenseHistoryContainer = null;
        resultView = budgetSummaryView = null;
        spendingBudgetInput = null;
        openingBalanceInput = null;
        budgetProgressBar = null;
        saveWorkButton = null;
        saveExpenseButton = null;

        switch (tab) {
            case 0: buildDashboard(); break;
            case 1: buildHistoryScreen(); break;
            case 2: buildSettingsScreen(); break;
            default: buildDashboard();
        }
    }

    private void buildDashboard() {
        header("Good day 👋", "Your money, at a glance.");
        LocalDate today = LocalDate.now();
        LocalDate start = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
        LocalDate end = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.withDayOfMonth(today.lengthOfMonth());
        LocalDate payday = getPaydayForCurrentCutoff(today);
        BigDecimal earned = database.getRecordedNetPayTotal(start.toString(), end.toString());
        BigDecimal spent = database.getExpenseTotal(start.toString(), end.toString());
        BigDecimal remaining = getAvailableCashBalance();

        LinearLayout totals = new LinearLayout(this);
        totals.setOrientation(LinearLayout.HORIZONTAL);
        totals.setGravity(Gravity.CENTER_VERTICAL);
        totals.setPadding(0, dp(4), 0, dp(12));
        totals.addView(metricCard("EXPECTED PAY", money(earned), resolveColor(androidx.appcompat.R.attr.colorPrimary)), new LinearLayout.LayoutParams(0, dp(84), 1f));
        View gap = new View(this);
        totals.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        totals.addView(metricCard("EXPENSES", money(spent), resolveColor(androidx.appcompat.R.attr.colorError)), new LinearLayout.LayoutParams(0, dp(84), 1f));
        LinearLayout.LayoutParams totalsParams = new LinearLayout.LayoutParams(-1, -2);
        totalsParams.bottomMargin = dp(2);
        page.addView(totals, totalsParams);

        LinearLayout snapshot = cardContainer();
        snapshot.setPadding(dp(18), dp(18), dp(18), dp(18));
        snapshot.setBackground(roundedBackground(resolveColor(com.google.android.material.R.attr.colorSecondaryContainer), dp(22)));
        TextView snapshotTitle = text("Budget snapshot", 18, true);
        snapshot.addView(snapshotTitle);
        TextView cutoffLabel = text((today.getDayOfMonth() <= 15 ? "1st–15th cutoff" : "16th–month-end cutoff")
                + "  ·  Payday " + payday.format(DateTimeFormatter.ofPattern("MMM d")), 12, false);
        cutoffLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        cutoffLabel.setPadding(0, dp(4), 0, dp(14));
        snapshot.addView(cutoffLabel);
        TextView remainingLabel = text("AVAILABLE CASH BALANCE", 11, true);
        remainingLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        snapshot.addView(remainingLabel);
        TextView amount = text(money(remaining), 30, true);
        amount.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        amount.setPadding(0, dp(3), 0, dp(12));
        snapshot.addView(amount);
        budgetProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        budgetProgressBar.setMax(100);
        budgetProgressBar.setProgressTintList(ColorStateList.valueOf(resolveColor(androidx.appcompat.R.attr.colorPrimary)));
        budgetProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(withAlpha(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer), 0x30)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(7));
        progressParams.bottomMargin = dp(10);
        snapshot.addView(budgetProgressBar, progressParams);
        budgetSummaryView = text("", 13, false);
        budgetSummaryView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        snapshot.addView(budgetSummaryView);
        page.addView(snapshot);
        CutoffPeriod paymentCutoff = getCutoffAwaitingPayment(today);
        boolean paymentAlreadyRecorded = database.getPaydayPayment(paymentCutoff.startDate.toString(),
                paymentCutoff.endDate.toString()) != null;
        MaterialButton recordPayday = button("＋ Record payday salary", true);
        recordPayday.setOnClickListener(v -> showPaydayPaymentDialog());
        recordPayday.setVisibility(paymentAlreadyRecorded ? View.GONE : View.VISIBLE);
        page.addView(recordPayday);
        refreshBudget();

        TextView disclaimer = text("Expected pay is an estimate. Available cash uses your opening balance and confirmed salary payments, minus recorded expenses.", 11, false);
        disclaimer.setAlpha(0.75f);
        disclaimer.setPadding(dp(2), dp(8), dp(2), dp(12));
        page.addView(disclaimer);

        section("Recent activity");
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(historyContainer);
        refreshDashboardActivity();

        section("Quick actions");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton addShift = button("＋ Work shift", false);
        addShift.setOnClickListener(v -> showWorkDialog(null));
        MaterialButton addExpense = button("＋ Expense", false);
        addExpense.setOnClickListener(v -> showExpenseDialog(null));
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        actionLp.setMargins(0, 0, dp(6), 0);
        actions.addView(addShift, actionLp);
        LinearLayout.LayoutParams expenseLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        expenseLp.setMargins(dp(6), 0, 0, 0);
        actions.addView(addExpense, expenseLp);
        page.addView(actions);
        maybePromptForPaydaySalary();
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
        header("History", "Review, edit, or delete your saved shifts and expenses.");
        section("Recent activity");
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
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            lastSaveSucceeded = false;
            addExpense();
            if (lastSaveSucceeded) {
                dialog.dismiss();
                showTab(currentTab);
            }
        });
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

        section("Spending budget");
        TextView budgetInfo = text("Set a spending cap for each cutoff period. The dashboard tracks recorded expenses against this limit. Enter 0 to disable the limit.", 13, false);
        budgetInfo.setPadding(0, 0, 0, dp(8));
        page.addView(budgetInfo);
        spendingBudgetInput = field("0.00", pref("spending_budget", "0.00"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(page, "Spending limit per cutoff (₱)", spendingBudgetInput);
        Button saveBudget = button("Save spending budget", true);
        saveBudget.setOnClickListener(v -> saveSpendingBudget());
        page.addView(saveBudget);

        section("About your data");
        TextView info = text("Your entries are stored locally on this device. Expected pay is estimated from saved shifts. Record the amount actually received on payday to update available cash. Opening balance is adjusted against all expenses currently saved in the app.", 14, false);
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
        breakContainer.addView(row);
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
            if (updatingExpense) {
                if (!database.updateExpense(editingExpenseId, date.toString(), category, value(expenseNoteInput), amount)) {
                    throw new IllegalArgumentException("This saved expense no longer exists.");
                }
                editingExpenseId = -1;
                if (saveExpenseButton != null) saveExpenseButton.setText("Save expense");
            } else {
                database.insertExpense(date.toString(), category, value(expenseNoteInput), amount);
            }
            if (!updatingExpense) {
                expenseAmountInput.setText("");
                expenseNoteInput.setText("");
            }
            refreshBudget();
            refreshExpenseHistory();
            Toast.makeText(this, updatingExpense ? "Expense updated" : "Expense saved", Toast.LENGTH_SHORT).show();
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
            historyContainer.addView(emptyState("No activity yet", "Saved work shifts and expenses will appear here, grouped by date."));
            return;
        }

        int shownDays = 0;
        for (DayHistory day : days.values()) {
            if (shownDays++ >= 30) break;
            LinearLayout dayCard = cardContainer();
            TextView dateTitle = text(day.date, 16, true);
            dateTitle.setPadding(0, 0, 0, dp(8));
            dayCard.addView(dateTitle);

            for (WorkDatabase.WorkEntry entry : day.workEntries) {
                LinearLayout recordContent = new LinearLayout(this);
                recordContent.setOrientation(LinearLayout.VERTICAL);
                TextView shiftTime = text(entry.timeIn + " - " + entry.timeOut, 14, true);
                shiftTime.setPadding(0, dp(3), 0, dp(2));
                recordContent.addView(shiftTime);
                recordContent.addView(text("Work " + duration(entry.netMinutes) + "  Break " + duration(entry.breakMinutes),
                        13, false));
                if (entry.overtimeMinutes > 0) {
                    recordContent.addView(text("Overtime: " + duration(entry.overtimeMinutes), 13, false));
                }
                recordContent.addView(text("Recorded net pay: " + money(new BigDecimal(entry.netPay)), 14, true));
                addSwipeReveal(dayCard, recordContent, "Edit shift", "Delete shift",
                        v -> editWorkEntry(entry), v -> confirmDeleteWorkEntry(entry));
            }

            if (!day.expenses.isEmpty()) {
                TextView expensesTitle = text("Expenses:", 14, true);
                expensesTitle.setPadding(0, dp(5), 0, dp(3));
                dayCard.addView(expensesTitle);
                for (WorkDatabase.ExpenseEntry entry : day.expenses) {
                    LinearLayout recordContent = new LinearLayout(this);
                    recordContent.setOrientation(LinearLayout.VERTICAL);
                    recordContent.addView(text(entry.category + "  " + money(new BigDecimal(entry.amount)), 14, true));
                    if (!entry.note.isEmpty()) {
                        TextView note = text(entry.note, 12, false);
                        note.setTextColor(resolveColor(android.R.attr.textColorSecondary));
                        recordContent.addView(note);
                    }
                    addSwipeReveal(dayCard, recordContent, "Edit expense", "Delete expense",
                            v -> editExpense(entry), v -> confirmDeleteExpense(entry));
                }
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
        actions.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface));
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(
                revealWidth, -1, Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(actions, actionParams);

        MaterialButton edit = button("Edit", false);
        edit.setTextSize(12);
        edit.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        edit.setPadding(0, 0, 0, 0);
        edit.setMinWidth(0);
        edit.setMinimumWidth(0);
        edit.setMinimumHeight(0);
        edit.setCornerRadius(dp(12));
        edit.setBackgroundTintList(ColorStateList.valueOf(
                resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)));
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
        delete.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnErrorContainer));
        delete.setPadding(0, 0, 0, 0);
        delete.setMinWidth(0);
        delete.setMinimumWidth(0);
        delete.setMinimumHeight(0);
        delete.setCornerRadius(dp(12));
        delete.setBackgroundTintList(ColorStateList.valueOf(
                resolveColor(com.google.android.material.R.attr.colorErrorContainer)));
        delete.setRippleColor(ColorStateList.valueOf(
                withAlpha(resolveColor(androidx.appcompat.R.attr.colorError), 0x33)));
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

        recordContent.setPadding(dp(8), dp(8), dp(8), dp(8));
        recordContent.setMinimumHeight(dp(64));
        recordContent.setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface));
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
        CutoffPeriod cutoff = CutoffPeriod.forDate(today);
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

        LocalDate payday = cutoff.payday(firstPayday, secondPayday);
        long daysUntilPayday = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, payday));
        BigDecimal earned = database.getRecordedNetPayTotal(cutoff.startDate.toString(), cutoff.endDate.toString());
        BigDecimal spent = database.getExpenseTotal(cutoff.startDate.toString(), cutoff.endDate.toString());
        BigDecimal remaining = getAvailableCashBalance();

        BigDecimal spendingLimit;
        try {
            spendingLimit = new BigDecimal(pref("spending_budget", "0.00"));
            if (spendingLimit.signum() < 0) spendingLimit = BigDecimal.ZERO;
        } catch (NumberFormatException ignored) {
            spendingLimit = BigDecimal.ZERO;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Payday in ").append(daysUntilPayday)
                .append(daysUntilPayday == 1 ? " day" : " days")
                .append("\\nAvailable cash: ").append(money(remaining));

        BigDecimal dailyGuide = BigDecimal.ZERO;
        boolean hasDailyGuide = false;
        if (daysUntilPayday > 0 && spendingLimit.signum() > 0) {
            BigDecimal budgetRemaining = spendingLimit.subtract(spent).max(BigDecimal.ZERO);
            dailyGuide = budgetRemaining.divide(BigDecimal.valueOf(daysUntilPayday),
                    2, java.math.RoundingMode.DOWN);
            hasDailyGuide = true;
        } else if (daysUntilPayday > 0 && remaining.signum() > 0) {
            dailyGuide = remaining.divide(BigDecimal.valueOf(daysUntilPayday),
                    2, java.math.RoundingMode.DOWN);
            hasDailyGuide = true;
        }

        summary.append("\n");
        if (hasDailyGuide) {
            summary.append("Daily spending guide: ").append(money(dailyGuide))
                    .append("\nUse this as a guide, not a guarantee; account for bills due before payday.");
        } else if (daysUntilPayday == 0) {
            summary.append("Payday is today. Review any expenses due before your next pay.");
        } else {
            summary.append("No daily guide yet. Add your spending limit in Settings, or record your income.");
        }

        if (spendingLimit.signum() > 0) {
            int usedPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(spendingLimit, 0, java.math.RoundingMode.HALF_UP)
                    .max(BigDecimal.ZERO).intValue();
            budgetProgressBar.setVisibility(View.VISIBLE);
            budgetProgressBar.setProgress(Math.min(100, usedPercent));

            BigDecimal budgetRemaining = spendingLimit.subtract(spent);
            BigDecimal actualPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(spendingLimit, 1, java.math.RoundingMode.HALF_UP);
            summary.append("\n").append(money(spent)).append(" spent of ").append(money(spendingLimit))
                    .append(" · ").append(actualPercent.toPlainString()).append("% used")
                    .append("\n").append(money(budgetRemaining.max(BigDecimal.ZERO))).append(" budget remaining");

            if (spent.compareTo(spendingLimit) > 0) {
                summary.append("\nOver budget by ").append(money(spent.subtract(spendingLimit)))
                        .append(". If possible, pause non-essential spending and review upcoming needs.");
                budgetSummaryView.setTextColor(resolveColor(androidx.appcompat.R.attr.colorError));
            } else if (spent.compareTo(spendingLimit.multiply(new BigDecimal("0.80"))) >= 0) {
                summary.append("\nMost of this cutoff’s budget is used. Check what still needs to be paid.");
                budgetSummaryView.setTextColor(resolveColor(androidx.appcompat.R.attr.colorError));
            } else {
                summary.append("\nYour spending is within the limit so far.");
                budgetSummaryView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
            }
        } else {
            budgetProgressBar.setVisibility(View.GONE);
            summary.append("\nSet a spending limit in Settings for a more useful daily guide.");
            budgetSummaryView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSecondaryContainer));
        }

        Map<String, BigDecimal> categoryTotals =
                database.getExpenseTotalsByCategory(cutoff.startDate.toString(), cutoff.endDate.toString());
        if (categoryTotals.isEmpty()) {
            summary.append("\nNo expenses recorded for this cutoff yet.");
        } else {
            Map.Entry<String, BigDecimal> topCategory = categoryTotals.entrySet().iterator().next();
            summary.append("\nTop category: ").append(topCategory.getKey())
                    .append(" · ").append(money(topCategory.getValue()));
        }
        budgetSummaryView.setText(summary.toString());
    }

    private BigDecimal getAvailableCashBalance() {
        BigDecimal opening;
        try {
            opening = new BigDecimal(pref("opening_cash_balance", "0.00"));
        } catch (NumberFormatException ignored) {
            opening = BigDecimal.ZERO;
        }
        return opening.add(database.getTotalReceivedPay())
                .subtract(database.getAllExpensesTotal())
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void saveOpeningBalance() {
        try {
            BigDecimal opening = decimal(openingBalanceInput, "Starting cash balance");
            if (opening.signum() < 0) {
                throw new IllegalArgumentException("Starting cash balance cannot be negative.");
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("opening_cash_balance",
                            opening.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                    .apply();
            Toast.makeText(this, "Starting cash balance saved", Toast.LENGTH_SHORT).show();
            showTab(2);
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

    private void maybePromptForPaydaySalary() {
        if (paydayDialogShowing || database == null || page == null || isFinishing() || isDestroyed()) return;
        LocalDate today = LocalDate.now();
        CutoffPeriod cutoff = getCutoffAwaitingPayment(today);
        if (database.getPaydayPayment(cutoff.startDate.toString(), cutoff.endDate.toString()) != null) {
            return;
        }
        if (!today.isBefore(getScheduledPayday(cutoff))) {
            page.post(() -> {
                if (!isFinishing() && !isDestroyed() && currentTab == 0
                        && !paydayDialogShowing
                        && database.getPaydayPayment(cutoff.startDate.toString(), cutoff.endDate.toString()) == null) {
                    showPaydayPaymentDialog(cutoff);
                }
            });
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
        EditText paydayInput = field("Payday date", existing == null ? suggestedPayday.toString() : suggestedPayday.toString(),
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
            showTab(2);
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        }
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
            showTab(2);
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

    private MaterialButton button(String label, boolean primary) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
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
