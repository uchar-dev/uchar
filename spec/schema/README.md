# IR JSON schema

This zone holds the machine-checkable schema for the uchar IR wire format, plus
the gate that enforces it. It is a gated artifact zone: it must carry its full
meaning without reference to any external repository or machine (see
`../../scripts/check-self-contained.sh`).

## The `uchar-ir-v1` format

`uchar-ir-v1` is the IR wire format. It began as `regelspraak-ir-v0`, the
mirror cut: the faithful, engine-neutral JSON dump of the reference frontend's
fully resolved model, with the original (Dutch) field names preserved verbatim.
The move to v1 is one break carried out in steps (`../proposals/V1-WIREMIGRATIE.md`
§7, steps M1–M8); each deviation from the mirror is a tracked delta
(`../CONSTRUCTS.md`), never a silent rewrite. Rationale and method:
`../IR-SPEC.md`.

The schema carries the **L3 shape**: the container is restructured (L1), the
reference layer is reduced to its symbolic form (L2), and the type system is in
its v1 nodes (L3); nothing else.

Shape, in brief:

- The root is `{ "format": "uchar-ir-v1", "model": {…}, "rules": {…},
  "diagnostics": […] }`.
- `model` is the declaration set (IR-SPEC ch. 2) as lists in declaration
  order: `objectTypes`, `factTypes`, `parameters`, `domains`, `unitSystems`,
  `dimensions`, `timelines`, `dayKinds`, then the optional extension list
  `functionDefs`. A list entry carries its name on its node; there is no map
  key, so each name is written once.
- `rules` carries the resolved rule lists under their v0 keys and in v0 order
  (`resolvedRegels`, `resolvedDagsoorten`, `objectCreaties`, `verdelingen`,
  `consistenties`, `uniciteiten`, `uniciteitUnies`, `feitCreaties`,
  `resolvedStartpunten`, `regelGroepen`), then the optional extension list
  `prioritizedDefaults`. The v0 `regels` stubs are gone.
- `diagnostics` is the producer's own account of what stayed unresolved in a
  document it did write (WF-113); `severity` is `WARNING` only.
- An `ObjectType` (`name`, `supertype`?, `animate`, `plural`?, `attributes`,
  `characteristics`) carries its members as `Attribute` nodes (`name`,
  `datatype`, `plural`?, `dimensions`?, `timeline`?) and `Characteristic` nodes
  (`name`, `kind`, `timeline`?), each with its own `TimelineFacet`
  (`granularity`, `count`, and on an attribute `timelineName`?). The eight v0
  side-maps keyed by member name are gone, and with them the possibility of a
  facet without a member.
- A `Domain` is `{ "node": "Domain", "name", "datatype" }` (next section).
- The reference layer is symbolic (L2). The five reference classes
  (`AttribuutRef`, `ParameterRef`, `DimensieCel`, `BinderRef`, `VariabeleRef`)
  carry no `type`: the declaration is the one source of a reference's type. An
  `AttribuutRef` navigates by its `navigatieBindingen` alone, and without them
  it is the subject's own read. The role check is one `RoleMembership` node
  (`factType`, `role`, `counterpartType`?, `negated`, `bron`; delta B9 of
  `../proposals/V1-WIREMIGRATIE.md`, `../CONSTRUCTS.md` row 5).
  `counterpartType` names the type the other role's filler must belong to: a
  declared object type that is a subtype of, or equal to, the declared filler
  type of the other role (WF-034, IR-SPEC ch. 4.6 (o)); absent, that declared
  filler type is meant. The migration and the emitter write the field only
  where it narrows (the L2 card,
  `../../backlog/done/20260923-l2-referentielaag.md`, U12). A fact creation
  carries its symbolic fields alone, a scoped uniqueness its `scopeRol`, a
  navigated characteristic check its hop as `navigatieBinding`.
  An unresolved expression's `type` is optional (absent: unknown), and the
  datatype union has no member for an unknown type (B16). The written ranges
  have their own section below.
- The type system is the v1 one (L3): `Number`, `Date`, `Unit`,
  `UnitSystem`, `Parameter` and `DayKind`, in their own section below.
