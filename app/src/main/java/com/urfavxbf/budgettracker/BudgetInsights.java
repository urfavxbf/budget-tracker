package com.urfavxbf.budgettracker;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure budget calculations shared by the budget summary and unit tests. */
public final class BudgetInsights {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");

    private BudgetInsights() {}

    public static BigDecimal resolveBudgetBase(BigDecimal cycleBudget, BigDecimal spendingLimit) {
        BigDecimal cycle = normalize(cycleBudget);
        BigDecimal limit = normalize(spendingLimit);
        if (cycle.signum() < 0) cycle = ZERO;
        if (limit.signum() < 0) limit = ZERO;

        BigDecimal base = cycle.signum() > 0 ? cycle : limit;
        if (limit.signum() > 0 && base.signum() > 0) {
            base = base.min(limit);
        }
        return normalize(base);
    }

    public static Status classify(BigDecimal spent, BigDecimal budgetBase) {
        BigDecimal totalSpent = normalize(spent);
        BigDecimal base = normalize(budgetBase);
        if (base.signum() <= 0) return Status.NO_BUDGET;
        if (totalSpent.compareTo(base) > 0) return Status.OVER_BUDGET;
        if (totalSpent.compareTo(base.multiply(WARNING_THRESHOLD)) >= 0) return Status.WARNING;
        return Status.WITHIN_BUDGET;
    }

    public static boolean hasDailyGuide(BigDecimal budgetBase, BigDecimal availableCash,
                                        long daysUntilPayday) {
        return daysUntilPayday > 0
                && (normalize(budgetBase).signum() > 0 || normalize(availableCash).signum() > 0);
    }

    public static BigDecimal dailySpendingGuide(BigDecimal budgetBase, BigDecimal spent,
                                                 BigDecimal availableCash, long daysUntilPayday) {
        if (daysUntilPayday <= 0) return ZERO;
        BigDecimal base = normalize(budgetBase);
        BigDecimal totalSpent = normalize(spent);
        BigDecimal available = normalize(availableCash);
        if (base.signum() > 0) {
            BigDecimal remainingBudget = base.subtract(totalSpent).max(BigDecimal.ZERO);
            BigDecimal spendable = available.max(BigDecimal.ZERO);
            return remainingBudget.min(spendable)
                    .divide(BigDecimal.valueOf(daysUntilPayday), 2, RoundingMode.DOWN);
        }
        if (available.signum() > 0) {
            return available.divide(BigDecimal.valueOf(daysUntilPayday), 2, RoundingMode.DOWN);
        }
        return ZERO;
    }

    /** Returns the savings needed for the portion of an expense beyond available cash. */
    public static BigDecimal savingsDrawForExpense(BigDecimal availableCash, BigDecimal savings, BigDecimal expenseAmount) {
        BigDecimal available = normalize(availableCash).max(BigDecimal.ZERO);
        BigDecimal saved = normalize(savings).max(BigDecimal.ZERO);
        BigDecimal expense = normalize(expenseAmount).max(BigDecimal.ZERO);
        return expense.subtract(available).max(BigDecimal.ZERO).min(saved).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public enum Status {
        NO_BUDGET,
        WITHIN_BUDGET,
        WARNING,
        OVER_BUDGET
    }
}
