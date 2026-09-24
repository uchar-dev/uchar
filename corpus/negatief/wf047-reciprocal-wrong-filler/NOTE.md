Violates WF-047 (`spec/IR-SPEC.md`): a fold-source binding into a
reciprocal fact type whose single role fills a different type than the
fold's element (and subject): the `count` fold on `Vondst` binds
`{maatschap, maat}`, but `maat` fills `Jutter`. The reciprocal shape
resolves (one role, bound = opposite), so the violation is the filler
disagreement — the clause this case pins for the single-role path.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
