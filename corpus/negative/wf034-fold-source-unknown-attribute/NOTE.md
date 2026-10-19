Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the source of a value fold over
the subject's `maat`s (a reciprocal relation over `Jutter`, its range
written) names the attribute `bestaatniet`, which no declaration carries. A
value fold's source reads an attribute of each contributor, and this one
resolves against nothing.

    WF-034 FAIL /rules/rules/1/versions/0/value/arguments/0: 'bestaatniet'
    is not a declared attribute of 'Jutter': a value fold's source reads an
    attribute of each contributor

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) Until L2 the fold
carried no range and the verifier's type-pair fallback stood in for it; the
attribute name was the defect then as now. Since L2 the range is written
(B12): the migration derived it from the one reciprocal role, and the
unknown name stayed.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
