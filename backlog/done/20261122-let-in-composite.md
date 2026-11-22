# let-in-composite: een lokale naam in een later deel van een voorwaarde, bij naam geweigerd

*Opgesteld door de hoofdsessie op 2026-11-22. Besluit van de eigenaar op dezelfde dag, na een onafhankelijke second
opinion: de combinatie hieronder wordt in de spec en in elke executor bij naam geweigerd, zodat de twee regels over een
binder van een `Let` uit ch. 1.5 vervallen.*

## Aanleiding

In een document met `inferFromEmpty: false` leest een samengestelde voorwaarde door voorbij een lid dat niet vaststaat;
zo'n later lid wordt voorlopig gelezen (ch. 1.5). Leest dat lid een lokale naam van een `Let` eromheen ("daarbij geldt"),
dan staat niet vast of die naam gelezen mag worden. Ch. 1.5 legt daarvoor twee regels vast: de barrière van een `Let` is
dan een open lezing, en wat het berekenen van de naam deponeert wacht op een gewone lezing. De eigenaar wil die regels
niet: geen enkele regelset kan ze gebruiken zolang de Drools-backend ze niet draagt, en in het corpus komt de vorm in één
golden voor (32 goldens met een `Let`; geen enkele leest de naam in een later lid van een samenstelling,
`jutterswet-grens-per-jutter` leest haar in het filter en in de toets van een kwantorvouw, zonder de eigenschap). De vorm
wordt daarom een vorm zonder lezing, die elke executor bij naam weigert. Is zij later nodig, dan komt er een eigen kaart.

Los daarvan staan in ref twee fouten in de lezing van ch. 1.5, gevonden doordat de clingo-vertaling op duizenden gevallen
naast ref gelegd werd:

1. ref houdt een attribuut en een kenmerk van één naam niet uit elkaar waar het om open leden gaat (`World.opened`,
   `open_inputs`: sleutel op label en genormaliseerde naam). Voorbeeld: een Vondst met het attribuut *toeslag*, dat een
   defaults-groep van *gemerkt* laat afhangen, en het kenmerk *toeslag*, dat een defaults-groep aan een volledig kenmerk
   geeft; *gemerkt* open. Ch. 1.5: het attribuut is open, het kenmerk staat vast als niet. ref noemt beide open. Waar het
   kenmerk invoer is en het attribuut geschreven wordt, noemt ref het kenmerk vaststaand afwezig waar ch. 1.5 het open noemt.
2. ref verliest een sluiting waar de barrière van een `Let` in de voorwaarde van een `IfThenElse` valt. Voorbeeld: "het
   keurgewicht van een Jutter is, daarbij geldt: t is de som van het gewicht van zijn vondsten waarvan getuigen leeg is, 1
   als t gelijk is aan 3, anders t", over een vondst met een volledig leeg gewicht en open getuigen. Een waarde die na een
   sluiting leeg is, laat haar doel open (ch. 1.5): het keurgewicht is open. ref geeft vaststaand leeg.

## Wat er komt

1. **De vorm zonder lezing.** Een rij in de tabel "Forms without the reading" van ch. 1.5, als laatste, met de slug
   `let-in-composite`: een `Let` (ch. 10.6) waarvan een voorwaarde in de romp de binder leest in een lid van een
   samenstelling dat niet haar eerste is, op elke diepte, of in het filter of de toets van een kwantorvouw. Een document
   met die vorm wordt geweigerd zoals elke vorm in de tabel (REF-014 met de slug).
2. **De twee regels vervallen uit ch. 1.5**: de zinnen over de barrière en over de binder in "The judgement of a condition"
   (en in de kwantorvouw, waar zij genoemd worden), met hun voorbeelden. Het voorlopig lezen zelf blijft (oordeel telt,
   deponering vervalt, fout maakt het lid onbepaald). De examples `WV-1.5-past-undetermined-barrier` en `-binder`
   vervallen; wat zij aan de ref-suite toevoegden, wordt vervangen door getuigen van de weigering (zie Acceptance).
