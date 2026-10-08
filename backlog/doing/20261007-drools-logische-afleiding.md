# drools-logische-afleiding: een proeftak in Drools die afleidt met truth maintenance

*Vastgelegd door de hoofdsessie op 2026-10-07 op verzoek van de eigenaar.
Bron: een inventaris van de exec-modeltak en een proefstuk in Drools 8.44
(27 tests, in de research-repo onder `drools-logische-afleiding-2026-10-07`).
Regelnummers en tellingen op uchar `af8cd4db`.*

## Aanleiding

De exec-modeltak schrijft een afgeleide waarde destructief: een setter op de
boon, gevolgd door `drools.update` (`werkBij`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`
regel 885–892). Truth maintenance wordt nergens gebruikt
(`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` regel 323–325). Drools'
agenda bepaalt daardoor wanneer een regel opnieuw wordt geëvalueerd, welke
regel eerst vuurt en of een regel nog eens vuurt. De tak legt de semantiek van
RegelSpraak daar met zes maatregelen overheen:

1. `NO_LOOP` op elke regel die zijn eigen onderwerp schrijft (onder meer
   regel 594, 633, 821–832, 8564);
2. salience uit `transpiler/Stratificatie.java` en `groepVolgorde`
   (regel 9040), zodat een ontkennende lezing pas na haar producenten vuurt;
3. klassereactiviteit via een ondoorzichtige `s -> true`-expressie
   (regel 524–564) en `.watch` op de bronpatronen van de verdeling, omdat
   Drools in een lambda niet ziet wat hij leest;
4. `update` alleen bij een gewijzigde waarde (`Objects.equals` in `werkBij`,
   `rsTijdreeksGelijk` voor tijdlijnen), anders blijven regels elkaar
   activeren;
5. de bewaking van initialisaties met `initEigen` (regel 850), omdat Drools
   niet bijhoudt welke regel een waarde schreef;
6. het vuurplafond `fireAllRules(100_000)` (`cases/CaseRunner.java` regel 500)
   met `Schrijfconflict` voor de melding.

`Stratificatie.java` regel 30–32 noemt de aanname waar dit op rust: "kenmerken
are monotone, so a missed positive is recoverable; a consumed negation is
not".

Drools' eigen middel voor afleiden is `logicalInsert`: een afgeleid feit blijft
alleen bestaan zolang de voorwaarde van de regel die het invoegde geldt, en
verdwijnt met alles wat erop steunde zodra die voorwaarde vervalt. Dan hangt de
uitkomst van een gelaagd programma niet meer af van de volgorde op de agenda, en
zijn maatregelen 1–5 niet meer nodig. Deze kaart toetst of dat voor de vormen
van RegelSpraak opgaat, in een derde tak naast de twee bestaande.

## Gemeten in het proefstuk (2026-10-07)

Drools 8.44.0.Final, regels gebouwd met `PatternDSL` en lambda's, KieBase via
`KieBaseBuilder.createKieBaseFromModel(model, EqualityBehaviorOption.EQUALITY)`,
afgeleide feiten via `drools.logicalInsert` in de consequentie.
`org.drools.model.Drools.logicalInsert(Object)` is publieke API;
`drools-tms` komt transitief mee met `drools-engine`.

- **Intrekken met cascade werkt.** Een keten k1 → k2 → Waarde wordt in één
  `fireAllRules` volledig ingetrokken zodra een later afgeleid feit de
  voorwaarde van k1 onwaar maakt, zonder extra vuring.
- **Ontkenningsketen zonder salience werkt.** k1 ⇐ c, k2 ⇐ niet k1,
  k3 ⇐ niet k2: in alle zes volgordes van de regels in het model is de
  eindtoestand juist; de slechtste volgorde kost 4 vuringen in plaats van 2.
- **Uitzondering op uitzondering werkt**, in alle zes volgordes voor elke
  combinatie van de twee voorwaarden.
- **Doorvallen bij een lege default werkt.** d1 hoger dan d2; d1 gerechtvaardigd
  maar leeg, dan beslist d2; wordt d1's waarde later afgeleid, dan verdwijnt
  d2's conclusie. Juist in 44 regelvolgordes × 8 combinaties. De goedkoopste
  vorm: één regel per default en één stuurfeit `Beslist(d)` per default met een
  lagere eronder; de lagere regel heeft `not Beslist(hoger)`.
- **Gelijke logische feiten worden samengevoegd** op `equals`/`hashCode`, met
  één rechtvaardiging per regel; dat gebeurt los van de KieBase-optie.
- **Initialisatie werkt alleen als haar blokkade de eigen uitvoer uitsluit.**
  `not Waarde(x, a, v), v != 0` geeft eerst 0 en daarna alleen 26. De naïeve
  vorm `not Waarde(x, a, _)` blokkeert zichzelf: de regel vuurt één keer, trekt
  zijn eigen feit in en vuurt niet meer, zodat het attribuut zonder berekening
  leeg eindigt in plaats van 0. Zonder melding.
- **Truth maintenance volgt alleen wat de LHS bindt.** Een consequentie die een
  waarde met een getter leest of zelf in het werkgeheugen opzoekt, legt geen
  afhankelijkheid vast: verandert of verdwijnt die waarde, dan blijft het
  afgeleide feit met de oude waarde staan. Met de waarde als patroon in de LHS
  gebonden wordt het feit vervangen, in beide volgordes.
- **Kringen van ontkenningen.** Een even kring (k1 ⇐ niet k2, k2 ⇐ niet k1) is
  stabiel, maar de eerste regel in modelvolgorde wint. Een oneven kring van
  lengte 2 of 3 wisselt tot de limiet van `fireAllRules`; lengte 1 (k ⇐ niet k)
  eindigt stil leeg.
- **Invoer naast een gelijk logisch feit.** In EQUALITY neemt een gesteld feit
  een gelijk logisch feit over. In IDENTITY ontstaan twee feiten op één sleutel,
  en een `delete` op een gesteld feit wordt in sommige toestanden stil genegeerd.
  Ook in EQUALITY verwijdert `delete(handle)` op een gesteld feit het logische
  feit mee terwijl zijn rechtvaardiging nog geldt; `delete(handle,
  FactHandle.State.STATED)` doet het juiste. Binnen één casus wordt invoer niet
  verwijderd, dus dit raakt de proef niet, wel een langlopende sessie.
- **Kosten.** Drie afleidingsstappen per object, N = 10 000: logisch 9,5–12 ms,
  setter met `update` en `NO_LOOP` 5,5–8,7 ms; beide 3N vuringen. Geen
  JMH-meting, alleen de ordegrootte. Drools gaf in de hele suite geen enkele
  waarschuwing.

## Ontwerpeisen voor de proeftak

1. **Elke lezing waarvan een afgeleide waarde afhangt, staat als patroon in de
   LHS.** Een consequentie rekent alleen met gebonden variabelen; geen getter
   op een boon voor een afgeleid lid en geen opzoeking in het werkgeheugen. De
   rekenhulpen van `runtime/RegelSpraakRuntime` blijven bruikbaar als functies
   van gebonden waarden. Dit is het grootste verschil met de exec-modeltak, die
   veel waarden in de consequentie leest.
2. **Afgeleide kenmerken en attributen zijn logische feiten**; invoer blijft
   gesteld op de bonen. Of de feiten generiek zijn (onderwerp, naam, waarde) of
   een gegenereerde klasse per lid, kiest de bouwer; de keuze en de reden
   staan in het `Result:`.
3. **De verhouding tussen invoer en afleiding blijft zoals ze is**:
   RUNTIME-SEMANTICS ch. 7.1 en 7.8, RegelSpraak §3.5, en
   `spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §1: invoer op
   een kenmerk staat tegen elk besluit van een defaults-groep, invoer op een
   attribuut wordt door een beslissende default vervangen.
   `jutterswet-opgave-boven-berekening` zit daarom in de proefset.
4. **Een initialisatie sluit in haar blokkade haar eigen uitvoer uit.**
5. **Uitzonderingen worden met logische stuurfeiten verlaagd**, niet met de
   reduceerregel van de exec-modeltak; de vorm met `Beslist(d)` is het
   uitgangspunt. Dit toetst het bezwaar van
   `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §5.1 onder (A),
   "elaborate control-fact bookkeeping", met een meting in plaats van een
   schatting.
6. **Een kring van ontkenningen wordt vóór het bouwen geweigerd**, met naam.
   Nagaan of de bestaande controles (WF-046, de resolver) dat al doen; zo niet,
   dan weigert de proeftak zelf. Even kringen ook: daar wint anders de
   modelvolgorde.
7. **KieBase in `EqualityBehaviorOption.EQUALITY`.**
8. **Geen van de maatregelen 1–5 in de proeftak.** Het vuurplafond van
   `CaseRunner` blijft.
9. **Na het vuren staan de afgeleide waarden op de bonen**, via één publieke
   functie in `cases/DroolsBackend.java` die `CaseRunner` aanroept. De
   vergelijkers van `CaseRunner` (regel 1029–1158) blijven ongewijzigd, en een
   afnemer kan later dezelfde functie aanroepen.
10. **Wat de proeftak niet draagt, weigert hij luid per vorm** via het
    Dekking-manifest, zoals de andere takken; nooit een stille val.

## Opzet

- Een derde waarde in `DroolsBackend.Branch` naast `EXEC_MODEL` en `DRL_TEXT`,
  een nieuw pakket onder `transpiler/` voor de lowering, en een vlag in
  `cases/DroolsBackendMain.java` naast `--drl`. De bouwer kiest namen die
  zeggen wat de tak doet.
- Hergebruik: `loader/IrLoader`, het genereren van de bonen,
  `runtime/RegelSpraakRuntime`, `transpiler/kern/`.
- Vormen in de proef: `Rule` (gelijkstelling, berekening, kenmerktoekenning,
  initialisatie), `AttributeReference` op het eigen onderwerp en via
  `RoleBinding`, `Binary`, `Comparison`, `Compound`, `CharacteristicCheck` ook
  ontkend, `Predicate` (gevuld/leeg), `Literal`, `Number`, `Date`,
  `ParameterReference`, `Validity`, `Consistency`, `PrioritizedDefaults` met
  `Default` en `ExceptionGroup`.
- Niet in de proef: `Aggregation` en elke andere accumulate, `QuantifierFold`,
  objectcreatie, feitcreatie, verdeling, tijdlijnen, `functionDefs`.

## Proefset

Acht goldens, alle in gate 12 gespeeld door de exec-modeltak, met alleen vormen
uit de lijst hierboven (gecontroleerd op de knooptypen in hun `ir.json`):

| golden | waarom |
|---|---|
| `jutterswet-meldingsketen` | ontkende lezing van een afgeleid kenmerk, navigatie, datum, defaults |
| `anchor/jutterswet-kustwacht` | ontkende lezing na een genavigeerde producent; de DRL-teksttak geeft hier een verkeerde waarde |
| `jutterswet-initialisatievolgorde` | twee initialisaties; de DRL-teksttak faalt hier op volgorde |
| `jutterswet-voorwaardelijke-initialisatie` | initialisatie met voorwaarde |
| `jutterswet-wrakinitialisatie` | initialisatie naast een defaults-groep |
| `anchor/jutterswet-nestdiepte` | uitzondering op uitzondering |
| `anchor/jutterswet-nestuitzondering` | uitzondering op uitzondering |
| `jutterswet-opgave-boven-berekening` | invoer boven berekening, consistentie, ontkende controle |

Of een van deze goldens een keten van twee ontkenningen bevat, is niet
vastgesteld; die vorm krijgt daarom een eigen getuige (acceptatie b).

## Acceptatie

- (a) Voor elke golden uit de proefset, met `JAR` zoals in
  `scripts/check-drools.sh` regel 108:
  `java -jar "$JAR" <vlag> corpus/golden/<golden>/ir.json corpus/golden/<golden>/case*.json`
  eindigt met exit 0, tegen dezelfde vastgelegde verwachtingen als gates 5, 6
  en 12.
- (b) Getuigen in `backends/drools/src/test/java`, elk gedraaid in elke
  volgorde van de betrokken regels in het model: een keten van twee
  ontkenningen; een initialisatie die zichzelf niet blokkeert (de naïeve vorm
  rood, de juiste groen); doorvallen bij een lege default waarbij de hogere
  default later alsnog beslist; een even en een oneven kring die vóór het
  bouwen worden geweigerd; het terugzetten van de waarden op de bonen.
- (c) `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over
  het nieuwe pakket geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd: gate 12 groen met
  `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` gelijk; alle gates van
  `CLAUDE.md` groen.
- (e) Het `Result:` geeft per golden het aantal vuringen en de wandkloktijd in
  de exec-modeltak en in de proeftak, en het aantal regels en stuurfeiten per
  defaults-groep.
- (f) Het `Result:` beantwoordt de beslisvraag voor de eigenaar: doorbouwen tot
  een volledige tak (met vervolgkaarten voor accumulates, creaties, verdeling
  en tijdlijnen, en een kaart voor de toolchain), of stoppen. Met de metingen
  van (e) en elke vorm waar de proeftak op stuitte.

## Raakvlakken

- `backlog/new/20261006-drl-defaults-lowering.md`, route A, gebruikt dezelfde
  stuurfeiten en noemt `insertLogical`. Als deze proef slaagt, wordt route A
  daar goedkoper; als beide Drools-takken dan dezelfde vorm gebruiken, toetst
  gate 12 ze niet meer onafhankelijk van elkaar. Dat weegt de eigenaar bij die
  kaart.
- `backlog/new/20261006-check-wf046-door-recursieve-creatie.md` gaat over
  negatieve kringen; ontwerpeis 6 raakt hetzelfde.
- `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` regel 318–330 noemt
  monotone uitvoering als voorwaarde voor een ontkende lezing. In de proeftak
  geldt die voorwaarde niet; dat hoort in het `Result:` als waarneming, niet als
  wijziging van het voorstel.
- De toolchain bouwt met `DroolsBackend.Branch.EXEC_MODEL`
  (regelspraak-toolchain
  `toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/RegelSpraakEngine.java`
  regel 57) en vuurt en leest zelf (`service/.../Uitvoerder.java` regel 69,
  `CaseSessie`). Deze kaart verandert daar niets.

## Volgorde

Afgesproken met de eigenaar op 2026-10-08. Deze kaart gaat vóór
`20261006-drl-defaults-lowering`. De route die die kaart kiest, hangt af van
de beslisvraag van (f): bij doorbouwen route B2 in de DRL-teksttak, bij stoppen
route A, zodat gate 12 in beide gevallen twee verschillende lowerings van
`prioritizedDefaults` houdt. Begin daar pas aan na de beslissing van de
eigenaar. Na beide kaarten volgt `20261008-website-drools-routes-en-faq`.

## Omvang

Middelgroot. De lowering beslaat de vormen van de proefset en de getuigen;
de rest van de backend wordt hergebruikt. De grootste post is ontwerpeis 1: de
lezingen die de exec-modeltak in consequenties doet, moeten hier als patronen
in de LHS.

May touch: het nieuwe pakket onder
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/`,
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`
(de derde tak en de functie van eis 9),
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackendMain.java`
(de vlag), `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseRunner.java`
(de aanroep van eis 9), nieuwe tests onder `backends/drools/src/test/java/`,
een sectie in `backends/drools/README.md`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak, `scripts/` (de proeftak
komt niet in een gate), `corpus/`, `spec/`, de toolchain, en de vormen onder
"Niet in de proef".

## Result (2026-10-08, ter beoordeling; niet gemerged)

*De kaart blijft in `doing/` tot de eigenaar akkoord geeft. Ze staat op de branch `card/drools-logische-afleiding`,
gerebased op main `87ca3f9a`. De commits:*

| Commit | Inhoud |
|---|---|
| `6093446b` | claim |
| `bf341d51` | stap 1: skelet, kenmerken, ontkenning, kringweigering, terugzetten, vlag |
| `ff534b91` | lensfix stap 1 |
| `2bc58b76` | stap 2: attributen, rekenen, navigatie, §6.5 |
| `854bbb7a` | stap 3: initialisatie |
| `c918f52e` | stap 4: consistentie en foutkanaal |
| `57e45c7f` | parameters via een standfeit, plus de corpusgetuige |
| `08a5b51a` | stap 5: defaults met stuurfeiten |
| `2aaf12a2` | meetharnas |
| `48f884a5` | terugzetgetuige en README |
| `629d4423` | weigerzin |
| `9e9056a7` | eindlensfix: deling in een initialisatie naast een schrijver geweigerd |

*De gates draaiden op `9e9056a7`. De commit van dit Result verandert alleen de kaart.*

### Wat er staat, en waarom zo

- **Namen.** De tak heet `DroolsBackend.Branch.TRUTH_MAINTENANCE`, in het pakket `transpiler/tms` met de transpiler
  `TmsTranspiler` (Drools-pakket `dev.uchar.regelspraak.tms`). De vlag is `--tms` in `DroolsBackendMain`, de ingang
  `CaseRunner.viaTruthMaintenance`.
  - De functie van ontwerpeis 9 is `DroolsBackend.zetAfgeleideWaarden(Build, KieSession)`. Op de andere takken doet ze
    niets.
  - `CaseRunner.vuurTotFixpunt` roept haar aan zodra het fixpunt bereikt is.
- **Generieke logische feitklassen, geen klasse per lid.** Het zijn records:
  - `HeeftKenmerk` en `Waarde` dragen de afleidingen.
  - `KenmerkStand`, `Stand`, `PadStand`, `ParameterStand` en `KanaalStand` zijn de standfeiten.
  - `Beslist` en `Uitkomst` zijn de stuurfeiten, met het merkinterface `Stuurfeit`.

  Ze vergelijken het onderwerp op identiteit en sleutelen het lid op het declarerende type. Een klasse per lid zou een
  extra compileerstap vragen; nu is het lid een constante alfaconstraint.
- **Totale standfeiten.** Per onderwerp en gelezen afgeleid lid staat er precies één standfeit, eventueel met een lege
  waarde. Daardoor is elke lezing een positief gebonden patroon in de LHS (ontwerpeis 1, A9). Een `not` komt alleen
  voor in de generieke standregels en in de blokkade van een initialisatie. Een consequentie rekent alleen met
  gebonden waarden.
- **Invoer blijft op de bonen.** Tijdens het vuren schrijft de tak niets op een boon en roept hij nooit `update` aan. Na
  het fixpunt zet `zetAfgeleideWaarden` de afleidingen op de bonen. Twee verschillende waarden op één lid geven een
  benoemde fout; de functie kiest nooit stil.
- **KieBase in `EQUALITY`** (ontwerpeis 7). Een gesteld feit neemt dan een gelijk logisch feit over (A6). Gelijke
  logische feiten worden ook zonder die optie samengevoegd (A3).
- **Kringen.** De backend draait de verifier niet. De frontend herhaalt WF-045 en WF-051 in `resolver/RuleChecker`,
  maar die klasse zit niet in de backendbuild. Daarom weigert de tak zelf, vóór het bouwen en met naam, elke kring in
  de graaf van `ProducentConsument.vormen` met het foutkanaal als extra knoop (ontwerpeis 6). Dat geldt voor even en
  oneven ontkenningskringen, en ook voor positieve kringen.
- **Initialisatie** (ontwerpeis 4). De blokkade eist dat het doel leeg is op de boon (de invoer) en dat er geen
  `Waarde` van een andere schrijver is (`not Waarde(…, schrijver != zijzelf)`), zoals ch. 5.4 vraagt. Twee
  initialisaties op één doel worden geweigerd (WF-079).
- **Consistentie.** Een schending wordt logisch ingevoegd als `Inconsistentie` en vervalt dus weer als de schending
  verdwijnt. De lezing "regelversie X is inconsistent" loopt via `KanaalStand`.
- **Parameters.** Elke parameter krijgt een `ParameterStand` uit een regel zonder patronen. Eerst las de tak de global
  via een `from`-bron, en Drools deelde die bron tussen regels. Daardoor las een tweede regel stil de parameter van een
  eerste.
- **Defaults** (ontwerpeis 5).
  - De eenvoudige vorm is variant M van A10: per default een regel met `not Beslist(hogere)`.
  - De algemene vorm geldt bij uitzonderingsgroepen of bij meer groepen aan de wortel. Per afhandelende knoop komt er
    een `Uitkomst`, met regels voor doorgeven, verschil, eens en terugval zoals in `DefaultsRun`.
  - De effectieve stapel van ch. 7.10 staat in `Stapels`. Dat is een kopie van de samenstelling in de exec-modeltak,
    omdat die tak niet gewijzigd mocht worden.
- **Volgorde van de stappen aangepast.** Het terugzetten en de vlag kwamen al in stap 1, zodat na elke stap een golden
  via de CLI kon spelen.
- **Wat de tak niet draagt, weigert hij luid per vorm** via het `Dekking`-manifest (ontwerpeis 10). Naast de vormen
  onder "Niet in de proef" weigert hij:
  - een versie met een begrensde geldigheid en een `TimeWindow`, omdat de tak geen rekendatum bindt; `Validity` draagt
    hij dus alleen als "geldig altijd", en geen golden van de proefset vraagt meer;
  - "is gevuurd";
  - een deling in een default, of in een initialisatie met een andere schrijver: die rekent de tak speculatief uit
    (ch. 7.1, 7.7);
  - een regel naast een defaults-groep op hetzelfde doel (WF-050);
  - twee rekenende regels op één doel;
  - een rekenende regel buiten het staande domein;
  - een test met meer dan 13 gebonden waarden, de grens van `DSL.expr`.

**Verbreding van "May touch".**
- `CaseRunner` kreeg meer dan de aanroep van eis 9. Zonder die toevoegingen kon de vlag niet spelen en was (e) niet te
  meten:
  - een parameter `Branch` op de interne replay;
  - de publieke ingang `viaTruthMaintenance`;
  - de package-private getuigennaad `speelCase(…, Branch)`;
  - de package-private meetnaad `voorMeting`/`meet`, met een veld voor de teruggave van `fireAllRules`.

  De bestaande signaturen en de vergelijkers zijn ongewijzigd.
- `Build` kreeg een component, en `Branch` een derde waarde. De toolchain construeert `Build` niet en switcht niet over
  `Branch` (nagelopen in `UcharBouw`, `BeslisserKern`, `ScenarioRunner` en `Playground`).
- In `README.md` noemt de alinea "The entry for consumers" nu de derde tak.

### Acceptatie

**(a)** Gedraaid met de jar die gate 12 op `9e9056a7` bouwde:
`java -jar "$JAR" --tms corpus/golden/<golden>/ir.json corpus/golden/<golden>/case*.json`.

| golden | uitkomst |
|---|---|
| `jutterswet-meldingsketen` | PLAYED 3, exit 0 |
| `anchor/jutterswet-kustwacht` | PLAYED 2, exit 0 |
| `jutterswet-initialisatievolgorde` | PLAYED 4, exit 0 |
| `jutterswet-voorwaardelijke-initialisatie` | PLAYED 4, exit 0 |
| `jutterswet-wrakinitialisatie` | PLAYED 4, exit 0 |
| `anchor/jutterswet-nestdiepte` | PLAYED 3, exit 0 |
| `anchor/jutterswet-nestuitzondering` | PLAYED 9, exit 0 |
| `jutterswet-opgave-boven-berekening` | PLAYED 12, exit 0 |

**(b)** De getuigen staan in `backends/drools/src/test/java/dev/uchar/regelspraak/cases/`.
- Elke getuige draait alle documentvolgordes van de betrokken regels. Waar het kan, draait hij daarnaast een geseede
  steekproef van volgordes van de gegenereerde regellijst.
- Elke getuige is rood gemeten: vóór de code, of met een mutant die niet gecommit is. De rode regel staat letterlijk in
  de javadoc.

| Getuige | Wat hij toont | Rood gemeten |
|---|---|---|
| `TmsOntkenningsketenWitnessTest` | Keten van twee ontkenningen: 6 en 24 documentvolgordes, 102 gegenereerde volgordes. | Een gewone `insert` in de kenmerkregel of in de onwaar-stand maakt elke rij rood. |
| `TmsInitialisatieWitnessTest` | Een initialisatie die zichzelf niet blokkeert. | Met de naïeve blokkade zijn in case 1 van de golden bergloon en vindloon leeg in plaats van 26 en 18. Met meer objecten: "geen fixpunt". |
| `TmsDoorvalWitnessTest` | Doorvallen bij een lege default, waarbij de hogere default later alsnog beslist. 16 invoercombinaties × 24 documentvolgordes, plus 102 gegenereerde volgordes. De overname komt 76 keer voor, de teruggave 164 keer. | Een gewone `insert` voor `Beslist`: "expected: <200> but was: <null>". |
| `TmsKringWitnessTest` | Even en oneven kringen worden vóór het bouwen geweigerd. | Zonder weigering: "Expected …Excluded to be thrown, but nothing was thrown". |
| `TmsTerugzettingWitnessTest` | Terugzetten voor elke schrijver: kenmerk, berekening, initialisatie, en defaults op attribuut en kenmerk. 4 documentvolgordes. Ook de benoemde fout bij twee waarden. | Vijf mutanten. |

Verder zijn er:
- `TmsUitzonderingWitnessTest`
- `TmsKenmerkbesluitWitnessTest`
- `TmsStapelWitnessTest`
- `TmsConsistentieWitnessTest`
- `TmsParameterWitnessTest`
- `TmsAttribuutketenWitnessTest`: A9, de lezing in de consequentie, maakt 24 van 24 documentvolgordes rood.
- `TmsProefsetWitnessTest`

`TmsCorpusWitnessTest` speelt elke golden met cases via de tak.
- Ze eist PLAYED zonder fout of EXCLUDE, en geen FAIL.
- Voor het aantal gespeelde goldens geldt een ondergrens van 66, geen gelijkheid. Een gelijkheid zou parallelle kaarten
  één register laten delen (WAY-OF-WORKING §1).

De suite telt 1174 tests: 0 fout, 0 overgeslagen.

**(c)** De grep `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over het nieuwe pakket geeft niets.
`s -> true` komt er ook niet in voor.

**(d)** De exec-modeltak, de DRL-teksttak en `Stratificatie` zijn ongewijzigd: `git diff main..` over
`transpiler/model/`, `transpiler/drl/` en `Stratificatie.java` is leeg. Alle zestien gates van `CLAUDE.md` zijn groen op `9e9056a7`. Gate 12 speelt 240 goldens (`EXPECTED_PLAYED`) en de
DRL-teksttak 194 (`EXPECTED_DRL_PLAYED`); beide pins zijn ongewijzigd. De JUnit-suite telt 1174 tests. Gate 10 is groen
op de worktree, een schone checkout (zie open punt 3).

**(e)** De tabellen hieronder zijn de uitvoer van `TmsMeting`, gemeten op `48f884a5`. De twee latere commits veranderen
alleen weigerzinnen en de weigering van een vorm die geen golden van de proefset heeft.

Meetopzet:
- Per case is alleen `vuurTotFixpunt` getimed: `fireAllRules` onder het plafond van de replay, plus het terugzetten (op
  de exec-modeltak een no-op).
- Eerst draait alles 20 keer om op te warmen. Daarna wordt per case de mediaan van 31 runs genomen; per golden staat de
  som van die medianen in de tabel.
- De bouwtijd is de mediaan van 5 keer `DroolsBackend.build`.
- Machine: Java 25 (HotSpot, aarch64), 16 processors.

De tijden liggen tussen 17 en 365 µs per golden en zijn dus gevoelig voor ruis. Tussen twee runs schoof de totale
verhouding van 1,31 naar 1,27. Robuust zijn de aantallen vuringen, regels en feiten: die waren in beide runs gelijk.

| golden | cases | vuringen exec | vuringen tms | vuren+terugzetten exec (ms) | tms (ms) | tms/exec | bouw exec (ms) | bouw tms (ms) | regels exec | regels tms | feiten eind exec | feiten eind tms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| jutterswet-meldingsketen | 3 | 28 | 34 | 0.243 | 0.167 | 0.69 | 6.896 | 5.521 | 8 | 18 | 9 | 42 |
| anchor/jutterswet-kustwacht | 2 | 30 | 29 | 0.127 | 0.075 | 0.59 | 2.419 | 2.361 | 6 | 11 | 12 | 37 |
| jutterswet-initialisatievolgorde | 4 | 29 | 19 | 0.068 | 0.073 | 1.07 | 1.265 | 1.492 | 3 | 5 | 6 | 25 |
| jutterswet-voorwaardelijke-initialisatie | 4 | 2 | 2 | 0.017 | 0.020 | 1.17 | 0.656 | 0.505 | 1 | 1 | 6 | 8 |
| jutterswet-wrakinitialisatie | 4 | 8 | 5 | 0.030 | 0.042 | 1.39 | 0.893 | 1.342 | 2 | 4 | 10 | 17 |
| anchor/jutterswet-nestdiepte | 3 | 3 | 11 | 0.021 | 0.071 | 3.36 | 0.771 | 1.385 | 1 | 12 | 4 | 14 |
| anchor/jutterswet-nestuitzondering | 9 | 9 | 27 | 0.055 | 0.150 | 2.71 | 0.712 | 1.056 | 1 | 12 | 11 | 36 |
| jutterswet-opgave-boven-berekening | 12 | 109 | 124 | 0.196 | 0.365 | 1.86 | 1.446 | 2.103 | 9 | 22 | 16 | 103 |
| totaal | 41 | 218 | 251 | 0.757 | 0.962 | 1.27 | | | | | | |

Per defaults-groep (proeftak). "Los" is een `Uitkomst` van een uitzonderingslid onder een default die zelf verdrongen
is. Zo'n feit draagt de uitkomst niet en blijft toch staan.

| golden | groep | vorm | regels | soorten stuurfeiten | stuurfeiten eind: som | max per case | waarvan los: som | max |
|---|---|---|---:|---|---:|---:|---:|---:|
| jutterswet-meldingsketen | A:Jutter.erkende meldingsdatum | eenvoudig | 2 | Beslist | 1 | 1 | 0 | 0 |
| jutterswet-wrakinitialisatie | A:Wrakvondst.bergloon | eenvoudig | 3 | Beslist | 2 | 2 | 0 | 0 |
| anchor/jutterswet-nestdiepte | A:Jutter.heffing | algemeen | 12 | Uitkomst BESLIST, CONFLICT | 8 | 4 | 0 | 0 |
| anchor/jutterswet-nestuitzondering | A:Jutter.heffing | algemeen | 12 | Uitkomst BESLIST, CONFLICT | 19 | 3 | 2 | 2 |
| jutterswet-opgave-boven-berekening | A:Vondst.waarde | eenvoudig | 2 | Beslist | 4 | 1 | 0 | 0 |
| jutterswet-opgave-boven-berekening | A:Vondst.bergloon | eenvoudig | 3 | Beslist | 4 | 1 | 0 | 0 |

Corpusdekking, als achtergrond bij (f). Van de 292 goldens met cases spelen er 66 (222 cases): 0 FAIL, 226 EXCLUDE.
Ter vergelijking: de exec-modeltak speelt er 240, de DRL-teksttak 194.

Eerste weigering per golden, voor alle Drools-takken:
- loader: 37 (ListType 15, StructType 7, SumType 6, functionDefs 4, Match 2, TemporalRead 2, bronPad 1);
- botsing van bonennamen: 3.

Eerste weigering door een grens van de proeftak:

| Vorm | Goldens |
|---|---:|
| Aggregation (waarvan 2 in een default) | 65 |
| Let (waarvan 1 in een default) | 15 |
| afronding | 12 |
| QuantifierFold | 12 |
| verdeling | 12 |
| BuiltinCall | 10 |
| tijdsafhankelijk attribuut | 9 |
| twee rekenende regels op één doel | 9 |
| objectcreatie | 8 |
| uniciteit | 5 |
| procentvormen | 5 |
| RoleMembership | 4 |
| Clamp | 3 |
| duur in dagsoorten | 3 |
| herrekende operandgrens | 3 |
| `wholePeriod` | 3 |
| begrensde geldigheid | 2 |
| dagsoortdefinitie | 2 |
| `TimeWindow` | 1 |
| `power` | 1 |
| contextwaarde | 1 |
| feitcreatie | 1 |

Een golden kan na zijn eerste weigering nog op een volgende vorm stuiten. Hoeveel goldens er spelen als een vorm
erbij komt, is dus niet gemeten.

**(f) Beslisvraag: doorbouwen tot een volledige tak, of stoppen?**

Advies: doorbouwen, als vervanging van de huidige exec-modeltak en niet als derde tak. De eigenaar gaf op 2026-10-08
aan te willen bouwen als het idee haalbaar is, met git als weg terug. Het eindbeeld is twee Drools-takken.

Waarom haalbaar:
- **De maatregelen.** Maatregelen 1–5 zijn in deze tak niet nodig; de grep van (c) is leeg. Voor de vormen die de tak
  draagt, hangt de uitkomst niet af van de volgorde van de regels; de getuigen draaien elke documentvolgorde. Het
  vuurplafond (maatregel 6) blijft.
- **Stuurfeiten.** Het bezwaar "elaborate control-fact bookkeeping" (`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md`
  §5.1 onder (A)) blijkt bij meting bescheiden:
  - een eenvoudige groep: 2–3 regels, en per case hoogstens 2 `Beslist`;
  - nesting: 12 regels, en per case hoogstens 4 `Uitkomst`, waarvan 2 los.
- **Kosten.**
  - Vuren en terugzetten: 1,27× zo lang, onder een milliseconde per case.
  - Bouwtijd: van dezelfde orde.
  - Regels: 1 tot 12 keer zoveel. Feiten aan het eind: 1,3 tot 6,4 keer zoveel.
- **Geen fouten.** De tak speelt 66 goldens zonder één FAIL. Als onafhankelijke lezing vond hij in twee executors een
  bestaande fout (waarneming 1).
- **Wat nog ontbreekt, is grotendeels bekend Drools-gebied:**
  - een `accumulate` in de LHS wordt opnieuw uitgerekend als zijn feiten veranderen;
  - de expressievormen (`Let`, afronding, builtins, clamp, procenten) zijn functies over gebonden waarden, met de
    runtimehulpen die er al zijn;
  - een verdeling is een berekening over een gebonden verzameling;
  - een tijdlijn is een waarde in een standfeit.
- **Niet beproefd.** `accumulate` zelf, en vooral objectcreatie. Een logisch ingevoegd object vervalt met zijn
  rechtvaardiging, en recursieve creatie is precies de positieve kring die `Kringen` nu weigert. Daarnaast is nodig dat
  een fout een logisch feit wordt dat pas aan het fixpunt telt, want truth maintenance kan een geworpen fout niet
  intrekken (waarneming 2). Dat zijn de grootste risico's.

De eindlens adviseerde te stoppen. Dat advies ging uit van een derde executor naast de twee die gate 12 al vergelijkt.
Het doel is vervanging, dus dat argument vervalt. Zijn punt over `accumulate` en objectcreatie blijft: daarom komen die
vooraan.

Eindbeeld en volgorde:
- **Eindbeeld.** De TMS-verlaging is zelf een verlaging in het executable model. Ze groeit tot de dekking van de
  huidige exec-modeltak (gate 12: 240) en vervangt die dan, ook in de toolchain. Tot die omschakeling staat ze achter
  `--tms`, naast de oude.
- **De DRL-teksttak** blijft volgens `20261006-drl-defaults-lowering` destructief, met route B2 voor de defaults. Zo
  houdt gate 12 twee verschillend gebouwde verlagingen. Beide takken op truth maintenance kan ook, maar dan vergelijkt
  gate 12 twee verlagingen met hetzelfde ontwerp. Die keuze hoort bij die kaart.
- **Vervolgkaarten.** Het zijn bouwkaarten, geen aparte haalbaarheidsproeven; dat is de wens van de eigenaar. De
  riskantste vormen komen eerst, zodat een vorm die niet te dragen blijkt vroeg zichtbaar wordt:
  1. `accumulate` (aggregaties) en objectcreatie, met fouten als logisch feit;
  2. de expressie- en voorwaardevormen (`Let`, afronding, builtins, clamp, procenten, kwantorvouwen, rolcheck,
     uniciteit) en de rekendatum (geldigheid, vensters);
  3. verdeling en tijdlijnen;
  4. de omschakeling: de TMS-tak wordt de exec-modeltak van gate 12 en van de toolchain, en de destructieve verlaging
     en `Stratificatie` gaan eruit.
- **Review.** Zoals in deze kaart: een lens na elke bouwstap en een eindlens over de hele kaart. Daarnaast vangt de
  corpusgetuige doorlopend (nooit FAIL, over het hele corpus); in deze kaart vond de corpusdoorloop meer dan de lenzen
  per stap.
- **Vloer.** De vloer van de corpusgetuige blijft staan.

Bij stoppen zou gelden:
- route A voor `20261006-drl-defaults-lowering`;
- de vloer van de corpusgetuige vervangen door alleen "nooit FAIL", of alleen dit Result landen en de branch als
  archief bewaren.

### Waarnemingen

1. **Bestaande bug in de exec-modeltak en clingo**, gevonden in stap 3 en op main nagemeten.
   - Bij een initialisatie waarvan het doel in de invoer gevuld is, deponeren beide toch een deling door leeg; ref,
     de DRL-teksttak en de proeftak doen dat niet.
   - Clingo noemt daarbij de producent niet (`?`).
   - Op verzoek van de eigenaar staat er een kaart voor:
     `backlog/new/20261008-deling-door-leeg-onder-initialisatie.md` (`a0661940`).
   - De getuige in `TmsInitialisatieWitnessTest` vraagt van de exec-modeltak daarom alleen case 1.
2. **Bestaande confluentiebug in ref en beide Drools-takken**, gevonden door de eindlens en op main nagemeten. Het
   gaat om twee documenten:
   - document A: een initialisatie die deelt door een nul, naast een rekenende regel die beslist;
   - document B: een initialisatie op 0, naast een rekenende regel, plus een lezer die door het doel deelt.

   Ref, de exec-modeltak en de DRL-teksttak geven op beide in de ene volgorde van de regels de waarde en in de andere
   een fout. Clingo is volgorde-onafhankelijk: bij A faalt het in beide volgordes, bij B slaagt het in beide. Ch. 5.2
   belooft confluentie. Er staat een kaart voor: `backlog/new/20261008-fout-op-tussenstand.md` (`87ca3f9a`); die vraagt
   eerst een normbesluit. De proeftak weigert vorm A sinds `9e9056a7`. Bij vorm B hangt de uitkomst in de proeftak net
   zo van de volgorde af als in ref.
3. **§6.5 en tussenstanden.** Deze tak trekt een deponering die bij een tussenstand ontstond weer in. Ref houdt zo'n
   deponering vast (ch. 6.1, "recorded residuals"). Geen golden raakt dit verschil.
4. **Monotone uitvoering.** `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` regel 318–330 noemt monotone
   uitvoering als voorwaarde voor een ontkende lezing. In deze tak geldt die voorwaarde niet. Dit is een waarneming,
   geen wijziging van het voorstel.
5. **Kringen.** Over 322 documenten (golden, rood en toka) vond de kringcontrole 10 kringen, alle in documenten met
   objectcreatie, die al eerder geweigerd worden. Met de weigering uitgezet gemeten:
   - een even kring beslist op de volgorde van de regels in het model;
   - oneven kringen van lengte 1 en 2 wisselen tot het vuurplafond;
   - een positieve kring houdt zichzelf in stand nadat de invoer verviel.

   Een volledige tak moet recursieve creatie vrijlaten (WF-045) en per ch. 5.1 sleutelen.
6. **Te brede weigering.** Schrijvers en kringen worden gesleuteld op het declarerende type. Daardoor weigert de tak
   ook paren op zustersubtypes die WF-079 en WF-050 niet verbieden. De weigering is luid. In het corpus raakt ze onder
   meer `jutterswet-diepwrakbasis` en `jutterswet-opgave-vormen`.
7. **De parameterfout** kwam niet uit een lens. Ik vond haar met een doorloop van het hele corpus met de jar; sindsdien
   staat de corpusgetuige in de suite.

### Lensvondsten

Copilot draaide na elke bouwstap een lens op sol6 met effort xhigh. Vóór dit Result volgde één eindlens op Opus 5.5
(15 premium requests) over de hele diff; de werkopdracht vraagt Opus 5.5 voor die eindlens. De sol6-lens van stap 2
miste de parameterfout. Die vond ik met de corpusdoorloop, niet met een zwaardere lens; een eerdere Opus-ronde was
daarom niet nodig.

Aanvaard en gefixt:
- **Stap 1:** de reden voor EQUALITY in twee javadocs was onjuist. Daarbij kwam een getuige voor twee
  rechtvaardigingen (`ff534b91`).
- **Eindlens:** een deling in een initialisatie naast een andere schrijver werd niet geweigerd (`9e9056a7`, document A
  van waarneming 2).

Afgewezen, met reden:

- **Stap 1, "onterechte kringweigering bij zustersubtypes".** Over 322 documenten weigert de kringcontrole geen
  document dat de tak draagt; de tien kringen liggen in documenten met objectcreatie. De overweigering is luid
  (waarneming 6).
- **Stap 2, "geen getuige voor `PadStand` over twee rollen".** Dit is een testhiaat zonder aangetoonde fout. Geen
  golden of regelset leest een afgeleid lid achter twee rollen.
- **Stap 3, "ontbrekende scalaire parameter wordt een waarde".** Dat geldt voor de hele backend
  (`CaseRunner.zetGlobals`). Een golden-case zonder gelezen parameter haalt gate 5 al niet.
- **Stap 3, "verdringingsgetuige niet in elke modelvolgorde".** Elke documentvolgorde draait volledig. De steekproef
  over de gegenereerde regels is een aanvulling, en de lens noemt geen faalscenario.
- **Stap 3, "WF-079-zin bij zustersubtypes".** Geen enkele golden krijgt die weigering, en ze is luid.
- **Stap 4, "kanaalnaam byte-exact vergeleken".** Geen document in `corpus/golden`, `corpus/rood` of `toka/` leest
  "regelversie X is inconsistent". De exec-modeltak vergelijkt even exact, en de weigering is luid.
- **Stap 4, "parameternamen die onder `Namen.ident` samenvallen".** Dat geldt voor de hele backend, en het document is
  geconstrueerd.
- **Stap 5, "stuurfeiten van verdrongen uitzonderingsleden blijven staan".** De einduitkomst klopt; het is een
  kostenpost, en de meting toont die apart.
- **Stap 6, "README verwijst naar een Result dat er nog niet is".** Opgelost door dit Result.
- **Eindlens, "de proeftak zit via de suite feitelijk in gate 12".** Er is geen faalscenario op invoer. De keuze staat
  bij (f): bij doorbouwen blijft de vloer, bij stoppen gaat hij eruit.

### Open punten

1. Twee kaarten op main voor bestaande fouten buiten deze kaart:
   - `20261008-deling-door-leeg-onder-initialisatie`: exec-modeltak en clingo;
   - `20261008-fout-op-tussenstand`: eerst een normbesluit, daarna ref, clingo en beide Drools-takken.
2. Voor de vervolgkaarten:
   - `Stapels` delen met de exec-modeltak in plaats van een kopie te houden;
   - `Kringen` recursieve creatie laten vrijlaten en per ch. 5.1 laten sleutelen;
   - schrijvers per concreet type sleutelen;
   - de rekendatum binden, voor geldigheid en vensters;
   - fouten als logisch feit voeren;
   - een feit invoeren voor "schrijft leeg";
   - het terugzetten afstemmen op een service met incrementele invoer.

   De vervolgkaarten van (f) schrijft de hoofdsessie na het akkoord van de eigenaar.
3. In de hoofdcheckout is gate 10 rood. Oorzaak: 464 verouderde klassen onder het genegeerde
   `frontends/regelspraak/out/io/github/…`, van vóór de pakkethernoeming. Op een schone checkout is gate 10 groen; deze
   worktree ook. Advies: `make -C frontends/regelspraak clean`.
4. Registertekst voor `spec/CONSTRUCTS.md` of `spec/PLAN.md` stel ik niet voor: deze kaart verandert geen spec. Het
   besluit over (f) hoort in PLAN §9, bij de landing.
