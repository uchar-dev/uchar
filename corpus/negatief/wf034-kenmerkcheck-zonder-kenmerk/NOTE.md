Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **characteristic check**
(ch. 4.5) that names no characteristic. `kenmerk` is the only field the check
resolves through, so an empty one resolves against no declaration at all.

    WF-034 FAIL /rules/resolvedRegels/1/voorwaarde: characteristic check without a
    characteristic name: it resolves against no declaration

Why nothing else stood in for it: every arm that reads this node sits behind
`Some k` — WF-034's own declared-characteristic cross, WF-043's granularity
cross for a whole-period qualifier, WF-053's navigated hop, and the
dependency graph's edge from the reading rule to the characteristic. One
empty field silenced all four at once. The node kind is not a corner either:
the goldens carry **96** characteristic-check positions in twelve distinct
shapes over five top-level sections (a rule condition and its conjunction
members, a filter inside a rule value, an object-creation and a fact-creation
condition, a consistency assertion, and conditions at every depth inside a
prioritized-defaults group, exception members included). **89** of those 96
stayed verifier-clean when emptied one at a time; **0** do after this clause.
The seven that were already refused are refused for other reasons and are
untouched.

Measured before the closure, on this document against
`corpus/golden/jutterswet-keurmerk/case.json`:

    ref    : EVALUATION FAULT: missing KenmerkCheck.kenmerk in IR node
    clingo : TRANSLATION FAULT: missing kenmerk

Both executors refuse — loudly, but each in its own words and only at run
time, on a document the verifier had already called well-formed. That is the
whole argument for the rule: "this reference resolves" is a document-layer
property, not a discovery two evaluators are supposed to make separately.

*Measured dracht:* zero outside this witness — no `Kenmerk` node with a null
`kenmerk` occurs in the 153 golden + 22 rood + 30 parity documents or in the
257 negatief cases that predate this card. Over-rejection is impossible by
measurement, and the verifier's verdict over all 462 of those documents is
unchanged by this clause.

A hand-broken variant of `corpus/golden/jutterswet-keurmerk/ir.json` — one
field emptied, a one-field diff. The shape is schema-valid: `Kenmerk.kenmerk`
is `["string","null"]` on the v0 wire, like nearly every reference field,
which is precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: remove the `kenmerk = None` arm from `walk_cond`'s
`Kenmerk_check` case — a characteristic check is a CONDITION node, so the arm
lives in the condition walk, not in the expression walk — and this case alone
turns clean; measured over all four zones, with every other document keeping
its verdict and its cited rules.
