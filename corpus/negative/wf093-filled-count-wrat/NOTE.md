Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that a count
source demands `count` (since the L2 landing): a `filledCount` over the
`Vondst`en of the subject `Jutter` names `vondsten` as its source — a role of
the fold subject, and no attribute of the element type. Such a source hands
the fold the linked instances; the filled count reads a value on each
contributor and tests it for presence, and an instance has no value to test.

    WF-093 FAIL /rules/rules/0/versions/0/value/arguments/0: value
    aggregation over "vondsten", which is no attribute of "Vondst": a value fold
    reads an attribute per contributor, and a role name is a count source only a
    count folds

The message is the sentence `ref`, the ASP backend and the Drools loader
share, taken over by the verifier in the L2 fix round; until then the
verifier's own sentence named the kind ("… it is a COUNT source, which
yields the linked instances, and only AANTAL reads those — it cannot be the
source of this AANTAL_GEVULDE aggregation").

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) Until L2 the count
source was spelled with the unknown-type sentinel on the argument's own
`type`, and WF-093 keyed on that sentinel. L2 took the annotation off the
reference (B13) and the count source is recognised at the source itself
(WF-093): its `attribute` names a role of the fold subject and no attribute of
the element type. The document is the migration's translation
(`spec/proposals/v1-migratie/m2.py`), which also wrote the range the schema
demands; measured without the re-keying the verifier would have fallen back
on WF-034's name resolution.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
