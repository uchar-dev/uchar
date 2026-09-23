Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (a): a KEY of
`dagsoortPerEenheid` — a registered day-kind unit spelling — that is one of the
eighteen built-in calendar unit names.

    WF-017 FAIL /model/dagsoortPerEenheid/dagen: registered day-kind unit spelling
    'dagen' shadows a built-in calendar unit

**Where the spellings live.** On the v0 wire a day kind's spellings sit in two
places: the singular in the `dagsoorten` list, and every REGISTERED spelling —
singular and plural alike — as a key of `dagsoortPerEenheid`. WF-017's check
read the first place only, so its built-in conjunct held for the declared name
and for no other registered spelling.

**What is wrong with this key: the document contradicts itself.**
`RUNTIME-SEMANTICS.md` ch. 3.8 settles the reading normatively — a duration's
unit is recognised by spelling through the calendar table, and "a spelling
outside the table is a declared day kind". This document registers `dagen` as a
day-kind unit while the table carries it as the plural of `dag`. Both readings
at once is not a reading any consumer can hold.

**And the consumers do not hold one reading — measured 2026-09-10.** The
verifier (WF-035, WF-038), `ref` and the ASP backend all resolve a duration
unit through the calendar table FIRST and consult the day-kind registry only
where the table has nothing; on those three this entry changes NOTHING, and
`datum + 5 dagen` stays ordinary calendar arithmetic. The Drools backend asks
the registry BEFORE the table at all four of its day-kind positions, so on this
same document it reads day-kind arithmetic over the definition of `schouwdag`.
That split — one document, two readings across the executors — is the second
reason the arm exists. (The backend's lookup order is a conformance question of
its own against the normative sentence above; correcting it there would not
make this document consistent.)

**The IR was weaker than its own producer, and the REGISTRATION GUARD is what
carries that.** The RegelSpraak frontend (`frontends/regelspraak`) refuses to
register a spelling that coincides with any of the eighteen built-in time-unit
names, warns, and drops the entry. The grammar helps for most of those names
but not all: *measured 2026-09-10 over all eighteen as a day-kind plural,*
FOURTEEN do not parse — they are keywords, and the frontend exits non-zero
without emitting anything (`Dagsoort de vaardag (mv: weken)`) — while FOUR
parse cleanly: `uren`, `minuten`, `seconden`, `milliseconden`. For those four
the frontend emits with exit 0 and the guard alone does the work; the emitted
registry holds only the singular, and the warning lands in the document's own
`diagnostics` array rather than on the console. Either way no document that
frontend emits can carry this shape. That is exactly why the arm has to be a
WIRE rule or nothing: a hand-built or foreign-produced document is the only way
in, and it was walking straight through.

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

**The flank this witness does not cover.** The arm compares against the
eighteen names the frontend's registration guard knows — the nine units'
singulars and plurals — while the calendar table recognises thirty spellings.
The twelve abbreviations (`jr`, `mnd`, `kw`, `wk`, `dg`, `u`, `min`, `sec`,
`s`, `ms`, `secondes`, `millisecondes`) pass this arm; measured 2026-09-10, all
twelve are verifier-clean as registry keys and, unlike the eighteen, this flank
is producible — the frontend emits `Dagsoort de wk (mv: wks)` with exit 0 and
an empty `diagnostics`. Carded as
`backlog/new/20260910-dagsoort-afkortingssleutels.md`.

Falsifiable, arm-exact: make the arm (a) guard in `wf017`
(`spec/verifier/lib/checks.ml`) inert and this case, alone in all four zones
plus the read-only parity zone, turns clean — 585 documents compared
2026-09-10, one verdict moved, no report line elsewhere.
