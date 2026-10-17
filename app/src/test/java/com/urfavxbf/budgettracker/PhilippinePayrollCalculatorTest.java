package com.urfavxbf.budgettracker;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Philippine minimum-rate premium calculator for covered employees.
 *
 * This class calculates wage components only. It does not determine legal coverage,
 * holiday eligibility/no-work-no-pay conditions, company CBA rates, or statutory
 * deductions. Callers must supply the correctly classified minutes for each shift.
 */
public final class PhilippinePayrollCalculator {
    private static final BigDecimal ORDINARY_OT = new BigDecimal("1.25");
    private static final BigDecimal NIGHT_DIFFERENTIAL = new BigDecimal("0.10");
    private static final BigDecimal ONE = BigDecimal.ONE;

    private PhilippinePayrollCalculator() {}

    public enum DayType {
        ORDINARY,
        REGULAR_HOLIDAY,
        SPECIAL_NON_WORKING_DAY
    }

    public static Result calculate(
            BigDecimal hourlyRate,
            int regularMinutes,
            int overtimeMinutes,
            int regularNightMinutes,
            int overtimeNightMinutes,
            DayType dayType,
            boolean restDay) {
        if (hourlyRate == null || hourlyRate.signum() < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative or missing.");
        }
        if (regularMinutes < 0 || overtimeMinutes < 0
                || regularNightMinutes < 0 || overtimeNightMinutes < 0) {
            throw new IllegalArgumentException("Work minutes cannot be negative.");
        }
        if (regularNightMinutes > regularMinutes || overtimeNightMinutes > overtimeMinutes) {
            throw new IllegalArgumentException("Night minutes cannot exceed their corresponding work minutes.");
        }
        if (dayType == null) throw new IllegalArgumentException("Day type is required.");

        BigDecimal regularFactor = regularFactor(dayType, restDay);
        BigDecimal overtimeFactor = overtimeFactor(dayType, restDay);
        BigDecimal regularPay = wage(hourlyRate, regularMinutes, regularFactor);
        BigDecimal overtimePay = wage(hourlyRate, overtimeMinutes, overtimeFactor);
        BigDecimal regularNightPay = wage(hourlyRate, regularNightMinutes,
                regularFactor.multiply(NIGHT_DIFFERENTIAL));
        BigDecimal overtimeNightPay = wage(hourlyRate, overtimeNightMinutes,
                overtimeFactor.multiply(NIGHT_DIFFERENTIAL));
        BigDecimal total = regularPay.add(overtimePay)
                .add(regularNightPay).add(overtimeNightPay)
                .setScale(2, RoundingMode.HALF_UP);

        return new Result(regularPay, overtimePay, regularNightPay, overtimeNightPay, total,
                regularFactor, overtimeFactor);
    }

    private static BigDecimal regularFactor(DayType type, boolean restDay) {
        switch (type) {
            case REGULAR_HOLIDAY:
                return restDay ? new BigDecimal("2.60") : new BigDecimal("2.00");
            case SPECIAL_NON_WORKING_DAY:
                return restDay ? new BigDecimal("1.50") : new BigDecimal("1.30");
            case ORDINARY:
            default:
                return restDay ? new BigDecimal("1.30") : ONE;
        }
    }

    private static BigDecimal overtimeFactor(DayType type, boolean restDay) {
        switch (type) {
            case REGULAR_HOLIDAY:
                return restDay ? new BigDecimal("3.38") : new BigDecimal("2.60");
            case SPECIAL_NON_WORKING_DAY:
                return restDay ? new BigDecimal("1.95") : new BigDecimal("1.69");
            case ORDINARY:
            default:
                return restDay ? new BigDecimal("1.69") : ORDINARY_OT;
        }
    }

    private static BigDecimal wage(BigDecimal hourlyRate, int minutes, BigDecimal factor) {
        return hourlyRate.multiply(factor).multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public static final class Result {
        public final BigDecimal regularPay;
        public final BigDecimal overtimePay;
        public final BigDecimal regularNightDifferential;
        public final BigDecimal overtimeNightDifferential;
        public final BigDecimal totalWage;
        public final BigDecimal regularRateMultiplier;
        public final BigDecimal overtimeRateMultiplier;

        private Result(BigDecimal regularPay, BigDecimal overtimePay,
                       BigDecimal regularNightDifferential, BigDecimal overtimeNightDifferential,
                       BigDecimal totalWage, BigDecimal regularRateMultiplier,
                       BigDecimal overtimeRateMultiplier) {
            this.regularPay = regularPay;
            this.overtimePay = overtimePay;
            this.regularNightDifferential = regularNightDifferential;
            this.overtimeNightDifferential = overtimeNightDifferential;
            this.totalWage = totalWage;
            this.regularRateMultiplier = regularRateMultiplier;
            this.overtimeRateMultiplier = overtimeRateMultiplier;
        }
    }
}
