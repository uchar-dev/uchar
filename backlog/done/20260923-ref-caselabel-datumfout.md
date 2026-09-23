# ref-caselabel-datumfout: de misvormde inputdatum meldt veld noch fase

Gemeten door de ref-geldigheid-kaart (2026-09-23, op
jutterswet-bergingsklok): een misvormde `date`-waarde in de
case-INPUT wordt luid geweigerd maar verkeerd gelabeld —
`EVALUATION FAULT: malformed date lexeme "10-03-2026"` zonder
attribuutnaam en zonder laadfase, waardoor de eigen
`malformed %s date`-checks van de caselader onbereikbaar zijn. Nooit
een stil fout antwoord, wel een melding die de auteur laat zoeken.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer; traceer waarom de laderzin onbereikbaar is (welke
  lezer wint) en welke posities hetzelfde hebben.
- (b) De weigering draagt veld, waarde en fase (laadtijd); de dode
  laderchecks gaan werken of verdwijnen (fail-fast, geen dode armen).
  Rood-eerst suitegetuige; mutatieproef.
- (c) Corpussweep: geen replay beweegt; alle veertien gates groen.

May touch: `ref/**`, kaartverplaatsing. NB: serieel met andere
ref-caselaagkaarten.

## Acceptatie, aangescherpt bij de claim (2026-09-23)

- (a) Reproductie op `jutterswet-bergingsklok` case2 (één datum
  dag-eerst) in ref en clingo, letterlijk. Trace: welke lezer wint en
  waarom de `malformed %s date`-armen van de caselader niet vuren.
  Inventaris van ELKE positie in het casebestand waar een datum- of
  datumtijdlexeem binnenkomt — attribuutwaarde, tijdlijnranden en
  -segmentwaarden (attribuut, kenmerk, parameter), parameterwaarde,
  lijst-element, structveld, constructorpayload, verwachte waarde en
  verwachte tijdlijn, asOfDate — als tabel: positie, lezer, zin, fase.
- (b) Elke positie weigert bij het laden als `CASE ERROR` (exit 2) met
  positie (pad of attribuutnaam), lexeem en de reden van de lezer; de
  dode armen verdwijnen of gaan werken. Rood-eerst suitegetuigen in
  `ref/test/test_ref.ml` (via de gedeelde caselader), de hele zin
  gepind; mutatieproef (label terug ⇒ rood ⇒ herstel, `cmp`).
  HARDE RANDVOORWAARDE: de asOfDate-weigerzin (misvormd en
  kalender-ongeldig) blijft byte-gelijk aan main, vóór en na gemeten
  met `cmp`; bestaande tests blijven ongewijzigd.
- (c) Sweep ref en clingo over alle golden- en roodcases vóór/na
  identiek; gates 5, 6 en 8 identiek in getallen; alle veertien gates
  groen, elk los. Clingo's caselezing gemeten (deelt hij de zinnen of
  heeft hij eigen?) zonder `backends/clingo` aan te raken.

May touch (aangescherpt): `ref/lib/case.ml`, zo nodig `ref/lib/value.ml`,
`ref/test/test_ref.ml` (alleen toevoegen), deze kaart. Niet:
`ref/lib/eval.ml`, `backends/**`, `frontends/**`, `spec/**`, `corpus/**`,
`scripts/**`.

## Result:

Alle acceptatiepunten gehaald; alle veertien gates groen, elk los
gedraaid op `97f87646`. Fix en getuigen zitten in één commit tussen
claim en afsluiting (`ref: de caselader weigert misvormde lexemen bij
het laden, met positie`). Onder de maintaineraanwijzing van 2026-09-23
(bijvangst in dezelfde zone of hetzelfde mechanisme meteen mee) is de
kaart verbreed; zie "May-touch-verbreding".

**(a) Reproductie.** `jutterswet-bergingsklok` case2 met
`P4.meldingsdatum` = `"10-03-2026"`, main: ref `EVALUATION FAULT:
malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd)` rc 1,
clingo `TRANSLATION FAULT: malformed date lexeme "10-03-2026" (expected
ISO yyyy-mm-dd)` rc 1. Na: beide `CASE ERROR: P4.meldingsdatum
"10-03-2026" is not the uchar-case-v0 date lexeme yyyy-mm-dd (malformed
date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))` rc 2.

