Violates WF-047 (`spec/IR-SPEC.md`): the allocation's `verbindendeRol`
resolves — `berging` is a declared role of `bergingsdeelname` — but it
binds the **source-side** role: its filler is `Berging`, while the node
names `Berger` as `doelType` (and the opposite role's filler mismatches
`bronType` symmetrically). The binding must bind the recipient role.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
