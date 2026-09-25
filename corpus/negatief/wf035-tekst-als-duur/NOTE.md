Violates WF-035 (`spec/IR-SPEC.md`, the date ± duration clause): the
duration operand of a date plus is the text literal `'drie weken'`; the
duration operand is a number carrying a time unit.

    WF-035 FAIL /rules/rules/0/versions/0/value/right: date ± duration: the
    duration operand is text, must be a number carrying a time unit

A minimal hand-broken variant of `corpus/golden/jutterswet-vervaltermijn/ir.json`,
cut from `corpus/negatief/wf035-uur-op-dagdatum`. The sentence is spelled
once, `date_duration_operand_refusal` in `spec/verifier/lib/checks.ml`, and
`ref` and the ASP backend read it off the operand's static type ahead of its
unit when a document reaches them without the verifier — measured 2026-09-25
on this document: `EVALUATION FAULT: date ± duration: the duration operand
is text, must be a number carrying a time unit` and `TRANSLATION FAULT:`
with the same sentence. Until then both reported the text operand as a
number without a unit: `date ± duration: the duration operand carries no
time unit`. `expect.txt` pins the arm.
