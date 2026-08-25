# The LegalRuleML expressiveness audit

*Status: audit (read-only), 2026-08-25. Answers the standing PLAN §3
test — "can our IR express what LegalRuleML can?" — cluster by cluster,
against the OASIS standard, with every coverage claim carrying a
concrete reference (WF rule, IR-SPEC / RUNTIME-SEMANTICS chapter, corpus
witness) a lens can re-trace. This document makes no normative change and
proposes no diff to `spec/CONSTRUCTS.md`; where it finds a gap it names
the consequence — a documented PLAN non-goal, a tracked `CONSTRUCTS.md`
delta, or a card candidate — and nothing more.*

## 0. Source of record

- **Standard:** LegalRuleML Core Specification Version 1.0. **OASIS
  Standard, 30 August 2021.**
- **Authoritative URL:**
  `https://docs.oasis-open.org/legalruleml/legalruleml-core-spec/v1.0/os/legalruleml-core-spec-v1.0-os.html`
  (latest-stage mirror:
  `https://docs.oasis-open.org/legalruleml/legalruleml-core-spec/v1.0/legalruleml-core-spec-v1.0.html`).
- **Scope read:** the *metamodel* (spec §4: Modeling Legal Norms,
  Metadata, Associations and Context) and its glossary (§3), which is
  what the PLAN borrows — "not the XML vehicle, but the requirements
  list" (PLAN §3, table row LegalRuleML). The RDFS metamodel, the two XML
  serialisations, the Relax NG / XSD schemas, and the CURIE/IRI machinery
  (§5) are the *vehicle* and are deliberately out of scope for a semantic
  audit, per the same PLAN sentence. Section numbers below (`§4.2.1`,
  etc.) are LegalRuleML's; uchar chapters are named as
  `IR-SPEC.md ch. …` / `RUNTIME-SEMANTICS.md ch. …` (abbreviated RS).

### The standing test, verbatim

PLAN §3 borrows from LegalRuleML "the requirements list: **defeasibility
with override relations, multiple time axes, jurisdiction**" and sets the
standing test: *"can our IR express what LegalRuleML can?"* PLAN §9
decision 2 fixes one boundary in advance — "the single time axis
(valuation date = calculation date = vintage selector) is accepted v1
scope; the spec must name the axis explicitly and design the extension
point for a second, knowledge-time axis." `CONSTRUCTS.md` rows 8, 12, 13,
32, 33 record the per-construct verdicts this audit must stay consistent
with. This document does not restate the test as met or unmet by
assertion; it earns the answer in §7 from the cluster findings.

### Reading conventions

Each concept gets one of three verdicts, adversarially — the audit is
worthless if it reasons toward coverage:

- **GEDEKT** — expressible in the v1 IR *today*, with a WF rule, a spec
  chapter, and a corpus witness that already replays green.
- **DEELS** — partly expressible; the finding states exactly what is and
  is not, and points at the tracked delta or the missing witness.
- **NIET** — not expressible, by decision or by omission; the finding
  states the consequence (a cited PLAN non-goal / decision, or a card
  candidate).

The three verdicts are counted once, in §7. A concept the Jutterswet
happens to contain (art. 3's *penningrecht*, a legal "right") is judged
by how the IR models it, not by its name — see §5.4.

---

## 1. Cluster 1 — Defeasibility (strict / defeasible / defeater,
override, priority)

LegalRuleML (§4.2.1) types the body→head link by strength: **strict**
(`body -> head`, indisputable — `StrictStrength`), **defeasible**
(`body => head`, holds unless rebutted — `DefeasibleStrength`), and
**defeater** (`body ~> head`, can only block the opposite conclusion,
never assert its own — `Defeater`). Conflict resolution is by
**specificity**, **salience/weight**, or a **preference (superiority)
relation**; LegalRuleML picks the last, materialised as
`<lrml:Override over="#cs2" under="#cs1"/>` — a first-class ordered pair
in a defeasible priority relation, optionally itself derivable
(`body => (r1 > r2)`) and context-scoped. Strength is attached either in
a `<lrml:Context>` (localised) or via `<lrml:hasStrength>` (global).

