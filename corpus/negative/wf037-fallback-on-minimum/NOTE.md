Violates WF-037 (`spec/IR-SPEC.md`): The sum-only empty-value fallback attached to a minimum.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
