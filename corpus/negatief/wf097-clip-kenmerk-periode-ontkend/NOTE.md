Violates WF-097 (`spec/IR-SPEC.md`): the NEGATED whole-period qualifier on a
`gedurende` clip member. Same arm as `wf097-clip-kenmerk-periode`, opposite
polarity.

A minimal hand-broken variant of
`corpus/golden/jutterswet-tegenwacht/ir.json`: one field on one node —
`resolvedRegels[1].versions[0].value.filter.periode` goes from `null` to a
`GeheelPeriode` with `niet: true`.

Why the polarity gets its own witness: a negated whole-period demand is a
STRONGER claim than the affirmative one, and it was measured to be exactly as
invisible — the reading with `niet: true` was indistinguishable from `periode:
null` in the verifier and in both executors. A reader that honoured the
affirmative form and dropped the negation would still be silently wrong here, so
the refusal is pinned on both.
