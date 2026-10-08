# ref/ — the reference interpreter

A small, slow, readable tree-walker over `uchar-ir-v1` documents:
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
  2 = a load or cross-check error that no expectedFault pins (a
  pinned load fault observes as exit 0 since 2026-09-19).
- `test/test_ref.ml` — the worked examples of ch. 1–2 verbatim, plus the
  ch. 4.6 allocation examples (`dune build @ref/runtest`).

Gate 5 (`scripts/check-oracle.sh`) replays every
`corpus/golden/**/case*.json` and lists goldens that have no case file.

## Recorded readings and scope limits

Fail fast is the rule: any construct outside the implemented subset
faults with file/position context — never a silent fallback.

- **Date comparisons** follow the ch. 1.2d date row: ordering and
  inequality ride the numeric rules on the civil day number, but
  gelijk-aan is false on ANY empty operand — in particular both-empty,
  never the numeric two-empties-equal cell (v0's Tabel-38 guard on the
  date shape, `Arith.compare_op ~static_date`).
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
- **Creation runs in generations** (ch. 5.5). Within stratum k,
  generation 0 is the fixpoint of the forms that make nothing (rules,
  allocations, defaults groups); generation G+1 is every object and
  fact creation of the stratum against generation G's settled world,
  so no creation reads what another creation of its own generation
  makes or links, and a member initialisation takes the value at the
  moment of making whatever the document order. After each generation
  the forms that make nothing settle again, stratum by stratum: for
  j = 0, 1, …, k the forms of stratum ≤ j reach their fixpoint before
  a form of stratum j + 1 runs, so on a new instance a form of a
  higher stratum reads only what the lower strata have finished
  writing there. The stratum is done when a generation makes and
  links nothing; its consistency and uniqueness forms then fire once.
- **The generation budget** (ch. 5.5): a stratum admits 100
  generations that mint an object or lay a link, and a 101st that
  still does is the fault, never a truncation: "no fixpoint within
  100 generations — generation 101 still mints or links (object
  creation "werf eeuwig"), and divergence is a fault (ch. 5.5, 5.7)"
  (`corpus/rood/runaway-recursion`,
  `corpus/rood/finite-chain-over-budget`). The passes the forms that
  make nothing need between two generations do not count.
- **Divergence of the forms that make nothing is a fault, named where
  the state shows its cause** (ch. 5.7). These forms carry no budget
  of their own: without creations their graph is acyclic (WF-045), so
  stratum j's fixpoint is reached within as many passes as the stratum
  has such forms. The bound per stratum is the larger of 100 and that
  number plus one. When it runs out, two more passes are replayed form
  by form and the writes are read off the world
  (`World.changed_attrs`); an attribute of one instance that two or
  more writers change to different values in both passes is named
  with the writers, their values and the instance: "no fixpoint within
  100 passes — divergence is a fault (ch. 5.7): two writers of one
  target disagree: rule "melding op vinddag" writes 2026-03-04 and
  rule "melding op aanspoeldag" writes 2026-03-01 to the attribute
  meldingsdatum of V, a Vondst" (`corpus/rood/jutterswet-dubbele-melding`).
  Two conditional writers whose conditions both hold on one instance
  reach this (WF-079 refuses two unconditional ones), also when they
  sit in different strata, because the fixpoint of each stratum runs
  the forms of the lower strata again; so do a computing rule that
  evaluates leeg and the conditional initialisation beside it
  (ch. 5.4). Two writers that write the same value hold both their
  equations and play, and a standing writer (ch. 7.8) that evaluates
  leeg writes nothing.
- **Timelines** (ch. 3): `lib/timeline.ml` carries the algebra — read,
  calendar and anchored buckets with the month-end clamp, the grid a
  member reads (the calendar's, or the one a fixed start point lays —
  one function for the case loader, the `asOfDate` bucket and the day
  count; a grid of several units and a variable start refused), cut-merge
  (period-coalescing), temporal summation (contributor-skipping), and
  the temporal total with clipping — tested against the ch. 3 worked
  examples verbatim. Evaluation crosses the timeline/scalar boundary
  only through the mirrored collapse: the temporal total folds the
  `asOfDate` bucket's value the case carries (ch. 3.6, leeg for an
  absent timeline). A temporal attribute, temporal characteristic or
  granular parameter in any other scalar position is refused (ch. 3.3 —
  the §8.2 marker family). A temporal write target is refused too
  (ch. 3.11) except in the two shapes that produce buckets: the ch. 3.7
  pro-rata target and the ch. 3.8 day-count target. Never a silent
  coercion.
- **Not implemented** (each faults with context): day kinds,
  multi-period grids, dimension cells, dimension selections,
  variable/constant refs, rule groups, the "is gevuurd" test (row 30:
  decided as a dialect-only fixpoint predicate, not yet implemented),
  and a granular parameter read in a SCALAR slot — vintage selection is
  ch. 8 scope.
- **Landed since that list was written**, and no longer refused: the
  ch. 3.7 pro-rata and ch. 3.8 day-count temporal targets, navigated
  scalar reads over a hop path (WF-053), the die/dat subselection filter
  on a fold, object and fact creation, parent-scoped uniqueness
  (ch. 5.9, WF-054), uniqueness unions, and the temporal read of a
  granular parameter's own timeline.
