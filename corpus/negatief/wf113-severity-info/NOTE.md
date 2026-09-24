Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a diagnostic with severity
`info`.

    WF-113 FAIL /diagnostics/0/severity: severity info is not a wire severity: the list is the account of what stayed unresolved, and info is tooling talk

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one `info` entry saying that the rule was read
(line 5 of that golden's `bron.rgs`). `diagnostics` is the producer's account
of what stayed unresolved in the document; progress messages are not part of
it (measured 2026-09-23: no document in the corpus or the parity zone carried
an `info` entry). The sister witness for the other refused value is
`wf113-severity-error`. Since L5 the severities are spelled `warning`, `error`
and `info` (delta B6); `expect.txt` pins the arm.
