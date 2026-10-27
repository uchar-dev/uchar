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
directory may also hold `expect.txt`: the ARM the case pins. Its first line
is the arm key `WF-###.<slug>` of the declared rule and each further line a
`name=value` pair that must be among the parameters the verifier reports for
that key (`verifier --arms`; card `verifier-arm-keys`), so a case that moves to
another arm of its rule turns the gate red too, and a rewording of a message
does not. The rows of the registers below that speak of the fragment of an
`expect.txt` were written when the file held one literal fragment of the
message.

These files are not required to validate against the schema (gate 2
covers `corpus/golden/` only); they must stay *loadable* — a violation
is a semantic defect in a structurally well-formed document, not a
syntax error.

Paths in the notes are JSON pointers into the document, the spelling the
verifier reports (`/model/objectTypes/<i>/...`; a version of a named form is
`/rules/<list>/<f>/versions/<v>/...`, as in
`/rules/rules/0/versions/0/condition/host`). Every citation carries the
verifier's current spelling, quotes of measurements taken on a document's v0
form included: the position cited is the one measured. The quotes that a note
marks as measured on a wire form that has left the wire keep the spelling of
that form.

## Vervallen bij de v1-migratie

A witness whose rule the v1 migration takes away gets one of three stamps
(decision V14 (a), `spec/proposals/V1-WIREMIGRATIE.md`): **herschreven**
(rewritten on the rule that still holds), **vervangen** (replaced by a
successor witness of a successor rule) or **verwijderd-met-reden** (removed,
with the reason). This list names every one of them, per migration step, so a
witness never leaves the corpus unrecorded. The floor `EXPECTED_MIN_NEGATIVE`
in `scripts/check-verifier.sh` is a lower bound and did not have to move.

### M1 — the container (2026-09-23)

