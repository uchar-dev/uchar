# wf091-tijdlijntotaal-datum

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, "Derived-type identity on
composite positions", the numeric-operand arm): the subject's own temporal
`total` (`RUNTIME-SEMANTICS.md` ch. 3.6) over a timeline declared `Date`.
The temporal total multiplies each segment value by the whole grid units it
spans. That is arithmetic, and a total, a sum and a pro-rata part are typed
over a Number only.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the total of
    'bewaarvergoeding per maand' on 'Jutter' folds Date values: a total, sum
    or pro-rata part is arithmetic over a Number, and Date values are
    calendar positions

**Origin.** Until the card ref-klipvouw-nul this document was the red case
`corpus/rood/datumtijdlijn-totaalvouw` (card temporele-datumvouw), and it is
byte-identical: `golden/jutterswet-tempototaal` reduced to its third rule,
with the totalled attribute (granularity `month`) and the target retyped to
a day-precision `Date`. It was verifier-clean because the `total` row of the
derived-type identity passes operand 0's type through, so a `Date`
annotation agreed with a `Date` operand. Measured on it (2026-09-23): `ref`
faulted on the multiplication (`maal: expected a number, got 2024-01-15`),
and the ASP backend answered **19737**, the day number of 2024-01-15, until
the card temporele-datumvouw made it decline the form by name. The case file
did not move with the document: the verifier rejects the document, so no
executor replays it.

The executors keep their own refusals as a second line, with their
sentences unchanged: `ref` faults on the multiplication, the ASP translation
declines a date-valued timeline under a total by name ("… Declined in
translation"), and both Drools branches refuse the document through
`Vouwwaarde.tijdlijnWeigering` — the DRL-text branch as a marker, the exec
branch as a transpiler skip since the card ref-klipvouw-nul (until then its
case loader refused the date-valued segment, and a case without a value
played leeg).

Red first, measured on this document: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `4aa9ae86163a239aa72a7d7d4f54f678`) the single rejection above.
Mutation: with the arm's type test switched off (`when false && …`) all
five witnesses of the arm pass the verifier again and gate 3 names each of
them; `expect.txt` pins the arm.
