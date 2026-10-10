# tms-verdeling-en-tijdlijnen: de truth-maintenance-tak draagt verdelingen, tijdlijnen en dagsoorten

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08). De derde van vier bouwkaarten, na
`20261008-tms-expressievormen-en-rekendatum`; geen haalbaarheidsproef.
Tellingen uit de corpusdekking van de proeftak in dat `Result:` (bij (e)).*

## Aanleiding

Na de eerste twee bouwkaarten blijven drie groepen weigeringen over die de
exec-modeltak speelt:

- **verdeling** (12): `Allocation` naar rato en op volgorde, met de degradaties
  van RUNTIME-SEMANTICS ch. 4.3 en 4.4 op het kanaal;
- **tijdlijnen**: een tijdsafhankelijk attribuut (9), `wholePeriod` in een
  `CharacteristicCheck`, `Comparison` of `Predicate` (3), tijdsafhankelijke
  kenmerken en parameters, en `StartPointDetermination`;
- **dagsoorten**: een duur in dagsoorten (3), `DayKindDefinition` (2) en
  `BoundDay`.

Het `Result:` noemt een verdeling een berekening over een gebonden
verzameling, en een tijdlijn een waarde in een standfeit. De tijdlijnalgebra
van ch. 3 staat al in de runtime, en de staande tijdlijndoelen van ch. 7.8 in
`transpiler/kern/StaandeDoelen` (`staandTemporeel`).

Goal: de tak draagt verdelingen, tijdlijnen en dagsoorten; daarna weigert hij
geen golden meer die de exec-modeltak speelt, zodat
`20261008-tms-omschakeling` kan beginnen.

## Wat de tak erbij krijgt

1. **Verdeling**: de verdeling als berekening over de verzameling die de LHS
   bindt (de ontvangers met hun gewichten, ook afgeleide); de aandelen als
   logische feiten; de degradaties als feit op het kanaal.
2. **Tijdlijnen**: een tijdlijn als waarde in een standfeit; afgeleide
   tijdsafhankelijke attributen en kenmerken; `wholePeriod`; de staande
   tijdlijndoelen van ch. 7.8; `StartPointDetermination`; tijdsafhankelijke
   parameters.
3. **Dagsoorten**: `DayKindDefinition` met `BoundDay`, en een duur in
   dagsoorten.

## Acceptatie

- (a) Rood-eerst getuigen, elk in elke volgorde van de betrokken regels in het
  model:
  - een verdeling naar rato en een op volgorde, met een gewicht dat later
    afgeleid wordt;
  - twee verdelingen met één gedeelde ontvanger (de vorm van
    `20260923-drl-verdeling-dubbelbezet`, die de exec-modeltak draagt);
  - een degradatie op het kanaal;
  - een tijdsafhankelijk attribuut dat uit tijdsafhankelijke invoer wordt
    afgeleid, en een waarvan de invoer blijft staan (ch. 7.8);
  - `wholePeriod` in een voorwaarde;
  - een dagsoortdefinitie met een duur in die dagsoort.
- (b) De corpusgetuige heeft geen FAIL; de vloer gaat naar het gemeten aantal.
  Een doorloop van beide takken over het hele corpus toont dat de tak elke
  golden speelt die de exec-modeltak speelt (240 bij het schrijven van deze
  kaart), met dezelfde namen; het `Result:` geeft die doorloop en noemt elke
  golden die nog verschilt, met de reden.
- (c) De grep van de eerste kaart over `transpiler/tms/` geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd: gate 12 groen met
  beide pins gelijk; alle gates van `CLAUDE.md` groen.

## Raakvlakken

- Bouwt op de eerste twee bouwkaarten (aggregaties, fouten als feit,
  schrijvers, rekendatum).
- `20260923-drl-verdeling-dubbelbezet` is een fout in de destructieve
  DRL-teksttak; die gaat op in `20261008-drl-teksttak-op-truth-maintenance`.
  Hier dient zijn vorm alleen als getuige.
