# V1-WIREMIGRATIE — de brekende v0→v1-lijst, compleet en besluit-klaar

*Status: **VOORSTEL**. `spec/proposals/` is beschermd pad: dit document is de
uitkomst van de inventariskaart `20260918-v1-wiremigratie-inventaris`, niet
een genomen besluit. De maintainer beslist op de genummerde vragen van §5;
tot dan is elke aanbeveling hier een aanbeveling. Niets in dit document
wijzigt code, schema, corpus of gates — de kaart die het voortbracht deed dat
niet en mocht dat niet.*

*Meetdatum van alle tellingen: 2026-09-19, op `main` @ `245f7152`. Alle
veertien gates stonden bij die meting groen.*

---

## 0. Waarvoor dit document dient

`spec/IR-SPEC.md` (Method) reserveert één gebeurtenis voor de niet-additieve
opruiming:

> The `regelspraak-ir-v1` format bump is reserved for the non-additive
> cleanup — renames, and dropping the v0 fields the extensions supersede
> (`zetVeld`/`joinVeld` and kin).

Die gebeurtenis is nooit ingepland, en de opgespaarde lijst is intussen niet
meer te overzien uit één bron: hij staat verdeeld over de deltakolom van
`spec/CONSTRUCTS.md` (40 rijen plus een knoopinventaris), zestig
`**Delta`-markeringen in `spec/IR-SPEC.md`, en een handvol besluiten die
alleen in prozanoten staan. Dit document legt de lijst compleet op tafel, met
per brekende delta wat er van de wire verdwijnt, wie er meebeweegt, hoe groot
de corpusherschrijving is, en — waar een keuze open ligt — een genummerde
vraag met opties en een aanbeveling.

De reden om de lijst nú compleet te hebben en niet stuk voor stuk af te
handelen staat in de kaart: elke brekende delta dwingt **drie emitters**
(RegelSpraak, Catala, F-logic), **vier consumenten** (`ref`, `backends/clingo`,
`backends/drools`, `spec/verifier`), **het schema**, **650 corpusdocumenten**
en **de toolchain-pin** tot meebewegen. Dat is één keer betaalbaar en twintig
keer niet.

---

## 1. Afbakening: wat hier "brekend" heet

**Brekend** = een document dat vandaag geldig is, is na de wijziging ongeldig
of betekent iets anders. Concreet: een veld verdwijnt, een veld verandert van
vorm of type, een knoop wordt hernoemd of gesplitst of samengevoegd, een
verplichting komt erbij waar de oude vorm niet aan voldoet.

**Niet brekend, en dus niet op deze lijst:**

- **Additieve uitbreidingen.** De wire-entry-discipline van IR-SPEC (Method,
  besloten 2026-08-16) laat een v1-construct binnen als optioneel veld of
  optionele knoop náást de bestaande vorm. Alles wat zo geland is —
  `prioritizedDefaults`, de som- en recordfamilie, de vouwfamilie,
  `Kwantorvouw`, `Let`/`BinderRef`, `bronPad`, `supertype`, `TemporalRead`,
  `bronAnnotaties` — breekt niets en hoeft niet op de klap te wachten.
- **WF-verscherpingen zonder vormwijziging** (`**Delta (promotion)**`, 36 stuks
  in IR-SPEC). Die zijn al van kracht: ze verkleinen wat welgevormd is, maar
  laten de vorm van de wire ongemoeid. Hun kosten zijn al betaald.
- **Semantische deltas zonder wire-gevolg.** `RUNTIME-SEMANTICS.md` draagt 27
  `v0 mirror note`s en tien `tracked delta`s die over *betekenis* gaan (de
  lege-waarde-discipline, de staande norm van ch. 7.8, de
  call-by-need-barrière van ch. 10.6). Geen daarvan verandert een byte op de
  wire.

Eén grensgeval is bewust binnengehaald: **B12** (WF-048 vervalt) voegt geen
veld toe en haalt er geen weg, maar maakt een vandaag welgevormde vorm — de
bindingloze vouw — ongeldig. Dat is per bovenstaande definitie brekend, en het
raakt 16 documenten in de groene zones.

---

## 2. Hoe de lijst compleet is gemaakt

Twee onafhankelijke doorgangen, beide hieronder verantwoord met tellingen. Ze
overlappen grotendeels — dat is de bedoeling: waar ze elkaar niet dekken zit
het risico.

### 2.1 Registervergelijking — elke rij van `spec/CONSTRUCTS.md`

De 40 rijen van de scope-matrix zijn stuk voor stuk op hun deltakolom
geclassificeerd. Verdeling:

| Klasse | Rijen | Welke |
|---|---:|---|
| **Brekend** (levert een of meer B-items) | **13** | 1, 2, 3, 4, 5, 11, 16, 20, 22, 25, 26, 27, 30 |
| **Additief geland** (v1-vorm rijdt naast de v0-vorm) | **10** | 7, 8, 9, 10, 21, 24, 36, 38, 39, 40 (rij 21 draagt alleen de WF-027-versoepeling) |
| **Gedaan** (delta is uitgevoerd, geen wire-schuld meer) | **7** | 6, 14, 15, 17, 19, 23, 29 |
| **N.v.t. voor v1** (non-goal of later(v2)) | **10** | 12, 13, 18, 28, 31, 32, 33, 34, 35, 37 |

Samen 40. Twee brekende items hebben **geen** eigen matrixrij en komen uit de
preambule en de knoopinventaris: de globale hernoeming (preambule van
`CONSTRUCTS.md`, PLAN §9 besluit 6) en de container-boekhouding — de
`regels`-stubs en `diagnostics`, beide uit §2.3/§2.4 met het oordeel
`non-goal`.

*(Rij 31 — tekstinterpolatie — staat als n.v.t. en niet als brekend: het
`sugar`-oordeel geldt de brontaal, maar IR-SPEC ch. 4.2 houdt de knoop
uitdrukkelijk aan — "**TextComposition** … sugar verdict (row 31),
mirrored". Hetzelfde geldt voor `Begrensd`: ch. 4.2 houdt **Clamp** aan als
v1-knoop, en WF-099 hangt er sinds 2026-09-10 aan. Beide bewegen dus alleen
mee in de hernoeming B6, niet als eigen delta. Dit is de enige plek waar het
register en IR-SPEC uit elkaar lopen; zie **V3**.)*

