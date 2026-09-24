Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that a count
source demands AANTAL (D9): an `EERSTE` over the `Vondst`en of the subject
`Jutter` names `vondsten` as its source — a role of the fold subject, and no
attribute of the element type. `EERSTE` reads a value on each contributor; a
count source hands it the linked instances.

    WF-093 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: value aggregation
    over "vondsten", which is no attribute of "Vondst": a value fold reads an
    attribute per contributor, and a role name is a count source only a count
    folds

The message is the sentence `ref`, the ASP backend and the Drools loader
share, taken over by the verifier in the L2 fix round; until then the
verifier's own sentence named the kind ("… it is a COUNT source, which
yields the linked instances, and only AANTAL reads those — it cannot be the
source of this EERSTE aggregation").

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) As
`wf093-aantalgevulde-wrat`, under `EERSTE`: the sentinel spelling of the
count source left the wire (B13), WF-093 keys on the source itself (D9), and
the document is the migration's translation. `expect.txt` pins the arm (U1).
