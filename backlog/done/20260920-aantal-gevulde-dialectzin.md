# AANTAL_GEVULDE, kaart 3: de dialectzin in de RegelSpraak-frontend

Derde en laatste kaart onder het AANTAL_GEVULDE-ontwerp
(`backlog/done/20260919-aantal-gevulde-gemiddelde.md`, besluiten G1–G9 van
de maintainer, 2026-09-19). De wire, de verifierhekken en `ref` landden in
kaart 1 (`backlog/done/20260920-aantal-gevulde-wire.md`); clingo en beide
Drools-takken in kaart 2
(`backlog/done/20260920-aantal-gevulde-backends.md`). Wat rest is
**besluit G9**: de soort heeft in geen enkele frontend een producentroute,
en dat was opzet — de eerste twee kaarten zijn wire-first. Deze kaart geeft
haar er een.

Dit kaartje bestaat omdat de ontwerpkaart met kaart 2 naar `done/` ging en
daarmee de enige drager was van het voorstel hieronder; zonder deze kaart
zou het werk verweesd achterblijven in een afgesloten dossier.

Goal: een RegelSpraak-document kan de waardentelling SCHRIJVEN, de
frontend emitteert er de bestaande wire-vorm voor, en een corpusgolden
draagt een `bron.rgs` die byte-identiek heremitteert.

## Het voorstel, overgenomen uit de ontwerpkaart §4 (G9)

De woordgroep:

    het aantal gevulde waarden van <attribuut> van alle <rol> van de <onderwerp>

Langer dan `het aantal`, met het bestaande langste-match-precedent van
`het aantal dagen in`.

**De risico-aantekening hoort erbij en is de kern van de kaart.**
`het aantal gevulde …` alléén is NIET veilig: een officieel document mag
een attribuut of rol hebben wier naam met "gevulde" begint, en dan
verschuift maximal munch een bestaande zin. Het derde woord (`waarden`)
maakt dat onwaarschijnlijk maar niet onmogelijk. De frontend-kaart moet de
inertheid daarom METEN over het vlakke v2.3.0-corpus en het resultaat
noteren in `frontends/regelspraak/grammar/DIVERGENCES.md`, zoals de
`is een`-analyse dat voor `IS_EEN` deed — onder hetzelfde
inert-op-officiële-documenten-argument dat de `Uitzondering op`-uitbreiding
en de `is een`-uitbreiding van `spec/proposals/SUBTYPING.md` §9.2 gebruiken.
Meten, niet aannemen.

## De tweede taak, ook uit de ontwerpkaart

`VOUWSOORTEN` in `RuleChecker.java` gelijktrekken met de verifier-grens
`set_source_exempt` (`spec/verifier/lib/checks.ml`). Die twee grenzen horen
gelijk te blijven en lopen sinds kaart 1 uiteen: de verifier kent ZEVEN
waardevouwen sinds `AANTAL_GEVULDE` erbij kwam, de frontend-checker nog
niet.

## Acceptatie (bij oppakken aanscherpen)

- (a) Grammatica-uitbreiding plus de inertheidsmeting in
  `frontends/regelspraak/grammar/DIVERGENCES.md`.
- (b) Weigeringsfixtures per refusal, zoals de frontend die elders draagt.
- (c) Een `bron.rgs` op een van de goldens van kaart 2 — de voor de hand
  liggende is `corpus/golden/jutterswet-gevuldetelling` — die byte-identiek
  heremitteert. NB: die goldens zijn met de hand geschreven IR, dus de
  emissie moet naar de BESTAANDE `ir.json` toe, niet andersom.
- (d) `VOUWSOORTEN` ≡ `set_source_exempt`, met een getuige die uiteenlopen
  betrapt.
