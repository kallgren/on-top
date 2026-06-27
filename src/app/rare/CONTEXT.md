# Rare (surface)

The slower-upkeep surface — _posture: when you can_. Maintenance chores on a
monthly-to-yearly cadence, shown as category cards with due / missed / upcoming
states. See [CONTEXT-MAP.md](../../../CONTEXT-MAP.md) for the shared language
(Task, Category, Schedule, Occurrence, Done-through).

## Language

**Interval**:
A Rare **Task**'s recurrence rule: its cadence, drawn from a fixed set — `"2 weeks"`,
`"monthly"`, `"2 months"`, `"quarterly"`, `"6 months"`, `"yearly"`.
_Avoid_: frequency, period, recurrence

**Anchor date**:
The date of a task's first **Occurrence**, from which all later ones step forward
by the **Interval**. A calendar date, not a completion date.
_Avoid_: start date, seed date

**Current**:
A **Task** with an **Occurrence** later than its **Done-through** but on or before
today — due, not done.
_Avoid_: active, due, overdue, pending

**Upcoming**:
A **Task** whose **Done-through** covers every **Occurrence** on or before today —
caught up, with only future occurrences left.
_Avoid_: future, scheduled, pending

**Missed**:
The count of due-but-not-done **Occurrence**s beyond the earliest one on a
**Current** task's row.
_Avoid_: overdue, backlog, skipped, behind
