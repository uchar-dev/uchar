# jutterswet-vergunningketen — een waarde die dezelfde waarde van de vorige vergunning leest

Een Vergunning kan een vorige vergunning hebben. De einddag van een
Vergunning wordt verlengd met het restant van de vorige vergunning, en het
restant van een Vergunning hangt af van haar eigen einddag. Per type is dat
een kring (restant → einddag → restant); per object een keten die eindigt
bij een vergunning zonder voorganger. Het document is de getuige van de
instantiegeordende kring (`spec/IR-SPEC.md` ch. 5.3,
`spec/RUNTIME-SEMANTICS.md` ch. 6.2): de verifier laat de kring toe, en de
vormen rekenen per object in de volgorde van de keten.

Twee kringen over dezelfde relatie `vorige vergunning`:

- **met de leeg-toets**: "einddag" (basiseinddag plus het restant van de
  vorige vergunning, indien dat gevuld is) en "einddag zonder restant" (de
  basiseinddag, indien het restant van de vorige vergunning leeg is), met
  "restant" (de dagen van de stopdag tot de einddag, indien beide gevuld
  zijn en de stopdag eerder is). De leeg- en gevuld-toets op de voorganger
  zijn negatieve verschoven randen; de gevuld-toets op de eigen einddag is
  een negatieve lokale rand.
- **in de positieve vorm, met een satelliet**: "verlengde einddag"
  (basiseinddag plus het overschot van de termijn van de vorige
  vergunning, zonder voorwaarde; een leeg overschot telt als 0 dagen,
  ch. 1.2) en "overschot" op de Termijn (de dagen van de stopdag tot de
  verlengde einddag van zijn houder). De Termijn is een satelliet: de
  relatie `vergunningstermijn` is één-op-één en geen vorm schrijft haar.
  De lezing van de verlengde einddag gaat over één stap naar de voorganger
  en één stap naar diens termijn; de lezing van het overschot over één stap
  terug naar de houder.

## De handberekening

De duur in hele dagen tussen twee datums is hun verschil in kalenderdagen;
een datum plus een aantal dagen is een kalenderstap (ch. 3.8).

- 2026-03-01 tot 2026-06-01: maart 31, april 30, mei 31 → **92**.
- 2026-12-01 plus 92 dagen: december 30 (tot 31-12), januari 31, februari 28
  (2027 is geen schrikkeljaar) is 89, plus 3 → **2027-03-03**.
- 2026-09-01 tot 2027-03-03: september 30, oktober 31, november 30,
  december 31, januari 31, februari 28 is 181, plus 2 → **183**.
- 2027-06-01 plus 183 dagen: juni 30, juli 31, augustus 31, september 30,
  oktober 31, november 30 → **2027-12-01**.

| case | invoer | einddag | restant | verlengde einddag | overschot |
|---|---|---|---|---|---|
| `case1-zonder-voorganger` | V1: stopdag 2026-03-01, basiseinddag 2026-06-01, termijn T1 | V1 **2026-06-01** | V1 **92** | V1 **2026-06-01** | T1 **92** |
| `case2-keten-van-twee` | V1 als in case 1; V2: basiseinddag 2026-12-01, vorige V1, termijn T2 | V1 **2026-06-01**, V2 **2027-03-03** | V1 **92**, V2 **leeg** | V1 **2026-06-01**, V2 **2027-03-03** | T1 **92**, T2 **leeg** |
| `case3-keten-van-drie` | eerste als V1 (termijn eerste); tweede: stopdag 2026-09-01, basiseinddag 2026-12-01, vorige eerste, geen termijn; derde: basiseinddag 2027-06-01, vorige tweede (termijn derde) | eerste **2026-06-01**, tweede **2027-03-03**, derde **2027-12-01** | eerste **92**, tweede **183**, derde **leeg** | eerste **2026-06-01**, tweede **2027-03-03**, derde **2027-06-01** | termijn eerste **92**, termijn derde **leeg** |

