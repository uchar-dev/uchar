# drools-facet-keten — de ketensluiting van de Drools-backend

Goal: de reparatiekaart die vervolgpunt A van
`backlog/done/20260912-subtype-frontend-keten.md` uitvoert, met maintainersmandaat
voor route 1 (mechanisch). `backends/drools` leest de facet-ZIJKAARTEN van leden
(granulariteit, tijdlijnaantal, dimensies) op 121 gemeten plekken buiten de loader
rechtstreeks van het gedeclareerde/concrete type-object in plaats van via de
`⊑`-keten. Bij een subtype met een GEËRFD temporeel of gedimensioneerd lid
antwoordt zo'n lezing "niet temporeel" — geen ontbrekende binding maar een
VERKEERD oordeel (een tijdlijn die als scalair wordt gelowerd). Het vangnet
daartegen is een luide per-vorm-weigering in `IrLoader.bindSupertypes`, die de
combinatie subtype × temporeel/gedimensioneerd in zijn geheel afwijst; het
commentaar zegt letterlijk "until those reads are chain-aware". Gevolg: die
documentklasse werkt in ref en clingo maar speelt niet in Drools — een
gedocumenteerd gat in het differentiaalvierkant.

Deze kaart leidt de lezingen door de ketenbewuste accessors op
`RegelSpraakModel.Objecttype`, heft de weigering op, en landt de golden die de
vorige kaart bouwde, mat en weer moest verwijderen: `jutterswet-erfwacht`, de
subtype-tweeling van `jutterswet-getijdenwacht`.

Acceptance:
- rood-eerst: de golden eerst, gemeten op de basis (Drools EXCLUDEt hem per vorm),
  daarna de lezingen, daarna PLAYED;
- elke van de 121 plekken gemeten en geclassificeerd: LEDENLOOKUP (→ accessor) of
  ITERATIE over een eigen-ledenkaart (→ rauw, met de reden ter plekke — een
  iteratie per declarant bezoekt elk lid precies één keer, ketensluiting zou
  dubbeltellen);
- `corpus/golden/jutterswet-erfwacht`: ref = clingo exact (6 × 10 + 1 × 11 = 71),
  no-op weerlegd, PLAYED in beide transpilertakken voor zover die de vormen dragen;
- het vangnet: basis-jar en kop-jar over ALLE goldens-met-cases in BEIDE takken,
  regel voor regel gedift — alleen `jutterswet-erfwacht` mag verschijnen;
- alle veertien gates groen, gate 10 met byte-identieke re-emissie van élke
  rgs-golden (de model-accessors compileren mee in de frontend).

May touch: `backends/drools/`,
`frontends/regelspraak/src/main/java/.../model/RegelSpraakModel.java` en
`.../kern/Tijdrekening.java` (alleen accessor-toevoegingen in de bestaande vorm),
`corpus/golden/jutterswet-erfwacht` (nieuw), `scripts/check-drools.sh`
(`EXPECTED_PLAYED` + de kanaalvloerlijst, elk met reden), `spec/IR-SPEC.md` en
`spec/RUNTIME-SEMANTICS.md` (alleen de gerichte weigering-zinnen), dit
kaartbestand.

Out of scope: `spec/CONSTRUCTS.md` en `spec/PLAN.md` (registervoorstel als
letterlijke tekst in het Result), `CLAUDE.md`, `.github/`, `spec/verifier/`,
`ref/`, `backends/clingo`, de resolver/emitter van `frontends/regelspraak`,
`frontends/flogic`, `frontends/catala`, bestaande corpusbestanden.

Result:
