Violates WF-054 (`spec/IR-SPEC.md` ch. 3.6): a parent-scoped uniqueness
whose scope binding names the fact type `wijkindeling`, which the model does
not declare, so the scope resolves against nothing.

    WF-054 FAIL /rules/uniciteiten/0: role binding names undeclared fact type
    'wijkindeling'

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the uniqueness
lost its written scope field (B8, D12); the binding, which since L2 IS the
scoping, is unchanged. `expect.txt` pins the arm (U1).
