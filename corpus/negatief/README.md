# Negatief corpus

Deliberately ill-formed IR documents: the failing half of the spec
discipline (every WF rule of `spec/IR-SPEC.md` carries at least one
passing golden case and one failing case here).

Layout: one directory per case, named `wf###-<slug>` — the prefix
declares the violated rule. Each directory holds `ir.json` (a minimal
hand-broken variant of a golden dump) and `NOTE.md` (which rule, and
what was broken). Gate 3 (`scripts/check-verifier.sh`) requires the
verifier to reject every case here **for exactly the declared rule**; a
case that passes, or fails only for other rules, turns the gate red.

These files are not required to validate against the schema (gate 2
covers `corpus/golden/` only); they must stay *loadable* — a violation
is a semantic defect in a structurally well-formed document, not a
syntax error.

Paths in the notes are JSON pointers into the M1 document, the spelling the
verifier reports since 2026-09-23 (`/model/objectTypes/<i>/...`,
`/rules/resolvedRegels/<i>/...`). The citations were respelled when the
verifier's paths moved, quotes of measurements taken on a document's v0 form
included: the position cited is the same, only its spelling is the M1 one.

## Vervallen bij de v1-migratie

A witness whose rule the v1 migration takes away gets one of three stamps
(decision V14 (a), `spec/proposals/V1-WIREMIGRATIE.md`): **herschreven**
(rewritten on the rule that still holds), **vervangen** (replaced by a
successor witness of a successor rule) or **verwijderd-met-reden** (removed,
with the reason). This list names every one of them, per migration step, so a
witness never leaves the corpus unrecorded. The floor `EXPECTED_MIN_NEGATIEF`
in `scripts/check-verifier.sh` is a lower bound and did not have to move.

### M1 — the container (2026-09-23)

Measured before removal: `spec/proposals/v1-migratie/m1.py` refuses each of
the 21 removed documents at exactly the field that made it a witness (the
message is quoted per class below), so none of them has an M1 form that still
carries its defect. Every other negatief migrated mechanically and is rejected
on the rule its directory name declares, as before (gate 3).

| directory | rule | delta | stamp | reason |
| --- | --- | --- | --- | --- |
| `wf094-zelfnaam-objecttype` | WF-094 | B2 | verwijderd-met-reden | the map key is gone; the node carries its one name, so there is no second spelling to disagree (m1.py: "self-name differs from its map key") |
| `wf094-zelfnaam-parameter` | WF-094 | B2 | verwijderd-met-reden | idem |
| `wf094-zelfnaam-eenheidstelsel` | WF-094 | B2 | verwijderd-met-reden | idem |
| `wf094-zelfnaam-feittype` | WF-094 | B2 | verwijderd-met-reden | idem |
| `wf094-zelfnaam-dimensie` | WF-094 | B2 | verwijderd-met-reden | idem |
| `wf094-zelfnaam-null-objecttype` | WF-094 (d) | B2 | verwijderd-met-reden | `ObjectType.name` is a required string: an object type without a name cannot be written (m1.py: "self-name is null") |
| `wf094-zelfnaam-null-parameter` | WF-094 (d) | B2 | verwijderd-met-reden | no map key is left to fall back on, so the node's `naam` is the declaration's only name, and since M1 the schema types it as a non-null string: the null form cannot be written (m1.py: "self-name is null or absent") |
| `wf094-zelfnaam-null-eenheidstelsel` | WF-094 (d) | B2 | verwijderd-met-reden | idem |
| `wf094-zelfnaam-null-feittype` | WF-094 (d) | B2 | verwijderd-met-reden | idem |
| `wf094-zelfnaam-null-dimensie` | WF-094 (d) | B2 | verwijderd-met-reden | idem |
| `wf034-wees-attribuutmeervoud` | WF-034 (p) | B5 | verwijderd-met-reden | a facet is a field of its member's node, so a facet without a member cannot be written (m1.py: "key names no declared attribute") |
| `wf034-wees-attribuutdimensies` | WF-034 (p) | B5 | verwijderd-met-reden | idem |
| `wf034-wees-attribuutgranulariteit` | WF-034 (p) | B5 | verwijderd-met-reden | idem |
| `wf034-wees-attribuuttijdlijnaantal` | WF-034 (p) | B5 | verwijderd-met-reden | idem |
| `wf034-wees-attribuuttijdlijnnaam` | WF-034 (p) | B5 | verwijderd-met-reden | idem |
| `wf034-wees-kenmerksoorten` | WF-034 (p) | B5 | verwijderd-met-reden | idem (m1.py: "key names no declared characteristic") |
| `wf034-wees-kenmerkgranulariteit` | WF-034 (p) | B5 | verwijderd-met-reden | idem |
| `wf034-wees-kenmerktijdlijnaantal` | WF-034 (p) | B5 | verwijderd-met-reden | idem |
| `wf034-zijtabel-kapitalisatiewees` | WF-034 (p) | B5 | verwijderd-met-reden | the same class written with a recapitalised key; the key no longer exists, the facet hangs on the member it belongs to |
| `wf059-attribuut-zonder-granulariteit` | WF-059 arm 3 | B5 | verwijderd-met-reden | `TimelineFacet.granularity` is a required, non-null enum: a timeline without a grid cannot be written (m1.py: "granularity null is not one of DAG/WEEK/MAAND/KWARTAAL/JAAR") |
| `wf059-kenmerk-zonder-granulariteit` | WF-059 arm 2 | B5 | verwijderd-met-reden | idem, on a characteristic |
| `wf059-attribute-without-datatype` | WF-059 arm 1 | B5 | herschreven | the rule holds; the document was rewritten by hand in the M1 shape (an Attribute node with `"datatype": null`, which the schema admits) and is still rejected on WF-059, not by a load error |

The null half of WF-094 on the four nodes that otherwise keep their v0 shape
at M1 (`Parameter`, `Eenheidstelsel`, `Feittype`, `Dimensie`) is unwritable as
well: since M1 their `naam` is a non-null string in
`spec/schema/uchar-ir-v1.schema.json`, because with the map key gone it is the
declaration's only name. Measured 2026-09-23 on the four removed witnesses
rewritten in the M1 shape with `"naam": null`: gate 2 refuses each of them
(`None is not of type 'string'`), and the shared OCaml loader
(`spec/verifier/lib/loader.ml`, which `ref` and the ASP backend load through as
well) refuses them at load. No golden, red or negatief document carried a null
there (237 + 48 + 438 documents, 714 declarations on the four lists).
