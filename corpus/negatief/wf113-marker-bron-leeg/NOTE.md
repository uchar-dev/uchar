Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Onopgelost` marker whose
`bron` is the empty string, beside a second marker that the document's one
`WARNING` accounts for.

    WF-113 FAIL /resolvedRegels/0/voorwaarde: Onopgelost marker whose bron is blank: blank counts as absent, since nearly every message contains it and a message about another marker would account for this one

The document of `wf113-marker-bron-null`, with `"bron": ""` on the left marker
instead of `null`. The containment test of (b) is literal, and the empty string
is contained in every message, so without this clause the `WARNING` about
`'de verjaringsgrens'` would account for this marker as well and the document
would pass. Blank counts as absent, as it does for a named form's name
(WF-107): the verifier tests for a `bron` of only whitespace, and this witness
pins the empty case of that test.