uchar's model is the **default ladder** (`IR-SPEC.md` ch. 6.1; RS ch. 7;
`CONSTRUCTS.md` row 8): a first-class, per-target list of prioritised
rungs evaluated top-down with fall-through — the highest rung that
*decides* wins.

| LegalRuleML concept | Verdict | Evidence and the honest boundary |
|---|---|---|
| **Defeasible rule** (`=>`) | **GEDEKT** | The ladder rung is exactly a defeasible conclusion: it holds unless a higher rung decides. WF-050/051; `IR-SPEC.md` ch. 6.1; RS ch. 7.1. Witnesses: `anchor/jutterswet-uitsluiting` (characteristic ladder, worked example 7.6.1), `anchor/jutterswet-vondstwaardering` (attribute ladder + leeg fall-through + initialisation rung), both replayed by gates 5–6. |
| **Priority between conflicting rules** | **DEELS** | *Within one target* priority is first-class and explicit: `rungs` is a **descending** priority list (`IR-SPEC.md` ch. 6.1), and below-root branching nests it as a rose tree (WF-075, RS ch. 7.9; `anchor/jutterswet-nestuitzondering`, `-nestkenmerk`, `-nestdiepte`). *Across targets / across independently authored rules* there is **no** free superiority relation — see the Override row. LegalRuleML's third resolution method (preference relation) is the one uchar realises, but only intra-target; specificity and salience are not modelled (salience was deliberately removed — `CONSTRUCTS.md` row 6, PLAN §2/§4). |
| **Override** (`<lrml:Override>`, a free `r1 > r2` pair) | **DEELS / NIET (as a free relation)** | uchar expresses override as **structure**, not as a standalone relation object: a higher rung *displaces* a lower one (RS ch. 7.1), and unordered co-writers with differing decisions route to the §9.5 conflict channel instead of silently overriding (WF-050, RS ch. 7.2; `anchor/jutterswet-toelagegronden`, six conflict-path cases). What is **not** expressible: a first-class, possibly *dynamically argued* superiority pair between two rules on **different** targets, or a preference that is itself a rule head (`body => (r1 > r2)`). `CONSTRUCTS.md` row 8 records this precisely — "LegalRuleML adds strict-vs-defeasible tagging and cross-rule override as refinements." → candidate C2 (§7). |
| **Defeater** (`~>`, block-only) | **DEELS** | The block-only effect exists *inside* a ladder: a characteristic rung with a false decision "derives nothing and displaces" (`IR-SPEC.md` ch. 6.1; RS ch. 7.1) — it defeats lower rungs without asserting a positive conclusion, which is the defeater's intent. But there is **no** standalone `Defeater` node that attaches to an arbitrary rule as a strength; the block is only a rung position within a target's ladder. Witness: `anchor/jutterswet-uitsluiting` (art. 4 "geen recht", a displacing non-attribution). |
| **Strict rule** (`->`, indisputable) | **DEELS** | The nearest form is the **monotone** base: a bare computing rule decides like the lowest non-init rung (the ch. 7.8 standing norm, `anchor/jutterswet-waardestelling`), and a characteristic attribution (`KENMERKTOEKENNING`) is order-free and idempotent — extensionally a one-rung constitutive rule (`CONSTRUCTS.md` row 8, Phase-5d note). But uchar carries **no strength tag**: nothing marks a conclusion as *indisputable* vs *defeasible* on the wire. Defeasibility is expressed by *presence of a ladder*, not by a `StrictStrength`/`DefeasibleStrength` label — a structural encoding, not LegalRuleML's metadata one. |
| **Strength tagging** (`hasStrength` / `appliesStrength`) | **NIET** | No wire slot tags a rule's strength; strength is implicit in structure (ladder ⇒ defeasible; bare rule ⇒ monotone base). Consequence: the strict/defeasible/defeater *vocabulary* is a refinement uchar does not carry — `CONSTRUCTS.md` row 8 ("strict-vs-defeasible tagging … as refinements"). Not needed for evaluation (the structure already decides); a documented deviation, folded into candidate C2. |
| **Defeasibility over *derived* facts (stratification)** | **GEDEKT** (uchar-specific strength) | LegalRuleML leaves conflict *resolution order* to the strength model; uchar additionally guarantees a **stratified** negative-dependency graph so defeasible negation over derived characteristics is well-founded: WF-045 (acyclic), WF-046 (no negative edge on a cycle), WF-051 (no cycle through a ladder). RS ch. 6; `CONSTRUCTS.md` row 6; witness `jutterswet-penningjaar` (a verifier-clean negated read). This is where uchar is *stronger* than the LegalRuleML metamodel, which does not itself require stratification. |
| **Discretionary defeat** (open-textured exception) | **GEDEKT** | The Jutterswet art. 5 / art. 9bis hardship power — a discretionary override — is a ladder rung whose justification is a supplied discretionary fact: `anchor/jutterswet-hardheid` (both executors), and through the Catala frontend `jutterswet-hardheidsaftrek-catala` (a `context` override lowered to a generated top rung, `CONSTRUCTS.md` row 9, gate 9). Divergence witness that the conflict channel is real: `jutterswet-tariefrecht-catala/case4-conflict` — an unordered exception sub-group where `catala interpret` aborts with `ConflictError` while uchar deposits the observable, non-fatal, path-keyed §9.5 fact (RS ch. 7.9). |

