Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/`times` arm, unit facet: a rate times its own denominator unit:
`EUR/jr` maal `jr`. ch. 2.6's `times` join **cancels** there — "€/jr maal jr
= €" — so the product is `EUR`, but the node repeats the rate `EUR/jr`. A
consumer honouring the annotation carries a per-year denominator into a
value that no longer has one.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: unit mismatch:
    the times composite is typed 'EUR' of system 'Munt' (factor 1 to 'EUR') per
    'jr' of system 'Tijd' (factor 1 to 'jr'), but the type derived from its
    operands is 'EUR' of system 'Munt' (factor 1 to 'EUR')

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

This is the one arm with **no corpus witness at all**: measured 2026-09-04,
no `times` node in golden, rood, negatief or the parity zone carries two
unit-bearing operands, so the cancel rule is pinned by this negatief and by
nothing else. The two remaining `times` shapes stay outside the rule on
purpose — an opaque product (ch. 2.5 fixes its algebra but not its wire
spelling) and an unspellable rate factor (the shape the frontend refuses
outright) — and the `times` arm claims the unit only, never the precision.

Falsifiable: drop `unit` and this case turns clean; keep it and remove the
`Maal` branch and this case turns clean while the additive cases stay red.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it.
