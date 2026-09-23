# temporele-datumvouw: de §7.1-tijdlijnvouw telt dagnummers op waar ref een typefout werpt

Goal: dezelfde regel als de setvouw-kaart schond, één arm verder. De
§7.1 EIGEN-SUBJECT `TOTAAL` over een TIJDLIJN waarvan de waarden datums
zijn is verifier-SCHOON, `ref` werpt een typefout, en de ASP-backend
**antwoordt het dagnummer**. De kaart datum-setbron-breuk (2026-09-20)
sloot de SETBRON-arm en raakte deze niet: die loopt door de
tijdlijn-arm van `translate.ml`, niet door de setarm.

## Gemeten (2026-09-20, kaart datum-setbron-breuk, adviesronde)

Document: `corpus/golden/jutterswet-tempototaal` teruggebracht tot zijn
derde regel — de eigen-subject `TOTAAL` over `bewaarvergoeding per
maand` (granulariteit MAAND) — met dat attribuut én het doel hertypeerd
naar `Datum in dagen`. Casus: de asOfDate-emmerconventie, J1 met
`bewaarvergoeding per maand` = 2024-01-15 (dagnummer 19737),
`asOfDate` 2026-03-15.

| uitvoerder | uitkomst |
|---|---|
| verifier | **PASS** |
| `ref` | FOUT `maal: expected a number, got 2024-01-15` |
| ASP-backend | **antwoordt 19737** — het dagnummer |
| Drools exec | `EXCLUDE non-numeric timeline value on 'bewaarvergoeding per maand' is not carried by the v0 backend — de gegenereerde Segment draagt een BigDecimal waarde` |
| Drools `--drl` | `EXCLUDE drl compile: … Unable to Analyse Expression if (rsTijdTotaal($subject.getBewaarv…` |

Waarom clingo een getal geeft: de tijdlijnvouw vermenigvuldigt elke
segmentwaarde met haar aantal hele roostereenheden, en bij een
MAAND-granulariteit met één segment is die factor 1 — dus het dagnummer
komt onveranderd naar buiten en ziet eruit als een antwoord. `ref` raakt
dezelfde vermenigvuldiging en faalt erop (`maal`), precies zoals zij op
de setbron `plus` faalt.

Ook hier is er geen producentroute: de frontend typeert een
datum-getypeerde tijdlijn onder `totaal van` weg, net als bij de
setbron (`totaal van vereist numerieke operanden`).

## Acceptatie (bij oppakken aanscherpen)

- (a) De tabel hierboven hermeten op de kop van de kaart, per uitvoerder.
- (b) Geen stellig getal meer waar de arbiter faalt: de ASP-tijdlijnarm
  weigert een niet-numerieke segmentwaarde met een benoemde,
  fase-dragende zin die het construct noemt — geen bodemzin uit gate 8's
  lijst — rood-eerst gemeten, met mutatieproef.
- (c) Een RODE getuige pint beide OCaml-weigeringen; de Drools-uitkomst
  staat met reden in de uitsluitingslijst van gate 8 (de exec-tak weigert
  daar al bij naam, de DRL-tak valt nog op compile — die tweede is
  dezelfde klasse als wat datum-setbron-breuk in de setarm dichtte en kan
  dezelfde gedeelde redenfunctie gebruiken).
- (d) Nagaan of de zusterarmen dezelfde gaten hebben: de
  §7.1-`TOTAAL` over een NAVIGERENDE tijdlijn, en de gedurende-clip.
- (e) Alle veertien poorten groen.

May touch: `backends/clingo/**`, `backends/drools/**` (de DRL-tak),
`corpus/rood/**`, `scripts/check-rood.sh` (telling/lijst, voorstel),
`backlog/`. Out of scope: `ref/` (arbiter — zij faalt al correct),
`frontends/**`.
NB: serieel met andere `translate.ml`-kaarten.

## Aangescherpt bij oppakken (2026-09-23, basis main b91df009)

- (a) Hermeten op het meetdocument (tempototaal teruggebracht tot de
  derde regel, attribuut + doel `Datum in dagen`, J1 = 2024-01-15,
  asOfDate 2026-03-15), per uitvoerder; de gemeten stand regeert.
- (b) De ASP-weigering vraagt het aan de DECLARATIE (§2.10) en noemt
  soort, tijdlijn en objecttype; rood-eerst op gate 8, mutatieproef met
  `cmp`-herstel.
- (c) Rode getuige(n) in `corpus/rood/`, `expectedFault {ref, clingo}`;
  Drools met reden in gate 8's uitsluitingslijst (voorstel). De DRL-tak:
  eerst meten of de vorm hem nog bereikt; zo ja een per-vorm-MARKER via
  `kern.Vouwwaarde`, met een NIEUWE JUnit-getuige (bestaande tests
  ongemoeid) en mutatieproef.
- (d) Zusterarmen meten: navigerende tijdlijn (eigen en set), §7.1
  per-element setvouw, gedurende-clip. Dicht wat in `translate.ml` /
  `tijdreeksAggregatie` dezelfde klasse is; kaart de rest.
- (e) Veertien gates los groen; gate-12-pins 180/159 bewegen niet.
