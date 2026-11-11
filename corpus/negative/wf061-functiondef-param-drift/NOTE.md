Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the function
`beschikkingsweging` declares a parameter `b` of the sum domain
`beschikking` whose inline copy names its third constructor `Uitgesteld`
where the declaration has `Aangehouden`. The arm this case pins: a
`FunctionDef` parameter. The body is a bare literal, so no other rule
crosses the parameter.

A use site must repeat the declaration whole (same constructors, same
payloads, same order), at every depth of a datatype.

A minimal hand-built document.
