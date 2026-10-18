# Testing & Regression Checklist

Single source of truth for regression testing. Update statuses only after the test was actually run.

## Automated coverage already present
- [x] Salary calculation: regular pay, overtime, breaks, overnight shifts, rounding, excessive deductions, overlapping breaks, invalid inputs.
- [x] Philippine payroll: ordinary overtime, night differential, holiday/rest-day multipliers, invalid night minutes.
- [x] Cutoff/payday calendar: boundaries, leap years, month-end, year rollover, latest/next payday.
- [x] Budget insights: budget thresholds, daily guide, available cash bounds, savings draw.
- [x] Payday reminder policy: future, missed, delivered, stale reminders.
- [ ] Full CI on current branch passes before merge.

## Financial regression scenarios
### Available Cash, savings, expenses
- [ ] Available Cash equals total cash minus total savings; displayed balance is not negative.
- [ ] Expense within Available Cash does not draw savings.
- [ ] Expense beyond Available Cash draws only the shortfall, capped by remaining savings.
- [ ] Editing expense recalculates savings draw without double-counting.
- [ ] Deleting expense reverses its recorded savings draw.
- [ ] Contribution reduces Available Cash and increases savings by the same amount.
- [ ] Withdrawal increases Available Cash and reduces savings by the same amount.
- [ ] Goal progress, history, and completion state remain consistent after contribution/withdrawal.

### Payday cycle and Budget Snapshot
- [ ] Cycle advances on scheduled payday even without a recorded salary.
- [ ] Day after payday remains in the new cycle.
- [ ] Month-end, December/January, and February transitions are correct.
- [ ] Salary edits do not double-count carryover or allocations.
- [ ] Payday-date edits invalidate/review affected allocations.
- [ ] Deleting a payday payment does not make reserved funds incorrectly spendable.
- [ ] Expenses appear exactly once in the intended cycle.
- [ ] Dashboard Budget Snapshot matches the budget screen.

### Persistence, validation, lifecycle
- [ ] Blank goal names and invalid target amounts show validation.
- [ ] Goal target date persists after edit and app restart.
- [ ] Existing data survives app restart and database upgrade.
- [ ] Invalid/duplicate work entries are handled as intended.
- [ ] Edit/delete operations preserve related salary, expense, and savings records.
- [ ] Empty states, zero balances, negative inputs, and large currency values are safe.

## Manual Android device/emulator checklist
- [ ] Fresh install and first-launch setup.
- [ ] Upgrade path with existing saved data.
- [ ] Create/edit/delete goal; contribute and withdraw.
- [ ] Add/edit/delete expense, including one exceeding Available Cash.
- [ ] Record/edit/delete payday payment; inspect allocation/carryover.
- [ ] Dashboard summaries agree with Savings and budget screens.
- [ ] Navigation, keyboard validation, scrolling, and light/dark theme.
- [ ] Payday reminders respect opt-in and do not re-arm stale reminders.

## Rules to avoid repeated work
1. Checked automated items must have committed tests.
2. Checked manual items must have actually been run on a device/emulator and recorded in a PR or release note.
3. Green CI proves only checks configured in CI; it does not prove UI/SQLite integration unless included.
4. Before a feature, review this list and add only new regression cases it introduces.
5. Rerun verified scenarios only after relevant code changes or suspected regressions.
