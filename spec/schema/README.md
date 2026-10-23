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

The schema carries the **L5 shape**: the container is restructured (L1), the
reference layer is reduced to its symbolic form (L2), the type system is in its
v1 nodes (L3), the rule layer and the expression layer are in their v1 forms
(L4), and every source text is in the source record and every node, key and
enum value carries its IR-SPEC name (L5). The sections per landing below name
the wire of their own landing; since L5 the names are those of the L5 section.

Shape, in brief:

- The root is `{ "format": "uchar-ir-v1", "model": {…}, "rules": {…},
  "diagnostics": […] }`.
- `model` is the declaration set (IR-SPEC ch. 2) as lists in declaration
  order: `objectTypes`, `factTypes`, `parameters`, `domains`, `unitSystems`,
  `dimensions`, `timelines`, `dayKinds`, then the optional extension list
  `functionDefs`. A list entry carries its name on its node; there is no map
  key, so each name is written once.
- `rules` carries one list per rule-form kind, in the order of IR-SPEC ch. 2
  (`rules`, `dayKindDefinitions`, `objectCreations`, `allocations`,
  `consistencies`, `uniquenesses`, `factCreations`, `startPointDeterminations`,
  `ruleGroups`), then the optional extension list `prioritizedDefaults`. The
  v0 `regels` stubs are gone, and since L4 so is `uniciteitUnies`: the
  single-source and union uniqueness forms share `uniquenesses`. The lists
  carried their v0 keys until L5.
- `diagnostics` is the producer's own account of what stayed unresolved in a
  document it did write (WF-113); `severity` is `warning` only.
- An `ObjectType` (`name`, `supertype`?, `animate`, `plural`?, `attributes`,
  `characteristics`) carries its members as `Attribute` nodes (`name`,
  `datatype`, `plural`?, `dimensions`?, `timeline`?) and `Characteristic` nodes
  (`name`, `kind`, `timeline`?), each with its own `TimelineFacet`
  (`granularity`, `count`, `fixedStart`?, and on an attribute `timelineName`?;
  `fixedStart` is the `Timeline`'s own field, IR-SPEC ch. 2.8). The eight v0
  side-maps keyed by member name are gone, and with them the possibility of a
  facet without a member.
- A `Domain` is `{ "node": "Domain", "name", "datatype" }` (next section).
- The reference layer is symbolic (L2). The five reference classes
  (`AttributeReference`, `ParameterReference`, `DimensionCellRead`,
  `BinderRef`, `VariableReference`) carry no `type`: the declaration is the one
  source of a reference's type. An `AttributeReference` navigates by its
  `path` alone, and without it it is the subject's own read. The role check is
  one `RoleMembership` node (`factType`, `role`, `counterpartType`?, `negated`,
  `source`; delta B9 of
  `../proposals/V1-WIREMIGRATIE.md`, `../CONSTRUCTS.md` row 5).
  `counterpartType` names the type the other role's filler must belong to: a
  declared object type that is a subtype of, or equal to, the declared filler
  type of the other role (WF-034, IR-SPEC ch. 4.6 (o)); absent, that declared
  filler type is meant. The migration and the emitter write the field only
  where it narrows (the L2 card,
  `../../backlog/done/20260923-l2-referentielaag.md`, U12). A fact creation
  carries its symbolic fields alone, a scoped uniqueness its scope binding
  (`scope` since L4), a navigated characteristic check its hop as
  `navigation`.
  An unresolved expression's `type` is optional (absent: unknown), and the
  datatype union has no member for an unknown type (B16). The written ranges
  have their own section below.
- The type system is the v1 one (L3): `Number`, `Date`, `Unit`,
  `UnitSystem`, `Parameter` and `DayKind`, in their own section below.
- The rule layer and the expression layer are the v1 ones (L4): the six named
  forms with their versions, `Aggregation` and `BuiltinCall`, the typed
  `Literal` value, `ContextValue` and `BoundDay`, in their own section below.
- Every source text is in the source record `source` (L5), and every node,
  key and enum value carries its IR-SPEC name, the enum values English
  lowerCamel (IR-SPEC, Conventions), with one exception: the rule-status check
  keeps its v0 node `Regelversie` (`regel`, `inconsistent`) until its v1 form
  lands. On a renamed node a field IR-SPEC names optional is absent where v0
  wrote `null`. Its own section below.
- One field of four model-list declarations changed at M1: the name of a
  parameter, of a unit system, of a `FactType` and of a `Dimension` is a
  non-null string (spelled `name` since L3 on the first two and since L5 on the
  last two). With the map key gone it is the declaration's only name (delta
  B2), so a `null` there would be a declaration without a name; the schema no
  longer admits it, and no golden, red or negative document carried one
  (measured 2026-09-23). `Timeline` and `Role` keep their nullable `name`:
  timelines were a list in v0 already, and a role is not a model-list entry.
