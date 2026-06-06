# Use UIx for the React layer

Reagent is the default CLJS UI layer and the reflexive suggestion; we use UIx instead. UIx is hooks-first and treats React as a first-class citizen (a good fit for a React background), keeps Hiccup syntax for readable markup, and has clean shadow-cljs integration. Reagent's reactive-atom model and pre-hooks API add indirection we'd rather avoid. If an accessible component primitive is ever needed, UIx's plain React interop covers it.
