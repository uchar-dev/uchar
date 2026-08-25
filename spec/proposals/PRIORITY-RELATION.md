# C2 — the free priority relation: design exploration and proposal

*Status: PROPOSAL · 2026-08-25 · working prose, not normative. Answers
LegalRuleML-audit candidate C2 (`LEGALRULEML-AUDIT.md` §7): uchar
expresses override as *structure* — intra-target ladder priority — and
carries no free `r1 > r2` superiority relation across rules or targets
and no strict/defeasible/defeater strength tags. This document maps the
design space, recommends a design (**do not build**, with two small
positive companions), sketches the migration and verifier implications
of every option including the declined ones, and ends in §9 with
numbered ratification questions the maintainer can answer one by one.
Nothing in it changes `IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, the schema,
the verifier, an executor, or the corpus. It lives outside every gate-1
zone and may therefore name the private toolchain; none of its text may
be folded into a zoned document without stripping those references
first. External references: LegalRuleML Core Specification v1.0 (OASIS
Standard, 2021-08-30), as read by `LEGALRULEML-AUDIT.md` §0–§1; and
Merigoux, Chataing & Protzenko, "Catala: A Programming Language for the
Law", ICFP 2021 — "the paper", via `PHASE4-DEFAULTS.md` §2.*

## 1. The question, precisely

LegalRuleML resolves rule conflict by a **preference (superiority)
relation**: `<lrml:Override over="#cs2" under="#cs1"/>` — a *static*,
first-class ordered pair between Legal-Statement identifiers, attachable
to any two statements, on the same target or not (`LEGALRULEML-AUDIT.md`
§1, read from spec §4.2.1 and the glossary). Beside it sits a **strength
vocabulary** — `StrictStrength` (indisputable), `DefeasibleStrength`
(holds unless rebutted), `Defeater` (blocks, never asserts) — attached
via `hasStrength`/`appliesStrength`. A *derivable* superiority
(`body => (r1 > r2)`, the relation in a rule head) appears in the
standard only as cited literature (Antoniou [2] in spec §4.2.1) and is
**not** machinery the standard defines; the audit's lens round moved
the Override attribution to exactly that boundary ("Override-attributie
naar Antoniou", its review commit), and this document keeps it.

uchar has none of these as wire objects. The audit recorded the
shortfall (cluster 1: Override **DEELS/NIET as a free relation**,
strength tagging **NIET**) and `CONSTRUCTS.md` row 8 frames both as
"refinements". C2 asks: is that a gap to close, or a boundary to ratify?

**Demonstrated mechanically** (2026-08-25, before this proposal was
written): a copy of the `jutterswet-toelagegronden` anchor `ir.json`
extended with a hypothetical free pair

```json
"superiorities": [{"boven": "toelage voor vissers",
                   "onder": "toelage voor wachters"}]
```

is rejected by gate 2 in explicit-file mode:

```
FAIL: /tmp/c2-red/ir.json
  at: <root>
  Additional properties are not allowed ('superiorities' was unexpected)
