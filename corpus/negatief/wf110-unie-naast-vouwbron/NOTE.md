Violates WF-110 (c) (`spec/IR-SPEC.md` ch. 4.3): a well-formed two-selection
union under a count that ALSO carries `elementType: "Vondst"` and,
with it, a range. The union's selections carry the population and the
relation, one set per selection; the count's own source is a second claim on
one node, and nothing decides which of the two a consumer should read.

    WF-110 FAIL /rules/rules/0/versions/0/value: a count over an
    instance-set union names no source of its own, and this node carries
    elementType + sourceRole: the union's selections carry the population, the
    relation and the subselection, one set per selection, so a source here is a
    second claim on one node (ch. 4.3)

Measured before the card aantal-bronarm-en-drools-lader, with a case
outside the corpus: `ref` refused the node in its residual sentence
(`unsupported instance-fold source shape`), the ASP backend in a sentence
of its own, and both Drools branches played it, counting the union and
never reading the node's own source; with the range alone beside the union
`ref` counted it as if the range were not written. Since the card `ref`, the
ASP backend and the Drools loader refuse every such count in the sentence
above, naming its fields in the verifier's order.

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the count
carried the `elementObjecttype` alone. Since L2 a single-argument fold with
an `elementObjecttype` carries a range under every kind but `TOTAAL` (B12),
so the migration had to derive one and refused: nothing derives a range
beside a union. Written by hand with the first selection's relation as
`bronRol`; the clause now names both halves of the count's own source.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each with
only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. The L2 paragraph above
names the fields as they were spelled then (`elementObjecttype` → `elementType`,
`bronRol` → `sourceRole`; since L5 `TOTAAL` → `total`). `expect.txt` pins the
arm.
