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
  extension. **First pin bump (2026-08-17, toolchain `edecb46`):** the
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
  normalizes to the whole form (v0 behaviour, kept and specified).
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
  scrutinee's, while a `FunctionDef` parameter or result and an ordinary
  expression `type` naming a ghost domain are **unenforced**. Recorded
  here rather than repaired: backlog
  `20260901-use-site-declaratie-identiteit`, which also carries the other
  open half — that this rule binds a use site to the domain it *names*,
  never to the declaration it *reads from*.
  *v0 mirror note:* unchecked in v0. **Delta (promotion).**

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
  statically ill-formed, never false, ch. 11).

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
fold source (ch. 4.3, WF-034): name resolution first (declared name or
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
  variable read is ill-formed — and the invocation graph over
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
- **WF-023** Assignment typing: the value's type MUST be assignable to the
  target's declared type — same family, compatible units, enumeration
  labels from the target's domain, sign constraint respected, and the
  precision gate of `RUNTIME-SEMANTICS.md` ch. 2.4 (including its
  candidate rule: a possibly non-terminating value passes explicit
  rounding before a bounded write). *v0 mirror note:* enforced as resolver
  errors via the §6/Tabel 61 typing tables — except for characteristic
  attribution, where v0 skips the check (harmless today: the value is a
  constructed constant).
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
  boolean-typed expression (a boolean constant is the v0 form). The value
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
argument's `attribuut` slot. Resolution order is normative: the name
resolves FIRST, case-insensitively over the declared role name and the
declared plural — never a synthesized plural (the ch. 2.6 decision).
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
pinned (WF-040); no storage layout implied.

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
  scoped exception is the fold-source role name (ch. 4.3): a written
  name that resolves to no declared role (ci, declared name or declared
  plural) still resolves through the type-pair fallback **iff** the type
  pair derives a unique connecting role — exactly one fact type connects
  the fold subject type to the element type, and not a self-typed
  two-role fact type (which offers both its roles, blocking the
  derivation, ch. 4.1); zero or multiple connecting roles leave it a
  dangling reference — ill-formed. *v0 mirror
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
`Construct`, `Match`, `BinderRef` — never emitter-produced; CONSTRUCTS
row 36; semantics `RUNTIME-SEMANTICS.md` ch. 9; design and ratified
decisions `spec/proposals/PHASE5B-KERNEL.md` §2).

