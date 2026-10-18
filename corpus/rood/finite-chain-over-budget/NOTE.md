# finite-chain-over-budget

The budget as a BOUNDARY, not just a divergence guard. Unlike
`runaway-recursion` (a condition that never turns false), this chain is
FINITE: rank < 105 would legitimately terminate after 104 mints. The
creation budget of `RUNTIME-SEMANTICS.md` ch. 5.5 cuts it off first: every
minted Wachter has a creation depth in its stratum, one more than its
subject's, and the mint at depth 101 is a fault in every executor that
carries the budget, never a silent truncation (a partial chain presented as
a result). Both OCaml executors stop at that mint, in the same words:

- ref: `EVALUATION FAULT: creation budget exhausted: object creation "werf
  de lange keten" of stratum 0 would mint at creation depth 101, on
  oc0::…::W1 — …`. Pinned: the sentence up to `creation depth 101`, so a
  fault of another creation or at another depth does not pass.
- clingo: `UNSATISFIABLE — a fail-fast guard fired: viol("creation budget
  exhausted",oc0(oc0(…("W1")…)))`, one witness, on the term of the 101st
  Wachter. Pinned: `a fail-fast guard fired: viol("creation budget
  exhausted",oc0(`, the witness on a mint of the creation and not on an
  instance of the input; the ASP backend's suite checks the witness itself
  on the same chain (`ch5.5 creation budget, a chain of 101 mints`):
  exactly one witness, on the mint at depth 101.

The Drools red run drives the exec-model branch, which since the card
`tms-omschakeling` (2026-10-10) is the branch with truth maintenance. It
faults at the same mint: `creation budget exhausted: objectcreatie "werf de
lange keten" van stratum 0 zou een object maken op aanmaakdiepte 101, voor
een Wachter op diepte 100 — …`, pinned up to `aanmaakdiepte 101` in the
case and whole in `TmsCreationWitnessTest`
(`the_creation_budget_lies_where_the_corpus_pins_it`). Until that card the red
run drove the destructive lowering, which had no creation budget, only a cap
of 100 000 firings: it minted all 104 Wachters and reached a fixpoint, and
`scripts/check-rood.sh` listed the case as NOFAULT with that reason.

The green side of the bound is pinned twice: `corpus/golden/
synth2-budgetgrens-onder` (the same shape bounded at rank < 98, 97 mints,
beside a computing rule in the chain's stratum), and the pair `corpus/golden/
jutterswet-budgetgrens-op` and `corpus/rood/jutterswet-budgetgrens-over`,
one literal apart, which put the flip exactly at 100 → 101 mints.

On this chain every generation mints one Wachter on the last, so the depth
of a Wachter is the generation that minted it, and the bound lies where the
budget this case was written for put it. Until 2026-10-09 this note said
that the flip at 100 → 101 held in both executors "with no split window";
at exactly one generation it did not. Measured on main `8beff67a`, on this
chain without its consistency: 99 mints played in both executors, 100 played
in ref and were UNSAT in clingo (its depth ran on over the strata and its
witness fell at the 100th mint), and 101 faulted in both; with a computing
rule in the chain's stratum ref faulted at 100 as well, since its budget was
then 100 passes. Since the card `creatiebudget-ref-en-clingo` (2026-10-09)
100 mints play and 101 fault in ref, clingo and the truth-maintenance branch,
in both forms. Until that card the fragments pinned here were `no fixpoint
within` (ref) and `generation budget exhausted` (clingo); from its third
step until its fix round the same day both pinned `creation budget
exhausted` only, which a fault on another object or at another depth
satisfied as well.
