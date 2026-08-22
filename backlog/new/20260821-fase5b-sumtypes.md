# Fase 5b, ontwerp 1 — sum types met payload en compile-time-totale match (rij 36)

Goal: `spec/proposals/PHASE5B-KERNEL.md` §2 end-to-end, onder de
geratificeerde besluiten (vraag 1: één payload per constructor). Het
`SumType`-datatype gehost door `domeinen`; de expr-knopen `Construct`,
`Match` en `BinderRef` (nieuw — `VariabeleRef` blijft onaangeroerd
OPEN); de WF-regels van §2.4 (nummers vanaf WF-060, definitieve
toewijzing hier) met verifier-checks en negatiefs; RUNTIME-SEMANTICS
ch. 9 met worked examples (leeg-scrutinee propageert; structurele én
nominale gelijkheid; `Construct` met leeg argument propageert) die als
`WV-9.*`-blokken in `ref/test/test_ref.ml` landen; de
case-formaat-uitbreiding (`$defs/value` constructor-variant, loaders
met fail-fast-crosschecks, WF-059/018-backstop verbreed); beide
executors — ref per §2.6 en clingo met constructortermen,
per-arm-lowering zonder NAF, positieve `Ongelijk`-encodering
(beide-gebonden-én-verschillend), payload-geschaald naar de
payload-declaratie, `model.ml` met recursieve termparser en
constructor-verwachtingsarm buiten `scaled_expect` om; een nieuwe
ASP-TRANSLATION-sectie (inventaris blijft terminaal, hernummerd) met
Witnesses; Jutterswet-ankers per §2.8 (leeg-scrutinee-hoek als
reden-dragende no-op-survivor); CONSTRUCTS rij-36-deltastempel; en
daarná de plugin-vertaling per §2.7 (álle Catala-enums → `SumType`;
exhaustiviteitsvouw plugin-side gerepliceerd; `domeinen` niet langer
hard-wired leeg) met suite-fixtures en — indien een golden met
`bron.catala_en` bijkomt — byte-identieke regeneratie.

Acceptance: alle negen gates groen met de nieuwe ankers, negatiefs en
fixtures erin; rood-eerst getoond voor elke nieuwe WF-check en gate-
uitbreiding; daarna herpin in de toolchain (SPEC_REV-bump,
`make uchar-conformance UCHAR_DIR=…` 11/11 byte-identiek); twee
opus-lenzen met bevindingen gefixt vóór de merge.

May touch: spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md,
spec/ASP-TRANSLATION.md, spec/CONSTRUCTS.md, spec/schema/,
spec/verifier/, ref/ (incl. test — melden), backends/clingo/,
frontends/catala/, corpus/ (golden incl. anchor, negatief, rood,
jutterswet-bronmateriaal), scripts/check-noop.sh (survivors),
backlog/. Toolchain: uitsluitend de herpin-commit.

Out of scope: alles van ontwerp 2 (lijsten, folds, `FunctionDef`,
`Invoke`, `Lambda`, `IfThenElse` — behalve dat `BinderRef` en `locals`
zo landen dat ontwerp 2 erop voortbouwt); structs/subscopes (5c);
elke wijziging aan `Enumeratie` of `VariabeleRef`.
