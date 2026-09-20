Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a KEY of
`dagsoortPerEenheid` that is one of the thirty spellings of the normative
calendar table (`RUNTIME-SEMANTICS.md` ch. 3.8) — here `secondes`, which is
neither an abbreviation nor the `-en` plural but a SECOND PLURAL FORM.

    WF-017 FAIL /dagsoortPerEenheid/secondes: registered day-kind unit spelling
    'secondes' shadows a built-in calendar unit

**Why this class is witnessed apart from the abbreviations.** Two of the twelve
spellings that fell through the old eighteen-name comparison are not
abbreviations at all: `secondes` beside `seconden`, and `millisecondes` beside
`milliseconden`. They fell through for the same reason — the comparison list was
the frontend registration guard's singulars and plurals, and these are a second
plural the table carries and that list did not — but they are a different form,
so a comparison repaired for abbreviations alone would still admit them. The
maintainer decision of 2026-09-19 gives them the same answer as the ten
abbreviations, and this case is what holds the verifier to it: it fails if the
widened comparison is ever narrowed to "the eighteen plus the ten
abbreviations".

**Measured 2026-09-20** on the pre-card binary (md5
`97239d1f8c2706a2548594b0541b5f34`): this document was `verifier: PASS`.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
added entry, `"secondes": "schouwdag"` beside the host's four ordinary entries,
a one-line diff. The host's units are all `schouwdagen`, so nothing else in the
document reads the new key.

**One line, and it is this arm's.** The four ordinary entries all bind a
declared name to itself, so arm (b) is silent, and every value resolves, so
WF-034 (s) is silent.

Falsifiable, arm-exact: narrow the comparison in `wf017`
(`spec/verifier/lib/checks.ml`) back to the eighteen singulars and plurals and
this case turns clean — measured 2026-09-20 over all four corpus zones plus the
read-only parity zone, 702 documents, where exactly the three new cases move and
no report line changes anywhere else.
