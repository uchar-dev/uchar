# De caselezer-melding bij `kenmerkGranulariteit` met waarde null

Goal: de kenmerk-arm van de gedeelde caselezer geeft, op een handgebouwd
document dat een kenmerk temporeel verklaart zonder raster, een melding die dat
document tegenspreekt. Deze kaart splitst die arm zoals de attribuut-arm van
`20260909-attribuutgranulariteit-null` (geland 2026-09-10, WF-059 derde arm)
gesplitst is, zodat de lezer de werkelijke reden noemt. Zusterkaart van die
kaart, expliciet afgesplitst omdat `kenmerkGranulariteit` daar Out of scope
stond.

## Het gemeten probleem

`ref/lib/case.ml`, de `characteristicTimelines`-arm (±r. 371-379), vouwt twee
verschillende situaties samen in één catch-all:

    | Some (_, Some g) -> g
    | _ -> fail "%s.%s: a characteristic timeline on a non-temporal characteristic" label cname

`Some (_, None)` — de sleutel STAAT er, de waarde is `null`, dus het document
verklaart het kenmerk wél temporeel — valt daardoor samen met `None`, waar het
kenmerk werkelijk niet-temporeel is. Precies dezelfde vouw die de attribuut-arm
had voordat WF-059's derde arm hem opende.

Gemeten 2026-09-10 op `corpus/negatief/wf059-kenmerk-zonder-granulariteit/ir.json`
met `corpus/golden/jutterswet-tegenwacht/case1-overlap.json`, in beide
uitvoerders identiek (de ASP-backend deelt de lezer):

    CASE ERROR: J1.aangesteld: a characteristic timeline on a non-temporal characteristic

Het document verklaart `aangesteld` temporeel; de lader zegt van niet.

**Bereikbaarheid.** Sinds 2026-09-09 (WF-059 tweede arm) laat het schema geen
`null` meer toe in de `kenmerkGranulariteit`-enum en weigert de verifier de vorm,
dus de wire draagt hem niet meer. Alleen een handgebouwd document bereikt deze
arm — en dan moet hij de juiste reden noemen. Dat is dezelfde stand als op de
attribuutkant vóór de derde arm, en daar was het genoeg reden om de melding te
repareren.

**Klasse.** Diagnose-hygiëne, geen correctheidsgat: het pad sterft luid, alleen
met de verkeerde reden. Lage prioriteit, net als de zusterkaart.

## De aanpak

- De arm splitsen zoals de attribuut-arm: `Some (_, None)` krijgt een eigen tak
  met een melding in dezelfde stijl (het kenmerk IS temporeel verklaard, maar
  zonder raster; regelverwijzing `(WF-059)` erin), en de bestaande tekst blijft
  staan voor `None`, waar hij waar is.
- Additieve pin-test in `backends/clingo/test/test_clingo.ml`, in de huisvorm
  van het `attribuutlevering`-blok: needle op de **volle** zin, plus een
  over-afwijzingsklem (echte granulariteit laadt schoon) en een derde check die
  de oude melding op de `None`-tak vasthoudt. Beide mutantrichtingen moeten
  falsifieerbaar zijn — armen terugvouwen laat de ene check omvallen, de nieuwe
  melding ook op `None` de andere.
