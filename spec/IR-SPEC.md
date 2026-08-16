# uchar IR — Specification

*Status: v0.4 — the model layer (ch. 2), the rule layer (ch. 3), and the
expression and condition layer (ch. 4) are normative **draft**; everything
else is skeleton. Authority for undocumented behaviour still lives in
`regelspraak-java` (pinned commit `b85dff3`): the spec-§-citing javadoc of
`ir/` and `model/`, and the decision records in
`docs/grammar-divergences.md`; that authority pointer leaves this header at
Phase-1 exit, when this document graduates into the self-containedness gate
and must read stand-alone. Every `§…` reference is a section number of the
RegelSpraak language specification — numbers only; no text from it is
reproduced here (construct keywords, such as the sum-fallback clause named
in ch. 4.3, excepted). This document's own sections are cited as `ch. N`.*

## Method: mirror, then diverge

- **v0** is the faithful JSON dump of `regelspraak-java`'s in-memory IR
  (`Resolver --ir`, emitting since the toolchain commit `c51652a`) at a
  pinned commit — warts included.
- **v1** is what this document specifies: it deviates from v0 deliberately
  where the founding review (`IR-REVIEW.md`) flagged Drools leakage
  (bean-field names → role bindings, salience → declared stratification,
  `bron` string → structured source references, …).
- Every deviation is a tracked delta with a migration note, indexed in the
  delta column of `CONSTRUCTS.md` — never a silent rewrite. The delta list
  doubles as the Phase 0–4 roadmap.

## Conventions (binding)

- Normative keywords per RFC 2119 (MUST/SHOULD/MAY).
- Every MUST carries an ID `WF-###` and comes with: a check in
  `spec/verifier/`, at least one passing case in `corpus/golden/`, and one
  deliberately failing case in `corpus/negatief/`. Where those artifacts do
  not yet exist, the WF index marks them `OPEN` — the debt is visible,
  never silent.
- Every semantics clause carries a worked example with concrete expected
  output; those examples are the reference interpreter's test suite
  (`ref/`), verbatim.
- Every node is marked `layer: core` or `layer: dialect(regelspraak)`; the
  dialect→core lowering is itself specified and tested.
- **A spec sentence without a machine-checkable artifact is not done.**
- Metamodel node and field names are English (PLAN §9, decision 6); each
  node lists its v0 name. *Declared* domain names — object types,
  attributes, roles, labels as authored in a source document — are carried
  verbatim, in whatever language the source declares (Dutch in the
  Jutterswet corpus).
- Versioning: semantic-versioned from the first normative release; v0.x
  while the mirror-then-diverge phase runs.

## Document structure

1. **Scope and conformance** *(skeleton)*
2. **The model layer** — normative draft (since v0.1).
3. **The rule layer** — normative draft (since v0.2).
4. **The expression and condition layer** — **this release.**
5. **Time** *(skeleton)*
6. **Defaults and defeasibility** *(Phase 4)*
7. **Serialization** *(lands with `spec/schema/`)*
8. **Well-formedness rules** — the WF index, at the end of this document.
9. **The RegelSpraak dialect** *(skeleton)*
10. **Versioning policy** *(skeleton)*

---

## 2. The model layer

The model layer declares the vocabulary a rule set computes over: object
types with attributes and characteristics, relations (fact types with
roles), parameters, domains, datatypes, units, dimensions, timelines, and
day kinds. It contains no rules and no expressions; the rule and expression
layers reference model-layer declarations by name.

Layer marks: every node in this chapter is `layer: core`, with exactly two
inline exceptions — the `Characteristic.kind` presentation tag (ch. 2.2)
and the `Dimension.preposition` read-form tag (ch. 2.7), both
`layer: dialect(regelspraak)`.

A **model** is the root container. Its structure:

| Field | Type | Meaning |
|---|---|---|
| `objectTypes` | list of ObjectType | declaration order preserved |
| `factTypes` | list of FactType | — |
| `parameters` | list of Parameter | — |
| `domains` | list of Domain | — |
| `unitSystems` | list of UnitSystem | — |
| `dimensions` | list of Dimension | — |
| `timelines` | list of Timeline | — |
| `dayKinds` | list of DayKind | — |

v0 name: `RegelSpraakModel`. **Delta (tracked, `CONSTRUCTS.md` ch. 2.4):** the
v0 container also carries resolved-rule lists, build diagnostics, and
backend helper methods; those are not part of the IR. The v1 model is the
declaration set only. **Delta:** v0 keys its registries by exact declared
string; v1 makes the name-identity rules explicit (WF-001…WF-003).

- **WF-019** Declaration order is significant and MUST be preserved by
  serialization; consumers MUST NOT reorder (label sets and unit resolution
  are order-sensitive in the source language). Enforced by gate 2
  (`scripts/check-ir-schema.sh`, byte-identical round-trip); a corpus case
  with genuinely reorderable content is still **OPEN**.

### 2.1 Names and identity

Declared names are carried verbatim (whitespace-collapsed, case
preserved). Name *matching* is case-insensitive: the source language
declares names not to be case-distinguishing (§13.2.30).

- **WF-001** Within each of the eight top-level namespaces (object types,
  fact types, parameters, domains, unit systems, dimensions, timelines, day
  kinds), declared names MUST be unique under case-insensitive comparison.
  *v0 mirror note:* v0 silently merges re-declared object types
  (member-wise), silently overwrites duplicate domains, parameters, fact
  types, dimensions and unit systems (last wins), and appends duplicate
  timelines and day kinds (first wins on lookup). **Delta (promotion):** v1
  makes every such collision ill-formed. *Migration:* a v0 dump containing
  a collision fails verification; the source document must rename or merge
  explicitly. Timelines: v0 warns only when a same-named timeline disagrees
  on granularity or count; under WF-001 even an agreeing re-declaration is
  ill-formed.
- **WF-002** Within an object type, attribute names MUST be unique and
  characteristic names MUST be unique, each under case-insensitive
  comparison. Attributes and characteristics are separate namespaces: an
  attribute and a characteristic MAY share a name. *v0 mirror note:* v0
  warns on case-collisions (§13.2.30) but silently overwrites exact
  duplicate attributes and silently double-adds exact duplicate
  characteristics. **Delta (promotion):** ill-formed in v1.
- **WF-003** Every by-name reference in the model layer MUST resolve:
  a domain reference from an attribute or parameter, a dimension reference
  from a dimensioned attribute, a named-timeline reference, a role's filler
  object type. Resolution is case-insensitive. *v0 mirror note:* v0 is
  fail-soft — an unknown domain becomes the sentinel type `Onbekend`
  without diagnostic, unknown dimension and filler-type references are
  stored unchecked, an unresolved timeline reference leaves the member
  untagged. **Delta (promotion):** unresolved references are ill-formed in
  v1; the `Onbekend` datatype does not exist in v1 (ch. 2.4). *Migration:* v0
  dumps carrying `Onbekend` fail verification with the original unresolved
  name as the reason.

### 2.2 ObjectType

An entity type: named, with attributes and characteristics.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `animate` | boolean | v0 `bezield`; grammatical animacy, drives surface forms |
| `plural` | string? | declared plural, if any |
| `attributes` | list of Attribute | declaration order |
| `characteristics` | list of Characteristic | declaration order |

v0 name: `Objecttype`. **Delta (tracked, `CONSTRUCTS.md` row 11):** v0
stores attribute facets (plural, dimensions, timeline granularity/count/
name) and characteristic facets in parallel side-maps keyed by member name;
v1 reifies **Attribute** and **Characteristic** as nodes carrying their own
facets (their "v0 name" is therefore not a node but the side-map facets on
`Objecttype`; the same holds for **TimelineFacet**). *Migration:*
mechanical — each v0 map entry folds into the corresponding member node;
absent map entries become the documented defaults below.

**Attribute**

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `datatype` | Datatype | inline or resolved from a domain reference |
| `plural` | string? | declared plural; absent = none declared |
| `dimensions` | list of dimension names | absent/empty = scalar; order as declared |
| `timeline` | TimelineFacet? | absent = not time-dependent |

