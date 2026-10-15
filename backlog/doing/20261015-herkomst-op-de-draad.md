# herkomst-op-de-draad: een gegenereerd element draagt zijn herkomst in het IR-document

*Op verzoek van de eigenaar (2026-10-15; ontwerpadvies door de eigenaar overgenomen). VOORSTEL: de kaart raakt
beschermde paden (`spec/IR-SPEC.md`, `spec/schema/`, `spec/verifier/`, `scripts/`), dus de branch blijft ongemerged tot
de eigenaar haar beoordeelt. Regelnummers op `be3bbec0`.*

Goal: de RegelSpraak-frontend maakt zelf elementen onder een naam die geen declaratie in de bron draagt: de
opgave-familie (`opgegeven X`, `berekende X`, kenmerk `afwijkende opgave X`, regel `afwijking opgave X`, consistentie
`controle opgave X`, defaultsgroep `opgave boven berekening X`), de groep `B voor S` van een subtype, lid en regel
`<lid> via <rollen>` van een vouw met stappen na de verzameling, een rij van een beslistabel en het anker `startpunt
<tijdlijn>`. De kaart `gegenereerde-elementen-dragen-herkomst` legde dat vast in de zijtabel
`RegelSpraakModel.gegenereerd` (`model/RegelSpraakModel.java:73–79`), maar de emitter schrijft er niets van, dus een
model dat de IR-lader leest heeft haar leeg (`backends/drools/…/loader/IrLoader.java`). Een consument die van het
IR-document uitgaat (een beslisservice, een benamingen-lint, een werkbank) kan een gegenereerd element niet
onderscheiden van een geschreven declaratie, en niet zien waaruit het komt.

De kaart zet de herkomst op de draad als extensieveld `origin`, met hetzelfde zijtabelpatroon als `designations`
(IR-SPEC ch. 2.12: zonder semantiek, "written whole or not at all"), vlak vóór `designations` op dezelfde knoopsoorten:

```json
"origin": {"form": "inputAboveComputation",
           "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}
```

`form` is een gesloten verzameling in Engelse lowerCamel (de vijf frontendvormen van `Herkomstvorm`, niet
`defaultsstapel` en `versie`: dat zijn manifestnamen van de backend, geen declaraties); `from` is een verwijzing op naam
zoals elke IR-verwijzing (`kind`, `owner` alleen bij een soort met eigenaar, `name` canoniek). `source` wordt niet
gebruikt of overladen.

Acceptance (in deze volgorde, elke stap een eigen commit):
- (a) Voorstel IR-SPEC ch. 2.13 "Origin" met kruisverwijzing in 2.12, opname in de lijst extensievelden van § Method,
  de `form`-waarden `layer: dialect(regelspraak)`; schema: `$defs/Origin` plus de property `origin` vóór
  `designations` op elke knoop die `designations` draagt; WF-121 in ch. 2.13 en in de WF-index. WF-121 is het eerste
  vrije nummer (gecontroleerd op `be3bbec0`: WF-120 is het hoogste). Clausules: (a) `form` en `from.kind` in hun
  gesloten verzameling; (b) `from` noemt een declaratie van die soort in het document (een vorm in de namenruimte van
  WF-020), met `owner` precies bij een soort met eigenaar; (c) niet het element zelf, en geen kring over
  `origin`-verwijzingen; (d) de soort van `from` past bij `form` (neem op als ze zich zonder dialectkennis laat
  stellen; anders schrappen en in het Result motiveren). Een beslistabel is geen element van het model, haar rijen
  wel (`RuleChecker.gegenereerdeElementen`, "a decision table, the one source that is no element"): de `from` van een
  rij krijgt daarom de eigen soort `decisionTable`, die geen declaratie van het document noemt, en (b) geldt voor elke
  andere soort. Met de soort `form` zou een rij naar een vorm wijzen die niet bestaat, of naar een andere vorm met de
  naam van de tabel.
