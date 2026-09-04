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

### De handberekening, vóór enige executor

Per-maand-rooster, halfopen segmenten, hele eenheden per segment:

| bron | segmenten | hele maanden | × waarde | resultaat |
|---|---|---|---|---|
| `bewaargeld` (eigen tijdlijn) | 10 €/mnd op [2024-01-01, 2025-01-01) | 12 | 12 × 10 | **120 €** |
| `bewaargeld` (asOfDate-bucket 2024-01-01) | 10 €/mnd op [2024-01-01, 2024-02-01) | 1 | 1 × 10 | **10 €** |
| per-element V1 | 10 €/mnd over heel 2024 | 12 | 12 × 10 | 120 € |
| per-element V2 | 5 €/mnd op [2024-01-01, 2024-07-01) | 6 | 6 × 5 | 30 € |
| per-element V3 | geen tijdlijn | — | — | draagt niets bij |
| per-element totaal | — | — | 120 + 30 + 0 | **150 €** |

De noemer `mnd` valt in elke rij weg tegen de duur in maanden; het
resultaat draagt `€`. Het GETAL is in alle rijen precies de bestaande
eenheidloze telling — de kaart verandert alleen de eenheid, nooit het
getal. Daarom veranderen de vijf eenheidloze temporele TOTAAL-getuigen
niet (gemeten: poort 5 blijft 403 cases).

### Wat landde — route A: de eigen-tijdlijnvouw

`Timeline.totaal` vermenigvuldigt elk segment niet meer met een
eenheidloze telling maar met een **duur in de noemer-eenheid van de
segmentwaarde**; `Arith.mul` laat de noemer wegvallen (de bestaande
`€/mnd × mnd = €`-tak). Een noemer die niet de eigen
granulariteitseenheid noemt — `€/jr` op een maandrooster — weigert luid
in plaats van een niet-verklaarde omrekening te verzinnen. De frontend
laat die vorm wél door (gemeten: `€/jr voor elke maand` emitteert
schoon), dus de weigering is bereikbaar uit geldige bron; Drools
vermenigvuldigt daar stil met de maandtelling. Dat is een bewuste,
luide divergentie, geen stille.

```
voor : EVALUATION FAULT: incompatible units: EUR vs EUR
na   : oracle: PASS   (bewaargeld = 10 EUR, asOfDate-bucketcase)
```

Op tijdlijnniveau, de spec-waarde: `WV-3.6-g` pint 12 × 10 €/mnd =
**120 €** met `noemer = None`. Drools, van dezelfde wire, op dezelfde
volle tijdlijn: `PLAYED 1` op 120. En op de bucketcase: `PLAYED 1` op
10. Beide executors, beide vormen, gelijk.

### Wat landde — route B: de §7.1 per-element-vouw

De set-vouw las `World.attr` van een temporeel element; tijdlijn en
bucketwaarde zijn disjunct in `World`, dus dat leverde leeg en de vouw
gaf stil 0. Nu vouwt elk element éérst zijn eigen tijdlijn (ch. 3.6, op
de granulariteit van dat attribuut) en pas die scalairen vouwen over de
set. Een case die de asOfDate-bucketwaarde levert volgt dezelfde
conventie als de eigen-tijdlijnvouw. Rangschikkende en tellende vouwen
(min/max/eerste/laatste/aantal) over dezelfde vorm weigeren luid — er is
geen gedefinieerd moment om op te rangschikken; dat spiegelt de marker
die beide Drools-transpilers daar zetten.

```
voor : MISMATCH: J1.bewaarlast = 0, expected 150
na   : oracle: PASS (2 expectations)
```

En per element apart gemeten, tegen de handberekening:

```
V1        verwacht  120 -> oracle: PASS
V2        verwacht   30 -> oracle: PASS
V3        verwacht    0 -> oracle: PASS
V1+V2+V3  verwacht  150 -> oracle: PASS
```

