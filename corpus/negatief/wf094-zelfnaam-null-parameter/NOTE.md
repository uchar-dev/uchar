Violates WF-094 clause (d) (`spec/IR-SPEC.md` ch. 2.1, maintainer decision
2026-09-09): a **keyed declaration with no self-name at all** — `naam: null`
under the key `vondstdrempel`.

    WF-094 FAIL /parameters/vondstdrempel/naam: parameter under the key
    'vondstdrempel' carries no self-name (naam is null): a keyed declaration
    carries its own name, and an absent name is not one

**The empty half of the same field.** The clause above this one asks whether
the second spelling of the one name AGREES with its key; this one asks whether
it is there at all. The OMISSION flank needs no rule — `naam` is `required` in
`spec/schema/regelspraak-ir-v0.schema.json`, so gate 2 rejects a missing field
and all three OCaml loaders refuse it with a load error naming the path. The
NULL flank is different: the field is typed `["string", "null"]`, so
`naam: null` is schema-valid, round-trips byte-identically, and passed the
verifier.

**This arm's ground is MEASURED harm**, and it is the sharpest of the five.
Taken 2026-09-10 on `corpus/golden/jutterswet-penningdrempel` — the golden this
document is a hand-broken copy of — with the identical one-field change:

| consumer | untouched | `naam: null` |
| --- | --- | --- |
| verifier (before this clause) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 3 expectations | **PASS, 3 expectations** |
| ASP backend (`case.json`) | PASS, 3 expectations | **PASS, 3 expectations** |
| bean-model executor (`case.json`) | PLAYED | **ERROR** — `NullPointerException: Cannot invoke "String.trim()" because "naam" is null` |

Three consumers are silent and CORRECT — they key everything off the map key,
so for them nothing is missing — and the fourth dies. That is the shape the
ch. 4.6 (n)/(o) pair does not have, where both OCaml executors were loud on the
empty form, and it is the reason the WIRE answers rather than one loader: a
document that passes verifier, `ref` and clingo while killing the third
executor turns the differential square into a triangle without saying so.

The crash channel is the identifier contract (`Namen.ident` trims the name
before splitting it) — the same contract the non-null clause's harm runs
through. It is reached because the bean executor's loader takes the SELF-NAME
as the declaration's name, and on `parameters` it had no fall-back to the key.

**Route 2, landed on the same card, and why it does not replace this arm.**
That loader now falls back to the key on all five tables, so the measurement
above reads `PLAYED` after the fix and a document written before this rule
loads instead of dying. What the fix removes is the one LOUD reader: without a
wire rule the spec still says nothing about an absent self-name, every new
consumer decides again what it means, and the disagreement becomes invisible
rather than audible. The fall-back is pinned additively by
`ZelfnaamTerugvalWitnessTest` in the Drools backend's own suite.

**Measured cost: none.** Over the four zones (2026-09-10: 577 documents) the
five map-shaped tables hold **2000** keyed declarations — `objecttypen` 798,
`parameters` 657, `eenheidstelsels` 222, `feittypen` 311, `dimensies` 12 — and
not one carries `naam: null`. The five `wf094-zelfnaam-null-*` witnesses are
the only documents anywhere that do.

A hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json` —
one line, `"naam": "vondstdrempel",` becomes `"naam": null,`; schema-valid,
since the field admits null on the v0 wire.

Falsifiable, arm-exact: drop the `parameters` arm from `wf094_null`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (577 documents compared with both binaries, one verdict moved, no report
elsewhere). Dropping the `parameters` arm from `wf094` — the non-null sister —
does NOT free it: the two clauses are separate functions precisely so that each
of the ten arms is falsifiable on its own witness.