- Every other node — `Feittype`, `Dimensie`, `Tijdlijn`, the datatypes L3
  leaves alone (`Tekst`, `Booleaan`, `Enumeratie`, `SumType`, `ListType`,
  `StructType`), the expression and condition families, every `Resolved*`
  node — keeps its v0 name, field spellings and enum values until its own
  landing, less the keys L2 removed; `RoleMembership` is the one node L2
  renames (it restructures two). So do the enum values inside the new nodes
  (`DAG` … `JAAR`, `BIJVOEGLIJK`/`BEZITTELIJK`/`ZELFSTANDIG`, and a
  `Number`'s `sign`).
- One field of four model-list declarations changed at M1: the name of a
  parameter, of a unit system, of a `Feittype` and of a `Dimensie` is a
  non-null string (on the first two it is spelled `name` since L3). With the
  map key gone it is the declaration's only name (delta B2), so a `null` there
  would be a declaration without a name; the schema no longer admits it, and
  no golden, red or negatief document carried one (measured 2026-09-23). `Tijdlijn`
  and `Rol` keep their nullable `naam`: timelines were a list in v0 already,
  and a role is not a model-list entry.
- Every node object carries a `"node"` discriminator holding its subtype name.
  The `Datatype`, `Expr` and `Conditie` families are modelled as discriminated
  unions on that field.

Canonical form. **Key order is the `properties` order of the object's schema
definition.** The migration scripts write the M1 containers and nodes (the
root, `model`, `rules`, `ObjectType`, `Attribute`, `Characteristic`,
`TimelineFacet`, `Domain`), `RoleMembership` and the six L3 nodes (`Number`,
`Date`, `Unit`, `UnitSystem`, `Parameter`, `DayKind`) in exactly that order, and
every emitter must, so that gate 2's round-trip and the byte-identical
re-emission gates (9, 10, 11, 14) prove one and the same form. A node carried
over from v0 keeps the order its producer wrote, less the keys L2 removed; a
binding L2 derives is appended after the node's last key, where every
producer already writes it. Measured
2026-09-24 on the L3 shape, 23 of the 10802 node objects in `corpus/golden` and
`corpus/rood` deviate from their definition's `properties` order (kinds
`Default`, `PrioritizedDefaults`, `Functie`, `Kwantorvouw`, `Let`; none of the
six L3 nodes). Gate 2 does not check
key order: its round-trip preserves the order of the document it reads.
Beyond key order: two-space indentation, empty containers inline, numbers in
plain (non-exponential) form, non-ASCII and control characters escaped as
`\uXXXX`. This makes a document byte-stable for a given input.

## The declaration position of a domain

In v0 a domain was a map entry whose value was a datatype, and three datatypes
repeated the map key inside themselves: `Enumeratie.domein`, `SumType.domain`
and `StructType.domain` (a number domain carried none). In M1 the `Domain`
wrapper carries the name, and the datatype at that declaration position
carries no self-name — the principle of delta B2: nowhere on the wire does one
name have a second spelling. At every reference position (an attribute's, a
parameter's or an expression's datatype, a payload, a field, a list element)
the same field stays, and stays required: there it is the domain reference of
IR-SPEC ch. 2.4 (WF-003), not a self-name.

Mechanism: `Domain.datatype` refers to a separate union, `domainDatatype`. It
admits the same datatype nodes as `datatype`, except that `Enumeratie`,
`SumType` and `StructType` appear as `DomainEnumeratie`, `DomainSumType` and
`DomainStructType`: closed definitions with the same `node` value and without
the self-name property, whose remaining field is a `$ref` by JSON pointer into
the reference form (`#/$defs/Enumeratie/properties/waarden`,
`#/$defs/SumType/properties/constructors`,
`#/$defs/StructType/properties/fields`). This was chosen over the alternative
(making the self-name optional in the three definitions and requiring or
forbidding it per position through conditional subschemas) for three reasons:
the reference-position definitions stay byte-for-byte v0 and keep their
`required` list; `additionalProperties: false` makes a self-name at the
declaration position unwritable rather than merely optional; and each field
schema is still defined once, so the two forms cannot diverge.

## A characteristic's timeline facet

v0 recorded a timeline name for an attribute (`attribuutTijdlijnNaam`) and
none for a characteristic: there was no such side-map. So in M1 an
attribute's `TimelineFacet` may carry `timelineName` and a characteristic's
may not. Admitting the field on a characteristic would open a wire position
that no v0 document fills, no producer writes and no reader checks — WF-005
resolves a named timeline only for attributes — so a name written there would
be accepted and dropped. Measured 2026-09-23 before this closure: a
characteristic facet naming an undeclared timeline, or naming one whose grid
contradicts the facet's own, validated against the schema and passed the
verifier.