Drools, van dezelfde wire, dezelfde case: `PLAYED 1` op 150. De
0-vs-150-divergentie is dicht.

Geen enkele corpus- óf toka-golden draagt een set-vouw over een temporeel
attribuut (geverifieerd met een scan over alle `ir.json`), dus deze route
kon geen bestaande verwachting raken.

### De clingo-guard (gemandateerde may-touch-verruiming)

Eén guard op de fail-fast-lijn in de `Totaal`-arm van
`backends/clingo/lib/translate.ml`. Zelfde bron, zelfde case als meting
(3) hierboven:

```
voor : MISMATCH: J1.bewaarlast is underived (leeg), expected 15
na   : TRANSLATION FAULT: the per-element temporal total over a set (§7.1)
       is not translated: this arm totals the SUBJECT's own timeline, and
       "bewaartarief" is the element type's attribute
```

`ref` gaf op dezelfde vorm zowel vóór als ná `oracle: PASS` op 15, en
Drools `PLAYED 1` op 15 — het antwoord stond dus vast en clingo week er
stil van af. Nu weigert hij luid. Poort 6 blijft 403: geen corpuscase
draagt de vorm.

Niet verder gebouwd, zoals opgedragen: de set-vouw écht vertalen is een
eigen kaart. Bevinding voor die kaart: dezelfde arm mist óók de
`navigatie_rol`/`navigatie_pad`-vernauwing en de
subject-objecttypecontrole die de zusterarm met de `gedurende`-clip
(:1473) wél draagt — dat is dezelfde klasse verkeerd-object-lek en
verdient één opruiming.

### De casebeslissing: de golden blijft CASELOOS

Poort 6 heeft geen weigerkanaal, en `backends/clingo` weigert **beide**
routes van `corpus/golden/jutterswet-tempototaal`, elk om een eigen
reden. Gemeten ná de ref-implementatie:

```
heel document, bucketcase:
  ref    : oracle: PASS
  clingo : TRANSLATION FAULT: expression kind not translated
           (no corpus case exercises it)          <- de set-vouw-Totaal van bewaarlast

alleen bewaargeld, eigen document, bucketcase:
  ref    : oracle: PASS
  clingo : TRANSLATION FAULT: non-identical units under the write to
           "bewaargeld" are not translated (ref restates with an exact
           conversion factor this integer encoding does not carry)
```

De tweede is de kern: de ASP-laag draagt de temporele `Totaal` alleen
waar de schrijf-eenheid identiek is aan de waarde-eenheid, en juist het
wegvallen van de noemer maakt dat hier onmogelijk. Dus geen
`case*.json`; poorten 5 en 6 blijven **403** met de no-case-lijst op
**3** (`jutterswet-seizoenvondst`, `jutterswet-tempototaal`,
`jutterswet-vormenkabinet`). De pins wonen in `ref/test` (`WV-3.6-g`,
`WV-3.6-h`, de twee spec-voorbeelden verbatim) en de NOTE van de golden
draagt de meting.

### De show_unit-getuige

`show_unit` toont de noemer recursief; `conv_factor`'s
`rate/plain unit mismatch` erft dezelfde helper (dezelfde blinde vlek,
dezelfde functiegroep).

```
voor : EVALUATION FAULT: incompatible units: EUR vs EUR
na   : EVALUATION FAULT: incompatible units: EUR vs EUR/mnd
```

**Geen enkel `corpus/rood`-geval pinde een eenheidsfragment** (gegrept
over de hele corpus: alleen de NOTE van deze golden noemde er een, en
dat is proza, geen verwachting). Er verandert dus geen enkele
verwachting mee.

**En er komt geen nieuw rood-geval.** Poort 8 eist dat een rood geval in
`ref` én `clingo` observeerbaar faalt. Elke vorm die deze melding nog
kan halen, sneuvelt eerder: de frontend weigert `€/kg voor elke maand`
statisch (§4.20 eist een tijdseenheid-noemer), weigert sinds
2026-09-04 een `€/mnd`-doel op de gestripte set-lezing, en de enige
vorm die de melding vroeger wél haalde — de niet-set, niet-temporele
rate-lezing — weigert in beide executors om een ándere reden dan de
eenheden (gemeten, letterlijk):

