Violates WF-068 (`spec/IR-SPEC.md` ch. 4.8): the function `dubbel` declares
one parameter, but the `Invoke` supplies two arguments. An `Invoke`'s
argument count MUST equal the parameter count.

A minimal hand-built document; the `Invoke` rides a `GEVULD` predicate so the
verifier's expression walk reaches it.