- Every node object carries a `"node"` discriminator holding its subtype name.
  The `datatype`, `expr` and `condition` families are modelled as
  discriminated unions on that field.

Canonical form. **Key order is the `properties` order of the object's schema
definition.** The migration scripts write the M1 containers and nodes (the
root, `model`, `rules`, `ObjectType`, `Attribute`, `Characteristic`,
`TimelineFacet`, `Domain`), `RoleMembership`, the six L3 nodes (`Number`,
`Date`, `Unit`, `UnitSystem`, `Parameter`, `DayKind`) and the L4 nodes and
version records (the six named forms and their versions, `Source`,
`Aggregation`, `BuiltinCall`, `Literal`, `ContextValue`, `BoundDay`) in exactly
that order, and every emitter must, so that gate 2's round-trip and the
byte-identical re-emission gates (9, 10, 11, 14) prove one and the same form. A
node carried over from v0 keeps the order its producer wrote, less the keys L2
removed; a binding L2 derives is appended after the node's last key, where
every producer already writes it. L5 renames in place: a renamed node keeps its
L4 order, and the `properties` order of its definition is that order.
Measured 2026-09-24 on the L4 shape, 20 of the 11509 node objects and version
records in `corpus/golden` and `corpus/red` deviate from their definition's
`properties` order (kinds `Default`, `PrioritizedDefaults`, `QuantifierFold` (then `Kwantorvouw`),
`Let`; none of the L3 or L4 nodes and no version record; on the L3 shape the
three function nodes among the 23 deviating objects were the only ones of a
node L4 rewrites); measured again on the L5 shape, the same 20 objects deviate,
the quantifier fold now under its name `QuantifierFold`. On 2026-09-25
`PrioritizedDefaults` took `unordered` after `defaults`, the order of both
emitters that write the key (the RegelSpraak frontend, pinned by gate 10 on
`jutterswet-wachtgeld`, and the Catala plugin); the Catala plugin took this
order for a `Default`'s `exceptions` (before `source`, where the RegelSpraak
frontend writes it); and the hand-written objects that still deviated were put
in this order. Measured that day, none of the 12091 node objects and version
records of `corpus/golden` and `corpus/red` deviates, nor any of the 11592 of
`corpus/negative`. Gate 2 does not check key order: its round-trip preserves
the order of the document it reads.
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
admits the same datatype nodes as `datatype`, except that `Enumeration`,
`SumType` and `StructType` appear as `DomainEnumeration`, `DomainSumType` and
`DomainStructType`: closed definitions with the same `node` value and without
the self-name property `domain` (`domein` on `Enumeratie` until L5), whose
remaining field is a `$ref` by JSON pointer into the reference form
(`#/$defs/Enumeration/properties/values`,
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
`RoleBinding` on the node, and a reader that finds none has no type-pair
derivation to fall back on. The schema states the obligation where it can:

- `AllocationVersion.connectingRole` (`ResolvedVerdeling.verbindendeRol`
  until L4) and `InstanceSetBranch.sourceRole` are required and non-null: both
  read a relation, always.
- `QuantifierFold` carries exactly one of `sourceRole` (the one-step range)
  and `sourcePath` (the multi-step range; the v0 `Kwantorvouw` with `bronRol`
  and `bronPad` until L5): a quantifier fold ranges over linked instances,
  always.
- `Aggregation` (the aggregation kinds of the v0 `Functie`, since L4) carries
  exactly one of `sourceRole` and `sourcePath` when it has an `elementType`,
  exactly one argument and a kind other than `total` (an `if`/`then` with a
  `oneOf` over `required`), and never both, under any kind. A `BuiltinCall`
  carries neither field, nor an `elementType`.

The `Aggregation` condition is narrower than "has an `elementType`" on two
sides, each for a measured reason (2026-09-23, on the v0 names `Functie`,
`elementObjecttype`, `bronRol` and `bronPad`). A bindingless `total` is the
subject's own read over its own attribute, the timeline total (IR-SPEC ch. 4.3,
reading (iv)): fifteen folds in the golden and red zones carry it, they read no
relation, and the verifier holds that such a node really is the own read
(WF-083) — which depends on the declarations, so the schema cannot state it. A
fold with two or more arguments is the listed value fold, which reads its
operands and not a relation; the RegelSpraak frontend stamps an
element type on some of them anyway (five folds in two golden documents,
two in the parity zone), so a condition on the element type alone would demand
a range such a fold does not have. The single-argument scope is IR-SPEC
ch. 4.3's own (the `sourceRole` row): a range belongs to a fold with one
argument that reads linked instances. The precedent for a conditional
obligation is the `threshold` of `QuantifierFold` (WF-108 (e)).

