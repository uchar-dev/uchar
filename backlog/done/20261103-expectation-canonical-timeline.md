# expectation-canonical-timeline: een verwachte tijdlijn vergelijkt over read, niet over segmentlijsten

*Opgesteld door de hoofdsessie op 2026-11-03 uit de architectuurreview van Fable (`werk/review-fable-2026-11-03/advies.md`, §1 punt 1,
advies 5).*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.1 (rond r. 1048–1058): een gat en een expliciet leeg segment zijn onder `read` gelijk, en geen waardeproducerende
bewerking mag ze onderscheiden. Maar een verwachting in een case is segment-exact, en de producenten segmenteren elk op hun eigen manier:
rekenen per periode voegt gelijke buren samen (VB-A2), de dagentelling houdt getelde emmers apart en voegt alleen nulreeksen samen
(VB-C11, "omdat de goldens dat pinnen"), de pro rata voegt niets samen (ch. 3.7, "do not coalesce"). Drie conventies voor dezelfde waarde,
door gates 5, 6 en 12 letterlijk vergeleken; elke uitvoerder moet ze alle drie nabouwen (Drools `PeriodArithmetic.dayCount` tegen
`coalesced`) zonder dat de spec er één normeert.

## Goal

De vergelijking van een verwachte tijdlijn (case-lezer van ref en clingo, `CaseRunner` van Drools) gebeurt in één canonieke vorm: maximale
reeksen van gelijke waarden, gaten weg, open grenzen zoals ze zijn. De spec zegt dat in ch. 3.1 en in het case-formaat; de drie
segmenteerconventies zijn dan geen pin meer, en een normzin als "do not coalesce" in ch. 3.7 kan weg als hij alleen de pin droeg.

## Acceptance

- (a) Rood eerst: een case met een verwachting in een andere maar onder `read` gelijke segmentering faalt op main in ref, clingo en Drools
  (letterlijk), en speelt daarna.
- (b) Elke bestaande golden blijft groen zonder dat een verwachting verandert; meet of een golden alleen door de segmentering slaagde en
  noem het.
- (c) RS ch. 3.1 en het case-schema (beschrijving; voorstel) zeggen de vergelijking; gate 13 en 17 onveranderd.
- (d) `make gates` groen.

May touch: `ref/lib/case.ml` en de vergelijking, `backends/clingo/` (case-lezer en vergelijking), `backends/drools/**` (`CaseRunner`),
`spec/RUNTIME-SEMANTICS.md` (voorstel), `spec/schema/uchar-case-v0.schema.json` (alleen de beschrijving; voorstel), kaartverplaatsing.

## Omvang

Klein tot middel.

## Result

*Bouwer, 2026-11-04. Branch `card/expectation-canonical-timeline`, gerebased op main `62c922a54`. Niet gemerged, niet
gepusht; de kaart blijft in `doing/`. Beschermde paden (`spec/RUNTIME-SEMANTICS.md`, `spec/schema/`) zijn een voorstel.*

**Commits.** `81c5787c0` kaart naar doing; `fd6b36e33` ref; `42121d100` clingo; `2e500d2fc` Drools; `b975667cb` spec en
case-schema (voorstel); `4534acbca` Drools, de vorm hangt niet af van de volgorde; `4cb02d13f` ref, een rationale reeks.

**De canonieke vorm** (RS ch. 3.1, voorstel). De maximale reeksen van gelijke waarden met de gaten weg: elk leeg segment
en elk segment met `tot` ≤ `van` valt weg, daarna gaan twee aangrenzende segmenten `[a, b)` en `[b, c)` met gelijke
waarde samen tot `[a, c)`, tot er geen twee meer samengaan; een open grens blijft open. Gelijk is de gelijkheid van de
vergelijking (numeriek: 12 en 12,00 zijn één reeks, 10/3 en 20/6 ook). Twee welgevormde tijdlijnen lezen elke dag gelijk
precies als hun canonieke vormen gelijk zijn. Een verwachte tijdlijn vergelijkt zo: beide kanten canoniek, dan segment
voor segment, de grenzen exact, de waarden met die gelijkheid. Een gat verbindt zijn buren niet, en een overlap blijft
zichtbaar (alleen precies aangrenzende segmenten gaan samen), dus een overlappende uitkomst is nooit gelijk aan een
welgevormde verwachting.

**Per uitvoerder** (elk een eigen normalisatie; gate 1 groen):
- ref: `Timeline.canonical` (`ref/lib/timeline.ml`): leeg eruit, dan de bestaande `coalesce`, die sorteert, ontaarde
  segmenten weglaat en op `Arith.equal` samenvoegt. `Case.check` legt de verwachting en de uitkomst erin; de melding
  toont de canonieke vormen.
