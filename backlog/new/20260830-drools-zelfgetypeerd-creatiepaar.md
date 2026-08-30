# Drools: het zelfgetypeerde creatiepaar (+ de Phase-D-consistentie)

Goal: Twee vormen die de objectcreatie-kaart (2026-08-30) na eerlijke
triage reden-dragend heeft uitgesloten — de uitsplitsingsclausule van die
kaart, hier opgehaald. Beide zijn nu LUIDE weigeringen die gate 12 telt;
geen van beide is een waardedivergentie.

## 1. Het zelfgetypeerde rollenpaar bij objectcreatie (de hoofdvorm)

`IrLoader.objectCreatie` (r360-366) bouwt de `ResolvedObjectCreatie`
ZONDER de symbolische `verbindendeRol` die op de draad staat — het record
(`frontends/regelspraak/.../ir/ResolvedObjectCreatie.java` r20-21) draagt
dat veld niet, en de autoriteitsparagraaf van de loader (r55-64) archiveert
zo'n binding als "emitter-afgeleid, de transpiler leidt hem opnieuw af uit
het model". Voor een ZELFGETYPEERD paar valt daar niets af te leiden:
`RegelSpraakModel.verbindendeRol` telt twee gelijkgetypeerde zusterrollen,
komt op n=2 en geeft null — waarna `ExecModelTranspiler.objectCreatie`
terecht weigert ("zelfgetypeerd rollenpaar: uit twee gelijkgetypeerde
rollen leidt de bean geen referentieveld af").

ref gaat precies andersom (`ref/lib/eval.ml` r1517): de expliciete binding
WINT van de type-paar-afleiding en is "the only route for a self-typed
pair". De IR draagt hem ook echt — alle vier de getroffen artefacten
hebben `verbindendeRol = {opvolging, opvolger}`. De backend gooit dus weg
wat de scheidsrechter als enige gebruikt.

Sluiten vergt een ONTWERPBESLUIT in `FactModelGenerator`, geen doorgeef-
patch: `leden()` leidt referentievelden per TYPEPAAR af en slaat
`ot == doelwit` over; `zelfpaarRol` dekt alleen de wederkerige vorm
(§13.3.9.2, één rol die zijn eigen tegenhanger is). Een GEWOON zelfpaar
heeft twee onderscheiden zusterrollen die allebei door hetzelfde type
gevuld worden (`voorganger` én `opvolger` op `Wachter`) — welk(e) veld(en)
de bean dan draagt, en hoe de by-pair-navigatie van de transpilers ertussen
kiest, is de open vraag. `leden()` wordt gedeeld met `ContractValidator`,
dus elk antwoord verschuift beide kanten tegelijk.

Wat het ontgrendelt:
- twee goldens: `corpus/golden/anchor/jutterswet-wachtersketen` en
  `corpus/golden/synth2-budgetgrens-onder`. **LET OP** — die dragen ELK
  nog een TWEEDE, onafhankelijke uitsluiting: `transpiler SKIP: regel:
  opvolgerrang van een wachter — regel (aggregatie buiten Phase-C scope)`.
  De creatiefix alleen zet ze dus NIET op PLAYED; beide moeten landen, of
  de kaart levert de creatievorm met een eerlijk gebleven exclusie-regel.
- twee rood-fault-pins: `corpus/rood/finite-chain-over-budget` (het
  generatiebudget wordt nooit bereikt) en `corpus/rood/runaway-recursion`
  (de weggelopen recursie evenmin) — nu statische EXCLUDE-regels in
  `scripts/check-rood.sh` (r136, r141). Ze horen dan hun échte uitkomst te
  tonen: een FAULT met gepind `expectedFault.drools`-fragment, of een
  eerlijk hergelijste reden.

## 2. De Phase-D-consistentie (de tweede uitgesplitste vorm)

`corpus/golden/anchor/jutterswet-penninguitgifte` draagt sinds
2026-08-30 een uitvoerbare objectcreatie (twee creaties, waarvan één
toekenningloos), maar blijft uitgesloten op
`transpiler SKIP: consistentie: penningcontrole — consistentie (schending
buiten Phase-D scope)`. Het gaat om twee assertievormen in één golden:
- `penningcontrole`: een `Kenmerk`-assertie ("elke Penning is
  uitgereikt"); de SCHENDING is de genegeerde kenmerklezing — een
  kenmerk-negatie in de LHS;
- `houderscontrole`: een AANTAL-aggregatie over de `houder`-rol
  vergeleken met 1 ("elke Penning heeft een houder") — een
  aggregatie-assertie.
De consistentie-lowering is het enige wat deze golden nog van PLAYED
scheidt.

Daarnaast hoort hier de rood-case `corpus/rood/ambiguous-backlink`: ref en
clingo FAULTen daar bij evaluatie ("the created object's back-link is
ambiguous"), Drools WEIGERT bij lowering ("geen uniek verbindend
feittype") en staat daarom als EXCLUDE in `scripts/check-rood.sh` (r129).
Dezelfde ontwerpvraag als hierboven, aan de rode kant: hoort een ch.-5.5-
terugkoppelingsbesluit bij lowering (een eerlijke weigering) of bij fire
(een gepinde fault)? Beantwoord die vraag één keer, voor beide zijden.

Acceptance:
- Rood-eerst: de huidige EXCLUDE-regels en gate-tellingen eerst getoond
  (nulmeting bij het schrijven van deze kaart: gate 12 = played 92 /
  auto-excluded 44 / value-divergence 1 / no-case 1, JUnit 82; gate 8 =
  3 gepind / 19 gelijst), per werkstuk een falende toestand vóór en de
  groene erna.
- Elke verwachting EERST door ref (`_build/default/ref/bin/runner.exe
  <ir> <case>`); nooit een ongeverifieerde waarde pinnen.
- Het FactModelGenerator-besluit staat expliciet in de code, met de
  afgewezen alternatieven benoemd — een zelfpaarveld dat "onder-
  geïmpliceerd, nooit over-geïmpliceerd" blijft, zoals de bestaande
  afleiding zegt.
- `bash scripts/check-drools.sh` → PASS; elk getroffen artefact speelt
  tegen zijn ONGEWIJZIGDE pins óf staat reden-dragend gelijst (de
  aggregatie-SKIP op de twee goldens expliciet benoemd, niet stilzwijgend
  meegeteld).
- `sh scripts/check-rood.sh` → PASS; de drie rood-cases hertriagerd — een
  échte FAULT pint zijn `expectedFault.drools`-fragment (corpus/rood:
  uitsluitend dát veld), anders een eerlijk hergelijste EXCLUDE-reden.
- Nieuwe witnesses in `backends/drools/src/test` pinnen de zelfpaar-
  koppeling (beide uiteinden, ch. 5.5) en de twee consistentie-
  assertievormen; de bestaande weigerings-witnesses in
  `ObjectCreatieWitnessTest` verhuizen mee of vervallen mét reden.
- Alle twaalf gates groen; gates 5/6 met ongewijzigde getallen.

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `scripts/check-drools.sh` en
`scripts/check-rood.sh` (uitsluitend entries/lijsten),
`corpus/rood/*/case*.json` uitsluitend het `expectedFault.drools`-veld,
`toka/cases/parity.manifest`, `backlog/`.

Out of scope: ref/clingo/verifier/spec en alle corpusverwachtingen en
-pins, toka-ir's (byte-identiek), de v1-loadervormen (ListType/StructType/
SumType/Let/functionDefs/TemporalRead), de terugveld-degradatie (een
meervoudige subjectzijdige én een meervoudige gecreëerde rol SPELEN nu —
het schrijfbare uiteinde draagt ch. 5.5's koppeling, het meervoudige wordt
overgeslagen — beide gepind in `ObjectCreatieWitnessTest`; alleen het paar
met TWEE meervoudige rollen blijft een reden-dragende weigering ("beide
rollen zijn meervoudig", buiten scope van deze kaart)),
gate-scriptgedrag buiten de lijsten.
