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
`hour 25 is out of range` voor een onmogelijk veld. Alle drie de toegelaten
spellingen (`2026-03-15T12:00:00.000`, `2026-03-15T12:00:00`,
`15-03-2026 12:00:00.000`, `dd. 15-03-2026 12:00:00.000`) spelen exact
hetzelfde document na.

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

**3. De drie lezers klemmen zelf.** Rood-eerst, per lezer gemeten, precies
de vormen uit de kaarttabel:

| vorm | literal-lezer | Drools-case-lezer VOOR → NA | gedeelde case-lezer VOOR → NA |
|---|---|---|---|
| `…T12:00:00.1` | weigerde al | 100 ms → weigering | 1 ms → weigering |
| `…T12:00:00.100000` | weigerde al | 100 ms → weigering | 100 000 ms → weigering |
| `…T12:00:00.1234` | weigerde al | 123,4 ms → weigering | 1 234 ms → weigering |
| `2026-3-15T09:00:00` | weigerde al | parse-crash → weigering die de vorm noemt | 09:00 op 15 maart → weigering |
| `…T12:00` (geen seconden) | weigerde al | 12:00:00 → weigering | weigering → weigering met positie |
| `…T12:00:00Z` / `+01:00` / spatie / rommel | weigerde al | weigering | weigering |

De gedeelde lezer (`ref/lib/case.ml`) deelt nu de canonieke ISO-arm van
`Value`; de Drools-lezer (`CaseRunner.waardeNaarArg`) klemt op het
schema-patroon met de vorm in de melding, als HARDE fout en nooit een
`Excluded` (dat zou de golden stil uitsluiten). Gate 4 blijft staan — het
schema is nu het tweede hek. **Nul bestaande cases bewegen:** alle 24
`dateTime`-waarden (19 publiek + 5 in de pariteitszone) zijn
schema-conform drie-cijferig, geteld en getoetst; gate 11 draait onbewogen
(29 replays door ref, clingo en Drools, nul divergenties).

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
de millisecondegrens én op de dagrand, en de drie luide flanken). Geen
bestaande test gewijzigd.

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
  zijn getuige in `ref/test`. Voorstel voor een eigen kaartje: een
  Java-getuige naast `DatumTijdLexeemWitnessTest`.
- Datum-tijd (on)gelijk-aan blijft in de ASP-vertaling een luide flank met
  eigen reden (geen corpuscase); ref draagt hem wel.
- De emitterkant is onaangeraakt (`frontends/**` niet aangeraakt): een
  `DatumTijd`-literal krijgt nog het dag-eerste snippet in `waarde` én
  `bron`. WF-095 laat dat toe, dus de emitter is conform; de canonicalisatie
  is een eigen kaart met een herpin van de emitter-goldens.
- `spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt (rijvoorstel in het
  sessierapport).