## The type system (L3)

L3 moves the datatypes and the units to their v1 nodes and changes no meaning
(deltas B14, B15, B17 (i) and B18 of `../proposals/V1-WIREMIGRATIE.md`;
`../CONSTRUCTS.md` rows 26 and 27; the L3 card,
`../../backlog/done/20260924-l3-typesysteem.md`):

- `Number` (`whole`, `decimals`, `sign`, `unit`?, `presentation`?). `sign` is
  always written; a number declared without a sign is `unspecified`
  (`ONBEPAALD` until L5). A percentage is a `Number` whose
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
  schema cannot state a greatest common divisor. Both integers are also at
  most 4611686018427387903 (2^62 − 1): every loader reads them within that
  range and refuses a larger one in its factor sentence, so the schema states
  the same bound instead of admitting a document no reader loads.
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

## The rule and expression layer (L4)

L4 moves the rule layer and the expression layer to their v1 forms and changes
no meaning (deltas B19–B23 and B27 of `../proposals/V1-WIREMIGRATIE.md`; the L4
card, `../../backlog/done/20260924-l4-regel-en-expressielaag.md`):

- **The six named forms** (IR-SPEC ch. 3, Versioning): `Rule`, `Allocation`,
  `ObjectCreation`, `FactCreation`, `Consistency` and `Uniqueness` are each
  `{node, name, versions}`, with at least one version (B19). A version record
  carries no `node`: it is part of its form. `name` is nullable, because a
  missing or blank name is WF-107's with its own witnesses; a name ending in a
  space, `#` and digits cannot be written (the `formName` pattern), because
  that is how the v0 wire, and the readers in memory, spell one version of a
  form with several. `Rule` carries the extension field `statuteAnnotations`
  on the form, one list for all its versions.
- **`validity` on every version**, never null: a `Geldigheid` with both bounds
  null was always (since L5 a `Validity` with neither bound). An absent
  `condition` is unconditional.
- **`RuleVersion`** (`validity`, `target`, `value`, `condition`?,
  `initialisation`, `presentation`?). The target is an `AttribuutRef` and
  nothing else; whether it names an attribute or a characteristic is its
  declaration's (B20), so the v0 rule kind is no field: a computing version
  writes `initialisation: false` with the tag `presentation` (`GELIJKSTELLING`
  or `BEREKENING`, since L5 `equation` or `calculation`), an
  initialisation writes `initialisation: true` without a tag, and a
  characteristic write carries neither. The tag beside `initialisation: true`
  cannot be written (a `not` over both fields); whether the combination fits
  the target's member kind is WF-022's.
