Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a diagnostic with severity
`ERROR`.

    WF-113 FAIL /diagnostics/0/severity: severity error is not a wire severity: an error means the producer refused to write the document, so a document that was written carries none

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one entry in `diagnostics` — severity `ERROR`, a
message that claims the rule was not translated, line 8 of that golden's
`bron.rgs`. The document itself contradicts the message: the rule is there,
translated. That is the reason the wire has no ERROR: a producer that meets an
error does not write the document at all, so an ERROR inside a written one is
either stale or false. `spec/schema/uchar-ir-v1.schema.json` admits only
`WARNING`; the verifier's loader still reads `ERROR` and `INFO`, so that this
rule refuses them by name instead of the load failing without one.