**Cluster verdict.** The *reasoning* content of LegalRuleML
defeasibility — defeasible conclusions, priority, block-only defeat,
conflict detection — is **GEDEKT**, and stratification exceeds the
metamodel. The two honest shortfalls are both about *reification*, not
computation: (i) uchar has **no free superiority relation** across rules
or targets (only intra-target ladder priority), and (ii) uchar carries
**no strength tags** (`StrictStrength`/`Defeater`), encoding strength
structurally. Both are flagged in `CONSTRUCTS.md` row 8 as refinements;
neither blocks any Jutterswet witness. → candidate C2.

---

## 2. Cluster 2 — Time (multiple axes vs the single axis + validity
windows + vintages)

LegalRuleML (§4.3.5) models the **external** temporal dimensions of a
norm along *three legal axes* — "when they come into force (entry), when
they effect the intended result (**efficacy**), and when they apply
(**applicability**)" — plus, in the vocabulary, statuses such as
`InForce` and `Efficacious`. It does so with `<lrml:TemporalCharacteristic>`
= (`forStatus`, `hasStatusDevelopment` ∈ {`Starts`, `Ends`, …},
`atTime`), attachable at very fine granularity (atom, rel, if, then) via
Associations. It **explicitly excludes** the temporal dimension of the
*content* events ("not … when a person is to present a tax application").

uchar (RS ch. 8; PLAN §9 decision 2) has **one** external axis — the
**evaluation date** (`asOfDate`), simultaneously valuation date,
calculation date, and vintage selector (RS ch. 8.1) — and, unlike
LegalRuleML, **does** model content-event time as first-class object
timelines (RS ch. 3). The mismatch runs in *both* directions.

