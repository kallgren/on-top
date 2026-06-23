# Day (surface)

The timetable surface — _posture: where you are in it_. The shape of the whole
day as a vertical run of **Time block**s you move through as the clock advances,
with a now-line marking the present. Not a list you clear but a frame you track your
position against. Same data every day. See
[CONTEXT-MAP.md](../../../CONTEXT-MAP.md) for the shared language (Task, Schedule,
Occurrence, Done-through).

## Language

**Time block**:
Day's kind of **Task**: a named stretch of the day with a stable **id**, a
display **name**, and a **start** and **end** time. The time block is Day's whole
placement rule — start/end fix it _within_ the day, the way Week parity fixes a
Core task across the fortnight — and every time block recurs every day,
identically. The box you tap is the time block's **Occurrence** today.
_Avoid_: slot, timeslot, event, appointment, an unqualified "block"

**Open block**:
A **Time block** holding unstructured, open time (display name "Free") rather
than a task. It has no completion — never tapped done, never enters the
completions store, never carries a **Done-through**. It exists to fill what would
be gaps so the day stays contiguous, and renders recessively (no fill, dashed
border, dimmed label). A time block is therefore either completable or Open.
_Avoid_: gap, break, empty, buffer, spacer

**Now-line**:
The marker drawn across the timetable at the current time — the visual answer to
"where am I in the day". The one moving element on an otherwise fixed surface.
_Avoid_: cursor, playhead, marker
