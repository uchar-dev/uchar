# jutterswet-aantalfilter-gehele-jaar-creatieguard — hetzelfde filter in de voorwaarde van een objectcreatie

De telling van `jutterswet-aantalfilter-gehele-jaar`, "het aantal posten
van het Kustvak die gedurende het gehele jaar bemand zijn"
(`RUNTIME-SEMANTICS.md` ch. 3.9), staat hier in de voorwaarde van een
objectcreatie: een Kustvak met zo'n post krijgt een Wachtbrief met
briefbedrag 10, gekoppeld via `briefuitgifte`. Een tweede regel telt de
uitgegeven wachtbrieven van het Kustvak in het brieftal. Zo is de
creatie, die de case niet rechtstreeks kan noemen, waarneembaar in een
telling op een object met een label.

## De handberekening

Bemand per post (van, tot; tot is uitgesloten):

| post | perioden | telt mee |
|---|---|---|
| `P1` | 2026-01-01 – 2027-01-01 | ja: 2026 helemaal |
| `P2` | 2026-01-01 – 2026-07-01 | nee: 2026 alleen de eerste helft |
| `P3` | geen | ja: raakt geen jaar |
| `P4` | 2026-03-01 – 2027-01-01 | nee: januari en februari 2026 ontbreken |

| case | Kustvak: posten | telling | wachtbrief | brieftal |
|---|---|---|---|---|
| `case1-een-vaste-post` | K1: P1, P2; K2: P4 | K1 1, K2 0 | K1 ja, K2 nee | K1 **1**, K2 **0** |
| `case2-zonder-perioden` | K1: P3, P2 | 1 | ja | **1** |
| `case3-zonder-posten` | K1: geen | 0 | nee | **0** |

De creatie maakt per Kustvak hoogstens één Wachtbrief; het brieftal is
een telling en dus 0 waar geen brief bestaat, nooit leeg (ch. 1.2e). Elke
case pint ook dat het kanaal leeg is.

## Waarom er geen `bron.rgs` bij staat

Het document is in RegelSpraak geschreven:

```
Objecttype het Kustvak (mv: Kustvakken)
    het brieftal Numeriek (niet-negatief geheel getal);

Objecttype de Post (mv: Posten)
    is bemand kenmerk (bijvoeglijk) voor elke maand;

Objecttype de Wachtbrief (mv: Wachtbrieven)
    het briefbedrag Numeriek (geheel getal);

Feittype postindeling
    het kustvak Kustvak
    de post (mv: posten) Post
Eén kustvak omvat meerdere posten

Feittype briefuitgifte
    het uitgevende kustvak Kustvak
    de uitgegeven wachtbrief (mv: uitgegeven wachtbrieven) Wachtbrief
Eén uitgevende kustvak heeft één uitgegeven wachtbrief

Regel geef een wachtbrief uit
    geldig altijd
        Een Kustvak heeft een Wachtbrief met briefbedrag gelijk aan 10
        indien het aantal posten van het Kustvak die gedurende het gehele jaar bemand zijn groter is dan 0.

Regel tel de wachtbrieven
    geldig altijd
        Het brieftal van een Kustvak moet berekend worden als het aantal uitgegeven wachtbrieven van het Kustvak.
```

De frontend zet een die/dat-subselectie met een kwalificatie van de hele
periode op de operand van een aggregatie nog niet om: hij schrijft een
`Unresolved`-filter met een waarschuwing ("een 'die/dat'-subselectie op
een aggregatie-operand wordt nog niet omgezet"). In `ir.json` is dat
filter met de hand vervangen door de kenmerktoets `bemand` met de
kwalificatie van het jaar, en de waarschuwing is weggelaten. De bron
levert dus niet byte voor byte deze IR op, en gate 10 eist dat van elke
golden met een `bron.rgs`. De IR is daarom handgeschreven, zoals een
golden voor een vorm die geen frontend nog maakt (`corpus/golden/README.md`).

## Herkomst

Eigen werk (kaart `drl-teksttak-op-truth-maintenance`, stap 3a; de vorm
van kaart `drl-aantal-filter-zink`). Gemeten 2026-10-14 bij de toelating:
verifier PASS; ref en clingo spelen de drie cases; beide takken van de
Drools-backend PLAYED 3.
