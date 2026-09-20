Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a KEY of
`dagsoortPerEenheid` that is one of the thirty spellings the normative calendar
table of `RUNTIME-SEMANTICS.md` ch. 3.8 recognises — here the ABBREVIATION `wk`.

    WF-017 FAIL /dagsoortPerEenheid/wk: registered day-kind unit spelling
    'wk' shadows a built-in calendar unit

**Why the abbreviation needs its own witness.** Until 2026-09-20 arm (a)
compared against the EIGHTEEN names the frontend's registration guard knew: the
singular and the plural of the nine units. The table carries THIRTY, and the
twelve in between — `jr`, `mnd`, `kw`, `wk`, `dg`, `u`, `min`, `sec`, `s`, `ms`
and the second plurals `secondes` and `millisecondes` — fell through the rule
entirely. The contradiction is the same one
`corpus/negatief/wf017-eenheidsleutel-schaduwt-kalendereenheid` states for
`dagen`: ch. 3.8 settles that a duration's unit is recognised by spelling
through the table and that "a spelling outside the table is a declared day
kind", while this key says `wk` IS a day-kind unit. Both readings at once is not
a reading a consumer can hold.

**And this class is not hypothetical on the wire.** `corpus/golden/jutterswet-vervaltermijn`
uses `wk`, `mnd` and `kw` as ordinary calendar units. A document that registers
one of them as a day-kind unit says two things about the same spelling, and
until 2026-09-19 the executors answered differently: the verifier, `ref` and the
ASP backend consult the table first, the Drools backend asked the registry
first at all four of its day-kind positions. That lookup order is turned around
now, so all four read ch. 3.8's order — which removes the split but not the
contradiction, and the contradiction is what this document carries.

**Unlike the eighteen, this class was PRODUCIBLE.** Measured 2026-09-20 on the
pre-card binary (md5 `97239d1f8c2706a2548594b0541b5f34`): the reference frontend
emitted `Dagsoort de wk (mv: wks)` with exit 0, an EMPTY `diagnostics` array and
the registry key `wk`. The guard compares against the thirty since this card, so
that source no longer emits silently.

**Measured 2026-09-20** on that same binary: this document was `verifier: PASS`.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
added entry, `"wk": "schouwdag"` beside the host's four ordinary entries, a
one-line diff. The host's units are all `schouwdagen`, so nothing else in the
document reads the new key.

**One line, and it is this arm's.** The four ordinary entries all bind a
declared name to itself, so arm (b) is silent, and every value resolves, so
WF-034 (s) is silent.

Falsifiable, arm-exact: narrow the comparison in `wf017`
(`spec/verifier/lib/checks.ml`) back to the eighteen singulars and plurals and
this case turns clean — measured 2026-09-20 over all four corpus zones plus the
read-only parity zone, 702 documents, where exactly the three
`wf017-*-kalenderafkorting` / `-tweede-meervoudsvorm` cases move and no report
line changes anywhere else.