**Characteristic** — a boolean trait; it has no datatype.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `kind` | `adjectival` \| `possessive` \| `nominal` | presentation tag (below) |
| `timeline` | TimelineFacet? | absent = not time-dependent |

`kind` (v0 `KenmerkSoort`: BIJVOEGLIJK/BEZITTELIJK/ZELFSTANDIG; untagged
defaults to nominal) is a **presentation tag**: all three kinds denote the
same boolean membership; the tag only selects the predicate surface form
(§3.5, §8.1.8). This is the same pattern as the assignment presentation tag
(PLAN §9, decision 4). Layer: the tag is `dialect(regelspraak)`; the
boolean membership is core.

**TimelineFacet** — the time-dependence of one member or parameter:

| Field | Type | Meaning |
|---|---|---|
| `granularity` | Granularity | `day` \| `week` \| `month` \| `quarter` \| `year` |
| `count` | integer ≥ 1 | periods per segment; 1 = single-period |
| `timelineName` | string? | present iff declared via a named timeline |

*v0 mirror note:* inline declarations record granularity and count but no
name; only the named form records the name. A named reference adopts both
granularity and count from the timeline declaration (§13.3.6.10).
**Staged delta (`CONSTRUCTS.md` row 11):** v1 keeps the facet at the
member; lifting temporality into the datatype itself (timeline-of-element)
is the remaining step of the row-11 delta and lands with the expression
layer, where it has consequences. Recorded here so it is not mistaken for a
settled shape.

- **WF-004** A dimensioned attribute's `dimensions` list MUST be non-empty,
  duplicate-free, and every entry MUST resolve to a declared Dimension
  (WF-003). *v0 mirror note:* stored without any check. **Delta
  (promotion).**
- **WF-005** A `TimelineFacet` MUST have `count` ≥ 1 and, when
  `timelineName` is present, the named Timeline MUST exist and the facet's
  granularity and count MUST equal the declaration's (adoption is total,
  not partial). *v0 mirror note:* v0 adopts from the first matching
  declaration and leaves unresolved references silently untagged. **Delta
  (promotion).**

### 2.3 Domain

A named, reusable datatype.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `datatype` | Datatype | an Enumeration for a value-list domain, else a base datatype |

v0 name: `domeinen` registry entry.

- **WF-006** An enumeration domain MUST declare at least one value, and its
  values MUST be unique. Value order is normative and preserved. *v0 mirror
  note:* unchecked in v0. **Delta (promotion).**

### 2.4 Datatype

A closed sum. v1 variants:

| Variant | Fields | Notes |
|---|---|---|
| `Number` | `whole: boolean`, `decimals: int`, `sign`, `unit: Unit?` | see precision forms below |
| `Text` | — | equality only; not orderable |
| `Boolean` | — | equality only |
| `Date` | `precision: day \| millisecond` | orderable; `precision` is deliberately not called granularity — that term is reserved for the five-value timeline set |
| `Enumeration` | `domain: string`, `values: list of string` | `domain` is a resolvable reference to a declared Domain (WF-003), not a free string — this satisfies the "real reference, not a name string" delta of `CONSTRUCTS.md` ch. 2.4 |

`sign` ∈ {`unspecified`, `negative`, `nonNegative`, `positive`} (v0
`Teken`).

**Precision forms** (v0 encoding mirrored, made explicit): a `Number` is
exactly one of — (a) *whole*: `whole = true`, `decimals = 0`; (b) *fixed
decimals*: `whole = false`, `decimals ≥ 1`; (c) *unbounded*: `whole =
false`, `decimals = 0` (no precision ceiling; §13.3.3 form (c)). The
`whole` flag is the sole discriminator between (a) and (c).

- **WF-007** A `Number` MUST satisfy one of the three precision forms;
  `decimals` < 0 is ill-formed. A source declaration of "0 decimals"
  normalizes to the whole form (v0 behaviour, kept and specified).
- **WF-008** Two `Enumeration` types are compatible only when they name the
  same declaring domain; value-set equality does not create compatibility.
  (v0 behaviour at use-time, lifted into the type identity rule.) This is a
  *use-time* rule recorded with the type it governs; its verifier check
  belongs to the expression layer — forward reference, **OPEN**.

Orderability: `Number` and `Date` are orderable; `Text`, `Boolean`, and
`Enumeration` are equality-only. This is deliberate (an ordered use of a
non-orderable type is a rule-layer WF violation, specified with that
layer).

**Deltas (tracked, `CONSTRUCTS.md` row 27):**

- v0 `Percentage` is removed as a separate variant: a percentage is a
  `Number` whose unit is `%` (or a rate `%/<unit>`), plus a
  `dialect(regelspraak)` presentation tag on the declaration. *Migration:*
  `Percentage(whole, decimals, unit)` → `Number(whole, decimals,
  unspecified, unit)` + tag. The v0 rule that number and percentage are
  distinct typing families is preserved observationally by unit
  compatibility (`%` converts to nothing but itself); whether the
  expression layer needs the tag for the §4 percentage-of operations is
  decided in that chapter — **OPEN**.
- v0 `DatumInDagen`/`DatumTijd` merge into `Date(precision)`.
  *Migration:* mechanical. Mixed-precision date use remains a rule-layer
  typing error, as in v0.
- v0 `Onbekend(reden)` is removed: v1 fails fast at verification (WF-003)
  instead of carrying an unknown-type sentinel. Honest inexpressibility at
  the *expression* level remains the job of the `Onopgelost` node
  (`CONSTRUCTS.md` row 16), which is a different thing: a well-formed
  marker with a reason, not a type.

### 2.5 Unit and UnitSystem

A **UnitSystem** declares units with their spellings and conversions:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `units` | list of Unit | declaration order |

A **Unit**:

| Field | Type | Meaning |
|---|---|---|
| `abbreviation` | string | primary spelling (`jr`, `km`, `€`, `%`) |
| `system` | string? | declaring system; absent for the built-in `%` |
| `canonical` | string? | the system unit this reduces to; absent for opaque products |
| `factorToCanonical` | rational? | how many canonical units equal one of this unit |
| `denominator` | Unit? | present for rate units (`€/jr`, `%/jaar`) |
| `spellings` | word + plural aliases | resolve to the same unit |

v0 name: `Eenheid`/`Eenheidstelsel`, mirrored structurally.

**Semantics — conversion algebra** (normative; the worked examples become
`ref/` tests):

1. *Canonical reduction.* Every declared unit reduces to exactly one
   canonical unit of its system, with a positive rational factor; a
   self-canonical unit has factor 1. Two units are **convertible** iff both
   are plain or both are rates, their numerators reduce to the same
   canonical, and (for rates) their denominators are recursively
   convertible. Sharing a system is not sufficient: two independent base
   units in one system are not convertible.
2. *Restatement.* Restating a quantity from unit *u* to convertible unit
   *v* multiplies by `factor(u)/factor(v)` — carried as an exact rational
   (numerator, denominator), rounded once at the end, never pre-rounded.
   *v0 mirror note:* v0 has both a pre-rounded single ratio (DECIMAL64) and
   a num/den pair; even the pair is only as exact as the stored factors,
   which v0 pre-rounds for declared `1/N` forms. **Delta (tracked,
   `CONSTRUCTS.md` row 26):** v1 stores factors as true rationals and
   specifies only the exact restatement form; the pre-rounded single-ratio
   form is dropped. *v0 mirror note (fail-open):* v0's factor lookup
   returns 1 both for "identical" and for "not convertible", so
   correctness depends on the caller checking convertibility first; v1
   removes the ambiguity by making restatement between non-convertible
   units ill-formed (WF-010).
3. *Rates.* A rate unit converts by restating the numerator and inversely
   restating the denominator.
4. *Products.* Multiplying quantities multiplies units into a **product
   unit**: the sorted multiset of atomic factors (commutative,
   associative). Product units are deliberately opaque — no canonical, no
   factor, no cancellation — so any comparison or assignment against a
   concrete unit is a unit mismatch rather than a silent pass.

