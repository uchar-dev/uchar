Violates WF-069 (`spec/IR-SPEC.md` ch. 4.8): a `Lambda` appears as an
ordinary expression operand (here a `GEVULD` predicate's operand). A
`Lambda` is admissible only as the `function` operand of a `Fold` or
`MapList`; anywhere else it is ill-formed (a lambda is never a value).

A minimal hand-built document.
