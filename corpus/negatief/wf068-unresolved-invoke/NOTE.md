Violates WF-068 (`spec/IR-SPEC.md` ch. 4.8): the `Invoke` names the function
`aanspraakwaarde`, but no `FunctionDef` of that name is declared. An
`Invoke` MUST name a declared function definition.

A minimal hand-built document; the `Invoke` rides a `GEVULD` predicate so the
verifier's expression walk reaches it.
