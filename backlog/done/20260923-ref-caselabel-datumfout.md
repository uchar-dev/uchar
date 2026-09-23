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

*Stand van dit Result: de eerste ronde hieronder is ongewijzigd
bewaard; de secties "Fixronde 1", "Fixronde 2" en "Fixronde 3" onderaan
dragen de latere wijzigingen en de actuele getallen, aangevuld met
"Fixronde 4" en "Fixronde 5". De branch is in fixronde 5 op main
`a1b9daf0` gerebased; alle commithashes in dit Result zijn de hashes na
die rebase. Metingen uit de rondes 1 tot en met 4 zijn gedaan op de oude
basis `8dd05e25`; de getallen op de nieuwe basis staan in fixronde 5.*

Alle acceptatiepunten gehaald; alle veertien gates groen, elk los
gedraaid op `66a4c27c`. Fix en getuigen zitten in één commit tussen
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

**Gates** (elk los, na `eval $(opam env)`, op `66a4c27c`): 1 PASS; 2
PASS (278 bestanden, floor 192); 3 PASS (278 golden / 440 negatief,
floor 350); 4 PASS (638 cases); 5 PASS (ref-suite 847, floor 489; 595
cases, floor 427); 6 PASS (523 checks, 595 cases); 7 PASS (559 + 36
overlevers); 8 PASS (43 rood, 86 fragmenten, geen bodemzin; Drools 9/34);
9 PASS (201 checks, 12 goldens); 10 PASS (409 tests, 70 rgs-goldens); 11
PASS (ref 29, clingo 29, Drools 29 PLAYED); 12 PASS (played 182, DRL
161, JUnit 620); 13 PASS (131 twists over 27 goldens, 128 replays, 25
DRL-played, 0 overlevers); 14 PASS (130 checks, 2 goldens).

