# wf091-pro-rata-date-base

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): the
pro-rata part (`timeProportionalPart`, `RUNTIME-SEMANTICS.md` ch. 3.7) over
a base declared `Date`. The part is the base scaled by true days over bucket
days, which is arithmetic, and a total, a sum and a pro-rata part are typed
over a Number only. The target stays numeric; the arm reads the base.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the pro-rata
    part (timeProportionalPart) of 'borgtarief' on 'Jutter' scales Date
    values: a total, sum or pro-rata part is arithmetic over a Number, and
    Date values are calendar positions

**Origin.** Until the card ref-klipvouw-nul this document was the red case
`corpus/red/datumbasis-prorata` (card temporele-datumvouw, fix round), and
it is byte-identical: `golden/anchor/jutterswet-borgdeel` reduced to its
`borgdeel` rule, with the base `borgtarief` (granularity `year`) retyped to
a day-precision `Date`. It was verifier-clean. Measured on it (2026-09-23):
`ref` refused the base (`pro-rata base is not numeric: 2024-01-18`), and
over one whole characteristic month the ASP backend answered `borgdeel` =
**19740.00**, the day number of 2024-01-18, until the same card made it
decline the form by name.

The executors keep their own refusals as a second line, with their
sentences unchanged: `ref` refuses the base, the ASP translation declines a
date-valued base by name ("… Declined in translation"), and neither Drools
branch lowers this temporal assignment in any base type (exec "regel
(temporele toekenning buiten Phase-F scope)", DRL-text "TODO temporele
toekenning").

Red first, measured on this document: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `d2b5ce9992759f07860e95fbecec733e`) the single rejection above.
Mutation, arm-exact: with the pro-rata branch of the arm removed, this
document alone of the six witnesses of the arm passes again; `expect.txt`
pins the arm.
