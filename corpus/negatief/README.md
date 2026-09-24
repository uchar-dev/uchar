# Negatief corpus

Deliberately ill-formed IR documents: the failing half of the spec
discipline (every WF rule of `spec/IR-SPEC.md` carries at least one
passing golden case and one failing case here).

Layout: one directory per case, named `wf###-<slug>` — the prefix
declares the violated rule. Each directory holds `ir.json` (a minimal
hand-broken variant of a golden dump) and `NOTE.md` (which rule, and
what was broken). Gate 3 (`scripts/check-verifier.sh`) requires the
verifier to reject every case here **for exactly the declared rule**; a
case that passes, or fails only for other rules, turns the gate red. A
directory may also hold `expect.txt` (since L2, U1): one literal line of the
verifier message of the ARM the case pins, without a JSON pointer, which gate 3
requires verbatim in the report — so a case that moves to another arm of its
rule turns the gate red too.

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

### L2 — the reference layer (2026-09-24)

L2 (card `20260923-l2-referentielaag`, `backlog/done/20260923-l2-referentielaag.md`;
the decision numbers D# and U# below are that card's decision list) takes the reference layer to its symbolic
form: the five reference classes lose their own `type` (B13), a navigation, a
fold range, an allocation, a creation step and a scope carry their role binding
and no written name beside it (B7, B8, B11, B12, D53), the two role checks
become one `RoleMembership` node (B9), and the unknown-type node leaves the
datatype union (B16). Every witness whose defect lived in one of those fields
was measured before its stamp: either `spec/proposals/v1-migratie/m2.py`
refuses the document at exactly that field (the refusal is quoted), or the
migration's translation is verifier-clean (its subject left the wire), or the
translation falls on another rule, and then the witness is rewritten under that
rule (D17) — `old → new` in the first column. A rewritten or replaced witness
with an L2 delta carries `expect.txt`, one literal line of the verifier message of the arm it now
pins, without a JSON pointer; gate 3 requires that line in the report (U1). The
arm column names that arm in words, or, for a removed witness, the arm that left.
The negatief zone went from 444 to 394 documents at the L2 union (with the U12
witness `wf034-rollidmaatschap-vreemde-vullersklasse` added beside the stamps),
and to 400 in the L2 fix round (the restored `wf046-kwantorvouw-bereik-ander-type`
and the five witnesses the fix round added, listed in the last table beside the
U12 witness); the floor `EXPECTED_MIN_NEGATIEF` did not have to move.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf091-aantal-decimalen` | WF-091 | B13 (+B21, L4) | herschreven (m) | derived arm, AANTAL, decimals facet | a count's annotation is a derived position and keeps its type (V5 (a)); the second operand leaves with the count's operand list at B21 (L4), and until then WF-037 reports beside it |
| `wf091-aantalgevulde-eenheid` | WF-091 | B13 | herschreven (m) | derived arm, AANTAL_GEVULDE, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-afgerond-decimalen` | WF-091 | B13 | herschreven (m) | derived arm, rounding, decimals facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-afgerond-eenheid` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, rounding, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-begrensd-eenheid` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, clamp, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-binair-decimalen` | WF-091 | B13 | herschreven (m) | derived arm, PLUS, decimals facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-binair-eenheid` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, PLUS, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-binair-geheel` | WF-091 | B13 | herschreven (m) | derived arm, PLUS, wholeness facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-fold-decimalen` | WF-091 | B13 | herschreven (m) | derived arm, MINIMUM, decimals facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-foldwortel-eenheid` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, MAXIMUM, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-maal-eenheid` | WF-091 | B13 | herschreven (m) | derived arm, MAAL, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-som-eenheid` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, SOM, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-totaal-eenheid` | WF-091 | B13 | herschreven (m) | derived arm, TOTAAL, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-attribuutref-decimalen` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-eenheid-converteerbaar` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-eenheid-kast` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-geheel` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-teken` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-eenheid-erbij` → `wf023-eenheidloze-lezing-in-kg-doel` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 unit clause (unitless declaration into a kg target) | reference-type arm gone; measured: the translation falls on WF-023, which had no witness of this clause |
| `wf091-attribuutref-eenheid-ontbreekt` → `wf023-kg-lezing-in-eenheidloos-doel` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 unit clause (kg declaration into a unitless target) | as the row above, the other one-sided direction |
| `wf091-attribuutref-reststructuur` → `wf023-lijstlezing-in-somtypedoel` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 family clause (a list into a sumtype target) | reference-type arm gone; measured: WF-023 (D17; the form is also `wf023-schrijf-lijst-geen-domein-waarde`'s) |
| `wf091-attribuutref-soort` → `wf023-getallezing-in-tekstdoel` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 family clause (a number into a text target) | reference-type arm gone; measured: WF-023, the mirror of `wf023-text-into-numeric` (D17) |
| `wf091-binair-soort` | WF-091 | B13, B14 (L3) | herschreven (i) | derived arm, PLUS, soort facet | the annotation is rewritten to a boolean (D18), so the soort facet keeps the report when B14 (L3) makes a percentage a number with a unit; WF-023 reports the boolean in the percentage target beside it |
| `wf091-binderref-eenheid` → `wf076-let-eenheid-tegen-lichaam` | WF-091 → WF-076 | B13 | herschreven op WF-076 | WF-076 (Let type against body type) | the binder reference lost its own type; measured: the Let's `t` against the body's declared `kg` is WF-076 (D17) |
| `wf091-dimensiecel-eenheid` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (DimensionCellRead) | the cell read's own type left the wire; measured: the migration's translation is verifier-clean |
| `wf091-foldbron-eenheid` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (fold source) | the fold source's own type left the wire; against the declaration the SOM annotation is correct; measured: the migration's translation is verifier-clean |
| `wf091-parameterref-eenheid` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (ParameterReference) | the reference's own type left the wire; measured: the migration's translation is verifier-clean |
| `wf091-schrijfdoel-eenheid` | WF-091 | B13 | verwijderd-met-reden | rule-target arm | the rule target is a reference and lost its own type; measured: the migration's translation is verifier-clean |
| `wf089-aggregatie-doorgeefluik-enum` | WF-089 | B13 | herschreven (m) | aggregation (pass-through) arm, EERSTE | the annotation of the EERSTE is a derived position and stays; operand 0 is typed by its declaration; the WF-038 line beside it is unchanged |
| `wf089-attribuutref-enum` → `wf063-match-arm-buiten-gelezen-domein` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 arms outside the scrutinee's declared domain | reference-type arm gone; measured: WF-063 (D17; the form is `wf063-enum-alien-value`'s and `wf063-enum-missing-value`'s) |
| `wf089-attribuutref-geen-domein` → `wf023-enumeratielezing-in-getaldoel` | WF-089 → WF-023 | B13 | herschreven op WF-023 | WF-023 family clause (an enumeration into a number target) | reference-type arm gone; measured: WF-023 (D17) |
| `wf089-attribuutref-geen-keuzetype-declaratie` → `wf063-match-over-getallezing` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 scrutinee is no keuzetype | reference-type arm gone; measured: WF-063, an arm that had no witness |
| `wf089-attribuutref-lijst-sum` → `wf070-lambdaparameter-ander-somdomein` | WF-089 → WF-070 | B13 | herschreven op WF-070 | WF-070 lambda parameter against the source element type (sum domain) | reference-type arm gone; measured: WF-070, an arm that had no witness |
| `wf089-attribuutref-navigatie-enum` → `wf063-match-arm-buiten-domein-na-navigatie` | WF-089 → WF-063 | B13, B11 | herschreven op WF-063 | WF-063 arms outside the declared domain, navigated scrutinee | reference-type arm gone; measured: WF-063 (D17) |
| `wf089-attribuutref-sum` → `wf063-match-constructor-buiten-gelezen-somdomein` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 constructors outside the scrutinee's declared sum domain | reference-type arm gone; measured: WF-063 (D17) |
| `wf089-binderref-sum` → `wf063-match-over-binder-ander-somdomein` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 constructors outside the binder's declared sum domain | binder-reference arm gone; measured: WF-063 (D17) |
| `wf089-composiet-domein` | WF-089 | B13 | herschreven (m) | composite arm (PLUS annotated with a keuzetype) | the composite keeps its annotation (a derived position) |
| `wf089-dimensiecel-enum` → `wf063-match-arm-buiten-domein-dimensiecel` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 arms outside the declared domain, cell-read scrutinee | cell-read arm gone; measured: WF-063 (D17) |
| `wf089-enum-over-som` → `wf036-gelijkheid-somtype-tegen-enumeratie` | WF-089 → WF-036 | B13 | herschreven op WF-036 | WF-036 equality across families (sumtype vs enumeration) | reference-type arm gone; measured: WF-036, an arm that had no witness |
| `wf089-foldbron-enum` | WF-089 | B13 | verwijderd-met-reden | fold-source reference arm | measured: the translation stays WF-089 through the pass-through aggregation arm, byte-identical to `wf089-aggregatie-doorgeefluik-enum` (D18: an arm change gate 3 cannot see) |
| `wf089-minimum-doorgeefluik` | WF-089 | B13 | herschreven (m) | aggregation (pass-through) arm, MINIMUM | as `wf089-aggregatie-doorgeefluik-enum`; the WF-038 line beside it is unchanged |
| `wf089-parameterref-enum` → `wf063-match-arm-buiten-domein-parameter` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 arms outside the parameter's declared domain | parameter-reference arm gone; measured: WF-063 (D17) |
| `wf089-som-over-enum` → `wf036-gelijkheid-enumeratie-tegen-somtype` | WF-089 → WF-036 | B13 | herschreven op WF-036 | WF-036 equality across families (enumeration vs sumtype) | reference-type arm gone; measured: WF-036, the mirror of the row above (D17) |
| `wf090-attribuutref` → `wf074-veld-buiten-gelezen-structdomein` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074 field not declared on the base's struct domain | reference-type arm gone; measured: WF-074 (D17; the form is `wf074-unknown-field`'s) |
| `wf090-attribuutref-lijst` → `wf070-lambdaparameter-ander-structdomein` | WF-090 → WF-070 | B13 | herschreven op WF-070 | WF-070 lambda parameter against the source element type (struct domain) | reference-type arm gone; measured: WF-070, the struct side of the sum-domain row |
| `wf090-binderref` → `wf074-veld-buiten-structdomein-binder` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074, binder base | binder-reference arm gone; measured: WF-074 (D17) |
| `wf090-dimensiecel` → `wf074-veld-buiten-structdomein-dimensiecel` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074, cell-read base | cell-read arm gone; measured: WF-074 (D17) |
| `wf090-doorgeefluik` | WF-090 | B13 | herschreven (m) | aggregation (pass-through) arm, EERSTE | the EERSTE keeps its annotation; operand 0 is typed by its declaration; the WF-038 line beside it is unchanged |
| `wf090-foldbron` | WF-090 | B13 | verwijderd-met-reden | fold-source reference arm | measured: the translation stays WF-090 through the pass-through aggregation arm, on the arm of `wf090-doorgeefluik` (the same arm under EERSTE, with the two struct domains the other way round; D18: an arm change gate 3 cannot see) |
| `wf090-parameterref` → `wf074-veld-buiten-structdomein-parameter` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074, parameter base | parameter-reference arm gone; measured: WF-074 (D17) |
| `wf090-schrijf-defaults` | WF-090 | — | herschreven (m) | write half (a StructLiteral of another domain) | no L2 delta; the struct literal keeps its type |
| `wf090-schrijf-objectcreatie` | WF-090 | — | herschreven (m) | write half (a StructLiteral of another domain) | no L2 delta; the struct literal keeps its type |
| `wf090-schrijf-regeldoel` | WF-090 | — | herschreven (m) | write half (a StructLiteral of another domain) | no L2 delta; the struct literal keeps its type |
| `wf088-functiondef-drift` | WF-088 | — | herschreven (m) | enumeration use site against its declaration | no L2 delta on the defect; the body's binder reference lost its own type, and with it the third report line (the expression layer's), the two declaration lines remain |
| `wf088-literal-type-drift` | WF-088 | B22 (L4) | herschreven (m) | enumeration use site against its declaration | the drift sits on a `Literal`'s type, which stays (V9 (a)) |
| `wf088-use-site-value-drift` | WF-088 | — | herschreven (m) | enumeration use site against its declaration | no L2 delta; the drift sits on the attribute declaration |
| `wf088-use-site-value-order` | WF-088 | — | herschreven (m) | enumeration use site against its declaration | no L2 delta; the drift sits on the attribute declaration |
| `wf078-binderref-type-lie` → `wf076-let-soort-tegen-lichaam` | WF-078 → WF-076 | B13 | herschreven op WF-076 | WF-076 (Let type against body type) | WF-078 retired with the binder reference's own type; measured: the Let's text type against the body's declared number is WF-076 (D17) |
| `wf034-sentinel-builtin-operand` → `wf047-bereik-op-builtin` | WF-034 (sentinel fence) → WF-047 | B13 | herschreven op WF-047 | WF-047, a range on a node that is no instance fold | the sentinel left the wire; the migration wrote the range the schema demands on a single-argument Functie with an elementObjecttype (U2), and measured, WF-047 refuses it on a builtin (D17; the card expected verwijderd-met-reden, measured before the migration wrote the range) |
| `wf034-sentinel-builtin-wratoperand` | WF-034 (sentinel fence) | B13 (+V6) | herschreven (m), armwissel | WF-034 name resolution (a role name read as an attribute) | the sentinel left the wire; measured: the role name read on a builtin is an unresolved member; WF-047 reports the derived range beside it |
| `wf034-sentinel-gelest-vouwoperand` | WF-034 (sentinel fence) | B13 | verwijderd-met-reden | sentinel fence on a listed operand | the sentinel left the wire; the read is an ordinary read; measured: the migration's translation is verifier-clean |
| `wf034-sentinel-gelest-wratoperand` | WF-034 (sentinel fence) | B13 (+V6) | herschreven (m), armwissel | WF-034 name resolution (a role name read as an attribute) | the sentinel left the wire; measured: the role name as a listed operand is an unresolved member |
| `wf034-sentinel-geleste-setlezing` → `wf053-to-many-lijstvouwoperand` | WF-034 (sentinel fence) → WF-053 | B13, B11 | herschreven op WF-053 | WF-053 to-many hop in a listed operand | sentinel and set reading gone; measured: the translation is byte-identical to that of `wf096-setlezing-geleste-operand`, so both old witnesses share the one successor directory |
| `wf034-sentinel-kale-lezing` | WF-034 (sentinel fence) | B13 | verwijderd-met-reden | sentinel on an ordinary read | the sentinel left the wire; measured: the migration's translation is verifier-clean |
| `wf034-sentinel-lijstelement-payload` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-payload-onder-lijst` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-structveld-payload` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-sumtype-payload` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-null-vouwoperand` | WF-034 (sentinel fence) | B13 | verwijderd-met-reden | a null type on a listed operand | a reference without a type is the L2 form; measured: the migration's translation is verifier-clean |
| `wf093-aantalgevulde-wrat` | WF-093 | B13, B16 (+V6) | herschreven (m) | WF-093, a count source under AANTAL_GEVULDE | WF-093 keys on the source itself (D9) instead of the sentinel; the document is the migration's translation |
| `wf093-wratvouw-kale-eerste` | WF-093 | B13, B16 (+V6) | herschreven (m) | WF-093, a count source under EERSTE | as `wf093-aantalgevulde-wrat` |
| `wf093-wratvouw-kale-som` | WF-093 | B13, B16 (+V6) | herschreven (m) | WF-093, a count source under SOM | as `wf093-aantalgevulde-wrat` |
| `wf093-wratvouw-set-maximum` | WF-093 | B13, B11 | verwijderd-met-reden | the set spelling of the count source | without the sentinel and the set reading the source is an ordinary value fold's set source; measured: the migration's translation is verifier-clean |
| `wf093-wratvouw-set-som` | WF-093 | B13, B11 | verwijderd-met-reden | the set spelling of the count source | as the row above, under SOM; measured: the migration's translation is verifier-clean |
| `wf034-feitcreatie-hoofdletterveld` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf034-feitcreatie-loos-tussenstapveld` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf034-feitcreatie-onbekend-joinveld` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf034-feitcreatie-onbekend-tussenstaptype` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf034-feitcreatie-onbekend-tussenstapveld` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf034-feitcreatie-onbekend-zetveld` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf034-feitcreatie-zusterrol` | WF-034 (m) | B7 | verwijderd-met-reden | a bean field of a fact creation | B7 takes the bean fields off the fact creation: the symbolic block is the whole creation; the seven translations are byte-identical and the migration's translation is verifier-clean |
| `wf028-bean-only-creation` | WF-028 | B7 | verwijderd-met-reden | a creation with bean fields only | a creation without its symbolic block cannot be written (m2.py: "the symbolic creation block is incomplete") |
| `wf028-create-along-final-hop` | WF-028 | B7 | herschreven (m) | the created relation equals the final hop's | the symbolic clause stays; the bean fields left |
| `wf028-half-symbolic-creation` | WF-028 | B7 | verwijderd-met-reden | a half-symbolic creation | `doelRol` and `joinRol` are required (m2.py: "the symbolic creation block is incomplete") |
| `wf053-bindingless-ambiguous` | WF-053 | B11 | verwijderd-met-reden | the written-name arm (an ambiguous written hop) | the written hop name left the wire and nothing derives a binding; the migration refuses the two-candidate derivation (m2.py: "derives no unique binding") |
| `wf053-chain-near-side-mismatch` | WF-053 | B11 | herschreven (m) | bound chain, near side | the written-step lines left with the written names; the near-side and endpoint lines remain |
| `wf053-dangling-navigation-binding` | WF-053 | B11 | herschreven (m) | bound hop, undeclared fact type | unchanged but for the written hop name |
| `wf053-geleste-vouwoperand-onverbonden-hop` | WF-053 | B11 (+B12) | herschreven (i) | WF-053 unbound read with a foreign objecttype (D10) | no fact type relates the two types, so the migration refuses; written by hand as the read without bindings whose objecttype is not the subject type |
| `wf053-to-many-hop-in-scalar` | WF-053 | B11 | herschreven (m) | to-many hop in a scalar position (the whole rule value) | the written plural left; the migration derived the binding, and the to-many hop remains |
| `wf053-written-role-mismatch` | WF-053 | B11 | verwijderd-met-reden | written step against the bound role | there is no written step in L2; measured: the migration's translation is verifier-clean |
| `wf053-zelfverwijzing-vreemde-rol` | WF-053 | B11 | verwijderd-met-reden | the zero-hop self-reference test | the self-reference exists only as a written step; without it the read is the subject's own read (m2.py refuses the written name: "derives no unique binding") |
| `wf054-binding-on-unscoped` | WF-054 | B8 | verwijderd-met-reden | a scope binding on an unscoped uniqueness | the scope binding IS the scoping, so this shape cannot be written (m2.py: "a scope binding on an unscoped uniqueness") |
| `wf054-dangling-scope-binding` | WF-054 | B8 | herschreven (m) | bound scope, undeclared fact type | the written scope field left; the binding is the scope |
| `wf054-to-many-scope-role` | WF-054 | B8 | herschreven (m) | to-many scope role | the bindingless scope became a `scopeRol` derived from the written name; the to-many role remains |
| `wf096-aantal-setbron` → `wf093-telling-over-attribuutbron` | WF-096 → WF-093 | B11 (+V6) | vervangen (WF-093, D9) | WF-093, AANTAL over a source that is no count source | WF-096 retired with the set reading; its count half went into WF-093, which since L2 demands a count source under AANTAL (D9) |
| `wf096-aantal-setbron-bronpad` | WF-096 | B11 (+V6) | verwijderd-met-reden | the set reading under AANTAL, multi-step range | measured: the translation falls on WF-093 like the successor of `wf096-aantal-setbron`; the path length does not reach that rule |
| `wf096-aantal-setbron-wrat` | WF-096 | B11, B13 | verwijderd-met-reden | the set spelling of the count source under AANTAL | measured: the translation is a count source anchored on the element type, WF-083 (c) — the shape of `wf083-vouwbron-anker-op-operand`, rewritten in place |
| `wf096-setlezing-geleste-operand` → `wf053-to-many-lijstvouwoperand` | WF-096 → WF-053 | B11 | vervangen (WF-053) | WF-053 to-many hop in a listed operand | the set reading left; the navigated read with its to-many hop remains (U8 name) |
| `wf096-setlezing-operand` → `wf053-to-many-plusoperand` | WF-096 → WF-053 | B11 | vervangen (WF-053) | WF-053 to-many hop in an operand of PLUS | as the row above (U8 name) |
| `wf096-setlezing-regelwaarde` → `wf053-to-many-regelwaarde` | WF-096 → WF-053 | B11 | herschreven op WF-053 | WF-053 to-many hop in the whole rule value | the set reading left; measured: WF-053, the position `wf053-to-many-hop-in-scalar` also pins (D17) |
| `wf034-rolcheck-loze-andertefiller` | WF-034 (o) | B9 | verwijderd-met-reden | a role check without its filler type | the merged node names its fact type and role, both required: a check without them cannot be written (m2.py: "a role check without anderTeFiller") |
| `wf034-rolcheck-onbekend-andertefiller` → `wf034-rolcheck-onbekend-feittype` | WF-034 (o) → WF-034 (role arm) | B9 | vervangen (WF-034, role arm) | WF-034 role arm: the fact type is not declared | the filler derivation left; the successor names an undeclared fact type in `RoleMembership` (U8 name) |
| `wf034-verre-rolcheck-verkeerde-zijde` | WF-034 (t) → WF-034 (role arm) | B9 | herschreven (i) | WF-034 role arm: the subject does not fill the role | the same mistake in the merged form, written by hand (the migration finds no candidate): the red-first proof V4 (b) asks |
| `wf048-ambiguous-connecting-relation` | WF-048 | B12 | verwijderd-met-reden | an allocation without a derivable relation | WF-048 retired: the allocation carries its recipient role (m2.py: "does not derive one") |
| `wf048-telling-meerduidig` | WF-048 | B12 | verwijderd-met-reden | a count without a derivable range | WF-048 retired: a fold with an elementObjecttype carries its range (m2.py: "does not derive one") |
| `wf003-unresolved-domain` | WF-003 | B16 | herschreven (i) | WF-003, a declaration naming an undeclared domain | B16 takes the unknown-type node out of the datatype union; written by hand as the enumeration naming `Bedragen` (D15), compared for equality |
| `wf034-ambigu-typenpaar` | WF-034 | B12 (L2) | verwijderd-met-reden | the type-pair fallback of a bindingless count | a fold with an elementObjecttype carries its range, so the fallback does not exist (m2.py: "does not derive one") |
| `wf034-attribuutref-zonder-attribuut` | WF-034 | — | herschreven (m) | `attribuut` null | no L2 delta |
| `wf034-attribuutref-zonder-objecttype` | WF-034 | — | herschreven (m) | `objecttype` null | no L2 delta |
| `wf034-dangling-parameter` | WF-034 | — | herschreven (m) | an undeclared parameter | no L2 delta |
| `wf034-kenmerkcheck-zonder-kenmerk` | WF-034 | — | herschreven (m) | `kenmerk` null | no L2 delta |
| `wf034-kwantorvouw-filter-onderwerpkenmerk` | WF-034 | — | herschreven (m) | a characteristic of the subject read on the element | no L2 delta |
| `wf034-objectcreatie-loos-subject` | WF-034 | — | herschreven (m) | `subject` null | no L2 delta |
| `wf034-objectcreatie-onbekend-subject` | WF-034 | — | herschreven (m) | an undeclared subject | no L2 delta |
| `wf034-parameterref-zonder-parameter` | WF-034 | — | herschreven (m) | `parameter` null | no L2 delta |
| `wf034-dimensiecel-geen-referentie` | WF-034 | B13 (L2) | herschreven (m) | a cell read whose attribute position is no reference | the cell lost its own type; the literal in the attribute position remains |
| `wf034-dimensiecel-zonder-attribuut` | WF-034 | B13 (L2) | herschreven (m) | a cell read without an attribute | the cell lost its own type |
| `wf034-foldbron-onbekend-attribuut` | WF-034 | B12, B11 (L2) | herschreven (m) | an unresolved attribute at a value fold's source | the migration wrote the range; the unknown name remains |
| `wf034-foldbron-zonder-attribuut` | WF-034 | B13 (L2) | herschreven (m) | a fold source without an attribute | the WF-091 line of the source's own type left with it |
| `wf034-foldbron-zonder-objecttype` | WF-034 | B13 (L2) | herschreven (m) | a fold source without an object type | as the row above |
| `wf034-neerwaartse-lezing` | WF-034 | B11 (L2) | herschreven (m) | a supertype read of a subtype attribute at a fold source | the set reading left; the resolution stays |
| `wf034-onbekend-regeldoel` | WF-034 | B13 (L2) | verwijderd-met-reden | the unknown-type node on a rule target | the target lost its own type; measured: the migration's translation is verifier-clean |
| `wf034-regeldoel-zonder-type` | WF-034 | B13 (L2) | verwijderd-met-reden | a rule target without a type | a target without a type is the L2 form; measured: the migration's translation is verifier-clean (byte-identical to the row above) |
| `wf034-verdeling-lege-bronattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-lege-groepeerattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-lege-maximumattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-onbekend-bronattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-onbekend-doelattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-onbekend-groepeerattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-onbekend-maximumattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-onbekend-ratoattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-onbekend-restantattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-zonder-bronattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-zonder-brontype` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-zonder-doelattribuut` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-verdeling-zonder-doeltype` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-zelftype-typenpaar` | WF-034 | B12 (L2) | verwijderd-met-reden | the type-pair fallback over a self-typed pair | as `wf034-ambigu-typenpaar` |

#### Measured beyond the card's stamp list

The migration refused 32 documents; the stamp list covered some of them, and
the rest were measured here. So was every witness whose verifier report changed
between the L1 and the L2 form of the corpus.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf018-parameter-onbekend` | WF-018 | B16 | herschreven (i) | WF-018, a parameter naming an undeclared domain | as `wf003-unresolved-domain`, for a parameter (D15): written by hand as the enumeration naming `Drempels`, compared for equality |
| `wf020-crossform-collision` | WF-020 | B12 | herschreven (i) | WF-020, one name shared by a rule and an allocation (names are unique across all named forms) | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf024-complex` | WF-024 | B12 | herschreven (i) | WF-024, the allocation method COMPLEX | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf025-max-without-ratio` | WF-025 | B12 | herschreven (i) | WF-025, a maximum without a ratio attribute | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf026-two-allocations-one-target` | WF-026 | B12 | herschreven (i) | WF-026, two allocations writing one target | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf031-dagsoort-leest-rolstaat` | WF-031 | B9 | herschreven (i) | unchanged (role membership read in a day-kind condition) | a day-kind condition has no subject to derive the role check's fact type from; written by hand as `RoleMembership` naming it |
| `wf035-power-with-unit` | WF-035 | B13 | herschreven (i) | unchanged (a power over a unit-carrying base) | the unit sat on the reference's own type; measured, the translation was verifier-clean; written by hand with the unit on the attribute's declaration |
| `wf036-order-number-vs-text` | WF-036 | B13 | herschreven (i) | unchanged (an ordering operand that is text) | the text type sat on the reference's own type; measured, the translation was verifier-clean; written by hand with the parameter declared as text |
| `wf037-aantalgevulde-niet-setbron` | WF-037 | B11, B12 | herschreven (i) | the filled count's source is no attribute reference | the set-source demand is enforced at load since L2 (U2); the card expected a rewrite without range and elementObjecttype, measured a duplicate of `wf037-aantalgevulde-zonder-elementtype`'s arm, so the witness pins the arm that has none: a range beside a literal source |
| `wf072-expressietype-volgorde` | WF-072 | B13 | herschreven (i) | unchanged (a drifted struct copy at the expression layer) | the drift sat on a reference's own type; measured, the translation was verifier-clean; written by hand with the drift on a `StructLiteral`'s type |
| `wf083-fold-source-navigates-away` | WF-083 (b) | B11 | verwijderd-met-reden | a written role on a fold source | the written role has no L2 carrier (m2.py: "a written role on a fold source ... has no carrier") |
| `wf083-vouwbron-anker-op-operand` | WF-083 (c) | B11 | herschreven (i) | anchor of a count source (the fold subject) | measured (the card expected verwijderd-met-reden): without its written role the document is a count source anchored on the element type, which WF-083 (c) refuses per position (D8); the witness of that position |
| `wf083-vouwbron-navigatierol-buiten-de-wrat` | WF-083 (b) | B11 | verwijderd-met-reden | a written role on a set source | as `wf083-fold-source-navigates-away` |
| `wf083-vouwbron-anker-op-tussentype` | WF-083 (c) | B11, B13 | herschreven (m) | anchor of a set source (an intermediate path type) | the set reading left; the range makes the argument the set source (D8 (i)) |
| `wf083-vouwbron-vreemd-elementtype` | WF-083 (c) | B11, B13 | herschreven (m) | anchor of a set source (the fold subject) | as the row above |
| `wf083-vouwbron-vreemd-objecttype` | WF-083 (c) | B13 | herschreven (m) | anchor of the subject's own read (another type) | a TOTAAL without a range is the subject's own read (D8 (iv)) |
| `wf102-bronpad-naast-bronrol` | WF-102 (a) | B12 | verwijderd-met-reden | bronPad beside bronRol | one range per fold is the schema's and the loader's since L2 (U2) (m2.py: "bronPad beside bronRol") |
| `wf102-bronpad-op-niet-setbron` | WF-102 (c) | B13, B11 | herschreven (m) | WF-102 (c), a `bronPad` on a count source (a count's range is one step) | the count source lost its sentinel type (B13) and is recognised at the source itself (D9); the clause's wording and its message moved with the set reading: the admitted position is a VALUE fold's set source. The row and `expect.txt` were added in the L2 fix round; the union had changed the message without a row |
| `wf108-kwantorvouw-bereik-dubbel` | WF-108 (c) | B12 | verwijderd-met-reden | bronPad beside bronRol on a quantifier fold | as the row above, on a quantifier fold |
| `wf110-unie-naast-vouwbron` | WF-110 (c) | B12 | herschreven (i) | unchanged (the count's own source beside a union) | a single-argument count with an elementObjecttype carries a range (U2), and nothing derives one beside a union; written by hand with the first selection's relation as `bronRol` |
| `wf110-unie-tak-zonder-bronrol` | WF-110 (e) | B12 | verwijderd-met-reden | a union selection without its relation | `sourceRole` is required and non-null (D5 (c)) (m2.py: "a union selection without its relation") |
| `wf007-doeltype-geheel-met-decimalen` | WF-007 | B13 | verwijderd-met-reden | the rule-target arm | the rule target lost its own type, the one position of this arm; measured: the migration's translation is verifier-clean |
| `wf023-schrijf-doeltype-leugen` | WF-023 | B13 | verwijderd-met-reden | the target's declared type against its own lying type | the target lost its own type; measured: the translation is byte-identical to `wf023-schrijf-enum-domein` and falls on the same WF-023 line |
| `wf034-foldbron-rolnaam-waardevouw` → `wf093-telbron-onder-som` | WF-034 → WF-093 | B13 | herschreven op WF-093 | WF-093, a count source under SOM | measured: WF-093 recognises the count source at the source (D9), so the role name under SOM falls there (D17) |
| `wf046-kwantorvouw-bindingloos-bereik` → `wf046-kwantorvouw-bereik-ander-type` | WF-046 | B12 | herschreven (m) | WF-046, the range of an ALLE quantifier fold over the relation a recursive creation of ANOTHER type writes (a Post for a Wachter) | the loader refuses a quantifier fold without `bronRol` or `bronPad` (B12, U2), so the bindingless form cannot be written; the migration derives the range `bemanning`/`post`, and the translation fails WF-046 through the bound range. The L2 union first removed it as a duplicate of `wf046-kwantorvouw-alle-bereik`, which mints a Wachter for a Wachter; the removal criterion (the migration refuses the document, or its translation is verifier-clean) was not met, so the L2 fix round restored the translation, byte for byte, under a name without "bindingloos" |

Seven witnesses kept their rule and their arm while their report lost
incidental lines to B13 — a reference's own `type` that crossed nothing any more.
Three of them carry a stamp row because their NOTE records the migration:
`wf088-functiondef-drift` (in the WF-088 rows above) and the two WF-092 rows
below. The other four carry no stamp: `wf007-whole-with-decimals`,
`wf027-lying-attribuutref-type`, `wf034-eenheid-onbekend-stelsel` and
`wf040-unpinned-cell`. Where a NOTE cited a lost line or described the lost
type as present, the citation was brought to the current report — for
`wf027-lying-attribuutref-type` and `wf034-eenheid-onbekend-stelsel` in the L2
fix round.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf092-factor-zonder-canoniek` | WF-092 | — | herschreven (m) | a unit with a factor and no canonical unit | no L2 delta on the defect; the rule target and the attribute reference lost their own `type` (B13), and with them two of the five report lines |
| `wf092-noemer-zonder-factor` | WF-092 | — | herschreven (m) | a denominator unit with a canonical unit and no factor | no L2 delta on the defect; the rule targets and the fold sources lost their own `type` (B13), and with them five of the eleven report lines |

#### Added in the L2 union and the L2 fix round (2026-09-24)

Witnesses that no stamp produced: the U12 witness of the union, and the five
the lens on the union asked for (`expect.txt` in each).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf034-rollidmaatschap-vreemde-vullersklasse` | WF-034 | B9 (U12) | toegevoegd | role arm of `RoleMembership`: `counterpartType` is no subtype of the other role's filler | `counterpartType` carries the narrowing of the far role check that the first L2 form lost (U12); `corpus/golden/jutterswet-postbezetting` with a declared type outside the filler's chain |
| `wf034-rollidmaatschap-rol-als-meervoud` | WF-034 | B9 | toegevoegd | role arm of `RoleMembership`: `role` is no declared role name of `factType` | `role` spelled as the declared plural of the other role; the arm had no witness (lens on the union) |
| `wf034-rollidmaatschap-vullersklasse-onbekend` | WF-034 | B9 (U12) | toegevoegd | role arm of `RoleMembership`: `counterpartType` is no declared object type | the arm had no witness (lens on the union) |
| `wf053-eigen-lezing-supertype` | WF-053 | B11 | toegevoegd | a read without navigation bindings whose `objecttype` is a supertype of the subject | the own read names the subject type itself (equality, IR-SPEC ch. 4.1a); on the union `ref` and the ASP backend admitted the supertype |
| `wf083-totaal-vreemd-elementtype` | WF-083 (c) | B12 | toegevoegd | the `elementObjecttype` of a `TOTAAL` without a range is not the fold subject | new arm of the fix round; before it the field was verifier-clean and moved no answer |
| `wf091-plus-eenheid-kast` | WF-091 | B13 | toegevoegd | derived arm, PLUS, unit facet (unit names differ in case alone) | the byte-exact unit names lost their witness with `wf091-attribuutref-eenheid-kast` (removed above); the lie now sits on a composite's annotation |

### L3 — the type system (2026-09-24)

L3 (card `20260924-l3-typesysteem`, `backlog/done/20260924-l3-typesysteem.md`;
the decision numbers D# and U# below are that card's decision list) takes the
type system to its v1 form: a percentage is a `Number` whose unit is `%`, with
the presentation tag only on a declaration (B14, D22, D23); the two date nodes
are one `Date` with a `precision` (B15); a unit's conversion factor is the exact
rational `{num, den}` (B17 (i), D25); a parameter's time-dependence is a
timeline facet (B18); a unit system is a list of units that carry their further
spellings (D54); and a day kind is a node with its name and its plural (D55).
Every unit took the IR-SPEC keys (U4), every number the keys `whole`,
`decimals`, `sign` and `unit` (U5), and a number declared without a sign writes
`ONBEPAALD` (D24). Every witness was measured before its stamp: the L2 verifier
on the document's L2 form against the L3 verifier on its L3 form, the
translation of `spec/proposals/v1-migratie/m3.py`. On every document of the
three zones the two reports agree but for their paths and, in eight messages,
for the name of the date type — the six `wf095-*` witnesses and the two
`wf111-datumtijdgrens-*` witnesses name a `millisecond-precision date` where
the L2 message wrote `datum-tijd` —, except on the four the migration refuses
(removed below) and on `wf017-dagsoortnaam-bindt-vreemde-dagsoort`, whose arm
now reads the day kinds' own spellings (measured again in the L3 fix round: these
nine are the only reports whose text moved). A rewritten witness of the
card's stamp list carries `expect.txt`, one literal line of the verifier
message of the arm it pins, without a JSON pointer; so does every date witness
whose rule reads the precision, and every witness added here. The arm column
names that arm in words, or, for a removed witness, the arm that left. The
negatief zone went from 400 to 402 documents (four removed, six added); the
floor `EXPECTED_MIN_NEGATIEF` did not have to move, and `EXPECTED_MIN_EXPECT`
rose from 91 to 159 (`scripts/check-verifier.sh`).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf017-eenheidsleutel-schaduwt-kalenderafkorting` | WF-017 (a) | D55 | verwijderd-met-reden | a registry key that is a calendar spelling (the abbreviation `wk`) | the key is a third spelling of `schouwdag` beside its name and its plural `schouwdagen`, and a `DayKind` carries its name and one plural (m3.py: "a third spelling of the day kind "schouwdag" beside its name and the plural "schouwdagen": a DayKind carries its name and one plural"); the arm now reads the `plural`, where `wf017-meervoud-schaduwt-kalenderafkorting` pins the abbreviation class (added in the L3 fix round, below) |
| `wf017-eenheidsleutel-schaduwt-kalendereenheid` | WF-017 (a) | D55 | verwijderd-met-reden | a registry key that is a calendar spelling (the plural `dagen`) | idem; the class is pinned on the `plural` by `wf017-meervoud-schaduwt-kalendereenheid` |
| `wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm` | WF-017 (a) | D55 | verwijderd-met-reden | a registry key that is a calendar spelling (the second plural `secondes`) | idem; the class is pinned on the `plural` by `wf017-meervoud-schaduwt-tweede-meervoudsvorm` |
| `wf034-dagsoorteenheid-onbekende-dagsoort` | WF-034 (s) | D55 | verwijderd-met-reden | a registry value naming an undeclared day kind (the arm left with the registry) | a spelling stands on the day kind it belongs to or not at all, so there is no value to name an undeclared day kind (m3.py: "the registry entry names the day kind "havendag", which no declaration carries: the L3 shape has no registry, so a spelling is written on its own day kind or not at all"); arm (s) of WF-034 loses its object and becomes a history line in `spec/IR-SPEC.md` ch. 4.6 |
| `wf092-canoniek-zonder-factor` | WF-092 (a) | B17 (i), U4 | herschreven (m) | a canonical unit without a factor | the conversion pair is spelled `canonical`/`factorToCanonical`, the missing half absent where it was null; the report moved only in its paths |
| `wf092-factor-zonder-canoniek` | WF-092 (b) | B17 (i), U4 | herschreven (m) | a factor without a canonical unit | as the row above; the factor is `{num: 1, den: 1}` |
| `wf092-noemer-zonder-factor` | WF-092 (a) | B17 (i), U4, D54 | herschreven (m) | a canonical unit without a factor, on the denominator of a rate | as the rows above; the denominator is spelled `denominator`. The unit system carries a map key other than the abbreviation, now a spelling (D54, not on the defect) |
| `wf017-dagsoortnaam-bindt-vreemde-dagsoort` | WF-017 (b) | D55 | herschreven (m) | arm (b): the plural of one day kind is the name of another | measured: the one witness whose arm moved (eight others changed only the name of the date type, see above). Until L3 the arm reported the registry key of a declared name bound to another day kind; the day kinds now carry their own spellings, and the arm reports the plural that is another day kind's name |
| `wf017-dagsoortnaam-schaduwt-kalenderafkorting` | WF-017 | D55 | herschreven (m) | the declared-name clause: a name that is a calendar spelling (the abbreviation `wk`) | the name is the `DayKind`'s `name`; the report moved only in its path (`/model/dayKinds/2/name`) |
| `wf017-daykind-shadows-builtin` | WF-017 | D55 | herschreven (m) | the declared-name clause (the singular `dag`) | as the row above (`/model/dayKinds/0/name`) |
| `wf095-bronsnippet` | WF-095 | B15 | herschreven (m) | the lexeme of a millisecond-precision date literal (a day-first snippet) | the literal's type is `Date` with `precision: millisecond`, which is what makes WF-095 its rule; the message names the date by its precision |
| `wf095-fractiebreedte` | WF-095 | B15 | herschreven (m) | idem (a fraction of one digit) | as the row above |
| `wf095-omringende-tekst` | WF-095 | B15 | herschreven (m) | idem (text around the lexeme) | as the row above |
| `wf095-ontbrekende-seconden` | WF-095 | B15 | herschreven (m) | idem (no seconds field) | as the row above |
| `wf095-spatie-scheider` | WF-095 | B15 | herschreven (m) | idem (a space for `T`) | as the row above |
| `wf095-zone-suffix` | WF-095 | B15 | herschreven (m) | idem (a zone suffix) | as the row above |
| `wf034-eenheid-onbekend-stelsel` | WF-034 (r) | U4 | herschreven (m) | a unit's `system` naming no declared unit system | the field is spelled `system`; the report moved only in its paths |
| `wf009-conversion-cycle` | WF-009 | D25, U4 | herschreven (m) | a conversion cycle | the factors 2.0 and 0.5 are the exact rationals `{2, 1}` and `{1, 2}`; the report moved only in its paths (`/model/unitSystems/0/units/<i>`) |

The WF-091 family carries `expect.txt` since L2, and every fragment holds
unchanged. Its unit-facet arm compares unit records, which L3 respelled (U4);
fourteen of the fifteen hosts carry a unit-less percentage that became a
`Number` with the built-in `%` (D23), the card's percentage carriers, and in
none of them is it the defect.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf091-afgerond-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, rounding, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-begrensd-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, clamp, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binair-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, PLUS, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-foldwortel-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, MAXIMUM, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-som-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, SOM, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-plus-eenheid-kast` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, PLUS, unit facet (unit names differ in case alone) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-maal-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, MAAL, unit facet (a rate against a plain unit) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-totaal-eenheid` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, TOTAAL, unit facet (a rate against a plain unit) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-aantalgevulde-eenheid` | WF-091 | U4, D54 | herschreven (m) | derived arm, AANTAL_GEVULDE, unit facet | the unit record the arm compares is spelled with the L3 keys (U4); the unit system carries spellings (D54), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binair-soort` | WF-091 | B14, D23 | herschreven (m) | derived arm, PLUS, soort facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-aantal-decimalen` | WF-091 | B14, D23 | herschreven (m) | derived arm, AANTAL, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-afgerond-decimalen` | WF-091 | B14, D23 | herschreven (m) | derived arm, rounding, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binair-decimalen` | WF-091 | B14, D23 | herschreven (m) | derived arm, PLUS, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binair-geheel` | WF-091 | B14, D23 | herschreven (m) | derived arm, PLUS, wholeness facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-fold-decimalen` | WF-091 | B14, D23 | herschreven (m) | derived arm, MINIMUM, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |

The card's group rows — a sign written where none was (D24), a parameter's
timeline facet (B18), the day kinds' plurals (D55), a unit system's spellings
(D54) — rewrote each document at a place that is not its defect. Each keeps its
report, and pins its arm now.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf035-duur-min-datum` | WF-035 | D24 | herschreven (m) | duration − date | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf035-duur-plus-datum` | WF-035 | D24 | herschreven (m) | duration + date | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf038-extractor-text` | WF-038 | D24 | herschreven (m) | a day-precision extractor over text | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf038-tijdsduur-zonder-node-type` | WF-038 | D24 | herschreven (m) | a tijdsduur without a node type | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf038-tijdsduur-zonder-tijdseenheid` | WF-038 | D24 | herschreven (m) | a tijdsduur without a recognised time unit | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf049-read-of-nontemporal` | WF-049 | B18 | herschreven (m) | a temporal read of a non-temporal attribute | no L3 delta on the defect: the host's time-dependent parameter carries a timeline facet |
| `wf049-type-not-element` | WF-049 | B18 | herschreven (m) | a temporal read typed other than the element datatype | no L3 delta on the defect: the host's time-dependent parameter carries a timeline facet |
| `wf098-contextwaarde-naam-kapitalisatie` | WF-098 | B18 | herschreven (m) | a context-value name spelled in another case | no L3 delta on the defect: the host's time-dependent parameter carries a timeline facet |
| `wf031-sessiewaarde-in-dagsoort` | WF-031 | D55 | herschreven (m) | a day-kind condition reading a session value | no L3 delta on the defect: the host's day kinds carry their plurals |
| `wf098-onbekende-contextwaarde-naam` | WF-098 | D55 | herschreven (m) | an unknown context-value name | no L3 delta on the defect: the host's day kinds carry their plurals |
| `wf034-attribuutref-zonder-attribuut` | WF-034 | D54 | herschreven (m) | an attribute reference without an attribute name | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-attribuutref-zonder-objecttype` | WF-034 | D54 | herschreven (m) | an attribute reference without an object type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-dimensiecel-geen-referentie` | WF-034 | D54 | herschreven (m) | a dimension-cell read whose attribute position is no attribute reference | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-dimensiecel-zonder-attribuut` | WF-034 | D54 | herschreven (m) | a dimension-cell read without an attribute reference | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-foldbron-zonder-attribuut` | WF-034 | D54 | herschreven (m) | a fold source without an attribute name | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-foldbron-zonder-objecttype` | WF-034 | D54 | herschreven (m) | a fold source without an object type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-parameterref-zonder-parameter` | WF-034 | D54 | herschreven (m) | a parameter reference without a parameter name | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-sentinel-builtin-wratoperand` | WF-034 | D54 | herschreven (m) | a name that is neither an attribute nor a characteristic (an operand of a builtin) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-sentinel-gelest-wratoperand` | WF-034 | D54 | herschreven (m) | idem (an operand read on the subject) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-lege-maximumattribuut` | WF-034 | D54 | herschreven (m) | an allocation's empty `maximumAttribuut` | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-onbekend-bronattribuut` | WF-034 | D54 | herschreven (m) | an allocation's `bronAttribuut` naming no attribute of the source type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-onbekend-doelattribuut` | WF-034 | D54 | herschreven (m) | an allocation's `doelAttribuut` naming no attribute of the target type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-onbekend-maximumattribuut` | WF-034 | D54 | herschreven (m) | an allocation's `maximumAttribuut` naming no attribute of the target type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-onbekend-restantattribuut` | WF-034 | D54 | herschreven (m) | an allocation's `restantAttribuut` naming no attribute of the source type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-zonder-brontype` | WF-034 | D54 | herschreven (m) | an allocation without a source type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-verdeling-zonder-doeltype` | WF-034 | D54 | herschreven (m) | an allocation without a target type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantal-geen-operand` | WF-037 | D54 | herschreven (m) | a count without an element type (no argument) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantal-lijstliteraal` | WF-037 | D54 | herschreven (m) | a count without an element type (a list-literal argument) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantal-opgesomd` | WF-037 | D54 | herschreven (m) | a count in a listed form (two operands) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantal-opgesomd-drietal` | WF-037 | D54 | herschreven (m) | a count in a listed form (three operands) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantal-opgesomd-element` | WF-037 | D54 | herschreven (m) | a count in a listed form (two operands and an element type) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantal-zonder-elementtype` | WF-037 | D54 | herschreven (m) | a count without an element type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-dimensieselectie` | WF-037 | D54 | herschreven (m) | a dimension selection on the filled count | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-granulariteit` | WF-037 | D54 | herschreven (m) | a granularity facet on the filled count | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-leegwaarde` | WF-037 | D54 | herschreven (m) | an empty-value fallback on the filled count | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-niet-setbron` | WF-037 | D54 | herschreven (m) | the filled count's argument is no set source | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-tweede-argument` | WF-037 | D54 | herschreven (m) | the filled count with a second argument | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-zonder-elementtype` | WF-037 | D54 | herschreven (m) | the filled count without an element type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf047-bereik-op-builtin` | WF-047 | D54 | herschreven (m) | a fold-source role binding on a node that is no instance fold | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf047-bronpad-stap-onopgeloste-rol` | WF-047 | D54 | herschreven (m) | a `bronPad` step naming a plural as its role | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-eigen-lezing-supertype` | WF-053 | D54 | herschreven (m) | an own read whose `objecttype` is a supertype of the subject | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-geleste-vouwoperand-onverbonden-hop` | WF-053 | D54 | herschreven (m) | an own read whose `objecttype` is not the subject (a fold operand) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-to-many-lijstvouwoperand` | WF-053 | D54 | herschreven (m) | a to-many hop in a scalar slot (a list-fold operand) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-to-many-plusoperand` | WF-053 | D54 | herschreven (m) | a to-many hop in a scalar slot (a PLUS operand) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-to-many-regelwaarde` | WF-053 | D54 | herschreven (m) | a to-many hop in a scalar slot (a rule value) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-totaal-vreemd-elementtype` | WF-083 (c) | D54 | herschreven (m) | the `elementObjecttype` of a TOTAAL without a range is not the fold subject | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-vouwbron-anker-op-operand` | WF-083 (c) | D54 | herschreven (m) | anchor of a count source (the fold subject) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-vouwbron-anker-op-tussentype` | WF-083 (c) | D54 | herschreven (m) | anchor of a set source (an intermediate path type) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-vouwbron-vreemd-elementtype` | WF-083 (c) | D54 | herschreven (m) | anchor of a set source (the fold subject) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-vouwbron-vreemd-objecttype` | WF-083 (c) | D54 | herschreven (m) | anchor of the subject's own read (another type) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-aantalgevulde-wrat` | WF-093 | D54 | herschreven (m) | a value fold over a role name (the filled count) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-telbron-onder-som` | WF-093 | D54 | herschreven (m) | a count source under SOM | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-telling-over-attribuutbron` | WF-093 | D54 | herschreven (m) | a count over an attribute source | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-wratvouw-kale-eerste` | WF-093 | D54 | herschreven (m) | a value fold over a role name (EERSTE) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-wratvouw-kale-som` | WF-093 | D54 | herschreven (m) | a value fold over a role name (SOM) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-bronpad-een-stap` | WF-102 | D54 | herschreven (m) | a `bronPad` of one step | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-bronpad-landt-naast-elementtype` | WF-102 | D54 | herschreven (m) | a `bronPad` that does not land on the element type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-bronpad-leeg` | WF-102 | D54 | herschreven (m) | an empty `bronPad` | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-bronpad-onverbonden-stap` | WF-102 | D54 | herschreven (m) | a `bronPad` step that does not connect | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-bronpad-op-niet-setbron` | WF-102 (c) | D54 | herschreven (m) | a `bronPad` on a count source | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-bronpad-start-niet-op-onderwerp` | WF-102 | D54 | herschreven (m) | a `bronPad` that does not start at the fold subject | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |

#### Measured beyond the card's stamp list

The date witnesses whose rule reads the precision, which the card's WF-036
line names without a list, and the witnesses of two rules that gained a
sibling arm at L3, so that a fragment tells the arms apart.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf035-uur-op-dagdatum` | WF-035 | B15, D24 | herschreven (m) | a sub-day time unit on a day-precision date | the rule reads the date's precision, a field of `Date` since L3; the sign written where none was (D24) is not on the defect; the report is unchanged |
| `wf036-datumordening-precisie` | WF-036 | B15 | herschreven (m) | an ordering of two date precisions | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf038-eerste-gemengde-datumprecisie` | WF-038 | B15 | herschreven (m) | an ordering fold over two date precisions | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf038-tijdsduur-gemengde-precisie` | WF-038 | B15, D24 | herschreven (m) | a tijdsduur over two date precisions | the rule reads the date's precision, a field of `Date` since L3; the sign written where none was (D24) is not on the defect; the report is unchanged |
| `wf049-read-at-datetime` | WF-049 | B15, B18 | herschreven (m) | a temporal read at a millisecond-precision date | the rule reads the date's precision, a field of `Date` since L3; the host parameter's facet (B18) is not on the defect; the report is unchanged |
| `wf084-bare-non-iso` | WF-084 | B15 | herschreven (m) | the lexeme of a day-precision date literal (day-first) | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf084-source-snippet` | WF-084 | B15 | herschreven (m) | idem (a source snippet) | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf111-datumtijdgrens-attribuut` | WF-111 | B15 | herschreven (m) | a millisecond-precision window bound (an attribute) | the rule reads the date's precision, a field of `Date` since L3; the message names the bound a `millisecond-precision date` where L2 wrote `datum-tijd`, and the arm is unchanged |
| `wf111-datumtijdgrens-literal` | WF-111 | B15 | herschreven (m) | a millisecond-precision window bound (a literal) | the rule reads the date's precision, a field of `Date` since L3; the message names the bound a `millisecond-precision date` where L2 wrote `datum-tijd`, and the arm is unchanged |
| `wf007-whole-with-decimals` | WF-007 | U5 | herschreven (m) | the precision forms (`whole` with decimals) | WF-007 gained the presentation arm; the precision fields are spelled `whole`/`decimals`, and the witness pins the arm it always held |
| `wf005-count-zero` | WF-005 | — | herschreven (m) | an attribute's facet with count 0 | WF-005 gained the parameter-facet arm; the witness pins the attribute arm it always held |

#### Added at L3 (2026-09-24)

A witness for each arm L3 added or moved onto a new field (`expect.txt` in
each).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf092-factor-niet-in-laagste-termen` | WF-092 (c) | B17 (i) (U1) | toegevoegd | arm (c): a factor not in lowest terms (`{24, 2}` for twelve months) | the new arm: a schema cannot state a greatest common divisor, and WF-091 compares factors byte for byte; `corpus/golden/jutterswet-vergunningsduur` with the factor of `jaar` respelled at its three positions |
| `wf007-presentatietag-op-annotatie` | WF-007 | B14 (U2) | toegevoegd | the presentation tag on a type annotation (a `Literal.type`) | the new arm: the tag belongs to a declared datatype; `corpus/golden/jutterswet-vondsttarief` with the tag on one literal type |
| `wf017-meervoud-schaduwt-kalendereenheid` | WF-017 (a) | D55 (U7) | toegevoegd | arm (a): a plural that is a calendar spelling (`dagen`) | the arm on the `plural`, which the registry-key witnesses above held; `corpus/golden/jutterswet-schouwrooster` with an unused plural respelled |
| `wf017-meervoud-schaduwt-tweede-meervoudsvorm` | WF-017 (a) | D55 (U7) | toegevoegd | idem (the second plural `secondes`) | the second-plural class, which no other witness reaches; as the row above |
| `wf017-meervoud-botst-met-vreemd-meervoud` | WF-017 (b) | D55 (U7) | toegevoegd | arm (b): a plural that is another day kind's plural, differing in case alone | the plural-against-plural collision the registry could not show, and the pin of the ch. 2.1 identity, which left `corpus/golden/jutterswet-vormenkabinet` with its registry entries; as the row above |
| `wf005-parameterfacet-telling-nul` | WF-005 | B18 (U6) | toegevoegd | a parameter's facet with count 0 | the new arm: WF-018 defers a parameter's facet to WF-005; `corpus/golden/anchor/jutterswet-penningvintage` with the count set to 0 |

#### Added and pinned in the L3 fix round (2026-09-24)

The lens on the L3 union found two arms that the list forms of L3 made
necessary and did not have — an abbreviation declared twice in one unit system,
and a parameter's facet with more than one period — and two classes whose
witness had left with the registry or was never written: the abbreviation class
of WF-017 arm (a), and WF-001 in the two namespaces whose nodes L3 rewrote,
whose messages now name the wire lists `unitSystems` and `dayKinds`. Each
witness carries `expect.txt`, and so does the one WF-001 witness that had
none. The negatief zone went from 402 to 407 documents, and
`EXPECTED_MIN_EXPECT` rose from 159 to 165 (`scripts/check-verifier.sh`).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf001-duplicate-parameter` | WF-001 | U6 | herschreven (m) | the `parameters` namespace: two names differing in case alone | the defect sits on the parameter's `name`, spelled `naam` until L3 (U6); the report is unchanged, and `expect.txt` pins it since the fix round |
| `wf001-dubbele-dagsoort` | WF-001 | D55 (U7) | toegevoegd | the `dayKinds` namespace: two day-kind names differing in case alone | the message names the namespace by its wire list `dayKinds` since the fix round (the v0 `dagsoorten` until then); row 3 of the collision table of IR-SPEC ch. 2.9 had no witness; `corpus/golden/jutterswet-schouwrooster` with a third day kind `Schouwdag` |
| `wf001-dubbel-eenheidstelsel` | WF-001 | D54 (U8) | toegevoegd | the `unitSystems` namespace: two unit-system names differing in case alone | the message names the namespace by its wire list `unitSystems` since the fix round (the v0 `eenheidstelsels` until then); `corpus/golden/jutterswet-vangstklem` with a second unit system `gewicht` |
| `wf009-dubbele-afkorting` | WF-009 | D54 (U8) | toegevoegd | one abbreviation declared twice in one unit system | the new arm: the list form can write a second `t`, which the verifier, `ref` and the ASP backend passed while the Drools loader refused the document; `corpus/golden/jutterswet-vangstklem` with a second `t` that converts to `kg` by 100 |
| `wf005-parameterfacet-telling-twee` | WF-005 | B18 (U6) | toegevoegd | a parameter's facet with count 2 | the new arm: a parameter's count is 1 while `spec/RUNTIME-SEMANTICS.md` ch. 3.12 leaves multi-period grids open, because no executor evaluates it; `corpus/golden/anchor/jutterswet-penningvintage` with the count set to 2 |
| `wf017-meervoud-schaduwt-kalenderafkorting` | WF-017 (a) | D55 (U7) | toegevoegd | arm (a): a plural that is a calendar abbreviation (`wk`) | the abbreviation class, whose witness left with `wf017-eenheidsleutel-schaduwt-kalenderafkorting`; `corpus/golden/jutterswet-schouwrooster` with the plural of `stormvloeddag` respelled `wk` |
