Violates WF-038 (`spec/IR-SPEC.md` ch. 4.3, the `duration` clause): the
counted unit of a `duration` lives on the node's type and MUST be a
recognised time unit or a declared day kind; here it is `vadem`. Broken from
`corpus/golden/anchor/jutterswet-jutleeftijd`: the rule `Berekening
jutleeftijd` counts in `vadem` instead of `jr`, and the target attribute
`Jutter.jutleeftijd` is declared in `vadem` as well, so that no other rule
(WF-023's unit clause) reports the pair.

    WF-038 FAIL /rules/rules/0/versions/0/value: duration: the counted unit
    'vadem' is neither a recognised time unit nor a declared day kind

The sentence is spelled once, `duration_unknown_unit` in
`spec/verifier/lib/checks.ml`, and `ref` and the ASP backend speak it too
when a document reaches them without the verifier — measured 2026-09-25 on
this form over day-precision operands (this document) and over datum-tijd
operands (`jutterswet-getijdenpaar`): `EVALUATION FAULT: duration: the
counted unit 'vadem' is neither …` and `TRANSLATION FAULT: duration: the
counted unit 'vadem' is neither …`. Rood-eerst for the arm: until then the
three readers spoke two sentences — the verifier `'vadem' is not a
recognised time unit or declared day kind`, `ref` and the ASP backend both
`duration unit "vadem" is neither a recognised time unit nor a declared day
kind` — and the verifier of that day misses this `expect.txt`.
`expect.txt` pins the arm.
