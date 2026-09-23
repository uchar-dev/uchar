Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Rounding` arm, decimals facet: a rounding to **0** decimals whose `type`
still claims the base's 4. ch. 2.4 clause 2 is explicit — "a rounding
changes the *facing* precision: `afgerond op n` makes the expression
n-decimal" — and it is exactly that facing precision the assignment gate
reads. A rounding that claims not to have rounded is the ch. 2.4 gate's
own blind spot.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: decimals mismatch: the
    rounding is typed 4 decimals, but the type derived from its base and its
    decimal count is 0 decimals

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

The rounding is the one covered position whose derivation reads a NODE field
(`decimals`) beside its operand: the base supplies soort, unit and sign, the
node's own count supplies the precision.

Falsifiable: drop `decimals` and this case degrades to a `wholeness
mismatch` — still WF-091, still red (measured: the narrowed rounding type
disagrees in wholeness too, and that facet catches it); remove the `Afgerond`
branch and it turns clean together with `wf091-afgerond-eenheid`.
