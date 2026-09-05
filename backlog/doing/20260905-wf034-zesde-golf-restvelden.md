# WF-034, zesde golf: de restvelden buiten de expressiewandeling

Goal: de vijfde golf en haar fixronde sloten drie naamvlakken buiten de
expressiewandeling (de vier beanvelden van een feitcreatie, het subject van een
objectcreatie, `anderTeFiller` van de nabije rolcheck) en lieten hun eigen
restlijst gemeten achter. Die restlijst staat sinds 2026-09-05 gedateerd in de
open-lijst van ch. 4.6 — deze kaart is de opdracht om hem af te lopen: per veld
kruisen wat te kruisen valt, of het veld met een gemeten argument definitief
naar de open-lijst schrijven. Route als de vorige golven: één arm per vlak, elk
rood-eerst met eigen negatief, arm-exact mutant-bewezen, nul over-afwijzing over
de vier zones.

## De meting (2026-09-05, 503 vier-zone-documenten)

Alles hieronder is geteld over golden + rood + negatief + pariteit op de dag van
deze kaart; een latere telling wijkt af en moet opnieuw.

| veld | dracht | wat er nu mee gebeurt | executeergedrag | oordeel |
| --- | --- | --- | --- | --- |
| **zijtabelsleutels** van een objecttype: de acht per-lid-maps (`attribuutMeervoud`, `attribuutDimensies`, `attribuutGranulariteit`, `attribuutTijdlijnAantal`, `attribuutTijdlijnNaam`, `kenmerkSoorten`, `kenmerkGranulariteit`, `kenmerkTijdlijnAantal`) | **916 sleutels**, 0 wezen vandaag | geen regel eist dat een sleutel een gedeclareerd lid noemt | **te meten** (een weessleutel hangt een granulariteit/meervoud/dimensie aan een lid dat niet bestaat — de lezers vallen stil, geen van drieën klaagt) | **voorrangspost, sluiten** |
| `Kenmerk.eigenaar` (de expliciete eigenaar van een kenmerkcheck) | **2 van 190** checks, **0 golden** (beide dragers zijn negatiefs) | alleen gelezen als de sleutel van een afhankelijkheidskant (`dep_cond`), nergens tegen de objecttypetabel gekruist | **te meten** (een onbekende eigenaar bouwt stil een kant op een knoop die niet bestaat: de stratificatie leest een lege buur) | sluiten als er een getuige te maken is |
| `Eenheid.stelsel` | **1292 van 1292** aanwezig | nergens tegen de gedeclareerde eenheidstelsels gekruist | te meten (WF-009's buurt) | sluiten of gemotiveerd open |
| `Eenheidstelsel.naam` | **195 van 195** aanwezig | nergens tegen de mapsleutel gekruist | te meten | sluiten of gemotiveerd open |
| de onopgeloste `regels`-stubs | **658** | `ctx.doc.regels` wordt door **geen enkele check** gelezen | te meten (de opgeloste vormen dragen alles; een stub mag vandaag letterlijk alles beweren) | eigen vraag: kruisen, of de stub normatief tot presentatie verklaren |
| `dagsoortPerEenheid` | **60**, alle 60 in de **pariteitszone**, 0 daarbuiten | ongekruist | te meten | dilemma (zie acceptance): geen golden-getuige beschikbaar |
| `VerreRollidmaatschap`'s eigen vuller + backref | **0 posities in alle vier zones** | ongekruist; de tweelingzus van (o), die wél dracht heeft | niet te meten zonder eigen golden | dilemma (zie acceptance) |
| `objectCreaties[].subject: null` | de lege helft van (n) | ongekruist | gemeten: verifier PASS, **beide** uitvoerders luid (`missing ObjectCreatie.subject`) | sluiten — de getuige is goedkoop |
| `anderTeFiller: null` | **1 van de 10** rolchecks | (o) gaat er bewust overheen | te meten (de drager is een dagsoortconditie waarvan de wandeling het subject óók niet kent) | sluiten of gemotiveerd open |

## De voorrangspost: de zijtabelsleutels

916 posities is verreweg de grootste rest, en het is de enige waar de schade
een **stille semantiekwijziging** is in plaats van een lege lezing. Een sleutel
in `kenmerkGranulariteit` die geen gedeclareerd kenmerk noemt, zegt "dit kenmerk
is tijdsafhankelijk" over een kenmerk dat niet bestaat; het gedeclareerde
kenmerk waarvoor de sleutel bedoeld was, blijft daarmee stilzwijgend
níet-temporeel, en dat is een ander programma — geen fout, een ander antwoord.
Dezelfde vorm geldt voor `attribuutMeervoud` (de rolnaamresolutie van WF-047 en
de ch. 4.3-terugval lezen die tabel), voor `attribuutDimensies` (WF-040's
label/dimensie-kruising) en voor `attribuutGranulariteit` (WF-043). Eén
weessleutel schakelt dus meerdere lezers tegelijk uit — precies het patroon dat
de kenmerkcheck-arm van golf 3 motiveerde, nu een niveau hoger, op de
declaratie in plaats van op de referentie.

Begin hier, en meet per tabel afzonderlijk: de acht tabellen zijn acht
onafhankelijke lezers en een mutant per tabel moet precies zijn eigen document
vrijspelen, anders is het één arm met acht getuigen en geen acht armen.

## Acceptance

- Per gesloten vlak minstens één negatief, rood-eerst zichtbaar (PASS onder de
  verifier van vóór deze kaart, één regel erna), met de arm-exacte mutant erbij:
  alleen die arm weg → alleen het eigen document schoon, over de volle
  vier-zone-sweep.
- **Het nul-dracht-dilemma expliciet beslissen, niet omzeilen.** Twee regels van
  het huis staan hier tegenover elkaar: (a) een spec-zin zonder
  machine-controleerbaar artefact is niet af — dus geen clausule zonder getuige;
  (b) de open-lijst is de plek voor een gemeten flank die je *niet* sluit. Voor
  `VerreRollidmaatschap` (0 dracht) en `dagsoortPerEenheid` (0 buiten de
  pariteitszone) betekent (a) dat er eerst een **golden** moet komen die de vorm
  draagt, of dat het veld met de meting naar de open-lijst gaat. Een negatief
  bouwen op een vorm die geen enkele golden draagt is de derde weg en is
  verboden: hij zou een clausule geven die geen enkel echt document ooit
  passeert, en de sweep zou dat niet zien. Kies per veld, schrijf de keuze op,
  en als het (a) wordt: de golden hoort bij een aparte corpuskaart, niet hier.
- De tellingen in deze tabel worden bij het landen **hermeten en gedateerd**, in
  IR-SPEC (vier zones, negatiefzone), in de indexrijkop (armen, golven,
  fixrondes) en in de betrokken NOTE's. Een golf die zijn eigen tellingen laat
  verlopen is de fout die de fixrondes van golf 4 en 5 moesten herstellen.
- De nul-voorkomens-claim van de indexrij blijft **gesplitst**: hij geldt voor
  de achttien vormen van golf 1-4, niet voor armen die posities kruisen die het
  corpus wél draagt. Elke arm van deze golf hoort in de tweede helft.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

## Out of scope

- **De Drools-namencontract-bug — eigen backend-kaart.** De vijfde golf en haar
  fixronde maten drie keer hetzelfde: een feitcreatie met een beanveld dat geen
  §13.3.9-veld noemt laat de backend op *fire* sterven met een
  ongeclassificeerde `IllegalStateException: geen getter voor '…'`. Zijn eigen
  leer is anders — de case-invoerkant heeft er een luide poort voor
  (`naamdelingsPoort`, `heeftReferentieVeld`) en de vouwkant weigert dezelfde
  grens met een reden-dragende SKIP — en poort 12 zegt met zoveel woorden dat
  een ERROR bij fire nooit een waardedivergentie is. De schrijfkant van de
  feitcreatie mist die poort. Dat is een backendreparatie, geen verifierregel:
  deze kaart maakt invoer ill-formed, zij repareert geen uitvoerder.
- `Rollidmaatschap.rol` — presentatie: de uitvoerders leiden de verbindende rol
  af uit subject + `anderTeFiller` en lezen het veld niet. Er is niets om tegen
  te kruisen zonder eerst te beslissen dat het veld betekenis krijgt.
- `Eenheid.afkorting` en de rest van WF-009's eenhedenvraag: eigen regel, eigen
  kaart.
- De rij-1-wrat en zijn restwijdte (acht posities, twee golden — geteld in golf
  4 en door golf 4 gescoped, niet gerepareerd).
- De sentinel op de 9 fold-/aggregatie-argumenttypes: de aggregatietak vervangt
  daar de generieke wandeling, eigen vraag.
- WF-007's precisievormcontrole, die dezelfde blinde vlek in de containers heeft
  — die wandeling is van WF-007, niet van WF-034.
