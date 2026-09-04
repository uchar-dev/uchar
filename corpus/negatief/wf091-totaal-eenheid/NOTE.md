Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Aggregation`/TOTAAL arm, unit facet: a total over two `EUR/jr` operands,
annotated `EUR/jr`. §4.20 makes the total INTEGRATE a rate over time, so
the result drops the `/tijdseenheid` and keeps the numerator: the
derivation is `EUR`, and repeating the rate is the lie.

    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: the TOTAAL
    aggregation is typed 'EUR' of system 'Munt' (factor 1 to 'EUR') per 'jr'
    of system 'Tijd' (factor 1 to 'jr'), but the type derived from its first
    operand is 'EUR' of system 'Munt' (factor 1 to 'EUR')

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

The denominator strip is a **measured four-source divergence**, recorded at
the card and not resolved by it: the RegelSpraak frontend and §4.20 strip
the denominator, while the ASP translator's `eff_unit` and the Drools
backend's `EffectieveEenheid` both hand the first operand's unit through
whole. The verifier follows the two sources that state a TOTAAL rule; the
two executor-side derivations state none for the integration and are a
consumer-side question, not a well-formedness one. Every `TOTAAL` in the
corpus agrees with the emitter (7 golden, 5 negatief, measured), so nothing
observable turns on it today.

Falsifiable: drop `unit` and this case turns CLEAN — the derived arms
carry no backstop since the sign round, so the unit facet alone carries
this refusal (measured); remove the denominator strip from the `Totaal` branch and this
case turns **clean** while every other aggregation case stays red — the
sharpest possible pin on the strip itself.
