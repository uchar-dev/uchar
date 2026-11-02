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