Measured before removal: `spec/proposals/v1-migratie/m1.py` refuses each of
the 21 removed documents at exactly the field that made it a witness (the
message is quoted per class below), so none of them has an M1 form that still
carries its defect. Every other negative migrated mechanically and is rejected
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
well) refuses them at load. No golden, red or negative document carried a null
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
The negative zone went from 444 to 394 documents at the L2 union (with the U12
witness `wf034-role-membership-foreign-filler-class` added beside the stamps),
and to 400 in the L2 fix round (the restored `wf046-quantifier-fold-range-other-type`
and the five witnesses the fix round added, listed in the last table beside the
U12 witness); the floor `EXPECTED_MIN_NEGATIVE` did not have to move.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf091-count-decimals` | WF-091 | B13 (+B21, L4) | herschreven (m) | derived arm, AANTAL, decimals facet | a count's annotation is a derived position and keeps its type (V5 (a)); the second operand leaves with the count's operand list at B21 (L4), and until then WF-037 reports beside it |
| `wf091-filled-count-unit` | WF-091 | B13 | herschreven (m) | derived arm, AANTAL_GEVULDE, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-rounding-decimals` | WF-091 | B13 | herschreven (m) | derived arm, rounding, decimals facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-rounding-unit` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, rounding, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-clamp-unit` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, clamp, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-binary-decimals` | WF-091 | B13 | herschreven (m) | derived arm, PLUS, decimals facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-binary-unit` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, PLUS, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-binary-whole` | WF-091 | B13 | herschreven (m) | derived arm, PLUS, wholeness facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-fold-decimals` | WF-091 | B13 | herschreven (m) | derived arm, MINIMUM, decimals facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-fold-maximum-unit` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, MAXIMUM, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-times-unit` | WF-091 | B13 | herschreven (m) | derived arm, MAAL, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-sum-unit` | WF-091 | B13 (+B17, L3) | herschreven (m) | derived arm, SOM, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-total-unit` | WF-091 | B13 | herschreven (m) | derived arm, TOTAAL, unit facet | the operand references lost their own type; the composite's annotation is a derived position and is still crossed against what the operands' declarations derive |
| `wf091-attribuutref-decimalen` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-eenheid-converteerbaar` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-eenheid-kast` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-geheel` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-teken` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (AttributeReference) | the reference's own type left the wire: its type is its declaration's, so the lie cannot be written; measured: the migration's translation is verifier-clean |
| `wf091-attribuutref-eenheid-erbij` → `wf023-unitless-read-in-kg-target` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 unit clause (unitless declaration into a kg target) | reference-type arm gone; measured: the translation falls on WF-023, which had no witness of this clause |
| `wf091-attribuutref-eenheid-ontbreekt` → `wf023-kg-read-in-unitless-target` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 unit clause (kg declaration into a unitless target) | as the row above, the other one-sided direction |
| `wf091-attribuutref-reststructuur` → `wf023-list-read-in-sum-type-target` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 family clause (a list into a sumtype target) | reference-type arm gone; measured: WF-023 (D17; the form is also `wf023-write-list-none-domain-value`'s) |
| `wf091-attribuutref-soort` → `wf023-number-read-in-text-target` | WF-091 → WF-023 | B13 | herschreven op WF-023 | WF-023 family clause (a number into a text target) | reference-type arm gone; measured: WF-023, the mirror of `wf023-text-into-numeric` (D17) |
| `wf091-binary-kind` | WF-091 | B13, B14 (L3) | herschreven (i) | derived arm, PLUS, soort facet | the annotation is rewritten to a boolean (D18), so the soort facet keeps the report when B14 (L3) makes a percentage a number with a unit; WF-023 reports the boolean in the percentage target beside it |
| `wf091-binderref-eenheid` → `wf076-let-unit-against-body` | WF-091 → WF-076 | B13 | herschreven op WF-076 | WF-076 (Let type against body type) | the binder reference lost its own type; measured: the Let's `t` against the body's declared `kg` is WF-076 (D17) |
| `wf091-dimensiecel-eenheid` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (DimensionCellRead) | the cell read's own type left the wire; measured: the migration's translation is verifier-clean |
| `wf091-foldbron-eenheid` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (fold source) | the fold source's own type left the wire; against the declaration the SOM annotation is correct; measured: the migration's translation is verifier-clean |
| `wf091-parameterref-eenheid` | WF-091 | B13 | verwijderd-met-reden | reference-type arm (ParameterReference) | the reference's own type left the wire; measured: the migration's translation is verifier-clean |
| `wf091-schrijfdoel-eenheid` | WF-091 | B13 | verwijderd-met-reden | rule-target arm | the rule target is a reference and lost its own type; measured: the migration's translation is verifier-clean |
| `wf089-aggregation-pass-through-enum` | WF-089 | B13 | herschreven (m) | aggregation (pass-through) arm, EERSTE | the annotation of the EERSTE is a derived position and stays; operand 0 is typed by its declaration; the WF-038 line beside it is unchanged |
| `wf089-attribuutref-enum` → `wf063-match-arm-outside-read-domain` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 arms outside the scrutinee's declared domain | reference-type arm gone; measured: WF-063 (D17; the form is `wf063-enum-alien-value`'s and `wf063-enum-missing-value`'s) |
| `wf089-attribuutref-geen-domein` → `wf023-enumeration-read-in-number-target` | WF-089 → WF-023 | B13 | herschreven op WF-023 | WF-023 family clause (an enumeration into a number target) | reference-type arm gone; measured: WF-023 (D17) |
| `wf089-attribuutref-geen-keuzetype-declaratie` → `wf063-match-over-number-read` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 scrutinee is no keuzetype | reference-type arm gone; measured: WF-063, an arm that had no witness |
| `wf089-attribuutref-lijst-sum` → `wf070-lambda-parameter-other-sum-domain` | WF-089 → WF-070 | B13 | herschreven op WF-070 | WF-070 lambda parameter against the source element type (sum domain) | reference-type arm gone; measured: WF-070, an arm that had no witness |
| `wf089-attribuutref-navigatie-enum` → `wf063-match-arm-outside-domain-after-navigation` | WF-089 → WF-063 | B13, B11 | herschreven op WF-063 | WF-063 arms outside the declared domain, navigated scrutinee | reference-type arm gone; measured: WF-063 (D17) |
| `wf089-attribuutref-sum` → `wf063-match-constructor-outside-read-sum-domain` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 constructors outside the scrutinee's declared sum domain | reference-type arm gone; measured: WF-063 (D17) |
| `wf089-binderref-sum` → `wf063-match-over-binder-other-sum-domain` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 constructors outside the binder's declared sum domain | binder-reference arm gone; measured: WF-063 (D17) |
| `wf089-composite-domain` | WF-089 | B13 | herschreven (m) | composite arm (PLUS annotated with a keuzetype) | the composite keeps its annotation (a derived position) |
| `wf089-dimensiecel-enum` → `wf063-match-arm-outside-domain-dimension-cell` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 arms outside the declared domain, cell-read scrutinee | cell-read arm gone; measured: WF-063 (D17) |
| `wf089-enum-over-som` → `wf036-equality-sum-type-against-enumeration` | WF-089 → WF-036 | B13 | herschreven op WF-036 | WF-036 equality across families (sumtype vs enumeration) | reference-type arm gone; measured: WF-036, an arm that had no witness |
| `wf089-foldbron-enum` | WF-089 | B13 | verwijderd-met-reden | fold-source reference arm | measured: the translation stays WF-089 through the pass-through aggregation arm, byte-identical to `wf089-aggregation-pass-through-enum` (D18: an arm change gate 3 cannot see) |
| `wf089-minimum-pass-through` | WF-089 | B13 | herschreven (m) | aggregation (pass-through) arm, MINIMUM | as `wf089-aggregation-pass-through-enum`; the WF-038 line beside it is unchanged |
| `wf089-parameterref-enum` → `wf063-match-arm-outside-domain-parameter` | WF-089 → WF-063 | B13 | herschreven op WF-063 | WF-063 arms outside the parameter's declared domain | parameter-reference arm gone; measured: WF-063 (D17) |
| `wf089-som-over-enum` → `wf036-equality-enumeration-against-sum-type` | WF-089 → WF-036 | B13 | herschreven op WF-036 | WF-036 equality across families (enumeration vs sumtype) | reference-type arm gone; measured: WF-036, the mirror of the row above (D17) |
| `wf090-attribuutref` → `wf074-field-outside-read-struct-domain` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074 field not declared on the base's struct domain | reference-type arm gone; measured: WF-074 (D17; the form is `wf074-unknown-field`'s) |
| `wf090-attribuutref-lijst` → `wf070-lambda-parameter-other-struct-domain` | WF-090 → WF-070 | B13 | herschreven op WF-070 | WF-070 lambda parameter against the source element type (struct domain) | reference-type arm gone; measured: WF-070, the struct side of the sum-domain row |
| `wf090-binderref` → `wf074-field-outside-struct-domain-binder` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074, binder base | binder-reference arm gone; measured: WF-074 (D17) |
| `wf090-dimensiecel` → `wf074-field-outside-struct-domain-dimension-cell` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074, cell-read base | cell-read arm gone; measured: WF-074 (D17) |
| `wf090-pass-through` | WF-090 | B13 | herschreven (m) | aggregation (pass-through) arm, EERSTE | the EERSTE keeps its annotation; operand 0 is typed by its declaration; the WF-038 line beside it is unchanged |
| `wf090-foldbron` | WF-090 | B13 | verwijderd-met-reden | fold-source reference arm | measured: the translation stays WF-090 through the pass-through aggregation arm, on the arm of `wf090-pass-through` (the same arm under EERSTE, with the two struct domains the other way round; D18: an arm change gate 3 cannot see) |
| `wf090-parameterref` → `wf074-field-outside-struct-domain-parameter` | WF-090 → WF-074 | B13 | herschreven op WF-074 | WF-074, parameter base | parameter-reference arm gone; measured: WF-074 (D17) |
| `wf090-write-defaults` | WF-090 | — | herschreven (m) | write half (a StructLiteral of another domain) | no L2 delta; the struct literal keeps its type |
| `wf090-write-object-creation` | WF-090 | — | herschreven (m) | write half (a StructLiteral of another domain) | no L2 delta; the struct literal keeps its type |
| `wf090-write-rule-target` | WF-090 | — | herschreven (m) | write half (a StructLiteral of another domain) | no L2 delta; the struct literal keeps its type |
| `wf088-functiondef-drift` | WF-088 | — | herschreven (m) | enumeration use site against its declaration | no L2 delta on the defect; the body's binder reference lost its own type, and with it the third report line (the expression layer's), the two declaration lines remain |
| `wf088-literal-type-drift` | WF-088 | B22 (L4) | herschreven (m) | enumeration use site against its declaration | the drift sits on a `Literal`'s type, which stays (V9 (a)) |
| `wf088-use-site-value-drift` | WF-088 | — | herschreven (m) | enumeration use site against its declaration | no L2 delta; the drift sits on the attribute declaration |
| `wf088-use-site-value-order` | WF-088 | — | herschreven (m) | enumeration use site against its declaration | no L2 delta; the drift sits on the attribute declaration |
| `wf078-binderref-type-lie` → `wf076-let-kind-against-body` | WF-078 → WF-076 | B13 | herschreven op WF-076 | WF-076 (Let type against body type) | WF-078 retired with the binder reference's own type; measured: the Let's text type against the body's declared number is WF-076 (D17) |
| `wf034-sentinel-builtin-operand` → `wf047-bereik-op-builtin` | WF-034 (sentinel fence) → WF-047 | B13 | herschreven op WF-047 | WF-047, a range on a node that is no instance fold | the sentinel left the wire; the migration wrote the range the schema demands on a single-argument Functie with an elementObjecttype (U2), and measured, WF-047 refuses it on a builtin (D17; the card expected verwijderd-met-reden, measured before the migration wrote the range) |
| `wf034-sentinel-builtin-wratoperand` | WF-034 (sentinel fence) | B13 (+V6) | herschreven (m), armwissel | WF-034 name resolution (a role name read as an attribute) | the sentinel left the wire; measured: the role name read on a builtin is an unresolved member; WF-047 reports the derived range beside it |
| `wf034-sentinel-gelest-vouwoperand` | WF-034 (sentinel fence) | B13 | verwijderd-met-reden | sentinel fence on a listed operand | the sentinel left the wire; the read is an ordinary read; measured: the migration's translation is verifier-clean |
| `wf034-sentinel-listed-wratoperand` | WF-034 (sentinel fence) | B13 (+V6) | herschreven (m), armwissel | WF-034 name resolution (a role name read as an attribute) | the sentinel left the wire; measured: the role name as a listed operand is an unresolved member |
| `wf034-sentinel-geleste-setlezing` → `wf053-to-many-list-fold-operand` | WF-034 (sentinel fence) → WF-053 | B13, B11 | herschreven op WF-053 | WF-053 to-many hop in a listed operand | sentinel and set reading gone; measured: the translation is byte-identical to that of `wf096-setlezing-geleste-operand`, so both old witnesses share the one successor directory |
| `wf034-sentinel-kale-lezing` | WF-034 (sentinel fence) | B13 | verwijderd-met-reden | sentinel on an ordinary read | the sentinel left the wire; measured: the migration's translation is verifier-clean |
| `wf034-sentinel-lijstelement-payload` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-payload-onder-lijst` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-structveld-payload` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-sumtype-payload` | WF-034 (d) | B16 | verwijderd-met-reden | the unknown-type node in a payload of a node's type | B16 takes the unknown-type node out of the datatype union: the payload cannot be written (m2.py: "Onbekend outside a reference type, an Onopgelost type or a derived composite type") |
| `wf034-sentinel-null-vouwoperand` | WF-034 (sentinel fence) | B13 | verwijderd-met-reden | a null type on a listed operand | a reference without a type is the L2 form; measured: the migration's translation is verifier-clean |
| `wf093-filled-count-wrat` | WF-093 | B13, B16 (+V6) | herschreven (m) | WF-093, a count source under AANTAL_GEVULDE | WF-093 keys on the source itself (D9) instead of the sentinel; the document is the migration's translation |
| `wf093-wrat-fold-bare-first` | WF-093 | B13, B16 (+V6) | herschreven (m) | WF-093, a count source under EERSTE | as `wf093-filled-count-wrat` |
| `wf093-wrat-fold-bare-sum` | WF-093 | B13, B16 (+V6) | herschreven (m) | WF-093, a count source under SOM | as `wf093-filled-count-wrat` |
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
| `wf053-read-fold-operand-unconnected-hop` | WF-053 | B11 (+B12) | herschreven (i) | WF-053 unbound read with a foreign objecttype (D10) | no fact type relates the two types, so the migration refuses; written by hand as the read without bindings whose objecttype is not the subject type |
| `wf053-to-many-hop-in-scalar` | WF-053 | B11 | herschreven (m) | to-many hop in a scalar position (the whole rule value) | the written plural left; the migration derived the binding, and the to-many hop remains |
| `wf053-written-role-mismatch` | WF-053 | B11 | verwijderd-met-reden | written step against the bound role | there is no written step in L2; measured: the migration's translation is verifier-clean |
| `wf053-zelfverwijzing-vreemde-rol` | WF-053 | B11 | verwijderd-met-reden | the zero-hop self-reference test | the self-reference exists only as a written step; without it the read is the subject's own read (m2.py refuses the written name: "derives no unique binding") |
| `wf054-binding-on-unscoped` | WF-054 | B8 | verwijderd-met-reden | a scope binding on an unscoped uniqueness | the scope binding IS the scoping, so this shape cannot be written (m2.py: "a scope binding on an unscoped uniqueness") |
| `wf054-dangling-scope-binding` | WF-054 | B8 | herschreven (m) | bound scope, undeclared fact type | the written scope field left; the binding is the scope |
| `wf054-to-many-scope-role` | WF-054 | B8 | herschreven (m) | to-many scope role | the bindingless scope became a `scopeRol` derived from the written name; the to-many role remains |
| `wf096-aantal-setbron` → `wf093-count-over-attribute-source` | WF-096 → WF-093 | B11 (+V6) | vervangen (WF-093, D9) | WF-093, AANTAL over a source that is no count source | WF-096 retired with the set reading; its count half went into WF-093, which since L2 demands a count source under AANTAL (D9) |
| `wf096-aantal-setbron-bronpad` | WF-096 | B11 (+V6) | verwijderd-met-reden | the set reading under AANTAL, multi-step range | measured: the translation falls on WF-093 like the successor of `wf096-aantal-setbron`; the path length does not reach that rule |
| `wf096-aantal-setbron-wrat` | WF-096 | B11, B13 | verwijderd-met-reden | the set spelling of the count source under AANTAL | measured: the translation is a count source anchored on the element type, WF-083 (c) — the shape of `wf083-fold-source-anchor-by-operand`, rewritten in place |
| `wf096-setlezing-geleste-operand` → `wf053-to-many-list-fold-operand` | WF-096 → WF-053 | B11 | vervangen (WF-053) | WF-053 to-many hop in a listed operand | the set reading left; the navigated read with its to-many hop remains (U8 name) |
| `wf096-setlezing-operand` → `wf053-to-many-plusoperand` | WF-096 → WF-053 | B11 | vervangen (WF-053) | WF-053 to-many hop in an operand of PLUS | as the row above (U8 name) |
| `wf096-setlezing-regelwaarde` → `wf053-to-many-rule-value` | WF-096 → WF-053 | B11 | herschreven op WF-053 | WF-053 to-many hop in the whole rule value | the set reading left; measured: WF-053, the position `wf053-to-many-hop-in-scalar` also pins (D17) |
| `wf034-rolcheck-loze-andertefiller` | WF-034 (o) | B9 | verwijderd-met-reden | a role check without its filler type | the merged node names its fact type and role, both required: a check without them cannot be written (m2.py: "a role check without anderTeFiller") |
| `wf034-rolcheck-onbekend-andertefiller` → `wf034-role-check-unknown-fact-type` | WF-034 (o) → WF-034 (role arm) | B9 | vervangen (WF-034, role arm) | WF-034 role arm: the fact type is not declared | the filler derivation left; the successor names an undeclared fact type in `RoleMembership` (U8 name) |
| `wf034-far-role-check-wrong-side` | WF-034 (t) → WF-034 (role arm) | B9 | herschreven (i) | WF-034 role arm: the subject does not fill the role | the same mistake in the merged form, written by hand (the migration finds no candidate): the red-first proof V4 (b) asks |
| `wf048-ambiguous-connecting-relation` | WF-048 | B12 | verwijderd-met-reden | an allocation without a derivable relation | WF-048 retired: the allocation carries its recipient role (m2.py: "does not derive one") |
| `wf048-telling-meerduidig` | WF-048 | B12 | verwijderd-met-reden | a count without a derivable range | WF-048 retired: a fold with an elementObjecttype carries its range (m2.py: "does not derive one") |
| `wf003-unresolved-domain` | WF-003 | B16 | herschreven (i) | WF-003, a declaration naming an undeclared domain | B16 takes the unknown-type node out of the datatype union; written by hand as the enumeration naming `Bedragen` (D15), compared for equality |
| `wf034-ambigu-typenpaar` | WF-034 | B12 (L2) | verwijderd-met-reden | the type-pair fallback of a bindingless count | a fold with an elementObjecttype carries its range, so the fallback does not exist (m2.py: "does not derive one") |
| `wf034-attribute-reference-without-attribute` | WF-034 | — | herschreven (m) | `attribuut` null | no L2 delta |
| `wf034-attribute-reference-without-object-type` | WF-034 | — | herschreven (m) | `objecttype` null | no L2 delta |
| `wf034-dangling-parameter` | WF-034 | — | herschreven (m) | an undeclared parameter | no L2 delta |
| `wf034-characteristic-check-without-characteristic` | WF-034 | — | herschreven (m) | `kenmerk` null | no L2 delta |
| `wf034-quantifier-fold-filter-subject-characteristic` | WF-034 | — | herschreven (m) | a characteristic of the subject read on the element | no L2 delta |
| `wf034-object-creation-without-subject` | WF-034 | — | herschreven (m) | `subject` null | no L2 delta |
| `wf034-object-creation-unknown-subject` | WF-034 | — | herschreven (m) | an undeclared subject | no L2 delta |
| `wf034-parameter-reference-without-parameter` | WF-034 | — | herschreven (m) | `parameter` null | no L2 delta |
| `wf034-dimension-cell-none-reference` | WF-034 | B13 (L2) | herschreven (m) | a cell read whose attribute position is no reference | the cell lost its own type; the literal in the attribute position remains |
| `wf034-dimension-cell-without-attribute` | WF-034 | B13 (L2) | herschreven (m) | a cell read without an attribute | the cell lost its own type |
| `wf034-fold-source-unknown-attribute` | WF-034 | B12, B11 (L2) | herschreven (m) | an unresolved attribute at a value fold's source | the migration wrote the range; the unknown name remains |
| `wf034-fold-source-without-attribute` | WF-034 | B13 (L2) | herschreven (m) | a fold source without an attribute | the WF-091 line of the source's own type left with it |
| `wf034-fold-source-without-object-type` | WF-034 | B13 (L2) | herschreven (m) | a fold source without an object type | as the row above |
| `wf034-downward-read` | WF-034 | B11 (L2) | herschreven (m) | a supertype read of a subtype attribute at a fold source | the set reading left; the resolution stays |
| `wf034-onbekend-regeldoel` | WF-034 | B13 (L2) | verwijderd-met-reden | the unknown-type node on a rule target | the target lost its own type; measured: the migration's translation is verifier-clean |
| `wf034-regeldoel-zonder-type` | WF-034 | B13 (L2) | verwijderd-met-reden | a rule target without a type | a target without a type is the L2 form; measured: the migration's translation is verifier-clean (byte-identical to the row above) |
| `wf034-allocation-empty-source-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-empty-ordering-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-empty-maximum-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-unknown-source-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-unknown-target-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-unknown-ordering-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-unknown-maximum-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-unknown-ratio-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-unknown-remainder-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-without-source-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-without-source-type` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-without-target-attribute` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-allocation-without-target-type` | WF-034 | — | herschreven (m) | an allocation field | no L2 delta |
| `wf034-zelftype-typenpaar` | WF-034 | B12 (L2) | verwijderd-met-reden | the type-pair fallback over a self-typed pair | as `wf034-ambigu-typenpaar` |

#### Measured beyond the card's stamp list

The migration refused 32 documents; the stamp list covered some of them, and
the rest were measured here. So was every witness whose verifier report changed
between the L1 and the L2 form of the corpus.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf018-parameter-unknown` | WF-018 | B16 | herschreven (i) | WF-018, a parameter naming an undeclared domain | as `wf003-unresolved-domain`, for a parameter (D15): written by hand as the enumeration naming `Drempels`, compared for equality |
| `wf020-crossform-collision` | WF-020 | B12 | herschreven (i) | WF-020, one name shared by a rule and an allocation (names are unique across all named forms) | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf024-complex` | WF-024 | B12 | herschreven (i) | WF-024, the allocation method COMPLEX | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf025-max-without-ratio` | WF-025 | B12 | herschreven (i) | WF-025, a maximum without a ratio attribute | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf026-two-allocations-one-target` | WF-026 | B12 | herschreven (i) | WF-026, two allocations writing one target | the allocation had no source type, so the migration could not derive the required recipient role; written by hand with a fact type `strandgoedverdeling`, `Jutter` as source type and the `ontvanger` role bound; the rule still fires as before |
| `wf031-day-kind-reads-role-state` | WF-031 | B9 | herschreven (i) | unchanged (role membership read in a day-kind condition) | a day-kind condition has no subject to derive the role check's fact type from; written by hand as `RoleMembership` naming it |
| `wf035-power-with-unit` | WF-035 | B13 | herschreven (i) | unchanged (a power over a unit-carrying base) | the unit sat on the reference's own type; measured, the translation was verifier-clean; written by hand with the unit on the attribute's declaration |
| `wf036-order-number-vs-text` | WF-036 | B13 | herschreven (i) | unchanged (an ordering operand that is text) | the text type sat on the reference's own type; measured, the translation was verifier-clean; written by hand with the parameter declared as text |
| `wf037-filled-count-not-set-source` | WF-037 | B11, B12 | herschreven (i) | the filled count's source is no attribute reference | the set-source demand is enforced at load since L2 (U2); the card expected a rewrite without range and elementObjecttype, measured a duplicate of `wf037-filled-count-without-element-type`'s arm, so the witness pins the arm that has none: a range beside a literal source |
| `wf072-expression-type-order` | WF-072 | B13 | herschreven (i) | unchanged (a drifted struct copy at the expression layer) | the drift sat on a reference's own type; measured, the translation was verifier-clean; written by hand with the drift on a `StructLiteral`'s type |
| `wf083-fold-source-navigates-away` | WF-083 (b) | B11 | verwijderd-met-reden | a written role on a fold source | the written role has no L2 carrier (m2.py: "a written role on a fold source ... has no carrier") |
| `wf083-fold-source-anchor-by-operand` | WF-083 (c) | B11 | herschreven (i) | anchor of a count source (the fold subject) | measured (the card expected verwijderd-met-reden): without its written role the document is a count source anchored on the element type, which WF-083 (c) refuses per position (D8); the witness of that position |
| `wf083-vouwbron-navigatierol-buiten-de-wrat` | WF-083 (b) | B11 | verwijderd-met-reden | a written role on a set source | as `wf083-fold-source-navigates-away` |
| `wf083-fold-source-anchor-by-intermediate-type` | WF-083 (c) | B11, B13 | herschreven (m) | anchor of a set source (an intermediate path type) | the set reading left; the range makes the argument the set source (D8 (i)) |
| `wf083-fold-source-foreign-element-type` | WF-083 (c) | B11, B13 | herschreven (m) | anchor of a set source (the fold subject) | as the row above |
| `wf083-fold-source-foreign-object-type` | WF-083 (c) | B13 | herschreven (m) | anchor of the subject's own read (another type) | a TOTAAL without a range is the subject's own read (D8 (iv)) |
| `wf102-bronpad-naast-bronrol` | WF-102 (a) | B12 | verwijderd-met-reden | bronPad beside bronRol | one range per fold is the schema's and the loader's since L2 (U2) (m2.py: "bronPad beside bronRol") |
| `wf102-source-path-by-negated-set-source` | WF-102 (c) | B13, B11 | herschreven (m) | WF-102 (c), a `bronPad` on a count source (a count's range is one step) | the count source lost its sentinel type (B13) and is recognised at the source itself (D9); the clause's wording and its message moved with the set reading: the admitted position is a VALUE fold's set source. The row and `expect.txt` were added in the L2 fix round; the union had changed the message without a row |
| `wf108-kwantorvouw-bereik-dubbel` | WF-108 (c) | B12 | verwijderd-met-reden | bronPad beside bronRol on a quantifier fold | as the row above, on a quantifier fold |
| `wf110-union-beside-fold-source` | WF-110 (c) | B12 | herschreven (i) | unchanged (the count's own source beside a union) | a single-argument count with an elementObjecttype carries a range (U2), and nothing derives one beside a union; written by hand with the first selection's relation as `bronRol` |
| `wf110-unie-tak-zonder-bronrol` | WF-110 (e) | B12 | verwijderd-met-reden | a union selection without its relation | `sourceRole` is required and non-null (D5 (c)) (m2.py: "a union selection without its relation") |
| `wf007-doeltype-geheel-met-decimalen` | WF-007 | B13 | verwijderd-met-reden | the rule-target arm | the rule target lost its own type, the one position of this arm; measured: the migration's translation is verifier-clean |
| `wf023-schrijf-doeltype-leugen` | WF-023 | B13 | verwijderd-met-reden | the target's declared type against its own lying type | the target lost its own type; measured: the translation is byte-identical to `wf023-write-enum-domain` and falls on the same WF-023 line |
| `wf034-foldbron-rolnaam-waardevouw` → `wf093-count-source-under-sum` | WF-034 → WF-093 | B13 | herschreven op WF-093 | WF-093, a count source under SOM | measured: WF-093 recognises the count source at the source (D9), so the role name under SOM falls there (D17) |
| `wf046-kwantorvouw-bindingloos-bereik` → `wf046-quantifier-fold-range-other-type` | WF-046 | B12 | herschreven (m) | WF-046, the range of an ALLE quantifier fold over the relation a recursive creation of ANOTHER type writes (a Post for a Wachter) | the loader refuses a quantifier fold without `bronRol` or `bronPad` (B12, U2), so the bindingless form cannot be written; the migration derives the range `bemanning`/`post`, and the translation fails WF-046 through the bound range. The L2 union first removed it as a duplicate of `wf046-quantifier-fold-all-range`, which mints a Wachter for a Wachter; the removal criterion (the migration refuses the document, or its translation is verifier-clean) was not met, so the L2 fix round restored the translation, byte for byte, under a name without "bindingloos" |

Seven witnesses kept their rule and their arm while their report lost
incidental lines to B13 — a reference's own `type` that crossed nothing any more.
Three of them carry a stamp row because their NOTE records the migration:
`wf088-functiondef-drift` (in the WF-088 rows above) and the two WF-092 rows
below. The other four carry no stamp: `wf007-whole-with-decimals`,
`wf027-lying-attribute-reference-type`, `wf034-unit-unknown-system` and
`wf040-unpinned-cell`. Where a NOTE cited a lost line or described the lost
type as present, the citation was brought to the current report — for
`wf027-lying-attribute-reference-type` and `wf034-unit-unknown-system` in the L2
fix round.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf092-factor-without-canonical` | WF-092 | — | herschreven (m) | a unit with a factor and no canonical unit | no L2 delta on the defect; the rule target and the attribute reference lost their own `type` (B13), and with them two of the five report lines |
| `wf092-denominator-without-factor` | WF-092 | — | herschreven (m) | a denominator unit with a canonical unit and no factor | no L2 delta on the defect; the rule targets and the fold sources lost their own `type` (B13), and with them five of the eleven report lines |