- Een golden die na (b) nog verschilt, gaat niet naar een lijst: de omschakeling
  begint pas als er geen verschil meer is.

## Review

Zoals in de eerste bouwkaart: een lens na elke bouwstap, een eindlens over de
hele diff, na elke stap een doorloop van het hele corpus, en een lensvondst
alleen met een concreet faalscenario op echte invoer of een getuige.

## Omvang

Middelgroot. De runtime heeft de verdeling en de tijdlijnalgebra al; het werk
zit in het binden van de verzamelingen en tijdlijnen in de LHS.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`,
`cases/CaseRunner.java` en `cases/DroolsBackend.java` voor zover het terugzetten
van tijdlijnen dat vraagt, tests onder `backends/drools/src/test/java/`, de
sectie over de tak in `backends/drools/README.md`, kaartverplaatsing. NB:
serieel met andere Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak, `transpiler/kern/`,
`scripts/`, `corpus/`, `spec/`, de toolchain.

## Result (2026-10-10)

*Branch `card/tms-verdeling-en-tijdlijnen`, claim `0cb35584` op main `ac0f9008`, tip `e606e9ce`. Gemeten met een jar
uit een bevroren kopie van elke stapcommit, door de drie Drools-takken over alle 294 goldens met cases
(`/tmp/lens-tms-k3/stapklaar.sh`, die ook elke weigering per golden telt, niet alleen de eerste).*

**Wat de kaart opleverde.** De tak met truth maintenance draagt nu verdelingen, tijdlijnen en dagsoorten. Volgens het
besluit van 2026-10-08 vervangt hij de destructieve exec-modeltak.
- Hij speelt **alle 242 goldens die de exec-modeltak speelt**, met dezelfde namen (acceptatie (b)); vóór de kaart waren
  dat er 196.
- In totaal speelt hij 248 van de 294 goldens, waar dat er 201 waren, zonder FAIL of ERROR.
- Zes goldens speelt alleen hij: `anchor/jutterswet-erflijn`, `anchor/jutterswet-vindersloon`,
  `jutterswet-afdrachtstapel`, `jutterswet-klipwacht` (sinds deze kaart), `jutterswet-vondsttarief` en
  `synth2-afrondingscombinaties`.
- De 46 die hij weigert, weigert ook de exec-modeltak: 37 in de lader, 3 op een beannaambotsing en 6 op een vorm met
  reden.
- De exec-modeltak en de DRL-teksttak bleven na elke stap byte-gelijk aan de nulmeting.

Daarmee kan `20261008-tms-omschakeling` beginnen.

| Commit | Inhoud |
|---|---|
| `e1efd88f` | stap 1: de verdeling als berekening over de gebonden ontvangers |
| `a727137a`, `e9182168` | stap 1: README, vloer 201 → 216 |
| `e3b0ed22`, `8c729f8d` | stap 1: getuigen (lege pot op een tussenstand, tweede schrijver) |
| `89e662bf` | lensfix stap 1: een lege pot is geen verdeling, in elke methode (ch. 4.4 v1) |
| `b9f77aa7` | lensfix stap 1: getuige H pint de waarde van ref |
| `5553e09a` | stap 2: een tijdlijn als waarde, gelezen, getotaliseerd en geschreven |
| `c2005edf`, `f102549f`, `24c8688b`, `7546bda0` | stap 2: getuigen (volgordes, per lid, ooit-uniciteit, weigeringen) |
| `36cad47b` | lensfix stap 2: "gelijk aan" in een vergelijkingsknip luid weigeren |
| `2e09716a` | stap 2: README, vloer 216 → 235 |
| `c08ac038` | lensfix stap 2: een tarief over een raster zonder gedeclareerde eenheid weigeren |
| `98047f6c`, `0e879163` | lensfix stap 2: de invoervorm van een tijdlijn volgen (ch. 8.4), README |
| `6442495b` | stap 3: het eigen totaal zonder knip faalt zonder rekendatum |
| `2f85de14` | stap 3: "gedurende het gehele jaar" en "de gehele maand" |
| `f9808af7` | stap 3: dagsoorten als constante van het document |
| `10e5c737`, `ebba7b88`, `58614e85` | stap 3: kaartnaam in de verdelingsgetuige, README, vloer 235 → 248, javadoc |
| `eada99d6` | eindfix: de weigeringen van een gedimensioneerd attribuut noemen hun vorm |
| `e1d0d1df` | eindfix: README, wat een afnemer naast de bonen stelt (`Emmerinvoer`, `Lijstplaats`) |
| `e606e9ce` | herkeuring eindfix: de javadoc van `Lijstplaats` noemt elke regel die de plaats leest |

### Wat er staat, en waarom zo

- **Verdeling** (stap 1, ch. 4).
  - **Twee regels, naar het model van `Bijdrage`.** De eerste zet per bron en ontvanger logisch een waardegelijk
    `Ontvanger`-feit, met het gewicht, de sleutel en het maximum als gebonden stand. De tweede heeft per bron één
    activering, met de pot, de voorwaarde en de geldigheid (op `Rekendatum`) en een collect over de ontvangers.
  - **De berekening** is één statische functie zonder DSL-typen, `Verdeling.bereken`, zodat de DRL-teksttak van
    `20261008-drl-teksttak-op-truth-maintenance` haar kan aanroepen.
  - **De uitkomsten zijn logische feiten**: de aandelen, het restant en de deponeringen per ontvanger. Een later
    afgeleid gewicht of een ontvanger die later bijkomt, herrekent dus de hele verdeling (ch. 4.3).
  - **Volgens de spec, waar ref nog niet volgt** (besluit van de hoofdsessie). Een lege pot is in elke methode "geen
    verdeling", en een negatief gewicht naar rato telt als 0 met een deponering; dat zijn de v1-besluiten van ch. 4.4.
  - **Twee afwijkingen van de runtimehulp.** De tak geeft `rsVerdeelOpVolgorde` de invoer zoals ch. 4.2 haar leest:
    een tiegroep met alleen lege gewichten, en sleutels van één waarde in twee schalen. `runtime/` is niet gewijzigd.
  - **Een gecreëerde relatie** als koppeling wordt gedragen.
- **Tijdlijn als waarde** (stap 2, ch. 3 en 7.8).
  - **`Tijdvak`.** Een tijdlijn in een feit is een onveranderlijke `List<Tijdvak>`, met gelijkheid per segment (ch. 3.1
    en ref `write_timeline`: 7 = 7.00, een andere knip is een andere waarde). Het record heeft de getters en de
    constructor van de gegenereerde `Segment`, zodat de rekenhulpen het ongewijzigd als segmentklasse nemen.
  - **Omzetten op twee grenzen**: bij het binden van de invoer van de boon, en in `zetAfgeleideWaarden`.
  - **De vormen**: het totaal over de eigen tijdlijn met knip, het totaal per lid, de schrijvers naar rato en
    dagentelling met het staande domein, en ooit-uniciteit via `rsOoitWaarden`.
- **De invoervorm** (fixronde van stap 2, ch. 8.4).
  - **`Emmerinvoer`.** Op het pad van de tak stelt `CaseRunner` per asOfDate-emmerscalair een feit `Emmerinvoer`. De tak
    leest elk tijdsafhankelijk attribuut via zijn stand. Een vorm die de verkeerde invoervorm vindt, geeft een logische
    fout (`Invoervormfout`), alleen waar de evaluatie de vorm bereikt.
  - **Waar ref niets schrijft of 0 geeft** (de basis van een pro rata, de vergeleken tijdlijn van een knip, "gedurende
    het gehele"), gaat de spec voor; er komt een kaart voor ref en clingo.
  - **Een tarief** waarvan het document de eenheid van het raster niet of tweemaal declareert, weigert de tak zoals ref.
- **"Gedurende het gehele"** (stap 3, ch. 3.9, 3.9a).
  - **Wat de toets leest**: de gebonden tijdlijn (de eigen, of langs een pad naar één) en de gebonden grens, met
    `rsGeheelPeriode`. Geen regel bindt de rekendatum.
  - **In een kwantorvouw** is de vergelijking de matrix van de bestaande vouw, met haar kortsluiting en de NIET per lid.
  - **Een lege grens** maakt de toets onwaar (ch. 3.9).
- **Dagsoorten** (stap 3, ch. 3.8, WF-031).
  - **Een definitie is een constante van het document.** Ze leest niets uit het werkgeheugen en wordt pas gecompileerd
    waar een vorm haar gebruikt. Een ongebruikte definitie die geen uitvoerder compileert, laat het document dus spelen
    (`jutterswet-schouwkalender`).
  - **De reikwijdtefout van de wandeling** is een logische fout.
  - **Een definitie die een parameter leest**, weigert de tak met naam (`jutterswet-havenrooster`, die de
    exec-modeltak ook weigert).

**Buiten "May touch", klein.**
- **`CaseRunner`.** De kaart staat hem toe "voor zover het terugzetten van tijdlijnen dat vraagt". Op het pad van de tak
  stelt hij nu ook `Emmerinvoer`: dat is de invoerkant van een tijdlijn, zonder gevolg voor de andere takken (gate 12
  groen met gelijke pins).
- **Twee bestaande getuigen.** `DatumstapBereikTakkenWitnessTest` en `DatumstapDelingWitnessTest` pinden voor de tak de
  weigering van de dagsoorten; hun eigen javadoc zei dat ze waarden moesten verwachten zodra hij die draagt.

### Acceptatie

- **(a)** Rood-eerst getuigen, in elke documentvolgorde van de betrokken regels, plus een steekproef van gegenereerde
  volgordes waar een tussenstand anders niet optreedt. De verwachtingen zijn bevestigd door ref, en door clingo waar die
  de vorm vertaalt; weigert clingo, dan staat dat in de getuige. Het gaat om `TmsVerdelingWitnessTest`, `TijdvakTest`,
  `TmsTijdlijnWitnessTest`, `TmsTijdlijnVouwWitnessTest`, `TmsOoitUniciteitWitnessTest`,
  `TmsTijdlijnWeigeringWitnessTest`, `TmsInvoervormWitnessTest`, `TmsTotaalRekendatumWitnessTest`,
  `TmsGeheelPeriodeWitnessTest` en `TmsDagsoortWitnessTest`. Elke vorm van de kaart heeft er een:
  - een verdeling naar rato en een op volgorde, met een gewicht dat later afgeleid wordt;
  - twee verdelingen met één gedeelde ontvanger;
  - een degradatie op het kanaal;
  - een tijdsafhankelijk attribuut dat uit tijdsafhankelijke invoer afgeleid wordt, en een waarvan de invoer blijft
    staan;
  - "gedurende het gehele" in een voorwaarde;
  - een dagsoortdefinitie met een duur in die dagsoort.
- **(b)** De corpusgetuige heeft geen FAIL. `EXPECTED_MIN_PLAYED` gaat van 201 naar 248. Eerste weigering op het gat,
  van nulmeting naar eind: 46 → 0. De verdeling ging van 15 naar 0, het tijdsafhankelijke attribuut van 10 naar 0,
  het totaal over de eigen tijdlijn van 6 naar 0, "gedurende het gehele" van 8 naar 0, het totaal over een
  tijdsafhankelijk lid van 3 naar 0, en de dagsoorten van 4 naar 0.
- **(c)** `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over `transpiler/tms/` geeft niets.
- **(d)** De exec-modeltak en de DRL-teksttak zijn na elke stap byte-gelijk aan de nulmeting. Gate 12 is groen met
  played 242 en DRL 196, JUnit 1404 tests, 0 failures, 0 errors, 0 skipped. Alle 16 gates zijn groen op `e9182168`,
  `0e879163`, `58614e85` en `e606e9ce`. De eindfix verandert alleen tekst en een weigerzin; de doorloop op `e1d0d1df`
  was gelijk aan die van stap 3.

