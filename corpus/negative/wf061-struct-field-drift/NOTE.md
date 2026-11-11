Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the struct domain `dossier`
declares a field `uitkomst` of the sum domain `beschikking`, and that field's
inline copy names its third constructor `Uitgesteld` where the declaration
has `Aangehouden`. A field inside `domains` is a use site of another
domain's declaration (WF-071 admits a declared sum type there). The arm this
case pins: the struct-field position.

A use site must repeat the declaration whole (same constructors, same
payloads, same order), at every depth of a datatype.

A minimal hand-built document.
