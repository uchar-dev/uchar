Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/PLUS arm, soort facet: two plain numbers are summed and the
result is annotated a **percentage**. ch. 2.3's additive row derives a
number from two numbers; the soort is what routes a consumer's scaling, so
a percentage annotation over a plain sum is a factor-of-100 divergence
waiting at the first consumer that honours it.

    WF-091 FAIL /resolvedRegels/0/waarde/type: soort mismatch: the PLUS
    composite is typed percentage, but the type derived from its operands is
    number

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

Note what is *not* claimed here: the case pins "the sum of two numbers is a
number", never "the sum of two percentages is a number" — the frontend's
matrix does say the latter, but no document in any zone carries a
percentage-sided additive node (measured, four zones), so the rule states
nothing about a shape nothing exercises.

Falsifiable: drop `soort` from the facet list and this case turns clean
while the other three `wf091-binair-*` cases stay red.
