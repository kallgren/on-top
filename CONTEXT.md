# OnTop

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
The household and digital tasks the app owns today: the recurring weekly-to-fortnightly chores, as opposed to **Rare**. "Core" marks cadence and ownership — *these* tasks, *now* — not when they happen (that's the **Schedule**).
_Avoid_: routine (alone — ambiguous)

**Rare**:
The monthly-to-yearly chores that the app deliberately does NOT own yet — they stay in Apple Reminders until the app earns them. The counterpart to **Core routine**: same kind of chores, slower cadence, out of scope for now.
_Avoid_: occasional, infrequent

**Schedule**:
The concrete configuration of *when* each Core routine task occurs — the assignment of tasks to weekday slots across the two-week parity cycle. A domain concept ("add Gmail to the schedule on Mondays"), independent of how or where it's stored.
_Avoid_: calendar, plan, config

**Week parity**:
Which of the two-week cycle today falls in, derived from ISO week-number parity. The config's two top-level halves are `:week-odd` and `:week-even`. No parity vocabulary appears inside task data.
_Avoid_: fortnight phase, A/B week, biweekly index
