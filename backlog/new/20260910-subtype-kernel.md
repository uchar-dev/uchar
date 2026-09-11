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

**WF-nummers (gereserveerd vanaf WF-103; WF-102 is vandaag vergeven aan
de aggregatie-bereikpadregel; de hoofdsessie maakt de nummers
definitief bij de landing).**

- WF-103 — `supertype` benoemt een gedeclareerd objecttype.
  Negatief `corpus/negatief/wf103-onbekend-supertype`.
- WF-104 — de supertypegraaf is acyclisch, zelfverwijzing inbegrepen.
  Negatieven `wf104-supertype-cyclus` en
  `wf104-supertype-zelfverwijzing`. Model voor de implementatie:
  WF-085's kleuren-DFS met benoemd cyclus-pad,
  `spec/verifier/lib/checks.ml:2859–2912`.
- WF-105 — geen attribuut- of kenmerknaam twee keer op één keten
  (ci; WF-002 opgetild naar de keten). Negatief
  `wf105-hershadowd-attribuut`.
- WF-106 — een defaults-groep die een niet-lege geërfde
  uitzonderingsblok erft, declareert zijn laaggrensordening
  (`geerfdeUitzonderingen` ∈ `boven` | `onder` | `ongeordend`).
  Negatief `wf106-ongeordende-laaggrens`.
- WF-107 — in reserve voor wat de eigen lensronde oplevert; blijft
  onvergeven als het niet nodig is.

Alles wat de kaart verder raakt is een **herformulering van bestaande
regels** onder de ketenbewuste lezing van "resolveert", en krijgt géén
nieuw nummer: WF-034 (ledenkruising over de ketengesloten verzameling;
de neerwaartse lezing is de nieuwe bereikbare weigering, negatief
`wf034-neerwaartse-lezing`), WF-047 ("overeenkomen" verbreedt tot
⊑-compatibiliteit), WF-048 (uniciteit over de ketengesloten
typeparen), en WF-026/WF-050/WF-079.

**De zin die geland moet worden** (`IR-SPEC.md` ch. 6.1 en
`RUNTIME-SEMANTICS.md` ch. 7): de enkelschrijver-hekken worden gelezen
op de **effectieve stapel per type**, ná overervingsresolutie, nooit op
de ruwe declaratieverzameling. Zonder die zin struikelt de kaart over
haar eigen anchor: supertype-regel en subtype-regel op één doel zijn
geen twee schrijvers.

**Loweringsbesluiten, nu beslist.**

- **Drools: native klassehiërarchie.** De gegenereerde fact-bean van
  een subtype krijgt `extends` en `FactModelGenerator.leden` emit
  alleen de eigen leden. Geen enkele patroonplek, accumulate of
  contractcontrole verandert: Drools matcht klassepatronen via
  `instanceof`, `ContractValidator` loopt al door de hiërarchie
  (`c.getMethods()`), en het getypte accessorpad draagt de
  subtypewacht `owner.isInstance(o)` al, vooruitlopend op dit geval
  (`runtime/Accessors.java:117–120`, setterarm `:144`). Vlakke emissie
  met discriminator is verworpen: 65 `declarationOf`-plekken plus elke
  accumulate zouden een transpile-time transitieve sluiting nodig
  hebben, en de getypte to-one referentievelden zouden verbreed moeten
  worden.
- **clingo: de sluiting in de ASP**, niet in OCaml — `isa_sub/2`-feiten
  plus een transitieve regel, met de 52 `is_a(`-joins daardoorheen. Zo
  blijft het clingo-been een onafhankelijke herimplementatie in plaats
  van een tweede kopie van de OCaml-wandeling.
- **ref: één functie** — `World.of_type` (`ref/lib/world.ml:52–57`)
  vervangt `norm i.otype = norm otype` door een ketenwandeling.

**Anchor en getuigen.**
`corpus/golden/anchor/jutterswet-strandrechtketen` — `Strandrecht` →
`Wrakrecht` plus een aggregerende `Kustgemeente`, met de zescellige
tabel van SUBTYPING §7.2 als absolute verwachting. Drie cases:
`case1-basis.json` (10 / 20; registratiekosten 5 op béíde; totaal 30,
aantal 2 — een niet-extensionele lezing geeft 10 en 1 en valt hier
luid om), `case2-geerfde-uitzondering.json` (99 / 99, totaal 198), en
`case3-laaggrensconflict.json` (geen schrijving plus één
§9.5-conflictfeit, gesleuteld op `Strandrecht.uitkering` — het
declarerende type, niet het concrete, en op de wortel, niet padgesleuteld).

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
