Violates WF-035 (`spec/IR-SPEC.md`, the date ± duration clause): the
duration operand of a date plus is the unitless number `3`; the duration
operand carries a time unit or a declared day kind.

    WF-035 FAIL /rules/rules/0/versions/0/value/right: date ± duration: the
    duration operand carries no time unit

A minimal hand-broken variant of `corpus/golden/jutterswet-vervaltermijn/ir.json`,
cut from `corpus/negatief/wf035-uur-op-dagdatum`. The sentence is spelled
once, `date_duration_without_time_unit` in `spec/verifier/lib/checks.ml`;
`ref` and the ASP backend speak it too (measured 2026-09-25 on this
document). `expect.txt` pins the arm.
