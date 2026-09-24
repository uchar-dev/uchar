Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a) at the position of the whole
rule value: the rule reads `gewicht` of the `Vondst` that `vondstaanmelding`
links to the subject `Jutter`, one bound hop whose reached role `vondst` is
to-many. A rule value is one value; a to-many step needs an aggregation,
never a silent first pick. `wf053-to-many-hop-in-scalar` pins the same
position on another model.

    WF-053 FAIL /rules/resolvedRegels/0/versions/0/value: hop 0 reaches the
    to-many role 'vondst' in a scalar slot — a to-many step needs an
    aggregation, never a silent first pick

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-053 (D17).** Until
L2 this document was `wf096-setlezing-regelwaarde`, a witness of WF-096: the
read carried the set reading (a field saying "the members' values, one per
member") at a position that consumes one value. The field left the wire with
L2 (B11, D8): the set source is the single argument of a fold that carries a
range, and nothing else. Measured on the migration's translation
(`spec/proposals/v1-migratie/m2.py`), what remains is the navigated read
itself, which WF-053 refuses for its to-many hop. `expect.txt` pins the arm
(U1).
