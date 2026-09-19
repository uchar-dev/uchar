# De EERSTE/LAATSTE/AANTAL-setvouw: één uitvoerder draagt hem, de ASP-backend weigert

Goal: WF-096 (`spec/IR-SPEC.md` ch. 4.3, maintainerbesluit 2026-09-09) fencet
de POSITIE van een setlezing, niet de vouwSOORT: de exemptie laat elk enkel
argument onder een vouwende soort door. Lens 2 mat dat die exemptie een
LEVENDE producentroute doorlaat waarop de uitvoerders uiteenlopen. Deze kaart
haalt die divergentie weg — óf de ASP-backend leert de vorm, óf de wire
weigert hem — dezelfde tweesprong als de complementwandeling-kaart. Geen
regressie: de vorm is schoon vóór en ná WF-096 (de grens is WF-093's
bestaande paar), en hij viel buiten het besluit van die kaart, dat over de
positie ging.

## Wat er gemeten is (2026-09-09, lens 2, nagemeten in de bouwsessie)

**De producentroute.** Uit

    Regel peildatum van een jutter
        geldig altijd
            De peildatum van een Jutter moet berekend worden als de eerste
            van de vondstdatum van alle vondsten van de Jutter.

emitteert de RegelSpraak-frontend met `diagnostics: []` een `EERSTE`-vouw met
een echt-getypeerde setbron (`set: true`, `type: DatumInDagen`,
`elementObjecttype: "Vondst"`, `bronRol` gevuld). Eén woord verder — "de
laatste van …" — geeft dezelfde vorm onder `LAATSTE`, óók met
`diagnostics: []`.

**De soort als enige variabele.** Op één document, alleen `kind` verwisseld,
over twee families (een datum-setbron uit de producent en de numerieke
setbron van `corpus/golden/jutterswet-vondsttelling`), casus met twee leden:

| kind | verifier | `ref` | ASP-backend | Drools |
|---|---|---|---|---|
| SOM | PASS | antwoordt | antwoordt | gedragen (gate 12) |
| TOTAAL | PASS | antwoordt | antwoordt | gedragen |
| MINIMUM | PASS | antwoordt | antwoordt | gedragen |
| MAXIMUM | PASS | antwoordt | antwoordt | gedragen |
| **EERSTE** | **PASS** | **2024-01-15** | **TRANSLATION FAULT** | transpiler-SKIP |
| **LAATSTE** | **PASS** | **2024-03-01** | **TRANSLATION FAULT** | transpiler-SKIP |
| **AANTAL** | **PASS** | **2** | **TRANSLATION FAULT** | **2** |

