Violates WF-065 (`spec/IR-SPEC.md` ch. 2.4): the `reeksen` attribute is
`ListType`-typed and carries a granularity (`year`) — a list-typed temporal
attribute. A list-typed attribute MUST be scalar-carried; a granularity
declaration on it is the recorded v1 fence.

A minimal hand-built document.
