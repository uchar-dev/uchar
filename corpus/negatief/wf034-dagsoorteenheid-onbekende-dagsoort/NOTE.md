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
document is that shape, on the entry with the largest reach: `schouwdagen` is
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
nothing. The positions stay disjoint at the entry's own seam: since 2026-09-10
WF-017 reads this map too, but only its KEYS (arms (a) and (b)), where this arm
reads only the VALUES. Where the two could meet — a declared name as key whose
value names no declared day kind — arm (b) steps back by construction, so the
single diagnosis stays this arm's. *Measured 2026-09-10:* this document reports
exactly one line, and it is WF-034's; the two shapes that were verifier-clean
on 2026-09-09 (a registry key `"dagen": "schouwdag"` shadowing a calendar unit,
and a declared day kind whose own singular is already the first one's plural)
are now refused by those arms, each with its own witness under
`corpus/negatief/wf017-*`.

**The key side carries no RESOLUTION rule, deliberately** (WF-017's collision
arms are a different question and do read the keys). The carrying argument is
what the WIRE carries. A day kind's plural arrives as a FURTHER KEY of this map and
as no field of its own (ch. 2.9's `plural` is on the v1 node), so nothing here
distinguishes a DECLARED plural from a DERIVED one or from an abbreviation —
all three are ordinary keys. A key rule would have to reconstruct that
distinction by fixing Dutch morphology inside an engine-neutral IR, and would
bind every producer to one language's plural formation. (The source language
does make the plural an optional author field, §3.12; that is not the ground,
and it does not mean a plural is absent when the author omits one — the
reference frontend derives a `-en` plural in exactly that case and cites §3.12
for it. The wire cannot tell the two apart, which is the ground.)

Measured on the LANDED state: 35 of the 70 entries are keyed on something other
than the declared name — entries, not independent cases. The 70 carry four
distinct (registry, declarations) shapes, one repeated byte-identically over 30
parity fragments; the 35 keys spell three distinct words, 34 of them the plain
`<declared name>` + `en` form the frontend derives, and the one exception is
this document's own twisted value. Abbreviations and irregular plurals have
**zero** witnesses in the corpus.

**Measured 2026-09-09, four zones on the BASELINE that predates this witness
(164 golden + 23 rood + 337 negatief + 30 parity = 554 documents).** The map
held **64** entries over **31** documents — 4 in the one golden
`jutterswet-schouwrooster`, 60 in the 30 parity fragments, none in rood and
none in negatief before this one — and every one of those 64 values resolved
against a declared day kind; **0** were null. So the arm costs nothing: the
before/after sweep over all 555 documents (the 554 plus this witness) moves
this document's verdict and not one other verdict, and not one report line
anywhere else.

**On the state the card LANDS on** the same four zones hold **70** entries over
**33** documents against **45** declared day kinds, of which **69** resolve —
the one that does not is this document, put here to be refused. The extra six
over the baseline are this file's four and the two identity entries in
`jutterswet-vormenkabinet`.

**Three spellings, one arm.** A value naming an undeclared day kind, a `null`
value and the EMPTY STRING all fail to name a declared day kind (the schema
types the value `["string", "null"]`, so the null is schema-valid; the empty
string is an ordinary string that resolves against nothing and is reported as
`names day kind ''`), and the one reader collapses null onto the naming case —
its loader reads the value with `asText()`, which turns a JSON null into the
string `"null"` and then fails the same lookup. Only the naming spelling has a
corpus witness: of the 70 entries the four zones now hold, **zero** are null
and **zero** are empty, and making the null branch alone inert moves **0** of
the 555 documents. Both silent spellings are folded into this arm for that
reason rather than given clauses and witnesses of their own; off corpus each is
one edit from the host golden and is refused by its own message
(`day-kind unit 'schouwdagen' names no day kind at all`).

**The identity the arm compares with has its own witness, elsewhere.** This
document pins the RESOLUTION half. The `norm` half — collapse whitespace, lower
case — is pinned by `corpus/golden/jutterswet-vormenkabinet`, which carries two
entries that resolve only through normalisation; before those existed,
replacing `mem_ci` with a byte-exact comparison moved 0 of 555 documents and
gate 3 stayed green.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
field, `"schouwdagen": "schouwdag"` → `"schouwdagen": "havendag"`, a one-line
diff; schema-valid, since the map's values are `["string", "null"]`.

Falsifiable, arm-exact: delete the text `wf034_dagsoort_eenheden ctx; ` from
`let all` in `spec/verifier/lib/checks.ml` and this case, alone in all four
zones, turns clean — 555 documents compared, one verdict moved, no report
elsewhere.
