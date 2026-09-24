Violates WF-041 (`spec/IR-SPEC.md` ch. 4.6): the quantifier fold's `kwantor` is
`TEN_MINSTE` and its `drempel` is 0. A counting quantifier's threshold is at
least 1, and since 2026-09-16 the rule is checked at BOTH hosts of that
vocabulary — the propositional `Compound` (`wf041-threshold-zero`, the sibling
directory) and the first-order `QuantifierFold` here.

The number, not the shape, is why it carries WF-041 and not WF-108. WF-108 (e)
owns the field's PRESENCE on this node — present exactly under a counting
quantifier — and hands the VALUE to the rule that already governs it, exactly as
arm (c) hands an unresolvable range binding to WF-047 rather than renaming it.
One demand on one vocabulary, one number, wherever it is written.

What the bound refuses is the same in both places. A threshold of 0 under
*ten minste* asks nothing of the set at all, so the fold would be true of every
subject whatever the matrix says; under *ten hoogste* and *precies* it is `GEEN`
spelled a second way, and admitting it would put two quantifiers on the wire for
one reading — which is precisely the "one node, two spellings" defect this
node's `kwantor` field exists to prevent.

The schema also refuses this document (`drempel` has `minimum: 1`), so it is
deliberately schema-invalid as well. The negatief zone is gate 3's, not gate
2's: a rule whose only enforcement were the schema would have no citable
violation.

Verifier: `WF-041 FAIL /rules/resolvedRegels/0/versions/0/condition/drempel: counting quantifier
with threshold 0 < 1`.
