Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the direction that a count
source demands AANTAL (D9): an `AANTAL_GEVULDE` over the `Vondst`en of the
subject `Jutter` names `vondsten` as its source — a role of the fold
subject, and no attribute of the element type. Such a source hands the fold
the linked instances; the filled count reads a value on each contributor and
tests it for presence, and an instance has no value to test.

    WF-093 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: the fold source
    'vondsten' names a role of the fold subject and no attribute of the
    element type 'Vondst': it is a COUNT source, which yields the linked
    instances, and only AANTAL reads those — it cannot be the source of this
    AANTAL_GEVULDE aggregation

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the count
source was spelled with the unknown-type sentinel on the argument's own
`type`, and WF-093 keyed on that sentinel. L2 took the annotation off the
reference (B13) and the count source is recognised at the source itself
(D9): its `attribuut` names a role of the fold subject and no attribute of
the element type. The document is the migration's translation
(`spec/proposals/v1-migratie/m2.py`), which also wrote the range the schema
demands; measured without the re-keying the verifier would have fallen back
on WF-034's name resolution. `expect.txt` pins the arm (U1).
