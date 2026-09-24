Violates WF-038 (`spec/IR-SPEC.md`): a tijdsduur without a node type at all —
the counted unit lives on the node type, so an absent type cannot carry the
required time-unit spelling; without the check this wire is verifier-clean and
faults only at evaluation.

A minimal hand-broken variant of `corpus/golden/anchor/jutterswet-jutleeftijd/ir.json`.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
