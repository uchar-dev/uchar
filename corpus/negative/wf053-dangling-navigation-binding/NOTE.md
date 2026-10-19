Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a): a navigated read whose binding
names the fact type `bemanning`, which the model does not declare, so the
hop resolves against nothing.

    WF-053 FAIL /rules/rules/0/versions/0/value: role binding names
    undeclared fact type 'bemanning'

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the read lost
its written hop name (B11); the binding is unchanged. `expect.txt` pins the
arm.
