Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
proportional weight names an attribute *no declaration carries*.

    WF-034 FAIL /verdelingen/0/ratoAttribuut: 'bestaatniet' is not a declared
    attribute of the target type 'Berger': the allocation's ratoAttribuut
    resolves against no declaration

The clause: every filled attribute name on the allocation's six name fields
resolves against the declaration scope of **its own side**, the side both
executors read it on — `bronAttribuut` and `restantAttribuut` against
`bronType` (the pot that is distributed, and the remainder written back on the
source), `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut` and
`groepeerAttribuut` against `doelType` (the share written on each recipient,
and the three facets read per recipient). The two earlier arms of this rule
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
in the verifier and are equally closed.

*Measured breadth of the shape itself:* zero outside the four new
`-onbekend-` witnesses. Over all 483 documents of golden + rood + negatief +
parity the clause changes exactly seven verdicts (the seven witnesses this
card adds) and five further reports that keep their verdict — four
pre-existing allocation negatiefs and one parity fixture gaining a truthful
incidental line.

A hand-broken variant of `corpus/golden/anchor/jutterswet-bergingstoezicht/`
— one field repointed, a one-line diff; schema-valid, since
`ResolvedVerdeling.ratoAttribuut` is `["string","null"]` on the v0 wire.

Falsifiable, clause-exact: drop `wf034_verdeling_naamresolutie` from the
verifier's check list — its own function beside the expression walk, because
an allocation is not an expression — and this case and its three
`-onbekend-` allocation siblings, those four and nothing else, turn clean.
Measured over all four zones: 483 documents compared, 5 reports change, 4
verdicts.
