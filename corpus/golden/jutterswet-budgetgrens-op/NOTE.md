# jutterswet-budgetgrens-op — a chain of creations exactly at the creation budget

The creation budget of `RUNTIME-SEMANTICS.md` ch. 5.5 at its bound: a
recursive creation that makes 100 Wachters in one evaluation stratum, each
from the one made before it, plays in every executor. The 100th Wachter is
made at creation depth 100 and in generation 100; on a chain like this one,
where every creation waits only for its subject, the two counts are the same
number. The red twin `corpus/rood/jutterswet-budgetgrens-over` is this
document with one literal moved by one: its chain would make a 101st
Wachter, and every executor that carries the budget faults there.
`synth2-budgetgrens-onder` (97 Wachters made) stays as the green side
further down; `corpus/rood/finite-chain-over-budget` (104) and
`corpus/rood/runaway-recursion` (no end) are the red side further up.

## Source (`bron.rgs`)

- `Feittype opvolging`: a voorganger has one opvolger, both Wachters.
- `Regelgroep lange estafette is recursief` over the one creation `werf de
  lange keten`: a Wachter whose rang is kleiner dan 101 gets an opvolger with
  rang its rang plus 1 and the ketennummer of the Wachter, over
  `connectingRole {opvolging, opvolger}`. This is the form of
  `corpus/rood/finite-chain-over-budget`, with the bound at 101 instead of
  105; the ketennummer only carries the number of the chain to its end.
- Rule `opvolgerrang van een wachter`: the opvolgerrang of a Wachter is the
  rang of its opvolger.
- Rule `rangplafond`: `De rang van een Wachter moet kleiner zijn dan 101.`, a
  consistency rule.
- Rules `eerste keten aan het plafond` and `tweede keten aan het plafond`:
  consistency rules that a Wachter meets when its rang is kleiner dan 101 or
  its ketennummer is ongelijk aan 1 (the second: aan 2). Each flags exactly
  the Wachter of rang 101 on its own chain, so each case can pin that every
  one of its chains reached its end, where `rangplafond` gives one channel
  fact for any chain.

The group comes last in the source because a `Regelgroep` takes every
consecutive rule after its header (as in `jutterswet-wachtersketen-rgs`).

## The strata

The creation reads the rang and the population of Wachter, which it writes
itself: the cycle that WF-045 admits through the recursive group. The rule
reads the rang and the relation `opvolging` over positive edges, so it shares
stratum 0 with the creation; the three consistencies read the rang (and the
ketennummer) stratum-strictly and sit in stratum 1 (`verifier
--dependency-edges`). After the 100th
Wachter is made, the rule therefore still gives the Wachter before it an
opvolgerrang in stratum 0: a round in which only a rule changes something,
after the last creation. Until 2026-10-09 that round counted against the
budget in the reference interpreter (below).

## Hand computation

`case1-een-keten.json`: W1 with rang 1 and ketennummer 1. W1 (1 < 101)
makes W2 with rang 2, W2 makes W3 with rang 3, and so on; W100 (rang 100)
makes W101 with rang 101, the 100th Wachter made, and every one of them has
ketennummer 1. W101 makes nothing, since 101 is not kleiner dan 101, and
the stratum is done: 101 Wachters, 100 of them made.

- opvolgerrang(W1) = the rang of W2 = **2**.
- `rangplafond` and `eerste keten aan het plafond` hold for every Wachter but
  W101 (rang 101, ketennummer 1): the case is **inconsistent**, on both
  rules. This is the witness that the 100th Wachter exists; a chain that
  stopped one creation earlier, at W100 with rang 100, leaves the case
  consistent. `tweede keten aan het plafond` flags nothing, since no Wachter
  has ketennummer 2 (the case format pins no absent channel fact).

