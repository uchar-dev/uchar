# wf102-source-path-a-step

Violates **WF-102 (b)** (`spec/IR-SPEC.md` ch. 4.3): a one-step range spelled
as a path.

    WF-102 FAIL /rules/rules/0/versions/0/value/sourcePath: a
    sourcePath carries at least TWO steps, found 1 — the one-step range is
    spelled sourceRole

The one-step range already HAS a spelling — `sourceRole` — which every existing
document carries and every consumer already reads. Admitting the same meaning
in a second shape is what WF-087 refuses for the nullary keuzetype and what
WF-089/WF-090 keep true for domain identity: two frontends must not be able to
write one meaning two ways.

The step is deliberately a VALID one-step range, so the arity is the only
defect and the witness falls for its own reason: the document adds a direct
`vondstaanmelding` relating the fold subject `Jutter` to the element type
`Vondst`, and the single step binds it. Move it to `sourceRole` and the document
is clean. That isolation is why this witness is separate from
`wf102-source-path-empty`: the two arms of (b) fall independently.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
