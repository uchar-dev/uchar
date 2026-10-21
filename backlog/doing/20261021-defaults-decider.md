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
