Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Aggregation`/MAXIMUM arm, unit facet: the fold takes the larger of
`Jutter.gewicht` (declared `kg`) and a `kg` literal, so §4.6 *Eenheid1*
derives `kg` — the winning contributor's unit IS the node's unit — but the
fold root is annotated `t` (same canonical, factor 1000).

    WF-091 FAIL /rules/resolvedRegels/0/versions/0/value/type: unit mismatch:
    the MAXIMUM aggregation is typed 't' of system 'Gewicht' (factor 1000 to
    'kg'), but the type derived from its operands is 'kg' of system 'Gewicht'
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

This is honesty note 4's own counter-shape, the one the note recorded as
**pinned by no artifact**: a `MAXIMUM` over a `kg` attribute and a `kg`
literal, its own `type` annotated `t`, where `ref` answers `3,75` from the
declarations and a consumer honouring the node's annotation answers
`3.750.000`. The note also explained why a GOLDEN cannot pin it — the ASP
backend refuses a multi-operand `MINIMUM`/`MAXIMUM` ahead of translation,
so the shape can carry no pinned expectations — which is exactly why the
artifact that closes it is a negatief.

Falsifiable: drop `unit` from the facet list and this case turns clean (no facet
stands behind the unit facet: the min/max arm skipped sign and residual,
and since the L2 fix round the engine has neither); keep the facet and remove `Minimum | Maximum` from
`composite_expectation` and this case turns clean while `wf091-som-eenheid`
and `wf091-totaal-eenheid` stay red.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it (U1).
