Violates WF-094 clause (d) (`spec/IR-SPEC.md` ch. 2.1, maintainer decision
2026-09-09): a **keyed declaration with no self-name at all** — `naam: null`
under the key `seizoen`.

    WF-094 FAIL /dimensies/seizoen/naam: dimension under the key 'seizoen'
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

**This arm's ground is measured harm at the LOADER, with no playable witness.**
`dimensies` is, with `parameters`, one of the two tables where the bean-model
executor's loader took the self-name straight into the identifier contract with
no fall-back to the key — the shape whose end-to-end cost the parameter witness
measures as a `NullPointerException`. Here that end-to-end measurement cannot
be taken, and the reason is structural rather than an omission:

| consumer | untouched | `naam: null` |
| --- | --- | --- |
| verifier (before this clause) | PASS | **PASS** |
| `ref` | *(no case file — the shape is not executable)* | — |
| ASP backend | *(no case file — the shape is not executable)* | — |
| bean-model executor | *(no case file — the shape is not executable)* | — |

Measured 2026-09-10: the four zones hold **12** dimension declarations and the
only two goldens carrying a `dimensies` table — `jutterswet-seizoenvondst`
(this document's source) and `jutterswet-vormenkabinet` — are the two
**verifier-only** witness-cabinet documents, with no `case*.json`, because no
executor implements a dimension-cell read (see
`corpus/golden/jutterswet-seizoenvondst/NOTE.md`). So the table has a live key
and no live EXECUTION, exactly as the non-null dimension witness records for
its own arm. The wire clause stands in for the executor that is not there yet;
when the cell read becomes executable this witness gains a case-carrying
sister.

**What CAN be pinned, and is.** The loader level. Route 2 of the same card
taught this loader the same `!= null ? … : key` fall-back its three sister
tables already had, and `ZelfnaamTerugvalWitnessTest` in the Drools backend's
own suite pins it for `dimensies` too — the one place this arm's fall-back is
observable at all. Against the loader as it stood before that fix, that test
fails with `expected: <seizoen> but was: <null>`.

**Measured cost: none.** Over the four zones (2026-09-10: 577 documents) the
five map-shaped tables hold **2000** keyed declarations — `objecttypen` 798,
`parameters` 657, `eenheidstelsels` 222, `feittypen` 311, `dimensies` 12 — and
not one carries `naam: null`.

A hand-broken variant of `corpus/golden/jutterswet-seizoenvondst/ir.json` — one
line, `"naam": "seizoen",` becomes `"naam": null,`; schema-valid, since the
field admits null on the v0 wire.

Falsifiable, arm-exact: drop the `dimensies` arm from `wf094_null`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (577 documents compared with both binaries, one verdict moved, no report
elsewhere). Dropping the `dimensies` arm from `wf094` — the non-null sister —
does NOT free it: the two clauses are separate functions precisely so that each
of the ten arms is falsifiable on its own witness.
