Violates WF-076 (`spec/IR-SPEC.md` ch. 4.10): a `Let` binds `b` to a read of
`Jutter.gewicht`, declared in `kg` of the unit system `Gewicht`, and its body
reads `b`, so the body's type is the declaration's. The `Let` node repeats
that type in every facet but one: its unit names the system `GEWICHT`, and a
unit is compared byte for byte (WF-091). Cut from
`corpus/negatief/wf076-let-eenheid-tegen-lichaam`.

    WF-076 FAIL /rules/rules/0/versions/0/value/type: the let type (number(2
    decimals, kg, system GEWICHT)) differs from its body type (number(2
    decimals, kg, system Gewicht)) — the let type must equal the body type

What it pins is the message: where two types differ only in a facet the
rendering leaves out, each side names the facets in which they differ
(`detail_vs` in `spec/verifier/lib/checks.ml`). Until 2026-09-25 the verifier
reported the same type twice: `the let type (number(2 decimals, kg,
fractional)) differs from its body type (number(2 decimals, kg,
fractional))`. `expect.txt` pins the arm.
