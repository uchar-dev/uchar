# clingo-refusal-channel: gate 6 krijgt een weigerkanaal, en de verzuimrente-ankers worden exact

*Opgesteld door de hoofdsessie op 2026-11-03 uit de architectuurreview van Fable (`werk/review-fable-2026-11-03/advies.md`, §1 punt 6, §3
"Clingo", advies 2). Besluit van de eigenaar 2026-11-03: **gate 6 krijgt een weigerkanaal**.*

## Aanleiding

Gate 6 eist dat elke golden in clingo speelt; er is geen kanaal voor een vorm die de ASP-codering niet draagt. Daardoor vormde clingo het
corpus: de ankers `corpus/golden/anchor/jutterswet-verzuimrente-dagen` en `-30-360` ronden af op de cent (139,18 €, 73,77 €, 141,67 €, 72,22 €)
in plaats van de exacte waarden 139 13/73, 73 47/61, 141 2/3 en 72 2/9 €, omdat clingo geen niet-decimale waarde houdt, en het ene anker
kreeg een geheel afdrachtbedrag om onder de factorgrens 46340 te blijven. CLAUDE.md zegt dat de ankers nooit door een backend gevormd worden.
Clingo is het model-theoretische anker voor fixpunt, negatie, defaults en kringen; voor rekenkunde levert het weinig tweede mening (de
kalender rekent het met de code van ref, `calendar_weights`).

## Goal

`scripts/check-clingo.sh` krijgt een reason-carrying lijst `EXPECTED_CHANNEL` naar het model van gate 12: per golden die clingo niet speelt
`golden|CLINGO-###|reden`, waarbij de gate de code van de weigering vergelijkt met de regel (de code uit `backends/clingo/codes.tsv`, kind
`refusal` of `limit`), stale-bewaakt in beide richtingen (een regel voor een golden die speelt of weg is, faalt; een golden die weigert
zonder regel, faalt), en met een vloer of een nul-regel die zegt waarom. Daarna de twee verzuimrente-ankers terug naar hun exacte waarden in
de onbegrensde vorm, zoals de ankercasus van `spec/proposals/IMPLICIT-TIME.md` §4 ze geeft; clingo staat dan met naam in het kanaal ([V6]
en de factorgrens), ref en beide Drools-branches spelen ze exact.

## Acceptance

- (a) Rood eerst: de gate zonder kanaal faalt op een anker met exacte waarden (letterlijk); een zelftestrij per richting van de
  stale-bewaking.
- (b) CLAUDE.md gate 6 beschrijft het kanaal (voorstel; de hoofdsessie landt), in de stijl van gate 12, als huidige stand.
- (c) De ankers exact: de verwachtingen in de onbegrensde vorm (of een doel dat de breuk houdt), het afdrachtbedrag terug naar 10 000,00 €
  als dat de ankercasus is; ref en beide Drools-branches spelen ze; gate 13 en 17 groen.
- (d) Geen nieuwe rekenkunde en geen rationalen in `translate.ml`.
- (e) `make gates` groen.

May touch: `scripts/check-clingo.sh` (voorstel), `CLAUDE.md` (voorstel), `corpus/golden/anchor/jutterswet-verzuimrente-*` (voorstel),
`backends/clingo/codes.tsv` als een code ontbreekt, `scripts/check-drools.sh` als een pin verschuift, kaartverplaatsing.

Out of scope: andere goldens die nu om clingo heen geschreven zijn (meet en noem ze in het Result, als vervolg).

## Omvang

Klein tot middel.

## Result (bouwer, 2026-11-04)

Branch `card/clingo-refusal-channel`, gerebased op main `457debf65` (op `b55b6b6d7` één conflict, in
`scripts/check-drools.sh` bij `EXPECTED_PLAYED`: de stand van main, 328, met mijn commentaarregel erbij; op `457debf65`
zonder conflict, en geen van de zeven goldens van die fixronde is het anker, de zustergolden of een kanaalregel). Niet gemerged, niet gepusht.
Beschermd, dus **voorstel**: `scripts/check-clingo.sh`, `scripts/check-drools.sh`, `scripts/check-reftype-derivation.sh`,
`CLAUDE.md`, `corpus/golden/anchor/` (het anker en de README).

