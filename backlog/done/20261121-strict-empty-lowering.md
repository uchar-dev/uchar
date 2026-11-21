# strict-empty-lowering: de strikte lezing van leeg als omzetting van IR naar IR, met voorstel en proef

*Opgesteld door de hoofdsessie op 2026-11-21, na het akkoord van de eigenaar voor stap 2 van het advies "Leeg, onbekend
en niet van toepassing" (werkbank, `werknotities/20261121-null-logica/rapport.md`, §3 en §4). Stap 1, de meting, staat
in `backlog/done/20261121-empty-closures-inventory.md`.*

*Bij het lezen: de kop hieronder (Aanleiding tot en met "De omzetting per vorm") is de opdracht zoals zij tijdens de
bouw stond. De metingen van de kaart hebben drie regels eruit herzien: de toets op gevuld is geen sluiting, een lezing
over een afwezige link staat vast, en een afgeleid attribuut dat geen schrijver vult is vaststaand leeg en niet open
(dus ook: een som over geen lid is vaststaand leeg, geen 0). De lezing zoals zij nu geldt staat in
`spec/proposals/STRICT-EMPTY.md`; het Result zegt wat gemeten is. De code van de proef is niet geland.*

## Aanleiding

De eigenaar besloot op 2026-11-20: uit leeg mag niets afgeleid worden. Het is een eis aan het uchar-dialect: geen
closed-world assumption, het onderscheid tussen leeg en de twee markers behouden, en een lezing die uitlegbaar is voor
engineers en regelanalisten. De meting van stap 1 (`verifier --empty-closures`) telt 1331 plekken in het corpus en 206
tot 621 in de regelset v2 waar de huidige lezing een uitkomst op een lege waarde laat rusten. De regelset ving dat per
plek af; dat is een tussenstand die moet vervallen, met gelijke uitkomsten.

Het advies stelt een strikte lezing voor: leeg betekent "staat niet vast" en geeft door, een voorwaarde over leeg is
onbepaald (Kleene's sterke tabellen), en de wereld sluit alleen waar een vorm het zegt. Het verwacht dat die lezing
als omzetting naar de bestaande, tweewaardige IR te schrijven is, zonder de executors te wijzigen. Dat is niet gemeten.
Deze kaart schrijft het voorstel uit en meet het.

## Goal

1. **Het voorstel** `spec/proposals/STRICT-EMPTY.md`: de regel in drie zinnen, de tabellen, de omzetting per vorm, de
   sluitingen, het sluiten per gegeven, de opgave, wat de proef weigert, en de beslispunten voor de eigenaar met een
   aanbeveling en de meting erbij.
2. **De referentie-omzetting**: `Strict_empty.document : document -> document` in de bibliotheek van de verifier, en de
   modus `verifier --strict-empty <ir.json>`, die het omgezette document op de wire schrijft of bij naam weigert.
3. **Een golden** `jutterswet-strikt-leeg` met handberekende verwachtingen, gespeeld door ref, clingo en beide
   Drools-branches.
4. **Drie metingen**: wat `--empty-closures` op een omgezet document nog meldt; monotonie in de invoer; en v2 van vóór
   het afvangen, strikt, tegenover v2 van erna.

## Acceptance

- `verifier --strict-empty <ir.json>` schrijft een verifier-schoon document, of weigert met exit 3 en per geweigerde
  vorm haar naam en pointer. Zonder de modus verandert niets: `make gates` groen, en geen bestaand corpusbestand
  wijzigt.
- **Niets rust op leeg.** Op elk omgezet document meldt `verifier --empty-closures` geen plek van klasse `negation`, en
  in klasse `closure` alleen uitdrukkelijke sluitingen en plekken waarvan elk lid onder een toets op gevuld staat. Wat
  overblijft, staat per soort verklaard in het Result.
- **Monotonie.** Voor elke case van de golden en van de corpusgoldens die omzetten: maak één invoerlid leeg en speel
  het omgezette document in ref. Elke waarde, elk kenmerk en elk §9.5-feit van die run staat ook in de run met de
  volledige invoer. Elk verschil is een fout in de omzetting of een benoemde sluiting.
- **De golden** speelt in ref, clingo en beide Drools-branches met de verwachtingen van de case-bestanden, en een
  controle houdt haar `ir.json` byte voor byte gelijk aan de omzetting van haar gewone document.
- **Het corpus.** Hoeveel goldens zetten om zonder weigering; welke vormen weigert de proef en hoe vaak; hoeveel cases
  van de omgezette goldens houden hun verwachting; elk verschil ingedeeld.
- **v2.** De situaties van de scenario's van `8edee01` gespeeld op twee documenten: `8edee01` zoals het is (de
  referentie met het handmatige afvangen) en `229d744` strikt omgezet. Per scenario en per uitkomst: gelijk, of het
  verschil verklaard. Het aantal scenario's dat zonder het handwerk dezelfde uitkomst geeft, is de maat.
- **Wat het kost.** De grootte van het omgezette document tegenover het gewone, en de bouwtijd van Drools op v2.

## May touch

- `spec/proposals/STRICT-EMPTY.md` (nieuw);
- `spec/verifier/lib/strict_empty.ml` (nieuw), `spec/verifier/lib/empty_closures.ml` (alleen waar de afvanging van
  een omgezette vorm herkend moet worden), `spec/verifier/bin/verifier.ml`, `spec/verifier/README.md`,
  `spec/verifier/test/strict-empty/` (nieuw);
- `corpus/golden/jutterswet-strikt-leeg/` (nieuw), en wat een nieuwe golden in de suites en gates vraagt (de pins van
  de Drools-suite, `EXPECTED_PLAYED` van gate 12);
- `scripts/check-verifier.sh` (een poot in gate 3) en de alinea's van de geraakte gates in `CLAUDE.md`: een voorstel,
  dat de hoofdsessie landt;
- deze kaart.

## Out of scope

- De frontends: hoe een RegelSpraak-document zegt dat het strikt is, en de omzetting in de Java-frontend, horen bij de
  stap van het landen. Deze kaart raakt geen frontend.
- De executors. Dat zij ongewijzigd blijven is de hypothese die de proef toetst: weigert een executor een omgezette
  vorm, dan is dat een uitkomst van de meting, met vorm en aantal in het voorstel, en geen reden om de executor aan te
  passen.
