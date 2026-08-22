# Fase 5c, kaart 1 — structs als record-datatype

Goal: PHASE5C-TUTORIAL §2 end-to-end onder de geratificeerde besluiten
(vraag 1: A): het `StructType`-datatype (gehost door `domeinen`),
`StructLiteral`/`FieldAccess`, recordwaarden met no-hole-leegte en
structurele+nominale gelijkheid; WF-regels vanaf WF-071 met negatiefs;
de WF-065-verruiming (struct als lijst-element) en de
WF-067/069-verbredingen (`FieldAccess`/`StructLiteral` in de gesloten
body-whitelist); RUNTIME-SEMANTICS-hoofdstuk; case-formaat
`{"struct": {...}}`; beide executors (clingo: functorterm met vaste
declaratievolgorde, positionele projectie, schaal per veld);
Jutterswet-ankers (incl. list-of-struct met map-over-veldprojectie);
plugin (`TStruct`/`EStruct`/`EStructAccess`; `ctx_structs` doorgeven)
met fixtures.

Acceptance: alle negen gates groen met de nieuwe ankers, negatiefs en
fixtures; rood-eerst per stap; herpin (SPEC_REV-bump, conformance
11/11); twee opus-lenzen met bevindingen gefixt vóór de merge.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md,
spec/ASP-TRANSLATION.md, spec/CONSTRUCTS.md, spec/schema/,
spec/verifier/, ref/ (incl. test — melden), backends/clingo/,
frontends/catala/, corpus/, scripts/check-noop.sh (survivors),
backlog/. Toolchain: alleen de herpin-commit.

Out of scope: subscopes, geneste groepen, Let/decimals (kaarten 2–4);
struct-recursie; optionele velden (no-hole is de norm).

## Voortgang

- **Opdracht 1 (normtekst + schema's + verifier + negatiefs) — klaar.**
  IR-SPEC ch. 2.4 (`StructType`-rij + clausule, WF-071/072; orderability;
  Method-extensielijst), ch. 4.9 (`StructLiteral`/`FieldAccess`,
  WF-073/074), de WF-065/067/069-verbredingen tekstueel + WF-indexrijen.
  RUNTIME-SEMANTICS ch. 1.1 + nieuw ch. 11 (recordwaarden, no-hole,
  structurele+nominale gelijkheid met Tabel 38, de drie afnemers,
  ankerworked-examples, tijdlijn-flank). Schema's: `StructType` in de
  datatype-oneOf, `StructLiteral`/`FieldAccess` in de expr-oneOf,
  `{"struct": …}` in de case-value-unie. Verifier: `Struct_type`/
  `Struct_literal`/`Field_access` in ast/loader/checks; WF-071..074 met de
  gedeelde acycliciteitsgraaf (sum-payloads + struct-velden), WF-065-
  verruiming (struct als lijst-element), WF-067/069 (gesloten body's laten
  de record-knopen toe via de deny-lijst-default). Tien negatiefs
  (`wf071..wf074-*`), elk voor precies hun regel. Gates 1/2/3/4 groen
  (gate 3: 66 golden clean; 125 negatief). Rood-eerst getoond. Anker-ir
  uit het ontwerp gebouwd in /tmp en verifier-clean (positieve toets).
- **Opdracht 4 (clingo + ASP-sectie) — klaar.** Recordwaarden als
  grondtermen met vaste declaratievolgorde (`struct_value_term`, functor =
  genormaliseerde domeinnaam via de `ctor_functor`-fence, elk veld op zijn
  eigen schaal — `dossier(12050,3)`); `value_scale`/`element_scale_of`
  mappen struct → 0. `translate_case` emitteert struct-attributen (`val/3`)
  en struct-lijstelementen (`reify_list`); struct-TIJDLIJN → gerichte fault.
  `FieldAccess` als positionele projectie door unificatie
  (`project_field`/`struct_base`) — op de eigen struct-attribuutlezing én
  op een gebonden binder (de lelem-route voor MapList over `dossiers`).
  `StructLiteral` in waardepositie (`struct_literal_term`, het
  computed-Construct-patroon; no-hole-propagatie) en de struct-schrijf-tak
  in `translate_regel`. Struct-gelijkheid in conditiepositie: positief `==`
  en `!=` over gebonden termen (nooit `not`; Tabel-38-hoek voor leeg). De
  `GEVULD`-poort in `cond_atoms`. `model.ml`: struct-verwachtingen
  structureel via de recursieve termparser (`struct_expect`, één
  emissiebron). ASP-TRANSLATION §S16 (structs) nieuw vóór de terminale
  inventaris, die → S17 hernummerd; repo-breed de verwijzingen bijgewerkt
  (PLAN, IR-SPEC, CONSTRUCTS, rood-NOTEs, translate/model). Gates: `dune
  build backends/clingo` groen; check-clingo GROEN (154, ref ≡ clingo incl.
  de struct-verwachting en de Tabel-38-hoek); check-rood (14) en check-noop
  (134 + 20 survivors) groen.
- Nog te doen (deze kaart): plugin (`TStruct`/`EStruct`/`EStructAccess`;
  `ctx_structs` doorgeven), herpin, twee lenzen — buiten opdracht 4.
