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
  ch. 3.3 — the back-link role), `bronRol` and `bronPad` (ch. 4.3 —
  the fold source's range, one relation or a path of them), the
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

*One sharpening, stated once here rather than twice below.* Where a rule
invokes this **carriage** sentence as a COMPARISON — WF-094 below, and ch. 4.6
(p) at an object type's side-table keys — it compares **byte for byte,
whitespace included**, which is stricter than the collapse this sentence
describes. The two are not in conflict: the collapse is what a PRODUCER does
to a declared name **once**, when it takes the name off the source text; those
two rules ask whether a name already collapsed was then **copied** unchanged
into a second position. The gap between the two readings is measured empty and
no producer here can write into it — over the **1937** keyed declarations the
four zones hold (recounted 2026-09-10: **2016**, the same reading in every
respect), not one self-name differs from its key in whitespace alone
(the five that differ at all are WF-094's witnesses, each a one-character
recapitalisation), while **603** of those keys do carry internal whitespace,
so the flank is reached and not merely unvisited; and every emitter in this
repo writes the map key and the `naam` field from ONE string. So "verbatim"
means the byte at both sites, and the collapse stays what it always was: a
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
  v1; the `Onbekend` datatype does not exist in v1 (ch. 2.4). *Migration:* v0
  dumps carrying `Onbekend` fail verification with the original unresolved
  name as the reason.
- **WF-094** A **keyed declaration's self-name** MUST be the map key it is
  stored under, spelled **verbatim** — whitespace and case included (maintainer
  decision 2026-09-06). The v0 wire stores five declaration kinds as a **map**
  — `objecttypen`, `parameters`, `eenheidstelsels`, `feittypen`, `dimensies` —
  and every value repeats its own name in a `naam` field. That field is **not a
  reference**: nothing has to resolve through it. It is the declaration's
  identity written a SECOND time, the v0 denormalisation of the ONE `name` a v1
  declaration carries on its node (ch. 2.2, 2.5, 2.6, 2.7, 2.10). So the split
  this section makes applies the way it applies at the denormalised side-table
  keys of ch. 4.6 (p): this is the **carriage** rule above — declared names are
  carried verbatim — and not the matching rule, and the comparison is therefore
  byte-exact rather than case-insensitive, whitespace included, in the reading
  the sharpening note above fixes for both sites at once. The harm is a silently DIFFERENT
  program rather than a missing value: a document whose two spellings of one
  name disagree is well-formed to every reader that keys off the map and a
  different declaration to every reader that keys off the field.
  **FIVE ARMS, one per table**, because the five have five different
  readerships and each has its own witness (`wf094-zelfnaam-<kind>`); a mutant
  dropping one arm frees exactly one document, and a mutant relaxing the
  comparison to case-insensitive frees all five, every witness being a
  one-character recapitalisation.
  *The readers, measured 2026-09-06 across the four consumers.* The verifier
  reads three of the five self-names once each, in WF-057's target-text-safety
  walk, and never against the key; the parameter and unit-system self-names it
  does not read at all. `ref` reads **none** of the five. The ASP backend reads
  **one**, `Eenheidstelsel.naam`, and only to NAME a system inside two fault
  messages of the finest-unit derivation — a reader that cannot change WHETHER
  the backend refuses, only what the refusal says, and no `expectedFault` in
  the red corpus pins either fragment, so even that text has no observable
  channel. It is named here because a fault message is not a semantic reader.
  The bean-model executor is the one that reads them: its loader takes the
  SELF-NAME as the declaration's name — falling back to the key only when the
  field is absent, and for a parameter and a dimension not even then — and
  reads it back at **105** sites for an object type (the DRL-text transpiler
  41, the exec-model transpiler 33, the case runner 8, the fact-model generator
  8 — the generated bean's class name IS `ident(<self-name>)` — the contract
  validator 6, the producer/consumer graph 4, the model helpers 3,
  stratification 2), **32** for a parameter, **3** for a fact type, **1** for a
  dimension, and **0** for a unit system. *The instrument, named so the number
  can be re-taken:* each field was renamed in a `git archive` copy and the
  compiler asked to name every read — and it must be asked with
  `-Xmaxerrs 5000`, because javac stops at **100** diagnostics by default, so a
  bare compile reports exactly 100 and would have understated the largest of
  the five. Uncapped the object-type field reports **106** sites, of which the
  106th is the declaring class's own constructor assignment — a WRITE, not a
  read — leaving the 105 above.
  *The harm, measured the same day, one self-name twisted at a time over every
  golden (**315** positions per instrument, of which **310** sit in a golden
  that has a case file and can therefore be replayed at all — the denominators
  below are given both ways, as the dimension zero already forced; one further
  golden landed the same day and takes the corpus to 317/312, verifier-clean
  under this rule and outside the sweep).* Under a
  ONE-CHARACTER
  recapitalisation — the tightest possible deviation, and the one a
  case-insensitive rule would admit — **11 of the 34** golden parameter
  positions (34 of 34 executable) leave the verifier, `ref` and the ASP backend passing while the
  bean-model executor answers with **WRONG VALUES**, over seven documents
  (`jutterswet-aanvoergeld`, where `aanvoergeld` 322.50 becomes 310.00;
  `jutterswet-penningdrempel`; `jutterswet-penningjaar`;
  `jutterswet-schouwkalender`; `jutterswet-sleeprest`; `synth3-drempelheffing`;
  `synth3-takbarriere`). Not a refusal — a different answer, and the channel is
  a name meeting a name: the case binds its globals under the identifier of the
  WRITTEN parameter name while the transpiler registers them under the
  identifier of the SELF-NAME, and the "global not declared" that follows is
  swallowed by design, so the parameter never arrives. (An instrument that
  swapped the case of the WHOLE name would have measured zero here and been
  wrong: the identifier contract title-cases an ALL-CAPS word, folding such a
  twist back onto the declared spelling. The harm is visible exactly when the
  twist survives that normalisation — which is itself the evidence that the
  identifier contract is the channel.) The other four tables move **no**
  verdict under a recapitalisation: object types 0 of 205 (203 executable;
  three exclusion
  MESSAGES change on goldens the backend already declines), fact types 0 of 53
  (53 executable),
  unit systems 0 of 21 (20 executable), dimensions 0 of 2 — and there the zero
  is structural, and the split says why:
  **none** of the two is executable, the only two goldens carrying a
  `dimensies` table being the verifier-only
  witness-cabinet documents with no case file at all. Under a ci-DIFFERENT
  name the object-type readership shows itself as well: **4** of 205 positions
  move verdict, three PLAYED → EXCLUDE (the self-pair reference field stops
  being derived — that derivation compares the ROLE's filler name, which spells
  the KEY, against the self-name) and one PLAYED → ERROR
  (`jutterswet-aanmelderkrediet`, a missing getter on a bean class named after
  the twisted self-name). Across every twist in both instruments the verifier,
  `ref` and the ASP backend move on nothing.
  *Cost over the four zones: none.* The **1919** keyed declarations the five
  tables held before this rule (`objecttypen` 744, `parameters` 654,
  `feittypen` 301,
  `eenheidstelsels` 209, `dimensies` 11 — **1935** after it, the five witnesses
  bringing their whole source documents with them, and **1937** in the corpus
  as it now stands) all carry a self-name and
  all spell
  their key byte for byte — zero differ in byte while agreeing
  case-insensitively, zero differ outright. That is no accident and it is also
  no guarantee. *v0 mirror note:* the v0 toolchain never asks this question —
  its resolver puts each declaration into the registry under the very string it
  stores in `naam` and its emitter writes the map key and the field from that
  one string, so its own dumps agree by construction and it has no cross for a
  dump it did not write. **Delta (new wire law):** v1 makes the disagreement
  ill-formed, because the guarantee is the producer's and the reader is
  entitled to it. *Migration:* mechanical — a dump whose two spellings disagree
  is repaired by taking the key, which every consumer that resolves anything
  already uses. Over-rejection is settled directly: over all **532**
  documents of golden + rood + negatief + parity the verifier's verdict and
  report are unchanged but for the five new witnesses (negatief zone 320 → 325,
  four zones 527 → 532); the one golden that landed alongside takes the four
  zones to 533 and is verifier-clean under this rule, which gate 3 re-checks on
  every run.
  ***Clause (d) — the EMPTY half: that self-name MUST be PRESENT.*** A keyed
  declaration whose `naam` is `null` is ill-formed, on **all five tables**
  (maintainer decision 2026-09-09; the label is that decision's numbering, and
  this is the rule's second clause — the first asks whether the name written a
  second time AGREES, this one whether it is written at all). The OMISSION
  flank needs no clause: `naam` is `required` in the schema, so gate 2 rejects
  a missing field and the shared OCaml loader — one loader module, consumed by
  the verifier, `ref` and the ASP backend alike, so this is one refusal reached
  three times and not three independent ones — refuses it with a load error
  naming the path. The NULL flank did: the field is typed `["string", "null"]`,
  so `naam: null` is schema-VALID, round-trips byte-identically, and passed the
  verifier, `ref` and the ASP backend cleanly — while the bean-model executor
  DIED on it, a `NullPointerException` inside the identifier contract, which
  trims the name before splitting it, and precisely at the two tables whose
  loader took the self-name without falling back to the key. That is the same
  identifier contract this rule's first clause runs its harm through, and it is
  a sharper split than the ch. 4.6 (n)/(o) pair it resembles, where both OCaml
  executors were loud on the empty form: here two consumers are silent and
  CORRECT — they key everything off the map key, so for them nothing is missing
  — and the third dies. A document that passes the verifier, `ref` and clingo
  while killing the fourth consumer makes the differential square a triangle
  without saying so, which is why the WIRE answers and not one loader.
  **FIVE ARMS again, one witness each** (`wf094-zelfnaam-null-<kind>`), and the
  ground per arm is not the same, stated here as this rule already states it
  for its first clause: on `parameters` and `dimensies` the harm is MEASURED —
  those are the two tables without the fall-back — while `objecttypen`,
  `eenheidstelsels` and `feittypen` rest on the CONSTRUCT: the class is one
  class, the field is `required` because the declaration carries its own name,
  and a clause cut to only the tables that crash today would make the self-name
  mean something different per table (what the five-arm motivation above
  denies) and would move again with the next loader change. *Measured
  2026-09-10, one self-name nulled at a time on each arm's source golden:* the
  parameter arm goes `PLAYED` → `ERROR NullPointerException: Cannot invoke
  "String.trim()" because "naam" is null`, the object-type, unit-system and
  fact-type arms stay `PLAYED` unchanged, and `ref` and the ASP backend move on
  none of the four (3, 7, 5 and 7 expectations, before and after). The
  dimension arm has **no playable witness at all** — the four zones' only two
  goldens with a `dimensies` table are the verifier-only witness-cabinet
  documents with no case file — which is the structural zero the first clause's
  dimension arm already carries, one step further: a live key with no live
  execution. *Cost: none.* Of the **2000** keyed declarations the five tables
  held over the four zones before this clause (2026-09-10, 572 documents:
  `objecttypen` 798, `parameters` 657, `eenheidstelsels` 222, `feittypen` 311,
  `dimensies` 12) not one carries `naam: null`; the five witnesses are the only
  documents anywhere that do (2016 entries over 577 documents afterwards, the
  witnesses bringing their whole source documents with them). Over-rejection
  settled directly: both binaries over all 577 documents of the four zones,
  exactly the five new witnesses move and no report elsewhere.
  *v0 mirror note:* the v0 toolchain writes the map key and the field from one
  string, so it never produces a null self-name and never asks the question.
  **Delta (new wire law):** v1 makes the absent self-name ill-formed.
  *Migration:* mechanical, and the same repair as the first clause's — take the
  key. The bean-model executor's loader gained that fall-back on all five
  tables on 2026-09-10, so a document written before this rule loads instead of
  dying; that fix removes the one LOUD reader and is therefore no substitute
  for the clause, since without a wire rule the next consumer chooses again.
  *What this rule does NOT close, stated so the silence is not mistaken for
  coverage. (The EMPTY half stood first in this list until 2026-09-10; clause
  (d) above closes it, with a witness per table, and it is listed among the
  departures of ch. 4.6's open list.)*
  **(1) The two maps INSIDE a unit system, which look like this class and are
  not.** A unit system carries `eenheden` and `aliassen`; both are maps and
  both hold values with an `afkorting`. This rule does not reach them, and the
  reason is a definition rather than a gap: it crosses a key against the
  value's OWN name, and in neither map is the key that. In `aliassen` the key
  is by construction an ALIAS — a second spelling POINTING at a unit — so
  disagreement is the normal case and the numbers say so: **682 of 712** alias
  entries over the four zones spell something other than the `afkorting` they
  carry (recounted 2026-09-10: **710 of 740**), and the 30 that agree agree by
  coincidence (one alias, in the parity zone, spelling its unit's own
  abbreviation) — the same 30 at every recount. In `eenheden` the key IS an
  abbreviation, yet **58 of 639** entries disagree, over 58 documents of which
  10 are golden — and all 58 are one shape: the key `€` against the stored
  abbreviation `EUR`. *Recounted 2026-09-10: **59 of 654**, over 59 documents of
  which 10 are golden. The shape is unchanged and so is the golden count; the
  one extra pair is this rule's own clause-(d) witness
  `wf094-zelfnaam-null-eenheidstelsel`, a copy of a golden that carries the
  `€`/`EUR` pair, which is why a rule about SELF-names moved a count about UNIT
  names at all.* So the class sentence at the head of this rule — every
  value repeats its own name — is a statement about the five TOP-LEVEL
  declaration tables and holds nowhere else on the wire. Whether the v1 wire
  should admit the symbol/code pair is a question about UNIT identity, next to
  WF-009's and (r)'s, not about a declaration's; if it is ever closed it is
  closed on its own card.
  **(2) Maps with no self-name, and lists.** `tijdlijnen` and `dagsoorten` are
  LISTS, where the name IS the identity and there is nothing to cross;
  `domeinen` maps a name to a datatype, whose value carries no name at all; and
  `dagsoortPerEenheid` maps a written unit spelling to a day-kind NAME — a
  value that names something ELSE, which is why half its entries differ from
  their key by design (on the LANDED state of 2026-09-09, 35 of the 70 the four
  zones hold are keyed on something other than the declared name, the plurals
  among them — entries, not independent cases: 30 of them are byte-identical
  copies of one parity fragment, see ch. 4.6 (s)) and why it
  is a reference question, closed since
  2026-09-09 by ch. 4.6 (s) — which crosses the VALUE against `dagsoorten` and
  leaves the key free for exactly the reason stated here — rather than this
  cross. The `regels` stubs' own `naam` is declared
  non-normative presentation (ch. 3) and stays outside.

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
  uniqueness is computed over the chain-closed type-pair set — so adding a
  subtype can turn a previously well-formed bindingless navigation into a
  loud refusal, because a pair that was uniquely connected may become
  doubly connected once the chain closes. That is accepted, and the answer
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
  **This rule carries no verifier check and no `corpus/negatief` case, and
  that is a deliberate, argued departure from the spec discipline rather
  than an omission.** Single parenthood is true by SHAPE: the field is one
  string, so a second direct parent is not a well-formed-but-wrong document
  the verifier rejects, it is a document that cannot be written — the
  closed `Objecttype` node refuses the array form and gate 2 is where that
  refusal is mechanically enforced. A negatief case needs a well-formed
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
`Objecttype`; the same holds for **TimelineFacet**). *Migration:*
mechanical — each v0 map entry folds into the corresponding member node;
absent map entries become the documented defaults below. Until that
reification the side-maps are fenced where the v1 node shape fences them by
construction: **WF-034 requires every key of all eight maps to name a declared
member, in that member's declared spelling** (ch. 4.6 (p)) — the five attribute
maps an attribute, the three characteristic maps a characteristic — because an
orphan key is not a missing value but a silent change of meaning: it hangs the
facet on a member that does not exist and leaves the intended member at its
default, which every reader then takes without complaint. The spelling is part
of the requirement because the key is the *denormalisation of the member's
identity* — what v1 carries on the member node — and ch. 2.1 has identities
carried verbatim; a key that differs from its declaration in case alone is
found by the case-insensitive consumers and missed by the case-sensitive ones,
which is the same silence along a second road (ch. 4.6 (p), measured).

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

  **Second arm (maintainer decision 2026-09-09): every
  `kenmerkGranulariteit` entry MUST carry a granularity.** The same
  shape one side table over, and a sharper one. That entry declares its
  characteristic **time-dependent by the presence of its KEY** and
  names the grid **in its VALUE**, and the readers split on exactly
  that seam: the verifier's own WF-055 clip check, the reference
  interpreter and the ASP translator all decide temporality on key
  existence, while the shared case loader decides on the value. A null
  value therefore declares one characteristic temporal to three readers
  and non-temporal to the fourth.

  Measured 2026-09-09 on `jutterswet-tegenwacht` with one granularity
  value nulled and nothing else changed: the document was schema-valid
  AND verifier-clean, and then a case DELIVERING the characteristic's
  timeline died at load in both executors with "a characteristic
  timeline on a non-temporal characteristic" — a message contradicting
  the document that produced it — while a case OMITTING that timeline
  ran to completion and answered **60 and 120 where the golden pins 30
  and 30**: silently wrong, and wrong IDENTICALLY in both executors, so
  no differential gate could see it. (One of the two clips is negated;
  with no periods to complement it degenerates to the whole span. The
  failure mode is a wrong number, not the zero one might expect.)

  Closed twice on purpose. `regelspraak-ir-v0.schema.json` drops `null`
  from the enum, so the wire refuses the shape at gate 2; this arm
  refuses it in the verifier for any document that never met the
  schema. Cost: none — 0 of the 534 documents the four zones held
  BEFORE the rule carried a null there (on the branch head the count is
  1: this rule's own negatief witness), and neither emitter can produce
  one today: the
  RegelSpraak emitter guards its write with an explicit non-null test,
  and the Catala emitter's map is empty by construction.

  The case loader's contradicting message followed on 2026-09-11, after
  the third arm had repaired the attribute side: with the key present and
  the value null the shared loader now says "the characteristic is
  declared time-dependent but its granularity is null: the timeline has
  no grid to sit on (WF-059)" (that whole sentence pinned additively in
  the backend's own suite, because no CASE-REPLAYING zone can pin it:
  gate 2 schema-gates `corpus/golden` and `corpus/rood`, so the shape
  cannot sit there, while `corpus/negatief` — which does hold this arm's
  own witness — carries no case files at all and therefore pins no
  message). The wording quoted in the measurement above stays
  for the case it is true of — a characteristic with no granularity key
  at all, delivered as a timeline — and both executors share that loader,
  so they speak either sentence with one voice. The two side tables now
  match on the message as well as on the closure.

  One consequence of tightening the enum, recorded rather than left to
  be discovered. The Catala emitter writes `null` for **every** entry of
  this map (its value expression is a constant `null`), and what made
  that harmless was only that the map it maps over is always empty. That
  dead branch has changed character: before, it would have emitted a
  document with a real SEMANTIC defect; now it would emit one that is
  simply **schema-invalid**, and gate 2 fails on it the moment the
  Catala frontend ever models a temporal characteristic. That is the
  loud, defensible failure — the wire refusing a producer that has not
  caught up — but it is a latent conformance defect in a frontend rather
  than a live one, and it is named here so the eventual gate-2 red is
  read as expected rather than as a regression. The same consequence now
  holds for the attribute map as well — see the third arm.

  **Third arm (maintainer decision 2026-09-10): every
  `attribuutGranulariteit` entry MUST carry a granularity.** The same
  shape on the attribute side table. The seam is structurally identical —
  the verifier's `attr_temporal`, the reference interpreter's evaluator
  and the ASP translator read KEY EXISTENCE while the shared case
  loader's attribute arm reads the VALUE (`Some (Some gran)`) — but the
  fault it produces is a **different class**, and this arm closes on its
  own measurement rather than on the characteristic table's. There is no
  silent wrong answer anywhere: every path dies. What is wrong is the
  DIAGNOSIS.

  Measured 2026-09-10 with one granularity value nulled and nothing else
  changed (pre-arm verifier md5 `f55fdba98ccd5ac68bb99492519aa580`):

  - on a pro-rata base (`anchor/jutterswet-borgdeel`, `borgtarief`) the
    document is schema-valid and the verifier does catch it — but as
    **WF-056**, "pro-rata base 'borgtarief' is not a temporal attribute",
    which names a defect other than the one present;
  - on an ordinary clip base (`jutterswet-tegenwacht`, `wachtvergoeding
    per maand`), where WF-056 does not reach, the document is
    schema-valid **and verifier-PASS** — silent — and a case delivering
    the attribute's timeline then dies at load in both executors with "a
    timeline value on a non-temporal attribute", a message that
    contradicts the document that declares the attribute temporal;
  - the same document with the attribute delivered **scalar** — the shape
    the loader does accept — draws two different diagnoses of one defect:
    the reference interpreter's "temporal attribute … without a
    granularity" and the ASP backend's "the gedurende clip … needs the
    full timeline".

  Closed the same way as the second arm: the schema drops `null` from
  this enum too, so the wire refuses the shape at gate 2, and this arm
  refuses it in the verifier for any document that never met the schema.
  The case loader's contradicting message is corrected in the same
  change — with the enum shut, only a hand-built document can reach that
  arm, and it must then name the real reason: "the attribute is declared
  time-dependent but its granularity is null: the timeline has no grid to
  sit on (WF-059)" (that whole sentence pinned additively in the backend's
  own suite, since no corpus zone can hold a document carrying the shape).
  Both executors share that loader, so they speak with
  one voice there. The third measurement's divergence — scalar delivery,
  two diagnoses — is deliberately NOT touched: both messages are true of
  the document and both are loud, and unifying them is a separate
  question about where a missing timeline is diagnosed, not about this
  declaration rule.

  Cost: none — 0 of the 577 documents across the four zones and the
  parity zone carried a null there before this arm (on the branch head
  the count is 1: this arm's own negatief witness,
  `wf059-attribuut-zonder-granulariteit`), and neither emitter can
  produce one today: the RegelSpraak emitter guards its write with an
  explicit non-null test on this table as well, and the Catala emitter's
  map is empty by construction. That Catala emitter writes a constant
  `null` for **every** entry of the attribute map too, so the latent
  conformance defect recorded above for the characteristic map now
  applies to both side tables: gate 2 fails on that frontend's output the
  moment it ever models a temporal attribute, which is the loud,
  defensible failure and is named here so the eventual red reads as
  expected.

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
  domain, never the inline copy. *One neighbouring gap, recorded here and
  since closed.* **Orderability of the
  element-passing folds.** This rule makes the fold's domain honest, not
  its kind admissible: `EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM` are
  *ordering* folds (ch. 4.3), and Enumeration and SumType are
  equality-only by the orderability paragraph below — so an entirely
  truthful `EERSTE` over an enumeration attribute was verifier-clean and
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
  measured). Two neighbouring gaps were recorded
  rather than claimed shut; one is still open, one is closed. First — still
  open — WF-090's aggregation arm reaches the
  **element-passing** folds only, so a COMPUTING fold (`SOM`, `TOTAAL`)
  over struct operands of two domains is verifier-clean. That is a
  WF-038 operand question — `SOM` demands numeric operands — and it is
  no wrong answer: measured, ref faults fail-fast (`plus: expected a
  number, got {waarde: 120.5, telling: 3}`) and the ASP carrier refuses
  the form ahead of translation. Second — **closed 2026-09-05** — the same
  **orderability** flank
  WF-089 records: `EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM` are ordering
  folds while `StructType` is equality-only (the orderability
  paragraph above), so an entirely truthful struct `EERSTE` with two
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
unit at all. The remaining name, `stelsel`, is a **reference**, and since
2026-09-05 it is crossed: **WF-034 requires a present `stelsel` to name a
declared UnitSystem** (ch. 4.6 (r)). It is the occurrence's one link back to
the conversion table that gives it meaning, WF-009 checks only the table's
internal consistency, and a dangling system name switched the ASP backend's
literal-restating pass off in silence — a measured divergence on a
verifier-clean document. Absence keeps its meaning: `system` is legitimately
null for the built-in `%` and for the bare calendar units, and the rule passes
over those by construction.

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
  drops. **Delta (promotion).** *Scope note (2026-09-05):* this rule walks a
  DECLARED system's own table. The opposite direction — a unit OCCURRENCE
  naming the system it belongs to — is WF-034's since the sixth wave
  (ch. 4.6 (r)); whether the occurrence's `afkorting` is a unit that system
  declares is this rule's neighbourhood and stays OPEN, recorded in the
  ch. 4.6 open list.
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

**Roles and object-type members are separate namespaces.** WF-002 states
that attributes and characteristics are separate namespaces; roles are a
third. A role MAY share its name, case-insensitively, with an attribute
or a characteristic of an object type the same fact type relates. Such a
document is **well-formed**, and no rule above narrows it. The reason is
structural: a navigated read names its role hop and its attribute in
*different* fields — `navigatiePad`, plus the `navigatieBindingen`
extension of ch. 4.1a, against `attribuut` (in v1 the unified `path`
against `attribute`, row 4) — so a role name and a member name never
compete for one wire position. The reference interpreter and the ASP
backend both answer such a document correctly today, with no special
case. It is a REACHABLE but UNINHABITED shape: swept 2026-09-10 over
every corpus zone — 555 documents — the collision occurs zero times, so
no existing case pins this and the paragraph is what states it.

The neighbouring shape — one written name matching *several* reachable
roles — is a different question and keeps its own answer: a loud executor
fault, never a guess (ch. 4.3). Its two arms are pinned unevenly. The
navigation arm is refused statically by WF-053. The case-input arm, where
a case supplies links under a role key that resolves to two roles, is
refused identically by both executors but while LOADING the case, which
the red corpus's expected-fault contract does not reach; that gap is
carded in `backlog/new/20260910-rood-zone-laadfouten.md`.

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
whole-period fold over a navigated SET, which WF-096 makes unreachable
through a well-formed document. Those refusals are pinned by that
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
stripped, adoption total per WF-005). "Fixed start" here means only
`variableStart: false`: the wire carries no fixed start point, and a
fixed-start timeline sits on the calendar grid (`RUNTIME-SEMANTICS.md`
ch. 3.2, "Fixed starting points", for which start points that is and
why a producer refuses the rest).

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

*What the wire checks, spelled out because a day kind's spellings live in two
places.* On the v0 wire the singular name stands in the `dagsoorten` list, and
every REGISTERED spelling — singular and plural alike — stands as a KEY of the
`dagsoortPerEenheid` registry. Until 2026-09-10 the check read the first place
only, so both conjuncts held for the declared name and for no other registered
spelling. The rule now runs over both. Four subcases, covered by three clauses
of this rule plus WF-001's (maintainer decision 2026-09-10 — the key-space
clauses are ARMS of WF-017 and not a rule of their own: it is the same promoted
MUST, checked wherever a spelling is registered):

| # | collision | clause |
|---|---|---|
| 1 | declared name × built-in calendar unit | **WF-017**, over `/dagsoorten` |
| 2 | registered spelling (a registry key) × built-in calendar unit | **WF-017 arm (a)**, over `/dagsoortPerEenheid/<key>` |
| 3 | declared name × declared name | **WF-001** (the `dagsoorten` namespace, ch. 2.1 identity) |
| 4 | registered spelling × another day kind's spelling | **WF-017 arm (b)**, over `/dagsoortPerEenheid/<key>` — in the one form the wire can see: a DECLARED name registered as another day kind's spelling. The collision as such leaves no trace (one value per key), so no clause can reach it |

*Arm (a) — a key that shadows a calendar unit.* Such a key makes the document
contradict itself. `RUNTIME-SEMANTICS.md` ch. 3.8 settles the reading
normatively: a duration's unit is recognised by spelling through the calendar
table, and "a spelling outside the table is a declared day kind". A registry
key says *this spelling is a day-kind unit*; a spelling the table carries says
*this spelling is a calendar unit*. Both at once is not a reading a consumer
can hold, and it is not a defect any one consumer must notice for the wire to
be wrong.

Two of them did not, and two did — *measured 2026-09-10, and it is the second
reason the arm exists.* The verifier (WF-035, WF-038), `ref` and the ASP
backend all consult the calendar table FIRST and reach the day-kind registry
only where the table has nothing, so on those three a key `dagen` changes
nothing at all: `datum + 5 dagen` stays calendar arithmetic. Both Drools
branches reversed the order at all four of their day-kind positions — they
asked the registry before the table — so on the same document they read
day-kind arithmetic where the other three read calendar arithmetic. The
executors SPLIT, which is what makes this a wire question rather than a taste.
*Closed 2026-09-20:* the order is normative (`RUNTIME-SEMANTICS.md` ch. 3.8,
the paragraph on the lookup order) and all four of those positions now read the
table first, so no consumer acts on the contradiction either way — which does
not make the document admissible, and this arm is why it is not.

*Measured 2026-09-10:* before this arm, `"dagen": "schouwdag"` beside the
ordinary entries was verifier-clean, while the same spelling DECLARED was
already refused by clause 1. Here the IR was weaker than its own producer, and
what carries that is the REGISTRATION GUARD rather than the grammar: the
RegelSpraak frontend refuses to register a spelling that coincides with a
calendar spelling, warns, and drops the entry. It compares against all thirty
since 2026-09-20 (the paragraph below); what follows is the measurement of the
day it still knew eighteen, kept because it is what shows the guard and not the
grammar doing the work. *Measured 2026-09-10 over all eighteen as a day-kind
plural:* fourteen do not even parse — the names are grammar keywords, and the
frontend exits non-zero without emitting (`Dagsoort de vaardag (mv: weken)`) —
while four parse cleanly, `uren`, `minuten`, `seconden` and `milliseconden`.
For those four the frontend emits with exit 0 and the guard alone keeps the
spelling out of the registry; its warning is carried in the emitted document's
`diagnostics` array, and since 2026-09-19 it is also printed to stderr, so a
producer reading either surface sees it. Either way no document this frontend
emits can carry the shape; it is wire-only, which is exactly why it must be a
wire rule or nothing. Witness:
`corpus/negatief/wf017-eenheidsleutel-schaduwt-kalendereenheid`.

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
`diagnostics` array, the registry key `wk` and `wk` in `dagsoorten`. Goldens
carry the abbreviations as live calendar units (`jutterswet-vervaltermijn` uses
`wk`, `mnd` and `kw`), so under the lookup-order divergence above such a
document split the executors in earnest — measured on `jutterswet-bergingsklok`
respelled to `wk`: `ref` 2026-03-31 / 2026-02-17 / 5 / 5 against both Drools
branches' 2026-04-15 / 2026-02-01 / 3 / 3.

Both halves moved with the widening, which is what made it a single change
rather than three. The **comparison** is now the table lookup itself rather
than a list beside it, so a rule and the arithmetic it fences cannot come to
know different tables. The **producer** moved with it, spec-first: the
frontend's registration guard compares against the same table, and a declared
NAME that stands in it is an ERROR there — which blocks the emission, because
dropping the name alone would leave its §9.8 definition pointing at a day kind
`dagsoorten` no longer declares. The **executors** moved with it too (the
paragraph above). *Cost, measured 2026-09-20 over the four corpus zones plus
the read-only parity zone, 699 documents:* not one verdict and not one report
line moves, and the twelve stay verifier-clean wherever they are written as
ordinary calendar units. Witnesses: `wf017-eenheidsleutel-schaduwt-kalenderafkorting`
(a key), `wf017-dagsoortnaam-schaduwt-kalenderafkorting` (a declared name), and
`wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm` (the second-plural class,
which the other two do not reach).

*Arm (b) — a declared name that binds to another day kind.* Here the producer
is exactly as weak as the wire. Registration is `putIfAbsent` — first-wins,
silently — so a second day kind whose own SINGULAR name is already registered
as the first one's plural loses its own spelling without a diagnostic.
*Measured end-to-end 2026-09-10* on two day kinds `schouwdag` (plural
`schouwdagen`) and `schouwdagen` (plural `schouwdagens`): the frontend emits
with no warning and no error, the registry binds `"schouwdagen": "schouwdag"`,
and the verifier of the day before reported PASS — so `5 schouwdagen` reckons
with the definition of `schouwdag`. WF-001 does not reach it: the two DECLARED
names differ. What the wire can see is not the collision itself — the map holds
one value per key, so the losing registration is simply absent — but the shape
it leaves behind: a DECLARED day-kind name registered as the unit spelling of a
DIFFERENT day kind. That is the arm: a declared name occurring as a key MUST
bind to itself. Witness:
`corpus/negatief/wf017-dagsoortnaam-bindt-vreemde-dagsoort`, the frontend's own
emission of that source. The producer half — a diagnostic on the silent
`putIfAbsent` — belongs to the frontend and lands there, spec-first.

*Identity.* All three clauses compare under the ch. 2.1 identity (collapse
whitespace, lower case), the same one a consumer applies when it looks a
spelling up in the registry, so the three positions agree on what "the same
spelling" means. It is pinned from the positive side by
`corpus/golden/jutterswet-vormenkabinet`, which registers a declared
`schouwdag` as `"schouwdag": "SchouwDag"`: that entry binds to itself only
through the identity, and a byte-exact arm (b) reports it.

*What arm (b) does not reach.* A registry key that is NOT a declared name and
belongs to another day kind's spelling set stays admissible, for the reason
given with WF-034 (s) below: the wire carries no plural FIELD, only further
keys of this same map, so nothing distinguishes a declared plural from a
derived one or from an abbreviation, and a key rule of that width would have to
fix Dutch morphology inside an engine-neutral IR.

The registry's other half is a different rule and a different question. Its
VALUES are crossed by WF-034 (ch. 4.6 (s)): every value MUST name a day kind
`dagsoorten` declares. That is RESOLUTION — one name against a declaration
table — where WF-017 is COLLISION — two spellings against each other. Neither
doubles the other: the first compares nothing, the second resolves nothing, and
they read disjoint positions of the same entry (WF-017 the key, (s) the value).
Arm (b) steps back where the two would meet: it fires only when the value names
a day kind that IS declared, because an unresolvable value, a null value and
the empty string name no second day kind to collide with and are (s)'s single
diagnosis. The registry's KEYS carry no RESOLUTION rule, deliberately, for the
reason stated with (s).

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

**The unresolved `regels` stubs are PRESENTATION** (maintainer decision
2026-09-05), and this paragraph is their normative status. The v0 wire
carries, beside the resolved rule forms, a top-level `regels` array of
two-field stubs — a `naam` and a `soort`, and nothing else. They are the
build's own index of the rules it parsed: the resolved forms below carry the
whole of the executable content, and a stub adds no relation, no expression
and no target. **They are inert v0 mirror baggage, exactly like the bean
fields of ch. 3.4, and unlike those they are not even a superseded mirror of
something checked: they are NON-NORMATIVE. Nothing in this specification gives
a stub a meaning, no well-formedness rule crosses one, and a consumer reading
one would be reading something this document does not define.** That status is
stated rather than enforced, deliberately and by maintainer decision: a
crossing rule here would fence a field with no consequence, and the spec
discipline of this repository asks for a machine-checkable artifact behind
every normative MUST rather than a sentence that only sounds like one. *The
measurement that settles it, over the 522 documents of the four zones on
2026-09-05:* the zones carry **691** stubs (313 golden, 34 rood, 291 negatief,
53 parity) and **zero readers** — no check in `spec/verifier/` reads the array, the
reference interpreter does not read it, the ASP backend does not read it, and
the Drools backend's loader parses it into its model record without any
transpiler, runtime, graph or case-runner code reading that record back. What
holds the field on the wire is WF-019 alone: serialization preserves it
byte-identically because gate 2 round-trips whole documents, not because
anything consumes it. The consequence for producers is that a stub may say
what it likes and be ignored; the consequence for this spec is that the
naming class of WF-034 stops at the array's edge, which is why it left that
rule's open list rather than gaining a clause there.

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

- **WF-020** Form names MUST be unique (case-insensitive) across all
  **named forms** in a model — the versioned forms of this chapter and the
  `PrioritizedDefaults` groups of ch. 6.1, in one namespace. *v0 mirror
  note:* uniqueness in v0 is a by-product of the name-suffix scheme;
  nothing checks it. **Delta (promotion).** *Widened 2026-09-12* to the
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
  **What this rule is NOT about.** Four other top-level arrays put a name on
  this wire, and the boundary is drawn per array rather than left to be
  inferred (each measured 2026-09-12 over the four zones):
  - `resolvedDagsoorten` — a **day-kind definition**'s name is which day
    kind it defines and lives in the day-kind namespace, not the named
    forms', so its presence is **WF-031**'s clause, beside that rule's own
    uniqueness and its own dead-definition test.
  - `tijdlijnen` — a **timeline**'s name is its identity too, and it is
    already covered: WF-015 keys the declaration on it and every reference
    resolves against it, so a nameless timeline takes every reference to it
    down with it and the document falls on **WF-003** (measured: a nameless
    `het penningjaar` yields `WF-003 FAIL … reference to undeclared timeline
    'penningjaar'`). A second rule here would only re-refuse it.
  - `regelGroepen` — a **rule group**'s name is a label. WF-033 resolves a
    group's MEMBERS by name and nothing reads the group's own, so a nameless
    group is inert rather than ambiguous, and refusing it would be a rule
    without a failure.
  - `regels` — the parse-stage `Regel` **shell index** (ch. 3.1's v0 wart),
    which no check and no executor reads at all and which one frontend emits
    empty by design. A nameless stub is verifier-clean and stays so, for the
    same reason: it names nothing that anything looks up.

  Beside those four, `dagsoorten` is a list of bare strings rather than of
  nodes, and a `null` entry there is likewise admitted today — **stated as
  open rather than closed**: it is the day-kind namespace again, so it
  belongs with WF-031's family and not here, and no producer writes one.
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
| `connectingRole` | RoleBinding? | **extension field** (v0 wire `verbindendeRol`): the connecting relation, with the **recipient role bound** — the bound role's filler is ⊑-compatible with `targetType`, the opposite role's with `sourceType` (WF-047, widened from equality by ch. 2.2). Absent, the relation must be uniquely derivable from the type pair (WF-048); `RUNTIME-SEMANTICS.md` ch. 4.1 |

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
| `subject` | type ref | the creating subject: the population the creation runs over and the type every member value evaluates on. WF-034 requires it both to be PRESENT and to name a declared type (ch. 4.6 (n)) — WF-027 crosses `createdType` and stops there |
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

**The guard is the one position that binds TWO objects.** Everywhere else in
this specification a condition is about the enclosing form's subject; here the
anchor is iterated and the target is reached, and both are in scope. Which of
the two a characteristic check is about is said by its `eigenaar` — the
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
bound role's declared `many`). *Migration:* v0's
`zetVeld`/`joinVeld`/`veld` are derivable from the role bindings by the
backend's own role→field mapping; the derivation moves down into the
backend, out of the IR (the Phase-3 forcing move). Until that bump the
bean fields ride the wire *beside* the symbolic ones. **The symbolic
form is normative and required — it is what executes; the bean fields
are a superseded MIRROR of it, which a consumer may read in addition,
never instead.** That is the sentence WF-028's bean-field-only arm makes
checkable, and the one the reference interpreter and the ASP backend
quote by chapter when they refuse such a record. The mirror is fenced
too: WF-034 requires all four bean fields to be present, to name a
declared role of the relation their symbolic sister names, to name the
side the bean model actually stores the reference on where cardinality
derives one, and to spell it as declared (ch. 4.6 (m)) — a superseded
field is not an unchecked one, and the measurement that forced the point
is that all four were verifier-clean under a nonsense name while the
bean-model executor crashed on each.

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
  MUST differ from the relation of the final hop. And a creation MUST
  carry the symbolic form at all: a **bean-field-only** record — every
  symbolic key absent — is ill-formed. *(Closed 2026-09-04, fifth wave;
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
  golden, rood, negatief and the parity zone carried the symbolic form,
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

- **WF-031** The reference MUST be present, non-blank, and resolve to a
  declared DayKind (*presence added 2026-09-12* — the name IS which day kind
  is being defined, so an absent one defines nothing and is dead in exactly
  the way a bounded window below is dead; it slipped through the resolution
  cross itself, which had nothing to compare. It is deliberately not
  WF-107's: that rule holds of the NAMED FORMS, whose names share the one
  namespace WF-020 keeps unique, and a day kind's name lives in its own,
  which this rule keeps unique); the
  condition MUST be present and **pure** — a day-kind voorwaarde
  references only dates, date-components, eerste-paasdag, literals,
  parameters and other day-kind names, and MUST NOT read an attribute or
  a characteristic (the stratum-0 framing, ch. 5.4: a day kind classifies a
  bare date before the rule graph, with no fact base); the ONE context
  value it may carry is **`de dag`**, the bound day of the definition
  itself — a **session** value (`Rekendatum`, `Rekenjaar`) is refused,
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
  closed set stays WF-098's alone, and the empty half `naam: null` stays
  the open point ch. 4.1 records); there MUST be
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
| Literal | `value`, `type`, `source` | **Delta (row 27):** v0 carries a LEXEME as the value — a string the executors parse — and v1 carries a typed value. Which lexeme, per sort: WF-081 pins the bare number, WF-084 the ISO date, and a `Tekst` literal's value is the string BETWEEN the source delimiters (maintainer decision 2026-09-18). An enumeration value's lexeme is the BARE label, which WF-039 crosses byte-exactly against the declared value list, so the delimited spelling is ill-formed on this wire; the sentence that once stood here — "an enumeratiewaarde keeps its `'…'` delimiters, which readers strip" — described a FRONTEND reader, never the wire, and is corrected in the paragraph below (maintainer decision 2026-09-19). No sort carries a source-shaped lexeme any more: WF-095 admitted the day-first snippet for `DatumTijd` until the emitter wrote it into `waarde`, and since 2026-09-22 pins the ISO datetime alone (card `p5-datumtijd-lexeem`). The unparsed source slice always lives in `source` (`bron` on the v0 wire), which is where a reader that wants the author's spelling looks |
| AttributeReference | `attribute`, `subjectType`, `path` (role steps), `type`, `source` | **Delta (row 4):** v0 splits navigation over `navigatieRol` + `navigatiePad` + a `set` flag (with the invariant that a non-empty path implies a role) and keeps the transpiler-facing `onderwerpBron`; v1 unifies into one explicit path of role steps, with result cardinality derived from the declared role cardinalities. **Landed (2026-08-18):** the symbolic path rides the v0 wire as the `navigatieBindingen` extension field (ch. 4.1a, WF-053); the anchor `jutterswet-kustwacht` executes all three navigated shapes through both executors. *Migration:* the v1 unification drops the written-name fields the bindings supersede |
| ParameterReference | `parameter` (name ref), `type`, `source` | the v0 wire form is already symbolic (the emitter writes the name) — the "embed the model object" wart is in-memory only |
| VariableReference | `name`, `type`, `source` | a rule-local let-binding (§13.4.2.10); binding is frontend scope resolution |
| ContextValue | `name` ∈ evaluation-date, evaluation-year, day-kind day | **WF-098 (below):** the name is a CLOSED SET of exactly three — `Rekendatum`, `Rekenjaar`, `de dag` — crossed BYTE-EXACTLY. Nothing in the document declares a context value, so this is an enumeration, not a resolution. **Delta (row 25):** split from v0's `Constante` grab-bag; the pronoun referents that shared that node are surface forms the frontend resolves away. *v0 mirror note:* the wire carries `Constante` named `Rekendatum` (typed day-precision), `Rekenjaar` (whole number) or `de dag` (typed day-precision); `Rekendatum` is the ch. 8.2 execution-date constant and the index of the ch. 8.3 vintage read (`RUNTIME-SEMANTICS.md`); **`de dag`** is semantically the BOUND DAY of a day-kind definition (ch. 3.7) — the arbitrary day the definition is about, not a session value — spelled as a context value because v0 has no node kind of its own for it; whether v1 gives it one is an open point, deliberately not decided with this row. Its RUNTIME meaning **landed 2026-09-19** in `RUNTIME-SEMANTICS.md` ch. 3.8, with the day-kind duration forms that bind it — not in ch. 8, which is time SELECTION and owns the session values only: `de dag` is the bound day of a day-kind definition, so its meaning is stated where the binding is made, and outside one it denotes nothing and every executor faults by name (red case `contextwaarde-de-dag-buiten-dagsoort`). The converse — a SESSION value inside a day-kind voorwaarde — is refused by WF-031's position arm (ch. 3.7); WF-098 itself stays flat. `Rekenjaar` is admitted-and-unexecuted: no document carries it and the reference interpreter faults on it by name, which is the honest answer, so it stays in the set without a carrier |

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
documents present at that date (176 since) — 23 under a division, one under a division-by-absolute, one under a
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
| `Aggregation` aantal gevulde | the same constant, on its own row because the engine matches on the KIND: a filled count answers a cardinality whatever unit the folded attribute carries (`RUNTIME-SEMANTICS.md` ch. 3.4c). Added 2026-09-20, and not as bookkeeping — until it existed this was the one aggregation whose node type nothing crossed, so a filled count annotated `kg` into an equally-lying `kg` target was verifier-clean and `ref` played it | soort, domain, unit, decimals, wholeness |

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
The divergence is a finding, not a choice. **WF-099 (ch. 4.6) JOINS that
class rather than widening it:** since 2026-09-10 a clamp bound is crossed
against its base through the very same predicate, so a one-sided pair is
admitted at a clamp on the same terms and refused on the same terms.
Measured that day, the verifier-gated zones — golden (166), negatief (359)
and rood (26), 551 documents — carry no such clamp either, while the parity
zone's 30, which no gate puts through the verifier, carry two, both already
non-conforming for unrelated reasons. Admitting the class stays one decision for WF-023,
WF-035, WF-036 and WF-099 together.

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
again, and measured the same way: over all **493** documents of golden + rood
+ negatief + parity that predate that card (154 + 22 + 287 + 30) not one
verdict and not one report moves, and the seven new negatiefs — six under
WF-034, one under WF-028 — are the only additions (negatief zone 287 → 294,
four zones 493 → 500). Its **fix round (2026-09-05)** sharpened (m) where a
lens showed the wave had conceded too much: a bean field must also name the
side the v0 bean model actually derives a reference for (the §13.3.9 to-one
role, where cardinality picks one), must spell it byte-exactly, and must be
present at all — three arms, three witnesses, and again zero over-rejection
over the 500 documents that predate it (negatief zone 294 → 297, four zones
500 → 503). The SIXTH wave (2026-09-05) took the REST of the list that
paragraph left, ch. 4.6 (n)–(r): the eight per-member SIDE-TABLE keys of an
object type, each its own arm (an orphan key is a silent semantic change —
it hangs a granularity, plural or dimension on a member that does not exist
and leaves the intended one at its default, and two of those arms were
measured to split `ref` from the ASP backend, one of them on an anchor
golden); a characteristic check's explicit `eigenaar` against the object-type
table (read as the key of a dependency edge, and silently re-attributed to the
subject by the bean-model executor's null-yielding lookup); a unit's `stelsel`
against the declared unit systems (a measured `ref` ≢ clingo divergence: the
ASP restating pass gates on it and switches off in silence); and the two
EMPTY halves the fifth wave passed over, `subject: null` and
`anderTeFiller: null`. Twelve arms, twelve witnesses, and over-rejection
settled the same way: over the 510 documents predating that card not one
verdict moves and exactly one report does (negatief zone 303 → 315, four zones
510 → 522), and its fix round of 2026-09-06 — which made the eight side-table
keys byte-exact, see ch. 4.6 (p) — adds one more witness and moves nothing
else over all 523 (negatief zone 316, four zones 523). That
is again the list of channels closed **here**, not a claim that no deferral
can be self-served. *Measured, still open* — the residual positions of this
naming class, counted over the 510 four-zone documents that predate the sixth
wave (2026-09-05), so a later count will differ: the sentinel on the 9
fold-/aggregation argument types (the aggregation branch replaces the generic
walk there — its own question, not this descent's);
WF-009's own
question, whether a unit's `afkorting` is one the
system it names declares, which (r) deliberately leaves alone; and WF-007's
precision-form check, which has the identical blind spot
in containers and is its walk to make, not this rule's. (The unresolved
`regels` **stubs** left this list by maintainer decision on 2026-09-05: they
are declared non-normative presentation, ch. 3, with the measurement that
forced the point recorded there. **`Eenheidstelsel.naam` left it on
2026-09-06**: it was one instance of a CLASS — a keyed declaration's redundant
self-name — and WF-094 (ch. 2.1) now closes that class byte-exactly over all
five map-shaped declaration tables at once, with a witness per table. Two
things this entry had wrong while it stood here, both corrected there: the
fifth table is `parameters`, not `tijdlijnen` — a list, whose name IS its
identity and has no key to disagree with — and `parameters` is precisely the
table where a one-character disagreement makes a consumer answer with WRONG
VALUES, so the instance recorded here was the one member of the class with no
reader at all. **`dagsoortPerEenheid` left it on 2026-09-09**: (s) closes the
arm this entry asked for — every entry's VALUE names a day kind `dagsoorten`
declares — on the witness the golden `jutterswet-schouwrooster` supplied, one
field of it twisted. What (s) does NOT take is the map's KEY side, and that is
a decision recorded in (s) rather than a residue recorded here. **The EMPTY
half of the self-name class left it on 2026-09-10**: a keyed declaration whose
`naam` is `null` rather than disagreeing with its key, the (n)/(o) split one
table over, closed by WF-094 clause (d) (ch. 2.1) on all five tables with a
witness each. It never stood here for want of a witness — a null one is one
edit from any golden, as WF-094's five recapitalisations are — but because it
splits the executors the other way round from (n)/(o), two passing silently and
correctly while the bean-model executor crashed in the identifier contract, and
that shape needed a decision of its own. It got one; what the clause did NOT
also do is settle the two maps inside a unit system, which are recorded there,
not here. **`Constante.naam` left it on 2026-09-10**: WF-098 (ch. 4.1) writes
the ENUMERATION this entry said the position needed — the three admissible
context-value names, crossed byte-exactly — so the field that crossed against
nothing now crosses against a set the spec fixes. Two of the three things this
entry recorded held up and one did not. It was right that the ch. 4.1 row
failed to name `de dag`, and right that `Rekenjaar` is carried by no document;
the row settles both, the first by following the wire and the second by
admitting a name no executor implements. It was wrong about the count: 13
occurrences over 5 documents was measured before the rood document
`tijdsduur-reads-van-before-tot` arrived, and the re-measurement of 2026-09-10
finds **14** over **6**. Both figures cover the same set — golden, rood,
negatief and the parity zone — which this document names two ways ("the four
zones" and "the four zones and the parity zone", the same 577 documents at
2026-09-10); WF-098 and its witnesses spell the set out rather than rely on
either label. What the clause does
NOT take is the EMPTY half, `naam: null`, which is recorded with WF-098 and its
measurement rather than here. **The MEMBER half of (q) left it on 2026-09-10**:
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
2026-09-20**: (t) writes the cross the entry asked for. It stood here for the
one reason the others did not — not that the rule was unclear but that **no
document in any zone carried the form**, so a clause would have had no witness
and would have been exactly the spec sentence without a machine-checkable
artifact this repo refuses. The card that closed it did not write the clause
first: it wrote the WITNESS first — `corpus/golden/jutterswet-postbezetting`,
the form's first carrier anywhere — and the clause second, which is also why
the two OCaml executors gained a named arm for the form in the same step. The
entry's own words held up in full; what it did not say, because nothing had
measured it, is that both refusals it counted on were BOTTOM SENTENCES of
`scripts/check-rood.sh`'s list, so the form was not refused as a form at all.)

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
time, ahead of any executor. The Drools backend's literal reader holds the
same grammar, matched whole, since 2026-09-23 (card
`datum-lexeem-hele-match`): until then it searched the value for a date,
so `2024-01-2026` was silently 2024-01-20 on both of its transpiler
branches, and it also read the day-first snippet. Both now yield no
value: at a comparison each branch names the unreadable date lexeme;
elsewhere the rule is refused under its position's own reason, never
answered. The emitter writes the bare ISO lexeme and keeps the source
snippet in `bron`. Scope: day-precision date literals;
datetime lexemes (`DatumTijd`, millisecond precision) are WF-095 in the
paragraph below — the same ISO discipline and, since 2026-09-06, the same
normative standing; since 2026-09-22 also the same single spelling, the
snippet living in `bron` alone.

**The datetime-literal lexeme (WF-095).** A `Literal` whose `type` is
`DatumTijd` MUST carry as its value the **ISO datetime lexeme**, matched
WHOLE: the date lexeme above, the separator `T`, and a time of day.

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
`bron` and nowhere else on this wire.

*Why the snippet was admitted, and why it no longer is.* From 2026-09-06
to 2026-09-22 this rule admitted the snippet as a SECOND spelling, because
the repository's own emitter wrote it: for a day-precision date it wrote
ISO into `waarde` and the snippet into `bron`, but for a `DatumTijd`
literal it wrote the snippet into both (measured 2026-09-06), so a
single-spelling rule would have made the producer non-conforming. The
emitter now writes the ISO lexeme into `waarde` and keeps the snippet in
`bron`, symmetric with the date branch (card `p5-datumtijd-lexeem`,
2026-09-22), and the rule narrowed with it. The narrowing moved no
document: counted that day over the four zones plus the parity zone, the
only `DatumTijd` literals anywhere were golden `jutterswet-meldstip`'s two,
both canonical, and the five this rule's negatiefs carry, none of them the
snippet. The executors' literal readers kept reading the snippet as the
same instant for one more day, as tolerance behind the fence; since
2026-09-23 they refuse it by name like any other form — the reference
interpreter and the ASP backend (which reads the literal through the
interpreter's reader) fault quoting the lexeme, and the Drools reader
answers no value: at a comparison both of its transpiler branches name
the unreadable datum-tijd lexeme; elsewhere the rule is refused under its
position's own reason, never answered (card
`datumtijd-lezers-versmalling`). A reader that admits a form no producer
writes and no verifier admits is behaviour with no witness; the frontend
reads the snippet from its own source with a reader of its own, which no
back-end links. The emitter side is held by golden
`jutterswet-sluitstip`, the first `bron.rgs` golden with a `DatumTijd`
literal: three source spellings of one instant (the `dd. ` prefix with
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

On the IR wire there was no fence at all. The schema types a literal's
`waarde` as a plain string (or null) with no pattern, and the verifier
accepted a document whose `DatumTijd` literal read
`rommel 2024-01-31 12:00:00Z x` — measured 2026-09-06: schema-valid,
`verifier: PASS`. WF-095 makes such a document ill-formed at verification
time, ahead of any executor, exactly as WF-081 and WF-084 do for their
lexemes. The six negatief cases carry one refused field class each
(`wf095-zone-suffix`, `wf095-spatie-scheider`, `wf095-fractiebreedte`,
`wf095-ontbrekende-seconden`, `wf095-omringende-tekst`, and since
2026-09-22 `wf095-bronsnippet`, the day-first snippet the rule admitted
until then).

The passing side is golden `jutterswet-meldstip`: a `DatumTijd`
attribute ordered against a `DatumTijd` literal in the canonical lexeme,
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

**Scope: the PRESENT lexeme.** `waarde: null` is the empty half of the
same field and a different question (the (n)/(o) precedent of ch. 4.6). It
is not held open for want of a witness — a null one is one edit from this
rule's own golden — but because its shape is its own: the schema types
`waarde` as `["string", "null"]`, so a null LEXEME is schema-valid where a
missing `waarde` is refused by gate 2, and this rule's arm reads the
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
so a document nothing on the wire refuses reaches three readers with three
outcomes — the very shape this rule closes for the non-null half. Named
here and left open; closing it is a clause of its own, not a widening of
this one.

What this rule deliberately does NOT do: it does not force the emitter to
write the canonical lexeme. That is the remaining open flank, recorded
here so the next producer knows which form to target — the canonical one
is what this rule names first and what a consumer reads.

**The text-literal lexeme (maintainer decision 2026-09-18).** A `Literal`
whose `type` is `Tekst` carries as its value the PARSED string: what
stands between the source form's delimiters, with the outer `"…"` removed
and every character between them kept verbatim — no unescaping, no
whitespace folding, no case folding. The RegelSpraak string grammar admits
no escape sequence (a `"` always closes the string), so the removal is
total and lossless and an emitted value can never itself contain a `"`.
Unlike WF-081/084/095 this is NOT a well-formedness rule and carries no WF
number: a `Tekst` literal legitimately carries arbitrary text, so no wire
value can be rejected on its shape — a hand-authored document whose value
carries its own outer quotes stays well-formed, and every executor
compares it VERBATIM (ch. 4.1's raw-string comparison). The rule binds the
PRODUCER: an emitter that writes the delimiter-carrying snippet puts a
second spelling of one text value on the wire, which is what this decision
removed. Its machine-checkable artifacts are the emitter witness (`IrEmitt
erTest.tekst_literal_lexeme_is_de_geparste_waarde_snippet_in_bron`, gate
10) and gate 11's byte-identical re-emission of the pinned dumps.

**The enumeration-value lexeme (maintainer decision 2026-09-19).** A
`Literal` whose `type` is an `Enumeration` carries as its value the BARE
label — the text between the source form's `'…'` delimiters, nothing
removed but those two characters. Unlike the text rule above this needs no
new sentence to be normative: **WF-039** already crosses that value
byte-exactly against the declared value list, so a delimited spelling
names no declared value and is ill-formed, and **WF-034** (r) refuses the
unknown sentinel in the `type` beside it. The wire was therefore never
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
emitter witnesses (`IrEmitterTest.enum_literal_krijgt_zijn_domein_van_het_schrijfdoel`
and its three siblings, gate 10) and the golden `jutterswet-vondstklasse`,
which carries an enumeration literal in both positions and replays through
ref and clingo against hand-computed values (gates 3, 5 and 6).

**The validity bound's lexeme (WF-101).** A validity window's bounds
(ch. 3.10, v0 `Geldigheid`) are each either `null` — the open bound — or
the **strict ISO date lexeme WF-084 names above**, matched by that same
grammar and that same calendar: fixed field widths (four-two-two), single
`-` separators, month 1–12, day 1..days-in-month, leap-aware. Nothing else
is a validity bound. It holds at every carrier of a window on this wire —
rule versions, object creations, distributions, consistency rules,
uniqueness rules and their unions, fact creations, defaults groups, and
day-kind definitions, where WF-031 refuses a bounded window outright, so
that position is unreachable in a conforming document and the rule holds
of it anyway rather than of eight carriers out of nine. Scope is the
PRESENT bound: `null` is not the empty half of a field here but the
declared way to leave a side open, so it is admitted by the rule, not
merely unreached by it.

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
negatief cases carry one layer each (`wf101-geldigheidsgrens-tekenjaar`,
`wf101-geldigheidsgrens-kalenderdag`) and fall independently.

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
(golden 169, negatief 364, rood 26 — 559 documents): **1814** bound
positions, **1800** of them `null` and **14** carrying a value. Twelve of
those fourteen predate the rule and spell an admissible lexeme verbatim —
`2025-01-01`, `2025-12-31`, `2026-01-01`, `2026-12-31` over five documents
— and the other two are this rule's own witnesses. The parity zone adds
128 positions over 30 documents, all `null`. The rule finds no document it
has to break.

**The context-value name (WF-098).** A `Constante` — the v0 spelling of a
ContextValue — MUST carry as its `naam` one of exactly **three** names, and
MUST spell it **byte for byte**:

```
Rekendatum   -- the ch. 8.2 execution date, and the index of the ch. 8.3
                vintage read (day precision)
Rekenjaar    -- the execution year (whole number)
de dag       -- the bound day of a day-kind definition (ch. 3.7): the
                arbitrary day the definition is about (day precision)
```

Nothing else is a context value. Not a fourth name, not a recapitalisation of
one of these three.

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
differential — of the three `Rekendatum` carriers, the anchor golden is
excluded by the Drools loader for an unrelated reason and the other two are red
documents, required to fail — and a purpose-built player would be a document
whose entire content is the defect. Closing the wire is the route taken instead
of adding one, and the residue is stated rather than hidden: a HAND-BUILT
document that skips the verifier still reaches the executors with a
non-canonical name and still splits them. The rule bounds conforming documents,
not every input a runner can be handed.

**Why the set holds three and not two.** The row above enumerated `Rekendatum`
and `Rekenjaar` while the wire's commonest context value is `de dag` — measured
2026-09-10, the four zones plus the parity zone hold **14** `Constante` nodes
over **6** documents in exactly **two** spellings, `de dag` (11, in 3
documents) and `Rekendatum` (3, in 3 documents). Mirror-then-diverge decides
that: the wire is the v0 fact, so the row follows it and the set admits `de
dag`. What `de dag` IS remains what ch. 3.7 says — the bound day of a day-kind
definition, not a session value — and giving it a node kind of its own is a v1
question this clause does not answer.

The set is **flat**: any of the three names at any expression position the walk
reaches, with no position rule beside it. That flatness had a measured flank —
a `Rekendatum` inside a day-kind CONDITION was verifier-clean (measured
2026-09-10, one field of `jutterswet-schouwrooster` turned), because WF-031's
purity list (ch. 3.7) named dates, date-components, eerste-paasdag, literals,
parameters and day kinds but no context value at all, and its walk read a
`Constante` name-blind. **Closed 2026-09-19, and not here**: the position
rule belongs to WF-031, which owns the day-kind voorwaarde, and it became
answerable only when the duration forms started evaluating that voorwaarde
per calendar day (`RUNTIME-SEMANTICS.md` ch. 3.8). WF-031's arm admits `de
dag` — it IS the bound day — and refuses the two session values, naming them
and nothing else; negatief `wf031-sessiewaarde-in-dagsoort` is the 2026-09-10
mutation turned into a witness, and the arm leaves WF-098's own witness
`wf098-onbekende-contextwaarde-naam`, whose `de dagX` sits in exactly that
position, reporting WF-098 alone. **This clause stays flat**: what `de dag`
means OUTSIDE a day-kind voorwaarde is a runtime fault in every executor (red
case `contextwaarde-de-dag-buiten-dagsoort`) and not yet a static rule — the
mirror arm, recorded and open.

**`Rekenjaar` stays in the set without a carrier.** No document in any zone
carries it, and the reference interpreter faults on it by name ("not exercised
by any corpus case"). That fault is the honest answer for a name the spec
admits and no executor implements, so the name is admitted-and-unexecuted
rather than dropped: removing it from the set would make a document ill-formed
for using a construct ch. 8.2 defines.

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

**Scope: the PRESENT name.** A `naam` of `null` is schema-valid — the field is
typed `["string", "null"]` — and is the empty half of the same field, the
(n)/(o) precedent of ch. 4.6. This clause reads the present name only. Measured
2026-09-10 on a one-edit twist of `anchor/jutterswet-penningvintage`: the
verifier passes it, `ref` and the ASP backend both fall through to their
generic refusal (`expression kind not implemented` / `not translated`), and the
Drools readers compare a null against their literals and reach neither branch,
degrading the operand to a marker. Three fall-throughs and no dereference —
unlike WF-095's null lexeme, which one reader dereferences — so the shape is
recorded here with its measurement rather than folded in. No document in any
zone carries it.

**The one producer route that emits a fourth name was already ill-formed.**
The RegelSpraak frontend constructs a `Constante` named `hij` for the animate
pronoun, and the resolver normally resolves it away: over the 38 goldens with a
committed source, and the 14 whose source writes `hij`, not one emits a
`Constante` at all. It can leak — measured 2026-09-10, `indien hij gelijk is
aan hij` emits two of them with `diagnostics: []` — but it leaks carrying the
UNKNOWN-TYPE SENTINEL (`Onbekend`, "niet opgelost"), which WF-034's type
prologue already refuses at the same node. That is structural, not a sample:
the frontend has exactly THREE `Constante` construction sites and the `hij`
one hard-codes the unknown type, so no `hij` reaches the wire well-typed.
This clause therefore cuts no conforming producer route: it adds a second,
name-level line at a position the verifier rejected already, and the
frontend's own pronoun handling is untouched by it.

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
| `kind` | sum, total, count, minimum, maximum, first, last, **filled count** (the last a uchar dialect kind, `CONSTRUCTS.md` row 1 — see "The filled count" below) |
| `arguments` | one set-valued operand (instance fold), one `InstanceSetUnion` (the §5.7 concatenation, count only — see "The instance-set union" below), or ≥ 2 listed operands (value fold). **The listed form is the VALUE folds' alone since 2026-09-20**: under `AANTAL` and `AANTAL_GEVULDE` it is refused by WF-037, so the §5.7 concatenation reaches the count through the union node and never through this list. **Neither count has a sourceless form since 2026-09-23**: a count of one operand or none MUST carry `elementType` (WF-037), the union being the one exit because its selections carry the population and WF-110 (c) forbids a second one here |
| `elementType` | the instance type ranged over (instance folds) — the POPULATION, extensionally (ch. 2.2): the contributors are the instances the source relation reaches whose type is ⊑ `elementType`, so one it reaches that this type does not cover is not folded. Since WF-047's "agree" became ⊑-compatibility the source role's filler may be a strict SUPERTYPE of it, and reach and population are then genuinely different sets; witness `golden/jutterswet-munttelling` |
| `filter` | Condition? — the die/dat-subselection, evaluated PER CONTRIBUTOR (the element is the condition's subject; a leeg operand fails it, ch. 1.2d; an all-excluding filter leaves an empty source — the sum fallback fires, a count reads 0). **Landed (2026-08-19):** executes in both executors (`jutterswet-strandgoed`, an emitter golden; `RUNTIME-SEMANTICS.md` ch. 3.4a). The reference interpreter carries the full condition set; the ASP backend's corpus-exercised fragment faults loudly on the rest — the standing executor asymmetry, differential-gated over the corpus. The SAME field also carries the §7.1 gedurende timeline clip (on the temporal total, where the element type is the subject itself) — a timeline operation, never an instance subselection. **Landed (2026-08-19, WF-055):** the clip executes in both executors (`jutterswet-getijdenwacht`; `RUNTIME-SEMANTICS.md` ch. 3.6): intersect the full value timeline with the condition's true-periods (an own temporal characteristic, or a per-segment comparison against a scalar bound), then total whole units per clipped segment — floor, never pro-rata; a gap in the condition clips away; a fully clipped-away or absent timeline totals 0, never leeg. The negated clip (the complement) and the Gelijk/Ongelijk comparison clip execute in the reference interpreter only — the ASP side faults loudly on both. A negated characteristic subselection never reaches the wire executable (v0 marks it Onopgelost) |
| `emptyFallback` | expression? — **sum only**: the §5.8.2 "of … als die er niet zijn" clause |
| `dimensionSelection` | ? — **sum/min/max only**: fold over an ordered label selection instead of instances (dialect, row 28) |
| `sourceRole` | RoleBinding? — **extension field** (v0 wire `bronRol`), instance folds only: the connecting relation, with **the role whose filler instances the fold RANGES OVER bound** — the reach, which `elementType` then narrows to the population (ch. 2.2); the opposite role's filler is the fold's subject type (WF-047, ⊑-compatible since ch. 2.6, so the bound role's filler may be an ancestor of `elementType`). Absent, WF-048's unique derivability applies; a binding on a non-fold is ill-formed (WF-047) |
| `sourcePath` | RoleBinding list? — **extension field** (v0 wire `bronPad`), the MULTI-STEP range of a set source: the relations walked subject-outwards, each step spelled exactly as `sourceRole` is. Two or more steps; the one-step range IS `sourceRole` and the two fields are mutually exclusive (WF-102) |
| `type`, `source` | — |

Empty-source results differ per kind — the som/totaal split and the
no-fallback rule for min/max/first/last are `RUNTIME-SEMANTICS.md`
ch. 1.2, mirrored in the matrix (row on TOTAAL, `CONSTRUCTS.md` ch. 2.1).

**The anchored fold source (WF-083).** An instance fold's source does not
move: it is read at one type, fixed by the fold, and the argument's own
fields are description of that read, never a second way to choose what is
read. Two consequences, one rule.

**What a fold SOURCE is.** The rule below speaks about the **single**
argument of an instance fold — the argument that IS the folded set's
source. A fold carrying two or more arguments is the listed value fold
(§5.8.3, MIN/MAX/EERSTE/LAATSTE over listed operands); it has no source at
all, its `elementObjecttype` is vestigial where the resolver stamps one,
and its operands are ORDINARY READS, evaluated one by one against the rule
subject and governed by WF-053 like every other read. Everything below is
scoped to the single-argument shape, and that scope is normative: a
document whose listed operand navigates is well-formed, pinned by
`corpus/golden/jutterswet-sluitsom` through `ref`, the ASP backend and both
Drools transpiler branches.

*The anchor.* The type at which the source is read is the
`elementObjecttype` when the argument is the **set** read ("het bewaartarief
van alle vondsten van de Jutter": each member of the folded set is asked for
the attribute, and the declaration that answers is the element type's), and
the fold **subject** otherwise — the non-set source is the subject's own
read, which is what the ch. 3.6 temporal total totals and what the row-1
wart's written role name resolves from. The argument's `objecttype` MUST
name that anchor.

It is not a free annotation, and the precise reason is worth stating,
because the obvious one is false. The field is not ignored: at
`set = false` the ASP backend's `fold_operand_otype` RETURNS it as the
anchor, and `ref`'s temporal-total arm guards on `norm aot = norm
self.otype` — both read it. What neither does is let it CHOOSE where the
read happens. **It is a guard, not a selector**: it may only confirm the
anchor the fold has already fixed. A spelling that disagrees is refused
outright by `ref` (the arm's guard fails and the fold falls through to its
loud fault) or resolved by the ASP backend against a declaration on a type
the fold never walks. At `set = true` the field is inert in both — `ref`'s
set-source arm binds no `objecttype` at all and every ASP read hangs on the
element variable — which is not a reason to leave it free but the reason to
pin it: a field nobody reads is a field that can say anything.

*The written role.* The written form "het aantal `<rol>` van de
`<onderwerp>`" may name the fold subject by its OWN role name ("het aantal
vondsten van de aanmelder", evaluated on the Jutter who *is* the aanmelder).
The v0 wire then carries that written role in the fold-source argument's
`navigatieRol` — ch. 4.1a's field, but here without its meaning: it is
presentation of the written form, not navigation. No hop happens, the fold
stays anchored on its subject, and the v0 production runtime ignores the
field on this argument. That presentation belongs to the **row-1 wart**
alone — the instance-fold source spelled with the unknown sentinel on its
own `type`, the one source that names a role instead of reading an
attribute. There a `navigatieRol` is admissible **iff** the name denotes the
subject: it matches, case-insensitively on declared name or declared plural,
a role that the **fold subject** itself fills in some declared fact type.
On any other fold source a `navigatieRol` is navigation, which a fold source
never does (its relation is `sourceRole`; ch. 4.1a's emitter discipline).
That is not contradicted by a listed value fold whose operand carries one:
those operands are not sources, and a `navigatieRol` there is an ordinary
written hop that WF-053 resolves and constrains exactly as it does outside a
fold.

- **WF-083** A fold SOURCE — the argument of a **single-argument** instance
  fold — is ANCHORED and MUST NOT navigate. A fold with two or more
  arguments is the listed value fold (§5.8.3); its operands are ordinary
  reads governed by WF-053, and this rule says nothing about them.
  (a) A non-empty `navigatiePad` is ill-formed. (b) A `navigatieRol` is
  admissible only on the v0 instance-fold source (the row-1 wart, spelled
  with the unknown sentinel on the argument's own `type`), and there only
  when it is self-denoting — it names a role (ci, declared name or declared
  plural) that the **fold subject** itself fills. An away-pointing
  `navigatieRol` (naming only roles filled by other types, or no declared
  role at all), and any `navigatieRol` outside the wart, are ill-formed.
  (c) The fold source's `objecttype` MUST name the fold's anchor: the
  `elementObjecttype` when `set`, the fold subject otherwise. **A
  `sourcePath`'s INTERMEDIATE types are range, never anchor
  (2026-09-11).** The path (WF-102) introduces a third class of type into
  this position — the types the walk passes THROUGH — and the anchor is
  unmoved by them: every member of the folded set is still an
  `elementObjecttype` instance and every declaration is still looked up
  there, so a source `objecttype` naming an intermediate type is
  ill-formed exactly as any other non-anchor name is
  (`wf083-vouwbron-anker-op-tussentype`). The clause needs no new arm for
  it; it needs to have CONSIDERED it, because the shape where the anchor
  and the range diverge did not exist before the path did.
  Companion: WF-053 over the listed fold's operands, applied at the
  aggregation position for the same reason WF-089 and WF-092 are — the
  fold branch of the walk replaces the generic expression walk there, so
  without the repetition a listed operand is the one read position in the
  IR whose hop resolves against nothing
  (`wf053-geleste-vouwoperand-onverbonden-hop`). WF-096 is repeated there
  for the same reason and refuses a SET reading at a listed operand: it is
  an argument without being a source (`wf096-setlezing-geleste-operand`).
  Semantics: `RUNTIME-SEMANTICS.md` ch. 3.4a — the self-denoting count
  folds the subject's own linked instances exactly as the bare
  subject-anchored form does.

  *Wave note, 2026-09-05.* (b)'s anchor moved from the argument's own
  `objecttype` to the fold subject, and (c) is new. Both close the same
  defect from two sides: the verifier read a field neither OCaml executor
  reads in this position, and crossed nothing against the fields they do.
  Measured, each with its negatief witness and an arm-exact mutant, over
  golden + rood + negatief + parity: **not one verdict of the 504 documents
  predating this card moves** — the sole change is one sentence of the
  existing `wf083-fold-source-navigates-away` report, whose verdict is
  anchor-independent because its operand already spells the subject; the
  four new witnesses take the negatief zone to 302 and the four zones to
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
  is the seventh wave's; and a single argument at a NON-aggregation kind, a
  builtin carrying an `elementObjecttype`, has no source either and is the
  seventh wave's too. Nothing is reported twice and nothing falls between
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
  already fenced elsewhere and still are: an `elementObjecttype` equal to the
  fold subject is WF-048 (`bindingless instance fold: no fact type relates
  'Jutter' to 'Jutter'`), an operand whose `objecttype` is the element type
  rather than the anchor is WF-083 (c), and a `bronRol` beside it is WF-047
  (`the written fold source … does not denote the bound role …`). THREE were
  not fenced at all — the degraded own read, the wart verbatim, and the same
  read spelled `set: true` — and the second of them CRASHES the third
  executor. Leaving them open would have kept a live one-answers-two-refuse
  class, complete with a crash, behind a boundary that had just been declared
  exact; closing them moves zero documents. That asymmetry decided it.

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
  | listed operand, the UNSPELLED TWIN (`"type": null`) | PASS | **PASS** — it plays, on the declaration | FAULT (`mixed date/non-date operands …` on the date fold, `expression kind not translated` on the numeric one) | **PLAYED** on the date fold; **ERROR** `NullPointerException: Cannot invoke "…Datatype.eenheidOf()"` on a unit-bearing numeric operand |

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
  (`NullPointerException` on `Datatype.eenheidOf`), and declined as a form —
  and no executor that carries the shape across the board. That spread is the
  argument for refusing it at the wire instead of in four places. It is also
  why the earlier "the two OCaml executors refuse every one of them with the
  SAME fault" no longer holds as stated: it is true of rows 1–5 and FALSE of
  row 6, where `ref` plays. The decision covers all six anyway, and row 6 is
  the reason it must — a form two executors answer, agree on, and the third
  crashes on is not a form a specification can leave to the consumers. Seven
  negatiefs pin the rows: `wf034-sentinel-gelest-vouwoperand`,
  `wf034-sentinel-gelest-wratoperand`, `wf034-sentinel-geleste-setlezing`,
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

  **The regression boundary, counted 2026-09-06 over golden + rood +
  negatief + parity (533 documents before the witnesses, 537 after).** The
  four zones hold 71 unknown-sentinel nodes, of which **65** sit on a `type`
  slot; the other 6 are declaration-layer or payload positions, all in
  already-rejected negatiefs. Of the 65: **61** are fold arguments (every one
  of them under an `elementObjecttype`), **60** of those the single-argument
  wart — untouched by this wave — and exactly **ONE** a listed operand. The
  remaining 4 are not fold arguments: one rule TARGET in an already-rejected
  negatief, two plain reads, and the own `type` of the composite one of those
  reads feeds; all four are already refused by the walk's prologue or by the
  target clause. So the whole population this wave newly reaches is that ONE
  listed operand, and it sits in the PARITY zone, on a document that already
  carries a co-firing WF-034 at the very same pointer — its verdict does not
  move, its report gains one line (33 violations to 34). Swept before and
  after over all 537 documents: the three new rood-eerst witnesses turn from
  PASS to FAIL, that one parity line appears, and **not one golden, red or
  negatief report changes a sentence**. One bookkeeping line belongs beside
  it, the same one flank (2) carries: **no gate runs the verifier over the
  parity zone** — gate 3 walks the three corpus zones — so the parity
  measurements here are measurements and never pins, and gate 11 (which
  replays that zone through the frontend and the toolchain-pinned dumps, and
  never through the verifier) is unmoved by this wave, measured and not
  assumed.

  *The fix round's own boundary, counted the same way (546 documents after
  its three witnesses).* Two populations, both counted rather than assumed.
  The UNSPELLED TWIN: the four zones hold exactly **three** value nodes
  carrying an explicit null `type` — one `AttribuutRef` (a rule TARGET, in
  `wf034-regeldoel-zonder-type`), one `Literal` (`wf034-typeloos-literal`)
  and one `Functie` (`wf038-tijdsduur-zonder-node-type`) — every one of them
  in a negatief that is already rejected for exactly that, and **zero** of
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
  violation count stays at 34. No golden, no red case, no other negatief
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
  way: a second already-rejected document, `wf083-vouwbron-anker-op-operand`,
  gains a co-firing line beside the WF-083 verdict it keeps, and no golden,
  red case or other negatief moves a VERDICT. Four negatiefs do move a
  sentence, and that is the MESSAGE rewrite rather than this predicate: the
  clause had to name its kind half, so every document already citing WF-096
  restates one line.

**The wart is a COUNT source (WF-093).** The row-1 wart — the source of a
**single-argument** instance fold, spelled with the unknown sentinel on the
argument's own `type` — carries no datatype, and the rule keys on exactly
that: the SENTINEL, never on what the `attribuut` slot spells. Three
spellings of it are measured, and the slot says something different in each.
The BARE one names a ROLE, and is v0's wire presentation of "het aantal
`<rol>` van de `<onderwerp>`"; the SET one names an element ATTRIBUTE; and
the frontend's own route (below) names a CHARACTERISTIC. What the source
hands the fold is, in all three, the linked INSTANCES rather than a value
per contributor. That is exactly what `AANTAL` consumes, and exactly
what no other fold kind can use — `SOM`, `TOTAAL`, `MINIMUM`, `MAXIMUM`,
`EERSTE` and `LAATSTE` read a value PER CONTRIBUTOR, and the wart hands
them members instead. **Maintainer decision, 2026-09-06: the wart is a
count source, in BOTH wire spellings** — the bare one (`set: false`, no
`navigatieRol`) and the set one (`set: true`, `objecttype` =
`elementObjecttype`, with or without a subject-denoting `navigatieRol`:
that field is the ASP backend's fence, not this rule's, and the sub-class
without it is inside the rule on purpose — see below). `set` is not a
second reading of the source; the sentinel decides, and `set` only
describes.

- **WF-093** A single-argument instance fold whose SOURCE is the row-1
  wart — the argument carrying the unknown sentinel on its own `type` —
  MUST be of kind `AANTAL`. Under any of the other value-folding kinds
  (sum, total, minimum, maximum, first, last, and — since 2026-09-20 —
  filled count) the document is ill-formed,
  in either spelling: `set` does not enter the rule. That list is an
  enumeration of the family and grows with it; the rule's MECHANISM never
  moved when the family did, which is what `wf093-aantalgevulde-wrat`
  measures: the new kind fell under this rule because it reads a value per
  contributor, and no guard, message or clause of it was rewritten. A fold of two or more
  arguments is the listed value fold (§5.8.3), whose operands are ordinary
  reads with no source and no role name; this rule says nothing about them
  — the same scope WF-083's family carries, for the same reason.
  Semantics: `RUNTIME-SEMANTICS.md` ch. 3.4a — the count folds the
  subject's linked instances. That chapter is count-only: it says nothing
  about a value fold over this source in either direction, which is why
  this is a well-formedness rule and not a semantics clause.
  **Division of labour with WF-096, since 2026-09-19.** This rule sends the
  wart to `AANTAL` in both spellings, and sending is not admitting: it says
  which kind the sentinel demands and stops there. The SET spelling of the
  wart — the sentinel AND `set: true` under
  a count — is refused by **WF-096**, whose exemption narrowed to the six
  value folds that day, so the two rules now divide the wart's spellings
  between them: the bare one is admissible and is this rule's business, the
  set one is ill-formed and is WF-096's. Under a NON-count kind both
  spellings are still this rule's, which is what the sentence above means
  by "`set` does not enter the rule" and is why that sentence stands
  unamended (negatief `wf096-aantal-setbron-wrat`).

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
(`heeftLid`,
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java:620`;
the gate at
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java:470-481`),
while the reference builder finds no attribute datatype and falls back to
the sentinel (same file, :1663). Emitted and checked: "de som van het
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

*The regression boundary, counted 2026-09-06 over golden + rood + negatief
+ parity.* Of the **58** single-argument wart fold sources sitting under an
`elementObjecttype`, **54** predate this rule and every one of them is an
`AANTAL`: 49 bare (`set: false`, no `navigatieRol`), 4 bare carrying a
self-denoting `navigatieRol`, and 1 with `set: true` — the already-rejected
negatief `wf083-vouwbron-anker-op-operand`. There is no value fold over a
wart anywhere in the corpus; the form is hand-built by construction. Swept
before and after over all 527 documents of the four zones, exactly the four
new witnesses change verdict and not one existing report changes a
sentence.

**The set reading is the SOURCE's field (WF-096).** `set: true` on an
`AttributeReference` says the read yields the members' values, one per
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
`AANTAL_GEVULDE`, over a set source; the listed spelling of THAT is
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

- **WF-096** An `AttributeReference` with `set: true` is admissible ONLY as
  the SOURCE of a single-argument **VALUE** fold — the argument that IS the
  folded set's source under one of the **seven** value-folding kinds
  (ch. 4.3; six until 2026-09-20, when the filled count joined them — it
  reads a value per contributor and tests it for presence, which is what
  this exemption is about, and on it a set source is not merely admissible
  but required by WF-037). At
  every other READ position the document is ill-formed: as a whole rule value,
  as an operand of a composite node, inside a condition, as an operand of the
  listed value fold (§5.8.3, whose operands are ordinary reads with no
  source), and — since the maintainer decision of 2026-09-19 — as the source
  of a COUNT, which folds the contributors themselves and so reads no value
  per contributor. The word READ is the
  rule's scope and not a hedge: a rule TARGET is a WRITE position, this
  clause does not reach it, and the scope note below states what is measured
  there instead — a normative MUST that outran its verifier check would be
  the spec-level "a green subset is not green". The admissible position is
  WF-093's pair MINUS `AANTAL` — a single argument under one of the seven value
  folds (six until 2026-09-20; ch. 4.3) — and the one kind of daylight between the two boundaries is where
  the kinds differ rather than where the rules do: at the source the sentinel
  decides the KIND (WF-093) and `set` DESCRIBES a reading only a value fold
  performs; away from the source the sentinel is WF-034's seventh wave and
  `set` is this rule's. So the sentinel fence still exempts the whole folding
  family (the count source is spelled with the sentinel, ch. 4.3) while this
  fence exempts the value folds alone. No
  semantics clause corresponds: `RUNTIME-SEMANTICS.md` says nothing about a
  collection in a scalar slot in either direction, which is why this is a
  well-formedness rule and not a semantics clause — the same reason WF-093
  gives. What it DOES say about counting — a count folds instances, ch. 1.2e
  and ch. 3.4a — is the reason the boundary narrowed, and it needed no
  amendment to say it.

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
recorded one-token fork held by the maintainer. The negatief emissions stay
verbatim from before the diagnostic — they are this rule's evidence.
`ref`'s fault, the ASP refusal and the Drools skip stay where they are, a
second line of defence rather than dead code, on the house pattern: the form
no longer reaches them through a well-formed document.

*The regression boundary, counted 2026-09-09 over golden + rood + negatief +
parity (560 documents).* Exactly **one** document predating this rule carries
an `AttributeReference` with `set: true` outside a fold source: the negatief
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
never a pin. The clause is stated in TWO places — the generic expression walk
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
of them in the two zones that must stay clean (golden and rood) — and not one
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

**The filled count (`AANTAL_GEVULDE`, v1 `filledCount`).** *(Maintainer
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
the same order the `eerste`/`laatste` widening of 2026-09-04 took.

The node is the existing `Functie`, in the instance-fold shape. **No field
is added and no field is given a new meaning:**

| Field | On `AANTAL_GEVULDE` |
|---|---|
| `arguments` | exactly ONE, an `AttributeReference` with `set: true` — the set source |
| `elementType` | required: the population, exactly as ch. 3.4a reads it for every instance fold |
| `filter` | the die/dat subselection, per contributor, unchanged |
| `emptyFallback` | always absent — a count has no empty to fall back from |
| `dimensionSelection` | always absent — **postponed, not rejected** (below) |
| `granularity` | always absent — it buckets nothing |
| `type` | a whole, unitless number: a cardinality — **derived**, so the annotation is crossed against it under WF-091 exactly as a count's is, never believed (negatief `wf091-aantalgevulde-eenheid`) |
| `sourceRole` / `sourcePath` | the range, one step or many, exactly as on the value folds (WF-102) |

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

**WF-096's exemption is SEVEN value folds, not six** (from 2026-09-20). That
is the application of the 2026-09-19 narrowing and not an exception to it.
The narrowing's question is "does this kind read a VALUE per contributor?",
and what left the wire that day was a `set` field under a node whose
semantics fold CONTRIBUTORS. This kind is a count in its RESULT and a value
fold in its READING, and WF-096 fences the reading — so a set source is not
merely admissible here, it is REQUIRED (WF-037 below). WF-093's boundary is
unmoved and untouched: its pair is still "a single argument and a folding
kind", so the row-1 wart under this kind is ill-formed exactly as it is
under the other six, and the rule needed no amendment to say so
(`corpus/negatief/wf093-aantalgevulde-wrat`, whose diagnosis moves from
WF-034 to WF-093 on the widening alone).

**WF-037 carries the shape, in three arms, and two of them demand a form
instead of forbidding one** — the only arms of that rule that do, and they
are stated in ch. 4.6. (i) Exactly ONE argument: there is no listed form of
this kind, because there is no written form for one to mirror
(`wf037-aantalgevulde-tweede-argument`). (ii) An `elementType`: it is an
instance fold and without one its argument is no source at all
(`wf037-aantalgevulde-zonder-elementtype`). (iii) That one argument MUST be
a set reading (`wf037-aantalgevulde-niet-setbron`). Arm (iii) is what makes
the non-set spellings unreachable: the subject's own read has ONE value, so
its filled count is 0 or 1 — a form no author writes — and the bare role
source hands the fold instances instead of values, which `ref` already
refuses at evaluation (`value aggregation over a bare role source`). The
fence now stands on the wire, where it is checked before anything runs. The
three facets the kind never carries are refused by WF-037's existing,
positively written clauses without an amendment, and each has its witness:
`wf037-aantalgevulde-leegwaarde`, `-dimensieselectie`, `-granulariteit`.

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
same arm that has always crossed `AANTAL`'s** (ch. 4.1, the composite
derivation inventory; negatief `wf091-aantalgevulde-eenheid`). Without that
cross this kind would be the one aggregation whose node type nothing checked:
measured 2026-09-20, a filled count annotated `kg` and written into an
equally-lying `kg` target was verifier-CLEAN while the identical lie under
`AANTAL` was WF-091, and `ref` then played it — a unitless 1 landing in a
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
| `bron` | provenance of the written concatenation |

| Field of `InstanceSetBranch` | Meaning |
|---|---|
| `elementType` | the population this selection ranges over, read exactly as ch. 3.4a reads an instance fold's — and the SAME for every selection of one union (WF-110 (d)) |
| `sourceRole` | RoleBinding, **required**: the one-step relation this selection walks from the count's subject. No bindingless fallback and no `sourcePath` (below) |
| `filter` | Condition? — this selection's own die/dat subselection, evaluated PER CONTRIBUTOR with the element as its subject, exactly as a fold's `filter` is |
| `bron` | provenance of the written selection |

The node carries **no `type`**. It denotes instances and not a value, so
there is no annotation to believe and none for the ch. 2.10 consumer clause
or the gate-13 twist harness to reach; WF-034's type prologue exempts it by
node kind, exactly as it exempts a `Lambda`. The count above it derives its
own whole, unitless cardinality as it always has.

*Its position is ONE position.* The union is admissible as the SINGLE
argument of an `AANTAL` aggregation and nowhere else — not as a rule value,
not as an operand of a composite node, not as an operand of a condition, not
under any other kind. That is the shape of rule WF-096 draws for the set
reading, for the same reason: the one node that consumes a collection is the
node the collection belongs to, and everywhere else a collection stands where
a value is read.

**The clause is about the UNION node and not about the count above it**, and
the distinction is worth spelling out because the two readings differ and only
one is meant. "Not as an operand of a condition" forbids the BARE union in a
comparison; it says nothing about a COUNT over a union, which is an ordinary
whole-number expression and stands wherever any other count stands — in a
rule's value, in a `voorwaarde`, on either side of a comparison. That form is
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
omission.* A multi-step range under a COUNT is where WF-096 and WF-102 meet
(ch. 3.4b, consequence 2): a path is admissible only on the set source, and
a set source is not admissible under a count, so neither spelling of a
counting fold over a union of intermediates survives. Admitting a
`sourcePath` at a selection would settle that question by side effect — the
union becoming the back door through which a refused single-source shape
returns — and one card draws one boundary. A selection therefore carries a
one-step `sourceRole`, required, and the multi-step question stays exactly
where 2026-09-19 and 2026-09-11 left it. Named open flank, not a closed one.

- **WF-110** An `InstanceSetUnion` (ch. 4.3): **(a)** is admissible ONLY as
  the single argument of an `AANTAL` aggregation — at every other position
  the document is ill-formed (`wf110-unie-als-regelwaarde`,
  `wf110-unie-onder-een-builtin`). The clause is about the UNION node: a
  COUNT over a union is an ordinary whole-number expression and stands
  wherever any other count stands, a `voorwaarde` included
  (`jutterswet-unievoorwaarde`); **(b)** carries at least TWO selections,
  since one selection IS the ordinary count and already has a spelling
  (`wf110-unie-een-tak`); **(c)** the count carrying it names no source of
  its own — no `elementType`, `sourceRole`, `sourcePath` or `filter` on that
  node (`wf110-unie-naast-vouwbron`); **(d)** every selection names the SAME
  `elementType`, and it is declared (`wf110-unie-ongelijke-elementtypes`;
  an undeclared one is WF-034 at the new position); **(e)** every selection
  names its `elementType` and its `sourceRole`
  (`wf110-unie-tak-zonder-elementtype`, `wf110-unie-tak-zonder-bronrol`),
  and that binding resolves under WF-047 at the new position, as WF-102 (d)
  applies it per step. Semantics: `RUNTIME-SEMANTICS.md` ch. 3.4d.

**The fold source's RANGE is a PATH (WF-102).** *(Maintainer decision
2026-09-11.)* A set source describes its range with ONE relation:
`elementObjecttype` plus `sourceRole`. A written range that stacks two
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
and the LAST step's bound role is filled by the `elementObjecttype`. That is
the orientation and the discipline of `navigationBindings` (ch. 4.1a) and of
`sourceRole` itself, so WF-047's resolution is reused step for step rather
than restated. Two fields, one meaning, so exactly one spelling per meaning:
a one-step range IS `sourceRole` — the spelling every existing document
carries and every executor already reads — and `sourcePath` therefore
carries **two or more** steps and never stands beside `sourceRole`.

*The reading is the FLAT UNION, and it is normative.* The folded set is the
set of `elementObjecttype` instances REACHABLE from the fold subject along
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
  than "no carrier": a count over a multi-step range is **not expressible as an
  `AANTAL` FOLD**. A `sourcePath` is admissible only on the SET source
  (clause (c) above — the non-set sources hand the fold something else
  entirely), and a set source is no longer admissible under `AANTAL` (WF-096,
  the maintainer decision that took the form off the wire after `ref` and the
  Drools backend were measured reading it two different ways). The two fences
  meet at this NODE: with `set: true` the fold is WF-096's, without it the
  range is this rule's, so neither spelling of a counting FOLD survives, and
  `wf096-aantal-setbron-bronpad` is the negatief that pins the first of them.
  The scope of that sentence is this node and not the IR: whether some OTHER
  node counts over a multi-step range is that node's own question under its own
  rule, and a claim about it would need its own witness before it could be
  written here. The quantifier fold (ch. 4.5) is such a node, and for its
  at-least and at-most arms it has that witness: they take a multi-step
  range under WF-108 (c) and count over it FLAT, an element reached along
  two intermediate instances satisfying the matrix once. In
  `jutterswet-ketenkwantor` both alternative readings are ruled out: a
  count of join tuples turns the verdicts over where the shared element
  makes the flat count 3 and the tuple count 4, and the count of counts
  above — INTERMEDIATE INSTANCES — turns them over where two boten reach
  four elements; a second subject in one world pins the range itself.
  Replayed by gates 5 and 6. The exactly arm is not witnessed over a
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
it would need a new arm keyed on `meervoudig`, which no consumer reads in
this position, to refuse a shape whose semantics is already fixed by the
clause above. So "alle vondsten van de boot van de Jutter" — one to-one step
and one to-many step — is a two-step `sourcePath`, not a refusal.

*The written ANCHOR may be a role name, under the order ch. 4.1a already
fixes.* The wire never carries the written anchor; it carries the path,
whose first step starts at the fold subject by WF-102. What a producer must
decide is whether a written anchor that names a ROLE rather than an object
type ("de minimale waarde van het gewicht van alle vondsten van **de
aanmelder**") denotes the subject or another instance. The answer is the
test that already exists and the ORDER that already governs it (ch. 4.1a,
WF-083 (b)): far-role resolution FIRST — a role reachable from the subject
whose filler is some other instance is a HOP, and belongs on the path as a
step — and the self-denoting reading only at zero far hits, where the name
matches (ci, declared name or declared plural) a role the SUBJECT TYPE
itself fills, and then the path simply starts at the subject. The v0
resolver refuses every role anchor outright, on the ground that a role
whose filler happens to be the subject's own type denotes a DIFFERENT
instance; that ground is real and it is exactly what far-role-first already
catches, so uchar refuses the same shape without refusing the shape beside
it. Measured over the four zones (2026-09-11): of 56 member folds, **six**
write an anchor that names no object type — two in the negatief zone
(`wf093-wratvouw-set-som`, `wf093-wratvouw-set-maximum`, both already
rejected on their kind) and four in the parity fixtures — and every one of
them is a one-step range whose anchor is self-denoting, so the decision
moves no document. The producer's own witness is a frontend test, since the
form's wire result is the one-step `sourceRole` it already emits.

Semantics and worked examples, including the three empty corners:
`RUNTIME-SEMANTICS.md` ch. 3.4b.

- **WF-102** A set fold source's multi-step range is a `sourcePath`, and it
  is a CONNECTED path from the fold subject to the element type (ch. 4.3).
  (a) `sourcePath` and `sourceRole` MUST NOT both be present — the one-step
  range is `sourceRole`, and one meaning has one spelling.
  (b) A present `sourcePath` MUST carry at least TWO steps; the empty list
  and the single step are ill-formed.
  (c) `sourcePath` is admissible ONLY at the position `sourceRole` is, and
  only on the **set** source: the single argument of an instance fold read
  with `set: true`. On a listed value fold, on a builtin, on a non-set
  source (the temporal total and the row-1 wart, which hand the fold the
  subject's own read and the subject's linked instances respectively), it is
  ill-formed.
  (d) Every step MUST resolve (WF-047) and the steps MUST JOIN: step 1's
  opposite role is filled by the fold subject, each further step's opposite
  role is filled by the object type its predecessor's bound role reaches,
  and the last step's bound role is filled by the `elementObjecttype`. A
  path that starts elsewhere, breaks in the middle, or lands on another type
  is ill-formed — there is no bindingless fallback for a path, because
  WF-048's unique derivability answers for a type PAIR and a path is not one
  (its intermediate types are its own claim, not a derivable consequence of
  its endpoints).
  Companion: WF-083 (c), whose anchor is unmoved by the path and whose
  intermediate-type witness is named there.

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
| CharacteristicCheck | `characteristic`, `navigation?` (to-one role), `negated`, `wholePeriod?`, `source` | **Delta (`CONSTRUCTS.md` ch. 2.2):** v0's `eigenaar` field is the check's GASTHEER — the object type the check lives on — and the v1 wire is expected to say so by name; the DELTA is the spelling, not the field (`proposals/V1-WIREMIGRATIE.md`, a proposal). While it rides the v0 wire under its v0 name it carries three obligations: **WF-034 requires a present `eigenaar` to name a declared object type** (ch. 4.6 (q)) — it is the node key of the dependency edge the check contributes, and a bean-model executor re-attributes an unresolvable one to the subject in silence; **WF-100 requires the named type to DECLARE the characteristic** (ch. 4.6 (q1), maintainer decision 2026-09-10), the field and not the subject being the key of every declaration a reader consults; and **WF-109 requires it to DESIGNATE exactly one of the objects bound at the check's position** (ch. 4.6 (q2), maintainer decision 2026-09-19): exactly one of them satisfies it (`T ⊑ O`) and no second one overlaps it (some object could be both). One object everywhere, two at the guard of a fact creation (the anker and the doel), so that every executor reads the object the field names instead of refusing it (`RUNTIME-SEMANTICS.md` ch. 5.8b). Inside a `gedurende` clip the field is not fenced but REFUSED — WF-097 (below) — because the clip reads the subject's own characteristic timeline and consults no owner at all **Landed (2026-08-18):** the navigated check carries its one hop symbolically in the `navigatieBinding` extension field (ch. 4.1a, WF-053); semantics `RUNTIME-SEMANTICS.md` ch. 5.8 — an absent link is false, negated or not |
| RoleMembership | `factType`, `role` (the role the subject plays), `negated`, `source` | **Delta (row 5):** one existential node replaces v0's near-side (`Rollidmaatschap`, with its filler-type lookup hint) and far-side (`VerreRollidmaatschap`, with the literal `backref` bean field and the top-level-only restriction — both dropped). On the v0 wire the near-side node resolves through `anderTeFiller` alone (the written `rol` is presentation), and WF-034 crosses it in both spellings: it MUST be present and it MUST name a type the subject is related to (ch. 4.6 (o)). The far-side node carries NO written role name at all, so its own `filler` and `backref` are the whole of its reference; since 2026-09-20 WF-034 crosses THEM — together, as one pair that must meet on one declared fact type (ch. 4.6 (t)) — on the hand-written witness `corpus/golden/jutterswet-postbezetting`, which is also the first document in any zone to carry the form. *Migration:* near-side → the named role; far-side → the subject-side role of the relation |
| Compound | `quantifier` ∈ all, none, the, at-least (`threshold`), at-most (`threshold`), exactly (`threshold`), `parts`, `source` | all/none/the are kernel; the counting forms are sugar (count-and-compare, row on `Kwantor`, `CONSTRUCTS.md` ch. 2.2) — mirrored as quantifiers. Quantifies over **parts of one subject** — propositional. Its first-order sibling is the row below |
| QuantifierFold | `quantifier` ∈ all, none, at-least (`threshold`), at-most (`threshold`), exactly (`threshold`), `elementType`, `filter?`, `test`, `sourceRole?`, `sourcePath?`, `source` | **extension node** (wire `Kwantorvouw`), `layer: core`: quantification of a condition over the members of a linked **instance set** — "…indien de `<attribuut>` van ALLE `<leden>` van de `<onderwerp>` gedurende het gehele jaar groter is dan `<grens>`". The range fields are the instance fold's (ch. 4.3) unchanged — `elementType` is the POPULATION extensionally (ch. 2.2), `sourceRole`/`sourcePath` the REACH, `filter` the die/dat subselection evaluated per contributor — and `test` is the quantified matrix, evaluated per contributor with the element as subject. The counting quantifiers are the row above's, over instances instead of parts, and they carry that row's `threshold` under the same rule: the verdict COUNTS the contributors whose matrix holds and compares that number with it. WF-108, WF-041; semantics `RUNTIME-SEMANTICS.md` ch. 3.9a |
| TimeWindow | `from?`, `to?`, `toInclusive`, `source` | from inclusive, to exclusive unless `toInclusive`; never both absent; each bound a date literal or the subject's own scalar date attribute (WF-111); never under `none` or a counting quantifier (WF-112) |
| RuleStatusCheck | `rule`, `inconsistent`, `source` | the row-30 split verdict: "is gevuurd" is a v1 non-goal, "is inconsistent" waits on the §9.5 channel — mirrored here because v0 dumps carry it; its v1 admissibility is **OPEN** with row 30 |
| Unresolved | `source` | ch. 4.4 |

The **whole-period qualifier** (`year-or-month`, `negated`) attaches to
Comparison, Predicate, and CharacteristicCheck only —
`layer: dialect(regelspraak)`, lowering onto timeline algebra (row 29;
`RUNTIME-SEMANTICS.md` ch. 3). One position is carved out of that
attachment, and one field beside it, by WF-097 below.

- **WF-097** A **`gedurende` clip member** — a CharacteristicCheck
  standing as the condition of a `Function`'s `filter` where that filter
  is a **clip**, whether directly or as a member of the flat ALLE
  composite — MUST carry **neither an owner (`eigenaar`) nor a
  whole-period qualifier**, the negated qualifier included. Two arms of
  one rule, both closing the same thing: a qualification the reading has
  no room for.

  **All THREE clips, exhaustively.** The same node sits in the same
  field at each, so the rule names them as a closed list:

  | clip | kind | its own shape rule | `RUNTIME-SEMANTICS.md` |
  |---|---|---|---|
  | the temporal total | `TOTAAL` over an own temporal attribute | WF-055 | ch. 3.6 |
  | the pro-rata fold | `TIJDSEVENREDIG_DEEL` | WF-056 | ch. 3.7 |
  | the day count | `AANTAL_DAGEN_IN` | WF-058 | ch. 3.8 |

  The **fourth** reading of `Function.filter` — the die/dat instance
  subselection — is **not** a clip and is deliberately **not** fenced.
  There the condition is a per-contributor truth test, and the two
  fields land where they land for the scalar check generally, not in
  some better place: the whole-period qualifier is already refused
  (WF-043 reaches it, the operand being non-time-dependent), and the
  owner was **silent** — measured verifier-clean with both executors
  ignoring the field. That silence was real and is NOT closed here; it is
  the scalar-`eigenaar` question, which is a semantic one (WHICH owner is
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
  `eigenaar` — there it is ordinary name resolution, WF-034 (q)'s
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
  three OUTSIDE a clip, all in the negatief zone) and 0 with a
  qualifier. The §8.2 qualifier on a **Comparison** counted 5 nodes
  before the rule and counts 5 after it: that family is untouched, and
  every `GeheelPeriode` node added since is on the now-forbidden
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
WF-096 at the moment that rule was promoted to ERROR.

**The node quantifies; the operand does not.** The alternative this
replaces is the one the v0 wire actually carried and the production
toolchain actually executed: the comparison itself, with a SET-valued
operand, quantified implicitly. It is rejected on a measured defect, not
on taste. `set: true` records only THAT a quantifier was written, never
WHICH — the frontend computes it as "the onderwerp's quantor is not the
bare definite", so `alle`, `geen van de`, `ten minste 2 van de` and
`precies 1 van de` reach the wire as one and the same node. A reader
that took the set flag for a universal would answer ALLE for a sentence
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
(`wf047_binding`, `wf048_derivable`, `resolve_binding`), on the WF-097
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
verdict COUNTS the contributors whose `test` holds — *at-least n* is
count ≥ n, *at-most n* is count ≤ n, *exactly n* is count = n — which is
the count-and-compare `CONSTRUCTS.md` ch. 2.2 records for the
propositional sibling, over instances instead of parts. Nothing else
moves: the contributors are the same `reach ∩ population` after the same
`filter`, and `test` is the same per-member condition. **The empty set
needs no separate principle here**: a count of 0 answers every quantifier
by itself, so the vacuous reading the chapter states for `all` and `none`
is not restated for these three — it is what the comparison gives.

**`threshold` is present exactly where it is read.** It sits on the wire
only under the three counting quantifiers, and is ABSENT under `all` and
`none` — where `Compound` carries it always and writes a zero nobody
reads. That divergence is deliberate and is the argument two paragraphs
up, applied to this node's own field: a field that is meaningful in one
position and inert everywhere else is the "a field nobody reads is a
field that can say anything" defect of WF-083 (c), and `Compound`'s shape
is a v0 mirror this node does not have to inherit. The absence is checked
(WF-108 (e)), not merely permitted.

- **WF-108** A `QuantifierFold` MUST state a quantification something can
  satisfy. Five arms.
  (a) `quantifier` ∈ {all, none, at-least, at-most, exactly}. `the` is a
  definite description of one member and not a quantification over a set;
  it is refused BY NAME rather than ignored, for the reason the node
  exists: a reader that fell through to the universal would answer the
  opposite of what was written.
  (b) `elementType` is present, and names a declared object type. The
  presence is this rule's (a quantifier whose population is unnamed
  ranges over nothing a reader can name); the resolution is WF-034's,
  reported under that number at this position.
  (c) The RANGE is one meaning with two spellings: `sourcePath` and
  `sourceRole` are mutually exclusive, a `sourcePath` carries at least
  two steps, its steps form a connected walk from the quantifier's
  subject, and its last step lands on a role whose filler is
  ⊑-compatible with `elementType`. This is WF-102's discipline under
  this node's own number, and the number is new on purpose: WF-102 is
  scoped to a fold's ARGUMENT — the source position, which this node
  does not have — and a `corpus/negatief/` directory must be able to
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

**What this does NOT move.** WF-096 stands unamended and unweakened: a
set reading is still admissible only as an instance fold's source. The
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
  node whose static type is plain null, are refused alike — **at every
  position the rule reaches, since the seventh wave's fix round
  (2026-09-06)**. "Alike" was an aspiration while the fold-argument branch,
  which replaces the expression walk for its operands, repeated only the
  spelled half of the prologue there; both halves now ride the same guard on
  that branch, so the two spellings are refused together at a value node, at
  a rule target and at a fold operand alike. The exemptions are shared too,
  and there are exactly two: a Lambda, which is never a value and carries no
  type by construction (its position is governed by WF-069), and the single
  argument of an aggregation, which is the ch. 4.3 row-1 fold source.
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
  a parity document is a measurement, never a pin. The second wave's descent
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
  `groepeerAttribuut` — with emptiness decided by the ch. 2.1 name identity
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
  **Fifth wave 2026-09-04.** The same naming class, carried off the
  expression walk entirely: three reference positions the walk never reaches,
  each measured verifier-clean and each with an executor that treats it
  differently from the other two. Over-rejection is settled by the same
  instrument: over all **493** documents of golden + rood + negatief + parity
  that PREDATE this card (154 + 22 + 287 + 30) not one verdict and not one
  report moves; the **six** new negatief witnesses here (and a seventh under
  WF-028) take the negatief zone to 294 and the four zones to 500. **Its fix
  round, 2026-09-05,** sharpened (m) with two further arms and its empty half,
  measured the same way: over all **500** documents that predate the fix round
  not one verdict and not one report moves either, and its three witnesses
  take the negatief zone to 297 and the four zones to 503.
  (m) The four **bean-field names** of a fact creation (ch. 3.4) MUST be
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
  byte**: this is one of the two places the ch. 2.1 case-insensitive *matching*
  is not enough (the other is (p) below, on a different ground — there the key
  IS a member's carried identity; here it is a name that must survive as a
  property), because the name becomes a Java property reached by getter and that
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
  `verbindendeRol` names the back-link. That is incidental cover, not
  coverage, and it is measured: pointed at an undeclared name one creation at
  a time, 10 of the 11 golden object creations were already refused (all ten
  by WF-053, nine of those by WF-047 as well) and exactly ONE stayed
  verifier-clean — the only creation with no `toekenningen` at all
  (`anchor/jutterswet-penninguitgifte`, creation 1). There neither executor
  faults: they AGREE on a wrong answer (`ref` and clingo both report 0 dossiers
  where the anchor pins 1), because the creation simply runs over an empty
  population. That is a row where the differential square is blind and only
  the hand-pinned anchor expectation stands between the corpus and the defect.
  *Still open, measured and named:* `subject: null` is equally verifier-clean
  and there both executors are loud (`missing ObjectCreatie.subject`) — the
  empty-reference class of the earlier waves, with no witness yet. (o) A
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
  same: over all **510** documents of golden + rood + negatief + parity that
  PREDATE this card (155 + 22 + 303 + 30) not one verdict moves, and exactly
  ONE report does — `wf031-dagsoort-leest-rolstaat`, which gains a truthful
  incidental line from (o)'s empty half below and keeps the WF-031 verdict its
  directory declares. The twelve new negatief witnesses take the negatief zone
  to 315 and the four zones to 522. *Fix round 2026-09-06:* (p)'s identity is
  tightened from case-insensitive to byte-exact on the ground stated there, its
  thirteenth witness takes the negatief zone to 316 and the four zones to 523,
  and the same instrument over all 523 documents moves exactly that one
  document and nothing else — no verdict and no report elsewhere.
  (p) The eight **SIDE-TABLE KEYS** of an object type — `attribuutMeervoud`,
  `attribuutDimensies`, `attribuutGranulariteit`, `attribuutTijdlijnAantal`,
  `attribuutTijdlijnNaam`, `kenmerkSoorten`, `kenmerkGranulariteit`,
  `kenmerkTijdlijnAantal` — MUST each name a **declared member** of that
  object type: the five attribute tables an attribute, the three
  characteristic tables a characteristic, and MUST spell it **verbatim** —
  the member's declared spelling, whitespace and case included (*byte rule
  added in the fix round of 2026-09-06; the sixth wave landed this clause
  under the case-insensitive identity and that was wrong, see below*). An
  object type declares its members in `attributen` and `kenmerken` and hangs
  eight per-member facet maps beside them; nothing required a key to name a
  member. Every key is a reference, and it is more than that: it is the **v0
  denormalisation** of a facet v1 carries on the member node itself (ch. 2.2,
  `CONSTRUCTS.md` row 11 — "each v0 map entry folds into the corresponding
  member node"), so the key **is** that member's identity, written out beside
  the declaration instead of on it. That is why the rule is ch. 2.1's
  **carriage** rule — "Declared names are carried verbatim
  (whitespace-collapsed, case preserved)", read as that section's sharpening
  note fixes it for a COMPARISON: byte for byte, whitespace included — and not
  its matching rule: a
  side-table key is not a site where one name is matched against another, it
  is an identity being carried, and this clause asks of the key exactly what
  the v1 member node would answer by construction. The harm is a **silent
  semantic change** rather than a missing value, which is what separates this
  clause from the empty-reference class of (a)–(f): a key spelled `verjaart`
  where the declared characteristic is `verjaard` says "this characteristic is
  time-dependent" about a characteristic that does not exist, and leaves the
  declared one silently NON-temporal. No consumer complains; a
  case-insensitive reader looks the facet up by the declared member's name,
  finds nothing and takes the default, and the case-sensitive half of the
  readership reaches the same silence by the other road — it does not find the
  key at all. That is a different program, not a fault.
  **EIGHT arms, one per table**, because the eight are eight independent
  readers, and each has its own witness (`wf034-wees-<table>`), a mutant
  removing one arm freeing exactly one document. The readers differ sharply
  and the clause is honest about it, *measured 2026-09-05 across the four
  consumers*: `attribuutGranulariteit` and `kenmerkGranulariteit` are read by
  the verifier (`attr_temporal`, WF-005, WF-043 on both the expression and the
  condition side, the temporal prologue of the expression layer, WF-065,
  WF-086) **and by all three executors**; `attribuutTijdlijnAantal` and
  `kenmerkTijdlijnAantal` by WF-005 and by both Drools transpiler branches;
  `attribuutDimensies` by WF-004 and WF-040's cell cross; `attribuutTijdlijnNaam`
  by WF-005, which uses the KEY as a cross-table join into
  `attribuutGranulariteit` and `attribuutTijdlijnAantal` to compare the adopted
  facets against the declaration — WF-003 reads that table's value only, and
  the RegelSpraak frontend reads the key case-sensitively at two sites off its
  own in-memory model, which is the emitter side and not a consumer of the
  wire; and **two tables whose KEY no wire consumer reads at all today**:
  `kenmerkSoorten` — the largest table by far, 788 of the 924 keys the four
  zones held before this card — which the Drools loader parses and nothing reads
  back, and `attribuutMeervoud`, whose only walker WF-057 checks the **value**
  (the plural's target-text-safety) and discards the key, the key's sole reader
  being the emitter's plural matcher. Those two arms are for the wire, on the
  same argument the wave-3 dimension-cell arm rests on (g): the tables are
  keyed by member name in the serialization contract and a conforming consumer
  may read them tomorrow. *Measured, a spurious key added to each table on each of the
  205 golden object types in turn:* **205 of 205** stayed verifier-clean
  before this card and none does now, for each of the seven member-agnostic
  tables; `attribuutDimensies` needs a declared dimension for its value to
  resolve, which 2 golden object types offer — 2 of 2 before, 0 after. *The
  complementary instrument, renaming a LIVE golden key so the declared member
  silently loses its facet:* `attribuutMeervoud` 6 of 6 clean before and 0
  after, `attribuutGranulariteit` 20 of 33, `attribuutTijdlijnAantal` 1 of 1,
  `kenmerkSoorten` 124 of 124 — and, recorded rather than smoothed over, three
  tables where that instrument does not apply because renaming a live key is
  refused already: `attribuutDimensies` 0 of 2 and `kenmerkGranulariteit` 0 of
  9 (the member's own reads lose their ground and WF-040/WF-043 report them),
  `attribuutTijdlijnNaam` 0 of 2 (WF-005's adoption check), while
  `kenmerkTijdlijnAantal` has **no live golden key at all**. *Executor
  observability, renaming each live key and replaying every case of its
  golden:* `attribuutMeervoud` 8 measurements, `attribuutTijdlijnNaam` 1 and
  `kenmerkSoorten` 438, all of them **byte-identical before and after in both
  OCaml executors** — nothing sees it. `attribuutGranulariteit` 99
  measurements: 91 loud in both, 6 silent in both, and **2 DIVERGENT** — on
  `corpus/golden/synth2-prorata-breed` case4 a renamed `seizoenbasis` or
  `toeslagdeel` leaves `ref` replaying to its pinned expectations while the
  ASP backend refuses (`pro-rata base "seizoenbasis" is not a temporal
  attribute`). `kenmerkGranulariteit` 27: 26 loud in both and one divergent
  **on an anchor golden** — `anchor/jutterswet-dubbelwacht` case3, where a
  renamed `aangesteld` makes `ref` fault on `gedurende` while the ASP backend
  replays the case clean — *with the caveat that the carrying document is
  ill-formed independently of this clause and already was before this card:
  WF-055 refuses a `gedurende` over a non-temporal characteristic, which is why
  that rename is one of the 0-of-9 above. The divergence is real but sits
  behind an already-closed door, so it is a second-line finding and not this
  clause's justification; the underlying asymmetry — clingo's ch. 3.6 clip arm
  missing the temporality gate its pro-rata sisters carry — is its own card
  (`backlog/done/20260906-clingo-gedurende-spiegelpoort.md`).* Cost over the
  four zones: none. All **924** keys the four zones held before this card
  (24 + 9 + 74 + 2 + 2 + 788 + 24 + 1) name a declared member; the only orphans
  anywhere are the eight witnesses.
  **The identity is byte-exact, and that was measured, not chosen** (*fix
  round 2026-09-06*). The consumers do not agree on how they look these facets
  up: the verifier, `ref` and the ASP backend are case-insensitive, while the
  bean-model executor is not — the Drools backend reaches these maps through
  raw case-sensitive map lookups at **108** sites across four of the eight
  tables (`attribuutGranulariteit` 64, `kenmerkGranulariteit` 20,
  `attribuutTijdlijnAantal` 17, `kenmerkTijdlijnAantal` 7), against five
  case-insensitive lookups through its own name helper. Under a
  case-insensitive rule a key differing from its declaration in capitalisation
  alone is no orphan, and the document stays clean everywhere while the third
  executor drops the golden out of the differential in silence. *Measured
  2026-09-06 on `corpus/golden/synth2-clip-over-prorata`, the
  `kenmerkGranulariteit` key `borgplichtig` capitalised — a one-character
  diff:* the verifier PASSES, `ref` PASSES (3 expectations), the ASP backend
  PASSES (3 expectations), and the Drools backend goes from PLAYED to a
  per-form auto-exclusion (a transpiler skip: the assignment is no longer
  temporal), which takes gate 12 from played 109 to played 108 and leaves the
  gate **green** — its only floor is a non-empty played set. The wire must
  catch what no gate catches; the witness is
  `corpus/negatief/wf034-zijtabel-kapitalisatiewees`. *Cost of the byte rule,
  measured over the four zones:* of the **943** keys they held after the sixth
  wave and before this round, **zero** differ from the declared member in byte
  while resolving case-insensitively, and before the wave likewise zero of
  924 — the tightening rejects nothing that was there.
  (q) A characteristic check's explicit **`eigenaar`** MUST name a declared
  object type. It names the type the characteristic is read on when that is
  not the enclosing form's subject, and it IS read — it is the node key of the
  dependency edge the check contributes (ch. 5.1; the graph builder keys its
  characteristic node on `eigenaar` and falls back to the subject only when
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
  **2** carry an explicit `eigenaar`, both in the negatief zone and both
  naming a declared type, and **no golden carries the form**. *What this arm
  does NOT close:* whether the named type DECLARES the characteristic. That is
  a semantics question — which owner is authoritative — and it was answered on
  2026-09-10 in the field's favour; it is (q1) below. *Identity here is
  and stays case-insensitive*, unlike (p) above: this is ordinary name
  RESOLUTION — a name pointing at a declaration, not a declaration's identity
  written out — and every reader resolves it that way (the verifier's
  case-insensitive lookup, the ASP translator, and the Drools loader's
  exact-then-case-insensitive object-type lookup). The verbatim rule of (p) is
  scoped to the eight denormalised keys and to nothing else.
  (q1) A NON-navigated characteristic check's explicit **`eigenaar`** MUST name
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
  does the type declare it (WF-100 here), is it time-dependent
  (`kenmerkGranulariteit`), and the node key `char:<owner>:<name>` of the
  dependency edge the check contributes (ch. 5.1) — is the one the NAMED type
  carries. The dependency graph was already keyed this way before the rule, in
  the verifier, the reference interpreter and both Drools graphs; what the
  decision adds is that the two crosses now agree with it. The OBJECT the check
  is evaluated on is a separate question, and (q) is where the wire's answer
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
  becomes ordinary rather than exceptional. `corpus/negatief/`'s day-kind
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
  checks over the three verifier-gated zones (golden 166, negatief 359, rood 26
  — 551 documents), **6** with an explicit owner, all six in the negatief zone
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
  before/after sweep over the verifier-gated zones (golden 167, negatief 360,
  rood 26 — 553 documents) moves exactly the new witness's verdict and not one
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
  (q2) A characteristic check's explicit **`eigenaar`** is its **GASTHEER**: it
  names the object type the check LIVES ON, and it MUST DESIGNATE exactly one
  of the objects bound at the check's own position — exactly one of them
  SATISFIES it (`T ⊑ O`) and no second one OVERLAPS it (the declared type graph
  holds a type below both) (**WF-109**, maintainer decision 2026-09-19). This
  is the object half (q1) left open, and it is settled in the direction (q1)'s
  wire reading already pointed — the
  field says which pattern hosts the constraint — with the consequence (q1)
  did not draw: every executor honours it, so a verifier-clean owner is
  carried by all four and the differential square closes over the form.

  *The wire does not move.* The field is still spelled `eigenaar` and still
  carries one object-type string. v0 is the faithful dump and this is a
  semantics decision, not a shape one; a v1 rename to a hosting name is a
  migration PROPOSAL (`proposals/V1-WIREMIGRATIE.md`) and nothing here depends
  on it.

  *Which objects a position binds.* ONE, everywhere: the enclosing form's
  subject. TWO at exactly one position — the `voorwaarde` of a §13.4.7 fact
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
  object is admitted only where the check IS the whole `voorwaarde`, does not
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
  A `Vergelijking` has no `eigenaar` field at all, and the frontend produces no
  doel-hosted comparison; measured 2026-09-19, a fact creation whose guard
  compares a DOEL attribute has no verifier-clean instance when anker and doel
  differ (the derived reading falls to WF-045's cycle or WF-053's ambiguity).
  So this decision does not generalise to the comparison without a new wire
  field, and it does not claim to.

  *An open tension, named rather than resolved.* The same intuition gets two
  opposite answers from this wire. On a characteristic check a foreign type in
  `eigenaar` names the HOST — the object the check is about. On an
  `AttribuutRef` a foreign type in `objecttype` is read by every reader as
  NAVIGATION over the derived relation, silently. One field says "read it
  there", the other says "walk to it". Measured 2026-09-19; not decided here,
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
  213, negatief 399, rood 34), **260** characteristic checks — **10** carry an
  explicit owner. Eight name the subject's own type and are untouched. One
  names an UNDECLARED type and belongs to WF-034 (q), which this rule steps
  aside for: an owner that resolves to no declaration has no satisfaction
  question to ask. Exactly ONE names a declared type the subject is not, and it
  is the negatief witness WF-100 already rejects — so the rule adds a second
  citation to one already-rejected document and moves no other verdict
  anywhere. The parity zone adds 9 checks over 30 documents and 0 owners. The
  rule's own witnesses had to be written for it, which makes this, like WF-100,
  a decision on an empty corpus set — and, like WF-100, not one without
  measured damage: before it the producer's own emission shape was
  verifier-clean and unplayable in two of four executors, and the form the
  frontend writes could not become a golden at all.
  (r) A unit's **`stelsel`** MUST name a declared `Eenheidstelsel` (ch. 2.5).
  It is the unit occurrence's one link back to the conversion table that gives
  it meaning, and nothing crossed it: WF-009 checks a declared table's
  INTERNAL consistency and WF-092 the record's own conversion pair, and
  neither asks whether an occurrence points into a declared table at all. Two
  readers do: the ASP backend's literal-restating pass gates on it (a literal
  is restated into the finest base unit of the target's system only when the
  literal's own `stelsel` matches that system by name AND the system resolves),
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
  under the enclosing record*), **2244** carry a `stelsel` and every one of
  them resolves, while the remaining 133 carry `stelsel: null` (the percentage
  unit and the bare calendar units, which belong to no declared system) and
  this arm passes over them by construction. Scope: the back-reference only.
  Whether the unit's `afkorting` is a unit that system declares is WF-009's
  question and stays open. `Eenheidstelsel.naam` no longer does: it left the
  open list on 2026-09-06 as one instance of the self-name class, which WF-094
  (ch. 2.1) closes byte-exactly over all five map-shaped declaration tables —
  and note that the two rules run in opposite directions on the same table for
  exactly the reason (p) and (q) part company. A unit's `stelsel` POINTS at a
  system and resolves case-insensitively; the system's own `naam` IS that
  system's identity written beside its key and is carried verbatim.
  *Identity here is and stays case-insensitive*, as in (q) and unlike
  (p): this too is name resolution rather than a carried identity, and
  `corpus/negatief/wf091-attribuutref-eenheid-kast` pins the boundary
  deliberately — that document carries `stelsel: "GEWICHT"` against a declared
  `Gewicht` as its witness that name RESOLUTION is case-insensitive while copy
  FIDELITY is a different question. A byte rule on `stelsel` would make it say
  something else.
  And the two **empty halves** the fifth wave measured and left without a
  witness, each now its own arm because each is its own question — a name
  that resolves against nothing has a declaration table to be missing from,
  an ABSENT name has nothing to look up — and because the two spellings are
  observed differently. An **object creation** (n) MUST carry a `subject` at
  all: pointed at an undeclared name both OCaml executors agree on a wrong
  ANSWER, while emptied both are LOUD (`missing ObjectCreatie.subject`);
  11 of 11 golden creations were verifier-clean when emptied and none is now,
  and of the 30 creations the four zones held every one names a subject. A
  near-side **role check** (o) MUST carry an `anderTeFiller` at all: with the
  field absent there is no type to derive a connecting role TO and the written
  `rol` is presentation, so the node carries no reference whatever; both
  executors are loud (`missing Rollidmaatschap.anderTeFiller`), 4 of 4 golden
  positions were clean when emptied and none is now. That second arm is the
  one place this wave touches a pre-existing document, and it costs nothing:
  of the 10 role checks the four zones held, the one carrying
  `anderTeFiller: null` is a day-kind condition WF-031 already refuses
  outright — a day-kind voorwaarde may not read role state at all, which is
  exactly why no subject was there to derive a filler from — so it keeps its
  verdict and gains one truthful incidental line.
  **Seventh wave 2026-09-06** (maintainer decision), and it closes the
  unknown-type fence rather than the naming class: (r) **the unknown type is
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
  that already fence it. THREE hand-built spellings are indeed fenced, and
  still are: WF-048, WF-083 (c) and WF-047 respectively. Three more are NOT:
  the degraded own read, the wart verbatim, and the `set: true` reading, all
  three verifier-clean before this round — and the second CRASHES the third
  executor (`IllegalStateException: geen getter voor …`) while `ref` and the
  ASP backend refuse. Closing them moves zero documents: the four zones
  carried **no** non-aggregation `Functie` with an `elementObjecttype` at
  all, before the two witnesses that pin the exit.
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
  crashes on the unit-bearing variant). Cost, counted over golden + rood +
  negatief + parity: of the
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
  negatief report changes a sentence, in the wave or in its fix round.
  *One position the earlier waves left unwitnessed, closed in the same
  round:* the prologue arm itself — the sentinel at a plain read — had no
  negatief case anywhere in the zone, so a live arm could have regressed
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
  `dagsoortPerEenheid` MUST name a day kind `dagsoorten` declares**, under the
  ch. 2.1 name identity — trimmed and case-insensitive, the same `norm` the
  verifier already applies to the map's KEY.
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
  `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort` does not
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
  that predates this card** — 554 documents of golden + rood + negatief +
  parity (164 + 23 + 337 + 30) — the map held **64** entries over **31**
  documents (4 in the one golden `jutterswet-schouwrooster`, 60 in the 30
  parity fragments, none in rood and none in negatief), **every one of them
  resolving**, none null and none empty. The before/after sweep over those 554
  plus the card's own witness — **555** documents — moves the witness's verdict
  and NOTHING else: not one other verdict and not one report line. Arm-exact:
  removing the arm's one call from the verifier's check list turns exactly
  `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort` clean over the
  same 555 and moves nothing else.
  **On the state this card LANDS on** (2026-09-09, 555 documents: 164 + 23 +
  338 + 30) the same map holds **70** entries over **33** documents — 6 golden
  in 2 documents, 4 in the one negatief, 60 parity — against **45** declared
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
  **2026-09-20** (maintainer decision), the naming class once more and the one
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
  (ch. 2.2, extensionally; `RUNTIME-SEMANTICS.md` ch. 5.8a rule 2). A
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
  `scripts/check-rood.sh`'s list, residual arms reporting their own position
  (`ref`'s left that list on 2026-09-20, when its last catch, the period
  condition, became the ch. 3.10 gate; the ASP backend's is still a genuine
  wildcard and still listed) — and the same card gave each a named arm, which is why the golden now
  closes the differential square (ref ≡ clingo ≡ Drools) on the far form.
  *Cost: none.* The sweep runs over golden + rood + negatief + parity —
  **690** documents on 2026-09-20 (217 + 34 + 409 + 30) — and the whole
  corpus carries **6** far-side checks over **2** documents, both this
  card's: the golden's four and the negatief's two. Arm-exact: neutering
  this arm in `walk_cond` turns
  `corpus/negatief/wf034-verre-rolcheck-verkeerde-zijde` clean and moves no
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
- **WF-036** Comparison and predicate typing (mirror, with one ratified
  v1 tightening): ordering operands
  MUST be number-like with compatible units; **date-ordering operands
  MUST be dates of one representation** — both day-precision or both
  millisecond-precision; equality
  operands MUST share the family — same enumeration domain, same date
  precision, compatible units; an elfproef operand MUST be text or a
  whole number without unit; exact-digits MUST have a text operand and a
  whole digit count (v0 does not check positivity — mirrored; a ≥ 1
  tightening is a candidate); a day-kind predicate operand MUST be a
  date. A unitless literal adopts its sibling's unit before the check
  (mirrored convenience).
  **The date-ordering precision clause** (maintainer decision 2026-09-05)
  replaces the v0 letter this rule mirrored — date-ordering operands need
  only *be* dates, either precision, an asymmetry v0 left standing — with
  the per-representation family reading WF-038's ordering fold has
  carried since 2026-09-05, and ratifies it. `eerder`, `eerder of
  gelijk`, `later` and `later of gelijk` ask which of two dates comes
  first, so they need an order over both. `Date` has one **per
  precision**, and its two precisions are two of them: a day date names a
  whole day, a millisecond date a point inside one. The sibling EQUALITY
  arm of this same rule already refuses that seam ("equality across date
  precisions"); this clause is its ordering half. Neither operand is
  ill-formed alone — their combination is, and it is reported at the
  comparison node for that reason. An operand carrying **no type at all**
  defers: the whole arm is skipped and WF-034 reports it (`value node
  without a static type`). The unknown SENTINEL does not defer here —
  this same arm refuses it as a non-date operand (`date-ordering operand
  is unknown`), and WF-034 refuses it beside, so the precision clause
  never meets one. That asymmetry with WF-038's ordering fold, whose
  defer IS load-bearing, is deliberate: only the ch. 4.3 row-1 wart
  reaches that arm, while every operand here is walked as an ordinary
  expression. **Delta (v1 tightening, 2026-09-05).** *Why a rule and not
  a note:* measured on a verifier-clean document before the clause
  (`corpus/negatief/wf036-datumordening-precisie`, 2026-09-05), no
  executor answers it — and the three refuse at different depths. `ref`
  is the only leg that meets the defect itself: it plays a same-precision
  `DatumTijd` ordering and faults at evaluation on the mixed pair alone
  (`unordered comparison: 2025-03-05 vs @…ms`). The Drools backend draws
  exactly this line independently, which is the strongest evidence for
  the clause: it PLAYS a same-precision `DatumTijd` ordering — in a
  consistency and in a rule condition alike, discriminating a
  one-millisecond difference a day reading would call equal — and skips
  the mixed pair on an explicit guard whose own comment names it a §5.3
  type mismatch never to be coerced away. A third executor, built for
  another engine, fences where this clause fences. The ASP backend
  refuses more coarsely: `unscalable datatype in the ASP translation` is
  a FORM refusal and not a verdict on this defect — the same message
  comes on the CLEAN same-precision ordering, because the ASP scale table
  hands `DatumTijd` no scale, while a `DatumTijd` ordering FOLD
  (`jutterswet-hoogwaterpaar`) translates and plays. *(That last sentence
  described the ASP backend on 2026-09-05 and is now history: since
  2026-09-06 the clean same-precision ordering translates through the pair
  term's `dtcmpN` helper — the scale table still hands `DatumTijd` no
  scale, because it has a pair and not a scaled integer. What is refused
  at that arm now is the MIXED pair, by its own reason.)* *Honesty note:* this
  is a place where the wire fence runs AHEAD of the RegelSpraak frontend,
  which lets the four ordering operators cross the precision seam on
  purpose while demanding one precision for equality, assignment,
  tijdsduur and eerste/laatste. The corpus pays nothing for the
  tightening — over the four verified zones (golden, rood, negatief and
  parity, 504 documents on 2026-09-05) the only report that moved was the
  new negatief's — but a frontend that emits the shape is now refused at
  the wire, and closing that gap is frontend work this clause does not
  do. *A second honesty note, on what the clause is TESTED on:* all seven
  date orderings in golden and rood ordered two DAY-precision operands, so
  the admitted millisecond half rested on the executors and not on the
  corpus — refusing the same-precision `DatumTijd` pair too moved zero of
  those 504 reports. **That gap closed on 2026-09-06**: golden
  `jutterswet-meldstip` orders a `DatumTijd` attribute against a
  `DatumTijd` literal in both directions at a sub-day threshold, and all
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
  bounds annotated `kg` (`corpus/negatief/wf099-begrensd-grens-eenheid`,
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
  whose `canoniek` contradicts the system it names (`t` declared canonical
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
  golden (166), negatief (359) and rood (26) — 551 documents — moves
  exactly the witness's verdict and not one other report line. The parity
  zone's 30 documents, which no gate puts through the verifier, carry two,
  both already non-conforming for unrelated reasons. Admitting that class is one
  decision for WF-035, WF-036, WF-023 and this rule together, not a
  divergence to introduce at one of the four.
- **WF-037** Aggregation shape: facet validity per kind MUST hold —
  `emptyFallback` only on sum; `dimensionSelection` only on sum/min/max;
  `granularity` only on tijdsevenredig-deel and aantal-dagen-in (builtin
  side); an instance fold MUST carry `elementType`, a value fold MUST
  carry ≥ 2 arguments; builtin arities MUST follow the ch. 4.3 catalogue
  (one operand for the scalar and extractor kinds, two dates for
  tijdsduur, the component list for date construction). **The filled
  count** (`AANTAL_GEVULDE`, ch. 4.3; landed 2026-09-20) takes exactly ONE
  argument, MUST carry an `elementType`, and that one argument MUST be a
  SET reading — three arms, of which the last two are the only clauses of
  this rule that demand a form instead of forbidding one, and they are
  written that way because the alternatives are occupied rather than
  absent: a non-set source is the subject's own read or the row-1 wart, and
  neither has a filled count. The sentinel spelling is WF-093's and this
  rule steps aside on it, so one malformation carries one rule. The kind is
  outside the "≥ 2 arguments" clause above, whose sentence reads its arity
  law backwards. **The count** (`AANTAL`) stands outside that clause too, and
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
  2026-09-19 narrowing took off this kind in its set spelling, while the ASP
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
  defence instead of answering. *v0 mirror note:*
  the single node makes every combination representable, and v0 even
  populates the element type on multi-operand min/max value folds whose
  first operand is an attribute reference — an artifact the v1 split
  removes; v1 makes the invalid combinations ill-formed. **Delta
  (promotion).** The filled count's three arms are a **delta (new kind)**
  instead: the source language has no such operation, so they mirror
  nothing.
- **WF-038** Aggregation operand typing (mirror, with one ratified v1
  widening): numeric folds' operands MUST be numeric with compatible
  units; **every operand of an ORDERING fold — `first`, `last`,
  `minimum`, `maximum` — MUST come from one comparable family: numbers,
  or dates of one precision**; a totaal operand that is
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
  **Scope, stated because the silence would be read the other way
  (2026-09-20):** the filled count (`AANTAL_GEVULDE`, ch. 4.3) is NOT a
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
  not believed (negatief `wf091-aantalgevulde-eenheid`).
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
  `elementObjecttype` besides — the tag v0 stamps vestigially
  (`RUNTIME-SEMANTICS.md` ch. 1.2) — and reading the operands is exactly
  what makes that harmless. An operand carrying the unknown sentinel, or no
  type at all, **defers** — the sentinel is the ch. 4.3 row-1 wart
  WF-034 tolerates in a fold source. Where the defer is *unreachable* it
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
  holds open, then, is the wart itself — an instance ordering fold over a
  bare role source, which
  is verifier-clean today and which **no** executor carries (measured:
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
  rood, negatief and parity, 492 documents **at that date** — the only
  report that moved was the new negatief's.) WF-036 is no cover for the
  open half — a fold is not a comparison, and no arm of WF-036 reaches a
  fold operand — but it is, **since 2026-09-05**, a full precedent for
  the closed one: it crosses units for a numeric ordering, both units and
  precision for an equality, and now the date REPRESENTATION for a date
  ordering (`date ordering across two date precisions (day and
  millisecond), which share no order`, witness
  `corpus/negatief/wf036-datumordening-precisie`). The follow-up this
  paragraph tracked — that arm crossing neither, so that a day-versus-
  millisecond comparison was verifier-clean while `ref` faulted
  (`unordered comparison: 2025-03-05 vs @…ms`) and the ASP backend
  refused it as an unscalable datatype — is **closed**, and nothing on
  that arm is left open: dates carry no unit, so the unit half that stays
  open here has no counterpart there. A second
  honesty note on what the closed halves are *tested* on: the number
  family admits `Number` and `Percentage`, and no corpus document
  exercises the percentage half — dropping `Percentage` out of the family
  moves zero of those 492 reports. The index row below says so.
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
  the v0 error). The sentence is about the VOCABULARY, not about one
  node, so since 2026-09-16 it is checked at both of its hosts: the
  propositional `Compound` and the first-order `QuantifierFold`
  (ch. 4.5), whose WF-108 (e) hands the value here rather than judging it
  itself. What the bound refuses is the same in both places — a
  *threshold* of 0 asks nothing under *at-least*, and under *at-most* and
  *exactly* it is `none` spelled a second way, so admitting it would put
  two quantifiers on the wire for one reading.
- **WF-042** A TimeWindow MUST have at least one bound, and its bounds
  MUST be date expressions (mirror; v0 checks date-ness only — either
  precision is admitted, unchecked against the operand). WF-111 narrows
  the bound to two shapes, both of day precision — so "either precision"
  holds for the date typing this rule checks and no longer for a
  well-formed window.
- **WF-111** Each bound of a TimeWindow MUST be one of two shapes: a date
  `Literal`, or an `AttributeRef` to a **scalar** (non-time-dependent)
  date attribute of the enclosing form's own subject — un-navigated (no
  navigation role, path or binding), not a set read, on an object type
  the subject satisfies (`S ⊑ O`, ch. 2.2). Either shape MUST be of day
  precision (`DatumInDagen`): the window is a range of civil days
  (`RUNTIME-SEMANTICS.md` ch. 3.10) and no clause reads an instant bound.
  *(Maintainer decision 2026-09-23; the day-precision clause adopted by the
  maintainer at this rule's merge.)* That clause carries its own
  motivation, a latent split measured before the rule: a LEEG datum-tijd
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
  quantifier (`at-least`, `at-most`, `exactly`): not as a member of a
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
  type — the `eigenaar` where present, the enclosing form's subject
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
  removing the creation members of recursive rule groups: a dependency
  cycle is admissible only when it passes through recursive creation
  (ch. 5.3; mirror: v0 warns at build time and caps firing).
- **WF-046** No dependency cycle — including a WF-045-admissible one —
  may carry a negative edge (a negated or negation-quantified
  characteristic read, every read under a leeg/gevuld test in either
  polarity, or a regelversie channel test): evaluation strata MUST exist
  (ch. 5.2, 5.3; operational reading `RUNTIME-SEMANTICS.md` ch. 6.3). v0's
  single agenda boundary is the two-stratum special case.

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
  subtype. A fact creation mints no instance at all — its ch. 6.1
  population write is the conservative proxy beside the relation node — so
  that one stays literal.
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
(`corpus/negatief/wf045-subtypecreatie-cyclus`), and two rules whose mutual
dependency runs through one inherited attribute pair spelled at two
different owners (`corpus/negatief/wf045-erfattribuut-spelling-cyclus`).

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
  leeg/gevuld test in either polarity (*2026-09-23*, below), or a channel
  test (regelversie). **Once negative, conservatively negative** — a double
  negation does not cancel. A negative edge is stratum-strict *and*
  forbidden on any cycle (WF-046).

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

The witnesses are one per polarity plus the presence-true reading:
`corpus/negatief/wf046-leegtest-recursieve-creatie` (R1, `negated: false`
throughout), `wf046-leegtest-negatie-recursieve-creatie` (R3, spelled with
`negated: true` throughout) and `wf046-leegtest-aanwezigheid-recursieve-creatie`,
where the only test on the cycle is a negated leeg test. That last document
has one fixpoint, in which nothing is minted, and it is refused because the
decision counts both polarities; it is the witness that separates the
decision from the narrower reading "only a test that holds on absence is
negative". Off a cycle the rule orders a reader strictly above the writers
of its operand, which also closes an order dependence in the reference
interpreter: a leeg test in an ATTRIBUTE rule on a derived attribute shared
its writer's stratum, and the reader's position in the document decided
whether it acted on the transient leeg (`RUNTIME-SEMANTICS.md` ch. 6.4).

*Cost:* none in the corpus. Counted 2026-09-23, none of the 28 leeg/gevuld
tests in 26 corpus documents reads a node that some form writes, so the rule
turns no existing edge negative. No verdict moved in the golden (235), red
(43) or negatief (440) zones, and the reference interpreter's replay of all
638 golden and red cases is byte-identical.

*Not covered, recorded open:* other conditions that hold on absence without a
`negated` field keep their positive or stratum-strict reading: an inequality
with one leeg operand, a numeric equality of two leeg operands, a count or a
filled-value count tested against zero, a quantifier fold under *all* or
*none* over a comparison, a propositional *none* over a comparison, and an
ordering over arithmetic that coalesces leeg to 0. On an admissible creation
cycle each of them was measured verifier-clean with the executors split
(`RUNTIME-SEMANTICS.md` ch. 6.1, recorded residuals). This decision does not
cover them.

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
INSIDE a day-kind voorwaarde is the one place it is evaluated, which is
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
never played mis-stratified. A backend realising a **coarser**
stratification than the graph mandates without refusing the uncovered forms
is non-conforming.

*Recorded flank (measured 2026-09-23, open):* that guard does not reach the
leeg/gevuld test. On a document where a leeg test reads an attribute that
another rule derives and the reader precedes the writer, both Drools
branches act on the transient leeg: an attribute reader wrote 1 and a
characteristic reader attributed its characteristic, where `ref/` and
`backends/clingo` answer leeg and absent. With the writer first, both
branches agree with them. For the characteristic reader the edge was already
stratum-strict before this date; for the attribute reader it became negative
with the leeg/gevuld decision (ch. 5.2). No corpus document has the shape,
so gate 12 cannot see it. The fix is a salience below the operand's writers
or a named refusal, and it belongs to the backend.

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
| `name` | string | v0 wire `naam`. A **named form**: the wire type is nullable and WF-107 is what demands the name, and WF-020 holds it unique in the same namespace as the ch. 3 rule forms — a defaults group and a rule may not share one |
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
argued. Today those three fences key on the DECLARED object-type name, so
`Strandrecht.uitkering` and `Wrakrecht.uitkering` are two keys and the
fences do **not** fire on a supertype/subtype pair at all. The tempting
repair — lift the target key to the chain-closed set, now that member
resolution walks the chain — would make them fire on precisely the shape
inheritance legitimises: a supertype base plus a subtype base on one
target would become "two writers" and the construct would be unreachable.
In the effective stack that pair is ONE writer, because the subtype's own
base replaces the inherited one. Any chain-awareness added to these rules
must therefore ask *"do these two writers survive into one instance's
effective stack"*, never *"do these two targets close over the same
member"*. This is a new obligation, not a restatement, and it is witnessed
by `anchor/jutterswet-strandrechtketen`, whose two bases on one chain are
verifier-clean and replay through all three executors.

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
| WF-006 | enumeration domain: ≥ 1 value, values unique | gate 3 | `jutterswet-keurstaat` (the keurklasse domain) | `wf006-duplicate-enum-value` |
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
| WF-017 | day-kind spellings shadow no built-in unit and no other day-kind | gate 3, three clauses over the two wire positions a spelling can occupy (ch. 2.9): the DECLARED name against the built-ins, plus **arm (a)** a registry KEY against the built-ins and **arm (b)** a declared name registered as a DIFFERENT day kind's unit spelling — the wire-visible form of the silent first-wins collision, whose losing registration the wire cannot show (one value per key). "The built-ins" are the THIRTY spellings of the normative time-unit table (`RUNTIME-SEMANTICS.md` ch. 3.8), compared through that table itself since 2026-09-20, abbreviations and the second plurals `secondes`/`millisecondes` included; the producer moved with it (a declared name in the table is an ERROR in the RegelSpraak frontend) and so did the executors' lookup order. A key that is not a declared name stays free: no plural field exists on the v0 wire (the (s) argument, ch. 4.6) | `jutterswet-vormenkabinet` (two day kinds, each bound to itself — one of them only through the ch. 2.1 identity — and two plural keys, `schouwdagen` and `stormvloeddagen`, each binding to its own day kind and colliding with no declaration); `jutterswet-vervaltermijn` from the other side (`wk`, `mnd` and `kw` as ordinary calendar units, which the widening must leave clean) | `wf017-daykind-shadows-builtin` + `wf017-eenheidsleutel-schaduwt-kalendereenheid` (arm (a)) + `wf017-dagsoortnaam-bindt-vreemde-dagsoort` (arm (b), the frontend's own emission) + the three of the thirty-spelling widening: `wf017-eenheidsleutel-schaduwt-kalenderafkorting`, `wf017-dagsoortnaam-schaduwt-kalenderafkorting`, `wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm` |
| WF-018 | parameter datatype resolves; timeline facet well-formed | gate 3 (datatype clause; the facet clause carries the WF-005 caveat) | `jutterswet-penningjaar` | `wf018-parameter-onbekend` |
| WF-019 | serialization preserves declaration order | `scripts/check-ir-schema.sh` (byte-identical round-trip) | `jutterswet-vondsttelling` (multi-element ordered lists round-trip) | OPEN (no data-shaped counterexample exists: the canonical re-emit is a serializer property, so a reordering fault lives in a consumer, never in admissible data — recorded) |
| WF-020 | form names unique across all NAMED forms (ci) — the ch. 3 versioned rule forms and the ch. 6.1 defaults groups, one namespace. *Widened 2026-09-12* to the defaults groups: the sentence already said "across all rule forms" and only the list the check read was short, so this is an existing number reaching a form it always described, not a new rule. Deliberately NOT split off under its own number — one property, one number, or one collision would cite WF-020 between two rules and something else between a rule and a group. The widening also narrowed a false second report: the bare-versus-versioned arm now needs a versioned member to mix with, since without one the entries are an exact duplicate the first arm already names (it frees nothing — with no versioned member the stripped name IS the name) | gate 3 (via the v0 " #i" strip-and-group migration reading) | `jutterswet-vondstnummer`; and every defaults golden, whose group names differ from every rule name — measured 2026-09-12 over the four zones (golden 184, rood 26, negatief 386, parity 30): 626 documents, 1107 named forms of which 69 are defaults groups, and not one collision outside this rule's own witnesses | `wf020-crossform-collision` (between two ch. 3 forms) + `wf020-defaultsgroep-botst-met-regel` (a group against a rule) + `wf020-defaultsgroep-duplicaat` (two groups) — the three fall independently: a check reading only the ch. 3 list refuses the first and passes the other two, and one that adds the groups to only the cross arm or only the within arm passes one of those two |
| WF-021 | a named form's version validity windows do not overlap — a test performed on the RAW bound strings, which is calendar-correct only because WF-101 (ch. 4.1) demands the lexeme that makes lexicographic order calendar order | gate 3 | `anchor/jutterswet-jutheffing` (two dated versions of one rule name, and the replaying runtime witness: per-window absolute values through both executors, gates 5–6) | `wf021-version-overlap` |
| WF-022 | rule target resolves; characteristic value is the constant true | gate 3 | `corpus/golden/jutterswet-penningdrempel/` | `wf022-target-unresolvable` (the target clause) + `wf022-kenmerk-onwaar` (the value clause: a flat attribution with `onwaar`, which forced through splits the executors — ref absent, the ASP backend a translation fault, the Drools exec branch present, the DRL-text branch absent) |
| WF-023 | assignment typing (family, units, sign, keuzetype domain, precision gate) at every attribute write position: a rule target (`GELIJKSTELLING`, `BEREKENING`, `INITIALISATIE`; `KENMERKTOEKENNING` is WF-022's), an object-creation attribute member, a defaults consequence (recursing into exception groups) | gate 3 (the shallow slice between the target's DECLARED datatype — its serialized type where the target has no declared attribute datatype: an unresolvable target, which WF-022 forbids, or a CHARACTERISTIC target, which WF-022 admits and whose value it already pins to the boolean constant true — and the value's serialized type: family (numeric kinds as one), unit compatibility with literal adoption, contradictory sign restrictions, and the keuzetype clause (same variant, same domain name, ci, through a `ListType` element; one-sided only within a family, where the family message would not already name the defect). The domain clause is **keuzetype-scoped** (ch. 2.4's family) and stays so: the struct-domain clause over these same three write positions is **WF-090**, its own rule (maintainer decision 2026-09-03), sharing this function's machinery under its own number. The unknown sentinel defers — WF-034 refuses it at all three positions anyway. The precision gate needs expression-shape analysis and stays OPEN) | `jutterswet-vondsttelling` (numeric assignments) + `jutterswet-keurstaat` (the enum assignment) + `jutterswet-penninguitgifte` (anchor: object-creation members) + every prioritizedDefaults golden — and a measured zero over-rejection across every golden and rood document and, read-only, the parity zone | `wf023-text-into-numeric` + `wf023-schrijf-enum-domein` + `wf023-schrijf-som-domein` + `wf023-schrijf-spookdomein` (a ghost domain at a write) + `wf023-schrijf-doeltype-leugen` (target type and value type share the lie: the cross is against the DECLARED datatype) + `wf023-schrijf-lijst-domein` + `wf023-schrijf-lijst-geen-domein-doel` + `wf023-schrijf-lijst-geen-domein-waarde` (the two one-sided arms within the list family) + `wf023-objectcreatie-domein` + `wf023-objectcreatie-soort` + `wf023-defaults-domein` + `wf023-defaults-soort` (positions 2 and 3, domain and family) + `wf023-defaults-exceptiegroep-domein` (the recursion into exception groups, which the two root-consequence cases do not pin) + `wf023-initialisatie-domein` (the third rule kind at position 1, which the `GELIJKSTELLING`/`BEREKENING` cases do not pin) |
| WF-024 | allocation method is one of the three real methods (no COMPLEX) | gate 3 | `jutterswet-berging` | `wf024-complex` |
| WF-025 | allocation fields valid per method; ordering key orderable; share typing | gate 3 (field validity and orderability; share typing is WF-023) | `jutterswet-berging` | `wf025-max-without-ratio` |
| WF-026 | at most one allocation per written target | gate 3 | `jutterswet-berging` | `wf026-two-allocations-one-target` |
| WF-027 | object-creation members declared on the created type; characteristic init boolean (an attribute member's value typing is WF-023's second write position) | gate 3 | `jutterswet-penninguitgifte` (anchor: the mint, its member initialisations and both guards execute, gates 5–6) | `wf027-ghost-member` |
| WF-028 | symbolic fact creation present, all-or-nothing and resolvable; join chain reaches target; new ≠ final hop | gate 3 (symbolic extension form, and — since 2026-09-04, fifth wave — its PRESENCE: a bean-field-only record is ill-formed, where it was previously recorded as uncheckable) | `jutterswet-gemeenteberging` (anchor: the chain walk executes and folds over the created relation pin absolute values, gates 5–6) | `wf028-half-symbolic-creation` (all-or-nothing) + `wf028-create-along-final-hop` (the full chain walk) + `wf028-bean-only-creation` (the third corner — none of the symbolic keys: `ref` and clingo refuse the document by name while Drools replays it to the pinned expectations, so the corpus's three witnesses now cover none, some and all) |
| WF-029 | consistency assertion has a single subject | gate 3 | `jutterswet-vondstnummer` | `wf029-no-subject` |
| WF-030 | uniqueness sources: keys resolve; arity, datatype kind and units agree per position; a union carries ≥ 2 sources | gate 3 | `jutterswet-vondstnummer` + `anchor/jutterswet-kustregister` (the union — including the per-source sum over two same-type sources — and both scoped forms, executed by gates 5–6) | `wf030-arity-mismatch` + `wf030-single-source-union` |
| WF-031 | the defined day kind is NAMED and declared; one definition per day kind; condition present and **pure** (no attribute/characteristic read — the stratum-0 framing, ch. 5.4); validity ALTIJD. *Presence added 2026-09-12*, found while measuring WF-107 and closed here rather than there because the two names live in two namespaces: a null slipped past the resolution cross (which had nothing to compare) and left a definition bound to nothing, the same silently-dead shape the validity clause already refuses. Measured before it: verifier PASS, and on `jutterswet-schouwkalender`'s own case no answer moves — the defect is a lost definition, not a wrong number, which is why it needs a rule and not a gated case. The BLANK half is a second arm and it is tested BEFORE resolution on purpose: a blank name is not a declared day kind either, so under the natural clause order the resolution arm answers first with `undeclared day kind ' '` — true, and the wrong diagnosis, leaving the presence clause unreachable on every document that does not also declare a blank day kind. Cost: none — 24 day-kind definitions over the four zones (golden 6, negatief 17, parity 1) and every one of them named except this rule's own witness. **POSITION ARM added 2026-09-19** (the day-kind semantics card): the purity clause gains a NAME-AWARE arm at the ContextValue node — `de dag` is admitted, since it IS the bound day the predicate is applied to, and the two SESSION values (`Rekendatum`, `Rekenjaar`) are refused. It is a semantics rule, not a spelling one, and it became answerable only when the duration forms started evaluating the voorwaarde per calendar day (`RUNTIME-SEMANTICS.md` ch. 3.8): a session-dependent predicate classifies the same calendar day differently in two runs of the same document, which is exactly what ch. 5.4's stratum-0 framing denies. Observability before it: none — measured 2026-09-10, one field of `jutterswet-schouwrooster` turned from `de dag` to `Rekendatum` left `verifier: PASS` and `ref: oracle: PASS (7 expectations)`, because the walk matched `Constante` name-blind. The arm is EXACT in both directions: it names the two session values only — matched through `norm`, where WF-098 matches BYTE for byte, so a lower-case `rekendatum` in a day-kind voorwaarde reports BOTH rules, each for its own reason — so `wf098-onbekende-contextwaarde-naam`, whose `de dagX` sits in that very position, keeps reporting WF-098 alone, and the EMPTY half (`naam: null`) is untouched, still the open point ch. 4.1 records. Cost: zero, measured over all 673 documents of the four zones plus the parity zone — **31** `Constante` nodes inside a `resolvedDagsoorten` voorwaarde, **29** of them `de dag` and the other two the witnesses named here | gate 3 | `jutterswet-schouwkalender` (two pure day-kind definitions with ALTIJD validity, replayed by gates 5–6 over its ordinary rule) + `jutterswet-schouwrooster` + `jutterswet-bergingsklok` (the first day kind whose predicate is EVALUATED, by both duration forms) | `wf031-missing-condition` + `wf031-dagsoort-venster` (bounded validity window) + `wf031-dagsoort-leest-kenmerk` (the purity clause: a day-kind condition reading a characteristic) + `wf031-dagsoortdefinitie-naamloos` (the presence clause) + `wf031-sessiewaarde-in-dagsoort` (the position arm: the 2026-09-10 mutation turned into a witness) |
| WF-032 | start point: variable-start timeline, day-precision member, one per pair | gate 3 | `jutterswet-vormenkabinet` (the meetdatum anchor) | `wf032-fixed-start-anchor` |
| WF-033 | (dialect) rule-group members resolve; recursive group has a creation member (either kind); object creations guarded, relation creations guard-free | gate 3 | `anchor/jutterswet-wachtersketen` (the recursive chain executes to its fixpoint, gates 5–6; the runaway budget fault is red-pinned, gate 8) + `anchor/jutterswet-erflijn` (the guard-free relation-creation member: the transitive closure reaches its fixpoint in both executors) | `wf033-unguarded-recursive` (the guard clause) + `wf033-no-creation-member` (the containment clause) |
| WF-034 | every expression/condition reference resolves (no unknown-type sentinel, at the top level and anywhere in the PAYLOAD SUBTREE of a value node's type — the sum chain and the `ListType`/`StructType` containers under it alike; a reference NAMES both halves of what it resolves through — the object type and the member — a parameter reference names its parameter, a characteristic check names its characteristic, a dimension-cell read holds an attribute reference and nothing else, an allocation names its source and target attribute, spells no blank name in any of its six attribute-name fields, resolves every name it does spell against the object type of that name's OWN SIDE, and names both those object types; a fact creation's four v0 BEAN fields each name a declared role of the relation their symbolic sister names, or — for the intermediate step's `type` — the object type that hop reaches; an object creation names a declared `subject` and carries one at all; a near-side role check names an `anderTeFiller` its subject is related to and carries one at all; a FAR-side role check's `filler` and `backref` meet on one declared fact type — the back-reference naming the subject's own role, the filler the type at the other end — and it carries both at all (ch. 4.6 (t)); every key of an object type's eight per-member SIDE TABLES names a declared member of it in that member's declared spelling; a characteristic check's explicit `eigenaar` names a declared object type; a unit's `stelsel` names a declared unit system; every VALUE of the day-kind unit registry `dagsoortPerEenheid` names a day kind `dagsoorten` declares, the map's key side staying free by decision (ch. 4.6 (s)); and a rule target carries a type) | gate 3 (partial: sentinel types and parameter/day-kind/element-type/own-member references; variable bindings and navigation paths are not serialized in v0; excepted: a v0 instance-fold source argument names a role and carries the sentinel by construction — the row-1 wart, resolved against declared roles with the ch. 4.3 type-pair fallback for a name that resolves nowhere, and since wave 4 that exception is SCOPED to the wart, which the wire spells with exactly that sentinel: a fold source carrying a real datatype resolves as the ordinary attribute reference it is). **Widened 2026-09-04/09** (maintainer decision) in SEVEN waves, FIVE fix rounds (one per wave from the third on) and one single-arm card (2026-09-09, arm (s)), no new rule number and no documented deferral; the arm count is given a denominator anyone can re-take rather than a round number: **45** `v "WF-034"` emission sites in `spec/verifier/lib/checks.ml` (recounted 2026-09-09; 43 before arm (s), which spells its two value spellings at two sites), which is what a grep for that literal returns today; every one of them closes a **self-served exemption from the identity family** (WF-089/WF-090/WF-091), which defers on an unresolvable declaration and on a deferring type — so an emptied field or a sentinel switched the unit, precision and domain checks off on a document's own say-so while it stayed verifier-clean. *Wave 1* — an `AttributeReference` without an `objecttype` in a value position, the same in a FOLD SOURCE (its own arm: the aggregation branch replaces the generic walk and resolves only a named endpoint, so the empty field fell through both fences), and the sentinel on a rule TARGET's `type`. *Wave 2* — the same for the OTHER half of the pair, an `AttributeReference` without an `attribuut`, again in both positions (with the value-position arm alone 43 of the goldens' 751 emptied `attribuut` positions stayed clean, all fold-/aggregation arguments; with both, 0 of 751 — the same lesson the fold source taught in wave 1); a `ParameterRef` without a `parameter` (35 of 37 → 0); the UNSPELLED TWIN of the target sentinel, a target with no `type` at all (295 of 295 → 0), which wave 1 left out for want of a witness and which is the more dangerous half — it also switches off WF-007's target clause, and unlike every other shape here it replays CLEAN in `ref` and clingo while the Drools backend throws, so the verifier is the only place it can be caught before an engine crashes on well-formed input; and the sentinel one level DOWN, on a `SumType` constructor payload inside a value node's type (the fence matched the type's top level only; the walk now descends through payloads, WF-088's descent for the same reason). *Wave 3* — the four channels wave 2 measured and left open, six arms (the container half of them carrying three separately falsifiable branches: the list element, the record field, and the container→sum re-entry, which alone pins `wf034-sentinel-payload-onder-lijst`). A `Kenmerk` CHECK naming no `kenmerk` (89 of the goldens' 96 positions, in twelve position shapes over five top-level sections, stayed clean when emptied one at a time; 0 after), where one empty field silenced four readers at once — this rule's own declared-characteristic cross, WF-043's granularity cross, WF-053's navigated hop and the dependency edge. A `ResolvedVerdeling` naming no `bronAttribuut` or no `doelAttribuut` (11 of 11 each; 0 after) — its own function beside the expression walk, because an ALLOCATION is not an expression and its two endpoint attributes are the one reference pair `walk_expr` cannot reach, so WF-025's ordering-key check, WF-026's same-target comparison, WF-050's defaults overlap and both stratification edges keyed on them fell silent together. A `DimensieCel` whose `attribuut` is null **or holds a non-reference node** — two arms for two ways of missing one pattern, the cell arm resolving only through a complete inner `AttribuutRef`; measured on an existing artifact rather than argued, since emptying or overwriting `wf091-dimensiecel-eenheid`'s cell reference turns its pinned unit refusal off and the document CLEAN, and this is the one wave-3 channel with **no executor witness under today's executors** (a cell read is declined as a FORM by both OCaml executors, so no golden carrying one has or could have a case file today). The harm is pinned on FOUR existing artifacts, each measured under both spellings with the verifier of the day before this card and each turning `verifier: PASS` outright: `wf091-dimensiecel-eenheid`, `wf089-dimensiecel-enum`, `wf090-dimensiecel` and `wf040-unpinned-cell` — WF-040's label/dimension cross rides on the same match and goes with the identity family. The structural half of the argument, beyond the corpus: the reference frontend's IR types this field as an attribute reference, so its own emissions cannot reach the non-reference spelling; the clause is for the wire, which any conforming producer may write. And the CONTAINER half of wave 2's payload descent: the sentinel in a `ListType` element or `StructType` field under a constructor payload, and a sum reached back THROUGH such a container — the descent is now mutually recursive over the whole payload subtree, and the sum-chain messages it already pinned are unchanged. *Fix round, same day, same decision* — the FILLED half of wave 3's allocation arm: a present attribute name on any of the allocation's six name fields (`bronAttribuut`, `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut`, `restantAttribuut`, `groepeerAttribuut`) is not blank, emptiness decided by the ch. 2.1 name identity, while a null on the four facet fields keeps its meaning of a facet switched off. `""` satisfied every consumer's `Some a` and named nothing, so it walked through the null arm into the same silence: 11 of 11, 11 of 11, 7 of 7, 4 of 4, 9 of 9 and 3 of 3 golden positions stayed verifier-clean when blanked one at a time, and none does now. The executor consequence is the reason this needed a clause rather than a note: loud on the two endpoints and on `restantAttribuut`, but on `ratoAttribuut` and `groepeerAttribuut` BOTH executors play on and agree on a wrong answer, and on `maximumAttribuut` `ref` plays on (600 for a pinned 300) while the ASP backend refuses — ref and clingo diverging on a verifier-clean document. Wave 3's over-rejection check is direct and total: the verifier's verdict over all **462** documents of golden + rood + negatief + parity that PREDATE this card (153 + 22 + 257 + 30) is unchanged, with four pre-existing allocation negatiefs gaining a truthful incidental WF-034 line while keeping their verdict and the rule their directory declares; the eleven new negatiefs the card and its fix round add — negatief zone 257 → 268, four zones 462 → 473 — are the only additions, and a byte-level comparison of the two verifiers' reports over all 473 documents changes exactly those 15 reports and nothing else. All eight wave-1/2 shapes are carried by **zero** documents outside their own negatief witnesses (152 golden + 22 rood + 30 parity, plus every negatief that predates the wave, measured before and after each), so over-rejection is impossible by measurement and nothing repinned — and the wave-2 over-rejection check is direct: the verifier's report over all 447 documents of golden + rood + negatief + parity is byte-identical before and after but for the five new negatiefs. Nor does anything this repo's own frontends emit carry them (28 rgs goldens, 12 catala goldens, 45 parity fragments, all measured clean) — the shapes are open to any other conforming emitter, which is the audience of an engine-neutral IR. *Wave 4, same day, same decision* — the NAMING class the first three waves kept measuring and leaving behind: not a field left empty but a field filled with a name that resolves against nothing, three arms. A filled attribute name on ANY of the allocation's six name fields resolves against the declaration scope of its OWN SIDE — `bronAttribuut` and `restantAttribuut` against `bronType` (the pot, and the remainder written back on the source), `doelAttribuut`, `ratoAttribuut`, `maximumAttribuut` and `groepeerAttribuut` against `doelType` (the share, and the three facets read per recipient); an undeclared TYPE is another rule's business (WF-047 on the bound form, WF-048 on the bindingless one, both RULES measured loud — the executors are not: an undeclared `bronType` leaves `ref` silent with every share *leeg* while clingo refuses, ch. 4.6 (j)), so the arm resolves only where there is a scope to resolve in. Measured with `bestaatniet` one field at a time, named to their counts in the order the clause lists the fields (source side first): `bronAttribuut` 11 of 11 and `restantAttribuut` 9 of 9 against `bronType`; `doelAttribuut` 11 of 11, `ratoAttribuut` 7 of 7, `maximumAttribuut` 4 of 4 and `groepeerAttribuut` 3 of 3 against `doelType`. Every one of those golden positions stayed verifier-clean, none does now — and the observability rows are the fix round's exactly, one spelling on: loud on the two endpoints and `restantAttribuut`, BOTH executors silent and agreed on `ratoAttribuut` (the shares stay underived where the anchor pins 60 and 40) and on `groepeerAttribuut` (300 apiece where 1200/0/0/0 is pinned), and `ref` silent while clingo refuses on `maximumAttribuut` (600 for a pinned 300). An allocation NAMES both its object types: 11 of 11 each stayed clean with `bronType` or `doelType` emptied while both executors fault by the field's own name, and a null there also silences WF-025's ordering-key datatype, WF-050's defaults overlap, both stratification edges and this wave's own resolution scope. And the ch. 4.3 fold-source exception returns to its SCOPING: the row-1 wart is the fold-source argument carrying the unknown sentinel on its own type — that is how v0 spells it, a role name having no datatype to give, and it is the discriminator both OCaml executors already key their role-naming branch on (the Drools backend keys the same reading on `kind = AANTAL` plus its own role derivation and plays a real-datatype source both of them refuse — measured, ch. 4.3) — so a fold source carrying a real datatype resolves as an ordinary attribute reference, through neither the role-name route nor the type-pair fallback. Both branches measured live and separately falsifiable: on `anchor/jutterswet-maatschap`, whose reciprocal self-relation derives one connecting role for ANY name, a SOM source repointed at `bestaatniet` stayed clean while `ref` silently summed 0 where 12 and 30 are pinned and clingo refused; on `jutterswet-vondsttelling`, where the pair `Vondst`/`Vondst` derives NO connecting role and only the role route can admit anything, the same source repointed at the declared plural `vondsten` stayed clean while `ref` summed 0 where 3.75 kg is pinned and clingo refused. The regression boundary was counted before and after: of the corpus's 89 named fold sources — *named* meaning the argument spells both its `attribuut` and its `objecttype`, the pair this arm resolves through; count in the one source that spells an attribute and no object type (a wave-1 shape, refused there) and the totals read 90 and 58 — 32 carry the wart (24 resolving through a declared role name, 5 through the type pair, 3 already ill-formed under this rule's older arms) and 57 do not, of which 54 already resolve as a declared attribute and one names a characteristic (WF-080's arm, unchanged) — so the narrowing rejects nothing that was standing, and the counting folds that lean on the fallback (`jutterswet-kaveltelling`'s synthesized plural, and the declared-plural route of `jutterswet-aanmeldertelling` and `jutterswet-vondsttelling`) keep both routes and replay unchanged in both executors. Wave 4's over-rejection check is direct and total, the same instrument: over all **486** documents of golden + rood + negatief + parity (153 + 22 + 281 + 30) exactly 15 reports change and 10 verdicts — the ten being the wave's own new negatiefs, seven written with the clauses and three more in the fix round that gave the `restantAttribuut` and `doelAttribuut` sub-arms and the `doelType` arm a witness each; no golden and no rood document moves at all, and five documents keep their verdict while gaining truthful incidental lines (the four pre-existing type-less allocation negatiefs, one or two lines each, and the parity casus, whose own `doelAttribuut` names an attribute its target type does not declare — six incidental lines there, one per allocation). Negatief zone 271 → 281, four zones 476 → 486. *Wave 5, same decision* — the same naming class taken OFF the expression walk, to three positions the walk never reaches, ch. 4.6 (m)–(o). The four v0 BEAN-field names of a fact creation (`zetVeld`, `joinVeld`, `tussenstappen[].veld`, `tussenstappen[].type`): a binary relation lives in the v0 bean model as one scalar reference field, so each of the three name fields resolves against a declared role of the relation its symbolic sister names — `feittype`, `joinRol`'s, that hop's `rolBinding`'s — while the step's `type` resolves against the hop itself, the filler of the role that binding binds. The wave admitted BOTH roles and assumed neither, reasoning that the wire does not carry which side stores the reference; its fix round replaced that with the derivation (below), and corrected the corpus reading offered in support — read per creation it is two and two, not three and one, since `anchor/jutterswet-erflijn` carries two of the four golden creations and both spell the target's role (`joinVeld` being the bound role three times and the opposite role once still holds). 14 of 14 golden positions stayed verifier-clean when pointed at a name no declaration carries, 0 after — and this is the family's sharpest observability row: BOTH OCaml executors are blind by construction (they read the symbolic fields and never touch the bean fields, so all 14 replay to their pinned expectations and the differential square has nothing to catch), while Drools reads them reflectively and crashes on all four on `jutterswet-postwacht`, a golden it plays. An object creation's `subject` against the object-type table: WF-027 crosses only `nieuwType`, and the subject was reached solely by rules reading THROUGH it (WF-053 on a navigating member value, WF-047 on an explicit back-link binding) — measured, 10 of 11 golden creations were already refused by exactly those two and ONE stayed clean, the only creation without `toekenningen`, where `ref` and clingo agree on a wrong answer (0 dossiers for a pinned 1) on an ANCHOR golden. And a near-side role check's `anderTeFiller` against the ch. 4.3 role-filler candidates, the executors' own derivation: the written `rol` is presentation, so that one field is the whole reference; 4 of 4 golden positions clean before, 0 after, with `ref` and clingo both refusing loudly and Drools taking a third road (it drops the rule as a transpiler skip, so the document neither faults nor answers). Wave 5's over-rejection check is direct and total: over all **493** documents of golden + rood + negatief + parity that predate the card (154 + 22 + 287 + 30) not one verdict and not one report moves — a stricter result than waves 3 and 4, which each moved incidental lines — and the seven new negatiefs (six here, one under WF-028) are the only additions. Negatief zone 287 → 294, four zones 493 → 500. *Wave 5's fix round, 2026-09-05, same decision* — three arms on (m), where a lens showed the wave had conceded a decision the model actually makes. A binary relation lives in the v0 bean model as one scalar reference field, on the object type filling the TO-MANY role and named after the TO-ONE role (§13.3.9, the storage direction the Drools backend derives symbolically and cites in its own refusal reasons), so where a relation carries exactly one to-one role the bean field MUST name it; at equal cardinality the model derives no side and both names stay admissible, which is where the arm stops — `anchor/jutterswet-erflijn` and `anchor/jutterswet-gemeenteberging` both carry both-to-many relations and a stricter rule would make two anchor goldens ill-formed. The name MUST also be the DECLARED SPELLING byte for byte, one of the two places the ch. 2.1 case-insensitive matching is not enough — the other being the side-table keys of (p), on the different ground that the key IS a member's carried identity — because a bean-model executor turns it into a case-sensitive Java property (measured: an internal capital throws `geen getter voor 'seinVak'`, a first-letter change plays, since the identifier mapping lower-cases the initial — so the arm is knowingly stricter than the executor on that one shape, and `tussenstappen[].type` deliberately gets NO byte arm, its consumer being case-insensitive by documented contract). And all four fields MUST be PRESENT, as ONE list-driven arm rather than four: with one witness, three of four sub-arms move no document at all (0 of 503 measured), and a clause with no counterexample is not a clause. Observability, measured on `jutterswet-postwacht`: the sister role and the misspelling each leave verifier, `ref` and clingo clean while Drools throws a missing getter; of the four absences, two crash it with exactly the exceptions the filled-name witnesses pin and two make it drop the creation as a loud transpiler skip. Zero over-rejection over the 500 documents that predate the fix round; three new negatiefs. Negatief zone 294 → 297, four zones 500 → 503. *Wave 6, 2026-09-05, same decision* — the REST of the naming class off the walk, the field-by-field remainder waves 5 and its fix round measured and left in the open list; twelve arms, ch. 4.6 (n)-(r). The EIGHT per-member SIDE-TABLE keys of an object type (`attribuutMeervoud`, `attribuutDimensies`, `attribuutGranulariteit`, `attribuutTijdlijnAantal`, `attribuutTijdlijnNaam`, `kenmerkSoorten`, `kenmerkGranulariteit`, `kenmerkTijdlijnAantal`) each name a declared member of that type — eight arms because they are eight independent readers, and a mutant removing one frees exactly one document. The harm here is not a missing value but a SILENT SEMANTIC CHANGE: an orphan key hangs a granularity, plural or dimension on a member that does not exist and leaves the intended member at its default, and every reader looks the facet up by the declared name, finds nothing and takes that default. Measured with a spurious key added to each table on each of the 205 golden object types in turn: 205 of 205 verifier-clean before and 0 after per table (`attribuutDimensies` 2 of 2, since its value needs a declared dimension). The complementary instrument, renaming a LIVE golden key: `attribuutMeervoud` 6 of 6, `attribuutGranulariteit` 20 of 33, `attribuutTijdlijnAantal` 1 of 1, `kenmerkSoorten` 124 of 124 clean before and 0 after — and, recorded rather than smoothed over, three tables where the rename is refused already (`attribuutDimensies` 0 of 2, `kenmerkGranulariteit` 0 of 9, `attribuutTijdlijnNaam` 0 of 2) and one with no live golden key at all (`kenmerkTijdlijnAantal`). Executor observability splits the eight in two: `attribuutMeervoud` (8 measurements), `attribuutTijdlijnNaam` (1) and `kenmerkSoorten` (438) are byte-identical before and after in BOTH OCaml executors — nothing sees them, and `kenmerkSoorten` has no reader in any of the four consumers at all, so its arm is for the wire on the same argument the wave-3 cell arm rests on; while `attribuutGranulariteit` (99 measurements: 91 loud in both, 6 silent in both, 2 DIVERGENT — `ref` replaying `synth2-prorata-breed` case4 to its pinned expectations while the ASP backend refuses the pro-rata base) and `kenmerkGranulariteit` (27: 26 loud, 1 DIVERGENT on an ANCHOR golden — `anchor/jutterswet-dubbelwacht` case3, `ref` faulting on `gedurende` while the ASP backend replays it clean — with the caveat that the carrying document is ill-formed independently of this clause and already was before the card, since WF-055 refuses a `gedurende` over a non-temporal characteristic, so the divergence is a second-line finding behind an already-closed door and not this arm's justification; the asymmetry itself is its own card) are read by all three executors. A characteristic check's explicit `eigenaar` names a declared object type: it is the node key of the dependency edge the check contributes, and the bean-model executor's loader resolves it through a null-yielding by-name lookup, so an undeclared owner becomes NO owner and a rule that backend DECLINES under a real owner (measured on `jutterswet-wachtploeg`: a declared `Wachtpost` makes it drop the rule as a transpiler skip) is carried and answered under a nonsense one — with both OCaml executors blind by construction, as at the bean fields; 96 of 96 golden check positions clean before, 0 after, and no golden carries the form (2 of the four zones' 190 checks do, both negatief, both resolving). A unit's `stelsel` names a declared `Eenheidstelsel`: the ASP backend's literal-restating pass gates on it and the identity family reads it as part of a unit's structural identity, and the misspelling is a measured ref-vs-clingo DIVERGENCE on `jutterswet-sleepbaan` (`ref` PASS and Drools PLAYED against `non-identical units under the write to "sleeplengte" are not translated`); 21 of 21 (golden, system) pairs clean before with every use renamed consistently, 0 after — the consistent instrument being required, since a single renamed occurrence breaks unit identity and WF-091 reports that instead — and of the four zones' **2377** unit records (*recounted 2026-09-06: the wave reported 2442 from an instrument that counted the 73 denominator units of composite rates twice, once in their own right and once under the enclosing record*) the **2244** that carry a `stelsel` all resolve, the other 133 carrying null (the percentage unit and the bare calendar units) and passed over by construction. And the two EMPTY halves wave 5 measured and left witnessless, each its own arm because a name resolving against nothing and an absent name are different questions observed differently: an object creation carries a `subject` at all (11 of 11 golden creations clean when emptied, 0 after; both executors LOUD, where the filled half made them agree on a wrong answer) and a near-side role check carries an `anderTeFiller` at all (4 of 4, 0 after; both executors loud). Wave 6's over-rejection check is the same instrument and the same result: over all 510 documents of golden + rood + negatief + parity that predate it (155 + 22 + 303 + 30) not one verdict moves and exactly ONE report does — `wf031-dagsoort-leest-rolstaat` gains a truthful incidental line from the `anderTeFiller` arm and keeps the WF-031 verdict its directory declares, that document being a day-kind condition WF-031 refuses outright, which is why no subject was there to derive a filler from. Negatief zone 303 → 315, four zones 510 → 522. *Fix round, 2026-09-06, same decision* — the side-table identity of (p) tightened from case-insensitive to BYTE-EXACT: the key is not a matching site but the v0 denormalisation of a member's identity, and ch. 2.1 carries identities verbatim, so the key must be the member's declared spelling. The ground is measured, not stylistic: the consumers disagree about how they look these facets up — the verifier, `ref` and the ASP backend case-insensitively, the bean-model executor NOT (108 raw case-sensitive map lookups across four of the eight tables, against five case-insensitive ones through its own name helper) — so under the ci rule a key differing in capitalisation alone is no orphan and the document stays clean in three consumers while the fourth drops the golden out of the differential unseen (measured on `corpus/golden/synth2-clip-over-prorata`, `kenmerkGranulariteit` key `borgplichtig` capitalised: verifier, `ref` and clingo all PASS while Drools goes PLAYED → auto-excluded, taking gate 12 from played 109 to 108 with the gate still GREEN, its only floor being a non-empty played set — so the wire must catch what no gate catches). Cost nil: of the 943 keys the four zones held after the wave, ZERO differ from their declaration in byte while resolving case-insensitively, and zero of 924 before it; the sweep over all 523 documents moves exactly the one new witness, `wf034-zijtabel-kapitalisatiewees`. Negatief zone 315 → 316, four zones 522 → 523. *Wave 7, 2026-09-06, same decision* — the unknown-type fence rather than the naming class, ONE arm (r) on the branch that replaces the expression walk: the unknown type is refused on every fold operand that is not a fold SOURCE, in BOTH its spellings — the `Onbekend` sentinel and a `type` absent altogether — because the walk's own prologue has always refused the pair together and this branch had repeated neither. The exemption is the ch. 4.3 wart and exactly it, keyed on the pair WF-093 keys on (arity one AND a folding kind), so the two rules are disjoint and jointly exhaustive over the branch. Measured one mutant per half: the "single AND `AANTAL`" exemption doubles a citation on exactly the four `wf093-wratvouw-*` negatiefs; reverting the KIND half frees exactly the two `wf034-sentinel-builtin-*` witnesses; inverting the arity guard newly rejects 12 documents (10 goldens, 5 anchors, 2 red); deleting the unspelled-twin arm frees exactly `wf034-sentinel-null-vouwoperand`; deleting the clause outright frees all six operand witnesses and nothing else. Executor observability spans four dead ends over one position — PLAYED with an answer, an `IllegalStateException` on a getter of the role name, a `NullPointerException` on the missing unit of a null-typed numeric operand, and a transpiler decline — and, on the unspelled twin, `ref` on the ANSWERING side, replaying a date golden to all five of its pinned expectations off the declaration while the ASP backend refuses. Cost nil in both halves: zero of the four zones' null `type` positions sit at a fold argument (all three are in negatiefs already rejected for exactly that), and the four zones carried no non-aggregation `Functie` with an `elementObjecttype` at all. Zone growth, stated at the state this wave LANDS on (rebased onto the datum-tijd card, so main's own six documents are already in the base): negatief zone 330 → 337, four zones 539 → 546. Over that sweep exactly seven reports and three verdicts move: the three fix-round witnesses' verdicts (the semantic half, measured alone against the wave as first landed, moves those three and nothing else), plus four report-only restatements of the reworded message — three earlier witnesses and the parity casus, whose violation count is unchanged. The wave's own four witnesses took the negatief zone 325 → 329 and the four zones 533 → 537 before the rebase. *Re-measured 2026-09-06, and the fold-argument flank is CLOSED by wave 7 (r):* injecting the sentinel one position at a time at all **2701** datatype-valued golden positions of today's corpus leaves **ZERO** survivors — before wave 7 there were **6**, every one of them a LISTED fold operand (six positions over three listed value folds in two goldens: `jutterswet-peildatum` twice and `jutterswet-sluitsom` once), which is the whole of what wave 2's instrument then reported as "fold-/aggregation argument types". (The denominator is today's corpus state and a wave-2 count of 2758 over a smaller corpus is not comparable; the numerator is what carries the claim.) What remains is not a survivor but the documented exemption: the row-1 wart itself, the 15 no-op positions that carry the sentinel by construction, gedoogd and documented — narrowed by wave 4 to the position ch. 4.3 scopes it to, but the sentinel it carries there is still unfenced, and so is the name written beside it. *That residual width was counted over the four zones:* of the 32 wart sources, 26 sit on a type pair that derives exactly one connecting role, so the fallback admits ANY name there; on 18 of those 26 WF-047 crosses the written name against the bound role of an explicit binding, which leaves **eight** positions in which an arbitrary name simply stands — and only **two** of them are golden, `jutterswet-aanmeldertelling` and `jutterswet-kaveltelling`, the corpus's two bindingless counting folds. That is the whole of what the wart still buys a document, and it is why the narrowing is a scoping and not a repeal. (The two name-resolution gaps wave 3 recorded here as open — a name no declaration carries on any of the allocation's six attribute-name fields, and an empty `bronType`/`doelType` — and the fold-source fallback's own width are CLOSED by wave 4, ch. 4.6 (j)–(l).) Still open, and another rule's walk to make: WF-007's precision-form check, which has the identical blind spot at the payload positions wave 2 closed and in the containers wave 3 closed | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondsttelling` (role source) + `jutterswet-schouwrooster` (arm (s): four entries resolving, one of its keys load-bearing as a declared `eenheid` the executed arithmetic runs with) + `jutterswet-vormenkabinet` (arm (s)'s IDENTITY half: two entries that resolve only under `norm`, a capitalised value and a padded one, so a byte-exact comparison takes gate 3 down — verifier-only, the golden carries no case file) + `jutterswet-kaveltelling` (the type-pair fallback: an undeclared synthesized plural over a unique pair, both executors — and, since wave 4, the corpus's one golden fallback user, so it is the standing regression witness for the narrowed scoping) + `jutterswet-postbezetting` (arm (t): the far-side role check's first corpus carrier anywhere, hand-written as the witness the clause had to have before it could be written) — and the non-over-rejection argument splits in two, because the waves do. For the EIGHTEEN arms of waves 1-4 and their fix round, every golden and rood document is a non-over-rejection witness at once, since all eighteen shapes carry zero occurrences in those two zones (the only occurrences anywhere are the wave-3 and wave-4 arms' four pre-existing allocation negatiefs and the parity casus, which gain incidental lines and keep their verdict). For the NINE arms of wave 5 and its fix round, and for the TWELVE of wave 6, that is not the argument, and must not be borrowed: those arms cross positions the corpus does carry — 14 bean-field names plus 4 hop types, 14 object creations and 5 near-side role checks across golden and rood (11 + 3, 11 + 3 and 4 + 1); and, for wave 6, 924 side-table keys over the four zones (177 of them golden), 190 characteristic checks and 2244 unit records carrying a `stelsel` — and every one of them resolves. There the evidence is the sweep, not absence: 493 documents byte-identical across wave 5, 500 across its fix round, not one verdict and not one report moved; 510 across wave 6, not one verdict moved and exactly ONE report did, the incidental line `wf031-dagsoort-leest-rolstaat` gains from the `anderTeFiller` empty half | `wf034-dangling-parameter` + `wf034-ambigu-typenpaar` (unresolved name, two connecting fact types) + `wf034-zelftype-typenpaar` (unresolved name, one self-typed two-role fact type — both roles are candidates, counted in roles not fact types) + `wf034-typeloos-literal` (the unspelled twin of the sentinel, on a value node) + `wf034-attribuutref-zonder-objecttype` (the missing endpoint, carrying the measured answer-changing `t`-for-`kg` lie the empty field hid: `ref` faults, clingo refuses, Drools played it) + `wf034-foldbron-zonder-objecttype` (the same field emptied in a fold source, the same lie, and the worse measurement: `ref` PASS, clingo PASS, Drools PLAYED — all three answer off it) + `wf034-onbekend-regeldoel` (the sentinel on `/resolvedRegels/1/doel/type`; its NOTE names the `SumType` constructor payload as a second surviving position, which is true of the wave it was written in — since wave 2 the payload half is closed and only the row-1 fold-source wart remains) + `wf034-attribuutref-zonder-attribuut` + `wf034-foldbron-zonder-attribuut` (the other half of the pair, once per path, each carrying the same `t`-for-`kg` lie: `ref` and clingo fault, Drools skips the rule or throws) + `wf034-parameterref-zonder-parameter` (same lie on a parameter operand; `ref` and clingo fault, Drools throws) + `wf034-regeldoel-zonder-type` (the unspelled twin: `ref` PASS, clingo PASS, Drools throws — verifier-clean input crashing an executor) + `wf034-sentinel-sumtype-payload` (the sentinel a level down: `ref` replays it, clingo refuses to translate it — one document, two executors disagreeing about whether it exists) — and, for wave 3, eight more, one per form and per spelling: `wf034-kenmerkcheck-zonder-kenmerk` (`ref` "missing KenmerkCheck.kenmerk", clingo "missing kenmerk") + `wf034-verdeling-zonder-bronattribuut` + `wf034-verdeling-zonder-doelattribuut` (the allocation pair, each half its own document, `ref` and clingo faulting on the field's own name) + `wf034-dimensiecel-zonder-attribuut` + `wf034-dimensiecel-geen-referentie` (the two spellings of the cell escape, both VERIFIER-ONLY — a cell read is declined as a form by both executors, so the harm is pinned by `wf091-dimensiecel-eenheid` turning CLEAN when its cell reference is removed, not by a replay) + `wf034-sentinel-lijstelement-payload` + `wf034-sentinel-structveld-payload` + `wf034-sentinel-payload-onder-lijst` (the container half: the list branch, the record branch, and the container→sum direction of the recursion, each with `ref` replaying to its pinned expectations while clingo refuses to translate) — and, for the fix round, three more, one per measured executor shape of the blank name: `wf034-verdeling-lege-bronattribuut` (the loud shape: both executors fault) + `wf034-verdeling-lege-maximumattribuut` (the DIVERGENT shape: `ref` plays on and returns 600 for a pinned 300 while clingo refuses to translate) + `wf034-verdeling-lege-groepeerattribuut` (the SILENT shape: both executors play on and agree on the same wrong distribution — 300 apiece where 1200/0/0/0 is pinned — which is the one place only the anchor pinning stands between the corpus and a defect) — and, for wave 4 and its fix round, ten more, one per measured executor shape of the RESOLVING name and one per sub-arm that had none: `wf034-verdeling-onbekend-ratoattribuut` (the silent-and-agreed shape on an ANCHOR golden: neither executor faults, both leave the shares underived where 60 and 40 are pinned) + `wf034-verdeling-onbekend-groepeerattribuut` (the second silent-and-agreed shape, and the sharper one: both compute a whole distribution and it is the same wrong one) + `wf034-verdeling-onbekend-maximumattribuut` (the DIVERGENT shape: `ref` plays on uncapped at 600 for a pinned 300 while clingo refuses to translate) + `wf034-verdeling-onbekend-bronattribuut` (the loud shape, present so the two silent rows are not mistaken for the whole class) + `wf034-verdeling-onbekend-doelattribuut` (the second loud shape, on the field the allocation WRITES — the one the parity casus carries six times over) + `wf034-verdeling-onbekend-restantattribuut` (the third, and the sub-arm that left no trace at all: before this witness, deleting its one line changed no document in any of the four zones) + `wf034-verdeling-zonder-brontype` + `wf034-verdeling-zonder-doeltype` (the missing object type, both executors faulting by the field's own name — one witness per half, since each type is its own match arm and deleting either alone moves only its own document) + `wf034-foldbron-onbekend-attribuut` (the type-pair branch of the scoping, on `anchor/jutterswet-maatschap` where a reciprocal self-relation derives one connecting role for any name at all: `ref` silently sums 0 where 12 and 30 are pinned, clingo refuses) + `wf034-foldbron-rolnaam-waardevouw` (the role-name branch, deliberately placed on `jutterswet-vondsttelling` where the type pair derives NO connecting role, so only the role route could ever admit it — which is what makes the two branches separately falsifiable: each mutant frees exactly one of the two documents) — and, for wave 5, six more, one per arm: `wf034-feitcreatie-onbekend-zetveld` + `wf034-feitcreatie-onbekend-joinveld` + `wf034-feitcreatie-onbekend-tussenstapveld` + `wf034-feitcreatie-onbekend-tussenstaptype` (the four bean fields, all four on `jutterswet-postwacht` — the one of the three fact-creation goldens Drools actually plays, so each carries a real crash and not a hypothetical one: a missing getter via a consequence exception, two constraint-evaluation exceptions, and a null dereference of the object-type lookup, while `ref` and clingo replay all four to their pinned expectations) + `wf034-objectcreatie-onbekend-subject` (the toekenningsloze creation on an anchor golden: no fault anywhere, both executors agreeing on 0 where 1 is pinned) + `wf034-rolcheck-onbekend-andertefiller` (both OCaml executors refusing by the same sentence while Drools drops the rule) — and, for wave 5's fix round, three more on the same golden: `wf034-feitcreatie-zusterrol` (a DECLARED role of the created relation, and still a getter that exists on neither bean) + `wf034-feitcreatie-hoofdletterveld` (the byte-exact arm; an INTERNAL capital, because a first-letter one is harmless and would have made the crash claim false) + `wf034-feitcreatie-loos-tussenstapveld` (the presence arm, on the absence that crashes rather than one of the two that skip) — and, for wave 6, twelve more, one per arm: `wf034-wees-attribuutmeervoud` + `wf034-wees-attribuutdimensies` + `wf034-wees-attribuutgranulariteit` + `wf034-wees-attribuuttijdlijnaantal` + `wf034-wees-attribuuttijdlijnnaam` + `wf034-wees-kenmerksoorten` + `wf034-wees-kenmerkgranulariteit` + `wf034-wees-kenmerktijdlijnaantal` (the eight side tables, seven of them one added key on `jutterswet-vondstverjaring` and the dimension one on `jutterswet-vormenkabinet`, whose declared dimension the value needs; the two granularity witnesses carry the divergences, the other six the silence) + `wf034-kenmerkcheck-onbekende-eigenaar` (on an ANCHOR golden, with both OCaml executors blind and the bean-model executor silently re-attributing the check to the subject) + `wf034-eenheid-onbekend-stelsel` (the ref-vs-clingo divergence: `ref` plays it, the ASP backend refuses the write) + `wf034-objectcreatie-loos-subject` + `wf034-rolcheck-loze-andertefiller` (the two empty halves, on the same two documents their filled-name siblings break, so each position is witnessed in both spellings) — and, for wave 7 (r), seven more: `wf034-sentinel-gelest-vouwoperand` + `wf034-sentinel-gelest-wratoperand` + `wf034-sentinel-geleste-setlezing` (the three spellings of the sentinel on a LISTED fold operand, all three rood-eerst and each with a different outcome in the third executor — played, thrown, declined) + `wf034-sentinel-kale-lezing` (the plain read, labelled in its NOTE as the one witness that was not rood-eerst: it pins the walk's own prologue arm, which had no case anywhere in the zone) — and, from the fix round the same day, `wf034-sentinel-builtin-operand` + `wf034-sentinel-builtin-wratoperand` (the KIND half: a single-argument builtin carrying an `elementObjecttype` has no fold source either, and the second of the two CRASHES the third executor on a getter of the role name) + `wf034-sentinel-null-vouwoperand` (the unspelled twin, and the row where `ref` ANSWERS: two executors agree off the declaration while the ASP backend refuses, and a unit-bearing numeric variant of the same shape throws in the third) — and, for arm (s), one more: `wf034-dagsoorteenheid-onbekende-dagsoort` (a `dagsoortPerEenheid` value naming a day kind no declaration carries, on the load-bearing plural key of `jutterswet-schouwrooster`; the row where NO consumer notices — verifier PASS, `ref` PASS, the ASP backend PASS and the Drools backend PLAYED, all four reproducing the untouched golden's own outcomes) — and, for arm (t), one more: `wf034-verre-rolcheck-verkeerde-zijde` (a far-side role check whose `backref` names a real role on the FILLER's side, so both names resolve and the pair does not; the row where the form was not refused as a form at all — the two OCaml sentences were bottom sentences of the gate-8 list, which is why the same card gave each executor a named arm) |
| WF-035 | operator typing (numeric, unitless power, date±duration order/unit/precision, unit compatibility, clamp) | gate 3 (over serialized operand types) | `anchor/jutterswet-wachtersketen` (the numeric Plus, executed by gates 5–6) + `jutterswet-vervaltermijn` (date ± duration, the date-left plus and min forms, executed by gates 5–6) | `wf035-power-with-unit` + `wf035-datum-plus-datum` + `wf035-duur-plus-datum` + `wf035-duur-min-datum` + `wf035-uur-op-dagdatum` |
| WF-036 | comparison/predicate typing (ordering, precision, family, domain, builtin operands) | gate 3 (over serialized operand types; the date-ordering arm crosses the date REPRESENTATION since 2026-09-05. A TYPELESS operand defers — the arm is skipped and WF-034 reports it — but the unknown SENTINEL does NOT defer: this arm reports it as a non-date operand, measured, exactly as the numeric-ordering and equality arms report theirs. Tested on day precision only until 2026-09-06: all seven corpus date orderings were day×day, so refusing the admitted millisecond half too moved zero of 504 reports — `jutterswet-meldstip` closed that, ordering a `DatumTijd` attribute against a `DatumTijd` literal in both directions) | `corpus/golden/jutterswet-penningdrempel/` (v0 mirror form) + `jutterswet-vondstnummer` (date ordering, both operands day-precision) + `jutterswet-meldstip` (the millisecond half, EERDER and LATER at a sub-day threshold) | `wf036-order-number-vs-text` + `wf036-datumordening-precisie` (the date-ordering arm's representation crossing — a v1 tightening the RegelSpraak frontend does not yet mirror) |
| WF-037 | aggregation/builtin shape: facet validity and arity per kind. **The filled count** (`AANTAL_GEVULDE`, ch. 4.3, landed 2026-09-20) adds three arms and two of them demand a form instead of forbidding one — exactly ONE argument (no listed form exists, because no written form does), an `elementType` (it is an instance fold), and that argument a SET reading (a non-set source is the subject's own read or the row-1 wart, and neither has a filled count; `ref` already refuses the bare role source at evaluation). The sentinel spelling is WF-093's and this rule steps aside on it, so one malformation carries one rule; the kind is excluded from the "≥ 2 arguments" clause, whose sentence reads its arity law backwards. The three facets it never carries — `emptyFallback`, `dimensionSelection`, `granularity` — are refused by the existing, positively written clauses with no amendment, which is why each needed a witness of its own: a positively written MUST grants a facet to a named set and so acquires no failing case when the set grows. **The count** (`AANTAL`, maintainer decision 2026-09-20) is the other kind that clause excludes, and its own arity arm is the mirror image: NOT two or more arguments, because a count folds instances (ch. 3.4a) and the source language spells no operand list for one. It cuts a LIVE producer route: the RegelSpraak frontend's grammar carries an operand list at this rule — an unregistered widening of the one-operand EBNF — through which three sentences emitted unblocked, two with `diagnostics: []`; the frontend refuses them at the node from the same day and the grammar keeps the widening as a registered divergence, deleting an alternative being a parse change rather than a refusal. It closes the gap the 2026-09-19 set-source narrowing left on the other side of the same node — verifier-clean, `ref` answering the count of the FILLED operands, the ASP backend and both Drools branches refusing — and it does not read `elementType`, because a count carrying one BESIDE an operand list is the same listed form with a second claim (there all three executors refuse, and the document is still verifier-clean without this arm). Arity 0 and arity 1 without an element type are NOT a listed form and carry a second count arm (maintainer decision 2026-09-23): a count MUST carry an `elementType`, word for word the filled count's element-type arm, so both counts carry one law at one position. Before it they were verifier-clean and `ref` alone answered — the filled count once more on one own read, and 1 on a single `ListLiteral` whether the list held two elements or none; its value arm under the count now refuses by name. The single exit is an `InstanceSetUnion` argument, whose selections carry the population (WF-110 (c) forbids a second one on the count) | gate 3 (fixed-arity kinds; date-construction arity OPEN) | `jutterswet-vondsttelling` — the one-argument instance count, which this arm leaves untouched by construction (across the four zones, the 112 one-argument counts over an `AttributeReference` all carry an element type and keep their reports, as do the 12 over an `InstanceSetUnion` without one, which take the count-source arm's exit, and the 1 union count WITH one, which WF-110 (c) refuses). **The filled count has no golden yet** — the wire, the rule and `ref` landed first, its worked examples living in `ref/test/` where gate 5's second leg runs them; the corpus witnesses arrive with the clingo and Drools legs, since a golden without a translator breaks gate 6 | `wf037-fallback-on-minimum`; for the filled count `wf037-aantalgevulde-tweede-argument` (the listed spelling), `wf037-aantalgevulde-zonder-elementtype`, `wf037-aantalgevulde-niet-setbron`, plus one per refused facet: `wf037-aantalgevulde-leegwaarde`, `wf037-aantalgevulde-dimensieselectie`, `wf037-aantalgevulde-granulariteit`; for the count `wf037-aantal-opgesomd` (the measured two-operand form), `wf037-aantal-opgesomd-drietal` (the open end of the bound, reported by its operand count) and `wf037-aantal-opgesomd-element` (the spelling that also claims an instance source); for the count's source `wf037-aantal-zonder-elementtype` (one own read), `wf037-aantal-geen-operand` (no argument) and `wf037-aantal-lijstliteraal` (one `ListLiteral`, the spelling that showed the defect) |
| WF-038 | aggregation operand typing (ordering-fold family, units, precision, rate for totaal, year for paasdag, date for the day-precision extractors, tijdsduur operands and node-type unit). **Scope note, 2026-09-20:** the filled count (`AANTAL_GEVULDE`, ch. 4.3) is not a numeric fold in this rule's sense and has no operand-typing clause — its source may carry ANY type, because the test is presence and nothing is added or compared, so neither the numeric clause nor the ordering-fold family reaches it and the ch. 2.4 orderability paragraph has no grip on a fold that ranks nothing. Stated rather than left silent: without it the kind could be read under the numeric arm, and the typelessness of the test would be withdrawn by omission. No negatief corresponds, and that is the point of a scope note — it removes a position from the rule instead of adding one | gate 3 (the ordering-fold family clause — form-independent, per-operand orderability and the cross-operand family with the two date precisions as two families, deferring on the unknown sentinel — plus paasdag, day-precision and sub-day extractors, sum fallback, tijdsduur; two halves stay OPEN and unchecked: the totaal rate clause and unit compatibility between a fold's numeric operands) | `jutterswet-vondsttelling` (the sum fallback slice) + `anchor/jutterswet-jutleeftijd` (the tijdsduur slice, executed by gates 5–6) + `jutterswet-paasvondst` (the paasdag and extractor slice, executed by gates 5–6) + `jutterswet-peildatum` (the ordering-fold slice and the widening's own witness: an articulated `first`/`last` pair over dates beside a second pair over numbers, which the v0 letter would refuse; executed by gates 5–6) + `jutterswet-hoogwaterpaar` (the date pair) + `jutterswet-vondstpiek` (the instance-fold `minimum`/`maximum` over a numeric element) | `wf038-paasdag-text` + `wf038-extractor-text` + `wf038-tijdsduur-gemengde-precisie` + `wf038-tijdsduur-zonder-tijdseenheid` + `wf038-tijdsduur-zonder-node-type` + `wf038-minimum-enum-setbron` + `wf038-maximum-somtype-setbron` (the instance-fold form, once per equality-only family) + `wf038-laatste-enum-geleed` + `wf038-eerste-somtype-geleed` (the articulated form, the same two families — the four crossing the two forms with the two families, one per ordering kind) + `wf038-eerste-gemengde-familie` + `wf038-eerste-gemengde-datumprecisie` (the cross-operand arm's two messages, each separately falsifiable) |
| WF-039 | enum label in domain; literal sign restriction | gate 3 | `jutterswet-keurstaat` (the gaaf label) + `anchor/jutterswet-taxatie` (the sign-restricted literals 2 and 3) | `wf039-negative-under-nonneg` |
| WF-040 | dimension cell pins every dimension; selection labels resolve in order | gate 3 | `jutterswet-vormenkabinet` (the voorjaar cell read) | `wf040-unpinned-cell` |
| WF-041 | counting-quantifier threshold ≥ 1, at BOTH hosts of the vocabulary since 2026-09-16 — the propositional `Compound` and the first-order `QuantifierFold`, whose WF-108 (e) owns the field's PRESENCE and hands its VALUE here (the shared-helper discipline of WF-108 (c), applied to a bound) | gate 3 | `jutterswet-wachtploeg` (ten minste 2, on the composite) + `jutterswet-reddingsvloot` (all three counting quantifiers on the fold, thresholds 1 and 2) | `wf041-threshold-zero` (the composite) + `wf041-kwantorvouw-drempel-nul` (the fold) |
| WF-042 | time window: ≥ 1 bound; bounds are dates, either precision | gate 3 | `jutterswet-vormenkabinet` (the vanaf window) | `wf042-empty-window` |
| WF-043 | whole-period qualifier only on a time-dependent operand | gate 3 | `jutterswet-keurploeg` (whole-year over the MAAND-granular keurvergoeding) + `jutterswet-sleepvloot` (the negated one) | `wf043-wholeperiod-nontemporal` |
| WF-044 | (consumer) Unresolved surfaces, never silently lowered | behavioural — gate 8 (`scripts/check-rood.sh`): the two mandatory executors (`expectedFault.{ref, clingo}`) must both fault, fragment-pinned. The additive `expectedFault.drools` leg (decision V3-b) pins a Drools fault only where the v0 backend carries the red-path form; where it does not, the gate lists the case with a reason (a reason-carrying exclusion list, stale-guarded) — Onopgelost is such a case (its reaching rule does not lower: a transpiler SKIP leaves the value out of scope), so it is listed, never Drools-fragment-pinned | `corpus/rood/wf044-onopgelost-reached` (the red witness: ref faults the evaluation, the ASP backend the translation; the Drools leg lists it — the marker's rule never fires) | n/a (the marker is legal wire content; the fence is behavioural, not well-formedness) |
| WF-045 | dependency graph acyclic (creation recursion excepted) — over the CHAIN-CLOSED node identity of ch. 5.1 since *2026-09-12*, and the three write positions do NOT close alike: a population write closes upward, a member write by a form that ITERATES a type closes over the ⊑-comparable types in both directions, a member write by an OBJECT CREATION closes upward only (it fills the one instance it mints, and creation is not polymorphic), and reads stay literal. Until then both keyed on the written type name alone, which made the rule's promise false of every model with a supertype: the two witnesses below were verifier-clean and diverged in the reference interpreter, which is the last line of defence and reports `no fixpoint within 100 passes` — loud, but naming neither the rule nor the type. The boundary is measured and narrow: a subtype creation is not itself refused (the mirror creation, from subtype subject to supertype, and a creation sideways into a chain both stay clean, as does either inside a guarded recursive group), and two INCOMPARABLE subtypes stay two nodes. Cost: none, and not vacuously so — counted 2026-09-12, the closure widens a write in **9** corpus documents and not one of them gains a cycle, and a tenth — `jutterswet-wrakboeking` — exists precisely because the closure must NOT widen there (gate 3: 209 verifier-clean documents before, 210 after, the one arrival being that witness; gate 11's parity zone unchanged) | gate 3 | `jutterswet-vondsttelling`; the chain-closed reading's own clean witnesses are `anchor/jutterswet-strandrechtketen` and `golden/jutterswet-wrakrecht`, which write ONE inherited attribute from BOTH spellings (a rule at the ancestor, defaults groups at each), `golden/jutterswet-hoofdberging`, whose allocation writes an inherited target attribute from the subtype, `golden/jutterswet-zustervondst`, where a write at the parent reaches TWO incomparable children and they still do not reach each other, and `golden/jutterswet-wrakboeking`, the creation arm's own witness — a creation that mints its own SUPERTYPE and fills an inherited attribute, which the both-directions reading turns into a WF-045 refusal on a model with no cycle in it (measured 2026-09-12, both readings, with ref declining and clingo answering under the wrong one), replayed by gates 5, 6 and 12 | `wf045-cyclic-attribute-dependency` (one type, one spelling) + `wf045-subtypecreatie-cyclus` (the population arm) + `wf045-erfattribuut-spelling-cyclus` (the member arm) — the three fall independently: a graph keyed on the written name catches only the first |
| WF-046 | no negative edge on a dependency cycle (strata exist); since *2026-09-23* every read under a leeg/gevuld test is a negative edge, in both polarities (ch. 5.2) | gate 3 | `jutterswet-penningjaar` (the negated aanmeldplichtig reading — a negative edge, verifier-clean, oracle-replayed); the leeg/gevuld reading has no golden whose test reads a written node (counted 2026-09-23: none of 28), so its clean witness is the reference interpreter's WV-6.4-f (a leeg test in an attribute rule on a derived attribute, off any cycle, answering alike in both rule orders) | `wf046-negated-read-in-recursive-cycle` + `wf046-leegtest-recursieve-creatie` (`negated: false`) + `wf046-leegtest-negatie-recursieve-creatie` (`negated: true`) + `wf046-leegtest-aanwezigheid-recursieve-creatie` (a presence-true test alone: the both-polarities clause) |
| WF-047 | role binding resolves; fillers agree with the use site; written source denotes the bound role | gate 3 | `anchor/jutterswet-bergingstoezicht` (two fact types over one pair; allocation and fold bound, replayed by gates 5–6) + `anchor/jutterswet-maatschap` (the reciprocal binding: bound = opposite) + `anchor/jutterswet-wachtersketen` (the creation back-link binding) | `wf047-dangling-role-binding` (resolution) + `wf047-bound-role-fills-source` (filler agreement) + `wf047-reciprocal-wrong-filler` (the single-role filler clause) + `wf047-creation-binding-wrong-filler` (the creation site) |
| WF-048 | bindingless allocation/instance fold: connecting relation uniquely derivable | gate 3 (contexts with a statically known fold subject; a subjectless context — none exists in v0 dumps, whose folds sit under a typed rule target — is left to the executors' runtime fault) | `jutterswet-berging` + `jutterswet-vondsttelling` (bindingless, uniquely derivable) + `anchor/jutterswet-maatschap` (bindingless over a reciprocal relation) | `wf048-ambiguous-connecting-relation` |
| WF-049 | temporal read: basis temporal (attribute/granular parameter), day-precision date, element type | gate 3 | `anchor/jutterswet-aanmeldquotum` (attribute + parameter reads; hit, gap→leeg, empty date→leeg; replayed by gates 5–6) | `wf049-read-of-nontemporal` (basis) + `wf049-read-at-datetime` (date operand) + `wf049-type-not-element` (element type) |
| WF-050 | defaults group: ≥ 1 default; exactly one target, resolving; default shapes per kind (recursing into sub-defaults, ch. 7.9 — a group-default carries `exceptions` and no own content); init default lowest; no other writer; multiplicity only declared-unordered | gate 3 | `anchor/jutterswet-uitsluiting` (characteristic) + `anchor/jutterswet-vondstwaardering` (attribute + init) + `anchor/jutterswet-toelagegronden` (unordered groups, six conflict-path cases; all replayed by gates 5–6) | `wf050-empty-defaults` + `wf050-two-defaults-one-target` + `wf050-defaults-beside-rule` + `wf050-both-targets` + `wf050-init-not-lowest` + `wf050-rule-beside-attribute-defaults` + `wf050-allocation-beside-defaults` + `wf050-creation-beside-defaults` + `wf050-half-marked-group` + `wf050-subdefault-wrong-shape` |
| WF-051 | no dependency cycle through a defaults group (justification and consequence reads are negative edges, folded recursively through sub-defaults, ch. 7.9) | gate 3 (over the extended dep_forms graph, shared with ref/'s strata) | `anchor/jutterswet-uitsluiting` (the defaults group sits below its negated consumer — strata exist, oracle-replayed) | `wf051-defaults-read-own-target` + `wf051-cycle-through-subdefault` |
| WF-052 | a connecting relation with a to-one created-side role has at most one object-creation writer (fact creations are fault-paired already) | gate 3 | `anchor/jutterswet-penninguitgifte` (one writer per to-one relation, executed by gates 5–6) | `wf052-two-writers-one-to-one-relation` |
| WF-053 | a navigated read resolves its walk: bindings resolve, chain-type, count and written names agree, the endpoint is reached; bindingless steps are uniquely derivable; scalar-slot hops are to-one; bindings on a non-navigating node are ill-formed; a single bindingless written step at zero far hits on a same-typed endpoint is the zero-hop self-reference iff the name denotes a role the subject itself fills (ch. 4.1a) | gate 3 | `anchor/jutterswet-kustwacht` (by-name, chain and by-pair shapes plus the navigated characteristic check, executed by gates 5–6 with leeg-on-absent and the negation asymmetry pinned) + `jutterswet-postwacht` (emitter-produced bindings since the 2026-08-18 pin bump) + `jutterswet-aanmelderkrediet` (the zero-hop self-reference beside the reciprocal far hop, executed by gates 5–6) | `wf053-dangling-navigation-binding` (resolution) + `wf053-chain-near-side-mismatch` (chain typing) + `wf053-to-many-hop-in-scalar` (cardinality) + `wf053-written-role-mismatch` (written-name denotation) + `wf053-bindingless-ambiguous` (unique derivability) + `wf053-zelfverwijzing-vreemde-rol` (the self-reference narrowing) + `wf053-geleste-vouwoperand-onverbonden-hop` (the listed value fold's operand, where the fold branch of the walk replaces the generic one) |
| WF-054 | parent-scoped uniqueness: the scope role resolves (bound or uniquely derivable from the written scope field), its near side fills the subject, the written name denotes the bound role, the scope role is to-one; a scope binding on an unscoped uniqueness is ill-formed | gate 3 | `anchor/jutterswet-kustregister` (bound and bindingless scoped forms, executed by gates 5–6 with the scope-less exclusion pinned) + `jutterswet-postwacht` (emitter-produced `scopeRol` since the 2026-08-18 pin bump) | `wf054-dangling-scope-binding` (resolution) + `wf054-to-many-scope-role` (cardinality) + `wf054-binding-on-unscoped` (the non-scoped guard) |
| WF-055 | the gedurende clip: a filter on the temporal total is a timeline operation — its condition is an own temporal characteristic check, a per-segment comparison of an own temporal attribute against a scalar bound, or a FLAT ALLE composite of characteristic checks (`RUNTIME-SEMANTICS.md` ch. 3.6: the intersection of the members' true-periods; v0 partitions static comparison members into value-side guards and markers nested bullets — v1 narrows composite members to characteristic checks, a recorded narrowing) | gate 3 | `jutterswet-getijdenwacht` (both simple shapes) + `anchor/jutterswet-dubbelwacht` (the flat composite: overlap, disjoint, one-empty and split periods, executed by gates 5–6) | `wf055-nontemporal-clip-kenmerk` + `wf055-navigated-clip-condition` + `wf055-composite-comparison-leaf` + `wf055-composite-nontemporal-leaf` + `wf055-composite-nested` |
| WF-056 | the pro-rata fold (tijdsevenredig-deel) MUST carry a gedurende condition and one temporal source argument (a temporal attribute or granular parameter) as coarse as or coarser than its bucket; on a temporal target the bucket granularity MUST equal the target's declared granularity | gate 3 | `anchor/jutterswet-borgdeel` (the ch. 3.7 fold, executed by gates 5–6) | `wf056-no-gedurende-condition` + `wf056-scalar-base` + `wf056-bucket-target-mismatch` |
| WF-057 | declared names are target-text-safe: no double quote, backslash or control character in any declared/form name (ch. 2.1) | gate 3 | every golden (a universal name-safety property: no single legal name exercises the boundary more than another; gate 3 witnesses none over-rejects) | `wf057-quote-in-attribute` + `wf057-backslash-in-kenmerk` + `wf057-newline-in-rule-name` |
| WF-058 | the day count (aantal-dagen-in, `RUNTIME-SEMANTICS.md` ch. 3.8) MUST carry a gedurende condition over an own temporal characteristic, takes no arguments, and its stated bucket MUST equal the target's declared granularity | gate 3 | `anchor/jutterswet-dagentelling` (the ch. 3.8 worked example verbatim, executed by gates 5–6) | `wf058-no-gedurende-condition` + `wf058-bucket-target-mismatch` + `wf058-with-argument` |
| WF-059 | three declaration arms, one shape (ch. 2.2). (1) every declared attribute carries a datatype — the attribute pendant of WF-018's datatype clause; a null datatype splits the executors on otherwise clean input. (2) every `kenmerkGranulariteit` entry carries a granularity (maintainer decision 2026-09-09): the entry declares its characteristic time-dependent by the presence of its KEY and names the grid in its VALUE, and the readers split on that seam — the WF-055 clip check, `ref` and the ASP translator all decide temporality on KEY EXISTENCE while the shared case loader decides on the VALUE, so a null declares one characteristic temporal to three readers and non-temporal to the fourth. Measured 2026-09-09 on `jutterswet-tegenwacht` with one value nulled: schema-valid AND verifier-clean, after which a case DELIVERING the timeline died at load in both executors with a message contradicting its own document ("a characteristic timeline on a non-temporal characteristic") while a case OMITTING it ran to completion and answered 60 and 120 where the golden pins 30 and 30 — silently wrong, and wrong IDENTICALLY in both, so no differential gate could see it. Closed twice: the schema drops `null` from the enum (gate 2) and this arm refuses it in the verifier for any document that never met the schema; the loader's message followed on 2026-09-11, after arm 3 had repaired the attribute side — key present with a null value now reads "the characteristic is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)", that whole sentence pinned additively in the backend's own suite, while the old wording stays for the key-absent case. Cost: none — 0 of the 534 documents the four zones held BEFORE the rule carried it (1 on the branch head: this rule's own witness), the RegelSpraak emitter guards its write with a non-null test and the Catala emitter's map is empty by construction — though with `null` gone from the enum that emitter's dead constant-`null` branch becomes a latent CONFORMANCE defect, gate-2-invalid the moment its map ever fills, which is loud and defensible but is named so the eventual red reads as expected. (3) every `attribuutGranulariteit` entry carries a granularity (maintainer decision 2026-09-10): the same key-vs-value seam one side table back (`attr_temporal` reads the key, the shared case loader's attribute arm reads the value), but a DIFFERENT CLASS of fault, closed on its own measurement. Measured 2026-09-10 on the pre-arm binary (verifier md5 f55fdba98ccd5ac68bb99492519aa580) with one value nulled: no silent wrong answer anywhere — every path dies — but the DIAGNOSIS is wrong. On a pro-rata base the verifier catches it as WF-056 ("pro-rata base 'borgtarief' is not a temporal attribute"), naming another defect; on an ordinary clip base, where WF-056 does not reach, the document is schema-valid AND verifier-PASS and a delivered timeline then draws "a timeline value on a non-temporal attribute", contradicting the document that declares the attribute temporal; the same document with the attribute delivered SCALAR draws two diagnoses of one defect (ref "temporal attribute … without a granularity" against clingo "the gedurende clip … needs the full timeline"). Closed like arm 2: `null` leaves this enum too and this arm refuses it for any document that never met the schema, while the case loader's message is corrected in the same change to "the attribute is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)" — that whole sentence pinned additively in the backend's own suite, and one voice, since both executors share that loader. The scalar-delivery divergence is deliberately left: both messages are true and loud. Cost: none — 0 of 577 documents over the four zones and the parity zone carried one before the arm (1 on the branch head: its own witness), the RegelSpraak emitter guards this table's write with a non-null test too, and the Catala emitter's constant-`null` branch over this map is the same latent CONFORMANCE defect recorded for arm 2, now on both side tables | gate 3 | every golden (a universal declaration property; gate 3 witnesses none over-rejects) — arm 2's clip witnesses are `jutterswet-tegenwacht` and `anchor/jutterswet-dubbelwacht`; arm 3's is `jutterswet-tegenwacht` again, with `anchor/jutterswet-borgdeel` as the WF-056 flank | `wf059-attribute-without-datatype`, `wf059-kenmerk-zonder-granulariteit`, `wf059-attribuut-zonder-granulariteit` (one per arm) |
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
| WF-083 | a SINGLE-argument fold source is anchored and never navigates (ch. 4.3; a fold with two or more arguments is the listed value fold, whose operands are ordinary reads governed by WF-053 and — since 2026-09-09 — by WF-096, which refuses a set reading there because a listed operand is an argument without being a source): (a) a non-empty `navigatiePad` is ill-formed; (b) a `navigatieRol` is admissible only on the row-1 wart and only when self-denoting — it names a role (ci, declared name or plural) the FOLD SUBJECT itself fills; away-pointing, and any `navigatieRol` outside the wart, are ill-formed; (c) the fold source's `objecttype` names the fold's anchor — the `elementObjecttype` when `set`, the fold subject otherwise. Closes the silent fall-through where a navigating fold source passed the verifier while ref faults on it, and the anchor gap where one wire field flipped the verdict while no executor moved and two executors disagreed under a clean document | gate 3 | `jutterswet-aanmeldertelling` (the self-denoting count "het aantal vondsten van de aanmelder" through both executors); `jutterswet-tempototaal` (the set anchor and the subject anchor side by side); `jutterswet-sluitsom` (the SCOPE: a listed value fold whose operand navigates, played by ref, the ASP backend and both Drools branches) | `wf083-fold-source-navigates-away`, `wf083-vouwbron-anker-op-operand`, `wf083-vouwbron-navigatierol-buiten-de-wrat`, `wf083-vouwbron-vreemd-objecttype`, `wf083-vouwbron-vreemd-elementtype` |
| WF-084 | a day-precision date literal's lexeme is the strict ISO form `yyyy-mm-dd` the executors parse (ch. 4.1) — fixed field widths (four-two-two digits), single `-` separators, month 1–12, day 1..days-in-month (leap-aware); the Dutch day-first source snippet is ill-formed, whether the bare `31-01-2024` (silently misread as year 31 → `0036-07-16`, a differential-blind wrong answer both executors agreed on) or the `dd. `-prefixed form (both executors fault) — closes that blind spot, the emitter now writing the bare ISO lexeme and keeping the snippet in `bron`; datetime lexemes out of scope | gate 3 | `jutterswet-vondstverjaring` (an ISO date literal through the fence, replayed by gates 5–6) | `wf084-source-snippet` + `wf084-bare-non-iso` |
| WF-085 | the day-kind reference graph is acyclic (ch. 5.4 / ch. 3.7): a day-kind condition may reference other day kinds (a `Predicaat` of soort `DAGSOORT`), but mutually recursive definitions have no fixpoint — a loud refusal where the v0 toolchain resolves a cycle to a silent false (the §9.10 recursion prohibition unenforced for day kinds); diverge-from-v0 (v0 neither checks nor documents it) | gate 3 | `jutterswet-schouwkalender` (an acyclic `hoogschouwdag -> schouwdag` reference, verifier-clean, replayed by gates 5–6 over its ordinary rule) | `wf085-dagsoort-verwijzingscyclus` (two mutually referencing day kinds) |
| WF-086 | a defaults group MUST NOT target a temporal attribute (declared `attribuutGranulariteit`) nor a temporal characteristic (`kenmerkGranulariteit`) — a defaults group decides at one fixpoint and has no timeline coordinate to write (ch. 6); the document-layer fence ahead of the executors (ref faults on both arms — defense-in-depth — and the Drools defaults service skips); temporal defaults targets are unscheduled (PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a) | gate 3 (over the prioritized-defaults node via the shared `attr_temporal` helper and the `kenmerkGranulariteit` table) | `anchor/jutterswet-vondstwaardering` (attribute defaults on a non-temporal target — gate 3 witnesses WF-086 does not over-fire) | `wf086-temporele-defaultstarget` (attribute arm), `wf086-temporele-defaultskenmerk` (characteristic arm) |
| WF-087 | the keuzetype family's nullary rule (ch. 2.4): a `SumType` declaration whose constructors ALL carry a null payload is ill-formed — the nullary keuzetype has one wire form, the `Enumeration` domain, so such a declaration normalises to it (nullary-as-sum is sugar, the enumeration is the kernel; two frontends cannot express one meaning in two shapes). Stated over the declaration; WF-061 carries the use sites. No converse: payload never converts | gate 3 (over `domeinen`, beside WF-060) | every payload-carrying SumType golden — `anchor/jutterswet-beschikkingsom`, `anchor/jutterswet-aanspraaktelling`, the `synth3-*` sum witnesses (gate 3 witnesses WF-087 does not over-fire on a keuzetype with payload) | `wf087-all-nullary-sumtype` + `wf087-unit-sumtype` (the lower boundary: ONE nullary constructor — a unit type is a nullary keuzetype too, and it is what a `List.length > 1` weakening of the rule would let through) |
| WF-088 | an `Enumeration` use site repeats the value list of the domain it names — same values (ci), declaration order (ch. 2.4). The enumeration sibling of WF-061, and what makes "the declaration is the source of truth" mechanically true for this variant: WF-039 reads the LOCAL list where WF-063 reads the DECLARATION, so a drifted copy passed both while ref faulted at evaluation and clingo derived nothing | gate 3 (model layer: attribute, parameter and `FunctionDef` datatypes; expression layer: every node's `type`, beside WF-007's number-form check — the same "every datatype in the document" split, though the two walks recurse differently — see WF-007's OPEN note). One position stays deliberately outside the walk: a rule's TARGET `type`, which is WF-022's lvalue, not part of the expression walk — a drifted copy there was verifier-clean at this rule's landing and measured harmless (drift the `doel` type of `jutterswet-keurstaat` and all three cases still replay to their pinned expectations in ref AND clingo, because both write through the target attribute's DECLARED datatype); since WF-091's rule-target arm the same drift is refused on an ATTRIBUTE target (residual structure mismatch, measured on that same document) and stays clean only on a CHARACTERISTIC target, where the identity family defers by construction (and a value that shares the target type's lie is refused by WF-023's keuzetype clause, which crosses that same declared datatype: `wf023-schrijf-doeltype-leugen`) | `jutterswet-vaarwatertoeslag-catala` (the `Vaarwater` domain repeated identically at FOUR use sites: the attribute datatype, the `Match` scrutinee's type, and the two operands of the equality — an `AttribuutRef` type and a `Literal` type) + `jutterswet-keurstaat` (the `keurklasse` domain repeated at ELEVEN use sites: three attribute datatypes, five `AttribuutRef` types and three `Literal` types) | `wf088-use-site-value-drift` (a value the declaration lacks) + `wf088-use-site-value-order` (the same values, reordered — WF-006 order is normative) + `wf088-literal-type-drift` (an impeccable model layer, a drifted `Literal` type deep in a rule: the expression-layer arm) + `wf088-functiondef-drift` (three violations on one `FunctionDef` — parameter, result and body type — all reached through the walk's recursion into sum-type payloads, the arm no other case exercises) |
| WF-089 | domain identity, read half (ch. 2.4): a use site that TAKES a keuzetype value names the keuzetype — variant AND domain name — of the source it takes it from, whenever either side names one, ci, looked up through a `ListType` element. Two use sites: a **reference expression** takes it from the declaration it READS FROM — attribute (at the endpoint type; the dimension-cell read over it; the fold source, which the expression walk hands off to the fold-source shape rules), parameter, binder (a match arm's payload, a lambda/function parameter, a `Let` value, a filter element) — and an **element-passing aggregation** (`EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM`, the folds that select a contributor rather than compute) takes it from EACH OPERAND, which is itself anchored. **Since 2026-09-04 a third: a COMPOSITE position.** WF-091's derived arms (ch. 4.1) run this same facet engine against a formula instead of a declaration, and the number-follows-the-facet clause hands the domain facet to THIS rule there as well — an additive `Binary`, a `maal`, a `Rounding`, a `Clamp`, a `SOM`, a `TOTAAL` and an `AANTAL` all report under WF-089 when their annotation names a keuzetype domain their derivation does not (the element-passing folds keep the per-operand cross above instead, and the derived cross skips the facet there for exactly that reason). The derived side of that comparison names no domain whenever it is numeric, so the firing arm is the one-sided one. This is the domain extension of the WF-023/027/078 soort cross and the other half of WF-088/WF-061 (which bind a use site to the domain it NAMES). Its write half — every value entering such a declaration names the same keuzetype — is WF-023 (rule target, object-creation member, defaults consequence) and WF-067 (function body); no half suffices alone — the read half without the write half was measured open (fix round 1), the reference positions without the aggregation position were measured open again (fix round 2) — and only all three together make ref's "no arm" fault unreachable on a verifier-clean document whose case input the shared loader admitted. `TemporalRead`, `Invoke` and `Lambda` parameters are already crossed structurally (WF-049/068/070); `Construct` reads no declaration. The declaration side needs no rule: WF-060/065/071 fence a nested `Enumeration` out of every declaration position. Its struct sibling — the same sentence for struct domains, which are NOT keuzetypes — is WF-090 (2026-09-03), sharing this rule's machinery and call sites under its own number | gate 3 (the expression walk's `AttribuutRef`, `DimensieCel`, `ParameterRef` and `BinderRef` arms — the first two crossing a NAVIGATED read at its endpoint attribute, which the parameter and binder arms have no analogue for — plus the fold-source arm and the aggregation arm, and since 2026-09-04 WF-091's derived-arm entry point, where this number carries the domain facet at a composite; the unknown sentinel, an unresolvable reference or an absent `type` defers). Two honesty notes. The **ci** comparison is pinned since fix round 2 by `jutterswet-vaarwaterspelling`, the one golden whose use-site domain differs from the declaration in case alone: making the comparison case-sensitive turns that golden — and only that golden — red. The `ParameterRef` arm's positive witness is `jutterswet-meldplichtspoor`, whose `Fold` takes as its LIST OPERAND a reference to a list parameter of declared-`SumType` elements — an ordinary walked reference on THIS arm, NOT the fold-source arm, which in these rows is attribute-only (it fires for an `AttribuutRef` fold argument and for nothing else; every other fold argument, this list parameter among them, falls through to the ordinary expression walk): the `ListType` look-through this rule states puts that reference INSIDE the arm, use site and declaration naming the same domain. What carries the form is TWO legs of the square, said exactly: all eight of that golden's cases replay exactly in `ref` and in clingo (gates 5 and 6); the golden carries no frontend source at all (no `bron`, so gates 9 and 10 never see it); and the Drools leg declines it at the LOADER, ahead of any case, with its own reason (`EXCLUDE loader: form not carried by the v0 Drools backend: Datatype:SumType (unknown datatype node)` — measured 2026-09-05), so the golden sits in gate 12's grouped auto-refusals and never in its played set. Re-measured 2026-09-05 over the 176 golden and rood documents: making the arm OVER-reject — fire whenever its cross is LIVE (either side names a keuzetype) instead of only when the two disagree — turns that golden, and only that golden, red (`WF-089 FAIL /resolvedRegels/0/waarde/list/type`), so over-rejection there is now pinned. The parity zone adds nothing here and says so with a number: its 30 documents enter this arm 18 times over 9 of them, not one of those references names a keuzetype, and the same over-rejecting mutant is silent across all 30 — the golden is the only witness in any zone. The `DimensieCel` arm still has **no positive witness**: no golden, rood or parity document carries a keuzetype-typed `DimensionCellRead` — the two golden cell reads name no keuzetype domain, and the parity zone carries no cell read at all — so OVER-rejection in that one arm remains unpinned (the same over-rejecting mutant applied there leaves all 176 documents clean). That zero is a REACHED zero, not an unvisited arm: the cell arm is entered, and performs its comparison, at exactly the two golden cell reads — 2 comparisons over the 176 documents, against 46 comparisons over 31 documents at the `ParameterRef` arm (probe, re-measured 2026-09-05) — so the mutant had its two chances there and neither read names a keuzetype | every keuzetype golden — `jutterswet-vaarwatertoeslag-catala` + `jutterswet-keurstaat` (enumeration references), `anchor/jutterswet-beschikkingsom` + `anchor/jutterswet-aanspraaktelling` (sum references: attribute, list-typed attribute, arm/lambda/function binders), `jutterswet-meldplichtspoor` (the `ParameterRef` arm, crossed through the `ListType` element: an ordinary reference to a list parameter of declared-`SumType` elements, standing as a `Fold`'s list operand — not the fold-source arm) — and a measured zero over-rejection across every golden and rood document and, read-only, the parity zone | `wf089-attribuutref-enum` + `wf089-attribuutref-sum` + `wf089-parameterref-enum` + `wf089-binderref-sum` (a `Let` binder) + `wf089-dimensiecel-enum` (verifier-only: the case format carries no cell input) + `wf089-attribuutref-lijst-sum` (the element-domain look-through: verifier-clean before, ref faulted, clingo folded 0 for 35.00 — a wrong answer) + `wf089-attribuutref-geen-domein` (one one-sided arm: typed number over an enumeration attribute — ref wrote the label into the numeric target, clingo faulted, Drools crashed at fire) + `wf089-attribuutref-geen-keuzetype-declaratie` (the mirror one-sided arm, which the other case does not pin: typed enumeration over a numeric attribute) + `wf089-attribuutref-navigatie-enum` (the navigated read, crossed at the endpoint attribute) + `wf089-enum-over-som` + `wf089-som-over-enum` (the variant half: one namespace, so the same name under the other variant is a lie — verifier-clean while both executors faulted in different words) + `wf089-aggregatie-doorgeefluik-enum` + `wf089-minimum-doorgeefluik` (the aggregation position, both fold halves: with one contributor the fold makes no comparison and hands an alien label on) + `wf089-foldbron-enum` (the fold source, which the expression walk never entered) + `wf089-composiet-domein` (fix round 2026-09-04: the COMPOSITE arm, a `PLUS` over two numbers annotated with a keuzetype domain — the one-sided arm at a derived position, which had a firing path since the derivation law landed and no artifact until this case. Mutant-proved: switching the whole derived cross off turns it clean together with the thirteen `wf091-*` derived cases and no others; switching the ADDITIVE arm off turns it clean with the four `wf091-binair-*` and no others; adding `domain` to that arm's skip list turns this case, and only this case of all 297 negatiefs, clean) |
| WF-090 | struct-domain identity (ch. 2.4): a struct-typed use site names the same struct domain (ci, through a `ListType` element) as the declaration it crosses. The struct sibling of WF-089 + WF-023's domain clause, over the same positions and the same verifier machinery under its own rule number (maintainer decision 2026-09-03 — struct domains are not keuzetypes, so the ch. 2.4 family term is unchanged). READ half: `AttribuutRef` (at the endpoint attribute of a navigated read), the `DimensieCel` over one, `ParameterRef`, `BinderRef`, the fold source, the element-passing aggregation against each operand, and — since 2026-09-04, through WF-091's derived arms — a **composite** position, where this number carries the struct-domain facet against the DERIVATION exactly as WF-089 carries the keuzetype one. No artifact, and the reason is measured: the only two composites in the four zones that touch a struct type at all are `EERSTE` folds (`wf090-doorgeefluik`, `wf090-foldbron`), the one position where the derived cross skips the domain facet by design because this rule's own per-operand aggregation arm already crosses it — so the composite arm is a firing path with zero occurrences here, and `wf089-composiet-domein` pins the shared engine under WF-089's number. WRITE half: rule target, object-creation member, defaults consequence (recursing into exception groups). No arm where an existing rule crosses the FULL datatype (WF-049/067/068/070/073/074); the aggregation arm reaches the ELEMENT-PASSING folds only, so a computing fold (`SOM`, `TOTAAL`) over struct operands of two domains is verifier-clean — a WF-038 operand question and no wrong answer (measured: ref faults `plus: expected a number, got {waarde: 120.5, telling: 3}` and the ASP carrier refuses the form) — and the ORDERABILITY flank WF-089 records holds for structs too: `StructType` is equality-only, so a truthful `EERSTE` with two struct contributors is verifier-clean and faults observably (measured: ref `unordered comparison: {waarde: 20, telling: 2} vs {waarde: 10, telling: 1}`, clingo refuses the form) — also WF-038's, not this rule's; the unknown sentinel, an unresolvable source and an absent `type` defer | gate 3 (the six reference/aggregation arms share one entry point with WF-089, the write half rides `wf023_assignment` — each arm falsifiable on its own). Two honesty notes, measured. The **ci** comparison is pinned since fix round 1 by `jutterswet-dossierspelling`, the one golden whose struct use sites differ from the declaration in case alone, at BOTH halves at once: making the comparison case-sensitive in `wf090_struct_identity` and in the struct clamp of `wf023_assignment` turns that golden — and only that golden — red on two messages. Of the six read arms only `AttribuutRef` (50 struct-typed references) and `BinderRef` (9) have a POSITIVE witness, and each is witnessed BROADLY: a mutant that fires on any struct-typed use site at the `AttribuutRef` arm turns TEN documents red (eight goldens + `rood/list-element-overprecision` + `rood/struct-field-overprecision`), the same mutant at the `BinderRef` arm five (`anchor/jutterswet-vondstdossier`, `jutterswet-dossierrecht-catala`, `jutterswet-kamerhuishouden-catala` and the two rood documents) — measured. The `ParameterRef`, `DimensieCel`, fold-source and aggregation arms carry no struct-typed use site anywhere in the corpus, so OVER-rejection in those four is unpinned, and a golden cannot be added today because the ASP carrier refuses the forms ahead of translation (measured: `TRANSLATION FAULT` on the faithful twins) | `anchor/jutterswet-vondstdossier` (the struct anchor and the richest witness of the two witnessed arms: nine struct-typed references and three `StructLiteral`s, all faithful, replayed by gates 5–6) + `jutterswet-dossierspelling` (the ci witness, both halves) + `jutterswet-vangstwaardering` + `jutterswet-waarderingsinvoer` + `jutterswet-dossierrecht-catala` + `jutterswet-kamerhuishouden-catala` + `synth3-nestwaarde` + `synth3-vondststaat` (the further non-over-rejection witnesses) — and a measured zero over-rejection across every golden and rood document: ten of the 171 carry a `StructType` at all, together 59 struct-typed references (`AttribuutRef` 50 + `BinderRef` 9) and 16 `StructLiteral`s. The parity zone is verified read-only too and is clean, but it confirms nothing HERE: measured, none of its 30 documents carries a single `StructType` | `wf090-schrijf-regeldoel` + `wf090-schrijf-objectcreatie` (where the ASP side reported a full green pass on a document ref cannot evaluate) + `wf090-schrijf-defaults` (the three write positions) + `wf090-attribuutref` + `wf090-attribuutref-lijst` (the `ListType` look-through, where clingo folded 0 for 12.50 — a wrong answer) + `wf090-binderref` (a lambda parameter, the same wrong answer) + `wf090-parameterref` + `wf090-dimensiecel` (verifier-only: the case format carries no cell input) + `wf090-foldbron` (the position the expression walk never enters) + `wf090-doorgeefluik` (the fold node's own type against its truthful operand) |
| WF-091 | reference-type identity (ch. 4.1): a use site's serialized `type` is IDENTICAL to the declaration it takes its value from — not compatible, not convertible — up to the ci comparison WF-001 fixes for a declared DOMAIN name, the three UNIT names compared BYTE for byte, facet by facet in one fixed order: **soort → domain → unit → decimals → wholeness → sign → residual structure**, of which the FIRST differing facet is reported — one comparison, one message, never two facets at once; per COMPARISON, not per path, since a walk that crosses one path repeatedly (the aggregation's domain cross, once per operand) can report it more than once. The rule NUMBER follows the facet and the arm: the domain facet is WF-089/WF-090 at every arm and the soort facet is WF-078 at a `BinderRef`, each keeping its wording; every facet without a number is this rule's and names itself in the message — so a further facet costs a message, not a rule number. Two clauses of the order are load-bearing: the soort facet **stands aside** wherever either side names a keuzetype or a struct domain (one namespace — that discrepancy is WF-089's variant/one-sided arms and WF-090's, numbered there), and the residual facet is the **backstop** that makes the refusal total (the domain facet looks THROUGH a `ListType` element, so a list-typed declaration read as its element passes both earlier facets). **Unit identity is structural** — abbreviation, declaring system, canonical unit, factor, denominator recursively, the three NAMES compared BYTE for byte and the factor exactly (copy fidelity is not name resolution, WF-001's domain: a consumer that reads those three names literally to pick its conversion diverges silently on a case-only lie — measured, `ref` 3.75 against a third executor's 3750.00 on `wf091-attribuutref-eenheid-kast`) — never convertible: `kg` and `t` share a canonical unit and a shape, so a lie inside the convertible class is compatible by construction and WF-035/WF-023's unit cross cannot reach it. ARMS: the five REFERENCE use sites WF-089/WF-090 name (`AttributeReference` at the endpoint of a navigated read, the `DimensionCellRead` over one, `ParameterReference`, a binder reference, the fold source), plus a rule TARGET's own `type` — the lvalue is WF-022's and the expression walk never enters it (the omission WF-088's row records as measured harmless for a drifted enumeration copy, both executors writing through the DECLARED datatype; it is not harmless for a unit). Their sixth use site, the element-passing AGGREGATION against each operand, stays outside this rule and keeps the domain facet alone: a reference type is a COPY of one declaration, an aggregation `type` is a type DERIVED over an operand list (`MINIMUM`/`MAXIMUM` take the least operand precision, the conjunction of wholeness, and operand 0's sign and unit; `EERSTE`/`LAATSTE` take operand 0's type in full), so identity against every operand is unsatisfiable the moment two operands differ — as in ch. 4.6's blessed "het minimum van het gewicht en 0". The domain is the one facet a pass-through fold hands on unchanged, which is why WF-089/WF-090 can cross it against EVERY operand and no further facet can. Since 2026-09-04 those further facets are crossed against the DERIVATION instead — one comparison, not one per operand — by this rule's second half. The other two write positions NAME their target and carry no reference type of their own: what enters them is WF-023's and WF-090's assignment cross — compatibility, a different law. **DERIVED ARMS (2026-09-04, ch. 4.1 "Derived-type identity on composite positions"):** the same law, same facet order, same rule-number RULE, against a FORMULA instead of a declaration — a composite has no single declaration opposite it, and the expectation is computed from the operands' own serialized types by RUNTIME-SEMANTICS ch. 2.3 and ch. 2.6. "Same number" means the same number-follows-the-facet clause, not one number: the DOMAIN facet reports under WF-089/WF-090 at a composite exactly as it does at a reference (measured — a `PLUS` and a `Rounding` annotated with a keuzetype domain each report WF-089's one-sided arm, pinned since fix round by `wf089-composiet-domein`), and only the facets without a number of their own are WF-091's here. Twelve positions (eleven until 2026-09-20), each claiming the facets its formula states and no more: additive `Binary` (maxDec, wholeness as conjunction, the LEFT unit, and a soort that COLLAPSES to a number — `Percentage + Percentage` derives `number`, so that is the only annotation the position admits; a date ± duration gives the date operand's type in full), `Binary` maal (the unit only — hand-through or rate cancel — with the same soort collapse), `Rounding` (the base plus its OWN decimal count), `Clamp` and `Aggregation` som/eerste/laatste (a pass-through), `Aggregation` totaal (the pass-through with a rate's denominator stripped wherever the fold INTEGRATES over time — the emitter's own `setLezing` predicate mirrored, so the non-temporal SET reading, an alias of the sum, passes the element type through instead), `Aggregation` minimum/maximum (least precision, conjunction, operand 0's sign and unit), `Aggregation` aantal (whole, zero, unitless) and — since 2026-09-20 — `Aggregation` **aantal gevulde** (the same formula, on its own arm because the engine matches on the KIND: a filled count answers a cardinality whatever unit the folded attribute carries, `RUNTIME-SEMANTICS.md` ch. 3.4c). That twelfth arm closed a real hole rather than tidying a list: measured the day the kind landed, a filled count annotated `kg` and written into an equally-lying `kg` target was verifier-CLEAN while the identical lie under `AANTAL` was WF-091, and `ref` then played it — a unitless 1 landing in a kg-declared attribute, oracle PASS. A derived type that nothing crosses is an annotation that may say anything. A facet a formula does NOT claim is not crossed and takes the residual backstop with it, since the backstop would otherwise report exactly what the formula declined — and because the SIGN is claimed nowhere, that stand-down is total: no derived arm keeps the backstop, so the refusal there is exactly the row's named facets and, unlike the reference half, is not total by construction. The chain anchors only as far as the crossed operands reach: where every operand under a composite is itself crossed, the composite is bound to the declarations underneath it, but a FOURTH-CLASS operand breaks the chain — a `GEDEELD_DOOR` annotated `kg` over two unitless operands is a fabrication nothing crosses, and the law above it consumes it as ground truth up to a `kg` target, verifier-clean (measured on a probe; 26 such operand positions over 19 of the 174 golden+rood documents of the measured 2026-09-04 state — one merge before `jutterswet-tempototaal` made it 175 — 23 of them under a division). The sentinel/empty door is shut (an untyped or sentinel-typed operand is WF-034-ill-formed) but the fabrication door is the stronger escape and stays OPEN: it does not switch the cross off, it feeds it a premise. What stays UNCROSSED is ch. 2.10's fourth class: a division, a power, a percentage-of, a root, an absolute value, the date/time extractors — v0 derives no result unit there — plus a duration node (the annotation IS the source, WF-038), and `IfThenElse`/`Let`, whose whole type WF-069 and WF-076 already cross. That set and gate 13's exception set are TWO INDEPENDENTLY MAINTAINED LISTS, aligned by hand with no tripwire comparing them: they differ, measured, on `AANTAL` and, since 2026-09-20, on `AANTAL_GEVULDE` (both derived here, neither in gate 13, which twists units only — and a cardinality is unitless by construction, so no twist table would reach either), on the duration OPERAND axis gate 13 exempts and this rule has no notion of, on a `Let` gate 13 twists, and on `GEDEELD_DOOR_ABS`, which gate 13's prose omits and its set-complement covers. Outside: `TemporalRead`, `Invoke`, `Lambda` parameters, `StructLiteral` and `FieldAccess` already cross the FULL datatype (WF-049/068/070/073/074), so no node is crossed twice under two identity rules; `Construct` reads no declaration; WF-023's write-side PRECISION gate is a different question and stays OPEN. The unknown sentinel, an absent `type` and an unresolvable declaration defer | gate 3 (one entry point shared with WF-089/WF-090 plus the rule-target registration, and since 2026-09-04 a SECOND entry point beside the expression walk's type prologue for the derived arms — one function, one facet order, one rule number; every arm and every facet mutated and measured). Four honesty notes. (1) At the REFERENCE arms, which claim the whole datatype, the named facets decide the **diagnosis** and the residual facet decides the **refusal**: neutering any named facet degrades its case's message to `residual structure mismatch` while the document stays refused — only neutering the RESIDUAL facet lets a document PASS (`wf091-attribuutref-reststructuur`, measured). At the DERIVED arms that does not hold and cannot: every one of them claims less than the whole datatype and stands the backstop down with the sign, so neutering a named facet lets its case pass outright (measured in fix round: the unit facet turns all seven derived unit cases clean; eight since 2026-09-20, re-measured with the filled-count arm). What the facet order pins is therefore the NUMBER, and that is falsifiable: neutering the soort facet moves `wf078-binderref-type-lie` to WF-091, and neutering the engine's domain facet moves 18 of the 24 `wf089-*`/`wf090-*` cases to WF-091 — gate 3 red in both cases. The six that keep their own number are the ones that do not run through the engine: the three element-passing-aggregation cases, whose arm calls the domain cross DIRECTLY (it is not an arm of this rule), and the three `wf090-schrijf-*` cases, which ride the assignment cross. (2) The **ci** comparison of the DOMAIN names is pinned by exactly two goldens: making the residual comparison case-sensitive turns `jutterswet-dossierspelling` and `jutterswet-vaarwaterspelling` — and only those two — red. The UNIT names are pinned the other way round and by a negatief: folding the three of them ci (in `eenheid_identical` and in the residual comparison form) turns `wf091-attribuutref-eenheid-kast` — and only that case — clean, gate 3 red (measured, fix round 2). (3) POSITIVE-witness breadth per arm, measured with a mutant that fires on every unit-bearing use site at one arm, re-measured 2026-09-05 over the 176 golden+rood documents: rule target **17** documents red, `AttribuutRef` **13**, `BinderRef` 2, fold source **3** (`jutterswet-vondsttelling` + `jutterswet-tempototaal` + `jutterswet-tempovouw`), and — since 2026-09-04 — `ParameterRef` 1 (`jutterswet-aanvoergeld`) and `DimensieCel` 1 (`jutterswet-seizoenvondst`), the two arms that carried **zero** until then. The rule-target and `AttribuutRef` numbers stood at 13 and 11 before that date and grew to 15 and 13 by exactly the two documents added for the new arms: `jutterswet-aanvoergeld` and `jutterswet-seizoenvondst` carry unit-bearing rule targets and attribute references too, so each counts in three lists at once — the breadth of the two established arms did not otherwise change by that step. What moved since is the CORPUS, not the rule: the two temporal goldens `jutterswet-tempototaal` and `jutterswet-tempovouw` each carry a unit-bearing rule target AND a unit-bearing fold source, which is the whole of the 15 → 17 and the 1 → 3 (same mutant, same day). Each of the two 2026-09-04 witnesses is TWO-SIDED, measured: the golden is clean under the unmutated engine, and a `t`-for-`kg` copy of that same arm in that same document (one canonical unit, factor 1000 — the convertible class no other rule reaches) is refused arm-exact (`unit mismatch: parameter reference 'aanvoerdrempel' …`, `unit mismatch: dimension-cell read of 'vondstgewicht' …`). The `DimensieCel` witness is necessarily VERIFIER-ONLY, and that too is measured, not assumed: a cell read is declined by every executor (`ref` `EVALUATION FAULT: expression kind not implemented in this card`, clingo `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`, the Drools backend `EXCLUDE dimensioned attribute input … is not carried by the v0 backend`) and the case format carries no per-cell input variant at all, so no case over that document could pin anything and gates 5–6 would turn red on one that tried. What closes at these two arms is the UNIT facet only; the DOMAIN facets split FOUR ways, and only three of the four are empty. Re-measured 2026-09-05 with an arm-and-class-limited over-rejecting mutant — fire whenever that one arm's cross is LIVE for that one class — over the 176 golden and rood documents: a struct-typed `ParameterReference`, a keuzetype-typed `DimensionCellRead` and a struct-typed `DimensionCellRead` each leave all 176 clean, and all four pairings leave the 30 parity documents clean, so WF-090's note stands unchanged at both arms and WF-089's stands unchanged at its `DimensieCel` half. The fourth pairing does NOT: a keuzetype-typed `ParameterReference` turns exactly one of the 176 red — `jutterswet-meldplichtspoor`, through the `ListType` look-through — which is why WF-089's `ParameterRef` half is the one note that changed, in this same diff, and now carries that witness instead of claiming none. The parity zone carries 12 unit-bearing `ParameterRef` use sites over 6 documents and is verified read-only, but it is not a golden and pins nothing here. Both arms also carry a unit-facet negatief of their own (`wf091-parameterref-eenheid`, `wf091-dimensiecel-eenheid`, the latter since fix round 2, which completes the set: every arm this rule crosses in full has one) — a negatief pins the REFUSAL and a golden the NON-over-rejection, and since 2026-09-04 both halves exist at every arm this rule crosses in full. (4) The DERIVED half's coverage, and what it still leaves open. It performs **228** comparisons over 102 of the 174 golden+rood documents (the measured 2026-09-04 state; `jutterswet-tempototaal` landed one merge later and adds a 175th document with two TOTAAL comparisons — the strip witness in the negatief column is measured against that 175 state; measured 2026-09-04 with a temporary probe in `composite_identity`, which the same walk reproduces per arm: additive `Binary` 114, maal 38, rounding 28, aantal 18, som 12, totaal 7, clamp 3, eerste 3, laatste 3, minimum 1, maximum 1), of which only **14** are unit-bearing, spread over 10 documents. So the UNIT facet has a golden witness at three arms only — additive `Binary` (8 documents), maal (3) and som (1) — and at the nine others (eight until the filled-count arm) it is pinned by a negatief and by nothing else, which pins a REFUSAL and never a non-refusal. The maal arm's CANCEL rule has no corpus witness at all: no `Binary` maal node in any of the four zones carries two unit-bearing operands, so `wf091-maal-eenheid` is its whole evidence. The counter-shape of note 4's own old example still cannot become a golden — the ASP backend refuses a multi-operand `MINIMUM`/`MAXIMUM` ahead of translation, so it can carry no pinned expectations — and that is exactly why the artifact that closes it is `wf091-foldwortel-eenheid`. Three formulas stay OUTSIDE the law with their reason and their price, each measured the same day: the SIGN at ALL TWELVE derived positions without exception (it runs both ways — a derivation mostly under-claims it while a document narrows truthfully, the sharpest case being a clamp with a minimum of 0, which IS non-negative whatever its base says; and it over-claims at the aantal arm, where two anchor goldens widen `NIET_NEGATIEF` to `ONBEPAALD`. 12 documents over the three zones narrow it truthfully and 5 widen it; crossing it costs 5 golden+rood documents on the additive arm alone, 7 at every arm, 27 with every exclusion emptied — the six arms corrected in fix round carry NO golden+rood witness, so turning the sign off there left the verifier's report over all four zones byte-identical and the class is pinned by a probe, not by the corpus); the PRECISION of a maal, whose price needs both numbers because they are different measurements (against ch. 2.3's actual rule — the SUM of the operands' decimals, never whole — 9 decimals and 9 wholeness divergences over the four zones and **11** golden+rood documents red, 3 on decimals and 8 on wholeness; against the PLACEHOLDER the implementation writes where the facets are unclaimed, zero decimals and fractional, merely un-skipping the two facets turns **20** red. 11 is the rule's price, 20 the placeholder's — this corpus carries three producers with three precision policies, the Catala plugin writing the pinned ch. 2.9 scale); and the pro-rata builtin plus the one-sided additive unit pair — of which only ONE ORDER is contested (a unitless LEFT beside a unit-bearing right, where ch. 2.6 gives no unit and both executor-side derivations give the right one; with the unit on the LEFT all four sources agree). The rule claims neither order: its guard is the pair's unit compatibility, and splitting it would pin the uncontested half — which carries zero golden+rood occurrences — while leaving the contested half open anyway. The one node the corpus does carry is in the contested order and annotates the RIGHT unit, against ch. 2.6 (`jutterswet-sleeprest`, `50 min <km>`); it is also a shape WF-035's adoption clause settles independently, the unitless side being a literal, so the genuinely open class is narrower still. Two facets of two arms have no artifact either way: a `Rounding` over a PERCENTAGE base (zero occurrences in four zones — as for the fall-through row, a rounding over a base that is neither), and the opaque PRODUCT of two plain units, whose algebra ch. 2.5 fixes but whose wire spelling it does not | every unit-bearing golden — `jutterswet-vondsttelling` (the fold-source witness), `jutterswet-lichterloon` + `jutterswet-vondstrecht` (the two `BinderRef` witnesses), `jutterswet-meldloon` + `jutterswet-sleepbaan` + `jutterswet-sleeprest` + `jutterswet-stukopbrengst` + `jutterswet-vergunningsduur` + `jutterswet-vervaltermijn` + `jutterswet-vondsttarief` + `ww-herleving-inkomen` + `synth-grensrand` (the `AttribuutRef` arm) + `jutterswet-jutleeftijd` + `jutterswet-getijdenpaar` (further rule-target witnesses), plus `jutterswet-dossierspelling` + `jutterswet-vaarwaterspelling` (the two ci witnesses), plus `jutterswet-aanvoergeld` (the `ParameterRef` arm, two unit-bearing parameters in two unit systems, read in a condition and in a value) + `jutterswet-seizoenvondst` (the `DimensieCel` arm, verifier-only — see honesty note 3) — and a measured zero over-rejection at every facet across every golden and rood document. What that measurement counts, stated exactly, because two earlier counts of it disagreed: **929 comparisons the engine PERFORMED** over the 174 documents of that same dated state (834 golden + 95 rood), **46 of them unit-bearing** — one per arm entry that reaches the facet chain, tallied by a temporary per-arm probe over every `ir.json` in both zones (re-measured 2026-09-04 with the same probe, which reproduces the previous 921/38/71/14 exactly on the 172 documents of the previous count). It excludes 72 further invocations that DEFER before comparing anything (an absent `type`, the v0 unknown sentinel, an unresolvable declaration), and it excludes the element-passing aggregation's domain-only cross (14 more comparisons, none unit-bearing), which is no longer an arm of this rule. It also counts the REFERENCE half only: the derived arms perform a further 228 comparisons over the same two zones, tallied and split per position in honesty note 4. Same count in the parity zone, read-only: 147 performed comparisons over 30 documents, 30 of them unit-bearing, plus 5 aggregation comparisons — that last number re-measured in fix round 2 and stated with the distinction two earlier counts blurred: the aggregation site is ENTERED **6** times there, and **5** of those entries perform a comparison, one deferring before it. In golden+rood the two numbers coincide at 14, which is why only the parity zone ever showed the disagreement | `wf091-attribuutref-eenheid-converteerbaar` (the rood-eerst case: the same canonical unit, factor 1000 — the class WF-035's compatibility cross admits by construction) + `wf091-attribuutref-eenheid-ontbreekt` + `wf091-attribuutref-eenheid-erbij` (the two one-sided unit arms) + `wf091-attribuutref-decimalen` + `wf091-attribuutref-geheel` (the two precision forms WF-007 admits that share a decimal count) + `wf091-attribuutref-teken` (a WIDENED sign restriction, which WF-023's contradiction clause cannot see) + `wf091-attribuutref-soort` + `wf091-attribuutref-reststructuur` (a list-typed declaration read as its element: the hole the facet order makes by design) + `wf091-binderref-eenheid` (the arm where WF-078 keeps the soort) + `wf091-parameterref-eenheid` (the arm whose golden witness, `jutterswet-aanvoergeld`, arrived only on 2026-09-04 — see honesty note 3) + `wf091-foldbron-eenheid` (the fold source, which the expression walk never enters) + `wf091-schrijfdoel-eenheid` (the rule target) + `wf091-attribuutref-eenheid-kast` (the case-only unit lie, which pins the BYTE comparison of the three unit names — see honesty note 2) + `wf091-dimensiecel-eenheid` (the cell read's own type, the last arm without a unit case) — and, for the DERIVED half, fourteen more, one per (position, facet): `wf091-binair-eenheid` (the rood-eerst case, the 550.000 shape: `kg` derived, `t` annotated) + `wf091-foldwortel-eenheid` (honesty note 4's own counter-shape, a `MAXIMUM` over `kg` annotated `t` — 3,75 against 3.750.000, and the negatief exists because a golden cannot) + `wf091-binair-decimalen` (the maxDec rule, under-reported — the direction that walks past the ch. 2.4 assignment gate) + `wf091-binair-geheel` (the conjunction rule, both sides 0 decimals so WF-007's form check passes either way) + `wf091-binair-soort` (a plain sum annotated a percentage) + `wf091-maal-eenheid` (the ch. 2.6 rate CANCEL, the one arm with no corpus witness at all) + `wf091-afgerond-decimalen` (a rounding claiming not to have rounded) + `wf091-afgerond-eenheid` + `wf091-begrensd-eenheid` + `wf091-som-eenheid` (the fold ROOT, where `wf091-foldbron-eenheid` lies at the SOURCE) + `wf091-totaal-eenheid` (the §4.20 denominator strip, which it reaches by carrying `set: false` on both its operands; removing the strip turns exactly this case green, and also turns `jutterswet-tempototaal`'s temporal node red — while making the strip UNCONDITIONAL, the shape before fix round 3, turns that same golden's non-temporal set fold red. The strip is therefore two-sided; the granularity half of the emitter predicate that guards it is not, and says so at ch. 4.1) + `wf091-fold-decimalen` (the least-operand-precision formula: its operands stand higher-precision-first so that taking operand 0's precision instead turns exactly this case green) + `wf091-aantal-decimalen` + `wf091-aantalgevulde-eenheid` (the twelfth arm's own case, and the rood-eerst witness for it: with the arm absent — the binary byte-identical to the one that shipped the kind — it is verifier-PASS, and the arm is the only difference). Mutant-proved per (facet, arm), re-measured in fix round after the sign exclusion became total: neutering the unit facet turns all **8** unit cases clean (7 until 2026-09-20; re-measured that day, the eighth being `wf091-aantalgevulde-eenheid`) (before that round it turned 3 clean and degraded 4 to `residual structure mismatch`, the split the claims-every-facet column then made — no derived arm keeps the backstop now, so the degradation is gone); neutering decimals, wholeness or soort turns 2, 1 and 1 clean; disabling one position turns that position's cases clean and no others; disabling the derived cross entirely turns all fourteen clean — fifteen with `wf089-composiet-domein` — and leaves every reference-arm case red |
| WF-092 | unit wire form (ch. 2.5): a unit spells its CONVERSION PAIR whole — `canoniek` and `factorNaarCanoniek` filled together or null together, on the numerator and, recursively, on the `noemer` of a rate. The BARE form (both null) stays admissible and is not a dead branch: 28 occurrences over the golden corpus (the WF-038 duration parameters, the WF-035 duration operands), and making the executor's bare branch fault turns 9 corpus cases over 3 goldens red. Either HALF is ill-formed — a canonical target with no factor states a conversion it cannot perform, a factor with no canonical target names no conversion at all. It is a SHAPE law on ONE unit, not a cross between two, which is what no neighbouring rule can supply: WF-009 tests a factor it FINDS and a chain it can FOLLOW, so a missing half is nothing to look at; WF-035/WF-023 cross units for CONVERTIBILITY, which reads the canonical names and never the factor, and reads a missing canonical as the ABBREVIATION — so the half-form is compatible with its whole sibling by construction wherever those two coincide, and refused for the WRONG reason wherever they do not (measured on `km`: `WF-023 assignment across incompatible units` plus `WF-035 additive operands have incompatible units`, which is why the factor-half case sits on the SELF-CANONICAL `m`); and WF-091 compares byte for byte, agreeing the moment the half-form is copied consistently across the arms it crosses. Because it is a shape law it holds at every position a unit can occupy — the declared systems (`/eenheidstelsels/*/{eenheden,aliassen}/*`), the model layer (attributes, domains, parameters, `functionDefs` parameters and result), every walked expression `type`, a FOLD SOURCE's own `type` and an inline LAMBDA's parameter datatypes (the two positions the expression walk does not reach: the fold-source branch replaces that walk, and a lambda's parameters are declared where only its body is walked), and a rule TARGET's own `type`, the position the expression walk never enters either — and a document carrying the shape is refused ONCE PER POSITION, which is what makes the position set falsifiable at all. That totality is measured, not asserted: an injection sweep that puts a half-filled unit at each of the goldens' **2161** `Numeriek`/`Percentage` positions in turn and demands a WF-092 citation reports **0 unreached** (it reported 85 before the fold-source and lambda-parameter call sites landed — 27 and 58 respectively). The unit extraction reaches strictly FURTHER into a datatype than WF-007's own form check: it recurses through a `ListType` element, a `StructType` field and a `SumType` payload, three of the doors WF-007's row records as out of its reach — measured, no unit sits behind one of them in any zone today, so that recursion carries no corpus witness and is pinned by the injection sweep alone, which does reach them through the lambda-parameter site. MEASURED DRACHT ZERO at the moment it lands: over the 476 `ir.json` of the four zones (153 golden, 22 rood, 271 negatief, 30 parity) not one half-filled unit exists outside the three cases this rule brings — golden 213 units, 28 bare / 185 whole, 11 of them denominators; rood carries NO unit at all; the parity zone 1510 units, every one whole, no denominator; 34 distinct unit forms over golden+rood+parity. The rule therefore refuses a shape nothing in the corpus writes, and its evidence of non-over-rejection is the corpus itself. What it does NOT say: nothing about a factor whose VALUE is wrong (WF-009 owns positivity, and a wrong-but-positive factor is unreachable by any rule), nothing about the bare form's meaning, and nothing at a use site — the identity cross stays WF-091's | gate 3 (one shape function `check_eenheid_form` with its denominator descent, one datatype extraction, six call sites — since 2026-09-05 that same function also carries WF-034's `stelsel` back-reference arm (ch. 4.6 (r)), one walk serving two rules, and every branch measurement below is of WF-092's OWN branches; every branch and every call site mutated and swept byte for byte over all 476 documents of the four zones, and the position set additionally pinned by the 2161-position injection sweep). Four honesty notes. (1) BRANCH-exactness, measured: neutering the canonical-without-factor branch turns `wf092-canoniek-zonder-factor` AND `wf092-noemer-zonder-factor` clean (they share that branch) and nothing else; neutering the factor-without-canonical branch turns `wf092-factor-zonder-canoniek` clean, alone; removing the denominator descent turns `wf092-noemer-zonder-factor` clean, alone. Every one of the three mutants leaves every OTHER document in the four zones byte-identical — zero over-rejection and zero under-rejection elsewhere. The third mutant takes TWO edits, not one: dropping the recursive call also makes the `rec` flag unused, which the build treats as an error (warning 39), so an edit that removes only the call fails to build and leaves a sweep quietly re-running the previous binary — a green that means nothing. (2) POSITION-exactness, measured the same way and weaker BY DESIGN: no call site can be pinned by a case turning clean, because each case is refused at several positions at once. What each mutant loses is CITATIONS, and the six sites partition the 19 citations exactly: `eenheidstelsels` **2**, model layer **6**, walked expression `type`s **5**, fold sources **3**, rule targets **3** — and the lambda-parameter site **0**. That zero is the honest one: no document in any zone carries a unit on a lambda parameter, so that call site loses no citation when it is removed and its whole evidence is the injection sweep (58 of the 85 positions it opened). A position whose citations vanish without any case turning red is exactly what a per-position negatief could not fix — the shape law is total by construction, so redundancy is the design, not a gap. Three RESIDUAL positions stay outside the totality and are named rather than papered over: a fold source whose `objecttype` or whose `attribuut` is absent, and a Lambda in a position where no lambda is admissible. Each sits behind an arm that has already REFUSED the node — WF-034 twice, WF-069 once — so the document is rejected, but rejected without a WF-092 citation at that unit. Measured over the four zones: the three negatiefs that reach those arms are `wf034-foldbron-zonder-objecttype` and `wf034-foldbron-zonder-attribuut` (8 units each, every one whole) and `wf069-lambda-outside-fold` (no unit at all), so extending the rule into them would add a branch no document in any zone could witness — the honest note is the artifact here, not the code. (3) POSITIVE-witness breadth per position, measured with a probe that fires WF-092 on EVERY unit at one position: declared systems **15** goldens, model layer **20**, walked expression types **19**, rule targets **16**, fold sources **2**, lambda parameters **0**, out of the 22 unit-bearing goldens; rood scores **0** at all six, because it carries no unit anywhere, so the red zone witnesses nothing here and says so. (4) The EXECUTOR asymmetry the rule answers to, measured on the two plain-unit cases against the verifier that did not yet carry it (it reported `verifier: PASS` on all three): on the canonical-without-factor half, `ref` and the ASP backend both read the missing factor as 1, compute `2,5 + 300 = 302,5` where the restatement should have given `2,8`, and report a DECIMALS complaint about a number whose unit was already wrong, while the third executor names the missing factor; on the factor-without-canonical half nothing faulted anywhere and the three executors returned TWO answers — 2800, 2800 and 302.5 — a wrong value from a document every rule admitted. Both halves are now answered in the executors, so the second line of defence is not a dead letter on either: `ref`'s factor question faults by name on BOTH (measured on the same case files, `EVALUATION FAULT: unit m carries conversion factor 1 but names no canonical unit to convert into (WF-092)` where it used to replay a clean 2800), and the ASP backend inherits both refusals unchanged through the same function, as `TRANSLATION FAULT` (its own translator carries no factor reading — its unit fence compares whole unit records). The third executor still computes 302.5 on the factor-without-canonical half: there the missing canonical makes its convertibility predicate answer "not convertible", which its ungated predicate callers read as "no restatement needed" rather than as a refusal. That is a backend question on a shape no corpus document carries and this rule now refuses before any executor sees it | every unit-bearing golden — **22** of the 153, each a witness that the rule does not over-reject, at the six positions with the breadth honesty note 3 measures (15 / 20 / 19 / 16 / 2 / 0). Named for the two shapes the negatiefs copy: `jutterswet-sleepbaan` (the factor≠1 conversion the two plain-unit cases break, `km → m ×1000`) and `jutterswet-tempototaal` (the only golden in the corpus carrying a rate unit at all, hence the only possible base for the denominator case). The three further factor≠1 goldens `jutterswet-berging` (`t → kg ×1000`), `jutterswet-sleeprest` (`km → m ×1000`) and `jutterswet-vergunningsduur` (`jaar → maand ×12`) witness the executor half: they are the corpus's whole stock of genuine conversions, and gates 5 and 6 replay all 403 cases unchanged with the executor fail-fast in place. The parity zone, read-only, carries 1510 whole units over 30 documents and pins nothing here | `wf092-canoniek-zonder-factor` (the rood-eerst case: `km` naming canonical `m` with `factorNaarCanoniek: null`, verifier-clean before the rule and carrying the measured three-executor split of honesty note 4) + `wf092-factor-zonder-canoniek` (the mirror half on the SELF-CANONICAL `m`, the one stand in which no neighbouring rule masks it — the sharper case, since before the rule NOTHING faulted and the three executors disagreed in silence; two of the three now refuse it by name, the third is honesty note 4's open backend question) + `wf092-noemer-zonder-factor` (the denominator, the recursion's whole evidence: without it the descent is unfalsifiable, since both plain-unit cases are refused at identical positions with and without it. Necessarily VERIFIER-ONLY and measured so: its base golden is one of the three gate 5 lists as not replayed, and it is the only golden with a denominator, so no case over it could pin an executor value) |
| WF-093 | the row-1 wart is a COUNT source (ch. 4.3, maintainer decision 2026-09-06): a SINGLE-argument instance fold whose source carries the unknown sentinel on its own `type` MUST be of kind `AANTAL` — under the sentinel the source yields the linked instances, which only `AANTAL` reads, and `set` does not enter the rule (both wire spellings are refused). The rule keys on the SENTINEL, never on what the `attribuut` slot spells: a ROLE in the bare spelling, an element ATTRIBUTE in the set spelling, a CHARACTERISTIC on the frontend's own route — three measured guises of one wire shape. A fold of two or more arguments is the listed value fold (§5.8.3) and is out of scope, the same scope WF-083's family carries. Closes the flank where four kinds stayed verifier-clean while `ref` faulted `value aggregation over a bare role source`, the ASP backend refused the expression kind on those four witnesses (its set arms fence on `navigatieRol` and read `type` as a wildcard, so a sentinel source WITHOUT that field translates and answers 19.00/12.00 with Drools — measured 2026-09-06, a sub-class this rule covers on purpose, since `ref` refuses the whole sentinel class and a clean version of it would be a live divergence), and the Drools backend ANSWERED (19/12/7). Boundary counted 2026-09-06: of 58 wart fold sources over the four zones, the 54 predating the rule are all `AANTAL` and none moves | gate 3 | `jutterswet-vondsttelling` (the bare wart under `AANTAL` beside a real set-source `SOM` in one document — the pair this rule separates); `jutterswet-aanmeldertelling` (the self-denoting count) | `wf093-wratvouw-kale-som`, `wf093-wratvouw-kale-eerste`, `wf093-wratvouw-set-som`, `wf093-wratvouw-set-maximum`, and since 2026-09-20 `wf093-aantalgevulde-wrat` — the filled count arriving in the value-folding family put it under this rule without a letter of the rule moving, which the witness measures: its diagnosis moves from WF-034 to WF-093 on that widening alone |
| WF-094 | a keyed declaration's SELF-NAME is its map key, verbatim (ch. 2.1, maintainer decision 2026-09-06): the five map-shaped declaration tables — `objecttypen`, `parameters`, `eenheidstelsels`, `feittypen`, `dimensies` — repeat the declared name inside each value, and that `naam` MUST spell the key byte for byte, case included. Not a reference and not a matching site: it is the v0 denormalisation of the ONE name a v1 declaration carries on its node, so ch. 2.1's carriage rule governs it, as at the side-table keys of ch. 4.6 (p). Five arms, one per table, five readerships: `ref` reads no self-name at all, the verifier only through WF-057's text-safety walk (three of five, never against the key), the ASP backend only to name a unit system inside a fault message (no `expectedFault` pins it), and the bean-model executor at **105** sites for an object type, **32** for a parameter, **3** for a fact type, **1** for a dimension and **0** for a unit system (the enumeration order of the sites, which is not the order the five tables are listed in above), having loaded the self-name AS the declaration's name. Measured 2026-09-06, one self-name recapitalised at a time over every golden: 11 of 34 parameter positions leave verifier, `ref` and clingo passing while the bean-model executor answers WRONG VALUES (322.50 → 310.00) over seven documents; under a ci-different name 4 of 205 object-type positions move verdict as well (3 drop-outs from gate 12's played set — silent when this was measured, named by the expected-channel floor that landed the same day — and 1 crash). **Clause (d)** (maintainer decision 2026-09-09; the label is that decision's numbering, and it is this rule's SECOND clause — the first asks whether the name written a second time AGREES, this one whether it is written at all) closes the EMPTY half on the same five tables: a self-name that is `null` rather than disagreeing is ill-formed too. That flank was schema-VALID (the field is typed `["string", "null"]`) and passed the verifier, `ref` and clingo while the bean-model executor DIED on it — `NullPointerException` in the identifier contract — at exactly the two tables, `parameters` and `dimensies`, whose loader took the self-name without falling back to the map key; measured 2026-09-10, one self-name nulled at a time: the parameter arm PLAYED → ERROR, the object-type, unit-system and fact-type arms PLAYED unchanged, `ref` and clingo unmoved on all four, and the dimension arm unplayable (both goldens with a `dimensies` table carry no case file). Two of the five arms therefore rest on measured harm and three on the CONSTRUCT, said so per arm in the witnesses' notes. That loader gained the fall-back on all five tables on 2026-09-10 (a pre-rule document loads instead of dying; pinned additively in the backend's own suite), which removes the loud reader and is no substitute for the wire rule. What the rule does NOT close is the `eenheden`/`aliassen` maps inside a unit system, where the key is not the value's own name. Cost: none — all 1919 entries the five tables held before the first clause (1935 after its witnesses, 2000 over 572 documents before clause (d), 2016 over 577 after) already spell their key byte-exactly and not one is null; over all 532 documents nothing but the first five witnesses moved, and over all 577 nothing but the five null witnesses | gate 3 | every golden satisfies both clauses; `jutterswet-penningdrempel` (the parameter position whose recapitalisation the third executor answers wrong, and whose nulling killed it), `anchor/jutterswet-maatschap`, `jutterswet-berging`, `jutterswet-vondsttelling`, `jutterswet-seizoenvondst` (the five sources both sets of witnesses are broken from) | `wf094-zelfnaam-objecttype`, `wf094-zelfnaam-parameter`, `wf094-zelfnaam-eenheidstelsel`, `wf094-zelfnaam-feittype`, `wf094-zelfnaam-dimensie` (one per arm; each is a ONE-CHARACTER recapitalisation, so a ci relapse frees all five and a dropped arm frees exactly one) + `wf094-zelfnaam-null-objecttype`, `wf094-zelfnaam-null-parameter`, `wf094-zelfnaam-null-eenheidstelsel`, `wf094-zelfnaam-null-feittype`, `wf094-zelfnaam-null-dimensie` (clause (d), one per arm, each a single field set to null; the two clauses are separate verifier functions, so each of the TEN arms is falsifiable on its own witness — a dropped null-arm frees its own document and not its non-null sister) |
| WF-095 | a datum-tijd literal's lexeme is the canonical ISO `yyyy-mm-ddThh:mm:ss[.fff]`, matched whole (ch. 4.1) — fixed field widths, `T` never a space, the seconds field always written, no zone suffix and no offset, a fraction that is optional but exactly three digits, the date half calendar-valid. **Narrowed 2026-09-22 (card `p5-datumtijd-lexeem`):** until then the rule also admitted the day-first source snippet `[dd. ]d-m-yyyy hh:mm:ss[.SSS]`, because the repository's own emitter wrote it into `waarde` AND `bron` (measured 2026-09-06); the emitter now writes ISO into `waarde` and keeps the snippet in `bron`, as WF-084's date branch does, and the snippet is refused on the wire. The narrowing moved no document — the snippet had no carrier in any zone, the five negatiefs of the time each carrying a non-snippet defect — and the readers kept reading the snippet behind the fence until 2026-09-23, when they narrowed with the wire and refuse it — `ref` and the ASP backend quoting the lexeme, the Drools branches naming it at a comparison and elsewhere refusing the rule under the position's own reason, never answering (card `datumtijd-lezers-versmalling`). Everything else is refused — precisely the forms every reader already refused, and the wire refused none of them: `rommel 2024-01-31 12:00:00Z x` was schema-valid and verifier-clean (measured 2026-09-06). It is the wire half of a three-reader fence closed on the same day: the shared interpreter/ASP case reader read `.1` as ONE millisecond and `.100000` as a hundred THOUSAND where the Drools case reader read a hundred and the literal reader refused, and gate 4's schema pattern was the ONLY reason that never showed. Boundary counted 2026-09-06 over the four zones (539 documents): the only WELL-FORMED `DatumTijd` literals anywhere are this rule's own golden's two, beside the five its negatiefs carry — the rule finds no document predating it, so nothing of the corpus moves. The 30 `dateTime` values on the CASE wire (25 public, 5 in the parity zone) were all already canonical, so the readers' own fences move none of them either. **Scope: the PRESENT lexeme.** `waarde: null` is the empty half of the same field and a different question (the (n)/(o) precedent of ch. 4.6), left open with its shape stated rather than for want of a witness: the schema types `waarde` as `["string", "null"]`, so a null LEXEME is schema-valid where a MISSING `waarde` is refused by gate 2, and this rule's arm reads the present lexeme only. Measured 2026-09-06 on a one-edit twist of this rule's own golden: `verifier: PASS`, while the executors split — `ref` and the ASP backend both fault BY NAME (`missing Literal.waarde`) and the bean-model executor DEREFERENCES it (`NullPointerException: Cannot invoke "java.lang.CharSequence.length()" because "this.text" is null` — the shared datum-tijd reader matches its patterns against the handed value with no null guard). Two named refusals and one dereference under a document the wire admits: the same three-reader shape this rule closes for the non-null half | gate 3 | `jutterswet-meldstip` (a canonical-lexeme literal ordered against a `DatumTijd` attribute at a sub-day threshold, replayed by gates 5, 6 and 12 — the first `DatumTijd` coverage of the differential); `jutterswet-sluitstip` (since 2026-09-22: the first `bron.rgs` golden with a `DatumTijd` literal — three source spellings of one instant emitted as one ISO lexeme, in a write and in both orderings, re-emitted by gate 10 and replayed by gates 5, 6 and 12) | `wf095-zone-suffix`, `wf095-spatie-scheider`, `wf095-fractiebreedte`, `wf095-ontbrekende-seconden`, `wf095-omringende-tekst`, `wf095-bronsnippet` (the snippet the rule admitted until 2026-09-22 — verifier-clean under the two-spelling arm, so it is the witness that fails if the snippet is ever readmitted) |
| WF-096 | the set reading is the VALUE fold SOURCE's field (ch. 4.3, maintainer decisions 2026-09-09 and — the kind half — 2026-09-19): an `AttributeReference` with `set: true` is admissible ONLY as the source of a SINGLE-argument VALUE fold — the one node that consumes a collection is the value fold that owns the source. At every other position the document is ill-formed: as a whole rule value, as an operand of a composite node, inside a condition, as an operand of the listed value fold (§5.8.3), whose operands are ordinary reads with no source, and as the source of a COUNT, which folds the contributors themselves and reads no value per contributor (ch. 3.4a states it — a count folds instances, not values, so a leeg element attribute still counts — and ch. 3.4b restates it for the union as ELEMENTS; ch. 1.2e adds only the neighbouring clause that a count is never empty; the normative RegelSpraak specification §5.8.1 defines the count over INSTANCES). The admissible position is WF-093's pair MINUS `AANTAL` (a single argument under one of the SEVEN value folds — six until 2026-09-20, when the filled count joined them: it reads a value per contributor and tests it for presence, so the exemption widened by the narrowing's own question rather than in spite of it, and on that kind a set source is REQUIRED by WF-037 rather than merely admitted), one kind narrower than the sentinel fence beside it — deliberately, because v0 spells a count source with the SENTINEL, which stays admissible, while the SET spelling of it left the wire on 2026-09-19 after `ref` was measured counting the FILLED VALUES where the Drools backend counted the MEMBERS (2 against 3 as soon as one member's attribute is leeg) and the ASP backend refused the kind, with no producer route to any of the three: the frontend refuses the sentence citing this rule. Both executors' arms stay as second lines of defence, unreachable rather than removed, and the value count got a normatively defined operation of its own instead of riding a count's node — `AANTAL_GEVULDE`, ch. 4.3 and `RUNTIME-SEMANTICS.md` ch. 3.4c, landed 2026-09-20; the average stays hand-spelled as a division by it (maintainer decision, with the divide-by-zero on a wholly empty source left deliberately with the rule author). Closes ch. 4.3 flank (5), and closes it as a WIRE question rather than an executor question: the shape was verifier-clean while NO executor carried it — `ref` faults `unsupported AttribuutRef shape (set/sentinel outside a fold)`, the ASP backend refuses the expression kind, Drools drops the rule as a transpiler skip — three refusals under one clean document, which no differential gate can catch because there is no answer to differ about. Measured identically at BOTH read positions, which is why it is one rule; and both are LIVE producer routes, re-emitted from source on 2026-09-09 with `diagnostics: []` and committed verbatim as the first two negatiefs, so the rule cuts a producer route and the frontend follow-up is spec-first on its own card. Boundary counted 2026-09-09 over the four zones (560 documents): exactly ONE document predating the rule carries the shape — the already-rejected `wf034-sentinel-geleste-setlezing`, whose verdict does not move and whose report gains one co-firing line — while the 16 LEGITIMATE set fold sources across 14 goldens are untouched. **Scope: the READ.** `set: true` on a rule TARGET is a write position, is not reached by this clause and stays verifier-clean, left open on evidence: zero documents carry it and all three executors ignore the field and write the scalar (measured), which is three-way agreement on an inert annotation rather than this rule's class | gate 3 | `jutterswet-vondsttelling` (its `SOM` over "alle vondsten" is a legitimate set fold source — the position the rule leaves alone, replayed by gates 5, 6 and 12) | `wf096-setlezing-regelwaarde`, `wf096-setlezing-operand`, `wf096-setlezing-geleste-operand`, and for the 2026-09-19 kind half `wf096-aantal-setbron` (the real-typed element attribute over a `bronRol` range — the form that was measured), `wf096-aantal-setbron-wrat` (the row-1 wart's set spelling) and `wf096-aantal-setbron-bronpad` (the multi-step range of ch. 3.4b, which the narrowing makes inexpressible as an `AANTAL` FOLD — the claim is scoped to this node, not to the IR) |
| WF-097 | a `gedurende` clip member carries NEITHER an owner NOR a whole-period qualifier (ch. 4.5, maintainer decision 2026-09-09; widened to all three clips 2026-09-10): a CharacteristicCheck standing as the condition of a clip `filter` — directly or as a member of the flat ALLE composite — is refused an `eigenaar` and refused a `periode`, the NEGATED qualifier included. The rule names its three clips as a CLOSED list: the temporal total (`TOTAAL` over an own temporal attribute, WF-055, ch. 3.6), the pro-rata fold (`TIJDSEVENREDIG_DEEL`, WF-056, ch. 3.7) and the day count (`AANTAL_DAGEN_IN`, WF-058, ch. 3.8); the FOURTH reading of the same field, the die/dat instance subselection, is NOT a clip and is deliberately not fenced — there an owner and a qualifier are ordinary condition fields. Two arms, one reason: a clip reads THE SUBJECT's own characteristic periods over the WHOLE span it clips, so an owner names a subject no reader consults and a whole-period demand re-states the span the clip already ranges over. Every reader matched both fields away with `_` and none said so. Measured on the pre-rule binaries, one field twisted at a time: 3 twists at the total (`jutterswet-tegenwacht`) verifier-clean and unchanged in 36 of 36 replays, plus 6 twists at the other two clips (`anchor/jutterswet-borgdeel`, `anchor/jutterswet-dagentelling`) verifier-clean and unchanged in 60 of 60 — 96 replays, 96 unchanged answers, `negated: true` indistinguishable from absent throughout, which is why both polarities are refused. Each of the three artifacts implements the fence ONCE, in a single shared helper reached from TWO OR THREE call sites depending on how that artifact already discriminates the clips (the verifier needs two — its two folds are one kind-keyed branch — while ref and the ASP backend carry three): what the rule requires is the SHARED helper, not a call count, and it is required rather than noted because the rule first landed on the total alone and the other two clips answered silently for a day. The two executors fault with the same words but NOT at the same time (ref lazy, ASP eager): behind a false rule condition ref replays clean while the ASP backend refuses at translation — measured, and measured to be INHERITED from WF-055, which splits identically; bounded by the verifier rejecting any such document, so no gated zone can hold one. **Scope is the clip member.** The SCALAR check outside a clip keeps its `eigenaar` (name resolution, WF-034 (q)) and keeps its qualifier (the frontend produces that form from source — a producer-cutting decision with its own card); the qualifier on a **Comparison** is implemented, carried by 5 corpus nodes before the rule and 5 after, and untouched, a widened WF-043 being over-rejection. WF-043's own characteristic arm cannot reach inside a clip: it fires only on a NON-time-dependent characteristic and WF-055 already requires a time-dependent one there — that unreachability is what left the position unguarded. Cost: none — counted over the four zones before the rule (534 documents), 39 clip filters across 25 documents hold 37 characteristic checks, 0 with an owner and 0 with a qualifier; corpus-wide 193 characteristic checks, 3 with an owner (all outside a clip, all in the negatief zone), 0 with a qualifier | gate 3 | `jutterswet-tegenwacht` (total), `anchor/jutterswet-borgdeel` (pro rata), `anchor/jutterswet-dagentelling` (day count) — the three sources the witnesses are broken from, each staying clean and replayed by gates 5, 6 and 12 | `wf097-clip-kenmerk-eigenaar` (composite member), `wf097-clip-kenmerk-periode` and `wf097-clip-kenmerk-periode-ontkend` (plain member, both polarities), `wf097-prorata-kenmerk-eigenaar`, `wf097-prorata-kenmerk-periode`, `wf097-dagentelling-kenmerk-eigenaar`, `wf097-dagentelling-kenmerk-periode` — one per clip per arm, so a dropped ARM frees its three or four and a dropped CLIP SITE frees exactly that clip's |
| WF-098 | the context-value name is a CLOSED SET of three, spelled byte for byte (ch. 4.1, maintainer decision 2026-09-10): a `Constante`'s `naam` is `Rekendatum`, `Rekenjaar` or `de dag`, and nothing else — not a fourth name and not a recapitalisation of one of the three. It is an ENUMERATION and not a resolution, which is why it carries its own number rather than an arm of WF-034: no table in the document declares a context value, so what has to be written is the set itself, and writing it decides what v1 admits at this node. The set holds THREE because the wire does: measured 2026-09-10 over the four zones plus the parity zone (578 documents), 14 `Constante` nodes over 6 documents in exactly two spellings — `de dag` (11, in 3 documents) and `Rekendatum` (3, in 3 documents) — while the ch. 4.1 row named `Rekendatum` and `Rekenjaar` only, so the commonest name on the wire was one the row did not name. Mirror-then-diverge settles that in the wire's favour; what `de dag` IS stays ch. 3.7's bound day of a day-kind definition, spelled as a context value because v0 has no node kind for it, and whether v1 gives it one is left open. `Rekenjaar` stays in the set with no carrier at all: the reference interpreter faults on it by name, which is the honest answer for a construct ch. 8.2 defines and no executor implements. **Byte-exact is the decision the rule carries.** The consumers disagree about case, and one disagrees with itself: `ref` and the ASP backend compare a NORMALISED name, the Drools constant reader compares with `equals` and its day-kind reader with `equalsIgnoreCase`. Measured 2026-09-10, `Rekendatum` -> `rekendatum` on `anchor/jutterswet-penningvintage`: verifier PASS, `ref` PASS (2 expectations), ASP PASS (2) — three consumers accepting what the fourth would not recognise. No differential can pin it, and that is structural rather than a gap: of the three `Rekendatum` carriers the anchor golden is excluded by the Drools loader for an unrelated reason and the other two are red documents, so the only carrier Drools could play would be one built for the purpose, whose whole content is the defect. The rule closes the WIRE instead — a document carrying `rekendatum` never reaches an executor, which makes the normalisation inside `ref` and the ASP backend unreachable on any conforming document and leaves all three executors untouched by this card. Residue, stated rather than hidden: a HAND-BUILT document that skips the verifier still splits them. **Observability before the rule: none in either direction** — `de dag` -> `de dagX` left verifier, `ref` and the ASP backend on exactly the untwisted answers (PASS 7 and 3 in both), because no executor evaluates a day-kind definition today; `Rekendatum` -> `RekendatumX` is loud in both OCaml executors but with each one's FALL-THROUGH message (`expression kind not implemented` / `not translated`), which names no defect. **Scope: the PRESENT name.** A `naam` of `null` is schema-valid (the field is typed `["string", "null"]`), carried by no document, and splits nothing: measured 2026-09-10, verifier PASS and both OCaml executors falling through to their generic refusal with no dereference anywhere — unlike WF-095's null lexeme — so it is recorded in ch. 4.1 with its measurement instead of folded in. **The one producer route that emits a fourth name was already ill-formed**: the RegelSpraak frontend builds a `Constante` named `hij` for the animate pronoun and normally resolves it away (of the 38 goldens with a committed source, the 14 whose source writes `hij` emit no `Constante` at all), and where it does leak — measured, `indien hij gelijk is aan hij`, `diagnostics: []` — it leaks carrying the UNKNOWN-TYPE SENTINEL, which WF-034's type prologue refuses at the same node. This rule adds a second, name-level line there and cuts no conforming producer route. Cost: none — all 14 context values predating the rule spell an admissible name verbatim; the before/after sweep over all 580 documents moves exactly the two witnesses' verdicts and not one report line elsewhere, and all 14 positions were twisted one at a time to show the check reaches each (flat and nested composites, a function argument, a temporal read's date index, a `Let` body) | gate 3 | `jutterswet-schouwrooster` (the four `de dag` positions of two day-kind definitions) and `anchor/jutterswet-penningvintage` (the `Rekendatum` index of a vintage read) — the two sources the witnesses are broken from, each staying clean and replayed by gates 5 and 6 | `wf098-onbekende-contextwaarde-naam` (a name outside the set), `wf098-contextwaarde-naam-kapitalisatie` (a recapitalisation of one inside it) — the two fall independently: dropping the whole arm frees both, relaxing only the byte-exactness frees the second alone |
| WF-099 | a clamp bound's unit is compatible with its BASE's (ch. 4.6, maintainer decision 2026-09-10): the `minimum` and the `maximum` of a `Clamp` are each crossed against the base — both unitless, or both present and convertible, with a unitless numeric literal adopting the base's unit first — through the SAME predicate WF-035 uses on an additive pair and WF-036 on a comparison, so the rule adds a position rather than a fourth notion of compatibility. Crossed against the BASE and not against the sibling bound because the base is what a bound is restated INTO, which three clauses of `RUNTIME-SEMANTICS.md` carry between them: ch. 2.7 is the RETURN law alone (the clamp yields base, minimum or maximum, minimum applied first, comparisons exact) and carries no unit sentence at all; ch. 2.10 gives a clamp, as a composite position, the effective type computed from its BASIS; and ch. 2.6 is where the restating lives — an operand restated into the other's unit with exact factors, a comparison across units restating BOTH sides by their exact canonical factors. Every comparison a clamp makes being base-against-bound, min-against-max needs no clause of its own — a clamp never compares them; a bound carrying no type defers, and a non-numeric bound stays WF-035's clamp arm rather than being reported twice. **It is the position the other three left uncovered.** WF-035 crosses an ADDITIVE node's operands and a clamp is not one — its own clamp arm reaches the bounds for numeric-ness and for presence, never for units; WF-036 crosses a COMPARISON; WF-023 crosses the written value against its target, which on the witness is `EUR` against `EUR` and agrees; and WF-091 cannot reach a bound at all, the bound being a `Literal` and therefore one of the three ch. 2.10 positions where the annotation IS the source, with no declaration to cross. Measured 2026-09-10 on a clamp over a `EUR` base with both bounds annotated `kg`: verifier PASS before the rule, `ref` `EVALUATION FAULT: incompatible units: EUR vs kg`, the ASP backend `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated`, and the Drools backend `ERROR Eenheidsconflict: onverenigbare eenheden bij een §6.1.4 begrenzing-ondergrens: 'kg' (canoniek kg) herschaalt niet naar 'EUR' (canoniek EUR) — er bestaat geen omrekenfactor` — the verifier admitting what all three executors refuse, which is the direction that matters. Those two Drools branches are not one fence and are not elided into one: the exec-model branch carries a clamp-SPECIFIC gate per bound (the `§6.1.4 begrenzing-ondergrens` and `-bovengrens` restatings the message names) while the DRL-text branch catches the same defect through its generic §4 value-restating, both arriving at the one shared rescalability demand. **Convertible and not identical, measured rather than read off the messages**, which disagree: `ref` says `incompatible` and tests convertibility, the ASP backend says `non-identical` but tests identity of units already normalised per declared system (so within one system it IS convertibility), and the Drools backend restates through a rescalability demand pinning `kg`/`t` rescalable and `jr`/`kg` a fault. On the same document with both bounds moved to `t` (system `Gewicht`, factor 1000 to canonical `kg`) over a `kg` base, `ref` and the ASP backend both restate and both answer 10 t = 10000 kg — identical values, no fault on either leg — so identity would have refused a form all three play. The choice also CLOSES two measured ref/ASP divergences, both refused here because canonical names differ: a bound whose `canoniek` contradicts the system it names (`t` declared canonical `ton` over `kg`), and a bound relabelled into the base's system (`kg` relabelled into `Munt` over `EUR`, where the ASP backend answers the untwisted values and silently treats kilograms as euros). What it does NOT close is not the shape first suspected: a bound naming an UNDECLARED system is already refused by WF-034 (measured 2026-09-10, once per bound: `unit 't' declares unit system 'Massa', which no declaration carries: the unit resolves against no conversion table`), so no conforming document carries it. **Closed (2026-09-19) by the EXECUTOR, not by the rule:** the CROSS-SYSTEM bound whose system is fully DECLARED — `Massa` declared beside `Gewicht` (`kg` factor 1, `t` factor 1000, both canonical `kg`), a `t` bound over a `kg` base in `Gewicht`: convertible by canonical name, so verifier and rule admit it, while on 2026-09-10 `ref` restated to 10000 and Drools answered 10000 on both branches and the ASP backend refused to translate, its per-system normalisation finding no identity ACROSS systems. The rule's own choice settled it — convertibility is a canonical-NAME law, so the backend was enforcing a fence the norm does not carry, and the backend conformed rather than the spec narrowing: it now gathers its conversion table by canonical name across declared systems and admits an exact-factor-1 pair at the identity fence, with the integer encoding untouched (a cross-system ratio that is not a positive integer into the finest unit stays the same loud refusal a same-system one gets). Re-measured 2026-09-19 on all four positions: the WF-099 bound, the WF-035 join and the WF-023 write move to the pinned values, and so does the factorless `kg`@Massa / `kg`@Gewicht pair; the WF-036 comparison moves only for the factorless pair, because the normalisation pass rewrites rule VALUES and not conditions — and the identical SAME-system document is refused by the same message before and after, so what remains is the pass's scope and not a system fence. Cost: zero elsewhere — the Drools sweep over both branches moves exactly the new golden's line (exec 153 → 154, DRL-text 126 → 127) and the corpus replay counts grow only by it. Golden `jutterswet-stelselweging` is the cross-system carrier: two declared systems sharing canonical `kg`, crossed at all four positions with and without a factor, `lastgewicht` crossing twice in one rule (a `Massa` bound over a `Gewicht` base written to a `Massa` target), hand-computed absolutes replayed by ref ≡ clingo ≡ Drools on both branches; gate 12's `EXPECTED_PLAYED` 153 → 154, gate 13's twist set 108 → 126 annotations over 22 → 23 goldens with the floor unchanged. `backlog/done/20260910-kruis-stelsel-eenheid-divergentie.md`. Cost of the original 2026-09-10 landing: none — the before/after sweep over the verifier-gated zones (golden 166, negatief 359, rood 26 — 551 documents) moves exactly the witness's verdict and not one other report line; the one-sided-pair posture inherited from the sibling predicate (ch. 4.1's open class) is carried by no clamp in those zones and by two of the parity zone's 30 documents, which no gate puts through the verifier and which were already non-conforming for unrelated reasons | gate 3 | `jutterswet-grensbedrag` — the corpus's only clamp carrier, unitless on base and on both bounds, so it pins the admitted both-unitless reading and is replayed by gates 5, 6 and 12. **Landed (2026-09-11):** `jutterswet-vangstklem` carries the unit-bearing clamp, so the convertible half rests on a gated document too and not only on the executors and the witness's falsifiable variant: a `1 t` minimum and a fractional `2,50 t` maximum over a `kg` base in one declared system (factor 1000), plus a mixed clamp whose `2500 kg` maximum is the same weight the `2,50 t` arm reaches by restating. ref ≡ clingo ≡ Drools on hand-computed absolutes, and the restating is measured rather than assumed — relabelling a bound to `kg` moves the answer on every leg. Gate 12's `EXPECTED_PLAYED` equality 122 → 123, not the 119 → 120 this row predicted: the channel shifted three times under the card while it sat in the backlog — `jutterswet-keurhouder` took the 119 → 120 slot, then `jutterswet-ketenband` and `jutterswet-kavelgrens`. Gate 13 moved nothing mechanical, as predicted: the twist set grew 74 → 83 annotations over 16 goldens instead of 15 with the floor unchanged; only the script's prose note about the DRL branch's `Begrensd` value arm went stale, and it was corrected in the same commit | `wf099-begrensd-grens-eenheid` (both bounds foreign) — the two bound positions fail independently, so dropping either arm frees exactly its own report, and moving the bounds to a convertible unit or to no unit at all turns the case clean |
| WF-100 | a characteristic check's explicit `eigenaar` DECLARES the characteristic it names (ch. 4.6 (q1), maintainer decision 2026-09-10): on a NON-navigated `CharacteristicCheck` the type the field names is crossed against that type's `kenmerken`, and the same key carries WF-034's existence cross and WF-043's time-dependence cross — where the field is present it is the key, where it is absent the enclosing form's subject is, one key computed once. It is the SECOND half of WF-034 (q), which fenced the field's RESOLUTION and said in as many words that it left this open, and it is a SEMANTIC decision rather than a resolution one: it settles WHICH OWNER IS AUTHORITATIVE, the field or the subject. **The field is.** **What the key means to a reader**, written out so no executor has to guess: every declaration consulted for the characteristic — does the type declare it, is it time-dependent (`kenmerkGranulariteit`), and the node key `char:<owner>:<name>` of the dependency edge the check contributes — is the NAMED type's. The dependency graph was already keyed that way in all four readers (the shared graph the verifier and the reference interpreter use, and both Drools graphs); what the rule adds is that the two crosses now agree with it. The OBJECT the check is evaluated on is the other half of the field and is NOT settled here: the field names the type whose PATTERN hosts the constraint, which is why the v0 producer emits it at all (it tags a fact-creation guard with the DOEL type so the constraint lands on the doel pattern and never on the anker, even under a same-named anker characteristic), and a bean-model executor honours that. Until 2026-09-19 the reference interpreter and the ASP backend read exactly ONE object per condition, so where the owner was not that object's type they REFUSED instead of answering from the subject — the reading this decision rejects. **That half is now decided too** (WF-109, ch. 4.6 (q2)): the fact-creation guard binds two objects and all four executors read the one the field names, so the refusal became a verifier verdict (`RUNTIME-SEMANTICS.md` ch. 5.8b). An absent owner and an owner naming the subject's own type are the same reading and always were, which is why no document that predates the rule moves. **Measured before the rule (2026-09-10), on a verifier-clean document** whose owner named a declared type OTHER than the subject's with both types declaring a same-named characteristic: verifier PASS, `ref` and the ASP backend both answering from the SUBJECT's value under the other type's name, and the Drools backend carrying no rule at all (`EXCLUDE transpiler SKIP: regel (voorwaarde buiten Phase-B scope)`) — one document, three executors, two readings and a silent one. Point the owner instead at a type that does NOT declare the characteristic and the third executor degrades further: on the fact-creation guard, where its bean model really does host the constraint on the named type, the DRL-text branch emits a property constraint against a bean with no such field and never compiles it (`Unable to Analyse Expression gehesen == true: unable to resolve method using strict-mode`) while the exec-model branch builds and dies at fire with `ConstraintEvaluationException: Error evaluating constraint` — both measured 2026-09-10 on the producer shape with the characteristic taken off the owner, where the verifier now answers `WF-100 FAIL /feitCreaties/0/voorwaarde/eigenaar: 'gehesen' is not a declared characteristic of 'Sein'`. **The subject-less day-kind position needs no separate sentence**: there the walk knows no subject, so the cross did not run at all and the field was read by nobody; under the owner key it is simply the only key there is. **Scope of the CROSS is the NON-navigated check** — on a navigated one the characteristic lives on the REACHED type, which WF-053's arm crosses there, so an owner is not that type and crossing it would reject a form nothing has decided; the `gedurende` clip member is refused an owner outright by WF-097. **The executors' hosting refusal is NOT so scoped and runs on both forms**, the owner naming the pattern the constraint hangs on whether or not the check then hops: measured 2026-09-10 on `jutterswet-postwacht` with the reached type in the field (`risicopost` via `post`, owner `Post`, subject `Wachter`), verifier PASS before and after, `ref` and the ASP backend PASS (10) before and faulting after, both Drools branches refusing throughout — so that form had NO answering executor at all, deliberately, and both executor suites carried a message-asserting witness for it; since WF-109 it is simply off the wire. Cost: none — re-counted the day the rule landed, **216** characteristic checks over the three verifier-gated zones (golden 166, negatief 359, rood 26 — 551 documents), **6** with an explicit owner, all six in the negatief zone and all six consistent (five naming a type that declares the characteristic, the sixth naming a type that is not declared at all, which (q) already refuses); the parity zone adds 9 more over 30 documents and 0 of them carry an owner. There is NO counter-example in the corpus and the rule's first witness was written for it, so this is a design decision on an empty CORPUS set — but NOT one without measured damage: **the pre-rule cross rejected the frontend's own emission shape**, measured 2026-09-10 on the `jutterswet-postwacht` fact creation (anker `Kustvak`, doel `Sein`, a `gehesen` characteristic on the doel, guard `eigenaar: "Sein"`) — before, `WF-034 FAIL /feitCreaties/0/voorwaarde: 'gehesen' is not a declared characteristic of 'Kustvak'`; after, PASS. The cost rides with the repair: on that now conforming document the two OCaml executors FAULT while BOTH Drools branches play it and answer `vakseinaantal` 1 from the DOEL instances, where before the rule the two OCaml executors answered 0 by reading the anker — one verifier-clean document, one executor answering and two refusing, which is exactly the third option the residue card records. The before/after sweep over the verifier-gated zones (golden 167, negatief 360, rood 26 — 553 documents) moves exactly the witness's verdict and not one other report line. **Residue, carded rather than noted, and since closed** (`backlog/done/20260910-eigenaar-gastheer-object.md`)**:** the OBJECT half — that a hosted guard is about the named type's instance — stayed open here, a refusal being a smaller answer than an implementation, and WF-109 is the implementation | gate 3 | `jutterswet-keurhouder` — the corpus's FIRST golden carrying an `eigenaar` at all (before it the field lived in the negatief zone only, so no gated, replayed case pinned what a reader does with it): two rules over both polarities, each naming the subject's own type as owner, beside a second declared type that carries a characteristic of its own. It is the source the witness is broken from and is replayed by gates 5, 6 and 12. **Stated rather than implied:** while WF-100 stood alone the REFUSING half sat outside the corpus for a ZONE reason and not an impossibility — a golden must replay, so it was not one; and a red case must fault in every executor that CARRIES its form, which the Drools backend did not do here (on a plain rule it drops the rule as a transpiler skip; on the producer shape it plays and answers without faulting). Since WF-109 the question is settled the other way: the form the producer writes REPLAYS in all four and is a golden (`jutterswet-seinhijsing`), and the unreachable owner is a verifier verdict with a negatief witness. The executors' refusal survives as a fail-fast fence on a non-conforming document and rests on their own gated suites, `ref/test/test_ref.ml` (ch. 5.8b, gate 5) and `backends/clingo/test/test_clingo.ml` (gate 6) | `wf100-eigenaar-verklaart-kenmerk-niet` (a declared owner that declares another characteristic and not this one) — falsifiable in two directions, each turning it clean: give the owner the characteristic (the verifier then admits it and the executors refuse it, a different rule and a different report), or put the subject's own type back in the field |
| WF-101 | a validity window's BOUNDS carry WF-084's date lexeme (ch. 4.1, maintainer decision 2026-09-11): every non-null `vanaf`/`tot` on every `Geldigheid` is the strict ISO form `yyyy-mm-dd` AND a real calendar day, checked by the very predicate WF-084 uses on a date LITERAL — the same grammar at the one date position on this wire that grammar did not reach. `null` is admitted and is not the WF-095 empty half: here it is the DECLARED open bound, so the rule reads present bounds only. **It turns an assumption three readers already rely on into a demand on the document.** The verifier, the reference interpreter and the ASP backend each HELD a bound as an OPAQUE STRING and ordered it with `<=` (until 2026-09-23; since card `ref-geldigheid-datumlezing` the executors read the bounds through the strict date reader and compare day numbers); `RUNTIME-SEMANTICS.md` ch. 8's version selection WAS that comparison and WF-021's overlap test still rests on the raw order in the verifier alone. Lexicographic order is calendar order only INSIDE this lexeme, and until this rule nothing on the wire demanded the lexeme — the assumption sat in a comment beside the verifier's overlap check and held of nothing. **Measured 2026-09-11 on `jutterswet-vensterheffing`**, one bound substituted and nothing else touched, verifier PASS in every row before the rule: `+012026-01-01` and `-0500-01-01` sort BELOW every four-digit year (`+` is 0x2B, `-` is 0x2D, both under every digit), so the window silently opens at the beginning of time and both executors applied the version to the casus BEFORE it — 125 where the pinned answer is leeg; `niet-een-datum` begins with a letter and sorts ABOVE every date, closing the window over its own casus — leeg where the pinned answer is 125. Not one row faults in any reader: every wrong answer is silent, and `ref` and the ASP backend agree row for row, so the differential cannot see it either. **Two layers, and the schema pattern can only carry one.** The pattern gate 2 puts on these two fields refuses three of those four lexemes and cannot refuse the fourth: `2026-02-30` is four-two-two digits, matches, and even ORDERS correctly (lexicographically between `2026-02-28` and `2026-03-01`), so no opaque-string reader ever REPORTS that February has no thirtieth. Whether it moves an ANSWER is the one place in the measurement where the substituted POSITION decides, and it is named rather than elided: at the LOWER bound of the window both executors replay the pinned expectations unchanged, while the same lexeme at that version's UPPER bound silently shrinks the window past the in-window casus and both answer leeg where 125 is pinned (measured 2026-09-11, both positions, neither faulting). Never reported and only sometimes visible in an answer is what a gated case cannot be relied on to catch, which is the argument for a rule here. A calendar is not a shape; that layer belongs where WF-084's already is. **What it makes true:** `<=` on the raw bound IS calendar order, so WF-021 in the verifier is correct AS WRITTEN — and since 2026-09-23 the executors parse the bounds outright, closing the same gap from the reader side. **Residue, stated rather than hidden:** a document reaching an executor WITHOUT passing the verifier met no fence in the executors until 2026-09-23; since then every executor parses the lexeme (ref and the ASP backend through the shared validity gate, Drools on its own threshold) and only the verifier reads the raw string — the same standing WF-081, WF-084 and WF-095 have. Cost: none — counted the day it landed, **1814** bound positions over the three verifier-gated zones (golden 169, negatief 364, rood 26 — 559 documents), **1800** null and **14** with a value; the twelve that predate the rule spell an admissible lexeme verbatim (`2025-01-01`, `2025-12-31`, `2026-01-01`, `2026-12-31`, over five documents) and the other two are this rule's own witnesses, while the parity zone adds 128 positions over 30 documents, all null. The before/after sweep over those zones moves exactly the two witnesses' verdicts and not one other report line | gate 3 | `jutterswet-vensterheffing` — two INDEPENDENTLY dated rules, not two versions of one name: they carry distinct names, write distinct attributes and sit in no rule group, the first writing both bounds and the second only an upper one, so the document exercises the WINDOW and not ch. 8 version selection (measured 2026-09-11). It is replayed by gates 5 and 6 on a casus inside the first window and one before it, and it is the document every measurement above is a twist of. The VERSIONED witness is `anchor/jutterswet-jutheffing`, where `#1` and `#2` of one name share a target attribute across adjacent windows with absolute per-window values through both executors — the two documents cover the two shapes a bound appears in, and only the second one puts WF-021 and the ch. 8 selection to work | `wf101-geldigheidsgrens-tekenjaar` (the SHAPE arm: a sign-prefixed extended year, which the schema pattern also refuses — in a zone gate 2 never validates) + `wf101-geldigheidsgrens-kalenderdag` (the CALENDAR arm: `2026-02-30`, which the pattern admits) — the two fall independently, so dropping the rule frees both and relaxing only the calendar half frees the second alone |
| WF-102 | a set fold source's MULTI-STEP range is a `bronPad`, and it is a CONNECTED path from the fold subject to the element type (ch. 4.3, maintainer decision 2026-09-11): **(a)** never beside `bronRol` — the one-step range IS `bronRol`, so one meaning keeps one spelling (the discipline WF-087 draws for the nullary keuzetype); **(b)** at least TWO steps, so the empty list and the single step stay the one-step spelling's territory; **(c)** admissible only at the position `bronRol` stands at and only on the **set** source — the non-set sources hand the fold something else entirely (the temporal total reads the subject's own timeline, the row-1 wart hands it the subject's linked instances), so a range does not apply to them; **(d)** every step resolves (WF-047, per step, at a new position rather than under a new rule) and the steps JOIN — step 1's opposite role filled by the fold subject, each further step's by the type its predecessor reaches, the last step's BOUND role by the `elementObjecttype`. **There is deliberately no bindingless fallback:** WF-048 derives a relation from a type PAIR, and a path's intermediate types are its own claim, never a consequence of its endpoints. **What it makes expressible, and what that closes.** Before it, a written range stacking two relations had no wire form and could only DEGRADE: the outer step fell away, and where a fact type happened to relate the subject to the element type directly the degraded document was verifier-clean and ref, the ASP backend and Drools all answered the same WRONG number — no gate saw it, since the differential square agreed (measured on the card's document B: 5,00 where the sentence says 3,00). The RESOLUTION-side half of that was shut on 2026-09-11 by refusing the sentence; this rule is the other half, which makes it expressible instead. The reading is the FLAT UNION and it is normative (`RUNTIME-SEMANTICS.md` ch. 3.4b): a SET of reachable elements, an element reachable along two intermediates contributing once, the empty-source rules applying to the union and never to an intermediate step. The nested reading is NOT a desugaring of it and differs at MIN/MAX (an empty intermediate has no fallback to absorb it), at AANTAL (elements versus intermediate instances) and at SOM with a non-zero `emptyFallback` (once, versus once per empty intermediate — with a zero fallback the two coincide numerically, which is why that third divergence is named and pinned nowhere). **Depth is UNBOUNDED, decided rather than assumed:** a cap is a constant with no semantics behind it and it only ever LOOSENS — raising it later turns ill-formed documents into well-formed ones, a wire-format event — so the corpus pins two steps and three instead. A TO-ONE step is an ordinary step: multiplicity is a declaration fact and does not enter the reading, which is what makes the to-one intermediate expressible too. **Cost, counted:** the four zones held 56 member folds and NOT ONE multi-step range before this rule, so no document moves; `ref` dedups the union by instance label, the ASP backend gets it free (its aggregate term names only the element, the intermediates being existentially quantified inside), and the Drools backend refuses the form by name in gate 12's channel floor (`Functie.bronPad`) — an EXPLICIT refusal, because its loader checks no unknown keys inside a node and a dropped field would re-derive the very misbinding this rule closes | gate 3 | `anchor/jutterswet-bootberging` — the two-step range and the three-step one on one document, replayed by gates 5 and 6 over two cases: the minimum on the FIRST intermediate while the second contributes (the answer that proves both are walked), the three empty corners, and the shared element that makes the sum 15,00 and not 18,00 — plus `jutterswet-vondstketen`, the EMITTED witness (gate 10) on a model that also carries a direct subject-to-element relation | `wf102-bronpad-naast-bronrol` (a), `wf102-bronpad-leeg` + `wf102-bronpad-een-stap` (b, falling independently — the second spells a VALID one-step range so the arity is its only defect), `wf102-bronpad-op-niet-setbron` (c), and three for (d): `wf102-bronpad-start-niet-op-onderwerp` (the first join — the wire form of the very degradation the field replaces), `wf102-bronpad-onverbonden-stap` (a middle join, on a document a rule checking only endpoints would pass) and `wf102-bronpad-landt-naast-elementtype` (the landing, one step too far). Beside them the two rules the new position widened rather than added: `wf047-bronpad-stap-onopgeloste-rol` (WF-047 at a step) and `wf083-vouwbron-anker-op-tussentype` (WF-083 (c) against an INTERMEDIATE type — the third class of type this field introduces into that position) |
| WF-103 | a present `supertype` names a declared object type (ch. 2.2): the chain must end at a declaration, or no member resolves through it. WF-003's territory by kind; it takes a number of its own because the failure mode is specific and a witness has to be able to cite it | gate 3 | `anchor/jutterswet-strandrechtketen` (the `Wrakrecht is een Strandrecht` chain, replayed by gates 5, 6 and 12) | `wf103-onbekend-supertype` |
| WF-104 | the supertype graph is acyclic, self-reference included (ch. 2.2). A cycle leaves member resolution no root to stop at, and every reader would either walk it forever or cut it off at a private depth of its own. Implemented as WF-085's colour DFS with a named cycle path, because the failure is the same shape: no fixpoint | gate 3 | `anchor/jutterswet-strandrechtketen` | `wf104-supertype-cyclus` (a two-step cycle) + `wf104-supertype-zelfverwijzing` (the depth-1 cycle, which falls independently: a reader that special-cases the self-edge passes the first and not the second) |
| WF-105 | no attribute or characteristic name is declared twice along one chain, ci (ch. 2.2) — WF-002's per-kind uniqueness lifted from the declaration to the chain. One instance would otherwise carry two declarations of one name, and which one a reader finds decides that name's datatype, dimensions and timeline facet on every read of it. The measured consumer consequence is sharper than a compile error: the Drools bean generator deduplicates member names silently, first-wins, so a shadowed name yields a bean that generates and compiles perfectly and is quietly missing one member. What is NOT refused is the sibling collision — two incomparable subtypes of one supertype declaring the same name — which is admitted without a rule (ch. 2.2): no instance is of two incomparable types, so no read is ambiguous, and the supertype-anchored read of such a name is refused by WF-034 rather than by this rule | gate 3 | `anchor/jutterswet-strandrechtketen` (`Wrakrecht` adds one own attribute beside three inherited ones, none of them redeclared) | `wf105-hershadowd-attribuut` |
| WF-106 | an object type declares at most one direct supertype (ch. 2.2) | **none, and none possible** — `supertype` is a bare string, so a second direct parent is not a well-formed-but-wrong document a check could reject but one that cannot be WRITTEN; the closed `Objecttype` node refuses the array form and gate 2 is where the refusal is mechanically enforced. A **deliberate, argued departure** from the spec discipline, recorded in ch. 2.2: the alternative was a `supertypes: [string]` wire form plus an "at most one" rule, which buys a real negatief at the price of a wire shape that can express exactly what the design forbids, and it was rejected. What the number buys is one citable name for one refusal across three layers | `anchor/jutterswet-strandrechtketen` declares one parent, which costs nothing and proves nothing — the rule's real witnesses are the two frontend fixtures | **none, and none possible** — the rule bounds the language ABOVE the wire, so its witnesses live where that language is read: the RegelSpraak dialect frontend's `is een A en B` fixture and the F-logic frontend's two-`::`-facts fixture, the latter being the sharp case since F-logic can actually write two parents |
| WF-107 | every NAMED form carries a present, non-blank name (ch. 3.1) — the ch. 3 versioned rule forms and the ch. 6.1 defaults groups, the same list WF-020 reads. Presence and uniqueness are two properties and take two numbers: WF-020 compares names, and until this rule it compared only the names that were there, filtering the nulls out before the comparison. **What the name IS**, so no reader has to decide: the form's identity — ch. 8 groups a form's versions by it, the ch. 5.1 dependency graph keys its channel node on it, and a regelversie channel test (`RUNTIME-SEMANTICS.md` ch. 5.6) names a rule by it. **Measured before the rule (2026-09-12)** on a nameless `ResolvedRegel`: verifier PASS, the reference interpreter and the ASP backend evaluating it without noticing, the Drools exec-model branch playing it, and the DRL-text branch throwing a `NullPointerException` out of the rule-name escaper, which calls a string method on the name it is handed — one verifier-clean document, four consumers, three answers and one crash. On a defaults group it is wider still: that array was in no name list at all, so a nameless group, a duplicate group name and a group colliding with a rule all three passed. **Blank counts as absent** because WF-020 compares over collapsed whitespace, so two blank names are one name and every blank form collides with every other. **The wire type stays nullable** and the schema is deliberately not the enforcement layer — a nameless form has to remain a well-formed document, or the rule would have no witness (the WF-106 situation, and the reason that rule has none). Cost: none — counted the day it landed, **1107** named forms over the four zones (626 documents: golden 184, rood 26, negatief 386, parity 30), every one of them carrying a non-blank name already except this rule's own three witnesses, so no producer moves and the rule closes a door nothing used. **Boundary, drawn per array rather than inferred** (all measured 2026-09-12 over the four zones): FOUR other top-level arrays put a name on this wire and each is outside for its own reason — `resolvedDagsoorten`, whose name is which day kind is being defined and lives in the day-kind namespace, so its presence is **WF-031**'s clause (added the same day, beside that rule's own dead-definition test); `tijdlijnen`, already covered, since WF-015 keys the declaration on the name and a nameless timeline takes every reference down with it, the document falling on **WF-003** (measured: `WF-003 FAIL … reference to undeclared timeline 'penningjaar'`); `regelGroepen`, a label nothing reads, WF-033 resolving a group's MEMBERS by name, so a nameless group is inert rather than ambiguous and refusing it would be a rule without a failure; and `regels`, the parse-stage shell index no check or executor reads, which one frontend emits empty by design. `dagsoorten` is a list of bare strings and a `null` entry there is admitted too — recorded as OPEN rather than closed, since it is the day-kind namespace again and no producer writes one | gate 3 | every golden — the rule's positive witness is the whole corpus, which is what makes it a promotion of existing practice rather than a new demand; named most sharply by `anchor/jutterswet-uitsluiting`, whose rule and defaults group are two named forms with two names | `wf107-naamloze-regel` (absent, on a rule) + `wf107-blanco-regelnaam` (present and blank — a presence-only check admits it, so it falls independently) + `wf107-naamloze-defaultsgroep` (the array no name rule reached) |
| WF-108 | the QUANTIFIER FOLD states a quantification something can satisfy (ch. 4.5). Five arms: (a) `quantifier` ∈ {all, none, at-least, at-most, exactly} — `the` is a definite description of ONE member and is refused BY NAME because a reader that fell through to the universal would answer the opposite of what the sentence wrote. **The three counting quantifiers were this node's named extension point until 2026-09-16 and are admitted since** (`Compound`'s vocabulary over instances instead of parts, with `Compound`'s `threshold` under `Compound`'s rule); (b) `elementType` present (its RESOLUTION is WF-034's, reported under that number at this position); (c) the RANGE is one meaning with two spellings — `sourcePath` not beside `sourceRole`, at least two steps, a connected walk from the quantifier's subject, landing ⊑-compatibly on `elementType` — WF-102's discipline under this node's own number, new on purpose because WF-102 is scoped to a fold's ARGUMENT, a position this node does not have, and a negatief directory must be able to declare the rule it violates; (d) `test` present, since a quantifier with no matrix states a range and asks nothing of it; (e) `threshold` present EXACTLY under the three counting quantifiers — absent there names no number to compare the count with, present under all/none records a quantification the `quantifier` field contradicts. The range MACHINERY is shared, not copied — `wf047_binding`, `wf048_derivable` and `resolve_binding` are the fold's helpers called from here, on the WF-097 shared-helper discipline — so an unresolvable binding still reports **WF-047** exactly as a fold's does, and the threshold's VALUE reports **WF-041** for the same reason; only the shape questions this node introduces carry this number | gate 3 | `jutterswet-ploegpremie` (the §8.2 form over seven hand-computed cells, ALLE and GEEN in one document: the five the production toolchain pins, plus the two that separate an ABSENT member timeline from a partially covered one — the second of which is the cell where the vacuous reading changes sign under GEEN) + `jutterswet-reddingsvloot` (the counting arms: all three quantifiers over one §8.2 matrix, each pinned at its threshold from both sides, plus the empty set, the empty SELECTION beside a filled range, and two vloten in one case for the range) + `jutterswet-ketenkwantor` (the one golden carrying (c)'s `sourcePath`: TEN_MINSTE 4 and TEN_HOOGSTE 3 over one two-step range with an element shared by two intermediates, counted once, each from both sides of its threshold, plus a second subject that pins the range; PRECIES is not witnessed over a path) | `wf108-kwantorvouw-kwantor-de` (the bare definite) + `wf108-kwantorvouw-drempel-ontbreekt` (a counting quantifier with no threshold) + `wf108-kwantorvouw-drempel-op-alle` (a threshold beside ALLE) + `wf108-kwantorvouw-zonder-toets` (no matrix) + `wf108-kwantorvouw-bereik-dubbel` (bronPad beside bronRol) + `wf108-kwantorvouw-zonder-elementtype` (unnamed population) |
| WF-109 | a characteristic check's explicit `eigenaar` is its GASTHEER and designates EXACTLY ONE object bound at the check's own position (ch. 4.6 (q2), maintainer decision 2026-09-19): exactly one binding SATISFIES it (`T ⊑ O`, every object it can hold is one) and no second binding OVERLAPS it (the declared type graph holds a type below both, so some object could be either). It is the OBJECT half of the field, which WF-100 settled the DECLARATION half of and recorded as open in as many words. A position binds ONE object, the enclosing form's subject — except the `voorwaarde` of a ch. 3.4 fact creation, which binds TWO (the anker it iterates and the doel its join chain reaches) and is the only such position in the language. Satisfaction is extensional (ch. 2.2, `T ⊑ O`), so an absent owner and an owner naming the subject's own type or a supertype of it are one reading, and no document that predates the rule moves. **Three flanks, all FAIL.** ZERO designated — the ordinary rule with a foreign owner, and the fact-creation guard naming a THIRD type — is a check about no object; until this rule it was an EXECUTOR refusal in `ref` and the ASP backend while both Drools branches dropped or markered the form, so the smaller answer was the only answer and the differential never closed. PARTIAL — an owner naming a strict SUBTYPE of the bound object, which holds for some of its objects and faults for the rest — is a run-dependent verdict and stays out; whether such an owner should instead read FALSE for the objects it does not cover is a question nobody has answered, and refusing it leaves that open. TWO designated — two satisfying, or one satisfying beside a second merely OVERLAPPING — is a type string that reaches both objects and settles nothing: the SELF-TYPED anker/doel pair, every common supertype of the two, and every subtype edge BETWEEN them. That last shape is why the count is over overlap and not over `⊑`: measured 2026-09-19, with `Sein ⊑ Kustvak` a concrete `Sein` fills an anker declared `Kustvak`, so an owner naming `Sein` reached both objects at run time while a `⊑` count saw one — and schema, verifier, `ref` and the ASP backend all passed that document. No precedence is invented for any of the three, because the producer has no route to them either (on `doel == anker` the frontend leaves the guard an unresolved marker). **Scope of the DOEL, with its reason — three fences:** the second bound object is admitted only where the check IS the whole `voorwaarde`, does NOT navigate, and carries NO whole-period qualifier. Measured 2026-09-19: the bean model's exec-model branch lowers a doel-side guard through a fact-only leaf predicate and carries no composite there, so admitting a nested doel host would split the executors instead of closing them; a navigated owner names the host while the characteristic lives on the type the hop reaches, a shape the producer never emits and no witness measures; and under a ch. 3.9 whole-period qualifier BOTH bean-model branches run the fold arm BEFORE the host test, so the field decides nothing there — one branch reached the doel by its own route and the other lowered the fold onto the ANKER and sank the package at compile. All three are refused under this number with the anker as the only bound object, and all three are cheap to revisit — a witness in all four executors is the price, and the third fence falls with the §8.2 family's own decision. The SUBJECT-LESS day-kind condition binds no object, so the cross does not run there and WF-031 refuses the document anyway. **What the rule buys:** the producer's own emission shape becomes replayable. Measured 2026-09-19 on `jutterswet-seinhijsing` before the implementation — verifier PASS, `ref` EVALUATION FAULT and the ASP backend TRANSLATION FAULT (both quoting the superseded hosting refusal), both Drools branches PLAYED with `vakseinaantal` 1 — and after it, all four answer 1, with the anker carrying the same characteristic so that the anker reading (2) and the intermediate-step reading (0) are each positively excluded. **Cost: none.** Counted the day the rule was written, before its own witnesses existed: 260 characteristic checks in 646 documents (golden 213, negatief 399, rood 34), 10 with an explicit owner — eight naming the subject's own type, one naming an UNDECLARED type that WF-034 (q) owns and this rule steps aside for, and exactly ONE naming a declared type the subject is not, which is the negatief witness WF-100 already rejects. The parity zone adds 9 owner-free checks over 30 documents. No document in any zone goes from clean to rejected; the rule adds a second citation to one already-rejected document and moves no other verdict anywhere. The EXTENSIONAL reading is the executors' too, and had to be made so: both Drools branches asked satisfaction by NAME IDENTITY until this rule landed, which refused a legal supertype owner in both of them while the other three executors carried it | gate 3 | `jutterswet-seinhijsing` (the producer's shape with one intermediate step: the anker AND the intermediate type declare and carry the same characteristic, so 1 is the doel reading and neither the anker's 2 nor the intermediate's 0) + `jutterswet-mastsein` (the same law over TWO intermediate steps, where the guard still reaches past both to the doel: 2, not the anker's 3, the first step's 0 or the second's 3) + `jutterswet-keurhouder` (the owner naming the subject's own type — the reading the rule leaves untouched) + `jutterswet-bakensein` (the owner naming a SUPERTYPE of the doel: hand-written IR, because the frontend tags a guard with the doel's own type and never with an ancestor) | `wf109-gastheer-buiten-bereik` (an ordinary rule whose owner DECLARES the characteristic, so WF-100 is satisfied and this rule is the only one speaking) + `wf109-gastheer-derde-type` (a fact-creation guard hosted on the intermediate step's type: two patterns, a name for neither) + `wf109-gastheer-ambigu-zelfgetypeerd` (anker and doel the same type, the degenerate string) + `wf109-doelgastheer-samengesteld` (the doel host nested in a composite) + `wf109-doelgastheer-genavigeerd` (the doel host on a navigating check) + `wf109-doelgastheer-periode` (the doel host under a whole-period qualifier) + `wf109-subtype-overlap` (the anker's declared type can itself be the host type) + `wf109-gastheer-ambigu-supertype` (one type above both bound objects) + `wf109-gastheer-deelpopulatie` (a host only some of the bound object's population is) |
| WF-110 | the INSTANCE-SET UNION — the §5.7 concatenation of subject expressions as the source of an instance count (ch. 4.3, maintainer decision 2026-09-20). Five arms: **(a)** an `InstanceSetUnion` is admissible ONLY as the SINGLE argument of an `AANTAL` aggregation, because it denotes INSTANCES and the one node that folds instances is the instance count (ch. 1.2e, ch. 3.4a) — the shape of rule WF-096 draws for the set reading, drawn for the other collection; **(b)** it carries at least TWO selections, one selection being the ordinary single-source count, which has a spelling already (`elementObjecttype` beside `bronRol`) — one meaning, one spelling, the discipline WF-102 (b) draws for the one-step range; **(c)** the count carrying it names NO source of its own, since the selections carry the population, the relation and the subselection one set each, and a source beside them is a second claim on one node (the setbron ground of 2026-09-19); **(d)** every selection names the SAME `elementType` — a count answers a cardinality over one DECLARED population (ch. 2.2), and with a population per selection whether two selections can share an instance becomes a question about the subtype lattice (ch. 2.6) that deduplication on identity would have to answer and this rule does not; the v0 toolchain refuses the same written shape for a MECHANISM reason of its own (one accumulate has one source pattern), so the boundary agrees and the ground does not; **(e)** every selection names its `elementType` and its `sourceRole`, with deliberately NO bindingless fallback — WF-048 derives a relation from a TYPE PAIR and clause (d) gives every selection of one union the same pair, so a derived range would make the selections indistinguishable and the union a restatement of one of them. The binding resolves under WF-047 at the new position, as WF-102 (d) applies it per step. **Deduplication on instance identity is the operation**: the members are the union of the selections' contributors and an instance two selections both select counts ONCE, so the answer is |A ∪ B| and never |A| + |B| (`RUNTIME-SEMANTICS.md` ch. 3.4d). **The node carries no `type`** — it denotes instances, not a value — and WF-034's type prologue exempts it by node kind exactly as it exempts a `Lambda`; without that exemption every union would carry a co-firing `value node without a static type`. **What it does NOT reopen:** a selection carries a one-step `sourceRole` and no `sourcePath`, so the multi-step range under a count stays where WF-096 and WF-102 met it (ch. 3.4b, consequence 2) — a named open flank rather than a side effect. **Cost, counted 2026-09-20 over the four zones:** the node is new to the wire, so not one of the 718 documents predating this card can carry it; swept with the pre-card binary against this one, the ONLY outputs that differ are the seven `wf110-*` witnesses, which the pre-card loader cannot read at all — zero verdict changes and zero report-line changes everywhere else | gate 3 | `jutterswet-vondstunie` (two selections over DIFFERENT relations with a shared element — the dedup witness no OR-filter can spell) + `jutterswet-keurunie` (two selections over ONE relation with overlapping characteristic filters, and a filtered selection beside an unfiltered one) + `jutterswet-unievoorwaarde` (the count over a union in a `voorwaarde` — the position clause (a) does NOT forbid, pinned so that reading the clause one notch too wide costs a failing gate) | `wf110-unie-als-regelwaarde` + `wf110-unie-onder-een-builtin` (a), `wf110-unie-een-tak` (b), `wf110-unie-naast-vouwbron` (c), `wf110-unie-ongelijke-elementtypes` (d), `wf110-unie-tak-zonder-elementtype` + `wf110-unie-tak-zonder-bronrol` (e) |
| WF-111 | time-window BOUND SHAPE (maintainer decision 2026-09-23): each bound is a date literal or an AttributeRef to a scalar date attribute of the form's own subject — un-navigated, not a set read, on a type the subject satisfies — and of day precision. The general date expression is an OPEN wire decision (RUNTIME-SEMANTICS ch. 3.12) | gate 3 | `jutterswet-premievenster` (four literal windows and the subject's own `meldatum` bound) | `wf111-periodegrens-parameterdatum` (a date PARAMETER as bound — until 2026-09-23 the red witness of the same form, refused by all four executors) |
| WF-112 | time window never under `none` or a counting quantifier — as a Compound member at any depth, or in a QuantifierFold's filter or test (maintainer decision 2026-09-23): the window gates the RULE and no reading of a complemented or counted verdict is specified | gate 3 | `jutterswet-premievenster` (`borgpremie`: the window as an `all` member) | `wf112-periodevenster-tellende-kwantor` (the window under `none` — until 2026-09-23 the red witness, refused by all four executors) |

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
