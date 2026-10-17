package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import org.junit.Test;

public class PhilippinePayrollCalculatorTest {
    @Test public void ordinaryDayOvertimeIsPaidAt125Percent() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 120, 0, 0,
                PhilippinePayrollCalculator.DayType.ORDINARY, false);
        assertEquals(new BigDecimal("800.00"), result.regularPay);
        assertEquals(new BigDecimal("250.00"), result.overtimePay);
        assertEquals(new BigDecimal("1050.00"), result.totalWage);
    }

    @Test public void addsNightDifferentialToRegularAndOvertimeHours() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 60, 120, 30,
                PhilippinePayrollCalculator.DayType.ORDINARY, false);
        assertEquals(new BigDecimal("20.00"), result.regularNightDifferential);
        assertEquals(new BigDecimal("12.50"), result.overtimeNightDifferential);
        assertEquals(new BigDecimal("957.50"), result.totalWage);
    }

    @Test public void appliesHolidayAndRestDayMultipliers() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 60, 0, 0,
                PhilippinePayrollCalculator.DayType.REGULAR_HOLIDAY, true);
        assertEquals(new BigDecimal("2080.00"), result.regularPay);
        assertEquals(new BigDecimal("338.00"), result.overtimePay);
        assertEquals(new BigDecimal("2418.00"), result.totalWage);
    }

    @Test public void rejectsNightMinutesAboveWorkedMinutes() {
        assertThrows(IllegalArgumentException.class, () -> PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 60, 481, 0,
                PhilippinePayrollCalculator.DayType.ORDINARY, false));
    }
}