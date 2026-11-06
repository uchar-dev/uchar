# uitvoerdernotities-na-truth-maintenance: teksten die de destructieve verlagingen als huidig beschrijven

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14). Voor de
exec-model-tak verouderd sinds `tms-omschakeling` (2026-10-10), voor de DRL-teksttak sinds die landing. Geen fout in
het gedrag: de teksten beschrijven een stand die er niet meer is.*

## Aanleiding

Sinds 2026-10-14 leiden beide Drools-takken af met truth maintenance, als twee weergaven van één analyse. Een afleiding
waarvan een lezing verandert vervalt, en een creatie vuurt pas aan een fixpunt van de regellaag. Verschillende teksten
beschrijven nog het gedrag van de destructieve verlagingen (salience, `no-loop`, schrijven tijdens het vuren) als
huidig:

- `spec/IR-SPEC.md` ch. 5.5 ("Executor neutrality"), de gedateerde notities over de Drools-flank: "A leeg or gevuld
  test outside a fold, and the DRL-text branch, keep the form described here", "The DRL-text branch keeps the form
  described here", "The DRL-text branch refuses such a test by name", en over de creatievolgorde "closed in the
  exec-model branch the same day, open in the DRL-text branch". De exec-model-tak die daar "closed" heet, is de
  destructieve verlaging, die sinds `tms-omschakeling` weg is. Geteld op `722d8fb6`: IR-SPEC noemt de DRL-teksttak 18
  keer en de exec-model-tak 29 keer, RUNTIME-SEMANTICS 6 en 9 keer, ASP-TRANSLATION de DRL-teksttak 1 keer; niet elke
  vermelding is verouderd.