Zonder voorganger is de einddag de basiseinddag: het restant van de vorige
vergunning is leeg, want er is geen vorige (ch. 5.8). Een vergunning
zonder stopdag heeft een leeg restant en een leeg overschot. In case 3 heeft
`tweede` geen termijn: de verlengde einddag van `derde` leest een leeg
overschot en blijft de basiseinddag, terwijl de einddag van `derde` het
restant van `tweede` (183) erbij telt. De labels van case 3 zijn zo gekozen
dat noch de volgorde in het bestand (tweede, derde, eerste) noch de
alfabetische (derde, eerste, tweede) de volgorde van de keten is. Elke case
pint ook dat het kanaal leeg is.

## Waarom er geen `source.rgs` bij staat

Het document is in RegelSpraak geschreven:

```
Eenheidsysteem Tijd
    de dag dg

Objecttype de Vergunning (mv: Vergunningen)
    de stopdag Datum in dagen;
    de basiseinddag Datum in dagen;
    de einddag Datum in dagen;
    het restant Numeriek (geheel getal) met eenheid dg;
    de verlengde einddag Datum in dagen;

Objecttype de Termijn (mv: Termijnen)
    het overschot Numeriek (geheel getal) met eenheid dg;

Feittype vorige vergunning
    de vergunning Vergunning
    de vorige vergunning Vergunning
Eén vergunning heeft één vorige vergunning

Feittype vergunningstermijn
    de houder Vergunning
    de termijn Termijn
Eén houder heeft één termijn

Regel restant
    geldig altijd
        Het restant van een Vergunning moet berekend worden als de tijdsduur van zijn stopdag tot zijn einddag in hele dagen
        indien hij aan alle volgende voorwaarden voldoet:
        - zijn stopdag is gevuld
        - zijn einddag is gevuld
        - zijn stopdag is eerder dan zijn einddag.

Regel einddag
    geldig altijd
        De einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het restant van de vorige vergunning van de Vergunning
        indien het restant van de vorige vergunning van de Vergunning gevuld is.

Regel einddag zonder restant
    geldig altijd
        De einddag van een Vergunning moet gesteld worden op zijn basiseinddag
        indien het restant van de vorige vergunning van de Vergunning leeg is.

Regel overschot
    geldig altijd
        Het overschot van een Termijn moet berekend worden als de tijdsduur van de stopdag van de houder van de Termijn tot de verlengde einddag van de houder van de Termijn in hele dagen
        indien hij aan alle volgende voorwaarden voldoet:
        - de stopdag van de houder van de Termijn is gevuld
        - de verlengde einddag van de houder van de Termijn is gevuld
        - de stopdag van de houder van de Termijn is eerder dan de verlengde einddag van de houder van de Termijn.

Regel verlengde einddag
    geldig altijd
        De verlengde einddag van een Vergunning moet berekend worden als zijn basiseinddag plus het overschot van de termijn van de vorige vergunning van de Vergunning.
```

De frontend weigert de kring nog in `check` (WF-045 in zijn eigen
afhankelijkheidsanalyse) en emitteert dan niets. Het IR is gemaakt door de
bron met twee hulpattributen te laten emitteren (de restant-regel las een
`hulpeinddag`, de overschot-regel een `hulpverlenging`), en daarna in de
JSON die lezingen terug te zetten op `einddag` en `verlengde einddag` en de
hulpattributen weg te laten. Tot de frontend de vorm toelaat, is de golden
handgeschreven, zoals een golden voor een vorm die geen frontend nog maakt
(`corpus/golden/README.md`).

## Herkomst

Eigen werk (kaart `cycle-across-instances`, 2026-10-22), naar de vorm van
art. 42b WW (de duur van een nieuw recht wordt verlengd met het restant van
het eerdere recht); case 2 is de casus van de kaart.
