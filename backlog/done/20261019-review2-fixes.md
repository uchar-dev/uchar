# review2-fixes

*Op verzoek van de eigenaar (2026-10-19), na de tweede eindreview van het programma "Engelse namen" (bevinding M3, M5, M6, L9, L10 en de punten 1, 5, 10, 11, 12 van de vorige review).*

Goal: de namen die het hernoemen nog miste zijn Engels, en documentatie en commentaar noemen alleen namen die bestaan. Er verandert geen gedrag.

## Punten

1. Gemiste identifiers in Java, OCaml, Python en shell (`huidigSubject`, `oud`, `kern`, `neem`, `huidig`, `noteer*`, `opgegeven`, `category` als parameter van een `Kind`, de Python van gate 13, `tekst` in de grammatica), met een kleine scan naar meer.
2. Testdumps die Nederlandse labels schrijven worden Engels; de pinnende verwachting volgt.
3. Documentatie en commentaar: Drools-README, ASP-predicaten `role`/`inputrole`, `{@code}` naar `{@link}` of de juiste naam, verwijzingen Java naar OCaml, CONSTRUCTS.
4. `Inconsistentie` en `Segment` in "Not renamed", javadoc en `toString` van de generator noemen de echte klasse.
5. `.git-blame-ignore-revs`: "wordt", geen pad buiten de repository.
6. De naamregel vindbaar in `README.md`; Conventions aangevuld.

## Acceptance

- Frontend en Drools compileren (main, test, source-route), suites groen, alle gates groen op de eindstand.

## Result

Gebouwd op main `bf7c9b9f7`, branch `card/review2-fixes`; niet gepusht. Commits: hernoeming `7b39490b4` en vervolg `75913ad97` (alleen namen en
labels, beide in `.git-blame-ignore-revs`), documentatie `60ae7b2c1`, `Inconsistentie` en Conventions `e0049aecd`, blame-lijst en README
`c917c08ae`. De distributie las geen van de hernoemde publieke leden (grep buiten haar `uchar/`: `Allocation.bereken`, `Typing.UnairePred`, de
componenten `gewogen` en `aandeel` van `Consequence`, `huidigSubject`, de property `uchar.vuurspoor`), dus geen `MAPPING.tsv`.
Gereedschap (niet in de repo): `werk/engelse-namen/na2/uchar/tool/` (`scan.py`, `ids.py`, `rename_java.py`, `plan_labels.py`, `xref.py`, `codetags.py`).

**1. Gemiste identifiers.** Alle genoemde namen weg: `huidigSubject` (119), `oud` (15), `kern` (11), `neem` (7), `huidig`, `noteerType`/`noteer`
(16), `opgegeven`, en uit de scan van Nederlandse subwoorden in Java, OCaml, Python en shell (848 niet-Engelse subwoorden in main en 744 in tests, met de hand
gelezen; OCaml was schoon): `kandidaat` (48), `kleur`, `opvolgers`, `verdeler`, `gescoped`, `UnairePred`, `LEEG` (`EffectiveUnit`), `aandeel`
(27), `gewogen` (24), `gerechtigd`, `gewichten`, `bereken`, `optellen` (12), `stapels`, `geschonden`, `nieuwe`, `enkelvoudig`, `probeer`/`vangst`,
`reeksen`, `inlineMeldingen`, `verifierzin`, `inhoud`, `gelift`, `posten`, en in een test `houder` en `ja`/`nee`. `category` als variabele of
parameter van een Kind-type werd `kind`, `originKind`, `predicateKind`, `ruleKind`, `aggregationKind`, `domainKind` of `formKind` (11 plekken, waar
`kind` al bestond een eigen naam). `String tekst` in `RegelSpraak.g4:49` is een gewone variabele in een `@members`-blok, geen parserregel: `text`. Gate 13:
de drie Python-heredocs (45 namen, de sleutels van de teller en het achtervoegsel `-verdraaid`, dus `-twisted`) en `n_gecontroleerd`; de twister gaf op
60 goldens dezelfde tellingen als voor de hernoeming. Bewust gelaten: de runtime (`Breuk`, `Accessors.leesEigenschap`/`eigenschapGetter`/
`eigenschapSetter`, `rs*`, `gelijkWaarde`), `Namen`, `Period.getWaarde` en de getters van `Segment`, de DRL-bindingen in `FormsPlan` en de gegenereerde
tekst, `Modelplan`, `Coverage.category` en `Binding.category` (een tekst en een `Class`, geen Kind), de v0-spelling van enumconstanten (`LEEG`,
`GEVULD`, `TOTAAL`) en de v0-sleutels in de migratietests van `check-ir-schema.sh`.

