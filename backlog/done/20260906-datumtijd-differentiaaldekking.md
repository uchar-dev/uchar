# De differentiaal heeft nul DatumTijd-dekking

Goal: `DatumTijd` is een gedeclareerd datatype van de wire, maar geen
enkele executor-differentiaal raakt het. Gemeten 2026-09-06: het
golden-corpus bevat **nul** `DatumTijd`-vergelijkingen en **nul**
`DatumTijd`-literals — wel 19 `dateTime`-waarden op de case-wire, die
dus alleen worden ingelezen en nooit ergens mee vergeleken. De ASP-vertaling
faalt op een `DatumTijd`-ordening die **geen literal draagt**: een document
dat twee `DatumTijd`-attributen met `EERDER` vergelijkt geeft
`TRANSLATION FAULT: unscalable datatype in the ASP translation`, terwijl de
referentie-interpreter datzelfde document exact naspeelt
(`oracle: PASS`). Draagt het document wél een literal, dan valt ook de
interpreter uit: `EVALUATION FAULT: literal of this datatype not
implemented` — in beide lexemen. Alleen de Drools-backend voert zo'n regel
uit. Eén executor is geen differentiaal: elke `DatumTijd`-semantiek die
vandaag zou verschuiven, verschuift ongezien.

Daaronder ligt een tweede gat. Er zijn **drie** lezers van een
datum-tijd-lexeem — de gedeelde case-lezer van interpreter + ASP-backend,
de eigen case-lezer van de Drools-backend, en de literal-lezer van de
IR-wire — en buiten het schema-patroon van `uchar-case-v0` zijn ze het
oneens (gemeten 2026-09-06):

| vorm | literal-lezer | case-lezer (Drools) | case-lezer (interpreter + ASP) |
|---|---|---|---|
| `…T12:00:00.1` | weigert | 100 ms | 1 ms |
| `…T12:00:00.100000` | weigert | 100 ms | 100 000 ms |
| `…T12:00:00.1234` | weigert | 123,4 ms | 1 234 ms |
| `2024-1-31T09:00:00` | weigert | weigert | 09:00 op 31 januari |
| `…T12:00` (geen seconden) | weigert | 12:00:00 | `malformed dateTime` |

Vandaag is dat onbereikbaar: gate 4 dwingt op elk casebestand het
schema-patroon af (vaste breedtes, `T`, secondenveld, fractie exact drie
cijfers), en binnen dat hek vallen de drie lezingen samen. Het hek is dus
de enige reden dat de divergentie nooit zichtbaar wordt — geen van de
lezers weigert zelf wat hij niet eenduidig kan lezen. Een casebestand dat
langs gate 4 komt (een handmatige run, een generator, een tweede
case-producent) krijgt drie antwoorden. Dat is precies de klasse fout die
uchar met de differentiaal wil vangen, en die de differentiaal hier niet
kan vangen.

De kaart sluit beide gaten in één slag, en levert daarmee de golden op die
`backlog/done/20260905-drools-datumtijd-iso-lexeem.md` bewust niet kon
bouwen (de lexeempassage in IR-SPEC ch. 4.1 is beschrijvend gebleven omdat
elke MUST hier een doorlopende golden nodig heeft, en die kan pas bestaan
als ref en de ASP-vertaling `DatumTijd` dragen).

Acceptance:
- **Ref draagt de `DatumTijd`-literal.** De literal-arm leest het
  canonieke ISO-lexeem naar dezelfde ms-laag waarin de case-wire zijn
  `dateTime` al leest; een niet-canoniek lexeem faalt luid met de
  positie, nooit stil. Meetbaar: het document dat vandaag
  `EVALUATION FAULT: literal of this datatype not implemented` geeft,
  speelt daarna exact.
- **De ASP-vertaling draagt de `DatumTijd`-ordening** (en daarna de
  literal). Vandaag faalt `decimals_of_datatype` erop; de ms-laag is een
  gehele schaal, dus de ordening is vertaalbaar — de vraag die de kaart
  moet beantwoorden is welke schaal de vertaling voert en wat dat met de
  bestaande integer-schaal doet. Meetbaar: hetzelfde literaalvrije
  ordeningsdocument dat nu `TRANSLATION FAULT` geeft, geeft daarna
  dezelfde stabiele modeluitkomst als ref.
