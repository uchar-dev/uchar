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
`toka/casus/ir.json`, `toka/cases/*/ir.json`, `toka/README.md`, `scripts/check-reftype-derivation.sh`, de
beschrijving van gate 13 in `CLAUDE.md`, de rijen voor de nieuwe schemanamen in `spec/proposals/v1-naamtabel.json`,
deze kaart.

Out of scope: de delen van een samengestelde naam (`B voor S`, `<lid> via <rollen>`) op de draad: één `from` drukt
ze niet uit, een latere lijst `parts` is additief en wordt in de spec als niet gebouwd genoemd; `spec/CONSTRUCTS.md`
en `spec/PLAN.md` (rijtekst in het Result); de Drools-manifestnamen `defaultsstapel` en `versie`; de distributie
(regelspraak-toolchain) en de werkbank, die het veld na een pin-bump overnemen op hun eigen kaart.

## Result (voorstel, 2026-10-15)

Branch `card/herkomst-op-de-draad`, vanaf `be3bbec0`, ongemerged en ongepusht: de kaart raakt beschermde paden. De
kaart blijft in `doing/` tot de eigenaar het voorstel keurt. `main` staat inmiddels op `fe8901fb` (de kaart
`waarnemer-vuur`); `git merge-tree` geeft geen conflict.

Commits per stap. De goldens (e) zijn vóór de lader (d) gecommit, omdat de getuige van (d) de gecommitte goldens
leest; elke andere stap volgt de volgorde van de kaart.

- (a) `ac23a399` IR-SPEC ch. 2.13 "Origin", kruisverwijzing in 2.12, § Method, de laagmarkering van ch. 2, een rij
  `origin` in elke veldtabel met `designations`, WF-121 in ch. 2.13 en in de WF-index; schema `$defs/Origin`,
  `$defs/OriginSource` en de property `origin` vóór `designations` op de 14 defs die `designations` dragen.
- (b) `b067656d` verifier (lader en WF-121) en vijf negatieve gevallen.
- (c) `6c01d7cf` emitter en `Spelling`, met `HerkomstOpDeDraadTest`; `208d0311` en (bij g) `9e755428` vullen die
  test aan.
- (e) `8a6f91c1` negen goldens, de TOKA-dump en vijf TOKA-slices, `toka/README.md`.
- (d) `b7236f7f` `IrLoader`, `HerkomstWitnessTest`, `HerkomstBronrouteTest`, de javadoc van
  `RegelSpraakModel.gegenereerd`.
- (f) `227ea33e` de herkomstleg van gate 13, met de beschrijving in `CLAUDE.md`.
- (g) `9e755428` `grammar/DIVERGENCES.md`, "Gegenereerde elementen".
- `d488ef85` rijen in `spec/proposals/v1-naamtabel.json` (zie Open).

### Het veld

```json
"Origin": {"type": "object", "additionalProperties": false, "required": ["form", "from"],
           "properties": {"form": {"enum": ["inputAboveComputation", "exceptionForSubtype", "decisionTableRow",
                                            "stepsAfterSet", "startPoint"]},
                          "from": {"$ref": "#/$defs/OriginSource"}}},
"OriginSource": {"type": "object", "additionalProperties": false, "required": ["kind", "name"],
                 "properties": {"kind": {"enum": ["objectType", "attribute", "characteristic", "factType", "role",
                                                  "enumerationValue", "parameter", "namedForm", "ruleGroup",
                                                  "decisionTable"]},
                                "owner": {"type": "string"}, "name": {"type": "string"}}}
```

Uit `corpus/golden/jutterswet-opgave-vormen/ir.json`, het attribuut `berekende waarde` van `Vondst` en de regel
`stukprijs van een aanspoelsel 1`:

```json
"origin": {"form": "inputAboveComputation",
           "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}
"origin": {"form": "decisionTableRow",
           "from": {"kind": "decisionTable", "name": "stukprijs van een aanspoelsel"}}
```

Afwijking van het ontwerp: de soorten van `from` zijn de negen van `Benoembaar.Soort` (op de draad `namedForm` voor
`VORM`) plus `decisionTable`. Het model legt de bron van een beslistabelrij vast als vorm, omdat de tabel geen element
is (`RuleChecker.gegenereerdeElementen`). Met `namedForm` zou de rij naar een vorm wijzen die niet bestaat, of naar een
andere: een regel mag de naam van de tabel dragen (gemeten: `Regel Bonustabel` naast `Beslistabel Bonustabel` is
schoon, getuige `HerkomstOpDeDraadTest.een_regel_met_de_naam_van_de_tabel_is_niet_de_bron_van_haar_rijen`).
`Spelling.originKind`/`originSoort` vertalen; de lader maakt van `decisionTable` weer `VORM`.