Mechanism: `Characteristic.timeline` refers to `CharacteristicTimelineFacet`,
a closed definition with the same `node` value (`TimelineFacet`) and without
`timelineName`, whose `granularity` and `count` are `$ref`s by JSON pointer
into `TimelineFacet`. It follows the domain precedent above rather than an
`allOf` with a `not: {required: ["timelineName"]}` clause, for the same
reasons: `additionalProperties: false` makes the field unwritable instead of
conditionally forbidden, the attribute's facet definition stays as it is, and
each field schema is defined once. The shared loader
(`../verifier/lib/loader.ml`) refuses the field on a characteristic as a load
failure that names its path.

## The written ranges (L2)

A range is written, never derived (delta B12 of
`../proposals/V1-WIREMIGRATIE.md`; `../CONSTRUCTS.md` row 4): the relation an
instance fold, a quantifier fold, an allocation or a union selection reads is a
`RolBinding` on the node, and a reader that finds none has no type-pair
derivation to fall back on. The schema states the obligation where it can:

- `ResolvedVerdeling.verbindendeRol` and `InstanceSetBranch.sourceRole` are
  required and non-null: both nodes read a relation, always.
- `Kwantorvouw` carries exactly one of `bronRol` (the one-step range) and
  `bronPad` (the multi-step range): a quantifier fold ranges over linked
  instances, always.
- `Functie` carries exactly one of the two when it has an `elementObjecttype`,
  exactly one argument and a kind other than `TOTAAL` (an `if`/`then` with a
  `oneOf` over `required`), and never both, under any kind.

The `Functie` condition is narrower than "has an `elementObjecttype`" on two
sides, each for a measured reason (2026-09-23). A bindingless `TOTAAL` is the
subject's own read over its own attribute, the timeline total (IR-SPEC ch. 4.3,
reading (iv)): fifteen folds in the golden and red zones carry it, they read no
relation, and the verifier holds that such a node really is the own read
(WF-083) — which depends on the declarations, so the schema cannot state it. A
fold with two or more arguments is the listed value fold, which reads its
operands and not a relation; the RegelSpraak frontend stamps an
`elementObjecttype` on some of them anyway (five folds in two golden documents,
two in the parity zone), so a condition on the element type alone would demand
a range such a fold does not have. The single-argument scope is IR-SPEC
ch. 4.3's own (the `sourceRole` row): a range belongs to a fold with one
argument that reads linked instances. The precedent for a conditional
obligation is the `drempel` of `Kwantorvouw` (WF-108 (e)).

## The type system (L3)

L3 moves the datatypes and the units to their v1 nodes and changes no meaning
(deltas B14, B15, B17 (i) and B18 of `../proposals/V1-WIREMIGRATIE.md`;
`../CONSTRUCTS.md` rows 26 and 27; the L3 card,
`../../backlog/done/20260924-l3-typesysteem.md`):

- `Number` (`whole`, `decimals`, `sign`, `unit`?, `presentation`?). `sign` is
  always written, in its v0 enum spelling until its own landing; a number
  declared without a sign is `ONBEPAALD`. A percentage is a `Number` whose
  unit is `%`, or a rate with `%` as its numerator (B14). `presentation:
  "percentage"` is its presentation tag, written only at a declaration
  position: the datatype of an attribute, a parameter or a domain and every
  datatype nested in one (a payload, a list element, a struct field), a
  function or lambda parameter, a function result. It is never written on a
  type annotation. The schema admits it on any `Number`, because one datatype
  union serves both kinds of position; the verifier refuses it on an
  annotation (WF-007). The tag has no meaning for evaluation.
- `Date` (`precision`: `day` | `millisecond`, B15). The precision is always
  written: the two share no order (WF-036).
- `Unit` (`abbreviation`, `system`?, `canonical`?, `factorToCanonical`?,
  `denominator`?). No field is ever null. The factor is the exact rational
  `{"num": n, "den": d}`, two integers of at least 1, without a node
  discriminator (B17 (i)); that it is in lowest terms is WF-092's, because a
  schema cannot state a greatest common divisor.
- A unit's further spellings (`spellings`, at least one when present) belong
  to its declaration. `UnitSystem.units` holds `DeclaredUnit`: the node
  `Unit` with that one field after the others, its other field schemas taken
  from `Unit` by JSON pointer. Every other position holds `Unit`, which has no
  such field. This follows the precedent of the characteristic's facet below:
  a spelling written on an occurrence would otherwise be accepted and dropped.
