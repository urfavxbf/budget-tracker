# Implementation Roadmap

## Implemented in the initial Android MVP
- Java 17 native Android project, min SDK 26, target SDK 35.
- Adaptive system light/dark theme resources.
- Daily time-in/time-out entry; an earlier time-out is interpreted as an overnight shift and identical times are rejected.
- Add/remove multiple break periods.
- Break validation: every interval must fit entirely inside the shift; overlapping breaks are rejected.
- Configurable hourly rate, regular hours, ordinary OT multiplier, daily allowance, and daily deduction.
- Currency calculations use `BigDecimal` with two-decimal rounding.
- Local SQLite storage for work entries, salary snapshots, and expenses.
- Recent work-entry history.
- Expense entry with date, category, amount, and optional note.
- Current cutoff projection for the 1st–15th or 16th–month-end, using saved estimated net earnings minus recorded expenses.
- Unit tests for ordinary overtime math, overnight shifts, and overlapping breaks.
- GitHub Actions workflow configured to run unit tests and assemble a debug APK.

## Next modules
1. Persist salary settings and support hourly, daily, and monthly salary profiles with explicit conversion policies.
2. Payday management: confirm actual payout, compare against estimates, and track partial or delayed payments.
3. Bills: recurring schedules, due dates, paid/unpaid status, and reserved bill amounts.
4. Savings goals, categories, and remaining daily spending allowance based on confirmed available funds.
5. Work-entry edit/delete flows, duplicate-entry guard, finalized cutoff locking, and audit history.
6. Firebase Authentication and Firestore sync with account-scoped security rules, once Firebase configuration is supplied.
7. Offline sync status, conflict strategy, export/restore, notifications, and accessibility.
8. Holiday, rest-day, and night-differential payroll rules.
9. More unit tests for invalid durations, edge cases, deductions, cutoff boundaries, and budget calculations.

## Payroll correctness notes
- Current calculator is an ordinary-day estimate, not a statutory payroll engine.
- Overtime is calculated after the configured regular-minute threshold.
- Current estimated net pay is floored at zero if deductions exceed gross pay; the entered deduction remains recorded.
- The salary rate, OT multiplier, allowance, and deduction are stored per entry so later input changes do not rewrite saved results.
- Cutoff summary is a projection, not cash on hand. It subtracts recorded expenses from estimated earnings, and does not yet reconcile against confirmed salary received.
- Cloud sync must not be enabled until Firebase project configuration and Firestore Security Rules are supplied and verified.
