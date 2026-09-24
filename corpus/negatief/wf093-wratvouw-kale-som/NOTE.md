Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that a count
source demands AANTAL (D9): a `SOM` over the `Vondst`en of the subject
`Jutter` names `vondsten` as its source — a role of the fold subject, and no
attribute of the element type.

    WF-093 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: the fold source
    'vondsten' names a role of the fold subject and no attribute of the
    element type 'Vondst': it is a COUNT source, which yields the linked
    instances, and only AANTAL reads those — it cannot be the source of this
    SOM aggregation

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) As
`wf093-aantalgevulde-wrat`, under `SOM`: the sentinel spelling of the count
source left the wire (B13), WF-093 keys on the source itself (D9), and the
document is the migration's translation. `wf093-telbron-onder-som` pins the
same arm on the document that was WF-034's role-name witness. `expect.txt`
pins the arm (U1).
