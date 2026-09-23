Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Clamp` arm, unit facet: a clamp over a `kg` base, annotated `t`. ch. 2.7
makes a clamp return one of three values — the base, the minimum or the
maximum — all of the base's own type, so the clamp derives its base's type
whole.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: the clamp is
    typed 't' of system 'Gewicht' (factor 1000 to 'kg'), but the type derived
    from its base is 'kg' of system 'Gewicht' (factor 1 to 'kg')

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

Falsifiable: drop `unit` and this case turns CLEAN — the derived arms
carry no backstop since the sign round, so the unit facet alone carries
this refusal (measured); remove
the `Begrensd` branch and it turns clean while every other case stays red.
