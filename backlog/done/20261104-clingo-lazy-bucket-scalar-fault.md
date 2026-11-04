# clingo-lazy-bucket-scalar-fault: EF-037 alleen waar de vorm geëvalueerd wordt

*Opgesteld door de hoofdsessie op 2026-11-04, uit het Result van stap B van `defaults-per-period-on-timeline` (vraag 1).*

## Aanleiding

EF-037: een vorm die een hele tijdlijn leest, vindt alleen de emmerscalair van de rekendatum. ref en Drools geven die fout waar de vorm
geëvalueerd wordt. Een gevolg van een default dat op geen dag aan de beurt is, evalueren ze niet, dus geen fout (RS ch. 7.11,
`WV-7.11-gevolg-buiten-de-beurt`: met kustwacht en winterwacht in april 2025 **5**, **60** in april, **5**, geen fout). De ASP-backend leest
de emmerscalair al tijdens het vertalen en faalt op beide cases met EF-037. Dat is een fout waar de norm er geen kent, geen weigering van
de backend; gate 6 laat zoiets op een golden niet toe, maar het document staat alleen als fixture
(`ref/test/fixtures/defaults-per-day/lazy-consequence`), dus geen gate ziet het. RS ch. 7.11 noemt het nu als feit.

## Goal

clingo geeft EF-037 alleen waar ref en Drools het geven: waar de vorm die de tijdlijn leest, geëvalueerd wordt (een gevolg op een dag van
zijn beurt waar zijn rechtvaardiging geldt; een voorwaarde of waarde die bereikt wordt). Kan de codering dat niet dragen, dan weigert
clingo met een code van het soort `refusal` of `limit` uit `backends/clingo/codes.tsv`, nooit met een EF-code. Meet eerst elke plaats waar
clingo EF-037 (of een andere EF-code) bij het vertalen geeft, en vergelijk met ref op dezelfde documenten: dezelfde vroege fout buiten de
defaults hoort bij deze kaart.

## Acceptance

- (a) Rood eerst: de twee cases van `lazy-consequence` als golden (de case zonder fout) en als rode casus (EF-037, `attribute kustloon`,
  `label J1`) in het corpus; op main faalt clingo de golden met EF-037, daarna speelt hij (of staat hij met een refusal-code in het kanaal
  van gate 6, met reden). ref en beide Drools-branches spelen beide.
- (b) Elke andere plaats van een vroege EF-code in clingo: gemeten, en opgelost of als vraag in het Result.
- (c) RS ch. 7.11 zonder de zin over de ASP-backend (voorstel); ASP-TRANSLATION en CONSTRUCTS rij 8 als huidige stand.
- (d) `make gates` groen.

May touch: `backends/clingo/`, `corpus/golden/`, `corpus/red/`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `scripts/check-clingo.sh`
(alleen het kanaal; voorstel), kaartverplaatsing.

## Omvang

Klein.

## Result

*Bouwer, 2026-11-04. Niet gemerged, niet gepusht. Logs onder `werk/clingo-lazy-bucket-scalar-fault/logs/`, de scripts van
meting en mutaties onder `werk/clingo-lazy-bucket-scalar-fault/scripts/` (`instrument.py`, `measure.py`, `mutations.py`).*

### Commits

| hash | inhoud |
|---|---|
| `fc4faa529` | kaart naar `doing/` |
| `ba7440df5` | clingo: EF-037 als `viol/2`-getuige onder het bereik van de lezing (`translate.ml`), suite, README |
| `e27cb4ae0` | corpus: golden `jutterswet-gevolg-buiten-de-beurt`, rode casus `jutterswet-emmerscalair-gevolg`; `scripts/check-drools.sh` (beschermd, voorstel) `EXPECTED_PLAYED` 329 → 330 |
| `402c0a0c5` | RUNTIME-SEMANTICS (beschermd, voorstel): ch. 7.11 zonder de zin over de ASP-backend, de corpusankers, de getuige in de rij van EF-037 |
| `e4bfe54a5` | ref: de lezing (ch. 5.12) legt een fout in de periode-aritmetiek aan de knoop (`period_timeline`); ref-tests `WV-5.12-default-per-dag-gevolg-faalt` en `-som` |
| `3c0f9d39d` | RUNTIME-SEMANTICS (beschermd, voorstel): ch. 5.12 het uitgewerkte voorbeeld van die twee lezingen |
| (deze) | Result |