- **De drie lezers klemmen zelf.** Elke lezer weigert wat buiten het
  schema-patroon valt (fractie ≠ drie cijfers, veldbreedte, ontbrekend
  secondenveld, zone/offset) in plaats van te gokken — fail-fast, met de
  vorm in de foutmelding. Rood-eerst per lezer: een vorm uit de tabel
  hierboven die vandaag drie antwoorden geeft, geeft daarna drie
  weigeringen. De literal-lezer klemt al (2026-09-06); de twee
  case-lezers zijn het werk. Gate 4 blijft staan — het schema is dan een
  tweede hek, niet het enige.
- **De golden.** Eén nieuwe golden met een `DatumTijd`-attribuut, een
  ordening tegen een `DatumTijd`-literal in het canonieke lexeem, en een
  subdag-drempel (een middernachtdrempel verbergt precies de fout die de
  vorige kaart aantoonde). Case-waarden vóór, óp en ná de drempel, plus
  een millisecondenpaar. Hij moet door gates 5, 6 en 12 heen: ref ≡
  clingo ≡ Drools op absolute, met de hand berekende waarden.
- **Daarna pas de wire-vraag.** Met een doorlopende golden kan de
  beschrijvende ch.-4.1-passage een genummerde MUST worden (verifier-check
  + negatief-cases per veld). Die stap is een eigen voorstel op een
  beschermd pad; deze kaart levert er de voorwaarde voor en mag hem
  voorstellen, niet mergen.
- Alle dertien gates groen; geen bestaande pin beweegt. Let op gate 13:
  een nieuwe golden met unit-dragende annotaties komt in de twist-tabel.

