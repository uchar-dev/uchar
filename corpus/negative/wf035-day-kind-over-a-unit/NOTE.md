Violates WF-035 (`spec/IR-SPEC.md`, the date ± duration clause): the
duration operand of a date plus is `3 werkdag/m`. The document declares the
day kind `werkdag`, but a day kind names a declared unit, and `werkdag/m` is
two factors. The sentence names the whole unit:

    WF-035 FAIL /rules/rules/0/versions/0/value/right: date ± duration: the
    duration unit 'werkdag/m' is neither a recognised time unit nor a declared
    day kind

A variant of `corpus/negative/wf035-unknown-unit`. The Drools loader refuses
the document at load in the same sentence, at the same pointer
(`TypeSystemRefusalWitnessTest.a_day_kind_over_a_unit_is_refused_at_load`).
`expect.txt` pins the arm.
