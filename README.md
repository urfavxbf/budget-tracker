# Budget Tracker

Native Android salary tracker and payday budget planner. Package: `com.urfavxbf.budgettracker`.

## Current MVP

- Java 17 / Android Gradle project, min SDK 26, target SDK 35.
- Adaptive system light/dark theme.
- Daily time-in/time-out entry; earlier time-out is interpreted as an overnight shift.
- Multiple break-duration entries that are summed for salary calculation. The current UI does not capture each break's start/end time, so it cannot validate whether breaks fall within the shift or overlap.
- Configurable hourly rate, regular workday hours, ordinary overtime multiplier, daily allowance, and daily deduction.
- Salary calculations use `BigDecimal` and store a snapshot of the rate and result for each saved work entry.
- Local SQLite persistence and recent work-entry history.
- Local expense entry with date, category, amount, and optional note.
- Current cutoff summary for the 1st–15th or 16th–month-end, based on saved estimated earnings and recorded expenses.
- Unit tests for regular overtime math, overnight shifts, break interval validation in the calculator, payday reminder scheduling policy, and daily spending-guide limits.
- GitHub Actions workflow for unit tests and debug APK build.

## Open in Android Studio

Open this repository as an existing Gradle project. Use JDK 17 and install Android SDK Platform 35 / Build Tools 35.0.0.

## Important limitations

This is an early local-first MVP, not a finished payroll or budgeting app. Salary estimates are not confirmed payments. The current calculation assumes an ordinary workday and a user-configured OT multiplier; it does not yet implement daily/monthly salary conversion, statutory deductions, holiday/rest-day rates, or night differential. Expenses currently track recorded spending; recurring bills, savings goals, and payout reconciliation remain on the roadmap.

Firebase Authentication and Firestore sync are not wired yet because a Firebase project configuration and secured Firestore rules have not been supplied. See [Firebase setup](docs/FIREBASE_SETUP.md) before enabling cloud sync.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md).
