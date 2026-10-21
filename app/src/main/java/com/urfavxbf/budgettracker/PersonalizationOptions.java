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

    /** Newline-delimited optional user categories; case-insensitive uniqueness is preserved. */
    public static java.util.List<String> parseCategories(String input) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (input == null || input.trim().isEmpty()) return result;
        for (String row : input.split("\\r?\\n")) {
            String category = row.trim();
            if (category.isEmpty()) continue;
            if (category.length() > 32) {
                throw new IllegalArgumentException("Each category must be 32 characters or fewer.");
            }
            boolean duplicate = false;
            for (String existing : result) {
                if (existing.equalsIgnoreCase(category)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) continue;
            if (result.size() == 20) {
                throw new IllegalArgumentException("You can save up to 20 custom categories.");
            }
            result.add(category);
        }
        return result;
    }

    public static String dashboardTitle(String input) {
        if (input == null || input.trim().isEmpty()) return "My finances";
        String clean = input.trim();
        return clean.length() <= 32 ? clean : clean.substring(0, 32);
    }

    public static float textScale(int index) {
        switch (index) {
            case 0: return 0.90f;
            case 2: return 1.12f;
            default: return 1.00f;
        }
    }
}
