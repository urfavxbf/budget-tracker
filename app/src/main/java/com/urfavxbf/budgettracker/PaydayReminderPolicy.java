package com.urfavxbf.budgettracker;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pure scheduling rules for payday reminders. */
public final class PaydayReminderPolicy {
    private PaydayReminderPolicy() {}

    /** Allows a missed reminder only on the current payday, and only once. */
    public static boolean shouldSchedule(LocalDate payday, LocalDate today,
                                         LocalDateTime trigger, LocalDateTime now,
                                         boolean alreadyDelivered) {
        if (!trigger.isBefore(now)) return true;
        return payday.equals(today) && !alreadyDelivered;
    }
}
