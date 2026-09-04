Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/PLUS arm, wholeness facet: a whole operand plus an unbounded
(`getal`) one: ch. 2.3 makes the sum whole **iff both** operands are, so
the derivation is fractional and the node's `whole` claim is a lie. Both
sides carry 0 decimals, so WF-007's precision-FORM check passes on either
spelling — the wholeness flag is the only difference, and it is what gates
the §5.13 elfproef and the date builtins downstream.

    WF-091 FAIL /resolvedRegels/0/waarde/type: wholeness mismatch: the PLUS
    composite is typed whole, but the type derived from its operands is
    fractional

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

Falsifiable: drop `wholeness` from the facet list and this case turns clean
while the unit, decimals and soort cases on the same arm stay red.
