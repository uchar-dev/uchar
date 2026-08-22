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
- Nog te doen (deze kaart): het `anchor/jutterswet-vondstdossier`-golden
  mét case1/2/3 door het corpus, ref + clingo (case-formaat `{"struct":
  …}`, functorterm-encodering), plugin (`TStruct`/`EStruct`/
  `EStructAccess`), herpin, twee lenzen — buiten opdracht 1.
