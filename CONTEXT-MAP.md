# On Top

A calm personal routine system with two surfaces over one shared language:
a daily check-off of the chores due today, and a slower surface for upkeep you
get to when you can. Tapping marks things done; nothing nags.

This map fixes the language shared by both surfaces and points to where each
surface's own language lives. It is a glossary, not a spec — no implementation
details.

## Surfaces

**Surface**:
One of the two self-contained halves the product presents — **Core** or **Rare**.
Each was once a standalone app; now each is a bounded context with its own
**Schedule**, view, and completion store, sharing the kernel language below. The
two are distinguished by **posture** — the relationship you have to the work —
not by how often a task recurs. ("Core" and "Rare" are working names; the
distinction is posture, not frequency.) A Surface is a *concept* — a bounded
context, the unit a completion is tagged with — not the region of screen it
occupies: it is *rendered in* a **Pane**. Reserve "surface" for "Core or Rare".
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

**Pane**:
The on-screen region that renders one **Surface** in the shell's current layout.
A view-layer term, not a domain one — the Pane can change shape (side by side,
swipeable) without touching what a Surface *is* or how a completion is tagged.
One Pane per Surface today.
_Avoid_: using "Pane" for the Surface concept or a completion's tag (that's the
Surface)

## Shared language

These terms mean the same thing on both surfaces. Each surface adds its own
terms on top (see the surface contexts above).

**Task**:
A recurring chore with a stable **id**, a **Category**, and a display **name** —
the name can be reworded without changing the task's identity or losing its
**Done-through** history. A task is a template, not an **Occurrence**: it carries
a **recurrence rule** that places it in time and generates many dated occurrences.
Within a surface a task belongs to exactly one Category. The recurrence rule's
*shape* is surface-specific.
_Avoid_: item, todo, chore (in code), reminder

**Category**:
The kind of a task. Exactly two exist: `:digital` and `:household`. Fixed, not
user-configurable. The same two categories on both surfaces.
_Avoid_: section, group, type

**Schedule**:
The full set of **Task** definitions and their recurrence rules for one surface —
the source from which **Occurrence**s are computed. A domain concept ("add Gmail
to the schedule"), independent of how or where it's stored. Each surface has its
own Schedule, stored its own way.
_Avoid_: calendar, plan, config

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
