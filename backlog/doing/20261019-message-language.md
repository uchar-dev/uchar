# message-language: elke melding in de taal van de catalogus die haar geeft

*Uit de tweede eindreview van het programma "Engelse namen" (2026-10-19, bevinding M4). De eigenaar besloot
2026-10-19 dit als eigen kaart te doen, na dat programma, met de keuzes hieronder.*

Goal: wie de frontend in het Nederlands draait, krijgt Nederlandse meldingen, en wie de Drools-backend of de
distributie in het Engels draait, krijgt Engelse; een melding die een gebruiker te zien krijgt, komt uit een
catalogus en heeft een code.

Gemeten op main (steekproef van de review, bij de start na te tellen):
- `frontends/regelspraak/.../message/messages-frontend.nl.tsv`: ongeveer 60 entries (38 hoofdcodes) zijn gelijk aan
  de Engelse, bijvoorbeeld RGS-295..299 ("date ± date is ill-formed …"), RGS-328 en RGS-344. RGS-200 mengt in één
  zin ("periodevergelijking: window bound is {kind}, must be a date"); RGS-083 is in beide catalogi half Nederlands,
  half Engels. Volgens het Result van `backlog/done/20261019-message-catalogues.md` bestonden er vertalingen voor de 58
  Engelse frontendbronnen.
- De Engelse frontendcatalogus schrijft "objecttype" 24 keer als gewoon woord; RGS-300 "in one EN row" leest als
  "English" (bedoeld: een EN-rij, de AND van een beslistabel).
  *(2026-10-22: RGS-300 vervalt met `third-condition-level`, een rij met meer of-cellen wordt nu geschreven; het punt
  over RGS-300 hier en in besluit 2 vervalt.)*
- `backends/drools/.../message/messages-drools.nl.tsv`: naast 29 Engelse zinnen, die de eigenaar eerder zo liet, zijn
  ook EF-003, EF-007..012 en EF-019 Engels.
- Buiten de catalogus: in `backends/drools/src/main` (zonder de runtime) ongeveer 143 Nederlandse exceptieteksten
  zonder code tegen 38 Engelse, onder meer `cases/DroolsBackend.java` (verkeerde invoer van de publieke API),
  `transpiler/core/UnitConflict.java` en `transpiler/tms/Watch.java`; in de frontend `model/ParameterSet.java` en
  `model/Unit.java`, waarvan de distributie de tekst ook onder `--language en` doorgeeft (`ParameterSet.valuesOn`).

Besluiten van de eigenaar (2026-10-19), in lijn met eerdere besluiten:
1. **Exceptieteksten.** Een tekst die een gebruiker te zien krijgt, krijgt een code (`DROOLS-###` of `RGS-###`, uit
   het register van zijn onderdeel) en een entry in beide catalogi. Dat zijn in elk geval de teksten bij verkeerde
   invoer van de publieke API van de Drools-backend (`cases/DroolsBackend.java`) en de teksten van
   `model/ParameterSet.java` en `model/Unit.java` die de distributie doorgeeft. Een tekst die een interne invariant
   bewaakt (een toestand die bij correcte code niet voorkomt), wordt Engels, zonder code. Grond: kaart `meldingscodes`
   (een melding krijgt een code, los van haar tekst) en de naamregel (alleen wat uit RegelSpraak komt, blijft
   Nederlands).
2. **Frontendcatalogus.** De Engelse entries van `messages-frontend.nl.tsv` worden Nederlands, met de vertalingen uit
   `message-catalogues` als uitgangspunt; RGS-200 en RGS-083 worden in beide catalogi één taal. In de Engelse
   catalogus wordt "objecttype" als gewoon woord "object type" (als geciteerd RegelSpraak-sleutelwoord blijft het),
   en RGS-300 zegt "AND row ('EN')". Grond: de frontend is standaard Nederlands (besluit bij stap 7 van het
   programma), en een melding is helemaal in één taal, ook haar parameterwaarden (kaart `catalogue-gaps`).
3. **Drools-catalogus.** EF-003, EF-007..012 en EF-019 blijven in `messages-drools.nl.tsv` Engels, net als de 29
   zinnen die de eigenaar eerder zo liet. Ze staan samen op één uitzonderingslijst met reden. Grond: de
   Drools-backend is standaard Engels, en zijn Nederlandse catalogus dient vooral zijn eigen suite.

Acceptance:
- `scripts/check-rgs-codes.sh` en `scripts/check-drools-codes.sh` weigeren een entry van de Nederlandse catalogus die
  gelijk is aan haar Engelse, behalve een regel van de uitzonderingslijst van besluit 3; een lijstregel die niet meer
  klopt, faalt (stale in beide richtingen); de lijst krijgt een gemeten vloer, zoals elke telling van de gates.
- Elke tekst van besluit 1 die een gebruiker te zien krijgt, heeft een code uit het register en een entry in beide
  catalogi; de registercontroles van gate 10 en 12 houden dat zoals nu.