Worked examples (expected values exact):

- Declared: `jaar (jr) = 12 mnd`, `maand (mnd)` canonical. Then `3 jr`
  restated in `mnd` = `36 mnd` (factor 12/1); `18 mnd` restated in `jr` =
  `3/2 jr` (factor 1/12, exact — not 0.0833…).
- `€/mnd` → `€/jr`: numerator factor 1, denominator inverts: factor 12/1;
  `100 €/mnd` = `1200 €/jr`.
- `€ · m` and `m · €` are the same product unit; `€·m` is not convertible
  to `€`, to `m`, or to `€·m²`.
- `%` is convertible only to `%`; `%/jaar` only to `%`-numerator rates
  with a year-convertible denominator.

- **WF-009** Unit declarations MUST reduce: every unit's conversion chain
  terminates in a canonical unit of its own system; conversion cycles and
  conversions targeting unknown units are ill-formed. Conversion factors
  MUST be positive rationals. *v0 mirror note:* a conversion cycle
  silently makes the re-entered unit its own base; an *unknown* target is
  worse — v0 adopts the unknown name as the canonical, so two units both
  declared against the same unknown target silently become convertible
  through a unit that does not exist; an unreadable factor warns and
  drops. **Delta (promotion).**
- **WF-010** Restatement (in any layer) between non-convertible units is
  ill-formed. Like WF-008 this is a *use-time* rule — it constrains the
  layers where restatement occurs, and its check lands with the expression
  layer. A unit-less operand meets a unit-bearing operand only where the
  operator table of the expression layer explicitly allows it (forward
  reference; specified with that layer — **OPEN**).

### 2.6 FactType and Role

A **FactType** relates object types through named roles; it is the
relation-typed core of the model.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `reciprocal` | boolean | v0 `wederkerig`: one role relating a type to itself |
| `roles` | list of Role | declaration order |

A **Role**:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | the navigation label |
| `objectType` | string | filler object-type name (resolves per WF-003) |
| `plural` | string? | declared plural; absent = none |
| `many` | boolean | cardinality: to-many (`meerdere`) vs to-one (`één`) |

v0 names: `Feittype`, `Rol`. The v0 model layer is already symbolic — no
bean-field names appear here; the bean-field leakage lives in the v0
*navigation contract* and the rule-layer encodings, whose replacement by
symbolic role bindings is `CONSTRUCTS.md` rows 4/22 (rule-layer chapters).

Role-name matching is case-insensitive, over the declared name and the
declared plural. *v0 mirror note:* when no plural is declared, v0 admits a
best-effort synthesized plural (`+s`/`+en`) at use sites; a declared
plural always wins over a synthesized one. **OPEN (design decision for the
expression layer):** whether v1 keeps best-effort plural matching at all —
it is morphology, not semantics, and belongs in the frontend if anywhere.

- **WF-011** A reciprocal fact type MUST have exactly one role; a
  non-reciprocal fact type MUST have at least two. Whether more than two
  roles are admitted is **OPEN** (v0 stores what the source declares and
  never checks; no corpus case exercises >2).
- **WF-012** Role names within a fact type MUST be unambiguous: no two
  roles of one fact type may collide case-insensitively on name or
  declared plural. *v0 mirror note:* v0 resolves navigation fail-safe (a
  non-unique match returns "no navigation" downstream); v1 moves the
  guarantee to declaration time. **Delta (promotion).**
- **WF-013** A reciprocal fact type's role MUST be to-one, and a
  non-reciprocal fact type's roles MUST each carry an explicit cardinality.
  *v0 mirror note:* v0's cardinality marking skips reciprocal fact types
  (§3.11), leaving their role at the to-one default — v1 states the
  default as a requirement; for non-reciprocal roles v0 matches
  cardinality segments fail-open (an unmatched role silently stays
  to-one). **Delta (promotion)** for the fail-open part.

### 2.7 Dimension

| Field | Type | Meaning |
|---|---|---|
| `name` | string | singular name |
| `plural` | string? | plural name (used by aggregation-over-all forms) |
| `preposition` | string? | present = suffix read form (`<attr> <prep> <label>`); absent = prefix form (`<label> <attr>`) |
| `labels` | list of string | ordered; the order is normative |

v0 name: `Dimensie`. The `preposition` is presentational
(`dialect(regelspraak)`, `CONSTRUCTS.md` row 28); the ordered label set is
core — an aggregation over a label interval ranges over this order.

- **WF-014** A dimension's label set MUST be non-empty and duplicate-free.
  *v0 mirror note:* unchecked. **Delta (promotion).**

### 2.8 Timeline

A named period grid.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | matching is normalized: whitespace-collapsed, case-insensitive, one leading Dutch article stripped |
| `variableStart` | boolean | anchor is bound per object by a start-point determination (§9.9) |
| `granularity` | Granularity | closed set of five |
| `count` | integer ≥ 1 | periods per segment |

v0 name: `Tijdlijn`. Granularity is one of `day`, `week`, `month`,
`quarter`, `year` — deliberately no sub-day granularity (mirrored).

*v0 mirror note:* the name-matching rule above (whitespace collapse,
case-insensitivity, stripping one leading Dutch article) is v0 morphology
mirrored into an identity rule. **OPEN (same family as the best-effort
role-plural matching, ch. 2.6):** whether v1 keeps source-language
morphology inside a core identity rule at all, or moves it to the frontend
so the core sees only resolved names.

- **WF-015** All references to a timeline name MUST resolve (WF-003), and —
  with WF-001 — there is exactly one declaration per normalized name, so
  the v0 "first declaration wins" rule vanishes with the duplicate it
  resolved. **Delta (promotion).**
- **WF-016** A `variableStart` timeline MUST be anchored by a start-point
  determination. This is a cross-layer rule: the determination lives in the
  rule layer, so the check is specified there — **OPEN** here, recorded so
  the model layer names the obligation. *v0 mirror note:* v0 discovers the
  missing anchor only at transpile time (marker/skip).

Worked example (facet adoption, becomes a `ref/` test): declaration
"timeline `het penningjaar`, granularity year, count 1, fixed start" +
attribute declared against timeline `penningjaar` → the attribute's facet
is `{granularity: year, count: 1, timelineName: "penningjaar"}` (article
stripped, adoption total per WF-005).

### 2.9 DayKind

A named class of days (`werkdag`, `stormvloeddag`) usable as a predicate
and as a duration unit.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | singular |
| `plural` | string? | duration-unit spelling (`5 werkdagen`) |

v0 name: the `dagsoorten` registry (the day-kind's *definition* — a
predicate over a date — is a rule-layer form, `CONSTRUCTS.md` row 23).
Spelling lookup is trimmed and case-insensitive.

- **WF-017** A day-kind's spellings MUST NOT collide with the built-in
  calendar units (day/week/month/quarter/year/hour/minute/second/
  millisecond, singular or plural), and MUST NOT collide with another
  day-kind's spellings. *v0 mirror note:* the built-in collision warns and
  skips; the day-kind/day-kind collision is silently first-wins. **Delta
  (promotion).**

### 2.10 Parameter

A top-level named value.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `datatype` | Datatype | inline or via domain reference |
| `timeline` | TimelineFacet? | present = time-dependent (value is a per-period timeline) |

v0 name: `Parameter`. The *values* of parameters (validity-versioned
vintages, selected by evaluation date, fail-fast on gaps and overlaps) are
data-plane, specified in `RUNTIME-SEMANTICS.md` ch. 8 — deliberately not a
model-layer node (`CONSTRUCTS.md` ch. 2.4, `Parameterset`).

*v0 mirror note:* a v0 parameter records only a bare granularity — no
period count and no timeline name (a parameter declared over a multi-period
or named timeline loses that information in v0). **Delta:** v1 gives
parameters the same full `TimelineFacet` as attributes. *Migration:* a v0
granularity becomes `{granularity, count: 1}`; no v0 parameter carries a
timeline name.

