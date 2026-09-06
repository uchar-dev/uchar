# WF-034, zesde golf: de restvelden buiten de expressiewandeling

Goal: de vijfde golf en haar fixronde sloten drie naamvlakken buiten de
expressiewandeling (de vier beanvelden van een feitcreatie, het subject van een
objectcreatie, `anderTeFiller` van de nabije rolcheck) en lieten hun eigen
restlijst gemeten achter. Die restlijst staat sinds 2026-09-05 gedateerd in de
open-lijst van ch. 4.6 — deze kaart is de opdracht om hem af te lopen: per veld
kruisen wat te kruisen valt, of het veld met een gemeten argument definitief
naar de open-lijst schrijven. Route als de vorige golven: één arm per vlak, elk
rood-eerst met eigen negatief, arm-exact mutant-bewezen, nul over-afwijzing over
de vier zones.

## De meting (2026-09-05, 503 vier-zone-documenten)

Alles hieronder is geteld over golden + rood + negatief + pariteit op de dag van
deze kaart; een latere telling wijkt af en moet opnieuw.

| veld | dracht | wat er nu mee gebeurt | executeergedrag | oordeel |
| --- | --- | --- | --- | --- |
| **zijtabelsleutels** van een objecttype: de acht per-lid-maps (`attribuutMeervoud`, `attribuutDimensies`, `attribuutGranulariteit`, `attribuutTijdlijnAantal`, `attribuutTijdlijnNaam`, `kenmerkSoorten`, `kenmerkGranulariteit`, `kenmerkTijdlijnAantal`) | **916 sleutels**, 0 wezen vandaag | geen regel eist dat een sleutel een gedeclareerd lid noemt | **te meten** (een weessleutel hangt een granulariteit/meervoud/dimensie aan een lid dat niet bestaat — de lezers vallen stil, geen van drieën klaagt) | **voorrangspost, sluiten** |
| `Kenmerk.eigenaar` (de expliciete eigenaar van een kenmerkcheck) | **2 van 190** checks, **0 golden** (beide dragers zijn negatiefs) | alleen gelezen als de sleutel van een afhankelijkheidskant (`dep_cond`), nergens tegen de objecttypetabel gekruist | **te meten** (een onbekende eigenaar bouwt stil een kant op een knoop die niet bestaat: de stratificatie leest een lege buur) | sluiten als er een getuige te maken is |
| `Eenheid.stelsel` | **1292 van 1292** aanwezig | nergens tegen de gedeclareerde eenheidstelsels gekruist | te meten (WF-009's buurt) | sluiten of gemotiveerd open |
| `Eenheidstelsel.naam` | **195 van 195** aanwezig | nergens tegen de mapsleutel gekruist | te meten | sluiten of gemotiveerd open |
| de onopgeloste `regels`-stubs | **658** | `ctx.doc.regels` wordt door **geen enkele check** gelezen | te meten (de opgeloste vormen dragen alles; een stub mag vandaag letterlijk alles beweren) | eigen vraag: kruisen, of de stub normatief tot presentatie verklaren |
| `dagsoortPerEenheid` | **60**, alle 60 in de **pariteitszone**, 0 daarbuiten | ongekruist | te meten | dilemma (zie acceptance): geen golden-getuige beschikbaar |
| `VerreRollidmaatschap`'s eigen vuller + backref | **0 posities in alle vier zones** | ongekruist; de tweelingzus van (o), die wél dracht heeft | niet te meten zonder eigen golden | dilemma (zie acceptance) |
| `objectCreaties[].subject: null` | de lege helft van (n) | ongekruist | gemeten: verifier PASS, **beide** uitvoerders luid (`missing ObjectCreatie.subject`) | sluiten — de getuige is goedkoop |
| `anderTeFiller: null` | **1 van de 10** rolchecks | (o) gaat er bewust overheen | te meten (de drager is een dagsoortconditie waarvan de wandeling het subject óók niet kent) | sluiten of gemotiveerd open |

## De voorrangspost: de zijtabelsleutels

916 posities is verreweg de grootste rest, en het is de enige waar de schade
een **stille semantiekwijziging** is in plaats van een lege lezing. Een sleutel
in `kenmerkGranulariteit` die geen gedeclareerd kenmerk noemt, zegt "dit kenmerk
is tijdsafhankelijk" over een kenmerk dat niet bestaat; het gedeclareerde
kenmerk waarvoor de sleutel bedoeld was, blijft daarmee stilzwijgend
níet-temporeel, en dat is een ander programma — geen fout, een ander antwoord.
Dezelfde vorm geldt voor `attribuutMeervoud` (de rolnaamresolutie van WF-047 en
de ch. 4.3-terugval lezen die tabel), voor `attribuutDimensies` (WF-040's
label/dimensie-kruising) en voor `attribuutGranulariteit` (WF-043). Eén
weessleutel schakelt dus meerdere lezers tegelijk uit — precies het patroon dat
de kenmerkcheck-arm van golf 3 motiveerde, nu een niveau hoger, op de
declaratie in plaats van op de referentie.

Begin hier, en meet per tabel afzonderlijk: de acht tabellen zijn acht
onafhankelijke lezers en een mutant per tabel moet precies zijn eigen document
vrijspelen, anders is het één arm met acht getuigen en geen acht armen.

## Acceptance

- Per gesloten vlak minstens één negatief, rood-eerst zichtbaar (PASS onder de
  verifier van vóór deze kaart, één regel erna), met de arm-exacte mutant erbij:
  alleen die arm weg → alleen het eigen document schoon, over de volle
  vier-zone-sweep.
- **Het nul-dracht-dilemma expliciet beslissen, niet omzeilen.** Twee regels van
  het huis staan hier tegenover elkaar: (a) een spec-zin zonder
  machine-controleerbaar artefact is niet af — dus geen clausule zonder getuige;
  (b) de open-lijst is de plek voor een gemeten flank die je *niet* sluit. Voor
  `VerreRollidmaatschap` (0 dracht) en `dagsoortPerEenheid` (0 buiten de
  pariteitszone) betekent (a) dat er eerst een **golden** moet komen die de vorm
  draagt, of dat het veld met de meting naar de open-lijst gaat. Een negatief
  bouwen op een vorm die geen enkele golden draagt is de derde weg en is
  verboden: hij zou een clausule geven die geen enkel echt document ooit
  passeert, en de sweep zou dat niet zien. Kies per veld, schrijf de keuze op,
  en als het (a) wordt: de golden hoort bij een aparte corpuskaart, niet hier.
- De tellingen in deze tabel worden bij het landen **hermeten en gedateerd**, in
  IR-SPEC (vier zones, negatiefzone), in de indexrijkop (armen, golven,
  fixrondes) en in de betrokken NOTE's. Een golf die zijn eigen tellingen laat
  verlopen is de fout die de fixrondes van golf 4 en 5 moesten herstellen.
- De nul-voorkomens-claim van de indexrij blijft **gesplitst**: hij geldt voor
  de achttien vormen van golf 1-4, niet voor armen die posities kruisen die het
  corpus wél draagt. Elke arm van deze golf hoort in de tweede helft.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

## Out of scope

- **De Drools-namencontract-bug — eigen backend-kaart.** De vijfde golf en haar
  fixronde maten drie keer hetzelfde: een feitcreatie met een beanveld dat geen
  §13.3.9-veld noemt laat de backend op *fire* sterven met een
  ongeclassificeerde `IllegalStateException: geen getter voor '…'`. Zijn eigen
  leer is anders — de case-invoerkant heeft er een luide poort voor
  (`naamdelingsPoort`, `heeftReferentieVeld`) en de vouwkant weigert dezelfde
  grens met een reden-dragende SKIP — en poort 12 zegt met zoveel woorden dat
  een ERROR bij fire nooit een waardedivergentie is. De schrijfkant van de
  feitcreatie mist die poort. Dat is een backendreparatie, geen verifierregel:
  deze kaart maakt invoer ill-formed, zij repareert geen uitvoerder.
- `Rollidmaatschap.rol` — presentatie: de uitvoerders leiden de verbindende rol
  af uit subject + `anderTeFiller` en lezen het veld niet. Er is niets om tegen
  te kruisen zonder eerst te beslissen dat het veld betekenis krijgt.
- `Eenheid.afkorting` en de rest van WF-009's eenhedenvraag: eigen regel, eigen
  kaart.
- De rij-1-wrat en zijn restwijdte (acht posities, twee golden — geteld in golf
  4 en door golf 4 gescoped, niet gerepareerd).
- De sentinel op de 9 fold-/aggregatie-argumenttypes: de aggregatietak vervangt
  daar de generieke wandeling, eigen vraag.
- WF-007's precisievormcontrole, die dezelfde blinde vlek in de containers heeft
  — die wandeling is van WF-007, niet van WF-034.

---

## Result (geland 2026-09-05, branch `card/wf034-zesde-golf`)

**Twaalf armen, twaalf negatiefs, alle dertien gates groen.** Negatiefzone
303 → **315**; vier zones 510 → **522**. Over de **510** documenten die deze
kaart voorafgaan verschuift **geen enkel oordeel** en verschuift **precies één
rapport** — `wf031-dagsoort-leest-rolstaat` krijgt een waarachtige incidentele
WF-034-regel en houdt zijn WF-031-oordeel.

### Wat er landde

**De voorrangspost — acht armen op de zijtabelsleutels (ch. 4.6 (p)).** Elke
sleutel van de acht per-lid-maps moet een gedeclareerd lid noemen (ci-identiteit
van ch. 2.4, want zo zoekt elke lezer de facet op). Acht afzonderlijke armen in
één functie `wf034_zijtabelsleutels`, acht negatiefs `wf034-wees-<tabel>`, en
acht arm-exacte mutanten: één regel weg → precies één document schoon en de
volle vier-zone-sweep verder identiek.

De lezersmeting per tabel (2026-09-05, over verifier, `ref`, `backends/clingo`
en `backends/drools`) staat in de code, in ch. 4.6 (p) en in elke NOTE:

| tabel | lezers | dracht (4 zones, vóór de kaart) |
| --- | --- | --- |
| `attribuutMeervoud` | WF-057; Drools laadt hem, niemand leest hem terug | 24 |
| `attribuutDimensies` | WF-004, WF-040's celkruising | 9 |
| `attribuutGranulariteit` | `attr_temporal`, WF-005, WF-043, de temporele proloog van `expression_layer`, WF-065 — **en alle drie de uitvoerders** | 74 |
| `attribuutTijdlijnAantal` | WF-005; beide Drools-transpilertakken | 2 |
| `attribuutTijdlijnNaam` | WF-003, WF-005; Drools laadt hem, niemand leest hem terug | 2 |
| `kenmerkSoorten` | **niemand, in geen van de vier consumenten** | 788 |
| `kenmerkGranulariteit` | WF-043 (expressie én conditie), `expression_layer`, WF-086 — **en alle drie de uitvoerders** | 24 |
| `kenmerkTijdlijnAantal` | WF-005; beide Drools-transpilertakken | 1 |

Metingen: een **toegevoegde** wees-sleutel op elk van de 205 golden-objecttypen
→ 205/205 schoon vóór, 0 na, per tabel (`attribuutDimensies` 2/2, want zijn
waarde heeft een gedeclareerde dimensie nodig). Een **hernoemde levende**
sleutel: `attribuutMeervoud` 6/6, `attribuutGranulariteit` 20/33,
`attribuutTijdlijnAantal` 1/1, `kenmerkSoorten` 124/124 schoon vóór en 0 na —
en, eerlijk genoteerd, drie tabellen waar dat instrument niet werkt omdat het
hernoemen al wordt afgewezen (`attribuutDimensies` 0/2,
`kenmerkGranulariteit` 0/9, `attribuutTijdlijnNaam` 0/2) en één zonder levende
golden-sleutel (`kenmerkTijdlijnAantal`).

Uitvoerdersgedrag van een hernoemde levende sleutel (golden × case):
`attribuutMeervoud` 8, `attribuutTijdlijnNaam` 1 en `kenmerkSoorten` 438
metingen, **alle byte-identiek in beide OCaml-uitvoerders** — niemand ziet het.
`attribuutGranulariteit` 99 metingen: 91 luid in beide, 6 stil in beide, en **2
DIVERGENT** (`synth2-prorata-breed` case4: `ref` speelt naar de gepinde
verwachtingen terwijl clingo de pro-rata-basis weigert). `kenmerkGranulariteit`
27: 26 luid, en **1 divergent op een ANKER-golden** —
`anchor/jutterswet-dubbelwacht` case3, waar `ref` op `gedurende` faalt en
clingo de case schoon speelt.

**`Kenmerk.eigenaar` — gesloten, gemotiveerd (ch. 4.6 (q)).** De kaart vroeg om
een expliciete beslissing; die is: **sluiten**. Het clausule-zonder-golden-
bezwaar gaat hier *niet* op, en waarom staat in de spec én in de NOTE: (1) het
veld wórdt gelezen — het is de knoopsleutel van de afhankelijkheidskant, in de
verifier én in de Drools-stratificatie en producent/consument-graaf; (2) de
vorm bestaat echt in het corpus (2 van de 190 checks, beide in de negatiefzone,
beide resolvend) — anders dan bij `VerreRollidmaatschap`, waar geen enkele zone
de vorm draagt; (3) de schade is **gemeten**, niet beredeneerd, en het is een
stille antwoordverandering in de derde uitvoerder. Op
`jutterswet-wachtploeg`: `eigenaar: null` → PLAYED; `eigenaar: "Wachtpost"`
(gedeclareerd, ≠ subject) → de backend LAAT DE REGEL VALLEN (transpiler SKIP);
`eigenaar: "Bestaatniet"` → PLAYED. Een regel die de engine onder een echte
eigenaar wéigert, draagt en beantwoordt hij onder een onzinnige. Beide
OCaml-uitvoerders zijn hier blind door constructie (alle spellingen PASS).
96/96 golden-checkposities schoon vóór, 0 na.

**`Eenheid.stelsel` — gesloten (ch. 4.6 (r)).** De arm rijdt mee in
`check_eenheid_form`, de enige wandeling die élk eenheidsrecord bereikt. De
schade is een **gemeten ref ≢ clingo-divergentie** op `jutterswet-sleepbaan`:
verifier-schoon, `ref` PASS (2 verwachtingen), Drools PLAYED, en clingo
`TRANSLATION FAULT: non-identical units under the write to "sleeplengte" are
not translated` — de ASP-hersteltellingpas gaat stil uit omdat de stelselnaam
niet meer resolveert. 21/21 (golden, stelsel)-paren schoon vóór, 0 na; van de
2442 eenheidsrecords in de vier zones dragen er 2309 een `stelsel` en resolven
ze alle, de overige 133 dragen `null` (het procentteken en de kale
kalendereenheden) en die passeert de arm door constructie.

**`Eenheidstelsel.naam` — gemotiveerd OPEN.** Gemeten 205/205 aanwezig en
205/205 in overeenstemming met de mapsleutel; enige lezer is één ASP-foutregel.
Reden om niet te sluiten: het is **geen referentie** (WF-034 gaat over
verwijzingen die resolven; dit is of een spiegelveld met zijn sleutel
overeenkomt — de naam-identiteit van ch. 2.1), en het is één instantie van een
**klasse** die objecttypen, feittypen, dimensies en tijdlijnen net zo goed
raakt en die vandaag nergens gekruist wordt. Alleen deze ene sluiten zou
willekeurig zijn. Staat in de open-lijst, met eigen kaartvoorstel
`backlog/new/20260905-zelfnaam-tegen-mapsleutel.md`.

**De twee lege helften — gesloten (ch. 4.6 (n) en (o)).**
`objectCreaties[].subject: null` (11/11 golden-creaties schoon vóór, 0 na;
beide uitvoerders luid `missing ObjectCreatie.subject`) en `anderTeFiller:
null` (4/4 schoon vóór, 0 na; beide luid `missing
Rollidmaatschap.anderTeFiller`). Elk een eigen arm naast de gevulde helft,
omdat een naam-die-nergens-op-slaat en een afwezige naam verschillende vragen
zijn die verschillend worden waargenomen. Beide getuigen breken hetzelfde
document als hun gevulde zusje, zodat elke positie in béíde spellingen een
getuige heeft.

**De `regels`-stubs — presentatie (ch. 3).** Per maintainerbesluit van
2026-09-05: een expliciete niet-normatieve status, volgens het patroon van de
inerte v0-spiegelbaggage van ch. 3.4, met de gedateerde meting. De
bevestigingsmeting die gevraagd werd: over de 522 documenten van de vier zones
dragen ze **691** stubs (313 golden, 34 rood, 291 negatief, 53 pariteit) en
**nul lezers** — geen check in `spec/verifier/`, niets in `ref/`, niets in
`backends/clingo`, en `backends/drools` párseert ze in zijn modelrecord zonder
dat transpiler, runtime, graaf of case-runner dat record ooit terugleest. Geen
kruisingsclausule. Het veld blijft op de wire door WF-019 alleen.

**Open gebleven, met gedateerde regel in ch. 4.6:** `dagsoortPerEenheid` (60
ingangen, alle 60 in de pariteitszone, 0 daarbuiten — geen publieke getuige, en
een negatief bouwen op zo'n vorm is door de kaart verboden; corpuskaart
`backlog/new/20260905-dagsoort-per-eenheid-golden.md`);
`VerreRollidmaatschap`'s eigen vuller en backref (0 posities in álle vier
zones, dus géén corpuskaart — de vorm bestaat nergens); de **lid-helft** van
(q) (of het genoemde type het kenmerk declareert — de bestaande kruising is op
het *subject* gesleuteld, en beslissen dat een expliciete eigenaar dat overrulet
is een semantiekvraag over wie gezag heeft, niet deze kaart); WF-009's
eenhedenvraag; en `Eenheidstelsel.naam` hierboven.

