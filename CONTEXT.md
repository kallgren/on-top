# OnTop

A calm daily check-off surface for a personal routine system: each day shows the household and digital chores due, and tapping marks them done. This glossary fixes the language the code and docs should use.

## Language

**Task**:
A recurring chore with a stable **id** and a separate display **text** — the text can be reworded without changing the task or losing its **Done through** history. The same task recurs across many days; within a single day its id appears at most once, in one category.
_Avoid_: item, todo, chore (in code)

**Category**:
The kind of a task. Exactly two exist: `:household` and `:digital`. Fixed, not user-configurable.
_Avoid_: section, group, type

**Completion**:
A task being done *today*: the derived view that its **Done through** date covers today (`today ≤ done-through`). A view computed for display, not a stored record — the stored datum is **Done through**. Identified by the task's id, not its display text.
_Avoid_: check, status, state, progress

**Done through**:
The date up to and including which a task is considered done — forward-looking *coverage*, not a backward "last done". Stored per task, one record each, surviving the date rollover (so storage is bounded by task count, never by days). A daily task marked done is done through today and so reads as not-done tomorrow. Unmarking rolls the date back to the task's previous scheduled occurrence rather than clearing it, so coverage slides between occurrences.
_Avoid_: last completion, last done, due date, expiry

**Core tasks**:
The **Task**s this app currently handles: the chores worth keeping on top of across the two-week schedule. The counterpart to **Rare tasks**.
_Avoid_: routine (alone — ambiguous)

**Rare tasks**:
**Task**s on a slower cadence — monthly to yearly — that the app does not handle yet; they stay in Apple Reminders for now. The counterpart to **Core tasks**.
_Avoid_: occasional, infrequent

**Schedule**:
The concrete configuration of *when* each Core task occurs — the assignment of tasks to weekday slots across the two-week parity cycle. A domain concept ("add Gmail to the schedule on Mondays"), independent of how or where it's stored.
_Avoid_: calendar, plan, config

**Week parity**:
Which of the two-week cycle today falls in, derived from ISO week-number parity. The config's two top-level halves are `:week-odd` and `:week-even`. No parity vocabulary appears inside task data.
_Avoid_: fortnight phase, A/B week, biweekly index