- (e) Poort 10: JUnit-vloer en rgs-goldenvloer bewegen vanzelf omhoog;
  vloeren hoeven niet opgehoogd te worden. Alle veertien poorten groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/**` (alleen een
`bron.rgs` toevoegen), deze kaart.

Out of scope: `spec/**`, `ref/**`, `backends/**` (alle drie dragen de soort
al), `spec/CONSTRUCTS.md` en `spec/PLAN.md` (rijvoorstel in het `Result:`).

## Voortgang

### Stap 1 — rood-eerst: de huidige stand, gemeten vóór enige wijziging

De zin die de golden zou moeten dragen, door de frontend van `main` gehaald:

    Het aantal gewogen vondsten van een Jutter moet berekend worden als
    het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter.

Uitkomst — en het is niet de uitkomst waar de kaart op rekende:

    WARNING (regel 22): 'gevulde waarden van het gewicht' is geen (meervoud
    van een) gedeclareerd attribuut van Vondst — de aggregatie blijft
    onopgelost (§13.4.17)
    ir: ... geschreven naar huidig.json      (exitcode 0)

De zin PARSEERT dus, en valt in een ANDERE lezing: `het aantal` bindt als de
INSTANTIEtelling en slikt "gevulde waarden van het gewicht" op als haar
elementwoord. De telling bindt geen element, de aggregatie wordt een
`Onopgelost`-marker met een waarschuwing, en het document bereikt gewoon de
schijf — met een marker waar de auteur een vouw schreef:

    "waarde": { "node": "Onopgelost",
                "type": { "node": "Onbekend", "reden": "niet opgelost" },
                "bron": "het aantal gevulde waarden van het gewicht van alle
                         vondsten van de Jutter" }

Dat is precies het gevaar dat de risico-aantekening van de kaart beschrijft,
van de andere kant gezien: niet "de zin parseert niet", maar "de zin parseert
naar iets anders". De tweede getuige is de spiegel ervan en staat onder Stap 4.

### Stap 2 — de spelling

Een WHOLE-PHRASE lexertoken `AANTAL_GEVULDE_WAARDEN_VAN`
(`'het aantal gevulde waarden van'`) en één alternatief van `functieAanroep`
met ÉÉN operand, een `primaireExpressie` — dezelfde precedentiegrond die de
§13.4.17.47-telling al geeft, zodat een staart `plus 1` BUITEN de telling
bindt. Geen facetgleuf: geen `leegwaardeAlternatief`, geen `dimensieSelectie`,
geen `conditieBijExpressie`, want alle drie zijn op deze soort ill-formed
(WF-037's "sum only" / "sum/min/max only", en besluit G7). Een facet dat niet
te schrijven is heeft geen weigering nodig.

Het token reserveert GEEN naamwoord: `gevulde` en `waarden` blijven overal
`LOWER_ID`, en alleen de exacte vijfwoordreeks verandert van betekenis. Dat is
het verschil met `Uitzonderingen`/`ongeordend` (§4 van DIVERGENCES), en het is
wat de inertheid draagt.

In de resolver rijdt de soort mee op de bestaande waardevouwmachinerie: het
elementtype komt van het objecttype van het argument (de VALUE-fold-afleiding,
niet die van de telling), het bereik gaat als `bronRol` of — bij een opgelost
meerstapsbereik — als `bronPad` de wire op, het `die/dat`-filter rijdt op het
gedeelde pad, en het resultaattype is een geheel, EENHEIDLOOS, niet-negatief
getal wat de gevouwen attribuut ook meet. `meldAggregatie` krijgt een expliciete
niets-toetsen-arm: besluit G8 maakt de toets typeloos, en een numerieke
operandtoets zou dat besluit stilzwijgend terugdraaien.

Eén nieuwe weigering: **WF-037, een bron die geen setlezing is.** ERROR, dus
het document bereikt de schijf niet. Elke andere soort laat een niet-set bron
om een eigen reden toe (de §7.1-tijdlijnvouw, de rij-1-telwrat), en daarom is
dit hek per soort getrokken en niet generiek.

### Stap 3 — VOUWSOORTEN ≡ set_source_exempt, met een lezende getuige

Gemeten: de verifier draagt sinds kaart 1 ZEVEN waardevouwen in
`value_fold_name`, de frontend droeg er ZES. Dat verschil was twee kaarten lang
onzichtbaar omdat geen frontend de soort kon schrijven — precies het spiegelbeeld
van de AANTAL-lek van 2026-09-19, die WEL zichtbaar was omdat de frontend hem
wél kon schrijven.

De grens is gelijkgetrokken, en de getuige is geen lijstpin maar een LEZER:
`VouwsoortenGrensTest` leest `spec/verifier/lib/checks.ml`, haalt de armen van
`value_fold_name` eruit en vergelijkt die met `RuleChecker.VOUWSOORTEN`. Hij pint
daarnaast de REGEL van `set_source_exempt` die op die functie keyt, zodat een
grens die stilletjes op een andere verzameling wordt getrokken niet langs de
eerste toets glipt. Een ontbrekend verifierbestand laat de test FALEN, nooit
overslaan.

Gevolg voor de melding: de WF-096-tekst somt de toelaatbare soorten op en die
opsomming groeit mee (zes → zeven namen). Twee bestaande tests pinden die tekst
woordelijk en zijn meebewogen; zie "Gewijzigde bestaande checks" onder Result.

### Stap 4 — de maximal-munch-meting

De kaart eist meten, niet aannemen. Gemeten op 2026-09-20 over **800 vlakke
RegelSpraak-documenten** in vier corpora, plus de zes markdowndocumenten van de
v2.3.0-specificatieset. De lensronde van dezelfde dag vond er drie corpora bij
en mat die na — samen 76 bestanden, alle 76 IR-identiek — zodat de meting op
**876 documenten** staat:

    64   emittergoldens van dit corpus
    47   officiële casusdocumenten van de pariteitszone
    176  documenten van het eigen corpus van de referentietoolchain
    513  documenten van een operationele ruleset-verzameling
    ---
    800  (deze sessie)
     76  nameting lensronde: regelspraak-dagloon-service,
         regelspraak-herleving-ww, regelspraak-rules (44 unieke hashes)
    ---
    876

Drie metingen, alle drie nul:

1. **de woordgroep komt nergens voor** — nul treffers op `aantal gevulde` in
   alle 876, en nul in de zes specificatiedocumenten;
2. **het woord `gevulde` komt twee keer voor in 876 documenten**, beide keren
   binnen een `//`-regelcommentaar in de operationele verzameling, waar de lexer
   niet komt. De ene treffer in de specificatie is de deelreeks binnen
   "ingevulde" in een prozabijschrift bij een figuur, geen RegelSpraak-naam;
3. **elk document emitteert byte-identiek vóór en ná.** Alle 876 zijn door de
   frontend van de commit VÓÓR de uitbreiding en die van de commit ERNA gehaald,
   en de geëmitteerde IR plus de diagnostiek is vergeleken. NUL
   herinterpretaties. Er zijn twee TEKSTverschillen en die zijn verantwoord,
   geen van beide een lezing: de WF-096-melding somt de toelaatbare soorten op
   (zes → zeven namen), en de ANTLR-"expecting {…}"-verzameling in een
   syntaxfoutmelding noemt het nieuwe token. Normaliseer precies die twee en er
   blijft in geen enkel document een verschil over.

**Wat de meting NIET beweert.** Zij kan niet uitsluiten dat een ongemeten
document een rol "gevulde waarden" heeft. Wat dat geval begrenst is de
grammatica zelf, en dat is apart gemeten met een opzettelijk botsend model: daar
verschuift de lezing WEL, en zij verschuift naar een LUIDE weigering. De rest van
de zin is dan een to-ONE-lezing, geen setlezing, en WF-037's arm weigert haar op
ERROR-niveau. Vóór de uitbreiding emitteerde dat document schoon, erna niet meer:

    OUD    ir: objecttypen=2 feittypen=1 resolvedRegels=1 geschreven naar …
    NIEUW  ERROR (regel 14): het aantal gevulde waarden van vereist een
           setlezing als bron: … 'het aantal gevulde waarden van de Jutter'
           noemt er geen … (WF-037, IR-SPEC ch. 4.3)

Gemeten afwezigheid plus een begrensde faalwijze: dat is het hele argument, en
beide helften dragen een fixture (`AantalGevuldeDialectTest`).

### Stap 5 — de golden

`corpus/golden/jutterswet-gevuldetelling/bron.rgs`, geschreven NAAR de
handgeschreven `ir.json` toe. Hij heremitteert byte-identiek, in één keer, zonder
dat er één letter aan de golden veranderde. De NOTE-slotalinea ("No `bron.rgs`:
the source language has no spelling …") was daarmee onwaar geworden en is
herschreven.

De twee regels van de golden staan één zin uit elkaar en beide zijn dragend: de
eerste schrijft de INSTANTIEtelling over een kale rolbron met de rij-1-sentinel,
de tweede de gevulde telling over een SETlezing. De heremissie pint dus dat de
uitbreiding de tweede bereikt zonder de eerste aan te raken.

Handberekening (ongewijzigd uit kaart 2, hier herhaald omdat de bron nu de
producent is; casus `case.json`, J1 meldt V1 = 2,50 kg, V2 = 1,25 kg en V3
zonder gewicht, J2 meldt niets, J3 meldt V4 zonder gewicht):

    jutter   leden   gevulde gewichten   AANTAL   AANTAL_GEVULDE
    J1       3       2                   3        2
    J2       0       0                   0        0
    J3       1       0                   1        0

en `case2-gelijke-gewichten` (V1 = 2,50, V2 = 2,50, V3 leeg): AANTAL 3,
AANTAL_GEVULDE 2 — een telling telt bijdragers, geen onderscheiden waarden.

### Stap 6 — mutatieproeven (elk gedraaid, gemeten en teruggedraaid)

    MG1  AANTAL_GEVULDE uit RuleChecker.VOUWSOORTEN
         -> de bron.rgs van de golden faalt bij naam met de WF-096-ERROR op
            regel 22 ("setlezing buiten een vouwbron"), het document bereikt de
            schijf niet meer en poort 10 wordt rood met 8 gevallen tests.
            De gelijktrekking is dus geen opruimwerk maar de voorwaarde
            waaronder de dialectzin überhaupt emitteert.
    MG2  de WF-037-weigering op een niet-set bron uitgeschakeld
         -> de zin "… van het gewicht van de Vondst" emitteert met
            `diagnostics: []`, en de verifier verwerpt datzelfde document met
            WF-037 (plus WF-083). Exact de lekvorm die AantalSetbronTest voor
            AANTAL pinde, nu voor de nieuwe soort.
    MG3  het parseralternatief weg, het lexertoken blijft staan
         -> de golden faalt met "extraneous input 'het aantal gevulde waarden
            van'": het token bestaat dan wel maar niemand consumeert het. Dat
            is de proef dat de twee helften van de uitbreiding elkaar nodig
            hebben en geen van beide dood gewicht is.

## Result (2026-09-20)

`AANTAL_GEVULDE` heeft een producentroute. Een RegelSpraak-document schrijft
`het aantal gevulde waarden van <setlezing>`, de frontend emitteert er de
bestaande wire-vorm voor, en `corpus/golden/jutterswet-gevuldetelling` draagt
een `bron.rgs` die byte-identiek heremitteert naar de handgeschreven `ir.json`.
De frontend-grens `VOUWSOORTEN` en de verifiergrens `set_source_exempt` dragen
weer dezelfde soorten, met een getuige die ze LEEST in plaats van erover te
beweren. Alle veertien poorten groen, elk los gedraaid.

### 1. Wat er landde

**De spelling.** Eén whole-phrase lexertoken en één alternatief van
`functieAanroep` met één `primaireExpressie`-operand en geen facetgleuf. In de
resolver: de VALUE-fold-elementafleiding, het gedeelde bereik (`bronRol`, of
`bronPad` bij een opgelost meerstapsbereik), het gedeelde `die/dat`-filter, een
geheel eenheidloos resultaattype, en een expliciete niets-toetsen-arm in
`meldAggregatie` zodat besluit G8 niet stilzwijgend sneuvelt. Eén nieuwe
weigering: WF-037, een bron die geen setlezing is, op ERROR-niveau.

**De gelijktrekking.** `VOUWSOORTEN` zes → ZEVEN, en de WF-096-melding somt de
zeven op. `VouwsoortenGrensTest` leest `value_fold_name` uit
`spec/verifier/lib/checks.ml`, vergelijkt de armen met `VOUWSOORTEN`, en pint
daarnaast de regel van `set_source_exempt` die op die functie keyt — een grens
die elders wordt getrokken glipt dus niet langs de eerste toets. Een ontbrekend
verifierbestand laat de test falen, niet overslaan.

**De golden.** Eén `bron.rgs`, geschreven naar de bestaande hand-IR toe.
De NOTE-slotalinea is herschreven: zij beweerde dat de soort in geen enkele
frontend een producentroute had.

**De inertheidsmeting** staat in `frontends/regelspraak/grammar/DIVERGENCES.md`
§6, met de aantallen, de twee verantwoorde tekstverschillen en de expliciete
grens van wat de meting NIET beweert.

### 2. De inertheidsmeting — wat gemeten is en wat niet kon

Gemeten over 876 vlakke RegelSpraak-documenten plus zes
specificatiedocumenten. Byte-identieke emissie vóór en ná, per corpus:

    64 / 64    emittergoldens van dit corpus
    47 / 47    officiële casusdocumenten van de pariteitszone
    176 / 176  het eigen corpus van de referentietoolchain
    513 / 513  een operationele ruleset-verzameling
     76 /  76  drie collecties uit de nameting van de lensronde
    ---------
    876 / 876  NUL herinterpretaties

Twee tekstverschillen, beide verantwoord en geen van beide een lezing: de
WF-096-melding somt de soorten op (zes → zeven namen), en de
ANTLR-"expecting {…}"-verzameling in een syntaxfoutmelding noemt het nieuwe
token. Normaliseer precies die twee en er blijft nergens een verschil over.

**Wat NIET gemeten kon worden, eerlijk.** De kaart noemt "het vlakke
v2.3.0-corpus". Dat corpus bestaat niet als één uitgepakte verzameling; wat
bestaat zijn de vier normatieve markdowndocumenten plus het casusdocument en de
conversienotities (zes stuks, alle zes doorzocht: nul treffers, de ene grep-hit
is de deelreeks binnen "ingevulde" in een prozabijschrift) en de
voorbeeldverzamelingen die de toolchain en de werkbank zelf dragen — die zijn
alle vier meegemeten en staan hierboven. Wat ontbreekt is dus een canonieke
uitgave die er niet is. Een document buiten de gemeten verzameling kan de
woordgroep dragen, en de begrenzing daarvan staat onder Stap 4: de verschuiving
is dan luid, niet stil.

**De vier corpora hierboven waren NIET alles wat op deze machine stond**, en dat
is gemeten in plaats van aangenomen — de lensronde vond er drie die deze sessie
had gemist: `regelspraak-dagloon-service`, `regelspraak-herleving-ww` en
`regelspraak-rules`, samen 76 bestanden met 44 unieke inhouds-hashes. De
oorspronkelijke zin hier ("geen corpus overgeslagen dat op deze machine
bereikbaar was") was daarmee te sterk en is hierboven ingetrokken. De nameting
van die 76 bestanden is alle 76 IR-identiek met nul diagnostiekverschillen, dus
de verbrede meting staat op **876 documenten met nul herinterpretaties** en de
conclusie wordt er sterker van in plaats van zwakker. De les is de zin, niet het
getal: "alles wat bereikbaar was" is een bewering die een tweede lezer moet
natellen, en die telling heeft er drie gevonden.

### 3. De melding voor de herpin

**Dit is een emissieWIJZIGING van de frontend**, anders dan kaart 2's
leesverbreding. Wat de toolchain-herpin moet overnemen:

- het lexertoken `AANTAL_GEVULDE_WAARDEN_VAN` en het parseralternatief;
- de resolverarmen, en het zijn er ZES, niet vier — de twee onderaan zijn in de
  eerste opsomming van deze kaart vergeten en staan er hier bij, omdat een
  herpin die alleen de opsomming volgt ze allebei mist:
  - de elementafleiding (de VALUE-fold-tak van `aggregatieElement`);
  - het resultaattype (`functieType`: geheel, eenheidloos, niet-negatief);
  - de WF-037-weigering op een niet-set bron;
  - de niets-toetsen-arm van `meldAggregatie` (besluit G8);
  - **`aggregeert()`** — zonder die arm loopt de Let-inlijning mis: een
    aggregatie is een structuurgevoelige positie en moet met
    GESUBSTITUEERDE variabelelezingen worden opgebouwd, en een soort die
    daar ontbreekt leest de vorm van een `BinderRef` in plaats van van de
    opgeloste operand;
  - **`aggregatieKind`** in `bouwFunctie` — zonder die arm slaat de
    gedeclareerde-lid-WARNING de soort over, en een bron die geen
    gedeclareerd lid noemt vouwt dan over een getter die niet bestaat in
    plaats van eerlijk te markeren;
- `VOUWSOORTEN` zes → zeven, en de WF-096-meldingstekst die de zeven opsomt.
  Let op de ZICHTBAARHEID: het veld ging van `private static final` naar
  package-private `static final`, zodat `VouwsoortenGrensTest` het kan lezen.
  Dat is geen gedragswijziging maar wel een handtekeningwijziging, en een
  herpin die hem overslaat krijgt een getuige die niet compileert.

Tellingen ná deze kaart: **65 rgs-goldens** (was 64), JUnit-suite **351** (was
338, +13: TIEN nieuwe dialectfixtures en DRIE grensgetuigen, minus niets). Geen
vloer opgehoogd — poort 10's vloeren staan op 173 en 38 en groeiden vanzelf
voorbij.

**De pariteitszone beweegt NIET** (poort 11: 29 casussen, nul divergenties),
zoals voorspeld: geen enkel casusdocument daar draagt de woordgroep. Twee
documenten uit die zone tonen los geëmitteerd wél het WF-096-tekstverschil, maar
dat is de meldingsopsomming en geen lezing, en de poort vergelijkt IR.

### 4. De pinstand — expliciet, want er loopt een parallelle kaart

**Poort 12's twee gelijkheidspinnen zijn NIET bewogen en hoefden dat niet.**
`EXPECTED_PLAYED` blijft 172 en `EXPECTED_DRL_PLAYED` blijft 149, beide gemeten
groen. De reden is de vorm die de kaart koos: er komt geen NIEUWE golden bij —
`jutterswet-gevuldetelling` speelde sinds kaart 2 al in alle vier de uitvoerders
— er komt een `bron.rgs` bij op een golden die er al stond. De opdracht rekende
op +1/+1; gemeten is +0/+0.

**Deze kaart raakt `scripts/` dus in het geheel niet.** Er is geen
beschermd-padvoorstel en geen botsing met de parallelle kaart
(`20260919-datum-setbron-breuk`), die `scripts/check-drools.sh` wél aanraakt.
De tweede lander hoeft hier niets voor te rebasen.

### 5. Poortuitkomsten (elk los gedraaid, na `eval $(opam env)`)

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  262 documenten, vloer 192
     3 check-verifier           PASS  262 golden schoon; 420 negatief, vloer 350
     4 check-case-schema        PASS  609 casusbestanden
     5 check-oracle             PASS  ref-suite 755, vloer 489; 570 cases, vloer 427
     6 check-clingo             PASS  backendsuite 461, vloer 83; 570 cases
     7 check-noop               PASS  534 cases, 36 overlevers
     8 check-rood               PASS  39 rode cases, vloer 26; 78 fragmenten citeren
                                      geen van de 36 bodemzinnen
     9 check-catala             PASS  201 checks; 12 goldens
    10 check-regelspraak        PASS  351 tests (was 338), vloer 173;
                                      65 rgs-goldens (was 64), vloer 38
    11 check-toka-parity        PASS  29 casussen, nul divergenties
    12 check-drools             PASS  gespeeld 172 (pin ONGEWIJZIGD),
                                      DRL-tekst 149 (pin ONGEWIJZIGD), JUnit 514
    13 check-reftype-derivation PASS  131 twists over 27 goldens, vloer 74;
                                      128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks; 2 goldens

Geen enkele vloer en geen enkele pin bewogen.

**Poort 12's JUnit-getal is 514 en blijft dat na de rebase — nagemeten.** De
lensronde meldde 518 en schreef dat verschil toe aan de enumlezer-kaart. Dat
klopt niet voor deze tak, en het is nagemeten in plaats van beredeneerd: de
backendsuite op de taktop telt 514 (`mvn -o -f backends/drools/pom.xml
package`, 0 overgeslagen), het registerwerk van de enumlezer zit AL in de
merge-base, en het enige wat `main` sindsdien toevoegde is één kaartbestand
onder `backlog/new/`. Een rebase kan dus geen test toevoegen. 518 hoort bij een
werkboom die ook ongemergde kaarten draagt; 514 is wat deze tak, voor en na de
rebase, oplevert.

### 6. Gewijzigde bestaande checks (testmandaat)

Additief was vrij; deze vier zijn WIJZIGINGEN en worden daarom gemeld:

- `SetlezingZonderVouwTest.MELDING` en `AantalSetbronTest.MELDING` pinden de
  WF-096-tekst woordelijk, inclusief de opsomming van toelaatbare soorten. Die
  opsomming groeit met de gelijktrekking mee (zes → zeven namen, en "een
  telling" werd "een instantietelling", want met twee tellingen in huis was het
  woord dubbelzinnig geworden). De pinnen zijn meebewogen; wat zij pinnen is
  niet verzwakt.
- `MarkerRedenPinTest.GESANCTIONEERD`: `bouwFunctie` 2 → 3 null-markers, met de
  reden erbij (de WF-037-ERROR staat vlak boven de derde).
- `AantalSetbronTest`' klassencommentaar sprak van "de zes waardevouwen"; dat
  getal is zeven geworden.

### 7. Wat bewust NIET gebeurde

- **Geen nieuwe golden.** De kaart vroeg een `bron.rgs` OP een golden van
  kaart 2, met de emissie naar de bestaande hand-IR toe. Dat is de strengere
  toets (de emitter moet de handgeschreven bytes raken, niet andersom) en het
  is meteen de reden dat poort 12 niet beweegt.
- **`spec/**`, `ref/**`, `backends/**` onaangeraakt.** Alle drie dragen de
  soort al.
- **Geen catala- en geen flogic-spelling.** Elk van die frontends draagt een
  eigen weigeringslijst en een wire-soort die zij niet schrijven kost hun niets.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`** — voorstel in §8.

### 8. Registervoorstel (hoofdsessie, serieel)

Voor `spec/CONSTRUCTS.md` rij 1 (de aggregatierij), aan te hangen aan het blok
dat kaart 2 voorstelde:

> **De producentroute geland (2026-09-20, kaart 3; besluit G9 uitgevoerd):** de
> soort is niet langer alleen op de wire schrijfbaar. De RegelSpraak-frontend
> kent haar een DIALECTspelling toe — `het aantal gevulde waarden van`, een
> whole-phrase token met één setlezing als operand en geen facetgleuf — die
> geen naamwoord reserveert: alleen de exacte vijfwoordreeks verandert van
> betekenis. Dat de uitbreiding op bestaande documenten inert is, is GEMETEN en
> niet aangenomen: 876 vlakke RegelSpraak-documenten (het eigen corpus, de
> pariteitszone, het corpus van de referentietoolchain en vier operationele
> ruleset-verzamelingen) emitteren byte-identiek vóór en ná, met nul
> herinterpretaties en twee verantwoorde tekstverschillen in meldingen; de
> woordgroep komt in geen van die 876 en in geen van de zes
> specificatiedocumenten voor. Wat de meting niet kan uitsluiten begrenst de
> grammatica: op een opzettelijk botsend model verschuift de lezing wél, en zij
> verschuift naar een LUIDE WF-037-weigering in plaats van naar een ander
> antwoord — gemeten met beide frontends naast elkaar. Bij de spelling hoort de
> gelijktrekking van twee grenzen die één regel zijn: `VOUWSOORTEN` in de
> frontend en `set_source_exempt` in de verifier dragen weer dezelfde ZEVEN
> waardevouwen, en de getuige LEEST voortaan de verifierlijst in plaats van
> haar na te vertellen — de frontend liep twee kaarten lang zes tegen zeven
> zonder dat iets het mat, omdat geen frontend de soort kon schrijven.
> `corpus/golden/jutterswet-gevuldetelling` draagt de `bron.rgs`; de emissie is
> naar de handgeschreven IR toe geschreven en raakt haar byte-identiek.

Voor `spec/PLAN.md` is er geen scopewijziging.
