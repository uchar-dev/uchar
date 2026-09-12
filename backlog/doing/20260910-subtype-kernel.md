# Subtypering in de kernel: IR, verifier en de drie executors

Goal: enkelvoudige objecttype-overerving als eersteklas kernel-begrip, uitgevoerd volgens `spec/proposals/SUBTYPING.md` (de onderzoekskaart is de voorganger en haar besluiten zijn bindend): de optionele supertype-verwijzing op het objecttype in IR-SPEC met WF-regels voor acycliciteit, enkelvoudigheid, keten-naamresolutie, sibling-botsingen, rolcompatibiliteit, concrete-type-instantiatie met discriminator in de invoer, en de overervingssemantiek van opdrachtbesluit 3: per target erft het subtype de defaults-stapel van het supertype, subtype-eigen gewone regels vervangen de geërfde basis, geërfde uitzonderingen blijven erboven, en de subtype-eigen uitzonderingen staan als blok boven de geërfde — bij een botsing over de laaggrens wint de subtype-eigen definitie, deterministisch en zonder conflictfeit (maintainerbesluit 2026-09-12, SUBTYPING §11.2, naar Flora's principe "Method definitions in subclasses override the definitions that appear in the superclasses"); de lowering in alle drie de executors — ref met subtype-check in de evaluator, clingo met `isa_sub/2`-feiten plus een transitieve sluiting in de ASP zelf, Drools volgens de in het proposal besliste vorm — plus de minimale jutterswet-use-case uit opdrachtdeel 1.5 als anchor.

Acceptance: de nieuwe anchor speelt groen door verifier → ref → clingo (→ Drools conform het proposal-besluit) met de vier bewijsmomenten expliciet in de verwachte uitkomsten: een supertype-regel die voor een subtype-instantie vuurt, een supertype-kwantificatie die super- en subtype-instanties samen telt, de pinguïn-toets (een subtype-eigen basis vervangt de geërfde basis terwijl een geërfde uitzondering erboven blijft winnen), en een botsende geërfde-tegen-eigen uitzondering die deterministisch naar de subtype-eigen beslissing gaat — een gewone schrijf, `consistent`, geen conflictfeit; negatieve getuigen voor cyclus, geshadowd attribuut (de kaartfrase "botsend sibling-attribuut" gelezen als SUBTYPING §3.3 geval (a)), onbekend supertype en de neerwaartse lezing (`wf034-neerwaartse-lezing`, de nieuw bereikbare vorm onder de bestaande WF-034) weigeren luid met de gereserveerde respectievelijk bestaande WF-nummers, terwijl de tweede directe ouder per besluit §11.1 een weigering bóven de wire is: WF-106, zonder verifier-check en zonder `corpus/negatief`, met zijn getuigen in de fixtures van de twee frontendkaarten; het volledige bestaande corpus parseert, verifieert en emit byte-identiek; de volledige gate-reeks van de repo is groen.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md` (nieuwe rij), de verifier, `ref/`, de clingo- en Drools-backends, `corpus/` (nieuwe anchor + negatieven), en de eigen kaart.

Out of scope: elke frontend (RegelSpraak-dialect en F-logic hebben eigen kaarten); meervoudige overerving; een vrije specificiteitsrelatie tussen willekeurige regels (het C2-besluit blijft staan); wijzigingen aan de defaults-machinerie anders dan wat de groepserving van besluit 3 vereist.

## Aangescherpt door de onderzoekskaart (2026-09-11), bijgewerkt op de maintainerbesluiten (2026-09-12)

`spec/proposals/SUBTYPING.md` is bindend voor deze kaart; wat hieronder
staat is de uitvoerbare neerslag ervan, niet een tweede besluit. Drie
van de zes open §11-punten zijn op 2026-09-12 beslist en hieronder
verwerkt: het gereserveerde nummer voor de meervoudige-ouderweigering
(§11.1), de vaste laaggrensordening zonder veld (§11.2) en de
ASP-sluiting voor clingo (§11.3). §11.4–§11.6 staan nog open, maar
raken deze kaart niet.

**Wirevorm.** `ObjectType.supertype: string?` — één extensieveld
(`IR-SPEC.md` § Method: optioneel, nooit `required`, nooit null, heel of
niet), wire-sleutel `supertype`. De kernelkaart landt het
emitter-vrij met handgeschreven anchorgetuigen; de dialectkaart maakt
het daarna emitter-dragend, langs de route die `Let`/`BinderRef` namen.

**Laaggrens: vaste blokorde, geen wire-oppervlak** (besluit §11.2). De
effectieve stapel per target is, hoog naar laag: de subtype-eigen
uitzonderingen in gedeclareerde orde, dan de geërfde uitzonderingen in
geërfde orde, dan de basis (de subtype-eigen als die er is, anders de
geërfde), en daaronder de initialisatie- en virtueel-input-default, die
nooit worden vervangen. Er komt géén `geerfdeUitzonderingen`-veld, geen
enum en geen regel die een declaratie eist. Let op de fijnmazigheid,
want hier zit het verschil met F-logic: dit is soortsgewijze override,
geen attribuutbrede annulering — een geërfde uitzondering zonder
toepasselijke eigen tegenhanger blijft gewoon boven de subtype-basis
winnen (rij 4 van de zescellige tabel, 99 en niet 20). Alleen bij
gedeelde toepasselijkheid wint de eigen.

**WF-nummers (gereserveerd vanaf WF-103).** WF-102 (de
bereikpadregel, geland 2026-09-12) is de hoogste regel in de WF-index
op de basis van deze branch. De hoofdsessie maakt de nummers
definitief bij de landing.

- WF-103 — `supertype` benoemt een gedeclareerd objecttype.
  Negatief `corpus/negatief/wf103-onbekend-supertype`.
- WF-104 — de supertypegraaf is acyclisch, zelfverwijzing inbegrepen.
  Negatieven `wf104-supertype-cyclus` en
  `wf104-supertype-zelfverwijzing`. Model voor de implementatie:
  WF-085's kleuren-DFS met benoemd cyclus-pad,
  `spec/verifier/lib/checks.ml:2859–2915`.
- WF-105 — geen attribuut- of kenmerknaam twee keer op één keten
  (ci; WF-002 opgetild naar de keten). Negatief
  `wf105-hershadowd-attribuut`.
- WF-106 — **herbestemd op 2026-09-12** (SUBTYPING §11.1 en §10): het
  nummer droeg in de vorige revisie de laaggrensordening; besluit
  §11.2 hief die regel op en het vrijgekomen nummer benoemt nu de
  weigering die het hele ontwerp draagt — *een objecttype declareert
  hoogstens één direct supertype*. Struikel niet over het nummer: er
  bestaat geen `wf106-ongeordende-laaggrens` meer. Deze regel krijgt
  **geen verifier-check en geen negatief**, en dat is bewust: onder
  `supertype: string?` is een tweede ouder geen document dat de
  verifier afkeurt maar een document dat niet te schrijven valt (gate
  2 weigert de arrayvorm op de gesloten node). De regel begrenst de
  taal bóven de wire, dus haar getuigen wonen waar die taal gelezen
  wordt: de fixtures van de dialectkaart (`is een A en B`, twee
  `is een`-clausules) en van de F-logic-kaart (twee `::`-feiten, die
  écht twee ouders kunnen schrijven). Die getuigen vallen buiten deze
  kaart — beide frontends staan in Out of scope — dus wat deze kaart
  levert is het nummer, de normatieve zin en de melding van het
  schema; de spec-disciplineafwijking is beargumenteerd in SUBTYPING
  §3.1 en hoeft hier niet opnieuw afgewogen te worden.
- WF-107 — in reserve, zonder benoemde gegadigde. De gegadigde die
  hier eerst stond, de markeringsplicht uit SUBTYPING §4.1 regel 3,
  verviel met de regel zelf toen besluit §11.2 de laaggrens vast
  ordende: er valt niets meer te markeren en er staat geen
  ongehandhaafde SHOULD meer open. Het nummer blijft gereserveerd voor
  wat de eigen lensronde van deze kaart oplevert.

De meeste andere raakvlakken zijn een **aanpassing van bestaande
regels** onder de ketenbewuste lezing van "resolveert", zonder nieuw
nummer: WF-034 (ledenkruising over de ketengesloten verzameling; de
neerwaartse lezing is de nieuwe bereikbare weigering, negatief
`wf034-neerwaartse-lezing`), WF-047 ("overeenkomen" verbreedt tot
⊑-compatibiliteit) en WF-048 (uniciteit over de ketengesloten
typeparen). WF-026/WF-050/WF-079 zijn de uitzondering op die
formulering: daar komt een nieuwe **beperking** bij, geen
herformulering — zie hieronder.

**De zin die geland moet worden** (`IR-SPEC.md` ch. 6.1 en
`RUNTIME-SEMANTICS.md` ch. 7): de enkelschrijver-hekken worden gelezen
op de **effectieve stapel per type**, ná overervingsresolutie, nooit op
de ruwe declaratieverzameling en nooit op een ketengesloten doelsleutel.

Let op de richting van het risico, want die is contra-intuïtief en
gemeten. Vandaag vuren de hekken **niet** op een supertype/subtype-paar:
WF-026 vergelijkt gedeclareerd type én attribuut paarsgewijs
(`checks.ml:1295–1298`), WF-079 vergelijkt `ot1 = ot2 && a1 = a2`
(`:7581`, met de defaults-lookup op `:7558`), en WF-050 bouwt zijn
sleutel als `"A:" ^ norm l.pd_onderwerp ^ ":" ^ norm tn`
(`:7392–7394`, met de drie lifts op `:7423`, `:7442`, `:7469`).
`Strandrecht.uitkering` en `Wrakrecht.uitkering` zijn dus twee
sleutels, en de anchor kan er niet over struikelen. Het echte gevaar is
de naïeve reparatie: wie de doelsleutel optilt naar de ketengesloten
verzameling laat de hekken juist vuren op precies de vorm die besluit 3
legitimeert. Dit is daarmee een **nieuwe beperking op de verifier**,
geen herformulering.

**Loweringsbesluiten, nu beslist.**

- **Drools: native klassehiërarchie.** De gegenereerde fact-bean van
  een subtype krijgt `extends` en `FactModelGenerator.leden` emit
  alleen de eigen leden. Geen enkele patroonplek of accumulate
  verandert: Drools matcht klassepatronen via `instanceof`, en het
  getypte accessorpad draagt de subtypewacht `owner.isInstance(o)` al,
  vooruitlopend op dit geval (`runtime/Accessors.java:117–120`,
  setterarm `:144`). Het werk zit in **drie bestanden**: de
  beangenerator, de IR-lader, en de **contractvalidator** — die loopt
  met zijn lookup wél door de hiërarchie (`c.getMethods()`,
  `ContractValidator.java:90–94`), maar bouwt zijn verwachtingen uit
  `FactModelGenerator.leden(model, ot)` (`:34`), dus hij beweegt mee
  zodra `leden` eigen-leden-alleen wordt. Vlakke emissie met
  discriminator is verworpen: 65 `declarationOf`-plekken plus elke
  accumulate zouden een transpile-time transitieve sluiting nodig
  hebben, en de getypte to-one referentievelden zouden verbreed moeten
  worden.
- **clingo: de sluiting gaat in de ASP — beslist op 2026-09-12**
  (SUBTYPING §11.3). `isa_sub/2`-feiten plus een transitieve regel,
  met de **49 `is_a(`-joinregels / 50 voorkomens** daardoorheen; van de
  52 regels / 53 voorkomens zijn er 2 commentaar op `:1256` en `:3818`
  en 1 de feitemitter op `:6457`. De grond is dat de sluiting dan in
  het model zit dat de solver ziet, zodat het clingo-been een
  onafhankelijke herimplementatie blijft in plaats van een tweede kopie
  van de OCaml-ketenwandeling. Het inspanningsargument wees de andere
  kant op en is overruled; een OCaml-zijdige typeset-expansie is
  daarmee geen optie meer.
- **ref: elf gemeten plekken, niet één.** `World.of_type`
  (`ref/lib/world.ml:52–57`) beslist de kwantificatie, maar de anchor
  **laadt niet eens** zonder de rest: `case.ml:277–281` (de
  instantielader zoekt een aangeleverd attribuut alleen op het eigen
  type — `case1` levert `grondslag` op een `Wrakrecht`),
  `case.ml:554/569/576/600` (de verwachtingslader weigert een niet op
  het eigen type gedeclareerd attribuut of kenmerk), `case.ml:535`
  (rolvuller: letterlijke typegelijkheid — hier leeft de
  bovengrensregel), `eval.ml:480` (navigatie-eindpunt) en
  `eval.ml:2508` met de naamwachten op `:627`, `:803`, `:1108`,
  `:1159`.

**Anchor en getuigen.**
`corpus/golden/anchor/jutterswet-strandrechtketen` — `Strandrecht` →
`Wrakrecht` plus een aggregerende `Kustgemeente`, met de zescellige
tabel van SUBTYPING §7.2 als absolute verwachting. **Vier** cases, want
met drie blijft rij 5 ongedekt:

- `case1-basis.json` (cellen 1 en 3): 10 / 20; registratiekosten 5 op
  béíde; uitkeringstotaal 30, registertal 2 — een niet-extensionele
  lezing geeft 10 en 1 en valt hier luid om;
- `case2-geerfde-uitzondering.json` (cellen 2 en 4): 99 / 99,
  uitkeringstotaal 198;
- `case3-eigen-uitzondering.json` (cel 5): één `Wrakrecht`, niet
  betwist, bergingsdiepte 25 → 55. Dit is de cel die een lowering
  betrapt die de eigen uitzondering van het subtype stil laat vallen;
  met alleen case 1 en 2 komt die er ongezien doorheen;
- `case4-laaggrensvoorrang.json` (cel 6): één `Wrakrecht` dat betwist
  ís én bergingsdiepte 25 heeft, dus met de geërfde en de eigen
  uitzondering tegelijk toepasselijk. Verwacht: uitkering **55**,
  registratiekosten **5**, uitkeringstotaal **55**, registertal **1**,
  `consistent` — een gewone schrijf. De gepinde waarde scheidt de drie
  lezingen: kind-wint schrijft 55, ouder-wint zou 99 schrijven, en de
  ingetrokken ongeordende lezing zou niets schrijven en een
  §9.5-conflictfeit deponeren. Case 3 en case 4 verwachten dezelfde
  waarden en verschillen alleen in `betwist`; dat is het punt — case 3
  toont de eigen uitzondering alleen, case 4 toont haar tegen een
  levende tegenstander.

De sleutelvorm van een §9.5-conflictfeit — `Strandrecht.uitkering`,
het type dat het **attribuut** declareert, niet het concrete type, en
op de wortel in plaats van padgesleuteld — verdwijnt met cel 6 niet
uit de kaart, maar heeft er geen getuige meer: het restgeval is een
ch.-7.2-botsing bínnen een geërfd intern-ongeordend blok
(SUBTYPING §4.4). Zoek er een eigen case bij of meld expliciet dat de
sleutelvorm ongepind blijft.

Datatypes in de bron volgen de corpusspelling `Numeriek (…)` (137
voorkomens; `Bedrag` en `Getal` komen nergens voor), en de twee
Kustgemeente-attributen heten `uitkeringstotaal` en `registertal` —
níet "aantal strandrechten", want een attribuut dat de rolnaam deelt
zet de anchor boven op de ch. 2.6-naamdelingsvorm die de Drools-bean
niet kan dragen.

**De tweede directe ouder: beslist, en de acceptatietekst is erop
bijgewerkt.** SUBTYPING §11 punt 1 is op 2026-09-12 beantwoord. De
enkele string staat vast — `supertypes: [string]` plus een "hoogstens
één"-regel is verworpen, want dat koopt een `corpus/negatief`-getuige
met een wirevorm die kan uitdrukken wat het ontwerp verbiedt. De
weigering krijgt wél een nummer: **WF-106**, herbestemd van de
opgeheven laaggrensregel. Er is daarmee één citeerbare naam over de
drie lagen heen — de arrayweigering van het schema, de dialectmelding
en de F-logic-melding zijn zichtbaar dezelfde weigering — en geen
verifier-check, want er valt niets te checken. De twee CGM-gevallen
`RechtOpUitkeringWazo` en `RechtUitkeringZw` blijven bij naam in de
meldingen van de twee frontends staan; daar wonen ook de getuigen.

## Result (2026-09-12)

### Wat er staat

**De wire.** `ObjectType.supertype: string?` als optioneel extensieveld op de
gesloten `Objecttype`-node (`spec/schema/regelspraak-ir-v0.schema.json`),
`ot_supertype` in de verifier-AST, gelezen door de OCaml-lader en door
`IrLoader` van de Drools-backend. Enkelvoudigheid door vorm; het schema
draagt de weigering van de arrayvorm en die weigering staat in de
velddescriptie.

**De verifier.** De hefboom is dat er precies één plek is waar een objecttype
uit het document komt: `Checks.objecttype ctx name` geeft nu de
ketengesloten weergave terug, dichtstbij eerst. Daarmee werd elk lidkruis
ketenbewust zonder de 35 kruisplekken aan te raken. Drie regels lezen
bewust de rauwe declaratie — WF-002, WF-105 en de zijtabelsleutels van
WF-034 — want die gaan over wat een type zélf declareert.

- **WF-103/104/105** nieuw, met check en negatief. WF-104 is WF-085's
  kleuren-DFS met benoemd cykelpad.
- **WF-106** landt als normatieve zin in ch. 2.2 met de beargumenteerde
  afwijking van de spec-discipline erbij: geen check, geen negatief, want
  onder één string is de overtreding niet te schrijven; het nummer koopt
  één citeerbare naam over drie lagen. Getuigen komen in de twee
  frontendkaarten.
- **Ketenbewust zonder nieuw nummer:** WF-034 (het lidkruis over de
  ketengesloten verzameling; de neerwaartse lezing is de nieuwe bereikbare
  weigering), WF-047 ("overeenkomen" → ⊑-compatibiliteit) en WF-048 plus
  de navigatiehelpers (`connecting_candidates`,
  `connecting_role_candidates`, `nav_roles_by_name`, `nav_role_by_pair`)
  over de ketengesloten typeparen.
- **De enkelschrijver-zin** staat in IR-SPEC ch. 6.1 én als commentaar bij
  WF-026, WF-050 en WF-079 in `checks.ml`, op de plek waar de naïeve
  optilling geschreven zou worden.

**De drie uitvoerders.** ref en clingo delen één definitie van de effectieve
stapel (`Ir_v0.Desugar.effective_defaults`, naast de ch.-7.7-ontsuikering);
Drools draagt de Java-tweeling ervan met dezelfde blokorde. Per concreet
type één stapel, dus de groep draait op instanties van precies dat type.

- **ref:** de elf gemeten plekken, plus `World.of_type` dat de
  lidmaatschapspredikaat van de aanroeper krijgt (de keten zit in het
  document, niet in de feitenbasis), plus `owner_hosts`, de
  navigatie-eindpuntcheck en de vier naamwachten.
- **clingo:** `is_a/2` staat nu voor het concrete type en `isa/2` voor de
  sluiting daarover; alle 48 joinplekken lezen `isa/2`, de twee producenten
  en de defaults-stapel lezen `is_a/2`. De transitieve regel en de
  `isa_sub/2`-feiten staan in het programma dat de solver ziet. **Afwijking
  van de letterlijke besluittekst, benoemd:** het proposal schrijft
  `isa_sub/2` + transitieve regel met de joinregels "via de sluiting
  gerouteerd"; ik heb de sluiting in een nieuw predicaat `isa/2` gezet in
  plaats van in `is_a/2` zelf, omdat de defaults-stapel juist het concrete
  type nodig heeft. Inhoudelijk hetzelfde besluit — de sluiting zit in het
  ASP-model — met één predicaat meer.
- **Drools:** `extends` op de gegenereerde bean, `leden()` alleen eigen
  leden (met aftrek van de geërfde, want de afgeleide referentie-armen
  leiden hetzelfde rolveld nog eens af), en de reduce-regel van een
  defaults-stapel bindt zijn patroon aan precies één klasse zodra het model
  een hiërarchie heeft. `heeftReferentieVeld` en `rolHeeftObjecttype` lopen
  door de keten. Patroonplekken en accumulates ongemoeid.

**De anchor.** `corpus/golden/anchor/jutterswet-strandrechtketen` met vier
cases; alle zes cellen van SUBTYPING §7.2 exact zoals gepind, en alle vier
bewijsmomenten spelen door verifier → ref → clingo → Drools.

**Twee extra goldens.**

`corpus/golden/jutterswet-wrakconflict` pint de sleutelvorm van een
§9.5-conflictfeit onder overerving — de vorm die de anchor kwijtraakte toen
cel 6 ging schrijven: een botsing binnen een geërfd intern-ongeordend blok,
gesleuteld op `Strandrecht.uitkering`, het type dat het attribuut
declareert. De case zet beide instantiesoorten naast elkaar: het Wrakrecht
scheidt declarant van concreet type, het vlakke Strandrecht ernaast laat
zien dat de sleutel niet van de instantiesoort afhángt. Speelt door alle
drie de uitvoerders.

`corpus/golden/jutterswet-zustervondst` pint de **zustertoelating** van
SUBTYPING §3.3(b): twee onvergelijkbare subtypes van één supertype
declareren allebei `toeslag`, met verschillende datatypes (2 decimalen
tegen geheel). Dat maakt hem een discriminator en geen illustratie: een
lezer die de twee ketens kruist schrijft 12,50 in een geheelgetalveld en
loopt tegen de schrijfgrens aan in plaats van stil verkeerd te antwoorden.
Daarnaast vuurt de supertyperegel `vergoeding` voor alle drie de
instantiesoorten. Speelt door alle drie de uitvoerders.

**Vijf negatief-directories,** elk met NOTE.md: `wf103-onbekend-supertype`,
`wf104-supertype-cyclus`, `wf104-supertype-zelfverwijzing`,
`wf105-hershadowd-attribuut`, `wf034-neerwaartse-lezing`.

**Spec.** IR-SPEC ch. 2.2 (veld, opwaartse resolutie, extensionele
kwantificatie, bovengrenslezing, concrete-type-instantiatie, de expliciete
niet-weigeringen, WF-103..106), ch. 6.1 (de enkelschrijver-zin met de
gemeten richting van het risico), vier WF-indexrijen.
RUNTIME-SEMANTICS ch. 7.10 met de zescellige tabel als absolute uitkomst en
de twee extensionele uitkomsten eronder; die voorbeelden staan verbatim in
`ref/test/test_ref.ml` (WV-7.10-a..d en WV-2.2-a/b/c, 30 nieuwe
voorbeelden; de suite gaat van 553 naar 583).

Ch. 7.10 draagt ook de **onbereikbaarheidszin** bij de conflictsleutel: een
botsing in een geërfd blok kan géén botsing zijn die het *subtype*
toepasselijk maakt. De condities van een op de voorouder gedeclareerde
groep resolveren op de voorouder en resolutie loopt alleen omhoog, dus een
lid of rol die alleen het subtype declareert bestaat daar niet (WF-034).
Wat overerving hier verandert is de sleutel, niet de bereikbaarheid.

### Rood-eerst

1. **De vijf negatiefs** vóór de wire: gate 3 rood met
   `LOAD ERROR at /objecttypen/Wrakrecht: unknown field 'supertype'` op alle
   vijf — precies de melding die de volgende commit door de geclaimde
   regelnaam moest vervangen (commit `4844297`, fix in `6919f02`).
2. **De anchor** vóór de uitvoerders: gate 5 rood met
   `CASE ERROR: attribute "grondslag" is not declared on "Wrakrecht"` op
   alle vier de cases, terwijl de verifier het document al goedkeurde
   (commit `c808212`, fix in `b3c1c8f`).
3. **Tussenstap binnen de ref-fix,** genoteerd omdat hij het ontwerp
   bevestigt: met de lader ketenbewust maar zónder de stapelsamenstelling
   gaven alle vier de cases `EVALUATION FAULT: no fixpoint within 100
   passes` — de Strandrecht-groep en de Wrakrecht-groep schreven om beurten
   over één Wrakrecht. Datzelfde rood kwam bij Drools terug (`geen fixpunt
   (kringverwijzing?)`) tot de klassenklem erin zat.
4. **Gate 12** vóór de Drools-fix: `ERROR NullPointerException ... "naar" is
   null` op de anchor.

### Gate-slotregels (alle dertien groen)

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 203 file(s) validate and round-trip, floor 192)
check-verifier: PASS (203 golden clean; 378 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 479 case file(s) validate)
check-oracle: PASS (ref suite 591 worked examples, floor 489; 453 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 125 checks, floor 83; 453 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (417 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (207 tests, floor 173); 42 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS
  re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45; TOKA suite 27 tests, floor 27
  differential replay: 29 toka cases (floor 29) through ref, clingo, Drools, ZERO silent divergences
check-drools: PASS
  played 128  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
  auto-excluded 46; channel floor 49 non-PLAYED goldens pinned by name; played matches the pinned 128
  JUnit 328 tests, floor 239  (the backend's own suite, green with a non-zero count)
check-reftype-derivation: PASS (92 unit annotations twisted over 17 goldens, floor 74; 80 executor replays reproduce the untouched outcome, 14 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
```

Gate 13's boekhoudklem meldt `0 outside the twist table`: `supertype` draagt
geen eenheid en is nergens een eenheiddragende annotatie op een nieuwe plek
geworden. Gate 7: alle vier de anchorcases, beide wrakconflict-cases, de
zustercase en beide wrakbasis-cases zijn no-op-rood; geen enkele hoefde in
de survivor-lijst.

### Beschermde paden die deze kaart aanraakt

- `scripts/check-drools.sh`: **`EXPECTED_PLAYED` 124 → 128**, met reden en
  datum in het script, in vier stappen die apart te wegen zijn. +1 voor de
  anchor (de vier bewijsmomenten door alle drie de uitvoerders zijn de
  acceptatie van deze kaart; de kaart stond deze expliciet toe). +1 voor
  `jutterswet-wrakconflict` (de conflictsleutelvorm heeft drie lezers en
  geen andere getuige). +1 voor `jutterswet-zustervondst` (de
  zustertoelating van SUBTYPING §3.3b, die "a golden pins the admission"
  eist en die niets anders pint). +1 voor `jutterswet-wrakbasis` (de
  kale-regeltak van §4.1 regel 1, in de derde ronde van weigering naar
  norm gegaan — zonder golden zou die norm door geen enkele gate lopen).
- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
  `spec/verifier/`, `corpus/golden/anchor/` — binnen de may-touch van de
  kaart, maar alle beschermd: dit blijft een voorstel tot de maintainer
  merget.

### Buiten de may-touch, benoemd

- `frontends/catala/lib/translate.ml`: **één regel** (`ot_supertype = None`).
  Zonder die regel bouwt de OCaml niet meer — het is een recordliteraal van
  het gewijzigde AST-type.
- `ref/test/test_ref.ml`: dezelfde eenregelige compileerfix, plus dertig
  voorbeelden die de kaart en de spec-discipline eisen (WV-7.10-a..d,
  WV-2.2-a/b/c) en een `expect_fault_msg`-helper: `expect_fault` pinde
  alleen dát er een fout kwam, niet welke — precies het gat waardoor de
  stille Drools-hoek door de suite glipte.
- `frontends/regelspraak/src/main/java/.../model/RegelSpraakModel.java`: het
  `Objecttype`-veld `supertype`, `keten()`, `isEen()`, `declarantVan()` en
  de ketenbewuste lezing van `attribuut`, `dimensiesVan`, `heeftLid`,
  `kenmerkSoort` en `rolHeeftObjecttype`. Dit bestand staat fysiek in de
  frontend maar is **de enige kopie** en wordt door `backends/drools/pom.xml`
  in-tree meegecompileerd; de Drools-backend kan er niet omheen. De
  wijziging is additief en identiek van gedrag op een vlak model (elk type
  is dan zijn eigen enige voorouder) — gate 10 blijft groen op 207 tests en
  42 byte-identieke heruitgiftes. De frontend zelf emit `supertype` niet:
  dat is de dialectkaart.

### Wat ik NIET heb gehaald, en de conservatieve lezingen

- **Twee vormen worden nog geweigerd; twee andere zijn in de derde ronde
  juist gelowerd.** Wat weigert, weigert luid, met dezelfde benoemde melding
  in alle drie de uitvoerders, vóór er een waarde bestaat:

  - **één eigen groep over een geërfde gedeclareerd-ongeordende set** —
    §4.1 regel 3 definieert hem, maar hij is niet verlaagd. De reden is een
    eigenschap van de wire en staat in ch. 7.10: die lezing is drielaags
    terwijl de leden van een groep als gelijken verzoenen, en het enige
    generieke mechanisme dat wél terugvalt (de group-default van ch. 7.9)
    verplaatst de conflictsleutel weg van de wortelvorm die §4.4 pint. Dit
    is de terugvalregel toegepast, met de gemeten reden;
  - **een gedeclareerd-ongeordende set op het eigen niveau** tegen een
    geërfde stapel — werkelijk ongedefinieerd: regel 3 gaat over een
    ongeordende OUDER-groep, en voor deze vorm geeft het proposal geen
    lezing.

  Beide gepind in `ref/test/` (WV-7.10-c) en in de eigen suite van de
  Drools-backend, elk op meldingsfragment. Geen corpuscase kan ze dragen:
  een golden moet afspelen en ref fault op allebei.

  Wat er niet meer bij staat: een gewone regel tegen een geërfde stapel, in
  beide richtingen. Die is nu norm en werkt — zie de derde ronde.
- **De facetzijtabellen van de Drools-backend blijven per gedeclareerd type
  gelezen** op zo'n veertig plekken in beide transpilertakken. Een keten met
  een temporeel of gedimensioneerd lid weigert de IR-lader daarom bij naam.
  Zonder die weigering zou een geërfd tijdlijnattribuut op een subtype als
  scalair lageren — stil fout in plaats van luid. In OCaml speelt dit niet:
  daar zijn de facetkaarten deel van de effectieve weergave.
- **WF-107 blijft ongeclaimd.** De twee resterende weigeringen zijn geen
  documentfouten — de documenten zijn legitiem volgens het proposal — maar
  vormen die geen uitvoerder verlaagt, dus een WF-nummer zou de spec
  tegenspreken. Ze horen thuis als luide runtime-weigering, en dat zijn ze.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md` zijn niet aangeraakt.** Het
  voorgestelde rijmateriaal staat hieronder.

### Tweede ronde, na de adversariële lens (2026-09-12)

De lens draaide alle dertien gates zelf groen, bevestigde de anchorpins en
de blokorde op drie niveaus empirisch in alle drie de uitvoerders, en
verklaarde de joinstelling van clingo volledig (48 = 49 − 2
defaults-plekken die terecht het concrete type lezen + 1 destijds verkeerd
getelde objectcreatie-kop). Hij blokkeerde de merge op twee punten en
adviseerde er vier. Alle zes zijn hier verwerkt.

1. **Drools speelde hoek (b) stil.** Ch. 7.10 belooft dat beide hoeken door
   alle drie de uitvoerders geweigerd worden vóór er een waarde bestaat.
   Gemeten op een verifier-clean document — kale regel op voorouder `Aa`,
   eigen defaults-groep op afstammeling `Cc` — faulten ref en clingo met
   dezelfde zin en speelde Drools het document: **30,00 uit de Cc-basis, de
   voorouderregel spoorloos**. Oorzaak: `inherited_rule_clashes` bestond
   alleen in de gedeelde OCaml-desugar. De Java-tegenhanger staat nu in de
   prelude die beide transpilertakken delen, met de OCaml-zin letterlijk
   overgenomen. Bijvangst: hoek (a) was luid maar kwam eruit als
   ongeclassificeerde `ERROR`; die loopt nu door het manifest, zodat gate 12
   hem in het auto-uitgesloten kanaal met reden zou rapporteren in plaats
   van als crash.
2. **De spec verhief de weigering tot norm.** SUBTYPING §4.1 regel 4 is
   beslist en niet als filling-in gemarkeerd; ch. 7.10 presenteerde de
   weigering als de norm en voerde er een ontwerpargument voor aan dat tegen
   het besluit pleit. Herschreven: de norm is weer regel 4 (de kale
   voorouderregel ís de geërfde basis, vervangbaar per regel 1), met de
   weigering expliciet als *tijdelijke, luide plaatsvervanger* en met
   verwijzing naar het proposal en naar de getuigen. Hoek (a) blijft
   geweigerd op eigen kracht en was al eerlijk geformuleerd. Result en het
   CONSTRUCTS-rijvoorstel dragen nu dezelfde formulering.
3. **De wrakconflict-formulering klopte niet.** `check-drools.sh` en het
   Result zeiden dat het conflict "voor het eerst zichtbaar wordt op een
   subtype-instantie omdat het subtype twee leden tegelijk toepasselijk
   maakt". Dat is onjuist — beide kenmerken staan op `Strandrecht` zelf en
   de golden pint óók de vlakke instantie. Sterker: die vorm is **per
   constructie onbereikbaar**, en dat staat nu als één zin in ch. 7.10 met
   het WF-034-argument: de condities van een op de voorouder gedeclareerde
   groep resolveren op de voorouder en resolutie loopt alleen omhoog, dus
   geen lid en geen rol die alleen het subtype declareert bestaat daar. Wat
   overerving verandert is de sleutel, niet de bereikbaarheid. Overal
   gecorrigeerd naar wat de golden werkelijk pint.
4. **De zustertoelating had geen golden** terwijl §3.3(b) er een eist.
   `corpus/golden/jutterswet-zustervondst` — zie hierboven. Speelt, dus
   `EXPECTED_PLAYED` 126 → 127.
5. **De drie Drools-laderweigeringen hadden geen getuige.** Nu drie
   JUnit-tests in `OverervingWeigeringWitnessTest`, naast de twee
   hoekgetuigen: zes tests, de eigen suite gaat van 321 naar 327.
6. **Drie normatieve zinnen zonder artefact.** (a) De drieniveauclaim staat
   nu als WV-7.10-d in `ref/test/`, met de lens-probe overgenomen: 11,00
   (grootouder dóór het kind heen), 22,00 (kind boven grootouder), 99,00
   (eigen boven alles, tegen de 30,00 die een ruwe vouw geeft). (b) De
   extensionele posities uniciteitsscope, consistentie-onderwerp en
   kenmerkcheck-eigenaar zijn WV-2.2-a/b/c — de zesdelige opsomming in de
   spec was tot nu toe een opsomming. (c) `expect_fault` pinde alleen dát er
   een fout kwam; er is nu een `expect_fault_msg` en de twee hoekgetuigen
   pinnen hun meldingstekst. Dát gat is precies waardoor punt 1 door deze
   suite glipte.

### Derde ronde: lens 2, en de koerswijziging (2026-09-12)

Lens 2 reproduceerde alle dertien gates en blokkeerde op één nieuwe vorm.
Halverwege de bouw kwamen Yebs drie sturingsprincipes binnen — **point of
least astonishment**, **generieke oplossingen boven specials**, en **meer
functionaliteit in de taal in plaats van minder** — en dat derde keerde het
kernpunt om. Wat als weigering gebouwd was, is loweren geworden. Hieronder
staat de eindstand, met de metingen die haar rechtvaardigen.

**De vorm die tussen beide hekken door viel.** Een gewone regel op het
SUBTYPE zelf tegen een GEËRFDE stapel. WF-050's enkelschrijver-lift is
bewust ketenblind (`IR-SPEC.md` ch. 6.1 legt uit waarom dat zo moet
blijven) en mijn toenmalige clash-detectie eiste een strikte afstammeling.
Gemeten op een verifier-clean document — `Aa` draagt een geordende groep op
`uitkering` (basis 30,00; uitzondering 77,00 bij k3), `Bb is een Aa` draagt
een kale regel `= 7,00`:

- ref: `EVALUATION FAULT: no fixpoint within 100 passes` — waar, benoemt niets;
- clingo: een **tweewaardig model**, `[3000;700]`;
- Drools: **speelt**, en antwoordt stil 30,00 uit de geërfde basis.

**De route: loweren.** Ch. 7.8 leest een kale rekenregel al als een
default. Overerving neemt dat letterlijk: een gewone regel is het
één-ledige **basislid van de laag die hem declareert**. Daarmee vallen
§4.1 regel 1 (de eigen regel vervangt de geërfde basis, geërfde
uitzonderingen blijven erboven) en regel 4 (de voorouderregel wórdt de
geërfde basis) uit de bestaande samenstelling, zonder nieuwe wirevorm en
zonder special: de regel wordt gevouwen zoals de ch.-7.7-ontsuikering er al
één vouwt, conditie en al, met de lege-ALLE voor een onvoorwaardelijke
regel.

Nu gepind, handberekend, door alle drie de uitvoerders: 7,00 zonder de
uitzondering en 77,00 ermee op de lens-probe; 10,00/20,00 en 99,00/99,00 op
de nieuwe golden.

**De poort, en waarom hij er is.** Een regel wordt alleen opgenomen op een
target waarvan de KETEN ergens een defaults-groep declareert. Een kale
regel zonder groep boven of onder zich blijft de gewone ch.-7.8-norm en
komt in geen enkele stapel — dat is elke gewone regel in een vlak model en
elke regel die het corpus vandaag draagt. Nul gedragswijziging daar, en de
gates zijn de regressietoets: 203 goldens verifier-clean, 453 cases exact
herspeeld door ref en clingo, 128 door Drools.

**De regel uit het circuit.** Een opgenomen regel mag niet óók als regel
vuren. `Desugar.absorbed_on` geeft de concrete typen waarop dat gebeurt;
ref filtert zijn subjecten, clingo zet `not is_a(S,"T")` in de body, en de
exec-model-tak een klassenklem op het patroon — nodig omdat een patroon op
de voorouderklasse met native `extends` óók de feiten van de afstammeling
matcht. Eén predicaat, drie lezers, dus geen twee die op verschillende
instanties opnemen. De DRL-tekst-tak raakt dit niet: die weigert elk
document met defaults bij binnenkomst, dus daar is de verzameling per
definitie leeg.

**Wat verwijderd is.** `inherited_rule_clashes` (OCaml) en
`CaseRunner.geerfdeRegelBotsingen` (Java) zijn weg, niet bewaard, en met
hen hun aanroepplekken. De twee weigergetuigen `WV-7.10-c-...` zijn
omgebouwd tot de waardegetuigen WV-7.10-e/f; de JUnit-hoekgetuigen tot één
waardegetuige plus de grensgetuige. Verantwoord hier omdat er weiger-dekking
mee verdween: wat die getuigen bewaakten is nu gedrag dat een golden en
zeven voorbeelden pinnen.

**NOOT B — gemeten, en het was géén artefact.** De recursie vouwt wél over
de EFFECTIEVE ouderstapel. Twee geërfde groepen ontstaan maar op één
manier: een voorouder declareert zelf een gedeclareerd-ongeordende set —
precies het onderwerp van §4.1 regel 3, dus legitiem.

Kan het dan componeren? **Nee, en de reden is scherp genoeg om de
terugvalregel te gebruiken.** De lezing van regel 3 is een DRIELAAGS
prioriteit — eigen uitzonderingen boven het ongeordende geërfde blok boven
de eigen basis — terwijl de leden van een defaults-groep als GELIJKEN
verzoenen, zonder onderlinge orde. De justificatie van de eigen basis is
onvoorwaardelijk, dus wáár hij ook zou zitten beslist hij zodra een geërfd
lid beslist, en "een geërfde uitzondering wint van de subtype-basis" (§4.4)
valt dan niet te onderscheiden van "de basis beslist als niemand anders
beslist". Het enige generieke mechanisme dat wél *verzoen-een-set-anders-
val-terug* uitdrukt is de group-default van ch. 7.9 — en het blok daarin
wikkelen verplaatst de conflictsleutel van de WORTELvorm die §4.4 pint naar
de padgesleutelde vorm. Dat zou het bindende proposal stilzwijgend
wijzigen, precies wat de vorige ronde terecht afkeurde. Het vraagt een
gelaagde stapel die deze wire niet heeft.

Route: de kopzin versmald in plaats van de vorm gecomponeerd, en de twee
gevallen uit elkaar gehaald — regel 3 (gedefinieerd, niet verlaagd, mét de
bovenstaande reden) en een eigen ongeordende set tegen een geërfde stapel
(werkelijk ongedefinieerd). Beide met eigen melding, beide getuigd in beide
suites.

**NOOT A.** De geciteerde zin stond nergens in `spec/`. Ch. 7.10 draagt nu
letterlijk *"not lowered in this release"* op de enige plek waar hij nog
geldt.

**NOOT C.** De onbereikbaarheidspassage noemt nu **WF-053** bij het
rollenbeen naast WF-034 bij het ledenbeen, plus de bijzin dat case-invoer
voorouder-gedeclareerde leden vrij zet op de vlakke instantie — een
subtype-regel beperkt het subtype en bevrijdt het niet.

**NOOT D.** Ch. 7.10.1 draagt nu drie tabellen: de zes cellen, de
drieniveaugetallen (90,00 / 11,00 / 22,00 / 99,00 / 22,00 / 99,00) en de
regel-in-het-basisvak (10,00 / 20,00 / 99,00). De zes-posities-opsomming in
IR-SPEC ch. 2.2 wijst naar haar getuigen.

**DRL-tekst-tak, de resterende weigering: niet meegenomen.** Die tak
weigert *elk* document met `prioritizedDefaults` bij binnenkomst — hij
heeft geen defaults-lowering — dus de generieke melding gaat vooraf aan en
omvat de benoemde reden. Hem daar alsnog laten dragen zou een reden
toevoegen aan een tak die de vorm hoe dan ook niet kan verlagen.

### Voorgesteld CONSTRUCTS-rijmateriaal (letterlijke celtekst, rij 39)

| # | Construct | Origin | Verdict (v1) | Kernel or sugar? | v0→v1 delta | Notes |
|---|---|---|---|---|---|---|
| 39 | Object-type inheritance (a single supertype) | RegelSpraak / the canonical data model, which uses it functionally (measured 2026-09-10: 157 entities with a direct parent, 44 chains deeper than one level, exactly 2 with two direct parents) | core | kernel | new kernel (extension per the § Method, not emitter-produced yet — the dialect card makes it emitter-borne, the route `Let`/`BinderRef` took): `ObjectType.supertype: string?` on the closed `Objecttype` node (IR-SPEC ch. 2.2), one direct parent by declared name, absent = a root type. Member resolution walks the chain UPWARD ONLY and the effective member set is the chain-closed union; quantification is EXTENSIONAL at every type-ranging position, with no exact-type quantifier; a role's declared filler type is an UPPER bound; a case instance names its CONCRETE type in the existing `objectType` key and creation is not polymorphic. Four rules: WF-103 (the reference resolves), WF-104 (acyclic, self-reference included), WF-105 (no shadowing along the chain — WF-002 lifted) and WF-106 (at most one direct supertype), the last with **no verifier check and no negatief** because the wire shape makes the violation unwritable — a deliberate, argued departure recorded in ch. 2.2, its witnesses the two frontend fixtures. Three rules widen without a number: WF-034's member cross over the chain-closed set (the downward read is the new reachable refusal, negatief `wf034-neerwaartse-lezing`), WF-047's "agree" to ⊑-compatibility, WF-048's uniqueness over the chain-closed pair set (so a subtype can turn a well-formed bindingless navigation into a loud refusal — accepted). WF-026/050/079 gain a CONSTRAINT rather than a restatement: read on the per-type effective stack, never on a chain-closed target key (ch. 6.1). Semantics `RUNTIME-SEMANTICS.md` ch. 7.10 — a subtype inherits the whole defaults stack per target, folded over the parent's EFFECTIVE stack in one fixed block order (own exceptions, inherited exceptions, base with the own one replacing the inherited one, init/input defaults never replaced), so a clash across the boundary is an ordinary write decided by the subtype and deposits no conflict fact, while the ch. 7.2 conflict key names the type that DECLARES the attribute. **Landed (2026-09-12):** all three executors — `ref` and the ASP backend share the composition in `Ir_v0.Desugar`, clingo carries the type closure as a RULE in the ASP (`isa/2` over `is_a/2` plus `isa_sub/2` facts, so the leg stays an independent reimplementation instead of a second copy of the OCaml chain walk), and Drools lowers to native class-hierarchy subtyping (`extends` on the generated bean, own members only, no pattern site or accumulate changed). Anchor `anchor/jutterswet-strandrechtketen` (four cases, the six cells of ch. 7.10.1; gates 5, 6 and 12), plus `jutterswet-wrakconflict` (the conflict-key form: a clash inside an inherited internally-unordered block, keyed on the declarant, with both instance kinds on one case) `jutterswet-zustervondst` (the §3.3b sibling admission, the two `toeslag` declarations carrying different datatypes so a crossed chain hits the write boundary) and `jutterswet-wrakbasis` (§4.1 rule 1's bare-rule branch: the subtype's own RULE as its own base, 10,00/20,00 without the exception and 99,00/99,00 with), and five negatiefs | single inheritance holds at three layers by construction: the wire shape, Java's single class inheritance, and the two frontends' refusals — so the two known multiple-parent cases of the canonical data model are named in the frontends' messages and nowhere else. An ORDINARY rule is the base of its level, so SUBTYPING §4.1 rule 1 (the subtype's own rule replaces the inherited base) and rule 4 (an ancestor's rule IS the inherited base) are lowered rather than refused — ch. 7.8 already reads a bare rule as a default and inheritance takes that at its word. The fold is GATED on the target's chain declaring a group somewhere, so a bare rule with no group above or below it stays the ordinary ch. 7.8 standing norm, which is every ordinary rule in a flat model; an absorbed rule is suppressed as a rule from one shared predicate, so no two executors absorb on different instances. TWO forms still refuse loudly in all three, with witnesses in `ref/test/` (WV-7.10-c) and the Drools backend's own suite, since no corpus case can hold either (a golden must replay and `ref` faults on both): one own group over an inherited declared-unordered SET — DEFINED by §4.1 rule 3 but not lowered, because that reading is a three-layer priority while a group's members reconcile as equals, and the one generic fall-back mechanism (ch. 7.9's group-default) would move the conflict key off the root form §4.4 pins — and a declared-unordered set on the OWN level, which the proposal leaves undefined. The Drools loader additionally refuses a chain carrying a temporal or dimensioned member: its facet side maps are read per declared type at some forty places, and an inherited timeline member would lower as a scalar |
