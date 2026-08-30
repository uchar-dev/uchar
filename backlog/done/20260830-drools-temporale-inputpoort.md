# Drools: de temporele input-poort (gate-12-dekking, familie 1)

Goal: De case-inputlaag van `backends/drools` (`cases/CaseRunner.java`) draagt
de temporele vormen die de geporteerde productieruntime allang kan
(`List<Segment>`-beanvelden, tijdlijnalgebra in `RegelSpraakRuntime`): (a) een
timeline-JSON-attribuutinput wordt `List<Segment>` op de bean; (b) een
scalaire input op een temporeel attribuut volgt de asOfDate-bucket-conventie
van `corpus/golden/README.md`, exact zoals `ref` die leest (de scalar is de
bucketwaarde die uitsluitend de dedicated collapse bedient — nooit een
punt-lezing); (c) `characteristicTimelines` worden het
`List<Segment>`-kenmerkveld; (d) de `attributeTimeline`-read-back vergelijkt
segmentlijsten (van/tot exact, waarde scale-safe met `compareTo`). Daarmee
spelen de vijf getroffen goldens — `jutterswet-penningjaar`,
`jutterswet-bakenmeester`, `jutterswet-getijdenwacht`,
`synth2-clip-over-prorata`, `synth2-clip-cond-over-prorata` — tegen exact de
gepinde ref≡clingo-verwachtingen: gate 12 van 71 naar 76 played. Elke échte
waarde-divergentie die de poort blootlegt gaat reden-dragend in
`EXCLUDE_KNOWN` van `scripts/check-drools.sh` of wordt een genummerde
maintainersvraag — nooit wordt een verwachting bewogen.

Acceptance:
- Rood-eerst: vóór de fix de vijf EXCLUDE-regels getoond (`java -jar
  backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar <ir.json>
  <case*.json>` per golden); na de fix PLAYED.
- `bash scripts/check-drools.sh` → PASS met played 76, auto-excluded 58,
  value-divergence 3, no-case 1. Speelt een van de vijf niet door een échte
  divergentie, dan reden-dragend in `EXCLUDE_KNOWN` én benoemd in de Result.
  *(Geamendeerd bij de landing, 2026-08-30: de poort liet ACHT goldens fire
  bereiken, niet vijf — drie extra speelden mee (dubbelwacht, nummerregister,
  strandregister) en drie legden een reeds bestaand backend-defect bloot en
  staan reden-dragend in `EXCLUDE_KNOWN` (borgdeel, synth2-prorata-breed,
  dagentelling; zie de volgkaart 20260830-drools-fixpunt-en-ch78). De
  gate-uitkomst is daarmee played 79, auto-excluded 52, value-divergence 6,
  no-case 1.)*
- `bash scripts/check-rood.sh` → PASS; de vijf temporeel-getinte entries in
  de Drools-exclusielijst eerlijk bijgewerkt: een case die nu écht FAULT
  verliest zijn entry en pint zijn `expectedFault.drools`-fragment in de case
  (ref/clingo-fragmenten onaangeroerd); een NOFAULT wordt gehercategoriseerd
  met reden; `vintage-uncovered-date` blijft (TemporalRead-loaderweigering,
  v1-vorm).
- Alle twaalf gates groen; gates 2/5/6 met ongewijzigde getallen; geen
  ref/clingo-verwachting, pin of WF-nummer gewijzigd.
- De weigering voor gedimensioneerde input (`Map`-velden) blijft staan met
  een gesplitste, precieze reden — de huidige tekst claimt
  temporal/dimensioned in één adem en wordt onwaar zodra de temporele arm
  opengaat.

May touch:
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
`backends/drools/src/test/**` (nieuwe witness-tests),
`scripts/check-drools.sh` (uitsluitend een eventuele
`EXCLUDE_KNOWN`-entry), `scripts/check-rood.sh` (de Drools-exclusielijst),
`corpus/rood/*/case*.json` uitsluitend voor het TOEVOEGEN van
`expectedFault.drools`, `backlog/`.