### WF-121 en de verifier

- (a) `form` in de vijf vormen en `from.kind` in de tien soorten, byte voor byte. De verifier noemt het onder WF-121;
  elke andere lezer van de gedeelde OCaml-lader (ref, clingo, de Catala- en F-logic-frontend) en de Drools-lader
  weigert het document bij het laden in dezelfde zin zonder nummer (`Checks.herkomst_refusal`), omdat zij de herkomst
  niet kunnen bewaren met een waarde die de verzameling niet kent; gemeten met ref op het negatieve geval.
- (b) `owner` precies bij `attribute`, `characteristic`, `role`, `enumerationValue`; behalve bij `decisionTable` noemt
  `from` een declaratie van die soort in het document (naamidentiteit ch. 2.1; een attribuut of kenmerk op het type dat
  het zelf declareert).
- (c) niet de eigen declaratie; geen kring over `origin`-verwijzingen (eenmaal gemeld, bij het eerste lid in het
  document). Een `from` mag zelf gegenereerd zijn.
- (d) de soort die de vorm noemt: `attribute` bij `inputAboveComputation` en `stepsAfterSet`, `namedForm` bij
  `exceptionForSubtype` en `startPoint`, `decisionTable` bij `decisionTableRow`. Opgenomen: de clausule is een tabel
  van vijf regels en vraagt geen dialectkennis buiten die tabel. Niet getoetst: welke knoop een vorm maakt.

De lader leest `origin` in `benoembaar` (nieuw veld `bn_herkomst`); het `document`-record is ongewijzigd. Negatieve
gevallen, elk op de heruitgezonden `jutterswet-opgave-bedrag`, elk met `expect.txt` en `NOTE.md` en elk met precies
één bevinding: `wf121-herkomst-vorm-onbekend` (a), `-bron-onbekend` (b), `-zichzelf` (c), `-kring` (c),
`-soort-past-niet` (d).

### Heruitgezonden

Goldens (alleen `origin` erbij, regels +/-): `jutterswet-meldtermijn-strandjutter` (+7), `-opgave-bedrag` (+51 -3),
`-opgave-boven-berekening` (+102 -6), `-opgave-datum` (+51 -3), `-opgave-subtype` (+51 -3), `-opgave-vormen` (+169
-11), `-rubriektabel-rgs` (+16 -2), `-sluitdag-gemeld` (+102 -6), `-zwaarste-weging` (+51 -3). Elk ander document is
bytegelijk (gate 10, 11, 16); geen anchor-golden draagt een rgs-bron, dus `corpus/golden/anchor/` is onaangeroerd.
Geen case-bestand veranderde.

TOKA: `toka/casus/ir.json` (+31 -3: het startpuntanker en drie rijen van `Woonregio factor`); de slices met de hand,
met de serialiser van gate 2: `scenario-toka-46-woonregio-factor-1`, `-2`, `-3` (+24 -3 elk), `scenario-toka-43-
startpunt-vertrek` en `scenario-toka-44-startpunt-min-reserveringstermijn` (+7 elk, het anker uit de bepaling die de
slice zelf draagt). De andere 24 slices dragen het anker zonder de regel die het schrijft en krijgen geen origin.
Divergentie in `toka/README.md`, met een bijgewerkte zin in Well-formedness. Het verifierrapport van de dump is
ongewijzigd.

### Tests

Nieuw:
- `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/HerkomstOpDeDraadTest.java`, 9 tests: elke vorm op de
  draad, een geschreven anker zonder origin, de plaats vlak voor `designations`, een regel met de naam van de tabel,
  het document zonder de tabel is per rgs-golden het document zonder `origin` byte voor byte (en zonder gemaakte
  declaratie: geen `origin`), de spelling tegen de enums van het schema. Rood vóór de emitter: 7 van de 8 toen
  bestaande tests vielen tegen de oude emitter met de nieuwe `Spelling` (`een origin per gegenereerd element: [] ==>
  expected: <6> but was: <0>`); de spellingtest was groen.
- `backends/drools/src/test/java/dev/uchar/regelspraak/loader/HerkomstWitnessTest.java`, 6 tests: lezen, ook een rij
  en een startpunt; de weigering van (a) met plek en zin; wat de bron noemt weigert de lader niet; structureel lezen.
