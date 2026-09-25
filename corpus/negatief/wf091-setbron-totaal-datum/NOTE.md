# wf091-setbron-totaal-datum

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): a
`total` over a set source whose element attribute is declared `Date` — the
set fold over a plain, non-temporal attribute, an alias of the sum
(§4.20). Adding members is arithmetic, and a total, a sum and a pro-rata
part are typed over a Number only.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the total of
    'vondstdatum' on 'Vondst' folds Date values: a total, sum or pro-rata
    part is arithmetic over a Number, and Date values are calendar positions

**Origin.** Until the card ref-klipvouw-nul this document was the red case
`corpus/rood/datumsetbron-totaalvouw` (card datum-setbron-breuk), and it is
byte-identical: the `total` twin of `wf091-setbron-som-datum`, both built
from `golden/jutterswet-vondstvenster` with its `first` fold turned into an
additive one. It was verifier-clean because the `total` row of the
derived-type identity passes operand 0's type through on the set reading.
Measured on it (2026-09-19, again 2026-09-20): with two members `ref`
faulted on the addition (`plus: expected a number, got 2024-01-15`), and the
ASP backend answered **39520**, the sum of the day numbers 19737 and 19783,
until that card made it decline the form by name.

Below two filled members `ref` performed no addition and answered — the one
member itself, a date, at one member, and at none the `total`'s
unconditional 0 (§4.20), a number in a date slot (measured 2026-09-20). The
case reader could not write that 0 down as an expectation at all (`value
variant "number" does not match the declared datatype`). With the arm, no
verifier-clean document reaches those answers.

The executors keep their own refusals as a second line, with their
sentences unchanged: `ref` faults on the addition, the ASP translation
declines a date-valued set source under an additive fold by name ("…
Declined in translation"), and both Drools branches refuse the form one
phase before fire through `Vouwwaarde.weigering`.

Red first, measured on this document: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `d2b5ce9992759f07860e95fbecec733e`) the single rejection above.
Mutation: with the arm's type test switched off (`when false && …`), or
with `total` removed from the fold branch, this document passes again and
gate 3 names it; `expect.txt` pins the arm.
