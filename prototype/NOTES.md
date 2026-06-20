# Prototype: merged two-surface shell

**Question:** How should Core + Rare sit together in one view?

**Answer (the keeper):** A "bulletin board" layout.

- One header on top (wordmark + date), outside the scroll area.
- **Wide (≥1200px):** Core is the flexible *main* area, centering its `max-w-md`
  button column as the focus. Rare is a fixed `42rem` column the main area
  pushes against the right edge — anchored like a companion, with no border /
  background / chrome that announces "sidebar." Rare's content keeps Orbit's
  natural width (`max-w-2xl` minus its own `px-7` ≈ 38.5rem).
- **Narrow:** one surface at a time via native scroll-snap (Core first), with a
  2-dot indicator. Untouched by the wide layout.

Rejected on the way: centered fixed-width pair with side gutters; equal 50/50
halves; Core-widened-but-centered. All felt balanced-but-wrong. The win was
making Core the *main* region and Rare a quiet right anchor.

**Status:** throwaway reference. Markup/classes copied verbatim from
`on-top/src/app/core.cljs` (Core) and `routine/src/app/core.cljs` (Rare);
styled by the project's own Tailwind CLI (`input.css` → `main.css`). When the
real `app.shell` + `app.rare.*` get built in ClojureScript, this is the visual
reference. Delete then.

Rebuild CSS after editing the HTML:
`./node_modules/.bin/tailwindcss -i prototype/input.css -o prototype/main.css`
