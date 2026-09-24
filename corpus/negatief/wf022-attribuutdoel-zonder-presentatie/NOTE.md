Violates WF-022 (`spec/IR-SPEC.md` ch. 3.1), the member-kind arm in its second
direction: a rule version carries neither a `presentation` tag nor
`initialisation` true, and its target `meldloon` is an attribute of `Vondst`.

    WF-022 FAIL /rules/resolvedRegels/0/versions/0/target: a version without a
    presentation and without initialisation attributes a characteristic, so its
    target MUST name one, and 'meldloon' is an attribute of 'Vondst'

**Why the absence of both fields is a kind.** Since L4 the wire writes no rule
kind (delta B20). A version without a `presentation` tag and with
`initialisation` false is read as a characteristic attribution — the reading
the v0 kind `KENMERKTOEKENNING` had — and a characteristic attribution writes
a characteristic. Without this arm such a version on an attribute target is
read as attributing a characteristic of the attribute's name, which the
declaration does not have.

**The document.** `corpus/golden/jutterswet-meldloon/ir.json` with one field
removed from the first rule's version: `"presentation": "BEREKENING"`.

Falsifiable, arm-exact: emptying the arm `true, false when not computes` of the
member match in `wf022` of `spec/verifier/lib/checks.ml` turns this case clean
and nothing else — 698 documents compared over golden, rood and negatief, one
verdict moved. The sister witness `wf022-kenmerkdoel-met-presentatie` pins the
other direction.
