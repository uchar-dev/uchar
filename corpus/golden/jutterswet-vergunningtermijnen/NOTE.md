# jutterswet-vergunningtermijnen — lezingen binnen het huishouden van een vergunning

Een Vergunning kan een vorige vergunning hebben en heeft nul of meer
termijnen. De verlengde einddag van een Vergunning telt de overschotten van
de termijnen van de vorige vergunning bij haar basiseinddag op; het overschot
van een termijn hangt af van de verlengde einddag van zijn houder. Per type
is dat een kring, per object een keten die eindigt bij een vergunning zonder
voorganger. Het document is de getuige van de verruiming van de
instantiegeordende kring tot het *huishouden* van een object
(`spec/IR-SPEC.md` ch. 5.3, voorlopig besluit B2.1 van de kaart
`instance-cycle-household-reads`): een Termijn is een satelliet van een
Vergunning (de rol `houder` is to-one, geen vorm schrijft
`vergunningstermijn`), en alle termijnen van een vergunning staan op haar
diepte, hoeveel het er ook zijn.

De vijf regels en hun lezingen achter een rand binnen de kring:

- **overschot** (Termijn): de dagen van de verlengde einddag van zijn houder
  tot zijn einddag, indien hij vóór die verlengde einddag begon en daarna
  eindigt. De lezing gaat één stap terug naar de eigenaar: lokaal.
- **restant** (Vergunning): de som van de overschotten van de termijnen die
  vóór haar verlengde einddag begonnen. Een vouw over de eigen to-many
  satellieten (lokaal), met in het filter de verlengde einddag van de houder
  van de termijn: de stap naar de satelliet en terug (lokaal).
- **afgewikkeld** (kenmerk van de Vergunning): de einddag van alle termijnen
  ligt vóór haar verlengde einddag. Een kwantor `alle` over de eigen
  termijnen (het bereik negatief, ch. 5.2), met in de toets dezelfde weg
  heen en terug (lokaal). Zonder termijnen is een vergunning afgewikkeld.
- **verlengde einddag**: de basiseinddag plus de som van de overschotten van
  alle termijnen van de vorige vergunning, indien het restant van de vorige
  vergunning gevuld is en de vorige vergunning niet afgewikkeld is. Een stap
  naar de voorganger en een vouw over diens to-many satellieten: verschoven
  (tot deze kaart het negatief `wf045-instance-cycle-to-many-satellite-step`).
  De gevuld-toets en het ontkende kenmerk op de voorganger zijn negatieve
  verschoven randen. De tweede voorwaarde volgt uit de eerste (een gevuld
  restant heeft een termijn die na de verlengde einddag eindigt); zij brengt
  de kwantor in de kring.
- **verlengde einddag zonder restant**: de basiseinddag, indien het restant
  van de vorige vergunning leeg is (ook zonder vorige vergunning, ch. 5.8).

De niveaus binnen een diepte (RUNTIME-SEMANTICS ch. 6.2): de twee regels van
de verlengde einddag op 0 (zij lezen alleen de voorganger), het overschot en
het restant op 0, het kenmerk op 1 (een kenmerktoekenning leest een waarde
strikt).

## De handberekening

Een tijdsduur in hele dagen is het verschil in kalenderdagen; een datum plus
een aantal dagen is een kalenderstap (ch. 3.8). Een som slaat lege bijdragen
over en is leeg zonder gevulde bijdrage.

**case1-een-vergunning** (keten van één, één termijn). V1: basiseinddag
2026-06-01, geen vorige: verlengde einddag **2026-06-01**. T1 (2026-05-01 tot
2026-06-11) begon ervoor en eindigt erna: overschot **10**. Niet
afgewikkeld (T1 eindigt na 2026-06-01). Restant: T1 begon ervoor, **10**.

**case2-keten-van-twee** (twee termijnen en één). V1: basiseinddag 2026-06-01,
geen vorige: **2026-06-01**. T1 (2026-04-01 tot 2026-07-15): juni 30 plus 14,
**44**. T2 (2026-05-15 tot 2026-06-21): **20**. V1 niet afgewikkeld, restant
**64**. V2: basiseinddag 2026-12-01, vorige V1 met gevuld restant en niet
afgewikkeld: 2026-12-01 plus 44 + 20 = 64 dagen (december 30 tot 31-12,
januari 31, dan 3) is **2027-02-03**. T3 (2026-11-01 tot 2027-01-10) eindigt
vóór 2027-02-03: overschot **leeg**, V2 **afgewikkeld**, restant **leeg** (de
enige bijdrage is leeg). Zonder de verlenging zou T3 40 dagen overschot
hebben en V2 niet afgewikkeld zijn: het kenmerk van V2 mag pas na de
verlengde einddag van V2 gezet worden, en die pas na het restant van V1.