Out of scope: gedimensioneerde attribuutinput (geen corpus-getuige; de
weigering blijft luid), granular (timeline) parameters (`zetGlobals`), de
TemporalRead-expressienode (v1-vorm; `anchor/jutterswet-aanmeldquotum` en
`anchor/jutterswet-penningvintage` blijven loader-EXCLUDE), objectcreatie,
wederkerig self-relation (volgkaart
`backlog/new/20260830-drools-wederkerig-selfrelation.md`), elke wijziging aan
ref/clingo/verifier/spec/corpusverwachtingen.

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. De temporele case-inputlaag
van `backends/drools` is open: timeline-JSON → `List<Segment>` met ref's
load-validaties gespiegeld (≥1 segment, from<to, beide grenzen op het
granulariteitsraster, geordend/niet-overlappend), scalar-op-temporeel →
het asOfDate-emmersegment (JAAR/MAAND kalendergebonden; DAG/WEEK/KWARTAAL
en scalar-zonder-asOfDate luid geweigerd — zonder corpusgetuige, alleen
door lezing geverifieerd), `characteristicTimelines` → dag-precieze
true-period-segmenten met waarde null, en een
`attributeTimeline`-read-back-vergelijker (lengte, van/tot exact, waarde
`compareTo==0`, index-voor-index; ordeblindheid is niet-observeerbaar —
beide zijden komen geordend binnen — en wordt dus nergens geclaimd). De
gedimensioneerde arm bleef een luide weigering met gesplitste reden.

Gate 12: 71→79 played / 63→52 auto-excluded / 3→6 value-divergence / 1
no-case. Acht goldens bereikten fire i.p.v. de voorziene vijf: de vijf
kaartgoldens spelen alle tegen hun ongewijzigde pins, plus dubbelwacht,
nummerregister en strandregister; drie legden reeds bestaande
backend-defecten bloot en staan reden-dragend in `EXCLUDE_KNOWN`
(borgdeel + synth2-prorata-breed: exec-model-non-terminatie — de
§9.9-freshness-expr laat ongeankerde temporele writes class-reactief
terwijl de `rsTijdreeksGelijk`-garde alleen geankerde dekt; dagentelling
case4: de ch.-7.8-standing-target-arm ontbreekt in dit backend). Beide
klassen — (c) terminatiedefect, (d) implementatiegap tegen besloten spec —
staan benoemd in de banner en zijn gepaard gecard in
`backlog/new/20260830-drools-fixpunt-en-ch78.md` (verwacht 82/3 na die
kaart). Geen verwachting, pin of WF-nummer bewogen (beide lenzen
onafhankelijk bevestigd); corpus, spec, ref, clingo, toolchain en toka/
onaangeroerd. Gate 8: vier temporeel-getinte Drools-entries eerlijk
bijgewerkt — twee EXCLUDE-redenen gecorrigeerd (scalar zonder asOfDate
heeft geen emmer), twee hercategoriseerd naar NOFAULT (geen
ariteitshandhaving op temporele sleutelcomponenten waar ref/clingo de
sleutelvorm afwijzen); geen rood-case werd FAULT, dus geen
`expectedFault.drools` gepind en geen corpus/rood-bestand geraakt.

Lens 1 (3 MAJOR, 6 MINOR) bewees per mutatie dat de
read-back-vergelijker onbewaakt was en twee prozaclaims onwaar; fixronde
1 bracht de `speelCase`-seam met zes vergelijker-witnesses (blinde-mutant
rood herbewezen), de gecorrigeerde ref-beschrijving (ref wéígert
puntlezingen, ch. 3.3) en hardere case-defect-kanalen
(`IllegalArgumentException` i.p.v. `Excluded` voor onbekend label en
niet-temporele vormen — regressievrij, door lens 2 corpus-breed
bevestigd). Lens 2 (1 MAJOR, 2 MINOR) ving de nog onbewaakte van-arm; de
van-witness is rood/groen bewezen en de ordeclaim getrimd tot het
falsifieerbare. JUnit-suite: 20/0 (17 witness + 3 roundtrip).

Bewust gelaten: de DAG/WEEK/KWARTAAL- en scalar-zonder-asOfDate-armen aan
de golden-kant zijn corpusgetuigeloos; granular (timeline) parameters en
gedimensioneerde input blijven dicht; de witness-suite hangt aan `mvn
package`/CI, niet aan een gate-testtelling (gate-10-model —
volgkaartmateriaal); de ariteitsrestrictie op temporele sleutels (de twee
NOFAULT-rood-cases) heeft geen volgkaart. Alle twaalf gates integraal
groen op de eindstand (160/168, 396, 374, 374, 338+36, 22, 169+11,
144+23, 29-toka met Drools 0/29, 79/52/6/1).
