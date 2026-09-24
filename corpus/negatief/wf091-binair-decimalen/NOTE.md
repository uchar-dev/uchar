Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/PLUS arm, decimals facet: the operands are declared at 2 and 4
decimals, so ch. 2.3 derives **maxDec** = 4 for the sum, but the node
claims 2. An **under-report**, and the direction matters: the ch. 2.4
assignment gate reads the value's declared precision, so a sum that claims
fewer decimals than it has walks into a narrower target without the
explicit rounding the gate exists to demand.

    WF-091 FAIL /rules/resolvedRegels/0/versions/0/value/type: decimals
    mismatch: the PLUS composite is typed 2 decimals, but the type derived from
    its operands is 4 decimals

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

Falsifiable: drop `decimals` from the facet list and this case turns clean
while `wf091-binair-eenheid` and `wf091-binair-geheel` stay red on the same
arm.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it.
