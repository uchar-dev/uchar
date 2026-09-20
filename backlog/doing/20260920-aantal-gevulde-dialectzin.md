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
RegelSpraak-documenten** — elk document dat op dat moment bereikbaar was — plus
de zes markdowndocumenten van de v2.3.0-specificatieset:

    64   emittergoldens van dit corpus
    47   officiële casusdocumenten van de pariteitszone
    176  documenten van het eigen corpus van de referentietoolchain
    513  documenten van een operationele ruleset-verzameling
    ---
    800

Drie metingen, alle drie nul:

1. **de woordgroep komt nergens voor** — nul treffers op `aantal gevulde` in
   alle 800, en nul in de zes specificatiedocumenten;
2. **het woord `gevulde` komt twee keer voor in 800 documenten**, beide keren
   binnen een `//`-regelcommentaar in de operationele verzameling, waar de lexer
   niet komt. De ene treffer in de specificatie is de deelreeks binnen
   "ingevulde" in een prozabijschrift bij een figuur, geen RegelSpraak-naam;
3. **elk document emitteert byte-identiek vóór en ná.** Alle 800 zijn door de
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
