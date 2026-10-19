Violates WF-038 (`spec/IR-SPEC.md` ch. 4.3, the duration clause): the
counted unit of an `absoluteDuration` lives on the node's type and MUST be a
recognised time unit or a declared day kind; here it is `vadem`. Broken from
`corpus/golden/anchor/jutterswet-jutleeftijd`: the rule `Berekening
maandafstand` counts in `vadem` instead of `mnd`, and the target attribute
`Jutter.maandafstand` is declared in `vadem` as well, so that no other rule
(WF-023's unit clause) reports the pair.

    WF-038 FAIL /rules/rules/2/versions/0/value: absoluteDuration: the
    counted unit 'vadem' is neither a recognised time unit nor a declared
    day kind

The sentence names the builtin's own kind; it is spelled once,
`duration_unknown_unit` in `spec/verifier/lib/checks.ml`, and `ref` and the
ASP backend speak it too (measured 2026-09-25 on this document: `EVALUATION
FAULT: absoluteDuration: the counted unit 'vadem' is neither …` and
`TRANSLATION FAULT:` with the same sentence). Until then all three readers
named this node `duration:`, the other kind's name — the verifier of that
day misses this `expect.txt`. The `duration` form of the same defect is
`corpus/negative/wf038-duration-unknown-unit`. The Drools loader refuses
the document at load in the same sentence, at the same pointer (measured
2026-09-25). `expect.txt` pins the arm.
