Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Onopgelost` marker whose
`bron` is null, beside a second marker that the document's one `WARNING`
accounts for.

    WF-113 FAIL /resolvedRegels/0/voorwaarde: Onopgelost marker whose bron is null: no diagnostic message can contain it, so it cannot be accounted for

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
both operands of the rule's comparison are replaced by EXPRESSION markers of
type `DatumInDagen` — the left one (the attribute reference `zijn vondstdag`)
with `"bron": null`, the right one (the date literal `31-01-2024`) with
`"bron": "de verjaringsgrens"` — and the comparison's own `bron` text follows
the right one. `diagnostics` carries one `WARNING` (line 8) in the frontend's
message form, quoting `'de verjaringsgrens'`, so the right marker is accounted
for and only the left one falls. A null has no text a message could contain. A
reading that turns the null into an empty string would find that string in
every message and let the report about the other marker account for this one;
the rule refuses the marker instead. The empty-string variant is
`wf113-marker-bron-leeg`.