| Node | Fields | Notes |
|---|---|---|
| Construct | `type` (the SumType), `constructor`, `argument?` (expr), `source` | builds a constructor value; a leeg argument propagates (the whole node evaluates leeg — a constructor value never carries a leeg payload) |
| Match | `type` (the result datatype), `scrutinee` (expr), `arms`, `source` | takes a **keuzetype** value apart (ch. 2.4) — the scrutinee is sum-typed *or* enumeration-typed. Each arm: `{constructor, binder: string \| null, body: expr}`; on an enumeration scrutinee `constructor` carries the declared value label and `binder` is always null (enumeration alternatives are nullary). Evaluation `RUNTIME-SEMANTICS.md` ch. 9.2 — a leeg scrutinee propagates, no implicit leeg-arm, in both variants |
| BinderRef | `name`, `type`, `source` | reads the nearest enclosing binder (a unary match arm's `binder`; the row-10 card adds lambda and function parameters). Deliberately distinct from `VariabeleRef`, which stays the rule-local let-binding of row 24 with its unserialized definitions and its motivated OPEN — the two roles never share a node |

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
  cross is by soort only — number, text, boolean, date, sum type, list,
  struct — leaving decimals and units uncrossed exactly as the
  `AttributeReference` cross does. A binder whose declared datatype is
  unknown, or a reference carrying no `type`, defers. *Motivation:* WF-035
  reads the same serialized `type` but exempts additive (`+`/`−`) operands
  so date ± duration can pass; a soort-lie on a binder in an additive
  position slips that exemption — verifier-clean, ref computes on, clingo
  faults the translation — the split the adversarial hunt found (the
  card). WF-078 crosses the annotation against the binder's real datatype
  at every use, closing the gap independent of operator position. **Delta
  (promotion).**

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

The **WF-069 closed-body whitelist widens** (the Phase-5c widening):
`FieldAccess` and `StructLiteral` are admissible inside a closed lambda
or function body — the tutorial's map-over-struct-field
(`λ individual. individual.kindertal`, a `FieldAccess` on a binder) is
otherwise ill-formed inside a lambda body (the reads that break closure —
attribute, dimension, aggregation, temporal, variable — are unchanged;
the two record nodes join the admissible set, their own operand reads
still following the closed-body discipline).

### 4.10 The value-level binding — Let

`layer: core`; **extension node** per the Method (wire node `Let` —
never emitter-produced; Phase-5c core; semantics
`RUNTIME-SEMANTICS.md` ch. 10.6 — **call-by-need**, maintainer decision
O3 2026-09-01; design and ratified decision
`spec/proposals/PHASE5C-TUTORIAL.md` §6, question 2 = A). A `Let` is an
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
| WF-007 | number precision is exactly one of whole / fixed / unbounded | gate 3 | `jutterswet-vondstnummer` | `wf007-whole-with-decimals` |
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
| WF-023 | assignment typing (family, units, labels, sign, precision gate) | gate 3 (the shallow slice between the serialized target and value types: family — numeric kinds as one — unit compatibility with literal adoption, contradictory sign restrictions; the precision gate needs expression-shape analysis and stays OPEN) | `jutterswet-vondsttelling` (numeric assignments) + `jutterswet-vormenkabinet` (the enum assignment) | `wf023-text-into-numeric` |
| WF-024 | allocation method is one of the three real methods (no COMPLEX) | gate 3 | `jutterswet-berging` | `wf024-complex` |
| WF-025 | allocation fields valid per method; ordering key orderable; share typing | gate 3 (field validity and orderability; share typing is WF-023) | `jutterswet-berging` | `wf025-max-without-ratio` |
| WF-026 | at most one allocation per written target | gate 3 | `jutterswet-berging` | `wf026-two-allocations-one-target` |
| WF-027 | object-creation members declared on the created type; characteristic init boolean | gate 3 | `jutterswet-penninguitgifte` (anchor: the mint, its member initialisations and both guards execute, gates 5–6) | `wf027-ghost-member` |
| WF-028 | symbolic fact creation all-or-nothing and resolvable; join chain reaches target; new ≠ final hop | gate 3 (symbolic extension form; a bean-field-only v0 record stays uncheckable — recorded) | `jutterswet-gemeenteberging` (anchor: the chain walk executes and folds over the created relation pin absolute values, gates 5–6) | `wf028-half-symbolic-creation` (all-or-nothing) + `wf028-create-along-final-hop` (the full chain walk) |
| WF-029 | consistency assertion has a single subject | gate 3 | `jutterswet-vondstnummer` | `wf029-no-subject` |
| WF-030 | uniqueness sources: keys resolve; arity, datatype kind and units agree per position; a union carries ≥ 2 sources | gate 3 | `jutterswet-vondstnummer` + `anchor/jutterswet-kustregister` (the union — including the per-source sum over two same-type sources — and both scoped forms, executed by gates 5–6) | `wf030-arity-mismatch` + `wf030-single-source-union` |
| WF-031 | one definition per day kind; condition present and **pure** (no attribute/characteristic read — the stratum-0 framing, ch. 5.4); validity ALTIJD | gate 3 | `jutterswet-vormenkabinet` (the schouwdag definition, ALTIJD validity) + `jutterswet-schouwkalender` (a pure day-kind chain, replayed by gates 5–6 over its ordinary rule) | `wf031-missing-condition` + `wf031-dagsoort-venster` (bounded validity window) + `wf031-dagsoort-leest-kenmerk` (the purity clause: a day-kind condition reading a characteristic) |
| WF-032 | start point: variable-start timeline, day-precision member, one per pair | gate 3 | `jutterswet-vormenkabinet` (the meetdatum anchor) | `wf032-fixed-start-anchor` |
| WF-033 | (dialect) rule-group members resolve; recursive group has a creation member (either kind); object creations guarded, relation creations guard-free | gate 3 | `anchor/jutterswet-wachtersketen` (the recursive chain executes to its fixpoint, gates 5–6; the runaway budget fault is red-pinned, gate 8) + `anchor/jutterswet-erflijn` (the guard-free relation-creation member: the transitive closure reaches its fixpoint in both executors) | `wf033-unguarded-recursive` (the guard clause) + `wf033-no-creation-member` (the containment clause) |
| WF-034 | every expression/condition reference resolves (no unknown-type sentinel) | gate 3 (partial: sentinel types and parameter/day-kind/element-type/own-member references; variable bindings and navigation paths are not serialized in v0; excepted: a v0 instance-fold source argument names a role and carries the sentinel by construction — the row-1 wart, resolved against declared roles with the ch. 4.3 type-pair fallback for a name that resolves nowhere) | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondsttelling` (role source) + `jutterswet-kaveltelling` (the type-pair fallback: an undeclared synthesized plural over a unique pair, both executors) | `wf034-dangling-parameter` + `wf034-ambigu-typenpaar` (unresolved name, two connecting fact types) + `wf034-zelftype-typenpaar` (unresolved name, one self-typed two-role fact type — both roles are candidates, counted in roles not fact types) |
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
| WF-067 | FunctionDef: ci-unique names, typed unique parameters, resolvable result, closed body (the 5c widening admits FieldAccess/StructLiteral), acyclic invocation graph (ch. 2.11) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf067-recursive-functions` + `wf067-attribute-read-in-body` |
| WF-068 | Invoke: resolvable function, arity, argument types, result type (ch. 4.8) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf068-unresolved-invoke` + `wf068-arity-mismatch` |
| WF-069 | Lambda only as a Fold/MapList operand and the operand only an inline Lambda (no bare FunctionDef name); IfThenElse only inside lambda, function or (the 5c widening) `Let` bodies; lambda arity per host, typed parameters, closed bodies (the 5c widening admits FieldAccess/StructLiteral); IfThenElse branch agreement (ch. 4.8/4.10) | gate 3 | `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-vondstdossier` (the map-over-struct-field body) + `anchor/jutterswet-tariefstelling` (the Let-body IfThenElse) | `wf069-lambda-outside-fold` + `wf069-ifthenelse-outside-body` + `wf069-named-function-operand` |
| WF-070 | fold-family shapes: list-typed source; Fold accumulator first-order non-list shared with init/body/type, element parameter = source element; MapList/FilterList/ListLength result typing (ch. 4.8) | gate 3 | `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-aanspraaktelling` | `wf070-list-accumulator` + `wf070-scalar-source` |
| WF-071 | StructType declaration: ≥ 1 field, ci-unique names, admissible field datatypes (Number, day-precision Date, declared SumType, declared StructType), acyclic field graph shared with WF-060 (ch. 2.4). Since WF-087 (2026-09-01) an all-nullary keuzetype normalises to `Enumeration`, so it is no longer an admissible field datatype: the struct carrier demands the payload form (`frontends/catala/test/refuse_struct_nullary_enum.catala_en`) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf071-duplicate-field` + `wf071-text-field` + `wf071-recursive-struct` |
| WF-072 | StructType use site agrees with its declaring domain; two struct-typed comparison operands name the same domain (the nominal rule, ch. 2.4) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf072-use-site-drift` + `wf072-cross-struct-equality` |
| WF-073 | StructLiteral: declared struct type; exactly the declared fields, each once; each value type matches the field declaration (ch. 4.9) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf073-missing-field` + `wf073-alien-field` + `wf073-field-type-mismatch` |
| WF-074 | FieldAccess: struct-typed base; field a declared field; node type = the field datatype (ch. 4.9) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf074-unknown-field` + `wf074-nonstruct-base` |
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
| WF-088 | an `Enumeration` use site repeats the value list of the domain it names — same values (ci), declaration order (ch. 2.4). The enumeration sibling of WF-061, and what makes "the declaration is the source of truth" mechanically true for this variant: WF-039 reads the LOCAL list where WF-063 reads the DECLARATION, so a drifted copy passed both while ref faulted at evaluation and clingo derived nothing | gate 3 (model layer: attribute, parameter and `FunctionDef` datatypes; expression layer: every node's `type`, beside WF-007's number-form check — the same "every datatype in the document" split). One position stays deliberately outside the walk: a rule's TARGET `type`, which is WF-022's lvalue, not part of the expression walk — a drifted copy there is verifier-clean and measured harmless (drift the `doel` type of `jutterswet-keurstaat` and all three cases still replay to their pinned expectations in ref AND clingo, because both write through the target attribute's DECLARED datatype) | `jutterswet-vaarwatertoeslag-catala` (the `Vaarwater` domain repeated identically at FOUR use sites: the attribute datatype, the `Match` scrutinee's type, and the two operands of the equality — an `AttribuutRef` type and a `Literal` type) + `jutterswet-vormenkabinet` (the `kwaliteit` domain) | `wf088-use-site-value-drift` (a value the declaration lacks) + `wf088-use-site-value-order` (the same values, reordered — WF-006 order is normative) + `wf088-literal-type-drift` (an impeccable model layer, a drifted `Literal` type deep in a rule: the expression-layer arm) + `wf088-functiondef-drift` (three violations on one `FunctionDef` — parameter, result and body type — all reached through the walk's recursion into sum-type payloads, the arm no other case exercises) |

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
