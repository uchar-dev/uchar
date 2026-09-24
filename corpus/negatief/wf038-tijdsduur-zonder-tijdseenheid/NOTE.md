Violates WF-038 (`spec/IR-SPEC.md`): a tijdsduur whose node type carries no
recognised time unit — the counted unit is the node type's unit, keyed on its
spelling; without one the count is meaningless.

A minimal hand-broken variant of `corpus/golden/anchor/jutterswet-jutleeftijd/ir.json`.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
