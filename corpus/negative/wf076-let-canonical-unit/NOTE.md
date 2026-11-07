Violates WF-076 (`spec/IR-SPEC.md` ch. 4.10): a `Let` binds `b` to a read of
`Jutter.gewicht`, declared in `kg` with the canonical unit `kg`, and its body
reads `b`, so the body's type is the declaration's. The `Let` node repeats
that type in every facet but one: its unit names the canonical unit `KG`, and
a unit is compared byte for byte (WF-091). Cut from
`corpus/negative/wf076-let-unit-against-body`.

    WF-076 FAIL /rules/rules/0/versions/0/value/type: the let type (number(2
    decimals, kg, canonical KG)) differs from its body type (number(2
    decimals, kg, canonical kg)) — the let type must equal the body type

The `Let` node's unit is also refused by WF-009's arm on the conversion of a
unit where it occurs (`WF-009.undeclared-conversion`): its canonical `KG` is
not its declaration's `kg` byte for byte. The case keeps it, because the
facet that differs is what the message has to name; gate 3 holds it to its own
arm.

What it pins is the message: where two types differ only in a facet the
rendering leaves out, each side names the facets in which they differ
(`detail_vs` in `spec/verifier/lib/checks.ml`). Until 2026-09-25 the verifier
reported the same type twice: `the let type (number(2 decimals, kg,
fractional)) differs from its body type (number(2 decimals, kg,
fractional))`. `expect.txt` pins the arm.
