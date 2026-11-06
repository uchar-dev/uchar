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

## Result

Gebouwd op branch `card/drools-frontend-cleanup` (opruimronde 2026-11-06). Logs onder
`uchar-dev/werk/java-cleanup/logs/`.

- **De reparatie.** `TmsTranspiler.typePairRelation(model, subject, fresh, k)` is statisch en package-private
  geworden en leest het typenpaar met ⊑: een type vult een rol die een van zijn supertypen vult (`fills` vraagt
  `type.isA(filler)`), zoals de verifier (`connecting_candidates`) en ref (`connecting_pair`). `Cycles.backLink`
  leidt de relatie van een creatie zonder binding nu af met hetzelfde gedeelde besluit
  (`FactModelGenerator.creationLink`) en dezelfde methode, zodat de strata precies de relatie zien die de lowering
  schrijft. De weigering DROOLS-148 vervalt; de code is `RETIRED` (register, `CLAUDE.md` gate 12). Waar het besluit
  rollen koppelt die geen feittype draagt, werpt de lowering nu een `IllegalStateException` met de naam van de
  creatie (een gebroken invariant, voorheen gemaskeerd door de weigering). De javadoc van `Cycles.backLink` klopt
  weer; de README (lijst "the creations it does not build yet") noemt de vorm niet meer.
- **(a)** Nieuwe getuige `ObjectCreationWitnessTest.a_second_creation_reads_a_relation_derived_through_a_supertype_after_the_first`
  (de strata-getuige met `Jaarbesluit is een Penningbesluit` als gemaakt type). Rood op de oude code:
  `Excluded … DROOLS-148` (`k1-red.log`). Met alleen `typePairRelation` gerepareerd en `Cycles` oud:
  `case: attribuut aantal afwijzingen: verwacht 0, werkelijk 1` (`k1-half.log`), de fout uit de kaart. Groen met
  beide (`k1-green.log`), op beide takken (`play` vergelijkt exec-model en DRL-tekst). Verifier, ref en clingo PASS
  op hetzelfde document (`probe/k1/`).
- **(b)** `a_type_pair_via_a_supertype_is_refused_on_build` heet nu `a_type_pair_via_a_supertype_plays` en verwacht
  het antwoord van ref (uitkering 250, uitkeringsjaar 2025) in plaats van de weigering: de kaart verandert dit gedrag
  uitdrukkelijk.
- **(c)** Gate 12 groen (`k1-gate12.log`): 350 gespeeld, gelijk aan de pin; geen golden heeft de vorm, dus geen pin
  bewoog. JUnit 1866 tests. `check-drools-codes.sh`: 316 codes, 27 retired.

### Landing (hoofdsessie, 2026-11-06)

Geland op main na `make gates` op de bouwerstand (`b45b3f766`, 17 van 17 groen; na de rebase op main alleen backlog
anders).