### Meting van de vroege EF-codes (vóór de wijziging, op `fc4faa529`)

Werkwijze: een kopie (`git worktree add --detach`, opgeruimd) waarin de runner en de suite elke fout met een EF-code loggen die
`Translate.translate` gooit, met de plaats uit de backtrace; de suite schrijft per zo'n vertaling het paar (ir, case) weg. Door de
runner: alle 1252 paren met een `ir.json` ernaast onder `corpus/golden`, `corpus/red`, `ref/test/fixtures` en
`backends/drools/src/test/resources`, plus de zes fixture-cases waarvan het document elders staat (`defaults-per-day-cases/*`,
`period-arithmetic/sum-member-without-timeline`). Daarnaast de clingo-suite (2514 checks). Elk gelogd paar ging daarna door ref.
Logs `04-measure/early-table.tsv` (vóór) en `18-measure-after/early-table.tsv` (na, op `402c0a0c5`).

**Vóór: 66 vroege EF-fouten** (41 uit paren, 25 uit de suite): EF-037 30, EF-014 7, EF-004 6, EF-011 5, EF-019 3, EF-020 3,
EF-021 3, EF-010 2, EF-022 2, EF-024 2, EF-012 1, EF-013 1, EF-018 1. Op 63 ervan faalt ref met dezelfde code (parameters gelijk,
behalve `place` van EF-004, dat de ASP-backend niet noemt; RS ch. 5.7b). Waar ref speelt, op drie:

1. `defaults-per-day/lazy-consequence/case.json`, EF-037 `kustloon`/`J1` — deze kaart.
2. Suiteframe `README [L1]: the horizon fence behind a condition that fails (hf-cond)`, EF-014 `minimumgewicht`: het filter van
   een per-element-totaal wordt bij het vertalen gelezen.
3. Suiteframe `README [L1]: the day-kind table behind a condition that fails (dk-cond)`, EF-014 `grensdatum`: de dagsoorttabel
   wordt bij het vertalen gebouwd.

De plaatsen in `translate.ml` op `fc4faa529` en hoe ref de fout geeft:

| code | plaats(en) | ref | stand |
|---|---|---|---|
| EF-037 | `refuse_bucket_scalar` (11 aanroepen: periode-aritmetiek eigen attribuut en som, voorwaarde per periode, pro-rata-basis, knip waarde en vergeleken tijdlijn, per-element-totaal, kwalificator eigen, over een pad en in een vergelijking, invoer van de initialisatiedefault per dag) en `require_attr_timeline` (puntlezing) | bij evaluatie van de vorm | **opgelost**: `viol/2` onder het bereik |
| EF-014 | periode-aritmetiek, tijdsafhankelijke parameter (r. 11449) | bij evaluatie | **opgelost**: `viol/2` onder het bereik |
| EF-014 | filter van het per-element-totaal (r. 5425), dagsoorttabel (r. 8002) | bij evaluatie | gemeten, README [L1]; vraag 1 |
| EF-010, EF-011, EF-019, EF-021, EF-022 | `de dag`/`BoundDay` buiten een dagsoort; contextwaarde zonder rekendatum (duur, clamp, negatie onderaan een stapel, evaluatiejaar); `Unresolved`; gevuld-telling over een tijdsafhankelijke verzameling; geneste dagsoorttoets op een andere datum | bij evaluatie van de knoop | gemeten, overal gelijk aan ref; vraag 2 |
| EF-018, EF-020, EF-024 | dubbelzinnige terugkoppeling (via `Eval.connecting_pair`); tijdsafhankelijke uniciteitssleutel als scalair; twee tijdsafhankelijke sleutels | bij de creatie, per instantie van de uniciteit | gemeten, overal gelijk; verschil alleen waar ref de vorm op geen instantie draait; vraag 2 |
| EF-004, EF-012, EF-013 | invoer past niet (case); geldigheid zonder rekendatum (`Eval.applies`, per vorm); Parameterset dekt de rekendatum niet | bij het laden, per vorm, bij de start van de sessie | gelijk door constructie |

