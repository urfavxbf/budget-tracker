package com.urfavxbf.budgettracker;

/**
 * Validated, display-only personalization options.
 * Kept independent of Android so defaults and saved value handling can be unit tested.
 */
public final class PersonalizationOptions {
    public static final String[] ACCENTS = {"System", "Blue", "Teal", "Violet", "Rose"};
    public static final String[] TEXT_SIZES = {"Small", "Normal", "Large"};
    public static final String[] SPACING = {"Comfortable", "Compact"};
    public static final String[] START_TABS = {"Home", "Activity", "Budget", "Savings", "Settings"};

    public static final String[] DASHBOARD_KEYS = {
            "dashboard_balance", "dashboard_shortcuts", "dashboard_budget",
            "dashboard_trend", "dashboard_recent", "dashboard_savings"
    };
    public static final String[] DASHBOARD_LABELS = {
            "Available cash", "Quick actions", "Budget snapshot",
            "Income vs expenses chart", "Recent transactions", "Savings summary"
    };

    private PersonalizationOptions() {}

    public static int optionIndex(String value, String[] values, int fallback) {
        if (values == null || values.length == 0) return 0;
        if (value != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equalsIgnoreCase(value.trim())) return i;
            }
        }
        return boundedIndex(fallback, values.length, 0);
    }

    public static int boundedIndex(int value, int count, int fallback) {
        if (count <= 0) return 0;
        if (value >= 0 && value < count) return value;
        return fallback >= 0 && fallback < count ? fallback : 0;
    }

    public static float textScale(int index) {
        switch (index) {
            case 0: return 0.90f;
            case 2: return 1.12f;
            default: return 1.00f;
        }
    }
}
