Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a **composite** position,
`Aggregation`/MINIMUM arm, decimals facet: a minimum over operands
declared at 4 and 2 decimals, in that order, annotated 4. ch. 2.3 takes a
min/max at the **least** operand precision — the winner may be either
operand, so only the floor is knowable — and claiming the higher one
over-reports a precision the value need not have.

    WF-091 FAIL /rules/resolvedRegels/0/versions/0/value/type: decimals
    mismatch: the MINIMUM aggregation is typed 4 decimals, but the type derived
    from its operands is 2 decimals

Why it has to exist. A composite type is not a copy of one declaration
but a **derivation** over its operands, so until this card nothing crossed
it — WF-091's honesty note 4 said so in as many words, and the four rules
that come nearest all stop short: WF-035 crosses operand *convertibility*
(and `kg`/`t` are convertible by construction), WF-023 crosses the value
against the target for *compatibility*, WF-007 crosses the precision
*form* and not its value, and WF-089/WF-090 reach a fold node on the
domain facet alone. Measured before the law landed: this document is
verifier-clean.

This pins the one aggregation formula that is not a pass-through: the least
operand precision, the conjunction of wholeness, operand 0's sign and unit —
the sentence WF-091's own clause already stated informally about
`MINIMUM`/`MAXIMUM` and nothing enforced.

Falsifiable: drop `decimals` and this case turns clean (no facet stands
behind it: the min/max arm skipped sign and residual, and since the L2 fix
round the engine has neither) while
`wf091-foldwortel-eenheid` stays red on the same arm. And the FORMULA is
pinned as sharply as the facet: replace the `min` fold over the operands
with operand 0's own precision and this case turns clean too — which is
why the operands stand in this order, the higher precision first.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it (U1).
