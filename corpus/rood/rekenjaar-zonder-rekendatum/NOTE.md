# rekenjaar-zonder-rekendatum

The ch. 8.2 execution year without an evaluation date. `evaluationYear` is
the calendar year of the session's evaluation date (RUNTIME-SEMANTICS
ch. 8.2): it is derived from the one date a session carries and never
supplied beside it. A case without `asOfDate` therefore has no year to give,
and every executor faults instead of reading leeg.

## The shape

`ir.json` is `golden/jutterswet-rekenjaar` byte for byte: three rules that
read `evaluationYear` as a rule value, as the left operand of a
subtraction and as a comparison operand on either side. The case carries
one `Vondst` (V1, vondstjaar 2026) and `asOfDate: null`; only the case is
red.

## What each executor says (measured 2026-09-26)

All three give the same sentence:

- `ref`: `EVALUATION FAULT: the evaluationYear context value needs an
  evaluation date; the case carries asOfDate null`;
- the ASP backend: `TRANSLATION FAULT: the evaluationYear context value
  needs an evaluation date; the case carries asOfDate null`;
- the Drools backend, at fire: `FAULT ConstraintEvaluationException: …
  <- IllegalStateException: the evaluationYear context value needs an
  evaluation date; the case carries asOfDate null` — the fragment pinned
  is the runtime's own sentence (`RegelSpraakRuntime`), never the platform
  text around it.

## Falsifiable

Measured 2026-09-26 with each executor made to read a fixed year where the
date is missing: `ref` then reports `EXPECTED FAULT DID NOT OCCUR:
evaluation succeeded`, the ASP runner `EXPECTED FAULT DID NOT OCCUR: clingo
found a model`, and the Drools red run `NOFAULT ran to a model without
faulting`, which gate 8's Drools leg refuses for a case that pins a
`drools` fragment.

## The sister context value

`evaluationDate` without a date faults in `ref` and the ASP backend in the
same form ("the evaluationDate context value needs an evaluation date; the
case carries asOfDate null"), but the Drools replay reads it as leeg and
plays on. That split is recorded, not pinned here: it is the sister's
existing behaviour, and a red case for it would need the Drools leg listed
as NOFAULT.