| LegalRuleML concept | Verdict | Evidence and boundary |
|---|---|---|
| **Applicability axis** (gating: does the norm apply on date d) | **GEDEKT** | The evaluation date **gates** a versioned form against its validity window: WF-021 (version windows non-overlapping), `IR-SPEC.md` ch. 3.10, RS ch. 8.2; witness `jutterswet-vormenkabinet` (two dated versions of one rule name). This is the "applicability / in-force gating" of one axis, done exactly. |
| **Parameter versioning over time** (LegalRuleML's "penalty 500\$/750\$/1000\$ by crime date" example, §4.2.3.2) | **GEDEKT** | The vintage read: a granular parameter read at the evaluation date, `read(tl_p, Rekendatum)` — RS ch. 8.3–8.4, WF-018/005; witnesses `jutterswet-penningjaar` and `anchor/jutterswet-penningvintage` (RS ch. 8.5 worked example verbatim, gates 5–6), with a session-start coverage check that faults on a missing vintage before any rule fires. This is precisely LegalRuleML's "point to the correct penalty according to the time." |
| **Content-event timelines** (durations, per-period values) | **GEDEKT** (and *beyond* LegalRuleML) | Object/attribute timelines with granularity, cut-merge, pro-rata, day counts: RS ch. 3, WF-005/015/016/055/056/058; witnesses `jutterswet-getijdenwacht`, `anchor/jutterswet-borgdeel`, `anchor/jutterswet-dagentelling`. LegalRuleML deliberately does **not** model this (§4.3.5); the direction of the mismatch here is that uchar is richer, as PLAN §5 (Phase 5) predicts. |
| **Multiple *norm-status* axes** (entry-into-force vs **efficacy** vs applicability, held distinct) | **NIET** | uchar collapses valuation = calculation = vintage into one axis by design. Consequence: a **documented decision**, not an accident — PLAN §9 decision 2 ("single time axis … accepted v1 scope"); `CONSTRUCTS.md` row 12 ("Second time axis (knowledge time / retroactivity) — later(v2) … LegalRuleML distinguishes in-force / efficacy / application — the extension point must not hard-code 'exactly two axes'"). The extension point is *named* in RS ch. 8.1 ("a second, knowledge-time axis … a session-shape change, not a re-foundation"). |
| **Knowledge-time / retroactivity axis** | **NIET (later v2, extension point named)** | Same decision. RS ch. 8.1 names it as the second session parameter selecting rule versions/vintages "as known at k"; `CONSTRUCTS.md` row 12, PLAN §9 (2). Not a v1 gap to be carded — a designed-for future. |
| **Status developments** (`Starts` / `Ends` / and by extension Suspended / Repealed / Annulled) | **DEELS / NIET** | Validity windows give a norm-version's **start and end of applicability** (WF-021, `IR-SPEC.md` ch. 3.10) — the `Starts`/`Ends` of applicability. But there is **no** first-class status-transition model: no `Suspended`, `Repealed`, `Annulled`, no `forStatus`/`hasStatusDevelopment` pair. A repeal is expressed by *ending a version's window* and opening the successor's, not by a status event on the norm. Honest shortfall; naturally folded into the row-12 second-axis work rather than a standalone v1 card. |
| **Fine-grained temporal attachment** (temporal parameters on an atom/if/then, §4.3.5 + Associations) | **NIET (as attachment) / partially subsumed** | uchar attaches validity to a **rule version** (`geldigheid`) and a source ref to **every node** (`IR-SPEC.md` ch. 4), but there is no Association mechanism binding a temporal characteristic to an arbitrary sub-expression. In practice the single axis + per-version window covers the Jutterswet's needs; the sub-atom temporal-tagging generality is unmodelled and unneeded at v1. |

**Cluster verdict.** On the **single** axis LegalRuleML cares about
(applicability gating + version/vintage selection), uchar is **GEDEKT**
and, on content-event time, richer than LegalRuleML. On the
**multiplicity** of norm-status axes (efficacy ≠ enforceability ≠
applicability) and on status-development events, uchar is **NIET** — but
this is the single most-cited *decision* in the plan (PLAN §9 (2),
`CONSTRUCTS.md` row 12), with the extension point explicitly named in
RS ch. 8.1. No v1 card; the row-12 later(v2) work absorbs the
status-development refinement too.

---

## 3. Cluster 3 — Jurisdiction

LegalRuleML (§4.3.3) makes `<lrml:Jurisdiction>` a first-class metadata
node — "a geographic area or subject-matter over which an Authority
applies its legal power" — bound to statements through
`appliesJurisdiction` in an Association/Context, so the same rule can be
interpreted differently per jurisdiction.

| Concept | Verdict | Evidence and consequence |
|---|---|---|
| **Jurisdiction annotation** | **NIET** | uchar has no jurisdiction node, attribute, or binding. Consequence: a **documented non-goal** — `CONSTRUCTS.md` row 13 ("Jurisdiction — LegalRuleML — non-goal (v1) — standing LegalRuleML test"), and row 32's note names rows 12–13 as "the explicit boundary of the standing LegalRuleML test." PLAN §3 lists jurisdiction in the requirements list but the plan's own scope (a constitutive calculation IR) draws the line here. Not a card — a stated boundary. |

**Cluster verdict.** **NIET**, by explicit non-goal (`CONSTRUCTS.md`
row 13). If jurisdiction is ever needed it enters as metadata of the same
family as authority (row 33) — provenance, not evaluation-relevant — so
it would land as sugar on the source layer, never in the core calculus.

---

## 4. Cluster 4 — Deontic (obligation / permission / prohibition /
right)

