# ref leert de Tabel-30-tijdintegratie: de tijdlijnvouw vermenigvuldigt met de duur

Goal: `ref` voert de §4.20-noemerstrip op **geen enkele route** uit. De
frontend typeert `het totaal van <€/mnd-tijdlijn>` als `€` (Tabel 30 rij 2,
resultaat `Eenheid1`) omdat de operator over de tijd **integreert**; `ref`
vouwt de tijdlijn met een **eenheidloze** telling, zodat de `€/mnd`-eenheid
de collapse overleeft en het `€`-doel hem weigert. Gemeten op de
`jutterswet-tempototaal`-golden (regel `bewaargeld`, asOfDate-bucketcase):

```
ref:  EVALUATION FAULT: incompatible units: EUR vs EUR
```

Twee lekken maken het één kaart:

* **De per-element-vouw divergeert in waarde.** `het totaal van <temporeel
  attribuut> van alle <set>` is de §7.1 per-element-tijdlijnvouw: elk
  element wordt éérst over de tijd gevouwen, daarna over de set. Gemeten op
  één bron (drie vondsten: 10 €/mnd over heel 2024, 5 €/mnd over het eerste
  halfjaar, één zonder tijdlijn):

  | executor | uitkomst |
  |---|---|
  | Drools | `PLAYED` op **150** (120 + 30 + 0) |
  | `ref` | `MISMATCH: J1.bewaarlast = 0, expected 150` |
  | `backends/clingo` | weigert op déze vorm reden-dragend (`TRANSLATION FAULT: non-identical units under the write …`) — pre-existent. NB (lens 2): de `Totaal`-arm in `translate.ml` (~:1682) mist een `set = false`-guard; op een EENHEIDLOZE, jaar-granulaire set-operand vuurt hij tegen het subject i.p.v. het element en antwoordt FOUT (`underived` waar ref en Drools 15 geven) — die guard hoort bij deze kaart of een eigen regel |

  Drools is hier de **gemeten referentie**: zijn twee transpilers
  (`ExecModelTranspiler` r. 962/1060-1065, `DrlTranspiler` r. 2882/2917)
  wikkelen de elementwaarde in `rsTijdTotaal`, en de toolchain-differential
  `ExecModelDifferentialTest` Phase PE pint 120+30+0=150 absoluut. `ref`
  geeft 0 — het vouwt de `List<Segment>` niet.

* **De plain-tijdlijnvouw voert de strip niet uit.** `Timeline.totaal`
  vermenigvuldigt elk segment met een eenheidloze duurtelling in plaats van
  met de duur *in de noemer-eenheid*, dus de eenheid komt er ongewijzigd
  uit. Drools speelt diezelfde regel wél: `bewaargeld = 120` bij een
  tijdlijn van 10 €/mnd over 2024 (`PLAYED 1`).

Onzichtbaar tot 2026-09-04, omdat élke temporele `TOTAAL` in het corpus
eenheidloos is (`jutterswet-getijdenwacht`, `jutterswet-penningjaar`,
`synth2-clip-over-prorata`, `synth2-clip-cond-over-prorata`,
`anchor/jutterswet-dubbelwacht`) en de strip daar een no-op is.

Twee extra's die het werk raken en apart genoemd moeten worden:

