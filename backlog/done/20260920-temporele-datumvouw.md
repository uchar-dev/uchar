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

---

## Result (2026-09-23)

**Het gat stond open zoals de kaart het tekende, en het stond in TWEE
armen van `translate.ml`, niet één.** De eigen-subject-TOTAAL en de
gedurende-clip vermenigvuldigen allebei elke segmentwaarde met haar hele
roostereenheden en gaven allebei het dagnummer als antwoord. Beide
weigeren nu bij naam; de DRL-tak valt niet meer op compile maar markeert
per vorm. `ref`, `frontends/`, `spec/` onaangeraakt.

### 1. Hermeting (main b91df009 + claim)

Meetdocument: `jutterswet-tempototaal` teruggebracht tot de derde regel,
`bewaarvergoeding per maand` (MAAND) en `bewaargeld` naar `Datum in
dagen`; J1 = 2024-01-15 als asOfDate-emmerscalar, asOfDate 2026-03-15.
Dit document is nu `corpus/rood/datumtijdlijn-totaalvouw` (zonder het
ongebruikte `Vondst`/`bewaring`).

| uitvoerder | vóór | ná |
|---|---|---|
| verifier | PASS | PASS (ongewijzigd) |
| `ref` | FOUT `maal: expected a number, got 2024-01-15` | idem (arbiter) |
| clingo | **`J1.bewaargeld = [19737]`** | TRANSLATION FAULT `the TOTAAL over the timeline of "bewaarvergoeding per maand" on "Jutter" multiplies DATE-valued timeline segments … Declined in translation` |
| Drools exec | EXCLUDE `non-numeric timeline value on 'bewaarvergoeding per maand' is not carried by the v0 backend — the generated Segment holds a BigDecimal waarde` | idem (niet aangeraakt) |
| Drools `--drl` | EXCLUDE `drl compile: … Unable to Analyse Expression if (rsTijdTotaal($subject.getBewaarvergoedingPerMaand(), "MAAND") != null && …` | EXCLUDE `drl-transpiler MARKER: … TODO vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand' van Jutter: de segmentwaarden zijn van type Datum in dagen en niet getalachtig …` |

De kaartstand van 2026-09-20 is exact gereproduceerd. De vorm bereikt
de DRL-tak dus nog steeds.

Extra meting: een casus ZONDER waarde voor het attribuut — `ref` leeg,
clingo (vóór) leeg, exec PLAYED 1 (leeg), DRL compilefout.

### 2. Zusterarmen, gemeten

| vorm | verifier | `ref` | clingo vóór | uitkomst |
|---|---|---|---|---|
| gedurende-clip (erfwacht, datumgetypeerd, 1 maand geclipt) | PASS | `maal: expected a number, got 2024-01-15` | **`K1.wachtsom = [19737]`** | ZELFDE gat, gedicht (eigen arm r. ~2760, zelfde helper) |
| §7.1 per-element SET-vouw over een temporele datum | PASS | fout (emmerscalar; met volle tijdlijn `maal`) | weigert al: de setbron-DATE-zin van datum-setbron-breuk, want die guard staat vóór de temporele splitsing | geen gat |
| NAVIGERENDE eigen tijdlijn (`navigatieRol` naar de bewarende jutter) | **FAIL** WF-083 (2×) | `unsupported instance-fold source shape` | weigert al bij naam (`the temporal total of the NAVIGATED …`) | geen gat, verifier wijst af |
| NAVIGERENDE set (bronPad/navigatie op het argument) | — | — | de setarm-datumguard staat vóór `source_join`; argument-navigatie valt in de benoemde `NAVIGATED per-element`-arm | geen gat (codelezing, niet apart gemeten) |
| datum-tijd / tekst tijdlijn onder TOTAAL | PASS | `maal: expected a number` | weigert al, met BODEMZIN (`dt_term` r. 167 / `symbolic_term` r. 162 van de 36-lijst) | geen fout getal; bodemzin — zie §7 |
| datumtijdlijn naar NUMERIEK doel | FAIL WF-091 | `maal` | gaf 19737, weigert nu ook (de guard vraagt de declaratie, niet het doel) | tweede verdedigingslinie |
| ch. 3.7 PRO-RATA over een DATUMbasis (`translate_pro_rata`; lensvondst B1, fixronde) | PASS | `pro-rata base is not numeric: 2024-01-18` | **`borgdeel` = 19740.00** over een hele maand (19737.00 bij 2024-01-15); over een deelmaand datumafhankelijk: de exactheidsguard vuurt alleen als het dagnummer niet deelbaar uitkomt | ZELFDE klasse, gedicht (fixronde, zie §11) |