#### Added in the L2 union and the L2 fix round (2026-09-24)

Witnesses that no stamp produced: the U12 witness of the union, and the five
the lens on the union asked for (`expect.txt` in each).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf034-role-membership-foreign-filler-class` | WF-034 | B9 (U12) | toegevoegd | role arm of `RoleMembership`: `counterpartType` is no subtype of the other role's filler | `counterpartType` carries the narrowing of the far role check that the first L2 form lost (U12); `corpus/golden/jutterswet-postbezetting` with a declared type outside the filler's chain |
| `wf034-role-membership-role-as-plural` | WF-034 | B9 | toegevoegd | role arm of `RoleMembership`: `role` is no declared role name of `factType` | `role` spelled as the declared plural of the other role; the arm had no witness (lens on the union) |
| `wf034-role-membership-filler-class-unknown` | WF-034 | B9 (U12) | toegevoegd | role arm of `RoleMembership`: `counterpartType` is no declared object type | the arm had no witness (lens on the union) |
| `wf053-own-read-supertype` | WF-053 | B11 | toegevoegd | a read without navigation bindings whose `objecttype` is a supertype of the subject | the own read names the subject type itself (equality, IR-SPEC ch. 4.1a); on the union `ref` and the ASP backend admitted the supertype |
| `wf083-total-foreign-element-type` | WF-083 (c) | B12 | toegevoegd | the `elementObjecttype` of a `TOTAAL` without a range is not the fold subject | new arm of the fix round; before it the field was verifier-clean and moved no answer |
| `wf091-plus-unit-case` | WF-091 | B13 | toegevoegd | derived arm, PLUS, unit facet (unit names differ in case alone) | the byte-exact unit names lost their witness with `wf091-attribuutref-eenheid-kast` (removed above); the lie now sits on a composite's annotation |

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
(removed below) and on `wf017-day-kind-name-binds-foreign-day-kind`, whose arm
now reads the day kinds' own spellings (measured again in the L3 fix round: these
nine are the only reports whose text moved). A rewritten witness of the
card's stamp list carries `expect.txt`, one literal line of the verifier
message of the arm it pins, without a JSON pointer; so does every date witness
whose rule reads the precision, and every witness added here. The arm column
names that arm in words, or, for a removed witness, the arm that left. The
negative zone went from 400 to 402 documents (four removed, six added); the
floor `EXPECTED_MIN_NEGATIVE` did not have to move, and `EXPECTED_MIN_EXPECT`
rose from 91 to 159 (`scripts/check-verifier.sh`).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf017-eenheidsleutel-schaduwt-kalenderafkorting` | WF-017 (a) | D55 | verwijderd-met-reden | a registry key that is a calendar spelling (the abbreviation `wk`) | the key is a third spelling of `schouwdag` beside its name and its plural `schouwdagen`, and a `DayKind` carries its name and one plural (m3.py: "a third spelling of the day kind "schouwdag" beside its name and the plural "schouwdagen": a DayKind carries its name and one plural"); the arm now reads the `plural`, where `wf017-plural-shadows-calendar-abbreviation` pins the abbreviation class (added in the L3 fix round, below) |
| `wf017-eenheidsleutel-schaduwt-kalendereenheid` | WF-017 (a) | D55 | verwijderd-met-reden | a registry key that is a calendar spelling (the plural `dagen`) | idem; the class is pinned on the `plural` by `wf017-plural-shadows-calendar-unit` |
| `wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm` | WF-017 (a) | D55 | verwijderd-met-reden | a registry key that is a calendar spelling (the second plural `secondes`) | idem; the class is pinned on the `plural` by `wf017-plural-shadows-second-plural-form` |
| `wf034-dagsoorteenheid-onbekende-dagsoort` | WF-034 (s) | D55 | verwijderd-met-reden | a registry value naming an undeclared day kind (the arm left with the registry) | a spelling stands on the day kind it belongs to or not at all, so there is no value to name an undeclared day kind (m3.py: "the registry entry names the day kind "havendag", which no declaration carries: the L3 shape has no registry, so a spelling is written on its own day kind or not at all"); arm (s) of WF-034 loses its object and becomes a history line in `spec/IR-SPEC.md` ch. 4.6 |
| `wf092-canonical-without-factor` | WF-092 (a) | B17 (i), U4 | herschreven (m) | a canonical unit without a factor | the conversion pair is spelled `canonical`/`factorToCanonical`, the missing half absent where it was null; the report moved only in its paths |
| `wf092-factor-without-canonical` | WF-092 (b) | B17 (i), U4 | herschreven (m) | a factor without a canonical unit | as the row above; the factor is `{num: 1, den: 1}` |
| `wf092-denominator-without-factor` | WF-092 (a) | B17 (i), U4, D54 | herschreven (m) | a canonical unit without a factor, on the denominator of a rate | as the rows above; the denominator is spelled `denominator`. The unit system carries a map key other than the abbreviation, now a spelling (D54, not on the defect) |
| `wf017-day-kind-name-binds-foreign-day-kind` | WF-017 (b) | D55 | herschreven (m) | arm (b): the plural of one day kind is the name of another | measured: the one witness whose arm moved (eight others changed only the name of the date type, see above). Until L3 the arm reported the registry key of a declared name bound to another day kind; the day kinds now carry their own spellings, and the arm reports the plural that is another day kind's name |
| `wf017-day-kind-name-shadows-calendar-abbreviation` | WF-017 | D55 | herschreven (m) | the declared-name clause: a name that is a calendar spelling (the abbreviation `wk`) | the name is the `DayKind`'s `name`; the report moved only in its path (`/model/dayKinds/2/name`) |
| `wf017-day-kind-shadows-built-in` | WF-017 | D55 | herschreven (m) | the declared-name clause (the singular `dag`) | as the row above (`/model/dayKinds/0/name`) |
| `wf095-source-snippet` | WF-095 | B15 | herschreven (m) | the lexeme of a millisecond-precision date literal (a day-first snippet) | the literal's type is `Date` with `precision: millisecond`, which is what makes WF-095 its rule; the message names the date by its precision |
| `wf095-fraction-width` | WF-095 | B15 | herschreven (m) | idem (a fraction of one digit) | as the row above |
| `wf095-surrounding-text` | WF-095 | B15 | herschreven (m) | idem (text around the lexeme) | as the row above |
| `wf095-missing-seconds` | WF-095 | B15 | herschreven (m) | idem (no seconds field) | as the row above |
| `wf095-space-separator` | WF-095 | B15 | herschreven (m) | idem (a space for `T`) | as the row above |
| `wf095-zone-suffix` | WF-095 | B15 | herschreven (m) | idem (a zone suffix) | as the row above |
| `wf034-unit-unknown-system` | WF-034 (r) | U4 | herschreven (m) | a unit's `system` naming no declared unit system | the field is spelled `system`; the report moved only in its paths |
| `wf009-conversion-cycle` | WF-009 | D25, U4 | herschreven (m) | a conversion cycle | the factors 2.0 and 0.5 are the exact rationals `{2, 1}` and `{1, 2}`; the report moved only in its paths (`/model/unitSystems/0/units/<i>`) |

