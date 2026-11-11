Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the sum domain `bezwaar`
declares a constructor `Ingediend` whose payload is the sum domain
`beschikking`, and that payload's inline copy names its third constructor
`Uitgesteld` where the declaration has `Aangehouden`. A payload inside
`domains` is a use site of another domain's declaration (WF-060 admits a
declared sum type there). The arm this case pins: the payload position.

A use site must repeat the declaration whole (same constructors, same
payloads, same order), at every depth of a datatype.

A minimal hand-built document.
