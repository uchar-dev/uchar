Violates WF-027 (`spec/IR-SPEC.md` ch. 3.3): a characteristic initialisation
in an object creation carries an `AttribuutRef` value whose serialized `type`
lies — it declares `{Booleaan}` while the attribute it reads is numeric.

Pins the cross-validation flank of the Phase 5c WF-027 relaxation. The relaxed
rule admits a boolean-*typed* expression, but an `AttribuutRef`'s own `type`
field is not trusted: the verifier resolves the referenced attribute's actual
datatype (the WF-023 cross-validation used in rekenposities). Here the created
`Jutter`'s characteristic `penninggerechtigd` is initialised from
`de vloototal van de Rederij`, forced to `type {Booleaan}`; the subject
`Rederij`'s attribute `vloototal` resolves to `Numeriek`, so the verifier
refuses it citing WF-027. Without this cross-validation the lie passes the
verifier and diverges at runtime (ref faults on the non-boolean write; a
guardless model would deposit silently).
