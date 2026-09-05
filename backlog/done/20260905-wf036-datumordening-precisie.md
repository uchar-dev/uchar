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
    ref     : EVALUATION FAULT: unordered comparison: 2025-03-05 vs @1740816000000ms
    clingo  : TRANSLATION FAULT: unscalable datatype in the ASP translation

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
2. **Drools werpt geen ClassCastException.** De kaart verwachtte de
   fold-zusters `LocalDate`→`ChronoLocalDate`-cast. Gemeten: de backend
   komt er niet aan toe. Hij weigert de vorm per document, vóór het
   vuren. Apart nagemeten, zodat de weigering niet voor dekking
   doorgaat: dezelfde fout in een regelvoorwaarde (een handgebroken
   `corpus/golden/jutterswet-vondstverjaring`) levert `EXCLUDE
   transpiler SKIP: regel (voorwaarde buiten Phase-B scope)`, en de
   gelijk-precieze `DatumTijd`-controle óók — de backend draagt
   überhaupt geen millisecondeprecieze datumvergelijking. De drieslag
   blijft daarmee intact in zijn kern: geen enkele uitvoerder
   beantwoordt dit document.
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
- De frontend-kant van afwijking 3 hierboven: `Typering.vergelijk` laat
  de kruising nog toe. Kandidaat voor een eigen kaart.
