# toka/ — the TOKA parity zone

This top-level zone is the **one place** in the repository that carries
**TOKA** material, by maintainer decision of 2026-08-27, **for feature-parity
testing** of uchar's own RegelSpraak frontend against the original toolchain
(gate 11). Every other zone stays TOKA-free (gate 1): the corpus is own work,
and TOKA is third-party material whose release is not yet confirmed in
writing.

## Provenance and status

The same text stands in the root `README.md` and in `CLAUDE.md`.

> **TOKA casus.** Source: *RegelSpraak specificatie* v2.3.0 (4-12-2025),
> Bijlage 1, "Treinen Op Korte Afstand" (TOKA, a fictitious act from the
> Belastingdienst's ALEF course), published by the Belastingdienst at
> https://wendbarewetsuitvoering.pleio.nl/page/view/291f5846-cdfc-42d9-9f31-a2158fe59128/specificaties-regelspraak.
> Rights notice of the source:
> "© 2025 Belastingdienst / Alle rechten voorbehouden".
> Status: the Belastingdienst indicated to the maintainer on 2026-10-13 that
> TOKA is meant to be releasable. It is probably under EUPL-1.2, like ALEF
> (https://github.com/belastingdienst/ALEF) and the repository
> MinBZK/regels.overheid.nl, which carries a 2022 version of the course
> casus. Written confirmation: not yet received.
> The licences of this repository (Apache-2.0, CC BY 4.0) do not cover this
> material.
>
> Files in this repository. Copied verbatim from the source: the 47
> definition fragments `toka/casus/toka-*.rgs`. Derived from them or own
> work: the IR dump `toka/casus/ir.json` and the 29 IR slices
> `toka/cases/scenario-*/ir.json` (the dump and 22 of the slices carry text
> of the fragments in their source snippets); the 29 case files
> `toka/cases/scenario-*/case.json`, with situations and expected values
> derived by hand; the parity manifest `toka/cases/parity.manifest`; the three
> tests under `toka/src/test/java/`, which quote lines of the fragments or
> names written in them and whose identifiers follow every rename of the
> frontend; and `toka/README.md`.

## What is here

- `casus/*.rgs` — the TOKA casus: a mechanical, verbatim split of the
  object/parameter/rule definitions of the worked example published in the
  RegelSpraak v2.3.0 specification. The TOKA data is explicitly fictitious
  ("volledig verzonnen", per the spec's own disclaimer) and relates to no real
  case.
- `casus/ir.json` — the pinned parity dump: the faithful v0 IR emitted **once
  with the original toolchain** at the frontend pin, minus the recorded
  divergences below. Gate 11 (`scripts/check-toka-parity.sh`) re-emits it
  through uchar's own frontend and requires byte-identity, so any *unrecorded*
  emitter divergence between toolchain and frontend is caught.
- `cases/<scenario>/{ir.json,case.json}` — the replay fixtures of gate 11's
  differential leg. Those `ir.json` are **slices** of the casus dump: they carry
  no sibling `.rgs`, so gate 11 re-emits none of them (it schema-checks and
  replays them instead). A slice therefore does not follow the casus dump by
  itself — when the emitter moves, the slices are moved by hand in the same
  card, or the zone carries two spellings of one construct. A slice carries
  beside its scenario whatever anchors its model (see Well-formedness).
- `src/test/java/**` — two of the three integration tests excluded at the
  frontend takeover (CasusTest, ModelTest), in their original form (only the
  fixture path and, where needed, the package were adjusted). They exercise
  the frontend end-to-end on the TOKA shapes. RuleCheckTest stayed behind:
  it imports the Drools transpiler, which is out of this zone's scope — it
  returns with a Drools-backend card. Beside them stands one own test,
  SemanticTokensCasusTest (2026-10-22, after the card `semantic-tokens`): the
  semantic tokens of the 47 fragments read as one document — full coverage,
  the text of every span, every reference linked to a declaration of its
  kind, and the spans `unresolved` pinned per name the fragments declare
  nowhere (six names, 24 spans), with floors. The frontend's own corpus
  witness cannot read the fragments, because gate 1 keeps `frontends/` free
  of TOKA material.

## The holdout

The full casus raises exactly **two** resolver ERRORs, both pinned by
`CasusTest` as the tolerated diagnostics: the §4.1 datum+datum casus-quirk in
`toka-23` (the `bevestigingsinterval`, a Datum-tijd parameter added to a
Datum-tijd, which typeringen §4.1 forbids), and the dimensional inconsistency
in `toka-45` (decision 16Y, 2026-08-29: the identity unit typing types the
product, so the written subtraction fails the §4 unit rule — uchar-v1
deliberately diverges from the toolchain here). Because `--ir` writes nothing
for a model with any error, the pinned dump is emitted from every fragment
**except** those two; the gate holds both out by name and fails if either
holdout ever goes stale.

## Well-formedness

Since 2026-09-25 (the card `20260925-toka-welgevormd.md` in `backlog/`) the
zone claims more than parity: every replay slice but one named holdout is a
well-formed program, verifier-clean under gate 3's verifier, and gate 11
checks it (its obligation 4). The casus dump stays outside that claim, on
purpose.

- **The slice rule: a slice carries beside its scenario whatever anchors its
  model.** The casus declares a timeline with a variable start, and such a
  timeline is well-formed only when a start-point determination anchors it
  (WF-016), at a member declared as a day-precision date on the object type
  the determination names (WF-032). The frontend writes that member itself,
  as the last attribute of its object type, when it resolves the
  determination. Slicing had cut both from 27 of the 29 slices, which each
  failed on WF-016 (26 on it alone, `toka-24` on WF-099 as well); with the
  determination only, WF-032 takes WF-016's place (measured). Those 27 now
  carry the casus's one determination and its member declaration, copied
  mechanically from `casus/ir.json`; the two start-point slices carried both
  already and did not move, and all 29 now carry one identical model (up to
  the `origin` of that member, which since 2026-10-15 stands only in the two
  slices that carry the rule it names; see the divergences below). The
  anchor's write rule stays with its own scenario: a slice needs its model
  anchored, not the anchor computed. That holds only while no member carries
  a timeline facet on the timeline with a variable start (measured: none in
  the casus and none in a slice); once one does, the write rule belongs in
  every slice that reads such a member. The replays in ref, clingo and Drools
  print byte for byte what they printed before.
- **The standing: 28 of the 29 slices are verifier-clean.** `toka-24` is held
  out by gate 11, pinned to the one report it draws (WF-099 at
  `/rules/rules/0/versions/0/value/base/minimum`): the casus writes a clamp's
  minimum as a literal with a unit over a base whose operands are declared
  without one, the one-sided unit pair that IR-SPEC ch. 4.1 leaves open as one
  decision for WF-023, WF-035, WF-036 and WF-099 together. That is a property
  of the casus and not of the emitter, which writes the unit the source
  writes; every executor replays the slice. The holdout fails when it goes
  stale in either direction: when the slice turns clean, or when it draws any
  other report.
- **The casus dump is not a well-formed program, and is not meant to be.**
  As one document it draws 14 reports, one per pointer and sentence: since
  the card `verifier-allocatie-keten` (the same day) WF-026 no longer
  repeats one sentence at one pointer, which made 18 lines before. Its
  scenarios each write the same targets in their own way, legitimate per
  scenario and not as one program (3 × WF-026, 1 × WF-079). Until
  2026-10-06 a dependency cycle through the object creation of the
  contingent and the fact creation of the passengers with a right to train
  miles made a fifteenth (1 × WF-045). It did not exist: the fact creation
  wrote the population of Natuurlijk persoon as a proxy, and the creation
  counts the passengers of the flight; the proxy went that day
  (RUNTIME-SEMANTICS ch. 6.1, golden `jutterswet-premiepot`). It also uses
  four attributes and one characteristic that it does not declare and the
  slices, cut in the scoping of the toolchain's own scenario tests, do (8
  × WF-034: seven references to those attributes and the one derived
  position their absence leaves untyped; 1 × WF-022, a rule writing that
  characteristic). The last report, WF-099, sits at the very rule
  `toka-24` carries. Its role is the parity object of the byte-identical
  re-emission, so gate 11 keeps it out of the well-formedness leg.

## Deliberate divergences from the toolchain emission

Gate 11 compares uchar's frontend against the files pinned here, not against a
live toolchain. So once uchar's emitter changes **on purpose**, the pin is
re-taken in the same card and the comparison becomes "frontend == the recorded
state" for that construct. Every such move is listed below, so the parity claim
stays honest about where it no longer proves toolchain agreement.

- **2026-09-18 — a Tekst literal's `waarde` is the parsed string.** Maintainer
  decision of that date, executed by the card
  `backlog/done/20260918-tekstlexeem-canonicalisatie.md`. The emitter used to
  write the source snippet *with* its own outer `"` delimiters; it now writes
  what stands between them. (The RegelSpraak string grammar has no escape
  sequence — a `"` always closes the string — so that strip is total, lossless
  and never unescapes anything.) 48 literal values moved: 12 in `casus/ir.json`
  and the same 12 in each of the three `cases/scenario-toka-46-woonregio-factor-*`
  slices, plus the 3 matching quote-carrying case inputs, which had to move with
  their literals because every executor compares Tekst **verbatim**. Nothing
  else in those seven files changed. The original toolchain still writes the
  snippet form, so for text literals this dump has stopped being a toolchain
  dump.
- **2026-09-19 — every surviving `Onopgelost` carries a WARNING.** Maintainer
  decision of that date, executed by the card
  `backlog/done/20260919-frontend-stille-markers.md`: a marker stays the honest
  wire form, but the author is told what stayed unresolved and why. The casus
  holds exactly one marker — a bullet whose written name binds neither a kenmerk
  nor a role of its subject — so one `Diagnostic` object was appended to
  `casus/ir.json`'s `diagnostics` list. Six added lines; nothing else in the
  file, and nothing in any `cases/` slice, moved. The original toolchain emits
  no such diagnostic, so for this one list the dump has stopped being a
  toolchain dump. The marker itself is byte-identical, which is the point: the
  parity of every structural node is untouched.
- **2026-09-23 — the zone is in the M1 shape of `uchar-ir-v1`.** Maintainer
  decision of that date (the zone stays until v1 is done, and gate 11 moves with
  it), executed by the M1 card (`20260923-m1-schema-en-container.md` in
  `backlog/`). The container moved and nothing else: the root nests
  `{format, model, rules, diagnostics}`, the declaration maps became lists, an
  object type's eight side-maps became member nodes, a domain became a `Domain`
  wrapper, and the `regels` stubs left the wire; every other node is
  byte-identical. The 29 `cases/` slices were migrated mechanically
  (`spec/proposals/v1-migratie/m1.py`); `casus/ir.json` was migrated the same way
  and then re-emitted by uchar's frontend, which reproduced the migrated file
  byte for byte (45 fragments, holdouts unchanged). The original toolchain still
  writes `regelspraak-ir-v0`, so for the container this dump has stopped being a
  toolchain dump: what gate 11's comparison proves is "frontend == the recorded
  state".
- **2026-09-24 — the zone is in the L2 shape of `uchar-ir-v1`.** Executed by the
  L2 card (`20260923-l2-referentielaag.md` in `backlog/`). The reference layer
  moved and nothing else: a reference to a declared thing now carries its
  symbolic form only. Gone from the wire are the `type` annotation of the five
  reference nodes (`AttribuutRef`, `ParameterRef`, `DimensieCel`, `BinderRef`,
  `VariabeleRef`), the written navigation beside an attribute reference's
  binding (`onderwerpBron`, `navigatieRol`, `navigatiePad`, `set`), a
  characteristic check's `navigatie`, the bean fields of a fact creation
  (`zetVeld`, `joinVeld`, and a step's `type`, `veld` and `viaOuderVeld`) and of
  a scoped uniqueness (`scopeVeld`), and the `Onbekend` datatype; the two role
  checks became one `RoleMembership` node. In `casus/ir.json` that removed 69
  reference types; 2 counts whose range every reader used to derive now carry it
  (`bronRol`, from the type pair), and their sources name the bound role by its
  declared name (the written spelling stays in `bron`); 1 role check became a
  `RoleMembership`; and 3 written navigations became own readings without a
  binding — two zero-hop self-references and one count source naming its own
  subject. No navigation binding was added. One
  derived position the frontend cannot type (`/rules/resolvedRegels/16/waarde`,
  from fragments toka-43/44 and not from the holdout toka-45: a `Binair` `MIN`
  whose left operand is an `AttribuutRef` naming no attribute declared on its
  object type, and whose right operand is a `Numeriek` parameter in months)
  carries `"type": null`, its one L2 spelling, where it carried an `Onbekend`.
  The 22 `cases/` slices that carry a reference layer were migrated
  mechanically (`spec/proposals/v1-migratie/m2.py`), the other 7 are
  byte-identical; `casus/ir.json` was migrated the same way and then
  re-emitted by uchar's frontend, which reproduced the migrated file byte for
  byte (45 fragments, holdouts unchanged). The original toolchain still writes
  the v0 reference layer, so for these nodes too the comparison proves
  "frontend == the recorded state".
- **2026-09-24 — the zone is in the L3 shape of `uchar-ir-v1`.** Executed by the
  L3 card (`20260924-l3-typesysteem.md` in `backlog/`). The type system moved
  and nothing else: a number or a percentage is a `Number` (a percentage one
  with the unit `%`, tagged `"presentation": "percentage"` at a declaration
  only, with its `sign` always written), the two date types are one `Date` with
  its `precision`, a unit is a `Unit` whose conversion factor is an exact
  fraction `{num, den}`, a unit system is a `UnitSystem` listing each unit once
  with its further `spellings` (the alias map is gone, and a spelling equal to
  the unit's abbreviation is none), a parameter carries `name` and, when
  time-dependent, a `timeline` facet, and a day kind is a `DayKind` node with
  its `plural` (the registry beside the list is gone). In `casus/ir.json` that
  rewrote 65 numbers, 3 of them percentages at a declaration, 16 dates (8 per
  precision), 54 unit nodes, 3 unit systems with 14 units, 19 spellings (1 map
  key and 18 aliases; 1 alias equal to its abbreviation dropped), 18 parameters
  (none time-dependent) and 1 day kind with its plural. 20 of the 54 unit nodes
  were copies under a second key of a system's maps (19 aliases, 1 map key), so
  34 remain. Of the 54 factors 36 were whole numbers and carried over 1; the
  other 18 were v0 doubles, each the product of per-step 16-digit roundings, and
  were reconstructed as the fraction with the smallest denominator within 10^-14
  relative of the double (`spec/proposals/v1-migratie/m3.py`): denominators 24
  (2), 100 (3), 1000 (3), 1440 (6), 86400 (2), 86400000 (2). Ten of them stand
  in the migrated file (24, 100, 1000, 86400 and 86400000 once each, 1440 five
  times); the other eight sat on alias copies, seven of which became spellings
  and one, equal to its abbreviation, nothing. The 29 `cases/` slices were
  migrated mechanically with the same script; `casus/ir.json` was migrated the
  same way and then re-emitted by uchar's frontend, which writes each factor
  from the fraction the unit declarations write (`1/N` along the declaration
  chain), not from a decimal, and it reproduced the migrated file byte for byte
  (45 fragments, holdouts unchanged): every reconstructed fraction is the
  declared one. The original toolchain still writes the v0 type system, so for
  these nodes too the comparison proves "frontend == the recorded state".
- **2026-09-24 — the zone is in the L4 shape of `uchar-ir-v1`.** Executed by the
  L4 card (`20260924-l4-regel-en-expressielaag.md` in `backlog/`). The rule layer
  and the expression layer moved and nothing else: the six named forms are
  `{node, name, versions}` (a version record without `node`, its `validity`
  always written), a rule version writes its kind as `initialisation` and
  `presentation`, a fact creation's target type left the wire and its steps and
  final hop became one `path`, the two uniqueness lists became one, a function
  node became an `Aggregation` or a `BuiltinCall`, a literal writes the typed
  `value` its type names, and a constant became a `BoundDay` or a
  `ContextValue`. In `casus/ir.json` that rewrote 20 rules (20 forms of one
  version each: 6 `BEREKENING`, 6 `GELIJKSTELLING`, 1 initialisation, 7
  characteristic attributions), 4 allocations, 1 object creation, 1 fact
  creation (a path of 2), 1 consistency, 5 aggregations, 10 builtin calls, 31
  literals (11 `number`, 8 `boolean`, 12 `text`) and 3 bound days; 16 null
  conditions and 13 null allocation fields were left out. **Two allocations
  left the dump:** the ones the frontend could not give a method, at
  `/rules/verdelingen/3` and `/rules/verdelingen/5` of the L3 dump, which the L4
  method set does not carry. The emitter leaves such an allocation out and
  appends a WARNING naming it, with line 0, after the model's own diagnostics,
  so the dump's `diagnostics` grew from 2 entries to 4. The 29 `cases/` slices
  carry no such allocation and were migrated mechanically
  (`spec/proposals/v1-migratie/m4.py`); `casus/ir.json` was re-emitted by uchar's
  frontend (45 fragments, holdouts unchanged) and is byte-identical to that
  script's migration of a copy of the L3 dump without those two allocations and
  with the two diagnostics appended. The original toolchain still writes the v0
  rule and expression layers, so for these nodes too the comparison proves
  "frontend == the recorded state".
- **2026-09-25 — the zone is in the L5 shape of `uchar-ir-v1`.** Executed by the
  L5 card (`20260924-l5-bronrecord-en-hernoeming.md` in `backlog/`). Two steps
  and nothing else: every source text moved into the source record (a `bron`
  string became `"source": {"snippet": …}` byte for byte, and the `line` of a
  consistency version went into that record), and every node, key and
  enumeration value that L1–L4 left in its v0 spelling took its IR-SPEC name,
  with an optional field that was null left out. In `casus/ir.json` that moved
  168 snippets and 1 line, renamed 203 nodes, 411 keys and the 9 lists under
  `rules`, respelled 157 enumeration values and left out 99 nulls; nothing was
  refused. The 29 `cases/` slices were migrated mechanically with the same
  script (`spec/proposals/v1-migratie/m5.py`): 244 snippets in 22 slices and 1
  line, 820 nodes, 1934 keys, 261 list keys, 1756 values and 330 nulls.
  `casus/ir.json` was migrated the same way and then re-emitted by uchar's
  frontend, which reproduced the migrated file byte for byte (45 fragments,
  holdouts unchanged): no divergence. The original toolchain still writes the
  v0 names, so for every renamed node the comparison proves "frontend == the
  recorded state".
- **2026-09-25 — a creation writes the role its sentence names, and a one-sided
  clamp unit draws a WARNING.** Executed by the card
  `20260925-frontend-emitterrestanten.md` in `backlog/`. Two moves in
  `casus/ir.json`, eleven added lines and nothing else. The casus's one object
  creation names its created object by a role, and the frontend now writes that
  role as the creation's `connectingRole` (five lines): the binding the type
  pair derives on this document as well (one connecting fact type, not
  self-typed), so nothing the creation links changes. The one slice that
  carries the creation, `cases/scenario-toka-28-vastgestelde-contingent`, got
  the same five lines, written with gate 2's serialiser, and stays
  verifier-clean. The casus's one clamp is the one-sided unit pair `toka-24` is
  held out for (see Well-formedness); the frontend now reports it in the
  verifier's WF-099 sentence as a WARNING, because as an ERROR it would write no
  document for the casus at all, so `diagnostics` grew from 4 entries to 5 (six
  lines). No slice carries that diagnostic, as with the 2026-09-19 entry, and
  the dump's verifier reports keep their rules and pointers (15, one per
  pointer and sentence; see Well-formedness). The original toolchain writes
  neither, so for these two nodes the comparison proves "frontend == the
  recorded state".
- **2026-10-15 — a declaration the frontend made carries its origin.**
  Executed by the card `20261015-herkomst-op-de-draad.md` in `backlog/`. The
  emitter writes the field `origin` (IR-SPEC ch. 2.13) on every declaration
  the frontend made itself. In `casus/ir.json` that is four nodes, 31 added
  lines and nothing else: the anchor `startpunt variabele periode van 3
  maanden` of `Vlucht`, from the rule `Startpunt variabele periode op datum
  van vertrek` (the first of the two determinations that write it), and the
  three rows of the decision table `Woonregio factor`, each from that table
  (the kind `decisionTable`). The slices moved by hand, written with gate 2's
  serialiser, wherever they carry the source of an origin: each
  `cases/scenario-toka-46-woonregio-factor-*` slice the three rows (24 added
  lines), `cases/scenario-toka-43-startpunt-vertrek` the anchor from its own
  determination, as in the casus, and
  `cases/scenario-toka-44-startpunt-min-reserveringstermijn` the anchor from
  the determination it carries, `Startpunt variabele periode op datum van
  vertrek min reserveringstermijn`, which is what the frontend writes for a
  document whose only determination is that one (7 added lines each). The
  other 24 slices carry the anchor without a rule that writes it (the slice
  rule, Well-formedness), so a `from` there would name a form the slice does
  not declare (WF-121 (b)); they carry no origin. Every slice stays
  verifier-clean, the dump's verifier report is unchanged, and no executor
  reads the field (gate 13), so the replays print what they printed before.
  The original toolchain writes no origin, so for these nodes the comparison
  proves "frontend == the recorded state".
- **2026-10-22 — a snippet stops before the period that closes the
  sentence.** A review finding of the distribution, fixed without a card on
  the branch of `20261022-check-admits-instance-cycle.md` in `backlog/`. The
  grammar lets the last bullet of a list take the sentence's period, so a
  compound condition ending in a bullet list carried it in its snippet. In
  `casus/ir.json` nine snippets lost their final `.` and nothing else changed:
  the day kind's condition and its nested group, the conditions of six rules,
  and a nested group in one of them. The slices moved with the same nine strings wherever they carry them:
  13 snippets in six slices (`scenario-toka-26-hoogseizoen`,
  `-41-duurzaamheidskorting-alternatief`, `-42-passagier-18-24` and the three
  `-46-woonregio-factor-*`). No executor computes with a snippet, so the replays
  print what they printed before. The original toolchain still writes the period, so
  for these snippets the comparison proves "frontend == the recorded state".
- The precedent runs the other way: `toka-45` (decision 16Y, 2026-08-29) is held
  **out** of the re-emission rather than re-pinned, because uchar's unit typing
  rejects the fragment as written. Divergence by holdout and divergence by
  re-pin are both recorded; a silent divergence is not on offer.

## Lifecycle

- The zone stays, with its git history (maintainer decision 2026-10-13). The
  repository is public since 2026-10-12, and its history is not rewritten.
- Gate 11 is a regular gate of the set since 2026-10-13.
- Every other zone stays TOKA-free: the TOKA tripwire in
  `scripts/check-self-contained.sh` keeps firing everywhere else. This zone is
  exempt from the self-containedness prose rules but must carry **no
  machine-local paths**.