- **WF-018** A parameter's datatype MUST resolve (WF-003) and its timeline
  facet, when present, MUST satisfy WF-005. **Delta (promotion).**

---

## 3. The rule layer

The rule layer holds the executable forms: assignment, allocation, object
and fact creation, consistency, uniqueness, day-kind definitions,
start-point determinations, and (dialect) rule groups. Rule forms reference
model-layer declarations by name (WF-003 applies throughout); their
operational meaning — forward chaining to a fixpoint, freshness,
idempotence — is `RUNTIME-SEMANTICS.md` ch. 5. List order is preserved by
serialization (WF-019) but carries no execution semantics: the evaluation
model is order-independent, and nothing in this chapter introduces a
priority. Layer marks: every node in this chapter is `layer: core`, with
two exceptions — RuleGroup (ch. 3.9) and the `RuleVersion.presentation`
tag (ch. 3.1), both `layer: dialect(regelspraak)`.

**Versioning (applies to every *named* form).** Rule, Allocation,
ObjectCreation, FactCreation, Consistency, and Uniqueness all share one
shape: `{name, versions}` with `versions` ≥ 1 in declaration order. The
`name` sits on the form; everything else — including `validity` and
`condition` — sits on each version, so a version is the form's full body.
The field tables in ch. 3.2–3.6 therefore describe *one version's body*;
ch. 3.1 shows the pattern in full. Day-kind definitions, start-point
determinations, and rule groups are unversioned. *v0 mirror note:* v0
flattens every versioned form into per-version entries under the `" #i"`
name suffix; the migration of ch. 3.1 (strip and group) applies uniformly.

### 3.1 Rule (assignment and characteristic attribution)

One node covers computing an attribute and attributing a characteristic
(PLAN §9, decision 4). A **Rule** names a target and carries one or more
**versions**:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | rule identity |
| `versions` | list of RuleVersion, ≥ 1 | in declaration order |

**RuleVersion**:

| Field | Type | Meaning |
|---|---|---|
| `validity` | Validity | which evaluation dates this version governs (ch. 3.10) |
| `target` | attribute reference | the written member: an attribute, or a characteristic |
| `value` | expression | the computed value; for a characteristic target: the boolean constant true (v0 mirror; Phase 4 generalises this into the default model) |
| `condition` | Condition? | absent = unconditional |
| `initialisation` | boolean | write-only-if-empty (§9.6) — the defeasible default of `CONSTRUCTS.md` row 7; semantics in `RUNTIME-SEMANTICS.md` ch. 7 |
| `presentation` | `calculation` \| `equation` | dialect tag: which surface form (§13.4.4/.9) produced it; no semantic difference |

v0 name: `ResolvedRegel` with `Soort` ∈ {GELIJKSTELLING, BEREKENING,
INITIALISATIE, KENMERKTOEKENNING}. **Delta (tracked, `CONSTRUCTS.md`
row 2):** v0 flattens a multi-version rule into separate entries whose
names carry a `" #i"` suffix (1-based); v1 makes versions explicit
structure under one name. The suffix rides *every* rule form built from a
versioned rule (allocations, creations, consistency, uniqueness alike) —
not day-kind definitions, start points, or groups. *Migration:* group v0
entries of any form by stripping the suffix; a single-version rule becomes
one version. **Delta (row 2 /
decision 4):** GELIJKSTELLING and BEREKENING collapse into the
`presentation` tag (v0 distinguishes them only by which surface token was
used); INITIALISATIE becomes the `initialisation` flag; KENMERKTOEKENNING
becomes a characteristic-typed `target` — the "target-kind flag" of
`CONSTRUCTS.md` row 2 is realised as the target's member kind (attribute
vs characteristic reference), needing no separate boolean; ch. 3.3's
MemberInit keeps an explicit flag only because its member is a bare name.
(v0 marks it with the tag, types the target boolean, and fixes `value` to
the literal true — mirrored.)

- **WF-020** Rule names MUST be unique (case-insensitive) across all rule
  forms in a model. *v0 mirror note:* uniqueness in v0 is a by-product of
  the name-suffix scheme; nothing checks it. **Delta (promotion).**
- **WF-021** The validity windows of one named form's versions MUST NOT
  overlap — the rule holds for every versioned form of this chapter, not
  only Rule.
  Gaps are allowed — on a date no version covers, the rule simply does not
  apply. *v0 mirror note:* v0 already diagnoses overlap as a resolver
  **error** at build time, while still emitting both versions; v1 lifts the
  same condition into IR well-formedness (an overlapping dump is
  ill-formed), matching the fail-fast vintage discipline
  (`RUNTIME-SEMANTICS.md` ch. 8).
- **WF-022** A version's `target` MUST resolve to a declared attribute or
  characteristic of the subject type, reachable over the declared
  navigation; a characteristic target's `value` MUST be the boolean
  constant true. *v0 mirror note:* v0 is fail-soft here too — an
  unresolvable target degrades to an unknown-typed marker and the typing
  check silently skips it; only the *typing* of a resolved target errors
  (that is WF-023). The characteristic-value shape is constructed, not
  checked. **Delta (promotion).**
- **WF-023** Assignment typing: the value's type MUST be assignable to the
  target's declared type — same family, compatible units, enumeration
  labels from the target's domain, sign constraint respected, and the
  precision gate of `RUNTIME-SEMANTICS.md` ch. 2.4 (including its
  candidate rule: a possibly non-terminating value passes explicit
  rounding before a bounded write). *v0 mirror note:* enforced as resolver
  errors via the §6/Tabel 61 typing tables — except for characteristic
  attribution, where v0 skips the check (harmless today: the value is a
  constructed constant).

The corpus case `corpus/golden/jutterswet-penningdrempel/` instantiates
this node in its **v0 mirror form** (a flat entry with the
KENMERKTOEKENNING tag): one rule, one version, characteristic target,
unconditional validity, a comparison condition.

### 3.2 Allocation

Distribute a source amount over related recipients. v0 name:
`ResolvedVerdeling`; semantics as recurrence equations in
`RUNTIME-SEMANTICS.md` ch. 4. Versioned per the chapter rule; the table
below (minus `name`) is one version's body.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `sourceType`, `sourceAttribute` | type ref, string | the amount being distributed |
| `targetType`, `targetAttribute` | type ref, string | each recipient's share |
| `method` | `equalShares` \| `proportional` \| `ordered` | §9.7.2 |
| `ratioAttribute` | string? | weight; present iff proportional, or ordered with a proportional tie-break |
| `maximumAttribute` | string? | per-recipient cap (§9.7.3); requires a ratio |
| `roundingDecimals` | integer? | floor each share at n decimals (§9.7.4) |
| `remainderAttribute` | string? | source attribute receiving the undistributed rest (§9.7.5) |
| `orderingAttribute` | string? | ordered only: the sort key |
| `descending` | boolean | ordered only |
| `condition` | Condition? | — |
| `validity` | Validity | — |

**Delta (tracked, `CONSTRUCTS.md` row 17):** v0's fourth method value
`COMPLEX` — the donor's unhandled-form placeholder — is not admissible IR;
what v0 marked COMPLEX is ill-formed in v1 (WF-024). *v0 mirror note:* v0
can also carry ordering fields alongside COMPLEX (set before the method
was demoted); such hybrids disappear with the method value.

- **WF-024** `method` MUST be one of the three real methods.
- **WF-025** Field validity per method: `ratioAttribute` present iff the
  method is proportional or ordered-with-proportional-tie;
  `maximumAttribute` only together with a ratio; `orderingAttribute` (and
  `descending`) only for ordered, and the ordering key's type MUST be
  orderable; share-assignment typing per WF-023 applied to
  target ← source. *v0 mirror note:* v0 enforces the typing as an error
  and demotes the rest to COMPLEX. **Delta (promotion)** for the demotion
  paths.
- **WF-026** At most one allocation MAY write a given (type, attribute)
  target. *v0 mirror note:* two allocations on one target are
  agenda-order-dependent and v0's exec backend refuses them as a
  principled skip. **Delta (promotion, row 17).**
