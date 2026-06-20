# PRD: Merge the Rare surface into one two-surface product

Status: ready-for-agent

## Problem Statement

I keep my recurring upkeep in two separate apps that look and feel almost
identical: a daily check-off of the chores due today (the **Core** surface), and
a separate app for slower monthly-to-yearly maintenance (the **Rare** surface,
currently the standalone "Orbit" / `routine` repo). They share styling, the same
two **Categories**, the same `task-id → Done-through` completion shape, and most
of their plumbing — yet I open two apps, install two PWAs, and only one of them
has the recent niceties (visibility/focus refresh, remote **Schedule** from a
gist, and Supabase sync). The maintenance app has none of those, and building
them a second time in a repo I'm about to retire would be wasted work.

## Solution

Fold the two apps into a **single product with two surfaces**, Core and Rare,
distinguished by **posture** rather than frequency:

- **Core** — _posture: today._ The pile-up areas (inboxes, files, photos) I've
  committed to a rhythm and clear the day they come due. Big tap-to-toggle
  buttons.
- **Rare** — _posture: when you can._ Slower upkeep done when time and energy
  allow, plus a few with real deadlines. Category cards with Current / Upcoming /
  Missed states.

One app, one PWA, one header. On a wide screen the two surfaces sit side by side
(Core left, Rare right); on a narrow screen they show one at a time and I swipe
between them. The recently-built Core features — visibility refresh, gist-backed
Schedule, Supabase sync — become shared code that serves both surfaces, so Rare
gains them without a second implementation.

## User Stories

1. As a user, I want one app holding both my daily chores and my slower upkeep, so that I stop juggling two near-identical apps.
2. As a user, I want a single installed PWA, so that my home screen has one icon, not two.
3. As a user on a wide screen, I want Core and Rare side by side (Core left, Rare right), so that I see my whole routine at a glance.
4. As a user on a narrow screen, I want one surface at a time, so that the small screen isn't crowded.
5. As a user on a narrow screen, I want to swipe horizontally between the two surfaces, so that switching feels native and effortless.
6. As a user on a narrow screen, I want Core shown first by default, so that my daily driver is what I land on.
7. As a user on a narrow screen, I want a subtle indicator that a second surface exists, so that I discover Rare without being told.
8. As a user, I want a single header with one wordmark and today's date, so that the merged app feels like one thing, not two stitched together.
9. As a user, I want the two surfaces to be visually self-evident (button grid vs maintenance cards), so that I don't need labels to tell them apart.
10. As a Core user, I want the big juicy toggle buttons to behave exactly as before, so that the merge costs me nothing I already rely on.
11. As a Core user, I want a task marked done to read as done through today and not-done on its next scheduled occurrence, so that coverage tracks the two-week rhythm.
12. As a Core user, I want my Schedule to honour Week parity (odd/even week), so that fortnightly rotations land on the right day.
13. As a Rare user, I want my maintenance cards, folds, frequency badges, missed counts and due labels exactly as before, so that the move loses none of that surface's behaviour.
14. As a Rare user, I want a Current task to show its earliest due Occurrence plus a Missed count, so that a backlog collapses into one row without hiding that it exists.
15. As a Rare user, I want Upcoming tasks hidden until I reveal them, so that caught-up work stays unobtrusive.
16. As a Rare user, I want deadline tasks to surface a "Due in N days" countdown inside their lead window, so that time-sensitive upkeep (e.g. bookkeeping) actually bites.
17. As a user, I want each surface to refresh when I return to the app (visibility/focus), so that rolling past midnight or reopening shows the correct day on both surfaces.
18. As a user, I want both surfaces' Schedules editable via a gist without a redeploy, so that I can adjust either routine from my phone.
19. As a user, I want a single gist holding both surfaces' Schedules behind one URL, so that there's one file to edit and one place to point at.
20. As a user, I want each surface to paint instantly from a cached/seed Schedule and revalidate in the background, so that the app is fast and correct offline.
21. As a user, I want completions on both surfaces to sync across my devices via Supabase, so that marking something done on my phone shows on my laptop.
22. As a user, I want a Core task and a Rare task to be able to share a name without clobbering each other's completion, so that the two surfaces' task vocabularies stay independent.
23. As a user, I want sync failures to be silent and safe (eventually consistent, never lossy), so that a flaky network never loses a toggle.
24. As an unconfigured user, I want the app to stay pure-local with no credentials, so that it works out of the box without setup.
25. As the maintainer, I want both surfaces to share one state pattern (pure core + store + subscription), so that I reason about and test them the same way.
26. As the maintainer, I want the business logic of both surfaces testable as plain data in/out, so that I don't need a DOM to verify behaviour.
27. As the maintainer, I want both Schedules in EDN with one parser, so that I stop carrying a second format and a JSON key-normalisation step.
28. As the maintainer, I want the sync/outbox/config code written once and instantiated per surface, so that the two surfaces never drift apart in their plumbing.
29. As the maintainer, I want a glossary that names the shared kernel and each surface's own terms, so that the language stays precise as the product grows.
30. As the maintainer, I want the retired maintenance repo archived after the copy-in, so that there's a single source of truth.

## Implementation Decisions

