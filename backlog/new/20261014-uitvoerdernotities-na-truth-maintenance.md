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
  `TmsTerugzettingWitnessTest` (r. 51: "On the DRL-text branch the call writes nothing: its rules write the beans
  while they fire"; de test draait beide takken met dezelfde terugzetting) en `VouwfilterVerreKantWitnessTest` r. 33
  ("The DRL-text branch writes such a test into the element pattern").

Wat de code bij de landing liet zien: een genavigeerde lezing in een voorwaarde (`anchor/jutterswet-kustwacht`, dat de
destructieve DRL-teksttak liet falen, en `jutterswet-havenketen`), een toets per element via een rol van het element
(`jutterswet-late-vondst`, `VouwfilterVerreKantWitnessTest`) en de creatievolgorde (`TmsCreatieWitnessTest`) spelen op
beide takken zoals ref. Een leeg- of gevuldtoets vóór haar schrijver heeft een getuige op de exec-model-tak alleen
(`LezerOnderSchrijverWitnessTest`; `GenavigeerdeVoorwaardeWitnessTest` evenzo) en geen corpusgolden.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een inventaris van elke zin in `spec/` en in `backends/drools` (bron, testbron, README) die een destructieve
  verlaging als huidig beschrijft, met per zin een voorstel; gedateerde geschiedenis blijft staan.
- (b) Waar een nieuwe zin iets van beide takken zegt, draait een getuige beide takken: `LezerOnderSchrijverWitnessTest`
  en `GenavigeerdeVoorwaardeWitnessTest` krijgen de DRL-teksttak erbij, of de zin noemt alleen de tak die getuigd is.
- (c) De wijzigingen in `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` en `spec/ASP-TRANSLATION.md` zijn voorstellen
  (beschermd pad), elk in een eigen commit; gate 1 groen, en gate 12 als de suite verandert.

May touch: `backends/drools/**` (javadoc, commentaar, testbron), `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/ASP-TRANSLATION.md` (**voorstel**).

Out of scope: gedrag van een uitvoerder; `scripts/`.