- A declared `roundingDecimals` or `maximumAttribute` without a
  `remainderAttribute` silently discards the rest; producers SHOULD
  declare the remainder target (v0 warns — kept advisory, the legal
  meaning may genuinely be "the rest lapses").

### 3.3 Object creation

A subject creates an object of a declared type and initialises members.
v0 name: `ResolvedObjectCreatie` (+ `Toekenning`). Versioned per the
chapter rule; the table below (minus `name`) is one version's body.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `createdType` | type ref | — |
| `subject` | type ref | the creating subject |
| `initialisations` | list of MemberInit | declaration order |
| `condition` | Condition? | — |
| `validity` | Validity | — |

**MemberInit**: `member` (string), `value` (expression), `characteristic`
(boolean — v0 `kenmerk`: true = §13.4.6.4 characteristic-value form, the
member names a declared characteristic and the value is a boolean
constant; false = §13.4.6.3 attribute-value form).

- **WF-027** Every `member` MUST be a declared attribute or characteristic
  of `createdType`; a characteristic initialisation's value MUST be a
  boolean constant. *v0 mirror note:* both are resolver errors, but v0
  still appends the offending initialisation after diagnosing — v1 makes
  the form itself ill-formed. **Delta (promotion of the append).**
  Assignment typing per WF-023 applies per initialisation.

### 3.4 Fact creation

Create a relation instance linking a subject to targets reached along a
navigation path. v0 name: `ResolvedFeitCreatie` (+ `Tussenstap`).
Versioned per the chapter rule; the table below (minus `name`) is one
version's body.

**Delta (tracked, `CONSTRUCTS.md` rows 4/22 — the heaviest v0 leak):** v0
encodes the creation as bean reference-fields: `zetVeld` (the field set in
the consequence), `joinVeld` (the field joined in the condition), and
per-hop `Tussenstap.veld` + `viaOuderVeld` (which side holds the field).
v1 re-expresses the same content symbolically:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `factType` | fact-type ref | the relation being created |
| `anchorRole`, `targetRole` | role refs into `factType` | which role the anchor fills, which the target |
| `anchor` | type ref | the iterated subject |
| `path` | list of PathStep | how targets are reached from the anchor; empty = one hop |
| `condition` | Condition? | guard on anchor or target |
| `validity` | Validity | — |

**PathStep**: `factType` (ref), `role` (ref), `direction` (`toOne` \|
`toMany`) — a symbolic hop over an existing relation. *Migration:* v0's
`zetVeld`/`joinVeld`/`veld` are derivable from the role bindings by the
backend's own role→field mapping; the derivation moves down into the
backend, out of the IR (the Phase-3 forcing move).

- **WF-028** `factType`, both roles, and every path step MUST resolve
  (WF-003); the path MUST actually reach the target type; the created
  relation MUST differ from the relation of the final hop. *v0 mirror
  note:* v0 enforces all of this fail-soft — seven distinct
  warning-and-drop reasons; an emitted v0 record is always fully
  resolved. **Delta (promotion):** in v1 the ill-formed variants are
  verifier errors, not silent drops.

### 3.5 Consistency

An assertion that must hold; a violation is observable on the §9.5
inconsistency channel — declarative, not "fire on the negation"
(`CONSTRUCTS.md` row 19). v0 name: `ResolvedConsistentie`. Versioned per
the chapter rule; the table below (minus `name`) is one version's body.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `subject` | type ref | the asserted-about type |
| `assertion` | Condition | must hold for every instance |
| `validity` | Validity | — |

- **WF-029** The assertion MUST have a single subject type. *v0 mirror
  note:* v0 additionally restricts the assertion's *shape* (comparisons,
  empty/filled predicates, and all-quantified compounds of those;
  everything else warned-and-dropped) — that is backend maturity, not IR
  scope: v1 admits the general condition form and the coverage obligation
  of `CONSTRUCTS.md` row 19 stands.

### 3.6 Uniqueness

One construct for single-type and union (`verenigd met`) uniqueness
(`CONSTRUCTS.md` row 20). v0 names: `ResolvedUniciteit` +
`ResolvedUniciteitUnie` (+ `Bron`). Versioned per the chapter rule; the
table below (minus `name`) is one version's body.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `sources` | list of Source, ≥ 1 | one = single-type; more = union |
| `scope` | navigation ref? | absent = global; present = unique per scope owner |
| `validity` | Validity | — |

**Source**: `objectType` (ref), `keys` (list of attribute names, ≥ 1 — a
multi-attribute key is jointly unique by positional concatenation, §5.7).
Semantics note (mirrored): an empty key value never collides (§8.1.6).

**Delta (row 20):** v0's `scopeVeld` — a bean reference-field name — is
replaced by a symbolic scope navigation (role reference), same move as
ch. 3.4. *Migration:* derivable via the backend's role→field mapping.

- **WF-030** Every source's keys MUST resolve to declared attributes; all
  sources MUST agree on key arity and per-position datatype kind
  (including unit, for numeric keys). *v0 mirror note:* v0 checks
  arity/kind for the union form (warn-and-drop) and nothing for the
  single form. **Delta (promotion).** **OPEN** (`CONSTRUCTS.md` row 20):
  whether a time-dependent attribute is admissible as a key — v0's
  backends decline temporal keys; the spec must decide, not inherit the
  refusal.

### 3.7 Day-kind definition

The predicate that defines a declared DayKind (model layer, ch. 2.9): a
condition over "the day". v0 name: `ResolvedDagsoort`.

| Field | Type | Meaning |
|---|---|---|
| `dayKind` | day-kind ref | the declared name (ch. 2.9) |
| `condition` | Condition | over the day; may reference other day kinds |
| `validity` | Validity | — |

- **WF-031** The reference MUST resolve to a declared DayKind; the
  condition MUST be present; there MUST be exactly one definition per
  DayKind. *v0 mirror note:* a missing condition is warn-and-drop; a
  duplicate definition warns and appends with first-wins lookup.
  **Delta (promotion).** **OPEN:** whether mutually recursive day-kind
  definitions are admissible (v0 neither checks nor documents this) —
  decided with the stratification chapter.

### 3.8 Start-point determination

The anchor metadata for a variable-start timeline: which member of which
type carries the per-object start date. The *write* of that member is an
ordinary Rule (ch. 3.1) — the determination itself carries no condition
and no validity (they ride the write rule). v0 name: `ResolvedStartpunt`.

| Field | Type | Meaning |
|---|---|---|
| `timeline` | timeline ref | — |
| `objectType` | type ref | — |
| `member` | string | the synthetic day-precision date attribute |

- **WF-032** The timeline MUST be declared with `variableStart` (the
  converse of WF-016 — an anchor for a fixed-start timeline is
  ill-formed; v0 warns and lowers anyway: **delta, promotion**); the
  member MUST be a day-precision Date, and so must the deriving
  expression of the anchor's write rule (v0 errors on both, the latter
  non-aborting; in v1 the latter is WF-023 typing applied to that rule);
  at most one determination per (objectType, timeline) — v0 deduplicates
  silently, v1 makes the duplicate ill-formed.

### 3.9 RuleGroup — `layer: dialect(regelspraak)`

Namespacing over member rules plus a recursion marker (§9.10, documented
extension; stays out of the core per PLAN §9, decision 5). v0 name:
`ResolvedRegelGroep`.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `recursive` | boolean | the §9.10 marker |
| `members` | list of rule names | written order |

Members are ordinary rules; the group adds no execution semantics. The
`recursive` marker's meaning awaits the neutral fixpoint semantics of
`RUNTIME-SEMANTICS.md` ch. 5 (tracked in `CONSTRUCTS.md` row 14 — or it
is a non-goal); until then it is carried, not interpreted.

- **WF-033** (dialect) Every member name MUST resolve to a rule form in
  the model; a `recursive` group MUST contain at least one object-creation
  member, and every object-creation member of a recursive group MUST be
  conditional (an unguarded creation in a recursive group cannot
  terminate). *v0 mirror note:* containment is a v0 error; the unguarded
  case only warns. **Delta (promotion)** for the guard.

