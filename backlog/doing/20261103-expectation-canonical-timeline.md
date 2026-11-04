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