- Javadoc en commentaar in de testbron van `backends/drools`, bijvoorbeeld de klassedoc van
  `TmsWriteBackWitnessTest` (r. 51: "On the DRL-text branch the call writes nothing: its rules write the beans
  while they fire"; de test draait beide takken met dezelfde terugzetting) en `FoldFilterFarSideWitnessTest` r. 33
  ("The DRL-text branch writes such a test into the element pattern").

Wat de code bij de landing liet zien: een genavigeerde lezing in een voorwaarde (`anchor/jutterswet-kustwacht`, dat de
destructieve DRL-teksttak liet falen, en `jutterswet-havenketen`), een toets per element via een rol van het element
(`jutterswet-late-vondst`, `FoldFilterFarSideWitnessTest`) en de creatievolgorde (`TmsCreationWitnessTest`) spelen op
beide takken zoals ref. Een leeg- of gevuldtoets vóór haar schrijver heeft een getuige op de exec-model-tak alleen
(`ReaderUnderWriterWitnessTest`; `NavigatedConditionWitnessTest` evenzo) en geen corpusgolden.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een inventaris van elke zin in `spec/` en in `backends/drools` (bron, testbron, README) die een destructieve
  verlaging als huidig beschrijft, met per zin een voorstel; gedateerde geschiedenis blijft staan.
- (b) Waar een nieuwe zin iets van beide takken zegt, draait een getuige beide takken: `ReaderUnderWriterWitnessTest`
  en `NavigatedConditionWitnessTest` krijgen de DRL-teksttak erbij, of de zin noemt alleen de tak die getuigd is.
- (c) De wijzigingen in `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` en `spec/ASP-TRANSLATION.md` zijn voorstellen
  (beschermd pad), elk in een eigen commit; gate 1 groen, en gate 12 als de suite verandert.

May touch: `backends/drools/**` (javadoc, commentaar, testbron), `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/ASP-TRANSLATION.md` (**voorstel**).

Out of scope: gedrag van een uitvoerder; `scripts/`.

## Result

Bouwer drools-residue, 2026-11-06, branch `card/drools-residue`. De inventaris is alleen-lezend gemaakt door een
subagent (opus55) en per zin nagelopen tegen de code; logs in `werk/drools-residue/logs/`.

**(a) Inventaris en wat ermee gebeurde.**
- `spec/IR-SPEC.md` ch. 5.5, commit `d1ff1a5a1` (voorstel): de alinea over de tweede houding noemde de Drools-backend
  als haar instantie (kenmerk-salience, defaults-salience); de flank "measured 2026-09-23, both branches, open" en de
  notitie over de creatievolgorde beschreven de salience-ordening, `NO_LOOP` en de DRL-teksttak ("keeps the form
  described here", "refuses such a test by name", "orders no creation") als huidig. Nu: de backend neemt de eerste
  houding (truth maintenance, creaties per stratum aan een fixpunt, `Cycles` weigert wat de verifier weigert), de
  tweede houding van vóór 2026-10-10 als cursieve geschiedenis, en de drie vormen van de flank in één gedateerde
  notitie met de getuigen. De notitie "measured 2026-10-02, ronde klein 2, open" staat in verleden tijd met de huidige
  weigering. Bijvangst in dezelfde hoofdstuknotities: "The runner happens to pick the first" (ASP-backend, `all`-vouw
  met twee modellen) is sinds `233fd6438` (2026-10-22) CLINGO-030; de zin noemt dat nu.
- `backends/drools` javadoc en commentaar, commit van deze kaart: groep A (een eigen gedrag van de DRL-teksttak als
  huidig: `TmsWriteBackWitnessTest` r. 46–52 en 95–97, `TmsConsumerWitnessTest`, `TmsInputWitnessTest`,
  `FoldFilterFarSideWitnessTest` r. 33 en de kop "forms that refuse by name", `WholePeriodQuantifierFoldWitnessTest`,
  `DateSetFoldWitnessTest` (`RANGE`), `FoldMatrixFormWitnessTest` en `NonTemporalQuantifierFoldWitnessTest` ("Gate 12
  replays … the EXEC-MODEL branch only"; `reddingsvloot` "is markered"), `FilterFaultWitnessTest`,
  `DrlCreationLinkWitnessTest`, `SilentGuardflankWitnessTest`); groep B (javadoc van `Tms*`-getuigen van vóór de
  omschakeling, waarin "the exec-model/DRL-text branch" de destructieve takken betekende: `TmsNegationChain`,
  `TmsStartPoint`, `TmsAllocation`, `TmsComputeForm` (2×), `TmsFoldBinder`, `TmsBuiltin`, `TmsCharacteristicAssignment`,
  `TmsFilterFault`, `TmsAggregation`, `TmsLikeRef`); groep C (salience of `NO_LOOP` als huidig mechanisme:
  `LetClusterWitnessTest` (2×), `CompanionPathWitnessTest`, `DefaultsJustificationWitnessTest`,
  `CreationOrderWitnessTest`, `TemporalFixpointWitnessTest`); en twee plaatsen in de bron (`FactModelGenerator` r.
  ~501, `IrLoader` r. ~2500). Elk naar verleden tijd met "the destructive … lowering", of naar de huidige stand.
- `backends/drools/README.md`: niets verouderd (r. 389–391 noemt salience en `NO_LOOP` als wat de tak níet nodig heeft).
- `spec/ASP-TRANSLATION.md`: niets verouderd.
- Na de landingsronde van de hoofdsessie (mandaat verruimd, RUNTIME-SEMANTICS ch. 1.2 uitgezonderd), gemeten op beide
  takken en uitgevoerd in een eigen commit (zie (c)); metingen in `werk/drools-residue/probe/kaart3/`:
  - RUNTIME-SEMANTICS ch. 5.5 r. ~4373 ("in each of its three branches. Its exec-model and DRL-text branches carry no
    creation budget at all", een vaste cap van 100 000): nu twee takken, beide met het creatiebudget, de cap
    `max(10 000, 4 × R × N0)` van de README. Opnieuw gemeten: `anchor/jutterswet-erflijn` over 450 Wachters (101 025
    links) geeft op beide takken erfvolgeraantal(W1) = 449, `PLAYED 1`, in 719 s (exec-model) en 213 s (DRL-tekst)
    (`erflijn/meting.log`); de meting van 2026-10-09 blijft als geschiedenis staan.
  - RUNTIME-SEMANTICS ch. 6.1 r. ~7384 en IR-SPEC r. ~9409 (`opvolgerquorum`, de `all`-matrix "merk kleiner dan 2" en
    "merk ongelijk aan 2", case 4): ref en beide Drools-takken 2, clingo 1 (`quorum/meting.log`); was "the DRL-text
    branch 1".
  - IR-SPEC r. ~2873 (`wf079-chain-unconditional-writers`): alle vier vallen met een fout die beide schrijvers noemt
    (ref en clingo EF-016, beide Drools-takken `WritersConflict`); de oude stand in verleden tijd. IR-SPEC r. ~2981
    (`wf026-chain-two-allocations`): ref EF-016, clingo twee waarden, beide Drools-takken weigeren bij naam (DROOLS-140,
    WF-026); was "the DRL-text branch answers by document order" (`meting-negatieven.log`).
  - IR-SPEC r. ~3427: de vaste 100 000 wordt "a cap that grows with the rules and the facts of the session".
  - IR-SPEC r. ~7534 en ~7543 (de doel-poging, gemeten 2026-09-19): verleden tijd, met de huidige weigering bij naam op
    beide takken (DROOLS-157; `WholePeriodTargetAttemptWitnessTest`).
  - IR-SPEC r. ~8034 (een lege `filler`, een `NullPointerException`): de vorm bestaat sinds L2 (2026-09-24,
    `RoleMembership`) niet meer op de draad, dus niet meer te meten; verleden tijd, met die reden.
  - IR-SPEC r. ~8174 (`wf099-clamp-bound-unit`, twee routes per tak): beide takken geven nu één zin, `ERROR
    UnitConflict: onverenigbare eenheden bij een begrenzing (minimum) …` (`meting-wf099.log`).
  - `scripts/check-drools.sh` r. ~584–591 (`jutterswet-reddingsvloot` "markers it"): verleden tijd, met de stand sinds
    `drl-teksttak-op-truth-maintenance`. De andere tegenwoordige-tijdzinnen over de DRL-teksttak in de
    `EXPECTED_PLAYED`-geschiedenis van dat script vallen onder de bestaande NB van r. ~347–351 (2026-10-14): een
    vermelding daar betekent de destructieve DRL-weergave van haar datum.
- Overgeslagen als gedateerde geschiedenis: ongeveer 190 regels in ongeveer 80 testbestanden, 7 in IR-SPEC, 8 in
  RUNTIME-SEMANTICS, 2 in ASP-TRANSLATION.

**(b) Zinnen over beide takken hebben een getuige op beide takken.** `BothBranches.verdict` speelt een case op beide
takken en geeft null waar beide alles halen, of de regel van elk; een weigering moet op beide in één zin vallen.
`ReaderUnderWriterWitnessTest`, `NavigatedConditionWitnessTest`, `CreationOrderWitnessTest`,
`ChainConditionWitnessTest` en `FoldFilterFarSideWitnessTest` spelen nu elke rij op beide takken (hun `play` en de
twee weigeringsrijen), ook de rijen die een kring als WF-045 weigeren en de anker-golden `jutterswet-kustwacht`.
Gemeten: 66 rijen groen op beide takken (`logs/card3-witnesses.log`). De nieuwe zin in IR-SPEC ch. 5.5 noemt precies
deze vijf getuigen, "every row on both branches". Rood eerst is hier geen reparatie maar een bredere getuige; de
DRL-teksttak speelt elke rij zoals ref.

**(c) Commits.** `d1ff1a5a1` de IR-SPEC-wijziging van ch. 5.5 (voorstel, eigen commit); de javadoc, het commentaar en
de getuigen in de commit van deze kaart; de zinnen in RUNTIME-SEMANTICS, IR-SPEC buiten ch. 5.5 en het commentaar van
`check-drools.sh` in een eigen commit daarna (gate 1 groen, `logs/punt1-gate1.log`; `bash -n` op het script en
`check-drools-codes.sh` groen). Gate 1 groen (`logs/card3-gate1.log`); gate 12 groen (`logs/card3-gate12.log`: 353 gespeeld
op beide takken, JUnit 1880).