In de DRL-tak dekt de ene guard in `tijdreeksAggregatie` eigen, to-one
genavigeerd en geclipt tegelijk (ze delen die functie); de set-vorm gaat
daar al vóór uit (`a.set()`) naar de accumulate-arm, die
`Vouwwaarde.weigering` al vraagt.

### 3. De ASP-weigering, woordelijk

`backends/clingo/lib/translate.ml`, nieuwe helper
`refuse_date_timeline_total`, gevraagd aan de DECLARATIE, vóór enige
emissie en vóór de casusgebonden checks, in beide armen:

> the TOTAAL over the timeline of "bewaarvergoeding per maand" on
> "Jutter" multiplies DATE-valued timeline segments by their whole grid
> units: a temporal total is arithmetic, a date is a calendar POSITION,
> and scaling one has no meaning — this translation would multiply its
> day number and answer a definite number no calendar reads back.
> Declined in translation

In de clip-arm met kop `the gedurende-clipped TOTAAL over the timeline`.
Noemt soort, tijdlijn en type; geen bodemzin (gate 8: 86 fragmenten, geen
enkele cites een van de 36). Match op `Datum_in_dagen` EXACT, zoals de
setarm: datum-tijd/tekst/enum/boolean bereiken deze armen niet (gemeten,
tabel §2), en `decimals_of_datatype` geeft andere niet-numerieke typen
geen schaal.

**Asymmetrie, per arm verschillend (gecorrigeerd in de fixronde):** de
weigering hangt aan het document, dus ook een casus zonder waarde wordt
geweigerd. In de EIGEN-SUBJECT-arm antwoordt `ref` daar leeg — de
onschadelijke richting, zoals in de setarm. In de KLIP-arm NIET: `ref`
antwoordt `K1.wachtsom = 0` zowel zonder waardetijdlijn als met een
weggeklipte waarde (gemeten 2026-09-23) — een betekenisloos getal in
een datumslot; clingo antwoordde vóór deze kaart dezelfde lege som
([0] = 1970-01-01), en de Drools exec-tak CRASHT op de casus zonder
waarde (`ClassCastException: Cannot cast java.math.BigDecimal to
java.time.LocalDate`). Daar is de arbiterclaim dus onwaar. Het is een
eigen kaart, `20260923-ref-klipvouw-nul` (de hoofdsessie maakt hem
aan); beide NOTEs zeggen het.

**De DRL-match is breder dan de ASP-match, bewust.** `tijdlijnWeigering`
weigert elk niet-getalachtig type, de ASP-arm exact `Datum_in_dagen`.
Een datum-tijd- of teksttijdlijn krijgt in de DRL-tak dus de benoemde
marker en in clingo een bodemzin (`dt_term`/`symbolic_term`, die hem
eerder vangen). Onschadelijk — beide weigeren — en vastgelegd.

### 4. De Drools-weigering, woordelijk

`kern/Vouwwaarde.tijdlijnWeigering` (nieuw, naast `weigering`), gevraagd
in `DrlTranspiler.tijdreeksAggregatie` direct na de granulariteitscheck,
als `Frag.todo` (per-vorm-MARKER):

> vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand' van Jutter:
> de segmentwaarden zijn van type Datum in dagen en niet getalachtig,
> terwijl de tijdlijnvouw elke segmentwaarde met haar aantal
> roosterperioden vermenigvuldigt. Vermenigvuldigen is rekenkunde en
> alleen bedragen schalen; ref werpt op de vermenigvuldiging zelf een
> typefout. Geen enkele uitvoerder draagt deze vorm (§7.1/ch. 3.6)

Sluit op "niet getalachtig" zoals de setarm-helper. De exec-tak is NIET
aangeraakt: zijn caselader weigert de datumsegmentwaarde al bij naam vóór
fire, en op een lege casus speelt hij leeg zoals `ref`. Daardoor weigeren
de twee takken in verschillende fasen en woorden; `BeideTakken.geweigerd`
(één fragment voor beide) paste niet, dus de nieuwe getuige pint elke tak
apart.

### 5. Getuigen