The WF-091 family carries `expect.txt` since L2, and every fragment holds
unchanged. Its unit-facet arm compares unit records, which L3 respelled (U4);
fourteen of the fifteen hosts carry a unit-less percentage that became a
`Number` with the built-in `%` (D23), the card's percentage carriers, and in
none of them is it the defect.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf091-rounding-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, rounding, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-clamp-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, clamp, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binary-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, PLUS, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-fold-maximum-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, MAXIMUM, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-sum-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, SOM, unit facet (`t` against `kg`) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-plus-unit-case` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, PLUS, unit facet (unit names differ in case alone) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-times-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, MAAL, unit facet (a rate against a plain unit) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-total-unit` | WF-091 | U4, B14, D23 | herschreven (m) | derived arm, TOTAAL, unit facet (a rate against a plain unit) | the unit record the arm compares is spelled with the L3 keys (U4); the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-filled-count-unit` | WF-091 | U4, D54 | herschreven (m) | derived arm, AANTAL_GEVULDE, unit facet | the unit record the arm compares is spelled with the L3 keys (U4); the unit system carries spellings (D54), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binary-kind` | WF-091 | B14, D23 | herschreven (m) | derived arm, PLUS, soort facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-count-decimals` | WF-091 | B14, D23 | herschreven (m) | derived arm, AANTAL, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-rounding-decimals` | WF-091 | B14, D23 | herschreven (m) | derived arm, rounding, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binary-decimals` | WF-091 | B14, D23 | herschreven (m) | derived arm, PLUS, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-binary-whole` | WF-091 | B14, D23 | herschreven (m) | derived arm, PLUS, wholeness facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |
| `wf091-fold-decimals` | WF-091 | B14, D23 | herschreven (m) | derived arm, MINIMUM, decimals facet | the host's unit-less percentage is a `Number` with the built-in `%` (D23), not on the defect; `expect.txt` since L2, unchanged |

The card's group rows — a sign written where none was (D24), a parameter's
timeline facet (B18), the day kinds' plurals (D55), a unit system's spellings
(D54) — rewrote each document at a place that is not its defect. Each keeps its
report, and pins its arm now.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf035-duration-minus-date` | WF-035 | D24 | herschreven (m) | duration − date | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf035-duration-plus-date` | WF-035 | D24 | herschreven (m) | duration + date | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf038-extractor-text` | WF-038 | D24 | herschreven (m) | a day-precision extractor over text | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf038-duration-without-node-type` | WF-038 | D24 | herschreven (m) | a tijdsduur without a node type | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf038-duration-without-time-unit` | WF-038 | D24 | herschreven (m) | a tijdsduur without a recognised time unit | no L3 delta on the defect: a number declared without a sign writes `sign: ONBEPAALD` now, and the rule reads no sign |
| `wf049-read-of-nontemporal` | WF-049 | B18 | herschreven (m) | a temporal read of a non-temporal attribute | no L3 delta on the defect: the host's time-dependent parameter carries a timeline facet |
| `wf049-type-not-element` | WF-049 | B18 | herschreven (m) | a temporal read typed other than the element datatype | no L3 delta on the defect: the host's time-dependent parameter carries a timeline facet |
| `wf098-context-value-name-capitalization` | WF-098 | B18 | herschreven (m) | a context-value name spelled in another case | no L3 delta on the defect: the host's time-dependent parameter carries a timeline facet |
| `wf031-session-value-in-day-kind` | WF-031 | D55 | herschreven (m) | a day-kind condition reading a session value | no L3 delta on the defect: the host's day kinds carry their plurals |
| `wf098-unknown-context-value-name` | WF-098 | D55 | herschreven (m) | an unknown context-value name | no L3 delta on the defect: the host's day kinds carry their plurals |
| `wf034-attribute-reference-without-attribute` | WF-034 | D54 | herschreven (m) | an attribute reference without an attribute name | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-attribute-reference-without-object-type` | WF-034 | D54 | herschreven (m) | an attribute reference without an object type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-dimension-cell-none-reference` | WF-034 | D54 | herschreven (m) | a dimension-cell read whose attribute position is no attribute reference | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-dimension-cell-without-attribute` | WF-034 | D54 | herschreven (m) | a dimension-cell read without an attribute reference | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-fold-source-without-attribute` | WF-034 | D54 | herschreven (m) | a fold source without an attribute name | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-fold-source-without-object-type` | WF-034 | D54 | herschreven (m) | a fold source without an object type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-parameter-reference-without-parameter` | WF-034 | D54 | herschreven (m) | a parameter reference without a parameter name | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-sentinel-builtin-wratoperand` | WF-034 | D54 | herschreven (m) | a name that is neither an attribute nor a characteristic (an operand of a builtin) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-sentinel-listed-wratoperand` | WF-034 | D54 | herschreven (m) | idem (an operand read on the subject) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-empty-maximum-attribute` | WF-034 | D54 | herschreven (m) | an allocation's empty `maximumAttribuut` | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-unknown-source-attribute` | WF-034 | D54 | herschreven (m) | an allocation's `bronAttribuut` naming no attribute of the source type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-unknown-target-attribute` | WF-034 | D54 | herschreven (m) | an allocation's `doelAttribuut` naming no attribute of the target type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-unknown-maximum-attribute` | WF-034 | D54 | herschreven (m) | an allocation's `maximumAttribuut` naming no attribute of the target type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-unknown-remainder-attribute` | WF-034 | D54 | herschreven (m) | an allocation's `restantAttribuut` naming no attribute of the source type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-without-source-type` | WF-034 | D54 | herschreven (m) | an allocation without a source type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf034-allocation-without-target-type` | WF-034 | D54 | herschreven (m) | an allocation without a target type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-count-none-operand` | WF-037 | D54 | herschreven (m) | a count without an element type (no argument) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-count-list-literal` | WF-037 | D54 | herschreven (m) | a count without an element type (a list-literal argument) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-count-enumerated` | WF-037 | D54 | herschreven (m) | a count in a listed form (two operands) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-count-enumerated-triple` | WF-037 | D54 | herschreven (m) | a count in a listed form (three operands) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-count-enumerated-element` | WF-037 | D54 | herschreven (m) | a count in a listed form (two operands and an element type) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-count-without-element-type` | WF-037 | D54 | herschreven (m) | a count without an element type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-filled-count-dimension-selection` | WF-037 | D54 | herschreven (m) | a dimension selection on the filled count | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-aantalgevulde-granulariteit` | WF-037 | D54 | herschreven (m) | a granularity facet on the filled count | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-filled-count-empty-fallback` | WF-037 | D54 | herschreven (m) | an empty-value fallback on the filled count | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-filled-count-not-set-source` | WF-037 | D54 | herschreven (m) | the filled count's argument is no set source | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-filled-count-second-argument` | WF-037 | D54 | herschreven (m) | the filled count with a second argument | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf037-filled-count-without-element-type` | WF-037 | D54 | herschreven (m) | the filled count without an element type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf047-bereik-op-builtin` | WF-047 | D54 | herschreven (m) | a fold-source role binding on a node that is no instance fold | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf047-source-path-step-unresolved-role` | WF-047 | D54 | herschreven (m) | a `bronPad` step naming a plural as its role | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-own-read-supertype` | WF-053 | D54 | herschreven (m) | an own read whose `objecttype` is a supertype of the subject | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-read-fold-operand-unconnected-hop` | WF-053 | D54 | herschreven (m) | an own read whose `objecttype` is not the subject (a fold operand) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-to-many-list-fold-operand` | WF-053 | D54 | herschreven (m) | a to-many hop in a scalar slot (a list-fold operand) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-to-many-plusoperand` | WF-053 | D54 | herschreven (m) | a to-many hop in a scalar slot (a PLUS operand) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf053-to-many-rule-value` | WF-053 | D54 | herschreven (m) | a to-many hop in a scalar slot (a rule value) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-total-foreign-element-type` | WF-083 (c) | D54 | herschreven (m) | the `elementObjecttype` of a TOTAAL without a range is not the fold subject | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-fold-source-anchor-by-operand` | WF-083 (c) | D54 | herschreven (m) | anchor of a count source (the fold subject) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-fold-source-anchor-by-intermediate-type` | WF-083 (c) | D54 | herschreven (m) | anchor of a set source (an intermediate path type) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-fold-source-foreign-element-type` | WF-083 (c) | D54 | herschreven (m) | anchor of a set source (the fold subject) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf083-fold-source-foreign-object-type` | WF-083 (c) | D54 | herschreven (m) | anchor of the subject's own read (another type) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-filled-count-wrat` | WF-093 | D54 | herschreven (m) | a value fold over a role name (the filled count) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-count-source-under-sum` | WF-093 | D54 | herschreven (m) | a count source under SOM | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-count-over-attribute-source` | WF-093 | D54 | herschreven (m) | a count over an attribute source | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-wrat-fold-bare-first` | WF-093 | D54 | herschreven (m) | a value fold over a role name (EERSTE) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf093-wrat-fold-bare-sum` | WF-093 | D54 | herschreven (m) | a value fold over a role name (SOM) | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-source-path-a-step` | WF-102 | D54 | herschreven (m) | a `bronPad` of one step | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-source-path-lands-beside-element-type` | WF-102 | D54 | herschreven (m) | a `bronPad` that does not land on the element type | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-source-path-empty` | WF-102 | D54 | herschreven (m) | an empty `bronPad` | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-source-path-unconnected-step` | WF-102 | D54 | herschreven (m) | a `bronPad` step that does not connect | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-source-path-by-negated-set-source` | WF-102 (c) | D54 | herschreven (m) | a `bronPad` on a count source | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |
| `wf102-source-path-start-negated-by-subject` | WF-102 | D54 | herschreven (m) | a `bronPad` that does not start at the fold subject | no L3 delta on the defect: the unit system carries its aliases, and a map key other than the abbreviation, as spellings |

#### Measured beyond the card's stamp list

The date witnesses whose rule reads the precision, which the card's WF-036
line names without a list, and the witnesses of two rules that gained a
sibling arm at L3, so that a fragment tells the arms apart.

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf035-hour-by-day-date` | WF-035 | B15, D24 | herschreven (m) | a sub-day time unit on a day-precision date | the rule reads the date's precision, a field of `Date` since L3; the sign written where none was (D24) is not on the defect; the report is unchanged |
| `wf036-date-ordering-precision` | WF-036 | B15 | herschreven (m) | an ordering of two date precisions | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf038-first-mixed-date-precision` | WF-038 | B15 | herschreven (m) | an ordering fold over two date precisions | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf038-duration-mixed-precision` | WF-038 | B15, D24 | herschreven (m) | a tijdsduur over two date precisions | the rule reads the date's precision, a field of `Date` since L3; the sign written where none was (D24) is not on the defect; the report is unchanged |
| `wf049-read-at-datetime` | WF-049 | B15, B18 | herschreven (m) | a temporal read at a millisecond-precision date | the rule reads the date's precision, a field of `Date` since L3; the host parameter's facet (B18) is not on the defect; the report is unchanged |
| `wf084-bare-non-iso` | WF-084 | B15 | herschreven (m) | the lexeme of a day-precision date literal (day-first) | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf084-source-snippet` | WF-084 | B15 | herschreven (m) | idem (a source snippet) | the rule reads the date's precision, a field of `Date` since L3; the report is unchanged |
| `wf111-date-time-bound-attribute` | WF-111 | B15 | herschreven (m) | a millisecond-precision window bound (an attribute) | the rule reads the date's precision, a field of `Date` since L3; the message names the bound a `millisecond-precision date` where L2 wrote `datum-tijd`, and the arm is unchanged |
| `wf111-date-time-bound-literal` | WF-111 | B15 | herschreven (m) | a millisecond-precision window bound (a literal) | the rule reads the date's precision, a field of `Date` since L3; the message names the bound a `millisecond-precision date` where L2 wrote `datum-tijd`, and the arm is unchanged |
| `wf007-whole-with-decimals` | WF-007 | U5 | herschreven (m) | the precision forms (`whole` with decimals) | WF-007 gained the presentation arm; the precision fields are spelled `whole`/`decimals`, and the witness pins the arm it always held |
| `wf005-count-zero` | WF-005 | — | herschreven (m) | an attribute's facet with count 0 | WF-005 gained the parameter-facet arm; the witness pins the attribute arm it always held |

#### Added at L3 (2026-09-24)

