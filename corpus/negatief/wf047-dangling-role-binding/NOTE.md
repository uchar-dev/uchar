Violates WF-047 (`spec/IR-SPEC.md`): the allocation's `verbindendeRol`
binds role `bewaker`, which is not a declared role of fact type
`bergingsdeelname` (its roles are `berging` and `berger`; the plural
does not bind either).

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