- `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, het schema: normatieve tekst komt bij het landen.
- Tijdlijnen, de verdeling, dagsoorten, de lijst- en vouwfamilie en gesloten bodies: de proef weigert ze bij naam.
- De uitleg, de dienst, de scenariotaal, de toolchain, de werkbank, regelspraak-rules.
- Geen naam of afspraak van een regelset in code, tests, golden of voorstel; v2 staat alleen in de meting.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): akkoord voor stap 2: het voorstel en een proef, zonder dat een bestaand document
verandert. Daarna besluit hij over het landen.

Van de hoofdsessie (voorlopig, ter bevestiging):

- **De proef is een omzetting van IR naar IR, in OCaml, naast de ontsuikering van ch. 7.7.** Zo geldt zij voor elk
  document, van welke frontend ook, draait zij direct op de drie documenten van v2, en blijft de Java-frontend buiten
  de proef. Bij het landen wordt zij de referentie waartegen de omzetting in de frontend gehouden wordt. Het advies
  noemde een vlag in de frontend; dit wijkt daarvan af.
- **Het omgezette document gebruikt alleen bestaande knopen** en is positief: geen ontkenning van een afgeleid kenmerk.
- **Een opgave is gesloten door haar declaratie.** Onder "opgave boven berekening" beslist de berekening als de opgave
  leeg is: wie een attribuut "mag opgegeven worden" noemt, zegt dat een ontbrekende opgave geen opgave is. De omzetting
  herkent de stapel aan de herkomst van haar leden (IR-SPEC ch. 2.13) en meldt haar als sluiting.
- **Een lezing over een link die er niet is, is geen lege invoer.** Een relatie is gesloten, ook voor wie over een
  afwezige link leest: de lezing telt niet mee in `d`, en de vorm waarin zij staat, leest haar zoals de lezing van nu
  doet. Alleen een lezing over een link die er wél is en waarvan het attribuut leeg is, is onbepaald. De hoofdsessie
  herzag deze regel op 2026-11-21 na de meting op de regelset (de eigenaar, kaart van stap 1: pas als een marker is
  gegeven of een object of een relatie ontbreekt, mag worden doorgerekend).
- **Een kenmerk dat geen vorm toekent en dat de case niet noemt, is onbepaald.** De case zegt "onwaar" door het
  gegenereerde ontkennende kenmerk te noemen.
- **Sluiten per gegeven is in de proef een parameter van de modus**, geen syntaxis: een lijst van leden die als
  volledige invoer gelden. Het voorstel noemt de vormen die het dialect ervoor kan krijgen.

### De omzetting per vorm

Voor een voorwaarde `c` maakt de omzetting twee voorwaarden: `c⁺` (staat vast dat wel) en `c⁻` (staat vast dat niet).
Voor een expressie `e` maakt zij `d(e)`: de voorwaarde dat elke lezing waar `e` van afhangt gevuld is. De expressie
zelf blijft staan: met gevulde operanden rekenen beide lezingen gelijk.

| vorm | ⁺ | ⁻ |
|---|---|---|
| vergelijking `a op b` | `d(a)` en `d(b)` en `a op b` | `d(a)` en `d(b)` en `a op' b`, met `op'` de complementaire operator |
| `is leeg` | voor de eigen vorm `is leeg` (een sluiting); voor een andere vorm onwaar | `is gevuld` |
| `is gevuld` | `is gevuld` | voor de eigen vorm `is leeg` (een sluiting); voor een andere vorm onwaar |
| `alle` | elk deel⁺ | ten minste één deel⁻ |
| `ten minste k` van n | ten minste k delen⁺ | ten minste n − k + 1 delen⁻ |
| `geen` | elk deel⁻ | ten minste één deel⁺ |
| `ten hoogste k` van n | ten minste n − k delen⁻ | ten minste k + 1 delen⁺ |
| `precies k` van n | ten minste k delen⁺ en ten minste n − k delen⁻ | ten minste k + 1 delen⁺, of ten minste n − k + 1 delen⁻ |
| kenmerkcheck `K` | `K` | het gegenereerde kenmerk `K⁻` |
| ontkende kenmerkcheck | `K⁻` | `K` |
| toets op een marker | de toets | het attribuut is gevuld en draagt een andere constructor |
| kwantorvouw `alle` met filter `F` en toets `T` | voor elk lid: `F⁻` of `T⁺` | er is een lid met `F⁺` en `T⁻` |
| kwantorvouw `geen` | voor elk lid: `F⁻` of `T⁻` | er is een lid met `F⁺` en `T⁺` |
| de tellende kwantorvouwen | met tellingen van de leden die vaststaand wel en vaststaand niet voldoen, tegen het aantal leden | idem |

- **"X is leeg" staat alleen vast voor de vorm die het zelf schreef.** In de wandeling die de eigen beslisvoorwaarde
  van een vorm maakt (`c⁺` van een regel, een default, een creatie; `a⁻` van een consistentieregel), is de kant van een
  toets die "X is leeg" betekent een sluiting: de auteur schreef haar, direct of door zijn eigen ontkenning. In elke
  wandeling die het tegendeel van een voorwaarde maakt ten behoeve van een andere vorm (`K⁻`, "geen hogere default
  geldt", "geen eigen uitzondering", een gedeelde voorwaarde daarvan), is die kant nooit vastgesteld, behalve voor een
  gesloten lid en over een afwezige link. De kant die "X is gevuld" betekent, is in beide wandelingen de toets op
  gevuld. Een sluiting die een auteur over een afgeleid lid schrijft, blijft en wordt geteld. De hoofdsessie herzag
  deze regel op 2026-11-21 na de meting op de regelset.
- **Een regel** vuurt op `c⁺` en `d(waarde)`.
- **Een kenmerk** `K` met toekenningen onder `c₁ … cₙ`: `K` blijft (elke toekenning onder haar `cᵢ⁺`), en `K⁻` wordt
  toegekend als elke `cᵢ⁻` geldt, over alle vormen die `K` aan dat object kunnen toekennen, de vormen op een supertype
  en een subtype inbegrepen. Een default op een kenmerk met besluit "niet toekennen" die beslist, geeft `K⁻`.
- **Een default** `i` in een stapel beslist onder `cᵢ⁺`, `d(waardeᵢ)` en, voor elke hogere default `j`, `cⱼ⁻`. Een
  hogere default die geldt met een lege waarde, of die onbepaald is, laat het doel leeg. Een uitzonderingsgroep telt
  als hoger dan de eigen inhoud van haar default (ch. 7.9). De initialisatie blijft de laagste default en is een
  sluiting.
- **Een vouw over een verzameling**: de verzameling is gesloten. `d` van een som, minimum, maximum, eerste of laatste
  is: voor elk lid staat vast dat het filter niet geldt, of het filter geldt en de waarde is gevuld. Het filter van de
  vouw wordt `F⁺`. Een som over de lege verzameling is 0; een minimum of maximum is leeg. `d` van een telling is: voor
  elk lid staat het filter vast.
- **Rekenen**: `d` van een operator is `d` van elke operand; ook van de duur bij een datum, een grens van `begrensd`,
  een tekstdeel en de deler. Een deling door leeg is leeg en geen fout; een deling door 0 blijft een fout.
- **Een consistentieregel** meldt alleen als haar bewering vaststaand onwaar is.
- **Een lid dat als volledige invoer geldt**, telt niet mee in `d`.

Een vorm buiten deze lijst weigert de proef bij naam.

## Result

*Geschreven door de bouwers van deel 1 tot en met 5 op 2026-11-21; de landing staat onderaan.*

**De derde stand (deel 5).** Een attribuut is op een object gevuld, vaststaand leeg of open. Een afgeleid attribuut is
vaststaand leeg waar elke schrijver vaststaand niets vult (zijn voorwaarde is vaststaand onwaar, of hij geldt met een
vaststaand lege waarde; een stapel waarvan elke default niet geldt of doorvalt); waar alle lezingen van een uitdrukking
of een atomaire voorwaarde vaststaan, rekent zij zoals de gewone lezing. De lezing is gehouden aan twee eisen:
**monotonie** (geen enkele regel niet geplaatst, op getuigen, golden en corpus) en **behoud bij volledige kennis** (met
elk invoerlid gesloten speelt 582 van de 593 corpuscases zijn eigen verwachtingen; de 11 andere zijn vier beperkingen
van de proef; op de regelset 248 van 265 situaties gelijk aan de gewone lezing van dezelfde versie, 19 leden open, van
één van die beperkingen). Alle getallen hieronder zijn van deel 5, tenzij er "deel 3" of "deel 4" staat.

**De code van de proef landt niet.** De eigenaar bouwt door op de route waarin de wire het document van de auteur
draagt en elke backend de strikte lezing zelf toepast, en houdt van deze kaart alleen wat nuttig is voor wie een
regelset ontwikkelt; code die daarna niet gebruikt wordt, bewaart hij niet (koerswijziging, 2026-11-21). Daarom landen
niet: de omzetting `Strict_empty` en de modus `verifier --strict-empty`, haar getuigen en verslaggetuigen, de golden
`jutterswet-strikt-leeg` met het omgezette document en haar DRL-pin, de poten in gate 3, 5 en 10 en `EXPECTED_PLAYED`
van gate 12. De branch `card/strict-empty-lowering` blijft staan als werkmateriaal waar de bouwers van de backends uit
lezen. **Wat landt:** het voorstel `spec/proposals/STRICT-EMPTY.md`, als ontwerp waar de bouwkaarten van de backends
naar verwijzen, en deze kaart als verslag.

### Commits

Branch `card/strict-empty-lowering`, op `main` `d815fa675`. Deel 1 tot en met 3:

| commit | wat |
|---|---|
| `0f4d23860`, `eba483abb` | de kaart, geclaimd |
| `bf992c31a` | dertien getuigen van de kern, vóór de omzetting |
| `4666bba4a` | `spec/verifier/lib/strict_empty.ml`, de modus `verifier --strict-empty`, de README |
| `dbcfdb1d6` | zeven verslaggetuigen (`report/`) |
| `798daa44c` | een toets op gevuld die de auteur al schreef, niet nog eens |
| `f8d698023` | het voorstel `spec/proposals/STRICT-EMPTY.md` |
| `fa4bb6c42` | tien getuigen van de stapel, de vouwen, Let, `--closed`, vóór de vormen |
| `8aa95b667` | de defaults-stapel, de uitzonderingsgroepen, de initialisatie, de opgave, de kwantorvouw, de vouwen, Let, `--closed` |
| `4c8ca98ee` | de golden `jutterswet-strikt-leeg` (ir.json, ordinary.json, ordinary.rgs, NOTE.md, 4 cases) en haar DRL-pin |
| `332d0f68f` | `scripts/check-drools.sh`: `EXPECTED_PLAYED` 383 → 384 |
| `0511c2998`, `153199428`, `1c2527cfd` | getuige en vorm van een operand die door een fout leeg is; een binder als zijn waarde in de toets |
| `fd163f9f2` | het voorstel, §5–§12 |
| `ab0ca24fb`, `4fb64117f` | getuigen en vorm van een toets op leeg of gevuld van een berekende waarde; `initialisation-value` |
| `2ee3ef54c`, `683e5fe16` | getuige en vorm van een opgesomde eerste/laatste/minimum/maximum zonder een vouw zonder lid |
| `1a8580c69` | de README van de verifier |
| `1bb5fcf5e`, `8d2c291ee`, `c72f20830` | de poten in gate 3, 5 (`scripts/lib/strict-empty-oracle.py`) en 10 |
| `1df0fd98c`, `4c10764b0`, `8a3be567f` | het voorstel, het Result, de route van het landen |

Deel 4:

| commit | wat |
|---|---|
| `d9140ba91` | getuigen `filled-test-not-closure`, `absent-link` en `report/closure-over-derived`, `navigated-read` case3 naar de nieuwe regel; rood op de omzetting van dat moment |
| `9bebcc931` | het pad van twee stappen in een eigen getuige (`absent-link-path`); getallen van `filled-test-not-closure` binnen het bereik van clingo |
| `0ea844f1d` | de twee correcties: een toets op gevuld is geen sluiting voor een andere vorm; een lezing over een afwezige link is gesloten |
| `e18f38b70` | een pad van twee stappen via een gegenereerd kenmerk `<rol> (ontbreekt)`, zonder de vouw over een enkelvoudige rol die Drools weigert |

Daarna het voorstel en dit Result.

Deel 5:

| commit | wat |
|---|---|
| `86b5c2129` | drie getuigen van een vaststaand leeg afgeleid attribuut (`settled-empty`, `-stack`, `-fold`), vóór de vorm; rood |
| `b679e6bd1` | de derde stand in `strict_empty.ml`: `A (leeg)`, "d = staat vast", doorval met een vaststaand lege waarde, geen toets van de knoop zelf meer, geen terugval 0, `--closed-inputs`; de gewijzigde getuigen |
| `71e3b5c08` | wat de meting van behoud vond: een `Let`, een creatie, een payload-toegang, een gedateerde groep, het filter van een vouw |
| `af88dfce4` | getuige `settled-empty-date`, vóór de correctie; rood |
| `5d6ddd5b3` | een lege datum geeft door in datumrekenen (gevonden met de regelset) |
| `77395ffb7` | verslaggetuigen `report/settled-empty-across-subtypes` (weigering) en `report/settled-empty-by-creation` (notitie), geschreven na de code |

Daarna het voorstel en dit Result.

### De twee correcties van deel 4

1. **"X is leeg" staat alleen vast voor de vorm die het zelf schreef.** De omzetting bouwt elke voorwaarde in een
   wandeling: de eigen (`c⁺` van een regel, een default, een creatie; `a⁻` van een consistentieregel) of een andere (`K
   (niet)`, "geen hogere default", "geen eigen uitzondering", de gedeelde voorwaarden daarvan). De kant "X is leeg" is
   in de eigen wandeling de sluiting die de auteur schreef, in een andere de constante onwaar (`none [all []]`), behalve
   voor een gesloten lid en over een afwezige link. Dat geldt ook voor de toets van een berekende waarde, de telling van
   gevulde waarden en een filter dat op gevuld toetst. De toets op een marker en de opgave blijven zoals ze waren. Een
   sluiting over een afgeleid lid krijgt `note closure-over-derived`.
2. **Een lezing over een afwezige link is gesloten.** `d` van een genavigeerde lezing is "de link ontbreekt (de
   ontkende rolcheck van de stap), of de lezing is gevuld"; een pad van twee of meer stappen: de eerste link ontbreekt,
   of het object dat zij bereikt draagt het gegenereerde kenmerk dat de rest ontbreekt. Een vergelijking over zo'n
   lezing beslist als de gewone lezing; waar die operator en complement allebei onwaar maakt, komt de lege kant in `c⁻`
   (de tabel per operator staat in het voorstel §5.8). Een genavigeerde kenmerkcheck: `c⁻` is "de link ontbreekt, of
   het object aan de andere kant draagt `K (niet)`"; de ontkende vorm is over een afwezige link onwaar, zoals nu. Een
   lid dat de aanroeper sluit, wordt in een vergelijking op dezelfde manier gelezen (bijvangst: zo'n vergelijking bleef
   eerder open op een leeg gesloten lid).

### De derde stand (deel 5)

- **`A (leeg)`**: een gegenereerd kenmerk per afgeleid attribuut dat een lezing nodig heeft, op het type dat het
  attribuut declareert, met per type dat het schrijft één regel `A (leeg) van U`: elke regel daar vult vaststaand niets
  (`c-`, of `c+` met `d(waarde)` en de waarde leeg); per stapel: elke default geeft vaststaand voorrang (`c-`, of `c+`
  met een vaststaand lege waarde) en de initialisatie, als die er is, heeft een vaststaand lege waarde. Naam naar het
  patroon van `K (niet)`. Waar geen schrijver het kan geven (een letterlijke waarde zonder voorwaarde), wordt het niet
  gedeclareerd en vallen zijn lezingen weg.
- **`d` van een lezing** is "gevuld, of vaststaand leeg" (over een pad: of een link ontbreekt; een pad van twee stappen
  via een gegenereerd kenmerk `A van <rest> (leeg)` op het type van de eerste stap). Waar `d` geldt, blijft de knoop van
  de auteur staan en rekent zoals de gewone lezing; de toets van de knoop zelf (deling, macht, wortel, datum uit delen)
  en de voorwaarde "ten minste één lid haalt het filter" van een minimum/maximum/eerste/laatste zijn weg: een lege
  uitkomst op vaststaande operanden is vaststaand leeg, met haar deponering. Een som zonder terugval krijgt geen 0 meer.
- **De toets op leeg** voor een andere vorm staat vast waar X vaststaand leeg is: `A (leeg)` en de toets.
- **Een vergelijking**: de kant die vaststaand leeg kan zijn (een afgeleid attribuut, een lezing over een link, een
  gesloten lid, een vouw, een deling, een datum plus een duur over zo'n kant) voegt de lege kant toe aan `c-` volgens
  de tabel van deel 4. De complementaire operatoren blijven: `c-` als "niet de vergelijking" zou een kenmerk in een
  vouwfilter van een kant negatief lezen.
- **De stapel**: een default die geldt met een vaststaand lege waarde valt door; met een open waarde is het doel open.
- **Niet geschreven, open gelaten (veilig) en geteld**: een cel van een afgeleid gedimensioneerd attribuut, een attribuut
  dat een creatie toekent (op het gemaakte object; naast een schrijver op een vergelijkbaar type met een waarde die open
  kan zijn: helemaal geen `A (leeg)`), een gedateerde groep buiten haar geldigheid. **Geweigerd**: schrijvers van één
  attribuut op twee vergelijkbare typen (`settled-empty-across-subtypes`).

### De getuigen en de golden

33 getuigen, 150 cases met handberekende verwachtingen, 11 verslaggetuigen. Op het omgezette document: ref 150/150,
Drools exec-model en DRL-tekst 33/33 (150 cases), clingo 139/150 (dezelfde 11 als in deel 3 en 4: `arithmetic`
CLINGO-005, `comparison` en `exact-digits` "unscalable datatype"). Op het gewone document falen 59 cases, elke getuige
ten minste één. De golden verandert niet: haar `ir.json` is byte voor byte de omzetting van `ordinary.json` op
`5d6ddd5b3`, en haar 4 cases zijn groen in ref.

**Rood eerst.** De getuigen `settled-empty`, `-stack` en `-fold` stonden in `86b5c2129`, vóór de code: op de omzetting
van deel 4 faalden 6 cases, op het gewone document 5 (werkmap `logs/p5-witnesses-01-red-old*.log`). `settled-empty-date`
stond in `af88dfce4`, vóór de correctie van `5d6ddd5b3`: op de omzetting van `71e3b5c08` faalde 1 case (werkmap
`logs/p5-red-date.log`).

**Gewijzigde verwachtingen in deel 5** (de nieuwe norm; geen verzwakking):

- `empty-by-fault/case2-deling-door-nul` (gewicht 10,00, aantal getuigen 0, schatting 4,00): oud `afwijkend` niet,
  totaal leeg; nieuw `afwijkend` en totaal 4,00 (de deling door 0 op vaststaande operanden is vaststaand leeg, de plus
  telt haar als 0, ≠ met één lege kant is waar), beide EF-005 zoals oud.
- `empty-by-fault/case3-geen-vondsten`: oud `lichtvoetig` niet; nieuw `lichtvoetig` (het minimum over geen vondst is
  vaststaand leeg, en leeg ≠ 5,00).
- `set-fold/case3-geen-vondsten`: oud totaalgewicht 0,00; nieuw leeg (een som over geen lid is vaststaand leeg, zoals
  de gewone lezing).
- Nieuw in een bestaande getuige: `empty-by-fault/case4-aantal-leeg` (aantal getuigen leeg: open, afwijkend niet, totaal
  leeg, geen deponering), zodat de getuige rood blijft op het gewone document.
- De andere 29 getuigen van deel 1 tot en met 4 spelen ongewijzigd groen.

### Monotonie

Met de plaatsing van deel 3 (`scripts/lib/strict-empty-oracle.py` op de branch, voorstel §11.3); de eindtoestand bevat
`K (niet)` en `A (leeg)`:

- Getuigen en golden: 34 documenten, 154 cases, 327 runs, 49 regels, alle geplaatst (toets op leeg of gevuld 31,
  telling van gevulde waarden 7, initialisatie 3, gesloten lid 3, telling over een gemaakte relatie 1, invoer naast een
  schrijver 4).
- Corpus: 228 documenten, 593 cases (1 waarvan de volle run faalt), 2124 runs, **241 regels** (deel 4: 247): toets op
  leeg of gevuld 82, initialisatie 49, telling van gevulde waarden 18, telling over een gemaakte relatie 18, invoer naast
  een schrijver 42 en stroomafwaarts 26, fout in beide lezingen 6, niet geplaatst 0.

### Behoud bij volledige kennis

Elke golden die omzet, met elk invoerlid gesloten (`verifier --strict-empty --closed-inputs`: elk attribuut en elk
kenmerk dat geen vorm schrijft), en elke case met zijn eigen verwachtingen in ref (werkmap `tools/behoud.py`,
`logs/p5-behoud-04.log`): 228 goldens, 593 cases, **582 gelijk**, 11 niet, in 5 goldens:

| oorzaak | cases | goldens |
|---|---|---|
| een deponering van een hogere default die niet beslist, herhaald in de rechtvaardiging van de lagere (een deling door 0 of leeg in een default die doorvalt; de gewone lezing houdt haar speculatief, ch. 7.1) | 6 | 2 |
| een kenmerk dat alleen een subtype declareert, gelezen op een object van het supertype (de smallere gastheer): buiten het subtype vaststaand onwaar; de proef heeft geen positieve typetoets | 3 | 1 |
| een attribuut gedeclareerd op een type en alleen op een subtype geschreven: op een object van alleen het type is het invoer, maar een lid sluit per declarerend type en een vorm schrijft het | 1 | 1 |
| een gedateerde defaults-groep buiten haar geldigheid | 1 | 1 |

Geen van de 11 is een case waarin de strikte lezing zelf van de gewone afwijkt. Het lezen van de eerste ronde vond vier
fouten in de omzetting, hersteld vóór deze getallen (`71e3b5c08`): een `Let` met een lege waarde, een creatie naast een
schrijver, een payload-toegang (WF-127), het filter van een vouw met een weggelaten `A (leeg)`. De regelset vond een
vijfde (`5d6ddd5b3`, de datum plus een duur).

### Het corpus

389 goldens, **228** zetten om (deel 4: 229; `jutterswet-melding-naar-aard` weigert nu op
`settled-empty-across-subtypes`). Notities: 39 initialisaties, 13 opgaven, 9 sluitingen over een afgeleid lid (7
goldens), 10 toekenningen van een creatie waar een vaststaand leeg attribuut open blijft (8 goldens), 1 gedateerde
groep. Zonder iets te sluiten houden **458** van de 593 cases hun verwachting (deel 4: 449 van 595); de 135 andere
ingedeeld (een case kan meer soorten hebben): operand als 0 of overgeslagen 64, doorval in een stapel of op een
initialisatie 50, een fout of §9.5-feit dat de strikte lezing niet of juist wel geeft 23, invoerkenmerk niet genoemd 19,
vergelijking over een open waarde 16, ontkenning van een afgeleid kenmerk 14, een afwezige instantie 1, een met de hand
geschreven opgave 1, een object buiten het type van een smallere gastheer 1, fout van de omzetting 0 (3 met de hand
gelezen). Tegenover deel 4 houden 11 cases nu hun verwachting (een som, minimum of deling over een vaststaand leeg lid,
een keten over een vaststaand lege datum, een vouw zonder bijdrage) en verloor er geen een.

`--empty-closures` op de 228 omgezette goldens: 147 plekken van klasse `negation` (deel 4: 149): doorval 59, een
vergelijking die op leeg onwaar is in een vouwfilter 59, een ontkend kenmerk in een vouwfilter 29; en 98 plekken van
klasse `closure` die geen uitdrukkelijke sluiting zijn en die de inventaris niet als bewaakt ziet (deel 4: 119):
`fold-skips-empty` 78, `coalesce-*` 13, `empty-divisor-fault` 6, `true-on-empty` 1. Grootte 0,92–5,01× (mediaan 1,31;
deel 4: 0,92–7,24×, 1,35); nieuwe negatieve randen 74 op attributen, 9 op relaties, 2 op een kenmerk buiten een
defaults-groep (een toets op leeg van een berekende waarde waarvan een vouwfilter een kenmerk leest; voorstel §5.12).

### De regelset buiten de repo, als getallen

De meting met namen, de lijst per gesloten lid en per verschil de lezing staan in de werkmap van de kaart (`meting/`,
de uitvoer onder `out/`, de scripts onder `tools/`). Vergeleken op de afgeleide leden die A en B allebei declareren (265
situaties; 2043 kenmerken zonder gegenereerde ontkenning apart; `A (leeg)` valt buiten de vergelijking zoals `K
(niet)`):

| | situaties gelijk aan A | leden gelijk | open | een uitkomst waar A geen heeft | andere waarde |
|---|---|---|---|---|---|
| B, gewoon | 224 | 8903 van 9116 | 0 | 186 | 27 |
| C, strikt, deel 3 | 0 | 5166 van 6925 | 1698 | 38 | 23 |
| C, strikt, deel 4 | 3 | 5131 van 6901 | 1756 | 14 | 0 |
| **C, strikt, deel 5** | **34** | **5812 van 6942** | **1069** | **55** | **6** |
| C met alle invoerleden gesloten, deel 4 (de 29 die een situatie leeg laat) | 3 | 5492 van 6931 | 1395 | 44 | 0 |
| **C met alle invoerleden gesloten, deel 5** | **209** | **6841 van 7073** | **20** | **186** | **26** |

- **Behoud** (C met alle invoerleden gesloten tegenover B, de gewone lezing van dezelfde versie): 248 van 265 situaties
  gelijk (de 6 apart meegeteld), 7053 van 7072 leden gelijk, 19 open, 0 andere waarde, 0 een uitkomst die B niet heeft.
  De 19: een afgeleid kenmerk op een object van een type dat zijn schrijvers niet bereiken (de derde oorzaak hierboven).
  De afstand tot A is daarmee die van B: het handwerk van A, niet de strikte lezing.
- De 6 andere waarden van C zijn de uitkomsten van B zelf (een toets op gevuld die B over een vaststaand leeg afgeleid
  lid schrijft), die het handwerk van A veranderde; deel 3 vond er 9 van die soort.
- Kanaal: 6 feiten die A meldt en C niet (deel 4: 7).
- **Sluiten per gegeven** (`logs/p5-closing-01.log` in de werkmap): per lid apart gesloten verplaatsen 10 van de 29
  invoerleden leden van open naar A's uitkomst (714, 28, 18, 11, 11, 9, 2, 2, 2, 1), 19 niets. Gretig: 11 leden, samen
  846 leden van open naar A's uitkomst (open 1069 → 209), 188 situaties gelijk (deel 4: 8 leden, open 1756 → 1496, 3
  situaties). Alle 29 tegelijk: gelijk aan de rij "alle invoerleden gesloten" (20 open, 209 situaties). Eén invoerlid, dat
  geen situatie geeft, verplaatst er alleen 714.

### Wat het kost

Grootte (canonieke tekst): versie zonder handwerk 1 106 818 bytes, omgezet 5 153 132 (4,66×; deel 4: 3,10×); versie met
handwerk 11 260 618 (6,44×; deel 4: 5,28×), de latere versie 18 186 001 (8,85×; deel 4: 6,72×). De groei komt van de
disjunctie "gevuld, of vaststaand leeg" per lezing van een afgeleid lid, de lege kanten van een vergelijking daarover
en de regels van `A (leeg)`. Drools, wandklok van één run met één situatie (JVM-start, laden, bouwen, spelen), mediaan
van drie, belasting (1 minuut) bij de start van een run 2,7 tot 7,9 op 16 kernen (werkmap `logs/p5-drools-timing-01.log`):
exec-model 0,9 s gewoon, 1,3 s omgezet (deel 4: 1,1); DRL-tekst 2,8 s gewoon, 262,3 s omgezet (runs 262,3, 262,0 en
276,2 s; deel 4: 181,6 s onder een belasting van 11 tot 29). Het omgezette document draagt 732 `atLeast`-composities
(deel 4: 337), 308 checks van een `(leeg)` en 195 ontkende rolchecks (deel 4: 131); de DRL-tekst-bouw groeit daarmee,
het exec-model nauwelijks.

### Acceptance, per eis

- **De modus schrijft een verifier-schoon document of weigert met exit 3; zonder de modus verandert niets.** Gehaald
  op de branch: exit 4 komt op het corpus 0 keer voor (ook met `--closed-inputs`), geen bestaand corpusbestand
  gewijzigd. `make gates` liep groen op `1df0fd98c` (deel 3); in deel 4 en 5 niet opnieuw (vervallen met de
  koerswijziging).
- **Niets rust op leeg.** **Niet gehaald** voor de inventaris: 147 `negation`- en 98 niet-uitdrukkelijke
  `closure`-plekken (zie "Het corpus"), elk een plek waar de inventaris de bewaking van de omzetting niet ziet.
  Monotonie is het bewijs.
- **Monotonie.** Gehaald voor de golden, de getuigen en de 228 omgezette corpusgoldens: 0 niet geplaatst.
- **Behoud bij volledige kennis** (eis van deel 5): 582 van 593 corpuscases, 248 van 265 situaties van de regelset; de
  rest is benoemd (vier beperkingen van de proef, geen afwijking van de lezing zelf).
- **De golden** speelt in vier executors; haar `ir.json` is byte voor byte de omzetting: gehaald op de branch.
- **Het corpus**: gehaald (aantallen hierboven).
- **De regelset**: gemeten, met de tellingen van deel 3, 4 en 5 naast elkaar, sluiten per gegeven, alles gesloten en
  behoud. De maat: 34 van 265 situaties gelijk zonder het handwerk (deel 3: 0, deel 4: 3).
- **Wat het kost**: gemeten (hierboven).

### Besluiten van de eigenaar (2026-11-21)

1. De sterke tabel van Kleene, ook als een eerder lid van een voorwaarde onbepaald is.
2. De initialisatie is een sluiting over haar stapel.
3. De route voor het landen: de wire draagt het document van de auteur met de schakelaar, elke executor past de
   strikte lezing toe in zijn eigen verlaging. De code van de proef landt niet (koerswijziging); het voorstel wel.

### Besluiten van de hoofdsessie na de meting op de regelset (2026-11-21, voorlopig, ter bevestiging)

- Een lezing over een link die er niet is, is gesloten (voorstel §5.8, beslispunt 10).
- "X is leeg" staat alleen vast voor de vorm die het zelf schreef (voorstel §5.4, beslispunt 12); sinds deel 5 alleen
  over een open X: over een vaststaand lege X staat het overal vast.
- De derde stand en de twee eisen, monotonie en behoud bij volledige kennis (opdracht van deel 5; voorstel §1.1, §2,
  §5.6a, beslispunt 16).

### Voorlopige besluiten van de bouwers

Deel 1:
1. De omzetting werkt op de wire en leest het geladen document ernaast (`Strict_empty.document : document -> json ->
   outcome`), niet `document -> document` via `Emit`, dat niet elke knoop schrijft.
2. Pointers: alles buiten een herschreven voorwaarde houdt pointer en bron; een gekopieerde knoop houdt zijn bron, een
   veranderde knoop krijgt er geen; gegenereerde vormen achteraan hun lijst.
3. Een consistentieregel wordt `none [a-]`.
4. Toetsen op gevuld zijn negatieve randen; gemeten wordt "geen nieuwe op een kenmerk".
5. `d(waarde)` volledig in de regelvoorwaarde; bij een objectcreatie weigeren waar de waarde leeg niet vanzelf doorgeeft.
6. Kenmerken op vergelijkbare typen geweigerd (`characteristic-across-subtypes`).
7. Versies met geldigheid tellen alle mee in `K (niet)` (note).
8. Een onvoorwaardelijke toekenning: geen regel voor `K (niet)` op dat type (note).
9. Uniciteit alleen geweigerd waar een vorm een sleutel schrijft.
10. De opgesomde min/max/eerste/laatste van twee of meer operanden is een operator.
11. `the` wordt gelezen als `all`.
12. Een document met een default-vocabulaire geweigerd (`default-vocabulary`).
13. Notities en weigeringen per regel op stderr; exit 4 voor een onschoon resultaat.

Deel 2:
14. De initialisatie als sluiting over haar stapel (inmiddels besloten door de eigenaar).
15. Een som zonder terugval krijgt 0.
16. Gegenereerde namen `<doel> (geen hogere <n>)`, `<doel> (geen eigen uitzondering)`, groepen `… van <type>`.
17. Een binder gelezen in een vouw: zijn `d` buiten de vouw.
18. Een telling over een gemaakte relatie telt wat staat (een sluiting).
19. Sluiten per gegeven als herhaalde optie `--closed`; een getuige draagt `closed.txt`.

Deel 3:
20. Een toets op leeg of gevuld van een berekende waarde wordt strikt gelezen (gevuld = `d` en de toets; leeg = de eerste
    afhankelijkheid die niet vaststaat, in volgorde, anders de toets).
21. Een initialisatie-default met een waarde die leeg niet vanzelf doorgeeft, wordt geweigerd (`initialisation-value`).
22. Een vouw over een verzameling zonder lid is naast andere operanden van een opgesomde eerste/laatste/min/max geen lege
    invoer: de opgesomde vouw gaat zonder haar door.
23. De plaatsing van de monotonie: in de volgorde van het voorstel §11.3; een telling over een gemaakte relatie geldt als
    uitdrukkelijke sluiting; invoer naast een schrijver gaat vóór een sluiting waar beide passen.
24. De meting op de regelset vergelijkt de afgeleide leden die beide versies declareren; een kenmerk zonder gegenereerde
    ontkenning telt als niet waarneembaar; een parameter die de oudere versie niet declareert, telt niet als lid.
25. De Drools-bouwtijd is gemeten als wandklok van één run met één situatie (JVM-start, laden, bouwen, spelen).

Deel 4:

26. Een ontkende genavigeerde kenmerkcheck is over een afwezige link onwaar, zoals de gewone lezing (RUNTIME-SEMANTICS
    ch. 5.8 regel 3): `c⁺` is "het object aan de andere kant draagt `K (niet)`", `c⁻` is "de link ontbreekt, of het
    object draagt `K`". De opdracht noemde alleen de bevestigende vorm.
27. De constante onwaar is `none [all []]`: clingo vertaalt een `atLeast` zonder delen niet.
28. Een vergelijking over een gesloten lid leest als de gewone lezing, met dezelfde tabel als een afwezige link.
29. Een pad van twee of meer stappen: per type en rest een gegenereerd kenmerk `<rol> (ontbreekt)` met één regel; een
    stap vanaf het vormsubject in een positie per lid: een kwantorvouw `none` over die stap.
30. De telling van gevulde waarden vraagt voor een andere vorm de waarde van elk lid; het filter dat een vouw in de
    uitvoer draagt, is haar `c⁺` in de eigen wandeling.
31. De opgave houdt haar sluiting "de opgave is leeg" ook in een andere wandeling: zij is een sluiting van de
    declaratie, geen toets.
32. Bij gelijk en ongelijk komt "beide kanten leeg" alleen in `c⁻` als het datatype bekend niet-numeriek is; onbekend
    voegt niets toe (de veilige kant).
33. De meting op de regelset: een invoerlid is leeg in een situatie als een instantie van zijn type de waarde niet
    heeft (een kenmerk: niet noemt); de oorzaken zijn de invoerleden stroomopwaarts over de vormen van B, doorsneden met
    wat leeg is; gretig kiest per stap uit de leden die bij een nog open lid stroomopwaarts leeg zijn.

Deel 5:

34. De naam van het gegenereerde kenmerk is `<attribuut> (leeg)`, met de regel `<attribuut> (leeg) van <type>`; over
    een pad van twee stappen `<attribuut> van <rol> (leeg)` op het type van de eerste stap. Een `(leeg)` zonder
    schrijver die het kan geven, wordt niet gedeclareerd; zijn lezingen worden onwaar en vallen weg.
35. `c-` van een vergelijking blijft de complementaire operator met de lege kanten volgens de tabel, niet "niet de
    vergelijking": die zou een kenmerk in een vouwfilter van een kant negatief lezen. Een kant "kan vaststaand leeg zijn"
    is een overschatting (een afgeleid of gesloten lid, een pad, een parameter, een sessiewaarde, een deling, macht of
    wortel, een vouw, een datum plus of min een duur over zo'n kant); een overbodige lege kant is onwaar.
36. Een som zonder terugval krijgt geen 0 meer: over geen bijdrage is zij vaststaand leeg, zoals de gewone lezing. Dat
    volgt uit behoud bij volledige kennis en keert voorlopig besluit 15 om.
37. De toets van de knoop zelf in `d` (besluit 1 van de hoofdsessie in deel 2: deling, macht, wortel, datum uit delen)
    vervalt: een lege uitkomst op vaststaande operanden is vaststaand leeg, met haar deponering.
38. "Valt door" en `A (leeg)` worden in de andere wandeling gebouwd, zoals `K (niet)`: een sluiting "X is leeg" over een
    open X die een hogere default zelf schreef, laat een lagere default niet beslissen (de veilige kant).
39. Een creatie die een attribuut een waarde geeft die open kan zijn, naast een schrijver op een vergelijkbaar type:
    geen `A (leeg)` voor dat attribuut (het blijft open waar het leeg is), geen weigering. Een waarde die alleen
    vaststaand leeg kan zijn: het gemaakte object houdt het open. Een cel van een afgeleid gedimensioneerd attribuut en
    een gedateerde groep: open gelaten en geteld.
40. Schrijvers van één attribuut op twee vergelijkbare typen worden geweigerd (`settled-empty-across-subtypes`): een
    gekopieerde voorwaarde kan niet op een ander onderwerptype gelezen worden (WF-053). Corpus 1 golden.
41. `--closed-inputs` sluit elk lid dat geen vorm schrijft, per declarerend type: de meting van behoud.

### Open punten

1. **De dominantieregel in `empty_closures.ml`** is niet gebouwd (147 `negation`- en 98 `closure`-plekken). In een
   eigen verlaging leest de inventaris het document van de auteur met de eigenschap.
2. **De regelset en sluiten per gegeven**: met de derde stand brengt sluiten de regelset wel bij de gewone lezing (alle
   invoer gesloten: 248 van 265 situaties gelijk aan B); de afstand tot A is het handwerk. Welke gegevens de regelset
   als volledige invoer wil verklaren (de gretige elf, of alle), en in welke vorm (`(volledig)` op de declaratie), is
   een beslissing van de eigenaar over de regelset, buiten deze kaart.
3. **De met de hand geschreven opgave** (een uitzondering "indien X gevuld is" boven een basisregel) beslist over een
   open X niets: 1 corpuscase. Over een vaststaand lege X geeft zij voorrang en beslist de basis.
4. **clingo** weigert een toets op gevuld van een enumeratie- of tekstattribuut met een fout zonder registercode
   ("unscalable datatype"), ook in een gewoon document. Een fout van clingo buiten de kaart; hersteld op `main` met de
   kaart `clingo-filled-test`.
5. **De DRL-tekst-bouw** groeit op de regelset (zie "Wat het kost") door de disjuncties per lezing; het exec-model niet.
   Een eigen verlaging hoeft die niet te schrijven.
6. **Een lid op een object dat zijn schrijvers niet bereiken** (behoud, tweede en derde oorzaak; voorstel §12.18): een
   kenmerk dat alleen een subtype declareert, gelezen op een object van het supertype, en een lid dat een supertype
   declareert en alleen een subtype schrijft. De proef heeft geen positieve typetoets en laat beide open: 4 corpuscases,
   19 leden van de regelset. Sluiten per objecttype in plaats van per declarerend type zou volledige kennis voor zo'n lid
   uitdrukbaar maken.
7. **Een deponering van een hogere default die niet beslist** (behoud, eerste oorzaak; voorstel §5.12, §12.17): de proef
   evalueert haar opnieuw in de rechtvaardiging van de lagere default, waar zij blijft staan: 6 corpuscases in 2 goldens.
   Geen vraag van de lezing; een eigen verlaging leest de voorwaarde één keer.
8. **Een invoerkenmerk naast een regel die zijn ontkenning afleidt** geeft beide; de gegenereerde consistentie (voorstel
   §12.4) is niet gebouwd.
9. **Ongeordende groepen en andere composities** blijven geweigerd (10 plekken in 6 goldens, 11 in 8).
10. **De plaatsing van de monotonie is een statisch bereik**, geen bewijs per regel.
11. **Wat de proef open laat waar de lezing het vaststelt** (veilig, geteld): een gedateerde groep buiten haar
    geldigheid (1 corpuscase; voor een kenmerk verbiedt WF-050 een regel naast de groep), een cel van een afgeleid
    gedimensioneerd attribuut, een attribuut dat een creatie toekent (10 notities in 8 goldens).
12. **Twee negatieve lezingen van een kenmerk buiten een defaults-groep** (2 corpusgoldens): een toets op leeg van een
    berekende waarde waarvan een vouwfilter een kenmerk leest. De graaf blijft gestratificeerd; de witness-controle
    "geen nieuwe negatieve rand op een kenmerk" geldt daarmee niet meer voor het hele corpus.

### `make gates`

Niet gedraaid in deel 4 en 5: vervallen met de koerswijziging, omdat de poten in de gates niet landen. Laatste groene
run op `1df0fd98c` (deel 3, 17 gates, 637 s, `out/gates/20261121-140739/summary.txt`). In deel 5 gedraaid: de build, de
getuigenrunner (ref, gate 2 expliciet op de omgezette getuigen, verifier-schoon), de uitvoerders op de getuigen, de poot
van gate 5 met de hand (`strict-empty-oracle.py gate`: 33 getuigen, 150 groen, 59 rood, 327 runs, 0 niet geplaatst), en
gate 1 vóór elke commit. De floors van de poten in gate 3 en 5 op de branch zijn niet verhoogd.

### Landing (hoofdsessie, 2026-11-21)

Geland is het verslag: dit bestand en `spec/proposals/STRICT-EMPTY.md`, op een eigen kleine branch vanaf `main`; de
branch `card/strict-empty-lowering` met de code van de proef is niet gemerged en blijft lokaal staan. Nagelezen: het
meetscript van de vergelijking op de regelset, de getuigen van de drie herzieningen en het voorstel. In het voorstel
bijgewerkt bij de landing: de schakelaar staat inmiddels op `main` (§3), de route per executor (§12.21), en wat het
landen nog vraagt (§12.15). De schakelaar zelf landde met de kaarten `infer-from-empty-property` en
`regelspraak-infer-from-empty`. Alleen gate 1 gedraaid: de landing raakt geen code en geen gate.
