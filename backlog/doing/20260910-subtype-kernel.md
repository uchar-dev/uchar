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
