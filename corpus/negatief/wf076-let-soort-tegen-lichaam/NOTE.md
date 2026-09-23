Violates WF-078 (`spec/IR-SPEC.md` ch. 4.7): a `BinderRef`'s serialized
`type` lies about the soort of the binder it resolves to. The `Let`
binds `b` to `de basis van de Jutter`, a `Numeriek` attribute, so `b`'s
declared datatype is `Numeriek`; the body then reads `b` with a serialized
`type {Tekst}`.

Pins the leaf-ref cross-check the card carries. WF-035 reads the same
serialized `type` but exempts additive (`PLUS`/`MIN`) operands — the escape
the adversarial hunt found: a soort-lie on a binder in an additive position
is verifier-clean, ref computes on (annotation ignored), and clingo faults
the translation, splitting the differential square with no corpus witness.
WF-078 crosses the `BinderRef.type` against the resolved binder datatype
(the same cross WF-023/WF-027 perform for a resolved `AttribuutRef`), so the
lie is refused before any executor sees it — regardless of the operator
position. Soort only: decimals and units stay uncrossed, exactly as the
`AttribuutRef` cross-check leaves them.

Old verifier: `PASS` (the binder resolves for WF-064; the lie rides through).
New verifier: `WF-078 FAIL … binder reference 'b' is typed text, but its
binder is number`.