- `UnitSystem` (`name`, `units`): each unit once, in declaration order, with
  its spellings on the unit itself.
- `Parameter` (`name`, `datatype`, `timeline`?). Its facet is
  `ParameterTimelineFacet`, the attribute's `TimelineFacet` without
  `timelineName`, because no parameter records a timeline name (B18). The
  `datatype` is required and non-null (IR-SPEC ch. 2.10).
- `DayKind` (`name`, `plural`?): `model.dayKinds` is a list of these nodes. A
  day kind's name and plural are every spelling it has on the wire; there is
  no registry of spellings beside the day kinds (IR-SPEC ch. 2.9).

## `uchar-ir-v1.schema.json`

JSON Schema (draft 2020-12) for the format. It is strict: every object sets
`additionalProperties: false` and lists every always-emitted key as `required`;
enum-valued fields are constrained to their exact constant sets; fields are
nullable only where the format actually emits `null`. `$id`:
`https://uchar.dev/schema/uchar-ir-v1.schema.json`. It replaces the
`regelspraak-ir-v0` schema; the two do not exist side by side.

Three admissions are deliberate and leave the refusal to the verifier:
`Attribute.datatype` admits `null`, as the v0 attribute map did, so a
datatype-less attribute stays a loadable document that the verifier refuses
(WF-059); `TimelineFacet.count` admits any integer, because `count ≥ 1` is
WF-005, a verifier check with its own negatief witness; and `Diagnostic.line`
admits any integer, because `line ≥ 0` (0 = the producer reports no source
position: a hand-built document, or a frontend warning without a token) is
WF-113 (a), likewise with its own witness.

**Extension fields** (the decided wire-entry discipline, `../IR-SPEC.md`
§ Method): a v1 construct enters the wire additively, as an *optional*
property — the `RolBinding`-carrying fields, the `TemporalRead` expression
node (the row-11 point read), and the `prioritizedDefaults` list under
`rules` (the Phase-4 defaults groups). When present they are written whole,
never null; the loaders admit exactly these keys and nothing else. Since L2
part of that set is required, because the fields it superseded left the wire:
`verbindendeRol` on `ResolvedVerdeling`, the symbolic fact-creation fields
(`feittype`/`ankerRol`/`doelRol`/`joinRol`, `Tussenstap.rolBinding`), and the
ranges of the section above. The rest stays optional where its absence has a
meaning of its own: no `navigatieBindingen` is the subject's own read, no
`scopeRol` an unscoped uniqueness, no `navigatieBinding` a check on the subject
itself. The non-additive cleanup (renames; dropping the superseded fields) is
the v1 migration itself, landed step by step.

**Migration.** `../proposals/v1-migratie/m1.py` converts a `regelspraak-ir-v0`
document to the M1 shape (file arguments, or `--all` for every `ir.json` in the
corpus zones and the parity zone; `--check` lists the documents still in v0 and
exits 1 if there is one). It writes with gate 2's own serialiser, so a migrated
document round-trips byte-identically, and it refuses — with the file name and a
JSON pointer into the v0 input — every v0 value the M1 shape cannot carry: a
null or disagreeing self-name, a null datatype, a side-map key naming no member,
a facet entry without a granularity key, an empty dimension list, a value
outside its enum. A document already in `uchar-ir-v1` is left untouched, so a
second run changes nothing.

`../proposals/v1-migratie/m2.py` converts an L1 document to the L2 shape, with
the same interface and the same serialiser. It removes what L2 takes off the
wire and writes out every binding the L2 shape requires and the document
lacks, with the resolution the verifier performed until L2
(`../verifier/lib/checks.ml` at main 2a701b23: the range from the type pair,
the navigation walk, the scope and the characteristic hop by role name, the two
role checks; L2 removed that resolution from the verifier) and the subject type
taken from the same context. The script's header lists, with measured document
counts, where it derives more than that resolution did. Zero or several
candidates refuse the document with the file name and a JSON pointer into the
L1 input, and so does every L1 value the L2 shape cannot carry. One line on
stderr per migrated document counts what was derived and removed; a document
already in the L2 shape is left untouched.