A witness for each arm L3 added or moved onto a new field (`expect.txt` in
each).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf092-factor-not-in-lowest-terms` | WF-092 (c) | B17 (i) (U1) | toegevoegd | arm (c): a factor not in lowest terms (`{24, 2}` for twelve months) | the new arm: a schema cannot state a greatest common divisor, and WF-091 compares factors byte for byte; `corpus/golden/jutterswet-vergunningsduur` with the factor of `jaar` respelled at its three positions |
| `wf007-presentation-tag-by-annotation` | WF-007 | B14 (U2) | toegevoegd | the presentation tag on a type annotation (a `Literal.type`) | the new arm: the tag belongs to a declared datatype; `corpus/golden/jutterswet-vondsttarief` with the tag on one literal type |
| `wf017-plural-shadows-calendar-unit` | WF-017 (a) | D55 (U7) | toegevoegd | arm (a): a plural that is a calendar spelling (`dagen`) | the arm on the `plural`, which the registry-key witnesses above held; `corpus/golden/jutterswet-schouwrooster` with an unused plural respelled |
| `wf017-plural-shadows-second-plural-form` | WF-017 (a) | D55 (U7) | toegevoegd | idem (the second plural `secondes`) | the second-plural class, which no other witness reaches; as the row above |
| `wf017-plural-collides-with-foreign-plural` | WF-017 (b) | D55 (U7) | toegevoegd | arm (b): a plural that is another day kind's plural, differing in case alone | the plural-against-plural collision the registry could not show, and the pin of the ch. 2.1 identity, which left `corpus/golden/jutterswet-vormenkabinet` with its registry entries; as the row above |
| `wf005-parameter-facet-count-zero` | WF-005 | B18 (U6) | toegevoegd | a parameter's facet with count 0 | the new arm: WF-018 defers a parameter's facet to WF-005; `corpus/golden/anchor/jutterswet-penningvintage` with the count set to 0 |

#### Added and pinned in the L3 fix round (2026-09-24)

The lens on the L3 union found two arms that the list forms of L3 made
necessary and did not have — an abbreviation declared twice in one unit system,
and a parameter's facet with more than one period — and two classes whose
witness had left with the registry or was never written: the abbreviation class
of WF-017 arm (a), and WF-001 in the two namespaces whose nodes L3 rewrote,
whose messages now name the wire lists `unitSystems` and `dayKinds`. Each
witness carries `expect.txt`, and so does the one WF-001 witness that had
none. The negative zone went from 402 to 407 documents, and
`EXPECTED_MIN_EXPECT` rose from 159 to 165 (`scripts/check-verifier.sh`).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf001-duplicate-parameter` | WF-001 | U6 | herschreven (m) | the `parameters` namespace: two names differing in case alone | the defect sits on the parameter's `name`, spelled `naam` until L3 (U6); the report is unchanged, and `expect.txt` pins it since the fix round |
| `wf001-duplicate-day-kind` | WF-001 | D55 (U7) | toegevoegd | the `dayKinds` namespace: two day-kind names differing in case alone | the message names the namespace by its wire list `dayKinds` since the fix round (the v0 `dagsoorten` until then); row 3 of the collision table of IR-SPEC ch. 2.9 had no witness; `corpus/golden/jutterswet-schouwrooster` with a third day kind `Schouwdag` |
| `wf001-duplicate-unit-system` | WF-001 | D54 (U8) | toegevoegd | the `unitSystems` namespace: two unit-system names differing in case alone | the message names the namespace by its wire list `unitSystems` since the fix round (the v0 `eenheidstelsels` until then); `corpus/golden/jutterswet-vangstklem` with a second unit system `gewicht` |
| `wf009-duplicate-abbreviation` | WF-009 | D54 (U8) | toegevoegd | one abbreviation declared twice in one unit system | the new arm: the list form can write a second `t`, which the verifier, `ref` and the ASP backend passed while the Drools loader refused the document; `corpus/golden/jutterswet-vangstklem` with a second `t` that converts to `kg` by 100 |
| `wf005-parameter-facet-count-two` | WF-005 | B18 (U6) | toegevoegd | a parameter's facet with count 2 | the new arm: a parameter's count is 1 while `spec/RUNTIME-SEMANTICS.md` ch. 3.12 leaves multi-period grids open, because no executor evaluates it; `corpus/golden/anchor/jutterswet-penningvintage` with the count set to 2 |
| `wf017-plural-shadows-calendar-abbreviation` | WF-017 (a) | D55 (U7) | toegevoegd | arm (a): a plural that is a calendar abbreviation (`wk`) | the abbreviation class, whose witness left with `wf017-eenheidsleutel-schaduwt-kalenderafkorting`; `corpus/golden/jutterswet-schouwrooster` with the plural of `stormvloeddag` respelled `wk` |

### L4 — the rule and expression layer (2026-09-24)

