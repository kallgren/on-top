# Day timetable — UI prototype notes

**THROWAWAY.** The look has been chosen; fold it into the real code and delete
this (see "Cleanup").

## Question (answered)

What should the Day timetable *look like*? Explored four variants (Ledger,
Ledger+now, Fit-to-screen, Now-focus); **F — Fit-to-screen won.**

## Run it

```
pnpm prototype
```

→ http://localhost:8081/day-prototype.html

Tap a Block to toggle its done-state (in-memory only). The now-line uses the real
clock when within the day's span, else a 13:42 demo time.

## The chosen design

A vertical timetable that **fits the whole day in the viewport** (no scroll),
squeezed to **2/3 width** and centred. Start times in a left gutter aligned to
each Block's top border; the **day's end time** closes the gutter under the last
Block. Sharp bordered boxes, name centred.

- **Layout fit.** Water-filled: each Block's height is duration-proportional but
  floored at 44px so short Blocks stay tappable. Proportionality therefore
  **bends** near the floored short Blocks (DW and Free read a touch shorter than
  their true share to pay for the floored 30-min trio). This is the
  "fit-whole-day" layout the original handoff rejected for crushing short
  Blocks — revisited and accepted here with the floor.
- **Current Block.** Filled blue, white text, small "NOW" tag.
- **Past Blocks.** Dimmed to 40% (gutter time + box).
- **Done.** Green box, **name stays**, white ✓ at the right.
- **"Free" special case.** Open/unstructured time: never fills blue — keeps a
  surface bg with a **dashed border** (blue when active), and its label is
  **always dimmed**.
- **Now-line.** Blue rule across the timetable + blue current-time in the gutter.

Seed: DW 05:00–09:30, NM 09:30–10:30, Core tasks 10:30–11:00, Todoist
11:00–11:30, Messages 11:30–12:00, Free 12:00–19:30, Wind down 19:30–20:30.

## Still open before folding in

- **Name-stays-on-done** is the prototype's bet (vs Core's replace-with-check) —
  confirm it still reads right.
- Whether "fit the whole day, no scroll" survives a **longer day** than this seed
  (more Blocks ⇒ thinner boxes). The floor protects short Blocks but at some point
  the tall ones get squeezed flat.

## Cleanup (once folded in)

Delete: `src/app/day/prototype.cljs`, `src/app/day/NOTES.md`,
`public/day-prototype.html`, `public/js-prototype/`, the `:prototype` build in
`shadow-cljs.edn`, and the `prototype` script in `package.json`. Then fold the
look into the real `day/view.cljs` (and add a proper `--color-now` blue token to
`main.css` — the prototype hardcodes `#1f6feb` / `#5b9dff`).
