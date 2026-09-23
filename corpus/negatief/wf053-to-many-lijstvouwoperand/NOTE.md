Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a) at a listed value fold's
operand: `MAXIMUM(drempelgewicht, gewicht van de vondsten)` lists two
operands, and the second reads `Vondst.gewicht` from the subject `Jutter`
through the to-many role `vondst` of `vondstaanmelding`. A listed fold's
operands are ordinary reads, one value each (§5.8.3); a to-many step there
needs an aggregation of its own.

    WF-053 FAIL /rules/resolvedRegels/0/waarde/argumenten/1: hop 0 reaches the
    to-many role 'vondst' in a scalar slot — a to-many step needs an
    aggregation, never a silent first pick

**Successor at L2 (2026-09-24), stamp: vervangen (U8).** This directory
replaces `wf096-setlezing-geleste-operand`, whose defect was the set reading
on a listed operand (WF-096), and `wf034-sentinel-geleste-setlezing`, whose
defect was the unknown-type sentinel on the same operand (WF-034's sentinel
fence). Both fields left the wire with L2 (B11, B13), and the migration
(`spec/proposals/v1-migratie/m2.py`) translated the two documents into this
one, byte for byte; measured on it, the verifier refuses the to-many hop on
WF-053. `expect.txt` pins the arm (U1).
