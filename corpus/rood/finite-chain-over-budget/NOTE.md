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
  oc0::…::W1 — …`. Pinned: `creation budget exhausted`.
- clingo: `UNSATISFIABLE — a fail-fast guard fired: viol("creation budget
  exhausted",…)`, on the term of the 101st Wachter. Pinned: `creation
  budget exhausted`.

The Drools red run drives the exec-model branch, which has no creation
budget, only a cap of 100 000 firings: it mints all 104 Wachters and
reaches a fixpoint, so `scripts/check-rood.sh` lists the case as NOFAULT
with that reason. The branch with truth maintenance (`--tms`), which the red
run does not drive, faults here with the same fragment.

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
within` (ref) and `generation budget exhausted` (clingo).
