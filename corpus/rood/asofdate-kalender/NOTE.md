# asofdate-kalender

A case whose `asOfDate` has the right shape but names no day:
`2026-02-30`. The case schema admits it. All three executors refuse it
by name while they read the case, before anything is evaluated.

## The shape

`ir.json` is `golden/jutterswet-vensterheffing` byte for byte: two
independently dated rules, with distinct names, that write distinct
attributes. `vensterheffing in 2026` writes vensterheffing and is valid
from 2026-01-01 to 2026-12-31; `overgangsheffing tot en met 2025` writes
overgangsheffing and is valid up to 2025-12-31. What the document
exercises is the validity WINDOW, not version selection: for each rule
on its own, the evaluation date decides whether the window admits it.
The case is that golden's input (M1, meldwaarde 100) with another
evaluation date. The document was taken over whole because its answer
depends on that date.

## Why 2026-02-30

The case schema's `asOfDate` pattern is `^[0-9]{4}-[0-9]{2}-[0-9]{2}$`.
It checks the field widths and not the calendar. Measured against the
schema with the validator gate 4 uses: a day-first `30-02-2026`, a
short `2026-2-3` and a signed `+02026-02-28` are rejected, while
`2026-13-01`, `2026-00-10`, `2026-04-31`, `2025-02-29` and `2026-02-30`
are all admitted. This case takes `2026-02-30`, for three reasons.

- February has no 30th day in any year, so the hand check needs no
  leap-year rule.
- As a string the lexeme sorts between 2026-02-28 and 2026-03-01. A
  reader that compares the raw string places it inside the 2026 window
  and gives a plausible answer (below). `2026-13-01` sorts after every
  2026 date and gives a visibly odd one.
- The IR schema's pattern self-test (gate 2) pins the same lexeme as
  the layer boundary for a validity bound: the pattern admits it and the
  next layer refuses it. This case is that boundary on the case wire,
  with the case reader as the next layer.

## Why each executor fails

- `ref` and the ASP backend share one case reader. It reads `asOfDate`
  through the strict date reader at load, before it builds the world,
  and refuses a lexeme that names no day: `CASE ERROR: asOfDate
  "2026-02-30" is not the uchar-case-v0 date lexeme yyyy-mm-dd (day out
  of range in date lexeme "2026-02-30")`. Both pins are that whole
  sentence, including the reader's own reason, because the reason is the
  calendar half this case exists for. As with every load fault in this
  zone, the two pins are one reader seen twice: a regression fence, not
  a differential.
- The Drools backend reads `asOfDate` with its own strict reader and
  refuses it: `malformed date '2026-02-30' on 'asOfDate': the field
  widths are right but the calendar is not`, followed by the Java
  runtime's parse message in parentheses. The pin stops before that
  parenthesis, because that part is the platform's text and not the
  backend's. The red runner reads the date after the KieBase is built
  and before any rule fires, and reports the refusal as FAULT, because
  the backend treats a malformed lexeme as a document defect and not as
  a form it does not carry. So gate 8's Drools leg pins the backend's
  own sentence. The green replay path reports the same refusal as ERROR
  in both branches, for the same reason.

## What the old reader answered

Until 2026-09-23 the shared case reader kept `asOfDate` as an opaque
string, and the validity gate compared it with the window bounds as
strings. By hand: "2026-02-30" is not below "2026-01-01" and not above
"2026-12-31", so the window of `vensterheffing in 2026` admits it and
vensterheffing = 100 + 25 = **125**. "2026-02-30" is above
"2025-12-31", so the window of `overgangsheffing tot en met 2025`
excludes it and overgangsheffing stays **leeg**.

Measured on this document with runners built from before the change:
`ref` reported `EXPECTED FAULT DID NOT OCCUR: evaluation succeeded`, and
the ASP runner reported `EXPECTED FAULT DID NOT OCCUR: clingo found a
model`. A copy of the case with those two hand values as `expected`
passed in both. That is the 2026 answer, given as if February 30 were a
day of 2026, and nothing said otherwise. The Drools reader already
refused the lexeme then, so two executors of three answered in silence.

## Where the calendar check lives

A regular expression could list the month lengths and the Gregorian
leap rule, so the schema could be taught the calendar. It is not, on
purpose. The schema checks files; the reader checks every value that
reaches an executor by any route, so the reader has to hold the fence
either way. A calendar in a pattern would be a second one that must stay
in step with the reader's. And if the pattern refused this lexeme, the
case could not live in this zone at all, because gate 4 validates every
red case: the refusal would be pinned in the executors' own suites
alone again.
