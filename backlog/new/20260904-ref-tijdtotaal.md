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
  | `backends/clingo` | weigert reden-dragend (`TRANSLATION FAULT: non-identical units under the write …`) — **pre-existent**, de ASP-laag draagt de vorm sowieso niet |

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

Out of scope: clingo's tijdgranulariteitsgrens (de ASP-laag draagt de
set-fold `Totaal` niet en dat is een eigen kaart), de frontends — de
typering is per maintainerbesluit 2026-09-04 beslist en staat hier niet
opnieuw ter discussie.
