Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (s)): an entry of
`dagsoortPerEenheid` whose **value** — the day kind the registered unit
spelling stands for — names a day kind *`dagsoorten` does not declare*.

    WF-034 FAIL /dagsoortPerEenheid/schouwdagen: day-kind unit 'schouwdagen'
    names day kind 'havendag', which no declaration carries: the registry entry
    resolves against no day kind

**What the field is.** `dagsoortPerEenheid` couples a written unit spelling
(the key) to the name of a day kind (the value). Declaring a day kind
registers both its spellings, so the host golden's map carries four entries:
`schouwdag` and `schouwdagen` both to `schouwdag`, `stormvloeddag` and
`stormvloeddagen` both to `stormvloeddag`.

**Why the value needed an arm.** The KEY side is already read: the verifier
asks whether a spelling occurs as a key and, if it does, admits it as a
duration unit beside the nine calendar units — in date arithmetic (WF-035) and
in a tijdsduur (WF-038). The VALUE was crossed against nothing. So the
admission could rest on a name no declaration carries: the spelling is let
through as a day-kind unit while the day kind it names does not exist. This
document is that shape, on the entry where it bites hardest — `schouwdagen` is
the declared `eenheid` of three `Strandmeester` attributes and of the
`schouwdrempel` parameter, so the spelling reaches evaluation while its
registry entry points at nothing.

**Who reads the value.** `ref` and the ASP backend read the map at zero
positions. The verifier read only the key. The Drools backend reads the value
at exactly two positions — the §6.10 tijdsduur and the §6.11 date arithmetic —
and defends itself there ad hoc: an unresolvable name yields a null day-kind
global and the transpiler emits a MARKER, so the form is silently not carried
rather than refused. That is one consumer, on one of four, and only at the two
duration positions — and no golden can carry those two forms, because `ref`
and the ASP backend both refuse day-kind durations outright ("a recorded
flank, not executed").

**Observability: no consumer notices.** Replayed against the host golden's own
two cases, with the verifier of the day before this card calling it
well-formed (2026-09-09):

    verifier  PASS
    ref       oracle: PASS (7 expectations) / PASS (3 expectations)
    clingo    PASS (7 expectations)          / PASS (3 expectations)
    drools    PLAYED 2

— byte for byte what the untouched golden yields in all four. That is what
makes this a wire arm rather than a differential finding: nothing in the
corpus, and nothing any executor runs today, would notice that the registry
entry has stopped meaning anything. It is the wave-6 `kenmerkSoorten`
argument — a name that crosses nothing in any executor still points at a
declaration that has to exist for the document to say what it says.

**Not WF-017.** WF-017 (ch. 2.9) asks whether two spellings COLLIDE — a day
kind against a built-in calendar unit, or against another day kind — and
resolves nothing. This arm asks whether one name RESOLVES, and compares
nothing. The positions are disjoint too: WF-017 walks the declared names under
`/dagsoorten`, this arm the values under `/dagsoortPerEenheid`. This document
carries no WF-017 defect and the report shows one line.

**The key side stays free, deliberately.** Half the entries are plural
spellings by construction — 32 of the 64 the four zones hold — and the v0 wire
carries no `plural` field for a day kind (ch. 2.9's `plural` is on the v1 node
only), so a key has nothing on the wire to be crossed against. An
engine-neutral IR must leave a producer free to register a third spelling, an
abbreviation among them, besides.

**Measured 2026-09-09, four zones (164 golden + 23 rood + 337 negatief + 30
parity = 554 documents predating this witness).** The map holds **64** entries
over **31** documents — 4 in the one golden `jutterswet-schouwrooster`, 60 in
the 30 parity fragments, none in rood and none in negatief before this one —
and every one of the 64 values resolves against a declared day kind; **0**
carry `null`. So the arm costs nothing: the before/after sweep over all 555
documents moves this document's verdict and not one other verdict, and not one
report line anywhere else.

**Both spellings, one arm.** A value naming an undeclared day kind and a
`null` value both fail to name a declared day kind (the schema types the value
`["string", "null"]`, so the null is schema-valid), and the one reader
collapses them — its loader reads the value with `asText()`, which turns a
JSON null into the string `"null"` and then fails the same lookup. The null
half has no corpus witness: zero of the 64 entries carry it, and making that
branch alone inert moves **0** of the 555 documents. It is folded into this
arm for that reason rather than given a clause and a witness of its own; off
corpus it is one edit from the host golden and is refused —
`day-kind unit 'schouwdagen' names no day kind at all`.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
field, `"schouwdagen": "schouwdag"` → `"schouwdagen": "havendag"`, a one-line
diff; schema-valid, since the map's values are `["string", "null"]`.

Falsifiable, arm-exact: delete the text `wf034_dagsoort_eenheden ctx; ` from
`let all` in `spec/verifier/lib/checks.ml` and this case, alone in all four
zones, turns clean — 555 documents compared, one verdict moved, no report
elsewhere.
