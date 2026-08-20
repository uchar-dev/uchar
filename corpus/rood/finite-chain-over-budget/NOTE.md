# finite-chain-over-budget

The budget as a BOUNDARY, not just a divergence guard. Unlike
`runaway-recursion` (a condition that never turns false), this chain is
FINITE: rank < 105 would legitimately terminate after 104 generations.
The ch. 5.5/5.7 evaluation budget (100 — passes in the reference
interpreter, generations per chain in the ASP backend, deliberately the
same number because one chain advances one generation per pass) cuts it
off first, and both executors MUST stop identically loud: ref exhausts
its pass budget ("no fixpoint within"), clingo's generation depth
reaches `oc_budget` and derives the "generation budget exhausted"
`viol/2` witness — UNSAT. A silent truncation (a partial chain
presented as a result), or one executor finishing where the other
faults, would each be a spec violation. The green side of the boundary
is pinned by `corpus/golden/synth2-budgetgrens-onder`: the same
document shape bounded at rank < 98 (97 generations) replays exactly
in both executors, its consistency assertion (every rank < 99)
witnessing termination by the chain's own condition. The exact flip
sits at 100 → 101 generations, verified in BOTH executors with no
split window between the pinned points; the green pin sits two
generations under the largest green chain as a deliberate robustness
buffer against future evaluation-pass drift.
