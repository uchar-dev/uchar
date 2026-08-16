Violates WF-028 (`spec/IR-SPEC.md`): a **fully symbolic** fact creation
whose every binding resolves and whose join chain reaches the target —
but the relation of the final hop (`joinRol` over `bergingstoezicht`)
is the *created* relation itself. Creating a link along the very
relation being joined is ill-formed: the created relation must differ
from the relation of the final hop. This case exercises the whole
chain walk (completeness, end agreement, hop direction, reached type)
up to its last clause.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.
