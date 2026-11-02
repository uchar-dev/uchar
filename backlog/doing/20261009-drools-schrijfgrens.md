# drools-schrijfgrens: de Drools-backend schrijft een waarde die niet in de decimalen van het doel past

*Gevonden op 2026-10-09 door de bouwer van stap 1 van `20261008-tms-verdeling-en-tijdlijnen` en door de hoofdsessie
nagemeten en vastgelegd volgens de afspraak dat een bestaande bug buiten de oude takken een kaart krijgt. Gemeten met
ref, clingo en de verifier uit `_build` van main `ac0f9008` en de shaded jar van die kaartbranch (`e9182168`).
Bijgewerkt op 2026-11-01 met onderzoek en voorstel; metingen op main `3d4676b88`.*

## Aanleiding

RUNTIME-SEMANTICS ch. 2.4 punt 1: een waarde schrijven waarvan de precisie de gedeclareerde decimalen van het doel
overschrijdt, is een fout; de auteur moet expliciet afronden. Ref controleert dat bij het schrijven (`ref/lib/world.ml`,
"the write boundary (ch. 2.4)"), clingo met een benoemde `viol`. De Drools-backend controleert het op geen van zijn twee
branches, ook niet bij een rekenende regel (nu nagemeten).

Document: `corpus/golden/jutterswet-gelijkdeling` zonder `roundingDecimals` op de verdeling, met de case
`case2-restant` (1001 in gelijke delen over twee jutters, het aandeel een geheel getal). Verifier PASS.

| Uitvoerder | J1.veilingdeel |
|---|---|
| ref | EF-006: value 1001/2 does not fit the declared 0 decimals |
| clingo | `viol("allocation: a share does not fit the target scale …","J1")`, zonder EF-code |
| Drools, exec-model en DRL-tekst | 500.5 |

Geen golden raakt het, want gate 5 speelt elke case eerst door ref.

## Onderzoek

Rapport, notities en metingen: `/Users/yeb/Developer/uchar-dev/research/precisie-en-afronding-2026-11-01/` (`rapport.md`,
`notes/`, `meting/`, `fable-advies.md`). De kern:

**Gedefinieerde precisie bestaat in uchar al als norm, en vraagt geen nieuw rekenmodel.** Rekenen gebeurt in exacte
breuken, een auteur rondt af met een expliciete `Rounding`, en een declaratie begrenst alleen wat er geschreven mag
worden (ch. 2.1, 2.4, 2.5, 2.9). Een gedeclareerde werkprecisie zou uitkomsten veranderen en is niet nodig. Wat ontbreekt
is de handhaving in Drools: **een begrensd attribuut draagt in rust een eindigende decimaal met ten hoogste `decimals`
cijfers, en een onbegrensd attribuut draagt voor elke lezer zijn exacte waarde.**

**Pas aan het einde of tussentijds afronden.**

| Bron | Tussenresultaten | Afronden |
|---|---|---|
| uchar | exact | alleen waar de auteur het schrijft; niet passend bij schrijven is een fout |
| Java `BigDecimal` zonder `MathContext`, PostgreSQL `numeric` | exact bij `+ − ×`, deling op een schaal | `setScale(d, UNNECESSARY)` is het dichtste bij de schrijfgrens |
| Python `decimal`, C#, Rust, DMN FEEL (34 cijfers), Moneta | afgerond op contextprecisie, half-even | impliciet bij elke bewerking |
| COBOL, PL/I, Ada, ABAP | afgeleid uit declaraties | afkappen of commercieel, op tussenresultaten |
| Python `fractions`, Haskell, Scheme | exact | geen gedeclareerde schaal, geen fout bij schrijven |
| Catala | `decimal` exact, `money` in centen | money-bewerkingen ronden meteen af: `$100/3*3 = $99,99`, `$100*3/3 = $100,00` |
| OpenFisca, PolicyEngine | `float32` | half-even, door de auteur |
| RegelSpraak (specificatie v2.3.0, §3.3, §6.1.2, §6.5; ALEF `BigRational`) | rationaal domein; zwijgt over tussenresultaten | expliciet vóór een toekenning; manier verplicht (§6.1.3, §13.4.17) |

De combinatie van uchar (exact, per veld een schaal, fout bij schrijven) vonden we in geen onderzochte taal als
standaardgedrag; regelrecht (MinBZK) heeft de schaal gedeclareerd maar niet afgedwongen. Tussentijds afronden verandert
uitkomsten op gewone bedragen: 21% en 15% over 100.000 bedragen, elk op centen, hangt bij 5,75% van de volgorde af.
Exact doorrekenen kost weinig: 360 perioden samengestelde rente in `fractions` 1 ms, noemer 1325 cijfers. Een
`Rounding` op een vaste plaats blijft een functie van haar invoer, dus de evaluatievolgorde van regels speelt daarbij geen
rol (eigen afleiding).

