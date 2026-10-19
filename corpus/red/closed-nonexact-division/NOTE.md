# closed-nonexact-division

The closed-body sibling of `decimal-nonterminating`: a fold body whose
per-element constant-divisor division is **not exact**. `quotientsom` folds
`acc + x / 3` over the list parameter `tellers`; with `tellers = [1]` the single
element is `1 / 3`, which does not terminate.

The divisor-side non-exact fence moves into the closed-body context (S15/S18):
both executors must fault loudly, and the differential holds where it runs —

- **clingo** scales by the declared decimals: the closed-body division derives
  the `viol/2` witness *"non-exact division (an explicit rounding is mandated
  before precision is lost)"* (UNSAT), keyed by the fold's list-id — the
  divisor-side fence firing in body context.
- **ref** stays exact-rational: the fold accumulates 1/3, and the bounded write
  of that 0-decimal `quotientsom` target refuses — *"does not fit the declared 0
  decimals"*.

The case uses no `Let` and a bare `Binary` `dividedBy` by a compile-time
constant, so it exercises only nodes both executors carry.