May touch: `ref/` (de literal-arm en de case-lezer),
`backends/clingo/` (de vertaling van de ordening en de literal),
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/`
(de eigen case-lezer), `corpus/golden/**` (de nieuwe golden),
`spec/IR-SPEC.md` en `spec/schema/` (voorstel, alleen als de MUST-stap
binnen deze kaart wordt gehaald), `backlog/`.

Out of scope: de literal-lezer van de IR-wire zelf (geland 2026-09-06,
verankerd en met getuige); de emitter die voor een `DatumTijd`-literal nog
het dag-eerste snippet in `waarde` schrijft (eigen kaart — dat vergt een
herpin van de emitter-goldens); de dagprecisie-kant van WF-084 (dicht);
`Typering.vergelijk` op de precisienaad (restpunt van de
WF-036-kaart).

---

Result (2026-09-06, branch `card/datumtijd-dekking`, **gerebased op `main`
44b393c**; oorspronkelijk gebouwd op 8fb95ee).
**Alle vier de acceptatiepunten gehaald, plus de wire-vraag: de
beschrijvende ch.-4.1-passage is WF-095 geworden** (maintainerbesluit
2026-09-06, genummerde vraag). Dertien gates groen vanuit de worktree op de
gerebasede kop. De branch blijft ONGEMERGED — hij raakt beschermde paden
(`spec/IR-SPEC.md`, `spec/verifier/`, `scripts/`) en is dus een voorstel.

**Nummerbotsing en hernoeming (rebase, 2026-09-06).** Deze regel landde in
de sessie als WF-094 — op 8fb95ee liep de index tot WF-093 en 094/095
kwamen nergens voor. Terwijl deze kaart liep landde de zelfnaam-kaart op
`main` en die claimde WF-094 (ch. 2.1, vijf armen, vijf
`corpus/negatief/wf094-zelfnaam-*`-getuigen, eigen indexrij). Bij de rebase
is deze regel daarom **WF-094 → WF-095** hernoemd, overal: de verifier-arm
en zijn melding, de ch.-4.1-passage en de indexrij, de vijf negatief-mappen
(`git mv`) en hun NOTEs, de golden-NOTE, de ASP-vertaalnotitie, beide
suites en dit Result. De rebase gaf **één** conflict — de indexrijregel,
waar beide kaarten een rij op `WF-094` toevoegden — deterministisch opgelost
door **beide** rijen te behouden, zelfnaam (094) vóór lexeem (095);
`spec/verifier/lib/checks.ml` merge-de automatisch (de zelfnaam-arm zit in
`wf094 ctx` bij de modeltabellen, de lexeem-arm in de `Literal`-walk).
Controle na de sweep: elke resterende `WF-094`/`wf094` in de repo hoort bij
de zelfnaam-regel (9 in `IR-SPEC.md`, 5 in `checks.ml`, plus haar eigen
kaarten en getuigen), en `WF-095`/`wf095` dekt deze regel volledig (60
voorkomens over 15 bestanden).

**1. Ref draagt de DatumTijd-literal.** Rood-eerst gemeten op een
document met een `EERDER` tegen een canonieke `DatumTijd`-literal:
`EVALUATION FAULT: literal of this datatype not implemented:
"2026-03-15T12:00:00.000"`. Daarna `oracle: PASS (4 expectations)`.
`Value.datetime_of_lexeme` leest BEIDE gedragen spellingen naar de ms-laag
waarin de case-wire zijn `dateTime` al leest — symmetrisch met de
Drools-literal-lezer, één betekenis, twee spellingen — en weigert al het
andere MET DE POSITIE, bv.
`at offset 20: expected exactly three fraction digits` voor `.1`,
`at offset 10: expected 'T'` voor de spatie-variant, en
`hour 25 is out of range` voor een onmogelijk veld. Beide toegelaten
grammatica's, in alle vier de spellingen (`2026-03-15T12:00:00.000` en
`2026-03-15T12:00:00` — canoniek ISO, met en zonder fractie;
`15-03-2026 12:00:00.000` en `dd. 15-03-2026 12:00:00.000` — het
bronsnippet, met en zonder de-dato-prefix), spelen exact hetzelfde document
na.

**2. De ASP-vertaling draagt de ordening én de literal.** Rood-eerst op het
LITERAALVRIJE ordeningsdocument (twee `DatumTijd`-attributen onder
`EERDER`): `ref oracle: PASS`, `clingo TRANSLATION FAULT: unscalable
datatype in the ASP translation`. Daarna `clingo: PASS (2 expectations)`,
en met literal `clingo: PASS (4 expectations)`.

*De schaalkeuze, op meting.* GEEN epoch-ms-geheel. `2026-03-15T12:00:00.000`
is 1 773 576 000 000 ms; clingo 5.8.2 grondt `a(1773576000000)` als
`a(-245493248)` — het wrapt stil op 32 bits — en de ordening kán dan
omklappen: `flip :- 1775968976896 < 1773576000000.` wordt AFGELEID, dus
een moment 27,7 dagen later heet eerder. De vertaling voert daarom het
bestaande paarterm `dt(D, MS)` (RUNTIME-SEMANTICS ch. 3.8, A4), waarin
beide componenten per constructie binnen 2^31 blijven. Gevolg voor de
bestaande integer-schaal: NUL. `decimals_of_datatype` blijft `DatumTijd`
weigeren en dat is precies goed — een `DatumTijd` IS geen geschaalde
integer, hij heeft een paar. Er is geen nieuw overflow-hek nodig: de
bestaande hekken (`max_int31`, de 46340-guard, `range_guard`) zitten op
PRODUCTEN en op de absolute ms-waarde, en die vormt dit pad nooit. De
ordening is lexicografisch: twee regels voor één `dtcmpN`-hulpatoom (een
disjunctie kan niet in een ASP-body), beide positieve joins, dus een lege
operand maakt de vergelijking onwaar — de ch. 1.2d-datumrij, gratis.
`spec/ASP-TRANSLATION.md` is additief bijgewerkt: `DatumTijd` verhuist uit
de niet-vertaald-lijst voor de ORDENING en de LITERAL; wat luid blijft is
datum-tijd (on)gelijk-aan (de `eqN`-harmonisatie vraagt een schaal die het
paar niet heeft, en Tabel 44's één-leeg-waar-cel de negatieroute erover;
geen corpuscase).

*Twee luide flanken erbij, gemeten in de fixronde (2026-09-06).* De
catch-all van `dt_term` noemde zijn weigergrond te smal. Twee vormen die de
wire toelaat en die ref gewoon speelt, vallen erin:

| vorm | verifier | ref | clingo |
|---|---|---|---|
| ordening tegen een gedeclareerde `DatumTijd`-**parameter** | PASS | `oracle: PASS` | `TRANSLATION FAULT` (dt_term-catch-all) |
| linkeroperand is een `Let` die het attribuut bindt en als `BinderRef` teruggeleest wordt | PASS | `oracle: PASS` | `TRANSLATION FAULT` (dt_term-catch-all) |

Beide melden nu met naam en toenaam in de catch-all, samen met de
residuele `VariabeleRef`, de set-getypeerde vouw, de
`DATUM_MET`/`DATUMTIJD_MET`-constructor en de genavigeerde lezing; de
DRAAG-kant van dezelfde melding noemt voortaan óók de literal en de
eerste/laatste over opgesomde operanden, zodat een lezer aan één regel
genoeg heeft. Alleen de MELDINGSTEKST beweegt: sweep vóór en ná over alle
434 casebestanden geeft een byte-identieke verdictset (geen enkele
`expectedFault` in `corpus/rood` pint deze tekst — geteld: nul).

*De twee-plekken-twee-meldingen-observatie.* Eén en dezelfde
`DatumTijd`-parameter faalt op TWEE plekken met TWEE verschillende
meldingen, en welke je krijgt hangt niet af van de parameter maar van wie
hem aanraakt. Leest een regel hem, dan valt hij in de `dt_term`-catch-all
hierboven. Leest GEEN regel hem, maar geeft de case hem een waarde, dan
faalt de parameter-emissie met `unscalable datatype in the ASP
translation` — dezelfde woorden die een datum-tijd in `expr_term`'s
scalaire codering krijgt (gemeten: ref `oracle: PASS (6 expectations)`,
clingo TRANSLATION FAULT; laat de case-waarde weg en clingo speelt
diezelfde zes). De declaratie alleen kost dus niets; de case-waarde wel.
Beide observaties staan nu in `spec/ASP-TRANSLATION.md`, inclusief dat
"unscalable datatype" daarmee tweeduidig is geworden.

**3. De drie lezers klemmen zelf.** Rood-eerst, per lezer gemeten, precies
de vormen uit de kaarttabel:

| vorm | literal-lezer | Drools-case-lezer VOOR → NA | gedeelde case-lezer VOOR → NA |
|---|---|---|---|
| `…T12:00:00.1` | weigerde al | 100 ms → weigering | 1 ms → weigering |
| `…T12:00:00.100000` | weigerde al | 100 ms → weigering | 100 000 ms → weigering |
| `…T12:00:00.1234` | weigerde al | 123,4 ms → weigering | 1 234 ms → weigering |
| `2026-3-15T09:00:00` | weigerde al | parse-crash → weigering die de vorm noemt | 09:00 op 15 maart → weigering |
| `…T12:00` (geen seconden) | weigerde al | 12:00:00 → weigering | weigering → weigering met positie |
| `2026-02-30T12:00:00.000` | weigerde al | parse-exceptie → weigering die de kalender noemt | **2 maart** → weigering, `day 30 is out of range` |
| `2026-03-15T25:00:00.000` | weigerde al | parse-exceptie → weigering die de kalender noemt | **01:00 de volgende dag** → weigering, `hour 25 is out of range` |
| `…T12:00:00Z` / `+01:00` / spatie / rommel | weigerde al | weigering → weigering die de vorm noemt | weigering |

De twee kalenderrijen zijn erbij gekomen in de fixronde en maken de claim
BREDER dan de kaart hem adverteerde: het hek zit niet alleen op de
veldbreedtes maar ook op de kalender. De gedeelde lezer rekende de
burgerlijke dag zuiver rekenkundig (`days_from_civil` zonder
geldigheidstoets), dus 30 februari rolde door naar 2 maart en uur 25 naar
01:00 de dag erna — hermeten: die twee lexemen leverden identiek dezelfde
ms-waarde als `2026-03-02T12:00:00` respectievelijk
`2026-03-16T01:00:00`. Dat is dezelfde klasse als de fractierijen (een
antwoord dat niemand geschreven heeft) via een andere deur, en beide deuren
zijn nu dicht. De Z-rij toont de andere helft van dezelfde reparatie: hij
werd al door alle drie geweigerd, maar de Drools-lezer weigerde hem als
kale `DateTimeParseException`; nu noemt de melding de vorm die hij
verwachtte. Een weigering die niet zegt wat hij wilde, is een weigering
waar een producent niets mee kan.

De gedeelde lezer (`ref/lib/case.ml`) deelt nu de canonieke ISO-arm van
`Value`; de Drools-lezer (`CaseRunner.waardeNaarArg`) klemt op het
schema-patroon met de vorm in de melding, als HARDE fout en nooit een
`Excluded` (dat zou de golden stil uitsluiten). Gate 4 blijft staan — het
schema is nu het tweede hek. **Nul bestaande cases bewegen:** alle 24
`dateTime`-waarden (19 publiek + 5 in de pariteitszone) zijn
schema-conform (fractie afwezig, of exact drie cijfers), geteld en
getoetst. Hertelling in de fixronde, want "drie-cijferig" was te sterk
gezegd: van de 19 publieke waarden van vóór deze kaart draagt er **nul** een
fractie — 19 van 19 fractieloos — en alleen de 5 waarden in de
pariteitszone zijn drie-cijferig. Het schemapatroon maakt de fractie
optioneel en dat is precies waarom beide vormen conform zijn; geen enkele
waarde in welke zone dan ook draagt een andere breedte. Gate 11 draait
onbewogen (29 replays door ref, clingo en Drools, nul divergenties).

**4. De golden: `corpus/golden/jutterswet-meldstip`.** Eén
`DatumTijd`-attribuut, twee ordeningen (`EERDER` → `tijdig`, `LATER` →
`verlaat`) tegen één `DatumTijd`-literal in het CANONIEKE lexeem, op de
SUBDAG-drempel `2026-03-15T12:00:00.000`. Handgeschreven wire zonder
`bron.rgs` (het emitterpatroon van de Catala- en getijdenpaar-goldens).
Zeven instanties over drie casebestanden: een milliseconde ervóór en erná,
exact erop, leeg, en een hele DAG ervóór en erná — zo kan noch de
ms-van-de-dag alleen, noch de burgerlijke dag alleen antwoorden.
Handberekening in `NOTE.md` (dag 20 527, ms-van-de-dag 43 200 000,
absoluut 1 773 576 000 000) en elk van de zes momenten daarna mechanisch
bevestigd tegen ref. Uitslag: `ref ≡ clingo ≡ Drools` op absolute waarden
— gates 5, 6 en 12, en gate 12 telt hem als PLAYED (110 → 111). Gate 13
beweegt niet: het document draagt geen enkele eenheid-dragende annotatie.

**5. WF-095** (bij aanvang WF-094 — het toen volgende vrije nummer; na de
rebase hernummerd, zie de nummerbotsing hierboven). De MUST laat BEIDE gedragen lexemen toe —
canoniek ISO `yyyy-mm-ddThh:mm:ss[.fff]` én het bronsnippet
`[dd. ]d-m-yyyy hh:mm:ss[.SSS]` — en weigert al het andere. Reden expliciet
in de spec: de eigen emitter schrijft voor een `DatumTijd`-literal nog het
snippet in `waarde` én `bron`, dus een enkel-lexeem-regel (zoals WF-084)
zou de eigen producent onconform maken. Vijf negatiefs, één geweigerde
veldklasse per stuk (`wf095-zone-suffix`, `wf095-spatie-scheider`,
`wf095-fractiebreedte`, `wf095-ontbrekende-seconden`,
`wf095-omringende-tekst`), alle vijf ROOD-EERST: `verifier: PASS` onder de
verifier van de branch-kop vóór de arm (md5
`957d3eeb5667188e33d5468b80f21b71`, gemeten 2026-09-06 op 8fb95ee — een
historische meting die de rebase niet ongedaan maakt), `WF-095 FAIL` onder
de arm, elk met exact één regel in het rapport en geen tweede bevinding.
Op de GEREBASEDE kop hermeten: de gewapende verifier is md5
`4c82d1c9f9ab5168421503ea5f7b7f77` en weigert alle vijf met `WF-095 FAIL`.

*Armexacte mutant, hergedraaid op de gerebasede kop.* Arm uitgeschakeld
(md5 `79c77bbe9728e9d938344532af7d448a`) tegenover gewapend (md5
`4c82d1c9f9ab5168421503ea5f7b7f77`), volle vier-zone-sweep over **539**
documenten (golden 157, rood 22, negatief 330, pariteitszone 30 — 534 op de
pre-rebase kop, 528 vóór deze kaart): precies vijf regels veranderen, en
dat zijn deze vijf negatiefs. **Nul over-afwijzing**, nul onder-dekking; na
het terugdraaien staat de md5 weer op `4c82d1c9f9ab5168421503ea5f7b7f77`.

*Het recept, want de md5 alleen pint niets.* Die md5's zijn van het
GEBOUWDE binary, en dune bakt het buildpad erin: dezelfde broncode in een
andere map geeft een andere md5. Een md5 is dus een vingerafdruk van één
draai, geen reproduceerbaar artefact — hij pint alleen iets samen met het
recept dat hem maakte. Het recept hier is één snede in
`spec/verifier/lib/checks.ml`, in de `Literal`-arm van de expressiewandeling
(~r. 3740–3746): de tak
`| Some Datum_tijd, Some w when not (lexeme_datum_tijd w) -> [ v "WF-095" … ]`
verdwijnt, zodat een datum-tijd-literal doorvalt naar de `| _ -> []` eronder.
Wat blijft staan: het predicaat `lexeme_datum_tijd` zelf op r. 256 — het
heeft na de snede nul aanroepers, en de build accepteert dat (hermeten in
de fixronde: `dune build spec/verifier` slaagt zonder een tweede bewerking,
anders dan de `rec`-val die WF-092's mutant-notitie beschrijft) — plus de
WF-081-tak voor het getallexeem en de WF-084-tak voor het datumlexeem er
direct boven, en de rest van de `Literal`-arm (enumeratielabel,
tekenrestrictie). Eén tak eruit, geen tweede regel geraakt. Dat is
falsifieerbaar en overdraagbaar; de md5 is dat niet. Bewijs uit eigen huis:
lens 1 deed dezelfde arm-exacte snede in een andere checkout en kreeg md5
`e8f56085…` — een ANDER getal, met exact dezelfde vijf-regel-uitslag. Twee
md5's, één recept, één uitkomst: het recept is wat de meting draagt.
De fixronde heeft het recept zelf hergedraaid, in een `git archive
HEAD`-kopie: gewapend tegenover gemuteerd over dezelfde **539** documenten
van de vier zones geeft **exact vijf** gewijzigde regels — `WF-095 FAIL` →
`verifier: PASS` op precies de vijf `wf095-*`-negatiefs, en geen enkel ander
document beweegt.

Boundary geteld: de enige WELGEVORMDE `DatumTijd`-literals over de vier
zones zijn de twee van deze kaarts eigen golden (naast de vijf van haar
negatiefs), dus de regel treft geen enkel document van vóór zichzelf. Op de
case-wire staan nu 30 `dateTime`-waarden (25 publiek + 5 pariteitszone),
alle canoniek — de klemmingen bewegen er nul van.
Bovendien 29 lexemen kruislings getoetst: de ref-lezer en het
verifier-predicaat zijn het over alle 29 eens (beide spellingen,
kalendergeldigheid, voorloop-/naloopspatie).

*De herschreven passage.* Elke claim die "wordt waar na de fixronde" was,
is hermeten en herschreven: de lezerstabel draagt nu VOOR → NA per rij;
"Nothing on the IR wire enforces that grammar" is vervangen door de
WF-095-formulering met de meting die hem rechtvaardigde; "the reference
interpreter has no datetime-literal arm" en "the ASP backend faults ahead
of it" staan er nu als geschiedenis, met datum. Ook bijgetrokken: WF-084's
scope-zin, WF-036's prozaclausule en indexrij (de "missing regression
witness" bestaat nu: `jutterswet-meldstip`).

**Additieve suites** (maintainerbesluit 2026-09-06): `ref/test` 417 → 445
(het lexeem zelf: beide spellingen naar hetzelfde moment, de fractie als
hele milliseconden, elke geweigerde veldklasse, de kalendercontrole, en dat
de CASE-wire smaller is dan de literal-wire); `backends/clingo/test`
53 → 70 (de literaalvrije ordening, de lege operand, de literaalordening op
de millisecondegrens én op de dagrand, en de drie luide flanken), en in de
fixronde door naar **80** (de OF_GELIJK-familie exact op de drempel, F4;
beide operanden leeg, F13). Geen bestaande test gewijzigd.

**Gate-12 speelvloer (gesanctioneerde may-touch, één regel).** Sinds de
speelvloer-landing pint `scripts/check-drools.sh` `EXPECTED_PLAYED` naast
de kanaallijst. `jutterswet-meldstip` is een **speler**, dus hoort hij per
ontwerp NIET in de kanaallijst en beweegt alleen het getal: 110 → 111. Geen
andere regel van het script is aangeraakt.

**Gates, alle dertien vanuit de worktree op de gerebasede kop.** 1 PASS ·
2 PASS (179 files) · 3 PASS (179 golden clean, **330** negatief — 325 van
`main` plus deze kaarts vijf) · 4 PASS (434 casebestanden) · 5 PASS (412
cases) · 6 PASS (412 cases) · 7 PASS (376 weerleggen de no-op, 36 gepinde
survivors — GEEN nieuwe survivor: elk van de drie nieuwe cases draagt
minstens één aanwezige verwachting) · 8 PASS (22 rode cases) · 9 PASS (201
checks, 12 catala goldens) · 10 PASS (173 tests, 31 rgs-goldens) · 11 PASS
(45 .rgs byte-identiek, 2 holdouts, 27 tests, 29 replays, nul divergenties
— onbewogen) · 12 PASS (played **111**, kanaalvloer 46, auto-excluded 43,
value-divergence 1, no-case 2, JUnit 207) · 13 PASS (74 twists over 15
goldens, 0 survivors — ongewijzigd).

**Eerlijk over wat NIET gedekt is.**
- De Drools-case-lezer-klem heeft GEEN artefact in de suite. Het weiger-half
  is gemeten door de jar zelf (acht vormen, hierboven), maar geen
  JUnit-getuige legt hem vast; het niet-over-afwijzen wordt wél mechanisch
  bewaakt (gates 11 en 12 lezen 30 conforme waarden). De ref/ASP-kant kreeg
  zijn getuige in `ref/test`. Uitgeschreven als eigen kaartje:
  `backlog/new/20260906-drools-caselezer-getuige.md`.
- Datum-tijd (on)gelijk-aan blijft in de ASP-vertaling een luide flank met
  eigen reden (geen corpuscase); ref draagt hem wel.
- De emitterkant is onaangeraakt (`frontends/**` niet aangeraakt): een
  `DatumTijd`-literal krijgt nog het dag-eerste snippet in `waarde` én
  `bron`. WF-095 laat dat toe, dus de emitter is conform; de canonicalisatie
  is een eigen kaart met een herpin van de emitter-goldens.
- `spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt (rijvoorstel in het
  sessierapport).

---

## Fixronde (2026-09-06, na twee lenzen)

Twee lezingen over de landing heen; de landing houdt volledig — alles
reproduceert, dertien gates cijfer-exact. Wat de fixronde deed was
uitsluitend claims bijstellen die te sterk of te smal waren gezegd, plus
twee additieve suite-blokken en één presentatiecorrectie in de golden. Geen
productiesemantiek gewijzigd; de enige codewijziging buiten de suites is
een MELDINGSTEKST.

| # | wat | waar |
|---|---|---|
| F1 | het EDIT-RECEPT naast de mutant-md5's, met de les dat een md5 alleen mét recept pint (lens 1's arm-exacte snede gaf een ánder getal en dezelfde vijf regels) | deze kaart, §5 |
| F2a | de `dt_term`-catch-all-melding herschreven: de DRAAG-kant noemt nu ook de literal en de eerste/laatste over opgesomde operanden, de FLANK-kant noemt alle zes vormen bij naam | `backends/clingo/lib/translate.ml` |
| F2b | de flankenlijst uitgebreid met de parameter-lezing en de `Let`/`BinderRef`-vorm, mét meting | `spec/ASP-TRANSLATION.md` |
| F2c | de datum-tijd-parameter als zusje bij de parameter-carriage-passage, en "unscalable datatype" benoemd als tweeduidig | `spec/ASP-TRANSLATION.md` |
| F2d | beide nieuwe luide flanken + de twee-plekken-twee-meldingen-observatie | deze kaart, §2 |
| F3 | de scopezin naar WF-094-model: `waarde: null` BENOEMD, niet gesloten | `spec/IR-SPEC.md` (ch. 4.1 + indexrij) |
| F4 | de OF_GELIJK-familie op de literaalordening, exact op de drempel, incl. de KLEINER/GROTER-aliassen — acht armen ref ≡ clingo | `backends/clingo/test/test_clingo.ml` |
| F5 | "alle drie de toegelaten spellingen" gevolgd door vier vormen → beide grammatica's, vier spellingen | deze kaart, §1 |
| F6 | "schema-conform drie-cijferig" → "fractie afwezig, of exact drie cijfers", met de hertelling | deze kaart, §3 |
| F7 | twee kalenderrijen bij de VOOR → NA-tabel: de fix is breder dan geadverteerd | `spec/IR-SPEC.md` + deze kaart, §3 |
| F8 | de Z-rij krijgt zijn →-pijl in de Drools-kolom (kale exceptie → melding die de vorm noemt) | `spec/IR-SPEC.md` |
| F10 | de javadoc-claim "geen golden kan dit dragen" afgedateerd — `jutterswet-meldstip` IS die golden | `…/cases/DatumTijdLexeemWitnessTest.java` (uitsluitend javadoc) |
| F11 | `onderwerpBron`/`bron`: "zijn" → "haar" (de aanmelding is vrouwelijk; het corpus draagt de vorm al bij `Vondst`) | `corpus/golden/jutterswet-meldstip/ir.json` |
| F12 | A3 als eigen rij in de handberekening; en eerlijk gezegd dat de tweede grammatica geen wire-getuige heeft | `corpus/golden/jutterswet-meldstip/NOTE.md` |
| F13 | BEIDE operanden leeg onder EERDER → afwezig in beide | `backends/clingo/test/test_clingo.ml` |

**De familie-uitbreiding (F4).** De literaalordening was gepind op de
STRIKTE armen: een ms ervoor, exact erop, een ms erna, en de dagrand. Die
vier pinnen het DAG-symbool van de lexicografische helper en zeggen niets
over het ms-symbool op gelijke dagen, want zodra de dagen verschillen vuurt
de eerste regel en komt de tweede er niet aan te pas. De niet-strikte
familie zet precies daar zijn getuige: op de drempel zijn beide componenten
gelijk, dus alleen de tweede regel kan vuren en alleen zijn ms-symbool
beslist. Schrijf `<` waar `<=` hoort en alle vier de bestaande checks
blijven groen terwijl de grens stil omklapt. Vier spellingen —
`EERDER_OF_GELIJK`, `LATER_OF_GELIJK` en hun `KLEINER`/`GROTER`-aliassen,
die de wire op een datum-getypeerde vergelijking toelaat en die geen tweede
semantiek mogen zijn — elk in ref én clingo: acht armen, alle acht
ref ≡ clingo.

**Bevestigde niet-bevindingen** (lens 2 keek, vond niets, en dat versterkt
het verslag — elk hier hermeten):

- **WF-091 vangt de liegende annotatie.** Zet op de `AttribuutRef` van deze
  golden de annotatie `DatumInDagen` terwijl de declaratie `DatumTijd` is,
  en de verifier weigert het document met twee regels: `WF-036 FAIL` (date
  ordering across two date precisions) én `WF-091 FAIL … soort mismatch:
  attribute reference 'meldstip' is typed date-day, but the declaration it
  reads from is date-ms`. De annotatie-onverschilligheid van gate 13 rust
  dus hier op een regel die de leugen apart benoemt, niet alleen op de
  precisiebotsing.
- **De gelijkstelling draagt.** Een `GELIJKSTELLING` die een
  `DatumTijd`-attribuut SCHRIJFT vanaf een `DatumTijd`-literal is
  `verifier: PASS`, `oracle: PASS (1 expectations)` en
  `clingo: PASS (1 expectations)`. De landing dekt dus niet alleen de
  LEESkant van `DatumTijd`; de schrijfkant valt er zonder extra werk onder.
- **Beide-leeg is consistent met ch. 1.2d.** Twee lege datum-tijd-operanden
  onder `EERDER` geven afwezigheid in beide uitvoerders — geen `#sup`/`#inf`
  die als waarde ontsnapt, geen hulpatoom dat op een lege body afleidt. Nu
  ook vastgelegd (F13) in plaats van beredeneerd.

**De F11-pinmeting.** De golden draagt zes stringwijzigingen (twee
`onderwerpBron`, vier `bron`). Vóór en ná, op alle drie de casebestanden:
`oracle: PASS (4/6/4)` en `clingo: PASS (4/6/4)` — byte-identiek gedift.
`verifier: PASS`; gate 2 round-tript de 179 bestanden ongewijzigd; gate 12
speelt `jutterswet-meldstip` onveranderd (`PLAYED`, played 111). Nul pins
bewegen; het zijn presentatievelden en ze gedragen zich ernaar.

**De `waarde: null`-flank, hermeten met de jar erbij.** Eén bewerking op
deze kaarts eigen golden: `verifier: PASS`, ref
`EVALUATION FAULT: missing Literal.waarde in IR node`, clingo
`TRANSLATION FAULT: missing Literal.waarde`, en de bean-modeluitvoerder
`ERROR NullPointerException: Cannot invoke
"java.lang.CharSequence.length()" because "this.text" is null`. Twee
benoemde weigeringen en één dereferentie onder een document dat de wire
toelaat — benoemd in ch. 4.1 en in de indexrij, niet gesloten.

**Gates ná de fixronde, alle dertien vanuit de worktree.** 1 PASS · 2 PASS
(179 files) · 3 PASS (179 golden clean, 330 negatief) · 4 PASS (434
casebestanden) · 5 PASS (412 cases) · 6 PASS (412 cases) · 7 PASS (376
weerleggen de no-op, 36 gepinde survivors) · 8 PASS (22 rode cases) ·
9 PASS (201 checks, 12 catala goldens) · 10 PASS (173 tests, 31
rgs-goldens) · 11 PASS (45 .rgs byte-identiek, 2 holdouts, 27 tests, 29
replays, nul divergenties) · 12 PASS (played 111, kanaalvloer 46,
auto-excluded 43, value-divergence 1, no-case 2, JUnit 207) · 13 PASS (74
twists over 15 goldens, 0 survivors). Elk cijfer gelijk aan de landing; de
enige teller die beweegt is de clingo-suite, **70 → 80** (acht armen voor
F4, twee voor F13). `ref/test` blijft 445.