### 3.10 Validity

The shared validity-window value (v0 name: `Geldigheid`): `from` (date?)
and `to` (date?), both inclusive, an absent bound open; both absent =
always. Year shorthands ("vanaf 2024" = from 2024-01-01) are frontend
sugar, widened before the IR. Selection semantics (which version governs
an evaluation date) are `RUNTIME-SEMANTICS.md` ch. 8. *v0 mirror note:* a
malformed bound is a frontend error — v0 skips the whole version, so a
Validity in emitted IR is always well-formed; WF-021 adds the cross-
version constraint.

---

## 4. The expression and condition layer

Expressions compute typed values; conditions decide. Types are the ch. 2.4
datatypes; evaluation semantics — emptiness, exact arithmetic, rounding —
are `RUNTIME-SEMANTICS.md` ch. 1–2, whose worked examples bind the meaning
of every operator named here. Layer marks: core, with exactly five inline
dialect exceptions — the date-builtin library and the three dialect
predicate kinds (ch. 4.3/4.5), the abs-division operator (ch. 4.2), the
dimension forms `DimensionCellRead` and `dimensionSelection` (ch. 4.3, row
28), and the whole-period qualifier (ch. 4.5). Every node carries a source
reference; **pending delta
(tracked, `CONSTRUCTS.md` row 3):** today that is the v0 `bron` string —
the structured source reference (`file, line, column, snippet`) lands with
the cross-repo source-anchoring card and will change the wire format and
the schema when it does.

### 4.1 Literals and references

| Node | Fields | Notes |
|---|---|---|
| Literal | `value`, `type`, `source` | **Delta (row 27):** v0 carries the unparsed source string as the value; v1 carries a typed value |
| AttributeReference | `attribute`, `subjectType`, `path` (role steps), `type`, `source` | **Delta (row 4):** v0 splits navigation over `navigatieRol` + `navigatiePad` + a `set` flag (with the invariant that a non-empty path implies a role) and keeps the transpiler-facing `onderwerpBron`; v1 unifies into one explicit path of role steps, with result cardinality derived from the declared role cardinalities. *Migration:* mechanical merge of the three fields |
| ParameterReference | `parameter` (name ref), `type`, `source` | the v0 wire form is already symbolic (the emitter writes the name) — the "embed the model object" wart is in-memory only |
| VariableReference | `name`, `type`, `source` | a rule-local let-binding (§13.4.2.10); binding is frontend scope resolution |
| ContextValue | `name` ∈ evaluation-date, evaluation-year | **Delta (row 25):** split from v0's `Constante` grab-bag; the pronoun referents that shared that node are surface forms the frontend resolves away |

### 4.2 Operators

**Binary** (`op`, `left`, `right`, `type`, `source`). The operator set,
with layer marks per `CONSTRUCTS.md` ch. 2.1: plus, min, maal,
gedeeld-door, tot-de-macht — core kernel; verminderd-met — core with its
own empty rule (`RUNTIME-SEMANTICS.md` ch. 1.2; its fate is tied to the
row-18 decision); gedeeld-door-abs — dialect (a defined collapse point,
ch. 2.2 there); percentage-van — sugar over kernel arithmetic, mirrored as
an operator. *v0 mirror note:* a typing failure degrades the node's `type`
to the unknown sentinel rather than erroring the node away — with WF-035
the sentinel disappears from well-formed v1 IR.

**Rounding** (`base`, `mode`, `decimals`, `type`, `source`) — the five
modes of `RUNTIME-SEMANTICS.md` ch. 2.5; the type carries the rounded
precision (the WF-023/ch. 2.4 gate leans on this). **Clamp** (`base`,
`minimum?`, `maximum?`) — sugar verdict (row 1), mirrored as a node; at
least one bound present. **TextComposition** (`parts`: literal chunks and
embedded expressions) — sugar verdict (row 31), mirrored; empty coerces to
`""` per `RUNTIME-SEMANTICS.md` ch. 1.2.

### 4.3 Aggregation and builtin calls

**Delta (tracked, `CONSTRUCTS.md` row 1):** v0's single overloaded
`Functie` node splits in v1 into two nodes.

**Aggregation** — folds over object instances or dimension cells:

| Field | Meaning |
|---|---|
| `kind` | sum, total, count, minimum, maximum, first, last |
| `arguments` | one set-valued operand (instance fold), or ≥ 2 listed operands (value fold / §5.8.1 union count) |
| `elementType` | the instance type ranged over (instance folds) |
| `filter` | Condition? — the die/dat-subselection or a temporal (gedurende) restriction |
| `emptyFallback` | expression? — **sum only**: the §5.8.2 "of … als die er niet zijn" clause |
| `dimensionSelection` | ? — **sum/min/max only**: fold over an ordered label selection instead of instances (dialect, row 28) |
| `type`, `source` | — |

Empty-source results differ per kind — the som/totaal split and the
no-fallback rule for min/max/first/last are `RUNTIME-SEMANTICS.md`
ch. 1.2, mirrored in the matrix (row on TOTAAL, `CONSTRUCTS.md` ch. 2.1).

**BuiltinCall** — scalar, text, and date builtins with plain arguments:
wortel, absolute-waarde (core kernel); concatenatie (text fold);
tijdsduur/absolute-tijdsduur (two date operands, result unit declared);
tijdsevenredig-deel and aantal-dagen-in (each carrying a `granularity`
bucket ∈ month/year); eerste-paasdag, datum-met, datumtijd-met, and the
seven date/time part extractors — the date library is
`layer: dialect(regelspraak)` (row on date builtins, `CONSTRUCTS.md`
ch. 2.1); semantics in `RUNTIME-SEMANTICS.md` ch. 2–3.

*v0 mirror notes:* one node carries the union of all facets, with two
convenience constructors defaulting the rest; the accumulate-pattern
framing in its documentation is the known leak (row 1). A v0 count whose
subject happens to name a declared "aantal …" parameter is re-read as a
ParameterReference by the frontend — surface disambiguation, not IR.
*Migration:* split by kind category; facets carry over one-to-one.

**DimensionCellRead** (`attribute`, `labels`, `type`, `source`) — dialect
(row 28): a single-cell read of a dimensioned attribute; every dimension
pinned (WF-040); no storage layout implied.

### 4.4 Unresolved

**Unresolved** is the honest-inexpressibility marker (row 16, permanent
contract): an expression variant carrying its declared-unknown type and
the source slice, and a condition variant carrying the source slice. A
well-formed IR MAY contain Unresolved nodes; what it may never do is hide
them (WF-044). **Delta (row 16):** v1 adds a structured `reason` (the
four-way taxonomy of `CONSTRUCTS.md` ch. 4: structural, declared-names
refusal, spec-underdetermined, safety refusal) — **OPEN** until the
negatief corpus fixes the taxonomy's values.

### 4.5 Conditions

| Node | Fields | Notes |
|---|---|---|
| Comparison | `op`, `left`, `right`, `wholePeriod?`, `source` | operator set: ordering, equality, date ordering (ch. 2.2 inventory); v1 may unify date ordering under typed ordering — carried as-is in the mirror |
| Predicate | `kind` ∈ empty, filled, elfproef, exact-digits (`digits`), day-kind (`dayKind`), `operand`, `negated`, `wholePeriod?`, `source` | empty/filled are core kernel (three-valued rules in `RUNTIME-SEMANTICS.md` ch. 1.2); the other three are dialect builtins |
| CharacteristicCheck | `characteristic`, `navigation?` (to-one role), `negated`, `wholePeriod?`, `source` | **Delta (`CONSTRUCTS.md` ch. 2.2):** v0's `eigenaar` pattern-steering field (a Drools attachment hint set only on fact-creation guards) is dropped |
| RoleMembership | `factType`, `role` (the role the subject plays), `negated`, `source` | **Delta (row 5):** one existential node replaces v0's near-side (`Rollidmaatschap`, with its filler-type lookup hint) and far-side (`VerreRollidmaatschap`, with the literal `backref` bean field and the top-level-only restriction — both dropped). *Migration:* near-side → the named role; far-side → the subject-side role of the relation |
| Compound | `quantifier` ∈ all, none, the, at-least (`threshold`), at-most (`threshold`), exactly (`threshold`), `parts`, `source` | all/none/the are kernel; the counting forms are sugar (count-and-compare, row on `Kwantor`, `CONSTRUCTS.md` ch. 2.2) — mirrored as quantifiers |
| TimeWindow | `from?`, `to?`, `toInclusive`, `source` | from inclusive, to exclusive unless `toInclusive`; never both absent |
| RuleStatusCheck | `rule`, `inconsistent`, `source` | the row-30 split verdict: "is gevuurd" is a v1 non-goal, "is inconsistent" waits on the §9.5 channel — mirrored here because v0 dumps carry it; its v1 admissibility is **OPEN** with row 30 |
| Unresolved | `source` | ch. 4.4 |

