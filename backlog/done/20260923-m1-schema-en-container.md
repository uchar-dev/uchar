# M1 — het schema en de container (B1–B5 + V3): de eerste klapkaart van v1

Eerste kaart van fase 2 van `spec/proposals/V1-WIREMIGRATIE.md` (§7, de
M-tabel), op het startsein van de maintainer van 2026-09-23. M1 zet de
CONTAINER van de wire om en niets anders: de formaatnaam en het schema-id
(B1), de zes kaart-tabellen worden lijsten en de zelfnaam-verdubbeling
verdwijnt (B2), de inerte `regels`-stubs verlaten de wire (B3),
`diagnostics` blijft en krijgt zijn normatieve regel (B4, besluit V2 (ii)),
de acht zijkaarten van `Objecttype` worden ledenknopen (B5), en de root
nestelt tot `{format, model, rules, diagnostics}` (besluit V3 (b)). Alle
andere knopen, velden en waardespellingen blijven byte-voor-byte v0 tot hun
eigen M-stap (M2–M8).

Goal: één ff-landing waarna alle veertien gates groen zijn op de M1-vorm
van de wire, alle 769 documenten (corpus/golden 237, corpus/negatief 454,
corpus/rood 48, toka/ 30) in die vorm staan, de drie emitters hem schrijven, de
vier consumenten hem lezen, en de toka-zone meeverhuist (maintainerbesluit
2026-09-23: toka blijft tot v1 klaar is; gate 11 blijft in de set, de
casus-dump wordt door de frontend opnieuw geëmitteerd, de 29 replay-slices
gaan mechanisch mee, README-divergentie erbij). Deze kaart raakt
schema, verifier, ref, clingo, drools en alle drie de frontends tegelijk;
daarom lopen er GEEN parallelle zonekaarten naast (route b is gepauzeerd).

## Besluiten die deze kaart uitvoert (niet heropenen)

- **V1 (b)**: het formaat heet `uchar-ir-v1`; het schemabestand wordt
  `spec/schema/uchar-ir-v1.schema.json` (`$id`, `title`, `description`,
  `const` mee); het v0-schema verdwijnt (geen twee schema's naast elkaar —
  de klap is één klap, V2 (a)).
- **V3 (b)**: de root nestelt: `{ "format", "model", "rules",
  "diagnostics" }`. De modellaag is alleen de declaratieverzameling
  (IR-SPEC ch. 2); de regellaag is de rest.
