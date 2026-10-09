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

## Result (2026-10-09)

*Geschreven door de hoofdsessie. De eigenaar was offline en vroeg bij twijfel een Fable-adviseur te raadplegen; het
besluit boven is dus VOORLOPIG, en de kaart stopt vóór de merge. Draait de eigenaar het besluit om, dan vervallen
stap 2 en de spectekst, en blijven van stap 1 de volgorde van de strata en de consistenties na een creatie staan (die
lossen bestaande bugs op, zie onder).*

| Commit | Wat |
|---|---|
| `b662d566` | claim |
| `dabfb3b5` | ref: creaties in generaties per stratum, budget in generaties (stap 1) |
| `f0d8c5fe` | ref: voorbeeld voor een feitcreatie in de generatie |
| `b14b5df0` | ref: na een creatie komen de strata op volgorde tot rust (lensfix) |
| `215cd772` | ref/README: generaties, het generatiebudget, de strata na een creatie (lensfix) |
| `a165fda8` | clingo: een initialisatie die op haar creatiekring leest, weigert (stap 2) |
| `6f711231` | VOORSTEL spec ch. 5.5, 6.1, 6.3: generaties binnen een stratum |
| `f15b5953` | ref: consistenties en uniciteiten vuren na elk stratum in rust (lensfix) |
| `aee2bdb4` | clingo: alleen een lid dat uitsluitend creaties schrijven, blijft vrij (lensfix) |
| `4893545f` | VOORSTEL spec ch. 5.5: uitzondering alleen voor een lid van creaties |
| `e1254f72` | VOORSTEL spec ch. 5.5: toetsen na elk stratum in rust |
| `792776e2` | VOORSTEL IR-SPEC ch. 5.5: clingo neemt de tweede houding voor een vorm (eindlens) |
| `ba3aac2f` | VOORSTEL spec ch. 5.5: absolute uitkomsten van de toetsen na een creatie (eindlens) |
| `cc9aa3de` | ref: regelgroepen staan niet meer bij wat niet geïmplementeerd is (eindlens; README en één commentaar) |

### Wat er staat

**Ref** (`ref/lib/eval.ml`) evalueert per stratum k in generaties. Generatie 0 is het fixpunt van de vormen die niets
maken (regels, verdelingen, defaults-groepen van stratum ≤ k). In generatie G + 1 leest elke creatie van stratum ≤ k
(feitcreaties en objectcreaties) dezelfde uitgerekende wereld van generatie G en schrijft in een aparte wereld; daarna
komen de vormen die niets maken stratum voor stratum tot rust (0, 1, …, k, elk tot zijn fixpunt, de lagere vormen
meegerekend zodat een tweeschrijversconflict over strata gemeld blijft), en na elk stratum vuren de consistenties,
uniciteiten en uniciteitsunies van dat stratum. Een stratum is klaar als een generatie niets maakt of linkt. Omdat de
lus het budget opnieuw vastlegt, bevat deze kaart ook het ref-deel van het voorlopige besluit van
`20261009-creatiebudget-ref-en-clingo` (acceptatie (a) daar): ten hoogste 100 generaties per stratum die maken of
linken (een 101e geeft `no fixpoint within 100 generations — generation 101 still mints or links (…)`), en voor de
vormen die niets maken per stratum het grootste van 100 en N_j + 1 rondes, met de bestaande zin over twee schrijvers
die het niet eens zijn.

**Clingo** (`backends/clingo/lib/translate.ml`) kan het moment van maken niet uitdrukken en weigert daarom luid en bij
naam (de tweede houding van IR-SPEC ch. 5.5) elke initialisatie van een objectcreatie die een knoop leest die een vorm
op haar door WF-045 toegelaten kring nog kan veranderen: wat een regel, verdeling of defaults-groep op de kring
schrijft, of een populatie of relatie die een creatie op de kring laat groeien. Een lid dat uitsluitend creaties
schrijven, blijft gedragen. De toets leest de afhankelijkheidsgraaf van `Ir_v0.Checks`, dezelfde als ref voor de strata;
hij weigert daardoor ruimer dan nodig (een regel op de kring waarvan de waarde bij het maken al vastligt, wordt ook
geweigerd). Geen golden heeft een geweigerde vorm.

**Spec** (beschermd pad, VOORSTEL als diff op de branch): een alinea "Generations" in ch. 5.5 met het document van de
kaart, de kopievariant en de feitcreatie als uitgewerkte voorbeelden en de weigering van clingo; aanvullingen in ch. 6.1
(stratum-strikt; de restpost "a value that grows"; de rolcheck, die de verifier sinds 2026-10-06 met WF-045 weigert) en
ch. 6.3. De letterlijke tekst staat in de diff van de specommit(s).

