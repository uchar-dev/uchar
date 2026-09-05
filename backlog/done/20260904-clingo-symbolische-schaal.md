# clingo crasht op een symbolische term in de schaal-deling — nette fault

Goal: de gelede EERSTE/LAATSTE over een SumType-element laat clingo
hard crashen: `Fatal error: exception Division_by_zero`, exitcode 2,
zonder file/line — geen fault, een crash. Gemeten (meetfase
doorgeeffold, 2026-09-04): `backends/clingo/lib/translate.ml` deelt in
de gelede-fold-arm (~r. 1485) `m / s` waar `s = 0`, de schaal die
`expr_term` (~r. 114-120) aan een constructorterm geeft; de
`is_symbolic_operand`-fence op dezelfde arm (~r. 1456) dekt Tekst,
Booleaan en Enumeratie maar niet SumType. Fail-fast betekent een
reden-dragende `TRANSLATION FAULT`, nooit een ongevangen exceptie: de
fence leert SumType kennen (of de schaal-nul wordt zelf de luide
weigering), en de kaart loopt na of méér plekken een symbolische term
door een numerieke schaal-deling laten lopen (grep op de
schaal-delingen in translate.ml; elke vondst zelfde behandeling of een
reden in commentaar).

Onder het orderbaarheidsbesluit van 2026-09-04
(`20260902-doorgeeffold-orderbaarheid.md`) wordt de dragende vorm
ill-formed, maar een crash-pad hoort ook onbereikbaar-net te zijn: de
translator mag op geen enkele invoer een ongevangen exceptie gooien.

Acceptance:
- Rood-eerst: de crash gereproduceerd (letterlijke uitvoer in de
  kaart); ná de fix een reden-dragende TRANSLATION FAULT op dezelfde
  invoer.
- De sweep over de schaal-delingen met per plek fence of reden.
- Alle dertien gates groen; geen bestaande pin beweegt (gemeten
  verwachting: geen corpusdocument raakt de arm).

May touch: `backends/clingo/lib/**`, `backlog/`.

Out of scope: de orderbaarheidsclausule zelf (eigen kaart), ref,
frontends, corpus.

---

Result (2026-09-05): **geland.** De crash is een reden-dragende
TRANSLATION FAULT, de snede zit op de schaal in plaats van op de
annotatie, en de sweep dekt elke OCaml-schaal-deling in `translate.ml`.
Alle dertien poorten groen, geen enkele bestaande telling beweegt.

**Nagelezen en bijgesteld in de fixronde van dezelfde dag** — de sweep
bleek twee ASP-geschreven delers te hebben overgeslagen en drie stukken
tekst hierboven beloofden meer dan er stond. Zie **Fixronde** onderaan;
de correcties zijn in de betreffende secties zelf verwerkt.

## De reproductie, letterlijk

Het dragende document staat al in het corpus als negatief:
`corpus/negatief/wf038-eerste-somtype-geleed/ir.json` — de gelede
`EERSTE` over twee operanden van het somtype `stand`. Met een casus
ernaast (buiten de repo gehouden; de negatief-zone draagt er geen):

    $ clingo_runner corpus/negatief/wf038-eerste-somtype-geleed/ir.json case.json
    Fatal error: exception Division_by_zero
    EXITCODE=2

Twee feiten, allebei genoteerd zoals de kaart vroeg:

1. **De translator crashte.** Geen fault, geen regel, geen file/line —
   een ongevangen exceptie en exit 2.
2. **Het document is ill-formed.** Sinds de WF-038-ordeningsclausule
   (geland 2026-09-05) wijst de verifier precies deze vorm af:

       WF-038 FAIL /resolvedRegels/0/waarde/argumenten/0: operand of the
       EERSTE ordering fold is sumtype, must be a number or a date
       WF-038 FAIL /resolvedRegels/0/waarde/argumenten/1: operand of the
       EERSTE ordering fold is sumtype, must be a number or a date
       EXITCODE=1

   De kaart blijft daarmee overeind in plaats van te vervallen: de
   meting gaat over de translator, niet over de verifier. Een
   translator moet op élke invoer een reden geven, ook op invoer die
   buiten de verifier om binnenkomt.

Dezelfde aanroep ná de fix:

    TRANSLATION FAULT: a symbolic operand reaches an eerste/laatste fold:
    a value with no numeric scale (today a sum-type or record value) cannot
    be harmonised against one (ref faults the operation; this arm refuses it
    rather than divide by the absent scale)
    EXITCODE=1

