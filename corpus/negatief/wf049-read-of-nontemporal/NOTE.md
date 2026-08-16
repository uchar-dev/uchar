Violates WF-049 (`spec/IR-SPEC.md`): the temporal read's `basis`
references `vast quotum`, a plain (non-temporal) attribute — there is
no timeline to read. The basis must be an own temporal attribute
reference or a granular parameter reference.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-aanmeldquotum/ir.json`.
