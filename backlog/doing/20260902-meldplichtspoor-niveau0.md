# Niveau 0: de meldplicht als gevouwen positiewaarde (nul IR-wijziging)

Goal: Eén nieuwe golden, `corpus/golden/jutterswet-meldplichtspoor/`, die
met UITSLUITEND gelande constructen een normatieve positie draagt en over
een gebeurtenisreeks bijwerkt: een keuzetype `meldstatus` (`Openstaand`
met de resterende termijn als getallading, `Vervuld`, `Geschonden`) als
accumulator van een `Fold` over een lijstinvoer van gebeurteniswaarden, met
`Match` op de accumulator en `IfThenElse`/`Let` in het lichaam, geschreven
naar een somtype-getypeerd attribuut op de Jutter. Het doel is BEWIJS, niet
vorm: `TOEKOMST_hohfeld_deontiek_20260822.md` §10 stelt niveau 0 voor als
stresstest die met bewijsmateriaal — niet met architectuurintuïtie —
bepaalt welke knopen van §5 hun plaats verdienen, en deze kaart maakt dat
uitvoerbaar zonder één IR-wijziging. Twee dingen komen er hard uit. (a) Of
de gelande som-, record- en foldkern een positie werkelijk draagt door BEIDE
executors: de samengestelde vorm — een `Fold` met SOMTYPE-accumulator over
een LIJSTINVOER — draait vandaag NERGENS in het corpus, terwijl WF-065 een
gedeclareerd `SumType` als lijstelement toelaat en WF-070 een
`SumType`-accumulator uitdrukkelijk toelaatbaar verklaart; de twee helften
draaien alleen los (somtype-accumulator over een objectattribuut in
`jutterswet-penningstaffel-catala` en `jutterswet-kamerhuishouden-catala`;
numerieke accumulator over een lijstparameter in `jutterswet-kinderdeler`
en `anchor/jutterswet-staffelvergoeding`). Die lacune sluiten heeft
corpuswaarde los van de deontische vraag. (b) WAAR de vorm breekt: de
verwachting is dat hij breekt op IDENTITEIT, want WF-060 laat als
somtype-lading alleen een getal, een dagpreciezige datum of een
gedeclareerd somtype toe (tekst is al negatief-gepind door
`corpus/negatief/wf060-text-payload`) en WF-071 kent voor structvelden
dezelfde beperking plus structs — houder en wederpartij kunnen dus niet ín
de waarde zitten. De houder is daarom het regelsubject en de wederpartij
per regel vast. Die grens wordt in de NOTE vastgelegd als het scherpste
argument dat een latere kernuitbreiding moet beantwoorden. De kaart
levert géén IR-uitbreiding, géén WF-nummer en géén spec-tekst; zij levert
een golden, hand-gerekende absolute verwachtingen, en een eerlijk
opgetekende grens.