(De melding is in de fixronde verbreed; zie daar. De vorm van de weigering
— reden-dragend, exitcode 1 — is dezelfde.)

## De snede: de schaal, niet de annotatie

`numeric_scale ~what s` (`translate.ml`, naast `value_scale`) faalt luid
op `s < 1`. Schaal 0 markeert een SYMBOLISCHE term — een
constructorterm, een recordterm, en elk toekomstig datatype waaraan
`value_scale` geen numerieke schaal geeft. Zo'n term *dragen* is
legitiem (een vouw-accumulator, een sum-getypeerd doel, een
gelijkheidsjoin); hem *harmoniseren* is het niet, en juist daar wordt
door de schaal gedeeld.

Waarom niet de fence uitbreiden met SumType, de andere optie uit de
kaart: `is_symbolic_operand` en `numeric_ok` lezen `typ_of_expr`, dus de
wire-annotatie, terwijl de schaal uit de DECLARATIE komt
(`RUNTIME-SEMANTICS` §2.10 — een consument leidt af uit declaraties, wat
poort 13 mechanisch afdwingt). Een ontbrekende annotatie (`None` glipt
door `numeric_ok`) of een liegende annotatie loopt de annotatie-hekken
straal voorbij en tot in de deling. Een hek op de schaal kan dat niet,
en het dekt bovendien het symbolische type dat er nog niet is: dat faalt
op de dag dat het wordt toegevoegd, niet op de dag dat het crasht.

## De sweep — plek → verdict

Alle OCaml-delingen en -moduli in `translate.ml` nagelopen met commentaar
en strings weggestreept; 24 rekenplekken. Daarnaast de plekken die de deler
in de ASP-TEKST schrijven — die deelt clingo zelf. **De eerste ronde deed
die groep af met één zin: "clingo deelt zelf, met een `viol/2`-hek ervoor".
Dat gold niet voor alle.** Een ASP-deling door een VARIABELE draagt inderdaad
haar `!= 0`-hek (deling, verdeling, tijdlijnbreuk); een ASP-deling door een
CONSTANTE die uit een schaal komt, draagt er geen — er valt niets te toetsen
op de wire, de constante staat er al in. Twee zulke plekken bestonden: de
datumrekenkunde-duur en haar datum-tijd-zuster. Ze zijn in de fixronde
gehekt; de tabel gaat daarmee van 16 naar 18 rijen (de tellingen "16 gehekt"
in de landings-commit van de eerste ronde zijn in zoverre achterhaald).

**Gehekt** (`numeric_scale`, op de plek waar de schaal binnenkomt):

| plek | arm |
| --- | --- |
| `harmonised` | zelf-hekkend, voor een aanroeper die het vergeet |
| gelede `eerste`/`laatste` | **de gemeten crash** |
| gedurende-clip-vergelijking | de `grens`-operand |
| `plus`/`min`/`verminderd met` | beide operanden |
| `Afgerond` | de basis (schaal is er óók modulus) |
| `Begrensd` | basis + beide grenzen |
| `translate_division` | deeltal + deler (worden gcd-operanden) |
| `translate_power` | de basis |
| numerieke gelijkheid | beide operanden |
| ordeningsvergelijking | beide operanden |
| `body_condition` | beide operanden (closed body) |
| closed-body `plus`/`min` | beide operanden |
| closed-body macht | de basis |
| `closed_division` | deeltal + deler |
| `value_condition` | beide operanden |
| `fitted` | de waardeschaal, ná de identiteitstak |
| datumrekenkunde-duur | de duur-operand — **fixronde**, deler in de ASP-tekst |
| datum-tijd-duur | idem, de millisecondenzuster — **fixronde** |

**Reden in commentaar** (een symbolische term kan er aantoonbaar niet
binnenlopen):

| plek | reden |
| --- | --- |
| `const_div_reduce` | beide aanroepers hekken `sl`/`sr`; `const_div` laat alleen `c > 0` toe, dus nooit `gcd 0 0` |
| `pow_le_max` | deelt door de basis-WAARDE, geen schaal; `k = 0` apart afgevangen |
| pro-rata `kn`/`kd` | `pow10 (attr_decimals …)`, en `attr_decimals` fault al op een sum-attribuut |
| verdeling-opschaling, uniciteitssleutel-opschaling | idem `pow10`, geen expressieschaal |
| sub-dag-factor `u`, `ms_per_day` | compile-time constanten |