- (b) Verifier: `Loader.vocabulaires` leest `origin` mee in `benoembaar` (nieuw veld), de checks staan naast WF-116 tot
  WF-120 (`Checks.vocabulaires`); het `document`-record blijft ongewijzigd. Vijf negatieve gevallen
  `corpus/negatief/wf121-herkomst-vorm-onbekend`, `-bron-onbekend`, `-zichzelf`, `-kring`, `-soort-past-niet` (vervalt
  als (d) vervalt), elk met `expect.txt` en `NOTE.md`. `opam exec -- sh scripts/check-verifier.sh` groen.
- (c) De emitter schrijft `origin` uit `gegenereerd`; een nieuwe testklasse
  `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/HerkomstOpDeDraadTest.java`, eerst rood: elke vorm op de
  draad, de spelling van `form` en `kind`, en een document zonder gegenereerd element is byte voor byte het document van
  vóór de kaart (zoals `VocabulaireDialectTest.zonder_de_extensie_is_het_document_hetzelfde`).
- (d) `IrLoader` leest `origin` terug in `m.gegenereerd`; een nieuwe getuige
  `backends/drools/src/test/java/dev/uchar/regelspraak/loader/HerkomstWitnessTest.java` (het lezen, de weigering van
  (a) in de zin van de gedeelde lader, wat de bron noemt is geen laadweigering) en, omdat de Maven-build van de backend
  de resolver niet compileert, een getuige in de bronroutesuite van gate 15,
  `backends/drools/src/bronroute/java/dev/uchar/regelspraak/loader/HerkomstBronrouteTest.java`: per rgs-golden is de
  tabel van het geladen model gelijk aan die van de frontend. De zinnen "a model the IR loader reads leaves it empty"
  (`RegelSpraakModel.java`, `grammar/DIVERGENCES.md`) bijgewerkt.
- (e) Goldens met gegenereerde elementen en de TOKA-dump en -slices heruitgezonden, per stuk genoemd, met een
  divergentie in `toka/README.md`; elk ander document byte-gelijk (gate 10, 11, 16). `corpus/golden/anchor/` blijft
  onaangeroerd; valt daar iets onder, dan stopt de kaart en meldt het.
- (f) Gate 13: een herkomstleg naast de vocabulaireleg die elke golden met `origin` en case-bestanden met de velden
  GESTRIPT terugspeelt op ref, clingo en beide Drools-takken, met vloeren; rood getoond met een mutant die `origin`
  leest, dan groen.
- (g) `grammar/DIVERGENCES.md` "Gegenereerde elementen" bijgewerkt; voorgestelde `spec/CONSTRUCTS.md`-rij (naast rij
  41) als tekst in het Result.

Alle gates van `CLAUDE.md` groen op de eindcommit (volledige set, `WAY-OF-WORKING.md` §6). Bestaande tests worden
niet gewijzigd: moet er één veranderen, dan gaat de patch in het Result en niet in de branch.

May touch: `spec/IR-SPEC.md`, `spec/schema/uchar-ir-v1.schema.json`, `spec/verifier/lib/ast.ml`,
`spec/verifier/lib/loader.ml`, `spec/verifier/lib/checks.ml`, `spec/verifier/bin/verifier.ml`,
`corpus/negatief/wf121-herkomst-*/`, de `ir.json` (en waar gate 16 ze schrijft de case-bestanden) van de goldens met
gegenereerde elementen buiten `corpus/golden/anchor/`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/emit/IrEmitter.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/ir/Spelling.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/RegelSpraakModel.java`,
`frontends/regelspraak/grammar/DIVERGENCES.md`,
`backends/drools/src/main/java/dev/uchar/regelspraak/loader/IrLoader.java`, de drie nieuwe testklassen,
`toka/casus/ir.json`, `toka/cases/*/ir.json`, `toka/README.md`, `scripts/check-reftype-derivation.sh`, deze kaart.

Out of scope: de delen van een samengestelde naam (`B voor S`, `<lid> via <rollen>`) op de draad: één `from` drukt
ze niet uit, een latere lijst `parts` is additief en wordt in de spec als niet gebouwd genoemd; `spec/CONSTRUCTS.md`
en `spec/PLAN.md` (rijtekst in het Result); de Drools-manifestnamen `defaultsstapel` en `versie`; de distributie
(regelspraak-toolchain) en de werkbank, die het veld na een pin-bump overnemen op hun eigen kaart.