### Bijvangst en semantiekverbreding — expliciet

1. **`check_eenheid_form` draagt nu twee regels.** De functie was WF-092's
   vormwet; de `stelsel`-arm rijdt erin mee, omdat dit de enige wandeling is
   die élk eenheidsrecord (inclusief noemers) bereikt. `~stelsels` is door
   `check_datatype_units` heen geregen naar alle zeven aanroepplekken en is
   **verplicht, niet optioneel**: een nieuwe aanroepplek kan de kruising niet
   stilzwijgend overslaan. WF-092's indexrij is bijgewerkt zodat de claim "één
   vormfunctie, elke tak gemuteerd" waar blijft — die metingen gaan over
   WF-092's *eigen* takken.
2. **Eén bestaand document beweegt.** `wf031-dagsoort-leest-rolstaat` krijgt de
   incidentele WF-034-regel van de `anderTeFiller`-lege-helft. Het is geen
   legitieme vorm die geweigerd wordt: WF-031 verbiedt een rolstaatlezing in
   een dagsoortconditie sowieso, en juist daarom was er geen subject om de
   vuller uit af te leiden.
3. **De `kenmerkSoorten`-arm heeft vandaag geen enkele lezer.** Hij is er voor
   de wire, op hetzelfde argument als de dimensiecel-arm van golf 3, en dat
   staat met zoveel woorden in ch. 4.6 (p) en in zijn NOTE — niet weggemoffeld.
