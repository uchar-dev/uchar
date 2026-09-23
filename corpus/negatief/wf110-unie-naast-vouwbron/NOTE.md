Violates WF-110 (c) (`spec/IR-SPEC.md` ch. 4.3): a well-formed two-selection
union under a count that ALSO carries `elementObjecttype: "Vondst"` and,
with it, a range. The union's selections carry the population and the
relation, one set per selection; the count's own source is a second claim on
one node, and nothing decides which of the two a consumer should read.

    WF-110 FAIL /rules/resolvedRegels/0/waarde: a count over an instance-set
    union names no source of its own, and this node carries elementObjecttype
    + bronRol: the union's selections carry the population, the relation and
    the subselection, one set per selection, so a source here is a second
    claim on one node (ch. 4.3)

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the count
carried the `elementObjecttype` alone. Since L2 a single-argument fold with
an `elementObjecttype` carries a range under every kind but `TOTAAL` (U2),
so the migration had to derive one and refused: nothing derives a range
beside a union. Written by hand with the first selection's relation as
`bronRol`; the clause now names both halves of the count's own source.
`expect.txt` pins the arm (U1).
