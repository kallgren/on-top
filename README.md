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

## Configuration

Runtime settings live in a single device-local JSON blob under the `on-top/config` localStorage key (see `docs/adr/0007`). Every key is optional — an unconfigured app stays purely local, painting from each surface's seed schedule with no remote sync.

| Key | Purpose |
| --- | --- |
| `coreScheduleUrl` | Gist URL for a custom Core schedule — see [Custom schedule](#custom-schedule) |
| `rareScheduleUrl` | Gist URL for a custom Rare schedule — see [Custom schedule](#custom-schedule) |
| `completionsDbUrl` | Supabase REST endpoint for the completions table — see [Completion sync](#completion-sync-supabase) |
| `supabasePublishableKey` | Supabase publishable key |

Set it in the browser devtools console. To avoid clobbering keys you've already stored, merge rather than overwrite:

```js
const config = JSON.parse(localStorage.getItem("on-top/config") ?? "{}")
localStorage.setItem("on-top/config", JSON.stringify({
  ...config,
  coreScheduleUrl: "https://gist.githubusercontent.com/.../raw/core-schedule.edn",
  rareScheduleUrl: "https://gist.githubusercontent.com/.../raw/rare-schedule.edn",
  completionsDbUrl: "https://<project>.supabase.co/rest/v1/completions",
  supabasePublishableKey: "<publishable-key>"
}))
```

Unknown keys are ignored (and logged). Seeding is one-time per device; there's no settings UI yet, so this is devtools-only for now.

## Custom schedule

Each surface ships with its own seed schedule (`src/app/core/seed.edn` and `src/app/rare/seed.edn`). You can override either at runtime — without redeploying — by pointing its config key at a [GitHub gist](https://gist.github.com) holding EDN of the same shape as that surface's seed. Core reads `coreScheduleUrl`, Rare reads `rareScheduleUrl`; each surface fetches its own gist independently, so you can edit one routine without the other in front of you.

1. Create a gist with one surface's schedule as an EDN file.
2. Grab its **raw-latest** URL — the raw link with the commit hash removed, so it always serves the newest revision:
   ```
   https://gist.githubusercontent.com/<user>/<id>/raw/<file>.edn
   ```
3. Store it as `coreScheduleUrl` or `rareScheduleUrl` in `on-top/config` (see [Configuration](#configuration)).

On every load each surface paints instantly from its last good copy (or its seed), then fetches its gist in the background and swaps it in. If a gist is missing, unreachable, or not valid EDN, that surface keeps its last good schedule and falls back to its seed — the reason is logged to the console. To revert a surface to its seed, drop its URL from the config blob and clear its cached copy (`on-top/core-schedule-cache` or `on-top/rare-schedule-cache`); removing only the URL stops refreshing but the last cached gist still shows.

## Completion sync (Supabase)

Completions sync across devices through Supabase, with localStorage as a read-cache only (see `docs/adr/0007`). It's optional — without `completionsDbUrl` and `supabasePublishableKey` in the [config blob](#configuration), the app stays purely local.

### Database

Run this once in the Supabase SQL editor to create the table and its policies. Completion state is recoverable (the app re-hydrates from Supabase and merges the local outbox), so it's safe to re-run from the `drop` if you need a clean slate.

```sql
drop table if exists completions;

create table completions (
  surface      text not null,
  task_id      text not null,
  done_through date not null,
  primary key (surface, task_id)
);

alter table completions enable row level security;

create policy "anon read"   on completions for select using (true);
create policy "anon insert" on completions for insert with check (true);
create policy "anon update" on completions for update using (true) with check (true);
```

The app talks to PostgREST with the publishable key over raw `fetch`. Toggling a task issues an upsert (`INSERT … ON CONFLICT DO UPDATE`), so it needs **both** `insert` and `update` policies — `insert` validates the new row via `with check`, `update` gates existing rows via `using`. There's no delete path, so no delete policy. `(surface, task_id)` is the composite primary key the upsert resolves the conflict onto, so a task id reused across surfaces never clobbers (see `docs/adr/0009`).

### Pointing the app at it

Set `completionsDbUrl` (your project's `…/rest/v1/completions` endpoint) and `supabasePublishableKey` in the [config blob](#configuration).

## License

MIT
