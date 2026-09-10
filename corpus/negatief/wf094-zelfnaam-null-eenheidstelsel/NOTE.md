Violates WF-094 clause (d) (`spec/IR-SPEC.md` ch. 2.1, maintainer decision
2026-09-09): a **keyed declaration with no self-name at all** — `naam: null`
under the key `Munt`.

    WF-094 FAIL /eenheidstelsels/Munt/naam: unit system under the key 'Munt'
    carries no self-name (naam is null): a keyed declaration carries its own
    name, and an absent name is not one

**The empty half of the same field.** The clause above asks whether the second
spelling of the one name agrees with its key; this one asks whether it is there
at all. The OMISSION flank needs no rule — `naam` is `required` in
`spec/schema/regelspraak-ir-v0.schema.json`, so gate 2 rejects a missing field
and all three OCaml loaders refuse it with a load error naming the path. The
NULL flank is different: the field is typed `["string", "null"]`, so
`naam: null` is schema-valid, round-trips byte-identically, and passed the
verifier.

**This arm's ground is the CONSTRUCT, not measured damage.** Measured
2026-09-10 on `corpus/golden/jutterswet-berging`, the golden this document is a
hand-broken copy of:

| consumer | untouched | `naam: null` |
| --- | --- | --- |
| verifier (before this clause) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 5 expectations | **PASS, 5 expectations** |
| ASP backend (`case.json`) | PASS, 5 expectations | **PASS, 5 expectations** |
| bean-model executor (`case.json`) | PLAYED | **PLAYED** — unchanged |

Nothing moves, and this table has the thinnest readership of the five: the
verifier does not read `Eenheidstelsel.naam` at all against its key, `ref`
reads no self-name anywhere, the ASP backend reads this one only to NAME a
system inside two fault messages no `expectedFault` pins, and the bean
executor's loader falls back to the key when the field is empty. So the arm
stands on the class and not on a crash: `eenheidstelsels` is one of the FIVE
map-shaped declaration tables, the field is `required` because the declaration
carries its own name, and a rule cut to only the tables that crash today would
make the self-name mean something different per table — the very thing
WF-094's five-arm motivation denies — and would move again with the next loader
change.

Two of the five arms — `parameters` and `dimensies` — carry the measured harm;
see `corpus/negatief/wf094-zelfnaam-null-parameter/NOTE.md` for the
`NullPointerException` this class produces where the fall-back was missing.
(Route 2 of the same card gave those two the fall-back as well, and
`ZelfnaamTerugvalWitnessTest` pins all five, this table included.)

**Measured cost: none.** Over the four zones (2026-09-10: 577 documents) the
five map-shaped tables hold **2000** keyed declarations — `objecttypen` 798,
`parameters` 657, `eenheidstelsels` 222, `feittypen` 311, `dimensies` 12 — and
not one carries `naam: null`.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one line,
`"naam": "Munt",` becomes `"naam": null,`; schema-valid, since the field admits
null on the v0 wire.

Falsifiable, arm-exact: drop the `eenheidstelsels` arm from `wf094_null`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (577 documents compared with both binaries, one verdict moved, no report
elsewhere). Dropping the `eenheidstelsels` arm from `wf094` — the non-null
sister — does NOT free it: the two clauses are separate functions precisely so
that each of the ten arms is falsifiable on its own witness.