### Lensvondsten

Er liepen zeven lenzen op sol6, elk één premium request: na elke stap een lens, een vroege lens op de hoofdcode van
stap 2, en na elke fixronde een herkeuring. Vóór de landing kwam er een eindlens op Opus 5.5 bij. De hoofdsessie mat
elke vondst na met een proefdocument dat de verifier goedkeurt.

Aanvaard en gefixt:
- **stap 1.** Getuige H pinde de waarde van ref niet (`b9f77aa7`).
- **hoofdcode stap 2.**
  - De invoervorm van een tijdlijn ging verloren (`98047f6c`). De tak antwoordde waar ref en clingo een fout geven: 135
    bij een lid van een vouw, 11 bij de knip, inconsistent bij de ooit-sleutel en 126 bij het eigen totaal over een
    volledige tijdlijn. Bij de pro rata over een emmerscalair schreef hij 306.
  - "Gelijk aan" met een lege kant in een knip gaf 0 waar ref 50 geeft (`36cad47b`, nu met naam geweigerd).
- **herkeuring stap 2.** Een tarief waarvan het document de noemer niet declareert, gaf 120 waar ref faalt
  (`c08ac038`).
- **eindlens (Opus 5.5), ADVIES.** Een kwantorvouw over een meervoudige rol met een "gedurende het gehele"-matrix over
  een attribuut vraagt een `Lijstplaats` per lid, ook zonder emmerscalair; een afnemer zonder die feiten krijgt een
  IllegalStateException, en de README noemde alleen `Emmerinvoer` (`e1d0d1df`). De eindlens vond geen blokkerend punt.
