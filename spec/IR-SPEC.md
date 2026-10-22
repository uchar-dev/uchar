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
  the wire format (`regelspraak-ir-v0` until M1, `uchar-ir-v1` since): the schema admits it as an optional field or node
  (never `required`, never null — written whole or not at all), the
  loaders accept it, and the pinned v0 emitter does not produce it until
  a pin bump lands it there. Every
  extension field is marked as such at its definition and tracked in the
  `CONSTRUCTS.md` delta column like any deviation; hand-written anchor
  cases (`corpus/golden/anchor/`) are its golden witnesses until the
  emitter follows via a pin bump. The format bump to `uchar-ir-v1`
  (decision V1 (b)) carries the non-additive cleanup — renames, and dropping the v0
  fields the extensions supersede — in the steps M1–M8 of
  `spec/proposals/V1-WIREMIGRATIE.md` §7; M1, the container, landed on
  2026-09-23 (landing L1), and M2 + M3, the reference layer, on 2026-09-24
  (landing L2: the written navigation names, the bean fields of a fact
  creation and a scope, the reference's own `type` and the unknown-type
  node left the wire, and the bindings the extension fields carried became
  required on the allocation, the quantifier fold, the union selection and
  the single-argument fold with an `elementType` under every kind but
  `TOTAAL`; the object creation's back-link binding stays optional,
  ch. 3.3). The delta labels B1–B28 cited in this document are the entries
  of that proposal's §3, and the decisions V1–V17 its §5–§6; a landing
  decision that no delta states is recorded in the landing's card, for L2
  `backlog/done/20260923-l2-referentielaag.md`. Current
  extension fields: `connectingRole` (ch. 3.2; and on object creation,
  ch. 3.3 — the back-link role), `sourceRole` and `sourcePath` (ch. 4.3 —
  the fold source's range, one relation or a path of them; the quantifier
  fold of ch. 4.5 carries the same two fields), the symbolic fact-creation
  fields (ch. 3.4), `path` on the attribute reference and `navigation` on
  the characteristic check (ch. 4.1a — the navigation hops), `scope` on the
  scoped uniqueness (ch. 3.6, WF-054), `statuteAnnotations` on the rule
  and on the default (ch. 3.1 and 6.1, WF-082 — the statute annotations),
  `vocabularies` on the
  document with `designations` on the designatable declarations and
  `valueDesignations` on an enumeration's declaration (ch. 2.12, WF-116 to
  WF-120 — the vocabulary side table, which the RegelSpraak frontend
  produces from the dialect's annotations); `origin` on the same
  declarations (ch. 2.13, WF-121 — the origin of a declaration a producer
  made itself, which the RegelSpraak frontend writes on the declarations it
  generates); until L4 the wire spelled
  them `verbindendeRol`, `bronRol`, `bronPad`, `scopeRol` and
  `bronAnnotaties`, and until L5 the navigation hops `navigatieBindingen`
  and `navigatieBinding` and the quantifier fold's range `bronRol` and
  `bronPad` — the names the pin-bump history below uses; current extension nodes and
  forms: `TemporalRead` (ch. 4.3a), the `prioritizedDefaults` list
  (ch. 6.1), the sum-type family — the `SumType` datatype
  (ch. 2.4) with the `Construct`/`Match`/`BinderRef` expression nodes
  (ch. 4.7) — and the fold family — the `ListType` datatype (ch. 2.4),
  the `functionDefs` list under `model` (ch. 2.11; top-level until M1) and the
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
  dropped, the field omitted when none remain);
  `jutterswet-hardheidsoverschot` is the emitter golden carrying one- and
  two-annotation rules — and an unannotated rule without a trace — through
  both executors (added as `jutterswet-hardheid` and renamed at its landing
  the same day: the bare name is the hardship anchor's, which carries no
  annotations).
  The remaining extensions stay anchor-only: `TemporalRead` and
  prioritized defaults have no surface syntax in the toolchain yet.

## Conventions (binding)

- Normative keywords per RFC 2119 (MUST/SHOULD/MAY).
- Every MUST carries an ID `WF-###` and comes with: a check in
  `spec/verifier/`, at least one passing case in `corpus/golden/`, and one
  deliberately failing case in `corpus/negative/`. Where those artifacts do
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
- Enumeration values on the wire are English identifiers in lowerCamel
  case: one word in lower case (`sum`, `year`), several words joined with
  each further word capitalised (`nonNegative`, `equalShares`,
  `filledCount`); no upper-case start, no hyphen, no space, no Dutch. The
  case format spells its kinds the same way (`attributeValue`,
  `characteristicPresent`). Each family's table lists its v0 values. (The
  wire has carried this spelling since landing L5, step M8; until then it
  kept the v0 values.)
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

Layer marks: every node in this chapter is `layer: core`, with exactly three
inline exceptions — the `Characteristic.kind` presentation tag (ch. 2.2),
the `Dimension.preposition` read-form tag (ch. 2.7) and the values of
`Origin.form` (ch. 2.13), all three `layer: dialect(regelspraak)`.

A **document** is the root container (decision V3 (b); landed at M1,
2026-09-23). Its structure:

| Field | Type | Meaning |
|---|---|---|
| `format` | string | the format name, `uchar-ir-v1` |
| `vocabularies` | list of Vocabulary? | extension list: the named vocabularies of the designations (ch. 2.12); optional, written whole or not at all |
| `model` | Model | the declaration set (below) |
| `rules` | Rules | the rule forms of ch. 3 (below) |
| `diagnostics` | list of Diagnostic | the producer's own account of what stayed unresolved (WF-113) |

A **model** is the declaration set. Its structure:

| Field | Type | Meaning |
|---|---|---|
| `objectTypes` | list of ObjectType | declaration order preserved |
| `factTypes` | list of FactType | — |
| `parameters` | list of Parameter | — |
| `domains` | list of Domain | — |
| `unitSystems` | list of UnitSystem | — |
| `dimensions` | list of Dimension | — |
| `timelines` | list of Timeline | — |
| `dayKinds` | list of DayKind | each carries its name and its plural (ch. 2.9) |
| `functionDefs` | list of FunctionDef? | extension list: optional, written whole or not at all (ch. 2.11) |

A list entry carries its name on its node; there is no map key, so each
name is written once (landed at M1; until then six of these lists were maps
keyed by the declared name — WF-094 in the WF index).

**Rules** carries the rule forms of ch. 3, one list per form kind:

| Field | Type | Meaning |
|---|---|---|
| `rules` | list of Rule | ch. 3.1 |
| `dayKindDefinitions` | list of DayKindDefinition | ch. 3.7 |
| `objectCreations` | list of ObjectCreation | ch. 3.3 |
| `allocations` | list of Allocation | ch. 3.2 |
| `consistencies` | list of Consistency | ch. 3.5 |
| `uniquenesses` | list of Uniqueness | ch. 3.6; the single-source and the union uniquenesses share this one list |
| `factCreations` | list of FactCreation | ch. 3.4 |
| `startPointDeterminations` | list of StartPointDetermination | ch. 3.8 |
| `ruleGroups` | list of RuleGroup | ch. 3.9 |
| `prioritizedDefaults` | list of PrioritizedDefaults? | extension list: optional, written whole or not at all (ch. 6.1) |

A wire pointer into a named form reads `/rules/rules/<form>/versions/<version>/…`,
and so on per list. *History:* the lists kept their v0 keys until landing L5
(M8, the renaming) — `resolvedRegels`, `resolvedDagsoorten`,
`objectCreaties`, `verdelingen`, `consistenties`, `uniciteiten`,
`feitCreaties`, `resolvedStartpunten` and `regelGroepen`, in the order of the
table; `prioritizedDefaults` kept its name. Landing L4 (M5, the rule layer)
put the six named forms in those lists into their v1 shape (ch. 3,
Versioning) and the single-source and union uniquenesses into the one list
`uniciteiten`, where the v0 list `uniciteitUnies` left the wire.

**Diagnostic** — one entry of `diagnostics`:

| Field | Type | Meaning |
|---|---|---|
| `severity` | `warning` | the one wire severity (WF-113 (a)) |
| `message` | string | neither empty nor blank (WF-113 (a)) |
| `line` | integer ≥ 0 | the source line the producer reports; 0 = the producer reports no source position (a hand-built document, or a frontend warning without a token) (WF-113 (a)) |

v0 values: `WARNING` → `warning`; and the two values the verifier refuses
by name (WF-113 (a)), `ERROR` → `error` and `INFO` → `info`.

v0 name: `RegelSpraakModel`. **Delta (tracked, `CONSTRUCTS.md` ch. 2.4):** the
v0 container also carries resolved-rule lists and backend helper methods; the
helper methods are not part of the IR, and the rule lists sit under `rules`,
beside the declaration set, which is what the v1 model is. `diagnostics`
stays: WF-113 (decision V2 (ii), 2026-09-19). **Delta:** v0 keys its
registries by exact declared string; v1 makes the name-identity rules
explicit (WF-001…WF-003).

- **WF-113** `diagnostics` is the producer's own account of what stayed
  unresolved in a document it did write. (a) Every entry carries
  `severity: warning`, a `message` that is neither empty nor blank, and an
  integer `line` ≥ 0, where 0 means the producer reports no source position
  (a hand-built document, or a frontend warning without a token — the
  RegelSpraak frontend writes 0 for a circular-reference warning, for one);
  `error` and `info` are not wire severities — an
  error means the producer refused to write the document, and info is
  tooling talk (measured 2026-09-23: zero carriers in 769 documents) — so
  the schema enum shrinks to the one value and the verifier refuses the
  other two by name, as it refuses an entry whose severity is null. (b)
  Every `Unresolved` node MUST be accounted for: the document carries at
  least one diagnostic whose `message` contains that node's
  `source.snippet` verbatim (the 2026-09-19 decision "every surviving
  marker carries a WARNING", made normative and checkable). A marker
  without a `source`, or whose snippet is blank, cannot be accounted for
  and is refused: blank counts as absent, as for a named form's name
  (WF-107), because the containment is literal — nearly every message
  contains a blank snippet, and a message about another marker would
  account for it. The schema requires `source` on an `Unresolved` and every
  executor refuses a marker without one at load; the verifier loads it, so
  that this clause names it. An EMPTY snippet cannot be written at all: the
  schema requires at least one character, and every reader, the verifier
  included, refuses an empty snippet at load under no WF number ("a source
  snippet is never empty: a node without source text carries no source",
  ch. 4). Until L5 the marker was the v0 node `Onopgelost` and this clause
  read its `bron` string, which could be null or empty. (c) No consumer
  reads `diagnostics` for evaluation: the list carries no semantics, exactly
  as the ch. 3 paragraph says of the former `regels` stubs — stated, not
  fenced, because a rule on consumers has no document to reject.

- **WF-019** Declaration order is significant and MUST be preserved by
  serialization; consumers MUST NOT reorder (label sets and unit resolution
  are order-sensitive in the source language). Enforced by gate 2
  (`scripts/check-ir-schema.sh`, byte-identical round-trip); a corpus case
  with genuinely reorderable content is still **OPEN**.

### 2.1 Names and identity

Declared names are carried verbatim (whitespace-collapsed, case
preserved). Name *matching* is case-insensitive: the source language
declares names not to be case-distinguishing (§13.2.30). The same match
holds where a rule compares two datatypes whole (WF-049, WF-061, WF-062,
WF-064, WF-066 to WF-070, WF-072 to WF-074, WF-076): every name the
datatype carries — its domain, an enumeration value, a constructor, a
field — is matched case-insensitively, and a unit is compared byte for
byte (WF-091). *(Since 2026-09-25; until then those rules compared a
datatype byte for byte, and a case-only drift in a domain name was
refused.)*

*(History since M1, 2026-09-23: both comparison sites this note names —
WF-094 and ch. 4.6 (p) — left the wire together with the map keys and the side
tables they compared. The reading of "verbatim" it fixes stands.)*
*One sharpening, stated once here rather than twice below.* Where a rule
invoked this **carriage** sentence as a COMPARISON — WF-094 below, and ch. 4.6
(p) at an object type's side-table keys — it compared **byte for byte,
whitespace included**, which is stricter than the collapse this sentence
describes. The two were not in conflict: the collapse is what a PRODUCER does
to a declared name **once**, when it takes the name off the source text; those
two rules asked whether a name already collapsed was then **copied** unchanged
into a second position. The gap between the two readings was measured empty and
no producer here could write into it — over the **1937** keyed declarations the
four zones held (recounted 2026-09-10: **2016**, the same reading in every
respect), not one self-name differed from its key in whitespace alone
(the five that differed at all were WF-094's witnesses, each a one-character
recapitalisation), while **603** of those keys did carry internal whitespace,
so the flank was reached and not merely unvisited; and every emitter in this
repo wrote the map key and the `naam` field from ONE string. So "verbatim"
meant the byte at both sites, and the collapse stays what it always was: a
normalisation performed before the name enters the document, never a licence
to re-normalise inside it.

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
  v1; the unknown-type datatype does not exist in v1 (ch. 2.4). **Landed L2
  (2026-09-24, B16):** the unknown-type node left the datatype union, so
  an unfindable domain is spelled as what it is — a declaration naming a
  domain no declaration carries: an attribute (or domain) whose enumeration
  names an undeclared domain is refused here, a parameter doing the same by
  WF-018 (ch. 2.10). The migration refuses the unknown-type node at a
  declaration position; the two witnesses were rewritten by hand
  (`wf003-unresolved-domain`, `wf018-parameter-unknown`).
- **WF-094** *(removed at M1, 2026-09-23 — history.)* Landed M1 2026-09-23:
  the map key is gone, the node carries its one name. Until then the v0 wire
  stored five declaration kinds — `objecttypen`, `parameters`,
  `eenheidstelsels`, `feittypen`, `dimensies` — as maps whose values repeated
  the key in a `naam` field, and this rule required that self-name to spell its
  key byte for byte (maintainer decision 2026-09-06), with a clause (d) against
  a `null` self-name (maintainer decision 2026-09-09). The harm it closed was
  measured on the bean-model executor, which loaded the self-name as the
  declaration's name: a one-character recapitalisation of a parameter's
  self-name made it answer with wrong values (`jutterswet-aanvoergeld`: 322.50
  became 310.00) while the verifier, `ref` and the ASP backend passed, and a
  `null` parameter self-name stopped it with a `NullPointerException` in the
  identifier contract. The measurements, the reader counts and the ten
  witnesses are recorded in `backlog/done/20260905-zelfnaam-tegen-mapsleutel.md`
  and `backlog/done/20260906-zelfnaam-null-flank.md`; the witnesses were removed
  with reason at M1 (`corpus/negative/README.md`, "Vervallen bij de
  v1-migratie"). *Clause (d)'s null form:* since M1 it cannot be written — on the
  four nodes that kept their v0 shape at M1 (`Parameter`, `Eenheidstelsel`,
  `Feittype`, `Dimensie`) the schema typed `naam` as a non-null string, because
  with no map key beside it that field is the declaration's only name, and an
  ObjectType's `name` is a required string. Since L3 (2026-09-24) the first two
  are `Parameter` and `UnitSystem` with a required `name` (ch. 2.10, ch. 2.5),
  and since L5 the other two are `FactType` and `Dimension`, whose `name` is
  the same non-null string (ch. 2.6, ch. 2.7). *What the
  rule's scope note recorded, and how it was settled:* in a unit system's v0
  `eenheden` map, 59 of 654 entries (recounted 2026-09-10) were keyed `€`
  against the stored abbreviation `EUR`, a question of unit identity next to
  WF-009 and ch. 4.6 (r). Since L3 the map is a list and a key that differs
  from the abbreviation became the first spelling of its unit, so `€` is a
  spelling of `EUR` (ch. 2.5); what a spelling means to a reader is the OPEN
  point of ch. 2.5.

### 2.2 ObjectType

An entity type: named, with attributes and characteristics.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `supertype` | string? | the single direct parent, by declared name; absent = a root type |
| `animate` | boolean | v0 `bezield`; grammatical animacy, drives surface forms |
| `plural` | string? | declared plural, if any |
| `attributes` | list of Attribute | declaration order |
| `characteristics` | list of Characteristic | declaration order |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the type's names in the vocabularies (ch. 2.12) |

**The supertype relation.** Wire key `supertype`, an **extension field**
under the § Method discipline: optional, never `required`, never null,
written whole or not at all. Write `⊑` for the reflexive-transitive closure
of it. Three things follow, and each is a definition rather than a rule:

- **Member resolution walks the chain UPWARD ONLY.** A name is looked up on
  the type, then on its parent, and so on to the root; the first
  declaration found answers. The **effective member set** of a type T is
  the union of the attributes (respectively characteristics) declared on
  every S with T ⊑ S. A member declared on a strict *sub*type and read at
  the supertype does **not** resolve — not every instance of the supertype
  has it — and that read is refused by the chain-aware **WF-034** cross,
  which is the one new reachable refusal this relation creates in an
  existing rule.
- **Quantification is extensional at every type-ranging position**, with no
  exact-type quantifier: the instance fold's `elementType` (ch. 4.3), a
  rule's subject type, a defaults group's `subject` (ch. 6.1), a
  characteristic check's owner type (ch. 4.5), the uniqueness scope
  (ch. 3.6), the consistency rule's subject and an allocation's `targetType`
  (ch. 3.2) all range over
  `{ i : typeOf(i) ⊑ T }`. A subtype with no own members is therefore not
  inert but a pure classification, which is the shape the relation exists
  for.

  The seven split by HOW their set is obtained, and the split decides
  what a witness has to show. **Four name a population the executor
  SCANS**: the rule subject, the defaults subject, the consistency
  subject, and the population a uniqueness ranges over before its scope
  hop groups it. There the quantifier IS the scan, so its two directions
  cannot come apart — an instance is enumerated because its type
  satisfies `⊑ T`, and one that does not is never enumerated to begin
  with. **One is not a set at all**: a characteristic check's owner is a
  hosting statement about the one subject the check is evaluated on, so
  `⊑` is read there as a predicate and there is nothing to exclude FROM.
  **Two name a type against a set a RELATION delivers** — the instance
  fold's `elementType` and an allocation's `targetType` — and there the
  clause is two separate
  claims, because the relation and the declared type are two different
  declarations: the relation supplies the REACH, the declared type
  supplies the POPULATION, and the quantifier is their intersection. The
  inclusion half says an instance the relation reaches counts when its
  type is a strict SUBTYPE of the declared one; the exclusion half says
  an instance the relation reaches does NOT count when its type is not
  `⊑` the declared one at all. The two halves coincided until ch. 2.6
  widened **WF-047**'s "agree" from equality to ⊑-compatibility; only
  since then can a bound role's filler be a strict supertype of the type
  its position declares, and only since then is the second claim
  reachable.

  All seven carry a witness rather than a mention. The rule subject and
  the defaults subject replay in `anchor/jutterswet-strandrechtketen`
  (gates 5, 6 and 12); the characteristic check's owner, the uniqueness
  scope and the consistency subject — which no golden reaches — are
  worked examples WV-IR2.2-a/b/c in `ref/test/`. The two relation-fed
  positions carry both halves: the allocation target replays in
  `golden/jutterswet-hoofdberging` (and, from RegelSpraak source, in its
  twin `golden/jutterswet-bergersdeling`), where the connecting role's
  filler is a strict supertype of `targetType` and the recipients outside
  that type receive nothing; the instance fold's `elementType` is
  witnessed for inclusion by the two aggregations of that same anchor,
  and for exclusion by `golden/jutterswet-munttelling`, where both folds
  declare an `elementType` strictly BELOW what the source role fills and
  a plain supertype instance linked beside the subtypes is reached and
  not folded.

  *Recorded, because the gap was real and the executors did not all share
  it.* Until 2026-09-16 `ref` and `clingo` folded the reached set whole
  and counted that plain instance in, while the Drools backend did not —
  its exec-model branch matches the element type as a class pattern over
  beans wired with native inheritance, so the subtype pattern never saw
  the supertype fact. That was a three-way divergence on a
  verifier-clean document, with this clause and Drools on one side; the
  two interpreters were corrected to it, not the other way round. The
  same shape was measured and closed one construct over, on the
  allocation's recipients, on 2026-09-12.
- **An inherited member carries its facets** — its dimensions and its
  timeline granularity/count are read on the chain like the declaration
  itself, so a subtype instance of a type whose ancestor declares a
  time-dependent attribute or characteristic holds a timeline, not a
  scalar. Witness `golden/jutterswet-erfwacht` (an inherited monthly
  attribute folded over an inherited monthly characteristic, replayed by
  gates 5, 6 and 12). The Drools backend refused this whole combination
  until 2026-09-12 because its facet side-maps were read per declared
  type; what it refuses now is **narrower and named**: an **inherited
  attribute riding a named timeline that has a variable start point**
  (ch. 2.8, ch. 9.9), or a **start-point determination declared on an
  ancestor**. That family is not a member lookup but a protocol keyed on
  the DECLARED SUBJECT NAME — the anchor is a synthetic attribute
  declared on a named object type — so resolving one of its four sites
  through the chain only moves the seam; it is widened whole or not at
  all, and until then the loader refuses those two shapes loudly rather
  than lowering an unanchored timeline in silence. The **variable** start
  point is the whole of the condition: on a fixed-start named timeline
  the predicate answers the same from the subtype as from the declaring
  ancestor, so there is nothing to be blind about and the shape loads.
- **A role's declared filler type is an UPPER bound** (ch. 2.6): a role
  declaring filler T accepts an instance of any S ⊑ T. **WF-047**'s "agree"
  is consequently ⊑-compatibility and not equality, and **WF-048**'s
  uniqueness was computed over the chain-closed type-pair set (until L2,
  when WF-048 left with the bindingless forms) — so adding a
  subtype could turn a previously well-formed bindingless navigation into a
  loud refusal, because a pair that was uniquely connected may become
  doubly connected once the chain closes. That was accepted, and the answer
  is an explicit role binding.

A **case instance names its CONCRETE (most specific) type** in the existing
`objectType` key; there is no second discriminator field, and the member set
the loader accepts is the chain-closed one. **Object creation (ch. 3.3)
creates an instance of exactly the type it names — creation is not
polymorphic.** Recorded as an explicit non-refusal so no rule is written
for it. A **sibling collision** — two incomparable subtypes of one
supertype each declaring the same member name — is likewise **admitted**
and is not a rule: under single inheritance no instance is of two
incomparable types, so no read is ambiguous; a read of that name anchored
at the common supertype simply does not resolve, and WF-034 says so.

- **WF-103** A present `supertype` MUST name a declared object type. This
  is WF-003's territory by kind, and it takes a rule of its own because the
  failure mode is specific — a chain that ends at an undeclared name
  resolves no member through it — and a witness has to be able to cite it.
- **WF-104** The supertype graph MUST be acyclic, self-reference included
  (self-reference is the depth-1 cycle). A cycle has no root for resolution
  to stop at, and every reader would either walk it forever or cut it off
  at a private depth of its own.
- **WF-105** No attribute or characteristic name is declared twice along
  one chain (ci) — WF-002's per-kind uniqueness lifted from the declaration
  to the chain. One instance would otherwise carry two declarations of one
  name, and which one a reader finds decides that name's datatype,
  dimensions and timeline facet on every read; the measured consumer
  consequence is a bean generator that deduplicates silently, first-wins,
  and produces a class that compiles and is quietly missing one member.
- **WF-106** An object type declares **at most one direct supertype**.
  **This rule carries no verifier check and no `corpus/negative` case, and
  that is a deliberate, argued departure from the spec discipline rather
  than an omission.** Single parenthood is true by SHAPE: the field is one
  string, so a second direct parent is not a well-formed-but-wrong document
  the verifier rejects, it is a document that cannot be written — the
  closed `ObjectType` node refuses the array form and gate 2 is where that
  refusal is mechanically enforced. A negative case needs a well-formed
  wire document to put in it, and under `supertype: string?` that document
  does not exist by construction; inventing a representable-but-forbidden
  wire shape purely to satisfy the letter of the discipline was the
  alternative, and it was rejected, because it would buy a witness at the
  price of a wire form that can express exactly what the design forbids.
  What the number buys instead is ONE citable name for ONE refusal across
  three layers, so that the schema's array rejection and the two frontends'
  messages are visibly the same refusal rather than three unrelated ones.
  The rule bounds the language **above** the wire, so its witnesses live
  where that language is read: the fixtures of the RegelSpraak dialect
  frontend (`is een A en B`, two `is een` clauses) and of the F-logic
  frontend (two `::` facts, which really can write two parents). A rule
  whose violation is unrepresentable in the checked artefact is the case
  the discipline does not cover; naming it and placing its witnesses is the
  honest answer.

v0 name: `Objecttype`. **Delta (tracked, `CONSTRUCTS.md` row 11):** v0
stores attribute facets (plural, dimensions, timeline granularity/count/
name) and characteristic facets in parallel side-maps keyed by member name;
v1 reifies **Attribute** and **Characteristic** as nodes carrying their own
facets (their "v0 name" is therefore not a node but the side-map facets on
`Objecttype`; the same holds for **TimelineFacet**). *Migration (landed M1,
2026-09-23):* mechanical — `spec/proposals/v1-migratie/m1.py` folds each v0 map
entry into the corresponding member node, and absent map entries become the
documented defaults below. The enum VALUES inside the new nodes kept their v0
spelling until M8 (landing L5), which wrote the v1 values the tables below
give; each table lists its v0 values. Until
that reification the side-maps were fenced where the node shape fences them by
construction: WF-034 clause (p) required every key of all eight maps to name a
declared member, in that member's declared spelling, because an orphan key hung
a facet on a member that did not exist and left the intended member at its
default. The clause left the wire with the side-maps (ch. 4.6 (p), WF index).

**Attribute**

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `datatype` | Datatype | inline or resolved from a domain reference |
| `plural` | string? | declared plural; absent = none declared |
| `dimensions` | list of dimension names | absent = scalar; when present non-empty (WF-004); order as declared |
| `timeline` | TimelineFacet? | absent = not time-dependent |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the attribute's names in the vocabularies (ch. 2.12) |

**Characteristic** — a boolean trait; it has no datatype.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `kind` | `adjectival` \| `possessive` \| `nominal` | presentation tag (below) |
| `timeline` | TimelineFacet? | absent = not time-dependent |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the characteristic's names in the vocabularies (ch. 2.12) |

v0 values: `BIJVOEGLIJK` → `adjectival`, `BEZITTELIJK` → `possessive`,
`ZELFSTANDIG` → `nominal`.

`kind` (v0 `KenmerkSoort`; untagged defaults to `nominal`) is a
**presentation tag**: all three kinds denote the
same boolean membership; the tag only selects the predicate surface form
(§3.5, §8.1.8). This is the same pattern as the assignment presentation tag
(PLAN §9, decision 4). Layer: the tag is `dialect(regelspraak)`; the
boolean membership is core.

**TimelineFacet** — the time-dependence of one member or parameter:

| Field | Type | Meaning |
|---|---|---|
| `granularity` | Granularity | `day` \| `week` \| `month` \| `quarter` \| `year` |
| `count` | integer ≥ 1 | periods per segment; 1 = single-period |
| `fixedStart` | string? | the point the periods run from, as on a `Timeline` (ch. 2.8, WF-115); on a facet that names a timeline, that timeline's |
| `timelineName` | string? | present iff declared via a named timeline |

v0 values: `DAG` → `day`, `WEEK` → `week`, `MAAND` → `month`, `KWARTAAL` →
`quarter`, `JAAR` → `year` — one granularity family, the same five values on
a `Timeline` (ch. 2.8) and on a `BuiltinCall` (ch. 4.3).

A characteristic's facet carries no `timelineName`: v0 recorded none, and M1
keeps the position closed.

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
  wire admits `null` as an Attribute's `datatype`, but a datatype-less
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

  **Arms 2 and 3 — removed at M1 (2026-09-23), history.** They required
  every `kenmerkGranulariteit` entry (arm 2, maintainer decision
  2026-09-09) and every `attribuutGranulariteit` entry (arm 3, 2026-09-10)
  to carry a granularity. A v0 side-table entry declared its member
  time-dependent by its KEY and named the grid in its VALUE, and the readers
  split on that seam: with one value nulled, `jutterswet-tegenwacht`
  answered 60 and 120 where the golden pins 30 and 30, wrong identically in
  both executors (arm 2), and on the attribute side every path died with a
  diagnosis naming another defect (arm 3). Since M1 a member's timeline is a
  TimelineFacet node on the member, and its `granularity` is a required,
  non-null enum, so a timeline without a grid cannot be written: the schema
  refuses it and so does the shared loader. The measurements are in
  `backlog/done/20260906-gedurende-kwalificatie-stiltes.md` and
  `backlog/done/20260909-attribuutgranulariteit-null.md`; the two witnesses
  were removed with reason (`corpus/negative/README.md`, "Vervallen bij de
  v1-migratie").

- **WF-004** A dimensioned attribute's `dimensions` list MUST be non-empty,
  duplicate-free, and every entry MUST resolve to a declared Dimension
  (WF-003). *v0 mirror note:* stored without any check. **Delta
  (promotion).**
- **WF-005** A `TimelineFacet` MUST have `count` ≥ 1 and, when
  `timelineName` is present, the named Timeline MUST exist and the facet's
  granularity and count MUST equal the declaration's (adoption is total,
  not partial). On a parameter's facet (ch. 2.10) the count MUST be 1 while
  multi-period grids are OPEN (`RUNTIME-SEMANTICS.md` ch. 3.12). *v0 mirror
  note:* v0 adopts from the first matching declaration and leaves unresolved
  references silently untagged. **Delta (promotion).**

### 2.3 Domain

A named, reusable datatype.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `datatype` | Datatype | an Enumeration for a value-list domain, a SumType for a variant domain (extension, ch. 2.4), else a base datatype |

At this declaration position the datatype carries no domain name of its own:
an `Enumeration`, `SumType` or `StructType` written as `Domain.datatype` omits
its `domain` field, because the wrapper's `name` is the domain's one spelling
(landed M1, delta B2). At every reference position — an attribute's or
parameter's datatype, a payload, a field, a list element, an expression's
`type` — the field stays and is the domain reference of ch. 2.4 (WF-003).
At the declaration position alone an `Enumeration` may carry
`valueDesignations`, the designations of its values (ch. 2.12, extension
field): a value is a bare string, so its names in the vocabularies stand
beside the list that declares it.

v0 name: `domeinen` registry entry.

- **WF-006** An enumeration domain MUST declare at least one value, and its
  values MUST be unique. Value order is normative and preserved. *v0 mirror
  note:* unchecked in v0. **Delta (promotion).**

### 2.4 Datatype

A closed sum. v1 variants:

| Variant | Fields | Notes |
|---|---|---|
| `Number` | `whole: boolean`, `decimals: int`, `sign`, `unit: Unit?`, `presentation: "percentage"?` | see precision forms below; a percentage is a `Number` whose unit is `%`, and `presentation` is its tag, at a declaration only (the deltas below, WF-007) |
| `Text` | — | equality only; not orderable |
| `Boolean` | — | equality only |
| `Date` | `precision: day \| millisecond` | orderable; `precision` is deliberately not called granularity — that term is reserved for the five-value timeline set |
| `Enumeration` | `domain: string`, `values: list of string` | `domain` is a resolvable reference to a declared Domain (WF-003), not a free string — this satisfies the "real reference, not a name string" delta of `CONSTRUCTS.md` ch. 2.4 |
| `SumType` | `domain: string`, `constructors: list of {name, payload: Datatype?}` | **extension node** (Method; wire node `SumType`, never emitter-produced): the payload-carrying variant family, CONSTRUCTS row 36 — see the SumType clause below |
| `ListType` | `element: Datatype` | **extension node** (Method; wire node `ListType`, never emitter-produced): an ordered, possibly empty sequence type, CONSTRUCTS row 10 — see the ListType clause below |
| `StructType` | `domain: string`, `fields: list of {name, datatype: Datatype}` | **extension node** (Method; wire node `StructType`, never emitter-produced): the record datatype, Phase-5c core (`spec/proposals/PHASE5C-TUTORIAL.md` §2) — see the StructType clause below |

`sign` ∈ {`unspecified`, `negative`, `nonNegative`, `positive`} (v0
`Teken`), always written: since L3 (2026-09-24) a number declared without a
sign writes the unspecified sign (`ONBEPAALD` until L5), where the v0 dump
wrote `null` (the same reading, one spelling).
v0 values: `ONBEPAALD` → `unspecified`, `NEGATIEF` → `negative`,
`NIET_NEGATIEF` → `nonNegative`, `POSITIEF` → `positive` — the wire spelling
until L5.
A `Number` without `unit` carries no unit; `unit` is never `null`.

**Precision forms** (v0 encoding mirrored, made explicit): a `Number` is
exactly one of — (a) *whole*: `whole = true`, `decimals = 0`; (b) *fixed
decimals*: `whole = false`, `decimals ≥ 1`; (c) *unbounded*: `whole =
false`, `decimals = 0` (no precision ceiling; §13.3.3 form (c)). The
`whole` flag is the sole discriminator between (a) and (c).

- **WF-007** A `Number` MUST satisfy one of the three precision forms;
  `decimals` < 0 is ill-formed. A source declaration of "0 decimals"
  normalizes to the whole form (v0 behaviour, kept and specified). The
  form is a property of the datatype ITSELF, not a cross against a
  declaration, so it holds at **every** `Number` in the document: the
  declarations, and every `type` a value node carries of its own (since L2,
  2026-09-24, a reference carries none, B13). *(History until L2:)* from
  2026-09-04 it held at a rule TARGET's own `type` as well, which the
  expression walk deliberately never enters (the lvalue is WF-022's); that
  annotation left the wire with L2, and the rule-target arm with it (its
  witness `wf007-doeltype-geheel-met-decimalen` was removed with reason,
  `corpus/negative/README.md`, L2). Measured
  2026-09-03: `whole` with 2 decimals was verifier-clean there while the
  identical form on any walked expression type was refused. Where the
  target's attribute declaration resolves, the identity family had been
  masking the shape incidentally since WF-091 — a facet mismatch under
  WF-091's number, in a comparison message that says nothing about the
  precision form; on a CHARACTERISTIC target it defers by construction
  (no attribute declaration to compare with), and there — 54 such targets
  over 34 golden+red documents, the anchor subset included — this check
  is the only guard OF THE NUMBER FORM: a `Text`-typed copy, a drifted
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
  *The presentation arm (since L3, 2026-09-24).* The tag
  `presentation: "percentage"` MUST NOT stand inside a type annotation —
  the `type` a value node carries (a derived position, a `Literal`, a
  `Let`, a `Fold`, every node that writes one) and every datatype nested in
  it. It belongs to a DECLARED datatype: the datatype of an attribute, a
  parameter or a domain and every datatype nested in one (a sum-type
  payload, a list element, a struct field), a `FunctionDef` or lambda
  parameter, a `FunctionDef` result. An annotation is a copy of what the
  declarations decide, and a tag there would be a second presentation of
  one number. The schema admits the field on every `Number`, since it
  cannot tell a declaration position from an annotation; the loaders
  validate the value — `"percentage"` is the only one — and refuse the tag
  on a `Number` whose unit is not `%`, and this arm refuses the position.
  Witness `wf007-presentation-tag-by-annotation` (a `Literal.type`); every
  golden that carries a percentage literal or a derived percentage is the
  positive side, the tag standing only at its declarations.
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
  keuzetype. Since L2 (2026-09-24) there is one such use site on the read
  side: an
  **element-passing aggregation** — `first`, `last`, `minimum`,
  `maximum`, the folds that select a contributor instead of computing a
  new value — takes its value from **each of its operands**, and its own
  `type` MUST name that operand's keuzetype; the operand is typed by its
  declaration where it is a reference (`RUNTIME-SEMANTICS.md` ch. 2.10) and
  crossed structurally where it is a composite, so the fold chains to a
  declaration rather than laundering a domain. A composite position's
  annotation is crossed on this facet against its derivation too (ch. 4.1,
  `wf089-composite-domain`). *(History until L2:)* a **reference
  expression** was the second use site — its own serialized `type` against
  the **declaration it reads**. That annotation left the wire with L2
  (B13): a reference's type IS its declaration's, so it cannot name another
  domain, and the reference arm and its witnesses left with it (the
  witnesses whose translation fell on another rule moved there — WF-063,
  WF-023, WF-070, WF-036 — `corpus/negative/README.md`, L2). (`sum`,
  `total` and `count` compute a number; no keuzetype travels through
  them.) The variant
  travels with the name because `domains` is **one namespace** — a domain
  is declared as an enumeration or as a sum type, never both, so an
  `Enumeration`-typed reference over a sum-typed declaration of the same
  name (or the mirror) is a lie whatever the spelling agrees on. This is
  the **domain identity** rule, read half: the domain extension of the
  soort cross WF-023/WF-027 perform (WF-078 too, until L2), and the other half of
  WF-088/WF-061 — those bind a use site to the domain it *names*, this
  binds it to the declaration it *reads*. Its **write half** — every value
  entering such a declaration names the same keuzetype — is WF-023 (rule
  target, object-creation member, defaults consequence) and WF-067 (a
  function body against its declared result); read and write half together
  are one maintainer decision (2026-09-02), and neither is sufficient
  alone. *(History until L2.)* The declaration source per reference shape was: for
  an `AttributeReference` — and the `DimensionCellRead` over one, whose
  cell carries the attribute's datatype — the attribute at the
  reference's endpoint type (ch. 4.1a: a navigated read crosses against
  the attribute it lands on); for a `ParameterReference` the parameter;
  for a `BinderRef` the binder's declared datatype — a match arm's
  declared payload, a lambda or `FunctionDef` parameter's declared type,
  a `Let` value's type, a filter binder's element type (the WF-078
  environment). A **fold source** — the `AttributeReference` an
  aggregation with an `elementType` reads each contributor through
  — is an `AttributeReference` like any other and crosses against the
  attribute it reads; it is called out because its shape is checked by
  the fold-source rules (WF-034/WF-053/WF-080/WF-083) rather than by the
  ordinary expression walk, and that routing, not the rule, once left it
  uncrossed (`corpus/negative/wf089-aggregation-pass-through-enum`). Three reference
  shapes need no arm of their own because
  an existing rule already crosses their full type *structurally* against
  the declaration: a `TemporalRead`'s type equals the timeline's element
  datatype (WF-049), an `Invoke`'s type and argument types equal the
  declared result and parameters (WF-068), a `Lambda` parameter equals
  the source element (WF-070) — and that source is a reference, anchored
  here; a `Construct` reads no declaration (it names its own domain,
  WF-062). An operand whose type nothing declares — an unresolvable
  reference, a count source's role name, an `Unresolved` of unknown type —
  defers, as under WF-023. *Motivation (measured, the card; the reference
  half is history since L2):* without it a reference typed with a second,
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
  `SumType` repeated as a payload or field inside `domains`, is
  verifier-clean when its inline copy drifts (WF-061 walks attribute and
  parameter datatypes only) and is recorded as an open flank rather than
  pinned here: every verifier rule that consumes it resolves the *named*
  domain, never the inline copy. *One neighbouring gap, recorded here and
  since closed.* **Orderability of the
  element-passing folds.** This rule makes the fold's domain honest, not
  its kind admissible: `first`/`last`/`minimum`/`maximum` are
  *ordering* folds (ch. 4.3), and Enumeration and SumType are
  equality-only by the orderability paragraph below — so an entirely
  truthful `first` over an enumeration attribute was verifier-clean and
  faulted in ref (`unordered comparison: Zeegat vs Wad`) as soon as a
  second contributor existed, while a single contributor silently
  succeeded. That is a WF-038 operand question, not a domain-identity
  one, and WF-038's ordering-fold clause (2026-09-05) answers it: every
  operand of an ordering fold comes from one comparable family, numbers
  or dates. The three negatiefs that pin the domain identity at this
  position over an enumeration therefore now carry two independent
  defects, and their falsifiability notes say so; the domain-identity arm
  stays separately falsifiable by the rule each document is *cited*
  under.
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
landing pattern (extension node, hosted by `domains`, never
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
  struct declaration** in `domains` (WF-071 admits declared domains
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
  positions are deliberately outside the walk, each because no negative
  can isolate it: a matched struct's own `velden` (the agreement test is
  an equality on the whole field datatype, so a drifted nested copy
  already makes the enclosing copy disagree — the argument WF-073/074
  make for their nested forms), a sum-type payload (WF-060 admits no
  struct payload in a declaration at all, so such a payload can only sit
  in a drifted inline sum copy — WF-061's recorded open flank, not
  this rule's), and — exactly as WF-088 records for enumerations — a
  rule's TARGET `type`, which is WF-022's lvalue rather than an
  expression node, so the expression walk never enters it. That third
  position is real: five goldens and two red documents carry a
  struct-typed `doel` `type`, and drift there is verifier-clean and
  **measured harmless** (reverse the fields of
  `jutterswet-dossierspelling`'s `/resolvedRegels/1/doel/type` — a
  pointer on the v0 wire of that measurement —, or
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
  clause, over the same positions. **Read half:** an **element-passing
  aggregation** (`first`/`last`/`minimum`/`maximum`) names the struct
  domain of each of its operands, which are typed by their declarations in
  turn, so the fold cannot launder a domain; a composite position's
  annotation is crossed against its derivation. *(History until L2,
  2026-09-24:)* a reference expression — an `AttributeReference`, the
  `DimensionCellRead` over one, a `ParameterReference`, a `BinderRef` and a
  fold source — also named the struct domain of the declaration it reads,
  in its own serialized `type`; that annotation left the wire with L2 (B13),
  and the witnesses moved to the rules their translations fall on (WF-074,
  WF-070 — `corpus/negative/README.md`, L2). **Write half:** every struct value entering such a declaration
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
  datatype, domain included. An operand whose type nothing declares and a
  use site carrying no `type` defer, as under WF-023/WF-089. *Motivation
  (measured, the card):* WF-072 binds a
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
  measured). Two neighbouring gaps were recorded
  rather than claimed shut; one is still open, one is closed. First — still
  open — WF-090's aggregation arm reaches the
  **element-passing** folds only, so a COMPUTING fold (`sum`, `total`)
  over struct operands of two domains is verifier-clean. That is a
  WF-038 operand question — `sum` demands numeric operands — and it is
  no wrong answer: measured, ref faults fail-fast (`plus: expected a
  number, got {waarde: 120.5, telling: 3}`) and the ASP carrier refuses
  the form ahead of translation. Second — **closed 2026-09-05** — the same
  **orderability** flank
  WF-089 records: `first`/`last`/`minimum`/`maximum` are ordering
  folds while `StructType` is equality-only (the orderability
  paragraph above), so an entirely truthful struct `first` with two
  contributors was verifier-clean and failed observably — measured, ref
  `EVALUATION FAULT: unordered comparison: {waarde: 20, telling: 2} vs
  {waarde: 10, telling: 1}` and the ASP carrier refuses the form. A
  WF-038 operand question, not a domain-identity one, and WF-038's
  ordering-fold clause now refuses it: a struct contributor under an
  ordering fold is outside both comparable families. *v0 mirror note:* `StructType` is a v1
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
  decided in that chapter — **OPEN**. **Landed L3 (2026-09-24, B14).** The
  tag is `presentation: "percentage"`, the one value, written only at a
  declaration position and refused on a type annotation (WF-007, the
  presentation arm above); it carries no meaning for evaluation — every
  reader reads a `Number` whose unit is `%` as a percentage, tag or none.
  A unit-less v0 percentage became a `Number` with the built-in `%`,
  `{"abbreviation": "%", "canonical": "%", "factorToCanonical": {"num": 1,
  "den": 1}}` without a `system` (the form the other percentages already
  carried), and its sign is `unspecified` (`ONBEPAALD` until L5). That is
  the one sign a percentage has: the v0 percentage carried none and no
  reading of a signed percentage is specified, so every reader refuses
  another at load ("a percentage carries no sign restriction: a number
  whose unit is '%' has the sign unspecified"), and refuses the tag on a
  `Number` whose unit is not `%`
  ("the presentation "percentage" on a number whose unit is not '%'").
  Measured by the migration: no v0 `Numeriek` in the four zones or the
  parity zone carried the unit `%`, so on every document there the reading
  "a `Number` whose unit is `%` is a percentage" is the v0 distinction
  exactly. At a DERIVED position the reading is a refinement of meaning,
  stated here: the RegelSpraak frontend types a sum or a difference of two
  percentages as a number carrying the left operand's unit — `%`, or a rate
  whose numerator is `%` — and its emitter writes that annotation as a
  `Number` with the unit `%`, which every reader now reads as a percentage.
  WF-091 derives the same (ch. 4.1, the derived-type table: a `%` result is
  a percentage), so the frontend's own emission stays verifier-clean; while
  the derivation still built a number there, the verifier refused it
  (measured 2026-09-24 on the frontend's emission of `A plus B`, `A min B`,
  `A verminderd met B` and `A plus 5%` over two percentage attributes; card
  `20260924-l3-typesysteem`, which pins the form with a golden).
- v0 `DatumInDagen`/`DatumTijd` merge into `Date(precision)`.
  *Migration:* mechanical. Mixed-precision date use remains a rule-layer
  typing error, as in v0. **Landed L3 (2026-09-24, B15).** `Date {precision:
  day | millisecond}`: `DatumInDagen` became `precision: day`, `DatumTijd`
  `precision: millisecond`. `precision` is required — a date without one
  cannot be written — and the two precisions share no order (WF-036,
  WF-038).
- v0 `Onbekend(reden)` is removed: v1 fails fast at verification (WF-003)
  instead of carrying an unknown-type sentinel. Honest inexpressibility at
  the *expression* level remains the job of the `Unresolved` node
  (`CONSTRUCTS.md` row 16), which is a different thing: a well-formed
  marker with a reason, not a type. **Landed L2 (2026-09-24, B16).**
  The unknown-type node left the datatype union: the schema carries no
  such node and every loader refuses it by name as an unknown datatype
  node. An unfindable domain is a declaration naming a domain that no
  declaration carries (WF-003, WF-018); an `Unresolved` expression whose
  type is unknown carries no `type` (ch. 4.4); a derived position the
  producer cannot type carries `type: null` (ch. 4.2). Of the 137 unknown-type
  nodes the four zones and the parity zone carried (measured by the L2
  migration), 130 sat on reference types and left with the reference's own
  `type` (B13), one on a derived position became `type: null` (ch. 4.2;
  recorded in the L2 card, `backlog/done/20260923-l2-referentielaag.md`), and the
  six at declarations and payloads of negative witnesses were rewritten or
  removed with reason (`corpus/negative/README.md`, L2).

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
| `system` | string? | declaring system; absent for the built-in `%` and the bare calendar units |
| `canonical` | string? | the system unit this reduces to, by its `abbreviation`; absent for opaque products and bare units |
| `factorToCanonical` | `{num, den}`? | how many canonical units equal one of this unit: an exact rational, two integers of at least 1 and at most 2^62 − 1 in lowest terms (WF-092) |
| `denominator` | Unit? | present for rate units (`€/jr`, `%/jaar`) |
| `spellings` | list of string? | the further spellings that resolve to this unit (word and plural aliases): on its declaration in `units` only, at least one when present; the migration and the emitters write none equal to the `abbreviation` |

v0 name: `Eenheid`/`Eenheidstelsel`. **Landed L3 (2026-09-24).** No field is
ever `null`: absent is the one spelling of a field not given. The unit system
is the list `units`, one `Unit` per abbreviation — a second declaration of an
abbreviation in one system is ill-formed (WF-009) — and a unit carries its
further spellings on its own declaration; an occurrence of the unit — on a
`Number`, as a `denominator` — is the same node without `spellings`, and every
reader refuses the field there. *(History until L3.)* The v0 unit system was a
map from a key to a unit beside a second map from an alias to a unit, so one
unit could be written several times over. The migration wrote the map as the
list in map order, made every alias a spelling of the unit with the same
abbreviation, and made a map key that differs from the abbreviation the first
spelling of its unit (card `20260924-l3-typesysteem`).

*What a spelling means is **OPEN**.* No rule constrains the content of
`spellings`: one spelling may stand on two units of a system, and a spelling
may equal another unit's abbreviation (measured 2026-09-24: `kg` carrying
the spelling `t` beside the unit `t`, and `kg` and `t` both carrying `ton`,
are verifier-clean). No executor reads them either — the two OCaml executors
never consult them, and the Drools loader stores them in its model, where
nothing reads them — so today a spelling changes no answer, and a rule would
refuse documents on behalf of a reader that does not exist. The rule belongs
with the first reader: a frontend or executor that resolves a unit by its
spelling needs spellings unique within their system and disjoint from its
abbreviations, compared under the ch. 2.1 identity.

**The wire form of a unit.** A unit writes the four scalar fields
`abbreviation`, `system`, `canonical` and `factorToCanonical`, plus a nested
`denominator` (until L3 the v0 spelling `afkorting`, `stelsel`, `canoniek`,
`factorNaarCanoniek` and `noemer`); the three NAMES, the factor — its `num` and
its `den` — **and the `denominator` recursively** are exactly what WF-091
(ch. 4.1) compares BYTE for byte when it crosses one unit against another,
which makes their spelling load-bearing rather than decorative. That rule
compares two units and can say nothing about ONE: it passes the moment both
sides carry the same spelling, however meaningless. The form law that holds of
a unit on its own is **WF-092** below — the conversion pair, `canonical` and
`factorToCanonical`, is one fact in two fields and the schema lets each be
absent independently. Of the four combinations **two** are units and two are
not: both present is the ordinary declared unit; both absent is the **bare**
form; either half ALONE is not a unit at all. The remaining name, `system`, is
a **reference**, and since 2026-09-05 it is crossed: **WF-034 requires a
present `system` to name a declared UnitSystem** (ch. 4.6 (r)). It is the
occurrence's one link back to the conversion table that gives it meaning,
WF-009 checks only the table's internal consistency, and a dangling system
name switched the ASP backend's literal-restating pass off in silence — a
measured divergence on a verifier-clean document. Absence keeps its meaning:
`system` is legitimately absent for the built-in `%` and for the bare calendar
units, and the rule passes over those by construction.

The bare form covers the two shapes that legitimately have no conversion pair
to spell. One is the self-standing declared unit — 28 occurrences over the
golden corpus, the WF-038 duration parameters and WF-035 duration operands,
every one of them also naming no system. The other is the opaque **product**
unit of point 4 below, for which the field table above already records
`canonical` as absent.

Bare does not mean inert, and a bare unit is not one that converts into
itself and nothing else. Convertibility reads a missing `canonical` as the
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
   form is dropped. **Landed L3 (2026-09-24, B17 (i)).** The wire writes a
   factor as the exact rational `{"num": n, "den": d}`, two integers of at
   least 1, without a node discriminator and in lowest terms (WF-092 (c));
   a factor written as a JSON number or a string, or with an integer below
   1 or above 2^62 − 1, is refused at load by every reader, in one
   sentence: "a conversion factor is an exact rational {num, den} whose two
   integers are at least 1 and at most 4611686018427387903 (2^62 - 1)". A
   v0 factor that was a whole number migrated verbatim over 1; any
   other became the fraction with the smallest denominator within 10^-14,
   relative, of the v0 double — the v0 doubles are products of ratios
   rounded per step, so exact double equality does not find the declared
   `1/N` form, and the RegelSpraak frontend's re-emission of the parity
   dump, which knows the declared forms, checks the result (card
   `20260924-l3-typesysteem`). The pre-rounded ratio the Drools backend
   still computes from a factor is the remainder of the row-26 delta.
   *v0 mirror note (fail-open):* v0's factor lookup
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
  MUST be positive rationals — since L3 (2026-09-24, B17 (i)) the wire form
  itself: `{num, den}` with two integers of at least 1, held by the schema
  and every loader, in lowest terms by WF-092 (c), so no clause of this rule
  reads the factor any more. A `canonical` names a unit of its own system by
  that unit's `abbreviation`, compared under the ch. 2.1 identity (the v0 map
  key it resolved against until L3 is gone; measured at the migration, every
  `canonical` in the four zones and the parity zone names an abbreviation of
  its system). Under the same identity a unit system MUST declare each
  abbreviation once — one `Unit` per abbreviation (since L3, 2026-09-24): the
  v0 map key made a second declaration unwritable, the L3 list can write it,
  and then an abbreviation names two units with two factors. The readers
  parted on that document — the verifier, `ref` and the ASP backend passed
  it, the Drools loader refused it (measured 2026-09-24 on a second `t` with
  another factor) — so the verifier refuses the second declaration at its
  own position: `"the unit '<a>' is declared twice in the unit system '<s>':
  one Unit per abbreviation"`, the sentence the Drools loader writes too
  (witness `wf009-duplicate-abbreviation`). *v0 mirror note:* a conversion cycle
  silently makes the re-entered unit its own base; an *unknown* target is
  worse — v0 adopts the unknown name as the canonical, so two units both
  declared against the same unknown target silently become convertible
  through a unit that does not exist; an unreadable factor warns and
  drops. **Delta (promotion).** *Scope note (2026-09-05):* this rule walks a
  DECLARED system's own table. The opposite direction — a unit OCCURRENCE
  naming the system it belongs to — is WF-034's since the sixth wave
  (ch. 4.6 (r)); whether the occurrence's `abbreviation` is a unit that
  system declares is this rule's neighbourhood and stays OPEN, recorded in
  the ch. 4.6 open list.
- **WF-010** Restatement (in any layer) between non-convertible units is
  ill-formed. Like WF-008 this is a *use-time* rule — it constrains the
  layers where restatement occurs, and its check lands with the expression
  layer. A unit-less operand meets a unit-bearing operand only where the
  operator table of the expression layer explicitly allows it (forward
  reference; specified with that layer — **OPEN**).
- **WF-092** A unit MUST spell its conversion pair whole: `canonical` and
  `factorToCanonical` (v0 `canoniek`, `factorNaarCanoniek`) are present
  together or absent together, on the numerator and, recursively, on the
  `denominator` of a rate. The bare form (both absent) is admissible and
  unchanged; either HALF is ill-formed — (a) a canonical target with no
  factor states a conversion it cannot perform, and (b) a factor with no
  canonical target names no conversion at all. (c) Since L3 (2026-09-24,
  B17 (i)) a factor MUST be in lowest terms: gcd(`num`, `den`) = 1. One
  value then has one spelling, which WF-091 relies on — it compares a
  factor byte for byte, so `{24, 2}` against `{12, 1}` would be a
  difference between two units that convert identically; a schema cannot
  state a greatest common divisor, so the verifier does. That both integers
  are at least 1 and at most 2^62 − 1 is the wire form (WF-009). Witnesses: (a)
  `wf092-canonical-without-factor` and `wf092-denominator-without-factor` (the
  recursion), (b) `wf092-factor-without-canonical`, (c)
  `wf092-factor-not-in-lowest-terms`. This is a SHAPE law on every unit in
  the document, not a cross between two of them, so it holds at every
  position a unit can occupy — the declared systems, the model-layer
  datatypes, every `type` a value node carries of its own, and **an inline
  lambda's parameter datatypes** (declared where only the lambda's body is
  walked) — and a document carrying the shape is refused once per position.
  *(Until L2, 2026-09-24, also a fold source's own `type` and a rule
  target's own `type`; both left the wire with the reference's annotation,
  B13.)* No neighbouring rule reaches it: WF-009 follows a chain it can
  FOLLOW (until L3 it also tested a factor it FOUND for positivity, which
  since L3 is the factor's wire form), and a missing half is nothing to look
  at; WF-035/WF-023 cross units for CONVERTIBILITY, which
  reads the canonical names and never the factor, and reads a missing
  canonical as the abbreviation; WF-091 compares byte for byte and agrees
  the moment the half-form is copied consistently. Where both rules speak at
  one position their REPORT ORDER differs — WF-092 precedes WF-091 on a
  walked expression type and (until L2) followed it on a rule target, because
  the two clauses sat on opposite sides of the identity cross in each. Measured, and
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
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the fact type's names in the vocabularies (ch. 2.12) |

A **Role**:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | the navigation label |
| `objectType` | string | filler object-type name (resolves per WF-003) |
| `plural` | string? | declared plural; absent = none |
| `many` | boolean | cardinality: to-many (`meerdere`) vs to-one (`één`) |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the role's names in the vocabularies (ch. 2.12) |

v0 names: `Feittype`, `Rol`. The v0 model layer is already symbolic — no
bean-field names appear here; the bean-field leakage lives in the v0
*navigation contract* and the rule-layer encodings, whose replacement by
symbolic role bindings is `CONSTRUCTS.md` rows 4/22 (rule-layer chapters).

A **RoleBinding** (an extension node per the Method; v0 wire node
`RolBinding`, the wire name until L5) is the symbolic reference those
chapters bind with:

| Field | Type | Meaning |
|---|---|---|
| `factType` | string | v0 `feittype`: a declared fact type, by name |
| `role` | string | v0 `rol`: one of its roles, by **declared role name** — the plural is presentation and does not bind |

- **WF-047** A role binding MUST resolve: the fact type is declared, the
  role names exactly one of its roles (case-insensitive over the declared
  name), and the fact type has exactly two roles — or exactly **one**,
  reciprocal (WF-011/013), in which case the bound and the opposite role
  are that same role: both sides fill the one declared type. At each
  use site the bound role's filler type and the opposite role's filler
  type MUST agree with the types the node declares (the site chapters
  state which side is bound: ch. 3.2, 4.3), and where the node also
  carries a written role name — the COUNT source of an instance fold,
  whose `attribute` is the written name of the role its range binds
  (ch. 4.3, the reading of the source, (ii)) — that name MUST denote the
  bound role, on declared name or declared plural. **Since L2
  (2026-09-24):** where the written name on a count source denoted
  neither, the L2 migration and the emitter write the declared name of
  the bound role in `attribute` and keep the written spelling in `source`
  (`bron` until L5; measured: two goldens, `jutterswet-kaveltelling` and
  `jutterswet-muntpeiling`, and four places in the parity zone; recorded
  in the L2 card, `backlog/done/20260923-l2-referentielaag.md`); the rule
  stays "denotes on name or plural".
- **WF-048** *(removed at L2, 2026-09-24 — history.)* Landed L2
  2026-09-24: the binding is required on the nodes this rule governed. The
  object creation's back-link role was never among them: it stays
  optional (`connectingRole`, ch. 3.3), and without it the executors still
  derive the relation from the type pair (`RUNTIME-SEMANTICS.md`
  ch. 5.5). Until then a node could navigate the connecting relation *without* a binding — a
  bindingless allocation (ch. 3.2) or instance fold (ch. 4.3) — and this
  rule made it well-formed only if **exactly one** fact type related its
  type pair (a two-role fact type over the pair, or — for a self-typed
  pair — a reciprocal one): the ch. 4.1 runtime derivation fault of
  `RUNTIME-SEMANTICS.md` lifted to well-formedness. As this entry
  announced, the binding became required and WF-048 retired with the
  format bump (B12): an allocation carries its recipient role, a quantifier
  fold its range, a union selection its relation, and a single-argument
  fold with an `elementType` its range under every kind but `total` —
  the schema and every loader refuse the bindingless form, so no reader
  derives the relation of these nodes. The two witnesses
  (`wf048-ambiguous-connecting-relation`, `wf048-telling-meerduidig`) were
  removed with reason. The migration derived the binding of the other
  bindingless forms with exactly this derivation; where it found no single
  candidate — five negative documents, four allocations without a source
  type and one count beside an instance-set union — the witness was
  rewritten by hand with its binding written (`corpus/negative/README.md`,
  L2).

Role-name matching is case-insensitive, over the declared name and the
declared plural. *v0 mirror note:* when no plural is declared, v0 admits a
best-effort synthesized plural (`+s`/`+en`) at use sites; a declared
plural always wins over a synthesized one. **Decided (2026-08-25):** v1
keeps NO best-effort plural matching — morphology is not semantics and
belongs in the frontend, if anywhere; the `+s`/`+en` synthesis is a
tracked, unmirrored delta. What that admission actually bought at v0's
use site was narrowed to the deterministic **type-pair fallback** of the
instance-fold source until L2: name resolution first (declared name or
declared plural, ci); a name that resolved to no declared role stood
only when exactly one fact type connected the subject type to the
element type. Since L2 (2026-09-24) the count source carries its range,
and its written name MUST denote the bound role (WF-047): no reader
resolves a written name to a relation any more, so the fallback no
longer exists.

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

**Roles and object-type members are separate namespaces.** WF-002 states
that attributes and characteristics are separate namespaces; roles are a
third. A role MAY share its name, case-insensitively, with an attribute
or a characteristic of an object type the same fact type relates. Such a
document is **well-formed**, and no rule above narrows it. The reason is
structural: a navigated read names its role hop and its attribute in
*different* fields — the `path` of ch. 4.1a against `attribute` (row 4;
until L5 `navigatieBindingen` against `attribuut`) — so at
a read a role name and a member name do not compete for one wire
position. The reference interpreter and the ASP backend both answer such
a read correctly, with no special case. It is a REACHABLE but UNINHABITED
shape: swept 2026-09-10 over every corpus zone — 555 documents — the
collision occurs zero times, so no existing case pins this and the
paragraph is what states it.

**One wire position is shared, and there the collision has a
consequence (since L2, 2026-09-24).** The COUNT source of an instance
fold (ch. 4.3) writes the name of the role its range binds in
`attribute`, the field in which a value fold's set source names the
member it reads per contributor, and the two readings are told apart by
that name alone: a name that is an attribute or a characteristic of the
element type is a member read, any other name a count source (WF-093; the
set flag and the unknown-type sentinel that used to mark the count source
left the wire with B11 and B13). A role whose written name — its declared
name or its declared plural, whichever the source writes (WF-047) — is
also the name of a member of the element type therefore cannot be counted
under that spelling: the source reads as the member, and WF-093 (b)
refuses `count` over it. Where the other declared spelling does not
collide, the count is written with that one; where every declared
spelling collides, no count source can name the role.

The neighbouring shape — one written name matching *several* reachable
roles — has no wire position since L2 (2026-09-24, B11, B12). A
navigation and a fold range name their relation by a role binding (a
fact type and a declared role name), so no reader resolves a written name
to a relation: the binding decides which relation is walked, and the
written name stays in `source` — for the count source in `attribute`, where
WF-047 requires only that it denote the bound role.
`corpus/golden/ambiguous-fold-role-name` pins it: the count's written
name `vriend` is the declared name of a role in two fact types the
subject reaches, and the count answers through its `sourceRole`.
*(History until L2:)* a written name that resolved to several roles was a
loud executor fault, never a guess (ch. 4.3), and the navigation arm was
refused statically by WF-053. The case-input arm is not changed by L2: a
case that supplies links under a role key that resolves to two roles is
refused identically by both executors while LOADING the case, and the red
corpus pins that refusal (`corpus/red/ambiguous-case-role-key`; the
red corpus's expected-fault contract admits a load fault since
2026-09-19, `backlog/done/20260910-rood-zone-laadfouten.md`).

*Consumer note, recorded rather than hidden.* A consumer that
materialises each object type as a record with **one property per
identifier** — a Java bean, a struct, a row — cannot hold that shape: it
has one slot where the document has two names, so one of the two is
displaced. This bites wherever the relation's scalar reference lands,
which is at the end whose *connecting role is to-one* — not only the many
side of a one-to-many fact type, but each end of a one-to-one one, and a
self-typed pair on the single bean it has.

That is a limit of the target format, not a well-formedness rule, and
this specification does not lift it into one: rejecting the document
would deny a program that has exactly one meaning, which the other
executors compute. What such a consumer does instead is refuse the
affected construct per form, loudly and naming the displaced reference,
rather than reading the displacing member in the role's place. The
Drools backend does exactly that in both of its transpiler branches, at
each of the readers audited so far: the case's role input, the object
creation's back-link and the aggregation join (2026-08-30), and the
navigated single-hop, chain, comparison and reactivity reads plus the
ch. 8.1.7 role-membership check — which does not navigate but null-tests
the same field — since 2026-09-10. Those refusals are pinned by that
backend's own test suite and by gate 12, not by `spec/verifier/` — a
verifier that enforced them would be enforcing one target format's shape
on every consumer. Since 2026-09-11 the audit is complete for the v0 backend: the ch. 8.1.7
FAR-side membership join, the temporal set summation's own join, the
navigated characteristic timeline, the allocation's recipient join, the
fact creation's three field positions, and the presence count of a to-one
role are all gated through the same shared question, each first measured
on a verifier-clean document through `ref`, clingo and both Drools
branches. One reader was audited and deliberately left ungated: the
whole-period fold over a navigated SET, which is unreachable through a
well-formed document (WF-096 until L2; since L2 a navigated read with a
to-many hop is WF-053's to-many clause). Those refusals are pinned by that
backend's own test suite and by gate 12, not by `spec/verifier/` — a
verifier that enforced them would be enforcing one target format's shape
on every consumer.

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
| `fixedStart` | string? | the point the periods run from (below); absent = the calendar grid, or a variable start |

v0 name: `Tijdlijn`. Granularity is one of `day`, `week`, `month`,
`quarter`, `year` — deliberately no sub-day granularity (mirrored).
v0 values: `DAG` → `day`, `WEEK` → `week`, `MAAND` → `month`, `KWARTAAL` →
`quarter`, `JAAR` → `year`, as on the `TimelineFacet` (ch. 2.2).

*v0 mirror note:* the name-matching rule above (whitespace collapse,
case-insensitivity, stripping one leading Dutch article) is v0 morphology
mirrored into an identity rule. **OPEN (same family as the best-effort
role-plural matching, ch. 2.6):** whether v1 keeps source-language
morphology inside a core identity rule at all, or moves it to the frontend
so the core sees only resolved names.

**The fixed start point** (§13.3.6.3–.7). A timeline whose periods do not
start where the calendar's do writes the point they start from in
`fixedStart`, in one of two lexemes, told apart by their first character:

- a **date** `yyyy-mm-dd`, WF-084's strict ISO lexeme (ch. 4.1), on a week
  grid and on a grid of `count` ≥ 2 — the source writes "startend op
  5-1-2026";
- a **day-month** `--mm-dd`, the `gMonthDay` form of XML Schema (a date
  without its year), on a month, quarter or year grid of `count` 1 — the
  source writes "startend op dag 15 van de maand", "startend op dag 1 van
  maand 2, 5, 8 en 11", "startend op dag 15 van maand 10". The periods start
  on day `dd` of month `mm` and of every month one unit apart from it. The
  spelling is canonical: `mm` is the earliest month of the year with a wall
  on day `dd` itself, so a month grid writes `01`, a quarter grid the first
  month of its cycle (`01`, `02` or `03`) unless day `dd` does not occur in
  that month, and a year grid its one month; day 31 of months 2, 5, 8 and
  11 is `--05-31`.

Absent, the timeline lies on the calendar grid (`RUNTIME-SEMANTICS.md`
ch. 3.2). The calendar's own start point is therefore never written: day 1
of the month, day 1 of months 1, 4, 7 and 10 and day 1 of month 1 are the
timeline without a `fixedStart`, and `--01-01` is refused (WF-115). The
timeline facet of an attribute, a characteristic or a parameter carries the
same field (ch. 2.2): the inline forms ("voor elk jaar startend op dag 15
van maand 10") name no timeline, so their start point sits on the facet, and
a facet that names a timeline adopts it with the granularity and the count.
The grid a fixed start point lays is `RUNTIME-SEMANTICS.md` ch. 3.2.

- **WF-015** All references to a timeline name MUST resolve (WF-003), and —
  with WF-001 — there is exactly one declaration per normalized name, so
  the v0 "first declaration wins" rule vanishes with the duplicate it
  resolved. **Delta (promotion).**
- **WF-016** A `variableStart` timeline MUST be anchored by a start-point
  determination. This is a cross-layer rule: the determination lives in the
  rule layer, so the check is specified there — **OPEN** here, recorded so
  the model layer names the obligation. *v0 mirror note:* v0 discovers the
  missing anchor only at transpile time (marker/skip).
- **WF-115** A `fixedStart` MUST fit its grid, wherever it is written — on a
  `Timeline`, or on the timeline facet of an attribute, a characteristic or
  a parameter. Three arms. **(a)** A `Timeline` with `variableStart: true`
  carries none: its periods run from the anchor its start-point
  determination binds per object (ch. 3.8), and a timeline has one start.
  **(b)** Its lexeme is the one its grid takes: none on a day grid of
  `count` 1, whose walls are all day boundaries; a date `yyyy-mm-dd` naming
  a calendar day on a week grid and on a grid of `count` ≥ 2; on a month,
  quarter or year grid of `count` 1 a day-month `--mm-dd` naming a day of a
  leap year, spelt with the earliest month (above), and never `--01-01`.
  **(c)** A facet that names a timeline carries that timeline's
  `fixedStart`, or none when the timeline has none: adoption is total, as
  WF-005 holds it for the granularity and the count. *v0 mirror note:* v0
  resolved the fixed start point in its model, and the wire had no field
  for it; the RegelSpraak frontend refused a start point off the calendar
  grid at emission rather than drop it. **Delta (addition).**

Worked example (facet adoption, becomes a `ref/` test): declaration
"timeline `het penningjaar`, granularity year, count 1, fixed start" +
attribute declared against timeline `penningjaar` → the attribute's facet
is `{granularity: year, count: 1, timelineName: "penningjaar"}` (article
stripped, adoption total per WF-005). "Fixed start" here is the calendar's:
`variableStart: false` without a `fixedStart`, the timeline on the calendar
grid.

Worked example (the fixed start point; the declarations of
`corpus/golden/jutterswet-vast-startpunt`): "Tijdlijn het stormjaar heeft
tijdvakken van een jaar startend op dag 1 van maand 10" →
`{name: "het stormjaar", variableStart: false, granularity: year, count: 1,
fixedStart: "--10-01"}`, and an attribute declared "voor elke stormjaar" →
its facet `{granularity: year, count: 1, fixedStart: "--10-01",
timelineName: "stormjaar"}`; an attribute declared "voor elke maand
startend op dag 31 van de maand" → `{granularity: month, count: 1,
fixedStart: "--01-31"}`; a characteristic declared "voor elke week startend
op 30-9-2024" → `{granularity: week, count: 1, fixedStart: "2024-09-30"}`.
Spelt by the same rule, "voor elk kwartaal startend op dag 31 van maand 2,
5, 8 en 11" is `--05-31`.

### 2.9 DayKind

A named class of days (`werkdag`, `stormvloeddag`) usable as a predicate
and as a duration unit.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | singular |
| `plural` | string? | the further duration-unit spelling (`5 werkdagen`) |

Both spellings name the day kind as a duration unit (`1 werkdag`, `5
werkdagen`); they are every spelling a day kind has on the wire. v0 name: the
`dagsoorten` list (the day-kind's *definition* — a predicate over a date — is a
rule-layer form, `CONSTRUCTS.md` row 23). Spelling lookup is trimmed and
case-insensitive. **Landed L3 (2026-09-24).** `model.dayKinds` is a list of
`DayKind {name, plural?}` nodes. *(History until L3.)* The v0 wire carried the
names as a list of strings and every registered unit spelling, singular and
plural alike, as a KEY of a separate registry whose value named the day kind
(`dagsoortPerEenheid`). The migration dropped a key equal to its day kind's
name, since the name is that spelling, wrote a second key as the `plural`, and
refused a third; a key whose value named no declared day kind had no L3 form
either. Those documents were negative witnesses, and each was removed with
reason (`corpus/negative/README.md`, L3; card `20260924-l3-typesysteem`).

- **WF-017** A day-kind's spellings MUST NOT collide with a spelling of the
  built-in calendar units, and MUST NOT collide with another day-kind's
  spellings. The calendar spellings are the **thirty** of the normative
  time-unit table (`RUNTIME-SEMANTICS.md` ch. 3.8) — the nine units'
  singular, abbreviation and plural (twenty-seven), plus the second's second
  abbreviation `s` and the second plurals `secondes` and `millisecondes`
  (thirty) — and the comparison is against that table itself, so the rule and
  the arithmetic that would read the spelling cannot drift apart. Spellings
  are compared trimmed and case-insensitively, as ch. 3.8 states and the
  ch. 2.1 identity has it.
  *v0 mirror note:* the built-in collision warns and skips; the
  day-kind/day-kind collision is silently first-wins. **Delta (promotion).**

*What the wire checks.* A day kind has exactly two spellings on the wire, its
`name` and its `plural`, and the rule reads both. Five collisions are possible,
covered by three clauses of this rule plus WF-001's (maintainer decision
2026-09-10 — the clauses beyond the first are ARMS of WF-017 and not a rule of
their own: it is the same promoted MUST, checked wherever a spelling stands):

| # | collision | clause |
|---|---|---|
| 1 | name × built-in calendar unit | **WF-017**, over `/model/dayKinds/<i>/name` |
| 2 | plural × built-in calendar unit | **WF-017 arm (a)**, over `/model/dayKinds/<i>/plural` |
| 3 | name × another day kind's name | **WF-001** (the `dayKinds` namespace, ch. 2.1 identity; witness `wf001-duplicate-day-kind`) |
| 4 | plural × another day kind's name | **WF-017 arm (b)**, over the plural |
| 5 | plural × another day kind's plural | **WF-017 arm (b)**, over the later day kind's plural |

A plural equal to its own day kind's name collides with nothing. *(History
until L3.)* The further spellings stood as registry keys, so rows 2 and 4 read
a key — arm (a) a key that is a calendar spelling, arm (b) a DECLARED name
registered as the spelling of a DIFFERENT day kind, the one form of row 4 the
registry could show — and row 5 was invisible: the registry held one day kind
per key, and the losing registration was simply absent.

*Arm (a) — a plural that shadows a calendar unit.* Such a plural makes the
document contradict itself. `RUNTIME-SEMANTICS.md` ch. 3.8 settles the reading
normatively: a duration's unit is recognised by spelling through the calendar
table, and "a spelling outside the table is a declared day kind". A day kind's
plural says *this spelling is a day-kind unit*; a spelling the table carries
says *this spelling is a calendar unit*. Both at once is not a reading a
consumer can hold, and it is not a defect any one consumer must notice for the
wire to be wrong.

Two of them did not, and two did — *measured 2026-09-10 on the registry form,
and it is the second reason the arm exists.* The verifier (WF-035, WF-038),
`ref` and the ASP backend all consult the calendar table FIRST and reach the day
kinds only where the table has nothing, so on those three a further spelling
`dagen` changes nothing at all: `datum + 5 dagen` stays calendar arithmetic.
Both Drools branches reversed the order at all four of their day-kind positions
— they asked the day kinds before the table — so on the same document they read
day-kind arithmetic where the other three read calendar arithmetic. The
executors SPLIT, which is what makes this a wire question rather than a taste.
*Closed 2026-09-20:* the order is normative (`RUNTIME-SEMANTICS.md` ch. 3.8,
the paragraph on the lookup order) and all four of those positions now read the
table first, so no consumer acts on the contradiction either way — which does
not make the document admissible, and this arm is why it is not.

*Measured 2026-09-10:* before this arm, a registry key `"dagen"` for
`schouwdag` beside the ordinary entries was verifier-clean, while the same
spelling DECLARED was already refused by clause 1. Here the IR was weaker than
its own producer, and what carries that is the REGISTRATION GUARD rather than
the grammar: the RegelSpraak frontend refuses to register a spelling that
coincides with a calendar spelling, warns, and drops the entry. It compares
against all thirty since 2026-09-20 (the paragraph below); what follows is the
measurement of the day it still knew eighteen, kept because it is what shows the
guard and not the grammar doing the work. *Measured 2026-09-10 over all
eighteen as a day-kind plural:* fourteen do not even parse — the names are
grammar keywords, and the frontend exits non-zero without emitting (`Dagsoort de
vaardag (mv: weken)`) — while four parse cleanly, `uren`, `minuten`, `seconden`
and `milliseconden`. For those four the frontend emits with exit 0 and the guard
alone keeps the spelling off the wire; its warning is carried in the emitted
document's `diagnostics` array, and since 2026-09-19 it is also printed to
stderr, so a producer reading either surface sees it. Either way no document
this frontend emits can carry the shape; it is wire-only, which is exactly why
it must be a wire rule or nothing. Witness:
`corpus/negative/wf017-plural-shadows-calendar-unit` (since L3; until then
`wf017-eenheidsleutel-schaduwt-kalendereenheid`, a registry key), and for the
abbreviation class of the widening below
`corpus/negative/wf017-plural-shadows-calendar-abbreviation` (a plural `wk`;
until L3 the registry key of `wf017-eenheidsleutel-schaduwt-kalenderafkorting`).

*The comparison is the TABLE, all thirty spellings of it (maintainer decision
2026-09-19, landed 2026-09-20).* Until then arm (a) and clause 1 both compared
against the eighteen names the frontend's registration guard knew — the
singular and plural of the nine units — while the calendar table of
`RUNTIME-SEMANTICS.md` ch. 3.8 recognises **thirty**. The twelve that are
neither singular nor `-en` plural fell through both: the ten abbreviations
`jr`, `mnd`, `kw`, `wk`, `dg`, `u`, `min`, `sec`, `s`, `ms`, and the two SECOND
PLURAL FORMS `secondes` and `millisecondes`, which are not abbreviations at all
and which a repair aimed at abbreviations alone would have left standing. They
get the same answer, and the second plurals are named here because that is the
only thing that keeps them from being read past. *Measured 2026-09-20 on the
binary of the day before (md5 `97239d1f8c2706a2548594b0541b5f34`):* all twelve
were verifier-clean as registry keys AND as declared names.

Unlike the eighteen this flank was NOT wire-only, which is what made it
urgent: the frontend emitted `Dagsoort de wk (mv: wks)` with exit 0, an EMPTY
`diagnostics` array, and `wk` both as a registry key and as a declared day kind.
Goldens carry the abbreviations as live calendar units
(`jutterswet-vervaltermijn` uses `wk`, `mnd` and `kw`), so under the
lookup-order divergence above such a document split the executors in earnest —
measured on `jutterswet-bergingsklok` respelled to `wk`: `ref` 2026-03-31 /
2026-02-17 / 5 / 5 against both Drools branches' 2026-04-15 / 2026-02-01 / 3 /
3.

Both halves moved with the widening, which is what made it a single change
rather than three. The **comparison** is now the table lookup itself rather
than a list beside it, so a rule and the arithmetic it fences cannot come to
know different tables. The **producer** moved with it, spec-first: the
frontend's registration guard compares against the same table, and a declared
NAME that stands in it is an ERROR there — which blocks the emission, because
dropping the name alone would leave its §9.8 definition pointing at a day kind
the document no longer declares. The **executors** moved with it too (the
paragraph above). *Cost, measured 2026-09-20 over the four corpus zones plus
the read-only parity zone, 699 documents:* not one verdict and not one report
line moves, and the twelve stay verifier-clean wherever they are written as
ordinary calendar units. Witnesses: `wf017-day-kind-name-shadows-calendar-abbreviation`
(a declared name, an abbreviation) and, since L3, on the plural:
`wf017-plural-shadows-second-plural-form` (the second-plural class, which
the other witnesses do not reach). The two registry-key witnesses of the
widening had no L3 form and left with the registry.

*Arm (b) — a plural that is another day kind's spelling.* A spelling that names
two day kinds has no reading: `5 schouwdagen` counts the days of whichever day
kind a consumer resolves it to. The producer is exactly as weak as the wire
here. Registration is `putIfAbsent` — first-wins, silently — so a second day
kind whose own SINGULAR name is already registered as the first one's plural
loses its own spelling without a diagnostic. *Measured end-to-end 2026-09-10*
on two day kinds `schouwdag` (plural `schouwdagen`) and `schouwdagen` (plural
`schouwdagens`): the frontend emits with no warning and no error, and the
verifier of the day before reported PASS — so `5 schouwdagen` reckoned with the
definition of `schouwdag`. WF-001 does not reach it: the two DECLARED names
differ. Since L3 both halves of the collision are on the wire, the first day
kind's `plural` and the second one's `name`, and the arm reports the plural
(row 4); witness `corpus/negative/wf017-day-kind-name-binds-foreign-day-kind`,
the frontend's own emission of that source. Row 5, two plurals that are one
spelling, had no form on the registry and has one now; witness
`corpus/negative/wf017-plural-collides-with-foreign-plural`. The producer half —
a diagnostic on the silent `putIfAbsent` — belongs to the frontend and lands
there, spec-first.

*Identity.* All three clauses compare under the ch. 2.1 identity (collapse
whitespace, lower case), the same one a consumer applies when it resolves a
spelling, so the positions agree on what "the same spelling" means. It is pinned
by `corpus/negative/wf017-plural-collides-with-foreign-plural`, whose two plurals
differ in case alone: a byte-exact arm (b) passes that document. *(History
until L3.)* The identity was pinned from the positive side, by entries of
`corpus/golden/jutterswet-vormenkabinet`'s registry that bound a declared day
kind to itself only through it (`"schouwdag": "SchouwDag"`); those entries
left the wire with the registry, and the golden's day kinds carry no spelling
that needs the identity.

*(History until L3.)* The registry's VALUES were crossed by WF-034 (ch. 4.6
(s)): every value had to name a day kind `dayKinds` declares — RESOLUTION, one
name against a declaration table, where WF-017 is COLLISION. Arm (b) stepped
back where the two met, firing only when the value named a declared day kind.
That arm left with the registry: a spelling stands on its own day kind, so no
value is left to resolve. A registry key that was not a declared name and
belonged to another day kind's spelling set stayed admissible then, because
the wire carried no plural field and nothing distinguished a declared plural
from a derived one or from an abbreviation; since L3 the plural is a field,
and row 5 of the table checks it.

### 2.10 Parameter

A top-level named value.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | declared name |
| `datatype` | Datatype | inline or via domain reference |
| `timeline` | TimelineFacet? | present = time-dependent (value is a per-period timeline); the facet has no `timelineName` |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the parameter's names in the vocabularies (ch. 2.12) |

v0 name: `Parameter`. The *values* of parameters (validity-versioned
vintages, selected by evaluation date, fail-fast on gaps and overlaps) are
data-plane, specified in `RUNTIME-SEMANTICS.md` ch. 8 — deliberately not a
model-layer node (`CONSTRUCTS.md` ch. 2.4, `Parameterset`).

*v0 mirror note:* a v0 parameter records only a bare granularity — no
period count and no timeline name (a parameter declared over a multi-period
or named timeline loses that information in v0). **Delta:** v1 gives
parameters the same full `TimelineFacet` as attributes. *Migration:* a v0
granularity becomes `{granularity, count: 1}`; no v0 parameter carries a
timeline name. **Landed L3 (2026-09-24, B18).** A parameter is `Parameter
{name, datatype, timeline?}`: `timeline` is a `TimelineFacet` carrying
`granularity` and `count` and no `timelineName` — no v0 parameter records one,
so the position is closed, as a characteristic's is (ch. 2.2), and every
reader refuses the field there. A v0 parameter without a granularity carries
no facet. `datatype` is required: a parameter without one cannot be written
(no v0 parameter was without one). The facet's `count` is 1, and WF-005
refuses any other on a parameter while multi-period grids are OPEN
(`RUNTIME-SEMANTICS.md` ch. 3.12): no executor evaluates a parameter's count
— `ref` and the ASP backend take the granularity alone, the Drools loader
refuses another count by name — so before that clause a count of 2 was
verifier-clean, replayed by two executors as a one-period facet and refused
by the third (measured 2026-09-24; witness
`wf005-parameter-facet-count-two`).

- **WF-018** A parameter's datatype MUST resolve (WF-003) and its timeline
  facet, when present, MUST satisfy WF-005. Since L2 (2026-09-24, B16) an
  unresolvable domain is a parameter whose enumeration names a domain no
  declaration carries (the unknown-type node left the datatype union;
  witness `wf018-parameter-unknown`, rewritten by hand). Since L3
  (2026-09-24, B18) the facet is the attribute's node, and its clause is
  WF-005's own count clause, reported under WF-005 (witnesses
  `wf005-parameter-facet-count-zero` and, for a count above 1,
  `wf005-parameter-facet-count-two`); the datatype clause no longer has a
  missing datatype to report, which the wire cannot carry. **Delta
  (promotion).**

### 2.11 FunctionDef — named reusable logic

`layer: core`; an **extension form** per the Method: an optional
list `functionDefs` under `model` (top-level until M1), never emitter-produced (CONSTRUCTS
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
  (`BinderRef`), document parameters (`ParameterReference`), literals,
  operators, `Construct`/`Match`/`IfThenElse`, `StructLiteral`/
  `FieldAccess` (the **Phase-5c widening** — a record built or read over
  binders and parameters keeps the body closed), `Let` (a call-by-need binding
  over closed operands stays closed, ch. 4.10), and other functions via
  `Invoke`; any attribute, characteristic, aggregation, temporal or
  variable read is ill-formed; the **body's static type MUST equal the
  declared `result`** — full datatype equality, soort and keuzetype domain
  alike, as WF-068/069/070 cross their operands (the body is the write
  into the result every `Invoke` reads: the write half of ch. 2.4's domain
  identity for functions, where WF-068 is the read half; a body whose type
  nothing declares defers, as under WF-023; a parameter read in the body is
  typed by the parameter's declaration) — and the invocation graph over
  `functionDefs` MUST be acyclic (no recursion: the Catala mirror, and
  what keeps termination and stratification trivial — a closed body
  contributes no dependency edges, so WF-045/046 are untouched).

### 2.12 Vocabularies and designations

A declaration has one **canonical name**: its declared name (ch. 2.1), the
name every rule is written in and every reference resolves. A **vocabulary**
is a named set of **designations**: the name of a declaration in that
vocabulary — a term of a handbook, a field name of a data model — at most one
per declaration. The vocabularies of a document and their designations are a
**side table without semantics**, `layer: core`, and every field of it is an
**extension field** under the § Method's discipline: optional, never null,
written whole or not at all, absent on a document that declares no
vocabulary.

The document declares its vocabularies in `vocabularies`, after `format`
(ch. 2). A **Vocabulary**:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | the name every designation names it by (WF-116) |
| `default` | boolean | whether this is the default vocabulary, which names every designatable declaration (WF-119); at most one per document (WF-116) |
| `language` | string? | a language code (WF-116); absent = none declared. It describes the vocabulary and never identifies it |

A vocabulary has no kind. Every designation is **text**: not empty, trimmed
at both ends, spaces inside admitted, and free of parentheses (maintainer
decision 2026-09-27). Whether the designations of a vocabulary can serve as
identifiers — as field names in a data contract, say — is a question of the
consumer that speaks it, not a rule of this format.

A declaration writes its designations in `designations`, the last field of
its node: an ObjectType, an Attribute, a Characteristic (ch. 2.2), a FactType
and a Role (ch. 2.6), a Parameter (ch. 2.10), a named form — the six forms of
ch. 3 and a defaults group of ch. 6.1, the namespace of WF-020 — and a
RuleGroup (ch. 3.9). Each entry is a record without a node discriminant:

| Field | Type | Meaning |
|---|---|---|
| `vocabulary` | string | a declared vocabulary, by name (WF-117) |
| `designation` | string | the declaration's name in that vocabulary |

An enumeration value is a bare string on the wire, so its designations stand
beside the list it is declared in: the `Enumeration` at a domain's
declaration position (ch. 2.3) carries them in `valueDesignations`, its last
field, one entry per value and vocabulary, each the record `value`,
`vocabulary`, `designation`, where `value` names one of the domain's values
(WF-117). A use site of the enumeration repeats the values (WF-088) and
carries no designations. Nothing else carries one: a domain, a unit system
or a unit, a dimension or its labels, a timeline, a day kind, a day-kind
definition and a start-point determination have no designation on this wire.
A producer writes the designations of one declaration in the order the
vocabularies are declared in; the order carries no meaning.

**No semantics.** A designation never resolves. Every reference in the rule
and expression layers names a declaration by its canonical name; a name that
only a designation spells resolves to nothing, and is refused wherever an
undeclared name is refused (WF-003, WF-034). No executor reads the side table
for evaluation: every reader reads it structurally, as it reads every node,
and a document whose designations are removed, or exchanged between the
declarations of one scope or between two vocabularies, gives every executor
the same answer (gate 13 holds the three executors to that). The case format
`uchar-case-v0` names declarations by their canonical names only.

**The effective name** of a declaration in a vocabulary is its designation
there, and its canonical name where it carries none. The reverse — from an
effective name in a vocabulary to the declaration — is what a consumer that
speaks the vocabulary needs, and WF-118 makes it a function per scope, for
every vocabulary, complete or not. Worked example: in the golden
`jutterswet-vocabulaires`, the attribute `vondstbedrag` of `Jutter` carries
the designation `totale premie` in the vocabulary `handboek` and
`VONDST_BEDRAG` in the vocabulary `cgm`; its effective names are those two,
and in `cgm` the name `VONDST_BEDRAG` of `Jutter` goes back to
`vondstbedrag`. The attribute `naam` of `Jutter` carries `naam` in
`handboek` — its canonical name, so a redundant designation (WF-120) — and
`NAAM` in `cgm`, the default vocabulary, where every declaration carries one
(WF-119).

The RegelSpraak dialect writes a vocabulary as `Vocabulaire <name>` with an
optional `(standaard)` and `(taal: <code>)`, and a designation as an
annotation `(<vocabulary>: <designation>)` at the end of a declaration's name
group (`frontends/regelspraak/grammar/DIVERGENCES.md`, "Vocabulaires en
benamingen"). A designation that carried a parenthesis could not be written
there, and neither could a vocabulary name with whitespace, a parenthesis or
a colon: those two limits are the rules below.

- **WF-116** The vocabulary declarations: (a) a vocabulary's `name` MUST
  be a word — not empty, and free of whitespace, parentheses and the colon;
  (b) the names of the vocabularies MUST be unique under case-insensitive
  comparison (ch. 2.1), since a designation names its vocabulary by name;
  (c) at most one vocabulary MUST carry `default: true`; (d) a present
  `language` MUST be a code: not empty and free of whitespace. What a code
  denotes is not checked (maintainer decision 2026-09-27).
- **WF-117** Every designation: (a) MUST name a declared vocabulary, under
  case-insensitive comparison; (b) a declaration MUST carry at most one
  designation per vocabulary, for an enumeration value counted over the
  entries of `valueDesignations` that name it; (c) MUST NOT be empty, and
  MUST NOT begin or end with whitespace; (d) MUST NOT carry a parenthesis;
  (e) an entry of `valueDesignations` MUST name a value of its enumeration,
  compared case-insensitively as WF-088 compares values.
- **WF-118** Within each vocabulary the effective names of the declarations
  of one scope MUST be unique under case-insensitive comparison. The scopes
  are those of the canonical names: the object types, the fact types, the
  parameters, the named forms (WF-020) and the rule groups, each on its own;
  the attributes of an object type together with the attributes it inherits,
  and likewise its characteristics (WF-002, WF-105); the roles of a fact
  type; the values of an enumeration domain. Two effective names that are
  both canonical names collide under WF-001, WF-002, WF-020 or WF-105, and
  this rule does not repeat them.
- **WF-119** In the default vocabulary every designatable declaration MUST
  carry a designation: every object type, attribute, characteristic, fact
  type, role, parameter, named form and rule group, and every value of every
  enumeration domain. A consumer that speaks the default vocabulary then
  never falls back to a canonical name, so a fallback that would mix two
  vocabularies in one contract cannot happen.
- **WF-120** A designation in a vocabulary that is not the default, which
  spells the canonical name of its own declaration byte for byte, is
  redundant: the effective name is the same without it. It is admitted, and
  the document MUST account for it with a warning: at least one diagnostic
  (WF-113) whose `message` contains the designation verbatim, as a marker is
  accounted for (WF-113 (b)), with the same weakness — the containment is
  literal, so another message that contains the text accounts for it as
  well. In the default vocabulary a designation is required (WF-119) and
  never redundant, and a designation that differs from the canonical name in
  case only is no redundancy: it is the vocabulary's spelling.

v0 has no vocabulary and no designation, so neither has a v0 name. **Delta
(v1 addition, `CONSTRUCTS.md`):** the side table enters as extension fields
(card `vocabulaires-benamingen`, 2026-09-27); the RegelSpraak frontend
produces it from the dialect's annotations, and every other producer writes
none. Not specified here, and deliberately: a service or an explanation per
vocabulary, a status per designation (preferred, deprecated), a vocabulary
per language, and an output that strips the annotations from a source.

A declaration that a producer made itself carries its **origin** in the
field `origin`, just before `designations` (ch. 2.13): the same side-table
discipline, on the same declarations. The designations of such a
declaration are judged by the rules above as a written one's are.

### 2.13 Origin

A producer that lowers a dialect form can make declarations of its own,
under names no source declares, from a declaration the source does declare.
The RegelSpraak frontend does so for five forms of its dialect
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Gegenereerde elementen").
Such a declaration is a declaration like every other: references resolve to
it, it carries designations, and every rule of this document applies to it.
What it has in addition is an **origin**: the form that made it and the
declaration it comes from. A consumer that shows a document — a list of the
attributes a rule set declares, a check on the names of a vocabulary —
needs the origin to tell such a declaration from a written one and to point
to its source, and cannot read it off the name: an author can write the
same words in a name of his own (the golden `jutterswet-wachtgeld` declares
a rule `wachtgeld voor kustwachters`, written, beside defaults groups the
frontend names `<base rule> voor <subtype>`).

The origin is a **side table without semantics**, `layer: core` apart from
the values of `form`, and the field is an **extension field** under the
§ Method's discipline: optional, never null, written whole or not at all. A
declaration carries it in `origin`, on the nodes that carry `designations`
(ch. 2.12) — an ObjectType, an Attribute, a Characteristic, a FactType, a
Role, a Parameter, a named form (the six forms of ch. 3 and a defaults group
of ch. 6.1) and a RuleGroup — written just before `designations`, which
stays the last field of its node. A producer writes it on a declaration it
made itself and on no other. An **Origin** is a record without a node
discriminant:

| Field | Type | Meaning |
|---|---|---|
| `form` | string | the form that made the declaration, from the closed set below (WF-121 (a)); `layer: dialect(regelspraak)` |
| `from` | OriginSource | the declaration it comes from, by name (WF-121 (b) to (d)) |

An **OriginSource** is a reference by name, as every reference of this
format is (ch. 2.1), and likewise a record without a node discriminant:

| Field | Type | Meaning |
|---|---|---|
| `kind` | string | the kind of declaration, from the closed set below (WF-121 (a)) |
| `owner` | string? | the declaration that scopes it: the object type that declares an attribute or a characteristic (not one that inherits it), the fact type of a role, the domain of an enumeration value; absent for every other kind (WF-121 (b)) |
| `name` | string | its canonical name (ch. 2.1) |

The kinds are the designatable kinds of ch. 2.12 and one more:
`objectType`, `attribute`, `characteristic`, `factType`, `role`,
`enumerationValue`, `parameter`, `namedForm` (a form in the one namespace
of WF-020), `ruleGroup`, and `decisionTable`. A decision table of the
RegelSpraak dialect is no declaration on the wire: its rows are, each a
Rule. A `from` of kind `decisionTable` therefore names the table as the
source names it, and no declaration of the document answers it; with the
kind `namedForm` it would name a form that does not exist, or another form
that happens to carry the table's name.

The forms, each `layer: dialect(regelspraak)`:

| `form` | The declarations it makes | `from` |
|---|---|---|
| `inputAboveComputation` | for an attribute X marked as one whose value may be given as input: the attributes `opgegeven X` and `berekende X`, the characteristic `afwijkende opgave X`, the rule `afwijking opgave X`, the consistency `controle opgave X` and the defaults group `opgave boven berekening X` | the attribute X (`attribute`) |
| `exceptionForSubtype` | the defaults group `B voor S` of the blocks written for a subtype S under the base rule B | the form B (`namedForm`) |
| `decisionTableRow` | each rule the rows of a decision table become | the table (`decisionTable`) |
| `stepsAfterSet` | the attribute and the rule `<member> via <role 1>, … en <role n>` of a fold over a set with to-one steps after it | the attribute the steps reach, on the object type that declares it (`attribute`) |
| `startPoint` | the anchor attribute `startpunt <timeline>` of a start-point determination (ch. 3.8) | the rule that writes the anchor (`namedForm`) |

v0 has no origin, so neither the record nor its values have a v0 name; the
RegelSpraak frontend kept the origin in its own model under its own keys
(`opgave-boven-berekening`, `uitzondering-voor-subtype`, `beslistabelrij`,
`stappen-na-de-verzameling`, `startpunt`), into which the Drools backend's IR
loader reads the wire back.

**No semantics.** An origin never resolves anything and decides nothing:
no executor reads it for evaluation, and a document whose `origin` fields
are removed gives every executor the same answer (gate 13 holds ref, clingo
and both Drools branches to that). Every reader reads it structurally; an
executor refuses a `form` or a `kind` outside its closed set at load, in
WF-121 (a)'s sentence without its number, as it refuses a context value
outside WF-098's set, because a reader that keeps the origin cannot hold a
value the set does not name. What a `from` names — whether it resolves,
whether it fits its form — is the verifier's alone (WF-121 (b) to (d)).

**One source.** A `from` names one declaration. Two of the names above are
made of more than one: `B voor S` of the form B and the object type S, and
`<member> via <role 1>, … en <role n>` of the attribute and a role per
step. The origin names the first part (B; the attribute), the declaration
the made one stands in for; the other parts are not on the wire. A list of
the parts beside `from` would be an additive extension of this record; it
is not specified here, and no producer writes one.

A `from` may itself name a declaration that carries an origin: the
attribute `opgegeven X` as the member of a fold with steps, a decision
table's row as the base rule B of a subtype's group. Following `from` from
declaration to declaration ends at a written declaration or at a decision
table; WF-121 (c) refuses a chain that comes back.

Worked example: in the golden `jutterswet-opgave-vormen` the attribute
`waarde` of `Vondst` is marked, and the decision table `stukprijs van een
aanspoelsel` has two rows. The attribute `berekende waarde` of `Vondst`
carries

```json
"origin": {"form": "inputAboveComputation",
           "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}
```

and the rule `stukprijs van een aanspoelsel 1` carries

```json
"origin": {"form": "decisionTableRow",
           "from": {"kind": "decisionTable", "name": "stukprijs van een aanspoelsel"}}
```

The defaults group `waardering`, which the frontend also made but which
carries the name of the base rule the author wrote, carries no origin, and
neither does the group `wachtgeld voor kustwachters`, a member of an
unordered block under the name its author wrote.

- **WF-121** Every origin: (a) its `form` MUST be one of
  `inputAboveComputation`, `exceptionForSubtype`, `decisionTableRow`,
  `stepsAfterSet` and `startPoint`, and its `from.kind` one of the ten kinds
  above, both byte for byte; (b) its `from` MUST carry `owner` exactly when
  the kind is `attribute`, `characteristic`, `role` or `enumerationValue`,
  and, unless the kind is `decisionTable`, MUST name a declaration of that
  kind in the document, under the name identity of ch. 2.1: an object type,
  a fact type, a parameter or a rule group of that name; an attribute or a
  characteristic that the object type `owner` declares itself; a role of the
  fact type `owner`; a value of the enumeration domain `owner`; a named form
  of that name (WF-020); (c) its `from` MUST NOT name its own declaration,
  and following `from` through the declarations that carry an origin MUST
  NOT come back to a declaration already passed; (d) the kind of its `from`
  MUST be the one its form names in the table above: `attribute` under
  `inputAboveComputation` and `stepsAfterSet`, `namedForm` under
  `exceptionForSubtype` and `startPoint`, `decisionTable` under
  `decisionTableRow`. Which declarations a form makes, and on which node, is
  the dialect's and is not checked: a document can carry an origin on a
  declaration whose name or node another form would make.

**Delta (v1 addition, `CONSTRUCTS.md`):** the origin enters as an extension
field (card `herkomst-op-de-draad`, 2026-10-15); the RegelSpraak frontend
writes it on every declaration it made itself, and every other producer
writes none.

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

**The unresolved `regels` stubs left the wire at M1** (2026-09-23, delta B3;
history). The v0 wire carried, beside the resolved rule forms, a top-level
`regels` array of two-field stubs — a `naam` and a `soort`, and nothing
else — the build's own index of the rules it parsed. By maintainer decision
of 2026-09-05 they were declared **non-normative presentation**: nothing in
this specification gave a stub a meaning, no well-formedness rule crossed one,
and a consumer reading one would have been reading something this document
did not define; that status was stated rather than enforced, because a
crossing rule would have fenced a field with no consequence. The measurement
that settled it, over the 522 documents of the four zones on 2026-09-05:
**691** stubs and **zero readers**. Since M1 the array is not written at all,
not even empty. The same stated-not-fenced status holds for `diagnostics`
towards consumers (WF-113 (c), ch. 2).

**Versioning (applies to every *named* form).** Rule, Allocation,
ObjectCreation, FactCreation, Consistency, and Uniqueness all share one
shape: `{node, name, versions}` with `versions` ≥ 1 in declaration order.
The `name` sits on the form (and a rule's `statuteAnnotations`, ch. 3.1,
and the form's `designations`, ch. 2.12, its names in the vocabularies,
written last and on the form, one list for all its versions, with its
`origin`, ch. 2.13, just before them);
everything else — including `validity` and `condition` — sits on each
version, so a version is the form's full body. A version record carries no
`node` of its own: it is part of its form. The field tables in ch. 3.2–3.6
therefore describe *one version's body*; ch. 3.1 shows the pattern in full.
Day-kind definitions, start-point determinations, and rule groups are
unversioned. **Landed at L4** (2026-09-24, delta B19): the wire writes this
shape. A form's `name` never ends in a space, `#` and digits: that is the
spelling v0 gave one version of a form with several, and the readers still
spell a version so in memory (`<name> #1` … `#n` for a form with two or
more versions, the bare name for a form with one), so a form carrying it
would read as a version of another — the schema and every loader refuse it.
*History:* v0 flattened every versioned form into per-version entries under
the `" #i"` name suffix; the L4 migration grouped them back (ch. 3.1).

### 3.1 Rule (assignment and characteristic attribution)

One node covers computing an attribute and attributing a characteristic
(PLAN §9, decision 4). A **Rule** names a target and carries one or more
**versions**:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | rule identity |
| `versions` | list of RuleVersion, ≥ 1 | in declaration order |
| `statuteAnnotations` | list of string? | **extension field** (v0 wire `bronAnnotaties`): the `// Bron:` statute annotations written directly above the rule header, the text after `Bron:` trimmed, in source order — the PLAN §4 traceability thread from a rule back to its wetsartikel. Provenance only, never evaluation-relevant: executors ignore it. When present the list is non-empty with non-empty trimmed entries; a rule without annotations omits the field — written whole or not at all (WF-082). It sits on the form, one list for all its versions (landed at L4, delta B19). A Default carries the same field for the block it comes from (ch. 6.1). **Each entry is opaque:** one string per `// Bron:` line, trimmed, in source order, and nothing more — provenance, which no executor and no verifier reads or parses beyond the two form rules of WF-082 (non-empty, trimmed). What a rule set writes inside it, and any convention it keeps there, is the rule set's own business and not uchar's: uchar splits no entry and knows no structure in one. *History:* v0 flattened versions into separate `ResolvedRegel` entries, so the v0 wire carried the field per flattened entry, stamped from the one comment block above the shared rule header |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the rule's names in the vocabularies (ch. 2.12), on the form |

**RuleVersion**:

| Field | Type | Meaning |
|---|---|---|
| `validity` | Validity | which evaluation dates this version governs (ch. 3.10) |
| `target` | attribute reference | the written member: an attribute, or a characteristic |
| `value` | expression | the computed value; for a characteristic target: the boolean constant true — v0 mirror, the emitter's only form (Phase 4 generalises this into the default model; the ch. 3.3 initialisation form now generalises the characteristic value to a boolean-typed expression, WF-027 — the v1 relaxation the Catala frontend uses) |
| `condition` | Condition? | absent = unconditional |
| `initialisation` | boolean | write-only-if-empty (§9.6) — the defeasible default of `CONSTRUCTS.md` row 7; semantics in `RUNTIME-SEMANTICS.md` ch. 7 |
| `presentation` | (`equation` \| `calculation`)? | dialect tag: which surface form (§13.4.4/.9) produced it; no semantic difference. Written only on a computing version and never beside `initialisation` true; absent is no tag. v0 values: `GELIJKSTELLING` → `equation`, `BEREKENING` → `calculation` |

v0 name: `ResolvedRegel` with `Soort` ∈ {GELIJKSTELLING, BEREKENING,
INITIALISATIE, KENMERKTOEKENNING}. **Delta (tracked, `CONSTRUCTS.md`
row 2), landed at L4 (2026-09-24, delta B19):** v0 flattened a
multi-version rule into separate entries whose names carried a `" #i"`
suffix (1-based); v1 makes versions explicit structure under one name. The
suffix rode *every* rule form built from a versioned rule (allocations,
creations, consistency, uniqueness alike) — not day-kind definitions, start
points, or groups. *Migration (history):* the L4 migration grouped the v0
entries of one list whose names were byte-equal after stripping the
suffix, in the order of `i`; a single-version rule became one version.
**Delta (row 2 / decision 4), landed at L4 (delta B20):** GELIJKSTELLING
and BEREKENING collapse into the `presentation` tag (v0 distinguishes them
only by which surface token was used); INITIALISATIE becomes the
`initialisation` flag; KENMERKTOEKENNING becomes a version with neither,
whose `target` names a characteristic — the "target-kind flag" of
`CONSTRUCTS.md` row 2 is realised as the target's member kind: which
member a version writes is its declaration's, and WF-022 crosses it
against the version's own fields, needing no separate boolean; ch. 3.3's
MemberInit keeps an explicit flag only because its member is a bare name.
The wire writes no rule kind: a reader reads it from the two fields alone,
never from the model — a `presentation` tag is a computation,
`initialisation` true an initialisation, and neither a characteristic
attribution. (v0 marked the attribution with the tag, typed the target
boolean and fixed `value` to the literal true; the value is the typed
literal `{"boolean": true}` since L4.)

- **WF-020** Form names MUST be unique (case-insensitive) across all
  **named forms** in a model — the versioned forms of this chapter and the
  `PrioritizedDefaults` groups of ch. 6.1, in one namespace. A form writes
  its name once for all its versions, so two forms of one name collide
  whatever number of versions each carries. *v0 mirror note (history since
  L4):* uniqueness in v0 was a by-product of the name-suffix scheme;
  nothing checked it. **Delta (promotion).** *Widened 2026-09-12* to the
  defaults groups, which put a name on the wire and were crossed by
  nothing: the sentence already said "across all rule forms", so what grew
  is the list the rule reads and not the property it states. One property
  keeps one number — splitting it would make one collision cite WF-020
  between two rules and a different rule between a rule and a group.
- **WF-107** Every **named form** MUST carry a name that is present and
  non-blank (blank = empty after whitespace collapse, the same
  normalisation WF-020 compares under). The name is the form's IDENTITY on
  this wire and three readings depend on it: ch. 8 groups a form's versions
  by it, the ch. 5.1 dependency graph keys a channel node on it, and a
  regelversie channel test (`RUNTIME-SEMANTICS.md` ch. 5.6) names a rule by
  it. Without one,
  each reader invents an identity of its own — measured 2026-09-12 on a
  nameless `ResolvedRegel`: verifier-clean, evaluated without complaint by
  the reference interpreter, the ASP backend and one Drools branch, and a
  `NullPointerException` out of the other Drools branch's rule-name
  escaper. The wire type stays nullable (`string?` on every named form) and
  the schema is not the enforcement layer: a nameless form is a well-formed
  document that this rule refuses, which is what gives it a witness.
  **What this rule is NOT about.** Three other arrays put a name on this
  wire, and until M1 a fourth did (all four were top-level arrays until M1;
  since M1 the day-kind definitions and the rule groups sit under `rules` —
  `dayKindDefinitions` and `ruleGroups` since L5, `resolvedDagsoorten` and
  `regelGroepen` until then —, `tijdlijnen` is the model list `timelines`,
  and the `regels` stubs left the wire), and the boundary is drawn per array
  rather than left to be inferred (each measured 2026-09-12 over the four
  zones):
  - `dayKindDefinitions` — a **day-kind definition**'s name is which day
    kind it defines and lives in the day-kind namespace, not the named
    forms', so its presence is **WF-031**'s clause, beside that rule's own
    uniqueness and its own dead-definition test.
  - `timelines` — a **timeline**'s name is its identity too, and it is
    already covered: WF-015 keys the declaration on it and every reference
    resolves against it, so a nameless timeline takes every reference to it
    down with it and the document falls on **WF-003** (measured: a nameless
    `het penningjaar` yields `WF-003 FAIL … reference to undeclared timeline
    'penningjaar'`). A second rule here would only re-refuse it.
  - `ruleGroups` — a **rule group**'s name is a label. WF-033 resolves a
    group's MEMBERS by name and nothing reads the group's own, so a nameless
    group is inert rather than ambiguous, and refusing it would be a rule
    without a failure.
  - `regels` — the parse-stage `Regel` **shell index** (ch. 3.1's v0 wart),
    which no check and no executor read at all and which one frontend emitted
    empty by design; it left the wire at M1 (ch. 3). A nameless stub was
    verifier-clean and stayed so, for the same reason: it named nothing that
    anything looked up.

  Beside those three, `dagsoorten` (the model list `dayKinds` since M1) was
  a list of bare strings rather than of nodes until L3, and a `null` entry
  there was likewise admitted — stated as open rather than closed: it is the
  day-kind namespace again, so it belongs with WF-031's family and not here,
  and no producer wrote one. **Closed at L3 (2026-09-24):** `dayKinds` is a
  list of `DayKind` nodes with a required `name` (ch. 2.9), so a nameless or
  `null` day kind cannot be written, and the loaders refuse it (measured
  2026-09-24 on the shared OCaml loader: `expected object, got null` and
  `missing field 'name'`; the Drools loader requires `name` on a `DayKind`).
  **Delta (promotion).**
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
  constant true (the literal `{"boolean": true}`). **The member kind**
  (since L4, delta B20): a version with a `presentation` tag or with
  `initialisation` true writes an attribute, so its `target` MUST name an
  attribute; a version with neither attributes a characteristic, so its
  `target` MUST name a characteristic. A target name that is both an
  attribute and a characteristic of the subject type (its supertypes
  included, under WF-001's identity) is ill-formed: which member a rule
  writes decides its kind, and that name decides nothing. **Who refuses a
  member-kind mismatch** (measured 2026-09-24): the verifier, and the Drools
  loader, which reads the model, at load with the verifier's sentences; the
  shared OCaml loader of `ref` and the ASP backend does not consult the
  model and loads the document, as it does WF-009's duplicate abbreviation
  (ch. 2.5). Below that loader both OCaml executors read the kind from the
  version's fields and fail only where that reading breaks: a tag on a
  characteristic target faults at evaluation as an unknown attribute
  (`wf022-characteristic-target-with-presentation`), and a computed value without tag or
  flag faults on the value clause above
  (`wf022-attribute-target-without-presentation`). A version without tag or flag
  that writes `{"boolean": true}` to a `Boolean` attribute is evaluated
  without any refusal as a characteristic attribution, and the attribute
  stays empty: measured on `jutterswet-borgvlag` with such a version on
  `Berging.geborgd`, both executors answer leeg where the case expects waar.
  Only the verifier and the Drools loader refuse that document. *v0 mirror note:* v0 is fail-soft here too — an
  unresolvable target degrades to an unknown-typed marker and the typing
  check silently skips it; only the *typing* of a resolved target errors
  (that is WF-023). The characteristic-value shape is constructed, not
  checked. **Delta (promotion).**
- **WF-023** Assignment typing, at **every attribute write position** — a
  rule version's `target`, an attribute member among an `ObjectCreation`
  version's `initialisations` (a characteristic member's value is WF-027's
  boolean check), and a
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
  states for the read side: WF-089 crosses what a read hands on (since L2
  the element-passing aggregation; until L2 also a reference's own `type`),
  this crosses what is *written* into the declaration a read reads. It is also what
  makes "labels from the target's domain" mechanically true — a `Literal`'s
  label belongs to the domain the literal itself names (WF-039 against the
  local list, WF-088 for that list against the declaration), so once the
  value's domain **is** the target's, its labels are the target's. The
  cross uses the target's DECLARED datatype, never its serialized `type`:
  a matching pair of lies — target and value both typed with the same
  foreign domain — is exactly the shape a serialized-against-serialized
  slice admits. Since L2 the target carries no serialized `type` at all (B13),
  and a value that is a reference is typed by its declaration
  (`RUNTIME-SEMANTICS.md` ch. 2.10). Deferrals, as elsewhere: a target that
  does not resolve (WF-022), a value whose type nothing declares (an
  unresolved reference, an `Unresolved` of unknown type). The precision gate needs expression-shape analysis and stays
  OPEN — a different question from WF-091's precision facets (ch. 4.1),
  which cross a derived node's annotated decimals and wholeness against
  what its operands derive (until L2 also a reference's annotated decimals
  against its own declaration); this one would cross a computed value
  against its target's declared precision. The **struct twin** of the keuzetype clause is CLOSED (2026-09-03):
  `domains` declares struct domains too, and a `StructLiteral` of domain B
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
- **WF-079** A (type, attribute) target that carries no defaults MUST NOT
  be written by two **unconditional** computing versions of **different
  forms** whose validities overlap. A computing version is one tagged
  `presentation` `equation` or `calculation` (so its `initialisation` is
  false, and WF-022 makes its target an attribute); unconditional means it
  carries no `condition`; two versions belong to different forms when
  their forms' names differ under WF-020's comparison (a version of a
  nameless form, already WF-107's, pairs with every other version, those
  of its own form included); two validities overlap when they share a day
  (ch. 3.10: both bounds inclusive, an absent bound open). The versions of
  one named form never pair here — whether they overlap is WF-021's
  question. Two such versions over-determine the target on the
  days they share, with no explicit priority — the single-writer
  discipline of WF-026/WF-050/WF-052 extended to bare attribute writers
  (`RUNTIME-SEMANTICS.md` ch. 7.7: the desugaring admits at most one
  version-free computing rule per target). Priority is a defaults group (explicit defaults) or `unordered`
  (explicitly absent), never implicit agenda order. **Through a type chain
  the rule is read per concrete type** (ch. 2.2; `RUNTIME-SEMANTICS.md`
  ch. 7.10): a version whose target names an ancestor of T writes T's
  instances too, so the writers of T's attribute are the versions declared
  on T and on its ancestors. Where T's chain — T and its ancestors —
  declares a defaults group on the attribute, the fold of ch. 7.10 makes each
  such version the base of its level and the subtype's base replaces the
  inherited one, so the chain holds one writer; what the fold cannot take it
  refuses by name, and a version beside a group of its own level is
  WF-050's. Where the chain declares none — outside the gate of ch. 7.10 —
  each such version writes T's instances, and two of them declared on two
  types of the chain, one an ancestor of the other, pair exactly as two on
  one type do. The report cites the version on the lower of the two types,
  the first concrete type both write; two versions on one declared type keep
  the report above. Through a chain the priority is a defaults group alone,
  and the chain report names only that: `unordered` there sets a
  declared-unordered set against another level's version, a composition
  ch. 7.10 refuses in every executor. This is the chain-awareness ch. 6.1
  asks of the single-writer rules: whether two writers survive into one
  instance's effective stack. Characteristic
  attributions (a version with neither tag nor flag) are exempt — a monotone
  many-grounds-one-truth — and a guarded case split (each writer carrying
  a `condition`) carries its own priority and stays outside the rule; only
  the bare collision is statically over-determined. *Motivation:* the
  `PHASE5D-DIFF` §8.2 witness — the flat form is un-pinnable in every
  zone (ref oscillates and faults the fixpoint budget, clingo is silently
  multi-valued, so the executors disagree and no corpus zone admits it);
  WF-079 refuses it ahead of the differential square. The chain pair
  behaves the same way (`corpus/negative/wf079-chain-unconditional-writers`): ref
  and the Drools exec-model branch find no fixpoint, clingo derives two
  values, and the DRL-text branch answers by document order. Since
  2026-10-01 all four executors fault such a pair by name where both
  writers apply to one instance (`RUNTIME-SEMANTICS.md` ch. 5.7); WF-079
  keeps refusing the unconditional pair ahead of evaluation. **The
  initialisation arm.** A (type, attribute) target that carries no defaults
  MUST NOT be written by two **initialisation** versions (`initialisation`
  true) of different forms whose validities overlap either — conditional or
  not, on one type or, outside the gate of `RUNTIME-SEMANTICS.md` ch. 7.10,
  on two types of one chain, reported at the lower type's version as above.
  A conditional pair has no case-split reading here: the initialisation's
  guard turns an overlap of the two conditions into an order — in v0 the
  first to fire fills the target and the other finds it filled — and nothing
  ranks the two (`RUNTIME-SEMANTICS.md` ch. 5.4). Measured on verifier-clean
  pairs with both conditions holding: the value followed the document order
  in the reference interpreter and both Drools branches, and the ASP backend
  refused the shape (`corpus/negative/wf079-two-initialisations`,
  `corpus/negative/wf079-chain-two-initialisations`). Inside the gate the
  fold takes the two into one stack on every instance they share, the
  inherited initialisation above the own one (ch. 7.10, block 4), and the
  pair is not reported; an initialisation beside a defaults group of its own
  level is WF-050's. **Delta (promotion).**
- **WF-082** The `statuteAnnotations` extension field of a Rule (v0 wire
  `bronAnnotaties`) and of a Default (ch. 6.1), when present, MUST be
  a non-empty list whose entries are non-empty and carry no leading or
  trailing whitespace; a rule or a default without annotations MUST omit
  the field (absence leaves no trace — an empty or blank form is
  ill-formed). These two form rules are all the verifier checks: the
  content of an entry is opaque, one string per `// Bron:` line, which
  no executor and no verifier reads or parses (ch. 3.1). The emitter
  satisfies the rule by trimming the text after `// Bron:` and dropping
  blank annotations. The verifier reports a violation with the parameter
  `kind` (`rule` or `default`), the node the list stands on. *v0 mirror
  note:* upstream builds the
  annotation list in Phase 0 but the pinned v0 emitter did not put it on
  the wire; the field enters as an extension (Method) with the
  2026-08-25 pin bump. The field on a Default entered on 2026-10-21 (card
  `default-statute-annotations`); until then the RegelSpraak frontend
  joined the annotations of a block into one `source` snippet of the
  default, from which a reader could not recover the strings one by
  one.

The corpus case `corpus/golden/jutterswet-penningdrempel/` instantiates
this node: one rule with one version that carries neither a `presentation`
tag nor `initialisation` true (a characteristic attribution), a
characteristic target, the always-validity, a comparison condition.

### 3.2 Allocation

Distribute a source amount over related recipients. v0 name:
`ResolvedVerdeling`; semantics as recurrence equations in
`RUNTIME-SEMANTICS.md` ch. 4. Versioned per the chapter rule; the table
below (minus `name`) is one version's body.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `sourceType`, `sourceAttribute` | type ref, string | the amount being distributed |
| `targetType`, `targetAttribute` | type ref, string | each recipient's share; `targetType` is the population the allocation ranges over (extensional, ch. 2.2), `targetAttribute` resolves on its effective member set, so it may be declared on an ancestor |
| `method` | `equalShares` \| `proportional` \| `ordered` | §9.7.2 |
| `ratioAttribute` | string? | weight; present iff proportional, or ordered with a proportional tie-break |
| `maximumAttribute` | string? | per-recipient cap (§9.7.3); requires a ratio |
| `roundingDecimals` | integer? | floor each share at n decimals (§9.7.4) |
| `remainderAttribute` | string? | source attribute receiving the undistributed rest (§9.7.5) |
| `orderingAttribute` | string? | ordered only: the sort key |
| `descending` | boolean | ordered only |
| `condition` | Condition? | — |
| `validity` | Validity | — |
| `connectingRole` | RoleBinding | **extension field** (v0 wire `verbindendeRol`), **required since L2 (2026-09-24, B12)**: the connecting relation, with the **recipient role bound** — the bound role's filler is ⊑-compatible with `targetType`, the opposite role's with `sourceType` (WF-047, widened from equality by ch. 2.2). Until L2 it could be absent, and the relation then had to be uniquely derivable from the type pair (WF-048, retired with the bindingless form); `RUNTIME-SEMANTICS.md` ch. 4.1 |

**Delta (tracked, `CONSTRUCTS.md` row 17), landed at L4 (2026-09-24):**
v0's fourth method value `COMPLEX` — the donor's unhandled-form placeholder
— is not admissible IR, and since L4 it cannot be written: `method` is a
closed enum of the three real methods in the schema and in every loader,
so a `COMPLEX` is a load error (`unknown method value "COMPLEX"`). Since L5
the three values carry the spelling of the table.
v0 values: `GELIJKE_DELEN` → `equalShares`, `NAAR_RATO` → `proportional`,
`OP_VOLGORDE` → `ordered`. *History:* until L4 the wire
admitted the placeholder and WF-024 refused it (witness `wf024-complex`,
removed at L4); v0 could also carry ordering fields alongside COMPLEX (set
before the method was demoted), and such hybrids disappeared with the
method value.

- **WF-024** *Retired at L4 (history).* `method` MUST be one of the three
  real methods — since L4 the method enum of the schema and of every loader
  states it, and there is no document left for a rule to refuse.
- **WF-025** Field validity per method: `ratioAttribute` present iff the
  method is proportional or ordered-with-proportional-tie;
  `maximumAttribute` only together with a ratio; `orderingAttribute` (and
  `descending`) only for ordered, and the ordering key's type MUST be
  orderable; share-assignment typing per WF-023 applied to
  target ← source. *v0 mirror note:* v0 enforces the typing as an error
  and demotes the rest to COMPLEX. **Delta (promotion)** for the demotion
  paths.
- **WF-026** At most one allocation MAY write a given (type, attribute)
  target. **Through a type chain the rule is read per concrete type**
  (ch. 2.2): `targetType` is the population an allocation ranges over, so
  an allocation over an ancestor of T writes T's instances too, and no
  stack absorbs an allocation (`RUNTIME-SEMANTICS.md` ch. 7.10 folds
  ordinary rules only). Two allocations whose targets name one attribute on
  two types of one chain, the one an ancestor of the other, therefore both
  write the lower type's instances and pair exactly as two on one type do,
  whatever defaults groups the chain declares: unlike WF-079's chain clause
  (ch. 3.1) this one has no gate. The report cites the version on the lower
  type, the first concrete type both write; the expressible forms are one
  allocation, or the second share written to an attribute the lower type
  declares itself. Witness `corpus/negative/wf026-chain-two-allocations`:
  ref and the Drools exec-model branch find no fixpoint, clingo derives two
  values, and the DRL-text branch answers by document order. *v0 mirror
  note:* two allocations on one target are agenda-order-dependent and v0's
  exec backend refuses them as a principled skip; that skip, ported to the
  Drools backend, keys on the declared type and does not see the chain
  pair. **Delta (promotion, row 17).**
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
| `subject` | type ref | the creating subject: the population the creation runs over and the type every member value evaluates on. WF-034 requires it both to be PRESENT and to name a declared type (ch. 4.6 (n)) — WF-027 crosses `createdType` and stops there |
| `initialisations` | list of MemberInit | declaration order |
| `condition` | Condition? | — |
| `validity` | Validity | — |
| `connectingRole` | RoleBinding? | **extension field** (v0 wire `verbindendeRol`): the created-side back-link role, named explicitly (see below). The wire takes this name since L4, where this row was corrected from the v0 name |

**MemberInit**: `member` (string), `value` (expression), `characteristic`
(boolean — v0 `kenmerk`: true = §13.4.6.4 characteristic-value form, the
member names a declared characteristic and the value is a boolean-typed
expression — a boolean constant is the v0 form; false = §13.4.6.3
attribute-value form). The wire has written it as the node `MemberInit`
since L5; v0 wrote it as the node `Toekenning` (`attribuut`, `waarde`,
`kenmerk`).

The created object's **back-link** relation may be named explicitly
through the extension field `connectingRole` (a RoleBinding per the
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
  non-boolean one; a value whose type nothing declares (an unresolved
  reference, an `Unresolved` of unknown type) cannot be refuted and defers
  to the runtime bounded-write fail-fast (the WF-023 skip). *v0 mirror note:*
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
encoded the creation as bean reference-fields — the field set in the
consequence, the field joined in the condition, and per hop a field and the
side that holds it (until L2 `zetVeld`, `joinVeld`, and per hop
`Tussenstap.type`, `Tussenstap.veld` and `viaOuderVeld`). v1 re-expresses the same content symbolically, and since
L2 (2026-09-24, B7) the symbolic form is the only one on the wire:

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `factType` | fact-type ref | the relation being created |
| `anchorRole`, `targetRole` | role refs into `factType` | which role the anchor fills, which the target |
| `anchor` | type ref | the iterated subject |
| `path` | non-empty list of PathStep | how targets are reached from the anchor; the final step is the join that reaches the target type. The target type is not written: it is the declared filler of `targetRole` in `factType` |
| `condition` | Condition? | guard on anchor or target |
| `validity` | Validity | — |

**The guard is the one position that binds TWO objects.** Everywhere else in
this specification a condition is about the enclosing form's subject; here the
anchor is iterated and the target is reached, and both are in scope. Which of
the two a characteristic check is about is said by its `host` — the
GASTHEER, ch. 4.6 (q2), WF-109 — and by nothing else: an absent owner, or one
the anchor satisfies, guards the ANCHOR and the creation makes nothing for an
anchor that fails it; an owner the target satisfies guards the TARGET, and the
condition then FILTERS the targets rather than gating the anchor. The second
reading is admitted only where the check IS the whole condition, does not
navigate and carries no whole-period qualifier (q2 carries the three fences and
their reasons); nested in a composite, navigating, or qualified, the anchor is
the only bound object. Semantics and worked examples:
`RUNTIME-SEMANTICS.md` ch. 5.5 and ch. 5.8b.

**PathStep**: a RoleBinding (ch. 2.6) — a symbolic hop over an existing
relation, binding **the role the hop's far side fills**; the near side
fills the opposite role, so direction is implied and the earlier
sketch's separate `direction` field is dropped (cardinality is the
bound role's declared `many`). **Landed L2 (2026-09-24, B7):** the
bean fields left the wire. A backend that stores relations as reference
fields derives them from the role bindings by its own role→field mapping:
the derivation moved down into the backend, out of the IR (the Phase-3
forcing move). The symbolic form is normative and required — it is what
executes, and the schema and every loader read `factType`, `anchorRole`,
`targetRole` and each step of `path` as required; a field of the bean form
is an unknown field there. **Landed L4 (2026-09-24, delta B19):** the node
is `FactCreation` with its versions, and the join chain is the one `path`
— a step is the RoleBinding itself, with no node around it, and the last
step is the hop that reaches the target; the target type left the wire,
because the declaration of `targetRole` names it. *History:* until L4 the
v0 names rode the wire (`feittype`, `ankerRol`, `doelRol` and `anker`, the
intermediate steps as `tussenstappen` of `Tussenstap` nodes and the final
hop as `joinRol`), and the target type `doel` beside them, which the L4
migration checked against the filler of `doelRol` on every document
before dropping it. *(History until L2:)* the bean fields rode the wire beside the symbolic
ones as a superseded MIRROR, which a consumer could read in addition,
never instead; WF-028's bean-field-only arm made that checkable, and
WF-034 (m) fenced the mirror — all four bean fields present, naming a
declared role of the relation their symbolic sister names, on the side
the bean model stores the reference, spelled as declared — because all
four were verifier-clean under a nonsense name while the bean-model
executor crashed on each. Both arms and their eight witnesses left with
the fields (`corpus/negative/README.md`, L2).

The symbolic content rides the wire on each `FactCreation` version:
`factType` (the created relation), `anchorRole` and `targetRole` (the roles
its two ends fill, by declared name), and `path` (the RoleBinding of every
hop, the last reaching the target), all required. The join chain reads:
anchor → the steps of `path` in order → target, and the target type is the
declared filler of `targetRole`.

- **WF-028** `factType` resolves to a declared
  two-role fact type; `anchorRole` and `targetRole` are distinct roles of
  it, the anchor filling the first; every hop's binding
  resolves (ch. 2.6) with its near side matching the chain's current
  type; the chain MUST reach the target type — the declared filler of
  `targetRole`, which is what a reader takes the target type to be, and
  which is left undetermined (reported here) where the role
  does not resolve; and the created relation MUST differ from the relation
  of the final hop. *(History until L2,
  2026-09-24: the rule also demanded the symbolic form all-or-nothing, and
  refused a **bean-field-only** record — every symbolic key absent. Since
  L2 the symbolic block is required by the schema and every loader (B7),
  so both arms are enforced at load and their witnesses
  `wf028-bean-only-creation` and `wf028-half-symbolic-creation` were
  removed with reason. Closed 2026-09-04, fifth wave;
  until then it was recorded as "uncheckable, its relational content is
  not symbolic on the wire", which inverts the rule: the content is not
  there, and that IS the defect. Measured on
  `corpus/golden/jutterswet-postwacht` with all five symbolic keys
  removed, the three executors disagree about whether the document
  exists — `ref` and the ASP backend refuse it by name ("carries only
  bean fields; the symbolic form is what executes") while the Drools
  backend replays it to the pinned expectations, because the bean fields
  are exactly what it reads. A verifier that admits it makes the IR's
  meaning depend on which engine reads it. Witness
  `wf028-bean-only-creation`; over the four zones no document loses
  anything — measured at the pre-card state, all 8 fact creations in
  golden, red, negative and the parity zone carried the symbolic form,
  and of the 16 those zones hold since 15 do, the sixteenth being that
  witness.)* *v0 mirror note:* v0
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
  harmless (idempotent links; both executors agree). A writer is a
  **form**: the versions of one object creation are one writer — version
  selection is temporal (`RUNTIME-SEMANTICS.md` ch. 7.4), so two versions
  never co-apply, and whether they overlap is WF-021's question, as the
  versions of one named form never pair under WF-079. A version of a
  nameless form, already WF-107's, is a writer of its own, as it pairs
  with every other version under WF-079. Two DIFFERENT
  creations on one to-one role stay refused. *(Since 2026-09-25; until
  then each version counted as a writer, and two disjoint versions of one
  creation were refused.)*

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
| `source` | source reference? | the version's source text and, where the producer reports one, its source line — `snippet` and `line` of the structured source reference (ch. 4). On the version since L4; the record since L5, where the v0 fields `bron` and `line` went into it (`line` only beside a `bron` string, and dropped where `bron` was null) |

- **WF-029** The assertion MUST have a single subject type: the version's
  `subject` is present and declared. *v0 mirror
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
| `scope` | navigation ref? | absent = global; present = unique per scope owner. Only on a version with one source: a version with two or more carries none (the scoped union is an open design question, below) |
| `validity` | Validity | — |
| `source` | source reference? | the version's source text, the `snippet` of the structured source reference (ch. 4). On the version since L4; the record since L5 (v0 `bron`) |

**Source**: `objectType` (ref), `keys` (list of attribute names, ≥ 1 — a
multi-attribute key is jointly unique by positional concatenation, §5.7).
Semantics note (mirrored): an empty key value never collides (§8.1.6).

**Delta (row 20):** v0's bean reference-field name for the scope is
replaced by a symbolic scope navigation (role reference), same move as
ch. 3.4; a backend that stores the scope as a field derives it via its own
role→field mapping. **Landed (2026-08-18):** the symbolic scope rides the
wire as the `scopeRol` extension field (`scope` since L4; a RoleBinding whose bound role
reaches the scope parent), and both uniqueness forms execute in both
executors: the union as one shared key domain across all sources, the
scoped form per scope parent, with the v0 narrowing that scope-less
instances do not participate mirrored and recorded
(`RUNTIME-SEMANTICS.md` ch. 5.9). The emitter produces `scopeRol` since
the 2026-08-18 pin bump (toolchain `fe46e49`). **Landed L2 (2026-09-24,
B8):** the written bean-field name of the scope, `scopeVeld`, left the wire, and
`scopeRol` IS the scoping — a uniqueness carrying it is scoped, one
without it is not. **WF-054:** the scope binding resolves (WF-047's
resolution: the declared role name binds), its near side is filled by the
uniqueness subject, and the scope role is to-one (a scope parent is one
instance). The one golden scope that was bindingless
(`anchor/jutterswet-kustregister`, its second uniqueness) got its
`scopeRol` from the migration. *(History until L2:)* the written field
had to denote the bound role, a bindingless scope resolved by role name
or plural from the subject alone, and a binding on an unscoped uniqueness
was ill-formed; the last cannot be written since L2 and its witness
`wf054-binding-on-unscoped` was removed with reason. A scoped union is not
representable in v0 and stays out.

**Landed L4 (2026-09-24, delta B27):** the two v0 forms are one
`Uniqueness` node whose versions carry a list of `sources`, each a
`Source` node; the v0 list `uniciteitUnies` left the wire, and every
uniqueness stands in the one list `uniquenesses` (`uniciteiten` until L5;
the migration and the
emitters write the single-source ones first, then the unions, each in v0
order; the order carries no meaning). A version with two or more sources
carries no `scope`: the scoped union is an open design question, and the
schema and every loader refuse it. *History:* until L4 the single-type
form was `ResolvedUniciteit` (`subject`, `sleutels`, `scopeRol`) and the
union `ResolvedUniciteitUnie` (a list of `Bron` nodes with `type` and
`sleutels`), and WF-030 demanded two or more sources of a union; a union of
one source is the single-type form since L4, and that arm's witness
`wf030-single-source-union` was removed with reason.

- **WF-030** Every source MUST name a declared object type
  (`objectType`) and its keys MUST resolve to declared attributes of it;
  all sources of a version MUST agree on key arity and per-position
  datatype kind
  (including unit, for numeric keys). *v0 mirror note:* v0 checks
  arity/kind for the union form (warn-and-drop) and nothing for the
  single form. **Delta (promotion).** **OPEN** (`CONSTRUCTS.md` row 20):
  whether a time-dependent attribute is admissible as a key — v0's
  backends decline temporal keys; the spec must decide, not inherit the
  refusal.

### 3.7 Day-kind definition

The predicate that defines a declared DayKind (model layer, ch. 2.9): a
condition over "the day". v0 name: `ResolvedDagsoort`.
A **DayKindDefinition** node carries it on the wire (since L5), in the list
`dayKindDefinitions` under `rules` (ch. 2).

| Field | Type | Meaning |
|---|---|---|
| `dayKind` | day-kind ref | the declared name (ch. 2.9) |
| `condition` | Condition | over the day; **pure** (WF-031) and may reference other day kinds **acyclically** (WF-085) |
| `validity` | Validity | MUST be always (neither bound written); no evaluator reads a day-kind window, so a bounded one is refused (WF-031) |

- **WF-031** The reference MUST be present, non-blank, and resolve to a
  declared DayKind (*presence added 2026-09-12* — the name IS which day kind
  is being defined, so an absent one defines nothing and is dead in exactly
  the way a bounded window below is dead; it slipped through the resolution
  cross itself, which had nothing to compare. It is deliberately not
  WF-107's: that rule holds of the NAMED FORMS, whose names share the one
  namespace WF-020 keeps unique, and a day kind's name lives in its own,
  which this rule keeps unique); the
  condition MUST be present and **pure** — a day-kind condition
  references only dates, date-components, `easterSunday`, literals,
  parameters and other day-kind names, and MUST NOT read an attribute or
  a characteristic (the stratum-0 framing, ch. 5.4: a day kind classifies a
  bare date before the rule graph, with no fact base); the bound day of
  the definition itself (**`BoundDay`**, until L4 the context value
  `de dag`) is admitted, and a `ContextValue` naming a **session** value
  (`evaluationDate`, `evaluationYear`) is refused,
  because the predicate is evaluated per calendar day
  (`RUNTIME-SEMANTICS.md` ch. 3.8) and a session-dependent one would
  classify the same day differently in two runs of the same document
  (*position arm added 2026-09-19*: WF-098 closes the NAME set at this
  node and deliberately says nothing about position, and until the
  duration forms executed there was nothing for a position rule to be
  true of — measured 2026-09-10, a `Rekendatum` swapped into
  `jutterswet-schouwrooster`'s first day-kind comparison left the
  verifier and both executors green on the untouched values. The arm
  names the two session values and nothing else: a name OUTSIDE the
  closed set stays WF-098's alone, and a context value without a name
  cannot be written since L4); there MUST be
  exactly one definition per DayKind; the validity MUST be always —
  `from` and `to` both absent (a bounded window is refused, since no
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
  condition may reference other day kinds (a `Predicate` of kind
  `dayKind`), which builds an edge from the defined day kind to each day
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
A **StartPointDetermination** node carries it on the wire (since L5), in the
list `startPointDeterminations` under `rules` (ch. 2).

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
`ResolvedRegelGroep`. The wire has written it as a `RuleGroup` node since
L5, in the list `ruleGroups` under `rules` (ch. 2).

| Field | Type | Meaning |
|---|---|---|
| `name` | string | — |
| `recursive` | boolean | the §9.10 marker |
| `members` | list of rule names | written order |
| `source` | source reference? | the group's source text and, where the producer reports one, its source line — `snippet` and `line` of the structured source reference (ch. 4). Since L5; the v0 fields `bron` and `line` went into it (`line` only beside a `bron` string, and dropped where `bron` was null) |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the group's names in the vocabularies (ch. 2.12) |

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
  unguarded object creation in a recursive group cannot terminate) — every
  version of it, a guard being a property of a version (since
  2026-09-25; until then the check read the first version of the named
  form only). A
  relation-creation member needs no guard: it mints nothing — its writes
  are idempotent links over the fixed universe, so its fixpoint is the
  transitive closure and termination is structural
  (`RUNTIME-SEMANTICS.md` ch. 5.5). The creation budget of
  `RUNTIME-SEMANTICS.md` ch. 5.5 does not bound it: the norm sets no
  bound on the length of a closure (since 2026-10-09; until then the
  reference interpreter's pass budget cut a closure of 100+ hops where
  the ASP grounding completed it). An executor's own named resource
  bound, which is no norm, can still end one: the Drools backend fires a
  case at most 100 000 times, so a closure that needs more firings ends
  there without a fixpoint, reported as such, where the reference
  interpreter and the ASP backend complete it (`RUNTIME-SEMANTICS.md`
  ch. 5.5, Resource bounds). *v0 mirror note:* containment is a
  v0 error; the unguarded case only warns. **Delta (promotion)** for the
  guard.

### 3.10 Validity

The shared validity-window value (v0 name: `Geldigheid`): `from` (date?)
and `to` (date?), both inclusive, an absent bound open; both absent =
always. Year shorthands ("vanaf 2024" = from 2024-01-01) are frontend
sugar, widened before the IR. Selection semantics (which version governs
an evaluation date) are `RUNTIME-SEMANTICS.md` ch. 8. A present bound
carries the strict ISO date lexeme and a real calendar day — **WF-101**
(ch. 4.1), the rule that makes the verifier's raw-string comparison sound (the
executors parse the bounds since 2026-09-23); WF-021 adds the cross-version constraint on top
of it. *v0 mirror note:* a malformed bound is a frontend error — v0 skips
the whole version, so a Validity in emitted IR is always well-formed. That
is a promise about one PRODUCER, never a law of the wire, and until
WF-101 (2026-09-11) no reader held it: a bound reading `niet-een-datum`
was a well-formed document for the verifier, the reference interpreter and
the ASP backend alike.

Every version of a named form writes its `validity` since L4, and since L5
that is a `Validity` node; always is the node with neither bound. *History:*
until L4 the v0 spelling of always on a rule or a consistency was
`geldigheid: null`, which the L4 migration wrote as a `Geldigheid` node with
both bounds null; since L5 the node is `Validity` and an open bound is
absent rather than null.

---

## 4. The expression and condition layer

Expressions compute typed values; conditions decide. Types are the ch. 2.4
datatypes; evaluation semantics — emptiness, exact arithmetic, rounding —
are `RUNTIME-SEMANTICS.md` ch. 1–2, whose worked examples bind the meaning
of every operator named here. Layer marks: core, with exactly five inline
dialect exceptions — the date-builtin library and the three dialect
predicate kinds (ch. 4.3/4.5), the abs-division operator (ch. 4.2), the
dimension forms `DimensionCellRead` and `dimensionSelection` (ch. 4.3, row
28), and the whole-period qualifier (ch. 4.5).

**The source reference.** A node MAY carry a source reference under the key
`source`: the structured source reference (`file, line, column, snippet`)
of the source-anchoring design (`CONSTRUCTS.md` row 3), of which the wire
writes two fields. `snippet` is the source text the node was produced from,
never empty; `line` is its source line, an integer ≥ 1, written only on a
consistency version (ch. 3.5) and on a rule group (ch. 3.9), and only beside
a `snippet`. The record carries no `node` and has no node name. The schema
admits it on the expression and condition nodes of this chapter, on a
`FunctionDef` (ch. 2.11), on the versions of a consistency and a uniqueness
(ch. 3.5, 3.6), on a rule group (ch. 3.9) and on the three nodes of the
defaults layer (ch. 6.1), where it stands in the position the v0 `bron`
string had. A node without source text carries no `source`, and every reader
takes an absent `source` as no source text. An `Unresolved` node (ch. 4.4)
MUST carry one, because WF-113 (b) accounts for the marker through its
snippet. Every reader, the verifier included, refuses an empty snippet at
load with the sentence "a source snippet is never empty: a node without
source text carries no source", under no WF number: the schema requires at
least one character, so no document can carry the defect as a witness.
The schema admits `line` in every source record, so every reader loads it
on whichever node carries it, refuses a value below 1 at load with the
sentence "a source line is at least 1", under no WF number for the same
reason, and drops it where its model has no line number.
`file` and `column` are not written yet: their conventions — the base of
`line` and `column`, the form of `file`, and what `snippet` means beside a
position — belong to the first producer that writes positions, and the
schema admits neither field until then. **Landed at M7 (landing L5, delta
B26):** the v0 `bron` string became `source.snippet`, byte for byte and
whitespace included; a `bron` of `null` became an absent `source`; and the
v0 `line` of a consistency version and of a rule group went into
`source.line` where the node had a `bron` string, and was dropped where it
had none.

### 4.1 Literals and references

| Node | Fields | Notes |
|---|---|---|
| Literal | `value`, `type`, `source` | **Delta (row 27), landed at L4 (2026-09-24, delta B22):** v0 carried a LEXEME as the value — a string the executors parse — and v1 carries a **typed value**: `value` is exactly one variant of the value union of the case format (`uchar-case-v0`) — `{"number": string}`, `{"boolean": true\|false}`, `{"text": string}`, `{"date": string}`, `{"dateTime": string}`, `{"enumeration": string}` or `{"constructor": string}`, the last without a payload (a literal names a constructor, it does not apply one) — and the variant is the one the literal's `type` names (**WF-114**, below). The strings keep the lexemes the rules below pin: WF-081 the number with a point, WF-084 the ISO date, WF-095 the ISO datetime; a `text` is the string BETWEEN the source delimiters (maintainer decision 2026-09-18); an `enumeration` is the BARE label, which WF-039 crosses byte-exactly against the declared value list. The unparsed source slice lives in `source.snippet` (the v0 `bron` string), which keeps the author's spelling — a decimal comma included — and is where a reader that wants it looks; a literal without source text, where v0 wrote `bron: null`, carries no `source`. `type` is nullable: a typeless value node is WF-034's. *History:* until L4 the value was the lexeme `waarde` (`waar`/`onwaar` for a boolean; a number with either decimal separator, which the L4 migration and the emitter write with a point); the sentence that once stood here — "an enumeratiewaarde keeps its `'…'` delimiters, which readers strip" — described a FRONTEND reader, never the wire (maintainer decision 2026-09-19); and WF-095 admitted the day-first snippet for `DatumTijd` until 2026-09-22 (card `p5-datumtijd-lexeem`) |
| AttributeReference | `attribute`, `subjectType`, `path` (role steps), `source` | **Delta (row 4):** v0 split navigation over a written role name (`navigatieRol`), a written path (`navigatiePad`) and a set flag (`set`), and kept a transpiler-facing subject spelling (`onderwerpBron`); v1 unifies into one explicit path of role steps, with result cardinality derived from the declared role cardinalities. **Landed (2026-08-18):** the symbolic path rides the wire as the `path` extension field (`navigatieBindingen` until L5; ch. 4.1a, WF-053); the anchor `jutterswet-kustwacht` executes all three navigated shapes through both executors. **Landed L2 (2026-09-24, B11, B13):** the written-name fields, the set flag and the reference's own `type` left the wire. A reference carries no type: its type is the declaration it reads (`RUNTIME-SEMANTICS.md` ch. 2.10). A read without `path` is the subject's own read, and its `subjectType` EQUALS the subject type — a supertype of the subject does not suffice (ch. 4.1a, WF-053). **Landed L5:** the node is `AttributeReference` on the wire, with the fields `attribute`, `subjectType` and `path` (v0 `AttribuutRef` with `attribuut`, `objecttype` and `navigatieBindingen`); `subjectType` names the endpoint the attribute lives on, which is the subject type for an own read (ch. 4.1a) |
| ParameterReference | `parameter` (name ref), `source` | the wire form is symbolic (the emitter writes the name) — the "embed the model object" wart is in-memory only. No `type` since L2 (B13): the parameter's declaration is the one source |
| VariableReference | `name`, `source` | a rule-local let-binding (§13.4.2.10); binding is frontend scope resolution; the node survives only as the marker for a variable the resolver could not bind. No `type` since L2 (B13; `CONSTRUCTS.md` row 24). **Refused since 2026-09-25 (WF-034):** the wire serializes no variable binding site — a rule-local variable is a `Let`, read by a `BinderRef` (ch. 4.10) — so the reference resolves against nothing; every executor refuses it at load in the rule's sentence |
| ContextValue | `name` ∈ `evaluationDate`, `evaluationYear`; `type`, `source` | **WF-098 (below):** the name is a CLOSED SET of exactly two — `evaluationDate`, `evaluationYear` — crossed BYTE-EXACTLY, and the `type` is required and repeats the type the name fixes (the type arm, since 2026-09-26). Nothing in the document declares a context value, so this is an enumeration, not a resolution. **Delta (row 25), landed at L4 (2026-09-24, delta B23):** split from v0's `Constante` grab-bag; the pronoun referents that shared that node are surface forms the frontend resolves away, and the bound day of a day-kind definition, which v0 spelled as a context value named `de dag`, is the node `BoundDay` (next row). `evaluationDate` is the ch. 8.2 execution-date constant and the index of the ch. 8.3 vintage read (`RUNTIME-SEMANTICS.md`), typed day-precision; `evaluationYear` is the ch. 8.2 execution year, the calendar year of the evaluation date, typed as a whole number without a unit (`Number`, `whole: true`, `decimals: 0`, no `unit`); every executor carries it since 2026-09-26 (card `rekenjaar-als-waarde`), and the golden `jutterswet-rekenjaar` pins it, where from L4 until then it was admitted and executed by no one. v0 values: `Rekendatum` → `evaluationDate`, `Rekenjaar` → `evaluationYear`; since L5 the two v0 spellings fall outside the set like any other name (WF-098). The converse of the bound day — a SESSION value inside a day-kind condition — is refused by WF-031's position arm (ch. 3.7); WF-098 itself stays flat |
| BoundDay | `type`, `source` | the **bound day** of a day-kind definition (ch. 3.7): the arbitrary day the definition is about, not a session value, typed day-precision. It carries no name. **Landed at L4 (2026-09-24, delta B23):** v0 spelled it as a `Constante` named `de dag`, having no node kind of its own for it; since L4 it has one, and a `ContextValue` named `de dag` falls outside WF-098's set like any other name. Its RUNTIME meaning **landed 2026-09-19** in `RUNTIME-SEMANTICS.md` ch. 3.8, with the day-kind duration forms that bind it — not in ch. 8, which is time SELECTION and owns the session values only: the meaning is stated where the binding is made, and outside a day-kind definition the bound day denotes nothing and every executor faults by name (red case `context-value-de-dag-outside-day-kind`) |

**The reference carries no type (landed L2, 2026-09-24, B13).** The five
reference classes — `AttributeReference`, `ParameterReference`,
`VariableReference`, `DimensionCellRead` and `BinderRef` — carry no `type`
of their own. A reference's type is the declaration it reads: the attribute
on the type the reference names (the subject, the endpoint of a navigation,
the element of a fold's set source), the parameter, the dimensioned
attribute of a cell read, the binder's declared datatype
(`RUNTIME-SEMANTICS.md` ch. 2.10, the consumer clause). A copy that cannot
be serialized cannot drift, so the reference arms of the identity rules
below left the wire with the annotation, and so did their witnesses — each
one removed, or rewritten on the rule its translation falls on
(`corpus/negative/README.md`, L2).

**Domain identity (WF-089, ch. 2.4).** A use site that takes a keuzetype
value from somewhere names the same keuzetype as its source. Since L2 the
read-side use site is the **element-passing aggregation**
(`first`/`last`/`minimum`/`maximum`): it returns a contributor
unchanged, so its own `type` is a use site of its operand's and must name
the same domain. *(History until L2:)* an `AttributeReference`'s or
`ParameterReference`'s `type` was a use site of the declaration it reads —
a repetition of the declaration, never a second opinion, crossed at the
endpoint of a navigated read, at a `DimensionCellRead` and at a fold
source. **WF-090 (ch. 2.4) says the same
sentence for struct domains** over these same use sites — struct domains
are not keuzetypes, so the identity is stated twice under two rule
numbers, not once under a widened term.

**Type identity (WF-091).** The domain is one **facet** of that
sentence; the sentence is stated here whole. Since L2 its use sites are
the derived positions (the next paragraphs): the reference-type arm and
the rule-target arm left the wire with the reference's own `type` (B13).
A derived node's `type` MUST be **identical** to the type its operands
derive — not compatible with it, not convertible to it: identical on
every facet the position's formula claims, up to the case-insensitive
comparison WF-001 fixes for a declared DOMAIN name. The three UNIT names
are compared byte for byte, for the reason the unit paragraph below
gives. The facets are compared in one fixed order,

> **soort → domain → unit → decimals → wholeness**,

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
(struct domains) at every arm (until L2 the soort facet was **WF-078** at
a `BinderRef`, ch. 4.7). Every facet without one is **WF-091**'s, and
names itself in the message. That is what makes the rule extensible: a further
facet on the wire costs a message, not a rule number.

*(History until L2, the reference arm as it stood:)* a use site's
serialized `type` had to be identical to the declaration it takes its
value from, on every facet the datatype carries, and the order ran on to
**sign → residual structure**: the reference arm claimed the whole
datatype, so the sign restriction was a facet of it, and a residual
comparison of the whole datatype stood behind the named facets. No
derived position claims either (the sign exclusion below; the residual
would report exactly the facet a formula declines), so with the
reference arm gone (B13) no comparison reached them — measured: each of
the sixteen places where a derived position hands the engine the list of
facets it does not claim names both — and the verifier removed both
facets in the L2 fix round.

Two clauses of the order were deliberate and load-bearing; the first
still is.

- The **soort facet stands aside wherever either side names a keuzetype
  or a struct domain.** `domains` is ONE namespace, so
  enumeration-versus-sum-type and domain-versus-no-domain are the domain
  facet's own clauses — WF-089's variant and one-sided arms, WF-090's —
  stated and numbered there.
- *(History until L2.)* The **residual facet was the reference arm's
  backstop.** The named facets decided the *diagnosis*; the residual
  comparison — the whole datatype, every declared domain name folded to
  its ci form and the unit names left byte-exact, the same split the unit
  facet makes — decided that nothing escaped. It was not redundant there:
  the domain facet looks *through* a `ListType` element, so a list-typed
  declaration and a reference typed as its element agreed on domain while
  soort had stood aside, and only the residual comparison saw the missing
  layer. A derived position claims less than the whole datatype and never
  ran it; the facet was removed with the reference arm (see above).

**Unit identity is structural, never convertible.** Two units are the
same unit iff their abbreviation, declaring system, canonical unit,
conversion factor and — recursively — the denominator of a composite rate
all agree. Since L3 (2026-09-24, B17 (i)) the factor is the exact rational
`{num, den}` and is compared on both integers; WF-092 (c) holds it in lowest
terms, so one value has one spelling and this comparison cannot tell two
spellings of one factor apart from two factors. The three NAMES are compared
**byte for byte** — an annotation
repeats the unit it states literally, case included — where every
declared domain name in this rule folds to its ci form. Name RESOLUTION
is case-insensitive: that is WF-001's domain, and the domain facet of
this very rule stays ci, pinned by its two golden witnesses. Copy
FIDELITY is another question, and a consumer that reads those three
names literally to pick its conversion diverges silently on an
annotation that differs in case alone. Since L2 the unit names this rule
crosses stand on derived positions: `wf091-plus-unit-case` annotates a
`plus` over two reads of `Jutter.gewicht` (declared `kg`, system
`Gewicht`, canonical `kg`, factor 1) with `KG`, system `GEWICHT`,
canonical `KG`, factor 1, and the unit facet refuses it. *Measured until
L2, on a reference's own copy:* a `KG`/`GEWICHT`/`KG` copy of a
`kg`/`Gewicht`/`kg` declaration, factor 1 on both sides, written into a
target declared in `t` — `ref` read the declaration and answered `3.75`
for an input of `3750.00`, while a third executor whose unit lookup is
case-sensitive found no match, fell back to factor one and answered
`3750.00` (the ASP backend carries no cross-unit write and refused the
form ahead of translation, so it neither confirmed nor contradicted) —
`wf091-attribuutref-eenheid-kast`, removed with reason at L2 when the
reference lost its own `type` (B13). The factor and the rate shape agree
exactly as well, being no names. Convertibility is what WF-035 and WF-023
cross, so a lie *inside* the convertible class is compatible by
construction: `kg` and `t` share a canonical unit and a shape.
*Motivation (measured, the
card):* a `t`-for-`kg` annotation on an `AttributeReference`, a
`BinderRef` or a rule target was verifier-clean; `ref` and the ASP
backend answered from the declaration and a consumer honouring the
annotation rescaled by the factor — a wrong answer nothing refused, in
the one class WF-035 cannot reach. Since L2 that class cannot be written
at those three positions: none of them carries an annotation of its own.

**Arms.** *(History until L2.)* The five REFERENCE use sites WF-089 and WF-090 named —
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
**derived** type over an operand LIST. A `minimum`/`maximum` takes its
decimals as the least operand precision, its wholeness as the conjunction
over the operands, and its sign and unit from operand 0; an
`first`/`last` takes operand 0's type in full. Identity against EVERY
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
from those operands' static types (a reference's is its declaration's, any
other operand's its own annotation) by the ch. 2.3 precision table
and the ch. 2.6 unit rules — in the same facet order, with the same
message shape, and under the same **number-follows-the-facet** rule. It is
the same law: an annotation repeats what is fixed *elsewhere*, and here
*elsewhere* is a formula instead of a declaration.

"The same number" therefore means the same *rule for choosing* the number,
not one number for the whole clause. The DOMAIN facet reports under
**WF-089** (keuzetype domains) and **WF-090** (struct domains) at a composite
exactly as it did at a reference until L2: a `plus` or a `Rounding` annotated with a
keuzetype domain its derivation does not name is refused on WF-089's
one-sided arm (`wf089-composite-domain`). Only the facets that carry no
number of their own — soort, unit, decimals, wholeness — are WF-091's here.

**How far the chain reaches, exactly.** An operand's own type is anchored in
turn — a reference IS its declaration's type since L2, a composite is
crossed against its own operands — and where every operand underneath a
composite is anchored that way, the composite is bound to the declarations
beneath it. That is the good case, not a theorem: the chain reaches only
through operands this law actually crosses, and it **breaks at a
fourth-class operand**.
A division, a power, a percentage-of, a root, an absolute value and the
date/time extractors carry an annotation nothing crosses, so a composite that
reads one consumes a **fabrication** as ground truth — a `dividedBy` over
two unitless operands may be annotated `kg`, and a rounding above it then
derives `kg`, and a `kg` target accepts it, all verifier-clean (measured on a
probe; the shape is admitted, not carried). *Measured 2026-09-04:* the corpus
holds **26** such operand positions over **19** of the 174 golden+red
documents present at that date (176 since) — 23 under a division, one under a division-by-absolute, one under a
power, one under a percentage-of; 12 of them read by a rounding and 14 by an
additive node. The deferral proper cannot be self-served — a value node
carrying no type at all is WF-034-ill-formed, and a reference or an
`Unresolved` whose type nothing declares is reported by its own rule, so a
document cannot switch this cross off by emptying a field —
but a fourth-class annotation is the stronger escape and it stays **open**: it
does not switch the cross off, it feeds it a premise. Closing it means deriving
a result type at those positions (`RUNTIME-SEMANTICS.md` ch. 2.10's fourth
class), which is a decision of its own.

A formula may be **silent on a facet while speaking on the rest**, so each
position states which facets it *claims*. A facet it does not claim is not
crossed, and no further comparison runs after the claimed ones: the
refusal here is exactly the named facets of the row, and unlike the
reference arm the rule had until L2 it is not total by construction. *(History until the L2
fix round:)* the engine carried a residual backstop behind the named
facets, and every derived position switched it off, since it would have
reported the very facet the formula declined to state — and since the
**sign** is claimed nowhere (the first exclusion below), at every derived
position. With the reference arm gone no caller ran either facet, and the
verifier removed both.

| Position | Derived type | Facets claimed |
|---|---|---|
| `Binary` `plus` / `minus` / `reducedBy`, numeric | maxDec; whole iff **both** operands are; the **left** operand's unit. The soort **follows that unit** as a reader reads a `Number` (ch. 2.4): a result whose unit is `%` — a rate whose numerator is `%` among them — is a percentage, any other a number. *(History until L3:)* the soort collapsed to a number, `Percentage + Percentage` derived `number`, and a number was the only annotation the position admitted; since L3 that annotation carries the unit `%` and is itself a percentage | soort, domain, unit, decimals, wholeness |
| `Binary` `plus` / `minus` over a date and a duration | the **date** operand's type, unchanged (the duration carries the calendar unit and contributes nothing to the result type) | soort, domain, unit, decimals, wholeness |
| `Binary` `times` | the ch. 2.6 join: a unitless operand hands the other unit through; a rate times its convertible denominator unit **cancels** to the numerator. The soort follows the unit as it does on the additive row (until L3 it collapsed to a number there too) | soort, domain, unit |
| `Rounding` over a numeric or percentage base | the base's soort, unit and sign, with the node's **own** decimal count (whole iff zero) — ch. 2.4's *facing* precision | soort, domain, unit, decimals, wholeness |
| `Rounding` over any other base | the base's type, unchanged — the fall-through of the row above, and a shape ch. 2.4 does not describe: see *Measured, and open* | soort, domain, unit, decimals, wholeness |
| `Clamp` | the base's type, unchanged (ch. 2.7 returns the base, the minimum or the maximum) | soort, domain, unit, decimals, wholeness |
| `Aggregation` `sum` | operand 0's type, unchanged (§4.6 *Eenheid1*) | soort, domain, unit, decimals, wholeness |
| `Aggregation` `first` / `last` | operand 0's type, unchanged | soort, unit, decimals, wholeness — **not the domain**, which WF-089/WF-090's own arm already crosses at this position, once per operand |
| `Aggregation` `total` | operand 0's type with a rate's **denominator stripped** — but only where the fold *integrates over time*. The scheidslijn is the emitter's own predicate: the **non-temporal set reading** ("het totaal van *x* van alle *elementen*" over a plain attribute) is an alias of the sum, folds over instances and integrates over nothing, so it passes operand 0's type through unchanged; every other reading — the non-set rate read, and the set read over a **temporal** attribute, which is the §7.1 per-element timeline fold — integrates, and there `EUR/jr` totals to `EUR` (§4.20) — and a rate whose numerator is `%` (`%/jr`) totals to `%`, which is still a percentage | soort, domain, unit, decimals, wholeness |
| `Aggregation` `minimum` / `maximum` | the **least** operand precision, the **conjunction** of wholeness, operand 0's sign and unit — over a percentage operand 0 as over a number (since L3 a `%` number is a percentage; a percentage carries no sign) | soort, unit, decimals, wholeness (the domain for the same reason as `first`/`last`) |
| `Aggregation` `count` | whole, zero decimals, unitless — a constant of the operator, reading no operand | soort, domain, unit, decimals, wholeness |
| `Aggregation` `filledCount` | the same constant, on its own row because the engine matches on the KIND: a filled count answers a cardinality whatever unit the folded attribute carries (`RUNTIME-SEMANTICS.md` ch. 3.4c). Added 2026-09-20, and not as bookkeeping — until it existed this was the one aggregation whose node type nothing crossed, so a filled count annotated `kg` into an equally-lying `kg` target was verifier-clean and `ref` played it | soort, domain, unit, decimals, wholeness |

"Unchanged" above is about the *derivation*, never about the wholeness facet:
a pass-through hands on whatever precision form its operand carries, whole or
fractional, and the wholeness facet then crosses that form like any other.

**The operand of an arithmetic fold is a Number.** A `sum` and a `total` add
their contributors, and the pro-rata part (`timeProportionalPart`,
`RUNTIME-SEMANTICS.md` ch. 3.7) scales its base by true days over bucket
days: all three are arithmetic, and all three are typed over a `Number`
alone. Every operand of a `sum` or a `total`, in each of its readings — the
listed sum, the set fold over a plain attribute, the §7.1 per-element fold,
and the subject's own temporal total with or without its gedurende clip
(`RUNTIME-SEMANTICS.md` ch. 3.5, 3.6) — and the base of the pro-rata part
MUST therefore be declared a `Number` (a `%` number included); any other
declaration — a `Date`, `Text`, `Boolean`, `Enumeration`, `SumType`,
`ListType` or `StructType` — is ill-formed under WF-091 (WF-038's numeric
clause, checked here). The `sum` and
`total` rows above pass operand 0's type through, so over a `Date` operand a
`Date` annotation agreed with its derivation and the document was
verifier-clean, while `ref` faulted on the arithmetic (`maal`, `plus`,
"pro-rata base is not numeric") and the ASP translation answered the day
number until it was made to decline the form by name. Over such an operand
there is no derived type: the arm reports at the operand
(`…/arguments/<i>`), once per operand that is not a Number, and where it
reports, the row's identity cross does not run. An operand whose type
nothing declares defers. The report names the fold, the operand and its
type,

> the total of 'bewaarvergoeding per maand' on 'Jutter' folds Date values: a
> total, sum or pro-rata part is arithmetic over a Number, and Date values
> are calendar positions

with `sum` in place of `total` for a sum, `the pro-rata part
(timeProportionalPart) of … scales` for the pro-rata part, and for every type
but a `Date` the tail `and <type> values are not amounts`; an operand that is
neither an attribute nor a parameter reference is named `its operand <i>`, a
parameter operand `the parameter '<name>'`. The executors keep their own
refusals as a second line, with their sentences unchanged. *Measured
2026-09-25*, the verifier with the arm and with its type test switched off,
over golden (250 documents), red (42), negative (433) and the parity zone
(30): exactly nine documents change their report, all nine in the negative
zone, and no other document does.
Five are the witnesses that were red cases until that day and moved there
when the arm made them ill-formed — `wf091-timeline-total-date`,
`wf091-clip-fold-date`, `wf091-pro-rata-date-base`,
`wf091-set-source-total-date`, `wf091-set-source-sum-date` — and four were
added for the clauses those five do not reach: `wf091-set-source-sum-text`
for the tail the other types take, and the three named below. The arm
reads 47 operand positions over 35 goldens (20 `sum`, 17 `total`, 10
pro-rata bases), all Numbers, one of them a `%` number. Every clause has a
witness. The `%` number has the golden `jutterswet-aandelensom`, a set sum
over a percentage attribute, which an arm narrowed to plain numbers
refuses; the `ListType` tail and the two operand names have a negative
each: `wf091-set-source-sum-list-type` (a set sum over a list-typed attribute),
`wf091-sum-date-parameters` and `wf091-sum-date-literals` (a listed sum of
two `Date` parameters and of two date literals).

The `times` row states the join in the two shapes the IR *fixes*. It has a
third and a fourth, and the rule claims neither: ch. 2.5's opaque
**product** of two plain units, whose algebra the spec fixes (a sorted
multiset of atomic factors, no canonical, no factor) but whose wire
*spelling* it does not — an identity cross there would pin an
`abbreviation` and a `system` ch. 2.5 does not require — and the
unspellable rate factor the producing frontend refuses outright
(`RUNTIME-SEMANTICS.md` ch. 2.10). *Measured 2026-09-04:* no `times` node
in any of the four zones carries two unit-bearing operands at all, so the
cancel rule itself rests on one negative.

The `total` row is the one place this law does not compute its own formula
but **mirrors a producer's**. The four sources disagree about the strip and
they do not disagree symmetrically: `ref` and the ASP translator do not
strip at all (both answer operand 0's unit whole), while the RegelSpraak
frontend — since the *set-fold "totaal van"* card — strips on every reading
*except* the non-temporal set one, tested there by one predicate
(`ExpressionBuilder.setRead`: exactly one operand, an attribute reference
read as the set source — since L2 the argument of a fold that carries a
range, ch. 4.3 — over an attribute its object type declares **no** granularity
for). This rule mirrors that predicate rather than restating it,
because it is the shape the emitter can actually produce: an unconditional
strip invented a wire type no executor can compute, the set fold adding
`EUR/mnd` values into an `EUR/mnd` target. *Measured 2026-09-04, and the two
halves are pinned differently.* The **strip itself** is two-sided in one
golden: `jutterswet-tempototaal` carries both readings, and dropping the
strip entirely turns its temporal node red (and `wf091-total-unit`
clean), while making it unconditional turns its set-fold node red. The
**granularity half** of the predicate has no witness in these zones at all —
no `total` in golden, red, negative or the parity zone reads a *temporal*
attribute as a set, so removing that half leaves gate 3 green. It is carried
on the emitter's authority alone: the frontend narrowed its own predicate to
that half against a measured executor divergence on the shape (a set read
over a temporal attribute folds each element's timeline *first*, §7.1, so it
does integrate), and neither this corpus nor the frontend's suite carries the
shape. Where the operand names an object type the document does not
declare, the granularity is unreadable and the arm **defers** — the family's
treatment of an unresolvable declaration; the corpus carries no such
`total` either.

Two exclusions from that table are **measured**, not chosen, and each was
priced before it was taken.

- The **sign** is claimed at no derived position — at none of the eleven,
  without exception. It runs both ways, which is why neither direction can
  be crossed. A derivation mostly *under*-claims a sign restriction
  (`unspecified`) while the document narrows it truthfully: a sum of two
  non-negative operands is non-negative, and — the sharpest case — a clamp
  with a minimum of `0` **is** non-negative whatever its base says. And it
  over-claims in the other direction at the `count` arm, where the formula
  says `nonNegative` and two anchor goldens write `unspecified`. *Measured
  2026-09-04:* **12** documents over golden, red and negative narrow it
  truthfully and **5** widen it; crossing the sign on the additive arm alone
  turns 5 golden+red documents red, at every derived arm 7 (the two extra
  are the `count` widenings), and emptying every exclusion in the table 27.
  The exclusion became total only in fix round 2026-09-04: six arms — clamp,
  rounding, `sum`, `first`/`last`, `total` and a single-operand
  `minimum`/`maximum` — were crossing the sign against the text of this
  bullet. Those six carry **zero** golden+red witnesses, so removing the
  cross left the verifier's report over all four zones byte-identical; what
  it stopped refusing is pinned by a probe rather than by the corpus (the
  clamp-with-a-minimum-of-0 shape above, refused before and clean after).
- The **precision** of a `times` is claimed nowhere either, and its price
  needs both numbers stated, because they are not the same measurement.
  ch. 2.3's rule is "the SUM of the operands' decimals, never whole", and
  the frontend follows it, while this corpus's other producers do not — the
  Catala plugin writes the pinned ch. 2.9 decimal scale instead. *Measured
  2026-09-04, against that rule:* 9 decimals and 9 wholeness divergences
  over the four zones, and crossing it turns **11** golden+red documents
  red (3 on decimals, 8 on wholeness, disjoint). The *implementation* holds
  no sum: with the facets unclaimed it writes a placeholder (zero decimals,
  fractional), and merely un-skipping the two facets against that
  placeholder turns **20** golden+red documents red. 11 is the price of the
  rule; 20 is the price of the placeholder, and only the first is what
  ch. 2.3 costs. *Since 2026-09-25 the wholeness half is claimed,* at no
  price: by then every product in the four zones was annotated fractional
  (0 of 57 whole), because the hand-written documents and the Catala plugin
  had been brought to ch. 2.3 the same day, and claiming it moved not one
  report of their 793 documents. `wf091-times-whole` (two whole operands,
  annotated whole) is its witness. The decimals half stays unclaimed, for
  the reason above.

**What stays uncrossed, and is named.** ch. 2.10's **fourth class** — a
division (`dividedBy`, `dividedByAbs`), a power, a percentage-of, a
root, an absolute value and the seven date/time extractors — derives no
result unit in v0, so this rule claims no unit there. It claims one facet
of two of them since 2026-09-25: a `dividedBy` and a `power` annotated whole
are refused on the wholeness facet, because ch. 2.3 makes a quotient and a
power fractional whatever their operands are (`wf091-division-whole`,
`wf091-power-whole`; 0 of 74 such nodes in the four zones was annotated
whole, and no report moved). Their unit, soort and decimals stay unclaimed,
so the unit set below is unchanged. A duration node's own
type is the operator's parameter (WF-038), not a derived result. `IfThenElse`
and `Let` stay out for the opposite reason: WF-069 and WF-076 already cross
their whole type against their branches and their body, and no node is
crossed twice.

The executor harness of `scripts/check-reftype-derivation.sh` (gate 13) keys
its own set on (node, operator) for the same reason, and the two sets were
**aligned by hand** — they are two independently maintained lists, and no
tripwire compares them today. Where they differ, they differ for a reason and
the difference is measured, not accidental: gate 13's derived set carries no
`count` (a count derives no *unit*, which is all that harness twists, while
this rule claims its whole numeric shape); gate 13 exempts an axis this rule
has no notion of, the DURATION **operand** of a date ± duration node, because
both executors route the calendar unit off that operand's static type; gate 13
twists a `Let`, which this rule leaves to WF-076; and gate 13's prose lists the
fourth-class branches without `dividedByAbs`, which its set-complement in
fact covers and which this rule names. Keeping them aligned is a reviewer's
duty, not a mechanism — and making it one is a card, not a claim to make here.

*Measured, and open.* The pro-rata builtin (`timeProportionalPart`, the
dialect's `tijdsevenredig deel`) is a
pass-through in the frontend and in **no other source** — ch. 2.3 lists no
row for it, and neither executor-side derivation reaches it — and one
golden annotates it with its TARGET's type rather than its base's (two
nodes, `synth2-prorata-breed`). A one-source formula is not a law; it
stays uncrossed and stays written down here. Its BASE is crossed: it MUST be
a Number (the operand of an arithmetic fold, above).

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
red — while the half that is contested stayed open anyway. So the split buys
nothing today and pins something tomorrow. What the corpus does carry is one
node in the contested order (`jutterswet-sleeprest`, `50 min <km>`), and it
annotates the RIGHT unit — siding with the executors against ch. 2.6. Note
that WF-035's adoption clause already reads a unit into that very node: the
unitless side is a LITERAL, so it adopts its sibling's `km`, and under that
reading "the left unit" IS `km` and the four sources agree again. The
genuinely open class is therefore narrower still — a one-sided pair whose
unitless side is not an adopting literal, of which this corpus carries none.
The divergence is a finding, not a choice. **WF-099 (ch. 4.6) JOINS that
class rather than widening it:** since 2026-09-10 a clamp bound is crossed
against its base through the very same predicate, so a one-sided pair is
admitted at a clamp on the same terms and refused on the same terms.
Measured that day, the verifier-gated zones — golden (166), negative (359)
and red (26), 551 documents — carry no such clamp either, while the parity
zone's 30 carry two: the casus dump, which gate 11 keeps out of its
well-formedness leg, and parity slice 24, which that leg holds out on this one
report (since 2026-09-25, the parity well-formedness card; until then no gate
put the zone through the verifier). Admitting the class stays one decision for
WF-023, WF-035, WF-036 and WF-099 together.

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
`Binary` (8 documents), `times` (3) and `sum` (1). At the other eight it is
pinned by a negative and by nothing else, and a negative pins a refusal, never
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
side absent, or a declaration that does not resolve. *(Partly history
since L2, 2026-09-24: the rest of this paragraph records the waves that
closed the empty and unresolvable reference shapes on the L1 wire, in the
v0 names that wire carried. The name-level arms it lists stay in force
unchanged, in the L5 names — a reference naming no `subjectType` or no
`attribute`, a `ParameterReference` naming no `parameter`, a
`DimensionCellRead` whose `attribute` is null or no reference, a
`CharacteristicCheck` naming no `characteristic`, an allocation's name
fields. The type-level
shapes left with their fields: a reference, a fold source and a rule
target carry no `type` of their own since L2 (B13) and the unknown-type
node is no datatype (B16), so the sentinel and absent-type arms name
shapes that cannot be written. Of the arms of ch. 4.6, (m) left with the
bean fields of the fact creation (B7), and (o) and (t) were replaced by
the role arm over `RoleMembership`, which ch. 4.6 states as (o) (B9).)* Since 2026-09-04
the shapes measured IN THESE FOUR WAVES AND THEIR FIX ROUND to let a
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
does after; outside their own negative witnesses the shapes are carried
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
golden + red + negative + parity that PREDATE this card (153 + 22 + 257 +
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
too, and measured the same way: over all **486** documents of golden + red +
negative + parity, 15 reports change and 10 verdicts, the ten being the
card's own new negatiefs — seven in the wave, three more in its fix round,
one per sub-arm; no golden and no red document moves, and five
documents keep their verdict while gaining truthful incidental lines. The
FIFTH wave (2026-09-04) took the same naming class off the expression walk
altogether, to three positions the walk never reaches (ch. 4.6 (m)–(o)): the
four BEAN-field names of a fact creation, each resolving against a declared
role of the relation its symbolic sister names (14 of 14 golden positions
clean before, 0 after — with both OCaml executors blind by construction and
the Drools backend crashing on all four); an object creation's `subject`
against the object-type table (1 of 11 golden creations was still unfenced,
the one without `toekenningen`, and there both executors AGREE on a wrong
answer on an anchor golden); and a near-side role check's `anderTeFiller`
against the role fillers (4 of 4 clean before, 0 after). Zero over-rejection
again, and measured the same way: over all **493** documents of golden + red
+ negative + parity that predate that card (154 + 22 + 287 + 30) not one
verdict and not one report moves, and the seven new negatiefs — six under
WF-034, one under WF-028 — are the only additions (negative zone 287 → 294,
four zones 493 → 500). Its **fix round (2026-09-05)** sharpened (m) where a
lens showed the wave had conceded too much: a bean field must also name the
side the v0 bean model actually derives a reference for (the §13.3.9 to-one
role, where cardinality picks one), must spell it byte-exactly, and must be
present at all — three arms, three witnesses, and again zero over-rejection
over the 500 documents that predate it (negative zone 294 → 297, four zones
500 → 503). The SIXTH wave (2026-09-05) took the REST of the list that
paragraph left, ch. 4.6 (n)–(r): the eight per-member SIDE-TABLE keys of an
object type, each its own arm (an orphan key is a silent semantic change —
it hangs a granularity, plural or dimension on a member that does not exist
and leaves the intended one at its default, and two of those arms were
measured to split `ref` from the ASP backend, one of them on an anchor
golden); a characteristic check's explicit `eigenaar` against the object-type
table (read as the key of a dependency edge, and silently re-attributed to the
subject by the bean-model executor's null-yielding lookup); a unit's `stelsel`
(since L3 `system`) against the declared unit systems (a measured `ref` ≢ clingo divergence: the
ASP restating pass gates on it and switches off in silence); and the two
EMPTY halves the fifth wave passed over, `subject: null` and
`anderTeFiller: null`. Twelve arms, twelve witnesses, and over-rejection
settled the same way: over the 510 documents predating that card not one
verdict moves and exactly one report does (negative zone 303 → 315, four zones
510 → 522), and its fix round of 2026-09-06 — which made the eight side-table
keys byte-exact, see ch. 4.6 (p), a clause M1 removed together with the side
tables — adds one more witness and moves nothing
else over all 523 (negative zone 316, four zones 523). That
is again the list of channels closed **here**, not a claim that no deferral
can be self-served. *Measured, still open* — the residual positions of this
naming class, counted over the 510 four-zone documents that predate the sixth
wave (2026-09-05), so a later count will differ: the sentinel on the 9
fold-/aggregation argument types (the aggregation branch replaces the generic
walk there — its own question, not this descent's);
WF-009's own
question, whether a unit's `abbreviation` is one the
system it names declares, which (r) deliberately leaves alone; and WF-007's
precision-form check, which has the identical blind spot
in containers and is its walk to make, not this rule's. (The unresolved
`regels` **stubs** left this list by maintainer decision on 2026-09-05: they
are declared non-normative presentation, ch. 3, with the measurement that
forced the point recorded there. **`Eenheidstelsel.naam` left it on
2026-09-06**: it was one instance of a CLASS — a keyed declaration's redundant
self-name — and WF-094 (ch. 2.1) closed that class byte-exactly over all
five map-shaped declaration tables at once, with a witness per table, until M1
removed the map keys and with them the class (WF-094 is history since then). Two
things this entry had wrong while it stood here, both corrected there: the
fifth table is `parameters`, not `tijdlijnen` — a list, whose name IS its
identity and has no key to disagree with — and `parameters` is precisely the
table where a one-character disagreement makes a consumer answer with WRONG
VALUES, so the instance recorded here was the one member of the class with no
reader at all. **`dagsoortPerEenheid` left it on 2026-09-09**: (s) closes the
arm this entry asked for — every entry's VALUE names a day kind `dagsoorten`
declares — on the witness the golden `jutterswet-schouwrooster` supplied, one
field of it twisted. What (s) does NOT take is the map's KEY side, and that is
a decision recorded in (s) rather than a residue recorded here. (The registry
and arm (s) with it left the wire at L3, 2026-09-24: a day kind carries its
own spellings since, ch. 2.9 and ch. 4.6 (s).) **The EMPTY
half of the self-name class left it on 2026-09-10**: a keyed declaration whose
`naam` is `null` rather than disagreeing with its key, the (n)/(o) split one
table over, closed by WF-094 clause (d) (ch. 2.1) on all five tables with a
witness each until M1, since when the null form cannot be written (ch. 2.1). It
never stood here for want of a witness — a null one was one edit from any
golden, as WF-094's five recapitalisations were — but because it
split the executors the other way round from (n)/(o), two passing silently and
correctly while the bean-model executor crashed in the identifier contract, and
that shape needed a decision of its own. It got one; what the clause did NOT
also do is settle the two maps inside a unit system, which are recorded there,
not here. **`Constante.naam` left it on 2026-09-10**: WF-098 (ch. 4.1) writes
the ENUMERATION this entry said the position needed — the admissible
context-value names, crossed byte-exactly: three until L4, and two since, when
the bound day became the node `BoundDay` and the field `ContextValue.name` — so
the field that crossed against nothing now crosses against a set the spec
fixes. Two of the three things this entry recorded held up and one did not. It
was right that the ch. 4.1 row failed to name `de dag`, and right that
`Rekenjaar` was carried by no document; the row settled both, the first by
following the wire (until L4 gave the bound day a node of its own) and the
second by admitting a name no executor implemented then (every executor has
carried it since 2026-09-26, above). It was wrong about the count: 13
occurrences over 5 documents was measured before the red document
`duration-reads-van-before-tot` arrived, and the re-measurement of 2026-09-10
finds **14** over **6**. Both figures cover the same set — golden, red,
negative and the parity zone — which this document names two ways ("the four
zones" and "the four zones and the parity zone", the same 577 documents at
2026-09-10); WF-098 and its witnesses spell the set out rather than rely on
either label. What the clause did
NOT take is the EMPTY half, `naam: null`, which is recorded with WF-098 and its
measurement rather than here, and which L4 closed: the name is a required
string since. **The MEMBER half of (q) left it on 2026-09-10**:
whether the type an explicit `eigenaar` names DECLARES the characteristic. It
stood here because it is not a resolution question — the cross was keyed on the
subject, and moving it to the field decides which owner is AUTHORITATIVE — and
that decision was taken in the field's favour, so (q1) writes the cross and
WF-100 carries it. The entry's own words held up: it was a semantics question,
and answering it moved the executors as well as the verifier. What it did not
foresee is that the two OCaml executors would first answer with a REFUSAL
rather than a new value, the object half of the field being a hosting statement
they had no second object to honour. **That half was decided on 2026-09-19**
(ch. 4.6 (q2), WF-109): the field is the GASTHEER, the fact-creation guard
binds two objects, and all four executors read the one it names. Both halves
are recorded in (q1)/(q2) and in `RUNTIME-SEMANTICS.md` ch. 5.8b, not here.
**`VerreRollidmaatschap`'s own filler and back-reference left it on
2026-09-20**: (t) wrote the cross the entry asked for (merged into (o) at
L2, the role arm over `RoleMembership`). It stood here for the
one reason the others did not — not that the rule was unclear but that **no
document in any zone carried the form**, so a clause would have had no witness
and would have been exactly the spec sentence without a machine-checkable
artifact this repo refuses. The card that closed it did not write the clause
first: it wrote the WITNESS first — `corpus/golden/jutterswet-postbezetting`,
the form's first carrier anywhere — and the clause second, which is also why
the two OCaml executors gained a named arm for the form in the same step. The
entry's own words held up in full; what it did not say, because nothing had
measured it, is that both refusals it counted on were BOTTOM SENTENCES of
`scripts/check-red.sh`'s list, so the form was not refused as a form at all.)

**The numeric-literal lexeme (WF-081).** A `Literal` whose value is the
`number` variant MUST carry in it the **number pattern of the case
format** — one grammar for one variant, the string the executors parse:

```
number   ::= integer | decimal | rational
integer  ::= '-'? digits
decimal  ::= '-'? digits '.' digits
rational ::= '-'? digits '/' denom     -- a denominator without a leading zero
denom    ::= [1-9] [0-9]*
digits   ::= [0-9]+
```

Nothing else is a number: no whitespace, no leading `+`, no exponent, no
grouping separators, no decimal comma, no fraction without a leading digit
(`.5`), and — the fence this rule exists for — no unit or currency token
inside the string (`"5 €"` is ill-formed; a unit belongs in the datatype's
`unit`, never in the value). The rule keys on the VARIANT, not on the type:
which variant a `Number` names is WF-114's. Every executor refuses a
`number` string outside this grammar at load with this rule's own sentence
(since the L4 landing's fix round: the shared OCaml loader in its executor
stance and the Drools loader, the same function that writes the verifier's
finding; until then the executors' lexeme readers faulted at evaluation or
translation with sentences of their own, and the Drools branches read `.5`
and `5 €` as 5); WF-081 makes such a document ill-formed at verification
time, ahead of any executor. Scope: the `number` variant only — the `date` and `dateTime`
variants are governed by their own rules below, and a `text` legitimately
carries arbitrary text. *History:* until L4 the rule read the lexeme of a
literal whose TYPE was numeric, after mapping every `,` to `.` (the Dutch
decimal comma was admitted as a decimal separator, never as grouping), and
it admitted a fraction without a leading digit and a denominator with a
leading zero (`decimal ::= '-'? digits? '.' digits`,
`rational ::= '-'? digits '/' digits`, the denominator non-zero). Since L4
the migration and the RegelSpraak emitter write the point and keep the
author's comma in `source`; the narrowing moved no document (no literal
carried the refused forms after the migration), and its witness is
`wf081-decimal-comma`.

**The date-literal lexeme (WF-084).** A `Literal` whose value is the
`date` variant MUST carry in it the **strict ISO date lexeme** the executors
parse (the rule keys on the variant since L4; that a day-precision `Date`
names the `date` variant is WF-114's):

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
ASP backend agreed on, while the prefixed `dd. 31-01-2024` faults. Every
executor refuses a `date` string outside this grammar at load with this
rule's own sentence (since the L4 landing's fix round: the shared OCaml
loader in its executor stance and the Drools loader); WF-084 makes such a
document ill-formed at verification time, ahead of any executor. History:
until that fix round the reference interpreter faulted at evaluation and
the ASP backend at translation, each with a sentence of its own, through
their shared `date_of_iso` reader; the Drools backend's literal reader held
the grammar, matched whole, since 2026-09-23 (card
`datum-lexeem-hele-match` — until then it searched the value for a date,
so `2024-01-2026` was silently 2024-01-20 on both of its transpiler
branches, and it also read the day-first snippet) and answered no value,
naming the unreadable lexeme at a comparison and refusing the rule under
its position's own reason elsewhere. The emitter writes the bare ISO lexeme and keeps the source
snippet in `source`. Scope: the `date` variant; the `dateTime`
variant is WF-095 in the
paragraph below — the same ISO discipline and, since 2026-09-06, the same
normative standing; since 2026-09-22 also the same single spelling, the
snippet living in `source` alone.

**The datetime-literal lexeme (WF-095).** A `Literal` whose value is the
`dateTime` variant (the variant a millisecond-precision `Date` names,
WF-114; `DatumTijd` until L3) MUST carry in it the **ISO datetime lexeme**,
matched WHOLE: the date lexeme above, the separator `T`, and a time of day.

```
datetime ::= date 'T' hh ':' mm ':' ss ( '.' fff )?
hh       ::= digit digit               -- 00–23
mm       ::= digit digit               -- 00–59
ss       ::= digit digit               -- 00–59
fff      ::= digit digit digit         -- milliseconds, exactly three
```

Fixed field widths throughout, `T` as the separator (never a space), the
seconds field always written, no zone suffix and no offset, and a
fractional part that is optional but exactly three digits when present,
read as whole milliseconds. The date half is WF-084's date lexeme,
calendar-valid (29 February only in a leap year).

**Nothing else is a datetime lexeme.** Not a zone suffix, not an offset,
not a space where the lexeme writes `T`, not a one-digit hour, not a
missing seconds field, not a fraction of any width but three, not a lexeme
with text around it — and not the day-first RegelSpraak source snippet
(`15-03-2026 12:00:00.000`, optionally `dd. `-prefixed), which belongs in
`source` and nowhere else on this wire.

*Why the snippet was admitted, and why it no longer is.* From 2026-09-06
to 2026-09-22 this rule admitted the snippet as a SECOND spelling, because
the repository's own emitter wrote it: for a day-precision date it wrote
ISO into the lexeme and the snippet into `bron`, but for a `DatumTijd`
literal it wrote the snippet into both (measured 2026-09-06), so a
single-spelling rule would have made the producer non-conforming. The
emitter now writes the ISO lexeme into the `dateTime` variant of `value`
(`waarde` until L4) and keeps the snippet in `source` (`bron` until L5), symmetric with the date branch (card `p5-datumtijd-lexeem`,
2026-09-22), and the rule narrowed with it. The narrowing moved no
document: counted that day over the four zones plus the parity zone, the
only `DatumTijd` literals anywhere were golden `jutterswet-meldstip`'s two,
both canonical, and the five this rule's negatiefs carry, none of them the
snippet. The executors' literal readers kept reading the snippet as the
same instant for one more day, as tolerance behind the fence; since
2026-09-23 they refused it by name (card `datumtijd-lezers-versmalling`),
each with a sentence of its own, and since the L4 landing's fix round every
executor refuses a `dateTime` string outside this grammar at load with this
rule's own sentence — the shared OCaml loader in its executor stance and
the Drools loader, the same function that writes the verifier's finding. A reader that admits a form no producer
writes and no verifier admits is behaviour with no witness; the frontend
reads the snippet from its own source with a reader of its own, which no
back-end links. The emitter side is held by golden
`jutterswet-sluitstip`, the first `source.rgs` golden with a
millisecond-precision date literal: three source spellings of one instant (the `dd. ` prefix with
one-digit day and month, two-digit, and mixed) re-emit as the one ISO
lexeme through gate 10, and gates 5, 6 and 12 replay it.

The canonical lexeme is not a fresh invention: it is the form the CASE
wire already pins — the `dateTime` value of `uchar-case-v0`, whose
pattern (fixed widths, `T`, the seconds field, an optional fraction of
exactly three digits) gate 4 enforces on every case file — so the two
wires name an instant the same way. What the case wire does not have is
ONE reader: the reference interpreter and the ASP backend share one (the
ASP backend links the interpreter's case module rather than
reimplementing it) and the Drools backend has its own — two case readers,
plus the literal reader, which reads the IR wire.

**Why this is a rule and not a description of what readers happen to do.**
Each field of the grammar was a place where those readers stopped
agreeing. Measured 2026-09-06, one row per form, BEFORE the fences below
and AFTER them:

| form | literal reader | case reader (Drools) | case reader (interpreter + ASP) |
|---|---|---|---|
| `…T12:00:00.1` | no value | **100 ms** → refused | **1 ms** → refused |
| `…T12:00:00.100000` | no value | **100 ms** → refused | **100 000 ms** (a minute and forty seconds later) → refused |
| `…T12:00:00.1234` | no value | **123.4 ms** → refused | **1 234 ms** (1.234 seconds later) → refused |
| `2024-1-31T09:00:00` | no value | refused (a parse crash) → refused, with the form named | **accepted, 09:00 on 31 January** → refused |
| `…T12:00` (no seconds) | no value | **accepted, 12:00:00** → refused | refused → refused, with the position |
| `2026-02-30T12:00:00.000` | no value | refused (a parse exception) → refused, the calendar named | **accepted, 2 March** → refused, `day 30 is out of range` |
| `2026-03-15T25:00:00.000` | no value | refused (a parse exception) → refused, the calendar named | **accepted, 01:00 the NEXT day** → refused, `hour 25 is out of range` |
| `…T12:00:00Z` | no value | refused (a parse exception) → refused, with the form named | refused |

The first three rows are the same three digits read three ways. A zone
suffix was the one form all three refused; the seconds field was not — the
Drools case reader accepted `…T12:00` where the shared reader faulted, so
"the executors refuse a missing seconds field" was false of one of them.
Three digits is the only width every reader agrees on, which is why the
grammar admits no other.

The last three rows say the fence is WIDER than the field widths it
advertises. Field widths alone would admit `2026-02-30` and hour `25`, and
the shared reader's civil-day formula was pure arithmetic with no calendar
in it: day 30 of February simply carried into March and hour 25 into the
next day, so the reader answered an instant nobody wrote — a wrong answer
with no diagnostic, the same class as the fraction rows and reached by a
different door. Both readers now hold the calendar too, and the zone row
shows the other half of the same repair: it was already refused on both
case readers, but the Drools one refused it as a bare parse exception; it
now names the form it expected. A refusal that does not say WHAT it wanted
is a refusal a producer cannot act on.

None of that was reachable through the corpus, and the reason is the
point: gate 4 enforces the schema pattern on every case FILE, and inside
that fence the three readings coincide. **The schema was the only reason
the divergence never showed.** A case reaching a reader another way — a
generator, a hand run, a second case producer — got three answers. Each
reader now holds the fence itself (2026-09-06): the literal reader
refuses outright, and both case readers refuse the form by name, naming
the position or the lexeme. Gate 4 stays; it is the second fence now, not
the only one.

On the IR wire there was no fence at all. The schema typed a literal's
`waarde` as a plain string (or null) with no pattern until L4 (since L4 it
carries the case format's patterns on the `number`, `date` and `dateTime`
variants), and the verifier
accepted a document whose `DatumTijd` literal read
`rommel 2024-01-31 12:00:00Z x` — measured 2026-09-06: schema-valid,
`verifier: PASS`. WF-095 makes such a document ill-formed at verification
time, ahead of any executor, exactly as WF-081 and WF-084 do for their
lexemes. The six negative cases carry one refused field class each
(`wf095-zone-suffix`, `wf095-space-separator`, `wf095-fraction-width`,
`wf095-missing-seconds`, `wf095-surrounding-text`, and since
2026-09-22 `wf095-source-snippet`, the day-first snippet the rule admitted
until then).

The passing side is golden `jutterswet-meldstip`: a millisecond-precision
attribute ordered against a millisecond-precision literal in the canonical
lexeme,
in both directions, at a **sub-day** threshold — a millisecond either
side of it, the instant exactly on it, and the civil day either side, so
neither the ms-of-day alone nor the day alone can answer. All three
executors replay it against the same hand-computed values (gates 5, 6 and
12), which is what a numbered rule here needed and did not have before:
until 2026-09-06 the reference interpreter had no datetime-literal arm at
all (`EVALUATION FAULT: literal of this datatype not implemented`, in
EITHER lexeme) and the ASP backend faulted ahead of it — not only on the
literal, but on a datetime ORDERING carrying no literal at all
(`TRANSLATION FAULT: unscalable datatype in the ASP translation`, where
the interpreter replayed that same document exactly). Both now carry it.
The Drools backend reads both lexemes through one shared reader, so the
spelling no longer decides whether a rule survives lowering — the gap
that made a conforming producer's document lose a whole rule on one
branch and get a silently wrong answer on the other, where an unreadable
datetime fell through to the bare DATE reader and compared against
midnight.

**The empty half, closed at L4.** *History:* `waarde: null` was the empty
half of the same field and a different question (the (n)/(o) precedent of
ch. 4.6), and since L4 it cannot be written — a literal's `value` is a
required variant carrying a string or a boolean, and the loaders refuse
anything else — so the open point below is history. It
was not held open for want of a witness — a null one was one edit from this
rule's own golden — but because its shape was its own: the schema typed
`waarde` as `["string", "null"]`, so a null LEXEME was schema-valid where a
missing `waarde` was refused by gate 2, and this rule's arm read the
present lexeme only. Measured 2026-09-06 on a one-edit twist of
`jutterswet-meldstip`: `verifier: PASS`, and the executors split three
ways below the wire — the reference interpreter faults by name
(`EVALUATION FAULT: missing Literal.waarde in IR node`), the ASP backend
faults by name too (`TRANSLATION FAULT: missing Literal.waarde`), and the
bean-model executor DEREFERENCES it —
`ERROR NullPointerException: Cannot invoke "java.lang.CharSequence.length()"
because "this.text" is null`, the shared datum-tijd reader matching its two
patterns against the value it is handed with no null guard before the
match. Two named refusals and one dereference is not a differential,
so a document nothing on the wire refused reached three readers with three
outcomes — the very shape this rule closes for the non-null half. It was
named here and left open until L4 closed it at the wire.

What this rule deliberately does NOT do: it does not force the emitter to
write the canonical lexeme. That is the remaining open flank, recorded
here so the next producer knows which form to target — the canonical one
is what this rule names first and what a consumer reads.

**The text-literal lexeme (maintainer decision 2026-09-18).** A `Literal`
whose `type` is `Text` carries as its value the PARSED string: what
stands between the source form's delimiters, with the outer `"…"` removed
and every character between them kept verbatim — no unescaping, no
whitespace folding, no case folding. The RegelSpraak string grammar admits
no escape sequence (a `"` always closes the string), so the removal is
total and lossless and an emitted value can never itself contain a `"`.
Unlike WF-081/084/095 this is NOT a well-formedness rule and carries no WF
number: a `Text` literal legitimately carries arbitrary text, so no wire
value can be rejected on its shape — a hand-authored document whose value
carries its own outer quotes stays well-formed, and every executor
compares it VERBATIM (ch. 4.1's raw-string comparison). The rule binds the
PRODUCER: an emitter that writes the delimiter-carrying snippet puts a
second spelling of one text value on the wire, which is what this decision
removed. Its machine-checkable artifacts are the emitter witness (`IrEmitt
erTest.text_literal_lexeme_is_the_parsed_value_snippet_in_source`, gate
10) and gate 11's byte-identical re-emission of the pinned dumps.

**The literal's variant against its type (WF-114).** A `Literal`'s value
variant MUST be the one its `type` names: `number` for a `Number` (a
percentage included), `boolean` for a `Boolean`, `text` for a `Text`,
`date` for a day-precision `Date`, `dateTime` for a millisecond-precision
`Date`, `enumeration` for an `Enumeration`, `constructor` for a `SumType`. A
literal whose `type` is null is WF-034's typeless value node and this rule
does not reach it; a list or struct type names no variant. One datatype
union serves every literal, so the schema cannot cross the two, and the
spelling rules above key on the variant, not on the type: this rule is the
one place where variant and type are crossed. The verifier reports
`literal value is <a|an> <variant> value but its type is <type>: the
variant is the one the type names`, with `an` before `enumeration` and `a`
before every other variant, and `<type>` one of `a Number` (a percentage
included), `a Boolean`, `a Text`, `a day-precision Date`, `a
millisecond-precision Date`, `an Enumeration`, `a SumType`, `a ListType` or
`a StructType`; every executor refuses the same literal at
load with the same sentence — the shared OCaml loader when an executor
loads the document, and the Drools loader — so no executor reads
`{"text": "5"}` under a `Number` as a number. Landed at L4 (2026-09-24,
delta B22). Golden: every golden literal (532 literals in 163 goldens, six
of the seven variants; the `constructor` variant is carried by the
negative `wf036-equality-enumeration-against-sum-type` alone). Negative:
`wf114-literal-variant-against-type` (a `text` variant under a `Number`) and
`wf114-date-variant-against-precision` (a `date` variant under a
millisecond-precision `Date`).

**The enumeration-value lexeme (maintainer decision 2026-09-19).** A
`Literal` whose `type` is an `Enumeration` carries as its value the BARE
label — the text between the source form's `'…'` delimiters, nothing
removed but those two characters. Unlike the text rule above this needs no
new sentence to be normative: **WF-039** already crosses that value
byte-exactly against the declared value list, so a delimited spelling
names no declared value and is ill-formed, and the `type` beside it names
the domain (until L2 **WF-034** refused the unknown sentinel there; since L2
the unknown-type node is no datatype, B16). The wire was therefore never
ambiguous here, and no document on it has ever carried the delimited form.
What the decision corrects is a sentence that used to stand in ch. 4.1's
`Literal` row — "an enumeratiewaarde keeps its `'…'` delimiters, which
readers strip" — which described a FRONTEND reader rather than this wire,
and a PRODUCER that had followed it: the RegelSpraak frontend wrote the
delimited source snippet under the unknown sentinel and so could not emit
the construct in any well-formed shape at all. That was a coverage gap,
not a divergence.

The second half of the rule is what makes the first half producible. An
enumeration literal names a LABEL and not a domain, so which `Enumeration`
its `type` must carry is a property of the literal's POSITION, not of its
lexeme. Two positions supply one, and they are the two the value-membership
rules already read: the **write target**'s declared datatype (WF-023's
keuzetype clause, at each of that rule's three write positions) and the
**other side of a comparison** (the sibling operand's type). The `type` a
producer writes there is the declared domain repeated in full, which is
what WF-088 demands of every use site. Where neither position names an
`Enumeration` there is no domain to derive, and a producer emits an
`Unresolved` marker rather than a guessed domain — a label written under a
domain nobody declared for that position would be a silently wrong answer
where the marker is a visible gap. The machine-checkable artifacts are the
emitter witnesses (`IrEmitterTest.enum_literal_gets_its_domain_of_the_write_target`
and its three siblings, gate 10) and the golden `jutterswet-vondstklasse`,
which carries an enumeration literal in both positions and replays through
ref and clingo against hand-computed values (gates 3, 5 and 6).

**The validity bound's lexeme (WF-101).** A validity window's bounds
(ch. 3.10, the `Validity` node, v0 `Geldigheid`) are each either absent —
the open bound — or
the **strict ISO date lexeme WF-084 names above**, matched by that same
grammar and that same calendar: fixed field widths (four-two-two), single
`-` separators, month 1–12, day 1..days-in-month, leap-aware. Nothing else
is a validity bound. It holds at every carrier of a window on this wire —
rule versions, object creations, distributions, consistency rules,
uniqueness rules and their unions, fact creations, defaults groups, and
day-kind definitions, where WF-031 refuses a bounded window outright, so
that position is unreachable in a conforming document and the rule holds
of it anyway rather than of eight carriers out of nine. Scope is the
PRESENT bound: an absent bound (`null` until L5) is not the empty half of a
field here but the declared way to leave a side open, so it is admitted by
the rule, not merely unreached by it.

**Why this is a rule and not a description of what readers happen to do.**
Until 2026-09-23 three readers held a bound as an **opaque string** and
ordered it with `<=` — this verifier, the reference interpreter and the
ASP backend — and `RUNTIME-SEMANTICS.md` ch. 8's version selection in the
two executors WAS that comparison; since then (card
`ref-geldigheid-datumlezing`) the executors read the evaluation date and
both bounds through the strict date reader and compare day numbers, and
only this verifier's WF-021 overlap test still orders the raw strings.
Lexicographic order IS calendar order, but only inside this lexeme. Until
this rule nothing on the wire demanded the lexeme: the assumption sat in a
comment beside the verifier's overlap test, WF-021 rested on it directly,
and it held of nothing. Measured 2026-09-11 on golden
`jutterswet-vensterheffing` — the LOWER bound of the first of its two dated
rules substituted, nothing else touched, both replayed casus per row (every
row below is that one position; the last row is the one where the position
matters, see under the table). Two independently dated rules, deliberately:
they carry distinct names and write distinct attributes, so what this
document exercises is the WINDOW, not version selection — the versioned
witness, where one name's `#1` and `#2` share a target across adjacent
windows, is `anchor/jutterswet-jutheffing`:

| bound | verifier, before | casus IN the window | casus BEFORE it |
|---|---|---|---|
| `2026-01-01` (the golden) | PASS | 125 | leeg |
| `+012026-01-01` | PASS | 125 | **125** — wrong |
| `-0500-01-01` | PASS | 125 | **125** — wrong |
| `niet-een-datum` | PASS | **leeg** — wrong | leeg |
| `2026-02-30` | PASS | 125 | leeg |

The reference interpreter and the ASP backend agree row for row, so the
differential cannot see any of it. **Not one row faults anywhere**: every
wrong answer above is silent. The mechanism is plain once the strings are
compared as strings — `+` (0x2B) and `-` (0x2D) sort below every digit, so
a sign-prefixed extended year is smaller than every four-digit year and
the window silently opens at the beginning of time; `niet-een-datum`
begins with a letter and sorts above every date, so the window closes over
its own casus.

**Two layers, and only one of them is a shape.** The schema pattern on
these two fields (gate 2) refuses three of the four defective lexemes
above and cannot refuse the fourth: `2026-02-30` is four-two-two digits,
matches the pattern, and even orders correctly — lexicographically it sits
between `2026-02-28` and `2026-03-01`, so no opaque-string reader ever
REPORTS that February has no thirtieth. Whether it moves an ANSWER is the
one place in the table above where the substituted POSITION decides, which
is why that position is named: at the lower bound the window still opens
before the in-window casus and both executors replay the pinned values,
while the same lexeme at the UPPER bound of that version silently shrinks
the window past that casus and both answer leeg where 125 is pinned
(measured 2026-09-11, both positions, neither faulting). Never reported and
only sometimes visible in an answer is precisely what a gated case cannot
be relied on to catch. A calendar is not a shape, which is why the second
layer belongs where WF-084's already is: in the verifier, in the same
predicate, one field deeper than any pattern reaches. The two
negative cases carry one layer each (`wf101-validity-bound-signed-year`,
`wf101-validity-bound-calendar-day`) and fall independently.

**What the rule makes true, and what it leaves standing.** With it, `<=`
on the raw bound is calendar order, so WF-021's overlap test in this
verifier is correct **as written**; since 2026-09-23 the two executors
parse the bounds outright (card `ref-geldigheid-datumlezing`), so rule
and readers close the same gap from both sides. That is the cheapest
available answer to the question this rule was asked, and it is now
measured rather than assumed. Residue, stated rather than hidden: a
document that reaches an executor **without passing the verifier** still
met no fence in the executors until 2026-09-23; since then every executor
parses the lexeme — ref and the ASP backend through the shared validity
gate, the Drools backend on its own threshold — and only this verifier
still reads the raw string. This rule
closes the wire, not the reader; that is the same standing WF-081, WF-084
and WF-095 have, and the same argument the case-wire clamps were set down
with.

Cost: none. Counted over the verifier-gated zones the day the rule landed
(golden 169, negative 364, red 26 — 559 documents): **1814** bound
positions, **1800** of them `null` and **14** carrying a value. Twelve of
those fourteen predate the rule and spell an admissible lexeme verbatim —
`2025-01-01`, `2025-12-31`, `2026-01-01`, `2026-12-31` over five documents
— and the other two are this rule's own witnesses. The parity zone adds
128 positions over 30 documents, all `null`. The rule finds no document it
has to break.

**The context-value name (WF-098).** A `ContextValue` MUST carry as its
`name` one of exactly **two** names, and MUST spell it **byte for byte**:

```
evaluationDate   -- the ch. 8.2 execution date, and the index of the ch. 8.3
                    vintage read (day precision)
evaluationYear   -- the ch. 8.2 execution year: the calendar year of the
                    evaluation date (a whole number without a unit)
```

Nothing else is a context value. Not a third name, not a recapitalisation of
one of these two, not their v0 spellings `Rekendatum` and `Rekenjaar`, which
since L5 fall outside the set like any other name (witness
`wf098-v0-name-evaluation-date`), and not `de dag`: since L4 the bound day of a day-kind
definition is the node `BoundDay` (the table above), so a `ContextValue`
named `de dag` falls outside the set like any other name (witness
`wf098-de-dag-as-context-value`). The verifier reports `context value '<n>'
is not one of the two admissible names evaluationDate, evaluationYear: the
context-value name is a closed set, spelled byte for byte`, and every
executor refuses the same node at load with the same sentence (the shared
OCaml loader when an executor loads the document, and the Drools loader).
*History:* until L4 the node was the v0 `Constante`, its name was `naam`,
and the set held three names, `de dag` the third; until L5 the two names
kept their v0 spelling, `Rekendatum` and `Rekenjaar`, and the measurements
below were taken on that spelling.

**The type the name fixes (WF-098's type arm, since 2026-09-26).** A
`ContextValue` with an admissible name MUST carry as its `type` the type
that name fixes, and nothing else: `evaluationDate` a day-precision `Date`
(`precision: "day"`), `evaluationYear` a `Number` with `whole: true`,
`decimals: 0`, `sign: "unspecified"` and no `unit`. Nothing in the document
declares that type, so the annotation can only repeat it. The verifier
reports `context value '<n>' is annotated with a type other than the one its
name fixes: <the fixed type>`, the fixed type spelled `a day-precision Date`
or `a whole Number with 0 decimals, the sign unspecified and no unit`. The
arm is the verifier's alone: every executor derives the value from the name
(`RUNTIME-SEMANTICS.md` ch. 2.10) and reads the annotation nowhere, so no
executor has anything to refuse. That is also why the arm was needed.
Measured 2026-09-26 on `jutterswet-rekenjaar`, one of its four
`evaluationYear` positions twisted at a time. An annotation with two
decimals (`whole: false, decimals: 2`) passed the verifier before the arm
as the rule value and as either comparison operand; as the subtraction
operand WF-091's derived-type cross caught it (`decimals mismatch: the
minus composite is typed 0 decimals, but the type derived from its
operands is 2 decimals`). At all four positions `ref`, the ASP backend and
both Drools branches still answered the year on both cases. A `Date`
annotation was caught at every position, each time by the rule that reads
the operand there: WF-023 at the rule value (`a date-day value is assigned
to a number target`), WF-035 at the subtraction (`date ± duration: the
duration operand carries no time unit`) and WF-036 at either comparison
(`ordering operand is date-day`). A wrong annotation on `evaluationDate`
is likewise reported by the rule that reads the date where it stands
(WF-049 at a vintage read, WF-038 in a duration builtin). The arm reports
each of these at the node itself, beside whatever the position's own rule
reports, and it is the only report for the three two-decimal positions. Witnesses `wf098-evaluation-year-with-decimals` and
`wf098-evaluation-date-with-time`. Cost: none. Over the golden, red and negative
zones and the parity zone (824 documents, measured 2026-09-26) every
context value with an admissible name carries its fixed type, and adding
the arm moved the verdict of these two witnesses and of no other document.

**This is an enumeration, not a resolution.** No table in the document declares
a context value, so there is nothing for the name to resolve against and
WF-034's class does not reach it: what has to be written here is the closed set
itself, and writing it decides what v1 admits at this node. That is why the
clause carries its own number rather than joining WF-034 — WF-034 crosses a
name against declarations the document carries, this one against a set the
spec fixes.

**Why byte-exact.** The consumers below the wire do not agree about case, and
the disagreement is not between them alone but inside one of them. The
reference interpreter and the ASP backend both compare a normalised name
(whitespace collapsed, lower-cased) and so accept `rekendatum`; the Drools
backend's constant reader compares with `equals` and would not recognise it,
while its day-kind reader compares `de dag` with `equalsIgnoreCase` and would.
Measured 2026-09-10, one character recapitalised on the anchor golden
`jutterswet-penningvintage` (`Rekendatum` → `rekendatum`): `verifier: PASS`,
`ref` PASS on 2 expectations, the ASP backend PASS on the same 2 — three
consumers agreeing on the twisted document, and a fourth that reads the field
differently. The rule takes the byte-exact side and closes the question ON THE
WIRE: with the name pinned at verification time a document carrying
`rekendatum` never reaches an executor, which makes the normalisation inside
the two OCaml consumers unreachable on any conforming document and leaves the
Drools readers as they are. The corpus cannot show that divergence as a
differential — of the three carriers (`evaluationDate` since L5), the anchor golden is
excluded by the Drools loader for an unrelated reason and the other two are red
documents, required to fail — and a purpose-built player would be a document
whose entire content is the defect. Closing the wire is the route taken instead
of adding one, and the residue is stated rather than hidden: a HAND-BUILT
document that skips the verifier still reaches the executors with a
non-canonical name and still splits them. The rule bounds conforming documents,
not every input a runner can be handed.

**Why the set held three until L4.** The row above enumerated `Rekendatum`
and `Rekenjaar` while the wire's commonest context value was `de dag` — measured
2026-09-10, the four zones plus the parity zone hold **14** `Constante` nodes
over **6** documents in exactly **two** spellings, `de dag` (11, in 3
documents) and `Rekendatum` (3, in 3 documents). Mirror-then-diverge decides
that: the wire is the v0 fact, so the row follows it and the set admits `de
dag`. What `de dag` IS remained what ch. 3.7 says — the bound day of a
day-kind definition, not a session value — and giving it a node kind of its own
was a v1 question this clause did not answer. L4 answered it (delta B23): the
bound day is `BoundDay`, and the set holds the two session values since.

The set is **flat**: either of the two names at any expression position the walk
reaches, with no position rule beside it. That flatness had a measured flank —
a `Rekendatum` inside a day-kind CONDITION was verifier-clean (measured
2026-09-10, one field of `jutterswet-schouwrooster` turned), because WF-031's
purity list (ch. 3.7) named dates, date-components, eerste-paasdag, literals,
parameters and day kinds but no context value at all, and its walk read a
`Constante` name-blind. **Closed 2026-09-19, and not here**: the position
rule belongs to WF-031, which owns the day-kind condition, and it became
answerable only when the duration forms started evaluating that condition
per calendar day (`RUNTIME-SEMANTICS.md` ch. 3.8). WF-031's arm admits the
bound day (`BoundDay`, until L4 the context value `de dag`) — it IS that day
— and refuses the two session values, naming them
and nothing else; negative `wf031-session-value-in-day-kind` is the 2026-09-10
mutation turned into a witness, and the arm leaves WF-098's own witness
`wf098-unknown-context-value-name`, whose `de dagX` sits in exactly that
position, reporting WF-098 alone. **This clause stays flat**: what the bound
day means OUTSIDE a day-kind condition is a runtime fault in every executor
(red case `context-value-de-dag-outside-day-kind`) and not yet a static rule —
the mirror arm, recorded and open.

**`evaluationYear` is executed (since 2026-09-26, card `rekenjaar-als-waarde`).**
It denotes the calendar year of the evaluation date, the value
`yearOf(evaluationDate)` has (`RUNTIME-SEMANTICS.md` ch. 8.2). Its type is
fixed here and not declared by the document: `Number` with `whole: true`,
`decimals: 0` and no `unit`, the type the RegelSpraak frontend writes on the
node. The node's `type` annotation repeats that type, and WF-098's type arm
(above) crosses it against the name; what the verifier reported per
position before that arm came, the same day, is measured there. All three
executors carry the name, and the golden
`jutterswet-rekenjaar` pins it at four positions (a rule value, an
arithmetic operand, and a comparison operand on either side) in two
sessions that lie one day and one year apart. *History:* from L4 until that card no document carried it, and
every executor refused it by name in one sentence — the reference
interpreter and the ASP backend at evaluation, the Drools backend before
lowering (until the L4 landing that backend had read it as the year of the
evaluation date). The name stayed in the set instead of being dropped,
because removing it would have made a document ill-formed for using a
construct ch. 8.2 defines; that is why the card could give it a carrier
without changing this rule.

**Observability before the rule: none, in either direction.** Measured
2026-09-10 on the state that predates this clause, one field at a time:
`de dag` → `de dagX` on `jutterswet-schouwrooster` left the verifier, `ref` and
the ASP backend on exactly the untwisted answers (PASS on 7 and 3 expectations
in both executors), because no executor evaluates a day-kind definition today;
`Rekendatum` → `rekendatum` left all three passing as shown above. The one
twist that is loud, `Rekendatum` → `RekendatumX`, is loud with the WRONG
message: `ref` says `expression kind not implemented in this card` and the ASP
backend `expression kind not translated`, which is each one's fall-through, not
a diagnosis about a name. So the wire admitted a name nothing recognised, and
the executors either answered as if nothing had happened or refused without
saying what was wrong.

**The empty half, closed at L4.** Since L4 a context value's `name` is a
required string and the loaders refuse a null, so the open point below is
history. *History:* a `naam` of `null` was schema-valid — the field was typed
`["string", "null"]` — and was the empty half of the same field, the (n)/(o)
precedent of ch. 4.6; this clause read the present name only. Measured
2026-09-10 on a one-edit twist of `anchor/jutterswet-penningvintage`: the
verifier passes it, `ref` and the ASP backend both fall through to their
generic refusal (`expression kind not implemented` / `not translated`), and the
Drools readers compared a null against their literals and reached neither
branch, degrading the operand to a marker. Three fall-throughs and no
dereference — unlike WF-095's null lexeme, which one reader dereferenced — so
the shape was recorded here with its measurement rather than folded in. No
document in any zone carried it.

**The one producer route that emitted a name outside the set was already
ill-formed.**
The RegelSpraak frontend constructs a `Constante` named `hij` for the animate
pronoun, and the resolver normally resolves it away: over the 38 goldens with a
committed source, and the 14 whose source writes `hij`, not one emitted a
`Constante` at all. It could leak — measured 2026-09-10, `indien hij gelijk is
aan hij` emitted two of them with `diagnostics: []` — but it leaked carrying the
UNKNOWN-TYPE SENTINEL ("niet opgelost"), which WF-034's type prologue already
refused at the same node (since L2 the unknown-type node is no datatype at
all, and the emitter refuses to write it, B16). That is structural, not a sample:
the frontend has exactly THREE `Constante` construction sites and the `hij`
one hard-codes the unknown type, so no `hij` reaches the wire well-typed.
This clause therefore cuts no conforming producer route: it adds a second,
name-level line at a position the verifier rejected already, and the
frontend's own pronoun handling is untouched by it. **Since L4 the route ends
at the producer** (delta B23): the emitter writes the frontend's in-memory
`Constante` named `de dag` as `BoundDay` and one named `Rekendatum` or
`Rekenjaar` as a `ContextValue` — since L5 named `evaluationDate` or
`evaluationYear`, while the in-memory names keep their v0 spelling —, and
refuses any other name loudly without
writing a document — measured
2026-09-24, the same `indien hij gelijk is aan hij` added to the source of
`jutterswet-penningrecht` ends the emission with exit 1 on the refusal that
names `hij`.

**Cost: none.** Over the 578 documents of the four zones plus the parity zone
that predate this clause, all 14 context values spell an admissible name
verbatim; the two witnesses are the only documents anywhere that do not. The
before/after sweep over all 580 moves exactly those two verdicts and not one
report line elsewhere. The check reaches every position the wire uses, and it
does so structurally rather than by sample: the arm rides the expression walk,
so it reaches every expression position that walk reaches. Both ends measured
2026-09-10 — all 14 corpus nodes twisted one at a time fire (a day-kind
definition's flat composite, a nested composite, a function argument, the date
index of a temporal read, a `Let` body), and so do **320 of the 329** distinct
expression-slot shapes the corpus exhibits, under an injected context value.
The 9 that do not split into two measured classes: **7** are slots that cannot
structurally carry a `Constante` at all, and **2** are slots where another
rule already rejects the document.

### 4.1a The navigated read (WF-053)

An attribute reference whose value lives off-subject **navigates**: its
`subjectType` is the endpoint the attribute lives on, and the walk from
the subject to that endpoint takes one role hop per step.

**Landed L2 (2026-09-24, B11): one explicit path.** The walk is
carried by **`path`** on the attribute reference (`navigatieBindingen`
until L5) — a list of
RoleBindings (ch. 2.6), one per hop, subject-outward, each with **the
role the hop reaches bound** (the far role; its filler is the type the
walk stands on next) — and by **`navigation`** on the
characteristic check (`navigatieBinding` until L5), its one hop in the same
orientation. Written whole
or not at all, never null, never empty. A reference without bindings is
the subject's **own read**: nothing is derived, and its `subjectType`
EQUALS the subject type (compared in the WF-001 form). Equality and not
⊑: the executors read an own read on the subject itself and never let the
field choose the type, so the field has one correct value, and a
supertype of the subject — which the subject's instances do belong to —
is a second spelling of the same read that one reader could admit and
another refuse (until the L2 fix round `ref` and the ASP backend admitted
a supertype there while the verifier and the Drools backend refused it). A
read of an attribute the subject inherits from a supertype names the
subject type, whose supertype chain declares the attribute (ch. 2.2). One
rule for every reader: the verifier refuses any other `subjectType` under
WF-053, and every executor refuses it with the same sentence. The zero-hop self-reference ("het strandtegoed van de
aanmelder", read on the Jutter who *is* the aanmelder) is such an own
read: the reference denotes the subject and reads its own attribute, and
the written role that named the subject stays in `source`. *(History until
L2:)* the wire carried the walk in three written shapes beside the
bindings — a written role name (possibly a plural, single hop), a chain
of declared role names (multi-hop), or a directly named object type with
no written role (the by-pair shape, derived from the type pair) — and
the characteristic check a written hop; a bindingless step had to be
uniquely derivable by name or by pair, the zero-hop self-reference was a
single bindingless written step that passed a self-denotation test, and a
written step had to denote its hop's bound role. The L2 migration derived
every binding the verifier's derivation found unique and turned the
self-references into own reads; the witnesses of the written-name arms
were removed with reason (`corpus/negative/README.md`, L2).

- **WF-053** A navigated reference MUST resolve its walk: every binding
  resolves (WF-047's resolution: declared fact type, declared role name —
  the plural does not bind — two roles, or one reciprocal), each hop's
  opposite role is filled by the type the walk stands on (the subject for
  hop one; ⊑ through the supertype chain, ch. 2.6), and the last bound
  role's filler is ⊑ the reference's `subjectType` (the endpoint;
  one-sided, since the executors refuse the other direction per
  instance). A role whose declared filler is a strict supertype of the
  endpoint is refused (`wf053-jutterswet-unbound-read-subtype`: the
  role `jutter` filled by `Jutter`, the endpoint its subtype
  `Strandjutter`, which alone declares the member read; since 2026-10-01
  the RegelSpraak resolver refuses that read before the wire, naming the
  member, the filler and the role). A reference WITHOUT bindings is the subject's own read, so
  its `subjectType` MUST EQUAL the subject type (WF-001 form; equality, not
  ⊑): a foreign type there names what only a navigation reaches
  (`wf053-read-fold-operand-unconnected-hop`), and a supertype of the
  subject is refused as well (`wf053-own-read-supertype`) — a read of
  an inherited attribute names the subject type.
  Every hop's reached role MUST be to-one — a to-many step needs an
  aggregation, never a silent first pick (the v0 resolver builds only
  to-one chains; its NAAR_VEEL marker, lifted to well-formedness); since
  L2 no read position consumes a collection, so the clause has no
  exception (until L2 the set flag exempted a read from it, WF-096). A
  fold's source never navigates: bindings on it are ill-formed (its
  relation is the fold's range). The navigated characteristic check
  follows the same rule for its one hop, and the characteristic lives on
  the reached type. Semantics: `RUNTIME-SEMANTICS.md` ch. 5.8 — an
  absent link at any hop reads the operand as leeg; the navigated
  characteristic check on an absent link is false, negated or not.

Emitter discipline (the pin-bump contract): an emitter binds the
**scalar navigated read and the navigated characteristic check only** —
a fold-source argument never carries a `path`, its relation
is the fold's own range (`sourceRole` or `sourcePath`). WF-053 enforces this:
bindings on a fold source are ill-formed.

Dependency discipline (ch. 6.1 of `RUNTIME-SEMANTICS.md`): a navigated
read consumes the link set of every hop relation — the edge that orders
navigation after relation creation — and a navigated characteristic
check reads the characteristic **on the reached type**, negatively when
negated.

### 4.2 Operators

**Binary** (`op`, `left`, `right`, `type`, `source`). The operator set,
with layer marks per `CONSTRUCTS.md` ch. 2.1: `plus`, `minus`, `times`,
`dividedBy`, `power` — core kernel; `reducedBy` — core with its own empty
rule (`RUNTIME-SEMANTICS.md` ch. 1.2; its fate is tied to the row-18
decision); `dividedByAbs` — dialect (a defined collapse point, ch. 2.2
there; `Abs` names the dialect variant); `percentageOf` — sugar over kernel
arithmetic, mirrored as an operator.
v0 values: `PLUS` → `plus`, `MIN` → `minus`, `VERMINDERD_MET` → `reducedBy`,
`MAAL` → `times`, `GEDEELD_DOOR` → `dividedBy`, `GEDEELD_DOOR_ABS` →
`dividedByAbs`, `TOT_DE_MACHT` → `power`, `PERCENTAGE_VAN` → `percentageOf`;
until L5 this chapter spelled them `plus`, `min`, `maal`, `gedeeld-door`,
`tot-de-macht`, `verminderd-met`, `gedeeld-door-abs` and `percentage-van`.
*v0 mirror note:* a typing failure degrades the node's `type`
to the unknown-type node rather than erroring the node away. Since L2 that
node is no datatype (B16): at a derived position like this one the producer
writes `type: null` where it cannot type the node (recorded in the L2
card, `backlog/done/20260923-l2-referentielaag.md`), and WF-034 refuses
such a node in a verified document (a value node without a static type).

**Rounding** (`base`, `mode`, `decimals`, `type`, `source`) — the five
modes of `RUNTIME-SEMANTICS.md` ch. 2.5, `mode` ∈ `floor` (toward −∞),
`ceiling` (toward +∞), `towardZero`, `awayFromZero`, `halfAwayFromZero`;
the type carries the rounded precision (the WF-023/ch. 2.4 gate leans on
this).
v0 values: `NAAR_BENEDEN` → `floor`, `NAAR_BOVEN` → `ceiling`,
`RICHTING_NUL` → `towardZero`, `WEG_VAN_NUL` → `awayFromZero`,
`REKENKUNDIG` → `halfAwayFromZero`.

**Clamp** (`base`, `minimum?`, `maximum?`) — sugar verdict (row 1),
mirrored as a node; at least one bound present; an absent bound is no bound
on that side (a `null` until L5).

**TextComposition** (`parts`: literal chunks and embedded expressions) —
sugar verdict (row 31), mirrored; empty coerces to `""` per
`RUNTIME-SEMANTICS.md` ch. 1.2. Each part is a node: a **LiteralChunk**
(`text`, the chunk's literal text) or an **EmbeddedExpression** (`expr`,
the expression whose value is inserted). In v0 the three nodes were
`TekstReeks` (with `delen`), `Letterlijk` (with `tekst`) and `Ingevoegd`
(with `expr`), the wire names until L5.

### 4.3 Aggregation and builtin calls

**Delta (tracked, `CONSTRUCTS.md` row 1):** v0's single overloaded
`Functie` node splits in v1 into two nodes. **Landed at L4 (2026-09-24,
delta B21):** the wire writes the two nodes, each with only its own fields
(the two tables below); the `kind` decides the node — the eight folds are
an `Aggregation`'s, the seventeen other kinds a `BuiltinCall`'s — and a
kind of the other node is an unknown value of this one. Since L5 the kinds
carry the spelling of the two tables, each table with its v0 values.

**Aggregation** — folds over object instances or dimension cells:

| Field | Meaning |
|---|---|
| `kind` | `sum`, `total`, `count`, `minimum`, `maximum`, `first`, `last`, `filledCount` (the last a uchar dialect kind, `CONSTRUCTS.md` row 1 — see "The filled count" below) |
| `arguments` | one set-valued operand (instance fold), one `InstanceSetUnion` (the §5.7 concatenation, count only — see "The instance-set union" below), or ≥ 2 listed operands (value fold). **The listed form is the VALUE folds' alone since 2026-09-20**: under `count` and `filledCount` it is refused by WF-037, so the §5.7 concatenation reaches the count through the union node and never through this list. **Neither count has a sourceless form since 2026-09-23**: a count of one operand or none MUST carry `elementType` (WF-037), the union being the one exit because its selections carry the population and WF-110 (c) forbids a second one here. **Since 2026-09-25 an `elementType` needs a source**: a fold that carries one carries at least one argument, under every kind, and where it carries one, that argument is an `AttributeReference`, its fold source — a `Literal`, a `Binary`, a `ParameterReference` or any other node is a value or a marker, and a `ListLiteral` a list of values, and none of them is a source of instances (WF-037; a `ListLiteral` under a value fold is refused on its type, WF-091) |
| `elementType` | the instance type ranged over (instance folds) — the POPULATION, extensionally (ch. 2.2): the contributors are the instances the source relation reaches whose type is ⊑ `elementType`, so one it reaches that this type does not cover is not folded. Since WF-047's "agree" became ⊑-compatibility the source role's filler may be a strict SUPERTYPE of it, and reach and population are then genuinely different sets; witness `golden/jutterswet-munttelling` |
| `filter` | Condition? — the die/dat-subselection, evaluated PER CONTRIBUTOR (the element is the condition's subject; a leeg operand fails it, ch. 1.2d; an all-excluding filter leaves an empty source — the sum fallback fires, a count reads 0). **Landed (2026-08-19):** executes in both executors (`jutterswet-strandgoed`, an emitter golden; `RUNTIME-SEMANTICS.md` ch. 3.4a). The reference interpreter carries the full condition set; the ASP backend's corpus-exercised fragment faults loudly on the rest — the standing executor asymmetry, differential-gated over the corpus. The SAME field also carries the §7.1 gedurende timeline clip (on the temporal total, where the element type is the subject itself) — a timeline operation, never an instance subselection. **Landed (2026-08-19, WF-055):** the clip executes in both executors (`jutterswet-getijdenwacht`; `RUNTIME-SEMANTICS.md` ch. 3.6): intersect the full value timeline with the condition's true-periods (an own temporal characteristic, or a per-segment comparison against a scalar bound), then total whole units per clipped segment — floor, never pro-rata; a gap in the condition clips away; a fully clipped-away or absent timeline totals 0, never leeg. The negated clip (the complement) and the Gelijk/Ongelijk comparison clip execute in the reference interpreter only — the ASP side faults loudly on both. A negated characteristic subselection never reaches the wire executable (the producer marks it `Unresolved`; v0 `Onopgelost`) |
| `emptyFallback` | expression? — **sum only**: the §5.8.2 "of … als die er niet zijn" clause |
| `dimensionSelection` | DimensionSelection? — **`sum`/`minimum`/`maximum` only**: fold over an ordered label selection instead of instances (dialect, row 28). The value is a **DimensionSelection** node: `dimension`, the declared dimension it selects on, and `labels`, the selected labels, which resolve against that dimension's declared order (WF-040). v0 wrote it as the node `DimensieSelectie` (`dimensie`, `labels`), the wire name until L5 |
| `sourceRole` | RoleBinding? — **extension field** (v0 wire `bronRol`), instance folds only: the connecting relation, with **the role whose filler instances the fold RANGES OVER bound** — the reach, which `elementType` then narrows to the population (ch. 2.2); the opposite role's filler is the fold's subject type (WF-047, ⊑-compatible since ch. 2.6, so the bound role's filler may be an ancestor of `elementType`). **Since L2 (2026-09-24, B12)** a fold with ONE argument and an `elementType` carries its range — this field or `sourcePath` — under every kind but `total`, where its absence makes the argument the subject's own read; the schema and every loader refuse a bindingless fold elsewhere, so nothing derives a range (until L2 WF-048 demanded a uniquely derivable one). A binding on a node that is no instance fold is ill-formed (WF-047) |
| `sourcePath` | RoleBinding list? — **extension field** (v0 wire `bronPad`), the MULTI-STEP range of a value fold's set source: the relations walked subject-outwards, each step spelled exactly as `sourceRole` is. Two or more steps; the one-step range IS `sourceRole`, and since L2 the schema and every loader admit at most one of the two fields (WF-102) |
| `type`, `source` | — |

v0 values: `SOM` → `sum`, `TOTAAL` → `total`, `AANTAL` → `count`, `MINIMUM` →
`minimum`, `MAXIMUM` → `maximum`, `EERSTE` → `first`, `LAATSTE` → `last`,
`AANTAL_GEVULDE` → `filledCount`.

Empty-source results differ per kind — the som/totaal split and the
no-fallback rule for min/max/first/last are `RUNTIME-SEMANTICS.md`
ch. 1.2, mirrored in the matrix (row on TOTAAL, `CONSTRUCTS.md` ch. 2.1).

**The anchored fold source (WF-083).** An instance fold's source does not
move: it is read at one type, fixed by the fold, and the argument's own
fields are description of that read, never a second way to choose what is
read.

**What a fold SOURCE is.** The rule below speaks about the **single**
argument of an aggregation carrying an `elementType` — the argument that IS
the folded set's source. A fold carrying two or more arguments is the
listed value fold (§5.8.3, `minimum`/`maximum`/`first`/`last` over listed operands); it
has no source at all, its `elementType` is vestigial where the
resolver stamps one, and its operands are ORDINARY READS, evaluated one by
one against the rule subject and governed by WF-053 like every other read.
A builtin has no source either, and since L4 it cannot carry an
`elementType` or a range at all (delta B21; *history:* until L4 the shape
could be written, the range was refused there by WF-047, and its witness
`wf047-bereik-op-builtin` was removed with reason at L4). Everything below
is scoped to the single-argument aggregation, and that scope is normative:
a document whose listed operand navigates is well-formed, pinned by
`corpus/golden/jutterswet-sluitsom` through `ref`, the ASP backend and both
Drools transpiler branches.

**The reading of the source (landed L2, 2026-09-24, B11, B12).** The fold's range
decides it, and nothing else on the argument does. (i) With a range —
`sourceRole` or `sourcePath` — the single argument is the **set source**:
the fold reads it on each instance the range reaches and `elementType`
admits. (ii) Under `count` the set source is the **count source**: it
counts the instances the range reaches, and its `attribute` is the written
name of the role the range binds, which MUST denote that role on declared
name or declared plural (WF-047). (iii) Under the value folds and
`filledCount` the source's `attribute` is read on each contributor and
resolves on `elementType`. (iv) Without a range the argument is the
subject's **own read** — the ch. 3.6 temporal total; the schema and every
loader admit that only under `total` (B12: a single-argument fold with an
`elementType` carries a range under every other kind), and its
`elementType` is then the subject type (WF-083 (c)). The count source is
recognised at the source itself (WF-093 below): its `attribute` names no
member — no attribute and no characteristic — of the element type.
*(History until L2:)* the reading was carried by fields of the argument — a
set flag on the reference, and the unknown-type sentinel on its own `type`
for the count source ("the row-1 wart") — which left the wire with B11 and
B13; the migration translated each fold by exactly the reading those fields
stated.

*The anchor.* The type at which the source is read is the `elementType` for
the set source of a value fold ("het bewaartarief van alle vondsten van de
Jutter": each member of the folded set is asked for the attribute, and the
declaration that answers is the element type's), and the fold **subject**
for the count source (its written role name names the subject's relation)
and for the subject's own read (what the ch. 3.6 temporal total totals).
The argument's `subjectType` MUST name that anchor.

It is not a free annotation, and the precise reason is worth stating,
because the obvious one is false. The field is not ignored: for the own
read the ASP backend's `fold_operand_otype` RETURNS it as the anchor, and
`ref`'s temporal-total arm guards on it — both read it (until L2 the
guard was the name comparison `norm aot = norm self.otype`). What neither
does is let it CHOOSE where the read happens. **It is
a guard, not a selector**: it may only confirm the anchor the fold has
already fixed. A spelling naming a type the subject is no instance of is
refused outright by `ref` (the arm's guard fails and the fold falls
through to its loud fault) or resolved by the ASP backend against a
declaration on a type the fold never walks; this rule compares the names
(equality, WF-001 form), so it refuses a supertype of the subject as
well. At
the set source the field is inert in both — `ref`'s set-source arm binds no
`subjectType` at all and every ASP read hangs on the element variable —
which is not a reason to leave it free but the reason to pin it: a field
nobody reads is a field that can say anything.

*The written role (history until L2).* The written form "het aantal
`<rol>` van de `<onderwerp>`" may name the fold subject by its OWN role
name ("het aantal vondsten van de aanmelder", evaluated on the Jutter who
*is* the aanmelder). Until L2 the wire carried that written role on the
fold source, admissible only on the count source and only when it denoted
the fold subject; any other written role on a fold source was navigation,
which a fold source never does. The field left the wire with L2 (B11): the
form is a count source read on its subject like every other, and the
written role stays in `source`.

- **WF-083** A fold SOURCE — the single argument of an aggregation carrying
  an `elementType` — is ANCHORED and MUST NOT navigate. A fold with two or
  more arguments is the listed value fold (§5.8.3); its operands are
  ordinary reads governed by WF-053, and this rule says nothing about
  them. (a) and (b) are history since L2 (2026-09-24): they refused a
  written navigation path on a fold source, and a written role outside the
  count source or not denoting the fold subject; both fields left the wire
  (B11), and so did the witnesses of (b) (`corpus/negative/README.md`,
  L2). Navigation bindings on a fold source are WF-053's (the fold's
  relation is its range). (c) The fold source's `subjectType` MUST name the
  fold's anchor, per position (the reading of the source above): the
  `elementType` for the set source of a value fold, the fold subject for
  the count source and for the subject's own read (a `total` without a
  range). Where WF-093 refuses the reading itself — a count source under a
  value fold, a member read under `count` — the anchor is not crossed, so
  one defect is reported under one rule. For the subject's own read the
  node's own `elementType` MUST name the fold subject too (equality, WF-001
  form; since the L2 fix round): without a range the fold totals the
  subject's own timeline, so a foreign type in the field names instances
  the fold does not fold over (measured before the clause:
  verifier-clean, every answer unchanged; since the fix round every
  executor refuses it with the verifier's sentence).
  Witnesses: `wf083-fold-source-foreign-element-type` (a set source anchored on
  the subject), `wf083-fold-source-anchor-by-operand` (a count source anchored
  on the element type), `wf083-fold-source-foreign-object-type` (an own read
  anchored on another type), `wf083-total-foreign-element-type` (a `total`
  without a range whose `elementType` names another type than the
  subject). **A `sourcePath`'s INTERMEDIATE types are
  range, never anchor (2026-09-11).** The path (WF-102) introduces a third
  class of type into this position — the types the walk passes THROUGH —
  and the anchor is unmoved by them: every member of the folded set is
  still an `elementType` instance and every declaration is still
  looked up there, so a source `subjectType` naming an intermediate type is
  ill-formed exactly as any other non-anchor name is
  (`wf083-fold-source-anchor-by-intermediate-type`). The clause needs no new arm for
  it; it needs to have CONSIDERED it, because the shape where the anchor
  and the range diverge did not exist before the path did.
  Semantics: `RUNTIME-SEMANTICS.md` ch. 3.4a.

  *(History until L2, 2026-09-24: the notes below measured the rule on
  the L1 wire, whose fold source still carried a written role, a set flag
  and the unknown-type sentinel, and where the fold branch of the walk
  replaced the generic expression walk at every operand; since L2 a listed
  operand is walked as the ordinary read it is.)*

  *Wave note, 2026-09-05.* (b)'s anchor moved from the argument's own
  `objecttype` to the fold subject, and (c) is new. Both close the same
  defect from two sides: the verifier read a field neither OCaml executor
  reads in this position, and crossed nothing against the fields they do.
  Measured, each with its negative witness and an arm-exact mutant, over
  golden + red + negative + parity: **not one verdict of the 504 documents
  predating this card moves** — the sole change is one sentence of the
  existing `wf083-fold-source-navigates-away` report, whose verdict is
  anchor-independent because its operand already spells the subject; the
  four new witnesses take the negative zone to 302 and the four zones to
  508. The census behind the boundary: of 122 fold-source
  `AttributeReference` operands under an `elementObjecttype`, **110** are
  the single-argument instance-fold source and 12 are operands of a listed
  value fold carrying the tag vestigially; all 122 agree with their anchor
  today, and all four operands carrying a `navigatieRol` at all are warts.
  The scope of (c) is the single-argument shape because a listed value
  fold's operands are ordinary reads, where a foreign `objecttype` is a
  legitimate type-pair hop and not an anchor lie. What each half closes was
  measured on hand-built witnesses: for (b)'s anchor, one wire field
  (`objecttype` `Vondst` versus `Jutter` at the same `navigatieRol`)
  flipped the verdict PASS/FAIL while `ref` faulted `unsupported
  instance-fold source shape` and the ASP backend refused the expression
  kind on BOTH spellings — and the Drools backend played both and answered;
  for (b)'s scope, a set source carrying a subject-denoting `navigatieRol`
  is refused by both OCaml executors and would have become verifier-clean
  the moment the anchor moved; for (c), a temporal total naming another
  type's temporal attribute (`ref` faults, the ASP backend refuses by
  reason, Drools answers the OTHER object's value) and a non-temporal set
  total naming an attribute the element type does not declare — the sharpest
  of the four, where the two OCaml executors DISAGREE under a
  verifier-clean document: `ref` folds every member's absent value and
  answers 0, the ASP backend faults `unknown attribute … on "Vondst"`, and
  Drools throws.

  *Fix round, same day.* The scope of the preceding paragraph was widened
  from (c) alone to the WHOLE family: the navigation-bindings arm, (a) and
  (b) are single-argument-scoped too, on the same ground and the same
  measurement (zero verdict changes over the 510 documents of the four
  zones; a mutant that drops the scope changes exactly one report,
  `jutterswet-sluitsom`'s). Unscoped, (a) and (b) refused what all three
  executors agree on: measured on a `LAATSTE` whose second operand
  navigates one hop, `ref`, the ASP backend and both Drools branches all
  answer the pinned 30, while the branch's own verifier said WF-083 — a
  regression this card introduced and this round retracts. The
  path-carrying sibling of that form (a listed operand with a
  `navigatiePad` and its bindings) was a PRE-EXISTING over-refusal of the
  same class: `ref` and the ASP backend both play it, and the Drools
  exec-model branch refuses it per form. What the widening costs at the
  other end is bounded by the WF-053 companion, which now reaches the
  listed operands the fold branch used to skip.

  *Still open at this branch, measured and named — two flanks, neither
  claimed shut; the third and the fourth are closed below, and a fifth is
  named.* (1) A
  single-argument, non-set, non-wart fold source over
  a NON-temporal attribute of the subject satisfies (a)–(c) and stays
  verifier-clean while neither OCaml executor carries it (`ref`:
  `unsupported instance-fold source shape`; the ASP backend refuses the
  expression kind). (2) Both halves DEFER when the walk carries no subject
  type: (b) checks nothing without one, and (c)'s anchor is then absent in
  the non-set case. That defer is stated rather than hidden — measured by
  mutation (each defer arm made to emit a marker, swept over the four
  zones), it is reached **0 times in the 527 documents** of the four zones
  (re-measured 2026-09-06; the first count reported the same zero over the
  510 documents the corpus then had); every position that
  could reach it is already carried by a companion rule, so the deferral
  costs nothing today and is a hole the moment a subject-less fold
  position arrives. (3) The
  **row-1 wart as the source of a VALUE fold** rather than a count — the
  flank this branch opened when the anchor move newly ADMITTED the SET
  spelling (`set: true`, the unknown sentinel, `objecttype` =
  `elementObjecttype`, a `navigatieRol` denoting the subject), on which
  `ref` and the ASP backend both refuse while **Drools answers** —
  SOM/TOTAAL 19, MAXIMUM 12, MINIMUM 7 over members valued 12 and 7 — is
  **CLOSED by WF-093 below** (maintainer decision 2026-09-06): the wart is
  a count source in BOTH spellings, so those four kinds are ill-formed
  input and the question stops being a semantics question. It was a SHAPE
  rule and not an anchor rule, which is why it was tracked separately and
  answered separately. Flank (1) is likewise a
  SHAPE rule — which single-argument sources exist at all — not an anchor
  rule, and it is recorded as a flank rather than claimed shut.

  *(4) — the unknown type at an ORDINARY read, CLOSED (maintainer decision
  2026-09-06, boundary widened in its fix round the same day).* Closing (3)
  had to draw a line, and the line is the fold SOURCE. The unknown type also
  occurs where no fold-source rule reaches it: on a listed value fold's
  operand, on the operand of a single-argument BUILTIN carrying an
  `elementObjecttype`, and on a plain reference. That was the same
  one-answers-two-refuse class, but a question about the unknown TYPE rather
  than about fold sources, so it was tracked separately and is answered
  separately — by WF-034's **seventh wave** (ch. 4.6), which refuses it at
  every read the fold-argument branch reaches, in **both spellings**: the
  explicit `Onbekend` sentinel and its unspelled twin, an operand with no
  `type` at all. One guard governs both, for the reason the walk's own
  prologue treats them as one — a consumer cannot tell them apart, and
  neither can a rule that reads the wire. The clause is stated in ch. 4.6;
  what belongs here is the boundary it draws against the wart, and why the
  line falls where it does.

  **The boundary is the ARITY *and the KIND*, drawn on exactly the pair
  WF-093 keys on.** The expression walk's own type prologue has always
  refused the unknown type at a value node; the fold-argument branch
  REPLACES that walk, and the unknown type survived at exactly the positions
  the branch reaches. Of those, ONE is legitimate: the **single** argument of
  an **aggregation** IS the fold source, the row-1 wart, which carries the
  sentinel by construction because a written role name has no datatype to
  give — and whose kind is WF-093's question. So the exemption is the
  conjunction WF-093 itself uses, arity one AND a folding kind (the seven value
  folds plus `AANTAL`; six until 2026-09-20), and the two rules meet edge to edge over four
  quadrants: single + `AANTAL` is the count source both leave alone;
  single + value fold is WF-093's alone; two or more arguments is the listed
  value fold (§5.8.3) — no source, no role name, only ordinary reads — and
  is the seventh wave's; and a single argument at a NON-aggregation kind —
  the single operand of a builtin — has no source either and is the
  seventh wave's too (until L4 such a builtin could also carry an
  `elementObjecttype`; since L4 a `BuiltinCall` carries none, delta B21).
  Nothing is reported twice and nothing falls between
  them, and both halves of that were measured rather than argued. A mutant
  that moves the exemption from "single argument AND a folding kind" to
  "single argument AND `AANTAL`" adds a second citation to exactly the four
  `wf093-wratvouw-*` negatiefs and to nothing else in the four zones — the
  doubling the kind set avoids. A mutant that reverts the KIND half, leaving
  the arity alone — the boundary as the wave first landed — frees exactly
  `wf034-sentinel-builtin-operand` and `wf034-sentinel-builtin-wratoperand`
  and moves no other verdict and no other report line in the four zones: the
  gap the fix round closed, measured at its own size. A mutant that INVERTS
  the arity guard frees the three listed-operand witnesses and the
  unspelled-twin one, and newly rejects 12 documents (10 goldens, 5 of them
  anchors, and 2 red cases); a mutant that drops the arity scope altogether
  rejects the same 12.

  **What the fix round found, and why it was not a tidy-up.** With the
  boundary on the arity alone, a single-argument builtin — an
  `ABSOLUTE_WAARDE`, a `WORTEL`, a date extractor — carrying an
  `elementObjecttype` inherited the wart's licence while having no source
  whatever. Six spellings of that shape were built and measured. THREE were
  already fenced elsewhere and stayed so until L4: an `elementObjecttype` equal to the
  fold subject is WF-048 (`bindingless instance fold: no fact type relates
  'Jutter' to 'Jutter'`), an operand whose `objecttype` is the element type
  rather than the anchor is WF-083 (c), and a `bronRol` beside it is WF-047
  (`the written fold source … does not denote the bound role …`). THREE were
  not fenced at all — the degraded own read, the wart verbatim, and the same
  read spelled `set: true` — and the second of them CRASHED the third
  executor. Leaving them open would have kept a live one-answers-two-refuse
  class, complete with a crash, behind a boundary that had just been declared
  exact; closing them moves zero documents. That asymmetry decided it.
  Since L4 none of the six can be written: each needs an element type on
  the builtin, and a `BuiltinCall` carries no `elementType` and no range
  (delta B21) — the loaders refuse such a field by name.

  **One decision, six measured subforms.** They behave differently in the
  third executor and — since the fix round — in `ref` as well, and the
  differences are recorded rather than allowed to split the rule. All were
  verifier-PASS before the clause that now refuses them, except the plain
  read, which the walk's prologue already covered:

  | subform | before | `ref` | ASP backend | Drools |
  |---|---|---|---|---|
  | listed operand, the sentinel over a read that RESOLVES (v0's failed typing of a real attribute) | PASS | FAULT `unsupported AttribuutRef shape (set/sentinel outside a fold)` | FAULT `expression kind not translated` | **PLAYED** — it fires and answers 12.00 |
  | listed operand, the wart VERBATIM (a declared ROLE name in `attribuut` under the sentinel) | PASS | idem | idem | **ERROR** `IllegalStateException: geen getter voor 'vondsten' op …Jutter` |
  | listed operand, the SET reading (`set: true`, `objecttype` = `elementObjecttype`) | PASS | idem | idem | **EXCLUDE**, transpiler SKIP — refused per form |
  | a plain read (the whole rule value, or a composite's operand) | already refused | idem | idem | **PLAYED** — it fires and answers 12.00 |
  | single-argument BUILTIN operand, the degraded read and the wart verbatim (`elementObjecttype` present, no source) | PASS | idem | idem | **PLAYED** (12.00) and **ERROR** `IllegalStateException` respectively; the `set: true` third spelling is an **EXCLUDE** |
  | listed operand, the UNSPELLED TWIN (`"type": null`) | PASS | **PASS** — it plays, on the declaration | FAULT (`mixed date/non-date operands …` on the date fold, `expression kind not translated` on the numeric one) | **PLAYED** on the date fold; **ERROR** `NullPointerException: Cannot invoke "…Datatype.unitOf()"` on a unit-bearing numeric operand |

  How to read each row, because they are not equally attributable:

  - **Row 1** — the ASP column is not this shape's doing. The enclosing fold
    is refused by that backend with a REAL type on the operand too, so the
    column reports the backend's coverage, not the sentinel's effect. What is
    attributable is the pair `ref` FAULT / Drools PLAYED.
  - **Row 2** — fully attributable, and the sharpest: the executor READS the
    role name and reaches for a Java getter of it.
  - **Row 3** — pins the set-outside-a-fold FLANK as much as the unknown
    type: the Drools EXCLUDE is a refusal of `set: true` at a non-source
    position, which is a question of its own (still open; see the
    set-outside-a-fold card) and not a verdict on the sentinel. It is listed
    because the decision covers `set` positions too, not because it isolates
    the sentinel.
  - **Row 4** — fully attributable; the walk's prologue already refused it,
    and the row exists so the "one decision for every read position" claim is
    machine-checked at the position that needed no new clause.
  - **Row 5** — fully attributable, and the fix round's own: same three
    outcomes as rows 1–3 with no fold anywhere in sight.
  - **Row 6** — fully attributable, and the one that breaks the pattern: here
    `ref` is on the ANSWERING side. It derives the type from the declaration
    (the ch. 2.10 effective-type law) and replays a date golden to all five of
    its pinned expectations, so the split is **two answer and agree, one
    refuses** — and on a unit-bearing numeric operand the third executor stops
    answering and crashes instead.

  **Four kinds of dead end** over one position — played with an answer, thrown
  from a missing getter (`IllegalStateException`), thrown from a missing unit
  (`NullPointerException` on `Datatype.unitOf`), and declined as a form —
  and no executor that carries the shape across the board. That spread is the
  argument for refusing it at the wire instead of in four places. It is also
  why the earlier "the two OCaml executors refuse every one of them with the
  SAME fault" no longer holds as stated: it is true of rows 1–5 and FALSE of
  row 6, where `ref` plays. The decision covers all six anyway, and row 6 is
  the reason it must — a form two executors answer, agree on, and the third
  crashes on is not a form a specification can leave to the consumers. Seven
  negatiefs pin the rows: `wf034-sentinel-gelest-vouwoperand`,
  `wf034-sentinel-listed-wratoperand`, `wf034-sentinel-geleste-setlezing`,
  `wf034-sentinel-kale-lezing`, `wf034-sentinel-builtin-operand`,
  `wf034-sentinel-builtin-wratoperand` and
  `wf034-sentinel-null-vouwoperand`; the two spellings not given their own
  document (the builtin `set` reading and the numeric null operand) share a
  witness's reason and its message exactly, and their measurements are
  recorded in those witnesses' NOTEs.

  **A producer reaches it, and that was measured, not assumed
  (2026-09-06).** The RegelSpraak frontend's reference builder falls back to
  the sentinel when it finds no attribute datatype, and every recovery path
  that DOES bind one also rewrites the name and endpoint it binds — so the
  sentinel is emitted only where the reference resolves against nothing.
  At a plain read that always co-fires the name-resolution arm, and at a
  listed operand it usually does too. One case escapes: this branch carries
  ch. 4.3's role-name exception, keyed on the sentinel, so when the
  unresolved written name happens to coincide (ci) with a declared role name
  or plural, the resolution half ADMITS it and the sentinel is the
  document's only defect. Emitted and checked from
  "… als de maximale waarde van `<een attribuut>` en de `<rolmeervoud>` van
  de …": a two-operand `MAXIMUM` whose second operand is the wart verbatim,
  **no diagnostic at all** from the frontend, `verifier: PASS` before this
  wave — and the emission differs from the pinned witness in exactly THREE
  leaves, re-measured leaf by leaf in the fix round, none of them the defect
  (two `bron` provenance strings and the fold node's own `decimalen`) — and
  Drools throws on it while `ref` and the ASP backend refuse. So
  (i) cuts off a live producer route, not only a hand form. The sibling
  spelling, where the name matches no declared role, was never clean.

  *The UNSPELLED TWIN measures the other way, and that was measured too
  (fix round, same day).* The emitter's datatype writer emits JSON `null`
  only where the model hands it a null datatype, and the reference builder
  substitutes the sentinel before that can happen — so the twin should never
  reach the wire, and it does not: emitting every one of the repo's `.rgs`
  sources that resolves standing alone (73 of 78) yields **zero**
  occurrences of `"type": null` in any of them. That half of the fence
  therefore closes a WIRE form open to any conforming emitter rather than a
  producer route, and the two halves are refused alike anyway, because no
  consumer distinguishes them and the walk's own prologue never has.

  **What the other answer would have cost.** Reading the sentinel as a
  legitimate LEESvorm (answer (ii)) would have required an arm in `ref`
  deciding what a read whose type is absent even means — the datatype comes
  from the DECLARATION under the effective-type law
  (`RUNTIME-SEMANTICS.md` ch. 2.10), which is exactly what gate 13 already
  demands of every consumer, so the derivation exists; what does not exist
  is a reason to want it — a translation in the ASP backend, and a golden
  with hand-computed absolute pins. And it would have CANONISED a form that
  only ever arises as a DEGRADATION: no frontend writes the sentinel on
  purpose, and the wire spelling it produces is a reference that resolves
  against nothing. The cost of (i), by contrast, is four negatiefs and one
  clause.

  **Option (iii), weighed here as the card required, and rejected.** During
  WF-093's fix round a third key surfaced: sleutel the rule not on the
  sentinel but on "the `attribuut` slot resolves to a ROLE". It looks
  sharper — it separates the role-naming wart from a degraded attribute
  read — and it is measured DANGEROUS on its own: the subclass *sentinel on
  a genuine element-attribute set reading without a `navigatieRol`* becomes
  verifier-CLEAN under it, and on that form clingo and Drools both answer
  the right number while `ref` refuses, a live two-against-one divergence
  under a well-formed document. The card admitted (iii) only together with
  (ii), and (ii) is refused; with (i) the question dissolves rather than
  being deferred, because the sentinel is now ill-formed at every read
  position in the first place. The warning stands recorded for anyone
  re-opening it: rekeying WF-093 away from the sentinel re-opens exactly the
  divergence WF-093 closed, and would have to be done together with a `ref`
  arm, never before one.

  **The regression boundary, counted 2026-09-06 over golden + red +
  negative + parity (533 documents before the witnesses, 537 after).** The
  four zones hold 71 unknown-sentinel nodes, of which **65** sit on a `type`
  slot; the other 6 are declaration-layer or payload positions, all in
  already-rejected negatiefs. Of the 65: **61** are fold arguments (every one
  of them under an `elementObjecttype`), **60** of those the single-argument
  wart — untouched by this wave — and exactly **ONE** a listed operand. The
  remaining 4 are not fold arguments: one rule TARGET in an already-rejected
  negative, two plain reads, and the own `type` of the composite one of those
  reads feeds; all four are already refused by the walk's prologue or by the
  target clause. So the whole population this wave newly reaches is that ONE
  listed operand, and it sits in the PARITY zone, on a document that already
  carries a co-firing WF-034 at the very same pointer — its verdict does not
  move, its report gains one line (33 violations to 34). Swept before and
  after over all 537 documents: the three new rood-eerst witnesses turn from
  PASS to FAIL, that one parity line appears, and **not one golden, red or
  negative report changes a sentence**. One bookkeeping line belongs beside
  it, the same one flank (2) carries: **no gate runs the verifier over the
  parity zone** — gate 3 walks the three corpus zones — so the parity
  measurements here are measurements and never pins, and gate 11 (which
  replays that zone through the frontend and the toolchain-pinned dumps, and
  never through the verifier) is unmoved by this wave, measured and not
  assumed. *Since 2026-09-25 (the parity well-formedness card) gate 11 does put the
  replay slices through the verifier — 28 of 29 clean, parity slice 24 held out by
  name on its WF-099 report — while the casus dump stays outside that leg;
  the sentence records the state on its date.*

  *The fix round's own boundary, counted the same way (546 documents after
  its three witnesses).* Two populations, both counted rather than assumed.
  The UNSPELLED TWIN: the four zones hold exactly **three** value nodes
  carrying an explicit null `type` — one `AttribuutRef` (a rule TARGET, in
  `wf034-regeldoel-zonder-type`), one `Literal` (`wf034-untyped-literal`)
  and one `Functie` (`wf038-duration-without-node-type`) — every one of them
  in a negative that is already rejected for exactly that, and **zero** of
  them at a fold argument. The twin arm therefore reaches nothing that
  existed before its own witness. The BUILTIN exit: **zero** documents in the
  four zones carry a single-argument non-aggregation `Functie` with an
  `elementObjecttype` at all, so that half reaches nothing either. Swept
  before and after over all 546 documents, the whole fix round moves **seven
  reports and three verdicts**, and the two halves of that are separable
  because they were measured separately. The SEMANTIC half — the widened
  guard and the twin arm together, against the wave exactly as it first
  landed — moves **three documents and nothing else**, every one of them a
  verdict and every one of them one of the round's own new witnesses. The
  remaining four reports are the MESSAGE rewrite, which had to name the
  second exit: three existing `wf034-sentinel-*` witnesses and the parity
  casus restate one line, no verdict anywhere, and the parity document's
  violation count stays at 34. No golden, no red case, no other negative
  moves a sentence; gate 11 is untouched for the same reason it was
  untouched by the wave.

  *(5) — `set: true` at an ordinary read OUTSIDE a fold: CLOSED by WF-096
  below (maintainer decision 2026-09-09).* Flank (4) settled the unknown TYPE
  at a read position, in both spellings; it did not settle the other field of
  the same node. An `AttributeReference` with `set: true`, a REAL datatype and
  no enclosing fold was verifier-CLEAN — WF-093 did not reach it (it keys on
  the sentinel, and this one carries a genuine type) and the seventh wave did
  not either (that is about the type, not about `set`) — while **no executor
  carried it**: `ref` faults `unsupported AttribuutRef shape (set/sentinel
  outside a fold)`, the ASP backend refuses the expression kind, and the
  Drools backend drops the rule as a transpiler skip (`waarde buiten scope`).
  Three refusals, three kinds, one clean document — the PLAN §6 class in its
  other guise: not a divergence about the ANSWER but a well-formed document
  nothing can execute, which no differential gate can catch because there is
  no answer to differ about. Measured at both positions and identically: as
  the whole rule value and as a composite's operand. And it is a LIVE
  producer route, not a hand form — from "… als het gewicht van **alle**
  vondsten van de Jutter" the RegelSpraak frontend emits exactly that node,
  with a real `Numeriek` type, a `navigatieBindingen` role binding and
  `diagnostics: []`; re-emitted from source on 2026-09-09 and committed as
  WF-096's first two negatiefs verbatim, the second of them the same node
  under a `Binair`, which makes the OPERAND position a live producer route
  too. The two questions stayed ORTHOGONAL to the end: this shape's type is
  real, so the unknown-type fence never had jurisdiction over it, and the two
  rules now divide the position — WF-034 refuses the TYPE, WF-096 the FIELD.
  What the flank recorded as "the regression boundary is nil in the other
  direction too" was re-counted when the rule landed and is now one document
  rather than zero — not because the corpus moved but because the PREDICATE
  narrowed. The flank counted set readings outside a fold ARGUMENT; WF-096
  refuses them outside a fold SOURCE, and a listed value fold's operand is an
  argument without being a source. The one document the narrower predicate
  reaches, `wf034-sentinel-geleste-setlezing`, was already in the corpus when
  the flank counted zero, is already rejected for its sentinel, and gains one
  co-firing line. The predicate narrowed once more on 2026-09-19 — outside a
  **VALUE** fold's source, `AANTAL` out — and that step was counted the same
  way: a second already-rejected document, `wf083-fold-source-anchor-by-operand`,
  gains a co-firing line beside the WF-083 verdict it keeps, and no golden,
  red case or other negative moves a VERDICT. Four negatiefs do move a
  sentence, and that is the MESSAGE rewrite rather than this predicate: the
  clause had to name its kind half, so every document already citing WF-096
  restates one line.

**The count source (WF-093; two-sided since L2, 2026-09-24, B11, B13).** A
single-argument instance fold's set source is the COUNT source when its
`attribute` names no member — no attribute and no characteristic — of the
element type: it then names a role of the fold subject's relation, the
written name of the role the range binds (WF-047), and it hands the fold the
linked INSTANCES rather than a value per contributor. That is exactly what
`count` consumes, and exactly what no other fold kind can use: `sum`,
`total`, `minimum`, `maximum`, `first`, `last` and `filledCount`
read a value PER CONTRIBUTOR. The recognition reads the source and never the
node's kind, which is what lets the rule say both directions without being
circular. It reads the name alone, and that has one consequence a document
can meet: a role whose written name — the declared name or the declared
plural, whichever the source writes — is also the name of a member of the
element type is read as that member, so a count over it is refused by
(b) below, and the count has to be written with the role's other declared
spelling where that one does not collide (ch. 2.6).

- **WF-093** For the single argument of an aggregation carrying an
  `elementType` and a range: (a) a count source — an `attribute` that
  names no member of the element type — MUST be the source of `count`:
  under a value fold (`sum`, `total`, `minimum`, `maximum`, `first`,
  `last`, `filledCount`) a source that names a role of the fold subject is
  ill-formed
  (`wf093-filled-count-wrat`, `wf093-wrat-fold-bare-first`,
  `wf093-wrat-fold-bare-sum`, `wf093-count-source-under-sum`), while a name that
  is neither a member of the element type nor a role of the subject is an
  unresolved member (WF-034); (b) `count` MUST read a count source: a
  count whose source names a member of the element type reads a value per
  contributor, which a count does not consume
  (`wf093-count-over-attribute-source`). A fold of two or more arguments is
  the listed value fold (§5.8.3), whose operands are ordinary reads with
  no source and no role name; this rule says nothing about them — the
  same scope WF-083's family carries, for the same reason. Semantics:
  `RUNTIME-SEMANTICS.md` ch. 3.4a — the count folds the subject's linked
  instances. That chapter is count-only: it says nothing about a value
  fold over a count source in either direction, which is why this is a
  well-formedness rule and not a semantics clause.
  *(History until L2.)* The rule keyed on the unknown-type sentinel on the
  source's own `type` — v0's spelling of the count source, the row-1 wart,
  in a bare and a set spelling (maintainer decision 2026-09-06) — and had
  direction (a) alone; direction (b), a set reading under `AANTAL`, was
  WF-096's since 2026-09-19. With the sentinel gone (B13) and the set flag
  gone (B11) the recognition moved to the source itself, and WF-096's
  count half became (b). The paragraphs below record the rule as it stood
  on the L1 wire.

*Why the count and not the set, and what the other answer would have
cost.* `ref` is the norm here, and it already says so at evaluation: its
FIRST fold-source arm matches the sentinel — before the set arm, and
regardless of `set` — so both spellings yield instances and both are
refused with `value aggregation over a bare role source`.

The ASP backend refuses the expression kind **on the four witnesses**, and
what that refusal is keyed on is worth saying exactly, because it is not
the sentinel: its two set arms match the source with `typ` as a WILDCARD
and demand `navigatieRol = None` (`backends/clingo/lib/translate.ml`, the
som/totaal arm at :1444 and the minimum/maximum arm at :1723, read
2026-09-06), and the witnesses carry a subject-denoting `navigatieRol`.
Strip that one field and the same document translates. Measured 2026-09-06:
a set source carrying the sentinel with NO `navigatieRol` replays exactly —
19.00 for the sum and 12.00 for the maximum over members valued 12 and 7,
the same two values Drools answers — and on `jutterswet-vondsttelling` with
its real set source's `type` degraded to the sentinel, all seven pinned
expectations replay, the 3.75 among them, while `ref` refuses every one of
those documents with the fault above. That SUB-CLASS is inside this rule
deliberately: `ref` is the norm and refuses the whole sentinel class
uniformly, so a verifier-clean version of the sub-class would be a live
`ref` ≢ clingo/Drools divergence under a well-formed document — precisely
the class this rule closes. Keying the rule more narrowly — on "the
`attribuut` slot resolves to a role", say — re-opens it, which is why
WF-093 keys on the sentinel and on nothing else.

Reading the wart as a legitimate SET source instead would have cost, all of
it new: a fold arm in `ref` deciding what "per member" even means for a
source that, in the bare spelling, names no attribute at all, a translation
in the ASP backend, and a golden with hand-computed absolute
pins to anchor the answer — and it would have CANONISED a shape the
frontends DO produce, but never as a document's only defect. That is the
form the argument really takes, and it is measured rather than assumed
(2026-09-06). The RegelSpraak frontend derives a value fold's element type
from the operand's `objecttype` (a real attribute read) and takes the
written-role route on the `AANTAL` branch alone, and the Catala frontend
never constructs the sentinel at all — but one route escapes that
description: a set aggregation over a CHARACTERISTIC of the element type.
The frontend's unresolved-member gate asks whether the element type
declares a MEMBER, and a characteristic is one, so the gate stays open
(`RegelSpraakModel.ObjectType.hasMember` in the frontend's model package; the gate in
`ExpressionBuilder`, the resolver's expression builder), while the reference
builder finds no attribute datatype and falls back to the sentinel (the same
class). Emitted and checked: "de som van het
waardevol van alle vondsten van de Jutter" produces exactly this source,
with `set: true`, no `navigatieRol` and no diagnostic at all — and the
verifier reports THREE rules on it, WF-034 (the fold's own `type` is the
sentinel too) and WF-080 (a characteristic endpoint in an aggregation
source) beside WF-093. So a producer exists; what does not exist is a
document in which this rule is the only thing wrong. What kept the question
alive was therefore still not a producer but a third executor: the Drools
backend keys its role reading
on the fold KIND plus its own derivation, so on the set spelling it
ANSWERS — 19 for sum and total, 12 for maximum, 7 for minimum over members
valued 12 and 7 (measured 2026-09-05, re-measured 2026-09-06) — while two
executors refuse under a clean document. Those three answers are now
unreachable: the input is ill-formed. `ref`'s fault stays where it is, a
second line of defence rather than dead code, on the house pattern.

*The regression boundary, counted 2026-09-06 over golden + red + negative
+ parity.* Of the **58** single-argument wart fold sources sitting under an
`elementObjecttype`, **54** predate this rule and every one of them is an
`AANTAL`: 49 bare (`set: false`, no `navigatieRol`), 4 bare carrying a
self-denoting `navigatieRol`, and 1 with `set: true` — the already-rejected
negative `wf083-fold-source-anchor-by-operand`. There is no value fold over a
wart anywhere in the corpus; the form is hand-built by construction. Swept
before and after over all 527 documents of the four zones, exactly the four
new witnesses change verdict and not one existing report changes a
sentence.

**The set reading (WF-096) — removed at L2 (2026-09-24), history.** Since L2
the set source is a POSITION, not a field: the single argument of an
instance fold that carries a range (the reading of the source, (i)). No
other position can state a collection, so the forms WF-096 refused
cannot be written (B11). What it refused at an
ordinary read is since L2 a navigated read with a to-many hop, WF-053's
to-many clause (`wf053-to-many-rule-value`, `wf053-to-many-plusoperand`,
`wf053-to-many-list-fold-operand`); what it refused under `AANTAL` is WF-093
(b) (`wf093-count-over-attribute-source`). The rest of this subsection is the
rule's history on the L1 wire, where a set flag on the reference carried the
reading. *(History until L2:)* `set: true` on an
`AttributeReference` said the read yields the members' values, one per
member — a COLLECTION, where every other read yields one value. Exactly one
node consumes a collection: the VALUE fold that OWNS the source. So the
field has exactly one admissible position, and it is the one ch. 4.3 has
described all along — the SINGLE argument of a value aggregation, the set
fold source ("het bewaartarief van alle vondsten van de Jutter": each member
is asked for the attribute, and the fold collapses the values). At every
other position the wire states a collection into a slot that takes one value,
and the document says nothing about which value that is. **Maintainer
decision, 2026-09-09: the shape is ill-formed on the wire.**

*The word VALUE in that sentence is the SECOND decision's* (2026-09-19, the
exemption note below). A count is the one aggregation that reads no value per
contributor — it folds the CONTRIBUTORS themselves (`RUNTIME-SEMANTICS.md`
ch. 3.4a says exactly that, and ch. 3.4b restates it for the union as
ELEMENTS; ch. 1.2e adds only the neighbouring clause that a count is never
empty; and the normative RegelSpraak specification's own §5.8.1 defines the
count over INSTANCES) — so `set` under `AANTAL` names a
collection the node does not consume, and the position is one of the other
positions rather than the
admissible one. That is why the set fence's boundary is NARROWER by exactly
one kind than the sentinel fence's beside it: v0's own spelling of a count
source is the unknown sentinel, which stays admissible under `AANTAL`, while
the set spelling of it leaves the wire.

*The other half of that sentence, and it took a second card to see it*
(maintainer decision 2026-09-20). If a count reads no value per contributor,
then a count over an OPERAND LIST has no reading either, and the wire
admitted one: WF-037's "≥ 2 arguments" clause — the clause that DEFINES the
listed value fold — excluded `AANTAL` so that the one-argument instance count
would pass, and left the listed spelling of a count admissible as a side
effect. The two fences missed each other by exactly that. Measured on a
well-formed document, and it is the same shape of finding as the set
spelling's: the verifier passed it, `ref` answered the count of the FILLED
operands — the meaning the 2026-09-19 narrowing had just taken off this kind
— the ASP backend refused the expression kind, and both Drools branches
dropped the rule. One answers, two refuse, which `PLAN.md` §6 calls a spec
fault. There was nothing to mirror either: §5.8.1 defines the count over
instances, the source language has no listed spelling for one (it has one for
"de eerste van A en B"), and the typing document excludes the value reading.

*And it had a LIVE producer route, which is the half a reading of the source
language alone got wrong.* The RegelSpraak frontend's own grammar spells the
list — `aantalFunc` reads `primaireExpressie ( COMMA primaireExpressie )* ( EN
primaireExpressie )?`, a widening of the one-operand EBNF carried over from the
ordering folds beside it, unregistered until this decision — and its resolver
built every child as an argument. Measured 2026-09-20: three sentences emitted
unblocked, two of them with `diagnostics: []` and the third, which binds an
element, with a non-blocking warning. So this is not a wire form nobody could
write; it is a wire form one producer DID write, and the rule cuts that route.
The frontend refuses all three at the node since the same day, and the grammar
keeps the widening as a registered divergence rather than deleting it, because
deleting an alternative re-associates sentences instead of refusing them. So
the listed count left the wire on the same
ground as the set source, under WF-037 rather than this rule, because what is
wrong with it is its ARITY and not a field. The meaning authors do want —
count the filled ones — has carried its own name since 2026-09-20,
`AANTAL_GEVULDE` (`filledCount` since L5), over a set source; the listed
spelling of THAT is
expressible the day a source language norms one.

*One decision for two positions, and what the other answer would have cost.*
The question was measured at both — the whole rule value and an operand of a
composite node — and the two came back identical, the same three refusals
word for word from the same three readers: `ref` faults `unsupported
AttribuutRef shape (set/sentinel outside a fold)`, the ASP backend refuses
the expression kind, the Drools backend drops the rule as a transpiler skip
(`waarde buiten scope`). Three refusals, three kinds of refusal, one document
the wire admitted — and no differential gate can catch it, because there is
no answer to differ about: gates 5 and 6 never see a document without a
golden case, and gate 12 counts the form among its auto-exclusions. Two
positions that measure identically are one question, so splitting them would
have produced two rules with one boundary and a defect answered by a
different number for no reason a reader could state.

The other answer — the set reading as a legitimate scalar read — would have
had to say what the value IS, and every arm of that answer is new: a
semantics clause in `RUNTIME-SEMANTICS.md` fixing the one-member case and
then the zero and many cases (a fault? the empty value? a first pick?), an
arm in `ref`, a translation in the ASP backend, a golden with hand-computed
absolute pins to anchor it, and a Drools decision to carry or refuse with a
reason. It would also have canonised a collapse the language does not
otherwise perform: in v0 a set reaches a scalar slot through a FOLD and
nowhere else, and the fold is what NAMES the collapse. The cheap half of that
argument — "one member, so take it" — is exactly the half that has no answer
at zero and at more than one, which is where a silent first pick would have
entered the semantics.

- **WF-096** *(removed at L2, 2026-09-24 — history.)* Landed L2
  2026-09-24: the set flag left the wire (B11), and the set source is the
  argument of a fold that carries a range (ch. 4.3, the reading of the
  source). Until then an
  `AttributeReference` with `set: true` was admissible ONLY as the SOURCE
  of a single-argument **VALUE** fold (seven value-folding kinds since
  2026-09-20, the filled count among them), and ill-formed at every other
  READ position: as a whole rule value, as an operand of a composite
  node, inside a condition, as an operand of the listed value fold, and —
  since the maintainer decision of 2026-09-19 — as the source of a COUNT.
  Its witnesses were removed or replaced (`corpus/negative/README.md`,
  L2): the ordinary-read positions by WF-053's to-many clause, the count
  position by WF-093 (b).

*The producer route this cuts.* Not a hand form. From "Het maatgevend gewicht
van een Jutter moet berekend worden als het gewicht van **alle** vondsten van
de Jutter." the RegelSpraak frontend emits exactly this node — a real
`Numeriek` type, a `navigatieBindingen` role binding, `set: true` — with
`diagnostics: []`; and from "… als het gewicht van alle vondsten van de
Jutter **plus** het drempelgewicht van de Jutter." it emits the same node as
the LEFT OPERAND of a `Binair`, again with no diagnostic. Both were
re-emitted from source while this rule was written and are committed verbatim
as its first two negatiefs. So the rule cut a live producer route and the
frontend has moved (2026-09-11,
`backlog/done/20260909-frontend-alle-zonder-vouw.md`): a resolver after-pass
diagnoses the shape at every Expr-bearing value and condition slot — eight
measured routes from that same sentence fragment — at WARNING level, so the
emitted document now states its own ill-formedness; the ERROR promotion is a
recorded one-token fork held by the maintainer. The negative emissions stay
verbatim from before the diagnostic — they are this rule's evidence.
`ref`'s fault, the ASP refusal and the Drools skip stay where they are, a
second line of defence rather than dead code, on the house pattern: the form
no longer reaches them through a well-formed document.

*The regression boundary, counted 2026-09-09 over golden + red + negative +
parity (560 documents).* Exactly **one** document predating this rule carries
an `AttributeReference` with `set: true` outside a fold source: the negative
`wf034-sentinel-geleste-setlezing`, whose listed operand is already rejected
for its sentinel. Its verdict does not move and its report gains one
co-firing line, which the register admits ("incidental extra violations are
allowed"). The position the rule LEAVES ALONE is populated and pinned: **16**
legitimate set fold sources across **14** goldens, `jutterswet-vondsttelling`
among them, replayed by gates 5, 6 and 12 — none of them moves. Swept before
and after with the arm-exact mutant (the clause removed from both walk paths,
verifier md5 `022b856c82718077d52c0e4cb1ac973d`, against the head's
`5111932e2d1e51de144290ab16abab00`): **four** documents differ in total — the
three new witnesses, each a verdict, and that one existing line. No golden,
no red case and no parity document changes a sentence. Gate 3 does not run
the verifier over the parity zone, so the parity half is measurement and
never a pin (since 2026-09-25, the parity well-formedness card, gate 11 does hold
the replay slices verifier-clean, parity slice 24 held out by name; the casus dump
stays outside that leg). The clause is stated in TWO places — the generic expression walk
and the fold-argument branch, which replaces it for a fold's arguments
exactly as it replaces the type prologue WF-034's seventh wave had to repeat
there — and the two halves were mutated SEPARATELY as well, each arm-exact on
its own: removing the read half moves exactly the two producer-authored
witnesses and nothing else; removing the fold half moves exactly the listed
witness and that one co-firing line, and nothing else.

*Three classes this clause does NOT reach, all measured rather than assumed
(2026-09-09).* The rule TARGET: `set: true` on a rule's `doel` is
verifier-CLEAN and stays so — the target is a write position, reached by the
target clause and not by the expression walk. It is left open on evidence
rather than fenced without a witness, and the population is counted with its
predicate stated: the four zones hold **783** rule-target positions — **357**
of them in the two zones that must stay clean (golden and red) — and not one
carries `set: true`. On a hand-built twist all THREE executors IGNORE the field
and write the scalar (12.00 from `ref`, from the ASP backend and from Drools
alike). That is three-way agreement on an inert annotation, not the
one-refuses-two-answer class this rule closes — it is WF-083 (c)'s question
instead ("a field nobody reads is a field that can say anything"), and it
belongs to whoever asks it. The fold argument whose RESOLVING PAIR is
incomplete: there `attribuut` or `objecttype` is absent and WF-034 reports the
missing name; the set clause is computed ahead of those arms precisely so the
field's rule cannot hide behind the name's, and no document in the four zones
carries the combination. And the EXEMPTION itself: as this rule first landed it fenced the
POSITION, never the fold KIND, so a set source under `EERSTE`, `LAATSTE` or
`AANTAL` passed the exemption and stayed verifier-clean whatever the executors
did with it. That is the PLAN §6 class one door further in and it was NOT a
regression of this rule — the boundary was WF-093's pre-existing pair — so it
was cut as its own card rather than settled here, where the decision was about
the position. **That card is done (2026-09-19,
`backlog/done/20260909-eerste-laatste-setvouw-kloof.md`), and it answered the
kinds one at a time.** `EERSTE` and `LAATSTE` are CLOSED: they are the same
fold as `MINIMUM`/`MAXIMUM` — an ordering over the contributed VALUES, never a
positional pick of a member (`RUNTIME-SEMANTICS.md` ch. 1.2e) — so the ASP
backend carries them on the very arm those two already used, and `ref` ≡ clingo
holds on hand-computed absolutes (`jutterswet-vondstvenster`, emitted from the
live producer sentence; gates 5, 6 and 10). The Drools backend lowers neither
of its transpiler branches for the form and refuses the golden per form with a
named reason, recorded in gate 12's channel list: a two-executor agreement,
stated as such. **`AANTAL` is CLOSED TOO, and the other way (maintainer
decision 2026-09-19, option (ii) of that card's §6): the wire REFUSES the
form, and this rule's exemption is what narrowed to say so** — from WF-093's
pair to the six value folds alone (seven since 2026-09-20, ch. 4.3), so a set reading under a count is now this
rule's own violation. The reason was measured, not argued: the two executors
that carried it disagreed as soon as one member's attribute was leeg — `ref`
counted the FILLED VALUES and Drools counted the MEMBERS, 2 against 3 on one
document — while the ASP backend refused the kind outright; both readings were
verifier-clean; `RUNTIME-SEMANTICS.md` ch. 3.4a says a count folds INSTANCES
and ch. 3.4b restates it for the union as ELEMENTS, the normative RegelSpraak
§5.8.1 defines the count that way too, and ch. 1.2e adds the neighbouring
clause that a count is never empty — so one of the two readings is simply
wrong rather than merely different; and no producer emits the
shape at all (the RegelSpraak frontend refuses the one sentence that would
write it, citing this very rule). So the option that cost nothing at the
producer was taken, and the price is named: `ref`'s set-source count arm and
the Drools lowering become unreachable through a well-formed document and stay
as second lines of defence, exactly as this rule's own first landing left
three such arms. The counting of VALUES and the average over them are a
separate, normatively defined operation when they are wanted —
`backlog/new/20260919-aantal-gevulde-gemiddelde.md`. Three negatiefs pin the
kinds of spelling the form had (`wf096-aantal-setbron`, the real-typed element
attribute over a `bronRol` range; `wf096-aantal-setbron-wrat`, the row-1
wart's set spelling; `wf096-aantal-setbron-bronpad`, the multi-step range of
ch. 3.4b), and two further hand-built spellings — a bindingless source and one
carrying a self-denoting `navigatieRol` — were measured to fall to the same
arm and are left unpinned on purpose: one arm, one witness per spelling, not
per field combination. The four kinds that predate the question stay
carried and agree over a NUMERIC set source; over a DATE one the card measured
them apart, and the break is named rather than left in the sentence:
`SOM`/`TOTAAL` fault in `ref` (date plus date) while the ASP backend adds the
day numbers and Drools throws, and `MINIMUM`/`MAXIMUM` agree in the two OCaml
executors while Drools throws — all under verifier-clean documents, none of
them reachable from a producer (the frontend types the sentence out), and all
of it that card's follow-up finding rather than this rule's business.

**The count source's role name (WF-047; landed L2, 2026-09-24).** The
count source writes a role name in its argument's `attribute` slot: the
written name of the role its range binds (the reading of the source,
(ii)). That name MUST denote
the bound role, case-insensitively on declared name or declared plural —
never a synthesized plural (the ch. 2.6 decision) — and WF-047 crosses it
against the range. Where the written name denoted neither, the L2
migration and the emitter write the declared name of the bound role and
keep the written spelling in `source` (ch. 2.6, WF-047). A value fold's source
reads an attribute value and resolves as an ordinary attribute reference
on its anchor (WF-034, ch. 4.6 (l)). Semantics: `RUNTIME-SEMANTICS.md`
ch. 3.4a; the ASP backend keys the count by the connecting role's
canonical name, never by the written lexeme. *(History until L2:)* the
count source was spelled with the unknown-type sentinel on its own `type`,
the discriminator both OCaml executors keyed their role-naming branch on;
within it a written name that resolved to no declared role was admitted
when the type pair derived a unique connecting role (counted in candidate
roles, WF-048's derivation), and was WF-034 otherwise. With the range
written (B12) and the sentinel gone (B13) neither the key nor the fallback
exists: the range names the role, and the fallback's two witnesses
(`wf034-ambigu-typenpaar`, `wf034-zelftype-typenpaar`) were removed with
reason.

**The filled count (`filledCount`, v0 `AANTAL_GEVULDE`).** *(Maintainer
decision 2026-09-19, decisions G1–G8; landed 2026-09-20.)* One aggregation
kind folds the contributors' VALUES by PRESENCE: it answers how many
contributors have a value that is not leeg. It is an instance fold, it reads
a value per contributor, and its result is a whole, unitless cardinality
whatever the folded attribute carries. Semantics:
`RUNTIME-SEMANTICS.md` ch. 3.4c, with the ch. 1.2e row.

**A tracked v1 delta, not a mirror** (`CONSTRUCTS.md` row 1, the aggregation
row). The source language has no spelling for this operation at all: its
§5.8 aggregation catalogue is a closed alternation of five forms, "gevuld"
appears there only as a PREDICATE over one subject expression (§8.1.2), and
its own typing document rules the value reading OUT of the count operator by
saying that operator takes subject expressions and therefore has no empty-
value question. The count counts instances and keeps doing so. So this kind
is written FIRST on the wire and has no producer route in any frontend —
the same order the `first`/`last` widening of 2026-09-04 took.

The node is the existing `Aggregation` (the v0 `Functie` until L4), in the
instance-fold shape. **No field is added and no field is given a new
meaning:**

| Field | On `filledCount` |
|---|---|
| `arguments` | exactly ONE, an `AttributeReference` — the set source, which the fold's range makes it (the reading of the source, (i); the set flag until L2) |
| `elementType` | required: the population, exactly as ch. 3.4a reads it for every instance fold |
| `filter` | the die/dat subselection, per contributor, unchanged |
| `emptyFallback` | always absent — a count has no empty to fall back from |
| `dimensionSelection` | always absent — **postponed, not rejected** (below) |
| `granularity` | not a field of an `Aggregation` since L4 (delta B21) — it buckets nothing |
| `type` | a whole, unitless number: a cardinality — **derived**, so the annotation is crossed against it under WF-091 exactly as a count's is, never believed (negative `wf091-filled-count-unit`) |
| `sourceRole` / `sourcePath` | the range, one step or many, exactly as on the value folds (WF-102); required, as on every single-argument fold with an `elementType` under a kind other than `total` (B12) |

*Why no node of its own.* The v1 split of row 1 separates aggregation-with-a-
source from ordinary builtin call. This kind falls on the aggregation side
and has precisely the facets that side already has — a source, an element
type, a filter. A second node would duplicate a facet set.

*Why it cannot be desugared.* No existing node expresses "count the non-empty
values". The count folds instances; the `filter` evaluates a condition per
contributor but no condition can ask after the fold SOURCE's own filledness,
since the filter stands beside the source rather than inside it; and the list
world of ch. 4.8 is a different world from the instance fold. The kind is
kernel by the row-1 criterion, not sugar.

**WF-096's exemption was SEVEN value folds, not six** (from 2026-09-20 until
L2, when WF-096 left with the set flag — history). That
is the application of the 2026-09-19 narrowing and not an exception to it.
The narrowing's question is "does this kind read a VALUE per contributor?",
and what left the wire that day was a `set` field under a node whose
semantics fold CONTRIBUTORS. This kind is a count in its RESULT and a value
fold in its READING, and WF-096 fences the reading — so a set source is not
merely admissible here, it is REQUIRED (WF-037 below). WF-093's boundary is
unmoved and untouched: its pair is still "a single argument and a folding
kind", so the row-1 wart under this kind is ill-formed exactly as it is
under the other six, and the rule needed no amendment to say so
(`corpus/negative/wf093-filled-count-wrat`, whose diagnosis moves from
WF-034 to WF-093 on the widening alone).

**WF-037 carries the shape, in three arms, and two of them demand a form
instead of forbidding one** — as the count's source arms do since
2026-09-23 and 2026-09-25; all are stated in ch. 4.6. (i) Exactly ONE argument: there is no listed form of
this kind, because there is no written form for one to mirror
(`wf037-filled-count-second-argument`). (ii) An `elementType`: it is an
instance fold and without one its argument is no source at all
(`wf037-filled-count-without-element-type`). (iii) That one argument MUST be
the set source. Since L2 (2026-09-24) the range makes it one (the reading
of the source, (i)), and the schema and every loader demand the range of a
single-argument fold with an `elementType` under every kind but `total`
(B12): the subject's own read
cannot reach this kind at all, so the set-source demand is enforced at load.
What the verifier still decides under (iii) is a range beside a source that
is no attribute reference (`wf037-filled-count-not-set-source`, since L2 a
literal source), and since 2026-09-25 the fold-source arm of WF-037
decides it, naming the node, for every kind alike (ch. 4.6). The own read has ONE value, so its filled count would be 0
or 1 — a form no author writes — and a count source hands the fold instances
instead of values, which WF-093 (a) refuses and `ref` refuses at evaluation
(`value aggregation over <name>, which is no attribute of <element type>`;
until L2 `value aggregation over a bare role source`). The three facets the kind
never carries were refused by WF-037's existing, positively written clauses
without an amendment, each with its witness:
`wf037-filled-count-empty-fallback`, `-dimensieselectie`, `-granulariteit`. Since
L4 (delta B21) `granularity` is a `BuiltinCall`'s field alone, so it cannot
stand on this node at all, and `wf037-aantalgevulde-granulariteit` was removed
with reason.

*`dimensionSelection`: postponed, not rejected.* The dimension fold is
itself a dialect form (`CONSTRUCTS.md` row 28), and a combination of two
dialect forms with no casus is one nobody writes and nobody checks. A later
card with a real casus finds the door closed and not locked; this sentence
is what says so.

**WF-038 does not reach this kind, and says so.** It is not a numeric fold
in the sense of that rule: its operand may carry ANY VALUE type, because the
test is presence and nothing is added or compared. The ch. 2.4 orderability
paragraph has no grip here either, for the same reason — no contributor is
ranked against another. Without this scope sentence the kind could be read
under WF-038's numeric-fold arm, and the typelessness of the test would be
silently withdrawn.

*One exclusion, and it is on the TEMPORAL axis rather than on a value type.*
A set read of a time-dependent attribute folds twice — each member's own
timeline collapses over time first — and only the som and the totaal define
that first collapse, because only they integrate (`RUNTIME-SEMANTICS.md`
ch. 3.6). A count has no defined MOMENT at which to ask a timeline whether
it is filled, so a filled count over a time-dependent attribute fails
observably at evaluation, naming the attribute. Stated normatively in
ch. 3.4c rather than left to the executors: it is a missing rule, not an
unimplemented one, and a later card may supply the moment rule as a clause.

*The result type is DERIVED, so it is crossed and not believed.* It is fixed
by the operation — a whole, non-negative, unitless cardinality — rather than
read off an operand, and **WF-091 crosses the annotation against it, on the
same arm that has always crossed `count`'s** (ch. 4.1, the composite
derivation inventory; negative `wf091-filled-count-unit`). Without that
cross this kind would be the one aggregation whose node type nothing checked:
measured 2026-09-20, a filled count annotated `kg` and written into an
equally-lying `kg` target was verifier-CLEAN while the identical lie under
`count` was WF-091, and `ref` then played it — a unitless 1 landing in a
kg-declared attribute with the oracle reporting PASS. No unit travels through
the node, and the consumer clause of `RUNTIME-SEMANTICS.md` ch. 2.10 leaves
it alone precisely because the operation, not the annotation, fixes it.

**The instance-set union — the §5.7 concatenation of subject expressions
(WF-110).** *(Maintainer decision 2026-09-20.)* The source language lets a
count range over a CONCATENATION: "het aantal ⟨rolselectie⟩ en
⟨rolselectie⟩" joins the instance sets the selections yield into one new set
(§5.7), and the typing document says of the count operator that it admits a
concatenation of expressions, unbounded in number, and takes subject
expressions only (§4.11). That is a different reading from the multi-operand
VALUE count WF-037 refused the day before, and the refusal is unmoved: this
form counts INSTANCES, so what the operand list would have to carry is not a
list of values but a list of SELECTIONS. The wire therefore gets a node for
the selection list instead of a licence for the operand list, and the two
forms are distinguishable at a glance — a count whose `arguments` hold two or
more expressions is WF-037's refusal, byte for byte; a count whose ONE
argument is an `InstanceSetUnion` is this form.

*The wire form.*

| Field of `InstanceSetUnion` | Meaning |
|---|---|
| `branches` | list of `InstanceSetBranch`, **two or more** — the selections, in written order |
| `source` | provenance of the written concatenation (the source reference, ch. 4) |

| Field of `InstanceSetBranch` | Meaning |
|---|---|
| `elementType` | the population this selection ranges over, read exactly as ch. 3.4a reads an instance fold's — and the SAME for every selection of one union (WF-110 (d)) |
| `sourceRole` | RoleBinding, **required**: the one-step relation this selection walks from the count's subject. No bindingless fallback and no `sourcePath` (below) |
| `filter` | Condition? — this selection's own die/dat subselection, evaluated PER CONTRIBUTOR with the element as its subject, exactly as a fold's `filter` is |
| `source` | provenance of the written selection (the source reference, ch. 4) |

The node carries **no `type`**. It denotes instances and not a value, so
there is no annotation to believe and none for the ch. 2.10 consumer clause
or the gate-13 twist harness to reach; WF-034's type prologue exempts it by
node kind, exactly as it exempts a `Lambda`. The count above it derives its
own whole, unitless cardinality as it always has.

*Its position is ONE position.* The union is admissible as the SINGLE
argument of a `count` aggregation and nowhere else — not as a rule value,
not as an operand of a composite node, not as an operand of a condition, not
under any other kind. That is the shape of rule WF-096 drew for the set
reading until L2, for the same reason: the one node that consumes a
collection is the node the collection belongs to, and everywhere else a
collection stands where a value is read.

**The clause is about the UNION node and not about the count above it**, and
the distinction is worth spelling out because the two readings differ and only
one is meant. "Not as an operand of a condition" forbids the BARE union in a
comparison; it says nothing about a COUNT over a union, which is an ordinary
whole-number expression and stands wherever any other count stands — in a
rule's value, in a `condition`, on either side of a comparison. That form is
producer-reachable ("Een Jutter is ruim voorzien indien het aantal ⟨selectie⟩
en ⟨selectie⟩ groter is dan 2"), all four executors carry it, and
`jutterswet-unievoorwaarde` is its witness — pinned rather than assumed,
because a rule whose scope is read one notch too wide silently removes a legal
form. The count that carries it names **no source of its own** —
`elementType`, `sourceRole`, `sourcePath` and `filter` on that node are
absent, because the selections carry all four, one set each, and a source
beside the union would be a second claim on one node (the setbron ground of
2026-09-19).

*Deduplication on instance identity is the operation, not a detail.* The
members are the union of the selections' contributors, and an instance that
two selections both select contributes ONCE — so the count is `|A ∪ B|` and
never `|A| + |B|`. Semantics, with worked absolutes:
`RUNTIME-SEMANTICS.md` ch. 3.4d.

*One population per union, and the boundary is drawn on a ground of our
own.* Every selection names the same `elementType` (WF-110 (d)). The v0
toolchain refuses the same written shape and its own test says why: one
Drools accumulate has ONE source pattern, so selections over different
element types cannot be unioned without silently dropping the foreign set.
That is a property of a code generator, and it is not the reason here.
`elementType` is the POPULATION everywhere else in the IR (ch. 2.2), a count
answers a cardinality over one declared type, and with a population per
selection the membership test differs per member — whether two selections
could then share an instance is a question about the subtype lattice
(ch. 2.6, where a role filler is an upper bound and `Munt ⊑ Vondst`), which
deduplication on identity would have to answer. This clause does not answer
it and refuses the shape instead of picking an answer by side effect. The
door is closed and not locked: a later card with a real casus may norm the
multi-population union, as a clause here carrying its own dedup statement
across the lattice.

*Where uchar DIVERGES from the toolchain, deliberately: the selection with
no subselection.* The toolchain also leaves unlowered a concatenation one of
whose operands carries no resolvable characteristic filter — a bare
selection beside a filtered one — and that too is a property of its
mechanism, which lowers the concatenation by OR-ing the operands' filter
terms and has no term to OR for an unfiltered operand. Our wire form carries
the selections themselves, so `filter: null` is the ordinary case rather
than an unrepresentable one, and all four executors carry it (witness
`jutterswet-keurunie`, its second rule). Refusing it would have been
importing a code generator's limit as a normative rule.

*How the OR-filter spelling relates to this one — it is not a second
spelling of it.* A single-source count narrowed by a composite OR condition
("het aantal aangemelde vondsten van de Jutter die gemerkt of gekeurd zijn")
is an ORDINARY count with an ordinary `filter`; it was expressible before
this decision, every executor already carried it, and nothing here changes
it. It happens to have the same EXTENSION as the union of two selections
over one range, which is a fact about those two sentences and not a wire
identity: the sentences differ, the nodes differ, and a producer emits the
node its sentence spells. So the maintainer's second option — "voor dezelfde
bronrange, één telling met een OR-filter" — is not normed as a union form.
The RegelSpraak frontend emits the union for every §5.7 concatenation,
including the equal-range case, so one written form keeps one wire form. The
price of the choice is a node; what it buys is the general case, since two
selections over DIFFERENT relations ("aangemelde vondsten van de Jutter en
geborgen vondsten van de Jutter") have no OR-filter spelling at all, and it
is exactly there that deduplication stops being automatic and becomes an
operation each executor must get right.

*No `sourcePath` at a selection, and that is a boundary rather than an
omission.* A multi-step range under a COUNT is where WF-093 and WF-102 meet
(ch. 3.4b, consequence 2; until L2 WF-096 and WF-102): a path is admissible
only as the range of a value fold's set source, and a count reads a count
source over a one-step range, so neither spelling of a counting fold over a
union of intermediates survives. Admitting a
`sourcePath` at a selection would settle that question by side effect — the
union becoming the back door through which a refused single-source shape
returns — and one card draws one boundary. A selection therefore carries a
one-step `sourceRole`, required, and the multi-step question stays exactly
where 2026-09-19 and 2026-09-11 left it. Named open flank, not a closed one.

- **WF-110** An `InstanceSetUnion` (ch. 4.3): **(a)** is admissible ONLY as
  the single argument of a `count` aggregation — at every other position
  the document is ill-formed (`wf110-union-as-rule-value`,
  `wf110-union-under-a-builtin`). At the source of any other fold that
  carries an `elementType`, WF-037's fold-source arm names the union as the
  operand that is no source, and the clause steps aside there as it does
  for (c) under a count, so the node is reported once
  (`wf037-sum-element-type-union`). The clause is about the UNION node: a
  COUNT over a union is an ordinary whole-number expression and stands
  wherever any other count stands, a `condition` included
  (`jutterswet-unievoorwaarde`); **(b)** carries at least TWO selections,
  since one selection IS the ordinary count and already has a spelling
  (`wf110-union-a-branch`); **(c)** the count carrying it names no source of
  its own — no `elementType`, `sourceRole`, `sourcePath` or `filter` on that
  node (`wf110-union-beside-fold-source`; since L2 an `elementType` on a
  single-argument count comes with a range, B12, and the clause names both;
  since 2026-09-25 `ref`, the ASP backend and the Drools loader refuse the
  count in the rule's sentence, where before `ref` counted the union beside a
  range alone and both Drools branches played every such count);
  **(d)** every selection names the SAME
  `elementType`, and it is declared (`wf110-union-unequal-element-types`;
  an undeclared one is WF-034 at the new position); **(e)** every selection
  names its `elementType` (`wf110-union-branch-without-element-type`), and its
  `sourceRole` resolves under WF-047 at the new position, as WF-102 (d)
  applies it per step. Since L2 (2026-09-24, B12) `sourceRole` is
  required and non-null in the schema and every loader, so (e)'s arm for a
  missing relation is enforced at load; its witness
  `wf110-unie-tak-zonder-bronrol` was removed with reason. Semantics:
  `RUNTIME-SEMANTICS.md` ch. 3.4d.

**The fold source's RANGE is a PATH (WF-102).** *(Maintainer decision
2026-09-11.)* A set source describes its range with ONE relation:
`elementType` plus `sourceRole`. A written range that stacks two
relations — "het gewicht van alle geborgen vondsten van alle gevoerde boten
van de Jutter" — has no form on that wire, and the two spellings it can
degrade to are both wrong: dropping the outer step folds a DIFFERENT set
(and where a fact type happens to relate the subject to the element type
directly, the degraded document is well-formed and every executor agrees on
the wrong answer), while a fold nested inside a fold has no binder that
hands the outer element to the inner source and is refused by WF-047 and by
both OCaml executors. The range is therefore generalised, not the fold: one
relation is the one-step case of a PATH.

*The wire form.* `sourcePath` is a list of `RoleBinding`s, subject-outwards:
step 1's opposite role is filled by the fold subject, each further step's
opposite role is filled by the type its predecessor's bound role reaches,
and the LAST step's bound role is filled by the `elementType`. That is
the orientation and the discipline of an attribute reference's `path`
(ch. 4.1a) and of
`sourceRole` itself, so WF-047's resolution is reused step for step rather
than restated. Two fields, one meaning, so exactly one spelling per meaning:
a one-step range IS `sourceRole` — the spelling every existing document
carries and every executor already reads — and `sourcePath` therefore
carries **two or more** steps and never stands beside `sourceRole`.

*The reading is the FLAT UNION, and it is normative.* The folded set is the
set of `elementType` instances REACHABLE from the fold subject along
the whole path: gather step 1's linked instances, gather step 2's from each
of those, and so on; the result is a SET — an element reachable through more
than one intermediate instance contributes ONCE. The alternative reading —
fold per intermediate instance, then fold the results — is NOT a desugaring
of this one and is deliberately not offered, because the two disagree in
three places:

- **MIN/MAX/FIRST/LAST.** They coincide while every inner set is non-empty
  (minimum is associative over a union) and diverge as soon as one
  intermediate instance reaches nothing: the flat reading ignores that
  instance, the nested reading produces an EMPTY inner result for it, and
  min/max/first/last have no `emptyFallback` to absorb it (ch. 1.2) — so the
  nested reading must decide what an empty contributor does to the outer
  fold, and the flat reading never asks.
- **COUNT.** They do not coincide at all. A count over the flat union counts
  ELEMENTS; a count of counts counts INTERMEDIATE INSTANCES. On the worked
  example: three finds against two boats. Like the sum's below, this divergence
  is **named and pinned nowhere** — and since 2026-09-19 for a reason stronger
  than "no carrier": a count over a multi-step range is **not expressible as a
  `count` FOLD**. A `sourcePath` is admissible only as the range of a VALUE
  fold's set source (clause (c) below), and a count reads a count source,
  whose range is one step (WF-093 (b) refuses a count over a member read —
  until L2 the set flag under `AANTAL` was WF-096's, the maintainer decision
  that took the form off the wire after `ref` and the Drools backend were
  measured reading it two different ways). The two fences meet at this NODE:
  a count over a path with a count source is this rule's (c), with a member
  source WF-093 (b)'s, so neither spelling of a counting FOLD survives.
  The scope of that sentence is this node and not the IR: whether some OTHER
  node counts over a multi-step range is that node's own question under its own
  rule, and a claim about it would need its own witness before it could be
  written here. The quantifier fold (ch. 4.5) is such a node, and for its
  `atLeast` and `atMost` arms it has that witness: they take a multi-step
  range under WF-108 (c) and count over it FLAT, an element reached along
  two intermediate instances satisfying the matrix once. In
  `jutterswet-ketenkwantor` both alternative readings are ruled out: a
  count of join tuples turns the verdicts over where the shared element
  makes the flat count 3 and the tuple count 4, and the count of counts
  above — INTERMEDIATE INSTANCES — turns them over where two boten reach
  four elements; a second subject in one world pins the range itself.
  Replayed by gates 5 and 6. The `exactly` arm is not witnessed over a
  multi-step range.

  The flat-union reading stated here therefore remains the reading — it is
  what ch. 3.4a's worked example says
  and what `RUNTIME-SEMANTICS.md` ch. 1.2e demands of a count — and it waits
  for a carrier that a normatively defined counting operation would give it
  (`backlog/new/20260919-aantal-gevulde-gemiddelde.md` asks the neighbouring
  question for the VALUE count and the average).
- **SUM with an `emptyFallback`** ("of 0 als die er niet zijn"). The flat
  reading fires the fallback at most once, for a subject that reaches no
  element at all; the nested reading fires it once per empty intermediate
  instance. With a zero fallback the totals coincide numerically, so the
  divergence is invisible in the corpus today; with any other fallback it is
  not.

The flat union is what the sentence says ("alle geborgen vondsten van alle
gevoerde boten") and what a join produces. The nested reading is a different
construct and would need its own node, not a second reading of this one.

*Depth is UNBOUNDED (two or more steps), decided rather than assumed.* A cap
would be a constant in a normative rule with nothing behind it: the union is
defined by induction on the list, every consumer walks the list as a fold,
and no clause above degrades at three steps. A cap also only ever loosens —
raising it later turns documents that were ill-formed into well-formed ones,
which is a wire-format event — so a cap not taken costs nothing and a cap
taken costs that. The v0 toolchain caps its own chain at two because it
emits one join PATTERN per step and wanted the code-generation blast radius
bounded; that is a property of a code generator, not of the range, and uchar
consumes the list symbolically. The corpus pins two steps and three steps
(`jutterswet-bootberging`).

*A TO-ONE step is admitted, and that closes the question the acute flank
left open.* A role's declared multiplicity does not enter the reading: a
step through a to-one role contributes its zero-or-one linked instance to
the union exactly as a to-many step contributes its zero-or-more. Refusing
it would need a new arm keyed on `many`, which no consumer reads in
this position, to refuse a shape whose semantics is already fixed by the
clause above. So "alle vondsten van de boot van de Jutter" — one to-one step
and one to-many step — is a two-step `sourcePath`, not a refusal.

*The written ANCHOR may be a role name, under the order ch. 4.1a already
fixes.* The wire never carries the written anchor; it carries the path,
whose first step starts at the fold subject by WF-102. What a producer must
decide is whether a written anchor that names a ROLE rather than an object
type ("de minimale waarde van het gewicht van alle vondsten van **de
aanmelder**") denotes the subject or another instance. The answer is the
test that already exists and the ORDER that already governs it (ch. 4.1a;
until L2 also WF-083 (b)): far-role resolution FIRST — a role reachable from the subject
whose filler is some other instance is a HOP, and belongs on the path as a
step — and the self-denoting reading only at zero far hits, where the name
matches (ci, declared name or declared plural) a role the SUBJECT TYPE
itself fills, and then the path simply starts at the subject. The v0
resolver refuses every role anchor outright, on the ground that a role
whose filler happens to be the subject's own type denotes a DIFFERENT
instance; that ground is real and it is exactly what far-role-first already
catches, so uchar refuses the same shape without refusing the shape beside
it. Measured over the four zones (2026-09-11): of 56 member folds, **six**
write an anchor that names no object type — two in the negative zone
(`wf093-wratvouw-set-som`, `wf093-wratvouw-set-maximum`, both already
rejected on their kind) and four in the parity fixtures — and every one of
them is a one-step range whose anchor is self-denoting, so the decision
moves no document. The producer's own witness is a frontend test, since the
form's wire result is the one-step `sourceRole` it already emits.

Semantics and worked examples, including the three empty corners:
`RUNTIME-SEMANTICS.md` ch. 3.4b.

*Steps after the elements are no range (2026-10-02).* A read through to-one
steps after a fold's elements ("het gewicht van de weging van de vondst van
alle jutters van de Ploeg") is no range and no navigation on the fold source
(WF-053): a producer spells it as a member of the element type that a rule
sets from the chain, and folds over that member (the RegelSpraak frontend
generates both, `frontends/regelspraak/grammar/DIVERGENCES.md`, "A fold over
a set and to-one steps after it"). A sum or a filled count over it then adds
or counts per element, an object two elements reach once for each; the flat
union of RUNTIME-SEMANTICS ch. 3.4b is the reading of a range whose last
step reaches the objects the fold reads, and a producer writes the shared
object as a set step to have it.

- **WF-102** A value fold source's multi-step range is a `sourcePath`, and
  it is a CONNECTED path from the fold subject to the element type
  (ch. 4.3).
  (a) *(Enforced at load since L2, 2026-09-24.)* `sourcePath` and
  `sourceRole` do not both appear — the one-step range is `sourceRole`,
  and one meaning has one spelling; since L2 the schema and every loader
  refuse a fold carrying both (B12), and the clause's witness
  `wf102-bronpad-naast-bronrol` was removed with reason.
  (b) A present `sourcePath` MUST carry at least TWO steps; the empty list
  and the single step are ill-formed.
  (c) `sourcePath` is admissible ONLY as the range of a VALUE fold's set
  source: the single argument of an aggregation of one of the seven
  value-folding kinds, which the range makes the set source (the reading
  of the source, (i)). On
  a listed value fold and on the count source — whose range
  is one step, `sourceRole` — it is ill-formed; on a builtin it cannot be
  written since L4 (delta B21).
  (d) Every step MUST resolve (WF-047) and the steps MUST JOIN: step 1's
  opposite role is filled by the fold subject, each further step's opposite
  role is filled by the object type its predecessor's bound role reaches,
  and the last step's bound role is filled by the `elementType`. A
  path that starts elsewhere, breaks in the middle, or lands on another
  type is ill-formed. No range is derived since L2 (B12), and a path could
  not have been: a type-pair derivation answers for a type PAIR, and a path
  is not one (its intermediate types are its own claim, not a derivable
  consequence of its endpoints).
  Companion: WF-083 (c), whose anchor is unmoved by the path and whose
  intermediate-type witness is named there.

**BuiltinCall** — scalar, text, and date builtins with plain arguments:
`squareRoot`, `absoluteValue` (core kernel; each takes one number, WF-038);
`concatenation` (text fold);
`duration`/`absoluteDuration` (two date operands, result unit declared);
`timeProportionalPart` and `dayCount` (each carrying a `granularity`
bucket ∈ `month`/`year` — `timeProportionalPart` MUST carry a gedurende
condition in `filter` and one temporal source argument as coarse as or
coarser than its bucket, and on a temporal target the bucket MUST equal
the target's declared granularity, WF-056; semantics ch. 3.7 of
`RUNTIME-SEMANTICS.md`); `easterSunday`, `dateWith`, `dateTimeWith`, and the
seven date/time part extractors `yearOf`, `monthOf`, `dayOf`, `hourOf`,
`minuteOf`, `secondOf` and `millisecondOf` — the date library is
`layer: dialect(regelspraak)` (row on date builtins, `CONSTRUCTS.md`
ch. 2.1); semantics in `RUNTIME-SEMANTICS.md` ch. 2–3.

| Field | Meaning |
|---|---|
| `kind` | the seventeen builtin kinds above |
| `arguments` | the operands, plain expressions: a builtin has no set source |
| `filter` | Condition? — the gedurende condition of `timeProportionalPart` and `dayCount` (WF-056, WF-058) |
| `granularity` | the bucket? — one of the five granularity values of ch. 2.2, `day`, `week`, `month`, `quarter` or `year`: required on `timeProportionalPart` and `dayCount` and admitted on no other kind (WF-037) |
| `type`, `source` | — |

v0 values: `WORTEL` → `squareRoot`, `ABSOLUTE_WAARDE` → `absoluteValue`,
`CONCATENATIE` → `concatenation`, `TIJDSDUUR` → `duration`,
`ABSOLUTE_TIJDSDUUR` → `absoluteDuration`, `EERSTE_PAASDAG` →
`easterSunday`, `TIJDSEVENREDIG_DEEL` → `timeProportionalPart`,
`AANTAL_DAGEN_IN` → `dayCount`, `JAAR_UIT` → `yearOf`, `MAAND_UIT` →
`monthOf`, `DAG_UIT` → `dayOf`, `UUR_UIT` → `hourOf`, `MINUUT_UIT` →
`minuteOf`, `SECONDE_UIT` → `secondOf`, `MILLISECONDE_UIT` →
`millisecondOf`, `DATUM_MET` → `dateWith`, `DATUMTIJD_MET` →
`dateTimeWith`; `granularity` has the v0 values of ch. 2.2 (`DAG` …
`JAAR`). Until L5 this chapter spelled the kinds in Dutch with hyphens
(`wortel`, `absolute-waarde`, `tijdsevenredig-deel`, …).

An element type, a range, an empty fallback and a dimension selection are
an `Aggregation`'s fields, and `granularity` is a `BuiltinCall`'s alone, so
a facet on the other node cannot be written: the schema and every loader
refuse it (delta B21). *History:* v0's one node carried the union of all
facets, with two convenience constructors defaulting the rest; the
accumulate-pattern framing in its documentation was the known leak
(row 1). The L4 migration split it by kind category and carried the
facets over one-to-one, refusing a document whose facet stood on the other
node (three negative documents, removed with reason). A v0 count whose
subject happens to name a declared "aantal …" parameter is re-read as a
ParameterReference by the frontend — surface disambiguation, not IR.

**DimensionCellRead** (`attribute`, `labels`, `source`) — dialect
(row 28): a single-cell read of a dimensioned attribute; every dimension
pinned (WF-040); no storage layout implied. `attribute` is an attribute
reference and nothing else — neither absent nor any other expression node
(WF-034, ch. 4.6 (g)). The cell's type is the dimensioned attribute's
datatype. Since L2 (2026-09-24, B13) the node carries no `type` of its own:
the declaration is its one source (until L2 its own `type` was a use site
of that declaration under WF-089, ch. 2.4).

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
| `base` | expression | names the timeline: an own temporal attribute reference, or a granular parameter reference. Never evaluated as a scalar (that is the refused ch. 3.3 shape) |
| `date` | expression | the day-precision date index |
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

- **WF-049** The `base` MUST be a navigation-free reference to a
  declared temporal attribute of the subject type or to a granular
  parameter (a temporal *characteristic* operand is **OPEN** — the case
  format carries no characteristic timelines yet); the `date` operand
  MUST carry the day-precision date type (millisecond precision is
  ill-formed); the node's `type` MUST equal the timeline's element
  datatype.

### 4.4 Unresolved

**Unresolved** is the honest-inexpressibility marker (row 16, permanent
contract): an expression variant carrying an optional `type` and the
source slice, and a condition variant carrying the source slice. Both
variants MUST carry `source` (ch. 4): its `snippet` is what WF-113 (b)
looks for in the producer's diagnostics, so a marker without one cannot be
accounted for. The schema requires it and every executor refuses a marker
without one at load; the verifier loads such a marker so that WF-113 (b)
names it. The node has been `Unresolved` on the wire since L5 (v0
`Onopgelost`, which carried the source slice as its `bron` string). The
expression variant's `type` is absent when the type is unknown and, when
present, a real datatype — never null; there is no declared-unknown
type (ch. 2.4). A well-formed IR MAY contain Unresolved nodes; what it may
never do is hide them (WF-044, unchanged). **Landed L2 (2026-09-24, B16;
the L2 card, `backlog/done/20260923-l2-referentielaag.md`):** until L2 the expression variant carried the unknown-type node (or
null) as its `type`; the migration removed it, and a real datatype stays
(`corpus/red/wf044-unresolved-reached` carries a number datatype, `Numeriek` until L3, and keeps it).
Every reader takes an absent `type` as unknown, never as a default
datatype. **Delta (row 16):** v1 adds a structured `reason` (the
four-way taxonomy of `CONSTRUCTS.md` ch. 4: structural, declared-names
refusal, spec-underdetermined, safety refusal) — **OPEN** until the
negative corpus fixes the taxonomy's values.

### 4.5 Conditions

| Node | Fields | Notes |
|---|---|---|
| Comparison | `op`, `left`, `right`, `wholePeriod?`, `source` | operator set (ch. 2.2 inventory): ordering `less`, `lessOrEqual`, `greater`, `greaterOrEqual`; equality `equal`, `notEqual`; date ordering `earlier`, `earlierOrEqual`, `later`, `laterOrEqual` — the date ordering keeps words of its own, as in v0; v1 may unify date ordering under typed ordering — carried as-is in the mirror. v0 values: `KLEINER` → `less`, `KLEINER_OF_GELIJK` → `lessOrEqual`, `GROTER` → `greater`, `GROTER_OF_GELIJK` → `greaterOrEqual`, `GELIJK` → `equal`, `ONGELIJK` → `notEqual`, `EERDER` → `earlier`, `EERDER_OF_GELIJK` → `earlierOrEqual`, `LATER` → `later`, `LATER_OF_GELIJK` → `laterOrEqual` |
| Predicate | `kind` ∈ `empty`, `filled`, `elevenTest`, `exactDigits`, `dayKind` (with the field `dayKind`, the day kind tested; absent on every other kind); `digits` (always written; read only under `exactDigits`), `operand`, `negated`, `wholePeriod?`, `source` | `empty`/`filled` are core kernel (three-valued rules in `RUNTIME-SEMANTICS.md` ch. 1.2); the other three are dialect builtins. v0 values: `LEEG` → `empty`, `GEVULD` → `filled`, `ELFPROEF` → `elevenTest`, `NUMERIEK_MET_EXACT` → `exactDigits`, `DAGSOORT` → `dayKind`; until L5 this table spelled them empty, filled, elfproef, exact-digits and day-kind |
| CharacteristicCheck | `characteristic`, `navigation?` (one to-one hop, a RoleBinding), `negated`, `host?` (the object type the check lives on; absent = the subject), `wholePeriod?`, `source` | **Delta (`CONSTRUCTS.md` ch. 2.2), landed at L5 (delta B10):** `host` is the check's GASTHEER — the object type the check lives on — and the wire says so by name: it is v0's `eigenaar` field, and the DELTA was the spelling, not the field (`proposals/V1-WIREMIGRATIE.md`). An absent `host` is the subject (`RUNTIME-SEMANTICS.md` ch. 5.8b). The field carries three obligations: **WF-034 requires a present `host` to name a declared object type** (ch. 4.6 (q)) — it is the node key of the dependency edge the check contributes, and a bean-model executor re-attributes an unresolvable one to the subject in silence; **WF-100 requires the named type to DECLARE the characteristic** (ch. 4.6 (q1), maintainer decision 2026-09-10), the field and not the subject being the key of every declaration a reader consults; and **WF-109 requires it to DESIGNATE exactly one of the objects bound at the check's position** (ch. 4.6 (q2), maintainer decision 2026-09-19): exactly one of them satisfies it (`T ⊑ O`) and no second one overlaps it (some object could be both). One object everywhere, two at the guard of a fact creation (the anker and the doel), so that every executor reads the object the field names instead of refusing it (`RUNTIME-SEMANTICS.md` ch. 5.8b). Inside a `gedurende` clip the field is not fenced but REFUSED — WF-097 (below) — because the clip reads the subject's own characteristic timeline and consults no owner at all. **Landed (2026-08-18):** the navigated check carries its one hop symbolically in the `navigation` extension field (`navigatieBinding` until L5; ch. 4.1a, WF-053); semantics `RUNTIME-SEMANTICS.md` ch. 5.8 — an absent link is false, negated or not. **Landed L2 (2026-09-24, B11; the L2 card):** the written hop name, the `navigatie` field, left the wire; `navigation` IS the hop, and a check without it reads the subject itself (the migration derived the binding for the four checks, in three documents, that carried only the written name) |
| RoleMembership | `factType`, `role` (the role the subject plays), `counterpartType?`, `negated`, `source` | **Delta (row 5), landed L2 (2026-09-24, B9):** one existential node, wire `RoleMembership {node, factType, role, counterpartType?, negated, source}` (keys in that order; `bron` until L5), replaces v0's near-side and far-side role checks; true iff a link of the declared fact type `factType` exists in which the subject fills `role`, named by its declared name, and the filler of the OTHER role of `factType` is an instance of `counterpartType` (`negated`: no such link exists). `counterpartType` is optional: it names a declared object type ⊑ the declared filler of the other role, and absent it IS that declared filler. The relation is ⊑, so a `counterpartType` equal to the filler is well-formed and means what its absence means; a producer writes the field only where it narrows, to a strict subtype, and the migration and the emitter write it only there. The far side's top-level-only restriction is dropped with it. WF-034's role arm crosses the node (ch. 4.6 (o)): the fact type is declared, `role` is one of its roles by declared name, the subject fills that role, and a written `counterpartType` is a declared object type ⊑ the other role's filler. *Migration:* `counterpartType` comes from the far-side node's `filler` and the near-side node's filler type where either named a strict subtype of the other role's declared filler, and from nothing else; `corpus/golden/jutterswet-postbezetting` carries it on its two Strandjutter checks. *(History until L2:)* the near-side node (`Rollidmaatschap`) resolved through a filler type alone (`anderTeFiller`; its written role was presentation) and the far-side node (`VerreRollidmaatschap`) through a filler and a back-reference (`filler`, `backref`) that had to meet on one declared fact type (ch. 4.6 (t), 2026-09-20, first carried by `corpus/golden/jutterswet-postbezetting`); the migration derived `factType` and `role` with exactly those two derivations and refused where they found no single candidate |
| Compound | `quantifier` ∈ `all`, `none`, `the`, `atLeast`, `atMost`, `exactly`; `threshold` (always written; read only under `atLeast`, `atMost` and `exactly`), `parts`, `source` | `all`/`none`/`the` are kernel; the counting forms are sugar (count-and-compare, row on `Kwantor`, `CONSTRUCTS.md` ch. 2.2) — mirrored as quantifiers. Quantifies over **parts of one subject** — propositional. Its first-order sibling is the row below. v0 values: `DE` → `the`, `ALLE` → `all`, `GEEN` → `none`, `TEN_MINSTE` → `atLeast`, `TEN_HOOGSTE` → `atMost`, `PRECIES` → `exactly`, the same for both quantifier families; until L5 this table spelled them all, none, the, at-least, at-most, exactly |
| QuantifierFold | `quantifier` ∈ `all`, `none`, `atLeast` (`threshold`), `atMost` (`threshold`), `exactly` (`threshold`), `elementType`, `filter?`, `test`, `sourceRole?`, `sourcePath?`, `source` | **extension node**, `layer: core`, on the wire as `QuantifierFold` since L5 (v0 wire node `Kwantorvouw`, with `kwantor`, `drempel`, `elementObjecttype`, `toets`, `bronRol` and `bronPad`): quantification of a condition over the members of a linked **instance set** — "…indien de `<attribuut>` van ALLE `<leden>` van de `<onderwerp>` gedurende het gehele jaar groter is dan `<grens>`". The range fields are the instance fold's (ch. 4.3) unchanged — `elementType` is the POPULATION extensionally (ch. 2.2), `sourceRole`/`sourcePath` the REACH, `filter` the die/dat subselection evaluated per contributor — and `test` is the quantified matrix, evaluated per contributor with the element as subject. The counting quantifiers are the row above's, over instances instead of parts, and they carry that row's `threshold` under the same rule: the verdict COUNTS the contributors whose matrix holds and compares that number with it. WF-108, WF-041; semantics `RUNTIME-SEMANTICS.md` ch. 3.9a |
| TimeWindow | `from?`, `to?`, `toInclusive`, `source` | from inclusive, to exclusive unless `toInclusive`; never both absent; each bound a date literal or the subject's own scalar date attribute (WF-111); never under `none` or a counting quantifier (WF-112) |
| RuleStatusCheck | `rule`, `inconsistent`, `source` | both statuses stay (maintainer decision 2026-09-19): "is gevuurd" carries its v2.3.0 meaning — a global, monotone status that becomes true once the named valid rule version has fired for at least one subject, a no-op firing included — and "is inconsistent" waits on the §9.5 channel. The v1 node form waits on a golden with full ref ≡ clingo ≡ Drools parity on that meaning, so the wire keeps its v0 node `Regelversie` until then, with its v0 fields `regel` and `inconsistent` and, since L5, its source text in `source` (v0 `bron`) — mirrored here because v0 dumps carry it. *History:* until 2026-09-24 this row called "is gevuurd" a v1 non-goal, following the row-30 split verdict |
| Unresolved | `source` (required) | ch. 4.4 |

The **whole-period qualifier** — the field `wholePeriod?` — attaches to
Comparison, Predicate, and CharacteristicCheck only —
`layer: dialect(regelspraak)`, lowering onto timeline algebra (row 29;
`RUNTIME-SEMANTICS.md` ch. 3). Its value is a **WholePeriod** node:
`granularity` ∈ `year`, `month` (the period the qualifier spans) and
`negated` (boolean). An absent `wholePeriod` is no qualifier. v0 wrote the
node as `GeheelPeriode {jaar, niet}`, the wire form until L5, with the
boolean `jaar` for the granularity — v0 values: `true` → `year`, `false` →
`month` — and `niet` for `negated`. One position is carved out of that
attachment, and one field beside it, by WF-097 below.

- **WF-097** A **`gedurende` clip member** — a CharacteristicCheck
  standing as the condition of a `Function`'s `filter` where that filter
  is a **clip**, whether directly or as a member of the flat `all`
  composite — MUST carry **neither an owner (`host`) nor a
  whole-period qualifier**, the negated qualifier included. Two arms of
  one rule, both closing the same thing: a qualification the reading has
  no room for.

  **All THREE clips, exhaustively.** The same node sits in the same
  field at each, so the rule names them as a closed list:

  | clip | kind | its own shape rule | `RUNTIME-SEMANTICS.md` |
  |---|---|---|---|
  | the temporal total | `total` over an own temporal attribute | WF-055 | ch. 3.6 |
  | the pro-rata fold | `timeProportionalPart` | WF-056 | ch. 3.7 |
  | the day count | `dayCount` | WF-058 | ch. 3.8 |

  The **fourth** reading of `Function.filter` — the die/dat instance
  subselection — is **not** a clip and is deliberately **not** fenced.
  There the condition is a per-contributor truth test, and the two
  fields land where they land for the scalar check generally, not in
  some better place: the whole-period qualifier is already refused
  (WF-043 reaches it, the operand being non-time-dependent), and the
  owner was **silent** — measured verifier-clean with both executors
  ignoring the field. That silence was real and is NOT closed here; it is
  the scalar-owner question (the `host` field, v0 `eigenaar`), which is a
  semantic one (WHICH owner is
  authoritative, the field or the subject) rather than the
  no-reading-exists one this rule answers. It was answered separately on
  2026-09-10, in the field's favour: ch. 4.6 (q1), WF-100. Fencing it on
  this rule's reasoning would have answered a different question by
  accident. That the subselection shares the field is also why the
  total's clip is identified by SHAPE (WF-055's guard) rather than by
  kind, while the other two are identified by kind alone.

  Each of the three artifacts that reads a clip — the verifier, the
  reference interpreter and the ASP backend — implements this fence
  **once**, in a single shared helper, and reaches all three clips from
  two or three call sites depending on how that artifact already
  discriminates them. (The verifier needs two: its pro-rata and day-count
  arms are one kind-keyed branch, so one call covers both, and the
  temporal total's shape-guarded arm calls the same helper separately.
  The reference interpreter and the ASP backend each carry three, one per
  clip arm.) What the rule requires is the SHARED helper, not a
  particular number of calls: the rule first landed on the temporal total
  alone (2026-09-09) and the pro-rata fold and the day count went on
  answering silently for a day, which is precisely the drift separate
  copies invite.

  The clip is not a truth test on a subject at an instant; it is a
  timeline operation that reads THE SUBJECT's own characteristic
  periods over the WHOLE span of the value timeline it clips. An owner
  names a subject the clip never consults, and a whole-period demand
  re-states the span the clip already ranges over — which is why every
  reader matched both fields away and none of them ever said so.

  Measured on the pre-rule binaries, one field twisted at a time and
  nothing else changed. At the temporal total (2026-09-09,
  `jutterswet-tegenwacht`): 3 twists, each verifier-clean, each
  reproducing the untwisted answer in **36 of 36** executor replays (3 ×
  6 cases × {reference interpreter, ASP backend}). At the other two clips
  (2026-09-10, `anchor/jutterswet-borgdeel` for pro rata and
  `anchor/jutterswet-dagentelling` for the day count): 6 twists, all six
  verifier-clean, reproducing the untwisted answer in **60 of 60**
  replays. Ninety-six replays, ninety-six unchanged answers.
  `negated: true` was indistinguishable from an absent qualifier in all
  of them, so the arm refuses both polarities; a reader that honoured the
  affirmative form and dropped the negation would still be silently
  wrong.

  **Scope is exactly the clip member**, and the three neighbouring
  positions are left standing deliberately, each for its own reason.
  (i) The SCALAR characteristic check outside a clip keeps its
  `host` — there it is ordinary name resolution, WF-034 (q)'s
  business, joined since 2026-09-10 by WF-100 (q1), which crosses the
  named type against the characteristic it claims to own — a cross that reaches
  the NON-navigated form only — and since 2026-09-19 by WF-109 (q2), the
  hosting cross, which runs on both forms alike.
  (ii) That same scalar check keeps its whole-period
  qualifier: the frontend produces that form from source, so refusing
  it is a producer-cutting decision that needs its own card and its own
  measurement, not this rule's. (iii) The qualifier on a **Comparison**
  is untouched — it is implemented in a backend, the corpus carries 5
  such nodes, and widening WF-043 to swallow it would be
  over-rejection. WF-043's characteristic arm, note, cannot reach
  inside a clip at all: it fires only on a non-time-dependent
  characteristic, and WF-055 already requires a time-dependent one
  there. That unreachability is what left this position unguarded.

  Cost: none. Counted over the four zones before the rule (534
  documents): 39 clip filters across 25 documents holding 37
  characteristic checks, of which **0** carried an owner and **0** a
  qualifier; corpus-wide 193 characteristic checks, 3 with an owner (all
  three OUTSIDE a clip, all in the negative zone) and 0 with a
  qualifier. The §8.2 qualifier on a **Comparison** counted 5 nodes
  before the rule and counts 5 after it: that family is untouched, and
  every whole-period node added since (`GeheelPeriode` until L5) is on the
  now-forbidden
  characteristic position, in this rule's own witnesses.

  **What the two executors do NOT guarantee.** They fault on the same
  document with the same words, but they do not fault at the same TIME:
  the reference interpreter is lazy and the ASP translation eager. Put a
  violation inside a rule whose condition is false and the reference
  interpreter never reaches it — `oracle: PASS` — while the ASP backend
  refuses at translation. Measured 2026-09-10, and measured to be
  **inherited**: WF-055 splits identically on the identical construction,
  so this is a standing lazy-versus-eager property of the pair and not
  something this rule introduces. It is bounded, not resolved: the
  verifier rejects any such document, so it can never enter a gated
  zone, and gate 6 only ever compares verifier-clean documents.

**The quantifier fold (WF-108).** *(Maintainer decision 2026-09-16, the
heelperiode-forall card.)* The condition layer had propositional
quantification — `Compound`, over the PARTS of one subject — and no
first-order one. The set-ranging machinery lived on one node only, the
instance fold of ch. 4.3, and it produced a VALUE. So the official §8.2
sentence shape — a condition asked of every member of a linked set —
had nowhere to land, and the frontend's emission of it was cut by
WF-096 at the moment that rule was promoted to ERROR (WF-096 is history
since L2; the paragraphs below keep the rationale as it was measured on
the L1 wire, where the set reading was a flag on the reference).

**The node quantifies; the operand does not.** The alternative this
replaces is the one the v0 wire actually carried and the production
toolchain actually executed: the comparison itself, with a SET-valued
operand, quantified implicitly. It is rejected on a measured defect, not
on taste. `set: true` records only THAT a quantifier was written, never
WHICH — the frontend computes it as "the onderwerp's quantor is not the
bare definite", so `alle`, `geen van de`, `ten minste 2 van de` and
`precies 1 van de` reach the wire as one and the same node. A reader
that took the set flag for a universal would answer `all` for a sentence
that wrote "geen van de", silently and with a straight face. That is the
one-answer-and-no-question class this repository refuses, and no
exemption to WF-096 can repair it: the information is not on the wire to
exempt.

The second candidate — keep the comparison and record the quantor as a
field on the READ (`AttributeReference.quantifier`) — was rejected for
two reasons of the same kind. A binder would then sit on an operand and
scope over a condition two levels above it, which no other node in this
IR does and which leaves "which operand binds, when both sides are set
reads" with no answer; and the field would be meaningful in exactly one
position and inert everywhere else — on a fold source, `geen van de`
reads as nothing at all — which is the "a field nobody reads is a field
that can say anything" defect WF-083 (c) names.

**What it reuses, deliberately and exactly.** `elementType`, `filter`,
`sourceRole` and `sourcePath` are the instance fold's fields, with the
instance fold's meanings and the instance fold's rules. The contributors
are `reach ∩ population`, then the `filter` survivors, per
`RUNTIME-SEMANTICS.md` ch. 3.4a/3.4b — one definition, not two. The
verifier reaches them through the SAME helpers the fold's arms call
(`wf047_binding`, `resolve_binding`; until L2 also `wf048_derivable`,
which left with WF-048), on the WF-097
shared-helper discipline: what the rule requires is one engine, not one
call site. An unresolvable binding therefore still reports **WF-047**,
exactly as a fold's does, and only the shape questions this node
introduces carry the new number.

**`filter` and `test` are not the same field twice.** `filter` selects
WHICH members the sentence is about (the die/dat subselection, ch. 4.3);
`test` is what each selected member must satisfy. Both are conditions
over the same element subject, which is why they share machinery, and
they answer different questions, which is why they are two fields: "alle
bergers **die een keurmerk dragen** hebben gedurende het gehele jaar meer
dan 40" excludes the unkeurmerkte berger from the claim rather than
failing on him.

**The counting quantifiers, and why they need no new machinery.**
*(Maintainer decision 2026-09-16, the tellende-kwantoren card.)* The node
landed with `all` and `none` and named the counting quantifiers its
extension point. They are built now, and the build adds exactly one
field: `threshold`, `Compound`'s own, under `Compound`'s own rule. The
verdict COUNTS the contributors whose `test` holds — `atLeast` n is
count ≥ n, `atMost` n is count ≤ n, `exactly` n is count = n — which is
the count-and-compare `CONSTRUCTS.md` ch. 2.2 records for the
propositional sibling, over instances instead of parts. Nothing else
moves: the contributors are the same `reach ∩ population` after the same
`filter`, and `test` is the same per-member condition. **The empty set
needs no separate principle here**: a count of 0 answers every quantifier
by itself, so the vacuous reading the chapter states for `all` and `none`
is not restated for these three — it is what the comparison gives.

**`threshold` is present exactly where it is read.** It sits on the wire
only under the three counting quantifiers, and is ABSENT under `all` and
`none`. `Compound` writes it under every quantifier and reads it only under
the three counting ones, so under `all`, `none` and `the` it is a number
nobody reads. That divergence is deliberate and is the argument two paragraphs
up, applied to this node's own field: a field that is meaningful in one
position and inert everywhere else is the "a field nobody reads is a
field that can say anything" defect of WF-083 (c), and `Compound`'s shape
is a v0 mirror this node does not have to inherit. The absence is checked
(WF-108 (e)), not merely permitted.

- **WF-108** A `QuantifierFold` MUST state a quantification something can
  satisfy. Five arms.
  (a) `quantifier` ∈ {`all`, `none`, `atLeast`, `atMost`, `exactly`}. `the` is a
  definite description of one member and not a quantification over a set;
  it is refused BY NAME rather than ignored, for the reason the node
  exists: a reader that fell through to the universal would answer the
  opposite of what was written.
  (b) `elementType` is present, and names a declared object type. The
  presence is this rule's (a quantifier whose population is unnamed
  ranges over nothing a reader can name); the resolution is WF-034's,
  reported under that number at this position.
  (c) The RANGE is one meaning with two spellings: a `sourcePath` carries
  at least two steps, its steps form a connected walk from the
  quantifier's subject, and its last step lands on a role whose filler is
  ⊑-compatible with `elementType`; a `sourceRole` resolves and agrees
  under WF-047. Since L2 (2026-09-24, B12) the node carries EXACTLY
  ONE of the two, and the schema and every loader refuse it with both
  or with neither: the arm for both spellings at once is enforced at
  load (its witness `wf108-kwantorvouw-bereik-dubbel` was removed with
  reason), and there is no bindingless range to derive (until L2 WF-048
  demanded a uniquely derivable one). This is WF-102's discipline under
  this node's own number, and the number is new on purpose: WF-102 is
  scoped to a fold's ARGUMENT — the source position, which this node
  does not have — and a `corpus/negative/` directory must be able to
  declare the rule it violates.
  (d) `test` is present. A quantifier with no matrix states a range and
  asks nothing of it, and has no verdict to give.
  (e) `threshold` is present **exactly** under the three counting
  quantifiers: a counting quantifier without one names no number to
  compare the count with, and one under `all` or `none` is a number no
  reader consults — the field would then record a quantification the
  `quantifier` field contradicts, and a reader is entitled to trust
  either. Its VALUE is not this rule's: a threshold below 1 is
  **WF-041**, reported under that number at this position, because it is
  the same demand on the same vocabulary and the shared-helper discipline
  of (c) applies to a bound exactly as it applies to a binding.
  Semantics: `RUNTIME-SEMANTICS.md` ch. 3.9a.

**What this did NOT move** *(history until L2, when WF-096 left with the
set flag — the set source is since then the argument of a fold that
carries a range, ch. 4.3)*. WF-096 stood unamended and unweakened: a
set reading was still admissible only as an instance fold's source. The
reclaimed route does not become an exemption to it — the lowered
document contains **no set reading at all**, because the quantifier
binds the member and the matrix reads that member's own attribute.
Measured against 2026-09-09's census: **no census route moves** — all
eight keep the WF-096 ERROR verbatim, the unqualified rule condition
among them. What lowers is a NINTH, narrower shape the census did not
separate out, because until this node existed there was nothing to
separate it for: the condition carrying the §8.2 whole-period
qualifier. Widening the lowering to the unqualified condition is
possible with the same node and deliberately not done here — it has no
pinned witness in two of three executors (`RUNTIME-SEMANTICS.md`
ch. 3.12), and it belongs to whoever measures it.

### 4.6 Well-formedness

- **WF-034** Every reference in an expression or condition MUST resolve:
  attributes (with their full navigation path), parameters, variables,
  characteristics, roles and fact types, aggregation element types,
  dimension names and labels, day kinds, and referenced rules. The one
  scoped exception is the COUNT source's role name (ch. 4.3): the single
  argument of an aggregation carrying an `elementType` and a range, whose
  `attribute` names no member of the element type, writes the name of the
  role its range binds (ch. 4.3, the reading of the source, (ii), and
  WF-093's recognition), and WF-047 — not this rule — crosses
  that name against the range. **Outside** the count source a fold-source
  argument is an ordinary attribute reference and resolves like one
  (ch. 4.6 (l)); a role name at a value fold's source is WF-093 (a).
  *v0 mirror note:* fail-soft throughout — unresolved references degrade
  to the unknown-typed sentinel or an Unresolved node, warned at best.
  **Delta (promotion):** in v1, "could not resolve" is either an explicit
  Unresolved node (honest) or ill-formed IR (a dangling reference in a
  structured node) — never a sentinel type inside an otherwise-structured
  node; the unknown datatype does not exist in v1 (ch. 2.4). **Landed L2
  (2026-09-24, B13, B16):** the unknown-type node is no datatype any more
  and every loader refuses it by name, and a reference carries no type of
  its own; so what the rule still says about types is that a value node
  which carries its own annotation — a literal, a composite, a derived
  position — carries one: a plain null there is refused. A reference, a
  Lambda, an instance-set union and an `Unresolved` without a `type`
  carry none by construction. *(History until L2:)* the exception was
  scoped to the **v0 instance-fold source** — the row-1 wart, spelled on
  the wire by the unknown sentinel on the argument's own `type` and keyed
  on exactly that sentinel by the reference interpreter and the ASP
  backend (the Drools backend keyed the same reading on `kind = AANTAL`
  and its own role derivation instead) — and there a written name that
  resolved to no declared role still resolved through the type-pair
  fallback iff the type pair derived a unique connecting role; and the
  rule covered the unknown type in both its spellings — the explicit
  sentinel and a plain null — at a value node, at a rule target and at a
  fold operand alike (since the seventh wave's fix round, 2026-09-06),
  with two exemptions, a Lambda and the single argument of an
  aggregation. The waves below record how each arm was measured on the
  L1 wire; the arms whose fields left with L2 say so where they stand. The
  arms that stand name their fields in the L5 spelling; they were measured
  on the same fields under the v0 names the wire then carried
  (`objecttype`, `attribuut`, `kenmerk`, `eigenaar`, the `AttribuutRef`,
  `ParameterRef`, `DimensieCel` and `Kenmerk` nodes), which the measurement
  notes below keep.
  **Widened 2026-09-04** (maintainer decision), closing three measured
  channels through which a document could *serve itself* an exemption from
  the identity family (WF-089/WF-090/WF-091), each carried by **zero**
  documents — 152 golden + 22 red + 237 negative + 30 parity, measured —
  so no clause can over-reject. First: an
  `AttributeReference` MUST name the **`subjectType`** its value lives on
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
  kilogrammes with nothing refusing anywhere. Third *(history until L2 — a
  rule target carries no type since L2, B13, and the arm left with it)*:
  the sentinel was refused on a rule **TARGET's `type`** as well — the one position the expression
  walk does not enter (the lvalue is WF-022's), and the last place it
  survived outside the documented row-1 fold-source wart. No executor reads
  the target field (all three write
  through the DECLARED datatype), but the identity family crosses it
  (WF-091's rule-target arm) and defers on the sentinel, so writing it there
  switched the unit, precision and domain checks off one's own write
  targets.
  **Widened again 2026-09-04** (same decision, second wave), with the four
  shapes the first wave measured and left open — each, like the first three,
  carried by **zero** documents outside its own negative witness (152 golden
  + 22 red + 238 negative + 30 parity, measured before and after), so no
  clause can over-reject. (a) An `AttributeReference` MUST name the
  **`attribute`** it reads — the other half of the resolving pair, silencing
  the same rules from the other side, and needing the same TWO arms as the
  first half (the fold source never reaches the generic walk: with the
  value-position arm alone, 43 of the goldens' 751 emptied `attribuut`
  positions stayed verifier-clean, all of them fold-/aggregation arguments;
  with both arms, none of 751). (b) A `ParameterReference` MUST name its
  **`parameter`** — the same shape one node kind over (35 of 37 positions
  survived emptying, now none). (c) *(History until L2 — a target without a
  type is the L2 form, B13; the arm and its witness left.)* The unspelled
  twin on a rule **target**, a target with **no `type`** at all (295 of 295
  survived, now none): the
  first wave left it out for want of a witness, and it is the more dangerous
  half — besides WF-091's rule-target arm it also switches off WF-007's
  target clause, and the document it produces replays CLEAN in `ref` and
  clingo while the Drools backend throws on it, so the verifier is the only
  place it can be caught before an engine crashes on well-formed input.
  (d) *Removed at L2 (2026-09-24, B16) — history.* The unknown-type node
  is no datatype since L2, so no payload can carry it and every loader
  refuses it by name; the four witnesses (`wf034-sentinel-*-payload`) were
  removed with reason (`corpus/negative/README.md`, L2). The clause as it
  stood: the sentinel is refused on a `SumType` **constructor payload** inside a
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
  1 true instance-fold source), which stayed open at this wave — the
  aggregation branch replaces the generic walk for those arguments — and are
  closed by (r), the seventh wave. *Re-measured 2026-09-06 over today's
  corpus with the harness stated in full below:* **10** such survivors before
  it, every one of them a listed fold operand, and **none** after — the
  denominator is 2813 injections over 157 goldens, not wave 2's 2758 over a
  smaller corpus, so the two counts are not comparable and only the numerator
  carries the claim. *A tenth open position at
  the same branch, recorded 2026-09-05:* WF-038's ordering-fold clause
  defers on that same sentinel, so an instance ORDERING fold over a bare
  role source is verifier-clean by construction — and measured, no executor
  carries it (`ref` faults `value aggregation over a bare role source`, the
  ASP backend refuses the expression kind, the Drools backend throws a
  constraint-evaluation exception). Refusing it under WF-038 would refuse
  the ch. 4.3 wart itself, which is why the defer stands and this is
  recorded as a flank rather than claimed shut. One bookkeeping line
  belongs beside it: **no gate runs the verifier over the parity zone** —
  gate 3 walks the three corpus zones — so every verdict quoted here about
  a parity document is a measurement, never a pin. *Since 2026-09-25 (the
  parity well-formedness card) gate 11 does put the replay slices through
  the verifier — 28 of 29 clean, parity slice 24 held out by name on its
  WF-099 report — while the casus dump stays outside that leg; the sentence
  records the state on its date.* The second wave's descent covered the
  sum-in-sum chain only; the CONTAINER half is (h) below.
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
  all **462** documents that predate this card (153 golden + 22 red + 257
  negative + 30 parity) is unchanged by these arms; the **eleven** new
  negative witnesses the card and its fix round add — taking the negative
  zone to 268 and the four zones to 473 — are the only additions. The only
  pre-existing documents whose report changes at all are four minimal
  allocation negatiefs, which gain a truthful
  incidental line and keep both their verdict and the rule their directory
  declares. (e) A **characteristic check** (ch. 4.5) MUST name its
  `characteristic`. It is the only field the node resolves through, and behind it
  sit four readers at once — this rule's declared-characteristic cross,
  WF-043's granularity cross for a whole-period qualifier, WF-053's
  navigated hop, and the dependency graph's edge from the reading rule to
  the characteristic. 89 of the goldens' 96 check positions (twelve
  position shapes over five top-level sections) stayed verifier-clean when
  emptied one at a time, and none does now; `ref` and the ASP backend both
  fault at run time on the emptied field. (f) An **allocation** (ch. 3.2)
  MUST name its `sourceAttribute` and its `targetAttribute` (the v0 fields
  `bronAttribuut` and `doelAttribuut` until L4; the clauses on an allocation
  name the L4 fields, and their measurements were taken on the same fields
  under their v0 names). This is the one
  reference pair the expression walk cannot reach — an allocation is not an
  expression — so the clause lives beside the walk rather than in it; with
  either field empty, WF-025's ordering-key type check, WF-026's same-target
  comparison, WF-050's overlap with a defaults group and the stratification
  edges keyed on the pair all fall silent together. 11 of 11 golden
  allocations survived emptying either field, and none does now; both
  executors fault on the field's own name. The two wider movements this
  clause left open — a name that no declaration *carries* on any of the six
  fields, and an empty `sourceType`/`targetType` — are closed by (j) and (k)
  below. (g) A **dimension-cell read** MUST hold
  an attribute reference in its `attribute` position — refused in BOTH
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
  identity family's silence, not an engine divergence. (h) *(Removed at L2 with
  (d) — history.)* The **container half** of
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
  clause covers all SIX name fields at once — `sourceAttribute`,
  `targetAttribute`, `ratioAttribute`, `maximumAttribute`, `remainderAttribute`,
  `orderingAttribute` — with emptiness decided by the ch. 2.1 name identity
  (whitespace collapsed, lowercased), so a blank spelling is the same empty
  name. Absence keeps its meaning where it carries one: absence of the four
  facet fields (a null until L4) is a facet switched OFF and stays legal, and
  an absent `ratioAttribute` under `proportional` is already WF-025's business.
  *Measured, one field blanked at a time over the golden allocations, every
  position verifier-clean before and none after:* `sourceAttribute` 11 of 11
  and `targetAttribute` 11 of 11 and `remainderAttribute` 9 of 9, on all of which
  both executors fault loudly; `ratioAttribute` **7 of 7** and
  `orderingAttribute` **3 of 3**, on which BOTH executors play on and agree
  on a wrong answer (an ordered allocation loses its rank and pays every
  recipient the same 300 where 1200/0/0/0 is pinned); and
  `maximumAttribute` **4 of 4**, on which `ref` plays on uncapped and returns
  600 for a pinned 300 while the ASP backend refuses to translate — ref and
  clingo **diverging** on a document the verifier had called well-formed.
  Those three rows are the one place in this rule where the differential
  square is structurally blind and only the hand-pinned anchor expectations
  catch the defect, which is the whole argument for a document-layer fence.
  **Fourth wave 2026-09-04.** The naming class the first three waves kept
  measuring and leaving: not a field left empty, but a field **filled with a
  name that resolves against nothing**. (j) Every filled attribute name on an
  **allocation** MUST resolve against the declaration scope of **its own
  side** — `sourceAttribute` and `remainderAttribute` against `sourceType` (the pot
  that is distributed, and the remainder written back on the source),
  `targetAttribute`, `ratioAttribute`, `maximumAttribute` and
  `orderingAttribute` against `targetType` (the share written on each
  recipient, and the three facets read per recipient). (f) asks whether the
  field is there and (i) whether it is more than blanks; (j) asks the
  question the rule is about. An *undeclared* type is another rule's business
  (WF-047 on the bound form — since L2 the only form — and until L2 WF-048
  on the bindingless one, both **rules** measured loud — the executors are
  not: an undeclared `sourceType` leaves `ref`
  silent, every share *leeg* where 300/200/300 and a 400 remainder are pinned,
  while the ASP backend refuses; only an undeclared `targetType` faults in both),
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
  every position verifier-clean before and none after:* `sourceAttribute` 11 of
  11, `targetAttribute` 11 of 11, `remainderAttribute` 9 of 9 — all loud in both
  executors; `ratioAttribute` 7 of 7 and `orderingAttribute` 3 of 3 — BOTH
  executors silent and agreed (the shares stay underived where 60 and 40 are
  pinned; the ordering collapses and every recipient is paid the same 300
  where 1200/0/0/0 is pinned); `maximumAttribute` 4 of 4 — `ref` silent and
  uncapped (600 for a pinned 300) while the ASP backend refuses to translate.
  The observability rows are (i)'s exactly, one spelling on. (k) An
  **allocation** MUST name its `sourceType` and its `targetType`. The
  empty-reference class of (a)–(f), one field further along: an *undeclared*
  type is caught by WF-047 (and until L2 by WF-048), but those rules read a
  present name, and a null takes with it WF-025's ordering-key datatype, WF-050's defaults
  overlap, the dependency graph's read and write edges, and (j)'s own
  resolution scope. 11 of 11 golden allocations survived emptying either
  type, and none does now; both executors fault by the field's own name
  (`missing bronType`, `missing doelType`, the v0 field names of the time). (l) A **fold-source argument
  that is not the count source** MUST resolve as an ordinary attribute
  reference on its anchor (ch. 4.3, WF-083 (c)): since L2 (2026-09-24) the
  count source's written role name is WF-047's (ch. 4.3, the reading of
  the source, (ii)) and the type-pair
  fallback no longer exists (B12), so a name that is no attribute of the
  anchor is this arm's report ("is not a declared attribute of …"), and a
  role name at a value fold's source is WF-093 (a). *(History until L2:)*
  the ch. 4.3 role-name route and the type-pair fallback behind it were
  scoped to the v0 instance-fold source, which the wire spelled with the
  unknown sentinel on the argument's own `type`. Both branches were
  measured live and both are separately falsifiable. The type-pair branch, on
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
  **Fifth wave 2026-09-04.** The same naming class, carried off the
  expression walk entirely: three reference positions the walk never reaches,
  each measured verifier-clean and each with an executor that treats it
  differently from the other two. Over-rejection is settled by the same
  instrument: over all **493** documents of golden + red + negative + parity
  that PREDATE this card (154 + 22 + 287 + 30) not one verdict and not one
  report moves; the **six** new negative witnesses here (and a seventh under
  WF-028) take the negative zone to 294 and the four zones to 500. **Its fix
  round, 2026-09-05,** sharpened (m) with two further arms and its empty half,
  measured the same way: over all **500** documents that predate the fix round
  not one verdict and not one report moves either, and its three witnesses
  take the negative zone to 297 and the four zones to 503.
  (m) *Removed at L2 (2026-09-24, B7) — history.* The bean fields of a
  fact creation left the wire: the symbolic block is the whole creation,
  required by the schema and every loader (ch. 3.4), so a bean field is an
  unknown field and none can be misspelled. The seven witnesses
  (`wf034-feitcreatie-*`) were removed with reason
  (`corpus/negative/README.md`, L2). The clause as it stood: the four
  **bean-field names** of a fact creation (ch. 3.4) MUST be
  present and MUST resolve. `zetVeld`, `joinVeld` and
  `tussenstappen[j].veld` are the v0 half of the node — the fields a
  bean-model executor navigates and writes — and WF-028 crosses only their
  symbolic sisters, so all four sat beside a fully checked form spelling
  whatever a producer liked. They are not free-form: a binary relation lives
  in the v0 bean model as ONE SCALAR reference field, on the object type that
  fills the **to-many** role, named after the **to-one** role — the §13.3.9
  storage direction, which the Drools backend derives symbolically and cites
  by name in its own refusal reasons. Three requirements follow, each its own
  arm and each with its own witness. First, the name MUST be a **declared
  role** of the relation its symbolic sister names — `zetVeld` of `feittype`,
  `joinVeld` of `joinRol`'s fact type, `tussenstappen[j].veld` of that hop's
  `rolBinding`'s. Second, where that relation carries **exactly one to-one
  role**, the name MUST be that role: it is the only side the bean model
  derives a field for, so its sister names a field that exists on neither
  bean. At EQUAL cardinality the requirement stands down — two to-many roles
  imply no scalar field at all, two to-one roles imply one on each bean — and
  there either name is admissible; that is not a hedge but the corpus, since
  `anchor/jutterswet-erflijn` creates a both-to-many relation and
  `anchor/jutterswet-gemeenteberging` joins over one, and a rule that picked a
  side there would make two anchor goldens ill-formed for a choice no model
  makes. Third, the name MUST be the role's **declared spelling, byte for
  byte**: this is one of the places the ch. 2.1 case-insensitive *matching*
  is not enough (until M1 there was a second, (p) below, on a different ground —
  there the side-table key WAS a member's carried identity; here it is a name
  that must survive as a property), because the name becomes a Java property reached by getter and that
  lookup is case-sensitive (a first-letter difference is harmless — the
  identifier mapping lower-cases the initial — an internal capital is not).
  Only the declared name binds; the plural is presentation, as WF-047 already
  has it for the bindings. *(The fifth wave admitted both roles everywhere,
  reasoning that "which side the bean model stores is a bean-model decision
  the wire does not carry"; the backend refutes that — it derives the side
  from cardinality alone — and the corpus reading offered in support was
  mis-counted. Per CREATION it is two and two: two golden creations spell
  `zetVeld` with the anchor's role and two with the target's, since
  `anchor/jutterswet-erflijn` carries two creations. The anchor/target framing
  predicts nothing anyway — `jutterswet-postwacht` spells `joinVeld` with the
  OPPOSITE of its bound role, and that is precisely the to-one role — while
  the to-one reading explains all eleven golden positions at once. `joinVeld`
  being the bound role three times and the opposite once still holds.
  Corrected in the fix round of 2026-09-05.)* The fourth field,
  `tussenstappen[j].type`, is not a role
  name but the object type the hop lands on, and crosses against the hop
  itself: the filler of the role that binding binds, resolved with the same
  machinery as WF-028's chain walk; where the binding does not resolve this
  arm stays silent, because that report is WF-028's. It deliberately gets no
  byte-exact arm: its consumer resolves object types case-insensitively by
  documented contract, so such an arm would be stricter than all three
  executors and would have no witness. And all four MUST be **present**: the
  empty half of the same class, held by ONE list-driven arm over the four
  fields rather than four sub-arms, because with one witness three of four
  sub-arms move no document at all and a clause with no counterexample is not
  a clause. *Measured, one field at
  a time pointed at a name no declaration carries, over all 14 golden
  positions (4 `zetVeld`, 4 `joinVeld`, 3 `type`, 3 `veld`, over
  `anchor/jutterswet-erflijn`, `anchor/jutterswet-gemeenteberging` and
  `jutterswet-postwacht`):* 14 of 14 verifier-clean before, none after — and
  both OCaml executors replay all 14 to their pinned expectations, because
  they are **blind here by construction**: they read the symbolic fields and
  never touch the bean fields, so there is no divergence for the differential
  square to catch. The Drools backend does read them, reflectively, and on
  `jutterswet-postwacht` — a golden it plays — every one of the four throws it
  out of the engine (a missing getter on the target fact class via a
  consequence exception for `zetVeld`; a constraint-evaluation exception for
  `joinVeld` and for `tussenstappen[].veld`; a null dereference of the
  object-type lookup for `tussenstappen[].type`). Verifier-clean input that
  two executors answer and the third crashes on is the class an
  engine-neutral IR must not admit. *The fix round measured the two arms it
  added on the same golden, and both land in that class.* The **sister role**:
  `zetVeld` moved from `seinvak` to `vaksein` — a declared role of the created
  relation, so the wave's arm admits it — leaves the verifier, `ref` and
  clingo all clean while Drools throws `IllegalStateException: geen getter
  voor 'vaksein'`. The **spelling**: `seinVak` for `seinvak` gives the same
  three clean verdicts and `geen getter voor 'seinVak'`, while `Seinvak`
  (first letter only) plays without complaint — which is why the witness
  changes an internal letter, and why the byte-exact arm is recorded as being
  stricter than the executor on exactly that one shape. The **presence** arm's
  four absences were measured together: `tussenstappen[].veld` and
  `tussenstappen[].type` crash the engine with exactly the exceptions the two
  filled-name witnesses pin, while `zetVeld` and `joinVeld` make the backend
  drop the whole creation as a loud transpiler skip — a third executor
  behaviour again, neither faulting nor answering. Cost over the four zones:
  none. Of the 45 bean name positions the zones hold, 27 name their relation's
  single to-one role byte-exactly, 8 sit at equal cardinality, 4 sit on a fact
  type WF-028 already reports missing, and the remaining 6 are this family's
  own witnesses; every one of the 13 `tussenstappen[].type` positions is
  present. (n) An **object creation** (ch. 5.5) MUST
  name a declared `subject`. It is the population the creation runs over and
  the type every member value evaluates on; WF-027 crosses the CREATED type
  and stops there, and the subject was reached only by rules that read
  THROUGH it — WF-053 when a member value navigates, WF-047 when an explicit
  `connectingRole` names the back-link. That is incidental cover, not
  coverage, and it is measured: pointed at an undeclared name one creation at
  a time, 10 of the 11 golden object creations were already refused (all ten
  by WF-053, nine of those by WF-047 as well) and exactly ONE stayed
  verifier-clean — the only creation with no `initialisations` at all
  (`anchor/jutterswet-penninguitgifte`, creation 1). There neither executor
  faults: they AGREE on a wrong answer (`ref` and clingo both report 0 dossiers
  where the anchor pins 1), because the creation simply runs over an empty
  population. That is a row where the differential square is blind and only
  the hand-pinned anchor expectation stands between the corpus and the defect.
  *Still open, measured and named:* `subject: null` is equally verifier-clean
  and there both executors are loud (`missing ObjectCreatie.subject`) — the
  empty-reference class of the earlier waves, with no witness yet. (o) A
  **role check** — `RoleMembership {factType, role, counterpartType?,
  negated}`, since L2 (2026-09-24, B9) one node for both directions of
  ch. 5.8a — MUST name a declared fact type in `factType` and one of its
  roles, by declared name, in `role` (the plural does not name a role
  here, as in a role binding: `wf034-role-membership-role-as-plural`
  writes the declared plural of the role the subject fills), and the
  check's SUBJECT MUST fill that role: the subject type
  is ⊑ the role's declared filler, the one-sided near-side test every hop
  in this chapter uses. A role of the right fact type on the wrong side
  names a link the subject can never be in, and every name in it resolves
  (`wf034-far-role-check-wrong-side`, the red-first proof V4 (b)
  asked of this arm); an undeclared fact type resolves against nothing
  (`wf034-role-check-unknown-fact-type`). A written `counterpartType` MUST
  be a declared object type (`wf034-role-membership-filler-class-unknown`)
  ⊑ the declared filler of the OTHER role of `factType` (the reciprocal
  role is its own other end): a type outside that filler's chain names a
  link whose other end can never be one
  (`wf034-role-membership-foreign-filler-class`). The relation is ⊑, not
  strict: a `counterpartType` equal to that filler is well-formed and
  means what its absence means, and a producer writes the field only
  where it narrows (the migration and the emitter do exactly that). A position without a subject (a
  day-kind condition) crosses the declarations only, and WF-031 refuses a
  role-state read there at all (`wf031-day-kind-reads-role-state`).
  *(History until L2: the near-side node carried a written role that was
  presentation and a filler type the executors derived the connecting
  role towards; the clause as it stood follows.)* A
  near-side **role check** (ch. 5.8a) MUST name an `anderTeFiller` that some
  declared fact type relates its subject to. The written `rol` is
  presentation — the executors take the connecting role from the subject to
  `anderTeFiller` and null-test it — so that one field is the whole of the
  reference, and nothing crossed it. One cross, against the role fillers: the
  ch. 4.3 candidate list, the executors' own derivation; an EMPTY list is this
  arm's report, exactly `ref`'s fault, while a list with several entries stays
  WF-048's ambiguity question and a subject the walk does not know keeps the
  arm silent. *Measured over all four golden positions
  (`jutterswet-wachtindeling` 2, `jutterswet-wachtploeg` 2):* 4 of 4
  verifier-clean before, none after, with `ref` and the ASP backend both
  refusing loudly ("no fact type relates …") and the Drools backend taking a
  third road again — it stops carrying the rule (a transpiler skip), so the
  document neither faults nor answers.
  **Sixth wave 2026-09-05** (maintainer decision), the REST of the naming
  class off the expression walk: the field-by-field remainder the fifth wave
  and its fix round measured and left in the open list. Twelve arms, twelve
  witnesses, no new rule number and no documented deferral. Over-rejection is
  settled by the same instrument the fifth wave used, and the result is the
  same: over all **510** documents of golden + red + negative + parity that
  PREDATE this card (155 + 22 + 303 + 30) not one verdict moves, and exactly
  ONE report does — `wf031-day-kind-reads-role-state`, which gains a truthful
  incidental line from (o)'s empty half below and keeps the WF-031 verdict its
  directory declares. The twelve new negative witnesses take the negative zone
  to 315 and the four zones to 522. *Fix round 2026-09-06:* (p)'s identity is
  tightened from case-insensitive to byte-exact on the ground stated there, its
  thirteenth witness takes the negative zone to 316 and the four zones to 523,
  and the same instrument over all 523 documents moves exactly that one
  document and nothing else — no verdict and no report elsewhere.
  (p) *Removed at M1 (2026-09-23).* The eight side-table keys of an object
  type (`attribuutMeervoud` … `kenmerkTijdlijnAantal`) had to name a declared
  member, byte for byte. The side tables left the wire at M1: each facet is a
  field of its member's node (ch. 2.2), so a key that names no member, or names
  it in another spelling, cannot be written. The clause's text, its eight arms
  and its measurements are in `backlog/done/20260905-wf034-zesde-golf-restvelden.md`
  (wave 6 and its fix round of 2026-09-06); its nine witnesses were removed
  with reason (`corpus/negative/README.md`, "Vervallen bij de v1-migratie").
  (q) A characteristic check's explicit **`host`** MUST name a declared
  object type. It names the type the characteristic is read on when that is
  not the enclosing form's subject, and it IS read — it is the node key of the
  dependency edge the check contributes (ch. 5.1; the graph builder keys its
  characteristic node on `host` and falls back to the subject only when
  the field is absent, and the Drools backend does the identical thing in its
  own stratification and producer/consumer graphs) — but nothing crossed it
  against the object-type table, so an undeclared owner builds an edge on a
  node no form writes and the stratification reads an empty neighbour. The
  third executor degrades further and **silently**: its loader resolves the
  field through a by-name object-type lookup that yields null for an unknown
  name, so an undeclared owner becomes NO owner and the two transpiler
  branches stop emitting the "hosted elsewhere" marker they emit for an owner
  that differs from the subject. *Measured on
  `corpus/golden/jutterswet-wachtploeg`, one field at a time:* `eigenaar:
  null` is played, a declared `Wachtpost` (≠ the subject) makes the backend
  DROP the rule as a transpiler skip, and an undeclared name is played again —
  a rule the engine declines under a real owner is carried and answered under
  a nonsense one. Both OCaml executors are **blind by construction** here, as
  they are at the bean fields of (m): all three spellings replay to the pinned
  expectations in `ref` and in the ASP backend alike. *Measured, one
  characteristic check at a time pointed at an undeclared owner:* 96 of the
  goldens' 96 check positions stayed verifier-clean before this card, and none
  does now. Cost: none — of the 190 characteristic checks the four zones held,
  **2** carry an explicit `eigenaar`, both in the negative zone and both
  naming a declared type, and **no golden carries the form**. *What this arm
  does NOT close:* whether the named type DECLARES the characteristic. That is
  a semantics question — which owner is authoritative — and it was answered on
  2026-09-10 in the field's favour; it is (q1) below. *Identity here is
  and stays case-insensitive*, unlike (p) above while it stood: this is
  ordinary name RESOLUTION — a name pointing at a declaration, not a
  declaration's identity written out — and every reader resolves it that way
  (the verifier's case-insensitive lookup, the ASP translator, and the Drools
  loader's exact-then-case-insensitive object-type lookup). The verbatim rule
  of (p) was scoped to the eight denormalised keys and to nothing else, and it
  left with them at M1.
  (q1) A NON-navigated characteristic check's explicit **`host`** MUST name
  a type that DECLARES the named characteristic (**WF-100**, maintainer
  decision 2026-09-10). This is the second half of (q) and it is a semantic
  decision, not a resolution one: it settles WHICH OWNER IS AUTHORITATIVE, the
  field or the enclosing form's subject. **The field is.** Where it is present
  the declared-characteristic cross of WF-034 is keyed on it and not on the
  subject, and so is the time-dependence cross of WF-043 — one key, computed
  once, because a rule that moved one of them and not the other would be two
  laws about one field. Where the field is absent nothing moves: the subject
  keeps the resolution it always had.

  *What "the declaration key" means to a reader,* stated operationally so no
  executor has to guess. Every declaration consulted for the characteristic —
  does the type declare it (WF-100 here), is it time-dependent (a `timeline`
  facet on its Characteristic node; v0 `kenmerkGranulariteit`), and the node
  key `char:<owner>:<name>` of the dependency edge the check contributes
  (ch. 5.1) — is the one the NAMED type carries. The dependency graph was
  already keyed this way before the rule, in the verifier, the reference
  interpreter and both Drools graphs; what the decision adds is that the two
  crosses now agree with it. The OBJECT the check is evaluated on is a separate
  question, and (q) is where the wire's answer
  lives: the field names the type whose PATTERN hosts the constraint. A
  bean-model executor can honour that, and does — it attaches the constraint to
  the named type's pattern and to no other, which is the whole reason the v0
  producer emits the field at all (it tags the guard of a fact creation with
  the DOEL type, so the constraint lands on the doel pattern "and never the
  anker, even when the anker declares a same-named characteristic"). *Until
  2026-09-19* the reference interpreter and the ASP backend read exactly ONE
  object per condition, the enclosing form's subject, and where the owner was
  not that object's type they REFUSED rather than answer from the subject —
  which is the reading this decision rejected. (q2) replaces that refusal with
  the reading: the guard position binds two objects and every executor reads
  the one the field names. An owner that names the subject's own type, and an
  absent owner, are the same reading and always were.

  *The measured pre-divergence, which is the motivation.* On a verifier-clean
  document whose owner named a declared type OTHER than the subject's, with
  both types declaring a characteristic of the same name (2026-09-10, a
  `Vondst` subject carrying `gekeurd`, owner `Jutter`): the verifier said PASS,
  the reference interpreter and the ASP backend both answered from the
  SUBJECT's value under the other type's name, and the Drools backend carried
  no rule at all (`EXCLUDE transpiler SKIP: regel (voorwaarde buiten Phase-B
  scope)`). One document, three executors, two readings and a silent one.
  Point the same owner at a type that does NOT declare the characteristic and
  the third executor degrades further still: on the fact-creation guard, where
  its bean model really does host the constraint on the named type, the
  DRL-text branch emits a property constraint against a bean that has no such
  field and never compiles it (`Unable to Analyse Expression gehesen == true:
  unable to resolve method using strict-mode`), while the exec-model branch
  builds and then dies at fire with `ConstraintEvaluationException: Error
  evaluating constraint`. Both measured 2026-09-10 on the producer shape below
  with the characteristic taken off the owner. WF-100 keeps that document off
  the wire; ch. 5.8b closes the two-readings half.

  *The rule also REPAIRS a live rejection, which the corpus count cannot show.*
  The producer emits this field on exactly one shape, and on that shape the
  guard's characteristic belongs to the DOEL while the enclosing subject is the
  ANKER — so a subject-keyed cross rejects the frontend's own emission.
  Measured 2026-09-10 on the `jutterswet-postwacht` fact creation (anker
  `Kustvak`, doel `Sein`) with a `gehesen` characteristic on the doel and the
  guard `eigenaar: "Sein"`: **before** this rule the verifier answered `WF-034
  FAIL /feitCreaties/0/voorwaarde: 'gehesen' is not a declared characteristic
  of 'Kustvak'`, **after** it answers PASS. So "no measured damage" is true of
  the corpus zones and false of the producer: outside the corpus the cross was
  rejecting the one shape that exists, and the owner key is what makes it
  conforming.

  *And the same measurement carried a cost, which (q2) has since paid.* On that
  now conforming document the two OCaml executors FAULTED while BOTH Drools
  branches played it and answered — `vakseinaantal` 1, the count that follows
  from which `Sein` instances carry `gehesen`, where before WF-100 the two
  OCaml executors answered 0 by reading the anker, which never carries it. One
  verifier-clean document, one executor answering from the doel and two
  refusing: named here rather than left to be discovered, and carded. Since
  2026-09-19 all four answer 1, and the corpus pins it.

  *The subject-less position needs no separate sentence.* In a day-kind
  condition (ch. 3.7) the walk knows no subject at all, so before this rule the
  declared-characteristic cross simply did not run there and the field was read
  by nobody. Under the owner key it is the only key there is, and the position
  becomes ordinary rather than exceptional. `corpus/negative/`'s day-kind
  witness carries such a check (`Jutter`/`penninggerechtigd`, consistent), and
  WF-031 refuses that document for reading object state at all, so no executor
  reaches it either way.

  *Scope of the CROSS: the NON-navigated check.* On a navigated one the
  characteristic lives on the REACHED type, which the WF-053 arm crosses there;
  an owner is not that type, and crossing it would reject a form nothing has
  decided. No document in any zone carries owner-plus-navigation, and the shared
  dependency graph already ignores the owner on that arm. *The HOSTING cross is
  not so scoped and runs on both forms*: the owner names the pattern the
  constraint hangs on, and that pattern is the subject's whether or not the
  check then hops. Stated because the consequence is sharp — measured
  2026-09-10 on `jutterswet-postwacht` with the reached type in the field
  (`risicopost` via `post`, owner `Post`, subject `Wachter`), the verifier
  answers PASS before AND after WF-100 (the declaration cross is correctly out
  of scope) while `ref` and the ASP backend went from PASS (10) to faulting and
  both Drools branches refused throughout (a transpiler SKIP on one, a `TODO
  kenmerk 'risicopost' hoort bij Post` marker on the other) — a form with no
  answering executor at all, which is what carded the object half. Since
  2026-09-19 that document is not on the wire: WF-109 (q2) refuses a host no
  bound object is, so what was an executor split is a verifier verdict. The
  `gedurende` clip member is refused an owner outright by WF-097 and is
  untouched throughout.

  *Cost: none.* Re-counted on the day the rule landed: **216** characteristic
  checks over the three verifier-gated zones (golden 166, negative 359, red 26
  — 551 documents), **6** with an explicit owner, all six in the negative zone
  and all six consistent — five naming a type that declares the characteristic,
  the sixth naming a type that is not declared at all and which (q) already
  refuses. The parity zone adds **9** more over its 30 documents and **0** of
  them carry an owner, so the toolchain-pinned material does not exercise the
  form either.
  There is no counter-example in the corpus; the rule's first witness had to be
  written for it, which makes this a design decision on an empty CORPUS set. It
  is NOT a decision without measured damage — the producer paragraph above
  shows the pre-rule cross rejecting the frontend's own emission shape — and the
  two statements are recorded together rather than one standing for both. The
  before/after sweep over the verifier-gated zones (golden 167, negative 360,
  red 26 — 553 documents) moves exactly the new witness's verdict and not one
  other report line.

  *The residue this rule left, and where it went.* WF-100 closed the
  declaration half and recorded the OBJECT half as open — that a hosted guard
  is about the named type's instance and not the subject's, which the bean
  model implements on both branches while the two OCaml executors answered with
  a refusal. It was closed on 2026-09-19 in the same direction: (q2) below,
  WF-109. The second residue went with it. Under WF-100 the only owner shape
  that REPLAYED in every executor was the one naming the subject's own type,
  which the v0 producer never writes, so owner-carrying replay and
  owner-carrying production were disjoint; under (q2) the producer's own shape
  replays, and the corpus carries it (`jutterswet-seinhijsing`,
  `jutterswet-mastsein`).
  (q2) A characteristic check's explicit **`host`** is its **GASTHEER**: it
  names the object type the check LIVES ON, and it MUST DESIGNATE exactly one
  of the objects bound at the check's own position — exactly one of them
  SATISFIES it (`T ⊑ O`) and no second one OVERLAPS it (the declared type graph
  holds a type below both) (**WF-109**, maintainer decision 2026-09-19). This
  is the object half (q1) left open, and it is settled in the direction (q1)'s
  wire reading already pointed — the
  field says which pattern hosts the constraint — with the consequence (q1)
  did not draw: every executor honours it, so a verifier-clean owner is
  carried by all four and the differential square closes over the form.

  *The wire field.* It carries one object-type string, spelled `host` since
  L5 (delta B10), the hosting name the migration proposal
  (`proposals/V1-WIREMIGRATIE.md`) gave v0's `eigenaar`. The decision itself
  was a semantic one, not a shape one, and the rename moved no meaning: until
  L5 the field was `eigenaar`, and this clause read it the same way.

  *Which objects a position binds.* ONE, everywhere: the enclosing form's
  subject. TWO at exactly one position — the `condition` of a §13.4.7 fact
  creation (ch. 3.4), when the check IS that condition, does not navigate and
  carries no whole-period qualifier: the ANKER, which the form iterates, and
  the DOEL, which the join chain reaches. NONE in the subject-less day-kind
  condition, where the walk binds no object at all; there this rule has nothing
  to cross and stays silent, and WF-031 refuses the document for reading object
  state in the first place.

  *Two questions of every binding, and the difference between them is the rule.*
  A binding whose declared type is `T` **SATISFIES** the owner `O` iff `T ⊑ O`
  (ch. 2.2): every object it can hold IS an `O`, so the reading is settled for
  all of them. It **OVERLAPS** `O` iff the declared type graph holds a type
  below BOTH — iff some object could be a `T` and an `O` at once. Satisfaction
  is the reading; overlap is the ambiguity. The rule wants **exactly one
  satisfying binding, and no second overlapping one.**

  Both halves are load-bearing, and each was measured wrong on its own.
  Counting satisfaction alone was the rule until 2026-09-19 and a subtype edge
  walked straight past it: declare `Sein ⊑ Kustvak` and a concrete `Sein` fills
  an anker position declared `Kustvak`, so an owner naming `Sein` designates
  the doel AND the anker at run time while a `⊑` count sees one — measured on a
  verifier-clean document that schema, verifier, `ref` and the ASP backend all
  passed. Counting overlap alone fails the other way: it ADMITS an owner naming
  a strict SUBTYPE of the only bound object, where the reading holds for some
  objects and faults for the rest, which is a verdict the wire cannot settle
  and a run-dependent answer is exactly what this rule exists to end.

  *Three flanks, and all three are FAIL.* **Zero** satisfying and zero
  overlapping: the field names a type nothing here can be — the ordinary rule
  with a foreign owner (which until this rule was an executor refusal,
  `RUNTIME-SEMANTICS.md` ch. 5.8b) and the fact-creation guard naming a THIRD
  type, which has two patterns and is handed a name for neither. **Zero**
  satisfying but some overlapping: the partial population above. **Two**
  designated — two satisfying, or one satisfying beside a second overlapping:
  the type string reaches both objects and settles nothing. That is not a
  corner but three shapes — the SELF-TYPED pair (anker type = doel type), every
  common supertype of the two, and every subtype edge between them. No
  precedence is invented for any of them: the producer has no route to the
  shape either (on `doel == anker` the frontend leaves the guard an unresolved
  marker), so a silent anker-wins or doel-wins rule would be a reading nothing
  wrote.

  *Scope of the DOEL, stated with its reason — three fences.* The second bound
  object is admitted only where the check IS the whole `condition`, does not
  navigate, and carries no whole-period qualifier. NESTED inside a composite it
  is not: the bean model's exec-model branch lowers a doel-side guard through a
  fact-only leaf predicate and carries no composite there (measured
  2026-09-19), so admitting it would split the executors instead of closing
  them — and a verifier that admits what one executor cannot carry is the split
  this decision exists to end. NAVIGATING it is not either: there the
  characteristic lives on the type the hop REACHES while the owner names the
  host, and the producer emits only the bare form, so widening would norm a
  shape no witness measures. QUALIFIED by ch. 3.9's `gedurende het gehele
  <periode>` it is not, and that fence is the measured one: in both bean-model
  branches the whole-period fold arm runs BEFORE the host test, so under a
  qualifier the field decides nothing and the fold picks its own subject —
  measured 2026-09-19 on one verifier-clean document, `ref` and the ASP backend
  answered the doel reading, the exec branch reached the same number by another
  route, and the DRL-text branch lowered the fold onto the ANKER and sank the
  whole package at compile (`Unable to Analyse Expression
  rsGeheelPeriode($subject.getGehesen(), "JAAR", …)`). The §8.2 whole-period
  family is another decision's and is out of scope here, so the shape is
  refused until that decision norms it. All three are refused under this same
  number, with the anker as the only bound object, and all three are cheap to
  revisit: a witness in all four executors is what each widening costs — and
  the third falls the day the §8.2 family reaches this position.

  *Vorm B — the doel-hosted COMPARISON — is out of scope by construction.*
  A `Comparison` has no `host` field at all, and the frontend produces no
  doel-hosted comparison; measured 2026-09-19, a fact creation whose guard
  compares a DOEL attribute has no verifier-clean instance when anker and doel
  differ (the derived reading falls to WF-045's cycle or WF-053's ambiguity).
  So this decision does not generalise to the comparison without a new wire
  field, and it does not claim to.

  *An open tension, named rather than resolved.* The same intuition gets two
  opposite answers from this wire. On a characteristic check a foreign type in
  `host` names the HOST — the object the check is about. On an
  `AttributeReference` a foreign type in `subjectType` was read by every
  reader as NAVIGATION over the derived relation, silently (the wire of
  2026-09-19, where the fields were `AttribuutRef` and `objecttype`; since L2
  an own read's `subjectType` MUST equal the subject type, WF-053). One field
  said "read it there", the other "walk to it". Measured 2026-09-19; not
  decided here,
  because deciding it means touching a field this card does not.

  *The other executors read the same two predicates.* The reference interpreter
  and the ASP backend ask satisfaction and overlap from the same shared
  definitions the verifier uses, so a document cannot be legal there and read
  differently here; the bean model asks satisfaction EXTENSIONALLY too, through
  the declared chain — it asked it by NAME IDENTITY until 2026-09-19, which made
  a legal supertype owner (`Sein ⊑ Baken`, owner `Baken`) refuse itself in both
  its branches while the other three carried the document, and the golden
  `jutterswet-bakensein` is that sentence's witness.

  *Cost, counted on the day the rule was written and before its own witnesses
  existed (2026-09-19).* Over the verifier-gated zones — 646 documents (golden
  213, negative 399, red 34), **260** characteristic checks — **10** carry an
  explicit owner. Eight name the subject's own type and are untouched. One
  names an UNDECLARED type and belongs to WF-034 (q), which this rule steps
  aside for: an owner that resolves to no declaration has no satisfaction
  question to ask. Exactly ONE names a declared type the subject is not, and it
  is the negative witness WF-100 already rejects — so the rule adds a second
  citation to one already-rejected document and moves no other verdict
  anywhere. The parity zone adds 9 checks over 30 documents and 0 owners. The
  rule's own witnesses had to be written for it, which makes this, like WF-100,
  a decision on an empty corpus set — and, like WF-100, not one without
  measured damage: before it the producer's own emission shape was
  verifier-clean and unplayable in two of four executors, and the form the
  frontend writes could not become a golden at all.
  (r) A unit's **`system`** (v0 `stelsel`) MUST name a declared `UnitSystem`
  (ch. 2.5).
  It is the unit occurrence's one link back to the conversion table that gives
  it meaning, and nothing crossed it: WF-009 checks a declared table's
  INTERNAL consistency and WF-092 the record's own conversion pair, and
  neither asks whether an occurrence points into a declared table at all. Two
  readers do: the ASP backend's literal-restating pass gates on it (a literal
  is restated into the finest base unit of the target's system only when the
  literal's own `system` matches that system by name AND the system resolves),
  and the identity family reads the declaring system as part of a unit's
  structural identity. *The harm is a measured `ref` / clingo divergence,* on
  `corpus/golden/jutterswet-sleepbaan` with its own case: with the system name
  misspelt at every use outside the declaration the document was
  verifier-clean, `ref` replayed it to its pinned expectations, the Drools
  backend played it, and the ASP backend REFUSED it (`non-identical units
  under the write to "sleeplengte" are not translated`) — the restating pass
  switched off in silence, the write no longer seeing identical units.
  *Measured, every use of one declared system renamed consistently, one
  (golden, system) pair at a time — the instrument has to be consistent, since
  renaming a single occurrence breaks unit identity and WF-091 reports that
  instead:* 21 of 21 pairs verifier-clean before, none after. Cost: none — of
  the **2377** unit records the four zones hold (*recounted 2026-09-06; the
  sixth wave reported 2442 and 2309 from an instrument that counted the 73
  denominator units of composite rates twice, once in their own right and once
  under the enclosing record*), **2244** named a system and every one of them
  resolved, while the remaining 133 named none (the percentage unit and the
  bare calendar units, which belong to no declared system, and since L3 carry
  no `system` at all) and this arm passes over them by construction. Scope:
  the back-reference only. Whether the unit's `abbreviation` is a unit that
  system declares is WF-009's question and stays open. The system's own name
  no longer does: it left the
  open list on 2026-09-06 as one instance of the self-name class, which WF-094
  (ch. 2.1) closed byte-exactly over all five map-shaped declaration tables
  until M1 removed the map keys — and until then the two rules ran in opposite
  directions on the same table for exactly the reason (p) and (q) part
  company. A unit's `system` POINTS at a system and resolves
  case-insensitively; the system's own `name` is that system's identity,
  written once on its node since M1 (beside its map key until then), and is
  carried verbatim.
  *Identity here is and stays case-insensitive*, as in (q) and unlike
  (p) while it stood: this too is name resolution rather than a carried identity, and
  `corpus/negative/wf091-plus-unit-case` pins the boundary deliberately
  (until L2 `wf091-attribuutref-eenheid-kast` did) — that document carries
  `system: "GEWICHT"` against a declared `Gewicht` on a derived position's
  unit: the system resolves case-insensitively, so this arm passes it, and
  WF-091's unit facet refuses the annotation, whose names are compared byte
  for byte. It is the witness that name RESOLUTION is case-insensitive
  while copy FIDELITY is a different question. A byte rule on `system`
  would make it say something else. *(Since L3, 2026-09-24:* the field is
  spelled `system` and the arm reads it; witness
  `wf034-unit-unknown-system`, migrated by the L3 migration.)
  And the two **empty halves** the fifth wave measured and left without a
  witness, each now its own arm because each is its own question — a name
  that resolves against nothing has a declaration table to be missing from,
  an ABSENT name has nothing to look up — and because the two spellings are
  observed differently. An **object creation** (n) MUST carry a `subject` at
  all: pointed at an undeclared name both OCaml executors agree on a wrong
  ANSWER, while emptied both are LOUD (`missing ObjectCreatie.subject`);
  11 of 11 golden creations were verifier-clean when emptied and none is now,
  and of the 30 creations the four zones held every one names a subject.
  *(History until L2 — since L2 `RoleMembership` requires `factType` and
  `role`, so this half is enforced at load and its witness
  `wf034-rolcheck-loze-andertefiller` was removed with reason:)* A
  near-side **role check** (o) had to carry an `anderTeFiller` at all: with the
  field absent there is no type to derive a connecting role TO and the written
  `rol` is presentation, so the node carries no reference whatever; both
  executors are loud (`missing Rollidmaatschap.anderTeFiller`), 4 of 4 golden
  positions were clean when emptied and none is now. That second arm is the
  one place this wave touches a pre-existing document, and it costs nothing:
  of the 10 role checks the four zones held, the one carrying
  `anderTeFiller: null` is a day-kind condition WF-031 already refuses
  outright — a day-kind condition may not read role state at all, which is
  exactly why no subject was there to derive a filler from — so it keeps its
  verdict and gains one truthful incidental line.
  **Seventh wave 2026-09-06** (maintainer decision) — *removed at L2
  (2026-09-24, B13, B16), history*: a fold operand is walked as the ordinary
  read it is since L2, a reference carries no type, and the unknown-type
  node is no datatype, so none of the forms the fence refused can be
  written. Its
  witnesses were removed, or moved to the rule their translation falls on
  (`wf034-sentinel-*` in `corpus/negative/README.md`, L2). As it stood, it
  closed the unknown-type fence rather than the naming class: (r) **the unknown type is
  refused on every fold operand that is not a fold SOURCE — in both its
  spellings, the `Onbekend` sentinel and a `type` absent altogether**. The
  expression walk's type prologue refuses both at any value node, and the
  fold-argument branch REPLACES that walk, so both survived at exactly the
  positions that branch reaches. One of them is legitimate and stays: the
  **single** argument of an AGGREGATION is the fold SOURCE, the ch. 4.3 row-1
  wart, which carries the sentinel by construction and whose kind WF-093
  governs. Everything else the branch reaches is an ordinary read — no
  source, no role name — and the unknown type is as ill-formed there as at
  any other read.
  The scope is the ARITY **and the KIND**, drawn on exactly the pair WF-093
  keys on (arity one AND a folding kind: the seven value folds plus `AANTAL`),
  which is what keeps this arm off WF-093's ground and leaves no gap beside
  it: the two conditions are disjoint and jointly exhaustive over the branch,
  so no position is reported twice and none falls between them. Measured, one
  mutant per half: moving the exemption to "single AND `AANTAL`" adds a
  second citation to exactly the four `wf093-wratvouw-*` negatiefs and to
  nothing else; reverting the KIND half to the arity alone frees exactly
  `wf034-sentinel-builtin-operand` and `wf034-sentinel-builtin-wratoperand`
  and moves no other verdict and no other report line; inverting the arity
  guard newly rejects 12 documents, 10 goldens among them, 5 of those
  anchors; dropping the arity scope rejects the same 12; deleting the
  unspelled-twin arm alone frees exactly `wf034-sentinel-null-vouwoperand`.
  *The kind half is the fix round's, and it was a live gap, not a tidy-up.*
  A single-argument shape at a NON-aggregation kind — a builtin carrying an
  `elementObjecttype`, which has no source either — was left to the rules
  that already fence it. THREE hand-built spellings were indeed fenced, and
  stayed so until L4: WF-048, WF-083 (c) and WF-047 respectively. Three more were NOT:
  the degraded own read, the wart verbatim, and the `set: true` reading, all
  three verifier-clean before this round — and the second CRASHED the third
  executor (`IllegalStateException: geen getter voor …`) while `ref` and the
  ASP backend refused. Closing them moved zero documents: the four zones
  carried **no** non-aggregation `Functie` with an `elementObjecttype` at
  all, before the two witnesses that pin the exit. Since L4 the shape cannot
  be written — a `BuiltinCall` carries no `elementType` and no range (delta
  B21) — and the witness that carried both,
  `wf034-sentinel-builtin-wratoperand`, was removed with reason; so was the
  other, `wf034-sentinel-builtin-operand`, which L2 had rewritten as
  `wf047-bereik-op-builtin` (`corpus/negative/README.md`, L2 and L4).
  What the shape bought itself while it was clean is
  this wave's familiar currency: the identity family defers on the sentinel
  and the MINIMUM/MAXIMUM derived expectation defers when any operand type
  is unknown, so one degraded annotation switched off unit, precision,
  wholeness and domain checking at that operand. And no executor carries
  it — `ref` and the ASP backend refuse the sentinel spellings with one fault
  each, while the third plays some, throws on others and declines the rest
  (the table in ch. 4.3, flank (4), where the decision and its alternatives
  are argued, and where the unspelled twin's row breaks the pattern: `ref`
  ANSWERS it, off the declaration, agreeing with a third executor that
  crashes on the unit-bearing variant). Cost, counted over golden + red +
  negative + parity: of the
  **65** sentinel `type` positions the four zones hold, **60** are the
  single-argument wart (untouched) and exactly **one** is a listed operand —
  in the parity zone, on a document already carrying a co-firing WF-034 at
  the same pointer, so its verdict does not move; of the **three** null
  `type` positions, all three are in negatiefs already rejected for that very
  thing and **none** is a fold argument. *Re-counted on the head this wave
  actually lands on* (546 documents, the datum-tijd card in the base): 71
  sentinel `type` positions, 66 at a fold argument, 62 of them the
  single-argument wart and 4 listed — the three witnesses this wave brings
  plus that same ONE parity operand. The invariant, not the totals, is what
  carries the claim: the corpus holds exactly one listed sentinel operand
  that this rule did not put there, and it is in the ungated zone. Not one golden, red or
  negative report changes a sentence, in the wave or in its fix round.
  *One position the earlier waves left unwitnessed, closed in the same
  round:* the prologue arm itself — the sentinel at a plain read — had no
  negative case anywhere in the zone, so a live arm could have regressed
  unnoticed; `wf034-sentinel-kale-lezing` pins it, and is labelled in its own
  NOTE as the one witness of this round that was NOT rood-eerst.
  *The injection harness, stated so it can be re-run.* For each of the corpus's
  **157** goldens, every JSON object whose `node` is one of the eleven datatype
  kinds (`Numeriek`, `Percentage`, `Tekst`, `Booleaan`, `DatumInDagen`,
  `DatumTijd`, `Enumeratie`, `Onbekend`, `SumType`, `ListType`, `StructType`)
  is replaced, ONE AT A TIME, by `{"node":"Onbekend","reden":"niet opgelost"}`
  and the document run through the verifier; a SURVIVOR is an injection that
  still exits 0, and the **15** positions already holding the sentinel by
  construction are counted separately as no-ops. That is **2813** injections.
  Before (r): **10** survivors, all listed fold operands — eight in
  `jutterswet-peildatum` (four folds, two operands each) and two in
  `jutterswet-sluitsom`. After (r): **0**.
  **2026-09-09** (maintainer decision), the naming class again and ONE arm, on
  the last position of that class the corpus still carried — a name resolving
  against a declaration table the document holds itself: (s) **every VALUE of
  `dagsoortPerEenheid` MUST name a day kind `dayKinds` declares**, under the
  ch. 2.1 name identity — trimmed and case-insensitive, the same `norm` the
  verifier already applies to the map's KEY.
  *(History since L3, 2026-09-24: arm (s) left the wire with the registry.* A
  day kind carries its own spellings since, its `name` and its `plural`
  (ch. 2.9), so a spelling stands on the day kind it belongs to and no value
  is left to resolve. The witness `wf034-dagsoorteenheid-onbekende-dagsoort`
  had no L3 form — its value named a day kind no declaration carries — and was
  removed with reason (`corpus/negative/README.md`, L3); the identity pin of
  `jutterswet-vormenkabinet` below left with the golden's registry entries.
  What follows records the arm as it stood, until L3.*)
  The map couples a written unit SPELLING (the key) to the NAME of a day kind
  (the value). The key side was read and the value side was not, and that
  asymmetry is the defect: the verifier asks whether a spelling occurs as a
  key and, if it does, admits it as a duration unit beside the nine calendar
  units — in date arithmetic (WF-035) and in a tijdsduur (WF-038) — so the
  admission could rest on a name no declaration carries. The spelling is let
  through as a day-kind unit while the day kind it names does not exist.
  *Observability: no consumer notices*, which is what makes this a wire arm and
  not a differential finding. Measured 2026-09-09 on the witness's own twist,
  replayed against the host golden's two cases: `verifier` PASS, `ref` PASS
  (7 and 3 expectations), the ASP backend PASS (7 and 3), the Drools backend
  PLAYED 2 — byte for byte the untouched golden's own outcomes in all four.
  `ref` and the ASP backend read the map at ZERO positions. The Drools backend
  reads the value at exactly two, the §6.10 tijdsduur and the §6.11 date
  arithmetic, and defends itself there ad hoc rather than by a verdict: an
  unresolvable name yields a null day-kind global and the transpiler emits a
  MARKER, so the form is silently NOT CARRIED instead of refused — one
  consumer, on one of four, at two positions out of the four entries. And no
  golden can carry those two forms, because `ref` and the ASP backend both
  refuse day-kind durations outright (a recorded flank, not executed), so the
  arm rests on the wave-6 `kenmerkSoorten` argument: a name that crosses
  nothing in any executor still points at a declaration that has to exist for
  the document to say what it says.
  *This arm is not WF-017's* (ch. 2.9). WF-017 asks whether two spellings
  COLLIDE — a day kind against a built-in calendar unit, or against another
  day kind — and resolves nothing; (s) asks whether one name RESOLVES, and
  compares nothing. Their positions stay disjoint at the entry's own seam:
  since 2026-09-10 WF-017 also reads this map, but only its KEYS (ch. 2.9,
  arms (a) and (b)), where (s) reads only the VALUES. Where the two could meet —
  a declared name as key whose value names no declared day kind — arm (b) steps
  back and leaves the single diagnosis to (s), so no document is reported twice
  for one defect. NO corpus document instantiates that touch point, so it is
  *measured by construction, 2026-09-10:* the three shapes of a declared name as
  key with an unresolvable value, a null and the empty string each report
  exactly one line, and each time it is (s)'s. (The witness
  `corpus/negative/wf034-dagsoorteenheid-onbekende-dagsoort` does not
  instantiate it — its offending key is not a declared name, so arm (b) never
  reaches the step-back there.)
  *The key side carries no RESOLUTION rule, by decision and not by omission*
  (it does carry WF-017's collision arms since 2026-09-10; the two questions are
  independent). The carrying argument is what the WIRE carries, not what the
  source language does. A day kind's plural reaches v0 as a FURTHER KEY of this
  same map and as no field of its own (ch. 2.9's `plural` lives on the v1
  node), so nothing on the wire
  distinguishes a DECLARED plural from a DERIVED one or from an abbreviation —
  all three arrive as an ordinary key. A key rule would therefore have to
  reconstruct the missing distinction by fixing Dutch morphology inside an
  engine-neutral IR, and it would bind every producer to one language's plural
  formation. A producer must instead stay free to register a further spelling,
  an abbreviation among them, for the same day kind. *(The source language does
  make the plural an optional author field, §3.12 — but that is not what
  carries this decision, and it must not be read as saying that a plural is
  absent when the author omits one: the reference frontend derives a `-en`
  plural in exactly that case and cites §3.12 for doing so. The wire cannot see
  which of the two it is holding, and that is the point.)*
  *Measured 2026-09-09, on the state THIS ARM's own card landed on — not on any
  later one; the corpus has grown since:* **35 of the 70**
  entries the four zones hold are keyed on something other than the declared
  name. That figure is a count of ENTRIES and not of independent cases, and the
  difference matters here: the 70 entries carry only **four** distinct
  (registry, declarations) shapes, one of which is repeated byte-identically
  across **30** parity fragments, and the 35 non-declaring keys spell only
  **three** distinct words. **34 of those 35 are the plain `<declared name>` +
  `en` form** — the very shape the frontend derives — and the one exception is
  this card's own witness, twisted on purpose. So the corpus has **zero**
  witnesses for an abbreviation and **zero** for an irregular plural: the
  freedom this decision preserves is argued from the wire's shape, not
  demonstrated by a document.
  What a key must not do is COLLIDE, and that is WF-017's clause; since
  2026-09-10 that check reaches the keys of this map as well — a key that is a
  built-in calendar unit, and a key that is a declared day-kind name bound to a
  different day kind (ch. 2.9, arms (a) and (b), each with its witness).
  *Cost: none, measured directly, with both states named.* **On the BASELINE
  that predates this card** — 554 documents of golden + red + negative +
  parity (164 + 23 + 337 + 30) — the map held **64** entries over **31**
  documents (4 in the one golden `jutterswet-schouwrooster`, 60 in the 30
  parity fragments, none in red and none in negative), **every one of them
  resolving**, none null and none empty. The before/after sweep over those 554
  plus the card's own witness — **555** documents — moves the witness's verdict
  and NOTHING else: not one other verdict and not one report line. Arm-exact:
  removing the arm's one call from the verifier's check list turns exactly
  `corpus/negative/wf034-dagsoorteenheid-onbekende-dagsoort` clean over the
  same 555 and moves nothing else.
  **On the state this card LANDS on** (2026-09-09, 555 documents: 164 + 23 +
  338 + 30) the same map holds **70** entries over **33** documents — 6 golden
  in 2 documents, 4 in the one negative, 60 parity — against **45** declared
  day kinds; **69 of the 70** values resolve and the one that does not is this
  card's own witness, put there to be refused. Zero null, zero empty. The
  figures in the paragraphs above are the baseline ones and are labelled as
  such wherever they appear, because a count that silently mixes the two states
  is exactly the error the fix rounds of waves 4 and 5 had to repair.
  *The IDENTITY is pinned, not just stated.* The rule compares under `norm` —
  collapse whitespace, lower case. Before this card **no document exercised
  that**: every corpus value was byte-identical to its declaration, so
  replacing the comparison with a byte-exact one moved 0 of 555 documents and
  gate 3 stayed green — "trimmed and case-insensitive" was unfalsifiable.
  `corpus/golden/jutterswet-vormenkabinet` now carries **two** entries that
  resolve only through normalisation, one per half: a value `SchouwDag` against
  a declared `schouwdag` (case) and a padded `  schouwdag  ` (whitespace).
  Under the byte-exact mutant that golden reports both entries and gate 3
  FAILS on it. The witness is verifier-only — that golden has no case file and
  is already pinned NOCASE in the differential gate — so it pins the identity
  without touching any executor channel.
  *Three spellings, one arm, stated so the fold is not mistaken for coverage of
  three questions.* A value naming an undeclared day kind, a `null` value and
  the EMPTY STRING all fail to name a declared day kind; the schema types the
  value `["string", "null"]`, so the null is schema-valid, and the empty string
  is an ordinary string the guard rejects by resolving it against nothing
  (`names day kind ''`). The ONE reader collapses null onto the naming case:
  its loader reads the value with `asText()`, which turns a JSON null into the
  string `"null"` and then fails the same lookup. Only the naming spelling has
  a corpus witness — of the 70 entries **zero** are null and **zero** are
  empty, and making the null branch alone inert moves 0 of the 555 documents —
  so both silent spellings are folded into this arm on the wave-5-fix-round
  precedent (one list-driven arm rather than one clause per sub-arm, where a
  sub-arm with no counterexample is not its own clause) rather than given
  clauses and negatiefs of their own. Off corpus each is one edit from the host
  golden and is refused by its own message.
  **2026-09-20** (maintainer decision) — *(t) merged into (o) at L2
  (2026-09-24, B9), history.* The far-side node left the wire: the
  migration derived its `factType` and its `role` (the subject's side) with
  exactly this crossing, and refused the documents where it found no single
  candidate. **The narrowing, carried by `counterpartType` since L2
  (2026-09-24):** the far-side node's `filler` also NARROWED the population
  (a strict subtype of the far role's type counts only the objects of that
  subtype, `RUNTIME-SEMANTICS.md` ch. 5.8a). `RoleMembership` as B9 states
  it has no field for that, and on `corpus/golden/jutterswet-postbezetting`
  the two Strandjutter checks would have migrated to the same node as its
  two Jutter checks, losing the narrowing `case3-strandwacht` pins;
  `counterpartType` carries it (clause (o) above). The clause as it stood: the naming class once more and the one
  entry of the open list above that stood there for want of a WITNESS rather
  than for want of a rule: (t) **a far-side role check MUST carry both a
  `filler` and a `backref`, and the two MUST MEET ON ONE DECLARED FACT
  TYPE** — `backref` naming the role the **subject** fills, `filler` the
  object type that fills the role at the OTHER end. Role-name identity is
  ch. 2.1's, trimmed and case-insensitive, over the declared name or the
  declared plural, as everywhere else this chapter looks a role up.
  The far-side check ("de Wachtpost heeft een wachtjutter", the §8.1.7 form
  read from the other end; `RUNTIME-SEMANTICS.md` ch. 5.8a) carries **no
  written role name at all**. That is what separates it from its near-side
  twin (o), whose `rol` field exists and is presentation: here there is no
  third field to be presentation, so the pair IS the reference, and the rule
  has to be a CROSSING rather than two lookups. Each field alone is satisfied
  by a document that means nothing — a `filler` naming a declared type the
  subject is unrelated to, or a `backref` naming a real role on the wrong
  SIDE, the filler's own role instead of the subject's, which is precisely
  the mistake a producer makes — and in both the exists-join ranges over an
  empty relation while every individual name resolves.
  One cross, against the role fillers, mirroring (o): the declared fact types
  whose two roles are filled by the subject and by `filler`, with the
  subject's own role named by `backref`. An EMPTY candidate list is this
  arm's report. A list with SEVERAL entries stays WF-048's ambiguity
  question, and a subject the expression walk does not know keeps the arm
  silent — both exactly as in (o). The two EMPTY spellings are folded into
  the same arm on the (s) precedent: an absent `filler` leaves no type to
  range over and an absent `backref` no role to join on, and with either one
  gone the node carries no reference whatever.
  *What the crossing settles about `filler`, and what the EXECUTORS then
  do with it.* The cross reads `filler` one-sidedly — `filler ⊑` the far
  role's declared type, the same `fills` test every other arm here uses —
  so a **strict subtype** of the role's type is well-formed and a type
  ABOVE it is refused. That refusal is arm (o)'s own strictness carried
  over, recorded as a deliberate choice rather than discovered: a filler
  the role cannot hold names a population the relation cannot reach.
  What the subtype admission costs is that the field then carries MEANING
  beyond resolution, and every reader must spend it — the objects the
  relation reaches count only where their own type is ⊑ `filler`
  (ch. 2.2, extensionally; `RUNTIME-SEMANTICS.md` ch. 5.8a). A
  reader that resolves the pair and stops answers the ROLE's whole
  population instead of the named one. Not hypothetical: it is what `ref`
  and the ASP backend did when this clause first landed, measured
  2026-09-20 on a verifier-clean document with a `Strandjutter` under the
  `Jutter` — both reported a post manned by a beachcomber it did not
  have, while both Drools branches, which filter on the filler class,
  answered correctly. The corpus now carries the discriminator
  (`jutterswet-postbezetting` cases 1 and 3); a witness whose filler
  equals the role's declared type cannot tell the two readings apart at
  all, which is why the card's first witness could not catch this and the
  second was built to.
  *What this arm deliberately does NOT cross, and why.* Not the ARITY of
  either role. A producer writes the far form where the named role is to-many
  from the subject, and a bean-model executor needs the `backref` role to be
  to-one because it stores one reference field per to-one role — but neither
  `ref` nor the ASP backend needs either property: ch. 5.7a makes the relation
  symmetric, so the join reads the relation and not a direction. A MUST there
  would refuse documents two of the four executors execute correctly. An
  engine limit belongs where the bean-model executor already puts it, in a
  per-form refusal carrying its own reason (§13.3.9), and that refusal is
  measured: with the roles arranged so that no field is derived, both Drools
  branches name the missing reference field and neither falls over.
  *Measured 2026-09-20, and the order of the card is part of the measurement.*
  The WITNESS came first: `corpus/golden/jutterswet-postbezetting` is the
  first document in any zone to carry a `VerreRollidmaatschap`, and it was
  hand-written with its expectations computed before an executor saw it. On
  that document, before anything else moved: verifier PASS, `ref`
  `EVALUATION FAULT: condition kind not implemented in this card`, the ASP
  backend `TRANSLATION FAULT: condition kind not translated (no corpus case
  exercises it)`, and BOTH Drools branches `PLAYED` with exactly the
  hand-computed values. So the form was never *refused* by the two OCaml
  executors — both sentences were BOTTOM SENTENCES of
  `scripts/check-red.sh`'s list, residual arms reporting their own position
  (`ref`'s left that list on 2026-09-20, when its last catch, the period
  condition, became the ch. 3.10 gate; the ASP backend's is still a genuine
  wildcard and still listed) — and the same card gave each a named arm, which is why the golden now
  closes the differential square (ref ≡ clingo ≡ Drools) on the far form.
  *Cost: none.* The sweep runs over golden + red + negative + parity —
  **690** documents on 2026-09-20 (217 + 34 + 409 + 30) — and the whole
  corpus carries **6** far-side checks over **2** documents, both this
  card's: the golden's four and the negative's two. Arm-exact: neutering
  this arm in `walk_cond` turns
  `corpus/negative/wf034-far-role-check-wrong-side` clean and moves no
  other verdict and no other report line over all 690.
  *The two empty spellings are measured off corpus*, each one edit from the
  golden, and they are not the same defect. An absent `backref` is loud
  everywhere: both OCaml executors fault by the field's own name and both
  Drools branches refuse the form, naming the role `null`. An absent
  `filler` is **not**: the OCaml executors fault, but both Drools branches
  throw an unclassified `NullPointerException` — verifier-clean input
  crashing an executor, the wave-2 shape — which is the sharper half of the
  reason the empty spellings are crossed at all.
- **WF-080** A value-position `AttributeReference` MUST resolve to an
  **attribute** of its endpoint type, never a characteristic. A value
  position is where an expression yields a value: a rule/member value, a
  default consequence, a condition operand, and the source of a value-yielding
  aggregation (a `sum`/`total`/`minimum`/… fold whose element values are
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
  kind, with the date on the left of both `plus` and `minus` (a
  duration-left operand is ill-formed — v0's typing opens its date branch
  only for a date-left operand, so `plus` does not commute), date ± date
  ill-formed,
  and a sub-day unit demanding a
  millisecond-precision date operand; additive operands MUST have
  compatible units (both
  unitless or both present and convertible — WF-010); a clamp's base and
  bounds MUST be numeric, and at least one bound MUST be present (that
  last clause is structural in v0's grammar, not a typing error —
  promoted into the rule).
- **WF-036** Comparison and predicate typing (mirror, with one ratified
  v1 tightening): ordering operands
  MUST be number-like with compatible units; **date-ordering operands
  MUST be dates of one representation** — both day-precision or both
  millisecond-precision; equality
  operands MUST share the family — same enumeration domain, same date
  precision, compatible units; an `elevenTest` operand MUST be text or a
  whole number without unit; `exactDigits` MUST have a text operand and a
  whole digit count (v0 does not check positivity — mirrored; a ≥ 1
  tightening is a candidate); a `dayKind` predicate operand MUST be a
  date. A unitless literal adopts its sibling's unit before the check
  (mirrored convenience).
  **The date-ordering precision clause** (maintainer decision 2026-09-05)
  replaces the v0 letter this rule mirrored — date-ordering operands need
  only *be* dates, either precision, an asymmetry v0 left standing — with
  the per-representation family reading WF-038's ordering fold has
  carried since 2026-09-05, and ratifies it. `earlier`, `earlierOrEqual`,
  `later` and `laterOrEqual` (the dialect's `eerder`, `eerder of gelijk`,
  `later` and `later of gelijk`) ask which of two dates comes first, so they
  need an order over both. `Date` has one **per
  precision**, and its two precisions are two of them: a day date names a
  whole day, a millisecond date a point inside one. Since L3 (2026-09-24,
  B15) the precision is the `precision` field of the one `Date` node, `day`
  or `millisecond`, and the clause reads it. The sibling EQUALITY
  arm of this same rule already refuses that seam ("equality across date
  precisions"); this clause is its ordering half. Neither operand is
  ill-formed alone — their combination is, and it is reported at the
  comparison node for that reason. An operand carrying **no type at all**
  defers: the whole arm is skipped and the operand's own rule reports it
  (WF-034 for a value node without a static type; since L2 a reference is
  typed by its declaration, and an unresolvable one is WF-034's too).
  *(History until L2:)* the unknown SENTINEL did not defer here — this arm
  refused it as a non-date operand and WF-034 refused it beside — while
  WF-038's ordering fold deferred on it, because only the ch. 4.3 row-1
  wart reached that arm. **Delta (v1 tightening, 2026-09-05).** *Why a rule and not
  a note:* measured on a verifier-clean document before the clause
  (`corpus/negative/wf036-date-ordering-precision`, 2026-09-05), no
  executor answers it — and the three refuse at different depths. `ref`
  is the only leg that meets the defect itself: it plays a same-precision
  millisecond ordering and faults at evaluation on the mixed pair alone
  (`unordered comparison: 2025-03-05 vs @…ms`). The Drools backend draws
  exactly this line independently, which is the strongest evidence for
  the clause: it PLAYS a same-precision millisecond ordering — in a
  consistency and in a rule condition alike, discriminating a
  one-millisecond difference a day reading would call equal — and skips
  the mixed pair on an explicit guard whose own comment names it a §5.3
  type mismatch never to be coerced away. A third executor, built for
  another engine, fences where this clause fences. The ASP backend
  refuses more coarsely: `unscalable datatype in the ASP translation` is
  a FORM refusal and not a verdict on this defect — the same message
  comes on the CLEAN same-precision ordering, because the ASP scale table
  hands a millisecond-precision date no scale, while a millisecond-precision
  ordering FOLD
  (`jutterswet-hoogwaterpaar`) translates and plays. *(That last sentence
  described the ASP backend on 2026-09-05 and is now history: since
  2026-09-06 the clean same-precision ordering translates through the pair
  term's `dtcmpN` helper — the scale table still hands a
  millisecond-precision date no scale, because it has a pair and not a scaled integer. What is refused
  at that arm now is the MIXED pair, by its own reason.)* *Honesty note:* this
  is a place where the wire fence runs AHEAD of the RegelSpraak frontend,
  which lets the four ordering operators cross the precision seam on
  purpose while demanding one precision for equality, assignment,
  tijdsduur and eerste/laatste. The corpus pays nothing for the
  tightening — over the four verified zones (golden, red, negative and
  parity, 504 documents on 2026-09-05) the only report that moved was the
  new negative's — but a frontend that emits the shape is now refused at
  the wire, and closing that gap is frontend work this clause does not
  do. *A second honesty note, on what the clause is TESTED on:* all seven
  date orderings in golden and red ordered two DAY-precision operands, so
  the admitted millisecond half rested on the executors and not on the
  corpus — refusing the same-precision millisecond pair too moved zero of
  those 504 reports. **That gap closed on 2026-09-06**: golden
  `jutterswet-meldstip` orders a millisecond-precision attribute against a
  millisecond-precision literal in both directions at a sub-day threshold, and all
  three executors replay it (gates 5, 6 and 12) — the admitted half now
  has its regression witness, and the clause is two-sided in the corpus.
- **WF-099** A **clamp bound** — the `minimum` or the `maximum` of a
  `Clamp` — MUST carry a unit compatible with its BASE's: both unitless,
  or both present and convertible, a unitless numeric literal adopting the
  base's unit first. That is the same demand WF-035 makes of an additive
  pair and WF-036 of a comparison, read through the same predicate: the
  rule adds a POSITION, not a fourth notion of compatibility. Each bound is
  crossed against the BASE and not against its sibling, because the base is
  what a bound is restated INTO, and three clauses of
  `RUNTIME-SEMANTICS.md` carry that between them: ch. 2.7 is the RETURN law
  (a clamp yields the base, the minimum or the maximum, minimum applied
  first, comparisons exact, an absent bound no bound) and carries no unit
  sentence at all; ch. 2.10 gives a clamp, as a composite position, the
  **effective type computed from its basis**; and ch. 2.6 is where the
  restating lives — an operand is restated into the other's unit with exact
  factors, and a comparison across units restates *both* sides by their
  exact canonical factors. Every comparison a clamp makes is
  base-against-bound, so min-against-max needs no clause of its own — a
  clamp never compares them.
  An operand carrying no type defers, and a non-numeric
  operand is WF-035's clamp arm and is not reported twice.
  **Delta (promotion, 2026-09-10).**
  *Why a rule and not a note:* this was the one measured unit position with
  no rule on it while its three siblings each had one, and the gap ran in
  the worst direction — the verifier admitted a document every executor
  refuses. Measured 2026-09-10 on a clamp over a `EUR` base with both
  bounds annotated `kg` (`corpus/negative/wf099-clamp-bound-unit`,
  the witness): verifier **PASS** before the rule, `ref` `EVALUATION FAULT:
  incompatible units: EUR vs kg`, the ASP backend `TRANSLATION FAULT:
  non-identical units under a Begrensd bound are not translated`; and the
  Drools backend `ERROR Eenheidsconflict: onverenigbare eenheden bij een
  §6.1.4 begrenzing-ondergrens: 'kg' (canoniek kg) herschaalt niet naar
  'EUR' (canoniek EUR) — er bestaat geen omrekenfactor`. That backend's two
  branches reach the refusal by different routes, which is worth not
  eliding: the exec-model branch carries a clamp-SPECIFIC gate per bound
  (the `§6.1.4 begrenzing-ondergrens` and `-bovengrens` restatings the
  message names), while the DRL-text branch catches the same defect through
  its generic §4 value-restating — both arriving at the one shared
  rescalability demand.
  **Convertible, not identical — measured, because the three executors do
  not spell the fence alike.** `ref` restates through convertibility and
  faults `incompatible units`; the Drools backend restates through a
  rescalability demand whose own witness test pins `kg`/`t` as rescalable
  and `jr`/`kg` as a fault; the ASP backend demands IDENTITY, but of units
  already normalised per declared unit system, so WITHIN one system its
  identity is convertibility. Measured 2026-09-10 on the same document with
  both bounds moved to `t` (system `Gewicht`, factor 1000 to canonical
  `kg`) over a `kg` base: verifier PASS, and `ref` and the ASP backend both
  restate and both answer 10 t = 10000 kg — the same value, a fault on
  neither leg. Convertibility is therefore the line all three executors
  agree on, and identity would refuse a form every one of them plays.
  *Two ref/ASP divergences the choice CLOSES,* both measured the same day
  and both refused by the rule because canonical names differ: a bound
  whose `canonical` (v0 `canoniek`) contradicts the system it names (`t` declared canonical
  `ton` over a `kg` base) and a bound relabelled into the base's system
  (`kg` relabelled into `Munt` over a `EUR` base) each fault in `ref` while
  the ASP backend answers — in the second case answering the untwisted
  values, silently treating kilograms as euros. *One it does NOT close, and
  it is not the one first suspected:* a bound whose unit names an
  UNDECLARED system is already refused, by WF-034 (measured 2026-09-10,
  once per bound: `unit 't' declares unit system 'Massa', which no
  declaration carries: the unit resolves against no conversion table`), so
  no conforming document carries that shape and nothing is left open there.
  *Closed (2026-09-19), and it was the ASP BACKEND that moved, not the
  rule:* the CROSS-SYSTEM bound whose system is fully declared — with
  `Massa` declared beside `Gewicht` (`kg` factor 1, `t` factor 1000, both
  canonical `kg`), a `t` bound over a `kg` base in `Gewicht` — is
  convertible by canonical name, so the verifier and this rule admit it,
  and on 2026-09-10 `ref` restated to 10000 and the Drools backend answered
  10000 on both branches while the ASP backend refused to translate, its
  per-system normalisation finding no identity ACROSS two systems. That was
  executor divergence and not an open normative question: this rule had
  already chosen convertibility-on-canonical-name, and the backend was
  enforcing a fence the norm does not carry. It now gathers its conversion
  table by canonical name across the declared systems and lets a pair whose
  exact factor is 1 through its identity fence. The integer encoding is
  untouched: a cross-system factor that is not a positive integer into the
  finest unit stays the same loud refusal a same-system one gets.
  Re-measured 2026-09-19, one probe per position, `ref` and both Drools
  branches unmoved throughout: the WF-099 bound, the WF-035 additive join
  and the WF-023 write all go from `TRANSLATION FAULT: non-identical units
  under …` to the pinned values, and so does the factorless pair (`kg` in
  `Massa` against `kg` in `Gewicht`). The WF-036 comparison moves for the
  factorless pair and NOT for the factor-1000 one, and that remainder is
  not a system fence: the normalisation pass rewrites rule VALUES and not
  conditions, so the IDENTICAL same-system document (a `kg` attribute
  against a `10 t` literal in a condition, one declared system) is refused
  by the same message before and after. Golden `jutterswet-stelselweging`
  carries the four crossed positions in the full square, with and without a
  factor; `backlog/done/20260910-kruis-stelsel-eenheid-divergentie.md`.
  *Inherited, and stated:* calling the sibling predicate inherits its
  treatment of the **one-sided unit pair** (ch. 4.1) whole, open class
  included — one side unitless and the other unit-bearing is admitted when
  the unitless side is an adopting literal and refused otherwise, though
  all three executors let any one-sided pair through. Measured 2026-09-10,
  the verifier-gated zones carry no such clamp: the before/after sweep over
  golden (166), negative (359) and red (26) — 551 documents — moves
  exactly the witness's verdict and not one other report line. The parity
  zone's 30 documents carry two: the casus dump, which gate 11 keeps out of
  its well-formedness leg, and parity slice 24, which that leg holds out on
  this one report (since 2026-09-25, the parity well-formedness card; until
  then no gate put the zone through the verifier). Admitting that class is
  one decision for WF-035, WF-036, WF-023 and this rule together, not a
  divergence to introduce at one of the four.
- **WF-037** Aggregation shape: facet validity per kind MUST hold —
  `emptyFallback` only on `sum`; `dimensionSelection` only on `sum`,
  `minimum` and `maximum`; `granularity` only on `timeProportionalPart` and
  `dayCount` (builtin side); an instance fold MUST carry `elementType`, a
  value fold MUST carry ≥ 2 arguments, and an aggregation that carries
  `elementType` MUST carry an argument, and where it carries one, an
  `AttributeReference`, its fold source; builtin arities MUST follow the
  ch. 4.3 catalogue (one operand for the scalar and extractor kinds, two
  dates for `duration` and `absoluteDuration`, the component list for date
  construction). **The filled count** (`filledCount`, ch. 4.3; landed
  2026-09-20) takes exactly ONE
  argument, MUST carry an `elementType`, and that one argument MUST be the
  SET source — three arms, of which the last two are clauses of
  this rule that demand a form instead of forbidding one, and they are
  written that way because the alternatives are occupied rather than
  absent: the subject's own read has one value, and a count source hands
  the fold instances, and neither has a filled count. **Since L2
  (2026-09-24, B11, B12) the set-source arm reads "carries a range":** the
  range makes the single argument the set source, and the schema and every
  loader demand it of a single-argument fold with an `elementType` under
  every kind but `total`, so a filled count without one is refused at
  load. What the arm still decided was a range beside a source that is no
  attribute reference (`wf037-filled-count-not-set-source`), and since
  2026-09-25 the fold-source arm below decides it, naming the node, so the
  filled count keeps two arms of its own. A count source
  under the kind is WF-093 (a)'s, and this rule steps aside on it, so one
  malformation carries one rule. The kind is
  outside the "≥ 2 arguments" clause above, whose sentence reads its arity
  law backwards. **The count** (`count`) stands outside that clause too, and
  since 2026-09-20 carries its mirror image: it MUST NOT carry two or more
  arguments. A count folds INSTANCES (`RUNTIME-SEMANTICS.md` ch. 3.4a) and
  the source language spells no operand list for one — §5.8.1 defines the
  count over instances and its typing document excludes the value reading —
  so there is no listed form to admit. One PRODUCER spelled it anyway: the
  RegelSpraak frontend's grammar carries an operand list at this rule, a
  widening of the one-operand EBNF that went unregistered until this
  decision, and three sentences emitted through it unblocked (ch. 4.3). The
  rule cuts that route, and the frontend now refuses it at the node with a
  named ERROR. The arm does NOT read `elementType`: a
  count carrying an element type BESIDE an operand list is the same listed
  form with a second claim on one node, and the wire may not say two things
  at once. What left the wire was a verifier-clean document exactly ONE
  executor answered — `ref` counted the FILLED operands, the meaning the
  2026-09-19 narrowing took off this kind in its set spelling (WF-096, until L2), while the ASP
  backend refused the expression kind and both Drools branches dropped the
  rule; in the element-type spelling all three refused, which is the same
  class of defect seen from the other side (ch. 4.3). BELOW two operands
  that arm is silent — a count of one operand or none is no LISTED form —
  and **since 2026-09-23 a second count arm carries it: a count MUST carry
  an `elementType`**, the sentence the filled count's element-type arm
  already states, so both counts carry one law at one position (maintainer
  decision of that date, option (a) of
  `backlog/done/20260920-aantal-zonder-bron.md`). A count without one has no
  source: nothing names the instances it counts. Measured before the arm,
  on well-formed documents: `ref` answered 0 or 1 on one own read (the
  FILLED count again), 0 on no operand, and **1** on a single `ListLiteral`
  (ch. 4.8) whether that list carried two elements or none — it counted the
  OPERAND and not its contents — while the ASP backend refused the
  expression kind and both Drools branches refused the rule or the node.
  One sentence covers arity 0 and arity 1, because both are the same defect:
  no source. The one exit is a single `InstanceSetUnion` argument, whose
  selections each carry their own element type and beside which WF-110 (c)
  FORBIDS one on the count, so there the source is present one node down;
  without that exit five goldens turn red. Arity ≥ 2 stays the listed-form
  arm's and is not reported twice. Cost: no existing document — every one
  of the 112 one-argument counts over an `AttributeReference` in the four
  zones carries an element type, and the 12 over a union take the exit (the one union count that carries an
  element type is WF-110 (c)'s refusal, which this arm leaves alone). The
  reference interpreter's value arm under the count, now out of reach
  through any well-formed document, refuses by name as the second line of
  defence instead of answering. **The converse arm, since 2026-09-25 (the
  card aantal-bronargument-welgevormd): an aggregation that carries
  `elementType` MUST carry an argument, and under `count` and `filledCount`
  that argument is no `ListLiteral`.** A fold that names its population and
  hands no source is ill-formed: an aggregation that carries `elementType`
  and NO argument, under every kind, and a `count` or `filledCount` whose
  one argument is a `ListLiteral` (ch. 4.8), a list of values where the fold
  reads instances.
  Measured before the arm, on the L5 wire: with no argument the fold was
  verifier-clean under every kind but `filledCount`, whose arity arm refused
  it on the listed form's ground, and every executor refused it — `ref` and
  the ASP backend in their residual sentences, both Drools branches by
  excluding the rule; the `ListLiteral` under a count, which since L2 loads
  only beside a range, was refused on WF-047 alone, a sentence about the
  binding and not the argument. The arm speaks one sentence per shape and
  names the kind in its wire spelling; the filled count's arity and
  set-source arms and WF-047's range arm step aside on the shape, so one
  malformation keeps one rule, and `ref` and the ASP backend refuse it in
  the same two sentences from named arms, their residual sentences staying
  for the other forms. A `ListLiteral` under a value fold is left to the
  rules that already refused it (WF-091 on its type, beside WF-038 or
  WF-047). Cost: no existing document — none of the 737 in the four zones
  before the arm carries an `elementType` beside no argument or beside a
  single `ListLiteral`. **The fold-source arm, since 2026-09-25
  (the card aantal-bronarm-en-drools-lader): the one argument of an
  aggregation that carries `elementType` is an `AttributeReference`.** That
  argument is the fold source (ch. 4.3, the reading of the source): the
  count source that names the counted role under `count`, the set source
  read on each contributor under `filledCount` and the value folds, the
  subject's own read under a `total` without a range. Every other node there
  is ill-formed and named: a `Literal`, a `Binary`, a `ParameterReference`, a
  `VariableReference`, a `BuiltinCall`, an `Unresolved` marker, a nested
  `Aggregation`, and any other. A `ListLiteral` keeps the converse arm's
  sentence under the counts and its WF-091 refusal under the value folds,
  and a union under a count is WF-110 (c)'s; under every other kind the
  union is this arm's and WF-110 (a) steps aside on it, so the node is
  reported once, where until the fix round of the card both rules reported
  it. Measured before the arm, on the
  L5 wire: with a range written the form was refused on WF-047 alone, a
  sentence about the binding and not the argument — under `filledCount` on
  its set-source arm, which did not name the node — while `ref` and the ASP
  backend refused it in their residual sentences; both Drools branches
  PLAYED the counts, counting the instances the range reaches and never
  reading the argument, the DRL-text branch played the filled count the same
  way and crashed on a `sum`, `total`, `minimum` or `maximum` over a literal;
  and a `total` without a range over a `Literal` was verifier-clean while no
  executor ran it. The arm speaks one sentence for every kind and names the
  kind and the node in their wire spelling; it takes over the filled count's
  set-source arm, and WF-047's range arm steps aside on the shape, so one
  malformation keeps one rule. `ref` and the ASP backend refuse it in the
  same sentence from named arms (the ASP backend in each term function that
  meets the fold), and the Drools loader refuses at load, in the verifier's
  sentences, the three shapes of this rule that name a population and hand
  no source: no argument, a `ListLiteral` under a count, and a node that is
  no `AttributeReference`. With a `sourcePath` beside the node the fold is
  reported under WF-102 (c) as well, and the executors report that first.
  Cost: no verdict of an existing document changes — of the 756 documents
  in the four zones before the arm only
  `wf037-filled-count-not-set-source` moves, from the set-source sentence to
  this arm's, and every one of the 30 totals without a range carries an
  `AttributeReference`. *v0 mirror note:*
  the single node makes every combination representable, and v0 even
  populates the element type on multi-operand min/max value folds whose
  first operand is an attribute reference — an artifact the v1 split
  removes; v1 makes the invalid combinations ill-formed. **Delta
  (promotion).** The filled count's three arms are a **delta (new kind)**
  instead: the source language has no such operation, so they mirror
  nothing.
- **WF-038** Aggregation operand typing (mirror, with one ratified v1
  widening): numeric folds' operands MUST be numeric with compatible
  units (the numeric half is checked as WF-091's numeric-operand arm,
  ch. 4.1); **every operand of an ORDERING fold — `first`, `last`,
  `minimum`, `maximum` — MUST come from one comparable family: numbers,
  or dates of one precision**; a `total` operand that is
  non-temporal MUST carry a rate unit per time unit; `easterSunday` MUST
  take a whole, unitless year; the day-precision part extractors
  (`yearOf`, `monthOf`, `dayOf`) MUST take a date operand — either
  precision; the sub-day part extractors MUST take a
  millisecond-precision date; a sum's `emptyFallback` MUST be numeric;
  the two operands of a `duration` or an `absoluteDuration` MUST be dates of one
  precision and its node type MUST carry a recognised time-unit or
  declared day-kind spelling (the ch. 3.8 table of
  `RUNTIME-SEMANTICS.md`), a sub-day unit demanding
  millisecond-precision operands.
  **Scope, stated because the silence would be read the other way
  (2026-09-20):** the filled count (`filledCount`, ch. 4.3) is NOT a
  numeric fold in the sense of this rule and has no operand-typing clause
  here. Its source may carry any VALUE type — the test is presence, nothing
  is added and nothing is compared — so neither the numeric clause above nor
  the ordering-fold clause below reaches it, and the ch. 2.4 orderability
  paragraph has no grip on a fold that ranks nothing. The one exclusion is
  the TEMPORAL axis rather than a value type: a set read of a time-dependent
  attribute folds twice, and only the som and the totaal define the first
  collapse (`RUNTIME-SEMANTICS.md` ch. 3.4c and ch. 3.6), so a filled count
  over one fails observably at evaluation. Its result type is
  fixed by the operation (a whole, unitless cardinality) rather than
  derived from its operand — and being derived it is crossed under WF-091,
  not believed (negative `wf091-filled-count-unit`).
  **The ordering-fold clause** (maintainer decision 2026-09-04, landed
  2026-09-05) replaces the v0 letter this rule mirrored — "first/last's
  operands are dates of equal precision" — with the family reading the
  runtime semantics already carried as a tracked deviation
  (`RUNTIME-SEMANTICS.md` ch. 1.2), and ratifies it. What the widening
  widens is the TYPE FAMILY, not the precision discipline: that half of
  the old letter is back inside the checked MUST since 2026-09-05. The four ordering kinds keep the least or
  greatest CONTRIBUTOR, so they need an order over the contributor's
  type. `Number` has one. `Date` has one **per precision**, and its two
  precisions are two of them: a day date names a whole day, a
  millisecond date a point inside one, so ordering the two against each
  other is as unanswerable as ordering a number against a date (measured
  in `ref`: `unordered comparison: 2026-06-21 vs @…ms`). The
  equality-only types of the ch. 2.4 orderability paragraph —
  `Enumeration`, `SumType`, `StructType`, `Text`, `Boolean`, `ListType`
  — have no order at all. Two operands from *different* families are
  equally ill-formed: neither is wrong alone, their combination is, and
  it is reported at the fold node for that reason, in two distinct
  messages — a precision mix is not a number-versus-date mix and does
  not read as one. The clause is **independent of the wire form**: it
  reads the node's operand list and nothing else, and both forms spell
  their contributors there — the instance fold as one set-valued
  argument, whose type is the element attribute's datatype, the
  articulated fold as two or more listed operands, each deciding for
  itself. The two are no dichotomy the rule could have keyed on anyway:
  4 of the corpus's 14 articulated ordering folds carry
  `elementType` besides — the tag v0 stamps vestigially
  (`RUNTIME-SEMANTICS.md` ch. 1.2) — and reading the operands is exactly
  what makes that harmless. An operand whose type nothing declares
  **defers**: since L2 (2026-09-24) that is an unresolvable reference, an
  `Unresolved` of unknown type, or a count source — whose name is a role
  and no attribute, and whose place under an ordering fold is WF-093 (a)'s
  refusal. *(History until L2, the defer as it stood:)* an operand carrying
  the unknown sentinel, or no type at all, deferred — the sentinel was the
  ch. 4.3 row-1 wart WF-034 tolerated in a fold source. Where the defer is *unreachable* it
  is dead weight and where it is reachable it is load-bearing, and since
  WF-034's seventh wave and its fix round (2026-09-06) only the **single
  argument of an aggregation** reaches it at all. Both routes in are shut,
  and shut symmetrically: an operand walked as an ordinary expression is
  refused by WF-034 in either spelling (`unknown-type sentinel inside a
  structured node`, `value node without a static type`, both measured), and
  so is a fold operand that is not a fold source — the same pair, on the same
  guard, on the branch that replaces the walk. That narrows this defer to the
  one position ch. 4.3 exempts, in both spellings for the same reason (a fold
  source's wire annotation is not what any consumer reads there — measured:
  nulling the source type of a single-argument fold leaves three goldens
  verifier-clean and replaying unchanged through all three executors), and
  takes the parity document below out of the picture entirely. What the defer
  held open, then, was the wart itself — an instance ordering fold over a
  bare role source, which
  was verifier-clean until 2026-09-06, when WF-093 (a) took it, and which **no** executor carries (measured:
  `ref` faults `value aggregation over a bare role source`, the ASP
  backend refuses the expression kind, the Drools backend throws a
  constraint-evaluation exception). That is an **open flank recorded
  here, not a shape claimed sound**; the one parity document that also
  spells a sentinel at an ordering operand is refused already, on
  several rules including WF-034 at that very position, so it is not
  what the defer protects. *Why a rule and not a note:* the refused shapes
  are where the three executors stop agreeing about what a document even
  is. Measured
  one form at a time on verifier-clean documents, before the clause: the
  instance fold over an enumeration element had `ref` silently succeed
  with one contributor and fault from the second on (`unordered
  comparison`), the ASP backend refuse at its enum carrier fence, and the
  Drools backend **throw** on one contributor as readily as on two (its
  accumulating min/max lowering assumes a number) — a verifier-clean
  document crashing an engine. The articulated fold over enumeration
  labels had `ref` fault and the ASP backend refuse while Drools **played
  it and answered**, the alphabetically extreme label under Java's
  `Comparable` on the written spelling: not an order anyone designed, and
  a `ref` ≢ Drools value divergence. Over a sum type the ASP backend does
  not refuse the articulated form but **crashes** translating it. And the
  mixed-family fold had `ref` fault, the ASP backend refuse by name, and
  Drools throw a cast. The mixed-PRECISION fold, measured when the
  precision half was closed a day later, splits the same three ways:
  `ref` faults the unordered comparison, the ASP backend refuses ahead of
  translation, and Drools throws a `LocalDate`-to-`ChronoLocalDate`
  cast. Under the clause all five are unreachable through a valid
  document. *One half of this rule stays OPEN,* stated and unchecked,
  with no witness and no verifier arm — named here rather than left to be
  found: unit compatibility between the numeric operands of one fold.
  (Its sister half, precision agreement between date operands, closed on
  2026-09-05 and cost nothing: over the four verified zones — golden,
  red, negative and parity, 492 documents **at that date** — the only
  report that moved was the new negative's.) WF-036 is no cover for the
  open half — a fold is not a comparison, and no arm of WF-036 reaches a
  fold operand — but it is, **since 2026-09-05**, a full precedent for
  the closed one: it crosses units for a numeric ordering, both units and
  precision for an equality, and now the date REPRESENTATION for a date
  ordering (`date ordering across two date precisions (day and
  millisecond), which share no order`, witness
  `corpus/negative/wf036-date-ordering-precision`). The follow-up this
  paragraph tracked — that arm crossing neither, so that a day-versus-
  millisecond comparison was verifier-clean while `ref` faulted
  (`unordered comparison: 2025-03-05 vs @…ms`) and the ASP backend
  refused it as an unscalable datatype — is **closed**, and nothing on
  that arm is left open: dates carry no unit, so the unit half that stays
  open here has no counterpart there. A second
  honesty note on what the closed halves are *tested* on: the number
  family admits a plain number and a percentage (a `Number` whose unit is
  `%`), and no corpus document exercises the percentage half — dropping the
  percentage out of the family
  moves zero of those 492 reports. The index row below says so.
- **WF-039** A written enumeration label MUST belong to the declared
  domain; a numeric literal MUST satisfy the target's sign restriction
  (mirror; also invoked from WF-023). The sign is that of the value the
  lexeme spells, not of the lexeme: `-0`, `0.00` and `0/5` are zero —
  non-negative, neither positive nor negative (since 2026-09-25; until
  then a leading minus counted as negative and only an all-zero lexeme
  as zero). Two restrictions reach a literal:
  its own type's, crossed where the literal stands, and — since
  2026-09-25 — the one the declaration it is written into carries, at
  WF-023's three write positions (a rule target, an object creation's
  attribute member, a defaults consequence); the second stands aside
  where the first already refuses the value, and where the literal's own
  restriction contradicts the target's, which WF-023's sign clause
  reports, so one defect gives one report.
- **WF-040** A DimensionCellRead MUST pin every declared dimension of its
  attribute exactly once; a `dimensionSelection`'s labels MUST resolve
  against the dimension's declared order. *v0 mirror note:* the pinning
  is the node's documented contract and the labels are pre-expanded at
  build (unresolvable selections become Unresolved markers); v1 turns
  both into checks. **Delta (promotion).** (Row 28; the v0 restriction of
  selections to the subject's own single-dimension attributes is frontend
  maturity, not IR scope — noted, not normative.)
- **WF-041** A counting quantifier's `threshold` MUST be ≥ 1 (mirror of
  the v0 error). The sentence is about the VOCABULARY, not about one
  node, so since 2026-09-16 it is checked at both of its hosts: the
  propositional `Compound` and the first-order `QuantifierFold`
  (ch. 4.5), whose WF-108 (e) hands the value here rather than judging it
  itself. What the bound refuses is the same in both places — a
  *threshold* of 0 asks nothing under `atLeast`, and under `atMost` and
  `exactly` it is `none` spelled a second way, so admitting it would put
  two quantifiers on the wire for one reading.
- **WF-042** A TimeWindow MUST have at least one bound, and its bounds
  MUST be date expressions (mirror; v0 checks date-ness only — either
  precision is admitted, unchecked against the operand). WF-111 narrows
  the bound to two shapes, both of day precision — so "either precision"
  holds for the date typing this rule checks and no longer for a
  well-formed window.
- **WF-111** Each bound of a TimeWindow MUST be one of two shapes: a date
  `Literal`, or an `AttributeReference` to a **scalar** (non-time-dependent)
  date attribute of the enclosing form's own subject — un-navigated (no
  navigation role, path or binding), not a set read, on an object type
  the subject satisfies (`S ⊑ O`, ch. 2.2). Either shape MUST be of day
  precision (`Date`, `precision: day`): the window is a range of civil days
  (`RUNTIME-SEMANTICS.md` ch. 3.10) and no clause reads an instant bound.
  *(Maintainer decision 2026-09-23; the day-precision clause adopted by the
  maintainer at this rule's merge.)* That clause carries its own
  motivation, a latent split measured before the rule: a LEEG millisecond-precision
  attribute bound played in the reference interpreter (a leeg bound never
  matches — a dynamic net that looks at the value, and refuses only a
  FILLED instant) while the ASP backend refused it statically, so the two
  answered differently on a verifier-clean document. Day precision closes
  that split statically, for every executor. This is the v0 backends' carried
  pair, promoted from backend maturity to the norm. **Delta
  (restriction):** v0's IR admits any date expression here (WF-042) and
  its backends refuse the rest; v1 refuses it on the wire instead. The
  general date expression — a parameter, a navigated read, arithmetic — is
  an OPEN wire decision, recorded in `RUNTIME-SEMANTICS.md` ch. 3.12: no
  corpus document asks for it, and a bound that is not tied to the subject
  has nothing an ASP encoding could attach its fault to. A position with
  no subject (a day-kind definition) therefore admits the literal only.
- **WF-112** A TimeWindow MUST NOT sit under `none` or a counting
  quantifier (`atLeast`, `atMost`, `exactly`): not as a member of a
  `Compound` with such a quantifier, at any depth, and not inside the
  `filter` or `test` of a `QuantifierFold` with one. `all` and `the` carry
  it. *(Maintainer decision 2026-09-23.)* The window is a gate on the
  RULE (`RUNTIME-SEMANTICS.md` ch. 3.10); a quantifier that counts or
  complements its verdict reads it as something else, and under `none` a
  time-free session — where the window never matches — would make the
  composite true. No reading of that complement is specified, so the
  position is refused rather than left to each executor. **Delta
  (promotion):** v0's backends refuse the position (ch. 3.10's mirror
  note); v1 makes it ill-formed. The quantifier fold is covered by the same
  rule because its quantifiers are the composite's, applied to instances
  instead of parts.
- **WF-043** A whole-period qualifier MUST sit on a time-dependent
  operand (mirror: v0 errors and emits Unresolved). *Recorded scope
  (2026-09-11):* on a characteristic check the cross resolves against ONE
  type — the `host` where present, the enclosing form's subject
  otherwise (WF-100, ch. 4.6 (q1)) — so it reaches the **non-navigated**
  form only. On a navigated check the characteristic lives on the reached
  type, which this rule does not cross; both OCaml executors therefore
  hold the time-dependence fence themselves at the ch. 3.9 fold, because
  without it a non-temporal characteristic would fold over an always-empty
  period set and pass vacuously. Widening the rule to the reached type is
  a candidate, not a decision.
- **WF-044** (consumer conformance) A consumer MUST surface Unresolved
  nodes in its output or coverage report; silently lowering or dropping
  one is non-conforming. This is the unsolved-state harness guard
  (`PLAN.md` §8) written as a rule; its check is behavioural (CI), not
  structural.
- **WF-045** The rule-form dependency graph (ch. 5.1 — attribute,
  characteristic, channel, population and relation nodes; operational
  companion `RUNTIME-SEMANTICS.md` ch. 6.1) MUST be acyclic after
  removing the creation members of recursive rule groups and the shifted
  edges of instance-ordered parts: a dependency cycle is admissible only
  when it passes through recursive creation or is closed by a value of an
  object reading the same value of its predecessor (ch. 5.3; mirror: v0
  warns at build time and caps firing).
- **WF-046** No dependency cycle — including a WF-045-admissible one —
  may carry a negative edge (a negated or negation-quantified
  characteristic read, every read under a leeg/gevuld test in either
  polarity, every read in the range of a quantifier fold under `all`,
  `none`, `atMost` or `exactly` and in its matrix under `none`, `atMost`
  or `exactly`, a negated role check, or a regelversie channel test):
  evaluation strata MUST
  exist (ch. 5.2, 5.3; operational reading `RUNTIME-SEMANTICS.md` ch. 6.3).
  v0's single agenda boundary is the two-stratum special case.

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
| BinderRef | `name`, `source` | reads the nearest enclosing binder (a unary match arm's `binder`; the row-10 card adds lambda and function parameters; the 5c widening adds a `Let` binder, ch. 4.10 — which is what the RegelSpraak emitter now produces for a §13.4.2.10 variabele read). Its type is the binder's declared datatype; since L2 (2026-09-24, B13) the node carries no `type` of its own. Deliberately distinct from `VariableReference`, which since the let-lowering card carries only what a `BinderRef` cannot: a row-24 variable reference the resolver could **not** bind (a forward or cyclic definition) — an honest residual marker, never a bound read; the two roles never share a node |

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
  body; a `VariableReference` remains outside it, row 24.) **The closure
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
- **WF-078** *(removed at L2, 2026-09-24 — history.)* Landed L2
  2026-09-24: a `BinderRef` carries no `type` of its own (B13); its type is
  the binder's declared datatype — a match arm binder's constructor
  payload, a lambda or `FunctionDef` parameter's declared type, a `Let`
  binder's value type, a filter binder's element type — so a binder read
  cannot state another soort, and there is no copy for the rule to
  compare.
  Until then a `BinderRef`'s serialized `type`, where present, had to agree
  in **soort** with the datatype of the binder it resolves to — the soort
  facet of the reference-type identity at this arm, beside WF-089/WF-090
  (domain) and WF-091 (the other facets) — because WF-035 reads the
  operand's type but exempts additive operands, and a soort-lie on a binder
  in an additive position slipped that exemption (verifier-clean, ref
  computing on, clingo faulting the translation). Measured on the
  witness's translation, the `Let` whose type followed the lie now falls on
  WF-076 (`wf076-let-kind-against-body`, `corpus/negative/README.md`,
  L2).

### 4.8 The fold family — ListLiteral, Lambda, IfThenElse, Invoke, Fold, MapList, FilterList, ListLength

`layer: core`; **extension nodes** per the Method (never
emitter-produced; CONSTRUCTS row 10; semantics `RUNTIME-SEMANTICS.md`
ch. 10; design and the ratified closed-kernel decision
`spec/proposals/PHASE5B-KERNEL.md` §3). Together with ch. 2.11 these
carry general folds and function values over list *values*; the
`Aggregation` catalogue (ch. 4.3) keeps its instance-set world
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
`corpus/negative/wf090-*`). Because WF-073's field-value clause and
WF-074's node-type clause are equalities on the *whole* datatype, the
nested forms — a struct field value of another domain, a struct-typed
field read of another domain — are already theirs and WF-090 adds no arm
there.

Both rules resolve the *named* domain in `domains`, never the inline
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
  binders, parameters and literals — arithmetic combinators (`Binary`,
  `Rounding`, `Clamp`) over those are admissible, since what matters
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
  allocation that names it in a role binding (ch. 3.4) and by every role
  check that names it (`RoleMembership`, ch. 4.5; since 2026-10-06).

Which form writes and reads which nodes is the producer→consumer table of
`RUNTIME-SEMANTICS.md` ch. 6.1, whose rows the edge-kind rules below
govern. **Parameters and input-only nodes** — nodes no form writes —
contribute no edges. **Day kinds are not graph nodes** (ch. 5.4).

**A node is identified up to the subtype chain as well as the name**
(ch. 2.2, *2026-09-12*). The name of a type on the wire is a spelling, not
an identity: an instance answers to every type on its chain, so a write
under one spelling is read under another and the two must be the same node
or the edge is lost. The **write** side closes and the **read** side stays
literal — every legal read spelling is reached from the write side, so
closing both would only invent edges. **How far a write closes is decided by
what it writes ON**, and the answer is not the same in all three places, so
the three are spelled out rather than generalised:

- a **population** write closes UPWARD ONLY. An object creation of type T
  grows T's population and every ancestor's, since every T instance is an
  instance of each type on its chain; it grows no descendant's, since
  creation is not polymorphic (ch. 2.2) and mints nothing of a strict
  subtype. A fact creation mints no instance at all and writes no
  population (since 2026-10-06). Until then it wrote the population of its
  target role's filler, literally, as a conservative proxy beside the
  relation node; that proxy closed a cycle that does not exist with an
  object creation counting the members of a role over that type, and
  WF-045 refused the document (`RUNTIME-SEMANTICS.md` ch. 6.1, golden
  `jutterswet-premiepot`).
- a **member** write by a form that ITERATES a type — a rule's `target`, an
  allocation's `targetAttribute` and `remainderAttribute`, a defaults
  group's target — closes in BOTH directions, over the ⊑-**comparable**
  types that carry the member. Such a form writes the member on *every*
  instance of its owner T, and the instance sets of T and S overlap exactly
  when the two are comparable: S above T is handed T's instances, S below T
  reads instances the write covers. Two INCOMPARABLE types share no
  instance and are left apart — which is why this is a comparability walk
  and not a rewrite of both sides to the declaring type. That shorter
  rewrite merges two sibling subtypes of one parent into one node and
  refuses a document that runs (measured 2026-09-12: two incomparable
  subtypes each computing one inherited attribute from the other, clean in
  the reference interpreter and refused under the merge).
- a **member** write by an **object creation** — an `ObjectCreation`'s
  member assignment (ch. 3.3) — closes UPWARD ONLY, like the population
  write beside it and for the same reason. It writes on the ONE instance
  the creation mints, whose concrete type is exactly the created type, so a
  strict subtype of that type is never written even though it inherits the
  member. Reading this one as an extension write is unsound in the
  over-refusing direction and was measured to be so, 2026-09-12: a creation
  that mints its own SUPERTYPE and fills an inherited attribute drew a
  false edge back to a rule anchored at the subtype it can never mint, and
  a model with no cycle in it became a WF-045 refusal — with the reference
  interpreter, which shares this graph, declining to evaluate while the ASP
  backend answered the pinned numbers, so the differential square split on
  a document neither of them had any quarrel with.

Both halves make WF-045's promise true where it was not. Measured
2026-09-12, one witness each, both verifier-clean before and both ending in
`no fixpoint within 100 passes` in the reference interpreter: a creation
whose new type is a subtype of its own subject
(`corpus/negative/wf045-subtype-creation-cycle`), and two rules whose mutual
dependency runs through one inherited attribute pair spelled at two
different owners (`corpus/negative/wf045-inherited-attribute-spelling-cycle`).

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
  comparison; the leeg/gevuld test reads negatively, below) by a form whose
  write is **irrevocable** — a characteristic attribution (monotone), a
  consistency or uniqueness (channel facts are never retracted), or a
  creation. Such a form MUST evaluate strictly above the producers of what
  it reads: a transient it acted on would persist. Within a WF-045-admissible
  recursive-creation cycle the strictness is discharged by the
  one-generation-per-instance progression (ch. 5.5), not a stratum boundary.
- **negative**: a negated characteristic read, a characteristic read under
  a negation-like quantifier (*geen*, *ten hoogste*, *precies* — their
  truth can be lost by a later monotone write), every read under a
  leeg/gevuld test in either polarity (*2026-09-23*, below), every read in
  the range of a quantifier fold under `all`, `none`, `atMost` or
  `exactly` and in its matrix under `none`, `atMost` or `exactly`
  (*2026-09-23*, below), a negated role check (*2026-10-06*: it holds on
  absence, and a later link makes it false), or a channel test
  (regelversie). **Once negative,
  conservatively negative** — a double negation does not cancel. A negative
  edge is stratum-strict *and* forbidden on any cycle (WF-046).

**The leeg/gevuld test is a negative edge** *(maintainer decision
2026-09-23, card `verifier-leegtest-negafhankelijkheid`)*. The decision, in
full:

> A LEEG/GEVULD test counts in the WF-045/046 walk as a NEGATIVE edge, in
> BOTH polarities: the test depends on absence, whether it carries
> `negated: false` or `negated: true` (wire `negatie`). A cycle through
> such an edge is forbidden by WF-046, also through a recursive creation
> group that WF-045 admits. Motivation: verifier-clean documents whose
> meaning depends on the evaluation order (the reference interpreter
> answered R3 order-dependently, the ASP backend found five stable models)
> are the class this repository never admits.

*Editorial note (the L5 landing):* this decision and the next one in this
section are quoted in the wire spelling of their day; on the current wire
the LEEG/GEVULD test is a `Predicate` of kind `empty` or `filled` whose
polarity field is `negated` (the wire `negatie` above), and the quantifiers
ALLE, GEEN, TEN_MINSTE, TEN_HOOGSTE and PRECIES of the next decision are
`all`, `none`, `atLeast`, `atMost` and `exactly`.

Every node the operand reads is read negatively, the hop relations of a
navigated operand included: an absent link makes the operand leeg, so the
test reads the absence of the link as well. The rule holds in every position
the walk reaches (a rule or creation condition, an assertion, a fold filter,
a quantifier matrix, a composite member). On an admissible creation cycle
the one-generation progression discharges stratum-strictness (above), and it
does not discharge this read: the creation acts on an absence that the cycle
fills afterwards. Measured on two probes of the
`corpus/golden/anchor/jutterswet-wachtersketen` shape, both verifier-clean
before the rule, with one input Wachter of rang 1. **R1**: the recursive
creation mints a successor while "het merk is leeg", and the rule that
writes merk = 1 reads the successor's rang under "gevuld". The reference
interpreter minted a chain of five in which each of the first four creations
is justified by a leeg merk that its own final world fills; the ASP backend
found no stable model. **R3**: the same cycle with "niet gevuld" in the
creation and the successor's rang tested leeg in the rule. The reference
interpreter answered merk(W1) = 1, which is what its pass order (rules
before creations) produces; the opposite order mints W2 and leaves merk(W1)
leeg, and the ASP backend found five stable models, one per chain length.

The witnesses are one per polarity, one for the presence-true reading and
one for the route into a quantifier fold.
`corpus/negative/wf046-empty-test-recursive-creation` is R1, with
`negated: false` throughout; `wf046-empty-test-negated-recursive-creation` is
R3, spelled with `negated: true` throughout. In
`wf046-empty-test-presence-recursive-creation` the only test on the cycle
is a negated leeg test. That document has one fixpoint, in which nothing is
minted, and it is refused because the decision counts both polarities; it
is the witness that separates the decision from the narrower reading "only
a test that holds on absence is negative". In
`wf046-empty-test-quantifier-fold-recursive-creation` the test is the matrix of an
`all` fold over the successors that the creation mints.

Off a cycle the rule orders a reader strictly above the writers of its
operand, which also closes an order dependence in the reference
interpreter: a leeg test in an ATTRIBUTE rule on a derived attribute shared
its writer's stratum, and the reader's position in the document decided
whether it acted on the transient leeg (`RUNTIME-SEMANTICS.md` ch. 6.4).

*Cost:* none in the corpus. Counted 2026-09-23 on the corpus of that day's
main line, none of the 31 leeg/gevuld tests in 29 corpus documents reads a
node that some form writes, so the rule turns no existing edge negative. No
verdict moved in the golden (236), red (48) or negative (441) zones, and
the reference interpreter's replay of all 652 golden and red cases is
byte-identical.

*Not covered, recorded open:* other conditions that hold on absence without a
`negated` field keep their positive or stratum-strict reading: an inequality
with one leeg operand, a numeric equality of two leeg operands, a count or a
filled-value count tested against zero, a propositional `none` over a
comparison, and an ordering over arithmetic that coalesces leeg to 0. On an
admissible creation cycle each of them was measured verifier-clean with the
executors split, and off a cycle each leaves the reference interpreter
order-dependent in the condition of an attribute-writing form
(`RUNTIME-SEMANTICS.md` ch. 6.1, recorded residuals). This decision does not
cover them. The quantifier fold, first measured in the same round, is
covered by the decision that follows, but only in its range under `all`,
`none`, `atMost` and `exactly` and in its matrix under `none`, `atMost`
and `exactly`. A comparison in the matrix of an `all` or `atLeast` fold
keeps its positive or stratum-strict reading.

**The quantifier fold's range and matrix** *(maintainer decision 2026-09-23,
same card, second fix round)*. The decision, in full:

> The quantifier classes now come under the arm, with this reading: the
> RANGE of a quantifier fold is a negative edge under ALLE, GEEN,
> TEN_HOOGSTE and PRECIES (the outcome can be lost as the range grows; not
> under TEN_MINSTE); the MATRIX, comparisons included, is negative only
> under GEEN, TEN_HOOGSTE and PRECIES (under TEN_MINSTE and ALLE the
> outcome is monotone in the matrix). Motivation: the card builder's
> variant carried `neg` through to filter and matrix for EVERY quantifier,
> and the review round found the counter-witness — a recursive creation
> under "rang < 5 en ten minste 1 opvolger heeft rang > 3", on which ref
> and clingo agree (nothing minted, at rang 1 and at rang 4), the branch
> tip passes it and the variant gives WF-046: an unjustified refusal.

How it reads in the walk. The range is the reach (`sourceRole` or
`sourcePath`) together with the `filter`: those are the fold's range fields
in ch. 4.5, and a member that newly passes the filter joins the range the
way a new link does. Since L2 (2026-09-24, B12) the reach is always written:
the loader admits a quantifier fold with exactly one of `sourceRole` and
`sourcePath`. *(History until L2:)* a bindingless fold had no written reach;
its range was the one connecting relation WF-048 demanded, read as that
relation plus the element population, and before 2026-09-23 the walk drew no
range edge at all for such a fold. "Negative" means every read under the part,
comparisons and positive characteristic reads included. In a monotone part a
read keeps its own kind, so a negated characteristic, a leeg/gevuld test and
a channel test stay negative there, and a positive characteristic read in a
monotone part (the matrix of an `all` fold, either part of an `atLeast`
fold) is now positive where the walk used to read it negatively. A fold
under an enclosing negation-like composite is negative in both parts.

The witnesses. `corpus/negative/wf046-quantifier-fold-all-range` is an `all`
fold over the successors a recursive creation mints (the R5 shape): its
matrix is monotone, so the range alone refuses it.
`wf046-quantifier-fold-none-comparison` is the `none` twin, where range and
matrix both close the cycle. In `wf046-quantifier-fold-none-matrix` the `none`
fold ranges over an input relation and its matrix reads a merk the cycle
derives, so the matrix alone refuses it; before the rule the executors split
there without failing. `wf046-quantifier-fold-range-other-type` is the one
witness whose creation mints ANOTHER type: "richt een post in" mints a
`Post` for a `Wachter` under an `all` fold over the `Wachter`'s posts, and
the creation writes that relation through its back-link, so the range
alone refuses it. Until L2 the same document was
`wf046-kwantorvouw-bindingloos-bereik`, a bindingless ALLE fold whose range
was the one connecting relation WF-048 demanded; at L2 the migration
derived that range (`bemanning`/`post`) and the translation still fails on
this rule, so it was rewritten under the new name rather than removed
(`corpus/negative/README.md`, L2). The remaining clauses have one witness each, every
one the golden's model below with another creation condition:
`wf046-quantifier-fold-at-most-range` and `wf046-quantifier-fold-exactly-range`
(the range under `atMost` and `exactly`, over the successors the creation
mints, with an input-only matrix), `wf046-quantifier-fold-at-most-matrix` and
`wf046-quantifier-fold-exactly-matrix` (the matrix under `atMost` and `exactly`,
over an input-only range, reading a merk the cycle derives), and
`wf046-quantifier-fold-under-none` (an `atLeast` fold, positive on its own,
under an enclosing propositional `none`). The clean witness is the golden
`corpus/golden/jutterswet-opvolgerquorum`: the counter-witness above as its
first case, an `atLeast` fold whose range grows under the creation without
losing the verdict, and an `all` fold whose matrix turns true when another
creation fires. The reference interpreter, the ASP backend and both Drools
branches agree on all four of its cases. Each clause has a mutation that
fails gate 3 on its own witness (measured 2026-09-23): the range positive
under `all`, `atMost` or `exactly`, the matrix positive under `none`,
`atMost` or `exactly`, the enclosing context ignored, and no bindingless
reach; and in the other direction the range negative under `atLeast`, or
the matrix negative under `atLeast` or `all`, each of which refuses the
golden.

*Recorded open, not decided here:* "monotone in the matrix" is a property of
the verdict, and it holds only while the matrix itself cannot lose its
truth. A comparison that a growing value falsifies can: in the golden's
model, with the `all` matrix read as "merk kleiner dan 2" (or "merk ongelijk
aan 2") and merk the number of successors, the document is verifier-clean
and the executors split on case 4 (the reference interpreter and the Drools
exec branch 2, the ASP backend and the DRL-text branch 1); with "ten minste 1
ploeglid heeft een merk kleiner dan 2", and a ploeglid under W3 so that W3
mints, the reference interpreter and both Drools branches answer 2 and the
ASP backend 1. The same class without a fold predates this card: "het merk
van de ploegleider is ongelijk aan 2" is verifier-clean on the main line and
splits the reference interpreter (2) from the ASP backend (1). The
one-generation progression that discharges strictness on an admissible
cycle is unsound for every read whose truth can be lost, a growing value
included (`RUNTIME-SEMANTICS.md` ch. 6.1).

*Cost:* none. Over the 755 corpus and parity-zone documents that this card
did not add, 62 read positions in 18 documents change kind against the
leeg/gevuld rule alone (54 turn negative, and 8 characteristic reads in a
monotone part turn positive), and none of them has a writer. Against the
main line both rules together change 82 positions in 35 documents, again
none with a writer. No verdict moved, and the reference interpreter's
replay of the 652 golden and red cases is byte-identical.

**The WF-051 defaults fold.** A defaults group (ch. 6;
`RUNTIME-SEMANTICS.md` ch. 7) folds into this graph as the writer of its
target: every default-justification read — and, on attribute defaults,
every consequence read (the gevuld gate makes it decision-bearing) — is a
**negative edge** toward the group's write, recursed through the nested
`exceptions` sub-defaults (ch. 6.1; `RUNTIME-SEMANTICS.md` ch. 7.9); the
initialisation default's implicit input-emptiness justification reads
nothing. An ordinary rule that the fold of `RUNTIME-SEMANTICS.md` ch. 7.10
takes into a defaults stack is a default there, so its condition and value
reads are negative edges as well — a folded initialisation's condition
included; an executor stratifies a composed stack by every form it was
composed from. WF-051 names this defaults-specific rule; the WF-045/046
principles apply to the extended graph unchanged, and the shifted edges of an
instance-ordered part (ch. 5.3) leave its walk as they leave theirs: a
defaults group may sit on such a cycle, its displacement reads of the
predecessor included.

### 5.3 Acyclicity and stratified negation

- **WF-045 (acyclicity).** The dependency graph MUST be acyclic after
  removing every creation form — object creation or relation creation —
  that is a member of a recursive rule group (ch. 3.9), and every shifted
  edge of an instance-ordered part (below): a cycle is admissible **only**
  when it passes through such a form or is closed by such edges. These are
  the two controlled exceptions. Recursive creation advances exactly one
  generation per instance, WF-033 guards the author's termination
  obligation, and the runtime's bounded budget makes divergence a fault
  (`RUNTIME-SEMANTICS.md` ch. 5.5/6.2). The instance-ordered cycle advances
  one object at a time along a chain that the case input lays, and a cycle
  in that chain is a fault of the case input, EF-033, before any rule fires
  (`RUNTIME-SEMANTICS.md` ch. 6.2). The two are disjoint: the first takes
  forms out of the graph and settles strictness by generations, the second
  takes edges out and settles it by the depth of an object in its chain,
  and a part of the graph that holds a creation of a recursive group is no
  instance-ordered part. With WF-045 and the single-writer disciplines
  (WF-026, WF-079) the fixpoint of the positive part is unique for every
  case the loading admits: rule, activation and insertion order do not
  matter.
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
  two-stratum special case. The shifted edges of an instance-ordered part
  are not edges of the graph this rule walks, so a negative shifted edge —
  a leeg/gevuld test on the predecessor's value, a negated characteristic
  of the predecessor — is admitted on such a cycle: the predecessor is
  settled before the object reads it.

**The instance-ordered cycle** *(voorlopig besluit 2026-10-22, card
`cycle-across-instances`; the owner can reverse it)*. A node of the graph is a type and a member
(ch. 5.1), not an object. A value of an object that reads the same value of
another object of its type — the einddag of a Vergunning that is extended
by the restant of its vorige vergunning, whose restant depends on its own
einddag — closes a cycle of the graph, while per object the reads form a
chain that ends at an object without a predecessor. Until this decision
WF-045 refused every such document, and a statute rule of that form (the
duration of a new right extended by the remainder of the earlier one) had
to be written with a member of its own and the earlier value as input of
the next case. The definitions, all read off the document:

- A **switch relation** is a declared fact type with two roles whose
  fillers are ⊑-comparable types (one type T and its chain), whose role a
  hop over it reaches is to-one (`many: false`: the predecessor), and whose
  link set no form writes (no fact creation, no back-link of an object
  creation), so that its links are the case's. A reciprocal fact type is
  none: its links run both ways.
- A **satellite relation** of T is a declared fact type other than the
  switch relation, with two roles, whose link set no form writes, one of
  whose roles is filled by a type comparable with T and is to-one (the
  owner's role: a satellite belongs to at most one object of T), and whose
  other role is filled by a type not comparable with T (the satellite).
- The **path** of a read is the list of role bindings from the subject of
  the form to the object the read lands on: the ranges (`sourceRole`,
  `sourcePath`) of the folds it sits in, then its own hops (`path` of an
  attribute reference, `navigation` of a characteristic check). An own read
  has the empty path.
- A read is **shifted** when its path is one hop over a switch relation to
  the predecessor's role, followed by at most one hop over a satellite
  relation of T to the satellite, every hop reaching a to-one role: it
  lands on the predecessor or on its one satellite. A read is **local**
  when its path is empty or one hop over a satellite relation of T, in
  either direction, reaching a to-one role: it lands on the object, its
  satellite or its owner.
- A **shifted edge** is an edge p → c whose node c reads only in shifted
  reads. Where c reads the node shifted and also otherwise (directly, or
  over another relation), the edge is an ordinary one.
- An **instance-ordered part** is a strongly connected part of the graph
  that holds no creation of a recursive group and only rules and defaults
  groups (the forms that write per subject of their own type), whose
  internal edges include at least one shifted edge, whose shifted reads all
  walk one switch relation to one role, every read behind whose internal
  edges is shifted or local, whose satellite types are each reached through
  one satellite relation, and which is acyclic without its shifted edges.
  Its shifted edges are the ones WF-045, WF-046 and WF-051 leave out. The
  **level** of a member is its longest path over the internal edges that
  remain, a strict or negative edge counting one.

Each clause has a reason and a witness. Without the to-one predecessor the
predecessor is a set: `corpus/negative/wf045-instance-cycle-to-many-predecessor`
(a fold over all vorige vergunningen). Without "no writer" the order of the
chain is known only after the evaluation:
`wf045-instance-cycle-created-relation` (a fact creation links the
relation). Without the owner's to-one role a satellite is shared, and the
depth of one object does not order what another object's restant feeds into
it: `wf045-instance-cycle-shared-satellite`. Without "only shifted" a direct
read rides along on an edge the walk leaves out, and the object reads itself:
`wf045-instance-cycle-direct-and-shifted-read`. Without the to-one hops of a
shifted read the read lands on a set: `wf045-instance-cycle-to-many-satellite-step`
(a fold over the termijnen of the predecessor). Each witness but the fourth
is verifier-clean with that one clause relaxed in its document (measured
2026-10-22); the fourth closes a cycle on one object, which no order of the
objects breaks. A set of predecessors (depth one more than the deepest) and a
set of satellites of the predecessor are sound and left to a later decision;
"local" is narrowed in the same way, and a part that reads a node of its own
over a fold of the population or over any other relation is no
instance-ordered part. The clean witness is the golden
`corpus/golden/jutterswet-vergunningketen`: the einddag with a leeg-test on
the predecessor's restant, and the verlengde einddag in the positive form,
over a satellite Termijn, in a chain of one, two and three Vergunningen;
`ref/`, the ASP backend and both Drools branches answer its pinned values.

*Cost:* none. Measured 2026-10-22 over every corpus and parity-zone document,
the verifier's reports and its `--dependency-edges` (gate 15) are byte-equal
to the main line's for every document but the new golden: the edge list does
not change, only the walk of WF-045, WF-046 and WF-051 leaves edges out.

*v0 mirror note:* v0 warns on a circular reference at build time and bounds
execution with a fire cap; WF-045/046 make the refusal structural.

### 5.4 Day kinds are stratum 0

A day-kind definition (ch. 3.7) classifies a bare date; it is **input,
evaluated before the rule graph** — stratum 0 — and is therefore **not** a
dependency-graph node and carries **no** edges. Two well-formedness rules,
both at WF-031's home (ch. 3.7), keep that framing sound: a day-kind
condition is **pure** — it references only dates, date-components,
`easterSunday`, literals, parameters and other day-kind names, never
derived object state (WF-031) — and the **day-kind reference graph is
acyclic** — a day-kind condition may reference other day kinds, but
mutually recursive definitions have no fixpoint (WF-085). A day kind is
therefore consulted only as a stratum-0 classifier of a date, never as a
derived read inside the rule fixpoint, and the two rules above are what
make the classification well defined.

*Executed since 2026-09-19.* The predicate is now genuinely evaluated —
by the day-kind **duration** forms (`RUNTIME-SEMANTICS.md` ch. 3.8),
which walk the calendar day by day and ask `P_k` of each day. That is
still stratum 0: the answer depends on the day, on literals and on
parameters, and on nothing the rule graph derives. Two boundaries hold
it there. WF-031's purity clause bars derived state AND session values,
so `P_k` is a function of the day alone; and the day-kind predicate **as
a rule condition** (§8.1.5, "… is een werkdag" in a voorwaarde) stays
refused by name in `ref/` and `backends/clingo` — the nested reference
INSIDE a day-kind condition is the one place it is evaluated, which is
the §9.8 composition WF-085's reference graph is about. (Option A of the
ratified stratification round,
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
never played mis-stratified. Since 2026-10-09 the ASP backend takes it for
one form: a member initialisation of an object creation that reads a node
a later generation of the creation's own stratum can still change, because
a creation on a dependency cycle that WF-045 admits in that stratum, the
creation itself included where it lies on one, reaches a writer of the
node in the dependency graph in a version the case's date admits; the
backend has no moment of making and refuses such an initialisation by
name (`RUNTIME-SEMANTICS.md` ch. 5.5, Generations). A backend realising a
**coarser**
stratification than the graph mandates without refusing the uncovered forms
is non-conforming.

*Recorded flank (measured 2026-09-23 with the gate-12 jar, both branches,
open).* That guard does not reach the leeg/gevuld test, and the divergence
has two forms.

- On the subject's OWN operand, in a fold filter or a quantifier filter
  inside a condition, and in the matrix of a quantifier fold (measured
  2026-09-30, card `drools-exec-kwantorvoorwaarde-eigen-operand`: with "de
  bergdag van geen van de vondsten van de Berging gevuld is" declared before
  the rule that derives the bergdag, both Drools branches write 1 where
  `ref/` and `backends/clingo` answer leeg), it depends on the document order. With the reader
  before the rule that derives the tested attribute, both Drools branches
  act on the transient leeg: an attribute reader writes 1 and a
  characteristic reader attributes its characteristic, where `ref/` and
  `backends/clingo` answer leeg and absent. With the writer first, both
  branches agree with them. (A fold in a rule's VALUE is re-evaluated, but that did not suffice:
  measured 2026-09-30, card `drools-exec-vouwfilter-latere-afleiding`, the
  exec-model branch's NO_LOOP dropped the revision when the rule's own write
  was the subject's last change, and in both Drools branches an ordering fold
  whose subselection emptied kept the value it had computed over the
  transient source, since leeg decides nothing on a standing target,
  RUNTIME-SEMANTICS ch. 7.8.)
  *Closed in the exec-model branch on 2026-09-30 for a test inside a fold
  (card `drools-exec-vouwfilter-latere-afleiding`, golden
  `jutterswet-zwaarste-vondst`): a rule is placed below the writers of what a
  fold in its condition or value reads negatively, in the polarity of
  WF-046, and a rule whose value folds with a subselection carries no
  NO_LOOP. A leeg or gevuld test outside a fold, and the DRL-text branch,
  keep the form described here.*
  The exec-model branch's navigated leeg test, over one role and since
  2026-10-01 over a chain, has this form as well (measured 2026-10-01, card
  `drools-exec-voorwaarde-twee-rollen-ver`: with the reader first it writes
  the vinddag where `ref/` and `backends/clingo` answer leeg).
  *Closed in the exec-model branch on 2026-10-01 for a test anywhere in a
  condition or a value (witness `ReaderUnderWriterWitnessTest`): a rule is
  placed after every writer of what it reads under a leeg or gevuld test,
  and a characteristic assignment after every writer of what it reads
  stratum-strict (RUNTIME-SEMANTICS ch. 6.1), with every node such a writer
  waits on — an ordinary rule declared after it, a defaults group, an object
  or fact creation, an allocation — strictly below it in salience or level
  with it and added before the rule; a consistency rule takes the same
  placement, and a rule's companions take the rule's salience. A leeg or
  gevuld test on a cycle with its writer, its own target included, is
  refused by name (WF-046). The exec-model branch orders here more strictly
  than the graph asks: a comparison in the condition of an attribute-writing
  rule reads positively (RUNTIME-SEMANTICS ch. 6.1), and the branch places
  that rule after the writers of its operands as well, a finer order that
  reaches the same fixpoint (before, with two operands derived later, it
  wrote where `ref/` and `backends/clingo` answer leeg); on a cycle such a
  rule keeps its place. The DRL-text branch keeps the form described here.*
- On a NAVIGATED operand in a condition it does not depend on the order.
  *Closed in the exec-model branch on 2026-09-28 (card
  `drools-exec-voorwaarde-genavigeerde-afleiding`, golden
  `jutterswet-meldingsketen`): a condition that reads through a to-one
  navigation is tested after a far-side join on the reached object; the
  DRL-text branch keeps the form described here.*
  *Widened in the exec-model branch on 2026-10-01 (card
  `drools-exec-voorwaarde-twee-rollen-ver`, golden `jutterswet-havenketen`,
  witness `ChainConditionWitnessTest`): a condition that reads through a
  chain of two or more to-one roles is tested after a far-side join on every
  object the chain passes, so a relation in the middle of the chain that a
  fact creation sets while the rules run is seen too; a rule's value over a
  chain takes the same joins (before, it joined the reached object only and
  read leeg when the middle relation came later). The DRL-text branch
  refuses such a condition by its marker and keeps the one join in a value.*
  *Widened again in the exec-model branch on 2026-10-01 (golden
  `jutterswet-late-vondst`, witness `FoldFilterFarSideWitnessTest`): the
  subselection of a fold, and the filter and the matrix of a quantifier
  fold, read with the element as subject (RUNTIME-SEMANTICS ch. 3.4a,
  3.9a); where such a test reads through a role of the element, the source
  of the accumulate joins every object the read reaches (every object a
  chain passes), and the subselection is tested on the element bound again
  after those joins, so a later write on the reached object revises the
  fold. Before, the test sat on the element's pattern and the fold kept the
  members it had at insertion: an earliest date read leeg, a count 0 and a
  count or quantifier in a condition never fired, where `ref/` and
  `backends/clingo` read the derived far value. A union selection that
  reads so is refused by name. The DRL-text branch refuses such a test by
  name.*
  With a rule deriving the far side's attribute, both branches answer as if
  that attribute were never written, in either order: "de a van het ding is
  leeg" yields 1 where `ref/` and `backends/clingo` give leeg, and the
  presence tests "… is gevuld" and "… is groter dan 3" yield leeg where
  they give 1. The cause is not the order: a navigated read in a rule's
  CONDITION does not react to a later write on the far side in either
  branch, because the backend gives that far-side reactivity to a rule's
  value and not to its condition. A salience alone therefore does not cover
  it, and this form predates the leeg/gevuld decision and reaches any
  navigated condition, a comparison included.

For the own characteristic reader the edge was already stratum-strict before
this date; for the own attribute reader it became negative with the
leeg/gevuld decision (ch. 5.2). No corpus document has either shape, so
gate 12 cannot see them. The fix belongs to the backend: far-side
reactivity for a condition or a named refusal, and a salience below the
operand's writers for the order-dependent form (in the exec-model branch:
the far-side join since 2026-09-28, the salience since 2026-10-01).

*Recorded (measured 2026-10-01, card `drools-exec-objectcreatie-vouw`;
closed in the exec-model branch the same day, open in the DRL-text branch).*
A creation reads stratum-strict (RUNTIME-SEMANTICS ch. 6.1): it evaluates
strictly above the producers of what its condition and its toekenningen
read. Both Drools branches fired an object creation once per subject, at
the default salience, after the ordinary rules and before the fact
creations and (in the exec-model branch) the defaults groups, so a producer
later on the agenda was not seen: with a toekenning that reads a value a
defaults group writes, the exec-model branch wrote leeg where `ref/` and
`backends/clingo` write 7 and 15; with a count over the objects a creation
declared after it mints, or over a relation a fact creation lays, both
branches wrote 0 where they write 1. *Closed in the exec-model branch on
2026-10-01 (witness `CreationOrderWitnessTest`):* the object and the
fact creations are nodes of the order that already placed the defaults
groups; a creation reads its condition, its toekenningen, the population
of its subject and the links its folds, quantifiers, navigations and hops
follow, writes its toekenningen, the created type's population and its
link, and goes below every node it waits on that would otherwise fire
after it, a rule the stratification places lower and a creation declared
after it included; a recursive creation stays on its generations
(RUNTIME-SEMANTICS ch. 5.5). *Closed in the exec-model branch the same day
(witness `ReaderUnderWriterWitnessTest`):* a reader of a creation's output
that writes irrevocably, such as a characteristic assignment over a count of
the created objects or over the links a fact creation lays, and a leeg test
on what a creation writes, wait on the creation; before, a characteristic
was attributed where `ref/` and `backends/clingo` leave it absent. What
stays open: the DRL-text branch orders no creation, so a later creation and
a fact creation still read 0 there and a reader of their output acts on
that 0, a coarser order than the graph without a loud refusal.

*Recorded flank, the ASP backend (measured 2026-09-23, open).* Its `all`
fold is a double negation (a helper for "some member fails the matrix",
read under `not`). On a POSITIVE cycle through the matrix, which the
quantifier-fold decision admits, that encoding has a second, self-supporting
stable model where the graph mandates the least fixpoint. In the model of
the golden `jutterswet-opvolgerquorum`, with W1 and W3 each leading the other
and each carrying an input opvolger of rang 4, the document is
verifier-clean, the reference interpreter answers merk 1 for both, and the
ASP program has two models: merk 1 and 1, and merk 2 and 2 with both
creations justifying each other. The runner happens to pick the first. The
same holds for a positive characteristic read in the matrix ("alle
ploegleden zijn actief", actief derived from the number of successors), a
document the walk refused before the decision and admits after it. A
`atLeast` fold, encoded as a count, has one model on the same input. No
corpus document has the shape. The fix belongs to the backend: a monotone
encoding of the `all` fold (a count) or a named refusal.

*Recorded (measured 2026-10-02, ronde klein 2, open).* The exec-model branch plays a document whose dependency
graph has a cycle that WF-045 refuses to a fixpoint, without a word: a value of one object read from the same value
of another object of its type over a role, the same through a defaults group (WF-046 and WF-051 as well), and a
count over a characteristic that reads the count over a role. `ref/` refuses each under WF-045; the ASP backend
answers the first two and finds no stable model for the third. The branch refuses by name only a leeg or gevuld
test on a cycle (WF-046). The RegelSpraak frontend refuses such a document before it is written since the same day,
in the verifier's sentences (WF-045, WF-051; `grammar/DIVERGENCES.md`). Two kinds of cycle were excepted until
2026-10-06 and are refused like any other since then: a cycle its own warning names on a rule's own subject, the
refusal naming WF-046 too where a negative edge lies between two forms of the cycle (negative
`wf046-characteristics-read-each-other-negation`), and a cycle through an object creation outside a recursive group. The
second exception stood because the casus of the parity zone carried such a cycle; it was the relation creation's
population proxy (ch. 5.1), which went the same day.

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
list `prioritizedDefaults` under `rules` (top-level until M1) of `PrioritizedDefaults` nodes —
never emitter-produced. The fields below carry their names since L5; until
then the wire kept the v0 names each row gives.

| Field | Type | Meaning |
|---|---|---|
| `name` | string | v0 `naam`. A **named form**: the wire type is nullable and WF-107 is what demands the name, and WF-020 holds it unique in the same namespace as the ch. 3 rule forms — a defaults group and a rule may not share one |
| `subject` | type ref | v0 `onderwerp`: the iterated subject type |
| `characteristic` | string? | v0 `kenmerk`: the target characteristic — **exactly one** of the two targets; absent on an attribute group (a `null` until L5) |
| `attribute` | string? | v0 `attribuut`: the target attribute |
| `defaults` | non-empty list of Default | **descending** priority: highest first |
| `unordered` | boolean? | v0 `ongeordend`: declared-unordered (ch. 7.2 there) — multiple defaults groups on one target are admissible iff **every** one carries it; the group decides jointly, and differing simultaneous decisions route to the §9.5 channel (keyed by the target form; no write — the target keeps what the fact base holds). An absent `unordered` is `false`: the group is not declared unordered |
| `source`, `validity` | — | v0 `bron` and `geldigheid`; `source` is the source reference of ch. 4 |
| `origin` | Origin? | extension field: the declaration a producer made this one from, and the form that made it (ch. 2.13); absent on a declaration the source declares |
| `designations` | list of Designation? | extension field: the group's names in the vocabularies (ch. 2.12) — a defaults group is a named form, as the rule it is lifted from |

**Default**: `decision` (v0 `besluit`, boolean? — characteristic
defaults only: attribute vs do-not-attribute; absent where a default carries
no decision), `justification` (v0 `voorwaarde`, a condition; absent only on
the initialisation default and a group-default), `consequence` (v0
`consequentie`, expression? — attribute defaults only), `initialisation`
(v0 `initialisatie`, boolean? — the ch. 7.3 default: justified iff the target
is empty in the input facts; lowest only, **root-only**; an absent
`initialisation` is `false`: the default is not the initialisation default),
`exceptions` (v0
`excepties`, the optional nested exception groups — the **Default.exceptions and
ExceptionGroup** paragraph below), `source` (v0 `bron`), `name` (string? —
an extension field, written just after `node`: the name the source gives the
block the default comes from; absent where the source gives none), and
`statuteAnnotations` (list of string? — an extension field, written last:
the statute annotations written directly above the block the default comes
from, in source order, the field a Rule carries, ch. 3.1, under the same
WF-082). Until L5 an absent
`decision` or `justification` was a `null`. v0 has neither `name` nor
`statuteAnnotations`.

**Default.name has no semantics**, as `source` has none: it resolves
nothing, it is in no namespace (WF-020 holds the names of forms, and a
default is none), and no executor reads it for evaluation, so a document
whose `name` fields are removed gives every executor the same answer
(gate 13 holds ref, clingo and both Drools branches to that). It is there
for a consumer that shows a decision — an explanation that names the
default that decided — and that otherwise has only the index path of
ch. 7.9 to go by. The RegelSpraak frontend writes the name of the block
(`grammar/DIVERGENCES.md` §4): the base rule's on the base default, the
name after the colon of an `Uitzondering op X:` or `Afwijking van X:` block
on its default, a member's own rule name in an `Uitzonderingen ongeordend`
block, and an authored initialisation's on the initialisation default of
the opgave group (§7); a group-default a head makes and the two defaults
the opgave marker generates carry none. Worked example: in the golden
`jutterswet-meldingsketen` the default of the block `Uitzondering op
erkende meldingsdatum: herstelde melding` carries `"name": "herstelde
melding"`, and the base default the name of its rule, `erkende
meldingsdatum`. The Catala and F-logic frontends and the hand-written
anchors write no name.

**Default.statuteAnnotations has no semantics either**, as on a Rule: no
executor reads it for evaluation, so a document whose `statuteAnnotations`
on the defaults are removed gives every executor the same answer (gate 13
strips them in the copy that loses the names). Each entry is opaque, as on
a Rule (ch. 3.1): one string per `// Bron:` line, trimmed, in source order —
provenance, which no executor and no verifier reads or parses beyond the
form rules of WF-082. It is there for a consumer outside uchar that shows
where a deciding default comes from; what that consumer finds inside an
entry is its own reading, not uchar's. The RegelSpraak
frontend writes the `// Bron:` annotations of the block a default comes
from (`grammar/DIVERGENCES.md`, the annotations of lifted rules): the base
rule's on the base default, an `Uitzondering op X:` or `Afwijking van X:`
block's on its own default at any depth, an `Uitzonderingen ongeordend`
member's on its sibling group's default, and an authored initialisation's
on the initialisation default of the opgave group; a group-default a head
makes and the two defaults the opgave marker generates carry none, as no
annotation stands above them. The default then carries no `source`: the
frontend has no source text for a block as a whole. The defaults group a
base names keeps that base's annotations in its own `source` snippet, joined
with `"; "` (a single source record, unchanged by the field). Worked
example: in the golden `jutterswet-tweeledige-grondslag` the base default of
`penningrecht art 3` carries `"statuteAnnotations": ["Jutterswet art. 3",
"Jutterswet art. 3b; een vondst op een stormvloeddag telt tweemaal"]` — two
`// Bron:` lines, two entries, each kept whole whatever it contains — the
default of `schorsing art 4bis` carries `["Jutterswet art. 4bis"]`, and the
default of the unannotated `weigering art 4` omits the field. The Catala and F-logic frontends and the
hand-written anchors write no `statuteAnnotations`.

Semantics:
`RUNTIME-SEMANTICS.md` ch. 7.1 —
per subject the highest default that **decides** wins: on a
characteristic every justified default decides (a false decision derives
nothing and displaces); on an attribute a default decides when its
justification holds *and* its consequence evaluates gevuld — a leeg
consequence **falls through**, a defaults group without a decision **writes
nothing** (the input value is left standing), and default evaluation is
speculative (only the deciding default's channel deposits persist).

**Default.exceptions and ExceptionGroup** (`RUNTIME-SEMANTICS.md` ch. 7.9,
Phase 5c — the below-root branching form): a default's `exceptions` slot,
the record `{ unordered, members }` (v0 `excepties = { ongeordend, leden }`),
holds its **exception groups** — its `members` are `ExceptionGroup` nodes
(`defaults` — a non-empty default chain, descending, highest first — and
`source`; the node carried the name `ExceptionGroup` in v0 as well). An exception group inherits the root target
and carries **none of its own**, and it may nest again via
`Default.exceptions` — the defaults group is a rose tree. Two default shapes: a **group-default**
(`exceptions`, no own justification, decision or consequence — the exception groups
*are* its decision, carrying a multi-member base or exception group as a chain
position; a stray justification here is ill-formed, WF-075) and a
**form-a default** (own decision or consequence too — the
exception groups decide first, its own consequence the fall-back when they
decide nothing). A default reconciles its exception groups by the same rule
the declared-unordered root group uses (ch. 7.2), recursively;
**differing** exception groups propagate the conflict to the **root** — no
write, one path-keyed §9.5 fact `subject.target/<default index path>`
(ch. 7.9, vraag 5 A). The initialisation and input defaults are
**root-only** — a sub-default is never one. `Default.required` is untouched,
so every existing document validates unchanged.

**Inheritance, and the one obligation it puts on this chapter.** A defaults
group's `subject` ranges extensionally over the chain (ch. 2.2), and a
subtype inherits, per target, its supertype's whole stack: the composition
is `RUNTIME-SEMANTICS.md` ch. 7.10, and it produces exactly **one effective
stack per (concrete type, target)**. The obligation that follows is a
constraint on the verifier and is stated here because the natural repair is
the wrong one:

> **The single-writer rules — WF-026, WF-050 and WF-079 — are evaluated on
> the per-type EFFECTIVE stack, after inheritance resolution, never on the
> raw declaration set and never on a chain-closed target key.**

The hazard runs the counter-intuitive way, so it is measured rather than
argued. Keyed on the DECLARED object-type name, `Strandrecht.uitkering` and
`Wrakrecht.uitkering` are two keys, and a fence keyed that way does **not**
fire on a supertype/subtype pair at all. The tempting
repair — lift the target key to the chain-closed set, now that member
resolution walks the chain — would make these fences fire on precisely
the shape inheritance legitimises: a supertype base plus a subtype base on
one target would become "two writers" and the construct would be
unreachable.
In the effective stack that pair is ONE writer, because the subtype's own
base replaces the inherited one. Any chain-awareness added to these rules
must therefore ask *"do these two writers survive into one instance's
effective stack"*, never *"do these two targets close over the same
member"*. This is a new obligation, not a restatement, and it is witnessed
by `anchor/jutterswet-strandrechtketen`, whose two bases on one chain are
verifier-clean and replay through all three executors. WF-079's chain
clause (ch. 3.1) asks exactly that of two bare computing versions: outside
the gate of `RUNTIME-SEMANTICS.md` ch. 7.10 both write a subtype instance,
inside it the fold makes them one.

For two writers that carry a condition the question is asked at
evaluation, not statically: two conditional computing writers of one
target that survive into one instance's effective stack — on one type, or
on a type and its ancestor outside the gate of `RUNTIME-SEMANTICS.md`
ch. 7.10 — are well-formed, and where both conditions hold on that
instance with different values the session faults as a divergence
(`RUNTIME-SEMANTICS.md` ch. 5.7). Every executor names the two writers and
the target: `ref/` when its pass budget runs out, the ASP backend in a
`viol/2` witness per pair of writers that share a concrete type, the
Drools replay at its fire cap.

- **WF-050** A defaults group MUST carry at least one default and MUST
  target exactly one of a characteristic or an attribute, resolving on its
  subject. Default shapes MUST match the target kind (characteristic:
  `decision`, no `consequence`; attribute: `consequence`, no
  `decision`); the initialisation default MUST be the lowest, MUST carry
  no written justification, and MUST NOT sit on characteristic
  defaults. The target MUST have no other writer — no second defaults
  group, no rule, no allocation, and no object-creation initialisation on
  the same target: an unordered plain writer would silently defeat the
  group's decisions, and a creation-time write would void the
  initialisation default's input-emptiness justification (the ch. 7.1
  single-writer lift). **Through a type chain the allocation and creation
  clauses are read per concrete type**, as WF-026 is (ch. 3.2), and no stack
  absorbs an allocation or a creation (`RUNTIME-SEMANTICS.md` ch. 7.10 folds
  ordinary rules only), so the pairs named here are ill-formed, with no
  gate. An allocation writes the group's attribute through its shares on
  every instance of its `targetType` or through its remainder on every
  instance of its `sourceType`: where that type and the group's subject lie
  on one chain, the one an ancestor of the other, both write the lower
  type's instances. An object creation writes only the instance it creates,
  of exactly its `createdType` (creation is not polymorphic): where
  `createdType` lies strictly below the group's subject, that instance
  inherits the group's stack and has both writers, while a creation of an
  ancestor of the subject creates no instance the group runs on and stays
  clean (`corpus/golden/jutterswet-strandmelding`). The report sits at the
  group, as the flat clauses' do (witnesses
  `corpus/negative/wf050-chain-assignment-beside-group` for the shares,
  `corpus/negative/wf050-chain-remainder-beside-group` for the remainder and
  `corpus/negative/wf050-chain-creation-beside-group` for the creation).
  Multiple defaults groups on one target MUST all
  carry the `unordered` marking — priority is explicit (ordered defaults)
  or explicitly absent (the declared-unordered group), never implicit. The
  default-shape checks (content per target kind; a non-init default carries
  a justification) **recurse into sub-defaults** (ch. 7.9, an exception
  group inheriting the root target kind); a group-default (`exceptions`, no
  own content) is exempt from the content and justification requirements,
  and a contentless default — neither content nor exception group — is
  WF-075's concern.
- **WF-086** A defaults group MUST NOT target a **temporal attribute** —
  one carrying a declared granularity (a `timeline` facet on its Attribute
  node; v0 `attribuutGranulariteit`), whether anchor-only or on a named
  timeline — **nor a temporal characteristic** (a `timeline` facet on its
  Characteristic node; v0 `kenmerkGranulariteit`; the lens catch
  2026-08-29: it would otherwise evaluate silently atemporally). A defaults
  group is atemporal: it decides once, at one fixpoint
  (`RUNTIME-SEMANTICS.md` ch. 7), and carries no timeline coordinate at
  which to write.
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
negative case. `OPEN` = the artifact does not exist yet; an `OPEN` entry is
tracked debt under the spec discipline, and the Phase-1 exit requires every
row closed or its OPEN motivated in the row itself (three motivated OPENs
remain: WF-008 and WF-010 await a wire form or witness v0 cannot yet
produce; WF-019's negative has no data-shaped counterexample). "gate 3" = implemented in `spec/verifier/` and run by
`scripts/check-verifier.sh` over every golden and negative case; a
parenthesis names the clause the v0 wire format cannot carry (that clause
stays OPEN with the reason). Negatief cases live in
`corpus/negative/wf###-<slug>/`; the directory name declares the violated
rule and gate 3 requires the verifier to reject the case citing that rule
(incidental extra violations are allowed).

| ID | Statement (see ch. 2–4) | Verifier check | Golden | Negatief |
|---|---|---|---|---|
| WF-001 | top-level names unique per namespace (ci) | gate 3 | `jutterswet-vondsttelling` | `wf001-duplicate-parameter` + `wf001-duplicate-day-kind` and `wf001-duplicate-unit-system` (the two namespaces whose nodes L3 rewrote) |
| WF-002 | member names unique per kind within an object type (ci) | gate 3 | `jutterswet-vondsttelling` | `wf002-duplicate-characteristic` |
| WF-003 | every model-layer name reference resolves; since L2 (2026-09-24, B16) an unfindable domain is a declaration naming an undeclared domain — the unknown-type node left the datatype union | gate 3 (partial: enum domains, role fillers, named timelines; a reference v0 *dropped* is invisible in the dump) | `jutterswet-penningjaar` | `wf003-unresolved-domain` (rewritten at L2: an enumeration naming the undeclared domain `Bedragen`) |
| WF-004 | dimensioned attribute: non-empty, duplicate-free, resolving dimension list | gate 3 (the schema also refuses an empty list since M1: absent is the one spelling of a scalar attribute) | `jutterswet-vormenkabinet` (the dimensioned opbrengst) | `wf004-duplicate-dimension` + `wf004-empty-dimension-list` (a present, empty list: the negative zone is not schema-gated, so this witness holds the verifier's own arm in place) |
| WF-005 | timeline facet: count ≥ 1; named adoption total and consistent. **Since L3 (2026-09-24, B18):** a parameter carries the same facet, without a timeline name, and its count clause reports here — WF-018 defers the facet to this rule (ch. 2.10); on a parameter the count is 1 while multi-period grids are OPEN (`RUNTIME-SEMANTICS.md` ch. 3.12), since no executor evaluates it | gate 3 (attributes, characteristics and, since L3, parameters) | `jutterswet-penningjaar` + `jutterswet-penningvintage` (a parameter's facet) | `wf005-count-zero` + `wf005-parameter-facet-count-zero` and `wf005-parameter-facet-count-two` (a parameter's facet below and above 1, since L3) |
| WF-006 | enumeration domain: ≥ 1 value, values unique | gate 3 | `jutterswet-keurstaat` (the keurklasse domain) | `wf006-duplicate-enum-value` |
| WF-007 | **Since L3 (2026-09-24, B14):** a second arm — the presentation tag `presentation: "percentage"` stands only at a declaration position, never inside a type annotation (ch. 2.4); the precision forms are unchanged, on `whole`/`decimals`. number precision is exactly one of whole / fixed / unbounded — at every `Number` in the document: the declarations (model layer) and every `type` a value node carries of its own (since L2, 2026-09-24, a reference and a rule target carry none, B13; the rule-target arm of 2026-09-04 left the wire with the target's `type`). Measured and OPEN: the form check reaches none of a `ListType` element, `StructType` field, `SumType` payload, `FunctionDef` result/parameter or lambda parameter's `datatype`; two of those yield verifier-clean documents today (witnesses `jutterswet-postenheffing`, `jutterswet-waarderingsinvoer`), the other four are refused everywhere by neighbouring rules (WF-062/067/068/070/073/074/091); the enum walk recurses through lists, sums and structs, the struct walk only through lists, and a lambda parameter's `datatype` is reached by neither | gate 3 (model layer + the walk's type prologue) | `jutterswet-vondstnummer` (declarations) + `jutterswet-vondsttarief` (until L2 the rule-target arm crossing a percentage target clean; since L3 the presentation arm's positive side, the tag on its two declarations and on none of its annotations) | `wf007-whole-with-decimals`; `wf007-presentation-tag-by-annotation` (the presentation arm, since L3: a `Literal.type` carrying the tag); `wf007-doeltype-geheel-met-decimalen` (the rule-target arm) was removed with reason at L2 (`corpus/negative/README.md`, L2) |
| WF-008 | enumeration compatibility = same declaring domain (use-time; expression layer) | OPEN (no document carries two enum operands in one expression; the WF-036 guard already covers the equality site — fold WF-008 into the expression layer when such a witness lands) | OPEN | OPEN |
| WF-009 | unit conversions reduce: no cycles, no unknown targets, positive rational factors. **Since L3 (2026-09-24, B17 (i)):** a factor is the exact rational `{num, den}` of two integers ≥ 1, so positivity is its wire form (the schema and every loader) and lowest terms is WF-092 (c); a `canonical` resolves against its system's units by `abbreviation`; and a unit system declares each abbreviation once, under the ch. 2.1 identity (ch. 2.5: one `Unit` per abbreviation, which the v0 map key could not violate and the L3 list can) | gate 3 (termination, unknown targets and, since L3, the abbreviation declared twice; until L3 also positivity, while exactness was not representable in v0's float factor) | `jutterswet-berging` | `wf009-conversion-cycle` + `wf009-duplicate-abbreviation` (an abbreviation declared twice, since L3) |
| WF-010 | no restatement between non-convertible units (use-time; expression layer) | OPEN (v0 has no restatement node to anchor the check) | OPEN | OPEN |
| WF-011 | fact-type arity: reciprocal = 1 role, else ≥ 2 (>2 OPEN) | gate 3 | `jutterswet-vondsttelling` | `wf011-one-role` |
| WF-012 | role names unambiguous within a fact type (incl. plurals) | gate 3 | `jutterswet-vondsttelling` | `wf012-ambiguous-roles` |
| WF-013 | reciprocal role is to-one; other roles carry explicit cardinality | gate 3 (first clause; declared-vs-defaulted cardinality has no provenance in v0) | `anchor/jutterswet-maatschap` (a reciprocal fact type, folds replayed by gates 5–6) | `wf013-reciprocal-many` |
| WF-014 | dimension labels non-empty, duplicate-free, ordered | gate 3 | `jutterswet-vormenkabinet` (the seizoen labels) | `wf014-empty-labels` |
| WF-015 | one timeline declaration per normalized name; references resolve | gate 3 | `jutterswet-penningjaar` | `wf015-timeline-collision` |
| WF-016 | variable-start timeline is anchored (rule-layer check) | gate 3 (existence of the determination; the anchor-write typing is WF-032/WF-023) | `jutterswet-vormenkabinet` (the variable-start boekjaar with its start point) | `wf016-unanchored-variable-start` |
| WF-017 | day-kind spellings shadow no built-in unit and no other day-kind | gate 3, three clauses over a day kind's two spellings, `name` and `plural` (ch. 2.9, since L3, 2026-09-24): the DECLARED name against the built-ins, **arm (a)** the plural against the built-ins and **arm (b)** a plural against another day kind's name or plural; name against name is WF-001's. Until L3 the further spellings were keys of a separate registry, and arm (b) read the one form of the collision the registry showed, a declared name registered as a DIFFERENT day kind's unit spelling — the losing registration was absent (one value per key). "The built-ins" are the THIRTY spellings of the normative time-unit table (`RUNTIME-SEMANTICS.md` ch. 3.8), compared through that table itself since 2026-09-20, abbreviations and the second plurals `secondes`/`millisecondes` included; the producer moved with it (a declared name in the table is an ERROR in the RegelSpraak frontend) and so did the executors' lookup order. Until L3 a key that was not a declared name stayed free, because no plural field existed on the v0 wire (the (s) argument, ch. 4.6); since L3 the plural is a field and arm (b) reads it | `jutterswet-vormenkabinet` (two day kinds, each with its plural, colliding with no calendar spelling and not with each other); `jutterswet-vervaltermijn` from the other side (`wk`, `mnd` and `kw` as ordinary calendar units, which the widening must leave clean) | `wf017-day-kind-shadows-built-in` + `wf017-day-kind-name-shadows-calendar-abbreviation` (the name clause; the second is of the thirty-spelling widening) + `wf017-plural-shadows-calendar-unit`, `wf017-plural-shadows-calendar-abbreviation` and `wf017-plural-shadows-second-plural-form` (arm (a), since L3: a plural, an abbreviation and a second plural of the calendar table; the three registry-key witnesses of arm (a) and the widening were removed with reason at L3) + `wf017-day-kind-name-binds-foreign-day-kind` (arm (b), a plural that is another day kind's name — the frontend's own emission) + `wf017-plural-collides-with-foreign-plural` (arm (b), two plurals differing in case alone — the pin of the ch. 2.1 identity) |
| WF-018 | parameter datatype resolves (since L2, 2026-09-24, B16: an enumeration naming an undeclared domain; the unknown-type node left the datatype union); timeline facet well-formed (since L3, 2026-09-24, B18: the attribute's `TimelineFacet` without a timeline name, its count clause reported under WF-005; a parameter without a datatype cannot be written) | gate 3 (datatype clause; the facet clause under WF-005) | `jutterswet-penningjaar` | `wf018-parameter-unknown` (rewritten at L2: the parameter names the undeclared domain `Drempels`) |
| WF-019 | serialization preserves declaration order | `scripts/check-ir-schema.sh` (byte-identical round-trip) | `jutterswet-vondsttelling` (multi-element ordered lists round-trip) | OPEN (no data-shaped counterexample exists: the canonical re-emit is a serializer property, so a reordering fault lives in a consumer, never in admissible data — recorded) |
| WF-020 | form names unique across all NAMED forms (ci) — the ch. 3 versioned rule forms and the ch. 6.1 defaults groups, one namespace. *Widened 2026-09-12* to the defaults groups: the sentence already said "across all rule forms" and only the list the check read was short, so this is an existing number reaching a form it always described, not a new rule. Deliberately NOT split off under its own number — one property, one number, or one collision would cite WF-020 between two rules and something else between a rule and a group. The widening also narrowed a false second report: the bare-versus-versioned arm needed a versioned member to mix with, since without one the entries are an exact duplicate the first arm already names. **Since L4** (delta B19) a form writes its name once, so the rule compares FORMS, and the bare-versus-versioned arm left with the v0 suffix: two forms of one name are a duplicate whatever number of versions each carries | gate 3 (over the forms; until L4 via the v0 " #i" strip-and-group reading) | `jutterswet-vondstnummer`; and every defaults golden, whose group names differ from every rule name — measured 2026-09-12 over the four zones (golden 184, red 26, negative 386, parity 30): 626 documents, 1107 named forms of which 69 are defaults groups, and not one collision outside this rule's own witnesses | `wf020-crossform-collision` (between two ch. 3 forms) + `wf020-defaults-group-collides-with-line` (a group against a rule) + `wf020-defaults-group-duplicate` (two groups) — the three fall independently: a check reading only the ch. 3 list refuses the first and passes the other two, and one that adds the groups to only the cross arm or only the within arm passes one of those two |
| WF-021 | a named form's version validity windows do not overlap — a test performed on the RAW bound strings, which is calendar-correct only because WF-101 (ch. 4.1) demands the lexeme that makes lexicographic order calendar order | gate 3 | `anchor/jutterswet-jutheffing` (two dated versions of one rule name, and the replaying runtime witness: per-window absolute values through both executors, gates 5–6) | `wf021-version-overlap` + `wf021-creation-versions-overlap` (the `objectCreations` list, since 2026-09-25) |
| WF-022 | rule target resolves; characteristic value is the constant true (`{"boolean": true}`); **since L4** (delta B20) the member kind: a version with a `presentation` or with `initialisation` true writes an attribute, one with neither attributes a characteristic, and the target MUST name that kind of member; a target name that is both an attribute and a characteristic of the subject type decides no kind and is ill-formed | gate 3 | `corpus/golden/jutterswet-penningdrempel/` + `anchor/jutterswet-toelagegronden` (declares `Jutter.toelage` as an attribute and a characteristic, with no rule writing it: the golden of the name arm) | `wf022-target-unresolvable` (the target clause) + `wf022-characteristic-false` (the value clause: a flat attribution with `onwaar`, which forced through splits the executors — ref absent, the ASP backend a translation fault, the Drools exec branch present, the DRL-text branch absent) + `wf022-characteristic-target-with-presentation` and `wf022-attribute-target-without-presentation` (the member kind, one direction each) + `wf022-target-attribute-and-characteristic` (the name that decides no kind) |
| WF-023 | assignment typing (family, units, sign, keuzetype domain, precision gate) at every attribute write position: a rule target (a computing version or an initialisation — a `presentation` tag or `initialisation` true; a characteristic attribution, neither, is WF-022's), an object-creation attribute member, a defaults consequence (recursing into exception groups) | gate 3 (the shallow slice between the target's DECLARED datatype — until L2 its serialized type where the target has no declared attribute datatype: an unresolvable target, which WF-022 forbids, or a CHARACTERISTIC target, which WF-022 admits and whose value it already pins to the boolean constant true; since L2 the target carries no `type` of its own, B13 — and the value's static type (since L2 a reference's type is its declaration's): family (numeric kinds as one), unit compatibility with literal adoption, contradictory sign restrictions, and the keuzetype clause (same variant, same domain name, ci, through a `ListType` element; one-sided only within a family, where the family message would not already name the defect). The domain clause is **keuzetype-scoped** (ch. 2.4's family) and stays so: the struct-domain clause over these same three write positions is **WF-090**, its own rule (maintainer decision 2026-09-03), sharing this function's machinery under its own number. Until L2 the unknown sentinel deferred — WF-034 refused it at all three positions anyway; since L2 it is no datatype (B16). The precision gate needs expression-shape analysis and stays OPEN) | `jutterswet-vondsttelling` (numeric assignments) + `jutterswet-keurstaat` (the enum assignment) + `jutterswet-penninguitgifte` (anchor: object-creation members) + every prioritizedDefaults golden — and a measured zero over-rejection across every golden and red document and, read-only, the parity zone | `wf023-text-into-numeric` + `wf023-literal-sign-against-target` (the sign clause: a literal typed `negative` written into a `nonNegative` target, one report since 2026-09-25) + `wf023-write-enum-domain` + `wf023-write-sum-domain` + `wf023-write-ghost-domain` (a ghost domain at a write) + `wf023-schrijf-doeltype-leugen` (target type and value type shared the lie: the cross is against the DECLARED datatype) — removed with reason at L2 (translation = `wf023-write-enum-domain`: the target lost its own `type`, B13) + `wf023-write-list-domain` + `wf023-write-list-none-domain-target` + `wf023-write-list-none-domain-value` (the two one-sided arms within the list family) + `wf023-object-creation-domain` + `wf023-object-creation-kind` + `wf023-defaults-domain` + `wf023-defaults-kind` (positions 2 and 3, domain and family) + `wf023-defaults-exception-group-domain` (the recursion into exception groups, which the two root-consequence cases do not pin) + `wf023-initialisation-domain` (the third rule kind at position 1, which the `equation`/`calculation` cases do not pin) + since L2 the translations of five removed reference-type witnesses that fall on this rule: `wf023-unitless-read-in-kg-target` and `wf023-kg-read-in-unitless-target` (the unit clause, both one-sided directions), `wf023-list-read-in-sum-type-target`, `wf023-number-read-in-text-target` and `wf023-enumeration-read-in-number-target` (the family clause) |
| WF-024 | *Retired at L4 (2026-09-24), history:* allocation method is one of the three real methods (no COMPLEX) — since L4 the closed method enum of the schema and of every loader states it, so `COMPLEX` is a load error and no document is left for a rule to refuse | — (a load error since L4) | `jutterswet-berging` | `wf024-complex`, removed with reason at L4 (`corpus/negative/README.md`, L4) |
| WF-025 | allocation fields valid per method; ordering key orderable; share typing | gate 3 (field validity and orderability; share typing is WF-023) | `jutterswet-berging` | `wf025-max-without-ratio` |
| WF-026 | at most one allocation per written target; through a type chain read per concrete type — an allocation over a type and one over its ancestor both write the lower type's instances and pair as two on one type do, with no gate (an allocation is never folded, `RUNTIME-SEMANTICS.md` ch. 7.10), reported at the lower type's version | gate 3 | `jutterswet-berging` + `jutterswet-hoofdberging`, `jutterswet-bergingsdeling` and `jutterswet-bergersdeling` (one allocation over the subtype `Hoofdberger` writes the inherited `bergingsaandeel`: one writer on the chain, so the chain clause forms no pair there) | `wf026-two-allocations-one-target` + `wf026-chain-two-allocations` |
| WF-027 | object-creation members declared on the created type; characteristic init boolean (an attribute member's value typing is WF-023's second write position) | gate 3 | `jutterswet-penninguitgifte` (anchor: the mint, its member initialisations and both guards execute, gates 5–6) | `wf027-ghost-member` |
| WF-028 | fact creation resolvable: the created fact type is declared, `anchorRole`/`targetRole` are distinct roles of it filled by anchor and target, every hop binding of `path` resolves and joins, the path reaches the target — since L4 the declared filler of `targetRole`, the target type being no longer written — new ≠ final hop. Since L2 (2026-09-24, B7) the symbolic block is required by the schema and every loader, so the all-or-nothing and bean-field-only arms are enforced at load (history) | gate 3 | `jutterswet-gemeenteberging` (anchor: the chain walk executes and folds over the created relation pin absolute values, gates 5–6) | `wf028-create-along-final-hop` (the full chain walk); `wf028-half-symbolic-creation` and `wf028-bean-only-creation` were removed with reason at L2 (`corpus/negative/README.md`, L2) |
| WF-029 | consistency assertion has a single subject | gate 3 | `jutterswet-vondstnummer` | `wf029-no-subject` |
| WF-030 | uniqueness sources: each names a declared object type and its keys resolve; arity, datatype kind and units agree per position. Until L4 also: a union carries ≥ 2 sources — since L4 (delta B27) one construct carries both shapes, and a version with one source IS the single-type form (history) | gate 3 | `jutterswet-vondstnummer` + `anchor/jutterswet-kustregister` (the union — including the per-source sum over two same-type sources — and both scoped forms, executed by gates 5–6) | `wf030-arity-mismatch`; since the L4 fix round `wf030-source-type-not-declared` (a source naming no declared object type) and `wf030-key-none-attribute` (a key that is no declared attribute of it), each the declared plural where the declared name belongs; `wf030-single-source-union` was removed with reason at L4 (`corpus/negative/README.md`, L4) |
| WF-031 | the defined day kind is NAMED and declared; one definition per day kind; condition present and **pure** (no attribute/characteristic read — the stratum-0 framing, ch. 5.4); validity always (neither bound written). *Presence added 2026-09-12*, found while measuring WF-107 and closed here rather than there because the two names live in two namespaces: a null slipped past the resolution cross (which had nothing to compare) and left a definition bound to nothing, the same silently-dead shape the validity clause already refuses. Measured before it: verifier PASS, and on `jutterswet-schouwkalender`'s own case no answer moves — the defect is a lost definition, not a wrong number, which is why it needs a rule and not a gated case. The BLANK half is a second arm and it is tested BEFORE resolution on purpose: a blank name is not a declared day kind either, so under the natural clause order the resolution arm answers first with `undeclared day kind ' '` — true, and the wrong diagnosis, leaving the presence clause unreachable on every document that does not also declare a blank day kind. Cost: none — 24 day-kind definitions over the four zones (golden 6, negative 17, parity 1) and every one of them named except this rule's own witness. **POSITION ARM added 2026-09-19** (the day-kind semantics card): the purity clause gains a NAME-AWARE arm at the ContextValue node — the bound day is admitted, since it IS the day the predicate is applied to (the `BoundDay` node since L4; until L4 the context value `de dag`), and a `ContextValue` naming one of the two SESSION values (`evaluationDate`, `evaluationYear`; `Rekendatum`, `Rekenjaar` until L5) is refused. It is a semantics rule, not a spelling one, and it became answerable only when the duration forms started evaluating the condition per calendar day (`RUNTIME-SEMANTICS.md` ch. 3.8): a session-dependent predicate classifies the same calendar day differently in two runs of the same document, which is exactly what ch. 5.4's stratum-0 framing denies. Observability before it: none — measured 2026-09-10, one field of `jutterswet-schouwrooster` turned from `de dag` to `Rekendatum` left `verifier: PASS` and `ref: oracle: PASS (7 expectations)`, because the walk matched `Constante` name-blind. The arm is EXACT in both directions: it names the two session values only — matched through `norm`, where WF-098 matches BYTE for byte, so a lower-case `evaluationdate` in a day-kind condition reports BOTH rules, each for its own reason — so `wf098-unknown-context-value-name`, whose `de dagX` sits in that very position, keeps reporting WF-098 alone, and the EMPTY half (`naam: null`) was untouched — since L4 it cannot be written. Cost: zero, measured over all 673 documents of the four zones plus the parity zone — **31** `Constante` nodes inside a `resolvedDagsoorten` voorwaarde, **29** of them `de dag` and the other two the witnesses named here | gate 3 | `jutterswet-schouwkalender` (two pure day-kind definitions with an always validity, replayed by gates 5–6 over its ordinary rule) + `jutterswet-schouwrooster` + `jutterswet-bergingsklok` (the first day kind whose predicate is EVALUATED, by both duration forms) | `wf031-missing-condition` + `wf031-day-kind-window` (bounded validity window) + `wf031-day-kind-reads-characteristic` (the purity clause: a day-kind condition reading a characteristic) + `wf031-day-kind-definition-unnamed` (the presence clause) + `wf031-session-value-in-day-kind` (the position arm: the 2026-09-10 mutation turned into a witness) |
| WF-032 | start point: variable-start timeline, day-precision member, one per pair | gate 3 | `jutterswet-vormenkabinet` (the meetdatum anchor) | `wf032-fixed-start-anchor` |
| WF-033 | (dialect) rule-group members resolve; recursive group has a creation member (either kind); object creations guarded, relation creations guard-free — every version of an object-creation member (since 2026-09-25) | gate 3 | `anchor/jutterswet-wachtersketen` (the recursive chain executes to its fixpoint, gates 5–6; the runaway budget fault is red-pinned, gate 8) + `anchor/jutterswet-erflijn` (the guard-free relation-creation member: the transitive closure reaches its fixpoint in both executors) | `wf033-unguarded-recursive` (the guard clause) + `wf033-no-creation-member` (the containment clause) + `wf033-second-version-unguarded` (the second version of a two-version member unguarded; rejected only on WF-052 before the clause read every version) |
| WF-034 | **Since 2026-09-25:** a `VariableReference` resolves against no binding and is refused at the node — `variable reference '<name>' resolves against no binding: a rule-local variable is a Let, read by a BinderRef`; the wire serializes no variable binding site (ch. 4.1, the `VariableReference` row), and every executor refuses the node at load in that sentence; inside a closed body the closure clause (WF-067, WF-069) refuses it first. Until then the verifier let it pass. **Since L5:** the arms read their fields under the L5 names — the attribute reference's `subjectType` and `attribute` (v0 `objecttype`, `attribuut`), the parameter reference's `parameter`, the characteristic check's `characteristic` and `host` (v0 `kenmerk`, `eigenaar`; arms (q), (q1), (q2)), the dimension-cell read's `attribute`, on the nodes `AttributeReference`, `ParameterReference`, `CharacteristicCheck` and `DimensionCellRead` (v0 `AttribuutRef`, `ParameterRef`, `Kenmerk`, `DimensieCel`); the record below keeps the v0 names it was measured on. **Since L3 (2026-09-24):** arm (s) left the wire with the day-kind unit registry — a day kind carries its own spellings (ch. 2.9), so no registry value is left to resolve — and its witness `wf034-dagsoorteenheid-onbekende-dagsoort` was removed with reason (the `v "WF-034"` emission sites in `spec/verifier/lib/checks.ml` went from 37 to 35 with it); arm (r) reads the unit's `system` (v0 `stelsel`). **Since L2 (2026-09-24):** arm (d) (the unknown-type node in a payload), arm (m) (the bean fields of a fact creation), the rule-target and fold-operand arms of the unknown-type fence (the first wave's third clause, the second wave's (c), the seventh wave's (r)) left the wire with their fields (B16, B7, B13): the unknown-type node is no datatype and a reference carries no type; (o) and (t) merged into the v1 role arm over `RoleMembership {factType, role, counterpartType?, negated}` (B9) — the fact type is declared, `role` is one of its roles by declared name (the plural does not name it), the subject fills it (ch. 4.6 (o)); the count source's role name is WF-047's and the type-pair fallback is gone (B12, ch. 4.6 (l)); a far-side filler's narrowing to a strict subtype is carried by `RoleMembership.counterpartType`, which names a declared object type ⊑ the other role's declared filler — equality is well-formed and means what absence means, and a producer writes the field only where it narrows. What follows records the rule as it stood until L2, and where it spells a unit's `stelsel` or the day-kind unit registry `dagsoortPerEenheid`, it records the v0 spelling it was measured on (L3 above). every expression/condition reference resolves (no unknown-type sentinel, at the top level and anywhere in the PAYLOAD SUBTREE of a value node's type — the sum chain and the `ListType`/`StructType` containers under it alike; a reference NAMES both halves of what it resolves through — the object type and the member — a parameter reference names its parameter, a characteristic check names its characteristic, a dimension-cell read holds an attribute reference and nothing else, an allocation names its source and target attribute, spells no blank name in any of its six attribute-name fields, resolves every name it does spell against the object type of that name's OWN SIDE, and names both those object types; a fact creation's four v0 BEAN fields each name a declared role of the relation their symbolic sister names, or — for the intermediate step's `type` — the object type that hop reaches; an object creation names a declared `subject` and carries one at all; a near-side role check names an `anderTeFiller` its subject is related to and carries one at all; a FAR-side role check's `filler` and `backref` meet on one declared fact type — the back-reference naming the subject's own role, the filler the type at the other end — and it carries both at all (ch. 4.6 (t)); clause (p), that every key of an object type's eight per-member side tables names a declared member, was removed at M1 (2026-09-23) — the side tables left the wire and each facet is a field of its member's node (ch. 2.2, ch. 4.6 (p)); a characteristic check's explicit `eigenaar` names a declared object type; a unit's `stelsel` names a declared unit system; every VALUE of the day-kind unit registry `dagsoortPerEenheid` names a day kind `dayKinds` declares, the map's key side staying free by decision (ch. 4.6 (s)); and a rule target carries a type) | gate 3 (partial: sentinel types and parameter/day-kind/element-type/own-member references; variable bindings and navigation paths are not serialized in v0; excepted: a v0 instance-fold source argument names a role and carries the sentinel by construction — the row-1 wart, resolved against declared roles with the ch. 4.3 type-pair fallback for a name that resolves nowhere, and since wave 4 that exception is SCOPED to the wart, which the wire spells with exactly that sentinel: a fold source carrying a real datatype resolves as the ordinary attribute reference it is). **Widened 2026-09-04/09** (maintainer decision) in SEVEN waves, FIVE fix rounds (one per wave from the third on) and one single-arm card (2026-09-09, arm (s)), no new rule number and no documented deferral; the arm count is given a denominator anyone can re-take rather than a round number: **45** `v "WF-034"` emission sites in `spec/verifier/lib/checks.ml` (recounted 2026-09-09; 43 before arm (s), which spells its two value spellings at two sites), which is what a grep for that literal returns today; every one of them closes a **self-served exemption from the identity family** (WF-089/WF-090/WF-091), which defers on an unresolvable declaration and on a deferring type — so an emptied field or a sentinel switched the unit, precision and domain checks off on a document's own say-so while it stayed verifier-clean. *Wave 1* — an `AttributeReference` without an `objecttype` in a value position, the same in a FOLD SOURCE (its own arm: the aggregation branch replaces the generic walk and resolves only a named endpoint, so the empty field fell through both fences), and the sentinel on a rule TARGET's `type`. *Wave 2* — the same for the OTHER half of the pair, an `AttributeReference` without an `attribuut`, again in both positions (with the value-position arm alone 43 of the goldens' 751 emptied `attribuut` positions stayed clean, all fold-/aggregation arguments; with both, 0 of 751 — the same lesson the fold source taught in wave 1); a `ParameterRef` without a `parameter` (35 of 37 → 0); the UNSPELLED TWIN of the target sentinel, a target with no `type` at all (295 of 295 → 0), which wave 1 left out for want of a witness and which is the more dangerous half — it also switches off WF-007's target clause, and unlike every other shape here it replays CLEAN in `ref` and clingo while the Drools backend throws, so the verifier is the only place it can be caught before an engine crashes on well-formed input; and the sentinel one level DOWN, on a `SumType` constructor payload inside a value node's type (the fence matched the type's top level only; the walk now descends through payloads, WF-088's descent for the same reason). *Wave 3* — the four channels wave 2 measured and left open, six arms (the container half of them carrying three separately falsifiable branches: the list element, the record field, and the container→sum re-entry, which alone pins `wf034-sentinel-payload-onder-lijst`). A `Kenmerk` CHECK naming no `kenmerk` (89 of the goldens' 96 positions, in twelve position shapes over five top-level sections, stayed clean when emptied one at a time; 0 after), where one empty field silenced four readers at once — this rule's own declared-characteristic cross, WF-043's granularity cross, WF-053's navigated hop and the dependency edge. An allocation naming no `sourceAttribute` or no `targetAttribute` (`bronAttribuut`/`doelAttribuut` until L4) (11 of 11 each; 0 after) — its own function beside the expression walk, because an ALLOCATION is not an expression and its two endpoint attributes are the one reference pair `walk_expr` cannot reach, so WF-025's ordering-key check, WF-026's same-target comparison, WF-050's defaults overlap and both stratification edges keyed on them fell silent together. A `DimensieCel` whose `attribuut` is null **or holds a non-reference node** — two arms for two ways of missing one pattern, the cell arm resolving only through a complete inner `AttribuutRef`; measured on an existing artifact rather than argued, since emptying or overwriting `wf091-dimensiecel-eenheid`'s cell reference turns its pinned unit refusal off and the document CLEAN, and this is the one wave-3 channel with **no executor witness under today's executors** (a cell read is declined as a FORM by both OCaml executors, so no golden carrying one has or could have a case file today). The harm is pinned on FOUR existing artifacts, each measured under both spellings with the verifier of the day before this card and each turning `verifier: PASS` outright: `wf091-dimensiecel-eenheid`, `wf089-dimensiecel-enum`, `wf090-dimensiecel` and `wf040-unpinned-cell` — WF-040's label/dimension cross rides on the same match and goes with the identity family. The structural half of the argument, beyond the corpus: the reference frontend's IR types this field as an attribute reference, so its own emissions cannot reach the non-reference spelling; the clause is for the wire, which any conforming producer may write. And the CONTAINER half of wave 2's payload descent: the sentinel in a `ListType` element or `StructType` field under a constructor payload, and a sum reached back THROUGH such a container — the descent is now mutually recursive over the whole payload subtree, and the sum-chain messages it already pinned are unchanged. *Fix round, same day, same decision* — the FILLED half of wave 3's allocation arm: a present attribute name on any of the allocation's six name fields (`sourceAttribute`, `targetAttribute`, `ratioAttribute`, `maximumAttribute`, `remainderAttribute`, `orderingAttribute`) is not blank, emptiness decided by the ch. 2.1 name identity, while a null on the four facet fields keeps its meaning of a facet switched off. `""` satisfied every consumer's `Some a` and named nothing, so it walked through the null arm into the same silence: 11 of 11, 11 of 11, 7 of 7, 4 of 4, 9 of 9 and 3 of 3 golden positions stayed verifier-clean when blanked one at a time, and none does now. The executor consequence is the reason this needed a clause rather than a note: loud on the two endpoints and on `remainderAttribute`, but on `ratioAttribute` and `orderingAttribute` BOTH executors play on and agree on a wrong answer, and on `maximumAttribute` `ref` plays on (600 for a pinned 300) while the ASP backend refuses — ref and clingo diverging on a verifier-clean document. Wave 3's over-rejection check is direct and total: the verifier's verdict over all **462** documents of golden + red + negative + parity that PREDATE this card (153 + 22 + 257 + 30) is unchanged, with four pre-existing allocation negatiefs gaining a truthful incidental WF-034 line while keeping their verdict and the rule their directory declares; the eleven new negatiefs the card and its fix round add — negative zone 257 → 268, four zones 462 → 473 — are the only additions, and a byte-level comparison of the two verifiers' reports over all 473 documents changes exactly those 15 reports and nothing else. All eight wave-1/2 shapes are carried by **zero** documents outside their own negative witnesses (152 golden + 22 red + 30 parity, plus every negative that predates the wave, measured before and after each), so over-rejection is impossible by measurement and nothing repinned — and the wave-2 over-rejection check is direct: the verifier's report over all 447 documents of golden + red + negative + parity is byte-identical before and after but for the five new negatiefs. Nor does anything this repo's own frontends emit carry them (28 rgs goldens, 12 catala goldens, 45 parity fragments, all measured clean) — the shapes are open to any other conforming emitter, which is the audience of an engine-neutral IR. *Wave 4, same day, same decision* — the NAMING class the first three waves kept measuring and leaving behind: not a field left empty but a field filled with a name that resolves against nothing, three arms. A filled attribute name on ANY of the allocation's six name fields resolves against the declaration scope of its OWN SIDE — `sourceAttribute` and `remainderAttribute` against `sourceType` (the pot, and the remainder written back on the source), `targetAttribute`, `ratioAttribute`, `maximumAttribute` and `orderingAttribute` against `targetType` (the share, and the three facets read per recipient); an undeclared TYPE is another rule's business (WF-047 on the bound form, WF-048 on the bindingless one, both RULES measured loud — the executors are not: an undeclared `sourceType` leaves `ref` silent with every share *leeg* while clingo refuses, ch. 4.6 (j)), so the arm resolves only where there is a scope to resolve in. Measured with `bestaatniet` one field at a time, named to their counts in the order the clause lists the fields (source side first): `sourceAttribute` 11 of 11 and `remainderAttribute` 9 of 9 against `sourceType`; `targetAttribute` 11 of 11, `ratioAttribute` 7 of 7, `maximumAttribute` 4 of 4 and `orderingAttribute` 3 of 3 against `targetType`. Every one of those golden positions stayed verifier-clean, none does now — and the observability rows are the fix round's exactly, one spelling on: loud on the two endpoints and `remainderAttribute`, BOTH executors silent and agreed on `ratioAttribute` (the shares stay underived where the anchor pins 60 and 40) and on `orderingAttribute` (300 apiece where 1200/0/0/0 is pinned), and `ref` silent while clingo refuses on `maximumAttribute` (600 for a pinned 300). An allocation NAMES both its object types: 11 of 11 each stayed clean with `sourceType` or `targetType` emptied while both executors fault by the field's own name, and a null there also silences WF-025's ordering-key datatype, WF-050's defaults overlap, both stratification edges and this wave's own resolution scope. And the ch. 4.3 fold-source exception returns to its SCOPING: the row-1 wart is the fold-source argument carrying the unknown sentinel on its own type — that is how v0 spells it, a role name having no datatype to give, and it is the discriminator both OCaml executors already key their role-naming branch on (the Drools backend keys the same reading on `kind = AANTAL` plus its own role derivation and plays a real-datatype source both of them refuse — measured, ch. 4.3) — so a fold source carrying a real datatype resolves as an ordinary attribute reference, through neither the role-name route nor the type-pair fallback. Both branches measured live and separately falsifiable: on `anchor/jutterswet-maatschap`, whose reciprocal self-relation derives one connecting role for ANY name, a SOM source repointed at `bestaatniet` stayed clean while `ref` silently summed 0 where 12 and 30 are pinned and clingo refused; on `jutterswet-vondsttelling`, where the pair `Vondst`/`Vondst` derives NO connecting role and only the role route can admit anything, the same source repointed at the declared plural `vondsten` stayed clean while `ref` summed 0 where 3.75 kg is pinned and clingo refused. The regression boundary was counted before and after: of the corpus's 89 named fold sources — *named* meaning the argument spells both its `attribuut` and its `objecttype`, the pair this arm resolves through; count in the one source that spells an attribute and no object type (a wave-1 shape, refused there) and the totals read 90 and 58 — 32 carry the wart (24 resolving through a declared role name, 5 through the type pair, 3 already ill-formed under this rule's older arms) and 57 do not, of which 54 already resolve as a declared attribute and one names a characteristic (WF-080's arm, unchanged) — so the narrowing rejects nothing that was standing, and the counting folds that lean on the fallback (`jutterswet-kaveltelling`'s synthesized plural, and the declared-plural route of `jutterswet-aanmeldertelling` and `jutterswet-vondsttelling`) keep both routes and replay unchanged in both executors. Wave 4's over-rejection check is direct and total, the same instrument: over all **486** documents of golden + red + negative + parity (153 + 22 + 281 + 30) exactly 15 reports change and 10 verdicts — the ten being the wave's own new negatiefs, seven written with the clauses and three more in the fix round that gave the `remainderAttribute` and `targetAttribute` sub-arms and the `targetType` arm a witness each; no golden and no red document moves at all, and five documents keep their verdict while gaining truthful incidental lines (the four pre-existing type-less allocation negatiefs, one or two lines each, and the parity casus, whose own `targetAttribute` names an attribute its target type does not declare — six incidental lines there, one per allocation). Negative zone 271 → 281, four zones 476 → 486. *Wave 5, same decision* — the same naming class taken OFF the expression walk, to three positions the walk never reaches, ch. 4.6 (m)–(o). The four v0 BEAN-field names of a fact creation (`zetVeld`, `joinVeld`, `tussenstappen[].veld`, `tussenstappen[].type`): a binary relation lives in the v0 bean model as one scalar reference field, so each of the three name fields resolves against a declared role of the relation its symbolic sister names — `feittype`, `joinRol`'s, that hop's `rolBinding`'s — while the step's `type` resolves against the hop itself, the filler of the role that binding binds. The wave admitted BOTH roles and assumed neither, reasoning that the wire does not carry which side stores the reference; its fix round replaced that with the derivation (below), and corrected the corpus reading offered in support — read per creation it is two and two, not three and one, since `anchor/jutterswet-erflijn` carries two of the four golden creations and both spell the target's role (`joinVeld` being the bound role three times and the opposite role once still holds). 14 of 14 golden positions stayed verifier-clean when pointed at a name no declaration carries, 0 after — and this is the family's sharpest observability row: BOTH OCaml executors are blind by construction (they read the symbolic fields and never touch the bean fields, so all 14 replay to their pinned expectations and the differential square has nothing to catch), while Drools reads them reflectively and crashes on all four on `jutterswet-postwacht`, a golden it plays. An object creation's `subject` against the object-type table: WF-027 crosses only `createdType`, and the subject was reached solely by rules reading THROUGH it (WF-053 on a navigating member value, WF-047 on an explicit back-link binding) — measured, 10 of 11 golden creations were already refused by exactly those two and ONE stayed clean, the only creation without `initialisations`, where `ref` and clingo agree on a wrong answer (0 dossiers for a pinned 1) on an ANCHOR golden. And a near-side role check's `anderTeFiller` against the ch. 4.3 role-filler candidates, the executors' own derivation: the written `rol` is presentation, so that one field is the whole reference; 4 of 4 golden positions clean before, 0 after, with `ref` and clingo both refusing loudly and Drools taking a third road (it drops the rule as a transpiler skip, so the document neither faults nor answers). Wave 5's over-rejection check is direct and total: over all **493** documents of golden + red + negative + parity that predate the card (154 + 22 + 287 + 30) not one verdict and not one report moves — a stricter result than waves 3 and 4, which each moved incidental lines — and the seven new negatiefs (six here, one under WF-028) are the only additions. Negative zone 287 → 294, four zones 493 → 500. *Wave 5's fix round, 2026-09-05, same decision* — three arms on (m), where a lens showed the wave had conceded a decision the model actually makes. A binary relation lives in the v0 bean model as one scalar reference field, on the object type filling the TO-MANY role and named after the TO-ONE role (§13.3.9, the storage direction the Drools backend derives symbolically and cites in its own refusal reasons), so where a relation carries exactly one to-one role the bean field MUST name it; at equal cardinality the model derives no side and both names stay admissible, which is where the arm stops — `anchor/jutterswet-erflijn` and `anchor/jutterswet-gemeenteberging` both carry both-to-many relations and a stricter rule would make two anchor goldens ill-formed. The name MUST also be the DECLARED SPELLING byte for byte, since M1 the one place the ch. 2.1 case-insensitive matching is not enough — until M1 there was a second, the side-table keys of (p), on the different ground that the key WAS a member's carried identity; (p) left the wire with the side tables — because a bean-model executor turns it into a case-sensitive Java property (measured: an internal capital throws `geen getter voor 'seinVak'`, a first-letter change plays, since the identifier mapping lower-cases the initial — so the arm is knowingly stricter than the executor on that one shape, and `tussenstappen[].type` deliberately gets NO byte arm, its consumer being case-insensitive by documented contract). And all four fields MUST be PRESENT, as ONE list-driven arm rather than four: with one witness, three of four sub-arms move no document at all (0 of 503 measured), and a clause with no counterexample is not a clause. Observability, measured on `jutterswet-postwacht`: the sister role and the misspelling each leave verifier, `ref` and clingo clean while Drools throws a missing getter; of the four absences, two crash it with exactly the exceptions the filled-name witnesses pin and two make it drop the creation as a loud transpiler skip. Zero over-rejection over the 500 documents that predate the fix round; three new negatiefs. Negative zone 294 → 297, four zones 500 → 503. *Wave 6, 2026-09-05, same decision* — the REST of the naming class off the walk, the field-by-field remainder waves 5 and its fix round measured and left in the open list; twelve arms, ch. 4.6 (n)-(r). The EIGHT per-member SIDE-TABLE keys of an object type (`attribuutMeervoud`, `attribuutDimensies`, `attribuutGranulariteit`, `attribuutTijdlijnAantal`, `attribuutTijdlijnNaam`, `kenmerkSoorten`, `kenmerkGranulariteit`, `kenmerkTijdlijnAantal`) each name a declared member of that type — eight arms because they are eight independent readers, and a mutant removing one frees exactly one document. The harm here is not a missing value but a SILENT SEMANTIC CHANGE: an orphan key hangs a granularity, plural or dimension on a member that does not exist and leaves the intended member at its default, and every reader looks the facet up by the declared name, finds nothing and takes that default. Measured with a spurious key added to each table on each of the 205 golden object types in turn: 205 of 205 verifier-clean before and 0 after per table (`attribuutDimensies` 2 of 2, since its value needs a declared dimension). The complementary instrument, renaming a LIVE golden key: `attribuutMeervoud` 6 of 6, `attribuutGranulariteit` 20 of 33, `attribuutTijdlijnAantal` 1 of 1, `kenmerkSoorten` 124 of 124 clean before and 0 after — and, recorded rather than smoothed over, three tables where the rename is refused already (`attribuutDimensies` 0 of 2, `kenmerkGranulariteit` 0 of 9, `attribuutTijdlijnNaam` 0 of 2) and one with no live golden key at all (`kenmerkTijdlijnAantal`). Executor observability splits the eight in two: `attribuutMeervoud` (8 measurements), `attribuutTijdlijnNaam` (1) and `kenmerkSoorten` (438) are byte-identical before and after in BOTH OCaml executors — nothing sees them, and `kenmerkSoorten` has no reader in any of the four consumers at all, so its arm is for the wire on the same argument the wave-3 cell arm rests on; while `attribuutGranulariteit` (99 measurements: 91 loud in both, 6 silent in both, 2 DIVERGENT — `ref` replaying `synth2-prorata-breed` case4 to its pinned expectations while the ASP backend refuses the pro-rata base) and `kenmerkGranulariteit` (27: 26 loud, 1 DIVERGENT on an ANCHOR golden — `anchor/jutterswet-dubbelwacht` case3, `ref` faulting on `gedurende` while the ASP backend replays it clean — with the caveat that the carrying document is ill-formed independently of this clause and already was before the card, since WF-055 refuses a `gedurende` over a non-temporal characteristic, so the divergence is a second-line finding behind an already-closed door and not this arm's justification; the asymmetry itself is its own card) are read by all three executors. A characteristic check's explicit `eigenaar` names a declared object type: it is the node key of the dependency edge the check contributes, and the bean-model executor's loader resolves it through a null-yielding by-name lookup, so an undeclared owner becomes NO owner and a rule that backend DECLINES under a real owner (measured on `jutterswet-wachtploeg`: a declared `Wachtpost` makes it drop the rule as a transpiler skip) is carried and answered under a nonsense one — with both OCaml executors blind by construction, as at the bean fields; 96 of 96 golden check positions clean before, 0 after, and no golden carries the form (2 of the four zones' 190 checks do, both negative, both resolving). A unit's `stelsel` names a declared `Eenheidstelsel`: the ASP backend's literal-restating pass gates on it and the identity family reads it as part of a unit's structural identity, and the misspelling is a measured ref-vs-clingo DIVERGENCE on `jutterswet-sleepbaan` (`ref` PASS and Drools PLAYED against `non-identical units under the write to "sleeplengte" are not translated`); 21 of 21 (golden, system) pairs clean before with every use renamed consistently, 0 after — the consistent instrument being required, since a single renamed occurrence breaks unit identity and WF-091 reports that instead — and of the four zones' **2377** unit records (*recounted 2026-09-06: the wave reported 2442 from an instrument that counted the 73 denominator units of composite rates twice, once in their own right and once under the enclosing record*) the **2244** that carry a `stelsel` all resolve, the other 133 carrying null (the percentage unit and the bare calendar units) and passed over by construction. And the two EMPTY halves wave 5 measured and left witnessless, each its own arm because a name resolving against nothing and an absent name are different questions observed differently: an object creation carries a `subject` at all (11 of 11 golden creations clean when emptied, 0 after; both executors LOUD, where the filled half made them agree on a wrong answer) and a near-side role check carries an `anderTeFiller` at all (4 of 4, 0 after; both executors loud). Wave 6's over-rejection check is the same instrument and the same result: over all 510 documents of golden + red + negative + parity that predate it (155 + 22 + 303 + 30) not one verdict moves and exactly ONE report does — `wf031-day-kind-reads-role-state` gains a truthful incidental line from the `anderTeFiller` arm and keeps the WF-031 verdict its directory declares, that document being a day-kind condition WF-031 refuses outright, which is why no subject was there to derive a filler from. Negative zone 303 → 315, four zones 510 → 522. *Fix round, 2026-09-06, same decision* — the side-table identity of (p) tightened from case-insensitive to BYTE-EXACT: the key is not a matching site but the v0 denormalisation of a member's identity, and ch. 2.1 carries identities verbatim, so the key must be the member's declared spelling. The ground is measured, not stylistic: the consumers disagree about how they look these facets up — the verifier, `ref` and the ASP backend case-insensitively, the bean-model executor NOT (108 raw case-sensitive map lookups across four of the eight tables, against five case-insensitive ones through its own name helper) — so under the ci rule a key differing in capitalisation alone is no orphan and the document stays clean in three consumers while the fourth drops the golden out of the differential unseen (measured on `corpus/golden/synth2-clip-over-prorata`, `kenmerkGranulariteit` key `borgplichtig` capitalised: verifier, `ref` and clingo all PASS while Drools goes PLAYED → auto-excluded, taking gate 12 from played 109 to 108 with the gate still GREEN, its only floor being a non-empty played set — so the wire must catch what no gate catches). Cost nil: of the 943 keys the four zones held after the wave, ZERO differ from their declaration in byte while resolving case-insensitively, and zero of 924 before it; the sweep over all 523 documents moves exactly the one new witness, `wf034-zijtabel-kapitalisatiewees`. Negative zone 315 → 316, four zones 522 → 523. *Wave 7, 2026-09-06, same decision* — the unknown-type fence rather than the naming class, ONE arm (r) on the branch that replaces the expression walk: the unknown type is refused on every fold operand that is not a fold SOURCE, in BOTH its spellings — the `Onbekend` sentinel and a `type` absent altogether — because the walk's own prologue has always refused the pair together and this branch had repeated neither. The exemption is the ch. 4.3 wart and exactly it, keyed on the pair WF-093 keys on (arity one AND a folding kind), so the two rules are disjoint and jointly exhaustive over the branch. Measured one mutant per half: the "single AND `AANTAL`" exemption doubles a citation on exactly the four `wf093-wratvouw-*` negatiefs; reverting the KIND half frees exactly the two `wf034-sentinel-builtin-*` witnesses; inverting the arity guard newly rejects 12 documents (10 goldens, 5 anchors, 2 red); deleting the unspelled-twin arm frees exactly `wf034-sentinel-null-vouwoperand`; deleting the clause outright frees all six operand witnesses and nothing else. Executor observability spans four dead ends over one position — PLAYED with an answer, an `IllegalStateException` on a getter of the role name, a `NullPointerException` on the missing unit of a null-typed numeric operand, and a transpiler decline — and, on the unspelled twin, `ref` on the ANSWERING side, replaying a date golden to all five of its pinned expectations off the declaration while the ASP backend refuses. Cost nil in both halves: zero of the four zones' null `type` positions sit at a fold argument (all three are in negatiefs already rejected for exactly that), and the four zones carried no non-aggregation `Functie` with an `elementObjecttype` at all. Zone growth, stated at the state this wave LANDS on (rebased onto the datum-tijd card, so main's own six documents are already in the base): negative zone 330 → 337, four zones 539 → 546. Over that sweep exactly seven reports and three verdicts move: the three fix-round witnesses' verdicts (the semantic half, measured alone against the wave as first landed, moves those three and nothing else), plus four report-only restatements of the reworded message — three earlier witnesses and the parity casus, whose violation count is unchanged. The wave's own four witnesses took the negative zone 325 → 329 and the four zones 533 → 537 before the rebase. *Re-measured 2026-09-06, and the fold-argument flank is CLOSED by wave 7 (r):* injecting the sentinel one position at a time at all **2701** datatype-valued golden positions of today's corpus leaves **ZERO** survivors — before wave 7 there were **6**, every one of them a LISTED fold operand (six positions over three listed value folds in two goldens: `jutterswet-peildatum` twice and `jutterswet-sluitsom` once), which is the whole of what wave 2's instrument then reported as "fold-/aggregation argument types". (The denominator is today's corpus state and a wave-2 count of 2758 over a smaller corpus is not comparable; the numerator is what carries the claim.) What remains is not a survivor but the documented exemption: the row-1 wart itself, the 15 no-op positions that carry the sentinel by construction, gedoogd and documented — narrowed by wave 4 to the position ch. 4.3 scopes it to, but the sentinel it carries there is still unfenced, and so is the name written beside it. *That residual width was counted over the four zones:* of the 32 wart sources, 26 sit on a type pair that derives exactly one connecting role, so the fallback admits ANY name there; on 18 of those 26 WF-047 crosses the written name against the bound role of an explicit binding, which leaves **eight** positions in which an arbitrary name simply stands — and only **two** of them are golden, `jutterswet-aanmeldertelling` and `jutterswet-kaveltelling`, the corpus's two bindingless counting folds. That is the whole of what the wart still buys a document, and it is why the narrowing is a scoping and not a repeal. (The two name-resolution gaps wave 3 recorded here as open — a name no declaration carries on any of the allocation's six attribute-name fields, and an empty `sourceType`/`targetType` — and the fold-source fallback's own width are CLOSED by wave 4, ch. 4.6 (j)–(l).) Still open, and another rule's walk to make: WF-007's precision-form check, which has the identical blind spot at the payload positions wave 2 closed and in the containers wave 3 closed | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondsttelling` (role source) + `jutterswet-schouwrooster` (arm (s): four entries resolving, one of its keys load-bearing as a declared `eenheid` the executed arithmetic runs with) + `jutterswet-vormenkabinet` (arm (s)'s IDENTITY half: two entries that resolve only under `norm`, a capitalised value and a padded one, so a byte-exact comparison takes gate 3 down — verifier-only, the golden carries no case file) + `jutterswet-kaveltelling` (the type-pair fallback: an undeclared synthesized plural over a unique pair, both executors — and, since wave 4, the corpus's one golden fallback user, so it is the standing regression witness for the narrowed scoping) + `jutterswet-postbezetting` (arm (t): the far-side role check's first corpus carrier anywhere, hand-written as the witness the clause had to have before it could be written) — and the non-over-rejection argument splits in two, because the waves do. For the EIGHTEEN arms of waves 1-4 and their fix round, every golden and red document is a non-over-rejection witness at once, since all eighteen shapes carry zero occurrences in those two zones (the only occurrences anywhere are the wave-3 and wave-4 arms' four pre-existing allocation negatiefs and the parity casus, which gain incidental lines and keep their verdict). For the NINE arms of wave 5 and its fix round, and for the TWELVE of wave 6, that is not the argument, and must not be borrowed: those arms cross positions the corpus does carry — 14 bean-field names plus 4 hop types, 14 object creations and 5 near-side role checks across golden and red (11 + 3, 11 + 3 and 4 + 1); and, for wave 6, 924 side-table keys over the four zones (177 of them golden), 190 characteristic checks and 2244 unit records carrying a `stelsel` — and every one of them resolves. There the evidence is the sweep, not absence: 493 documents byte-identical across wave 5, 500 across its fix round, not one verdict and not one report moved; 510 across wave 6, not one verdict moved and exactly ONE report did, the incidental line `wf031-day-kind-reads-role-state` gains from the `anderTeFiller` empty half | `wf034-dangling-parameter` + `wf034-ambigu-typenpaar` (unresolved name, two connecting fact types) + `wf034-zelftype-typenpaar` (unresolved name, one self-typed two-role fact type — both roles are candidates, counted in roles not fact types) + `wf034-untyped-literal` (the unspelled twin of the sentinel, on a value node) + `wf034-attribute-reference-without-object-type` (the missing endpoint, carrying the measured answer-changing `t`-for-`kg` lie the empty field hid: `ref` faults, clingo refuses, Drools played it) + `wf034-fold-source-without-object-type` (the same field emptied in a fold source, the same lie, and the worse measurement: `ref` PASS, clingo PASS, Drools PLAYED — all three answer off it) + `wf034-onbekend-regeldoel` (the sentinel on `/resolvedRegels/1/doel/type`; its NOTE names the `SumType` constructor payload as a second surviving position, which is true of the wave it was written in — since wave 2 the payload half is closed and only the row-1 fold-source wart remains) + `wf034-attribute-reference-without-attribute` + `wf034-fold-source-without-attribute` (the other half of the pair, once per path, each carrying the same `t`-for-`kg` lie: `ref` and clingo fault, Drools skips the rule or throws) + `wf034-parameter-reference-without-parameter` (same lie on a parameter operand; `ref` and clingo fault, Drools throws) + `wf034-regeldoel-zonder-type` (the unspelled twin: `ref` PASS, clingo PASS, Drools throws — verifier-clean input crashing an executor) + `wf034-sentinel-sumtype-payload` (the sentinel a level down: `ref` replays it, clingo refuses to translate it — one document, two executors disagreeing about whether it exists) — and, for wave 3, eight more, one per form and per spelling: `wf034-characteristic-check-without-characteristic` (`ref` "missing KenmerkCheck.kenmerk", clingo "missing kenmerk") + `wf034-allocation-without-source-attribute` + `wf034-allocation-without-target-attribute` (the allocation pair, each half its own document, `ref` and clingo faulting on the field's own name) + `wf034-dimension-cell-without-attribute` + `wf034-dimension-cell-none-reference` (the two spellings of the cell escape, both VERIFIER-ONLY — a cell read is declined as a form by both executors, so the harm is pinned by `wf091-dimensiecel-eenheid` turning CLEAN when its cell reference is removed, not by a replay) + `wf034-sentinel-lijstelement-payload` + `wf034-sentinel-structveld-payload` + `wf034-sentinel-payload-onder-lijst` (the container half: the list branch, the record branch, and the container→sum direction of the recursion, each with `ref` replaying to its pinned expectations while clingo refuses to translate) — and, for the fix round, three more, one per measured executor shape of the blank name: `wf034-allocation-empty-source-attribute` (the loud shape: both executors fault) + `wf034-allocation-empty-maximum-attribute` (the DIVERGENT shape: `ref` plays on and returns 600 for a pinned 300 while clingo refuses to translate) + `wf034-allocation-empty-ordering-attribute` (the SILENT shape: both executors play on and agree on the same wrong distribution — 300 apiece where 1200/0/0/0 is pinned — which is the one place only the anchor pinning stands between the corpus and a defect) — and, for wave 4 and its fix round, ten more, one per measured executor shape of the RESOLVING name and one per sub-arm that had none: `wf034-allocation-unknown-ratio-attribute` (the silent-and-agreed shape on an ANCHOR golden: neither executor faults, both leave the shares underived where 60 and 40 are pinned) + `wf034-allocation-unknown-ordering-attribute` (the second silent-and-agreed shape, and the sharper one: both compute a whole distribution and it is the same wrong one) + `wf034-allocation-unknown-maximum-attribute` (the DIVERGENT shape: `ref` plays on uncapped at 600 for a pinned 300 while clingo refuses to translate) + `wf034-allocation-unknown-source-attribute` (the loud shape, present so the two silent rows are not mistaken for the whole class) + `wf034-allocation-unknown-target-attribute` (the second loud shape, on the field the allocation WRITES — the one the parity casus carries six times over) + `wf034-allocation-unknown-remainder-attribute` (the third, and the sub-arm that left no trace at all: before this witness, deleting its one line changed no document in any of the four zones) + `wf034-allocation-without-source-type` + `wf034-allocation-without-target-type` (the missing object type, both executors faulting by the field's own name — one witness per half, since each type is its own match arm and deleting either alone moves only its own document) + `wf034-fold-source-unknown-attribute` (the type-pair branch of the scoping, on `anchor/jutterswet-maatschap` where a reciprocal self-relation derives one connecting role for any name at all: `ref` silently sums 0 where 12 and 30 are pinned, clingo refuses) + `wf034-foldbron-rolnaam-waardevouw` (the role-name branch, deliberately placed on `jutterswet-vondsttelling` where the type pair derives NO connecting role, so only the role route could ever admit it — which is what makes the two branches separately falsifiable: each mutant frees exactly one of the two documents) — and, for wave 5, six more, one per arm: `wf034-feitcreatie-onbekend-zetveld` + `wf034-feitcreatie-onbekend-joinveld` + `wf034-feitcreatie-onbekend-tussenstapveld` + `wf034-feitcreatie-onbekend-tussenstaptype` (the four bean fields, all four on `jutterswet-postwacht` — the one of the three fact-creation goldens Drools actually plays, so each carries a real crash and not a hypothetical one: a missing getter via a consequence exception, two constraint-evaluation exceptions, and a null dereference of the object-type lookup, while `ref` and clingo replay all four to their pinned expectations) + `wf034-object-creation-unknown-subject` (the toekenningsloze creation on an anchor golden: no fault anywhere, both executors agreeing on 0 where 1 is pinned) + `wf034-rolcheck-onbekend-andertefiller` (both OCaml executors refusing by the same sentence while Drools drops the rule) — and, for wave 5's fix round, three more on the same golden: `wf034-feitcreatie-zusterrol` (a DECLARED role of the created relation, and still a getter that exists on neither bean) + `wf034-feitcreatie-hoofdletterveld` (the byte-exact arm; an INTERNAL capital, because a first-letter one is harmless and would have made the crash claim false) + `wf034-feitcreatie-loos-tussenstapveld` (the presence arm, on the absence that crashes rather than one of the two that skip) — and, for wave 6, twelve more, one per arm: `wf034-wees-attribuutmeervoud` + `wf034-wees-attribuutdimensies` + `wf034-wees-attribuutgranulariteit` + `wf034-wees-attribuuttijdlijnaantal` + `wf034-wees-attribuuttijdlijnnaam` + `wf034-wees-kenmerksoorten` + `wf034-wees-kenmerkgranulariteit` + `wf034-wees-kenmerktijdlijnaantal` (the eight side tables, seven of them one added key on `jutterswet-vondstverjaring` and the dimension one on `jutterswet-vormenkabinet`, whose declared dimension the value needs; the two granularity witnesses carry the divergences, the other six the silence) — these eight, and the fix round's `wf034-zijtabel-kapitalisatiewees`, were removed with clause (p) at M1 (stamp verwijderd-met-reden, `corpus/negative/README.md`) + `wf034-characteristic-check-unknown-host` (on an ANCHOR golden, with both OCaml executors blind and the bean-model executor silently re-attributing the check to the subject) + `wf034-unit-unknown-system` (the ref-vs-clingo divergence: `ref` plays it, the ASP backend refuses the write) + `wf034-object-creation-without-subject` + `wf034-rolcheck-loze-andertefiller` (the two empty halves, on the same two documents their filled-name siblings break, so each position is witnessed in both spellings) — and, for wave 7 (r), seven more: `wf034-sentinel-gelest-vouwoperand` + `wf034-sentinel-listed-wratoperand` + `wf034-sentinel-geleste-setlezing` (the three spellings of the sentinel on a LISTED fold operand, all three rood-eerst and each with a different outcome in the third executor — played, thrown, declined) + `wf034-sentinel-kale-lezing` (the plain read, labelled in its NOTE as the one witness that was not rood-eerst: it pins the walk's own prologue arm, which had no case anywhere in the zone) — and, from the fix round the same day, `wf034-sentinel-builtin-operand` + `wf034-sentinel-builtin-wratoperand` (the latter removed with reason at L4, where a `BuiltinCall` carries no range; the KIND half: a single-argument builtin carrying an `elementObjecttype` has no fold source either, and the second of the two CRASHES the third executor on a getter of the role name) + `wf034-sentinel-null-vouwoperand` (the unspelled twin, and the row where `ref` ANSWERS: two executors agree off the declaration while the ASP backend refuses, and a unit-bearing numeric variant of the same shape throws in the third) — and, for arm (s), one more: `wf034-dagsoorteenheid-onbekende-dagsoort` (a `dagsoortPerEenheid` value naming a day kind no declaration carries, on the load-bearing plural key of `jutterswet-schouwrooster`; the row where NO consumer notices — verifier PASS, `ref` PASS, the ASP backend PASS and the Drools backend PLAYED, all four reproducing the untouched golden's own outcomes) — and, for arm (t), one more: `wf034-far-role-check-wrong-side` (a far-side role check whose `backref` names a real role on the FILLER's side, so both names resolve and the pair does not; the row where the form was not refused as a form at all — the two OCaml sentences were bottom sentences of the gate-8 list, which is why the same card gave each executor a named arm); at L2 (2026-09-24, `corpus/negative/README.md`, L2): removed with reason `wf034-sentinel-lijstelement-payload`, `wf034-sentinel-payload-onder-lijst`, `wf034-sentinel-structveld-payload`, `wf034-sentinel-sumtype-payload` (d), the seven `wf034-feitcreatie-*` (m), `wf034-sentinel-gelest-vouwoperand`, `wf034-sentinel-kale-lezing`, `wf034-sentinel-null-vouwoperand`, `wf034-onbekend-regeldoel`, `wf034-regeldoel-zonder-type`, `wf034-ambigu-typenpaar`, `wf034-zelftype-typenpaar`, `wf034-rolcheck-loze-andertefiller`; moved to another rule `wf034-sentinel-builtin-operand` (→ `wf047-bereik-op-builtin`), `wf034-sentinel-geleste-setlezing` (→ `wf053-to-many-list-fold-operand`), `wf034-foldbron-rolnaam-waardevouw` (→ `wf093-count-source-under-sum`); replaced `wf034-rolcheck-onbekend-andertefiller` by `wf034-role-check-unknown-fact-type` (the role arm: an undeclared fact type); rewritten `wf034-far-role-check-wrong-side` (the role arm: the subject does not fill the role, the red-first proof V4 (b)); added `wf034-role-membership-foreign-filler-class` (the role arm: a `counterpartType` that is no subtype of the other role's filler), and `wf034-sentinel-builtin-wratoperand` (removed at L4), `wf034-sentinel-listed-wratoperand` (an arm change to the name resolution); added in the L2 fix round `wf034-role-membership-role-as-plural` (the role arm: `role` written as the declared plural of the role the subject fills) and `wf034-role-membership-filler-class-unknown` (the role arm: a `counterpartType` that is no declared object type) + since 2026-09-25 `wf034-variable-without-binding` (a `VariableReference` as an operand at rule level; verifier-PASS before the arm, and `ref` replayed its cases) |
| WF-035 | operator typing (numeric, unitless power, date±duration order/unit/precision, unit compatibility, clamp). **Since 2026-09-25** each date ± duration form — two dates, a duration on the left, and the step's operand and unit — is one sentence in the verifier, `ref`, the ASP backend and the Drools loader, which refuses the form at load; the form is decided on the date type of either precision before the precision of the date picks an encoding | gate 3 (over serialized operand types) | `anchor/jutterswet-wachtersketen` (the numeric Plus, executed by gates 5–6) + `jutterswet-vervaltermijn` (date ± duration, the date-left plus and min forms, executed by gates 5–6) | `wf035-power-with-unit` + `wf035-datum-plus-datum` + `wf035-duration-plus-date` + `wf035-duration-minus-date` + `wf035-hour-by-day-date` + `wf035-unknown-unit` + `wf035-duration-without-time-unit` + `wf035-text-as-duration` (the date ± duration arms, each pinned by its `expect.txt`) |
| WF-036 | comparison/predicate typing (ordering, precision, family, domain, builtin operands) | gate 3 (over serialized operand types; the date-ordering arm crosses the date REPRESENTATION since 2026-09-05. A TYPELESS operand defers — the arm is skipped and WF-034 reports it — but the unknown SENTINEL does NOT defer: this arm reports it as a non-date operand, measured, exactly as the numeric-ordering and equality arms report theirs. Tested on day precision only until 2026-09-06: all seven corpus date orderings were day×day, so refusing the admitted millisecond half too moved zero of 504 reports — `jutterswet-meldstip` closed that, ordering a millisecond-precision attribute against a millisecond-precision literal in both directions; since L3, 2026-09-24, B15, the arm reads `Date.precision`) | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondstnummer` (date ordering, both operands day-precision) + `jutterswet-meldstip` (the millisecond half, `earlier` and `later` at a sub-day threshold) | `wf036-order-number-vs-text` + `wf036-date-ordering-precision` (the date-ordering arm's representation crossing — a v1 tightening the RegelSpraak frontend does not yet mirror) |
| WF-037 | aggregation/builtin shape: facet validity and arity per kind. **The filled count** (`filledCount`, ch. 4.3, landed 2026-09-20) adds three arms and two of them demand a form instead of forbidding one — exactly ONE argument (no listed form exists, because no written form does), an `elementType` (it is an instance fold), and that argument the SET source — since L2 (2026-09-24) the range makes it one (ch. 4.3) and the schema and every loader demand the range (B12), so what the verifier still decides is a range beside a non-reference source (the subject's own read has one value and a count source hands the fold instances, and neither has a filled count; `ref` refuses a role name at a value fold's source at evaluation). The count source is WF-093 (a)'s and this rule steps aside on it, so one malformation carries one rule; the kind is excluded from the "≥ 2 arguments" clause, whose sentence reads its arity law backwards. The three facets it never carries — `emptyFallback`, `dimensionSelection`, `granularity` — were refused by the existing, positively written clauses with no amendment, which is why each needed a witness of its own: a positively written MUST grants a facet to a named set and so acquires no failing case when the set grows. **Since L4** (delta B21) `granularity` is a `BuiltinCall`'s field alone and `emptyFallback`/`dimensionSelection` an `Aggregation`'s, so the arms that crossed a facet on the other node lost their object: the granularity arm reaches the builtin kinds only, and `wf037-aantalgevulde-granulariteit` was removed with reason. **The count** (`count`, maintainer decision 2026-09-20) is the other kind that clause excludes, and its own arity arm is the mirror image: NOT two or more arguments, because a count folds instances (ch. 3.4a) and the source language spells no operand list for one. It cuts a LIVE producer route: the RegelSpraak frontend's grammar carries an operand list at this rule — an unregistered widening of the one-operand EBNF — through which three sentences emitted unblocked, two with `diagnostics: []`; the frontend refuses them at the node from the same day and the grammar keeps the widening as a registered divergence, deleting an alternative being a parse change rather than a refusal. It closes the gap the 2026-09-19 set-source narrowing left on the other side of the same node — verifier-clean, `ref` answering the count of the FILLED operands, the ASP backend and both Drools branches refusing — and it does not read `elementType`, because a count carrying one BESIDE an operand list is the same listed form with a second claim (there all three executors refuse, and the document is still verifier-clean without this arm). Arity 0 and arity 1 without an element type are NOT a listed form and carry a second count arm (maintainer decision 2026-09-23): a count MUST carry an `elementType`, word for word the filled count's element-type arm, so both counts carry one law at one position. Before it they were verifier-clean and `ref` alone answered — the filled count once more on one own read, and 1 on a single `ListLiteral` whether the list held two elements or none; its value arm under the count now refuses by name. The single exit is an `InstanceSetUnion` argument, whose selections carry the population (WF-110 (c) forbids a second one on the count). **The converse arm** (2026-09-25): an aggregation carrying an `elementType` with NO argument, under every kind, and a `count` or `filledCount` whose one argument is a `ListLiteral`, are ill-formed. Before it the first shape was verifier-clean under every kind but `filledCount` and no executor ran it, and the second was refused under `count` on WF-047's binding sentence alone and under `filledCount` on the filled count's set-source arm; the filled count's arity and set-source arms and WF-047 step aside on the shape, and `ref` and the ASP backend refuse it in the arm's two sentences (ch. 4.3). **The fold-source arm** (2026-09-25): the one argument of an aggregation that carries `elementType` is an `AttributeReference`, under every kind, and every other node there — a `Literal`, a `Binary`, a `ParameterReference`, a `VariableReference`, a `BuiltinCall`, an `Unresolved` marker and any other — is ill-formed and named in the arm's sentence (a `ListLiteral` under a value fold stays WF-091's, and a union under a count WF-110 (c)'s; under every other kind WF-110 (a) steps aside on the union, so it is reported once). Before it the node was refused on WF-047's binding sentence alone where a range was written, a `total` without a range over a literal was verifier-clean, and the Drools branches played the counts by counting the range and crashed on or excluded the value folds; it takes over the filled count's set-source arm, WF-047 steps aside, `ref` and the ASP backend refuse it in its sentence, and the Drools loader refuses at load, in the verifier's sentences, the three shapes that name a population and hand no source (ch. 4.3) | gate 3 (fixed-arity kinds; date-construction arity OPEN) | `jutterswet-vondsttelling` — the one-argument instance count, which this arm leaves untouched by construction (across the four zones, the 112 one-argument counts over an `AttributeReference` all carry an element type and keep their reports, as do the 12 over an `InstanceSetUnion` without one, which take the count-source arm's exit, and the 1 union count WITH one, which WF-110 (c) refuses). **The filled count has no golden yet** — the wire, the rule and `ref` landed first, its worked examples living in `ref/test/` where gate 5's second leg runs them; the corpus witnesses arrive with the clingo and Drools legs, since a golden without a translator breaks gate 6 | `wf037-fallback-on-minimum`; for the filled count `wf037-filled-count-second-argument` (the listed spelling), `wf037-filled-count-without-element-type`, `wf037-filled-count-not-set-source` (since 2026-09-25 the fold-source arm's, below), plus one per refused facet: `wf037-filled-count-empty-fallback`, `wf037-filled-count-dimension-selection` (and `wf037-aantalgevulde-granulariteit` until L4, removed with reason); for the granularity facet on the builtin side, since the L4 fix round, `wf037-granularity-by-year-of` (the facet on a non-bucketing kind) and `wf037-time-proportional-part-without-granularity` (a bucketing kind without it); for the builtin arities and the value fold's operand count, also since the L4 fix round, `wf037-year-of-two-operands` (a one-operand kind with two), `wf037-duration-one-operand` (a two-date kind with one) and `wf037-first-one-operand` (a value fold with one operand); for the count `wf037-count-enumerated` (the measured two-operand form), `wf037-count-enumerated-triple` (the open end of the bound, reported by its operand count) and `wf037-count-enumerated-element` (the spelling that also claims an instance source); for the count's source `wf037-count-without-element-type` (one own read), `wf037-count-none-operand` (no argument) and `wf037-count-list-literal` (one `ListLiteral`, the spelling that showed the defect); for the converse arm `wf037-count-element-type-without-operand`, `wf037-count-element-type-list-literal`, `wf037-filled-count-element-type-without-operand`, `wf037-filled-count-element-type-list-literal` and `wf037-sum-element-type-without-operand` (the six value folds, witnessed by one kind); for the fold-source arm `wf037-count-element-type-literal`, `wf037-count-element-type-binary`, `wf037-count-element-type-parameter`, `wf037-count-element-type-variable`, `wf037-count-element-type-built-in` and `wf037-count-element-type-unresolved` under the count, `wf037-filled-count-not-set-source` under the filled count (moved there from the set-source arm), `wf037-sum-element-type-literal`, `wf037-sum-element-type-parameter` and `wf037-sum-element-type-union` (a union, reported by this arm alone) under a value fold with a range, and `wf037-total-element-type-literal` as the argument of a total without a range |
| WF-038 | **Since 2026-09-25:** the operand of `squareRoot` and of `absoluteValue` is a number (a `Number`, the percentage presentation included): `<kind> operand is <datatype>, must be a number`, a sentence `ref` and the ASP backend speak as well, read off the operand's static type before its value; the Drools loader refuses this form and every form of the duration clause at load, in the same sentences. Until then no clause read that operand, and a text operand was verifier-clean. The same day the duration clause's counted unit that is neither a time unit nor a declared day kind took one sentence in the three readers, `<kind>: the counted unit '<abbr>' is neither a recognised time unit nor a declared day kind`, the kind being `duration` or `absoluteDuration` (until then the verifier said `'<abbr>' is not a recognised time unit or declared day kind` and `ref` and the ASP backend `duration unit "<abbr>" is neither …`). aggregation operand typing (ordering-fold family, units, precision, rate for `total`, year for `easterSunday`, date for the day-precision extractors, `duration` operands and node-type unit). **Scope note, 2026-09-20:** the filled count (`filledCount`, ch. 4.3) is not a numeric fold in this rule's sense and has no operand-typing clause — its source may carry ANY type, because the test is presence and nothing is added or compared, so neither the numeric clause nor the ordering-fold family reaches it and the ch. 2.4 orderability paragraph has no grip on a fold that ranks nothing. Stated rather than left silent: without it the kind could be read under the numeric arm, and the typelessness of the test would be withdrawn by omission. No negative corresponds, and that is the point of a scope note — it removes a position from the rule instead of adding one | gate 3 (the ordering-fold family clause — form-independent, per-operand orderability and the cross-operand family with the two date precisions as two families, deferring on the unknown sentinel — plus `easterSunday`, day-precision and sub-day extractors, sum fallback, `duration`; two halves stay OPEN and unchecked: the `total` rate clause and unit compatibility between a fold's numeric operands) | `jutterswet-vondsttelling` (the sum fallback slice) + `anchor/jutterswet-jutleeftijd` (the `duration` slice, executed by gates 5–6) + `jutterswet-paasvondst` (the `easterSunday` and extractor slice, executed by gates 5–6) + `jutterswet-peildatum` (the ordering-fold slice and the widening's own witness: an articulated `first`/`last` pair over dates beside a second pair over numbers, which the v0 letter would refuse; executed by gates 5–6) + `jutterswet-hoogwaterpaar` (the date pair) + `jutterswet-vondstpiek` (the instance-fold `minimum`/`maximum` over a numeric element) | `wf038-easter-sunday-text` + `wf038-extractor-text` + `wf038-duration-mixed-precision` + `wf038-duration-without-time-unit` + `wf038-duration-without-node-type` + `wf038-minimum-enum-set-source` + `wf038-maximum-sum-type-set-source` (the instance-fold form, once per equality-only family) + `wf038-last-enum-articulated` + `wf038-first-sum-type-articulated` (the articulated form, the same two families — the four crossing the two forms with the two families, one per ordering kind) + `wf038-first-mixed-family` + `wf038-first-mixed-date-precision` (the cross-operand arm's two messages, each separately falsifiable) + since 2026-09-25 `wf038-absolute-value-text` + `wf038-square-root-text` (a text operand under each numeric builtin, verifier-PASS before the clause) + `wf038-duration-unknown-unit` (a duration counted in `vadem`; the arm fragment is the shared sentence) + `wf038-absolute-duration-unknown-unit` (the same for an `absoluteDuration`, whose sentence names its own kind) + `wf038-duration-text-operand` + `wf038-duration-hour-by-day-dates` (a text operand and a sub-day unit over day-precision operands; like every other arm of the duration clause they are one sentence in the three readers, naming the builtin's own kind) |
| WF-039 | enum label in domain; literal sign restriction; **since 2026-09-25** also the target's restriction: a numeric literal written into a declaration satisfies its sign restriction (`negative literal '<w>' is written into a target under a non-negative sign restriction`, and its positive and negative siblings), at WF-023's three write positions, standing aside where WF-023's sign clause already reports the literal's own restriction against the target's; `ref` and the ASP backend write such a value without refusal (measured) | gate 3 | `jutterswet-keurstaat` (the gaaf label) + `anchor/jutterswet-taxatie` (the sign-restricted literals 2 and 3) + `jutterswet-zustervondst` (the literals 12.50 and 3 written into `nonNegative` targets, clean under the target clause) + `jutterswet-nulspelling` (zero spelled `-0.00` and `-0` under `nonNegative`, both clauses; refused while the sign was read off the lexeme) | `wf039-negative-under-nonneg` + `wf039-literal-under-target-sign` (a literal `-5` of unrestricted type written into a `nonNegative` target; verifier-PASS before the clause) + `wf039-min-zero-under-negative-target` + `wf039-zero-rational-under-positive-target` (a zero spelled `-0` under `negative`, and `0/5` under `positive`; verifier-PASS while the sign was read off the lexeme) |
| WF-040 | dimension cell pins every dimension; selection labels resolve in order | gate 3 | `jutterswet-vormenkabinet` (the voorjaar cell read) | `wf040-unpinned-cell` |
| WF-041 | counting-quantifier threshold ≥ 1, at BOTH hosts of the vocabulary since 2026-09-16 — the propositional `Compound` and the first-order `QuantifierFold`, whose WF-108 (e) owns the field's PRESENCE and hands its VALUE here (the shared-helper discipline of WF-108 (c), applied to a bound) | gate 3 | `jutterswet-wachtploeg` (ten minste 2, on the composite) + `jutterswet-reddingsvloot` (all three counting quantifiers on the fold, thresholds 1 and 2) | `wf041-threshold-zero` (the composite) + `wf041-quantifier-fold-threshold-zero` (the fold) |
| WF-042 | time window: ≥ 1 bound; bounds are dates, either precision | gate 3 | `jutterswet-vormenkabinet` (the vanaf window) | `wf042-empty-window` |
| WF-043 | whole-period qualifier only on a time-dependent operand | gate 3 | `jutterswet-keurploeg` (whole-year over the `month`-granular keurvergoeding) + `jutterswet-sleepvloot` (the negated one) | `wf043-wholeperiod-nontemporal` |
| WF-044 | (consumer) Unresolved surfaces, never silently lowered | behavioural — gate 8 (`scripts/check-red.sh`): the two mandatory executors must both fault, pinned by `expectedFault {code, params}`: the fault kind EF-019 and its parameters, never a fragment of text. The Drools leg (decision V3-b) holds a Drools fault to the same pin only where the v0 backend carries the red-path form; where it does not, the gate lists the case with a reason (a reason-carrying exclusion list, stale-guarded) | `corpus/red/wf044-unresolved-reached` (the red witness: ref faults the evaluation, the ASP backend the translation; the Drools leg holds it to the same pin) | n/a (the marker is legal wire content; the fence is behavioural, not well-formedness) |
| WF-045 | dependency graph acyclic (creation recursion excepted; since *2026-10-22* the instance-ordered cycle as well, ch. 5.3: a value of an object that reads the same value of its predecessor over a switch relation whose links are the case's, evaluated per object by its depth in the chain, a cycle in the chain refused at load as EF-033) — over the CHAIN-CLOSED node identity of ch. 5.1 since *2026-09-12*, and the three write positions do NOT close alike: a population write closes upward, a member write by a form that ITERATES a type closes over the ⊑-comparable types in both directions, a member write by an OBJECT CREATION closes upward only (it fills the one instance it mints, and creation is not polymorphic), and reads stay literal. Until then both keyed on the written type name alone, which made the rule's promise false of every model with a supertype: the two witnesses below were verifier-clean and diverged in the reference interpreter, which is the last line of defence and reports `no fixpoint within 100 passes` — loud, but naming neither the rule nor the type. The boundary is measured and narrow: a subtype creation is not itself refused (the mirror creation, from subtype subject to supertype, and a creation sideways into a chain both stay clean, as does either inside a guarded recursive group), and two INCOMPARABLE subtypes stay two nodes. Cost: none, and not vacuously so — counted 2026-09-12, the closure widens a write in **9** corpus documents and not one of them gains a cycle, and a tenth — `jutterswet-wrakboeking` — exists precisely because the closure must NOT widen there (gate 3: 209 verifier-clean documents before, 210 after, the one arrival being that witness; gate 11's parity zone unchanged) | gate 3 | `jutterswet-vondsttelling`; the chain-closed reading's own clean witnesses are `anchor/jutterswet-strandrechtketen` and `golden/jutterswet-wrakrecht`, which write ONE inherited attribute from BOTH spellings (a rule at the ancestor, defaults groups at each), `golden/jutterswet-hoofdberging`, whose allocation writes an inherited target attribute from the subtype, `golden/jutterswet-zustervondst`, where a write at the parent reaches TWO incomparable children and they still do not reach each other, and `golden/jutterswet-wrakboeking`, the creation arm's own witness — a creation that mints its own SUPERTYPE and fills an inherited attribute, which the both-directions reading turns into a WF-045 refusal on a model with no cycle in it (measured 2026-09-12, both readings, with ref declining and clingo answering under the wrong one), replayed by gates 5, 6 and 12; the instance-ordered cycle's is `golden/jutterswet-vergunningketen` (a leeg-test on the predecessor and the positive form over a satellite, chains of one, two and three), replayed by gates 5, 6, 12 and 17 | `wf045-cyclic-attribute-dependency` (one type, one spelling) + `wf045-subtype-creation-cycle` (the population arm) + `wf045-inherited-attribute-spelling-cycle` (the member arm) — the three fall independently: a graph keyed on the written name catches only the first; for the instance-ordered cycle one per clause, `wf045-instance-cycle-to-many-predecessor`, `wf045-instance-cycle-created-relation`, `wf045-instance-cycle-shared-satellite`, `wf045-instance-cycle-direct-and-shifted-read` and `wf045-instance-cycle-to-many-satellite-step` (ch. 5.3) |
| WF-046 | no negative edge on a dependency cycle (strata exist); since *2026-09-23* every read under a leeg/gevuld test is a negative edge, in both polarities, and so is every read in a quantifier fold's range under `all`/`none`/`atMost`/`exactly` and in its matrix under `none`/`atMost`/`exactly` (ch. 5.2); since *2026-10-06* a role check reads its relation, and a negated one negatively | gate 3 | `jutterswet-penningjaar` (the negated aanmeldplichtig reading — a negative edge, verifier-clean, oracle-replayed); `jutterswet-opvolgerquorum` (the monotone parts of the quantifier fold — an `atLeast` range and matrix and an `all` matrix — on a recursive creation cycle, verifier-clean, replayed by gates 5, 6 and 12); the leeg/gevuld reading has no golden whose test reads a written node (counted 2026-09-23: none of 31), so its clean witness is the reference interpreter's WV-6.4-f (a leeg test in an attribute rule on a derived attribute, off any cycle, answering alike in both rule orders); `jutterswet-premiepot` (a role check on a relation a fact creation lays, read stratum-strict since 2026-10-06, off any cycle, replayed by gates 5, 6 and 12) | `wf046-negated-read-in-recursive-cycle` + `wf046-empty-test-recursive-creation` (`negated: false`) + `wf046-empty-test-negated-recursive-creation` (`negated: true`) + `wf046-empty-test-presence-recursive-creation` (a presence-true test alone: the both-polarities clause) + `wf046-empty-test-quantifier-fold-recursive-creation` (the test as a quantifier fold's matrix) + `wf046-quantifier-fold-all-range` (the `all` range) + `wf046-quantifier-fold-none-comparison` (`none`, range and matrix) + `wf046-quantifier-fold-none-matrix` (the `none` matrix alone) + `wf046-quantifier-fold-at-most-range` + `wf046-quantifier-fold-exactly-range` (the `atMost` and `exactly` ranges) + `wf046-quantifier-fold-at-most-matrix` + `wf046-quantifier-fold-exactly-matrix` (the `atMost` and `exactly` matrices) + `wf046-quantifier-fold-under-none` (the enclosing negation) + `wf046-quantifier-fold-range-other-type` (the `all` range over the relation a creation of ANOTHER type writes through its back-link, Wachter → Post; rewritten at L2 from `wf046-kwantorvouw-bindingloos-bereik`, whose derived range the migration wrote) + `wf046-negated-role-check-recursive-creation` (a negated role check on the relation the recursive creation lays, since 2026-10-06) |
| WF-047 | role binding resolves; fillers agree with the use site; the count source's written name denotes the bound role (declared name or plural; since L2 the migration and the emitter write the declared role name where the written one denoted neither) | gate 3 | `anchor/jutterswet-bergingstoezicht` (two fact types over one pair; allocation and fold bound, replayed by gates 5–6) + `anchor/jutterswet-maatschap` (the reciprocal binding: bound = opposite) + `anchor/jutterswet-wachtersketen` (the creation back-link binding) + `jutterswet-kaveltelling`, `jutterswet-muntpeiling` (the count sources whose written name the L2 migration replaced by the declared role name) | `wf047-dangling-role-binding` (resolution) + `wf047-bound-role-fills-source` (filler agreement) + `wf047-reciprocal-wrong-filler` (the single-role filler clause) + `wf047-creation-binding-wrong-filler` (the creation site) + `wf047-source-path-step-unresolved-role` (a path step); `wf047-bereik-op-builtin` (a range on a builtin; at L2 from `wf034-sentinel-builtin-operand`) was removed with reason at L4, where a `BuiltinCall` can carry no range, and since the L4 fix round `wf047-range-by-enumerated-fold` (a range on a listed value fold) pins the arm on the node that keeps it |
| WF-048 | removed at L2 (2026-09-24): the binding is required on the nodes this rule governed — an allocation's recipient role, a quantifier fold's range, a union selection's relation, and the range of a single-argument fold with an `elementType` under every kind but `total` (B12) — so no reader derives their relation from a type pair; the object creation's back-link role, which this rule never governed, stays optional and is still derived when absent (ch. 3.3; ch. 2.6, history) | — | — | `wf048-ambiguous-connecting-relation` and `wf048-telling-meerduidig` were removed with reason (`corpus/negative/README.md`, L2) |
| WF-049 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. temporal read: `base` temporal (attribute/granular parameter), day-precision `date`, element type | gate 3 | `anchor/jutterswet-aanmeldquotum` (attribute + parameter reads; hit, gap→leeg, empty date→leeg; replayed by gates 5–6) | `wf049-read-of-nontemporal` (`base`) + `wf049-read-at-datetime` (date operand) + `wf049-type-not-element` (element type) |
| WF-050 | defaults group: ≥ 1 default; exactly one target, resolving; default shapes per kind (recursing into sub-defaults, ch. 7.9 — a group-default carries `exceptions` and no own content); init default lowest; no other writer; multiplicity only declared-unordered; the allocation and creation clauses through a type chain per concrete type, with no gate — an allocation's shares or remainder on a type of the group's chain, above its subject or below it, and an object creation of a strict subtype of its subject (a creation of an ancestor creates no instance the group runs on) | gate 3 | `anchor/jutterswet-uitsluiting` (characteristic) + `anchor/jutterswet-vondstwaardering` (attribute + init) + `anchor/jutterswet-toelagegronden` (unordered groups, six conflict-path cases; all replayed by gates 5–6) + `jutterswet-strandmelding` (an object creation of the supertype beside a group on the subtype: the created instance does not inherit the stack, so the creation clause's chain arm does not report — the direction half, which a mutant reporting in both directions refuses on this golden and on no other document) | `wf050-empty-defaults` + `wf050-two-defaults-one-target` + `wf050-defaults-beside-rule` + `wf050-both-targets` + `wf050-init-not-lowest` + `wf050-rule-beside-attribute-defaults` + `wf050-allocation-beside-defaults` + `wf050-creation-beside-defaults` + `wf050-half-marked-group` + `wf050-subdefault-wrong-shape` + `wf050-remainder-beside-group` (the remainder half of the flat allocation clause) + `wf050-chain-assignment-beside-group` + `wf050-chain-remainder-beside-group` + `wf050-chain-creation-beside-group` |
| WF-051 | no dependency cycle through a defaults group (justification and consequence reads are negative edges, folded recursively through sub-defaults, ch. 7.9) | gate 3 (over the extended dep_forms graph, shared with ref/'s strata) | `anchor/jutterswet-uitsluiting` (the defaults group sits below its negated consumer — strata exist, oracle-replayed) | `wf051-defaults-read-own-target` + `wf051-cycle-through-subdefault` |
| WF-052 | a connecting relation with a to-one created-side role has at most one object-creation writer (fact creations are fault-paired already); a writer is a form — the versions of one object creation are one writer (since 2026-09-25), and a version of a nameless form, already WF-107's, is a writer of its own | gate 3 | `anchor/jutterswet-penninguitgifte` (one writer per to-one relation, executed by gates 5–6) + `jutterswet-opvolgerversies` (two disjoint versions of one creation on the to-one role `opvolging`, from `source.rgs`, replayed by gates 5, 6 and 12; refused under WF-052 before the form became the writer) | `wf052-two-writers-one-to-one-relation` (two different creations, still refused) + `wf021-creation-versions-overlap` (two overlapping versions of one creation, WF-021's since) |
| WF-053 | a navigated read resolves its walk: every binding resolves, each hop's near side is filled by the type the walk stands at, the endpoint is reached (⊑), every hop is to-one; a read without bindings is the subject's own read, so its `subjectType` EQUALS the subject type — WF-001 form, equality and not ⊑, one rule for the verifier and every executor (since L2 2026-09-24, B11); bindings on a fold source are ill-formed; the navigated characteristic check the same for its one hop. Until L2 also: written names agreed with the bindings, bindingless steps were uniquely derivable, and a written zero-hop self-reference passed a self-denotation test (ch. 4.1a, history) | gate 3 | `anchor/jutterswet-kustwacht` (single hop, chain and by-pair-derived bindings plus the navigated characteristic check, executed by gates 5–6 with leeg-on-absent and the negation asymmetry pinned) + `jutterswet-postwacht` (emitter-produced bindings since the 2026-08-18 pin bump) + `jutterswet-aanmelderkrediet` (the zero-hop self-reference, since L2 an own read, beside the reciprocal far hop, executed by gates 5–6) | `wf053-dangling-navigation-binding` (resolution) + `wf053-chain-near-side-mismatch` (chain typing) + `wf053-to-many-hop-in-scalar`, `wf053-to-many-rule-value`, `wf053-to-many-plusoperand`, `wf053-to-many-list-fold-operand` (cardinality; the last three from WF-096 at L2) + `wf053-read-fold-operand-unconnected-hop` (the unbound read with a foreign `subjectType`) + `wf053-own-read-supertype` (the unbound read whose `subjectType` is a supertype of the subject, added in the L2 fix round); `wf053-written-role-mismatch`, `wf053-bindingless-ambiguous` and `wf053-zelfverwijzing-vreemde-rol` were removed with reason at L2 |
| WF-054 | parent-scoped uniqueness: since L2 (2026-09-24, B8) the scope binding (`scope` since L4, `scopeRol` until then) IS the scoping, and since L4 only a version with one source carries one; the scope binding resolves, its near side fills the subject, the scope role is to-one. Until L2 also: a bindingless scope derived from the written scope field, the written name denoting the bound role, and a binding on an unscoped uniqueness being ill-formed (ch. 3.6, history) | gate 3 | `anchor/jutterswet-kustregister` (two scoped forms, the second's `scopeRol` derived by the L2 migration, executed by gates 5–6 with the scope-less exclusion pinned) + `jutterswet-postwacht` (emitter-produced `scopeRol` since the 2026-08-18 pin bump) | `wf054-dangling-scope-binding` (resolution) + `wf054-to-many-scope-role` (cardinality); `wf054-binding-on-unscoped` was removed with reason at L2 |
| WF-055 | the gedurende clip: a filter on the temporal total is a timeline operation — its condition is an own temporal characteristic check, a per-segment comparison of an own temporal attribute against a scalar bound, or a FLAT `all` composite of characteristic checks (`RUNTIME-SEMANTICS.md` ch. 3.6: the intersection of the members' true-periods; v0 partitions static comparison members into value-side guards and markers nested bullets — v1 narrows composite members to characteristic checks, a recorded narrowing) | gate 3 | `jutterswet-getijdenwacht` (both simple shapes) + `anchor/jutterswet-dubbelwacht` (the flat composite: overlap, disjoint, one-empty and split periods, executed by gates 5–6) | `wf055-nontemporal-clip-characteristic` + `wf055-navigated-clip-condition` + `wf055-composite-comparison-leaf` + `wf055-composite-nontemporal-leaf` + `wf055-composite-nested` |
| WF-056 | the pro-rata fold (`timeProportionalPart`) MUST carry a gedurende condition and one temporal source argument (a temporal attribute or granular parameter) as coarse as or coarser than its bucket; on a temporal target the bucket granularity MUST equal the target's declared granularity | gate 3 | `anchor/jutterswet-borgdeel` (the ch. 3.7 fold, executed by gates 5–6) | `wf056-no-during-condition` + `wf056-scalar-base` + `wf056-bucket-target-mismatch` |
| WF-057 | declared names are target-text-safe: no double quote, backslash or control character in any declared/form name (ch. 2.1) | gate 3 | every golden (a universal name-safety property: no single legal name exercises the boundary more than another; gate 3 witnesses none over-rejects) | `wf057-quote-in-attribute` + `wf057-backslash-in-characteristic` + `wf057-newline-in-rule-name` |
| WF-058 | the day count (`dayCount`, `RUNTIME-SEMANTICS.md` ch. 3.8) MUST carry a gedurende condition over an own temporal characteristic, takes no arguments, and its stated bucket MUST equal the target's declared granularity | gate 3 | `anchor/jutterswet-dagentelling` (the ch. 3.8 worked example verbatim, executed by gates 5–6) | `wf058-no-during-condition` + `wf058-bucket-target-mismatch` + `wf058-with-argument` |
| WF-059 | every declared attribute carries a datatype — the attribute pendant of WF-018's datatype clause; a null datatype splits the executors on otherwise clean input (ch. 2.2). Arms (2) and (3) — every `kenmerkGranulariteit` / `attribuutGranulariteit` entry carries a granularity (maintainer decisions 2026-09-09 and 2026-09-10) — were removed at M1 (2026-09-23): the side tables left the wire and `TimelineFacet.granularity` is a required, non-null enum, so a timeline without a grid cannot be written; their measurements are in `backlog/done/20260906-gedurende-kwalificatie-stiltes.md` and `backlog/done/20260909-attribuutgranulariteit-null.md` | gate 3 | every golden (a universal declaration property; gate 3 witnesses none over-rejects) | `wf059-attribute-without-datatype` (rewritten by hand in the M1 shape at M1: an Attribute node with `"datatype": null`, which the schema admits, still rejected on WF-059 and not by a load error); arms (2) and (3) lost `wf059-kenmerk-zonder-granulariteit` and `wf059-attribuut-zonder-granulariteit` with them (stamp verwijderd-met-reden, `corpus/negative/README.md`) |
| WF-060 | SumType declaration: ≥ 1 constructor, unique names (ci), admissible payload datatypes (Number, day-precision Date, declared SumType), acyclic payload graph (ch. 2.4) | gate 3 | `anchor/jutterswet-beschikkingsom` (executed by gates 5–6) | `wf060-duplicate-constructor` + `wf060-text-payload` + `wf060-recursive-sumtype` |
| WF-061 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. SumType use site agrees with its declaring domain; two sum-typed comparison operands name the same domain (the nominal rule, ch. 2.4) | gate 3 | `anchor/jutterswet-beschikkingsom` | `wf061-use-site-drift` + `wf061-cross-sumtype-equality` + `wf061-undeclared-domain` |
| WF-062 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. Construct: sum-typed node; declared constructor; argument present iff unary; argument type matches the declared payload (ch. 4.7) | gate 3 | `anchor/jutterswet-beschikkingsom` | `wf062-nullary-with-argument` + `wf062-payload-type-mismatch` + `wf062-construct-on-enum` (the asymmetry the 2026-09-01 widening left standing: `Match` widened to enumeration scrutinees, `Construct` deliberately did not — an enumeration value is a `Literal`) |
| WF-063 | Match totality over BOTH keuzetype variants (ch. 2.4/4.7): keuzetype-typed scrutinee (sum type or enumeration) naming a domain declared in that variant; arms cover the declared alternatives exactly, each once, none alien; on a sum scrutinee binder iff unary, on an enumeration scrutinee binder always null | gate 3 | `anchor/jutterswet-beschikkingsom` (the sum arm) + `jutterswet-vaarwatertoeslag-catala` (the enumeration arm: an all-nullary keuzetype normalised to its `Enumeration` domain, WF-087, matched arm by arm — replayed by gates 5–6) | `wf063-missing-arm` + `wf063-alien-constructor` + `wf063-binder-on-nullary` + `wf063-duplicate-arm` (sum) + `wf063-enum-missing-value` + `wf063-enum-binder-on-arm` + `wf063-enum-alien-value` (enumeration) |
| WF-064 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; `jutterswet-meldtermijnen` carries the case-only drift at this rule. every BinderRef resolves to an enclosing binder (a match arm's binder, a lambda/function parameter, or — the 5c widening — a `Let` binder scoped to its body); the closure clause: inside a closed lambda/function body the set is closed at the boundary, so a BinderRef reaching an enclosing rule's Let binder or an outer fold's parameter is ill-formed (an inside-body Let stays in scope); match arm bodies agree on one result datatype (ch. 4.7/4.8/4.10; `VariableReference` stays outside — row 24) | gate 3 | `anchor/jutterswet-beschikkingsom` + `anchor/jutterswet-tariefstelling` (Let binders) + `jutterswet-meldtermijnen` (a domain name in another case at this rule, since 2026-09-25) | `wf064-unbound-binderref` + `wf064-mixed-arm-types` + `wf064-enclosing-binder-in-closed-body` (the closure clause) |
| WF-065 | ListType: first-order non-list element (Number, day-precision Date, declared SumType, or — the 5c widening — declared StructType); no granularity on a list-typed attribute (ch. 2.4). Since WF-087 (2026-09-01) an all-nullary keuzetype normalises to `Enumeration`, so it is no longer an admissible element type: the list carrier demands the payload form (`frontends/catala/test/refuse_list_nullary_enum.catala_en`) | gate 3 | `anchor/jutterswet-staffelvergoeding` (executed by gates 5–6) + `anchor/jutterswet-vondstdossier` (the list-of-struct widening) | `wf065-list-of-list` + `wf065-temporal-list-attribute` |
| WF-066 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. ListLiteral elements carry the declared element datatype (ch. 4.8) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf066-heterogeneous-literal` |
| WF-067 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. FunctionDef: ci-unique names, typed unique parameters, resolvable result, body type EQUALS the declared result (soort and keuzetype domain alike — the write half of ch. 2.4's domain identity for functions, where WF-068 is the read half), closed body (the 5c widening admits FieldAccess/StructLiteral), acyclic invocation graph (ch. 2.11) | gate 3 | `anchor/jutterswet-aanspraaktelling` | `wf067-recursive-functions` + `wf067-attribute-read-in-body` + `wf067-body-result-drift` (a body of a second enumeration domain under a truthful `Invoke`) |
| WF-068 | **Since 2026-09-25:** the datatypes this rule compares — each argument against its parameter, and the node's `type` against the declared result — are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte. `jutterswet-aanspraakherziening` carries the drift at the result (an `Invoke` typed `AANSPRAAK` against the result `aanspraak`); the argument comparison runs through the same function and no golden carries the drift there. Invoke: resolvable function, arity, argument types, result type (ch. 4.8) | gate 3 | `anchor/jutterswet-aanspraaktelling` + `jutterswet-aanspraakherziening` | `wf068-unresolved-invoke` + `wf068-arity-mismatch` |
| WF-069 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; `jutterswet-meldtermijnen` carries the case-only drift at this rule. Lambda only as a Fold/MapList operand and the operand only an inline Lambda (no bare FunctionDef name); IfThenElse only inside lambda, function or (the 5c widening) `Let` bodies; lambda arity per host, typed parameters, closed bodies (the 5c widening admits FieldAccess/StructLiteral); IfThenElse branch agreement (ch. 4.8/4.10) | gate 3 | `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-vondstdossier` (the map-over-struct-field body) + `anchor/jutterswet-tariefstelling` (the Let-body IfThenElse) + `jutterswet-meldtermijnen` (a domain name in another case at this rule, since 2026-09-25) | `wf069-lambda-outside-fold` + `wf069-ifthenelse-outside-body` + `wf069-named-function-operand` |
| WF-070 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; `jutterswet-meldtermijnen` carries the case-only drift at this rule at a `Fold`, `jutterswet-aanspraakherziening` at a `MapList` (a lambda body typed `AANSPRAAK` under the element `aanspraak`). fold-family shapes: list-typed source; Fold accumulator first-order non-list shared with init/body/type, element parameter = source element; MapList/FilterList/ListLength result typing (ch. 4.8) | gate 3 | `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-aanspraaktelling` + `jutterswet-meldtermijnen` (a domain name in another case at this rule, since 2026-09-25) + `jutterswet-aanspraakherziening` (the same at a `MapList`) | `wf070-list-accumulator` + `wf070-scalar-source` + `wf070-maplist-constructor-list` (since 2026-09-25: two datatypes that differ only inside their members — here a constructor's payload — are each rendered whole, where the message named one type twice) |
| WF-071 | StructType declaration: ≥ 1 field, ci-unique names, admissible field datatypes (Number, day-precision Date, declared SumType, declared StructType), acyclic field graph shared with WF-060 (ch. 2.4). Since WF-087 (2026-09-01) an all-nullary keuzetype normalises to `Enumeration`, so it is no longer an admissible field datatype: the struct carrier demands the payload form (`frontends/catala/test/refuse_struct_nullary_enum.catala_en`) | gate 3 | `anchor/jutterswet-vondstdossier` | `wf071-duplicate-field` + `wf071-text-field` + `wf071-recursive-struct` |
| WF-072 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. StructType use site agrees with its declaring domain — same fields, datatypes, ORDER; two struct-typed comparison operands name the same domain (the nominal rule, ch. 2.4). It binds a use site to the domain it NAMES; binding a use site to the declaration it reads from or writes into is WF-090 | gate 3 (a datatype WALK, the struct sibling of WF-088's `enum_use_site`, over every `StructType` in the MODEL and EXPRESSION layers: attribute and parameter datatypes, a `ListType` element inside one, `FunctionDef` parameters and result, a struct-typed FIELD inside `domains` — a position WF-088 does not need, since WF-060/065/071 fence enumerations out of every declaration position — and every expression node's `type`, via `walk_expr`'s type prologue. THREE positions stay outside the walk because no negative can isolate them: a matched struct's own `velden`, since the agreement test is an equality on the whole field datatype; a sum-type payload, since WF-060 admits no struct payload in a declaration at all; and — the same position WF-088 records — a rule's TARGET `type`, WF-022's lvalue rather than an expression node, present in five goldens and two red documents, where drift is measured harmless (reverse OR rename the fields of `jutterswet-dossierspelling`'s `/resolvedRegels/1/doel/type` — a v0 wire pointer — and both variants stay schema-valid and verifier-clean while ref and clingo each replay all three pinned expectations, and a value sharing the target type's lie is caught by WF-090's write half against the DECLARED datatype — measured) — all three measured. The 2026-09-03 widening is what makes "the declaration is the source of truth" mechanically true for this variant: the inline field list is what the executors read — ref's shared case loader crosses a record against it, the ASP backend encodes struct terms POSITIONALLY out of it — so a drifted copy is a wire-level lie both consumers act on) | `anchor/jutterswet-vondstdossier` + `jutterswet-dossierspelling` (the ci witness; both replayed by gates 5–6) — and a measured zero over-rejection across every golden and red document and, read-only, the parity zone | `wf072-use-site-drift` (an attribute datatype) + `wf072-cross-struct-equality` + `wf072-list-element-drift` (the `ListType` look-through: verifier-clean before, ref faulted at the field read while clingo reported `PASS (3 expectations)`) + `wf072-domain-field-drift` (the `domains` position) + `wf072-functiondef-drift` (the `functionDefs` position, which the corpus carries for real in `jutterswet-kamerhuishouden-catala`) + `wf072-expression-type-order` (the expression layer, and the wrong-answer witness: verifier, ref and schema clean on ONE reordered inline copy while clingo derived 5.03 for 125.50) |
| WF-073 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; `jutterswet-vangstwaardering` carries the case-only drift at this rule (a nested `StructLiteral` typed `KERNWAARDE` in a field declared `kernwaarde`). StructLiteral: declared struct type; exactly the declared fields, each once; each value type matches the field declaration — a full datatype equality, so a nested struct field value of another domain is caught here and needs no WF-090 arm (ch. 4.9) | gate 3 | `anchor/jutterswet-vondstdossier` + `jutterswet-vangstwaardering` (the case-only drift, since 2026-09-25) | `wf073-missing-field` + `wf073-alien-field` + `wf073-field-type-mismatch` |
| WF-074 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; `jutterswet-vangstwaardering` carries the case-only drift at this rule (a `FieldAccess` of the field `kern` typed `KERNWAARDE` against the declared `kernwaarde`). FieldAccess: struct-typed base; field a declared field; node type = the field datatype — a full datatype equality, so a struct-typed field read of another domain is caught here and needs no WF-090 arm; the domain the BASE reads from is WF-090's (ch. 4.9) | gate 3 (against the DECLARATION, never the base's inline copy — which is why WF-072's walk reaches every expression `type`: measured, a base whose inline copy renamed the declared fields was verifier-clean and ref-green while the ASP backend raised this same WF-074 against the copy) | `anchor/jutterswet-vondstdossier` + `jutterswet-vangstwaardering` (the case-only drift, since 2026-09-25) | `wf074-unknown-field` + `wf074-nonstruct-base` |
| WF-075 | nested exception form (ch. 7.9): a default's `exceptions` field carries ≥ 1 exception group; two or more require the `unordered` marking (the root regime, at every level); no initialisation/input default in sub-defaults; every default carries own content, a nested exception group, or is the initialisation default; a group-default (`exceptions`, no own content) carries no own justification; recurses | gate 3 | `anchor/jutterswet-nestuitzondering` (attribute target) + `anchor/jutterswet-nestkenmerk` (characteristic target) + `anchor/jutterswet-nestdiepte` (three-level depth) — admitted through the Phase-5c executor card | `wf075-empty-leden` + `wf075-two-members-unmarked` + `wf075-init-in-subdefault` + `wf075-contentless-default` + `wf075-groupdefault-with-justification` |
| WF-076 | **Since 2026-09-25:** the datatypes this rule compares are compared as ch. 2.1 matches names — a domain, value, constructor or field name case-insensitively, a unit byte for byte; no golden carries the drift at this rule, which compares through the one function WF-064, WF-069 and WF-070 are witnessed at. `Let`: the node `type` equals its `body` type; the binder is visible only in the `body` (never in its own `value`); `value` and `body` well-formed; the binder joins WF-064 (ch. 4.10) | gate 3 | `anchor/jutterswet-tariefstelling` — admitted through the Phase-5c let/decimals executor card | `wf076-body-type-mismatch` + `wf076-unbound-binder-in-value` + `wf076-let-sign` + `wf076-let-unit-system` + `wf076-let-canonical-unit` (since 2026-09-25: a let type that differs from its body in one facet the rendering leaves out — sign, unit system, canonical unit — names that facet on each side, where the message named one type twice) |
| WF-077 | the condition-operand discipline: everywhere an `IfThenElse` is admissible (lambda, function or `Let` bodies), its condition READS only binders, parameters and literals (arithmetic combinators over those are admissible — the reads are what matter); an attribute/field/characteristic read routes through a `Let` binding first — the leeg-barrier at the read (ch. 4.10) | gate 3 | `anchor/jutterswet-tariefstelling` + `anchor/jutterswet-staffelvergoeding` (a `Binary` over binders as a condition operand) | `wf077-attribute-in-condition` + `wf077-fieldaccess-in-condition` |
| WF-078 | removed at L2 (2026-09-24): a `BinderRef` carries no `type` of its own (B13) — its type is its binder's declared datatype — so a binder read cannot state another soort (ch. 4.7, history) | — | — | `wf078-binderref-type-lie` was rewritten on WF-076 at L2 (`wf076-let-kind-against-body`) |
| WF-079 | no two unconditional computing versions (tagged `presentation` `equation` or `calculation`, without `condition`) of different forms with overlapping validity on one (type, attribute) target without defaults — the single-writer discipline of WF-026/WF-050/WF-052 for bare attribute writers (ch. 7.7); characteristic attributions (neither tag nor flag) exempt (monotone), guarded case splits outside, and the versions of one form never paired (their overlap is WF-021's); the PHASE5D-DIFF §8.2 un-pinnable form refused ahead of the square; through a type chain the rule is read per concrete type — outside the gate of `RUNTIME-SEMANTICS.md` ch. 7.10 (no defaults group on the target in the lower type's chain) a version on a type and one on its ancestor pair as well, reported at the lower type's version, and there the priority the report names is a defaults group alone (`unordered` across a chain is a composition ch. 7.10 refuses); the initialisation arm: no two initialisation versions of different forms with overlapping validity on such a target, conditional or not (their guard turns an overlap into an order, `RUNTIME-SEMANTICS.md` ch. 5.4), read per concrete type through the chain as the computing pair is | gate 3 | `anchor/jutterswet-rekenkamer-overzee` (a guarded gewone/overzeese case split on one target — the fence does not over-fire on conditional writers) + `jutterswet-diepwrakbasis` (bare rules on the highest and the lowest type of a three-level chain and a defaults group on the middle type: the pair lies inside the gate and is not reported — the gate half of the chain arm, which a mutant without the gate condition refuses on this golden and on no other document of the four zones) + `anchor/jutterswet-strandrechtketen` (a defaults group on each of two levels), `jutterswet-wrakbasis` (a group on the supertype, a bare rule on the subtype) and `jutterswet-wrakerfbasis` (a bare rule on the supertype, a group on the subtype), whose chains carry at most one bare writer on the target, so the chain arm forms no pair there; for the initialisation arm `jutterswet-initialisatievolgorde` (two initialisations of one type on two targets: not paired) and `jutterswet-wrakinitialisatie` (one initialisation, folded into a subtype's stack) | `wf079-two-unconditional-writers` + `wf079-chain-unconditional-writers` + `wf079-two-initialisations` + `wf079-chain-two-initialisations` |
| WF-080 | a value-position `AttributeReference` resolves to an attribute, not a characteristic (a characteristic is read through a characteristic check, ch. 4.5) — closes the tolerated `has_attr||has_kenmerk` leak in both the generic value walk and the aggregation-source path (a value-yielding fold's source; a count's role source is out of its scope: since L2 its written name is WF-047's, which requires it to denote the bound role, and a count source that names a member of the element type is WF-093's; until L2 it stayed in scope of WF-034); the rule TARGET lvalue is out of scope (WF-022) | gate 3 | `jutterswet-vondsttelling` + `anchor/jutterswet-penninguitgifte` (a `sum` folding an attribute source; gate 3 witnesses none over-rejects) | `wf080-value-position-characteristic` + `wf080-aggregation-source-characteristic` |
| WF-081 | the string of a literal's `number` variant is the number pattern of the case format, the grammar the executors share (ch. 4.1): `'-'? digits`, `'-'? digits '.' digits`, or `'-'? digits '/' [1-9][0-9]*`; unit or currency tokens, whitespace, `+`, exponents, grouping, a decimal comma and a fraction without a leading digit are ill-formed (the unit belongs in the datatype's `unit`) — closes the gate blind spot where a unit written into the lexeme passed the verifier while both executors fault on it; keyed on the variant, the `date` and `dateTime` variants and `text` out of scope. **Narrowed at L4** (2026-09-24, delta B22): until then the rule mapped `,` to `.` and admitted `.5` and a denominator with a leading zero; no literal carried those forms after the migration, which writes the point and keeps the comma in `source` | gate 3 | `anchor/jutterswet-tariefstelling` (integer and decimal lexemes through the fence; gate 3 witnesses none over-rejects) | `wf081-unit-in-lexeme` + `wf081-decimal-comma` (the decimal comma, since L4) |
| WF-082 | the `statuteAnnotations` extension field of a Rule (ch. 3.1; `bronAnnotaties` on each flattened entry until L4) and, since 2026-10-21, of a Default (ch. 6.1): when present a non-empty list of non-empty trimmed statute annotations; a rule or a default without annotations omits the field — absence leaves no trace | gate 3 | `jutterswet-hardheidsoverschot` (two annotated rules on the wire; replayed by gates 5–6 — until 2026-09-24 this row named `jutterswet-hardheid`, which carries no annotations: measured, the one golden that does is this one); `jutterswet-tweeledige-grondslag` (a default with two annotations, one containing `"; "`, and one without; replayed by gates 5, 6 and 12, re-emitted by gates 10 and 16) | `wf082-empty-source-annotations` + `wf082-blank-entry` + `wf082-padded-entry` (on a rule); `wf082-default-empty-annotations` + `wf082-default-blank-entry` + `wf082-default-padded-entry` (on a default, the last one nested in an exception group; each pins `kind=default`) |
| WF-083 | a SINGLE-argument aggregation's fold source is anchored and never navigates (ch. 4.3; a fold with two or more arguments is the listed value fold, whose operands are ordinary reads governed by WF-053): (c) the source's `subjectType` names the fold's anchor per position (ch. 4.3, the reading of the source; since L2 2026-09-24) — the `elementType` for a value fold's set source, the fold subject for the count source and for the subject's own read (a `total` without a range) — and, since the L2 fix round, a `total` without a range names the fold subject as its own `elementType` as well (equality, WF-001 form: without a range the fold is the subject's own read); navigation bindings on a fold source are WF-053's. (a) a written path and (b) a written role on a fold source left the wire with L2 (B11, history) | gate 3 | `jutterswet-aanmeldertelling` (the self-denoting count "het aantal vondsten van de aanmelder" through both executors, since L2 a plain count source); `jutterswet-tempototaal` (the set anchor and the subject anchor side by side); `jutterswet-sluitsom` (the SCOPE: a listed value fold whose operand navigates, played by ref, the ASP backend and both Drools branches) | `wf083-fold-source-foreign-element-type` (a set source anchored on the subject), `wf083-fold-source-anchor-by-operand` (a count source anchored on the element type, rewritten at L2), `wf083-fold-source-foreign-object-type` (an own read anchored elsewhere), `wf083-fold-source-anchor-by-intermediate-type` (an intermediate path type), `wf083-total-foreign-element-type` (a `total` without a range whose `elementType` names another type than the subject, added in the L2 fix round); `wf083-fold-source-navigates-away` and `wf083-vouwbron-navigatierol-buiten-de-wrat` were removed with reason at L2 |
| WF-084 | the string of a literal's `date` variant (keyed on the variant since L4; a day-precision `Date` names it, WF-114) is the strict ISO form `yyyy-mm-dd` the executors parse (ch. 4.1) — fixed field widths (four-two-two digits), single `-` separators, month 1–12, day 1..days-in-month (leap-aware); the Dutch day-first source snippet is ill-formed, whether the bare `31-01-2024` (silently misread as year 31 → `0036-07-16`, a differential-blind wrong answer both executors agreed on) or the `dd. `-prefixed form (both executors fault) — closes that blind spot, the emitter now writing the bare ISO lexeme and keeping the snippet in `source` (`bron` until L5); datetime lexemes out of scope | gate 3 | `jutterswet-vondstverjaring` (an ISO date literal through the fence, replayed by gates 5–6) | `wf084-source-snippet` + `wf084-bare-non-iso` |
| WF-085 | the day-kind reference graph is acyclic (ch. 5.4 / ch. 3.7): a day-kind condition may reference other day kinds (a `Predicate` of kind `dayKind`), but mutually recursive definitions have no fixpoint — a loud refusal where the v0 toolchain resolves a cycle to a silent false (the §9.10 recursion prohibition unenforced for day kinds); diverge-from-v0 (v0 neither checks nor documents it) | gate 3 | `jutterswet-schouwkalender` (an acyclic `hoogschouwdag -> schouwdag` reference, verifier-clean, replayed by gates 5–6 over its ordinary rule) | `wf085-day-kind-reference-cycle` (two mutually referencing day kinds) |
| WF-086 | a defaults group MUST NOT target a temporal attribute (a `timeline` facet on its Attribute node; v0 `attribuutGranulariteit`) nor a temporal characteristic (a `timeline` facet on its Characteristic node; v0 `kenmerkGranulariteit`) — a defaults group decides at one fixpoint and has no timeline coordinate to write (ch. 6); the document-layer fence ahead of the executors (ref faults on both arms — defense-in-depth — and the Drools defaults service skips); temporal defaults targets are unscheduled (PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a) | gate 3 (over the prioritized-defaults node via the shared `attr_temporal` helper and the loader's characteristic-granularity table, built from the `timeline` facets) | `anchor/jutterswet-vondstwaardering` (attribute defaults on a non-temporal target — gate 3 witnesses WF-086 does not over-fire) | `wf086-temporal-default-target` (attribute arm), `wf086-temporal-default-characteristic` (characteristic arm) |
| WF-087 | the keuzetype family's nullary rule (ch. 2.4): a `SumType` declaration whose constructors ALL carry a null payload is ill-formed — the nullary keuzetype has one wire form, the `Enumeration` domain, so such a declaration normalises to it (nullary-as-sum is sugar, the enumeration is the kernel; two frontends cannot express one meaning in two shapes). Stated over the declaration; WF-061 carries the use sites. No converse: payload never converts | gate 3 (over `domains`, beside WF-060) | every payload-carrying SumType golden — `anchor/jutterswet-beschikkingsom`, `anchor/jutterswet-aanspraaktelling`, the `synth3-*` sum witnesses (gate 3 witnesses WF-087 does not over-fire on a keuzetype with payload) | `wf087-all-nullary-sumtype` + `wf087-unit-sumtype` (the lower boundary: ONE nullary constructor — a unit type is a nullary keuzetype too, and it is what a `List.length > 1` weakening of the rule would let through) |
| WF-088 | an `Enumeration` use site repeats the value list of the domain it names — same values (ci), declaration order (ch. 2.4). The enumeration sibling of WF-061, and what makes "the declaration is the source of truth" mechanically true for this variant: WF-039 reads the LOCAL list where WF-063 reads the DECLARATION, so a drifted copy passed both while ref faulted at evaluation and clingo derived nothing | gate 3 (model layer: attribute, parameter and `FunctionDef` datatypes; expression layer: every node's `type`, beside WF-007's number-form check — the same "every datatype in the document" split, though the two walks recurse differently — see WF-007's OPEN note). Until L2 one position stayed deliberately outside the walk: a rule's TARGET `type`, which is WF-022's lvalue, not part of the expression walk — a drifted copy there was verifier-clean at this rule's landing and measured harmless (drift the `doel` type of `jutterswet-keurstaat` and all three cases still replay to their pinned expectations in ref AND clingo, because both write through the target attribute's DECLARED datatype); from WF-091's rule-target arm on the same drift was refused on an ATTRIBUTE target (residual structure mismatch, measured on that same document) and stayed clean only on a CHARACTERISTIC target, where the identity family defers by construction (and a value that shared the target type's lie was refused by WF-023's keuzetype clause, which crosses that same declared datatype: `wf023-schrijf-doeltype-leugen`). Since L2 the target carries no `type` of its own (B13), so that position no longer exists | `jutterswet-vaarwatertoeslag-catala` (the `Vaarwater` domain repeated identically at two use sites since L2: the attribute datatype and a `Literal` type; four until L2, when the `Match` scrutinee's and an equality operand's `AttribuutRef` type left the wire) + `jutterswet-keurstaat` (the `keurklasse` domain repeated at six use sites since L2: three attribute datatypes and three `Literal` types; eleven until L2, with five `AttribuutRef` types) | `wf088-use-site-value-drift` (a value the declaration lacks) + `wf088-use-site-value-order` (the same values, reordered — WF-006 order is normative) + `wf088-literal-type-drift` (an impeccable model layer, a drifted `Literal` type deep in a rule: the expression-layer arm) + `wf088-functiondef-drift` (two violations on one `FunctionDef` — parameter and result type — both reached through the walk's recursion into sum-type payloads, the arm no other case exercises; until L2 a third on the body's binder reference type, which left the wire with B13) |
| WF-089 | **Since L5:** the element-passing aggregation is `first`/`last`/`minimum`/`maximum` and the composite positions name their operators and kinds in the L5 spelling (`times`, `sum`, `total`, `count`, …); the record below keeps the v0 names it was written in. **Since L2 (2026-09-24):** the reference-expression use site left the wire with the reference's own `type` (B13) — a reference's type IS its declaration's; the element-passing aggregation and the composite positions remain (ch. 2.4, ch. 4.1). What follows records the rule as it stood until L2. domain identity, read half (ch. 2.4): a use site that TAKES a keuzetype value names the keuzetype — variant AND domain name — of the source it takes it from, whenever either side names one, ci, looked up through a `ListType` element. Two use sites: a **reference expression** takes it from the declaration it READS FROM — attribute (at the endpoint type; the dimension-cell read over it; the fold source, which the expression walk hands off to the fold-source shape rules), parameter, binder (a match arm's payload, a lambda/function parameter, a `Let` value, a filter element) — and an **element-passing aggregation** (`EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM`, the folds that select a contributor rather than compute) takes it from EACH OPERAND, which is itself anchored. **Since 2026-09-04 a third: a COMPOSITE position.** WF-091's derived arms (ch. 4.1) run this same facet engine against a formula instead of a declaration, and the number-follows-the-facet clause hands the domain facet to THIS rule there as well — an additive `Binary`, a `maal`, a `Rounding`, a `Clamp`, a `SOM`, a `TOTAAL` and an `AANTAL` all report under WF-089 when their annotation names a keuzetype domain their derivation does not (the element-passing folds keep the per-operand cross above instead, and the derived cross skips the facet there for exactly that reason). The derived side of that comparison names no domain whenever it is numeric, so the firing arm is the one-sided one. This is the domain extension of the WF-023/027/078 soort cross and the other half of WF-088/WF-061 (which bind a use site to the domain it NAMES). Its write half — every value entering such a declaration names the same keuzetype — is WF-023 (rule target, object-creation member, defaults consequence) and WF-067 (function body); no half suffices alone — the read half without the write half was measured open (fix round 1), the reference positions without the aggregation position were measured open again (fix round 2) — and only all three together make ref's "no arm" fault unreachable on a verifier-clean document whose case input the shared loader admitted. `TemporalRead`, `Invoke` and `Lambda` parameters are already crossed structurally (WF-049/068/070); `Construct` reads no declaration. The declaration side needs no rule: WF-060/065/071 fence a nested `Enumeration` out of every declaration position. Its struct sibling — the same sentence for struct domains, which are NOT keuzetypes — is WF-090 (2026-09-03), sharing this rule's machinery and call sites under its own number | gate 3 (the expression walk's `AttribuutRef`, `DimensieCel`, `ParameterRef` and `BinderRef` arms — the first two crossing a NAVIGATED read at its endpoint attribute, which the parameter and binder arms have no analogue for — plus the fold-source arm and the aggregation arm, and since 2026-09-04 WF-091's derived-arm entry point, where this number carries the domain facet at a composite; the unknown sentinel, an unresolvable reference or an absent `type` defers). Two honesty notes. The **ci** comparison is pinned by `jutterswet-vaarwaterspelling`, re-measured 2026-09-25 — until L2 its case drift sat on the reference's own `type`, which left the wire with it (B13), and from then until that date no golden pinned the clause. The identity this rule compares (variant and domain name, ci) is the one WF-023's write half compares, and no read-half position admits a keuzetype in a verifier-clean document — the element-passing folds take numbers and dates only (WF-038), the composite positions are numeric — so the golden pins it at a write: an enumeration literal typed `VAARWATER` written into an attribute declared `Vaarwater`. Making the identity case-sensitive turns that golden red under WF-023, and with it `jutterswet-meldtermijnen`, whose write carries a sum-type domain name in another case (WF-023 at `/rules/rules/1/versions/0`), and no other golden or red document (measured 2026-09-25 over the 301 of them). The same golden's attribute declaration names the domain `VAARWATER` as well, which pins the ci LOOKUP of the domain a use site names: a case-sensitive lookup turns it red under WF-063. The rules that compare two datatypes whole (WF-049, WF-061, WF-062, WF-064, WF-066 to WF-070, WF-072 to WF-074, WF-076) compared the domain name in them byte for byte until later that day; since then they match it case-insensitively as well (ch. 2.1), witnessed by `jutterswet-meldtermijnen`. The `ParameterRef` arm's positive witness is `jutterswet-meldplichtspoor`, whose `Fold` takes as its LIST OPERAND a reference to a list parameter of declared-`SumType` elements — an ordinary walked reference on THIS arm, NOT the fold-source arm, which in these rows is attribute-only (it fires for an `AttribuutRef` fold argument and for nothing else; every other fold argument, this list parameter among them, falls through to the ordinary expression walk): the `ListType` look-through this rule states puts that reference INSIDE the arm, use site and declaration naming the same domain. What carries the form is TWO legs of the square, said exactly: all eight of that golden's cases replay exactly in `ref` and in clingo (gates 5 and 6); the golden carries no frontend source at all (no `bron`, so gates 9 and 10 never see it); and the Drools leg declines it at the LOADER, ahead of any case, with its own reason (`EXCLUDE loader: form not carried by the v0 Drools backend: Datatype:SumType (unknown datatype node)` — measured 2026-09-05), so the golden sits in gate 12's grouped auto-refusals and never in its played set. Re-measured 2026-09-05 over the 176 golden and red documents: making the arm OVER-reject — fire whenever its cross is LIVE (either side names a keuzetype) instead of only when the two disagree — turns that golden, and only that golden, red (`WF-089 FAIL /resolvedRegels/0/waarde/list/type`), so over-rejection there is now pinned. The parity zone adds nothing here and says so with a number: its 30 documents enter this arm 18 times over 9 of them, not one of those references names a keuzetype, and the same over-rejecting mutant is silent across all 30 — the golden is the only witness in any zone. The `DimensieCel` arm still has **no positive witness**: no golden, red or parity document carries a keuzetype-typed `DimensionCellRead` — the two golden cell reads name no keuzetype domain, and the parity zone carries no cell read at all — so OVER-rejection in that one arm remains unpinned (the same over-rejecting mutant applied there leaves all 176 documents clean). That zero is a REACHED zero, not an unvisited arm: the cell arm is entered, and performs its comparison, at exactly the two golden cell reads — 2 comparisons over the 176 documents, against 46 comparisons over 31 documents at the `ParameterRef` arm (probe, re-measured 2026-09-05) — so the mutant had its two chances there and neither read names a keuzetype | every keuzetype golden — `jutterswet-vaarwatertoeslag-catala` + `jutterswet-keurstaat` (enumeration references), `anchor/jutterswet-beschikkingsom` + `anchor/jutterswet-aanspraaktelling` (sum references: attribute, list-typed attribute, arm/lambda/function binders), `jutterswet-meldplichtspoor` (the `ParameterRef` arm, crossed through the `ListType` element: an ordinary reference to a list parameter of declared-`SumType` elements, standing as a `Fold`'s list operand — not the fold-source arm) — and a measured zero over-rejection across every golden and red document and, read-only, the parity zone | `wf089-attribuutref-enum` + `wf089-attribuutref-sum` + `wf089-parameterref-enum` + `wf089-binderref-sum` (a `Let` binder) + `wf089-dimensiecel-enum` (verifier-only: the case format carries no cell input) + `wf089-attribuutref-lijst-sum` (the element-domain look-through: verifier-clean before, ref faulted, clingo folded 0 for 35.00 — a wrong answer) + `wf089-attribuutref-geen-domein` (one one-sided arm: typed number over an enumeration attribute — ref wrote the label into the numeric target, clingo faulted, Drools crashed at fire) + `wf089-attribuutref-geen-keuzetype-declaratie` (the mirror one-sided arm, which the other case does not pin: typed enumeration over a numeric attribute) + `wf089-attribuutref-navigatie-enum` (the navigated read, crossed at the endpoint attribute) + `wf089-enum-over-som` + `wf089-som-over-enum` (the variant half: one namespace, so the same name under the other variant is a lie — verifier-clean while both executors faulted in different words) + `wf089-aggregation-pass-through-enum` + `wf089-minimum-pass-through` (the aggregation position, both fold halves: with one contributor the fold makes no comparison and hands an alien label on) + `wf089-foldbron-enum` (the fold source, which the expression walk never entered) + `wf089-composite-domain` (fix round 2026-09-04: the COMPOSITE arm, a `plus` over two numbers annotated with a keuzetype domain — the one-sided arm at a derived position, which had a firing path since the derivation law landed and no artifact until this case. Mutant-proved: switching the whole derived cross off turns it clean together with the thirteen `wf091-*` derived cases and no others; switching the ADDITIVE arm off turns it clean with the four `wf091-binair-*` and no others; adding `domain` to that arm's skip list turns this case, and only this case of all 297 negatiefs, clean); at L2 (2026-09-24, `corpus/negative/README.md`, L2) the reference-arm witnesses were rewritten on the rule their translation falls on — `wf089-attribuutref-enum`, `wf089-attribuutref-navigatie-enum`, `wf089-attribuutref-sum`, `wf089-binderref-sum`, `wf089-dimensiecel-enum`, `wf089-parameterref-enum`, `wf089-attribuutref-geen-keuzetype-declaratie` on WF-063, `wf089-attribuutref-geen-domein` on WF-023, `wf089-attribuutref-lijst-sum` on WF-070, `wf089-enum-over-som` and `wf089-som-over-enum` on WF-036 — and `wf089-foldbron-enum` was removed with reason (its translation is `wf089-aggregation-pass-through-enum`); the aggregation and composite witnesses stay |
| WF-090 | **Since L5:** the element-passing aggregation is `first`/`last`/`minimum`/`maximum` and the computing folds are `sum` and `total`; the record below keeps the v0 names it was written in. **Since L2 (2026-09-24):** the reference positions of the read half left the wire with the reference's own `type` (B13); the element-passing aggregation, the composite positions and the write half remain (ch. 2.4). What follows records the rule as it stood until L2. struct-domain identity (ch. 2.4): a struct-typed use site names the same struct domain (ci, through a `ListType` element) as the declaration it crosses. The struct sibling of WF-089 + WF-023's domain clause, over the same positions and the same verifier machinery under its own rule number (maintainer decision 2026-09-03 — struct domains are not keuzetypes, so the ch. 2.4 family term is unchanged). READ half: `AttribuutRef` (at the endpoint attribute of a navigated read), the `DimensieCel` over one, `ParameterRef`, `BinderRef`, the fold source, the element-passing aggregation against each operand, and — since 2026-09-04, through WF-091's derived arms — a **composite** position, where this number carries the struct-domain facet against the DERIVATION exactly as WF-089 carries the keuzetype one. No artifact, and the reason is measured: the only two composites in the four zones that touch a struct type at all are `EERSTE` folds (`wf090-pass-through`, `wf090-foldbron`), the one position where the derived cross skips the domain facet by design because this rule's own per-operand aggregation arm already crosses it — so the composite arm is a firing path with zero occurrences here, and `wf089-composite-domain` pins the shared engine under WF-089's number. WRITE half: rule target, object-creation member, defaults consequence (recursing into exception groups). No arm where an existing rule crosses the FULL datatype (WF-049/067/068/070/073/074); the aggregation arm reaches the ELEMENT-PASSING folds only, so a computing fold (`SOM`, `TOTAAL`) over struct operands of two domains is verifier-clean — a WF-038 operand question and no wrong answer (measured: ref faults `plus: expected a number, got {waarde: 120.5, telling: 3}` and the ASP carrier refuses the form) — and the ORDERABILITY flank WF-089 records holds for structs too: `StructType` is equality-only, so a truthful `EERSTE` with two struct contributors is verifier-clean and faults observably (measured: ref `unordered comparison: {waarde: 20, telling: 2} vs {waarde: 10, telling: 1}`, clingo refuses the form) — also WF-038's, not this rule's; the unknown sentinel, an unresolvable source and an absent `type` defer | gate 3 (the six reference/aggregation arms share one entry point with WF-089, the write half rides `wf023_assignment` — each arm falsifiable on its own). Two honesty notes, measured. The **ci** comparison is pinned since fix round 1 by `jutterswet-dossierspelling`, the one golden whose struct use sites differ from the declaration in case alone, at BOTH halves at once: making the comparison case-sensitive in `wf090_struct_identity` and in the struct clamp of `wf023_assignment` turns that golden — and only that golden — red on two messages. Of the six read arms only `AttribuutRef` (50 struct-typed references) and `BinderRef` (9) have a POSITIVE witness, and each is witnessed BROADLY: a mutant that fires on any struct-typed use site at the `AttribuutRef` arm turns TEN documents red (eight goldens + `red/list-element-overprecision` + `red/struct-field-overprecision`), the same mutant at the `BinderRef` arm five (`anchor/jutterswet-vondstdossier`, `jutterswet-dossierrecht-catala`, `jutterswet-kamerhuishouden-catala` and the two red documents) — measured. The `ParameterRef`, `DimensieCel`, fold-source and aggregation arms carry no struct-typed use site anywhere in the corpus, so OVER-rejection in those four is unpinned, and a golden cannot be added today because the ASP carrier refuses the forms ahead of translation (measured: `TRANSLATION FAULT` on the faithful twins) | `anchor/jutterswet-vondstdossier` (the struct anchor and the richest witness of the two witnessed arms: nine struct-typed references and three `StructLiteral`s, all faithful, replayed by gates 5–6) + `jutterswet-dossierspelling` (the ci witness: both halves until L2, the write half since — re-measured 2026-09-25, a case-sensitive write half turns it and no other golden or red document red, while a case-sensitive read half leaves every one clean, no read position admitting a struct in a verifier-clean document) + `jutterswet-vangstwaardering` + `jutterswet-waarderingsinvoer` + `jutterswet-dossierrecht-catala` + `jutterswet-kamerhuishouden-catala` + `synth3-nestwaarde` + `synth3-vondststaat` (the further non-over-rejection witnesses) — and a measured zero over-rejection across every golden and red document: ten of the 171 carry a `StructType` at all, together 59 struct-typed references (`AttribuutRef` 50 + `BinderRef` 9) and 16 `StructLiteral`s. The parity zone is verified read-only too and is clean, but it confirms nothing HERE: measured, none of its 30 documents carries a single `StructType` | `wf090-write-rule-target` + `wf090-write-object-creation` (where the ASP side reported a full green pass on a document ref cannot evaluate) + `wf090-write-defaults` (the three write positions) + `wf090-attribuutref` + `wf090-attribuutref-lijst` (the `ListType` look-through, where clingo folded 0 for 12.50 — a wrong answer) + `wf090-binderref` (a lambda parameter, the same wrong answer) + `wf090-parameterref` + `wf090-dimensiecel` (verifier-only: the case format carries no cell input) + `wf090-foldbron` (the position the expression walk never enters) + `wf090-pass-through` (the fold node's own type against its truthful operand); at L2 (2026-09-24, `corpus/negative/README.md`, L2) `wf090-attribuutref`, `wf090-binderref`, `wf090-dimensiecel`, `wf090-parameterref` were rewritten on WF-074 and `wf090-attribuutref-lijst` on WF-070, and `wf090-foldbron` was removed with reason; `wf090-pass-through` and the three `wf090-schrijf-*` stay |
| WF-091 | **Since 2026-09-25: the wholeness of a product, a quotient and a power.** A `times`, `dividedBy` or `power` annotated whole is refused on the wholeness facet: ch. 2.3 makes each fractional whatever its operands are. The `times` arm claims it beside the unit; a quotient and a power carry an arm of their own that claims the wholeness alone, their unit staying ch. 2.10's fourth class and their decimals the ch. 2.4 under-report. Measured that day: 0 of the 131 such nodes in golden, red, negative and the parity zone was annotated whole, and the report of not one of those 793 documents moved. **Since 2026-09-25 (card ref-klipvouw-nul): the numeric operand.** Every operand of a `sum` or a `total`, in each reading — the listed sum, the set fold, the §7.1 per-element fold, the subject's own temporal total and its gedurende clip — and the base of the pro-rata part (`timeProportionalPart`) MUST be a `Number` (a `%` number included); any other declaration is ill-formed, reported at the operand, and there the row's identity cross does not run: over a non-numeric operand there is no derived type (ch. 4.1, "The operand of an arithmetic fold is a Number"). Until then the `sum` and `total` rows passed a `Date` operand's type through and a `Date` annotation agreed with it. **Since L5:** the positions and kinds carry the L5 spelling (`plus`, `times`, `dividedBy`, `sum`, `total`, `minimum`, `maximum`, `first`, `last`, `count`, `filledCount`; the sign values `unspecified` and `nonNegative`); the record below keeps the v0 names it was written in. **Since L3 (2026-09-24, B17 (i)):** a unit is compared on its L3 keys — `abbreviation`, `system`, `canonical`, `factorToCanonical`, `denominator` recursively — and the factor on `num` and `den`, which WF-092 (c) holds in lowest terms. **Since L2 (2026-09-24): derived-type identity.** The reference-type arms and the rule-target arm left the wire with the reference's own `type` (B13); the rule crosses a derived position's annotation against the type its operands derive — a reference operand typed by its declaration (ch. 4.1) — facet by facet in the order soort → domain → unit → decimals → wholeness, the three unit NAMES compared byte for byte (`wf091-plus-unit-case`, a `plus` over two `kg` reads annotated `KG`/`GEWICHT`/`KG`, factor 1). The sign and residual facets, which only the reference arm claimed, were removed from the verifier in the L2 fix round: every derived position excluded both, so no comparison reached them. What follows records the rule as it stood until L2. reference-type identity (ch. 4.1): a use site's serialized `type` is IDENTICAL to the declaration it takes its value from — not compatible, not convertible — up to the ci comparison WF-001 fixes for a declared DOMAIN name, the three UNIT names compared BYTE for byte, facet by facet in one fixed order: **soort → domain → unit → decimals → wholeness → sign → residual structure**, of which the FIRST differing facet is reported — one comparison, one message, never two facets at once; per COMPARISON, not per path, since a walk that crosses one path repeatedly (the aggregation's domain cross, once per operand) can report it more than once. The rule NUMBER follows the facet and the arm: the domain facet is WF-089/WF-090 at every arm and the soort facet is WF-078 at a `BinderRef`, each keeping its wording; every facet without a number is this rule's and names itself in the message — so a further facet costs a message, not a rule number. Two clauses of the order are load-bearing: the soort facet **stands aside** wherever either side names a keuzetype or a struct domain (one namespace — that discrepancy is WF-089's variant/one-sided arms and WF-090's, numbered there), and the residual facet is the **backstop** that makes the refusal total (the domain facet looks THROUGH a `ListType` element, so a list-typed declaration read as its element passes both earlier facets). **Unit identity is structural** — abbreviation, declaring system, canonical unit, factor, denominator recursively, the three NAMES compared BYTE for byte and the factor exactly (copy fidelity is not name resolution, WF-001's domain: a consumer that reads those three names literally to pick its conversion diverges silently on a case-only lie — measured, `ref` 3.75 against a third executor's 3750.00 on `wf091-attribuutref-eenheid-kast`) — never convertible: `kg` and `t` share a canonical unit and a shape, so a lie inside the convertible class is compatible by construction and WF-035/WF-023's unit cross cannot reach it. ARMS: the five REFERENCE use sites WF-089/WF-090 name (`AttributeReference` at the endpoint of a navigated read, the `DimensionCellRead` over one, `ParameterReference`, a binder reference, the fold source), plus a rule TARGET's own `type` — the lvalue is WF-022's and the expression walk never enters it (the omission WF-088's row records as measured harmless for a drifted enumeration copy, both executors writing through the DECLARED datatype; it is not harmless for a unit). Their sixth use site, the element-passing AGGREGATION against each operand, stays outside this rule and keeps the domain facet alone: a reference type is a COPY of one declaration, an aggregation `type` is a type DERIVED over an operand list (`MINIMUM`/`MAXIMUM` take the least operand precision, the conjunction of wholeness, and operand 0's sign and unit; `EERSTE`/`LAATSTE` take operand 0's type in full), so identity against every operand is unsatisfiable the moment two operands differ — as in ch. 4.6's blessed "het minimum van het gewicht en 0". The domain is the one facet a pass-through fold hands on unchanged, which is why WF-089/WF-090 can cross it against EVERY operand and no further facet can. Since 2026-09-04 those further facets are crossed against the DERIVATION instead — one comparison, not one per operand — by this rule's second half. The other two write positions NAME their target and carry no reference type of their own: what enters them is WF-023's and WF-090's assignment cross — compatibility, a different law. **DERIVED ARMS (2026-09-04, ch. 4.1 "Derived-type identity on composite positions"):** the same law, same facet order, same rule-number RULE, against a FORMULA instead of a declaration — a composite has no single declaration opposite it, and the expectation is computed from the operands' own serialized types by RUNTIME-SEMANTICS ch. 2.3 and ch. 2.6. "Same number" means the same number-follows-the-facet clause, not one number: the DOMAIN facet reports under WF-089/WF-090 at a composite exactly as it does at a reference (measured — a `PLUS` and a `Rounding` annotated with a keuzetype domain each report WF-089's one-sided arm, pinned since fix round by `wf089-composite-domain`), and only the facets without a number of their own are WF-091's here. Twelve positions (eleven until 2026-09-20), each claiming the facets its formula states and no more: additive `Binary` (maxDec, wholeness as conjunction, the LEFT unit, and a soort that FOLLOWS that unit — a `%` result, a rate whose numerator is `%` among them, is a percentage since the L3 fix round, the reading every loader applies to the annotation; until L3 the soort COLLAPSED to a number, `Percentage + Percentage` derived `number`, and that was the only annotation the position admitted; a date ± duration gives the date operand's type in full), `Binary` maal (the unit only — hand-through or rate cancel — with the soort following the unit as on the additive arm; until L3 the same collapse), `Rounding` (the base plus its OWN decimal count), `Clamp` and `Aggregation` som/eerste/laatste (a pass-through), `Aggregation` totaal (the pass-through with a rate's denominator stripped — a `%` rate's too, since the L3 fix round — wherever the fold INTEGRATES over time — the emitter's own `setLezing` predicate mirrored, so the non-temporal SET reading, an alias of the sum, passes the element type through instead), `Aggregation` minimum/maximum (least precision, conjunction, operand 0's sign and unit — over a percentage operand 0 too, since the L3 fix round), `Aggregation` aantal (whole, zero, unitless) and — since 2026-09-20 — `Aggregation` **aantal gevulde** (the same formula, on its own arm because the engine matches on the KIND: a filled count answers a cardinality whatever unit the folded attribute carries, `RUNTIME-SEMANTICS.md` ch. 3.4c). That twelfth arm closed a real hole rather than tidying a list: measured the day the kind landed, a filled count annotated `kg` and written into an equally-lying `kg` target was verifier-CLEAN while the identical lie under `AANTAL` was WF-091, and `ref` then played it — a unitless 1 landing in a kg-declared attribute, oracle PASS. A derived type that nothing crosses is an annotation that may say anything. A facet a formula does NOT claim is not crossed and takes the residual backstop with it, since the backstop would otherwise report exactly what the formula declined — and because the SIGN is claimed nowhere, that stand-down is total: no derived arm keeps the backstop, so the refusal there is exactly the row's named facets and, unlike the reference half, is not total by construction. The chain anchors only as far as the crossed operands reach: where every operand under a composite is itself crossed, the composite is bound to the declarations underneath it, but a FOURTH-CLASS operand breaks the chain — a `GEDEELD_DOOR` annotated `kg` over two unitless operands is a fabrication nothing crosses, and the law above it consumes it as ground truth up to a `kg` target, verifier-clean (measured on a probe; 26 such operand positions over 19 of the 174 golden+red documents of the measured 2026-09-04 state — one merge before `jutterswet-tempototaal` made it 175 — 23 of them under a division). The sentinel/empty door is shut (an untyped or sentinel-typed operand is WF-034-ill-formed) but the fabrication door is the stronger escape and stays OPEN: it does not switch the cross off, it feeds it a premise. What stays UNCROSSED is ch. 2.10's fourth class: a division, a power, a percentage-of, a root, an absolute value, the date/time extractors — v0 derives no result unit there; since 2026-09-25 a `dividedBy` and a `power` are crossed on their wholeness alone — plus a duration node (the annotation IS the source, WF-038), and `IfThenElse`/`Let`, whose whole type WF-069 and WF-076 already cross. That set and gate 13's exception set are TWO INDEPENDENTLY MAINTAINED LISTS, aligned by hand with no tripwire comparing them: they differ, measured, on `AANTAL` and, since 2026-09-20, on `AANTAL_GEVULDE` (both derived here, neither in gate 13, which twists units only — and a cardinality is unitless by construction, so no twist table would reach either), on the duration OPERAND axis gate 13 exempts and this rule has no notion of, on a `Let` gate 13 twists, and on `GEDEELD_DOOR_ABS`, which gate 13's prose omits and its set-complement covers. Outside: `TemporalRead`, `Invoke`, `Lambda` parameters, `StructLiteral` and `FieldAccess` already cross the FULL datatype (WF-049/068/070/073/074), so no node is crossed twice under two identity rules; `Construct` reads no declaration; WF-023's write-side PRECISION gate is a different question and stays OPEN. The unknown sentinel, an absent `type` and an unresolvable declaration defer | gate 3 (one entry point shared with WF-089/WF-090 plus the rule-target registration, and since 2026-09-04 a SECOND entry point beside the expression walk's type prologue for the derived arms — one function, one facet order, one rule number; every arm and every facet mutated and measured). Four honesty notes. (1) At the REFERENCE arms, which claim the whole datatype, the named facets decide the **diagnosis** and the residual facet decides the **refusal**: neutering any named facet degrades its case's message to `residual structure mismatch` while the document stays refused — only neutering the RESIDUAL facet lets a document PASS (`wf091-attribuutref-reststructuur`, measured). At the DERIVED arms that does not hold and cannot: every one of them claims less than the whole datatype and stands the backstop down with the sign, so neutering a named facet lets its case pass outright (measured in fix round: the unit facet turns all seven derived unit cases clean; eight since 2026-09-20, re-measured with the filled-count arm). What the facet order pins is therefore the NUMBER, and that is falsifiable: neutering the soort facet moves `wf078-binderref-type-lie` to WF-091, and neutering the engine's domain facet moves 18 of the 24 `wf089-*`/`wf090-*` cases to WF-091 — gate 3 red in both cases. The six that keep their own number are the ones that do not run through the engine: the three element-passing-aggregation cases, whose arm calls the domain cross DIRECTLY (it is not an arm of this rule), and the three `wf090-schrijf-*` cases, which ride the assignment cross. (2) The **ci** comparison of the DOMAIN names was pinned by exactly two goldens until L2: making the residual comparison case-sensitive turned `jutterswet-dossierspelling` and `jutterswet-vaarwaterspelling` — and only those two — red. The residual comparison left with the reference arms (B13). Re-measured 2026-09-25: `jutterswet-vaarwaterspelling` pins the ci of the domain identity at WF-023's write half and the ci lookup under WF-063 (WF-089's row); `jutterswet-dossierspelling` pins the ci lookup of a struct domain, a case-sensitive lookup turning it red under WF-072 and WF-073, and the ci of WF-090's write half, a case-sensitive write half turning it red under WF-090 (WF-090's row). The UNIT names are pinned the other way round and by a negative: folding the three of them ci (in `eenheid_identical` and in the residual comparison form) turns `wf091-attribuutref-eenheid-kast` — and only that case — clean, gate 3 red (measured, fix round 2). (3) POSITIVE-witness breadth per arm, measured with a mutant that fires on every unit-bearing use site at one arm, re-measured 2026-09-05 over the 176 golden+red documents: rule target **17** documents red, `AttribuutRef` **13**, `BinderRef` 2, fold source **3** (`jutterswet-vondsttelling` + `jutterswet-tempototaal` + `jutterswet-tempovouw`), and — since 2026-09-04 — `ParameterRef` 1 (`jutterswet-aanvoergeld`) and `DimensieCel` 1 (`jutterswet-seizoenvondst`), the two arms that carried **zero** until then. The rule-target and `AttribuutRef` numbers stood at 13 and 11 before that date and grew to 15 and 13 by exactly the two documents added for the new arms: `jutterswet-aanvoergeld` and `jutterswet-seizoenvondst` carry unit-bearing rule targets and attribute references too, so each counts in three lists at once — the breadth of the two established arms did not otherwise change by that step. What moved since is the CORPUS, not the rule: the two temporal goldens `jutterswet-tempototaal` and `jutterswet-tempovouw` each carry a unit-bearing rule target AND a unit-bearing fold source, which is the whole of the 15 → 17 and the 1 → 3 (same mutant, same day). Each of the two 2026-09-04 witnesses is TWO-SIDED, measured: the golden is clean under the unmutated engine, and a `t`-for-`kg` copy of that same arm in that same document (one canonical unit, factor 1000 — the convertible class no other rule reaches) is refused arm-exact (`unit mismatch: parameter reference 'aanvoerdrempel' …`, `unit mismatch: dimension-cell read of 'vondstgewicht' …`). The `DimensieCel` witness is necessarily VERIFIER-ONLY, and that too is measured, not assumed: a cell read is declined by every executor (`ref` `EVALUATION FAULT: expression kind not implemented in this card`, clingo `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`, the Drools backend `EXCLUDE dimensioned attribute input … is not carried by the v0 backend`) and the case format carries no per-cell input variant at all, so no case over that document could pin anything and gates 5–6 would turn red on one that tried. What closes at these two arms is the UNIT facet only; the DOMAIN facets split FOUR ways, and only three of the four are empty. Re-measured 2026-09-05 with an arm-and-class-limited over-rejecting mutant — fire whenever that one arm's cross is LIVE for that one class — over the 176 golden and red documents: a struct-typed `ParameterReference`, a keuzetype-typed `DimensionCellRead` and a struct-typed `DimensionCellRead` each leave all 176 clean, and all four pairings leave the 30 parity documents clean, so WF-090's note stands unchanged at both arms and WF-089's stands unchanged at its `DimensieCel` half. The fourth pairing does NOT: a keuzetype-typed `ParameterReference` turns exactly one of the 176 red — `jutterswet-meldplichtspoor`, through the `ListType` look-through — which is why WF-089's `ParameterRef` half is the one note that changed, in this same diff, and now carries that witness instead of claiming none. The parity zone carries 12 unit-bearing `ParameterRef` use sites over 6 documents and is verified read-only, but it is not a golden and pins nothing here. Both arms also carry a unit-facet negatief of their own (`wf091-parameterref-eenheid`, `wf091-dimensiecel-eenheid`, the latter since fix round 2, which completes the set: every arm this rule crosses in full has one) — a negative pins the REFUSAL and a golden the NON-over-rejection, and since 2026-09-04 both halves exist at every arm this rule crosses in full. (4) The DERIVED half's coverage, and what it still leaves open. It performs **228** comparisons over 102 of the 174 golden+red documents (the measured 2026-09-04 state; `jutterswet-tempototaal` landed one merge later and adds a 175th document with two TOTAAL comparisons — the strip witness in the negative column is measured against that 175 state; measured 2026-09-04 with a temporary probe in `composite_identity`, which the same walk reproduces per arm: additive `Binary` 114, maal 38, rounding 28, aantal 18, som 12, totaal 7, clamp 3, eerste 3, laatste 3, minimum 1, maximum 1), of which only **14** are unit-bearing, spread over 10 documents. So the UNIT facet has a golden witness at three arms only — additive `Binary` (8 documents), maal (3) and som (1) — and at the nine others (eight until the filled-count arm) it is pinned by a negative and by nothing else, which pins a REFUSAL and never a non-refusal. The maal arm's CANCEL rule has no corpus witness at all: no `Binary` maal node in any of the four zones carries two unit-bearing operands, so `wf091-times-unit` is its whole evidence. The counter-shape of note 4's own old example still cannot become a golden — the ASP backend refuses a multi-operand `MINIMUM`/`MAXIMUM` ahead of translation, so it can carry no pinned expectations — and that is exactly why the artifact that closes it is `wf091-fold-maximum-unit`. Three formulas stay OUTSIDE the law with their reason and their price, each measured the same day: the SIGN at ALL TWELVE derived positions without exception (it runs both ways — a derivation mostly under-claims it while a document narrows truthfully, the sharpest case being a clamp with a minimum of 0, which IS non-negative whatever its base says; and it over-claims at the aantal arm, where two anchor goldens widen `NIET_NEGATIEF` to `ONBEPAALD`. 12 documents over the three zones narrow it truthfully and 5 widen it; crossing it costs 5 golden+red documents on the additive arm alone, 7 at every arm, 27 with every exclusion emptied — the six arms corrected in fix round carry NO golden+red witness, so turning the sign off there left the verifier's report over all four zones byte-identical and the class is pinned by a probe, not by the corpus); the PRECISION of a maal, whose price needs both numbers because they are different measurements (against ch. 2.3's actual rule — the SUM of the operands' decimals, never whole — 9 decimals and 9 wholeness divergences over the four zones and **11** golden+red documents red, 3 on decimals and 8 on wholeness; against the PLACEHOLDER the implementation writes where the facets are unclaimed, zero decimals and fractional, merely un-skipping the two facets turns **20** red. 11 is the rule's price, 20 the placeholder's — this corpus carries three producers with three precision policies, the Catala plugin writing the pinned ch. 2.9 scale); and the pro-rata builtin plus the one-sided additive unit pair — of which only ONE ORDER is contested (a unitless LEFT beside a unit-bearing right, where ch. 2.6 gives no unit and both executor-side derivations give the right one; with the unit on the LEFT all four sources agree). The rule claims neither order: its guard is the pair's unit compatibility, and splitting it would pin the uncontested half — which carries zero golden+rood occurrences — while leaving the contested half open anyway. The one node the corpus does carry is in the contested order and annotates the RIGHT unit, against ch. 2.6 (`jutterswet-sleeprest`, `50 min <km>`); it is also a shape WF-035's adoption clause settles independently, the unitless side being a literal, so the genuinely open class is narrower still. Two facets of two arms have no artifact either way: a `Rounding` over a PERCENTAGE base (zero occurrences in four zones — as for the fall-through row, a rounding over a base that is neither), and the opaque PRODUCT of two plain units, whose algebra ch. 2.5 fixes but whose wire spelling it does not Since 2026-09-25 the numeric-operand arm runs first inside that second entry point and replaces the derived cross where it reports; mutant-proved per branch: switching its type test off lets all nine of its witnesses pass, removing `total`, `sum` or the pro-rata branch lets exactly the witnesses of that branch pass (3, 5 and 1), and giving the non-`Date` types the `Date` tail fails gate 3 on `wf091-set-source-sum-text` and `wf091-set-source-sum-list-type` alone, through their `expect.txt` (re-measured with the three later witnesses; before them six, 3, 2 and 1, and `wf091-set-source-sum-text` alone). | every unit-bearing golden — `jutterswet-vondsttelling` (the fold-source witness), `jutterswet-lichterloon` + `jutterswet-vondstrecht` (the two `BinderRef` witnesses), `jutterswet-meldloon` + `jutterswet-sleepbaan` + `jutterswet-sleeprest` + `jutterswet-stukopbrengst` + `jutterswet-vergunningsduur` + `jutterswet-vervaltermijn` + `jutterswet-vondsttarief` + `ww-herleving-inkomen` + `synth-grensrand` (the `AttribuutRef` arm) + `jutterswet-jutleeftijd` + `jutterswet-getijdenpaar` (further rule-target witnesses), plus `jutterswet-dossierspelling` + `jutterswet-vaarwaterspelling` (the two ci witnesses of honesty note 2), plus `jutterswet-aanvoergeld` (the `ParameterRef` arm, two unit-bearing parameters in two unit systems, read in a condition and in a value) + `jutterswet-seizoenvondst` (the `DimensieCel` arm, verifier-only — see honesty note 3) — and a measured zero over-rejection at every facet across every golden and red document. What that measurement counts, stated exactly, because two earlier counts of it disagreed: **929 comparisons the engine PERFORMED** over the 174 documents of that same dated state (834 golden + 95 rood), **46 of them unit-bearing** — one per arm entry that reaches the facet chain, tallied by a temporary per-arm probe over every `ir.json` in both zones (re-measured 2026-09-04 with the same probe, which reproduces the previous 921/38/71/14 exactly on the 172 documents of the previous count). It excludes 72 further invocations that DEFER before comparing anything (an absent `type`, the v0 unknown sentinel, an unresolvable declaration), and it excludes the element-passing aggregation's domain-only cross (14 more comparisons, none unit-bearing), which is no longer an arm of this rule. It also counts the REFERENCE half only: the derived arms perform a further 228 comparisons over the same two zones, tallied and split per position in honesty note 4. Same count in the parity zone, read-only: 147 performed comparisons over 30 documents, 30 of them unit-bearing, plus 5 aggregation comparisons — that last number re-measured in fix round 2 and stated with the distinction two earlier counts blurred: the aggregation site is ENTERED **6** times there, and **5** of those entries perform a comparison, one deferring before it. In golden+rood the two numbers coincide at 14, which is why only the parity zone ever showed the disagreement; for the numeric operand, the non-over-rejection: the 35 goldens that read a `sum`, `total` or pro-rata base (47 operand positions, all Numbers, one of them a `%` number, measured 2026-09-25), beside `jutterswet-vondstbereik` and `jutterswet-vondstvenster`, the ordering folds over a date source, which the arm does not read; the `%` clause's own witness is `jutterswet-aandelensom`, a set sum over a percentage attribute, which a mutant narrowing the arm to plain numbers turns red and no other document; the wholeness claim of 2026-09-25 has its non-refusal in every golden that carries a `times`, `dividedBy` or `power` (101 such nodes, all fractional) | `wf091-attribuutref-eenheid-converteerbaar` (the rood-eerst case: the same canonical unit, factor 1000 — the class WF-035's compatibility cross admits by construction) + `wf091-attribuutref-eenheid-ontbreekt` + `wf091-attribuutref-eenheid-erbij` (the two one-sided unit arms) + `wf091-attribuutref-decimalen` + `wf091-attribuutref-geheel` (the two precision forms WF-007 admits that share a decimal count) + `wf091-attribuutref-teken` (a WIDENED sign restriction, which WF-023's contradiction clause cannot see) + `wf091-attribuutref-soort` + `wf091-attribuutref-reststructuur` (a list-typed declaration read as its element: the hole the facet order makes by design) + `wf091-binderref-eenheid` (the arm where WF-078 keeps the soort) + `wf091-parameterref-eenheid` (the arm whose golden witness, `jutterswet-aanvoergeld`, arrived only on 2026-09-04 — see honesty note 3) + `wf091-foldbron-eenheid` (the fold source, which the expression walk never enters) + `wf091-schrijfdoel-eenheid` (the rule target) + `wf091-attribuutref-eenheid-kast` (the case-only unit lie, which pins the BYTE comparison of the three unit names — see honesty note 2) + `wf091-dimensiecel-eenheid` (the cell read's own type, the last arm without a unit case) — and, for the DERIVED half, fourteen more, one per (position, facet): `wf091-binary-unit` (the rood-eerst case, the 550.000 shape: `kg` derived, `t` annotated) + `wf091-fold-maximum-unit` (honesty note 4's own counter-shape, a `MAXIMUM` over `kg` annotated `t` — 3,75 against 3.750.000, and the negative exists because a golden cannot) + `wf091-binary-decimals` (the maxDec rule, under-reported — the direction that walks past the ch. 2.4 assignment gate) + `wf091-binary-whole` (the conjunction rule, both sides 0 decimals so WF-007's form check passes either way) + `wf091-binary-kind` (a plain sum annotated a boolean since L2; until then a percentage) + `wf091-times-unit` (the ch. 2.6 rate CANCEL, the one arm with no corpus witness at all) + `wf091-rounding-decimals` (a rounding claiming not to have rounded) + `wf091-rounding-unit` + `wf091-clamp-unit` + `wf091-sum-unit` (the fold ROOT, where `wf091-foldbron-eenheid` lies at the SOURCE) + `wf091-total-unit` (the §4.20 denominator strip, which it reaches by carrying `set: false` on both its operands; removing the strip turns exactly this case green, and also turns `jutterswet-tempototaal`'s temporal node red — while making the strip UNCONDITIONAL, the shape before fix round 3, turns that same golden's non-temporal set fold red. The strip is therefore two-sided; the granularity half of the emitter predicate that guards it is not, and says so at ch. 4.1) + `wf091-fold-decimals` (the least-operand-precision formula: its operands stand higher-precision-first so that taking operand 0's precision instead turns exactly this case green) + `wf091-count-decimals` + `wf091-filled-count-unit` (the twelfth arm's own case, and the rood-eerst witness for it: with the arm absent — the binary byte-identical to the one that shipped the kind — it is verifier-PASS, and the arm is the only difference). Mutant-proved per (facet, arm), re-measured in fix round after the sign exclusion became total: neutering the unit facet turns all **8** unit cases clean (7 until 2026-09-20; re-measured that day, the eighth being `wf091-filled-count-unit`) (before that round it turned 3 clean and degraded 4 to `residual structure mismatch`, the split the claims-every-facet column then made — no derived arm keeps the backstop now, so the degradation is gone); neutering decimals, wholeness or soort turns 2, 1 and 1 clean; disabling one position turns that position's cases clean and no others; disabling the derived cross entirely turns all fourteen clean — fifteen with `wf089-composite-domain` — and leaves every reference-arm case red; at L2 (2026-09-24, `corpus/negative/README.md`, L2) removed with reason `wf091-attribuutref-decimalen`, `wf091-attribuutref-eenheid-converteerbaar`, `wf091-attribuutref-eenheid-kast`, `wf091-attribuutref-geheel`, `wf091-attribuutref-teken`, `wf091-dimensiecel-eenheid`, `wf091-foldbron-eenheid`, `wf091-parameterref-eenheid`, `wf091-schrijfdoel-eenheid`; rewritten on WF-023 `wf091-attribuutref-eenheid-erbij`, `wf091-attribuutref-eenheid-ontbreekt`, `wf091-attribuutref-reststructuur`, `wf091-attribuutref-soort`, and on WF-076 `wf091-binderref-eenheid`; the derived-arm witnesses stay (`wf091-binary-kind` rewritten to a boolean annotation); added in the L2 fix round `wf091-plus-unit-case` (the byte comparison of the unit names, now at a derived position) — and, for the numeric operand (2026-09-25), `wf091-timeline-total-date` (the subject's own temporal total) + `wf091-clip-fold-date` (its gedurende clip) + `wf091-pro-rata-date-base` (the pro-rata base) + `wf091-set-source-total-date` + `wf091-set-source-sum-date` (the set fold under each additive kind) — the five red cases of the cards temporele-datumvouw and datum-setbron-breuk, moved when the arm made them ill-formed — and `wf091-set-source-sum-text` (the tail of every type but a `Date`), and, the same day, `wf091-set-source-sum-list-type` (the tail over a `ListType`, whose element the arm does not look through), `wf091-sum-date-parameters` (a listed sum of two `Date` parameters: the parameter name, one report per operand) and `wf091-sum-date-literals` (a listed sum of two date literals: the positional name) + since 2026-09-25, the wholeness of a product, a quotient and a power: `wf091-times-whole`, `wf091-division-whole`, `wf091-power-whole` (two whole operands annotated whole; each verifier-clean before the claim, each freed by removing its own arm and no other) |
| WF-092 | **Since L3 (2026-09-24, B17 (i)):** the conversion pair is `canonical`/`factorToCanonical`, present together or absent together — (a) a canonical without a factor, (b) a factor without a canonical — and (c) the factor, the exact rational `{num, den}`, is in lowest terms; the declared units stand in `/model/unitSystems/*/units/*`, one list, spellings on the unit. Where the text below spells the pair `canoniek`/`factorNaarCanoniek` and the half "null", it records the v0 spelling it was measured on, and where it has WF-009 test or own a factor's positivity, it records that rule until L3, when positivity became the factor's wire form. **Since L2 (2026-09-24):** the rule target and the references carry no `type` of their own (B13), so the call sites on the target's `type` and on reference types left; the unit systems, the declarations and every annotation a value node carries of its own remain. unit wire form (ch. 2.5): a unit spells its CONVERSION PAIR whole — `canoniek` and `factorNaarCanoniek` filled together or null together, on the numerator and, recursively, on the `noemer` of a rate. The BARE form (both null) stays admissible and is not a dead branch: 28 occurrences over the golden corpus (the WF-038 duration parameters, the WF-035 duration operands), and making the executor's bare branch fault turns 9 corpus cases over 3 goldens red. Either HALF is ill-formed — a canonical target with no factor states a conversion it cannot perform, a factor with no canonical target names no conversion at all. It is a SHAPE law on ONE unit, not a cross between two, which is what no neighbouring rule can supply: WF-009 tests a factor it FINDS and a chain it can FOLLOW, so a missing half is nothing to look at; WF-035/WF-023 cross units for CONVERTIBILITY, which reads the canonical names and never the factor, and reads a missing canonical as the ABBREVIATION — so the half-form is compatible with its whole sibling by construction wherever those two coincide, and refused for the WRONG reason wherever they do not (measured on `km`: `WF-023 assignment across incompatible units` plus `WF-035 additive operands have incompatible units`, which is why the factor-half case sits on the SELF-CANONICAL `m`); and WF-091 compares byte for byte, agreeing the moment the half-form is copied consistently across the arms it crosses. Because it is a shape law it holds at every position a unit can occupy — the declared systems (`/model/unitSystems/*/{eenheden,aliassen}/*`), the model layer (attributes, domains, parameters, `functionDefs` parameters and result), every walked expression `type`, a FOLD SOURCE's own `type` and an inline LAMBDA's parameter datatypes (the two positions the expression walk does not reach: the fold-source branch replaces that walk, and a lambda's parameters are declared where only its body is walked), and a rule TARGET's own `type`, the position the expression walk never enters either — and a document carrying the shape is refused ONCE PER POSITION, which is what makes the position set falsifiable at all. That totality is measured, not asserted: an injection sweep that puts a half-filled unit at each of the goldens' **2161** `Numeriek`/`Percentage` positions in turn and demands a WF-092 citation reports **0 unreached** (it reported 85 before the fold-source and lambda-parameter call sites landed — 27 and 58 respectively). The unit extraction reaches strictly FURTHER into a datatype than WF-007's own form check: it recurses through a `ListType` element, a `StructType` field and a `SumType` payload, three of the doors WF-007's row records as out of its reach — measured, no unit sits behind one of them in any zone today, so that recursion carries no corpus witness and is pinned by the injection sweep alone, which does reach them through the lambda-parameter site. MEASURED DRACHT ZERO at the moment it lands: over the 476 `ir.json` of the four zones (153 golden, 22 red, 271 negative, 30 parity) not one half-filled unit exists outside the three cases this rule brings — golden 213 units, 28 bare / 185 whole, 11 of them denominators; red carries NO unit at all; the parity zone 1510 units, every one whole, no denominator; 34 distinct unit forms over golden+red+parity. The rule therefore refuses a shape nothing in the corpus writes, and its evidence of non-over-rejection is the corpus itself. What it does NOT say: nothing about a factor whose VALUE is wrong (WF-009 owns positivity, and a wrong-but-positive factor is unreachable by any rule), nothing about the bare form's meaning, and nothing at a use site — the identity cross stays WF-091's | gate 3 (one shape function `check_eenheid_form` with its denominator descent, one datatype extraction, six call sites — since 2026-09-05 that same function also carries WF-034's `stelsel` back-reference arm (ch. 4.6 (r)), one walk serving two rules, and every branch measurement below is of WF-092's OWN branches; every branch and every call site mutated and swept byte for byte over all 476 documents of the four zones, and the position set additionally pinned by the 2161-position injection sweep). Four honesty notes. (1) BRANCH-exactness, measured: neutering the canonical-without-factor branch turns `wf092-canonical-without-factor` AND `wf092-denominator-without-factor` clean (they share that branch) and nothing else; neutering the factor-without-canonical branch turns `wf092-factor-without-canonical` clean, alone; removing the denominator descent turns `wf092-denominator-without-factor` clean, alone. Every one of the three mutants leaves every OTHER document in the four zones byte-identical — zero over-rejection and zero under-rejection elsewhere. The third mutant takes TWO edits, not one: dropping the recursive call also makes the `rec` flag unused, which the build treats as an error (warning 39), so an edit that removes only the call fails to build and leaves a sweep quietly re-running the previous binary — a green that means nothing. (2) POSITION-exactness, measured the same way and weaker BY DESIGN: no call site can be pinned by a case turning clean, because each case is refused at several positions at once. What each mutant loses is CITATIONS, and the six sites partition the 19 citations exactly: `unitSystems` **2**, model layer **6**, walked expression `type`s **5**, fold sources **3**, rule targets **3** — and the lambda-parameter site **0**. That zero is the honest one: no document in any zone carries a unit on a lambda parameter, so that call site loses no citation when it is removed and its whole evidence is the injection sweep (58 of the 85 positions it opened). *Since 2026-09-25* that holds of golden and red only: `wf092-lambda-parameter-unit` carries a unit on a lambda parameter, and five positions now have a case that leaves WF-092 when the walk into the position is removed (the negative column). A position whose citations vanish without any case turning red is exactly what a per-position negative could not fix — the shape law is total by construction, so redundancy is the design, not a gap. Three RESIDUAL positions stay outside the totality and are named rather than papered over: a fold source whose `subjectType` or whose `attribute` is absent, and a Lambda in a position where no lambda is admissible. Each sits behind an arm that has already REFUSED the node — WF-034 twice, WF-069 once — so the document is rejected, but rejected without a WF-092 citation at that unit. Measured over the four zones: the three negatiefs that reach those arms are `wf034-fold-source-without-object-type` and `wf034-fold-source-without-attribute` (8 units each, every one whole) and `wf069-lambda-outside-fold` (no unit at all), so extending the rule into them would add a branch no document in any zone could witness — the honest note is the artifact here, not the code. (3) POSITIVE-witness breadth per position, measured with a probe that fires WF-092 on EVERY unit at one position: declared systems **15** goldens, model layer **20**, walked expression types **19**, rule targets **16**, fold sources **2**, lambda parameters **0**, out of the 22 unit-bearing goldens; red scores **0** at all six, because it carries no unit anywhere, so the red zone witnesses nothing here and says so. (4) The EXECUTOR asymmetry the rule answers to, measured on the two plain-unit cases against the verifier that did not yet carry it (it reported `verifier: PASS` on all three): on the canonical-without-factor half, `ref` and the ASP backend both read the missing factor as 1, compute `2,5 + 300 = 302,5` where the restatement should have given `2,8`, and report a DECIMALS complaint about a number whose unit was already wrong, while the third executor names the missing factor; on the factor-without-canonical half nothing faulted anywhere and the three executors returned TWO answers — 2800, 2800 and 302.5 — a wrong value from a document every rule admitted. Both halves are now answered in the executors, so the second line of defence is not a dead letter on either: `ref`'s factor question faults by name on BOTH (measured on the same case files, `EVALUATION FAULT: unit m carries conversion factor 1 but names no canonical unit to convert into` where it used to replay a clean 2800; the sentence carried a `(WF-092)` tail until 2026-09-25), and the ASP backend inherits both refusals unchanged through the same function, as `TRANSLATION FAULT` (its own translator carries no factor reading — its unit fence compares whole unit records). The third executor still computes 302.5 on the factor-without-canonical half: there the missing canonical makes its convertibility predicate answer "not convertible", which its ungated predicate callers read as "no restatement needed" rather than as a refusal. That is a backend question on a shape no corpus document carries and this rule now refuses before any executor sees it | every unit-bearing golden — **22** of the 153, each a witness that the rule does not over-reject, at the six positions with the breadth honesty note 3 measures (15 / 20 / 19 / 16 / 2 / 0). Named for the two shapes the negatiefs copy: `jutterswet-sleepbaan` (the factor≠1 conversion the two plain-unit cases break, `km → m ×1000`) and `jutterswet-tempototaal` (the only golden in the corpus carrying a rate unit at all, hence the only possible base for the denominator case). The three further factor≠1 goldens `jutterswet-berging` (`t → kg ×1000`), `jutterswet-sleeprest` (`km → m ×1000`) and `jutterswet-vergunningsduur` (`jaar → maand ×12`) witness the executor half: they are the corpus's whole stock of genuine conversions, and gates 5 and 6 replay all 403 cases unchanged with the executor fail-fast in place. The parity zone, read-only, carries 1510 whole units over 30 documents and pins nothing here | `wf092-canonical-without-factor` (the rood-eerst case: `km` naming canonical `m` with `factorNaarCanoniek: null`, verifier-clean before the rule and carrying the measured three-executor split of honesty note 4) + `wf092-factor-without-canonical` (the mirror half on the SELF-CANONICAL `m`, the one stand in which no neighbouring rule masks it — the sharper case, since before the rule NOTHING faulted and the three executors disagreed in silence; two of the three now refuse it by name, the third is honesty note 4's open backend question) + `wf092-denominator-without-factor` (the denominator, the recursion's whole evidence: without it the descent is unfalsifiable, since both plain-unit cases are refused at identical positions with and without it. Necessarily VERIFIER-ONLY and measured so: its base golden is one of the three gate 5 lists as not replayed, and it is the only golden with a denominator, so no case over it could pin an executor value) + `wf092-factor-not-in-lowest-terms` (arm (c), since L3: a factor `{24, 2}`) + since 2026-09-25, one case per position whose cited pointer was repaired that day, each the only WF-092 position of its document: `wf092-struct-field-unit` (a struct field's datatype, in a struct domain no rule reads) + `wf092-constructor-payload-unit` (a constructor payload, in a sum-type domain no rule reads) + `wf092-parameter-datatype-unit` (a parameter's datatype, arm (c)) + `wf092-function-parameter-unit` (a function definition's parameter, in a function no rule invokes) + `wf092-lambda-parameter-unit` (a lambda parameter, with the WF-070 that position brings: the datatype the fold hands the parameter is itself a WF-092 position). Removing the walk into each position takes its case, and no other of the 438, out of WF-092 (measured 2026-09-25). Gate 3 compares the rule and the arm and, since its check 4 of the same day, resolves every cited pointer in its document; each NOTE records the cited pointer |
| WF-093 | two-sided since L2 (2026-09-24, B11, B13): the COUNT source — the set source of a single-argument aggregation whose `attribute` names no member of the element type — is recognised at the source itself; (a) a count source MUST be the source of `count` (under a value fold a role of the fold subject is ill-formed), (b) `count` MUST read a count source (a member read under a count is ill-formed). Because the recognition reads the name alone, a role whose written name is also a member of the element type cannot be counted under that spelling (ch. 2.6). A fold of two or more arguments is out of scope. Until L2 the rule keyed on the unknown-type sentinel on the source's own `type` (maintainer decision 2026-09-06) and had direction (a) only; (b) was WF-096's count half (ch. 4.3, history) | gate 3 | `jutterswet-vondsttelling` (a count source under `count` beside a set-source `sum` in one document — the pair this rule separates); `jutterswet-aanmeldertelling` (the count source read on its subject) | (a): `wf093-wrat-fold-bare-sum`, `wf093-wrat-fold-bare-first`, `wf093-filled-count-wrat`, `wf093-count-source-under-sum` (at L2 from WF-034); (b): `wf093-count-over-attribute-source` (at L2 from `wf096-aantal-setbron`); `wf093-wratvouw-set-som` and `wf093-wratvouw-set-maximum` were removed with reason at L2 |
| WF-094 | removed at M1 (2026-09-23): the map key is gone and the node carries its one name, so a self-name has no key to disagree with (ch. 2.1, history; delta B2). Clause (d)'s null form cannot be written since M1 either: at M1 the schema typed `naam` on `Parameter`, `Eenheidstelsel`, `Feittype` and `Dimensie` as a non-null string — since L3 `Parameter` and `UnitSystem` carry a required `name` instead, and since L5 `FactType` and `Dimension` do too — and `ObjectType.name` is a required string | — | — | the ten witnesses `wf094-zelfnaam-<kind>` and `wf094-zelfnaam-null-<kind>` were removed with reason (`corpus/negative/README.md`) |
| WF-095 | the string of a literal's `dateTime` variant (keyed on the variant since L4; a millisecond-precision `Date` names it, WF-114; `DatumTijd` until L3) is the canonical ISO `yyyy-mm-ddThh:mm:ss[.fff]`, matched whole (ch. 4.1) — fixed field widths, `T` never a space, the seconds field always written, no zone suffix and no offset, a fraction that is optional but exactly three digits, the date half calendar-valid. **Narrowed 2026-09-22 (card `p5-datumtijd-lexeem`):** until then the rule also admitted the day-first source snippet `[dd. ]d-m-yyyy hh:mm:ss[.SSS]`, because the repository's own emitter wrote it into the lexeme AND `bron` (measured 2026-09-06); the emitter now writes ISO into the `dateTime` variant of `value` (`waarde` until L4) and keeps the snippet in `source` (`bron` until L5), as WF-084's date branch does, and the snippet is refused on the wire. The narrowing moved no document — the snippet had no carrier in any zone, the five negatiefs of the time each carrying a non-snippet defect — and the readers kept reading the snippet behind the fence until 2026-09-23, when they narrowed with the wire and refused it, each with a sentence of its own (card `datumtijd-lezers-versmalling`); since the L4 landing's fix round every executor refuses it at load with this rule's own sentence, the shared OCaml loader in its executor stance and the Drools loader alike. Everything else is refused — precisely the forms every reader already refused, and the wire refused none of them: `rommel 2024-01-31 12:00:00Z x` was schema-valid and verifier-clean (measured 2026-09-06). It is the wire half of a three-reader fence closed on the same day: the shared interpreter/ASP case reader read `.1` as ONE millisecond and `.100000` as a hundred THOUSAND where the Drools case reader read a hundred and the literal reader refused, and gate 4's schema pattern was the ONLY reason that never showed. Boundary counted 2026-09-06 over the four zones (539 documents): the only WELL-FORMED `DatumTijd` literals anywhere are this rule's own golden's two, beside the five its negatiefs carry — the rule finds no document predating it, so nothing of the corpus moves. The 30 `dateTime` values on the CASE wire (25 public, 5 in the parity zone) were all already canonical, so the readers' own fences move none of them either. **The empty half, closed at L4:** since L4 a literal's `value` is a required variant, so a null cannot be written and the loaders refuse it. *History:* `waarde: null` was the empty half of the same field and a different question (the (n)/(o) precedent of ch. 4.6), left open with its shape stated rather than for want of a witness: the schema typed `waarde` as `["string", "null"]`, so a null LEXEME was schema-valid where a MISSING `waarde` was refused by gate 2, and this rule's arm read the present lexeme only. Measured 2026-09-06 on a one-edit twist of this rule's own golden: `verifier: PASS`, while the executors split — `ref` and the ASP backend both fault BY NAME (`missing Literal.waarde`) and the bean-model executor DEREFERENCES it (`NullPointerException: Cannot invoke "java.lang.CharSequence.length()" because "this.text" is null` — the shared datum-tijd reader matched its patterns against the handed value with no null guard). Two named refusals and one dereference under a document the wire admitted: the same three-reader shape this rule closes for the non-null half | gate 3 | `jutterswet-meldstip` (a canonical-lexeme literal ordered against a millisecond-precision attribute at a sub-day threshold, replayed by gates 5, 6 and 12 — the first millisecond-precision coverage of the differential); `jutterswet-sluitstip` (since 2026-09-22: the first `source.rgs` golden with a millisecond-precision literal — three source spellings of one instant emitted as one ISO lexeme, in a write and in both orderings, re-emitted by gate 10 and replayed by gates 5, 6 and 12) | `wf095-zone-suffix`, `wf095-space-separator`, `wf095-fraction-width`, `wf095-missing-seconds`, `wf095-surrounding-text`, `wf095-source-snippet` (the snippet the rule admitted until 2026-09-22 — verifier-clean under the two-spelling arm, so it is the witness that fails if the snippet is ever readmitted) |
| WF-096 | removed at L2 (2026-09-24): the set flag left the wire (B11); the set source is the single argument of an instance fold that carries a range (ch. 4.3), so no other position can state a collection. What the rule refused at an ordinary read is WF-053's to-many clause since L2, and what it refused under `count` is WF-093 (b) (ch. 4.3, history) | — | — | at L2 (`corpus/negative/README.md`, L2) `wf096-setlezing-regelwaarde` was rewritten on WF-053 (`wf053-to-many-rule-value`), `wf096-setlezing-operand` and `wf096-setlezing-geleste-operand` replaced by `wf053-to-many-plusoperand` and `wf053-to-many-list-fold-operand`, `wf096-aantal-setbron` by `wf093-count-over-attribute-source`; `wf096-aantal-setbron-bronpad` and `wf096-aantal-setbron-wrat` were removed with reason |
| WF-097 | a `gedurende` clip member carries NEITHER an owner NOR a whole-period qualifier (ch. 4.5, maintainer decision 2026-09-09; widened to all three clips 2026-09-10): a CharacteristicCheck standing as the condition of a clip `filter` — directly or as a member of the flat `all` composite — is refused a `host` and refused a `wholePeriod`, the NEGATED qualifier included. The rule names its three clips as a CLOSED list: the temporal total (`total` over an own temporal attribute, WF-055, ch. 3.6), the pro-rata fold (`timeProportionalPart`, WF-056, ch. 3.7) and the day count (`dayCount`, WF-058, ch. 3.8); the FOURTH reading of the same field, the die/dat instance subselection, is NOT a clip and is deliberately not fenced — there an owner and a qualifier are ordinary condition fields. Two arms, one reason: a clip reads THE SUBJECT's own characteristic periods over the WHOLE span it clips, so an owner names a subject no reader consults and a whole-period demand re-states the span the clip already ranges over. Every reader matched both fields away with `_` and none said so. Measured on the pre-rule binaries, one field twisted at a time: 3 twists at the total (`jutterswet-tegenwacht`) verifier-clean and unchanged in 36 of 36 replays, plus 6 twists at the other two clips (`anchor/jutterswet-borgdeel`, `anchor/jutterswet-dagentelling`) verifier-clean and unchanged in 60 of 60 — 96 replays, 96 unchanged answers, `negated: true` indistinguishable from absent throughout, which is why both polarities are refused. Each of the three artifacts implements the fence ONCE, in a single shared helper reached from TWO OR THREE call sites depending on how that artifact already discriminates the clips (the verifier needs two — its two folds are one kind-keyed branch — while ref and the ASP backend carry three): what the rule requires is the SHARED helper, not a call count, and it is required rather than noted because the rule first landed on the total alone and the other two clips answered silently for a day. The two executors fault with the same words but NOT at the same time (ref lazy, ASP eager): behind a false rule condition ref replays clean while the ASP backend refuses at translation — measured, and measured to be INHERITED from WF-055, which splits identically; bounded by the verifier rejecting any such document, so no gated zone can hold one. **Scope is the clip member.** The SCALAR check outside a clip keeps its `host` (name resolution, WF-034 (q)) and keeps its qualifier (the frontend produces that form from source — a producer-cutting decision with its own card); the qualifier on a **Comparison** is implemented, carried by 5 corpus nodes before the rule and 5 after, and untouched, a widened WF-043 being over-rejection. WF-043's own characteristic arm cannot reach inside a clip: it fires only on a NON-time-dependent characteristic and WF-055 already requires a time-dependent one there — that unreachability is what left the position unguarded. Cost: none — counted over the four zones before the rule (534 documents), 39 clip filters across 25 documents hold 37 characteristic checks, 0 with an owner and 0 with a qualifier; corpus-wide 193 characteristic checks, 3 with an owner (all outside a clip, all in the negative zone), 0 with a qualifier | gate 3 | `jutterswet-tegenwacht` (total), `anchor/jutterswet-borgdeel` (pro rata), `anchor/jutterswet-dagentelling` (day count) — the three sources the witnesses are broken from, each staying clean and replayed by gates 5, 6 and 12 | `wf097-clip-characteristic-host` (composite member), `wf097-clip-characteristic-period` and `wf097-clip-characteristic-period-negated` (plain member, both polarities), `wf097-prorata-characteristic-host`, `wf097-prorata-characteristic-period`, `wf097-day-count-characteristic-host`, `wf097-day-count-characteristic-period` — one per clip per arm, so a dropped ARM frees its three or four and a dropped CLIP SITE frees exactly that clip's |
| WF-098 | the context-value name is a CLOSED SET of two, spelled byte for byte (ch. 4.1, maintainer decision 2026-09-10; two since L4, 2026-09-24, delta B23): a `ContextValue`'s `name` is `evaluationDate` or `evaluationYear`, and nothing else — not a third name, not a recapitalisation of one of the two, not their v0 spellings `Rekendatum` and `Rekenjaar` (outside the set since L5), and not `de dag`, the bound day of a day-kind definition, which is the `BoundDay` node since L4. Every executor refuses the same names at load with the verifier's sentence. **Type arm (2026-09-26):** a context value with an admissible name carries the type its name fixes — `evaluationDate` a day-precision `Date`, `evaluationYear` a `Number` with `whole: true`, `decimals: 0`, the sign unspecified and no `unit` — because nothing in the document declares it; the arm is the verifier's alone, since every executor derives the value from the name and reads the annotation nowhere (measured before the arm on `jutterswet-rekenjaar`, one position at a time: a year annotated with two decimals passed the verifier as the rule value and as either comparison operand and was caught as the subtraction operand by WF-091 alone, a `Date` annotation was caught at each position by that position's rule, WF-023, WF-035 or WF-036, and at every position all three executors, both Drools branches included, answered the year). It is an ENUMERATION and not a resolution, which is why it carries its own number rather than an arm of WF-034: no table in the document declares a context value, so what has to be written is the set itself, and writing it decides what v1 admits at this node. *History:* the set held THREE until L4 because the wire did: measured 2026-09-10 over the four zones plus the parity zone (578 documents), 14 `Constante` nodes over 6 documents in exactly two spellings — `de dag` (11, in 3 documents) and `Rekendatum` (3, in 3 documents) — while the ch. 4.1 row named `Rekendatum` and `Rekenjaar` only, so the commonest name on the wire was one the row did not name. Mirror-then-diverge settles that in the wire's favour; what `de dag` IS stays ch. 3.7's bound day of a day-kind definition, spelled as a context value because v0 has no node kind for it, and whether v1 gives it one was left open until L4 gave it `BoundDay`. `evaluationYear` (v0 `Rekenjaar`) stayed in the set with no carrier at all from L4 until 2026-09-26, when every executor came to carry it as the calendar year of the evaluation date (`RUNTIME-SEMANTICS.md` ch. 8.2) and the golden `jutterswet-rekenjaar` became its carrier. **Byte-exact is the decision the rule carries.** The consumers disagree about case, and one disagrees with itself: `ref` and the ASP backend compare a NORMALISED name, the Drools constant reader compares with `equals` and its day-kind reader with `equalsIgnoreCase`. Measured 2026-09-10, `Rekendatum` -> `rekendatum` on `anchor/jutterswet-penningvintage`: verifier PASS, `ref` PASS (2 expectations), ASP PASS (2) — three consumers accepting what the fourth would not recognise. No differential can pin it, and that is structural rather than a gap: of the three carriers (`Rekendatum` then, `evaluationDate` since L5) the anchor golden is excluded by the Drools loader for an unrelated reason and the other two are red documents, so the only carrier Drools could play would be one built for the purpose, whose whole content is the defect. The rule closes the WIRE instead — a document carrying `rekendatum` never reaches an executor, which makes the normalisation inside `ref` and the ASP backend unreachable on any conforming document and leaves all three executors untouched by this card. Residue, stated rather than hidden: a HAND-BUILT document that skips the verifier still splits them. **Observability before the rule: none in either direction** — `de dag` -> `de dagX` left verifier, `ref` and the ASP backend on exactly the untwisted answers (PASS 7 and 3 in both), because no executor evaluates a day-kind definition today; `Rekendatum` -> `RekendatumX` is loud in both OCaml executors but with each one's FALL-THROUGH message (`expression kind not implemented` / `not translated`), which names no defect. **The empty half, closed at L4:** a context value's `name` is a required string since L4 and the loaders refuse a null. *History:* a `naam` of `null` was schema-valid (the field was typed `["string", "null"]`), carried by no document, and split nothing: measured 2026-09-10, verifier PASS and both OCaml executors falling through to their generic refusal with no dereference anywhere — unlike WF-095's null lexeme — so it is recorded in ch. 4.1 with its measurement instead of folded in. **The one producer route that emitted a name outside the set was already ill-formed**: the RegelSpraak frontend builds a `Constante` named `hij` for the animate pronoun and normally resolves it away (of the 38 goldens with a committed source, the 14 whose source writes `hij` emitted no `Constante` at all), and where it did leak — measured 2026-09-10, `indien hij gelijk is aan hij`, `diagnostics: []` — it leaked carrying the UNKNOWN-TYPE SENTINEL, which WF-034's type prologue refused at the same node. This rule added a second, name-level line there and cut no conforming producer route; since L4 the emitter itself refuses every name but `de dag`, `Rekendatum` and `Rekenjaar` and writes no document (measured 2026-09-24). Cost: none — all 14 context values predating the rule spell an admissible name verbatim; the before/after sweep over all 580 documents moves exactly the two witnesses' verdicts and not one report line elsewhere, and all 14 positions were twisted one at a time to show the check reaches each (flat and nested composites, a function argument, a temporal read's date index, a `Let` body) | gate 3 | `anchor/jutterswet-penningvintage` (the `evaluationDate` index of a vintage read), clean and replayed by gates 5 and 6 — from L4 until 2026-09-26 the one golden that carried a `ContextValue` (measured 2026-09-24; the red zone carries two more, both `evaluationDate` since L5); since 2026-09-26 also `jutterswet-rekenjaar` (`evaluationYear` at four positions), clean and replayed by gates 5, 6 and 12. Until L4 also `jutterswet-schouwrooster`, whose four `de dag` positions of two day-kind definitions are `BoundDay` since; the witnesses are broken from these two and from `jutterswet-bergingsklok` | `wf098-unknown-context-value-name` (a name outside the set), `wf098-context-value-name-capitalization` (a recapitalisation of one inside it; `EvaluationDate` since L5) — the two fall independently: dropping the whole arm frees both, relaxing only the byte-exactness frees the second alone — and `wf098-de-dag-as-context-value` (a `ContextValue` named `de dag`, since L4) and `wf098-v0-name-evaluation-date` (a `ContextValue` named `Rekendatum`, the v0 spelling, outside the set since L5); the type arm: `wf098-evaluation-year-with-decimals` (an `evaluationYear` annotated with two decimals) and `wf098-evaluation-date-with-time` (an `evaluationDate` annotated millisecond-precision, which WF-049 also reports where the vintage read uses it) — dropping the type arm frees both, relaxing either half frees its own witness alone |
| WF-099 | a clamp bound's unit is compatible with its BASE's (ch. 4.6, maintainer decision 2026-09-10): the `minimum` and the `maximum` of a `Clamp` are each crossed against the base — both unitless, or both present and convertible, with a unitless numeric literal adopting the base's unit first — through the SAME predicate WF-035 uses on an additive pair and WF-036 on a comparison, so the rule adds a position rather than a fourth notion of compatibility. Crossed against the BASE and not against the sibling bound because the base is what a bound is restated INTO, which three clauses of `RUNTIME-SEMANTICS.md` carry between them: ch. 2.7 is the RETURN law alone (the clamp yields base, minimum or maximum, minimum applied first, comparisons exact) and carries no unit sentence at all; ch. 2.10 gives a clamp, as a composite position, the effective type computed from its BASIS; and ch. 2.6 is where the restating lives — an operand restated into the other's unit with exact factors, a comparison across units restating BOTH sides by their exact canonical factors. Every comparison a clamp makes being base-against-bound, min-against-max needs no clause of its own — a clamp never compares them; a bound carrying no type defers, and a non-numeric bound stays WF-035's clamp arm rather than being reported twice. **It is the position the other three left uncovered.** WF-035 crosses an ADDITIVE node's operands and a clamp is not one — its own clamp arm reaches the bounds for numeric-ness and for presence, never for units; WF-036 crosses a COMPARISON; WF-023 crosses the written value against its target, which on the witness is `EUR` against `EUR` and agrees; and WF-091 cannot reach a bound at all, the bound being a `Literal` and therefore one of the three ch. 2.10 positions where the annotation IS the source, with no declaration to cross. Measured 2026-09-10 on a clamp over a `EUR` base with both bounds annotated `kg`: verifier PASS before the rule, `ref` `EVALUATION FAULT: incompatible units: EUR vs kg`, the ASP backend `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated`, and the Drools backend `ERROR Eenheidsconflict: onverenigbare eenheden bij een §6.1.4 begrenzing-ondergrens: 'kg' (canoniek kg) herschaalt niet naar 'EUR' (canoniek EUR) — er bestaat geen omrekenfactor` — the verifier admitting what all three executors refuse, which is the direction that matters. Those two Drools branches are not one fence and are not elided into one: the exec-model branch carries a clamp-SPECIFIC gate per bound (the `§6.1.4 begrenzing-ondergrens` and `-bovengrens` restatings the message names) while the DRL-text branch catches the same defect through its generic §4 value-restating, both arriving at the one shared rescalability demand. **Convertible and not identical, measured rather than read off the messages**, which disagree: `ref` says `incompatible` and tests convertibility, the ASP backend says `non-identical` but tests identity of units already normalised per declared system (so within one system it IS convertibility), and the Drools backend restates through a rescalability demand pinning `kg`/`t` rescalable and `jr`/`kg` a fault. On the same document with both bounds moved to `t` (system `Gewicht`, factor 1000 to canonical `kg`) over a `kg` base, `ref` and the ASP backend both restate and both answer 10 t = 10000 kg — identical values, no fault on either leg — so identity would have refused a form all three play. The choice also CLOSES two measured ref/ASP divergences, both refused here because canonical names differ: a bound whose `canonical` (v0 `canoniek`) contradicts the system it names (`t` declared canonical `ton` over `kg`), and a bound relabelled into the base's system (`kg` relabelled into `Munt` over `EUR`, where the ASP backend answers the untwisted values and silently treats kilograms as euros). What it does NOT close is not the shape first suspected: a bound naming an UNDECLARED system is already refused by WF-034 (measured 2026-09-10, once per bound: `unit 't' declares unit system 'Massa', which no declaration carries: the unit resolves against no conversion table`), so no conforming document carries it. **Closed (2026-09-19) by the EXECUTOR, not by the rule:** the CROSS-SYSTEM bound whose system is fully DECLARED — `Massa` declared beside `Gewicht` (`kg` factor 1, `t` factor 1000, both canonical `kg`), a `t` bound over a `kg` base in `Gewicht`: convertible by canonical name, so verifier and rule admit it, while on 2026-09-10 `ref` restated to 10000 and Drools answered 10000 on both branches and the ASP backend refused to translate, its per-system normalisation finding no identity ACROSS systems. The rule's own choice settled it — convertibility is a canonical-NAME law, so the backend was enforcing a fence the norm does not carry, and the backend conformed rather than the spec narrowing: it now gathers its conversion table by canonical name across declared systems and admits an exact-factor-1 pair at the identity fence, with the integer encoding untouched (a cross-system ratio that is not a positive integer into the finest unit stays the same loud refusal a same-system one gets). Re-measured 2026-09-19 on all four positions: the WF-099 bound, the WF-035 join and the WF-023 write move to the pinned values, and so does the factorless `kg`@Massa / `kg`@Gewicht pair; the WF-036 comparison moves only for the factorless pair, because the normalisation pass rewrites rule VALUES and not conditions — and the identical SAME-system document is refused by the same message before and after, so what remains is the pass's scope and not a system fence. Cost: zero elsewhere — the Drools sweep over both branches moves exactly the new golden's line (exec 153 → 154, DRL-text 126 → 127) and the corpus replay counts grow only by it. Golden `jutterswet-stelselweging` is the cross-system carrier: two declared systems sharing canonical `kg`, crossed at all four positions with and without a factor, `lastgewicht` crossing twice in one rule (a `Massa` bound over a `Gewicht` base written to a `Massa` target), hand-computed absolutes replayed by ref ≡ clingo ≡ Drools on both branches; gate 12's `EXPECTED_PLAYED` 153 → 154, gate 13's twist set 108 → 126 annotations over 22 → 23 goldens with the floor unchanged. `backlog/done/20260910-kruis-stelsel-eenheid-divergentie.md`. Cost of the original 2026-09-10 landing: none — the before/after sweep over the verifier-gated zones (golden 166, negative 359, red 26 — 551 documents) moves exactly the witness's verdict and not one other report line; the one-sided-pair posture inherited from the sibling predicate (ch. 4.1's open class) is carried by no clamp in those zones and by two of the parity zone's 30 documents, which no gate puts through the verifier and which were already non-conforming for unrelated reasons | gate 3 | `jutterswet-grensbedrag` — the corpus's only clamp carrier, unitless on base and on both bounds, so it pins the admitted both-unitless reading and is replayed by gates 5, 6 and 12. **Landed (2026-09-11):** `jutterswet-vangstklem` carries the unit-bearing clamp, so the convertible half rests on a gated document too and not only on the executors and the witness's falsifiable variant: a `1 t` minimum and a fractional `2,50 t` maximum over a `kg` base in one declared system (factor 1000), plus a mixed clamp whose `2500 kg` maximum is the same weight the `2,50 t` arm reaches by restating. ref ≡ clingo ≡ Drools on hand-computed absolutes, and the restating is measured rather than assumed — relabelling a bound to `kg` moves the answer on every leg. Gate 12's `EXPECTED_PLAYED` equality 122 → 123, not the 119 → 120 this row predicted: the channel shifted three times under the card while it sat in the backlog — `jutterswet-keurhouder` took the 119 → 120 slot, then `jutterswet-ketenband` and `jutterswet-kavelgrens`. Gate 13 moved nothing mechanical, as predicted: the twist set grew 74 → 83 annotations over 16 goldens instead of 15 with the floor unchanged; only the script's prose note about the DRL branch's `Begrensd` value arm went stale, and it was corrected in the same commit | `wf099-clamp-bound-unit` (both bounds foreign) — the two bound positions fail independently, so dropping either arm frees exactly its own report, and moving the bounds to a convertible unit or to no unit at all turns the case clean. **Since 2026-09-25 (parity well-formedness card):** gate 11 holds the parity replay slices verifier-clean; the one replay slice that carries the one-sided pair, parity slice 24, is its named holdout on this report; the casus dump, which carries the same rule, stays outside that leg. |
| WF-100 | a characteristic check's explicit `host` (v0 `eigenaar`) DECLARES the characteristic it names (ch. 4.6 (q1), maintainer decision 2026-09-10): on a NON-navigated `CharacteristicCheck` the type the field names is crossed against that type's `characteristics` (v0 `kenmerken`), and the same key carries WF-034's existence cross and WF-043's time-dependence cross — where the field is present it is the key, where it is absent the enclosing form's subject is, one key computed once. It is the SECOND half of WF-034 (q), which fenced the field's RESOLUTION and said in as many words that it left this open, and it is a SEMANTIC decision rather than a resolution one: it settles WHICH OWNER IS AUTHORITATIVE, the field or the subject. **The field is.** **What the key means to a reader**, written out so no executor has to guess: every declaration consulted for the characteristic — does the type declare it, is it time-dependent (a `timeline` facet on its Characteristic node; v0 `kenmerkGranulariteit`), and the node key `char:<owner>:<name>` of the dependency edge the check contributes — is the NAMED type's. The dependency graph was already keyed that way in all four readers (the shared graph the verifier and the reference interpreter use, and both Drools graphs); what the rule adds is that the two crosses now agree with it. The OBJECT the check is evaluated on is the other half of the field and is NOT settled here: the field names the type whose PATTERN hosts the constraint, which is why the v0 producer emits it at all (it tags a fact-creation guard with the DOEL type so the constraint lands on the doel pattern and never on the anker, even under a same-named anker characteristic), and a bean-model executor honours that. Until 2026-09-19 the reference interpreter and the ASP backend read exactly ONE object per condition, so where the owner was not that object's type they REFUSED instead of answering from the subject — the reading this decision rejects. **That half is now decided too** (WF-109, ch. 4.6 (q2)): the fact-creation guard binds two objects and all four executors read the one the field names, so the refusal became a verifier verdict (`RUNTIME-SEMANTICS.md` ch. 5.8b). An absent owner and an owner naming the subject's own type are the same reading and always were, which is why no document that predates the rule moves. **Measured before the rule (2026-09-10), on a verifier-clean document** whose owner named a declared type OTHER than the subject's with both types declaring a same-named characteristic: verifier PASS, `ref` and the ASP backend both answering from the SUBJECT's value under the other type's name, and the Drools backend carrying no rule at all (`EXCLUDE transpiler SKIP: regel (voorwaarde buiten Phase-B scope)`) — one document, three executors, two readings and a silent one. Point the owner instead at a type that does NOT declare the characteristic and the third executor degrades further: on the fact-creation guard, where its bean model really does host the constraint on the named type, the DRL-text branch emits a property constraint against a bean with no such field and never compiles it (`Unable to Analyse Expression gehesen == true: unable to resolve method using strict-mode`) while the exec-model branch builds and dies at fire with `ConstraintEvaluationException: Error evaluating constraint` — both measured 2026-09-10 on the producer shape with the characteristic taken off the owner, where the verifier then answered, on the wire of that date, `WF-100 FAIL /feitCreaties/0/voorwaarde/eigenaar: 'gehesen' is not a declared characteristic of 'Sein'`. **The subject-less day-kind position needs no separate sentence**: there the walk knows no subject, so the cross did not run at all and the field was read by nobody; under the owner key it is simply the only key there is. **Scope of the CROSS is the NON-navigated check** — on a navigated one the characteristic lives on the REACHED type, which WF-053's arm crosses there, so an owner is not that type and crossing it would reject a form nothing has decided; the `gedurende` clip member is refused an owner outright by WF-097. **The executors' hosting refusal is NOT so scoped and runs on both forms**, the owner naming the pattern the constraint hangs on whether or not the check then hops: measured 2026-09-10 on `jutterswet-postwacht` with the reached type in the field (`risicopost` via `post`, owner `Post`, subject `Wachter`), verifier PASS before and after, `ref` and the ASP backend PASS (10) before and faulting after, both Drools branches refusing throughout — so that form had NO answering executor at all, deliberately, and both executor suites carried a message-asserting witness for it; since WF-109 it is simply off the wire. Cost: none — re-counted the day the rule landed, **216** characteristic checks over the three verifier-gated zones (golden 166, negative 359, red 26 — 551 documents), **6** with an explicit owner, all six in the negative zone and all six consistent (five naming a type that declares the characteristic, the sixth naming a type that is not declared at all, which (q) already refuses); the parity zone adds 9 more over 30 documents and 0 of them carry an owner. There is NO counter-example in the corpus and the rule's first witness was written for it, so this is a design decision on an empty CORPUS set — but NOT one without measured damage: **the pre-rule cross rejected the frontend's own emission shape**, measured 2026-09-10 on the `jutterswet-postwacht` fact creation (anker `Kustvak`, doel `Sein`, a `gehesen` characteristic on the doel, guard `eigenaar: "Sein"`) — before, `WF-034 FAIL /feitCreaties/0/voorwaarde: 'gehesen' is not a declared characteristic of 'Kustvak'`; after, PASS. The cost rides with the repair: on that now conforming document the two OCaml executors FAULT while BOTH Drools branches play it and answer `vakseinaantal` 1 from the DOEL instances, where before the rule the two OCaml executors answered 0 by reading the anker — one verifier-clean document, one executor answering and two refusing, which is exactly the third option the residue card records. The before/after sweep over the verifier-gated zones (golden 167, negative 360, red 26 — 553 documents) moves exactly the witness's verdict and not one other report line. **Residue, carded rather than noted, and since closed** (`backlog/done/20260910-eigenaar-gastheer-object.md`)**:** the OBJECT half — that a hosted guard is about the named type's instance — stayed open here, a refusal being a smaller answer than an implementation, and WF-109 is the implementation | gate 3 | `jutterswet-keurhouder` — the corpus's FIRST golden carrying a `host` (then `eigenaar`) at all (before it the field lived in the negative zone only, so no gated, replayed case pinned what a reader does with it): two rules over both polarities, each naming the subject's own type as owner, beside a second declared type that carries a characteristic of its own. It is the source the witness is broken from and is replayed by gates 5, 6 and 12. **Stated rather than implied:** while WF-100 stood alone the REFUSING half sat outside the corpus for a ZONE reason and not an impossibility — a golden must replay, so it was not one; and a red case must fault in every executor that CARRIES its form, which the Drools backend did not do here (on a plain rule it drops the rule as a transpiler skip; on the producer shape it plays and answers without faulting). Since WF-109 the question is settled the other way: the form the producer writes REPLAYS in all four and is a golden (`jutterswet-seinhijsing`), and the unreachable owner is a verifier verdict with a negative witness. The executors' refusal survives as a fail-fast fence on a non-conforming document and rests on their own gated suites, `ref/test/test_ref.ml` (ch. 5.8b, gate 5) and `backends/clingo/test/test_clingo.ml` (gate 6) | `wf100-host-declares-characteristic-negated` (a declared owner that declares another characteristic and not this one) — falsifiable in two directions, each turning it clean: give the owner the characteristic (the verifier then admits it and the executors refuse it, a different rule and a different report), or put the subject's own type back in the field |
| WF-101 | a validity window's BOUNDS carry WF-084's date lexeme (ch. 4.1, maintainer decision 2026-09-11): every present `from`/`to` on every `Validity` (v0 `vanaf`/`tot` on a `Geldigheid`) is the strict ISO form `yyyy-mm-dd` AND a real calendar day, checked by the very predicate WF-084 uses on a date LITERAL — the same grammar at the one date position on this wire that grammar did not reach. `null` is admitted and is not the WF-095 empty half: here it is the DECLARED open bound, so the rule reads present bounds only. **It turns an assumption three readers already rely on into a demand on the document.** The verifier, the reference interpreter and the ASP backend each HELD a bound as an OPAQUE STRING and ordered it with `<=` (until 2026-09-23; since card `ref-geldigheid-datumlezing` the executors read the bounds through the strict date reader and compare day numbers); `RUNTIME-SEMANTICS.md` ch. 8's version selection WAS that comparison and WF-021's overlap test still rests on the raw order in the verifier alone. Lexicographic order is calendar order only INSIDE this lexeme, and until this rule nothing on the wire demanded the lexeme — the assumption sat in a comment beside the verifier's overlap check and held of nothing. **Measured 2026-09-11 on `jutterswet-vensterheffing`**, one bound substituted and nothing else touched, verifier PASS in every row before the rule: `+012026-01-01` and `-0500-01-01` sort BELOW every four-digit year (`+` is 0x2B, `-` is 0x2D, both under every digit), so the window silently opens at the beginning of time and both executors applied the version to the casus BEFORE it — 125 where the pinned answer is leeg; `niet-een-datum` begins with a letter and sorts ABOVE every date, closing the window over its own casus — leeg where the pinned answer is 125. Not one row faults in any reader: every wrong answer is silent, and `ref` and the ASP backend agree row for row, so the differential cannot see it either. **Two layers, and the schema pattern can only carry one.** The pattern gate 2 puts on these two fields refuses three of those four lexemes and cannot refuse the fourth: `2026-02-30` is four-two-two digits, matches, and even ORDERS correctly (lexicographically between `2026-02-28` and `2026-03-01`), so no opaque-string reader ever REPORTS that February has no thirtieth. Whether it moves an ANSWER is the one place in the measurement where the substituted POSITION decides, and it is named rather than elided: at the LOWER bound of the window both executors replay the pinned expectations unchanged, while the same lexeme at that version's UPPER bound silently shrinks the window past the in-window casus and both answer leeg where 125 is pinned (measured 2026-09-11, both positions, neither faulting). Never reported and only sometimes visible in an answer is what a gated case cannot be relied on to catch, which is the argument for a rule here. A calendar is not a shape; that layer belongs where WF-084's already is. **What it makes true:** `<=` on the raw bound IS calendar order, so WF-021 in the verifier is correct AS WRITTEN — and since 2026-09-23 the executors parse the bounds outright, closing the same gap from the reader side. **Residue, stated rather than hidden:** a document reaching an executor WITHOUT passing the verifier met no fence in the executors until 2026-09-23; since then every executor parses the lexeme (ref and the ASP backend through the shared validity gate, Drools on its own threshold) and only the verifier reads the raw string — the same standing WF-081, WF-084 and WF-095 have. Cost: none — counted the day it landed, **1814** bound positions over the three verifier-gated zones (golden 169, negative 364, red 26 — 559 documents), **1800** null and **14** with a value; the twelve that predate the rule spell an admissible lexeme verbatim (`2025-01-01`, `2025-12-31`, `2026-01-01`, `2026-12-31`, over five documents) and the other two are this rule's own witnesses, while the parity zone adds 128 positions over 30 documents, all null. The before/after sweep over those zones moves exactly the two witnesses' verdicts and not one other report line | gate 3 | `jutterswet-vensterheffing` — two INDEPENDENTLY dated rules, not two versions of one name: they carry distinct names, write distinct attributes and sit in no rule group, the first writing both bounds and the second only an upper one, so the document exercises the WINDOW and not ch. 8 version selection (measured 2026-09-11). It is replayed by gates 5 and 6 on a casus inside the first window and one before it, and it is the document every measurement above is a twist of. The VERSIONED witness is `anchor/jutterswet-jutheffing`, where `#1` and `#2` of one name share a target attribute across adjacent windows with absolute per-window values through both executors — the two documents cover the two shapes a bound appears in, and only the second one puts WF-021 and the ch. 8 selection to work | `wf101-validity-bound-signed-year` (the SHAPE arm: a sign-prefixed extended year, which the schema pattern also refuses — in a zone gate 2 never validates) + `wf101-validity-bound-calendar-day` (the CALENDAR arm: `2026-02-30`, which the pattern admits) — the two fall independently, so dropping the rule frees both and relaxing only the calendar half frees the second alone |
| WF-102 | a VALUE fold's set source's MULTI-STEP range is a `sourcePath` (`bronPad` until L4), and it is a CONNECTED path from the fold subject to the element type (ch. 4.3, maintainer decision 2026-09-11): **(a)** never beside `sourceRole` — since L2 (2026-09-24) enforced by the schema and every loader (B12) — the one-step range IS `sourceRole`, so one meaning keeps one spelling (the discipline WF-087 draws for the nullary keuzetype); **(b)** at least TWO steps, so the empty list and the single step stay the one-step spelling's territory; **(c)** admissible only as the range of a VALUE fold's set source (since L2, 2026-09-24: the count source's range is one step, and the subject's own read has none) — until L2 phrased as: only at the position `bronRol` stands at and only on the **set** source, the non-set sources handing the fold something else entirely (the temporal total reads the subject's own timeline, the row-1 wart hands it the subject's linked instances), so a range does not apply to them; **(d)** every step resolves (WF-047, per step, at a new position rather than under a new rule) and the steps JOIN — step 1's opposite role filled by the fold subject, each further step's by the type its predecessor reaches, the last step's BOUND role by the `elementType`. **There is deliberately no bindingless fallback** (and since L2 none for any range, B12): WF-048 derived a relation from a type PAIR, and a path's intermediate types are its own claim, never a consequence of its endpoints. **What it makes expressible, and what that closes.** Before it, a written range stacking two relations had no wire form and could only DEGRADE: the outer step fell away, and where a fact type happened to relate the subject to the element type directly the degraded document was verifier-clean and ref, the ASP backend and Drools all answered the same WRONG number — no gate saw it, since the differential square agreed (measured on the card's document B: 5,00 where the sentence says 3,00). The RESOLUTION-side half of that was shut on 2026-09-11 by refusing the sentence; this rule is the other half, which makes it expressible instead. The reading is the FLAT UNION and it is normative (`RUNTIME-SEMANTICS.md` ch. 3.4b): a SET of reachable elements, an element reachable along two intermediates contributing once, the empty-source rules applying to the union and never to an intermediate step. The nested reading is NOT a desugaring of it and differs at `minimum`/`maximum` (an empty intermediate has no fallback to absorb it), at `count` (elements versus intermediate instances) and at `sum` with a non-zero `emptyFallback` (once, versus once per empty intermediate — with a zero fallback the two coincide numerically, which is why that third divergence is named and pinned nowhere). **Depth is UNBOUNDED, decided rather than assumed:** a cap is a constant with no semantics behind it and it only ever LOOSENS — raising it later turns ill-formed documents into well-formed ones, a wire-format event — so the corpus pins two steps and three instead. A TO-ONE step is an ordinary step: multiplicity is a declaration fact and does not enter the reading, which is what makes the to-one intermediate expressible too. **Cost, counted:** the four zones held 56 member folds and NOT ONE multi-step range before this rule, so no document moves; `ref` dedups the union by instance label, the ASP backend gets it free (its aggregate term names only the element, the intermediates being existentially quantified inside), and the Drools backend carries it since 2026-09-18 (card `drools-bronpad`: `jutterswet-vondstketen` plays in gate 12, and the two documents whose middle relation is many-to-many are refused by a transpiler marker named in gate 12's channel floor); until then it refused the form by name at load (`Functie.bronPad`) — an EXPLICIT refusal, because its loader then checked no unknown keys inside a node and a dropped field would have re-derived the very misbinding this rule closes | gate 3 | `anchor/jutterswet-bootberging` — the two-step range and the three-step one on one document, replayed by gates 5 and 6 over two cases: the minimum on the FIRST intermediate while the second contributes (the answer that proves both are walked), the three empty corners, and the shared element that makes the sum 15,00 and not 18,00 — plus `jutterswet-vondstketen`, the EMITTED witness (gate 10) on a model that also carries a direct subject-to-element relation | (a)'s `wf102-bronpad-naast-bronrol` was removed with reason at L2 (the loader refuses both ranges); `wf102-source-path-empty` + `wf102-source-path-a-step` (b, falling independently — the second spells a VALID one-step range so the arity is its only defect), `wf102-source-path-by-negated-set-source` (c), and three for (d): `wf102-source-path-start-negated-by-subject` (the first join — the wire form of the very degradation the field replaces), `wf102-source-path-unconnected-step` (a middle join, on a document a rule checking only endpoints would pass) and `wf102-source-path-lands-beside-element-type` (the landing, one step too far). Beside them the two rules the new position widened rather than added: `wf047-source-path-step-unresolved-role` (WF-047 at a step) and `wf083-fold-source-anchor-by-intermediate-type` (WF-083 (c) against an INTERMEDIATE type — the third class of type this field introduces into that position) |
| WF-103 | a present `supertype` names a declared object type (ch. 2.2): the chain must end at a declaration, or no member resolves through it. WF-003's territory by kind; it takes a number of its own because the failure mode is specific and a witness has to be able to cite it | gate 3 | `anchor/jutterswet-strandrechtketen` (the `Wrakrecht is een Strandrecht` chain, replayed by gates 5, 6 and 12) | `wf103-unknown-supertype` |
| WF-104 | the supertype graph is acyclic, self-reference included (ch. 2.2). A cycle leaves member resolution no root to stop at, and every reader would either walk it forever or cut it off at a private depth of its own. Implemented as WF-085's colour DFS with a named cycle path, because the failure is the same shape: no fixpoint | gate 3 | `anchor/jutterswet-strandrechtketen` | `wf104-supertype-cycle` (a two-step cycle) + `wf104-supertype-self-reference` (the depth-1 cycle, which falls independently: a reader that special-cases the self-edge passes the first and not the second) |
| WF-105 | no attribute or characteristic name is declared twice along one chain, ci (ch. 2.2) — WF-002's per-kind uniqueness lifted from the declaration to the chain. One instance would otherwise carry two declarations of one name, and which one a reader finds decides that name's datatype, dimensions and timeline facet on every read of it. The measured consumer consequence is sharper than a compile error: the Drools bean generator deduplicates member names silently, first-wins, so a shadowed name yields a bean that generates and compiles perfectly and is quietly missing one member. What is NOT refused is the sibling collision — two incomparable subtypes of one supertype declaring the same name — which is admitted without a rule (ch. 2.2): no instance is of two incomparable types, so no read is ambiguous, and the supertype-anchored read of such a name is refused by WF-034 rather than by this rule | gate 3 | `anchor/jutterswet-strandrechtketen` (`Wrakrecht` adds one own attribute beside three inherited ones, none of them redeclared) | `wf105-reshadowed-attribute` |
| WF-106 | an object type declares at most one direct supertype (ch. 2.2) | **none, and none possible** — `supertype` is a bare string, so a second direct parent is not a well-formed-but-wrong document a check could reject but one that cannot be WRITTEN; the closed `Objecttype` node refuses the array form and gate 2 is where the refusal is mechanically enforced. A **deliberate, argued departure** from the spec discipline, recorded in ch. 2.2: the alternative was a `supertypes: [string]` wire form plus an "at most one" rule, which buys a real negative at the price of a wire shape that can express exactly what the design forbids, and it was rejected. What the number buys is one citable name for one refusal across three layers | `anchor/jutterswet-strandrechtketen` declares one parent, which costs nothing and proves nothing — the rule's real witnesses are the two frontend fixtures | **none, and none possible** — the rule bounds the language ABOVE the wire, so its witnesses live where that language is read: the RegelSpraak dialect frontend's `is een A en B` fixture and the F-logic frontend's two-`::`-facts fixture, the latter being the sharp case since F-logic can actually write two parents |
| WF-107 | every NAMED form carries a present, non-blank name (ch. 3.1) — the ch. 3 versioned rule forms and the ch. 6.1 defaults groups, the same list WF-020 reads. Presence and uniqueness are two properties and take two numbers: WF-020 compares names, and until this rule it compared only the names that were there, filtering the nulls out before the comparison. **What the name IS**, so no reader has to decide: the form's identity — ch. 8 groups a form's versions by it, the ch. 5.1 dependency graph keys its channel node on it, and a regelversie channel test (`RUNTIME-SEMANTICS.md` ch. 5.6) names a rule by it. **Measured before the rule (2026-09-12)** on a nameless `ResolvedRegel`: verifier PASS, the reference interpreter and the ASP backend evaluating it without noticing, the Drools exec-model branch playing it, and the DRL-text branch throwing a `NullPointerException` out of the rule-name escaper, which calls a string method on the name it is handed — one verifier-clean document, four consumers, three answers and one crash. On a defaults group it is wider still: that array was in no name list at all, so a nameless group, a duplicate group name and a group colliding with a rule all three passed. **Blank counts as absent** because WF-020 compares over collapsed whitespace, so two blank names are one name and every blank form collides with every other. **The wire type stays nullable** and the schema is deliberately not the enforcement layer — a nameless form has to remain a well-formed document, or the rule would have no witness (the WF-106 situation, and the reason that rule has none). Cost: none — counted the day it landed, **1107** named forms over the four zones (626 documents: golden 184, red 26, negative 386, parity 30), every one of them carrying a non-blank name already except this rule's own three witnesses, so no producer moves and the rule closes a door nothing used. **Boundary, drawn per array rather than inferred** (all measured 2026-09-12 over the four zones): FOUR other top-level arrays put a name on this wire then, THREE since M1, and each is outside for its own reason — `dayKindDefinitions` (under `rules` since M1; `resolvedDagsoorten` until L5), whose name is which day kind is being defined and lives in the day-kind namespace, so its presence is **WF-031**'s clause (added the same day, beside that rule's own dead-definition test); `tijdlijnen` (the model list `timelines` since M1), already covered, since WF-015 keys the declaration on the name and a nameless timeline takes every reference down with it, the document falling on **WF-003** (measured: `WF-003 FAIL … reference to undeclared timeline 'penningjaar'`); `ruleGroups` (under `rules` since M1; `regelGroepen` until L5), a label nothing reads, WF-033 resolving a group's MEMBERS by name, so a nameless group is inert rather than ambiguous and refusing it would be a rule without a failure; and `regels`, the parse-stage shell index no check or executor read, which one frontend emitted empty by design and which left the wire at M1. `dagsoorten` (the model list `dayKinds` since M1) is a list of bare strings and a `null` entry there is admitted too — recorded as OPEN rather than closed, since it is the day-kind namespace again and no producer writes one | gate 3 | every golden — the rule's positive witness is the whole corpus, which is what makes it a promotion of existing practice rather than a new demand; named most sharply by `anchor/jutterswet-uitsluiting`, whose rule and defaults group are two named forms with two names | `wf107-unnamed-rule` (absent, on a rule) + `wf107-blank-rule-name` (present and blank — a presence-only check admits it, so it falls independently) + `wf107-unnamed-defaults-group` (the array no name rule reached) |
| WF-108 | the QUANTIFIER FOLD states a quantification something can satisfy (ch. 4.5). Five arms: (a) `quantifier` ∈ {`all`, `none`, `atLeast`, `atMost`, `exactly`} — `the` is a definite description of ONE member and is refused BY NAME because a reader that fell through to the universal would answer the opposite of what the sentence wrote. **The three counting quantifiers were this node's named extension point until 2026-09-16 and are admitted since** (`Compound`'s vocabulary over instances instead of parts, with `Compound`'s `threshold` under `Compound`'s rule); (b) `elementType` present (its RESOLUTION is WF-034's, reported under that number at this position); (c) the RANGE is one meaning with two spellings — exactly one of `sourcePath` and `sourceRole` (since L2, 2026-09-24, enforced by the schema and every loader, B12, so the both-spellings arm and the bindingless derivation are history), at least two steps, a connected walk from the quantifier's subject, landing ⊑-compatibly on `elementType` — WF-102's discipline under this node's own number, new on purpose because WF-102 is scoped to a fold's ARGUMENT, a position this node does not have, and a negative directory must be able to declare the rule it violates; (d) `test` present, since a quantifier with no matrix states a range and asks nothing of it; (e) `threshold` present EXACTLY under the three counting quantifiers — absent there names no number to compare the count with, present under all/none records a quantification the `quantifier` field contradicts. The range MACHINERY is shared, not copied — `wf047_binding` and `resolve_binding` are the fold's helpers called from here (until L2 also `wf048_derivable`), on the WF-097 shared-helper discipline — so an unresolvable binding still reports **WF-047** exactly as a fold's does, and the threshold's VALUE reports **WF-041** for the same reason; only the shape questions this node introduces carry this number | gate 3 | `jutterswet-ploegpremie` (the §8.2 form over seven hand-computed cells, `all` and `none` in one document: the five the production toolchain pins, plus the two that separate an ABSENT member timeline from a partially covered one — the second of which is the cell where the vacuous reading changes sign under `none`) + `jutterswet-reddingsvloot` (the counting arms: all three quantifiers over one §8.2 matrix, each pinned at its threshold from both sides, plus the empty set, the empty SELECTION beside a filled range, and two vloten in one case for the range) + `jutterswet-ketenkwantor` (the one golden carrying (c)'s `sourcePath`: `atLeast` 4 and `atMost` 3 over one two-step range with an element shared by two intermediates, counted once, each from both sides of its threshold, plus a second subject that pins the range; `exactly` is not witnessed over a path) | `wf108-quantifier-fold-quantifier-the` (the bare definite) + `wf108-quantifier-fold-threshold-missing` (a counting quantifier with no threshold) + `wf108-quantifier-fold-threshold-by-all` (a threshold beside `all`) + `wf108-quantifier-fold-without-test` (no matrix) + `wf108-quantifier-fold-without-element-type` (unnamed population); `wf108-kwantorvouw-bereik-dubbel` (bronPad beside bronRol) was removed with reason at L2, when the schema and every loader came to demand exactly one range (B12) |
| WF-109 | a characteristic check's explicit `host` (v0 `eigenaar`) is its GASTHEER and designates EXACTLY ONE object bound at the check's own position (ch. 4.6 (q2), maintainer decision 2026-09-19): exactly one binding SATISFIES it (`T ⊑ O`, every object it can hold is one) and no second binding OVERLAPS it (the declared type graph holds a type below both, so some object could be either). It is the OBJECT half of the field, which WF-100 settled the DECLARATION half of and recorded as open in as many words. A position binds ONE object, the enclosing form's subject — except the `condition` of a ch. 3.4 fact creation, which binds TWO (the anker it iterates and the doel its join chain reaches) and is the only such position in the language. Satisfaction is extensional (ch. 2.2, `T ⊑ O`), so an absent owner and an owner naming the subject's own type or a supertype of it are one reading, and no document that predates the rule moves. **Three flanks, all FAIL.** ZERO designated — the ordinary rule with a foreign owner, and the fact-creation guard naming a THIRD type — is a check about no object; until this rule it was an EXECUTOR refusal in `ref` and the ASP backend while both Drools branches dropped or markered the form, so the smaller answer was the only answer and the differential never closed. PARTIAL — an owner naming a strict SUBTYPE of the bound object, which holds for some of its objects and faults for the rest — is a run-dependent verdict and stays out; whether such an owner should instead read FALSE for the objects it does not cover is a question nobody has answered, and refusing it leaves that open. TWO designated — two satisfying, or one satisfying beside a second merely OVERLAPPING — is a type string that reaches both objects and settles nothing: the SELF-TYPED anker/doel pair, every common supertype of the two, and every subtype edge BETWEEN them. That last shape is why the count is over overlap and not over `⊑`: measured 2026-09-19, with `Sein ⊑ Kustvak` a concrete `Sein` fills an anker declared `Kustvak`, so an owner naming `Sein` reached both objects at run time while a `⊑` count saw one — and schema, verifier, `ref` and the ASP backend all passed that document. No precedence is invented for any of the three, because the producer has no route to them either (on `doel == anker` the frontend leaves the guard an unresolved marker). **Scope of the DOEL, with its reason — three fences:** the second bound object is admitted only where the check IS the whole `condition`, does NOT navigate, and carries NO whole-period qualifier. Measured 2026-09-19: the bean model's exec-model branch lowers a doel-side guard through a fact-only leaf predicate and carries no composite there, so admitting a nested doel host would split the executors instead of closing them; a navigated owner names the host while the characteristic lives on the type the hop reaches, a shape the producer never emits and no witness measures; and under a ch. 3.9 whole-period qualifier BOTH bean-model branches run the fold arm BEFORE the host test, so the field decides nothing there — one branch reached the doel by its own route and the other lowered the fold onto the ANKER and sank the package at compile. All three are refused under this number with the anker as the only bound object, and all three are cheap to revisit — a witness in all four executors is the price, and the third fence falls with the §8.2 family's own decision. The SUBJECT-LESS day-kind condition binds no object, so the cross does not run there and WF-031 refuses the document anyway. **What the rule buys:** the producer's own emission shape becomes replayable. Measured 2026-09-19 on `jutterswet-seinhijsing` before the implementation — verifier PASS, `ref` EVALUATION FAULT and the ASP backend TRANSLATION FAULT (both quoting the superseded hosting refusal), both Drools branches PLAYED with `vakseinaantal` 1 — and after it, all four answer 1, with the anker carrying the same characteristic so that the anker reading (2) and the intermediate-step reading (0) are each positively excluded. **Cost: none.** Counted the day the rule was written, before its own witnesses existed: 260 characteristic checks in 646 documents (golden 213, negative 399, red 34), 10 with an explicit owner — eight naming the subject's own type, one naming an UNDECLARED type that WF-034 (q) owns and this rule steps aside for, and exactly ONE naming a declared type the subject is not, which is the negative witness WF-100 already rejects. The parity zone adds 9 owner-free checks over 30 documents. No document in any zone goes from clean to rejected; the rule adds a second citation to one already-rejected document and moves no other verdict anywhere. The EXTENSIONAL reading is the executors' too, and had to be made so: both Drools branches asked satisfaction by NAME IDENTITY until this rule landed, which refused a legal supertype owner in both of them while the other three executors carried it | gate 3 | `jutterswet-seinhijsing` (the producer's shape with one intermediate step: the anker AND the intermediate type declare and carry the same characteristic, so 1 is the doel reading and neither the anker's 2 nor the intermediate's 0) + `jutterswet-mastsein` (the same law over TWO intermediate steps, where the guard still reaches past both to the doel: 2, not the anker's 3, the first step's 0 or the second's 3) + `jutterswet-keurhouder` (the owner naming the subject's own type — the reading the rule leaves untouched) + `jutterswet-bakensein` (the owner naming a SUPERTYPE of the doel: hand-written IR, because the frontend tags a guard with the doel's own type and never with an ancestor) | `wf109-host-outside-range` (an ordinary rule whose owner DECLARES the characteristic, so WF-100 is satisfied and this rule is the only one speaking) + `wf109-host-third-type` (a fact-creation guard hosted on the intermediate step's type: two patterns, a name for neither) + `wf109-host-ambiguous-self-typed` (anker and doel the same type, the degenerate string) + `wf109-target-host-compound` (the doel host nested in a composite) + `wf109-target-host-navigated` (the doel host on a navigating check) + `wf109-target-host-period` (the doel host under a whole-period qualifier) + `wf109-subtype-overlap` (the anker's declared type can itself be the host type) + `wf109-host-ambiguous-supertype` (one type above both bound objects) + `wf109-host-partial-population` (a host only some of the bound object's population is) |
| WF-110 | the INSTANCE-SET UNION — the §5.7 concatenation of subject expressions as the source of an instance count (ch. 4.3, maintainer decision 2026-09-20). Five arms: **(a)** an `InstanceSetUnion` is admissible ONLY as the SINGLE argument of a `count` aggregation, because it denotes INSTANCES and the one node that folds instances is the instance count (ch. 1.2e, ch. 3.4a) — the shape of rule WF-096 drew for the set reading until L2, drawn for the other collection; at the source of any other fold that carries an `elementType` the clause steps aside for WF-037's fold-source arm, which names the union, so the node is reported once; **(b)** it carries at least TWO selections, one selection being the ordinary single-source count, which has a spelling already (`elementType` beside `sourceRole`) — one meaning, one spelling, the discipline WF-102 (b) draws for the one-step range; **(c)** the count carrying it names NO source of its own, since the selections carry the population, the relation and the subselection one set each, and a source beside them is a second claim on one node (the setbron ground of 2026-09-19); **(d)** every selection names the SAME `elementType` — a count answers a cardinality over one DECLARED population (ch. 2.2), and with a population per selection whether two selections can share an instance becomes a question about the subtype lattice (ch. 2.6) that deduplication on identity would have to answer and this rule does not; the v0 toolchain refuses the same written shape for a MECHANISM reason of its own (one accumulate has one source pattern), so the boundary agrees and the ground does not; **(e)** every selection names its `elementType` and its `sourceRole` (the latter since L2, 2026-09-24, required and non-null in the schema and every loader, B12), with deliberately NO bindingless fallback — WF-048 derived a relation from a TYPE PAIR and clause (d) gives every selection of one union the same pair, so a derived range would make the selections indistinguishable and the union a restatement of one of them. The binding resolves under WF-047 at the new position, as WF-102 (d) applies it per step. **Deduplication on instance identity is the operation**: the members are the union of the selections' contributors and an instance two selections both select counts ONCE, so the answer is |A ∪ B| and never |A| + |B| (`RUNTIME-SEMANTICS.md` ch. 3.4d). **The node carries no `type`** — it denotes instances, not a value — and WF-034's type prologue exempts it by node kind exactly as it exempts a `Lambda`; without that exemption every union would carry a co-firing `value node without a static type`. **What it does NOT reopen:** a selection carries a one-step `sourceRole` and no `sourcePath`, so the multi-step range under a count stays where WF-096 (until L2; since then WF-093 (b)) and WF-102 met it (ch. 3.4b, consequence 2) — a named open flank rather than a side effect. **Cost, counted 2026-09-20 over the four zones:** the node is new to the wire, so not one of the 718 documents predating this card can carry it; swept with the pre-card binary against this one, the ONLY outputs that differ are the seven `wf110-*` witnesses, which the pre-card loader cannot read at all — zero verdict changes and zero report-line changes everywhere else | gate 3 | `jutterswet-vondstunie` (two selections over DIFFERENT relations with a shared element — the dedup witness no OR-filter can spell) + `jutterswet-keurunie` (two selections over ONE relation with overlapping characteristic filters, and a filtered selection beside an unfiltered one) + `jutterswet-unievoorwaarde` (the count over a union in a `condition` — the position clause (a) does NOT forbid, pinned so that reading the clause one notch too wide costs a failing gate) | `wf110-union-as-rule-value` + `wf110-union-under-a-builtin` (a), `wf110-union-a-branch` (b), `wf110-union-beside-fold-source` (c), `wf110-union-unequal-element-types` (d), `wf110-union-branch-without-element-type` (e); `wf110-unie-tak-zonder-bronrol` was removed with reason at L2 — `sourceRole` is required and non-null in the schema and every loader (B12) |
| WF-111 | time-window BOUND SHAPE (maintainer decision 2026-09-23): each bound is a date literal or an `AttributeReference` to a scalar date attribute of the form's own subject — un-navigated, not a set read, on a type the subject satisfies — and of day precision. The general date expression is an OPEN wire decision (RUNTIME-SEMANTICS ch. 3.12) | gate 3 | `jutterswet-premievenster` (four literal windows and the subject's own `meldatum` bound) | `wf111-period-boundary-parameter-date` (a date PARAMETER as bound — until 2026-09-23 the red witness of the same form, refused by all four executors) |
| WF-112 | time window never under `none` or a counting quantifier — as a Compound member at any depth, or in a QuantifierFold's filter or test (maintainer decision 2026-09-23): the window gates the RULE and no reading of a complemented or counted verdict is specified | gate 3 | `jutterswet-premievenster` (`borgpremie`: the window as an `all` member) | `wf112-time-window-counting-quantifier` (the window under `none` — until 2026-09-23 the red witness, refused by all four executors) |
| WF-113 | `diagnostics` is the producer's own account of what stayed unresolved in a document it did write (ch. 2; decision V2 (ii), 2026-09-19; landed M1 2026-09-23). (a) every entry carries severity `warning`, a `message` that is neither empty nor blank, and an integer `line` ≥ 0 (0 = the producer reports no source position: a hand-built document, or a frontend warning without a token); `error` and `info` are refused by name, and so is a null severity (the schema admits `warning` only; the verifier's loader admits the others so that this rule, not a load failure, gives the reason; v0 `WARNING`, `ERROR`, `INFO`). (b) every `Unresolved` node — the expression and the condition marker, at every expression and condition position — is accounted for: at least one diagnostic's `message` contains its `source.snippet` verbatim. The containment is literal, so a marker without a `source`, or whose snippet is blank, cannot be accounted for and is refused (blank counts as absent, the WF-107 precedent): a blank snippet would be contained in nearly every message, and a message about another marker would account for it. The schema requires `source` on an `Unresolved` and every executor refuses a marker without one at load; the verifier's loader admits it so that this rule gives the reason. An EMPTY snippet cannot be written: the schema requires one character at least, and every reader, the verifier included, refuses it at load under no WF number ("a source snippet is never empty: a node without source text carries no source", ch. 4). Until L5 the marker was `Onopgelost` and (b) read its `bron` string, refusing a null and an empty one here. (c) no consumer reads `diagnostics` for evaluation — stated, not fenced | gate 3 ((a) and (b); that `line` is an integer is the loader's check, that it is ≥ 0 is (a)'s; an empty snippet is the loader's refusal, under no WF number; (c) has no document to reject) | `jutterswet-kaletabel` (a `source.rgs` golden whose frontend route writes one diagnostic of severity `warning`, for a decision table with only a header row; the positive witness of (a)); the first corpus carriers of (b) are the red documents `wf044-unresolved-guard` and `wf044-unresolved-reached`, verifier-gated and clean | `wf113-severity-error`, `wf113-severity-info`, `wf113-severity-null`, `wf113-empty-message`, `wf113-message-blanco`, `wf113-line-negative` ((a), one per refused shape), `wf113-marker-without-report` ((b): an expression marker inside a condition, empty list), `wf113-report-names-marker-negated` ((b): a condition marker whose warning quotes the `source.snippet` with one letter recapitalised, so an any-diagnostic or case-insensitive reading frees it), `wf113-marker-source-null` and `wf113-marker-source-empty` ((b): a marker without a `source`, resp. whose snippet is blank — a single space since L5, because an empty snippet can no longer be written —, beside a second marker whose warning would account for it under an absent-as-empty or a literal reading) |
| WF-114 | a `Literal`'s value variant is the one its `type` names (ch. 4.1, landed at L4 2026-09-24, delta B22): `number` for a `Number` (a percentage included), `boolean` for a `Boolean`, `text` for a `Text`, `date` for a day-precision `Date`, `dateTime` for a millisecond-precision `Date`, `enumeration` for an `Enumeration`, `constructor` for a `SumType`; a list or struct type names no variant, and a literal whose `type` is null is WF-034's typeless value node, which this rule does not reach. The one crossing of variant and type: WF-081, WF-084 and WF-095 key on the variant. Every executor refuses the same literal at load with the verifier's sentence, so none reads a value its type does not name | gate 3 | every golden literal: 532 literals in 163 goldens, six of the seven variants (the `constructor` variant has no golden carrier; its one carrier is the negative `wf036-equality-enumeration-against-sum-type`, whose rule reads the type) | `wf114-literal-variant-against-type` (a `text` variant under a `Number`) + `wf114-date-variant-against-precision` (a `date` variant under a millisecond-precision `Date`) |
| WF-115 | a `fixedStart` fits its grid (ch. 2.8), on a `Timeline` and on the timeline facet of an attribute, a characteristic or a parameter: **(a)** none on a `Timeline` with `variableStart: true`; **(b)** the lexeme the grid takes — none on a day grid of count 1, a date `yyyy-mm-dd` naming a calendar day on a week grid or a grid of count ≥ 2, on a month, quarter or year grid of count 1 a day-month `--mm-dd` naming a day of a leap year, spelt with the earliest month and never `--01-01`; **(c)** a facet that names a timeline carries that timeline's `fixedStart` (adoption total, as WF-005 for granularity and count) | gate 3 | `jutterswet-vast-startpunt` (a named year grid `--10-01` adopted by its facet, an inline month grid `--01-31`, an inline week grid `2024-09-30` on a characteristic) | `wf115-fixed-start-point-at-variable` (a) + `wf115-fixed-start-point-outside-grid` (b) + `wf115-fixed-start-point-negated-adopted` (c) |
| WF-116 | the vocabulary declarations (ch. 2.12): **(a)** a vocabulary's name is a word — not empty, free of whitespace, parentheses and the colon; **(b)** vocabulary names are unique (ci); **(c)** at most one vocabulary is the default; **(d)** a present `language` is a code — not empty, free of whitespace | gate 3 | `jutterswet-vocabulaires` (`handboek` with a language, and `cgm`, the default, with one) | `wf116-vocabulary-name-none-word` (a) + `wf116-duplicate-vocabulary` (b) + `wf116-two-default-vocabularies` (c) + `wf116-language-with-whitespace` (d) |
| WF-117 | every designation (ch. 2.12): **(a)** names a declared vocabulary (ci); **(b)** at most one per vocabulary on one declaration, an enumeration value's counted over the entries of `valueDesignations` that name it; **(c)** not empty and trimmed at both ends; **(d)** no parenthesis; **(e)** an entry of `valueDesignations` names a value of its enumeration (ci). A designation is text in every vocabulary: there is no vocabulary kind (maintainer decision 2026-09-27) | gate 3 | `jutterswet-vocabulaires` (a designation on every designatable kind, the enumeration values included) | `wf117-unknown-vocabulary` (a) + `wf117-two-designations-a-vocabulary` (b) + `wf117-designation-negated-trimmed` (c) + `wf117-designation-with-bracket` (d) + `wf117-value-designation-unknown-value` (e) |
| WF-118 | the effective names of one scope are unique per vocabulary (ci) — the designation where there is one, else the canonical name — so that the way back from a name in a vocabulary to the declaration is a function (ch. 2.12). The scopes are the canonical ones: **(a)** the object types, the fact types, the parameters, the named forms (WF-020) and the rule groups, each on its own; **(b)** the attributes of an object type with the ones it inherits, and likewise its characteristics; **(c)** the roles of a fact type; **(d)** the values of an enumeration domain. A collision of two canonical names is WF-001's, WF-002's, WF-020's or WF-105's | gate 3 | `jutterswet-vocabulaires` | `wf118-effective-name-object-types` (a) + `wf118-effective-name-attributes` (b) + `wf118-effective-name-roles` (c) + `wf118-effective-name-labels` (d) |
| WF-119 | the default vocabulary is complete (ch. 2.12): every object type, attribute, characteristic, fact type, role, parameter, named form and rule group, and every value of every enumeration domain, carries a designation in it | gate 3 | `jutterswet-vocabulaires` (every declaration named in `cgm`) | `wf119-default-vocabulary-incomplete` |
| WF-120 | a redundant designation — in a vocabulary that is not the default, spelling its declaration's canonical name byte for byte — is admitted with a warning: some diagnostic's `message` contains it verbatim (ch. 2.12), the accounting of WF-113 (b) and its weakness | gate 3 | `jutterswet-vocabulaires` (the `handboek` designation `naam` of the attribute `naam`, and the frontend's warning that quotes it) | `wf120-redundant-designation-without-warning` |
| WF-121 | every origin (ch. 2.13): **(a)** its `form` is one of the five forms and its `from.kind` one of the ten kinds, byte for byte (an executor refuses either outside its set at load, in this sentence without its number); **(b)** its `from` carries `owner` exactly for an attribute, a characteristic, a role or an enumeration value, and, unless its kind is `decisionTable`, names a declaration of that kind in the document (ci): an attribute or a characteristic declared by the object type `owner` itself, a role of the fact type `owner`, a value of the enumeration domain `owner`, a named form in WF-020's namespace, an object type, fact type, parameter or rule group of that name; **(c)** its `from` does not name its own declaration, and following `from` through the declarations that carry an origin never comes back; **(d)** the kind of its `from` is the one its form names: `attribute` under `inputAboveComputation` and `stepsAfterSet`, `namedForm` under `exceptionForSubtype` and `startPoint`, `decisionTable` under `decisionTableRow`. Which declarations a form makes is the dialect's and is not checked | gate 3 | `jutterswet-opgave-vormen` (`inputAboveComputation` on three attributes, and two rows of a decision table, `decisionTableRow` from a `decisionTable`), `jutterswet-meldtermijn-strandjutter` (`exceptionForSubtype` from a `namedForm`), `jutterswet-zwaarste-weging` (`stepsAfterSet`, three members and their rules); `inputAboveComputation` also in `jutterswet-opgave-bedrag`, `-opgave-boven-berekening`, `-opgave-datum`, `-opgave-subtype` and `jutterswet-sluitdag-gemeld`, `decisionTableRow` in `jutterswet-rubriektabel-rgs`. No golden carries `startPoint`: no golden source writes a start-point determination; the frontend suite carries it (`OriginOnTheWireTest`) | `wf121-origin-form-unknown` (a) + `wf121-origin-source-unknown` (b) + `wf121-origin-itself` (c, its own declaration) + `wf121-origin-cycle` (c, a cycle of two) + `wf121-origin-kind-fits-negated` (d) |

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
