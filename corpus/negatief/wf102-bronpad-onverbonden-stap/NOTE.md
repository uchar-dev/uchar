# wf102-bronpad-onverbonden-stap

Violates **WF-102 (d)** (`spec/IR-SPEC.md` ch. 4.3), the MIDDLE join: the
second step does not start where the first one ends.

    WF-102 FAIL /rules/resolvedRegels/0/versions/0/value/sourcePath/1: the
    opposite role 'verpakte vondst' fills 'Vondst', but this step starts at
    'Boot' (the type the previous step reaches) — a sourcePath is a connected
    walk

Jutter → `gevoerde boten` reaches a Boot; the next step is `inpakking`, whose
other role is filled by Vondst. Both steps resolve on their own (WF-047 is
satisfied twice) and the path lands on the declared element type, so the
document would be clean under a rule that only checked its endpoints. That is
what makes this the arm's own witness: a path is not a pair of endpoints with
decoration, and a consumer walking a broken chain reaches nothing at all while
one deriving by pair would happily fold SOMETHING.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
