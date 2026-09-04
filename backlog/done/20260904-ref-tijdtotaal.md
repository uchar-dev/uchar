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
  | `backends/clingo` | weigert op déze vorm reden-dragend (`TRANSLATION FAULT: non-identical units under the write …`) — pre-existent. NB (lens 2): de `Totaal`-arm in `translate.ml` (~:1682) mist een `set = false`-guard; op een EENHEIDLOZE, jaar-granulaire set-operand vuurt hij tegen het subject i.p.v. het element en antwoordt FOUT (`underived` waar ref en Drools 15 geven) — die guard hoort bij deze kaart of een eigen regel. **Geland** in commit `e124372` van deze tak, en de fixronde trok hem door naar de `Som`- en `Min`/`Max`-arm (zie Results, "Fixronde"); wat er nog aan armvernauwing openstaat is `backlog/new/20260904-clingo-setfold-armen.md` |

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
resultaat draagt `€`. In deze rijen — en alleen daar — is het GETAL
precies de bestaande eenheidloze telling: de noemer is hier de
roostereenheid zelf, dus de omrekenfactor is 1. **De ruimere claim "de
kaart verandert alleen de eenheid, nooit het getal" is onwaar** en is in
de fixronde ingetrokken; hij geldt uitsluitend voor de segment-collapse
waar noemer en roostereenheid samenvallen. Vier gemeten tegenvoorbeelden
staan onder "Fixronde" (`€/jr` → 10 i.p.v. 120, `€/dg` met een
gedeclareerde factor → 3600, en de twee weigerende vormen). Wat wél
volgt: de vijf eenheidloze temporele TOTAAL-getuigen veranderen niet,
want een eenheidloze waarde raakt de roostereenheid nooit (gemeten:
poort 5 blijft 403 cases).

### Wat landde — route A: de eigen-tijdlijnvouw

`Timeline.totaal` vermenigvuldigt elk segment niet meer met een
eenheidloze telling maar met een **duur in een tijdseenheid**;
`Arith.mul` laat de noemer wegvallen (de bestaande `€/mnd × mnd =
€`-tak). *In deze commit* was die duur nog uitgedrukt in de noemer van
de segmentwaarde en moest de noemer de eigen granulariteitseenheid
noemen — `€/jr` op een maandrooster weigerde. **De fixronde heeft dat
vervangen** (maintainerbesluit 2: de duur staat in de ROOSTEReenheid en
elke convertibele noemer integreert exact); zie "Fixronde" hieronder.
De frontend laat de `€/jr`-vorm door (gemeten: `€/jr voor elke maand`
emitteert schoon), dus beide takken zijn bereikbaar uit geldige bron.

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

**Bullets 2 en 3 zijn in de fixronde herschreven** — de exacte
integratie over de roostereenheid, de twee conventies naast elkaar, de
Drools-executor-note en de coverage-note. Wat hierboven staat beschrijft
de tussenstand van commit `bac25b1`, niet de eindtekst.

### Fixronde (2026-09-04, ná de twee lenzen)

Twee lenzen bevestigden de landing en maten vijf punten; de maintainer
nam er twee ontwerpbesluiten op. Alles hieronder is gemeten vóór en ná,
letterlijk.

#### Blokkerend: de subject-arm schaduwde de set-vouw

`eval.ml`'s eigen-attribuut-arm matchte op `navigatie_rol = None`,
`navigatie_pad = []` en `norm aot = norm subject`, maar **niet** op
`set = false`. Bij een **zelfrelatie** (elementtype = subjecttype) is dat
precies de §7.1-vorm, en de arm vuurde eerst: hij telde de tijdlijn van
het SUBJECT. De vorm is bereikbaar — poort 12 draagt de wederkerige
zelfrelatie. Gemeten op een wederkerige zelfrelatie (eigen bron,
`het totaal van het bewaartarief van alle strandmaten van de Jutter`;
J1 eigen bucketwaarde 99, lid J2 = 7; de §7.1-bullet eist 7):

