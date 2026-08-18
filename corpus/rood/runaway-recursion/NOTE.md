# runaway-recursion

The ch. 5.5 budget fault: a recursive creation whose condition never
turns false (rank ≥ 1) is WF-033-conditional and verifier-clean, but
cannot terminate. The bounded budget makes divergence a **fault, never
a silent truncation**: the reference interpreter exhausts its pass
budget ("no fixpoint within … passes"), and the ASP backend's
generation depth reaches `oc_budget` (the same bound: 100 — passes in
ref, generations per chain in clingo, aligned because one chain
advances one generation per pass), deriving the
"generation budget exhausted" `viol/2` witness — UNSAT. The same
author-error, the same loud stop, in both executors.