LegalRuleML's deontic layer (§4.2.3): **prescriptive** norms carry a head
that is a `SuborderList` of deontic formulas over the operators `[OBL]`
(Obligation), `[PER]` (Permission), `[FOR]` (Prohibition), `[RIGHT]`
(Right); operators are **directed** (a `Bearer`, an `AuxiliaryParty`);
obligations can be **violated** (`([OBL]p) and -p`), triggering
`Penalty` → `Reparation` compensation chains; permissions come in weak
and strong flavours. LegalRuleML separates these from **constitutive**
("counts-as") norms, whose head defines an institutional fact and may
**not** be a deontic formula.

The distinction is the crux of the finding.

| LegalRuleML concept | Verdict | Evidence and boundary |
|---|---|---|
| **Constitutive / counts-as rule** (head = institutional fact) | **GEDEKT** | This *is* uchar's `KENMERKTOEKENNING` — "X counts as Y" as a characteristic attribution (`IR-SPEC.md` ch. 3.1; `CONSTRUCTS.md` row 8 Phase-5d note on monotone attribution). Witness: `jutterswet-penningrecht` — art. 3 *penningrecht* is modelled as the characteristic `penninggerechtigd` (`BIJVOEGLIJK`), a constitutive status, exactly LegalRuleML's constitutive half. uchar's whole calculus is the constitutive category. |
| **Prescriptive rule / deontic head** (`SuborderList` of `[D]A`) | **NIET** | No deontic operator, no prescriptive statement, no suborder list. Consequence: **documented non-goal** — `CONSTRUCTS.md` row 32 ("Deontic layer (obligation/permission/prohibition, violation → reparation, alternatives) — non-goal (v1) … uchar v1 is a constitutive calculation IR: rules define what counts as what, not who must do what"). |
| **Obligation / Prohibition / Permission / Right** | **NIET** | None expressible. Note the adversarial trap the corpus itself sets: art. 3 is literally a *recht* (right), yet uchar models it **constitutively** (a kenmerk `penninggerechtigd`), **not** as a LegalRuleML `[RIGHT]` with a Bearer and AuxiliaryParty. So even where the domain contains a deontic-sounding term, the IR expresses only the counts-as projection of it. Row 32. |
| **Violation → Penalty → Reparation** (compensation chains) | **NIET** | No violation state, no penalty/reparation linkage. Row 32. LegalRuleML's temporal-versioned penalty example (§4.2.3.2) is doubly out — both the penalty layer (row 32) and the multi-axis time it rides on (row 12) are unmodelled. |
| **Directed operators** (Bearer / AuxiliaryParty) | **NIET** | uchar has roles and role membership (`CONSTRUCTS.md` rows 4/5, WF-047) — a *structural* party model — but no deontic direction (who is bound, who benefits). The role machinery is for fact navigation, not for deontic party assignment. Row 32. |

