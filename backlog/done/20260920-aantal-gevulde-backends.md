# AANTAL_GEVULDE, kaart 2: clingo, beide Drools-takken en de goldens

Uitvoeringskaart 2 van 2 onder het ontwerp
`backlog/doing/20260919-aantal-gevulde-gemiddelde.md`. Het besluitenblok
daar (G1–G9, maintainer 2026-09-19) is bindend en wordt hier niet
heropend; §5 "Uitvoeringsschets" verdeelt het werk over twee kaarten en
dit is de tweede. Kaart 1 (`backlog/done/20260920-aantal-gevulde-wire.md`)
landde de wire, de verifierhekken en `ref`; haar §9 is de
OVERDRACHTSNOTITIE die deze kaart uitvoert. De ontwerpkaart gaat met deze
kaart mee naar `done/`.

Goal: de waardentelling `AANTAL_GEVULDE` wordt door ALLE VIER de
uitvoerders beantwoord — `ref` (kaart 1), clingo, de Drools-exec-tak en de
Drools-DRL-tekst-tak — en het corpus draagt de eerste goldens waarop dat
vierkant sluit, plus één rode casus voor de temporele setbron die beide
OCaml-uitvoerders luid weigeren.

## Acceptatie

- (a) **clingo.** De benoemde weigerarm in
  `backends/clingo/lib/translate.ml` wordt een vertaling: een `#count`
  over dezelfde aanwezigheidsliteral waarmee de som/min/max hun
  presence-guard bouwen, met dezelfde `bronRol`/`bronPad`/filter-vorm als
  de SOM/TOTAAL-setarm. De temporele setbron houdt een EIGEN benoemde
  weigerzin (geen bodemzin), want `ref` weigert de vorm ook.
- (b) **Drools, in deze volgorde.** (1) `AANTAL_GEVULDE` in
  `Expr.FunctieKind` — een frontend-BESTAND, geen emissiewijziging, maar
  het record compileert in de backend mee; (2) `ExecModelTranspiler` (de
  accumulate-CE naast de count-tak); (3) `DrlTranspiler` (de
  `case AANTAL -> "count"`-tak). De klasse-brede `IrLoader`-Refusal laat
  de soort daarna door; de getuige uit kaart 1
  (`OnbekendeFunctieKindWitnessTest`) moet op een ECHT onbekende soort
  blijven slaan.
- (c) **Goldens, met handberekende verwachtingen VOORAF in het dossier.**
  Minstens (i) een basisgolden die de vouw dragend maakt: gemengd
  gevuld/leeg, met de instantietelling ernaast zodat
  `AANTAL_GEVULDE ≠ AANTAL` zichtbaar is; (ii) de `bronPad`-golden met
  een element dat langs twee tussenknopen bereikbaar is (de unie telt hem
  één keer); (iii) een niet-numerieke getuige (besluit G8), met de
  booleaanse `onwaar` als scherpste rij.
- (d) **Rood.** Minstens één rode casus voor de temporele setbron, met
  gepinde fragmenten in `ref` én clingo die GEEN bodemzin zijn.
- (e) **Poortpinnen.** `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` in
  `scripts/check-drools.sh` zijn GELIJKHEDEN en bewegen per spelende
  golden, met de reden per golden bij de pin. Zuiver additief — een
  parallelle kaart raakt hetzelfde script.
- (f) Alle veertien poorten groen, elk los gedraaid.

