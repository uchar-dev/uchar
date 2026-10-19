Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2)
whose source attribute is *present but blank*. The field is there, it is a
string, and it names nothing.

    WF-034 FAIL /rules/allocations/0/versions/0/sourceAttribute: sourceAttribute
    is present but empty: an attribute name that names nothing resolves against
    no declaration

**Why a second clause on the same six fields.** The sibling witnesses
(`wf034-allocation-without-source-attribute`, `-zonder-doelattribuut`) close the
*null* half: the field `null`. Every consumer of these fields — WF-025's
orderable-type check on the ordering key, WF-026's same-target comparison,
WF-050's overlap with a defaults group, and the dependency graph's read and
write edges — is written as `Some a`, which reads as "a name is there". `""`
satisfies `Some a` and is not a name, so it walks straight through the null
clause and lands in exactly the same silence one layer further on. This
witness and its two siblings (`-lege-maximumattribuut`,
`-lege-groepeerattribuut`) pin the filled half, over all six attribute-name
fields of an allocation at once: `sourceAttribute`, `targetAttribute`,
`ratioAttribute`, `maximumAttribute`, `remainderAttribute`,
`orderingAttribute`.

Absence keeps its meaning and stays legal where it carries one: an absent
`ratioAttribute`, `maximumAttribute`, `remainderAttribute` or
`orderingAttribute` is a facet switched **off**, and a `proportional` without a
`ratioAttribute` is already refused by WF-025's method-facet cross. Emptiness
is decided by the verifier's own name identity (whitespace collapsed,
lowercased, ch. 2.4), so a blank spelling is the same empty name.

**Measured, one field blanked at a time over the golden allocations** — every
position verifier-CLEAN before this clause, **0** after:

| field | positions | executors on the blanked document |
|---|---|---|
| `bronAttribuut` | 11 of 11 | both fault, loudly |
| `doelAttribuut` | 11 of 11 | both fault (`unknown attribute ""`) |
| `restantAttribuut` | 9 of 9 | both fault (`unknown attribute ""`) |
| `ratoAttribuut` | 7 of 7 | **both silent, both wrong** |
| `maximumAttribuut` | 4 of 4 | ref **silent and wrong**, clingo faults |
| `groepeerAttribuut` | 3 of 3 | **both silent, both wrong** |

This document is the loud row. Measured on it, against both cases beside
`corpus/golden/jutterswet-gelijkdeling/`:

    ref    : EVALUATION FAULT: empty source amount in equal-shares allocation
             (v0 mirror hole, ch. 4.4)
    clingo : TRANSLATION FAULT: unknown attribute "" on "Strandveiling"

Both refuse, but only at run time, and only after the verifier had already
called the document well-formed. The two sibling witnesses carry the rows
where that safety net is not there at all: on `maximumAttribuut` ref plays on
and computes a wrong number while clingo refuses — ref and clingo *disagree*
on a verifier-clean document — and on `groepeerAttribuut` both play on and
agree on the same wrong number, which is the one place the differential square
is structurally blind and only the hand-pinned anchor expectations catch the
defect.

*Measured dracht:* zero outside the three new witnesses. No allocation field
in the 153 golden, 22 red, 30 parity or 265 pre-existing negative documents
carries a blank attribute name, so this clause cannot over-reject: the verdict
and the cited rules of every one of those documents are byte-identical with
the clause on and off.

A hand-broken variant of `corpus/golden/jutterswet-gelijkdeling/ir.json` — one
field blanked, a one-field diff. The shape is schema-valid: the version's
`sourceAttribute` is `["string","null"]` on the wire and `""` is a string,
which is precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: drop `wf034_verdeling_namen` from the verifier's
check list and this case and its two `-lege-` siblings — those three and
nothing else — turn clean. Measured over all four zones: 473 documents
compared, 3 changed.

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