**Cluster verdict.** Split cleanly along LegalRuleML's own
constitutive/prescriptive line: the **constitutive** half is **GEDEKT**
(it is uchar's core); the **prescriptive/deontic** half is **NIET**, an
explicit, single-sentence non-goal (`CONSTRUCTS.md` row 32). This is the
sharpest boundary of the standing test and the plan draws it on purpose:
uchar computes what-counts-as-what, not who-must-do-what.

---

## 5. Cluster 5 — Sources / authority / traceability (the Bron
machinery)

LegalRuleML (§4.3.1) is built on **isomorphism**: `<lrml:LegalReference>`
(non-IRI ids, e.g. Akoma Ntoso `/au/2012-05-30/C628:2012/…#sec2.2`,
ECLI), `<lrml:LegalSource>` (IRI-based), `<lrml:Source>` (external
non-legal), all wired to rules through `<lrml:Association>` in an **N:M**
relation, at fine granularity (rule, fragment, atom). `<lrml:Authority>`
(§4.3.4) and Agent/Figure/Role (§4.3.2) qualify provenance —
who created/endorsed the rule, acting in which function.

uchar's answer is the **Bron** machinery (`CONSTRUCTS.md` row 3; PLAN §4
"Traceability is a first-class obligation").

| LegalRuleML concept | Verdict | Evidence and boundary |
|---|---|---|
| **Source reference on every rule/fragment** | **DEELS** | Every IR node carries a `source` (`IR-SPEC.md` ch. 4.1, ch. 4 preamble). But (a) it is today the **v0 `bron` string** — the structured `Bron(file, line, column, snippet)` is a **tracked delta** not yet on the wire (`IR-SPEC.md` ch. 4 preamble "pending delta (tracked, `CONSTRUCTS.md` row 3)"; row 3); (b) the string points to the **frontend surface fragment** (the RegelSpraak/Catala text — witness: `jutterswet-penningrecht/ir.json`, `"bron": "Een Jutter is penninggerechtigd"`), not to a legal-source identifier. So intra-repo, rule↔surface traceability exists; it is coarse and not yet structured. |
| **Legal-source identifier** (Akoma Ntoso / ECLI / IRI) + N:M source↔rule isomorphism | **NIET** | uchar has no `LegalReference`/`LegalSource` node, no external legal-source id, and no N:M source↔rule Association. The statute lives in the corpus as prose (`corpus/jutterswet/wet-v*.md`, art. 1–5), but the IR does not reference statute articles by a machine identifier. PLAN §4 states the *ambition* ("back to … the statute article … via `// Bron:` annotations"), and `// Bron:` annotations exist upstream in `regelspraak-java` (Phase 0, `bronAnnotaties`) — **but zero corpus goldens carry `bronAnnotaties` on the wire** (verified: `grep -rl bronAnnotat corpus/golden` → 0). So statute-article traceability is *promised and upstream-built* yet **unwitnessed in this repo**. → candidate C1. |
| **Authority** (who endorsed the norm) | **NIET (later v2)** | No authority node. Consequence: `CONSTRUCTS.md` row 33 ("Norm authority/agent attribution — LegalRuleML — later(v2) — sugar (metadata on row 3) — provenance metadata on source references; cheap, not evaluation-relevant"). A stated later, not a v1 gap. |
| **Agent / Figure / Role** (author acting in a function) | **NIET (later v2)** | Same row 33. uchar's `Rol` is a fact-navigation role (WF-047), a different notion from LegalRuleML's authorship Role/Figure; the provenance-agent model is row 33 metadata. |

**Cluster verdict.** **DEELS.** The traceability *skeleton* is real and
first-class (every node has a source ref; the structured form is a
tracked row-3 delta), and it is exactly the surface-source isomorphism.
The two shortfalls are honest: (i) **legal-source identifiers** (statute
articles by machine id) and the N:M source↔rule isomorphism are
**unmodelled**, and the `// Bron:` statute-annotation path — built
upstream and named as a PLAN §4 obligation — is **not witnessed** by any
corpus golden (→ candidate C1); (ii) **authority/agent** provenance is a
stated later (row 33). uchar's Bron answers "which surface text produced
this node," not yet "which article of law, endorsed by whom."