```
bucketleden      voor : MISMATCH: J1.bewaarlast = 99, expected 7
                 na   : (met besluit 1) luide horizon-fault op lid "J2"
tijdlijnleden    voor : EVALUATION FAULT: the temporal total of "bewaartarief"
                        over a case-supplied full timeline is not exercised …
                 na   : oracle: PASS  (84 = 12 × 7, de §7.1-route)
clingo, beide    voor én na : TRANSLATION FAULT (de guard van deze tak)
```

Sinds de guard was dit óók `ref ≢ clingo`: ref antwoordde stil fout waar
clingo luid weigerde. `set = false` in het patroon sluit het.

#### Maintainerbesluit 1 — de conventie van de per-element-vouw

*"Een bucket in de per-element-vouw is toegestaan mits hij zijn eigen
horizon draagt. Een bucket zonder horizon in een set-vouw faalt luid —
de executor kan de integratie niet uitvoeren zonder te raden. Volle
tijdlijnen blijven altijd toegestaan. Mengen mag. §8.4 blijft intact."*

Uitgevoerd. Het case-schema (`spec/schema/uchar-case-v0.schema.json`)
kent voor een temporeel attribuut precies twee invoervormen: de
`value`-scalar (draagt géén tijdvak) en de `timelineValue`-variant (per
segment `from`/`to` — de enige zelfbeschrijvende, horizon-dragende
vorm). Dus: tijdlijnen mogen en mengen, de horizonloze bucketscalar
faalt luid mét het lid-label, een afwezig lid draagt 0 bij. De
subject-eigen route blijft ongewijzigd bucket-eisend; §8.4 en
`spec/schema/README.md` zijn niet aangeraakt.

Gemeten (eigen bron, `Vondst.bewaartarief` = `€/mnd voor elke maand`):

```
drie tijdlijnleden (120 + 30 + 0)  voor: 150   na: 150
één bucketlid                      voor:  10   na: EVALUATION FAULT … over "V1":
                                                   the case supplies an asOfDate-bucket
                                                   scalar, which carries no period of its
                                                   own …
menging tijdlijn + bucketlid       voor: 125   na: dezelfde fault, over "V2"
alle leden afwezig                 voor:   0   na:   0
```

De generieke `bucketing is defined for month and year only (ch. 3.2)`
die een DAG/WEEK/KWARTAAL-bucketlid opleverde is mee vervallen: die tak
bestaat niet meer. Gemeten op een `voor elke dag`-attribuut met een
bucketlid — vóór die melding, ná de horizon-fault.

#### Maintainerbesluit 2 — de vreemde noemer: volledig exact integreren

*"Elke naar de roostereenheid CONVERTIBELE noemer integreert exact via
de gedeclareerde factoren; alleen een niet-convertibele noemer weigert
luid."*

De rekenkracht bestond al (`Arith.mul`'s rate-cancel via `conv_factor`,
exact over rationalen); wat ontbrak was een eenheid-record voor de
ROOSTEReenheid. `Timeline.totaal` heeft nu een `?grid_unit`, eenmaal per
vouw opgelost door `Timeline.grid_unit` over `doc.eenheidstelsels` met de
ch.-3.8-spellingstabel, doorgegeven op alle drie de aanroepplekken in
`eval.ml`. De opzoeking kent drie uitkomsten en elke mislukking is luid:
geen roostereenheid gedeclareerd, meerdere met verschillende canonieke
ketens, of een noemer die de declaraties nergens aan die eenheid
verbinden — die laatste melding noemt nu **beide** eenheidsnamen (dat
deed hij niet). De onjuiste motiveringszin *"a conversion the
declaration does not state"* is weg: bij `€/jr` op een maandrooster
staat die omrekening er wél, in het eenheidstelsel.

Eerst met de hand, daarna door `ref` (unitniveau én end-to-end door de
runner op een eigen bron):