- `spec/IR-SPEC.md` (voorstel): als de melding daar geciteerd wordt, de
  citaat-plus-halve-zin-vorm aanhouden ("pinned additively in the backend's own
  suite"), en de WF-059-clausule zegt dan niet langer dat de meldingskant van de
  tweede arm openstaat.
- `corpus/negatief/wf059-kenmerk-zonder-granulariteit/NOTE.md`: de alinea die nu
  zegt dat de twee armen matchen op de WF-sluiting maar niet op de meldingstekst
  bijwerken zodra dat wél zo is.

Acceptance:
- De nieuwe melding letterlijk in de Result, met vóór/ná-output uit **beide**
  uitvoerders op hetzelfde handgebouwde document.
- Rood eerst: de pin-test faalt aantoonbaar vóór de armsplitsing.
- Beide mutantrichtingen gemeten en genoteerd.
- Gate 6 groen met een gegroeide check-telling (vloer 83 blijft staan); gates 1,
  3, 5 groen; alle dertien groen bij het landen.
- Nul beweging in de corpusoordelen: geen enkel golden of negatief verandert van
  verdict of melding (de arm raakt alleen documenten met sleutel-aanwezig en
  waarde-null, en die zijn er 1 — de getuige van de tweede arm).

May touch: `ref/**`, `backends/clingo/**`, `spec/IR-SPEC.md` (**voorstel** —
beschermd pad), `corpus/negatief/**`, `backlog/`.

## Out of scope

- `attribuutGranulariteit`: de melding daar is 2026-09-10 al gerepareerd
  (WF-059 derde arm).
- De WF-sluiting van `kenmerkGranulariteit` zelf: 2026-09-09 geland (tweede
  arm), schema én verifier. Deze kaart raakt alleen de meldingstekst.
- De scalaire-levering-divergentie tussen `ref` en de ASP-backend (twee
  diagnoses van één ontbrekende tijdlijn): eigen vraag, eigen kaart.

## Result (2026-09-11)

Branch `card/kenmerk-null-caselezer-melding`, vanaf `3ae57e2`. De branch blijft
**voorstel**: hij raakt `spec/IR-SPEC.md`, een beschermd pad.

Gewijzigd: `ref/lib/case.ml` (de armsplitsing), `backends/clingo/test/test_clingo.ml`
(drie additieve checks), `spec/IR-SPEC.md` (WF-059 tweede arm, plus de regelrij),
`corpus/negatief/wf059-kenmerk-zonder-granulariteit/NOTE.md` (de asymmetrie-alinea).
`backends/drools/**` en `corpus/golden/**` zijn niet aangeraakt — parallelle
sporen.

### Rood eerst, beide uitvoerders

Het meetdocument is het handgebouwde negatief-document met de golden-case die de
tijdlijn wél levert:

    $ ./_build/default/ref/bin/runner.exe \
        corpus/negatief/wf059-kenmerk-zonder-granulariteit/ir.json \
        corpus/golden/jutterswet-tegenwacht/case1-overlap.json
    CASE ERROR: J1.aangesteld: a characteristic timeline on a non-temporal characteristic
    (exit 2)

    $ ./_build/default/backends/clingo/bin/clingo_runner.exe   … dezelfde twee bestanden
    CASE ERROR: J1.aangesteld: a characteristic timeline on a non-temporal characteristic
    (exit 2)

Woordelijk identiek — de ASP-backend laadt via `Uchar_ref.Case.load_file`. Het
document verklaart `aangesteld` temporeel; de lezer sprak het tegen.

De pin-test faalde vóór de splitsing, met de oude tekst in de foutregel:

    FAIL een tijdlijn op een kenmerk met granulariteit null refused at load
      (W1.dienstdoend: a characteristic timeline on a non-temporal characteristic)
    test_clingo: 96 checks, 1 FAILURES

Checks 2 en 3 stonden op dat moment al groen: de armscheiding was het enige dat
ontbrak.

### De armsplitsing

`ref/lib/case.ml`, de `characteristicTimelines`-arm. `Some (_, Some g)` blijft de
gewone tak; de catch-all `_` is uit elkaar gehaald in `Some (_, None)` — sleutel
aanwezig, waarde null — en `None`. Precies de vorm die de attribuut-arm sinds
2026-09-10 heeft, met de reden in het commentaar (welke lezers de SLEUTEL lezen,
en dat alleen een handgebouwd document hier nog komt sinds de enum dicht is).

### Groen, beide uitvoerders, hetzelfde document

    CASE ERROR: J1.aangesteld: the characteristic is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)
    (ref, exit 2)

    CASE ERROR: J1.aangesteld: the characteristic is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)
    (clingo_runner, exit 2)