- `backends/drools/src/bronroute/java/dev/uchar/regelspraak/loader/HerkomstBronrouteTest.java`, 1 test (gate 15):
  per rgs-golden de tabel van de draad gelijk aan die van de bron, 125 goldens, 9 met een gemaakte declaratie. Hier
  en niet in de Maven-suite, omdat de Maven-build van de backend de resolver niet compileert.

Bestaande tests die het oude gedrag vastleggen en vallen; ongewijzigd op de branch, patch buiten de repository
aan de eigenaar geleverd:
- `OpgaveMarkeringTest.de_wire_draagt_de_lowering_in_de_spelling_van_het_uitgeschreven_patroon` en
  `op_een_datum_in_dagen_draagt_de_wire_het_datumtype_en_de_datumliteral`: de verwachte draadfragmenten krijgen
  `origin`.
- `StappenNaDeVerzamelingTest.het_gegenereerde_lid_is_de_handgeschreven_vorm`: de vergelijking laat `origin` weg,
  zoals `source`.
- `GegenereerdeNamenWitnessTest.een_model_uit_de_wire_heeft_geen_gegenereerde_elementen`: wordt
  `een_model_uit_de_wire_heeft_de_gegenereerde_elementen_van_de_frontend` met de zes ingangen van
  `jutterswet-opgave-subtype`.

### Gates

Met de testpatch toegepast (ongecommitteerd), alle 16 groen op `9e755428` (de boom met patch `aa3703a5`),
16:13:59–16:33:52; daarna raakte `d488ef85` alleen `spec/proposals/v1-naamtabel.json`, die SpellingTest (gate 10) en
de clingo-suite (gate 6) lezen, en gate 1, 6 en 10 zijn op `d488ef85` met patch (boom `392916ca`) opnieuw groen
(16:34:23–16:38:09). Wat daarna volgt, raakt alleen `backlog/`.

1 PASS; 2 348 bestanden valideren en round-trippen, 46008 objecten in 859 documenten in sleutelvolgorde (was 45141 in
854); 3 348 golden schoon, 511 negatief afgewezen (was 506), 354 op hun arm (was 349), 619 pointers (was 614); 4 873
case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite 2198 checks, 823 cases; 7 778 cases, 45 survivors; 8 50
rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1199 tests (was 1190, plus 9 nieuwe), 125 rgs-goldens
bytegelijk; 11 45 TOKA-bronnen bytegelijk, TOKA-suite 27 tests, 28 slices verifier-schoon, 29 TOKA-cases gespeeld; 12
639 s, 255 gespeeld op beide takken, JUnit 1584 (was 1578, plus 6 nieuwe); 13 43 twists over 24 goldens, 122 replays,
vocabulaireleg 18 replays, herkomstleg 9 goldens, 71 origins gestript, 142 replays; 14 150 checks, 2 goldens; 15 125
goldens op de bronroute, 70 unittests (was 69); 16 30 scenario-goldens, 102 case-bestanden en hun `ir.json`
bytegelijk.

Zonder de patch: gate 10 rood op precies de drie frontendtests, gate 12 rood op precies de ene backendtest
(gemeten met de suites, niet met een volle gaterun zonder patch). Nulmeting op `be3bbec0` (15:38:04–15:57:28): alle
16 groen.

### Beschermde paden in dit voorstel

`spec/IR-SPEC.md`, `spec/schema/uchar-ir-v1.schema.json`, `spec/verifier/lib/{ast,loader,checks}.ml`,
`scripts/check-reftype-derivation.sh`, `CLAUDE.md` (de beschrijving van gate 13), en onder het CODEOWNERS-net van
`/spec/` `spec/proposals/v1-naamtabel.json`. `corpus/golden/anchor/` niet.

### Voorgestelde rij voor `spec/CONSTRUCTS.md` (na rij 41)

| 42 | Origin of a declaration a producer made itself | the RegelSpraak frontend's lowerings (the opgave marker, a subtype's exception group, decision-table rows, a fold with steps after the set, a start point's anchor) make declarations under names no source declares; a consumer of the IR document (a decision service, a designations lint, a workbench) could not tell them from written ones, nor see their source | core (metadata); the `form` values `dialect(regelspraak)` | neither: a side table without semantics | v0 has none. **Proposed (2026-10-15, card `herkomst-op-de-draad`):** extension field `origin` (`Origin {form, from: OriginSource {kind, owner?, name}}`) just before `designations` on the nodes that carry it (IR-SPEC ch. 2.13); `form` ∈ `inputAboveComputation`, `exceptionForSubtype`, `decisionTableRow`, `stepsAfterSet`, `startPoint`; `kind` the nine designatable kinds (`namedForm` for a form) plus `decisionTable`, a source that is no declaration on the wire. WF-121 with four clauses and 5 negatiefs: (a) the closed sets, refused at load by every executor in the same sentence; (b) `owner` exactly on a scoped kind, and a source that resolves unless it is a `decisionTable`; (c) not itself, no cycle; (d) the kind the form names. The frontend writes it for every entry of `RegelSpraakModel.gegenereerd`; the Drools loader reads it back, equal to the frontend's table on every rgs golden. Witnesses: 9 rgs goldens, every form but `startPoint` (TOKA casus and the frontend suite); gate 13's origin leg holds the executors indifferent to the field stripped | out: the parts of a composite name (`B voor S`, `<member> via <roles>`), one `from` only, a `parts` list being additive; the Drools backend's manifest names (`defaultsstapel`, `versie`), no declarations; which node a form makes |

