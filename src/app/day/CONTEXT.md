# Day (surface)

The timetable surface — _posture: where you are in it_. The whole day as a
vertical run of **Time block**s you move through as the clock advances, a now-line
marking the present — a frame to track your position against, not a list to clear.
See [CONTEXT-MAP.md](../../../CONTEXT-MAP.md) for the shared language (Task,
Schedule, Occurrence, Done-through).

## Language

**Time block**:
Day's kind of **Task**: a named stretch of the day with a stable **id**, a display
**name**, and **start** and **end** times. Start/end are its whole placement rule,
and every time block recurs daily, identically.
_Avoid_: slot, timeslot, event, appointment, an unqualified "block"

**Open block**:
A **Time block** holding unstructured time (display name "Free") instead of a
task, with no completion — it never carries a **Done-through**.
_Avoid_: gap, break, empty, buffer, spacer

**Now-line**:
The marker drawn across the timetable at the current time — the one moving element
on an otherwise fixed surface.
_Avoid_: cursor, playhead, marker
