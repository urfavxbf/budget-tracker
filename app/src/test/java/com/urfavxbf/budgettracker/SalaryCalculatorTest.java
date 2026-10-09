package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class SalaryCalculatorTest {
    @Test
    public void calculatesRegularPayOvertimeAndBreak() {
        SalaryCalculator.Result result = SalaryCalculator.calculate(
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                Collections.singletonList(new BreakInterval(LocalTime.of(12, 0), LocalTime.of(13, 0))),
                8 * 60, new BigDecimal("100"), new BigDecimal("1.25"),
                BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(60, result.breakMinutes);
        assertEquals(540, result.netWorkMinutes);
        assertEquals(480, result.regularMinutes);
        assertEquals(60, result.overtimeMinutes);
        assertEquals(new BigDecimal("800.00"), result.regularPay);
        assertEquals(new BigDecimal("125.00"), result.overtimePay);
        assertEquals(new BigDecimal("925.00"), result.grossPay);
    }

    @Test
    public void handlesOvernightShiftAndBreak() {
        SalaryCalculator.Result result = SalaryCalculator.calculate(
                LocalTime.of(22, 0), LocalTime.of(6, 0),
                Collections.singletonList(new BreakInterval(LocalTime.of(2, 0), LocalTime.of(2, 30))),
                8 * 60, new BigDecimal("100"), new BigDecimal("1.25"),
                BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(480, result.shiftMinutes);
        assertEquals(30, result.breakMinutes);
        assertEquals(450, result.netWorkMinutes);
        assertEquals(0, result.overtimeMinutes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOverlappingBreaks() {
        SalaryCalculator.calculate(
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                Arrays.asList(
                        new BreakInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
                        new BreakInterval(LocalTime.of(12, 30), LocalTime.of(13, 30))),
                8 * 60, new BigDecimal("100"), new BigDecimal("1.25"),
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
