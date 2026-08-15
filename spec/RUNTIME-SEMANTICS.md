# uchar IR — Runtime semantics

*Status: unwritten — chapter skeleton only (Phase 1, see `PLAN.md` §6).
Authority today lives in `regelspraak-java`: `runtime/RegelSpraakRuntime.java`
(the single shared implementation both Drools back-ends call) and the decision
records in `docs/grammar-divergences.md`. This document exists to move that
authority into prose plus worked examples; Phase 2 then moves it into the
reference interpreter (`ref/`) — the final arbiter.*

Every chapter must state its semantics as equations and tables plus worked
examples with concrete expected values; the examples become `ref/` unit tests
verbatim.

## Chapters (planned)

1. **Values and emptiness.** The empty-value discipline. OPEN DESIGN QUESTION
   (`PLAN.md` §9, decision 7): "empty = 0" in arithmetic — with the
   verminderd-met left-operand exception — versus empty-as-unknown;
   three-valued consequences; the interaction with closed- versus open-world
   modelling in ASP. The decision and its exception cases are recorded here
   before the behaviour is frozen.
2. **Arithmetic and rounding.** Exact rational intermediates, the §6 operator
   tables, the division variants, precision, and explicit rounding as the
   assignment gate.
3. **Timeline algebra.** Segments, cut-merge, granularity and multi-unit
   periods, variable starting points, pro-rata (tijdsevenredig deel).
4. **Allocation (verdeling).** Equal shares, pro-rata, and ordered group
   depletion as recurrence equations over groups — not "one helper call" —
   including maximum, rounding-floor, and the undistributed remainder.
5. **The evaluation model.** Forward chaining to fixpoint; freshness and
   idempotence of writes; what "a berekening is an equation" means
   operationally.
6. **Stratification.** Negation over derived characteristics requires
   stratification — a well-formedness condition on the IR (with a WF rule),
   not an engine agenda trick.
7. **Defaults.** Initialisation as an explicitly defeasible default; from
   Phase 4: the adopted default calculus (priorities, conflict detection,
   the inconsistency channel).
8. **Time selection.** The single axis (valuation = calculation = vintage
   selection); fail-fast vintage semantics (gap and overlap are errors,
   never silent defaults); the named extension point for a knowledge-time
   axis.
