# wf102-source-path-empty

Violates **WF-102 (b)** (`spec/IR-SPEC.md` ch. 4.3): the range is present
and empty.

    WF-102 FAIL /rules/rules/0/versions/0/value/sourcePath: a
    sourcePath carries at least TWO steps, found 0 — the one-step range is
    spelled sourceRole

The empty list is not "no range": it is a stated range that reaches nothing,
and a consumer reading it as "walk no steps" would fold the SUBJECT itself as
its own element. The field is loaded verbatim — empty included — precisely so
this is a violation the rule can cite rather than a load failure with no rule
to name.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
