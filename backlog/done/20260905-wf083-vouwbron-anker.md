# WF-083 ankert de zelfbenoemende vouwbron op het verkeerde objecttype

Goal: WF-083 controleert de zelfbenoemende rol van een vouwbron tegen
het **operand**-objecttype — `spec/verifier/lib/checks.ml` (~:2497,
`self_denoting_role ctx ~otype:otn nr`) — terwijl beide executors op het
**vouwSUBJECT** ankeren. En geen enkele regel kruist het
vouwbron-objecttype tegen het subject of tegen het
`elementObjecttype`. Daardoor kantelt één wire-veld het verifier-oordeel
zonder dat een executor beweegt, en blijven vormen verifier-clean die
beide executors weigeren.

Drie gemeten getuigen (2026-09-05, eigen bron, jaargranulariteit;
Vondst-subject, `Eén bewarende jutter bewaart meerdere vondsten`):

**1. Het anker.** Dezelfde set-vouw, dezelfde `navigatieRol` (`vondst`),
alleen het operand-`objecttype` gedraaid:

```
objecttype "Vondst" : verifier PASS
objecttype "Jutter" : WF-083 FAIL /resolvedRegels/0/waarde/argumenten/0:
                      navigatieRol 'vondst' on a fold source does not
                      denote the fold subject …
ref     : in BEIDE gevallen EVALUATION FAULT: unsupported instance-fold
          source shape
clingo  : in BEIDE gevallen dezelfde luide §7.1-weigering
```

Eén wire-veld kantelt de verifier; geen executor beweegt. Het veld dat
de executors wél lezen — het vouwsubject — komt in de regel niet voor.

**2. De ontbrekende kruising.** Een temporele `TOTAAL` met een
operand-objecttype dat niet het subjecttype is en zónder navigatie:

```
verifier : PASS   (gemeten op 73b824a, de vierde-golf-verifier)
ref      : EVALUATION FAULT: unsupported instance-fold source shape
clingo   : TRANSLATION FAULT: the temporal total of "bewaartarief" is
           declared on "Jutter", not on the subject type "Vondst" …
           (reden-dragend sinds 20260904-clingo-setfold-armen)
```

Beide executors weigeren, de verifier laat door. Precies de vorm die
vóór die kaart stil 99 antwoordde: de verifier had hem kunnen vangen en
deed dat niet.

**3. Ref ≢ clingo onder een welgevormd document.** Een NIET-temporele
set-`TOTAAL`, verder identiek:

```
verifier : PASS
ref      : V1.bewaarlast = 0
clingo   : TRANSLATION FAULT: expression kind not translated
           (no corpus case exercises it)
```

Een welgevormd document waarop de twee executors het oneens zijn — de
soort divergentie die per PLAN §6 per definitie een spec-bug is.

Acceptance:
- Het anker verplaatst van het operand-objecttype naar het
  **vouwsubject**, plus de ontbrekende kruisregel (vouwbron-objecttype
  tegen subject dan wel `elementObjecttype`).
- Elke nieuwe of gewijzigde regel krijgt een negatief-case én
  arm-exact mutant-bewijs: de mutatie die de regel uitschakelt maakt
  precies dat negatief groen en niets anders.
- De regressiegrens geteld over alle vier de zones vóór het landen
  (verwachting: 0 van de 79 enkelvoudige `AttribuutRef`-vouwoperanden
  beweegt; 74 daarvan bestonden al vóór de vierde WF-034-golf).
- Poort 3 telt minstens twee negatiefs erbij; alle dertien gates groen.
- De kaart wordt op `73b824a`-of-later gewerkt: de wart-versmalling van
  de vierde golf zit tegen deze arm aan. Die versmalling is
  verdict-neutraal gemeten over 64 getuigedocumenten (0
  verdictverschillen), dus de metingen hierboven staan er ook op.

May touch: `spec/verifier/**` (voorstel — beschermd pad, de maintainer
merget), `spec/IR-SPEC.md` (voorstel — idem), `corpus/negatief/**`,
`backlog/`.

