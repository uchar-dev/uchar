# drools-exec-datumvouw-over-rol: een vouw over datums langs een rol speelt niet in de exec-tak

*Vondst van een regelsetbouwer, 2026-09-28 (brug K1 in zijn README), bevestigd
door de hoofdsessie op uchar `056df376` via de distributie; reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/k1k3/` (k1a.rgs; alleen lezen). Luid
(exit 3), geen fout antwoord; wel een gat dat een regelset dwingt tot
dagtellingen als hulpattributen.*

## Aanleiding

Een regel die de vroegste of laatste datum van een verzameling langs een
rol neemt ("de eerste van de vinddag van alle vondsten van de Jutter")
resolvet en speelt in ref en clingo, maar de exec-tak markeert hem: "regel
(waarde buiten scope)". Een regelset die per grond een einddatum kent en op
het recht de laatste (of eerste) daarvan nodig heeft, schrijft daarom per
object een aantal dagen vanaf een ankerdatum en neemt daar het maximum van
(een getalvouw speelt wel; gemeten: `het aantal vondsten van de Jutter plus
1` in een berekening geeft exit 0). Dezelfde bouwer meldt dat ook een vouw
in een berekening ("aggregatie-in-arithmetiek buiten scope") in zijn model
markeerde; meet welke vorm dat was.

## Minimale reproductie

`056df376`, distributie `bin/regelspraak check`.

    Objecttype de Jutter (mv: Jutters) (bezield)
        de eerste vinddag Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
    Feittype JutterHeeftVondst
        de jutter Jutter
        de vondst (mv: vondsten) Vondst
    Eén jutter heeft meerdere vondsten

    Regel eerste vinddag
        geldig altijd
            De eerste vinddag van een Jutter moet berekend worden als de eerste van de vinddag van alle vondsten van de Jutter.

    nu: resolved=1; "niet uitvoerbaar in uchar (exit 3): transpiler SKIP: regel: eerste vinddag —
        regel (waarde buiten scope): eerste vinddag".

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  eerste en de laatste datum over een to-many rol; dezelfde vouw met een
  voorwaarde op de elementen; een datumvouw als operand in een berekening
  (plus een duur); de getalvouwen (aantal, som, minimum, maximum) als operand
  in een berekening; twee rollen diep.
- (b) De exec-tak speelt de datumvouw over een rol (eerste/laatste), en de
  vormen uit (a) die met dezelfde lowering meegaan; wat niet meegaat blijft
  een benoemde SKIP met de vorm in de zin. Rood-eerst JUnit-getuigen.
- (c) Golden (werknaam `jutterswet-vroegste-vondst`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` in `scripts/check-drools.sh` als
  **voorstel**; de DRL-teksttak volgens haar eigen kanaal.
- (d) Corpussweep: played-telling en kanaal van gate 12 ongewijzigd voor de
  bestaande goldens; alle zestien gates groen (bash waar de shebang dat zegt).

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaarten
drools-exec-voorwaarde-genavigeerde-afleiding en drools-subtype-in-supertype-rol.

## Result (2026-09-30, bouwer)

