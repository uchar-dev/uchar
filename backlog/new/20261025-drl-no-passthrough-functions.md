# drl-no-passthrough-functions: de DRL-tekst maakt geen functie die alleen doorgeeft of niets toetst

*Voorstel van de werkbank (2026-10-25), op besluit van de eigenaar ("optie 1"). Niet gecommit tot de bouw. Zelfde
bestand als `20261025-drl-function-names-from-rule`; die kaart komt na deze (`WAY-OF-WORKING.md` §1). Vervolg in de
toolchain: `backlog/new/20261025-drl-readable-functions-pin.md`.*

## Aanleiding

Volgens de eigenaar is de DRL-tekst bedoeld als vorm van de verlaging die een Java-engineer kan lezen. Sinds kaart
`drl-teksttak-op-truth-maintenance` (`927e3821`, 2026-10-13) is elke toets en elke waarde van het plan een functie in
het Java-dialect met de RegelSpraak-zin erboven. Dat volgt uit twee besluiten van de eigenaar: besluit 5
(2026-10-12: runtime-aanroepen met de bronzin, geen MVEL-operatoren) en normvraag 1 van Fable-advies 2 (2026-10-13:
"altijd een functie", omdat MVEL geen `try`/`throw` kent). Het voorbeeld bij die vraag had één functie per
RegelSpraak-zin. De weergave maakt daarnaast een functie voor elke knoop met eigen statements (`DrlRendering`, javadoc
r. 54 e.v.), ook waar die functie alleen doorgeeft of niets toetst.

Meting op v2/herleving-ww (regels `ece012e`, uchar `229c5141`, toolchain `ec2b6bb0`): 339 regels en 562 functies.

- 115 functies `rN_toets…` doen alleen `return rN_samengesteld…(…);`: de toets van een regel waarvan de voorwaarde
  samengesteld is (`test`, r. 678, rol `CONDITION`/`VORM` op r. 686, en `compound`, r. 819). Beide dragen dezelfde zin.
- 19 samengestelde voorwaarden hebben nul leden, kwantor `ALLE`, alle in defaults-varianten `default N`; commentaar
  `// altijd`. Zij geven `ComputeHelpers.endVerdict(ALLE, 0, 0, 0)`, en dat is `true`; de regel roept ze aan als
  `eval( rN_toets0() )`. Dat zijn 38 functies (toets plus samengesteld).
- 17 samengestelde voorwaarden hebben één lid met kwantor `DE`.
- Bij nul leden declareert `compound` `Boolean vast` zonder het te gebruiken, en `raak` alleen om de vaste 0 door te
  geven.

Voorbeeld, `defaults A:Eindigingsgrond.datum aanvang eindigingsgrond recht op uitkering: default 1`:

```java
// altijd
function boolean r122_toets0() {
    return r122_samengesteld1();
}

// altijd
function boolean r122_samengesteld1() {
    int raak = 0;
    Boolean vast;
    return ComputeHelpers.endVerdict(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, raak, 0);
}
```

Goal: de DRL-weergave schrijft een functie alleen waar die iets toetst of berekent; dezelfde termboom, dezelfde
semantiek, minder functies.

- Een toets waarvan de voorwaarde samengesteld is, is één functie: de toets draagt het lichaam van de samengestelde
  voorwaarde. Bij de rollen met een `try` (`WITHOUT_FAULT`, `FAULT`, `DEPOSIT`) komt dat lichaam binnen de `try`, of het
  `Result:` zegt waarom daar twee functies blijven.
- Een samengestelde voorwaarde zonder leden waarvan het oordeel `true` is (`ALLE`, `DE`): geen functie en geen `eval`.
  Bij een andere kwantor zonder leden kiest de bouwer de vorm en zegt waarom.
- Een samengestelde voorwaarde met één lid wordt dat lid, waar het oordeel van de kwantor over één lid gelijk is aan
  dat van het lid (volgens `established` en `endVerdict`: `ALLE`, `DE`, `TEN_MINSTE 1`; de andere per kwantor
  nagaan). Staat zij in een andere samengestelde voorwaarde, dan verdwijnt haar aanroep uit het lichaam van de ouder.
- Een local (`raak`, `vast`) staat alleen waar hij gelezen wordt.
- Er gaat geen RegelSpraak-zin verloren: heeft een functie die opgaat in een andere een andere zin dan die andere, dan
  blijft haar zin als commentaarregel op haar plaats in het lichaam.

Acceptance:
- (a) Een getuige over het corpus, voor elke golden die de backend als DRL-tekst draagt: geen functie waarvan het
  lichaam alleen de aanroep van een andere functie van dezelfde regel teruggeeft, en geen functie voor een
  samengestelde voorwaarde zonder leden met oordeel `true`.
- (b) Rood eerst, gericht: een toets over een samengestelde voorwaarde in elke rol (`CONDITION`, `VORM`,
  `WITHOUT_FAULT`, `FAULT`, `DEPOSIT`); een default zonder voorwaarde; een samengestelde voorwaarde met één lid voor
  elke kwantor; zo'n voorwaarde genest in een andere; een zin die als commentaarregel blijft staan.
- (c) Geen gedragsverandering: `BranchDumpsCorpusWitnessTest` ongewijzigd groen, `planvorm` en `modelvorm` gelijk aan
  hun nulmeting (het plan verandert niet), gate 12 met dezelfde statusregel per golden en dezelfde `EXPECTED_PLAYED`.
- (d) In het `Result:` de meting op v2/herleving-ww vóór en na: het aantal functies (verwacht rond 562 − 115 − 19), de
  tellingen van (a), en het stuk van `r122` na de wijziging.
- (e) De vloeren van `DrlPiecesCorpusWitnessTest` (functies, commentaar, aanroepen, gemeten 2026-10-24) en de
  verwachte functies in `DrlPiecesWitnessTest` en `FormsPlan` veranderen, omdat deze kaart het aantal functies
  verandert; het `Result:` noemt elke aangepaste verwachting.
- (f) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/DrlRendering.java`; de DRL-getuigen in
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/` en `.../transpiler/tms/`; de sectie "The DRL-text branch"
van `backends/drools/README.md`; deze kaart.

Out of scope:
- De namen van de functies (kaart `20261025-drl-function-names-from-rule`).
- Een term die niet kan falen inline zetten ("optie 2", op 2026-10-13 afgewezen), zoals een constante
  (`return Boolean.TRUE;`) of één `collapse` van een gelezen waarde in een `try` (31 op v2).
- De analyse en het plan (`TmsTranspiler`, `plan/`), de exec-modelweergave, `ComputeHelpers`.
- MVEL-operatoren; besluit 5 blijft staan.
