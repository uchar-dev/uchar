Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8): the fold's lambda body and the
type the fold requires differ in **wholeness** alone. Broken from
`corpus/rood/closed-nonexact-division`: the fold `quotientsom` takes back its
v0 annotation `whole: true` on the fold's own `type`, on `init` and on the
accumulator parameter `acc`, while the body `acc + x / 3` stays fractional —
a division is not whole (`RUNTIME-SEMANTICS.md` ch. 2.3), and the `plus`
above it takes that over (WF-091). Every other WF-070 comparison agrees, so
the report is the one line.

    WF-070 FAIL /rules/rules/0/versions/0/value/function: lambda body is
    number(0 decimals, fractional), but the fold requires number(0 decimals,
    whole)

What it pins is the message, not the rule: until 2026-09-25 the verifier
rendered both sides without their wholeness and reported `lambda body is
number(0 decimals), but the fold requires number(0 decimals)` — one type
named twice. A pair of types that renders alike now names the wholeness of
each (`detail_vs` in `spec/verifier/lib/checks.ml`); a pair that already
renders differently is unchanged, measured over the 742 documents of the
golden, rood and negatief zones. Rood-eerst: the verifier before that change
rejects this document on WF-070 without the `expect.txt` fragment.
`expect.txt` pins the arm.
