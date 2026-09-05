# WF-036's datum-ORDENINGsarm leert de per-representatie-datumfamilie

Goal: WF-038 weigert sinds 2026-09-05 een ordenende fold die twee
datumprecisies mengt (`DatumInDagen` naast `DatumTijd`) — twee
representaties zonder gedeelde orde. De zusterroute, de
vergelijkingsoperator onder WF-036, doet dat nog niet: zijn
**gelijkheids**arm kruist de precisie wél ("equality across date
precisions", ~`spec/verifier/lib/checks.ml` r. 4586) en zijn
**numerieke ordenings**arm kruist eenheden, maar de **datum-ordenings**arm
(`eerder`/`later`/`eerder of gelijk`/`later of gelijk`) toetst alleen dat
beide operanden datums zijn — precisie noch eenheid. Die arm leert de
per-representatie-familie die WF-038 nu draagt.

Gemeten getuige (2026-09-05, op een hand-gebroken variant van
`corpus/golden/jutterswet-vondstnummer`: het attribuut `vondstdag`
verklaard en geannoteerd als `DatumTijd`, `aanmelddatum` blijft
`DatumInDagen`, in de bestaande `LATER_OF_GELIJK`-consistentie):

    verifier: PASS
    ref     : EVALUATION FAULT: unordered comparison:
              2025-03-05 vs @1740816000000ms
    clingo  : TRANSLATION FAULT: unscalable datatype in the ASP
              translation

Dus: verifier-schoon, en géén uitvoerder die het beantwoordt — dezelfde
vorm van gat dat de fold-kant net gesloten heeft. De Drools-kant is nog
niet gemeten en hoort bij de meetfase van deze kaart.

Acceptance:
- Rood-eerst: één negatief (`corpus/negatief/wf036-…`) dat vandaag
  verifier-PASS is, met de gemeten drieslag in zijn NOTE (Engels,
  self-contained) en een clausule-exacte falsifieerbaarheidszin.
- De clausule in `spec/verifier/lib/checks.ml` op de
  datum-ordeningstak van de vergelijking (voorstel), mutant-bewezen op
  precies die arm.
- Nul over-afwijzing: volledige byte-sweep over de vier zones (golden,
  rood, negatief, parity) waarin alléén het nieuwe negatief beweegt.
- De WF-036-tekst in `spec/IR-SPEC.md` (regel + indexrij) mee, en de
  WF-038-passage die deze arm vandaag als open noemt ("its DATE-ORDERING
  arm crosses neither") hermeten.
- Alle dertien gates groen; geen bestaande pin beweegt.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: de gelijkheidsarm van WF-036 (die kruist de precisie al);
de eenhedenhelft van WF-038 (nog steeds open, eigen werk); executors,
frontends en bestaande goldens.

---

## Result (2026-09-05)

Geland: de datum-ordeningsarm van WF-036 kruist de representatie. Nieuw
negatief `corpus/negatief/wf036-datumordening-precisie` (rood-eerst,
commit `3e3d667`), de clausule in `spec/verifier/lib/checks.ml` (commit
`5875136`), en het spec-voorstel — WF-036-regel, WF-036-indexrij en de
hermeten WF-038-parenthese (commit `7720de6`). Alle dertien poorten
groen; poort 3 van 297 naar 298 negatief, elke andere telling
ongewijzigd.

### Getuige, opnieuw gemeten (2026-09-05)

Het document: `corpus/golden/jutterswet-vondstnummer` met twee regels
verschil — de declaratie van `Vondst.vondstdag` van `DatumInDagen` naar
`DatumTijd`, en de ene operandannotatie die haar leest. Verder niets.

```
verifier (vóór de clausule): PASS
verifier (ná de clausule)  : WF-036 FAIL /consistenties/0/assertie:
                             date ordering across two date precisions
                             (day and millisecond), which share no order
ref                        : EVALUATION FAULT: unordered comparison:
                             2025-03-05 vs @1740787200000ms
clingo                     : TRANSLATION FAULT: unscalable datatype in
                             the ASP translation
Drools                     : EXCLUDE transpiler SKIP: consistentie:
                             aanmelding niet voor de vondstdag —
                             consistentie (schending buiten Phase-D
                             scope)
```

Drie afwijkingen van de kaart, alle drie meetresultaten:

1. **Het ms-getal.** De kaart noteert `@1740816000000ms`, de meting hier
   `@1740787200000ms`. Om de fault te bereiken moet de case-invoer mee:
   met de declaratie op `DatumTijd` weigert de ref-loader de `date`-
   variant (`CASE ERROR: V1.vondstdag: value variant "date" does not
   match the declared datatype`), dus is `vondstdag` herschreven als
   `dateTime` — en het gekozen tijdstip (`2025-03-01T00:00:00`) bepaalt
   het getal. De vorm van de getuige is dezelfde; alleen het lexeem
   verschilt.
2. **Drools werpt geen ClassCastException — hij weigert de kruising
   gericht.** De kaart verwachtte de fold-zusters
   `LocalDate`→`ChronoLocalDate`-cast. Gemeten: de backend komt er niet
   aan toe, want hij skipt dit document vóór het vuren. Dat is geen
   gebrek maar zijn eigen hek: de gelijk-precieze `DatumTijd`-ordening
   SPEELT wél — in deze consistentie én in een regelvoorwaarde — en
   discrimineert tot op de milliseconde (de omgekeerde verwachting op
   dezelfde invoer faalt, dus er wordt echt geëvalueerd). Alleen het
   gemengde paar wordt geweigerd, bij de expliciete
   `sameDatumKind`-bewaker, wiens eigen commentaar het een §5.3 Tabel
   36-mismatch noemt die nooit lossy gecoerceerd mag worden. De
   drieslag blijft intact in zijn kern (geen uitvoerder beantwoordt dit
   document) — en de derde uitvoerder trekt onafhankelijk exact de
   grens die deze clausule trekt. De zin die hier eerst stond ("de
   backend draagt überhaupt geen millisecondeprecieze
   datumvergelijking") was onwaar; zie de fixronde hieronder.
3. **De RegelSpraak-frontend weigert deze vorm níét.** Anders dan bij de
   fold-zuster: `Typering.vergelijk` laat `EERDER`/`LATER`/… de
   precisiegrens met opzet kruisen, met een test die dat vastlegt, en
   eist één precisie alleen voor gelijk/ongelijk, toekenning, tijdsduur
   en eerste/laatste. Deze clausule is dus een v1-aanscherping vóór de
   frontend uit, geen hek dat de schrijver inhaalt. Dat staat nu
   expliciet in de WF-036-tekst, de indexrij en de NOTE. De frontend
   bijtrekken viel buiten de kaart en is niet gedaan — het is de
   openstaande consequentie van dit voorstel.

### Mutant-bewijs, precies op de arm

Mutant: laat de datum-ordeningsarm de twee representaties als één
familie lezen (`Datum_in_dagen` en `Datum_tijd` beide één `Date`-tag).
Sweep over de vier zones (golden, rood, negatief, parity — 504
documenten), rapport per document byte-vergeleken:

```
goed → mutant: 1 regel verandert, en het is het nieuwe negatief
  corpus/negatief/wf036-datumordening-precisie/ir.json
    WF-036 FAIL … → verifier: PASS
```

Dezelfde sweep vóór/ná de clausule geeft precies dezelfde ene regel:
nul over-afwijzing, de 503 bestaande documenten houden hun rapport
byte-identiek.

### Poorten (alle dertien groen)

```
 1 check-self-contained     PASS
 2 check-ir-schema          PASS (176 bestanden valideren en round-trippen)
 3 check-verifier           PASS (176 golden clean; 298 negatief
                                  afgewezen — was 297)
 4 check-case-schema        PASS (427 case-bestanden valideren)
 5 check-oracle             PASS (405 cases exact herspeeld; 2 golden
                                  zonder case: seizoenvondst,
                                  vormenkabinet)
 6 check-clingo             PASS (405 cases exact — ref = clingo)
 7 check-noop               PASS (369 weerleggen; 36 gepinde overlevers)
 8 check-rood               PASS (22 rode cases; Drools-been 5
                                  fault-pinned, 17 not-carried)
 9 check-catala             PASS (201 checks; 12 catala-golden
                                  byte-identiek)
10 check-regelspraak        PASS (171 JUnit-tests; 30 rgs-golden
                                  byte-identiek)
11 check-toka-parity        PASS (ref 29/29, clingo 29/29, Drools 29
                                  PLAYED; 45 .rgs byte-identiek;
                                  TOKA-suite 27 tests)
12 check-drools             PASS (108 played; 1 value-divergence
                                  (ongewijzigd); JUnit 199)
13 check-reftype-derivation PASS (64 twists over 13 golden; 58 replays,
                                  11 door de DRL-teksttak; 0 overlevers)
```

Het negatief telt alleen in poort 3 mee — de zone draagt geen
case-bestanden en geen bron — dus elke andere telling staat stil. Geen
bestaande pin bewoog.

### Wat open blijft

- De eenhedenhelft van WF-038 (numerieke fold-operanden) — ongemoeid,
  zoals de kaart voorschreef. WF-036 is daar geen dekking voor; de
  parenthese zegt dat nu ook, en noemt de datum-ordeningsarm gesloten.
- Op de datum-ordeningsarm zelf blijft niets open: een datum draagt geen
  eenheid, dus de eenhedenhelft heeft daar geen tegenhanger.
- **De frontend-flank** (afwijking 3 hierboven): `Typering.vergelijk`
  laat de vier ordeningsoperatoren de precisienaad met opzet kruisen —
  het commentaar erbij noemt Tabellen 45–52, die anders dan
  tijdsduur/eerste/laatste géén precisie-eis stellen. Eerlijk gezegd:
  dit is **de eerste plek in deze spec waar het draadhek vóór de
  frontend uit loopt**. Er is geen precedent — de zusterhekken worden
  door de frontend juist gevangen (WF-035's `datum + datum` valt in
  `Typering.reken` op `numeriekBinair` stuk). Praktische bijtkracht:
  een geschreven datumliteraal ZONDER tijddeel is dag-precisie (de
  DATE-token draagt het tijddeel optioneel), dus een
  `DatumTijd`-attribuut vergeleken met een gewoon geschreven datum ís
  de kruising die deze clausule nu weigert — de schrijver moet de tijd
  uitspellen om op één precisie te blijven.
  Voorgestelde deltarij voor `spec/CONSTRUCTS.md` (de `— Operator`-rij,
  r. 109, laatste kolom; **de rij zelf landt de hoofdsessie serieel** —
  CONSTRUCTS is voor deze kaart een verboden pad):

      EERDER/LATER are date-typed ordering — v1 may unify under typed
      ordering. **v1 delta (2026-09-05, WF-036):** the date-ordering
      arm is per REPRESENTATION — a day-precision and a
      millisecond-precision operand share no order and are refused at
      the wire, while `Typering.vergelijk` still admits the crossing
      (§5.7–5.10 Tabellen 45–52 impose no precision restriction). The
      first place the wire fence runs AHEAD of the frontend; bringing
      the frontend along is tracked work, not a silent divergence.

- **De ontbrekende ms-getuige.** Alle zeven datum-ordeningen in golden
  en rood zijn dag×dag; de toegelaten milliseconde-helft óók laten
  weigeren beweegt nul van de 504 rapporten. De helft rust dus op de
  uitvoerders, niet op het corpus: een golden met een gelijk-precieze
  `DatumTijd`-ordening is de ontbrekende regressiegetuige. Genoteerd in
  de spec-passage, de indexrij en de NOTE.
- `backlog/new/20260905-drools-datumtijd-iso-lexeem.md` — het
  wire-neutraliteitsgat dat de fixronde onderweg opgroef (zie hieronder).

### Fixronde (2026-09-05)

Twee lenzen lazen het geland werk na. De **kern hield**: de mutant
beweegt precies één van 504 rapporten, de sweep vóór/ná de clausule is
byte-identiek op de overige 503, en het rood-eerst is echt. Twee
tekstclaims waren **onwaar gemeten** en zijn hier vervangen; alles
hieronder is in deze ronde zelf opnieuw gemeten.

1. **"Drools draagt geen millisecondeprecieze datumvergelijking."**
   Onwaar. Hermeten met één gelijk-precieze attribuut-vs-attribuut-probe
   (beide operanden `DatumTijd`, invoer één milliseconde uit elkaar):
   consistentie `PLAYED 2`, regelvoorwaarde `PLAYED 1`, en op dezelfde
   invoer met omgekeerde verwachting `FAIL: verwacht consistent,
   werkelijk inconsistent` resp. `FAIL: kenmerk verjaard: verwacht
   afwezig, werkelijk aanwezig` — de discriminator, want een
   dag-lezing zou beide paren gelijk noemen. Alleen het gemengde paar
   wordt geskipt, en de bewaker die dat doet staat expliciet in de
   bron. Wat de bouwer voor "de backend kan het niet" aanzag was een
   **literal-pad-artefact**: `Literalen.parseDatumTijd` leest alleen
   `dd-mm-yyyy hh:mm:ss` en geeft op het canonieke ISO-lexeem `null`,
   waardoor een `DatumTijd`-literalprobe skipt om een heel andere reden.
   Dat gat is nu een eigen kaart (restpunt hierboven).
2. **"Deze arm defert op de onbekend-sentinel."** Onwaar op alle drie
   de plekken die het beweerden (proza, indexrij, codecommentaar).
   Hermeten, één sentinel-variant per arm: datum-ordening →
   `WF-036 FAIL …/rechts: date-ordering operand is unknown`; gelijkheid
   → `WF-036 FAIL …: equality across families: date-day vs unknown`;
   numerieke ordening → `WF-036 FAIL …/rechts: ordering operand is
   unknown` (steeds naast WF-034's `unknown-type sentinel inside a
   structured node`). Alléén `type: null` defert echt, en dan vangt
   WF-034 het (`value node without a static type`). De asymmetrie met
   WF-038's fold-arm — die wél expliciet defert — is bewust: daar
   bereikt alleen de rij-1-wrat de arm.

Twee meetresultaten die de tekst nu draagt en die er nog niet stonden:

- **Clingo weigert op vormniveau.** `decimals_of_datatype` geeft
  `DatumTijd` geen schaal, dus dezelfde `TRANSLATION FAULT: unscalable
  datatype in the ASP translation` komt óók op de SCHONE gelijk-precieze
  ordening — het is geen oordeel over dit defect. Dat het datatype zelf
  wél doorkomt, laat `corpus/golden/jutterswet-hoogwaterpaar` zien (een
  `DatumTijd`-ordeningsfold: `clingo: PASS`, drie cases). Het gat zit in
  het schaalpad van de vergelijking. `ref` is daarmee het enige been dat
  het defect zélf ontmoet: het speelt de gelijk-precieze ordening
  (`oracle: PASS`) en faalt alleen op het gemengde paar.
- **De toegelaten helft heeft geen getuige.** Alle zeven
  datum-ordeningen in golden+rood zijn dag×dag; een mutant die de
  `DatumTijd`×`DatumTijd`-helft óók weigert beweegt **nul van 504**
  rapporten (mutant leeft: hij vuurt op de probe). Het Percentage-
  patroon van WF-038, gespiegeld — en genoteerd op alle drie de plekken.