**Wetgeving en financiële systemen.** Teksten die over tussenstappen spreken, zeggen dat een niet-genoemde stap niet
afrondt: Vo. (EG) 1103/97 art. 4 lid 2, het Duitse PAP 2026 §3, IRS-instructies 1040, Handboek Loonheffingen §13.2.6, de
UWV-bedrijfsregels. Een voorgeschreven tussentijdse afronding staat als stap in de regel, met plaats en meestal
richting (Besluit Wfsv art. 2.6 lid 7, Regeling Wfsv art. 3.4b, § 32a EStG). Dat past bij de norm: de auteur schrijft die
stap als `Rounding`. Geen gelezen regeling noemt half-even. Art. 5a Uitvoeringsbesluit OB 1968 en HvJ C-484/06 laten de
plaats (regel of totaal) vrij; dat is een keuze van de auteur. Financiële software verdeelt het afrondingsverschil met
een eigen operatie; dat is in uchar `Allocation` met floor en rest. Art. 2.18 Wet IB 2001 rondt niet af, anders dan de
opdracht veronderstelde.

**RegelSpraak en ALEF.** §6.1.2 eist een expliciete afronding, maar het woord "statisch" in ch. 2.4 komt niet uit de
specificatie: die zegt niet wanneer dit wordt vastgesteld, noemt bij een literal zelf een waardeafhankelijke fout, en
spreekt zichzelf tegen over de decimalen van een deling (§6.5 tegenover het typeringsdocument §4.5). ALEF toetst alleen
statisch en met een bovengrens (een deling door een attribuut telt als oneindig veel decimalen), heeft geen toets in de
uitvoering, en laat bij een verdeling 1001/2 in een geheel attribuut toe (gelezen, niet gedraaid). §9.7 zwijgt over een
aandeel zonder afronding dat niet past.

**Drools zelf biedt hier niets:** geen schaalcontrole, geen annotatie voor decimalen, geen rationaal type. Zijn mvel- en
exec-model-infix voegen `DECIMAL128` toe, maar beide branches roepen eigen runtimefuncties aan, dus die route speelt niet.
De toets hoort in de eigen plan-laag; logische invoeging levert de volgordeonafhankelijkheid.

## Metingen

Alle op een kopie van HEAD; een proefbouw van de toets staat in `meting/proto.diff` (geen codevoorstel). Niet gedraaid:
`make gates`, de JUnit-suite.

| Document | ref | clingo | Drools nu | Proefbouw |
|---|---|---|---|---|
| `y = x gedeeld door 2`, x = 1001, geheel | EF-006 | EF-006 | `500.5` | EF-006 |
| idem, x = 1000 | PASS | PASS | PLAYED | PLAYED |
| `y = x maal 0,5`; `y = 1,5`; `y = t` met t = 1.50 | EF-006 | EF-006 | `500.5`; `1.5`; `1.50` | EF-006 |
| 90/7 alleen op een tussenstand, beide volgordes | PASS | PASS | PLAYED | PLAYED |
| 90/7 aan het fixpunt, beide volgordes | EF-006 | EF-006 | `12.85714285714286` | EF-006 |
| invoer 1.234 op 2 decimalen | EF-004 | EF-004 | speelt door | speelt door (niet gebouwd) |

De proefbouw gaf op 346 goldens dezelfde statusregels: 299 PLAYED en 47 EXCLUDE, op beide branches. Alle 2260 afgeleide
begrensde waarden aan het fixpunt passen, dus de pins van gate 12 bewegen niet.

**Dezelfde afwijking raakt een echte beslissing.** De actuele regelset van de werkbank (v2 `herleving-ww`) declareert de
factor C/D van art. 47 lid 1 WW als `Numeriek (getal)`, schrijft hem met een onafgeronde deling, en vergelijkt voor art. 20
lid 1 onder c WW het inkomen maal de factor met de herlevingsgrens. Met gemaximeerd dagloon 240, ongemaximeerd 360 en
inkomen 6851,25 is dat exact 4567,50, gelijk aan de grens, dus niet "groter dan" (`meting/werkbank/`):

| Uitvoerder | Grens overschreden? |
|---|---|
| ref | nee (PASS) |
| Drools, beide branches, nu | **ja** (0.6666666666666667 × 6851,25 = 4567,500000000000228) |
| Proefbouw met alleen de toets | ja |
| Proefbouw met de exacte waarde in rust, beide branches | nee (PLAYED) |
| clingo | weigert (eenheid in vermenigvuldiging) |

Daarmee is de exacte waarde in rust geen randgeval van de corpus (0 goldens declareren een onbegrensd attribuut) maar de
vorm waarmee een auteur een onafgeronde tussenwaarde over attributen heen draagt, en dat is wat de wet en de
uitvoeringsregels vragen. Op de 175 scenariogevallen van v2 spelen de huidige jar, de proefbouw met de toets en de proefbouw
met de exacte waarde allemaal (`PLAYED`); ref haalt er 157 en liep op de andere 18 tegen de meetgrens van 60 s per geval aan.
Eén geval heeft een niet-eindigende factor (130/150), maar ligt niet op de grens. De oude `v1/dagloon-ww` (9 onafgeronde
delingen naar begrensde doelen, ook `Bedrag`) gaat niet door de huidige frontend (eenheid `1/dg` naar een doel zonder eenheid),
en is dus niet nagemeten. Onder de norm geeft die vorm EF-006, en de oplossing is `Numeriek (getal)` plus een expliciete
afronding aan het eind.