- clingo: `Model.canonical_timeline` (`backends/clingo/lib/model.ml`) over de geschaalde segmenten (van, tot, waarde),
  voor `because` en voor de tl/5-atomen. De case-lezer blijft die van ref en normaliseert niets; de ASP-poot draagt haar
  vergelijking zelf. tl/5 kent geen leeg segment.
- Drools: `CaseRunner.canonical` over een record `TimelineSegment(from, to, value)`; `timeline()` vergelijkt beide
  kanten canoniek, voor beide branches (de vergelijking is gedeeld). De javadoc van `DroolsBackend.compare` zegt het.

**Rood op `457debf65`** (de fixtures in `ref/test/fixtures/canonical-timeline`, op de IR van drie goldens; log
`werk/expectation-canonical-timeline/logs/03-red-on-457debf65.log`). Alle drie de positieve cases faalden in ref, clingo
en beide Drools-branches, letterlijk, bijvoorbeeld de pro rata:
- ref: `MISMATCH: J1.borgdeel = [2024-01-01,2024-02-01)=12 [2024-02-01,2024-03-01)=12 … [2025-02-01,2025-03-01)=4.5,
  expected [2024-01-01,2025-01-01)=12 [2025-01-01,2025-02-01)=18 [2025-02-01,2025-03-01)=4.5`;
- clingo: `MISMATCH: J1.borgdeel timeline = [19723,19754)=1200 [19754,19783)=1200 … [20120,20148)=450, expected
  [19723,20089)=1200 [20089,20120)=1800 [20120,20148)=450`;
- Drools exec-model en DRL-tekst: `FAIL pro-rata.json: attribuut borgdeel: verwacht [2024-01-01,2025-01-01)=12
  [2025-01-01,2025-02-01)=18 [2025-02-01,2025-03-01)=4.5, werkelijk [2024-01-01,2024-02-01)=12 …`.
De dagentelling (twee getelde emmers als één, een open nulreeks als twee) en het rekenen per periode (een segment en
beide open reeksen als twee) idem. Na de wijziging spelen ze in de drie suites, op beide branches. De twee negatieve
cases (`gap-not-bridged`: 40 in januari en maart, een reeks over februari verwacht; `value-differs`: één waarde van drie
anders) waren rood en blijven rood met dezelfde mismatch.

**Mutaties** (niet gecommit, in een losse worktree op `2fcd37859`, de code inhoudelijk gelijk aan die na de rebase); elke
bewaking heeft een rode test:

| | mutatie | rood |
|---|---|---|
| R1 | ref: verwachting niet canoniek | `WV-3.1-canonical-expectation-day-count`, `-period-arithmetic`, `WV-3.10-voorwaarde-per-periode`, `WV-3.10-kenmerk-venster` |
| R2 | ref: uitkomst niet canoniek | `WV-3.1-canonical-expectation-pro-rata`, `-day-count`, de twee WV-3.10 |
| R3 | ref: leeg segment blijft | `WV-3.1-canonical`, `WV-3.1-canonical-only-leeg` |
| R4 | ref: reeks over een gat (`coalesce`, `<=`) | `WV-3.1-canonical`, `WV-3.1-canonical-mismatch-gap-not-bridged` |
| R5 | ref: ongelijke buren samen (`coalesce`) | 26 voorbeelden, o.a. `WV-3.1-canonical-unequal-neighbours`, `WV-3.4-a` |
| C1 | clingo: verwachting niet canoniek | fixtures day-count, period-arithmetic, afdracht, kenmerk-venster |
| C2 | clingo: model niet canoniek (wel gesorteerd) | fixtures pro-rata, day-count, afdracht, kenmerk-venster |
| C3 | clingo: reeks over een gat | twee eenheidschecks, `gap-not-bridged stays a mismatch` |
| C4 | clingo: ongelijke buren samen | twee eenheidschecks, `value-differs stays a mismatch` |
| C5 | clingo: ontaard segment blijft | `a degenerate segment contributes nothing` |
| C6 | clingo: niet sorteren | 11 checks (de atomen komen ongeordend uit het model) |
| D1 | Drools: verwachting niet canoniek | `the_read_back_compares_in_the_canonical_form_on_both_branches` |
| D2 | Drools: uitkomst niet canoniek | idem |
| D3 | Drools: leeg segment blijft | `equal_neighbours_merge_a_leeg_segment_is_a_gap_…` (NullPointerException) |
| D4 | Drools: reeks over een gat | drie tests, waaronder de fixtures op beide branches |
| D5 | Drools: ongelijke buren samen | drie tests, idem |
| D6 | Drools: ontaard segment blijft | `a_degenerate_segment_contributes_nothing` |
| D7 | Drools: niet sorteren | `the_form_does_not_depend_on_the_order_of_the_segments` |

