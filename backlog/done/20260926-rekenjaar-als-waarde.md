# rekenjaar-als-waarde: de contextwaarde evaluationYear krijgt haar corpusgetuige en haar uitvoering

`evaluationYear` ("het rekenjaar") staat sinds L4 in WF-098's gesloten
naamverzameling als TOEGELATEN-EN-ONUITGEVOERD: RUNTIME-SEMANTICS ch. 8.2
zegt dat geen corpuscase haar raakt en dat elke uitvoerder haar bij naam
weigert met één zin ("the evaluationYear context value is not exercised by
any corpus case (ch. 8.2)": `ref/lib/eval.ml` r. 1048,
`backends/clingo/lib/translate.ml` r. 5395, de Drools-replay vóór het
loweren via `CaseRunner.draagtRekenjaar` en `transpiler/kern/Rekenjaar`,
plus beide transpilers op de knoop). De frontend emitteert haar al
(`Spelling` rij `Rekenjaar` → `evaluationYear`; `ExpressieBouwer` r. 492).
De oude toolchain voerde het rekenjaar wél uit (als het jaar van de
rekendatum); de maintainer besloot op 2026-09-26 dat uchar de vorm nu
draagt.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Semantiek (RS ch. 8.2, één clausule met worked example → `ref/test`):
  `evaluationYear` is het kalenderjaar van de evaluatiedatum, een heel,
  eenheidsloos getal (de bouwer toetst aan de RegelSpraak-specificatie,
  read-only, §5.3 "Rekenjaar", en legt vast of het getal een eenheid
  `jr` draagt; standaard: eenheidsloos, zoals de oude toolchain het las).
  IR-SPEC ch. 4.1: de aantekening "admitted-and-unexecuted" gaat weg; de
  typering van de knoop (`whole: true`, geen eenheid) staat er.
- U2 De drie uitvoerders voeren haar uit: ref en clingo evalueren de knoop;
  de Drools-replay laat de uitsluiting `draagtRekenjaar` vallen en beide
  takken loweren haar (het jaar uit de rekendatum van de sessie); de
  weigerzin verdwijnt uit alle vier plekken (`git grep "not exercised by
  any corpus case"` leeg).
- U3 Golden `jutterswet-rekenjaar` (**voorstel**): een regel die het
  rekenjaar als waarde gebruikt (bijvoorbeeld een leeftijd of een
  jaarklasse uit het rekenjaar en een geboortejaar) en een die het in een
  voorwaarde vergelijkt; cases met `asOfDate` in twee jaren; ref ≡ clingo
  ≡ Drools (beide takken). De negatief van WF-098 blijft (de naamverzameling
  verandert niet).
- U4 Pins: gate 12 `EXPECTED_PLAYED`/`EXPECTED_DRL_PLAYED` +1, klassekop
  herrekend; gate 7 (noop) en gate 13 (twist) per de vaste patronen.
- U5 Klaar: alle veertien gates groen; naamtabel-toets rc 0; geen
  regelnummerstaarten; de NOTE-citaten uit echte uitvoer.

## Acceptatie

- (a) U1–U5; (b) `ref/test` draagt het worked example; (c) een document met
  `evaluationYear` speelt in de drie uitvoerders met dezelfde uitkomsten.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 8.2, `spec/IR-SPEC.md` ch. 4.1
