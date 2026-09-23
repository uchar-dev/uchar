Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), the DECLARED-NAME clause: a day
kind in `dagsoorten` whose name is one of the thirty spellings the normative
calendar table of `RUNTIME-SEMANTICS.md` ch. 3.8 recognises — here the
ABBREVIATION `wk`.

    WF-017 FAIL /model/dayKinds: day-kind 'wk' shadows a built-in calendar unit

**The flank sat in the whole rule, not only in arm (a).** The clause over
`/model/dayKinds` and the clause over the registry keys compared against the same
list, so widening one without the other would have left a day kind that is
simply CALLED `wk` admissible while a registry key `wk` was refused — and the
declared name is itself a registered spelling on any producer that registers the
singular. This case is the declared-name half; the key half is
`corpus/negatief/wf017-eenheidsleutel-schaduwt-kalenderafkorting`.

**The producer side, measured 2026-09-20** on the pre-card binary (md5
`97239d1f8c2706a2548594b0541b5f34`): the reference frontend emitted
`Dagsoort de wk (mv: wks)` with exit 0, `dagsoorten: ["wk"]` and an EMPTY
`diagnostics` array. Since this card a declared name that stands in the table is
an ERROR in the frontend, which blocks the emission entirely — dropping the name
alone would leave its §9.8 definition pointing at a day kind `dagsoorten` no
longer declares, which is its own refusal.

**Measured 2026-09-20** on that same binary: this document was `verifier: PASS`.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
added element, `"wk"` beside `schouwdag` and `stormvloeddag` in `dagsoorten`, a
one-line diff. It is declared and never registered, so the registry is the
host's own and untouched.

**One line, and it is this clause's.** `wk` occurs in no `dagsoortPerEenheid`
key, so neither arm (a) nor arm (b) reaches it; no `dagsoortPerEenheid` value
names it, so WF-034 (s) is silent; and a declared day kind without a §9.8
definition is not itself a defect on this wire (WF-031 refuses a second
definition, never a missing one).

Falsifiable, arm-exact: narrow the comparison in `wf017`
(`spec/verifier/lib/checks.ml`) back to the eighteen singulars and plurals and
this case turns clean — measured 2026-09-20 over all four corpus zones plus the
read-only parity zone, 702 documents, where exactly the three new cases move and
no report line changes anywhere else.
