Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Aggregation`/AANTAL arm, decimals facet: a count annotated at 2 decimals.
ch. 2.3 makes a count a whole, non-negative, unitless number, and unlike
every other arm here that derivation reads no operand at all — a count's
type is a constant of the operator.

    WF-091 FAIL /resolvedRegels/0/waarde/type: decimals mismatch: the AANTAL
    aggregation is typed 2 decimals, but the type a count derives is
    0 decimals

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

The SIGN is deliberately not crossed here, and the reason is measured, not
assumed: two anchor goldens annotate a count `ONBEPAALD` where the
derivation says `NIET_NEGATIEF`, and a derivation that can only under-claim
a sign restriction may not refuse a document that narrows it truthfully.
That is the same reason the sign facet is skipped at every derived position
in the rule.

Falsifiable: drop `decimals` and this case turns clean (the count arm skips
sign and residual) while every other case stays red; remove the `Aantal`
branch and only this case turns clean.
