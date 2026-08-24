# De tariefrecht-divergentiegetuige

Goal: de flank die fase 5d bewust buiten het paar hield: ongeordende,
verschillend besluitende uitzonderingen — Catala aborteert daar met
ConflictError, ons model deponeert een observeerbaar, niet-fataal
§9.5-conflictfeit (de getrackte rij-8-deviatie). Inventariseer wat er
al ligt (jutterswet-toelagegronden pint de ongeordende groep;
jutterswet-tariefrecht-catala is de Catala-kant) en dicht wat
ontbreekt: de vergelijkende getuige — het conflict-geval door beide
IR-vormen en beide executors gepind, en een aanvulling op
spec/proposals/PHASE5D-DIFF.md die de daar genoemde grens sluit
(inclusief de bevinding, als die standhoudt, dat de v0-platte vorm
deze semantiek niet kan uitdrukken — dat is dan zelf een
documentaire vaststelling, geen fout).

Acceptance: alle negen gates groen; rood-eerst per nieuwe case; de
PHASE5D-DIFF-aanvulling en de CONSTRUCTS-rij-8-notitie kloppen met
wat empirisch is aangetoond; twee opus-lenzen verwerkt vóór de merge.

May touch: corpus/golden/ (nieuw of bestaand tariefrecht-materiaal),
spec/proposals/PHASE5D-DIFF.md, spec/CONSTRUCTS.md (UITSLUITEND rij
8), scripts/check-noop.sh (survivor-lijst, alleen indien nodig),
backlog/. Out of scope: elke normatieve IR-SPEC/RS-wijziging;
frontends/; backends/.

## Voortgang (2026-08-24)

Inventaris. `jutterswet-tariefrecht-catala` pinde drie cases
(basistarief/grootgezin/vrijstelling) die stuk voor stuk **hooguit
één** sub-uitzondering raken — de ongeordende `Rung.excepties`-groep
(grootgezin 10 % / kleine beurs 0 %) zat wél in de IR maar bleef
onbeproefd; er was nog géén conflict-case. `anchor/jutterswet-toelagegronden`
pint de ongeordende top-level groep met het §9.5-conflictpad
(case3-conflict: visser+wachter → `attributeEmpty toelage` +
`inconsistent Jutter.toelage`). PHASE5D-DIFF §1/§6 hield `tariefrecht`
bewust buiten het penningrecht-paar en noemde de scherpe flank (twee
ongeordend-verschillend-besluitende uitzonderingen) "out of scope …
awaits a fragment that can carry it honestly".

