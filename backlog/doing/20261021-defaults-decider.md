# defaults-decider: de motor zegt welke variant van een defaults-groep besliste

*Vastgelegd door de hoofdsessie op 2026-10-21, voor de toolchain-kaart `backlog/new/20261020-explanation-defaults.md`
van `uchar-dev/regelspraak-toolchain` ("de uitleg verwoordt ook de defaults-groepen"). Die kaart vraagt dat de uitleg
de variant noemt die besliste, en zegt dat dat van de motor komt, niet uit een herrekening door de Verbalisator. Deze
kaart levert de publieke ingang daarvoor en de naam van een variant op de draad; zij landt vóór de toolchain-kaart,
die haar met een pin-bump volgt. Regelnummers op main `d9c0b9a5`.*

## Aanleiding

Gemeten in de code:

- **Wie besliste, staat niet altijd in de sessie.** De eenvoudige vorm van de TMS-lowering (één keten zonder
  uitzonderingsgroepen, `TmsTranspiler.simpleForm`, `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`
  r. 2622–2652) zet `Decides(subject, stack, index)` alleen als er onder de variant een lagere variant met een regel
  staat; de onderste variant die beslist laat alleen haar schrijving achter. Een onwaar-besluit ("is niet …", "heeft
  geen …") onderaan de keten krijgt helemaal geen regel (r. 2631–2633): het verdringt niets, dus de lowering slaat het
  over, en in de eindtoestand is het niet te onderscheiden van "geen variant besliste". De algemene vorm
  (`generalForm`/`chainRules`, r. 2655–2712) zet per variant die beslist een `Outcome`, ook bij onwaar.
- **Niets ervan is publiek.** `DroolsBackend` (`cases/DroolsBackend.java`) geeft `fixpointOf`, `setDerivedValues` en
  `reached`; `Fixpoint` draagt de afgeleide waarden en kenmerken, niet de beslisser. `Decides` en `Outcome` zijn
  interne controlefeiten.
- **De naam van een variant haalt de draad niet.** `Default` (IR-SPEC ch. 6.1; `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/ir/Default.java`)
  draagt `decision`, `justification`, `consequence`, `initialisation`, `exceptions` en `source`. In `source` staat de
  `// Bron:`-annotatie van het blok (`RuleChecker.defaultOf`, r. 2304–2309). De naam van een `Uitzondering op X:
  <naam>` of `Afwijking van X: <naam>` komt niet in het IR en ook niet in het model (`RuleChecker.java` r. 356: "a name
  that does not reach the wire"); alleen de groep heeft een naam, die van de basisregel. In de regelset herleving-WW v2
  van de werkbank dragen alle 23 uitzonderingen en afwijkingen een artikelverwijzing in hun naam ("herlevingstermijn
  verstreken art. 21 lid 3 en 4 en art. 8 lid 3").

## Besluiten van de eigenaar (2026-10-21)

Voorgelegd door de hoofdsessie, gekozen zoals aanbevolen:

1. **Varianten die niet beslisten.** De uitleg noemt per besliste waarde de beslisser met "omdat" en haar voorwaarden;
   elke variant die vóór de beslisser aan de beurt was met "gold niet, want <eerste falende voorwaarde>" (bij een
   attribuut "gaf geen waarde"); de varianten erna alleen bij naam, als verdrongen. Beslist geen variant, dan zegt de
   uitleg dat en wat het doel houdt; een conflict heet een conflict. (Dat bouwt de toolchain-kaart; deze kaart levert
   wie besliste.)
2. **Geneste lagen.** De uitleg noemt de opgetelde voorwaarde zoals het IR haar draagt (ch. 7.9), met de keten van
   lagen bij naam. Geen wijziging in uchar nodig.
3. **Naam op de draad.** De frontend schrijft de bloknaam als optioneel veld `name` op `Default`, zonder semantiek,
   zoals `source`. In deze kaart.

## Technische keuzes van de hoofdsessie

- **T1 Identiteit van een variant: een JSON-pointer** (RFC 6901) in het IR-document, naar het `Default`-knooppunt:
  `/rules/prioritizedDefaults/3/defaults/1/exceptions/members/0/defaults/0`. Grond: eenduidig op elke diepte, ook in
  een effectieve stapel die overerving samenstelt (RUNTIME-SEMANTICS ch. 7.10), op beide routes van de toolchain op het
  model terug te leggen (bron → frontendmodel → IR, en IR → loader), en de verifier citeert al pointers. Het
  default-indexpad van ch. 7.9 (`Jutter.heffing/0/0`) laat de index van het lid weg en is daarom geen identiteit.
- **T2 De ingang: één publieke methode naast `fixpointOf`** in `DroolsBackend`, met dezelfde weigeringen (dezelfde
  toets op de sessie) en zonder wijziging aan `Fixpoint`, `fixpointOf`, `setDerivedValues` of `reached`. Werknaam
  `decisionsOf(Build, KieSession)`, met per (onderwerp, effectieve stapel) waarop iets besliste één record: het
  onderwerp (de bean), het doel (kenmerk of attribuut), en óf de pointer van de beslissende variant met haar besluit
  (`Boolean` bij een kenmerk, de waarde zoals `Fixpoint.DerivedValue` haar geeft bij een attribuut), óf een conflict
  met de pointers en besluiten die verschilden (ch. 7.2 aan de wortel, ch. 7.9 eronder). Geen record betekent: geen
  variant besliste. Een betere Engelse naam in het vocabulaire van de code mag; de vorm hierboven is de eis.
- **T3 Volledig uit de motor.** Elke beslissing staat als controlefeit in de sessie, ook die van de onderste variant
  van de eenvoudige vorm en een onwaar-besluit onderaan. Kleinste wijziging die de bouwer vindt, binnen deze grenzen:
  de ingang leest controlefeiten en rekent geen voorwaarde na; de semantiek verandert niet (gates 5, 6, 12 en 13 geven
  dezelfde uitkomsten, `EXPECTED_PLAYED` blijft); de wijziging zit in de ene analyse, zodat beide renderingen
  (PatternDSL en DRL-tekst) haar dragen. Onder truth maintenance telt alleen de eindtoestand: een besluit dat werd
  ingetrokken, staat er niet meer. Bij de algemene vorm volgt de ingang de `Outcome`-feiten van de wortel naar de
  variant wier eigen inhoud besliste (een verzoening van leden tegenover de terugval van een vorm-a-variant, ch. 7.9);
  stemmen meer leden overeen, dan noemt hij het eerste dat de verzoening leest, en zegt dat in zijn javadoc.
- **T4 `Default.name`.** Optionele string, een extensieveld zonder semantiek zoals `source`, geen WF-regel (er is geen
  MUST). De RegelSpraak-frontend schrijft de naam van het blok waaruit een variant komt: de basisregel (`Regel
  <naam>`), `Uitzondering op X: <naam>`, `Afwijking van X: <naam>`, en een lid van `Uitzonderingen ongeordend op` als
  dat een naam draagt. Een variant zonder geschreven naam (een groep-default uit een kop, de gegenereerde varianten van
  de opgave-markering, die hun `source` houden zoals nu) krijgt er geen. Catala, F-logic en de handgeschreven ankers
  schrijven geen naam; hun IR blijft gelijk. De zin "zonder semantiek" krijgt zijn artefact zoals `origin` dat kreeg
  (gate 13, ORIGIN-leg, kaart `herkomst-op-de-draad`): een leg die elke `Default.name` weghaalt en dezelfde uitkomst
  eist van ref, clingo en beide Drools-takken, met vloeren.

## Doel

Een consument van de Drools-backend leest na een fixpunt, zonder herrekening, per onderwerp en defaults-groep welke
variant besliste (pointer en besluit, het onwaar-besluit inbegrepen), welk conflict er stond, of dat niemand besliste;
en de variant draagt op de draad de naam die de bron haar gaf.

## Acceptatie

- (a) Rood eerst: een JUnit-getuige in `backends/drools` met een klein eigen model (Jutterswet-namen), die de ingang op
  beide takken bevraagt en op main faalt (de ingang ontbreekt). Handgeschreven verwachte pointers en besluiten, per
  vorm: eenvoudige vorm — de hoogste, een middelste en de onderste variant beslist; een onwaar-besluit in het midden;
  een onwaar-besluit onderaan; niemand beslist; een attribuut dat bij een lege waarde doorvalt; de
  initialisatievariant (ch. 7.3). Algemene vorm — een diep besluit in een geneste uitzondering (ch. 7.9); de terugval
  van een vorm-a-variant; een conflict van een ongeordende wortel (ch. 7.2); een genest conflict. Eén geval met
  overerving (ch. 7.10, een effectieve stapel uit twee declaraties).
- (b) Over het corpus, op beide takken, als JUnit-test naast `BranchDumpsCorpusWitnessTest`: voor elke golden-case met
  een defaults-groep stemt de ingang overeen met `fixpointOf` — een waar-besluit ↔ het kenmerk afgeleid; een
  onwaar-besluit ↔ niet afgeleid; een attribuutbesluit ↔ dezelfde waarde (getallen met `compareTo`); geen record ↔ het
  doel houdt zijn invoer; een conflict ↔ het §9.5-feit. Met een vloer op het aantal getoetste (case, groep, onderwerp).
- (c) `Default.name`: IR-SPEC ch. 6.1 (de alinea **Default**), het schema (`spec/schema/uchar-ir-v1.schema.json`, op
  een canonieke plaats), `ir/Default`, de emitter, de Drools-loader (veldlijst `IrLoader.java` r. 825), de
  verifier-loader (veldlijst `spec/verifier/lib/loader.ml` r. 1595) en de lezer van ref en clingo waar die velden
  toetst. De goldens met blokken (17, `grep -lE "^(Uitzondering op|Afwijking van|Uitzonderingen ongeordend)"
  corpus/golden/*/source.rgs`) opnieuw uitgegeven door de frontend (gates 10 en 16 bytegelijk). Een getuige dat de
  bronroute en de IR-route dezelfde naam dragen. De leg in gate 13 met vloeren (T4).
- (d) De semantiek onveranderd: gates 5, 6, 12 en 13 dezelfde uitkomsten, `EXPECTED_PLAYED` en de lijsten van gate 12
  ongewijzigd. Tests die DRL-tekst of tellingen pinnen, alleen aangepast waar het nieuwe controlefeit ze verschuift,
  elk genoemd in de Result.
- (e) Alle zestien gates groen op de eindstand (`werk/gates.sh`).
- (f) Result: wat er veranderde, de rode getuige, de gate-samenvatting, de veranderde tests en pins met reden, en de
  voorgestelde tekst voor `spec/CONSTRUCTS.md` (het nieuwe extensieveld) en voor `CLAUDE.md` (gate 13: de nieuwe leg
  en haar vloeren).

May touch: `backends/drools/**` behalve de productieruntime (`backends/drools/src/main/java/dev/uchar/regelspraak/runtime/`);
`frontends/regelspraak/**` (`ir/Default`, `RuleChecker`, `IrEmitter`, tests, `grammar/DIVERGENCES.md` §4 als de naam
daar hoort); `spec/IR-SPEC.md` ch. 6.1, `spec/schema/uchar-ir-v1.schema.json` en `spec/verifier/lib/loader.ml` (beschermd:
voorstel, de eigenaar merget); `ref/` en `backends/clingo/` alleen hun veldlijst; `corpus/golden/*/ir.json` en
case-bestanden van de goldens met blokken (opnieuw uitgegeven, niet met de hand); `scripts/check-reftype-derivation.sh`
(beschermd: voorstel); deze kaart.

Out of scope: de uitleg zelf (toolchain-kaart `explanation-defaults`); graaf en dekking; een beslisser in ref of clingo;
een WF-regel op de naam; elke wijziging aan de semantiek van ch. 7.

## Result (voorstel, 2026-10-21)

Tak `card/defaults-decider`, vanaf `d9c0b9a5`, ongemerged en ongepusht: de kaart raakt beschermde paden (IR-SPEC
ch. 6.1, het schema, `spec/verifier/lib/loader.ml`, `scripts/check-reftype-derivation.sh`). De kaart blijft in
`doing/` tot de eigenaar het voorstel keurt.

- `8fbfe052` de ingang (T1–T3; acceptatie (a), (b), (d)).
- `3b94de40` `Default.name` (T4; acceptatie (c)): frontend, schema, IR-SPEC, beide laders, twintig goldens.
- `5d7a0d0a` de naamleg van gate 13 (T4).
- `1b28549e` dit Result.
- `bf4642bc` (hoofdsessie) de corpustoets legt elke pointer op het document.
- `87b48426` (hoofdsessie) de rode case `defaults-negation-at-bottom-without-evaluation-date`.

### De ingang

`DroolsBackend.decisionsOf(Build, KieSession)` → `List<DefaultsDecision>`, naast `fixpointOf`, met dezelfde toets op de
sessie: de controle van `TmsTranspiler.readFixpoint` staat nu in `requireFixpoint`, die `readOf`,
`setDerivedValues` en `decisionsOf` delen, met dezelfde zinnen. `Fixpoint`, `fixpointOf`, `setDerivedValues` en
`reached` zijn ongewijzigd.

```java
public record DefaultsDecision(Object object, String member, boolean characteristic, List<List<String>> rootChains,
                               Decider decider, Conflict conflict) {
    public record Decider(String pointer, Object decision) {}
    public record Conflict(String pointer, String key, List<Decider> deciders) {}
}
```

Precies één van `decider` en `conflict`; geen record voor (object, stapel) betekent: geen default besliste.
`member` zoals het model het lid declareert (als `Fixpoint`). `decision` is bij een kenmerk een `Boolean` en bij een
attribuut de waarde die de stapel schreef (de vorm van `Fixpoint.DerivedValue`). `Conflict.pointer` is de default
wiens uitzonderingsgroepen conflicteerden, null aan de wortel; `key` de regel van het §9.5-feit; `deciders` elk
beslissend lid in ledenvolgorde, elk tot zijn eigen default gevolgd. `rootChains` geeft per groep aan de wortel van de
effectieve stapel de pointers in evaluatievolgorde: bij overerving (ch. 7.10) is dat geen van beide declaraties, en
daarmee kan de Verbalisator de varianten vóór en na de beslisser noemen; onder de wortel volgt dat uit de pointer
zelf, want een uitzonderingsgroep wordt nooit herschikt.

Pointers (T1): naar de `Default`-knoop, `/rules/prioritizedDefaults/i/defaults/j(/exceptions/members/m/defaults/l)*`.
Afwijking van T1, nodig: een gewone regel die de stapel van een subtype als basis of initialisatie opneemt (ch. 7.10,
SUBTYPING §4.1; in het corpus `jutterswet-diepwrakbasis`, `-opgave-subtype`, `-wrakbasis`, `-wrakerfbasis`,
`-wrakinitialisatie`) heeft geen `Default`-knoop; die default heet naar zijn regelversie,
`/rules/rules/k/versions/v`. De backend leidt k en v af uit de modelvolgorde en de versienaamgeving van de lader
("<naam> #i"); een naamloze regel vóór de opgenomen regel maakt dat onbepaald en wordt luid geweigerd (WF-107 laat
zo'n document niet door de verifier).

Voorbeelden uit `DefaultsDecisionWitnessTest` (P = `/rules/prioritizedDefaults/`):

- genest besluit, twee uitzonderingslagen diep (ch. 7.9):
  `P1 penninggerechtigd: P0/defaults/0/exceptions/members/0/defaults/0/exceptions/members/0/defaults/0 true`;
- terugval van een vorm-a-variant één laag diep, met een onwaar-besluit:
  `P2 penninggerechtigd: P0/defaults/0/exceptions/members/0/defaults/0 false`;
- onwaar-besluit in het midden en onderaan van de eenvoudige vorm: `J2 vrijgesteld: P0/defaults/1 false`,
  `J4 vrijgesteld: P0/defaults/3 false`;
- genest conflict: `P4 penninggerechtigd: conflict at P0/defaults/1 under Jutter.penninggerechtigd/1:
  P0/defaults/1/exceptions/members/0/defaults/0 true, P0/defaults/1/exceptions/members/1/defaults/0 false`;
- conflict van een ongeordende wortel: `P8 wachtgeld: conflict at the root under Jutter.wachtgeld: P1/defaults/0 30,
  P2/defaults/0 45`; stemmen de leden overeen, dan het eerste: `P9 wachtgeld: P1/defaults/0 30`;
- overerving: op een Wrakrecht de geërfde uitzondering op plaats twee, `W2 uitkering: P0/defaults/0 99`, met
  `rootChains` `[[P1/defaults/0, P0/defaults/0, P1/defaults/1]]`; de opgenomen regel van een Diepwrak,
  `D1 uitkering: /rules/rules/0/versions/0 200`.

Hoe de ingang leest (T3): de eenvoudige vorm laat per object één `Decides` staan, op de default die besliste; de
algemene vorm per keten één `Outcome`, van haar hoogste default die afrondde. De ingang volgt de `Outcome`s van de
wortel omlaag: bij een default met uitzonderingsgroepen de leden als één ervan afrondde, anders de default zelf (de
terugval), en leest de leden zoals de verzoening ze leest (eerste conflict gaat door, verschillende besluiten zijn
een conflict, gelijke besluiten beslissen als het eerste lid). Hij rekent geen voorwaarde na. Waar een
evaluatiefout staat, geeft hij wat de controlefeiten zeggen; de javadoc zegt dat een afnemer eerst de fouten van
`fixpointOf` leest.

### De lowering

Alleen `TmsTranspiler.simpleForm` veranderde, in de ene analyse, dus in beide renderingen: elke default die beslist
zet zijn `Decides` in, ook de onderste, en een onwaar-besluit onderaan krijgt weer een regel (die alleen zijn
`Decides` inzet). De algemene vorm liet al bij elk besluit een `Outcome` staan. Gemeten vóór de wijziging, met de
ingang al gebouwd: de getuige gaf voor de eenvoudige vorm 8 van de 15 records (de onderste beslissers en `J4
vrijgesteld` ontbraken), voor de overerving 4 van de 7, en de algemene vorm was al groen.

Wat het verschoof:

- `DefaultsplanWitnessTest` (twee pins): de onderste default van `anchor/jutterswet-vondstwaardering` en van de
  gedateerde `anchor/jutterswet-uitsluiting` draagt nu `Decides[..., index=2]` in zijn feiten; de klasse-doc zegt
  waarom. Geen andere test pinde DRL-tekst of tellingen die verschoven: de volledige suite had na de wijziging
  precies deze twee rood (1635 tests).
- `TmsTranspiler.DefaultsGroup`: `controlFacts` noemt `Beslist` nu bij elke eenvoudige stapel met een beslissende
  default; `rules` groeit met één regel waar een kenmerkgroep op een onwaar-besluit eindigt, in het corpus alleen
  `jutterswet-penningrecht-catala`. Geen test pint die getallen.
- Gate 12: `EXPECTED_PLAYED` 261 en alle lijsten ongewijzigd, gate 12 `--fast` groen na deel 1 en deel 2.

### (b) over het corpus

`DefaultsDecisionCorpusWitnessTest`, naast `BranchDumpsCorpusWitnessTest`: per case, stapel en object van het type
van de stapel, op beide takken, tegen `fixpointOf`, en elk §9.5-feit met EF-027 tegen het conflict van een record.
Gemeten 2026-10-21: 324 (case, stapel, object) per tak in 42 goldens — 48 toekennen, 19 niet toekennen, 194
attribuutbesluiten, 12 conflicten, 51 zonder record. Vloeren `EXPECTED_MIN_CHECKED=324`, `EXPECTED_MIN_GOLDENS=42`.

Aangevuld door de hoofdsessie (`bf4642bc`): elke pointer van een record (de wortelketens, de beslisser, het conflict
en zijn beslissers) moet in het document van de golden een knooppunt opleveren dat het doel van het record schrijft:
een `Default` van een groep op dat lid, of de versie van een regel met dat doel; en de beslisser ligt op of onder een
default van de wortelketens. De toolchain legt deze pointers op zijn model terug, dus een verschoven pointer moet hier
falen. Er moet minstens één pointer naar een regelversie zijn opgelost. Rood met een mutant, niet gecommit:
`rulePointer` met `form + 1` faalt op 68 records (onder meer `jutterswet-diepwrakbasis`, `-wrakbasis`,
`-opgave-subtype`).

### De rode getuige (a)

`DefaultsDecisionWitnessTest` op `d9c0b9a5` plus alleen de test (`mvn -o -q test-compile`):

```
[ERROR] .../DefaultsDecisionWitnessTest.java:[5,44] cannot find symbol
  symbol:   class DefaultsDecision
  location: package dev.uchar.regelspraak.transpiler.tms
[ERROR] .../DefaultsDecisionWitnessTest.java:[271,56] cannot find symbol
  symbol:   method decisionsOf(dev.uchar.regelspraak.cases.DroolsBackend.Build,org.kie.api.runtime.KieSession)
  location: class dev.uchar.regelspraak.cases.DroolsBackend
```

En met de ingang maar vóór de lowering (EXEC_MODEL, eenvoudige vorm), verwacht 15 records, gekregen:

```
[J1 toeslag: P2/defaults/0 7, J1 vergoeding: P1/defaults/0 40, J1 vrijgesteld: P0/defaults/0 true,
 J2 vergoeding: P1/defaults/1 10, J2 vrijgesteld: P0/defaults/1 false, J3 vergoeding: P1/defaults/1 10,
 J6 toeslag: P2/defaults/0 7, J6 vrijgesteld: P0/defaults/0 true]
```

Groen sinds `8fbfe052`, op beide takken.

### Default.name

Een optioneel extensieveld `name` op `Default`, in het schema direct na `node` (gate 2 houdt de canonieke volgorde),
zonder semantiek zoals `source`, geen WF-regel. De frontend schrijft (`RuleChecker.defaultOf`) de naam van de regel
waaruit een default komt: de basisregel, het blok na de dubbele punt van `Uitzondering op X:` en `Afwijking van X:`,
een lid van `Uitzonderingen ongeordend` (met en zonder `op`), en een geschreven initialisatie die de opgavemarkering in
haar groep opneemt. Een groep-default uit een kop en de twee gegenereerde opgavedefaults krijgen er geen.

Uitgezocht: een lid van `Uitzonderingen ongeordend op` draagt altijd een eigen naam. De grammatica:
`uitzonderingGroep : UITZONDERINGEN ONGEORDEND ( OP uitzonderingBasisNaam )? COLON regel+` en
`regel : REGEL regelNaam benaming* regelVersie+`; in `jutterswet-nestkenmerk-rgs` heten de leden "vrijstelling
vervalt bij ny" en "vrijstelling bij nz".

- `ir/Default`: component `name` (laatste, naast `source`); `RuleChecker` op zeven plaatsen; `IrEmitter.dflt`; de
  javadoc bij WF-020 (`checkFormNames`) zegt nu dat de bloknaam geen vorm op de draad noemt.
- Drools-lader: `name` in de veldlijst, gelezen met `optText`. `Stacks.ruleMember` geeft een opgenomen regel zijn
  naam ook.
- Verifier-lader: `name` in de `ext`-lijst, gelezen als string en niet bewaard (het AST-record `default` is
  ongewijzigd). ref en clingo lezen het document via diezelfde bibliotheek (`uchar-ir-v0`) en hebben geen eigen
  veldlijst; de Catala- en F-logic-frontend bouwen dat record en hoefden niet te veranderen.
- IR-SPEC ch. 6.1: de alinea **Default** noemt het veld, en een nieuwe alinea **Default.name has no semantics** met
  een uitgewerkt voorbeeld (`jutterswet-meldingsketen`). `grammar/DIVERGENCES.md` §4, "Emission": waar de naam
  vandaan komt.
- Twintig goldens opnieuw uitgegeven door de frontend (`--ir`, en `--ir --cases` bij een `scenario.rgs`): de 17 met
  blokken en `jutterswet-opgave-bedrag`, `-opgave-boven-berekening`, `-opgave-datum`, waar de geschreven
  initialisatie van de opgavegroep haar naam krijgt. Alleen `name`-velden kwamen erbij (64), nagerekend door elk
  document zonder de namen met het oude te vergelijken; geen case-bestand veranderde; de andere 106 rgs-goldens zijn
  bytegelijk.
- `InputMarkingTest` (twee pins): de initialisatiedefault van de opgavegroep draagt nu `"name"`; de test zegt
  bovendien dat de twee gegenereerde defaults er geen dragen.
- Getuige van de twee routes: `backends/drools/src/source-route/.../loader/DefaultNameSourceRouteTest` (gate 15):
  per rgs-golden geeft de lader op elke pointer de naam die de resolver de default daar gaf; 20 goldens, 64 namen.

### De naamleg van gate 13

Naar het voorbeeld van de herkomstleg: elke golden met een benoemde default en case-bestanden opnieuw gespeeld met
elke `name` weggehaald, ref, clingo en beide Drools-takken moeten de onaangeraakte uitkomst geven; geen
overlevendenlijst. Vloeren `EXPECTED_MIN_NAME_GOLDENS=20` en `EXPECTED_MIN_NAME_STRIPPED=64`, gemeten 2026-10-21.
Groen: "name leg: 20 golden(s) with named defaults, floor 20; 64 names stripped (floor 64); 256 executor replays
reproduce the untouched outcome" (182 s voor de hele gate). Rood met een mutatie, niet gecommit: de Drools-lader die
een default aan de wortel overslaat waarvan de naam met "melding" begint, rood op `jutterswet-opgave-datum`,
`-opgave-subtype` en `-premiestaffel`, op beide takken.

### Voorgestelde tekst voor `spec/CONSTRUCTS.md` (een nieuwe rij)

```
| 43 | Name of a default — the block it comes from | an explanation of a defaults decision names the default that decided; the RegelSpraak frontend lifts each `Regel`, `Uitzondering op X: <naam>`, `Afwijking van X: <naam>` and `Uitzonderingen ongeordend` member into a default, and until then its name reached neither the wire nor the model (only the ch. 7.9 index path, which leaves out the member index) | core (metadata) | neither: an extension field without semantics | v0 has none. **Landed (2026-10-21, card `defaults-decider`):** optional `name` on `Default`, written just after `node` (IR-SPEC ch. 6.1, "Default.name has no semantics"); no WF rule, no namespace (WF-020 holds form names only). The frontend writes the name of the block a default comes from, an authored initialisation in the opgave group included; a group-default a head makes and the two generated opgave defaults carry none; Catala, F-logic and the hand-written anchors write none. The Drools loader keeps it, the shared OCaml loader reads it as a string and keeps it nowhere. Witnesses: 20 rgs goldens with 64 names, `DefaultNameSourceRouteTest` (the two routes give every default the same name); gate 13's name leg holds the executors indifferent to the field stripped | out: names of exception groups (they keep the head's text in `source`); the Drools backend's identity of a deciding default is its JSON pointer (`DroolsBackend.decisionsOf`), not its name |
```

### Voorgestelde tekst voor `CLAUDE.md` (gate 13, na de zin over de ORIGIN-leg)

```
    Since 2026-10-21 (card `defaults-decider`) a NAME leg beside it:
    every golden whose defaults carry a `name` (IR-SPEC ch. 6.1,
    `Default.name`) and that has case files is replayed with every name
    STRIPPED, and ref, clingo and both Drools branches must reproduce the
    untouched outcome (no survivor list); floors
    `EXPECTED_MIN_NAME_GOLDENS=20` and `EXPECTED_MIN_NAME_STRIPPED=64`
    (measured 2026-10-21).
```

### Niet gedaan of open, met reden

- De Verbalisator bouwt niets van dit; de toolchain-kaart `explanation-defaults` volgt met een pin-bump. Voor die
  kaart: een pointer `/rules/rules/k/versions/v` (een opgenomen regel) wijst naar een regelversie, niet naar een
  `Default`; de emitter groepeert versies van één vorm, dus k is de index van de vorm op de draad, niet die in
  `model.resolvedRules`.
- Voor de pin-bump: een kenmerkstapel die op een onwaar-besluit eindigt, heeft nu één regel meer, die ook vuurt; een
  scenario-trace of dekking van de toolchain die het vuren per vorm telt, kan daar één vuring meer tonen. In het
  uchar-corpus raakt dat alleen `jutterswet-penningrecht-catala`, en geen uchar-test pint die telling.
- `decisionsOf` weigert niet waar een evaluatiefout staat; het geeft dan wat de controlefeiten zeggen, zoals
  `fixpointOf` de fouten als gegevens geeft. Bewust: de ingang staat naast `fixpointOf`, en de javadoc zegt dat een
  afnemer de fouten eerst leest.
- De lijst van `decisionsOf` heeft geen vaste volgorde, zoals de lijsten van `Fixpoint`.
- Op getuige (b) alleen de mutatieproef van de pointers (hierboven); op de naamleg die van de bouwer.

### Vondsten buiten de kaart

- Een fout in de rechtvaardiging van een onwaar-besluit onderaan een eenvoudige kenmerkstapel (een datumstap buiten
  bereik, een contextwaarde zonder rekendatum) gaf in Drools geen fout, want die default had geen regel en dus geen
  foutregel; ref evalueert de rechtvaardiging van elke default die aan de beurt komt (`own_decision` in
  `ref/lib/eval.ml`) en faalt daar. Met de regel van deze kaart krijgt zo'n default ook zijn foutregel
  (`TmsTranspiler.ownRule`), zodat Drools nu ook faalt. Nagemeten door de hoofdsessie met een contextwaarde zonder
  rekendatum (EF-011): ref en clingo falen; de Drools-jar op `d9c0b9a5` geeft `NOFAULT ran to a model without
  faulting`, die van deze tak `FAULT EF-011 {}`. Vastgelegd als rode case
  `corpus/red/defaults-negation-at-bottom-without-evaluation-date/` (`87b48426`), die gate 8 in de drie uitvoerders
  houdt. Met een niet-gebonden parameter (EF-014) is het verschil er niet: Drools faalt daar op geen enkele vorm, de
  bekende `NOFAULT`-regel van `aanvoergeld-parameter-unbound` in `scripts/check-red.sh`.

### Gates

De volledige set op `5d7a0d0a`, `bash werk/gates.sh … gates-1`: alle zestien `rc=0`, `EXIT 0` (gate 12 649 s, played
261 en de DRL-teksttak 261; gate 13 193 s, met de naamleg). Tijdens de bouw: de Drools-suite (1635 tests na deel 1,
groen na de twee pins), de frontendsuite (1221 tests), gate 12 `--fast` na deel 1 en deel 2, gates 2, 3, 4, 10, 13,
15 en 16 los.