3. **ref verkleint**: `provisional` op de uitgestelde binder, het omzetten van de barrière in `judge_cond` en het
   uitstellen van deponeringen van een binder vervallen; `Let_barrier` en de lezing van een `Let` in een gewoon document
   blijven byte-gelijk.
4. **De twee fouten in ref hersteld**, elk met een getuige die vóór het herstel rood is.

## Niet in deze kaart

clingo (de branch `card/open-values-in-clingo` neemt de slug over bij zijn landing) en Drools (neemt hem over in het
volgende bouwdeel). Deze kaart raakt `ref/` en ch. 1.5.

## May touch

`spec/RUNTIME-SEMANTICS.md` ch. 1.5 (en de verwijzingen ernaar in ch. 3.9a en ch. 5.12 die de vervallen voorbeelden noemen),
`ref/lib/`, `ref/test/`, `ref/README.md` waar het de vorm noemt. Dit is mandaat van de eigenaar voor dit hoofdstuk.

## Acceptance

- Getuigen `WV-1.5-let-in-composite-*`: een samenstelling die de naam in een later lid leest, één die haar in een genest
  later lid leest, een filter en een toets van een kwantorvouw, worden geweigerd met de slug; een `Let` waarvan de romp de
  naam alleen in het eerste lid, of buiten elke voorwaarde leest, speelt onveranderd; zonder `inferFromEmpty: false` speelt
  elk van deze documenten zoals nu.
- Getuigen voor fout 1 (attribuut en kenmerk met één naam, beide kanten) en fout 2 (sluiting en barrière), elk rood op de
  code van vóór het herstel.
- De suite van ref telt niet minder dan 489 voorbeelden (gate 5); elke vervallen getuige staat in het Result met de reden.
- Gates 1, 3, 5, 6, 7, 8 (ref-been), 14 groen; gate 5 met dezelfde tellingen voor behoud en monotonie als op main
  (280 goldens, 784 cases; 2255 runs, 401 sluitingen), of met de gemeten reden waarom een getal verschuift.

## Result

### Wat er is

| commit | inhoud |
|---|---|
| `9dd217a48` | fout 1: de open stand van een lid staat onder object en lid, het lid met zijn soort (`World.member`: `Attribute`, `Characteristic`, `Relation`); `open_inputs` vraagt de schrijvers per soort; ch. 1.5 "The three states" noemt attribuut en kenmerk van één naam twee leden |
| `6c3e913ee` | fout 2: de grens van een oordeel of waarde (`open_read_boundary`) geeft met de barrière van een `Let` ook de geregistreerde sluiting door; ch. 1.5 "A closure inside a value" |
| `691c760d8` | de vorm `let-in-composite`: de rij in de tabel van ch. 1.5, de weigering in `refuse_uncarried`, de getuigen; de twee regels en hun voorbeelden uit ch. 1.5, de verwijzingen uit ch. 5.12 |
| `465ae8024` | ref verkleind: de binding van een `Let` is weer `Later of (unit -> Value.t)`, `judge_cond` vangt bij een voorlopig gelezen lid alleen een fout |
| `f8c4f94ed` | `written_member`: het lid dat een regel schrijft, op één plaats naar zijn soort |
| `7d4e7c8f4` | de berekening van een binder is één waarde met haar eigen grens: elke lezing meldt haar sluiting, en een berekening met een open lezing deponeert niets; ch. 1.5 "A closure inside a value" |
| `1739f19bb` | `World.Open_read` draagt het lid met zijn soort; `open_text` zoekt de spelling bij die soort; ch. 5.12 |
| `19241c33d` | de drie floors van gate 5, met mandaat van de hoofdsessie voor precies die drie regels |
| `0a9ef6db0` | de slug in `UncarriedForms` van de Drools-backend, `TABLE` 23, en de vorm in haar wandeling met de grens van ref, met mandaat van de hoofdsessie voor die klasse en `InferFromEmptyRefusalWitnessTest` |

