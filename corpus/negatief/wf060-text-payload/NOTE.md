Violates WF-060 (`spec/IR-SPEC.md` ch. 2.4): the `Toegekend` constructor
carries a `Tekst` payload. Admissible payloads are `Number`,
day-precision `Date`, or a declared `SumType` only — text, boolean,
enumeration and millisecond-date payloads are the recorded v1 fence.

A minimal hand-built document — the shape of the intended
`corpus/golden/anchor/jutterswet-beschikkingsom` declaration.
