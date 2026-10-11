# creatie-zonder-binding-via-supertype: de exec-model-tak draagt een creatie zonder binding waarvan het typenpaar via een supertype loopt

*Vastgelegd door de hoofdsessie bij de landing van `20261010-objectcreatie-zonder-connectingrole-binding` (2026-10-11),
uit open punt 1 van het verslag van de bouwer van die kaart. Regelnummers op de tip van die kaart.*

## Aanleiding

Sinds `20261010-objectcreatie-zonder-connectingrole-binding` draagt de exec-model-tak een objectcreatie zonder
`connectingRole`-binding. De terugkoppeling komt dan uit het gedeelde beslisblok `FactModelGenerator.creatiekoppeling`.
Dat blok bereikt een rol ook via een supertype (`verbindendeRol`, met `isEen`). De tak zoekt de relatie daarna op
exacte vullers (`TmsTranspiler.typenpaarrelatie`), omdat zijn strata de terugkoppeling ook uit de exacte typen afleiden
(`Kringen.terugkoppeling`). Loopt het typenpaar via een supertype van het onderwerp of van het gemaakte type, dan
weigert de tak de creatie met naam:

    een objectcreatie zonder connectingRole-binding waarvan de terugkoppeling uit het typenpaar ('<backRol>' en
    '<fwdRol>') via een supertype van het onderwerp of van het gemaakte type loopt: de strata van deze tak leiden de
    relatie van de terugkoppeling af uit de exacte typen en zien haar dan niet geschreven: deze tak draagt deze vorm nog
    niet

**Gemeten** door de bouwer van die kaart:
- **Het document.** Het document van de getuige
  `ObjectCreatieWitnessTest.een_tweede_creatie_leest_de_afgeleide_relatie_na_de_eerste`
  (`backends/drools/src/test/resources/objectcreatie/afwijzing-na-het-besluit`), met `Jaarbesluit is een
  Penningbesluit` als gemaakt type.
- **Verifier, ref en clingo:** PASS, J1 0 afwijzingen.
- **Een bouw zonder de weigering** (een vuller via de keten van supertypen, nooit gecommit) zet beide creaties in
  stratum 0 en geeft `FAIL case.json: attribuut aantal afwijzingen: verwacht 0, werkelijk 1`.

De verifier (`connecting_candidates`, met `fills`) en ref (`connecting_pair`) lezen het paar via ⊑. Ook de DRL-teksttak
speelt de vorm: `ObjectCreatieWitnessTest.een_typenpaar_via_een_supertype_weigert_bij_de_bouw` meet daar PLAYED 1. Een
creatie mét binding heeft het probleem niet, want `Kringen.terugkoppeling` neemt dan het gebonden feittype (lens 1 van
die kaart).

## Wat er gebeurt

`Kringen.terugkoppeling` en `TmsTranspiler.typenpaarrelatie` krijgen dezelfde ⊑-lezing van het typenpaar als de
verifier en ref. Daarna draagt de tak de vorm en vervalt de weigering. De javadoc van `Kringen.terugkoppeling` (r. 510–512) noemt zijn lezing nu al
"the verifier's reading of the by-pair route", terwijl de code exacte vullers vergelijkt en de verifier `fills` (ook
via supertypen) gebruikt (eindlens van `20261010-objectcreatie-zonder-connectingrole-binding`, advies 3). Na deze kaart
klopt die zin weer.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst. De strata-getuige met haar subtypetweeling (het document hierboven met een `Jaarbesluit`) speelt in de
  exec-model-tak het antwoord van ref: J1 0 afwijzingen.
- (b) `een_typenpaar_via_een_supertype_weigert_bij_de_bouw` speelt het antwoord van ref (uitkering 250, uitkeringsjaar
  2025), in plaats van de weigering.
- (c) Alle gates van `CLAUDE.md` groen. Speelt er daardoor een golden meer, dan gaan de pins van gate 12 mee (beschermd
  pad: voorstel).

## Omvang

Klein. May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/Kringen.java` en
`TmsTranspiler.java`, getuigen onder `backends/drools/src/test`, `scripts/check-drools.sh` (pins; voorstel),
kaartverplaatsing. Serieel met andere Drools-kaarten.

## Raakvlakken

- `20261008-drl-teksttak-op-truth-maintenance` splitst `TmsTranspiler` in een analyse en twee weergaven. Daarna
  geldt deze reparatie voor beide takken.
