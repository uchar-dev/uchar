Violates WF-097 (`spec/IR-SPEC.md`): the NEGATED whole-period qualifier on a
`gedurende` clip member. Same arm as `wf097-clip-kenmerk-periode`, opposite
polarity.

A minimal hand-broken variant of
`corpus/golden/jutterswet-tegenwacht/ir.json`: one field on one node —
`rules[1].versions[0].value.filter.wholePeriod` goes from absent to a
`WholePeriod` with `negated: true`.

Why the polarity gets its own witness: a negated whole-period demand is a
STRONGER claim than the affirmative one, and it was measured to be exactly as
invisible — the reading with `negated: true` was indistinguishable from no
qualifier at all in the verifier and in both executors. A reader that honoured
the affirmative form and dropped the negation would still be silently wrong
here, so the refusal is pinned on both.

Since L5 the qualifier is a `WholePeriod {granularity, negated}` (delta B6);
the report names it `wholePeriod`, and `expect.txt` pins the arm.
