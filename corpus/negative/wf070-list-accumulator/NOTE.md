Violates WF-070 (`spec/IR-SPEC.md` ch. 4.8): the `Fold`'s accumulator is
list-typed — its `type`, `init` and accumulator parameter are all
`ListType`. A fold accumulator MUST be a first-order non-list datatype
(scalar or `SumType`); a list-typed accumulator is the ASP term-depth fence
of the ratified design.

A minimal hand-built document; the source and element parameter are
well-formed, so the accumulator shape is the sole violation.