**Welke lezer wint, en waarom de laderzin onbereikbaar was.**
`Value.date_of_iso` geeft uitsluitend `Date` terug of werpt
`Value.Fault`. De armen `| _ -> fail "%s: malformed %s date"` in
`timeline_of_json` en in de kenmerkperiodelezer matchten een niet-`Date`
RESULTAAT, dat nooit voorkomt; een waardepatroon vangt geen uitzondering,
dus de Fault liep langs de arm. De waardeposities (`value_of_json`,
`"date"`) riepen de lezer kaal aan. Beide runners omsluiten het laden
met `try … with Case.Error` en vangen `Value.Fault` pas in de buitenste
handler: EVALUATION FAULT (ref) of TRANSLATION FAULT (clingo), exit 1,
zonder veld. Gevolg daarbovenop: een rode case kon zo'n weigering niet
pinnen, want de laadarm leest alleen `Case.Error` en de pin wordt dan
niet geraadpleegd. Gemeten met attribuutdatum `"2026-02-30"` (gate 4
laat hem door) en `expectedFault` `day out of range in date lexeme
"2026-02-30"`: main ref `EVALUATION FAULT: day out of range in date
lexeme "2026-02-30"` rc 1, clingo idem als TRANSLATION FAULT rc 1; na
`oracle: expected fault observed at load (…)` rc 0, clingo `clingo:
expected fault observed at load (…)` rc 0.

**Inventaris**, gemeten met de runners (main- en na-binaries) op een
handgebouwd IR-document dat elke positie één keer draagt (`/tmp`, niet
in de repo), telkens één lexeem misvormd (`"10-03-2026"`, dateTime
`"2026-03-10 09:00:00"`); ref en clingo gaven op elke rij dezelfde zin
(op main alleen het woord EVALUATION/TRANSLATION verschillend):

| positie | lezer | zin op main | fase main | na (fase: laden, CASE ERROR, exit 2) |
| --- | --- | --- | --- | --- |
| `asOfDate` | `date_of_iso`, gevangen in `load_file` | `asOfDate "…" is not the uchar-case-v0 date lexeme yyyy-mm-dd (…)` | laden, exit 2 | ongewijzigd, zelfde sjabloon, byte-gelijk |
| attribuutwaarde `date` | `date_of_iso` kaal | `malformed date lexeme "…" (expected ISO yyyy-mm-dd)` | EVALUATION/TRANSLATION, exit 1 | `V1.vondstdag "…" is not the uchar-case-v0 date lexeme yyyy-mm-dd (…)` |
| lijstelement `date` | idem | idem | exit 1 | `V1.vangstdagen[1] "…" …` |
| structveld `date` | idem | idem | exit 1 | `V1.dossier.peildag "…" …` |
| constructorpayload `date` | idem | idem | exit 1 | `V1.status "…" …` |
| attribuuttijdlijn `from`/`to` | `day_of`, dode arm | idem | exit 1 | `V1.tarief.timeline[0].from "…" …` |
| attribuuttijdlijn segmentwaarde `date` | `date_of_iso` kaal | idem | exit 1 | `V1.ijkdag.timeline[0].value "…" …` |
| kenmerkperiode `from`/`to` | `day_of`, dode arm | idem | exit 1 | `V1.dienstdoend[0].from "…" …` |
| parameter `date` | `date_of_iso` kaal | idem | exit 1 | `parameter peildatum "…" …` |
| parametertijdlijn `from`/`to` | `day_of`, dode arm | idem | exit 1 | `parameter tariefreeks.timeline[0].from "…" …` |
| parametertijdlijn segmentwaarde | `date_of_iso` kaal | idem | exit 1 | `parameter ijkreeks.timeline[0].value "…" …` |
| verwachte waarde `date` | idem | idem | exit 1 | `expected V1.vondstdag "…" …` |
| verwachte tijdlijn `from`/`to` | `day_of`, dode arm | idem | exit 1 | `expected V1.tarief.timeline[0].from "…" …` |
| `dateTime` (attribuut, parameter, verwachte waarde) | `ms_of_datetime` (`Case.Error`) | `malformed dateTime "…" (at offset 10: expected 'T')` | laden, exit 2, zonder veld | `V1.meldstip "…" is not the uchar-case-v0 dateTime lexeme yyyy-mm-ddThh:mm:ss[.fff] (malformed dateTime "…" (at offset 10: expected 'T'))` |
| `number` (elke positie) | `q_of_lexeme`, de IR-literalgrammatica (WF-081) | `"1,000"` stil als 1 (bergingsklok `oracle: PASS`), `"12,5"` en `".5"` gelezen; `"1E3"`, `"+5"`, `"abc"`, `"1/0"`: `malformed number lexeme …` / `zero denominator …` | stil, of exit 1 | `P5.bergingsbestek "1,000" is not the uchar-case-v0 number lexeme [-]digits[.digits] or [-]digits/digits, the denominator without a leading 0` |