* `spec/RUNTIME-SEMANTICS.md:1024-1028` regelt alleen de **plain-unit**
  tijdcase ("a plain `€`-valued per-month timeline totals to `€` by value ×
  month-count") en **zwijgt** over een operand met een expliciete
  tempo-eenheid (`€/mnd voor elke maand`) — precies de vorm waarop de
  frontend de strip toepast. De spec-zin moet die vorm dekken vóór `ref`
  hem implementeert, anders implementeert `ref` een ongeschreven regel.
* `ref/lib/arith.ml:16-21` — `show_unit` print alleen de **afkorting** van
  de teller, zodat een `EUR/mnd` en een `EUR` allebei als `EUR` verschijnen.
  Vandaar de nutteloze melding `incompatible units: EUR vs EUR`. Een
  noemer-tonende `show_unit` maakt elke toekomstige eenheidsfault leesbaar
  en is een voorwaarde voor bruikbare rood-metingen op deze kaart.

Acceptance:
- `ref` voert de Tabel-30-integratie uit op **beide** tijdlijnvouwen: de
  segmentwaarde × duur in de **teller-eenheid** (`€/mnd` × maanden = `€`),
  zowel voor de eigen-tijdlijnvouw als voor de per-element-vouw over een
  set. Absolute pins eerst met de hand, dan gemeten tegen Drools (120 op
  `bewaargeld`, 150 op de per-element-vouw).
- De 0-vs-150-divergentie is dicht: `ref` en Drools geven dezelfde waarde
  op de per-element-vouw.
- De golden `corpus/golden/jutterswet-tempototaal` krijgt zijn `case*.json`
  zodra `ref` de vorm draagt — de handberekende absolute waarden staan al
  in haar `NOTE.md`. Als clingo de set-fold `Totaal` dan nog steeds niet
  draagt, blijft de golden caseloos en zegt de kaart dat met de meting
  erbij (poort 6 heeft geen weigerkanaal).
- `show_unit` toont de noemer, en een negatief/rood-getuige pint dat de
  eenheidsfault de twee eenheden onderscheidbaar noemt.
- De RUNTIME-SEMANTICS-clausule dekt de expliciete-tempo-eenheid-operand,
  met gewerkt voorbeeld (proposal — beschermd pad).
- Alle dertien gates groen.

May touch: `ref/**`, `corpus/golden/jutterswet-tempototaal/case*`,
`backlog/`. (`spec/RUNTIME-SEMANTICS.md` alleen als **voorstel** —
beschermd pad, de maintainer merget.)

**Verruimingen op de may-touch, met reden:**

* `backends/clingo/lib/translate.ml` — **één guard**, expliciet
  maintainermandaat (zie Results, "de clingo-guard"): de `Totaal`-arm mist
  een `set`-guard en antwoordt op de §7.1-vorm stil fout tegen het
  verkeerde subject. De verruiming is precies die ene fail-fast-regel;
  de set-vouw zelf blijft onvertaald (eigen kaart).
* `corpus/golden/jutterswet-tempototaal/NOTE.md` — de NOTE draagt de
  gemeten `ref`-weigering (`incompatible units: EUR vs EUR`) als
  vaststaand feit. Die meting is ná deze kaart onwaar; een NOTE die een
  opgeheven defect als staand beschrijft is een leugen in een
  artefactzone. Alleen die passage wordt bijgewerkt, met de nieuwe
  metingen erin.

Out of scope: clingo's tijdgranulariteitsgrens (de ASP-laag draagt de
set-fold `Totaal` niet en dat is een eigen kaart), de frontends — de
typering is per maintainerbesluit 2026-09-04 beslist en staat hier niet
opnieuw ter discussie.

## Results

### Rood eerst — de drie metingen vóór één regel code veranderde

Alle drie op de commit waar de kaart geclaimd werd, met de
`scripts/check-oracle.sh`-aanroepvorm (`runner.exe <ir> <case>`).

**(1) De eigen-tijdlijnvouw, `bewaargeld` van
`corpus/golden/jutterswet-tempototaal` — asOfDate-bucketcase**
(J1, `bewaarvergoeding per maand` = 10 €/mnd als bucketwaarde,
asOfDate 2024-01-01, verwacht 10):

```
EVALUATION FAULT: incompatible units: EUR vs EUR
```

**(1b) Dezelfde regel met een VOLLE tijdlijn** (10 €/mnd over heel 2024,
verwacht 120) loopt op een tweede, oudere hek — niet op de eenheden:

```
EVALUATION FAULT: the temporal total of "bewaarvergoeding per maand" over a
case-supplied full timeline is not exercised by any corpus case; the
asOfDate-bucket convention carries the total
```

**(2) De per-element-vouw over een set** (§7.1). Eigen bron in `/tmp`,
door de frontend geëmit: `bewaartarief` van `Vondst` is
`€/mnd voor elke maand`, `bewaarlast` van `Jutter` is `€`, regel
`het totaal van het bewaartarief van alle vondsten van de Jutter`. De
frontend typeert het resultaat `€` (de strip vuurt — `setLezing` sluit
de temporele set-lezing uit). Case: V1 = 10 €/mnd over heel 2024,
V2 = 5 €/mnd over het eerste halfjaar, V3 zonder tijdlijn; verwacht 150:

```
MISMATCH: J1.bewaarlast = 0, expected 150
```

**(3) De clingo-`Totaal`-arm zonder `set`-guard** (`translate.ml` ~:1682).
Eigen bron in `/tmp`: `bewaartarief` van `Vondst` **eenheidloos,
`voor elk jaar`**, `bewaarlast` van `Jutter` eenheidloos, dezelfde
set-vouw; V1 = 10, V2 = 5 als bucketwaarden op asOfDate 2024-06-15;
handberekend 10×1 + 5×1 = **15**:

```
ref    : oracle: PASS (2 expectations)
clingo : MISMATCH: J1.bewaarlast is underived (leeg), expected 15
```

De arm matcht op `attr_temporal` van het ELEMENT-objecttype maar emitteert
`val(<subject>, ...)`, dus hij leest het attribuut van het verkeerde
object. Geen weigering, geen melding — een stil fout antwoord.
