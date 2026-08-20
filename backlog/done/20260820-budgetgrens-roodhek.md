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
side happens to finish. The green counterpart brackets the fence
from below: the same document shape with a 97-generation chain (a
deliberate two-generation robustness buffer under the largest green
chain) replays exactly in both executors, its consistency assertion
(every rank < 99) witnessing that the chain terminated by its own
condition, not by the budget.

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

Result: the budget boundary is fenced from both sides. The red case
(corpus/rood/finite-chain-over-budget): a FINITE 104-generation chain
(rank < 105) that would legitimately terminate is cut off by the
ch. 5.5/5.7 budget and fails identically loud in both executors —
ref "no fixpoint within", clingo's "generation budget exhausted"
viol/2 (both fragments unique in their codebases, lens-verified). This
pins a class runaway-recursion does not: an executor with genuine
non-termination detection would still fault the runaway but would
COMPLETE the finite chain and violate this red expectation — the
"smart" executor is excluded. The green side
(corpus/golden/synth2-budgetgrens-onder, via admit.sh, re-admitted
once to fix a stale copy-paste bron text the lens caught): a
97-generation chain replays exactly, its consistency assertion (every
rank < 99) witnessing termination by the chain's own condition. The
verify round swept the whole window: both executors flip at exactly
100 → 101 generations with NO split window on any intermediate value —
the two budget accountings (passes vs generations) provably coincide.
The green pin sits two generations under the largest green chain as a
deliberate robustness buffer; the first "AT the boundary" wording
overstated that and is corrected. Recorded costs and cosmetics: the
green case is the corpus's most expensive clingo grounding (~9 s
local, ~36% of gate 6 — acceptable, watch under CI); the red chain's
inherited "eeuwig" naming contradicted its finiteness and is renamed.
Corpus: 93 running cases, 79 negatiefs, 7 red; ref ≡ clingo
everywhere.
