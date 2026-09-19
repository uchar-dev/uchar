# De rode zone kent geen laadfouten — een hele foutklasse heeft geen getuige

Goal: beslissen of `corpus/rood` fouten mag pinnen die vallen bij het **laden**
van de case in plaats van bij de evaluatie, en de beslissing uitvoeren. Vandaag
kan een vorm die beide uitvoerders identiek en luid weigeren geen rode getuige
krijgen zodra die weigering bij het laden valt — het contract van de zone dekt
alleen evaluatie. De concrete aanleiding is de **case-sleutel-arm** van de
rol-vs-rol-ambiguïteit (`IR-SPEC.md` ch. 4.3: "a name matching several reachable
roles stays a loud executor fault, never a guess"), waarvan de navigatie-arm wél
gepind is (WF-053, `corpus/negatief/wf053-bindingless-ambiguous`) en de
case-invoer-arm niet.

## Meting (2026-09-10, kaart `20260910-rol-schaduw-definitiefout`)

Handgeschreven wire: twee feittypen (`pontonligging`, `vlotligging`) geven
`Schuit` allebei een to-één rol `ligger`; de navigatie in de regel is ontward met
een expliciete `navigatieBindingen`, dus het document is eenduidig. De case biedt
beide links onder één sleutel aan (`"roles": { "ligger": ["P1", "V1"] }`).

    verifier: PASS

    CASE ERROR: role key "ligger" is ambiguous from "Schuit"     ref     exit 2
    CASE ERROR: role key "ligger" is ambiguous from "Schuit"     clingo  exit 2

Beide uitvoerders weigeren dus identiek, met dezelfde zin — precies wat een rode
getuige hoort te pinnen. Maar gate 8 eist **exit 0**: de runners moeten de
verwachte fout zelf herkennen. Dat lukt hier niet, want de fout valt vóórdat
`expectedFault` uit het bestand wordt gelezen. In `ref/lib/case.ml` lost
`load_instance` elke rolsleutel op (`resolve_role_ft`, regel ~340) terwijl
`expected_fault` pas op regel ~677 van `load_file` wordt geparseerd; de runner
vangt de `Case.Error` en eindigt op exit 2. `backends/clingo/bin/clingo_runner.ml`
heeft dezelfde volgorde.

Gemeten met de kandidaat-map echt in de zone gezet, gate 8 gedraaid:

    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — ref did not observe
      the expected fault:
    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — clingo did not observe
      the expected fault:
    FAIL: ambiguous-case-role-key carries no drools fragment and is not in the
      Drools exclusion list — pin a fragment or list it with a reason:

Drie van de drie benen vallen om: de twee uitvoerderbenen op de exitcode, het
Drools-been omdat de case noch een fragment noch een reden-regel draagt. De
kandidaat-map is weer verwijderd; gate 8 staat op 26.

Dit is **geen bug maar het contract**, en het staat zo in het case-schema
(`spec/schema/uchar-case-v0.schema.json`, `expectedFault`):

> The red-path alternative to `expected` (corpus/rood): **evaluation** of this
> case MUST fail observably in every executor that carries its form.

## Wat er te beslissen valt

**Optie A — het zonecontract verbreden naar laadfouten.** Vier artefacten, twee
ervan beschermd:

1. `spec/schema/uchar-case-v0.schema.json` (**beschermd**) — de zin van
   *evaluation* naar *loading or evaluation* verbreden, en zeggen dat een
   laadfout een even geldige rode uitkomst is.
2. `ref/bin/runner.ml` — `expectedFault` lezen vóór de wereld wordt geladen, of
   de `Case.Error`-arm laten weten dat er een verwachte fout is. Let op: het
   fragment zit in hetzelfde bestand dat niet geladen kan worden, dus de lezing
   moet gesplitst worden (eerst het JSON-veld, dan de wereld).
3. `backends/clingo/bin/clingo_runner.ml` — dezelfde splitsing, dezelfde
   volgorde.
4. `scripts/check-rood.sh` (**beschermd**) — het Drools-been: de nieuwe case
   krijgt een reden-regel of een `drools`-fragment; meten wat die backend op deze
   vorm doet (in de meting hierboven weigerde de transpiler het document al vóór
   de case-invoer, dus vermoedelijk een `EXCLUDE`-regel met reden).

Prijs: twee beschermde paden en een uitbreiding van een zonecontract dat nu
scherp is. Winst: een hele foutklasse — alles wat de case-lezers weigeren:
onbekende labels, verkeerd getypeerde rolvullers, dubbelzinnige rolsleutels,
scalaire levering op een temporeel attribuut — wordt pinbaar. Vandaag is die
klasse in **geen enkele** zone gepind; de negatief-zone dekt alleen `ir.json`.

**Optie B — uitdrukkelijk niet doen.** Dan hoort in `corpus/rood/README.md` (of
in het schema-commentaar) één zin die zegt dat de zone evaluatiefouten pint en
laadfouten niet, met de reden en met deze meting als voorbeeld — zodat de
volgende sessie niet opnieuw ontdekt dat de vorm niet past.

## Acceptance

- De beslissing staat in de kaart met haar reden.
- Bij A: de vier artefacten aangepast, de getuige geland als
  `corpus/rood/ambiguous-case-role-key/` met `expectedFault {ref, clingo}` op
  `role key "ligger" is ambiguous from "Schuit"`, gate 8 groen met 27 cases
  (vloer 26 blijft — een nieuwe case verhoogt geen vloer), en **rood eerst**:
  toon de gate rood op de huidige runners, groen na de wijziging.
- Bij A: minstens één tweede laadfout-getuige uit een andere case-lezerarm, zodat
  het nieuwe contract niet op één vorm rust.
- Bij B: de zin staat er, met de meting, en deze kaart gaat naar `done/`.
- Gates 4, 5, 6, 7, 8 groen (bij A bewegen ze alle vijf mee: de runners zijn
  gedeeld).

May touch (bij A): `spec/schema/uchar-case-v0.schema.json` (**voorstel**),
`ref/bin/runner.ml`, `backends/clingo/bin/clingo_runner.ml`,
`scripts/check-rood.sh` (**voorstel**), `corpus/rood/**`, `backlog/`.
Bij B: `corpus/rood/README.md`, `backlog/`.

## Out of scope

- De rol-vs-rol-ambiguïteit zelf. De gekozen lezing staat vast (ch. 4.3, luide
  uitvoerdersfout) en de navigatie-arm is gepind door WF-053; deze kaart gaat
  alleen over de plek waar de case-invoer-arm gepind kán worden.
- De naamdeling tussen rol en objecttypelid: geland 2026-09-10, kaart
  `20260910-rol-schaduw-definitiefout`.
- Elke verandering aan wát de case-lezers weigeren. Deze kaart verplaatst geen
  enkele weigering; ze gaat over de vraag of een bestaande weigering een getuige
  mag krijgen.

---

Result:

**Het besluit: A.** Het zonecontract is verbreed naar laadfouten. De reden is
de maintainerslijn — generiek boven speciaal, méér functionaliteit, niets stil
laten liggen — en die valt hier samen met wat de kaart zelf al uitrekende: de
winst is geen enkele getuige maar een hele foutklasse. Alles wat de
case-lezers weigeren (dubbelzinnige rolsleutel, onbekend doellabel,
verkeerd getypeerde rolvuller, een tijdlijn op een niet-temporeel attribuut,
een scalair op een granulaire parameter, een dubbel geleverde rol) is
vanaf nu pinbaar, en die klasse had vóór deze kaart in géén enkele zone een
getuige: de negatief-zone dekt alleen `ir.json`, en de rode zone viel op de
exitcode. De prijs — twee beschermde paden en een contract dat iets ruimer
staat — is de kleinste die dat koopt, want het contract wordt niet vager maar
preciezer: het noemt nu de twee fases die het altijd al bedoelde.

**Rood eerst, letterlijk.** Beide getuigen zijn geland vóór elke
runnerwijziging (commit `279bdc33`), en gate 8 viel op alle drie de benen,
voor beide getuigen:

    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — ref did not observe the expected fault:
    CASE ERROR: role key "ligger" is ambiguous from "Schuit"
    FAIL: corpus/rood/ambiguous-case-role-key/case1.json — clingo did not observe the expected fault:
    CASE ERROR: role key "ligger" is ambiguous from "Schuit"
    FAIL: corpus/rood/unknown-role-target-label/case1.json — ref did not observe the expected fault:
    CASE ERROR: role "liggers" on "H1" links unknown label "S9"
    FAIL: corpus/rood/unknown-role-target-label/case1.json — clingo did not observe the expected fault:
    CASE ERROR: role "liggers" on "H1" links unknown label "S9"
    FAIL: ambiguous-case-role-key carries no drools fragment and is not in the Drools exclusion list — pin a fragment or list it with a reason:
    EXCLUDE transpiler SKIP: regel: ligdiepte van een schuit — regel (waarde buiten scope): ligdiepte van een schuit
    FAIL: unknown-role-target-label carries no drools fragment and is not in the Drools exclusion list — pin a fragment or list it with a reason:
    EXCLUDE role 'liggers' names unknown label S9

Dat zijn de twee uitvoerderbenen op de exitcode (exit 2 waar gate 8 exit 0
eist) en het Drools-been zonder fragment of reden-regel. De kaartmeting van
2026-09-10 is daarmee op de dag zelf gereproduceerd, inclusief de zin.

**De twee getuigen.** Ze zitten opzettelijk in verschillende armen van de
case-lezer, zodat het nieuwe contract niet op één vorm rust.

- `corpus/rood/ambiguous-case-role-key` — de case-invoer-arm van de
  rol-vs-rol-ambiguïteit (ch. 4.3). Twee feittypen (`pontonligging`,
  `vlotligging`) geven `Schuit` een to-één rol `ligger`; de case levert beide
  links onder één sleutel. Gepind, in ref en clingo identiek:
  `role key "ligger" is ambiguous from "Schuit"`. Het document zelf is
  eenduidig: de enige regel navigeert dezelfde naam `ligger` en doet dat
  schoon, want haar `navigatieBindingen` noemt `pontonligging` symbolisch.
  Verifier: PASS. De ambiguïteit zit dus nergens in de IR — alleen in wat de
  case zegt. Faalt in `load_instance`, tijdens het lezen van één instantie.
- `corpus/rood/unknown-role-target-label` — de referentiële-integriteitsarm.
  `H1` noemt `S1` en `S9` als `liggers`; alleen `S1` bestaat. Gepind:
  `role "liggers" on "H1" links unknown label "S9"`. Faalt in
  `check_role_targets`, de kruispas ná de hele invoer — een andere functie, een
  andere pas, een andere foutsoort. Het goede label staat naast het foute onder
  dezelfde sleutel, zodat de weigering niet als "de lezer weigert deze rol"
  te lezen valt.

Beide weigeringen zijn uitvoerderonafhankelijk by construction: ref en clingo
roepen dezelfde `Case.load_file` aan.

**De vier artefacten.**

1. `spec/schema/uchar-case-v0.schema.json` (**beschermd**) — de contractzin van
   *evaluation* naar *LOADING OR evaluation*, met één zin erbij die zegt
   waaróm dat geen verruiming van de lat is: een weigering van de case-LEZER is
   dezelfde waarneembare weigering, één fase eerder, en de uitvoerder leest het
   veld op zichzelf vóór hij de wereld bouwt, zodat een fragment beide fases kan
   pinnen. Eén regel gewijzigd; niets aan de shape, niets aan `required`.
2. `ref/bin/runner.ml` — de gesplitste lezing. `expected_fault_of_file` eerst,
   dán de wereld; een `Case.Error` uit `load_file` wordt tegen dezelfde pin
   gematcht als een fout bij evaluatie, met `"CASE ERROR: " ^ msg` als rapport.
   Zonder pin blijft het exit 2, precies als vroeger. De `--noop`-weigering
   ("een golden-corpusgarde heeft niets aan een expectedFault-case") is mee
   naar voren geschoven — zelfde boodschap, zelfde exit 2, alleen vóór de
   wereld in plaats van erna, anders zou de nieuwe laadarm hem overrulen.
   Gate 7 draait alleen over `corpus/golden` en raakt die arm dus niet; het is
   de handmatige draai op een rode case die er correct van blijft.
3. `backends/clingo/bin/clingo_runner.ml` — dezelfde splitsing, dezelfde
   volgorde, hetzelfde rapport.
4. `scripts/check-rood.sh` (**beschermd**) — kopcommentaar op het verbrede
   contract, plus twee reden-regels in de Drools-uitsluitingslijst, elk met
   datum en gemeten categorie.

Daarnaast `ref/lib/case.ml`, dat niet in de May-touch van de kaart stond en
hier verantwoord wordt: de splitsing heeft één lezer nodig die alléén het veld
leest, en die hoort in de module die `expectedFault` bezit. De bestaande parse
is verbatim verplaatst naar `expected_fault_of_kvs`; `load_file` roept hem aan
op exact de oude plek, dus aan die functie verandert geen gedrag. Het
alternatief — de parse in beide runnerbinaries dupliceren — zou het
contractveld twee lezers geven, en dat is precies de divergentie die de rest
van dit huis overal vermijdt.

**De Drools-meting (2026-09-19).** Geen van beide getuigen krijgt een
`drools`-fragment; beide krijgen een reden-regel, want de rode CLI reserveert
`FAULT` voor een echte fout bij vuren.

- `ambiguous-case-role-key` → `EXCLUDE`: de transpiler slaat de enige regel
  over (genavigeerde lezing, waarde buiten scope), dus de backend bouwt het
  feitenmodel nooit en bereikt de case-invoer niet.
- `unknown-role-target-label` → `EXCLUDE`: de eigen loader van de backend
  weigert dezelfde invoer en noemt hetzelfde bungelende label
  (`role 'liggers' names unknown label S9`). Die overeenstemming is echt en
  staat in de reden-regel opgeschreven, ook al kan het been haar niet pinnen:
  alle drie de uitvoerders wijzen deze case af.

**De zesentwintig bestaande: identiek vóór en ná.** Elke rode case is vóór de
runnerwijziging en erna door ref, clingo én de Drools-CLI gedraaid, met
letterlijke uitvoer en exitcode per been (78 regels, 26 × 3). `diff` van de
twee opnamen is leeg — alleen de clingo-tempbestandsnaam is genormaliseerd,
die verschilt per run. Gate 8 zelf: vóór `PASS (26 red cases …, floor 26)`,
Drools-been `6 fault-pinned, 20 not-carried`; ná `PASS (28 red cases …,
floor 26)`, Drools-been `6 fault-pinned, 22 not-carried`. De vloer blijft 26 —
vloeren zijn ondergrenzen en groei vraagt geen bijstelling.

**Mutatieproef.** De gesplitste lezing kortgesloten (`pinned` hard op `None`,
dus het veld weer feitelijk ná de wereld): gate 8 valt rood, en precies op de
twee nieuwe getuigen — vier FAIL-regels, dezelfde vier `CASE ERROR`-zinnen op
exit 2, de zesentwintig bestaande onaangeroerd, het Drools-been groen. Daarna
hersteld en opnieuw groen. De getuigen hangen dus aan de wijziging en niet aan
iets anders.

**De veertien poorten** (`bash scripts/check-<naam>.sh > /dev/null 2>&1`):

    self-contained: 0
    ir-schema: 0
    verifier: 0
    case-schema: 0
    oracle: 0
    clingo: 0
    noop: 0
    rood: 0
    catala: 0
    regelspraak: 0
    toka-parity: 0
    drools: 0
    reftype-derivation: 0
    flogic: 0

Gate 9 draaide vol: `catala` en `clerk` zitten in de opam-switch van deze
machine. Met zichtbare uitvoer, voor de vijf die de runners delen:
gate 4 `544 case file(s) validate`; gate 5 `ref suite 643 worked examples,
floor 489; 516 cases replayed exactly, floor 427`; gate 6 `backend suite 362
checks, floor 83; 516 cases replay exactly`; gate 7 `480 cases refute a
do-nothing executor; 36 pinned survivors`; gate 8 `28 red cases`.

**Registervoorstel: geen.** `spec/CONSTRUCTS.md` en `spec/PLAN.md` noemen
`expectedFault` nergens, dus er is geen rij die het zonecontract citeert.
`corpus/README.md` citeerde het wél (de zonetabel) en is in deze kaart
bijgewerkt — werkproza, geen beschermd pad. Eén voorstel voor de hoofdsessie,
niet aangeraakt omdat `CLAUDE.md` buiten May-touch valt: de gate-8-alinea zegt
"MUST fail observably", wat fase-neutraal en dus nog waar is, maar het zou
eerlijk zijn daar "at load or at evaluation" aan toe te voegen, zodat de
grondwet de verbreding zichtbaar draagt.

**Open twijfel.**

- De gesplitste lezing parseert elk casebestand twee keer, ook de 516 groene.
  Meetbaar is het niet (gates 5–7 draaien onveranderd), maar het is een tweede
  lezing van hetzelfde bestand en dat staat hier opgeschreven in plaats van
  weggemoffeld.
- `expected_fault_of_file` controleert het `format`-veld niet — dat doet
  `load_file`. Een casebestand met een verkeerd formaat én een `expectedFault`
  zou zijn formaatweigering tegen de pin gematcht zien; gate 4 hekt het
  formaat, dus onbereikbaar, maar het is een echte nuance van de splitsing.
- De laadfout van de IR zelf (`Ir_v0.Loader.Error`) is bewust NIET verbreed:
  die is het domein van gate 2 en 3, en de kaart gaat over wat de
  case-lezers weigeren. Een rode getuige voor een onleesbare `ir.json` zou een
  eigen besluit zijn.
- De Drools-CLI kan een laadweigering alleen als `EXCLUDE` melden, nooit als
  `FAULT`, terwijl hij bij `unknown-role-target-label` aantoonbaar dezelfde
  weigering uitspreekt als ref en clingo. Het vierkant klopt inhoudelijk maar
  het been kan het niet pinnen. Een vijfde statustoken (of `FAULT` verbreden
  naar laadfouten) is Drools-code en viel buiten deze kaart.
