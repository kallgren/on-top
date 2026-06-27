# Categories derived from Schedule keys

## Context

Categories were a fixed two-item allowlist (`:digital`/`:household`) compiled into
each surface's view, duplicated across both. The glossary called them "exactly
two, fixed, not user-configurable," and 0008 listed Category among the shared
kernel identical across surfaces. But the keys already live in each Schedule file,
and adding or renaming a Category meant a code change to match a config change.

## Decision

A surface's Categories are the top-level keys of its own Schedule, derived at
render time. One function, `schedule->categories`, returns ordered `[key label]`
pairs as the single source of truth; callers needing only keys slice the firsts.
Each surface derives independently from its own file, the label is title-cased
from the key (`:home-office` → "Home Office"), and Categories render in the order
the keys appear in the file.

## Considered options

- **Explicit `:category-order` vector in the Schedule** — robust and controllable,
  but a second source of truth to keep in sync with the keys.
- **Alphabetical order** — robust, but surrenders control over ordering.
- **File order via EDN insertion order** (chosen) — no extra config, order is just
  how you wrote it; fragile past 8 keys, which a personal config won't hit.

## Consequences

- The two surfaces may diverge in their Categories; accepted, not guarded against
  (per 0010's separation of the two files).
- Order relies on EDN array-map insertion order, preserved only up to 8 keys;
  beyond that it is undefined. An acceptable ceiling here.
- No validation: a mistyped key with no tasks drops out (empty Categories never
  render), and one with tasks surfaces its own misfiling. Title-casing has no
  acronym escape hatch.
