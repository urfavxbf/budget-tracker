package com.urfavxbf.budgettracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public final class MainActivity extends Activity {
    private static final String PREFS = "salary_preferences";
    private static final String[] TAB_NAMES = {"Home", "Work", "Expenses", "Settings"};
    private final List<BreakInput> breakInputs = new ArrayList<>();
    private LinearLayout page;
    private LinearLayout bottomNav;
    private int currentTab = 0;
    private EditText dateInput, timeInInput, timeOutInput;
    private EditText hourlyRateInput, regularHoursInput, overtimeMultiplierInput, allowanceInput, deductionInput;
    private EditText firstPaydayInput, secondPaydayInput;
    private Spinner payRateTypeInput;
    private EditText expenseDateInput, expenseCategoryInput, expenseAmountInput, expenseNoteInput;
    private LinearLayout breakContainer, historyContainer, expenseHistoryContainer;
    private TextView resultView, budgetSummaryView;
    private WorkDatabase database;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSharedPreferences(BudgetTrackerApp.CRASH_PREFS, MODE_PRIVATE)
                .contains(BudgetTrackerApp.CRASH_REPORT)) {
            startActivity(new android.content.Intent(this, CrashActivity.class));
            finish();
            return;
        }
        database = new WorkDatabase(this);
        getWindow().setStatusBarColor(resolveColor(android.R.attr.colorBackground));
        getWindow().setNavigationBarColor(resolveColor(android.R.attr.colorBackground));
        buildShell();
        applyImmersiveMode();
        showTab(currentTab);
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
        shell.setBackgroundColor(resolveColor(android.R.attr.colorBackground));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(8), dp(6), dp(8), dp(6));
        bottomNav.setBackgroundColor(resolveColor(android.R.attr.colorBackground));
        shell.addView(bottomNav, new LinearLayout.LayoutParams(-1, dp(68)));
        setContentView(shell);
        buildBottomNav();
    }

    private void buildBottomNav() {
        bottomNav.removeAllViews();
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int tab = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(5), dp(2), dp(5));
            TextView icon = text(new String[]{"⌂", "◷", "−", "⚙"}[i], 21, true);
            TextView label = text(TAB_NAMES[i], 11, currentTab == i);
            boolean selected = currentTab == i;
            int accent = resolveColor(android.R.attr.colorAccent);
            icon.setTextColor(selected ? accent : resolveColor(android.R.attr.textColorSecondary));
            label.setTextColor(selected ? accent : resolveColor(android.R.attr.textColorSecondary));
            item.addView(icon);
            item.addView(label);
            item.setOnClickListener(v -> {
                currentTab = tab;
                buildBottomNav();
                showTab(tab);
            });
            bottomNav.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
        }
    }

    private void showTab(int tab) {
        page.removeAllViews();
        breakInputs.clear();
        dateInput = timeInInput = timeOutInput = null;
        hourlyRateInput = regularHoursInput = overtimeMultiplierInput = allowanceInput = deductionInput = null;
        expenseDateInput = expenseCategoryInput = expenseAmountInput = expenseNoteInput = null;
        breakContainer = historyContainer = expenseHistoryContainer = null;
        resultView = budgetSummaryView = null;

        switch (tab) {
            case 0: buildDashboard(); break;
            case 1: buildWorkScreen(); break;
            case 2: buildExpensesScreen(); break;
            case 3: buildSettingsScreen(); break;
            default: buildDashboard();
        }
    }

    private void buildDashboard() {
        header("Good day 👋", "Here’s your salary and budget overview.");
        LocalDate today = LocalDate.now();
        LocalDate start = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
        LocalDate end = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.withDayOfMonth(today.lengthOfMonth());
        LocalDate payday = getPaydayForCurrentCutoff(today);
        BigDecimal earned = database.getRecordedNetPayTotal(start.toString(), end.toString());
        BigDecimal spent = database.getExpenseTotal(start.toString(), end.toString());
        BigDecimal remaining = earned.subtract(spent);

        card("PAYDAY CUTOFF", (today.getDayOfMonth() <= 15 ? "15th cutoff" : "Month-end cutoff"),
                start.format(DateTimeFormatter.ofPattern("MMM d")) + " – " + end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                        + "\nPayday: " + payday.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        LinearLayout balance = cardContainer();
        TextView eyebrow = text("RECORDED PAY MINUS EXPENSES", 12, true);
        eyebrow.setAlpha(0.78f);
        balance.addView(eyebrow);
        TextView amount = text(money(remaining), 32, true);
        amount.setPadding(0, dp(8), 0, dp(6));
        balance.addView(amount);
        balance.addView(text("Saved work-entry pay total minus recorded expenses", 12, false));
        page.addView(balance);

        rowCards("TOTAL RECORDED PAY", money(earned), "RECORDED EXPENSES", money(spent));
        section("Quick actions");
        actionButton("＋  Add work shift", "Record time-in, time-out and breaks", 1);
        actionButton("−  Add an expense", "Track spending for this cutoff", 2);
        section("Recent work entries");
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(historyContainer);
        refreshHistory();
        section("Budget snapshot");
        budgetSummaryView = text("", 14, false);
        page.addView(budgetSummaryView);
        refreshBudget();
        TextView disclaimer = text("Calculated from saved work entries; not a confirmed employer payout.", 12, false);
        disclaimer.setAlpha(0.7f);
        disclaimer.setPadding(0, dp(16), 0, 0);
        page.addView(disclaimer);
    }

    private void buildWorkScreen() {
        header("Work tracker", "Log your shift and calculate your estimated daily pay.");
        LinearLayout card = cardContainer();
        page.addView(card);
        dateInput = field("Select work date", LocalDate.now().toString(), InputType.TYPE_NULL);
        configureDatePicker(dateInput);
        addField(card, "Work date", dateInput);
        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        timeInInput = field("Choose time", "08:00", InputType.TYPE_NULL);
        timeOutInput = field("Choose time", "17:00", InputType.TYPE_NULL);
        configureTimePicker(timeInInput);
        configureTimePicker(timeOutInput);
        times.addView(timeInInput, new LinearLayout.LayoutParams(0, dp(56), 1f));
        times.addView(space(dp(8)), new LinearLayout.LayoutParams(dp(8), 1));
        times.addView(timeOutInput, new LinearLayout.LayoutParams(0, dp(56), 1f));
        addField(card, "Time in / time out", times);

        addSection(card, "Break duration");
        breakContainer = new LinearLayout(this);
        breakContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(breakContainer);
        addBreakRow("1");
        Button addBreak = button("＋ Add break duration", false);
        addBreak.setOnClickListener(v -> addBreakRow(""));
        card.addView(addBreak);

        addSection(card, "Pay rate and calculation");
        payRateTypeInput = createRateTypeSpinner();
        addField(card, "Pay rate type", payRateTypeInput);
        hourlyRateInput = field("Rate amount", pref("pay_rate", pref("hourly_rate", "100.00")), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        regularHoursInput = field("Regular hours per day", pref("regular_hours", "8"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        overtimeMultiplierInput = field("OT multiplier", pref("ot_multiplier", "1.25"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        allowanceInput = field("Daily allowance", pref("allowance", "0.00"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        deductionInput = field("Daily deduction", pref("deduction", "0.00"), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addField(card, "Rate amount (₱)", hourlyRateInput);
        updateRateLabel(payRateTypeInput, hourlyRateInput);
        addField(card, "Regular hours", regularHoursInput);
        addField(card, "Ordinary OT multiplier", overtimeMultiplierInput);
        addField(card, "Allowance (₱)", allowanceInput);
        addField(card, "Deduction (₱)", deductionInput);
        Button calculate = button("Calculate and save shift", true);
        calculate.setOnClickListener(v -> calculateAndSave());
        card.addView(calculate);
        resultView = text("Your pay breakdown will appear here after calculation.", 14, false);
        resultView.setPadding(0, dp(14), 0, 0);
        card.addView(resultView);
        section("Recent shifts");
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(historyContainer);
        refreshHistory();
    }

    private void buildExpensesScreen() {
        header("Expenses", "Record spending and see how it affects your cutoff.");
        expenseDateInput = field("Select expense date", LocalDate.now().toString(), InputType.TYPE_NULL);
        configureDatePicker(expenseDateInput);
        expenseCategoryInput = field("e.g. Food, Transport, Bills", "", InputType.TYPE_CLASS_TEXT);
        expenseAmountInput = field("0.00", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        expenseNoteInput = field("Optional note", "", InputType.TYPE_CLASS_TEXT);
        LinearLayout form = cardContainer();
        page.addView(form);
        addField(form, "Date", expenseDateInput);
        addField(form, "Category", expenseCategoryInput);
        addField(form, "Amount (₱)", expenseAmountInput);
        addField(form, "Note", expenseNoteInput);
        Button save = button("Save expense", true);
        save.setOnClickListener(v -> addExpense());
        form.addView(save);

        section("Current cutoff");
        budgetSummaryView = text("", 14, false);
        page.addView(budgetSummaryView);
        refreshBudget();
        section("Recent expenses");
        expenseHistoryContainer = new LinearLayout(this);
        expenseHistoryContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(expenseHistoryContainer);
        refreshExpenseHistory();
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
        section("About your data");
        TextView info = text("Your entries are stored locally on this device. Pay amounts are estimates calculated from the hours and rates you enter. Cloud sync and confirmed payroll reconciliation are not enabled yet.", 14, false);
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
        try {
            LocalDate date = LocalDate.parse(value(dateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime timeIn = LocalTime.parse(value(timeInInput), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime timeOut = LocalTime.parse(value(timeOutInput), DateTimeFormatter.ofPattern("HH:mm"));
            BigDecimal enteredRate = decimal(hourlyRateInput, "Pay rate");
            String rateType = payRateTypeInput == null ? pref("pay_rate_type", "Hourly") : String.valueOf(payRateTypeInput.getSelectedItem());
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
            saveSalaryPreferences(enteredRate, rateType, regularHours, multiplier, allowance, deduction);
            database.insertEntry(date.toString(), timeIn.toString(), timeOut.toString(),
                    serializedBreaks.isEmpty() ? "0h" : android.text.TextUtils.join(", ", serializedBreaks),
                    result, effectiveHourlyRate, multiplier);
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
            Toast.makeText(this, "Shift saved", Toast.LENGTH_SHORT).show();
        } catch (DateTimeParseException ex) {
            toast("Check date format (YYYY-MM-DD) and time format (HH:mm).");
        } catch (ArithmeticException ex) {
            toast("Regular hours must convert to a whole number of minutes.");
        } catch (IllegalArgumentException ex) {
            toast(ex.getMessage());
        } catch (Exception ex) {
            toast("Could not save shift: " + ex.getMessage());
        }
    }

    private void addExpense() {
        try {
            LocalDate date = LocalDate.parse(value(expenseDateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            String category = value(expenseCategoryInput);
            if (category.isEmpty()) throw new IllegalArgumentException("Expense category is required.");
            BigDecimal amount = decimal(expenseAmountInput, "Expense amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
            database.insertExpense(date.toString(), category, value(expenseNoteInput), amount);
            expenseAmountInput.setText("");
            expenseNoteInput.setText("");
            refreshBudget();
            refreshExpenseHistory();
            Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show();
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
        List<String> entries = database.getRecentEntries(8);
        if (entries.isEmpty()) {
            historyContainer.addView(emptyState("No shifts yet", "Your saved work shifts will appear here."));
            return;
        }
        for (String entry : entries) addListItem(historyContainer, entry);
    }

    private void refreshExpenseHistory() {
        if (expenseHistoryContainer == null) return;
        expenseHistoryContainer.removeAllViews();
        List<String> entries = database.getRecentExpenses(20);
        if (entries.isEmpty()) {
            expenseHistoryContainer.addView(emptyState("No expenses yet", "Add your first expense above."));
            return;
        }
        for (String entry : entries) addListItem(expenseHistoryContainer, entry);
    }

    private void refreshBudget() {
        if (budgetSummaryView == null) return;
        LocalDate today = LocalDate.now();
        LocalDate start = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(1) : today.withDayOfMonth(16);
        LocalDate end = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.withDayOfMonth(today.lengthOfMonth());
        LocalDate payday = getPaydayForCurrentCutoff(today);
        BigDecimal earned = database.getRecordedNetPayTotal(start.toString(), end.toString());
        BigDecimal spent = database.getExpenseTotal(start.toString(), end.toString());
        BigDecimal remaining = earned.subtract(spent);
        budgetSummaryView.setText("Cutoff  " + start + " to " + end
                + "\nPayday                 " + payday
                + "\nTotal recorded pay      " + money(earned)
                + "\nRecorded expenses      " + money(spent)
                + "\nRemaining after expenses " + money(remaining)
                + "\n\nPay total comes from saved work entries.");
        if (currentTab == 0) {
            // Dashboard cards are rebuilt on tab selection so their totals always refresh.
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
        LocalDate targetMonth = firstCutoff ? today : today.plusMonths(1);
        return targetMonth.withDayOfMonth(Math.min(Math.max(day, 1), targetMonth.lengthOfMonth()));
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
            String rateType = payRateTypeInput == null ? pref("pay_rate_type", "Hourly") : String.valueOf(payRateTypeInput.getSelectedItem());
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

    private Spinner createRateTypeSpinner() {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Hourly", "Daily"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection("Daily".equals(pref("pay_rate_type", "Hourly")) ? 1 : 0);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (hourlyRateInput != null) updateRateLabel(spinner, hourlyRateInput);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        return spinner;
    }

    private void updateRateLabel(Spinner spinner, EditText input) {
        if (spinner != null && input != null) input.setHint("Daily".equals(String.valueOf(spinner.getSelectedItem())) ? "Daily rate amount" : "Hourly rate amount");
    }

    private void configureDatePicker(EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setOnClickListener(v -> {
            LocalDate current;
            try { current = LocalDate.parse(value(input)); } catch (Exception ignored) { current = LocalDate.now(); }
            new DatePickerDialog(this, (picker, year, month, day) ->
                    input.setText(LocalDate.of(year, month + 1, day).toString()),
                    current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
        });
    }

    private void configureTimePicker(EditText input) {
        input.setFocusable(false);
        input.setClickable(true);
        input.setOnClickListener(v -> {
            LocalTime current;
            try { current = LocalTime.parse(value(input)); } catch (Exception ignored) { current = LocalTime.of(8, 0); }
            new TimePickerDialog(this, (picker, hour, minute) ->
                    input.setText(String.format(Locale.ROOT, "%02d:%02d", hour, minute)),
                    current.getHour(), current.getMinute(), true).show();
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

    private void actionButton(String title, String detail, int tab) {
        LinearLayout item = cardContainer();
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(text(title, 16, true));
        TextView desc = text(detail, 12, false);
        desc.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        words.addView(desc);
        item.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        item.addView(text("›", 26, false));
        item.setOnClickListener(v -> {
            currentTab = tab;
            buildBottomNav();
            showTab(tab);
        });
        page.addView(item);
    }

    private LinearLayout cardContainer() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        boolean night = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        bg.setColor(night ? 0xFF303038 : 0xFFF3F2F8);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), night ? 0xFF484852 : 0xFFE5E3EC);
        card.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private void addField(LinearLayout parent, String label, View input) {
        TextView caption = text(label, 12, true);
        caption.setTextColor(resolveColor(android.R.attr.textColorSecondary));
        caption.setPadding(0, dp(8), 0, dp(2));
        parent.addView(caption);
        parent.addView(input);
    }

    private EditText field(String hint, String initial, int inputType) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setHint(hint);
        edit.setText(initial);
        edit.setInputType(inputType);
        edit.setTextSize(16);
        edit.setPadding(dp(12), 0, dp(12), 0);
        edit.setSelectAllOnFocus(false);
        edit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(resolveColor(android.R.attr.colorAccent)));
        edit.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        return edit;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        if (primary) {
            button.setTextColor(0xFFFFFFFF);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(resolveColor(android.R.attr.colorAccent));
            bg.setCornerRadius(dp(16));
            button.setBackground(bg);
        }
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
        divider.setBackgroundColor(0x33888888);
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
