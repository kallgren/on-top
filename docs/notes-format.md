# Task notes format

How a task's **name** and **note** are authored and parsed. This is the
one bespoke format in the product; everything here is operational detail. For
what the terms mean (Task, Task id, Schedule, Notes file, Task definition, Note),
see [CONTEXT-MAP.md](../CONTEXT-MAP.md).

## Architecture

Two kinds of file, joined by **id**:

- **Core and Rare schedules drop names.** They carry ids and recurrence only —
  never names or notes. Each keeps its own shape:
  - **Core** — arrays of id strings:
    `{:digital {:week-odd {:monday ["gmail"] …}}}`
  - **Rare** — maps without `:name` (`:anchor`, optional `:before`):
    `{:digital {"monthly" [{:id "back-up-files" :anchor "Jun 14"} …]}}`
- **One global Markdown notes file** holds every Core/Rare task's name + note,
  one definition per task, keyed by id, shared across Core and Rare.
- **Day is out of scope.** The Day schedule keeps its names inline
  (`{:id … :name … :start … :end …}`) and never consults the notes file.

A Core/Rare name lives in exactly one place — the notes file. Reword it there
without touching identity or Done-through, because the binding is the id.

## Notes file — grammar

Applied after the input is normalised (CRLF → LF, leading BOM stripped).

```ebnf
notes        = preamble , { definition } ;
preamble     = { body-line } ;                  (* lines before the first heading — ignored *)
definition   = heading-line , { body-line } ;   (* body runs to the next heading-line or EOF *)

heading-line = opt-ws , "#" , ws , heading-text , "\n" ;
heading-text = ? text containing ≥1 code-span ? ;   (* id = LAST span; name = remainder, trimmed *)
code-span    = "`" , { char - "`" } , "`" ;
id           = lower , { lower | digit | "-" } ;    (* the span's contents must match this *)

opt-ws = { " " | "\t" } ;  ws = ( " " | "\t" ) , { " " | "\t" } ;
lower  = "a".."z" ;  digit = "0".."9" ;
```

## Notes file — resolved semantics

The grammar is a skeleton; these rules pin down what it can't express.

1. **Fence-aware headings.** A `heading-line` is recognised only when *not*
   inside an open code fence. A fence opens/closes on a line whose trimmed
   content starts with ```` ``` ```` or `~~~`; an unterminated fence runs to EOF.
   Inside a fence, lines starting with `# ` are ordinary body, never headings —
   so a shell `# comment` in a note won't split it.

2. **id = the last code span** on the heading line. **name = the heading text
   with that span removed, then trimmed.** A name may itself contain code spans;
   only the last one is the id. An empty name falls back to the id, so
   `` # `gmail` `` is valid shorthand for "task `gmail`, name = `gmail`".

3. **note = the body lines, verbatim**, with leading and trailing blank
   lines stripped and the interior left untouched — indentation, blank lines,
   `##` subheadings and fenced blocks all preserved. An empty or whitespace-only
   body means the task has no note.

4. **Join.** A definition attaches to the task(s) whose id matches, on Core or
   Rare. A scheduled id with no definition takes its id as its name and has no
   note.

## id

`[a-z0-9-]+` — lowercase letters, digits, dashes. Matching between a schedule and
the notes file is exact and case-sensitive; the same charset is enforced on
schedule ids, so the two sides cannot drift on case.

Ids are globally unique across all three surfaces — stricter than per-surface
done-through storage strictly requires (see
[docs/adr/0009](adr/0009-per-surface-completions-with-surface-discriminator.md)),
adopted so the notes file can key every Core/Rare definition by id alone. A
shared id therefore means a shared name and note, by design. Day participates in
the same global id namespace but draws its names from its own schedule, so a
notes definition matching only a Day id is an orphan (below).

## Diagnostics

The notes file is enrichment, not structure: it never hard-fails the product. It
warns and degrades, the product running on id-fallback names. Hard failure —
falling back to the compiled-in seed — stays a *schedule-EDN* behaviour only.

| Condition | Tier | Behaviour |
|---|---|---|
| Schedule EDN unparseable | Hard | Fall back to seed/cache + warn *(existing)* |
| Notes file unfetchable / not text | Warn | No notes; all names = id |
| Text before the first heading | Silent | Ignored |
| Heading with no / malformed / empty id | Warn | Skip that definition |
| More than one code span in heading | Silent | Last = id, the rest = name |
| Duplicate id | Warn | Last one wins |
| Definition id in no Core/Rare schedule (orphan) | Silent¹ | Ignore the definition |
| Scheduled id with no definition | Silent | name = id, no note |
| Empty notes file / no headings | Silent | All names = id |

¹ Ignore is correct-by-construction — enrichment only ever looks up *scheduled*
ids, so an unmatched definition is simply never read. The orphan *warning* is
deferred: emitting it needs the union of the Core and Rare schedules at one
point, a cost not yet worth paying.

## Example

```markdown
A title or comment up here is preamble — ignored.

# Gmail inbox `gmail`
Alternativ:
- Snooze eller Todoist om jag ska agera senare

# Calendar (2w/4w) `calendar`
Syfte att plocka upp TODOs.

## Var
- Google Calendar

# Deploy script `deploy`
Run this:

```bash
# build first — not a new task, it's inside a fence
make build
```

# `downloads`
```

Parses to: `gmail` (name "Gmail inbox", two-line note), `calendar` (name
"Calendar (2w/4w)", note with a `## Var` subheading), `deploy` (name "Deploy
script", note containing a code block whose `# build first` line is preserved),
and `downloads` (name falls back to "downloads", no note).