- **bijvangst van de bouwer van stap 3.** De weigering van een gedimensioneerd attribuut op een tijdlijn gaf nog de
  reden "tijdlijnen zijn buiten de proef" (`eada99d6`).
- **herkeuring eindfix, ADVIES.** De javadoc van `Lijstplaats` en `leestLijstplaatsen` noemde alleen de filterfout als
  lezer (`e606e9ce`).

Afgewezen, met reden:
- **herkeuring stap 2, ADVIES.** "Clingo bevestigt de vouw per lid over een afgeleide tijdlijn niet." Clingo weigert
  die vorm met een vertaalfout, en de getuige zegt dat. Ref bevestigt de exacte vorm.
- **stap 3, BLOKKEREND.** "Een tijdsafhankelijk kenmerk verdwijnt na een gemaakte koppeling." Nagemeten met een
  feitcreatie die de koppeling maakt, vier cases en beide regelvolgordes: de tak geeft telkens de verwachte uitkomst,
  gelijk aan clingo. Het lid zonder schrijver leest de route van de boon, niet uit een `KenmerkStand`. De meting vond
  wel een bug in ref: zie de bijvangst.

### Bijvangst

Gemeten, niet in deze kaart gerepareerd. Nieuwe kaarten:
- `20261009-verdeling-lege-pot-en-negatief-gewicht`: ref en clingo volgen de twee v1-besluiten van ch. 4.4 niet, en de
  zin over het negatieve tiegewicht in ch. 4.2 is dubbelzinnig.
