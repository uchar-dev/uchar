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