May touch: `backends/clingo/lib/translate.ml`,
`backends/clingo/test/test_clingo.ml`,
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/ir/Expr.java`
(alleen de `FunctieKind`-enum),
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
een JUnit-getuige onder `backends/drools/src/test/`,
`corpus/golden/**` (nieuwe zones), `corpus/rood/**` (nieuwe zone),
`scripts/check-drools.sh` (alleen de twee gelijkheidspinnen en hun
redenblokken — beschermd pad, gemandateerd voorstel), deze kaart en de
ontwerpkaart.

**Uitbreiding tijdens de uitvoering, hier genoteerd in plaats van
stilzwijgend gedaan:** `scripts/check-rood.sh` is óók aangeraakt, met ÉÉN
regel — de nieuwe rode casus op de Drools-uitzonderingslijst van dat
been. Dat been eist per rode casus een gepind fragment of een reden, en
een rode casus toevoegen zonder de regel maakt de poort rood. De
uitbreiding volgt dus rechtstreeks uit acceptatie (d) en valt onder
hetzelfde beschermde-padmandaat als de pinnen van poort 12. De tweede
rode casus kreeg géén lijstregel maar een echt Drools-fragment, want die
faalt werkelijk bij vuren.

Out of scope: `spec/**` (kaart 1 normeerde ch. 4.3 en ch. 3.4c; deze
kaart voegt er geen letter aan toe), `ref/**` (draagt de vorm al),
`spec/verifier/**`, `frontends/**` behalve de ene enumconstante (de
dialectzin is besluit G9 en kaart 3), `spec/CONSTRUCTS.md` en
`spec/PLAN.md` (rijvoorstel gaat als tekst in het `Result:`).

## Handberekeningen (VOORAF, ch. 3.4c)

De Jutterswet-populatie van de basisgolden, per jutter: J1 meldt V1
(gewicht 2,50), V2 (1,25) en V3 (geen gewicht); J2 meldt niets; J3 meldt
V4 (geen gewicht).

    jutter   AANTAL   AANTAL_GEVULDE   SOM (of 0)
    J1       3        2                3,75
    J2       0        0                0
    J3       1        0                0

De `bronPad`-golden: de vlakke unie van de losplaatsen van alle gevoerde
boten. In `case1` bereiken drie boten drie bergplaatsen (P1 3,00; P2 leeg;
P3 5,00), in `case2` bereiken vier boten dezelfde drie met P1 langs twee
routes. Beide keren is de unie {P1, P2, P3} en dus `AANTAL_GEVULDE` = 2;
wie de joinkoppels vouwt antwoordt in `case2` 3.

De niet-numerieke getuige: drie vondsten, per waardesoort twee gevuld en
één leeg, telkens 2 naast een instantietelling van 3. De booleaanse rij
draagt `onwaar` als gevulde waarde — leeg is de afwezigheid van een
waarde, `onwaar` is er een.

Tijdens de uitvoering kwamen er twee getuigen bij, beide handberekend vóór
zij gemeten werden. `case2-gelijke-gewichten` bij de basisgolden: twee
leden met HETZELFDE getal (2,50 en 2,50) naast één leeg lid, antwoord 2 —
een telling telt bijdragers, geen onderscheiden waarden. En de vierde
golden met haar rode tweelingbroer, hieronder onder Stap 4:

    jutter   waarderingen   gevuld   som   gemiddelde (bewaakt)
    J1       4, 6, leeg     2        10    5
    J2       (geen)         0        0     leeg
    J3       leeg           0        0     leeg

Zonder de bewaking is J2 en J3 `0 ÷ 0` en dat is de rode casus.

## Voortgang

### Stap 1 — rood-eerst: het corpus vóór de uitvoerders

Vier zones toegevoegd, elk vóór enige uitvoerderswijziging gemeten:

    corpus/golden/jutterswet-gevuldetelling    verifier PASS, ref PASS (7)
                                               clingo: WEIGERT bij naam
    corpus/golden/jutterswet-gevuldepad        verifier PASS, ref PASS (2+2)
    corpus/golden/jutterswet-gevuldesoorten    verifier PASS, ref PASS (6)
    corpus/rood/aantalgevulde-temporele-setbron
                                               verifier PASS (de vorm is
                                               WELGEVORMD — de weigering hoort
                                               bij de uitvoerders, niet bij de
                                               poort)
                                               ref: verwachte fout gezien
                                               clingo: MISMATCH — hij weigert
                                               nog met kaart 1's naamzin in
                                               plaats van met de temporele

`ref` rekent alle drie de goldens meteen goed: hij draagt de vouw sinds
kaart 1, en de handberekende getallen hierboven zijn precies wat hij
antwoordt. Dat is de gewenste asymmetrie van deze kaart — het corpus pint
wat `ref` al kan en dwingt de andere drie uitvoerders daarheen.

De `bronPad`-golden is zo gebouwd dat de vlakke unie een ANDER getal geeft
dan de joinkoppels: in `case2-gedeelde-bergplaats` bereiken vier boten drie
bergplaatsen, waarvan P1 langs twee. Wie de koppels vouwt telt P1 twee keer
en antwoordt 3; de gepinde 2 is de unie. Zonder dat verschil zou de casus
niets onderscheiden.

De gedeelde bergplaats vroeg om een ANDERE relatievorm dan de
anker-golden `jutterswet-bootberging` gebruikt. Daar is de middenrelatie
veel-op-veel, waardoor het beanmodel aan geen van beide kanten een veld
afleidt en beide Drools-takken de golden bij naam weigeren. Hier lost een
boot in ÉÉN bergplaats en delen meerdere boten er één — een
veel-op-één-middenrelatie, waarvan het veld op de boot staat. Dat is
dezelfde deelbaarheid met een beanveld eronder, dus de vraag "telt de unie
of tellen de koppels" wordt in alle vier de uitvoerders gesteld in plaats
van in twee.

### Stap 2 — clingo

De weigerarm is weg en op de plaats van de setvouwen staat een `#count`.
De vorm is die van de SOM/TOTAAL-arm: één `source_join`, één `val/3` per
lid, hetzelfde die/dat-filter — alleen het aggregaat verschilt. Twee
dingen zijn eigen en geen kopie:

- **de teltsleutel is de ELEMENTvariabele**, niet de waarde. `#count` telt
  onderscheiden tupels, dus dat is precies wat een element dat langs twee
  tussenknopen bereikbaar is één keer laat meetellen;
- **geen presence-guard.** De som heeft er een omdat zijn lege antwoord
  leeg is; het lege antwoord van een telling is 0, en dat is wat clingo's
  `#count` over nul tupels vanzelf geeft.

De temporele setbron krijgt een EIGEN weigerzin met eigen hoofdstuknummer
(ch. 3.4c), zodat de rode casus kan pinnen wélke arm sprak — de
rangschikkende vouwen weigeren op dezelfde grond met hun eigen zin
(ch. 3.6).

De twee checks in `test_clingo.ml` zijn gekanteld en er zijn er twee
bijgekomen (452 → 454 in kaart 1, nu 456). Wat ze pinnen is verschoven, niet
verdwenen: de welgevormde setvorm VERTAALT nu, en wat nog geweigerd wordt
staat met eigen zin buiten de bodemzin.

**Mutatieproeven (elk gedraaid, gesweept en teruggedraaid).**

    MC1  de val/3-aanwezigheidsliteral uit het contribuantenlichaam
         -> alle vijf de casussen MISMATCH: de telling wordt de
            instantietelling (3 waar 2 hoort)
    MC2  de teller op de WAARDE gesleuteld in plaats van op het element
         -> precies ÉÉN corpuscasus valt om, case2-gelijke-gewichten,
            plus de vormcheck in de eigen suite. De andere vier blijven
            groen — hun gevulde waarden zijn toevallig onderling
            verschillend. Dat is de reden dat die casus bestaat, en de
            meting is het bewijs dat hij niet spare is.
    MC3  AANTAL_GEVULDE toegevoegd aan eff_unit (bij de zes)
         -> het corpus beweegt NIET; de proefsteen wél. Een handgespeld
            gemiddelde `puntensom ÷ AANTAL_GEVULDE(kg-bron)` gaat van
            `clingo: PASS` naar `TRANSLATION FAULT: a unit-carrying
            multiplicative operand is not translated`, terwijl `ref` hem
            gewoon uitrekent — één-antwoordt-één-weigert onder een
            welgevormd document
    MC4  beide tellingen toegevoegd aan provably_unitless
         -> poort 6 blijft groen, 456 checks en 563 casussen ongewijzigd:
            de verbreding heeft in het hele gepoorte corpus geen getuige

### Het besluit over de drie eenheidsafleiders

`AANTAL_GEVULDE` gaat in GEEN van de drie, en blijft daarmee precies waar
`AANTAL` staat. Per afleider, met de grond:

**`eff_unit` — niet toevoegen, en dat is geen keuze maar een eis.** De
functie geeft de EFFECTIEVE eenheid van een expressie; een kardinaliteit
heeft er geen, hoe zwaar het gevouwen attribuut ook weegt. MC3 maakt dat
meetbaar in plaats van beweerd: met de soort erbij leest de deling de
eenheid van de BRON als de eenheid van de TELLING en weigert luid, en
juist omdat de telling zwijgt is het handgespelde gemiddelde dat besluit
G1 aan de regelschrijver overlaat in clingo überhaupt uitdrukbaar. De
schrijffence merkte er niets van (`units_identical_ok` zwijgt zodra één
kant `None` is), dus de fout zou stil zijn gebleven tot iemand de deling
schreef.

**`provably_unitless` en `provably_pure_pct` — als PAAR laten staan.** Hier
is de val-door behoudend: "niet bewijsbaar eenheidloos" leidt tot een
weigering, nooit tot een verkeerd antwoord. Toevoegen zou meer vertaalbaar
maken, en het is even zeer `AANTAL`'s vraag als die van de nieuwe soort —
twee soorten die dezelfde kardinaliteit opleveren horen hier niet uit
elkaar te lopen. Gemeten waarom het geen kaartwerk is: over alle 703
documenten in golden, rood, negatief en de pariteitszone staat een telling
precies TWEE keer onder een vermenigvuldigende of delende knoop, en beide
keren in `toka/`, waar clingo niet draait. MC4 bevestigt het van de andere
kant: met beide tellingen erbij beweegt poort 6 geen cijfer. Een
verbreding zonder getuige is precies wat de huisregel verbiedt, dus zij
gaat niet stilzwijgend mee — zij is hier genoteerd als eigen vraag voor
een kaart die er een casus bij heeft.

### Stap 3 — Drools, beide takken

In de volgorde uit de overdracht. **(1) `Expr.FunctieKind`** krijgt de
constante. Dat is een bestand onder `frontends/` maar geen
emissiewijziging: de parser produceert de soort nergens en geen
grammaticaregel schrijft hem, dus de constante verbreedt wat er GELEZEN
kan worden en niet wat er geschreven wordt. Het record is de gedeelde
IR-vorm die de backend meecompileert, en zonder de constante haalt geen
enkel document met de soort de transpilers.

**(2) `ExecModelTranspiler`.** De soort komt in `isAggregatieKind`, zodat
de regel de aggregatieroute neemt, en krijgt in `vouw` een eigen tak vóór
de attribuutvouwen: dezelfde `CountAccumulateFunction` over de
ELEMENTvariabele, met de bron versmald tot de leden wier waarde bestaat.
Die `!= null` is dezelfde bewaking die de som/min/max-vouwen daar al voor
§4.12 zetten — dus de tak voegt een soort toe, geen mechanisme. De
`count`-vlag blijft AANTAL-eigen, want die stuurt óók de rolnaamopzoeking
(bij AANTAL is het argument een ROLnaam, bij AANTAL_GEVULDE een
ATTRIBUUTnaam, en die twee mogen nooit tegen elkaar gematcht worden).

**(3) `DrlTranspiler`.** `case AANTAL, AANTAL_GEVULDE -> "count"`, en de
`!= null`-bewaking uit de "geen telling"-voorwaarde getild. Dat was de
enige echte ingreep: in die tak is de bewaking geen voorzorg tegen een
halverwege afgeleide waarde maar de VOUW ZELF.

**(4) De loader.** De klasse-brede `Refusal` bleef staan en de soort
passeert hem nu. De getuige uit kaart 1 sloeg op `AANTAL_GEVULDE` en zou
dus voortaan niets meer meten; hij verspelt de vouw nu als `GEMIDDELDE` —
niet als willekeurig token maar als de vouw die besluit G1 bewust NIET
gebouwd heeft, zodat de getuige een BESLOTEN afwezigheid noemt en de dag
dat dat besluit herzien wordt vanzelf om een nieuwe stand-in vraagt.

**Mutatieproeven (elk gedraaid, gesweept en teruggedraaid, met
jar-herbouw in dezelfde keten).**

    MD1  exec: de != null-bewaking uit de gevulde-telling-tak
         -> alle drie de goldens FAIL in de EXEC-tak (verwacht 2,
            werkelijk 3), de DRL-tekst-tak blijft groen
    MD2  DRL-tekst: gevuldeTelling uit de foldScope-voorwaarde
         -> alle drie de goldens FAIL in de DRL-TEKST-tak, de exec-tak
            blijft groen
    MD3  exec: de soort uit isAggregatieKind
         -> alle drie verlaten het PLAYED-kanaal met
            "regel (waarde buiten scope)" — de EXPECTED_PLAYED-gelijkheid
            vangt precies dat, bij naam

MD1 en MD2 samen zijn het bewijs dat de twee takken werkelijk los van
elkaar gemeten worden: elke mutatie laat de andere tak ongemoeid, dus geen
van beide leunt op de ander.

### Stap 4 — de vierde getuige, die de ontwerpkaart afmaakt

De ontwerpschets noemde in §5 een VIERDE getuige: de bewaakte deling. Die
stond niet in de opdracht van deze kaart, maar toen de drie andere eenmaal
in alle vier de uitvoerders speelden was hij te meten in plaats van te
begroten — en hij bleek zonder verdere ingreep te spelen. Hij is daarom
alsnog gebouwd, met zijn rode tweelingbroer.

`corpus/golden/jutterswet-gevuldegemiddelde` spelt het gemiddelde met de
hand, zoals besluit G1 het laat: `SOM ÷ AANTAL_GEVULDE`, met "indien het
aantal gewaardeerde vondsten groter is dan 0" in het voorwaardendeel.

    jutter   waarderingen   gevuld   som   gemiddelde
    J1       4, 6, leeg     2        10    5
    J2       (geen)         0        0     leeg (de regel vuurt niet)
    J3       leeg           0        0     leeg (de regel vuurt niet)

Dit is de enige getuige waar de twee tellingen in een VOORWAARDE tegenover
elkaar staan, en waar het verschil iets kost: `AANTAL` is 1 voor J3, dus
een bewaking met de instantietelling laat de deling juist binnen en faalt.

`corpus/rood/aantalgevulde-ongewaakte-deling` is hetzelfde document met de
voorwaarde eraf, op J2. Alle DRIE de uitvoerders falen er, elk met een
eigen gepind fragment (ref en clingo op de deling, de Drools-runtime in de
consequentie). Daarmee is de ruil van besluit G1 niet meer alleen
uitgerekend maar gepind: stil verkeerd wordt luid fout, en de ontsnapping
die het besluit veronderstelt bestaat werkelijk.

Merk op dat dit tweetal alleen bestaat omdat de telling GEEN eenheid
afgeeft: de bron van dit gemiddelde is eenheidloos, maar MC3 laat zien dat
een telling die de eenheid van haar bron doorgeeft de deling in clingo
onvertaalbaar maakt. De ontwerpvraag over de eenheidsafleiders en deze
getuige zijn dus dezelfde vraag, van twee kanten.

## Result (2026-09-20)

`AANTAL_GEVULDE` wordt door ALLE VIER de uitvoerders beantwoord. Vier
goldens en twee rode casussen; elke uitvoerderstak met een eigen
mutatieproef gemeten en teruggedraaid; alle veertien poorten groen, elk
los gedraaid. De ontwerpkaart gaat hiermee mee naar `done/`.

### 1. Wat er landde

**clingo.** De benoemde weigerarm van kaart 1 is weg; op de plaats van de
setvouwen staat een `#count` over dezelfde `val/3`-literal waarmee de som
zijn lege bijdragers al overslaat. De soort krijgt geen eigen machinerie:
één `source_join`, één `val/3` per lid, hetzelfde die/dat-filter — alleen
het aggregaat verschilt. Twee dingen zijn wél eigen en staan in de
comment: de teltsleutel is het ELEMENT (zo telt een element dat langs twee
tussenknopen bereikbaar is één keer, en twee leden met dezelfde waarde
twee keer), en er is GEEN presence-guard (het lege antwoord van een
telling is 0, wat `#count` over nul tupels vanzelf geeft). De temporele
setbron houdt een eigen benoemde weigerzin met eigen hoofdstuknummer.

**Drools, beide takken.** `Expr.FunctieKind` krijgt de constante;
`ExecModelTranspiler` krijgt de soort in `isAggregatieKind` en een eigen
tak in `vouw` (dezelfde `CountAccumulateFunction`, bron versmald met
`!= null`); `DrlTranspiler` krijgt `case AANTAL, AANTAL_GEVULDE ->
"count"` en tilt diezelfde bewaking uit de "geen telling"-voorwaarde. De
`count`-vlag blijft AANTAL-eigen omdat zij ook de rolnaamopzoeking stuurt,
en het argument van de nieuwe soort is een ATTRIBUUTnaam.

**Het corpus.** Vier goldens en twee rode casussen, alle zes zonder
`bron.rgs` — wire-first, precies zoals besluit G9 het laat.

    corpus/golden/jutterswet-gevuldetelling    de telling naast de telling
    corpus/golden/jutterswet-gevuldepad        het meerstapsbereik (WF-102)
    corpus/golden/jutterswet-gevuldesoorten    de typeloosheid (G8)
    corpus/golden/jutterswet-gevuldegemiddelde de bewaakte handdeling (G1)
    corpus/rood/aantalgevulde-temporele-setbron   ch. 3.4c's uitsluiting
    corpus/rood/aantalgevulde-ongewaakte-deling   de prijs van G1

### 2. De melding voor de herpin

**`frontends/regelspraak/.../ir/Expr.java` is aangeraakt**, en dat is de
enige aanraking buiten backends, corpus, scripts en backlog. Het is ÉÉN
enumconstante in `FunctieKind`, tussen `LAATSTE` en `WORTEL`, zodat de
volgorde het aggregatieblok van het schema volgt. Geen emissiewijziging:
de parser produceert de soort nergens, geen grammaticaregel schrijft hem,
geen enkele `bron.rgs` in enige zone bevat hem, en poort 10 heremitteert
64 rgs-goldens byte-identiek — de constante verbreedt wat er GELEZEN kan
worden. Zij hoort hier en niet bij de latere dialectkaart omdat de
Drools-backend dit gedeelde IR-record meecompileert: zonder haar bereikt
geen document met de soort de transpilers.

Voor de toolchain-herpin betekent dat: de conformance-PR daar moet deze
constante overnemen (leesroute), maar hoeft er geen producent bij te
bouwen. De producentroute is kaart 3 (besluit G9).

### 3. De eenheidsafleiders — besloten als paar, en gemeten

`AANTAL_GEVULDE` gaat in GEEN van de drie afleiders en staat daarmee
precies waar `AANTAL` staat. De volledige afweging staat hierboven onder
"Het besluit over de drie eenheidsafleiders"; in het kort:

- **`eff_unit`: niet toevoegen, en dat is een eis.** MC3 maakt het
  meetbaar: met de soort erbij weigert clingo het handgespelde gemiddelde
  `puntensom ÷ AANTAL_GEVULDE(kg-bron)` dat `ref` gewoon uitrekent. De
  schrijffence zou er niets van gemerkt hebben, dus de fout zou stil
  gebleven zijn tot iemand die deling schreef — en de vierde golden is
  precies die deling.
- **`provably_unitless` en `provably_pure_pct`: als paar gelaten.** De
  val-door is behoudend (een weigering, nooit een verkeerd antwoord), het
  is even zeer `AANTAL`'s vraag, en er is geen getuige: over alle 703
  documenten in golden, rood, negatief en de pariteitszone staat een
  telling twee keer onder een vermenigvuldigende of delende knoop, beide
  keren in `toka/`, waar clingo niet draait. MC4 bevestigt het van de
  andere kant — met beide tellingen erbij beweegt poort 6 geen cijfer.
  Genoteerd als eigen vraag voor een kaart die er een casus bij heeft.

### 4. Poortuitkomsten (elk los gedraaid)

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  258 documenten, vloer 192
     3 check-verifier           PASS  258 golden schoon; 417 negatief, vloer 350
     4 check-case-schema        PASS  600 casusbestanden
     5 check-oracle             PASS  ref-suite 725, vloer 489; 564 cases
     6 check-clingo             PASS  backendsuite 456 (was 454), vloer 83;
                                      564 cases, ref = clingo
     7 check-noop               PASS  528 cases, 36 overlevers (ongewijzigd)
     8 check-rood               PASS  36 rode cases (was 34), vloer 26;
                                      72 fragmenten citeren geen bodemzin;
                                      Drools-been 9 fragmentgepind, 27 met reden
     9 check-catala             PASS  201 checks; 12 goldens
    10 check-regelspraak        PASS  327 tests; 64 rgs-goldens
    11 check-toka-parity        PASS  29 cases, nul divergenties
    12 check-drools             PASS  gespeeld 169 (pin, was 165),
                                      DRL-tekst 146 (pin, was 142),
                                      JUnit 504, vloer 239
    13 check-reftype-derivation PASS  131 twists (was 129), vloer 74;
                                      128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks; 2 goldens

**Bewogen pinnen en vloeren.** Alleen de twee GELIJKHEDEN van poort 12,
elk met een redenblok per golden bij de pin: `EXPECTED_PLAYED` 165 → 169
en `EXPECTED_DRL_PLAYED` 142 → 146, beide zuiver additief (geen regel
verplaatst, geen bestaande reden herschreven). Verder één entry op de
Drools-uitzonderingslijst van poort 8. Geen enkele VLOER is opgehoogd:
poort 6 (456 tegen vloer 83), poort 13 (131 tegen 74) en poort 12's
JUnit (504 tegen 239) groeiden vanzelf, en een vloer die niet hoeft te
bewegen wordt niet bewogen.

Poort 13 verdient een eigen zin, want kaart 1 deed er een voorspelling
over: de twisttabel raakt de TELLING niet (haar type is per constructie
eenheidloos) maar wél haar BRON, en die bron is bij deze soort een echt
getypeerde attribuutlezing. Gemeten: 129 → 131 twists, twee erbij, precies
de twee `kg`-annotaties op de vouwbronnen van golden 1 en 2 — en de
klem sloeg niet aan, dus de soort brengt geen positie mee die buiten de
tabel valt. De voorspelling klopte en is nu een meting.

### 5. Mutatieproeven, alle zeven werkelijk gedraaid

    MC1  clingo: de val/3-literal uit het contribuantenlichaam
         -> alle vijf casussen MISMATCH (3 waar 2 hoort)
    MC2  clingo: de teller op de WAARDE gesleuteld
         -> precies ÉÉN corpuscasus valt om plus de vormcheck in de suite
    MC3  clingo: de soort toegevoegd aan eff_unit
         -> corpus stil, proefsteen weigert wat ref uitrekent
    MC4  clingo: beide tellingen aan provably_unitless
         -> poort 6 beweegt geen cijfer
    MD1  Drools exec: de != null-bewaking eruit
         -> drie goldens FAIL in de exec-tak, DRL-tekst groen
    MD2  Drools DRL-tekst: gevuldeTelling uit de foldScope
         -> drie goldens FAIL in de DRL-tekst-tak, exec groen
    MD3  Drools exec: de soort uit isAggregatieKind
         -> drie goldens verlaten het PLAYED-kanaal bij naam

Werkboom na alle mutanten schoon; de Drools-mutanten inclusief jar-herbouw
in dezelfde keten, zodat geen meting op een oude jar berust.

### 6. Registervoorstel (hoofdsessie, serieel)

Voor `spec/CONSTRUCTS.md` rij 1 (de aggregatierij), aan te hangen aan het
blok dat kaart 1 voorstelde — de slotzin van dat blok luidt nu "Uitvoering
in de backends: kaart 2", en die wordt:

> **Uitvoering in de backends geland (2026-09-20, kaart 2):** clingo
> vertaalt de soort als een `#count` over dezelfde aanwezigheidsliteral
> waarmee de som zijn lege bijdragers overslaat, gesleuteld op het
> ELEMENT zodat de vlakke unie van een `bronPad` een element één keer
> telt; beide Drools-takken lagen hem naar hun bestaande telaccumulate
> over een bron versmald tot de gevulde leden. De vier uitvoerders
> antwoorden daarmee gelijk op vier goldens — de telling naast de
> instantietelling, het meerstapsbereik, de typeloosheid over tekst,
> datum, enumeratie en booleaan (`onwaar` telt als gevuld), en de
> handgespelde bewaakte deling — en twee rode casussen pinnen de twee
> weigeringen: de temporele setbron, die geen moment heeft om op te
> vouwen, en de ONgewaakte deling, die in alle drie de uitvoerders luid
> faalt. `Expr.FunctieKind` kreeg de constante omdat de Drools-backend
> het gedeelde IR-record meecompileert; dat is een LEESverbreding, geen
> emissiewijziging, en de producentroute blijft kaart 3 (besluit G9).
> De drie eenheidsafleiders van de ASP-backend kregen de soort NIET: een
> kardinaliteit heeft geen eenheid, en gemeten is dat een telling die de
> eenheid van haar bron doorgeeft het handgespelde gemiddelde
> onvertaalbaar maakt.

Voor `spec/PLAN.md` is er geen scopewijziging.

### 7. Wat bewust NIET gebeurde

- **`spec/**` onaangeraakt.** Kaart 1 normeerde ch. 4.3 en ch. 3.4c; deze
  kaart voert ze uit en voegt er geen letter aan toe. Dat is de toets die
  de kaart aflegt: een spectekst die pas klopt nadat de uitvoerder hem
  mocht herschrijven was geen spectekst.
- **Geen `bron.rgs`, geen `bron.catala_en`, geen `bron.flogic`.** Besluit
  G9; de poorten 9, 10 en 14 zagen dus niets bewegen dat van deze kaart
  komt.
- **De drie eenheidsafleiders ongemoeid**, met de meting die zegt waarom
  (§3).
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`** — voorstel in §6.

### 8. Openstaande flanken na deze kaart

1. `provably_unitless`/`provably_pure_pct` dragen geen van beide
   tellingen; de verbreding is behoudend uitgesteld en heeft in het
   gepoorte corpus geen getuige (§3).
2. De dialectspelling in de brontaal-frontend: kaart 3, besluit G9.
   Diezelfde kaart trekt `VOUWSOORTEN` in `RuleChecker.java` gelijk met
   `set_source_exempt`.
3. Het MOMENT van een gevulde telling over een tijdlijnattribuut blijft
   ongedefinieerd; ch. 3.4c zegt normatief dat de vorm luid faalt en alle
   VIER de uitvoerders doen dat nu ook — de twee OCaml-uitvoerders met een
   gepind fragment, beide Drools-takken met een per-vorm-weigering op de
   uitzonderingslijst van poort 8.
4. De OPGESOMDE `AANTAL` telt in `ref` nog steeds gevulde waarden —
   kaart 1's vondst, niet van deze kaart, kaartje
   `backlog/new/20260920-aantal-opgesomde-telling.md`.