De oude tekst blijft staan voor de tak waar hij waar is — een kenmerk zonder
granulariteitssleutel, als tijdlijn geleverd. Die tak heeft geen corpusgetuige
(zo'n golden zou bij het laden sterven) en leeft dus in check 3 van de pin.

### De pin (gate 6), met mutantproef in beide richtingen

Drie checks in `backends/clingo/test/test_clingo.ml`, in de huisvorm van het
`attribuutlevering`-blok en met dezelfde helper (`laadt_ir_case`): document in
het geheugen → tempfile → `Ir_v0.Loader.load_file` + `Uchar_ref.Case.load_file`
→ `contains`. De needle van check 1 is de **volle** zin, `(WF-059)` inbegrepen,
omdat `spec/IR-SPEC.md` hem woordelijk citeert.

1. `"dienstdoend": null` → de volle nieuwe zin;
2. `"dienstdoend": "MAAND"` → laadt schoon (over-afwijzingsklem);
3. granulariteitstabel leeg → de oude zin (de armscheiding).

Mutantproef, één richting tegelijk, telkens teruggedraaid:

- armen terugvouwen naar de oude catch-all (beide takken de oude melding):
  `FAIL een tijdlijn op een kenmerk met granulariteit null … (W1.dienstdoend: a
  characteristic timeline on a non-temporal characteristic)` — check 1 valt om,
  check 3 overleeft, want die catch-all is precies wat check 3 verwacht;
- de nieuwe melding óók op de `None`-arm: `FAIL een tijdlijn op een kenmerk
  zonder granulariteitssleutel … (W1.dienstdoend: the characteristic is declared
  time-dependent but its granularity is null: the timeline has no grid to sit on
  (WF-059))` — check 3 valt om, check 1 overleeft.

Geen van beide checks ziet beide richtingen; samen pinnen ze de armgrens.

### De IR-SPEC-passage

`spec/IR-SPEC.md` citeert de kenmerk-melding op twee plekken, beide binnen
WF-059's **tweede** arm en beide in de meting van 2026-09-09 (de derde arm zegt
er niets over — de asymmetrie stond in `CONSTRUCTS.md` rij 1 en in de
getuige-NOTE). Beide bijgewerkt naar de nieuwe stand:

- de tweede-arm-clausule krijgt een alinea na "Closed twice on purpose": de
  melding volgde op 2026-09-11, met het citaat van de volle nieuwe zin en de
  halve zin "that whole sentence pinned additively in the backend's own suite",
  plus dat de oude tekst blijft staan voor de sleutel-afwezig-tak en dat de twee
  zijtabellen nu ook op de melding overeenkomen;
- de WF-059-rij in de regeltabel krijgt dezelfde mededeling als één bijzin
  achter "…never met the schema", zoals arm (3) in die rij zijn eigen
  meldingscorrectie al noemt.

De meetzin zelf (past tense, 2026-09-09) blijft staan: hij beschrijft wat er op
die datum gemeten is.

### De getuige-NOTE

`corpus/negatief/wf059-kenmerk-zonder-granulariteit/NOTE.md`, de slotalinea, zei
"de twee armen matchen op de WF-sluiting en NOT yet op de melding" en verwees
naar deze kaart. Die alinea draagt nu de nieuwe melding letterlijk, de datum, en
de reden waarom de pin in de suite van de ASP-backend zit en niet in een
corpuszone. De zuster-NOTE
(`corpus/negatief/wf059-attribuut-zonder-granulariteit/NOTE.md`) noemt de
asymmetrie niet — die noemt alleen het klasseverschil silent-fout tegen
luid-fout — en blijft dus ongewijzigd kloppen.

### Poorten

