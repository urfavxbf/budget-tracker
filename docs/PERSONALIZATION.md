# Budget Tracker Personalization

## Version 0.4.0: customization foundation

All controls live under **Settings**. Preferences are stored on-device in `salary_preferences`; the financial SQLite database and recorded salary/expense/savings entries are unchanged.

### Appearance
- **Accent:** System (follows existing Material You or app theme), Blue, Teal, Violet, Rose. Presets include day/night contrast values.
- **Text size:** Small (0.90x), Normal (1.00x), Large (1.12x), applied to shared text, field, and button widgets. Android's own accessibility font scaling still applies.
- **Spacing:** Comfortable or Compact page/card padding.
- **Start tab:** Home, Activity, Budget, Savings, Settings.
- **Dashboard heading:** custom title up to 32 characters.
- Apply requires one tap and recreates the activity to apply theme values; current visible tab is restored.

### Dashboard visibility
Choose to show or hide the available cash panel, quick actions, budget snapshot, monthly trend, recent transactions, and savings summary. All are enabled by default. Hidden cards **never delete financial data**. Critical payday recording prompts and the explanatory salary disclaimer remain visible.

### Custom categories
Create up to 20 category suggestions (one per line; <=32 characters per name, case-insensitive deduplication), shown in new expense forms and category-budget settings. Existing transaction history and categories remain intact even after the presets change.

### Reset
Reset personalization to defaults asks for confirmation and removes only customization keys, leaving balances, salaries, savings, records, budgets, and notification opt-in unchanged.

## Code / testing
- `PersonalizationOptions.java`: pure preference validation, label definitions, category normalization; unit tested.
- `MainActivity.java`: saved options, settings controls, dashboard visibility, suggestion lists.
- `values/styles.xml` and `values-night/styles.xml`: accent overlays, retaining the installed Material 3 theme.
- CI must pass on both Gradle 8.9 and 9.6.1, including the CoGo missing-AndroidX-artifact regression test.
- Physical device checks still required for appearance contrast, large-font accessibility, settings scroll, app relaunch, default tab, and upgrade preserving financial records.

## Future phases toward fully customizable
1. **Theme mode:** explicit System / Light / Dark; dynamic-color controls and contrast checks.
2. **Dashboard layout editor:** drag-to-reorder cards, compact/full widget size variants, presets, and undo.
3. **Category manager:** custom icons and colors, category merge/rename with transaction-history-safe migrations.
4. **Widgets and alerts:** personalized chart timeframes, expense thresholds, reminders, budget notification schedules.
5. **Localization and display:** optional currencies for display, date formatting, accessible typography and color controls (salary accounting must remain in the original ledger currency unless the user explicitly converts).

Never conflate appearance settings with payroll or currency conversions. Each phase needs regressions and device testing before merging.