4. **Ci-identiteit, geen byte-exactheid.** De acht armen gebruiken de
   naam-identiteit van ch. 2.4, want dat is hoe elke lezer de facet opzoekt.
   Een sleutel die alleen in kapitalisatie afwijkt is dus géén wees. Bewust, en
   genoteerd.
5. **De presentatie-alinea mint geen ongedekte MUST.** Hij stelt de status
   vast ("niet-normatief; niets in deze specificatie geeft een stub betekenis")
   in plaats van een consumenten-MUST te schrijven waar geen artefact achter
   zit — het huis eist een machine-controleerbaar artefact achter elke
   normatieve MUST, en die is er hier bewust niet.
6. **Kruisverwijzingen toegevoegd** in ch. 2.2 (de zijtabellen), ch. 2.5 (de
   `stelsel`-referentie), ch. 3.3 (`subject` present én gedeclareerd), ch. 4.5
   (de `eigenaar`-cel en de `Rollidmaatschap`-cel), WF-009's clausule (de
   scheiding tussen tabel-intern en occurrence-terugverwijzing) en WF-092's
   indexrij. Ch. 2.2 en ch. 2.5 herhalen de nieuwe MUSTs normatief; de rest
   zijn wijzers.
7. **De nul-voorkomens-claim van de indexrij blijft gesplitst.** De twaalf
   armen van deze golf staan in de tweede helft, bij de negen van golf 5: ze
   kruisen posities die het corpus wél draagt (924 zijtabelsleutels, 190
   kenmerkchecks, 2309 stelseldragende eenheden), dus hun bewijs is de sweep,
   niet de afwezigheid.

