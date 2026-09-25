Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in the source arm
of the two counts: a `filledCount` over the `Vondst`en of the subject, its
range `vondstaanmelding`/`vondst` written, whose single argument is a
literal. The filled count reads a VALUE on each contributor and tests it for
presence; its source is the set source, an attribute reference read per
contributor, and a literal is no such read.

    WF-037 FAIL /rules/rules/0/versions/0/value: a filledCount aggregation
    carries elementType "Vondst" and a Literal operand: the source of an
    instance fold is an AttributeReference over its range

**Moved to the source arm of the two counts (the card
aantal-bronarm-en-drools-lader).** Until that card the filled count's own
set-source arm refused this node, without naming the argument:

    WF-037 FAIL /rules/rules/0/versions/0/value: the filled count's
    single argument MUST be its set source, an attribute reference read on
    each contributor: this kind reads a VALUE per contributor and tests that
    value for presence (ch. 4.3)

That arm is now the source arm of both counts, which names the node it
refuses, and this document keeps witnessing it under the filled count;
the six `wf037-aantal-elementtype-*` witnesses carry it under the count.
Measured before that card on this document, with a case outside the corpus
(the population of golden `jutterswet-vondsttelling`), `ref` and the ASP
backend refused in their bottom sentences (`unsupported instance-fold
source shape`; `expression kind not translated (no corpus case exercises
it)`), the Drools exec-model branch excluded the rule (transpiler SKIP) and
the DRL-text branch played it as an ordinary count (PLAYED 1, the range
counted and the argument never read). Since the card `ref`, the ASP backend
and the Drools loader refuse it in the verifier's sentence. The sweep and
its mutants are recorded in
`corpus/negatief/wf037-aantal-elementtype-literaal/NOTE.md`.

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
argument was the subject's own attribute read without the set reading,
beside an `elementObjecttype` and without a range. Since L2 the set source
is the argument of a fold that carries a range (IR-SPEC ch. 4.3), and a
single-argument fold with an `elementObjecttype` carries one under every
kind but `TOTAAL` (B12): the old shape is refused by the schema and the
loader before WF-037 is asked, so the set-source demand of the arm is
enforced at load. What the verifier still decides is a range beside a source
that is no attribute reference, and this document pins that. The card
expected a rewrite without range and without `elementObjecttype`; measured,
that shape is `wf037-aantalgevulde-zonder-elementtype`'s arm, already
witnessed, while this arm had none.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. The L2 paragraph above
names the fields as they were spelled then (`elementObjecttype` →
`elementType`; since L5 `TOTAAL` → `total`). `expect.txt` pins the arm.
