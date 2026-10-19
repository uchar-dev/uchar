Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Unresolved` marker without
a `source`, beside a second marker that the document's one `warning` accounts
for.

    WF-113 FAIL /rules/rules/0/versions/0/condition: Unresolved marker
    without a source: no diagnostic message can contain it, so it cannot be
    accounted for

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
both operands of the rule's comparison are replaced by EXPRESSION markers of
the day-precision `Date` type — the left one (the attribute reference
`zijn vondstdag`) without a `source`, the right one (the date literal
`31-01-2024`) with `"source": {"snippet": "de verjaringsgrens"}` — and the
comparison's own source snippet follows the right one. `diagnostics` carries
one `warning` (line 8) in the frontend's message form, quoting
`'de verjaringsgrens'`, so the right marker is accounted for and only the left
one falls. A marker without a source has no text a message could contain. A
reading that turns the missing source into an empty string would find that
string in every message and let the report about the other marker account for
this one; the rule refuses the marker instead. The blank-snippet variant is
`wf113-marker-source-empty`.

**Rewritten by hand at L5 (2026-09-25), stamp: herschreven (m).** Until L5 the
marker carried `"bron": null`. Since L5 `source` is required on an
`Unresolved` marker (delta B26): the schema refuses the marker without one and
every executor refuses it at load, while the verifier's loader loads it so
that WF-113 (b) can cite it with the sentence of the null arm. The migration
refused the document; it was written by hand. `expect.txt` pins the arm.
