Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Aggregation`/SOM arm, unit facet: a sum over a `kg` attribute and a `kg`
literal, annotated `t`. §4.6 *Eenheid1* gives a value aggregation its
first operand's type, so the derivation is `kg`.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: unit mismatch:
    the sum aggregation is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the type derived from its first operand is 'kg' of system 'Gewicht'
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

Distinct from `wf091-foldbron-eenheid`, which lies at the fold SOURCE (an
attribute reference inside the fold, a *reference* position) while the fold
root stayed honest. Here the source is honest and the ROOT lies — the half
no reference arm can reach.

Falsifiable: drop `unit` and this case turns CLEAN — the derived arms carry no
backstop since the sign round, so the unit facet alone carries this refusal
(measured); remove `Som` from the aggregation branch and it turns clean while
the MAXIMUM, TOTAAL, MINIMUM and AANTAL cases stay red.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it.
