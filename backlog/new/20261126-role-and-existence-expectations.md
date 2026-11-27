# role-and-existence-expectations: verwachtingen voor niet-bestaan, roltelling en gecreëerde objecten in scenariotoetsen

*Aangemaakt in de werkbank op 2026-11-26 naar aanleiding van scenario `s37-v01` van `herleving-ww` ("Er ontstaat geen
nieuw WW-recht"); op 2026-11-27 naar deze backlog overgebracht. Nog niet opgepakt. Raakt beschermde paden
(`spec/schema/`, `spec/RUNTIME-SEMANTICS.md`): vereist expliciet mandaat. Vervolgkaart in de toolchain:
`20261126-role-and-existence-expectations-in-service.md`.*

Goal: een scenariotoets en een `uchar-case-v0`-casus kunnen rechtstreeks toetsen dat een rol van een bestaand label
vaststaand leeg is, precies $n$ instanties bevat, naar een bepaald label wijst of (onder `Afleiden uit leeg: nee`) niet
vaststaat, zodat niet-creatie door `objectcreatie` of `feitcreatie` toetsbaar is zonder tel-hulpregels in het
domeinmodel; eventueel gevolgd door een type-brede telling en door genavigeerde verwachtingen op gecreëerde objecten.

Acceptance (bij oppakken aanscherpen):
- **Ontwerpbesluit** over de expectation kinds (`roleEmpty`, `roleCount`, `roleTarget`, `roleOpen` en eventueel
  `instanceCount`/`createdCount`) en hun driewaardige semantiek onder `Afleiden uit leeg: nee` (zie § 4 hieronder).
- **Schema & semantiek:** uitbreiding van `spec/schema/uchar-case-v0.schema.json` en `spec/RUNTIME-SEMANTICS.md`.
- **Uitvoerders:** `ref`, `clingo` en `Drools` (beide weergaven) spelen de nieuwe verwachtingen gelijk.
- **Scenariolaag:** de RegelSpraak-scenariofrontend (parser, resolver, lowering) en
  `frontends/regelspraak/grammar/DIVERGENCES.md` (sectie *Scenariotoetsen*) dragen de vormen uit § 1.
- **Getuige:** op `corpus/golden/jutterswet-penningbesluit/` (of een nieuwe getuige) is niet-creatie rechtstreeks
  toetsbaar zonder het hulpattribuut `aantal penningbesluiten`.
- Volledige `make gates` (17 gates, `red=0`) en `scripts/check-self-contained.sh` groen.

May touch:
- `spec/schema/uchar-case-v0.schema.json`, `spec/RUNTIME-SEMANTICS.md`
- `frontends/regelspraak/**` (scenarioparser, resolver, lowering, `DIVERGENCES.md`)
- `ref/`, `backends/clingo/`, `backends/drools/`
- `corpus/golden/` (getuige)
- deze kaart (`backlog/new/20261126-role-and-existence-expectations.md` → `doing/` → `done/`)

Out of scope:
- de werkbank (`werkbank/`, `v2/herleving-ww`, `eval/datasets/herleving-ww`)
- `regelspraak-toolchain` (vervolgkaart `20261126-role-and-existence-expectations-in-service.md` na de uchar-pin)

## Aanleiding

In `uchar-case-v0` (`spec/schema/uchar-case-v0.schema.json`) en de scenariotaal
(`frontends/regelspraak/grammar/DIVERGENCES.md`, sectie *Scenariotoetsen*) bestaan op dit moment uitsluitend
verwachtingen op een **attribuut**, **kenmerk** of **consistentie** van een al bestaand invoerlabel:

- `attributeValue`, `attributeEmpty`, `attributeTimeline`, `attributeOpen`, `cellValue`, `cellEmpty`
- `characteristicPresent`, `characteristicAbsent`, `characteristicOpen`
- `consistent`, `inconsistent`

Er bestaat geen verwachtingsvorm om rechtstreeks te toetsen dat:
1. een objectcreatie-regel **geen** instantie heeft aangemaakt (of precies $n$ instanties);
2. een rol van een bestaand object **leeg** is gebleven of precies $n$ doelinstanties bevat (na `objectcreatie` of
   `feitcreatie`);
3. een door `objectcreatie` aangemaakt object (dat geen invoerlabel draagt) bepaalde attribuut- of kenmerkwaarden heeft.

Dat gebrek is al zichtbaar in het eigen golden-corpus: in `corpus/golden/jutterswet-penningbesluit/source.rgs` zijn
speciaal voor de toetsing drie hulpregels en drie hulpattributen op `Jutter` toegevoegd (`het aantal penningbesluiten`
met `Regel tel de penningbesluiten`, plus `uitkering` en `uitkeringsjaar`), uitsluitend om in
`case2-onder-de-drempel.json` te kunnen controleren dat er **geen** `Penningbesluit` is gecreëerd
(`"aantal penningbesluiten": 0`).

In domeinregelsets zoals een toekomstige WW-ontstaansregelset (vgl. `s37-v01`) of andere toekenningsregelingen wil je
rechtstreeks op het (niet-)bestaan van een gecreëerd object of een gevulde/lege rol kunnen toetsen zonder het
domeinmodel te vervuilen met tel-hulpregels.

## Ontwerpvoorstel voor de scenariotaal en `uchar-case-v0`

### 1. Rol- en bestaansverwachtingen op een bestaand label (kern; kleinste stap)

Sluit aan bij de bestaande scenariotaal en bij `open-to-one-roles` (`- de|het <rol> is leeg` in situaties):

| Scenariotaal (`Met als verwacht resultaat:`) | `uchar-case-v0` expectation `kind` | Betekenis |
|---|---|---|
| `- de\|het <rol> van <LABEL> is leeg` | `roleEmpty` (`label`, `role`) | De rol van `<LABEL>` is vaststaand leeg (geen gekoppelde of gecreëerde instanties; onder `Afleiden uit leeg: nee` faalt dit als de creatievoorwaarde open bleef) |
| `- het aantal <rol-mv> van <LABEL> is <n>` | `roleCount` (`label`, `role`, `count`) | De rol van `<LABEL>` bevat precies `<n>` instanties (incl. `0` voor *"er is geen ... aangemaakt/gekoppeld"*) |
| `- de\|het <rol> van <LABEL> is <LABEL2>` | `roleTarget` (`label`, `role`, `target`) | Voor `to-one` (of `feitcreatie`): de rol wijst naar invoerlabel `<LABEL2>` |
| `- de\|het <rol> van <LABEL> staat niet vast` / `- het aantal <rol-mv> van <LABEL> staat niet vast` | `roleOpen` (`label`, `role`) | Onder `Afleiden uit leeg: nee`: de rol/creatie staat niet vast (bijv. ontbrekende invoer in de voorwaarde van een `objectcreatie`) |

Voorbeeld op `jutterswet-penningbesluit` (zonder `Regel tel de penningbesluiten`):
- `- het toegekend besluit van J3 is leeg` (of `- het aantal toegekende besluiten van J3 is 0`)

Voorbeeld op een toekomstige WW-ontstaansregelset (`s37-v01`):
- `- het nieuwe recht van A2 is leeg` (of `- het aantal nieuwe rechten van N1 is 0`)

### 2. Optioneel: type-brede creatie-/bestaanstelling (zonder ankerrol)

Wanneer een objecttype zelfstandig of zonder vaste ankerrol wordt getoetst:

| Scenariotaal | `uchar-case-v0` `kind` | Betekenis |
|---|---|---|
| `- er bestaat geen <Objecttype>` / `- het aantal <Objecttype-mv> is <n>` | `instanceCount` (`objectType`, `count`) | Totaal aantal instanties van `<Objecttype>` in de eindwereld (of apart `createdCount` voor alleen door `objectcreatie` aangemaakte instanties) |

Aandachtspunt: een ankerrol (`roleEmpty`/`roleCount`) is specifieker dan een globale telling zodra er al een *eerder*
recht van hetzelfde type (`R1` naast een al dan niet ontstaan `R2`) in de situatie zit. In `s37` bestaat `R1` al als
`RechtOpUitkeringWwBijOntslag`, dus *"er bestaat geen RechtOpUitkeringWwBijOntslag"* klopt daar niet, terwijl *"het
toegekende recht van A2 is leeg"* of *"er wordt geen RechtOpUitkeringWwBijOntslag aangemaakt"* (alleen gecreëerde
instanties) wel precies de juiste toets is.

