# drl-rule-part: de herkomst van een regel noemt ook zijn rol, en een stapel haar type en samenstelling

*Voorstel van de werkbank (2026-10-24), met akkoord van de eigenaar door de werkbank-sessie in deze backlog gezet (niet
gecommit). Vervolg op `drl-rule-origin` (done). Klein; landt vóór de toolchain-kaart `drl-rule-part-fields`
(toolchain `backlog/new/20261024-drl-rule-part-fields.md`), die erna de pin bumpt.*

## Aanleiding

De werkbank toont een defaults-stapel aan een jurist als **beslisvolgorde**: de varianten van een regel, de hoogste
eerst (ch. 7.1). Op de kaart "Verlaagde DRL" van een variant staan de DRL-regels die haar default verlaagt, en in de
trace en de dekking van een scenario de stapels zelf. Twee dingen kan de werkbank daar niet zeggen zonder de naam van
een regel te ontleden, en dat doet zij niet:

1. **Welke rol een regel in de verlaging heeft.** `originOf` zegt sinds `drl-rule-origin` van welke default een regel
   is (`RuleOrigin.Kind.DEFAULT` met de pointer), maar niet wat hij voor die default doet. De javadoc van
   `Kind.DEFAULT` noemt de rollen: de regel van de eigen beslissing, de terugval, de foutregel van de rechtvaardiging,
   de doorgeef-, verschil- en eensregels die de uitzonderingsgroepen verzoenen (ch. 7.9), de bijdrageregels van de
   vouwen in rechtvaardiging of gevolg (met hun filterfout-, lidfout- en filterdeponeringsregels), en de leesregel;
   `Kind.STACK` de verzoening aan de wortel (ch. 7.2) en `geen rekendatum`. Die rol staat nu alleen in het label van
   de naam (`TmsTranspiler` r. ±3159 `": default "`, r. ±3220 `": terugval "`, r. ±5163 `": filterfout in "`, …).
   Gemeten op v2 (regels `ece012e`, toolchain `b0b3f849`, de bundel fundament plus herleving-ww): van de 339 regels van
   de DRL-tekst zijn er 64 `default`, 32 `bijdrage aan vouw`, 9 `fout`, 8 `eens`, 4 `terugval` en 2 `filterfout`.
2. **Voor welk type een stapel loopt.** `generated()` geeft bij een stapel het lid dat zij beslist, "as declared on
   its owner's chain" (`GeneratedFrom` met `owner` = het declarerende type). Het type waarvoor dit exemplaar van de
   stapel loopt (`st.owner()` in `stackName`, r. ±2891; ch. 7.10, "the effective stack per type") staat alleen in de
   naam. Op v2 zijn `defaults K:Recht op uitkering WW.indicatie herleving recht op uitkering` en
   `defaults K:RechtOpUitkeringWwBijOntslag.indicatie herleving recht op uitkering` twee stapels met dezelfde
   `generatedFrom` (`{KENMERK, RechtOpUitkering, indicatie herleving recht op uitkering, defaultsstapel}`); de
   werkbank kan ze niet uit elkaar houden of het subtype noemen.