`case2-twee-ketens.json`: W1 with rang 1 and ketennummer 1, W2 with rang 1
and ketennummer 2. Each starts a chain of its own, and the two chains grow
side by side in the same stratum, each Wachter with the ketennummer of the
one it was made from: 200 Wachters made, 100 on each chain, each chain 100
deep. opvolgerrang(W1) = **2**, opvolgerrang(W2) = **2**, and the case is
**inconsistent** on `rangplafond` (both Wachters of rang 101), on `eerste
keten aan het plafond` (the Wachter of rang 101 on W1's chain) and on
`tweede keten aan het plafond` (the one on W2's chain). The rang goes up by
one per Wachter made, from 1, so a chain reaches rang 101 only with its
100th Wachter, and the two rules pin that both chains make all 100: 200
Wachters in one stratum. The budget does not count the objects a stratum
makes.

Until the card's fix round (2026-10-09) the document had neither the
ketennummer nor the two rules per chain, and case 2 pinned only `rangplafond`,
which one channel fact satisfies: a run in which W2's chain stopped after its
first Wachter passed it as well (lens finding). Measured on a variant of this
document whose creation condition is "de rang van de Wachter plus 99 maal het
ketennummer van de Wachter kleiner is dan 200", so that W1's chain runs to
rang 101 and W2's stops at rang 2: the old expectations of case 2 PASS in ref
and clingo, and the new ones fail in both on `tweede keten aan het plafond`.

Measured beside the hand computation: with the three consistencies at kleiner
dan 102 instead of 101 both cases are consistent in ref and clingo, so no
Wachter reaches rang 102.

## Red before the cards

Until 2026-10-09 no OCaml executor played this document:

- ref (main `8beff67a`): `EVALUATION FAULT: no fixpoint within 100 passes —
  divergence is a fault (ch. 5.7)`, in both cases. Its budget counted passes,
  and the round after the last creation, in which only the rule changes
  something, was a 101st pass.
- clingo (main `8beff67a`, and `e15baf01`, after the reference
  interpreter's change and before the ASP backend's): `UNSATISFIABLE — a
  fail-fast guard fired: viol("generation budget exhausted",…)` in both
  cases. Its depth reached the bound at the 100th Wachter of a chain.
- the Drools branch with truth maintenance (`--tms`, before the card
  `creatiebudget-ref-en-clingo`): `geen fixpunt binnen 100 generaties:
  generatie 101 maakt niets meer, maar de regellaag verandert nog 1 waarde,
  kenmerk of link na wat generatie 100 maakte …` in case 1, and `nog 2
  waarden` in case 2.

Re-measured on the document of the fix round, with the ketennummer and the
two rules per chain: the same sentences, in both cases, from ref and clingo
on `8beff67a`, clingo on `e15baf01` (where ref plays both) and the branch
with truth maintenance on the card's base.

On main `8beff67a` the bound lay one creation lower for a chain with a rule
in its stratum (this document, and `synth2-budgetgrens-onder`'s form with
the bound moved up): 99 creations played in both executors, 100 faulted in
both. Without such a rule (`corpus/rood/finite-chain-over-budget`'s form)
ref played 100 creations and clingo faulted on them. ref plays this document
since the card `creatie-initialisatie-leest-groeiende-waarde`, clingo and the
branch with truth maintenance since the card `creatiebudget-ref-en-clingo`.

## Executors

ref and clingo replay both cases with the pinned values (clingo in about 0.2
and 0.4 s). The Drools backend plays both in all three branches, PLAYED 2
each (measured 2026-10-09, and again on the document of the fix round): the exec-model branch and the DRL-text branch,
which have no creation budget and only a cap of 100 000 firings, and the
branch with truth maintenance. Neither case survives a no-op run: the
opvolgerrang is derived, and the channel is empty without evaluation.

The rule reads the rang of the opvolger, where `synth2-budgetgrens-onder`
sums the rang of all opvolgers with 0 as the fallback. With the sum this
document takes about 23 s in the ASP backend for one chain and 46 s for two,
and synth2 about 22 s (measured 2026-10-09, on this card's code and on the
code before it alike), against 0.2 and 0.5 s with the read; gates 6 and 13
replay every golden through the ASP backend on every run.

## Not in the corpus: a chain over three strata

A chain of 50 creations, one creation in the stratum above it, and 50
creations in the stratum above that, plays in every executor: the budget is
per stratum. It is pinned in the ASP backend's suite (the frame `ch5.5
creation budget, a chain over three strata (50 + 1 + 50)`, beside the
reference interpreter) and in `TmsCreatiestratumWitnessTest`
(`een_keten_over_drie_strata_telt_per_stratum`), not here: the ASP grounding
of that document takes about 23 s and about 1.7 GB of rules (measured
2026-10-09).
