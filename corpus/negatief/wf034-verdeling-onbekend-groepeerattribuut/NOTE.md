Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
ordering key names an attribute *no declaration carries*.

    WF-034 FAIL /rules/verdelingen/0/groepeerAttribuut: 'bestaatniet' is not a
    declared attribute of the target type 'Jutter': the allocation's
    groepeerAttribuut resolves against no declaration

The clause is the one described in full in the sibling NOTE
(`wf034-verdeling-onbekend-ratoattribuut`): every filled attribute name on the
allocation's six name fields resolves against the declaration scope of its own
side — the source-side pair against `bronType`, the four target-side fields
against `doelType`. `groepeerAttribuut` is the key an `OP_VOLGORDE` allocation
orders recipients by: it decides *who gets served first*.

The second **silent, agreeing** witness, and the sharper one, because here the
executors do not merely underive — they compute a whole distribution and it is
the wrong one. Measured on this document, against
`corpus/golden/jutterswet-rangaflopend/case.json`:

    ref    : MISMATCH: J1.sluitdeel = 300, expected 1200
             MISMATCH: J2.sluitdeel = 300, expected 0
             MISMATCH: J3.sluitdeel = 300, expected 0
             MISMATCH: J4.sluitdeel = 300, expected 0
    clingo : MISMATCH: J1.sluitdeel = [300], expected 1200
             MISMATCH: J2.sluitdeel = [300], expected 0
             MISMATCH: J3.sluitdeel = [300], expected 0
             MISMATCH: J4.sluitdeel = [300], expected 0

Neither faults. The key resolves to nothing, the ordering collapses, and the
pot is spread evenly over all four recipients instead of being paid out in
rank order — **the same wrong distribution in both executors**. This is the
case that shows why parity between backends is never accepted as truth by
itself. Take the pins away — replay a case the corpus has not pinned
absolutely — and the defect is invisible to the whole differential square.

Measured breadth for this field: **3 of 3** golden allocations that carry a
`groepeerAttribuut` stayed verifier-clean when it was pointed at an undeclared
name (`jutterswet-rangaflopend`, `jutterswet-rangdeling`,
`jutterswet-rangmaximering`). **0** stay clean after the clause.

*Measured breadth of the shape itself:* zero outside the six `-onbekend-`
allocation witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-rangaflopend/ir.json` — one
field repointed, a one-line diff; schema-valid, since
`ResolvedVerdeling.groepeerAttribuut` is `["string","null"]` on the v0 wire.

Falsifiable, clause-exact: drop `wf034_verdeling_naamresolutie` from the
verifier's check list and this case and its five `-onbekend-` allocation
siblings, those six and nothing else, turn clean.
