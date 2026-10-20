# creatie-zonder-binding-via-supertype: de exec-model-tak draagt een creatie zonder binding waarvan het typenpaar via een supertype loopt

*Vastgelegd door de hoofdsessie bij de landing van `20261010-objectcreatie-zonder-connectingrole-binding` (2026-10-11),
uit open punt 1 van het verslag van de bouwer van die kaart. Regelnummers op de tip van die kaart.*

## Aanleiding

Sinds `20261010-objectcreatie-zonder-connectingrole-binding` draagt de exec-model-tak een objectcreatie zonder
`connectingRole`-binding. De terugkoppeling komt dan uit het gedeelde beslisblok `FactModelGenerator.creationLink`.
Dat blok bereikt een rol ook via een supertype (`connectingRole`, met `isA`). De tak zoekt de relatie daarna op
exacte vullers (`TmsTranspiler.typePairRelation`), omdat zijn strata de terugkoppeling ook uit de exacte typen afleiden
(`Cycles.backLink`). Loopt het typenpaar via een supertype van het onderwerp of van het gemaakte type, dan
weigert de tak de creatie met naam:

    een objectcreatie zonder connectingRole-binding waarvan de terugkoppeling uit het typenpaar ('<backRole>' en
    '<fwdRole>') via een supertype van het onderwerp of van het gemaakte type loopt: de strata van deze tak leiden de
    relatie van de terugkoppeling af uit de exacte typen en zien haar dan niet geschreven: deze tak draagt deze vorm nog
    niet

**Gemeten** door de bouwer van die kaart:
- **Het document.** Het document van de getuige
  `ObjectCreationWitnessTest.a_second_creation_reads_the_derived_relation_after_the_first`
  (`backends/drools/src/test/resources/object-creation/rejection-after-the-decision`), met `Jaarbesluit is een
  Penningbesluit` als gemaakt type.
- **Verifier, ref en clingo:** PASS, J1 0 afwijzingen.
- **Een bouw zonder de weigering** (een vuller via de keten van supertypen, nooit gecommit) zet beide creaties in
  stratum 0 en geeft `FAIL case.json: attribuut aantal afwijzingen: verwacht 0, werkelijk 1`.

De verifier (`connecting_candidates`, met `fills`) en ref (`connecting_pair`) lezen het paar via ⊑. Ook de DRL-teksttak
speelt de vorm: `ObjectCreationWitnessTest.a_type_pair_via_a_supertype_is_refused_on_build` meet daar PLAYED 1. Een
creatie mét binding heeft het probleem niet, want `Cycles.backLink` neemt dan het gebonden feittype (lens 1 van
die kaart).

## Wat er gebeurt

`Cycles.backLink` en `TmsTranspiler.typePairRelation` krijgen dezelfde ⊑-lezing van het typenpaar als de
verifier en ref. Daarna draagt de tak de vorm en vervalt de weigering. De javadoc van `Cycles.backLink` (r. 510–512) noemt zijn lezing nu al
"the verifier's reading of the by-pair route", terwijl de code exacte vullers vergelijkt en de verifier `fills` (ook
via supertypen) gebruikt (eindlens van `20261010-objectcreatie-zonder-connectingrole-binding`, advies 3). Na deze kaart
klopt die zin weer.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst. De strata-getuige met haar subtypetweeling (het document hierboven met een `Jaarbesluit`) speelt in de
  exec-model-tak het antwoord van ref: J1 0 afwijzingen.
- (b) `a_type_pair_via_a_supertype_is_refused_on_build` speelt het antwoord van ref (uitkering 250, uitkeringsjaar
  2025), in plaats van de weigering.
- (c) Alle gates van `CLAUDE.md` groen. Speelt er daardoor een golden meer, dan gaan de pins van gate 12 mee (beschermd
  pad: voorstel).

## Omvang

Klein. May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/Cycles.java` en
`TmsTranspiler.java`, getuigen onder `backends/drools/src/test`, `scripts/check-drools.sh` (pins; voorstel),
kaartverplaatsing. Serieel met andere Drools-kaarten.

## Raakvlakken

- `20261008-drl-teksttak-op-truth-maintenance` splitst `TmsTranspiler` in een analyse en twee weergaven. Daarna
  geldt deze reparatie voor beide takken.
