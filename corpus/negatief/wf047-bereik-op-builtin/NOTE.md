Violates WF-047 (`spec/IR-SPEC.md` ch. 2.6): the builtin `ABSOLUTE_WAARDE`
over the subject's own `drempelgewicht` carries an `elementObjecttype` and,
because a single-argument `Functie` with an `elementObjecttype` carries a
range under every kind but `TOTAAL` (the schema's condition, U2), a
`bronRol`. A builtin is no instance fold: it has no source and ranges over
nothing, so the binding binds nothing.

    WF-047 FAIL /rules/resolvedRegels/0/waarde/bronRol: a fold-source role
    binding on a node that is not an instance fold over a relation

**Rewritten at L2 (2026-09-24), stamp: herschreven op WF-047 (D17).** Until
L2 this document was `wf034-sentinel-builtin-operand`, a witness of WF-034's
sentinel fence: the builtin's operand carried the unknown-type sentinel. The
sentinel left the wire with L2 (B13), and the migration
(`spec/proposals/v1-migratie/m2.py`) derived the range the schema demands at
this node; measured on that translation, the verifier refuses the range on
WF-047. The stamp list of the L2 card expected verwijderd-met-reden (its
measurement predates the range the migration writes); a builtin with an
`elementObjecttype` stays ill-formed either way — without a range the loader
refuses it, with one WF-047 does. `expect.txt` pins the arm (U1).
