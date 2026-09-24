Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a diagnostic with severity
`error`.

    WF-113 FAIL /diagnostics/0/severity: severity error is not a wire severity: an error means the producer refused to write the document, so a document that was written carries none

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one entry in `diagnostics` — severity `error`, a
message that claims the rule was not translated, line 8 of that golden's
`bron.rgs`. The document itself contradicts the message: the rule is there,
translated. That is the reason the wire has no `error`: a producer that meets
an error does not write the document at all, so an `error` inside a written
one is either stale or false. `spec/schema/uchar-ir-v1.schema.json` admits only
`warning`; the verifier's loader still reads `error` and `info`, so that this
rule refuses them by name instead of the load failing without one, and every
executor refuses them at load with the same sentence. Since L5 the severities
are spelled `warning`, `error` and `info` (delta B6); `expect.txt` pins the
arm.
