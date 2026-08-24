# Configuration

OnTop runs with no setup at all — an unconfigured app stays purely local, painting
from each surface's compiled-in seed schedule with no remote sync. This document
covers the optional parts: pointing it at your own tasks, and syncing completions
across devices.

Runtime settings live in a single device-local JSON blob under the `on-top/config`
localStorage key (see [ADR 0007](adr/0007-supabase-truth-sync-with-localstorage-cache.md)).
Every key is optional.

| Key | Purpose |
| --- | --- |
| `gistUrl` | URL of one gist holding every remote file — `core.edn`, `rare.edn`, `day.edn`, `notes.md` — see [Custom schedule](#custom-schedule) and [Notes file](#notes-file) |
| `completionsDbUrl` | Supabase REST endpoint for the completions table — see [Completion sync](#completion-sync-supabase) |
| `supabasePublishableKey` | Supabase publishable key |

Set it in the browser devtools console. To avoid clobbering keys you've already stored, merge rather than overwrite:

```js
const config = JSON.parse(localStorage.getItem("on-top/config") ?? "{}")
localStorage.setItem("on-top/config", JSON.stringify({
  ...config,
  gistUrl: "https://gist.github.com/<user>/<id>",
  completionsDbUrl: "https://<project>.supabase.co/rest/v1/completions",
  supabasePublishableKey: "<publishable-key>"
}))
```

Unknown keys are ignored (and logged). Seeding is one-time per device; there's no settings UI yet, so this is devtools-only for now.

## Custom schedule

Each surface ships with its own seed schedule (`src/app/core/seed.edn`, `src/app/rare/seed.edn`, and `src/app/day/seed.edn`). You can override them at runtime — without redeploying — by pointing `gistUrl` at a single [GitHub gist](https://gist.github.com) holding the override files under fixed names: `core.edn`, `rare.edn`, and `day.edn` (plus `notes.md`, see [Notes file](#notes-file)). Each is EDN of the same shape as that surface's seed and is optional — a name that isn't present just leaves that surface on its seed. Core and Rare are maps of categorised tasks; Day is a flat, ordered vector of time blocks (`{:id "..." :name "..." :start "HH:MM" :end "HH:MM"}`, with `:open? true` marking free time) — match the shape of the surface's seed.

In Core and Rare, the map's top-level keys **are** the categories — name and order them however you like (each surface derives its own from its own file). A key becomes a heading title-cased from its name: `:digital` shows as "Digital", and a multi-word key written with dashes, `:home-office`, shows as "Home Office". Keys render in the order you write them, and a category with no tasks due simply doesn't render (see [ADR 0012](adr/0012-categories-derived-from-schedule-keys.md)).

1. Create one gist and add only the files you want to override, each under its expected name (`core.edn`, `rare.edn`, `day.edn`, `notes.md`). Any name you leave out stays on its seed — the files are independent.
2. Store the gist's URL as `gistUrl` in `on-top/config` (see [Configuration](#configuration)). Either form works — the gist page URL or a file's raw URL — the app reduces it to each file's **raw-latest** link (the commit hash removed, so it always serves the newest revision):
   ```
   https://gist.githubusercontent.com/<user>/<id>/raw/<file>
   ```

Each surface fetches its own file from the gist independently. On every load it paints instantly from its last good copy (or its seed), then fetches in the background and swaps in the result. If the gist is missing or unreachable, or a file is absent or isn't valid EDN, that surface keeps its last good schedule and falls back to its seed — the reason is logged to the console. To revert a surface to its seed, remove its file from the gist and clear its cached copy (`on-top/core-schedule-cache`, `on-top/rare-schedule-cache`, or `on-top/day-schedule-cache`); dropping `gistUrl` stops refreshing every surface but the last cached copies still show.

The design is recorded in [ADR 0005](adr/0005-remote-schedule-override-via-gist.md) and [ADR 0010](adr/0010-per-surface-gist-schedule-overrides.md).

## Notes file

Core and Rare schedules carry only ids; each task's display **name**, its optional **note** and its optional **link** live in one global Markdown notes file, shared across both surfaces (Day keeps its names inline). Override the seed (`src/app/seed-notes.md`) by adding a `notes.md` file to the same [`gistUrl`](#custom-schedule) gist. For the authoring format, see [notes-format.md](notes-format.md).

A link points at wherever a task's real instructions live — a note in your notes app, a doc, a dashboard. Give a task one by adding a `[link]:` line under its heading, with a blank line above it:

```markdown
# Gmail inbox `gmail`

[link]: upnote://x/9f3c1a

Two-minute rule: reply, archive, or turn it into a task.
```

Press <kbd>o</kbd> with the keyboard cursor on that task to open it. Nothing in the app shows the link or hints that one exists, and a task without one does nothing when you press the key. Any scheme works — `https:`, or a custom one like `upnote://` that opens a desktop app — except those that execute script.

## Completion sync (Supabase)

Completions sync across devices through Supabase, with localStorage as a read-cache only (see [ADR 0007](adr/0007-supabase-truth-sync-with-localstorage-cache.md)). It's optional — without `completionsDbUrl` and `supabasePublishableKey` in the [config blob](#configuration), the app stays purely local.

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

The app talks to PostgREST with the publishable key over raw `fetch`. Toggling a task issues an upsert (`INSERT … ON CONFLICT DO UPDATE`), so it needs **both** `insert` and `update` policies — `insert` validates the new row via `with check`, `update` gates existing rows via `using`. There's no delete path, so no delete policy. `(surface, task_id)` is the composite primary key the upsert resolves the conflict onto, so a task id reused across surfaces never clobbers (see [ADR 0009](adr/0009-per-surface-completions-with-surface-discriminator.md)).

### Pointing the app at it

Set `completionsDbUrl` (your project's `…/rest/v1/completions` endpoint) and `supabasePublishableKey` in the [config blob](#configuration).
