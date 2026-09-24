Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Onopgelost` marker with a
`WARNING` beside it that does not contain the marker's `bron` verbatim.

    WF-113 FAIL /rules/resolvedRegels/0/versions/0/condition: Onopgelost marker
    'zijn vondstdag eerder is dan de verjaringsgrens' is not accounted for: no
    diagnostic message contains its bron verbatim

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the rule's whole condition is replaced by the CONDITION marker
`{"node": "Onopgelost", "bron": "zijn vondstdag eerder is dan de verjaringsgrens"}`,
and `diagnostics` carries one `WARNING` (line 8) in the frontend's own message
form — `onopgelost: <reason> — '<bron>' blijft als marker op de wire staan` —
whose quoted bron differs from the marker's in ONE letter:
`… dan de Verjaringsgrens`. So there is a diagnostic, it is a WARNING, it is
about this marker, and it still does not account for it: the link between a
marker and its report is the verbatim `bron`, and a case-insensitive or
"any diagnostic will do" reading of the rule frees this document. The
expression-marker variant with an empty list is `wf113-marker-zonder-verslag`.
