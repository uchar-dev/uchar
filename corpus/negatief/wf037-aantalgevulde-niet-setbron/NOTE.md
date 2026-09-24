Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in the filled count's
source arm: an `AANTAL_GEVULDE` over the `Vondst`en of the subject, its
range `vondstaanmelding`/`vondst` written, whose single argument is a
literal. The filled count reads a VALUE on each contributor and tests it for
presence; its source is the set source, an attribute reference read per
contributor, and a literal is no such read.

    WF-037 FAIL /rules/resolvedRegels/0/versions/0/value: the filled count's
    single argument MUST be its set source, an attribute reference read on each
    contributor: this kind reads a VALUE per contributor and tests that value
    for presence (ch. 4.3)

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
argument was the subject's own attribute read without the set reading,
beside an `elementObjecttype` and without a range. Since L2 the set source
is the argument of a fold that carries a range (D8 (i)), and a
single-argument fold with an `elementObjecttype` carries one under every
kind but `TOTAAL` (U2): the old shape is refused by the schema and the
loader before WF-037 is asked, so the set-source demand of the arm is
enforced at load. What the verifier still decides is a range beside a source
that is no attribute reference, and this document pins that. The card
expected a rewrite without range and without `elementObjecttype`; measured,
that shape is `wf037-aantalgevulde-zonder-elementtype`'s arm, already
witnessed, while this arm had none. `expect.txt` pins the arm (U1).

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
