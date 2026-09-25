Violates WF-035 (`spec/IR-SPEC.md`, the date ± duration clause): the
duration operand of a date plus is `3 vadem`, and `vadem` is neither a
recognised time unit nor a declared day kind (the document declares none).

    WF-035 FAIL /rules/rules/0/versions/0/value/right: date ± duration: the
    duration unit 'vadem' is neither a recognised time unit nor a declared
    day kind

A minimal hand-broken variant of
`corpus/golden/jutterswet-vervaltermijn/ir.json`, cut from
`corpus/negatief/wf035-uur-op-dagdatum`. The sentence is spelled once,
`date_duration_unknown_unit` in `spec/verifier/lib/checks.ml`; `ref` and the
ASP backend speak it too (measured 2026-09-25 on this document). The Drools
loader refuses the document at load in the same sentence, at the same
pointer (measured 2026-09-25). `expect.txt` pins the arm.