- **`AllocationVersion`**: the fields of IR-SPEC ch. 3.2 under their IR-SPEC
  names. `method` is one of the three real methods: the v0 placeholder of an
  unhandled form cannot be written. The four type and attribute fields are
  nullable because WF-034 holds their presence, and an empty attribute name
  stays WF-034's; an absent optional field is none.
- **`ObjectCreationVersion`** (`createdType`, `subject`, `initialisations`,
  `condition`?, `validity`, `connectingRole`?): absent, the back-link is
  derived (RUNTIME-SEMANTICS ch. 5.5). Each initialisation stayed a v0
  `Toekenning` until L5, since then a `MemberInit`.
- **`FactCreationVersion`** (`factType`, `anchorRole`, `targetRole`,
  `anchor`, `path`, `condition`?, `validity`): the target type is not written,
  because it is the declared filler of `targetRole`, and `path` is a non-empty
  list of `RolBinding`s, the last reaching the target type.
- **`ConsistencyVersion`** carried `bron` and `line`, and
  **`UniquenessVersion`** `bron`, under their v0 names until the source record
  landed (L5). A uniqueness names its `sources`, at least one, each a `Source`
  (`objectType`, `keys`, at least one key); one source is the single-type
  form, two or more the union (B27). `scope` stands only beside one source: a
  scoped union is an open design question (IR-SPEC ch. 3.6), so the schema
  closes the position.
- **`Aggregation` and `BuiltinCall`** (B21) split the v0 function node by
  kind: the eight fold kinds carry `elementType`?, `filter`?,
  `emptyFallback`?, `dimensionSelection`?, `sourceRole`? and `sourcePath`?,
  the seventeen builtins `filter`? and `granularity`? (its five buckets, in
  their v0 spelling until L5). A
  field of the one node on the other, and a kind of the other node, cannot be
  written; which kind within a node requires or admits a field is WF-037's and
  WF-056's. The written ranges of the section above hold on `Aggregation`.
- **`Literal.value`** (B22) is exactly one variant of the case format's value
  union (`uchar-case-v0.schema.json`), with its patterns: `number` (an exact
  decimal with a point, or a rational `a/b`, never a JSON number), `boolean`,
  `text`, `date`, `dateTime`, `enumeration`, `constructor` (without a
  payload). The source's spelling, a decimal comma included, stays in the
  source text (`bron` until L5, since then the snippet of `source`).
  The patterns are shape tests: a calendar day and a clock are WF-084's and
  WF-095's, and that the variant is the one the literal's `type` names is
  WF-114's, because one datatype union serves every literal. `type` stays
  nullable: a typeless value node is WF-034's. The text variant admits the
  empty string, as the case format's does.
- **`ContextValue`** (`name`, `type`, `bron`) and **`BoundDay`** (`type`,
  `bron`) split the v0 constant (B23): the evaluation date and year kept their
  v0 spellings `Rekendatum` and `Rekenjaar` until L5 (since then
  `evaluationDate` and `evaluationYear`), and the bound day of a day-kind
  definition carries no name. `ContextValue.name` is a required string and
  otherwise free: the name set is closed and crossed byte for byte by WF-098,
  which gives a name outside it a witness of its own, `de dag` included.

## The source record and the renaming (L5)

L5 moves every source text into the source record (step M7) and gives every
node, key and enum value its IR-SPEC name (step M8), and changes no meaning
(deltas B26, B6 and B10 of `../proposals/V1-WIREMIGRATIE.md`; the L5 card,
`20260924-l5-bronrecord-en-hernoeming`).

