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

## License

MIT
