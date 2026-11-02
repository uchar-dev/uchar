# wf023-closed-nonexact-division

Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1), the precision gate of RUNTIME-SEMANTICS ch. 2.4: a quotient by the literal 3 inside the body of a fold, written into a whole target.

    WF-023.precision-unbounded FAIL /rules/rules/0/versions/0/value

At run time the executors fault on this write with EF-006, the evaluation fault that stands behind the gate (ch. 2.4 point 1); what follows describes the document and that runtime fault.

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