Buiten de lexemen, zelfde mechanisme (een laadweigering als
`Value.Fault`): een dubbel instantielabel (`World.add_instance`) gaf
`EVALUATION FAULT: duplicate instance label "V1"` rc 1, nu `CASE ERROR:
duplicate instance label "V1"` rc 2; een tijdlijn op een DAG-, WEEK- of
KWARTAAL-rooster (de roostercontrole loopt door `Timeline.bucket`, die
ch. 3.2 alleen voor maand en jaar definieert) gaf `EVALUATION FAULT:
bucketing is defined for month and year only (ch. 3.2)` rc 1, nu `CASE
ERROR: V1.tarief: bucketing is defined for month and year only (ch.
3.2)` rc 2. Clingo telkens identiek.

**(b) Het ontwerp.** Eén sjabloon per soort, het sjabloon dat asOfDate
sinds de geldigheidskaart al sprak: `<positie> "<lexeem>" is not the
uchar-case-v0 <soort> lexeme <vorm> (<weigering van de lezer>)`.
`Case.date_at ~at` leest door `date_of_iso` en zet een `Value.Fault` om
in een `Case.Error` met positie; `day_at` geeft het dagnummer voor
grenzen (`| _ -> assert false`, het huispatroon van
`Eval.validity_day`); `datetime_at ~at` leest door `ms_of_datetime`,
dat de enige dateTime-lezer van de lader blijft (en de bouwer van drie
bestaande suitevoorbeelden), en draagt diens zin als reden;
`number_at ~at` houdt het getalpatroon van het case-schema
(`-?[0-9]+(\.[0-9]+)?` of `-?[0-9]+/[1-9][0-9]*`, heel gematcht) en
leest daarna door `q_of_lexeme`, dat op alles wat het hek toelaat niet
kan falen. asOfDate loopt door `date_at ~at:"asOfDate"`, zodat zijn zin
en die van de andere posities één formatstring zijn en niet uit elkaar
kunnen lopen (mutant M1c hieronder). De dode `day_of`-armen zijn weg;
de grenzen, de segmentwaarde en de weigeringen per segment dragen de
index uit de JSON (`.timeline[i].from`, `.timeline[i].value`,
`.timeline[i]: segment 'to' must lie after 'from'`, kenmerkperiode
`[i]`); de twee weigeringen die segmenten onderling vergelijken houden
de positie van de tijdlijn. Labeluniciteit staat nu in de lader zelf
(de kop van `case.ml` claimde haar al); de roostercontrole zet de Fault
van `Timeline.bucket` om. In `ref/lib/eval.ml` verdween één dode arm
van dezelfde klasse (`| _ -> Value.fault "malformed asOfDate %S"` in
de periodeconditie, achter `date_of_iso`), vervangen door `assert
false` met een regel commentaar; gedrag ongewijzigd.

