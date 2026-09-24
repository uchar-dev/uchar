Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
proportional weight names an attribute *no declaration carries*.

    WF-034 FAIL /rules/verdelingen/0/versions/0/ratioAttribute: 'bestaatniet' is
    not a declared attribute of the target type 'Berger': the allocation's
    ratioAttribute resolves against no declaration

The clause: every filled attribute name on the allocation's six name fields
resolves against the declaration scope of **its own side**, the side both
executors read it on — `sourceAttribute` and `remainderAttribute` against
`sourceType` (the pot that is distributed, and the remainder written back on
the source), `targetAttribute`, `ratioAttribute`, `maximumAttribute` and
`orderingAttribute` against `targetType` (the share written on each
recipient, and the three facets read per recipient). The two earlier arms of this rule
ask whether the field is there and whether it is more than blanks; this one
asks the question the rule is about — does the name resolve?

This is the witness for the **silent, agreeing** row. Measured on this
document, against `corpus/golden/anchor/jutterswet-bergingstoezicht/case.json`:

    ref    : MISMATCH: J1.bergingsaandeel = leeg, expected 60
             MISMATCH: J2.bergingsaandeel = leeg, expected 40
             MISMATCH: expected consistent, but the channel carries:
                       verdeel de opbrengst van een berging × J2;
                       verdeel de opbrengst van een berging × J1
    clingo : MISMATCH: J1.bergingsaandeel is underived (leeg), expected 60
             MISMATCH: J2.bergingsaandeel is underived (leeg), expected 40
             MISMATCH: expected consistent, but the model carries 2
                       inconsistent fact(s)

Neither faults. Every recipient's weight reads as *leeg*, the proportional
allocation has nothing to divide by, the shares stay underived, and both
executors deposit the same inconsistency and answer the same nothing. This is
an **anchor** golden — hand-written, absolute expected values — and the
pinning is the only thing that catches it: backend agreement here is agreement
on a defect.

Measured breadth for this field: **7 of 7** golden allocations that carry a
`ratoAttribuut` stayed verifier-clean when it was pointed at an undeclared
name, each silently wrong in both executors. **0** stay clean after the
clause. The same silence was measured on `groepeerAttribuut` (3 of 3, its own
witness) and half of it on `maximumAttribuut` (4 of 4, `ref` silent while
clingo refuses — the divergent witness). The loud rows — `bronAttribuut` 11 of
11, `doelAttribuut` 11 of 11, `restantAttribuut` 9 of 9 — were equally clean
in the verifier, are equally closed, and since the fix round each carries its
own witness too: one document per sub-arm, six in all.

*Measured breadth of the shape itself:* zero outside the six `-onbekend-`
allocation witnesses. Over all 486 documents of golden + rood + negatief +
parity the card and its fix round change exactly ten verdicts (their ten
witnesses) and five further reports that keep their verdict — four
pre-existing allocation negatiefs and the parity casus, which gains six
truthful incidental lines, one per allocation.

A hand-broken variant of `corpus/golden/anchor/jutterswet-bergingstoezicht/`
— one field repointed, a one-line diff; schema-valid, since the version's
`ratioAttribute` is an optional string on the wire.

Falsifiable, clause-exact: drop `wf034_verdeling_naamresolutie` from the
verifier's check list — its own function beside the expression walk, because
an allocation is not an expression — and this case and its five
`-onbekend-` allocation siblings, those six and nothing else, turn clean.
Measured over all four zones: 486 documents compared, 7 reports change, 6
verdicts.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`bronAttribuut`
→ `sourceAttribute`, `doelAttribuut` → `targetAttribute`, `ratoAttribuut` →
`ratioAttribute`, `maximumAttribuut` → `maximumAttribute`, `restantAttribuut`
→ `remainderAttribute`, `groepeerAttribuut` → `orderingAttribute`).
`expect.txt` pins the arm.
