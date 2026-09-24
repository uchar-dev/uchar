Violates WF-028 (`spec/IR-SPEC.md`): a **fully symbolic** fact creation
whose every binding resolves and whose join chain reaches the target —
but the relation of the final hop (the last step of the `path`, over
`bergingstoezicht`) is the *created* relation itself. Creating a link along the very
relation being joined is ill-formed: the created relation must differ
from the relation of the final hop. This case exercises the whole
chain walk (completeness, end agreement, hop direction, reached type)
up to its last clause.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.

    WF-028 FAIL /rules/feitCreaties/0/versions/0: the created relation must
    differ from the relation of the final hop

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the fact
creation lost its bean fields and carries its symbolic block alone (B7,
D12); the created relation equals the relation of the final hop. The rule
still fires on the same arm, and `expect.txt` pins it (U1).

Since L4 the join chain is the one `path` of the `FactCreationVersion` — the
intermediate steps followed by the hop that reaches the target — and the
target type is not written: it is the declared filler of `targetRole` (IR-SPEC
ch. 3.4). `expect.txt` pins the arm.