Daarnaast is de **knoopinventaris** van `CONSTRUCTS.md` §2.1–§2.4 integraal
doorgenomen — 12 `Expr`-subtypen, 9 `Conditie`-subtypen, 13 top-level
`Resolved*`-vormen en 20 modellaag-knopen. Die leverde vier items op die geen
eigen matrixrij hebben: de `Regel`-stub (non-goal, "collapses away"),
`Severity`/`Diagnostic` (non-goal, "build-time tooling, not IR"), `Rationaal`
(frontend-hulpmiddel, staat niet op de wire — geverifieerd tegen het schema),
en `Methode.COMPLEX` (WF-024 geland, maar zie B21's noot over de toka-zone).

### 2.2 Mechanische sweep

Zoektermen over `spec/` en het schema, met ruwe treffertellingen:

| Term | IR-SPEC | RUNTIME-SEM. | CONSTRUCTS | ASP-TRANSL. | schema |
|---|---:|---:|---:|---:|---:|
| `v0 mirror` | 58 | 27 | 0 | 1 | 0 |
| `mirror` (breed) | 112 | 77 | 11 | 18 | 1 |
| `**Delta` | 60 | 6 | 1 | 0 | 0 |
| `*Migration:*` | 14 | 0 | 0 | 0 | 0 |
| `tracked delta` | 4 | 10 | 3 | 2 | 0 |
| `format bump` | 2 | 0 | 1 | 0 | 0 |
| `inert` | 12 | 0 | 2 | 2 | 1 |
| `baggage`/`bagage` | 1 | 0 | 0 | 0 | 0 |
| `superseded` | 3 | 0 | 1 | 0 | 0 |
| `retires`/`retire` | 1 | 4 | 2 | 1 | 0 |
| `deprecated`/`legacy` | 0 | 0 | 0 | 0 | 0 |
| `OPEN` | 107 | 48 | 18 | 11 | 1 |

De **60 `**Delta`-markeringen in IR-SPEC** zijn de dichtste bron en zijn
uitputtend geclassificeerd:

- **36 × `(promotion)`** — WF-verscherping, geen wire-vorm. Afgehandeld per
  §1: niet brekend.
- **3 × `(v1 normalisation / tightening / widening)`** en **2 × `(new wire
  law)`** en **1 × `(diverge-from-v0)`** — alle zes geland. Niet brekend.
- **18 vormdragende markeringen** — `(tracked …)` (8), `(row …)` (7), kaal
  `Delta:` (2), `(CONSTRUCTS.md ch. 2.2)` (1). Deze achttien zijn één-op-één
  afgehandeld; de kolom "sweep-herkomst" in §6 noemt per B-item zijn
  regelnummer.

Vier brekende punten vielen **buiten** de `**Delta`-markeringen en zijn alleen
via de sweep op `inert` / `baggage` / `retires` / `mirror` en via het register
gevonden. Ze staan hier apart omdat ze laten zien dat één bron niet genoeg was:

1. **De `regels`-stubs** (IR-SPEC ch. 3, maintainersbesluit 2026-09-05) —
   "They are inert v0 mirror baggage, exactly like the bean fields of ch. 3.4,
   and unlike those they are not even a superseded mirror of something checked:
   they are NON-NORMATIVE." Geen `**Delta`-markering, geen WF-regel. → **B3**.
2. **WF-048 vervalt** (IR-SPEC regel 1537) — "In v1 the binding becomes
   required and WF-048 retires with the format bump." → **B12**.
3. **De referentiepositie-`type` verlaat de wire** — staat alleen in de
   deltakolom van CONSTRUCTS rij 1, als "Tracked v1 delta", en wordt machinaal
   voorbereid door gate 13. Geen `**Delta`-markering in IR-SPEC. → **B13**.
4. **De globale hernoeming NL→EN** — staat in de preambule van CONSTRUCTS, in
   de Conventions van IR-SPEC, en als PLAN §9 besluit 6. → **B6**.

**Sweep over de consumenten.** Los van het proza is gemeten wie wat leest. De
193 propertynamen van het schema zijn gekruist tegen de vier consumenten. De
uitkomst corrigeert een aanname die de kaart nog droeg:

> De gedeelde OCaml-loader (`spec/verifier/lib/loader.ml`, gebruikt door
> verifier, `ref` én clingo) leest alle 193 namen, want hij is strikt: hij
> controleert de volledige sleutelset per knoop. De Drools-loader leest er
> 172. **Er is geen enkel veld met nul lezers.** De spiegelvelden zijn dus
> *niet* inert: de verifier kruist ze (WF-028, WF-034 (m)/(o)/(q), WF-054), en
> `backends/drools` **voert ze uit** — beide transpilertakken.

De javadoc van de Drools-loader zegt het zelf, en dit is de zwaarste enkele
vondst van deze kaart:

> **Authority.** The v0 BEAN fields are authoritative: where the emitter also
> writes a symbolic extension that is a record field … it is reconstructed
> faithfully; where the extension is purely emitter-derived and NOT a record
> field (an AttribuutRef's `navigatieBindingen`, a Functie's `bronRol`, a
> Kenmerk's `navigatieBinding`, a verdeling's `verbindendeRol`) it is ignored —
> the transpiler re-derives it from the model.

Voor vier symbolische uitbreidingen geldt dus: de Drools-backend negeert ze en
leidt ze zelf af uit het model. Het v0-veld weghalen is daar geen opruiming
maar een **amputatie**. Dat bepaalt de volgorde van het vervolgprogramma (§6).

### 2.3 Wat er verder buiten valt, met reden

- `spec/ASP-TRANSLATION.md` levert nul brekende punten op. De vijf treffers op
  de zoektermen gaan over rekenkundige inertie en over semantische deltas, niet
  over de wire.
- `spec/schema/uchar-case-v0.schema.json` is een **apart formaat** en is al
  Engelstalig. Het verwijst nergens naar IR-knoopnamen; het noemt alleen
  *gedeclareerde* namen (objecttype, attribuut, kenmerk, rol, enumeratielabel),
  die per conventie verbatim in de brontaal blijven. Het casusformaat hoeft dus
  **niet** mee te versienummeren. Drie uitzonderingen, en dat zijn precies de
  literaalvormen: `enumeration` (9 bestanden), `dateTime` (10 bestanden) en
  `number` met eenheid (506 bestanden) bewegen mee als B22 respectievelijk B17
  landt. Dat is exact wat de tekstlexeem-kaart van 2026-09-18 al meemaakte: drie
  case-invoeren moesten mee omdat elke uitvoerder `Tekst` verbatim vergelijkt.
- De 566 casusbestanden zelf blijven geldig, op die drie waardesoorten na.

---

## 3. De brekende lijst

Vijfentwintig items in zeven families. Per item: de wire-diff, de geraakte
partijen, de gemeten corpusomvang, de migratienota, en waar nodig de
beslisvraag.

**Notatie in de partijen-regel:** `E:` emitters, `C:` consumenten, `S:` schema,
`K:` corpus, `T:` toolchain-pin.

---

### Familie A — de container en zijn boekhouding

#### B1 — De formaatnaam en het schema-id

**Wire-diff.**

```json
// vóór
{ "formaat": "regelspraak-ir-v0", ... }
// ná
{ "format": "uchar-ir-v1", ... }
```

**Partijen.** `E:` alle drie. `C:` alle vier (de formaatcontrole is de eerste
regel van elke loader). `S:` `$id`, `title`, `description`, de `const`. `K:` **650
van 650** documenten. `T:` de pin.

**Migratienota.** Mechanisch. Dit is het enige item dat de klap *zichtbaar*
maakt: een consument die v0 verwacht faalt hier luid in plaats van een veld
stil te missen. Het bestandspad van het schema verhuist mee
(`spec/schema/regelspraak-ir-v0.schema.json` → een v1-naam), en gate 2's
`SCHEMA`-variabele met het.

**Beslisvraag: V1.**

---

#### B2 — De zes kaart-tabellen worden lijsten; de zelfnaam verdwijnt

**Wire-diff.**

```json
// vóór — een MAP, waarvan elke waarde haar eigen sleutel herhaalt
"objecttypen": { "Jutter": { "node": "Objecttype", "naam": "Jutter", ... } }
// ná — een LIJST, de naam één keer
"objectTypes": [ { "name": "Jutter", ... } ]
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` zes top-level properties
(`objecttypen`, `parameters`, `eenheidstelsels`, `feittypen`, `dimensies`,
`domeinen`) plus het `naam`-veld op elk van hun waardeknopen. `K:` 639 documenten
dragen `objecttypen` (966 objecttypen), 215 `feittypen` (386), 142 `domeinen`
(221), 136 `eenheidstelsels` (248), 134 `parameters` (664), 13 `dimensies` (13).
Het veld `naam` staat 5177 keer in 642 documenten — niet alles daarvan is
zelfnaam, maar de zes tabellen leveren 2498 van die voorkomens. `T:` de pin.

**Migratienota.** WF-094 (2026-09-06, vier clausules) beschrijft de zelfnaam al
letterlijk als "the v0 denormalisation of the ONE `name` a v1 declaration
carries on its node". De migratie is mechanisch: neem de sleutel, gooi het veld
weg. Bij de klap **vervallen alle vier de WF-094-clausules** — er is geen tweede
spelling meer om tegen te kruisen. Hun tien negatief-getuigen verliezen hun
onderwerp (zie V15).

Het argument om de tabellen naar lijsten te brengen is **WF-019**: declaratievolgorde
is normatief en moet door serialisatie behouden blijven. Een JSON-object
garandeert die volgorde niet; een array wel. Vandaag rust WF-019 op de
byte-identieke round-trip van gate 2, dus op het gedrag van de serialisator, niet
op de vorm. De lijst maakt de eis structureel.

**Beslisvraag: geen** — de richting staat in IR-SPEC ch. 2 (de rootcontainer
noemt `objectTypes` een "list of ObjectType") en in WF-094. Wel geraakt door
**V3** (nesten of vlak).

---

#### B3 — De inerte `regels`-stubs verlaten de wire

**Wire-diff.**

```json
// vóór
"regels": [ { "node": "Regel", "naam": "jutheffing", "soort": "BEREKENING" }, ... ]
// ná — het veld bestaat niet meer
```

**Partijen.** `E:` RegelSpraak (schrijft ze; Catala en F-logic schrijven een lege
array). `C:` geen enkele consument leest ze inhoudelijk — de OCaml-loader
valideert de vorm, de Drools-loader parseert ze in een record dat niets
terugleest. `S:` één required top-level property, één `$def`. `K:` **472
documenten, 917 stubs.** `T:` de pin.

**Migratienota.** Dit is het enige item op de lijst met een expliciet
maintainersbesluit dat de vorm **niet-normatief** verklaart (2026-09-05,
IR-SPEC ch. 3):

> Nothing in this specification gives a stub a meaning, no well-formedness rule
> crosses one, and a consumer reading one would be reading something this
> document does not define.

Weghalen kost per document één array. Geen WF-regel valt weg (er is er geen).
Wat wél wegvalt is de enige plek waar `soort` als kale string voorkomt buiten
`ResolvedRegel`, wat B21 een fractie eenvoudiger maakt.

**Beslisvraag: geen.** Het besluit is al genomen; dit is de uitvoering ervan.

---

#### B4 — `diagnostics` verlaat de wire

**Wire-diff.**

```json
// vóór
"diagnostics": [ { "node": "Diagnostic", "severity": "WARNING", "message": "…", "line": 12 } ]
// ná — het veld bestaat niet meer
```

**Partijen.** `E:` alle drie (RegelSpraak vult hem, de andere twee schrijven
leeg). `C:` de OCaml-loader en de Drools-loader parseren hem; niemand leest hem
terug. `S:` één required top-level property, één `$def`. `K:` 650 documenten
dragen de sleutel, **2 dragen een niet-lege array** (beide in de toka-zone).
`T:` de pin.

**Migratienota.** `CONSTRUCTS.md` §2.4 velt het oordeel: `Severity`/`Diagnostic`
→ "non-goal — build-time tooling, not IR", en IR-SPEC ch. 2 herhaalt het
("the v0 container also carries … build diagnostics … those are not part of the
IR").

Er is één argument tegen dat expliciet gewogen moet worden, en het is in de
laatste maanden zwaarder geworden: **de wire draagt sinds meerdere kaarten
bewust een WARNING** als de manier waarop een document zijn eigen defect
uitspreekt (de WF-096-producentroute van 2026-09-11; de `Onopgelost`-markering
van de geneste-aggregatiekaart; de gedropte kalendereenheidsleutel van WF-017
arm (a)). Die keuzes zijn gemaakt *terwijl* het register `diagnostics` als
non-goal had staan. Weghalen zonder vervanging haalt het kanaal weg waarlangs
een emitter vandaag zegt dat hij iets niet kon.

**Beslisvraag: V2.**

---

#### B5 — De zijkaarten van `Objecttype` worden ledenknopen

**Wire-diff.**

```json
// vóór — acht parallelle kaarten, gesleuteld op ledennaam
{ "node": "Objecttype", "naam": "Jutter",
  "attributen": { "leeftijd": {...} },
  "attribuutGranulariteit": { "leeftijd": "JAAR" },
  "attribuutTijdlijnAantal": { "leeftijd": 1 },
  "attribuutTijdlijnNaam": {}, "attribuutMeervoud": {}, "attribuutDimensies": {},
  "kenmerkSoorten": {}, "kenmerkGranulariteit": {}, "kenmerkTijdlijnAantal": {} }
// ná — één knoop per lid, met zijn eigen facetten
{ "name": "Jutter",
  "attributes": [ { "name": "leeftijd", "datatype": {...},
                    "timeline": { "granularity": "year", "count": 1 } } ] }
```

**Partijen.** `E:` alle drie. `C:` alle vier — en dit is de plek waar de
Drools-backend sinds 2026-09-12 116 van zijn 122 ledenlezingen door
ketensluitende kaartaccessors laat lopen; die accessors zijn op de kaartvorm
gebouwd. `S:` acht map-properties op `Objecttype`. `K:` **639 documenten**
dragen `attribuutGranulariteit` en `kenmerkGranulariteit`. `T:` de pin.

**Migratienota.** IR-SPEC ch. 2.2 draagt de migratienota woordelijk: "each v0
map entry folds into the corresponding member node; absent map entries become
the documented defaults below". Mechanisch, en per document lokaal.

Bij de klap vervalt **WF-034 clausule (p)** — "every key of all eight maps
MUST name a declared member, in that member's declared spelling" — met zijn
negen negatief-getuigen (`wf034-wees-*` ×8 plus
`wf034-zijtabel-kapitalisatiewees`). Dat is de bedoeling: die regel bestaat
alleen omdat de v1-knoopvorm de overtreding onschrijfbaar maakt, en hij zegt
dat zelf ("Until that reification the side-maps are fenced where the v1 node
shape fences them by construction").

Twee armen van **WF-059** (`attribuutGranulariteit: null` en
`kenmerkGranulariteit: null`, geland 2026-09-10 en -09-11) vervallen om
dezelfde reden.

**Beslisvraag: geen.** Richting en migratienota staan vast in IR-SPEC ch. 2.2.

---

### Familie B — de globale hernoeming

#### B6 — Nederlands → Engels over knopen en velden

**Wire-diff.**

```json
// vóór
{ "node": "Vergelijking", "op": "GROTER", "links": {...}, "rechts": {...}, "bron": "…" }
// ná
{ "node": "Comparison", "op": "greater", "left": {...}, "right": {...}, "source": {...} }
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` het hele schema. `K:` **650
van 650**. `T:` de pin.

**Gemeten omvang.** Het schema telt **80 knoopdiscriminanten**, waarvan **56
Nederlands** en 24 al Engels (de 5b/5c-kernuitbreidingen: `Let`, `Match`,
`Fold`, `StructType`, `SumType`, `FunctionDef`, …). Het telt **193
propertynamen**, waarvan **155 Nederlands** en 38 al Engels of neutraal
(`node`, `type`, `value`, `name`, `kind`, `body`, `arms`, `scrutinee`, …).

**De wire is dus vandaag tweetalig**, en dat is geen ongeluk maar het gevolg van
de wire-entry-discipline: elke v1-uitbreiding kwam binnen onder de v1-conventie
(Engels), elke gespiegelde v0-vorm hield zijn v0-naam. Dat maakt B6 ongewoon:
het is niet "hernoem alles", het is "maak af wat al half gebeurd is".

**Migratienota.** Volledig mechanisch, en het enige item op deze lijst waarvoor
een **naamtabel** het hele werk is. PLAN §9 besluit 6 en de Conventions van
IR-SPEC leggen de richting vast ("Metamodel node and field names are English;
each node lists its v0 name"), en IR-SPEC draagt de doelnamen al: elke knoop in
ch. 2–4 staat onder zijn Engelse naam met een `v0 name:`-regel eronder. **De
tabel hoeft dus niet bedacht te worden, hij hoeft alleen geëxtraheerd.**

Eén architectonisch feit maakt dit goedkoper dan het lijkt: `backends/drools`
compileert de recordtypen van de frontend mee (`build-helper` voegt
`frontends/regelspraak` model/ir/kern toe als bronmap, met de expliciete reden
"the shared IR shape lives in frontends/regelspraak/ir — the card's 'no second
copy'"). Emitter en Drools-loader delen dus **één** verzameling Java-records.
Een hernoeming is daar één bewerking, niet twee.

Hetzelfde feit maakt het duurder aan de andere kant: die recordverzameling is
precies wat de toolchain vendort op `SPEC_REV`. Een hernoeming is daarmee per
constructie een her-vendor plus een pin-bump.

**Beslisvraag: geen over het *of*; wel over de *fasering* — zie V2.**

---

### Familie C — de bonenmodel-spiegelbagage

*Dit is de familie die IR-SPEC's Method met "`zetVeld`/`joinVeld` and kin"
bedoelt. §2.2 hierboven corrigeert de aanname dat de symbolische opvolgers de
enige gelezen vorm zijn: voor `ref` en clingo klopt dat op de feitcreatie, voor
`scopeVeld` klopt het nergens, en voor de Drools-backend klopt het in geen enkel
geval.*

#### B7 — De feitcreatie-spiegelvelden

**Wire-diff** (echt fragment uit `corpus/golden/anchor/jutterswet-erflijn`):

```json
// vóór
{ "node": "ResolvedFeitCreatie", "naam": "erf de directe opvolger",
  "anker": "Wachter", "doel": "Wachter",
  "zetVeld": "erfvolger", "joinVeld": "opvolger",       // ← bonenvelden
  "tussenstappen": [],
  "feittype": "erflijn", "ankerRol": "erflater", "doelRol": "erfvolger",
  "joinRol": { "node": "RolBinding", "feittype": "opvolging", "rol": "opvolger" } }
// ná
{ "name": "erf de directe opvolger",
  "factType": "erflijn", "anchorRole": "erflater", "targetRole": "erfvolger",
  "anchor": "Wachter",
  "path": [ { "factType": "opvolging", "role": "opvolger" } ] }
```

Vervalt: `zetVeld`, `joinVeld`, en per `Tussenstap` de velden `veld`,
`viaOuderVeld` en `type`.

**Partijen.** `E:` RegelSpraak (schrijft alle vijf). `C:` de verifier kruist ze
onder WF-028 en WF-034 (m); **`backends/drools` voert ze uit** — de
exec-tak schrijft de bonenproperty uit `zetVeld`, joint op `joinVeld` en hopt
via `Tussenstap.veld`/`viaOuderVeld`; `ref` en clingo lopen uitsluitend de
symbolische vorm. `S:` vijf properties over twee `$def`s. `K:` 16 documenten
dragen `zetVeld`/`joinVeld` (4 golden, 1 rood, 10 negatief, 1 toka); 13 dragen
`veld`/`viaOuderVeld` (idem verdeling). `T:` de pin.

**Migratienota.** IR-SPEC ch. 3.4 draagt hem: "v0's `zetVeld`/`joinVeld`/`veld`
are derivable from the role bindings by the backend's own role→field mapping;
the derivation moves down into the backend, out of the IR (the Phase-3 forcing
move)."

Het werk zit niet in het corpus (16 documenten) maar in `backends/drools`: de
afleiding rol→bonenveld moet dáár komen te staan. Die afleiding bestaat er al —
`FactModelGenerator` leidt per rol het scalaire referentieveld af, en sinds de
rolvelden-kaart van 2026-09-18 doet hij dat per ROL in plaats van per typepaar.
De feitcreatie-lezers moeten erop worden aangesloten. Het is precies de
"forcing move" die de spec voorspelde, en hij is zes maanden na dato nog steeds
de juiste.

**Bij de klap vervalt:** WF-028's bonenveld-alleen-arm en WF-034 clausule (m)
(vier velden aanwezig, een gedeclareerde rol noemend, aan de opslagzijde, in de
gedeclareerde spelling). Zes negatief-getuigen (`wf034-feitcreatie-*`) plus drie
`wf028-*` verliezen hun onderwerp.

**Beslisvraag: geen over het *of*; de volgorde staat in §6.**

---

#### B8 — `scopeVeld` verdwijnt; `scopeRol` wordt verplicht

**Wire-diff.**

```json
// vóór
{ "node": "ResolvedUniciteit", "objecttype": "Vondst", "sleutels": ["nummer"],
  "scopeVeld": "strand",
  "scopeRol": { "node": "RolBinding", "feittype": "strandligging", "rol": "strand" } }
// ná
{ "objectType": "Vondst", "keys": ["nummer"],
  "scope": { "factType": "strandligging", "role": "strand" } }
```

**Partijen.** `E:` RegelSpraak. `C:` **alle vier gebruiken het inhoudelijk** —
de verifier onder WF-054, `ref` en clingo als bindingloze terugval, beide
Drools-takken rechtstreeks als gezag. `S:` één property. `K:` 19 documenten (5
golden, 2 rood, 12 negatief, 0 toka). `T:` de pin.

**Migratienota.** IR-SPEC ch. 3.6 zegt "derivable via the backend's role→field
mapping" — dezelfde beweging als B7, en dus dezelfde voorwaarde: de afleiding
moet eerst in `backends/drools` staan.

Het verschil met B7 is dat hier ook `ref` en clingo iets kwijtraken: hun
terugval leest vandaag `scopeVeld` als rolnaam wanneer `scopeRol` ontbreekt. Als
`scopeRol` verplicht wordt is er niets meer terug te vallen — en dat is de
bedoeling. Drie `wf054-*`-getuigen verliezen hun onderwerp.

**Beslisvraag: geen.**

---

#### B9 — De rollidmaatschapsknopen smelten samen

**Wire-diff.**

```json
// vóór — twee knopen
{ "node": "Rollidmaatschap", "rol": "wachters", "anderTeFiller": "Post", "negatie": false }
{ "node": "VerreRollidmaatschap", "rol": "wachter", "filler": "Jutter",
  "backref": "post", "negatie": false }
// ná — één
{ "node": "RoleMembership", "factType": "postbezetting", "role": "wachter", "negated": false }
```

**Partijen.** `E:` RegelSpraak. `C:` alle vier (de Drools-backend leest
`backref` als bonenproperty; `ref` weigert de verre vorm bij evaluatie). `S:`
twee `$def`s worden één; de properties `anderTeFiller`, `filler`, `backref`
vervallen. `K:` `Rollidmaatschap` in 8 documenten (2 golden, 1 rood, 3 negatief,
2 toka); **`VerreRollidmaatschap` in 0 documenten**, `backref` en `filler`
eveneens in 0. `T:` de pin.

**Migratienota.** IR-SPEC ch. 4.5 draagt hem: "near-side → the named role;
far-side → the subject-side role of the relation". De nabije vorm noemt vandaag
een objecttype (`anderTeFiller`) waar de v1-vorm een feittype plus rol noemt;
die afleiding gaat langs dezelfde weg als B7.

**Twee dingen die deze delta apart maken.** Ten eerste vervalt met de
samensmelting ook de **topniveaubeperking** van de verre vorm — een
Drools-patroonartefact dat CONSTRUCTS rij 5 al in 2026-08 als leak aanwees en
dat, in de woorden van diezelfde rij, "has no translation target" in de
ASP-vertaling. Ten tweede: de verre vorm heeft **nul corpusdragers in alle
zones**, en IR-SPEC ch. 4.5 zegt daar uitdrukkelijk over dat `filler` en
`backref` daarom ongekruist blijven — "no document in any zone carries one, so a
clause there would have no witness". Een knoop zonder getuige samenvoegen is de
goedkoopste delta op deze lijst, en tegelijk de riskantste: er is niets dat de
samenvoeging betrapt als hij fout is. Zie **V4**.

**Beslisvraag: V4.**

---

#### B10 — `Kenmerk.eigenaar` verdwijnt

**Wire-diff.**

```json
// vóór
{ "node": "Kenmerk", "kenmerk": "bevoegd", "eigenaar": "Jutter", "negatie": false }
// ná
{ "node": "CharacteristicCheck", "characteristic": "bevoegd", "negated": false }
```

**Partijen.** `E:` RegelSpraak. `C:` alle vier (de verifier onder WF-034 (q) en
WF-100; `ref` en de ASP-backend weigeren waar de eigenaar niet het type van het
gelezen object is). `S:` één property. `K:` 8 documenten (1 golden, 7 negatief).
`T:` de pin.

**Migratienota.** Het veld is een Drools-aanhechtingshint, alleen gezet op
feitcreatie-guards. IR-SPEC ch. 4.5 noemt hem "dropped" in v1.

De delta is sinds 2026-09-10 **scherper dan hij was**, en dat is winst: WF-100
maakte het veld GEZAGHEBBEND zolang het op de wire staat. In de woorden van
CONSTRUCTS §2.2: "wat er in v1 verdwijnt is nu een veld met een uitgeschreven
betekenis, geen leak met een onbekende". Weghalen betekent dus: het onderwerp is
altijd de sleutel. Dat is precies de lezing die vóór WF-100 stilzwijgend gold.

Eén ding blijft open en is gekaart, niet hier op te lossen: de OBJECT-helft —
of een gastheer-voorwaarde over de *instantie* van het genoemde type gaat, wat
het bonenmodel wél doet (`backlog/new/20260910-eigenaar-gastheer-object.md`).
**Dat kaartje moet vóór B10 landen**, want als het antwoord "ja" is, is het veld
geen hint maar betekenis, en dan verdwijnt het niet.

**Beslisvraag: geen** — maar de volgordeafhankelijkheid staat in §6.

---

#### B11 — De navigatievelden van `AttribuutRef`

**Wire-diff** (echt fragment uit `corpus/golden/anchor/jutterswet-kustwacht`):

```json
// vóór — drie geschreven-naamvelden, een vlag, plus de symbolische opvolger
{ "node": "AttribuutRef", "attribuut": "gevarenklasse", "objecttype": "Kustvak",
  "onderwerpBron": "zijn post", "navigatieRol": "post",
  "navigatiePad": [ "post", "kustvak" ], "set": false,
  "navigatieBindingen": [ { "node": "RolBinding", "feittype": "postbezetting", "rol": "post" },
                          { "node": "RolBinding", "feittype": "kustligging", "rol": "kustvak" } ],
  "type": { "node": "Numeriek", ... }, "bron": "de gevarenklasse van het kustvak van zijn post" }
// ná
{ "node": "AttributeReference", "attribute": "gevarenklasse", "subjectType": "Kustvak",
  "path": [ { "factType": "postbezetting", "role": "post" },
            { "factType": "kustligging", "role": "kustvak" } ],
  "source": { ... } }
```

Vervalt: `onderwerpBron`, `navigatieRol`, `navigatiePad`, `set` — en `type`
(dat is B13).

**Partijen.** `E:` alle drie. `C:` de verifier (WF-053, WF-096), `ref` en clingo
lopen de bindingen; **`backends/drools` negeert `navigatieBindingen` en leidt de
navigatie zelf af uit `navigatieRol`/`navigatiePad`** — zie de
loader-javadoc in §2.2. `S:` vier properties. `K:` `navigatiePad` in **537
documenten**, `onderwerpBron` in 504, `navigatieRol` in 43, `set: true` in 75.
`T:` de pin.

**Migratienota.** IR-SPEC ch. 4.1: "the v1 unification drops the written-name
fields the bindings supersede". Mechanisch voor `ref`, clingo en de verifier —
die lezen de bindingen al.

**Voor `backends/drools` is het geen migratie maar een port.** De transpiler
moet van "her-afleiden uit de geschreven naam" naar "de binding lezen". Dat is
de grootste enkele brok werk van het hele programma, en het moet vóór de klap,
niet erbij.

`set` verdient een eigen zin. WF-096 (2026-09-09) beperkte de vlag al tot de
vouwbron; WF-093 hangt de telwrat eraan op. In v1 is de collectiviteit van de
vouwbron een eigenschap van de *positie*, niet van een vlag — dus de vlag gaat
weg en de twee regels gaan mee. Drie `wf096-*`-getuigen en vier
`wf093-*`-getuigen verliezen hun onderwerp.

**Beslisvraag: geen over het *of*; de volgorde staat in §6.**

---

#### B12 — WF-048 vervalt: de binding wordt verplicht

**Wire-diff.**

```json
// vóór — welgevormd zonder binding, mits precies één feittype het typenpaar relateert
{ "node": "Functie", "kind": "SOM", "elementObjecttype": "Vondst", "argumenten": [...] }
// ná — de binding is verplicht
{ "node": "Aggregation", "kind": "sum", "elementType": "Vondst",
  "sourcePath": [ { "factType": "strandvondst", "role": "vondsten" } ], ... }
```

**Partijen.** `C:` verifier (WF-047/WF-048), `ref`, clingo, Drools — alle vier
dragen vandaag een bindingloze afleiding. `S:` `bronRol`/`bronPad` en
`verbindendeRol` worden required. `K:` **54 documenten dragen een bindingloze
vouw** (2 anchor, 13 golden, 1 rood, 34 negatief, 4 toka) en **6 een bindingloze
verdeling** (alle negatief). In de groene zones: **16 documenten** moeten een
binding krijgen. `T:` de pin.

**Migratienota.** IR-SPEC regel 1536: "In v1 the binding becomes required and
WF-048 retires with the format bump."

De 16 groene documenten kunnen mechanisch worden voorzien: de afleiding die
WF-048 vandaag uitvoert (precies één feittype over het typenpaar) is dezelfde
afleiding die de binding uitrekent. Voor de `bron.rgs`-dragers eronder doet de
emitter het al — hij schrijft `bronRol` sinds de eerste pin-bump waar hij
uniek afleidbaar is.

Eén waarschuwing die de kaart van 2026-09-11 al gaf en die hier blijft staan:
de bindingloze vorm *verbergt* een fout in plaats van hem weg te nemen. Er is
gemeten dat een bindingloze vouw over een uniek verbonden paar verifier-schoon
is en dat elke uitvoerder dezelfde verkeerde relatie opnieuw afleidt. B12 sluit
dat gat structureel.

**Beslisvraag: geen.**

---

### Familie D — de typeannotatie

#### B13 — De referentiepositie-`type` verlaat de wire

**Wire-diff.**

```json
// vóór
{ "node": "AttribuutRef", "attribuut": "leeftijd", "objecttype": "Jutter",
  "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
            "teken": "ONBEPAALD", "eenheid": null } }
// ná — de consument leest de declaratie
{ "node": "AttributeReference", "attribute": "leeftijd", "subjectType": "Jutter" }
```

**Partijen.** `E:` alle drie. `C:` alle vier — en **gate 13 bewijst sinds
2026-09-04 dat geen van de vier de annotatie leest**: elke eenheiddragende
annotatie op referentie- en composietposities wordt systematisch verdraaid, en
`ref`, clingo én beide Drools-transpilertakken moeten exact dezelfde
uitkomsten geven. `S:` het `type`-veld op elke referentieknoop. `K:` een
`type`-annotatie staat 4519 keer in 567 documenten; gate 13 verdraait er 74 over
15 goldens (de vloer). `T:` de pin.

**Migratienota.** CONSTRUCTS rij 1 formuleert het argument in één zin: "a copy
that cannot be serialized cannot drift". De machinale voorbereiding is af — gate
13 is er precies voor gebouwd, en `RUNTIME-SEMANTICS.md` ch. 2.10 (de
consumentenclausule) is de wet die in de plaats komt.

**Wat er tegelijk vervalt is groot en moet expliciet gewogen worden.** De hele
identiteitsfamilie bestaat alleen omdat de herhaling op de wire staat:

- **WF-091** (referentietype-identiteit, 27 negatief-getuigen) — vervalt geheel;
- **WF-089** (domeinidentiteit leeshelft, 15 getuigen) en **WF-090**
  (structdomein, 10) — vervallen aan de leeskant;
- **WF-078** (bindersoort, 1) en **WF-088** (enumeratie-use-site, 4) — vervallen;
- **WF-092** (eenheidswire-vorm, 3) blijft, want die zit op de *declaratie*;
- **WF-007** (getalprecisie) verliest zijn expressiewandelarm.

Dat is **57 negatief-getuigen** die hun onderwerp verliezen — het
grootste enkele blok van de klap. Dat is geen verlies aan dekking: die regels
bewaken een kopie die er niet meer is. Het is wel een groot blok
corpusonderhoud, en het is onomkeerbaar.

**Er is een tweede gevolg dat bijna gratis is en dat de volgorde bepaalt.** Van
de 97 `Onbekend`-voorkomens op de wire staan er **90 op `AttribuutRef.type`**.
In de groene zones (9 anchor-, 22 golden- en 3 rood-documenten) is dat per
WF-093 en WF-034 arm (r) uitsluitend de eenargumentige AANTAL-telbron — de enige
nog toegelaten sentinelpositie; in de negatief-zone zijn het de
`wf034-sentinel-*`-getuigen die de vorm juist moeten afwijzen. Als de
referentie-`type` de wire verlaat, verdwijnen die 90 met hem mee, en houdt B16
(`Onbekend` uit de datatypesom) nog maar **zeven** voorkomens over: zes in
negatief-documenten (twee `SumType`-payloads, één `ListType`-element, één
`StructType`-datatype, één attribuutdeclaratie en één `Parameter`-datatype) en
één `Binair.type` in `toka/casus/ir.json` — de toka-45-holdout van besluit 16Y.
**B13 vóór B16 maakt B16 bijna kosteloos.**

**Beslisvraag: V5** (hoe ver gaat het — alleen eenheiddragend, of alle
referentieposities?).

---

### Familie E — het typesysteem

#### B14 — `Percentage` lost op in `Numeriek` + `%`-eenheid

**Wire-diff.**

```json
// vóór
{ "node": "Percentage", "geheel": false, "decimalen": 2, "eenheid": null }
// ná
{ "node": "Number", "whole": false, "decimals": 2, "sign": "unspecified",
  "unit": { "name": "%" }, "presentation": "percentage" }
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` één `$def` verdwijnt uit de
datatype-unie. `K:` 47 documenten (2 golden, 1 anchor, 1 rood, 13 negatief,
**30 toka**). `T:` de pin.

**Migratienota.** IR-SPEC ch. 2.4 draagt hem letterlijk: `Percentage(whole,
decimals, unit)` → `Number(whole, decimals, unspecified, unit)` + tag. De
inhoudelijke beslissing is al genomen en geratificeerd (2026-08-29, Q10): de
convertibiliteitsklasse van `%` is het singleton `{%}`, en de ÷100-brug is
gereserveerd voor `PERCENTAGE_VAN`, dat bovendialect-suiker wordt met de
gespecificeerde verlaging `MAAL(strip%(P)÷100, V)`.

Let op de verdeling: **30 van de 47 dragers zitten in de toka-zone**. Dat is
het grootste toka-aandeel van alle deltas en weegt mee in §5.

**Beslisvraag: geen.**

---

#### B15 — `DatumInDagen` en `DatumTijd` versmelten

**Wire-diff.**

```json
// vóór — twee knopen
{ "node": "DatumInDagen" }
{ "node": "DatumTijd" }
// ná — één, geparametriseerd
{ "node": "Date", "precision": "day" }
{ "node": "Date", "precision": "millisecond" }
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` twee `$def`s worden één.
`K:` `DatumInDagen` in 81 documenten (4 anchor, 10 golden, 2 rood, 35 negatief,
30 toka), `DatumTijd` in 42 (3 golden, 9 negatief, 30 toka). `T:` de pin.

**Migratienota.** "*Migration:* mechanical. Mixed-precision date use remains a
rule-layer typing error, as in v0" (IR-SPEC ch. 2.4).

Dat laatste is sinds 2026-09-05 met een regel bekrachtigd die de versmelting
*moeilijker* maakt en dat expliciet zegt: **WF-036** eist één
datumrepresentatie per vergelijking, want "`DatumInDagen` and `DatumTijd` share
no order". CONSTRUCTS §2.2 trekt daar de conclusie uit: "A v1 unification under
typed ordering must therefore unify ACROSS the representation seam, not beneath
it." Met andere woorden: de versmelting mag de precisie niet wegabstraheren,
want dan verdwijnt de naad waarop WF-036 staat. Dat is precies waarom de
v1-vorm `Date(precision)` is en niet `Date`.

**Beslisvraag: geen.**

---

#### B16 — `Onbekend` verlaat de datatypesom

**Wire-diff.**

```json
// vóór
{ "node": "Onbekend", "reden": "kon 'aantal aangemelde vondsten' niet typeren" }
// ná — bestaat niet; WF-003 weigert het document
```

**Partijen.** `E:` RegelSpraak (de enige die hem produceert). `C:` alle vier.
`S:` één `$def` verdwijnt. `K:` 97 voorkomens in 71 documenten — **maar zie de
volgordevondst bij B13**: 90 daarvan staan op `AttribuutRef.type`. Ná B13 blijven
er zeven over, waarvan zes in negatief-documenten en één in de toka-casus.
`T:` de pin.

**Migratienota.** IR-SPEC ch. 2.4: "v0 `Onbekend(reden)` is removed: v1 fails
fast at verification (WF-003) instead of carrying an unknown-type sentinel."
Het sentinel-hek is in twee golven al bijna dicht: WF-034 golf 7 arm (r)
(2026-09-06) maakte het sentinel ill-formed op **elke** leespositie behalve de
WF-093-telbron, en WF-093 eist daar `AANTAL`.

De enige nog levende producentroute is de telwrat zelf. Als B13 landt is die
positie er niet meer; als B13 *niet* landt, moet er beslist worden wat de
telbron dan wél als type draagt. Dat is **V6**.

De `Binair.type`-drager in `toka/casus/ir.json` is de gedocumenteerde
16Y-divergentie: de toolchain typeert daar niet, uchar wel. Die drager verdwijnt
niet met een migratie — hij is precies het punt waarop uchar-v1 van de toolchain
afwijkt.

**Beslisvraag: V6.**

---

#### B17 — `factorNaarCanoniek` wordt exact; de canonieke-eenheid-wire

**Wire-diff.**

```json
// vóór — één vooraf afgeronde ratio
{ "node": "Eenheid", "naam": "t", "canoniek": "kg", "factorNaarCanoniek": "1000" }
// ná — een exacte rationaal
{ "name": "t", "canonical": "kg", "factorToCanonical": { "num": 1000, "den": 1 } }
```

**Partijen.** `E:` alle drie. `C:` alle vier — clingo het zwaarst, want daar
hangt de integer-codering van de grounder aan. `S:` één property verandert van
type. `K:` **143 documenten, 2777 voorkomens** (2 anchor, 28 golden, 83
negatief, 30 toka). `T:` de pin.

**Migratienota.** IR-SPEC ch. 2.5: "v1 stores factors as true rationals and
specifies only the exact restatement form; the pre-rounded single-ratio form is
dropped." Het geratificeerde besluit (2026-08-29, Q9) hangt er een tweede,
grotere stap aan:

> source units stay on the wire; a specified normalisation lowering pass
> canonicalises to each stelsel's FINEST base unit … the canonical-unit wire is
> the recorded eventual v1 form; `factorToCanonical` migrates to the exact
> `{num,den}` at that epoch.

Twee dingen dus, en ze hoeven niet tegelijk: (a) de factorvorm exact maken, (b)
de wire zelf canonicaliseren. (a) is mechanisch; (b) is een
betekenisverschuiving die elke eenheiddragende waarde op de wire beweegt — ook
in de **506 casusbestanden die `number` dragen**. Zie **V7**.

Eén gemeten divergentie hangt eraan en is al gekaart: de kruis-stelsel-grens
waar `ref` en Drools 10000 rekenen en de ASP-backend weigert te vertalen
(`backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`). Die kaart hoort
vóór (b), niet erna.

**Beslisvraag: V7.**

---

#### B18 — `Parameter` krijgt de volledige tijdlijnfacet

**Wire-diff.**

```json
// vóór — alleen een kale granulariteit
{ "node": "Parameter", "naam": "tarief", "granulariteit": "JAAR" }
// ná
{ "name": "tarief", "timeline": { "granularity": "year", "count": 1, "name": null } }
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` één property wordt een
object. `K:` 134 documenten dragen `parameters` (664 parameters). `T:` de pin.

**Migratienota.** "*Migration:* a v0 granularity becomes `{granularity,
count: 1}`; no v0 parameter carries a timeline name" (IR-SPEC ch. 2.10). Volledig
mechanisch en verliesloos — er is niets om te kiezen, want v0 kán de extra
informatie niet dragen.

De reden dat dit toch brekend is en niet additief: de verlaging maakt `Parameter`
en `Attribuut` één facetvorm (`TimelineFacet`), en die deelt WF-005 met de
attribuutkant. Een parameter die vandaag een kale granulariteit draagt is na de
klap ongeldig.

**Beslisvraag: geen.**

---

### Familie F — de regellaag

#### B19 — De versie-afvlakking keert om

**Wire-diff.**

```json
// vóór — één entry per versie, met " #i"-achtervoegsel in de naam
"resolvedRegels": [
  { "node": "ResolvedRegel", "naam": "jutheffing #1", "geldigheid": {...}, ... },
  { "node": "ResolvedRegel", "naam": "jutheffing #2", "geldigheid": {...}, ... } ]
// ná — één vorm, expliciete versies
"rules": [ { "name": "jutheffing",
             "versions": [ { "validity": {...}, ... }, { "validity": {...}, ... } ] } ]
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` de vorm van zes
`Resolved*`-lijsten. `K:` `ResolvedRegel` in **501 documenten** (922 regels);
het `" #i"`-achtervoegsel zelf in slechts **4 documenten** — vrijwel geen
corpusdocument heeft meer dan één versie per vorm. `T:` de pin.

**Migratienota.** IR-SPEC ch. 3 draagt hem: "group v0 entries of any form by
stripping the suffix; a single-version rule becomes one version." Het
achtervoegsel rijdt op *elke* vanuit een versieregel gebouwde vorm — verdelingen,
creaties, consistenties, uniciteiten — maar niet op dagsoortdefinities,
startpunten of groepen.

De omvang is misleidend klein in het corpus (4 documenten met een echt
achtervoegsel) en groot in de code: **elke** benoemde vorm krijgt een extra
nestlaag, en elke consument moet erdoorheen. Vergelijk met **WF-107** (geland
2026-09-12), die al eist dat elke benoemde vorm een niet-blanco naam draagt op
precies dezelfde lijst — die lijst is dus al gedefinieerd en gecontroleerd, wat
B19 aanzienlijk goedkoper maakt dan een jaar geleden.

**Beslisvraag: geen.**

---

#### B20 — `Soort` klapt in

**Wire-diff.**

```json
// vóór
{ "node": "ResolvedRegel", "soort": "GELIJKSTELLING", ... }
{ "node": "ResolvedRegel", "soort": "INITIALISATIE", ... }
{ "node": "ResolvedRegel", "soort": "KENMERKTOEKENNING", "doel": "bevoegd", ... }
// ná
{ "presentation": "equation", ... }                       // GELIJKSTELLING/BEREKENING
{ "initialisation": true, ... }                           // INITIALISATIE
{ "target": { "kind": "characteristic", "name": "bevoegd" }, ... }  // KENMERKTOEKENNING
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` één enum verdwijnt, drie
velden komen ervoor in de plaats. `K:` `BEREKENING` in 289 documenten,
`GELIJKSTELLING` in 147, `KENMERKTOEKENNING` in 141, `INITIALISATIE` in 4.
`T:` de pin.

**Migratienota.** PLAN §9 besluit 4 is de bron: "absent a behavioural
difference, one semantic node with a presentation tag for verbalisation; two
nodes without a meaning difference are surface syntax sneaking into the core."
IR-SPEC ch. 3.1 werkt het uit, inclusief het punt dat KENMERKTOEKENNING **geen**
aparte vlag wordt maar volgt uit de ledensoort van het doel.

Dit is de enige delta waar het schema iets *wint*: de drie velden zijn
onafhankelijk controleerbaar, waar één enum vier combinaties uitsluit die
vandaag alleen door proza zijn uitgesloten.

**Beslisvraag: geen.**

---

#### B21 — `Functie` splitst in aggregatie en builtin-aanroep

**Wire-diff.**

```json
// vóór — één overladen knoop
{ "node": "Functie", "kind": "SOM", "elementObjecttype": "Vondst",
  "bronRol": {...}, "filter": {...}, "leegwaarde": {...}, "dimensieSelectie": null,
  "argumenten": [...] }
{ "node": "Functie", "kind": "WORTEL", "elementObjecttype": null,
  "bronRol": null, "filter": null, "leegwaarde": null, "dimensieSelectie": null,
  "argumenten": [...] }
// ná — twee knopen, elk met alleen zijn eigen velden
{ "node": "Aggregation", "kind": "sum", "elementType": "Vondst",
  "sourcePath": [...], "filter": {...}, "emptyValue": {...} }
{ "node": "BuiltinCall", "builtin": "sqrt", "arguments": [...] }
```

**Partijen.** `E:` alle drie. `C:` alle vier. `S:` één `$def` wordt twee.
`K:` `Functie` in 170 documenten (285 voorkomens). `T:` de pin.

**Migratienota.** IR-SPEC ch. 4.3 draagt de splitsing; CONSTRUCTS rij 1 noemt de
velden per kant. De winst is dat vijf velden die vandaag op elke builtin `null`
staan er niet meer zijn — en dat is niet cosmetisch: WF-083 (c) formuleert het
bezwaar als "a field nobody reads is a field that can say anything", en WF-108
(e) paste datzelfde argument toe op de drempel van `Kwantorvouw` (die er
uitdrukkelijk *niet* staat onder ALLE/GEEN, waar `Samengesteld` hem altijd
schrijft en een nul schrijft die niemand leest).

Bij de splitsing verdwijnt ook `Methode.COMPLEX` definitief — WF-024 weigert hem
al, maar `toka/casus/ir.json` draagt er één, omdat de toka-zone niet
verifier-gegatet is. Dat is een van de twee plekken waar een pure regeneratie van
die zone naar v1 niet lukt; zie §5.

**Beslisvraag: geen.**

---

### Familie G — de expressielaag

#### B22 — Literalen worden getypeerde waarden

Dit is het item met de meeste al-voorgesorteerde deelstappen en de enige waar
het corpus vandaag **twee spellingen van dezelfde waarde** draagt.

**Wire-diff.**

```json
// vóór — een lexeem dat elke consument parseert
{ "node": "Literal", "waarde": "12.50", "type": { "node": "Numeriek", ... } }
// ná — een getypeerde waarde
{ "node": "Literal", "value": { "number": "12.50" }, "type": { ... } }
```

**Stand van zaken per literaalsoort** (IR-SPEC ch. 4.1, herzien 2026-09-18):

| Soort | Lexeemvorm vandaag | Gehekt door | Corpusdragers |
|---|---|---|---|
| getal | kaal getal, eenheid in het type | WF-081 | 632 literalen |
| datum | kale ISO `yyyy-mm-dd` | WF-084 | 53 |
| tekst | de **geparste** string (sinds 2026-09-18) | emittergetuige, gate 10/11 | 75 |
| booleaan | `waar`/`onwaar` | — | 201 |
| **datum-tijd** | **canoniek ISO óf de bronsnippet** | WF-095 (laat beide toe) | 7 |
| **enumeratiewaarde** | **kaal label óf `'label'`** | niets | 17 |

**B22a — het DatumTijd-lexeem.** WF-095 (2026-09-06) laat twee spellingen toe:
canoniek `yyyy-mm-ddThh:mm:ss[.fff]` en de dag-eerst-bronsnippet. De regel zegt
zelf waarom — "unlike WF-084 the rule admits the snippet, because the emitter
still writes it into `waarde` and `bron` (canonicalisation is its own card)".
Gemeten: de RegelSpraak-emitter schrijft inderdaad de snippet
(`ExpressieBouwer`, de dedato-tak met tijdcomponent), terwijl diezelfde methode
één regel eronder voor een kale datum wél de ISO-vorm schrijft.
**Corpusdragers van de snippetvorm: vijf, alle vijf in `corpus/negatief/wf095-*`
— en die zijn er om afgewezen te worden.** De enige golden met een
`DatumTijd`-literal (`jutterswet-meldstip`) draagt de canonieke vorm en is
handgeschreven. De emitterkant heeft dus **nul goldengetuigen**.
→ Versmallen kost één emitterregel, één WF-095-arm, vijf negatief-getuigen
herschrijven, en **nul goldenregeneratie**. Dit is de goedkoopste brekende stap
van de hele lijst.

**B22b — de enumeratiewaarde-delimiters.** Dit is een levend defect, gemeten op
deze kop, en het is de exacte tweelingbroer van wat de tekstlexeem-kaart op
2026-09-18 sloot:

- de RegelSpraak-emitter schrijft de **bronsnippet mét delimiters** (`'Ruimzee'`)
  — `ExpressieBouwer`, de `ENUM_LITERAL`-tak;
- de Catala-plugin en elke handgeschreven wire schrijven het **kale label**
  (`Ruimzee`) — gemeten over alle 650 documenten: **nul dragen de
  gedelimiteerde vorm**;
- `ref` leest het lexeem **verbatim** als enumwaarde (`Some (Enumeratie _) ->
  Value.Enum lex`), en alle vier de consumenten vergelijken enumwaarden
  byte-voor-byte.

Twee producenten, twee spellingen, verbatim vergelijking, **en geen enkele
getuige** — want geen `bron.rgs`-golden draagt een enumeratieliteral.
IR-SPEC ch. 4.1 beschrijft de gedelimiteerde vorm als de wire-vorm ("an
enumeratiewaarde keeps its `'…'` delimiters, which readers strip"), maar de
genoemde stripper (`Literalen.enumLabel`) zit in de **frontend**, niet in een
consument. Die zin klopt dus niet met wat de consumenten doen.
→ Dit is geen v1-item maar een v0-defect. Het hoort **vóór** de klap gesloten,
precies zoals de tekstlexeem-kaart dat deed: canonicaliseer bij emissie op het
kale label, borg met een emittergetuige, en voeg één `bron.rgs`-golden met een
enumeratieliteral toe die de zaak machinaal vasthoudt. Zie **V8**.

**B22c — de volle overgang naar getypeerde waarden.** Van "een string die elke
consument parseert" naar "een JSON-waarde van de juiste soort". Dat raakt alle
992 literalen in 368 documenten, alle vier de consumenten en — via de
waardesoorten `number`/`date`/`dateTime`/`enumeration` — een deel van de 566
casusbestanden. Zie **V9**.

**Partijen (B22 geheel).** `E:` alle drie. `C:` alle vier. `S:` het
`waarde`-veld van `Literal`, plus het `pattern` dat WF-095's eerste laag dekt.
`K:` 368 documenten dragen literalen. `T:` de pin.

**Beslisvragen: V8, V9.**

---

#### B23 — `Constante` splitst

**Wire-diff.**

```json
// vóór — één grabbelknoop
{ "node": "Constante", "naam": "Rekendatum", "type": {...} }
{ "node": "Constante", "naam": "de dag", "type": {...} }
{ "node": "Constante", "naam": "hij", "type": { "node": "Onbekend", ... } }
// ná
{ "node": "ContextValue", "name": "evaluationDate" }
{ "node": "BoundDay" }            // óf een ContextValue — zie V10
// "hij" bestaat niet; de frontend lost hem op of weigert luid
```

**Partijen.** `E:` RegelSpraak (de enige producent). `C:` alle vier. `S:` één
`$def`. `K:` 10 documenten (25 voorkomens): `de dag` in 6, `Rekendatum` in 3,
`hij` in **0**. `T:` de pin.

**Migratienota.** WF-098 (2026-09-10) heeft de naamverzameling al gesloten tot
precies drie, byte-exact gekruist. Dat maakt de splitsing mechanisch: er zijn
geen onbekende namen meer om te verdelen.

Twee dingen die de splitsing *niet* beslist en die IR-SPEC ch. 4.1 expliciet
openlaat. Ten eerste: `de dag` is semantisch de **gebonden dag van een
dagsoortdefinitie**, geen sessiewaarde — en of v1 hem een eigen knoopsoort geeft
"is an open point, deliberately not decided". Ten tweede: `RUNTIME-SEMANTICS.md`
ch. 8 draagt zijn looptijdbetekenis **niet** (de kaart
`backlog/new/20260910-dagsoort-semantiek.md` staat daarvoor open). Een knoop
splitsen naar een vorm waarvan de semantiek nog niet is opgeschreven is de
verkeerde volgorde.

`Rekenjaar` is toegelaten-en-onuitgevoerd: geen document draagt hem en `ref`
faalt erop bij naam. Dat is de eerlijke stand en hoeft niet te veranderen.

**Beslisvraag: V10.**

---

#### B24 — `Onopgelost` krijgt een gestructureerde reden

**Wire-diff.**

```json
// vóór
{ "node": "Onopgelost", "type": { "node": "Onbekend", "reden": "…" }, "bron": "…" }
// ná
{ "node": "Unresolved", "reason": { "kind": "specUnderdetermined", "detail": "…" },
  "source": { ... } }
```

**Partijen.** `E:` RegelSpraak (produceert de markering langs meerdere routes;
de andere twee frontends weigeren in plaats van te markeren). `C:` alle vier —
WF-044 verbiedt het verbergen van de knoop, dus elke uitvoerder faalt er
zichtbaar op. `S:` twee `$def`s. `K:` **2 documenten** (1 rood, 1 toka). `T:`
de pin.

**Migratienota.** IR-SPEC ch. 4.4 draagt de delta én de blokkade:

> v1 adds a structured `reason` (the four-way taxonomy of `CONSTRUCTS.md`
> ch. 4: structural, declared-names refusal, spec-underdetermined, safety
> refusal) — **OPEN** until the negatief corpus fixes the taxonomy's values.

De taxonomie is dus geschetst (vier categorieën, `CONSTRUCTS.md` §4) maar de
waarden zijn niet vastgelegd, en de voorwaarde die IR-SPEC stelt — dat de
negatief-zone ze vastlegt — is niet vervuld. Met twee corpusdragers is er ook
weinig om op te meten.

Dit is het enige item op de lijst dat vandaag **niet besluit-klaar te maken is
zonder eerst werk te doen**, en daarom staat het in §6 vóór de klap als een
eigen kaart en niet als een vraag.

**Beslisvraag: V11** (wel of niet meenemen in deze klap).

---

#### B25 — `Regelversie` (regeluitvoeringsreflectie): blijft hij toelaatbaar?

**Wire-diff.**

```json
// vóór
{ "node": "Regelversie", "regel": "jutheffing", "inconsistent": true }
// ná — óf hernoemd (RuleStatusCheck), óf de knoop bestaat niet
```

**Partijen.** `E:` geen enkele emitter produceert hem. `C:` alle vier dragen
hem in hun loader. `S:` één `$def`. `K:` **0 documenten in alle 650.** `T:` de pin.

**Migratienota.** Rij 30 is een gesplitst oordeel: "is gevuurd" is beslist als
een declaratief dialectpredicaat (de conditie van X geldt bij het dekpunt,
`RUNTIME-SEMANTICS.md` ch. 5.6), "is inconsistent" wacht op de spec van het
§9.5-kanaal. IR-SPEC ch. 4.5 draagt het gevolg woordelijk: "mirrored here
because v0 dumps carry it; its v1 admissibility is **OPEN** with row 30."

Het item staat op deze lijst omdat het schema de knoop draagt en v1 hem
mogelijk niet draagt — en omdat een knoop met nul dragers de goedkoopste plek is
om een besluit uit te stellen tot hij niet meer uitstelbaar is.

**Beslisvraag: V13.**

---

## 4. Samenvattende tabel

| # | Delta | Familie | Groene-zone-dragers | Alle zones | Vraag |
|---|---|---|---:|---:|---|
| B1 | formaatnaam + schema-id | A | 225 | **650** | V1 |
| B2 | kaart-tabellen → lijsten, zelfnaam weg | A | 225 | 639 | V3 |
| B3 | `regels`-stubs eruit | A | 205 | 472 | — |
| B4 | `diagnostics` eruit | A | 225 | 650 | V2 |
| B5 | `Objecttype`-zijkaarten → ledenknopen | A | 225 | 639 | — |
| B6 | hernoeming NL→EN (56 knopen, 155 velden) | B | 225 | **650** | V2 |
| B7 | feitcreatie-spiegelvelden | C | 5 | 16 | — |
| B8 | `scopeVeld` → `scopeRol` verplicht | C | 7 | 19 | — |
| B9 | rollidmaatschap samensmelten | C | 3 | 8 | V4 |
| B10 | `Kenmerk.eigenaar` | C | 1 | 8 | — |
| B11 | `AttribuutRef`-navigatievelden + `set` | C | 201 | 537 | — |
| B12 | WF-048 vervalt, binding verplicht | C | 16 | 60 | — |
| B13 | referentiepositie-`type` van de wire | D | 206 | 567 | V5 |
| B14 | `Percentage` lost op | E | 4 | 47 | — |
| B15 | `DatumInDagen` + `DatumTijd` → `Date(p)` | E | 19 | 89 | — |
| B16 | `Onbekend` uit de datatypesom | E | 25 (→0 ná B13) | 71 | V6 |
| B17 | exacte factor + canonieke-eenheid-wire | E | 31 | 143 | V7 |
| B18 | `Parameter` volledige tijdlijnfacet | E | 32 | 134 | — |
| B19 | versie-afvlakking keert om | F | 190 | 501 | — |
| B20 | `Soort` klapt in | F | 190 | 501 | — |
| B21 | `Functie` splitst | F | 61 | 170 | — |
| B22 | literalen → getypeerde waarden | G | 148 | 368 | V8, V9 |
| B23 | `Constante` splitst | G | 4 | 10 | V10 |
| B24 | `Onopgelost` krijgt een reden | G | 1 | 2 | V11 |
| B25 | `Regelversie` — blijft hij toelaatbaar? | G | 0 | **0** | V13 |

*"Groene-zone-dragers" = anchor + golden + rood, de zones die gate 2/3/5/6/12
gaten. "Alle zones" telt negatief en toka mee. De zonetotalen zijn: anchor 40,
golden 159, rood 26, negatief 395, toka 30 — samen 650 `ir.json`, plus 566
casusbestanden.*

---

## 5. De genummerde beslisvragen

*Twaalf vragen: **V1–V11** en **V13** hier, **V12** — de toka-volgorde — in §6.
Alle dertien staan open. De aanbeveling is de aanbeveling van deze kaart, niet
een besluit.*

---

### V1 — Hoe heet het formaat, en verhuist het schema?

De huidige `const` is `regelspraak-ir-v0` en het schemabestand heet zo.
De naam draagt de brontaal van de eerste frontend, terwijl de wire er drie
bedient (RegelSpraak, Catala, F-logic) en de missie "engine-neutral" is.

- **(a)** `regelspraak-ir-v1` — continuïteit met de Method-zin die de bump al
  bij naam noemt; de naam blijft wringen.
- **(b)** `uchar-ir-v1` — parallel met `uchar-case-v0`, dat al zo heet en al
  Engelstalig is; consistent met de missie.
- **(c)** `uchar-ir-1.0` — semver vanaf de eerste normatieve uitgave, zoals de
  Conventions van IR-SPEC aankondigen.

**Aanbeveling: (b).** Het casusformaat heeft de knoop al doorgehakt en heet
`uchar-case-v0`; twee formaten uit één repo met twee naamconventies is een last
zonder opbrengst. (c) mag later alsnog: `uchar-ir-v1` is een formaat-id, geen
versienummer, precies zoals `uchar-case-v0` dat is.

---

### V2 — Eén klap of twee epochen?

De lijst valt schoon in tweeën: de **opruiming** (B2–B5, B7–B12, B16, B18,
B19–B21 — wegnemen wat superseded of inert is) en de **hernoeming** (B1, B6 —
elke naam). Beide zijn brekend, maar ze breken op verschillende manieren: de
opruiming raakt weinig documenten diep, de hernoeming raakt alle documenten
ondiep.

- **(a) Eén klap.** Alles tegelijk; één regeneratie, één pin-bump, één
  reviewronde. Maximale samenhang, maximale kans dat er iets misgaat waar geen
  gate op staat.
- **(b) Twee epochen.** Eerst de opruiming (formaat blijft `…-v0`, want de
  wire-entry-discipline laat wegnemen niet toe — dus dit móét een versieklap
  zijn: `…-v1`), dan de hernoeming (`…-v2`). Twee pin-bumps, twee
  regeneraties, en een tussentoestand die niemand nodig heeft.
- **(c) Hernoeming eerst, opruiming daarna.** Mechanisch het veiligst — een
  pure hernoeming is machinaal verifieerbaar met een naamtabel en een
  round-trip — maar hernoemt velden die daarna verdwijnen.

**Aanbeveling: (a).** Twee epochen kosten twee keer de duurste post (de
corpusregeneratie over 650 documenten plus de toolchain-pin) en leveren geen
review-winst: de opruiming is per delta lokaal en toetsbaar, de hernoeming is
mechanisch. Wat (a) nodig heeft is dat de gevaarlijke porten (B11 en B7 in
`backends/drools`) **vóór** de klap zijn geland — zie §6.

**Deelvraag bij B4 (`diagnostics`):** het register zegt non-goal, maar de wire
draagt sinds meerdere kaarten bewust een WARNING als de manier waarop een
document zijn eigen defect uitspreekt. Opties: **(i)** weg, zoals het register
zegt; **(ii)** blijven, met een normatieve status en een WF-regel; **(iii)**
weg uit de IR en terug als een apart, naast het IR-document geleverd
diagnostiek-artefact. *Aanbeveling: (ii).* Het kanaal wordt gebruikt en er is
in het afgelopen halfjaar meer dan eens op gestuurd; het als non-goal weghalen
is een besluit uit 2026-08 dat door de praktijk is ingehaald. Als het blijft,
hoort er een WF-regel bij, want vandaag is het een veld dat niemand leest — het
bezwaar van WF-083 (c), toegepast op zichzelf.

---

### V3 — Blijft de topcontainer vlak, of nestelt hij?

IR-SPEC ch. 2 noemt "a **model**" de rootcontainer en geeft hem acht velden —
alle acht declaraties. De v0-wire is vlak: 24 top-level sleutels waarin
declaraties en `Resolved*`-lijsten door elkaar staan. Ch. 2 zegt daarbij "The v1
model is the declaration set only", maar het document *als geheel* draagt ook
regels.

- **(a) Vlak blijven.** 22 top-level sleutels (na B3 en B4), Engels hernoemd.
  Kleinste stap.
- **(b) Nesten:** `{ "format": …, "model": { … acht declaratielijsten … },
  "rules": { … de regellaagvormen … } }`. Volgt ch. 2 letterlijk en maakt de
  laagscheiding die IR-SPEC hoofdstuk voor hoofdstuk aanhoudt zichtbaar op de
  wire.

**Aanbeveling: (b).** Het is dezelfde bewerking als (a) plus twee niveaus, het
corpus wordt er niet zwaarder van (één mechanische herschikking per document),
en het maakt één ding waar dat vandaag alleen proza is: de modellaag bevat geen
regels en geen expressies. Bijkomend: de *rootvorm* wordt daarmee zelf een
versiegetuige — een v0-lezer die een v1-document krijgt struikelt op de vorm,
niet pas op een veld.

**Bijvangst van deze kaart, hier vastgelegd zodat hij niet verdwijnt:** het
register (`CONSTRUCTS.md` §2.1) noemt `Begrensd` en `TekstReeks` `sugar`, wat
zich laat lezen als "verdwijnt van de wire", terwijl IR-SPEC ch. 4.2 beide
uitdrukkelijk als v1-knoop aanhoudt ("sugar verdict … **mirrored as a node**").
WF-099 hangt sinds 2026-09-10 aan `Clamp`. Dat is geen delta maar een
registerrij die scherper moet: het `sugar`-oordeel geldt de *brontaal*, niet de
wire. Voorstel voor de hoofdsessie: de deltakolom van rij 1 en rij 31 krijgen
elk één zin die dat zegt.

---

### V4 — Hoe smelten de twee rollidmaatschapsknopen samen zonder getuige?

`VerreRollidmaatschap` heeft **nul dragers in alle 650 documenten**, en IR-SPEC
ch. 4.5 zegt dat `filler` en `backref` daarom ongekruist blijven. Een knoop
samenvoegen die niemand schrijft is gratis en ongecontroleerd tegelijk.

- **(a) Samenvoegen zoals ch. 4.5 beschrijft**, zonder getuige. Goedkoop; als de
  afbeelding fout is merkt niemand het tot een frontend de vorm gaat schrijven.
- **(b) Eerst een getuige maken**: één handgeschreven golden die de verre vorm
  draagt en door alle drie de uitvoerders wordt gespeeld, plus een negatief. Dán
  samenvoegen, met die getuige als rood-eerst-bewijs.
- **(c) De verre vorm schrappen** in plaats van samenvoegen: hij heeft geen
  drager, geen producentroute en `ref` weigert hem bij evaluatie. Als v1 hem
  nodig heeft komt hij terug met een getuige erbij.

**Aanbeveling: (b).** De repo-discipline is "a spec sentence without a
machine-checkable artifact is not done", en dit is precies zo'n zin. De getuige
kost één golden en is los van de klap te maken — hij hoort in de voorbereiding
(§6). (c) is aantrekkelijk maar te ver: de nabije vorm *heeft* acht dragers, en
de samensmelting is juist de plek waar de topniveaubeperking sneuvelt, wat een
echte v1-winst is.

---

### V5 — Hoe ver gaat B13?

De referentiepositie-`type` verlaat de wire. Maar welke posities precies?
Gate 13 verdraait vandaag "every unit-bearing `type` annotation on reference and
composite positions" — eenheiddragend, en met een expliciete uitzonderingenset
(het `Literal`-lexeem, de duurzijde van datumrekenkunde, de eigen eenheid van
TIJDSDUUR) en een klem die faalt zodra een eenheiddragende annotatie buiten de
tabel opduikt.

- **(a) Alleen de referentieposities** (`AttribuutRef`, `ParameterRef`,
  `DimensieCel`, `BinderRef`) — precies de posities waar WF-091 de herhaling
  vandaag mechanisch waar maakt. Afgeleide posities (`Binair`, `Functie`,
  `Afgerond`, `Begrensd`, `Let`) houden hun annotatie.
- **(b) Alle annotaties weg**, ook de afgeleide. De consument leidt alles af.
  Maximale opruiming; maar ch. 2.10 kent een **vierde klasse** waar v0 niets
  afleidt (`GEDEELD_DOOR`, `TOT_DE_MACHT`, `PERCENTAGE_VAN`, absolute waarde,
  wortel, de datumextractors) — daar zou de annotatie de enige bron zijn, en er
  is geen afleidingsregel om hem te vervangen.
- **(c) Referentieposities weg, afgeleide posities behouden mét een WF-regel**
  die hun afleiding kruist — de omgekeerde WF-091, op de afgeleide kant.

**Aanbeveling: (a).** (b) valt af op de vierde klasse: ch. 2.10 zegt zelf dat v0
daar geen samenvoegingsregel stelt, en gate 13's klem is er expliciet om de
eerste getuige daarvan te betrappen. (c) is het juiste einddoel maar is een
eigen ontwerpstap (een afleidingswet per operator, normatief opgeschreven) en
hoort niet in dezelfde klap.

**Let op de koppeling:** (a) neemt 90 van de 97 `Onbekend`-voorkomens mee en
maakt V6 bijna leeg. (b) en (c) doen dat ook. Er is geen optie waarbij B16 duur
blijft, mits B13 eerst gaat.

---

### V6 — Wat draagt de AANTAL-telbron als `Onbekend` verdwijnt?

WF-093 (2026-09-06) maakt de telwrat normatief: een eenargumentige
instantievouwbron die het onbekend-sentinel op zijn eigen `type` draagt MOET
soort `AANTAL` hebben. Dat is vandaag de **enige** toegelaten sentinelpositie op
de wire, en hij draagt 90 van de 97 voorkomens.

- **(a) Niets** — B13 haalt de `type`-annotatie van die positie weg en de vraag
  lost op. WF-093 wordt een regel over de vouwsoort alleen; WF-034 arm (r)
  vervalt.
- **(b) De telbron krijgt het type van het getelde element.**
- **(c) De telbron krijgt een eigen v1-vorm** waarin de bron een
  *verzamelingsverwijzing* is en geen attribuutverwijzing met een vlag, zodat de
  vraag niet meer gesteld kan worden.

**Aanbeveling: (a), met (c) als eigen kaart erna.** (a) is gratis zodra B13 is
beslist en verandert geen enkele betekenis. (b) liegt: het type van het element
is niet het type van de telling. (c) is de structureel juiste vorm en is precies
wat B11 (`set` weg) en B21 (`Functie` splitst) samen al half opleveren — maar
hij verdient een eigen meting.

---

### V7 — Landt de canonieke-eenheid-wire in deze klap?

B17 bestaat uit twee stappen: **(i)** `factorNaarCanoniek` wordt een exacte
`{num, den}`, **(ii)** de wire draagt canonieke eenheden in plaats van
bron-eenheden.

- **(a) Alleen (i) in deze klap.** Mechanisch, verliesloos, en het maakt de
  `1/N`-vooraf-afronding die v0 uitvoert exact. (ii) blijft een latere epoch —
  maar dan is er een derde brekende klap.
- **(b) (i) en (ii) samen.** Q9 noemt (ii) "the recorded eventual v1 form", dus
  dit is waar het naartoe gaat. Het beweegt elke eenheiddragende waarde op de
  wire én in de 506 casusbestanden met `number`-waarden.
- **(c) Alleen (i); (ii) wordt expliciet een v2-item** en verdwijnt uit de
  v1-lijst.

**Aanbeveling: (a), met (ii) gekoppeld aan een openstaande kaart.** De
kruis-stelsel-eenheidsdivergentie
(`backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`) is een levende
ref/clingo-splitsing op een verifier-schoon document, gemeten op alle vier de
eenheidsposities. (ii) uitvoeren met die divergentie open betekent de
canonicalisatie bouwen op een rekenregel waar twee uitvoerders het oneens over
zijn. Die kaart eerst, dan (ii) — desnoods als v1.1, want (ii) is een
*waardenverschuiving*, geen vormverschuiving, en kan additief worden gefaseerd
als de eenheid op de wire blijft staan.

---

### V8 — De enumeratiewaarde-delimiters: nu of bij de klap?

Gemeten defect, twee spellingen, verbatim vergelijking, nul getuigen (B22b).

- **(a) Nu, als eigen v0-kaart**, exact zoals de tekstlexeem-kaart van
  2026-09-18: canonicaliseer bij emissie op het kale label, borg met een
  emittergetuige in de frontend-suite, voeg één `bron.rgs`-golden met een
  enumeratieliteral toe, en corrigeer de zin in IR-SPEC ch. 4.1.
- **(b) Meenemen in B22.** Bespaart een kaart; laat het defect tot de klap
  openstaan.
- **(c) Andersom canonicaliseren**: de gedelimiteerde vorm wordt de wire-vorm en
  de consumenten gaan strippen. Volgt de huidige IR-SPEC-zin.

**Aanbeveling: (a).** Het is een v0-producentendefect, geen v1-vorm, en het
precedent staat er (`backlog/done/20260918-tekstlexeem-canonicalisatie.md`) met
gemeten kosten. (c) valt af op hetzelfde argument dat de tekstkaart gebruikte:
de delimiters zijn brontaalsyntaxis, en een `Tekst`-waarde en een enumlabel
horen langs dezelfde regel te lopen — anders draagt de wire twee
canonicalisatiewetten voor twee soorten die beide "een string" zijn.

**De toka-consequentie is er ook en is klein**: de toka-zone draagt 30
documenten met enumeratiedeclaraties; of daar een emitterdivergentie ontstaat die
een herpin vraagt, is met één meting vast te stellen bij de uitvoering.

---

### V9 — Worden literalen echte JSON-waarden?

B22c. Vandaag is `Literal.waarde` altijd een string; elke consument parseert
hem. De rij-27-delta zegt "literals become typed values".

- **(a) Volledig**: `{"number": 12.50}` / `{"date": "2026-03-15"}` /
  `{"text": "…"}` / `{"boolean": true}` / `{"enumeration": "Ruimzee"}` — precies
  de vormen die `uchar-case-v0` al gebruikt voor casuswaarden.
- **(b) Alleen de exactheid**: een getal wordt een JSON-string die exact is
  (geen float), de rest blijft lexeem. Lost de parseerlast niet op.
- **(c) Niets nu**: de WF-081/084/095-hekken pinnen het lexeem al per soort, en
  een lexeem met een gepind grammaticaal formaat is functioneel een getypeerde
  waarde. Schrap de delta.

**Aanbeveling: (a).** Twee redenen. Ten eerste: het casusformaat heeft deze
vormen al, dus de vormentabel hoeft niet bedacht te worden en producent en
verwachting gaan langs dezelfde vorm — precies het probleem dat de
tekstlexeem-kaart tegenkwam toen een geëmitteerde golden alleen speelde als de
casusinvoer de delimiters óók droeg. Ten tweede: JSON-getallen zijn géén optie
voor exacte decimalen (IEEE-754), dus `{"number": "12.50"}` met een string
binnenin is de juiste vorm — dezelfde keus die het casusschema al maakte. (c)
is verdedigbaar maar houdt vier parsers in vier talen in stand voor iets wat de
serialisatie gratis kan doen.

---

### V10 — Krijgt `de dag` een eigen knoopsoort?

IR-SPEC ch. 4.1 laat dit uitdrukkelijk open. Zes documenten dragen de vorm.

- **(a) Eigen knoop** (`BoundDay`), want het ís iets anders: geen sessiewaarde
  maar de gebonden dag van een dagsoortdefinitie. Rij 25 mat bij de landing van
  WF-098 dat elf van de veertien `Constante`-voorkomens op de wire deze vorm
  waren; vandaag draagt `de dag` zes documenten tegen `Rekendatum` drie.
- **(b) `ContextValue` met drie namen**, zoals WF-098 hem vandaag sluit.
  Kleinste stap.
- **(c) Uitstellen** tot `backlog/new/20260910-dagsoort-semantiek.md` is gedaan,
  want `RUNTIME-SEMANTICS.md` ch. 8 draagt de looptijdbetekenis van `de dag`
  vandaag niet.

**Aanbeveling: (c), daarna (a).** Een knoopsoort maken voor een vorm waarvan de
semantiek niet is opgeschreven is de verkeerde volgorde, en het semantiekgat is
al gekaart. Als die kaart landt vóór de klap, wordt het (a); landt hij niet, dan
(b) — wat geen schuld toevoegt, alleen bestendigt.

---

### V11 — Gaat B24 (`Onopgelost`-reden) mee in deze klap?

IR-SPEC ch. 4.4 markeert de taxonomie als **OPEN** "until the negatief corpus
fixes the taxonomy's values", en die voorwaarde is niet vervuld. Twee
corpusdragers.

- **(a) Mee**, met de vier categorieën van `CONSTRUCTS.md` §4 als
  enumwaarden, ook al zijn ze niet door het corpus gevalideerd.
- **(b) Niet mee**: `Onopgelost` houdt zijn v0-vorm (hernoemd door B6), en de
  reden komt later additief — een optioneel `reason`-veld breekt niets.
- **(c) Eerst een kaart** die de taxonomie meet tegen de ~24
  `Onopgelost`-productiesites van de v0-toolchain plus de routes die uchar's
  eigen frontend intussen zelf heeft toegevoegd, en pas daarna beslissen.

**Aanbeveling: (b).** Dit is de enige delta op de lijst die géén brekende
wijziging hóéft te zijn: een reden toevoegen is additief zolang hij optioneel
is. Hem in de klap trekken kost een ontwerpbesluit dat nergens door gemeten is,
en levert niets wat een latere additieve landing niet ook levert. (c) blijft
zinvol als eigen kaart, los van de klap.

---

### V13 — Blijft `Regelversie` in v1?

B25. Nul dragers in alle 650 documenten; geen emitter produceert hem; IR-SPEC
ch. 4.5 markeert zijn v1-toelaatbaarheid als OPEN.

- **(a) Hernoemen en houden** (`RuleStatusCheck`), zoals nu, met de OPEN-status
  erbij. De knoop blijft zonder getuige.
- **(b) De "is gevuurd"-helft houden, de "is inconsistent"-helft schrappen** tot
  het §9.5-kanaal gespecificeerd is — rij 30's eigen splitsing gevolgd.
- **(c) Beide schrappen.** De knoop heeft geen producent, geen drager en geen
  gespecificeerde semantiek; hij komt terug wanneer een frontend hem vraagt,
  met een getuige erbij. `CONSTRUCTS.md` rij 35 gebruikt precies dat argument
  voor de parameterschalen: "adopt, don't invent".

**Aanbeveling: (c).** De "is gevuurd"-helft *is* beslist (RUNTIME-SEMANTICS
ch. 5.6 draagt de vuurmarkering), maar er is geen enkel document dat hem
uitvoert en geen emitter die hem schrijft — dus wat er vandaag in het schema
staat is een vorm zonder betekenisdrager. De formaatklap is het goedkoopste
moment om zo'n vorm weg te nemen, en de weg terug is de gewone
wire-entry-discipline: additief, met een getuige.

---

## 6. De toka-volgordevraag

### V12 — Zone opruimen vóór de migratie, of mee-regenereren?

**Wat er op het spel staat.** Gate 11 vergelijkt uchar's eigen frontend tegen
`toka/casus/ir.json` — een dump die **één keer met de originele toolchain** is
geëmitteerd. Dat is het enige directe byte-anker dat uchar tegen de
productietoolchain heeft. De zone draagt 30 `ir.json` en 47 `.rgs`-bronfragmenten,
waarvan de gate er 45 byte-identiek her-emitteert (de twee holdouts `toka-23` en
`toka-45`), met vloeren `EXPECTED_MIN_TESTS=27`, `EXPECTED_MIN_FRAGMENTS=45`,
`EXPECTED_MIN_CASES=29`.

**Bij een v1-klap houdt dat anker per definitie op te bestaan.** De toolchain
emitteert v0; uchar's frontend gaat v1 emitteren. Byte-vergelijking tussen twee
formaten bestaat niet. Wat overblijft is "frontend ≡ de opgeslagen toestand",
wat de zone-README zelf al erkent voor de tekstlexeem-divergentie: "for text
literals this dump has stopped being a toolchain dump."

**Twee gemeten obstakels voor een pure regeneratie**, beide gevonden bij deze
kaart:

1. `toka/casus/ir.json` draagt een `"methode": "COMPLEX"`. WF-024 maakt dat
   ill-formed in v1. De zone is niet verifier-gegatet (gate 11 schema-checkt
   alleen de replay-slices), dus vandaag valt dat niet op — maar een v1-dump met
   een COMPLEX is geen v1-dump.
2. De zone draagt de tweede geboekte toolchain-divergentie al (`toka-45`,
   besluit 16Y): een fragment dat uchar's eenheidstypering afwijst en dat
   daarom permanent uit de her-emissie wordt gehouden. Dat is één holdout; een
   v1-klap voegt er potentieel meer toe, per delta waar uchar van de toolchain
   afwijkt.

**Toka-aandeel per delta**, gemeten: `Percentage` 30 van 47 dragers,
`DatumInDagen` 30 van 81, `DatumTijd` 30 van 42, `Eenheid`/`factorNaarCanoniek`
30 van 143, `Enumeratie` 30 van 68, `Tekst` 30 van 51. De zone is dus **zwaar
oververtegenwoordigd in familie E** (het typesysteem) — precies de familie waar
uchar de meeste bewuste divergenties van de toolchain heeft.

**Opties.**

- **(a) Zone opruimen vóór de migratie**, met de geschiedenis-herschrijving die
  PLAN §9 besluit 10 toch al eist voor de publieke flip. Gate 11 verlaat de
  gate-set — de gedocumenteerde uitzondering op monotonie, die daarvoor al is
  afgesproken. De klap raakt dan 620 documenten in plaats van 650, en de
  frontend-pariteit tegen de originele toolchain vervalt op het moment dat hij
  toch zou vervallen.
- **(b) Mee-regenereren**, de zone naar v1 brengen, gate 11 verandert van een
  pariteitsgate in een regressiegate ("frontend ≡ de opgeslagen v1-toestand"),
  en opruimen bij de flip.
- **(c) Zone bevriezen op v0**: de zone blijft v0, gate 11 blijft draaien tegen
  een v0-emitter die alleen daarvoor nog bestaat. Kost een tweede emitterpad.

**Aanbeveling: (a).** Drie argumenten.

Ten eerste: **de prijs is al geaccepteerd.** `CLAUDE.md` legt vast dat de flip
zowel het verwijderen van de zone als een geschiedenis-herschrijving vraagt, en
dat gate 11 met de zone vertrekt. (a) betaalt die prijs op een moment dat er
toch al één grote, alles rakende bewerking gaande is.

Ten tweede: **wat (b) bewaart is niet wat het lijkt te bewaren.** Na de klap
bewijst gate 11 geen toolchain-pariteit meer, want de vergelijking is tegen een
opgeslagen v1-toestand die uchar zelf heeft geschreven. Dat is een
regressiegate, en die hebben we al: gate 10 eist byte-identieke her-emissie over
54 `bron.rgs`-goldens, en de tekstlexeem-kaart heeft gate 10's emittergetuige
al aangewezen als "de blijvende machinale borging" voor precies dit geval.

Ten derde: **(b) is duurder dan hij oogt.** De zone draagt 30 van de 47
`Percentage`-dragers en 30 van de 42 `DatumTijd`-dragers; hij is
oververtegenwoordigd in precies de deltas waar uchar bewust van de toolchain
afwijkt. Elke zo'n delta wordt in (b) een extra holdout of een extra herpin, met
de bijbehorende redenzin — en de zone-README zegt zelf dat elke herpin de
pariteitsclaim verder uitholt.

**Wat (a) wél kost en eerlijk benoemd moet worden:** de TOKA-casus is de enige
plek waar uchar's frontend tegen een *echt* stelsel van productieregels is
getoetst. De Jutterswet is eigen materiaal en daarmee mede door de frontend
gevormd. Als de zone weggaat vóór de klap, gaat de v1-frontend zonder die toets
de lucht in. Een gedeeltelijke verzachting die deze kaart voorstelt: **laat de
zone staan tot en met de laatste voorbereidende kaart van §6** — de porten van
`backends/drools` en de enumeratie-canonicalisatie zijn allemaal v0-werk en
profiteren van de toka-toets. Ruim hem op als **laatste stap vóór** de klap,
niet als eerste.

---

## 7. Het vervolgprogramma in kaartvorm

Drie fasen. De eerste is v0-werk en hoeft niet op een besluit te wachten; de
tweede is de klap zelf; de derde is de nasleep bij de toolchain.

### Fase 1 — voorbereiding (v0, geen versieklap, gates blijven groen)

Deze kaarten maken de klap mechanisch in plaats van riskant. Ze zijn stuk voor
stuk los te landen en verbeteren de v0-toestand ook als de klap nooit komt.

| # | Kaart | Waarom vóór de klap | Blokkeert |
|---|---|---|---|
| P1 | **`backends/drools` leest `navigatieBindingen`** | de loader negeert ze vandaag en leidt navigatie her-af uit `navigatieRol`/`navigatiePad`; die velden verdwijnen bij B11 | B11 |
| P2 | **`backends/drools` leidt bonenvelden af uit de rolbindingen** — feitcreatie (`zetVeld`/`joinVeld`/`Tussenstap.*`) en uniciteitsscope (`scopeVeld`) | de backend *voert die velden uit*; ze weghalen is zonder deze port een amputatie | B7, B8 |
| P3 | **`backends/drools` leest `verbindendeRol` op de verdeling en `bronRol` op de vouw** | dezelfde her-afleiding, dezelfde loader-javadoc; B12 maakt de binding verplicht | B12 |
| P4 | **Enumeratiewaarde-lexeem gecanonicaliseerd** (V8 (a)) | gemeten v0-defect: twee producenten, twee spellingen, verbatim vergelijking, nul getuigen | B22b |
| P5 | **DatumTijd-lexeem gecanonicaliseerd** (WF-095 versmalt tot canoniek ISO) | één emitterregel, vijf negatief-getuigen, nul goldenregeneratie — de goedkoopste stap op de lijst | B22a |
| P6 | **Getuige voor de verre rollidmaatschap** (V4 (b)) | de knoop heeft nul dragers; samenvoegen zonder getuige is onbewijsbaar | B9 |
| P7 | **`eigenaar`-gastheerobject beslissen** (`backlog/new/20260910-eigenaar-gastheer-object.md`) | als de OBJECT-helft betekenis is, verdwijnt het veld niet | B10 |
| P8 | **Kruis-stelsel-eenheidsdivergentie** (`backlog/new/20260910-kruis-stelsel-eenheid-divergentie.md`) | een levende ref≢clingo-splitsing onder de eenheidscanonicalisatie | B17 (ii) |
| P9 | **Dagsoortsemantiek** (`backlog/new/20260910-dagsoort-semantiek.md`) | `de dag` krijgt geen knoopsoort voordat zijn looptijdbetekenis er staat | B23 / V10 |
| P10 | **Naamtabel v0→v1 geëxtraheerd en getoetst** | IR-SPEC draagt elke Engelse naam al met een `v0 name:`-regel; extraheer die tot één machineleesbare tabel en toets hem op volledigheid tegen de 80 knopen en 193 properties van het schema | B6 |
| P11 | **Toka-zone opruimen + geschiedenis-herschrijving** (V12 (a)) | laatste stap vóór de klap, zodat P1–P10 nog van de toka-toets profiteren | de klap |

### Fase 2 — de klap

Eén kaart is niet realistisch en één-kaart-per-delta is verspilling. De
voorgestelde snede volgt de families, omdat elke familie één samenhangend
codestuk raakt en de gates er per familie iets van merken:

| # | Kaart | Deltas | Gates die bewegen |
|---|---|---|---|
| M1 | **Het schema en de container** | B1, B2, B3, B4, B5 | 2 (schema-id + `EXPECTED_MIN_DOCS`), 3, 4 |
| M2 | **De spiegelbagage eruit** | B7, B8, B9, B10, B11, B12 | 3 (~30 negatief-getuigen), 5, 6, 12 |
| M3 | **De typeannotatie** | B13, B16 | 3 (57 negatief-getuigen), **13** — de annotatie-onverschilligheidsgate verliest zijn onderwerp en moet worden ingetrokken of herzien |
| M4 | **Het typesysteem** | B14, B15, B17 (i), B18 | 2, 3, 5, 6, 12 |
| M5 | **De regellaag** | B19, B20, B21 | 3, 5, 6, 12 |
| M6 | **De expressielaag** | B22 (a/b/c), B23, B24-of-niet, B25 | 2, 3, 5, 6, 9, 10, 14 |
| M7 | **De hernoeming** | B6 | **alle veertien** |

M7 staat achteraan en is bewust puur mechanisch: als M1–M6 geland zijn is de
hernoeming een tabelvervanging plus een regeneratie, en dan is de
round-trip-eigenschap van gate 2 het bewijs.

**Wat elke M-kaart in elk geval moet doen** — dit is de terugkerende
boekhouding, niet per kaart opnieuw te bedenken:

- de drie emitters bijwerken; de emitter-goldens regenereren (gate 9: 12
  Catala-goldens; gate 10: 54 `bron.rgs`; gate 14: 1 `bron.flogic`);
- de vier consumenten bijwerken; hun eigen suites houden hun vloeren
  (`EXPECTED_MIN_CHECKS` 201/81/83, `EXPECTED_MIN_TESTS` 239/173,
  `EXPECTED_MIN_EXAMPLES` 489);
- de corpuszones regenereren en de vloeren opnieuw meten
  (`EXPECTED_MIN_DOCS` 192, `EXPECTED_MIN_NEGATIEF` 350, `EXPECTED_MIN_CASES`
  427, `EXPECTED_MIN_ROOD_CASES` 26);
- gate 12's `EXPECTED_PLAYED` (vandaag 150) en zijn benoemde
  `EXPECTED_CHANNEL`-lijst opnieuw vaststellen — dat is een **gelijkheid**, geen
  vloer, dus die beweegt bij elke M-kaart;
- de negatief-getuigen langslopen die hun regel verliezen — zie hieronder.

**De negatief-zone is de grootste post en verdient zijn eigen besluit.** Van de
396 negatief-mappen zijn er **136** getuige van een regel die bij de klap zijn
onderwerp verliest of verandert (wf034 ×65 waarvan ~18 een verdwijnend veld
hekken, wf091 ×27, wf094 ×10, wf053 ×7, wf095 ×5, wf054 ×3, wf092 ×3, wf096 ×3,
wf028 ×3, wf098 ×2, wf048 ×2, wf084 ×2, wf100/wf081/wf024/wf003 ×1). Elk van
die getuigen moet één van drie dingen worden: herschreven op de v1-vorm,
verwijderd omdat de regel weg is, of vervangen door een getuige van de
opvolgerregel. **Dat is geen restwerk; dat is de helft van de klap.** Het
alternatief — de zone laten staan met getuigen van regels die niet meer bestaan
— maakt gate 3 groen op iets wat niets meer bewaakt, en dat is de definitie van
een groene deelverzameling.

### Fase 3 — de toolchain

De afhankelijkheid loopt sinds de omdraaiing van 2026-08-29 zo: `regelspraak-java`
**vendort** `frontends/regelspraak` byte-identiek op `SPEC_REV` en compileert
die slice als bronmap; zijn eigen `uchar-conformance`-gate is de driftcheck. En
`backends/drools` compileert diezelfde recordtypen mee. Eén verzameling
Java-records draagt dus de emitter, de Drools-loader én de toolchain.

| # | Stap | Inhoud |
|---|---|---|
| T1 | **Herpin** | `SPEC_REV` naar de v1-kop; de gevendorde kopie opnieuw byte-identiek maken; de driftgate opnieuw groen |
| T2 | **Toolchain-consumenten porten** | de toolchain voert sinds 2026-09-09 de wire-`Let` zelf uit in beide back-ends (de R1-port); die lezers bewegen mee met elke delta die de expressielaag raakt |
| T3 | **Toolchain-emittergoldens herpinnen** | `uchar-conformance` pint 20 emitter-goldens; die zijn per constructie v0 en moeten alle twintig opnieuw |
| T4 | **Scenariolaag** | `ScenarioResolver`/`ResolvedScenario`/`ScenarioRunner` bleven toolchain-lokaal op de geconsumeerde typen; ze bewegen mee met B19 (versievorm) en B20 (`Soort`) |

**De volgorde tussen uchar en de toolchain is de bestaande: spec-first.** De
delta landt hier (spec + corpus + schema), daarna landt de pin-bump daar. Dat
werkt per M-kaart, mits elke M-kaart in uchar groen afsluit — wat de eis van elke
kaart al is.

---

## 8. Wat deze kaart bewust niet heeft gedaan

- **Geen besluit genomen.** Alle dertien vragen staan open; de aanbevelingen
  zijn aanbevelingen.
- **Geen code, schema, corpus of gate aangeraakt.** De veertien gates draaiden
  één keer ter bevestiging en stonden alle veertien groen (exitcode 0).
- **De additieve deltas niet geïnventariseerd.** Die vallen per §1 buiten de
  opdracht en hoeven niet op de klap te wachten.
- **De v1-naamtabel niet geëxtraheerd.** Dat is P10 en het is echt werk: IR-SPEC
  draagt de namen wel, maar verspreid over ch. 2–4 en niet machineleesbaar.
- **De kosten niet in tijd geschat.** De maten hier zijn documentaantallen,
  regelaantallen en gatevloeren, want die zijn gemeten; een tijdsschatting zou
  dat niet zijn.

## 9. Voorgestelde registerrijen

*Voor de hoofdsessie, bij de landing van deze kaart — `spec/CONSTRUCTS.md` en
`spec/PLAN.md` zijn het serieel-bijgehouden boekhoudkanaal van de hoofdsessie en
zijn door deze kaart niet aangeraakt.*

1. **Rij 1 en rij 31, deltakolom:** één zin die zegt dat het `sugar`-oordeel op
   `Begrensd` en `TekstReeks` de *brontaal* betreft en niet de wire — IR-SPEC
   ch. 4.2 houdt `Clamp` en `TextComposition` als v1-knoop aan, en WF-099 hangt
   sinds 2026-09-10 aan de eerste (zie V3).
2. **Rij 27, deltakolom:** de meting van de enumeratiewaarde-delimiters — de
   RegelSpraak-emitter schrijft de gedelimiteerde vorm, de Catala-plugin en elke
   handgeschreven wire het kale label, alle vier de consumenten vergelijken
   verbatim, en **nul van de 650 documenten** dragen de gedelimiteerde vorm, want
   geen `bron.rgs`-golden draagt een enumeratieliteral. De zin in IR-SPEC
   ch. 4.1 die zegt dat de delimiters blijven, beschrijft de emitter en niet de
   consumenten (zie V8).
3. **Rijen 4/5/22, deltakolom:** de gemeten autoriteitsverhouding — de
   Drools-loader behandelt de v0-bonenvelden als GEZAGHEBBEND en negeert vier
   symbolische uitbreidingen (`navigatieBindingen`, `navigatieBinding`, een
   vouw-`bronRol`, een verdelings-`verbindendeRol`), die hij zelf her-afleidt uit
   het model. De formulering "de symbolische opvolgers zijn de enige gelezen
   vorm" geldt voor `ref` en clingo op de feitcreatie, en nergens anders.