### Commits

- `7d2ea2295` clingo-refusal-channel: kaart naar doing
- `ff23961c9` corpus: het verzuimrente-anker exact, de afgeronde vorm als zustergolden
- `6038545af` clingo: het kalendertotaal buiten een afronding weigert met CLINGO-032
- `255963a92` gates 6 en 12: een weigerkanaal voor clingo, het exacte anker gepind (voorstel)
- `dac545205` gate 13 en CLAUDE.md: een golden in het kanaal van gate 6 (voorstel)
- het commit van dit Result

### De vorm van het kanaal (gate 6)

`EXPECTED_CHANNEL` in `scripts/check-clingo.sh`, één regel per golden: `<golden>|CLINGO-###|<reden>`. Spelen is de
standaard. Een golden in de lijst weigert op **elke** case met de gepinde code aan de kop van het rapport van de runner
(`TRANSLATION FAULT: CLINGO-032: …`, of `CLINGO-031: clingo: UNSATISFIABLE — …` voor een bewaking bij het oplossen), een
code uit `backends/clingo/codes.tsv` van de soort `refusal` of `limit`. De rechter staat in Python in het script (zoals
`check-exec-codes.sh`) en faalt bij golden-naam op:

- een golden die met een code weigert en in geen regel staat;
- een regel waarvan de golden elke case speelt, met een andere code weigert, of geen casebestand meer heeft (stale in
  beide richtingen);
- een golden waarvan de cases deels spelen en deels weigeren (een regel dekt elke case van zijn golden);
- een case die faalt zonder registercode aan de kop, met een EF-code, met twee codes, met een fallback-code, met een
  onbekende code, met een mismatch of een laadfout, ook als de golden in de lijst staat: alleen een weigering is te noemen;
- de eigen bewakingen van de lijst: een golden twee keer, een regel zonder drie velden of zonder reden, een code die geen
  `refusal` of `limit` van het register is.

De rechter speelt eerst 18 eigen rijen (`SELF_TEST`, één per bewaking, beide richtingen van de stale-bewaking erbij); een
rij die haar oordeel niet geeft, laat de gate falen. Geen vloer, met de zin waarom in het script: de lijst draagt namen en
is in beide richtingen bewaakt, haar lengte is een telling van weigeringen (krimp is vooruitgang), en de telling van de
gespeelde cases is de caseset die gate 5 vloert. Stand: één regel, `anchor/jutterswet-verzuimrente-dagen|CLINGO-032`.
PASS-regel in `make gates`: `check-clingo: PASS (backend suite 2462 checks, floor 83; 1058 cases replay exactly through clingo — ref =
clingo over the corpus; 1 golden(s) in the refusal channel, by name, 2 cases refused with the pinned code; the channel
rule passes 18 rows of its own)`.

De weigering van [V6] droeg geen code (`fault` zonder code in `translate.ml`); ze kreeg **CLINGO-032**
`calendar-total-outside-rounding`, soort `limit`, parameter `attribute`, op de ene plaats in `expr_term`. De vertaling
verandert niet; geen nieuwe rekenkunde, geen rationalen in `translate.ml`.

Gate 13 nam aan dat clingo elke golden onaangeroerd speelt. Voor een golden in het kanaal moet de vervalste kopie nu
dezelfde weigering geven als de onaangeroerde run, byte voor byte (`clingo_same`, in alle vier de benen); een
onaangeroerde run die niet met een registercode aan de kop eindigt, telt niet als antwoord, zodat twee gecrashte runs
niet gelijk uitvallen.

### De ankers en de zustergolden

