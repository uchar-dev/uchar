# wf034-sentinel-builtin-wratoperand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the single operand of the
BUILTIN `ABSOLUTE_WAARDE` names `vondsten` — the declared plural of the role
`vondst` — as an attribute of `Jutter`. A builtin has no fold source, so its
operand is an ordinary read, and a role name read as an attribute resolves
to nothing.

The document. `ABSOLUTE_WAARDE` with `elementObjecttype: "Vondst"` over
`vondsten` on `Jutter`. Because a single-argument `Functie` with an
`elementObjecttype` carries a range under every kind but `TOTAAL` (the
schema's condition, U2), the migration derived `bronRol`
`vondstaanmelding`/`vondst`, and WF-047 refuses that range on a builtin —
the second line of the report, pinned on its own by
`wf047-bereik-op-builtin`.

    WF-034 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: 'vondsten' is
    neither an attribute nor a characteristic of 'Jutter'

**Migrated at L2 (2026-09-24), stamp: herschreven (m), armwissel.** Until L2
the operand carried the unknown-type sentinel on its own `type` — v0's
spelling of a count source — at a position that is no fold source, and
WF-034 refused the sentinel there. Measured on the executors then: `ref`
faulted (`unsupported AttribuutRef shape`), the ASP backend refused the
expression kind, and the Drools backend threw looking for a getter of the
role name on the subject bean. The sentinel left the wire with the
reference's own annotation (B13); the migration's translation
(`spec/proposals/v1-migratie/m2.py`) reads the role name as an ordinary
attribute of `Jutter`, and WF-034 refuses it in its name-resolution arm. The
witness now guards the edge of the count-source recognition (D9): a role
name is a count source only as the single argument of an aggregation.
`expect.txt` pins the new arm (U1).
