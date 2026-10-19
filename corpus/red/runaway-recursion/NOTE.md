# runaway-recursion

The ch. 5.5 budget fault: a recursive creation whose condition never
turns false (rank ≥ 1) is WF-033-conditional and verifier-clean, but
cannot terminate. The creation budget makes divergence a **fault, never
a silent truncation**: every minted Wachter has a creation depth in its
stratum, one more than its subject's, and the mint at depth 101 is the
fault. The reference interpreter faults there with "creation budget
exhausted: object creation "werf eeuwig" of stratum 0 would mint at
creation depth 101, …", and the ASP backend derives the "creation budget
exhausted" `viol/2` witness on the same mint — UNSAT. The same
author-error, the same loud stop, in both executors, in the same words.
Pinned for ref: the sentence up to `creation depth 101`; for clingo: `a
fail-fast guard fired: viol("creation budget exhausted",oc0(`, the witness
on a mint of the creation and not on an instance of the input. A fragment
cannot carry the term of 101 levels whole, so the ASP backend's suite
checks the witness itself on a chain of the same form (`ch5.5 creation
budget, a chain of 101 mints`): exactly one witness, on the mint at depth
101.

The Drools red run drives the exec-model branch, which derives with truth
maintenance since the card `tms-omschakeling` (2026-10-10). It faults at
the mint at depth 101, like the two OCaml executors: `creation budget
exhausted: objectcreatie "werf eeuwig" van stratum 0 zou een object maken
op aanmaakdiepte 101, voor een Wachter op diepte 100 — …`, pinned up to
`aanmaakdiepte 101` in the case and whole in `TmsCreationWitnessTest`
(`the_creation_budget_lies_where_the_corpus_pins_it`). Until that card the
red run drove the destructive lowering, which had no creation budget: it
minted until its cap of 100 000 firings and reported "geen fixpunt
(kringverwijzing?)", the fragment pinned for it then.

Until 2026-10-09 (card `creatiebudget-ref-en-clingo`) the pinned
fragments were `no fixpoint within` (ref, which counted 100 passes and
then 100 generations per stratum) and `generation budget exhausted`
(clingo, the depth of a chain of mints); from the card's third step until
its fix round the same day both pinned `creation budget exhausted` only,
which a fault on another object or at another depth satisfied as well.
