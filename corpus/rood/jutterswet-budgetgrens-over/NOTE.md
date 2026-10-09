# jutterswet-budgetgrens-over — one creation past the creation budget

The red twin of `corpus/golden/jutterswet-budgetgrens-op`: the same
`bron.rgs` with one literal moved by one. A Wachter makes an opvolger while
its rang is kleiner dan 102, not 101. From W1 with rang 1 the chain makes W2
to W101 and would make W102, with rang 102, as its 101st Wachter: creation
depth 101 in one stratum, one past the creation budget of
`RUNTIME-SEMANTICS.md` ch. 5.5 (on this chain it is generation 101 as well).
Because the two documents differ in that one literal, the pair puts the flip
exactly at the bound: 100 creations play, 101 fault. Every executor that
carries the budget faults, in the same words, and none presents the chain
cut off at 100:

- ref: `EVALUATION FAULT: creation budget exhausted: object creation "werf
  de lange keten" of stratum 0 would mint at creation depth 101, on
  oc0::…::W1 — …`. Pinned: `creation budget exhausted`.
- clingo: `UNSATISFIABLE — a fail-fast guard fired: viol("creation budget
  exhausted",…)`, on the term of the 101st Wachter. Pinned: `creation
  budget exhausted`.
- the Drools branch with truth maintenance (`--tms`), which the red run does
  not drive: `creation budget exhausted: objectcreatie "werf de lange keten"
  van stratum 0 zou een object maken op aanmaakdiepte 101, voor een Wachter
  op diepte 100 — …`.

Until the card's third step (2026-10-09) the fragments were `no fixpoint
within` (ref, which counted 100 generations per stratum that mint or link)
and `generation budget exhausted` (clingo).

The Drools red run drives the exec-model branch. That branch and the DRL-text
branch have no creation budget, only a cap of 100 000 firings: they make all
101 Wachters and reach a fixpoint, so the red run reports `NOFAULT ran to a
model without faulting`, and `scripts/check-rood.sh` lists the case with that
reason, as it lists `finite-chain-over-budget` (measured 2026-10-09).

The case keeps the golden's rule and consistency; the fault comes before
either is observed. On the code before the cards
`creatie-initialisatie-leest-groeiende-waarde` and
`creatiebudget-ref-en-clingo` both OCaml executors faulted here as well (main
`8beff67a`: ref `no fixpoint within 100 passes`, clingo `generation budget
exhausted`); what moved is the golden side of the pair, which then faulted
too.

`bron.rgs` sits beside `ir.json`, which the RegelSpraak frontend emitted from
it (2026-10-09). Gate 10 re-emits the sources under `corpus/golden` only, so
no gate holds this `ir.json` to its source; the two files differ from the
golden's pair in the one literal and the snippets that carry it.