Drie van de gehekte plekken vergelijken twee symbolische operanden. De
eerste ronde noemde er twee **stil** en zette de ordeningsvergelijking
erbij; dat laatste klopte niet. Nagemeten met het hek er per plek uit:

| plek | zonder hek | ref |
| --- | --- | --- |
| closed-body-vergelijking | `clingo: PASS`, exit 0 — **stil** | `unordered comparison: Vervuld vs Melding` |
| waardepositie-`IfThenElse` | `boete = 900,00`, `clingo: PASS`, exit 0 — **stil** | `unordered comparison: Openstaand(30) vs Vervuld` |
| ordeningsvergelijking | `Fatal error: exception Division_by_zero`, exit 2 — **CRASH** | `unordered comparison: Vervuld vs Openstaand(30)` |

De twee stille plekken schalen met een helper die bij `s = m` de deling
overslaat, en met twee symbolische operanden is `m = max 0 0 = 0`, dus
`s = m`: er wordt niets gedeeld en clingo vergelijkt constructortermen in
zijn eigen termorde. De ordeningsvergelijking heeft die helper óók, maar
er staat een `range_guard` vóór, en die deelt `max_int31` door `(m / s1)`
— `0 / 0`. Twee stille divergenties dus, plus een derde crash naast de
gemeten vouw-crash.

`fitted` houdt zijn identiteitstak vóór het hek als **boekhouding**, niet
als semantiek. De eerste ronde schreef op dat `schaal = doel = 0` de
legitieme route van een sum-/structwaarde zou zijn; dat kan niet. `doel`
is altijd `pow10 (attr_decimals …)` en dus ≥ 1, en een sum- of
struct-getypeerd DOEL bereikt `fitted` nooit — zijn eigen schrijfarm vangt
het eerder af. Gemeten door het hek bóven de identiteitstak te hijsen:
suite 47/0, poort 6 405/405, niets verandert.

## De getuige, rood eerst

De kaart vroeg te kijken of `faults_with` een crash überhaupt van een
fault onderscheidt. Gemeten: **hij telde een crash nooit als fault** (hij
ving alleen `Uchar_ref.Value.Fault`), maar hij nam wel de hele suite mee
— `Fatal error: exception Division_by_zero`, exit 2, en de telregel
`test_clingo: N checks, M FAILURES` verscheen helemaal niet. Een crash
was dus niet te onderscheiden van een suite die nooit gedraaid had.
`faults_with` heeft er nu een expliciete arm bij, zodat een crash één
genoemde failure is en de suite doortelt:

    rood:  FAIL a sum-typed operand under the articulated ordering fold:
           expected a loud fault, got a CRASH (Division_by_zero)
           test_clingo: 45 checks, 1 FAILURES
    groen: test_clingo: 45 checks, 0 failures

(Die arm dekte één aanroepplek van de acht en is in de fixronde vervangen
door één wrapper om alle acht; de meldregel luidt sindsdien anders.)

De getuige staat in de suite en niet in `corpus/rood`: het dragende
document is WF-038-ill-formed, dus geen enkele geldige route komt bij de
arm — en dat is precies waarom het hek gemeten moet worden op de plek
waar de verifier er níet voor staat.

## De metingen

Suite 44 → 45 checks (additief; het maintainer-akkoord op additieve
suitegroei staat). Alle dertien poorten, gedraaid ná commit:

| poort | uitslag |
| --- | --- |
| 1 self-contained | PASS |
| 2 ir-schema | PASS (176 bestanden valideren en round-trippen) |
| 3 verifier | PASS (176 golden clean; 287 negatief afgewezen op hun eigen regel) |
| 4 case-schema | PASS (427 casusbestanden) |
| 5 oracle | PASS (405 casussen exact herspeeld) |
| 6 clingo | PASS (405 casussen — ref ≡ clingo over het corpus) |
| 7 noop | PASS (369 weerleggen een niets-doener; 36 gepinde overlevers) |
| 8 rood | PASS (22 rode casussen; Drools-been 5 fault-gepind, 17 met reden niet gedragen) |
| 9 catala | PASS (201 checks; 12 goldens byte-identiek) |
| 10 regelspraak | PASS (171 tests; 30 rgs-goldens byte-identiek) |
| 11 toka-pariteit | PASS (29/29 ref, 29/29 clingo, 29 PLAYED Drools) |
| 12 drools | PASS (108 played) |
| 13 reftype-afleiding | PASS (64 annotaties getwist over 13 goldens; 0 overlevers) |

Alle tellingen gelijk aan `main`.

