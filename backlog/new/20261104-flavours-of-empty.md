# flavours-of-empty: welke soorten afwezigheid de IR onderscheidt

*Opgesteld door de hoofdsessie op 2026-11-04 op een vraag van de eigenaar, na het besluit dat een lege waarde niets beslist
(kaart `empty-write-decides-nothing`). Voor later; eerst onderzoek.*

## Aanleiding

uchar kent één leeg: de waarde die er niet is. Sinds 2026-11-04 beslist een schrijver die met leeg vuurt niets, op een tijdlijndoel en op
een scalair doel: de invoer blijft staan. Een regel kan een attribuut dus niet meer leegmaken. RegelSpraak heeft daar ook geen vorm voor:
`leeg` staat alleen in een predicaat ("is leeg") en in een verwachting van een toetsdocument, niet als waarde van een gelijkstelling.

De eigenaar vraagt hoe je dan "known unknown" schrijft: een waarde waarvan vaststaat dat ze onbekend is, tegenover een waarde die nog niet
bekend is. Misschien zijn er meer soorten (niet aangeleverd, niet van toepassing, bewust gewist, onbepaald door een fout).

## Goal

Een voorstel (`spec/proposals/`) dat de soorten afwezigheid opsomt die regelgeving en uitvoeringspraktijk onderscheiden, met per soort: of
RegelSpraak, ALEF, Catala, DMN/FEEL, OpenFisca en de werkbank-regelsets haar kennen en hoe; wat ze met de semantiek van uchar zou doen
(ch. 1 leeg, ch. 3.1 een gat, ch. 3.11 en 5.7 het schrijven, ch. 7 defaults, het §9.5-kanaal); en een aanbeveling: welke soorten de IR
moet dragen, en of een regel er een moet kunnen schrijven. Normvragen naar de eigenaar.

## Acceptance

- (a) Het voorstel met bronnen per soort (paragraafnummers, bestand en regel, geen tekst uit de RegelSpraak-specificatie).
- (b) Per aanbevolen soort een uitgewerkt voorbeeld met absolute waarden.
- (c) De normvragen apart, elk met een aanbeveling.

May touch: `spec/proposals/` (nieuw document), kaartverplaatsing.

## Omvang

Onderzoek, middel.

## Result
