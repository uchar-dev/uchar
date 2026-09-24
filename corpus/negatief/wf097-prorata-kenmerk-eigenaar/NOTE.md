Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries
an `eigenaar`. The clip reads the subject's own characteristic timeline over the whole
span it ranges over, so the owner names a subject no reader consults.

This is the **pro-rata fold (ch. 3.7, the WF-056 clip)** — one of the three clips WF-097 fences. The rule lives in
ONE shared helper per artifact, reached from two or three call sites depending on
how that artifact discriminates the clips: this witness is what stops being
rejected if the reach to THIS clip is lost — in the verifier that means narrowing
the kind-keyed branch that covers both folds, elsewhere dropping the call
outright.

A minimal hand-broken variant of
`corpus/golden/anchor/jutterswet-borgdeel/ir.json`: one field on one node —
`resolvedRegels[0].versions[0].value.filter.eigenaar` goes from `null` to
`"Jutter"` — a DECLARED type, so WF-034's owner arm (which only asks whether
the name resolves) stays silent.

Before the rule reached this clip the document was verifier-clean and both
executors replayed every one of the anchor's cases with the untwisted answers.
