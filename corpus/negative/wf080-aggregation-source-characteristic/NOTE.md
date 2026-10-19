Violates WF-080 (`spec/IR-SPEC.md` ch. 4.1): the value position an aggregation
folds over resolves to a **characteristic** endpoint, not an attribute. The rule
`penningtotaal van een jutter` sums over `Penning`, but its `sum` source
`AttributeReference` names `uitgereikt` — a declared kenmerk of `Penning`, not
an attribute — so each element carries no value to fold.

Pins the sibling of the `has_attr || has_kenmerk` leak: the aggregation source
takes a separate path from the generic value walk (`aggregation_source_check`),
which admitted an attribute, a characteristic, OR a role. A value-yielding fold
(`sum`/`total`/`minimum`/…) reads an attribute value; a count names a role
(`is_role_name`, kept legal — the count-over-role v0 encoding); a characteristic
endpoint is ill-formed in neither shape. The walk_expr WF-080 branch already
refuses a characteristic in a value slot; this case pins the fold source too.

The split is un-pinnable by construction: with `bedrag` swapped for the kenmerk
`uitgereikt`, `ref` folds the characteristic as empty and computes `0`, while
`clingo` faults `TRANSLATION FAULT: unknown attribute "uitgereikt" on "Penning"`
— one executor yields a value, the other refuses, exactly the class WF-080 keeps
out.

Old verifier: `PASS` (the has_attr||has_kenmerk leak admits it). New verifier:
`WF-080 FAIL … 'uitgereikt' is a characteristic of 'Penning', not an attribute
— a characteristic endpoint is ill-formed in an aggregation source position`.
