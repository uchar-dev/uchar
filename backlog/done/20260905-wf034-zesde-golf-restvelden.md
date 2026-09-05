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
