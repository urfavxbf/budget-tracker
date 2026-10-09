package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

public class CutoffPeriodTest {
    @Test
    public void fifteenthBelongsToFirstCutoff() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2026, 10, 15));

        assertEquals(LocalDate.of(2026, 10, 1), cutoff.startDate);
        assertEquals(LocalDate.of(2026, 10, 15), cutoff.endDate);
    }

    @Test
    public void sixteenthBelongsToSecondCutoff() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2026, 10, 16));

        assertEquals(LocalDate.of(2026, 10, 16), cutoff.startDate);
        assertEquals(LocalDate.of(2026, 10, 31), cutoff.endDate);
    }

    @Test
    public void firstCutoffEndsOnFifteenthEvenInFebruary() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2025, 2, 15));

        assertEquals(LocalDate.of(2025, 2, 1), cutoff.startDate);
        assertEquals(LocalDate.of(2025, 2, 15), cutoff.endDate);
    }

    @Test
    public void secondCutoffUsesTwentyNineDaysInLeapYearFebruary() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2024, 2, 29));

        assertEquals(LocalDate.of(2024, 2, 16), cutoff.startDate);
        assertEquals(LocalDate.of(2024, 2, 29), cutoff.endDate);
    }

    @Test
    public void secondCutoffUsesTwentyEightDaysInCommonYearFebruary() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2025, 2, 28));

        assertEquals(LocalDate.of(2025, 2, 16), cutoff.startDate);
        assertEquals(LocalDate.of(2025, 2, 28), cutoff.endDate);
    }

    @Test
    public void secondCutoffEndsOnThirtyDaysInApril() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2026, 4, 30));

        assertEquals(LocalDate.of(2026, 4, 16), cutoff.startDate);
        assertEquals(LocalDate.of(2026, 4, 30), cutoff.endDate);
    }

    @Test
    public void firstCutoffPaydayIsInTheSameMonth() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2026, 10, 15));

        assertEquals(LocalDate.of(2026, 10, 22), cutoff.payday(22, 7));
    }

    @Test
    public void secondCutoffPaydayIsInTheFollowingMonth() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2026, 10, 16));

        assertEquals(LocalDate.of(2026, 11, 7), cutoff.payday(22, 7));
    }

    @Test
    public void decemberSecondCutoffPaydayMovesToJanuaryOfNextYear() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2026, 12, 31));

        assertEquals(LocalDate.of(2027, 1, 7), cutoff.payday(22, 7));
    }

    @Test
    public void paydayIsClampedToLastDayOfFebruary() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2025, 1, 16));

        assertEquals(LocalDate.of(2026, 2, 28), cutoff.payday(22, 31));
    }

    @Test
    public void leapYearFebruaryPaydayIsClampedToFebruaryTwentyNinth() {
        CutoffPeriod cutoff = CutoffPeriod.forDate(LocalDate.of(2024, 1, 16));

        assertEquals(LocalDate.of(2024, 2, 29), cutoff.payday(22, 31));
    }
}
