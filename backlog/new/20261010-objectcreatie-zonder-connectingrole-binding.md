# objectcreatie-zonder-connectingrole-binding: de exec-model-tak leidt de terugkoppeling van een objectcreatie zonder binding af uit het typenpaar

*Vastgelegd door de hoofdsessie bij de landing van `20261008-tms-omschakeling`, na het besluit van de eigenaar van
2026-10-10 (besluit 12 van die kaart): het dekkingsverlies van de omschakeling is aanvaard, met één vervolgkaart direct
daarna voor deze vorm, een golden plus het dragen ervan. Herkomst: punt 4 van de lijst "Dekkingsverlies (C)" in het
verslag van stap 2 van die kaart. Regelnummers op `card/tms-omschakeling` `1df4affc`.*

## Aanleiding

Een objectcreatie zonder `connectingRole`-binding laat de terugkoppeling van het gemaakte object open; de relatie volgt
dan uit het typenpaar. De destructieve exec-model-tak leidde die terugkoppeling uit het typenpaar af, zoals de
DRL-teksttak nog doet. De exec-model-tak met truth maintenance weigert de vorm met naam
(`TmsTranspiler.objectCreatie`, r. 2003–2005):

    een objectcreatie zonder connectingRole-binding, waarvan de terugkoppeling uit het typenpaar volgt: deze tak draagt
    deze vorm nog niet

De tak roept het gedeelde beslisblok `FactModelGenerator.creatiekoppeling` al aan (r. 1999) en weigert daarna als er
geen binding is.

- **De frontend schrijft de vorm**: zij laat de binding weg als de zin het type noemt in plaats van een rol
  (`RegelSpraakModel.benoemdeRolBinding`, verslag van stap 2).
- **Geen golden draagt de vorm zichtbaar.** Eén golden heeft haar, `anchor/jutterswet-penninguitgifte`, maar beide
  takken weigeren die golden eerder op een consistentie (gate 12: "consistentie: houderscontrole — een Aggregation over
  de enkelvoudige elementrol 'houder' wiens zusterrol 'penning' meervoudig is …", §13.3.9).
- **De pariteitszone** heeft twee objectcreaties, allebei met een `connectingRole` (verslag van stap 2). v2
  herleving-ww speelt in de exec-model-tak alle 160 cases (doorloop van die kaart).
- **Voor de distributie** is dit de zwaarste post van het dekkingsverlies: een gewone regelset met een creatie waarvan de
  zin het type noemt, bouwt na de pinbump niet meer.

**Verifier-schone documenten met de uitkomsten** (de vijf getuigen die de omschakeling op deze weigering zette; de
verifier gaf PASS op elk document; de getuigen bouwen ze zelf):

| Getuige | Document | ref (javadoc van de getuige) | DRL-teksttak | exec-model-tak |
|---|---|---|---|---|
| `ObjectCreatieWitnessTest.een_kenmerkassertie_vlagt_de_genegeerde_lezing` | penninguitgifte zonder de andere consistentie; P1 in de invoer, nooit uitgereikt | P1 inconsistent onder `penningcontrole`, de gemaakte penning niet | niet gemeten | geweigerd met de zin hierboven |
| `ObjectCreatieWitnessTest.dezelfde_kenmerkassertie_vlagt_de_vervulde_lezing_niet` | idem, met het kenmerk op P1 | elke Penning consistent | niet gemeten | geweigerd |
| `ObjectCreatieWitnessTest.een_aantalassertie_over_een_enkelvoudige_rol_vlagt_de_lege_telling` | penninguitgifte met de houderscontrole op `dossiervoering` | inconsistent @ J1 (aantal 0) | niet gemeten | geweigerd |
| `ObjectCreatieWitnessTest.dezelfde_aantalassertie_vlagt_een_gevulde_telling_niet` | idem, met een dossier in de case | consistent (aantal 1) | niet gemeten | geweigerd |
| `DrlCreatieTakWitnessTest.de_naar_veel_arm_koppelt_alleen_aan_de_gemunte_zijde` | `penning` naar veel en `dossier` 1:1 in één document | — | speelt: de vouw vanaf het onderwerp bereikt het gemaakte object (1 penning, totaal 7, 1 dossier) | geweigerd |

Clingo op deze documenten en de DRL-teksttak op de vier documenten van `ObjectCreatieWitnessTest` zijn niet gemeten;
meten bij het oppakken.

## Wat er gebeurt

De exec-model-tak leidt bij een objectcreatie zonder binding de terugkoppeling uit het typenpaar af met het gedeelde
beslisblok, zoals de DRL-teksttak, en schrijft de koppeling op dezelfde manier als bij een creatie met binding. Een
dubbelzinnig paar blijft met naam geweigerd (ch. 5.5; ref faalt daar ook op een lege wereld, en gate 8 houdt
`corpus/rood/ambiguous-backlink` op de uitsluitingslijst van het Drools-been).

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, eigen Jutterswet-casus, rechtenvrij, met `bron.rgs` waarin de zin het type noemt, zodat
  de frontend geen binding schrijft, en waarin de creatie zonder binding de enige vorm is die de exec-model-tak niet
  droeg. Cases met absolute verwachtingen: een creatie die maakt en koppelt en via de relatie gelezen wordt, en een
  case waarin de voorwaarde niet geldt. Ref ≡ clingo ≡ Drools (beide takken).
- (b) Rood eerst: de vijf getuigen hierboven spelen in de exec-model-tak weer het antwoord van ref (hun javadoc zegt
  dat: "then this row plays the ref answer above again", "then this row plays on both branches again").
- (c) Een dubbelzinnig paar blijft geweigerd; de vermelding van `ambiguous-backlink` in `scripts/check-rood.sh` blijft
  gelijk.
- (d) Gate 12: `EXPECTED_PLAYED` één omhoog, en `EXPECTED_DRL_PLAYED` ook als de DRL-teksttak de golden speelt
  (beschermd pad: voorstel); alle gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel. De beslissing bestaat al; het werk is de weg zonder binding in `TmsTranspiler.objectCreatie`, de
golden en de getuigen.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/schema/FactModelGenerator.java` (alleen als het beslisblok dat
vraagt, zonder gedragswijziging in de DRL-teksttak), getuigen onder `backends/drools/src/test/java`,
`corpus/golden/<nieuwe golden>/`, `scripts/check-drools.sh` (pins; voorstel), kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

## Raakvlakken

- De kaarttekst voor de toolchain in het `Result:` van `20261008-tms-omschakeling` noemt deze vorm in het
  dekkingsverlies; landt deze kaart, dan verzet de distributie haar pin opnieuw.
- `20260918-drools-veelopveel-beanmodel`: `anchor/jutterswet-penninguitgifte` weigert ook op §13.3.9; die golden speelt
  pas als beide kaarten geland zijn.
- `20261008-drl-teksttak-op-truth-maintenance`: de DRL-teksttak deelt het beslisblok en draagt de vorm al.