(**voorstel**), `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`corpus/golden/jutterswet-rekenjaar/`, `scripts/check-drools.sh`,
`scripts/check-noop.sh`, `scripts/check-reftype-derivation.sh` (pins,
**voorstel**). Niet in deze kaart: de frontend (emitteert al), het schema.

## Result (2026-09-26, bouwer)

### Stand

- Basis: main `b88a8ccc`. Claim: `0a98a4d5`. Kop: de commit die dit Result
  toevoegt (in het eindbericht genoemd). Werkboom schoon na elke commit.
- Commits: `c911c4cd` rekenjaar: de drie uitvoerders voeren evaluationYear
  uit; `2054d88d` spec: het rekenjaar als kalenderjaar van de rekendatum;
  `142f94d2` golden jutterswet-rekenjaar en de pins van gate 12; `892cbe09`
  gate 12: de kop van klasse (A) herrekend; daarna dit Result.

### Per U-besluit

**U1 (semantiek).** RUNTIME-SEMANTICS ch. 8.2 heeft een clausule "The
execution year": `evaluationYear` is het kalenderjaar van de evaluatiedatum,
heel en zonder eenheid, de waarde van `yearOf(evaluationDate)`; afgeleid uit
de ene datum van de sessie en nooit ernaast opgegeven; zonder datum faalt het
lezen. Het worked example (twee regels, V1 met `vondstjaar` 2025; sessies
2026-12-31 → 2026 / 1, 2027-01-01 → 2027 / 2, null → fault) staat verbatim
in `ref/test/test_ref.ml` als zeven voorbeelden `WV-8.2-rekenjaar-*`, plus
de eenheidsloosheid en de gelijkheid met `yearOf` op dezelfde sessie. Eenheid:
eenheidsloos (standaardantwoord bevestigd). Getoetst aan (1) de
RegelSpraak-specificatie §5.3, read-only en niet geciteerd: de paragraaf
noemt voor het rekenjaar geen eenheid, en de grammatica zet het rekenjaar bij
de getalexpressies naast de jaar-uit-functie; (2) de oude toolchain, die het
jaar als kaal geheel getal las (`rsRekenjaar`, `BigDecimal.valueOf(getYear())`,
teruggehaald uit commit `866eeea9`); (3) uchar's eigen `yearOf`, dat
`Num (jaar, Unitless)` geeft. Eén verschil met §5.3 is bewust: daar worden
datum en jaar beide opgegeven, in uchar wordt het jaar afgeleid (RS 8.1: één
datum per sessie); de reden staat in de clausule. IR-SPEC ch. 4.1: de
aantekening "admitted-and-unexecuted" is weg uit de tabelrij, het blok van
WF-098 en de alinea over de naam zonder drager; de typering staat er
(`Number`, `whole: true`, `decimals: 0`, geen `unit`). Ook bijgewerkt,
omdat ze de oude stand in de tegenwoordige tijd noemden: de geschiedenisalinea
eerder in ch. 4.1 ("was carried by no document") en de WF-098-rij van de
WF-tabel (drager en golden-kolom). Nieuw en gemeten: WF-098 leest alleen de
naam en toetst de `type`-annotatie niet tegen het vaste type (restpunt 2).

**U2 (uitvoering).** ref (`eval.ml`): `Arith.jaar_uit` op de datum van de
case. clingo (`translate.ml`): programmaconstante, het jaar van de dag van
`asOfDate`, schaal 1. Drools: `rsRekenjaar` terug in de runtime (fault zonder
datum), DRL `rekenConstante` en exec `scalarWaarde` verlagen de knoop,
`CaseRunner.draagtRekenjaar` en `transpiler/kern/Rekenjaar` zijn weg, de
ongebruikte `Spelling`-import in `CaseRunner` ook. Twee exec-plekken vonden
de rekendatum-global niet en weigerden de knoop daarom: een objectcreatie
(las `globals.get` in plaats van `waardeGlobal`, en castte de slot naar
`BigDecimal`) en een defaultsgroep (`heeftTijdgebonden` scande de defaults
niet). Beide gerepareerd; rood-eerst gemeten op de branch vóór de reparatie:
`objectcreatie (niet-geregistreerde global)` en `defaults (…): consequentie
leest niet-geregistreerde global rekendatum`. De weigerzin: `git grep` op de
zin geeft buiten `backlog/` niets; binnen `backlog/` alleen twee afgesloten
kaarten (geschiedenis, één daarvan over een andere zin), zie restpunt 7.

**U3 (golden).** `corpus/golden/jutterswet-rekenjaar`: `bron.rgs` (drie
regels: meldjaar = het Rekenjaar; aantal verstreken penningjaren = het
Rekenjaar min zijn vondstjaar indien zijn vondstjaar ≤ het Rekenjaar;
kenmerk laat gemeld indien het Rekenjaar > zijn vondstjaar), `ir.json` via
`make ir`, twee cases met dezelfde vier vondsten (2026, 2025, leeg, 2027) op
2026-12-31 en 2027-01-01, `NOTE.md` met handberekening en citaten uit echte
uitvoer. Vier posities (regelwaarde, aftrekoperand, vergelijkingsoperand
rechts en links), alle uitkomstklassen (waarde, leeg, aanwezig, afwezig).
Rood-eerst op de basis: verifier PASS; ref `EVALUATION FAULT` en clingo
`TRANSLATION FAULT` met de oude weigerzin; Drools beide takken `EXCLUDE` met
dezelfde zin. Na de bouw: verifier PASS; ref `oracle: PASS (13 expectations)`
en clingo `clingo: PASS (13 expectations)` per case; Drools exec `PLAYED 2`,
DRL `PLAYED 2`. Mutatie "jaar van de dag ervoor" in ref, clingo en de
Drools-runtime: case1 blijft groen, case2 faalt overal (8 afwijkingen in ref
en clingo, `FAIL case2-nieuwjaarsdag.json` in beide Drools-takken). De
negatief van WF-098 blijft ongewijzigd.

**U4 (pins).** `scripts/check-drools.sh`: `EXPECTED_PLAYED` 209 → 210 en
`EXPECTED_DRL_PLAYED` 179 → 180, elk met redenblok. Klassekop herrekend
tegen de lijst: A 34, B 37, C 10, D 2, E 3 = 86 regels, kloppen; de kop van
(A) noemde een verschil van 29 tussen de takken, dat sinds 208 → 209
(`jutterswet-initialisatievolgorde`, klasse D, alleen exec speelt) 30 is
(210 − 180); gecorrigeerd. Gate 7: de golden is geen overlever (noop faalt
op 7 en 9 verwachtingen), `check-noop.sh` ongewijzigd. Gate 13: de golden
draagt geen eenheid, wordt niet getwist, `check-reftype-derivation.sh`
ongewijzigd.

**U5 (klaar).** Zie gates. Naamtabel-toets `v1-naamtabel-toets: OK` (rc 0).
Geen regelnummerstaarten in de zinnen; geen D-/U-nummers in spec, scripts,
NOTE of code; de NOTE-citaten komen uit de runs hierboven.

### Zinnen letterlijk

- Nieuw, in ref, clingo en de Drools-runtime (`RegelSpraakRuntime.REKENJAAR_ZONDER_REKENDATUM`) gelijk:
  `the evaluationYear context value needs an evaluation date; the case carries asOfDate null`
  (ref met prefix `EVALUATION FAULT: `, clingo `TRANSLATION FAULT: `, Drools
  een `IllegalStateException` in de oorzakenketen van een fault bij het vuren).
- Weg: de weigerzin die de kaarttekst citeert, op alle vier plekken.

### Gates (op de branch; 12, 1, 3 en de naamtabel nogmaals op `892cbe09`)

1 `check-self-contained: PASS`; 2 PASS, 308 files, 37990 objecten in
schemavolgorde; 3 PASS, 308 golden clean, 481 negatief, 324 op hun arm,
584 pointers; 4 PASS, 754 cases; 5 PASS, ref-suite 1054 voorbeelden (+7),
711 cases; 6 PASS, suite 1882 checks, 711 cases; 7 PASS, 667 weerlegd, 44
overlevers; 8 PASS, 43 rode cases, Drools-leg 11 fault-pinned, 32 met
reden; 9 PASS, 241 checks, 12 goldens; 10 PASS, JUnit 764, 94
rgs-goldens (+1); 11 PASS, 29/29/29; 12 PASS, played 210 (pin 210), DRL
180 (pin 180), JUnit 815 (+1: vijf tests weg, zes erbij); 13 PASS, 41
twists over 23 goldens, 106 replays, 22 DRL-spelers; 14 PASS, 150 checks,
2 goldens. Logs: (buiten de repo).

### Getwiste posities (Drools-getuige `RekenjaarWitnessTest`)

De oude weigergetuige is met `git mv` de uitvoeringsgetuige geworden (zes
tests). Per positie, datum 2026-03-15 waar de case geen had, verwachting =
ref's antwoord op het getwiste document: consistentie exec `EXCLUDE …
schending buiten Phase-D scope`, DRL speelt; creatie-initialisatie beide
spelen (2027, inconsistent); allocatiewacht beide; feitcreatiewacht beide;
vouwfilter exec `EXCLUDE … aggregatie buiten Phase-C scope`, DRL speelt;
defaults-consequentie exec speelt (20260.00 / 20270.00), DRL `EXCLUDE` (geen
defaults). De getwiste creatiewacht is vervallen: daar weigeren ref (`no
fixpoint within 100 passes`) en clingo (`generation budget exhausted`) op
het generatiebudget en draaien beide Drools-takken door, het bekende
verschil van `finite-chain-over-budget`, los van het rekenjaar.

### Zelf genomen technische besluiten

1. Drools faalt zonder datum (niet leeg), in de zin van ref en clingo.
2. De twee exec-reparaties vallen onder "beide takken loweren haar".
3. De testbestanden die de weigering pinden (`RekenjaarWeigeringWitnessTest`,
   het clingo-blok) zijn omgezet: de kaart eist dat de zin verdwijnt, en die
   tests pinden haar.
4. De WF-098-rij en de geschiedenisalinea van ch. 4.1 zijn mee bijgewerkt.

Geen normvraag open.

### Voorgestelde registertekst

- CONSTRUCTS rij 25, achteraan de laatste cel: "**Landed (2026-09-26, card
  `backlog/done/20260926-rekenjaar-als-waarde.md`):** `evaluationYear` is
  executed — the calendar year of the evaluation date, a whole number
  without a unit (`RUNTIME-SEMANTICS.md` ch. 8.2, worked examples
  WV-8.2-rekenjaar-*); ref, clingo and both Drools branches carry it, the
  refusal by name is gone, and the golden `jutterswet-rekenjaar` is its
  carrier. WF-098 does not yet cross the node's type annotation against the
  name (recorded open in `IR-SPEC.md` ch. 4.1)."
- PLAN §9, bij de golf vóór de verhuizing: "`rekenjaar-als-waarde` landed
  (2026-09-26): `evaluationYear` is the calendar year of the evaluation date
  in all three executors, with the golden `jutterswet-rekenjaar`; gate 12
  at 210 / 180."
- CLAUDE.md: geen regel; er kwam geen gate bij.

### Restpunten

1. De exec-tak verlaagt een contextwaarde (beide namen) niet in een
   consistentie-assertie (de schendingslezer bindt geen globals) en niet in
   een vouwfilter; de DRL-tak speelt beide met ref's antwoord. Per tak
   gepind in `RekenjaarWitnessTest`.
2. WF-098 toetst de `type`-annotatie van een `ContextValue` niet tegen het
   vaste type van de naam. Gemeten op de golden: `evaluationYear` met twee
   decimalen geannoteerd → verifier PASS en alle uitvoerders geven het jaar;
   een `Date`-annotatie vangt alleen WF-023 op die plek. Voorstel: een arm
   in WF-098 (evaluationDate `Date` day, evaluationYear `Number` heel, 0
   decimalen, geen eenheid) met negatief-getuigen; verifier is beschermd en
   buiten deze kaart.
3. Zonder datum leest Drools `evaluationDate` als leeg en speelt (beide
   takken `PLAYED 1` op een case die leeg verwacht), waar ref en clingo
   falen met "the evaluationDate context value needs an evaluation date; the
   case carries asOfDate null". Bestaand gedrag van de zuster; het
   rekenjaar faalt nu wel in alle drie. Raakt ook de geldigheidsvensters,
   die bij een ontbrekende datum niet matchen; daarom niet in deze kaart.
4. Een rode case `rekenjaar-zonder-rekendatum` (corpus/rood, buiten "May
   touch") zou de fault zonder datum in gate 8 voor alle uitvoerders pinnen;
   nu pinnen `ref/test`, de clingo-suite en `RekenjaarWitnessTest` haar.
5. Clingo weigert op de getwiste defaults-consequentie met zijn bereikhek
   (`arithmetic range exceeded`), waar ref antwoordt; met een letterlijke
   2026 op die plek gebeurt hetzelfde, dus los van het rekenjaar.
6. `spec/proposals/V1-WIREMIGRATIE.md` (rond r. 1343) noemt het rekenjaar
   nog toegelaten-en-onuitgevoerd; historisch voorstelstuk, buiten "May
   touch".
7. Het acceptatiecommando van U2 (`git grep` op de weigerzin) is leeg
   buiten `backlog/`; twee afgesloten kaarten houden de tekst als
   geschiedenis.

## Lens en fixronde (2026-09-26, hoofdsessie)

Lens uitvoerders (unie cfa643d0): EERST FIXEN op vier punten, waarvan twee (de dubbele pinregels in gate 12 en de
routesuite die de vervallen emitterweigering toetste) al de samenvoegfix e6844fba van de hoofdsessie waren. Gemeten
voor deze kaart: regelwaarde, operand, vergelijking en de voorwaarde van een objectcreatie in vier uitvoerders gelijk
op 2026-12-31 en 2027-01-01; zonder datum dezelfde zin in ref en clingo, Drools toont haar alleen in het rode pad;
de mutant "jaar van de dag ervoor" overal gevangen (golden case2, ref-suite, clingo-suite, RekenjaarWitnessTest);
consistentie-assertie en vouwfilter: ref, clingo en de DRL-tekst gelijk, de exec-tak weigert generiek. Fixronde
(branch fix/uitvoerders): de rode case voor het rekenjaar zonder datum (A3), de WF-098-typearm met de juiste
beschrijving per positie (A1), de derde plek die de exec-tak niet verlaagt — de justificatie van een default — als
getuige (A2); registerteksten aangescherpt (A10).
Uitkomst fixronde (branch fix/uitvoerders): A3 rode case `rekenjaar-zonder-rekendatum` (drie uitvoerders, één zin); A1 en de WF-098-typearm met twee negatieven (`wf098-rekenjaar-met-decimalen`, `wf098-rekendatum-met-tijd`; normvraag met standaardantwoord: het teken van `evaluationYear` is "unspecified"); A2 de justificatie van een default als derde exec-plek in `RekenjaarWitnessTest`; V1-WIREMIGRATIE bijgewerkt. Open: de Drools-replay zonder datum leest `evaluationDate` leeg en speelt (zustergedrag).
