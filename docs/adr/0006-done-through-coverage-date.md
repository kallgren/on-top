# Done-through stored as a coverage date

Completion is stored as a single per-task *done-through* date — the date through which the task counts as done — keyed by a stable task id rather than its display text, so a task can be reworded without losing history. A task reads as done on a day when its done-through is on or after that day. Marking done sets the date to that day; unmarking rolls it back to the task's previous scheduled occurrence rather than clearing it. Because the schedule repeats on a fixed two-week cycle, a previous occurrence always exists within the last two weeks, so the roll-back is always defined. This keeps a single boundary date that slides along a task's occurrences — the shape a potential future full-week view needs. It also keeps completion modelled identically to the separate rare-task app, so the two can share this logic and potentially merge later; that compatibility is a primary reason for the coverage shape over a simpler per-day done flag.

## Considered options

- **Clear on unmark.** Simplest, and observationally identical today, but stores nothing a week view could read and diverges from the rare-task app's model.
- **Keep the last two dates so unmark restores the real previous value.** More truthful for an accidental check, but adds per-task history the coverage model doesn't need and forks the rare-task app's logic.

## Consequences

- Unmarking moves the date *backwards in time*, which is surprising in isolation and only coherent under the coverage reading.
- The unmark path depends on the Schedule (to find the previous occurrence), coupling completion logic to the schedule.
- A recomputed date can assert coverage through an occurrence that was never actually done; this over-claims "done" rather than under-claims, which suits the no-shame stance.
