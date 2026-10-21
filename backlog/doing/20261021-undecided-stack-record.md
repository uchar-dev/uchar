# undecided-stack-record: decisionsOf geeft ook een record waar niemand besliste

*Vastgelegd door de hoofdsessie op 2026-10-21, kaart U1 uit het Fable-advies over de uitleg
(`/Users/yeb/Developer/uchar-dev/werk/defaults-decider/fable-advies.md` §5 en §6; besluit van de eigenaar 2026-10-21:
U1 en de toolchain-fix T1 in deze ronde, vóór de landing van de toolchain-kaart `explanation-defaults`). Regelnummers
op main `b7186f78`.*

## Aanleiding

`DroolsBackend.decisionsOf` (kaart `defaults-decider`, `backlog/done/20261021-defaults-decider.md`) geeft per object en
effectieve stapel een `DefaultsDecision` met een beslisser of een conflict, en géén record waar niemand besliste. Dan
weet een afnemer niet welke varianten de stapel vormen: bij een stapel die overerving uit twee declaraties samenstelt
(RUNTIME-SEMANTICS ch. 7.10) staat de evaluatievolgorde alleen in `rootChains`, en die komt zonder record niet mee. De
uitleg van de toolchain zegt daar nu "welke varianten de stapel van een RechtOpUitkeringWwBijOntslag samenstellen, geeft
de motor zonder beslissing niet" (vier keer in de vijf service-smoke-verzoeken van de regelset herleving-WW v2, steeds de
herlevingstermijn). Zelf afleiden dat een eigen basis de geërfde vervangt, zou semantiek van uchar in de distributie
zijn.

## Doel

`decisionsOf` geeft voor elk object van de sessie en elke effectieve stapel van zijn type een record: met een beslisser,
met een conflict, of met geen van beide (niemand besliste), en altijd met `rootChains`. De lowering verandert niet; de
ingang leest dezelfde controlefeiten en rekent niets na.

## Acceptatie

- (a) Rood eerst in `DefaultsDecisionWitnessTest`: een stapel uit twee declaraties zonder besluit (zoals `W3` van de
  toolchain-kaart: een Wrakrecht waarop geen variant beslist) en een eenvoudige stapel zonder besluit, op beide
  takken, met handgeschreven `rootChains`; op main faalt de test omdat er geen record is.
- (b) `DefaultsDecision`: de eis "precies één van `decider` en `conflict`" wordt "hoogstens één"; de javadoc van het record, van
  `TmsTranspiler.decisionsOf` en van `DroolsBackend.decisionsOf` zegt dat een record zonder beide "niemand besliste" is
  en dat elk (object, stapel) een record heeft. Welke objecten: de beans van de invoer en de objecten die een creatie
  maakte (`Fixpoint.objects`), elk met de stapels van zijn concrete type.
- (c) `DefaultsDecisionCorpusWitnessTest`: "geen beslisser en geen conflict ↔ het doel houdt zijn invoer", elk getoetst
  (case, stapel, object) heeft precies één record, en de pointertoets geldt ook voor de `rootChains` van die records; de
  51 "zonder record" van 2026-10-21 worden zulke records. Vloeren opnieuw gemeten, met datum.
- (d) De semantiek onveranderd: gates 5, 6, 12 en 13 dezelfde uitkomsten, `EXPECTED_PLAYED` en de lijsten van gate 12
  ongewijzigd. Alle zestien gates groen (`werk/gates.sh`).
- (e) Result: wat er veranderde, de rode getuige, de gate-samenvatting, de vloeren.

May touch: `backends/drools/**` behalve de productieruntime (`backends/drools/src/main/java/dev/uchar/regelspraak/runtime/`),
deze kaart.

Out of scope: de lezingen van voorwaarden (kaart U2 `fixpoint-readings`), de uitleg zelf (toolchain).

## Result (2026-10-21)

Tak `card/undecided-stack-record`, vanaf `b7186f78`, ongemerged en ongepusht. Geen beschermd pad geraakt (alleen
`backends/drools/**` buiten de runtime en deze kaart); de hoofdsessie zet de kaart bij de landing in `done/`.