**Drools**: ongewijzigd. De exec-modeltak en de DRL-teksttak geven op het document van de kaart al 0, en vervallen met
de omschakeling en de DRL-kaart. De tak met truth maintenance geeft 0, maar leest de creaties van één generatie niet
tegen één wereld: vorm V2 (een initialisatie telt een relatie die een feitcreatie op de kring legt) geeft daar 1, ref nu
0. Dat neemt `20261009-creatiebudget-ref-en-clingo` mee, die de driver toch raakt.

### Acceptatie

- (a) Ref in generaties: gedaan; voorbeelden in `ref/test/` (rood eerst gemeten):
  `WV-5.5-generaties-keten` (2 volgordes), `WV-5.5-generaties-kopie` (6), `WV-5.5-generaties-feitcreatie` (2),
  `WV-5.5-budget-grens-{99,100,101}-generaties` (elk 2), `WV-5.5-budget-regelketen-105` (2),
  `WV-6.3-na-een-creatie` (2), `WV-5.7-twee-schrijvers-twee-strata` (2, rood op een mutant),
  `WV-6.3-consistentie-na-een-creatie` (2), `WV-6.3-uniciteit-na-een-creatie` (2). Suite 1154 → 1266.
- (b) Clingo weigert: gedaan; checks in `backends/clingo/test` (suite 1988 → 2084), rood met de vertaler van
  vóór de stap; de recursieve goldens (`jutterswet-wachtersketen` als anker en als rgs, `jutterswet-opvolgerquorum`,
  `jutterswet-opvolgerversies`, `synth2-budgetgrens-onder`, `jutterswet-erflijn`) blijven spelen.
- (c) Drools ongewijzigd; zie boven voor V2.
- (d) Spectekst: voorstel op de branch. De restposten van ch. 6.1 opnieuw gemeten in ref: alle 17 rijen gelijk vóór en
  na.
- (e) Geen golden, negatief of rood geval erbij. Over 872 cases (801 golden, 42 rood, 29 toka) geven ref en clingo
  dezelfde uitvoer als vóór de kaart, en ref ook dezelfde volledige eindwereld (instanties, waarden, kenmerken, kanaal
  in volgorde); alleen de foutzin van `finite-chain-over-budget` en `runaway-recursion` noemt nu de generaties (hun pin
  "no fixpoint within" klopt).

Budgetgrens in ref (grootste aantal generaties dat speelt, vóór → na): de vorm van `finite-chain-over-budget` 100 →
100; de vorm van `synth2-budgetgrens-onder` (met regel en rangdetector) 99 → 100; de q2-vormen van het advies 98–100 →
100; de L-documenten van `TmsCreatiestratumWitnessTest` 99–100 → 100; de keten over drie strata 100 → 100 per keten.
Overal faalt de 101e generatie, in elke documentvolgorde.

Volledige gateset van de hoofdsessie op `e1254f72` (lokaal): alle 16 groen. Gate 5: `ref suite 1266 worked examples;
801 cases replayed exactly`; gate 6: `backend suite 2084 checks; 801 cases replay exactly through clingo — ref =
clingo`; gate 8: `42 red cases …; 84 pinned fragments`; gate 12: `check-drools: PASS`. Daarna veranderden alleen teksten
(IR-SPEC, RUNTIME-SEMANTICS, de README van ref en één commentaar in `eval.ml`); op `cc9aa3de` draaiden gates 1, 5 en 6
opnieuw, groen. Beschermde paden: `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md`, alleen als VOORSTEL in eigen
commits (de eigenaar merget); `scripts/` niet geraakt.

### Lensvondsten

Copilot (sol6, effort xhigh), vier runs (lens op stap 1, herkeuring van de fix, lens op stap 2, eindlens), elk één
premium request. De eindlens met Opus 5.5 is niet gedraaid (eerst
afstemmen met de eigenaar).

Aanvaard en gefixt:
- lens op stap 1, BLOKKEREND: ref liet na een creatie alle strata ≤ k samen in één pas in documentvolgorde lopen, zodat
  een vorm van een hoger stratum op een nieuw object een tussenstand las (bestaande bug, ook in de oude ref; kaart
  `20261009-ref-strata-na-een-creatie`) → `b14b5df0`;
- lens op stap 1, ADVIES: de README van ref noemde nog het passenbudget → `215cd772`;
- herkeuring van die fix, BLOKKEREND: een consistentie van een lager stratum vuurde niet meer op de objecten van een
  latere creatie (bestaande bug; nagemeten: ref "consistent", clingo en de Drools-takken "inconsistent") → `f15b5953`,
  met een variant voor uniciteit;
