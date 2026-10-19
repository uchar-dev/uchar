# wf091-set-source-sum-date

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): a
`sum` over a set source whose element attribute is declared `Date`. Adding
two calendar positions gives no position, and a total, a sum and a pro-rata
part are typed over a Number only.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of
    'vondstdatum' on 'Vondst' folds Date values: a total, sum or pro-rata
    part is arithmetic over a Number, and Date values are calendar positions

**Origin.** Until the card ref-klipvouw-nul this document was the red case
`corpus/red/datumsetbron-somvouw` (card datum-setbron-breuk), and it is
byte-identical: `golden/jutterswet-vondstvenster` with its `first` fold
turned into a `sum`, the member and the target `Date` as before. It was
verifier-clean because the `sum` row of the derived-type identity passes
operand 0's type through. Measured on it (2026-09-19, again 2026-09-20):
with two members `ref` faulted on the addition (`plus: expected a number,
got 2024-01-15`), and the ASP backend answered **39520**, the sum of the day
numbers 19737 and 19783, until that card made it decline the form by name.
The two members carry the same date so that the `ref` fragment did not
depend on the order in which the addition read its operands.

The ordering folds over the same source stay well-formed and carried:
`golden/jutterswet-vondstbereik` pins `minimum`/`maximum` and
`golden/jutterswet-vondstvenster` pins `first`/`last` over these dates in
`ref` and clingo. The arm reads `sum`, `total` and the pro-rata part only.

The executors keep their own refusals as a second line, with their
sentences unchanged: `ref` faults on the addition, the ASP translation
declines a date-valued set source under an additive fold by name ("…
Declined in translation"), and both Drools branches refuse the form one
phase before fire through `Vouwwaarde.weigering`.

Red first, measured on this document: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `d2b5ce9992759f07860e95fbecec733e`) the single rejection above.
Mutation, arm-exact: with `sum` removed from the fold branch of the arm,
this document and `wf091-set-source-sum-text` — the two `sum` witnesses of the
six — pass again; `expect.txt` pins the arm.
