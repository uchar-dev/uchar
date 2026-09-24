Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2)
whose ordering key is *present but blank*.

    WF-034 FAIL /rules/verdelingen/0/versions/0/orderingAttribute: orderingAttribute
    is present but empty: an attribute name that names nothing resolves against
    no declaration

This is the witness for the worst row: **both executors play on, agree, and
are both wrong**. The clause is the one described in full in the sibling NOTE
(`wf034-verdeling-lege-bronattribuut`): a present attribute name on any of the
allocation's six name fields is not blank, while an absent facet field
keeps its meaning of a facet switched off. `orderingAttribute` is the key an
`OP_VOLGORDE` allocation orders recipients by; absent, WF-025 already
forbids it on any other method, so on an ordered allocation it is the field
that decides *who gets served first*.

Measured on this document, against
`corpus/golden/jutterswet-rangaflopend/case.json`:

    ref    : MISMATCH: J1.sluitdeel = 300, expected 1200
             MISMATCH: J2.sluitdeel = 300, expected 0
             MISMATCH: J3.sluitdeel = 300, expected 0
             MISMATCH: J4.sluitdeel = 300, expected 0
    clingo : MISMATCH: J1.sluitdeel = [300], expected 1200
             MISMATCH: J2.sluitdeel = [300], expected 0
             MISMATCH: J3.sluitdeel = [300], expected 0
             MISMATCH: J4.sluitdeel = [300], expected 0

Neither faults. The blank key collapses the ordering, the pot is spread evenly
over all four recipients instead of being paid out in rank order, and **both
executors produce the same wrong distribution**. This is the case that shows
why parity between backends is never accepted as truth by itself: agreement
here is agreement on a defect, and only the hand-pinned expectations of the
golden this document was broken from catch it. Take the pins away — replay a
case the corpus has not pinned absolutely — and the defect is invisible to the
whole differential square.

Measured breadth for this field: **3 of 3** golden allocations that carry a
`groepeerAttribuut` stayed verifier-clean when it was blanked
(`jutterswet-rangaflopend` 300 for 1200, `jutterswet-rangdeling` 25 for 50,
`jutterswet-rangmaximering` 360 for 500), each silently wrong in both
executors. **0** stay clean after this clause. The same silence, in both
executors, was measured on `ratoAttribuut` (7 of 7, the shares stay underived
where the anchor pins 60).

*Measured dracht:* zero outside the three new witnesses — see the sibling
NOTE; no document in any zone that predates this card carries a blank
allocation attribute name.

A hand-broken variant of `corpus/golden/jutterswet-rangaflopend/ir.json` — one
field blanked, a one-field diff; schema-valid, since the version's
`orderingAttribute` is an optional string on the wire and `""` is a string.

Falsifiable, clause-exact: drop `wf034_verdeling_namen` from the verifier's
check list and this case and its two `-lege-` siblings — those three and
nothing else — turn clean. Measured over all four zones: 473 documents
compared, 3 changed.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`ratoAttribuut`
→ `ratioAttribute`, `groepeerAttribuut` → `orderingAttribute`). `expect.txt`
pins the arm.
