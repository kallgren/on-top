# Keyboard cursor as controlled state, owned per surface

Keyboard navigation is driven by a *cursor* held as ordinary state — a per-surface index into that surface's visible rows — not by native DOM focus. Each surface owns its cursor through a shared hook, fed its own rows and toggle, so each keeps the traversal its layout needs. The shell owns only which surface is active and the moves between them; keeping just the active surface's keys live is what guarantees a single cursor. The cursor's rules are deliberately custom — dormant until woken, a remembered position per surface, advancing in place when a toggled row leaves the list, clamping at the ends — and owning the state makes each a few lines rather than a fight with the browser's own focus handling.

## Considered options

- **Native DOM focus (roving tabindex).** Buys a focus ring, scroll-into-view, and a11y for free. Rejected: every custom rule above collides with the browser's own focus moves (clicks, Tab, chrome), and the free a11y is low value for a single-user tool.
- **One cursor centralised in the shell.** Rejected: cracks open each surface's encapsulation and makes the shell learn two row shapes, for no gain over gating the active surface.

## Consequences

- Scroll-into-view and the focus ring are hand-rolled; the ring reuses the cue colour as one swappable token.
- No accessibility semantics beyond the underlying buttons — fine for one user, revisit otherwise.
- The layer is inert on touch (keys never fire), so the mobile experience is untouched.