Gedraaid op deze branchkop, alle groen:

    check-self-contained: PASS
    check-ir-schema:      PASS (schema valid; 194 file(s) validate and round-trip, floor 192)
    check-verifier:       PASS (194 golden clean; 362 negatief rejected for their declared rule, floor 350)
    check-case-schema:    PASS (schema valid; 459 case file(s) validate)
    check-oracle:         PASS (ref suite 515 worked examples, floor 489; 433 cases replayed exactly, floor 427)
    check-clingo:         PASS (backend suite 96 checks, floor 83; 433 cases replay exactly through clingo)
    check-noop:           PASS (397 cases refute a do-nothing executor; 36 pinned survivors)
    check-rood:           PASS (26 red cases fault observably in ref and clingo, floor 26)

De suite van de ASP-backend gaat van 93 naar 96 checks; vloer 83 blijft staan —
een vloer is een ondergrens, groei vraagt geen bijstelling, dus het beschermde
script beweegt niet. Nul beweging in de corpusoordelen: gates 3, 5, 6, 7 en 8
draaien met ongewijzigde tellingen, en het enige document dat de arm raakt is de
getuige van de tweede arm, die bij gate 3 nog steeds door de verifier wordt
afgewezen op zijn eigen regel.

Niet gedraaid: 9 (catala), 10/11 (RegelSpraak-frontend, pariteitszone), 12 en 13
(Drools) — die bouwen `backends/drools`, waar een zusterspoor werkt. Deze kaart
raakt geen frontend, geen schema en geen corpusdocument; de gedeelde caselezer
zit in `ref/lib`, die gates 12 en 13 niet langs deze arm passeren (een
`kenmerkGranulariteit`-null haalt de wire niet).

### Wat bewust niet bewoog

- **De sleutel-afwezig-melding.** "a characteristic timeline on a non-temporal
  characteristic" blijft woordelijk staan; hij is daar waar, en check 3 houdt
  hem vast.
- **De WF-sluiting.** Schema-enum en verifier zijn sinds 2026-09-09 dicht; deze
  kaart raakt alleen de meldingstekst.
- **De scalaire-levering-divergentie** tussen `ref` en de ASP-backend (twee
  diagnoses van één ontbrekende tijdlijn): stond als Out of scope in de kaart en
  is niet aangeraakt.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`**: niet aangeraakt, conform
  WAY-OF-WORKING §4. Voorstel hieronder.
- **Het commentaar bij de statische-levering-arm** (`case.ml` r. ±327) dat naar
  "de zusterarm hieronder" verwijst: blijft kloppen — die zusterarm is nu de
  `None`-tak en weigert nog steeds precies de spiegelfout.

### Voorgestelde zin voor `spec/CONSTRUCTS.md` rij 1 (hoofdsessie landt serieel)

In de bestaande **Landed (2026-09-10)**-clausule van WF-059's derde arm staat nu
de laatste zin:

> The sister's MESSAGE side is not part of this arm and is CARDED: a hand-built
> `kenmerkGranulariteit` null still draws "a characteristic timeline on a
> non-temporal characteristic", the contradicting wording this arm repaired on
> the attribute side, and splitting that arm the same way is
> `backlog/new/20260910-kenmerk-null-caselezer-melding.md`.

Voorstel: vervang die ene zin door

> The sister's MESSAGE side followed one card later (**2026-09-11**): the same
> split on the shared loader's `characteristicTimelines` arm, so a hand-built
> `kenmerkGranulariteit` null now draws "the characteristic is declared
> time-dependent but its granularity is null: the timeline has no grid to sit on
> (WF-059)" in both executors instead of the contradicting "a characteristic
> timeline on a non-temporal characteristic", which stays for the key-absent
> case — that whole sentence, its arm boundary and an over-rejection guard
> pinned by three more checks in the clingo backend's own suite (93 → 96), with
> no corpus verdict moving.
