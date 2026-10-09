package com.urfavxbf.budgettracker;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
    private final List<BreakInput> breakInputs = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout breakContainer;
    private LinearLayout historyContainer;
    private EditText dateInput;
    private EditText timeInInput;
    private EditText timeOutInput;
    private EditText hourlyRateInput;
    private EditText regularHoursInput;
    private EditText overtimeMultiplierInput;
    private EditText allowanceInput;
    private EditText deductionInput;
    private TextView resultView;
    private TextView budgetSummaryView;
    private LinearLayout expenseHistoryContainer;
    private EditText expenseDateInput;
    private EditText expenseCategoryInput;
    private EditText expenseAmountInput;
    private EditText expenseNoteInput;
    private WorkDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new WorkDatabase(this);
        buildUi();
        addBreakRow("12:00", "13:00");
        refreshHistory();
        refreshBudget();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(32));
        scrollView.setFillViewport(true);
        scrollView.addView(root);
        setContentView(scrollView);

        TextView title = text("Budget Tracker", 28, true);
        root.addView(title);
        TextView subtitle = text("Salary, attendance, and payday budget", 14, false);
        subtitle.setAlpha(0.75f);
        root.addView(subtitle);

        section("Daily work entry");
        dateInput = field("Work date (YYYY-MM-DD)", LocalDate.now().toString(), InputType.TYPE_CLASS_DATETIME);
        root.addView(dateInput);
        timeInInput = field("Time in (HH:mm)", "08:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(timeInInput);
        timeOutInput = field("Time out (HH:mm)", "18:00", InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        root.addView(timeOutInput);

        TextView breakTitle = text("Break periods", 18, true);
        breakTitle.setPadding(0, dp(12), 0, dp(4));
        root.addView(breakTitle);
        breakContainer = new LinearLayout(this);
        breakContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(breakContainer);
        Button addBreakButton = button("＋ Add break period");
        addBreakButton.setOnClickListener(v -> addBreakRow("", ""));
        root.addView(addBreakButton);

        section("Salary settings");
        hourlyRateInput = field("Hourly rate (₱)", "100.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(hourlyRateInput);
        regularHoursInput = field("Regular hours per workday", "8", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(regularHoursInput);
        overtimeMultiplierInput = field("Ordinary OT multiplier", "1.25", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(overtimeMultiplierInput);
        allowanceInput = field("Daily allowance (₱)", "0.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(allowanceInput);
        deductionInput = field("Daily deduction (₱)", "0.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(deductionInput);

        Button calculateButton = button("Calculate daily pay");
        calculateButton.setOnClickListener(v -> calculateAndSave());
        root.addView(calculateButton);

        resultView = text("", 16, false);
        resultView.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.addView(resultView);

        section("Recent work entries");
        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyContainer);
        TextView note = text("Local-first MVP. Salary estimates are not confirmed payroll payments.", 12, false);
        note.setAlpha(0.7f);
        note.setPadding(0, dp(20), 0, 0);
        root.addView(note);

        section("Current cutoff budget");
        budgetSummaryView = text("Calculating cutoff...", 16, false);
        budgetSummaryView.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(budgetSummaryView);

        section("Add expense");
        expenseDateInput = field("Expense date (YYYY-MM-DD)", LocalDate.now().toString(), InputType.TYPE_CLASS_DATETIME);
        root.addView(expenseDateInput);
        expenseCategoryInput = field("Category (e.g. Food, Transport)", "", InputType.TYPE_CLASS_TEXT);
        root.addView(expenseCategoryInput);
        expenseAmountInput = field("Amount (₱)", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(expenseAmountInput);
        expenseNoteInput = field("Note (optional)", "", InputType.TYPE_CLASS_TEXT);
        root.addView(expenseNoteInput);
        Button addExpenseButton = button("Save expense");
        addExpenseButton.setOnClickListener(v -> addExpense());
        root.addView(addExpenseButton);

        section("Recent expenses");
        expenseHistoryContainer = new LinearLayout(this);
        expenseHistoryContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(expenseHistoryContainer);
    }

    private void addBreakRow(String start, String end) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        EditText breakStart = field("Break start (HH:mm)", start,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        EditText breakEnd = field("Break end (HH:mm)", end,
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        row.addView(breakStart, new LinearLayout.LayoutParams(0, dp(58), 1f));
        row.addView(breakEnd, new LinearLayout.LayoutParams(0, dp(58), 1f));
        Button remove = new Button(this);
        remove.setText("×");
        remove.setMinWidth(dp(40));
        row.addView(remove, new LinearLayout.LayoutParams(dp(48), dp(52)));
        BreakInput input = new BreakInput(row, breakStart, breakEnd);
        remove.setOnClickListener(v -> {
            if (breakInputs.size() == 1) {
                breakStart.setText("");
                breakEnd.setText("");
                return;
            }
            breakInputs.remove(input);
            breakContainer.removeView(row);
        });
        breakInputs.add(input);
        breakContainer.addView(row);
    }

    private void calculateAndSave() {
        try {
            LocalDate date = LocalDate.parse(value(dateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime timeIn = LocalTime.parse(value(timeInInput), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime timeOut = LocalTime.parse(value(timeOutInput), DateTimeFormatter.ofPattern("HH:mm"));
            BigDecimal rate = decimal(hourlyRateInput, "Hourly rate");
            BigDecimal regularHours = decimal(regularHoursInput, "Regular hours");
            BigDecimal multiplier = decimal(overtimeMultiplierInput, "OT multiplier");
            BigDecimal allowance = decimal(allowanceInput, "Allowance");
            BigDecimal deduction = decimal(deductionInput, "Deduction");

            int regularMinutes = regularHours.multiply(BigDecimal.valueOf(60)).intValueExact();
            List<BreakInterval> breaks = new ArrayList<>();
            List<String> serializedBreaks = new ArrayList<>();
            for (BreakInput item : breakInputs) {
                String startText = value(item.start);
                String endText = value(item.end);
                if (startText.isEmpty() && endText.isEmpty()) {
                    continue;
                }
                if (startText.isEmpty() || endText.isEmpty()) {
                    throw new IllegalArgumentException("Complete both start and end for each break.");
                }
                LocalTime start = LocalTime.parse(startText, DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime end = LocalTime.parse(endText, DateTimeFormatter.ofPattern("HH:mm"));
                breaks.add(new BreakInterval(start, end));
                serializedBreaks.add(startText + "-" + endText);
            }

            SalaryCalculator.Result result = SalaryCalculator.calculate(timeIn, timeOut, breaks,
                    regularMinutes, rate, multiplier, allowance, deduction);
            String breakText = serializedBreaks.isEmpty() ? "None" : android.text.TextUtils.join("; ", serializedBreaks);
            database.insertEntry(date.toString(), timeIn.toString(), timeOut.toString(),
                    breakText, result, rate, multiplier);

            resultView.setText(String.format(Locale.getDefault(),
                    "DAILY PAY ESTIMATE\nShift: %s\nBreak: %s\nNet work: %s\nRegular: %s\nOvertime: %s\nRegular pay: ₱%s\nOT pay: ₱%s\nAllowance: ₱%s\nDeduction: ₱%s\nGross pay: ₱%s\nEstimated net pay: ₱%s",
                    duration(result.shiftMinutes), duration(result.breakMinutes), duration(result.netWorkMinutes),
                    duration(result.regularMinutes), duration(result.overtimeMinutes),
                    result.regularPay.toPlainString(), result.overtimePay.toPlainString(),
                    result.allowance.toPlainString(), result.deduction.toPlainString(),
                    result.grossPay.toPlainString(), result.estimatedNetPay.toPlainString()));
            refreshHistory();
            refreshBudget();
            Toast.makeText(this, "Daily work entry saved", Toast.LENGTH_SHORT).show();
        } catch (DateTimeParseException ex) {
            Toast.makeText(this, "Check the date (YYYY-MM-DD) and times (HH:mm).", Toast.LENGTH_LONG).show();
        } catch (ArithmeticException ex) {
            Toast.makeText(this, "Regular hours must be a valid number of minutes.", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Could not save entry: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshHistory() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews();
        List<String> entries = database.getRecentEntries(20);
        if (entries.isEmpty()) {
            TextView empty = text("No saved work entries yet.", 14, false);
            historyContainer.addView(empty);
            return;
        }
        for (String entry : entries) {
            TextView item = text(entry, 14, false);
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            historyContainer.addView(item);
            View divider = new View(this);
            divider.setBackgroundColor(0x33888888);
            historyContainer.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        }
    }

    private void addExpense() {
        try {
            LocalDate date = LocalDate.parse(value(expenseDateInput), DateTimeFormatter.ISO_LOCAL_DATE);
            String category = value(expenseCategoryInput);
            if (category.isEmpty()) {
                throw new IllegalArgumentException("Expense category is required.");
            }
            BigDecimal amount = decimal(expenseAmountInput, "Expense amount");
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Expense amount must be greater than zero.");
            }
            database.insertExpense(date.toString(), category, value(expenseNoteInput), amount);
            expenseAmountInput.setText("");
            expenseNoteInput.setText("");
            refreshBudget();
            Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show();
        } catch (DateTimeParseException ex) {
            Toast.makeText(this, "Use YYYY-MM-DD for the expense date.", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Could not save expense: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshBudget() {
        if (budgetSummaryView == null) return;
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        if (today.getDayOfMonth() <= 15) {
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(15);
        } else {
            start = today.withDayOfMonth(16);
            end = today.withDayOfMonth(today.lengthOfMonth());
        }
        BigDecimal estimatedEarnings = database.getEstimatedNetPayTotal(start.toString(), end.toString());
        BigDecimal expenses = database.getExpenseTotal(start.toString(), end.toString());
        BigDecimal projectedRemaining = estimatedEarnings.subtract(expenses);
        budgetSummaryView.setText(String.format(Locale.getDefault(),
                "Cutoff: %s to %s\\nRecorded estimated net earnings: ₱%s\\nRecorded expenses: ₱%s\\nProjected difference: ₱%s\\n\\nThis is a projection based on saved entries, not confirmed cash on hand.",
                start, end, estimatedEarnings.toPlainString(), expenses.toPlainString(),
                projectedRemaining.toPlainString()));

        if (expenseHistoryContainer != null) {
            expenseHistoryContainer.removeAllViews();
            List<String> expensesList = database.getRecentExpenses(20);
            if (expensesList.isEmpty()) {
                expenseHistoryContainer.addView(text("No expenses recorded yet.", 14, false));
            } else {
                for (String expense : expensesList) {
                    TextView item = text(expense, 14, false);
                    item.setPadding(dp(12), dp(10), dp(12), dp(10));
                    expenseHistoryContainer.addView(item);
                    View divider = new View(this);
                    divider.setBackgroundColor(0x33888888);
                    expenseHistoryContainer.addView(divider, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
                }
            }
        }
    }

    private void section(String label) {
        TextView view = text(label, 20, true);
        view.setPadding(0, dp(24), 0, dp(8));
        root.addView(view);
    }

    private EditText field(String hint, String initial, int inputType) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setHint(hint);
        editText.setText(initial);
        editText.setInputType(inputType);
        editText.setTextSize(16);
        editText.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        params.bottomMargin = dp(4);
        editText.setLayoutParams(params);
        return editText;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.topMargin = dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private TextView text(String value, int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
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

    private static String duration(int minutes) {
        return String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class BreakInput {
        final View row;
        final EditText start;
        final EditText end;

        BreakInput(View row, EditText start, EditText end) {
            this.row = row;
            this.start = start;
            this.end = end;
        }
    }
}
