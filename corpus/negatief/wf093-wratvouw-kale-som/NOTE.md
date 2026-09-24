Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that a count
source demands AANTAL (since the L2 landing): a `SOM` over the `Vondst`en of
the subject `Jutter` names `vondsten` as its source — a role of the fold
subject, and no attribute of the element type.

    WF-093 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/0: value
    aggregation over "vondsten", which is no attribute of "Vondst": a value fold
    reads an attribute per contributor, and a role name is a count source only a
    count folds

The message is the sentence `ref`, the ASP backend and the Drools loader
share, taken over by the verifier in the L2 fix round; until then the
verifier's own sentence named the kind ("… it is a COUNT source, which
yields the linked instances, and only AANTAL reads those — it cannot be the
source of this SOM aggregation").

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) As
`wf093-aantalgevulde-wrat`, under `SOM`: the sentinel spelling of the count
source left the wire (B13), WF-093 keys on the source itself, and the
document is the migration's translation. `wf093-telbron-onder-som` pins the
same arm on the document that was WF-034's role-name witness.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
