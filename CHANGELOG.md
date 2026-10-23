## 0.4.1 — Android release infrastructure

### Changed
- Raise target and compile SDK to Android 16 (API 36) to meet the 2026 Google Play submission requirement.
- Update the Gradle 8.x build to Gradle 8.13 with Android Gradle Plugin 8.13.2; retain CoGo Gradle 9.6.1 and Android Gradle Plugin 9.3.1 compatibility.
- Add signed APK/AAB release automation gated on permanent GitHub Actions signing secrets and a matching version tag.
- Add signature verification, SHA-256 checksums, release documentation and CI debug APK artifacts for internal testing.

### Release gates
- A persistent signing key and matching four repository secrets must be configured before publishing.
- Android 16 runtime UI/notification/SQLite upgrade regression tests have not yet been completed on a physical device.
- Publishing to Google Play requires its own account, app listing, policy declarations and rollout approval.

# Changelog

## 0.4.0 — Personalization foundation

### Added
- Settings controls for accent palettes, text size, spacing, startup tab and dashboard heading.
- Six independently configurable Home dashboard sections.
- Custom expense-category suggestions for expense entry and category budget limits.
- Reset-to-defaults control that never erases financial records or salary settings.

### Verification
- Pure personalization policy tests.
- Gradle 8.9 and 9.6.1 CI with CoGo AndroidX fallback regression.
- Real-device UI and data-retention smoke tests still required.

### Notes
- This is the first customization milestone; advanced dashboard ordering, dark-mode overrides, chart options, and category icons remain planned.
- No database migration is required.


## 0.3.0

### Added
- Add first-launch setup for pay rate, regular hours, overtime, payday schedule, opening cash, opening savings, and spending limit.
- Ask users to opt in to payday reminder notifications during setup.
- Add a setup confirmation summary before saving initial settings.
- Add opening savings balance to the tracked Savings Balance and expose it in Settings.
- Warn and ask for confirmation before saving an expense that uses savings or exhausts Available to Spend.
- Confirm changes to opening cash and savings balances before applying them.

### Improved
- Preserve existing user data by skipping first-time setup for users with saved settings or activity.
- Validate setup values and prevent negative starting balances or spending limits.

### Verification
- GitHub Actions unit tests and debug APK build passed for this PR.
- Manual device regression checks are still required before release.

### Notes
- Salary amounts are estimates based on user-entered settings and work records.
- Existing expense, payday, and budget-cycle records are intended to remain intact during upgrade.

## 0.2.0

### Added
- Warn when Available to Spend reaches ₱0 or is exhausted.
- Cover eligible overspending from Savings Balance, drawing only the amount above available spending funds.
- Cap savings deductions at the remaining savings balance.
- Track savings draws per expense so editing or deleting an expense adjusts the deduction correctly.

### Improved
- Show clearer dashboard warnings when spending funds or savings are exhausted.
- Keep the daily spending guide within its configured limits.

### Verification
- GitHub Actions unit tests and debug APK build passed for the overspending feature.
- Manual regression checks passed on-device for payday reminders, dashboard balances, expense add/edit/delete, budget insights, and layout.

### Notes
- Salary amounts are estimates based on user-entered settings and work records.
- This changelog describes the app version; it does not imply that a signed release APK has been published.