Out of scope: de executors — die weigeren de vormen al luid, en de
`clingo`-kant is net gedaan
(`backlog/done/20260904-clingo-setfold-armen.md`); ref's foutmelding
verfijnen (`unsupported instance-fold source shape` noemt de vorm niet,
maar hij is luid en dat is een eigen kaartje).

## Hermeten op de actuele stand (2026-09-05, `15d0df4`)

De kaart mat deels op `73b824a`; de wart-versmalling en de vijfde golf
zitten ertussen. Alles hermeten op eigen bron, gebouwd op de
declaraties van `corpus/golden/jutterswet-tempototaal` (feittype
`bewaring`: `bewarende jutter` — Jutter, `vondst`/`vondsten` — Vondst).

**1. Het anker.** Dezelfde set-vouw (Jutter-subject, `elementObjecttype`
Vondst), dezelfde `navigatieRol: "vondst"`, alleen het
operand-`objecttype` gedraaid:

```
objecttype "Vondst" : verifier PASS
objecttype "Jutter" : WF-083 FAIL … navigatieRol 'vondst' … is filled by
                      'Jutter' itself  (+ WF-034: Jutter declareert het
                      attribuut niet)
ref     : in BEIDE gevallen EVALUATION FAULT: unsupported instance-fold
          source shape
clingo  : in BEIDE gevallen TRANSLATION FAULT: expression kind not
          translated
Drools  : speelt BEIDE en antwoordt de vastgepinde 19
```

Eén wire-veld kantelt de verifier; geen executor beweegt — en de derde
executor antwoordt waar de eerste twee weigeren.

**2. De ontbrekende kruising (niet-set).** Vondst-subject, temporele
`TOTAAL`, `elementObjecttype` Vondst, operand
`bewaarvergoeding per maand` met `objecttype: "Jutter"`, geen navigatie:

```
verifier : PASS
ref      : EVALUATION FAULT: unsupported instance-fold source shape
clingo   : TRANSLATION FAULT: the temporal total of "bewaarvergoeding per
           maand" is declared on "Jutter", not on the subject type
           "Vondst" …
Drools   : vuurt en antwoordt 10 — de maandvergoeding van de Jutter,
           gelezen op een Vondst-subject
```

**3. Ref ≢ clingo onder een welgevormd document (set).** Jutter-subject,
niet-temporele set-`TOTAAL` over `elementObjecttype` Vondst, operand
`kistgeld` met `objecttype: "Jutter"`:

```
verifier : PASS
ref      : oracle PASS — bergloon = 0
clingo   : TRANSLATION FAULT: unknown attribute "kistgeld" on "Vondst"
Drools   : ERROR ConstraintEvaluationException
```

Afwijking van de kaart: clingo's melding is niet meer
`expression kind not translated` maar de attribuutopzoeking op het
elementtype — de vijfde golf draagt de set-vouw inmiddels, en weigert
één veld verderop. De divergentie zelf staat.

**4. Het gat dat de ankerverplaatsing alléén zou OPENEN** (niet in de
kaart, gemeten omdat de fix het moest afdekken): dezelfde set-vouw met
`navigatieRol: "bewarende jutter"` — een rol die het SUBJECT vervult.
Vandaag WF-083 FAIL, maar op het verkeerde anker (`'Vondst'` vervult die
rol niet); met het anker op het subject zou hij verifier-clean worden
terwijl ref en clingo hem weigeren en Drools hem speelt. Vandaar de
tweede helft van clausule (b): de geschreven rol hoort bij de wrat
alleen.

**Census van de regressiegrens** (504 documenten: 154 golden + 22 rood +
298 negatief + 30 parity): 122 vouwbron-`AttribuutRef`-operanden onder
een `elementObjecttype`, waarvan **110** de enkelvoudige
instantievouwbron en 12 operanden van een gelede vouw met een vestigiaal
`elementObjecttype`. Alle 122 kloppen vandaag met hun anker; alle 4
operanden die überhaupt een `navigatieRol` dragen zijn wratten. Verdeling
van de 110: 50 wrat-`AANTAL` (set=false), 32 `SOM` set, 14 `TOTAAL`
set=false (de eigen temporele totalen), 4 `MAXIMUM` set, 4 `EERSTE` set,
3 `TOTAAL` set, 3 `MINIMUM` set. Gebonden vormen (`bronRol`): 84.

