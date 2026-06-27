# On Top

A calm personal routine system with three surfaces over one shared language:
a daily check-off of the chores due today, a slower surface for upkeep you get
to when you can, and a timetable of the day's time blocks you move through. Tapping
marks things done; nothing nags.

This map fixes the language shared by all three surfaces and points to where each
surface's own language lives. It is a glossary, not a spec — no implementation
details.

## Surfaces

**Surface**:
One of the three self-contained parts the product presents — **Core**, **Rare**,
or **Day**. Each is a bounded context with its own **Schedule**, view, and
completion store, sharing the kernel language below. (Core and Rare each began as
a standalone app; Day was born inside the product.) The three are distinguished
by **posture** — the relationship you have to the work — not by how often a task
recurs. ("Core", "Rare" and "Day" are working names; the distinction is posture,
not frequency.) A Surface is a *concept* — a bounded context, the unit a
completion is tagged with — not the region of screen it occupies: it is
*rendered in* a **Pane**. Reserve "surface" for "Core, Rare or Day".
_Avoid_: app, view, tab, mode, screen; "surface" for the on-screen region (that's a Pane)

**Core** — _posture: today._
The areas where things pile up: inboxes, files, photos. You've committed to a
rhythm, so when a task comes due you clear it **that day**. Core exists to nudge
you daily.
Language: [src/app/core/CONTEXT.md](src/app/core/CONTEXT.md).

**Rare** — _posture: when you can._
Slower upkeep done when **time and energy allow**, plus a few with real
deadlines. Unobtrusive by design — it shouldn't speak up unless a date is
closing in.
Language: [src/app/rare/CONTEXT.md](src/app/rare/CONTEXT.md).

**Day** — _posture: where you are in it._
The shape of the whole day as a vertical timetable of **Time block**s — Deep
Work, Exercise, a time block for the day's Core tasks — that you move through as
the clock advances. Not a list you clear but a frame you track your position against; a
now-line marks the present moment. The same data every day.
Language: [src/app/day/CONTEXT.md](src/app/day/CONTEXT.md).

**Pane**:
The on-screen region that renders one **Surface** in the shell's current layout.
A view-layer term, not a domain one — the Pane can change shape (side by side,
swipeable) without touching what a Surface *is* or how a completion is tagged.
One Pane per Surface today.
_Avoid_: using "Pane" for the Surface concept or a completion's tag (that's the
Surface)

**Cursor**:
The single keyboard point of attention. At most one **Task** is focused at any
moment, in exactly one **Pane**. The Cursor is **dormant** until the first
navigation key wakes it; vertical keys move it within a Pane, horizontal keys
move the one Cursor across the Pane boundary. A view-layer term (like **Pane**),
not a domain one — it tags nothing, computes nothing, and is never stored. A
keyboard affordance only: it has no meaning on a touch layout.
_Avoid_: selection, focus (the DOM sense), highlight, active row, hovered row

## Shared language

These terms mean the same thing on both surfaces. Each surface adds its own
terms on top (see the surface contexts above).

**Task**:
A recurring chore with a stable **task id**, a **Category**, and a **recurrence
rule**. On **Core** and **Rare**, its display **name** and optional **Note** come
from the **Notes file**, keyed by its **task id** (on **Day** the name is carried
inline in the schedule); the name can be reworded without changing identity or
losing **Done-through** history. A task is a template, not an
**Occurrence**: its recurrence rule places it in time and generates many dated
occurrences. Within a surface a task belongs to exactly one Category — except on
**Day**, which has no categories and orders tasks by time of day. The recurrence
rule's *shape* is surface-specific.
_Avoid_: item, todo, chore (in code), reminder

**Task id**:
A task's stable identity: lowercase letters, digits and dashes (`[a-z0-9-]+`),
globally unique across all surfaces. Distinct from the **name**, which may change.
_Avoid_: key, slug; name

**Category**:
The kind of a task on **Core** and **Rare**: the top-level keys of that surface's
**Schedule**, in file order, each surface deriving its own. Its label is
title-cased from the key (`:home-office` → "Home Office"). **Day** has none — it
orders by time of day.
_Avoid_: section, group, type

**Schedule**:
Which **task id**s recur when, in a surface's own recurrence shape — the source
from which **Occurrence**s are computed. On **Core** and **Rare** it carries ids
and recurrence only, never names (those live in the **Notes file**); **Day**
keeps its names inline.
_Avoid_: calendar, plan, config; holding display names (Core/Rare)

**Notes file**:
The single Markdown document that gives **Core** and **Rare** tasks their display
**name** and optional **Note**, keyed by **task id** and shared across those two
surfaces. **Day** is out of scope — its schedule keeps names inline and never
consults the notes file. Its authoring and parse format is specced in
[docs/notes-format.md](docs/notes-format.md).
_Avoid_: descriptions file, glossary, schedule

**Note**:
The optional multi-line prose attached to a task in its **Task definition**;
authored and rendered as Markdown.
_Avoid_: description, comment, blurb

**Occurrence**:
A specific dated instance of a **Task**, computed by applying the task's
recurrence rule. Occurrences are derived — they are never stored.
_Avoid_: instance, entry, slot

**Done-through**:
The single per-task value recording completion: the **Occurrence** up to and
including which a task is considered done — forward-looking *coverage*, not a
backward "last done". One value per task (bounded by task count, not days),
surviving the date rollover. Toggling moves this one value; unmarking rolls it
back to the previous Occurrence rather than clearing it, so coverage slides
between Occurrences. There are no per-occurrence records.
_Avoid_: completion, last done, due date, expiry, status, progress