**HARDE RANDVOORWAARDE, gemeten.** Negen asOfDate-lexemen op
`jutterswet-vensterheffing` (`01-06-2026`, `2026-13-01`, `2025-02-29`,
`2026-02-30`, `2026-12-32`, `+012026-03-15`, `2026-3-15`,
`2026-06-01T00:00:00`, lege string), ref én clingo, volledige uitvoer
met exitcode: main-binaries vóór, eindbinaries na: `cmp` identiek (md5
beide `1baf670013d6d1ba8966c154eb29c81c`). Voorbeeld, vóór en na
gelijk: `CASE ERROR: asOfDate "2026-13-01" is not the uchar-case-v0
date lexeme yyyy-mm-dd (month out of range in date lexeme
"2026-13-01")` rc 2. De WV-3.10-caselader-voorbeelden pinnen die zin
ongewijzigd en blijven groen.

**Rood-eerst.** 36 nieuwe getuigen plus 5 canonieke controles
(canoniek laden; datum, negatief decimaal `-12.50`, breuk `3/4` en
datumtijd met fractie gelezen zoals geschreven) in
`ref/test/test_ref.ml`, als één blok vóór de slotregel; bestaande tests
niet gewijzigd. Tegen de main-lader (lib teruggezet, suite nieuw):

```
FAIL caselader-lexeem-attribuut-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-lijstelement-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-structveld-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-constructorpayload-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-attribuuttijdlijn-van (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-attribuuttijdlijn-tot (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-attribuuttijdlijn-waarde-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-kenmerktijdlijn-van (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-kenmerktijdlijn-tot (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-parameter-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-parametertijdlijn-van (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-parametertijdlijn-tot (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-parametertijdlijn-waarde-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-verwachte-waarde-datum (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-verwachte-tijdlijn-van (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-verwachte-tijdlijn-tot (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd))
FAIL caselader-lexeem-attribuut-datum-30-februari (escaped the loader as Value.Fault: day out of range in date lexeme "2026-02-30")
FAIL caselader-lexeem-attribuuttijdlijn-van-maand-13 (escaped the loader as Value.Fault: month out of range in date lexeme "2026-13-01")
FAIL caselader-lexeem-attribuut-datumtijd (got: malformed dateTime "2026-03-10 09:00:00" (at offset 10: expected 'T'))
FAIL caselader-lexeem-parameter-datumtijd (got: malformed dateTime "2026-03-10 09:00:00" (at offset 10: expected 'T'))
FAIL caselader-lexeem-verwachte-waarde-datumtijd (got: malformed dateTime "2026-03-10 09:00:00" (at offset 10: expected 'T'))
FAIL caselader-lexeem-attribuut-datumtijd-uur-24 (got: malformed dateTime "2026-03-10T24:00:00" (hour 24 is out of range))
FAIL caselader-lexeem-attribuut-getal-komma (loaded)
FAIL caselader-lexeem-attribuut-getal-kale-punt (loaded)
FAIL caselader-lexeem-attribuut-getal-exponent (escaped the loader as Value.Fault: malformed number lexeme "1E3")
FAIL caselader-lexeem-attribuut-getal-plusteken (escaped the loader as Value.Fault: malformed number lexeme "+5")
FAIL caselader-lexeem-attribuut-getal-tekst (escaped the loader as Value.Fault: malformed number lexeme "abc")
FAIL caselader-lexeem-attribuut-getal-noemer-nul (escaped the loader as Value.Fault: zero denominator in number lexeme "1/0")
FAIL caselader-lexeem-attribuuttijdlijn-waarde-getal-komma (loaded)
FAIL caselader-lexeem-parameter-getal-noemer-voorloopnul (loaded)
FAIL caselader-lexeem-verwachte-waarde-getal-exponent (escaped the loader as Value.Fault: malformed number lexeme "1.25E1")
FAIL caselader-dubbel-label (escaped the loader as Value.Fault: duplicate instance label "V1")
FAIL caselader-tijdlijn-dagrooster (escaped the loader as Value.Fault: bucketing is defined for month and year only (ch. 3.2))
FAIL caselader-segment-tot-niet-na-van (got: V1.tarief: segment 'to' must lie after 'from')
FAIL caselader-segment-buiten-rooster (got: V1.tarief: segment boundaries must sit on the declared granularity grid)
FAIL caselader-periode-tot-niet-na-van (got: V1.dienstdoend: period 'to' must lie after 'from')
36 failure(s)
```