### Open

- De testpatch (vier tests in drie bestanden) moet vóór of met de merge landen; zonder haar zijn gate 10 en 12 rood.
- `python3 spec/proposals/v1-naamtabel-toets.py`, op `main` groen, houdt op deze branch twee bevindingen: de enums op
  `Origin.form` en `OriginSource.kind` horen bij geen familie van de sectie `waarden`. Die sectie legt de hernoeming
  van v0 naar L5 vast, `SpellingTest` pint haar op 16 families gelijk aan `Spelling.FAMILIES`, de clingo-suite leest
  haar ook, en deze enums hebben geen v0-waarde. Keuze voor de eigenaar: (1) twee identiteitsfamilies in `waarden`,
  in `Spelling.FAMILIES` en in `spec/verifier/lib/spelling.ml`, en de telling in `SpellingTest` naar 18 (een
  testpatch); (2) een lijst families zonder v0 in de naamtabel, die de toets aanvaardt (een wijziging van de toets);
  (3) `form` en `kind` als vrije strings in het schema, alleen gesloten door WF-121 (a), zoals `ContextValue.name`.
  De kaart koos niets daarvan; de toets is geen gate.
- Executors weigeren een onbekende vorm of soort bij het laden (WF-121 (a)). Dat gaat verder dan "geen semantiek"
  in de opdracht; de reden: de Drools-lader houdt de herkomst als enum en kan zo'n waarde niet bewaren, en de
  OCaml-executors doen hetzelfde zodat de drie executors één document gelijk behandelen. Alternatief: alleen de
  verifier weigert, en de Drools-lader laat het element dan uit de tabel, wat een stille terugval zou zijn.
- Geen corpus-golden draagt `startPoint`: geen golden-bron schrijft een startpuntbepaling. De vorm staat in de
  TOKA-dump en twee TOKA-slices en in `HerkomstOpDeDraadTest` en `HerkomstWitnessTest`.
- De delen van een samengestelde naam staan niet op de draad (zoals ontworpen); IR-SPEC 2.13 noemt een lijst `parts`
  als additief en niet gebouwd. Op `be3bbec0` draagt `RegelSpraakModel.gegenereerd` één bron per element; de delen
  staan in `RuleChecker.naamdelen`, privé.
- De emitter toetst niet of `from` in een gescopet scenariodocument staat. Gemeten: een scope "toetst de regel
  meldtermijn voor Strandjutter" draagt ook de groep `meldtermijn`, en is verifier-schoon; een scope in een document
  met een startpuntbepaling wordt al geweigerd; de andere vormen komen uit een attribuut of een tabel.
- WF-121 toetst niet welke knoop een vorm maakt (een `decisionTableRow` op een attribuut is schoon); de spec zegt dat.
- `main` staat op `fe8901fb`; de branch moet bij het landen gerebased worden (merge-tree zonder conflict).

## Landing (2026-10-15)

De eigenaar droeg de keuring van dit voorstel over aan de hoofdsessie: landen als het het
ontwerpadvies volgt. Gekeurd tegen dat advies; de twee afwijkingen (de soort `decisionTable`,
en executors die een onbekende vorm of soort bij het laden weigeren zoals elk onbekend veld)
aanvaard. De testpatch op de vier bestaande tests landt in `95f4bde7` (akkoord eigenaar). De
bevindingen van de naamtabeltoets zijn opgelost met optie b, keuze van de eigenaar
(`9665f7d0`: sectie `waarden_zonder_v0` naar het voorbeeld van `spec_zonder_v0`). Rij 42 staat in
`spec/CONSTRUCTS.md`. Gerebased op `fe8901fb` (kaart `waarnemer-vuur`) en daar met alle gates
opnieuw gedraaid vóór de merge.
