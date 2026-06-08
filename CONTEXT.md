# On Top

A calm daily check-off surface for a personal routine system: each day shows the household and digital chores due, and tapping marks them done. This glossary fixes the language the code and docs should use.

## Language

**Task**:
A single chore shown for the day, written as a plain name (e.g. "Gmail inbox"). A task name is unique across the whole day — it never repeats, in either category, on the same day.
_Avoid_: item, todo, chore (in code)

**Category**:
The kind of a task. Exactly two exist: `:household` and `:digital`. Fixed, not user-configurable.
_Avoid_: section, group, type

**Completion**:
The record that a task has been marked done today. Lives only for the current day and is cleared when the date rolls over. Identified by the task's bare name.
_Avoid_: check, status, state, progress

**Core routine**:
The weekly + fortnightly set of household and digital tasks this app owns. The app's entire scope.
_Avoid_: schedule (ambiguous), routine

**Rare**:
The monthly-to-yearly cadence of chores that the app deliberately does NOT own yet — it stays in Apple Reminders until the app earns it.
_Avoid_: occasional, infrequent

**Week parity**:
Which of the two-week cycle today falls in, derived from ISO week-number parity. The config's two top-level halves are `:week-odd` and `:week-even`. No parity vocabulary appears inside task data.
_Avoid_: fortnight phase, A/B week, biweekly index
