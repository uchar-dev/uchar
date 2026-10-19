Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that a count
source demands `count` (since the L2 landing): the `sum` over the `Vondst`en of
the subject names `vondsten` as its source — a role of the fold subject, and
no attribute of the element type `Vondst`. Such a source hands the fold the
linked instances, and a sum reads a value per contributor.

    WF-093 FAIL /rules/rules/1/versions/0/value/arguments/0: value
    aggregation over "vondsten", which is no attribute of "Vondst": a value fold
    reads an attribute per contributor, and a role name is a count source only a
    count folds

The message is the sentence `ref`, the ASP backend and the Drools loader
share, taken over by the verifier in the L2 fix round; until then the
verifier's own sentence named the kind ("… it is a COUNT source, which
yields the linked instances, and only AANTAL reads those — it cannot be the
source of this SOM aggregation").

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-093, the rule its
translation falls on.** Until L2 this document was
`wf034-foldbron-rolnaam-waardevouw`, a WF-034 witness: outside the v0
count-source spelling a fold source was an ordinary attribute reference, and a
role name there resolved to no attribute. That spelling left the wire with L2
(B13), and WF-093 recognises the count source at the source itself, so the
migration's translation (`spec/proposals/v1-migratie/m2.py`) falls on WF-093,
the rule this directory now names.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