De vijf canonieke controles waren daar groen. Na de fix: `test_ref:
all 847 worked examples pass` (was 806). De getuigen zijn in drie
rondes geschreven (lexemen, dan de twee andere laadweigeringen, dan de
segmentposities), elke ronde eerst rood tegen de toen geldende lader;
de lijst hierboven is de hele set tegen main.

**Mutatieproeven** op de eindlader, elk één keten (mutant → bouw,
afgebroken als die faalt → beide suites → herstel → bouw → `cmp`
identiek → `test_ref: all 847 worked examples pass`, `test_clingo: 523
checks, 0 failures`), alle dertien met herstel `cmp identiek`:

- M1a waardepositie-datum ongelabeld (`"date"`-arm kaal door
  `date_of_iso`): `9 failure(s)`, o.a. `FAIL caselader-lexeem-attribuut-datum
  (escaped the loader as Value.Fault: malformed date lexeme "10-03-2026"
  (expected ISO yyyy-mm-dd))`.
- M1b grenzen ongelabeld (`day_at` kaal): `9 failure(s)`, de acht
  grensposities en `…-van-maand-13`.
- M1c sjabloontekst (`is not the` → `is not a`): `21 failure(s)`,
  waaronder de drie bestaande asOfDate-pins: `FAIL
  WV-3.10-caselader-weigert-dag-eerst-asofdate (got: asOfDate
  "01-06-2026" is not a uchar-case-v0 date lexeme yyyy-mm-dd (malformed
  date lexeme "01-06-2026" (expected ISO yyyy-mm-dd)))`, idem maand-13
  en 29-februari — het bewijs dat asOfDate en de andere posities één
  sjabloon delen.
- M2 datumtijd ongelabeld: `4 failure(s)` (`got: malformed dateTime
  "2026-03-10 09:00:00" (at offset 10: expected 'T')`, en uur-24).
- M3 getalhek uit: `9 failure(s)` (`… (loaded)` voor komma, kale punt,
  `12,5`, `3/04`; `escaped … malformed number lexeme` voor `1E3`, `+5`,
  `abc`, `1.25E1`; `zero denominator` voor `1/0`).
- M4 segmentindex uit de grenspositie: `7 failure(s)` (`got:
  V1.tarief.from "10-03-2026" …`).
- M5 periode-index weg: `2 failure(s)` (`got: V1.dienstdoend.from …`).
- M6 hek weigert breuken (overweigering): `9 failure(s)`, voorop `FAIL
  caselader-lexeem-canoniek-laadt (refused: parameter heffing "3/4" is
  not the uchar-case-v0 number lexeme …)`.
- M7 labeluniciteit uit de lader: `1 failure(s)` (`escaped the loader
  as Value.Fault: duplicate instance label "V1"`).
- M8 roosterweigering niet omgezet: `1 failure(s)` (`escaped … bucketing
  is defined for month and year only (ch. 3.2)`).
- M9 segmentwaarde zonder index: `3 failure(s)` (`got: V1.ijkdag
  "10-03-2026" …`).
- M10 / M11 segment- en periodeweigering zonder index: elk `1
  failure(s)` (`got: V1.tarief: segment 'to' must lie after 'from'`,
  `got: V1.dienstdoend: period 'to' must lie after 'from'`).

De clingo-suite bleef onder alle dertien `523 checks, 0 failures`: hij
pint geen enkele zin van de lader; de getuigen van de gedeelde lader
staan in de ref-suite.

**Clingo.** De clingo-runner laadt via `Uchar_ref.Case.load_file` en
deelt dus elke gewijzigde zin byte voor byte: alle twintig
positievarianten, het dubbele label, het DAG-rooster, de getallen en
asOfDate gaven in beide runners dezelfde `CASE ERROR`-regel, rc 2
(gecontroleerd per regel). Zijn eigen datumlezingen na het laden
(`translate.ml`) hebben eigen zinnen en zijn dode armen van dezelfde
klasse — zie open punt 2. `backends/clingo` is niet aangeraakt.