### Rood-eerst en mutant-uitslag

Alle twaalf negatiefs zijn eerst PASS getoond onder de verifier van vóór de
kaart (md5 `22e158ee7598d0b2c91f28dfe164fe8f`) en daarna FAIL onder de nieuwe
(md5 `a22b5bda655aaf4727b6b3805d6e2053`). Twaalf arm-exacte mutanten, elk in
een verse `git archive`-kopie in `/tmp`, elk met harde build-faal en
binary-md5 in de keten: elke mutant speelt **precies zijn eigen** negatief vrij
en laat de volle vier-zone-sweep (522 documenten) verder identiek. De
`anderTeFiller`-mutant beweegt er twee — zijn eigen negatief (naar PASS) en de
incidentele regel van `wf031-dagsoort-leest-rolstaat` (oordeel onveranderd) —
en dat is precies wat hij hoort te doen.

### Gates (eindstand, vanuit de worktree)

    1  check-self-contained        PASS
    2  check-ir-schema             PASS  (schema geldig; 177 bestanden valideren en round-trippen)
    3  check-verifier              PASS  (177 golden schoon; 315 negatief afgewezen voor hun eigen regel)
    4  check-case-schema           PASS  (schema geldig; 429 case-bestanden)
    5  check-oracle                PASS  (407 cases exact herspeeld)
    6  check-clingo                PASS  (407 cases; ref = clingo over het corpus)
    7  check-noop                  PASS  (371 cases weerleggen een niets-doener; 36 gepinde overlevers)
    8  check-rood                  PASS  (22 rode cases; Drools-been 5 fault-pinned / 17 niet-gedragen met reden)
    9  check-catala                PASS  (201 checks; 12 catala-goldens byte-identiek)
    10 check-regelspraak           PASS  (171 tests; 30 rgs-goldens byte-identiek)
    11 check-toka-parity           PASS  (ref 29 / clingo 29 / Drools 29)
    12 check-drools                PASS  (109 played; 1 waardedivergentie met reden; 2 zonder case; JUnit 199)
    13 check-reftype-derivation    PASS  (68 twists over 14 goldens; 64 replays; 0 gepinde overlevers)