De weigering is een wandeling over het document, zonder evaluatie: per binder van een omsluitende `Let` houdt zij bij of
de plaats binnen de romp van die `Let` in een later lid van een samenstelling of in een kwantorvouw staat. Een binder met
dezelfde naam die dieper gebonden is, verbergt de buitenste; een `Let` binnen een later lid wordt niet geweigerd om een
lezing van zijn eigen binder.

### Waarom alleen de barrière de sluiting doorgeeft

De barrière van een `Let` is een uitkomst: de waarde van die `Let`, leeg. De evaluatie die daar uitkwam, paste de
sluiting toe, dus voor een ander staat die leegte niet vast. Een fout heeft geen waarde en geen oordeel, dus er is geen
uitkomst die op de sluiting rust. Wordt een fout gevangen, dan is dat bij een voorlopig gelezen lid (`judge_cond ~past`):
het lid is dan onbepaald en telt niet mee, en een ander lid kan de samenstelling beslissen. Gaf de fout haar sluiting aan
de grens eromheen, dan heette de waarde van de vorm na een sluiting ontstaan terwijl geen deel van haar op die sluiting
rust: een vaststaand leeg doel werd open. Getuige `WV-1.5-closure-before-a-fault`; een mutant waarin elke uitzondering de
sluiting doorgeeft, geeft daar "open" waar "leeg" verwacht is, en geen andere getuige valt. Een fout die niet gevangen
wordt, beëindigt de evaluatie van de vorm zonder uitkomst (`fault_evaluation`).

### Een gewoon document

Zonder open lezing doet `open_read_boundary` niets anders dan de berekening zonder grens: de deponeringen gaan in hun
volgorde door, een fout gaat door met de deponeringen van daarvoor, en er is geen sluiting te melden. Gemeten: elke case
van `corpus/golden` en `corpus/red` (1313, waarvan 1300 zonder de eigenschap en 70 daarvan met een `Let`) door de runner
van deze branch en die van de basis, als replay, met `--end-state` en met `--trace`: uitvoer, foutuitvoer en exitcode
byte-gelijk, 0 verschillen. Gate 5 speelt 1211 goldencases met hun verwachting en gate 7 weerlegt er 1169.

### De getuigen per eis

Alle in de ref-suite, die gate 5 draait: 4217 voorbeelden (4082 op `main`; floor 489).

| eis | getuige | stand op de code van vóór de wijziging |
|---|---|---|
| een later lid leest de naam | `WV-1.5-let-in-composite-later-member` | speelt (rood) |
| op elke diepte | `-later-member-of-a-first-member`, `-first-member-of-a-later-member`, `-choice-in-a-later-member`, `-let-value-in-a-later-member`, `-under-a-let` | spelen (vijf keer rood) |
| het filter en de toets van een kwantorvouw | `-quantifier-fold-filter`, `-quantifier-fold-matrix`, `-quantifier-fold-matrix (a nested Let)`, op de regels 1, 0 en 2 van de golden `jutterswet-grens-per-jutter` met de eigenschap | spelen (drie keer rood) |
| zonder de eigenschap speelt elk zoals nu | dezelfde zes documenten met vier of drie situaties elk en absolute waarden; de golden op zijn twee cases | groen vóór en na |
| de naam alleen in het eerste lid, of buiten elke voorwaarde | `WV-1.5-let-in-composite-not-*` (14 rijen) en `-filter-of-a-fold` (regel 3 van de golden) | groen vóór en na |
| fout 1 | `WV-1.5-shared-name-*`: 15 rijen (twee defaults-groepen, twee regels, kenmerk als invoer, attribuut als invoer, een gemaakt object) | 9 rood, en 2 rijen van `WV-1.5-monotone` |
| fout 2 | `WV-1.5-closure-before-a-barrier-*`: 11 rijen | 4 rood, en 5 rijen van `WV-1.5-monotone` |
| een fout geeft niets door | `WV-1.5-closure-before-a-fault` | groen vóór en na; rood op de mutant hierboven |
| elke lezing van een binder draagt haar sluiting | `WV-1.5-closure-of-a-binder-*` | `-open` rood ("not", verwacht "open") en 2 rijen van `WV-1.5-monotone` rood ("bijzonder stond vast als niet" verdwijnt met een ingevuld gegeven) |
| een afgebroken berekening deponeert niets | `WV-1.5-binder-broken-off-*` | `-open` rood ("0 [uitkering V1 EF-005]", verwacht "0 []") |
| een open lezing noemt haar lid | `WV-5.12-open-shared-name` | rood (het kenmerk "toeslag" heette "Toeslag") |
| tabel en code noemen dezelfde vormen | `WV-1.5-uncarried-forms` | groen |
| Drools noemt de vorm waar ref haar noemt | `InferFromEmptyRefusalWitnessTest`: `the_list_opens_with_the_table_of_the_spec`, `a_let_read_in_a_later_member_of_a_composite_is_named` (zes documenten, en de weigering op beide branches), `a_let_read_in_a_quantifier_fold_is_named` (regels 0, 1 en 2 van de golden; regel 3 is `set-fold`), `a_let_read_elsewhere_is_not_named` (vijf documenten) | de eerste drie rood (22 tegen 23; `if-then-else` en `quantifier-fold` in plaats van `let-in-composite`), de vierde groen |