| vorm | handberekening | `ref` |
|---|---|---|
| `10 €/mnd`, heel 2024, maandrooster | 12 mnd × 10 = 120 € | **120 €** |
| `10 €/jr` (`jr = 12 mnd`), heel 2024 | 12 mnd = 1 jr → 10 €/jr × 1 jr = 10 € | **10 €** (`oracle: PASS`) |
| `10 €/bm` (`bm` canoniek mnd, factor 1), heel 2024 | 12 bm × 10 = 120 € | **120 €** |
| `10 €/bm`, één maandbucket | 1 bm × 10 = 10 € | **10 €** |
| `10 €/mnd` op een **kwartaal**rooster, heel 2024 | 4 kw × 3 mnd/kw × 10 = 120 € | **120 €** |
| `10 €/dg` met `mnd = 30 dg`, heel 2024 | 12 mnd × 30 dg/mnd × 10 = 3600 € | **3600 €** |
| `10 €/dg` zónder omrekening naar mnd | niet convertibel → weigering | `EVALUATION FAULT: the temporal total of a EUR/dg value cannot integrate over a mnd grid: the declarations connect no conversion between its denominator and the grid unit (ch. 3.6)` |
| `10 €/jr` zónder `jr = 12 mnd` | idem | dezelfde fault, met `EUR/jr` |

Kwartaal→maand blijkt dus convertibel zodra het stelsel het zegt (`kw`
canoniek mnd, factor 3), en dan is de uitkomst exact gelijk aan de
per-maand-lezing — een grover rooster verandert het getal niet. `€/dag`
op een maandrooster hangt volledig aan de declaratie: zonder
`mnd = 30 dg` blijft het de luide weigering, mét die regel integreert
het exact naar wat de declaratie zegt (3600, niet 3650) — de factor
regeert, niet de kalender.

**GEVOLG, prominent: de Drools-divergentie is nu SEMANTISCH.** De
gemeten backend weegt unit-blind: op een `€/jr`-per-maand-tijdlijn
antwoordt hij 120 waar deze regel 10 geeft — 12× te veel, en geen
weigering maar een getal. **Geen corpusvorm draagt hem** (elke temporele
TOTAAL in het corpus is eenheidloos), dus **poort 12 kan hem niet zien**.
Dat is kaartmateriaal voor de backend-kant; het staat ook als
executor-note in de spec-clausule.

#### Mandaatverruiming: clingo-symmetrie op alle vouwarmen

Van dezelfde klasse als de gemandateerde `Totaal`-guard, en hier
expliciet genoteerd als verruiming. De ref-fix landde op de GEDEELDE
vouwtak, dus `Som` erfde de §7.1-vouw mee; clingo's `Som`-arm (~:1293)
en `Min`/`Max`-arm (~:1358) hadden geen temporele controle:

```
Som, tijdlijnleden      voor: ref 150             clingo MISMATCH underived (leeg)
                        na  : ref 150             clingo TRANSLATION FAULT (luid)
Som, bucketleden        voor: ref  15             clingo 15  (toevallig gelijk)
                        na  : ref luide fault     clingo TRANSLATION FAULT (luid)
Min/Max, bucketleden    voor: ref luide weigering clingo PASS op 10 / 5
                        na  : ref luide weigering clingo TRANSLATION FAULT (luid)
Min/Max, tijdlijnleden  voor: ref luide weigering clingo underived
                        na  : ref luide weigering clingo TRANSLATION FAULT (luid)
Aantal, set-vorm        voor én na: beide luid (ref "unsupported AttribuutRef shape",
                                    clingo de catch-all) — mag zo blijven
```

`Eerste`/`Laatste` kennen geen set-arm (hun arm dispatcht op ≥2
opgesomde operanden), dus daar is niets te bewaken. Eindbeeld: op elke
temporele set-vouwvorm antwoordt clingo **luid of exact gelijk aan
ref** — nooit meer stil fout of leeg. Geen enkele corpus- óf
toka-golden draagt een set-vouw over een temporeel attribuut (opnieuw
gescand over alle `ir.json`), dus geen verwachting beweegt.

Ook meegenomen: de meldingstekst van de `Totaal`-guard was half-juist
bij een zelfrelatie ("… is the element type's attribute" terwijl
elementtype = subjecttype); hij zegt nu dat het attribuut van elk **LID**
gelezen wordt — een ander object, ook waar de typen samenvallen.