**Drools, ter vergelijking (niet aangeraakt, zelfde fixtures, jar uit
gate 8):** dag-eerst `ERROR IllegalArgumentException: malformed date
'10-03-2026' on 'meldingsdatum': the uchar-case-v0 lexeme is yyyy-mm-dd
— fixed field widths, digits only, no sign and no extended year`;
`2026-02-30` `… the field widths are right but the calendar is not
(Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')`;
getallen op `P5.bergingsbestek`: `12,5` en `1,000` `ERROR
NumberFormatException: Character , is neither a decimal digit number,
decimal point, nor "e" notation exponential mark.` (geen veld); `.5`,
`1E3`, `+5` `PLAYED 1`; `1/0` `EXCLUDE rational value on
'bergingsbestek' is not carried by the v0 backend`.

**(c) Niets bewoog.** Sweep van ref- en clingo-runner over alle 638
golden- en roodcases (595 + 43), volledige uitvoer met exitcode, main-
binaries tegen eindbinaries: `cmp` identiek (3262 regels, clingo's
tijdelijke `.lp`-namen genormaliseerd). Gates 5, 6 en 8 op main-code in
deze worktree gedraaid (stash, draaien, terugzetten, `cmp`) en na:
gate 5 `595 cases replayed exactly` vóór en na, suite 806 → 847; gate 6
uitvoer byte-identiek (`backend suite 523 checks …; 595 cases replay
exactly through clingo`); gate 8 uitvoer identiek (`43 red cases …; 86
pinned fragments cite none of the 36 listed bottom sentences`, Drools-
been `9 fault-pinned, 34 not-carried`).

**Gates** (elk los, na `eval $(opam env)`, op `97f87646`): 1 PASS; 2
PASS (278 bestanden, floor 192); 3 PASS (278 golden / 440 negatief,
floor 350); 4 PASS (638 cases); 5 PASS (ref-suite 847, floor 489; 595
cases, floor 427); 6 PASS (523 checks, 595 cases); 7 PASS (559 + 36
overlevers); 8 PASS (43 rood, 86 fragmenten, geen bodemzin; Drools 9/34);
9 PASS (201 checks, 12 goldens); 10 PASS (409 tests, 70 rgs-goldens); 11
PASS (ref 29, clingo 29, Drools 29 PLAYED); 12 PASS (played 182, DRL
161, JUnit 620); 13 PASS (131 twists over 27 goldens, 128 replays, 25
DRL-played, 0 overlevers); 14 PASS (130 checks, 2 goldens).

**May-touch-verbreding** (maintaineraanwijzing 2026-09-23): naast de
datumlexemen ook de dateTime- en getalposities, labeluniciteit, de
roostercontrole en de segmentposities in `ref/lib/case.ml`, en één
dode arm in `ref/lib/eval.ml` (periodeconditie, rond r. 2387 — let op
bij het samenvoegen met de parallelle verifier-normkaart, die rond
`eval.ml` een notitie mag toevoegen). `ref/lib/value.ml` onaangeroerd,
dus ook de redenen van de lezers. `test_ref.ml`: alleen een blok
toegevoegd vóór de slotregel. Niet aangeraakt: `backends/**`,
`frontends/**`, `spec/**`, `corpus/**`, `scripts/**`.

**Beschermd pad:** geen voorstel nodig; geen specificatiezin wordt
onwaar. IR-SPEC ch. 4.1 (de case-lezertabel bij WF-095) zegt dat de
gedeelde lezer weigert "with the position" (de grammaticapositie); dat
blijft waar, nu met de caseplaats erbij. De taakverdeling in
`spec/schema/README.md` ("label uniqueness … calendar validity of dates
beyond the lexical pattern — are the `ref/` case loader's checks") is
sinds deze kaart letterlijk waar, waar twee daarvan eerder als
evaluatiefout ontsnapten; "Numbers are exact lexemes (decimal-with-dot
or `a/b` rational)" is precies het hek dat de lader nu houdt.

