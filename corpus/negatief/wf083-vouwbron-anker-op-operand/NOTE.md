Violates WF-083 (c) (`spec/IR-SPEC.md` ch. 4.3), the anchor of a COUNT
source: the `AANTAL` over the `Vondst`en of the subject `Jutter`, its range
`vondstaanmelding`/`vondst`, names `vondsten` as its source — a count source
— and spells the source's `objecttype` as `Vondst`, the element type. A
count source is read on the fold subject: its written role name is the
subject's relation.

    WF-083 FAIL /rules/rules/0/versions/0/value/arguments/0: fold-source
    subjectType 'Vondst' is not the fold's anchor 'Jutter' (a count source is
    read on the fold subject): the executors resolve 'vondsten' against the
    anchor, never against this field, so the two spellings name different
    declarations

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the source
also carried a written role and the set reading, and the verifier reported the
set reading (WF-096) beside the anchor. Both fields left the wire with L2
(B11); the migration refused the written role, which has no L2 carrier.
Written by hand without it: what remains is the anchor, per position (IR-SPEC
ch. 4.3) — this document is the witness of the count-source anchor, beside
`wf083-vouwbron-vreemd-elementtype` (a value fold's set source anchored on the
subject) and `wf083-vouwbron-vreemd-objecttype` (a subject's own read anchored
elsewhere). `wf096-aantal-setbron-wrat` translated to the same shape and was
removed as a duplicate of this one.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
