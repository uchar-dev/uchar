Violates WF-071 (`spec/IR-SPEC.md` ch. 2.4): the `dossier` struct type
carries a `Tekst` field (`etiket`). The admissible field datatypes are
number, day-precision date, a declared sum type, and a declared struct
type — text, boolean, enumeration, datetime and list fields are
ill-formed (a recorded, loudly extensible v1 fence).

A minimal hand-built document.
