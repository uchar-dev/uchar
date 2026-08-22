Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8): the `Fold`'s `list` operand is
a scalar (`Numeriek`) literal, not a list value. The fold-family `list`
operand MUST be list-typed.

A minimal hand-built document; the accumulator and function are well-formed,
so the non-list source is the sole violation.
