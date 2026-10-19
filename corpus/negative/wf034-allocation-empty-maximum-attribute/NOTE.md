Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2)
whose maximum attribute is *present but blank*.

    WF-034 FAIL /rules/allocations/0/versions/0/maximumAttribute: maximumAttribute
    is present but empty: an attribute name that names nothing resolves against
    no declaration

This is the witness for the row where **the executors disagree**. The clause
is the one described in full in the sibling NOTE
(`wf034-allocation-empty-source-attribute`): a present attribute name on any of the
allocation's six name fields is not blank, while the absence of one of the
four facet fields — `ratioAttribute`, `maximumAttribute`,
`remainderAttribute`, `orderingAttribute` — keeps its meaning of a facet
switched off and stays legal. `maximumAttribute` is the ceiling per
recipient: with a name it caps
each share, absent it does not exist. Blank, it is neither.

Measured on this document, against `corpus/golden/jutterswet-berging/case.json`:

    ref    : MISMATCH: W.bergingsaandeel = 600, expected 300
             MISMATCH: D.bergingsaandeel = 400, expected 300
             MISMATCH: B1.onverdeelde rest = 0, expected 400
    clingo : TRANSLATION FAULT: unknown attribute "" on "Berger"

`ref` does not fault. It reads the blank name as "no ceiling", distributes the
whole pot uncapped, and returns numbers that are wrong by twice the pinned
value; clingo refuses to translate the same document. So on this document the
two executors do not merely fail differently — **one plays and one refuses**,
which is a divergence inside the differential square on a document the
verifier had called well-formed. The square cannot be the fence here; the
document layer has to be.

Measured breadth for this field: **4 of 4** golden allocations that carry a
`maximumAttribuut` stayed verifier-clean when it was blanked
(`jutterswet-berging` 600 for 300, `jutterswet-buitplafond` 40 for 25,
`jutterswet-rangmaximering` 1800 for 500, `jutterswet-schaaldeling` 66.66 for
50 — every one a silent overshoot in `ref` and a translation refusal in
clingo). **0** stay clean after this clause.

*Measured dracht:* zero outside the three new witnesses — see the sibling
NOTE; no document in any zone that predates this card carries a blank
allocation attribute name.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field blanked, a one-field diff; schema-valid, since the version's
`maximumAttribute` is an optional string on the wire and `""` is a string.

Falsifiable, clause-exact: drop `wf034_verdeling_namen` from the verifier's
check list and this case and its two `-lege-` siblings — those three and
nothing else — turn clean. Measured over all four zones: 473 documents
compared, 3 changed.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then
(`maximumAttribuut` → `maximumAttribute`). `expect.txt` pins the arm.