**case3-keten-van-drie** (drie termijnen, nul en één; de volgorde in het
bestand is noch die van de keten noch de alfabetische). eerste: basiseinddag
2026-03-01, geen vorige: **2026-03-01**. termijn a (2026-01-01 tot 2026-03-11):
**10**; termijn b (2026-02-01 tot 2026-03-21): **20**; termijn c (2026-03-05
tot 2026-04-01) begon na de verlengde einddag: overschot **leeg**, en het
filter van het restant laat haar weg. eerste niet afgewikkeld, restant 10 +
20 = **30**. tweede: basiseinddag 2026-08-01, vorige eerste: 2026-08-01 plus
30 dagen is **2026-08-31**; geen termijnen, dus **afgewikkeld** en restant
**leeg**. derde: basiseinddag 2027-01-01, vorige tweede met een leeg restant:
**2027-01-01**. termijn d (2026-12-01 tot 2027-01-31): **30**; derde niet
afgewikkeld, restant **30**.

| case | verlengde einddag | overschot | afgewikkeld | restant |
|---|---|---|---|---|
| `case1-een-vergunning` | V1 **2026-06-01** | T1 **10** | V1 nee | V1 **10** |
| `case2-keten-van-twee` | V1 **2026-06-01**, V2 **2027-02-03** | T1 **44**, T2 **20**, T3 **leeg** | V1 nee, V2 **ja** | V1 **64**, V2 **leeg** |
| `case3-keten-van-drie` | eerste **2026-03-01**, tweede **2026-08-31**, derde **2027-01-01** | a **10**, b **20**, c **leeg**, d **30** | eerste nee, tweede **ja**, derde nee | eerste **30**, tweede **leeg**, derde **30** |

Elke case pint ook dat het kanaal leeg is.

## Waarom er geen `source.rgs` bij staat

Het document is in RegelSpraak geschreven:

```
Eenheidsysteem Tijd
    de dag dg

Objecttype de Vergunning (mv: Vergunningen)
    de basiseinddag Datum in dagen;
    de verlengde einddag Datum in dagen;
    het restant Numeriek (geheel getal) met eenheid dg;
    is afgewikkeld kenmerk (bijvoeglijk);

Objecttype de Termijn (mv: Termijnen)
    de begindag Datum in dagen;
    de einddag Datum in dagen;
    het overschot (mv: overschotten) Numeriek (geheel getal) met eenheid dg;

Feittype vorige vergunning
    de vergunning Vergunning
    de vorige vergunning Vergunning
Eén vergunning heeft één vorige vergunning

Feittype vergunningstermijn
    de houder Vergunning
    de termijn (mv: termijnen) Termijn
Eén houder heeft meerdere termijnen

Regel overschot
    geldig altijd
        Het overschot van een Termijn moet berekend worden als de tijdsduur van de verlengde einddag van zijn houder tot zijn einddag in hele dagen
        indien hij aan alle volgende voorwaarden voldoet:
        - zijn begindag is eerder dan de verlengde einddag van zijn houder
        - de verlengde einddag van zijn houder is eerder dan zijn einddag.

Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de som van de overschotten van alle termijnen van de Vergunning die aan de volgende voorwaarde voldoen: zijn begindag is eerder dan de verlengde einddag van de Vergunning.

Regel afgewikkeld
    geldig altijd
        Een Vergunning is afgewikkeld
        indien de einddag van alle termijnen van de Vergunning eerder is dan de verlengde einddag van de Vergunning.

Regel verlengde einddag
    geldig altijd
        De verlengde einddag van een Vergunning moet berekend worden als zijn basiseinddag plus de som van de overschotten van alle termijnen van de vorige vergunning van de Vergunning
        indien hij aan alle volgende voorwaarden voldoet:
        - het restant van de vorige vergunning van de Vergunning is gevuld
        - de vorige vergunning van de Vergunning is niet afgewikkeld.

Regel verlengde einddag zonder restant
    geldig altijd
        De verlengde einddag van een Vergunning moet gesteld worden op zijn basiseinddag
        indien het restant van de vorige vergunning van de Vergunning leeg is.
```

De frontend weigert de kring nog in `check` (WF-045 in zijn eigen
afhankelijkheidsanalyse; de kaart `check-admits-instance-cycle`). Het IR is
gemaakt door de bron met één hulpattribuut te laten emitteren (het overschot,
het restant en het kenmerk lazen een `hulpeinddag`), en daarna in de JSON die
lezingen terug te zetten op de verlengde einddag en het hulpattribuut weg te
laten. Tot de frontend de vorm toelaat, is de golden handgeschreven, zoals
een golden voor een vorm die geen frontend nog maakt
(`corpus/golden/README.md`).

## Herkomst

Eigen werk (kaart `instance-cycle-household-reads`, 2026-10-22), naar de vorm
van de lezingen binnen het eigen huishouden van een recht in een regelset
voor de herleving van een WW-recht (een vouw over de eindigingsgronden van
het recht, een vouw over de inkomens met in het filter het recht van het
inkomen).
