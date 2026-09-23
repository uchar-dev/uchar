# frontend-aantal-attribuutnaam: de foutzin mist het "aantal …"-attribuut

Gemeten door de aantal-zonder-bron-lens (2026-09-23, advies 5): "het
aantal gewogen posten van de Jutter" met een gedeclareerd ATTRIBUUT
"aantal gewogen posten" krijgt sinds de `tellingBronFout`-ERROR de
zin "telling zonder bron" — de terugval op een gedeclareerde naam in
`ExpressieBouwer.bouwFunctie` vangt alleen parameters, geen attribuut
dat "aantal …" heet en met lidwoord wordt teruggelezen. Vóór de kaart
emitteerde de frontend hier stil een wrattelling die WF-034 al
weigerde, dus de fout is nu luider en geen regressie — maar de zin
noemt de echte oorzaak niet, en zulke attribuutnamen zijn in
RegelSpraak-modellen gangbaar.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer het geval; meet of de attribuutterugval veilig kan
  (wanneer is "het aantal X van Y" een attribuutlezing en wanneer een
  telling? — de brontaal beslist; sectieverwijzingen, niet citeren).
- (b) Óf de terugval leert het attribuut lezen (rood-eerst getuige,
  emissie als AttribuutRef), óf de foutzin benoemt de botsing ("de
  naam is een gedeclareerd attribuut; een telling vergt een
  instantiebron") — geen stille of misleidende fout.
- (c) Alle veertien gates groen; herpin bij frontend-beweging.

May touch: `frontends/regelspraak/**`, kaartverplaatsing.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/frontend-aantal-attribuutnaam`, basis `main` `582e8f7e`.

1. Reproductie met `make ir` op "het aantal gewogen posten van de
   Jutter" (attribuut `aantal gewogen posten` gedeclareerd).
2. Normmeting: de brontaal (spec v2.3.0, sectieverwijzingen) én de
   toolchain (dezelfde zinnen door zijn resolver; het vendored model in
   `toka/` op attribuutlezingen van een "aantal …"-naam). Het
   gedeclareerde attribuut mag onder "het aantal" alleen winnen waar de
   toolchain dat ook doet, niets breder; anders benoemt de foutzin de
   botsing en de spelling die het attribuut wél leest.
3. Rood-eerst getuige per spelling, een tegenproef dat de aangeraden
   spelling het attribuut schoon leest, een tegenproef zonder
   "aantal …"-attribuut; mutatieproef per voorwaarde.
4. Gates 10 en 11 byte-identiek (verwacht: geen corpusdrager); alle
   veertien gates groen, elk als losse aanroep; pins 181/160 bewegen niet.

## Result (2026-09-23)

Route: de foutzin benoemt de botsing. Geen attribuutterugval. Er verandert
geen emissie: de zin was al een ERROR en blijft dat, alleen met de
werkelijke oorzaak erin. Gates 10 en 11 blijven byte-identiek, alle
veertien gates groen, en dit is GEEN herpin-kaart.

### 1. Reproductie (basis `582e8f7e`)

Model: Jutter (bezield) met `het aantal gewogen posten`,
`de gewogen vangst` en `de score`. Regel: "De score van een Jutter moet
berekend worden als <E>." Gemeten met `make ir`:

    E                                            uchar-frontend
    het aantal gewogen posten van de Jutter      ERROR "telling zonder bron: …"
    (het aantal gewogen posten van de Jutter)    ERROR "telling zonder bron: …"
    het aantal gewogen posten van een Jutter     ERROR "telling zonder bron: …"
    aantal gewogen posten van de Jutter          AttribuutRef "aantal gewogen posten", diagnostics []
    de aantal gewogen posten van de Jutter       AttribuutRef, []
    zijn aantal gewogen posten                   AttribuutRef, []

### 2. Normmeting

**Brontaal (spec v2.3.0).** §13.4.17.3 laat een getalexpressie zowel een
`<getalaggregatie>` als een `<attribuutvanonderwerp>` zijn;
§13.4.17.46/.47 maakt van "het aantal" plus een expressie een aggregatie;
§13.3.2.5/.6 geeft een attribuut een naamwoord met optioneel bepaald
lidwoord, en §13.4.2.9 leest het als `<attribuutmetlidwoord> van
<onderwerp>`. De tekenreeks "het aantal gewogen posten van de Jutter"
is dus op beide manieren afleidbaar, en de EBNF kent geen voorrang.
Typeringen §4.11 staat onder `aantal` alleen onderwerpexpressies toe,
dus als telling is de zin ongetypeerd. De spec declareert zelf
"aantal …"-namen (§9, het ObjectCreatie-voorbeeld: twee attributen en een
parameter). Alleen de PARAMETER wordt met "het aantal …" in een
expressie teruggelezen, en dat vangt de bestaande parameterterugval al.
Nergens in de spec wordt een ATTRIBUUT met "aantal …" onder "het aantal"
gelezen.

**Toolchain.** Zijn grammatica heeft dezelfde voorrang (`aantalFunc` is
een `functieAanroep`, en die staat vóór `attribuutVanOnderwerp` in
`primaireExpressie`) en dezelfde terugval, die alleen parameters vangt.
Zijn resolver op dezelfde probes:

    het aantal gewogen posten van de Jutter     WARNING dekking: MARKER — TODO aggregatie AANTAL
    (het aantal …) / … van een Jutter           idem, MARKER
    aantal … / de aantal … / zijn aantal …      attribuutlezing, schoon

Het vendored model in `toka/` declareert twee "aantal …"-attributen
(toka-03). Het leest ze in een expressie NERGENS terug: alleen als
ObjectCreatie-doel (toka-28) of als verdeelbron onder "Het totaal
aantal …". De toolchain emitteert dus nergens een attribuutlezing onder
"het aantal". Precies daar waar hij het attribuut wél leest, leest de
uchar-frontend het ook.

**Corpus.** Acht goldens declareren "aantal …"-attributen. Ze lezen ze
terug als "zijn aantal …" (bergingsdrempel, penningdrempel, penningrecht,
penningjaar) of als regeldoel. Geen corpusdocument draagt de botsing.

### 3. De keuze (verbatim)

GEEN attribuutterugval onder "het aantal". Een telling zonder
instantiebron waarvan de operand een `AttribuutRef` `X` op objecttype `T`
is, terwijl `T` een attribuut "aantal X" declareert, krijgt een eigen
foutzin. Die zin noemt de botsing en de lidwoordloze spelling die het
attribuut wél leest. Motivering: de brontaal draagt beide lezingen zonder
voorrang. De toolchain leest zo'n attribuut nergens onder "het aantal".
Een terugval zou dus breder zijn dan de toolchain, en daarmee een
taalverbreding in plaats van een opgeloste dubbelzinnigheid. De
lidwoordloze spelling (§13.3.2.6) bestaat al en leest het attribuut in
beide implementaties schoon.

Nieuwe melding (ERROR, zelfde plek, zelfde regel):

    telling zonder bron, of een attribuut: 'het aantal gewogen posten van de
    Jutter' leest als telling van 'gewogen posten van de Jutter', en 'gewogen
    posten' noemt geen rol om te tellen — maar 'aantal gewogen posten' is een
    gedeclareerd attribuut van Jutter. De brontaal leidt de zin op beide
    manieren af (§13.4.17.3: getalaggregatie of attribuutvanonderwerp) en de
    grammatica kiest de telling, die zonder instantiebron niet welgevormd is
    (WF-037, IR-SPEC ch. 4.3, typeringen §4.11). Bedoel je het attribuut,
    schrijf dan 'aantal gewogen posten van de Jutter' — zonder lidwoord, dat
    bij een attribuutverwijzing optioneel is (§13.3.2.6)

Code: `ExpressieBouwer.aantalAttribuutBotsing`, aangeroepen vooraan in
`tellingBronFout`. De grammatica is niet aangeraakt.

### 4. Rood-eerst en mutatie

Getuige `AantalAttribuutnaamTest` (nieuw bestand, bestaande tests niet
aangeraakt): 3 geparametriseerde botsingen ("… van de Jutter", "… van een
Jutter", tussen haakjes) en 2 tegenproeven. De ene toetst dat de
aangeraden spelling schoon een `AttribuutRef "aantal gewogen posten"`
wordt; de andere dat "het aantal gewogen vangst …", zonder
"aantal …"-attribuut, de algemene zin houdt.

    vóór de fix:   5 tests, 2 successful, 3 failed
                   (alle drie: de algemene "telling zonder bron: …"-zin, de botsing niet genoemd)
    na de fix:     5 successful, 0 failed
    mutant 1 (botsing = null):                 2 successful, 3 failed — de drie botsingen
    mutant 2 (declaratietoets "aantal X" weg): 15 successful, 1 failed (samen met
                   AantalZonderBronTest) — zonder_aantal_attribuut_blijft_de_algemene_zin
                   krijgt de botsingszin voor 'aantal gewogen vangst'

### 5. Gates, elk als losse aanroep na `eval $(opam env)`

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  278 documenten, vloer 192
     3 check-verifier           PASS  278 golden schoon; 440 negatief, vloer 350
     4 check-case-schema        PASS  638 casusbestanden
     5 check-oracle             PASS  ref-suite 788, vloer 489; 595 cases
     6 check-clingo             PASS  suite 518, vloer 83; 595 cases ref = clingo
     7 check-noop               PASS  559 weerleggen; 36 overlevers
     8 check-rood               PASS  43 rood, vloer 26; 86 fragmenten; Drools 9 gepind, 34 niet-gedragen
     9 check-catala             PASS  201 checks, 12 goldens
    10 check-regelspraak        PASS  409 JUnit (404 + 5), vloer 173; 70 rgs-goldens byte-identiek
    11 check-toka-parity        PASS  45 .rgs byte-identiek, 27 tests, 29 cases nul divergenties
    12 check-drools             PASS  gespeeld 181 (pin), DRL-tekst 160 (pin), JUnit 605
    13 check-reftype-derivation PASS  131 twists over 27 goldens, 128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks, 2 goldens

Geen vloer of pin aangeraakt. Geen emissie veranderd, dus geen herpin.

### 6. Open punten

1. **De echte dubbele lezing.** Declareert een model zowel een rol
   "X" (mv) als een attribuut "aantal X" op het onderwerp, dan bindt
   "het aantal X van …" een elementtype en wint de telling stil. Dat is
   hetzelfde gedrag als de toolchain en hoort bij de voorrang van de
   grammatica. De frontend waarschuwt daar niet. Geen corpusdrager
   (gemeten: keurunie, vondstunie en unievoorwaarde tellen de rol
   "aangemelde vondsten" en declareren geen attribuut
   "aantal aangemelde vondsten"). Een WARNING bij die botsing is een
   eventuele eigen kaart.
2. Wil de maintainer het attribuut onder "het aantal" wél laten winnen
   (een taalkeuze boven de toolchain uit), dan is dat een
   CONSTRUCTS-delta en een normbesluit, geen frontendfix.

### 7. Registervoorstel — `spec/CONSTRUCTS.md` rij 1

Aansluitend op het blok "**Landed (2026-09-23, card `aantal-zonder-bron`)
…**", als laatste alinea ervan:

> **Follow-up (2026-09-23, card `frontend-aantal-attribuutnaam`): the
> refusal names an "aantal …" attribute it collides with.** "het aantal
> gewogen posten van de Jutter", with an attribute `aantal gewogen posten`
> declared on Jutter, derives in the source language both as a count and as
> an attribute read (§13.4.17.3), with no tie-break. The grammar takes the
> count, as the source toolchain's does, and the toolchain reads such an
> attribute under "het aantal" nowhere: it markers the count, and neither
> the specification nor the TOKA model writes the read. The frontend
> therefore does not fall back to the attribute, which would widen the
> language. Instead the source-less-count ERROR names the declared
> attribute and the article-less spelling that reads it ("aantal gewogen
> posten van de Jutter", §13.3.2.6), which both implementations already
> resolve clean. No emission changes and no re-pin.