**`anchor/jutterswet-verzuimrente-dagen` — exact.** De vorm van de ankercasus zoals de meting van IMPLICIT-TIME §5 haar
schreef (`werk/implicit-time/meting/b/source.rgs`): afdrachtbedrag `Numeriek (getal met 2 decimalen)`, invoer
`10000.00`; dagrente en verzuimrente `Numeriek (getal)`, de onbegrensde vorm, zonder afronding; de knip op het totaal en
de kalender zoals de kaart `timeline-arithmetic-per-period` (a) het anker vroeg. Verwachtingen: dagrente 400 / 500
(open grenzen), verzuimrente **`10160/73`** (139 13/73 €) en **`4500/61`** (73 47/61 €). `ir.json` door de frontend;
verifier PASS.

| uitvoerder | uitkomst | kanaal |
|---|---|---|
| ref | `oracle: PASS (3 expectations)` op beide cases | speelt |
| clingo | `TRANSLATION FAULT: CLINGO-032: the gedurende-clipped total of "dagrente" integrates its rate by the calendar …` | gate 6, CLINGO-032 |
| Drools, beide takken | `EXCLUDE DROOLS-042: rational value on 'verzuimrente' is not carried by the v0 backend` | gate 12, DROOLS-042 |

**`jutterswet-verzuimrente-dagen-afgerond` — de afgeronde zustergolden**, buiten `anchor/`: het oude anker ongewijzigd
verplaatst (`git mv`; afdrachtbedrag geheel, `10000`; dagrente 2 decimalen; verzuimrente rekenkundig op 2 decimalen),
verwachtingen **139,18 €** en **73,77 €**. Speelt in ref, clingo en beide Drools-takken (`PLAYED 2`). NOTE herschreven
als zustergolden, met waarom deze vorm (de schaal en de factorgrens 46 340 van de ASP-codering).

**`anchor/jutterswet-verzuimrente-30-360` — niet exact gemaakt (gemeten blokkade).** Twee pogingen:

