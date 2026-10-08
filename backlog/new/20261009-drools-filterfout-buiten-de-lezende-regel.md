# drools-filterfout-buiten-de-lezende-regel: de exec-modeltak en de DRL-teksttak geven een fout uit het filter van een vouw waar de lezende regel niet geldt

*Gevonden op 2026-10-08 door de lens op stap 2 van de kaart
`20261008-tms-aggregaties-creatie-en-fouten` (waarneming 2 van haar
`Result:`) en door de hoofdsessie vastgelegd volgens de afspraak dat een
bestaande bug een kaart krijgt. Gemeten op uchar main `54ba99f7` (2026-10-09),
met ref, clingo en de verifier uit `_build` van de hoofdcheckout en de shaded jar
van `backends/drools`, gebouwd uit een bevroren kopie van die commit. De
proefbestanden stonden onder `/tmp/tms-volgorde-dump-LOPc/docsF/`
(`Z0-niet-actief`, `Z1-actief`, `D0-niet-actief`, `D1-actief`); de documenten
staan hieronder, zodat de meting te herhalen is.*

## Aanleiding

Ref rekent eerst de voorwaarde van een regel uit en de waarde alleen waar de
regel vuurt. Een samengestelde voorwaarde rekent hij maar tot het lid waarna
haar oordeel vaststaat (`spec/RUNTIME-SEMANTICS.md` ch. 5.10), en het filter van
een vouw per lid alleen waar hij de vouw uitrekent. Deelt dat filter daar door
een numerieke nul, dan faalt de evaluatie (ch. 1.4, punt 4). Nergens anders
wordt die deling uitgevoerd, dus nergens anders ontstaat een fout.

Model: objecttype Jutter met `zware vondsten` (geheel getal) en de kenmerken
`actief` en `druk`; objecttype Vondst met `gewicht` en `deelnemers` (gehele
getallen); feittype aanmelding: één aanmelder (Jutter) heeft meer vondsten
(Vondst).

Document Z:

    Regel zware vondsten: De zware vondsten van een Jutter moet berekend worden
        als het aantal vondsten van de Jutter die aan de volgende voorwaarde
        voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2,
        indien hij actief is.

Document D:

    Regel drukte: Een Jutter is druk indien hij aan alle volgende voorwaarden
        voldoet:
        - hij is actief;
        - het aantal vondsten van de Jutter die aan de volgende voorwaarde
          voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2,
          is groter dan 0.

Case (beide documenten): J1 met de vondsten V1 (gewicht 10, deelnemers 2) en V2
(gewicht 9, deelnemers 0). J1 is niet actief. Verwacht bij Z: `zware vondsten`
van J1 leeg, consistent; bij D: J1 niet druk, consistent. Is J1 wel actief, dan
rekent ref de vouw uit en is de deling van V2 een fout.

## Gemeten (2026-10-09, main `54ba99f7`)

| Uitvoerder | Z, J1 niet actief | D, J1 niet actief | Z en D, J1 actief |
|---|---|---|---|
| verifier | `PASS` | `PASS` | `PASS` |
| ref | `oracle: PASS (2 expectations)` | idem | `EVALUATION FAULT: division by numeric zero` |
| clingo | `clingo: PASS (2 expectations)` | idem | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V2")` |
| Drools, exec-modeltak | `ERROR DelingDoorNul: deling door nul in de regel "zware vondsten": 9 gedeeld door 0 …` | idem, regel "drukte" | idem |
| Drools, DRL-teksttak | `ERROR ConstraintEvaluationException: Error evaluating constraint 'rsAggVergelijk(rsGedeeld(gewicht, deelnemers), …)'` | idem | idem |
| Drools, truth-maintenance-tak (`--tms`) | `PLAYED 1` | `PLAYED 1` | `ERROR DelingDoorNul …` |

De exec-modeltak en de DRL-teksttak rekenen het filter dus uit, ook waar de
lezende regel niet geldt. De tak met truth maintenance kreeg daarvoor in
`a987275c` een markeerfeit `Filterfout`; de fout ontstaat daar pas waar de
lezende vorm de vouw uitrekent. Zijn getuige `TmsFilterfoutWitnessTest`
bouwt beide documenten, met beide cases, en ook een document waarin twee
leden falen (het eerste in de vouwvolgorde telt, ch. 5.11) en een filter dat
alleen op een tussenstand faalt. Geen golden in `corpus/golden` heeft een
deling in het filter van een vouw onder een voorwaarde die niet geldt; daarom
zien de gates 5, 6 en 12 het niet.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met beide vormen (één document met beide regels mag), eigen
  Jutterswet-casus, rechtenvrij, met `bron.rgs`, `ir.json` en cases: J1 niet
  actief (de verwachting hierboven), en een tweede Jutter die wel actief is en
  geen vondst met deelnemers 0 heeft, zodat de vouw ook geteld wordt. Het geval
  "J1 actief" is een fout en hoort in `corpus/rood` (met
  `expectedFault {ref, clingo, drools}`), als het nog geen rood geval met deze
  vorm heeft.
- (b) De exec-modeltak en de DRL-teksttak gerepareerd, rood eerst: ref ≡ clingo
  ≡ Drools (alle drie de takken) op de nieuwe golden, tegen absolute
  verwachtingen, en de fout in het rode geval waar ref en clingo hem geven.
- (c) Nagaan of de andere plaatsen waar een vouw onder een voorwaarde staat
  hetzelfde doen: een vouw in de waarde van een regel met voorwaarde, in een lid
  van een samengestelde voorwaarde ná een lid dat het oordeel beslist, en in de
  voorwaarde of initialisatie van een creatie. Wat zo gevonden wordt, krijgt een
  case in de golden of een getuige.
- (d) Gates 5, 6, 8 en 12 groen; in `scripts/check-drools.sh` gaan
  `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` elk één omhoog (beschermd pad:
  voorstel); de corpusgetuige `TmsCorpusWitnessTest` speelt de golden; alle
  gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel. In beide takken waarschijnlijk dezelfde maatregel: het filter
pas laten rekenen waar de lezende vorm de vouw uitrekent, of een fout uit het
filter uitstellen tot daar, zoals het markeerfeit van de tak met truth
maintenance. De bouwer kiest.

May touch: `corpus/golden/<nieuwe golden>/`, `corpus/rood/<nieuw geval>/`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/DrlTranspiler.java`
(en wat die tak deelt, voor zover nodig), getuigen onder
`backends/drools/src/test/java`, `scripts/check-drools.sh` (de twee pins;
voorstel), kaartverplaatsing. Niet in deze kaart: ref en clingo (geven al het
goede antwoord), de spec, en de tak met truth maintenance.

## Raakvlakken

- Na het besluit van 2026-10-08 (PLAN §9) vervangt `20261008-tms-omschakeling`
  de exec-modeltak door de verlaging met truth maintenance, en zet
  `20261008-drl-teksttak-op-truth-maintenance` de DRL-teksttak op truth
  maintenance. De fix in die twee takken vervalt dan; de golden en het rode
  geval blijven. Deze kaart is klein en kan daarvóór landen.
- `20261008-fout-op-tussenstand` gaat over een fout op een tussenstand; deze
  kaart over een fout waar geen evaluatie is. Ze raken elkaar niet, behalve dat
  beide een fout als iets van de evaluatie zien en niet van de verlaging.
- Serieel met andere kaarten in `backends/drools` (de pins van gate 12).
