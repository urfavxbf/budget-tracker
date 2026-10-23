# Budget Tracker v0.4.1

This release candidate updates the app to Android 16 (target API 36) and adds a reproducible signed Android build pipeline.

- **APK**: direct Android installation with a persistent release signing key.
- **AAB**: bundle for manual upload to Google Play Console after completing developer account and policy requirements.
- **SHA256SUMS.txt**: integrity checksums for both assets.

The app remains local-first, with salary projections, work entries, expenses, budgets, savings, payday reminders and appearance preferences. Salary calculations are estimates, not official payroll statements.

**Upgrade caution:** release-signed APKs cannot replace debug-signed APKs in place. Uninstalling the existing debug app will delete local financial data; there is not yet a verified migration/export feature.

CI checks do not replace manual financial-regression, notification and accessibility testing on real Android 16 devices. No Play Store submission is performed automatically.