**Na: 34 vroege EF-fouten** (23 uit paren, 11 uit de suite), geen EF-037 meer; waar ref speelt alleen nog de twee frames van
README [L1]. De 18 paren die verdwenen, geven hun fout nu bij het oplossen.

### Wat er veranderde

- **`bucket_scalar_fault` / `bucket_scalar_guard`** (`translate.ml`): EF-037 is een `viol/2`-getuige per instantie die de
  emmerscalair geeft, in ref's zin, met die instantie als subject (dus `viol_code` leest `label`), en een body die zegt dat de
  lezing bereikt wordt plus de atomen die zeggen dat de lezing die instantie is (`S = "J1"`, het pad, of het subject van de
  verzameling). Het bereik per plaats:
  - periode-aritmetiek (eigen attribuut, som over de leden met de join), pro-rata-basis: `isa` en de voorwaarde die de regel
    poort (`cond`); in een default per dag is dat `ndN_jrK(S)`, bereikt en gerechtvaardigd op een periode;
  - voorwaarde per periode: `isa` en `ctx.rule_guard` bij binnenkomst (op een regel leeg, in een default `ndN_rchK(S)`);
  - invoer van de initialisatiedefault per dag: `ndN_rchK(S)` van die default (de vroege controle in
    `translate_defaults_timeline` is weg);
  - puntlezing (met de bindingen van de datum: ref leest alleen op een gevulde datum), knip waarde en vergeleken tijdlijn,
    kwalificator eigen/pad/vergelijking, per-element-totaal: `rule_reach` van de knoop, het bereik van de parameterbewaking
    (genomen tak, open prefix, Let-barrières, `param_reach` van een default).
  - Hangt een atoom van dat bereik aan de volgorde van gemaakte leden (`order_dependent`: `ctx.named` of `ctx.pre_reach`), dan
    blijft de fout bij het vertalen, over elke instantie (VB-2).
  - De puntlezing van een **afwezig** attribuut blijft een fout bij het vertalen, zonder code (VB-3).
- **EF-014 in de periode-aritmetiek**: een ongebonden tijdsafhankelijke parameter is een `viol/2` onder `isa` en `cond`. Een
  scalaire operand wordt nu onder `ctx.rule_guard <- cond` vertaald, zodat ook zijn parameterbewaking de voorwaarde van de regel
  draagt; die gaf EF-014 waar ref de waarde niet evalueert (probe `13-red-scalar-param.log`; VB-4).
- **`viol_code`**: zeven armen lezen EF-037 met `attribute` uit de zin en `label` uit het subject.
- **`refuse_bucket_scalar`** is weg, `require_attr_timeline` houdt alleen de afwezige arm, `translate_defaults_timeline` verloor
  de ongebruikte parameter `~owner`.
- **README**: de default per dag (wanneer gevolg, rechtvaardiging en invoer geëvalueerd worden) en een item over EF-037 en
  EF-014 onder het bereik van de lezing.
- **Corpus**: golden `jutterswet-gevolg-buiten-de-beurt` (case `case1-buiten-de-beurt`: J1 **5** op alle tijd, J2 **5**,
  **60** in april 2025, **5**, consistent) en rode casus `jutterswet-emmerscalair-gevolg` (EF-037, `kustloon`, `J1`), beide
  met het document en `source.rgs` van `lazy-consequence` (de frontend schrijft `ir.json` byte-gelijk terug) en een NOTE.md.
  ref, beide Drools-branches (`PLAYED 1`, rood: `FAULT EF-037 {"attribute":"kustloon","label":"J1"}`) en clingo spelen beide.
