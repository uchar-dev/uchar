Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a KEY of
`dagsoortPerEenheid` — a registered day-kind unit spelling — that is one of the
eighteen built-in calendar unit names.

    WF-017 FAIL /dagsoortPerEenheid/dagen: registered day-kind unit spelling
    'dagen' shadows a built-in calendar unit

**Where the spellings live.** On the v0 wire a day kind's spellings sit in two
places: the singular in the `dagsoorten` list, and every REGISTERED spelling —
singular and plural alike — as a key of `dagsoortPerEenheid`. WF-017's check
read the first place only, so its built-in conjunct held for the declared name
and for no other registered spelling.

**What the key does.** The key space is what the verifier consults when it asks
whether a unit spelling is a day-kind unit: a spelling occurring as a key is
admitted as a duration unit beside the calendar units, in date arithmetic
(WF-035) and in a tijdsduur (WF-038). With `"dagen": "schouwdag"` registered,
`datum + 5 dagen` stops being ordinary calendar arithmetic and becomes day-kind
arithmetic over the definition of `schouwdag`.

**The IR was weaker than its own producer.** The RegelSpraak frontend
(`frontends/regelspraak`) refuses at registration time to register a spelling
that coincides with a built-in time unit, warns, and drops the entry; and the
built-in unit names are grammar keywords besides, so the source language cannot
even express the shape — a source declaring a day kind with plural `weken`
fails to parse. No document that frontend emits can carry this. That is exactly
why the arm has to be a WIRE rule or nothing: a hand-built or foreign-produced
document is the only way in, and it was walking straight through.

**Measured 2026-09-10** on the binary of the day before this card (md5
67becaba23c6d3364a9f1a080355541e): this document was `verifier: PASS`, while
the same spelling as a DECLARED name is refused by the older clause
(`corpus/negatief/wf017-daykind-shadows-builtin`).

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
added entry, `"dagen": "schouwdag"` beside the host's four ordinary entries, a
one-line diff. The host's units are all `schouwdagen`, so nothing else in the
document reads the new key.

**One line, and it is this arm's.** The document carries no other defect: its
four ordinary entries all bind a declared name to itself, so arm (b) is silent,
and every value resolves, so WF-034 (s) is silent.

Falsifiable, arm-exact: make the arm (a) guard in `wf017`
(`spec/verifier/lib/checks.ml`) inert and this case, alone in all four zones
plus the read-only parity zone, turns clean — 585 documents compared
2026-09-10, one verdict moved, no report line elsewhere.
