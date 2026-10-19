# zero-division-channel: delen door een numerieke nul geeft leeg plus een §9.5-feit

*Uitvoering van een besluit dat al in de spec staat (RUNTIME-SEMANTICS ch. 1.4 punt 4: "Decided: v1 routes
numeric-zero division into the same inconsistency channel"). Gemeten bij kaart `channel-fact-codes` (2026-10-19),
die de verhuizing bewust niet deed. De eigenaar besloot 2026-10-19: een eigen kaart, later uit te voeren; tot dan
blijft delen door nul een fout met code EF-005.*

Goal: een deling van een gevuld deeltal door een numerieke nul geeft leeg plus een §9.5-feit met code EF-005, in
plaats van een harde fout; zo gedraagt delen door leeg en de wortel uit een negatief getal zich al. De code blijft;
het kanaal van EF-005 in de tabel van ch. 5.7b wordt §9.5, en de getuigen gaan van `corpus/red` naar golden
`inconsistent`-verwachtingen. De v0-fout en de weigering van een letterlijke `1/0` bij het ontleden blijven het
spiegelbeeld van de productieruntime.

Wat het raakt (gemeten op main 0ba90bff):
- ref: `Arith.div_gen`; `ref/test/test_ref.ml` 6 regels (onder meer `expect_fault "ch2.2-div-zero"`, de Let-rijen
  i/ii van ch. 10.6).
- clingo: de `viol/2`-wachters van de deling in `translate.ml`; `backends/clingo/test/test_clingo.ml` 29 regels.
- Drools: `ComputeHelpers.divided`/`dividedAbs` en de klassificatie in `CaseRunner.faultCode`; 23 testbestanden,
  72 regels (onder meer `ZeroDivisorWitnessTest`, `ZeroDivisorConditionWitnessTest`,
  `ZeroDivisorConsumerWitnessTest`, `TmsFaultWitnessTest`).
- RUNTIME-SEMANTICS: de uitgewerkte voorbeelden die de fout pinnen (ch. 3.4c besluit G1, ch. 5.10, ch. 5.11
  `begrenswaarde`, ch. 10.6 rij ii) en ch. 2.2, ch. 1.4 punt 4, ch. 5.7b.
- Corpus: 8 rode getuigen van EF-005. Zes kan clingo als golden spelen; twee niet:
  `clamp-reads-bounds-in-order` (clingo weigert de Clamp met een niet-grondig minimum) en
  `sum-fallback-always-evaluated` (clingo vertaalt een som over losse operanden niet). Gate 6 eist dat clingo elke
  golden speelt.

Eerst te beslissen door de eigenaar: de twee getuigen die clingo niet draagt (schrappen met een verlaagde vloer, of
eerst een clingo-kaart voor die vormen); en de testverwachtingen in de drie suites en de uitgewerkte voorbeelden
mogen veranderen (gedragswijziging). Gevolg voor de distributie: haar gebruikers krijgen bij delen door nul een leeg
resultaat met een melding in plaats van een fout; de pin-bump noemt dat.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-19)

Opdracht van de eigenaar 2026-10-19: uitvoeren, met de testverwachtingen die daardoor veranderen. Besluit van de
hoofdsessie over de twee getuigen die clingo niet als golden speelt: rood houden met een andere harde fout die in v1
blijft en dezelfde volgorde zichtbaar maakt.

Commits op `card/zero-division-channel`:
- `c4ffe9ba` — de kaart naar `doing/`.
- `5f8b2984` — ref en clingo: deling door een numerieke nul geeft leeg en een §9.5-feit EF-005.
- `66a1181e` — Drools: idem, met de codes van een deponering in voorwaarde en vouwfilter uit de evaluatie.
- `37d8214b` — corpus, spec en gates.
- de commit met dit Result.

Uitvoering:
- ref: `Arith.div_gen` geeft bij een gevuld deeltal en een numerieke nul `Empty` en `err {code = EF-005}`; een leeg
  deeltal blijft 0.