De ASP-fout is in alle drie de gevallen letterlijk
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises
it)`; de Drools-SKIP is `regel (waarde buiten scope)`.

**Twee ondervormen, niet één.** Dat onderscheid is de kern van de kaart en
lens 2's samenvatting vat het samen als één klasse:

- `EERSTE`/`LAATSTE`: `ref` antwoordt ALLEEN — twee uitvoerders weigeren.
  Levende producentroute, beide met `diagnostics: []`.
- `AANTAL`: `ref` EN Drools antwoorden allebei 2, alleen de ASP-backend
  weigert. Twee-tegen-één, wat een sterker argument is dat er een
  canoniek antwoord IS.

Die asymmetrie stuurt de tweesprong verschillend op, en de kaart moet ze apart
wegen in plaats van één regel over drie soorten te leggen.

**Eerlijkheidsnoot bij AANTAL.** De verifier-schone AANTAL-getuige is
HANDGEBOUWD: de mechanische soortwissel is in beide families om een andere
reden ill-formed (datumfamilie WF-091 — een telling leidt een getal af, niet
een datum; getalfamilie WF-037 — de `leegwaarde` van de SOM overleeft de
wissel niet), en pas met het knooptype op een telsoort gecorrigeerd is het
document schoon. Er is dus voor `AANTAL` géén producentroute gemeten; voor
`EERSTE` en `LAATSTE` wel. Wie de kaart oppakt moet dat niet omdraaien.

**Reproductiepad.** `/tmp/probe2/fe/` (lens 2: `eerste2.rgs`, `eerste2.json`,
`run/`), in de bouwsessie nagemeten en herbouwd onder `/tmp/setvouw/f1/`
(bron, `make ir`-emissie, de veertien soortvarianten onder `kinds/`, de
gecorrigeerde AANTAL-getuige en de casussen). Beide zijn tijdelijke paden, dus
de kaart die dit oppakt reproduceert uit de bronzin hierboven — die is
zelfdragend.

## De twee te wegen antwoorden

**(i) De ASP-backend leert de vorm.** De vouwsoorten EERSTE/LAATSTE/AANTAL
krijgen een setbron-vertaling, zoals SOM/TOTAAL/MINIMUM/MAXIMUM die al hebben.
Kosten: de vertaling zelf, en een getuige-golden met handberekende absolute
pins per soort — plus het antwoord op de vraag wat EERSTE en LAATSTE over een
ONGEORDENDE verzameling betekenen, want dat is precies waarom die twee anders
liggen dan MINIMUM/MAXIMUM: min/max hebben een ordening op de waarden, eerste/
laatste veronderstellen een ordening op de LEDEN die de casus niet uitspreekt.
Als dat antwoord "er is geen gedefinieerde volgorde" is, dan is (i) niet
haalbaar voor die twee en pleit de meting zelf voor (ii).

**(ii) De wire weigert de vorm.** Een WF-clausule die een setbron onder
EERSTE/LAATSTE (en mogelijk AANTAL) ill-formed maakt, met negatiefs per soort
en een arm-exacte mutant. Kosten: het snijdt een levende producentroute af, dus
de frontend moet meebewegen (eigen kaart, spec-first) — en voor `AANTAL` moet
(ii) uitleggen waarom twee overeenstemmende uitvoerders geen bewijs van een
canoniek antwoord zijn.

De kaart moet ook wegen of het antwoord PER SOORT verschilt: het is
toegestaan dat AANTAL (i) krijgt en EERSTE/LAATSTE (ii), en de meting wijst
die kant op. Eén regel voor drie soorten is hier niet vanzelfsprekend, anders
dan bij WF-096, waar de twee posities identiek maten.

## Acceptance

- Eén beslissing per vouwsoort, expliciet gemotiveerd, met de kosten van het
  niet-gekozen antwoord erbij — en met de EERSTE/LAATSTE-ordeningsvraag
  beantwoord in plaats van omzeild.
- Rood-eerst: bij (ii) de negatiefs met de verifier-md5 van vóór en ná; bij
  (i) de getuige-golden met absolute, met de hand berekende pins, en `ref` ≡
  ASP-backend erop.
- De meettabel hierboven wordt HERMETEN op de kop van de kaart (de exemptie
  kan intussen bewogen zijn) en de uitkomst per soort gepind.
- De Drools-kant benoemd: bij (i) draagt of weigert Drools reden-dragend
  (gate-12-kanaalvloer bijwerken); bij (ii) verdwijnt de vorm en is de SKIP
  tweede verdedigingslinie.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad), `spec/verifier/**`
(voorstel — idem) en `corpus/negatief/**` alléén bij (ii);
`backends/clingo/**` en `corpus/golden/**` alléén bij (i); `backlog/`.

Out of scope: WF-096 zelf — de POSITIE is beslecht (2026-09-09) en wordt hier
niet heropend; deze kaart gaat over de SOORT, wat precies de reden is dat de
twee vragen apart gesneden zijn. `frontends/**` — de emissie is hier
bewijsmateriaal; een frontend-wijziging is een eigen kaart ná deze, net als
bij WF-096. Het regel-DOEL en het onvolledige resolutiepaar: de twee andere
niet-bereikte klassen van WF-096, elk met hun eigen status in ch. 4.3.

---

## Result (2026-09-19)

**De beslissing is per soort verschillend uitgevallen, en niet zoals de kaart
hem zag aankomen.** EERSTE en LAATSTE krijgen route (i) en die is gebouwd.
AANTAL krijgt géén van beide routes in deze kaart: de hermeting haalde de
premisse weg waarop (i) voor die soort rustte, en wat ervoor in de plaats komt
is een genummerde maintainersvraag met de meting eronder. Dat is de kern van
de uitkomst; de rest van dit blok onderbouwt het.

### 1. De hermeting op de kop van de kaart

Gemeten op `9cedec4a` + kaartclaim, alle vier de uitvoerders, uit de bronzin
van de kaart opnieuw opgebouwd (de `/tmp`-paden van 2026-09-09 bestonden niet
meer). De producentroute reproduceert exact: de frontend emitteert uit die zin
een `EERSTE`-vouw met `set: true`, `type: DatumInDagen`,
`elementObjecttype: "Vondst"`, gevulde `bronRol` en `diagnostics: []`; één
woord verder geeft `LAATSTE` dezelfde vorm, ook schoon.

Datumfamilie, casus J1 met twee vondsten (2024-01-15, 2024-03-01), alleen
`kind` verwisseld:

| kind | verifier | `ref` | ASP-backend | Drools exec | Drools `--drl` |
|---|---|---|---|---|---|
| SOM | PASS | FOUT `plus: expected a number, got 2024-01-15` | **antwoordt 39520** | ERROR ClassCastException | EXCLUDE drl compile |
| TOTAAL | PASS | idem | **antwoordt 39520** | ERROR ClassCastException | EXCLUDE drl compile |
| MINIMUM | PASS | 2024-01-15 | 2024-01-15 | ERROR accumulate → BigDecimal | EXCLUDE drl compile |
| MAXIMUM | PASS | 2024-03-01 | 2024-03-01 | ERROR accumulate → BigDecimal | EXCLUDE drl compile |
| **EERSTE** | **PASS** | **2024-01-15** | **TRANSLATION FAULT** | EXCLUDE transpiler-SKIP | EXCLUDE marker |
| **LAATSTE** | **PASS** | **2024-03-01** | **TRANSLATION FAULT** | EXCLUDE transpiler-SKIP | EXCLUDE marker |
| **AANTAL** | **WF-091 FAIL** | 2 | TRANSLATION FAULT | ERROR ClassCastException | EXCLUDE drl compile |

De ASP-fout is woordelijk `TRANSLATION FAULT: expression kind not translated
(no corpus case exercises it)`; de exec-SKIP is
`transpiler SKIP: regel: … — regel (waarde buiten scope)`; de `--drl`-marker
is `TODO aggregatie EERSTE`.

Getalfamilie (`jutterswet-vondsttelling`, de SOM-setbron): de mechanische
soortwissel is bij ELKE niet-SOM-soort WF-037 FAIL — de `leegwaarde` overleeft
de wissel niet — en bij AANTAL bovendien WF-091 (`unit mismatch`). De
kaart-voetnoot klopt dus, en breder dan zij zelf stelde.

**Drie dingen wijken af van de kaarttabel, en alle drie zijn ze materieel.**

1. De kaarttabel zet SOM/TOTAAL op "antwoordt / antwoordt". In de DATUMfamilie
   is dat niet zo: `ref` faalt luid op datum-plus-datum terwijl de ASP-backend
   de dagnummers optelt tot 39520 en Drools crasht — onder een
   verifier-SCHOON document. Dat is een eigen, levende divergentie en geen
   deel van deze kaart; zij is gemeten en staat in §7 als vervolgkaart. Er is
   géén producentroute naar toe: de frontend typeert de zin weg met
   `som van vereist numerieke operanden, kreeg Datum in dagen`.
2. De Drools-kolom voor MINIMUM/MAXIMUM over een DATUM-setbron is geen
   "gedragen" maar een ERROR. Ook dat is pre-existent en buiten scope; het
   raakt geen corpusgolden (gate 12 is groen).
3. AANTAL: zie §3. Dat is de vondst die de kaart omdraait.

### 2. De ordeningsvraag, beantwoord uit de bron

De kaart eist dat de EERSTE/LAATSTE-ordeningsvraag beantwoord wordt en niet
omzeild. Antwoord: **het is een WAARDE-ordening, ondubbelzinnig, en `ref`
implementeert precies dat.** Drie onafhankelijke bronnen zeggen hetzelfde.

**(a) De normatieve RegelSpraak v2.3.0-specificatie** (read-only geraadpleegd
in de toolchain-werkbank; vindplaatsen, geen tekst):

- `RegelSpraak-specificatie v2.3.0.md` §5.8.4 (r. 2161–2199, p. 61) definieert
  *de eerste van* / *de laatste van* als het bepalen, binnen een verzameling
  datumwaarden, van de vroegste respectievelijk de jongste waarde. Het
  woordpaar dat de spec daarvoor gebruikt is oudste/jongste — leeftijd van de
  WAARDE, niet positie van een lid. Dezelfde paragraaf regelt leegwaarden
  (overslaan; alles leeg ⇒ leeg) en eist gelijke tijdprecisie over de
  operanden.
- Dezelfde file, r. 2045–2051: de opsomming van aggregatievormen zet
  "eerste/laatste waarde bij meervoudige instanties" als vorm 4 naast telling
  (5.8.1), sommatie (5.8.2) en min/max (5.8.3). De SETvorm valt dus binnen de
  spec en is geen wire-verzinsel.
- Dezelfde file, r. 2054–2062: de aggregatiegrammatica geeft
  `<datumaggregatie> ::= <datumaggregatiefunctie> <expressie>` — één
  expressie, precies de setbronvorm — naast de gelijste
  concatenatiegrammatica van §5.8.4 zelf (r. 2201–2208).
- `RegelSpraak-specificatie - typeringen v2.3.0.md` §4.14/§4.15 (r. 620–668,
  tabellen 24 en 25) typeert de operanden als datum-tijd en herhaalt de
  leegregel. Het aantal decimalen is expliciet niet van toepassing.

Waar de spec afwijkt van wat hier gebouwd is: zij kent *eerste van* /
*laatste van* alleen als DATUM-aggregatie. Dat is geen nieuw punt — het staat
al als geratificeerde verbreding in `CONSTRUCTS.md` rij `FunctieKind
(aggregations)` (2026-09-04): de ordenende vouwen nemen één vergelijkbare
familie, getallen óf datums van één precisie, en dat vervangt v0's
datums-alleen-letter bewust.

**(b) uchar's eigen normatieve tekst.** `RUNTIME-SEMANTICS.md` ch. 1.2e zegt
het al met zoveel woorden voor de gelijste vorm: eerste/laatste zijn ORDENENDE
vouwen, een `<`/`>`-vouw over de operandWAARDEN, nooit een positionele greep —
"de eerste van 5 en 3 is 3". De setvorm is dezelfde vouw met een andere bron.

**(c) Wat `ref` vandaag rekent.** `ref/lib/arith.ml` r. 487–495:
`agg_eerste`/`agg_laatste` zijn `agg_extreme` met de niet-strikte vergelijking
over `filled vs`, dus vroegste/laatste waarde met lege bijdragen overgeslagen
en leeg bij een lege verzameling. `ref/lib/eval.ml` r. 1487–1488 hangt de
setbron-arm daarop. Dat spoort met (a) en (b) tot en met de leegregel. Het
enige verschil met `agg_min`/`agg_max` is de gelijkspelregel (bij gelijke
waarden wint de latere operand in plaats van de eerste) en die is per
constructie onwaarneembaar: gelijkspel betekent waardegelijkheid, en de vouw
geeft een waarde terug, geen lid.

De kaart vroeg zich af of eerste/laatste "een ordening op de LEDEN
veronderstellen die de casus niet uitspreekt". Het antwoord is nee: die
veronderstelling bestaat in geen van de drie bronnen. De vorm is een MIN/MAX
onder een andere naam, en dat is precies waarom (i) haalbaar is.

### 3. AANTAL: de premisse van route (i) is weggevallen

De kaart bouwt haar AANTAL-argument op "twee-tegen-één": `ref` en Drools
antwoorden allebei 2, alleen de ASP-backend weigert, dus er IS een canoniek
antwoord. **Die meting is gedaan op een casus die de twee uitvoerders niet
kan onderscheiden.** Op een casus die dat wél kan lopen ze uiteen:

| casus | `ref` | Drools exec | Drools `--drl` |
|---|---|---|---|
| J1 met V1 (2,50 kg) en V2 (1,25 kg) — twee leden, beide gevuld | **2** | **2** | **2** |
| J1 met V1, V2 en V3 (V3 zonder `gewicht`) | **2** | **3** | **3** |

`ref` telt de GEVULDE waarden (`agg_aantal (filled vs)` op de setbron-arm),
Drools telt de LEDEN. De kaart-casus had twee gevulde leden, waarmee beide
lezingen 2 geven. Er is dus geen twee-tegen-één; er is een stille
waardedivergentie die pas zichtbaar wordt zodra één lid het attribuut mist —
en juist die vorm is wat `jutterswet-munttelling` elders in het corpus als
onderscheidend geval pint (J3 met een Munt zonder gewicht).

Erger: de normatieve tekst geeft Drools gelijk en `ref` ongelijk.
`RUNTIME-SEMANTICS.md` ch. 3.4a zegt bij het `jutterswet-aanmeldertelling`-
voorbeeld letterlijk dat een telling INSTANTIES vouwt en geen waarden, zodat
een leeg elementattribuut gewoon meetelt; ch. 3.4b herhaalt het voor het
meerstapsbereik ("a count over the union counts ELEMENTS"). En de v2.3.0-spec
§5.8.1 (r. 2105–2115) heet niet voor niets *Telling van instanties*: de invoer
is een meervoudige OBJECTexpressie en de uitkomst het aantal instanties dat de
rol invult. Een telling over een attribuut-setlezing is daar geen vorm van.

Dat laatste is ook mechanisch waar in de frontend, en dat is nieuw gemeten:

- `het aantal gewichten van alle vondsten van de Jutter` wordt door de
  RegelSpraak-frontend GEWEIGERD, met haar eigen WF-096-melding
  (`setlezing buiten een vouwbron: … hier wordt één waarde verwacht`);
- `het aantal gewichten van de vondsten van de Jutter` komt er wél door, maar
  als rij-1-wrat met `elementObjecttype: null` — een rolnaamtelling, geen
  setbron;
- de enige verifier-schone AANTAL-setbron die er is, is dus nog steeds
  handgebouwd, zoals de kaart al eerlijk vaststelde.

**Daarom is (i) voor AANTAL hier niet gebouwd.** Route (i) betekent voor deze
soort: kies een getal. De ASP-backend leren tellen zoals `ref` telt maakt
clingo ≡ ref en zet de Drools-divergentie vast als waardeverschil in het
vierkant (een `EXCLUDE_KNOWN`-regel in gate 12, buiten het mandaat van deze
kaart en inhoudelijk een erkenning dat het vierkant breekt). Hem leren tellen
zoals Drools telt maakt clingo ≡ Drools en breekt gate 5/6 tegen `ref` — en
`ref` is voor deze kaart de arbiter en staat buiten scope. Een getuige-golden
zonder leeg lid zou het verschil verbergen; dat is precies wat deze kaart
elders bestrijdt. De keuze die overblijft is een normbeslissing, niet een
technische, en zij staat in §6.

### 4. Wat er gebouwd is (route (i) voor EERSTE/LAATSTE)

**Rood eerst, woordelijk.** Op de kop van de kaart weigerde de ASP-backend de
setbronvouw voor alle drie de soorten met
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises
it)`.

**De vertaling.** `backends/clingo/lib/translate.ml`: de bestaande
setbron-arm voor `Minimum | Maximum` neemt `Eerste | Laatste` erbij, met
`Minimum | Eerste -> "#min"` en de rest `#max`. Eén arm, geen tweede, want het
is dezelfde vouw: dezelfde bijdragenverzameling als de som, dezelfde
lege-overslag door de join, dezelfde verplichte presence guard (`#min`/`#max`
over de lege verzameling geeft `#sup`/`#inf`, dat mag nooit een waarde worden),
dezelfde schaal van het elementattribuut. Over dagnummers IS vroegste `#min`.
De temporele weigering gaat mee en haar tekst is verbreed van
"a minimum/maximum over the temporal attribute …" naar "a ranking fold
(minimum/maximum/eerste/laatste) over the temporal attribute …" — `ref`
weigert die vier als één klasse ("this aggregation over the temporal attribute
… has no defined moment to fold"), dus de arm weigert mee in plaats van uit de
`asOfDate`-emmers te antwoorden.

**Suite-getuigen** (`backends/clingo/test/test_clingo.ml`, 408 → 418 checks):
vier waardeparen `ref`+clingo op een frame waarin de LEDENvolgorde omgekeerd
is aan de WAARDEvolgorde (een positionele lezing zou 2025-11-01 als "eerste"
geven waar 2025-02-14 gepind staat), vier paren op de geheel-lege bron
(beide leeg), en twee weigeringen op een temporeel elementattribuut — die
laatste twee kan geen corpusgetuige halen, want de vorm is in beide
uitvoerders dood.

**Mutatieproef, werkelijk uitgevoerd.**

- Mutant 1 — ordeningsrichting terug (`Minimum -> "#min"`, EERSTE valt naar
  `#max`): `FAIL ch1.2e EERSTE over een setbron is de VROEGSTE waarde: clingo
  pins the values (J1.eerste vondstdag = [20393], expected 20133)`, suite
  418 checks / 1 FAILURE, en de corpusgetuige `MISMATCH: J1.eerste vondstdag =
  [19783], expected 19737`.
- Mutant 2 — `Eerste | Laatste` weer uit het patroon: zes FAIL-regels, alle
  zes `expression kind not translated (no corpus case exercises it)`, suite
  418 checks / 6 FAILURES, corpusgetuige weer `TRANSLATION FAULT`.
- Beide teruggedraaid; suite 418 checks, 0 failures.

**De getuige-golden.** `corpus/golden/jutterswet-vondstvenster/`, geëmitteerd
uit een eigen `bron.rgs` (dus gate 10 draagt hem byte-identiek mee), met twee
regels — de eerste en de laatste vondstdag van een Jutter — en twee casussen
met handberekende absoluten:

- `case1-venster.json`: J1 heeft V1 2024-01-15, V2 2024-03-01 en V3 zonder
  datum ⇒ 2024-01-15 / 2024-03-01 (de lege bijdrage valt weg); J2 heeft geen
  vondsten ⇒ beide leeg (geen leegwaardevorm); J3 heeft één vondst zónder
  datum ⇒ beide leeg (alles-leeg); J4 heeft één vondst 2023-12-31 ⇒ eerste =
  laatste = 2023-12-31.
- `case2-volgorde.json`: J5 heeft V6 2025-06-30, V7 2025-02-14, V8 2025-11-01
  en V9 2025-02-14, in die schrijfvolgorde ⇒ eerste 2025-02-14, laatste
  2025-11-01. De ledenvolgorde noemt 2025-06-30 eerst en 2025-11-01 vóór de
  vroegste datum; alleen de ordenende lezing geeft de gepinde waarden. V9
  dupliceert de vroegste datum, zodat het gelijkspel meeloopt.

`ref` ≡ clingo op beide casussen (9 en 3 verwachtingen, exact).

**De Drools-kant, reden-dragend benoemd.** Geen van beide takken draagt de
vorm: de exec-model-tak laat de regel per vorm vallen
(`transpiler SKIP … waarde buiten scope`), de DRL-tekst-tak zet een
genoemde marker (`TODO aggregatie EERSTE`). De golden is dus een ref ≡
clingo-paar en niet het volle vierkant, en dát staat in
`scripts/check-drools.sh` opgeschreven in plaats van weggelaten: één regel in
`EXPECTED_CHANNEL` met de reden. `EXPECTED_PLAYED` blijft 159 — het script
zegt zelf dat een NIEUWE golden die niet speelt nooit is meegeteld — en de
kanaalvloer gaat van 49 naar 50 gepinde niet-spelers. Dat zijn de twee
toegestane plekken in dat script en er is verder niets aan veranderd.

### 5. Corpussweep en poorten

**Sweep over alle goldens, vóór en ná, in beide Drools-takken** plus verifier,
`ref` en clingo (208 → 209 goldens). Verschil: precies één regel, de nieuwe
golden zelf. Geen enkele bestaande golden beweegt in welke uitvoerder dan ook.

| # | poort | exit |
|---|---|---|
| 1 | `check-self-contained.sh` | 0 |
| 2 | `check-ir-schema.sh` | 0 — 237 bestanden, vloer 192 |
| 3 | `check-verifier.sh` | 0 — 237 golden schoon, 395 negatief |
| 4 | `check-case-schema.sh` | 0 — 567 casusbestanden |
| 5 | `check-oracle.sh` | 0 — ref-suite 643, 539 casussen exact |
| 6 | `check-clingo.sh` | 0 — suite 418 checks (was 408), 539 casussen |
| 7 | `check-noop.sh` | 0 — 503 casussen, 36 overlevers |
| 8 | `check-rood.sh` | 0 — 28 rode casussen |
| 9 | `check-catala.sh` | 0 — 201 checks, 12 goldens |
| 10 | `check-regelspraak.sh` | 0 — 298 tests, 58 rgs-goldens (was 57) |
| 11 | `check-toka-parity.sh` | 0 |
| 12 | `check-drools.sh` | 0 — played 159, kanaalvloer 50 |
| 13 | `check-reftype-derivation.sh` | 0 — 126 twists over 23 goldens |
| 14 | `check-flogic.sh` | 0 — 130 checks, 2 goldens |

De acceptance van deze kaart noemt dertien poorten; het zijn er veertien
sinds gate 14 (`check-flogic.sh`, 2026-09-12).

`main` schoof tijdens deze sessie van `9cedec4a` naar `fa990e05` (de
bereikafleiding-kaart, die `backends/drools/**` raakt). De tak is daarop
herbased en de veertien poorten staan hierboven zoals ze op de herbasede top
gemeten zijn; de sweep hierboven is op het oorspronkelijke vertrekpunt gedaan,
want dat is waar zij over gaat — de vraag of DEZE wijziging iets verplaatst.
Dat de Drools-verandering van `main` niets aan het kanaal van deze golden doet
is niet aangenomen maar gemeten: gate 12 noemt elke niet-speler bij naam en is
groen, en gates 5 en 6 pinnen elke casuswaarde opnieuw.

Gate 13 beweegt mechanisch niets: `DatumInDagen` draagt geen eenheid en geen
decimalen, dus de nieuwe golden levert geen twistpositie. De
bookkeeping-klem van die poort ziet hem dan ook niet als vierde klasse.

### 6. De maintainersvraag over AANTAL (genummerd)

**De vorm.** `AANTAL` met één argument dat een setlezing is
(`set: true`, een echt getypeerd elementattribuut, gevulde `bronRol`,
`elementObjecttype`). Verifier-PASS vandaag, door WF-093/WF-096's exemptie
(arity 1 ∧ vouwende soort). Geen producentroute: de RegelSpraak-frontend
weigert de natuurlijke zin met haar eigen WF-096-melding, en de andere
spelling wordt een rolnaamtelling.

**De meting.** `ref` telt gevulde waarden, Drools telt leden; op een casus met
één leeg elementattribuut is dat 2 tegen 3; de ASP-backend weigert de vorm.
Beide `ref`-lezing en Drools-lezing zijn vandaag verifier-schoon.

**De normtekst.** `RUNTIME-SEMANTICS.md` ch. 1.2e (aantal = 0 bij lege bron,
nooit leeg), ch. 3.4a (een telling vouwt instanties, niet waarden — een leeg
elementattribuut telt mee) en ch. 3.4b (een telling over de unie telt
ELEMENTEN). v2.3.0 §5.8.1 heet *Telling van instanties*.

**De opties.**

1. **(i-instanties)** De vorm blijft, en zij telt LEDEN. De ASP-backend krijgt
   een `#count`-arm over de setbron-join; Drools klopt al; `ref`'s
   setbron-arm moet van `agg_aantal (filled vs)` naar een telling van de
   bijdragende instanties. Kosten: een wijziging in `ref` — de arbiter — en
   daarmee een eigen kaart met rood-eerst op ref's eigen suite; winst: alle
   drie de uitvoerders eens, en de normtekst hoeft geen zin te bewegen.
2. **(i-waarden)** De vorm blijft, en zij telt GEVULDE WAARDEN. De
   ASP-backend leert `ref`'s lezing; ch. 3.4a en ch. 3.4b krijgen een
   uitzondering voor de setbron; gate 12 krijgt een waardedivergentieregel
   voor de Drools-kant. Kosten: het vierkant breekt op deze vorm, en de
   normtekst moet twee plaatsen tegenspreken die vandaag stellig zijn.
3. **(ii)** De wire weigert de vorm. Een WF-clausule die een setbron onder
   `AANTAL` ill-formed maakt (de exemptie van WF-093/WF-096 wordt arity 1 ∧
   een WAARDE-vouwende soort, met `AANTAL` eruit), met negatief per spelling
   en een arm-exacte mutant. Kosten: niets bij de producent, want die route
   bestaat niet; `ref`'s setbron-arm en Drools' lowering worden dode code die
   geen document meer bereikt; ch. 3.4a/3.4b blijven staan zoals ze staan.

**Aanbeveling van deze sessie: (3).** De vorm heeft geen producent, geen
normtekst die haar definieert, en twee uitvoerders die haar stil verschillend
lezen — dat is de klassieke drie-uit-drie voor "de wire weigert hem". (1) is
verdedigbaar en zou hem redden, maar betaalt met een wijziging in de arbiter
voor een vorm die niemand schrijft; (2) is de enige die de normtekst moet
tegenspreken en valt daarmee af. Wat (3) kost en (1) wint is precies het
verschil tussen een uitdrukbare en een gebruikte vorm, en dat is een
maintainersafweging, niet die van een kaartsessie.

**Voorstel voor de vervolgkaart** (tekst; deze sessie maakt hem niet aan):

> `backlog/new/20260919-aantal-setbron-besluit.md`
>
> # De AANTAL-setbron: telt hij leden of gevulde waarden, of telt hij niet
>
> Goal: `AANTAL` met een setbron-argument is verifier-schoon en wordt door
> `ref` en Drools stil verschillend gelezen — `ref` telt gevulde waarden,
> Drools telt leden, en op een casus waarin één lid het attribuut mist is dat
> 2 tegen 3 (gemeten 2026-09-19, kaart eerste-laatste-setvouw-kloof §3). De
> ASP-backend weigert de vorm. Er is geen producentroute: de
> RegelSpraak-frontend weigert de natuurlijke zin met haar eigen
> WF-096-melding. `RUNTIME-SEMANTICS.md` ch. 3.4a/3.4b en de normatieve
> RegelSpraak-spec §5.8.1 zeggen allebei dat een telling instanties telt.
> Deze kaart voert het besluit uit dat de maintainer neemt uit de drie
> genummerde opties van die kaart (i-instanties / i-waarden / ii).
>
> Acceptance: één besluit, met de kosten van de niet-gekozen opties erbij;
> rood-eerst (bij (ii) het negatief per spelling met de verifier-md5 vóór en
> ná plus een arm-exacte mutant; bij (i) de getuige-golden met een LEEG
> elementattribuut in de casus, want zonder dat lid is de vorm niet
> onderscheidend); de corpussweep vóór/ná; alle veertien poorten groen.
>
> May touch: bij (ii) `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL
> (beschermde paden) plus `corpus/negatief/**`; bij (i-waarden)
> `backends/clingo/**`, `corpus/golden/**` en de waardedivergentielijst van
> `scripts/check-drools.sh`; bij (i-instanties) bovendien `ref/**` en
> `spec/RUNTIME-SEMANTICS.md` als voorstel. `backlog/`.
>
> Out of scope: EERSTE/LAATSTE — beslecht 2026-09-19, route (i) gebouwd.
> `frontends/**`.

### 7. Registertekst en documentvoorstellen (niet zelf geland)

**`spec/CONSTRUCTS.md`, rij 1.** De zin die vandaag luidt

> The EERSTE/LAATSTE/AANTAL set-fold kinds inside the exemption stay a
> measured executor gap (ref answers, clingo refuses, Drools splits per kind)
> — carded, not hidden.

wordt voorgesteld te vervangen door

> **Landed (2026-09-19):** de EERSTE/LAATSTE-setvouw binnen de exemptie is
> geen uitvoerderskloof meer — de ASP-backend rijdt ze op de bestaande
> min/max-setarm, want het IS dezelfde vouw: een ordening over de WAARDEN
> (`RUNTIME-SEMANTICS.md` ch. 1.2e; normatieve RegelSpraak §5.8.4 leest
> vroegste/jongste), dus over dagnummers is eerste `#min` en laatste `#max`,
> met dezelfde lege-overslag, dezelfde presence guard en dezelfde temporele
> weigering (die vier soorten weigeren als één klasse). Getuige
> `jutterswet-vondstvenster` (uit de producentzin geëmitteerd; de lege
> bijdrage, de lege bron, de alles-lege bron, het singleton en een casus
> waarin de ledenvolgorde omgekeerd aan de waardevolgorde staat), `ref` ≡
> clingo op handberekende absoluten. De Drools-kant draagt geen van beide
> takken — exec-model SKIP, DRL-tekst marker — en dat staat als
> reden-dragende kanaalregel in gate 12 (`EXPECTED_PLAYED` onveranderd 159,
> kanaalvloer 49 → 50). **AANTAL** blijft open en is nu scherper gemeten dan
> "een kloof": `ref` telt gevulde waarden waar Drools LEDEN telt (2 tegen 3
> zodra één lid het attribuut mist), de ASP-backend weigert, er is geen
> producentroute — de frontend weigert de natuurlijke zin met haar eigen
> WF-096-melding — en ch. 3.4a/3.4b geven Drools gelijk. Drie genummerde
> opties liggen bij de maintainer
> (`backlog/done/20260909-eerste-laatste-setvouw-kloof.md` §6).

**`spec/ASP-TRANSLATION.md` S8** (beschermd pad, voorstel). De alinea
**Minimum / Maximum** (r. 930–942) heet voortaan **Minimum / Maximum /
Eerste / Laatste**, met één toegevoegde zin: eerste/laatste zijn dezelfde
ordenende vouw over de waarden (ch. 1.2e), dus `#min` voor eerste en `#max`
voor laatste, met de getuige `jutterswet-vondstvenster` naast
`jutterswet-vondstpiek`. In de catch-all-inventaris (r. 2348–2352) verdwijnt
"minimum/maximum over de SET-typed source" ten gunste van "minimum / maximum /
eerste / laatste over de SET-typed source", en de zin over wat nog ónder de
catch-all valt houdt de gelijste min/max en krijgt de AANTAL-setbron erbij,
met de verwijzing naar de vervolgkaart.

Geen voorstel voor `spec/IR-SPEC.md`: ch. 4.3 spreekt over de POSITIE en die
is niet geraakt; de soortbeslissing van deze kaart verandert geen enkele
normatieve MUST.

### 8. Open twijfel

- **De verbreding naar niet-datums is hier niet gemeten, alleen geërfd.** De
  nieuwe arm draagt EERSTE/LAATSTE over een numerieke setbron even goed als
  over een datum-setbron, en `ref` doet dat al. Geen corpusdocument en geen
  producentzin brengt die combinatie voort — de RegelSpraak-frontend typeert
  "de eerste van" als datumaggregatie, en de mechanische soortwissel in de
  getalfamilie is WF-037 FAIL. De verbreding staat als geratificeerde
  afwijking in `CONSTRUCTS.md` (2026-09-04) en wordt door de
  `agg_extreme`-deling van `ref` gedragen, maar ze is in deze kaart niet met
  een eigen getuige gepind. Dat is bewust smal gehouden en het is een gat.
- **De `--drl`-tak is niet verbeterd en de markertekst is onnauwkeurig.** Bij
  `LAATSTE` meldt die tak `TODO aggregatie LAATSTE: de eerste van …` — het
  kind klopt, de meegegeven bronzin komt van de EERSTE-variant waaruit het
  document getwist was. Op de echte golden meldt hij `TODO aggregatie
  EERSTE` op de eerste regel en stopt daar. Niet mijn bestand (Drools is
  buiten scope) en geen poortgevolg, maar het is een verwarrende melding.
- **SOM/TOTAAL over een datum-setbron is een levende, ongepinde divergentie.**
  Verifier-PASS, `ref` faalt, de ASP-backend telt dagnummers op, Drools
  crasht. Geen producentroute, dus geen haast — maar het is dezelfde soort
  gat als het gat dat deze kaart dichtte, één rij hoger in de tabel. Verdient
  een eigen kaart; ik heb er geen aangemaakt omdat deze kaart al één
  vervolgvoorstel draagt en twee losse kaartvoorstellen in één Result het
  registerwerk van de hoofdsessie onnodig belast.
- **MINIMUM/MAXIMUM over een datum-setbron crasht Drools.** Zelfde klasse,
  zelfde meting, geen corpusdrager. Hoort bij dezelfde vervolgkaart.
- **De frontend-melding voor WF-096 noemt `aantal` als vouwende soort** en
  weigert toch de enige zin die een AANTAL-setbron zou schrijven. Dat is geen
  fout in de weigering — de zin is om een andere reden niet wat zij lijkt —
  maar de meldingstekst belooft iets dat de bouwer niet levert. Raakt
  `frontends/**` en valt daarmee buiten deze kaart.