- `20261009-drools-schrijfgrens`: geen Drools-tak controleert ch. 2.4 punt 1. Een aandeel van 500.5 in een geheel getal
  speelt, waar ref en clingo falen.
- `20261009-runtime-tijdconditie-leeg`: `rsTijdConditie` leest een lege kant van een knip als geen periode.
- `20261009-emmerscalair-in-een-lezing`: ref en clingo lezen een emmerscalair als geen tijdlijn in drie lezingen waar
  ch. 8.4 een fout vraagt.
- `20261009-ref-gehele-periode-lege-grens`: ref leest "ongelijk aan" tegen een lege grens als waar, tegen ch. 3.9 in.
- `20261009-ref-gehele-periode-over-gemaakte-koppeling`: ref houdt de vacuüme uitkomst van vóór een creatie vast.

Aanvullingen:
- **`20261009-clingo-gemaakt-lid-met-twee-waarden`.** Een rekenende regel of initialisatie naast een verdeling op het
  aandeel geeft clingo twee waarden.
- **`20261008-fout-op-tussenstand`.** Ref is volgordeafhankelijk op delen van de nieuwe getuigen.
- **`20261008-tms-omschakeling` en `20261008-drl-teksttak-op-truth-maintenance`.** Wat alleen in de oude takken misgaat
  (afspraak van 2026-10-09), en voor de omschakeling de invoervorm voor een afnemer en de schaal van de verdeling.

