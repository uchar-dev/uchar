# ref/ — the reference interpreter

A small, slow, readable tree-walker over `regelspraak-ir-v0` documents:
the executable definition of `spec/RUNTIME-SEMANTICS.md`, on the
WebAssembly model. It shares no code with any production runtime; it
reuses exactly one library from this repository — the typed loader of
`spec/verifier/` (`ir_v0`, public name `uchar-ir-v0`) — so the verifier
and the interpreter cannot drift on the wire format or on name
normalisation.

## Layout

- `lib/value.ml` — the value domain (ch. 1.1): exact rationals (zarith),
  text, boolean, day/millisecond dates, enumeration, and **empty**.
- `lib/arith.ml` — every operator's empty rule (ch. 1.2–1.3), exact
  rational rule (ch. 2.2), unit rule (ch. 2.6), the five rounding modes
  (ch. 2.5), clamping (ch. 2.7), and the aggregation split (ch. 1.2e).
- `lib/alloc.ml` — the allocation recurrence of ch. 4.2 as a pure
  function; the ch. 4.6 worked examples test it directly.
- `lib/world.ml` — the fact base and the §9.5 error channel; the write
  boundary (ch. 2.4) with idempotent writes (ch. 5.1).
- `lib/eval.ml` — expressions, conditions, rule forms, and the fixpoint
  session (ch. 5).
- `lib/case.ml` — loader for `uchar-case-v0` files, owning the
  cross-checks the case schema assigns to this loader (label uniqueness,
  declared names, value variants, role targets).
- `bin/runner.ml` — `runner <ir.json> <case.json>`; exit 0 = every
  expectation matches exactly, 1 = mismatch or evaluation fault,
  2 = load or cross-check error.
- `test/test_ref.ml` — the worked examples of ch. 1–2 verbatim, plus the
  ch. 4.6 allocation examples (`dune build @ref/runtest`).

Gate 5 (`scripts/check-oracle.sh`) replays every
`corpus/golden/**/case*.json` and lists goldens that have no case file.

## Recorded readings and scope limits

Fail fast is the rule: any construct outside the implemented subset
faults with file/position context — never a silent fallback.

- **Date equality** follows the *numeric* empty-equality rules of
  ch. 1.2d (a day-precision date is a day count), not the non-numeric
  text/enum/boolean rules. Recorded as an adopted reading.
- **The connecting relation** for allocation recipients and instance
  folds is derived as the unique fact type relating the two object
  types — the recorded v0 wart of ch. 4.1; zero or two candidates fault.
- **Irrational results** are refused, not approximated: a square root
  reaches a value only under its mandatory explicit rounding (computed
  exactly on the true irrational via integer comparisons); a fractional
  power must denote an exact root. The v0 backends' decimal128/double
  approximations are implementation detail, not reference semantics.
- **The write boundary** rejects a value that does not fit the target's
  declared decimals (no implicit rounding anywhere, ch. 2.4).
- **Strata** (ch. 6.3): evaluation runs stratum by stratum, computed
  from the same dependency graph the verifier checks for WF-045/046
  (the loader library's `dep_forms` — shared by construction, so ref/
  and the verifier cannot drift); negated characteristic reads and
  channel reads sit strictly above their producers. A dependency cycle
  reaching ref/ is a fault, never divergence.
- **Timelines** (ch. 3): `lib/timeline.ml` carries the algebra — read,
  calendar and anchored buckets with the month-end clamp, cut-merge
  (period-coalescing), temporal summation (contributor-skipping), and
  the temporal total with clipping — tested against the ch. 3 worked
  examples verbatim. Evaluation reads a temporal attribute at the
  evaluation date (the case carries that bucket's value; the ch. 3.10
  gate faults a time-free read), and a year-granular parameter binds as
  the `asOfDate` vintage, fail-fast when unbound.
- **Not implemented** (each faults with context): temporal writes and
  pro-rata bucket producers (ch. 3.7/3.11), day kinds, multi-period
  grids, navigation paths, dimension cells, filters, variable/constant
  refs, object and fact creation, scoped uniqueness, uniqueness unions,
  rule groups, the "is gevuurd" test (row 30: decided as a dialect-only
  fixpoint predicate, not yet implemented), and parameter-set data
  beyond the single-vintage binding (ch. 8).
