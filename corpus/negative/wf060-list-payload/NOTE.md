Violates WF-060 (`spec/IR-SPEC.md` ch. 2.4): the `Toegekend` constructor
carries a `ListType` payload. A payload is a scalar datatype — a number, a
text, a boolean, a date of either precision, an enumeration — or a declared
`SumType`; a list and a struct are no payload.

A minimal hand-built document — the shape of the
`corpus/golden/anchor/jutterswet-beschikkingsom` declaration.