Gedicht.
1. **Conflict-case** `case4-conflict.json` toegevoegd (overlap
   kindertal=4 ∧ inkomen=8000). Catala-abort zelf geverifieerd via
   `catala interpret` op een /tmp-#[test]-kopie (letterlijke
   ConflictError in §8 geciteerd). ONZE uitkomst, beide executors:
   `tarief` leeg, path-gesleuteld §9.5-feit `Penningplichtige.tarief/0`,
   en `aanslag` lóst nog op tot 0.00 (MAAL coalescet de leeg-tarief naar
   nul — niet-fataal, tegenover Catala's scope-abort). Rood-eerst:
   eerst gepind op de naïeve `tarief=0.1000`+`consistent`; beide
   executors wezen dat af.
2. **v0-platte-vorm empirisch getoetst** (scratch: twee vlakke
   `BEREKENING`-schrijvers op één niet-ladder-doel): verifier-clean,
   maar **ref divergeert** (geen fixpoint, ch. 5.7) terwijl **clingo
   stil multi-valued** (`tarief = [0;1000]`, `consistent`) — ref ≢
   clingo, in géén corpus-zone toelaatbaar. De `ongeordend`-laddergroep
   is dus de enige IR-vorm die deze over-determinatie met
   executor-overeenstemming uitdrukt, geen suiker over vlakke regels.
   Verfijnt §6's speculatie "silently last-writer-win".
3. **PHASE5D-DIFF §8** geschreven (sluit de §6-grens; §6-bullet
   vooruitverwezen) en de **CONSTRUCTS-rij-8-zin** toegevoegd.

`scripts/check-noop.sh` NIET aangeraakt — de conflict-case faalt correct
onder `--noop` (geen survivor). Geen normatieve wijziging; geen gedwongen
maintainer-vraag (één latente, niet-blokkerende observatie in §8.2 voor de
lens-ronde: moet de verifier twee verschillend-schrijvende vlakke doelen
vóór het differentieelvierkant weigeren?).

Alle negen gates groen in deze worktree:
- 1 self-contained: PASS
- 2 ir-schema: PASS (schema valid; 82 files validate + round-trip)
- 3 verifier: PASS (82 golden clean; 138 negatief rejected)
- 4 case-schema: PASS (215 case files validate)
- 5 oracle: PASS (198 cases replayed exactly)
- 6 clingo: PASS (198 cases replay exactly — ref = clingo)
- 7 noop: PASS (174 refute; 24 pinned survivors)
- 8 rood: PASS (17 red cases fault in both executors)
- 9 catala: PASS (frontend suite 120 checks; 6 catala goldens regenerate
  byte-identically — `tariefrecht-catala` ongewijzigd geregenereerd,
  alleen een case toegevoegd)

Rest vóór merge: twee opus-lenzen (acceptance) — buiten deze
subagent-beurt.

## Lensronde 1 (2026-08-24)

Bevinding 1 [MINOR/CONFIRMED] verwerkt: PHASE5D-DIFF §8.2 labelde clingo's
platte-vorm-uitkomst als "`clingo`: `PASS`", in visuele parallel met de
letterlijke verifier-/ref-verdicten ernaast — maar de runner emit dáár nooit
`PASS`. Geverifieerd tegen `backends/clingo/bin/clingo_runner.ml`: de runner
print `clingo: PASS (N expectations)` uitsluitend wanneer `Model.check` `[]`
teruggeeft (alle pins matchen); voor de multi-valued `J1.tarief = [0;1000]`
matcht geen enkele single-value-pin, dus rapporteert de runner `MISMATCH`
(en `CASE ERROR: … unknown rule` op een `inconsistent`-pin). Fix: het label
vervangen door bewoording die de runner volgt (consistent stabiel model,
geen §9.5-feit, maar `MISMATCH`/`CASE ERROR` op elke pin — geen pinbare
`PASS`); dit heft ook de milde spanning met "un-pinnable in every zone" twee
alinea's verderop op. Substantie ongewijzigd (ref ≢ clingo, stil multi-valued
consistent). Alleen `spec/proposals/PHASE5D-DIFF.md` geraakt; geen
scripts/check-noop.sh, geen artefactzone. Alle negen gates opnieuw groen
(1 self-contained · 2 ir-schema 82 · 3 verifier 82/138 · 4 case-schema 215
· 5 oracle 198 · 6 clingo 198 · 7 noop 174/24 · 8 rood 17 · 9 catala 120/6).

Result: de 5d-grens is gesloten. case4-conflict op het
tariefrecht-paar pint het geval waarop 'catala interpret' met
ConflictError aborteert: beide executors deponeren exact het
pad-gesleutelde §9.5-feit Penningplichtige.tarief/0, het tarief
blijft leeg, en de aanslag rekent dóór tot 0.00 — het observeerbare,
niet-fatale contrast met Catala's scope-abort, letterlijk geciteerd
in PHASE5D-DIFF §8. De scherpste vondst: de v0-platte vorm (twee
vlakke schrijvers op één niet-ladder-doel) is verifier-clean maar
un-pinnable — ref bereikt geen fixpoint, clingo levert stil een
multi-valued model zonder conflictfeit; ref ≢ clingo, in geen enkele
corpuszone toelaatbaar. De ongeordende laddergroep is daarmee
aantoonbaar de enige IR-vorm die deze over-determinatie met
executor-overeenstemming uitdrukt; §8.2 corrigeert de eerdere
"last-writer-win"-speculatie van §6. Rood-eerst met een opzettelijk
foute prioriteitslezing, door beide executors afgewezen. De lensronde
(één MINOR: een misleidend PASS-label bij de clingo-uitvoer in §8.2)
is verwerkt. Open, vastgelegd in §8.2 en bewust géén genummerde
vraag: of de verifier de un-pinnable platte vorm defensief moet
weigeren vóór het differentieel-vierkant — de v0-emitter produceert
hem nooit en gates 5-6 vangen hem; een toekomstige kaart kan het
hek alsnog optrekken.