L4 (card `20260924-l4-regel-en-expressielaag`,
`backlog/done/20260924-l4-regel-en-expressielaag.md`; the decision numbers D#
and U# below are that card's decision list) takes the rule layer and the
expression layer to their v1 form: the six named forms are
`{node, name, versions}`, a version carrying its own `validity` and a rule's
statute annotations sitting on the form (B19); a rule version's kind is its
`initialisation` flag and its `presentation` tag, and whether it writes an
attribute or a characteristic is the target's declaration (B20); `Functie` is
an `Aggregation` or a `BuiltinCall`, each with only its own fields (B21); the
single-type and the union uniqueness are one `Uniqueness` with a list of
`sources` (B27); a `Literal` carries a typed value in the variants of the case
format, a number with a point (B22); and `Constante` is a `ContextValue`, one
of the two session values, or the `BoundDay` of a day-kind definition (B23).
Every witness was measured before its stamp: the L3 verifier on the
document's L3 form against the L4 verifier on its L4 form, the translation of
`spec/proposals/v1-migratie/m4.py`. Over the 696 documents of the three zones
the two reports are identical on 388, differ only in their pointers on 274
(`wf034-untyped-literal`, rewritten by hand, among them), and differ in their
message on 29 — the L4 field names in WF-034's allocation messages and in
WF-083, WF-102 and WF-110, the field name in WF-082, the narrowed grammar of
WF-081, the two names of WF-098, the bound day in WF-031's session-value
sentence, and `anchor`/`target` in WF-109 —; on the other five the rule moved,
and they are the five removed below. A rewritten witness of the card's
stamp list carries `expect.txt`; so does every witness added here. The arm
column quotes the pinned fragment, shortened where it is long. Every NOTE
citation was taken again from the L4 verifier's output. The negative zone went
from 407 to 417 documents (five removed, fifteen added: seven with the
landing, eight in its fix round); the floor `EXPECTED_MIN_NEGATIVE` did not
have to move, and `EXPECTED_MIN_EXPECT` rose from 165 to 220 with the landing
and to 228 in its fix round (`scripts/check-verifier.sh`).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf024-complex` | WF-024 | B21, D16 | verwijderd-met-reden | an allocation method `COMPLEX` (the rule left with it) | `COMPLEX` is not in the method enum of three: a load error in the schema and in every loader, and WF-024 leaves IR-SPEC and the verifier as a history line; measured, m4.py refuses the document: "at /rules/verdelingen/0/methode: the method "COMPLEX": the L4 method is one of the three real methods, so the v0 placeholder of an unhandled form cannot be written" |
| `wf037-aantalgevulde-granulariteit` | WF-037 | B21, D35 | verwijderd-met-reden | a granularity facet on a non-bucketing kind, on an aggregation | an `Aggregation` carries no `granularity`: unwritable; the arm keeps its object on the builtin kinds, where the fix round added its witness `wf037-granularity-by-year-of` (below). Measured, m4.py refuses the document: "at /rules/resolvedRegels/0/waarde/granulariteit: an aggregation (AANTAL_GEVULDE) with the bucket "JAAR": the L4 shape writes granularity on a builtin call only" |
| `wf047-bereik-op-builtin` | WF-047 | B21, D35 | verwijderd-met-reden | a fold-source role binding on a builtin | a `BuiltinCall` carries no `elementType` and no range: unwritable; the arm keeps its object on an aggregation that is no instance fold, where the fix round added its witness `wf047-range-by-enumerated-fold` (below). Measured, m4.py refuses the document at `…/waarde/elementObjecttype` and `…/waarde/bronRol`: "a builtin call (ABSOLUTE_WAARDE) with elementObjecttype: the L4 shape writes that field on an aggregation only" (and idem `bronRol`) |
| `wf034-sentinel-builtin-wratoperand` | WF-034 | B21, D35 | verwijderd-met-reden | the member resolution of a builtin operand that carried a range | idem: the `ABSOLUTE_WAARDE` carries `elementObjecttype` and `bronRol`, so the arm switch L2 recorded (D9) loses its carrier; m4.py refuses the document with the two sentences of the row above |
| `wf030-single-source-union` | WF-030 | B27 | verwijderd-met-reden | a uniqueness union of one source (the arm left with it) | a uniqueness with one source IS the single-type form, so the arm "a uniqueness union carries at least two sources" loses its object; measured, the verifier accepts the translation (`verifier: PASS`). The witness had no `NOTE.md` |
| `wf034-untyped-literal` | WF-034 | B22, D36 | herschreven (i) | a value node without a static type (a typeless `Literal`) | m4.py chooses the variant by the type and refuses a typeless literal ("a typeless literal: the L4 shape chooses the value variant by the type, and there is none"); rewritten by hand to `"value": {"number": "2"}` with `"type": null`; `expect.txt` pins the arm. WF-114 stays silent: there is no type to cross the variant against |
| `wf021-version-overlap` | WF-021 | B19 | herschreven (m) | validity windows of versions 'Kenmerktoekenning jutter penninggerechtigd #1' and … | one `Rule` with two versions whose `validity` windows overlap; the report cites the first version |
| `wf020-crossform-collision` | WF-020 | B19 | herschreven (m) | collides with 'Kenmerktoekenning jutter penninggerechtigd' in verdelingen | the colliding name is the `name` of the form; the rule compares forms and cites the form |
| `wf020-defaults-group-collides-with-line` | WF-020 | B19 | herschreven (m) | name 'penninggerechtigd tenzij uitgesloten' collides with 'penninggerechtigd tenzij … | the colliding name is the `name` of the form; the rule compares forms and cites the form |
| `wf107-unnamed-rule` | WF-107 | B19 | herschreven (m) | form in resolvedRegels carries no name (a named form's identity on this wire IS its name | `Rule.name` `null` resp. blank; the report cites `/name` of the form |
| `wf107-blank-rule-name` | WF-107 | B19 | herschreven (m) | form in resolvedRegels carries a blank name (a named form's identity on this wire IS its … | `Rule.name` `null` resp. blank; the report cites `/name` of the form |
| `wf082-empty-source-annotations` | WF-082 | B19 | herschreven (m) | statuteAnnotations is empty — an annotation-free rule omits the field entirely | `statuteAnnotations` on the form, verbatim (`[]`, `[""]`, a padded string); the message names the L4 field |
| `wf082-blank-entry` | WF-082 | B19 | herschreven (m) | blank statute annotation — every entry carries text | `statuteAnnotations` on the form, verbatim (`[]`, `[""]`, a padded string); the message names the L4 field |
| `wf082-padded-entry` | WF-082 | B19 | herschreven (m) | statute annotation carries leading/trailing whitespace — entries are trimmed | `statuteAnnotations` on the form, verbatim (`[]`, `[""]`, a padded string); the message names the L4 field |
| `wf022-target-unresolvable` | WF-022 | B20 | herschreven (m) | target 'spookkenmerk' is neither an attribute nor a characteristic of 'Jutter' | the `target` of a `RuleVersion` without a `presentation` |
| `wf022-characteristic-false` | WF-022 | B20, B22 | herschreven (m) | characteristic target's value must be the boolean constant true | the value `{"boolean": false}` on a characteristic target; the value clause reads `{"boolean": true}` |
| `wf079-two-unconditional-writers` | WF-079 | B19, B20 | herschreven (m) | two unconditional computing rules write target (Jutter, uitkomst) with no prioritized … | two forms with one version each, `presentation: "BEREKENING"`, without `condition` |
| `wf023-initialisation-domain` | WF-023 | B20 | herschreven (m) | a value of the enumeration domain 'Kust' is assigned to a target of the enumeration … | `initialisation: true` |
| `wf028-create-along-final-hop` | WF-028 | B19 (D31) | herschreven (m) | the created relation must differ from the relation of the final hop | `path` is the steps followed by the final hop; the target type is the filler of `targetRole` |
| `wf034-allocation-empty-source-attribute` | WF-034 | B19 (D33) | herschreven (m) | sourceAttribute is present but empty: an attribute name that names nothing resolves … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-empty-ordering-attribute` | WF-034 | B19 (D33) | herschreven (m) | orderingAttribute is present but empty: an attribute name that names nothing resolves … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-empty-maximum-attribute` | WF-034 | B19 (D33) | herschreven (m) | maximumAttribute is present but empty: an attribute name that names nothing resolves … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-unknown-source-attribute` | WF-034 | B19 (D33) | herschreven (m) | 'bestaatniet' is not a declared attribute of the source type 'Berging': the allocation's … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-unknown-target-attribute` | WF-034 | B19 (D33) | herschreven (m) | 'bestaatniet' is not a declared attribute of the target type 'Berger': the allocation's … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-unknown-ordering-attribute` | WF-034 | B19 (D33) | herschreven (m) | 'bestaatniet' is not a declared attribute of the target type 'Jutter': the allocation's … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-unknown-maximum-attribute` | WF-034 | B19 (D33) | herschreven (m) | 'bestaatniet' is not a declared attribute of the target type 'Berger': the allocation's … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-unknown-ratio-attribute` | WF-034 | B19 (D33) | herschreven (m) | 'bestaatniet' is not a declared attribute of the target type 'Berger': the allocation's … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-unknown-remainder-attribute` | WF-034 | B19 (D33) | herschreven (m) | 'bestaatniet' is not a declared attribute of the source type 'Berging': the allocation's … | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-without-source-attribute` | WF-034 | B19 (D33) | herschreven (m) | allocation without a source attribute: it resolves against no declaration | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-without-source-type` | WF-034 | B19 (D33) | herschreven (m) | allocation without a source object type: it resolves against no declaration | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-without-target-attribute` | WF-034 | B19 (D33) | herschreven (m) | allocation without a target attribute: it resolves against no declaration | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-allocation-without-target-type` | WF-034 | B19 (D33) | herschreven (m) | allocation without a target object type: it resolves against no declaration | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf025-max-without-ratio` | WF-025 | B19 (D33) | herschreven (m) | maximum requires a ratio attribute | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf026-two-allocations-one-target` | WF-026 | B19 (D33) | herschreven (m) | two allocations write target (Jutter, aantal aangemelde vondsten) | the `AllocationVersion` field names; the message names `sourceAttribute`, `orderingAttribute`, … where it named the v0 fields |
| `wf034-object-creation-without-subject` | WF-034 | B19 (D32) | herschreven (m) | object creation without a subject | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf034-object-creation-unknown-subject` | WF-034 | B19 (D32) | herschreven (m) | object creation's subject type 'Bestaatniet' is not declared: the creation resolves … | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf027-ghost-member` | WF-027 | B19 (D32) | herschreven (m) | 'spook' is not a declared attribute of 'Jutter' | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf027-lying-attribute-reference-type` | WF-027 | B19 (D32) | herschreven (m) | characteristic initialisation's value must be a boolean-typed expression | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf027-nonboolean-characteristic-init` | WF-027 | B19 (D32) | herschreven (m) | characteristic initialisation's value must be a boolean-typed expression | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf047-creation-binding-wrong-filler` | WF-047 | B19 (D32) | herschreven (m) | the bound role 'dossierhouder' fills 'Jutter', but the node names 'Dossier' | `ObjectCreationVersion` (`subject`, `initialisations`, `connectingRole`) |
| `wf029-no-subject` | WF-029 | B19 | herschreven (m) | consistency without a subject type | `ConsistencyVersion.subject: null` |
| `wf030-arity-mismatch` | WF-030 | B27 | herschreven (m) | union sources disagree on key arity | `sources` with two `Source` nodes whose `keys` differ in length |
| `wf054-dangling-scope-binding` | WF-054 | B27 | herschreven (m) | role binding names undeclared fact type 'wijkindeling' | the scope binding is the version's `scope`, which the report cites |
| `wf054-to-many-scope-role` | WF-054 | B27 | herschreven (m) | is to-many — a scope parent is one instance | the scope binding is the version's `scope`, which the report cites |
| `wf037-count-none-operand` | WF-037 | B21 | herschreven (m) | the count is an INSTANCE fold and MUST carry an element type | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-count-list-literal` | WF-037 | B21 | herschreven (m) | the count is an INSTANCE fold and MUST carry an element type | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-count-enumerated` | WF-037 | B21 | herschreven (m) | a count folds INSTANCES and has no LISTED form, and this node carries 2 operands | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-count-enumerated-triple` | WF-037 | B21 | herschreven (m) | a count folds INSTANCES and has no LISTED form, and this node carries 3 operands | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-count-enumerated-element` | WF-037 | B21 | herschreven (m) | a count folds INSTANCES and has no LISTED form, and this node carries 2 operands | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-count-without-element-type` | WF-037 | B21 | herschreven (m) | the count is an INSTANCE fold and MUST carry an element type | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-filled-count-dimension-selection` | WF-037 | B21 | herschreven (m) | dimension selection on a kind other than sum/min/max | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-filled-count-empty-fallback` | WF-037 | B21 | herschreven (m) | empty-value fallback on a non-sum aggregation | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-filled-count-not-set-source` | WF-037 | B21 | herschreven (m) | the filled count's single argument MUST be its set source, an attribute reference read on … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-filled-count-second-argument` | WF-037 | B21 | herschreven (m) | the filled count takes exactly ONE argument, its set source, and this node carries 2 | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-filled-count-without-element-type` | WF-037 | B21 | herschreven (m) | the filled count is an INSTANCE fold and MUST carry an element type | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf037-fallback-on-minimum` | WF-037 | B21 | herschreven (m) | empty-value fallback on a non-sum aggregation | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-first-mixed-date-precision` | WF-038 | B21 | herschreven (m) | EERSTE orders dates of two precisions (day and millisecond), which share no order | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-first-mixed-family` | WF-038 | B21 | herschreven (m) | EERSTE orders operands of two comparable families, numbers and dates, which share no … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-first-sum-type-articulated` | WF-038 | B21 | herschreven (m) | operand of the EERSTE ordering fold is sumtype, must be a number or a date | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-extractor-text` | WF-038 | B21 | herschreven (m) | day-precision extractor operand is text, must be a date | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-last-enum-articulated` | WF-038 | B21 | herschreven (m) | operand of the LAATSTE ordering fold is enumeration, must be a number or a date | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-maximum-sum-type-set-source` | WF-038 | B21 | herschreven (m) | operand of the MAXIMUM ordering fold is sumtype, must be a number or a date | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-minimum-enum-set-source` | WF-038 | B21 | herschreven (m) | operand of the MINIMUM ordering fold is enumeration, must be a number or a date | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-easter-sunday-text` | WF-038 | B21 | herschreven (m) | eerste-paasdag operand is text, must be a whole unitless number | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-duration-mixed-precision` | WF-038 | B21 | herschreven (m) | tijdsduur operands mix date precisions (both must share one precision) | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-duration-without-node-type` | WF-038 | B21 | herschreven (m) | tijdsduur without a node type (the counted unit is the node type's unit) | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf038-duration-without-time-unit` | WF-038 | B21 | herschreven (m) | tijdsduur without a recognised time unit on its node type | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf083-total-foreign-element-type` | WF-083 | B21 | herschreven (m) | of a TOTAAL without a range is not the fold subject | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf083-fold-source-anchor-by-operand` | WF-083 | B21 | herschreven (m) | is not the fold's anchor 'Jutter' (a count source is read on the fold subject) | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf083-fold-source-anchor-by-intermediate-type` | WF-083 | B21 | herschreven (m) | fold-source objecttype 'Boot' is not the fold's anchor 'Vondst' (a set source reads each … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf083-fold-source-foreign-element-type` | WF-083 | B21 | herschreven (m) | fold-source objecttype 'Jutter' is not the fold's anchor 'Vondst' (a set source reads … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf083-fold-source-foreign-object-type` | WF-083 | B21 | herschreven (m) | fold-source objecttype 'Jutter' is not the fold's anchor 'Vondst' (a fold without a range … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf093-filled-count-wrat` | WF-093 | B21 | herschreven (m) | value aggregation over "vondsten", which is no attribute of "Vondst": a value fold reads … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf093-count-source-under-sum` | WF-093 | B21 | herschreven (m) | value aggregation over "vondsten", which is no attribute of "Vondst": a value fold reads … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf093-count-over-attribute-source` | WF-093 | B21 | herschreven (m) | a count folds INSTANCES, so its source is a count source naming the relation's role, … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf093-wrat-fold-bare-first` | WF-093 | B21 | herschreven (m) | value aggregation over "vondsten", which is no attribute of "Vondst": a value fold reads … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf093-wrat-fold-bare-sum` | WF-093 | B21 | herschreven (m) | value aggregation over "vondsten", which is no attribute of "Vondst": a value fold reads … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf102-source-path-a-step` | WF-102 | B21 | herschreven (m) | a sourcePath carries at least TWO steps, found 1 — the one-step range is spelled … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf102-source-path-lands-beside-element-type` | WF-102 | B21 | herschreven (m) | the last step's bound role 'ingepakte kist' fills 'Kist', but the fold's elementType is … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf102-source-path-empty` | WF-102 | B21 | herschreven (m) | a sourcePath carries at least TWO steps, found 0 — the one-step range is spelled … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf102-source-path-unconnected-step` | WF-102 | B21 | herschreven (m) | the opposite role 'verpakte vondst' fills 'Vondst', but this step starts at 'Boot' | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf102-source-path-by-negated-set-source` | WF-102 | B21 | herschreven (m) | a sourcePath is admissible only as the range of a single-argument VALUE fold's set source | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf102-source-path-start-negated-by-subject` | WF-102 | B21 | herschreven (m) | the opposite role 'bergende boot' fills 'Boot', but this step starts at 'Jutter' (the … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf110-union-as-rule-value` | WF-110 | B21 | herschreven (m) | an instance-set union stands where nothing consumes it | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf110-union-a-branch` | WF-110 | B21 | herschreven (m) | an instance-set union carries at least TWO selections and this one carries 1 | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf110-union-beside-fold-source` | WF-110 | B21 | herschreven (m) | a count over an instance-set union names no source of its own, and this node carries … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf110-union-under-a-builtin` | WF-110 | B21 | herschreven (m) | an instance-set union stands where nothing consumes it | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf110-union-unequal-element-types` | WF-110 | B21 | herschreven (m) | the selections of an instance-set union range over ONE population | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf110-union-branch-without-element-type` | WF-110 | B21 | herschreven (m) | an instance-set union's selection MUST name the population it ranges over (ch. 4.3) | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf056-bucket-target-mismatch` | WF-056 | B21 | herschreven (m) | pro-rata bucket granularity differs from the target's declared granularity | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf056-no-during-condition` | WF-056 | B21 | herschreven (m) | tijdsevenredig-deel without a gedurende condition | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf056-nontemporal-characteristic` | WF-056 | B21 | herschreven (m) | gedurende over 'borgplichtig', which is not a temporal characteristic | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf056-scalar-base` | WF-056 | B21 | herschreven (m) | pro-rata base 'borgtarief' is not a temporal attribute | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf055-composite-comparison-leaf` | WF-055 | B21 | herschreven (m) | a composite gedurende member is an own temporal characteristic check | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf055-composite-nested` | WF-055 | B21 | herschreven (m) | a composite gedurende member is an own temporal characteristic check | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf055-composite-nontemporal-leaf` | WF-055 | B21 | herschreven (m) | gedurende over 'erkend', which is not a temporal characteristic of 'Jutter' | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf055-navigated-clip-condition` | WF-055 | B21 | herschreven (m) | the gedurende condition is an own temporal characteristic check, a per-segment comparison … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf055-nontemporal-clip-characteristic` | WF-055 | B21 | herschreven (m) | gedurende over 'wachtplichtig', which is not a temporal characteristic of 'Jutter' | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf047-bound-role-fills-source` | WF-047 | B19 | herschreven (m) | the bound role 'berging' fills 'Berging', but the node names 'Berger' | `AllocationVersion.connectingRole` (the defect sits on the allocation's connecting role, not on a fold); the verifier cites `/rules/verdelingen/0/versions/0/connectingRole`
| `wf047-source-path-step-unresolved-role` | WF-047 | B21 | herschreven (m) | role binding names no declared role 'geborgen vondsten' of fact type 'berging' (the … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf047-dangling-role-binding` | WF-047 | B19 | herschreven (m) | role binding names no declared role 'bewaker' of fact type 'bergingsdeelname' (the plural … | `AllocationVersion.connectingRole` (the defect sits on the allocation's connecting role, not on a fold); the verifier cites `/rules/verdelingen/0/versions/0/connectingRole`
| `wf047-reciprocal-wrong-filler` | WF-047 | B21 | herschreven (m) | the bound role 'maat' fills 'Jutter', but the node names 'Vondst' | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf034-fold-source-unknown-attribute` | WF-034 | B21 | herschreven (m) | 'bestaatniet' is not a declared attribute of 'Jutter': a value fold's source reads an … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf034-fold-source-without-attribute` | WF-034 | B21 | herschreven (m) | attribute reference on 'Vondst' without an attribute name: it resolves against no … | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf034-fold-source-without-object-type` | WF-034 | B21 | herschreven (m) | attribute reference 'gewicht' without an object type: it resolves against no declaration | `Aggregation`/`BuiltinCall` with their L4 fields; a message that named `elementObjecttype`, `bronRol`, `bronPad` or `argumenten` names the L4 field |
| `wf081-unit-in-lexeme` | WF-081 | B22 | herschreven (m) | numeric literal lexeme '5 €' is not a number of the case format | `{"number": "5 €"}`; the message states the narrowed grammar (U19) |
| `wf084-bare-non-iso` | WF-084 | B22 | herschreven (m) | date literal lexeme '31-01-2024' is not the ISO form yyyy-mm-dd (fixed field widths, … | the string in the `date` variant; the report cites `/value` |
| `wf084-source-snippet` | WF-084 | B22 | herschreven (m) | date literal lexeme 'dd. 31-01-2024' is not the ISO form yyyy-mm-dd (fixed field widths, … | the string in the `date` variant; the report cites `/value` |
| `wf095-source-snippet` | WF-095 | B22 | herschreven (m) | millisecond-precision date literal lexeme '15-03-2026 12:00:00.000' is not the canonical … | the string in the `dateTime` variant, each its own spelling fault; the report cites `/value` |
| `wf095-fraction-width` | WF-095 | B22 | herschreven (m) | millisecond-precision date literal lexeme '2026-03-15T12:00:00.1' is not the canonical … | the string in the `dateTime` variant, each its own spelling fault; the report cites `/value` |
| `wf095-surrounding-text` | WF-095 | B22 | herschreven (m) | millisecond-precision date literal lexeme 'rommel 2026-03-15T12:00:00 x' is not the … | the string in the `dateTime` variant, each its own spelling fault; the report cites `/value` |
| `wf095-missing-seconds` | WF-095 | B22 | herschreven (m) | millisecond-precision date literal lexeme '2026-03-15T12:00' is not the canonical ISO … | the string in the `dateTime` variant, each its own spelling fault; the report cites `/value` |
| `wf095-space-separator` | WF-095 | B22 | herschreven (m) | millisecond-precision date literal lexeme '2026-03-15 12:00:00' is not the canonical ISO … | the string in the `dateTime` variant, each its own spelling fault; the report cites `/value` |
| `wf095-zone-suffix` | WF-095 | B22 | herschreven (m) | millisecond-precision date literal lexeme '2026-03-15T12:00:00Z' is not the canonical ISO … | the string in the `dateTime` variant, each its own spelling fault; the report cites `/value` |
| `wf039-negative-under-nonneg` | WF-039 | B22 | herschreven (m) | negative literal '-3' under a non-negative sign restriction | `{"number": "-3"}` under `NIET_NEGATIEF` |
| `wf098-unknown-context-value-name` | WF-098 | B23 | herschreven (m) | context value 'de dagX' is not one of the two admissible names Rekendatum, Rekenjaar | `ContextValue {name: "de dagX"}`; the closed set holds two names |
| `wf098-context-value-name-capitalization` | WF-098 | B23 | herschreven (m) | context value 'rekendatum' is not one of the two admissible names Rekendatum, Rekenjaar | `ContextValue {name: "rekendatum"}`; the closed set holds two names |
| `wf031-session-value-in-day-kind` | WF-031 | B23 | herschreven (m) | day-kind condition reads the session value 'Rekendatum' | a `ContextValue` `Rekendatum` in a day-kind condition; the message admits the bound day (`BoundDay`) |

Mechanical, without an L4 delta on the defect (no row): the 26 witnesses whose
consistency had `geldigheid: null` and the 11 witnesses with a rule of
`geldigheid: null` (46 versions), which now write the always-validity
`{"vanaf": null, "tot": null}`; the 6 witnesses with a decimal-comma literal,
which now write a point in `value.number` (`wf001-duplicate-unit-system`,
`wf009-duplicate-abbreviation`, `wf034-unit-unknown-system`,
`wf045-inherited-attribute-spelling-cycle`, `wf092-canonical-without-factor`,
`wf092-factor-without-canonical`); `wf036-equality-enumeration-against-sum-type`,
the one carrier of the `constructor` variant (`{"constructor": "Wad"}`; WF-036
reads the type, not the value); and every other document whose pointers alone
moved. The 20 witnesses of WF-045 and WF-046 moved in their pointer only: the
dependency graph records a form at the pointer of its version.

#### Added at L4 (2026-09-24)

A witness for each arm L4 added or moved onto a new field (`expect.txt` and
`NOTE.md` in each). Each is arm-exact: with its arm disabled it turns clean
and no other document of golden, red and negative moves (698 compared).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf022-target-attribute-and-characteristic` | WF-022 | N3 (U22) | toegevoegd | a target name that is an attribute and a characteristic of the subject type | the new arm: the name decides no member kind; `corpus/golden/anchor/jutterswet-toelagegronden`, the arm's golden, with a rule writing `toelage` and without its defaults groups |
| `wf022-characteristic-target-with-presentation` | WF-022 | B20 (U11) | toegevoegd | a version with a `presentation` whose target is a characteristic | the new member-kind arm, first direction; `corpus/golden/jutterswet-penningdrempel` with `presentation: "GELIJKSTELLING"` on its characteristic rule |
| `wf022-attribute-target-without-presentation` | WF-022 | B20 (U11) | toegevoegd | a version without `presentation` and `initialisation` whose target is an attribute | the member-kind arm, second direction; `corpus/golden/jutterswet-meldloon` with the `presentation` of its first rule removed |
| `wf114-literal-variant-against-type` | WF-114 | B22 (U21) | toegevoegd | a `text` variant under a `Number` | the new rule; `corpus/golden/jutterswet-meldloon` with `{"number": "5"}` respelled `{"text": "5"}` |
| `wf114-date-variant-against-precision` | WF-114 | B22 (U21) | toegevoegd | a `date` variant under a millisecond-precision `Date` | the new rule, on the one datatype node that names two variants; `corpus/golden/jutterswet-meldstip` with its `dateTime` respelled as the `date` of the same day |
| `wf098-de-dag-as-context-value` | WF-098 | B23 (U23) | toegevoegd | a `ContextValue` named `de dag` | the closed set holds two names since L4; `corpus/golden/jutterswet-bergingsklok` with its first `BoundDay` replaced by that context value |
| `wf081-decimal-comma` | WF-081 | B22 (U19) | toegevoegd | a decimal comma in a `number` variant | the narrowed grammar; `corpus/golden/jutterswet-meldloon` with `{"number": "12.50"}` respelled `{"number": "12,50"}` (its `bron` already carries the comma) |

#### Added in the L4 fix round (2026-09-24)

The lens of the landing found five arms the L4 wire still reaches without a
witness: two lost theirs with the removals above, and three never had one (no
`wf030-*` directory in the repository's history pinned the two WF-030 arms,
and no corpus file ever carried the sentence of the bucketing arm). The fix
round found three more WF-037 arms without a witness — the two builtin
arities and the operand count of a value fold; no negative document reported
them. One witness each, eight in all, with `expect.txt` and `NOTE.md`. Each is
arm-exact: with its arm emptied it turns clean and no other document of
golden, red and negative moves (706 compared, for all eight).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf037-granularity-by-year-of` | WF-037 | B21 | toegevoegd | granularity facet on a non-bucketing kind | the arm lost its witness with `wf037-aantalgevulde-granulariteit`; `corpus/golden/jutterswet-paasvondst` with `"granularity": "JAAR"` on its `JAAR_UIT` call |
| `wf037-time-proportional-part-without-granularity` | WF-037 | B21 | toegevoegd | bucketing builtin without a granularity | the arm had no witness; `corpus/golden/jutterswet-bakenmeester` with the `granularity` of its `TIJDSEVENREDIG_DEEL` removed |
| `wf047-range-by-enumerated-fold` | WF-047 | B21 | toegevoegd | a fold-source role binding on a node that is not an instance fold over a relation | the arm lost its witness with `wf047-bereik-op-builtin`; `corpus/golden/jutterswet-sluitsom` with a `sourceRole` on its listed `LAATSTE` (the relation its second operand navigates) |
| `wf030-source-type-not-declared` | WF-030 | B27 | toegevoegd | source type 'Vondsten' is not declared | the arm had no witness; `corpus/golden/jutterswet-vondstnummer` with the source's `objectType` spelled as the declared plural |
| `wf030-key-none-attribute` | WF-030 | B27 | toegevoegd | key 'vondstnummers' is not a declared attribute of 'Vondst' | the arm had no witness; `corpus/golden/jutterswet-vondstnummer` with the source's key spelled as the attribute's declared plural |
| `wf037-year-of-two-operands` | WF-037 | B21 | toegevoegd | builtin arity 2, expected 1 | the arm had no witness; `corpus/golden/jutterswet-paasvondst` with the one argument of its `JAAR_UIT` repeated as a second |
| `wf037-duration-one-operand` | WF-037 | B21 | toegevoegd | builtin arity 1, expected 2 | the arm had no witness; `corpus/golden/jutterswet-getijdenpaar` with the second argument of its `TIJDSDUUR` removed |
| `wf037-first-one-operand` | WF-037 | B21 | toegevoegd | value fold with fewer than two operands | the arm had no witness; `corpus/golden/jutterswet-hoogwaterpaar` with the second operand of its listed `EERSTE` removed |

### L5 — the source record and the renaming (2026-09-25)

L5 (card `20260924-l5-bronrecord-en-hernoeming`,
`backlog/done/20260924-l5-bronrecord-en-hernoeming.md`; the decision numbers D#
and U# below are that card's decision list) completes the v1 wire in two
steps. M7, the source record (B26): every `bron` string became
`"source": {"snippet": …}`, byte for byte; a `bron: null` became absent; the
`line` of a consistency version and of a rule group went into `source.line`;
`source` is required on an `Unresolved` marker, and WF-113 (b) reads
`source.snippet`. M8, the renaming (B6, B10): every node name and key that
L1–L4 left in its v0 spelling carries its IR-SPEC name — among them
`Kenmerk.eigenaar` → `CharacteristicCheck.host`, `AttribuutRef.objecttype` →
`AttributeReference.subjectType`, `TemporalRead {base, date}`,
`WholePeriod {granularity, negated}` and the nine lists under `rules`
(D48–D51); every enumeration value follows the one spelling rule of N2,
English lowerCamel (D47); and a field that IR-SPEC calls optional went from
`null` to absent (D5 (a)). No rule came and none went.

M7 was measured on its own commits before anything else moved (U1, U2): over
the 736 documents of the four zones and the parity zone the verifier's output
stayed byte-identical except for the ten negative reports whose message named
`bron` — the six WF-095 witnesses and the four WF-113 (b) witnesses — and no
output of `ref`, clingo or Drools moved. Every witness was then measured
before its stamp: the L4 verifier on the document's L4 form against the L5
verifier on its L5 form, the translation of `spec/proposals/v1-migratie/m5.py`
(M7 + M8). Over the 417 witnesses the two reports carry the same rules in the
same order; they are identical on 70, differ only in their pointers on 252,
only in their message on 10 and in both on 85 — every pointer difference a
renamed segment, every message difference a name. The 289 golden and red
documents stay verifier-clean.

No witness is removed: the empty snippet and the marker without a `source`
stay witnesses of WF-113 (b), which the verifier's reading keeps judging. Two
witnesses whose L4 shape L5 cannot carry were rewritten by hand (i); every
other row below carries its L5 delta on the defect and is the migration's
translation (m), except `wf113-marker-source-null`, which the migration refuses
(the `source` of a marker is required) and which is written by hand. Each row
carries `expect.txt`: 31 pinned their arm for the first time, 12 re-pinned it
where the message moved, and 3 kept it. The arm column quotes the pinned
fragment, shortened where it is long. Every NOTE citation was taken again from
the L5 verifier's output; a NOTE that describes a v0 form, or a measurement
taken on one, says so and keeps that form's names. The negative zone went from
417 to 418 documents (none removed, one added); the floor
`EXPECTED_MIN_NEGATIVE` did not have to move, and `EXPECTED_MIN_EXPECT` rose
from 228 to 229 with the added witness and to 260 with the 31 first pins
(`scripts/check-verifier.sh`).

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf113-marker-source-empty` | WF-113 (b) | B26 (D42, U10) | herschreven (i) | Unresolved marker whose source snippet is blank | m5.py refuses `"bron": ""` (an empty snippet does not exist: the schema's `minLength: 1`, and every reader refuses it at load); rewritten by hand to `"source": {"snippet": " "}`, a snippet of one space, so the blank arm keeps its defect |
| `wf098-context-value-name-capitalization` | WF-098 | N2 (D47, U21) | herschreven (i) | context value 'EvaluationDate' is not one of the two admissible names evaluationDate, … | m5.py carries `rekendatum` verbatim (a name outside the table); rewritten by hand to `EvaluationDate`, the recapitalised L5 name, so the witness keeps pinning that the set is compared byte for byte |
| `wf034-characteristic-check-unknown-host` | WF-034 (q) | B10 (D48) | herschreven (m) | characteristic check's host 'Bestaatniet' is not a declared object type | `host: "Bestaatniet"`; the report cites `…/host` |
| `wf100-host-declares-characteristic-negated` | WF-100 | B10 (D48) | herschreven (m) | 'gekeurd' is not a declared characteristic of 'Jutter' | `host: "Jutter"`, which declares no `gekeurd`; the incidental WF-109 line stays |
| `wf109-target-host-navigated` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Sein', which no object bound here can be | `host: "Sein"` beside a `navigation` |
| `wf109-target-host-period` | WF-109 | B10 (D48), B6 (D51) | herschreven (m) | the characteristic check is hosted on 'Sein', which no object bound here can be | `host: "Sein"` beside a `WholePeriod` |
| `wf109-target-host-compound` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Sein', which no object bound here can be | `host: "Sein"` on the first member of an `all` composite, the second hosted on `Kustvak` |
| `wf109-subtype-overlap` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Sein', which the target 'Sein' is — but so can … | `host: "Sein"`, declared a subtype of the anchor `Kustvak` |
| `wf109-host-ambiguous-supertype` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Baken', which the anchor 'Kustvak' and the … | `host: "Baken"`, a supertype of anchor and target |
| `wf109-host-ambiguous-self-typed` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Wachter', which the anchor 'Wachter' and the … | `host: "Wachter"` on a self-typed creation |
| `wf109-host-outside-range` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Jutter', which no object bound here can be | `host: "Jutter"` where the subject is `Vondst` |
| `wf109-host-partial-population` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Strandvondst', which no object bound here IS | `host: "Strandvondst"`, a subtype of the subject `Vondst`; rewritten to the guard of a fact creation on 2026-10-27 (below, `subtype-characteristic-in-fold-filter`) |
| `wf109-host-third-type` | WF-109 | B10 (D48) | herschreven (m) | the characteristic check is hosted on 'Post', which no object bound here can be | `host: "Post"`, beside the legal target host `Sein` of the second creation |
| `wf097-clip-characteristic-host` | WF-097 | B10 (D48) | herschreven (m) | host 'Jutter' on a gedurende clip member | `host: "Jutter"` on the member of the clip; the message names `host` |
| `wf097-day-count-characteristic-host` | WF-097 | B10 (D48) | herschreven (m) | host 'Jutter' on a gedurende clip member | idem, on the day count's clip |
| `wf097-prorata-characteristic-host` | WF-097 | B10 (D48) | herschreven (m) | host 'Jutter' on a gedurende clip member | idem, on the pro-rata clip |
| `wf097-clip-characteristic-period` | WF-097 | B6 (D51) | herschreven (m) | wholePeriod qualifier (affirmative) on the gedurende clip member 'aangesteld' | `wholePeriod: {"node": "WholePeriod", "granularity": "year", "negated": false}` on the member of the clip; the message names `wholePeriod` |
| `wf097-clip-characteristic-period-negated` | WF-097 | B6 (D51) | herschreven (m) | wholePeriod qualifier (negated) on the gedurende clip member 'aangesteld' | idem, with `"negated": true` |
| `wf097-day-count-characteristic-period` | WF-097 | B6 (D51) | herschreven (m) | wholePeriod qualifier (affirmative) on the gedurende clip member 'borgplichtig' | idem, affirmative, on the day count's clip |
| `wf097-prorata-characteristic-period` | WF-097 | B6 (D51) | herschreven (m) | wholePeriod qualifier (affirmative) on the gedurende clip member 'borgplichtig' | idem, affirmative, on the pro-rata clip |
| `wf043-wholeperiod-nontemporal` | WF-043 | B6 (D51) | herschreven (m) | wholePeriod qualifier on a non-time-dependent operand | a `WholePeriod` on a non-temporal operand; the message names `wholePeriod` |
| `wf113-marker-source-null` | WF-113 (b) | B26 (D46, U11) | herschreven (m) | Unresolved marker without a source | an `Unresolved` without `source`: m5.py refuses it (a required field), written by hand; the verifier's loader loads it, every executor refuses it at load, and the null arm refuses it |
| `wf113-marker-without-report` | WF-113 (b) | B26 (D46) | herschreven (m) | Unresolved marker 'de verjaringsgrens' is not accounted for | `source.snippet` on the expression marker; the message names the snippet |
| `wf113-report-names-marker-negated` | WF-113 (b) | B26 (D46) | herschreven (m) | Unresolved marker 'zijn vondstdag eerder is dan de verjaringsgrens' is not accounted for | `source.snippet` on the condition marker; the message names the snippet |
| `wf113-severity-error` | WF-113 (a) | N2 (D47, U22) | herschreven (m) | severity error is not a wire severity | `severity: "error"`, refused by name; the verifier's loader loads it, every executor refuses it at load |
| `wf113-severity-info` | WF-113 (a) | N2 (D47, U22) | herschreven (m) | severity info is not a wire severity | `severity: "info"`, idem |
| `wf113-severity-null` | WF-113 (a) | N2 (D47) | herschreven (m) | diagnostic without a severity | `severity: null`; the message names `warning` as the one wire severity |
| `wf108-quantifier-fold-quantifier-the` | WF-108 (a) | N2 (D47, U22) | herschreven (m) | quantifier fold with quantifier the | `QuantifierFold.quantifier: "the"`, refused by name; the verifier's loader loads it, every executor refuses it at load |
| `wf098-unknown-context-value-name` | WF-098 | N2 (D47) | herschreven (m) | context value 'de dagX' is not one of the two admissible names evaluationDate, … | `de dagX` verbatim; the message names `evaluationDate`, `evaluationYear` |
| `wf098-de-dag-as-context-value` | WF-098 | N2 (D47) | herschreven (m) | context value 'de dag' is not one of the two admissible names evaluationDate, … | `de dag` verbatim; idem |
| `wf031-session-value-in-day-kind` | WF-031 | N2 (D47) | herschreven (m) | day-kind condition reads the session value 'evaluationDate' | `ContextValue {name: "evaluationDate"}` in a `DayKindDefinition.condition` |
| `wf049-read-at-datetime` | WF-049 | B6 (D50) | herschreven (m) | the read's date operand is date-ms, must be a day-precision date | `TemporalRead {base, date}`: the `date` operand at millisecond precision |
| `wf049-read-of-nontemporal` | WF-049 | B6 (D50) | herschreven (m) | temporal read of 'vast quotum', which is not a temporal attribute of 'Jutter' | `TemporalRead {base, date}`: a `base` that reads an attribute without a timeline |
| `wf049-type-not-element` | WF-049 | B6 (D50) | herschreven (m) | the read's type must equal the timeline's element datatype | `TemporalRead {base, date}`: a node `type` other than the timeline's element type |
| `wf053-own-read-supertype` | WF-053 | B6 (D49) | herschreven (m) | an attribute reference without a path is the subject's own read, so its subjectType is … | `subjectType: "Vondst"`, a supertype of the subject `Munt`, without a `path` |
| `wf034-attribute-reference-without-object-type` | WF-034 | B6 (D49) | herschreven (m) | attribute reference 'aangevoerd gewicht' without a subjectType: it resolves against no … | `subjectType: null`; the message names `subjectType` |
| `wf083-fold-source-anchor-by-intermediate-type` | WF-083 | B6 (D49) | herschreven (m) | fold-source subjectType 'Boot' is not the fold's anchor 'Vondst' (a set source reads … | the fold source's `subjectType` names an intermediate type of the path; the message names `subjectType` |
| `wf083-fold-source-foreign-element-type` | WF-083 | B6 (D49) | herschreven (m) | fold-source subjectType 'Jutter' is not the fold's anchor 'Vondst' (a set source reads … | the set source's `subjectType` names the fold subject; idem |
| `wf083-fold-source-foreign-object-type` | WF-083 | B6 (D49) | herschreven (m) | fold-source subjectType 'Jutter' is not the fold's anchor 'Vondst' (a fold without a … | the own read's `subjectType` names another type; idem |
| `wf021-version-overlap` | WF-021 | B6, D5 (a) | herschreven (m) | validity windows of versions 'Kenmerktoekenning jutter penninggerechtigd #1' and … | two versions whose `Validity {from?, to?}` windows overlap; the list name in the message is `rules` |
| `wf101-validity-bound-calendar-day` | WF-101 | B6, D5 (a) | herschreven (m) | validity bound '2026-02-30' is not the ISO form yyyy-mm-dd | the invalid bound in `to` of a `Validity` |
| `wf101-validity-bound-signed-year` | WF-101 | B6, D5 (a) | herschreven (m) | validity bound '+012026-01-01' is not the ISO form yyyy-mm-dd | the invalid bound in `from` of a `Validity` |
| `wf042-empty-window` | WF-042 | B6, D5 (a) | herschreven (m) | time window without any bound | a `TimeWindow` without `from` and without `to` (v0: both `null`) |
| `wf111-date-time-bound-attribute` | WF-111 | B6 | herschreven (m) | the from bound of a time window is a millisecond-precision date: the window is a range … | a millisecond-precision attribute in `TimeWindow.from`; the message names `from` |
| `wf111-date-time-bound-literal` | WF-111 | B6 | herschreven (m) | the from bound of a time window is a millisecond-precision date: the window is a range … | a millisecond-precision literal in `TimeWindow.from`; idem |
| `wf111-period-boundary-parameter-date` | WF-111 | B6 | herschreven (m) | the from bound of a time window is the parameter 'peildatum' | a parameter in `TimeWindow.from`; idem |

Mechanical, without an L5 delta on the defect (no row): the other 371
witnesses, of which 235 moved in their pointer only, 7 in their message only,
59 in both and 70 not at all. Among them, the 34 whose `expect.txt` named a
kind, an operator, a list or a field in its v0 spelling were re-pinned from the
real output: WF-020 and WF-107 (the list names, four), WF-038 (ten), WF-089
(three), WF-090 (one), WF-091 (twelve), and one each of WF-034
(`wf034-fold-source-without-object-type`), WF-053
(`wf053-read-fold-operand-unconnected-hop`), WF-056
(`wf056-no-during-condition`) and WF-083 (`wf083-total-foreign-element-type`).

#### Added at L5 (2026-09-25)

| directory | rule | delta | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf098-v0-name-evaluation-date` | WF-098 | N2 (D47, U20) | toegevoegd | context value 'Rekendatum' is not one of the two admissible names evaluationDate, … | the v0 spelling of `evaluationDate` falls outside the closed set since L5, as `de dag` has since L4; `corpus/golden/anchor/jutterswet-penningvintage` with its one `ContextValue` named `Rekendatum`. Arm-exact: with the name readmitted it alone turns clean (707 documents compared) |

## Vervallen bij een normwijziging

A witness whose defect a later decision admits leaves the corpus with the
stamp of the v1 migration, **verwijderd-met-reden**, in a row of this list,
measured before its removal, so that a witness never leaves the corpus
unrecorded. The floor `EXPECTED_MIN_NEGATIVE` in `scripts/check-verifier.sh`
is a lower bound and does not have to move.

### `instance-cycle-household-reads` (2026-10-22)

The card (`backlog/done/20261022-instance-cycle-household-reads.md`,
voorlopig besluit B2.1) widens the reads of an instance-ordered part
(`spec/IR-SPEC.md` ch. 5.3) to the household of an object: its satellites
over the satellite relations of the cycle's type, which all stand at its
depth. A hop to a satellite may reach a to-many role (the range of a fold or
a quantifier); the owner's role stays to-one.

| directory | rule | card | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf045-instance-cycle-to-many-satellite-step` | WF-045 | B2.1 | verwijderd-met-reden | `WF-045.dependency-cycle`, the hop after the predecessor reaching a to-many role | a fold over the termijnen of the vorige vergunning lands in the household of the predecessor, every member of which stands at its depth: since B2.1 a shifted read whatever the cardinality on the satellite's side. Measured 2026-10-22: the document is verifier-clean on the verifier of the card. The form is a clause of the golden `corpus/golden/jutterswet-vergunningtermijnen` (the verlengde einddag, a sum over the termijnen of the vorige vergunning) |

### `subtype-characteristic-in-fold-filter` (2026-10-27)

The card (`backlog/done/20261027-subtype-characteristic-in-fold-filter.md`,
voorlopige besluiten D1–D4) admits a host strictly below the one object a
position binds: the narrowing host (`spec/IR-SPEC.md` ch. 4.6 (q2),
`spec/RUNTIME-SEMANTICS.md` ch. 5.8b rule 4), on a check that neither navigates
nor carries a whole-period qualifier. The witness of that flank stood on a rule
and was rewritten to the guard of a fact creation, which binds two objects and
keeps the flank `only-some-objects`; the two fences on the check and the
required host got witnesses of their own. Measured 2026-10-27: the verifier of
`af5fd045`, before the card, refused each of the four documents in the arm of
its row, the three broken from the golden with `WF-109.only-some-objects` at
each of the golden's checks besides; after it each reports the arm of its row
alone, and the old witness's document (a rule on `Vondst` hosted on
`Strandvondst`) is clean.

| directory | rule | card | stamp | arm | reason |
| --- | --- | --- | --- | --- | --- |
| `wf109-host-partial-population` | WF-109 | D4 | herschreven | `WF-109.only-some-objects`, `host=Seinmast` | broken from `jutterswet-seinhijsing`: the guard's host `Seinmast`, a strict subtype of the target `Sein`; until 2026-10-27 a rule's host `Strandvondst` below its subject `Vondst`, which is now a narrowing host |
| `wf109-narrowing-host-navigated` | WF-109 | D5 | toegevoegd | `WF-109.only-some-objects`, `host=Wrakrecht` | broken from `jutterswet-ploegblokkade`: a navigated check ("zijn logboek vol is") with the host `Wrakrecht` below its subject `Strandrecht` |
| `wf109-narrowing-host-period` | WF-109 | D5 | toegevoegd | `WF-109.only-some-objects`, `host=Wrakrecht` | broken from `jutterswet-ploegblokkade`: a check under the whole-period qualifier with the host `Wrakrecht` below its subject `Strandrecht` |
| `wf034-characteristic-of-subtype-without-host` | WF-034 | D8 | toegevoegd | `WF-034.characteristic-undeclared`, `characteristic=vaartijd`, `objectType=Strandrecht` | broken from `jutterswet-ploegblokkade`: the check of `vaartijd`, declared on `Wrakrecht`, without its host |
