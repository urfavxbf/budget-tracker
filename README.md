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

## Build on Android with Code on the Go

This project supports Android phone-only development using Code on the Go (CoGo). Open the **repository root** as the Gradle project (the app module is `app/`). Current CoGo releases (26.38+) bundle **Gradle 9.6.1** and **Android Gradle Plugin 9.3.1** in an IDE-managed local Maven repository. The project's `settings.gradle` selects this supported AGP version when running Gradle 9.6+, while Gradle 8.9 keeps AGP 8.7.3 for the legacy build. This avoids requesting unavailable older plugin dependencies from CoGo's offline cache.

CoGo also injects its local Maven repository *before* Google Maven. If its local copy advertises an AndroidX module but lacks the `.aar` file (such as `androidx.activity:activity:1.8.0`), Gradle may fail rather than try Google Maven. `settings.gradle` moves **only CoGo's injected local dependency repository** after the normal remote repositories during settings evaluation, preserving the local repository as a fallback for IDE-provided artifacts. Disable `--offline` in CoGo's Build & Run > Gradle options and use a network connection on the first build to download missing app libraries. The project does not bundle third-party AARs.

After switching branches or updating the project, sync/reload Gradle and run `:app:assembleDebug` with CoGo's installed toolchain. Keep the IDE assets / offline Maven repository installed. If an error refers only to `/data/data/com.itsaky.androidide/files/home/maven/localMvnRepository`, inspect the active Gradle version and IDE asset installation before adding new dependencies. Do not manually place arbitrary JARs into the IDE's private repository.

GitHub Actions tests both Gradle versions. A successful CI build does **not** prove that a device's offline Maven assets are complete or that the APK was installed successfully.

## Open in Android Studio

Open this repository as an existing Gradle project. Use JDK 17 and install Android SDK Platform 35 / Build Tools 35.0.0 (and Build Tools 36.0.0 for AGP 9.3.1).

## Important limitations

This is an early local-first MVP, not a finished payroll or budgeting app. Salary estimates are not confirmed payments. The current calculation assumes an ordinary workday and a user-configured OT multiplier; it does not yet implement daily/monthly salary conversion, statutory deductions, holiday/rest-day rates, or night differential. Expenses currently track recorded spending; recurring bills, savings goals, and payout reconciliation remain on the roadmap.

Firebase Authentication and Firestore sync are not wired yet because a Firebase project configuration and secured Firestore rules have not been supplied. See [Firebase setup](docs/FIREBASE_SETUP.md) before enabling cloud sync.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md).
