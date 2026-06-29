# Rare (surface)

The slower-upkeep surface — _posture: when you can_. Maintenance chores on a
monthly-to-yearly cadence, shown as category cards with current / upcoming /
missed rows, plus a **Due** treatment for the few with real deadlines. See
[CONTEXT-MAP.md](../../../CONTEXT-MAP.md) for the shared language (Task, Category,
Schedule, Occurrence, Done-through).

## Language

**Interval**:
A Rare **Task**'s recurrence rule: its cadence, drawn from a fixed set — `"2 weeks"`,
`"monthly"`, `"2 months"`, `"quarterly"`, `"6 months"`, `"yearly"`.
_Avoid_: frequency, period, recurrence

**Anchor date**:
The date of a task's first **Occurrence**, from which all later ones step forward
by the **Interval**. A calendar date, not a completion date.
_Avoid_: start date, seed date

**Lead time**:
The number of days before an **Occurrence** that a **Deadline task** starts
surfacing as **Current** and showing its countdown (`:lead-days N` in the
**Schedule**). Zero for ordinary tasks.
_Avoid_: lead window, grace, buffer, before

**Deadline task**:
A **Task** with a non-zero **Lead time** — the only kind that can be **Due**.
_Avoid_: appointment, hard task, deadline (alone)

**Current**:
A **Task** whose latest **Occurrence** on or before today is later than its
**Done-through** — it has come round again but isn't done. Pure placement; says
nothing about deadlines.
_Avoid_: active, due, overdue, pending

**Upcoming**:
A **Task** whose **Done-through** covers every **Occurrence** on or before today —
caught up, with only future occurrences left.
_Avoid_: future, scheduled, pending

**Missed**:
The count of unfinished **Occurrence**s beyond the earliest one on a **Current**
task's row.
_Avoid_: overdue, backlog, skipped, behind

**Due**:
A **Deadline task** that is pressing right now: inside its 0–7 day countdown, or
overdue and not done. Distinct from **Current**, which is mere placement and
applies to every task.
_Avoid_: overdue, urgent, late, pending
