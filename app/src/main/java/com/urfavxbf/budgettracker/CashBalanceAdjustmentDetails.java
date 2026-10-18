package com.urfavxbf.budgettracker;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure balance math for rendering a cash reconciliation record. */
public final class CashBalanceAdjustmentDetails {
    private CashBalanceAdjustmentDetails() { }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** The stored target is the reconciled balance; adjustment is target minus previous balance. */
    public static BigDecimal previousBalance(BigDecimal targetBalance, BigDecimal adjustment) {
        return normalize(targetBalance).subtract(normalize(adjustment)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal change(BigDecimal adjustment) {
        return normalize(adjustment);
    }
}