Wie de hekken raakt, is in de fixronde geteld in plaats van geschat —
`numeric_scale` schreef tijdelijk elke passage weg, over het hele golden-
en rode corpus:

| meting | uitslag |
| --- | --- |
| hekpassages, golden + rood | **1915 + 64 → 1941 + 64** (de twee nieuwe hekken dragen 22 + 4, alle in golden) |
| kleinste schaal die een hek passeert | **1** — geen enkel corpusdocument rijdt een 0 een hek in |
| gelede `eerste`/`laatste`-vouw | **precies één golden**: `jutterswet-peildatum`, 24 passages (8 per casus × 3), állemaal `s = 1` |
| `jutterswet-hoogwaterpaar` op diezelfde arm | **nul keer** — het loopt langs de DatumTijd-zusterarm, die twee `dt(D, MS)`-paren lexicografisch ordent en geen schaal harmoniseert |
| de enige niet-1-duurharmonisatie | `jutterswet-vervaltermijn`, **3× `s = 10`**, op de datumrekenkunde-plek — vóór deze ronde de ongehekte |

De eerste ronde schreef hier dat "de twee goldens die de arm raken" niet
bewogen en dat `peildatum` er `s ∈ {1, 100}` doorheen stuurde. Beide
kloppen niet: één golden raakt de arm, met schaal 1. De echte
niet-triviale harmonisatie zat al die tijd op de plek die géén hek had.

## Fixronde (2026-09-05)

Twee lenzen lazen de landing na. Ze bevestigden de snede — het hek hoort
op de SCHAAL en niet op de annotatie — en vonden er samen vier gaten en
een handvol onware zinnen bij. Alles hieronder is opnieuw gemeten.

### De twee ongehekte delers: een STILLE ref ≢ clingo

De sweep keek naar OCaml-delingen. Twee plekken delen niet in OCaml maar
schrijven de deler in de ASP-TEKST: de datumrekenkunde-arm
(`((T)/sn)` na de coalesce van de duur-operand) en haar
datum-tijd-zuster. `day_time_unit_of` en `dt_time_unit_of` halen de
EENHEID uit de wire-annotatie, de SCHAAL komt uit de declaratie — dus een
somtype-attribuut dat op de wire `Numeriek{dag}` (resp. `{uur}`) heet
loopt binnen als duur en grondt als `.../0`.

Gemeten met zulke liegende annotaties, vóór het hek:

    clingo: info: operation undefined: (C4/0)
            clingo: PASS (1 expectations)      EXITCODE=0
    ref:    EVALUATION FAULT: plus: expected a number, got Openstaand(30)
                                              EXITCODE=1

Geen exceptie, geen exit 2, geen fault: **stille ref ≢ clingo** — erger
dan de crash waarmee de kaart begon, want er is niets dat het meldt. De
datum-tijd-zuster gaf hetzelfde beeld (twee `operation undefined`-regels,
dan PASS). Ná het hek beide luid:

    TRANSLATION FAULT: a symbolic operand reaches a date-arithmetic duration: …
    TRANSLATION FAULT: a symbolic operand reaches a datum-tijd duration: …
    EXITCODE=1

Twee getuigen in de suite (rood eerst: **47 checks, 2 FAILURES** zonder
de hekken, **47 checks, 0 failures** ermee). Een onafhankelijke hersweep
over de overige ASP-geschreven delers vond geen derde gat: elke andere is
óf een compile-time constante, óf een deler die zelf een gehekte schaal
is, óf een deling door een VARIABELE met haar `!= 0`-hek ernaast.

### `pow10` hing op een negatief decimalenaantal

`go` telt `n` naar 0 af, dus een negatieve `n` termineert nooit; het
schema legt geen ondergrens op `decimalen`.

    voor:  decimalen -1  → geen uitvoer, na 10 s gekilld (EXITCODE=137)
           decimalen 12  → TRANSLATION FAULT: a scale of 10^12 exceeds
                           clingo's integer range (EXITCODE=1)
    na:    decimalen -1  → TRANSLATION FAULT: a negative decimal count
                           reaches a scale (10^-1) (EXITCODE=1)

Suite en poort 6 ongewijzigd. **Flank, buiten deze kaart:** `ref` crasht
op dezelfde invoer met `Fatal error: exception Invalid_argument("Z.pow:
exponent must be nonnegative")`, exit 2. Eigen-kaart-materiaal — dit is
de clingo-kaart.

### De crash-arm werd een wrapper

