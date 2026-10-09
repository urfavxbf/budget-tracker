package com.urfavxbf.budgettracker;

import java.time.LocalDate;

/** Represents the fixed 1st–15th or 16th–last-day salary cutoff containing a date. */
public final class CutoffPeriod {
    public final LocalDate startDate;
    public final LocalDate endDate;

    private CutoffPeriod(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static CutoffPeriod forDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        if (date.getDayOfMonth() <= 15) {
            return new CutoffPeriod(date.withDayOfMonth(1), date.withDayOfMonth(15));
        }
        return new CutoffPeriod(date.withDayOfMonth(16), date.withDayOfMonth(date.lengthOfMonth()));
    }

    public LocalDate payday(int firstCutoffPayday, int secondCutoffPayday) {
        boolean firstCutoff = startDate.getDayOfMonth() == 1;
        int requestedDay = firstCutoff ? firstCutoffPayday : secondCutoffPayday;
        LocalDate targetMonth = firstCutoff ? startDate : startDate.plusMonths(1);
        int safeDay = Math.min(Math.max(requestedDay, 1), targetMonth.lengthOfMonth());
        return targetMonth.withDayOfMonth(safeDay);
    }
}
