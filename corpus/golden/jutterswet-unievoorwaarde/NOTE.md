# jutterswet-unievoorwaarde — de unie-telling in een VOORWAARDE

De positiegetuige bij WF-110 (a), en zij bestaat omdat die clausule op twee
manieren te lezen was. "Niet als operand van een conditie" gaat over de KALE
unie; een TELLING over een unie is een gewoon geheel getal en staat waar elke
andere telling staat — in de waarde van een regel, in een `condition`, aan
weerszijden van een vergelijking. Die vorm is producentbereikbaar:

    Een Jutter is ruim voorzien indien het aantal aangemelde vondsten van de
    Jutter en geborgen vondsten van de Jutter groter is dan 2.

Zonder deze golden pint niets dat de vier uitvoerders haar dragen, en een
lezer die clausule (a) één streepje te ruim leest haalt een legale vorm
stilzwijgend van de wire. Nu kost die lezing een falende poort.

## De handberekening

| jutter | aangemeld | geborgen | unie | som der delen | > 2? |
|---|---|---|---|---|---|
| `J1` | {V1, V2, V3} | {V3, V4} | **4** | 5 | ja → ruim voorzien |
| `J2` | {V5, V6} | {V5, V6} | **2** | 4 | **nee** |
| `J3` | {} | {V7} | **1** | 1 | nee |
| `J4` | {} | {} | **0** | 0 | nee |

`J2` is de reden dat dit méér is dan een positiegetuige: de unie is 2 en haalt
de drempel NIET, terwijl de som der delen 4 is en hem wél zou halen. Een
uitvoerder die de deeltellingen optelt kent `J2` het kenmerk toe. De
deduplicatie beslist hier dus over een KENMERK en niet over een getal, wat de
enige plek in het corpus is waar zij dat doet.

`J1` pint de andere kant (de gedeelde `V3` verlaagt 5 naar 4 en de drempel
wordt nog steeds gehaald, dus de regel is geen toevalstreffer van het
drempelgetal), `J3` dat één element te weinig is en `J4` dat de lege unie 0 is
en nooit leeg (ch. 1.2e).

## Gemeten

Verifier PASS; `ref` en clingo beide 4 verwachtingen exact; beide
Drools-takken PLAYED. `source.rgs` re-emitteert byte-identiek (poort 10).
