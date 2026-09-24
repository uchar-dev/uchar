Violates WF-027 (`spec/IR-SPEC.md` ch. 3.3): a characteristic initialisation
in an object creation carries an `AttributeReference` value whose declaration is
numeric, where the value of a characteristic must be a boolean-typed
expression.

    WF-027 FAIL /rules/objectCreations/0/versions/0/initialisations/0:
    characteristic initialisation's value must be a boolean-typed expression

Pins the cross-validation flank of the Phase 5c WF-027 relaxation. The relaxed
rule admits a boolean-*typed* expression, and the type of a reference is its
declaration's: here the created `Jutter`'s characteristic `penninggerechtigd`
is initialised from `de vloototal van de Rederij`, and the subject `Rederij`'s
attribute `vloototal` is declared `Number`, so the verifier refuses it
citing WF-027.

Until L2 the reference carried a serialized `type` of its own, and this
document forced it to `{Booleaan}` — the lie the directory name refers to.
The witness then pinned that the verifier did not trust that field but
resolved the referenced attribute's declared datatype (the WF-023
cross-validation used in rekenposities). Without that cross-validation the
lie passed the verifier and diverged at runtime (ref faulted on the
non-boolean write; a guardless model would deposit silently). L2 (B13) took
the field off the wire, so the declaration is the only source of the type
and the lie cannot be written any more; the document is now a numeric read
in a boolean slot, refused by the same arm with the same message.

Since L4 an object creation is an `ObjectCreation` with its versions (delta
B19); the defect sits on the `ObjectCreationVersion`, whose fields are
`createdType`, `subject`, `initialisations` (each a `MemberInit`) and
`connectingRole`. `expect.txt` pins the arm.
