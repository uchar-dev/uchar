Violates WF-069 (`spec/IR-SPEC.md` ch. 4.8): the `function` operand of a
`Fold` is a bare FunctionDef name (`"function": "optellen"`) rather than
an inline `Lambda`. A fold/map function operand MUST be an inline
`Lambda`; a named function reaches a fold through a lambda body that
`Invoke`s it (the mirror-safe route the anchors take). The named form is
structurally valid on the wire but has no executor witness, so the
verifier refuses it before translation.

A minimal hand-built document; the declared FunctionDef `optellen` is
otherwise well-formed, so WF-069 is the sole violation.
