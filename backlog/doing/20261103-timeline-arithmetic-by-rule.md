# timeline-arithmetic-by-rule: ch. 3.4 en 3.5 via een regel, over alle tijd (impliciete tijd, fase 1a en 2a)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` (fase 0) en het
voorstel `spec/proposals/IMPLICIT-TIME.md`, punt 2 en §5. Besluit van de eigenaar 2026-11-03 (normvraag 2): **alle tijd**. Een
tijdlijn deelt de hele tijd in, leeg buiten haar perioden; een scalair geldt altijd (RegelSpraak §5.1.1, §5.1.4.1; ALEF
`ITimed`, `Always`).*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.4 en 3.5 specificeren plus, min en de som per periode over tijdlijnen, maar geen uitvoerder schrijft
ze via een regel: ref weigert een tijdsafhankelijk attribuutdoel buiten pro rata en dagentelling, clingo met CLINGO-024, Drools
met DROOLS-105 (IMPLICIT-TIME §5). Het domein van ch. 3.4 is nu de span van de operanden; de eigenaar koos alle tijd, en dat
vraagt open segmenten.

## Stap A — spec, ref, clingo, frontend

Goal: een regel met een tijdsafhankelijk attribuutdoel waarvan de waarde plus, min of de som per periode van ch. 3.4 en 3.5 is,
draait in ref en clingo over alle tijd, gepind door een golden met absolute verwachtingen.

Acceptance:
- (a) Rood eerst: golden `jutterswet-<naam>` (eigen casus, met `source.rgs`) met de uitgewerkte voorbeelden van ch. 3.4 en
  3.5 via een regel, en een scalair naast een tijdlijn (`WV-3.4-domein`: `5 plus a` geeft 5 vóór, 105 binnen, 5 na); op main
  weigeren ref en clingo met de zinnen van IMPLICIT-TIME §5.
- (b) RS ch. 3.1 (open segmenten: een segment mag een open begin of einde hebben; een gat is een leeg segment), 3.4, 3.5 en
  3.12 (voorstel) zeggen alle tijd, met de voorbeelden van ch. 3.4 buiten de span herrekend. VB 2.1 en 2.2 van het voorstel:
  het case-schema `uchar-case-v0` laat `from` of `to` weg voor een open grens (beschermd: voorstel), de laders van ref en
  clingo lezen dat, een verwachting vergelijkt de hele opdeling; de ASP-codering van een open grens (`spec/ASP-TRANSLATION.md`,
  voorstel). VB 2.4: een totaal (ch. 3.6) over een open segment met waarde 0 telt 0, met een andere waarde is het een fout met
  een nieuwe EF-code (ch. 5.7b, rood in beide uitvoerders).
- (c) ref en clingo spelen de golden; gate 10 emitteert haar byte-gelijk; bestaande goldens en hun verwachtingen veranderen niet
  (meet het en noem elk verschil).
- (d) De golden staat met naam in de kanaallijst van `scripts/check-drools.sh` (voorstel) tot stap B.
- (e) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `corpus/golden/<nieuw>/`, `corpus/red/<nieuw>/`, `spec/RUNTIME-SEMANTICS.md`
(voorstel), `spec/schema/uchar-case-v0.schema.json` (voorstel), `spec/ASP-TRANSLATION.md`, `scripts/check-drools.sh` en
`scripts/check-red.sh` (voorstel), `frontends/regelspraak/**` (alleen een gebrek dat de golden blootlegt), kaartverplaatsing.

Out of scope: andere operatoren (`20261103-timeline-arithmetic-per-period`), voorwaarden per periode
(`20261103-conditions-per-period`).

## Stap B — Drools (direct na de landing van stap A)

Goal: de lowering met truth maintenance (`transpiler/tms`, één analyse in exec-model en DRL-tekst) draagt de vormen van stap A
als zuivere functies over segmentlijsten in de runtime (precedent `Allocation.compute`), open segmenten inbegrepen.

Acceptance: (a) rood eerst: de golden van stap A geeft op main DROOLS-105; (b) `PLAYED` op beide branches, `EXPECTED_PLAYED`
omhoog en de kanaalpin weg (voorstel), elke code die geen plek meer noemt `RETIRED`; (c) de lezingen gelijk aan de trace van
ref (gate 17), een getuige per nieuwe vorm in elke regelvolgorde; (d) de case-lezer van Drools leest een open grens; (e)
`make gates` groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (voorstel), kaartverplaatsing.

## Omvang

Middel (stap A), klein tot middel (stap B).
