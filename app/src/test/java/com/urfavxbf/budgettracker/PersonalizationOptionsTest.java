package com.urfavxbf.budgettracker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PersonalizationOptionsTest {
    @Test public void recognizesSavedAccentWithoutCaseSensitivity() {
        assertEquals(2, PersonalizationOptions.optionIndex("TEAL",
                PersonalizationOptions.ACCENTS, 0));
    }

    @Test public void invalidOrMissingAccentFallsBackToSystem() {
        assertEquals(0, PersonalizationOptions.optionIndex(null,
                PersonalizationOptions.ACCENTS, 0));
        assertEquals(0, PersonalizationOptions.optionIndex("unknown",
                PersonalizationOptions.ACCENTS, 0));
    }

    @Test public void invalidStartupTabFallsBackToHome() {
        assertEquals(0, PersonalizationOptions.boundedIndex(-1, 5, 0));
        assertEquals(0, PersonalizationOptions.boundedIndex(55, 5, 0));
        assertEquals(3, PersonalizationOptions.boundedIndex(3, 5, 0));
    }

    @Test public void configuredTextSizesUseBoundedReadableScale() {
        assertEquals(0.90f, PersonalizationOptions.textScale(0), 0.001f);
        assertEquals(1.00f, PersonalizationOptions.textScale(1), 0.001f);
        assertEquals(1.12f, PersonalizationOptions.textScale(2), 0.001f);
        assertEquals(1.00f, PersonalizationOptions.textScale(99), 0.001f);
    }

    @Test public void defaultsIncludeCoreDashboardSections() {
        assertEquals(6, PersonalizationOptions.DASHBOARD_KEYS.length);
        assertEquals("dashboard_balance", PersonalizationOptions.DASHBOARD_KEYS[0]);
        assertEquals(PersonalizationOptions.DASHBOARD_KEYS.length,
                PersonalizationOptions.DASHBOARD_LABELS.length);
    }

    @Test public void invalidSpacingFallsBackToComfortable() {
        assertEquals(0, PersonalizationOptions.optionIndex("compact-unknown",
                PersonalizationOptions.SPACING, 0));
        assertEquals(1, PersonalizationOptions.optionIndex("Compact",
                PersonalizationOptions.SPACING, 0));
    }
    @Test public void customCategoriesAreTrimmedDeduplicatedAndOrdered() {
        assertEquals(java.util.Arrays.asList("Food", "Transport", "Bills"),
                PersonalizationOptions.parseCategories(" Food \nTransport\nfood\nBills"));
    }

    @Test public void emptyCustomCategoryListProducesNoSuggestions() {
        assertEquals(0, PersonalizationOptions.parseCategories("  \n  ").size());
    }

    @Test public void categoryNameLongerThanThirtyTwoCharactersIsRejected() {
        org.junit.Assert.assertThrows(IllegalArgumentException.class,
                () -> PersonalizationOptions.parseCategories(
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZ-very-long-category"));
    }

    @Test public void dashboardTitleFallsBackWhenBlank() {
        assertEquals("My finances", PersonalizationOptions.dashboardTitle("  "));
        assertEquals("My monthly overview", PersonalizationOptions.dashboardTitle(" My monthly overview "));
    }

}
