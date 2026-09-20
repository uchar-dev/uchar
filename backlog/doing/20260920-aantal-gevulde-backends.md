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

De `bronPad`-golden: J1 voert B1 en B2; B1 bergt V1 (3,00) en V2 (geen
gewicht); B2 bergt V3 (5,00) én V1. De vlakke unie is {V1, V2, V3}, dus
`AANTAL` = 3 en `AANTAL_GEVULDE` = 2 — V1 telt één keer hoewel hij langs
twee boten bereikbaar is.

De niet-numerieke getuige: drie vondsten, per waardesoort twee gevuld en
één leeg, telkens 2. De booleaanse rij draagt `onwaar` als gevulde
waarde — leeg is de afwezigheid van een waarde, `onwaar` is er een.

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
