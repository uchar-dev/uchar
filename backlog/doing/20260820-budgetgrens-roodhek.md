# The budget-boundary red fence: a finite chain over the budget fails loud on both sides

Goal: the ch. 5.5/5.7 evaluation budget (100 — passes in ref,
generations per chain in clingo, deliberately equal) is pinned as a
BOUNDARY, not just as a divergence guard. The existing
`corpus/rood/runaway-recursion` pins the infinite chain (a condition
that never turns false); this card pins the subtler edge: a FINITE
recursive creation chain that would legitimately terminate at ~104
generations is cut off by the budget — both executors must stop
identically loud (ref "no fixpoint within", clingo "generation budget
exhausted"), never a silent truncation and never a split where one
side happens to finish. The green counterpart proves the fence sits AT
the boundary: the same document shape with the bound just under the
budget (a 97-generation chain) replays exactly in both executors, its
consistency assertion (every rank < 99) witnessing that the chain
terminated by its own condition, not by the budget.

Acceptance: all eight gates green; the red case fails observably in
both executors with the pinned fragments (gate 8); the green boundary
golden replays differentially (gates 5–6) through `scripts/admit.sh`.

May touch: `corpus/rood/finite-chain-over-budget/`,
`corpus/golden/synth2-*/` (via admit.sh), `backlog/`.

Out of scope: changing the budget value or making it configurable (a
design decision — the 100 is v0's pass bound, mirrored); the
Phase-4 defeasibility work; any executor change (this card is corpus
only — if the executors disagreed on either side of the boundary that
would be a find for a hacker-fixer round, not this card).