**May-touch-verbreding** (maintaineraanwijzingen 2026-09-23; stand na
fixronde 2). Aangeraakt: `ref/lib/case.ml` (lexemen, vormweigeringen,
labeluniciteit, rooster, segment- en payloadposities); `ref/lib/eval.ml`
op drie plekken — de dode `malformed asOfDate`-arm in de periodeconditie
(rond r. 2387), het gedateerde commentaar bij `periode_onder_telkwantor`
(rond r. 204, eerste extra item) en de WF-022-weigering vóór `run_rule`
(tweede extra item); `ref/test/test_ref.ml` (twee blokken vóór de
slotregel, alleen toegevoegd, plus de eigen getuigen van deze kaart
bijgesteld); `corpus/rood/inputdatum-kalender/` (nieuw, open punt 4 van
de eerste ronde, op aanwijzing van de hoofdsessie); en, uitsluitend als
beschermd-pad-VOORSTEL, `scripts/check-case-schema.sh` (commits
`a118fdc4`, `b76af45b` en `e6c3b413`) en `scripts/check-ir-schema.sh`
(commit `98884c2e`, fixronde 3, en commentaar in `b76af45b` en
`e6c3b413`). Onaangeroerd: `ref/lib/value.ml` (dus ook de
redenen van de lezers), `backends/**`, `frontends/**`, `spec/**`, de
rest van `corpus/**` en `scripts/**`. Samenloop: de parallelle
verifier-kaart raakt `ref/lib/eval.ml` en `ref/test/test_ref.ml` ook
(volgens de lens 3-weg schoon; de WF-022-weigering staat ver van haar
leeg-testnotitie); wie als tweede landt, draait gate 5 na de rebase
opnieuw.

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
also feed row 4)" toevoegen (versmald naar de meting, lensadvies 3):
"; **the shared case loader refuses a malformed lexeme or JSON shape at
load, by position, since 2026-09-23 (card `ref-caselabel-datumfout`)**:
a malformed date in any case position (attribute, list element, struct
field, constructor payload, timeline boundary and segment value,
characteristic period, parameter, expected value and timeline) left the
loader as an evaluation fault — `ref` reported `EVALUATION FAULT`, the
ASP backend `TRANSLATION FAULT`, exit 1, with neither the field nor the
load phase (measured on `jutterswet-bergingsklok`) — the timeline
readers' own refusal arms could never fire, and a red case could not
pin such a refusal; a dateTime refusal named no field; a number was
read by the IR literal grammar (WF-081) instead of the case schema's
pattern, so `"1,000"` loaded as 1 without a word where the Drools reader
errors; the JSON-shape refusals said `expected a string` or `missing
field` with no position. Now every date, dateTime and number lexeme is
refused at load as a case error (exit 2) naming the position and the
lexeme, with the reader's reason for dates and dateTimes and the
schema's form for numbers (`P4.meldingsdatum "10-03-2026" is not the
uchar-case-v0 date lexeme yyyy-mm-dd (…)`, the template of the asOfDate
refusal, whose sentence stays byte-identical); a shape refusal names the
position and what it found; a duplicate label is refused by the label;
a timeline on a day, week or quarter grid is refused by position and
declared grid as a v0 limit (ch. 3.2 buckets month and year only); an
empty instance label is refused by its array position. Both
OCaml executors share the loader and so the sentences. Red witness
`corpus/rood/inputdatum-kalender` (an attribute date `2026-02-30` that
gate 4 admits, pinned in ref, clingo and Drools); suite witnesses with a
mutation per mechanism; no replay moved (ref and clingo sweep over 643
cases identical to main except the new red case)".

**Registerrijtekst `spec/CONSTRUCTS.md`, rij `— Soort`** (fixronde 1,
tweede extra item; herschreven in fixronde 5, lensadvies 1) — achter
"…, no separate flag" toevoegen:
"; **`ref` refuses every KENMERKTOEKENNING whose value is not the constant
`waar` since 2026-09-23 (card `ref-caselabel-datumfout`)**: such a rule was
run anyway — `corpus/negatief/wf022-kenmerk-onwaar`, forced past the
verifier, answered `oracle: PASS` with the characteristic absent, and a
computed boolean was written like the constant, where the ASP backend
refuses at translation; `ref` now tests the constant (the literal `waar`
typed Booleaan) before any subject is taken and refuses by name with rule,
target, object type, value and the WF-022 ground. It holds the clause for
every KENMERKTOEKENNING, where the verifier holds it only for a target that
is a declared characteristic: a KENMERKTOEKENNING on an attribute passes the
verifier, and `ref` and the ASP backend refuse it. No golden, red or
parity-zone document carries the form, only that negatief, which gate 3
refuses".

### Fixronde 1 (hoofdsessie, 2026-09-23)

**Open punt 4: het rode document `corpus/rood/inputdatum-kalender`**
(commit `74b40103`). `ir.json` is `jutterswet-bergingsklok` byte voor byte
(`cmp`); `case1.json` is case2 van die golden met één waarde anders,
`P4.meldingsdatum` = `"2026-02-30"`, die het case-schema doorlaat; NOTE met
handmatige uitleg. Pins: ref en clingo `P4.meldingsdatum "2026-02-30" is
not the uchar-case-v0 date lexeme yyyy-mm-dd (day out of range in date
lexeme "2026-02-30")`; Drools `malformed date '2026-02-30' on
'meldingsdatum': the field widths are right but the calendar is not`, de
eigen zin van de caselezer tot vóór de JDK-tekst tussen haakjes.

Rood-eerst tegen mains lader: ref `EVALUATION FAULT: day out of range in
date lexeme "2026-02-30"` rc 1, clingo `TRANSLATION FAULT: day out of
range in date lexeme "2026-02-30"` rc 1 — de pin wordt niet eens gelezen.
Gate 8 met de lib van main en dit document: `FAIL:
corpus/rood/inputdatum-kalender/case1.json — ref did not observe the
expected fault: EVALUATION FAULT: day out of range in date lexeme
"2026-02-30"`, idem voor clingo, rc 1. Na: `oracle: expected fault
observed at load (P4.meldingsdatum "2026-02-30" is not …)` rc 0, clingo
idem. DroolsRoodMain (jar uit deze worktree): `FAULT
IllegalArgumentException: malformed date '2026-02-30' on 'meldingsdatum':
the field widths are right but the calendar is not (Text '2026-02-30'
could not be parsed: Invalid date 'FEBRUARY 30')`. `ref --bodemzin` met de
36 zinnen van gate 8: `PINS 2`, geen HIT. Gate 8: 44 rood (was 43), 88
fragmenten (was 86), Drools-been 10 fault-pinned (was 9) en 34
not-carried. `scripts/check-rood.sh` niet aangeraakt. Mains gate 8 (sinds
de rood-duo-kaart, nog niet op deze branch) rekent een benoemde weigering
van de caselezer tot FAULT en eist voor een pin de eigen zin zonder
platformtekst; deze pin voldoet daaraan.

**Open punt 3: positiedragende vormweigeringen** (commit `df683743`). De
strikte helpers `obj`, `str`, `list_` en `field` dragen een positie en
tonen wat ze vonden (`… : expected a string, got 20260310`); een veld en
zijn JSON-soort worden in één aanroep gelezen (`str_field`, `obj_field`,
`list_field`), zodat een nieuwe leesplek zijn positie niet kan vergeten.
Het padvocabulaire is dat van de lexeemweigeringen: een instantie bij
haar label zodra het gelezen is (`V1.objectType`, `V1.roles.melders[0]`),
daarvoor bij haar arrayplaats (`input.instances[0].label`), een
verwachting bij haar index (`expected[4].kind`), de verwachte waarde bij
label en attribuut (`expected V1.vondstdag`), de wortel als `<root>`, zoals
de runners de wortel van een IR noemen. De eigen formuleringen van de
waardevormen gingen in hetzelfde sjabloon op (`malformed value`,
`malformed boolean`, `a list value's "list" field is not an array`, `a
record value's "struct" field is not an object`, `malformed timeline
value`), de payload kreeg een eigen positie (`V1.status.payload`, ook voor
de lexeemzin), en drie kruiscontroles die bij meerdere kandidaten
onvindbaar waren noemen hun plaats (`M1.roles.vondst: to-one role "vondst"
carries 2 labels`, `expected[4]: an inconsistent label pin requires a rule
pin`, `expected[4].kind: unknown expectation kind "consistentt"`). Een
niet-string asOfDate: `asOfDate: expected a string or null, got
20260601` (was `malformed asOfDate`). Bewust anders gelaten: open punt 4
onderaan. Aanvaarding verandert nergens, alleen zinnen; de tijdlijnwrapper
leest nu zelf de segmentlijst (`timeline_json`), waardoor een verdedigende
arm onnodig werd.

De asOfDate-lexeemzin: negen lexemen × ref en clingo, `cmp` identiek met
main (md5 `1baf670013d6d1ba8966c154eb29c81c`, gemeten na elke wijziging).
De parallelle rood-kaart pint in `corpus/rood/asofdate-kalender` alleen de
lexeemzin (ref/clingo `asOfDate "2026-02-30" is not the uchar-case-v0 date
lexeme yyyy-mm-dd (day out of range in date lexeme "2026-02-30")`, Drools
`malformed date '2026-02-30' on 'asOfDate': …`), niet de niet-string-vorm;
haar case en IR, afgespeeld met de runners van deze branch: `oracle:
expected fault observed at load (…)` rc 0, clingo idem.

Rood-eerst, de hele set nieuwe en bijgestelde getuigen tegen de lader van
vóór deze stap (`74b40103`), suite van na de stap:

```
FAIL caselader-lexeem-constructorpayload-datum (got: V1.status "10-03-2026" is not the uchar-case-v0 date lexeme yyyy-mm-dd (malformed date lexeme "10-03-2026" (expected ISO yyyy-mm-dd)))
FAIL caselader-vorm-wortel-geen-object (got: expected an object)
FAIL caselader-vorm-format-ontbreekt (got: missing field "format")
FAIL caselader-vorm-format-geen-tekst (got: expected a string)
FAIL caselader-vorm-asofdate-ontbreekt (got: missing field "asOfDate")
FAIL caselader-vorm-asofdate-geen-tekst (got: malformed asOfDate)
FAIL caselader-vorm-input-geen-object (got: expected an object)
FAIL caselader-vorm-input-ontbreekt (got: missing field "input")
FAIL caselader-vorm-instances-geen-lijst (got: instances: expected a list)
FAIL caselader-vorm-instances-ontbreekt (got: missing field "instances")
FAIL caselader-vorm-parameters-geen-object (got: expected an object)
FAIL caselader-vorm-parameters-ontbreekt (got: missing field "parameters")
FAIL caselader-vorm-expected-geen-lijst (got: expected: expected a list)
FAIL caselader-vorm-expected-ontbreekt (got: missing field "expected")
FAIL caselader-vorm-expectedfault-geen-object (got: expected an object)
FAIL caselader-vorm-expectedfault-veld-ontbreekt (got: missing field "clingo")
FAIL caselader-vorm-expectedfault-geen-tekst (got: expected a string)
FAIL caselader-vorm-instantie-geen-object (got: expected an object)
FAIL caselader-vorm-label-ontbreekt (got: missing field "label")
FAIL caselader-vorm-label-geen-tekst (got: expected a string)
FAIL caselader-vorm-objecttype-geen-tekst (got: expected a string)
FAIL caselader-vorm-attributes-ontbreekt (got: missing field "attributes")
FAIL caselader-vorm-attributes-geen-object (got: expected an object)
FAIL caselader-vorm-characteristics-ontbreekt (got: missing field "characteristics")
FAIL caselader-vorm-characteristics-geen-lijst (got: characteristics: expected a list)
FAIL caselader-vorm-kenmerk-geen-tekst (got: expected a string)
FAIL caselader-vorm-roles-ontbreekt (got: missing field "roles")
FAIL caselader-vorm-roles-geen-object (got: expected an object)
FAIL caselader-vorm-rol-geen-lijst (got: role "melders": expected a list of labels)
FAIL caselader-vorm-rollabel-geen-tekst (got: expected a string)
FAIL caselader-vorm-kenmerktijdlijnen-geen-object (got: expected an object)
FAIL caselader-vorm-perioden-geen-lijst (got: V1.dienstdoend: expected a list of periods)
FAIL caselader-vorm-periode-geen-object (got: expected an object)
FAIL caselader-vorm-periodegrens-ontbreekt (got: missing field "to")
FAIL caselader-vorm-periodegrens-geen-tekst (got: expected a string)
FAIL caselader-vorm-waarde-geen-object (got: expected an object)
FAIL caselader-vorm-datum-geen-tekst (got: expected a string)
FAIL caselader-vorm-getal-geen-tekst (got: expected a string)
FAIL caselader-vorm-datumtijd-geen-tekst (got: expected a string)
FAIL caselader-vorm-waarde-meerdere-varianten (got: V1.vondstdag: malformed value)
FAIL caselader-vorm-boolean-geen-boolean (got: V1.gemeld: malformed boolean)
FAIL caselader-vorm-lijst-geen-lijst (got: V1.vangstdagen: a list value's "list" field is not an array)
FAIL caselader-vorm-struct-geen-object (got: V1.dossier: a record value's "struct" field is not an object)
FAIL caselader-vorm-payload-geen-object (got: expected an object)
FAIL caselader-vorm-tijdlijn-geen-lijst (got: V1.tarief: malformed timeline value)
FAIL caselader-vorm-constructor-geen-tekst (got: expected a string)
FAIL caselader-vorm-lijstelement-geen-tekst (got: expected a string)
FAIL caselader-vorm-structveld-geen-tekst (got: expected a string)
FAIL caselader-vorm-segment-geen-object (got: expected an object)
FAIL caselader-vorm-segmentwaarde-ontbreekt (got: missing field "value")
FAIL caselader-vorm-segmentgrens-geen-tekst (got: expected a string)
FAIL caselader-vorm-parameterwaarde-geen-object (got: expected an object)
FAIL caselader-vorm-parameterdatum-geen-tekst (got: expected a string)
FAIL caselader-vorm-verwachting-geen-object (got: expected an object)
FAIL caselader-vorm-kind-ontbreekt (got: missing field "kind")
FAIL caselader-vorm-kind-geen-tekst (got: expected a string)
FAIL caselader-vorm-verwacht-label-ontbreekt (got: missing field "label")
FAIL caselader-vorm-verwacht-label-geen-tekst (got: expected a string)
FAIL caselader-vorm-verwacht-attribuut-geen-tekst (got: expected a string)
FAIL caselader-vorm-verwachte-waarde-ontbreekt (got: missing field "value")
FAIL caselader-vorm-verwachte-waarde-geen-object (got: expected an object)
FAIL caselader-vorm-verwachte-tijdlijn-ontbreekt (got: missing field "timeline")
FAIL caselader-vorm-verwachte-tijdlijn-geen-lijst (got: expected V1.tarief: malformed timeline value)
FAIL caselader-vorm-kenmerk-ontbreekt (got: missing field "characteristic")
FAIL caselader-vorm-regel-geen-tekst (got: expected a string)
FAIL caselader-positie-onbekende-soort (got: unknown expectation kind "consistentt")
FAIL caselader-positie-labelpin-zonder-regel (got: an inconsistent label pin requires a rule pin)
FAIL caselader-positie-naar-een-rol-twee-labels (got: to-one role "vondst" carries 2 labels)
68 failure(s)
```

Mutaties (elk één keten met bouwcontrole, herstel `cmp identiek`, daarna
`test_ref: all 914 worked examples pass` en `test_clingo: 523 checks, 0
failures`): MV1 `obj` zonder positie `16 failure(s)` (`got: expected an
object, got []`); MV2 `str_field` op de ouder `9` (`got: <root>: expected
a string, got 0`); MV3 `field` zonder positie `17` (`got: missing field
"format"`); MV4 instantie blijft op het arraypad `1` (`got:
input.instances[0].objectType: …`); MV5 verwachtingsindex één te hoog `12`
(`got: expected[5]: …`); MV6 niet-string-asOfDate terug `1` (`got:
malformed asOfDate`); MV7 `list_` zonder vondst `8`; MV8 variantsleutel uit
de positie `6` (`got: V1.vondstdag: expected a string, got 20260310`); MV9
payload op de constructorpositie `2`; MV10 tijdlijnlijst op het attribuut
`2`; MV11a/b/c de drie kruiscontroles zonder positie, elk `1`.

**Eerste extra item: het commentaar bij `periode_onder_telkwantor`**
(commit `41ae5a22`, alleen commentaar, regels identiek aan basis
`8dd05e25` en daar geverifieerd). De meting van 2026-09-20 (de oude
DRL-weigerzin) en die van de vouw op 2026-09-23 heten nu metingen van vóór
de kaart drools-venster-vouwtoets, met de nieuwe stand erbij: beide
Drools-takken spelen het venster onder ALLE en weigeren het bij naam onder
GEEN en de tellende kwantoren, met WF-112, in samenstelling en vouw.

**Tweede extra item: WF-022 in ref** (commit `287cecaf`). Een platte
KENMERKTOEKENNING met een andere waarde dan de constante `waar` weigert nu
vóór er een onderwerp genomen wordt, met de test van de verifier zelf (het
literal `waar` getypt Booleaan):

```
rule "geen herleving" attributes the characteristic "herleefd" of "Recht" with the literal "onwaar": a characteristic target takes the boolean constant waar and nothing else, the position WF-022 refuses (a negative decision on a characteristic exists only inside a defaults group). Declined at evaluation
```

Het negatief `corpus/negatief/wf022-kenmerk-onwaar` (van main, nog niet op
deze branch), buiten de verifier om afgespeeld met een case (R1 met `nieuw
recht ontstaan`, R2 zonder): main ref `oracle: PASS (3 expectations)`
(stil, kenmerk afwezig), clingo `TRANSLATION FAULT: characteristic
attribution with a non-waar value`; deze branch ref `EVALUATION FAULT:
rule "geen herleving" attributes …` rc 1. Rood-eerst in de suite (de
inline nagebouwde versie van dat document): `FAIL
WF-022-kenmerk-onwaar-weigert (no fault at all; herleefd on R1: false, on
R2: false)` en `FAIL WF-022-kenmerk-berekend-weigert (no fault at all;
herleefd on R1: true, on R2: false)`; de controle met `waar` groen.
Mutaties: MW1 controle weg `2 failure(s)` (`herleefd on R1: true` — met de
vereenvoudigde arm zou `onwaar` het kenmerk dan zelfs zetten); MW2
controle aanvaardt elk boolean-literal `1 failure(s)`; herstel `cmp
identiek`. De evaluatiearmen voor `onwaar` en een niet-boolean waarde
werden door de controle onbereikbaar en zijn weg. Geen golden (99
kenmerktoekenningen), rood (8) of toka-document (14) draagt de vorm, en
van de negatieven alleen `wf022-kenmerk-onwaar` op main, dat hem juist
moet dragen (gate 3 weigert het); sweep ongewijzigd.

### Fixronde 2 (lens-adviezen, 2026-09-23; oordeel MERGEN)

1. **Overlevende tellermutant** (commit `1f34aa5b`). Getuigen `1.5/2` en
   `-/3` op `V1.gewicht`; tegen de lensmutant (`digits num && …` → `ignore
   num; …`): `FAIL caselader-lexeem-attribuut-getal-teller-decimaal
   (escaped the loader as Value.Fault: malformed number lexeme "1.5/2")`,
   idem `"-/3"`, `2 failure(s)`; herstel `cmp identiek`, 919 groen.
2. **Gate 4 en de afsluitende newline** (commit `a118fdc4`, BESCHERMD-PAD-
   VOORSTEL in `scripts/check-case-schema.sh`; commentaren in `3e402fe1`).
   Gemeten met vier tijdelijke cases naast `jutterswet-bergingsklok`
   (niet gecommit, na de meting verwijderd): met het oude script `PASS
   (schema valid; 643 case file(s) validate)`, terwijl de lader elk
   weigerde (`CASE ERROR: P4.meldingsdatum "2026-03-10\n" is not …`,
   `… "5\n" …`, `… "1/2\n" …`, `asOfDate "2026-06-01\n" is not …`). Met het
   voorstel (een `pattern`-keyword dat `re.fullmatch` toetst): rc 1 met
   `'2026-06-01\n' does not match '^[0-9]{4}-[0-9]{2}-[0-9]{2}$'`, `'1/2\n'
   does not match …`, `'2026-03-10\n' …`, `'5\n' …`; na verwijderen `PASS
   (schema valid; 639 case file(s) validate)`. De testkop en het
   commentaar van het getalhek zeggen nu wat onder beide lezingen waar is.
   Gate 2 heeft hetzelfde gat: open punt 3.
3. **Registerzin** (commit `560acca6`): versmald, zie boven.
4. **Roosterweigering** (commit `9ee362f5`). De lader vraagt het rooster
   zelf af en noemt het, als v0-grens: `V1.tarief: the declared grid is
   DAG, which every day boundary sits on, yet this loader checks a
   timeline's grid through the calendar bucketing and v0 defines buckets
   for month and year only (RUNTIME-SEMANTICS ch. 3.2, OPEN in ch. 3.12):
   refused as a v0 limit, not as a defect of the case`; voor WEEK en
   KWARTAAL `V1.tarief: the declared grid is WEEK, and v0 defines calendar
   buckets for month and year only (…), so this loader has no WEEK grid to
   check the segment boundaries against: refused as a v0 limit, not as a
   defect of the case`. Eerlijk gezegd: een DAG-tijdlijn ligt altijd op
   zijn rooster en wordt toch geweigerd, omdat de controle via de
   kalenderindeling loopt; dat is de bekende v0-grens van RUNTIME-SEMANTICS
   ch. 3.2 ("Bucketing by day/week/quarter: absent (mirrored); OPEN"), geen
   gedragswijziging. Getuigen DAG, WEEK, KWARTAAL; MG1 (DAG weer via de
   indeling) `1 failure(s)` (`escaped … bucketing is defined for month and
   year only (ch. 3.2)`), MG2 (roosternaam vast op WEEK) `1 failure(s)`;
   herstel `cmp identiek`, 921 groen.
5. **Payload-achtervoegsel**: al gedaan in fixronde 1 (`df683743`,
   `V1.status.payload`), vóór het advies kwam; getuigen
   `caselader-lexeem-constructorpayload-datum` en
   `caselader-vorm-payload-geen-object`, mutant MV9. Geen aparte commit.
6. **May-touch** (commit `ab3e2840`): de paragraaf klopt nu met de branch,
   met de samenloop en de gate-5-afspraak.

**Gates na fixronde 2** (elk los, na `eval $(opam env)`, op de codetip
`9ee362f5`; daarna alleen kaartcommits): 1 PASS; 2 PASS (279 bestanden,
floor 192); 3 PASS (279 schoon / 440 negatief, floor 350); 4 PASS (639
cases); 5 PASS (**ref-suite 921**, floor 489; 595 cases, floor 427); 6
PASS (523 checks, 595 cases; uitvoer byte-identiek aan main); 7 PASS (559
+ 36 overlevers); 8 PASS (44 rood, floor 26; 88 fragmenten, geen bodemzin;
Drools 10 fault-pinned, 34 not-carried); 9 PASS (201 checks, 12
goldens); 11 PASS (ref 29, clingo 29, Drools 29 PLAYED); 13 PASS (131
twists over 27 goldens, 128 replays, 25 DRL-played, 0 overlevers); 14 PASS
(130 checks, 2 goldens). Gates 10 en 12 raken geen ref-code en liepen
laatst op `66a4c27c` groen (409 tests / 70 goldens; played 182, DRL 161,
JUnit 620). Sweep ref en clingo over 639 cases op `9ee362f5`: tegenover
main verschilt alleen `inputdatum-kalender` (main rc 1 evaluatiefout, hier
rc 0 fault bij het laden); asOfDate-lexeemzin `cmp` identiek met main.

**Nog niet gerebased** (aanwijzing). Main is intussen verder (o.a. de
rood-duo-kaart met `corpus/rood/asofdate-kalender` en het negatief
`wf022-kenmerk-onwaar`), maar raakte geen bestand van deze branch aan
(`ref/`, `scripts/check-case-schema.sh`, `corpus/rood/inputdatum-kalender`,
deze kaart). Na de rebase telt gate 8 één rood document meer dan main.

### Fixronde 3 (hoofdsessie, 2026-09-23)

**Open punt 3: gate 2 en de afsluitende newline** (commit `98884c2e`,
BESCHERMD-PAD-VOORSTEL in `scripts/check-ir-schema.sh`). Hetzelfde
voorstel als voor gate 4: de corpuscontrole en de patroonzelftest van
gate 2 gebruiken een validator die `pattern` met `re.fullmatch` toetst.
Rood-eerst met een tijdelijk document (`corpus/golden/zz-tmp-…/ir.json`,
`jutterswet-vensterheffing` met `"vanaf": "2026-01-01\n"`; niet
gecommit, na de meting verwijderd): het oude script `PASS (schema valid;
24 pattern self-test row(s); 280 file(s) validate and round-trip, floor
192)`, terwijl de verifier het document weigert (`WF-101 FAIL
/resolvedRegels/0/geldigheid/vanaf: validity bound '2026-01-01\n' is not
the ISO form yyyy-mm-dd …`). Met het voorstel: `FAIL:
corpus/golden/zz-tmp-geldigheid-newline/ir.json`, `at:
resolvedRegels/0/geldigheid/vanaf`, `'2026-01-01\n' does not match
'^[0-9]{4}-[0-9]{2}-[0-9]{2}$'`, `check-ir-schema: FAIL (1 of 280 file(s)
bad)`; zonder het tijdelijke document `PASS (schema valid; 26 pattern
self-test row(s); 279 file(s) validate and round-trip, floor 192)`.

De zelftesttabel kreeg een rij `"2026-01-01\n"` (moet weigeren), zodat de
volle match een vaste getuige heeft. Die rij eerst alleen, onder de
standaardvalidator: `FAIL: pattern self-test, Geldigheid.vanaf =
'2026-01-01\n': schema accepts it, expected refuse`, idem `tot`,
`check-ir-schema: FAIL (2 pattern self-test row(s) bad)`. De gates 9 en
14 voeren het serialiserende deel van dit script uit; dat deel is
onveranderd en beide bleven groen.

Gates na fixronde 3 (elk los, op `98884c2e`; daarna alleen deze
kaartcommit): 1 PASS; 2 PASS (26 zelftestrijen, 279 bestanden, floor
192); 3 PASS (279 schoon / 440 negatief, floor 350); 9 PASS (201 checks,
12 goldens); 14 PASS (130 checks, 2 goldens). Fixronde 3 raakte geen
code onder `ref/`; de uitslagen van de gates 4 tot en met 8, 11 en 13 op
`9ee362f5` blijven gelden (suite 921).

### Fixronde 4 (hoofdsessie, 2026-09-23)

**Open punt 3: een vaste getuige voor het gate-4-voorstel** (commit
`b76af45b`, BESCHERMD-PAD-VOORSTEL in `scripts/check-case-schema.sh`). Naar
het model van gate 2 houdt gate 4 nu een patroonzelftest: een klem op het
patroonoppervlak van het case-schema (de tien posities: asOfDate, de
periodegrenzen van kenmerktijdlijnen, de waardevarianten number, date en
dateTime, de segmentgrenzen van waarde- en verwachte tijdlijnen) en per
soort een lexeemtabel in beide richtingen — datum 9 rijen (o.a.
`"2026-06-01"` aanvaarden, `"2026-02-30"` aanvaarden als laaggrens, die de
caselezers weigeren; `"2026-06-01\n"`, dag-eerst, spatie, teken, leeg
weigeren), datumtijd 8 (o.a. uur 24 aanvaarden als laaggrens,
`"2026-03-15T12:00:00\n"` weigeren), getal 12 (o.a. `5`, `-12.50`, `3/4`
aanvaarden; `"5\n"`, `1,000`, `.5`, `1E3`, `+5`, `1/0`, `3/04`, `1.5/2`
weigeren). Elke positie wordt via `$ref` door DEZELFDE validator getoetst
als de corpuscontrole; samen 92 zelftestrijen, en de PASS-regel noemt ze
zoals gate 2 doet: `check-case-schema: PASS (schema valid; 92 pattern
self-test row(s); 639 case file(s) validate)`.

Rood-eerst onder de oude `re.search`-validator (de volle match eruit,
daarna teruggezet, `cmp identiek`): `check-case-schema: FAIL (10 pattern
self-test row(s) bad)`, precies de tien newline-rijen, één per positie,
bijvoorbeeld `FAIL: pattern self-test, /properties/asOfDate/oneOf/0 =
'2026-06-01\n': schema accepts it, expected refuse` en `FAIL: pattern
self-test, /$defs/value/oneOf/0/properties/number = '5\n': schema accepts
it, expected refuse`. Ook een verruimd patroon valt: met het getalpatroon
tijdelijk op `^-?[0-9]+([.,][0-9]+)?$|…` (niet gecommit, schema daarna met
`cmp` hersteld) `FAIL: pattern self-test,
/$defs/value/oneOf/0/properties/number = '1,000': schema accepts it,
expected refuse`. Het commentaar in het gate-2-script dat de zelftest
voor de case-patronen nog als eigen kaart noemde, zegt nu dat gate 4 ze
sinds 2026-09-23 zelf houdt.

Gates na fixronde 4 (elk los, op `b76af45b`; daarna alleen deze
kaartcommit): 1 PASS; 4 PASS (92 zelftestrijen, 639 cases); 2 PASS (26
zelftestrijen, 279 bestanden; alleen een commentaarregel veranderd).

### Fixronde 5 (lensronde 2, oordeel MERGEN; laatste ronde vóór de merge)

1. **WF-022-teksten** (commit `1240a2f4`). ref toetst de waardeclausule op
   de soort, de verifier op een kenmerkdoel. Nagemeten (lensmeting V7):
   `jutterswet-penningdrempel` met een KENMERKTOEKENNING op het attribuut
   `aantal aangemelde vondsten`, getypeerd als dat attribuut, waarde
   `onwaar`: verifier `PASS`, main-ref `oracle: PASS (3 expectations)`
   (stil), clingo `TRANSLATION FAULT: characteristic attribution with a
   non-waar value`, branch-ref een weigering die het doel "the
   characteristic" noemde en de positie "the position WF-022 refuses". De
   zin, het commentaar in `ref/lib/eval.ml` en de registerzin zeggen nu
   dat ref de clausule voor elke KENMERKTOEKENNING houdt en de verifier
   alleen bij een kenmerkdoel; de zin noemt ook het type van de waarde:

   ```
   rule "aantal als kenmerk" is a KENMERKTOEKENNING on "aantal aangemelde vondsten" of "Jutter" with the literal "onwaar" typed Booleaan: a KENMERKTOEKENNING writes the boolean constant waar and nothing else (WF-022's value clause; this interpreter holds it for every KENMERKTOEKENNING, the verifier only where the target is a declared characteristic). Declined at evaluation
   ```

   Rood-eerst: de drie bestaande WF-022-getuigen met de nieuwe zin en een
   nieuwe getuige voor de V7-vorm vielen onder de oude zin (`3 failure(s)`),
   daarna groen. V8 en V11 staan als open punten 5 en 6.
2. **Beide helften van de constante** (commit `edf69873`). Getuigen
   `waar` getypt Tekst en `Waar` getypt Booleaan (beide weigeren). Onder
   MW3 (typehelft weg): `FAIL WF-022-kenmerk-waar-als-tekst-weigert (no
   fault at all; herleefd on R1: true, on R2: false)`, `1 failure(s)`; onder
   MW4 (lexeem hoofdletterongevoelig): `FAIL
   WF-022-kenmerk-hoofdletter-weigert (no fault at all; herleefd on R1:
   true, on R2: false)`, `1 failure(s)`; herstel `cmp identiek`.
3. **Fullmatch-commentaar** (commit `e6c3b413`, voorstel in beide
   scripts). Nagemeten met node: `^a$\n?` op `"a\n"`, `^\d+$` op
   U+0663 en `^\w+$` op `é` matchen in Python heel en falen in ECMA-262;
   voor de drie gedragen patronen geen verschil op 1437 paren (beide
   zelftesttabellen en elk corpuslexeem van deze soorten; de lens telde
   1449 op haar set). Het commentaar beperkt zich daartoe en zegt dat de
   zelftests een overstap van `[0-9]` naar `\d` niet zien (open punt 7).
4. **Vondst afgekapt** (commit `f30ca136`). `got …` houdt op na 80 bytes,
   op een tekengrens, met `…`: `<root>: expected an object, got
   [1,2,3,…,28,29,3…` in plaats van de hele lijst. Getuigen: de lijst van
   veertig getallen en een reeks van zestig é's (afgekapt na negenendertig
   hele é's). MK1 (geen afkapping) `2 failure(s)`, MK2 (zonder tekengrens)
   `1 failure(s)` met een half teken in de zin; herstel `cmp identiek`.
5. **Leeg instantielabel** (commit `2e82ef54`). `input.instances[0].label:
   expected a non-empty string, got ""`; rood-eerst `FAIL
   caselader-vorm-label-leeg (loaded)`, mutant ML1 idem, herstel `cmp
   identiek`.

**Rebase** op main `a1b9daf0`: schoon (23 commits), main raakte geen
bestand van deze branch; alle hashes in dit Result omgezet. Op de nieuwe
basis, elk los, op de codetip `2e82ef54`: gate 1 PASS; 2 PASS (26
zelftestrijen, 283 bestanden, floor 192); 3 PASS (283 schoon / 441
negatief); 4 PASS (92 zelftestrijen, 643 cases); 5 PASS (**ref-suite
927**, 595 cases); 6 PASS (523 checks, 595 cases); 8 PASS (**48 rood**, 96
fragmenten zonder bodemzin; Drools-been **11 fault-pinned**, 37
not-carried — mains rood-duo telt dit document mee als FAULT met eigen
zin); 9 PASS (201 checks, 12 goldens); 11 PASS (ref 29, clingo 29, Drools
29); 14 PASS (130 checks, 2 goldens). Sweep van ref en clingo over alle
643 cases, tegen de runners van main (ref en clingo zijn op main sinds
`8dd05e25` onveranderd): alleen `inputdatum-kalender` verschilt, zoals
bedoeld. De asOfDate-lexeemzin: `cmp` identiek met main.

**Open punten** (stand na fixronde 5; de punten 3 en 4 van de eerste
ronde zijn in fixronde 1 opgelost, het gate-2-punt in fixronde 3, de
getuige voor gate 4 in fixronde 4).

1. Drools-caselezer (`backends/drools`, andere zone): de getallezer
   (`new BigDecimal` in `waardeNaarArg`) aanvaardt `.5`, `1E3` en `+5`,
   die het case-schema, ref en clingo weigeren (gemeten `PLAYED 1`), en
   weigert `1,000`/`12,5` als kale `NumberFormatException` zonder veld,
   waar zijn datum- en datumtijdklemmen veld en vorm noemen. Het
   huispatroon is een klem met het schemapatroon, zoals
   `datumVanCaseWire`. Zijn datumweigering noemt het attribuut maar niet
   het instantielabel.
2. Clingo `backends/clingo/lib/translate.ml` (andere zone; de
   hoofdsessie geeft dit door aan de clingo-kaart): dode armen van
   dezelfde klasse als de hier verwijderde eval-arm — r. 1904 `malformed
   date literal %S`, r. 3746 en r. 5839 `malformed asOfDate %S`, en de
   stille `| _ -> ()` op r. 4764 achter `datetime_of_lexeme`. Daarnaast
   toetst zijn WF-022-arm (r. 7334) alleen het lexeem `waar` en niet het
   type Booleaan dat de verifier eist; een `waar` getypt als Tekst gaat
   daar door waar ref (sinds fixronde 1) en de verifier weigeren.
3. Opgelost in fixronde 4 (`b76af45b`): het gate-4-voorstel heeft nu een
   vaste getuige in de patroonzelftest van gate 4.
4. Bewust ongepositioneerd gelaten: drie weigeringen op de wortel die geen
   plaats nodig hebben omdat de wortel uniek is en de zin het veld noemt
   (`unsupported case format "…"`, `a case carries exactly one of
   expected / expectedFault`, `empty expectation list`), en de
   kruiscontroles die hun plaats al met namen dragen (`attribute "…" is
   not declared on "…"`, `role key "…" is ambiguous from "…"`,
   `expectation references unknown label "…"` en verwanten).
5. Verifier-lijn (lensmeting V8, bestond al op main): WF-022 koppelt de
   soort KENMERKTOEKENNING niet aan een kenmerkdoel. Op
   `jutterswet-penningdrempel` met een KENMERKTOEKENNING op het attribuut
   `aantal aangemelde vondsten` (getypeerd als dat attribuut) en de waarde
   `waar`: verifier PASS, en ref en clingo schrijven stil een kenmerk met
   de naam van dat numerieke attribuut (`oracle: PASS`, `clingo: PASS`); met
   `onwaar` (V7) weigeren ref en clingo sinds deze kaart wel. Een verifierregel
   die de soort aan een gedeclareerd kenmerk als doel bindt, sluit beide.
6. Kanttekening (lensmeting V11): de WF-022-controle van ref staat achter de
   geldigheidspoort (`applies` komt eerst in `run_rule`), dus een
   KENMERKTOEKENNING buiten haar venster weigert niet; clingo idem.
7. De patroonzelftests van gate 2 en gate 4 dragen geen niet-ASCII-cijfer;
   wie een patroon van `[0-9]` naar `\d` omzet, merkt het aan geen gate
   (Python leest `\d` Unicode-breed, ECMA-262 niet). Een rij met een
   Arabisch-Indisch cijfer die geweigerd moet worden, zou het zien; op
   aanwijzing (lensadvies 3) alleen genoteerd.