```
ref    : EVALUATION FAULT: unsupported instance-fold source shape
clingo : TRANSLATION FAULT: non-identical units under the write to
         "bewaargeld" are not translated (…)
```

Twee verschillende weigeringen, geen gedeelde eenheidsfault om te
pinnen. Een geval bouwen dat wél in beide
op de eenheidsfault landt zou een met de hand geschreven `ir.json`
vergen die de frontend nooit emitteert — een kunstgreep. De melding is
daarom gepind in `ref/test` (`verify-show-unit-noemer`,
`verify-show-unit-in-fault`), niet in `corpus/rood`.

### De spec-clausule (VOORSTEL — beschermd pad)

`spec/RUNTIME-SEMANTICS.md` ch. 3.6 kreeg drie clausules, geschreven
vóór de implementatie (commit 2 van deze tak):

1. de bestaande *Units*-bullet heet nu *Units, the plain operand* —
   inhoud ongewijzigd;
2. *Units, the explicit rate operand* — de integratie, het wegvallen van
   de noemer, en de eis dat de noemer de eigen granulariteitseenheid
   noemt (met de luide weigering erbij);
3. *The per-element fold over a set (§7.1)* — tweemaal vouwen, en de
   weigering van rangschikkende vouwen.

Plus twee gewerkte voorbeelden (120 € en 150 €), die `ref/test` verbatim
reproduceert. De maintainer merget dit; de tak is het voorstel.

### Wat openbleef

* **De volle-tijdlijnfence op de eigen-tijdlijnvouw zonder filter.** Een
  case die de VOLLE tijdlijn levert in plaats van de asOfDate-bucket
  loopt nog steeds op het oudere, niet-eenheidsgerelateerde hek
  (`the temporal total of … over a case-supplied full timeline is not
  exercised by any corpus case`). Bewust niet opgeheven: er is nog
  steeds geen corpuscase voor die vorm, en twee toegelaten
  invoervormen met verschillende uitkomsten voor hetzelfde attribuut is
  precies wat dat hek voorkomt. Gevolg voor déze kaart: de 120 is in
  `ref` gemeten op tijdlijnniveau (`WV-3.6-g`), niet end-to-end; de
  end-to-end-vergelijking met Drools loopt via de bucketcase (beide 10)
  en via de per-element-vouw (beide 150, en daarbinnen V1 = 120).
* **De clingo-set-vouw** blijft onvertaald (eigen kaart), inclusief de
  opruiming van de armvernauwing hierboven.
* **De niet-vertaalde `€/mnd`-schrijf in clingo** — de tweede weigering
  bij de casebeslissing — is de echte poortwachter voor een case op
  deze golden.

### Poorten (alle dertien groen)

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 175 file(s) validate and round-trip)
check-verifier: PASS (175 golden clean; 257 negatief rejected for their declared rule)
check-case-schema: PASS (schema valid; 425 case file(s) validate)
check-oracle: PASS (403 cases replayed exactly; NOT replayed, no case file yet:
  corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-tempototaal
  corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (403 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
check-rood: PASS (22 red cases fault observably in ref and clingo)
check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically
check-regelspraak: PASS — JUnit suite green (171 tests); 29 rgs golden(s) re-emit byte-identically
check-toka-parity: PASS (45 .rgs byte-identical; 29 toka cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)
check-drools: PASS (played 106; auto-excluded 43; value-divergence 1; JUnit 199 tests)
check-reftype-derivation: PASS (52 annotations twisted over 11 goldens; 0 pinned survivors)
```

`ref/test`: 378 → **382** gewerkte voorbeelden (vier nieuwe getuigen).