Tak `card/drools-exec-datumvouw-over-rol` in de worktree van de kaart, afgetakt van main `6b7ab8a2`; niet gerebased,
niet gepusht, niet gemerged. Twee rondes: de eerste bouwde de kaart; de tweede, op aanwijzing van de coördinator en met
diens toestemming voor twee bestaande testbestanden (`DatumSetvouwWitnessTest`, `DrlEenheidsplekWitnessTest`), liet
ook het minimum en maximum over datums spelen en zette de vorm "eigen datum plus een vouw in dagen" in de golden. Dit
Result beschrijft de stand na de tweede ronde. De kaart is geschreven op `056df376`; sinds dat punt landden in dezelfde transpiler ook
drools-exec-defaults-justificatie-vouw en drools-exec-kwantorvoorwaarde-eigen-operand, dus alles is op deze kop
opnieuw gemeten. Gemeten met de eigen uitvoerders van deze repo: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, `_build/default/spec/verifier/bin/verifier.exe`, de frontend
(`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de
teksttak). "Vóór" is de jar van de claim (`f4bb0a02`, backend gelijk aan main), "na" de jar van de kop. Elke vorm is een
eigen Jutterswet-document (bron.rgs + scenario.rgs), door een generator geschreven, door de frontend naar ir.json en
cases verlaagd en door een meetscript gemeten; beide staan buiten de repo, naast de reproducties van de kaart. Alle
documenten zijn verifier-schoon (`verifier: PASS`).

### De reproductie op deze kop

- k1a, "de eerste van de vinddag van alle vondsten van de Jutter": ref `oracle: PASS (1 expectations)`, clingo
  `clingo: PASS (1 expectations)`, exectak `EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten
  scope): eerste vinddag`, DRL-tak `EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de
  eerste van de vinddag van alle vondsten van de Jutter`. Ongewijzigd sinds `056df376`.
- k1a2, "de eerste datum van de vinddagen van alle vondsten van de Jutter": de frontend weigert de zin (`setlezing
  buiten een vouwbron 'de eerste datum van de vinddagen van alle vondsten van de Jutter': hop 0 reaches the to-many role
  'vondst' in a scalar slot — a to-many step needs an aggregation, never a silent first pick`); geen Drools-vorm.
- k1b, "het aantal vondsten van de Jutter plus 1": `PLAYED 1` in beide takken, ref en clingo `PASS`.
- k1c, "de som van de gewichten van alle vondsten van de Jutter plus 1": de frontend weigert de meervoudsvorm met
  dezelfde setlezingszin; "(de som van het gewicht van alle vondsten van de Jutter) plus 1" speelt al (d-som-plus).
- De vorm achter "aggregatie-in-arithmetiek buiten scope" van de regelsetbouwer is een eigen datum plus het minimum of
  maximum van een aantal dagen over een rol. Op Jutterswet-materiaal (c-datum-plus-getalvouw, "zijn startdag plus de
  maximale waarde van de zoekduur van alle vondsten van de Jutter"): `EXCLUDE transpiler SKIP: regel: meldgrens —
  regel (aggregatie-in-arithmetiek buiten scope): meldgrens`. Dezelfde zin kregen een begrenzing om een telling
  (d-begrensd) en een telling plus een genavigeerde lezing (d-genavigeerd).

### Oorzaak

1. **De datumvouw.** `ExecModelTranspiler.isAggregatieKind` kent EERSTE en LAATSTE niet. Een regel met de eerste of
   laatste datum over een rol kwam daardoor niet in de aggregatietak maar in de scalaire waarde, en `scalarWaarde` heeft
   voor een vouw over een setbron geen arm: `waarde buiten scope`. Ook `vouw` bouwde alleen SOM/TOTAAL/MINIMUM/MAXIMUM, en
   bond het lid als `BigDecimal`.
2. **Een vouw in een berekening.** Een waarde die een vouw bevatte, liep via een eigen evaluator, `lowerVal`, naast
   `scalarWaarde`. Die kende alleen getalliteralen, eigen attributen, parameters, de vier hoofdbewerkingen, afronden en
   wortel. Datumrekenkunde, een genavigeerde lezing, een begrenzing, een Let of een functie weigerde zij
   (`aggregatie-in-arithmetiek buiten scope`). Twee vormen rekende zij fout: `percentage van` en `gedeeld door (ABS)`
   vielen in haar `switch` op `default -> null`, zodat de waarde leeg werd (d-percentage: `verwacht 1.90, werkelijk
   leeg` voor elke Jutter); en een minimum of maximum bond zij zonder telling, zodat een lege bron geen rij gaf en de
   regel niet vuurde, waar ch. 1.2 "leeg plus 1" 1 maakt (d-min-plus, d-max-plus: `verwacht 1, werkelijk leeg`).

### Fix en de gekozen lowering

`ExecModelTranspiler` (+176, −50 regels over beide rondes, commentaar inbegrepen), in de tweede ronde ook
`kern/Vouwwaarde` (+16, −12: de zin voor een ordenende vouw over een datum) en een commentaar in `DrlTranspiler`;
`Stratificatie`, de loader en de beangeneratie zijn niet aangeraakt.

- **`vouwslot`.** In de waarde van een regel wordt elke vouw over een setbron (`isInstantievouw`: de telvouwen, de
  optelvouwen en de ordenende vouwen MINIMUM/MAXIMUM/EERSTE/LAATSTE, niet de gelijste vouw over twee of meer operanden)
  een accumulate naast het onderwerppatroon, met de bestaande `aggregatie`. `scalarWaarde` geeft voor die vouw een
  `WaardeFn` die haar uitkomst uit een slot leest in de map die elke `WaardeFn` al doorgeeft (sleutel `vouw N`, met een
  spatie, zoals de binderslots van een Let, zodat geen parameterglobal botst). De consequentie bindt `[onderwerp,
  globals…, per vouw haar telling (waar gebonden) en haar uitkomst]` en vult de slots (`vulVouwslots`): een telling 0 is
  de lege bron, dan de terugval van de som of leeg. Zo geldt rond de vouw elke operator van de scalaire waarde:
  datumrekenkunde (`datumRekenWaarde`), een tijdsduur, een percentage, een begrenzing, een genavigeerde lezing met haar
  far-side-join, een Let, en de lege operanden van ch. 1.2. Het verzamelen staat alleen aan terwijl `regel` zijn waarde
  verlaagt, en staat uit terwijl een vouw zelf verlaagt (een filter of een terugval hoort bij de accumulate).
- **EERSTE en LAATSTE** vouwen via `MinAccumulateFunction` en `MaxAccumulateFunction`, die elke `Comparable` vergelijken;
  een datumlid (LocalDate, LocalDateTime) wordt gebonden zoals het is (`Comparable`), een getal als `BigDecimal`. Een
  ordenende vouw in de waarde bindt een telling, dus een lege bron leest leeg en de regel vuurt. De bestaande `!= null`-
  bronbewaking slaat een lege bijdrager over (ch. 1.2e). Een lid dat geen getal en geen datum is, weigert met naam
  (tweede verdedigingslinie achter WF-038, zoals `Vouwwaarde` er een houdt voor MINIMUM/MAXIMUM).
- **Het minimum en maximum over een datumbron** (tweede ronde) zijn dezelfde ordenende vouw (ch. 1.2e) en gaan door
  dezelfde accumulate. In de waarde van een regel vraagt `aggregatie` de weigering van `Vouwwaarde` er niet voor
  (`ordenendeDatumvouw`, gelezen van de declaratie van het lid), en de kale getalvouw laat zo'n vouw aan de slots over,
  want haar uitkomst is geen BigDecimal. Buiten de waarde blijft de vorm geweigerd: `aggregatie` zet dan een eigen zin
  van deze tak (`vouw MINIMUM over de datumbron … buiten de waarde van een regel`). In een voorwaarde toont de regel de
  zin van `voorwaardeSkipReden` hieronder (gemeten, m-min-voorwaarde); in een rechtvaardiging en een objectcreatie is het
  niet gemeten.
  De datumzin van `Vouwwaarde` vraagt nu alleen de DRL-tak nog, en die zin noemt de DRL-tak als de tak die de vouw niet
  draagt: `… terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen
  waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van
  een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat
  in die tak, geen semantisch verschil (§5.8.3)`.
- De tak `containsAggregatie → lowerVal` is uit `regel`; `lowerVal` blijft alleen voor de objectcreatie.
- Een vouw naast een deling of wortel onder één Let of IfThenElse weigert met naam. De companion van zo'n §6.5/§6.6-
  plek evalueert de knoop opnieuw in een eigen regel (`sondeConsequentie`), zonder de accumulate van de gastregel;
  gemeten zonder deze weigering: de bouw valt om met `ERROR IllegalStateException: telling (deling door leeg): de
  drager van de §6.5/§6.6-plek lowert niet, terwijl de gastregel lowerde`. Een vouw onder een Let zonder zo'n plek
  speelt (h-let).
- Een ordenende vouw over een datum in een voorwaarde weigert met de vorm in de zin (`voorwaardeSkipReden`): `voorwaarde
  met een ordenende vouw over een datumbron (eerste, laatste, minimum of maximum over een rol), die deze tak alleen in de
  waarde van een regel vouwt`.
- `exprLezingen` (de graaf van `groepVolgorde`) leest de leden van een EERSTE/LAATSTE-vouw nu op het elementtype, zoals
  die van de andere vouwen.
- De kale getalvouw als hele waarde (`isAggregatieWaarde`) is ongewijzigd: een leeg minimum of maximum vuurt daar niet en
  het doel blijft leeg, hetzelfde antwoord.

Verworpen:
1. `lowerVal` uitbreiden met datumrekenkunde, genavigeerde lezingen, begrenzing en de ontbrekende operatoren: een tweede
   evaluator naast `scalarWaarde` die gelijk moet blijven lopen, en de twee foute antwoorden hierboven zijn precies zijn
   afwijking van `scalarWaarde`.
2. EERSTE en LAATSTE toevoegen aan `isAggregatieKind`: dat opent ook de voorwaardepaden (`aggregatieVoorwaarde`,
   `aggVsAgg`, de rechtvaardiging), die de uitkomst als getal vergelijken (`rsAggVergelijk` met `toBd`); een datum zou
   daar pas bij het vuren omvallen. De soorten worden alleen in de waarde herkend.
3. Een collectList-accumulate met de vouw in de consequentie (zoals de kwantorvouw): werkt ook, maar is zwaarder dan de
   Min/Max-functies, die al terugdraaien ondersteunen.
4. In de eerste ronde: het minimum en maximum over een datumbron laten rusten, omdat
   `DatumSetvouwWitnessTest` de weigering in beide takken pinde; in de tweede ronde met toestemming meegenomen.

### (a) Vormruimte, vóór en na

Basis: `Strandvoogd` (eerste en laatste vinddag, laagste gewicht, bonus), `Jutter` (eerste en laatste vinddag, eerste
en laatste bergdag, meldgrens, startdag, telling, uitkomst, spanne in dagen, eerste vindtijdstip, kenmerk vroeg),
`Vondst` (vinddag, bergdag, vindtijdstip, gewicht, zoekduur in dagen, kenmerk zwaar); één jutter heeft meerdere
vondsten, één strandvoogd beheert meerdere beheerde jutters. De situatie van elke vorm: J1 met V1 12-03-2026 (gewicht 4,
zoekduur 10), V2 20-03-2026 (7, 3, zwaar), V3 zonder vinddag (6, 20, zwaar) en V4 05-03-2026 (2), in die volgorde; J2
zonder vondsten; J3 met V5 zonder vinddag en gewicht; J4 met V6 31-12-2025 (9, 5, zwaar); startdag J1 en J2
01-03-2026, J4 01-01-2026; S1 beheert J1 en J3, S2 J2, S3 J4, S4 niemand. a-*: de datumvouw als hele waarde; b-*: met een
voorwaarde op de elementen; c-*: als operand, en in een voorwaarde; d-*: de getalvouwen als operand; e-*: twee rollen
diep; f-*: flanken van de lowering; m-*: het minimum en maximum over datums, een handgeschreven document (de ir.json van
de a-, b-, c- of e-vorm met first → minimum en last → maximum; de frontend typeert die spelling weg), met dezelfde cases;
h-*: een Let; g-*: een objectcreatie (de toekenning teruggelezen op de Jutter). "idem" in de kolom "exec na": gelijk aan
vóór. De DRL-kolom is die van de kop; het kanaal is vóór en na in elke rij gelijk, en alleen in de m-rijen veranderde de
tekst, van "terwijl beide takken elke waardevouw in BigDecimal accumuleren … maar deze tak heeft haar niet gebouwd" naar
de zin hierboven, die de DRL-tak noemt.

| vorm | omschrijving | ref | clingo | exec vóór | exec na | DRL-tekst na |
|---|---|---|---|---|---|---|
| a-afgeleid-na | vouw over een later afgeleide datum; de vouw staat vóór haar schrijver | oracle: PASS (8 expectations) | clingo: PASS (8 expectations) | EXCLUDE transpiler SKIP: regel: eerste bergdag — regel (waarde buiten scope): eerste bergdag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste bergdag — TODO aggregatie EERSTE: de eerste van de bergdag van alle vondsten van de Jutter |
| a-afgeleid-voor | idem, de schrijver eerst | oracle: PASS (8 expectations) | clingo: PASS (8 expectations) | EXCLUDE transpiler SKIP: regel: eerste bergdag — regel (waarde buiten scope): eerste bergdag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste bergdag — TODO aggregatie EERSTE: de eerste van de bergdag van alle vondsten van de Jutter |
| a-beide | eerste en laatste in één document | oracle: PASS (8 expectations) | clingo: PASS (8 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| a-eerste | de eerste datum over een to-many rol | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| a-gesteld | de eerste datum, gesteld | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| a-initialisatie | de eerste datum als initialisatie; J4 heeft een invoerwaarde | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| a-laatste | de laatste datum over een to-many rol | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: laatste vinddag — regel (waarde buiten scope): laatste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: laatste vinddag — TODO aggregatie LAATSTE: de laatste van de vinddag van alle vondsten van de Jutter |
| a-tijdstip | de eerste van een datum-en-tijd over een rol | oracle: PASS (4 expectations) | TRANSLATION FAULT: datum-tijd here is not the pair term's arithmetic. Carried: a datum-tijd literal, an own-subject read, datetime ± duration, and first/last over listed operands. A loud flank, no corpus case: a datum-tijd PARAMETER read, a Let/BinderRef-bound datum-tijd (and the residual VariableReference), a set-typed fold, a dateWith or dateTimeWith constructor, and a navigated datum-tijd read | EXCLUDE transpiler SKIP: regel: eerste vindtijdstip — regel (waarde buiten scope): eerste vindtijdstip | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vindtijdstip — TODO aggregatie EERSTE: de eerste van het vindtijdstip van alle vondsten van de Jutter |
| b-ander-attribuut | filter op een ander attribuut (zijn gewicht is groter dan 5) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: laatste vinddag — regel (waarde buiten scope): laatste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: laatste vinddag — TODO aggregatie LAATSTE: de laatste van de vinddag van alle vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 5 |
| b-kenmerk | filter op een ingevoerd kenmerk (die zwaar zijn) | oracle: PASS (8 expectations) | clingo: PASS (8 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter die zwaar zijn |
| b-kenmerk-afgeleid-na | filter op een later afgeleid kenmerk; de vouw eerst | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter die zwaar zijn |
| b-kenmerk-afgeleid-voor | idem, de schrijver eerst | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter die zwaar zijn |
| b-vergelijking | filter die de gevouwen datum zelf vergelijkt (die later zijn dan 10-03-2026) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter die later zijn dan 10-03-2026 |
| c-datum-plus-getalvouw | eigen datum plus de maximale waarde van een duur over de rol | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: meldgrens — regel (aggregatie-in-arithmetiek buiten scope): meldgrens | PLAYED 1 | FAIL case1-c-datum-plus-getalvouw.json: attribuut meldgrens: verwacht 2026-03-01, werkelijk leeg |
| c-min-duur | (de laatste van …) min 7 dagen | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: meldgrens — regel (waarde buiten scope): meldgrens | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: meldgrens — TODO datum-rekenkunde: (de laatste van de vinddag van alle vondsten van de Jutter) min 7 dagen |
| c-plus-duur | (de eerste van …) plus 30 dagen | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: meldgrens — regel (waarde buiten scope): meldgrens | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: meldgrens — TODO datum-rekenkunde: (de eerste van de vinddag van alle vondsten van de Jutter) plus 30 dagen |
| c-tijdsduur | de tijdsduur van de eerste tot de laatste in hele dagen | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: spanne — regel (waarde buiten scope): spanne | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: spanne — TODO tijdsduur: de tijdsduur van de eerste van de vinddag van alle vondsten van de Jutter tot de laatste van de vinddag van alle vondsten van de Jutter in hele dagen |
| c-voorwaarde | de datumvouw in een voorwaarde (eerder dan 01-03-2026) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: vroeg — regel (voorwaarde buiten Phase-B scope): vroeg | EXCLUDE transpiler SKIP: regel: vroeg — regel (voorwaarde met een ordenende vouw over een datumbron (eerste, laatste, minimum of maximum over een rol), die deze tak alleen in de waarde van een regel vouwt): vroeg | EXCLUDE drl-transpiler MARKER: regel: vroeg — TODO voorwaarde: de eerste van de vinddag van alle vondsten van de Jutter eerder is dan 01-03-2026 |
| c-voorwaarde-eigen | de datumvouw in een voorwaarde tegen een eigen datum (eerder dan zijn startdag) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: vroeg — regel (voorwaarde buiten Phase-B scope): vroeg | EXCLUDE transpiler SKIP: regel: vroeg — regel (voorwaarde met een ordenende vouw over een datumbron (eerste, laatste, minimum of maximum over een rol), die deze tak alleen in de waarde van een regel vouwt): vroeg | EXCLUDE drl-transpiler MARKER: regel: vroeg — TODO voorwaarde: de eerste van de vinddag van alle vondsten van de Jutter eerder is dan zijn startdag |
| d-aantal-plus | het aantal vondsten plus 1 (k1b) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-afgerond | ((de som …) gedeeld door 3) rekenkundig afgerond op 2 decimalen | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-begrensd | het aantal vondsten min 2, met een minimum van 0 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: telling — regel (aggregatie-in-arithmetiek buiten scope): telling | PLAYED 1 | PLAYED 1 |
| d-gedeeld | (de som …) gedeeld door 4 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-genavigeerd | het aantal vondsten plus de bonus van de strandvoogd van de Jutter | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: telling — regel (aggregatie-in-arithmetiek buiten scope): telling | PLAYED 1 | PLAYED 1 |
| d-maal | het aantal vondsten maal 2,5 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-max-plus | (de maximale waarde van het gewicht …) plus 1 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-d-max-plus.json: attribuut telling: verwacht 1, werkelijk leeg; attribuut telling: verwacht 1, werkelijk leeg | PLAYED 1 | FAIL case1-d-max-plus.json: attribuut telling: verwacht 1, werkelijk leeg; attribuut telling: verwacht 1, werkelijk leeg |
| d-min-plus | (de minimale waarde van het gewicht …) plus 1 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-d-min-plus.json: attribuut telling: verwacht 1, werkelijk leeg; attribuut telling: verwacht 1, werkelijk leeg | PLAYED 1 | FAIL case1-d-min-plus.json: attribuut telling: verwacht 1, werkelijk leeg; attribuut telling: verwacht 1, werkelijk leeg |
| d-percentage | 10% van de som van het gewicht | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-d-percentage.json: attribuut uitkomst: verwacht 1.90, werkelijk leeg; attribuut uitkomst: verwacht 0.00, werkelijk leeg; attribuut uitkomst: verwacht 0.00, werkelijk leeg; attribuut uitkomst: verwacht 0.90, werkelijk leeg | PLAYED 1 | PLAYED 1 |
| d-som-fallback-plus | (de som … of 0 als die er niet zijn) plus 1 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-som-plus | (de som van het gewicht …) plus 1 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-twee-vouwen | het aantal vondsten plus de som van het gewicht | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-verminderd | (de som …) verminderd met 1 | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| e-eerste | de eerste datum twee rollen diep (alle vondsten van alle beheerde jutters) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van alle beheerde jutters van de Strandvoogd |
| e-getal | controle: de minimale waarde van een getal twee rollen diep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | PLAYED 1 |
| e-laatste | de laatste datum twee rollen diep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: laatste vinddag — regel (waarde buiten scope): laatste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: laatste vinddag — TODO aggregatie LAATSTE: de laatste van de vinddag van alle vondsten van alle beheerde jutters van de Strandvoogd |
| f-groep-leest-vouw | een defaultsgroep waarvan de rechtvaardiging de vouwuitkomst leest; de groep eerst | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| f-groep-lid | vouw over een datum die een defaultsgroep schrijft; de vouw eerst | oracle: PASS (8 expectations) | clingo: PASS (8 expectations) | EXCLUDE transpiler SKIP: regel: eerste bergdag — regel (waarde buiten scope): eerste bergdag | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| f-jutterzijde | de relatie ingevoerd vanaf de Jutter (de vondsten zijn …) | oracle: PASS (8 expectations) | clingo: PASS (8 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| f-keten-na | een regel die de vouwuitkomst leest, vóór de vouwregel in de bron | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| f-kwantor | de vouw als waarde onder een kwantorvoorwaarde (de vinddag van alle vondsten gevuld) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| f-niet-zwaar | filter met een ontkend kenmerk (die niet zwaar zijn) | EVALUATION FAULT: Unresolved condition reached in evaluation | TRANSLATION FAULT: Unresolved condition reached in translation: this rule fails observably | EXCLUDE transpiler SKIP: regel: laatste vinddag — regel (waarde buiten scope): laatste vinddag | EXCLUDE transpiler SKIP: regel: laatste vinddag — regel (vouw LAATSTE over een setbron die deze tak niet in een waarde vouwt: de laatste van de vinddag van alle vondsten van de Jutter die niet zwaar zijn): laatste vinddag | EXCLUDE drl-transpiler MARKER: regel: laatste vinddag — TODO aggregatie LAATSTE: de laatste van de vinddag van alle vondsten van de Jutter die niet zwaar zijn |
| f-pad-filter | twee rollen diep met een voorwaarde op de elementen (die zwaar zijn) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van alle beheerde jutters van de Strandvoogd die zwaar zijn |
| f-voorwaarde-eigen | de vouw als waarde onder een eigen voorwaarde (zijn startdag gevuld) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (waarde buiten scope): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |
| m-max | het maximum over datums als hele waarde (handgeschreven) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: laatste vinddag — regel (vouw MAXIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal accumuleren. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref en de ASP-backend dragen haar, getuige golden/jutterswet-vondstbereik — maar deze tak heeft haar niet gebouwd. Een gat in deze backend, geen semantisch verschil (§5.8.3)): laatste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: laatste vinddag — vouw MAXIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3) |
| m-min | het minimum over datums als hele waarde (handgeschreven) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal accumuleren. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref en de ASP-backend dragen haar, getuige golden/jutterswet-vondstbereik — maar deze tak heeft haar niet gebouwd. Een gat in deze backend, geen semantisch verschil (§5.8.3)): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3) |
| m-min-filter | het minimum over de zware vondsten, zwaar later afgeleid (handgeschreven) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal accumuleren. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref en de ASP-backend dragen haar, getuige golden/jutterswet-vondstbereik — maar deze tak heeft haar niet gebouwd. Een gat in deze backend, geen semantisch verschil (§5.8.3)): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3) |
| m-min-pad | het minimum over datums twee rollen diep (handgeschreven) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: eerste vinddag — regel (vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal accumuleren. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref en de ASP-backend dragen haar, getuige golden/jutterswet-vondstbereik — maar deze tak heeft haar niet gebouwd. Een gat in deze backend, geen semantisch verschil (§5.8.3)): eerste vinddag | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: eerste vinddag — vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3) |
| m-min-plus-duur | (het minimum over datums) plus 30 dagen (handgeschreven) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: meldgrens — regel (aggregatie-in-arithmetiek buiten scope): meldgrens | PLAYED 1 | EXCLUDE drl-transpiler MARKER: regel: meldgrens — vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3) |
| m-min-voorwaarde | het minimum over datums in een voorwaarde (handgeschreven) | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | EXCLUDE transpiler SKIP: regel: vroeg — regel (voorwaarde buiten Phase-B scope): vroeg | EXCLUDE transpiler SKIP: regel: vroeg — regel (voorwaarde met een ordenende vouw over een datumbron (eerste, laatste, minimum of maximum over een rol), die deze tak alleen in de waarde van een regel vouwt): vroeg | EXCLUDE drl-transpiler MARKER: regel: vroeg — vouw MINIMUM over de setbron 'vinddag' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik — maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3) |
| h-let | een telling als gebonden waarde van Daarbij geldt (een Let), plus 1 | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | EXCLUDE transpiler SKIP: regel: telling — regel (waarde buiten scope): telling | PLAYED 1 | PLAYED 1 |
| h-let-deling | een telling onder een Let, gedeeld door een eigen attribuut onder dezelfde Let | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | EXCLUDE transpiler SKIP: regel: telling — regel (waarde buiten scope): telling | EXCLUDE transpiler SKIP: regel: telling — regel (een vouw over een setbron en een deling of wortel onder één Let of IfThenElse, ch. 4.10): telling | PLAYED 1 |
| g-pct | objectcreatie met 10% van een som als toekenning, teruggelezen | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-creatie.json: attribuut heffing: verwacht 1.10, werkelijk leeg; attribuut heffing: verwacht 0.00, werkelijk leeg | idem | PLAYED 1 |
| g-min-plus | objectcreatie met (het minimum van een gewicht) plus 1, teruggelezen | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-creatie.json: attribuut weeggrens: verwacht 1, werkelijk leeg | idem | FAIL case1-creatie.json: attribuut weeggrens: verwacht 1, werkelijk leeg |
| g-eerste | objectcreatie met de eerste vinddag als toekenning | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | EXCLUDE transpiler SKIP: objectcreatie: maak een afrekening — objectcreatie (toekenning-waarde buiten scope): maak een afrekening | idem | EXCLUDE drl-transpiler MARKER: objectcreatie: maak een afrekening — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter |

Samengevat over de 43 vormen a-* tot en met f-*: ref `oracle: PASS` op 42 en een `EVALUATION FAULT` op f-niet-zwaar
(de frontend laat "die niet zwaar zijn" als onopgeloste voorwaarde op de draad; clingo weigert haar ook); clingo
`clingo: PASS` op 41, `TRANSLATION FAULT` op a-tijdstip (een datum-en-tijd over een set) en f-niet-zwaar. Exectak vóór:
9 PLAYED, 3 FAIL (d-min-plus, d-max-plus, d-percentage), 31 EXCLUDE; na: 40 PLAYED, 3 EXCLUDE met de vorm in de zin
(c-voorwaarde en c-voorwaarde-eigen, open punt 1; f-niet-zwaar, waar ref en clingo zelf falen). DRL-tak vóór en na: 12
PLAYED, 3 FAIL, 28 EXCLUDE. De zes m-vormen: ref en clingo `PASS` op alle zes; exectak vóór 6 EXCLUDE, na 5 PLAYED en
m-min-voorwaarde met naam geweigerd; DRL-tak vóór en na EXCLUDE. Van de flanken speelt h-let na de fix en weigert
h-let-deling met naam (open punt 6); de drie objectcreaties zijn ongewijzigd (open punt 3). Uit het corpus:
`jutterswet-vondstvenster` (eerste/laatste over een set) exectak vóór `EXCLUDE transpiler SKIP: regel: eerste vondstdag
van een jutter — regel (waarde buiten scope): eerste vondstdag van een jutter`, na `PLAYED 2`;
`jutterswet-vondstbereik` (minimum/maximum over een datumbron) exectak vóór `EXCLUDE transpiler SKIP: regel: vroegste
vondstdag van een jutter — regel (vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Datum
in dagen en niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal accumuleren. De ordenende lezing KIEST
…)`, na `PLAYED 2`.

Meetnoot bij de frontend: een vouw direct gevolgd door "plus" of "min" leest zij als setlezing ("de eerste van de vinddag
van alle vondsten van de Jutter plus 30 dagen": `setlezing buiten een vouwbron 'de vinddag van alle vondsten van de
Jutter': hop 0 reaches the to-many role 'vondst' in a scalar slot …`); met haakjes gaat het. De vormen c-plus-duur,
c-min-duur, d-*-plus en de golden schrijven daarom de vouw tussen haakjes. De spelling "de minimale waarde van" over een
datum typeert zij weg (`minimale waarde van vereist numerieke operanden, kreeg Datum in dagen`), vandaar de
handgeschreven m-vormen.