- **V2 (a) + deelvraag B4 (ii)**: `diagnostics` BLIJFT op de wire, met een
  normatieve WF-regel (hieronder: WF-113). Het register (CONSTRUCTS §2.4
  "non-goal") en IR-SPEC ch. 2 ("build diagnostics … are not part of the
  IR") worden daarop bijgesteld — de registerregel door de hoofdsessie, de
  spec-zin door bouwer A2.
- **V14 (a)**: elke negatief-getuige die zijn regel verliest krijgt één van
  drie stempels — herschreven, vervangen, verwijderd-met-reden — in een
  NAAMDRAGENDE lijst (in `corpus/negatief/README.md`, sectie "Vervallen bij
  de v1-migratie", én in het dossier van deze kaart). De vloer
  `EXPECTED_MIN_NEGATIEF` (350) hoeft niet omlaag (454 − 21 = 433) en
  blijft dus staan; de nieuwe stand wordt gerapporteerd.
- **Naamprincipe van de M-stappen (hoofdsessie, 2026-09-23):** een M-stap
  hernoemt ALLEEN wat hij herstructureert, en gebruikt daarvoor de namen
  die IR-SPEC al draagt; hij verzint geen tussentijdse namen die in geen
  spec-tekst staan. Wat niet geherstructureerd wordt houdt zijn v0-naam en
  v0-waardespelling tot M8 (B6). Concreet voor M1: de root-sleutels en de
  acht modellijsten krijgen hun ch.-2-naam; `ObjectType` en zijn ledenknopen
  krijgen hun ch.-2.2-vorm; de `Domain`-wrapper zijn ch.-2.3-vorm. De
  knopen IN de andere lijsten (`Parameter`, `Feittype`, `Eenheidstelsel`,
  `Dimensie`, `Tijdlijn`, alle `Resolved*`) blijven v0, de sleutels onder
  `rules` blijven v0, en de enum-WAARDEN (`JAAR`, `BIJVOEGLIJK`, …) blijven
  v0 ook in de nieuwe ledenknopen — de waardespellingen zijn een gat in de
  naamtabel (P10 dekt knopen en properties) en gaan als genummerd open punt
  naar M8.

## De M1-vorm van de wire (normatief voor deze kaart)

Canonieke sleutelvolgorde = de `properties`-volgorde in het schema; het
migratiescript en alle drie de emitters schrijven exact die volgorde, zodat
gate 2's byte-round-trip en de byte-identieke her-emissie van gates 9, 10,
11 en 14 één en dezelfde vorm bewijzen. Serialisatie verder ongewijzigd
(twee spaties, `\uXXXX`-escapes, lege containers inline).

```
{
  "format": "uchar-ir-v1",
  "model": {
    "objectTypes":  [ ObjectType ],            // v0 objecttypen (map) → lijst, declaratievolgorde
    "factTypes":    [ Feittype ],              // v0 feittypen (map) → lijst; knoop v0
    "parameters":   [ Parameter ],             // v0 parameters (map) → lijst; knoop v0
    "domains":      [ Domain ],                // v0 domeinen (map) → lijst van wrappers (hieronder)
    "unitSystems":  [ Eenheidstelsel ],        // v0 eenheidstelsels (map) → lijst; knoop v0
    "dimensions":   [ Dimensie ],              // v0 dimensies (map) → lijst; knoop v0
    "timelines":    [ Tijdlijn ],              // v0 tijdlijnen, al een lijst; knoop v0
    "dayKinds":     [ string ],                // v0 dagsoorten, ongewijzigd
    "dagsoortPerEenheid": { … },               // v0, ongewijzigd (ch. 2.9 herstructureert later)
    "functionDefs": [ FunctionDef ]            // extensielijst, optioneel, ongewijzigd
  },
  "rules": {                                   // de v0-sleutels VERBATIM, in v0-volgorde
    "resolvedRegels": […], "resolvedDagsoorten": […], "objectCreaties": […],
    "verdelingen": […], "consistenties": […], "uniciteiten": […],
    "uniciteitUnies": […], "feitCreaties": […], "resolvedStartpunten": […],
    "regelGroepen": […],
    "prioritizedDefaults": […]                 // extensielijst, optioneel, ongewijzigd
  },
  "diagnostics": [ Diagnostic ]                // blijft; WF-113
}
```

`regels` (de stubs, B3) bestaat niet meer — niet als lege lijst, niet als
sleutel. De `required`-lijsten blijven wat ze in v0 waren (altijd
geschreven, eventueel leeg), de extensielijsten blijven optioneel en worden
heel of niet geschreven.

**ObjectType** (B5), in deze sleutelvolgorde:

```
{ "node": "ObjectType", "name": string, "supertype"?: string, "animate": boolean,
  "plural"?: string,                                   // v0 meervoud; null → afwezig
  "attributes": [ { "node": "Attribute", "name": string, "datatype": Datatype,
                    "plural"?: string,                 // v0 attribuutMeervoud[name]; null/afwezig → afwezig
                    "dimensions"?: [ string ],         // v0 attribuutDimensies[name]; minItems 1, een lege lijst wordt geweigerd (WF-004)
                    "timeline"?: { "node": "TimelineFacet",
                                   "granularity": "DAG"|"WEEK"|"MAAND"|"KWARTAAL"|"JAAR",   // v0-spelling tot M8
                                   "count": integer ≥ 1,                                  // v0 attribuutTijdlijnAantal[name]; afwezig → 1
                                   "timelineName"?: string } } ],                          // v0 attribuutTijdlijnNaam[name]
  "characteristics": [ { "node": "Characteristic", "name": string,
                         "kind": "BIJVOEGLIJK"|"BEZITTELIJK"|"ZELFSTANDIG",  // v0 kenmerkSoorten[name]; null/afwezig → ZELFSTANDIG (ch. 2.2: untagged = nominal); spelling v0 tot M8
                         "timeline"?: TimelineFacet } ] }
```

Een kenmerkfacet draagt geen `timelineName` (v0 had geen kenmerk-naamkaart;
de positie is gesloten, eigen def `CharacteristicTimelineFacet`). Een
`timeline` staat er precies dan wanneer de v0-granulariteitskaart een
sleutel voor het lid had (dat was al de temporaliteitsbeslissing van alle
lezers, WF-059 armen 2/3). Een v0-waarde die de v1-vorm niet kan dragen —
een `null` als attribuutnaam of kenmerknaam, een `null`-datatype buiten de
WF-059-getuige, een zijkaartsleutel zonder lid (de WF-034 (p)-vorm) — laat
het migratiescript LUID falen met bestandsnaam en positie; het script
repareert niets stilzwijgend. De documenten waarop dat gebeurt zijn per
constructie de getuigen die vervallen (hieronder) of een fout in de meting
van deze kaart, en beide worden gerapporteerd.

**Domain** (B2, ch. 2.3): `{ "node": "Domain", "name": string, "datatype":
Datatype }`, waarbij het datatype op de DECLARATIEPOSITIE géén zelfnaam
draagt (`Enumeratie.domein`, `SumType.domain`, `StructType.domain` vallen
daar weg; `Numeriek` had er geen). Op elke REFERENTIEPOSITIE (attributen,
parameters, expressies — gemeten 700+ dragers) blijft het veld staan zoals
het is: daar is het de domeinverwijzing van ch. 2.4 (`domain: string`, een
oplosbare referentie, WF-003) en geen zelfnaam; zijn hernoeming is B6. Hoe
het schema de declaratiepositie zonder zelfnaam typeert (een aparte `$def`
of een `not required`-variant) kiest bouwer A1 en motiveert hij in de
schema-README; het criterium is B2's principe: nergens op de wire een tweede
spelling van één naam.

## WF-113 — `diagnostics` is het eigen verslag van het document

Tekst voor IR-SPEC (bouwer A2 landt hem in ch. 2 bij de root-container en
in de WF-index; Engels, zoals de spec):

> **WF-113** `diagnostics` is the producer's own account of what stayed
> unresolved in a document it did write. (a) Every entry carries
> `severity: WARNING`, a non-empty `message` and an integer `line`;
> `ERROR` and `INFO` are not wire severities — an ERROR means the producer
> refused to write the document, and INFO is tooling talk (measured
> 2026-09-23: zero carriers in 817 documents) — so the schema enum shrinks
> to the one value and the verifier refuses the other two by name. (b)
> Every `Onopgelost` node MUST be accounted for: the document carries at
> least one diagnostic whose `message` contains that node's `bron`
> verbatim (the 2026-09-19 decision "every surviving marker carries a
> WARNING", made normative and checkable). (c) No consumer reads
> `diagnostics` for evaluation: the list carries no semantics, exactly as
> the ch. 3 paragraph says of the former `regels` stubs — stated, not
> fenced, because a rule on consumers has no document to reject.

Getuigen: passend — de twee rode documenten met een marker
(`corpus/rood/wf044-onopgelost-guard`, `wf044-onopgelost-reached`) krijgen
elk hun WARNING met de `bron` erin (rood is verifier-gegatet, dus dit zijn
de eerste corpusdragers van (b)); plus één golden uit RegelSpraak-bron met
een niet-lege diagnostics-lijst uit een echte producentroute (bouwer C: een
beslistabel met alleen een kop, de vorm die de frontend vandaag al als
WARNING meldt — golden `jutterswet-…` naar keuze, met case en verwachtingen
zodat gates 5/6/12 hem spelen). Falend — `wf113-severity-error`,
`wf113-severity-info`, `wf113-lege-message`, `wf113-marker-zonder-verslag`
(een marker, lege lijst) en `wf113-verslag-noemt-marker-niet` (een WARNING
die de `bron` niet bevat). Catala- en F-logic-emitters: meten of zij ooit
een `Onopgelost` schrijven; zo ja, dan schrijven zij vanaf deze kaart de
bijbehorende WARNING (bouwer B), zo nee, dan staat dat in het Result.

## Regels en getuigen die vervallen (V14 (a), de stempellijst)

Alle 21 met stempel **verwijderd-met-reden**: de v1-vorm maakt de
overtreding onschrijfbaar, dus er is geen opvolgerregel en geen
opvolgergetuige. Bouwer A2 verwijdert ze, schrijft de lijst, en meet
vooraf per getuige dat het document werkelijk niet migreerbaar is (het
script faalt erop) of na migratie zijn onderwerp kwijt is — een getuige die
NA migratie nog steeds op een andere regel valt wordt niet verwijderd maar
HERSCHREVEN op die regel, en dat staat dan als stempel.

- **WF-094, vier clausules** (10): `wf094-zelfnaam-{dimensie,
  eenheidstelsel, feittype, objecttype, parameter}` en
  `wf094-zelfnaam-null-{dimensie, eenheidstelsel, feittype, objecttype,
  parameter}` — er is geen kaartsleutel meer om tegen te kruisen. WF-094
  verdwijnt uit IR-SPEC en de verifier; de ch.-2.1-zin die de v0-
  denormalisatie beschrijft wordt geschiedenis ("until M1 …").
- **WF-034 clausule (p)** (9): `wf034-wees-{attribuutdimensies,
  attribuutgranulariteit, attribuutmeervoud, attribuuttijdlijnaantal,
  attribuuttijdlijnnaam, kenmerkgranulariteit, kenmerksoorten,
  kenmerktijdlijnaantal}` en `wf034-zijtabel-kapitalisatiewees` — een facet
  zonder lid is niet meer te schrijven. Clausule (p) verdwijnt; de andere
  armen van WF-034 blijven onaangeraakt.
- **WF-059 armen 2 en 3** (2): `wf059-attribuut-zonder-granulariteit`,
  `wf059-kenmerk-zonder-granulariteit` — `TimelineFacet.granularity` is een
  verplichte enum, dus een facet zonder raster is niet te schrijven. Arm 1
  (het datatype-loze attribuut, `wf059-attribute-without-datatype`) BLIJFT:
  het is een semantisch defect in een laadbaar document en de getuige
  wordt herschreven op de v1-vorm (stempel herschreven), met de eis dat de
  verifier hem nog steeds op WF-059 afwijst en niet op een laadfout.

Wat NIET vervalt en per getuige gecontroleerd wordt: alle overige 433
negatiefs migreren mechanisch en vallen daarna op precies dezelfde regel
als hun mapnaam declareert (gate 3). Een getuige die door de migratie van
regel wisselt is een bevinding, geen ruis.

## Acceptatie

Elke fase sluit met de genoemde gates groen op haar eigen branch; de kaart
sluit met alle veertien groen op de samengevoegde branch, en:

- (a) `python3 spec/proposals/v1-migratie/m1.py --check` meldt 0 v0-
  documenten in de vier zones en de pariteitszone; het script is
  idempotent (een tweede run wijzigt geen byte) en weigert een document dat
  al v1 is niet, maar laat het staan.
- (b) Gate 2: metaschema-valide `uchar-ir-v1.schema.json`; alle golden- en
  rood-documenten valideren en round-trippen byte-identiek; `SCHEMA` in
  `scripts/check-ir-schema.sh` wijst naar de nieuwe naam; de
  zelftestrijen van het script draaien op de v1-vorm; `EXPECTED_MIN_DOCS`
  blijft 192 (geen document verdwijnt uit golden/rood; de rood-zone krijgt
  er geen bij).
- (c) Gate 3: 237 (+1 met de WF-113-golden) goldens en 48 rood-documenten
  verifier-schoon (286 bestanden); 454 − 21 + 10 wf113 + 1 wf004 = 444
  negatiefs elk afgewezen op hun eigen regel; de stempellijst in
  `corpus/negatief/README.md` noemt alle 21.
- (d) Gate 4 ongewijzigd groen (658 case-files, +2 van de WF-113-golden) — het casusformaat
  verandert niet; als de gate tóch beweegt is dat een meting in het Result.
- (e) Gates 5, 6, 7, 8: ref-suite ≥ 931, 608 (+ de nieuwe) replays, clingo
  523 / 608, noop 571 + 37, rood 48 / 96 (49 / 98 zodra
  `clingo-parameter-failfast` geland is en de branch erop is gerebased) —
  GEEN uitkomst beweegt: M1 verandert geen betekenis, dus elke pin van
  gates 5/6/12 is een gelijkheid vóór en na.
- (f) Gate 9: 12 catala-goldens byte-identiek; gate 14: 2 flogic-goldens
  byte-identiek, suite ≥ 130; gate 10: 70 (+1) rgs-goldens byte-identiek,
  JUnit ≥ 492.
- (g) Gate 11: casus-dump byte-identiek opnieuw geëmitteerd (45
  fragmenten, holdouts ongewijzigd), 29 slices schema-valide en
  round-trip, differentiële replay 29/29 met dezelfde klassen als vóór M1
  (manifest ongewijzigd), TOKA-suite ≥ 27 tests; `toka/README.md` krijgt
  onder "Deliberate divergences" de M1-regel: de dump staat in de M1-vorm
  van `uchar-ir-v1`, de toolchain schrijft v0, dus voor de container is
  dit geen toolchain-dump meer — wat de vergelijking bewijst is "frontend
  ≡ de vastgelegde toestand" (maintainerbesluit 2026-09-23, toka blijft
  tot v1 klaar is).
- (h) Gate 12: `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` gelijk aan de
  stand op main bij de rebase (184/163 vandaag; 184/163 na de clingo-
  kaart), JUnit ≥ 653 zonder skips; de kanaallijsten ongewijzigd. Gate 13:
  131 twists, alle legs op hun eigen ongetwiste regel.
- (i) `python3 spec/proposals/v1-naamtabel-toets.py` rc 0 op het nieuwe
  schema: de rijen van de bij M1 gelande namen staan als `identiteit` met
  een `geland: "M1"`-noot (of een vorm die de toets aanvaardt en de bouwer
  motiveert), `diagnostics`/`severity`/`message`/`line@Diagnostic` van
  `vervalt` naar `identiteit` (V2 (ii)), en de nieuwe knopen `ObjectType`,
  `Attribute`, `Characteristic`, `TimelineFacet`, `Domain` hebben een rij.
- (j) Herpin in `regelspraak-java` (hoofdsessie, na de landing): `SPEC_REV`
  naar de M1-kop, vendored slice byte-identiek, `make uchar-conformance`
  groen, `make test` ≥ 553, `mvn -o -B test` groen — de toolchain-tests die
  de v0-container lezen (25 bestanden buiten vendor) worden op de M1-vorm
  herschreven, spec-first.

May touch (per bouwer, disjunct — zie werkverdeling): `spec/schema/**`,
`spec/proposals/v1-migratie/**`, `spec/proposals/v1-naamtabel.json` (+
toets), `spec/IR-SPEC.md` (alleen de genoemde plekken), `spec/verifier/**`,
`corpus/**` (migratie; nieuwe getuigen; verwijderde getuigen; de twee
rood-diagnostics), `toka/**` (migratie van de slices, de her-emissie van de
casus-dump, README, en de testbron alleen waar hij de container leest),
`ref/**`, `backends/clingo/**`, `backends/drools/**`, `frontends/**`,
`scripts/check-ir-schema.sh` (SCHEMA-pad en zelftestrijen), en van de
andere gate-scripts ALLEEN wat aantoonbaar de containervorm leest (gate 13
navigeert paden; gate 11/12 lezen niets van de vorm) — elke gate-script-
wijziging wordt in het Result per regel genoemd. NIET: `spec/CONSTRUCTS.md`,
`spec/PLAN.md`, `CLAUDE.md`, `WAY-OF-WORKING.md`, `.github/**`.

Out of scope: elke andere B-delta (B6–B28), de enum-waardespellingen, de
`rules`-sleutels, de `Resolved*`-knopen, `dagsoortPerEenheid`/ch. 2.9, de
verre-rolknoop, het typesysteem; P11; alles wat semantiek wijzigt. Een
bouwer die een betekenisverschil meet (een golden die ná M1 anders
uitkomt) STOPT op dat punt, legt de meting in het Result en gaat verder met
de rest — dat is precies de nulmeting die de M-slicing moet leveren.

## Werkverdeling

Vier bouwers, één kaartbranch `card/m1-schema-en-container`. A1 eerst;
daarna A2, B en C parallel op eigen branches vanaf A1's kop; de
hoofdsessie voegt ze serieel samen (ff/rebase), draait de veertien gates,
en zet de lens op de unie.

- **A1 — de wire.** `spec/schema/uchar-ir-v1.schema.json` (v0-schema
  weg), `spec/schema/README.md`, `scripts/check-ir-schema.sh`,
  `spec/proposals/v1-migratie/m1.py` (v0 → M1-vorm, luid falend, idempotent,
  `--check`), alle 817 documenten gemigreerd (corpus + toka/cases/**/ir.json +
  toka/casus/ir.json mechanisch — bouwer C vervangt de casus-dump daarna
  door de her-emissie en meet dat die byte-identiek is met de mechanische
  migratie; een verschil is een bevinding over het script of de emitter).
  Sluit met gate 2 en gate 4 groen; gate 3 en de rest zijn dan rood en dat
  is verwacht. Rapporteert per zone de aantallen en elk document waarop het
  script faalde (verwacht: precies de 21 getuigen).
- **A2 — verifier en spec.** `spec/verifier/**` (loader/ast/checks op de
  M1-vorm; WF-094 en WF-034 (p) en WF-059 armen 2/3 eruit; WF-113 erin),
  `spec/IR-SPEC.md` (ch. 2 root-tabel + `format`/`model`/`rules`/
  `diagnostics`; ch. 2.2 migratienota "geland M1"; ch. 3 `regels`-alinea →
  geschiedenis; WF-index), de 21 verwijderingen + stempellijst, de 5
  wf113-negatiefs, de 2 rood-diagnostics, `wf059-attribute-without-datatype`
  herschreven, naamtabel + toets. Sluit met gate 3 groen (en 2, 4).
- **B — de OCaml-kant.** `ref/lib/case.ml`, `ref/lib/eval.ml`,
  `ref/test/test_ref.ml`; `backends/clingo/lib/{model,normalise,translate}.ml`,
  `backends/clingo/test/test_clingo.ml` (34 ingebedde documenten);
  `frontends/catala/lib/{emit,translate}.ml`; `frontends/flogic/lib/{emit,
  lower}.ml`, `frontends/flogic/test/suite.ml`. Sluit met gates 5, 6, 7, 8,
  9, 14 groen.
- **C — de Java-kant.** `frontends/regelspraak` (`IrEmitter` en wat de
  emitter voedt; het in-memory model mag zijn zijkaarten houden — de
  ledenknopen zijn de WIRE-vorm; de tests met ingebedde v0-documenten),
  `backends/drools` (`IrLoader`, `ContractValidator`, `FactModelGenerator`,
  beide transpilers, `CaseRunner`, ~50 getuigentests met ingebedde
  documenten — mechanisch om te zetten met A1's script over de
  tekstblokken), `toka/casus/ir.json` opnieuw geëmitteerd, `toka/README.md`,
  `toka/src` waar hij de container leest, de WF-113-golden uit rgs-bron.
  Sluit met gates 10, 12 en de obligaties 1–3 van gate 11 groen; gate 11
  obligatie 4 en gate 13 pas op de unie (die hebben ref en clingo nodig).

Elke bouwer: commit klein en vaak met de branch altijd bouwbaar, houdt zijn
context zuinig (geen hele corpusbestanden lezen; meten met python), maakt
GEEN kaartjes in `backlog/new` (restpunten als genummerd open punt in het
Result), pusht en merget nooit, raakt CONSTRUCTS/PLAN nooit, en eindigt met
een Result-blok dat de stand, de gemeten aantallen, de gate-uitvoer en de
open punten draagt — ook als hij halverwege op zijn contextgrens stopt,
zodat een vervolgbouwer verder kan.

## Registerwerk bij de landing (hoofdsessie)

`spec/CONSTRUCTS.md`: §2.4 `RegelSpraakModel` → "geland M1: `{format,
model, rules, diagnostics}`"; `Severity`/`Diagnostic` van non-goal naar
"blijft, WF-113 (V2 (ii))"; rij 11 (`Objecttype`-zijkaarten → ledenknopen:
geland M1, waardespellingen tot M8); rij 37/lader waar de zelfnaam
genoemd wordt; de knoopinventaris voor `ObjectType`, `Attribute`,
`Characteristic`, `TimelineFacet`, `Domain`. `spec/PLAN.md`: de M-tabel
M1 → geland met datum en hash; V12 aangevuld met het besluit van
2026-09-23 (toka blijft tot v1 klaar is, gate 11 verhuist mee als
regressiegate). `spec/proposals/V1-WIREMIGRATIE.md` §7 M-tabel: M1
doorgestreept met verwijzing (bouwer A2 mag die ene regel zetten).

## Result

**Geland (L1 van de v1-klap, 2026-09-23/24; basis main 82c1f088, gerebased op 16234880; de landingscommit is deze kaartverplaatsing zelf).**
Vier bouwers (A1 wire, A2 verifier/spec, B OCaml, C Java), één lensronde
met negentien vondsten (vijf blokkerend), één fixronde in drie parallelle
bouwers, herkeuring door dezelfde lens. Alle veertien gates groen op de
unie; de branch stond op de main-tip, dus de unie-run is de main-run.

**De wire.** `uchar-ir-v1`, root `{format, model, rules, diagnostics}`;
de zes tabellen als lijsten in `model` (ch.-2-namen; `dagsoortPerEenheid`
en `functionDefs` ongewijzigd erin); `rules` met de v0-sleutels verbatim;
`ObjectType {node, name, supertype?, animate, plural?, attributes,
characteristics}` met `Attribute {node, name, datatype, plural?,
dimensions? (minItems 1), timeline?}`, `Characteristic {node, name, kind,
timeline?}` (facet zonder `timelineName`: gesloten positie),
`TimelineFacet {node, granularity, count, timelineName?}`; `Domain {node,
name, datatype}` waarbij het datatype op de declaratiepositie geen
zelfnaam draagt (`domainDatatype`-unie) en op referentieposities de
domeinverwijzing van ch. 2.4 blijft; `naam` non-null op Parameter,
Eenheidstelsel, Feittype en Dimensie; `regels`-stubs weg; `diagnostics`
blijft met WF-113. Enum-waarden en alle andere knopen byte-voor-byte v0.
Canonieke sleutelvolgorde = schema-`properties`-volgorde voor de M1-knopen
(23 v0-erfenisknopen in handgeschreven goldens wijken af; gate 2 toetst de
volgorde niet — open punt).

**Migratie.** `spec/proposals/v1-migratie/m1.py` (`--all`, `--check`,
`migrate_bytes`): 747 van 769 documenten mechanisch (golden 237, negatief
432, rood 48, toka 30), idempotent, luid falend met JSON-pointer; de 22
weigeringen waren de 21 stempelgetuigen plus `wf059-attribute-without-
datatype` (met de hand herschreven, valt nog op WF-059). Onafhankelijke
terugvouwing door de lens: 0 onverklaarde verschillen. Normalisaties:
1140 stubs uit 565 documenten; `plural` afwezig bij null (103); `count` 1
bij ontbrekende aantalkaart (111 + 46); `kind` ZELFSTANDIG (1); 30
Numeriek-domeinen kregen hun sleutel als naam.

**WF-113** (V2 (ii)): (a) alleen WARNING, niet-lege en niet-blanco
`message`, `line` ≥ 0 (0 = geen bronpositie); (b) elke `Onopgelost` met
niet-lege `bron` heeft een diagnostic waarvan de message die `bron`
letterlijk bevat; (c) geen consument leest de lijst. Verifier op alle
markerposities (lens: 101/101). Producent: `IrEmitter.emit` weigert elk
model met een niet-WARNING (14 tests herschreven), marker-WARNINGs eindigen
op de `bron`, JUnit-toets per markerroute (`Verslag`, `MarkerVerslagTest`);
catala/flogic bouwen nooit een marker en weigeren nu luid wat ze niet
schrijven. Golden `jutterswet-kaletabel` (WARNING uit een beslistabel met
alleen een kop; speelt ref/clingo/Drools, pins 184/163 → 185/164);
negatiefs `wf113-severity-error/-info/-null`, `-lege-message`,
`-message-blanco`, `-marker-zonder-verslag`, `-verslag-noemt-marker-niet`,
`-marker-bron-null`, `-marker-bron-leeg`, `-line-negatief`; de twee rode
markerdocumenten dragen hun WARNING.

**Stempels (V14 (a)), lijst in `corpus/negatief/README.md`:** 21 ×
verwijderd-met-reden (WF-094 ×10, WF-034 (p) ×9, WF-059 armen 2/3 ×2 —
alle in het schema onschrijfbaar), 1 × herschreven (WF-059 arm 1); erbij
`wf004-lege-dimensielijst`. Negatief-stand 454 → 444; vloer 350 blijft.
Verifieruitvoer van de 433 gemigreerde getuigen byte-gelijk vóór en na;
geen getuige wisselde van regel of arm. WF-094, WF-034 (p) en WF-059
armen 2/3 uit IR-SPEC en verifier (geschiedenisregels blijven).

**Consumenten en emitters.** Gedeelde OCaml-lader (`spec/verifier/lib`,
ook ref/clingo/flogic) op de M1-vorm; verifier-meldingen als M1-JSON-
pointers (307 NOTE.md-citaten mee); Drools `IrLoader` strikt (`node`,
exacte sleutels, null-optionelen geweigerd, JSON-typen, WF-001/002-
naamsleutel, `Documentfout` met pointer, 31 laderweigeringsgetuigen), elke
telling bewaard; catala- en flogic-emitters en `IrEmitter` schrijven de
M1-vorm in canonieke volgorde; toka-casus her-emissie byte-identiek aan de
mechanische migratie (README-divergentieregel M1); gate 13 ongewijzigd.

**Gate-scripts (per regel):** `check-ir-schema.sh` r. 2 en 21 (SCHEMA →
`uchar-ir-v1.schema.json`) + 22 vormzelftestrijen; `check-drools.sh`
EXPECTED_PLAYED 184 → 185, EXPECTED_DRL_PLAYED 163 → 164 (de WF-113-golden).
Buiten de may-touch, gemeld: `spec/RUNTIME-SEMANTICS.md` r. 3945 (alleen de
schemabestandsnaam); `spec/proposals/PRIORITY-RELATION.md` r. 60.

**Lens-herkeuring:** MERGEN; negen nieuwe adviespunten uit de fixronde in
een C-naloop gesloten (supertype-opzoeking op naamidentiteit; `count` buiten
het int-bereik als Refusal; emit-bewaking ook op INFO/null/blanco/`line` < 0;
`Verslag` en emitter weigeren een marker met lege of null `bron`;
`Documentfout`-plek op het ouderobject zoals de OCaml-lader; knoopcontrole
op Diagnostic, datatype-`node`, `dayKinds`/`dagsoortPerEenheid`, `format` en
`functionDefs` eerst op vorm; één normfunctie `Namen.norm`; gate 12 adviseert
bij `Documentfout` "repair the document — never an exclusion",
`scripts/check-drools.sh` r. 1163; regel 0 = "de producent meldt geen
bronpositie", ook een frontend-WARNING zonder token).

**Gates op de landing (alle veertien, elk los):** 1 PASS; 2 PASS 286
bestanden, 26 + 22 zelftestrijen; 3 PASS 286 schoon / 444 negatiefs; 4 PASS
658 case-files; 5 PASS ref-suite 932 / 610 replays; 6 PASS suite 523 / 610;
7 PASS 573 + 37; 8 PASS 48 / 96; 9 PASS 207 / 12; 10 PASS 507 tests / 71
goldens; 11 PASS 45 fragmenten, 27 tests, 29 slices, replay 29/29/29; 12
PASS played 185, DRL 164, JUnit 679; 13 PASS 131 twists / 128 replays; 14
PASS 135 / 2.

**Correcties op de kaarttekst:** 769 documenten (niet 817; rood was dubbel
geteld), 237 goldens (285 = golden + rood), gate 4 658 case-files (+2),
26 zelftestrijen, 2 flogic-goldens; `dimensions` leeg wordt geweigerd, niet
"afwezig".

**Open punten (genummerd, hoofdsessie weegt ze bij L2–L5):**
1. B3-namen: 26 echte regelnamen stonden alleen in de stubs (6 in 4
   goldens, bijv. de naam van een uitzonderingsregel) — naam op `Default`
   in L4? (normvraag, standaard: nee, `bron`-label volstaat).
2. Sleutelvolgorde-toets in gate 2 vraagt eerst een besluit over de 23
   v0-erfenisknopen (Default 10, PrioritizedDefaults 8, Functie 3,
   Kwantorvouw 1, Let 1; 2 uit een producent).
3. OCaml-bibliotheeknaam `uchar-ir-v0`/`Ir_v0` → L5.
4. Vijf verifierpaden zijn nog geen pointer (`…/datatype/velden/<naam>`,
   `…/parameters/<naam>`, een ontbrekend veld); vier NOTE-citaten bewust
   niet; metingscitaten met v0-paden in IR-SPEC (WF-089, WF-100) als
   geschiedenis.
5. Misvormde knopen aan de regelkant blijven in Drools een Refusal
   (EXCLUDE) tot hun eigen landing; `functionDefs`-weigeringstekst
   ongewijzigd (drie goldens hangen eraan).
6. Rood-document uit markerroute (c) niet gemaakt (Drools EXCLUDE → vraagt
   een regel in de uitsluitlijst van `check-rood.sh`).
7. `ref/lib/case.ml` 291–318 noemt v0 inhoudelijk (kalenderbuckets) en de
   enumspelling tot L5.
8. Dode/hulpcode: `Emissie.zonderFouten`-patroon in 11 frontendtests;
   `MarkerVerslagTest` dwingt per familie, niet per plek.
9. Testwijzigingen (gemeld, onder kaartmandaat): frontend 14 + 11 tests,
   Drools 59 klassen mechanisch + 4 herschreven + 2 nieuwe klassen,
   ref/clingo/catala/flogic-suites (embedded documenten, hulpen,
   getuigen).
10. Drools-lader één stap strenger dan de OCaml-lader: een Diagnostic met
    null-ernst of null-melding is daar een laadfout waar de verifier hem
    op WF-113 bij naam weigert; `isBlank()` telt ruimer dan `String.trim`;
    een onbekend veld wordt op het veld gemeld (OCaml: het ouderobject);
    `functionDefs` alleen op lijstvorm gecontroleerd; de tekst "unknown
    model section" bewust ongewijzigd (drie goldens in de kanaallijsten).