### 3. Vervolgvraag: eigenschappen van gecreëerde objecten toetsen

Gecreëerde instanties dragen geen case-label (`spec/schema/uchar-case-v0.schema.json` r. 288: "A created instance (no
case label) never matches"), dus een scenario kan geen attribuut van een gecreëerd object toetsen zonder hulpregel op
het ankerobject. Mogelijke vervolgstap:
- voor een `to-one`-rol: genavigeerde verwachting `- het besluitbedrag van het toegekend besluit van J3 is 250 €` (in
  `uchar-case-v0` als `rolePath: ["toegekend besluit"]` naast `label: "J3"`).

### 4. Koppeling met `Afleiden uit leeg: nee` (driewaardige creatie)

Onder `Afleiden uit leeg: nee` moet vastliggen wat `roleEmpty` / `roleCount(0)` doet wanneer de voorwaarde van een
`objectcreatie`-regel **open** blijft (niet waar en niet onwaar):
- Bij een **vaststaand onware** creatievoorwaarde (zoals `R2 heeft geen wekeneis` in `s37-v01`): `roleEmpty` /
  `roleCount(0)` **slaagt**.
- Bij een **open** creatievoorwaarde (bijv. aantal gewerkte weken niet gegeven): creatie is open → `roleEmpty`
  **faalt** en `roleOpen` **slaagt** (analoog aan `characteristicAbsent` tegenover `characteristicOpen`).

## Relatie met de werkbank

- In `v2/herleving-ww` ontbreekt een ontstaansregel voor nieuwe WW-rechten bewust (art. 15–19 WW vallen buiten de
  scope van herleving; `hww-17-wekeneis.rgs` toetst alleen de wekeneis op een aangeleverd recht `R2`). Daarom blijft
  *"Er ontstaat geen nieuw WW-recht"* in `s37-v01` voorlopig `buiten de beslisservice: regelset`.
- Deze kaart richt zich op de generieke taal- en engine-ondersteuning, bruikbaar voor bestaande
  `objectcreatie`/`feitcreatie`-modellen (`jutterswet-penningbesluit` e.a.) en voor toekomstige domeinregelsets
  (toekenning WW, WIA, ZW, enz.).

Hangt samen met: `20261126-open-collections-impact-analysis.md` (open rollen en collecties).

## Omvang

Klein tot middel (1–2 gerichte iteraties: eerst `roleEmpty`/`roleCount`/`roleOpen` in schema, frontend en uitvoerders,
daarna eventueel genavigeerde eigenschappen op gecreëerde objecten).