### Werkwijze

- **Bouwers.** Zeven opus55-bouwers:
  - stap 1, met een verse bouwer voor zijn fixronde;
  - stap 2 in twee delen, met een overdracht;
  - een fixronde van stap 2, en een eindfix;
  - stap 3, die vanaf 21:23 al las terwijl stap 2 afgemaakt werd.

  De bouwers van stap 2 en van zijn fixronde stopten rond 600k context met een overdracht. Die van stap 3 liep tot 790k,
  boven de afgesproken grens van 550k, maar maakte zijn stap af.
- **Fable 5.1.** Vóór de bouw gaf Fable ontwerpadvies op aanbod van de eigenaar: een tijdlijn als eigen record, de
  verdeling als `Ontvanger` plus één activering per bron, en de dagsoort als constante. De bouwers namen het over en
  verantwoordden hun afwijkingen.
- **Volgorde.** De lens op stap 3 en de Opus-eindlens liepen tegelijk, zodat één fixronde beide afhandelde.
- **Afwijking van de werkafspraken.** De bouwer van stap 3 gebruikte één keer `rm -rf` via `find -exec` met een variabel
  pad, binnen zijn eigen `mktemp`-map, tegen de afspraak in. Er is niets anders geraakt.

### Open punten (voor de landing)

1. **Afnemer.** Een afnemer van de publieke vorm moet per emmerscalair `Emmerinvoer` stellen (ch. 8.4). Doet hij dat
   niet, dan leest de tak elke invoertijdlijn als volledige tijdlijn. Voor `20261008-tms-omschakeling`, punt 12.
2. **Schaal.** De verdeling vuurt per bron opnieuw bij elke verandering van een ontvanger, dus O(N²) als N ontvangers
   één voor één binnenkomen. Voor de schaalproef van de omschakeling.
3. **Wat de tak bewust weigert**, zonder golden en met de reden in de README:
   - een variabel startpunt;
   - een tijdsafhankelijke parameter;
   - een som over tijdlijnen;
   - een tijdlijndoel met een tweede schrijver;
   - een tijdsafhankelijk kenmerk als doel;
   - het periodevenster op een tijdlijndoel (`jutterswet-pachtvenster`, ook de oude takken);
   - "(on)gelijk aan" in een knip;
   - de ontkenning binnen de kwalificatie op een kenmerk;
   - een kwalificatie langs een pad of tussen twee tijdlijnen;
   - `dayKind` als voorwaarde;
   - een duur in dagsoorten tussen datum-tijden;
   - een dagsoortdefinitie met een parameter;
   - een tweede schrijver van een verdelingsdoel;
   - een vouw per lid, of een kwantorvouw met de kwalificatie als matrix, over een gecreëerde relatie.
4. **Spelling van de noemer.** Een noemer gespeld als "maand" naast een gedeclareerde "mnd" (alleen in handgeschreven
   IR) aanvaardt de tak op spelling; ref weigert.
5. **Registertekst.** Geen tekst voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: deze kaart verandert de spec niet. De
   keuzes "spec boven ref" staan hierboven, met hun kaarten.