De arm in `faults_with` (sectie "De getuige, rood eerst") dekte één
aanroepplek van de acht. Elke `Translate.translate` in de suite loopt nu
door één wrapper die elke niet-`Fault`-exceptie hervangt als
`Fault "a CRASH, not a fault: …"` — geen needle matcht dat, dus een crash
is één GENOEMDE failure, waar hij ook valt. A/B gemeten met het hek op de
gelede vouw tijdelijk uit:

    zonder wrapper: Fatal error: exception Division_by_zero, EXITCODE=2
                    — geen telregel; de suite verdwijnt zonder spoor
    met wrapper:    FAIL a sum-typed operand under the articulated ordering
                    fold faults loudly (a CRASH, not a fault: Division_by_zero)
                    test_clingo: 47 checks, 1 FAILURES, EXITCODE=1

### Wat er niet klopte in de tekst hierboven

- Het **stil/stil-tweetal** was in werkelijkheid stil/stil/**crash**, met
  een andere derde plek: zie de tabel in "De sweep".
- De **`fitted`-reden** was semantisch geformuleerd terwijl hij
  boekhoudkundig is; `doel` is altijd ≥ 1.
- De **afsluitende meetparagraaf** stond op geschatte getallen; hij staat
  nu op getelde.
- De **`viol/2`-afdoening** van de ASP-plekken gold niet voor delers die
  uit een schaal komen.
- De **melding** van `numeric_scale` noemde alleen de twee vormen van
  vandaag, waar het commentaar ernaast elk toekomstig ongeschaald
  datatype belooft. Nu beloven ze hetzelfde.
- De **May-touch-regel** van de kaart staat terug op haar oorspronkelijke
  vorm; de testverruiming staat waar ze hoort, onder Afwijkingen.

Suite 45 → 47 checks (additief), plus de wrapper en één needle die met de
verbrede melding meebeweegt. Poort 6 onveranderd op 405.

### De dertien poorten, ná commit

| poort | uitslag |
| --- | --- |
| 1 self-contained | PASS |
| 2 ir-schema | PASS (176 bestanden valideren en round-trippen) |
| 3 verifier | PASS (176 golden clean; 287 negatief afgewezen op hun eigen regel) |
| 4 case-schema | PASS (427 casusbestanden) |
| 5 oracle | PASS (405 casussen exact herspeeld) |
| 6 clingo | PASS (405 casussen — ref ≡ clingo over het corpus) |
| 7 noop | PASS (369 weerleggen een niets-doener; 36 gepinde overlevers) |
| 8 rood | PASS (22 rode casussen; Drools-been 5 fault-gepind, 17 met reden niet gedragen) |
| 9 catala | PASS (201 checks; 12 goldens byte-identiek) |
| 10 regelspraak | PASS (171 tests; 30 rgs-goldens byte-identiek) |
| 11 toka-pariteit | PASS (29/29 ref, 29/29 clingo, 29 PLAYED Drools) |
| 12 drools | PASS (108 played; JUnit 199 tests) |
| 13 reftype-afleiding | PASS (64 annotaties getwist over 13 goldens; 0 overlevers) |

Elke telling gelijk aan die van de landing — de fixronde beweegt niets
behalve de suite (45 → 47).

## Afwijkingen van de kaart

- **May touch verruimd** met `backends/clingo/test/**` (additief): de
  kaart draagt in haar eigen regel alleen `backends/clingo/lib/**` en
  `backlog/`, terwijl acceptatiepunt 1 en de opdracht om de getuige te
  pinnen er niet zonder kunnen. De contractregel blijft staan zoals hij
  stond; de verruiming is een afwijking, geen werkveld. Onder het staande
  maintainer-akkoord op additieve suitegroei.
- **Gevonden buiten de opdracht:** twee stille divergenties (de
  closed-body-vergelijking en de waardepositie-`IfThenElse` over twee
  symbolische operanden) plus een tweede crash (de
  ordeningsvergelijking). Meegenomen in dezelfde snede.
- **Twee niet-additieve testregels** in de fixronde, beide gemeld: de
  wrapper (acht aanroepplekken hernoemd, de crash-arm in `faults_with`
  vervalt erdoor) en één needle die met de verbrede
  `numeric_scale`-melding meebeweegt. Geen check verandert van uitkomst.
- **Gevonden buiten de opdracht, fixronde:** de twee ongehekte
  ASP-geschreven delers (stille ref ≢ clingo) en de `pow10`-hang op een
  negatief decimalenaantal. De `ref`-kant van die laatste (`Z.pow`)
  blijft liggen als eigen kaart.
