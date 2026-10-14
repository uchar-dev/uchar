# jutterswet-uitsluiting-gedateerd — een stapel van defaults met een geldigheid

Het document is `anchor/jutterswet-uitsluiting` met één verschil: de groep
van defaults "uitsluiting van het penningrecht" is geldig vanaf
2026-01-01. De groep beslist het kenmerk `uitgesloten` van een Jutter in
de drie lagen van art. 4, van hoog naar laag; de hoogste default die
beslist, geeft de beslissing, en op een kenmerk beslist een default zodra
zijn rechtvaardiging geldt (`RUNTIME-SEMANTICS.md` ch. 7.1):

- derde laag: geweigerd, herroepen en de herroeping vervallen:
  **uitgesloten**;
- tweede laag: geweigerd en herroepen: **niet uitgesloten**;
- eerste laag: geweigerd: **uitgesloten**.

Beslist geen laag, dan staat `uitgesloten` niet. De regel
"penninggerechtigd tenzij uitgesloten" heeft geen geldigheid en geldt
altijd. Een gedateerde vorm doet alleen mee als de rekendatum in zijn
geldigheid valt (ch. 8.2, "It gates"); daarom draagt elke case een
rekendatum.

## De handberekening

| case | rekendatum | kenmerken van M | laag | uitgesloten | penninggerechtigd |
|---|---|---|---|---|---|
| `case1-geweigerd-2026` | 2026-06-01 | geweigerd | eerste | **ja** | **nee** |
| `case2-herroepen-2026` | 2026-06-01 | geweigerd, herroepen | tweede | **nee** | **ja** |
| `case3-vervallen-2026` | 2026-06-01 | geweigerd, herroepen, vervallen | derde | **ja** | **nee** |
| `case4-kaal-2026` | 2026-06-01 | geen | geen | **nee** | **ja** |
| `case5-vervallen-2025` | 2025-06-01 | geweigerd, herroepen, vervallen | groep niet geldig | **nee** | **ja** |

Case 1 tot en met 4 zijn de cases van de anchor met een rekendatum in de
geldigheid, en geven dezelfde uitkomst. Case 5 heeft de kenmerken van
case 3 maar een rekendatum vóór de geldigheid: de groep doet niet mee,
dus geen laag beslist en M is penninggerechtigd. Elke case pint ook dat
het kanaal leeg is.

Zonder rekendatum kan geen uitvoerder de geldigheid toetsen; die case
staat als rode case in `corpus/rood/jutterswet-uitsluiting-zonder-rekendatum`.

## Herkomst

Eigen werk (kaart `drl-teksttak-op-truth-maintenance`, stap 4): de IR van
de anchor met `validity.from` gezet op 2026-01-01, met de hand; er is geen
bron in RegelSpraak. Gemeten 2026-10-14 bij de toelating: verifier PASS;
ref en clingo spelen de vijf cases; beide takken van de Drools-backend
PLAYED 5.
