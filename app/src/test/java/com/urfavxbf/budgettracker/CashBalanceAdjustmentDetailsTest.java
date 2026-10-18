package com.urfavxbf.budgettracker;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class CashBalanceAdjustmentDetailsTest {
    @Test
    public void decreaseShowsPreviousBalanceAndNegativeChange() {
        BigDecimal newBalance = new BigDecimal("1200.00");
        BigDecimal change = new BigDecimal("-300.00");

        assertEquals(new BigDecimal("1500.00"),
                CashBalanceAdjustmentDetails.previousBalance(newBalance, change));
        assertEquals(new BigDecimal("-300.00"), CashBalanceAdjustmentDetails.change(change));
    }

    @Test
    public void increaseShowsPreviousBalanceAndPositiveChange() {
        BigDecimal newBalance = new BigDecimal("1500.00");
        BigDecimal change = new BigDecimal("300.00");

        assertEquals(new BigDecimal("1200.00"),
                CashBalanceAdjustmentDetails.previousBalance(newBalance, change));
        assertEquals(new BigDecimal("300.00"), CashBalanceAdjustmentDetails.change(change));
    }

    @Test
    public void unchangedBalanceShowsZeroChange() {
        BigDecimal newBalance = new BigDecimal("1200.00");
        BigDecimal change = BigDecimal.ZERO;

        assertEquals(new BigDecimal("1200.00"),
                CashBalanceAdjustmentDetails.previousBalance(newBalance, change));
        assertEquals(new BigDecimal("0.00"), CashBalanceAdjustmentDetails.change(change));
    }

    @Test
    public void valuesAreRoundedToCurrencyPrecision() {
        assertEquals(new BigDecimal("1500.00"),
                CashBalanceAdjustmentDetails.previousBalance(
                        new BigDecimal("1200.004"), new BigDecimal("-299.996")));
    }
}