#### De kleinere punten

* **`timeline.ml`'s wildcard.** De duur-match ving naast `Unitless` ook
  `Product` stil onder de plain-regel. De match is expliciet gemaakt;
  een `Product` wordt uitsluitend door `Arith.mul` uit twee noemerloze
  eenheden gebouwd (dat staat nu in commentaar) en een guard faalt luid
  als een representatiewijziging die aanname breekt.
* **De dubbele foutstring.** `"temporal attribute %S without a
  granularity"` stond letterlijk op twee plekken; het is één helper
  (`Eval.attr_granulariteit`).
* **Segment-eenheidsmenging.** Gemeten en als precieze spec-zin
  vastgelegd: binnen één collapse worden de per-segment-producten met de
  gewone ch.-2.6-optelling gesommeerd, dus een **convertibele** zuster-
  eenheid wordt exact ge-restate'd (120 € + 500 ct = **125 €**), een
  **vreemde** weigert luid, en alleen de **unitless**-kant adopteert
  stil (10 + 5 € = 15 €). De case-loader hangt de éne gedeclareerde
  eenheid aan elk segment, dus geen case kan de vorm bereiken; de regel
  geldt voor berekende tijdlijnen.
* **De onpinbare zin.** *"the frontend's static claim and this rule are
  the same statement"* is niet end-to-end te dekken zolang geen
  `uchar-case-v0`-case de vorm kan dragen (poort 6 heeft geen
  weigerkanaal, en clingo vertaalt noch de set-vouw noch de
  eenheid-veranderende schrijf). De zin is vervangen door een eerlijke
  coverage-note: de regel is op `ref`-unitniveau gepind, de
  end-to-end-claim blijft proza.
* **De opruimkaart.** Het pre-existente verkeerd-object-lek van de
  filterloze temporele `Totaal`-arm is uitgeschreven als
  `backlog/new/20260904-clingo-setfold-armen.md`, met de reproductie
  erin: `clingo` levert bij naamdeling het getal van het VERKEERDE
  object (`V1.bewaarlast = [99]` waar de bron de Jutters 10 leest) en
  zonder naamdeling stil `underived`, terwijl `ref` de vorm weigert
  (`unsupported instance-fold source shape`).

#### Wat de fixronde openliet

* **De Drools-divergentie hierboven** — semantisch, onzichtbaar voor de
  poorten, backend-kaartmateriaal.
* **De §7.1-vouw in clingo** blijft onvertaald voor álle vouwsoorten;
  alle armen weigeren nu luid.
* **De armvernauwing** van `translate.ml` — eigen kaart (zie hierboven).

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

### Poorten (alle dertien groen — hermeten ná de fixronde)

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
check-rood: PASS (22 red cases fault observably in ref and clingo;
  Drools leg 5 fault-pinned, 17 not-carried with reason)
check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically
check-regelspraak: PASS — JUnit suite green (171 tests); 29 rgs golden(s) re-emit byte-identically
check-toka-parity: PASS (45 .rgs byte-identical, 2 holdouts; TOKA suite 27 tests;
  29 toka cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED, zero silent divergences)
check-drools: PASS (played 106; auto-excluded 43; value-divergence 1; no-case 3; JUnit 199 tests)
check-reftype-derivation: PASS (52 unit annotations twisted over 11 goldens; 50 executor
  replays reproduce the untouched outcome, 9 of them PLAYED by the DRL-text branch;
  0 pinned survivors)
```

Geen enkel poortgetal beweegt ten opzichte van vóór de fixronde: de
nieuwe weigeringen liggen alle op vormen die het corpus niet draagt.

`ref/test`: main draagt **376** gewerkte voorbeelden, de tak vóór de
fixronde **382** (zes nieuwe getuigen — de kaart claimde eerder ten
onrechte "378 → 382, vier"), en ná de fixronde **410** (nog eens
achtentwintig: zestien voor besluit 2 en de kleinere punten, twaalf voor
besluit 1).
