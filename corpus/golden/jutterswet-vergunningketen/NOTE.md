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

## De bron

De bron staat in `source.rgs`. Sinds de kaart `check-admits-instance-cycle`
(2026-10-22) laat `check` de kring toe en geeft de frontend dit `ir.json`
byte-identiek uit (gate 10). Tot dan was de golden handgeschreven: de
frontend weigerde de kring, en het IR is gemaakt door de bron met
hulpattributen te laten emitteren en die lezingen daarna in de JSON terug te
zetten.

## Herkomst

Eigen werk (kaart `cycle-across-instances`, 2026-10-22), naar de vorm van
art. 42b WW (de duur van een nieuw recht wordt verlengd met het restant van
het eerdere recht); case 2 is de casus van de kaart.