- **ref, de lezing** (`eval.ml`, buiten "May touch", VB-7): gate 17 vergeleek de nieuwe golden en vond `ref
  /rules/prioritizedDefaults/0/defaults/1 "J2" 1 AttributeReference unreached` tegen `backend … fault EF-037` (beide branches).
  Ch. 5.12 leest het gevolg waar de rechtvaardiging op een dag geldt, ongeacht de beurt, en een knoop draagt de fout die hij geeft
  of doorgeeft; Drools volgt dat, ref niet: `period_timeline` meldde een fout niet aan de observer, ook niet aan de wortel, die
  zijn aanroeper alleen bij succes vastlegt. Nu legt elke knoop die als tijdlijn gelezen wordt (binaire operator, tijdsafhankelijk
  eigen attribuut, som over de leden, afronding, tijdsafhankelijke parameter) zijn fout vast via `observer.expr`; een scalaire
  operand doet dat al via `eval_expr`. Buiten een lezing (geen observer) verandert er niets. Ref-tests
  `WV-5.12-default-per-dag-gevolg-faalt` (de golden, J2) en `-som` (het gevolg "zijn kustloon plus 1": `1 Binary fault EF-037`,
  `2 AttributeReference fault EF-037`, `3 Literal unreached`); `rows` kreeg `?of_label` voor een case met meer subjecten.

### Tests die de kaart omdraait (oud → nieuw)

Veertien frames verwachtten de fout bij het vertalen; ze verwachten haar nu bij het oplossen, met dezelfde naald en strenger op
de code (`faults_at_solving`: het document vertaalt, clingo zegt UNSATISFIABLE, een afgevuurde getuige draagt de naald en
`viol_code` geeft code en parameters):

- `§7.1 set operand, member reached through the reverse leg (ch. 5.7a)` en `§7.1 horizon rule: a member with an
  asOfDate-bucket scalar (by name)`: oud `faults_with` (vertaalfout met naald), nieuw ook `EF-037` met `bewaartarief` en `J1`
  resp. `V1`;
- `period arithmetic: parameter-unbound`: oud `faults_with`, nieuw ook `EF-014` met `verzuimrentevoet`;
- de elf `bucket scalar: <fixture>`: oud een `Fault` met code `EF-037`, de parameters en de naald in de tekst, nieuw
  `viol_code` = (`EF-037`, dezelfde parameters) en de naald in de getuige.

Nieuw (+34 checks, 2514 → 2548): elke fixture van `bucket-scalar` behalve `condition` achter de voorwaarde 1 > 2 (`gated`), ref
en clingo spelen; idem `parameter-unbound`; een ongebonden scalaire operand van de periode-aritmetiek, met en zonder poort;
`lazy-consequence` in beide executors en de bereikte case als fout; een rechtvaardiging per periode van een default die nooit
aan de beurt is (speelt) en wel (EF-037); de initialisatiedefault met de emmerscalair als invoer (EF-037) en onder een opgave
op alle tijd, nooit aan de beurt (speelt).

### Rood eerst (letterlijk)

- Gate 6 op de code van `fc4faa529` met de nieuwe golden (`07-red-gate6-main.log`): `FAIL:
  corpus/golden/jutterswet-gevolg-buiten-de-beurt/case1-buiten-de-beurt.json does not replay through clingo (EF-037 at the head
  of the report, a fault where ref plays): TRANSLATION FAULT: EF-037: the period arithmetic of ch. 3.4/3.5 reads the temporal
  attribute "kustloon" of "J1" whole; …`.
- De nieuwe suite tegen de bibliotheek van `fc4faa529` (`14-red-suite-main.log`): `test_clingo: 2548 checks, 32 FAILURES`,
  o.a. `defaults per day: a consequence not evaluated: clingo pins the values (fault: the period arithmetic …)`, `bucket scalar:
  arithmetic: expected a fault at solving, got one in translation (…)`, `period arithmetic: a scalar parameter not bound, behind
  a condition that fails: clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)`.
- De probe van de scalaire operand op `fc4faa529` (`13-red-scalar-param.log`): ref `oracle: PASS`, clingo `EF-014: clingo:
  UNSATISFIABLE — … viol("parameter "toeslag" is unbound in this case …","J1")`.
- De eerste `make gates` op `402c0a0c5` (`gates-1.log`, `out/gates/20261104-170153/`): `17-readings rc=1`, `check-readings:
  FAIL — jutterswet-gevolg-buiten-de-beurt/case1-buiten-de-beurt.json (exec): the trace and the readings differ: ref
  /rules/prioritizedDefaults/0/defaults/1 "J2" 1 AttributeReference unreached / backend … fault EF-037`, idem `(drl)`; de
  andere zestien gates groen.