Acceptance:
- De golden bestaat als `corpus/golden/jutterswet-meldplichtspoor/` met
  `ir.json`, ten minste drie `case*.json` (op tijd gemeld → `Vervuld`;
  te laat of niet gemeld → `Geschonden`; lege gebeurtenisreeks →
  `Openstaand` met de volle termijn) en een `NOTE.md` in de vorm van
  `corpus/golden/jutterswet-kinderdeler/NOTE.md`: hand-gerekende absolute
  waarden per casus, de gate-verwijzingen, en de opgetekende grens uit (b).
  De NOTE draagt GEEN herkomst — geen externe taal- of projectnamen, geen
  paden buiten de repo (gate 1; corpusregel "code en data dragen geen
  herkomst, git-historie en het plan doen dat").
- Elke casus pint de einduitkomst ABSOLUUT als
  `attributeValue` met een `constructor`/`payload`-waarde — de vorm die
  `anchor/jutterswet-beschikkingsom` al door beide executors speelt — plus
  `consistent`.
- ROOD-EERST waar van toepassing: als de samengestelde vorm in `ref` of
  clingo op een bestaande weigering stuit (de S19-inventaris, bijvoorbeeld
  de gesloten-lichaam-grammatica of een lijstbron), toon die weigering
  eerst, en laat haar daarna groen worden of — als dragen buiten deze kaart
  valt — laat de golden NIET landen en lever in plaats daarvan de
  opgetekende weigering plus een vervolgkaart. Een golden die maar door één
  executor speelt, landt niet: dat is de gate-regel, geen onderhandeling.
- `scripts/check-oracle.sh` (gate 5) en `scripts/check-clingo.sh` (gate 6)
  spelen alle nieuwe casussen met exact deze verwachtingen — ref ≡ clingo.
- `scripts/check-noop.sh` (gate 7) faalt op elke nieuwe casus (de
  verwachtingen gelden niet op de rauwe invoer). Als een casus de no-op
  overleeft, is de casus verkeerd gesneden — herzien, NIET aan de
  survivorlijst toevoegen (`scripts/` is protected).
- `scripts/check-drools.sh` (gate 12) blijft groen: een lijstinvoer valt
  onder de bestaande per-vorm-autoweigering, die haar reden gegroepeerd in
  het gate-rapport afdrukt. Toon dat rapport; voeg NIETS toe aan de
  waarde-divergentielijst in het script.
- Alle twaalf gates groen, met getoonde uitvoer:
  `scripts/check-self-contained.sh`, `check-ir-schema.sh`,
  `check-verifier.sh`, `check-case-schema.sh`, `check-oracle.sh`,
  `check-clingo.sh`, `check-noop.sh`, `check-rood.sh`, `check-catala.sh`,
  `check-regelspraak.sh`, `check-toka-parity.sh`, `check-drools.sh`.
  Gates 9 en 10 zijn per constructie ongemoeid: de golden draagt geen
  `bron.catala_en` en geen `bron.rgs`.
- Het `Result:`-briefje bij het verplaatsen naar `done/` benoemt expliciet:
  welke vorm speelde, welke grens is geraakt, en welke van de vier posten
  uit `TOEKOMST_hohfeld_deontiek_20260822.md` §5 door dit bewijs
  waarschijnlijker respectievelijk onwaarschijnlijker is geworden.

May touch: `corpus/golden/jutterswet-meldplichtspoor/**` (nieuw),
`backlog/`.

Out of scope: elke IR-, schema-, verifier- of spec-wijziging (`spec/**` is
protected en er is hier geen nieuwe WF-regel nodig); `corpus/golden/anchor/`
(protected — promotie tot anchor is een maintainersbesluit, niet deze
kaart); `corpus/negatief/` en `corpus/rood/` (de identiteitsgrens is al
gepind door `wf060-text-payload`; nieuwe negatieven horen bij een nieuwe
WF-regel); `scripts/**` (protected — geen survivor- of exclusielijst
aanpassen); elke handelings- of positieknoop, elk beëindigingseffect, elke
frameregel en elke spoorindex in het casusformaat (dat is rij 32/37-werk en
vraagt een fase, geen kaart); en elke verwijzing naar externe normtalen in
corpusdata.

## Bouwnotitie

**Uitkomst: de golden is GELAND.** Geen enkele weigering onderweg: de
samengestelde vorm speelt door beide executors, met nul IR-, schema-,
verifier- of spec-wijziging. Het rood-eerst-protocol is niet geactiveerd,
want er was geen rood: de verifier was schoon in één keer en beide
executors gaven meteen de hand-gerekende waarden.

### De gebouwde vorm

`corpus/golden/jutterswet-meldplichtspoor/` — twee keuzetype-domeinen
(`meldstatus`: `Openstaand(dagen)`/`Vervuld`/`Geschonden`; `gebeurtenis`:
`Dagverloop(dagen)`/`Melding`), één lijstparameter `meldreeks` van
gebeurteniswaarden (WF-065's gedeclareerde `SumType` als lijstelement —
de smalste vorm die het casusschema draagt, en hij DRAAGT hem: geen
hervorm naar getallen nodig), en één BEREKENING naar het somtype-attribuut
`Jutter.meldpositie`. De waarde is een `Fold` met somtype-accumulator
(WF-070), lambda-lichaam = `Match` op de accumulator met geneste `Match`
op het element en `IfThenElse` daaronder. `Vervuld` en `Geschonden` zijn
absorberend. Init = `Openstaand(30)`.

Géén `Let`: die zit hier niet in het lichaam omdat er niets te delen valt
(de vorm is al minimaal). De ASP-kant draagt een `Let` binnen een gesloten
lichaam trouwens wél (getuige `jutterswet-kinderlet`); wat zij weigert is
een binder die BUITEN het gesloten lichaam gebonden is — die grens is hier
niet geraakt.

### Handrekening (vooraf, met de hand, termijn = 30)

| casus | reeks | verloop | verwacht |
|---|---|---|---|
| case1 op tijd gemeld | `[Dagverloop(12), Melding]` | `Openstaand(30)` → 12≤30 → `Openstaand(18)` → melding | **`Vervuld`** |
| case2 te laat gemeld | `[Dagverloop(20), Dagverloop(14), Melding]` | `Openstaand(30)` → 20≤30 → `Openstaand(10)` → 14≤10 onwaar → `Geschonden` → absorberend | **`Geschonden`** |
| case3 lege reeks | `[]` | lijst aanwezig, geen elementen → basis = init | **`Openstaand(30)`** |
| case4 niet gemeld | `[Dagverloop(31)]` | 31≤30 onwaar | **`Geschonden`** |
| case5 grensdag nog open (fixronde 1) | `[Dagverloop(30)]` | 30≤30 WAAR → `Openstaand(30-30)` | **`Openstaand(0)`** |
| case6 grensdag gemeld (fixronde 1) | `[Dagverloop(30), Melding]` | → `Openstaand(0)` → melding | **`Vervuld`** |
| case7 afgetelde termijn (fixronde 1) | `[Dagverloop(12)]` | 12≤30 → `Openstaand(30-12)` | **`Openstaand(18)`** |
| case8 kwijting absorbeert (fixronde 1) | `[Melding, Dagverloop(31)]` | → `Vervuld` → buitenste `Vervuld`-arm | **`Vervuld`** |

### Metingen per executor (echte uitvoer)

| casus | verwacht | ref (gate 5) | clingo (gate 6) | Drools (gate 12) |
|---|---|---|---|---|
| case1 | `Vervuld` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case2 | `Geschonden` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case3 | `Openstaand(30)` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case4 | `Geschonden` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case5 | `Openstaand(0)` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case6 | `Vervuld` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case7 | `Openstaand(18)` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case8 | `Vervuld` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |

Drools, letterlijk: `EXCLUDE loader: form not carried by the v0 Drools
backend: Datatype:SumType (unknown datatype node)` — gegroepeerd in het
gate-rapport (de `Datatype:SumType`-groep ging van 5x naar 6x). Niets
toegevoegd aan de waarde-divergentielijst; `scripts/` ongemoeid.

Alle twaalf gates groen, in deze checkout gedraaid (getallen NA fixronde 1,
dus met acht casussen):
gate 1 `check-self-contained: PASS`;
gate 2 `check-ir-schema: PASS (schema valid; 169 file(s) validate and round-trip)`;
gate 3 `check-verifier: PASS (169 golden clean; 206 negatief rejected for their declared rule)`;
gate 4 `check-case-schema: PASS (schema valid; 415 case file(s) validate)`;
gate 5 `check-oracle: PASS (393 cases replayed exactly; …)`;
gate 6 `check-clingo: PASS (393 cases replay exactly through clingo — ref = clingo over the corpus)`;
gate 7 `check-noop: PASS (357 cases refute a do-nothing executor; 36 pinned survivors …)` — de ACHT casussen falen alle acht onder `--noop` (exit 1), geen survivor-regel nodig;
gate 8 `check-rood: PASS (22 red cases fault observably in ref and clingo)`;
gate 9 `check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) …`;
gate 10 `check-regelspraak: PASS — JUnit suite green (159 tests); 26 rgs golden(s) …`;
gate 11 `check-toka-parity: PASS`;
gate 12 `check-drools: PASS — played 104, auto-excluded 41, value-divergence 1, JUnit 153 tests`
(de `Datatype:SumType`-groep blijft 6x: de weigering is per golden, niet per casus, dus vier
casussen erbij veranderen het rapport niet).

### De geraakte grens

Zoals voorspeld in (b): **IDENTITEIT**, en niets anders. De hele
toestandsmachine — open positie met termijn, aftellen, kwijting,
schending, absorberende eindtoestanden, de lege reeks als basis — draagt
de gelande kern zonder één uitbreiding, exact, door beide executors. Wat
zij níet draagt is WIE. WF-060 laat als somtype-lading alleen een getal,
een dagpreciezige datum of een gedeclareerd somtype toe, en WF-071 voor
structvelden diezelfde verzameling plus structs; geen van beide laat een
objectverwijzing toe. De partijen van een verplichting kunnen dus niet ín
de positiewaarde zitten. Daarom is hier de houder het REGELSUBJECT (de
Jutter waaraan het attribuut hangt) en de wederpartij per regel vast,
nergens in de waarde genoemd. De grens toont zich hier een tweede keer
van de andere kant: de gebeurtenisreeks is een documentparameter, dus de
positie is per subject geïndexeerd terwijl haar bewijs helemaal niet
geïndexeerd is. Onuitdrukbaar blijft: één verplichting per partijenpaar,
meerdere zulke verplichtingen tegelijk op één houder, elk met een eigen
termijn.

### Wat dit bewijs doet met de vier posten van §5

- **`PositionType` — WAARSCHIJNLIJKER**, en van de vier het enige dat dit
  bewijs echt draagt. Niet om zijn `conduct`/`deadline`-velden (die deed
  de fold met de hand), maar om `holder`/`counterparty`. Let op de
  PRECIEZE diagnose (aangescherpt in fixronde 1): het is niet zo dat
  WF-060/WF-071 een hek te smal trekken rond een bestaande waardesoort —
  de `datatype`-unie van het schema telt elf leden en een
  OBJECTVERWIJZING zit er niet bij, nergens. `holder`/`counterparty`
  vragen dus een NIEUWE datatypesoort in de kern, en pas dáárna wordt
  toelating als lading of veld een vraag, en pas dáárna moet de
  acycliciteit die de termdiepte voor de ASP-codering begrenst opnieuw
  worden beargumenteerd over een graaf die instanties raakt. Dat maakt
  de post niet minder waarschijnlijk maar wél duurder dan een
  hekverbreding.
- **`ActType` — GESPLITST.** Zijn `actor`/`recipient`-helft wordt
  waarschijnlijker om dezelfde identiteitsreden (de reeks kon niet per
  partij worden geïndexeerd); zijn `effects`-helft
  (`initiates`/`terminates`) wordt ONWAARSCHIJNLIJKER: de absorberende
  armen deden dat hier met bestaande knopen.
- **Schending als gedefinieerde semantiek — ONWAARSCHIJNLIJKER als
  KNOOP.** De schendingstoestand is hier gewoon een constructorwaarde die
  beide executors afleiden en die een casus absoluut pint. Wat overblijft
  is hoogstens één normatieve clausule met WF-nummer over bestaande
  vormen, geen nieuwe knoop.
- **Reparatie — ONWAARSCHIJNLIJKER als apart mechanisme.** Zij was al "een
  creatieregel die het schendingsfeit leest", en dat lezen is een gewone
  somtype-attribuutlezing; die vorm speelt aantoonbaar.

## Fixronde 1 (lensronde)

De lens vond de golden inhoudelijk correct maar ONDERGEPIND: van zeven
mutaties overleefden er drie álle vier casussen in BEIDE executors, en het
waren precies de drie dragende keuzes. Ik heb die mutatiematrix zelf
opnieuw gedraaid in een gesanctioneerde `git archive`-kopie (`/tmp/K`,
eigen `dune build`), met de vier nieuwe casussen erbij. `P` = casus
speelt door onder de mutant (mutant overleeft), `F` = casus doodt de
mutant; links `ref`, rechts clingo, en die twee waren in elke cel gelijk:

| mutant | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| M1 `KLEINER_OF_GELIJK`→`KLEINER` | P | P | P | P | **F** | **F** | P | P |
| M2 `MIN`-operanden omgewisseld | P | P | P | P | P | P | **F** | P |
| M3 buitenste `Vervuld`-arm→`Geschonden` | P | P | P | P | P | P | P | **F** |
| controle init `30`→`29` | P | P | **F** | P | **F** | **F** | **F** | P |
| controle `MIN`→`PLUS` | P | **F** | P | P | **F** | P | **F** | P |
| controle `Geschonden`-arm→`Vervuld` | P | **F** | P | P | P | P | P | P |

De drie controlemutanten werden ook vóór fixronde 1 al gedood, dus het
harnas werkte; de drie overlevers waren echte pin-gaten. Ná fixronde 1
wordt elk van de zes gedood, en elke overlever heeft precies één moordenaar
— de casussen 5–8 zijn geen ballast maar dragen elk een eigen argument.

### Verwerking per bevinding

- **M1-GRENSDAG-ONGEPIND — GEFIXT.** Twee casussen erbij, want het zijn
  twee verschillende uitspraken: `case5-grensdag-nog-open`
  (`[Dagverloop(30)]` → `Openstaand(0)`) pint dat het OPRAKEN van de
  termijn op zichzelf niet schendt — de positie blijft open met nul dagen —
  en `case6-grensdag-gemeld` (`[Dagverloop(30), Melding]` → `Vervuld`) pint
  dat een melding op de LAATSTE termijndag nog kwijt. Beide vooraf met de
  hand gerekend uit `dagen <= resterend`, daarna door `ref` bevestigd
  (`oracle: PASS (2 expectations)`), daarna door clingo. De NOTE zegt nu
  welke kant van dag 30 telt.
- **M2-AFTELLING-ONGEPIND — GEFIXT.** `case7-afgetelde-termijn`
  (`[Dagverloop(12)]` → `Openstaand(18)`) is de eerste casus die een
  GEVOUWEN `Openstaand` pint in plaats van de init. De lensdiagnose klopt
  letterlijk: de NOTE-tabel voerde `Openstaand(18)` en `Openstaand(10)` op
  als hand-gerekende absoluutwaarden terwijl geen executor ze ooit tegen
  een verwachting legde. Nu wel — voor `Openstaand(18)`; `Openstaand(10)`
  blijft een tussenstap van case2 en wordt door case7's zusterrekening
  gedekt, niet apart gepind.
- **M3-VERVULD-ARM-NOOIT-UITGEVOERD — GEFIXT.**
  `case8-kwijting-absorbeert` (`[Melding, Dagverloop(31)]` → `Vervuld`) is
  de eerste casus die de buitenste `Vervuld`-arm binnengaat. Daarmee is de
  absorptieclaim in de NOTE tweezijdig waargemaakt in plaats van half: een
  schending overleeft een late melding (case2) én een kwijting overleeft
  het verlopen van de termijn (case8). Dat tweede is juridisch de
  niet-herbewapening.
- **IDENTITEITSGRENS-VERKEERD-TOEGESCHREVEN — GEFIXT (tekst).** Nagemeten:
  de `datatype`-unie in `spec/schema/regelspraak-ir-v0.schema.json` telt
  elf leden (Numeriek, Percentage, Tekst, Booleaan, DatumInDagen,
  DatumTijd, Enumeratie, Onbekend, SumType, ListType, StructType) en een
  objectverwijzing zit er niet bij. WF-060/WF-071 sommen dus de verzameling
  op die ER IS; ze trekken geen hek te smal rond een bestaande soort. De
  grensparagraaf in de NOTE en het `PositionType`-oordeel hierboven zijn
  daarop herschreven: de kernuitbreiding is een NIEUWE datatypesoort, niet
  een hekverbreding, en de acycliciteitsvraag komt pas daarna en dan over
  een graaf die instanties raakt. `spec/` is niet aangeraakt (protected en
  buiten scope).
- **WF089-EERLIJKHEIDSNOTITIE-VEROUDERD — WEERLEGD ALS "geen defect",
  OPGETEKEND ALS WINST, VERVOLGKAART.** Zelf hermeten in `/tmp/K`: de
  `ParameterRef`-arm van WF-089 onvoorwaardelijk laten vuren (via
  `keuzetype_domain`, die door een `List_type` heen naar het element kijkt),
  `dune build --root spec/verifier`, daarna alle 169 `corpus/golden` +
  `corpus/rood` documenten door de verifier: **precies één wordt rood,
  `corpus/golden/jutterswet-meldplichtspoor/ir.json`.** Deze golden is dus
  de eerste positieve getuige voor die arm. Daarmee zijn twee helften van
  de eerlijkheidsnotitie bij WF-089 in `spec/IR-SPEC.md` achterhaald: (a)
  "no positive witness … measured mutants that make either arm fire
  unconditionally leave gate 3 green" geldt niet langer voor de
  `ParameterRef`-arm (voor de `DimensieCel`-arm nog wél — die blijft zonder
  getuige, en het casusformaat draagt geen celinvoer), en (b) "a
  keuzetype-typed parameter cannot be added to a golden today because the
  ASP carrier refuses the form ahead of translation" is onwaar: clingo
  speelt alle acht casussen exact. De verifier is in de werkkopie NIET
  gewijzigd (mutatie leefde alleen in `/tmp/K`). De spec-tekstwijziging
  vraagt een eigen kaart — zie `backlog/new/`.
- **VOUWRICHTING-OP-EEN-CASUS — GEEN DEFECT, tekst toegevoegd.** Nagemeten
  met sondecasussen, beide executors gelijk: case2 omgekeerd
  (`[Melding, Dagverloop(14), Dagverloop(20)]`) levert `Vervuld` waar case2
  `Geschonden` pint; case8 omgekeerd (`[Dagverloop(31), Melding]`) levert
  `Geschonden` waar case8 `Vervuld` pint. De linksvouw (RUNTIME-SEMANTICS
  ch. 10.2, `fold(f, init, [x1..xn]) = f(...f(f(init,x1),x2)...,xn)`) hangt
  daarmee niet langer aan één casus maar aan twee, van beide kanten. In de
  NOTE vastgelegd. En case1 heeft na fixronde 1 alsnog een eigen taak: zijn
  tussenstap `Openstaand(18)` is nu apart gepind door case7.
- **ZONE-EXTERNE-NAAM-IN-NOTE — GEFIXT.** De twee vergelijkingsmapnamen
  zijn uit de NOTE-inleiding verdwenen; de neutrale omschrijvingen stonden
  er al en dragen de vergelijking prima ("een somtype-accumulator over een
  objectattribuut van numerieke elementen" en "een numerieke accumulator
  over een lijstparameter"). Ik heb ze allebei laten vallen, niet alleen
  de naam met de externe taalnaam erin: het scheelt een grensvraag, en de
  verscherpte kaartregel vraagt hier geen herkomst. De ENIGE in-repo
  verwijzing die blijft is `corpus/negatief/wf060-text-payload` — die is
  geen herkomst maar een gepinde tegenhanger, en zulke verwijzingen staan
  in elke NOTE. De executor- en gatenamen (`ref`, clingo, Drools, gate 12)
  blijven eveneens: dat is de gate-set van deze repo, die de
  kaartacceptatie uitdrukkelijk in de NOTE vraagt, geen herkomst.

Niets in `scripts/`, `spec/`, `ir.json` of enig testbestand gewijzigd: de
fixronde is vier casusbestanden plus prose. De vier nieuwe verwachtingen
zijn EERST met de hand gerekend, daarna door `ref` bevestigd, en pas daarna
door clingo — geen pin is aangepast om een executor te laten slagen.