The **whole-period qualifier** (`year-or-month`, `negated`) attaches to
Comparison, Predicate, and CharacteristicCheck only —
`layer: dialect(regelspraak)`, lowering onto timeline algebra (row 29;
`RUNTIME-SEMANTICS.md` ch. 3).

### 4.6 Well-formedness

- **WF-034** Every reference in an expression or condition MUST resolve:
  attributes (with their full navigation path), parameters, variables,
  characteristics, roles and fact types, aggregation element types,
  dimension names and labels, day kinds, and referenced rules. *v0 mirror
  note:* fail-soft throughout — unresolved references degrade to the
  unknown-typed sentinel or an Unresolved node, warned at best. **Delta
  (promotion):** in v1, "could not resolve" is either an explicit
  Unresolved node (honest) or ill-formed IR (a dangling reference in a
  structured node) — never a sentinel type inside an otherwise-structured
  node; the unknown datatype does not exist in v1 (ch. 2.4).
- **WF-035** Operator typing (mirror of the v0 error set): numeric
  operators' operands MUST be number-like; power operands MUST
  additionally be unitless; a date ± duration MUST use a time unit or
  declared day kind; additive operands MUST have compatible units (both
  unitless or both present and convertible — WF-010); a clamp's base and
  bounds MUST be numeric, and at least one bound MUST be present (that
  last clause is structural in v0's grammar, not a typing error —
  promoted into the rule).
- **WF-036** Comparison and predicate typing (mirror): ordering operands
  MUST be number-like with compatible units; date-ordering operands MUST
  be dates — mixed precision is deliberately *allowed* there (a v0
  asymmetry kept as found; whether v1 keeps it is **OPEN**); equality
  operands MUST share the family — same enumeration domain, same date
  precision, compatible units; an elfproef operand MUST be text or a
  whole number without unit; exact-digits MUST have a text operand and a
  whole digit count (v0 does not check positivity — mirrored; a ≥ 1
  tightening is a candidate); a day-kind predicate operand MUST be a
  date. A unitless literal adopts its sibling's unit before the check
  (mirrored convenience).
- **WF-037** Aggregation shape: facet validity per kind MUST hold —
  `emptyFallback` only on sum; `dimensionSelection` only on sum/min/max;
  `granularity` only on tijdsevenredig-deel and aantal-dagen-in (builtin
  side); an instance fold MUST carry `elementType`, a value fold MUST
  carry ≥ 2 arguments; builtin arities MUST follow the ch. 4.3 catalogue
  (one operand for the scalar and extractor kinds, two dates for
  tijdsduur, the component list for date construction). *v0 mirror note:*
  the single node makes every combination representable, and v0 even
  populates the element type on multi-operand min/max value folds whose
  first operand is an attribute reference — an artifact the v1 split
  removes; v1 makes the invalid combinations ill-formed. **Delta
  (promotion).**
- **WF-038** Aggregation operand typing (mirror): numeric folds' operands
  MUST be numeric with compatible units; date folds' (first/last)
  operands MUST be dates of equal precision; a totaal operand that is
  non-temporal MUST carry a rate unit per time unit; eerste-paasdag MUST
  take a whole, unitless year; the sub-day part extractors MUST take a
  millisecond-precision date; a sum's `emptyFallback` MUST be numeric.
- **WF-039** A written enumeration label MUST belong to the declared
  domain; a numeric literal MUST satisfy the target's sign restriction
  (mirror; also invoked from WF-023).
- **WF-040** A DimensionCellRead MUST pin every declared dimension of its
  attribute exactly once; a `dimensionSelection`'s labels MUST resolve
  against the dimension's declared order. *v0 mirror note:* the pinning
  is the node's documented contract and the labels are pre-expanded at
  build (unresolvable selections become Unresolved markers); v1 turns
  both into checks. **Delta (promotion).** (Row 28; the v0 restriction of
  selections to the subject's own single-dimension attributes is frontend
  maturity, not IR scope — noted, not normative.)
- **WF-041** A counting quantifier's `threshold` MUST be ≥ 1 (mirror of
  the v0 error).
- **WF-042** A TimeWindow MUST have at least one bound, and its bounds
  MUST be date expressions (mirror; v0 checks date-ness only — either
  precision is admitted, unchecked against the operand).
- **WF-043** A whole-period qualifier MUST sit on a time-dependent
  operand (mirror: v0 errors and emits Unresolved).
- **WF-044** (consumer conformance) A consumer MUST surface Unresolved
  nodes in its output or coverage report; silently lowering or dropping
  one is non-conforming. This is the unsolved-state harness guard
  (`PLAN.md` §8) written as a rule; its check is behavioural (CI), not
  structural.
- **WF-045** The rule-form dependency graph — over attribute,
  characteristic, channel and population nodes
  (`RUNTIME-SEMANTICS.md` ch. 6.1) — MUST be acyclic after removing the
  object-creation members of recursive rule groups: a dependency cycle
  is admissible only when it passes through recursive creation
  (ch. 6.2; mirror: v0 warns at build time and caps firing).
- **WF-046** No dependency cycle — including a WF-045-admissible one —
  may carry a negative edge (a negated or negation-quantified
  characteristic read, or a regelversie channel test): evaluation
  strata MUST exist (`RUNTIME-SEMANTICS.md` ch. 6.3). v0's single
  agenda boundary is the two-stratum special case.

---

## WF index

Format per rule: statement (in ch. 2–4), verifier check, golden case,
negatief case. `OPEN` = the artifact does not exist yet; an `OPEN` entry is
tracked debt under the spec discipline, and the Phase-1 exit requires every
row closed. "gate 3" = implemented in `spec/verifier/` and run by
`scripts/check-verifier.sh` over every golden and negatief case; a
parenthesis names the clause the v0 wire format cannot carry (that clause
stays OPEN with the reason). Negatief cases live in
`corpus/negatief/wf###-<slug>/`; the directory name declares the violated
rule and gate 3 requires the verifier to reject the case citing that rule
(incidental extra violations are allowed).