Twee hulpen van de suite zijn mee: de functie `written` in `WV-1.5-monotone` vraagt de schrijvers per soort lid, en de
lijst van benoemde sluitingen heeft vier rijen van fout 2 (een volledig gewicht dat weggelaten wordt, en een vouwfilter op
leeg over een open waarde).

### Wat vervallen is

- Norm: de twee regels van ch. 1.5 met hun voorbeelden, 19 regels; in ch. 5.12 de barrière van een voorlopig gelezen lid,
  twee zinsdelen, en haar voorbeeld, 3 regels.
- Code: `465ae8024` haalt 57 regels uit `ref/lib/eval.ml` en zet er 22 voor terug.
- Getuigen: `WV-1.5-past-undetermined-barrier` (4 voorbeelden) en `WV-1.5-past-undetermined-binder` (12), omdat hun
  documenten de vorm hebben en nu geweigerd worden; `WV-5.12-open-past-undetermined (barrier)` (1), om dezelfde reden;
  met hen de 23 rijen van `WV-1.5-monotone` en `WV-1.5-preserved` over hun situaties. Zij zijn vervangen door
  `WV-1.5-let-in-composite-*`.

### Gates

- Gate 1, 3, 5, 7 en 14, op de inhoud van `19241c33d` vóór de rebase op `977b89a24`: PASS. Gate 3: 510 golden clean, 657 negative rejected. Gate 5: suite 4217, 1211
  cases replayed exactly, 279 goldens en 782 cases behouden, 152 goldens geweigerd per vorm (`let-in-composite` 1), 2226
  monotonie-runs, 401 met een sluiting apart. Gate 7: 1169 cases, 42 survivors. Gate 14: 164 checks, 3 goldens.
- Gate 6, op de inhoud van `f8c4f94ed`: PASS (backend suite 3231 checks, 1169 cases ref = clingo, 12 goldens in het weigerkanaal).
- Gate 8, op de inhoud van `465ae8024`, met een PATH zonder `mvn`: het ref- en het clingo-been en `check-exec-codes` zonder een rode
  regel; het Drools-been is niet gedraaid, en dat is de ene FAIL-regel van die run.
- Na de rebase: de ref-suite (4217) en `InferFromEmptyRefusalWitnessTest` (`Tests run: 9, Failures: 0, Errors: 0,
  Skipped: 0`) groen; de andere tests van de Drools-backend zijn niet gedraaid.

### Gate 5: waarom drie getallen verschuiven

| telling | `main` | nu |
|---|---|---|
| goldens die ref met de eigenschap draagt | 280 | 279 |
| hun cases | 784 | 782 |
| monotonie-runs | 2276 (floor 2255) | 2226 |
| runs met een sluiting apart | 401 | 401 |

