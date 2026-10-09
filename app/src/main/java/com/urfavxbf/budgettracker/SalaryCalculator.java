package com.urfavxbf.budgettracker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SalaryCalculator {
    private static final int MINUTES_PER_DAY = 24 * 60;

    private SalaryCalculator() {}

    public static Result calculate(
            LocalTime timeIn,
            LocalTime timeOut,
            List<BreakInterval> breaks,
            int regularMinutesPerDay,
            BigDecimal hourlyRate,
            BigDecimal overtimeMultiplier,
            BigDecimal allowance,
            BigDecimal deduction) {
        if (timeIn == null || timeOut == null) {
            throw new IllegalArgumentException("Time in and time out are required.");
        }
        if (regularMinutesPerDay <= 0 || regularMinutesPerDay > MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Regular work duration must be between 1 minute and 24 hours.");
        }
        requireNonNegative(hourlyRate, "Hourly rate");
        requirePositive(overtimeMultiplier, "Overtime multiplier");
        requireNonNegative(allowance, "Allowance");
        requireNonNegative(deduction, "Deduction");

        int start = timeIn.getHour() * 60 + timeIn.getMinute();
        int end = timeOut.getHour() * 60 + timeOut.getMinute();
        if (end == start) {
            throw new IllegalArgumentException("Time in and time out cannot be the same.");
        }
        if (end < start) {
            end += MINUTES_PER_DAY;
        }
        int shiftMinutes = end - start;
        if (shiftMinutes <= 0 || shiftMinutes > MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Shift duration must not exceed 24 hours.");
        }

        List<MinuteInterval> intervals = new ArrayList<>();
        if (breaks != null) {
            for (BreakInterval item : breaks) {
                if (item == null) {
                    throw new IllegalArgumentException("Break entry cannot be empty.");
                }
                int breakStartClock = item.start.getHour() * 60 + item.start.getMinute();
                int breakEndClock = item.end.getHour() * 60 + item.end.getMinute();
                int relativeStart = Math.floorMod(breakStartClock - start, MINUTES_PER_DAY);
                int relativeEnd = Math.floorMod(breakEndClock - start, MINUTES_PER_DAY);
                if (relativeEnd <= relativeStart) {
                    relativeEnd += MINUTES_PER_DAY;
                }
                if (relativeStart < 0 || relativeEnd > shiftMinutes || relativeEnd <= relativeStart) {
                    throw new IllegalArgumentException("Every break must fall entirely within the shift.");
                }
                intervals.add(new MinuteInterval(relativeStart, relativeEnd));
            }
        }

        intervals.sort(Comparator.comparingInt(interval -> interval.start));
        int breakMinutes = 0;
        int previousEnd = -1;
        for (MinuteInterval interval : intervals) {
            if (interval.start < previousEnd) {
                throw new IllegalArgumentException("Break periods must not overlap.");
            }
            breakMinutes += interval.end - interval.start;
            previousEnd = interval.end;
        }
        if (breakMinutes >= shiftMinutes) {
            throw new IllegalArgumentException("Break time must be shorter than the total shift.");
        }

        int netMinutes = shiftMinutes - breakMinutes;
        int regularMinutes = Math.min(netMinutes, regularMinutesPerDay);
        int overtimeMinutes = Math.max(0, netMinutes - regularMinutesPerDay);
        BigDecimal regularPay = hourlyRate
                .multiply(BigDecimal.valueOf(regularMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal overtimePay = hourlyRate
                .multiply(overtimeMultiplier)
                .multiply(BigDecimal.valueOf(overtimeMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal gross = regularPay.add(overtimePay).add(allowance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(deduction).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new Result(shiftMinutes, breakMinutes, netMinutes, regularMinutes, overtimeMinutes,
                regularPay, overtimePay, allowance.setScale(2, RoundingMode.HALF_UP),
                deduction.setScale(2, RoundingMode.HALF_UP), gross, net);
    }

    public static Result calculateWithBreakMinutes(
            LocalTime timeIn, LocalTime timeOut, int breakMinutes,
            int regularMinutesPerDay, BigDecimal hourlyRate,
            BigDecimal overtimeMultiplier, BigDecimal allowance, BigDecimal deduction) {
        if (timeIn == null || timeOut == null) {
            throw new IllegalArgumentException("Time in and time out are required.");
        }
        if (regularMinutesPerDay <= 0 || regularMinutesPerDay > MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Regular work duration must be between 1 minute and 24 hours.");
        }
        if (breakMinutes < 0) throw new IllegalArgumentException("Break duration cannot be negative.");
        requireNonNegative(hourlyRate, "Hourly rate");
        requirePositive(overtimeMultiplier, "Overtime multiplier");
        requireNonNegative(allowance, "Allowance");
        requireNonNegative(deduction, "Deduction");
        int start = timeIn.getHour() * 60 + timeIn.getMinute();
        int end = timeOut.getHour() * 60 + timeOut.getMinute();
        if (start == end) throw new IllegalArgumentException("Time in and time out cannot be the same.");
        if (end < start) end += MINUTES_PER_DAY;
        int shiftMinutes = end - start;
        if (shiftMinutes <= 0 || shiftMinutes > MINUTES_PER_DAY) {
            throw new IllegalArgumentException("Shift duration must not exceed 24 hours.");
        }
        if (breakMinutes >= shiftMinutes) {
            throw new IllegalArgumentException("Break time must be shorter than the total shift.");
        }
        int netMinutes = shiftMinutes - breakMinutes;
        int regularMinutes = Math.min(netMinutes, regularMinutesPerDay);
        int overtimeMinutes = Math.max(0, netMinutes - regularMinutesPerDay);
        BigDecimal regularPay = hourlyRate.multiply(BigDecimal.valueOf(regularMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal overtimePay = hourlyRate.multiply(overtimeMultiplier)
                .multiply(BigDecimal.valueOf(overtimeMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal gross = regularPay.add(overtimePay).add(allowance).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(deduction).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new Result(shiftMinutes, breakMinutes, netMinutes, regularMinutes, overtimeMinutes,
                regularPay, overtimePay, allowance.setScale(2, RoundingMode.HALF_UP),
                deduction.setScale(2, RoundingMode.HALF_UP), gross, net);
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " cannot be negative or missing.");
        }
    }

    private static void requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero.");
        }
    }

    private static final class MinuteInterval {
        final int start;
        final int end;

        MinuteInterval(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    public static final class Result {
        public final int shiftMinutes;
        public final int breakMinutes;
        public final int netWorkMinutes;
        public final int regularMinutes;
        public final int overtimeMinutes;
        public final BigDecimal regularPay;
        public final BigDecimal overtimePay;
        public final BigDecimal allowance;
        public final BigDecimal deduction;
        public final BigDecimal grossPay;
        public final BigDecimal estimatedNetPay;

        Result(int shiftMinutes, int breakMinutes, int netWorkMinutes, int regularMinutes,
               int overtimeMinutes, BigDecimal regularPay, BigDecimal overtimePay,
               BigDecimal allowance, BigDecimal deduction, BigDecimal grossPay,
               BigDecimal estimatedNetPay) {
            this.shiftMinutes = shiftMinutes;
            this.breakMinutes = breakMinutes;
            this.netWorkMinutes = netWorkMinutes;
            this.regularMinutes = regularMinutes;
            this.overtimeMinutes = overtimeMinutes;
            this.regularPay = regularPay;
            this.overtimePay = overtimePay;
            this.allowance = allowance;
            this.deduction = deduction;
            this.grossPay = grossPay;
            this.estimatedNetPay = estimatedNetPay;
        }
    }
}