check-ir-schema: FAIL (1 of 1 file(s) bad)
```

The top level of `regelspraak-ir-v0.schema.json` is
`additionalProperties: false`; no slot on the wire, no node in the
verifier's AST, no vocabulary anywhere in `spec/schema/` or
`spec/verifier/lib/` (grep for superiority/salience/priority terms:
zero hits outside the ladder machinery). The gap is real; the question
is whether it is a defect.

## 2. What uchar has instead — the verified inventory

Every claim below was re-checked against the source files for this
card; each carries its witness.

| Priority phenomenon | uchar encoding | Witness (verified) |
|---|---|---|
| defeasible conclusion | ladder rung: holds unless a higher rung decides | `IR-SPEC.md` ch. 6.1; RS ch. 7.1; anchors `jutterswet-uitsluiting` (characteristic), `jutterswet-vondstwaardering` (attribute, leeg fall-through, init rung) |
| explicit priority, intra-target | `rungs` — a **descending** list, highest first; below the root a rose tree via `Rung.excepties` | `IR-SPEC.md` ch. 6.1 (field table: "descending priority: highest first"); WF-075; anchors `jutterswet-nestuitzondering`, `-nestkenmerk`, `-nestdiepte` |
| explicitly *absent* priority | the declared-unordered group (`ongeordend: true` on every member) — "priority is explicit (rungs) or explicitly absent … never implicit" | RS ch. 7.1; WF-050 multiplicity clause; anchor `jutterswet-toelagegronden` (six ladders on `ir.json`: five group members carry `ongeordend: true` on the wire, the sixth is a lone ladder needing no marking) |
| conflict detection | differing simultaneous decisions → no write + one §9.5 channel fact, keyed by target form; nested conflicts path-keyed | RS ch. 7.2/7.9; `jutterswet-toelagegronden/case3-conflict.json` (expected `"inconsistent", "rule": "Jutter.toelage"`), `jutterswet-tariefrecht-catala/case4-conflict.json` (`"rule": "Penningplichtige.tarief/0"`) |
| block-only defeat (defeater) | a characteristic rung with `besluit: false` — "derives nothing and displaces" | `IR-SPEC.md` ch. 6.1; RS ch. 7.1; anchor `jutterswet-uitsluiting` (art. 4 "geldt niet") |
| indisputable conclusion (strict) | the single-writer bare form: WF-050's no-other-writer clause, WF-026, WF-052 and — for the unconditional collision — WF-079 fence the second writer; a guarded overlap escapes the static fences and falls to the differential square (fact 2 below); a deciding bare rule is displaced by nothing (a non-deciding one leaves the input standing, ch. 7.8) | `IR-SPEC.md` WF index; `spec/verifier/lib/checks.ml` (WF-050 from line ~3707, WF-079 from ~3963 — its comment scopes it to the bare collision); negatiefs `wf050-ladder-beside-rule`, `wf079-two-unconditional-writers` |
| priority vs. time | version selection is **temporal, never defeasible** — "a later version does not 'override' an earlier one … they never co-apply" | RS ch. 7.4; WF-021 |
| priority vs. stratification | every rung read (justifications and consequences, recursively through sub-rungs) folds into **negative edges** toward the ladder's write; strata must exist | WF-051; `checks.ml` `dep_forms` (the `ladders` block, `rung_reads` recursing through `rung_excepties`); negatiefs `wf051-ladder-reads-own-target`, `wf051-cycle-through-subrung` |
| displaced priority is speculative | a higher settling rung displaces a lower sub-group *including its conflict fact* | RS ch. 7.9; `jutterswet-nestuitzondering/case9-verdrongen-conflict.json` |

Three structural facts carry the whole analysis:

1. **A single ladder cannot conflict** — at most one rung decides
   (RS ch. 7.1, "a property of the semantics, not a claim about
   authors").
2. **Outside a ladder, competing writers are fenced — statically
   where that is sound, by the differential square beyond it, never
   arbitrated.** The static fences: WF-050 (no writer beside a
   ladder), WF-026 (one allocation per target), WF-052 (one creation
   writer per to-one relation), WF-079 (one *unconditional* bare
   computing rule per target). WF-079 is deliberately scoped to the
   unconditional collision — "only the bare (unconditional) collision
   is statically over-determined" (`checks.ml`, the WF-079 comment) —
   and its landing decision names the residue: "a conditional overlap
   that is only semantically over-determined stays a
   differential-square catch" (`PHASE5D-DIFF.md` §8.2, maintainer,
   2026-08-24). That residue is real and verifier-clean: two *guarded*
   bare writers whose guards overlap. Verified for this card
   (2026-08-25) on a scratch copy of `synth-grensrand/ir.json` with a
   second guarded `BEREKENING` on `Meting.indicatie` (value 2, guard
   `lengteA <= lengteB`, beside the golden's value-1 rule guarded
   `>=`): the verifier PASSes it; on `lengteA = lengteB = 30` (both
   guards true) `ref/` faults — `EVALUATION FAULT: no fixpoint within
   100 passes — divergence is a fault (ch. 5.7)`, an evaluation
   fault, not a §9.5 fact — while clingo exits 0 on a
   `consistent`-only pin, no fault and no §9.5 fact, and pinning
   either single value reports `MISMATCH: M1.indicatie = [2;1]`: the
   stable model silently holds both. The same
   un-pinnable ref ≢ clingo split §8.2
   exhibited for the unconditional pair: not a golden (ref ≢ clingo),
   not a negatief (verifier-clean), not a rood (clingo raises no
   fault) — no corpus zone admits the program, so the fence is
   gates 5–6, not a WF rule. The one multi-writer form the IR admits
   *with defined, pinnable semantics* is the declared-unordered
   group, and only that form reports its over-determination loudly
   (one leeg target, one §9.5 fact, both executors).
3. **Priority is static structure.** Rung order is document shape;
   the verifier checks it (WF-050/075) and folds it into static strata
   (WF-051) both executors share. Nothing about who-beats-whom is
   computed at runtime from data.

## 3. Why the gap exists on purpose — the design lineage

The absence of a free relation is not an oversight three times over:

- **Catala dropped it first.** The ladder is Catala's default calculus
  (adopted, `CONSTRUCTS.md` row 8; RS ch. 7.1); the paper explicitly
  removed the run-time pre-order `⩽` of prioritised default logic in
  favour of purely structural priority — exception nesting — and calls
  that its key simplification (`PHASE4-DEFAULTS.md` §2.1, paper §4.4).
  Importing a free `r1 > r2` would re-introduce precisely the mechanism
  the adopted calculus's authors identify as the thing they got rid of.
- **v0's salience was retired the same way.** The founding review
  flagged Drools salience as engine leakage; it became *declared
  stratification* (WF-045/046, `CONSTRUCTS.md` row 6 — "salience −1 →
  WF-045/WF-046"), not a portable priority number. A superiority
  relation is salience with pairs instead of integers.
- **The spec already stakes the boundary.** RS ch. 7.4:
  "Strict-versus-defeasible tagging and cross-rule override
  (LegalRuleML) are recorded refinements, not v1-design scope." Row 8's
  note says the same. C2 is the request to *decide* that recorded
  stance, not to discover it.

## 4. The design space

### 4.1 O1 — the free superiority relation as a wire object

A top-level `superiorities` list of ordered statement pairs, the direct
image of `<lrml:Override>`. Split by what the pair would relate:

- **Same target.** The only same-target multiplicity with defined,
  pinnable semantics is the declared-unordered group (§2 fact 2). A
  pair ordering two group members re-expresses what rung order in one
  ladder already expresses
  — two encodings of one priority, which WF-050's "explicit or
  explicitly absent, never implicit" exists to prevent. Worse, a pair
  ordering *some* members creates the **partially ordered group**, a
  third reconciliation regime between chain and unordered group that
  RS ch. 7 has no semantics for (which member displaces whom when
  `r1 > r2` holds but `r3` is unordered against both?) — new lattice
  semantics, a new conflict definition, and a re-run of the §7
  spurious-conflict analysis of `PHASE4-DEFAULTS.md`, for a construct
  no statute in the corpus needs. And where the group members *do*
  conflict today, the channel reports it loudly; an override pair would
  resolve it silently — trading the fail-fast §9.5 discipline for
  quiet arbitration, the exact trade RS ch. 7.5 row 5 retired
  ("silent last-writer-wins"). One same-target place remains where a
  free pair would genuinely have something to decide: the
  guarded-overlap residue of §2 fact 2 — two verifier-clean writers,
  no defined winner. But the IR already owns the arbitration form for
  exactly that competition: put both writers on one ladder and the
  rung order *is* the pair, statically checked (WF-050/075) and
  pinned by both executors. A free `r1 > r2` there adds no
  expressiveness; it would instead legitimise, as a supported
  program, the form the differential square exists to keep out of
  every corpus zone.
- **Different targets.** Two rules on different targets never compete:
  each writes its own value node, and conflict — the thing a
  superiority relation exists to resolve — cannot arise between them.
  LegalRuleML needs the free pair because its statements may derive
  contradictory conclusions anywhere; uchar confines that substrate to
  same-target competition, fenced per §2 fact 2 (statically or by the
  differential square) — cross-target there is no shared value node
  left to contradict. A cross-target `r1 > r2` would
  be a pair with nothing to decide: to give it meaning one would first
  have to *weaken* WF-050/026/079 and re-admit competing writers —
  dismantling the confluence argument of RS ch. 5.2/7.1 to make room
  for a mechanism whose job the dismantled rules were doing.

Cost if built anyway (sketched honestly, so the decision is informed):
an IS-Method extension field (additive, optional, never
emitter-produced — `IR-SPEC.md` § Method); two or three new WF rules
(references resolve; same-target-only or a defined cross-target
meaning; acyclicity of the pair digraph; interaction with `ongeordend`)
at the then-next free WF numbers (WF-080 is the highest landed; WF-081
is reserved by a seeded card); a new RS ch. 7 section defining
partially-ordered reconciliation; changes in both executors plus the
differential, anchors, and negatiefs. And **no producer**: RegelSpraak
has no priority surface (its salience was derived away, row 6), and the
Catala plugin cannot emit one (the paper's calculus compiled priority
into structure before we ever see it). The field would be authorable
only by hand-written anchors — wire surface with no frontend behind it.

### 4.2 O2 — strength tags on rules or rungs

A `strength ∈ {strict, defeasible, defeater}` enum. In this IR every
value of the tag is already **derivable from structure**: a laddered
target is defeasible (rungs above can displace); a `besluit: false`
rung is the defeater (blocks, asserts nothing — RS ch. 7.1); a bare
single-writer form is as strict as this calculus gets (no displacer
leaves a well-defined program, §2 fact 2). A wire tag therefore
carries either zero information
(it agrees with the structure — redundant) or a contradiction (a
"strict" tag on a rung below another rung — and then the verifier must
reject it, meaning the tag *still* carries zero information, at the
price of new WF rules to keep two encodings synchronised). Evaluation
would not read it: RS ch. 7's semantics is complete without it. The
audit said it precisely: strength is "a structural encoding, not
LegalRuleML's metadata one" — the encoding exists; only the label is
missing, and the label does no work.

### 4.3 O3 — the derived export mapping (reification without wire)

If LegalRuleML *interchange* ever becomes a goal, the reification is
mechanically derivable from structure, in the exporter, with no wire
change: each adjacent pair in one ladder's `rungs` ↦ one
`Override(over = higher, under = lower)`; a laddered target ↦
`DefeasibleStrength`; a `besluit: false` rung ↦ `Defeater`; a bare
single-writer form ↦ `StrictStrength`; a declared-unordered group ↦
deliberately **no** Override pairs (the absence is the declaration).
The mapping is total on the landed forms because structure is the
richer encoding — the direction structure → tags loses nothing, while
tags → structure (LegalRuleML's direction) needs the relation to be
complete and acyclic before it means anything. No export target exists
or is planned (PLAN §3 takes LegalRuleML as a requirements list, "not
the XML vehicle"), so this stays a recorded direction, not an artifact.

### 4.4 O4 — the derivable override (`body => r1 > r2`)

Rejected without a numbered question. It is outside the standard's own
requirements (cited literature, §1); it would make displacement
data-dependent, which no static WF-051 stratum computation can check —
re-opening exactly the hole "salience → declared stratification"
closed; and the adopted calculus dropped even the *static* run-time
pre-order (§3). If a future frontend genuinely needs it, that is a v2+
research question, not a v1 delta.

### 4.5 O5 — priority provenance (the part worth keeping)

The one genuinely valuable thing in `<lrml:Override>` for a
traceability-first IR is not the semantics (structure has it) but the
**assertion trace**: *which provision* ordered these grounds. That is
source metadata, and the machinery exists or is already carded: every
`Rung` carries `bron` (schema-verified), so the ordering provision's
surface text rides the rung that encodes it today; the structured
`Bron` is the tracked row-3 delta; and statute-article annotations on
the wire are candidate **C1** (`LEGALRULEML-AUDIT.md` §7, seeded as
`backlog/new/20260825-bron-wire.md`). A samenloop article that orders
two grounds compiles to an ordered ladder whose rungs cite that
article — provenance lands on row-3 machinery, and no new relation
object is needed.

## 5. The corpus test — does any law in scope demand it?

- The Jutterswet (v0–v4) contains **no samenloop provision**: grep over
  `corpus/jutterswet/wet-v*.md` for samenloop/priority phrasing finds
  none. Its priority phenomena — art. 4's nested "geldt niet / geldt
  weer niet", art. 4bis's parallel grounds, art. 5/9bis's discretionary
  hardship — are all landed as ladder structure and anchored
  (`jutterswet-uitsluiting`, `-toelagegronden`, `anchor/jutterswet-hardheid`,
  `jutterswet-hardheidsaftrek-catala`).
- The samenloop pattern itself ("bij samenloop van aanspraken geldt de
  hoogste") was analysed in `PHASE4-DEFAULTS.md` §6 and is a **priority
  declaration, not a conflict reader**: an ordered ladder, or a rung
  whose justification compares the grounds. That analysis is currently
  prose; §9 Q4 offers to convert it into a machine-checked witness.
- Both frontends compile priority structurally before the IR sees it
  (RegelSpraak: no surface; Catala: D-Label normalisation). A free
  relation on the wire would be dead surface from every producer that
  exists or is planned.

## 6. Interaction analysis (the two subsystems the card names)

**With conflict detection (§9.5).** The channel's design premise is
that over-determination is *reported*, never repaired: the conflict
fact is not rule-readable (RS ch. 7.2, decided 2026-08-20 — reopening
is additive), and equal decisions are no conflict (the tracked
deviation). A free superiority pair is a repair mechanism: it would
convert reported conflicts into silent resolutions, inverting the
decided direction. Where LegalRuleML uses Override to *prevent* the
conflict, uchar uses rung order — same effect, already expressible,
one encoding.

**With stratification (WF-045/046/051).** A *static* relation is
verifier-checkable but adds nothing: the ladder's displacement edges
already make every priority decision a static negative edge
(`dep_forms`, shared verbatim between the verifier and `ref/`'s
strata). A *partially ordered group* would need a new edge class
(which member's reads displace whose?) and a new reconciliation
fixpoint; a *derivable* relation (O4) cannot be statically stratified
at all. Every option other than "do not build" spends verifier and
executor complexity to re-house a decision the strata already carry.

## 7. Recommendation

**Do not build** the free superiority relation (O1) or strength tags
(O2) for v1; ratify the structural encoding as the decided boundary of
the standing LegalRuleML test on this point, with two positive
companions:

1. record O3 (the derived export mapping) as the on-demand answer
   should LegalRuleML interchange ever become a goal — a direction in
   this proposal, no artifact now;
2. leave priority *provenance* to the row-3/C1 source machinery (O5) —
   already carded, no new surface here;

and one optional corpus strengthening: a hand-written samenloop witness
(an own-authored Jutterswet-style samenloopartikel compiled as an
ordered ladder, golden through `admit.sh`) that turns
`PHASE4-DEFAULTS.md` §6's argument into a machine-checked case — §9 Q4.

The consequence for the audit's consequence ledger: candidate C2
closes as a **scope confirmation** (the audit's own framing: "a scope
confirmation, not new machinery"), and the DEELS/NIET verdicts of
cluster 1 stand as documented, decided boundaries — uchar expresses
LegalRuleML's defeasibility *content* and declines its *reification*,
on the record.

## 8. Migration sketch under the recommendation

Near-zero, by design:

- **No normative edit is required.** RS ch. 7.4's sentence already
  states the boundary; `IR-SPEC.md` ch. 6.1 and the WF index are
  untouched; schema, verifier, executors, corpus untouched.
- **The decision is recorded** where uchar records decisions: the
  maintainer's serial `CONSTRUCTS.md` row-8 note at landing (one
  sentence naming this proposal and the Q1/Q2 answers), plus decision
  lines in this file's §9 — the `PHASE5-CATALA.md` pattern.
- If Q4 is answered yes, that is one ordinary corpus card (new golden
  through `admit.sh`, gates 2–8), independent of the rest.
- If the maintainer overrules toward O1/O2, §4.1/§4.2 carry the honest
  cost sketch; the first artifact would be a spec-first design card in
  the 5b kernel style (`PHASE5B-KERNEL.md`), not a direct build.

## 9. Ratification questions for the maintainer

1. **The free superiority relation (O1).**
   **A:** do not build — the structural encoding (rungs, nested groups,
   declared-unordered) is the v1 priority mechanism, and cross-target
   override remains inexpressible by design;
   **B:** build the wire object (extension field + WF rules + RS ch. 7
   partial-order semantics + both executors, per the §4.1 cost sketch).
   *Recommendation: A.* Consequence of A: `CONSTRUCTS.md` row 8's
   "refinement" framing becomes a decided non-goal for v1 (recorded per
   §8); the audit's cluster-1 DEELS verdicts stand as boundaries, and
   any future frontend whose source carries free override assertions
   must compile them into ladder structure or refuse loudly.
   Consequence of B: the §4.1 cost lands on the roadmap ahead of any
   demand, with no producing frontend; the declared-unordered group
   acquires a partially-ordered sibling whose semantics must be
   designed from scratch.

2. **Strength tags (O2).**
   **A:** do not build — strict/defeasible/defeater remain structural
   readings (§4.2), never wire labels;
   **B:** add a redundant-but-checked strength enum (new WF rules
   enforcing tag/structure agreement).
   *Recommendation: A.* Consequence of A: LegalRuleML's strength
   *vocabulary* stays unexpressible as metadata — the audit's NIET on
   strength tagging becomes a decided deviation, folded into the same
   row-8 note. Consequence of B: two synchronised encodings of one
   fact, verifier rules whose only job is keeping them equal, and no
   evaluation change either way.

3. **The export mapping (O3).**
   **A:** record the structure→LegalRuleML mapping direction in this
   proposal only, building nothing until an interchange demand exists;
   **B:** specify the mapping table normatively now (a protected-path
   spec addition with its own card).
   *Recommendation: A.* Consequence of A: zero cost now; if interchange
   arrives, the mapping card starts from §4.3. Consequence of B: spec
   surface for a consumer that does not exist, against the discipline
   that every spec sentence needs a machine-checkable artifact.

4. **The samenloop witness (the optional corpus companion).**
   **A:** admit one own-authored samenloop golden — a
   Jutterswet-flavoured article ordering two grounds ("bij samenloop
   geldt de hoogste"), compiled as an ordered ladder (or a
   comparison-justified rung), through `admit.sh` under gates 2–8 — as
   a small follow-up card;
   **B:** leave `PHASE4-DEFAULTS.md` §6's samenloop analysis as prose.
   *Recommendation: A, at low priority.* Consequence of A: the
   load-bearing claim behind declining O1 — "samenloop is a priority
   declaration, hence an ordered ladder" — gains a machine-checked
   witness, and the corpus gains its first statute-shaped ordered
   *competition* (the existing chain anchors order exceptions, not
   competing entitlements). Consequence of B: the claim stays argued,
   not witnessed; nothing else changes.

5. **Where the decision is recorded.**
   **A:** decision lines in this file plus the maintainer's serial
   one-sentence `CONSTRUCTS.md` row-8 note (the smallest change; RS
   ch. 7.4's existing sentence already carries the normative boundary);
   **B:** additionally upgrade RS ch. 7.4's "recorded refinements, not
   v1-design scope" to name this decision explicitly (one sentence, a
   protected-path proposal branch).
   *Recommendation: A.* Consequence of A: no protected-path traffic;
   the normative text is already accurate. Consequence of B: one more
   protected-path merge for a sentence whose content RS ch. 7.4
   already states — justified only if the maintainer wants the C2
   decision citable from inside the normative document itself.

## 10. Decisions (2026-08-25)

The maintainer answered all five §9 questions with option **A** (numbered
ratification round, 2026-08-25):

1. **The free superiority relation: not built.** The structural encoding
   (rungs, nested groups, declared-unordered) is the v1 priority
   mechanism; cross-target override remains inexpressible by design. A
   future frontend whose source carries free override assertions must
   compile them into ladder structure or refuse loudly.
2. **Strength tags: not built.** Strict/defeasible/defeater remain
   structural readings (§4.2), never wire labels; the audit's NIET on
   strength tagging is a decided deviation.
3. **Export mapping: direction only.** §4.3 records the
   structure→LegalRuleML direction; nothing is specified normatively
   until an interchange demand exists.
4. **The samenloop witness: carded, low priority.** One own-authored
   Jutterswet samenloop golden as an ordered ladder —
   `backlog` card 20260825-samenloop-getuige.
5. **Recording: this section plus the serial `CONSTRUCTS.md` row-8
   note.** RS ch. 7.4's existing sentence already carries the normative
   boundary; no protected-path addition.
