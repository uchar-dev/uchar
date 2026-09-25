# closed-runtime-nonexact-division

The runtime-divisor sibling of `closed-nonexact-division`, exercising the
**two-factor** exactness fence in a closed fold body (S15/S18, form 5).
`delertotaal` folds `acc + (grondslag / x)`; with `kindertallen = [7]` the single
element is `60 / 7`, which does not terminate at the 2-decimal target scale.

A closed-body binder is never leeg (no §9.5 empty-divisor edge); the value is
guarded EXACT and a non-exact per-element quotient must fault loudly in **both**
executors — the differential diverges in *expressiveness*, never in *value* —

- **clingo** derives the `viol/2` witness *"non-exact division (an explicit
  rounding is mandated before precision is lost)"* (UNSAT), keyed by the fold's
  list-id — the two-factor divisor fence firing in body context.
- **ref** stays exact-rational: the fold accumulates `60/7`, and the bounded
  write of that 2-decimal `delertotaal` target refuses — *"does not fit the
  declared 2 decimals"*.

The case uses a bare `Binary` `dividedBy` by a bound list element (the runtime
divisor), exercising only nodes both executors carry.
