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

    /**
     * Returns the cutoff whose payday is next after today. On the payday itself,
     * advances to the following scheduled payday and its cutoff.
     */
    public static CutoffPeriod forNextPayday(LocalDate today, int firstCutoffPayday, int secondCutoffPayday) {
        if (today == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        CutoffPeriod next = null;
        LocalDate nextPayday = null;
        // Search surrounding months because the second cutoff is paid in the following month.
        for (int offset = -1; offset <= 2; offset++) {
            LocalDate month = today.withDayOfMonth(1).plusMonths(offset);
            CutoffPeriod[] candidates = {
                    forDate(month.withDayOfMonth(1)),
                    forDate(month.withDayOfMonth(16))
            };
            for (CutoffPeriod candidate : candidates) {
                LocalDate payday = candidate.payday(firstCutoffPayday, secondCutoffPayday);
                if (payday.isAfter(today) && (nextPayday == null || payday.isBefore(nextPayday))) {
                    next = candidate;
                    nextPayday = payday;
                }
            }
        }
        if (next == null) {
            throw new IllegalStateException("Unable to determine the next cutoff payday.");
        }
        return next;
    }

    public LocalDate payday(int firstCutoffPayday, int secondCutoffPayday) {
        boolean firstCutoff = startDate.getDayOfMonth() == 1;
        int requestedDay = firstCutoff ? firstCutoffPayday : secondCutoffPayday;
        LocalDate targetMonth = firstCutoff ? startDate : startDate.plusMonths(1);
        int safeDay = Math.min(Math.max(requestedDay, 1), targetMonth.lengthOfMonth());
        return targetMonth.withDayOfMonth(safeDay);
    }
}
