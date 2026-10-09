package com.urfavxbf.budgettracker;

import java.time.LocalTime;

public final class BreakInterval {
    public final LocalTime start;
    public final LocalTime end;

    public BreakInterval(LocalTime start, LocalTime end) {
        if (start == null || end == null || start.equals(end)) {
            throw new IllegalArgumentException("Break start and end must be valid and different.");
        }
        this.start = start;
        this.end = end;
    }
}