### (b) Getuigen

`DatumvouwOverRolWitnessTest` (exectak, vijftien rijen, documenten in de klasse op Jutterswet-materiaal): de eerste en
laatste datum over een rol, gesteld en geïnitialiseerd, een voorwaarde op de elementen (een later afgeleid kenmerk en
een toets op de gevouwen datum), een later afgeleide datum, de datumvouw plus en min een duur, een eigen datum plus het
maximum van een duur (de vorm van de regelsetbouwer), de tijdsduur tussen eerste en laatste, twee rollen diep, een
datum-en-tijd, de getalvouwen als operand (minimum en maximum van een lege bron plus 1, een percentage van een som, een
begrenzing om een telling, een telling plus een genavigeerde lezing), een vouw onder een Let, een vouw naast een deling
onder een Let (weigert met naam), het minimum en maximum over datums (kaal en plus een duur), een ordenende datumvouw in
een voorwaarde (eerste en minimum, weigert met naam), en een flank met de getalvouwen die al speelden. Elk document
apart nagemeten: `verifier: PASS`, `oracle: PASS`, `clingo: PASS` behalve de datum-en-tijd (`TRANSLATION FAULT:
datum-tijd here is not the pair term's arithmetic …`).

`DatumSetvouwWitnessTest` (tweede ronde): de rij die de weigering van het minimum over een datumbron in beide takken
pinde, is twee rijen geworden: `de_ordenende_vouw_over_een_datumbron_speelt_in_de_exectak` (beide cases van
`jutterswet-vondstbereik` met de waarden van ref) en `de_drl_tak_weigert_de_ordenende_datumvouw_bij_naam` (de zin noemt
de DRL-tak). De rijen over de optellende vouw en de tekstbron en de rij over een getalbron zijn ongewijzigd.