The first step of L5 moves every source text into the source record and changes
nothing else. The optional field `source` stands where the v0 field `bron`
stood, on every node that carried one, the defaults layer included, and is the
record `{ "snippet", "line"? }` (`$defs/sourceReference`), which carries no
`node` discriminator. `snippet` is the v0 string byte for byte, whitespace
included, and never empty: a node without source text carries no `source`, so a
v0 `null` is absent. The v0 field `line` of a consistency version and of a
`ResolvedRegelGroep` went into the record after the snippet, and was dropped
where `bron` was null; the migration and the emitters write `line` (at least 1)
there alone, while the schema admits it in every record. `source` is required on
both `Unresolved` variants (`Onopgelost` until the second step), because WF-113
(b) accounts for a marker by the diagnostic that quotes its snippet. `file` and
`column` are not admitted until the card that first writes positions fixes their
conventions. Where the sections above name `bron`, they describe the shape
before this step. `../proposals/v1-migratie/m5.py --stage m7` performs the step,
with the interface and the serialiser of `m4.py`, and refuses, with the file
name and a JSON pointer into the L4 input, an empty `bron` or one that is
neither a string nor null, an `Onopgelost` whose `bron` is null or absent, a
`line` that is not an integer of at least 1, that stands without a `bron` beside
it or that stands on another node than the two carriers and a `Diagnostic`, and
a document that mixes the two shapes; the two negative witnesses of WF-113 (b)
whose marker it refuses are written by hand.

The second step renames. The nine lists under `rules` take the keys of IR-SPEC
ch. 2 (`rules`, `dayKindDefinitions`, `objectCreations`, `allocations`,
`consistencies`, `uniquenesses`, `factCreations`, `startPointDeterminations`,
`ruleGroups`); `prioritizedDefaults` keeps its name. The 32 v0 nodes the earlier
landings left take their IR-SPEC names (`Text`, `Boolean`, `Enumeration`,
`FactType`, `Role`, `Dimension`, `Timeline`, `RoleBinding`, `Validity`,
`MemberInit`, `DayKindDefinition`, `StartPointDetermination`, `RuleGroup`,
`Binary`, `Rounding`, `Clamp`, `TextComposition`, `LiteralChunk`,
`EmbeddedExpression`, `AttributeReference`, `DimensionCellRead`,
`ParameterReference`, `VariableReference`, `Unresolved`, `DimensionSelection`,
`Comparison`, `Predicate`, `CharacteristicCheck`, `TimeWindow`, `Compound`,
`QuantifierFold`, `WholePeriod`), and their keys, with those of
`PrioritizedDefaults`, `Default` (and its `exceptions` record) and
`TemporalRead`, are renamed in place, so a renamed node keeps its L4 key order.
The characteristic check names its owner `host` (B10), an attribute reference
the endpoint the attribute lives on `subjectType`, a point read its operands
`base` and `date`, and the whole-period qualifier is `{granularity, negated}`,
its v0 boolean `jaar` now the granularity `year` or `month`. The schema
definitions carry the node names (`$defs/Clamp`, `$defs/Validity`, …), and the
three unions are `datatype`, `expr` and `condition`. The one node that keeps its
v0 shape is the rule-status check `Regelversie` (`regel`, `inconsistent`, and
`source` since the first step), until its v1 form lands (delta B25).

Every enum value is an English identifier in lowerCamel case (IR-SPEC,
Conventions), family by family: a `Number`'s `sign`, a `Characteristic`'s
`kind`, the granularity of a timeline facet, a `Timeline` and a `BuiltinCall`, a
rule version's `presentation`, an allocation's `method`, `Comparison.op`,
`Binary.op`, `Rounding.mode`, the `kind` of an `Aggregation`, a `BuiltinCall`
and a `Predicate`, the quantifier of a `Compound` and a `QuantifierFold`, the
granularity of a `WholePeriod`, and a diagnostic's `severity`. The pairs of v0
and L5 value are the section `waarden` of `../proposals/v1-naamtabel.json`, whose
test holds them equal to the enums of this schema. A `ContextValue`'s `name`
stays a free string: `evaluationDate` and `evaluationYear`, the set closed byte
for byte by WF-098. A value a verifier rule refuses by name has an L5 spelling
too, and the schema admits it nowhere it is refused: the severities `error` and
`info` (WF-113 (a)), and the quantifier `the` on a quantifier fold (WF-108 (a);
a compound admits it). A timeline's `granularity` keeps its `null`.