1. De ankercasus letterlijk (`werk/implicit-time/meting/a/source.rgs`: afdrachtbedrag 2 decimalen, maandrente en
   verzuimrente onbegrensd, het product in de volgorde van §4), verwachtingen 50/3, 100/3, 125/3, 50/3 en 25, 125/3, 50/9,
   totaal 425/3 en 650/9 (`logs/03-measure-design.log`): ref `oracle: PASS (4 expectations)` op beide; clingo `clingo:
   UNSATISFIABLE — a fail-fast guard fired: viol("a division into a number declared without decimals is not whole, and
   this encoding holds the unbounded form as a whole number","A1"); viol("arithmetic range exceeded (clingo grounds in
   machine integers)","A1")` (twee bewakingen, beide zonder code); Drools, beide takken: `ERROR ComputeFault: error in the
   rule "maandrente": the period arithmetic of ch. 3.4 writes 50/3 on [2025-11-01, 2025-12-01) into the timeline of
   "maandrente", of the unbounded form: the value does not terminate, and a s…`. Gate 12 laat een `ERROR` altijd falen,
   ook met een regel in `EXPECTED_CHANNEL` ("a fault at fire is not a value-divergence — make it a per-form
   auto-exclusion"). Drools kan hier dus niet met code in zijn kanaal staan zonder een wijziging in `backends/drools`.
2. Een vorm waarin de maandwaarden eindigen bestaat niet: 16 2/3 €/mnd, 5 5/9 €/mnd en hun veelvouden in €/kw of €/jr
   (66 2/3) eindigen niet decimaal, en een maandrente met decimalen zonder afronding is een statische precisiefout.
   Een herformulering die de deling naar de verzuimrente verschuift (een tijdlijn rentevoet × rentedagen, 0,6 / 1,2 /
   1,5 …) houdt het totaal exact maar pint de maandrente van de ankercasus niet meer; verworpen.

Het anker blijft daarom in zijn afgeronde vorm, met zijn NOTE ongewijzigd. Vervolg: zie Open punten 1.

### Rood eerst (letterlijk)

- Gate 6 zonder kanaal op het exacte anker (het corpuscommit, het script van main; `logs/04-red-gate6-no-channel.log`), exit 1:
  `FAIL: corpus/golden/anchor/jutterswet-verzuimrente-dagen/case1-voorbeeld.json does not replay through clingo:` /
  `TRANSLATION FAULT: the gedurende-clipped total of "dagrente" integrates its rate by the calendar (ch. 3.6): a day
  weighs 1/len of its calendar unit, so the total has a denominator that no decimal scale of this encoding holds, and it
  is translated only as the base of a rounding`, en dezelfde regel voor `case2-schrikkeljaar.json`. De zustergolden
  speelde.
- Gate 13 vóór `clingo_same` (`logs/07-gate13.log`), exit 1: `FAIL: corpus/golden/anchor/jutterswet-verzuimrente-dagen/
  case1-voorbeeld.json — clingo answers differently on the lying copy:` / `TRANSLATION FAULT: CLINGO-032: the
  gedurende-clipped total of "dagrente" …`, en idem `case2`.
- Gate 12 met het exacte anker en zonder pin: niet apart gedraaid; de statusregel `EXCLUDE DROOLS-042: …` op beide takken
  (gemeten met de jar, `logs/03-measure-design.log`) valt zonder pin onder de kanaalbewaking van gate 12 (`pinned
  PLAYED, measured EXCLUDE DROOLS-042`; gevolgtrekking uit het script, niet gedraaid).

### Pins, floors en registers, oud → nieuw

- Gate 6: `EXPECTED_CHANNEL` nieuw, 0 → 1 regel; geen vloer (de nul-regel in het script); `EXPECTED_MIN_SUITE_CHECKS` 83
  ongewijzigd (gemeten 2462). Gespeelde cases 1058 + 2 geweigerd = 1060, gelijk aan gate 5.
- Gate 12, op de stand van main `b55b6b6d7` en `457debf65`: `EXPECTED_CHANNEL` 46 → 47 regels (+
  `anchor/jutterswet-verzuimrente-dagen|EXCLUDE DROOLS-042|…`); `EXPECTED_PLAYED` 328 → 328 (het anker verlaat het
  gespeelde kanaal, de zustergolden komt erin, `PLAYED 2`), met een regel in het commentaarblok. JUnit 1831 tests
  (floor 1573), DRL-teksttak 328.
- `backends/clingo/codes.tsv`: 31 → 32 codes (CLINGO-032, `limit`); `EXPECTED_MIN_CLINGO_CODES` 27 ongewijzigd.
- Gate 13: geen floor gewijzigd; de eenheidsbeen speelt de twee cases van het anker als gereproduceerde weigering.
- Gate 17: geen floor gewijzigd; PASS met 329 goldens en 933 casebestanden op beide takken vergeleken (de zustergolden
  erbij; het anker leest Drools zonder de verwachtingen, dus vergeleken als de andere).
- Gate 5: `check-oracle: PASS (ref suite 2319 worked examples, floor 489; 1060 cases replayed exactly, floor 427)`.
- Gate 13: `check-reftype-derivation: PASS (115 unit annotations twisted over 38 goldens, floor 38; 220 executor replays
  reproduce the untouched outcome, …; 0 pinned survivors)`.
- Clingo-suite: de rij `period arithmetic: calendar-family factor` las de case van het oude anker; het pad volgt de
  naam naar `jutterswet-verzuimrente-dagen-afgerond` (dezelfde inhoud). De rij `README [V6]`
  (`calendar-total-unrounded`) blijft ongewijzigd groen. Geen test omgedraaid.

### Meting: goldens die om clingo heen geschreven zijn

Methode: elke `corpus/golden/**/NOTE.md` per alinea gelezen op een vermelding van clingo of de ASP-backend samen met een
keuze-woord (stays below, so that, deliberately, holds … as a whole number, translates neither, …), de vondsten met de
hand nagelezen; daarnaast `backlog/done/` doorzocht op `46340`, `factorgrens`, `bereikgrens`, `zodat clingo`,
`ASP-codering houdt`. Niet veranderd.

| golden | wat om clingo heen is geschreven | bron |
|---|---|---|
| `anchor/jutterswet-verzuimrente-30-360` | maandrente op 3 decimalen en verzuimrente op de cent in plaats van 16 2/3 … en 141 2/3 / 72 2/9; een geheel afdrachtbedrag; het product als rentevoet × rentedagen × afdrachtbedrag om onder 46 340 per factor te blijven | NOTE "Why the targets round"; VB-C20 van `conditions-per-period` |
| `jutterswet-havengeld` | bedragen zonder eenheid (clingo weigert een percentage van een bedrag met eenheid) en onder 200 (de bereikgrens) | NOTE r. 40–46 |
| `jutterswet-precisiepoort` | de onbegrensde `ruwe aandeel` alleen met hele waarden (clingo houdt de onbegrensde vorm als geheel getal); een gelijst maximum en een schrijfactie die haar eenheid herrekent weggelaten | NOTE r. 26–32 |
| `jutterswet-postenheffing` | de bedragen "bewust klein (100.00 schaalt naar 10000 ≤ 46340)" | `backlog/done/20260822-fase5c-subscopes-tutorial.md` r. 318–325 (de golden heeft geen NOTE) |
| `jutterswet-sluitdag` | geen maximum of minimum van twee operanden ("the ASP backend does not translate that value") | NOTE r. 68–70; kaart `20260930-drools-exec-kwantorvoorwaarde-eigen-operand` |
| `jutterswet-keurgemiddelde` | het gevraagde lid met een fout na de stop (EF-011) ontbreekt: clingo geeft EF-011 en EF-019 bij het vertalen, zodat de case gate 6 niet haalt; de golden pint een depot in plaats van een fout | NOTE "Why a deposit and not a fault" |
| `jutterswet-verzuimrente-dagen-afgerond` | de afgeronde vorm, nu bewust als zustergolden van het exacte anker | deze kaart |

Genoemd maar niet als vormkeuze geteld: `jutterswet-kistinhoud` (de ribbes 1,25 en 2,50 m liggen "far below" het
machthek van 8,12, een natuurlijke maat), en de goldens die een hek van clingo juist als getuige uitoefenen
(`jutterswet-kindermacht`, `jutterswet-kinderprocent`, `jutterswet-kinderfoldheffing`, `jutterswet-klipwacht`,
`jutterswet-randwacht`, `jutterswet-vroege-vondsten-zonder-schorsing`): daar is de grens het onderwerp, niet de vorm. Een
meting die elke golden zonder afronding of met grotere getallen zou herspelen, is niet gedaan.

### Voorgestelde tekst (huidige stand)

- **CLAUDE.md**, gate 6 en een bijzin in gate 13: aangebracht in `dac545205` (voorstel).
- **CONSTRUCTS rij 11**, de zin "Executed by `ref` and the ASP backend (the latter carries the calendar total under a
  rounding, `backends/clingo/README.md` [V6]); witnesses `anchor/jutterswet-verzuimrente-dagen` and
  `jutterswet-rekenen-per-dag`; since step B (2026-11-03) both branches of the Drools backend carry them too (…)" →

  > Executed by `ref`, the ASP backend and both branches of the Drools backend (`PeriodArithmetic`: the values between
  > two operators exact, a time-dependent parameter's timeline on its global as a `List<Period>`, the calendar on every
  > route of the subject's own total and refused by name in the per-element fold over a set, DROOLS-228); the ASP backend
  > carries the calendar total under a rounding and refuses it elsewhere by name (CLINGO-032, `backends/clingo/README.md`
  > [V6]). Witnesses `jutterswet-verzuimrente-dagen-afgerond` and `jutterswet-rekenen-per-dag` (every executor), and the
  > anchor `anchor/jutterswet-verzuimrente-dagen` with the exact totals in the unbounded form (`ref`; the ASP backend in
  > gate 6's channel, CLINGO-032, and both Drools branches in gate 12's, DROOLS-042, the rational expectation).
- **RS ch. 3.6**, het voorbeeld van formulering B (r. 2416–2418, op `7e16cb14c`): "The anchor rounds both to the cent, 139,18 € and 73,77 €;
  the suite fixture `calendar-total-unrounded` plays the first exactly." → "The anchor pins both exactly, in targets of
  the unbounded form; the golden `corpus/golden/jutterswet-verzuimrente-dagen-afgerond` rounds them to the cent, 139,18 €
  and 73,77 €."
- **RS ch. 3.11** (r. 3920–3924), de getuigen van het rekenen per periode: "`corpus/golden/anchor/jutterswet-verzuimrente-dagen`
  (a time-dependent parameter)" → "`corpus/golden/jutterswet-verzuimrente-dagen-afgerond` (a time-dependent parameter;
  the anchor `anchor/jutterswet-verzuimrente-dagen` pins its exact totals, played by `ref`)".
- **RS ch. 3.12**, rij "Plus, min and the sum per period" (r. 3999): "(IMPLICIT-TIME VB 2.3; witness
  `corpus/golden/anchor/jutterswet-verzuimrente-dagen`)" → "(IMPLICIT-TIME VB 2.3; witness
  `corpus/golden/jutterswet-verzuimrente-dagen-afgerond`)"; rij "The temporal total across the day and the month family"
  (r. 4003): "the ASP backend under a rounding of the gedurende-clipped total, refusing it by name elsewhere" → "… by name
  elsewhere (CLINGO-032)", en "Witness `corpus/golden/anchor/jutterswet-verzuimrente-dagen`" → "Witnesses
  `corpus/golden/jutterswet-verzuimrente-dagen-afgerond` (every executor) and the anchor
  `corpus/golden/anchor/jutterswet-verzuimrente-dagen`, the exact totals (`ref`)".
