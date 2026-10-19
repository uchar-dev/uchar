# closed-runtime-division-by-zero

The runtime-divisor sibling of `division-by-numeric-zero`, in a closed fold
body (S15/S18, form 5). `delertotaal` folds `acc + (grondslag / x)` over the
list parameter `kindertallen`; with `kindertallen = [2, 0]` the second
element is a numeric-zero divisor.

A closed-body binder is never leeg, so the empty-divisor §9.5 edge cannot
arise; the numeric zero can. Since 2026-10-19 (card `zero-division-channel`,
`RUNTIME-SEMANTICS.md` ch. 1.4 point 4 and ch. 10.1) its quotient is leeg
with a §9.5 deposit of kind EF-005 on the rule and J1, and `plus` reads the
leeg as 0 (ch. 1.2): the fold gives 0,00 + 30 + 0 = 30,00. The case pins
`delertotaal` 30 and the fact.

- **ref** deposits in `Arith.div_gen`, whose `err` is the rule's.
- **clingo** carries the leeg because it stands under a `plus`: the
  closed-body division gives 0 at a zero divisor and the deposit joins the
  reach of the subject that reads the fold. A zero divisor in a closed body
  where the leeg would propagate is a named limit of the backend
  (`backends/clingo/README.md` [V4]).
- **Drools** does not carry a list datatype (`EXCLUDE DROOLS-008`, gate 12's
  `EXPECTED_CHANNEL`).

Until 2026-10-19 this was a red case: both OCaml executors faulted,
*"division by numeric zero"*.
