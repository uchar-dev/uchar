# jutterswet-budgetgrens-over — one creation past the creation budget

The red twin of `corpus/golden/jutterswet-budgetgrens-op`: the same
`bron.rgs` with one literal moved by one. A Wachter makes an opvolger while
its rang is kleiner dan 102, not 101. From W1 with rang 1 the chain makes W2
to W101 and would make W102, with rang 102, as its 101st Wachter: creation
depth 101 and generation 101 in one stratum, one past the creation budget
of `RUNTIME-SEMANTICS.md` ch. 5.5. Because the two documents differ in that
one literal, the pair puts the flip exactly at the bound: 100 creations play,
101 fault. Every executor that carries the budget faults, and none presents
the chain cut off at 100:

- ref: `EVALUATION FAULT: no fixpoint within 100 generations — generation
  101 still mints or links (object creation "werf de lange keten"), and
  divergence is a fault (ch. 5.5, 5.7)`. Pinned: `no fixpoint within`.
- clingo: `UNSATISFIABLE — a fail-fast guard fired: viol("generation budget
  exhausted",…)`, on the term of the 101st Wachter. Pinned: `generation
  budget exhausted`.
- the Drools branch with truth maintenance (`--tms`), which the red run does
  not drive: `geen fixpunt binnen 100 generaties: generatie 101 maakt nog 1
  object of link — een creatie die niet stopt is een fout (RUNTIME-SEMANTICS
  ch. 5.5 en 5.7)`.

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