- **ASP-TRANSLATION S9** (r. 1628–1645): "A calendar total elsewhere is refused by name (`backends/clingo/README.md` [V6])"
  → "… by name (CLINGO-032, `backends/clingo/README.md` [V6]; the anchor `jutterswet-verzuimrente-dagen` stands in gate 6's
  channel)"; "Witness: the anchor `jutterswet-verzuimrente-dagen`, D = 365 in case 1 and 366 in case 2." → "Witness: the
  golden `jutterswet-verzuimrente-dagen-afgerond`, D = 365 in case 1 and 366 in case 2."; en in de getuigenlijst "the
  calendar by `anchor/jutterswet-verzuimrente-dagen`" → "the calendar by `jutterswet-verzuimrente-dagen-afgerond`".
- **`backends/drools/README.md`** (r. 695 en 704; buiten mijn may-touch, de Drools-bouwer werkt ernaast):
  "`anchor/jutterswet-verzuimrente-dagen`" en "anchor `jutterswet-verzuimrente-dagen`" → "`jutterswet-verzuimrente-dagen-afgerond`".
- Aangebracht in deze branch (geen beschermd pad of de kaart noemt het): `backends/clingo/README.md` [V6] noemt
  CLINGO-032 en het kanaal; `corpus/golden/anchor/README.md` (voorstel) de rij van het anker en een bijzin over het kanaal;
  `corpus/negative/wf023-precision-calendar-total/NOTE.md` en een commentaar in `PrecisionGateTest.java` volgen de naam
  van de zustergolden.

