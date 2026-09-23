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

The schema carries the **M1 shape**: the container is restructured and nothing
else.

Shape, in brief:

- The root is `{ "format": "uchar-ir-v1", "model": {…}, "rules": {…},
  "diagnostics": […] }`.
- `model` is the declaration set (IR-SPEC ch. 2) as lists in declaration
  order: `objectTypes`, `factTypes`, `parameters`, `domains`, `unitSystems`,
  `dimensions`, `timelines`, `dayKinds`, then `dagsoortPerEenheid` (a map,
  unchanged from v0) and the optional extension list `functionDefs`. A list
  entry carries its name on its node; there is no map key, so each name is
  written once.
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
- Every other node — `Parameter`, `Feittype`, `Eenheidstelsel`, `Dimensie`,
  `Tijdlijn`, the datatype, expression and condition families, every
  `Resolved*` node — keeps its v0 name, field spellings and enum values until
  its own M step. So do the enum values inside the new nodes (`DAG` … `JAAR`,
  `BIJVOEGLIJK`/`BEZITTELIJK`/`ZELFSTANDIG`).
- One field of four of those nodes does change at M1: `naam` on `Parameter`,
  `Eenheidstelsel`, `Feittype` and `Dimensie` is a non-null string. With the
  map key gone it is the declaration's only name (delta B2), so a `null` there
  would be a declaration without a name; the schema no longer admits it, and
  no golden, red or negatief document carried one (measured 2026-09-23). `Tijdlijn`
  and `Rol` keep their nullable `naam`: timelines were a list in v0 already,
  and a role is not a model-list entry.
- Every node object carries a `"node"` discriminator holding its subtype name.
  The `Datatype`, `Expr` and `Conditie` families are modelled as discriminated
  unions on that field.

Canonical form. **Key order is the `properties` order of the object's schema
definition.** The migration script writes the M1 containers and nodes (the
root, `model`, `rules`, `ObjectType`, `Attribute`, `Characteristic`,
`TimelineFacet`, `Domain`) in exactly that order, and every emitter must, so
that gate 2's round-trip and the byte-identical re-emission gates (9, 10, 11,
14) prove one and the same form. A node carried over unchanged from v0 keeps
the order its producer wrote, byte for byte until its own M step; measured
2026-09-23, 23 of the 12748 node objects in `corpus/golden` and `corpus/rood`
deviate from their definition's `properties` order (kinds `Default`,
`PrioritizedDefaults`, `Functie`, `Kwantorvouw`, `Let`). Gate 2 does not check
key order: its round-trip preserves the order of the document it reads.
Beyond key order: two-space indentation, empty containers inline, numbers in
plain (non-exponential) form, non-ASCII and control characters escaped as
`\uXXXX`. This makes a document byte-stable for a given input.

## The declaration position of a domain

In v0 a domain was a map entry whose value was a datatype, and three datatypes
repeated the map key inside themselves: `Enumeratie.domein`, `SumType.domain`
and `StructType.domain` (a `Numeriek` domain carried none). In M1 the `Domain`
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

## `uchar-ir-v1.schema.json`

JSON Schema (draft 2020-12) for the format. It is strict: every object sets
`additionalProperties: false` and lists every always-emitted key as `required`;
enum-valued fields are constrained to their exact constant sets; fields are
nullable only where the format actually emits `null`. `$id`:
`https://uchar.dev/schema/uchar-ir-v1.schema.json`. It replaces the
`regelspraak-ir-v0` schema; the two do not exist side by side.

Two admissions are deliberate and leave the refusal to the verifier:
`Attribute.datatype` admits `null`, as the v0 attribute map did, so a
datatype-less attribute stays a loadable document that the verifier refuses
(WF-059); and `TimelineFacet.count` admits any integer, because `count ≥ 1` is
WF-005, a verifier check with its own negatief witness.

**Extension fields** (the decided wire-entry discipline, `../IR-SPEC.md`
§ Method): a v1 construct enters the wire additively, as an *optional*
property the v0 emitter never produces — currently `verbindendeRol` on
`ResolvedVerdeling`, `bronRol` on `Functie`, and the symbolic
fact-creation fields (`feittype`/`ankerRol`/`doelRol`/`joinRol`,
`Tussenstap.rolBinding`), all carrying the `RolBinding` node, the
`TemporalRead` expression node (the row-11 point read), and the
`prioritizedDefaults` list under `rules` (the Phase-4 defaults groups).
When present they are written whole, never null; the loaders admit
exactly these keys and nothing else. Mirror purity holds per file: an emitter
dump carries none of them, a hand-written anchor case may carry them,
and the same schema validates both. The non-additive cleanup (renames;
dropping the superseded fields) is the v1 migration itself, landed step by
step.

**Migration.** `../proposals/v1-migratie/m1.py` converts a `regelspraak-ir-v0`
document to the M1 shape (file arguments, or `--all` for every `ir.json` in
the corpus zones and the parity zone; `--check` lists the documents still in
v0 and exits 1 if there is one). It writes with gate 2's own serialiser, so
a migrated document round-trips byte-identically, and it refuses — with the
file name and a JSON pointer into the v0 input — every v0 value the M1 shape
cannot carry: a null or disagreeing self-name, a null datatype, a side-map key
naming no member, a facet entry without a granularity key, a value outside its
enum. A document already in `uchar-ir-v1` is left untouched, so a second run
changes nothing.

## Gate: `../../scripts/check-ir-schema.sh`

The repo's second CI gate. It performs three checks:

1. **Metaschema** — the schema is itself a valid draft 2020-12 schema.
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