- De nieuwe ref-tests tegen de bibliotheek van `402c0a0c5` (`23-red-ref-test.log`): `FAIL
  WV-5.12-default-per-dag-gevolg-faalt`, `got: … "J2" 1 AttributeReference unreached …` (en `-som`).

### Groen

clingo-suite `test_clingo: 2548 checks, 0 failures` (was 2514); ref-suite `test_ref: all 2391 worked examples pass` (was
2389); gate 6 los (`17-gate6.log`): `1064 cases replay exactly through clingo`; Drools-suite in gate 12 van `make gates`
(hieronder).

### Mutaties (niet gecommit, vier `git worktree add --detach`, opgeruimd; `19-mutations/summary.txt`)

Elke mutatie maakt de suite rood (2548 checks):

| mutatie | rood (eerste frame) |
|---|---|
| M01 geen getuige periode-aritmetiek eigen attribuut | `defaults per day: a consequence evaluated`, `bucket scalar: arithmetic` |
| M02 geen getuige som over de leden | `bucket scalar: sum` |
| M03 geen getuige voorwaarde per periode | `… a justification of a default at its turn`, `bucket scalar: condition` |
| M04 geen getuige pro-rata-basis | `bucket scalar: pro-rata` |
| M05 geen getuige puntlezing | `bucket scalar: point-read` |
| M06 / M07 geen getuige knip waarde / vergeleken tijdlijn | `bucket scalar: clip-value` / `clip-compared` |
| M08 geen getuige per-element-totaal | de twee `§7.1`-frames, `bucket scalar: per-element` |
| M09 / M10 / M11 geen getuige kwalificator eigen / pad / vergelijking | `qualifier-own` / `qualifier-path` / `qualifier-comparison` |
| M12 geen getuige invoer initialisatiedefault | `… the initialisation default reads a bucket scalar input` |
| M13 geen getuige tijdsafhankelijke parameter | `period arithmetic: parameter-unbound` |
| R01 bereik periode-aritmetiek zonder `cond` | `a consequence not evaluated`, `a justification of a default never at its turn`, `arithmetic, behind a condition that fails` |
| R02 bereik voorwaarde per periode zonder de default | `a justification of a default never at its turn` |
| R03 bereik invoer zonder de beurt | `an initialisation default never at its turn` |
| R04 / R07 bereik puntlezing / per-element alleen `isa` | `point-read, behind …` / `per-element, behind …` |
| R05 scalaire operand zonder `cond` | `a scalar parameter not bound, behind a condition that fails` |
| R06 bereik parameter zonder `cond` | `parameter-unbound, behind a condition that fails` |
| V01–V07 de arm van `viol_code` voor elke zin weg | de frames van die zin (`faults at solving` zonder code) |

In ref (`25-ref-mutation-*.log`): de vastlegging in de arm van het eigen attribuut weg → `FAIL
WV-5.12-default-per-dag-gevolg-faalt` en `-som`; die in de binaire arm weg → `FAIL WV-5.12-default-per-dag-gevolg-faalt-som`.
De armen som, afronding en tijdsafhankelijke parameter hebben geen eigen getuige.

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS ch. 7.11** en de rij van EF-037 in ch. 5.7b: in `402c0a0c5`; **ch. 5.12** (het voorbeeld van de default per
dag, `WV-5.12-default-per-dag-gevolg-faalt` en `-som`): in `3c0f9d39d`.

**ASP-TRANSLATION**, de puntlezing (r. 1471–1473), vervangt "A point read needs the full timeline in the case; the
asOfDate-bucket scalar cannot serve it and faults (the ch. 4.3a convention: executors fault, never guess).":

> A point read needs the full timeline in the case (the ch. 4.3a convention: executors fault, never guess): the asOfDate-bucket
> scalar faults with EF-037 where the read is reached (below), an absent attribute at translation.

**ASP-TRANSLATION**, de periode-aritmetiek (r. 1537–1543), vervangt de zin "A form that reads a timeline whole — … — faults at
translation with EF-037 … over every instance of the type.":