- clingo: `translate_division` schrijft in plaats van de `viol/2`-wachter de deponering `inconsistent/2` +
  `inconsistent_code/3` met EF-005; `viol_code` kent geen EF-005-arm meer. In een gesloten body (`closed_division`) geeft
  een nuldeler onder plus, min, maal of percentage-van 0 (`Z = 1/(|t|+1)`) met de deponering op de lezende regel ×
  subject; elders in een gesloten body een benoemde `viol/2`-grens (README [V4]). Nieuw veld `deposit_subject`: een
  deponering in een vouwfilter, een kwantorelement of een gesloten body sleutelt op het onderwerp van de lezende vorm,
  zoals ref. Dat was voor EF-031 al een stille afwijking (clingo sleutelde op het lid, gemeten op
  `fold-filter-divides-by-zero` met een lege deler: `drukte × V2`); de golden had het nodig.
- Drools: `ComputeHelpers.divided`/`dividedAbs` beantwoorden de nul vóór `rsGedeeld` (de runtime faalt daar nog, het
  spiegelbeeld van v0). De codes van een deponering in de linkerkant stonden vast in het plan (altijd EF-031); nu
  evalueert het gevolg de voorwaarde opnieuw met een sonde (`Consequence.ConditionDeposit`, nieuw), draagt
  `FilterDeposit` de codes die zijn gevolg hoort, en rapporteert `Term.FoldRead` die codes uit een LIST-accumulate
  (`Route.REPORT_FILTER_DEPOSITS`, nieuw); `Term.FoldRead.codes` en `Fold.depositCodes` vervielen. `CaseRunner.addKind`
  klasseert `DelingDoorNul` niet meer als EF-005. `StaticFunction.DIVIDED` telt nog als falend (gedocumenteerd): de
  plannen van elke vorm met een deling bouwen daarop; omzetten is een eigen kaart.
- De letterlijke `1/0` blijft bij het ontleden geweigerd (frontend, WF-081, `TmsTranspiler.literal`): niet aangeraakt.

Spec-plekken:
- RUNTIME-SEMANTICS ch. 1.4 punt 4 (uitgevoerd, met wat de getuigen werden); ch. 2.2 (uitgewerkt voorbeeld: 5 ÷ 0 leeg
  met één deponering EF-005, 0 ÷ 0 idem, leeg ÷ 0 = 0 zonder; `ref/test` `ch2.2-div-zero`, `-by-zero`,
  `-empty-by-zero`); ch. 3.4c besluit G1 (0 ÷ 0 leeg met het feit, uitgewerkt op de golden); ch. 5.6 (de nuldeler in de
  lijst van producenten); ch. 5.7b (intro-voorbeeld, kanalen, rapportvoorbeeld EF-011/EF-016, parameterzin, de rij
  EF-005 op kanaal §9.5 met `rule`, `label` en de zes golden getuigen, het uitgewerkte voorbeeld nu
  `clamp-reads-bounds-in-order` met EF-011); ch. 5.10 (vacuüm-zin, rij 1 en de artefacttabel: een stop is zichtbaar als
  het feit dat niet ontstaat, de golden pint `consistent`; `at-most-reads-until-settled` als de andere richting);
  ch. 5.11 (het `Clamp`-voorbeeld met het rekenjaar); ch. 10.1 (een deponerende bewerking kan in een gesloten body leeg
  geven); ch. 10.6 rijen i en ii (42 zonder reden; leeg met één reden EF-005).
- IR-SPEC: geen plek noemt de deling door een numerieke nul als semantiek; alleen de weigering van een letterlijke noemer
  0 (WF-081, historie), ongewijzigd.
