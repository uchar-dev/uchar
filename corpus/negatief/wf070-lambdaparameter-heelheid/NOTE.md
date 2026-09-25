Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8): the accumulator parameter `acc`
of the fold's lambda is declared whole, while the type the fold hands it —
the fold's own `type` — is fractional. Broken from
`corpus/golden/jutterswet-kinderfoldheffing` by that one field. The body
`acc + x × 2` stays fractional (a product is not whole,
`RUNTIME-SEMANTICS.md` ch. 2.3), and the fold, its `init` and its body agree,
so the report is the one line.

    WF-070 FAIL /rules/rules/1/versions/0/value/function: lambda parameter
    'acc' is number(0 decimals, whole), expected number(0 decimals, fractional)

What it pins is the message, not the rule: until 2026-09-25 the verifier
rendered both sides without their wholeness and reported `lambda parameter
'acc' is number(0 decimals), expected number(0 decimals)`. A pair of types
that renders alike now names the wholeness of each (`detail_vs` in
`spec/verifier/lib/checks.ml`). Rood-eerst: the verifier before that change
rejects this document on WF-070 without the `expect.txt` fragment.
`expect.txt` pins the arm.
