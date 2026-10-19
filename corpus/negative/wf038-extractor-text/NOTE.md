Violates WF-038 (`spec/IR-SPEC.md`): a day-precision part extractor
(maand-uit) applied to a text operand — the extractor must take a date.

A minimal hand-broken variant of `corpus/golden/jutterswet-paasvondst/ir.json`.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