- **One product, two bounded contexts.** The merged app is a single product with two surfaces — **Core** and **Rare** — that share a kernel of language (Task, Category, Schedule, Occurrence, Done-through) but keep surface-specific terms (Core: Week parity; Rare: Interval, Anchor date, Current, Upcoming, Missed). The surfaces are distinguished by posture, not frequency. See ADR 0008 and `CONTEXT-MAP.md`.
- **"Completion" is retired** as a domain term; Done-through is the single completion concept on both surfaces. `completions` survives only as a code/storage name.
- **Module shape.** A thin shell module is the entry point and responsive container hosting both surfaces. A shared namespace holds storage, sync, config, date helpers, Done-through coverage primitives, and the cross-cutting view hooks (visibility/today, overflow). Two surface namespaces (Core, Rare) each hold that surface's store, view, schedule resolution, and seed. (Namespaces: `app.shell`, `app.shared.*`, `app.core.*`, `app.rare.*` — the former `app.core` entry is renamed to `app.shell` to free `app.core.*` for the Core surface.)
- **Shared state pattern.** Both surfaces converge on the maintenance app's split: a pure core (`init-state` / `toggle` / `select` — plain maps in and out, no atom, no localStorage) wrapped by a thin atom store and a subscription hook, with sync slotting in behind `init-state`. The check-off surface is refactored into this shape as the first step; its hooks-in-core logic is extracted out of React.
- **Per-surface completion store, shared mechanism.** Each surface keeps its own completion map under its own localStorage key and its own outbox. They sync through the same code and the same Supabase table from ADR 0007, now keyed `(surface, task_id)` via one additive `surface` column. The config blob's single `completionsDbUrl` is unchanged. No pooled store and no id-prefix convention. See ADR 0009.
- **Schedule format.** Rare's Schedule moves from JSON to EDN to match Core; the JSON key-normalisation step is deleted, not ported. One parser serves both.
- **Remote Schedule override.** One combined gist holding `{:core … :rare …}` behind the existing single `scheduleUrl`. The shell fetches once and hands each surface its slice; each surface still falls back gist → cache → seed and revalidates stale-while-revalidate (per ADR 0005).
- **Responsive shell.** A single flex container holds both surfaces with no swipe library. Narrow screens: horizontal scroll-snap (each surface full-width, snap-centered) giving native momentum swipe; Core is the first/default panel; a subtle 2-dot indicator. Wide screens (~1200px+): the same container flips to side-by-side at the surfaces' natural widths (Core ~`max-w-md`, Rare ~`max-w-2xl`), Core left, Rare right. The single header sits outside the scroll container.
- **Schema change.** Supabase `completions(task_id text, done_through date)` gains a `surface` column; primary key becomes `(surface, task_id)`. Additive, one table, one endpoint.
- **Repo strategy.** The maintenance app's source is copied into this repo under the Rare surface (history not preserved); the old repo is archived.

## Testing Decisions

- **Test external behaviour, not implementation.** Tests assert what goes in and what comes out of the pure cores — never how a store or hook is wired. Toggling, coverage, occurrence derivation, reconciliation, parsing, and projection are all plain-data functions and are tested as such.
- **The pure cores are the seams.** Both surfaces already expose their logic as data-in/data-out functions; the merge relocates these into the new namespaces and keeps them the highest test seam. No DOM, no atom, no localStorage in tests.
- **Modules under test.** Shared: Done-through coverage primitives, sync (`reconcile` / flush-payload / mark-dirty / clear-pending), config parsing, date helpers. Core: parity-based Schedule resolution and the day's task projection, plus its store's toggle/select. Rare: occurrence derivation and row collapse (Current/Upcoming/Missed, due labels) and its store's toggle/select.
- **Prior art.** The existing `node-test` suites carry straight over: Core's `schedule` / `completions` / `config` / `sync` / `tasks` / `date_utils` tests and Rare's `schedule` / `store` / `utils` tests. New shared-module tests follow the same `cljs.test` `deftest`/`is` style and run under the existing `pnpm test` (`shadow-cljs compile test && node`).
- **Per-surface sync regression.** A test asserts that a Core and a Rare task sharing a name do not clobber each other's Done-through, locking in the per-surface-store decision (ADR 0009).

## Out of Scope

- Any settings UI (surface labels, category-header toggle, etc.) — the merged shell has no surface labels by design; settings remain a future item.
- New completion/celebration polish (premium pop sound, card-flip done animation, focus timer, task icons) — unchanged by this merge.
- First-sync bootstrap of local edits upward — deliberately still skipped (per ADR 0007).
- Independent per-column vertical scroll on wide screens — page-level scroll for now.
- Shrink-to-fit side-by-side on tablets (~900–1024px) — natural widths only; tablets stay in swipe mode.
- Changing either surface's domain model (parity slots, anchor+interval) — the merge unifies plumbing and language, not the recurrence rules.
- Renumbering/relocating ADRs into system-wide vs per-surface directories, and updating `docs/agents/domain.md` to multi-context — follow-up doc housekeeping.

## Further Notes

- The glossary is already captured: `CONTEXT-MAP.md` (product framing + shared kernel) and per-surface `src/app/{core,rare}/CONTEXT.md`. `CLAUDE.md` points at the multi-context layout. The old root `CONTEXT.md` was retired.
- "Core" and "Rare" are working names for an axis that is posture, not cadence. The shell shows no surface labels, so the names are internal only and can change without user-facing churn.
- ADR 0007 already anticipated this merge ("the schema mirrors the sister rare-task app so the two can later merge"); ADRs 0008 and 0009 record the merge and the per-surface sync extension.
- Suggested sequence: (0) refactor Core onto the shared store pattern in place and ship; (1) copy Rare in as `app.rare.*`, convert to EDN, wire shared visibility/gist hooks; (2) merge the responsive shell; (3) shared Supabase sync for both surfaces; (4) doc/ADR housekeeping and archive the old repo.