**Meting over het corpus** (na de rebase). 159 `attributeTimeline`-verwachtingen in 17 goldenmappen, 18 daarvan leeg.
15 verwachtingen in 10 casebestanden van 6 goldens zijn niet canoniek: zij pinden de segmentering per emmer van hun
producent. Pro rata 5: `anchor/jutterswet-borgdeel` case1 (twaalf maanden 12, 11 samenvoegingen) en case6,
`synth2-clip-over-prorata` case1, `jutterswet-pachtvenster` `maandpacht` in case1 en case3. Dagentelling 10:
`anchor/jutterswet-dagentelling` case2 (`borgdagen`, `jaardagen`), `jutterswet-dagentelling-rgs` case2 (idem),
`jutterswet-pachtvenster` `zomerdagen` in case1 en case3, `jutterswet-vast-startpunt` `maandwachtdagen` in case2, 3, 5
en 6. Rekenen per periode: geen; die producent voegt al samen, dus zijn verwachtingen zijn canoniek
(`jutterswet-maandloon` case5-gelijke-buren pint de samengevoegde vorm). In de suitefixtures van ref nog twee
(`conditions-per-period/afdracht` `minimumdagen`, `kenmerk-venster` `rentedagen`, beide een dagentelling). Geen golden
slaagde of faalde alleen door de segmentering in de zin dat een oordeel omslaat: segment-exacte gelijkheid impliceert
canonieke gelijkheid, gates 5, 6 en 12 waren groen op main en zijn groen na de wijziging, geen verwachte waarde
veranderde. De 15 verwachtingen slaagden wel alleen omdat hun producent precies zo segmenteert: een producent die de
emmers samenvoegt, was er op main mee rood geworden, zoals de fixtures laten zien; nu niet meer. Gate 7: op de ruwe
invoer verandert het oordeel van geen enkele van de 159 tijdlijnverwachtingen (in Python gemeten), en `check-noop` geeft
PASS met dezelfde 41 overlevers.

**Gate 13 en 17, onveranderd.** Gate 13 vergelijkt de statusregel van een getwiste run met die van de onaangeroerde;
beide lopen door dezelfde, nu canonieke, case-vergelijking. Gate 17 vergelijkt de trace van ref letterlijk met de
readings van Drools, en daarin staat een tijdlijn als `[[<from> <to> <value>], …]` in de segmentering van de producent
(ch. 5.12). Daar houden ref en Drools elkaar nog aan één segmentering. Dat is geen verwachting en valt buiten
acceptatie (c); zie open punten.

**Voorgestelde tekst (huidige stand)**, aangebracht in `b975667cb`:
- RS ch. 3.1, nieuw punt na "Two representations": "The **canonical form** of a timeline is its maximal runs of equal
  values with the gaps omitted: every leeg segment and every zero-length or reversed one is dropped, then every two
  adjacent segments `[a, b)` and `[b, c)` with equal values merge into `[a, c)` until no two do; an open bound stays
  open. Values are equal as in the change-detection rule below (numeric equality for a number, so 12 and 12,00 are one
  run). Two well-formed timelines read alike on every day exactly when their canonical forms are equal. How a producer
  divides a run of equal values into segments is therefore representation, as the choice between a gap and an explicit
  leeg segment is. An expected timeline of a case (`uchar-case-v0`, `attributeTimeline`) is compared with the timeline
  the evaluation gives in this form: both sides canonical, then segment for segment, the bounds exactly and the values
  by that equality." Met het uitgewerkte voorbeeld `WV-3.1-canonical` en de verwijzing naar de fixtures.
