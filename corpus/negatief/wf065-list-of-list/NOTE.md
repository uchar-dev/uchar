Violates WF-065 (`spec/IR-SPEC.md` ch. 2.4): the `stapels` attribute is a
`ListType` whose `element` is itself a `ListType`. A list element datatype
MUST be first-order and non-list — one of `Numeriek`, day-precision `Date`,
or a declared `SumType`. Lists of lists are the recorded v1 fence.

A minimal hand-built document.