`DrlEenheidsplekWitnessTest` (tweede ronde): de getuige herkende de datumkant van een datum plus een duur alleen aan de
eigen typeannotatie van de knoop; hij leest nu, zoals de harnas van gate 13 (`gedeclareerd`), de declaratie die een
verwijzing noemt (via de supertypeketen, een parameter, een dimensiecel) en de binding van een Let.

Rood op de claim (`b89da841`, twaalf rijen):

    Tests run: 12, Failures: 2, Errors: 9, Skipped: 0
    een_datumvouw_in_een_voorwaarde_weigert_met_de_vorm:196 transpiler SKIP: regel: vroeg — regel (voorwaarde buiten Phase-B scope): vroeg ==> expected: <true> but was: <false>
    een_getalvouw_als_operand:154 het minimum plus 1 ==> expected: <null> but was: <case: attribuut telling: verwacht 1, werkelijk leeg; attribuut telling: verwacht 1, werkelijk leeg>
    acht rijen » Excluded transpiler SKIP: … regel (waarde buiten scope): …
    een_datum_plus_een_getalvouw_als_duur:113->speel:414 » Excluded transpiler SKIP: regel: meldgrens — regel (aggregatie-in-arithmetiek buiten scope): meldgrens

De flank was groen. De twee Let-rijen (`ce69e231`) rood op de claim (`Tests run: 2, Failures: 1, Errors: 1`: de
deling-rij `transpiler SKIP: regel: uitkomst — regel (waarde buiten scope): uitkomst ==> expected: <true> but was:
<false>`, de telling-rij `Excluded transpiler SKIP: regel: telling — regel (waarde buiten scope): telling`) en op de kop
ervoor, `a9d0c788`, waar de weigering breder stond (`Tests run: 14, Failures: 1, Errors: 1`, beide met `regel (een vouw
over een setbron onder een Let of IfThenElse, ch. 4.10)`). De rijen van de tweede ronde (`e80b1aec`) rood op de kop
ervoor (`Tests run: 21, Failures: 2, Errors: 2`):

    DatumSetvouwWitnessTest.de_drl_tak_weigert_de_ordenende_datumvouw_bij_naam:100 drl-transpiler MARKER: regel: vroegste vondstdag van een jutter — vouw MINIMUM over de setbron 'vondstdatum' van Vondst: die leden zijn van type Datum in dagen en niet getalachtig, terwijl beide takken elke waardevouw in BigDecimal accumuleren …
    DatumvouwOverRolWitnessTest.een_datumvouw_in_een_voorwaarde_weigert_met_de_vorm:200 first: transpiler SKIP: regel: vroeg — regel (voorwaarde met een datumvouw (EERSTE/LAATSTE) over een rol, die deze tak alleen in de waarde van een regel vouwt): vroeg ==> expected: <true> but was: <false>
    DatumSetvouwWitnessTest.de_ordenende_vouw_over_een_datumbron_speelt_in_de_exectak:92 » Excluded transpiler SKIP: regel: vroegste vondstdag van een jutter — regel (vouw MINIMUM over de setbron 'vondstdatum' van Vondst: …)
    DatumvouwOverRolWitnessTest.het_minimum_en_het_maximum_over_datums:211->speel:471 » Excluded transpiler SKIP: regel: eerste vinddag — regel (vouw MINIMUM over de setbron 'vinddag' van Vondst: …)