---

## 6. Cluster 6 — Alternatives / Context

LegalRuleML (§4.2.4) `<lrml:Alternatives>` records **multiple, possibly
conflicting interpretations** of the same textual source, selected by a
`<lrml:Context>` (§4.4.2) — the same provision reading one way in one
jurisdiction/time and another elsewhere. `<lrml:Context>` (§4.4)
bundles metadata (`appliesSource`, `appliesTemporalCharacteristics`,
`appliesStrength`, `appliesAuthority`, `appliesJurisdiction`,
`appliesAlternatives`) onto a rule scope via Associations — the bridge
between metadata and rules.

| LegalRuleML concept | Verdict | Evidence and consequence |
|---|---|---|
| **Alternatives** (divergent interpretations of one source) | **NIET** | uchar carries **one** interpretation per rule. Rule *versions* (WF-021) are **time-selected**, not competing interpretations; the §9.5 conflict channel records **over-determination** at runtime, not *authored interpretive alternatives*. Consequence: **documented non-goal** — `CONSTRUCTS.md` row 32 bundles "alternatives" into the deontic non-goal row. No card. |
| **Context** (metadata-bundle indirection over a rule scope) | **NIET (subsumed inline)** | uchar has no Context/Association indirection layer. What LegalRuleML localises in a Context (strength, temporal, source), uchar attaches **inline**: a ladder *is* the strength structure (§1), `geldigheid` *is* the temporal window on the version (§2), `bron` *is* the source on the node (§5). The metadata-vs-rule "bridge" is collapsed into the rule, so there is nothing to express separately — and, without jurisdiction/authority/alternatives (rows 13/33/32), the Context has no distinct payload to carry at v1. |

**Cluster verdict.** **NIET.** Both concepts are out — alternatives as an
explicit non-goal (row 32), context as an indirection with no v1 payload
(its would-be contents are either inline or themselves non-goals). This
is coherent with uchar's stance: a *calculation* IR fixes one reading and
computes it; representing the space of disputed readings is a different
product.

---

## 7. Answer to the standing PLAN §3 test, and the candidate list

**The answer, in one paragraph.** *Can our IR express what LegalRuleML
can?* On the **reasoning core** LegalRuleML exists to carry —
defeasibility with priority, block-only defeat, and conflict detection —
**yes**, and with a stratification guarantee the LegalRuleML metamodel
does not itself impose (cluster 1, GEDEKT: WF-050/051/075/045/046, ladder
ch. 6.1 / RS ch. 7, anchors `jutterswet-uitsluiting` /
`-vondstwaardering` / `-toelagegronden` / `-nestdiepte`); and on the
**one time axis** LegalRuleML's applicability/efficacy machinery reduces
to — version gating plus vintage selection — **yes**, indeed with richer
content-event timelines than LegalRuleML models (cluster 2, GEDEKT:
WF-021/018, RS ch. 8, `jutterswet-vormenkabinet` / `-penningvintage`).
But the standing test's own three headline items split: **override** is
only **DEELS** — uchar has intra-target ladder priority, not
LegalRuleML's free `r1 > r2` superiority relation or its
strict/defeasible/defeater strength tags (`CONSTRUCTS.md` row 8);
**multiple time axes** is a deliberate **NIET** — one axis by PLAN §9
decision 2, with the second (knowledge-time) axis a *named* extension
point (RS ch. 8.1, `CONSTRUCTS.md` row 12, later v2); and **jurisdiction**
is a stated **NIET** (`CONSTRUCTS.md` row 13). Beyond the headline list,
the **deontic** layer (obligation/permission/prohibition/right, violation
→ reparation) and **alternatives/context** are **NIET** by explicit
non-goal (`CONSTRUCTS.md` rows 32/33) — uchar v1 expresses the
**constitutive** ("counts-as") half of LegalRuleML's norm typology and,
by design, not the **prescriptive/deontic** half; and **traceability** is
**DEELS** — every node carries a surface-source ref (structured Bron a
tracked row-3 delta), but statute-article isomorphism to legal-source
identifiers is unmodelled and, though promised by PLAN §4 and built
upstream, is **unwitnessed** by any corpus golden. So the honest verdict:
uchar **can express LegalRuleML's defeasible-reasoning and single-axis
temporal core, and does so as far as the plan's declared scope reaches**;
everything it cannot express is a *cited decision* (single axis;
constitutive-not-deontic; jurisdiction/alternatives non-goals) rather
than an oversight, with exactly two live loose ends worth a card.

