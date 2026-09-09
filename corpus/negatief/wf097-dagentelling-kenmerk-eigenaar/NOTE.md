Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries
an `eigenaar`. The clip reads the subject's own characteristic timeline over the whole
span it ranges over, so the owner names a subject no reader consults.

This is the **day count (ch. 3.8, the WF-058 clip)** — one of the three clips WF-097 fences. The rule is one
shared helper called from three sites in each of the verifier, the reference
interpreter and the ASP backend; this witness is what fails if that clip's call
site is dropped.

A minimal hand-broken variant of
`corpus/golden/anchor/jutterswet-dagentelling/ir.json`: one field on one node —
`resolvedRegels[0].waarde.filter.eigenaar` goes from `null` to `"Jutter"` — a DECLARED type, so WF-034's owner arm stays silent.

Before the rule reached this clip the document was verifier-clean and both
executors replayed every one of the anchor's cases with the untwisted answers.
