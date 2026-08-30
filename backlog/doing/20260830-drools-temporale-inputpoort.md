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