`../proposals/v1-migratie/m3.py` converts an L2 document to the L3 shape, with
the same interface and the same serialiser. It rewrites the type system
mechanically: the tag only where a percentage stood at a declaration, an
absent unit of a percentage as the built-in `%`, a factor written as a whole
number verbatim over 1 and any other factor as the rational with the smallest
denominator (at most 10^6) whose double is exactly the factor's, a unit
system's map keys and aliases as the spellings of their unit, a day kind's
registered spellings as its plural. It refuses, with the file name and a JSON
pointer into the L2 input, every L2 value the L3 shape cannot carry: a factor
that is not positive or has no such rational, a third spelling of a day kind,
a registry entry that names no declared day kind, a null in a unit system's
maps, an alias of a unit the system does not declare, a percentage whose unit
is not `%`. One line on stderr per migrated document counts what was
rewritten; a document already in the L3 shape is left untouched.

## Gate: `../../scripts/check-ir-schema.sh`

The repo's second CI gate. It performs three checks, and two self-tests:

1. **Metaschema** — the schema is itself a valid draft 2020-12 schema.
   - **Self-tests** — what the schema refuses by itself has no corpus witness
     (golden and rood must validate, negatief is not validated here), so it is
     held against the schema directly, in both polarities: the date patterns
     against a lexeme table, and the positions the M1, L2 and L3 shapes
     close against one small valid document per shape with one mutation per
     row. The rows that must pass pin the three admissions above, the two
     narrowings of the written ranges, a `RoleMembership` carrying
     `counterpartType`, an `Onopgelost` without `type` (the unknown type
     since L2), and in the L3 shape the presentation tag on a type annotation
     and a factor not in lowest terms (both refused by the verifier), a
     percentage without the tag, a declared unit without spellings, the bare
     unit, and a parameter without a facet and a day kind without a plural.
2. **Validation** — every `corpus/golden/**/ir.json` validates against the
   schema. The file count is reported; an empty corpus passes and says so.
3. **Round-trip** — each such file is parsed (preserving the original number
   lexemes) and re-emitted canonically; the result must be byte-identical to the
   input. This is what ties the schema-blessed form to the emitter's form: a
   drift in either surfaces as a byte diff.

The gate needs a `python3` with the `jsonschema` package (it falls back to
running it in an isolated environment via `uv` if one is available). It also
accepts explicit file paths as arguments, to check a dump that lives outside the
corpus.

Corpus IR files land under `corpus/golden/**/ir.json` and are picked up
automatically once they exist.

## The case schema (`uchar-case-v0.schema.json`)

A golden case's *data* half: `case*.json` beside the `ir.json` carries the
input world (labelled instances with attributes, label-based role links,
characteristics asserted true), the parameter values, the single
evaluation date (`asOfDate`, null for a time-free case), and the expected
outcomes — an attribute's value, an attribute explicitly empty, a
characteristic present or absent, or the consistency verdict. The
reference interpreter (`ref/`) consumes these files as its test suite;
gate 4 (`scripts/check-case-schema.sh`) validates them.

Conventions, deliberate:

- **Numbers are exact lexemes** (decimal-with-dot or `a/b` rational),
  carried as strings — never JSON floats. The comparison contract is
  exact numeric equality, scale-insensitive.
- **Values are unit-normalised** to the referenced declaration's unit
  before they enter the file; a case file carries no unit spellings.
- **Absence means empty** on input; only an expectation can *assert*
  emptiness (`attributeEmpty`). Characteristics start false; input can
  only assert them true.
- One instance per label; role links point at labels; a to-one role
  carries exactly one label.
- A temporal attribute or granular parameter may carry the **timeline
  variant**: day-precision segments (from inclusive, to exclusive),
  ordered, non-overlapping, aligned to the declared granularity grid;
  absence between segments is a gap. The scalar convention (the
  asOfDate-bucket value serving the temporal total) coexists per
  attribute — an attribute carries one convention or the other, and the
  executors fault when a construct needs the one the case did not
  supply.

Two deliberate narrowings relative to the donor conformance layer this
format was mined from: a case has **no rule scope** — evaluation is
whole-model (in the donor, scope changes nothing about what fires; it
only steers coverage bookkeeping) — and the evaluation date is a whole
**date**, never a datetime.

Division of labour: JSON Schema (gate 4) checks structure only.
Cross-item invariants — label uniqueness, role targets naming existing
labels, names resolving against the sibling `ir.json`, calendar
validity of dates beyond the lexical pattern — are the **`ref/` case
loader's** checks, fail-fast, like the verifier's for the IR itself.

The expectation values are pinned against the normative worked examples
of `spec/RUNTIME-SEMANTICS.md`; machine replay of the cases is the
reference interpreter's oracle guard and lands with `ref/`.