On a renamed node, a field IR-SPEC names optional is absent where v0 wrote
`null`: the bounds of a `Validity` (neither bound is always) and of a
`TimeWindow` (the schema demands neither, because WF-042 refuses a window
without both and keeps its witness a document the schema admits), a role's and
a dimension's `plural`, a dimension's `preposition`, the `wholePeriod` of a
comparison, a predicate and a characteristic check, the check's `host`, a
predicate's `dayKind`, a default's `decision` and `justification`, a defaults
group's `characteristic`, a clamp's `minimum` and `maximum`, and a quantifier
fold's `filter`. A field whose presence a WF rule guards keeps its `null`, and
so does every other nullable field of a renamed node: this step tightens
nothing.

`../proposals/v1-migratie/m5.py` performs both steps. Its default run takes a
document in the L4 shape through the first and the second step, one in the M7
shape through the second alone, and leaves one in the L5 shape untouched;
`--stage m7` and `--stage m8` run one step. The second step refuses, with the
file name and a JSON pointer into its input, an enum value outside the table of
its family, a key the L4 node, form or version does not carry, a node the L4
shape does not have, and a document that mixes the shapes (an L5 node or value
in a document in the M7 shape, a rules container that is neither the M7 nor the
L5 one, a v0 name in a document with the L5 container). Gate 2 holds the
closures of both steps in the L5 rows of its shape self-test, and the patterns
of the validity bounds at `/$defs/Validity/properties/from` and `…/to`, where a
`null` bound is refused and an absent one admitted.

## The form anchor (WF-122)

An `AttributeReference`, a `CharacteristicCheck`, an `Aggregation` and a
`QuantifierFold` carry the optional field `anchor`, whose one value is `form`,
and the condition union has the node `IdentityCheck {node, anchor, negated,
source?}`, whose `anchor` is required (`../IR-SPEC.md` ch. 4.1a and 4.5). In
the filter of an instance fold or of a union selection, or in the filter or the
test of a quantifier fold, where the subject is the element, a node with the
anchor reads the subject of its form, and the identity check compares the
element with it. The canonical place of the field is after `subjectType` on the
reference, after `host` on the check and after `elementType` on the two folds.
The schema admits the field on every one of the four nodes wherever the node
stands, the target of a rule version included, and the identity check wherever
a condition stands: where the two may stand is WF-122, a verifier rule with its
own negative witnesses. The value `form` alone is the schema's: one enum, so a
second value would be a wire change.

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
WF-005, a verifier check with its own negative witness; and `Diagnostic.line`
admits any integer, because `line ≥ 0` (0 = the producer reports no source
position: a hand-built document, or a frontend warning without a token) is
WF-113 (a), likewise with its own witness.

**Extension fields** (the decided wire-entry discipline, `../IR-SPEC.md`
§ Method): a v1 construct enters the wire additively, as an *optional*
property — the `RoleBinding`-carrying fields, the `TemporalRead` expression
node (the row-11 point read), and the `prioritizedDefaults` list under
`rules` (the Phase-4 defaults groups). When present they are written whole,
never null; the loaders admit exactly these keys and nothing else. Since L2
part of that set is required, because the fields it superseded left the wire:
the allocation's connecting role (`connectingRole` since L4), the symbolic
fact-creation fields (since L4 `factType`, `anchorRole`, `targetRole` and the
`path`), and the ranges of the section above. The rest stays optional where
its absence has a meaning of its own: no `path` (`navigatieBindingen` until
L5) is the subject's own read, no `scope` an unscoped uniqueness, no
`navigation` (`navigatieBinding` until L5) a check on the subject itself. The
non-additive cleanup (renames; dropping the superseded fields) is the v1
migration itself, landed step by step.

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
number verbatim over 1 and any other factor as the fraction with the smallest
denominator whose value lies within 10^-14, relative, of the factor's double
(the v0 factors are products of ratios rounded to 16 digits per step, so exact
double equality does not find the intended fraction; the frontend's
re-emission of its declared `1/N` forms is what checks the result), a unit
system's map keys and aliases as the spellings of their unit (never the
abbreviation itself: an alias equal to it is left out), a day kind's
registered spellings as its plural. It refuses, with the file name and a JSON
pointer into the L2 input, every L2 value the L3 shape cannot carry: a factor
that is not positive or whose fraction within that tolerance has a denominator
above 10^9, a third spelling of a day kind,
a registry entry that names no declared day kind, a null in a unit system's
maps, an alias of a unit the system does not declare, a percentage whose unit
is not `%`. One line on stderr per migrated document counts what was
rewritten; a document already in the L3 shape is left untouched.

