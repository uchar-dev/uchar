Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a): a navigated read from the
subject `Wachter` whose two bindings stand in the wrong order — the first
hop, over `kustligging`, starts at a `Post`, while the walk stands at the
`Wachter`. Every binding resolves; the chain does not join.

    WF-053 FAIL /rules/rules/0/versions/0/value: hop 0 over
    'kustligging': the near side fills 'Post', but the walk stands at 'Wachter'
    WF-053 FAIL /rules/rules/0/versions/0/value: hop 1 over
    'postbezetting': the near side fills 'Wachter', but the walk stands at
    'Kustvak'
    WF-053 FAIL /rules/rules/0/versions/0/value: the navigation reaches
    'Post', not the endpoint 'Kustvak'

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the read lost
its written hop names (B11), and the two lines the verifier drew for a
written step that did not denote its bound role left with them; the
near-side and endpoint lines remain. `expect.txt` pins the arm.
