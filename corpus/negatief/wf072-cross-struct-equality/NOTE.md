Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4): the consistency compares a
`dossier`-typed operand with a `kaart`-typed operand. Two struct
operands of one comparison must name the same declaring domain — the
nominal-identity rule (cross-domain struct comparison is statically
ill-formed, never false).

A minimal hand-built document.
