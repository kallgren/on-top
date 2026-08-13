# Week grid — how to bring the prototype into production

Written 2026-08-13, on the throwaway branch `prototype/week-grid`. This is the
handover note for reviving it later: what the prototype settled, exactly what it
touches, and what to fix before it earns a place in the app.

The prototype lives in `week_grid_prototype.cljs` on this branch and nowhere
else. Nothing here is on `main`.

## What the prototype settled

Decisions reached by building it and looking at it. These supersede the original
handoff wherever they disagree.

- **Blocks always run ascending.** The upper week is never later than the lower
  one. The block matching today's **Week parity** is this week; the other is the
  adjacent week on whichever side keeps Schedule order (`:week-odd` above
  `:week-even`) and ascending order agreeing — next week when this week is odd,
  last week when it is even. This overturns the handoff's "the other block is
  the previous week, never the next one", which put the later week on top half
  the time.
- **At the 53-week boundary** the adjacent week repeats this week's parity, so
  the week on the other side stands in and the pair is still sorted ascending —
  which flips Schedule order for that one week a year. Asserted in the test.
- **Blocks are labelled `W33`, `W34`** in the left gutter. Week number only: no
  parity, no date range, no "this week" marker. The number is what removes the
  ambiguity about which real week a block is anchored to.
- **Cells keep their label whether or not they are done**, so the grid reads as
  the Schedule and not only as a state of completion — the overview is the point
  of the face. Done is the green fill plus a small corner tick; the day face
  keeps its big ✓.
- **Today's column is a step darker than the page**, not lighter, so it reads as
  a trough the cells sit in. The weekday letter above it carries the same fill
  so the column has a head. This is the `--color-today` token.
- **Occurrences after today are inert** — dimmed and disabled, still readable as
  schedule, not claimable as coverage. This overturns the handoff's "future
  cells are tappable and that is intended". Consequence: the whole lower block
  is read-only whenever this week is odd.
- **No new store operation was needed.** `core.store/next-done-through` turned
  out to already be parameterised by the date it is asked about, so marking at D
  sets Done-through to D and unmarking at D rolls back to the Occurrence before
  D. The only addition is dropping the key when there is no earlier Occurrence
  (`unset` in the prototype ns). The deferred question of whether the dated
  unmark warrants an ADR resolves to **no** — there is no new decision.

Rejected along the way: chronological block order ignoring Schedule order; a
quiet green-outline done state; a big ✓ with a permanent caption under every
cell; one row per Task across fourteen days. See the branch history.

Still unanswered, because it needs everyday use rather than a look: whether the
runs-of-checks-then-blank pattern actually reads as "here is where each Task's
frontier sits", and whether the live area shifting shape week to week (bottom
block inert in odd weeks, top block all past in even weeks) reads as calm
orientation or as half the screen being greyed out.

## What it touches

The entire contact surface with production, beyond the one self-contained ns:

| File | Change |
| --- | --- |
| `app/keymap.cljs` | one binding line (`w`) |
| `app/shell.cljs` | require, hoist `categories`, the `grid?` hook, one `if` around `surfaces`, suppress the drawer |
| `app/date_utils.cljs` | `defn-` → `defn` on `iso-week` |
| `css/main.css` | the `--color-today` token, both themes |
| `app/core/CONTEXT.md` | the **Week grid** glossary entry |

## Why it can go in as a trial

It is keyboard-only, gated at its own ~1100px threshold, and ephemeral — not
persisted, so a reload lands back on the daily buttons. Someone who never
presses `w` cannot reach it: no mobile surface, no button, no state to land in.
That is a stronger isolation guarantee than a feature flag, so it does not need
one.

## Fix before merging

1. **The double store** — the only real one. `use-grid-store` creates a second
   store atom over the same localStorage keys as `core.store`. It is correct
   only because the grid unmounts `surfaces`, so exactly one of the two is ever
   alive and handoff happens through localStorage. Nothing enforces that
   invariant; the day the grid becomes a pane instead of a takeover, the two
   stores diverge silently. Either hoist `use-store` into the shell and hand
   both faces the same store (touches `core/view` and `core/store`), or keep it
   and state the invariant loudly in the docstring. For a trial, the latter.
2. **`?` is unreachable from the grid.** The help overlay lives inside
   `corner-controls`, which the takeover suppresses — so the one place `w` is
   documented is the one place you cannot reach while using `w`. Keep
   `help/view` mounted.
3. **`d`, `r` and `c` still fire while the grid is up**, silently mutating pane
   state you cannot see, and `d`/`c` persist it — so you exit into a Layout you
   did not ask for. Make them no-ops while the grid is active, or have them exit
   the grid first.

Already deferred and still deferred: 2D keyboard navigation, mobile treatment,
`Enter`/details, any change to the day face.

## Suggested route

Branch `feature/week-grid` off `main`, cherry-pick both commits from this
branch, rename the ns to `app.core.week-grid` and rewrite its docstring, apply
fixes 2 and 3, document fix 1. That is a small, honest PR to live on for a few
weeks before deciding whether the face earns its place.