Niets buiten de "may touch" veranderd: `spec/verifier/**`, `spec/IR-SPEC.md`,
`corpus/negatief/**`, `backlog/`. `spec/CONSTRUCTS.md` en `spec/PLAN.md`
onaangeroerd; geen bestaand testbestand en geen golden gewijzigd. De branch
blijft onverwerkt staan als voorstel — `spec/IR-SPEC.md` en `spec/verifier/`
zijn beschermde paden.

---

## Fixronde-appendix (2026-09-06, zelfde branch `card/wf034-zesde-golf`)

Twee adversariële lenzen zijn over het Result hierboven gegaan. Het
oorspronkelijke Result blijft staan zoals het geland is; wat hieronder staat
corrigeert en vult aan. **De eerste vondst was blokkerend en is uitgevoerd.**

### 1. De verstrakking: de zijtabelsleutel is een gedragen identiteit

Bijvangst-punt 4 hierboven ("Ci-identiteit, geen byte-exactheid") **is
ingetrokken en vervangen.** Het argument dat er stond — "de acht armen
gebruiken de naam-identiteit van ch. 2.4, want dat is hoe elke lezer de facet
opzoekt" — is onwaar, en gemeten onwaar.

**De meting (2026-09-06, over de bronbestanden van de vier consumenten).** De
consumenten zijn het *niet* eens over hoe ze deze facetten opzoeken. Verifier
(`find_ci` / `norm`), `ref` en de ASP-vertaler zijn ci; de
bean-model-uitvoerder is dat niet. De Drools-backend bereikt deze maps met
rauwe `Map.get` / `Map.containsKey` op **108** plekken over **vier** van de
acht tabellen — `attribuutGranulariteit` 64, `kenmerkGranulariteit` 20,
`attribuutTijdlijnAantal` 17, `kenmerkTijdlijnAantal` 7 — tegenover **vijf**
hoofdletterongevoelige lookups via zijn eigen `Namen.bevatNaam`. De overige
vier tabellen (`attribuutMeervoud`, `attribuutDimensies`,
`attribuutTijdlijnNaam`, `kenmerkSoorten`) worden door de backend alleen
gelaad en nooit teruggelezen; `attribuutTijdlijnNaam` wordt wél
hoofdlettergevoelig gelezen door de RegelSpraak-**frontend**, op twee plekken,
maar dat is de emitter op zijn eigen model en geen wire-consument. *(De
lensmeting sprak van 110 plekken over vijf tabellen met `attribuutTijdlijnNaam`
2; die twee zitten in de frontend, niet in de backend. De getallen hierboven
zijn zelf gemeten en zijn de getallen die in de spec, de code en de NOTE's
staan.)*

