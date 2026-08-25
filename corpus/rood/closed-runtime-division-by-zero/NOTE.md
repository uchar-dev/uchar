# closed-runtime-division-by-zero

The runtime-divisor sibling of `division-by-numeric-zero`, in a closed fold body
(S15/S18, form 5). `delertotaal` folds `acc + (grondslag / x)` over the list
parameter `kindertallen`; with `kindertallen = [2, 0]` the second element is a
numeric-zero divisor.

A closed-body binder is never leeg, so the empty-divisor §9.5 edge cannot arise;
only the numeric-zero fault does, and it must fire loudly in **both** executors —

- **clingo** derives the `viol/2` witness *"division by numeric zero"* (UNSAT),
  keyed by the fold's list-id — the divisor-side zero fence in body context.
- **ref** faults hard in `Arith.div_gen` — *"division by numeric zero"*.

The case uses a bare `Binair GEDEELD_DOOR` by a bound list element (the runtime
divisor), exercising only nodes both executors carry.