- lens op stap 2, BLOKKEREND: de spectekst en de README maakten de uitzondering te ruim (een attribuut dat een creatie
  schrijft); de code was goed → `aee2bdb4`, `4893545f`, met twee getuigen voor een gedeelde schrijver (rang, graad);
- lens op stap 2, twee ADVIEZEN: de geldigheid van een schrijver op de kring, en de uitkomst van ref in de
  weigeringsgetuigen → de keuze voor de statische graaf (ook een verlopen schrijver op de kring) vastgelegd in README,
  commentaar en spectekst, met een getuige; de geweigerde frames voeren nu ook `ref_matches` uit (`aee2bdb4`).

Eindlens (sol6, de hele diff op `e1254f72`):
- BLOKKEREND, aanvaard, opgelost door de volgende kaart: op deze tip alleen speelt ref een keten van 100 generaties met
  een berekende eigenschap, waar clingo nog `generation budget exhausted` geeft (vóór de kaart faalden beide; de
  q2-vorm zonder berekende eigenschap was al gesplitst). Het ref-deel van het budget landde hier omdat de nieuwe lus
  het budget vastlegt; het clingo-deel is acceptatie (b) van `20261009-creatiebudget-ref-en-clingo`, die bovenop deze
  branch staat. **Deze kaart landt dus niet zonder die kaart** (open punt 9). De verouderde budgetteksten (ch. 5.5 "the
  reference interpreter's pass budget", de README van clingo, `translate.ml`) gaan met die kaart mee. Geen golden of
  rood geval raakt de grens anders dan vóór de kaart (gates groen).
- BLOKKEREND, aanvaard: IR-SPEC ch. 5.5 deelde clingo in bij de houding die het gestratificeerde resultaat geeft →
  voorstel `792776e2` (geldt alleen als de eigenaar het voorstel voor RUNTIME-SEMANTICS ch. 5.5 overneemt).
- ADVIES, aanvaard: de README van ref noemde regelgroepen niet geïmplementeerd, het commentaar boven
  `run_object_creatie` "non-recursive", en de zin over de toetsen na een creatie gaf geen absolute uitkomsten →
  `cc9aa3de`, `ba3aac2f`.

Afgewezen: geen.

### Open punten (voor de eigenaar)

1. **Het voorlopige besluit zelf** (de generaties; Fable-advies vraag 1 in `/tmp/lens-bugs/advies-fable.md`), en de
   normvraag die het open laat: de generatiezin legt ook voor de voorwaarde van een creatie de lezing van ref vast,
   zodat de restpost "a value that grows" van ch. 6.1 een tekort van clingo wordt in plaats van een normvraag.
2. **IR-SPEC ch. 5.5** zegt over de tweede houding "Its instance is `backends/drools`". Voorgestelde zin na de
   Drools-beschrijving: "Since 2026-10-09 the ASP backend takes it for one form: a member initialisation of a creation
   that reads a node a form on the creation's own WF-045-admitted cycle writes (RUNTIME-SEMANTICS ch. 5.5,
   Generations)."
3. **CONSTRUCTS** (voorstel voor een rij): "**Generations (2026-10-09, card
   `creatie-initialisatie-leest-groeiende-waarde`):** a creation reads the settled world of the generation before its
   own (RUNTIME-SEMANTICS ch. 5.5); `ref/` evaluates so, the ASP backend refuses by name an initialisation that reads
   what a form on its creation's cycle writes."
4. **PLAN §9** (voorstel, bij de landing): "Creation proceeds in generations within a stratum, and a creation reads the
   settled world of the generation before its own (provisional decision 2026-10-09 on Fable advice, confirmed by the
   owner on …; card `creatie-initialisatie-leest-groeiende-waarde`); the reference interpreter evaluates so, the ASP
   backend refuses by name what it cannot carry."
5. **De weigering van clingo is ruimer dan nodig** (de graaf, niet de waarden); een nauwere toets is niet gebouwd.
6. **V2 in de tak met truth maintenance** gaat naar `20261009-creatiebudget-ref-en-clingo`.
7. **`20261009-ref-strata-na-een-creatie`**: deze kaart repareert de ref-kant van beide vormen (lensvondsten); die kaart
   houdt de goldens. Bij de landing haar tekst bijwerken.
8. **Bijvangst, niet onderzocht**: in een proefdocument gaf `zijn rang maal 10` met een gehele typering WF-091 ("the
   times composite is typed whole, but the type derived from its operands is fractional").
9. **Landing**: de branch staat op main `8beff67a`; main is verder gegaan met alleen kaarten. Vóór de ff-merge
   `git rebase main` en de gates opnieuw. `20261009-creatiebudget-ref-en-clingo` staat bovenop deze branch.
