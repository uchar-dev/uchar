# uchar IR — Specification

*Status: v0.9 — the model layer (ch. 2), the rule layer (ch. 3), and the
expression and condition layer (ch. 4) are normative **draft**; everything
else is skeleton. New in v0.5: the wire-entry discipline for v1 constructs
(Method) and the symbolic role bindings (ch. 2.6, 3.2, 3.4, 4.3;
WF-047/048; WF-028's check). New in v0.6: the temporal-read node
(ch. 4.3a, WF-049 — the decided row-11 point read). New in v0.7: the
characteristic prioritized defaults (ch. 6.1, WF-050/051 — the Phase-4
first cut). New in v0.8: the attribute defaults with leeg fall-through and
the initialisation default (ch. 6.1). New in v0.9: the declared-unordered
default group and the §9.5 conflict path (ch. 6.1). This document reads stand-alone (graduated into the
self-containedness gate at the Phase-1 exit, 2026-08-19): behaviour it
does not document is undocumented. The mirror's provenance — which
toolchain, and the repo relation — lives in `spec/PLAN.md` §5; the
working pins are commit hashes of that toolchain, named in the Method
below and resolvable there and in the git history. Every `§…` reference is a section number of the
RegelSpraak language specification — numbers only; no text from it is
reproduced here (construct keywords, such as the sum-fallback clause named
in ch. 4.3, excepted). This document's own sections are cited as `ch. N`.*

## Method: mirror, then diverge

- **v0** is the faithful JSON dump of the first-frontend toolchain's
  in-memory IR (`spec/PLAN.md` §5; the working pin is its commit
  `edecb46`, emitting since `446dfa6`) at a pinned commit — warts
  included.
- **v1** is what this document specifies: it deviates from v0 deliberately
  where the founding review (`IR-REVIEW.md`) flagged Drools leakage
  (bean-field names → role bindings, salience → declared stratification,
  `bron` string → structured source references, …).
- Every deviation is a tracked delta with a migration note, indexed in the
  delta column of `CONSTRUCTS.md` — never a silent rewrite. The delta list
  doubles as the Phase 0–4 roadmap.
- **Wire entry of v1 constructs (decided 2026-08-16).** A v1 construct
  enters the wire as an **additive, optional extension** of
  `regelspraak-ir-v0`: the schema admits it as an optional field or node
  (never `required`, never null — written whole or not at all), the
  loaders accept it, and the pinned v0 emitter does not produce it until
  a pin bump lands it there. Every
  extension field is marked as such at its definition and tracked in the
  `CONSTRUCTS.md` delta column like any deviation; hand-written anchor
  cases (`corpus/golden/anchor/`) are its golden witnesses until the
  emitter follows via a pin bump. The `regelspraak-ir-v1` format bump is
  reserved for the non-additive cleanup — renames, and dropping the v0
  fields the extensions supersede (`zetVeld`/`joinVeld` and kin). Current
  extension fields: `verbindendeRol` (ch. 3.2; and on object creation,
  ch. 3.3 — the back-link role), `bronRol` (ch. 4.3), the
  symbolic fact-creation fields (ch. 3.4), `navigatieBindingen` on the
  attribute reference and `navigatieBinding` on the characteristic check
  (ch. 4.1a — the navigation hops), `scopeRol` on the scoped uniqueness
  (ch. 3.6, WF-054), `bronAnnotaties` on the resolved rule (ch. 3.1,
  WF-082 — the statute annotations); current extension nodes and
  forms: `TemporalRead` (ch. 4.3a), the `prioritizedDefaults` list
  (ch. 6.1), the sum-type family — the `SumType` datatype
  (ch. 2.4) with the `Construct`/`Match`/`BinderRef` expression nodes
  (ch. 4.7) — and the fold family — the `ListType` datatype (ch. 2.4),
  the `functionDefs` top-level list (ch. 2.11) and the
  `ListLiteral`/`Lambda`/`IfThenElse`/`Invoke`/`Fold`/`MapList`/
  `FilterList`/`ListLength` expression nodes (ch. 4.8) — both v1
  kernel extensions the emitter never produces
  (`spec/proposals/PHASE5B-KERNEL.md`); and the record family — the
  `StructType` datatype (ch. 2.4) with the `StructLiteral`/`FieldAccess`
  expression nodes (ch. 4.9) — the Phase-5c kernel extension, likewise
  never emitter-produced (`spec/proposals/PHASE5C-TUTORIAL.md`); and the
  value-level binding `Let` (ch. 4.10), the same Phase-5c kernel
  extension. **`Let` and `BinderRef` are the exception to
  "emitter-free" (2026-09-01, the let-lowering card):** the RegelSpraak
  frontend lowers a §13.4.2.10 variabelendeel (`Daarbij geldt:`) to a
  `Let` chain whose reads are `BinderRef`s, so those two nodes are
  emitter-borne — with hand-written and emitted goldens both — while
  the rest of the 5b/5c kernel stays emitter-free. **First pin bump
  (2026-08-17, toolchain `edecb46`):** the
  emitter produces `verbindendeRol` and `bronRol` where uniquely
  derivable from declared names (absent otherwise — never null, never a
  guessed plural); the emitter goldens carry them since that pin. **Second pin
  bump (2026-08-18, toolchain `fe46e49`):** the emitter produces the
  navigation bindings (`navigatieBindingen` on the scalar navigated
  read, `navigatieBinding` on the navigated characteristic check —
  never on a fold source, per the ch. 4.1a emitter discipline),
  `scopeRol` on the parent-scoped uniqueness, and the symbolic
  fact-creation fields (whole-or-not per node), each only where
  uniquely derivable from declared names; `jutterswet-postwacht` is the
  emitter golden carrying all four families through both executors.
  **Third pin bump (2026-08-25):** the emitter produces `bronAnnotaties`
  on every resolved rule whose source carries `// Bron:` annotation
  comments directly above the rule header (trimmed, blank annotations
  dropped, the field omitted when none remain); `jutterswet-hardheid`
  is the emitter golden carrying one- and two-annotation rules — and an
  unannotated rule without a trace — through both executors.
  The remaining extensions stay anchor-only: `TemporalRead` and
  prioritized defaults have no surface syntax in the toolchain yet.

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
5. **Stratification** — the dependency graph, its edge kinds and the illegal-cycle rules (WF-045/046/051); `RUNTIME-SEMANTICS.md` ch. 6 is the operational companion.
6. **Defaults and defeasibility** — prioritized defaults are normative in both target kinds, with unordered groups (ch. 6.1); the v0 desugaring is `RUNTIME-SEMANTICS.md` ch. 7.7.
7. **Time** *(skeleton)*
8. **Serialization** *(lands with `spec/schema/`)*
9. **Well-formedness rules** — the WF index, at the end of this document.
10. **The RegelSpraak dialect** *(skeleton)*
11. **Versioning policy** *(skeleton)*

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

- **WF-057** Every declared name — object type, attribute,
  characteristic, plural, role, fact type, parameter, domain, unit
  system, dimension, timeline, day kind, and every form, rule-group and
  defaults-group name — MUST be **target-text-safe**: free of the double
  quote (`"`), the backslash (`\`) and control characters (below U+0020).
  Such a name lands between quotes in generated target text (the ASP
  program, DRL string literals, diagnostics), where these characters
  break the syntax; a backend-side quoting fence catches them last-line
  and one executor's refusal makes the document untestable
  differentially — this rule rejects the document itself, once, for
  every backend at once. (Decided by the maintainer 2026-08-21; the
  clingo quoting fence remains as the last-line backstop, and alone
  still covers case labels, which are case-side.)

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

- **WF-059** Every declared attribute MUST carry a datatype — the
  attribute pendant of WF-018's datatype clause for parameters. The
  wire admits `null` in the attribute map, but a datatype-less
  declaration splits the executors on otherwise verifier-clean input:
  the reference interpreter reads case input against it as a bare
  unitless number while the integer-scaling ASP translation cannot
  scale it (the "unscalable datatype" fault). This rule rejects the
  document itself; the shared case loader additionally refuses a
  value against a datatype-less declaration as the last-line
  backstop. (Decided by the maintainer 2026-08-21, on the numbered
  question the card's lens round required: the corpus carries no null
  datatype anywhere, the emitter emits one only if its model carries
  it, and should the mirror ever legitimately do so, gate 3 fails
  loudly and the tracked-delta route reopens.)

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
| `datatype` | Datatype | an Enumeration for a value-list domain, a SumType for a variant domain (extension, ch. 2.4), else a base datatype |

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
| `SumType` | `domain: string`, `constructors: list of {name, payload: Datatype?}` | **extension node** (Method; wire node `SumType`, never emitter-produced): the payload-carrying variant family, CONSTRUCTS row 36 — see the SumType clause below |
| `ListType` | `element: Datatype` | **extension node** (Method; wire node `ListType`, never emitter-produced): an ordered, possibly empty sequence type, CONSTRUCTS row 10 — see the ListType clause below |
| `StructType` | `domain: string`, `fields: list of {name, datatype: Datatype}` | **extension node** (Method; wire node `StructType`, never emitter-produced): the record datatype, Phase-5c core (`spec/proposals/PHASE5C-TUTORIAL.md` §2) — see the StructType clause below |

`sign` ∈ {`unspecified`, `negative`, `nonNegative`, `positive`} (v0
`Teken`).

**Precision forms** (v0 encoding mirrored, made explicit): a `Number` is
exactly one of — (a) *whole*: `whole = true`, `decimals = 0`; (b) *fixed
decimals*: `whole = false`, `decimals ≥ 1`; (c) *unbounded*: `whole =
false`, `decimals = 0` (no precision ceiling; §13.3.3 form (c)). The
`whole` flag is the sole discriminator between (a) and (c).

- **WF-007** A `Number` MUST satisfy one of the three precision forms;
  `decimals` < 0 is ill-formed. A source declaration of "0 decimals"
  normalizes to the whole form (v0 behaviour, kept and specified). The
  form is a property of the datatype ITSELF, not a cross against a
  declaration, so it holds at **every** `Number` in the document — since
  2026-09-04 including a rule TARGET's own `type`, which the expression
  walk deliberately never enters (the lvalue is WF-022's). Measured
  2026-09-03: `whole` with 2 decimals was verifier-clean there while the
  identical form on any walked expression type was refused. Where the
  target's attribute declaration resolves, the identity family had been
  masking the shape incidentally since WF-091 — a facet mismatch under
  WF-091's number, in a comparison message that says nothing about the
  precision form; on a CHARACTERISTIC target it defers by construction
  (no attribute declaration to compare with), and there — 54 such targets
  over 34 golden+rood documents, the anchor subset included — this check
  is the only guard OF THE NUMBER FORM: a `Tekst`-typed copy, a drifted
  enum or struct copy, or a list element on that same target stays clean
  (measured; the WF-088/WF-072 flank is open there). Measured and still
  OPEN, a second flank of the same rule: the form check does not descend
  into containers and does not reach every declaration position — the
  form check reaches none of: a `ListType` element, a `StructType` field,
  a `SumType` constructor payload, a `FunctionDef` result or parameter,
  or a lambda parameter's `datatype`. Two of those yield a verifier-CLEAN
  document today (measured, with witnesses: three ill-formed `ListType`
  elements in `jutterswet-postenheffing` and eight ill-formed struct
  fields in `jutterswet-waarderingsinvoer`, a declaration among them);
  the other four are refused on every corpus document by a neighbouring
  rule (WF-062/067/068/070/073/074/091). The
  `enum_use_site`/`struct_use_site` contrast is narrower than symmetry:
  the enum walk recurses through lists, sums and structs, the struct
  walk only through lists, and a lambda parameter's `datatype` is
  reached by neither — there WF-007 and both neighbours share the blind
  spot.
- **WF-008** Two `Enumeration` types are compatible only when they name the
  same declaring domain; value-set equality does not create compatibility.
  (v0 behaviour at use-time, lifted into the type identity rule.) This is a
  *use-time* rule recorded with the type it governs; its verifier check
  belongs to the expression layer — forward reference, **OPEN**.
- **WF-088** An `Enumeration` **use site** — every `Enumeration` datatype
  in the document other than the domain declaration itself: an attribute
  or parameter datatype, a `FunctionDef` parameter or result, an
  expression's `type`, a `Match` scrutinee's type — MUST repeat, exactly,
  the value list of the enumeration domain it names: the same values,
  compared case-insensitively (WF-001), in the declared order (which
  WF-006 makes normative). The inline `values` copy is a repetition of
  the declaration, never a second opinion — **the declaration is the
  source of truth**, and this rule is what makes that sentence
  mechanically true for the enumeration variant, as WF-061 does for the
  sum-type variant. It is load-bearing rather than tidy because the two
  rules that read enumeration values read *different* lists: WF-039
  checks a literal's label against the **local** list, WF-063 checks a
  match's arms against the **declaration**. A drifted use site therefore
  passed both — verifier-clean, yet ref faulted at evaluation ("match has
  no arm for enumeration value …") and clingo silently derived nothing.
  This is a **repeat check and nothing more**: a use site whose `domain`
  names no declared enumeration domain is silently not this rule's
  business, and the cover elsewhere is partial — WF-003 owns an
  attribute's ghost domain, WF-018 a parameter's, WF-063 a `Match`
  scrutinee's, and — since the domain identity landed — WF-089 a
  reference's (against the declaration it reads) and WF-023/WF-067 a
  written value's (against the declaration it enters), each of which
  refuses a ghost domain on the spot because the identity with a declared
  domain fails. What stays **unenforced** is the residue those crossings
  cannot see: a keuzetype `type` naming a ghost domain where it touches no
  declaration at all — a literal compared with another such literal —
  which is measured harmless (both executors read the node's own inline
  copy and agree; a `Match` over it is refused by WF-063, which resolves
  the *named* domain). The other half — that this rule binds a use site to
  the domain it *names*, never to the declaration it *reads from* — is
  WF-089 below.
  *v0 mirror note:* unchecked in v0. **Delta (promotion).**
- **WF-089** A **use site that takes a keuzetype value from somewhere**
  whose `type` names a keuzetype domain — an `Enumeration` or `SumType`
  domain, looked up *through* a `ListType`'s element — MUST name the
  **same keuzetype** as the source it takes that value from: the same
  variant *and* the same domain name, compared ci (WF-001); and
  conversely a use site taking a keuzetype value MUST name that
  keuzetype. There are two such use sites. A **reference expression**
  takes its value from the **declaration it reads**. An
  **element-passing aggregation** — `EERSTE`, `LAATSTE`, `MINIMUM`,
  `MAXIMUM`, the folds that select a contributor instead of computing a
  new value — takes its value from **each of its operands**, and its own
  `type` MUST name that operand's keuzetype; the operand is in turn
  anchored by this rule or by a structural type equality, so the fold
  chains to a declaration rather than laundering a domain. (`SOM`,
  `TOTAAL` and `AANTAL` compute a number; no keuzetype travels through
  them.) The variant
  travels with the name because `domeinen` is **one namespace** — a domain
  is declared as an enumeration or as a sum type, never both, so an
  `Enumeration`-typed reference over a sum-typed declaration of the same
  name (or the mirror) is a lie whatever the spelling agrees on. This is
  the **domain identity** rule, read half: the domain extension of the
  soort cross WF-023/WF-027/WF-078 perform, and the other half of
  WF-088/WF-061 — those bind a use site to the domain it *names*, this
  binds it to the declaration it *reads*. Its **write half** — every value
  entering such a declaration names the same keuzetype — is WF-023 (rule
  target, object-creation member, defaults consequence) and WF-067 (a
  function body against its declared result); read and write half together
  are one maintainer decision (2026-09-02), and neither is sufficient
  alone. The declaration source per reference shape: for
  an `AttributeReference` — and the `DimensionCellRead` over one, whose
  cell carries the attribute's datatype — the attribute at the
  reference's endpoint type (ch. 4.1a: a navigated read crosses against
  the attribute it lands on); for a `ParameterReference` the parameter;
  for a `BinderRef` the binder's declared datatype — a match arm's
  declared payload, a lambda or `FunctionDef` parameter's declared type,
  a `Let` value's type, a filter binder's element type (the WF-078
  environment). A **fold source** — the `AttributeReference` an
  aggregation with an `elementObjecttype` reads each contributor through
  — is an `AttributeReference` like any other and crosses against the
  attribute it reads; it is called out because its shape is checked by
  the fold-source rules (WF-034/WF-053/WF-080/WF-083) rather than by the
  ordinary expression walk, and that routing, not the rule, once left it
  uncrossed (`corpus/negatief/wf089-foldbron-enum`). Three reference
  shapes need no arm of their own because
  an existing rule already crosses their full type *structurally* against
  the declaration: a `TemporalRead`'s type equals the timeline's element
  datatype (WF-049), an `Invoke`'s type and argument types equal the
  declared result and parameters (WF-068), a `Lambda` parameter equals
  the source element (WF-070) — and that source is a reference, anchored
  here; a `Construct` reads no declaration (it names its own domain,
  WF-062). The v0 unknown sentinel on either side, an unresolvable
  reference (a role-name fold source, a characteristic) and a reference
  carrying no `type` defer, as under WF-023/WF-078. *Motivation
  (measured, the card):* without it a reference typed with a second,
  internally consistent domain is verifier-clean — WF-063 checks the arms
  against the domain the scrutinee *names* — and the executors split
  three ways: ref faults ("match has no arm …"), the ASP backend derives
  nothing (a silent leeg for the wrong reason) and, on a list-typed
  reference whose element domain drifts, folds a **wrong answer** (0
  where 35.00 was expected); the one-sided lie (a reference typed number
  over an enumeration attribute) had ref write the label into a numeric
  attribute, clingo fault the translation and Drools crash at fire; the
  variant lie (an enumeration-typed reference over a sum-typed
  declaration of the same name) faulted both executors in different
  words. With the read half **and** the write half — the second measured
  necessary, not assumed: with the read half alone a `Literal` or
  `Construct` of another domain, a ghost-domain literal and a list of
  another domain still entered the read declaration verifier-clean at a
  rule target, an object-creation member and a defaults consequence, and a
  drifted function body did the same through a truthful `Invoke`; and the
  **reference** positions alone were insufficient in turn (fix round 2):
  with every reference and every write crossed, an element-passing fold
  whose own `type` named another domain than its truthfully typed operand
  still handed an alien label on — with a single contributor the fold
  makes no comparison at all, so nothing looks at the value — and so did a
  fold source, whose shape check the expression walk hands off to. Both
  reached the same "no arm" fault in ref, on documents whose only defect
  was the claim — with the aggregation position too, ref's "no arm" fault
  is unreachable on a verifier-clean document
  (`ref/lib/eval.ml`, both `Match` arms) and an underived `matchN` in the
  ASP encoding means a leeg scrutinee and nothing else
  (`ASP-TRANSLATION.md` S14). One route stays outside the WF layer and is
  named rather than claimed away: the **case input**, where the shared
  loader — not a WF rule — refuses a label or constructor the declaration
  does not carry. **The declaration side needs no rule** —
  measured, not assumed: a keuzetype nested *inside* a declaration is
  fenced out before it can drift — an `Enumeration` as a sum payload
  (WF-060), as a struct field (WF-071) or as a list element (WF-065) is
  ill-formed. The one nested form those fences admit, a declared
  `SumType` repeated as a payload or field inside `domeinen`, is
  verifier-clean when its inline copy drifts (WF-061 walks attribute and
  parameter datatypes only) and is recorded as an open flank rather than
  pinned here: every verifier rule that consumes it resolves the *named*
  domain, never the inline copy. *One neighbouring gap, measured and
  recorded rather than claimed shut.* **Orderability of the
  element-passing folds.** This rule makes the fold's domain honest, not
  its kind admissible: `EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM` are
  *ordering* folds in v0 (ch. 4.3), and Enumeration and SumType are
  equality-only by the orderability paragraph below — so an entirely
  truthful `EERSTE` over an enumeration attribute is verifier-clean and
  faults in ref (`unordered comparison: Zeegat vs Wad`) as soon as a
  second contributor exists, while a single contributor silently
  succeeds. That is a WF-038 operand question, not a domain-identity one.
  The **struct twin** of this rule is no longer a gap: struct domains are
  not keuzetypes (the family term above is Enumeration and SumType), and
  they carry their own identity rule, **WF-090** (the StructType clause
  below), landed 2026-09-03 over the same use sites and the same three
  write positions. **Delta (promotion).**

Orderability: `Number` and `Date` are orderable; `Text`, `Boolean`,
`Enumeration`, `SumType`, `ListType`, and `StructType` are equality-only.
This is deliberate (an
ordered use of a non-orderable type is a rule-layer WF violation,
specified with that layer).

**The keuzetype family (spec structure; maintainer decision
2026-09-01).** `Enumeration` and `SumType` are two wire forms of one
idea, and the spec names that idea once. A **keuzetype** is a declared,
closed set of labelled alternatives: it is hosted by a domain (ch. 2.3),
each alternative carries a label unique (ci) within the declaration and
at most one payload datatype, and wherever a keuzetype value is taken
apart the alternatives are covered exhaustively and literally — there
are no wildcards (`Match`, ch. 4.7, WF-063 over both variants). Naming
the family is what lets one rule govern both shapes; the family itself
is **not a wire node** — no document ever serializes an abstract
keuzetype, and every keuzetype on the wire is exactly one of these two
concrete variants:

| Variant | Alternatives | Leeg | Wire node |
|---|---|---|---|
| enumeration | all **nullary**: a label and nothing else | an enumeration-typed slot **can hold leeg** — it is an ordinary v0 value slot under the ch. 1.2 empty regime, the §5.6 Tabel-44 row listing enum among every datatype | `Enumeration` (the v0 mirror form, byte-unchanged) |
| sum type | **payload-carrying**: at least one alternative carries a payload datatype | a constructor value is gevuld by construction and reaches leeg only by **propagation** (`RUNTIME-SEMANTICS.md` ch. 9.1); optionality is an explicit variant, never an implicit third state | `SumType` (v1 extension, CONSTRUCTS row 36) |

The leeg difference is a *parameter* of the family, stated here rather
than hidden: the two variants differ in how a value can be absent, and
that is why the abstraction stops at declaration shape and totality and
does not reach into the value model.

Two rules make the split total, and both are one-directional:

- **Nullary lives in one form only** — WF-087 below. A keuzetype all of
  whose alternatives are nullary is an `Enumeration` domain; the
  all-nullary `SumType` is ill-formed on the wire and normalises to it.
  Nullary-as-sum is sugar, the enumeration is the kernel, so two
  frontends cannot express one meaning in two shapes.
- **Payload never converts.** A keuzetype with even one payload-carrying
  alternative is a `SumType` and stays one: no `Enumeration` gains a
  payload, and no payload-carrying declaration is rewritten to an
  enumeration. The two variants are distinct type identities across the
  split as well — enumeration compatibility is nominal per domain
  (WF-008) and so is sum-type identity (WF-061); a value of one is never
  a value of the other.

**SumType (extension; CONSTRUCTS row 36, design
`spec/proposals/PHASE5B-KERNEL.md` §2).** A declared, nominal variant
family: each constructor carries a `name` and either no payload
(`payload` absent — nullary) or exactly one payload datatype (unary;
the ratified single-payload decision — n-ary payloads are the 5c
struct territory and are ill-formed here). A `SumType` is declared as a
domain (ch. 2.3) and repeated whole at every use site, exactly as
`Enumeration` repeats `values`. It is the payload-carrying variant of
the keuzetype family above: `Enumeration` stays the payload-less v0
mirror form, payload never converts, and the nullary shape belongs to
the enumeration alone (WF-087).

- **WF-060** A `SumType` declaration MUST carry at least one
  constructor; constructor names MUST be unique (ci) within the
  declaration; every payload datatype MUST be one of `Number`,
  day-precision `Date`, or a declared `SumType` (text, boolean,
  enumeration, millisecond-date and nested-sum-beyond-declared payloads
  are ill-formed — the admissible set is a recorded v1 fence, loudly
  extensible); and the declaration graph over sum-type payloads MUST be
  acyclic (no recursive sum types — Catala has none, and acyclicity is
  what statically bounds constructor-term depth for the ASP encoding).
- **WF-087** A `SumType` declaration whose constructors ALL carry no
  payload is ill-formed: the nullary keuzetype has one wire form, and it
  is the `Enumeration` domain. Such a declaration MUST be normalised to
  an `Enumeration` domain carrying the constructor names as its
  `values`, with its use sites; a `Match` over it stays well-formed
  under the enumeration arm of WF-063 (ch. 4.7), so nothing is lost in
  the move. The rule is stated over the declaration and needs no
  use-site arm: WF-061 forces every use site to agree with its declaring
  domain. It starts at **one** constructor and stops below it: a
  declaration with *no* constructor at all is WF-060's terrain (it has no
  enumeration to normalise to), so WF-087 leaves the empty case alone
  rather than reporting both. Its converse is deliberately absent — an `Enumeration` is
  never rewritten to a `SumType`, and a payload-carrying declaration is
  never rewritten at all. **Delta (v1 normalisation).**
- **WF-061** A `SumType` use site MUST name a declared domain whose
  datatype is a `SumType` that agrees with the use site (same
  constructors, same payload datatypes, same order); and two sum-typed
  operands of one comparison MUST name the same declaring domain — the
  nominal-identity rule, the sum-type sibling of WF-008 (which stays
  stated over `Enumeration` and keeps its own OPEN).

A recorded flank, deliberately not a WF clause: the ASP backend carries
each constructor name as a *functor symbol*, so a name that does not
normalise to a valid symbolic constant (`[a-z][a-z0-9_]*` — e.g. one
carrying spaces) is a loud executor fence inventoried in
`ASP-TRANSLATION.md` S19, not a well-formedness violation — the WF-057
precedent fences only the string encoding, never the functor charset.

**ListType (extension; CONSTRUCTS row 10, design
`spec/proposals/PHASE5B-KERNEL.md` §3).** An ordered, possibly empty
sequence of one element datatype. The empty list is a *value*; leeg
remains the absence of one (`RUNTIME-SEMANTICS.md` ch. 10.1). List
values are equality-only (elementwise, in order) and never orderable;
they are built by `ListLiteral` (ch. 4.8) or supplied by a case, and
consumed by the fold family.

- **WF-065** A `ListType` element datatype MUST be first-order and
  non-list: one of `Number`, day-precision `Date`, a declared
  `SumType`, or — the **Phase-5c widening** — a declared `StructType`
  (`list of Individual` is the tutorial's input shape; the fence was
  written loudly extensible for exactly this). Lists of lists, text,
  boolean, enumeration, function and
  millisecond-date elements stay ill-formed — the admissible set of the
  ratified design, loudly extensible. A list-typed attribute MUST be
  scalar-carried: a granularity declaration on it (a list-typed
  temporal attribute) is ill-formed in v1 — a recorded fence, not a
  design position.

**StructType (extension; Phase-5c core, design
`spec/proposals/PHASE5C-TUTORIAL.md` §2).** A declared, nominal record: a
finite, ordered list of named `fields`, each carrying a datatype. A
`StructType` is declared as a domain (ch. 2.3) and repeated whole at
every use site, exactly as `SumType` repeats its constructors — the same
landing pattern (extension node, hosted by `domeinen`, never
emitter-produced). It is a value type: records are anonymous values
compared by value, never entities with identity (the rejected
object-type lowering, `PHASE5C-TUTORIAL.md` §2). Its expression nodes —
`StructLiteral` and `FieldAccess` — are ch. 4.9; the record value and its
no-hole/equality semantics are `RUNTIME-SEMANTICS.md` ch. 11.

- **WF-071** A `StructType` declaration MUST carry at least one field;
  field names MUST be unique (ci) within the declaration; every field
  datatype MUST be one of `Number`, day-precision `Date`, a declared
  `SumType`, or a declared `StructType` (text, boolean, enumeration,
  millisecond-date and list fields are ill-formed — the admissible set
  is a recorded v1 fence, loudly extensible); and the declaration graph
  MUST be acyclic — no recursive structs. This graph is the **same**
  acyclicity graph as WF-060's: it spans sum-type payloads and struct
  fields together, so a struct nesting a sum type nesting the struct is
  caught, and it is what statically bounds record-term depth for the ASP
  encoding.
- **WF-072** A `StructType` use site MUST name a declared domain whose
  datatype is a `StructType` that agrees with the use site (same fields,
  same field datatypes, same order); and two struct-typed operands of
  one comparison MUST name the same declaring domain — the
  nominal-identity rule, the struct sibling of WF-061 (fieldwise
  structural equality within one domain; cross-domain comparison is
  statically ill-formed, never false, ch. 11). "Use site" means every
  `StructType` in the **model layer and the expression layer**, at every
  depth: an
  attribute or parameter datatype, a `ListType` element inside one, a
  `FunctionDef` parameter or result, a struct-typed **field of another
  struct declaration** in `domeinen` (WF-071 admits declared domains
  there, so that field is a use site of *another* declaration), and
  every expression node's `type` — the "every datatype in the document"
  split WF-088 uses for enumerations. *Why the whole wire and not just
  the top-level declaration positions (measured, 2026-09-03):* the
  inline field list is not decoration, it is what the consumers read.
  The reference interpreter's shared case loader crosses a record
  against the inline `velden` of the use site, and the ASP backend
  encodes struct terms **positionally** out of it. Until the walk
  widened, a drifted copy one `ListType` deep left the document
  verifier-clean and let through a case input the loader admitted
  *against that copy*, on which ref then faulted at the field read while
  clingo passed green; and a single expression `type` carrying the
  declared fields in **reversed order** left verifier, ref and schema
  clean while clingo
  derived 5.03 where 125.50 was expected — a **wrong answer**. Three
  positions are deliberately outside the walk, each because no negatief
  can isolate it: a matched struct's own `velden` (the agreement test is
  an equality on the whole field datatype, so a drifted nested copy
  already makes the enclosing copy disagree — the argument WF-073/074
  make for their nested forms), a sum-type payload (WF-060 admits no
  struct payload in a declaration at all, so such a payload can only sit
  in a drifted inline sum copy — WF-061's recorded open flank, not
  this rule's), and — exactly as WF-088 records for enumerations — a
  rule's TARGET `type`, which is WF-022's lvalue rather than an
  expression node, so the expression walk never enters it. That third
  position is real: five goldens and two rood documents carry a
  struct-typed `doel` `type`, and drift there is verifier-clean and
  **measured harmless** (reverse the fields of
  `jutterswet-dossierspelling`'s `/resolvedRegels/1/doel/type`, or
  rename them to `bedrag`/`aantal`, and both variants stay schema-valid,
  verifier-clean and replay their three pinned expectations in ref AND
  clingo — both write through the target attribute's DECLARED datatype,
  and a value that shares the target type's lie is refused by WF-090's
  write half, which crosses that same declared datatype).
- **WF-090** A **struct-typed use site** — one whose `type` names a
  declared struct domain, looked up *through* a `ListType`'s element —
  MUST name the **same struct domain** (ci, WF-001) as the declaration it
  crosses; and conversely a use site taking a struct value MUST name that
  struct domain. This is **struct-domain identity**, and it has the same
  two halves the keuzetype family has under WF-089 and WF-023's domain
  clause, over the same positions. **Read half:** a reference expression
  names the struct domain of the declaration it reads from — an
  `AttributeReference` (at the reference's endpoint type, so a navigated
  read crosses against the attribute it lands on) and the
  `DimensionCellRead` over one, a `ParameterReference`, a `BinderRef`
  (a match arm's payload, a lambda or `FunctionDef` parameter, a `Let`
  value, a filter element), a **fold source**, and an **element-passing
  aggregation** (`EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM`) against each of
  its operands, which are anchored in turn so the fold cannot launder a
  domain. **Write half:** every struct value entering such a declaration
  names its domain — a rule target, an object-creation attribute member,
  a defaults consequence (recursing into exception groups), the three
  positions WF-023 owns. Struct domains are **not** keuzetypes — the
  ch. 2.4 family term covers `Enumeration` and `SumType` only — so this is
  its own rule rather than a widening of that term (maintainer decision
  2026-09-03); the verifier machinery is shared with WF-089/WF-023, the
  rule number is not. It needs no arm where an existing rule already
  crosses the FULL datatype: a `FunctionDef` body against its declared
  result is WF-067, a `TemporalRead` WF-049, an `Invoke` WF-068, a
  `Lambda` parameter WF-070, a `StructLiteral`'s field value WF-073 and a
  `FieldAccess`'s own `type` WF-074 — each an equality on the whole
  datatype, domain included. The v0 unknown sentinel on either side, an
  unresolvable reference and a use site carrying no `type` defer, as under
  WF-023/WF-078/WF-089. *Motivation (measured, the card):* WF-072 binds a
  use site to the declaration it NAMES, WF-073 a literal to the domain IT
  names and WF-074 a field read to the domain the BASE names — none of
  them looked at the declaration a struct value comes from or enters, and
  ten internally consistent documents were verifier-clean while the
  executors split: a `StructLiteral` of domain B written into an attribute
  of domain A had ref fault at the field read and the ASP backend leave
  the target underived; at an object-creation member the ASP side reported
  a full green pass on a document ref cannot evaluate; and through a
  drifted list element or a drifted binder the ASP backend folded a
  **wrong answer** (0 where 12.50 was expected). The declaration side
  needs no rule of its own: a struct nested inside a declaration is
  fenced (WF-071 admits only declared domains as field datatypes,
  WF-065 as list elements) and WF-072 — since the walk widened the same
  day — pins every `StructType` in the model and expression layers
  against its declaration, including the nested and expression-level
  copies that carry the field list the executors actually read (a rule
  TARGET's `type` stays outside that walk, as it does under WF-088; drift
  there is measured harmless, and a value sharing the lie is refused by
  this rule's write half, which crosses the DECLARED datatype —
  measured). Two neighbouring gaps are recorded
  rather than claimed shut. First, WF-090's aggregation arm reaches the
  **element-passing** folds only, so a COMPUTING fold (`SOM`, `TOTAAL`)
  over struct operands of two domains is verifier-clean. That is a
  WF-038 operand question — `SOM` demands numeric operands — and it is
  no wrong answer: measured, ref faults fail-fast (`plus: expected a
  number, got {waarde: 120.5, telling: 3}`) and the ASP carrier refuses
  the form ahead of translation. Second, the same **orderability** flank
  WF-089 records: `EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM` are ordering
  folds in v0 while `StructType` is equality-only (the orderability
  paragraph above), so an entirely truthful struct `EERSTE` with two
  contributors is verifier-clean and fails observably — measured, ref
  `EVALUATION FAULT: unordered comparison: {waarde: 20, telling: 2} vs
  {waarde: 10, telling: 1}` and the ASP carrier refuses the form. Also a
  WF-038 operand question, not a domain-identity one. *v0 mirror note:* `StructType` is a v1
  extension; unchecked in v0. **Delta (promotion).**

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

**The wire form of a unit.** The v0 dump spells the four scalar fields
`afkorting`, `stelsel`, `canoniek` and `factorNaarCanoniek`, plus a nested
`noemer`; the three NAMES, the factor **and the `noemer` recursively** are
exactly what WF-091 (ch. 4.1) compares BYTE for byte when it crosses one unit
against another, which makes their spelling load-bearing rather than
decorative. That rule compares two units and can say nothing about ONE: it
passes the moment both sides carry the same spelling, however meaningless.
The form law that holds of a unit on its own is **WF-092** below — the
conversion pair, `canoniek` and `factorNaarCanoniek`, is one fact in two
fields and the schema lets each be null independently. Of the four
combinations **two** are units and two are not: both filled is the ordinary
declared unit; both null is the **bare** form; either half ALONE is not a
unit at all.

The bare form covers the two shapes that legitimately have no conversion pair
to spell. One is the self-standing declared unit — 28 occurrences over the
golden corpus, the WF-038 duration parameters and WF-035 duration operands,
every one of them also naming no system. The other is the opaque **product**
unit of point 4 below, for which the field table above already records
`canonical` as absent.

Bare does not mean inert, and a bare unit is not one that converts into
itself and nothing else. Convertibility reads a missing `canoniek` as the
ABBREVIATION, so a bare unit is a canonical TARGET for every declared unit
whose canonical string spells that abbreviation, and a quantity crossing into
it restates on THAT unit's factor. Measured on a `jutterswet-vergunningsduur`
variant with all five `maand` records bared: `2 jaar + 3 maand` still yields
27 with the verifier clean and the reference interpreter and the ASP backend
agreeing, and with `jaar`'s factor moved from 12 to 6 both yield 15 — the
bare unit is what the 6 counts against. Reading its own factor as 1 is
therefore correct, and is the only reading that keeps that arithmetic exact.

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
- **WF-092** A unit MUST spell its conversion pair whole: `canonical` and
  `factorToCanonical` (v0 `canoniek`, `factorNaarCanoniek`) are filled
  together or null together, on the numerator and, recursively, on the
  `denominator` of a rate. The bare form (both null) is admissible and
  unchanged; either HALF is ill-formed — a canonical target with no factor
  states a conversion it cannot perform, and a factor with no canonical
  target names no conversion at all. This is a SHAPE law on every unit in
  the document, not a cross between two of them, so it holds at every
  position a unit can occupy — the declared systems, the model-layer
  datatypes, every expression `type`, **a fold source's own `type` and an
  inline lambda's parameter datatypes** (neither is reached by the
  expression walk: the fold-source branch replaces that walk, and a lambda's
  parameters are declared where only its body is walked), and a rule
  target's own `type` — and a document carrying the shape is refused once
  per position. No neighbouring rule reaches it: WF-009 tests a factor it
  FINDS (positivity) and a chain it can FOLLOW, and a missing half is
  nothing to look at; WF-035/WF-023 cross units for CONVERTIBILITY, which
  reads the canonical names and never the factor, and reads a missing
  canonical as the abbreviation; WF-091 compares byte for byte and agrees
  the moment the half-form is copied consistently. Where both rules speak at
  one position their REPORT ORDER differs — WF-092 precedes WF-091 on a
  walked expression type and follows it on a rule target, because the two
  clauses sit on opposite sides of the identity cross in each. Measured, and
  harmless: a report is a set of citations, and both are always present.
  *v0 mirror note (fail-open):* v0 reads a missing factor as
  1 — the same fail-open the *Restatement* note above records for
  non-convertible units — so the half-form computes silently and its
  wrongness surfaces, if at all, as some later complaint about the
  resulting number rather than about its unit. The shared v0 Java unit model
  no longer does so on the canonical-without-factor half: since the
  fail-fast card of 2026-09-04 the factor question there THROWS by name
  (measured: `onbekende omrekenfactor van 'm' (1) naar 'km' (null)`). The
  mirror half still computes silently in that model, through the ungated
  predicate callers that read convertibility off the canonical name alone.
  **Delta (promotion).**

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

A **RoleBinding** (wire node `RolBinding`; an extension node per the
Method) is the symbolic reference those chapters bind with:

| Field | Type | Meaning |
|---|---|---|
| `factType` | string | v0 wire `feittype`: a declared fact type, by name |
| `role` | string | v0 wire `rol`: one of its roles, by **declared role name** — the plural is presentation and does not bind |

- **WF-047** A role binding MUST resolve: the fact type is declared, the
  role names exactly one of its roles (case-insensitive over the declared
  name), and the fact type has exactly two roles — or exactly **one**,
  reciprocal (WF-011/013), in which case the bound and the opposite role
  are that same role: both sides fill the one declared type. At each
  use site the bound role's filler type and the opposite role's filler
  type MUST agree with the types the node declares (the site chapters
  state which side is bound: ch. 3.2, 4.3), and where the v0 form also
  carries a written role name (the row-1 fold source), that name MUST
  denote the bound role.
- **WF-048** A node that navigates the connecting relation *without* a
  binding — a bindingless allocation (ch. 3.2) or instance fold
  (ch. 4.3) — is well-formed only if **exactly one** fact type relates
  its type pair (a two-role fact type over the pair, or — for a
  self-typed pair — a reciprocal one): the ch. 4.1 runtime derivation
  fault of
  `RUNTIME-SEMANTICS.md` lifted to well-formedness. Every emitted v0
  document satisfies this (the v0 resolver enforces the same unique
  derivability at build time). In v1 the binding becomes required and
  WF-048 retires with the format bump.

Role-name matching is case-insensitive, over the declared name and the
declared plural. *v0 mirror note:* when no plural is declared, v0 admits a
best-effort synthesized plural (`+s`/`+en`) at use sites; a declared
plural always wins over a synthesized one. **Decided (2026-08-25):** v1
keeps NO best-effort plural matching — morphology is not semantics and
belongs in the frontend, if anywhere; the `+s`/`+en` synthesis is a
tracked, unmirrored delta. What that admission actually bought at v0's
use site is narrowed to the deterministic **type-pair fallback** of the
instance-fold source — the row-1 wart, and there only (ch. 4.3,
WF-034): name resolution first (declared name or
declared plural, ci); a name that resolves to no declared role stands
only when exactly one fact type connects the subject type to the
element type — no spelling guess, and zero or multiple connecting fact
types keep the loud rejection.

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

### 2.11 FunctionDef — named reusable logic

`layer: core`; an **extension form** per the Method: an optional
top-level list `functionDefs`, never emitter-produced (CONSTRUCTS
row 10 — this *is* DMN's Business Knowledge Model: named logic, bound
and invoked by name; design `spec/proposals/PHASE5B-KERNEL.md` §3).

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name (own namespace, ci-unique) |
| `parameters` | list of `{name, datatype}` | typed, uniquely named |
| `result` | Datatype | the declared result datatype |
| `body` | Expr | the closed body |

- **WF-067** Function definitions MUST carry ci-unique names; every
  parameter MUST carry a datatype and a name unique within the
  definition; the `result` MUST be a declared-resolvable datatype; the
  **body is closed** — it may reference its own parameters
  (`BinderRef`), document parameters (`ParameterRef`), literals,
  operators, `Construct`/`Match`/`IfThenElse`, `StructLiteral`/
  `FieldAccess` (the **Phase-5c widening** — a record built or read over
  binders and parameters keeps the body closed), `Let` (a call-by-need binding
  over closed operands stays closed, ch. 4.10), and other functions via
  `Invoke`; any attribute, characteristic, aggregation, temporal or
  variable read is ill-formed; the **body's static type MUST equal the
  declared `result`** — full datatype equality, soort and keuzetype domain
  alike, as WF-068/069/070 cross their operands (the body is the write
  into the result every `Invoke` reads: the write half of ch. 2.4's domain
  identity for functions, where WF-068 is the read half; the v0 unknown
  sentinel defers, as under WF-023) — and the invocation graph over
  `functionDefs` MUST be acyclic (no recursion: the Catala mirror, and
  what keeps termination and stratification trivial — a closed body
  contributes no dependency edges, so WF-045/046 are untouched).

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
| `statuteAnnotations` | list of string? | **extension field** (v0 wire `bronAnnotaties`): the `// Bron:` statute annotations written directly above the rule header, the text after `Bron:` trimmed, in source order — the PLAN §4 traceability thread from a rule back to its wetsartikel. Provenance only, never evaluation-relevant: executors ignore it. When present the list is non-empty with non-empty trimmed entries; a rule without annotations omits the field — written whole or not at all (WF-082). *v0 mirror note:* v0 flattens versions into separate `ResolvedRegel` entries, so the wire carries the field per flattened entry, stamped from the one comment block above the shared rule header |

**RuleVersion**:

| Field | Type | Meaning |
|---|---|---|
| `validity` | Validity | which evaluation dates this version governs (ch. 3.10) |
| `target` | attribute reference | the written member: an attribute, or a characteristic |
| `value` | expression | the computed value; for a characteristic target: the boolean constant true — v0 mirror, the emitter's only form (Phase 4 generalises this into the default model; the ch. 3.3 initialisation form now generalises the characteristic value to a boolean-typed expression, WF-027 — the v1 relaxation the Catala frontend uses) |
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
- **WF-023** Assignment typing, at **every attribute write position** — a
  rule version's `target`, an `ObjectCreation`'s attribute `member` (a
  characteristic member's value is WF-027's boolean check), and a
  `PrioritizedDefaults` **consequence**, a nested exception group's
  included (it inherits the root target, ch. 7.9): the value's type MUST
  be assignable to the target's **declared** type — same family,
  compatible units, sign constraint respected, **the same keuzetype**
  wherever either side names one (an `Enumeration` or `SumType` domain,
  looked up *through* a `ListType` element — the same variant and the same
  domain name, compared ci), and the precision gate of
  `RUNTIME-SEMANTICS.md` ch. 2.4 (including its candidate rule: a possibly
  non-terminating value passes explicit rounding before a bounded write).
  The keuzetype clause is the **write half** of the domain identity WF-089
  states for the read side: WF-089 crosses what a reference *reads*, this
  crosses what is *written* into the declaration it reads. It is also what
  makes "labels from the target's domain" mechanically true — a `Literal`'s
  label belongs to the domain the literal itself names (WF-039 against the
  local list, WF-088 for that list against the declaration), so once the
  value's domain **is** the target's, its labels are the target's. The
  cross uses the target's DECLARED datatype, never its serialized `type`:
  a matching pair of lies — target and value both typed with the same
  foreign domain — is exactly the shape a serialized-against-serialized
  slice admits. Deferrals, as elsewhere: the v0 unknown sentinel on either
  side (WF-034 refuses that sentinel at all three positions anyway —
  measured), a target that does not resolve (WF-022), a value carrying no
  `type`. The precision gate needs expression-shape analysis and stays
  OPEN — a different question from the READ-side precision facet, which is
  WF-091's (ch. 4.1) and closed: that one crosses a reference's annotated
  decimals against its own declaration, this one a computed value against
  its target's. The **struct twin** of the keuzetype clause is CLOSED (2026-09-03):
  `domeinen` declares struct domains too, and a `StructLiteral` of domain B
  assigned to an attribute of domain A used to be verifier-clean — ref
  faulted at the field read and the ASP backend gave a wrong answer. That
  is **WF-090** (ch. 2.4), which rides these same three write positions
  under its own rule number: the keuzetype clause here stays
  keuzetype-scoped and the struct clause is stated once, beside the struct
  rules. *v0 mirror
  note:* enforced as resolver errors via the §6/Tabel 61
  typing tables — except for characteristic attribution, where v0 skips
  the check (harmless today: the value is a constructed constant); the
  object-creation member and the defaults consequence carry no value
  typing in v0 at all. **Delta (promotion)** for those two positions and
  for the keuzetype clause.
- **WF-079** A (type, attribute) target that carries no defaults MUST have
  at most one **unconditional, version-free** computing rule
  (GELIJKSTELLING/BEREKENING). Two or more over-determine the target with
  no explicit priority — the single-writer discipline of WF-026/WF-050/
  WF-052 extended to bare attribute writers (`RUNTIME-SEMANTICS.md`
  ch. 7.7: the desugaring admits at most one version-free computing rule
  per target). Priority is a defaults group (explicit defaults) or `ongeordend`
  (explicitly absent), never implicit agenda order. Characteristic
  attributions (KENMERKTOEKENNING) are exempt — a monotone
  many-grounds-one-truth — and a guarded case split (each writer carrying
  a `condition`) carries its own priority and stays outside the rule; only
  the bare collision is statically over-determined. *Motivation:* the
  `PHASE5D-DIFF` §8.2 witness — the flat form is un-pinnable in every
  zone (ref oscillates and faults the fixpoint budget, clingo is silently
  multi-valued, so the executors disagree and no corpus zone admits it);
  WF-079 refuses it ahead of the differential square. **Delta
  (promotion).**
- **WF-082** The `bronAnnotaties` extension field, when present, MUST be
  a non-empty list whose entries are non-empty and carry no leading or
  trailing whitespace; a rule without annotations MUST omit the field
  (absence leaves no trace — an empty or blank form is ill-formed). The
  emitter satisfies this by trimming the text after `// Bron:` and
  dropping blank annotations. *v0 mirror note:* upstream builds the
  annotation list in Phase 0 but the pinned v0 emitter did not put it on
  the wire; the field enters as an extension (Method) with the
  2026-08-25 pin bump.

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
| `connectingRole` | RoleBinding? | **extension field** (v0 wire `verbindendeRol`): the connecting relation, with the **recipient role bound** — the bound role's filler is `targetType`, the opposite role's filler is `sourceType` (WF-047). Absent, the relation must be uniquely derivable from the type pair (WF-048); `RUNTIME-SEMANTICS.md` ch. 4.1 |

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
| `verbindendeRol` | RoleBinding? (extension) | the created-side back-link role, named explicitly (see below) |

**MemberInit**: `member` (string), `value` (expression), `characteristic`
(boolean — v0 `kenmerk`: true = §13.4.6.4 characteristic-value form, the
member names a declared characteristic and the value is a boolean-typed
expression — a boolean constant is the v0 form; false = §13.4.6.3
attribute-value form).

The created object's **back-link** relation may be named explicitly
through the extension field `verbindendeRol` (a RoleBinding per the
Method, binding the **created-side** role; WF-047's filler clauses
apply — bound = createdType, opposite = subject). Without the binding
the relation is derived as the unique fact type relating the pair
(`RUNTIME-SEMANTICS.md` ch. 5.5); a **self-typed** two-role pair is
role-ambiguous there — the pinned runtime derives null and drops the
link — so the binding is the only linked route for a recursive chain.

- **WF-027** Every `member` MUST be a declared attribute or characteristic
  of `createdType`; a characteristic initialisation's value MUST be a
  boolean-typed expression (a boolean constant is the v0 form). An
  **attribute** member's value typing — family, units, sign and keuzetype
  domain against the declared attribute — is WF-023's second write
  position, not this rule. The value
  evaluates on the creation subject, exactly as an attribute initialisation
  does (`RUNTIME-SEMANTICS.md` ch. 5.5): true attributes the characteristic,
  false **or empty** leaves it unattributed — equivalent to an absent
  initialisation, the closed-world reading characteristic checks already
  apply (the empty flank follows the attribute-initialisation parallel — a
  leeg value writes silently — and is indistinguishable from false to a
  characteristic check). The verifier
  admits a statically boolean-typed value and refuses a statically
  non-boolean one; the v0 unknown-typed sentinel it cannot refute defers to
  the runtime bounded-write fail-fast (the WF-023 skip). *v0 mirror note:*
  both the ghost-member and the non-boolean-value violations are resolver
  errors, but v0 still appends the offending initialisation after diagnosing
  — v1 makes the form itself ill-formed. **Delta (promotion of the append).**
  *v0 mirror note:* the v0 emitter produces only the constant form; the
  boolean-expression form is the v1 relaxation the Catala frontend uses for
  boolean subscope inputs (additive on the wire, `CONSTRUCTS.md` row 21).
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
| `path` | non-empty list of PathStep | how targets are reached from the anchor; the final step is the join that reaches the target type |
| `condition` | Condition? | guard on anchor or target |
| `validity` | Validity | — |

**PathStep**: a RoleBinding (ch. 2.6) — a symbolic hop over an existing
relation, binding **the role the hop's far side fills**; the near side
fills the opposite role, so direction is implied and the earlier
sketch's separate `direction` field is dropped (cardinality is the
bound role's declared `many`). *Migration:* v0's
`zetVeld`/`joinVeld`/`veld` are derivable from the role bindings by the
backend's own role→field mapping; the derivation moves down into the
backend, out of the IR (the Phase-3 forcing move).

The symbolic content rides the v0 wire as **extension fields** on
`ResolvedFeitCreatie` (per the Method), alongside the bean fields they
supersede: `feittype` (the created relation), `ankerRol` and `doelRol`
(the roles its two ends fill, by declared name), `joinRol` (the
RoleBinding of the hop that reaches the target), and per `Tussenstap` a
`rolBinding` (its hop). The join chain reads: anchor → `tussenstappen`
in order → `joinRol` → target.

- **WF-028** The symbolic form is **all-or-nothing**: once any symbolic
  field is present, all MUST be. `feittype` resolves to a declared
  two-role fact type; `ankerRol` and `doelRol` are distinct roles of it
  whose fillers are the anchor resp. target type; every hop's binding
  resolves (ch. 2.6) with its near side matching the chain's current
  type; the chain MUST reach the target type; and the created relation
  MUST differ from the relation of the final hop. A bean-field-only v0
  record stays uncheckable (its relational content is not symbolic on
  the wire) — recorded, not silently green. *v0 mirror note:* v0
  enforces the resolution clauses fail-soft — seven distinct
  warning-and-drop reasons; an emitted v0 record is always fully
  resolved. **Delta (promotion):** in v1 the ill-formed variants are
  verifier errors, not silent drops.
- **WF-052** A connecting relation whose **created-side role is
  to-one** MUST have at most one object-creation writer (ch. 3.3: the
  creations whose uniquely-derivable back-link it is). Two such writers
  would hand the to-one end on a shared subject a second filler: the
  reference interpreter faults that write (`RUNTIME-SEMANTICS.md`
  ch. 5.5) while a guardless stable model would silently hold both —
  the single-writer discipline of WF-026/WF-050, applied to created
  relations, closes that divergence statically. Deliberately
  conservative like its family: two writers with mutually exclusive
  conditions are still rejected. Fact creations are not restricted:
  their to-one writes are fault-paired already (the interpreter's
  writer fault ≡ the generated cardinality guard's UNSAT), and a
  to-many created-side role makes multi-writer object creation
  harmless (idempotent links; both executors agree).

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
**Landed (2026-08-18):** the symbolic scope rides the v0 wire as the
`scopeRol` extension field (a RoleBinding whose bound role reaches the
scope parent; the written `scopeVeld` must denote it — WF-054), and both
uniqueness forms execute in both executors: the union as one shared key
domain across all sources, the scoped form per scope parent, with the
v0 narrowing that scope-less instances do not participate mirrored and
recorded (`RUNTIME-SEMANTICS.md` ch. 5.9). Tracked delta within the
mirror: the bindingless scope resolves by role name/plural from the
subject alone (the wire carries no parent type), where v0's resolver
disambiguated by the (element, parent) pair — a same-named-roles clash
refuses loudly instead of picking; the `scopeRol` binding is the
unambiguous route. A scoped union is not
representable in v0 and stays out. The emitter produces `scopeRol` since the 2026-08-18 pin bump (toolchain `fe46e49`).

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
| `condition` | Condition | over the day; **pure** (WF-031) and may reference other day kinds **acyclically** (WF-085) |
| `validity` | Validity | MUST be ALTIJD (open on both ends); no evaluator reads a day-kind window, so a bounded one is refused (WF-031) |

- **WF-031** The reference MUST resolve to a declared DayKind; the
  condition MUST be present and **pure** — a day-kind voorwaarde
  references only dates, date-components, eerste-paasdag, literals,
  parameters and other day-kind names, and MUST NOT read an attribute or
  a characteristic (the stratum-0 framing, ch. 5.4: a day kind classifies a
  bare date before the rule graph, with no fact base); there MUST be
  exactly one definition per DayKind; the validity MUST be ALTIJD —
  `vanaf` and `tot` both null (a bounded window is refused, since no
  evaluator consults a day-kind validity, so a bounded one would be
  silently dead). *v0 mirror note:* a missing condition is warn-and-drop;
  a duplicate definition warns and appends with first-wins lookup; the
  validity is not read at all; the purity of the condition is neither
  checked nor documented. **Delta (promotion).** The recursion question
  is **closed** (decided with the stratification chapter,
  `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` Q8): mutually recursive
  definitions are refused by WF-085 below, derived-state reads by the purity clause
  here.
- **WF-085** The day-kind **reference graph** MUST be acyclic. A day-kind
  voorwaarde may reference other day kinds (a `Predicaat` of soort
  `DAGSOORT`), which builds an edge from the defined day kind to each day
  kind it names; mutually recursive definitions have no fixpoint and are
  refused (ch. 5.4). An undeclared reference is WF-034's concern and carries
  no edge here. *v0 mirror note:* v0 permits the references and resolves a
  cycle to a **silent false** (the §9.10 recursion prohibition unenforced
  for day kinds); WF-085 makes the refusal structural. **Delta
  (diverge-from-v0):** v0 neither checks nor documents this
  (`CONSTRUCTS.md`).

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

Members are ordinary rules; a non-recursive group adds no execution
semantics (namespacing only). The `recursive` marker HAS its neutral
meaning (decided with `CONSTRUCTS.md` row 14): membership of a
recursive group is exactly what WF-045 admits — a creation member may
sit on a dependency cycle, advancing one generation per instance to
the fixpoint (`RUNTIME-SEMANTICS.md` ch. 5.5/6.2), with the bounded
budget turning divergence into a fault in every executor.

- **WF-033** (dialect) Every member name MUST resolve to a rule form in
  the model; a `recursive` group MUST contain at least one creation
  member — an object creation or a relation (fact) creation — and every
  object-creation member of a recursive group MUST be conditional (an
  unguarded object creation in a recursive group cannot terminate). A
  relation-creation member needs no guard: it mints nothing — its writes
  are idempotent links over the fixed universe, so its fixpoint is the
  transitive closure and termination is structural
  (`RUNTIME-SEMANTICS.md` ch. 5.5). The shared pass budget still bounds
  the walk: a closure needing more passes than the budget (a chain of
  ~100+ hops) faults in the reference interpreter where the ASP
  grounding completes it — the recorded budget boundary of `RUNTIME-SEMANTICS.md` ch. 5.5,
  same family as the object-creation generation bound. *v0 mirror note:* containment is a
  v0 error; the unguarded case only warns. **Delta (promotion)** for the
  guard.

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
| AttributeReference | `attribute`, `subjectType`, `path` (role steps), `type`, `source` | **Delta (row 4):** v0 splits navigation over `navigatieRol` + `navigatiePad` + a `set` flag (with the invariant that a non-empty path implies a role) and keeps the transpiler-facing `onderwerpBron`; v1 unifies into one explicit path of role steps, with result cardinality derived from the declared role cardinalities. **Landed (2026-08-18):** the symbolic path rides the v0 wire as the `navigatieBindingen` extension field (ch. 4.1a, WF-053); the anchor `jutterswet-kustwacht` executes all three navigated shapes through both executors. *Migration:* the v1 unification drops the written-name fields the bindings supersede |
| ParameterReference | `parameter` (name ref), `type`, `source` | the v0 wire form is already symbolic (the emitter writes the name) — the "embed the model object" wart is in-memory only |
| VariableReference | `name`, `type`, `source` | a rule-local let-binding (§13.4.2.10); binding is frontend scope resolution |
| ContextValue | `name` ∈ evaluation-date, evaluation-year | **Delta (row 25):** split from v0's `Constante` grab-bag; the pronoun referents that shared that node are surface forms the frontend resolves away. *v0 mirror note:* the wire carries `Constante` named `Rekendatum` (typed day-precision) or `Rekenjaar` (whole number); `Rekendatum` is the ch. 8.2 execution-date constant and the index of the ch. 8.3 vintage read (`RUNTIME-SEMANTICS.md`); `Rekenjaar` has no corpus witness yet — executors fault on it |

**Domain identity (WF-089, ch. 2.4).** An `AttributeReference`'s or
`ParameterReference`'s `type` is a use site of the declaration it
reads: where either names a keuzetype domain, both name the same one —
looked up through a `ListType`'s element for a list-typed read. The
reference's `type` is a repetition of the declaration, never a second
opinion: the discipline WF-088/WF-061 apply to the inline value or
constructor list, applied to the domain name. A navigated read crosses
against the attribute at its endpoint type (ch. 4.1a); a
`DimensionCellRead` (ch. 4.3) crosses its own `type` against the
dimensioned attribute it reads; a **fold source** — the attribute
reference an aggregation reads each contributor through (ch. 4.3) —
crosses like any other attribute reference, even though its *shape* is
checked by the fold-source rules rather than the ordinary expression
walk. The same rule reaches one non-reference use site: an
**element-passing aggregation** (`EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM`)
returns a contributor unchanged, so its own `type` is a use site of its
operand's and must name the same domain. **WF-090 (ch. 2.4) says the same
sentence for struct domains** over these same use sites — struct domains
are not keuzetypes, so the identity is stated twice under two rule
numbers, not once under a widened term.

**Reference-type identity (WF-091).** The domain is one **facet** of that
sentence; the sentence is stated here whole. A use site's serialized
`type` MUST be **identical** to the declaration it takes its value from —
not compatible with it, not convertible to it: identical on every facet
the datatype carries, up to the case-insensitive comparison WF-001 fixes
for a declared DOMAIN name. The three UNIT names are compared byte for
byte, for the reason the unit paragraph below gives. The facets are
compared in one fixed order,

> **soort → domain → unit → decimals → wholeness → sign → residual
> structure**,

and the FIRST facet that differs is the single violation reported: one
comparison yields one message, never two facets at once. Per *comparison*,
not per path — a walk that crosses one path repeatedly can report it more
than once. The element-passing aggregation is the case in the corpus: it
crosses its own `type` once per operand, so a two-operand fold whose type
names another domain than both operands gives two WF-089 messages on the
one path (measured).

The rule *number* follows the facet and the arm, not the comparison. A
facet that already carries a number at that arm keeps it, with its
wording: the domain facet is **WF-089** (keuzetype family) and **WF-090**
(struct domains) at every arm, and the soort facet is **WF-078** at a
`BinderRef` (ch. 4.7). Every facet without one is **WF-091**'s, and names
itself in the message. That is what makes the rule extensible: a further
facet on the wire costs a message, not a rule number.

Two clauses of the order are deliberate and load-bearing.

- The **soort facet stands aside wherever either side names a keuzetype
  or a struct domain.** `domeinen` is ONE namespace, so
  enumeration-versus-sum-type and domain-versus-no-domain are the domain
  facet's own clauses — WF-089's variant and one-sided arms, WF-090's —
  stated and numbered there.
- The **residual facet is the backstop, not a leftover.** The named
  facets decide the *diagnosis*; the residual comparison — the whole
  datatype, every declared domain name folded to its ci form and the unit
  names left byte-exact, the same split the unit facet makes — decides
  that nothing escapes. It is not redundant: the domain facet looks
  *through* a `ListType` element, so a list-typed declaration and a
  reference typed as its element agree on domain while soort has stood
  aside, and only the residual comparison sees the missing layer.

**Unit identity is structural, never convertible.** Two units are the
same unit iff their abbreviation, declaring system, canonical unit,
conversion factor and — recursively — the denominator of a composite rate
all agree. The three NAMES are compared **byte for byte** — the copy
repeats the declaration literally, case included — where every declared
domain name in this rule folds to its ci form. Name RESOLUTION is
case-insensitive: that is WF-001's domain, and the domain facet of this
very rule stays ci, pinned by its two golden witnesses. Copy FIDELITY is
another question, and a consumer that reads those three names literally
to pick its conversion diverges silently on a copy that lies in case
alone. *Measured:* a `KG`/`GEWICHT`/`KG` copy of a `kg`/`Gewicht`/`kg`
declaration, factor 1 on both sides, written into a target declared in
`t` — `ref` reads the declaration and answers `3.75` for an input of
`3750.00`, while a third executor whose unit lookup is case-sensitive
finds no match, falls back to factor one and answers `3750.00` (the ASP
backend carries no cross-unit write and refuses the form ahead of
translation, so it neither confirms nor contradicts) —
`wf091-attribuutref-eenheid-kast`. The factor and the rate shape agree
exactly as well, being no names. Convertibility is what WF-035 and WF-023
cross, so a lie *inside* the convertible class is compatible by
construction: `kg` and `t` share a canonical unit and a shape.
*Motivation (measured, the
card):* a `t`-for-`kg` annotation on an `AttributeReference`, a
`BinderRef` or a rule target was verifier-clean; `ref` and the ASP
backend answered from the declaration and a consumer honouring the
annotation rescaled by the factor — a wrong answer nothing refused, in
the one class WF-035 cannot reach.

**Arms.** The five REFERENCE use sites WF-089 and WF-090 name —
`AttributeReference` (at the endpoint of a navigated read), the
`DimensionCellRead` over one, `ParameterReference`, a binder reference,
and the fold source — plus one they did not reach: a **rule target**. The
target lvalue is WF-022's and the expression walk never enters it —
WF-088's index row records that omission as measured harmless for a
drifted enumeration copy, because both executors write through the
DECLARED datatype — which left the target `type` the one reference type
in a document nothing crossed. It is not harmless for a unit. The other
two write positions, an object-creation member and a defaults
consequence, **name** their target and carry no reference type of their
own; what enters them is WF-023's and WF-090's assignment cross, a
different question under a different law (compatibility, not identity).

The sixth use site WF-089/WF-090 name — the **element-passing
aggregation** against **each** operand — is crossed there on the **domain
facet alone**, and stays outside the *identity* sentence above. A
reference type is a *copy* of one declaration; an aggregation `type` is a
**derived** type over an operand LIST. A `MINIMUM`/`MAXIMUM` takes its
decimals as the least operand precision, its wholeness as the conjunction
over the operands, and its sign and unit from operand 0; an
`EERSTE`/`LAATSTE` takes operand 0's type in full. Identity against EVERY
operand is therefore unsatisfiable the moment two operands differ — which
the form ch. 4.6 blesses, "het minimum van het gewicht en 0", a unitless
literal beside a `kg` attribute, does by construction. The domain is the
one facet a pass-through fold hands on **unchanged**: whichever
contributor wins, the winner's label IS the node's label, which is why
WF-089/WF-090 can cross it against every operand and no further facet can.
Those further facets are not left uncrossed, though — they are crossed
against the **derivation**, one comparison instead of one per operand, by
the clause that follows.

**Derived-type identity on composite positions.** Everything above
compares a use site against a **declaration**. At a **composite** position
there is none: an arithmetic node, a rounding, a clamp and a value
aggregation carry a type that is a **derivation** over their operands
(`RUNTIME-SEMANTICS.md` ch. 2.10, clause 2). The rule therefore compares
the annotation against **the expectation its operands derive** — computed
from those operands' own serialized types by the ch. 2.3 precision table
and the ch. 2.6 unit rules — in the same facet order, with the same
message shape, and under the same **number-follows-the-facet** rule. It is
the same law: an annotation repeats what is fixed *elsewhere*, and here
*elsewhere* is a formula instead of a declaration.

"The same number" therefore means the same *rule for choosing* the number,
not one number for the whole clause. The DOMAIN facet reports under
**WF-089** (keuzetype domains) and **WF-090** (struct domains) at a composite
exactly as it does at a reference: a `PLUS` or a `Rounding` annotated with a
keuzetype domain its derivation does not name is refused on WF-089's
one-sided arm (`wf089-composiet-domein`). Only the facets that carry no
number of their own — soort, unit, decimals, wholeness — are WF-091's here.

**How far the chain reaches, exactly.** An operand's own type may itself be
anchored — a reference against its declaration, a composite against its own
operands — and where every operand underneath a composite is anchored that
way, the composite is bound to the declarations beneath it. That is the good
case, not a theorem: the chain reaches only through operands this law (or a
reference arm) actually crosses, and it **breaks at a fourth-class operand**.
A division, a power, a percentage-of, a root, an absolute value and the
date/time extractors carry an annotation nothing crosses, so a composite that
reads one consumes a **fabrication** as ground truth — a `GEDEELD_DOOR` over
two unitless operands may be annotated `kg`, and a rounding above it then
derives `kg`, and a `kg` target accepts it, all verifier-clean (measured on a
probe; the shape is admitted, not carried). *Measured 2026-09-04:* the corpus
holds **26** such operand positions over **19** of the 174 golden+rood
documents — 23 under a division, one under a division-by-absolute, one under a
power, one under a percentage-of; 12 of them read by a rounding and 14 by an
additive node. The deferral proper cannot be self-served — an operand carrying
the unknown sentinel, or no type at all, is already WF-034-ill-formed at every
value node, so a document cannot switch this cross off by emptying a field —
but a fourth-class annotation is the stronger escape and it stays **open**: it
does not switch the cross off, it feeds it a premise. Closing it means deriving
a result type at those positions (`RUNTIME-SEMANTICS.md` ch. 2.10's fourth
class), which is a decision of its own.

A formula may be **silent on a facet while speaking on the rest**, so each
position states which facets it *claims*. A facet it does not claim is not
crossed, and the residual backstop stands down with it — otherwise the
backstop would report the very facet the formula declined to state. Since the
**sign** is claimed nowhere (the first exclusion below), the residual backstop
stands down at **every** derived position: the refusal here is exactly the
named facets of the row, and unlike the reference half it is not total by
construction. That is what a partial formula buys.

| Position | Derived type | Facets claimed |
|---|---|---|
| `Binary` plus / min / verminderd-met, numeric | maxDec; whole iff **both** operands are; the **left** operand's unit. The soort **collapses to a number**: `Percentage + Percentage` derives `number`, so a number is the only annotation this position admits | soort, domain, unit, decimals, wholeness |
| `Binary` plus / min over a date and a duration | the **date** operand's type, unchanged (the duration carries the calendar unit and contributes nothing to the result type) | soort, domain, unit, decimals, wholeness |
| `Binary` maal | the ch. 2.6 join: a unitless operand hands the other unit through; a rate times its convertible denominator unit **cancels** to the numerator. The soort collapses as it does on the additive row | soort, domain, unit |
| `Rounding` over a numeric or percentage base | the base's soort, unit and sign, with the node's **own** decimal count (whole iff zero) — ch. 2.4's *facing* precision | soort, domain, unit, decimals, wholeness |
| `Rounding` over any other base | the base's type, unchanged — the fall-through of the row above, and a shape ch. 2.4 does not describe: see *Measured, and open* | soort, domain, unit, decimals, wholeness |
| `Clamp` | the base's type, unchanged (ch. 2.7 returns the base, the minimum or the maximum) | soort, domain, unit, decimals, wholeness |
| `Aggregation` som | operand 0's type, unchanged (§4.6 *Eenheid1*) | soort, domain, unit, decimals, wholeness |
| `Aggregation` eerste / laatste | operand 0's type, unchanged | soort, unit, decimals, wholeness — **not the domain**, which WF-089/WF-090's own arm already crosses at this position, once per operand |
| `Aggregation` totaal | operand 0's type with a rate's **denominator stripped** — but only where the fold *integrates over time*. The scheidslijn is the emitter's own predicate: the **non-temporal set reading** ("het totaal van *x* van alle *elementen*" over a plain attribute) is an alias of the sum, folds over instances and integrates over nothing, so it passes operand 0's type through unchanged; every other reading — the non-set rate read, and the set read over a **temporal** attribute, which is the §7.1 per-element timeline fold — integrates, and there `EUR/jr` totals to `EUR` (§4.20) | soort, domain, unit, decimals, wholeness |
| `Aggregation` minimum / maximum | the **least** operand precision, the **conjunction** of wholeness, operand 0's sign and unit | soort, unit, decimals, wholeness (the domain for the same reason as eerste/laatste) |
| `Aggregation` aantal | whole, zero decimals, unitless — a constant of the operator, reading no operand | soort, domain, unit, decimals, wholeness |

"Unchanged" above is about the *derivation*, never about the wholeness facet:
a pass-through hands on whatever precision form its operand carries, whole or
fractional, and the wholeness facet then crosses that form like any other.

The `maal` row states the join in the two shapes the IR *fixes*. It has a
third and a fourth, and the rule claims neither: ch. 2.5's opaque
**product** of two plain units, whose algebra the spec fixes (a sorted
multiset of atomic factors, no canonical, no factor) but whose wire
*spelling* it does not — an identity cross there would pin an
`abbreviation` and a `system` ch. 2.5 does not require — and the
unspellable rate factor the producing frontend refuses outright
(`RUNTIME-SEMANTICS.md` ch. 2.10). *Measured 2026-09-04:* no `maal` node
in any of the four zones carries two unit-bearing operands at all, so the
cancel rule itself rests on one negatief.

The `totaal` row is the one place this law does not compute its own formula
but **mirrors a producer's**. The four sources disagree about the strip and
they do not disagree symmetrically: `ref` and the ASP translator do not
strip at all (both answer operand 0's unit whole), while the RegelSpraak
frontend — since the *set-fold "totaal van"* card — strips on every reading
*except* the non-temporal set one, tested there by one predicate
(`ExpressieBouwer.setLezing`: exactly one operand, an attribute reference
carrying `set`, over an attribute its object type declares **no**
granularity for). This rule mirrors that predicate rather than restating it,
because it is the shape the emitter can actually produce: an unconditional
strip invented a wire type no executor can compute, the set fold adding
`EUR/mnd` values into an `EUR/mnd` target. *Measured 2026-09-04, and the two
halves are pinned differently.* The **strip itself** is two-sided in one
golden: `jutterswet-tempototaal` carries both readings, and dropping the
strip entirely turns its temporal node red (and `wf091-totaal-eenheid`
clean), while making it unconditional turns its set-fold node red. The
**granularity half** of the predicate has no witness in these zones at all —
no `totaal` in golden, rood, negatief or the parity zone reads a *temporal*
attribute as a set, so removing that half leaves gate 3 green. It is carried
on the emitter's authority alone: the frontend narrowed its own predicate to
that half against a measured executor divergence on the shape (a set read
over a temporal attribute folds each element's timeline *first*, §7.1, so it
does integrate), and neither this corpus nor the frontend's suite carries the
shape. Where the operand names an object type the document does not
declare, the granularity is unreadable and the arm **defers** — the family's
treatment of an unresolvable declaration; the corpus carries no such
`totaal` either.

Two exclusions from that table are **measured**, not chosen, and each was
priced before it was taken.

- The **sign** is claimed at no derived position — at none of the eleven,
  without exception. It runs both ways, which is why neither direction can
  be crossed. A derivation mostly *under*-claims a sign restriction
  (`ONBEPAALD`) while the document narrows it truthfully: a sum of two
  non-negative operands is non-negative, and — the sharpest case — a clamp
  with a minimum of `0` **is** non-negative whatever its base says. And it
  over-claims in the other direction at the `aantal` arm, where the formula
  says `NIET_NEGATIEF` and two anchor goldens write `ONBEPAALD`. *Measured
  2026-09-04:* **12** documents over golden, rood and negatief narrow it
  truthfully and **5** widen it; crossing the sign on the additive arm alone
  turns 5 golden+rood documents red, at every derived arm 7 (the two extra
  are the `aantal` widenings), and emptying every exclusion in the table 27.
  The exclusion became total only in fix round 2026-09-04: six arms — clamp,
  rounding, `som`, `eerste`/`laatste`, `totaal` and a single-operand
  `minimum`/`maximum` — were crossing the sign against the text of this
  bullet. Those six carry **zero** golden+rood witnesses, so removing the
  cross left the verifier's report over all four zones byte-identical; what
  it stopped refusing is pinned by a probe rather than by the corpus (the
  clamp-with-a-minimum-of-0 shape above, refused before and clean after).
- The **precision** of a `maal` is claimed nowhere either, and its price
  needs both numbers stated, because they are not the same measurement.
  ch. 2.3's rule is "the SUM of the operands' decimals, never whole", and
  the frontend follows it, while this corpus's other producers do not — the
  Catala plugin writes the pinned ch. 2.9 decimal scale instead. *Measured
  2026-09-04, against that rule:* 9 decimals and 9 wholeness divergences
  over the four zones, and crossing it turns **11** golden+rood documents
  red (3 on decimals, 8 on wholeness, disjoint). The *implementation* holds
  no sum: with the facets unclaimed it writes a placeholder (zero decimals,
  fractional), and merely un-skipping the two facets against that
  placeholder turns **20** golden+rood documents red. 11 is the price of the
  rule; 20 is the price of the placeholder, and only the first is what
  ch. 2.3 costs.

**What stays uncrossed, and is named.** ch. 2.10's **fourth class** — a
division (`GEDEELD_DOOR`, `GEDEELD_DOOR_ABS`), a power, a percentage-of, a
root, an absolute value and the seven date/time extractors — derives no
result unit in v0, so this rule claims nothing there. A duration node's own
type is the operator's parameter (WF-038), not a derived result. `IfThenElse`
and `Let` stay out for the opposite reason: WF-069 and WF-076 already cross
their whole type against their branches and their body, and no node is
crossed twice.

The executor harness of `scripts/check-reftype-derivation.sh` (gate 13) keys
its own set on (node, operator) for the same reason, and the two sets were
**aligned by hand** — they are two independently maintained lists, and no
tripwire compares them today. Where they differ, they differ for a reason and
the difference is measured, not accidental: gate 13's derived set carries no
`AANTAL` (a count derives no *unit*, which is all that harness twists, while
this rule claims its whole numeric shape); gate 13 exempts an axis this rule
has no notion of, the DURATION **operand** of a date ± duration node, because
both executors route the calendar unit off that operand's static type; gate 13
twists a `Let`, which this rule leaves to WF-076; and gate 13's prose lists the
fourth-class branches without `GEDEELD_DOOR_ABS`, which its set-complement in
fact covers and which this rule names. Keeping them aligned is a reviewer's
duty, not a mechanism — and making it one is a card, not a claim to make here.

*Measured, and open.* The pro-rata builtin (`tijdsevenredig deel`) is a
pass-through in the frontend and in **no other source** — ch. 2.3 lists no
row for it, and neither executor-side derivation reaches it — and one
golden annotates it with its TARGET's type rather than its base's (two
nodes, `synth2-prorata-breed`). A one-source formula is not a law; it
stays uncrossed and stays written down here.

So does the **one-sided unit pair** an additive node may carry under WF-035's
literal-adoption clause (ch. 4.6), and here the contested part is smaller than
the silence. Only **one of the two orders** is disputed. When the LEFT operand
carries the unit, all four sources agree the result carries it: the frontend
and ch. 2.6 say "the left", and the ASP translator and the Drools backend say
"the left, else the right", which is the same answer. The dispute is the other
order — a unitless left beside a unit-bearing right, where ch. 2.6 gives the
result *no* unit and both executor-side derivations give it the right one. The
rule nevertheless claims **neither**, and the reason is worth stating rather
than implying: its guard is the operand pair's unit compatibility, not the
order, and splitting it would fix the LEFT-unit order as normative — the half
that is uncontested and, measured, carries **zero** occurrences in golden and
rood — while the half that is contested stayed open anyway. So the split buys
nothing today and pins something tomorrow. What the corpus does carry is one
node in the contested order (`jutterswet-sleeprest`, `50 min <km>`), and it
annotates the RIGHT unit — siding with the executors against ch. 2.6. Note
that WF-035's adoption clause already reads a unit into that very node: the
unitless side is a LITERAL, so it adopts its sibling's `km`, and under that
reading "the left unit" IS `km` and the four sources agree again. The
genuinely open class is therefore narrower still — a one-sided pair whose
unitless side is not an adopting literal, of which this corpus carries none.
The divergence is a finding, not a choice.

Two more shapes, and one coverage gap, are stated and **unwitnessed** —
which is a weaker thing than stated and pinned. A `Rounding` over a PERCENTAGE
base keeps its base's unit here — ch. 2.10, the ASP translator and the Drools
backend all say so and the RegelSpraak frontend drops it, a divergence this
rule records by following the majority — and the same holds for the
fall-through row, a rounding over a base that is neither number nor percentage.
Neither branch has an artifact either way: *measured 2026-09-04*, all **32**
roundings in the four zones stand over a plain `Numeriek` base, so both rows
are stated and unexercised. And the UNIT facet, the facet this whole rule was
built for, has a
golden witness at **three** of the eleven positions only — the additive
`Binary` (8 documents), `maal` (3) and `som` (1). At the other eight it is
pinned by a negatief and by nothing else, and a negatief pins a refusal, never
a non-refusal: nothing in this corpus would notice if those eight arms
over-claimed a unit.

**What stays outside.** `TemporalRead`, `Invoke`, `Lambda` parameters, a
`StructLiteral` and a `FieldAccess` already cross the FULL datatype under
WF-049/068/070/073/074, so the identity stays away from them and no node
is crossed twice under two identity rules; `Construct` reads no
declaration. The
write-side **precision gate** of WF-023 — a computed value against its
target's declared precision — needs expression-shape analysis and is a
different question that stays OPEN. Deferrals are the family's: either
side absent or the v0 unknown sentinel, or a declaration that does not
resolve — and since 2026-09-04 the shapes measured IN THESE FOUR WAVES AND
THEIR FIX ROUND to let a
document **self-serve** one are WF-034-ill-formed (further measured
shapes stay open below). Both halves of the resolving
pair, in a value position and in a fold source alike (four arms: the
aggregation branch replaces the generic walk, so each half needs a clause
on both paths) — a reference naming no `objecttype`, and a reference
naming no `attribuut`. A `ParameterRef` naming no `parameter`, the same
shape one node kind over. And both spellings of the unknown type on a
rule target's `type`: the sentinel and its unspelled twin, an absent
type. Measured on the goldens, one position emptied at a time: 409 of 751
`attribuut` positions, 35 of 37 `parameter` positions and 295 of 295
rule-target types stayed verifier-clean before these clauses, and none
does after; outside their own negatief witnesses the shapes are carried
by zero documents either way. Their executor behaviour was measured too,
and it is not uniform: the two `attribuut` shapes and the `parameter` one
are refused by `ref` and clingo (Drools skips the rule or throws), while a
target with no type replays **clean** in `ref` and clingo and only throws
in Drools — a verifier-clean document that crashes one executor, which is
why the quiet shape was the urgent one. The THIRD wave (2026-09-04) closed
the four channels that list left open, each measured the same way: a
`DimensieCel` whose `attribuut` is null **or holds a non-reference
expression** — two spellings of one escape, since the cell arm resolves only
through a complete inner `AttribuutRef` and an empty field misses that
pattern by being absent while any other node misses it by being the wrong
constructor, and either way the cell's OWN type goes uncrossed — measured on
**four** existing artifacts, each of which turns `verifier: PASS` outright
under either spelling: `wf091-dimensiecel-eenheid` (unit lie),
`wf089-dimensiecel-enum` (keuzetype-domain lie), `wf090-dimensiecel`
(struct-domain lie) and `wf040-unpinned-cell`, whose WF-040 label/dimension
cross rides on the same match and goes with them; a `Kenmerk` check
naming no `kenmerk` (89 of the goldens' 96 positions stayed clean when
emptied, 0 after); a `ResolvedVerdeling` naming no source or target
attribute, the one reference pair the expression walk cannot reach because an
allocation is not an expression (11 of 11 each, 0 after); and the CONTAINER
half of the payload descent, a sentinel in a `ListType` element or
`StructType` field under a constructor payload, plus a sum reached back
through such a container. The FIX ROUND of the same day added the filled half
of the allocation clause: a present attribute name on any of the allocation's
six name fields is not blank, since `""` satisfies every consumer's `Some a`
and names nothing (`bronAttribuut` 11 of 11, `doelAttribuut` 11 of 11,
`restantAttribuut` 9 of 9, `ratoAttribuut` 7 of 7, `maximumAttribuut` 4 of 4,
`groepeerAttribuut` 3 of 3 stayed clean before, 0 after — and on the last
three the executors do not fault at all, ch. 4.6 (i)). Zero over-rejection,
measured directly: the verifier's verdict over all **462** documents of
golden + rood + negatief + parity that PREDATE this card (153 + 22 + 257 +
30) is unchanged; the eleven new negatiefs the card and its fix round add are
the only additions, and four pre-existing
allocation negatiefs gain a truthful incidental WF-034 line and keep both
their verdict and the rule their directory declares. The FOURTH wave
(2026-09-04) closed the naming class the first three kept measuring and
leaving — not an emptied field but one filled with a name that resolves
against nothing (ch. 4.6 (j)–(l)): every filled attribute name on an
allocation resolves against the declaration scope of its own side
(`bronAttribuut` 11, `restantAttribuut` 9, `doelAttribuut` 11,
`ratoAttribuut` 7, `maximumAttribuut` 4, `groepeerAttribuut` 3 golden
positions clean before, 0 after — the two source-side fields first, as
ch. 4.6 (j) lists them), an allocation names
both its object types (11 of 11 each, 0 after), and the ch. 4.3 fold-source
exception returns to its scoping — the row-1 wart, which the wire spells with
the unknown sentinel — so that a fold source carrying a real datatype
resolves as the ordinary attribute reference it is. Zero over-rejection there
too, and measured the same way: over all **486** documents of golden + rood +
negatief + parity, 15 reports change and 10 verdicts, the ten being the
card's own new negatiefs — seven in the wave, three more in its fix round,
one per sub-arm; no golden and no rood document moves, and five
documents keep their verdict while gaining truthful incidental lines. That
is again the list of channels closed **here**, not a claim that no deferral
can be self-served. *Measured, still open:* the sentinel on the 9
fold-/aggregation argument types (the aggregation branch replaces the generic
walk there — its own question, not this descent's); and WF-007's
precision-form check, which has the identical blind spot
in containers and is its walk to make, not this rule's.

**The numeric-literal lexeme (WF-081).** A `Literal` whose `type` is
numeric (`Numeriek` or `Percentage`) MUST carry as its value a **bare
number lexeme**: after mapping every `,` to `.` (the Dutch decimal comma,
admitted as a decimal separator only — never as grouping), the lexeme
matches exactly

```
number   ::= integer | decimal | rational
integer  ::= '-'? digits
decimal  ::= '-'? digits? '.' digits
rational ::= '-'? digits '/' digits      -- denominator non-zero
digits   ::= [0-9]+
```

Nothing else is a number lexeme: no whitespace, no leading `+`, no
exponent, no grouping separators, and — the fence this rule exists for —
no unit or currency token inside the lexeme (`"5 €"` is ill-formed; a
unit belongs in the datatype's `eenheid`, never in the value string).
Both executors share this grammar in their lexeme parser and fault at
evaluation time on anything outside it; WF-081 makes such a document
ill-formed at verification time, ahead of any executor. Scope: numeric
literals only — date lexemes (ISO `yyyy-mm-dd`) and `Tekst` literals
(which legitimately carry arbitrary text) are governed by their own
forms, not by this rule.

**The date-literal lexeme (WF-084).** A `Literal` whose `type` is a
day-precision date (`DatumInDagen`) MUST carry as its value the **strict
ISO date lexeme** the executors parse:

```
date  ::= yyyy '-' mm '-' dd
yyyy  ::= digit digit digit digit
mm    ::= digit digit                 -- 01–12
dd    ::= digit digit                 -- 01..days-in-month, leap-aware
digit ::= [0-9]
```

Fixed field widths (four-two-two), single `-` separators, no prefix, no
whitespace, no time component; the month is 1–12 and the day is a real
day of that month in the proleptic Gregorian calendar (29 February only
in a leap year). This is the fence the rule exists for: a RegelSpraak
date literal is written Dutch day-first (`31-01-2024`, optionally the
`dd. ` de-dato prefix) — a lexeme both executors read as ISO, so a bare
`31-01-2024` was silently taken as year 31 (`0036-07-16` after the
day-2024 roll-over), a differential-blind wrong answer that ref and the
ASP backend agreed on, while the prefixed `dd. 31-01-2024` faults. Both
executors share this grammar in their `date_of_iso` reader and fault at
evaluation (ref) or translation (the ASP backend) time on anything
outside it; WF-084 makes such a document ill-formed at verification
time, ahead of any executor. The emitter writes the bare ISO lexeme and
keeps the source snippet in `bron`. Scope: day-precision date literals;
datetime lexemes (`DatumTijd`, millisecond precision) carry their own
ISO discipline, tracked separately (they are not exercised by any corpus
literal yet).

### 4.1a The navigated read (WF-053)

An attribute reference whose value lives off-subject **navigates**: its
`objecttype` is the endpoint the attribute lives on, and the walk from
the subject to that endpoint takes one role hop per step. The v0 wire
carries the walk in three written shapes (§13.4.1/§13.4.2.9): a written
role name (`navigatieRol`, possibly a plural — single hop), a resolved
chain of declared role names (`navigatiePad`, subject-outward, first hop
included — multi-hop), or a directly named object type with no written
role at all (the **by-pair** shape: the connecting relation is derived
from the type pair). The characteristic check (`RUNTIME-SEMANTICS.md` ch. 5) carries the same
idea in its `navigatie` field — one written role hop.

The symbolic route (extension fields per the Method):
**`navigatieBindingen`** on the attribute reference — a list of
RoleBindings (ch. 2.6), one per hop, subject-outward, each with **the
role the hop reaches bound** (the far role; its filler is the type the
walk stands on next) — and **`navigatieBinding`** on the characteristic
check, its one hop in the same orientation. Written whole or not at
all, never null, never empty.

- **WF-053** A navigated reference MUST resolve its walk. With
  bindings: the binding count equals the step count (one for the
  single-hop and by-pair shapes), every binding resolves (WF-047's
  resolution: declared fact type, declared role name — the plural does
  not bind — two roles, or one reciprocal), each hop's opposite role
  fills the type the walk stands on (the subject for hop one), the last
  bound role's filler is the reference's `objecttype` (the endpoint),
  and each written step, where the v0 form carries one, denotes its
  hop's bound role (declared name or plural). Bindingless: each step
  MUST be uniquely derivable — by written name (exactly one reachable
  role matches on declared name or plural, filtered on the endpoint
  type where the step is the last: the v0 by-name derivation
  disambiguates same-named roles by target type; its best-effort
  pluralisation is deliberately not mirrored — an unmatched written
  form refuses loudly instead of guessing a singular, a tracked delta)
  or by type pair (exactly one fact type relates the pair), or — the
  one exception, scoped precisely — the single written step is the
  zero-hop self-reference of the paragraph below and the walk is
  empty. In a scalar slot (`set` false) every hop's
  reached role MUST be to-one — a to-many step needs an aggregation,
  never a silent first pick (the v0 resolver builds only to-one chains;
  its NAAR_VEEL marker, lifted to well-formedness). Bindings on a
  reference or check that does not navigate are ill-formed. Semantics:
  `RUNTIME-SEMANTICS.md` ch. 5.8 — an absent link at any hop reads the
  operand as leeg; the navigated characteristic check on an absent link
  is false, negated or not.

**The zero-hop self-reference.** A definite reference may name the
subject by a role the subject ITSELF fills ("het strandtegoed van de
aanmelder", read on the Jutter who *is* the aanmelder): the reference
then denotes the subject and its walk is **empty** — zero hops, the
own attribute, read unconditionally (no link is consulted). On the
wire this is a single bindingless written step (`navigatieRol`; there
is no hop, so there is nothing to bind — a chain step or a binding is
never a self-reference) whose endpoint type equals the subject type,
and it is well-formed **iff** both hold: the step does not resolve as
a far role (zero hits under the by-name derivation above, endpoint
filter included), and the name matches, case-insensitively on declared
name or declared plural, a role the subject type itself fills in some
declared fact type (the WF-083 self-denotation test). The resolution
ORDER is normative: far-role resolution comes FIRST, and the
self-reference applies only at zero far hits — a reciprocal relation's
role name ("zijn strandmaat", on the type that fills that very role)
resolves as a far role and stays a real hop, never a self-read. Two v0
fall-throughs are deliberately NOT mirrored (tracked deltas in the
safe direction, `CONSTRUCTS.md` row 4): v0 reads the own attribute for
ANY unresolved written name on a same-typed reference — v1 demands the
self-denotation test, so a same-typed reference naming a vreemde rol
stays ill-formed (negatief `wf053-zelfverwijzing-vreemde-rol`) — and
v0 falls through on an AMBIGUOUS far match as well, where v1 keeps the
loud ambiguity refusal. The characteristic check's `navigatie` is NOT
in scope: v0's resolver never produces a self-referencing navigated
check, so its one written hop stays a real hop and a self-denoting
name there remains ill-formed — both executors refuse it explicitly.
Semantics: `RUNTIME-SEMANTICS.md` ch. 5.8 rule 4.

Emitter discipline (the pin-bump contract): an emitter binds the
**scalar navigated read and the navigated characteristic check only** —
a fold-source argument never carries `navigatieBindingen`, its relation
is the fold's own `bronRol` (mirroring the v0 emitter's
`bronRolBinding`, which is null exactly when the argument navigates).
WF-053 enforces this: bindings on a fold source are ill-formed.

Dependency discipline (ch. 6.1 of `RUNTIME-SEMANTICS.md`): a navigated
read consumes the link set of every hop relation — the edge that orders
navigation after relation creation — and a navigated characteristic
check reads the characteristic **on the reached type**, negatively when
negated.

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
| `filter` | Condition? — the die/dat-subselection, evaluated PER CONTRIBUTOR (the element is the condition's subject; a leeg operand fails it, ch. 1.2d; an all-excluding filter leaves an empty source — the sum fallback fires, a count reads 0). **Landed (2026-08-19):** executes in both executors (`jutterswet-strandgoed`, an emitter golden; `RUNTIME-SEMANTICS.md` ch. 3.4a). The reference interpreter carries the full condition set; the ASP backend's corpus-exercised fragment faults loudly on the rest — the standing executor asymmetry, differential-gated over the corpus. The SAME field also carries the §7.1 gedurende timeline clip (on the temporal total, where the element type is the subject itself) — a timeline operation, never an instance subselection. **Landed (2026-08-19, WF-055):** the clip executes in both executors (`jutterswet-getijdenwacht`; `RUNTIME-SEMANTICS.md` ch. 3.6): intersect the full value timeline with the condition's true-periods (an own temporal characteristic, or a per-segment comparison against a scalar bound), then total whole units per clipped segment — floor, never pro-rata; a gap in the condition clips away; a fully clipped-away or absent timeline totals 0, never leeg. The negated clip (the complement) and the Gelijk/Ongelijk comparison clip execute in the reference interpreter only — the ASP side faults loudly on both. A negated characteristic subselection never reaches the wire executable (v0 marks it Onopgelost) |
| `emptyFallback` | expression? — **sum only**: the §5.8.2 "of … als die er niet zijn" clause |
| `dimensionSelection` | ? — **sum/min/max only**: fold over an ordered label selection instead of instances (dialect, row 28) |
| `sourceRole` | RoleBinding? — **extension field** (v0 wire `bronRol`), instance folds only: the connecting relation, with **the role whose filler instances form the folded set bound**; the opposite role's filler is the fold's subject type (WF-047). Absent, WF-048's unique derivability applies; a binding on a non-fold is ill-formed (WF-047) |
| `type`, `source` | — |

Empty-source results differ per kind — the som/totaal split and the
no-fallback rule for min/max/first/last are `RUNTIME-SEMANTICS.md`
ch. 1.2, mirrored in the matrix (row on TOTAAL, `CONSTRUCTS.md` ch. 2.1).

**The self-denoting fold source (WF-083).** The written form "het aantal
`<rol>` van de `<onderwerp>`" may name the fold subject by its
OWN role name ("het aantal vondsten van de aanmelder", evaluated on the
Jutter who *is* the aanmelder). The v0 wire then carries that written
role in the fold-source argument's `navigatieRol` — ch. 4.1a's field,
but here without its meaning: it is presentation of the written form,
not navigation. No hop happens, the fold stays anchored on its subject,
and the v0 production runtime ignores the field on this argument. A
fold-source argument may therefore carry `navigatieRol` **iff** the name
denotes the subject: it matches, case-insensitively on declared name or
declared plural, a role that the argument's object type — the fold
subject — itself fills in some declared fact type. Anything else on a
fold-source argument would be navigation, which a fold source never does
(its relation is `sourceRole`; ch. 4.1a's emitter discipline).

- **WF-083** A fold-source argument MUST NOT navigate: a non-empty
  `navigatiePad` is ill-formed, and a `navigatieRol`, when present, MUST
  be self-denoting — it names a role (ci, declared name or declared
  plural) that the argument's object type itself fills. An away-pointing
  `navigatieRol` (naming only roles filled by other types, or no
  declared role at all) is ill-formed. Semantics: `RUNTIME-SEMANTICS.md`
  ch. 3.4a — the self-denoting count folds the subject's own linked
  instances exactly as the bare subject-anchored form does.

**The fold-source role name and the type-pair fallback (WF-034).** The
v0 instance-fold source (the row-1 wart) writes a role name in its
argument's `attribuut` slot. **Where the exception applies is itself
normative:** the wart is the fold-source argument carrying the *unknown
sentinel* on its own `type` — that is how v0 spells it, since a role
name has no datatype to give, and it is the discriminator **both OCaml
executors** already key their role-naming branch on (`ref`'s contributor
match, the ASP backend's count arm). The Drools backend does not: it keys
the same reading on `kind = AANTAL` plus its own role derivation, so it
plays a source carrying a real datatype where those two refuse — measured,
and one more reason this discriminator is stated normatively here instead
of being read off engine agreement. A fold-source argument carrying
a real datatype reads an attribute value, not a role, and resolves as an
ordinary attribute reference (WF-034, ch. 4.6 (l)); neither of the two
routes below reaches it. Within the wart, resolution order is normative:
the name resolves FIRST, case-insensitively over the declared role name
and the declared plural — never a synthesized plural (the ch. 2.6
decision).
A name that resolves to no declared role does not by itself make the
fold ill-formed: when the type pair derives a **unique connecting
role** — exactly one fact type connects the fold subject type to the
element type, and it is not a self-typed two-role fact type (whose two
roles are both candidates and so block the derivation, ch. 4.1) — that
role is the source, the same unique derivability WF-048 already demands
of the bindingless fold, so the unresolved written form degrades to
presentation. Uniqueness is counted in candidate **roles**, not fact
types: v0's `verbindendeRol` counts every role of the far type whose
fact type also relates the near type, and a lone self-typed two-role
fact type offers two. This is the narrowed mirror of v0's source
scoping, which matched best-effort (synthesized plurals included) and
fell back to the type pair: v1 keeps the deterministic fallback and
drops the spelling guess. A name that resolves to no declared role over
a type pair that derives no unique connecting role — zero or several
connecting fact types, or a single self-typed two-role fact type — is
ill-formed (WF-034); a name matching several reachable roles stays a
loud executor fault, never a guess.
Semantics: `RUNTIME-SEMANTICS.md` ch. 3.4a; the ASP backend keys the
count by the connecting role's canonical name, never by the written
lexeme.

**BuiltinCall** — scalar, text, and date builtins with plain arguments:
wortel, absolute-waarde (core kernel); concatenatie (text fold);
tijdsduur/absolute-tijdsduur (two date operands, result unit declared);
tijdsevenredig-deel and aantal-dagen-in (each carrying a `granularity`
bucket ∈ month/year — tijdsevenredig-deel MUST carry a gedurende
condition in `filter` and one temporal source argument as coarse as or
coarser than its bucket, and on a temporal target the bucket MUST equal
the target's declared granularity, WF-056; semantics ch. 3.7 of
`RUNTIME-SEMANTICS.md`); eerste-paasdag, datum-met, datumtijd-met, and the
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
pinned (WF-040); no storage layout implied. `attribute` is an attribute
reference and nothing else — neither absent nor any other expression node
(WF-034, ch. 4.6 (g)). The cell carries the
attribute's datatype, so the node's own `type` is a use site of that
declaration under WF-089 (ch. 2.4), beside the inner reference's.

### 4.3a TemporalRead — the point-in-time read

`layer: core`; an **extension node** per the Method (wire node
`TemporalRead`; never emitter-produced — the decided row-11 delta,
`RUNTIME-SEMANTICS.md` ch. 3.3). The primitive `read(tl, d)` of the
timeline algebra, exposed to the rule author: the value a timeline
carries at a day-precision instant that need not be the evaluation date
(the art. 1 shape — "the value at the child's birth" — and ch. 8's
vintage read).

| Field | Type | Meaning |
|---|---|---|
| `basis` | expression | names the timeline: an own temporal attribute reference, or a granular parameter reference. Never evaluated as a scalar (that is the refused ch. 3.3 shape) |
| `datum` | expression | the day-precision date index |
| `type` | datatype | the timeline's element datatype; required, non-null |
| `source` | — | v0 wire `bron` |

Semantics (`RUNTIME-SEMANTICS.md` ch. 3.1/3.3): the value of the
segment containing the index; **leeg on a gap**; an **empty date index
reads to leeg** (emptiness propagates, ch. 1.2). The data plane owns
ch. 8's fail-fast (Parameterset coverage is a session-start check, not
part of the read). A case serves a read only with the **full timeline**
value variant (`uchar-case-v0`); the asOfDate-bucket scalar convention
of the temporal total cannot serve a point read — executors fault,
never guess.

- **WF-049** The `basis` MUST be a navigation-free reference to a
  declared temporal attribute of the subject type or to a granular
  parameter (a temporal *characteristic* operand is **OPEN** — the case
  format carries no characteristic timelines yet); the `datum` operand
  MUST carry the day-precision date type (millisecond precision is
  ill-formed); the node's `type` MUST equal the timeline's element
  datatype.

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
| CharacteristicCheck | `characteristic`, `navigation?` (to-one role), `negated`, `wholePeriod?`, `source` | **Delta (`CONSTRUCTS.md` ch. 2.2):** v0's `eigenaar` pattern-steering field (a Drools attachment hint set only on fact-creation guards) is dropped. **Landed (2026-08-18):** the navigated check carries its one hop symbolically in the `navigatieBinding` extension field (ch. 4.1a, WF-053); semantics `RUNTIME-SEMANTICS.md` ch. 5.8 — an absent link is false, negated or not |
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
  dimension names and labels, day kinds, and referenced rules. The one
  scoped exception is the fold-source role name (ch. 4.3), and it is
  scoped to the **v0 instance-fold source** — the row-1 wart, spelled on
  the wire by the unknown sentinel on the argument's own `type` and keyed
  on exactly that sentinel by the reference interpreter and the ASP
  backend (the Drools backend keys the same reading on `kind = AANTAL`
  and its own role derivation instead — ch. 4.3). There a written name that
  resolves to no declared role (ci, declared name or declared
  plural) still resolves through the type-pair fallback **iff** the type
  pair derives a unique connecting role — exactly one fact type connects
  the fold subject type to the element type, and not a self-typed
  two-role fact type (which offers both its roles, blocking the
  derivation, ch. 4.1); zero or multiple connecting roles leave it a
  dangling reference — ill-formed. **Outside** the wart a fold-source
  argument is an ordinary attribute reference and resolves like one:
  neither the role-name route nor the type-pair fallback reaches it
  (ch. 4.6 (l)). *v0 mirror
  note:* fail-soft throughout — unresolved references degrade to the
  unknown-typed sentinel or an Unresolved node, warned at best. **Delta
  (promotion):** in v1, "could not resolve" is either an explicit
  Unresolved node (honest) or ill-formed IR (a dangling reference in a
  structured node) — never a sentinel type inside an otherwise-structured
  node; the unknown datatype does not exist in v1 (ch. 2.4). The rule
  covers the unknown type in both its spellings: a value node whose static
  type is the explicit Onbekend sentinel and its unspelled twin, a value
  node whose static type is plain null, are refused alike — the sole
  structural exception is a Lambda, which is never a value and carries no
  type by construction (its position is governed by WF-069).
  **Widened 2026-09-04** (maintainer decision), closing three measured
  channels through which a document could *serve itself* an exemption from
  the identity family (WF-089/WF-090/WF-091), each carried by **zero**
  documents — 152 golden + 22 rood + 237 negatief + 30 parity, measured —
  so no clause can over-reject. First: an
  `AttributeReference` MUST name the **`objecttype`** its value lives on
  (ch. 4.1a); a reference naming none resolves against no declaration, and
  the absent field is not one rule's deferral but a generic exemption — it
  silences the member check, WF-053, WF-080 and the whole identity family
  at once, and the shape is measured **answer-changing**: a `t`-for-`kg`
  annotation on such a reference was verifier-clean while `ref` and the ASP
  backend faulted on the missing field and the Drools backend played the
  document and answered off the lie. Second: the same requirement holds in a
  **fold source**, and needs its own clause, because the aggregation branch
  replaces the generic walk for that argument and resolves only a *named*
  endpoint — there the empty field fell through both fences at once, and the
  same `t`-for-`kg` shape was not merely played by one executor but by
  **all three**: `ref` and the ASP backend replay it to the pinned
  expectations and Drools plays it too, a tonne-typed sum answering in
  kilogrammes with nothing refusing anywhere. Third: the sentinel is refused
  on a rule **TARGET's `type`** as well — the one position the expression
  walk does not enter (the lvalue is WF-022's), and the last place it
  survived outside the documented row-1 fold-source wart. No executor reads
  the target field (all three write
  through the DECLARED datatype), but the identity family crosses it
  (WF-091's rule-target arm) and defers on the sentinel, so writing it there
  switched the unit, precision and domain checks off one's own write
  targets.
  **Widened again 2026-09-04** (same decision, second wave), with the four
  shapes the first wave measured and left open — each, like the first three,
  carried by **zero** documents outside its own negatief witness (152 golden
  + 22 rood + 238 negatief + 30 parity, measured before and after), so no
  clause can over-reject. (a) An `AttributeReference` MUST name the
  **`attribuut`** it reads — the other half of the resolving pair, silencing
  the same rules from the other side, and needing the same TWO arms as the
  first half (the fold source never reaches the generic walk: with the
  value-position arm alone, 43 of the goldens' 751 emptied `attribuut`
  positions stayed verifier-clean, all of them fold-/aggregation arguments;
  with both arms, none of 751). (b) A `ParameterReference` MUST name its
  **`parameter`** — the same shape one node kind over (35 of 37 positions
  survived emptying, now none). (c) The unspelled twin on a rule **target**,
  a target with **no `type`** at all (295 of 295 survived, now none): the
  first wave left it out for want of a witness, and it is the more dangerous
  half — besides WF-091's rule-target arm it also switches off WF-007's
  target clause, and the document it produces replays CLEAN in `ref` and
  clingo while the Drools backend throws on it, so the verifier is the only
  place it can be caught before an engine crashes on well-formed input.
  (d) The sentinel is refused on a `SumType` **constructor payload** inside a
  value node's type: the fence was a match on the type's top level, and a
  datatype is a tree — the walk now descends through payloads, the descent
  WF-088's use-site check already makes for the same reason (the consumers
  read the use site's inline constructor copy, while WF-062 crosses only the
  declaration). *Measured at the wave-2 corpus state (2026-09-04)* by
  injecting the sentinel at all 2758
  datatype-valued positions of every golden then present (2650 in the model
  and rules sections; `functionDefs`, `parameters` and `consistenties`
  carry the remaining 108) — the denominator is that state's, and a later
  count over a grown corpus or a differently drawn position set will differ;
  what is invariant, and re-confirmed in the fix round, is the numerator:
  36 injections stayed
  verifier-clean, of which 15 are no-ops (the position already holds the
  sentinel by construction — a count fold's role source, the row-1 wart), so
  21 real survivors: 12 on constructor payloads, which (d) closes, and 9 on
  fold-/aggregation argument types (8 element-passing aggregation operands,
  1 true instance-fold source), which stay open — the aggregation branch
  replaces the generic walk for those arguments. The second wave's descent
  covered the sum-in-sum chain only; the CONTAINER half is (h) below.
  *Measured and
  still open at the same twelve positions:* WF-007's precision-form check,
  which is a per-datatype predicate rather than a walk (`{"geheel": true,
  "decimalen": 2}` on a payload stays verifier-clean at all twelve); its
  blind spot spans list elements and struct fields as well, so sharing this
  descent is not one movement.
  **Widened again 2026-09-04** (same decision, third wave), with the four
  channels the second wave measured and left open, plus the fix round's (i).
  Over-rejection is settled
  by direct measurement rather than by argument: the verifier's verdict over
  all **462** documents that predate this card (153 golden + 22 rood + 257
  negatief + 30 parity) is unchanged by these arms; the **eleven** new
  negatief witnesses the card and its fix round add — taking the negatief
  zone to 268 and the four zones to 473 — are the only additions. The only
  pre-existing documents whose report changes at all are four minimal
  allocation negatiefs, which gain a truthful
  incidental line and keep both their verdict and the rule their directory
  declares. (e) A **characteristic check** (ch. 4.5) MUST name its
  `kenmerk`. It is the only field the node resolves through, and behind it
  sit four readers at once — this rule's declared-characteristic cross,
  WF-043's granularity cross for a whole-period qualifier, WF-053's
  navigated hop, and the dependency graph's edge from the reading rule to
  the characteristic. 89 of the goldens' 96 check positions (twelve
  position shapes over five top-level sections) stayed verifier-clean when
  emptied one at a time, and none does now; `ref` and the ASP backend both
  fault at run time on the emptied field. (f) An **allocation** (ch. 3.2)
  MUST name its `bronAttribuut` and its `doelAttribuut`. This is the one
  reference pair the expression walk cannot reach — an allocation is not an
  expression — so the clause lives beside the walk rather than in it; with
  either field empty, WF-025's ordering-key type check, WF-026's same-target
  comparison, WF-050's overlap with a defaults group and the stratification
  edges keyed on the pair all fall silent together. 11 of 11 golden
  allocations survived emptying either field, and none does now; both
  executors fault on the field's own name. The two wider movements this
  clause left open — a name that no declaration *carries* on any of the six
  fields, and an empty `bronType`/`doelType` — are closed by (j) and (k)
  below. (g) A **dimension-cell read** MUST hold
  an attribute reference in its `attribuut` position — refused in BOTH
  spellings, an absent field and a non-reference node, because the cell arm
  resolves only through a complete inner `AttributeReference` and an empty
  field misses that pattern by being absent while any other node misses it
  by being the wrong constructor. The reference frontend's IR types this
  field as an attribute reference, so its own emissions cannot reach the
  second spelling; the clause is for the wire, which any conforming producer
  may write, and that is the argument that also covers producers this corpus
  has never seen. Either way the cell's OWN type goes
  uncrossed by the identity family and WF-040's label/dimension cross goes
  with it; the harm is pinned on existing artifacts — **four** of them, each
  measured under both spellings with the verifier of the day before this
  card: `wf091-dimensiecel-eenheid` (unit lie), `wf089-dimensiecel-enum`
  (keuzetype-domain lie), `wf090-dimensiecel` (struct-domain lie) and
  `wf040-unpinned-cell` (an unpinned dimension plus a sign lie) each turn
  `verifier: PASS` — the whole refusal gone — when the cell's `attribuut` is
  emptied or overwritten with a literal. WF-040 travels with the identity
  family here, which is why the loss is a label/dimension cross as well as a
  type cross. This is the one wave-3
  channel with **no executor witness under today's executors**: a cell read
  is declined as a FORM by both OCaml executors, so no golden carrying one
  has or could have a case file today, and the argument for the clause is the
  identity family's silence, not an engine divergence. (h) The **container half** of
  (d): the sentinel is refused in a `ListType` element or a `StructType`
  field nested under a constructor payload, and in a sum reached back
  THROUGH such a container. The descent is mutually recursive over the whole
  payload subtree now; the sum-chain wordings (d) pinned are unchanged. Three
  hand-built witnesses on a sum-typed golden measured the gap the same way
  (d)'s did — `ref` replays each to its pinned expectations, the ASP backend
  refuses to translate it — and no document **predating this card** carries a
  container payload at all in any of the four zones, so the new branches
  cannot over-reject; the only container payloads anywhere are the card's own
  three witnesses (two `ListType`, one `StructType`, one of them reached back
  through a container into a sum), which is what they are for. The three
  witnesses are separately falsifiable: the branches are the list element,
  the record field, and the container→sum re-entry, and only the last one
  pins `wf034-sentinel-payload-onder-lijst` on its own.
  **Fix round 2026-09-04.** (i) A present attribute name on an
  **allocation** is not blank. (f) closes the null half of the same defect;
  every consumer of these fields is written as `Some a`, which reads as "a
  name is there", and `""` satisfies it while naming nothing — so the empty
  string walked through (f) into exactly the same silence one layer on. The
  clause covers all SIX name fields at once — `bronAttribuut`,
  `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut`, `restantAttribuut`,
  `groepeerAttribuut` — with emptiness decided by the ch. 2.4 name identity
  (whitespace collapsed, lowercased), so a blank spelling is the same empty
  name. Absence keeps its meaning where it carries one: null on the four
  facet fields is a facet switched OFF and stays legal, and
  `ratoAttribuut: null` under `NAAR_RATO` is already WF-025's business.
  *Measured, one field blanked at a time over the golden allocations, every
  position verifier-clean before and none after:* `bronAttribuut` 11 of 11
  and `doelAttribuut` 11 of 11 and `restantAttribuut` 9 of 9, on all of which
  both executors fault loudly; `ratoAttribuut` **7 of 7** and
  `groepeerAttribuut` **3 of 3**, on which BOTH executors play on and agree
  on a wrong answer (an ordered allocation loses its rank and pays every
  recipient the same 300 where 1200/0/0/0 is pinned); and
  `maximumAttribuut` **4 of 4**, on which `ref` plays on uncapped and returns
  600 for a pinned 300 while the ASP backend refuses to translate — ref and
  clingo **diverging** on a document the verifier had called well-formed.
  Those three rows are the one place in this rule where the differential
  square is structurally blind and only the hand-pinned anchor expectations
  catch the defect, which is the whole argument for a document-layer fence.
  **Fourth wave 2026-09-04.** The naming class the first three waves kept
  measuring and leaving: not a field left empty, but a field **filled with a
  name that resolves against nothing**. (j) Every filled attribute name on an
  **allocation** MUST resolve against the declaration scope of **its own
  side** — `bronAttribuut` and `restantAttribuut` against `bronType` (the pot
  that is distributed, and the remainder written back on the source),
  `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut` and
  `groepeerAttribuut` against `doelType` (the share written on each
  recipient, and the three facets read per recipient). (f) asks whether the
  field is there and (i) whether it is more than blanks; (j) asks the
  question the rule is about. An *undeclared* type is another rule's business
  (WF-047 on the bound form, WF-048 on the bindingless one, both **rules**
  measured loud — the executors are not: an undeclared `bronType` leaves `ref`
  silent, every share *leeg* where 300/200/300 and a 400 remainder are pinned,
  while the ASP backend refuses; only an undeclared `doelType` faults in both),
  so the clause resolves only where there is a scope to resolve in. A name that
  is a declared **characteristic** of the scope type falls under this clause
  too, and is reported as WF-034 rather than the WF-080 its sibling reference
  positions report — those split the two member kinds because a characteristic
  in a value slot carries truth, not a value, while an allocation field asks
  only whether the name is a declared *attribute*. Measured harmless: with a
  characteristic declared on either side of `jutterswet-berging`, all five
  fields that golden fills were refused when pointed at it, 5 of 5 — a
  deliberate difference in wording, recorded here rather than left to be
  found.
  *Measured, one field at a time pointed at a name no declaration carries,
  every position verifier-clean before and none after:* `bronAttribuut` 11 of
  11, `doelAttribuut` 11 of 11, `restantAttribuut` 9 of 9 — all loud in both
  executors; `ratoAttribuut` 7 of 7 and `groepeerAttribuut` 3 of 3 — BOTH
  executors silent and agreed (the shares stay underived where 60 and 40 are
  pinned; the ordering collapses and every recipient is paid the same 300
  where 1200/0/0/0 is pinned); `maximumAttribuut` 4 of 4 — `ref` silent and
  uncapped (600 for a pinned 300) while the ASP backend refuses to translate.
  The observability rows are (i)'s exactly, one spelling on. (k) An
  **allocation** MUST name its `bronType` and its `doelType`. The
  empty-reference class of (a)–(f), one field further along: an *undeclared*
  type is caught by WF-047/WF-048, but both of those read a present name, and
  a null takes with it WF-025's ordering-key datatype, WF-050's defaults
  overlap, the dependency graph's read and write edges, and (j)'s own
  resolution scope. 11 of 11 golden allocations survived emptying either
  type, and none does now; both executors fault by the field's own name
  (`missing bronType`, `missing doelType`). (l) A **fold-source argument
  outside the row-1 wart** MUST resolve as an ordinary attribute reference:
  the ch. 4.3 role-name route and the type-pair fallback behind it are scoped
  to the v0 instance-fold source, which the wire spells with the unknown
  sentinel on the argument's own `type`. Both branches were measured live and
  both are separately falsifiable. The type-pair branch, on
  `corpus/golden/anchor/jutterswet-maatschap` — whose only fact type is a
  reciprocal self-relation over `Jutter`, so the pair derives one connecting
  role for *any* name whatever: a `SOM` source repointed at `bestaatniet`
  stayed verifier-clean while `ref` silently summed 0 where 12 and 30 are
  pinned on an anchor golden and the ASP backend refused to translate. The
  role-name branch, on `corpus/golden/jutterswet-vondsttelling` — where the
  pair `Vondst`/`Vondst` derives *no* connecting role, so only the role route
  can admit anything: the same `SOM` source repointed at `vondsten`, the
  declared plural of a role in the document's one fact type, stayed clean
  while `ref` summed 0 where 3.75 kg is pinned and the ASP backend refused.
  A wrong answer and a refusal on one document, twice over. *Regression
  boundary, counted over all four zones:* of 89 named fold sources — *named*
  is the argument spelling both its `attribuut` and its `objecttype`, the pair
  this clause resolves through; count in the one source that spells an
  attribute and no object type (a wave-1 shape, refused there) and the totals
  read 90 and 58 — 32 carry
  the wart — 24 resolving through a declared role name, 5 through the type
  pair, 3 already ill-formed under this rule's older arms — and 57 do not; of
  those 57, 54 already resolve as a declared attribute and one names a
  characteristic (WF-080's, unchanged), so the narrowing rejects nothing that
  was standing and the only documents it moves are its own two witnesses. The
  counting folds that lean on the fallback keep both routes:
  `jutterswet-kaveltelling`'s synthesized plural over a role with no declared
  plural (the corpus's one golden fallback user), and the declared-plural
  route of `jutterswet-aanmeldertelling` and `jutterswet-vondsttelling`; all
  three replay unchanged in both executors.
- **WF-080** A value-position `AttributeReference` MUST resolve to an
  **attribute** of its endpoint type, never a characteristic. A value
  position is where an expression yields a value: a rule/member value, a
  default consequence, a condition operand, and the source of a value-yielding
  aggregation (a `SOM`/`TOTAAL`/`MINIMUM`/… fold whose element values are
  folded — a count's source names a role, not a value, and is out of
  scope). A characteristic carries truth, not an attribute value; it is
  read through a characteristic check (ch. 4.5), never an
  `AttributeReference` in a value slot. *v0 mirror note:* the resolver's
  member lookup admitted either member kind (`has_attr || has_kenmerk`) — a
  tolerated leak the kenmerk exploration surfaced; v1 splits it, in both the
  generic value walk and the separate aggregation-source path. The rule
  TARGET lvalue is out of scope (WF-022 governs it), so a characteristic
  attribution's target is unaffected. **Delta (promotion).**
- **WF-035** Operator typing (mirror of the v0 error set): numeric
  operators' operands MUST be number-like; power operands MUST
  additionally be unitless; a date ± duration MUST use a time unit (the
  ch. 3.8 spelling table of `RUNTIME-SEMANTICS.md`) or declared day
  kind, with the date on the left of both plus and min (a duration-left
  operand is ill-formed — v0's typing opens its date branch only for a
  date-left operand, so plus does not commute), date ± date ill-formed,
  and a sub-day unit demanding a
  millisecond-precision date operand; additive operands MUST have
  compatible units (both
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
  take a whole, unitless year; the day-precision part extractors
  (jaar-uit, maand-uit, dag-uit) MUST take a date operand — either
  precision; the sub-day part extractors MUST take a
  millisecond-precision date; a sum's `emptyFallback` MUST be numeric;
  a tijdsduur's (either variant) two operands MUST be dates of one
  precision and its node type MUST carry a recognised time-unit or
  declared day-kind spelling (the ch. 3.8 table of
  `RUNTIME-SEMANTICS.md`), a sub-day unit demanding
  millisecond-precision operands.
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
- **WF-045** The rule-form dependency graph (ch. 5.1 — attribute,
  characteristic, channel, population and relation nodes; operational
  companion `RUNTIME-SEMANTICS.md` ch. 6.1) MUST be acyclic after
  removing the creation members of recursive rule groups: a dependency
  cycle is admissible only when it passes through recursive creation
  (ch. 5.3; mirror: v0 warns at build time and caps firing).
- **WF-046** No dependency cycle — including a WF-045-admissible one —
  may carry a negative edge (a negated or negation-quantified
  characteristic read, or a regelversie channel test): evaluation
  strata MUST exist (ch. 5.3; operational reading `RUNTIME-SEMANTICS.md`
  ch. 6.3). v0's single agenda boundary is the two-stratum special case.

### 4.7 Sum-type expressions — Construct, Match, BinderRef

`layer: core`; **extension nodes** per the Method (wire nodes
`Construct`, `Match`, `BinderRef`; CONSTRUCTS
row 36; semantics `RUNTIME-SEMANTICS.md` ch. 9; design and ratified
decisions `spec/proposals/PHASE5B-KERNEL.md` §2). `Construct` and
`Match` are never emitter-produced; `BinderRef` **is**, since
2026-09-01 — it is how the RegelSpraak emitter spells a read of a
§13.4.2.10 variabele under the `Let` chain of ch. 4.10.

| Node | Fields | Notes |
|---|---|---|
| Construct | `type` (the SumType), `constructor`, `argument?` (expr), `source` | builds a constructor value; a leeg argument propagates (the whole node evaluates leeg — a constructor value never carries a leeg payload) |
| Match | `type` (the result datatype), `scrutinee` (expr), `arms`, `source` | takes a **keuzetype** value apart (ch. 2.4) — the scrutinee is sum-typed *or* enumeration-typed. Each arm: `{constructor, binder: string \| null, body: expr}`; on an enumeration scrutinee `constructor` carries the declared value label and `binder` is always null (enumeration alternatives are nullary). Evaluation `RUNTIME-SEMANTICS.md` ch. 9.2 — a leeg scrutinee propagates, no implicit leeg-arm, in both variants |
| BinderRef | `name`, `type`, `source` | reads the nearest enclosing binder (a unary match arm's `binder`; the row-10 card adds lambda and function parameters; the 5c widening adds a `Let` binder, ch. 4.10 — which is what the RegelSpraak emitter now produces for a §13.4.2.10 variabele read). Deliberately distinct from `VariabeleRef`, which since the let-lowering card carries only what a `BinderRef` cannot: a row-24 variable reference the resolver could **not** bind (a forward or cyclic definition) — an honest residual marker, never a bound read; the two roles never share a node |

There are no wildcards: the arms enumerate the declared alternatives
literally (a wildcard would make totality a runtime property again).

`Match` is the family node — it deconstructs either keuzetype variant
(ch. 2.4). `Construct` and `BinderRef` are **sum-only**: an enumeration
value is built by an ordinary `Literal` carrying the label (WF-039
checks it against the declared value list) and an enumeration arm binds
nothing, so neither node has an enumeration reading.

- **WF-062** A `Construct` MUST name a declared constructor of its
  `SumType`; the `argument` MUST be present iff the constructor is
  unary, and its static type MUST match the declared payload datatype.
  A `Construct` whose `type` is an `Enumeration` is ill-formed —
  building an enumeration value is a `Literal`.
- **WF-063** A `Match` scrutinee MUST be **keuzetype-typed** — sum-typed
  or enumeration-typed (ch. 2.4) — naming a domain declared in that same
  variant, and the arms MUST cover exactly the declared alternatives of
  that domain — each exactly once, none missing, none alien. The
  declaration is the source of truth in both variants, never the use
  site's inline copy. On a **sum-typed** scrutinee the alternatives are
  the declared constructors and an arm MUST carry a `binder` iff its
  constructor is unary. On an **enumeration-typed** scrutinee the
  alternatives are the declared `values` (compared ci, the WF-001
  identity rule) and an arm's `binder` MUST be null — enumeration
  alternatives are nullary by construction, so there is no payload to
  name. **Delta (v1 widening, 2026-09-01):** the enumeration arm is what
  keeps a match over a pure choice expressible after the WF-087
  normalisation folds every all-nullary sum into an enumeration domain.
- **WF-064** Every `BinderRef` MUST resolve to an enclosing binder of
  its name, and all arm bodies of a `Match` MUST agree on one result
  datatype — the `Match`'s own `type`. (This rule governs the new
  binder forms — a match arm's binder, a lambda/function parameter and,
  the Phase-5c widening, a `Let` binder (ch. 4.10), scoped to the `Let`
  body; a `VariabeleRef` remains outside it, row 24.) **The closure
  clause:** inside a **closed** body — a lambda or function body
  (ch. 4.8/2.11), the bodies WF-069/WF-067 hold closed — the
  enclosing-binder set is *closed at the boundary*. A `BinderRef` there
  MUST resolve to the body's own parameters or to a binder introduced
  **inside** the body (a match arm's binder, an inner `Let`, a filter
  binder); a `BinderRef` that reaches **outside** the closed body — an
  enclosing rule's `Let` binder, or an outer fold's parameter, read from
  within the body — is ill-formed. This is the binder companion to the
  attribute, dimension, aggregation, temporal and variable reads
  WF-069/WF-067 already deny; a `Let` bound *inside* the body stays in
  scope (ch. 4.10). *Motivation:* such a read is otherwise
  verifier-clean yet splits the executors — `ref` faults cleanly on the
  unbound binder while the ASP backend drops the free binder into an
  unsafe rule and clingo grounding-crashes (exit 65) rather than emitting
  a clean TRANSLATION FAULT; both fault loudly (no wrong answer), so the
  clause is a robustness/diagnosis fence, weening the ill-formed IR at
  gate 3 ahead of either executor. **Delta (promotion).**
- **WF-078** A `BinderRef`'s serialized `type`, where present, MUST agree
  in **soort** with the datatype of the binder it resolves to — the
  leaf-ref cross-check WF-023/WF-027 perform for a resolved
  `AttributeReference`, applied to a binder's declared datatype (a match
  arm binder's constructor payload, a lambda/function parameter's declared
  type, a `Let` binder's value type, a filter binder's element type). The
  cross is by soort — number, text, boolean, date, sum type, list, struct
  — because soort is WF-078's **facet** of the reference-type identity,
  not the whole of it. Since WF-091 (ch. 4.1) the decimals, the unit, the
  wholeness, the sign and the residual structure are crossed at this arm
  too, exactly as at every other; what this rule once left "uncrossed
  exactly as the `AttributeReference` cross does" is crossed at both, and
  WF-078 is the number the soort facet carries HERE. Where the soort
  facet stands aside — either side names a keuzetype or a struct domain —
  the domain facet reports instead (below). A binder whose declared
  datatype is unknown, or a reference carrying no `type`, defers.
  *Motivation:* WF-035
  reads the same serialized `type` but exempts additive (`+`/`−`) operands
  so date ± duration can pass; a soort-lie on a binder in an additive
  position slips that exemption — verifier-clean, ref computes on, clingo
  faults the translation — the split the adversarial hunt found (the
  card). WF-078 crosses the annotation against the binder's real datatype
  at every use, closing the gap independent of operator position. The
  domain half of the same cross is WF-089 (ch. 2.4): where the
  `BinderRef`'s `type` or the binder's declared datatype names a
  keuzetype domain, both name the same one — and WF-090 where either
  names a struct domain, the arm the `wf090-binderref` case pins (a
  struct-typed `BinderRef` over a truthfully declared lambda parameter
  passed the soort cross, struct against struct, and had clingo fold 0
  for 12.50). Every remaining facet at this arm is WF-091's
  (`wf091-binderref-eenheid` pins the unit). **Delta (promotion).**

### 4.8 The fold family — ListLiteral, Lambda, IfThenElse, Invoke, Fold, MapList, FilterList, ListLength

`layer: core`; **extension nodes** per the Method (never
emitter-produced; CONSTRUCTS row 10; semantics `RUNTIME-SEMANTICS.md`
ch. 10; design and the ratified closed-kernel decision
`spec/proposals/PHASE5B-KERNEL.md` §3). Together with ch. 2.11 these
carry general folds and function values over list *values*; the
`Functie` aggregation catalogue (ch. 4.3) keeps its instance-set world
untouched — the two never merge in v1.

| Node | Fields | Notes |
|---|---|---|
| ListLiteral | `type` (the ListType), `elements` (list of expr), `source` | ordered; a leeg element evaluation propagates — the whole literal evaluates leeg (a list never carries a hole, ch. 10.1) |
| Lambda | `parameters` (list of `{name, datatype}`), `body` (expr), `source` | **position-restricted**: admissible only as the `function` operand of `Fold`/`MapList`; the body is closed (the WF-067 clause); parameters bind via `BinderRef` |
| IfThenElse | `type`, `condition` (Condition), `then` (expr), `else` (expr), `source` | **position-restricted**: admissible only inside lambda, function and (the Phase-5c widening) `Let` bodies (ch. 4.10) — bare rule expressions keep the defaults/voorwaarde idiom; the condition reuses the ch. 4.5 condition union (no boolean-typed expressions enter the IR), evaluated under the ordinary ch. 1.2 comparison discipline, its operands disciplined by WF-077; exactly the chosen branch is evaluated |
| Invoke | `type`, `function` (name), `arguments` (list of expr), `source` | calls a ch. 2.11 FunctionDef; strict — a leeg argument propagates without entering the body |
| Fold | `type`, `list` (expr), `function` (an inline `Lambda`), `init` (expr), `source` | left fold, order normative; the function takes `(accumulator, element)`; the operand MUST be an inline `Lambda` (WF-069) — a named function reaches a fold through a lambda body that `Invoke`s it |
| MapList | `type` (a ListType), `list` (expr), `function` (an inline `Lambda`), `source` | elementwise, order-preserving; the operand MUST be an inline `Lambda` (WF-069) — a named function reaches it through a lambda body that `Invoke`s it |
| FilterList | `type` (the source ListType), `list` (expr), `binder` (name), `condition` (Condition), `source` | keeps the elements whose condition holds with the binder bound — the aggregation-`filter` precedent (a condition per contributor), which is why this node carries a binder + condition rather than a boolean-bodied lambda |
| ListLength | `type` (whole Number), `list` (expr), `source` | the element count |

- **WF-066** Every `ListLiteral` element's static type MUST equal the
  literal's declared element datatype.
- **WF-068** An `Invoke` MUST name a declared FunctionDef; the
  argument count MUST equal the parameter count, each argument's
  static type MUST match its parameter's datatype, and the node's
  `type` MUST equal the declared `result`.
- **WF-069** A `Lambda` outside a `Fold`/`MapList` `function` operand,
  or an `IfThenElse` outside a lambda, function or `Let` body
  (the Phase-5c widening, ch. 4.10), is ill-formed;
  a `Fold`/`MapList` `function` operand MUST be an inline `Lambda` — a
  bare FunctionDef name is ill-formed (the mirror-safe route: a named
  function reaches a fold through a lambda body that `Invoke`s it, as
  the anchors do); a lambda's parameters MUST be typed and its arity
  MUST fit its host (`Fold`: two — accumulator, element; `MapList`:
  one); lambda bodies MUST be closed (the WF-067 clause — which the
  **Phase-5c widening** extends to admit `FieldAccess` and
  `StructLiteral`, so a map-over-struct-field lambda body is well-formed);
  an `IfThenElse`'s branches MUST agree on one datatype — the node's own
  `type`.
- **WF-070** The fold-family `list` operand MUST be list-typed. For
  `Fold`: the `init`, the accumulator parameter, the function body and
  the node's `type` MUST share one first-order non-list datatype (a
  list-typed accumulator is ill-formed — the ASP term-depth fence of
  the ratified design; a `SumType` accumulator is admissible and is
  what the `Reduce` lowering uses), and the element parameter MUST
  carry the source element datatype. For `MapList`: the node's `type`
  MUST be the ListType of the body's datatype. For `FilterList`: the
  node's `type` MUST equal the source's. For `ListLength`: the node's
  `type` MUST be a whole Number.

### 4.9 Record expressions — StructLiteral, FieldAccess

`layer: core`; **extension nodes** per the Method (wire nodes
`StructLiteral`, `FieldAccess` — never emitter-produced; Phase-5c core;
semantics `RUNTIME-SEMANTICS.md` ch. 11; design and ratified decision
`spec/proposals/PHASE5C-TUTORIAL.md` §2, question 1 = A).

| Node | Fields | Notes |
|---|---|---|
| StructLiteral | `type` (the StructType, strictly non-null), `fields` (list of `{name, value: expr}` in declaration order), `source` | builds a record value; a leeg field value propagates (the whole node evaluates leeg — a record value never carries a leeg field, the `Construct`/`ListLiteral` no-hole rule) |
| FieldAccess | `type` (the field's datatype), `base` (expr), `field`, `source` | reads one declared field of a record; a leeg base propagates (ch. 11) |

- **WF-073** A `StructLiteral` MUST name a declared struct type in its
  `type` and supply exactly that type's declared fields — each declared
  field once, none missing, none alien; each field value's static type
  MUST equal the declared field datatype.
- **WF-074** A `FieldAccess` `base` MUST be struct-typed, its `field`
  MUST be a declared field of that struct, and the node's `type` MUST
  equal that field's declared datatype.

Both rules cross a use site against the domain it **names**: WF-073 the
literal against the type it declares, WF-074 the field against the domain
the *base* claims. Neither looks at the declaration the value **comes
from or enters** — that is **WF-090** (ch. 2.4), and without it a record
of domain B travelled into an attribute of domain A and reached this very
field read, where ref faults and the ASP backend answers wrong (measured;
`corpus/negatief/wf090-*`). Because WF-073's field-value clause and
WF-074's node-type clause are equalities on the *whole* datatype, the
nested forms — a struct field value of another domain, a struct-typed
field read of another domain — are already theirs and WF-090 adds no arm
there.

Both rules resolve the *named* domain in `domeinen`, never the inline
field list the node carries — which is why WF-072's walk had to widen to
every expression `type` on the same day (ch. 2.4). Measured before it
did: a `FieldAccess` base whose inline copy renamed the declared fields
was verifier-clean and ref replayed it green, while the ASP backend
raised `field "waarde" is not a declared field of struct "dossier"
(WF-074)` — the backend enforcing against the inline copy a WF-074 the
verifier enforced against the declaration. The two now cross the same
field list because WF-072 pins the copy to the declaration.

The **WF-069 closed-body whitelist widens** (the Phase-5c widening):
`FieldAccess` and `StructLiteral` are admissible inside a closed lambda
or function body — the tutorial's map-over-struct-field
(`λ individual. individual.kindertal`, a `FieldAccess` on a binder) is
otherwise ill-formed inside a lambda body (the reads that break closure —
attribute, dimension, aggregation, temporal, variable — are unchanged;
the two record nodes join the admissible set, their own operand reads
still following the closed-body discipline).

### 4.10 The value-level binding — Let

`layer: core`; **extension node** per the Method (wire node `Let`;
Phase-5c core; semantics
`RUNTIME-SEMANTICS.md` ch. 10.6 — **call-by-need**, maintainer decision
O3 2026-09-01; design and ratified decision
`spec/proposals/PHASE5C-TUTORIAL.md` §6, question 2 = A).

**Emitter-produced since 2026-09-01** (the let-lowering card) — the one
kernel node that is: the RegelSpraak frontend lowers a §13.4.2.10
variabelendeel (`Daarbij geldt: X is …`) to **one `Let` per declared
variable that the rule's RESULT part reads**, nested in declaration
order (§11.1 rule 1, first-declared outermost), every read a
`BinderRef` (ch. 4.7) — so a variable read twice is one `Let` with two
reads and sharing is visible on the wire. **Three** lowering boundaries
follow from the wire's own shape and are normative for the frontend, not
for the node: (a) a variable read only in the **voorwaarde** is
*substituted* at its use site instead, because a rule's `condition` and
its value are separate fields and no expression node spans them. What
substitution costs there is sharing: a variable read twice in one
condition is resolved twice and therefore evaluated twice, where one
`Let` would evaluate it once (`RUNTIME-SEMANTICS.md` ch. 10.6 clause 1).
No corpus case observes the difference — the two agree on every value —
but they are not identical in theory: a definition whose evaluation
deposits a §9.5 channel reason would deposit it once *per read*, not
once; (b) a variable **no** read reaches gets no
`Let` at all — an unread binder evaluates nothing (clause 3 below), so
such a node would be a guaranteed no-op on the wire; (c) a variable read
anywhere inside a **shape-reading construct** — an aggregation (the
seven instance/dimension aggregations) or a §7 temporal operator — is
substituted **at that use site**, per use site, exactly as v0 did. Such
a construct resolves against the *shape* of its operand rather than
against its value: its element type, its set-ness, its dimension
selection, its "gedurende" clip and the right-hand side of a §5.6
subselectie comparison are all read off form. A `BinderRef` is an opaque
leaf, so lowering there would fail every such shape test and silently
degrade the construct to an unresolved marker — a **wrong answer**, not
a lost optimisation. The boundary is the **whole sub-tree**, not the
shape-reading positions within it: the frontend switches substitution
back on for the construct and leaves it on until the construct is built,
so a *pure value* position underneath — a §5.8.2 leegwaarde fallback,
say — is substituted too, though it decides nothing about shape. One
scope, no half-inlined sub-tree; and the consequence is normative, not
incidental: **inside** an aggregation sub-tree the v0 inlining-coalesce
still governs, so the tracked v1 divergence of `RUNTIME-SEMANTICS.md`
ch. 10.6 (delta 1, the leeg-barrier) is a statement about reads
**outside** such a sub-tree, worked there against an absolute value.
Boundary (c) costs sharing only; the same variable read
elsewhere in the same rule still binds. Witnesses:
`corpus/golden/jutterswet-lichterloon` (sharing),
`corpus/golden/jutterswet-vondstrecht` (the barrier the v0 inlining
does not have), `corpus/golden/synth3-takbarriere` (the unread
binder in the branch not taken) and
`corpus/golden/jutterswet-drempelvangst` (boundary (c) from both sides:
one rule whose variable is a subselectie RHS and keeps the v0
substitution, one whose variable is read twice in arithmetic and becomes
one `Let` with two `BinderRef`s).

A `Let` is an
ordinary expression, admissible **anywhere** an expression is (unlike
`IfThenElse`, which stays position-restricted). It carries the
tutorial's `let … in …` at rule level and everywhere else. That
admissibility is a well-formedness statement, and the executors meet it
unevenly: `ref` evaluates a `Let` in every position; the ASP backend
carries it in every ordinary term position and — as a guarded
consequence (S18) — in the bare rule value, while two shapes it does not
yet carry fault loudly rather than silently (ASP-TRANSLATION `S19`): a
`Let` inside a closed lambda/function body, and a `Let` whose body is an
`IfThenElse` outside the bare rule value.

| Node | Fields | Notes |
|---|---|---|
| Let | `type` (strictly non-null), `binder` (name), `value` (expr), `body` (expr), `source` | binds `binder` to `value` as a **need**: `value` is evaluated in the enclosing scope at the FIRST read of the binder and reused after (call-by-need); a read whose `value` is leeg makes the whole `Let` leeg (the barrier, at the read); an UNREAD binder evaluates nothing and the `body`'s outcome is the `Let`'s. The binder is visible **only** in `body`, never in its own `value` |

The barrier at the read is the load-bearing point: because a read either
yields a gevuld value or ends the `Let`, a `body` binder is **never
leeg**. That is the **leeg-barrier** — an underivable value simply
never binds at the read site, so ref's leeg propagation and clingo's
underivability coincide by construction (the 5b lesson applied forward).
It is what lets `IfThenElse` widen into `Let` bodies (WF-069) under a
condition that reads only bound-non-leeg operands (WF-077), keeping the
NAF-free exact-complement branch encoding honest. The contrast is
normative and worked in `RUNTIME-SEMANTICS.md` ch. 10.6: a bare `maal`
over a leeg attribute *coalesces* to 0 (the ch. 1.2 coalesce table's
`maal` row), whereas the same attribute bound through a `Let` and then
*read* propagates leeg — which is exactly why the discipline exists.

- **WF-076** A `Let`'s `type` MUST equal its `body`'s datatype; the
  `binder` MUST NOT be referenced within its own `value` (it is visible
  only in `body`); the `value` and `body` MUST be well-formed. The
  binder joins the WF-064 binder regime for the `body` (a `BinderRef`
  in the body resolves to it). These are structural rules; whether the
  `body` in fact reads the binder is a *semantic* question (ch. 10.6
  clause 3) and never a well-formedness one — an unread binder is
  well-formed.
- **WF-077** Everywhere an `IfThenElse` is admissible (a lambda,
  function or `Let` body), its condition may **read** only
  binders, parameters and literals — arithmetic combinators (`Binair`,
  `Afgerond`, `Begrensd`) over those are admissible, since what matters
  is what is read. An attribute, field, characteristic, aggregation,
  temporal or variable read as (or inside) a condition operand is
  ill-formed: it MUST pass through a `Let` binding first. This is
  the condition-operand discipline — the leeg-barrier stated as a
  well-formedness rule. Its rationale under call-by-need: a condition
  operand is a *read* of the binder, and the barrier sits at the read,
  so the operand it delivers is non-leeg by construction — the branch
  guard stays a definite waar/onwaar and its complement stays exact.

**WF-064 widens** (the Phase-5c widening): a `Let` binder joins the
enclosing-binder set a `BinderRef` resolves against (alongside a match
arm's binder and a lambda/function parameter), scoped to the `Let`
`body`. **WF-069 widens**: `IfThenElse` becomes admissible inside a
`Let` body as well as inside lambda and function bodies — a `Let` body
is *not* a closed body (it may read attributes; only the condition of
an `IfThenElse` within it is disciplined, by WF-077).

---

## 5. Stratification

A rule set is one system of equations (`RUNTIME-SEMANTICS.md` ch. 5);
this chapter states, **normatively**, the well-formedness that makes its
fixpoint well-defined in the presence of negation and creation: the
**dependency graph**, its three **edge kinds**, and the two illegal-cycle
rules (WF-045, WF-046). Stratification is a property of the IR, **checked
by the verifier** — never an engine agenda trick; it is the declared
replacement for v0's single "salience −1" boundary (`IR-REVIEW.md`;
`RUNTIME-SEMANTICS.md` ch. 5.3). `RUNTIME-SEMANTICS.md` ch. 6 is the
operational companion: the same graph and strata read off the statute,
with the per-form read/write table and worked evaluations.

### 5.1 The dependency graph

The graph has one node per derivable value, identified up to name
normalisation (ch. 2.1):

- **attribute** — object type × attribute;
- **characteristic** — object type × characteristic;
- **channel** — a rule name (version-stripped): the §9.5 error entries the
  named consistency/uniqueness rule can deposit
  (`RUNTIME-SEMANTICS.md` ch. 5.6);
- **population** — object type: the instance set an instance fold (ch. 4.3)
  or an allocation's recipient derivation reads, and object creation grows;
- **relation** — a declared fact type: its link set, written by the fact
  creations that create it and read by every symbolic hop, fold or
  allocation that names it in a role binding (ch. 3.4).

Which form writes and reads which nodes is the producer→consumer table of
`RUNTIME-SEMANTICS.md` ch. 6.1, whose rows the edge-kind rules below
govern. **Parameters and input-only nodes** — nodes no form writes —
contribute no edges. **Day kinds are not graph nodes** (ch. 5.4).

### 5.2 Edge kinds

An edge runs producer → consumer — from every form that **writes** a node
to every form that **reads** it — in one of three kinds:

- **positive** (the default): a read by an attribute-writing form
  (assignment, initialisation, allocation) and any positive characteristic
  read. Attribute writes are revocable — idempotent, re-evaluated under
  freshness (`RUNTIME-SEMANTICS.md` ch. 5.1) — so a transient self-corrects
  within a stratum, and a positive characteristic read is monotone-safe
  (ch. 5.3).
- **stratum-strict**: any non-characteristic read (attribute value,
  emptiness/gevuld predicate, comparison) by a form whose write is
  **irrevocable** — a characteristic attribution (monotone), a consistency
  or uniqueness (channel facts are never retracted), or a creation. Such a
  form MUST evaluate strictly above the producers of what it reads: a
  transient it acted on would persist. Within a WF-045-admissible
  recursive-creation cycle the strictness is discharged by the
  one-generation-per-instance progression (ch. 5.5), not a stratum boundary.
- **negative**: a negated characteristic read, a characteristic read under
  a negation-like quantifier (*geen*, *ten hoogste*, *precies* — their
  truth can be lost by a later monotone write), or a channel test
  (regelversie). **Once negative, conservatively negative** — a double
  negation does not cancel. A negative edge is stratum-strict *and*
  forbidden on any cycle (WF-046).

**The WF-051 defaults fold.** A defaults group (ch. 6;
`RUNTIME-SEMANTICS.md` ch. 7) folds into this graph as the writer of its
target: every default-justification read — and, on attribute defaults,
every consequence read (the gevuld gate makes it decision-bearing) — is a
**negative edge** toward the group's write, recursed through the nested
`excepties` sub-defaults (ch. 6.1; `RUNTIME-SEMANTICS.md` ch. 7.9); the
initialisation default's implicit input-emptiness justification reads
nothing. WF-051 names this defaults-specific rule; the WF-045/046
principles apply to the extended graph unchanged.

### 5.3 Acyclicity and stratified negation

- **WF-045 (acyclicity).** The dependency graph MUST be acyclic after
  removing every creation form — object creation or relation creation —
  that is a member of a recursive rule group (ch. 3.9): a cycle is
  admissible **only** when it passes through such a form. That is the one
  controlled exception — recursive creation advances exactly one generation
  per instance, WF-033 guards the author's termination obligation, and the
  runtime's bounded budget makes divergence a fault
  (`RUNTIME-SEMANTICS.md` ch. 5.5/6.2). With WF-045 and the single-writer
  disciplines (WF-026, WF-079) the fixpoint of the positive part is
  unique: rule, activation and insertion order do not matter.
- **WF-046 (stratified negation).** No dependency cycle — including a
  WF-045-admissible one — may carry a negative edge. Equivalently, **a
  stratum assignment exists**: give every form the smallest stratum such
  that along every positive edge the consumer's stratum is ≥ the producer's,
  and along every stratum-strict or negative edge strictly greater
  (strictness on an admissible creation cycle discharged by the generation
  progression, not a boundary). "No negative edge on a cycle" is exactly
  the stratifiability condition; the verifier rejects a program for which no
  assignment exists. Evaluation then proceeds by strata
  (`RUNTIME-SEMANTICS.md` ch. 6.3): a negated read sees a settled producer,
  a channel test a complete channel. v0's single agenda boundary is the
  two-stratum special case.

*v0 mirror note:* v0 warns on a circular reference at build time and bounds
execution with a fire cap; WF-045/046 make the refusal structural.

### 5.4 Day kinds are stratum 0

A day-kind definition (ch. 3.7) classifies a bare date; it is **input,
evaluated before the rule graph** — stratum 0 — and is therefore **not** a
dependency-graph node and carries **no** edges. Two well-formedness rules,
both at WF-031's home (ch. 3.7), keep that framing sound: a day-kind
voorwaarde is **pure** — it references only dates, date-components,
eerste-paasdag, literals, parameters and other day-kind names, never
derived object state (WF-031) — and the **day-kind reference graph is
acyclic** — a day-kind condition may reference other day kinds, but
mutually recursive definitions have no fixpoint (WF-085). No executor
evaluates a day-kind predicate: the predicate kind is refused (`ref/`,
`backends/clingo`), so a day kind is consulted, if ever, only as a
stratum-0 classifier of an input date, never as a derived read inside the
rule fixpoint. (Option A of the ratified stratification round,
`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §7; the diverge-from-v0
delta — v0 neither checks nor documents this — is recorded on WF-031.)

### 5.5 Executor neutrality

Stratification is an IR property **checked by the verifier**; a backend is
conforming when it produces the stratified result the graph mandates, by
either of two postures:

- it realises a **sound refinement** of the graph strata — any evaluation
  order that respects the edge constraints, a finer stratification being
  admissible (`ref/` walks the graph strata directly; `backends/clingo`
  lets the ASP stable-model semantics realise them,
  `RUNTIME-SEMANTICS.md` ch. 6); **or**
- it carries a **narrower stratifier plus a loud auto-refusal** for the
  forms that stratifier does not cover — it MUST refuse, with a reason,
  every document outside its covered fragment, and MUST NOT silently
  mis-order one.

The second posture is a **bounded, named** exception, ratified here
(`PRIORITIZED-DEFAULTS-EVOLUTIE` §7). Its instance is `backends/drools`: a
kenmerk-only salience for the negated-characteristic boundary plus a
**defaults-local** salience for the ch. 6 defaults (strictly below every
producer of every default justification), each guarded by per-form
auto-refusals — a loader Refusal
or transpiler marker naming the uncovered form, loudly excluded at gate 12,
never played mis-stratified. A backend realising a **coarser**
stratification than the graph mandates without refusing the uncovered forms
is non-conforming.

---

## 6. Defaults and defeasibility

The model is `RUNTIME-SEMANTICS.md` ch. 7: a first-class group of
**prioritized defaults** per target evaluating top-down with
**fall-through** — the highest default that *decides* wins (attribute
target: justification holds and the consequence evaluates gevuld, leeg
falls through; characteristic target: a justified default always decides,
attribute or do-not-attribute), so a single defaults group cannot conflict
and never writes leeg — initialisation as the lowest default,
priority either explicit (ordered defaults) or explicitly absent (a
declared-unordered default group on one target — the only
over-determination surface, routed to the §9.5 channel);
under-determination is leeg (a tracked Catala deviation,
`CONSTRUCTS.md` row 8).

### 6.1 Prioritized defaults (Phase 4: characteristic and attribute forms landed)

*Renamed 2026-08-29 from "ladder"/"rung"/"sub-ladder" (coined here
2026-08-15/16; no term of art). Git history — commit messages and
pre-rename revisions — uses the old names throughout.*

`layer: core`; an **extension form** per the Method: an optional
top-level list `prioritizedDefaults` of `PrioritizedDefaults` nodes —
never emitter-produced.

| Field | Type | Meaning |
|---|---|---|
| `name` | string? | v0 wire `naam` |
| `subject` | type ref | v0 wire `onderwerp`: the iterated subject type |
| `characteristic` | string? | v0 wire `kenmerk`: the target characteristic — **exactly one** of the two targets |
| `attribute` | string? | v0 wire `attribuut`: the target attribute |
| `defaults` | non-empty list of Default | **descending** priority: highest first |
| `unordered` | boolean | v0 wire `ongeordend`: declared-unordered (ch. 7.2 there) — multiple defaults groups on one target are admissible iff **every** one carries it; the group decides jointly, and differing simultaneous decisions route to the §9.5 channel (keyed by the target form; no write — the target keeps what the fact base holds) |
| `source`, `validity` | — | v0 wire `bron`, `geldigheid` |

**Default**: `decision` (v0 wire `besluit`, boolean? — characteristic
defaults only: attribute vs do-not-attribute), `justification` (v0 wire
`voorwaarde`, a condition; null only on the initialisation and
group-defaults), `consequence` (v0 wire `consequentie`, expression? —
attribute defaults only), `initialisation` (v0 wire `initialisatie`,
boolean — the ch. 7.3 default: justified iff the target is empty in the
input facts; lowest only, **root-only**), `exceptions` (v0 wire
`excepties`, the optional nested exception groups — the **Default.exceptions and
ExceptionGroup** paragraph below), `source`. Semantics:
`RUNTIME-SEMANTICS.md` ch. 7.1 —
per subject the highest default that **decides** wins: on a
characteristic every justified default decides (a false decision derives
nothing and displaces); on an attribute a default decides when its
justification holds *and* its consequence evaluates gevuld — a leeg
consequence **falls through**, a defaults group without a decision **writes
nothing** (the input value is left standing), and default evaluation is
speculative (only the deciding default's channel deposits persist).

**Default.exceptions and ExceptionGroup** (`RUNTIME-SEMANTICS.md` ch. 7.9,
Phase 5c — the below-root branching form): a default's `exceptions` slot
(v0 wire `excepties = { unordered, members }`, `ongeordend` + `leden`)
holds its **exception groups** — its `members` are `ExceptionGroup` nodes (v0 wire
`ExceptionGroup`: `defaults` — a non-empty default chain, descending,
highest first — and `source`). An exception group inherits the root target
and carries **none of its own**, and it may nest again via
`Default.exceptions` — the defaults group is a rose tree. Two default shapes: a **group-default**
(`exceptions`, no own condition/besluit/consequence — the exception groups
*are* its decision, carrying a multi-member base or exception group as a chain
position; a stray justification here is ill-formed, WF-075) and a
**form-a default** (own besluit/consequence too — the
exception groups decide first, its own consequence the fall-back when they
decide nothing). A default reconciles its exception groups by the same rule
the declared-unordered root group uses (ch. 7.2), recursively;
**differing** exception groups propagate the conflict to the **root** — no
write, one path-keyed §9.5 fact `subject.target/<default index path>`
(ch. 7.9, vraag 5 A). The initialisation and input defaults are
**root-only** — a sub-default is never one. `Default.required` is untouched,
so every existing document validates unchanged.

- **WF-050** A defaults group MUST carry at least one default and MUST
  target exactly one of a characteristic or an attribute, resolving on its
  subject. Default shapes MUST match the target kind (characteristic:
  `besluit`, no `consequentie`; attribute: `consequentie`, no
  `besluit`); the initialisation default MUST be the lowest, MUST carry
  no written justification, and MUST NOT sit on characteristic
  defaults. The target MUST have no other writer — no second defaults
  group, no rule, no allocation, and no object-creation initialisation on
  the same target: an unordered plain writer would silently defeat the
  group's decisions, and a creation-time write would void the
  initialisation default's input-emptiness justification (the ch. 7.1
  single-writer lift). Multiple defaults groups on one target MUST all
  carry the `unordered` marking — priority is explicit (ordered defaults)
  or explicitly absent (the declared-unordered group), never implicit. The
  default-shape checks (content per target kind; a non-init default carries
  a justification) **recurse into sub-defaults** (ch. 7.9, an exception
  group inheriting the root target kind); a group-default (`exceptions`, no
  own content) is exempt from the content and justification requirements,
  and a contentless default — neither content nor exception group — is
  WF-075's concern.
- **WF-086** A defaults group MUST NOT target a **temporal attribute** —
  one carrying a declared granularity (`attribuutGranulariteit`), whether
  anchor-only or on a named timeline — **nor a temporal characteristic**
  (one carrying `kenmerkGranulariteit`; the lens catch 2026-08-29: it
  would otherwise evaluate silently atemporally). A defaults group is
  atemporal: it decides once, at one fixpoint (`RUNTIME-SEMANTICS.md`
  ch. 7), and carries no timeline coordinate at which to write.
  **Executor parity:** the reference interpreter already faults on a
  temporal defaults write, and the Drools defaults service fences it as a
  reason-carrying skip; this rule refuses it FIRST, at the document layer,
  so ill-formed defaults never reach an executor — ref keeps its fault as
  defense-in-depth. *v0 mirror note:* the v0 emitter never produces
  `prioritizedDefaults`, so no v0 dump can carry this shape; the rule
  fences a frontend that emits prioritized defaults (the Catala plugin,
  ch. 6.1). Temporal defaults targets are not yet scheduled
  (`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §9 Q12a, ratified
  2026-08-29); the fence lifts, with witnesses, when they are.
- **WF-051** Every default-justification read — and, on attribute
  defaults, every consequence read (the gevuld gate makes it
  decision-bearing) — joins the ch. 5 dependency graph (ch. 5.1) as a
  **negative edge** toward the group's write (the ch. 5.2 defaults fold;
  operational companion `RUNTIME-SEMANTICS.md` ch. 6.1); the
  initialisation default's implicit input-emptiness justification reads
  nothing. No dependency cycle may pass through a defaults group — strata
  must exist. The WF-045/046 principles apply to the extended graph (a
  violating document typically trips those too; WF-051 names the
  defaults-specific rule).
  This rule is **unchanged** by the nested form: `dep_forms` folds every
  descendant read (justifications and consequences, recursively through
  sub-defaults) into the same root negative edges (ch. 7.9), so a cycle
  through a sub-default read is caught here.
- **WF-075** A default's `exceptions` field (`RUNTIME-SEMANTICS.md`
  ch. 7.9) MUST carry at least one exception group; two or more exception
  groups MUST carry the `unordered` marking (the root regime of WF-050,
  applied at every level — the flag rides the `exceptions` field itself). A
  sub-default MUST NOT be an initialisation (or input) default — those are
  root-only. Every
  default MUST carry its own decision/consequence, a nested exception group,
  or be the initialisation default — a contentless default decides nothing. A
  **group-default** (`exceptions`, no own decision or consequence) MUST
  NOT carry a justification (`condition`) of its own: its members decide,
  so a condition there is never read and would be silently ignored —
  such a default is neither a well-formed form-a default nor a well-formed
  group-default and is rejected. The rule recurses to any depth. (The
  content-KIND per target and the descendant-read folding land with
  WF-050 and WF-051; a differing exception group's root propagation is
  evaluation semantics, ch. 7.9.)

The v0 desugaring of existing rule forms into prioritized defaults is
`RUNTIME-SEMANTICS.md` ch. 7.7 (implemented as `Ir_v0.Desugar`, the
ASP backend's reading; witnessed differentially by the
`jutterswet-waardering-v0vorm` anchor); the bare computing rule
carries the defaults reading directly through the ch. 7.8 standing norm
(the shared `standing_targets` domain, anchor
`jutterswet-waardestelling`). Rule-readability of the conflict fact is
**closed** for v1 (`RUNTIME-SEMANTICS.md` ch. 7.2, decided
2026-08-20; reopening is additive).

---

## WF index

Format per rule: statement (in ch. 2–4), verifier check, golden case,
negatief case. `OPEN` = the artifact does not exist yet; an `OPEN` entry is
tracked debt under the spec discipline, and the Phase-1 exit requires every
row closed or its OPEN motivated in the row itself (three motivated OPENs
remain: WF-008 and WF-010 await a wire form or witness v0 cannot yet
produce; WF-019's negatief has no data-shaped counterexample). "gate 3" = implemented in `spec/verifier/` and run by
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
| WF-004 | dimensioned attribute: non-empty, duplicate-free, resolving dimension list | gate 3 | `jutterswet-vormenkabinet` (the dimensioned opbrengst) | `wf004-duplicate-dimension` |
| WF-005 | timeline facet: count ≥ 1; named adoption total and consistent | gate 3 (attributes; characteristic and parameter facets are under-represented in v0 — ch. 2.10) | `jutterswet-penningjaar` | `wf005-count-zero` |
| WF-006 | enumeration domain: ≥ 1 value, values unique | gate 3 | `jutterswet-vormenkabinet` (the kwaliteit domain) | `wf006-duplicate-enum-value` |
| WF-007 | number precision is exactly one of whole / fixed / unbounded — at every `Number` in the document: the declarations (model layer), every `type` the expression walk enters, and, since 2026-09-04, a rule TARGET's own `type`, the position the walk never enters. Measured and OPEN: the form check reaches none of a `ListType` element, `StructType` field, `SumType` payload, `FunctionDef` result/parameter or lambda parameter's `datatype`; two of those yield verifier-clean documents today (witnesses `jutterswet-postenheffing`, `jutterswet-waarderingsinvoer`), the other four are refused everywhere by neighbouring rules (WF-062/067/068/070/073/074/091); the enum walk recurses through lists, sums and structs, the struct walk only through lists, and a lambda parameter's `datatype` is reached by neither | gate 3 (model layer + the walk's type prologue + the rule-target arm) | `jutterswet-vondstnummer` (declarations) + `jutterswet-vondsttarief` (the rule-target arm crossing a `Percentage` target clean; 239 numeric-typed targets over 119 golden+rood documents, the anchor subset included, ride it) | `wf007-whole-with-decimals` + `wf007-doeltype-geheel-met-decimalen` (the rule-target arm, on a CHARACTERISTIC target — the window where the shape is observable, since the identity family defers there by construction; on an attribute target WF-091 masks it with a facet mismatch, measured) |
| WF-008 | enumeration compatibility = same declaring domain (use-time; expression layer) | OPEN (no document carries two enum operands in one expression; the WF-036 guard already covers the equality site — fold WF-008 into the expression layer when such a witness lands) | OPEN | OPEN |
| WF-009 | unit conversions reduce: no cycles, no unknown targets, positive rational factors | gate 3 (positivity and termination; exactness is not representable in v0's float factor) | `jutterswet-berging` | `wf009-conversion-cycle` |
| WF-010 | no restatement between non-convertible units (use-time; expression layer) | OPEN (v0 has no restatement node to anchor the check) | OPEN | OPEN |
| WF-011 | fact-type arity: reciprocal = 1 role, else ≥ 2 (>2 OPEN) | gate 3 | `jutterswet-vondsttelling` | `wf011-one-role` |
| WF-012 | role names unambiguous within a fact type (incl. plurals) | gate 3 | `jutterswet-vondsttelling` | `wf012-ambiguous-roles` |
| WF-013 | reciprocal role is to-one; other roles carry explicit cardinality | gate 3 (first clause; declared-vs-defaulted cardinality has no provenance in v0) | `anchor/jutterswet-maatschap` (a reciprocal fact type, folds replayed by gates 5–6) | `wf013-reciprocal-many` |
| WF-014 | dimension labels non-empty, duplicate-free, ordered | gate 3 | `jutterswet-vormenkabinet` (the seizoen labels) | `wf014-empty-labels` |
| WF-015 | one timeline declaration per normalized name; references resolve | gate 3 | `jutterswet-penningjaar` | `wf015-timeline-collision` |
| WF-016 | variable-start timeline is anchored (rule-layer check) | gate 3 (existence of the determination; the anchor-write typing is WF-032/WF-023) | `jutterswet-vormenkabinet` (the variable-start boekjaar with its start point) | `wf016-unanchored-variable-start` |
| WF-017 | day-kind spellings shadow no built-in unit and no other day-kind | gate 3 (day-kind names vs built-ins; duration-spelling collisions are pre-filtered by v0 before emission) | `jutterswet-vormenkabinet` (schouwdag) | `wf017-daykind-shadows-builtin` |
| WF-018 | parameter datatype resolves; timeline facet well-formed | gate 3 (datatype clause; the facet clause carries the WF-005 caveat) | `jutterswet-penningjaar` | `wf018-parameter-onbekend` |
| WF-019 | serialization preserves declaration order | `scripts/check-ir-schema.sh` (byte-identical round-trip) | `jutterswet-vondsttelling` (multi-element ordered lists round-trip) | OPEN (no data-shaped counterexample exists: the canonical re-emit is a serializer property, so a reordering fault lives in a consumer, never in admissible data — recorded) |
| WF-020 | rule names unique across all rule forms (ci) | gate 3 (via the v0 " #i" strip-and-group migration reading) | `jutterswet-vondstnummer` | `wf020-crossform-collision` |
| WF-021 | a named form's version validity windows do not overlap | gate 3 | `jutterswet-vormenkabinet` (two dated versions of one rule name); `anchor/jutterswet-jutheffing` (the replaying runtime witness: per-window absolute values through both executors, gates 5–6) | `wf021-version-overlap` |
| WF-022 | rule target resolves; characteristic value is the constant true | gate 3 | `corpus/golden/jutterswet-penningdrempel/` | `wf022-target-unresolvable` |
| WF-023 | assignment typing (family, units, sign, keuzetype domain, precision gate) at every attribute write position: a rule target (`GELIJKSTELLING`, `BEREKENING`, `INITIALISATIE`; `KENMERKTOEKENNING` is WF-022's), an object-creation attribute member, a defaults consequence (recursing into exception groups) | gate 3 (the shallow slice between the target's DECLARED datatype — its serialized type where the target has no declared attribute datatype: an unresolvable target, which WF-022 forbids, or a CHARACTERISTIC target, which WF-022 admits and whose value it already pins to the boolean constant true — and the value's serialized type: family (numeric kinds as one), unit compatibility with literal adoption, contradictory sign restrictions, and the keuzetype clause (same variant, same domain name, ci, through a `ListType` element; one-sided only within a family, where the family message would not already name the defect). The domain clause is **keuzetype-scoped** (ch. 2.4's family) and stays so: the struct-domain clause over these same three write positions is **WF-090**, its own rule (maintainer decision 2026-09-03), sharing this function's machinery under its own number. The unknown sentinel defers — WF-034 refuses it at all three positions anyway. The precision gate needs expression-shape analysis and stays OPEN) | `jutterswet-vondsttelling` (numeric assignments) + `jutterswet-vormenkabinet` (the enum assignment) + `jutterswet-penninguitgifte` (anchor: object-creation members) + every prioritizedDefaults golden — and a measured zero over-rejection across every golden and rood document and, read-only, the parity zone | `wf023-text-into-numeric` + `wf023-schrijf-enum-domein` + `wf023-schrijf-som-domein` + `wf023-schrijf-spookdomein` (a ghost domain at a write) + `wf023-schrijf-doeltype-leugen` (target type and value type share the lie: the cross is against the DECLARED datatype) + `wf023-schrijf-lijst-domein` + `wf023-schrijf-lijst-geen-domein-doel` + `wf023-schrijf-lijst-geen-domein-waarde` (the two one-sided arms within the list family) + `wf023-objectcreatie-domein` + `wf023-objectcreatie-soort` + `wf023-defaults-domein` + `wf023-defaults-soort` (positions 2 and 3, domain and family) + `wf023-defaults-exceptiegroep-domein` (the recursion into exception groups, which the two root-consequence cases do not pin) + `wf023-initialisatie-domein` (the third rule kind at position 1, which the `GELIJKSTELLING`/`BEREKENING` cases do not pin) |
| WF-024 | allocation method is one of the three real methods (no COMPLEX) | gate 3 | `jutterswet-berging` | `wf024-complex` |
| WF-025 | allocation fields valid per method; ordering key orderable; share typing | gate 3 (field validity and orderability; share typing is WF-023) | `jutterswet-berging` | `wf025-max-without-ratio` |
| WF-026 | at most one allocation per written target | gate 3 | `jutterswet-berging` | `wf026-two-allocations-one-target` |
| WF-027 | object-creation members declared on the created type; characteristic init boolean (an attribute member's value typing is WF-023's second write position) | gate 3 | `jutterswet-penninguitgifte` (anchor: the mint, its member initialisations and both guards execute, gates 5–6) | `wf027-ghost-member` |
| WF-028 | symbolic fact creation all-or-nothing and resolvable; join chain reaches target; new ≠ final hop | gate 3 (symbolic extension form; a bean-field-only v0 record stays uncheckable — recorded) | `jutterswet-gemeenteberging` (anchor: the chain walk executes and folds over the created relation pin absolute values, gates 5–6) | `wf028-half-symbolic-creation` (all-or-nothing) + `wf028-create-along-final-hop` (the full chain walk) |
| WF-029 | consistency assertion has a single subject | gate 3 | `jutterswet-vondstnummer` | `wf029-no-subject` |
| WF-030 | uniqueness sources: keys resolve; arity, datatype kind and units agree per position; a union carries ≥ 2 sources | gate 3 | `jutterswet-vondstnummer` + `anchor/jutterswet-kustregister` (the union — including the per-source sum over two same-type sources — and both scoped forms, executed by gates 5–6) | `wf030-arity-mismatch` + `wf030-single-source-union` |
| WF-031 | one definition per day kind; condition present and **pure** (no attribute/characteristic read — the stratum-0 framing, ch. 5.4); validity ALTIJD | gate 3 | `jutterswet-vormenkabinet` (the schouwdag definition, ALTIJD validity) + `jutterswet-schouwkalender` (a pure day-kind chain, replayed by gates 5–6 over its ordinary rule) | `wf031-missing-condition` + `wf031-dagsoort-venster` (bounded validity window) + `wf031-dagsoort-leest-kenmerk` (the purity clause: a day-kind condition reading a characteristic) |
| WF-032 | start point: variable-start timeline, day-precision member, one per pair | gate 3 | `jutterswet-vormenkabinet` (the meetdatum anchor) | `wf032-fixed-start-anchor` |
| WF-033 | (dialect) rule-group members resolve; recursive group has a creation member (either kind); object creations guarded, relation creations guard-free | gate 3 | `anchor/jutterswet-wachtersketen` (the recursive chain executes to its fixpoint, gates 5–6; the runaway budget fault is red-pinned, gate 8) + `anchor/jutterswet-erflijn` (the guard-free relation-creation member: the transitive closure reaches its fixpoint in both executors) | `wf033-unguarded-recursive` (the guard clause) + `wf033-no-creation-member` (the containment clause) |
| WF-034 | every expression/condition reference resolves (no unknown-type sentinel, at the top level and anywhere in the PAYLOAD SUBTREE of a value node's type — the sum chain and the `ListType`/`StructType` containers under it alike; a reference NAMES both halves of what it resolves through — the object type and the member — a parameter reference names its parameter, a characteristic check names its characteristic, a dimension-cell read holds an attribute reference and nothing else, an allocation names its source and target attribute, spells no blank name in any of its six attribute-name fields, resolves every name it does spell against the object type of that name's OWN SIDE, and names both those object types; and a rule target carries a type) | gate 3 (partial: sentinel types and parameter/day-kind/element-type/own-member references; variable bindings and navigation paths are not serialized in v0; excepted: a v0 instance-fold source argument names a role and carries the sentinel by construction — the row-1 wart, resolved against declared roles with the ch. 4.3 type-pair fallback for a name that resolves nowhere, and since wave 4 that exception is SCOPED to the wart, which the wire spells with exactly that sentinel: a fold source carrying a real datatype resolves as the ordinary attribute reference it is). **Widened 2026-09-04** (maintainer decision) in FOUR waves plus a fix round, eighteen arms, no new rule number and no documented deferral; every one of them closes a **self-served exemption from the identity family** (WF-089/WF-090/WF-091), which defers on an unresolvable declaration and on a deferring type — so an emptied field or a sentinel switched the unit, precision and domain checks off on a document's own say-so while it stayed verifier-clean. *Wave 1* — an `AttributeReference` without an `objecttype` in a value position, the same in a FOLD SOURCE (its own arm: the aggregation branch replaces the generic walk and resolves only a named endpoint, so the empty field fell through both fences), and the sentinel on a rule TARGET's `type`. *Wave 2* — the same for the OTHER half of the pair, an `AttributeReference` without an `attribuut`, again in both positions (with the value-position arm alone 43 of the goldens' 751 emptied `attribuut` positions stayed clean, all fold-/aggregation arguments; with both, 0 of 751 — the same lesson the fold source taught in wave 1); a `ParameterRef` without a `parameter` (35 of 37 → 0); the UNSPELLED TWIN of the target sentinel, a target with no `type` at all (295 of 295 → 0), which wave 1 left out for want of a witness and which is the more dangerous half — it also switches off WF-007's target clause, and unlike every other shape here it replays CLEAN in `ref` and clingo while the Drools backend throws, so the verifier is the only place it can be caught before an engine crashes on well-formed input; and the sentinel one level DOWN, on a `SumType` constructor payload inside a value node's type (the fence matched the type's top level only; the walk now descends through payloads, WF-088's descent for the same reason). *Wave 3* — the four channels wave 2 measured and left open, six arms (the container half of them carrying three separately falsifiable branches: the list element, the record field, and the container→sum re-entry, which alone pins `wf034-sentinel-payload-onder-lijst`). A `Kenmerk` CHECK naming no `kenmerk` (89 of the goldens' 96 positions, in twelve position shapes over five top-level sections, stayed clean when emptied one at a time; 0 after), where one empty field silenced four readers at once — this rule's own declared-characteristic cross, WF-043's granularity cross, WF-053's navigated hop and the dependency edge. A `ResolvedVerdeling` naming no `bronAttribuut` or no `doelAttribuut` (11 of 11 each; 0 after) — its own function beside the expression walk, because an ALLOCATION is not an expression and its two endpoint attributes are the one reference pair `walk_expr` cannot reach, so WF-025's ordering-key check, WF-026's same-target comparison, WF-050's defaults overlap and both stratification edges keyed on them fell silent together. A `DimensieCel` whose `attribuut` is null **or holds a non-reference node** — two arms for two ways of missing one pattern, the cell arm resolving only through a complete inner `AttribuutRef`; measured on an existing artifact rather than argued, since emptying or overwriting `wf091-dimensiecel-eenheid`'s cell reference turns its pinned unit refusal off and the document CLEAN, and this is the one wave-3 channel with **no executor witness under today's executors** (a cell read is declined as a FORM by both OCaml executors, so no golden carrying one has or could have a case file today). The harm is pinned on FOUR existing artifacts, each measured under both spellings with the verifier of the day before this card and each turning `verifier: PASS` outright: `wf091-dimensiecel-eenheid`, `wf089-dimensiecel-enum`, `wf090-dimensiecel` and `wf040-unpinned-cell` — WF-040's label/dimension cross rides on the same match and goes with the identity family. The structural half of the argument, beyond the corpus: the reference frontend's IR types this field as an attribute reference, so its own emissions cannot reach the non-reference spelling; the clause is for the wire, which any conforming producer may write. And the CONTAINER half of wave 2's payload descent: the sentinel in a `ListType` element or `StructType` field under a constructor payload, and a sum reached back THROUGH such a container — the descent is now mutually recursive over the whole payload subtree, and the sum-chain messages it already pinned are unchanged. *Fix round, same day, same decision* — the FILLED half of wave 3's allocation arm: a present attribute name on any of the allocation's six name fields (`bronAttribuut`, `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut`, `restantAttribuut`, `groepeerAttribuut`) is not blank, emptiness decided by the ch. 2.4 name identity, while a null on the four facet fields keeps its meaning of a facet switched off. `""` satisfied every consumer's `Some a` and named nothing, so it walked through the null arm into the same silence: 11 of 11, 11 of 11, 7 of 7, 4 of 4, 9 of 9 and 3 of 3 golden positions stayed verifier-clean when blanked one at a time, and none does now. The executor consequence is the reason this needed a clause rather than a note: loud on the two endpoints and on `restantAttribuut`, but on `ratoAttribuut` and `groepeerAttribuut` BOTH executors play on and agree on a wrong answer, and on `maximumAttribuut` `ref` plays on (600 for a pinned 300) while the ASP backend refuses — ref and clingo diverging on a verifier-clean document. Wave 3's over-rejection check is direct and total: the verifier's verdict over all **462** documents of golden + rood + negatief + parity that PREDATE this card (153 + 22 + 257 + 30) is unchanged, with four pre-existing allocation negatiefs gaining a truthful incidental WF-034 line while keeping their verdict and the rule their directory declares; the eleven new negatiefs the card and its fix round add — negatief zone 257 → 268, four zones 462 → 473 — are the only additions, and a byte-level comparison of the two verifiers' reports over all 473 documents changes exactly those 15 reports and nothing else. All eight wave-1/2 shapes are carried by **zero** documents outside their own negatief witnesses (152 golden + 22 rood + 30 parity, plus every negatief that predates the wave, measured before and after each), so over-rejection is impossible by measurement and nothing repinned — and the wave-2 over-rejection check is direct: the verifier's report over all 447 documents of golden + rood + negatief + parity is byte-identical before and after but for the five new negatiefs. Nor does anything this repo's own frontends emit carry them (28 rgs goldens, 12 catala goldens, 45 parity fragments, all measured clean) — the shapes are open to any other conforming emitter, which is the audience of an engine-neutral IR. *Wave 4, same day, same decision* — the NAMING class the first three waves kept measuring and leaving behind: not a field left empty but a field filled with a name that resolves against nothing, three arms. A filled attribute name on ANY of the allocation's six name fields resolves against the declaration scope of its OWN SIDE — `bronAttribuut` and `restantAttribuut` against `bronType` (the pot, and the remainder written back on the source), `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut` and `groepeerAttribuut` against `doelType` (the share, and the three facets read per recipient); an undeclared TYPE is another rule's business (WF-047 on the bound form, WF-048 on the bindingless one, both RULES measured loud — the executors are not: an undeclared `bronType` leaves `ref` silent with every share *leeg* while clingo refuses, ch. 4.6 (j)), so the arm resolves only where there is a scope to resolve in. Measured with `bestaatniet` one field at a time, named to their counts in the order the clause lists the fields (source side first): `bronAttribuut` 11 of 11 and `restantAttribuut` 9 of 9 against `bronType`; `doelAttribuut` 11 of 11, `ratoAttribuut` 7 of 7, `maximumAttribuut` 4 of 4 and `groepeerAttribuut` 3 of 3 against `doelType`. Every one of those golden positions stayed verifier-clean, none does now — and the observability rows are the fix round's exactly, one spelling on: loud on the two endpoints and `restantAttribuut`, BOTH executors silent and agreed on `ratoAttribuut` (the shares stay underived where the anchor pins 60 and 40) and on `groepeerAttribuut` (300 apiece where 1200/0/0/0 is pinned), and `ref` silent while clingo refuses on `maximumAttribuut` (600 for a pinned 300). An allocation NAMES both its object types: 11 of 11 each stayed clean with `bronType` or `doelType` emptied while both executors fault by the field's own name, and a null there also silences WF-025's ordering-key datatype, WF-050's defaults overlap, both stratification edges and this wave's own resolution scope. And the ch. 4.3 fold-source exception returns to its SCOPING: the row-1 wart is the fold-source argument carrying the unknown sentinel on its own type — that is how v0 spells it, a role name having no datatype to give, and it is the discriminator both OCaml executors already key their role-naming branch on (the Drools backend keys the same reading on `kind = AANTAL` plus its own role derivation and plays a real-datatype source both of them refuse — measured, ch. 4.3) — so a fold source carrying a real datatype resolves as an ordinary attribute reference, through neither the role-name route nor the type-pair fallback. Both branches measured live and separately falsifiable: on `anchor/jutterswet-maatschap`, whose reciprocal self-relation derives one connecting role for ANY name, a SOM source repointed at `bestaatniet` stayed clean while `ref` silently summed 0 where 12 and 30 are pinned and clingo refused; on `jutterswet-vondsttelling`, where the pair `Vondst`/`Vondst` derives NO connecting role and only the role route can admit anything, the same source repointed at the declared plural `vondsten` stayed clean while `ref` summed 0 where 3.75 kg is pinned and clingo refused. The regression boundary was counted before and after: of the corpus's 89 named fold sources — *named* meaning the argument spells both its `attribuut` and its `objecttype`, the pair this arm resolves through; count in the one source that spells an attribute and no object type (a wave-1 shape, refused there) and the totals read 90 and 58 — 32 carry the wart (24 resolving through a declared role name, 5 through the type pair, 3 already ill-formed under this rule's older arms) and 57 do not, of which 54 already resolve as a declared attribute and one names a characteristic (WF-080's arm, unchanged) — so the narrowing rejects nothing that was standing, and the counting folds that lean on the fallback (`jutterswet-kaveltelling`'s synthesized plural, and the declared-plural route of `jutterswet-aanmeldertelling` and `jutterswet-vondsttelling`) keep both routes and replay unchanged in both executors. Wave 4's over-rejection check is direct and total, the same instrument: over all **486** documents of golden + rood + negatief + parity (153 + 22 + 281 + 30) exactly 15 reports change and 10 verdicts — the ten being the wave's own new negatiefs, seven written with the clauses and three more in the fix round that gave the `restantAttribuut` and `doelAttribuut` sub-arms and the `doelType` arm a witness each; no golden and no rood document moves at all, and five documents keep their verdict while gaining truthful incidental lines (the four pre-existing type-less allocation negatiefs, one or two lines each, and the parity casus, whose own `doelAttribuut` names an attribute its target type does not declare — six incidental lines there, one per allocation). Negatief zone 271 → 281, four zones 476 → 486. *Measured and still open:* the sentinel survives on 9 fold-/aggregation argument types (8 element-passing aggregation operands, 1 true instance-fold source) — of 2758 datatype-valued golden positions injected one at a time (the denominator is the WAVE-2 corpus state of 2026-09-04 and a later or differently drawn count will differ; the numerator is what carries the claim and is re-confirmed) 36 stayed clean, 15 of them no-ops on positions that already hold the sentinel (the row-1 wart), 12 the payloads wave 2 closed, and these 9; and the row-1 wart itself, the 15 no-op positions that carry the sentinel by construction, gedoogd and documented — narrowed by wave 4 to the position ch. 4.3 scopes it to, but the sentinel it carries there is still unfenced, and so is the name written beside it. *That residual width was counted over the four zones:* of the 32 wart sources, 26 sit on a type pair that derives exactly one connecting role, so the fallback admits ANY name there; on 18 of those 26 WF-047 crosses the written name against the bound role of an explicit binding, which leaves **eight** positions in which an arbitrary name simply stands — and only **two** of them are golden, `jutterswet-aanmeldertelling` and `jutterswet-kaveltelling`, the corpus's two bindingless counting folds. That is the whole of what the wart still buys a document, and it is why the narrowing is a scoping and not a repeal. (The two name-resolution gaps wave 3 recorded here as open — a name no declaration carries on any of the allocation's six attribute-name fields, and an empty `bronType`/`doelType` — and the fold-source fallback's own width are CLOSED by wave 4, ch. 4.6 (j)–(l).) Still open, and another rule's walk to make: WF-007's precision-form check, which has the identical blind spot at the payload positions wave 2 closed and in the containers wave 3 closed | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondsttelling` (role source) + `jutterswet-kaveltelling` (the type-pair fallback: an undeclared synthesized plural over a unique pair, both executors — and, since wave 4, the corpus's one golden fallback user, so it is the standing regression witness for the narrowed scoping) — and, for the eighteen widened arms, every golden and rood document is a non-over-rejection witness at once, since all eighteen shapes carry zero occurrences in those two zones (the only occurrences anywhere are the wave-3 and wave-4 arms' four pre-existing allocation negatiefs and the parity casus, which gain incidental lines and keep their verdict) | `wf034-dangling-parameter` + `wf034-ambigu-typenpaar` (unresolved name, two connecting fact types) + `wf034-zelftype-typenpaar` (unresolved name, one self-typed two-role fact type — both roles are candidates, counted in roles not fact types) + `wf034-typeloos-literal` (the unspelled twin of the sentinel, on a value node) + `wf034-attribuutref-zonder-objecttype` (the missing endpoint, carrying the measured answer-changing `t`-for-`kg` lie the empty field hid: `ref` faults, clingo refuses, Drools played it) + `wf034-foldbron-zonder-objecttype` (the same field emptied in a fold source, the same lie, and the worse measurement: `ref` PASS, clingo PASS, Drools PLAYED — all three answer off it) + `wf034-onbekend-regeldoel` (the sentinel on `/resolvedRegels/1/doel/type`; its NOTE names the `SumType` constructor payload as a second surviving position, which is true of the wave it was written in — since wave 2 the payload half is closed and only the row-1 fold-source wart remains) + `wf034-attribuutref-zonder-attribuut` + `wf034-foldbron-zonder-attribuut` (the other half of the pair, once per path, each carrying the same `t`-for-`kg` lie: `ref` and clingo fault, Drools skips the rule or throws) + `wf034-parameterref-zonder-parameter` (same lie on a parameter operand; `ref` and clingo fault, Drools throws) + `wf034-regeldoel-zonder-type` (the unspelled twin: `ref` PASS, clingo PASS, Drools throws — verifier-clean input crashing an executor) + `wf034-sentinel-sumtype-payload` (the sentinel a level down: `ref` replays it, clingo refuses to translate it — one document, two executors disagreeing about whether it exists) — and, for wave 3, eight more, one per form and per spelling: `wf034-kenmerkcheck-zonder-kenmerk` (`ref` "missing KenmerkCheck.kenmerk", clingo "missing kenmerk") + `wf034-verdeling-zonder-bronattribuut` + `wf034-verdeling-zonder-doelattribuut` (the allocation pair, each half its own document, `ref` and clingo faulting on the field's own name) + `wf034-dimensiecel-zonder-attribuut` + `wf034-dimensiecel-geen-referentie` (the two spellings of the cell escape, both VERIFIER-ONLY — a cell read is declined as a form by both executors, so the harm is pinned by `wf091-dimensiecel-eenheid` turning CLEAN when its cell reference is removed, not by a replay) + `wf034-sentinel-lijstelement-payload` + `wf034-sentinel-structveld-payload` + `wf034-sentinel-payload-onder-lijst` (the container half: the list branch, the record branch, and the container→sum direction of the recursion, each with `ref` replaying to its pinned expectations while clingo refuses to translate) — and, for the fix round, three more, one per measured executor shape of the blank name: `wf034-verdeling-lege-bronattribuut` (the loud shape: both executors fault) + `wf034-verdeling-lege-maximumattribuut` (the DIVERGENT shape: `ref` plays on and returns 600 for a pinned 300 while clingo refuses to translate) + `wf034-verdeling-lege-groepeerattribuut` (the SILENT shape: both executors play on and agree on the same wrong distribution — 300 apiece where 1200/0/0/0 is pinned — which is the one place only the anchor pinning stands between the corpus and a defect) — and, for wave 4 and its fix round, ten more, one per measured executor shape of the RESOLVING name and one per sub-arm that had none: `wf034-verdeling-onbekend-ratoattribuut` (the silent-and-agreed shape on an ANCHOR golden: neither executor faults, both leave the shares underived where 60 and 40 are pinned) + `wf034-verdeling-onbekend-groepeerattribuut` (the second silent-and-agreed shape, and the sharper one: both compute a whole distribution and it is the same wrong one) + `wf034-verdeling-onbekend-maximumattribuut` (the DIVERGENT shape: `ref` plays on uncapped at 600 for a pinned 300 while clingo refuses to translate) + `wf034-verdeling-onbekend-bronattribuut` (the loud shape, present so the two silent rows are not mistaken for the whole class) + `wf034-verdeling-onbekend-doelattribuut` (the second loud shape, on the field the allocation WRITES — the one the parity casus carries six times over) + `wf034-verdeling-onbekend-restantattribuut` (the third, and the sub-arm that left no trace at all: before this witness, deleting its one line changed no document in any of the four zones) + `wf034-verdeling-zonder-brontype` + `wf034-verdeling-zonder-doeltype` (the missing object type, both executors faulting by the field's own name — one witness per half, since each type is its own match arm and deleting either alone moves only its own document) + `wf034-foldbron-onbekend-attribuut` (the type-pair branch of the scoping, on `anchor/jutterswet-maatschap` where a reciprocal self-relation derives one connecting role for any name at all: `ref` silently sums 0 where 12 and 30 are pinned, clingo refuses) + `wf034-foldbron-rolnaam-waardevouw` (the role-name branch, deliberately placed on `jutterswet-vondsttelling` where the type pair derives NO connecting role, so only the role route could ever admit it — which is what makes the two branches separately falsifiable: each mutant frees exactly one of the two documents) |
| WF-035 | operator typing (numeric, unitless power, date±duration order/unit/precision, unit compatibility, clamp) | gate 3 (over serialized operand types) | `anchor/jutterswet-wachtersketen` (the numeric Plus, executed by gates 5–6) + `jutterswet-vervaltermijn` (date ± duration, the date-left plus and min forms, executed by gates 5–6) | `wf035-power-with-unit` + `wf035-datum-plus-datum` + `wf035-duur-plus-datum` + `wf035-duur-min-datum` + `wf035-uur-op-dagdatum` |
| WF-036 | comparison/predicate typing (ordering, precision, family, domain, builtin operands) | gate 3 (over serialized operand types) | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondstnummer` (date ordering) | `wf036-order-number-vs-text` |
| WF-037 | aggregation/builtin shape: facet validity and arity per kind | gate 3 (fixed-arity kinds; date-construction arity OPEN) | `jutterswet-vondsttelling` | `wf037-fallback-on-minimum` |
| WF-038 | aggregation operand typing (units, precision, rate for totaal, year for paasdag, date for the day-precision extractors, tijdsduur operands and node-type unit) | gate 3 (paasdag, day-precision and sub-day extractors, sum fallback, tijdsduur; the totaal rate clause and fold unit-compatibility are OPEN) | `jutterswet-vondsttelling` (the sum fallback slice) + `anchor/jutterswet-jutleeftijd` (the tijdsduur slice, executed by gates 5–6) + `jutterswet-paasvondst` (the paasdag and extractor slice, executed by gates 5–6) | `wf038-paasdag-text` + `wf038-extractor-text` + `wf038-tijdsduur-gemengde-precisie` + `wf038-tijdsduur-zonder-tijdseenheid` + `wf038-tijdsduur-zonder-node-type` |
| WF-039 | enum label in domain; literal sign restriction | gate 3 | `jutterswet-vormenkabinet` (the gaaf label and the sign-restricted aanwas) | `wf039-negative-under-nonneg` |
| WF-040 | dimension cell pins every dimension; selection labels resolve in order | gate 3 | `jutterswet-vormenkabinet` (the voorjaar cell read) | `wf040-unpinned-cell` |
| WF-041 | counting-quantifier threshold ≥ 1 | gate 3 | `jutterswet-vormenkabinet` (ten minste 2) | `wf041-threshold-zero` |
| WF-042 | time window: ≥ 1 bound; bounds are dates, either precision | gate 3 | `jutterswet-vormenkabinet` (the vanaf window) | `wf042-empty-window` |
| WF-043 | whole-period qualifier only on a time-dependent operand | gate 3 | `jutterswet-vormenkabinet` (whole-year over the waterstand) | `wf043-wholeperiod-nontemporal` |
| WF-044 | (consumer) Unresolved surfaces, never silently lowered | behavioural — gate 8 (`scripts/check-rood.sh`): the two mandatory executors (`expectedFault.{ref, clingo}`) must both fault, fragment-pinned. The additive `expectedFault.drools` leg (decision V3-b) pins a Drools fault only where the v0 backend carries the red-path form; where it does not, the gate lists the case with a reason (a reason-carrying exclusion list, stale-guarded) — Onopgelost is such a case (its reaching rule does not lower: a transpiler SKIP leaves the value out of scope), so it is listed, never Drools-fragment-pinned | `corpus/rood/wf044-onopgelost-reached` (the red witness: ref faults the evaluation, the ASP backend the translation; the Drools leg lists it — the marker's rule never fires) | n/a (the marker is legal wire content; the fence is behavioural, not well-formedness) |
| WF-045 | dependency graph acyclic (creation recursion excepted) | gate 3 | `jutterswet-vondsttelling` | `wf045-cyclic-attribute-dependency` |
| WF-046 | no negative edge on a dependency cycle (strata exist) | gate 3 | `jutterswet-penningjaar` (the negated aanmeldplichtig reading — a negative edge, verifier-clean, oracle-replayed) | `wf046-negated-read-in-recursive-cycle` |
| WF-047 | role binding resolves; fillers agree with the use site; written source denotes the bound role | gate 3 | `anchor/jutterswet-bergingstoezicht` (two fact types over one pair; allocation and fold bound, replayed by gates 5–6) + `anchor/jutterswet-maatschap` (the reciprocal binding: bound = opposite) + `anchor/jutterswet-wachtersketen` (the creation back-link binding) | `wf047-dangling-role-binding` (resolution) + `wf047-bound-role-fills-source` (filler agreement) + `wf047-reciprocal-wrong-filler` (the single-role filler clause) + `wf047-creation-binding-wrong-filler` (the creation site) |
| WF-048 | bindingless allocation/instance fold: connecting relation uniquely derivable | gate 3 (contexts with a statically known fold subject; a subjectless context — none exists in v0 dumps, whose folds sit under a typed rule target — is left to the executors' runtime fault) | `jutterswet-berging` + `jutterswet-vondsttelling` (bindingless, uniquely derivable) + `anchor/jutterswet-maatschap` (bindingless over a reciprocal relation) | `wf048-ambiguous-connecting-relation` |
| WF-049 | temporal read: basis temporal (attribute/granular parameter), day-precision date, element type | gate 3 | `anchor/jutterswet-aanmeldquotum` (attribute + parameter reads; hit, gap→leeg, empty date→leeg; replayed by gates 5–6) | `wf049-read-of-nontemporal` (basis) + `wf049-read-at-datetime` (date operand) + `wf049-type-not-element` (element type) |
| WF-050 | defaults group: ≥ 1 default; exactly one target, resolving; default shapes per kind (recursing into sub-defaults, ch. 7.9 — a group-default carries `exceptions` and no own content); init default lowest; no other writer; multiplicity only declared-unordered | gate 3 | `anchor/jutterswet-uitsluiting` (characteristic) + `anchor/jutterswet-vondstwaardering` (attribute + init) + `anchor/jutterswet-toelagegronden` (unordered groups, six conflict-path cases; all replayed by gates 5–6) | `wf050-empty-defaults` + `wf050-two-defaults-one-target` + `wf050-defaults-beside-rule` + `wf050-both-targets` + `wf050-init-not-lowest` + `wf050-rule-beside-attribute-defaults` + `wf050-allocation-beside-defaults` + `wf050-creation-beside-defaults` + `wf050-half-marked-group` + `wf050-subdefault-wrong-shape` |
| WF-051 | no dependency cycle through a defaults group (justification and consequence reads are negative edges, folded recursively through sub-defaults, ch. 7.9) | gate 3 (over the extended dep_forms graph, shared with ref/'s strata) | `anchor/jutterswet-uitsluiting` (the defaults group sits below its negated consumer — strata exist, oracle-replayed) | `wf051-defaults-read-own-target` + `wf051-cycle-through-subdefault` |
| WF-052 | a connecting relation with a to-one created-side role has at most one object-creation writer (fact creations are fault-paired already) | gate 3 | `anchor/jutterswet-penninguitgifte` (one writer per to-one relation, executed by gates 5–6) | `wf052-two-writers-one-to-one-relation` |
| WF-053 | a navigated read resolves its walk: bindings resolve, chain-type, count and written names agree, the endpoint is reached; bindingless steps are uniquely derivable; scalar-slot hops are to-one; bindings on a non-navigating node are ill-formed; a single bindingless written step at zero far hits on a same-typed endpoint is the zero-hop self-reference iff the name denotes a role the subject itself fills (ch. 4.1a) | gate 3 | `anchor/jutterswet-kustwacht` (by-name, chain and by-pair shapes plus the navigated characteristic check, executed by gates 5–6 with leeg-on-absent and the negation asymmetry pinned) + `jutterswet-postwacht` (emitter-produced bindings since the 2026-08-18 pin bump) + `jutterswet-aanmelderkrediet` (the zero-hop self-reference beside the reciprocal far hop, executed by gates 5–6) | `wf053-dangling-navigation-binding` (resolution) + `wf053-chain-near-side-mismatch` (chain typing) + `wf053-to-many-hop-in-scalar` (cardinality) + `wf053-written-role-mismatch` (written-name denotation) + `wf053-bindingless-ambiguous` (unique derivability) + `wf053-zelfverwijzing-vreemde-rol` (the self-reference narrowing) |
| WF-054 | parent-scoped uniqueness: the scope role resolves (bound or uniquely derivable from the written scope field), its near side fills the subject, the written name denotes the bound role, the scope role is to-one; a scope binding on an unscoped uniqueness is ill-formed | gate 3 | `anchor/jutterswet-kustregister` (bound and bindingless scoped forms, executed by gates 5–6 with the scope-less exclusion pinned) + `jutterswet-postwacht` (emitter-produced `scopeRol` since the 2026-08-18 pin bump) | `wf054-dangling-scope-binding` (resolution) + `wf054-to-many-scope-role` (cardinality) + `wf054-binding-on-unscoped` (the non-scoped guard) |
| WF-055 | the gedurende clip: a filter on the temporal total is a timeline operation — its condition is an own temporal characteristic check, a per-segment comparison of an own temporal attribute against a scalar bound, or a FLAT ALLE composite of characteristic checks (`RUNTIME-SEMANTICS.md` ch. 3.6: the intersection of the members' true-periods; v0 partitions static comparison members into value-side guards and markers nested bullets — v1 narrows composite members to characteristic checks, a recorded narrowing) | gate 3 | `jutterswet-getijdenwacht` (both simple shapes) + `anchor/jutterswet-dubbelwacht` (the flat composite: overlap, disjoint, one-empty and split periods, executed by gates 5–6) | `wf055-nontemporal-clip-kenmerk` + `wf055-navigated-clip-condition` + `wf055-composite-comparison-leaf` + `wf055-composite-nontemporal-leaf` + `wf055-composite-nested` |
| WF-056 | the pro-rata fold (tijdsevenredig-deel) MUST carry a gedurende condition and one temporal source argument (a temporal attribute or granular parameter) as coarse as or coarser than its bucket; on a temporal target the bucket granularity MUST equal the target's declared granularity | gate 3 | `anchor/jutterswet-borgdeel` (the ch. 3.7 fold, executed by gates 5–6) | `wf056-no-gedurende-condition` + `wf056-scalar-base` + `wf056-bucket-target-mismatch` |
| WF-057 | declared names are target-text-safe: no double quote, backslash or control character in any declared/form name (ch. 2.1) | gate 3 | every golden (a universal name-safety property: no single legal name exercises the boundary more than another; gate 3 witnesses none over-rejects) | `wf057-quote-in-attribute` + `wf057-backslash-in-kenmerk` + `wf057-newline-in-rule-name` |
| WF-058 | the day count (aantal-dagen-in, `RUNTIME-SEMANTICS.md` ch. 3.8) MUST carry a gedurende condition over an own temporal characteristic, takes no arguments, and its stated bucket MUST equal the target's declared granularity | gate 3 | `anchor/jutterswet-dagentelling` (the ch. 3.8 worked example verbatim, executed by gates 5–6) | `wf058-no-gedurende-condition` + `wf058-bucket-target-mismatch` + `wf058-with-argument` |
| WF-059 | every declared attribute carries a datatype (ch. 2.2 — the attribute pendant of WF-018's datatype clause; a null datatype splits the executors on otherwise clean input) | gate 3 | every golden (a universal declaration property; gate 3 witnesses none over-rejects) | `wf059-attribute-without-datatype` |
| WF-060 | SumType declaration: ≥ 1 constructor, unique names (ci), admissible payload datatypes (Number, day-precision Date, declared SumType), acyclic payload graph (ch. 2.4) | gate 3 | `anchor/jutterswet-beschikkingsom` (executed by gates 5–6) | `wf060-duplicate-constructor` + `wf060-text-payload` + `wf060-recursive-sumtype` |
| WF-061 | SumType use site agrees with its declaring domain; two sum-typed comparison operands name the same domain (the nominal rule, ch. 2.4) | gate 3 | `anchor/jutterswet-beschikkingsom` | `wf061-use-site-drift` + `wf061-cross-sumtype-equality` + `wf061-undeclared-domain` |
| WF-062 | Construct: sum-typed node; declared constructor; argument present iff unary; argument type matches the declared payload (ch. 4.7) | gate 3 | `anchor/jutterswet-beschikkingsom` | `wf062-nullary-with-argument` + `wf062-payload-type-mismatch` + `wf062-construct-on-enum` (the asymmetry the 2026-09-01 widening left standing: `Match` widened to enumeration scrutinees, `Construct` deliberately did not — an enumeration value is a `Literal`) |
| WF-063 | Match totality over BOTH keuzetype variants (ch. 2.4/4.7): keuzetype-typed scrutinee (sum type or enumeration) naming a domain declared in that variant; arms cover the declared alternatives exactly, each once, none alien; on a sum scrutinee binder iff unary, on an enumeration scrutinee binder always null | gate 3 | `anchor/jutterswet-beschikkingsom` (the sum arm) + `jutterswet-vaarwatertoeslag-catala` (the enumeration arm: an all-nullary keuzetype normalised to its Enumeratie domain, WF-087, matched arm by arm — replayed by gates 5–6) | `wf063-missing-arm` + `wf063-alien-constructor` + `wf063-binder-on-nullary` + `wf063-duplicate-arm` (sum) + `wf063-enum-missing-value` + `wf063-enum-binder-on-arm` + `wf063-enum-alien-value` (enumeration) |
| WF-064 | every BinderRef resolves to an enclosing binder (a match arm's binder, a lambda/function parameter, or — the 5c widening — a `Let` binder scoped to its body); the closure clause: inside a closed lambda/function body the set is closed at the boundary, so a BinderRef reaching an enclosing rule's Let binder or an outer fold's parameter is ill-formed (an inside-body Let stays in scope); match arm bodies agree on one result datatype (ch. 4.7/4.8/4.10; VariabeleRef stays outside — row 24) | gate 3 | `anchor/jutterswet-beschikkingsom` + `anchor/jutterswet-tariefstelling` (Let binders) | `wf064-unbound-binderref` + `wf064-mixed-arm-types` + `wf064-enclosing-binder-in-closed-body` (the closure clause) |
| WF-065 | ListType: first-order non-list element (Number, day-precision Date, declared SumType, or — the 5c widening — declared StructType); no granularity on a list-typed attribute (ch. 2.4). Since WF-087 (2026-09-01) an all-nullary keuzetype normalises to `Enumeration`, so it is no longer an admissible element type: the list carrier demands the payload form (`frontends/catala/test/refuse_list_nullary_enum.catala_en`) | gate 3 | `anchor/jutterswet-staffelvergoeding` (executed by gates 5–6) + `anchor/jutterswet-vondstdossier` (the list-of-struct widening) | `wf065-list-of-list` + `wf065-temporal-list-attribute` |
| WF-066 | ListLiteral elements carry the declared element datatype (ch. 4.8) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf066-heterogeneous-literal` |
| WF-067 | FunctionDef: ci-unique names, typed unique parameters, resolvable result, body type EQUALS the declared result (soort and keuzetype domain alike — the write half of ch. 2.4's domain identity for functions, where WF-068 is the read half), closed body (the 5c widening admits FieldAccess/StructLiteral), acyclic invocation graph (ch. 2.11) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf067-recursive-functions` + `wf067-attribute-read-in-body` + `wf067-body-result-drift` (a body of a second enumeration domain under a truthful `Invoke`) |
| WF-068 | Invoke: resolvable function, arity, argument types, result type (ch. 4.8) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf068-unresolved-invoke` + `wf068-arity-mismatch` |
| WF-069 | Lambda only as a Fold/MapList operand and the operand only an inline Lambda (no bare FunctionDef name); IfThenElse only inside lambda, function or (the 5c widening) `Let` bodies; lambda arity per host, typed parameters, closed bodies (the 5c widening admits FieldAccess/StructLiteral); IfThenElse branch agreement (ch. 4.8/4.10) | gate 3 | `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-vondstdossier` (the map-over-struct-field body) + `anchor/jutterswet-tariefstelling` (the Let-body IfThenElse) | `wf069-lambda-outside-fold` + `wf069-ifthenelse-outside-body` + `wf069-named-function-operand` |
| WF-070 | fold-family shapes: list-typed source; Fold accumulator first-order non-list shared with init/body/type, element parameter = source element; MapList/FilterList/ListLength result typing (ch. 4.8) | gate 3 | `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-aanspraaktelling` | `wf070-list-accumulator` + `wf070-scalar-source` |
| WF-071 | StructType declaration: ≥ 1 field, ci-unique names, admissible field datatypes (Number, day-precision Date, declared SumType, declared StructType), acyclic field graph shared with WF-060 (ch. 2.4). Since WF-087 (2026-09-01) an all-nullary keuzetype normalises to `Enumeration`, so it is no longer an admissible field datatype: the struct carrier demands the payload form (`frontends/catala/test/refuse_struct_nullary_enum.catala_en`) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf071-duplicate-field` + `wf071-text-field` + `wf071-recursive-struct` |
| WF-072 | StructType use site agrees with its declaring domain — same fields, datatypes, ORDER; two struct-typed comparison operands name the same domain (the nominal rule, ch. 2.4). It binds a use site to the domain it NAMES; binding a use site to the declaration it reads from or writes into is WF-090 | gate 3 (a datatype WALK, the struct sibling of WF-088's `enum_use_site`, over every `StructType` in the MODEL and EXPRESSION layers: attribute and parameter datatypes, a `ListType` element inside one, `FunctionDef` parameters and result, a struct-typed FIELD inside `domeinen` — a position WF-088 does not need, since WF-060/065/071 fence enumerations out of every declaration position — and every expression node's `type`, via `walk_expr`'s type prologue. THREE positions stay outside the walk because no negatief can isolate them: a matched struct's own `velden`, since the agreement test is an equality on the whole field datatype; a sum-type payload, since WF-060 admits no struct payload in a declaration at all; and — the same position WF-088 records — a rule's TARGET `type`, WF-022's lvalue rather than an expression node, present in five goldens and two rood documents, where drift is measured harmless (reverse OR rename the fields of `jutterswet-dossierspelling`'s `/resolvedRegels/1/doel/type` and both variants stay schema-valid and verifier-clean while ref and clingo each replay all three pinned expectations, and a value sharing the target type's lie is caught by WF-090's write half against the DECLARED datatype — measured) — all three measured. The 2026-09-03 widening is what makes "the declaration is the source of truth" mechanically true for this variant: the inline field list is what the executors read — ref's shared case loader crosses a record against it, the ASP backend encodes struct terms POSITIONALLY out of it — so a drifted copy is a wire-level lie both consumers act on) | `anchor/jutterswet-vondstdossier` + `jutterswet-dossierspelling` (the ci witness; both replayed by gates 5–6) — and a measured zero over-rejection across every golden and rood document and, read-only, the parity zone | `wf072-use-site-drift` (an attribute datatype) + `wf072-cross-struct-equality` + `wf072-lijstelement-drift` (the `ListType` look-through: verifier-clean before, ref faulted at the field read while clingo reported `PASS (3 expectations)`) + `wf072-domeinveld-drift` (the `domeinen` position) + `wf072-functiondef-drift` (the `functionDefs` position, which the corpus carries for real in `jutterswet-kamerhuishouden-catala`) + `wf072-expressietype-volgorde` (the expression layer, and the wrong-answer witness: verifier, ref and schema clean on ONE reordered inline copy while clingo derived 5.03 for 125.50) |
| WF-073 | StructLiteral: declared struct type; exactly the declared fields, each once; each value type matches the field declaration — a full datatype equality, so a nested struct field value of another domain is caught here and needs no WF-090 arm (ch. 4.9) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf073-missing-field` + `wf073-alien-field` + `wf073-field-type-mismatch` |
| WF-074 | FieldAccess: struct-typed base; field a declared field; node type = the field datatype — a full datatype equality, so a struct-typed field read of another domain is caught here and needs no WF-090 arm; the domain the BASE reads from is WF-090's (ch. 4.9) | gate 3 (against the DECLARATION, never the base's inline copy — which is why WF-072's walk reaches every expression `type`: measured, a base whose inline copy renamed the declared fields was verifier-clean and ref-green while the ASP backend raised this same WF-074 against the copy) | `anchor/jutterswet-vondstdossier` | `wf074-unknown-field` + `wf074-nonstruct-base` |
| WF-075 | nested exception form (ch. 7.9): a default's `exceptions` field carries ≥ 1 exception group; two or more require the `unordered` marking (the root regime, at every level); no initialisation/input default in sub-defaults; every default carries own content, a nested exception group, or is the initialisation default; a group-default (`exceptions`, no own content) carries no own justification; recurses | gate 3 | `anchor/jutterswet-nestuitzondering` (attribute target) + `anchor/jutterswet-nestkenmerk` (characteristic target) + `anchor/jutterswet-nestdiepte` (three-level depth) — admitted through the Phase-5c executor card | `wf075-empty-leden` + `wf075-two-members-unmarked` + `wf075-init-in-subdefault` + `wf075-contentless-default` + `wf075-groupdefault-with-justification` |
| WF-076 | `Let`: the node `type` equals its `body` type; the binder is visible only in the `body` (never in its own `value`); `value` and `body` well-formed; the binder joins WF-064 (ch. 4.10) | gate 3 | `anchor/jutterswet-tariefstelling` — admitted through the Phase-5c let/decimals executor card | `wf076-body-type-mismatch` + `wf076-unbound-binder-in-value` |
| WF-077 | the condition-operand discipline: everywhere an `IfThenElse` is admissible (lambda, function or `Let` bodies), its condition READS only binders, parameters and literals (arithmetic combinators over those are admissible — the reads are what matter); an attribute/field/characteristic read routes through a `Let` binding first — the leeg-barrier at the read (ch. 4.10) | gate 3 | `anchor/jutterswet-tariefstelling` + `anchor/jutterswet-staffelvergoeding` (a `Binair` over binders as a condition operand) | `wf077-attribute-in-condition` + `wf077-fieldaccess-in-condition` |
| WF-078 | a `BinderRef`'s serialized `type` agrees in soort with its resolved binder's declared datatype (a match arm's constructor payload, a lambda/function parameter, a `Let` binder's value, a filter binder's element) — the leaf-ref cross-check WF-023/WF-027 do for a resolved `AttribuutRef`, soort only (decimals/units uncrossed, as there); closes the WF-035 additive-position exemption for binders (ch. 4.7) | gate 3 | `anchor/jutterswet-aanspraaktelling` (fold/match binders) + `anchor/jutterswet-tariefstelling` (`Let` binders) | `wf078-binderref-type-lie` |
| WF-079 | at most one unconditional, version-free computing rule (Gelijkstelling/Berekening) per (type, attribute) target without defaults — the single-writer discipline of WF-026/WF-050/WF-052 for bare attribute writers (ch. 7.7); Kenmerktoekenning exempt (monotone), guarded case splits and versioned forms outside; the PHASE5D-DIFF §8.2 un-pinnable form refused ahead of the square | gate 3 | `anchor/jutterswet-rekenkamer-overzee` (a guarded gewone/overzeese case split on one target — the fence does not over-fire on conditional writers) | `wf079-two-unconditional-writers` |
| WF-080 | a value-position `AttribuutRef` resolves to an attribute, not a characteristic (a characteristic is read through a characteristic check, ch. 4.5) — closes the tolerated `has_attr||has_kenmerk` leak in both the generic value walk and the aggregation-source path (a value-yielding fold's source; a count's role source stays in scope of WF-034); the rule TARGET lvalue is out of scope (WF-022) | gate 3 | `jutterswet-vondsttelling` + `anchor/jutterswet-penninguitgifte` (a `SOM` folding an attribute source; gate 3 witnesses none over-rejects) | `wf080-value-position-kenmerk` + `wf080-aggregation-source-kenmerk` |
| WF-081 | a numeric literal's lexeme is a bare number in the executors' shared grammar — after `,`→`.`: `'-'? digits`, `'-'? digits? '.' digits`, or `'-'? digits '/' digits` with a non-zero denominator (ch. 4.1); unit or currency tokens, whitespace, `+`, exponents and grouping are ill-formed (the unit belongs in the datatype's `eenheid`) — closes the gate blind spot where a unit written into the lexeme passed the verifier while both executors fault on it; date lexemes and `Tekst` literals out of scope | gate 3 | `anchor/jutterswet-tariefstelling` (integer and decimal lexemes through the fence; gate 3 witnesses none over-rejects) | `wf081-unit-in-lexeme` |
| WF-082 | the `bronAnnotaties` extension field (ch. 3.1): when present a non-empty list of non-empty trimmed statute annotations; a rule without annotations omits the field — absence leaves no trace | gate 3 | `jutterswet-hardheid` (one- and two-annotation rules on the wire, the unannotated rule traceless; replayed by gates 5–6) | `wf082-empty-bron-annotaties` |
| WF-083 | a fold-source argument never navigates (ch. 4.3): a non-empty `navigatiePad` is ill-formed; a `navigatieRol`, when present, is self-denoting — it names a role (ci, declared name or plural) the argument's object type itself fills; away-pointing is ill-formed — closes the silent fall-through where a navigating fold source passed the verifier while ref faults on it | gate 3 | `jutterswet-aanmeldertelling` (the self-denoting count "het aantal vondsten van de aanmelder" through both executors) | `wf083-fold-source-navigates-away` |
| WF-084 | a day-precision date literal's lexeme is the strict ISO form `yyyy-mm-dd` the executors parse (ch. 4.1) — fixed field widths (four-two-two digits), single `-` separators, month 1–12, day 1..days-in-month (leap-aware); the Dutch day-first source snippet is ill-formed, whether the bare `31-01-2024` (silently misread as year 31 → `0036-07-16`, a differential-blind wrong answer both executors agreed on) or the `dd. `-prefixed form (both executors fault) — closes that blind spot, the emitter now writing the bare ISO lexeme and keeping the snippet in `bron`; datetime lexemes out of scope | gate 3 | `jutterswet-vondstverjaring` (an ISO date literal through the fence, replayed by gates 5–6) | `wf084-source-snippet` + `wf084-bare-non-iso` |
| WF-085 | the day-kind reference graph is acyclic (ch. 5.4 / ch. 3.7): a day-kind condition may reference other day kinds (a `Predicaat` of soort `DAGSOORT`), but mutually recursive definitions have no fixpoint — a loud refusal where the v0 toolchain resolves a cycle to a silent false (the §9.10 recursion prohibition unenforced for day kinds); diverge-from-v0 (v0 neither checks nor documents it) | gate 3 | `jutterswet-schouwkalender` (an acyclic `hoogschouwdag -> schouwdag` reference, verifier-clean, replayed by gates 5–6 over its ordinary rule) | `wf085-dagsoort-verwijzingscyclus` (two mutually referencing day kinds) |
| WF-086 | a defaults group MUST NOT target a temporal attribute (declared `attribuutGranulariteit`) nor a temporal characteristic (`kenmerkGranulariteit`) — a defaults group decides at one fixpoint and has no timeline coordinate to write (ch. 6); the document-layer fence ahead of the executors (ref faults on both arms — defense-in-depth — and the Drools defaults service skips); temporal defaults targets are unscheduled (PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a) | gate 3 (over the prioritized-defaults node via the shared `attr_temporal` helper and the `kenmerkGranulariteit` table) | `anchor/jutterswet-vondstwaardering` (attribute defaults on a non-temporal target — gate 3 witnesses WF-086 does not over-fire) | `wf086-temporele-defaultstarget` (attribute arm), `wf086-temporele-defaultskenmerk` (characteristic arm) |
| WF-087 | the keuzetype family's nullary rule (ch. 2.4): a `SumType` declaration whose constructors ALL carry a null payload is ill-formed — the nullary keuzetype has one wire form, the `Enumeration` domain, so such a declaration normalises to it (nullary-as-sum is sugar, the enumeration is the kernel; two frontends cannot express one meaning in two shapes). Stated over the declaration; WF-061 carries the use sites. No converse: payload never converts | gate 3 (over `domeinen`, beside WF-060) | every payload-carrying SumType golden — `anchor/jutterswet-beschikkingsom`, `anchor/jutterswet-aanspraaktelling`, the `synth3-*` sum witnesses (gate 3 witnesses WF-087 does not over-fire on a keuzetype with payload) | `wf087-all-nullary-sumtype` + `wf087-unit-sumtype` (the lower boundary: ONE nullary constructor — a unit type is a nullary keuzetype too, and it is what a `List.length > 1` weakening of the rule would let through) |
| WF-088 | an `Enumeration` use site repeats the value list of the domain it names — same values (ci), declaration order (ch. 2.4). The enumeration sibling of WF-061, and what makes "the declaration is the source of truth" mechanically true for this variant: WF-039 reads the LOCAL list where WF-063 reads the DECLARATION, so a drifted copy passed both while ref faulted at evaluation and clingo derived nothing | gate 3 (model layer: attribute, parameter and `FunctionDef` datatypes; expression layer: every node's `type`, beside WF-007's number-form check — the same "every datatype in the document" split, though the two walks recurse differently — see WF-007's OPEN note). One position stays deliberately outside the walk: a rule's TARGET `type`, which is WF-022's lvalue, not part of the expression walk — a drifted copy there was verifier-clean at this rule's landing and measured harmless (drift the `doel` type of `jutterswet-keurstaat` and all three cases still replay to their pinned expectations in ref AND clingo, because both write through the target attribute's DECLARED datatype); since WF-091's rule-target arm the same drift is refused on an ATTRIBUTE target (residual structure mismatch, measured on that same document) and stays clean only on a CHARACTERISTIC target, where the identity family defers by construction (and a value that shares the target type's lie is refused by WF-023's keuzetype clause, which crosses that same declared datatype: `wf023-schrijf-doeltype-leugen`) | `jutterswet-vaarwatertoeslag-catala` (the `Vaarwater` domain repeated identically at FOUR use sites: the attribute datatype, the `Match` scrutinee's type, and the two operands of the equality — an `AttribuutRef` type and a `Literal` type) + `jutterswet-vormenkabinet` (the `kwaliteit` domain) | `wf088-use-site-value-drift` (a value the declaration lacks) + `wf088-use-site-value-order` (the same values, reordered — WF-006 order is normative) + `wf088-literal-type-drift` (an impeccable model layer, a drifted `Literal` type deep in a rule: the expression-layer arm) + `wf088-functiondef-drift` (three violations on one `FunctionDef` — parameter, result and body type — all reached through the walk's recursion into sum-type payloads, the arm no other case exercises) |
| WF-089 | domain identity, read half (ch. 2.4): a use site that TAKES a keuzetype value names the keuzetype — variant AND domain name — of the source it takes it from, whenever either side names one, ci, looked up through a `ListType` element. Two use sites: a **reference expression** takes it from the declaration it READS FROM — attribute (at the endpoint type; the dimension-cell read over it; the fold source, which the expression walk hands off to the fold-source shape rules), parameter, binder (a match arm's payload, a lambda/function parameter, a `Let` value, a filter element) — and an **element-passing aggregation** (`EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM`, the folds that select a contributor rather than compute) takes it from EACH OPERAND, which is itself anchored. **Since 2026-09-04 a third: a COMPOSITE position.** WF-091's derived arms (ch. 4.1) run this same facet engine against a formula instead of a declaration, and the number-follows-the-facet clause hands the domain facet to THIS rule there as well — an additive `Binary`, a `maal`, a `Rounding`, a `Clamp`, a `SOM`, a `TOTAAL` and an `AANTAL` all report under WF-089 when their annotation names a keuzetype domain their derivation does not (the element-passing folds keep the per-operand cross above instead, and the derived cross skips the facet there for exactly that reason). The derived side of that comparison names no domain whenever it is numeric, so the firing arm is the one-sided one. This is the domain extension of the WF-023/027/078 soort cross and the other half of WF-088/WF-061 (which bind a use site to the domain it NAMES). Its write half — every value entering such a declaration names the same keuzetype — is WF-023 (rule target, object-creation member, defaults consequence) and WF-067 (function body); no half suffices alone — the read half without the write half was measured open (fix round 1), the reference positions without the aggregation position were measured open again (fix round 2) — and only all three together make ref's "no arm" fault unreachable on a verifier-clean document whose case input the shared loader admitted. `TemporalRead`, `Invoke` and `Lambda` parameters are already crossed structurally (WF-049/068/070); `Construct` reads no declaration. The declaration side needs no rule: WF-060/065/071 fence a nested `Enumeration` out of every declaration position. Its struct sibling — the same sentence for struct domains, which are NOT keuzetypes — is WF-090 (2026-09-03), sharing this rule's machinery and call sites under its own number | gate 3 (the expression walk's `AttribuutRef`, `DimensieCel`, `ParameterRef` and `BinderRef` arms — the first two crossing a NAVIGATED read at its endpoint attribute, which the parameter and binder arms have no analogue for — plus the fold-source arm and the aggregation arm, and since 2026-09-04 WF-091's derived-arm entry point, where this number carries the domain facet at a composite; the unknown sentinel, an unresolvable reference or an absent `type` defers). Two honesty notes. The **ci** comparison is pinned since fix round 2 by `jutterswet-vaarwaterspelling`, the one golden whose use-site domain differs from the declaration in case alone: making the comparison case-sensitive turns that golden — and only that golden — red. The `ParameterRef` and `DimensieCel` arms have **no positive witness**: no golden, rood or parity document carries a keuzetype-typed `ParameterReference` or `DimensionCellRead`, so OVER-rejection in those two arms is unpinned (measured mutants that make either arm fire unconditionally leave gate 3 green); a keuzetype-typed parameter cannot be added to a golden today because the ASP carrier refuses the form ahead of translation | every keuzetype golden — `jutterswet-vaarwatertoeslag-catala` + `jutterswet-vormenkabinet` (enumeration references), `anchor/jutterswet-beschikkingsom` + `anchor/jutterswet-aanspraaktelling` (sum references: attribute, list-typed attribute, arm/lambda/function binders) — and a measured zero over-rejection across every golden and rood document and, read-only, the parity zone | `wf089-attribuutref-enum` + `wf089-attribuutref-sum` + `wf089-parameterref-enum` + `wf089-binderref-sum` (a `Let` binder) + `wf089-dimensiecel-enum` (verifier-only: the case format carries no cell input) + `wf089-attribuutref-lijst-sum` (the element-domain look-through: verifier-clean before, ref faulted, clingo folded 0 for 35.00 — a wrong answer) + `wf089-attribuutref-geen-domein` (one one-sided arm: typed number over an enumeration attribute — ref wrote the label into the numeric target, clingo faulted, Drools crashed at fire) + `wf089-attribuutref-geen-keuzetype-declaratie` (the mirror one-sided arm, which the other case does not pin: typed enumeration over a numeric attribute) + `wf089-attribuutref-navigatie-enum` (the navigated read, crossed at the endpoint attribute) + `wf089-enum-over-som` + `wf089-som-over-enum` (the variant half: one namespace, so the same name under the other variant is a lie — verifier-clean while both executors faulted in different words) + `wf089-aggregatie-doorgeefluik-enum` + `wf089-minimum-doorgeefluik` (the aggregation position, both fold halves: with one contributor the fold makes no comparison and hands an alien label on) + `wf089-foldbron-enum` (the fold source, which the expression walk never entered) + `wf089-composiet-domein` (fix round 2026-09-04: the COMPOSITE arm, a `PLUS` over two numbers annotated with a keuzetype domain — the one-sided arm at a derived position, which had a firing path since the derivation law landed and no artifact until this case. Mutant-proved: switching the whole derived cross off turns it clean together with the thirteen `wf091-*` derived cases and no others; switching the ADDITIVE arm off turns it clean with the four `wf091-binair-*` and no others; adding `domain` to that arm's skip list turns this case, and only this case of all 257 negatiefs, clean) |
| WF-090 | struct-domain identity (ch. 2.4): a struct-typed use site names the same struct domain (ci, through a `ListType` element) as the declaration it crosses. The struct sibling of WF-089 + WF-023's domain clause, over the same positions and the same verifier machinery under its own rule number (maintainer decision 2026-09-03 — struct domains are not keuzetypes, so the ch. 2.4 family term is unchanged). READ half: `AttribuutRef` (at the endpoint attribute of a navigated read), the `DimensieCel` over one, `ParameterRef`, `BinderRef`, the fold source, the element-passing aggregation against each operand, and — since 2026-09-04, through WF-091's derived arms — a **composite** position, where this number carries the struct-domain facet against the DERIVATION exactly as WF-089 carries the keuzetype one. No artifact, and the reason is measured: the only two composites in the four zones that touch a struct type at all are `EERSTE` folds (`wf090-doorgeefluik`, `wf090-foldbron`), the one position where the derived cross skips the domain facet by design because this rule's own per-operand aggregation arm already crosses it — so the composite arm is a firing path with zero occurrences here, and `wf089-composiet-domein` pins the shared engine under WF-089's number. WRITE half: rule target, object-creation member, defaults consequence (recursing into exception groups). No arm where an existing rule crosses the FULL datatype (WF-049/067/068/070/073/074); the aggregation arm reaches the ELEMENT-PASSING folds only, so a computing fold (`SOM`, `TOTAAL`) over struct operands of two domains is verifier-clean — a WF-038 operand question and no wrong answer (measured: ref faults `plus: expected a number, got {waarde: 120.5, telling: 3}` and the ASP carrier refuses the form) — and the ORDERABILITY flank WF-089 records holds for structs too: `StructType` is equality-only, so a truthful `EERSTE` with two struct contributors is verifier-clean and faults observably (measured: ref `unordered comparison: {waarde: 20, telling: 2} vs {waarde: 10, telling: 1}`, clingo refuses the form) — also WF-038's, not this rule's; the unknown sentinel, an unresolvable source and an absent `type` defer | gate 3 (the six reference/aggregation arms share one entry point with WF-089, the write half rides `wf023_assignment` — each arm falsifiable on its own). Two honesty notes, measured. The **ci** comparison is pinned since fix round 1 by `jutterswet-dossierspelling`, the one golden whose struct use sites differ from the declaration in case alone, at BOTH halves at once: making the comparison case-sensitive in `wf090_struct_identity` and in the struct clamp of `wf023_assignment` turns that golden — and only that golden — red on two messages. Of the six read arms only `AttribuutRef` (50 struct-typed references) and `BinderRef` (9) have a POSITIVE witness, and each is witnessed BROADLY: a mutant that fires on any struct-typed use site at the `AttribuutRef` arm turns TEN documents red (eight goldens + `rood/list-element-overprecision` + `rood/struct-field-overprecision`), the same mutant at the `BinderRef` arm five (`anchor/jutterswet-vondstdossier`, `jutterswet-dossierrecht-catala`, `jutterswet-kamerhuishouden-catala` and the two rood documents) — measured. The `ParameterRef`, `DimensieCel`, fold-source and aggregation arms carry no struct-typed use site anywhere in the corpus, so OVER-rejection in those four is unpinned, and a golden cannot be added today because the ASP carrier refuses the forms ahead of translation (measured: `TRANSLATION FAULT` on the faithful twins) | `anchor/jutterswet-vondstdossier` (the struct anchor and the richest witness of the two witnessed arms: nine struct-typed references and three `StructLiteral`s, all faithful, replayed by gates 5–6) + `jutterswet-dossierspelling` (the ci witness, both halves) + `jutterswet-vangstwaardering` + `jutterswet-waarderingsinvoer` + `jutterswet-dossierrecht-catala` + `jutterswet-kamerhuishouden-catala` + `synth3-nestwaarde` + `synth3-vondststaat` (the further non-over-rejection witnesses) — and a measured zero over-rejection across every golden and rood document: ten of the 171 carry a `StructType` at all, together 59 struct-typed references (`AttribuutRef` 50 + `BinderRef` 9) and 16 `StructLiteral`s. The parity zone is verified read-only too and is clean, but it confirms nothing HERE: measured, none of its 30 documents carries a single `StructType` | `wf090-schrijf-regeldoel` + `wf090-schrijf-objectcreatie` (where the ASP side reported a full green pass on a document ref cannot evaluate) + `wf090-schrijf-defaults` (the three write positions) + `wf090-attribuutref` + `wf090-attribuutref-lijst` (the `ListType` look-through, where clingo folded 0 for 12.50 — a wrong answer) + `wf090-binderref` (a lambda parameter, the same wrong answer) + `wf090-parameterref` + `wf090-dimensiecel` (verifier-only: the case format carries no cell input) + `wf090-foldbron` (the position the expression walk never enters) + `wf090-doorgeefluik` (the fold node's own type against its truthful operand) |
| WF-091 | reference-type identity (ch. 4.1): a use site's serialized `type` is IDENTICAL to the declaration it takes its value from — not compatible, not convertible — up to the ci comparison WF-001 fixes for a declared DOMAIN name, the three UNIT names compared BYTE for byte, facet by facet in one fixed order: **soort → domain → unit → decimals → wholeness → sign → residual structure**, of which the FIRST differing facet is reported — one comparison, one message, never two facets at once; per COMPARISON, not per path, since a walk that crosses one path repeatedly (the aggregation's domain cross, once per operand) can report it more than once. The rule NUMBER follows the facet and the arm: the domain facet is WF-089/WF-090 at every arm and the soort facet is WF-078 at a `BinderRef`, each keeping its wording; every facet without a number is this rule's and names itself in the message — so a further facet costs a message, not a rule number. Two clauses of the order are load-bearing: the soort facet **stands aside** wherever either side names a keuzetype or a struct domain (one namespace — that discrepancy is WF-089's variant/one-sided arms and WF-090's, numbered there), and the residual facet is the **backstop** that makes the refusal total (the domain facet looks THROUGH a `ListType` element, so a list-typed declaration read as its element passes both earlier facets). **Unit identity is structural** — abbreviation, declaring system, canonical unit, factor, denominator recursively, the three NAMES compared BYTE for byte and the factor exactly (copy fidelity is not name resolution, WF-001's domain: a consumer that reads those three names literally to pick its conversion diverges silently on a case-only lie — measured, `ref` 3.75 against a third executor's 3750.00 on `wf091-attribuutref-eenheid-kast`) — never convertible: `kg` and `t` share a canonical unit and a shape, so a lie inside the convertible class is compatible by construction and WF-035/WF-023's unit cross cannot reach it. ARMS: the five REFERENCE use sites WF-089/WF-090 name (`AttributeReference` at the endpoint of a navigated read, the `DimensionCellRead` over one, `ParameterReference`, a binder reference, the fold source), plus a rule TARGET's own `type` — the lvalue is WF-022's and the expression walk never enters it (the omission WF-088's row records as measured harmless for a drifted enumeration copy, both executors writing through the DECLARED datatype; it is not harmless for a unit). Their sixth use site, the element-passing AGGREGATION against each operand, stays outside this rule and keeps the domain facet alone: a reference type is a COPY of one declaration, an aggregation `type` is a type DERIVED over an operand list (`MINIMUM`/`MAXIMUM` take the least operand precision, the conjunction of wholeness, and operand 0's sign and unit; `EERSTE`/`LAATSTE` take operand 0's type in full), so identity against every operand is unsatisfiable the moment two operands differ — as in ch. 4.6's blessed "het minimum van het gewicht en 0". The domain is the one facet a pass-through fold hands on unchanged, which is why WF-089/WF-090 can cross it against EVERY operand and no further facet can. Since 2026-09-04 those further facets are crossed against the DERIVATION instead — one comparison, not one per operand — by this rule's second half. The other two write positions NAME their target and carry no reference type of their own: what enters them is WF-023's and WF-090's assignment cross — compatibility, a different law. **DERIVED ARMS (2026-09-04, ch. 4.1 "Derived-type identity on composite positions"):** the same law, same facet order, same rule-number RULE, against a FORMULA instead of a declaration — a composite has no single declaration opposite it, and the expectation is computed from the operands' own serialized types by RUNTIME-SEMANTICS ch. 2.3 and ch. 2.6. "Same number" means the same number-follows-the-facet clause, not one number: the DOMAIN facet reports under WF-089/WF-090 at a composite exactly as it does at a reference (measured — a `PLUS` and a `Rounding` annotated with a keuzetype domain each report WF-089's one-sided arm, pinned since fix round by `wf089-composiet-domein`), and only the facets without a number of their own are WF-091's here. Eleven positions, each claiming the facets its formula states and no more: additive `Binary` (maxDec, wholeness as conjunction, the LEFT unit, and a soort that COLLAPSES to a number — `Percentage + Percentage` derives `number`, so that is the only annotation the position admits; a date ± duration gives the date operand's type in full), `Binary` maal (the unit only — hand-through or rate cancel — with the same soort collapse), `Rounding` (the base plus its OWN decimal count), `Clamp` and `Aggregation` som/eerste/laatste (a pass-through), `Aggregation` totaal (the pass-through with a rate's denominator stripped wherever the fold INTEGRATES over time — the emitter's own `setLezing` predicate mirrored, so the non-temporal SET reading, an alias of the sum, passes the element type through instead), `Aggregation` minimum/maximum (least precision, conjunction, operand 0's sign and unit) and `Aggregation` aantal (whole, zero, unitless). A facet a formula does NOT claim is not crossed and takes the residual backstop with it, since the backstop would otherwise report exactly what the formula declined — and because the SIGN is claimed nowhere, that stand-down is total: no derived arm keeps the backstop, so the refusal there is exactly the row's named facets and, unlike the reference half, is not total by construction. The chain anchors only as far as the crossed operands reach: where every operand under a composite is itself crossed, the composite is bound to the declarations underneath it, but a FOURTH-CLASS operand breaks the chain — a `GEDEELD_DOOR` annotated `kg` over two unitless operands is a fabrication nothing crosses, and the law above it consumes it as ground truth up to a `kg` target, verifier-clean (measured on a probe; 26 such operand positions over 19 of the 174 golden+rood documents of the measured 2026-09-04 state — one merge before `jutterswet-tempototaal` made it 175 — 23 of them under a division). The sentinel/empty door is shut (an untyped or sentinel-typed operand is WF-034-ill-formed) but the fabrication door is the stronger escape and stays OPEN: it does not switch the cross off, it feeds it a premise. What stays UNCROSSED is ch. 2.10's fourth class: a division, a power, a percentage-of, a root, an absolute value, the date/time extractors — v0 derives no result unit there — plus a duration node (the annotation IS the source, WF-038), and `IfThenElse`/`Let`, whose whole type WF-069 and WF-076 already cross. That set and gate 13's exception set are TWO INDEPENDENTLY MAINTAINED LISTS, aligned by hand with no tripwire comparing them: they differ, measured, on `AANTAL` (derived here, not in gate 13, which twists units only), on the duration OPERAND axis gate 13 exempts and this rule has no notion of, on a `Let` gate 13 twists, and on `GEDEELD_DOOR_ABS`, which gate 13's prose omits and its set-complement covers. Outside: `TemporalRead`, `Invoke`, `Lambda` parameters, `StructLiteral` and `FieldAccess` already cross the FULL datatype (WF-049/068/070/073/074), so no node is crossed twice under two identity rules; `Construct` reads no declaration; WF-023's write-side PRECISION gate is a different question and stays OPEN. The unknown sentinel, an absent `type` and an unresolvable declaration defer | gate 3 (one entry point shared with WF-089/WF-090 plus the rule-target registration, and since 2026-09-04 a SECOND entry point beside the expression walk's type prologue for the derived arms — one function, one facet order, one rule number; every arm and every facet mutated and measured). Four honesty notes. (1) At the REFERENCE arms, which claim the whole datatype, the named facets decide the **diagnosis** and the residual facet decides the **refusal**: neutering any named facet degrades its case's message to `residual structure mismatch` while the document stays refused — only neutering the RESIDUAL facet lets a document PASS (`wf091-attribuutref-reststructuur`, measured). At the DERIVED arms that does not hold and cannot: every one of them claims less than the whole datatype and stands the backstop down with the sign, so neutering a named facet lets its case pass outright (measured in fix round: the unit facet turns all seven derived unit cases clean). What the facet order pins is therefore the NUMBER, and that is falsifiable: neutering the soort facet moves `wf078-binderref-type-lie` to WF-091, and neutering the engine's domain facet moves 18 of the 24 `wf089-*`/`wf090-*` cases to WF-091 — gate 3 red in both cases. The six that keep their own number are the ones that do not run through the engine: the three element-passing-aggregation cases, whose arm calls the domain cross DIRECTLY (it is not an arm of this rule), and the three `wf090-schrijf-*` cases, which ride the assignment cross. (2) The **ci** comparison of the DOMAIN names is pinned by exactly two goldens: making the residual comparison case-sensitive turns `jutterswet-dossierspelling` and `jutterswet-vaarwaterspelling` — and only those two — red. The UNIT names are pinned the other way round and by a negatief: folding the three of them ci (in `eenheid_identical` and in the residual comparison form) turns `wf091-attribuutref-eenheid-kast` — and only that case — clean, gate 3 red (measured, fix round 2). (3) POSITIVE-witness breadth per arm, measured with a mutant that fires on every unit-bearing use site at one arm, re-measured over the 174 golden+rood documents in fix round 2: rule target **15** documents red, `AttribuutRef` **13**, `BinderRef` 2, fold source 1 (`jutterswet-vondsttelling`), and — since 2026-09-04 — `ParameterRef` 1 (`jutterswet-aanvoergeld`) and `DimensieCel` 1 (`jutterswet-seizoenvondst`), the two arms that carried **zero** until then. The first two numbers stood at 13 and 11 before that date and grew by exactly the two documents added for the new arms: `jutterswet-aanvoergeld` and `jutterswet-seizoenvondst` carry unit-bearing rule targets and attribute references too, so each counts in three lists at once — the breadth of the two established arms did not otherwise change. Each new witness is TWO-SIDED, measured: the golden is clean under the unmutated engine, and a `t`-for-`kg` copy of that same arm in that same document (one canonical unit, factor 1000 — the convertible class no other rule reaches) is refused arm-exact (`unit mismatch: parameter reference 'aanvoerdrempel' …`, `unit mismatch: dimension-cell read of 'vondstgewicht' …`). The `DimensieCel` witness is necessarily VERIFIER-ONLY, and that too is measured, not assumed: a cell read is declined by every executor (`ref` `EVALUATION FAULT: expression kind not implemented in this card`, clingo `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`, the Drools backend `EXCLUDE dimensioned attribute input … is not carried by the v0 backend`) and the case format carries no per-cell input variant at all, so no case over that document could pin anything and gates 5–6 would turn red on one that tried. What closes at these two arms is the UNIT facet only: no golden carries a keuzetype-typed or struct-typed `ParameterReference` or `DimensionCellRead`, so the corresponding notes at WF-089 and WF-090 stand unchanged. The parity zone carries 12 unit-bearing `ParameterRef` use sites over 6 documents and is verified read-only, but it is not a golden and pins nothing here. Both arms also carry a unit-facet negatief of their own (`wf091-parameterref-eenheid`, `wf091-dimensiecel-eenheid`, the latter since fix round 2, which completes the set: every arm this rule crosses in full has one) — a negatief pins the REFUSAL and a golden the NON-over-rejection, and since 2026-09-04 both halves exist at every arm this rule crosses in full. (4) The DERIVED half's coverage, and what it still leaves open. It performs **228** comparisons over 102 of the 174 golden+rood documents (the measured 2026-09-04 state; `jutterswet-tempototaal` landed one merge later and adds a 175th document with two TOTAAL comparisons — the strip witness in the negatief column is measured against that 175 state; measured 2026-09-04 with a temporary probe in `composite_identity`, which the same walk reproduces per arm: additive `Binary` 114, maal 38, rounding 28, aantal 18, som 12, totaal 7, clamp 3, eerste 3, laatste 3, minimum 1, maximum 1), of which only **14** are unit-bearing, spread over 10 documents. So the UNIT facet has a golden witness at three arms only — additive `Binary` (8 documents), maal (3) and som (1) — and at the eight others it is pinned by a negatief and by nothing else, which pins a REFUSAL and never a non-refusal. The maal arm's CANCEL rule has no corpus witness at all: no `Binary` maal node in any of the four zones carries two unit-bearing operands, so `wf091-maal-eenheid` is its whole evidence. The counter-shape of note 4's own old example still cannot become a golden — the ASP backend refuses a multi-operand `MINIMUM`/`MAXIMUM` ahead of translation, so it can carry no pinned expectations — and that is exactly why the artifact that closes it is `wf091-foldwortel-eenheid`. Three formulas stay OUTSIDE the law with their reason and their price, each measured the same day: the SIGN at ALL ELEVEN derived positions without exception (it runs both ways — a derivation mostly under-claims it while a document narrows truthfully, the sharpest case being a clamp with a minimum of 0, which IS non-negative whatever its base says; and it over-claims at the aantal arm, where two anchor goldens widen `NIET_NEGATIEF` to `ONBEPAALD`. 12 documents over the three zones narrow it truthfully and 5 widen it; crossing it costs 5 golden+rood documents on the additive arm alone, 7 at every arm, 27 with every exclusion emptied — the six arms corrected in fix round carry NO golden+rood witness, so turning the sign off there left the verifier's report over all four zones byte-identical and the class is pinned by a probe, not by the corpus); the PRECISION of a maal, whose price needs both numbers because they are different measurements (against ch. 2.3's actual rule — the SUM of the operands' decimals, never whole — 9 decimals and 9 wholeness divergences over the four zones and **11** golden+rood documents red, 3 on decimals and 8 on wholeness; against the PLACEHOLDER the implementation writes where the facets are unclaimed, zero decimals and fractional, merely un-skipping the two facets turns **20** red. 11 is the rule's price, 20 the placeholder's — this corpus carries three producers with three precision policies, the Catala plugin writing the pinned ch. 2.9 scale); and the pro-rata builtin plus the one-sided additive unit pair — of which only ONE ORDER is contested (a unitless LEFT beside a unit-bearing right, where ch. 2.6 gives no unit and both executor-side derivations give the right one; with the unit on the LEFT all four sources agree). The rule claims neither order: its guard is the pair's unit compatibility, and splitting it would pin the uncontested half — which carries zero golden+rood occurrences — while leaving the contested half open anyway. The one node the corpus does carry is in the contested order and annotates the RIGHT unit, against ch. 2.6 (`jutterswet-sleeprest`, `50 min <km>`); it is also a shape WF-035's adoption clause settles independently, the unitless side being a literal, so the genuinely open class is narrower still. Two facets of two arms have no artifact either way: a `Rounding` over a PERCENTAGE base (zero occurrences in four zones — as for the fall-through row, a rounding over a base that is neither), and the opaque PRODUCT of two plain units, whose algebra ch. 2.5 fixes but whose wire spelling it does not | every unit-bearing golden — `jutterswet-vondsttelling` (the fold-source witness), `jutterswet-lichterloon` + `jutterswet-vondstrecht` (the two `BinderRef` witnesses), `jutterswet-meldloon` + `jutterswet-sleepbaan` + `jutterswet-sleeprest` + `jutterswet-stukopbrengst` + `jutterswet-vergunningsduur` + `jutterswet-vervaltermijn` + `jutterswet-vondsttarief` + `ww-herleving-inkomen` + `synth-grensrand` (the `AttribuutRef` arm) + `jutterswet-jutleeftijd` + `jutterswet-getijdenpaar` (further rule-target witnesses), plus `jutterswet-dossierspelling` + `jutterswet-vaarwaterspelling` (the two ci witnesses), plus `jutterswet-aanvoergeld` (the `ParameterRef` arm, two unit-bearing parameters in two unit systems, read in a condition and in a value) + `jutterswet-seizoenvondst` (the `DimensieCel` arm, verifier-only — see honesty note 3) — and a measured zero over-rejection at every facet across every golden and rood document. What that measurement counts, stated exactly, because two earlier counts of it disagreed: **929 comparisons the engine PERFORMED** over the 174 documents of that same dated state (834 golden + 95 rood), **46 of them unit-bearing** — one per arm entry that reaches the facet chain, tallied by a temporary per-arm probe over every `ir.json` in both zones (re-measured 2026-09-04 with the same probe, which reproduces the previous 921/38/71/14 exactly on the 172 documents of the previous count). It excludes 72 further invocations that DEFER before comparing anything (an absent `type`, the v0 unknown sentinel, an unresolvable declaration), and it excludes the element-passing aggregation's domain-only cross (14 more comparisons, none unit-bearing), which is no longer an arm of this rule. It also counts the REFERENCE half only: the derived arms perform a further 228 comparisons over the same two zones, tallied and split per position in honesty note 4. Same count in the parity zone, read-only: 147 performed comparisons over 30 documents, 30 of them unit-bearing, plus 5 aggregation comparisons — that last number re-measured in fix round 2 and stated with the distinction two earlier counts blurred: the aggregation site is ENTERED **6** times there, and **5** of those entries perform a comparison, one deferring before it. In golden+rood the two numbers coincide at 14, which is why only the parity zone ever showed the disagreement | `wf091-attribuutref-eenheid-converteerbaar` (the rood-eerst case: the same canonical unit, factor 1000 — the class WF-035's compatibility cross admits by construction) + `wf091-attribuutref-eenheid-ontbreekt` + `wf091-attribuutref-eenheid-erbij` (the two one-sided unit arms) + `wf091-attribuutref-decimalen` + `wf091-attribuutref-geheel` (the two precision forms WF-007 admits that share a decimal count) + `wf091-attribuutref-teken` (a WIDENED sign restriction, which WF-023's contradiction clause cannot see) + `wf091-attribuutref-soort` + `wf091-attribuutref-reststructuur` (a list-typed declaration read as its element: the hole the facet order makes by design) + `wf091-binderref-eenheid` (the arm where WF-078 keeps the soort) + `wf091-parameterref-eenheid` (the arm without a golden witness) + `wf091-foldbron-eenheid` (the fold source, which the expression walk never enters) + `wf091-schrijfdoel-eenheid` (the rule target) + `wf091-attribuutref-eenheid-kast` (the case-only unit lie, which pins the BYTE comparison of the three unit names — see honesty note 2) + `wf091-dimensiecel-eenheid` (the cell read's own type, the last arm without a unit case) — and, for the DERIVED half, thirteen more, one per (position, facet): `wf091-binair-eenheid` (the rood-eerst case, the 550.000 shape: `kg` derived, `t` annotated) + `wf091-foldwortel-eenheid` (honesty note 4's own counter-shape, a `MAXIMUM` over `kg` annotated `t` — 3,75 against 3.750.000, and the negatief exists because a golden cannot) + `wf091-binair-decimalen` (the maxDec rule, under-reported — the direction that walks past the ch. 2.4 assignment gate) + `wf091-binair-geheel` (the conjunction rule, both sides 0 decimals so WF-007's form check passes either way) + `wf091-binair-soort` (a plain sum annotated a percentage) + `wf091-maal-eenheid` (the ch. 2.6 rate CANCEL, the one arm with no corpus witness at all) + `wf091-afgerond-decimalen` (a rounding claiming not to have rounded) + `wf091-afgerond-eenheid` + `wf091-begrensd-eenheid` + `wf091-som-eenheid` (the fold ROOT, where `wf091-foldbron-eenheid` lies at the SOURCE) + `wf091-totaal-eenheid` (the §4.20 denominator strip, which it reaches by carrying `set: false` on both its operands; removing the strip turns exactly this case green, and also turns `jutterswet-tempototaal`'s temporal node red — while making the strip UNCONDITIONAL, the shape before fix round 3, turns that same golden's non-temporal set fold red. The strip is therefore two-sided; the granularity half of the emitter predicate that guards it is not, and says so at ch. 4.1) + `wf091-fold-decimalen` (the least-operand-precision formula: its operands stand higher-precision-first so that taking operand 0's precision instead turns exactly this case green) + `wf091-aantal-decimalen`. Mutant-proved per (facet, arm), re-measured in fix round after the sign exclusion became total: neutering the unit facet turns all **7** unit cases clean (before that round it turned 3 clean and degraded 4 to `residual structure mismatch`, the split the claims-every-facet column then made — no derived arm keeps the backstop now, so the degradation is gone); neutering decimals, wholeness or soort turns 2, 1 and 1 clean; disabling one position turns that position's cases clean and no others; disabling the derived cross entirely turns all thirteen clean — fourteen with `wf089-composiet-domein` — and leaves every reference-arm case red |
| WF-092 | unit wire form (ch. 2.5): a unit spells its CONVERSION PAIR whole — `canoniek` and `factorNaarCanoniek` filled together or null together, on the numerator and, recursively, on the `noemer` of a rate. The BARE form (both null) stays admissible and is not a dead branch: 28 occurrences over the golden corpus (the WF-038 duration parameters, the WF-035 duration operands), and making the executor's bare branch fault turns 9 corpus cases over 3 goldens red. Either HALF is ill-formed — a canonical target with no factor states a conversion it cannot perform, a factor with no canonical target names no conversion at all. It is a SHAPE law on ONE unit, not a cross between two, which is what no neighbouring rule can supply: WF-009 tests a factor it FINDS and a chain it can FOLLOW, so a missing half is nothing to look at; WF-035/WF-023 cross units for CONVERTIBILITY, which reads the canonical names and never the factor, and reads a missing canonical as the ABBREVIATION — so the half-form is compatible with its whole sibling by construction wherever those two coincide, and refused for the WRONG reason wherever they do not (measured on `km`: `WF-023 assignment across incompatible units` plus `WF-035 additive operands have incompatible units`, which is why the factor-half case sits on the SELF-CANONICAL `m`); and WF-091 compares byte for byte, agreeing the moment the half-form is copied consistently across the arms it crosses. Because it is a shape law it holds at every position a unit can occupy — the declared systems (`/eenheidstelsels/*/{eenheden,aliassen}/*`), the model layer (attributes, domains, parameters, `functionDefs` parameters and result), every walked expression `type`, a FOLD SOURCE's own `type` and an inline LAMBDA's parameter datatypes (the two positions the expression walk does not reach: the fold-source branch replaces that walk, and a lambda's parameters are declared where only its body is walked), and a rule TARGET's own `type`, the position the expression walk never enters either — and a document carrying the shape is refused ONCE PER POSITION, which is what makes the position set falsifiable at all. That totality is measured, not asserted: an injection sweep that puts a half-filled unit at each of the goldens' **2161** `Numeriek`/`Percentage` positions in turn and demands a WF-092 citation reports **0 unreached** (it reported 85 before the fold-source and lambda-parameter call sites landed — 27 and 58 respectively). The unit extraction reaches strictly FURTHER into a datatype than WF-007's own form check: it recurses through a `ListType` element, a `StructType` field and a `SumType` payload, three of the doors WF-007's row records as out of its reach — measured, no unit sits behind one of them in any zone today, so that recursion carries no corpus witness and is pinned by the injection sweep alone, which does reach them through the lambda-parameter site. MEASURED DRACHT ZERO at the moment it lands: over the 476 `ir.json` of the four zones (153 golden, 22 rood, 271 negatief, 30 parity) not one half-filled unit exists outside the three cases this rule brings — golden 213 units, 28 bare / 185 whole, 11 of them denominators; rood carries NO unit at all; the parity zone 1510 units, every one whole, no denominator; 34 distinct unit forms over golden+rood+parity. The rule therefore refuses a shape nothing in the corpus writes, and its evidence of non-over-rejection is the corpus itself. What it does NOT say: nothing about a factor whose VALUE is wrong (WF-009 owns positivity, and a wrong-but-positive factor is unreachable by any rule), nothing about the bare form's meaning, and nothing at a use site — the identity cross stays WF-091's | gate 3 (one shape function `check_eenheid_form` with its denominator descent, one datatype extraction, six call sites; every branch and every call site mutated and swept byte for byte over all 476 documents of the four zones, and the position set additionally pinned by the 2161-position injection sweep). Four honesty notes. (1) BRANCH-exactness, measured: neutering the canonical-without-factor branch turns `wf092-canoniek-zonder-factor` AND `wf092-noemer-zonder-factor` clean (they share that branch) and nothing else; neutering the factor-without-canonical branch turns `wf092-factor-zonder-canoniek` clean, alone; removing the denominator descent turns `wf092-noemer-zonder-factor` clean, alone. Every one of the three mutants leaves every OTHER document in the four zones byte-identical — zero over-rejection and zero under-rejection elsewhere. The third mutant takes TWO edits, not one: dropping the recursive call also makes the `rec` flag unused, which the build treats as an error (warning 39), so an edit that removes only the call fails to build and leaves a sweep quietly re-running the previous binary — a green that means nothing. (2) POSITION-exactness, measured the same way and weaker BY DESIGN: no call site can be pinned by a case turning clean, because each case is refused at several positions at once. What each mutant loses is CITATIONS, and the six sites partition the 19 citations exactly: `eenheidstelsels` **2**, model layer **6**, walked expression `type`s **5**, fold sources **3**, rule targets **3** — and the lambda-parameter site **0**. That zero is the honest one: no document in any zone carries a unit on a lambda parameter, so that call site loses no citation when it is removed and its whole evidence is the injection sweep (58 of the 85 positions it opened). A position whose citations vanish without any case turning red is exactly what a per-position negatief could not fix — the shape law is total by construction, so redundancy is the design, not a gap. Three RESIDUAL positions stay outside the totality and are named rather than papered over: a fold source whose `objecttype` or whose `attribuut` is absent, and a Lambda in a position where no lambda is admissible. Each sits behind an arm that has already REFUSED the node — WF-034 twice, WF-069 once — so the document is rejected, but rejected without a WF-092 citation at that unit. Measured over the four zones: the three negatiefs that reach those arms are `wf034-foldbron-zonder-objecttype` and `wf034-foldbron-zonder-attribuut` (8 units each, every one whole) and `wf069-lambda-outside-fold` (no unit at all), so extending the rule into them would add a branch no document in any zone could witness — the honest note is the artifact here, not the code. (3) POSITIVE-witness breadth per position, measured with a probe that fires WF-092 on EVERY unit at one position: declared systems **15** goldens, model layer **20**, walked expression types **19**, rule targets **16**, fold sources **2**, lambda parameters **0**, out of the 22 unit-bearing goldens; rood scores **0** at all six, because it carries no unit anywhere, so the red zone witnesses nothing here and says so. (4) The EXECUTOR asymmetry the rule answers to, measured on the two plain-unit cases against the verifier that did not yet carry it (it reported `verifier: PASS` on all three): on the canonical-without-factor half, `ref` and the ASP backend both read the missing factor as 1, compute `2,5 + 300 = 302,5` where the restatement should have given `2,8`, and report a DECIMALS complaint about a number whose unit was already wrong, while the third executor names the missing factor; on the factor-without-canonical half nothing faulted anywhere and the three executors returned TWO answers — 2800, 2800 and 302.5 — a wrong value from a document every rule admitted. Both halves are now answered in the executors, so the second line of defence is not a dead letter on either: `ref`'s factor question faults by name on BOTH (measured on the same case files, `EVALUATION FAULT: unit m carries conversion factor 1 but names no canonical unit to convert into (WF-092)` where it used to replay a clean 2800), and the ASP backend inherits both refusals unchanged through the same function, as `TRANSLATION FAULT` (its own translator carries no factor reading — its unit fence compares whole unit records). The third executor still computes 302.5 on the factor-without-canonical half: there the missing canonical makes its convertibility predicate answer "not convertible", which its ungated predicate callers read as "no restatement needed" rather than as a refusal. That is a backend question on a shape no corpus document carries and this rule now refuses before any executor sees it | every unit-bearing golden — **22** of the 153, each a witness that the rule does not over-reject, at the six positions with the breadth honesty note 3 measures (15 / 20 / 19 / 16 / 2 / 0). Named for the two shapes the negatiefs copy: `jutterswet-sleepbaan` (the factor≠1 conversion the two plain-unit cases break, `km → m ×1000`) and `jutterswet-tempototaal` (the only golden in the corpus carrying a rate unit at all, hence the only possible base for the denominator case). The three further factor≠1 goldens `jutterswet-berging` (`t → kg ×1000`), `jutterswet-sleeprest` (`km → m ×1000`) and `jutterswet-vergunningsduur` (`jaar → maand ×12`) witness the executor half: they are the corpus's whole stock of genuine conversions, and gates 5 and 6 replay all 403 cases unchanged with the executor fail-fast in place. The parity zone, read-only, carries 1510 whole units over 30 documents and pins nothing here | `wf092-canoniek-zonder-factor` (the rood-eerst case: `km` naming canonical `m` with `factorNaarCanoniek: null`, verifier-clean before the rule and carrying the measured three-executor split of honesty note 4) + `wf092-factor-zonder-canoniek` (the mirror half on the SELF-CANONICAL `m`, the one stand in which no neighbouring rule masks it — the sharper case, since before the rule NOTHING faulted and the three executors disagreed in silence; two of the three now refuse it by name, the third is honesty note 4's open backend question) + `wf092-noemer-zonder-factor` (the denominator, the recursion's whole evidence: without it the descent is unfalsifiable, since both plain-unit cases are refused at identical positions with and without it. Necessarily VERIFIER-ONLY and measured so: its base golden is one of the three gate 5 lists as not replayed, and it is the only golden with a denominator, so no case over it could pin an executor value) |

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