Overige afwijkingen van ch. 2.1 tot en met 2.5, met metingen in `notes/drools-backend.md` par. 8: de verdeling rekent op
16 cijfers en rondt daarna nog eens af (rest `-0.03` bij 2999999999999999.97 over 3), het tijdsevenredig deel staat op 16
cijfers, `rsPlus`/`rsMin`/`rsMaal` ronden af op 34 cijfers (`(10^31 + 0,001) − 10^31` geeft `0.00`), de wortel rekent op
`DECIMAL64`-invoer. Op een begrensd doel en bij gewone bedragen ziet de corpus die niet.

## Voorstel

### Norm (voorstel op beschermd pad)

- **Ch. 2.4 punt 1 wordt een evaluatiefout op de waarde, EF-006, in plaats van de write (ch. 5.7).** "Static" vervalt, en
  de waarde telt, niet de `BigDecimal`-schaal: 1.00 past op een geheel attribuut. Dit beantwoordt (a): voor een regel en
  voor een verdeling kan het niet statisch, want 1001/2 en 1000/2 hebben hetzelfde type en de tweede moet spelen.
- **De statische poort komt erbij, in een eigen kaart (besluit Yeb 2026-11-02):** `20261102-static-precision-gate`, de
  ALEF-vorm in frontend en verifier, na deze kaart. De kandidaatregel van de v0-noot gaat daarin op. De toets bij uitvoering uit
  deze kaart blijft daarnaast staan: als vangnet, voor de verdeling, die de ALEF-vorm niet statisch toetst, en tot de poort
  er is.
- **Verdeling van de lagen.** De frontend houdt RGS-338 en RGS-340 als eigen diagnostiek (gate 10), de verifier toetst tot de
  poortkaart geen decimalen, en elke uitvoerder toetst de waarde op elke schrijfplaats.
- **Verdeling.** Een aandeel of rest die niet past is EF-006. Eén fout per verdeling × bron, alles-of-niets: geen `Value`
  voor de aandelen die wel passen, zoals ref de evaluatie terugrolt; `place` is de eerste misser in case-volgorde.
- **Tekst mee:** ch. 2.1 collapspunt 3 (de grens toetst, zij maakt niets decimaal), ch. 2.8 "target rounding" en ch. 3.7
  "assignment gate" (die fout, geen afronding), de zin in ch. 2.4 over Drools op de onbegrensde vorm, CONSTRUCTS rij 2 (de kandidaatregel),
  en één zin in ch. 5.7 dat de secundaire uitkomsten van een falende sessie niet genormeerd zijn: ref zet de wereld terug, Drools
  trekt de `Value` in.

De IR krijgt niets nieuws: geen werkprecisie, geen half-even (de `mode`-enum blijft gesloten), geen knoop voor afronden op een
veelvoud (`round(x/m) × m` is exact uit te drukken).

### Drools

1. **`StaticFunction.FIT_TARGET`** → `ComputeHelpers.fitTarget(value, decimals, place)` in de waardeterm, ná `restated` en in
   plaats van `COLLAPSE` op een begrensd doel. Toets zoals ref: `Breuk.van(value)` maal 10^d heeft noemer 1. Reken niet op "een
   `Breuk` past nooit": de waarde kan een eindigende `Breuk` zijn. Eén plan voor beide renderingen, in de bestaande `try`,
   dus een logisch `Fault`-feit dat met zijn lezingen verdwijnt. Geen code in `Outcome`, de setter, een eigen regel of
   `readFixpoint`: die zien de waarde te laat of laten een lezer de misser zien (`notes/drools-backend.md` par. 5).
2. **`WriteBoundaryFault`** met `named(label)` zoals `InputFormFault`, en `place`, `value` (de exacte breuk), `decimals` als
   parameters; catalogus nl en en.
3. **Exacte waarde in rust.** `Value` en `Decision` met `collapse = false`; de `collapse` verhuist naar `readFixpoint`. Op een
   begrensd doel is de bean dan verliesvrij, en het beancontract met de distributie blijft staan.
4. **De uitgang op de onbegrensde vorm blijft een gedocumenteerde decimaal (16 cijfers half-even),** op de bean en nergens anders.
   Hier wijk ik af van het Fable-advies, dat op termijn een weigering op naam voorstelt: de factor van art. 47 is uitvoer van
   de dienst, dus een weigering maakt de regelset onbruikbaar.
5. **`Contribution` met een niet-eindigende waarde wordt een benoemde weigering (`DROOLS-###`, soort limit),** geen stille collapse,
   zolang de exacte fold er niet is: anders geeft een som over drie keer 1/3 een onterechte EF-006 op `0.9999999999999999`.
   Raakt dit een golden, dan gaat `ExactSum` (hieronder) naar deze kaart.
