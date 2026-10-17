package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class PhilippinePayrollCalculatorTest {
    @Test
    public void ordinaryDayOvertimeUsesTwentyFivePercentPremium() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 120, 0, 0,
                PhilippinePayrollCalculator.DayType.ORDINARY, false);
        assertEquals(new BigDecimal("800.00"), result.regularPay);
        assertEquals(new BigDecimal("250.00"), result.overtimePay);
        assertEquals(new BigDecimal("1050.00"), result.totalWage);
    }

    @Test
    public void nightDifferentialAddsTenPercentToApplicableHourlyRate() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 60, 120, 60,
                PhilippinePayrollCalculator.DayType.ORDINARY, false);
        assertEquals(new BigDecimal("20.00"), result.regularNightDifferential);
        assertEquals(new BigDecimal("12.50"), result.overtimeNightDifferential);
        assertEquals(new BigDecimal("1032.50"), result.totalWage);
    }

    @Test
    public void calculatesRegularHolidayAndRestDayRates() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 60, 0, 0,
                PhilippinePayrollCalculator.DayType.REGULAR_HOLIDAY, true);
        assertEquals(new BigDecimal("2080.00"), result.regularPay);
        assertEquals(new BigDecimal("338.00"), result.overtimePay);
        assertEquals(new BigDecimal("2418.00"), result.totalWage);
    }

    @Test
    public void calculatesSpecialNonWorkingDayRates() {
        PhilippinePayrollCalculator.Result result = PhilippinePayrollCalculator.calculate(
                new BigDecimal("100"), 480, 60, 0, 0,
                PhilippinePayrollCalculator.DayType.SPECIAL_NON_WORKING_DAY, false);
        assertEquals(new BigDecimal("1040.00"), result.regularPay);
        assertEquals(new BigDecimal("169.00"), result.overtimePay);
        assertEquals(new BigDecimal("1209.00"), result.totalWage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNightMinutesGreaterThanWorkedMinutes() {
        PhilippinePayrollCalculator.calculate(new BigDecimal("100"), 480, 0, 481, 0,
                PhilippinePayrollCalculator.DayType.ORDINARY, false);
    }
}