> A form that reads a timeline whole — an own temporal attribute, the members of a sum, the tested attribute of a condition per
> period, the base of a pro-rata part, the value and the compared timeline of a gedurende clip, a member of a per-element total,
> a point read, the timeline of the whole-period qualifier, also at the end of a path, and the input of an initialisation
> default per day — faults with EF-037 where the case gives the asOfDate-bucket scalar (RUNTIME-SEMANTICS ch. 8.4, 5.7b), where
> ref evaluates the form: one `viol/2` witness per instance that gives the scalar, in ref's sentence and with the instance as
> its subject, whose body is the reach of the read — the condition that gates the rule, the reach of a default's consequence or
> of its justification and the initialisation default's input (S10), or inside a condition or a value the reach of the
> unbound-parameter guard (S5) — with the path or the set from the subject to the instance. An unbound parameter of the period
> arithmetic, time-dependent or a scalar operand, faults (EF-014) under the same reach. Where an atom of that reach hangs on the
> order of the members a creation adds, the fault is raised at translation, over every instance that gives the scalar.

**ASP-TRANSLATION** S10 "Per day on a timeline target", na "A deposit of a default stands where it decides on some period.":

> A consequence is evaluated where its default is reached and justified on some period (`ndN_jrK/1`), a justification and the
> input the initialisation default reads where its default is reached on some period (`ndN_rchK/1`), and a fault in them fires
> only there (`corpus/golden/jutterswet-gevolg-buiten-de-beurt`, `corpus/red/jutterswet-emmerscalair-gevolg`).

**CONSTRUCTS rij 8**, vervangt "In ref, clingo and both branches of the Drools backend (golden `jutterswet-opgave-per-periode`,
documents `ref/test/fixtures/defaults-per-day`, cases `ref/test/fixtures/defaults-per-day-cases`); the ASP backend faults while
it translates on a read of the asOfDate-bucket scalar in a consequence that ref and the Drools backend do not evaluate
(`lazy-consequence`).":

> In ref, clingo and both branches of the Drools backend (goldens `jutterswet-opgave-per-periode` and
> `jutterswet-gevolg-buiten-de-beurt`, red case `jutterswet-emmerscalair-gevolg`, documents `ref/test/fixtures/defaults-per-day`,
> cases `ref/test/fixtures/defaults-per-day-cases`); a read of the asOfDate-bucket scalar in a consequence faults (EF-037) only
> where the consequence is evaluated.

en achteraan de landing: "2026-11-04, card `clingo-lazy-bucket-scalar-fault`: the ASP backend raises EF-037 where the form is
evaluated."

**CONSTRUCTS rij 11** en **CLAUDE.md**: geen wijziging nodig. Rij 11 zegt alleen "is EF-037 (ch. 8.4)"; de tekst van gate 6
noemt geen fout bij het vertalen en die van gate 12 geen getal van `EXPECTED_PLAYED`.

### Voorlopige besluiten

- VB-1: alle twaalf plaatsen van EF-037, niet alleen de default per dag ("dezelfde vroege fout buiten de defaults"); de getuige
  heeft de gelezen instantie als subject, één regel per instantie met de scalair, en wordt alleen gebouwd waar de case zo'n
  instantie geeft, dus een document zonder zo'n instantie krijgt geen regel erbij (wel draagt een scalaire operand van de
  periode-aritmetiek nu de voorwaarde van de regel, VB-4).
- VB-2: hangt het bereik aan de volgorde van gemaakte leden, dan de fout bij het vertalen (het oude gedrag) in plaats van
  tweelingen met een eigen zin of een nieuwe code; geen document in corpus of suites komt daar.
- VB-3: de puntlezing van een afwezig attribuut (zonder code) blijft bij het vertalen; buiten de meting van EF-codes.
- VB-4: de scalaire operand onder `cond` meegenomen: dezelfde functie, dezelfde fout (EF-014 waar ref speelt), twee regels.
- VB-5: corpusnamen `jutterswet-gevolg-buiten-de-beurt` (golden) en `jutterswet-emmerscalair-gevolg` (rood, naast de drie
  `jutterswet-emmerscalair-*`); casus Art. 3m (eigen tekst).