`DrlEenheidsplekWitnessTest` rood met de nieuwe zoekgrens in de golden, vóór de reparatie (`Tests run: 1, Failures: 0,
Errors: 1`): `Documentfout malformed uchar-ir-v1 document at '/rules/rules/4/versions/0/value/right': date ± duration:
the duration unit 'dag-verdraaid7' is neither a recognised time unit nor a declared day kind`; na de reparatie groen.
Na beide rondes: `DatumvouwOverRolWitnessTest` 15 tests, `DatumSetvouwWitnessTest` 6, `DrlEenheidsplekWitnessTest` 1,
0 failures; de suite 922 tests, 0 failures, 0 skipped (906 op de claim; vloer 239).

### (c) Golden `jutterswet-vroegste-vondst` (`a498f118`, zoekgrens `a0eb2079`)

Tien regels: de eerste en laatste vinddag van een Jutter over zijn vondsten, de eerste zware vinddag (zwaar door een
regel die na elke vouw staat), de meldgrens "(de eerste van …) plus 30 dagen", de zoekgrens "zijn startdag plus de
maximale waarde van de zoekduur van alle vondsten van de Jutter" (de vorm van de regelsetbouwer: een eigen datum plus
een getalvouw in dagen), de vondstspanne (de tijdsduur van de eerste tot de laatste in hele dagen), de weeggrens "(de
minimale waarde van het gewicht …) plus 1", de heffing "10% van de som van het gewicht …", de eerste vinddag van een
Strandvoogd over de vondsten van al zijn beheerde jutters, en zwaar. Twee cases: de vier bronvormen (gevuld met een lege
bijdrager, geen vondsten, alleen een lege bijdrager, één bijdrager), en een case waarin de volgorde van de leden niet
die van de waarden is, met een gelijkspel. Handgerekend in NOTE.md. De IR uit `--ir bron.rgs` alleen is byte-gelijk aan
die met het scenario.

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-vier-bronvormen | 42 | `oracle: PASS (42 expectations)` | `clingo: PASS (42 expectations)` | EXCLUDE (golden) | PASS | EXCLUDE (golden) | 26 MISMATCH |
| case2-volgorde-en-gelijkspel | 22 | `oracle: PASS (22 expectations)` | `clingo: PASS (22 expectations)` | idem | PASS | idem | 17 MISMATCH |

