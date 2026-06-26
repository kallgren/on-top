Avoid adding comments to the code. Try making the code self-explanatory instead.

## Agent skills

### Issue tracker

Issues and PRDs live as GitHub issues, managed via the `gh` CLI; external PRs are not a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Default triage vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context layout: `CONTEXT-MAP.md` at the root holds the product framing and shared language; each surface keeps its own glossary at `src/app/<surface>/CONTEXT.md` (`core`, `rare`). See `docs/agents/domain.md`.