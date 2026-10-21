# Changelog

## Unreleased

### Fixed
- Respect payday reminder opt-in before scheduling or delivering notifications.
- Cancel any pending payday alarm when notifications are disabled.
- Do not re-arm an already delivered payday reminder.

### Improved
- Add a payday reminder checkbox in Settings so opt-in can be changed after onboarding.

### Verification
- Unit tests cover opt-out and delivered-reminder scheduling policy.
- Physical-device notification and permission-flow testing remains pending.


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
