package com.urfavxbf.budgettracker;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Shared MPAndroidChart builders for finance visualizations. Charts only display saved app data. */
final class FinanceCharts {
    private static final int INCOME = Color.rgb(43, 153, 115);
    private static final int EXPENSE = Color.rgb(220, 90, 87);
    private static final int BUDGET = Color.rgb(78, 126, 207);
    private static final int[] PALETTE = {
            Color.rgb(78, 126, 207), Color.rgb(142, 102, 194),
            Color.rgb(43, 153, 115), Color.rgb(230, 163, 74),
            Color.rgb(220, 90, 87), Color.rgb(63, 156, 174),
            Color.rgb(126, 139, 153), Color.rgb(184, 120, 95)
    };

    private FinanceCharts() {}

    static LineChart monthlyIncomeExpense(Context context, WorkDatabase database) {
        LineChart chart = new LineChart(context);
        chart.setNoDataText("Add income and expenses to see your monthly trend.");
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 4));
        chart.setTouchEnabled(true);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);

        List<String> labels = new ArrayList<>();
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        Map<YearMonth, BigDecimal> incomeByMonth = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> expenseByMonth = new LinkedHashMap<>();
        YearMonth thisMonth = YearMonth.now();
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM", Locale.getDefault());
        for (int i = 5; i >= 0; i--) {
            YearMonth month = thisMonth.minusMonths(i);
            labels.add(month.format(labelFormat));
            incomeByMonth.put(month, BigDecimal.ZERO);
            expenseByMonth.put(month, BigDecimal.ZERO);
        }

        for (WorkDatabase.WorkEntry item : database.getRecentWorkEntries(2000)) {
            try {
                YearMonth month = YearMonth.from(LocalDate.parse(item.date));
                if (incomeByMonth.containsKey(month)) {
                    incomeByMonth.put(month, incomeByMonth.get(month).add(new BigDecimal(item.netPay)));
                }
            } catch (RuntimeException ignored) { /* Skip malformed legacy rows. */ }
        }
        for (WorkDatabase.ExpenseEntry item : database.getRecentExpenseEntries(2000)) {
            try {
                YearMonth month = YearMonth.from(LocalDate.parse(item.date));
                if (expenseByMonth.containsKey(month)) {
                    expenseByMonth.put(month, expenseByMonth.get(month).add(new BigDecimal(item.amount)));
                }
            } catch (RuntimeException ignored) { /* Skip malformed legacy rows. */ }
        }

        int index = 0;
        for (YearMonth month : incomeByMonth.keySet()) {
            incomeEntries.add(new Entry(index, incomeByMonth.get(month).floatValue()));
            expenseEntries.add(new Entry(index, expenseByMonth.get(month).floatValue()));
            index++;
        }
        LineDataSet income = new LineDataSet(incomeEntries, "Income");
        styleLine(income, INCOME);
        LineDataSet expenses = new LineDataSet(expenseEntries, "Expenses");
        styleLine(expenses, EXPENSE);
        chart.setData(new LineData(income, expenses));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setLabelRotationAngle(-25f);
        styleAxis(chart.getXAxis(), context);
        styleAxis(chart.getAxisLeft(), context);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(11f);
        chart.animateX(450);
        chart.setContentDescription("Line chart comparing recorded estimated income and expenses for the last six months.");
        return chart;
    }

    private static void styleLine(LineDataSet set, int color) {
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(2.2f);
        set.setCircleRadius(3.5f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setHighlightEnabled(true);
    }

    static PieChart categoryDonut(Context context, Map<String, BigDecimal> categoryTotals) {
        PieChart chart = new PieChart(context);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Category spending will appear here after you add expenses.");
        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(62f);
        chart.setTransparentCircleRadius(66f);
        chart.setEntryLabelTextSize(10f);
        chart.setEntryLabelColor(themeColor(context, android.R.attr.textColorPrimary, Color.DKGRAY));
        chart.setCenterText("Spending\nby category");
        chart.setCenterTextSize(13f);
        chart.setDrawEntryLabels(false);
        List<PieEntry> entries = new ArrayList<>();
        if (categoryTotals != null) {
            List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(categoryTotals.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            BigDecimal other = BigDecimal.ZERO;
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<String, BigDecimal> item = sorted.get(i);
                if (item.getValue() == null || item.getValue().signum() <= 0) continue;
                if (entries.size() < 6) entries.add(new PieEntry(item.getValue().floatValue(), item.getKey()));
                else other = other.add(item.getValue());
            }
            if (other.signum() > 0) entries.add(new PieEntry(other.floatValue(), "Other"));
        }
        if (!entries.isEmpty()) {
            PieDataSet dataSet = new PieDataSet(entries, "");
            List<Integer> colors = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) colors.add(PALETTE[i % PALETTE.length]);
            dataSet.setColors(colors);
            dataSet.setSliceSpace(2f);
            dataSet.setSelectionShift(4f);
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.WHITE);
            chart.setData(new PieData(dataSet));
        }
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setTextSize(10f);
        chart.setExtraOffsets(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
        chart.animateY(450);
        chart.setContentDescription("Donut chart showing recorded expenses by category.");
        return chart;
    }

    static BarChart categoryBudgetComparison(Context context, List<String> categoryNames,
                                              Map<String, BigDecimal> actual,
                                              Map<String, BigDecimal> limits) {
        BarChart chart = new BarChart(context);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Add expenses or category limits to compare spending.");
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        chart.setFitBars(true);
        List<String> labels = new ArrayList<>();
        List<BarEntry> actualEntries = new ArrayList<>();
        List<BarEntry> limitEntries = new ArrayList<>();
        int index = 0;
        if (categoryNames != null) {
            for (String name : categoryNames) {
                BigDecimal spent = actual == null ? null : actual.get(name);
                BigDecimal limit = limits == null ? null : limits.get(name);
                if ((spent == null || spent.signum() <= 0) && (limit == null || limit.signum() <= 0)) continue;
                labels.add(name == null || name.trim().isEmpty() ? "Other" : name);
                actualEntries.add(new BarEntry(index, spent == null ? 0f : Math.max(0f, spent.floatValue())));
                limitEntries.add(new BarEntry(index, limit == null ? 0f : Math.max(0f, limit.floatValue())));
                index++;
            }
        }
        if (!actualEntries.isEmpty()) {
            BarDataSet actualSet = new BarDataSet(actualEntries, "Actual");
            actualSet.setColor(EXPENSE);
            actualSet.setDrawValues(false);
            BarDataSet limitSet = new BarDataSet(limitEntries, "Limit");
            limitSet.setColor(BUDGET);
            limitSet.setDrawValues(false);
            BarData data = new BarData(actualSet, limitSet);
            data.setBarWidth(0.34f);
            chart.setData(data);
            chart.getXAxis().setAxisMinimum(0f);
            chart.getXAxis().setAxisMaximum(labels.size());
            chart.getXAxis().setCenterAxisLabels(true);
            chart.getXAxis().setGranularity(1f);
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chart.getXAxis().setLabelRotationAngle(-30f);
            float groupSpace = 0.12f;
            float barSpace = 0.02f;
            chart.getXAxis().setAxisMinimum(0f);
            chart.getXAxis().setAxisMaximum(labels.size());
            data.groupBars(0f, groupSpace, barSpace);
        }
        styleAxis(chart.getXAxis(), context);
        styleAxis(chart.getAxisLeft(), context);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(11f);
        chart.setExtraOffsets(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        chart.animateY(450);
        chart.setContentDescription("Grouped bar chart comparing actual category expenses with saved category limits.");
        return chart;
    }

    static BarChart savingsGoalComparison(Context context, List<WorkDatabase.SavingsGoal> goals) {
        BarChart chart = new BarChart(context);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Create a savings goal to track progress here.");
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);
        chart.setPinchZoom(false);
        chart.setScaleEnabled(false);
        List<String> labels = new ArrayList<>();
        List<BarEntry> savedEntries = new ArrayList<>();
        List<BarEntry> targetEntries = new ArrayList<>();
        if (goals != null) {
            int index = 0;
            for (WorkDatabase.SavingsGoal goal : goals) {
                labels.add(goal.name == null || goal.name.trim().isEmpty() ? "Goal " + (index + 1) : goal.name);
                savedEntries.add(new BarEntry(index, Math.max(0f, goal.savedAmount.floatValue())));
                targetEntries.add(new BarEntry(index, Math.max(0f, goal.targetAmount.floatValue())));
                index++;
            }
        }
        if (!savedEntries.isEmpty()) {
            BarDataSet saved = new BarDataSet(savedEntries, "Saved");
            saved.setColor(INCOME);
            saved.setDrawValues(false);
            BarDataSet target = new BarDataSet(targetEntries, "Target");
            target.setColor(BUDGET);
            target.setDrawValues(false);
            BarData data = new BarData(saved, target);
            data.setBarWidth(0.34f);
            chart.setData(data);
            chart.getXAxis().setCenterAxisLabels(true);
            chart.getXAxis().setGranularity(1f);
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            data.groupBars(0f, 0.12f, 0.02f);
        }
        styleAxis(chart.getXAxis(), context);
        styleAxis(chart.getAxisLeft(), context);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setTextSize(11f);
        chart.setExtraOffsets(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        chart.animateY(450);
        chart.setContentDescription("Grouped bar chart comparing saved amounts with target amounts for each savings goal.");
        return chart;
    }

    private static void styleAxis(XAxis axis, Context context) {
        axis.setTextColor(themeColor(context, android.R.attr.textColorSecondary, Color.GRAY));
        axis.setTextSize(10f);
        axis.setDrawGridLines(false);
        axis.setDrawAxisLine(false);
    }

    private static void styleAxis(YAxis axis, Context context) {
        axis.setTextColor(themeColor(context, android.R.attr.textColorSecondary, Color.GRAY));
        axis.setTextSize(10f);
        axis.setDrawGridLines(true);
        axis.setGridColor(0x22808080);
        axis.setDrawAxisLine(false);
    }

    private static int themeColor(Context context, int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) {
                try { return context.getResources().getColor(value.resourceId, context.getTheme()); }
                catch (RuntimeException ignored) { return fallback; }
            }
            return value.data;
        }
        return fallback;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
