# Rare (surface)

The slower-upkeep surface — _posture: when you can_. Maintenance chores on a
monthly-to-yearly cadence, shown as category cards with due / missed / upcoming
states. See [CONTEXT-MAP.md](../../../CONTEXT-MAP.md) for the shared language
(Task, Category, Schedule, Occurrence, Done-through).

## Language

**Interval**:
A Rare **Task**'s recurrence rule: the cadence at which it recurs, drawn from a
fixed set of strings — `"2 weeks"`, `"monthly"`, `"2 months"`, `"quarterly"`,
`"6 months"`, `"yearly"`. No arbitrary cadences.
_Avoid_: frequency, period, recurrence

**Anchor date**:
The date of the first **Occurrence** of a task. All future occurrences are
computed by stepping forward from the anchor by the **Interval**. A calendar
date, not a completion date.
_Avoid_: start date, seed date

**Current**:
A **Task** with at least one **Occurrence** later than its **Done-through** and
on or before today — due but not done. Shown as a single row: the earliest such
occurrence's date, plus a **Missed** count for any others. The default view shows
only current tasks.
_Avoid_: active, due, overdue, pending

**Upcoming**:
A **Task** whose **Done-through** already covers every **Occurrence** on or
before today — it is caught up. Its row shows the next future occurrence. Hidden
by default; revealed by the "show upcoming" toggle.
_Avoid_: future, scheduled, pending

**Missed**:
The count of due-but-not-done **Occurrence**s beyond the earliest one shown on a
**Current** task's row, surfaced as "X missed". Lets a backlog collapse into one
row without hiding that it exists.
_Avoid_: overdue, backlog, skipped, behind