- `c21d4961` de ingang en de getuige (acceptatie (a), (b)).
- `977122f5` de corpustoets (acceptatie (c)).
- dit Result (acceptatie (e)).

### Wat er veranderde

`TmsTranspiler.decisionsOf` loopt nu over de objecten in plaats van over de controlefeiten: per object in de sessie
waarvan de klasse de beanklasse van de eigenaar van een stapel is, per stapel van die klasse één record. Een stapel
loopt op instanties van precies zijn eigenaar (ch. 7.10, `concreteType`), dus de stapels van een object zijn die van
zijn klasse; `StackLayout` draagt daarvoor de beanklasse van de eigenaar (`StackBuild.javaClass`). De sessie houdt
als objecten van een type alleen de beans van de invoer (`DroolsBackend.insert`) en de objecten die een creatie maakte
(`Fixpoint.objects`). Staat er op het object geen controlefeit van de stapel, of rondt geen ervan een keten aan de
wortel af, dan krijgt het record geen beslisser en geen conflict. Een controlefeit op een subject dat zo geen record
kreeg, faalt luid ("controlefeiten van … op …, en dat is in de sessie geen object van het type waarop de stapel
loopt"). De wortelketens worden per stapel één keer berekend, en alleen voor een stapel waarvan de sessie een object
heeft.

De lowering, `Fixpoint`, `fixpointOf`, `setDerivedValues` en `reached` zijn ongewijzigd; de lezing van de
controlefeiten (`settledInSimpleForm`, `settledInGeneralForm`, `reconciled`, `descended`) ook.

`DefaultsDecision`: de eis werd "hoogstens één van `decider` en `conflict`" (beide gezet: `IllegalArgumentException`
"een beslissing van een stapel noemt hoogstens één van beslisser en conflict, niet beide"). De nieuwe zinnen van de
javadoc van het record:

> Every object of the session, the beans of the input and the objects the creations made ({@link Fixpoint#objects}),
> has one record per effective stack of its concrete type. A record with neither a decider nor a conflict is one on
> which no default decided: its target keeps its input (RUNTIME-SEMANTICS ch. 7.1), and its {@link #rootChains} still
> name the defaults of the stack in their order.

De javadoc van `TmsTranspiler.decisionsOf` en `DroolsBackend.decisionsOf` zegt hetzelfde; die van `DroolsBackend` noemt
bij de overeenstemming met `fixpointOf` ook "a record without either with nothing derived on the target".

### Records zonder besluit (uit `DefaultsDecisionWitnessTest`, P = `/rules/prioritizedDefaults/`)

- Eenvoudig: `J5 toeslag: no default decided` met `rootChains` `[[P2/defaults/0, P2/defaults/1]]` (de
  initialisatie beslist niet, want toeslag draagt 9 in de invoer); `J5 vergoeding` met
  `[[P1/defaults/0, P1/defaults/1, P1/defaults/2]]`; `J5 vrijgesteld` met de vier defaults van P0.
- Algemene vorm: `P7 penninggerechtigd` met `[[P0/defaults/0, P0/defaults/1, P0/defaults/2]]`; `P6 wachtgeld` en
  `P7 wachtgeld` met de drie ongeordende groepen `[[P1/defaults/0], [P2/defaults/0], [P3/defaults/0]]`.
- Samengesteld (ch. 7.10, de `W3` van de toolchain-kaart): `W4 uitkering: no default decided` op een Wrakrecht, met
  `rootChains` `[[P1/defaults/0, P0/defaults/0, P1/defaults/1]]`: de eigen uitzondering, de geërfde uitzondering, de
  eigen basis; de geërfde basis `P0/defaults/1` staat niet in de stapel.

De getuige toetst nu ook per record dat zijn `rootChains` die van zijn stapel zijn (de handgeschreven kaart per type
en lid); tot nu werd per stapel alleen de laatst geziene vergeleken.

### De rode getuige (a)

`DefaultsDecisionWitnessTest` met alleen de test veranderd, op `0a40a70f` (= main `b7186f78` plus de kaart),
`mvn -o -q test -Dtest=DefaultsDecisionWitnessTest`:

```
[ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0 -- in dev.uchar.regelspraak.cases.DefaultsDecisionWitnessTest
```

Elk van de drie op de ontbrekende records, bijvoorbeeld `a_stack_composed_from_two_declarations`: verwacht
`[…, W3 uitkering: P1/defaults/1 80, W4 uitkering: no default decided]`, gekregen
`[…, W3 uitkering: P1/defaults/1 80]`; `the_simple_form` mist de drie van J5, `the_general_form` die van P6 en P7.
Groen sinds `c21d4961`, op beide takken.

### (c) over het corpus

`DefaultsDecisionCorpusWitnessTest` eist nu per getoetst (case, stapel, object), de aangemaakte objecten inbegrepen,
precies één record ("geen record, waar elk object per stapel van zijn type er één heeft" is een verschil), en bij een
record zonder beslisser en conflict dat het fixpunt het doel niet afleidt. De pointertoets liep al over elk record en
dus nu ook over de wortelketens van de records zonder besluit.

Gemeten 2026-10-21, per tak: 324 (case, stapel, object) in 42 goldens — 48 toekennen, 19 niet toekennen, 194
attribuutbesluiten, 12 conflicten, 51 waarop niemand besliste (de 51 "zonder record" van `defaults-decider`); 4 van de
324 op een aangemaakt object; 1017 opgeloste pointers (996 naar een `Default`, 21 naar een regelversie). Vloeren:
`EXPECTED_MIN_CHECKED=324` en `EXPECTED_MIN_GOLDENS=42` opnieuw gemeten en gelijk; nieuw `EXPECTED_MIN_UNDECIDED=51`
en `EXPECTED_MIN_POINTERS=1017`; daarnaast moet er minstens één (stapel, object) op een aangemaakt object getoetst
zijn, naast de bestaande eis van minstens één regelpointer.

Rood op de code van main (de drie bronbestanden zoals op `0a40a70f`, de nieuwe corpustoets erop): precies 51
verschillen per tak, alle "geen record", onder meer `anchor/jutterswet-hardheid/case3-stilte.json M
penninggerechtigd` en `anchor/jutterswet-nestkenmerk/case4-nz-zonder-nx.json M vrijgesteld`.

### Gates

De volledige set op `977122f5`, `bash werk/gates.sh … u1-gates-1` (`werk/defaults-decider/u1-gates-1.summary.txt`):
alle zestien `rc=0`, `EXIT 0` (13:08–13:29; gate 12 654 s, gate 13 199 s). De semantiek is onveranderd: de
PASS-regels van gates 5 (835 cases), 6 (835 cases), 12 (played 261, de DRL-teksttak 261, de kanaalvloer van 49
niet-gespeelde goldens bij naam) en 13 (met de naamleg) zijn letterlijk die van `gates-2` van `defaults-decider` op
`072e9def`; `EXPECTED_PLAYED` en de lijsten van gate 12 zijn niet aangeraakt.

Tijdens de bouw: de Drools-suite (`mvn -o -q package`, 1636 tests, 0 fouten, 0 overgeslagen) en beide getuigen los.

### Voor de pin-bump van de toolchain

- De Verbalisator van de toolchain-kaart `explanation-defaults` (`Verbaliser.java`, de lus na `perDecided`, zoals
  hij nu in de worktree van die kaart staat; gelezen, niet aangeraakt) neemt aan dat een record een beslisser of een conflict heeft: `d == null` → `undecided`, `d.conflict() != null` →
  `conflicted`, anders `decided(d)`, dat `d.decider().pointer()` leest. Na de pin-bump komt daar een record zonder
  beide, en `decided` gooit dan een `NullPointerException`. Een record zonder beide is `undecided`, nu met de
  `rootChains` van de stapel; een ontbrekend record komt niet meer voor op een object van de sessie.
- De lijst blijft zonder vaste volgorde.

### Niet gedaan, met reden

- Geen getuige met een objectcreatie in `DefaultsDecisionWitnessTest`: `TmsDocuments` heeft geen helper voor een
  creatie, en de corpustoets dekt het met 4 (case, stapel, object) op aangemaakte objecten en de eis dat dat er
  minstens één is.
- Geen tekst voor `spec/CONSTRUCTS.md` of `CLAUDE.md`: rij 43 noemt `decisionsOf` alleen als de identiteit van een
  beslissende default (de pointer), en geen gate verandert.