6. **`Allocation` en `rsVerdeelOpVolgorde` exact, met één afronding:** `naarDecimal(d, FLOOR)` op de echte breuk. Ook hier wijk ik
   af van het advies, dat dit naar de vervolgkaart schuift: de toets hoort op het exacte aandeel, en anders zijn `value` en de
   rest onjuist op het geval dat deze kaart zelf opent.
7. **EF-004 op invoer, parameter en dimensiecel** in de case-lezer (`CaseRunner.valueToArg`). Zonder deze poort geldt de
   invariant niet.

### Snede

Deze kaart: de norm en de zeven punten. Eén verse bouwer, `written-value-at-fixpoint` kan parallel (geen gedeelde code, wel
seriële landing voor de pins). Daarna `20261102-static-precision-gate`.

Vervolgkaart `drools-exact-arithmetic`, geen haast want geen gewoon bedrag ziet het: `DECIMAL128` uit `rsPlus`, `rsMin`, `rsMaal`
(gemeten drie regels, 4 ns/op, geen golden); `ExactSum` in beide renderingen en `Contribution` zonder collapse; het tijdsevenredig
deel exact, met ch. 2.4 op de tijdlijn in ref én Drools (ref kent daar nu geen grens, clingo wel); de wortel eenmaal correct onder zijn
afronding; ch. 5.12 op `t/n` in plaats van "16 cijfers half to even", dat 1/3 en 3333333333333333/10^16 gelijk schrijft; de
gebroken exponent als "eenmaal correct afgerond", met weigering als benoemde limiet. Witnesses in de Drools-suite, want clingo's
bereik houdt deze getallen uit de differentiële corpus.

