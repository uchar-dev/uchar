# jutterswet-aantalfilter-gehele-jaar — een telling met een filter "gedurende het gehele jaar"

Het postental van een Kustvak telt de posten van het Kustvak die
gedurende het gehele jaar bemand zijn, en het waaktarief is 10 als er zo'n
post is. Het filter van de telling is een kenmerktoets met de kwalificatie
van het gehele jaar (`RUNTIME-SEMANTICS.md` ch. 3.9): een post telt als
elk jaar dat zijn bemande perioden raken, op elke dag bemand is. Een post
zonder enige periode raakt geen jaar en telt dus mee (ch. 3.9, "passes
vacuously"). De kwalificatie hoort bij het filter van de telling, in een
rekenregel en in de voorwaarde van een gelijkstelling.

## De handberekening

Bemand per post (van, tot; tot is uitgesloten):

| post | perioden | telt mee |
|---|---|---|
| `P1` | 2026-01-01 – 2027-01-01 | ja: 2026 helemaal |
| `P2` | 2026-01-01 – 2026-07-01 | nee: 2026 alleen de eerste helft |
| `P3` | geen | ja: raakt geen jaar |
| `P4` | 2026-03-01 – 2027-01-01 | nee: januari en februari 2026 ontbreken |
| `P5` | 2025-01-01 – 2027-01-01 | ja: 2025 en 2026 helemaal |
| `P6` | 2025-06-01 – 2027-01-01 | nee: 2025 begint pas in juni |
| `P7` | 2026-01-01 – 2026-07-01, 2026-07-01 – 2027-01-01 | ja: twee aansluitende perioden dekken 2026 |

| case | Kustvak: posten | postental | waaktarief |
|---|---|---|---|
| `case1-een-vaste-post` | K1: P1, P2; K2: P4 | K1 **1**, K2 **0** | K1 **10**, K2 **leeg** |
| `case2-geen-vaste-post` | K1: P2, P4 | **0** | **leeg** |
| `case3-zonder-perioden-en-twee-jaren` | K1: P3, P5, P6, P7 | **3** | **10** |
| `case4-zonder-posten` | K1: geen | **0** | **leeg** |

Een telling over een lege verzameling is 0, nooit leeg (ch. 1.2e). Het
waaktarief blijft leeg waar de voorwaarde onwaar is: de regel schrijft
dan niets. Elke case pint ook dat het kanaal leeg is.

## Waarom er geen `source.rgs` bij staat

Het document is in RegelSpraak geschreven:

```
Objecttype het Kustvak (mv: Kustvakken)
    het postental Numeriek (niet-negatief geheel getal);
    het waaktarief Numeriek (geheel getal);

Objecttype de Post (mv: Posten)
    is bemand kenmerk (bijvoeglijk) voor elke maand;

Feittype postindeling
    het kustvak Kustvak
    de post (mv: posten) Post
Eén kustvak omvat meerdere posten

Regel postental van een kustvak
    geldig altijd
        Het postental van een Kustvak moet berekend worden als het aantal posten van het Kustvak die gedurende het gehele jaar bemand zijn.

Regel waaktarief van een kustvak
    geldig altijd
        Het waaktarief van een Kustvak moet gesteld worden op 10
        indien het aantal posten van het Kustvak die gedurende het gehele jaar bemand zijn groter is dan 0.
```

De frontend zet een die/dat-subselectie met een kwalificatie van de hele
periode op de operand van een aggregatie nog niet om: hij schrijft een
`Unresolved`-filter met een waarschuwing ("een 'die/dat'-subselectie op
een aggregatie-operand wordt nog niet omgezet"). In `ir.json` is dat
filter met de hand vervangen door de kenmerktoets `bemand` met de
kwalificatie van het jaar, en de waarschuwing is weggelaten. De bron
levert dus niet byte voor byte deze IR op, en gate 10 eist dat van elke
golden met een `source.rgs`. De IR is daarom handgeschreven, zoals een
golden voor een vorm die geen frontend nog maakt (`corpus/golden/README.md`).

## Herkomst

Eigen werk (kaart `drl-teksttak-op-truth-maintenance`, stap 3a; de vorm
van kaart `drl-aantal-filter-zink`). Gemeten 2026-10-14 bij de toelating:
verifier PASS; ref en clingo spelen de vier cases; beide takken van de
Drools-backend PLAYED 4.
