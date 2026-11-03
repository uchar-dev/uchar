Violates WF-056 (`spec/IR-SPEC.md`), arm `WF-056.condition-not-check`: the
gedurende condition of the pro-rata fold checks a characteristic of another
object, "gedurende de tijd dat zijn baken verlicht is", over the role `baken`
of the fact type `bakenbeheer`, not a characteristic of the subject itself.

A minimal hand-broken variant of the jutterswet-bakenmeester golden, with an
object type `Baken` that carries the temporal characteristic `verlicht` and
the fact type that links it to the Bakenmeester. Measured 2026-11-03: the
verifier before this arm passed the document.
