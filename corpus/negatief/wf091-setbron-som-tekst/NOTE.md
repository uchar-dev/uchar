# wf091-setbron-som-tekst

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): a
`sum` over a set source whose element attribute is declared `Text`. The
arm refuses every operand declaration that is not a Number, and this case
pins the message for the types other than `Date`, whose tail differs.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of
    'vindplaats' on 'Vondst' folds Text values: a total, sum or pro-rata
    part is arithmetic over a Number, and Text values are not amounts

**The document.** `wf091-setbron-som-datum` with the member and the target
retyped to `Text` and renamed (`vondstdatum` to `vindplaats`,
`vondstdagensom` to `vindplaatsensom`); nothing else differs. The card
datum-setbron-breuk measured this class (2026-09-20): an additive fold over
a text source was verifier-clean, because WF-038 refuses a text operand
under an ORDERING fold only. Measured on this document (2026-09-25), with
two members that both carry `strand`: `ref` faults on the addition (`plus:
expected a number, got "strand"`), the ASP translation refuses with its
generic text/enum/boolean sentence, and both Drools branches refuse one
phase before fire through `Vouwwaarde.weigering` ("… die leden zijn van type
Tekst en niet getalachtig …"). No executor answered; the arm makes the
document ill-formed, as the `Date` witnesses are.

Red first: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `d2b5ce9992759f07860e95fbecec733e`) the single rejection above.
Mutation: with the arm's type test switched off (`when false && …`), or
with `sum` removed from the fold branch, this document passes again and
gate 3 names it. With the non-`Date` tail replaced by the `Date` one the
document stays rejected, and gate 3 fails on this directory alone:
`expect.txt` pins the tail.
