# OnTop

Keep track of your daily routines.

## Prerequisites

- Java 21+ (e.g. `brew install --cask temurin@21`) — required by shadow-cljs 3
- [Clojure CLI](https://clojure.org/guides/install_clojure) (`clj`)
- [clj-kondo](https://github.com/clj-kondo/clj-kondo) — `brew install clj-kondo`
- [Node.js](https://nodejs.org) and [pnpm](https://pnpm.io) (e.g. `corepack enable pnpm`)

`shadow-cljs`, `tailwindcss`, `husky`, and `lint-staged` install with `pnpm install`. `cljfmt` runs through a `deps.edn` alias, no install needed.

## Setup

```bash
pnpm install
pnpm dev
```

Then open http://localhost:8080.

## Custom schedule

The schedule shipped with the app is the seed in `src/app/seed.edn`. You can override it at runtime — without redeploying — by pointing the app at a [GitHub gist](https://gist.github.com) holding EDN of the same shape as `seed.edn`.

1. Create a gist with your schedule as an EDN file.
2. Grab its **raw-latest** URL — the raw link with the commit hash removed, so it always serves the newest revision:
   ```
   https://gist.githubusercontent.com/<user>/<id>/raw/<file>.edn
   ```
3. In the browser devtools console, store it:
   ```js
   localStorage.setItem("on-top/schedule-url", "https://gist.githubusercontent.com/.../raw/schedule.edn")
   ```

On every load the app paints instantly from the last good copy (or the seed), then fetches the gist in the background and swaps it in. If the gist is missing, unreachable, or not valid EDN, the app keeps the last good schedule and falls back to the seed — the reason is logged to the console. To revert to the seed, clear both keys (`on-top/schedule-url` and the cached copy `on-top/schedule-cache`); dropping only the URL stops refreshing but the last cached gist still shows. There's no settings UI yet; this is devtools-only for now.

## License

MIT