`../proposals/v1-migratie/m4.py` converts an L3 document to the L4 shape, with
the same interface and the same serialiser. It groups the flattened versions
of one v0 list back under one name (the suffixes of a group exactly `" #1"` to
`" #n"`, contiguous, at least two, with equal statute annotations), maps the
v0 rule kind onto the flag and the tag, writes a null validity as always and
leaves a null condition or optional field out, builds a fact creation's path
from its steps and joining hop and drops its target type after checking it
against the declared filler of the target role, merges the two uniqueness
lists, splits the function node by kind, chooses a literal's value variant by
its type (a decimal comma written as a point, `bron` untouched), and splits
the constant into the bound day and the context values. It refuses, with the
file name and a JSON pointer into the L3 input, every L3 value the L4 shape
cannot carry: a group whose suffixes break those rules, a null or unknown rule
kind, a target that is not an `AttribuutRef`, a null value or assertion, the
method `COMPLEX` or a null method, a target type that is not the declared
filler, an empty key or source list, a bucket on an aggregation or a fold
field on a builtin, a literal without a lexeme or a type, of a type no variant
carries, or with a boolean lexeme other than `waar`/`onwaar`, and a constant
without a name or type. One line on stderr per migrated document counts what
was rewritten; a document already in the L4 shape is left untouched, and one
mixing the two shapes is refused.

## Gate: `../../scripts/check-ir-schema.sh`

The repo's second CI gate. It performs three checks, and two self-tests:

1. **Metaschema** — the schema is itself a valid draft 2020-12 schema.
   - **Self-tests** — what the schema refuses by itself has no corpus witness
     (golden and red must validate, negative is not validated here), so it is
     held against the schema directly, in both polarities: the patterns (the
     validity bounds, and since L4 a literal's number, date and dateTime
     values and the form name) against a lexeme table per kind, and the
     positions the M1, L2, L3, L4 and L5 shapes close against one small valid
     document per shape with one mutation per row. The rows that must pass
     pin the three admissions above, the two narrowings of the written
     ranges, a `RoleMembership` carrying `counterpartType`, an `Unresolved`
     without `type` (the unknown type since L2), in the L3 shape the
     presentation tag on a type annotation and a factor not in lowest terms
     (both refused by the verifier), a percentage without the tag, a declared
     unit without spellings, the bare unit, a factor at the bound 2^62 − 1,
     and a parameter without a facet and a day kind without a plural, and in
     the L4 shape the admissions the verifier refuses with a witness of its
     own (a nameless or blank form, a null reference name, an empty allocation
     attribute, a padded statute annotation, a builtin call without a type, a
     typeless literal, a value variant its type does not name, a context value
     named outside the closed set) and the absences with a meaning of their
     own (no annotations, no condition, no optional allocation field, no
     connecting role on an object creation, no scope, an initialisation, a
     characteristic write, an empty text), and in the L5 shape a source
     record with and without a line, a consistency version without a source,
     a `Validity` and a `TimeWindow` without bounds (the latter WF-042's), a
     context value named `Rekendatum` (WF-098's), the quantifier `the` on a
     compound, the granularity `month` of a whole period, and the rule-status
     check in its v0 node `Regelversie`.
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
  ordered, non-overlapping, aligned to the declared granularity grid —
  the calendar grid, or the one a fixed start point lays (RUNTIME-SEMANTICS
  ch. 3.2);
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