- Daarnaast, omdat hun zinnen anders onwaar werden: `spec/ASP-TRANSLATION.md` (S6, de deling, de gesloten body, het
  codevoorbeeld), `backends/clingo/README.md` ([V4]), `backends/drools/README.md` (fouten, "Recorded: a division by a
  numeric zero"), de javadoc van `Breuk.DelingDoorNul`, en een gedateerde voetnoot in de NOTE van acht goldens die de
  fout noemden.

Tests (gewijzigde toetsen; regels +/−):
- `ref/test/test_ref.ml` (+84/−35): `ch2.2-div-zero` eist nu leeg en EF-005, plus twee nieuwe voorbeelden (0 ÷ 0,
  leeg ÷ 0); ch. 10.6 rijen i en ii (geen reden; leeg met EF-005); ch. 5.10: `ongepoort-lid-fault-wel` eist het feit
  en tien toetsen die "geen fout" toetsten kregen een kanaaltoets (`unread`); ch. 5.11: de 27 volgordevoorbeelden
  gebruiken als eerste fout het rekenjaar zonder rekendatum (EF-011) in plaats van 1 ÷ 0. 1304 voorbeelden.
- `backends/clingo/test/test_clingo.ml` (+107/−105): 12 `both_break` worden `both_answer` met leeg (of de gecoalesceerde
  waarde) en EF-005; 3 gesloten-body-toetsen worden `pinned_loud ~entry:"V4"` (ref antwoordt, clingo weigert benoemd);
  10 `translates_carrying`-toetsen zoeken de EF-005-deponering of de [V4]-zin in plaats van de `viol`. `both_break`
  verviel; `both_answer` en `pinned_loud` staan hoger in het bestand. 2198 checks.
- Drools (27 bestanden; 72 falende toetsen aangepast, 2 nieuw; 1624 tests):
  - nieuwe uitkomst leeg + EF-005: `ZeroDivisorWitnessTest` 5, `ZeroDivisorConditionWitnessTest` 9,
    `ZeroDivisorConsumerWitnessTest` 1, `LetNeedWitnessTest` 1, `TmsLetWitnessTest` 3, `FoldCompanionWitnessTest` 1,
    `TmsAggregationWitnessTest` 2, `TmsQuantifierFoldWitnessTest` 1, `FilterFaultWitnessTest` 11 (het samengestelde
    "alle" in elke volgorde rekent nu met houdt/houdt niet en deponeert; J4 onderscheidt F vóór K nog steeds),
    `TmsFilterDepositWitnessTest` 1 (één feit met EF-005 en EF-031);
  - vervanging, omdat de toets de foutmachinerie toetste en de deling daar geen fout meer geeft (middel: de wortel zonder
    afronding van een getal dat geen kwadraat is, `TmsDocuments.root`/`irrational`): `TmsFaultWitnessTest` 8,
    `TmsConditionFaultWitnessTest` 3, `TmsFilterFaultWitnessTest` 7, `TmsAggregationWitnessTest` 1, `TmsInputWitnessTest`
    2, `CaseInputWitnessTest` 1, `TmsConsumerWitnessTest` 1, `TmsRepeatedFireWitnessTest` 3, `TmsMemberFaultWitnessTest`
    1, `CompanionPathWitnessTest` 1, `FilterFaultWitnessTest` 2 (het pad; nieuw: een wortel in de matrix van een
    kwantorvouw), `TmsFilterDepositWitnessTest` 1 nieuw (fout en deponering in één filter), `FoldPlanWitnessTest` 1;
  - andere rode case als middel: `ComparisonWitnessTest` 1, `RedBranchChoiceWitnessTest` 1 (beide
    `clamp-reads-bounds-in-order`);
  - plan en vloer: `RulePlanWitnessTest` 1 (`ConditionDeposit`), `DepositFlagSlotWitnessTest` 1, `RouteWitnessTest` 1,
    `DrlRenderingWitnessTest` 1 (via `FormsPlan`), `BranchDumpsCorpusWitnessTest` 1 (rode vloer 50 → 44); helpers in
    `TmsDocuments`, `FormsPlan`, `TermTree`.

Getuigen:
- Golden, met `inconsistent`-verwachting en code EF-005: `division-by-numeric-zero`, `division-abs-by-numeric-zero`,
  `filled-count-unguarded-division`, `at-most-reads-until-settled`, `fold-filter-divides-by-zero` (twee pins) en
  `closed-runtime-division-by-zero`. De eerste vijf halen het vierkant ref ≡ clingo ≡ Drools (beide takken). De zesde
  vouwt over een lijstparameter, die de Drools-backend niet draagt (`EXCLUDE DROOLS-008`, zoals de andere lijstgoldens):
  ref ≡ clingo, Drools in `EXPECTED_CHANNEL`.
- Rood gebleven met het rekenjaar zonder rekendatum (EF-011) op de plek van de deling: `clamp-reads-bounds-in-order`
  (minimum) en `sum-fallback-always-evaluated` (stand-in); `teller`/`noemer` gingen uit model en case. Afwijking van het
  besluit: bij de `Clamp` geeft clingo nu zelf EF-011 (het vertaalt het minimum vóór de grondheidstoets), dus de
  divergentie-entry van clingo moest weg (de verouderingswacht van gate 8 eist dat); bij de som blijft de entry
  (CLINGO-007) met bijgewerkte reden. Drools geeft op de `Clamp` EF-011 als ref.
- De vier stopgetuigen van ch. 5.10 (`jutterswet-ploegaandeel`, `-wachtdeling`, `-seinwacht`, `-vacuumwacht`) pinnen nu
  `consistent`: zonder dat zag geen golden meer dat een gestopt lid ongelezen bleef.

Gates: één volledige run op `37d8214b` (14:20–14:39): alle 16 groen. Gate 5 831 cases, 1304 voorbeelden; gate 6 831
cases, 2198 checks; gate 7 786 cases; gate 8 44 rode cases, 82 codetoetsen, 120 parameters, 80 golden codepins, Drools
21 fouten en 18 EXCLUDE-codes; gate 12 260 gespeeld op beide takken, JUnit 1624; gate 13 en 15 groen. Vloeren gate 8
(gemeten): `EXPECTED_MIN_RED_CASES` 26 → 44, `RED_CODES` 93 → 82, `RED_PARAMS` 131 → 120, `DROOLS_PARAMS` 33 → 28,
`DROOLS_EXCLUDE_CODES` 19 → 18, `GOLDEN_CODES` 73 → 80. Gate 12: `EXPECTED_PLAYED` 255 → 260 (gelijkheid). De
golden-vloeren die ondergrenzen zijn (gate 5 427, gate 2 192, gate 15 234 enz.) liggen ver onder de meting en bleven
staan: groei vraagt geen ophoging.

Open:
- DROOLS-290 (`deposit-kinds-mixed`) is met de codes uit de evaluatie niet meer nodig; intrekken vraagt een
  "retired"-soort in het register (`check-drools-codes.sh` eist dat elke code gebruikt wordt).
- `StaticFunction.DIVIDED` als niet-falend markeren (minder regels in de plannen, mogelijk meer gespeelde vormen).
- clingo [V4]: een nuldeler in een gesloten body waar het leeg niet coalesceert.

Voor de hoofdsessie:
- CONSTRUCTS rij 18, voorstel toe te voegen: "**Landed (2026-10-19, card `zero-division-channel`):** a filled dividend
  divided by a numeric zero is leeg with a §9.5 fact of kind EF-005 in ref, clingo and both Drools branches; the v0
  hard fault (the production runtime) and the parse-time rejection of a literal `1/0` remain the mirror. Witnesses:
  goldens `division-by-numeric-zero`, `division-abs-by-numeric-zero`, `filled-count-unguarded-division`,
  `at-most-reads-until-settled`, `closed-runtime-division-by-zero`, `fold-filter-divides-by-zero`."
- `CLAUDE.md`, gate 8: vervang "`FAULT EF-005 {…}: …`" door "`FAULT EF-016 {…}: …`" en de vloeren door
  "`EXPECTED_MIN_RED_CASES=44`, `EXPECTED_MIN_RED_CODES=82`, `EXPECTED_MIN_RED_PARAMS=120`, `EXPECTED_MIN_EF_CODES=31`,
  `EXPECTED_MIN_GOLDEN_CODES=80`, `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=18`, `EXPECTED_MIN_DROOLS_PARAMS=28`", en voeg toe:
  "Since 2026-10-19 (card `zero-division-channel`) EF-005, a division by a numeric zero, is a §9.5 kind and its
  witnesses are goldens."
- Distributie: haar gebruikers krijgen bij delen door een numerieke nul een leeg resultaat met een §9.5-melding (code
  EF-005) in plaats van een fout; de pin-bump noemt dat.