Los, elk klein: clingo's EF-006 op `x/3 maal 3`, waar de waarde past (een `CLINGO-###` soort limit); de zinnen ch. 2.5 "default when
no mode is written" en ch. 2.3 min/max; de javadoc van `Breuk` ("impossible-by-resolver-guard" klopt niet); de Catala-route (`round
of` en `money of` worden nu geweigerd, en S = 4 past niet op een tarief met vijf decimalen).

Pin-bump na deze kaart: een regelset die stil een waarde wegschreef die niet past, krijgt EF-006, zonder overgangsvlag. Op de
175 gevallen van v2 `herleving-ww` is er gemeten geen verschil met de proefbouw. De toolchain-commit noemt het, en de werkbank-sessie krijgt een tekst met het
geval van art. 20 lid 1 onder c.

## Keuzes die ik aan jou voorleg

Dit zijn voorlopige besluiten, gemarkeerd omdat ze verder gaan dan de bron of ervan afwijken. Bij elk staat wat er verandert als je
anders kiest.

1. *Besloten 2026-11-02:* allebei, eerst de toets bij uitvoering (deze kaart), daarna de statische poort in de ALEF-vorm
   (`20261102-static-precision-gate`).
2. **EF-006 op een verdeling, strenger dan ALEF en dan §9.7.** Kies je exact toelaten, dan vervalt de invariant op begrensde doelen, en
   ook de weigering die ref en clingo nu al geven.
3. **Exacte waarde in rust in deze kaart, niet apart.** Zonder dit geeft de toets een nieuwe onterechte fout en blijft de beslissing
   van art. 20 lid 1 onder c fout.
4. **16 cijfers op de bean van een onbegrensd attribuut.** Een `Breuk` of tekst in de uitvoer zou exact zijn maar breekt het
   beancontract met de distributie.
5. **Ch. 2.4 op de tijdlijn (vervolgkaart).** Dit verandert ref; de 103 segmenten in de corpus passen.

## Acceptatie

- (a) Rood eerst, op beide branches, in elke volgorde van de betrokken regels: `x gedeeld door 2`, `x maal 0,5`, een kopie en een
  literal naar een geheel attribuut, de verdeling van de aanleiding, initialisatie, default, creatie-initialisatie. Elk EF-006 met
  `place`, `value`, `decimals` als een logisch `Fault`-feit; dezelfde vorm die wel past, speelt.
- (b) Een tussenstand die niet past en een fixpunt dat wel past, speelt in elke volgorde (de vier documentfamilies m4a, m4c, m4d, m4f
  uit `meting/`).
- (c) Exacte waarde in rust: `1/3` op een onbegrensd attribuut maal 3 is 1 voor een lezer en een voorwaarde; eigen Jutterswet-witness
  voor "factor maal inkomen is precies de grens" in `RationalAtRestWitnessTest` (clingo kan de vorm niet dragen, dus geen golden), en
  dezelfde waarde in `ref/test/`.
- (d) EF-004 op attribuut, parameter en dimensiecel; `input-overprecision` en `-param` falen als EF-004.
- (e) Exacte verdeling: pot 2999999999999999.97 over 3 op 2 decimalen geeft 999999999999999.99 en rest 0; één fout per verdeling × bron.
- (f) Een rood geval `corpus/red/jutterswet-verdeling-past-niet` en rode gevallen voor `maal`, kopie en literal, met
  `expectedFault {code, params}` in ref, clingo en Drools; clingo's `allocation`-zin krijgt zijn afbeelding op EF-006
  (`backends/clingo/lib/translate.ml:758-760`).
- (g) Nagegaan en in het Result genoemd: of een falende default de lagere defaults verdringt (`decisionOf` keert terug vóór `Decides`;
  ref laat hem beslissen), en gate 17 op een regel met `FIT_TARGET` (de lezing moet de waarde vóór de toets zien).
- (h) `make gates` groen; `EXPECTED_PLAYED` blijft 299; de negen `NOFAULT`-regels op de schrijfgrens en de invoer in
  `scripts/check-red.sh` weg; `EF_PLACES` in `scripts/check-drools-codes.sh` bijgewerkt.

## Omvang

Middel. Plan- en DRL-tekstpins zijn niet geteld; `fout-op-tussenstand` moest bij een klein recordveld 22 plandumps bijwerken, en een
schrijfregel krijgt hier een extra aanroep in zijn tekst.

May touch: `backends/drools/**`, `backends/clingo/lib/translate.ml` (afbeelding), `ref/test/`, `corpus/red/<nieuwe gevallen>/`,
`spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `scripts/check-red.sh`, `scripts/check-drools-codes.sh` (voorstel), kaartverplaatsing.

Out of scope: de statische poort (`20261102-static-precision-gate`), `ref/lib` (gedrag verandert niet), de vervolgkaart
`drools-exact-arithmetic`.

## Result

Bouwer, 2026-11-02, op main `b3e49849a` (gerebased; de branch begon op `a0e0fa0ee`). Niet gemerged, niet gepusht.

### Commits

- `159813320` kaart naar doing
- `816c2ea0e` clingo: een aandeel dat niet past is EF-006
- `1a245e23a` drools: de schrijfgrens toetst de waarde, en rust is exact (Drools, rode gevallen, gate 8 en 8b-script: voorstel op beschermd pad)
- `c9903e8c2` spec: de schrijfgrens is een evaluatiefout op de waarde (voorstel op beschermd pad; ref/test)
- `d1a0792a4` drools: getuige van het besluit in de eenvoudige vorm
- en de commit van dit Result

### Wat veranderde

**Spec** (`spec/RUNTIME-SEMANTICS.md`, voorstel). Ch. 2.4 punt 1: EF-006 op de waarde, in plaats van de write; de waarde telt,
niet de getypeerde precisie en niet de schaal; een statische poort in de verifier is OPEN (de v0-noot houdt de kandidaatregel
als OPEN, voor wat zij niet beslist: het aandeel van een verdeling en een eindigend quotiënt dat niet past). Ch. 2.4 punt 3:
de Drools-zin beschrijft nu de exacte waarde in het werkgeheugen, de decimaal op de bean (16 cijfers half-even) en de weigering
van een vouw; nieuw uitgewerkt voorbeeld "factor maal inkomen is precies de grens". Ch. 2.1: de schrijfgrens is geen
collapspunt (twee collapspunten), met voorbeeld; ch. 2.9 volgt ("the two of ch. 2.1"). Ch. 2.8 en 3.7: geen "target rounding"
of afronding aan de gate; de gate op een tijdlijn is OPEN (ref schrijft exact, clingo weigert, Drools 16 cijfers). Ch. 4.2: de
floor eenmaal op de exacte breuk, aandeel en rest door de gate, één fout per verdeling × bron; ch. 4.4 een rij; ch. 4.6 twee
voorbeelden (exact vóór de floor; een aandeel dat niet past). Ch. 5.7: de secundaire uitkomsten van een falende sessie zijn
niet genormeerd (ref zet terug, Drools trekt de `Value` in), met voorbeeld. Ch. 5.7b: Drools emitteert `place`, `value`,
`decimals` voor EF-004 en EF-006; de vier nieuwe rode gevallen staan als getuigen van EF-006. Ch. 7.1: een besluit dat niet
past beslist wel. Elk voorbeeld staat in `ref/test/test_ref.ml` in een eigen blok na de ch. 4.6-voorbeelden: `WV-2.1-schrijfgrens-a..c`,
`WV-2.4-i..l`, `WV-2.4-p-factor`, `WV-2.4-p`, `WV-4.6-exact`, `WV-4.6-exact-rest`, `WV-5.7-schrijfgrens`, `WV-7.1-past-niet` (14
voorbeelden; de suite telt er 1789). IR-SPEC is niet aangeraakt.

**Drools** (`backends/drools`, de zeven punten):
1. `StaticFunction.FIT_TARGET` → `ComputeHelpers.fitTarget(value, decimals, attribute, creation)` in de waardeterm, ná
   `restated`, op regel, initialisatie en creatie-initialisatie met een begrensd doel; één plan voor beide renderingen. Een
   passende waarde komt als decimaal terug (een eindigende `Breuk` als zijn exacte decimaal). Bij een default staat de toets op
   de write (`StackFact.Value.decimals`, route `FITTED`, `ComputeHelpers.fitted`), zie (g).
2. `WriteBoundaryFault` (EF-006) met `place`, `value` (exacte breuk), `decimals`; de write-back noemt de plaats
   (`named(subject, label, inputOrder)`); catalogus nl en en (`EF-006`), `CaseRunner.faultKinds` leest code en parameters.
3. `Value` en `Decision` met `collapse = false`; `readFixpoint` collapset voor de bean en `DerivedValue`;
   `decisionsOf` geeft het besluit in dezelfde vorm. Het veld `collapse` blijft in het plan bestaan (de handplannen van de
   tests gebruiken het); geen plan van de transpiler zet het nog aan.
4. De bean van een onbegrensd attribuut krijgt `Breuk.naarDecimal()`: exact of 16 cijfers half-even (javadoc van `Breuk`
   bijgewerkt: de "resolver-guard"-zin klopte niet).
5. DROOLS-296 (`FOLD_OVER_UNBOUNDED_VALUE`, soort limit, nl en en): een vouw (som, minimum, maximum) over de waarde van een
   onbegrensd attribuut. Raakt geen golden en geen werkbank-geval, dus `ExactSum` bleef bij de vervolgkaart.
6. `Allocation` en `rsVerdeelOpVolgorde` rekenen in `Breuk`, met één `naarDecimal(d, FLOOR)` op de echte breuk; aandelen en
   rest zijn `BigDecimal` waar ze eindigen, `Breuk` waar niet. `payout` toetst aandeel en rest (`Payout.shareDecimals`,
   `restDecimals`, `fault`) en geeft bij een misser alleen de ene `Fault` op de bron (geen aandeel, geen rest, geen
   deponering), `place` de eerste misser in de volgorde van de invoerbeans (`Watch.inputPlace`).
7. EF-004 in de case-lezer (`CaseRunner.inputFits`, `InputPrecisionFault`, catalogus `EF-004`): attribuut (scalair en
   tijdlijn per segment) en parameter; een rationaal lexeem wordt eerst getoetst, daarna weigert DROOLS-042 de vorm.

**Clingo**: `allocation_share_misfit` als constante en zijn arm in `viol_code` op EF-006 zonder parameters (de zin noemt geen
place, value of decimals; het onderwerp is een label).

**Corpus**: `corpus/red/jutterswet-verdeling-past-niet` (gelijkdeling zonder afronding), `-halvering-past-niet` (maal 0,5),
`-kopie-past-niet` (taxatie 1.50 in een geheel bergloon), `-vast-bergloon-past-niet` (literal 1,5), elk `expectedFault
{EF-006, {}}`. Gemeten vóór de Drools-wijziging: ref EF-006 op alle vier (met place/value/decimals), clingo EF-006 op drie en op
de verdeling alleen de `viol` zonder code; na de clingo-commit EF-006 op alle vier. De pins zijn `{}`, omdat clingo bij EF-006
geen `value`/`decimals` emitteert en als `place` het kale attribuut (`veilingdeel`) waar ref en Drools `J1.veilingdeel` geven.

**Scripts** (voorstel): de negen `NOFAULT`-regels in `scripts/check-red.sh` weg; `EF_PLACES` in `scripts/check-drools-codes.sh`
kent `EF-004` en `EF-006` met `{place, value, decimals}`. Geen vloer veranderd; `EXPECTED_PLAYED` blijft 300 (op main sinds
`instance-depth-owner-hop`; de replay geeft 300 op beide takken).

### Rood eerst

Rode stand op `a0e0fa0ee` (detached worktree, de nieuwe testklassen en rode gevallen erin gekopieerd, niet gecommit):
`WriteBoundaryWitnessTest` en `RationalAtRestWitnessTest` 18 van 22 rood. Elk foutgeval "ran to a model without faulting"
(`NOFAULT`), ook de vier nieuwe rode gevallen en de negen `NOFAULT`-regels; de exacte verdeling gaf "verwacht 999999999999999.99,
werkelijk 1000000000000000.00" (rest -0.03); de invoer 11.50 op een geheel tijdlijnattribuut en `1/3` op twee decimalen speelden
of gaven DROOLS-042; de derde maal drie gaf "geheel: werkelijk 0.9999999999999999; kenmerk heel: werkelijk afwezig"; de factor
stond als `BigDecimal(0.6666666666666667)` in de feiten, het vrijgelaten inkomen 4567.500000000000228375 en "boven de grens"
aanwezig; de vouw sommeerde tot 0.9999999999999999. Groen op de oude code: de vier tussenstand-families van (b) (m4a, m4c, m4d,
m4f), omdat er daar geen toets was; die zijn rood met mutatie M6.

Mutaties, elk in een detached worktree van `c9903e8c2`/`d1a0792a4`, niet gecommit (script en logs in
`werk/drools-schrijfgrens/scratch/mutate.py`, `logs/mutation-*.log`):
- M1, de toets: `fitTarget` gooit niet → 8 van 18 rood (elk foutgeval `NOFAULT`, en de (g)-getuige).
- M2, EF-004: `inputFits` keert meteen terug → 3 rood (attribuut/parameter, tijdlijn, rationaal lexeem).
- M3, de exacte verdeling: het gelijke aandeel als `DECIMAL64`-quotiënt → 2 rood ("verwacht 999999999999999.99, werkelijk
  1000000000000000.00"; en bij 1001 over drie een fout op `V1.veilingrestant: -1/1000000000000` in plaats van op het aandeel).
- M4, de weigering van punt 5: de `throw` weg → de vouwgetuige rood ("P1.ploegsom: de waarde 9999999999999999/10000000000000000
  past niet").
- M5, het besluit blijft staan: geen controlefeiten naast de fout in de closure-rendering → de (g)-getuige in de eenvoudige vorm
  rood (de twee takken verschillen; de lagere regel schrijft). Op de eerste versie van de getuige, alleen de algemene vorm, bleef
  M5 groen; daarom de getuige in de eenvoudige vorm (`d1a0792a4`).
- M6, het logische feit: de `Fault` van een waardegevolg gesteld in plaats van logisch → 2 rood (m4a: "V1.aandeel: de waarde
  90/7 past niet" aan het fixpunt; m4d: de takken verschillen).

### Aangepaste verwachtingen, pins en vloeren

Pins die door de extra aanroep veranderen (het gedrag verandert expliciet): 23 testmethoden. Plandumps: `RulePlanWitnessTest`
(6), `ChannelPlanWitnessTest` (3), `DefaultsplanWitnessTest` (3), `CreationPlanWitnessTest` (2), `ValueTermWitnessTest` (2),
`RouteWitnessTest` (1, `PAYOUT` met drie parameters erbij en `FITTED`). DRL-tekst: `DrlValueFunctionsWitnessTest` (5),
`DrlConditionFunctionsWitnessTest` (1). Voorbeeld oud → nieuw (`RulePlanWitnessTest.a_subject_outside_a_stack`):
`value=RS_TIMES(Read 0, Const 2), … collapse=true` → `value=FIT_TARGET(RS_TIMES(Read 0, Const 2), Const 2, Const uitkering,
Const null), … collapse=false`, met de knopen een niveau dieper; in de DRL-tekst `Object v =
ComputeHelpers.collapse(new java.math.BigDecimal("10"));` → `Object v = new java.math.BigDecimal("10");`, en een constante in een
begrensd getal heeft nu een functie `return ComputeHelpers.fitTarget(new java.math.BigDecimal("5"), Integer.valueOf(2),
"registratiekosten", null);`. Handplannen: `FormsPlan` (drie `Payout`s met de nieuwe velden, twee tekstfragmenten),
`DefaultsplanWitnessTest` en `DrlCarriedValueWitnessTest` (`StackFact.Value` met `null`). Vloer in
`DrlPiecesCorpusWitnessTest`: "gevolgen die hun waarde dragen" 330 → 241 en "…, met de zin erboven" 258 → 165 (beide met en
zonder lezingen): een waardegevolg in een begrensd getal gaat nu door een functie. `TemporalInputWitnessTest`: de invoer 11.50
op het gehele attribuut `wachtvergoeding per maand` werd 11.00, want EF-004 weigert 11.50 nu, zoals ref. `RationalAtRestWitnessTest`
herschreven: de pin `value=BigDecimal(0.3333333333333333)` in de feiten wordt `Breuk("1/3")`; de pin op de bean blijft.

### (g)

1. Een falende default: met de toets in de waardeterm (de proefbouw) keerde `decisionOf` terug vóór `Decides`, en de lagere
   default (de regel "op 1,00") besliste en schreef naast de fout; ref laat de default beslissen en faalt op de write.
   Gerepareerd in de Drools-backend: bij een default staat de toets op de write, het besluit voegt zijn controlefeiten (`Decides`,
   of `Outcome` met de exacte waarde) naast de `Fault` in, en niet de `Value` en niet de deponering van zijn gevolg. In de
   algemene vorm toetst de afstemming aan de wortel, zodat een lid op zijn exacte waarde afstemt (met de toets in de term van
   het lid zou een conflict EF-027 van ref een EF-006 worden). Getuige en M5 hierboven; spec ch. 7.1. Niet veranderd: een andere
   evaluatiefout in het gevolg van een default keert nog terug vóór `Decides`; ref breekt dan de hele stapel op het onderwerp
   af. Dat is een secundaire uitkomst van een falende sessie (ch. 5.7).
2. Gate 17 op een regel met `FIT_TARGET`: de lezing van de waardeknoop is de term binnen de toets. Op het rode geval
   `jutterswet-halvering-past-niet` (verwachting `consistent` in plaats van de fout) geeft `--readings` `reading
   /rules/rules/0/versions/0 "J1" 0 Binary 500.5` en `HOLDS`, terwijl de vorm EF-006 geeft; ref's trace stopt daar met de fout van
   de sessie. Op de goldens draagt elke begrensde regel nu `FIT_TARGET`; gate 17 vergelijkt ze (zie de gates).

### Werkbankmeting

Werkbank `e27bed1`, `v2/herleving-ww`, het manifest opnieuw door de frontend van deze worktree (`scratch/werkbank.py`, alleen
lezen in de werkbank): 175 scenariogevallen plus de witness van art. 20 lid 1 onder c (`case1-factor-twee-derde-precies-op-de-grens`,
het scenario uit `meting/werkbank/`), op de eindjar: 176 × `PLAYED 1` op exec-model en 176 × op DRL-tekst. De witness verwacht
"I1 heeft geen herlevingsgrens overschreden" en speelt dus op beide takken; op de jar van `3d4676b88` gaf zij "kenmerk
herlevingsgrens overschreden: verwacht afwezig, werkelijk aanwezig" (`meting/werkbank/result.txt`). Ref niet opnieuw gedraaid.

### Voorlopige besluiten

1. Punt 5 als statische weigering (bij het bouwen, een vouw over een onbegrensd scalair attribuut, behalve een gevulde
   telling), niet bij het vuren: een weigering tijdens de fire heeft in deze backend geen route (weigeringen komen uit de bouw
   of de case-lezer; een uitzondering uit een gevolg wordt een ERROR). Raakt 0 goldens en geen werkbank-geval. Strenger dan
   "met een niet-eindigende waarde": ook een vouw over gehele waarden van die vorm wordt geweigerd.
2. De toets bij een default op de write (zie (g)), niet in de waardeterm zoals de kaart voor de andere vormen schrijft.
3. `place` van een creatie-initialisatie: `«creatie»(onderwerp).attribuut` (ref: `oc<i>::<label>.attribuut`); niet gepind.
4. `place` van een verdeling: de eerste misser in de volgorde van de invoerbeans, gemaakte objecten daarna; ref neemt de
   volgorde van de lijst van de verbindende rol. Gelijk waar de case zijn lijst in instantievolgorde schrijft (het rode geval).
5. EF-004: `value` als ref's lexeem (decimaal zonder nullen achteraan, anders t/n); een rationaal lexeem eerst getoetst.
6. EF-004 op een dimensiecel: niet gebouwd, er is geen route (het case-schema heeft geen celwaarde, en de case-lezer weigert
   invoer op een gedimensioneerd attribuut met DROOLS-047 vóór er een waarde gelezen wordt).
7. Clingo's verdelingsarm zonder parameters; de rode gevallen pinnen `{}`.
8. De zin over secundaire uitkomsten in ch. 5.7 (keuze van de norm: niet genormeerd, in plaats van een kant kiezen).

### Voorgestelde tekst voor `spec/CONSTRUCTS.md` rij 2

Vervang "Candidate WF (RUNTIME-SEMANTICS ch. 2.4): a possibly non-terminating value must pass explicit rounding before a bounded
write" door: "**Write boundary on the value (2026-11-02, card `drools-schrijfgrens`):** RUNTIME-SEMANTICS ch. 2.4 point 1 is the
evaluation fault EF-006 on the value, in place of the write, in all three executors (Drools: `StaticFunction.FIT_TARGET`, at the
write of a defaults stack `ComputeHelpers.fitted`; the input fence EF-004 in its case reader); a value of the unbounded form is
exact in Drools's working memory and a decimal on the bean. Red cases `jutterswet-verdeling-past-niet`, `-halvering-`, `-kopie-`,
`-vast-bergloon-past-niet`. The candidate WF rule (a possibly non-terminating value must pass an explicit rounding before a
bounded write) is OPEN, card `20261102-static-precision-gate`." `spec/PLAN.md`: geen scopewijziging.

### Voor `drools-exact-arithmetic`

`DECIMAL128` in `rsPlus`, `rsMin`, `rsMaal`; `ExactSum` in beide renderingen en `Contribution` zonder collapse (heft DROOLS-296
op); het tijdsevenredig deel exact, met ch. 2.4 op de tijdlijn in ref en Drools; de wortel (`toBd` geeft het radicand nog als
`DECIMAL64`); ch. 5.12 op `t/n`; de gebroken exponent. Nieuw gevonden: een onbegrensd attribuut dat een objectcreatie
initialiseert, gaat als decimaal op de nieuwe bean (de creatie collapset bij de constructie), dus een lezer ziet daar 16 cijfers;
en de grens van de kwalificatie over de gehele periode (`ComputeHelpers.wholePeriod`) leest een `Breuk` via `toBd`.

### make gates

Eén run op `d1a0792a4` (gerebased op `b3e49849a`), alle regels groen (`out/gates/20261102-114326`):

```
start 2026-11-02 11:43:26 d1a0792a4 card/drools-schrijfgrens
ocaml rc=0 3s ocaml-build: rebuilt (6 source path(s) newer than the stamp, e.g. backends/clingo/lib; 3s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 536 source files)
01-self-contained rc=0 1s
02-ir-schema rc=0 125s
04-case-schema rc=0 7s
03-verifier rc=0 36s
05-oracle rc=0 21s
06-clingo rc=0 261s
07-noop rc=0 12s
08-red rc=0 153s
09-catala rc=0 5s
10-regelspraak rc=0 217s
11-toka-parity rc=0 219s
12-drools rc=0 323s
13-reftype rc=0 260s
14-flogic rc=0 2s
15-graph rc=0 47s
16-scenario rc=0 30s
17-readings rc=0 239s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 11:48:52 326s red=0
```

Gate 17 groen: de lezingen van alle goldens, waar elke begrensde regel nu `FIT_TARGET` draagt, zijn gelijk aan de trace van ref.