---

## Result (2026-09-05, `card/wf083-vouwbron-anker`, afgetakt van `15d0df4`)

**Geland.** WF-083 is de ANKERREGEL van de vouwbron geworden: drie
clausules onder één nummer, geen nieuw nummer. Motivering voor het
nummer: WF-083's eigen tekst vooronderstelde de identiteit al ongetoetst
("a role that the argument's object type — **the fold subject** — itself
fills"), en de ontbrekende kruising is precies die vooronderstelling
gecontroleerd. Eén claim — "de vouwbron beweegt niet" — met een
navigatie-helft en een ankerhelft. Het kleinste juiste.

- **(a)** ongewijzigd: een niet-lege `navigatiePad` op een vouwbron is
  ongeldig.
- **(b) gewijzigd, twee helften.** De zelfbenoemingstoets ankert op het
  VOUWSUBJECT (ref: `self_denoting_role ~self_type:self.otype`; de
  ASP-backend: `~self_type:subject_ot`), en een `navigatieRol` is alleen
  op de rij-1-wrat toegestaan — op elke andere vouwbron is het navigatie,
  die ref's set-bronarm en zijn temporele totaal allebei afwezig eisen.
- **(c) nieuw.** Het `objecttype` van de enkelvoudige vouwbron MOET het
  anker van de vouw noemen: het `elementObjecttype` bij `set`, anders het
  vouwsubject. Dat is clingo's `fold_operand_otype` gekruist tegen het
  subject; ref's armen zeggen hetzelfde veld voor veld.

**Vier negatiefs, vier arm-exacte mutanten** (elk gemeten over alle 508
documenten van golden + rood + negatief + parity; elk verandert precies
één rapport en maakt precies één negatief groen):

| negatief | clausule | mutant |
|---|---|---|
| `wf083-vouwbron-anker-op-operand` | (b) anker | `~otype:otn` i.p.v. `~otype:st` |
| `wf083-vouwbron-navigatierol-buiten-de-wrat` | (b) scope | de niet-wrat-arm geeft `[]` |
| `wf083-vouwbron-vreemd-objecttype` | (c) niet-set | anker `Some otn` i.p.v. `subject` |
| `wf083-vouwbron-vreemd-elementtype` | (c) set | anker `Some otn` i.p.v. `element_type` |

Een vijfde mutant (de `single`-scope van (c) weggehaald) verandert
NIETS: de 12 operanden van gelede vouwen kloppen vandaag ook met hun
anker, dus de scope verbergt geen overtreding — hij staat er om
semantische reden (bij ≥2 operanden is een vreemd `objecttype` een
geldige typenpaar-hop, ref's `navigated`-schakelaar).

**Regressiegrens.** Over de 504 documenten die deze kaart voorafgaan
beweegt **geen enkel oordeel**. De enige verandering in het hele
sweep-diff is één zin in het bestaande rapport van
`wf083-fold-source-navigates-away` (`filled by 'Jutter' itself` →
`filled by the fold subject 'Jutter' itself`) — zijn oordeel is
anker-onafhankelijk omdat zijn operand het subject al spelt; NOTE
bijgewerkt met die reden. Expliciet gemeten als grens:

- **de wrat-route**: 50 wrat-bronnen over de vier zones
  (`jutterswet-kaveltelling`'s gesynthetiseerde meervoud, de
  gedeclareerde-meervoudsroute van `jutterswet-aanmeldertelling` en
  `jutterswet-vondsttelling`, en alle andere) — 0 bewegen; alle 50
  spellen hun subject als `objecttype`, dus (c) raakt ze niet;
- **de gebonden vormen** (`bronRol`): 84 — 0 bewegen;
- **de vier `navigatieRol`-dragers**: alle vier zijn wratten, en alle
  vier houden hun oordeel (`jutterswet-aanmeldertelling` PASS; de
  bestaande negatief FAIL; de twee parity-documenten PASS — hun operand-
  objecttype IS hun subject).

**Poort 3**: negatief 298 → **302**. **Alle dertien poorten groen**;
geen andere teller beweegt (176 golden+rood schema/verifier, 427
case-bestanden, 405 replays in ref en clingo, 369 no-op-weerleggingen,
22 rood, 201 catala-checks + 12 byte-identieke goldens, 171 JUnit + 30
rgs-goldens, 29 toka-cases, Drools 108 gespeeld + 199 JUnit, 64
type-twists over 13 goldens).

**Bewust open gelaten, gemeten en benoemd** (in de spec als flank
opgeschreven, niet dichtgeclaimd): een enkelvoudige, niet-set,
niet-wrat vouwbron over een NIET-temporeel eigen attribuut voldoet aan
(a)–(c) en blijft verifier-clean terwijl geen van beide OCaml-executors
hem draagt (ref `unsupported instance-fold source shape`, clingo
weigert de expressiesoort). Dat is een VORM-regel — welke enkelvoudige
bronnen er überhaupt bestaan — niet een ankerregel, en hij vraagt zijn
eigen kaartje.

**Afwijkingen van de kaart.** (1) Getuige 3's clingo-melding is niet
meer `expression kind not translated` maar `unknown attribute "kistgeld"
on "Vondst"`: de vijfde golf draagt de set-vouw inmiddels en weigert één
veld verderop; de ref ≢ clingo-divergentie zelf staat onveranderd.
(2) De kaart telde ~79 enkelvoudige vouwoperanden op `73b824a`; op
`15d0df4` zijn het er 110 (plus 12 gelede) — hertellingen in de kaart
opgenomen. (3) Vier negatiefs in plaats van twee: elke clausulehelft
heeft er één nodig om arm-exact te zijn. (4) Eén meting die de kaart
niet had: de ankerverplaatsing zou op zichzelf een gat OPENEN — vandaar
de scope-helft van (b). (5) Drools is overal meegemeten en is drie keer
de doorslag: hij SPEELT wat ref en clingo weigeren (getuige 1 en de
scope-getuige), antwoordt de waarde van het ANDERE object (getuige 2),
en gooit waar ref stil 0 antwoordt (getuige 3).

**Beschermd pad.** `spec/verifier/**` en `spec/IR-SPEC.md` zijn
voorstel; de branch blijft ongemerged tot de maintainer hem beoordeelt.

---

## Fixronde (2026-09-05, dezelfde branch)

Twee lenzen lazen de geland-stand na en bevestigden alle metingen
hierboven tot op het cijfer. Wat ze daarnaast vonden is één defect met
twee helften — een REGRESSIE van deze kaart en een gat dat er al zat — en
dat is hier hersteld. Alles opnieuw gemeten; elke bewering hieronder is
een meting op deze branch.

### 1. De familie-scope: de hele "een vouwbron navigeert nooit"-familie
### wordt enkelvoudig-gescoped

De Result-samenvatting hierboven beschrijft de `single`-scope van
clausule (c) als een SEMANTISCHE keuze: bij ≥2 operanden is de vouw de
GELESTE waardevouw (§5.8.3), haar operanden zijn gewone lezingen, en een
vreemd `objecttype` is daar een geldige typenpaar-hop. Dat klopt — en
precies daar zit de correctie die lens 1 aandroeg en die de samenvatting
niet maakte: **dezelfde semantiek was niet toegepast op de rest van de
familie.** De navigatieBindingen-arm, clausule (a) en clausule (b)
spraken ongescoped over ELKE vouwoperand, terwijl ze alleen over de
vouwBRON gaan — het enkelvoudige argument dat de gevouwen verzameling
levert. De vijfde-mutant-alinea hierboven ("de scope staat er om
semantische reden") is dus juist over (c) en onvolledig over de kaart:
zij verzweeg dat drie zusterarmen die reden niet volgden.

Gescoped zijn nu alle vier: de bindingen-arm, (a) `navigatiePad`, (b)
`navigatieRol`, (c) het anker. Bij (a) hoorde één vormdetail: het derde
patroon van de match moest `navigatie_pad = []` LOSLATEN, anders valt een
geleste operand mét pad door álle armen heen en bereikt de resolutie
eronder niet meer.

**De vierkanten die de scope afdwingen** (eigen bron op de
`bewaring`-declaraties, twee leden):

| vorm | verifier vóór | ref | ASP-backend | Drools exec | Drools DRL |
|---|---|---|---|---|---|
| `LAATSTE` met genavigeerde operand 2 | **WF-083 + WF-053 FAIL** | 30 | 30 | 30 | 30 |
| dezelfde met `navigatiePad` + bindingen | WF-083 + WF-053 FAIL | speelt | speelt | EXCLUDE per vorm | — |

De eerste rij is een REGRESSIE van deze kaart: clausule (b)'s
wrat-scope-helft is nieuw, en ze weigerde een vorm waar vier
executor-benen het over eens zijn. De tweede rij is dezelfde klasse maar
PRE-EXISTENT (de bindingen-arm en (a) stonden er al vóór deze kaart):
`ref` en de ASP-backend spelen hem allebei, de Drools exec-tak weigert
hem reden-dragend per vorm.

### 2. De WF-053-herhaling in de vouwbron-tak

Het spiegelbeeld van 1. De vouwtak van `walk_expr` VERVANGT de generieke
expressiewandeling voor deze argumenten — daarom staan K1's WF-034-armen
en WF-089/WF-092 daar al herhaald. WF-053 stond er niet. Gevolg: een
geleste operand was de ENIGE leespositie in de hele IR waarvan de hop
tegen niets resolveerde. Nu herhaald voor `List.length f.argumenten <> 1`,
met dezelfde `navigated`-schakelaar als de generieke arm (geschreven rol,
pad, of onderwerp-mismatch) en dezelfde parameters.

Gemeten effect: een `MAXIMUM` met operand 2 naar een objecttype waarheen
geen enkel feittype leidt wordt **WF-053 FAIL — `bindingless navigation:
no fact type relates 'Vondst' to 'Baken'`** — woordelijk wat dezelfde
lezing buiten een vouw al kreeg; de to-many-hop-in-een-scalairslot komt
mee.

### 3. De prijs, hermeten

**Byte-sweep over alle 510 documenten** (155 golden + 22 rood + 303
negatief + 30 parity), rapport voor rapport vergeleken: **0
oordeelskantelingen, 0 tekstwijzigingen.** De vijf wf083- en zes
wf053-negatiefs blijven rood met hun eigen regel.

**Mutanten, elk build+meting in één `&&`-keten met harde build-faal**
(een gebroken build laat een sweep stil op de oude binary draaien):

| mutant | uitschakeling | effect over 510 |
|---|---|---|
| X1 | (b)-anker `~otype:otn` i.p.v. `~otype:st` | alleen `wf083-vouwbron-anker-op-operand` groen |
| X2 | de niet-wrat-arm geeft `[]` | alleen `wf083-vouwbron-navigatierol-buiten-de-wrat` groen |
| X3 | (c) niet-set-anker `Some otn` | alleen `wf083-vouwbron-vreemd-objecttype` groen |
| X4 | (c) set-anker `Some otn` | alleen `wf083-vouwbron-vreemd-elementtype` groen |
| X5 | de WF-053-herhaling uit | alleen `wf053-geleste-vouwoperand-onverbonden-hop` groen |
| X6 | de hele familie-scope (`when single`) weg | alleen `jutterswet-sluitsom` ROOD (WF-083 + WF-053) |
| X8 | de twee defer-armen markeren | **0 treffers** |

X6 is de falsificatiegetuige van de scope: de nieuwe golden is het
ENIGE document van de 510 dat beweegt als je de scope weghaalt — de
scope verbergt dus niets in het bestaande corpus, en hij is niet
onbewijsbaar. X8 is de defer-eerlijkheid: de `subject = None`- en
`anker = None`-armen van (b) en (c) worden over 510 documenten **nul
keer** bereikt; de spec zegt dat nu hardop in plaats van het te verzwijgen.

### 4. Nieuw materiaal

**Negatief** `corpus/negatief/wf053-geleste-vouwoperand-onverbonden-hop/`
(303e). Rood-eerst in de gebruikelijke zin kon hier niet — de herhaling
bestond nog niet — dus in de omgekeerde volgorde bewezen en zo
gecommit: het document eerst gemeten als verifier-**PASS** op de stand
vóór de herhaling (het gat), daarna de herhaling, daarna FAIL. `ref`
faalt er met de zin van de verifier zelf: `EVALUATION FAULT: no fact type
relates "Vondst" to "Baken"`.

**Golden** `corpus/golden/jutterswet-sluitsom/` — de **gemandateerde
verruiming van May-touch** van deze fixronde, hier expliciet gemeld: de
oorspronkelijke kaart noemde `corpus/golden/**` niet. Het is een
POSITIEVE getuige en daarom mét case en metingen gecommit, niet
rood-eerst. De vorm: `de laatste van haar meldloon en het ijkloon van de
bewarende jutter` — geleste waardevouw, operand 2 navigeert één hop, met
`navigatieRol` én symbolische `navigatieBindingen` (de bond route wordt
zo ook geraakt). Twee cases, handberekend en daarna door alle benen:

```
case1  V1 meldloon 12, hop 30 -> 30 (de HOP wint)
       V2 meldloon 41, hop 30 -> 41 (de EIGEN lezing wint)
case2  V3 meldloon 7, geen hop -> 7;  V4 alles leeg -> leeg;
       V5 geen meldloon, hop 30 -> 30
ref PASS - ASP-backend PASS - Drools exec PLAYED - Drools DRL PLAYED
--noop: MISMATCH op beide cases (poort 7 weerlegt hem)
poort 13: 4 draaibare eenheidsannotaties (3x AttribuutRef + Functie/LAATSTE),
          0 buiten de draaitabel; alle vier benen reproduceren de
          onverdraaide uitkomst -> geen survivor-regel nodig
```

Geen frontend-emissie: kort gecheckt en tweemaal onmogelijk. De frontend
weigert `laatste van` op numerieke expressies (`laatste van vereist
datumexpressies, kreeg Numeriek`), en op een vorm die hij wél accepteert
schrijft hij `elementObjecttype: null` voor een geleste vouw en laat de
voornaamwoord-operand naast een genavigeerde zusteroperand onopgelost —
juist het vestigiale `elementObjecttype` is wat de vouwbron-arm laat
lopen en dus de hele getuige. Handgeschreven dus, zoals
`meldplichtspoor`; de NOTE legt het uit.

### 5. Proza

`spec/IR-SPEC.md` ch. 4.3 (voorstel, beschermd pad):

- de onbegrensde MUST is enkelvoudig-gescoped, met een nieuwe alinea
  "What a fold SOURCE is" ervóór;
- de zin "both executors derive the declaration from the anchor and
  **never from this field**" is VERVANGEN: hij was onhoudbaar. Gemeten:
  de `fold_operand_otype` van de ASP-backend geeft bij `set = false`
  precies dat veld terug als anker, en `ref`'s temporele totaal bewaakt
  `norm aot = norm self.otype`. Beide LEZEN het veld. Wat ze niet doen is
  het laten KIEZEN. De nieuwe formulering: *it is a guard, not a
  selector* — en bij `set = true` is het inert in beide, wat geen reden is
  het vrij te laten maar juist de reden het vast te pinnen;
- de (b)-motiveringszin krijgt de geleste-operanden-uitzondering; de
  WF-083-bullet opent met de scope en draagt WF-053 als companion;
- de flank-alinea draagt nu DRIE flanken in plaats van één: de
  niet-set/niet-wrat-vorm die er stond; de defer-eerlijkheid (0 van 510);
  en de **wrat-als-waardevouwbron**-klasse, inclusief de vermelding dat
  de ankerverplaatsing van deze kaart de `navigatieRol`-dragende SET-
  spelling ervan NIEUW toelaat (gemeten FAIL→PASS tegen de
  `15d0df4`-verifier) op een vorm die `ref` en de ASP-backend weigeren en
  Drools speelt (SOM/TOTAAL 19, MAXIMUM 12, MINIMUM 7);
- de indexrijen: WF-083 krijgt het `single`-voorvoegsel en
  `jutterswet-sluitsom` in de golden-kolom, WF-053 het nieuwe negatief.

Nieuw kaartje: `backlog/new/20260905-wrat-waardevouw.md` — de wrat als
bron van een waardevouw, met beide spellingen gemeten en de te beslechten
vraag (telbron met een weigerende WF-regel, of geldige setbron die `ref`
moet dragen).

### 6. Afwijkingen van het recept, en één ding dat niet kon

1. **De commit-titel van `9261e63` klopt niet en is niet te repareren**
   (history): "vier WF-083-negatiefs die de verifier van vandaag
   doorlaat" — het waren er DRIE. De vierde,
   `wf083-vouwbron-navigatierol-buiten-de-wrat`, werd op `15d0df4` al
   afgewezen, maar op het VERKEERDE ANKER (de rol `bewarende jutter`
   wordt niet vervuld door het operandtype `Vondst`); met het anker op
   het onderwerp zou juist die arm hem hebben doorgelaten, en dat is
   waarom (b)'s scope-helft bestaat. Drie-plus-één-op-het-verkeerde-
   anker, hier vastgelegd omdat de titel het niet meer kan zeggen.
2. De pad+bindingen-variant meet bij mij **niet** drie executors op één
   getal: `ref` en de ASP-backend spelen hem, de Drools exec-tak weigert
   hem per vorm (`transpiler SKIP … waarde buiten scope`). De klasse
   (pre-existente over-afwijzing) staat; het aantal instemmende benen is
   twee, niet drie.
3. De golden kreeg TWEE cases in plaats van één (de kern plus de lege
   hoeken), dus de replay-tellers gaan 405 → **407** in plaats van 406,
   en poort 7 369 → **371**.
4. De wrat-waardevouw-meting wijkt af van wat de lens rapporteerde: bij
   mij antwoordt **alleen Drools** met 19/12/7; de ASP-backend weigert de
   expressiesoort in ALLE spellingen die ik bouwde. Het kaartje draagt
   mijn getallen.

### 7. Tellingen

**Alle dertien poorten groen**, gemeten na het laatste commit:

| poort | vóór | ná |
|---|---|---|
| 2 schema golden+rood | 176 | **177** |
| 3 golden clean / negatief afgewezen | 176 / 302 | **177 / 303** |
| 4 case-bestanden | 427 | **429** |
| 5 ref-replays | 405 | **407** |
| 6 clingo-replays | 405 | **407** |
| 7 no-op-weerleggingen | 369 | **371** (36 gepinde survivors) |
| 8 rood | 22 | 22 (5 fault-gepind, 17 reden-dragend niet gedragen) |
| 9 catala | 201 checks + 12 goldens | ongewijzigd |
| 10 regelspraak | 171 tests + 30 rgs-goldens | ongewijzigd |
| 11 toka-pariteit | 29 cases, 45 rgs, 27 tests | ongewijzigd |
| 12 Drools gespeeld / JUnit | 108 / 199 | **109** / 199 |
| 13 draaiingen / goldens | 64 / 13 | **68 / 14** (0 survivors) |

Poort 13's boekhoudklem blijft op nul: 0 annotaties bij een
vierde-klasse-tak en 0 buiten de draaitabel.
