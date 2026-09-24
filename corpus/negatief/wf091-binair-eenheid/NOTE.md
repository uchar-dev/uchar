Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Binary`/PLUS arm, unit facet: both operands read `Jutter.gewicht`,
declared in `kg` (canonical `kg`, factor 1), so ch. 2.6 derives `kg` for
the sum — the result carries the LEFT unit — but the node's own `type`
carries `t` (the same canonical, factor 1000). Every other crossed facet
agrees, so only the unit differs, and the two units are **convertible**,
which is what puts the lie beyond WF-035's and WF-023's reach.

    WF-091 FAIL /rules/resolvedRegels/0/versions/0/value/type: unit mismatch:
    the PLUS composite is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the type derived from its operands is 'kg' of system 'Gewicht' (factor 1
    to 'kg')

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

This is the shape the card names the 550.000 form: `ref` and the ASP
translator answer from the declarations, a consumer honouring the node's
own annotation rescales by the factor of 1000, and before this rule
nothing refused the document.

Falsifiable, facet-exact and arm-exact: drop `unit` from the facet list in
`use_site_identity` and this case turns **clean** — no facet stands behind
the unit facet (measured again in the L2 fix round, 2026-09-24). Until that
round the engine had a residual backstop, which the derived arm skipped for
a formula that does not claim every facet; it was removed with the sign
facet, since no caller reached either. Disable the additive branch of
`composite_expectation` and this case turns clean together with the other
three `wf091-binair-*` cases while the rounding, clamp and aggregation
cases stay red.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it (U1).
