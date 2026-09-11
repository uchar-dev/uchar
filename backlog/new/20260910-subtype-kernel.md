# Subtypering in de kernel: IR, verifier en de drie executors

Goal: enkelvoudige objecttype-overerving als eersteklas kernel-begrip, uitgevoerd volgens `spec/proposals/SUBTYPING.md` (de onderzoekskaart is de voorganger en haar besluiten zijn bindend): de optionele supertype-verwijzing op het objecttype in IR-SPEC met WF-regels voor acycliciteit, enkelvoudigheid, keten-naamresolutie, sibling-botsingen, rolcompatibiliteit, concrete-type-instantiatie met discriminator in de invoer, en de overervingssemantiek van opdrachtbesluit 3: per target erft het subtype de defaults-stapel van het supertype, subtype-eigen gewone regels vervangen de geërfde basis, geërfde uitzonderingen blijven erboven, en botsende uitzonderingen over de laaggrens volgen de ongeordend/§9.5-route; de lowering in alle drie de executors — ref met subtype-check in de evaluator, clingo met `isa/2` en transitieve sluiting, Drools volgens de in het proposal besliste vorm — plus de minimale jutterswet-use-case uit opdrachtdeel 1.5 als anchor.

Acceptance: de nieuwe anchor speelt groen door verifier → ref → clingo (→ Drools conform het proposal-besluit) met de vier bewijsmomenten expliciet in de verwachte uitkomsten: een supertype-regel die voor een subtype-instantie vuurt, een supertype-kwantificatie die super- en subtype-instanties samen telt, de pinguïn-toets (een subtype-eigen basis vervangt de geërfde basis terwijl een geërfde uitzondering erboven blijft winnen), en een botsende geërfde-tegen-eigen uitzondering die als §9.5-conflictfeit deponeert in plaats van stil voorrang te krijgen; negatieve getuigen voor cyclus, tweede directe ouder, botsend sibling-attribuut en onbekend supertype weigeren luid met de gereserveerde WF-nummers; het volledige bestaande corpus parseert, verifieert en emit byte-identiek; de volledige gate-reeks van de repo is groen.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md` (nieuwe rij), de verifier, `ref/`, de clingo- en Drools-backends, `corpus/` (nieuwe anchor + negatieven), en de eigen kaart.

Out of scope: elke frontend (RegelSpraak-dialect en F-logic hebben eigen kaarten); meervoudige overerving; een vrije specificiteitsrelatie tussen willekeurige regels (het C2-besluit blijft staan); wijzigingen aan de defaults-machinerie anders dan wat de groepserving van besluit 3 vereist.

## Aangescherpt door de onderzoekskaart (2026-09-11)

`spec/proposals/SUBTYPING.md` is bindend voor deze kaart; wat hieronder
staat is de uitvoerbare neerslag ervan, niet een tweede besluit.

**Wirevorm.** `ObjectType.supertype: string?` — één extensieveld
(`IR-SPEC.md` § Method: optioneel, nooit `required`, nooit null, heel of
niet), wire-sleutel `supertype`. De kernelkaart landt het
emitter-vrij met handgeschreven anchorgetuigen; de dialectkaart maakt
het daarna emitter-dragend, langs de route die `Let`/`BinderRef` namen.

**WF-nummers (gereserveerd vanaf WF-103).** WF-101 is de hoogste regel
in de WF-index op de basis van deze branch; WF-102 staat daar níet in
en is geclaimd door de parallelle bereikpadkaart (branch
`card/geneste-aggregatie-bronpad`), die vandaag landt. De hoofdsessie
maakt de nummers definitief bij de landing.

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
- WF-106 — een defaults-groep die een niet-lege geërfde
  uitzonderingsblok erft, declareert zijn laaggrensordening
  (`geerfdeUitzonderingen` ∈ `boven` | `onder` | `ongeordend`).
  Negatief `wf106-ongeordende-laaggrens`. **Let op: het veld zelf is
  een aanbeveling, geen geratificeerd besluit** — SUBTYPING §11.2 legt
  de kleinere variant (laaggrens altijd ongeordend, `boven`/`onder`
  laten vallen) aan de maintainer voor. Bouw het niet voordat dat punt
  beantwoord is.
- WF-107 — in reserve voor wat de eigen lensronde oplevert. De meest
  waarschijnlijke gegadigde: de markeringsplicht uit SUBTYPING §4.1
  regel 3 (een subtype-regel die een ongeordende oudergroep binnenkomt
  moet zelf ongeordend gemarkeerd zijn). Die plicht heeft nu géén
  handhaving — WF-050's check is naam-gesleuteld en de optilling die
  hem zou laten vuren is precies wat §4.2 verbiedt. Deze kaart beslist
  of het een eigen, grens-gescopete regel wordt of een ongecontroleerde
  SHOULD blijft.

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
- **clingo: aanbeveling, geen besluit** — de sluiting in de ASP
  (`isa_sub/2`-feiten plus een transitieve regel, met de **49
  `is_a(`-joinregels / 50 voorkomens** daardoorheen; van de 52 regels /
  53 voorkomens zijn er 2 commentaar op `:1256` en `:3818` en 1 de
  feitemitter op `:6457`) houdt het clingo-been een onafhankelijke
  herimplementatie. Het inspanningsargument wijst de andere kant op;
  SUBTYPING §11.3 legt de keuze bij de maintainer.
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
- `case4-laaggrensconflict.json` (cel 6): geen schrijving plus één
  §9.5-conflictfeit, gesleuteld op `Strandrecht.uitkering` — het type
  dat het **attribuut** declareert, niet het concrete type en niet het
  type wiens groep botste — en op de wortel, niet padgesleuteld.

Datatypes in de bron volgen de corpusspelling `Numeriek (…)` (137
voorkomens; `Bedrag` en `Getal` komen nergens voor), en de twee
Kustgemeente-attributen heten `uitkeringstotaal` en `registertal` —
níet "aantal strandrechten", want een attribuut dat de rolnaam deelt
zet de anchor boven op de ch. 2.6-naamdelingsvorm die de Drools-bean
niet kan dragen.

**Eén afwijking van de acceptatietekst hierboven, expliciet gemeld.**
De negatieve getuige voor "tweede directe ouder" bestaat niet en kan
niet bestaan: `supertype` is één string, dus een tweede ouder is geen
document dat de verifier afkeurt maar een document dat niet te
schrijven valt (gate 2 weigert de arrayvorm op de gesloten node). De
weigering ligt bij het schema en bij de twee frontends, met de twee
CGM-gevallen `RechtOpUitkeringWazo` en `RechtUitkeringZw` bij naam.
SUBTYPING §11 punt 1 legt het alternatief (`supertypes: [string]` plus
een "hoogstens één"-regel) aan de maintainer voor; tot hij anders
beslist geldt de enkele string.
