Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a) at an operand of `PLUS`: the
left operand reads `Vondst.gewicht` from the subject `Jutter` through the
to-many role `vondst` of `vondstaanmelding`. An arithmetic operand is one
value; a to-many step there needs an aggregation.

    WF-053 FAIL /rules/resolvedRegels/0/versions/0/value/links: hop 0 reaches
    the to-many role 'vondst' in a scalar slot — a to-many step needs an
    aggregation, never a silent first pick

**Successor at L2 (2026-09-24), stamp: vervangen, named after the rule it now
pins.** This directory replaces `wf096-setlezing-operand`, whose defect was
the set reading on an operand of `PLUS` (WF-096). The field left the wire with
L2 (B11); measured on the migration's translation
(`spec/proposals/v1-migratie/m2.py`), what remains is the navigated read,
which WF-053 refuses for its to-many hop. `expect.txt` pins the arm.