- RS ch. 3.1 "Two representations": de zin over wat een regel schrijft ("holds its filled segments only — a leeg period
  is written as a gap", VB-A2) wordt "The case format (`uchar-case-v0`) writes filled segments only"; "(only the
  contributor-skipping rule of ch. 3.5 emits the latter)" vervalt.
- RS ch. 3.4: "Adjacent result segments with equal values are coalesced into one." wordt "How the result divides a run
  of equal values into segments is representation (ch. 3.1, the canonical form)."; "After equal neighbours coalesce"
  wordt "In its canonical form (ch. 3.1)".
- RS ch. 3.5: de zin over expliciete lege segmenten, samenvoegen en "writes those periods as gaps" wordt "An all-leeg
  period that the fallback does not fill reads leeg. Whether it is a gap or an explicit leeg segment, and how the sum
  divides a run of equal values into segments, is representation (ch. 3.1, the canonical form)."
- RS ch. 3.7: "the twelve 2024 buckets stay twelve separate segments: bucketing producers do **not** coalesce adjacent
  equal buckets" vervalt; ervoor in de plaats: "in the canonical form of ch. 3.1 the twelve months of 2024 are the one
  segment [2024-01-01, 2025-01-01) = 12, since whether the part writes them as one segment or as twelve is
  representation."
- RS ch. 3.8 (VB-C11): de telling geeft "every day of a bucket `B` with a true day the true-day count in `B` … and
  **0 to every day of a bucket without a true day, over all time** …: the 0 reaches before the first bucket with a
  true day and past the last, and where no bucket has one the count is 0 on (−∞, +∞). How the count divides a run of
  equal values into segments is representation (ch. 3.1, the canonical form)."
- RS ch. 3.11: ", with its leeg periods as gaps" vervalt.
- `uchar-case-v0`, beschrijving van `attributeTimeline.timeline`: "segment-exact" vervalt; erbij: "It pins what the
  target reads on each day, not how a run of equal values is divided into segments: an executor compares it with the
  timeline the evaluation gives in the canonical form of RUNTIME-SEMANTICS ch. 3.1 (the maximal runs of equal values,
  gaps omitted, open bounds as they stand), so two adjacent segments with equal values and the one segment over both are
  the same expectation."
- CONSTRUCTS en PLAN: geen rij nodig; de vergelijking hoort bij het case-formaat van uchar, niet bij een v0-delta.

**Voorlopige besluiten.**
- VB-1 — De getuige staat niet in `corpus/golden` maar als suitefixtures in `ref/test/fixtures/canonical-timeline`, op
  de `ir.json` van `anchor/jutterswet-borgdeel`, `anchor/jutterswet-dagentelling` en `jutterswet-maandloon`, gespeeld
  door de suites van ref, clingo en Drools (beide branches). Reden: `corpus/` staat niet in May touch; het precedent is
  de gedeelde fixtures van `conditions-per-period` en `bucket-scalar`.
- VB-2 — Een mismatchmelding toont in alle drie de canonieke vormen (wat vergeleken wordt). Het scenariorapport van
  Drools (`ExpectationResult`, de velden expected en actual) toont beide kanten ongewijzigd zoals de case en de bean ze
  hebben; een geslaagde rij kan dus twee segmenteringen tonen.
- VB-3 — De producenten blijven segmenteren zoals ze deden (ref, clingo, Drools `PeriodArithmetic`); hun conventies
  zijn geen pin meer, maar zijn niet gelijkgetrokken. Niets vraagt dat nu.
- VB-4 — De verwachting hoeft niet canoniek te zijn; de lezer houdt volgorde, geen overlap en het raster. De 15
  niet-canonieke verwachtingen blijven staan.
- VB-5 — De canonieke vorm sorteert; in Drools getuigt een ongeordende lijst daarvan, omdat de lijsten van de read-back
  geordend aankomen.

**Tests.** Geen bestaande test omgedraaid. `TemporalInputWitnessTest.comparator_sees_a_length_difference` blijft geldig:
het tweede jaar daar is een verschil onder `read`, geen segmentering. Nieuw: ref `WV-3.1-canonical*` (13 checks), clingo
9 checks, Drools `CanonicalTimelineWitnessTest` (5 tests).

**Suites en gates.** Ref-suite 2340 voorbeelden, clingo-suite 2471 checks, Drools-suite 1838 tests zonder skips, alle
groen. `make gates` in de worktree op `4cb02d13f`: alle zeventien gates en `after-run` groen, red=0, 344 s
(`out/gates/20261104-083836/`).

**Open punten.**
- Gate 17 / ch. 5.12: de lezing schrijft een tijdlijn in de segmentering van de producent, en de gate houdt ref en
  Drools daaraan. Als ch. 5.12 de tijdlijn in een lezing in de canonieke vorm schrijft, hoeft gate 17 niet te
  veranderen; dat raakt `ref/lib/reading.ml` en `SessionDump` en is een eigen kaart.
- Ch. 3.2 noemt bucketing "producers that emit one segment per calendar period"; dat is beschrijvend en bleef staan.
- Review §2: ch. 3.9 "Only the Drools backend writes such a segment" is verouderd (advies 1), niet hier aangeraakt.
