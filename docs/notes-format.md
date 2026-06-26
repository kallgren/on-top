# Task notes format

One global Markdown file gives every Core and Rare task its display **name** and
optional **note** — one definition per task, keyed by **id** and shared across both
surfaces. A name lives in exactly this one place, so reword it here without
touching identity or Done-through; the binding is the id. Day is out of scope: its
schedule keeps names inline and never consults this file.

This is the one bespoke format in the product. For what the terms mean (Task, Task
id, Notes file, Note), see [CONTEXT-MAP.md](../CONTEXT-MAP.md).

## Grammar

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

## Resolved semantics

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

`[a-z0-9-]+` — lowercase letters, digits, dashes. Matching against a schedule is
exact and case-sensitive. Ids are globally unique across surfaces, so a definition
shared by a Core and a Rare task gives both the same name and note, by design.

## Diagnostics

The notes file is enrichment, not structure: it never hard-fails the product. It
warns and degrades, the product running on id-fallback names.

| Condition | Tier | Behaviour |
|---|---|---|
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
