# backends/clingo — the ASP backend

The second executor: a systematic translator from `regelspraak-ir-v0`
documents plus `uchar-case-v0` cases to ASP-Core-2, run by clingo. With
it the differential is real: gate 6 (`scripts/check-clingo.sh`) demands
that clingo's stable model matches the same absolute expectations gate 5
pins for the reference interpreter — two semantically alien executors,
one answer, over every corpus case. Every divergence is by definition a
spec bug.

## Translation (spec/ASP-TRANSLATION.md's conventions, systematised)

- Instances are constants; a characteristic is `heeft(Inst, "naam")`; an
  attribute value is `val(Inst, "naam", V)`; a role link is
  `rol(Bron, "rolnaam", Doel)`; a parameter is `param("naam", V)`.
  Names are normalised on both sides.
- Exact arithmetic by declared-decimals integer scaling: a value of an
  attribute declared with d decimals is the integer value × 10^d;
  comparisons harmonise scales by multiplication. Clingo's integer
  division on the allocation's non-negative operands is exactly the
  floor the recurrence prescribes.
- Emptiness is absence: an underivable `val/3` atom is leeg, and an
  ordering comparison over leeg fails through the unbound body join —
  the ch. 1.2d ordering rule for free. The equality/inequality empty
  asymmetry has no such free encoding and faults until a corpus case
  demands it (recorded finding).
- Negated characteristic reads are negation-as-failure — sound because
  WF-045/046 guarantee stratifiability; the verifier is thereby also
  the guard that a document is clingo-suitable.
- Aggregation is `#count`/`#sum` with the contributor skip encoded by
  the body join; consistency and uniqueness derive `inconsistent/2`
  facts (the §9.5 channel); dates are civil day numbers; the temporal
  total folds the asOfDate year bucket (factor 1).
- Constructs outside the corpus-exercised subset fault the translator —
  fail fast, never a silent SKIP.

The backend reuses the loader (`uchar-ir-v0`) and, for IO only, the
case loading and value lexing of `uchar-ref`; nothing of the reference
evaluator's semantics is linked — the semantics run in clingo.

## Run

    dune build backends
    _build/default/backends/clingo/bin/clingo_runner.exe <ir.json> <case.json>

Exit 0: every expectation matches; 1: mismatch or translation fault;
2: load error or clingo unavailable.