| ID | Statement (see ch. 2–4) | Verifier check | Golden | Negatief |
|---|---|---|---|---|
| WF-001 | top-level names unique per namespace (ci) | gate 3 | `jutterswet-vondsttelling` | `wf001-duplicate-parameter` |
| WF-002 | member names unique per kind within an object type (ci) | gate 3 | `jutterswet-vondsttelling` | `wf002-duplicate-kenmerk` |
| WF-003 | every model-layer name reference resolves | gate 3 (partial: sentinel datatypes, enum domains, role fillers, named timelines; a reference v0 *dropped* is invisible in the dump) | `jutterswet-penningjaar` | `wf003-unresolved-domain` |
| WF-004 | dimensioned attribute: non-empty, duplicate-free, resolving dimension list | gate 3 | OPEN | `wf004-duplicate-dimension` |
| WF-005 | timeline facet: count ≥ 1; named adoption total and consistent | gate 3 (attributes; characteristic and parameter facets are under-represented in v0 — ch. 2.10) | `jutterswet-penningjaar` | `wf005-count-zero` |
| WF-006 | enumeration domain: ≥ 1 value, values unique | gate 3 | OPEN | `wf006-duplicate-enum-value` |
| WF-007 | number precision is exactly one of whole / fixed / unbounded | gate 3 | `jutterswet-vondstnummer` | `wf007-whole-with-decimals` |
| WF-008 | enumeration compatibility = same declaring domain (use-time; expression layer) | OPEN (no corpus witness with two enum operands yet) | OPEN | OPEN |
| WF-009 | unit conversions reduce: no cycles, no unknown targets, positive rational factors | gate 3 (positivity and termination; exactness is not representable in v0's float factor) | `jutterswet-berging` | `wf009-conversion-cycle` |
| WF-010 | no restatement between non-convertible units (use-time; expression layer) | OPEN (v0 has no restatement node to anchor the check) | OPEN | OPEN |
| WF-011 | fact-type arity: reciprocal = 1 role, else ≥ 2 (>2 OPEN) | gate 3 | `jutterswet-vondsttelling` | `wf011-one-role` |
| WF-012 | role names unambiguous within a fact type (incl. plurals) | gate 3 | `jutterswet-vondsttelling` | `wf012-ambiguous-roles` |
| WF-013 | reciprocal role is to-one; other roles carry explicit cardinality | gate 3 (first clause; declared-vs-defaulted cardinality has no provenance in v0) | OPEN (needs a reciprocal fact type) | `wf013-reciprocal-many` |
| WF-014 | dimension labels non-empty, duplicate-free, ordered | gate 3 | OPEN | `wf014-empty-labels` |
| WF-015 | one timeline declaration per normalized name; references resolve | gate 3 | `jutterswet-penningjaar` | `wf015-timeline-collision` |
| WF-016 | variable-start timeline is anchored (rule-layer check) | gate 3 (existence of the determination; the anchor-write typing is WF-032/WF-023) | OPEN | `wf016-unanchored-variable-start` |
| WF-017 | day-kind spellings shadow no built-in unit and no other day-kind | gate 3 (day-kind names vs built-ins; duration-spelling collisions are pre-filtered by v0 before emission) | OPEN | `wf017-daykind-shadows-builtin` |
| WF-018 | parameter datatype resolves; timeline facet well-formed | gate 3 (datatype clause; the facet clause carries the WF-005 caveat) | `jutterswet-penningjaar` | `wf018-parameter-onbekend` |
| WF-019 | serialization preserves declaration order | `scripts/check-ir-schema.sh` (byte-identical round-trip) | `jutterswet-vondsttelling` (multi-element ordered lists round-trip) | OPEN |
| WF-020 | rule names unique across all rule forms (ci) | gate 3 (via the v0 " #i" strip-and-group migration reading) | `jutterswet-vondstnummer` | `wf020-crossform-collision` |
| WF-021 | a named form's version validity windows do not overlap | gate 3 | OPEN | `wf021-version-overlap` |
| WF-022 | rule target resolves; characteristic value is the constant true | gate 3 | `corpus/golden/jutterswet-penningdrempel/` | `wf022-target-unresolvable` |
| WF-023 | assignment typing (family, units, labels, sign, precision gate) | OPEN (needs expression-shape analysis beyond the serialized types) | OPEN | OPEN |
| WF-024 | allocation method is one of the three real methods (no COMPLEX) | gate 3 | `jutterswet-berging` | `wf024-complex` |
| WF-025 | allocation fields valid per method; ordering key orderable; share typing | gate 3 (field validity and orderability; share typing is WF-023) | `jutterswet-berging` | `wf025-max-without-ratio` |
| WF-026 | at most one allocation per written target | gate 3 | `jutterswet-berging` | `wf026-two-allocations-one-target` |
| WF-027 | object-creation members declared on the created type; characteristic init boolean | gate 3 | OPEN | `wf027-ghost-member` |
| WF-028 | fact creation fully symbolic and resolvable; path reaches target; new ≠ traversed relation | OPEN (the relational content is bean-field-encoded in v0 — the row-22 delta must land first) | OPEN | OPEN |
| WF-029 | consistency assertion has a single subject | gate 3 | `jutterswet-vondstnummer` | `wf029-no-subject` |
| WF-030 | uniqueness sources: keys resolve; arity and datatype kind agree | gate 3 | `jutterswet-vondstnummer` | `wf030-arity-mismatch` |
| WF-031 | one definition per day kind; condition present (recursion OPEN) | gate 3 | OPEN | `wf031-missing-condition` |
| WF-032 | start point: variable-start timeline, day-precision member, one per pair | gate 3 | OPEN | `wf032-fixed-start-anchor` |
| WF-033 | (dialect) rule-group members resolve; recursive group has guarded creation | gate 3 | OPEN | `wf033-unguarded-recursive` |
| WF-034 | every expression/condition reference resolves (no unknown-type sentinel) | gate 3 (partial: sentinel types and parameter/day-kind/element-type/own-member references; variable bindings and navigation paths are not serialized in v0; excepted: a v0 instance-fold source argument names a role and carries the sentinel by construction — the row-1 wart, resolved against declared roles instead) | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondsttelling` (role source) | `wf034-dangling-parameter` |
| WF-035 | operator typing (numeric, unitless power, date±time-unit, unit compatibility, clamp) | gate 3 (over serialized operand types; the date±duration clause is OPEN) | OPEN | `wf035-power-with-unit` |
| WF-036 | comparison/predicate typing (ordering, precision, family, domain, builtin operands) | gate 3 (over serialized operand types) | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondstnummer` (date ordering) | `wf036-order-number-vs-text` |
| WF-037 | aggregation/builtin shape: facet validity and arity per kind | gate 3 (fixed-arity kinds; date-construction arity OPEN) | `jutterswet-vondsttelling` | `wf037-fallback-on-minimum` |
| WF-038 | aggregation operand typing (units, precision, rate for totaal, year for paasdag) | gate 3 (paasdag, sub-day extractors, sum fallback; the totaal rate clause and fold unit-compatibility are OPEN) | OPEN | `wf038-paasdag-text` |
| WF-039 | enum label in domain; literal sign restriction | gate 3 | OPEN | `wf039-negative-under-nonneg` |
| WF-040 | dimension cell pins every dimension; selection labels resolve in order | gate 3 | OPEN | `wf040-unpinned-cell` |
| WF-041 | counting-quantifier threshold ≥ 1 | gate 3 | OPEN | `wf041-threshold-zero` |
| WF-042 | time window: ≥ 1 bound; bounds are dates, either precision | gate 3 | OPEN | `wf042-empty-window` |
| WF-043 | whole-period qualifier only on a time-dependent operand | gate 3 | OPEN | `wf043-wholeperiod-nontemporal` |
| WF-044 | (consumer) Unresolved surfaces, never silently lowered | behavioural (CI harness guard) — OPEN | OPEN | OPEN |
| WF-045 | dependency graph acyclic (creation recursion excepted) | gate 3 | `jutterswet-vondsttelling` | `wf045-cyclic-attribute-dependency` |
| WF-046 | no negative edge on a dependency cycle (strata exist) | gate 3 | OPEN — no golden carries a negative edge yet; the ch. 6.4 pair needs a re-emitted negation golden (`ref/` tests cover the accept path meanwhile) | `wf046-negated-read-in-recursive-cycle` |

Promotion summary (v0 → v1), for the migration record. **Model layer
(WF-001..019):** v0 enforces none of these as errors — that layer is
fail-soft by design (seven warning sites, zero error sites, silent
overwrite/merge elsewhere); the v1 model layer closes exactly that gap.
**Rule layer (WF-020..033):** v0 is mixed — typing, version overlap, and
several creation checks are genuine resolver errors (mirrored as WF
rules), while resolution failures are fail-soft warn-and-drop or silent
degradation (promoted). **Expression layer (WF-034..044):** the typing
rules are v0 resolver errors, mirrored; reference resolution and facet
validity are promotions (v0 degrades to sentinels or relies on the single
overloaded node); WF-044 is the unsolved-state harness guard written as a
conformance rule. Every rule above cites its v0 behaviour inline.
