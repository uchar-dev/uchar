Violates WF-076 (`spec/IR-SPEC.md` ch. 4.10): the `Let` of the rule
`vangstsom van een vangst` is typed whole, while its body is fractional.
Broken from `corpus/golden/synth3-operandvolgorde` by that one field: the
body `deel + teller / noemer` is fractional — a division is not whole
(`RUNTIME-SEMANTICS.md` ch. 2.3), and the `plus` takes that over (WF-091) —
so the let type no longer equals the body type.

    WF-076 FAIL /rules/rules/0/versions/0/value/type: the let type (number(0
    decimals, whole)) differs from its body type (number(0 decimals,
    fractional)) — the let type must equal the body type

What it pins is the message, not the rule: until 2026-09-25 the verifier
rendered both sides without their wholeness and reported `the let type
(number(0 decimals)) differs from its body type (number(0 decimals))`. A pair
of types that renders alike now names the wholeness of each (`detail_vs` in
`spec/verifier/lib/checks.ml`). Rood-eerst: the verifier before that change
rejects this document on WF-076 without the `expect.txt` fragment.
`expect.txt` pins the arm.
