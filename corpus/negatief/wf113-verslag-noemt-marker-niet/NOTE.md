Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Unresolved` marker with a
`warning` beside it that does not contain the marker's source snippet
verbatim.

    WF-113 FAIL /rules/rules/0/versions/0/condition: Unresolved marker
    'zijn vondstdag eerder is dan de verjaringsgrens' is not accounted for: no
    diagnostic message contains its source snippet verbatim

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the rule's whole condition is replaced by the CONDITION marker
`{"node": "Unresolved", "source": {"snippet": "zijn vondstdag eerder is dan de
verjaringsgrens"}}`, and `diagnostics` carries one `warning` (line 8) in the
frontend's own message
form — `onopgelost: <reason> — '<bron>' blijft als marker op de wire staan` —
whose quoted snippet differs from the marker's in ONE letter:
`… dan de Verjaringsgrens`. So there is a diagnostic, it is a `warning`, it is
about this marker, and it still does not account for it: the link between a
marker and its report is the verbatim source snippet, and a case-insensitive or
"any diagnostic will do" reading of the rule frees this document. The
expression-marker variant with an empty list is `wf113-marker-zonder-verslag`.

Since L5 the marker is an `Unresolved` and its text sits in `source.snippet`
(delta B26), which the rule reads; `expect.txt` pins the arm.
