# jutterswet-vormenkabinet — no replay case, by design

This golden is the **verifier-only witness cabinet** (see the "witness
cabinet" section of `corpus/golden/README.md`). It exists to witness,
positively and verifier-clean, the well-formedness rules whose constructs
no *executing* corpus document carries yet — closing the Golden column of
the WF index for WF-004/006/014/016/017/021/031/032/039/040/041/042/043
(and the enum-assignment slice of WF-023). It deliberately carries no
`case*.json`, so gate 5 (the oracle) lists it as "NOT replayed, no case
file yet". This NOTE records *why* that is a true report and not a gap to
be filled with a manufactured case, and the exact condition under which it
becomes one.

## Why no replay case

The oracle (`ref/`) and the differential backend (`backends/clingo/`)
replay a case by evaluating the **whole** document to its fixpoint and then
checking the pinned expectations — there is no per-rule replay. Three of
this cabinet's rules exercise constructs both executors deliberately refuse
(they fault with context rather than guess — the fail-fast discipline of
`ref/lib/eval.ml` and `backends/clingo/lib/translate.ml`), so any case over
this IR faults before a single expectation can be checked:

| Rule | Construct (WF row) | Fault the reference interpreter raises |
|---|---|---|
| `vakcijfer uit voorjaarsopbrengst` | dimension-cell read of the dimensioned `opbrengst` (WF-004/014/040) | `expression kind not implemented in this card` |
| `peiling bij dubbele borging` | counting quantifier with a time-window (`Periode`) member (WF-041/042) | `condition kind not implemented in this card` |
| `doorwaadbaar bij lage stand #1`/`#2` | whole-period comparison over the temporal `waterstand` (WF-043) | `whole-period comparison is ch. 3 scope` |

The ASP backend mirrors these same refusals by construction
(`backends/clingo/lib/translate.ml`: everything outside the
corpus-exercised subset faults with context), so neither executor can
replay this document.

The two rules that *are* individually executable — `staat na schouw`
(enum-label assignment) and `aanwas na schouw` (sign-restricted literal) —
cannot be replayed on their own: the evaluator runs every `resolvedRegel`,
so the three refusing rules bring the whole replay down first.

A case with zero `Schouwveld` instances does not even evade the faults
cleanly. The reference interpreter is lazy per-instance: with no subject no
rule body runs, so `ref` reports `oracle: PASS`. But the ASP backend
translates every rule body **statically**, independent of instance count, so
`clingo` still faults —
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`
— and gate 6 would turn **red**, not merely pin nothing. Either way the case
exercises and pins nothing real: it is exactly the manufactured "make the
report go away" case this cabinet must not carry. Executable expectations
belong to executing goldens, not to a scaffold.

## Reproduction

From the workspace root, with the ref runner built (`dune build ref`), the
runner on this `ir.json` and any case carrying one `Schouwveld` instance
faults with `expression kind not implemented in this card` — the
dimension-cell read of the first rule. Filtering `resolvedRegels` down to a
single blocking rule in turn (for example with `jq`) reproduces the other
faults in the table above — with one wrinkle for the temporal row:

- `peiling bij dubbele borging` alone faults with
  `condition kind not implemented in this card`, at any `asOfDate`
  (including a null one).
- The two `doorwaadbaar bij lage stand` rules carry validity bounds
  (`#1 tot 2025-12-31`, `#2 vanaf 2026-01-01`), so filtering to them with a
  null `asOfDate` faults *first* with `a validity-bounded form needs an
  evaluation date; this session is time-free` — not the whole-period fault.
  The whole-period-comparison refusal (`whole-period comparison is ch. 3
  scope`) surfaces only once the case also carries an `asOfDate` inside a
  rule's window — for example `#2` with `asOfDate 2026-06-01`.

## When this cabinet shrinks

When one of the refused constructs becomes executable in **both**
executors, its witness moves into an executing golden with a replay case
and its rows leave this cabinet — the README's rule: the cabinet is
"scaffolding for the WF index, never a place for semantics". Until then the
case-less state is the correct one, and gate 5's "NOT replayed" line for
this directory is a true, motivated report — not a coverage hole.