- Geen Nederlandse exceptietekst zonder code meer in `backends/drools/src/main` buiten de runtime, en geen in
  `frontends/regelspraak/src/main/java` buiten de catalogus; een grep in de Result laat het zien.
- Testbestanden die een veranderde tekst pinnen, volgen mechanisch; geen verwachting over gedrag verandert.
- Gate set: alle 16. De distributie volgt in een eigen kaart met haar pin: onder `--language en` geeft zij geen
  Nederlandse tekst van uchar meer door, en `make gates` is groen.

May touch: `frontends/regelspraak`, `backends/drools` (niet de productieruntime), `scripts/check-rgs-codes.sh`,
`scripts/check-drools-codes.sh` en `spec/proposals/MESSAGE-CODES.md` §6. De tekst in `CLAUDE.md` over gate 10 en 12
stelt de kaart voor in haar Result; de hoofdsessie zet hem erin.

## Result (2026-11-06, branch `card/frontend-residue`)

Gemeten op `575de08e5` (main) en de branch; logs en hulpscripts onder `werk/frontend-residue/` (`logs/`, `k2/`).

**Acceptatie 1, de gates.**
- `scripts/check-rgs-codes.sh` (gate 10): een Nederlandse entry die gelijk is aan haar Engelse zus faalt, tenzij ze
  buiten haar plaatsen en citaten geen woord heeft (dan leest ze in beide talen hetzelfde) of `SAME_IN_BOTH` haar met
  reden noemt: `FRAME.scenario`, `PART.kind.percentage`, `PART.clamp.minimum`, `PART.clamp.maximum` (het Nederlandse woord
  is hetzelfde); vloer `EXPECTED_MIN_RGS_SAME_IN_BOTH=4`. Technische keuze bovenop de kaart: de spiegel van de bestaande
  controle op Nederlandse functiewoorden in Engelse entries, dus een Nederlandse entry met een Engels functiewoord faalt,
  tenzij `ENGLISH_EXCEPTIONS` (leeg) entry en woord noemt; zonder die spiegel bleven 47 gemengde entries ("Nederlandse
  aanloop: Engelse zin van de verifier") onzichtbaar. Beide lijsten falen als ze verouderd zijn. Rood op de catalogus van
  `575de08e5`: 63 gelijke entries en 205 Engelse woorden (`logs/k2-red-rgs-codes.log`); groen na.
- `scripts/check-drools-codes.sh` (gate 12): dezelfde gelijkheidstoets met `SAME_IN_BOTH` van 49 regels, vloer
  `EXPECTED_MIN_DROOLS_SAME_IN_BOTH=49`: de zinnen van EF-003, EF-007..012 en EF-019 (besluit 3) en die van EF-032, die er
  na het besluit bij kwam en dezelfde grond heeft (`ComputeFault.namesNoMoment` leest de zin terug, `FilterFaultWitnessTest`
  pint hem); de 29 zinnen van `message-catalogues` voor zover nog gelijk (28 codes met de zes delen van DROOLS-049;
  DROOLS-038 is inmiddels vertaald, en DROOLS-076, -157 en -289 hebben geen woord buiten hun plaatsen); en
  `FRAME.contract`, `.drl`, `.loader`, `.transpiler-verdict` (namen van onderdelen van de backend). De Drools-catalogi zijn
  niet bewerkt: alles wat gelijk was, valt onder het besluit. Rood getoond op een tijdelijk gemuteerde catalogus
  (`logs/k2-red-drools-codes.log`: een Engelse zin in DROOLS-001 en een verouderde lijstregel EF-032.day), groen op de echte.

**Besluit 2, de frontendcatalogus.** 110 Nederlandse entries vertaald (`k2/apply_nl.py`): de 63 die gelijk waren aan
de Engelse en 47 gemengde. RGS-200 en RGS-083 zijn in beide catalogi één taal (het Engelse RGS-083 citeert het
RegelSpraak-sleutelwoord tussen aanhalingstekens; de twee regels van `DUTCH_EXCEPTIONS` voor RGS-083 vervielen). In de
Engelse catalogus werd "objecttype" als gewoon woord 37 keer "object type" (het getal 24 van de kaart was oud); het
geciteerde `<Objecttype>` bleef. RGS-300 bestaat niet meer. Parameterwaarden die een woord van de zin zijn, zijn delen van
de catalogus geworden waarvan de Engelse entry het woord van de verifier is, dus de Engelse weergave is ongewijzigd:
`PART.kind.*` (de soortwoorden, `Typing.kindWord`), `PART.clamp.*` (basis, minimum, maximum), `PART.sign.*`
(tekenbeperking) en `PART.day-kind-read.*` (RGS-318). Namen van de wire (een lijst als `rules`) en het "geen" van de
verifier blijven staan.

**Besluit 1, exceptieteksten, frontend (acceptatie 2 en 3).** Met code en entry in beide catalogi: RGS-380 en RGS-381
(`ParameterSet.overlapErrors`, `valuesOn`), RGS-382..385 (`Unit`: breuk met noemer 0, onbekende omrekenfactor, geen
gedeclareerde eenheid, niet inwisselbaar) en RGS-386, RGS-387 (de invoercontroles van de API van de semantic tokens:
`Resolver.tokens`, `SemanticTokens.SourceFile`). Hun Nederlandse tekst is gelijk aan de oude, omdat de Drools-suite onder
`nl` de tekst van `Unit` pint (`UnitConflictWitnessTest`) en de distributie die van `ParameterSet`; `ParameterSet` houdt
zijn `List<String>`-API, de tekst komt in de taal van de catalogus. De plek geeft code en parameters als
`new Msg(Component.FRONTEND, code, params(...))`; `check-rgs-codes.sh` kent die vorm nu. De andere 83 exceptieteksten
(IrEmitter en zijn weigeringsredenen, de JSON-lezer van `Dependencies`, RuleChecker, Designations, ScenarioResolver,
SemanticTokens, Typing, DefinitionResolver, `RegelSpraakModel.copy`, CaseEmitter) bewaken een interne toestand en zijn
Engels zonder code (`k2/apply_java.py`). Grep (`k2/grep-dutch-exceptions.py`): 0 Nederlandse exceptieteksten in
`frontends/regelspraak/src/main/java` (`logs/k2-grep-frontend.log`).

**Besluit 1, Drools-kant: niet gedaan.** `backends/drools/src/main/java` zonder de runtime draagt 269 exceptieteksten met
Nederlandse woorden (`logs/k2-grep-drools.log`), waarvan 30 in `cases/DroolsBackend.java`, de publieke API die het besluit
noemt. Het mandaat van deze branch sloot de Java-code van `backends/drools` uit, omdat een andere bouwer daar tegelijk
werkt. Acceptatie 2 en 3 zijn voor de Drools-kant dus open; de kaart blijft daarvoor open.

**Acceptatie 4, tests.**
- `resolver/ReaderSentenceBoundaryTest` is inhoudelijk aangepast, en die wijziging vraagt de blik van de eigenaar: de
  zinnen van de verifier staan niet meer in de Nederlandse catalogus maar in de Engelse, dus de test leest nu de Engelse
  entries van 104 codes (`VERIFIER_COPIES`, per code het aantal contextsegmenten vóór de zin van de verifier) en slaat
  exceptieteksten en catalogussleutels in de Java-literals over; `WITHOUT_READER` verviel, want zijn twee regels waren de
  tekst van een exceptie. De garantie (een zin van de verifier in een melding van de frontend drijft niet af) en de vloer
  van 149 fragmenten blijven.
- 39 andere testbestanden van de frontend en `toka/src/test/java/.../CasusTest.java` volgen mechanisch: alleen
  stringliterals veranderden (gecontroleerd: de code zonder literals is gelijk aan `HEAD`); geen verwachting over gedrag.
- Frontendsuite: 1527 tests, 0 failures (`logs/k2-test-5.log`).

**Gate 11.** De `diagnostics` van de wire dragen de tekst in de standaardtaal, dus één WARNING in `toka/casus/ir.json`
(de begrenzing van toka-24) is nu Nederlands; opnieuw gepind en in `toka/README.md` onder "Deliberate divergences".

**Gate set.** Gericht groen: 1, 10, 11, 16 en `check-drools-codes.sh` (`logs/k2-gates-2.log`); de hele set aan het eind van
de branch. De distributie volgt met haar pin (onder `--language en` geeft zij dan Engelse frontendmeldingen; haar
`ParameterSet`-pins onder `nl` blijven gelijk).

**Voorstel voor CLAUDE.md** (de hoofdsessie zet het erin):
- gate 10, na de zin over de catalogi: "Since the card `message-language` (2026-11-06) the same script holds the Dutch
  catalogue Dutch: a Dutch entry equal to its English sister fails unless it has no word outside its places and
  quotations or `SAME_IN_BOTH` lists it with the reason (floor `EXPECTED_MIN_RGS_SAME_IN_BOTH=4`), and a Dutch entry that
  carries an English function word fails unless `ENGLISH_EXCEPTIONS` lists entry and word; both lists fail when stale. A
  site may pass its code as `new Msg(Component.FRONTEND, code, params(...))`, the form of an exception text a user sees
  (RGS-380..387)."
- gate 12, na de zin over de Drools-catalogi: "Since the card `message-language` (2026-11-06) a Dutch entry equal to its
  English sister fails unless it has no word outside its places and quotations or `SAME_IN_BOTH` lists it with the
  reason: the sentences of the fault kinds that the runtime writes and `ComputeFault` reads back (EF-003, EF-007..012,
  EF-019, EF-032), the 29 sentences left English at the card `message-catalogues` and the names of the backend's parts
  (floor `EXPECTED_MIN_DROOLS_SAME_IN_BOTH=49`)."

`spec/proposals/MESSAGE-CODES.md` §6 kreeg de twee besluiten in hun huidige stand.

Open punten: geen normvraag. Open werk: de Drools-kant van besluit 1 (zie boven).