- `corpus/rood/datumtijdlijn-totaalvouw/` en
  `corpus/rood/datumtijdlijn-klipvouw/` — `expectedFault.ref` =
  `maal: expected a number, got 2024-01-15`, `expectedFault.clingo` = de
  volle zin per arm. Klip: segment 2023-07-01..2023-09-01, kenmerk
  2023-07-01..2023-08-01 → precies één geclipte maand, dus vóór de fix
  19737.
- `backends/drools/src/test/.../cases/TijdlijnDatumvouwWitnessTest.java`
  (NIEUW bestand, bestaande tests ongemoeid), 4 rijen: eigen-subject en
  klip (DRL-marker + exec-lader, elk apart), de DRL-marker op een lege
  casus (exec speelt leeg, DRL weigert), smalheid (`jutterswet-tempototaal`
  en `jutterswet-erfwacht` spelen in beide takken).
- Geen nieuwe checks in `backends/clingo/test/test_clingo.ml`: dat is een
  bestaand testbestand zonder mandaat; de rode getuigen dragen de pin.

### 6. Rood eerst en mutatieproeven, letterlijk

Rood eerst (gate 8 op de nieuwe getuigen, vóór de fix):

```
FAIL: corpus/rood/datumtijdlijn-klipvouw/case1.json — clingo did not observe the expected fault:
EXPECTED FAULT DID NOT OCCUR: clingo found a model
FAIL: corpus/rood/datumtijdlijn-totaalvouw/case1.json — clingo did not observe the expected fault:
EXPECTED FAULT DID NOT OCCUR: clingo found a model
FAIL: datumtijdlijn-klipvouw carries no drools fragment and is not in the Drools exclusion list …
FAIL: datumtijdlijn-totaalvouw carries no drools fragment and is not in the Drools exclusion list …
```

Mutatie ASP (`| Some (Some Datum_in_dagen) when false ->`, herbouwd):

```
== datumtijdlijn-totaalvouw
EXPECTED FAULT DID NOT OCCUR: clingo found a model
== datumtijdlijn-klipvouw
EXPECTED FAULT DID NOT OCCUR: clingo found a model
RESTORED            (cp + cmp tegen de backup)
== datumtijdlijn-totaalvouw
clingo: expected fault observed (the TOTAAL over the timeline of …)
== datumtijdlijn-klipvouw
clingo: expected fault observed (the gedurende-clipped TOTAAL over the timeline of …)
```

Mutatie Drools (`if (true) return null;` bovenin `tijdlijnWeigering`,
`mvn -o test -Dtest=TijdlijnDatumvouwWitnessTest`):