**Registerrijtekst `spec/CONSTRUCTS.md`, rij 37 (Scenario /
conformance-test format)** — achter "…, label-based role values (which
also feed row 4)" toevoegen:
"; **the shared case loader reads every lexeme at load and refuses by
position since 2026-09-23 (card `ref-caselabel-datumfout`)**: a
malformed date in any case position (attribute, list element, struct
field, constructor payload, timeline boundary and segment value,
characteristic period, parameter, expected value and timeline) left the
loader as an evaluation fault — `ref` reported `EVALUATION FAULT`, the
ASP backend `TRANSLATION FAULT`, exit 1, with neither the field nor the
load phase (measured on `jutterswet-bergingsklok`), the timeline
readers' own refusal arms could never fire, and a red case could not pin
such a refusal; a dateTime refusal named no field; a number was read by
the IR literal grammar (WF-081) instead of the case schema's pattern, so
`"1,000"` loaded as 1 without a word where the Drools reader errors; a
duplicate label and a timeline on a grid other than month or year
escaped the same way. All of these now refuse at load as case errors
(exit 2) naming position, lexeme and the reader's reason, one template
per kind — `P4.meldingsdatum "10-03-2026" is not the uchar-case-v0 date
lexeme yyyy-mm-dd (…)` — the template the asOfDate refusal already used,
whose sentence stays byte-identical; both OCaml executors share the
loader and so the sentences; 36 suite witnesses, a mutation per
mechanism; no replay moved (ref and clingo sweep over 638 cases
identical)".

**Open punten.**

1. Drools-caselezer (`backends/drools`, andere zone): de getallezer
   (`new BigDecimal` in `waardeNaarArg`) aanvaardt `.5`, `1E3` en `+5`,
   die het case-schema, ref en clingo weigeren (gemeten `PLAYED 1`), en
   weigert `1,000`/`12,5` als kale `NumberFormatException` zonder veld,
   waar zijn datum- en datumtijdklemmen veld en vorm noemen. Een wire,
   gesplitste lezers buiten het hek van gate 4; het huispatroon is een
   klem met het schemapatroon, zoals `datumVanCaseWire`. Zijn
   datumweigering noemt het attribuut maar niet het instantielabel.
2. Clingo `backends/clingo/lib/translate.ml` (andere zone): dode armen
   van dezelfde klasse als de hier verwijderde eval-arm — r. 1904
   `malformed date literal %S`, r. 3746 en r. 5839 `malformed asOfDate
   %S`, en de stille `| _ -> ()` op r. 4764 achter
   `datetime_of_lexeme`. Geen gedrag in het geding (de lader weigert een
   niet-datum-asOfDate al, en de lezers geven alleen `Date` of
   `Date_time`), wel armen die een weigering beloven die nooit komt.
3. De JSON-vormweigeringen van de lader (`expected a string`, `expected
   an object`, `missing field "x"`, en `malformed asOfDate` voor een
   asOfDate die geen string is) dragen geen positie. Fase klopt (CASE
   ERROR, exit 2) en gate 4 houdt ze uit het corpus, maar een
   handgebouwde case krijgt geen veld. Posities erin brengen betekent
   een pad door de ongeveer veertig `obj`/`str`/`field`-aanroepen
   rijgen: een herziening van het foutmodel van de lader, van een andere
   orde dan deze kaart; `malformed asOfDate` blijft onder de
   asOfDate-randvoorwaarde staan.
4. Corpus (`corpus/rood`, andere zone): sinds deze kaart is een
   kalender-ongeldige datum op elke invoerpositie (bijvoorbeeld een
   attribuutdatum `2026-02-30`, die gate 4 doorlaat) als rode case bij
   het laden te pinnen; vóór deze kaart faalde zo'n case in gate 8 (rc
   1, pin niet geraadpleegd; gemeten). Een rode getuige voor de
   kalenderhelft van een invoerpositie hoort bij de parallelle kaart
   `rood-asofdate-kalender` of een vervolg; meet daar eerst hoe het
   Drools-been van gate 8 de Drools-ERROR op zo'n case telt.
