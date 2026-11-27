# open-collections-impact-analysis: impactanalyse op `v2/herleving-ww` voor `0..n`-collecties, kwantoren, open punt 52 en een controle op stap 1

*Aangemaakt in de werkbank op 2026-11-26 op verzoek vanuit de `uchar`/`regelspraak-toolchain`-sessie; op 2026-11-27
naar deze backlog overgebracht. Nog niet opgepakt. Alleen onderzoek en rapportage: geen code-, spec-, corpus- of
regelwijzigingen. Dit is de praktijktoetsing waar de geparkeerde kaarten `20261126-open-to-many-roles.md`,
`20261126-open-fallback-opgave.md` en (voor vraag 3) `20261126-strict-timelines.md` op wachten.*

Goal: een feitelijk onderbouwd verslag, gegrond op de strikte regelset `v2/herleving-ww` in de werkbank
(`regelspraak-werkbank`), dat langs vijf ontwerpvragen bepaalt waar engine-primitieven ophouden en model- en
regelsetkeuzes beginnen, als invoer voor het besluit of en hoe `uchar` `0..n`-collecties en `(mag opgegeven worden)`
(open punt 52) verder ondersteunt.

Acceptance:
- Een zelfstandig, beknopt verslag in de werkbank op `werknotities/20261126-uchar-stap2-onderzoek/verslag.md`, met
  tabellen per vraag 1 t/m 5, exacte bestands- en regelverwijzingen in `v2` en een advies per vraag.
- Bij afsluiting een `Result:`-notitie in deze kaart met het advies per vraag, zonder regelset-citaten (die blijven in
  het verslag in de werkbank).
- Geen wijziging in deze repository buiten deze kaart; in de werkbank geen wijziging in `rulesets/`, `eval/`,
  `werkbank/` of `toolchain/`.

May touch:
- deze kaart (`backlog/new/20261126-open-collections-impact-analysis.md` → `doing/` → `done/`)
- buiten deze repository: werkbank `werknotities/20261126-uchar-stap2-onderzoek/**`

Out of scope:
- implementatie in `uchar`, `regelspraak-toolchain`, `v2/herleving-ww` of de eval-dataset
- het aanpassen van de geparkeerde kaarten (dat besluit de maintainer op grond van het verslag)

## Aanleiding

In `uchar` en `regelspraak-toolchain` is **stap 1** van open-world-ondersteuning op objectniveau inmiddels geland
(`backlog/done/20261126-open-to-one-roles.md`; toolchain `backlog/done/20261126-open-to-one-roles-in-service.md`):
open `to-one`-rollen (`0..1` / `1..1`, open punt 51), met `rolesEmpty`, `- de|het <rol> is leeg` in scenario's,
`"roles": {"<rol>": {"state": "empty"}}` in strikte HTTP-verzoeken, `(volledig gegeven)` op een `één`-rol, en
`{"kind": "role", ...}` in `?openInputs=true`.

Voordat `uchar` beslist wat er in de engine moet komen voor **`0..n`-collecties** en **`(mag opgegeven worden)`**
(open punt 52), is een op `v2/herleving-ww` gegronde impactanalyse nodig: `s01`–`s08` (en waar nuttig `s09`–`s10`)
plus de volledige regelset.

## Onderzoeksvragen

1. **Kwantor-herschrijving in plaats van intervalrekenkunde op waardevouwen:**
   `uchar` bouwt geen intervalrekenkunde op waardevouwen (`aantal`, `max`, `min`, `som`): over een open collectie
   blijft een waardevouw `open`, terwijl **`QuantifierFold`** (`ten minste k`, `geen`, `ten hoogste k`, `alle`) wel
   monotoon kortsluit op bekende elementen.
   - Inventariseer alle regels in `v2/herleving-ww/rules/*.rgs` die over collecties vouwen (`het aantal ... > 0`,
     `het aantal ... == 0`, `de maximale waarde van ... > grens`, `eerste`/`laatste`, enz.).
   - Welke daarvan zijn inhoudelijk existentieel/universeel en kunnen 1-op-1 naar `ten minste één van ...` /
     `geen van ...` / `alle ...` herschreven worden zodat monotone kortsluiting werkt? Let ook op bestaande
     frontend-/dialectbeperkingen zoals een kwantor in een default-voorwaarde (vgl. `hww-04-herlevingstermijn.rgs`
     r. 14–16).
   - Welke zijn echte waardevouwen (zoals `eerste beëindigingsdatum` in `hww-03`, `laatste herlevingsdatum` in
     `hww-05`, `eerste kalendermaand` in `hww-16`) die inhoudelijk terecht pas kunnen vaststaan wanneer de collectie
     gesloten is?

