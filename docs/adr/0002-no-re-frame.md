# No re-frame

re-frame is the canonical CLJS state-management library and will be reflexively suggested. We start with a single CLJS atom plus a small `use-subscribe` hook instead — the state involved is small enough that re-frame's events/effects/co-effects machinery would be ceremony without payoff. Migrating to re-frame later is well-trodden if the app outgrows this shape.
