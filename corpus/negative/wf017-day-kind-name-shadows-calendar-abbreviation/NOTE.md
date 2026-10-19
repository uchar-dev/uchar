Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), the DECLARED-NAME clause: a day
kind whose `name` is one of the thirty spellings the normative calendar table
of `RUNTIME-SEMANTICS.md` ch. 3.8 recognises — here the ABBREVIATION `wk`.

    WF-017 FAIL /model/dayKinds/2/name: day-kind 'wk' shadows a built-in calendar unit

**The flank sat in the whole rule, not only in arm (a).** The clause over the
names and arm (a) over the further spellings compared against the same list, so
widening one without the other would have left a day kind that is simply CALLED
`wk` admissible while the same further spelling was refused — and a day kind's
name is itself one of its unit spellings. This case is the declared-name half.
The further-spelling half was pinned by a key of the separate spelling registry
until L3, which removed that registry and its witness
(`corpus/negative/README.md`, L3); since then a day kind's further spelling is
its `plural`, and the thirty-spelling class is pinned there by
`wf017-plural-shadows-calendar-unit` and
`wf017-plural-shadows-second-plural-form`.

**The producer side, measured 2026-09-20** on the pre-card binary (md5
`97239d1f8c2706a2548594b0541b5f34`): the reference frontend emitted
`Dagsoort de wk (mv: wks)` with exit 0, the day kind `wk` declared and an
EMPTY `diagnostics` array. Since this card a declared name that stands in the
table is an ERROR in the frontend, which blocks the emission entirely — dropping
the name alone would leave its §9.8 definition pointing at a day kind the
document no longer declares, which is its own refusal.

**Measured 2026-09-20** on that same binary: this document was `verifier: PASS`.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
added day kind, `{"node": "DayKind", "name": "wk"}` without a plural, beside
`schouwdag` and `stormvloeddag`. The other two day kinds are the host's own and
untouched.

**One line, and it is this clause's.** `wk` has no plural and is no other day
kind's plural, so neither arm (a) nor arm (b) reaches it; and a declared day
kind without a §9.8 definition is not itself a defect on this wire (WF-031
refuses a second definition, never a missing one). `expect.txt` pins the
clause.

Falsifiable, arm-exact: narrow the comparison in `wf017`
(`spec/verifier/lib/checks.ml`) back to the eighteen singulars and plurals and
this case turns clean — measured 2026-09-20 over all four corpus zones plus the
read-only parity zone, 702 documents, where exactly the three new cases move and
no report line changes anywhere else.