```
Tests run: 4, Failures: 3, Errors: 0, Skipped: 0 <<< FAILURE!
  de_eigen_tijdlijntotaal_over_datums_weigert_bij_naam  FAILURE
  de_drl_marker_hangt_aan_de_declaratie_en_niet_aan_de_casus  FAILURE
  de_gedurende_klip_over_datums_weigert_bij_naam  FAILURE
  (onder elk: Unable to Analyse Expression if (rsTijdTotaal(…)
RESTORED            (cp + cmp)
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

De smalheidsrij bleef groen onder de mutant, zoals hij hoort.

### 7. De veertien poorten, elk los na `eval $(opam env)`

| # | poort | rc | kerngetallen |
|---|---|---|---|
| 1 | self-contained | 0 | PASS |
| 2 | ir-schema | 0 | 276 bestanden, vloer 192 |
| 3 | verifier | 0 | 276 golden schoon; 431 negatief, vloer 350 |
| 4 | case-schema | 0 | 634 casusbestanden |
| 5 | oracle | 0 | ref-suite 780 (vloer 489); 591 casussen exact (vloer 427) |
| 6 | clingo | 0 | suite 480 (vloer 83); 591 casussen exact |
| 7 | noop | 0 | 555 casussen, 36 overlevers |
| 8 | rood | 0 | **43** rode casussen (was 41), vloer 26; **86** fragmenten (was 82), geen bodemzin; Drools 9 fault-pinned / **34** met reden (was 32) |

*Na de fixronde (§11) opnieuw gedraaid: gate 8 **44** rode casussen, **88** fragmenten, Drools 9 / **35** met reden; gate 2 277 bestanden, gate 3 277 schoon / 431 negatief, gate 4 635 casusbestanden, gate 6 suite 480 / 591 casussen, gate 13 groen; gate 1 groen. `backends/drools` niet geraakt in de fixronde, dus gate 12 niet opnieuw.*
| 9 | catala | 0 | 201 checks; 12 goldens |
| 10 | regelspraak | 0 | 393 tests; 70 rgs-goldens |
| 11 | toka-parity | 0 | ref 29/29, clingo 29/29, Drools 29 PLAYED |
| 12 | drools | 0 | played **180** = pin; DRL played **159** = pin; kanaalvloer 53, DRL-vloer 74; JUnit **604** (incl. de 4 nieuwe), vloer 239 |
| 13 | reftype-derivation | 0 | 131 twists / 27 goldens; 128 replays, 25 DRL-gespeeld |
| 14 | flogic | 0 | 130 checks; 2 goldens |

Gate-12-pins bewogen niet (de vorm heeft geen golden). Geen telvloer
aangeraakt.

### 8. Beschermd-padvoorstellen

- `scripts/check-rood.sh` (commit 773df583): twee regels in de
  Drools-uitsluitingslijst, alfabetisch na `datumsetbron-totaalvouw`:
  `datumtijdlijn-klipvouw|EXCLUDE …` en `datumtijdlijn-totaalvouw|EXCLUDE
  …`. Puur additief, niets herordend, geen bestaande reden herschreven.
  Telling Drools-been 32 → 34 met reden; in de fixronde een derde regel,
  `datumbasis-prorata|EXCLUDE …`, vóór `datumsetbron-somvouw` → 35.
- **Voor de vervolgkaart `20260923-ref-klipvouw-nul` (niet zelf
  uitgevoerd, beschermd pad `spec/verifier/` + `spec/IR-SPEC.md`):** een
  WF-regel "de operand van TOTAAL, SOM en het tijdsevenredig deel is
  numeriek". WF-091 vergelijkt nu alleen het aggregaattype met het
  operandtype en laat een geheel datumgetypeerde vouw dus door; met die
  regel worden alle drie de rode getuigen van deze kaart en de twee van
  datum-setbron-breuk verifier-afwijzingen, en verhuizen ze naar
  `corpus/negatief/`.

### 9. Registervoorstel `spec/CONSTRUCTS.md`, rij 1

Aan te sluiten na het blok "**Landed (2026-09-20, kaart
datum-setbron-breuk)**":

> **Landed (2026-09-23, card temporele-datumvouw):** the TIMELINE half of
> the same rule. A §7.1 temporal `TOTAAL` multiplies each segment value by
> the whole grid units it spans, so over a `Datum in dagen` timeline it
> has no reading: `ref` faults on the multiplication (`maal: expected a
> number`). The ASP backend answered the day number instead — 19737 for
> 2024-01-15 at a MONTH grid, where the factor is 1 — in BOTH temporal
> total arms, the own-subject total and the gedurende clip, under
> verifier-clean documents. Both arms now decline by name in translation,
> asked of the DECLARATION (so a case without a value is declined too
> where `ref` answers leeg in the own-subject arm; in the clip arm `ref`
> itself answers 0 on an empty or clipped-away case, carded separately as
> `20260923-ref-klipvouw-nul`).
> The DRL-text branch, which failed the whole file on compile, markers the
> form through `Vouwwaarde.tijdlijnWeigering`, the timeline half of the
> reason the set arm shares; the exec branch already refused the
> date-valued segment at case load. Measured and NOT a gap: the §7.1
> per-element set fold (the set arm's date guard fires first), and the
> navigated own-timeline total (verifier WF-083; clingo refuses it by
> name, `ref` with the bottom sentence "unsupported instance-fold source
> shape"). The same class sat in the ch. 3.7 pro-rata arm: a DATE base
> answered its day number as the part (19740.00 for 2024-01-18 over a
> whole month) where `ref` faults "pro-rata base is not numeric"; the
> ASP translation declines it by name too, witness
> `corpus/rood/datumbasis-prorata`; neither Drools branch lowers that
> temporal assignment at all. Witnesses `corpus/rood/datumtijdlijn-totaalvouw` and
> `corpus/rood/datumtijdlijn-klipvouw`. No producer route: the frontend
> types the form away. Gate 12 pins unchanged (180 / 159).

### 10. Open punten

- **Datum-tijd- en tekst-tijdlijn onder TOTAAL** weigeren in clingo met
  een bodemzin (`dt_term`, `symbolic_term`), zelfde bevinding als §9 van
  datum-setbron-breuk voor de setbron. Geen fout getal, dus niet het
  defect van deze kaart. Voorgestelde vervolgkaart (niet aangemaakt):
  *benoemde arm voor een niet-numerieke tijdlijn/setbron onder een
  optellende vouw in `symbolic_term` en `dt_term`, met rode getuige per
  arm* — één kaart voor beide helften.
- **De exec-tak weigert in een andere fase dan de DRL-tak.** Exec speelt
  een lege casus leeg (≡ `ref`), DRL weigert het document. Beide
  onschadelijk; wie de takken gelijk wil trekken, laat de exec-transpiler
  dezelfde `tijdlijnWeigering` vragen (verandert de exec-uitkomst alleen
  op niet-corpusdocumenten). Niet gedaan: de opdracht noemde alleen de
  DRL-tak.
- **"on \"Klipjutter\""** in de clip-zin noemt het objecttype van de
  lezing, niet het declarerende supertype `Jutter` — waar, maar een lezer
  kan het declarerende type verwachten.

### 11. Fixronde na de lens (2026-09-23)

**B1 — de pro-rata-arm, dezelfde klasse.** `translate_pro_rata` schaalt
de basis met ware dagen over emmerdagen en liet bij een DATUMbasis het
dagnummer als deel naar buiten. Meetdocument: `anchor/jutterswet-borgdeel`
teruggebracht tot de `borgdeel`-regel, `borgtarief` (JAAR) naar `Datum
in dagen`, doel numeriek; J1 met één waardesegment 2024-01-01..2025-01-01
en borgplichtig 2024-01-01..2024-02-01 (één hele maand).

| | verifier | `ref` | clingo |
|---|---|---|---|
| vóór, basis 2024-01-18 | PASS | `pro-rata base is not numeric: 2024-01-18` | `J1.borgdeel timeline = [19723,19754)=1974000` — **19740.00** |
| vóór, basis 2024-01-15 | PASS | idem met 2024-01-15 | `[19723,19754)=1973700` — **19737.00** |
| ná | PASS | ongewijzigd | TRANSLATION FAULT (zin hieronder) |

Drools: exec `transpiler SKIP … regel (temporele toekenning buiten
Phase-F scope)`, DRL `MARKER … TODO temporele toekenning` — beide
onafhankelijk van het basistype, niet aangeraakt.

Zin, gevraagd aan de DECLARATIE van de basis direct na de
temporaliteitscheck:

> the pro-rata part (tijdsevenredig deel) of the base "borgtarief" on
> "Jutter" scales a DATE-valued base by true days over bucket days: a
> pro-rata part is arithmetic, a date is a calendar POSITION, and
> scaling one has no meaning — this translation would scale its day
> number and answer a definite number no calendar reads back. Declined
> in translation

Derde rode getuige `corpus/rood/datumbasis-prorata` (de 19740-variant
over een hele maand, zodat hij het lek pint en niet de toevallige
exactheidsguard). Rood eerst (translate.ml op HEAD e2cc91be, herbouwd):

```
EXPECTED FAULT DID NOT OCCUR: clingo found a model
oracle: expected fault observed (pro-rata base is not numeric: 2024-01-18)
```

Mutatie (`| Some (Some Datum_in_dagen) when false ->` in de pro-rata-arm):

```
== mutant
EXPECTED FAULT DID NOT OCCUR: clingo found a model
MISMATCH: J1.borgdeel timeline = [19723,19754)=1974000, expected (empty)
RESTORED            (cp + cmp)
clingo: expected fault observed (the pro-rata part (tijdseve…
```

**B2 — de klip-arm: de arbiter antwoordt zelf een getal.** Gemeten op
`datumtijdlijn-klipvouw`: zonder waardetijdlijn `ref` `K1.wachtsom = 0`,
Drools exec `ERROR ConsequenceException … ClassCastException: Cannot
cast java.math.BigDecimal to java.time.LocalDate`, DRL de marker; met
weggeklipte waarde `ref` `0`, exec EXCLUDE (lader), DRL de marker.
clingo weigert beide. De onschadelijkheidsclaim in §3 en in de
registertekst gold alleen voor de eigen-arm en is gecorrigeerd; de
klip-NOTE heeft een eigen alinea. `ref` en de exec-crash zijn de kaart
`20260923-ref-klipvouw-nul` (hoofdsessie), met het WF-voorstel in §8.

**Advies 1** — de registertekst noemde de genavigeerde weigering in
beide OCaml-uitvoerders "by name"; `ref` weigert met de bodemzin
`unsupported instance-fold source shape`. Gecorrigeerd.

**Advies 3** — de DRL-match is breder dan de ASP-match; vastgelegd in §3.
