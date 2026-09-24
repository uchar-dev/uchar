Violates WF-027 (`spec/IR-SPEC.md` ch. 3.3): a characteristic initialisation
in an object creation carries a value that is not a boolean-typed expression.

Pins the new boundary of the Phase 5c WF-027 relaxation. The old rule demanded
a boolean *constant*; the relaxed rule admits any boolean-*typed* expression
(the constant stays the v0 form) but still refuses a statically non-boolean
value. Here the created `Jutter`'s characteristic `penninggerechtigd` is
initialised from the numeric literal `5` — the verifier types it `Number`,
not `Booleaan`, and refuses it citing WF-027.
