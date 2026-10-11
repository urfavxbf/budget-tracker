package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

public class PaydayReminderPolicyTest {
    @Test
    public void allowsFuturePaydayAlarm() {
        LocalDate payday = LocalDate.of(2026, 10, 22);
        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 8, 0);
        assertTrue(PaydayReminderPolicy.shouldSchedule(payday, payday,
                payday.atTime(9, 0), now, false));
    }

    @Test
    public void allowsOneLateReminderWhenTodaysAlarmWasMissed() {
        LocalDate payday = LocalDate.of(2026, 10, 22);
        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 10, 0);
        assertTrue(PaydayReminderPolicy.shouldSchedule(payday, payday,
                payday.atTime(9, 0), now, false));
    }

    @Test
    public void skipsLateReminderAfterItHasBeenDelivered() {
        LocalDate payday = LocalDate.of(2026, 10, 22);
        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 10, 0);
        assertFalse(PaydayReminderPolicy.shouldSchedule(payday, payday,
                payday.atTime(9, 0), now, true));
    }

    @Test
    public void skipsAnOldPaydayInsteadOfRearmingIt() {
        LocalDate payday = LocalDate.of(2026, 10, 7);
        LocalDate today = LocalDate.of(2026, 10, 8);
        LocalDateTime now = LocalDateTime.of(2026, 10, 8, 10, 0);
        assertFalse(PaydayReminderPolicy.shouldSchedule(payday, today,
                payday.atTime(9, 0), now, false));
    }
}