De ene oorzaak is de golden `jutterswet-grens-per-jutter`: vier van haar zes regels lezen de binder van een `Let` in de
toets van een kwantorvouw en één in het filter. Haar strikte kopie, die alleen gate 5 maakt, is nu geweigerd
(`let-in-composite 1` in de samenvatting); 2 cases en 2 × 25 runs vervallen. Zonder de eigenschap speelt zij in elke gate
als tevoren.

De floors zijn 279, 782 en 2226, met de reden per regel (`scripts/check-oracle.sh`).

### Voorlopige besluiten

1. De weigering is syntactisch: ook een samenstelling waarvan geen eerder lid onbepaald kan worden (alleen literals of
   parameters) wordt geweigerd als een later lid de binder leest.
2. Het filter van een kwantorvouw wordt niet voorlopig gelezen; het is geweigerd omdat de kaart het noemt.
3. De soort van een lid is een sum type (`World.member`), en een relatie is er een van; de sleutel leunt niet meer op een
   naam die geen lid kan spellen.
4. Ch. 1.5 krijgt een voorbeeld voor elk van de twee fouten (`WV-1.5-shared-name-*`, `WV-1.5-closure-before-a-barrier`,
   `WV-1.5-closure-before-a-fault`), zodat clingo en Drools dezelfde tekst hebben. De lezing van een binder (hierboven)
   verandert ref voor een vorm die clingo draagt.
5. Er staat geen vangnet voor een barrière die een voorlopig gelezen lid verlaat: `evaluate` weigert de vorm vóór het
   evalueren.

### Voorgestelde wijziging van `spec/CONSTRUCTS.md`, rij 48

De zin "… a fault leaves it undetermined, and a barrier of an enclosing Let that leaves it is an open read (ch. 1.5); a
creation on a cycle is refused by name (`creation-cycle`), and refuses a document with another form by name …" wordt: "… a
fault leaves it undetermined (ch. 1.5); a `Let` whose binder a member of a composite other than the first, or the filter
or the matrix of a quantifier fold, reads is refused by name (`let-in-composite`), and so is a creation on a cycle
(`creation-cycle`); it refuses a document with another form by name …". Bij de witnesses: `WV-1.5-let-in-composite-*`,
`WV-1.5-shared-name-*`, `WV-1.5-closure-before-a-barrier`, `WV-1.5-closure-before-a-fault`,
`WV-1.5-closure-of-a-binder-*`, `WV-1.5-binder-broken-off-*`, `WV-5.12-open-shared-name`.

De alinea's van de gates in `CLAUDE.md` veranderen niet.

### Open punten

1. clingo (`card/open-values-in-clingo`) neemt over: de slug `let-in-composite`, attribuut en kenmerk van één naam als twee
   leden, de sluiting die met de barrière meegaat, en de binder die bij elke lezing op de sluiting van haar berekening
   rust en bij een afgebroken berekening niets deponeert.

## Landing

`make gates` op `0160b042e` (met de registerrij): alle 17 gates groen, 732 s, after-run groen. Een tweede lezer (Google Neon,
alleen lezen) vond de vormgrens van `let-in-composite` precies passend en één randgeval: een deponering van een binder die een
afgebroken atomaire voorwaarde las, blijft staan, ook waar de voorwaarde open is. Dat is nagedraaid en schendt de monotonie
niet (het feit staat er ook met het gegeven); de berekening van een binder is één waarde met haar eigen grens, zoals ch. 1.5
zegt. Open voor de eigenaar: of zo'n feit bij een open voorwaarde hoort weg te vallen.

De weigering raakt ruimer dan de kaart bij de aanvang zei: een samenstelling in de romp van een `Let` kan haar binders alleen
in het eerste lid lezen (de voorwaarde van een `IfThenElse` leest alleen binders, parameters en literals, WF-077), en een
filter met "en" in de romp van een `Let` met de naam in een later lid wordt geweigerd. In het corpus één golden (de strikte
kopie van `jutterswet-grens-per-jutter`), in geen document met de schakelaar.