Per golden: exectak na `PLAYED 2`; exectak vóór `EXCLUDE transpiler SKIP: regel: eerste vinddag van een jutter — regel
(waarde buiten scope): eerste vinddag van een jutter`; DRL-tak `EXCLUDE drl-transpiler MARKER: regel: eerste vinddag van
een jutter — TODO aggregatie EERSTE: de eerste van de vinddag van alle vondsten van de Jutter`.

Toegelaten in de eerste ronde via `scripts/admit.sh` met het gate-12-voorstel in de werkboom: `admit: ADMITTED
corpus/golden/jutterswet-vroegste-vondst (2 case(s); verifier-clean, ref = clingo, no-op refuted, gates green)`. Toen
telde de zoekgrens de duur op bij de eerste vinddag, omdat `DrlEenheidsplekWitnessTest` de vorm met een eigen datum niet
droeg (de rode meting hierboven). In de tweede ronde is die getuige gerepareerd en draagt de golden de vorm van de
regelsetbouwer; `admit.sh` weigert een bestaande bestemming, dus de gewijzigde golden is per uitvoerder nagemeten
(`verifier: PASS`, de tabel hierboven) en door de zestien gates.

### Gate-12-voorstel (`74efcccc` en `1fa10fb1`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 226 → 229, in twee redenblokken na dat van jutterswet-opgave-subtype: "226 -> 228 (2026-09-30,
  the date fold over a role)" (`jutterswet-vondstvenster` verlaat `EXPECTED_CHANNEL` en speelt `PLAYED 2` tegen de
  ongewijzigde pinnen; `jutterswet-vroegste-vondst` komt nieuw binnen) en "228 -> 229 (2026-09-30, the minimum and
  maximum over a date)" (`jutterswet-vondstbereik` verlaat `EXPECTED_CHANNEL` en speelt `PLAYED 2`).