### Voorlopige besluiten

- VB-1 — De exacte vorm van het dagen-anker is die van de gemeten ankercasus: afdrachtbedrag 10 000,00 € (2 decimalen) en
  beide doelen onbegrensd. De kaart zei "terug naar 10 000,00 € als dat de ankercasus is"; de meting van IMPLICIT-TIME §5
  declareert 2 decimalen. Met 2 decimalen moet de dagrente onbegrensd zijn (grens 4 voor de precisiepoort).
- VB-2 — De zustergolden is het oude anker, ongewijzigd verplaatst; zo blijft de vorm die alle uitvoerders spelen byte
  voor byte gepind, en blijft `anchor/` de plaats van de waarheid.
- VB-3 — Een regel van het kanaal dekt elke case van zijn golden; een golden die deels speelt en deels weigert, faalt
  (de lijst kan zo'n golden niet beschrijven; splits dan de cases).
- VB-4 — Alleen precies één code aan de kop, een CLINGO-code van de soort `refusal` of `limit`, is te noemen. Een
  UNSAT met twee bewakingen (zoals het 30-360-anker exact) is daarmee niet te pinnen; zie Open punten 1.
- VB-5 — CLINGO-032 is een `limit`: de vorm wordt vertaald waar de waarde past (onder een afronding), en geweigerd
  omdat de schaal van de codering de waarde niet houdt, zoals CLINGO-031.
- VB-6 — Geen vloer op het kanaal (de nul-regel); de bestaande keuze "geen vloer op de gespeelde cases, die is van gate 5"
  blijft.
- VB-7 — Drools staat bij het exacte dagen-anker met DROOLS-042 in zijn kanaal: de case-lezer weigert het rationale
  lexeem, en ook zonder die weigering zou de scalaire onbegrensde waarde als decimaal op de bean staan. Geen
  Drools-wijziging (buiten de may-touch, en de Drools-bouwer werkt ernaast).
- VB-8 — Het 30-360-anker blijft afgerond: Drools faalt op het exacte anker bij het vuren zonder code (`ERROR`), wat
  gate 12 nooit noemt; een code daarvoor is Drools-werk buiten deze kaart.
- VB-9 — Gate 13 vergelijkt een kanaalweigering van clingo byte voor byte met de onaangeroerde run, in alle vier de
  benen, en alleen als die run met een registercode aan de kop eindigt.

### Open punten

1. **Het 30-360-anker exact** vraagt twee dingen: (a) Drools geeft het segment van de onbegrensde vorm dat niet eindigt
   (`ComputeFault.nonTerminatingSegment`) als weigering met een `LIMIT`-code bij het vuren (een `Excluded`), zodat gate
   12 het anker kan pinnen; (b) clingo geeft zijn twee bewakingen een code (de onbegrensde vorm als geheel getal, en
   "arithmetic range exceeded"), of gate 6 aanvaardt een kop met meer codes waarvan de gepinde de eerste is. Daarna de
   vorm van poging 1 als anker en de huidige als zustergolden `jutterswet-verzuimrente-30-360-afgerond`. Een kaart na
   stap B van `total-over-partial-unit`.
2. Drools draagt geen rationaal lexeem in een case en houdt een scalair van de onbegrensde vorm als decimaal op de bean;
   wie dat oplost, haalt het exacte dagen-anker in het vierkant (ref ≡ Drools).
3. De goldens uit de meting hierboven, als vervolg per golden (de kaart zette ze buiten scope).

### Mutaties (niet gecommit; een losse worktree, `logs/09-mutations.log`, driver `werk/clingo-refusal-channel/design/mutate.py`)

Elke mutatie alleen, gate 6 gedraaid, het bestand teruggezet. Alle 17 rood.

| # | mutatie | uitkomst |
|---|---|---|
| M1 | de kanaalregel van het anker weg | rood: `anchor/jutterswet-verzuimrente-dagen refuses with CLINGO-032 on every case but stands in no line of EXPECTED_CHANNEL` |
| M2 | een regel voor de spelende zustergolden | rood: `stale channel line: jutterswet-verzuimrente-dagen-afgerond is pinned CLINGO-032 but plays every case` |
| M3 | een regel voor een golden die er niet is | rood: `stale channel line: anchor/jutterswet-bestaat-niet is pinned CLINGO-032, but no golden of that name has a case file` |
| M4 | het anker op CLINGO-031 | rood: `anchor/jutterswet-verzuimrente-dagen is pinned CLINGO-031, but its cases refuse with CLINGO-032` |
| M5 | het anker twee keer | rood: `duplicate channel line: 'anchor/jutterswet-verzuimrente-dagen' is named more than once` |
| M6 | het anker op CLINGO-002 (fallback) | rood: `… pins CLINGO-002 of the kind fallback …` |
| M7 | een regel zonder reden | rood: `channel line without a reason: 'jutterswet-verzuimrente-dagen-afgerond'` |
| M8 | rechter: een gepinde golden die speelt is niet stale | rood, zelftestrij `stale: a pinned golden that plays every case` |
| M9 | rechter: een regel zonder golden is niet stale | rood, zelftestrij `stale: a pinned golden that is gone` |
| M10 | rechter: de code wordt niet vergeleken | rood, zelftestrij `a pinned golden that refuses with another code` |
| M11 | rechter: een weigering zonder regel mag | rood, zelftestrij `unpinned: a golden that refuses with a code and stands in no line` |
| M12 | rechter: een EF-code aan de kop is een weigering | rood, zelftestrij `an EF code at the head is no refusal` (de rij faalt nu op de volgende arm, "EF-006, which backends/clingo/codes.tsv does not hold": de bewaking staat dubbel) |
| M13 | rechter: spelen en weigeren in één golden mag | rood, zelftestrij `a golden whose cases play and refuse` |
| M14 | rechter: een dubbele regel mag | rood, zelftestrij `a golden named twice` |
| M15 | rechter: een fallback-code aan de kop is een weigering | rood, zelftestrij `a fallback code at the head is no refusal` |
| M16 | rechter: twee codes aan de kop zijn een weigering | rood, zelftestrij `two codes at the head are no refusal` |
| M17 | `translate.ml`: de weigering van [V6] weer zonder code | rood: `… case1-voorbeeld.json does not replay through clingo (no register code at the head of the report)`, idem case2 |

M8–M16 zijn rood op de zelftestrijen en zouden op het corpus groen blijven: geen golden van vandaag oefent die armen
(M1 en M2 zijn de corpuskant van M11 en M8). Gate 13: de bewaking zelf is de rood-eerst hierboven (`logs/07-gate13.log`).
Twee deelmutaties van `clingo_same` zijn op het corpus gelijkwaardig en niet gedraaid: zonder de byte-vergelijking of
zonder de kopcontrole blijft gate 13 groen, want de enige kanaalgolden geeft op elke kopie dezelfde weigering met code
(gevolgtrekking uit de groene run, niet gemeten).

### `make gates`

Groen op `59c51fabb`, de boom van `dac545205` met dit Result (daarna veranderde alleen de kaart), gerebased op
`457debf65`: 17 gates en de drie bouwstappen rc=0, `after-run` rc=0, 390 s (`out/gates/20261104-080922/`,
`logs/gates-2.log`):

```
01-self-contained rc=0 2s    02-ir-schema rc=0 136s   04-case-schema rc=0 8s    03-verifier rc=0 39s
05-oracle rc=0 23s           06-clingo rc=0 316s      07-noop rc=0 14s          08-red rc=0 169s
09-catala rc=0 6s            10-regelspraak rc=0 278s 11-toka-parity rc=0 253s  12-drools rc=0 386s
13-reftype rc=0 350s         14-flogic rc=0 3s        15-graph rc=0 48s         16-scenario rc=0 41s
17-readings rc=0 288s        after-run rc=0 0s        end 2026-11-04 08:15:52 390s red=0
```

Gemeten in die run: gate 6 en gate 12 met dezelfde regels als hierboven (2462 suitechecks, 1058 + 2 cases, kanaal 47,
328 gespeeld, JUnit 1831), gate 5 2319 voorbeelden en 1060 cases, gate 17 329 goldens en 933 casebestanden. Een eerdere
run op `b55b6b6d7` was ook groen (699 s, `out/gates/20261104-075600/`, `logs/gates-1.log`).

De kaart blijft in `doing/`.
