# Supabase-truth sync with a pending outbox

Completions sync across devices via Supabase as the source of truth; localStorage is only a read-cache, never a write-source. Load and focus hydrate as `Supabase ⊕ pending` (pending wins); each toggle writes a durable `pending` outbox entry and fires one per-task upsert against `completions(task_id text pk, done_through date)`, clearing the entry on success. Done is forward-looking coverage rather than a today-only flag: a read selects `done_through >= today`, marking upserts `done_through = today`, and unmarking upserts `done_through` back to the task's previous scheduled occurrence — so every toggle is an idempotent upsert with no delete path, storage is bounded by task count, and the date rollover needs no sweep. Chosen over a localStorage-truth model (a second device would never see the first) and a single JSON blob row (a stale device clobbers unrelated tasks); first-sync bootstrap is skipped on purpose so local edits never flow upward, and we use the publishable key with one permissive RLS policy plus raw `fetch` rather than auth or `supabase-js`. The schema mirrors the sister rare-task app so the two can later merge.

This consolidates remote credentials into a single localStorage config blob holding the Supabase endpoint, the publishable key, and the schedule's gist URL — superseding the standalone schedule-URL pointer from 0005. Nothing ships in the public bundle, so an unconfigured app stays pure-local exactly as before. The Schedule itself is unaffected otherwise: still read-only, still falling back gist → cache → seed.

## Consequences

- Manual localStorage surgery is inert: wiping the done cache re-hydrates from Supabase, hand-edits are overwritten on the next read. Only `pending` is ever pushed.
- Sync failure is silent (`console.warn`) but safe — the outbox makes divergence eventually-consistent, not lossy.
- Credentials are seeded via devtools, one-time per device (an iOS PWA needs a one-time tether). No settings UI.