**Consequence ledger** (every NIET/DEELS accounted for):

| Cluster | Concept | Verdict | Consequence |
|---|---|---|---|
| 1 Defeasibility | defeasible / priority (intra-target) / defeater-in-ladder / stratification / discretion | GEDEKT | — |
| 1 | cross-rule Override, strength tags | DEELS/NIET | refinement, `CONSTRUCTS.md` row 8 → candidate **C2** |
| 2 Time | applicability gating, vintage, content timelines | GEDEKT | — |
| 2 | efficacy/enforceability multiplicity, knowledge-time axis, status developments | NIET | PLAN §9 decision 2; `CONSTRUCTS.md` row 12 (later v2); extension point RS ch. 8.1 |
| 3 Jurisdiction | jurisdiction | NIET | non-goal, `CONSTRUCTS.md` row 13 |
| 4 Deontic | constitutive / counts-as | GEDEKT | — |
| 4 | obligation/permission/prohibition/right, violation→reparation, directed operators | NIET | non-goal, `CONSTRUCTS.md` row 32 |
| 5 Sources | source ref on every node (surface) | DEELS | structured Bron = tracked `CONSTRUCTS.md` row 3 delta |
| 5 | legal-source id + N:M isomorphism; statute annotations on the wire | NIET/unwitnessed | PLAN §4 obligation, `bronAnnotaties` upstream but 0 goldens → candidate **C1** |
| 5 | authority / agent | NIET | later v2, `CONSTRUCTS.md` row 33 |
| 6 Alternatives/Context | alternatives; context indirection | NIET | non-goal, `CONSTRUCTS.md` row 32 (alternatives); no v1 payload (context) |

**Candidate list** (proposals only; no diff to `CONSTRUCTS.md` is made
here):

- **C1 — Witness statute-article traceability on the uchar wire.** The
  `// Bron:` annotation path is built in `regelspraak-java` (Phase 0) and
  named a first-class obligation in PLAN §4, yet **no `corpus/golden`
  ir.json carries `bronAnnotaties`** (verified count 0). Candidate: bump
  the emitter/schema so a golden carries statute-article annotations, add
  the corpus witness, and (optionally) tie the annotation to a
  machine-readable legal-source id — the one place LegalRuleML's
  isomorphism requirement lands as a *concrete, checkable* uchar gap
  rather than a stated intention. Small, corpus-and-schema-scoped; the
  natural companion to the row-3 structured-`Bron` delta.
- **C2 — Decide the override/strength question.** uchar expresses
  priority only *within* a per-target ladder and tags no strength.
  Candidate: a design note (not a v1 build) settling whether
  LegalRuleML's free superiority relation (`<lrml:Override>`, incl.
  `body => (r1 > r2)`) and its strict/defeasible/defeater strength tags
  are a genuine requirement for the corpus's law, or whether the
  intra-target ladder is sufficient by construction — `CONSTRUCTS.md`
  row 8 already frames these as "refinements," so this is a scope
  confirmation, not new machinery.

- **Documented, no card (for completeness):** second time axis + status
  developments → PLAN §9 (2) / `CONSTRUCTS.md` row 12 (later v2, extension
  point named); jurisdiction → row 13 (non-goal); deontic layer +
  alternatives → row 32 (non-goal); authority/agent → row 33 (later v2,
  sugar on row 3). These are boundaries the plan already drew; the audit
  confirms them rather than reopening them.
