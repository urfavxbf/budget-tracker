# Changelog

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
