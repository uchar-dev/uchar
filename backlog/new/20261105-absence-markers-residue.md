# absence-markers-residue: de zijtabel van de frontend en de kopie van een tijdlijn met markers

*Opgesteld door de hoofdsessie op 2026-11-05, uit de vragen van stap B van `absence-markers`. Starten op sein van de eigenaar.*

## Aanleiding

1. De RegelSpraak-frontend typeert een attribuut met markers als zijn payloaddatatype en houdt de markers in een zijtabel
   (`absenceMarkerSites`); de emitter schrijft er de vorm met een sum type uit (`SumType`, `Construct`, `PayloadAccess`), en RGS-376
   toetst de lezing tegen die tabel. Het gedeelde model van frontend en Drools-backend kent de drie knopen sinds stap B, maar de
   resolver bouwt ze niet. De IR is goed (gate 10 houdt de golden `jutterswet-afwezigheidsmarkers` byte-gelijk, de verifier houdt
   WF-127); het gaat om de vorm binnen de frontend: een tweede bron voor wat de typering zou moeten dragen.
2. Een kopie van een tijdlijn met markers (ref's fixture `copy-of-a-timeline`) speelt in ref; clingo en de Drools-backend weigeren haar
   bij naam (Drools: DROOLS-303). Geen golden vraagt er nu om.

Besloten zonder kaart (hoofdsessie, technisch): een `Match` blijft in de Drools-backend geweigerd (DROOLS-021) tot een golden die het
sum type zonder markers draagt erom vraagt; hoe de toolchain een constructorwaarde in de uitleg weergeeft, hoort bij haar pin.

## Goal

- (1) De resolver bouwt en typeert elke lezing van een attribuut met markers als `PayloadAccess`, een geschreven of getoetste marker als
  `Construct`, en zet RGS-376 op die knopen; de zijtabel `absenceMarkerSites` verdwijnt. De emissie blijft byte-gelijk.
- (2) clingo en Drools dragen de kopie van een tijdlijn met markers, met een golden die ref, clingo en beide Drools-branches speelt, of
  het blijft een weigering bij naam als het dragen buiten verhouding is (met de gemeten reden in het Result).

## Acceptance

- (a) Rood eerst per punt; per nieuwe bewaking een mutatie die een test rood maakt.
- (b) Bij (1): gate 10 en de frontend-suite groen zonder de zijtabel; geen golden verandert.
- (c) `make gates` groen.

May touch: `frontends/regelspraak/`, `backends/clingo/`, `backends/drools/**`, `corpus/`, kaartverplaatsing.

## Omvang

(1) middel (resolver en typering); (2) klein tot middel.
