# navigated-read-on-beans: een afnemer leest een lezing over een rolpad zoals de exec-model-tak haar leest

*Vastgelegd op 2026-10-20 vanuit de distributie (regelspraak-toolchain, kaart `explanation-role-path`). Regelnummers
op main `4f3d2a8e`.*

Aanleiding: de uitleg van de distributie (haar `Verbaliser`) verantwoordt een kenmerk met de waarden van de operanden
van zijn voorwaarde, gelezen van de beans die `DroolsBackend.setDerivedValues` terugschreef. Een operand die over een
rol leest, "de herlevingsgrens van het recht van het Inkomen", kan zij daar niet bereiken, want uchar geeft de
wandeling van zo'n lezing en de lezing over de referentievelden niet publiek. Gemeten in de distributie op een
regelset van de werkbank (herleving-ww, scenario s01): vijf van de twintig uitlegzinnen noemen zo'n operand
"(onbekend)" terwijl de motor hem afleidde, en de zin spreekt zichzelf tegen ("heeft de herlevingsgrens
overschreden, omdat … de herlevingsgrens van het recht van het Inkomen in-een-kalendermaand (onbekend) gevuld is").
Op een model dat uchars IR-lader las, het model van een build, is het erger: de lezing draagt haar wandeling niet
(`IrLoader`, r. 1481–1493; de wandeling staat in de tabel `navigationBindings` naast het model). De distributie leest
haar daar als het eigen lid van het onderwerp, en heeft het onderwerp een lid met die naam, dan toont zij de waarde
van het verkeerde object.

Twee dingen zijn niet publiek:
1. De wandeling van een lezing van het model van een build. `TmsTranspiler` houdt de tabel van de lader (r. 235)
   privé.
2. Het object dat een wandeling vanaf een bean bereikt. De exec-model-tak leidt de velden af met
   `FactModelGenerator.navigationWalk` en `hasReferenceField` (`TmsTranspiler.path`, r. 6187–6216) en leest ze met
   `Accessors.eigenschapGetter` (`PatternDslRendering.along`, r. 845–857). Een afnemer die dat naschrijft, heeft een
   tweede afleiding van de navigatie.

Goal: twee publieke ingangen in de Drools-backend, zodat een afnemer van teruggeschreven beans een lezing over een
rolpad leest zoals de tak haar leest:
- `TmsTranspiler.navigationBindings(Expr.AttributeReference read)`: de wandeling die de lader naast de lezing las
  (IR-SPEC ch. 4.1a), één binding per hop vanaf het onderwerp, leeg voor het eigen lid van het onderwerp.
- `DroolsBackend.reached(RegelSpraakModel model, Object subject, ObjectType subjectType, Expr.AttributeReference read,
  List<RoleBindingRef> walk)`: het object waarop `read` zijn lid leest, vanaf de bean `subject` van het type
  `subjectType`. Bij een lege wandeling is dat `subject`; anders het object dat de wandeling hop voor hop over het
  referentieveld van de bereikte rol bereikt, met dezelfde afleiding als `TmsTranspiler.path`; null waar een link op
  de wandeling ontbreekt (RUNTIME-SEMANTICS ch. 5.8, regel 1). Een wandeling die de tak niet leest, weigert zij vóór
  ze iets leest, met de zin van de tak: `CaseRunner.Excluded` met DROOLS-280, -281 of -282. Voor een lezing van het
  model van een build komt de wandeling uit de eerste ingang; voor een lezing van een model dat de frontend
  resolvede, uit `IrEmitter.navigationBindings`, de wandeling die de emitter op de draad schrijft.

Acceptance:
- (a) Nieuwe getuige `backends/drools/src/test/java/dev/uchar/regelspraak/cases/NavigatedReadOnBeansWitnessTest.java`
  op golden `jutterswet-havenketen`, alleen via publieke ingangen: case 1 en 2 gespeeld met `CaseInput.of`, `insert`,
  `fire` en `setDerivedValues`, dan per genavigeerde lezing van het model van de build de wandeling en het bereikte
  object. Gepind: twee hoppen naar een Haven (een afgeleid en een ingevoerd lid), één hop naar een Jutter, null waar
  de tweede hop niets bereikt (V4 van case 1), en in case 2 een link die de feitcreatie `werving` legde en die de
  terugschrijving op het veld zette. Op het bereikte object staat de waarde die de motor las: `havenopening` van V1
  is de `afgeleide openingsdag` van de bereikte Haven. Een lezing van het eigen lid heeft een lege wandeling en
  bereikt het onderwerp zelf.
- (b) Weigeringen, elk met de zin van de tak en zonder dat een bean gelezen wordt: een wandeling die niet resolvet
  vanaf het onderwerp (DROOLS-280), één over een rol naar veel (DROOLS-281), en een hop zonder referentieveld
  (DROOLS-282: het verdrongen veld van `synth-navigatiepremie`, zoals `StorageDirectionWitnessTest` het maakt).
- (c) Eerst rood: elke rij valt tegen een tijdelijke mutatie van de ingang (gemeten, niet bewaard).
- (d) `backends/drools/README.md`, "A session in four calls", stap 4, noemt beide ingangen.
- (e) Geen bestaand testbestand gewijzigd. Alle 16 gates van `CLAUDE.md` groen op de eindstand van de tak.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java` (alleen de nieuwe accessor),
de nieuwe testklasse, `backends/drools/README.md`, deze kaart.

Out of scope:
- Een andere navigatie of opslagrichting; `TmsTranspiler.path` en de lezing in de tak blijven zoals ze zijn.
- De wandeling van een kenmerkcheck over een rol (`Condition.CharacteristicCheck`, `IrLoader` r. 2082–2088): de
  distributie vraagt haar in deze ronde niet.
- `spec/`, het schema, het corpus, de gate-scripts.

## Result (2026-10-20)

`b8a59b1a` maakt de wandeling van een lezing en het object dat zij bereikt publiek. Geen wijziging van spec, schema,
corpus of gate-scripts; geen bestaand testbestand gewijzigd.

- **Wandeling** (`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java` r. 387):
  `public List<RoleBindingRef> navigationBindings(Expr.AttributeReference read)`, de tabel van de lader op de knoop;
  leeg voor een eigen lid en voor een knoop die deze lader niet maakte.
- **Bereikt object** (`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java` r. 689):
  `public static Object reached(RegelSpraakModel model, Object subject, ObjectType subjectType,
  Expr.AttributeReference read, List<RoleBindingRef> walk)`. Een lege wandeling geeft `subject`. Anders
  `FactModelGenerator.navigationWalk(model, subjectType, walk, read.objectType())` (null: DROOLS-280; `toMany`:
  DROOLS-281) en per hop `FactModelGenerator.hasReferenceField` (nee: DROOLS-282), alle hoppen vóór de eerste lezing;
  dan per hop `Accessors.leesEigenschap(object, Namen.property(role))`, null zodra een link ontbreekt. Een weigering
  is een `CaseRunner.Excluded` met `PART.desc.read` en de brontekst van de lezing (zonder brontekst de naam van het
  attribuut); geen nieuwe catalogustekst.
- **Tekst**: `backends/drools/README.md`, "A session in four calls", stap 4; de Javadoc van beide ingangen.

### Acceptatie

- (a) Gehaald: `NavigatedReadOnBeansWitnessTest`, 6 tests. Op `jutterswet-havenketen` case 1: V1 en V2 bereiken H1,
  V3 H2, V4 niets (J3 heeft geen haven), één hop van V4 bereikt J3, een eigen lezing blijft op V1; `havenopening` van
  V1 is de `afgeleide openingsdag` van de bereikte H1 (2026-01-01). Case 2: V1 bereikt H1 over de link die `werving`
  legde, V2 niets. De wandeling van de lezing over twee hoppen staat twee keer in het model (voorwaarde van
  `havendatum`, waarde van `havenopening`) en is beide keren dezelfde; de lezing zelf draagt geen navigatie.
- (b) Gehaald: DROOLS-280 vanaf Haven en voor een wandeling die bij een Haven uitkomt waar de lezing een lid van een
  Jutter leest; DROOLS-281 over `havenjutter`; DROOLS-282 op het verdrongen veld `werf` van Schuit. Zinnen en codes
  gepind.
- (c) Gehaald, gemeten met tijdelijke mutaties (niet bewaard): M1 `reached` negeert de wandeling: 5 van 6 rood; M2
  geen toets op een rol naar veel: de rij van DROOLS-281; M3 geen toets op een wandeling die niet resolvet: de rij van
  DROOLS-280; M4 geen toets op het referentieveld: de rij van DROOLS-282; M5 de accessor geeft altijd leeg: 4 rood; M6
  geen eindpunt voor de wandeling: de rij van DROOLS-280. Elke mutatie laat een test vallen, elke test valt onder een
  mutatie.
- (d) Gehaald.
- (e) Alle 16 gates groen op `b8a59b1a` (boom `824714cb`), 1277 s; de commit daarna raakt alleen `backlog/`: 1 PASS;
  2 348 bestanden valideren en round-trippen; 3 348 golden schoon, 511 negatief afgewezen; 4 875 case-bestanden; 5
  ref-suite 1304, 831 cases; 6 clingo-suite 2198 checks, 831 cases; 7 786 cases, 45 survivors; 8 44 rode cases; 9 241
  checks, 12 catala-goldens; 10 JUnit 1210, 125 rgs-goldens bytegelijk; 11 29 TOKA-cases op ref, clingo en Drools; 12
  260 gespeeld op beide takken, JUnit 1630 (de 6 nieuwe erbij), 731 s; 13 43 twists over 24 goldens, 122 replays; 14
  150 checks, 2 goldens; 15 271 van 309 goldens vergeleken, 125 op de bronroute, 70 unittests; 16 30
  scenario-goldens, 102 case-bestanden bytegelijk.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: geen construct, wire, schema of
case-formaat verandert.

Open:
- `reached` leidt de hoppen af met dezelfde twee functies als `TmsTranspiler.path`, maar deelt geen code met die
  methode. Eén vorm leest de tak anders: een hop over een relatie die een creatie legt, op een kant zonder
  referentieveld. `path` leest die over `RelationLink`-feiten; `reached` weigert haar met DROOLS-282, omdat de
  terugschrijving zo'n link alleen op een veld zet dat het beanmodel geeft. Of een golden die vorm heeft, is niet
  gemeten: geen gate roept `reached` aan. Een kaart die `path` en `reached` één afleiding geeft, sluit het.
- De wandeling van een kenmerkcheck over een rol (`Condition.CharacteristicCheck`) blijft privé, zoals de kaart zei.
  De distributie leest zo'n check op het model van de lader als een kenmerk van het onderwerp; zie haar kaart
  `explanation-role-path`.
- De distributie (regelspraak-toolchain, kaart `explanation-role-path`) pint deze tak en gebruikt beide ingangen; zij
  landt na deze kaart.
