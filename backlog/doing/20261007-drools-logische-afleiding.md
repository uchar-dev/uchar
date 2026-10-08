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
