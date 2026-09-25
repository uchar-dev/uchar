Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries a `host`.
The clip reads the subject's own characteristic timeline over the whole span it
ranges over, so the host names a subject no reader consults.

This is the **day count (ch. 3.8, the WF-058 clip)** — one of the three clips WF-097 fences. The rule lives in
ONE shared helper per artifact, reached from two or three call sites depending on
how that artifact discriminates the clips: this witness is what stops being
rejected if the reach to THIS clip is lost — in the verifier that means narrowing
the kind-keyed branch that covers both folds, elsewhere dropping the call
outright.

A minimal hand-broken variant of
`corpus/golden/anchor/jutterswet-dagentelling/ir.json`: one field on one node
— `rules[0].versions[0].value.filter.host` goes from absent to `"Jutter"` — a
DECLARED type, so WF-034's host arm stays silent. Since L5 the field is `host`
(delta B10); the report names it, and `expect.txt` pins the arm.

Before the rule reached this clip the document was verifier-clean and both
executors replayed every one of the anchor's cases with the untwisted answers.