**2. Testdumps.** De sessiedump van de Drools-backend (`SessionDump`, main) schrijft Engelse labels (`input`, `firings`, `cap`, `noFixpoint`,
`fixpoint yes|no|throws`, `trace-count`, `trace-order`, `read value|characteristic|link|fault|inconsistency`, `facts`, `fact`, `empty`, `(characteristic)`)
en de property heet `uchar.fireTrace`; de vier pins in `src/test/resources/session-dump/` zijn met `-Duchar.dumppin` opnieuw gemaakt (de volgorde van de
`read`-regels veranderde met de labels). De plandumps van 14 tests in `transpiler/tms` en `TermTree` zijn Engels (`rule`, `form`, `group`, `readings`,
`consequence`, `Contribution`, `FilterFault`, `MemberFault`, `Read`, `Input`, `Division`, `probe`, `source`, ...; ongeveer 1.800 literals, met een script
op de plek van de labels, en de rode tests als toets). Mechanisch volgden: `BranchDumps`, `BranchDumpsCorpusWitnessTest`, `DrlTextFoldPathWitnessTest`,
`SeamWitnessTest`, `SessionDumpWitnessTest`, `RationalAtRestWitnessTest`, `ThreeSourcesUniquenessWitnessTest`, `FireTrace`, `ChannelPlan`-,
`ConditionTerm`-, `CreationPlan`-, `Defaultsplan`-, `DepositFlagSlot`-, `FoldPlan`-, `LeftSidePlan`-, `RulePlan`-, `StatePlan`-, `TestRole`-,
`TestTerm`- en `ValueTermWitnessTest`, `DrlPropertyWitnessTest` (eigen uitkomstlabels) en voor de hernoemde namen `DefaultsDivision`, `EvaluationYear`,
`LetCluster`, `TmsFault`, `ZeroDivisor*`, `UnitConflict`, `FixedStartPoint`, `ScenarioLowering`, `SpellingTest`, `TypingTest`, `FoldKindsBoundary` en
`ReaderSentenceBoundaryTest`. Bewust Nederlands gebleven: de generieke namen in `FormsPlan` (DRL-tekst), assertieteksten, en `TmsMeasurement` (een
meetrapport, geen dump).

**3. Documentatie.** Drools-README (`kern/` naar `core/`, `insert(build, session, input)`), `rol/3` en `inputrol/3` naar `role/3` en `inputrole/3`
in `RUNTIME-SEMANTICS.md`, `ASP-TRANSLATION.md`, de clingo-README en `translate.ml` (26 regels), `{@code naam}` naar `{@code name}` (98), `{@code category}`
(3), `.waarde`, `houder`, `exactHerschaalFactoren`, `kern.DayKinds`, `Allocation.bereken`; van 127 `{@code Type#lid}` werden er 44 `{@link}` (de rest ziet de
Drools-bouw of doclint niet), de frontend en Drools compileren met `-Xdoclint:reference` schoon. `run_object_creatie` en `run_verdeling` in `IrLoader`
naar `run_object_creation` en `run_allocation`, en dezelfde oude naam in een faalmelding van `ref/lib/eval.ml` en zijn pin in `ref/test`. `CONSTRUCTS.md`: 30
hernoemde namen (testklassen, methoden, `Expr.FunctionKind`), de vijf datumvouwgetuigen naar `corpus/negative/wf091-*`, `wf046-empty-test-*`, `wf053-chain-near-side-mismatch`.
Blijft staan: paden van verwijderde getuigen in de verleden tijd (`wf034-dagsoorteenheid-onbekende-dagsoort`, `wf053-bindingless-ambiguous` in een
rode NOTE, `wf096-setlezing-*`), `rsGeheelPeriodeTelling`, `Nuldelingmelder`, `initEigen`, `ScenarioResolver.regelParameters` (al voor het programma weg) en de namen in
`spec/PLAN.md` (serieel werk van de hoofdsessie).

**4. `Inconsistentie`.** Javadoc, een commentaarregel en `toString` van `FactModelGenerator` noemen de echte klasse (`Inconsistentie[`), met de reden dat de
runtime hem en `Segment(van, tot, waarde)` bij naam bouwt en leest; ook `Fixpoint`, `ContractValidator` en de README. Beide staan in "Not renamed".
**5. Blame-lijst.** "wordt", geen pad buiten de repository, de twee hernoemcommits erbij. **6. Naamregel.** Sectie "Naming" in `README.md`;
Conventions vult aan: `synth*`, `ww-*`, case-bestanden met een label, DRL-bindingen, geërfde gemengde regelnamen, bestaande kaarten in `backlog/new/`.

**Gates.** Frontend 1210 tests, Drools 1624 tests (floor 1573), alle 16 gates groen op `1545cf3e6` (`werk/engelse-namen/na2/uchar/gates-2.summary.txt`);
de eerste run (`gates-1`) was rood op gate 12 door twee tests die de dump lazen en door mijn scan niet gezien waren. Protected paden geraakt, dus een voorstel:
`CLAUDE.md`, `scripts/check-reftype-derivation.sh`, `spec/RUNTIME-SEMANTICS.md`, `spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`, `RegelSpraak.g4` valt onder de frontend.
