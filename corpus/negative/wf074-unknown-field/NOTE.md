Violates WF-074 (`spec/IR-SPEC.md` ch. 4.9): the `FieldAccess` reads a
field `onbekend` that the `dossier` struct does not declare. A field
access must name a declared field of its struct-typed base.

A minimal hand-built document.
