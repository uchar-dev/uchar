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

Result: rij 36 is kernel, end-to-end. Spec-first: IR-SPEC ch. 2.4
(`SumType` gehost door `domeinen`, WF-060/061 incl. de
functor-charset-flank) en ch. 4.7 (`Construct`/`Match`/`BinderRef`,
WF-062/063/064; `VariabeleRef` onaangeroerd), RUNTIME-SEMANTICS ch. 9
(constructorwaarden; totale match met leeg-propagatie; structurele én
nominale gelijkheid met beide-richtingen-onwaar bij leeg; defaults
ongewijzigd; temporale sum-dracht als recorded flank), WF-index
+5 rijen. Daarna de keten: schema's additief (datatype- en expr-oneOf,
case-waarde-unie), verifier met WF-060..064 en veertien
wf06x-negatiefs (rood-eerst getoond: eerst geweigerd zónder het juiste
nummer, daarna elk voor exact zijn eigen regel), het anker
anchor/jutterswet-beschikkingsom (vier cases met absolute waarden:
1275.50/0.00/12.00/leeg; de unaire bouw én leeg-argument-propagatie
via de herbeschikking-regel; de Tabel-38-hoek — leeg maakt = én ≠
onwaar — gepind in case4, dat als reden-dragende no-op-survivor
staat; gates 5/6/7 eerst rood getoond op de build), ref
(Ctor-waarde, locals/BinderRef, WV-9-blokken in test_ref.ml —
testbestand gewijzigd, kaart-gesanctioneerd en hier gemeld), clingo
(grondtermen, per-arm-lowering zonder NAF, positieve Ongelijk,
payload-schaling naar de payload-declaratie, recursieve termparser +
constructor-verwachtingsarm in model.ml; ASP-TRANSLATION S14 nieuw,
inventaris hernummerd naar S15 en terminaal gehouden), en de plugin
(alle Catala-enums → SumType; exhaustiviteitsvouw plugin-side —
scopelang garandeert geen totaliteit; 59 checks; tweede Catala-golden
jutterswet-penningbeschikking-catala, byte-identiek geregenereerd).
Lensronde (2 opus): één major beide lenzen — de unaire Construct was
bouwzijdig ongetuigd met een onwaar dekkingscommentaar — plus de
onterechte "catala interpret als derde oordeel"-claim in de
rij-36-stempel, vier verweesde S14-verwijzingen, en de
fence-/subclausule-getuigen; alles verwerkt (herbeschikking-regel,
stempel herschreven, S15-inventaris aangevuld met de sum-parameter- en
genest-sum-flanken, wf063-duplicate-arm en wf061-undeclared-domain
toegevoegd). Alle negen gates groen: 63 ir.json round-trip, 103
negatiefs elk voor hun eigen regel, 156 cases, 142 dubbel
gerepliceerd (ref ≡ clingo), 125 no-op-refuters + 17 survivors, 14
rood, 59 plugin-checks, 2 Catala-goldens byte-identiek. Bewust
gelaten: temporale sum-dracht, sum-getypeerde parameter en
genest-sum-payloadwaarden zijn ref-toegankelijke maar clingo-luide
flanken zonder corpus-case (S15-inventaris); Construct-argumenten
buiten literal/eigen-attribuut en sum-Construct in
vergelijkingspositie met gebonden payload blijven gerichte faults; de
Catala-tutorial blijft wachten op rij 10 + structs/subscopes (5c). De
toolchain-herpin volgt direct na de merge; de pin-SHA-stempel in
CONSTRUCTS komt met de folds-kaart.