2. **De 44 negatieve-existentie-aannames (`s01`–`s08`, plus `s09`–`s10`): hele collectie sluiten of rubrieken in het
   model:**
   Gedeeltelijke of gefilterde collectiesluiting in de engine (bijv. *"geen eindigingsgrond met reden X"*) bouwt
   `uchar` niet, omdat dat predicaatsubsumptie/SMT over afgeleide filters vereist en `openInputs` onhandelbaar maakt.
   De engine kan hooguit **per hele `0..n`-rol** onderscheid maken tussen `open` en `closed`
   (`{"state": "closed", "items": [...]}`).
   - Toets per collectie (`eindigingsgronden`, `uitsluitingsgronden`, `samenloop -> rechten`, `inkomens`) op
     `s01`–`s08` (en `s09`–`s10`): is sluiting op **hele-collectie-niveau** voor een voorportaal of behandelaar
     werkbaar (het voorportaal zet de collectie pas op `closed` zodra de relevante uitvraagvragen zijn doorlopen), en is
     een melding op relatieniveau in `?openInputs=true` dan specifiek genoeg?
   - Of zijn bepaalde deelvragen (zoals *WIA/WGA/IVA-samenloop* naast een nieuw WW-recht, of *beschikbaarheid /
     rechtmatig verblijf*) zulke zelfstandige vragen dat ze in het model beter een eigen rol of toetskenmerk krijgen,
     zodat `openInputs` en de uitleg die vraag bij naam noemen?

3. **`Inkomen in-een-kalendermaand` (`hww-16`):**
   `Inkomen` gedraagt zich als een maandreeks (`Maand -> Bedrag`) in plaats van losse gebeurtenissen (zichtbaar bij
   `I8`/`I10` met `0,00 €` in `s01`/`s08`, en `I1`/`I2` in `s04`/`s09`).
   - Hoe ziet het schoonste patroon er in `v2` uit zolang strikte maandtijdlijnen (`voor elke maand`) een apart
     vervolgspoor zijn (`20261126-strict-timelines.md`)?
   - Werkt expliciete aanlevering van de relevante beoordelingsmaanden (met bedrag `0,00 €`, bedrag `> 0 €`, of
     weggelaten bedrag = `open` op die maandinstantie, plus een gesloten collectie voor het venster) voor alle
     scenario's? Waar wringt het (bijv. `eerste kalendermaand zonder overschrijding`, `gewenste maand`, of perioden
     waarin geen inkomensgrond speelt)?

4. **Open punt 52 (`(mag opgegeven worden)` op `einddatum-van-een-eindigingsgrond`):**
   `opgegeven X` kan in `uchar` niet algemeen open-tenzij-`is leeg` worden, want dan blokkeert een weggelaten override
   altijd de berekening `berekende X` (zoals de afgeleide einddatum van grond 01 of andere berekende opgaveleden). De
   asymmetrie ontstaat doordat voor andere eindigingsgronden (zoals detentie, verblijf buitenland, zelfstandig werk,
   niet-beschikbaarheid) geen berekende regel bestaat en *"duurt voort"* en *"nog niet ingevuld"* daar samenvallen in
   *vaststaand leeg*.
   - Toets hoe het model eruitziet als `Eindigingsgrond` hiervoor een expliciet kenmerk krijgt (bijv. `is beëindigd` /
     `duurt voort`, afgeleid waar de einddatum uit een ander feit volgt en anders open invoer): lost dat de asymmetrie
     met `datum einde uitsluitingsgrond` schoon op in regels, scenario's, uitleg en `openInputs`? Dit is de
     voorkeursroute die `20261126-open-fallback-opgave.md` eerst getoetst wil zien.
   - Loop ook alle overige `(mag opgegeven worden)`-leden in `v2/fundament` en `v2/herleving-ww` langs
     (`datum aanvang eindigingsgrond`, `beëindigingsdatum`, `herlevingsdatum`, `maxdatum`, `uitkeringsduur`,
     `bedrag maandloon`, `aow-datum`) om te zien of dezelfde asymmetrie elders optreedt.

5. **Controle op stap 1 (`to-one`-rollen in `v2/herleving-ww`):**
   - Loop alle `Feittype`-declaraties met `één` in `v2/fundament/rules/fnd-06-feittypen.rgs` (en eventuele overige
     `fnd-*`/`hww-*`-bestanden) langs: zijn er `to-one`-rollen die bij weglaten juist **standaard gesloten** ("er is er
     geen") zouden moeten zijn met `(volledig gegeven)`, of hoort elke weggelaten `to-one`-rol daar `open` te zijn
     tenzij `- de|het <rol> is leeg` / `{"state": "empty"}`?
   - Controleer of er in `v2/fundament` + `v2/herleving-ww` een objecttype is met een attribuut én een bereikbare
     `to-one`-rol met exact dezelfde naam (naamconflict voor `- de|het <rol> is leeg` en `"roles"`).

## Bronnen (in de werkbank)

- `rulesets/regelspraak-rules/v2/fundament/rules/*.rgs` (met name `fnd-06-feittypen.rgs`, `fnd-03`, `fnd-04`,
  `fnd-07`, `fnd-08`, `fnd-10`)
- `rulesets/regelspraak-rules/v2/herleving-ww/rules/*.rgs` (alle 20 bestanden)
- `rulesets/regelspraak-rules/v2/herleving-ww/README.md` (`## Strikt`, open punten 51 en 52)
- `eval/datasets/herleving-ww/scenarios/s01*` t/m `s10*` (`situatie.md`, `vragen/*.md`)

Hangt samen met: `20261126-role-and-existence-expectations.md`.

## Omvang

Klein tot middel: één gerichte lees- en analysesessie.