- `EXPECTED_CHANNEL`: de regels van vondstvenster en vondstbereik weg.
- `DRL_CHANNEL` klasse (C): `"jutterswet-vroegste-vondst|EXCLUDE|MARKER regel: TODO aggregatie EERSTE over a set
  source"` op naamvolgorde; de reden bij vondstbereik zegt dat de zin van `kern.Vouwwaarde` nu de DRL-tak noemt en dat
  de exectak de golden speelt; kop 10 → 11 regels met een geschiedenisregel, "SEVEN of the eleven" weigert de exectak
  ook, "FOUR" alleen deze tak (vouwrand, vondstbereik, vondstvenster, vroegste-vondst). Klasse (A): het gat 35 → 38
  ("The 38 is 42 exec-only players …") en twee hertelzinnen (228 - 191, 229 - 191; A 37, B 37, C 11, D 2, E 3, F 3 = 93
  regels); lijstkop "eighty-seven of the ninety-three".
- `EXPECTED_DRL_PLAYED` blijft 191, `EXPECTED_MIN_TESTS` blijft 239, `EXCLUDE_KNOWN` ongewijzigd.

### (d) Sweep

Over alle 284 goldens (met de nieuwe golden), elke golden met al zijn cases, statusregel per golden, jar van de claim
tegen jar van de kop: in de exectak precies drie regels anders, `jutterswet-vondstbereik`, `jutterswet-vondstvenster` en
`jutterswet-vroegste-vondst`, elk van `EXCLUDE …` naar `PLAYED 2`, PLAYED 226 → 229; in de DRL-tak hetzelfde kanaal voor
alle 284 (PLAYED 191) en één regel met een andere tekst, vondstbereik, waarvan de weigering nu de DRL-tak noemt. Elke
golden die een vouw in een berekening al speelde via `lowerVal`, speelt via de slots met dezelfde statusregel. De
beangeneratie is niet aangeraakt.

### Gates