- VB-6: `scripts/check-drools.sh` stond niet in "May touch"; de gelijkheidspin `EXPECTED_PLAYED` moest mee omdat de golden in
  gate 12 speelt (d). Voorstel op een beschermd pad, met een regel in de commentaargeschiedenis.
- VB-7: `ref/lib/eval.ml` en `ref/test/test_ref.ml` stonden niet in "May touch". De golden die de kaart vraagt, maakte gate 17
  rood op een afwijking van ref van ch. 5.12, niet op een vraag van de norm (Drools volgt de tekst); een regel in `DEVIATIONS`
  van gate 17 had een bekende foute lezing vastgelegd. Dus ref gerepareerd, met de vastlegging in elke arm die een tijdlijn leest
  (de tekst: "the fault the node raised or passed on"), niet alleen in de arm die de golden raakt.

### Vragen aan de hoofdsessie

1. README [L1]: het filter van het per-element-totaal en de dagsoorttabel lezen hun parameter bij het vertalen en geven EF-014
   waar ref speelt (frames `hf-cond` en `dk-cond`). Gate 6 laat zo'n EF-code op een golden niet toe; nu raakt geen golden ze.
   Een weigering met een code van het soort `refusal` of `limit` (de kaartregel), of lui maken (een eigen kaart: de tabel wordt
   bij het vertalen gebouwd)?
2. EF-010, EF-011, EF-019, EF-021 en EF-022 vallen bij het vertalen van de knoop, waar ref ze geeft als hij de knoop evalueert;
   EF-018 en EF-020/EF-024 bij het vertalen van de creatie en de uniciteit, ook waar ref die op geen instantie draait. Op elk
   document van corpus en suites gelijk aan ref; achter een voorwaarde die faalt niet. Dezelfde behandeling (een `viol/2` onder
   `rule_reach`) in een vervolgkaart, of vastleggen als grens in de README?
3. VB-2: is de fout bij het vertalen bij een bereik dat aan de volgorde van gemaakte leden hangt goed genoeg, of wil je daar een
   weigering met een eigen code?
4. Drools leest een operand die in een gevolg van periode-aritmetiek de fout geeft als niet bereikt: op de golden met het gevolg
   "zijn kustloon plus 1" (probe, geen corpus) geven beide branches `1 Binary fault EF-037`, `2 AttributeReference unreached`,
   waar ch. 5.12 en ref (`-som`) `2 AttributeReference fault EF-037` lezen. Geen golden raakt het; een Drools-kaart, of een
   golden met zo'n gevolg zodat gate 17 het ziet?

### make gates

Een run aan het eind in de worktree op `3c0f9d39d` (`out/gates/20261104-171426/`, log `gates-2.log`); de commit daarna raakt
alleen deze kaart. (De run daarvoor op `402c0a0c5`, `out/gates/20261104-170153/`, was rood op gate 17, zie Rood eerst.)

```
start 2026-11-04 17:14:26 3c0f9d39d card/clingo-lazy-bucket-scalar-fault
ocaml rc=0 3s ocaml-build: rebuilt (4 source path(s) newer than the stamp, e.g. ref/lib; 3s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 560 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 131s
04-case-schema rc=0 9s
03-verifier rc=0 42s
05-oracle rc=0 24s
06-clingo rc=0 278s
07-noop rc=0 15s
08-red rc=0 166s
09-catala rc=0 5s
10-regelspraak rc=0 253s
11-toka-parity rc=0 227s
12-drools rc=0 352s
13-reftype rc=0 321s
14-flogic rc=0 2s
15-graph rc=0 52s
16-scenario rc=0 41s
17-readings rc=0 266s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 17:20:21 355s red=0
```

Gate 5: ref-suite 2391, 1068 cases. Gate 6: clingo-suite 2548, 1064 cases. Gate 8: 68 rode cases (was 67). Gate 12: JUnit 1846
tests, `played 330`, DRL-tekst `played 330`. Gate 17: 331 goldens, 939 case files, 7393 lezingen, 24093 knoopregels, 3886 niet
bereikt.