3. **Hoe een stapel is samengesteld.** De werkbank toont nu per IR-groep de varianten, de hoogste eerst. Een effectieve
   stapel van een subtype is meer dan één groep (ch. 7.10: "the subtype's own exceptions" boven "the inherited
   exceptions" en de basis). Gemeten op v2: in `defaults A:RechtOpUitkeringWwBijOntslag.herlevingstermijn` gaat de
   groep `herlevingstermijn art. 21 lid 3 en 5 voor RechtOpUitkeringWwBijOntslag` (met drie `Afwijking van` uit hww-13)
   voor op de basisregel `herlevingstermijn art. 21 lid 3 en 5`; dat staat alleen in de labels (`default 0.0.1.0.0`,
   `eens 0.0.1`) en in de volgorde van de DRL-tekst, en die leest de werkbank niet als semantiek.

## Doel

- `RuleOrigin` krijgt de rol van de regel in de verlaging van zijn vorm, default of stapel, als vaste sleutel: voor
  `DEFAULT` en `STACK` de rollen hierboven, voor `FORM` dezelfde sleutels waar een gewone vorm dezelfde soort
  hulpregel heeft (een bijdrage aan een vouw, een foutregel), en voor de eigen regel van een vorm een sleutel daarvoor.
  Gelezen uit wat de verlaging noteerde toen zij de regel bouwde, zoals `originOf` nu, nooit uit de naam. De bouwer
  kiest de sleutels en zegt waarom; ze zijn stabiel genoeg om in een publiek veld van de toolchain te staan.
- Waar het zonder nieuwe nummering kan: bij een bijdrageregel (en haar filterfout-, lidfout- en
  filterdeponeringsregels) de vouw waartoe zij hoort, als JSON Pointer naar de `Aggregation` in het document, zodat een
  lezer de tekst van die vouw (haar `source.snippet`) kan tonen. Kan dat niet zonder de nummering van de lowering na
  te bouwen, dan zegt het Result waarom en blijft het weg.
- `generated()` noemt bij een stapel ook het type waarvoor zij loopt (de eigenaar van de effectieve stapel), naast het
  lid zoals gedeclareerd; bij een versie van een vorm verandert niets. De bouwer kiest de vorm (een veld naast
  `GeneratedFrom`, of een eigen record voor een stapel) en zegt waarom.
- Per stapel haar samenstelling: de defaults in volgorde van voorrang over de groepen heen, met de nesting van ch. 7.9
  en 7.10, elk als de pointer die `decisionsOf` en `originOf` al gebruiken (een gewone regel die de stapel van een
  subtype opneemt, als `/rules/rules/f/versions/v`), en waar groepen naast elkaar staan zonder volgorde (`unordered`,
  IR-SPEC §6.1) dat ook. Zo kan een lezer de beslisvolgorde per type tonen in plaats van per groep.
- Geen gedragsverandering: DRL-tekst en exec-model bytegelijk (`BranchDumpsCorpusWitnessTest` ongewijzigd).

## Acceptatie

- (a) Een getuige over het corpus: elke regel met een vorm (`FORM`, `DEFAULT`, `STACK`) heeft een rol; elke stapel in
  `generated()` heeft een type, en dat type is het declarerende type of een subtype ervan.
- (b) Gericht: een groep met een uitzonderingsgroep (eigen beslissing, terugval, verzoening), een default met een vouw
  in zijn voorwaarde (de bijdragen en hun filterfout wijzen naar die vouw, als de pointer er komt), een gedateerde
  groep (`geen rekendatum`), en een stapel over een subtype (twee stapels met hetzelfde lid en elk hun eigen type, en
  in die van het subtype de eigen uitzonderingen in de samenstelling boven de geërfde).
- (c) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

## Niet in deze kaart

- Welke default van een stapel in een sessie vuurde (de trace van de toolchain telt per vorm). Als de werkbank dat
  later vraagt, is dat een eigen kaart.
- De namen van de regels en de stapels: die blijven zoals ze zijn.

## Daarna, in de werkbank (geen deel van deze kaart)

Na de toolchain-kaart en een pin-bump zegt de kaart "Verlaagde DRL" bij elke regel van een variant in de woorden van
een jurist wat hij doet (de toets van de variant, een verzamelstap voor haar voorwaarde, een foutregel, …), en noemen de
kaart, de trace en de dekking bij een stapel het type waarvoor zij loopt; de codenaam van de stapel gaat naar de
tooltip. De kaart toont dan per beslisvolgorde (stapel) haar samenstelling, niet alleen de groep van de pagina.

## Result (2026-10-24)

Tak `card/drl-rule-part`, vanaf `1f3d86fb` (main `a97f7579` plus de kaart naar `doing/`). Geen beschermd pad geraakt;
geen delta voor `spec/CONSTRUCTS.md` of `spec/PLAN.md` (geen spec-wijziging, een ingang van de backend). De draad, de
spec, het schema en het frontend veranderen niet.

- `ba04a004` de rol en de vouw in `RuleOrigin`, `stackTypes`, de bouw zonder lezingen met de plaatsen van de knopen, en
  de getuigen.
- dit Result, in het commit na `ba04a004`.

### De rol

```java
public record RuleOrigin(String form, Kind kind, Role role, String pointer, String fold) {
    public enum Kind { NO_FORM, FORM, DEFAULT, STACK }
    public enum Role { OWN, FALLBACK, CONDITION_FAULT, …;  String key(); Set<Kind> kinds(); boolean inFold(); }
}
```

`Role` heeft Engelse constanten en per constante een vaste stringsleutel (`key()`, Engels, met koppeltekens), die de
constructor van de enum expliciet meekrijgt: een latere hernoeming van een constante verandert de sleutel in het publieke
veld niet. `kinds()` zegt bij welke soorten herkomst de rol voorkomt. De constructor van `RuleOrigin` weigert elke andere
combinatie: een rol precies niet bij `NO_FORM`, een rol alleen bij een soort uit zijn `kinds()`, een vouw precies bij een
rol van een vouw (`inFold()`), een pointer precies bij `DEFAULT` (zoals voorheen).

| sleutel | soort | wat de regel doet | label in de naam (alleen ter oriëntatie) |
|---|---|---|---|
| `own` | FORM, DEFAULT | de regel van de vorm zelf: van een regel de regel die het doel schrijft of het kenmerk toekent, van een consistentie de regel die de inconsistentie deponeert, van een uniciteit de regel die de exemplaren met een botsende sleutel aanwijst, van een verdeling de regel die verdeelt, van een creatie de regel die maakt; bij een default de regel van zijn eigen beslissing, ook een gewone regel die de stapel van een subtype opneemt (ch. 7.10) | de naam van de vorm; `default <label>` |
| `fallback` | DEFAULT | de eigen beslissing van een default met uitzonderingsgroepen, waar geen van die groepen beslist (ch. 7.9) | `terugval <label>` |
| `condition-fault` | FORM, DEFAULT | voegt de fout in van een voorwaarde, bewering of rechtvaardiging die kan falen | `: fout in de voorwaarde` |
| `condition-deposit` | FORM | deponeert de §9.5-inconsistentie van een voorwaarde (deling door leeg, wortel van een negatief getal, ch. 1.2); een default bevat die niet | `: deponering in de voorwaarde` |
| `no-evaluation-date` | FORM, STACK | de fout van een sessie zonder rekendatum, naast een vorm met een begrensde geldigheid, of bij een stapel met een gedateerde groep (ch. 8.1) | `: geen rekendatum` |
| `reading` | FORM, DEFAULT | de leesregel van een bouw met lezingen (ch. 5.12), naast de regel die hij leest | `: lezing` |
| `reconcile-pass-on` | DEFAULT, STACK | verzoening (ch. 7.9 onder een default, ch. 7.2 aan de wortel): geeft het eerste conflict van de leden door | `doorgeven <label>` |
| `reconcile-difference` | DEFAULT, STACK | verzoening: maakt een conflict waar het eerste beslissende lid en een ander verschillend beslissen | `verschil <label>` |
| `reconcile-agreement` | DEFAULT, STACK | verzoening: neemt de beslissing van het eerste beslissende lid waar geen lid conflicteert en geen ander verschilt | `eens <label>` |
| `fold-contribution` | FORM, DEFAULT | voegt de bijdrage in van een lid van het bereik dat door het filter komt; één per bereik, en per selectie van een unie | `bijdrage aan vouw <n>` |
| `fold-filter-fault` | FORM, DEFAULT | noteert het lid waarvoor het filter faalt (ch. 5.11) | `filterfout in vouw <n>` |
| `fold-member-fault` | FORM, DEFAULT | noteert bij een per-element-vouw over een tijdlijn het lid dat de casus als emmerscalair geeft (ch. 3.6, 8.4) | `lidfout in vouw <n>` |
| `fold-filter-deposit` | FORM, DEFAULT | noteert het lid waarvoor het filter deponeert (ch. 5.10, 5.11) | `filterdeponering in vouw <n>` |
| `uniqueness-key` | FORM | voegt de sleutel in van een exemplaar van één bron (ch. 5.9) | `: sleutel van <type> <b>` |
| `uniqueness-key-form-fault` | FORM | de fout van een exemplaar waarvan de casus een sleutel op een tijdlijn als emmerscalair geeft (ch. 5.9, 8.4) | `: sleutelvorm van <type> <b>` |
| `uniqueness-channel` | FORM | zet de schending van een exemplaar op het §9.5-kanaal (ch. 5.6) | `: kanaal` |
| `allocation-recipient` | FORM | voegt een ontvanger van een bron in, met zijn gewicht, volgordesleutel en plafond (ch. 4) | `: ontvanger` |

Waarom zo ingedeeld:

- Eén rol per soort regel die de verlaging bouwt, niet per vorm of per tak. Waar een vorm en een default dezelfde soort
  regel hebben, is de sleutel gelijk: de foutregel van een voorwaarde en van een rechtvaardiging, de vier regels van een
  vouw, de leesregel, en `own` voor de regel van een vorm en de eigen beslissing van een default. De lezer combineert de
  rol met `kind` en `pointer`: de toets van een variant is `DEFAULT` met `own`, een verzamelstap voor haar voorwaarde
  `DEFAULT` met `fold-contribution`, de verzoening van de groepen aan de wortel `STACK` met een `reconcile-`-sleutel.
- `fallback` is een eigen rol en geen `own`: het is de eigen beslissing van de default, maar alleen waar geen
  uitzonderingsgroep beslist, en een jurist leest die regel als "anders".
- Families krijgen een voorvoegsel (`reconcile-`, `fold-`, `uniqueness-`, `allocation-`): de sleutel zegt bij welk
  construct de regel hoort zonder opzoektabel. De sleutels volgen de Nederlandse labels van de namen niet (`terugval`,
  `eens`), zodat een herformulering van een naam de sleutel niet raakt.
- Niet onderscheiden: welke soort vorm een `own`-regel heeft (dat staat in het document onder `form`), en het nummer van
  een bereik of selectie van een vouw (de eigen nummering van de verlaging).
- Een regel van geen vorm (`NO_FORM`: standen, koppelingen uit de invoer, de vergelijking van twee schrijvers) heeft geen
  rol; de kaart staat dat toe, en de werkbank toont die regels bij geen variant.

Hoe: `as(Role, RulePlan)` noteert de rol in `roleOfRule` op elke plaats waar de verlaging een regel van een vorm bouwt
(de eigen regels van regel, consistentie, uniciteit, verdeling en beide creaties; `validityFault`, `conditionFaultRule`,
`depositRule`; `keyRule`, `keyFormRule`, het kanaal, de ontvanger; `ownRule`, dat nu de rol `own` of `fallback`
meekrijgt; de drie regels van `reconciliation`; `stackValidityFault`; `contributionRules`, `quantifierContributions`,
`filterFaultRule`, `memberFaultRule`, `filterDepositRule`; `readingRule`). Een naam die één vorm in twee rollen bouwt,
faalt; een naam die twee vormen bouwen, is gedeeld en gaat naar de weigering DROOLS-055 (zie "Review (fixronde,
2026-10-24)"). Aan het eind van `run()` eist de bouw dat elke regel van een vorm, elke leesregel inbegrepen, een rol heeft
(een gedeelde naam uitgezonderd), en dat
een regel een vouw heeft precies als zijn rol een vouwrol is; een ontbrekende notitie faalt dus bij elke bouw van elk
document luid, met de naam van de regel (`IllegalStateException`), en niet pas als iemand `originOf` vraagt. `originOf`
leest de rol uit die notitie, nooit uit de naam.

Gedragsverandering ten opzichte van `drl-rule-origin`, met de getuige bijgewerkt: een leesregel had daar de herkomst van
de regel die hij leest, gelijk in alles; nu heeft hij diens vorm, soort en pointer met de rol `reading`. De corpusgetuige
eiste de gelijkheid en eist nu dit; de tabellen van `RuleOriginWitnessTest` noemen de rol bij elke regel.

### De vouwpointer

`RuleOrigin.fold()`: bij de vier `fold-`-rollen de JSON Pointer van de vouwknoop in het document, in de vorm die
`NodePositions` geeft (dezelfde vorm als de pointers van `decisionsOf` en van de lezingen): een `Aggregation` (ook de
telling over een unie van instantieverzamelingen, de knoop met het `InstanceSetUnion`-argument) of een
`QuantifierFold`. Voorbeelden: `/rules/prioritizedDefaults/3/defaults/0/justification/left` (de som in de
rechtvaardiging van "premie bij een zware vangst", `jutterswet-premiestaffel`), `/rules/rules/1/versions/0/condition`
(een kwantorvouw, `jutterswet-dringende-melding`). De tekst van de vouw is `source.snippet` van die knoop.

Zonder nieuwe nummering: `foldFor` bouwt de regels van een vouw bij haar knoop (de sleutel van `folds` is de knoop zelf,
per onderwerp) en noteert nu bij elke regel van de vouw die knoop (`foldOfRule`). `originOf` zoekt de pointer van de knoop
op in de plaatsen van de lader. Er wordt niets geteld.

De posities in een bouw zonder lezingen, zonder dat die bouw verandert: de lader bepaalt de plaatsen bij elke lading
(`positions.node` in `expression` en `condition`, `positions.version` bij de versies); `loadAll` gaf ze niet terug,
`loadWithPositions` wel, en verder is het dezelfde lading. `DroolsBackend.build` laadt nu altijd met plaatsen, en geeft ze
aan een nieuwe ingang `TmsTranspiler.build(IrLoader.Positioned, ClassLoader)` (zonder lezingen) of aan `withReadings`. In
de transpiler staan ze in een eigen veld `origins`, dat alleen `originOf` leest; het veld `positions`, waarop de verlaging
de lezingen aan- en uitzet, blijft null zonder lezingen. De probe hieronder toont dat het plan gelijk blijft.

Een vouw die meer vormen lezen: de lader leest elke knoop één keer (`NodePositions` weigert een tweede), en elke lezer
van een vouw leest dat ene record. Een gewone regel die de stapel van een subtype opneemt (ch. 7.10), leest de vouw in de
versie van die regel; twee stapels die een default van een supertype delen, lezen de vouw in die default. De pointer
wijst dus de ene knoop aan, en die staat onder de vorm die `form` noemt (de vorm die de vouw eerst inschreef, zie
`formOf`) of, bij `DEFAULT`, onder de default die `pointer` noemt. De corpusgetuige eist dat voor elke regel van een vouw
en meet geen uitzondering (291 regels zonder lezingen).

### Het stapeltype

`TmsTranspiler.stacks()`: `Map<String, Stack>`, per stapelnaam van `generated()` een `Stack(Designatable type,
StackComposition composition)`, in de volgorde van de dekking, met als type het type waarvoor de stapel loopt,
`Designatable.objectType(st.owner().name)`. (Tot het Fable-advies was dit `stackTypes()`, `Map<String, Designatable>`,
met hetzelfde type; het record kwam met de samenstelling, zie "De samenstelling (uitbreiding van 16:01)".) Elke stapel van
`generated` heeft er een, ook een geweigerde, die geen samenstelling heeft. Het type is het declarerende type van het lid
dat `generated` geeft, of een subtype ervan. Voorbeeld `jutterswet-meldtermijn-strandjutter`:
`defaults A:Jutter.meldtermijn` en `defaults A:Strandjutter.meldtermijn` hebben allebei `ATTRIBUUT Jutter.meldtermijn`,
en als type `OBJECTTYPE Jutter` en `OBJECTTYPE Strandjutter`.

Waarom zo:

- `GeneratedFrom` is het record van het frontendmodel dat de draad als `origin` draagt (IR-SPEC ch. 2.13); het krijgt
  geen veld.
- Een record naast `generated` zonder het lid, want het lid staat al in `generated`. De toolchain leest `generated` per
  naam van de dekking (`Playground.generated`); een tweede tabel met dezelfde sleutels voegt het type en de samenstelling
  toe.
- Een `Designatable` en geen string: het type is een element van het model, zoals `source` van `GeneratedFrom`, en de
  toolchain kan het noemen in het vocabulaire van het antwoord (`Naming.name`, `Naming.objectType`) en het schrijven in
  de vorm `{kind, owner, name}` die `generatedFrom` al heeft.
- Niet in `DefaultsGroup`: die noemt alleen de gedragen stapels, `stacks` volgt `generated`.

### Voorlopige besluiten

1. *Vervallen na het Fable-advies (2026-10-24):* de bouw uit de zijtabellen eist nu de plaatsen van de knopen
   (`NodePositions`, verplicht), dus geen bouw is er meer zonder, en `originOf` noemt de vouw van elke regel van een vouw
   (zie "De samenstelling (uitbreiding van 16:01)", punt 3 van het advies).
2. De leesregel krijgt de rol `reading` en niet de rol van de regel die hij leest: de kaart noemt de leesregel als een
   rol van een default, en zo kan een lezer hem herkennen. De DRL die de toolchain toont komt uit een bouw zonder
   lezingen, dus daar komt `reading` niet voor.
3. De sleutels met een voorvoegsel per familie en een expliciete stringsleutel naast de constante (zie boven).

### Getuigen

(a) `RuleOriginCorpusWitnessTest`, uitgebreid. Per regel van elke gedragen golden, op de tak `DRL_TEXT`, zonder en met
lezingen: elke regel met een vorm heeft een rol, en het is de rol die het label in de naam noemt (`roleByName`: de getuige
leest de naam als eigen orakel; de verlaging leest hem nooit); een leesregel heeft vorm, soort en pointer van de regel die
hij leest en de rol `reading`; elke vouw lost op in `ir.json` naar een `Aggregation` of `QuantifierFold`, onder de default
(`DEFAULT`) of onder de declaratie van de vorm (`/rules/<sectie>/<i>/` met de naam van `form`). Per stapel van
`generated`: een type in `stacks`, een objecttype van het model dat het lid declareert of erft, en de naam van de
stapel begint met `defaults A:<type>.` of `defaults K:<type>.` (een tweede orakel uit de naam); `stacks` noemt geen
andere stapel. Gemeten 2026-10-24, de nieuwe vloeren (`EXPECTED_MIN_ROLES` per soort en sleutel): 282 goldens; zonder
lezingen 2124 regels: FORM `own` 796, `fold-contribution` 251, `condition-fault` 48, `uniqueness-key` 19,
`fold-filter-fault` 17, `allocation-recipient` 17, `condition-deposit` 13, `uniqueness-channel` 13, `fold-filter-deposit`
8, `no-evaluation-date` 7, `uniqueness-key-form-fault` 6, `fold-member-fault` 3; DEFAULT `own` 163, `reconcile-agreement`
16, `fallback` 13, `fold-contribution` 12, `reconcile-difference` 8, `reconcile-pass-on` 1; STACK `reconcile-agreement`
15, `reconcile-pass-on` 6, `reconcile-difference` 4, `no-evaluation-date` 1; NO_FORM 687. Met lezingen 922 leesregels
(FORM 746, DEFAULT 176). 291 regels van een vouw (`EXPECTED_MIN_FOLD_RULES`); 69 stapels met een type, 15 daarvan op
een type dat het lid erft, in elf goldens (`EXPECTED_MIN_STACK_TYPES`, `EXPECTED_MIN_SUBTYPE_STACKS`). Geen golden heeft
de foutregel van een rechtvaardiging of een vouw van een default waarvan filter of lid kan falen; die vormen staan in (b).
Sinds de fixronde ook: twee vouwen van één vorm en één onderwerp noemen nooit dezelfde knoop, met de vloer
`EXPECTED_MIN_NESTED_FOLDS` (zie "Review (fixronde, 2026-10-24)"). Ongeveer 19 s.

(b) `RuleOriginWitnessTest`, twaalf tests (vijf nieuw, waarvan één uit de fixronde), per stapel of vorm de hele tabel `<naam> -> <soort> <sleutel>
<pointer> vouw <vouw>` in de volgorde van het model, en `GeneratedNamesWitnessTest`, vijf tests (één nieuw):

- uitzonderingsgroep: `anchor/jutterswet-nestdiepte`: de bladen `own`, onder elke default met groepen
  `reconcile-difference`, `reconcile-agreement`, `fallback` (en `reconcile-pass-on` onder `default 0`), aan de wortel
  `STACK` `reconcile-pass-on` en `reconcile-agreement`;
- default met een vouw in zijn voorwaarde: `jutterswet-premiestaffel` (de som, de telling, minimum en maximum, elk met de
  pointer van haar `Aggregation` onder de rechtvaardiging, en de tekst van de vouw op die pointer),
  `jutterswet-dringende-melding` (een kwantorvouw van een vorm en een van een default); en een document in de test
  (`the_fault_rule_of_a_filter_in_a_justification_names_the_fold_and_its_default`): een telling van de vondsten met een
  datumstap in het filter, waarvan `bijdrage aan vouw 0` en `filterfout in vouw 0` allebei
  `/rules/prioritizedDefaults/0/defaults/0/justification/left` noemen, met de default `/0/defaults/0`, en de default een
  `condition-fault`; de vouw uit `drl-rule-origin` (`the_fault_rule_of_a_justification_…`) noemt nu ook haar knoop;
- gedateerde groep: `jutterswet-uitsluiting-gedateerd`, `geen rekendatum` → `STACK` `no-evaluation-date`;
- stapel over een subtype: `jutterswet-meldtermijn-strandjutter`: de default van de groep op Jutter is `own` in beide
  stapels met dezelfde pointer `/rules/prioritizedDefaults/0/defaults/0`, de stapel van Strandjutter verzoent zijn twee
  groepen aan de wortel; `stacks` geeft `Jutter` en `Strandjutter` bij hetzelfde lid `Jutter.meldtermijn`, en in
  `jutterswet-opgave-subtype` vijf stapels op Vondst, Strandvondst en Barnsteenvondst;
- gewone vormen: `fold-filter-divides-by-zero` (`own`, `condition-fault`, `condition-deposit`, `fold-contribution`,
  `fold-filter-fault`, `fold-filter-deposit` met de vouw van een waarde en van een voorwaarde),
  `anchor/jutterswet-nummerregister` (de vier sleutelregels, `own`, `uniqueness-channel`), `jutterswet-bergersdeling`
  (`allocation-recipient`, `own`);
- een bouw uit de zijtabellen alleen weigert de vouw luid en noemt de rest (vervallen na het Fable-advies: die bouw eist
  nu de plaatsen, en de test is weg).

Rood, met mutaties in de werkmap (niet gecommit; elk tegen `ba04a004`, daarna teruggezet):

- M1, de filterfoutregel genoteerd als `fold-filter-deposit`: de corpusgetuige meldt 34 verschillen ("de rol is
  FOLD_FILTER_DEPOSIT, het label in de naam zegt FOLD_FILTER_FAULT"); rood ook de test van de gewone vormen en die van het
  filter in een rechtvaardiging.
- M2, `originOf` geeft de pointer van de ouder van de vouwknoop: de corpusgetuige meldt 582 vouwen die geen vouw noemen;
  vijf gerichte tests rood (vlakke groep, bijdragen, beide documenten in de test, gewone vormen).
- M3, `stackTypes` met het declarerende type in plaats van de eigenaar: de corpusgetuige meldt de 15 stapels op een
  subtype (de naam begint niet met het type); `two_stacks_of_one_member_each_name_the_type_they_run_for` rood.
- M4, de rol van het uniciteitskanaal niet genoteerd: de bouw faalt in `run()` met "de regel 'eenmalig kaveletiket:
  kanaal' hoort bij een vorm, en de verlaging noteerde niet welke rol hij in haar verlaging heeft"; de corpusgetuige en
  de test van de gewone vormen eindigen met die fout.
- M5, de leesregel genoteerd als `own`: de corpusgetuige meldt 922 leesregels zonder de herkomst van de regel die zij
  lezen en 922 keer een rol tegen het label; `a_subtype_stack_names_the_ordinary_rule_it_took_in` rood.
- M6, `eens` genoteerd als `reconcile-difference`: 62 verschillen in de corpusgetuige; de tests van de geneste
  uitzondering en van de twee stapels rood.
- M7, `terugval` genoteerd als `own`: 26 verschillen in de corpusgetuige; de test van de geneste uitzondering rood.
- M8, `originOf` geeft zonder plaatsen een null-vouw: de constructor van `RuleOrigin` weigert die
  (`IllegalArgumentException`), en de test van de bouw uit de zijtabellen, die de `IllegalStateException` met de
  aanwijzing eist, is rood. (Sinds het Fable-advies is er geen bouw zonder plaatsen, en de arm die M8 muteerde is weg.)

Zonder mutatie zijn de drie klassen groen (17 tests).

### Probe van de DRL-tekst

Een testklasse in de werkmap (niet gecommit, daarna verwijderd) schreef de DRL-tekst van alle 329 goldens, zonder en met
lezingen, naar bestanden: 658 bestanden, waarvan 564 teksten van de 282 gedragen goldens en 94 keer de weigering. Eerst
op `1f3d86fb` (code gelijk aan `a97f7579`), daarna op `ba04a004`: `diff -r` is leeg. Het exec-model rendert hetzelfde
plan; `BranchDumpsCorpusWitnessTest` is ongewijzigd en groen in de suite.

### Suite en gate 12

Op `ba04a004`:

- de JUnit-suite van `backends/drools` (`mvn -B -o test`): 1674 tests, 0 fouten, 0 overgeslagen, waarvan 2 van de
  probe in de werkmap; zonder de probe 1672;
- gate 12 (`scripts/check-drools.sh`): PASS; 279 goldens gespeeld op de exec-model-tak en op de DRL-teksttak, elke
  statusregel gelijk; kanaalvloer 50 niet-gespeelde goldens bij naam; de suite van de gate 1672 tests (vloer 1573),
  nul overgeslagen; `check-drools-codes` PASS (294 codes).

De volledige gate-set draait de hoofdsessie vóór de landing (acceptatie (c)).

### Review (fixronde, 2026-10-24)

Een review (alleen lezen) vond één fout en één gat in de getuigen. Beide zijn verwerkt op de tak
`fix/drl-rule-part-review`: `554128c8` (vondst 1), `d2de393d` (vondst 2) en het commit van deze sectie.

1. Fout: een naambotsing crashte in plaats van geweigerd te worden. Een vorm mag de naam dragen van een hulpregel van
   een andere vorm (WF-020 ziet geen twee vormen van één naam). Het model heeft dan twee regels van één naam. Op
   `a97f7579` gaf zo'n document de weigering DROOLS-055 (`refuseDuplicateRuleName` in `DroolsBackend.analyse`). Op
   `e3985673` faalde `as` eerder, in `run()`, met een `IllegalStateException`, die `UcharBuild.assemble` van de
   toolchain niet vangt. De vier botsingen: `laat` met een datumstap in zijn voorwaarde naast een regel `laat: fout in
   de voorwaarde`; de uniciteit `eenmalig jutternummer` naast een regel `eenmalig jutternummer: kanaal`; `vondstental`
   met een telling naast een regel `vondstental: bijdrage aan vouw 0`; en, in een bouw met lezingen, `actief` met een
   voorwaarde naast een regel `actief: lezing`.

   Wat de fix doet: `as` noteert bij elke regelnaam de vorm die hem als eerste bouwde (`notedBy`). Bouwt de verlaging
   van een tweede vorm een regel met dezelfde naam, dan is die naam gedeeld (`sharedNames`, met beide vormen). `as`
   noteert dan geen tweede rol en faalt niet; de controle aan het eind van `run()` (rol en vouw) slaat de naam over; de
   backend weigert het model met DROOLS-055, zoals op `a97f7579`. Wat luid blijft falen: één vorm die één naam in twee
   rollen bouwt (in `as`), en een regel van een vorm zonder notitie (aan het eind van `run()`). De andere tabellen die
   op de naam sleutelen (`formOfRule`, `defaultOfRule`, `stackOwnRules`, `readingRuleOf`, `foldOfRule`) overschrijven
   bij een botsing en falen niet; de controle in `stackRules` kijkt alleen naar namen onder de naam van een stapel; de
   controle op twee regels van één naam in `DrlRendering.render` komt pas na de weigering. Bij geen van de vier
   botsingen faalt iets vóór de weigering, op beide takken, met en zonder lezingen (getuige hieronder).

   Voorlopig besluit: `originOf` weigert een gedeelde naam luid (`IllegalStateException`, met de namen van beide
   vormen). Anders zou het een herkomst geven die voor een deel van de ene en voor een deel van de andere vorm is (de
   rol van de eerste notitie, de vorm van de laatste). Via `DroolsBackend.build` is dit niet te bereiken, want de
   backend weigert zo'n document. Het geldt alleen voor wie de analyse zelf bouwt (`TmsTranspiler.build(Positioned,
   ClassLoader)`).

   Getuige: `DuplicateRuleNameWitnessTest`, vier tests erbij (acht in de klasse), elk met een document dat in de test
   wordt geschreven. Per botsing eist de test op beide takken, met en zonder lezingen, een `CaseRunner.Excluded` met
   code `DROOLS-055` en de zin `exec-model: Duplicate rule name: <naam> — …`. Bij de botsing met de leesregel geldt dat
   alleen met lezingen; zonder lezingen bouwt het document op beide takken. De eerste test bouwt daarnaast de analyse
   zelf, en eist dat `originOf` de gedeelde naam weigert en voor `laat` `FORM own` geeft.
   - Rood op de code van `e3985673`: alle vier, met "de regel 'laat: fout in de voorwaarde' is gebouwd als
     CONDITION_FAULT en als OWN", "… 'eenmalig jutternummer: kanaal' … als OWN en als UNIQUENESS_CHANNEL", "…
     'vondstental: bijdrage aan vouw 0' … als FOLD_CONTRIBUTION en als OWN" en "… 'actief: lezing' … als READING en als
     OWN".
   - Groen op de bronnen van `a97f7579` (`git checkout a97f7579 -- backends/drools/src` in de werkmap, daarna
     teruggezet), in de vorm van de getuige zonder het deel over `originOf`, dat ik pas daarna toevoegde: de vier
     botsingen gaven daar DROOLS-055.
   - Groen na de fix.
   - Mutatie (niet gecommit, daarna teruggezet): `depositRule` noemt zijn regel `…: fout in de voorwaarde`, zodat één
     vorm één naam in twee rollen bouwt. `RuleOriginWitnessTest` faalt dan met "de regel 'drukte: fout in de voorwaarde'
     is gebouwd als CONDITION_FAULT en als CONDITION_DEPOSIT": die fout van de verlaging blijft luid.

2. Gat in de getuigen: een geneste vouw. Geen getuige legde vast dat een vouw in het filter van een andere vouw haar
   eigen knoop noemt. Eerst gemeten, met een probe in de werkmap (niet gecommit, daarna verwijderd), op de 282 gedragen
   goldens zonder lezingen: 291 regels van een vouw; binnen één vorm noemen twee vouwnummers nooit dezelfde knoop, ook
   zonder onderscheid naar onderwerp; zeven vouwen staan onder een andere vouw van hun vorm, in vier goldens
   (`anchor-forms` drie, `fold-filter-subject-two-spellings` twee, `fold-filter-inner-fold-via-element-role` en
   `jutterswet-schone-maand-na-zware-klacht` één). Elke regel van een vouw bindt in zijn blok van de DRL-tekst precies
   één `$vouwonderwerp`. Vijf van de zeven geneste vouwen hebben hetzelfde onderwerp als de vouw om hen heen.

   Getuigen:
   - Gericht: `RuleOriginWitnessTest.a_fold_in_the_filter_of_a_fold_names_its_own_node` (twaalf tests in de klasse). In
     `anchor-forms` de tabel van `gelijk aan later tarief` (vouw 7 `/rules/rules/6/versions/0/value`, vouw 8
     `/rules/rules/6/versions/0/value/filter/parts/1/right`, een `Aggregation` in een `Aggregation`) en die van `tarief
     naast duur register` (vouw 3 `/rules/rules/3/versions/0/value`, vouw 4
     `/rules/rules/3/versions/0/value/filter/parts/1`, een `QuantifierFold` in een `Aggregation`); de soort knoop op de
     vier pointers, en de tekst van de binnenste vouw.
   - Corpus: `RuleOriginCorpusWitnessTest` eist per vorm en per onderwerp van een vouw dat twee vouwnummers nooit
     dezelfde knoop noemen. Het nummer leest de getuige uit de naam van de regel (`vouw 8`), het onderwerp uit de
     DRL-tekst (de klasse van `$vouwonderwerp` in het blok van de regel); beide zijn orakels van de getuige zelf. Waarom
     per onderwerp: de verlaging bouwt een vouw één keer per knoop en onderwerp, dus één knoop die op twee onderwerpen
     gevouwen wordt, heeft terecht twee nummers; zonder het onderwerp kan de eis bij een later document vals rood geven.
     Nieuwe vloer `EXPECTED_MIN_NESTED_FOLDS = 5`: de vouwen onder een andere vouw van hun vorm en onderwerp, zodat de
     eis in het corpus iets heeft om te toetsen.
   - Rood met de mutatie uit de review (in `foldFor` krijgen de regels van een binnenste vouw de knoop van de buitenste;
     niet gecommit, daarna teruggezet): de corpusgetuige meldt tien verschillen, vijf vormen zonder en met lezingen,
     bijvoorbeeld "anchor-forms | gelijk aan later tarief op dev.uchar.regelspraak.facts.Vergunning: de vouwen [7, 8]
     noemen allebei /rules/rules/6/versions/0/value"; de gerichte test is rood. De twee geneste vouwen met een ander
     onderwerp dan de vouw om hen heen (`fold-filter-inner-fold-via-element-role`, en `eerste klachtvrije maand via de
     lading` in `fold-filter-subject-two-spellings`) ziet de corpuseis bij die mutatie niet; geen getuige pint hun
     pointers.
   - Groen zonder de mutatie.

De drie probes van de reviewer (niet gecommit, in `backends/drools/src/test/java/dev/uchar/regelspraak/cases/`) zijn
verwijderd: `ReviewCollisionTest.java` en `ReviewNestedFoldTest.java`, die de getuigen hierboven vervangen, en
`ReviewRedOriginTest.java`, een eenmalige meting van `originOf` over elk red- en golden-document, zonder probleem.

Geen gedragsverandering behalve dat de vier botsingen weer DROOLS-055 geven, en dat `originOf` een gedeelde naam weigert
in een analyse die iemand zelf bouwt. De plannen veranderen niet: `as` geeft de regel ongewijzigd terug, en geen golden
die de lader draagt heeft een gedeelde naam (anders had de analyse op `ba04a004` gefaald, en gate 12 was daar groen).

Suite en gate 12, op `d2de393d` in de worktree van de fixronde: gate 12 (`scripts/check-drools.sh`) PASS; 279 goldens
gespeeld op de exec-model-tak en op de DRL-teksttak, elke statusregel gelijk; kanaalvloer 50 niet-gespeelde goldens bij
naam; `check-drools-codes` PASS (294 codes). De suite van de gate: 1677 tests (vloer 1573), 0 fouten, 0 overgeslagen;
dat zijn de 1672 van `ba04a004` plus vier in `DuplicateRuleNameWitnessTest` en één in `RuleOriginWitnessTest`. De
volledige gate-set draait de hoofdsessie vóór de landing.

### De samenstelling (uitbreiding van 16:01)

Om 16:01, na de start van de bouw, breidde de werkbank de kaart uit met punt 3 van de Aanleiding, de bullet "Per stapel
haar samenstelling" van het Doel en het deel over de samenstelling in acceptatie (b); die tekst staat sinds `08888f04` in
de kaart. Gebouwd op `08888f04`, met daarna de drie punten van een advies van Fable (2026-10-24), die de hoofdsessie
overnam:

- `f2c522b5` per stapel haar type en samenstelling: `StackComposition`, `TmsTranspiler.stacks()` met het record
  `TmsTranspiler.Stack` (in plaats van `stackTypes()`, na het Fable-advies, punt 1), de getuigen;
- `80d07c25` `Role.inFold()` in plaats van `Role.fold()` (na het Fable-advies, punt 2);
- `8cf63746` geen bouw zonder de plaatsen van de knopen (na het Fable-advies, punt 3);
- `e89dc723` `TmsSkeletonWitnessTest` geeft de plaatsen mee (een achtste klasse van die ingang, zie hieronder);
- deze sectie, in het commit na `e89dc723`.

Geen beschermd pad geraakt; de draad, de spec, het schema en het frontend veranderen niet; geen delta voor
`spec/CONSTRUCTS.md` of `spec/PLAN.md`.

#### De vorm

```java
public Map<String, Stack> stacks()                                        // TmsTranspiler
public record Stack(Designatable type, StackComposition composition) {}   // TmsTranspiler.Stack
public record StackComposition(List<List<Entry>> chains) {                // transpiler.tms
    public boolean unordered() { return chains.size() > 1; }
    public record Entry(String pointer, StackComposition exceptions) {}
}
```

`stacks()` geeft per effectieve stapel van `generated()`, in de volgorde van de dekking, het type waarvoor zij loopt (zie
"Het stapeltype") en haar samenstelling. `stacks().get(naam)` is null voor een naam die geen stapel is (een versie van
een vorm), zoals `generated()` daar een andere vorm geeft. `composition` is null alleen voor een geweigerde stapel; de
reden staat in de dekking. Via `DroolsBackend.build` komt dat niet voor: die weigert dan het hele document.

De samenstelling geeft de ketens aan de wortel, elk met de defaults van hoog naar laag, en bij een default met
uitzonderingsgroepen (ch. 7.9) `exceptions`, in dezelfde vorm; `unordered()` zegt waar ketens naast elkaar staan. Elke
default staat er één keer in, als de pointer die `decisionsOf` en `originOf` gebruiken: een `Default`-knoop,
`/rules/prioritizedDefaults/1/defaults/0/exceptions/members/0/defaults/2`, of de versie van een gewone regel die de stapel
van een subtype opneemt, `/rules/rules/0/versions/0`. Gelezen uit `StackLayout.roots` en `Node.members`, de knopen die de
verlaging voor de stapel legde en waarop `decisionsOf` de controlefeiten legt; de ketens aan de wortel zijn dus
`rootChains`, pointer voor pointer. Nooit uit de labels van de namen (`default 0.0.1.0.0`) of de volgorde van de
DRL-tekst. Gevuld in `run()` direct na `meet("defaults", …)`; de verlaging leest er niets van.

Voorbeeld `jutterswet-meldtermijn-strandjutter`, dezelfde vorm als de meting op v2, met één `Afwijking van` in plaats van
drie:

```
defaults A:Jutter.meldtermijn        [[ /rules/prioritizedDefaults/0/defaults/0 ]]
defaults A:Strandjutter.meldtermijn  [[ /rules/prioritizedDefaults/1/defaults/0
                                          exceptions [[ /rules/prioritizedDefaults/1/defaults/0/exceptions/members/0/defaults/0 ]],
                                        /rules/prioritizedDefaults/0/defaults/0 ]]
```

`/1` is de groep `meldtermijn voor Strandjutter`, een groep-default met één uitzonderingsgroep: de afwijking
`meldtermijn tot het vergunningseinde`. `/0` is de groep `meldtermijn` op `Jutter`, de geërfde basis.

Waarom zo:

1. Eén recursief record voor de wortel en voor de uitzonderingsgroepen van een default, want een default verzoent zijn
   uitzonderingsgroepen zoals de wortel haar groepen verzoent (ch. 7.9: "exactly as the root group reconciles"). De
   ketens zijn `List<List<…>>`, zoals `rootChains`, zodat de wortel er één op één op ligt.
2. Alleen pointers, en geen blok van ch. 7.10 (eigen uitzondering, geërfde, basis, initialisatie) of niveau per default.
   De volgorde van ch. 7.10 is de volgorde van de lijst, en de pointer wijst de groep aan, met haar naam en haar
   onderwerp. De toolchain vertaalt een pointer al naar de sleutels van de beslisser.
3. Type en samenstelling in één record per stapel, gevuld tijdens de bouw (na het Fable-advies, punt 1). Mijn eerste vorm
   was een ingang per naam, `compositionOf(String)`, die bij elke aanroep uit de knopen las en een geweigerde stapel en
   een naam van geen stapel luid weigerde. Het advies koos één tabel naast `generated()` in plaats van twee, en vullen
   tijdens de bouw, zodat een fout van `pointerOf` (een gewone regel als basis met een naamloze regel ervoor) een fout van
   elke bouw is, zoals de controle op de rollen aan het eind van `run()`, en niet pas van wie ernaar vraagt.

Voorlopig besluit (niet uit de kaart, de spec of de code te halen): `unordered()` volgt uit het aantal ketens, meer dan
één, en niet uit de markering `unordered` van het document. Afweging: de verlaging verzoent elke lijst van ketens als
gelijken, of de markering er staat of niet; bij twee of meer ketens eisen WF-050 en WF-075 de markering, dus daar vallen
de twee samen. Bij één keten kan het document `unordered: true` dragen (een `Uitzonderingen ongeordend op X:` met één
lid), en dan staat er niets naast; een lezer die daar "ongeordend" toont, toont iets wat niets beslist. In het corpus
draagt geen groep en geen uitzonderingsplek met één lid de markering (gemeten 2026-10-24). Wil de werkbank de markering
tonen zoals de auteur haar schreef, dan leest zij die in het document op de pointer.

#### Na het Fable-advies (2026-10-24)

1. Eén record per stapel: zie "De vorm". `stackTypes()` is weg. De getuigen die het lazen (`RuleOriginCorpusWitnessTest`,
   `GeneratedNamesWitnessTest`) lezen nu `stacks().get(naam).type()`. De javadoc van `generated` en `DefaultsDecision` en
   de secties "Het stapeltype" en "Wat de toolchain ermee doet" noemen `stacks()`.
2. `RuleOrigin.Role.fold()` heet `Role.inFold()`, zodat het niet botst met `RuleOrigin.fold()`, de pointer van de vouw:
   vier plaatsen in `TmsTranspiler`, de definitie en de schets in "De rol".
3. De bouw uit de zijtabellen, `TmsTranspiler.build(RegelSpraakModel, …, NodePositions positions)`, eist de plaatsen van
   de knopen (`requireNonNull`), zodat `origins` nooit null is. De arm `origins == null` in `foldOf` is weg, en zo ook de
   voorbehouden in de javadoc van `originOf`, `IrLoader.Positioned` en `NodePositions`. De zeven testklassen van die
   ingang (`RuleOriginWitnessTest`, `EvaluationYearWitnessTest`, `DroolsBackendWitnessTest`,
   `MorePeriodicGateWitnessTest`, `TmsTimelineRefusalWitnessTest`, `UnitConflictWitnessTest`, `RoundTripWitnessTest`)
   laden met `IrLoader.loadWithPositions` en geven `positioned.positions()` mee; `DroolsBackendWitnessTest` houdt zijn
   `List.of()`-defaults. `the_fold_of_a_build_without_the_positions_of_its_nodes_is_refused` pinde gedrag dat niet meer
   bestaat en is weg; `RuleOriginWitnessTest` heeft nu elf tests. "Voorlopig besluit 1" hierboven vervalt.
   Twee bestaande getuigen veranderd, omdat de kaart het gedrag verandert dat zij toetsen; beide roepen de ingang via
   reflectie aan met precies de zijtabellen: `DroolsBackendWitnessTest.a_missing_side_table_is_loudly_refused` en
   `TmsSkeletonWitnessTest.the_only_entry_takes_every_side_table` (een achtste klasse, die de lijst van zeven niet
   noemde; de suite vond hem, `e89dc723`). Elk geeft nu de plaatsen mee en eist daarnaast dat de ingang null-plaatsen
   weigert (`NullPointerException` "positions"); wat zij over de zijtabellen eisen is ongewijzigd. Twee kleine
   javadoc-zaken mee: `IrLoader.load` verwijst voor alles wat transpileert naar `loadWithPositions` in plaats van
   `loadAll`, en de javadoc van de zijtabel-ingang, die los vóór `instanceWalks` stond, staat nu bij die ingang.

Niet veranderd, zoals het advies zei: de afhandeling van gedeelde namen (`notedBy`, `sharedNames`).

#### Getuigen

(a) `StackCompositionCorpusWitnessTest`, nieuw, op de exec-model-tak, de analyse waaruit de toolchain `generated` leest.
Per stapel van elke gedragen golden:

- de stapel heeft een samenstelling, en elke default staat er één keer in;
- elke pointer lost op in `ir.json` naar een `Default` van een groep op het lid van de stapel, of naar een regelversie met
  dat lid als doel; een regelversie staat aan de wortel;
- een default heeft uitzonderingsgroepen in de samenstelling precies waar hij ze in het document heeft, en in de volgorde
  van het document: lid m, default j is de pointer van de default met `/exceptions/members/<m>/defaults/<j>` erachter,
  zoveel leden en defaults als het document heeft;
- ketens naast elkaar zijn in het document ongeordend verklaard: aan de wortel draagt elke groep die een keten noemt
  `unordered`, onder een default draagt zijn `exceptions` het;
- aan de wortel, uit het document gelezen, de blokvolgorde van ch. 7.10: de defaults van één groep in de volgorde van de
  groep; de initialisaties onderaan; en vóór de laagste gewone default (de basis) de niveaus die de defaults declareren,
  vanaf het type van de stapel omhoog, dus de eigen uitzonderingen boven de geërfde en die van het kind boven die van de
  grootouder;
- tegen `originOf`: elke default van een regel van de stapel staat in de samenstelling, en een default die zelf beslist
  (een eigen gevolg of besluit, of een gewone regel) heeft daar precies één regel van zijn eigen beslissing, `fallback`
  met uitzonderingsgroepen en `own` zonder. Een groep-default heeft er geen, alleen de regels die zijn
  uitzonderingsgroepen verzoenen. De kaart vroeg "elke default met een rol own of fallback … precies één keer in haar
  samenstelling, en omgekeerd"; het omgekeerde geldt niet letterlijk voor een groep-default, die geen eigen beslissing
  heeft, dus eist de getuige dit;
- tegen `decisionsOf`: op elke case van elke golden met een defaultsgroep die de exec-model-tak speelt, zijn de
  wortelketens van elk record de ketens aan de wortel van de samenstelling van zijn stapel. De stapel van een record is
  die waarvan `Stack.type` het type van het object is en `generated` het lid van het record; de getuige leest geen naam.

Gemeten 2026-10-24, de vloeren: 43 gedragen goldens met een stapel, 69 stapels, 179 defaults (176 die zelf beslissen,
3 groep-defaults), waarvan 4 regelversies (`jutterswet-wrakbasis`, `jutterswet-diepwrakbasis`,
`jutterswet-wrakinitialisatie`, `jutterswet-opgave-subtype`) en 26 onder een default; 16 defaults met
uitzonderingsgroepen; 4 wortels en 8 uitzonderingsplekken met ketens naast elkaar; 8 stapels met een keten over twee
niveaus; 69 stapels, in 331 records, vergeleken met `decisionsOf`. `anchor/jutterswet-toelagegronden` heeft nog twee
ongeordende wortels, maar de backend draagt die golden niet (DROOLS-054). Ongeveer 6 s.

(b) `StackCompositionWitnessTest`, nieuw, zeven tests, per stapel de hele samenstelling:

- `anchor/jutterswet-strandrechtketen` (ch. 7.10.1): de stapel van `Wrakrecht` is `[[/1/defaults/0, /0/defaults/0,
  /1/defaults/1]]` (onder `/rules/prioritizedDefaults`): de eigen uitzondering 55,00, de geërfde 99,00, de eigen basis;
  die van `Strandrecht` is `[[/0/defaults/0, /0/defaults/1]]`; met de groepsnamen en gevolgen op de pointers;
- `jutterswet-meldtermijn-strandjutter`: het voorbeeld hierboven, met de namen van de groepen en de afwijking op de
  pointers;
- een document in de test met de vorm van de meting op v2: de basis op `Recht`, en voor `RechtBijOntslag` de groep
  `herlevingstermijn voor RechtBijOntslag`, een groep-default met drie afwijkingen in één geordende uitzonderingsgroep, de
  middelste met een eigen `Uitzondering op` (op v2 de labels `default 0.0.1.0.0` en `eens 0.0.1`). Op beide takken, in
  een sessie: de samenstelling van beide stapels, de wortelketens van elk record gelijk aan de wortel van zijn stapel, en
  de beslissers (bij twee afwijkingen die gelden de hoogste; bij de middelste met haar uitzondering die uitzondering; op
  een `Recht` de basis);
- `anchor/jutterswet-nestdiepte`: de geneste uitzondering op twee diepten, ongeordend op beide, één keten aan de wortel;
- `jutterswet-wachtgeld`: drie ongeordende groepen aan de wortel; `jutterswet-wrakconflict`: de geërfde ongeordende set,
  heel, in de stapel van `Wrakrecht`;
- `jutterswet-wrakbasis`, `jutterswet-diepwrakbasis`, `jutterswet-wrakinitialisatie`: een gewone regel als basis, ook
  over drie niveaus, en als initialisatie, elk als regelversie;
- een geweigerde stapel (een deling in een default; de analyse zelf gebouwd, want de backend weigert zo'n document heel):
  een vermelding met het type en zonder samenstelling; een naam van geen stapel: geen vermelding.

Rood eerst: de getuigen van een nieuwe ingang compileren niet op de oude code. Daarom per getuige een mutatie in de
werkmap (niet gecommit, elk tegen `8cf63746`, daarna teruggezet; `werk/drl-rule-part/composition-mutations/run.py`, logs
`werk/drl-rule-part/composition-logs/final-mutation-*.log`):

| mutatie | corpusgetuige | gerichte tests die rood worden |
|---|---|---|
| MC1 elke keten omgekeerd, op elke diepte | 379 verschillen: 298 records met andere wortelketens dan `decisionsOf`, 71 keer een default na een lagere van zijn groep, 10 keer een initialisatie boven een gewone default | strandrechtketen, meldtermijn, het v2-document, de regelversies |
| MC2 de uitzonderingsgroepen weggelaten | 52: 12 defaults zonder hun uitzonderingsgroepen, 9 keer `fallback` waar `own` verwacht werd, 31 regels van een default die er niet meer in staat | meldtermijn, het v2-document, nestdiepte |
| MC3 de uitzonderingsgroepen in omgekeerde volgorde van de leden | 8: de groepen niet in de volgorde van het document | nestdiepte |
| MC4 één keten telt als naast elkaar | 152: 144 defaults aan een wortel van één keten en 8 uitzonderingsplekken met één lid die als naast elkaar gelden, zonder `unordered` in het document | strandrechtketen, meldtermijn, nestdiepte |
| MC5 de laagste default van elke wortelketen weggelaten | 367: 298 records met andere wortelketens, 69 regels van een default die er niet in staat | strandrechtketen, meldtermijn, het v2-document, de regelversies |
| MC6 de wortelketens in omgekeerde volgorde | 12 records met andere wortelketens | de ongeordende wortels |
| MS in `Stacks.merge` de geërfde uitzonderingen boven de eigen (de samenstelling en de verlaging) | 3 stapels waarvan de uitzonderingen niet van het eigen type omhoog staan; de wortelketens zijn gelijk, want beide komen uit dezelfde knopen, en de getuige vangt het in het document | strandrechtketen |
| MR een geweigerde stapel niet in `stacks()` | geen (het corpus heeft geen geweigerde stapel in een gedragen golden) | de geweigerde stapel |

Zonder mutatie zijn beide klassen groen (8 tests).

#### Probe

Een testklasse in de werkmap (niet gecommit, daarna verwijderd; een kopie in
`werk/drl-rule-part/composition-logs/probe-src/`) schreef de DRL-tekst van alle 329 goldens, zonder en met lezingen, naar
bestanden: 658 bestanden, waarvan 94 keer de weigering. Eerst op de code van `c7bd3307` (de worktree op `08888f04`, dat
alleen de kaart verandert), daarna op `8cf63746`: `diff -r` is leeg. `BranchDumpsCorpusWitnessTest` is ongewijzigd en
groen in de suite.

#### Suite en gate 12

Op `e89dc723` (de code van `8cf63746`, met de skeletgetuige):

- de JUnit-suite van `backends/drools`, in gate 12 (`mvn -o package`): 1684 tests, 0 fouten, 0 overgeslagen. Dat zijn de
  1677 van `c7bd3307`, plus één in `StackCompositionCorpusWitnessTest` en zeven in `StackCompositionWitnessTest`, min de
  vervallen test van `RuleOriginWitnessTest`. Een losse run (`mvn -B -o test`) op `8cf63746` gaf 1684 tests met één fout,
  in `TmsSkeletonWitnessTest` ("wrong number of arguments: 8 expected: 9"); die verhielp `e89dc723`. Eerder groen in
  losse runs: 1685 op de eerste vorm (met `compositionOf`) en 1685 op `f2c522b5`;
- gate 12 (`scripts/check-drools.sh`): PASS; 279 goldens gespeeld op de exec-model-tak en op de DRL-teksttak, elke
  statusregel gelijk; kanaalvloer 50 niet-gespeelde goldens bij naam; de suite van de gate 1684 tests (vloer 1573), nul
  overgeslagen; `check-drools-codes` PASS (294 codes). Log: `werk/drl-rule-part/composition-logs/gate12-1.log`.

De volledige gate-set draait de hoofdsessie vóór de landing.

### Wat de toolchain ermee doet (kaart `drl-rule-part-fields`)

Per regelnaam van `drlRules`, zoals nu: `RuleOrigin o = build.execModelTranspiler().originOf(name)`. Daarbij:

- de rol: `o.role().key()` in een veld van de vermelding, bij `FORM`, `DEFAULT` en `STACK`; bij `NO_FORM` is
  `o.role()` null. De sleutels staan in de tabel hierboven; `Role.values()` geeft ze allemaal, met `kinds()`. Een
  `switch` over `o.role()` met een `default`-tak die faalt, vangt een rol die een latere pin toevoegt, zoals de
  `switch` over `o.kind()` in `Playground.drlRules` dat nu doet.
- de vouw: `o.fold()`, alleen bij de vier `fold-`-sleutels niet null, een JSON Pointer in hetzelfde document als
  `o.pointer()` (het document dat de emitter van het model schrijft). Bij `DEFAULT` begint hij met `o.pointer()`, bij
  `FORM` met `/rules/<sectie>/<i>/` van de vorm. De tekst van de vouw is `ir.at(o.fold()).path("source").path("snippet")`;
  `ir.at(o.fold()).path("node")` is `Aggregation` of `QuantifierFold`. Voor acceptatie (a) van de toolchain-kaart
  verwacht ik bij de bijdrageregel van de variant `/defaults/2` een vouw onder de pointer van die default, want zo is het
  op elke golden; op v2 heb ik het niet gemeten, die regelset staat buiten uchar.
- het stapeltype: in `Playground.generated`, bij een vermelding met `g.form() == OriginForm.DEFAULTS_STACK`,
  `TmsTranspiler.Stack s = build.execModelTranspiler().stacks().get(d.name())` en `Designatable type = s.type()`; de naam
  in het vocabulaire van het antwoord is `naming.name(type)` (of `naming.objectType(type.name())`), met `type.name()` als
  canonieke naam ernaast waar die verschilt. Elke stapel van `generated()` heeft een vermelding in `stacks()`; een
  ontbrekende is een fout van de analyse. `stacks().get(naam)` is null voor een versie. Sinds het Fable-advies is
  `stackTypes()` weg: `Playground.generated` (r. ±582 op `e39328b4`) leest het nog, en gaat bij de pin-bump over op
  `stacks()`.
- de samenstelling: in dezelfde lus `StackComposition c = s.composition()`, bij een gedragen stapel nooit null. Per keten
  van `c.chains()`, per `Entry e` van de keten, de sleutels van de beslisser uit `Verbaliser.decider(document,
  e.pointer())`, zoals `drlRules` dat al doet met `o.pointer()`: `group`, `pointer` (zonder de plaats van de groep),
  `position` en `variant`, of `rule` voor een regelversie; waar `e.exceptions()` niet null is, daaronder dezelfde vorm
  voor de uitzonderingsgroepen; en `unordered()` waar ketens naast elkaar staan. `document` is
  `Verbaliser.Document.of(IrLoader.loadWithPositions(ir))` van het document dat de emitter van het model schrijft, zoals
  in `drlRules`; `generated` heeft het nu niet, en krijgt het mee. Een vorm, ter oriëntatie (de toolchain kiest de
  sleutels): `"composition": {"unordered": false, "chains": [[{"group": …, "pointer": "/defaults/0", "position": 1,
  "exceptions": {"unordered": false, "chains": [[…]]}}, {…}]]}`. Op `jutterswet-meldtermijn-strandjutter` geeft
  `defaults A:Strandjutter.meldtermijn` één keten: eerst `{group: "meldtermijn voor Strandjutter", pointer: "/defaults/0",
  position: 1}` met daaronder één keten `[{group: "meldtermijn voor Strandjutter", pointer:
  "/defaults/0/exceptions/members/0/defaults/0", position: 1, variant: "meldtermijn tot het vergunningseinde"}]`, dan
  `{group: "meldtermijn", pointer: "/defaults/0", position: 1, variant: "meldtermijn"}`. Voor acceptatie (a) van de
  toolchain-kaart verwacht ik bij `defaults A:RechtOpUitkeringWwBijOntslag.herlevingstermijn` dezelfde vorm: eerst de
  groep-default van `herlevingstermijn art. 21 lid 3 en 5 voor RechtOpUitkeringWwBijOntslag` met in `exceptions` één
  keten van drie afwijkingen (de middelste met een eigen uitzonderingsgroep, het label `0.0.1.0.0`), dan de default van
  de basisregel `herlevingstermijn art. 21 lid 3 en 5`; op v2 heb ik het niet gemeten, die regelset staat buiten uchar.
- zonder DRL (`drlError`): `stacks()` en `generated()` komen uit de analyse (`TmsTranspiler`), niet uit de DRL-tekst, en
  staan op de transpiler van elke tak; de samenstelling dus ook.
- `Role.fold()` heet sinds het Fable-advies `Role.inFold()`; de toolchain gebruikt het niet (zij leest `o.fold()` en
  `o.role().key()`).
- de vouw is er in elke bouw, met en zonder lezingen: sinds het Fable-advies eist ook de bouw uit de zijtabellen
  (`TmsTranspiler.build(RegelSpraakModel, …, NodePositions)`) de plaatsen van de knopen; de toolchain gebruikt die ingang
  niet.
- `Playground.drlRules` hoeft niets aan de soorten te veranderen: er is geen nieuwe `Kind`. De constructor van
  `RuleOrigin` heeft twee componenten meer; de toolchain bouwt er geen.