**Het gevolg, per consument gemeten** op `corpus/golden/synth2-clip-over-prorata`
met de `kenmerkGranulariteit`-sleutel `borgplichtig` → `Borgplichtig` (één
teken):

    verifier (branchkop vóór de verstrakking)  PASS
    ref        case1                            PASS (3 expectations)
    clingo     case1                            PASS (3 expectations)
    Drools     case1                            PLAYED -> EXCLUDE
                                                (transpiler SKIP: temporele
                                                 toekenning buiten Phase-F scope)

Gate 12 zakt daarmee van `played 109` naar `played 108` en meldt **PASS**: zijn
enige speelvloer is `played > 0` (`scripts/check-drools.sh:197`). Geen gate
wordt rood. De golden valt uit de differentiaal en niets merkt het — dus moet
de wire het vangen.

**De houdbare grond is de dracht-regel, niet de matching-regel.** Ch. 2.1 draagt
er twee: "Declared names are carried verbatim (whitespace-collapsed, case
preserved)" én "Name matching is case-insensitive". Een zijtabelsleutel is geen
matching-plek. Hij is de **v0-denormalisatie** van een facet dat v1 op de
lidknoop zelf draagt (ch. 2.2 / `CONSTRUCTS.md` rij 11: "each v0 map entry
folds into the corresponding member node"), dus de sleutel *is* de identiteit
van dat lid, naast de declaratie geschreven in plaats van erop — en een
identiteit wordt verbatim gedragen. Huisprecedent:
`corpus/negatief/wf091-attribuutref-eenheid-kast` ("Name RESOLUTION is
case-insensitive; copy FIDELITY is a different question") en de golf-5-passage
over de byte-exacte rolnaam in ch. 4.6 (m).

**Kosten: nul, gemeten over de vier zones.** Van de **943** sleutels die de
vier zones ná golf 6 en vóór deze ronde droegen wijkt er **nul** in byte af
terwijl hij ci resolveert; vóór golf 6 evenzo nul van **924** (24 + 9 + 74 + 2
+ 2 + 788 + 24 + 1 — de spec-uitsplitsing, onafhankelijk nagerekend). De volle
523-documenten-sweep vóór/na de verstrakking verplaatst **precies één** regel:
de nieuwe getuige.

### 2. Rood-eerst en de mutanten

**Het nieuwe negatief** `corpus/negatief/wf034-zijtabel-kapitalisatiewees`
(drager `synth2-clip-over-prorata`, `kenmerkGranulariteit`-sleutel
`borgplichtig` → `Borgplichtig`, één-regel- en één-teken-diff):

    verifier branchkop  bfa76f6   md5 a22b5bda655aaf4727b6b3805d6e2053   PASS
    verifier na de verstrakking   md5 924f0e3cfb69e9e8d9be7aa916a9bfe7   WF-034 FAIL
      WF-034 FAIL /objecttypen/Jutter/kenmerkGranulariteit/Borgplichtig:
      kenmerkGranulariteit key 'Borgplichtig' names no declared characteristic
      of 'Jutter': the side table resolves against no declaration

Negatiefzone 315 → **316**, vier zones 522 → **523**.

**De ci-terugdraai-mutant** (alleen de `kenmerkGranulariteit`-aanroepplek
krijgt `has_kenmerk ot` terug; verse `git archive` in `/tmp`, harde build-faal,
`test -x`, md5 in de keten):

    md5 19b4aac4b6503d5f63f1badbd42ab28b
    523 documenten vergeleken -> 1 oordeel beweegt:
      wf034-zijtabel-kapitalisatiewees  FAIL -> PASS
    de overige 522 rapporten byte-identiek

Dat is waarom de predicaten **per tabel** worden doorgegeven: een ci-terugval
op één tabel is op één tabel meetbaar.

**De acht arm-weg-mutanten, herbewezen op de nieuwe code** (zelfde regime,
zelfde vorm; `orphan otn "<tabel>" …` → `[]`, 523 documenten vergeleken):

    arm weggehaald            binary md5                          bewogen oordelen
    attribuutMeervoud         c7c7786168d3fe4d31d083a1579d0433    1  (eigen getuige)
    attribuutDimensies        61869e7b7bf67091cc02afe925f5485a    1  (eigen getuige)
    attribuutGranulariteit    513707535f5817f5004f5cef5d1fbc83    1  (eigen getuige)
    attribuutTijdlijnAantal   7daffcc6adf631c99d2c9e3cbd9b66bd    1  (eigen getuige)
    attribuutTijdlijnNaam     b4d9d745bf9ff6e66b188447a120cd49    1  (eigen getuige)
    kenmerkSoorten            af3c04e7db4f19e9c3b9f21005aa252f    1  (eigen getuige)
    kenmerkGranulariteit      c31809dec803995148b4706e748641b2    2  (zie hieronder)
    kenmerkTijdlijnAantal     61770bf0c7c614f7ce74bfc411e11d38    1  (eigen getuige)

De `kenmerkGranulariteit`-arm draagt sinds deze ronde **twee** getuigen — de
wees die niets noemt en de sleutel die het juiste kenmerk verkeerd spelt — en
zijn arm-weg-mutant bevrijdt ze allebei en verder niets. Ze scheiden onder de
smallere mutant hierboven. Beide NOTE's zeggen dat nu met zoveel woorden; de
oude formulering "this case, alone in all four zones" was in de
`wees-kenmerkgranulariteit`-NOTE onwaar geworden en is gecorrigeerd.

**De vier ongewijzigde armen** — `Kenmerk.eigenaar`, `Eenheid.stelsel`,
`objectCreaties[].subject: null`, `anderTeFiller: null` — zijn niet geraakt
door deze ronde en zijn **niet opnieuw gedraaid**. Hun bewijs uit het Result
hierboven staat onverkort; hun NOTE's noemen daarom nog 522 documenten, wat de
sweepgrootte was waarop dat bewijs is gedraaid.

### 3. Tellingcorrecties (met de oorzaak, zodat het instrument niet terugkomt)

De eenheidsrecord-tellingen in het Result hierboven (r. 178, r. 259) en in de
spec waren **te hoog**: het telinstrument daalde in `noemer` af en telde de
denominator-eenheid van een samengesteld tarief **twee keer** — één keer op
eigen titel en één keer onder het omvattende record. Er zijn 73 zulke noemers,
en 2369 + 73 = 2442, 2236 + 73 = 2309.

    r. 178  "2442 eenheidsrecords … 2309 een stelsel"   -> pre-kaart 2369 / 2236
    r. 259  "2309 stelseldragende eenheden"             -> 2236

De **post-kaart-stand**, gemeten 2026-09-06 over de 523 documenten van de vier
zones, is **2377** eenheidsrecords waarvan **2244** een `stelsel` dragen (de
overige 133 dragen `null`). Dat zijn de getallen die nu in
`spec/IR-SPEC.md` ch. 4.6 (r), in de WF-034-indexrij en in
`corpus/negatief/wf034-eenheid-onbekend-stelsel/NOTE.md` staan, met de oorzaak
in één bijzin erbij. *Wie unit-records telt moet precies één keer in `noemer`
afdalen.*

### 4. MF3 — `attribuutMeervoud` is de tweede lezerloze tabel

De lezerskolom zei "`attribuutMeervoud` | WF-057". WF-057 loopt die tabel wel
af, maar leest de **waarde** (de tekstveiligheid van het meervoud) en gooit de
sleutel weg (`fun (_, m) -> …`, `checks.ml`). De **sleutel** heeft geen enkele
wire-consument: de Drools-lader zet hem in een map die niemand terugleest, en
de enige lezer van de sleutel is de emitterkant (de meervoudsmatcher van de
frontend, op zijn eigen model). `attribuutMeervoud` staat daarom nu naast
`kenmerkSoorten` in de wire-argument-zin, in de spec en in de code.

Tegelijk gepreciseerd: de **sleutel** van `attribuutTijdlijnNaam` wordt wél
gelezen, als **kruistabel-join** in WF-005, die hem meeneemt naar
`attribuutGranulariteit` en `attribuutTijdlijnAantal` om de overgenomen
facetten met de declaratie te vergelijken. WF-003 leest van die tabel alleen de
waarde.

### 5. K5 — `ch. 2.4` waar `ch. 2.1` bedoeld is

Ch. 2.4 is *Datatype*; de naam-identiteit staat in ch. 2.1, *Names and
identity*. Gecorrigeerd op de plekken die de lens noemde:
`spec/IR-SPEC.md` r. 2943 (de lege-naam-identiteit van de verdelingsvelden),
r. 3069 (de byte-exacte rolnaam) en de (p)-passage r. 3179, en `checks.ml`
r. 977 (die alinea is in zijn geheel herschreven). In dit kaartbestand staat
dezelfde mis-citatie op r. 116 en r. 242; het oorspronkelijke Result wordt niet
herschreven, dus zij worden hier genoemd in plaats van stilzwijgend gewijzigd —
lees daar ch. 2.1.

Bij r. 3069 is meteen een claim rechtgezet die door deze ronde onwaar werd: dat
was "the one place the case-insensitive identity is not enough". Het zijn er nu
twee, op verschillende grond — daar een naam die als property moet overleven,
in (p) een sleutel die een gedragen identiteit is.

Nog gevonden en **niet** aangeraakt, buiten de opdracht: `checks.ml` r. 1062 en
r. 6218 en twee plekken in de WF-034-indexrij dragen dezelfde mis-citatie. Ter
beoordeling van de maintainer.

### 6. Het divergentie-voorbehoud

De ankerdivergentie uit het Result (`kenmerkGranulariteit`,
`anchor/jutterswet-dubbelwacht` case3: `ref` faalt op `gedurende` met WF-055
terwijl clingo de case schoon speelt) is **2026-09-06 opnieuw gereproduceerd**,
met een voorbehoud dat er niet stond: het dragende document is *onafhankelijk
van deze clausule* ill-formed, en was dat al vóór de kaart. Dezelfde verifier
die de hernoemde sleutel afwijst, wijst het document ook af op **WF-055**
(`gedurende over 'aangesteld', which is not a temporal characteristic of
'Jutter'`) — precies de reden dat het hernoem-instrument op die tabel 0 van 9
scoort. De divergentie zit dus achter een al-dichte deur: tweede
verdedigingslinie, geen rechtvaardiging van de arm. Het voorbehoud staat nu in
ch. 4.6 (p), in de indexrij en in de NOTE.

De reproductie zelf, alle vier cases van het anker met de sleutel hernoemd:

    case1-overlap    ref CASE ERROR   =  clingo CASE ERROR
    case2-disjunct   ref CASE ERROR   =  clingo CASE ERROR
    case4-gespleten  ref CASE ERROR   =  clingo CASE ERROR
    case3-een-leeg   ref EVALUATION FAULT (WF-055)  vs  clingo PASS (2 expectations)

### 7. Twee kaartvoorstellen

- `backlog/new/20260906-clingo-gedurende-spiegelpoort.md` — de ch. 3.6-clip van
  de ASP-backend mist de temporaliteitspoort die zijn twee zusters wél dragen
  (`translate.ml` r. 1912-1915 tegen r. 4548-4556 en r. 4620-4625, die laatste
  met "mirror ref's fault" in het commentaar). Bij een niet-temporeel kenmerk
  levert de clip-arm nul `ktl`-feiten en een lege doorsnede — som 0, zonder één
  woord — waar `ref` faalt. Op welgevormde documenten onbereikbaar, dus tweede
  verdedigingslinie: het WF-092-eerlijkheidspatroon.
- `backlog/new/20260906-gate-12-speelvloer.md` — elke golden kan vandaag stil
  uit de Drools-differentiaal vallen; de enige vloer is `played > 0`. Voorstel:
  het check-noop-overleverspatroon, een reden-dragende verwachte-speellijst of
  een gepinde teller die faalt wanneer een golden zijn PLAYED-status verliest.
  Raakt `scripts/` — beschermd pad, dus een voorstel.

### 8. De scope-keuze, expliciet: eigenaar en stelsel blijven ci

Byte-exactheid hoort **uitsluitend** op de acht zijtabelsleutels. De twee
naburige armen van deze kaart blijven hoofdletterongevoelig, en dat is een
keuze met een grond en een getuige:

- Het zijn gewone **naamresoluties** — een naam die *naar* een declaratie wijst
  — en niet een identiteit die gedragen wordt. Al hun lezers resolven ci: de
  verifier via `find_ci`, de ASP-vertaler, en de Drools-lader met
  exact-dan-`equalsIgnoreCase` op de objecttypetabel.
- Byte-exact op `stelsel` zou `corpus/negatief/wf091-attribuutref-eenheid-kast`
  breken. Dat document draagt met opzet `stelsel: "GEWICHT"` tegen een
  gedeclareerde `Gewicht` — het is dé getuige van "resolutie ci, kopie byte" —
  en zou er een WF-034-regel bij krijgen, waarmee zijn eigen
  arm-exactheidsclaim onwaar wordt.

Die grens staat nu met zoveel woorden in ch. 4.6 (q) en (r) en in de
codecommentaar.