Alle zestien in de volgorde van `gates.yml`, op de kop `433c5a30` met een schone werkboom: `eval $(opam env)`, `dune
build`, dan elk script onder zijn shebang (bash voor catala, regelspraak, toka-parity, drools, flogic, graaf en
scenario; sh voor de negen andere), plus `dune build @ref/runtest` na gate 5. Catala aanwezig. Per gate de exitcode en
de laatste niet-lege regel van de uitvoer:

    dune build: exit 0
    check-self-contained (sh, 1s): exit 0 — check-self-contained: PASS
    check-ir-schema (sh, 24s): exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 323 file(s) validate and round-trip, floor 192; 42542 object(s) in 824 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema (sh, 2s): exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 812 case file(s) validate)
    check-verifier (sh, 13s): exit 0 — check-verifier: PASS (323 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle (sh, 5s): exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 772 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    dune build @ref/runtest: exit 0
    check-clingo (sh, 154s): exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 772 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop (sh, 4s): exit 0 — check-noop: PASS (728 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood (sh, 18s): exit 0 — check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala (bash, 3s): exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak (bash, 25s): exit 0 — check-regelspraak: PASS — JUnit suite green (1008 tests, floor 173); 111 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity (bash, 49s): exit 0 —     Drools excludes by form:
    check-drools (bash, 259s): exit 0 —   JUnit 922 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation (sh, 85s): exit 0 —   corpus-wide accounting: 91 unit-bearing annotations = 45 twistable + 46 exempt (the annotation IS the source) + 0 without a convertible sister + 0 at a §2.10 fourth-class branch (v0 derives no result unit) + 0 outside the twist table
    check-flogic (bash, 0s): exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf (bash, 11s): exit 0 — check-graaf: PASS
    check-scenario (bash, 6s): exit 0 — check-scenario: PASS — 18 scenario golden(s) (floor 7) regenerate 71 case file(s) (floor 31) and their ir.json byte-identically

Waar de laatste regel niet de PASS-regel is: gate 11 `check-toka-parity: PASS` (daarna de samenvatting, met als laatste
regel de lege lijst "Drools excludes by form:"); gate 12 `check-drools: PASS`, `played 229`, `channel floor 55 non-PLAYED
goldens pinned by name; played matches the pinned 229`, `DRL-text branch played 191 (… pin 191)`; gate 13
`check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce
the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`, daarna zijn
boekhouding; gate 15 ook `check-graaf: route leg — the graph of the wire equals the graph of the source on all 111
goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)`. Eerder groen: op de claim (`f4bb0a02`; gate 12
played 226, DRL-text 191, JUnit 906), na de eerste ronde op `be0eae73` (played 228, DRL-text 191, JUnit 920), en in de
toelating van de golden (`scripts/admit.sh`).

### README (`be0eae73`, `433c5a30`)

`backends/drools/README.md`, eigen sectie "Recorded: a fold over a set source inside a rule's value": wat de exectak nu
doet (goldens, getuigen), wat daarvoor weigerde of fout antwoordde, de drie grenzen die blijven (een ordenende datumvouw
in een voorwaarde, een vouw naast een deling of wortel onder één Let, de objectcreatie), en de DRL-tak.

### Afwijkingen van de kaart

1. Acceptatie (d) zegt "played-telling en kanaal van gate 12 ongewijzigd voor de bestaande goldens". Twee bestaande
   goldens dragen precies deze vorm en spelen nu in de exectak, `jutterswet-vondstvenster` en `jutterswet-vondstbereik`:
   hun kanaalpinnen gaan in het voorstel weg, `EXPECTED_PLAYED` stijgt daardoor met drie. Geen andere bestaande golden
   verschuift van kanaal; in de DRL-tak verandert alleen de weigertekst van vondstbereik.
2. De fix gaat verder dan de datumvouw: elke vouw in de waarde van een regel loopt nu via de slots, in plaats van via
   `lowerVal`. De kaart vraagt de vormen "die met dezelfde lowering meegaan", en twee gemeten vormen van (a) antwoordden
   via `lowerVal` fout (d-percentage, d-min-plus/d-max-plus).
3. Twee bestaande testbestanden gewijzigd, in de tweede ronde, op aanwijzing en met toestemming van de coördinator:
   `DatumSetvouwWitnessTest` (de weigering van het minimum en maximum over een datumbron is alleen voor de DRL-tak
   gepind, de exectak speelt de golden) en `DrlEenheidsplekWitnessTest` (herkent een eigen datumattribuut als datum).
4. Nieuwe of preciezere weigerzinnen: een ordenende datumvouw in een voorwaarde noemt nu de vorm (was `voorwaarde buiten
   Phase-B scope`), een vouw naast een deling onder een Let weigert met een eigen zin in plaats van `waarde buiten scope`,
   en de datumzin van `kern.Vouwwaarde` noemt de DRL-tak in plaats van "beide takken".

### Voorgestelde registertekst (voor de hoofdsessie)

- Geen rij in `spec/CONSTRUCTS.md` en geen tekst in `spec/`: RUNTIME-SEMANTICS ch. 1.2e schrijft de ordenende vouw al
  voor en ASP-TRANSLATION zegt niets over de Drools-dekking; de exectak voldoet er nu aan.
- `corpus/golden/jutterswet-vondstvenster/NOTE.md` (buiten de may-touch van deze kaart), in de inleiding: "The Drools
  backend carries neither of its transpiler branches for this fold and refuses the golden per form with a named reason
  (gate 12's channel list), so the square is a two-executor agreement here and says so." vervangen door "The Drools
  backend's exec-model branch plays it since 2026-09-30 (card drools-exec-datumvouw-over-rol: the fold as an accumulate
  over a Comparable), so the square is ref ≡ clingo ≡ Drools-exec; the DRL-text branch refuses it per form with a named
  marker (gate 12's DRL channel list)."
- `corpus/golden/jutterswet-vondstbereik/NOTE.md` (idem), de zin "The Drools backend carries neither of its transpiler
  branches for this fold — both accumulate in `BigDecimal` and a date-valued member has no place there — and refuses the
  golden per form with a named reason in gate 12's two channel lists. The square is a two-executor agreement here and
  says so." vervangen door "The exec-model branch of the Drools backend plays it since 2026-09-30 (card
  drools-exec-datumvouw-over-rol: the same accumulate over a Comparable that folds its first/last twin), so the square is
  ref ≡ clingo ≡ Drools-exec; the DRL-text branch accumulates every value fold in `BigDecimal` and refuses the golden per
  form with a named reason in gate 12's DRL channel list."

### Open punten

1. Een ordenende vouw over een datum in een voorwaarde (c-voorwaarde, c-voorwaarde-eigen, m-min-voorwaarde) weigert de
   exectak met naam; ref en clingo spelen haar. `aggregatieVoorwaarde` vergelijkt het accumulatieresultaat als getal
   (`rsAggVergelijk` met `toBd`); een datum vraagt daar een arm met `datumVergelijkTemporal`.
2. Opgelost in de tweede ronde: het minimum en maximum over een datumbron spelen in de exectak (`jutterswet-vondstbereik`
   PLAYED 2); alleen de DRL-tak weigert ze nog, met een zin die dat zegt.
3. Een objectcreatie verlaagt een vouw in een toekenning nog via `lowerVal` en rekent fout, vóór en na deze kaart:
   g-pct `FAIL case1-creatie.json: attribuut heffing: verwacht 1.10, werkelijk leeg; attribuut heffing: verwacht 0.00,
   werkelijk leeg` (de DRL-tak speelt haar); g-min-plus `FAIL case1-creatie.json: attribuut weeggrens: verwacht 1,
   werkelijk leeg` (over een lege bron maakt de regel geen object; de DRL-tak faalt ook); een EERSTE-vouw in een
   toekenning weigert (`toekenning-waarde buiten scope`). Dezelfde slots zouden in de consequentie van de creatie passen.
4. De DRL-teksttak markert de eerste/laatste-vouw over een set (`TODO aggregatie EERSTE`), weigert het minimum en
   maximum over een datum, en antwoordt fout op het minimum of maximum van een lege bron plus 1 (d-min-plus, d-max-plus:
   `verwacht 1, werkelijk leeg`) en op een datum plus het maximum van een lege bron (c-datum-plus-getalvouw: `verwacht
   2026-03-01, werkelijk leeg`); ongewijzigd.
5. Opgelost in de tweede ronde: `DrlEenheidsplekWitnessTest` leest het gedeclareerde type van een verwijzing, en de
   golden draagt "zijn startdag plus de maximale waarde van de zoekduur …".
6. Een vouw naast een deling of wortel onder één Let of IfThenElse (h-let-deling) weigert de exectak met naam; ref,
   clingo en de DRL-tak spelen haar. De padsonde van de companion (`sondeConsequentie`) zou de accumulates van de
   gastregel moeten meekrijgen.
7. Een datum-en-tijd over een set (a-tijdstip) speelt de exectak met de waarde van ref; de ASP-backend weigert de vorm
   (`TRANSLATION FAULT: … a set-typed fold …`), dus zij staat alleen in de getuige.
8. De frontend leest een vouw direct gevolgd door "plus" of "min" als setlezing en weigert de zin (meetnoot bij (a)), en
   laat "die niet zwaar zijn" in een subselectie als onopgeloste voorwaarde staan (f-niet-zwaar); buiten deze kaart.
9. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee.

### Commits

    f4bb0a02 claim: drools-exec-datumvouw-over-rol
    b89da841 DatumvouwOverRolWitnessTest: vouw over een rol in de waarde, rood
    a9d0c788 Exectak: een vouw over een rol in de waarde wordt een accumulate
    a498f118 golden jutterswet-vroegste-vondst: datumvouwen over een rol
    74efcccc voorstel gate 12: vondstvenster en vroegste-vondst spelen in de exectak
    ce69e231 DatumvouwOverRolWitnessTest: een vouw onder een Let, rood
    26e751db Exectak: een vouw onder een Let weigert alleen naast een deling
    be0eae73 backends/drools/README: een vouw over een setbron in de waarde
    73727d0a Result van de kaart drools-exec-datumvouw-over-rol
    e80b1aec Getuigen: minimum en maximum over datums spelen in de exectak, rood
    69f56fbd Exectak: minimum en maximum over datums vouwen als eerste en laatste
    08b76c0a DrlEenheidsplekWitnessTest: een eigen datumattribuut is een datum
    a0eb2079 golden jutterswet-vroegste-vondst: de zoekgrens telt bij de startdag op
    1fa10fb1 voorstel gate 12: vondstbereik speelt in de exectak
    433c5a30 backends/drools/README: minimum en maximum over datums spelen ook

en de commit die dit bijgewerkte Result in de kaart zet. Niet gepusht, niet gemerged.
