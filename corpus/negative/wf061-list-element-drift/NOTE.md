Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the attribute `eerdere
beschikkingen` is a list whose element is the sum domain `beschikking`, and
that element's inline copy names its third constructor `Uitgesteld` where the
declaration has `Aangehouden`. The arm this case pins: a `ListType` element.
The attribute `beschikking` repeats the declaration faithfully.

A use site must repeat the declaration whole (same constructors, same
payloads, same order), at every depth of a datatype.

A minimal hand-built document.
