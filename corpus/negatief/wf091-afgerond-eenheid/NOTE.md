Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Rounding` arm, unit facet: a rounding to 2 decimals over a `kg` base,
annotated `t`. A rounding changes the precision and **nothing else** (ch.
2.4, ch. 2.10: it carries its basis's unit), so the derived unit is the
base's.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: the rounding is
    typed 't' of system 'Gewicht' (factor 1000 to 'kg'), but the type derived
    from its base and its decimal count is 'kg' of system 'Gewicht'
    (factor 1 to 'kg')

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

Falsifiable: drop `unit` and this case turns CLEAN — since the sign
round removed the backstop from every derived arm, the unit facet alone
carries this refusal (measured; the stronger, facet-exact pin); remove the `Afgerond`
branch and it turns clean with `wf091-afgerond-decimalen`.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it (U1).
