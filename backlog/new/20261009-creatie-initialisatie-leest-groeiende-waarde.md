# creatie-initialisatie-leest-groeiende-waarde: een initialisatie van een gecreëerd object leest een waarde die na het maken nog groeit, en ref en clingo verschillen

*Gevonden op 2026-10-08 tijdens stap 3 van de kaart
`20261008-tms-aggregaties-creatie-en-fouten` (waarneming 1 van haar
`Result:`) en door de hoofdsessie vastgelegd volgens de afspraak dat een
bestaande bug een kaart krijgt. Gemeten op uchar main `54ba99f7`
(2026-10-09), met ref, clingo en de verifier uit `_build` van de hoofdcheckout
en de shaded jar van `backends/drools` uit een bevroren kopie van die commit.
Het document is de getuige `documentC()` in
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/TmsCreatieWitnessTest.java`
(regel 405–462 op main); de proefbestanden stonden onder
`/tmp/tms-stap3c/docs/C-keten/`.*

## Aanleiding

`spec/RUNTIME-SEMANTICS.md` ch. 5.5 zegt bij het maken: "the member
initialisations apply (values evaluated on the subject; …)". Op welke stand van
de feitenbasis die waarden gelden als de waarde van het onderwerp later in
dezelfde evaluatie nog verandert, zegt de spec niet. De restpost "a value that
grows" van ch. 6.1 gaat over voorwaarden op een toegelaten creatiekring, niet
over initialisaties. WF-045 laat de kring toe, omdat hij door een recursieve
creatie loopt (ch. 6.2).

Model: objecttype Wachter met `rang`, `stand`, `beginstand` en `opvolgerbegin`
(gehele getallen); feittype opvolging: één voorganger (Wachter) heeft één
opvolger (Wachter).

    Regel stand van een wachter: De stand van een Wachter moet berekend worden
        als het aantal opvolgers van de Wachter.
    Regel opvolgerbegin: Het opvolgerbegin van een Wachter moet gesteld worden
        op de beginstand van de opvolger van de Wachter.
    Objectcreatie werf de opvolger (lid van de recursieve regelgroep
        estafette): een Wachter waarvan de rang kleiner is dan 4 krijgt een
        opvolger (Wachter, over opvolging met de rol opvolger) met rang gelijk
        aan zijn rang plus 1 en beginstand gelijk aan zijn stand.

Case: W1 met rang 1. W1 maakt W2, W2 maakt W3, W3 maakt W4; W4 (rang 4) maakt
niets. Op het moment dat W1 zijn opvolger maakt, heeft hij er nog geen, dus zijn
stand is 0; daarna is ze 1.

## Gemeten (2026-10-09, main `54ba99f7`)

| Uitvoerder | stand van W1 | opvolgerbegin van W1 (= beginstand van W2) |
|---|---|---|
| verifier | `PASS` | |
| ref | 1 | 0 (de stand van W1 op het moment van maken) |
| clingo | 1 | 1 (`MISMATCH: W1.opvolgerbegin = [1], expected 0`: de stand van W1 in het stabiele model) |
| Drools, exec-modeltak | 1 | 0 (`PLAYED 1` op de verwachting van ref) |
| Drools, DRL-teksttak | 1 | 0 (idem) |
| Drools, truth-maintenance-tak | 1 | 0 (idem; de getuige pint het in elke regelvolgorde) |

Clingo heeft geen moment van maken: zijn initialisatie is een regel over het
stabiele model en leest de eindwaarde. Ref en de drie Drools-takken voeren de
creatie uit op een tussenstand en houden de waarde van dat moment. Geen golden
heeft deze vorm; daarom zien de gates het niet.

## Ook ref hangt hier van de volgorde af

De Fable-adviseur (zie onder) mat een variant met een kopieregel ertussen:
"De standkopie van een Wachter moet gesteld worden op zijn stand", en de
initialisatie "beginstand gelijk aan zijn standkopie". Ref geeft dan
`opvolgerbegin` van W1 leeg als de kopieregel vóór de stand staat
(`MISMATCH: W1.opvolgerbegin = leeg, expected 0`) en 0 in de andere volgorde;
clingo geeft in beide 1. Ref draait per pas de regels, verdelingen, defaults en
creaties één keer in documentvolgorde (`ref/lib/eval.ml` rond regel
3882–3918) en rekent een initialisatie tegen de lopende wereld uit (rond regel
3095), dus "het moment van maken" is bij ref een plaats in de pas. De
proefbestanden staan in `/tmp/lens-bugs/advies-probes/` (`q1-lezer-eerst`,
`q1-schrijver-eerst`).

## Besluit (voorlopig, 2026-10-09)

*De eigenaar was offline en vroeg bij twijfel een Fable-adviseur te
raadplegen. Het advies (Fable 5.1, alleen lezend, op main `54ba99f7`) staat in
`/tmp/lens-bugs/advies-fable.md`, vraag 1; dit is het aanbevolen antwoord, en
het blijft voorlopig tot de eigenaar het bevestigt. Elke kaart stopt vóór de
merge.*

**De waarde op het moment van maken, en dat moment is geen plaats in een pas
maar een generatie.** Binnen een stratum verloopt de evaluatie in generaties.
Generatie 0 is het fixpunt van de vormen van het stratum die niets maken, over
de invoer. Generatie G+1 is wat elke creatie van het stratum afleidt uit de
uitgerekende wereld van generatie G: elke creatie tegen dezelfde wereld, geen
die leest wat haar eigen generatie maakt of linkt; daarna komen de andere vormen
weer tot rust. De voorwaarde en de initialisaties van een creatie worden op die
uitgerekende wereld geëvalueerd. Een latere generatie mag de gelezen waarde nog
veranderen; de initialisaties van het gemaakte object volgen haar niet.
Dat is de lezing van ch. 5.5 ("values evaluated on the subject" bij het maken,
"exactly one generation per instance") en van ch. 6.1 (een creatie leest
stratum-strikt, en op een toegelaten kring maakt de voortgang van één generatie
per instantie die striktheid waar), en dezelfde als het besluit van de eigenaar
van 2026-10-08 voor de tak met truth maintenance (PLAN §9). Op dit document:
stand(W1) = 1 en opvolgerbegin(W1) = 0, in elke documentvolgorde, ook met de
kopieregel.

Afgewezen: de eindwaarde (dan hangt een initialisatie af van wat het gemaakte
object zelf veroorzaakt, en zou de voorwaarde consequent ook aan het fixpunt
gelezen moeten worden, precies de vormen waarop clingo geen stabiel model
vindt); een verifierregel (de waarde bij het maken is welgedefinieerd; zo'n
regel zou een grens van clingo in de spec zetten).

Welke vormen: alleen lezingen van een creatie op een kring die WF-045 toelaat.
Buiten zo'n kring leest de verifier elke attribuutlezing van een creatie
strikt, dus is elke producent klaar vóór de creatie vuurt.

## Acceptatie

- (a) **Ref** evalueert per stratum in generaties zoals hierboven (ook de
  feitcreaties, nu achter elkaar in documentvolgorde: elke creatie van een
  generatie tegen dezelfde wereld). Rood eerst: voorbeelden in `ref/test/` voor
  dit document en de kopievariant, elk in beide documentvolgordes, met de
  absolute uitkomsten hierboven; de kopievariant met de lezer eerst faalt nu.
- (b) **Clingo** kan het moment van maken zonder een kopie van de regellaag per
  generatie niet uitdrukken. Het weigert daarom luid en bij naam (de tweede
  houding van `spec/IR-SPEC.md` ch. 5.5) elke initialisatie van een creatie die
  een knoop leest die een vorm die niets maakt op dezelfde toegelaten kring
  schrijft, over de afhankelijkheidsgraaf die ref ook gebruikt. Een check in
  `backends/clingo/test`. De ankers en goldens met recursieve creatie
  (`jutterswet-wachtersketen`, `jutterswet-opvolgerquorum`, …) blijven spelen:
  hun initialisaties lezen alleen wat de creatie zelf schrijft; gate 6 meet dat.
- (c) **Drools**: ongewijzigd. De tak met truth maintenance doet het al (de
  driver laat de regellaag tot rust komen en vuurt dan een generatie); de
  exec-modeltak en de DRL-teksttak vervallen met de omschakeling en de
  DRL-kaart. De getuige `documentC` blijft.
- (d) **Spec** (beschermd pad: voorstel in het `Result:` en als diff op de
  branch): een alinea "Generations" in ch. 5.5 met dit document als uitgewerkt
  voorbeeld, en de aanvullingen in ch. 6.1 (een creatie leest de uitgerekende
  wereld van de vorige generatie) en ch. 6.3 (fixpunt per stratum, in
  generaties). Meet de uitgewerkte restposten van ch. 6.1 ("conditions that hold
  on absence", "a value that grows") opnieuw in ref en meld elke uitkomst die
  verandert; de generatiezin legt ook voor de voorwaarde de lezing van ref vast,
  zodat "a value that grows" een tekort van clingo wordt in plaats van een
  normvraag (twijfel van de adviseur; open punt voor de eigenaar).
- (e) Geen golden (gate 6 speelt elke golden door clingo, dat deze vorm
  weigert), geen negatief en geen rood geval. Geen bestaande verwachting in
  `corpus/golden` of `corpus/rood` verandert; verandert er een, dan stopt de
  bouwer en meldt het. Alle gates van `CLAUDE.md` groen.

## Omvang

Middel. Het werk zit in de evaluatielus van ref (per stratum een buitenste lus
over generaties om de lus van de regellaag), de weigering in clingo en de
spectekst.

May touch: `ref/lib/**`, `ref/test/**`, `backends/clingo/**`,
`spec/RUNTIME-SEMANTICS.md` (voorstel), kaartverplaatsing. Niet: de
Drools-backend, `corpus/`, `scripts/` (de vloeren groeien vanzelf).

## Raakvlakken

- `20261009-creatiebudget-ref-en-clingo` telt het budget in dezelfde
  generaties en bouwt op deze kaart; serieel, daarna.
- `20261008-fout-op-tussenstand` vraagt of een fout op een tussenstand telt;
  de generatiezin verandert daar niets aan.
