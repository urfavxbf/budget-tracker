package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class BudgetInsightsTest {
    @Test
    public void usesTheLowerOfSalaryCycleBudgetAndConfiguredSpendingLimit() {
        assertEquals(new BigDecimal("450.00"),
                BudgetInsights.resolveBudgetBase(new BigDecimal("600.00"), new BigDecimal("450.00")));
    }

    @Test
    public void usesConfiguredSpendingLimitWhenNoSalaryCycleBudgetExists() {
        assertEquals(new BigDecimal("450.00"),
                BudgetInsights.resolveBudgetBase(BigDecimal.ZERO, new BigDecimal("450.00")));
    }

    @Test
    public void warnsAtExactlyEightyPercentOfBudget() {
        assertEquals(BudgetInsights.Status.WARNING,
                BudgetInsights.classify(new BigDecimal("400.00"), new BigDecimal("500.00")));
    }

    @Test
    public void remainsWithinBudgetBelowEightyPercent() {
        assertEquals(BudgetInsights.Status.WITHIN_BUDGET,
                BudgetInsights.classify(new BigDecimal("399.99"), new BigDecimal("500.00")));
    }

    @Test
    public void classifiesSpendingAboveBudgetAsOverBudget() {
        assertEquals(BudgetInsights.Status.OVER_BUDGET,
                BudgetInsights.classify(new BigDecimal("500.01"), new BigDecimal("500.00")));
    }

    @Test
    public void calculatesDailyGuideWithoutAllowingNegativeRemainingBudget() {
        assertEquals(new BigDecimal("0.00"),
                BudgetInsights.dailySpendingGuide(new BigDecimal("500.00"),
                        new BigDecimal("550.00"), new BigDecimal("700.00"), 5));
    }

    @Test
    public void capsDailyGuideAtAvailableCashAfterSavings() {
        assertEquals(new BigDecimal("150.00"),
                BudgetInsights.dailySpendingGuide(new BigDecimal("1000.00"),
                        new BigDecimal("100.00"), new BigDecimal("300.00"), 2));
    }

    @Test
    public void doesNotRecommendSpendingWhenAvailableCashIsNegative() {
        assertEquals(new BigDecimal("0.00"),
                BudgetInsights.dailySpendingGuide(new BigDecimal("500.00"),
                        new BigDecimal("100.00"), new BigDecimal("-50.00"), 5));
    }

    @Test
    public void fallsBackToAvailableCashWhenNoBudgetIsConfigured() {
        assertEquals(new BigDecimal("140.00"),
                BudgetInsights.dailySpendingGuide(BigDecimal.ZERO, new BigDecimal("50.00"),
                        new BigDecimal("700.00"), 5));
    }
}
