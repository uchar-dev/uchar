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
