Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries a
whole-period qualifier. The clip already ranges over the whole timeline, so the
qualifier names no reading the clip has.

A minimal hand-broken variant of
`corpus/golden/jutterswet-tegenwacht/ir.json`: one field on one node —
`rules[1].versions[0].value.filter.wholePeriod` goes from absent to an
affirmative `WholePeriod`. The characteristic is TIME-DEPENDENT
(`aangesteld` carries a granularity), so WF-043's non-time-dependent arm
cannot fire here: inside a clip WF-055 already requires a temporal
characteristic, which is exactly why WF-043 left this position unguarded and
why the qualifier evaporated in silence.

The member is the plain (non-composite) clip condition, the sister path to
`wf097-clip-characteristic-host`'s composite member.

Its negated twin is `wf097-clip-characteristic-period-negated`: `negated: true` was
measured indistinguishable from no qualifier in every reader, so the arm refuses
both polarities and both are pinned.

Since L5 the qualifier is a `WholePeriod {granularity, negated}` (delta B6);
the report names it `wholePeriod`, and `expect.txt` pins the arm.
