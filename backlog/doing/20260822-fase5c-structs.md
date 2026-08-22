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
