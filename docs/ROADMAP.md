# Implementation Roadmap

## Implemented in the initial Android MVP
- Native Android Java project (min SDK 26, Java 17).
- Adaptive system light/dark theme resources.
- Daily time-in/time-out entry with overnight shift handling.
- Add/remove multiple break periods.
- Break validation: each interval must fit inside the shift; overlapping breaks are rejected.
- Regular hours and ordinary overtime split.
- Hourly rate, daily allowance, daily deduction, and configurable ordinary OT multiplier.
- `BigDecimal` for currency calculations and two-decimal rounding.
- Local SQLite storage of daily entries and calculation snapshots.
- Recent work-entry history.
- Unit tests for normal shifts, overnight shifts, and overlapping breaks.

## Next modules
1. Salary profile types: hourly, daily, and monthly; configurable conversion policies.
2. Payday periods: 1st-15th and 16th-month-end, with payout confirmation and partial/delayed payments.
3. Expenses, categories, recurring bills, bill reserves, savings goals, and daily allowance.
4. Settings persistence and per-entry salary-rule snapshots.
5. Firebase Authentication and Firestore sync with account-scoped security rules.
6. Offline sync status, conflict strategy, export/restore, notifications, and accessibility.
7. Holiday, rest-day, and night-differential payroll rules.

## Payroll correctness notes
- Current calculator is an ordinary-day estimate, not a statutory payroll engine.
- Overtime is calculated after the configured regular-minute threshold.
- Current net pay is floored at zero if deductions exceed gross pay; the raw deduction remains recorded.
- Historical rate fields are stored per entry so later settings changes do not rewrite old calculations.
- Cloud sync must not be enabled until Firebase project configuration and Firestore Security Rules are supplied and verified.
