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
One of the product's three self-contained parts — **Core**, **Rare**, or **Day** —
each a bounded context with its own **Schedule**, view, and completion store.
Surfaces are distinguished by **posture** (your relationship to the work), not by
how often a task recurs.
_Avoid_: app, view, tab, mode, screen; "surface" for the on-screen region (that's a Pane)

**Core** — _posture: today._
The areas where things pile up — inboxes, files, photos — cleared the day a task
comes due.
Language: [src/app/core/CONTEXT.md](src/app/core/CONTEXT.md).

**Rare** — _posture: when you can._
Slower upkeep done when time and energy allow, plus a few with real deadlines.
Language: [src/app/rare/CONTEXT.md](src/app/rare/CONTEXT.md).

**Day** — _posture: where you are in it._
The whole day as a vertical timetable of **Time block**s you move through as the
clock advances.
Language: [src/app/day/CONTEXT.md](src/app/day/CONTEXT.md).

**Pane**:
The on-screen region that renders one **Surface** in the shell's current layout.
One Pane per Surface today.
_Avoid_: "Pane" for the Surface concept or a completion's tag (that's the Surface)

**Cursor**:
The single keyboard point of attention — at most one **Task**, in exactly one
**Pane**. A view-layer affordance only: it tags nothing, stores nothing, and has
no meaning on touch.
_Avoid_: selection, focus (the DOM sense), highlight, active row, hovered row

## Shared language

These terms mean the same thing on every surface. Each surface adds its own terms
on top (see the surface contexts above).

**Task**:
A recurring chore with a stable **task id**, a **Category**, and a **recurrence
rule** — a template, not an **Occurrence**. On **Core** and **Rare** its display
**name** and optional **Note** come from the **Notes file**, keyed by task id; on
**Day** the name is carried inline in the schedule.
_Avoid_: item, todo, chore (in code), reminder

**Task id**:
A task's stable identity: lowercase letters, digits and dashes (`[a-z0-9-]+`),
globally unique across all surfaces. Distinct from the **name**, which may change.
_Avoid_: key, slug; name

**Category**:
The kind of a task on **Core** and **Rare**: the top-level keys of that surface's
**Schedule**, in file order, each surface deriving its own. Its label is
title-cased from the key (`:home-office` → "Home Office"); **Day** has none.
_Avoid_: section, group, type

**Schedule**:
Which **task id**s recur when, in a surface's own recurrence shape — the source
from which **Occurrence**s are computed. On **Core** and **Rare** it carries ids
and recurrence only, never names (those live in the **Notes file**); **Day** keeps
names inline.
_Avoid_: calendar, plan, config; holding display names (Core/Rare)

**Notes file**:
The single Markdown document giving **Core** and **Rare** tasks their display
**name** and optional **Note**, keyed by **task id**. Format:
[docs/notes-format.md](docs/notes-format.md).
_Avoid_: descriptions file, glossary, schedule

**Note**:
The optional multi-line Markdown prose attached to a task, supplied via the
**Notes file**.
_Avoid_: description, comment, blurb

**Occurrence**:
A specific dated instance of a **Task**, computed from its recurrence rule.
Derived, never stored.
_Avoid_: instance, entry, slot

**Done-through**:
The per-task **Occurrence** up to and including which a task counts as done —
forward-looking *coverage*, not a "last done". One value per task, surviving the
date rollover.
_Avoid_: completion, last done, due date, expiry, status, progress
