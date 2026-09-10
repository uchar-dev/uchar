Violates WF-094 clause (d) (`spec/IR-SPEC.md` ch. 2.1, maintainer decision
2026-09-09): a **keyed declaration with no self-name at all** — `naam: null`
under the key `Jutter`.

    WF-094 FAIL /objecttypen/Jutter/naam: object type under the key 'Jutter'
    carries no self-name (naam is null): a keyed declaration carries its own
    name, and an absent name is not one

**The empty half of the same field.** The clause above asks whether the second
spelling of the one name agrees with its key; this one asks whether it is there
at all. The OMISSION flank needs no rule — `naam` is `required` in
`spec/schema/regelspraak-ir-v0.schema.json`, so gate 2 rejects a missing field
and the shared OCaml loader — one `loader.ml`, consumed by the verifier, `ref`
and the ASP backend alike — refuses it with a load error naming the path. The
NULL flank is different: the field is typed `["string", "null"]`, so
`naam: null` is schema-valid, round-trips byte-identically, and passed the
verifier.

**This arm's ground is the CONSTRUCT, not measured damage — stated plainly,
because it is one of three that stand there.** Measured 2026-09-10 on
`corpus/golden/anchor/jutterswet-maatschap`, the golden this document is a
hand-broken copy of:

| consumer | untouched | `naam: null` |
| --- | --- | --- |
| verifier (before this clause) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 7 expectations | **PASS, 7 expectations** |
| ASP backend (`case.json`) | PASS, 7 expectations | **PASS, 7 expectations** |
| bean-model executor (`case.json`) | PLAYED | **PLAYED** — unchanged |

Nothing moves. The bean executor's loader took the SELF-NAME as the
declaration's name and, on this table, already fell back to the map key when
the field was empty, so the object type keeps its name and the document plays
on. The arm is here anyway, and the reason is a definition rather than a
finding: `objecttypen` is one of the FIVE map-shaped declaration tables, the
class is one class, and the field is `required` precisely because the
declaration carries its own name. A clause cut to only the two tables that
crash today would say the self-name means something different per table —
which is what WF-094's five-arm motivation denies — and would change again the
moment a loader changes. The wire does not follow one backend.

The honest reading of that: this arm rests on the construct, and its damage
column is empty by measurement, not by omission. Two of the five arms —
`parameters` and `dimensies` — carry the measured harm; see
`corpus/negatief/wf094-zelfnaam-null-parameter/NOTE.md` for the crash this
class produces where the fall-back was missing. (Route 2 of the same card gave
those two the fall-back as well, and `ZelfnaamTerugvalWitnessTest` pins all
five — including this one, so a later edit cannot quietly take this table's
fall-back away.)

**Measured cost: none.** Over the four zones **as they stood before these
witnesses** (2026-09-10: 572 documents) the five map-shaped tables held **2000**
keyed declarations — `objecttypen` 798, `parameters` 657, `eenheidstelsels` 222,
`feittypen` 311, `dimensies` 12 — and not one carried `naam: null`. The five `wf094-zelfnaam-null-*` witnesses are
the only documents anywhere that do (2016 entries over 577 documents once they
landed, each bringing its whole source document with it — this document among
them).

A hand-broken variant of `corpus/golden/anchor/jutterswet-maatschap/ir.json` —
one line, `"naam": "Jutter",` becomes `"naam": null,`; schema-valid, since the
field admits null on the v0 wire.

Falsifiable, arm-exact: drop the `objecttypen` arm from `wf094_null`
(`spec/verifier/lib/checks.ml`) — it is the HEAD of the `@`-chain, so the next
arm must lose its leading `@` or the mutant does not build — and this case,
alone in all four zones, turns clean (577 documents compared with both
binaries, one verdict moved, no report elsewhere). Dropping the `objecttypen`
arm from `wf094` — the non-null sister — does NOT free it: the two clauses are
separate functions precisely so that each of the ten arms is falsifiable on its
own witness.
