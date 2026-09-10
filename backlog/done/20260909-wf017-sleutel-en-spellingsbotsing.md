# WF-017: de sleutel- en de spellingsbotsing, allebei zonder check

Goal: WF-017 (`spec/IR-SPEC.md` ch. 2.9) is een **gepromoveerde** MUST met twee
conjuncten — een dagsoortspelling botst niet met een ingebouwde kalendereenheid
en niet met de spellingen van een ándere dagsoort — en de verifier controleert
daarvan vandaag één helft van één conjunct. Deze kaart sluit de twee gemeten
gaten, of legt per gat vast waarom hij open blijft. ch. 2.9 belooft deze kaart
sinds 2026-09-09 met zoveel woorden ("Closing it is WF-017's own card").

## Wat de verifier vandaag doet

`wf017` in `spec/verifier/lib/checks.ml` loopt uitsluitend `ctx.doc.dagsoorten`
— de lijst met **gedeclareerde enkelvoudsnamen** — en vergelijkt elke naam met
de achttien ingebouwde kalendereenheden. Meer niet. De registry
`dagsoortPerEenheid`, waarin élke geregistreerde spelling als sleutel staat,
wordt door WF-017 nergens gelezen.

Vier subgevallen, twee gedekt en twee niet:

| # | botsing | gedekt door |
| --- | --- | --- |
| 1 | gedeclareerde naam × ingebouwde kalendereenheid | **WF-017** |
| 2 | geregistreerde SPELLING (map-sleutel) × ingebouwde kalendereenheid | **niets** — gat (a) |
| 3 | gedeclareerde naam × gedeclareerde naam | **WF-001** (`ns "dagsoorten"`, ci) |
| 4 | geregistreerde SPELLING × spelling van een ándere dagsoort | **niets** — gat (b) |

De waardehelft van dezelfde map is sinds 2026-09-09 wél gekruist: WF-034
arm (s) (ch. 4.6). Die arm is een **resolutieregel** en raakt geen van deze
vier — hij vergelijkt geen twee spellingen.

## Gat (a): een map-sleutel die een kalendereenheid schaduwt

Gemeten 2026-09-09. Een handgeschreven wire-document met de registry-ingang
`"dagen": "schouwdag"` naast de gewone ingangen is **`verifier: PASS`**. De
sleutel `dagen` wordt daarmee door `day_kind_unit` toegelaten als duureenheid
naast de negen kalendereenheden (WF-035/WF-038), dus `datum + 5 dagen` kan in
zo'n document als dagsoortrekenkunde gelezen worden in plaats van als gewone
kalenderrekenkunde.

De vergelijking die het gat scherp maakt: dezelfde spelling als **gedeclareerde
naam** wordt wél geweigerd —

    WF-017 FAIL /dagsoorten: day-kind 'dagen' shadows a built-in calendar unit

**De producent is hier strenger dan de IR.** `DefinitionResolver`'s
`registreerDagsoortEenheid` weigert een spelling die met een ingebouwde
tijdseenheid samenvalt, geeft een `WARNING` en registreert de ingang niet. En
sterker nog: de ingebouwde eenheidsnamen zijn grammaticasleutelwoorden, dus de
brontaal kan de vorm niet eens uitdrukken —

    $ printf 'Dagsoort de vaardag (mv: weken);\n…' > builtin2.rgs
    $ java -cp <antlr>:frontends/regelspraak/out \
        io.github.yhavinga.regelspraak.resolver.Resolver --ir out.json builtin2.rgs
      WARNING (line 0): dagsoort-eenheid 'weken' valt samen met een ingebouwde
                        tijdseenheid en wordt niet als dagsoort-eenheid geregistreerd
      ERROR (line 1): syntaxfout: mismatched input 'weken' expecting {UPPER_ID, LOWER_ID}
    frontend exit=1   (geen ir geëmitteerd)

Dit is dus een **wire-only** vorm: geen enkel door deze frontend geproduceerd
document kan hem dragen, en precies daarom is het een wire-regel of niets.

## Gat (b): de tweede conjunct heeft geen enkele check

Dit is het scherpste van de twee, want het is **end-to-end aantoonbaar met de
eigen frontend, zonder één diagnostiek**. Bron (eigen materiaal):

    Dagsoort de schouwdag (mv: schouwdagen);
    Dagsoort de schouwdagen (mv: schouwdagens);

    Regel Dagsoortdefinitie schouwdag
        geldig altijd
            Een dag is een schouwdag
            indien de dag later is dan 28-02-2026.

    Regel Dagsoortdefinitie schouwdagen
        geldig altijd
            Een dag is een schouwdagen
            indien de dag later is dan 30-09-2026.

Reproductie, gemeten 2026-09-09:

    $ java -cp <antlr>:frontends/regelspraak/out \
        io.github.yhavinga.regelspraak.resolver.Resolver --ir collide-ir.json collide.rgs
    ir: objecttypen=0 feittypen=0 resolvedRegels=0 geschreven naar collide-ir.json
    frontend exit=0        ← geen WARNING, geen ERROR

    dagsoorten:         ["schouwdag", "schouwdagen"]
    dagsoortPerEenheid: {"schouwdag":    "schouwdag",
                         "schouwdagen":  "schouwdag",     ← eigen enkelvoud, verkeerd gebonden
                         "schouwdagens": "schouwdagen"}

    $ verifier.exe collide-ir.json
    verifier: PASS

De tweede dagsoort heet `schouwdagen`. Haar **eigen enkelvoudsspelling** is al
bezet door het meervoud van de eerste dagsoort, en `registreerDagsoortEenheid`
gebruikt `putIfAbsent` — first-wins, stil. Gevolg: `5 schouwdagen` resolveert
naar dagsoort `schouwdag`, niet naar `schouwdagen`. Een attribuut met
`eenheid schouwdagen` rekent dus met de verkeerde dagsoortdefinitie, en niets
in de keten zegt er iets van: de frontend niet (geen diagnostiek), de verifier
niet (PASS), en WF-001 niet (de twee gedeclareerde namen `schouwdag` en
`schouwdagen` botsen niet).

**Hier is de producent even zwak als de IR.** Anders dan bij gat (a) is dit
geen "IR zwakker dan zijn producent" maar een gat aan beide kanten — de
v0-mirror-notitie bij WF-017 zegt het zelf al ("the day-kind/day-kind collision
is silently first-wins"), alleen zonder dat iemand had gemeten wat die stilte
kost.

## Wat er te beslissen valt

1. **Waar landt de check?** Twee kandidaten met verschillende reikwijdte:
   (i) op `dagsoorten` × `dagsoortPerEenheid`-sleutels in de verifier (dekt
   gat (a) en gat (b) allebei, want beide zijn sleutelbotsingen), of (ii) een
   sleutel-uniciteitsregel plus een aparte ingebouwde-eenheidsregel. Merk op
   dat gat (b) op de wire **niet als botsing zichtbaar is**: de map heeft per
   sleutel één waarde, dus de verliezende registratie is er domweg niet meer.
   Wat de verifier wél kan zien is dat een gedeclareerde dagsoortnaam
   (`schouwdagen`) als SLEUTEL naar een ándere dagsoort (`schouwdag`) wijst —
   dat is de wire-zichtbare vorm van dit defect en de meest waarschijnlijke
   clausule.
2. **Frontend of wire of allebei?** Gat (b) verdient bijna zeker óók een
   frontend-diagnostiek (de `putIfAbsent` die stil verliest). Dat is
   `regelspraak-java`-werk aan de andere kant van de pin en dus spec-first:
   eerst de clausule hier, dan de conformance-PR daar.
3. **Blijft WF-017 één regel?** Vier subgevallen, twee regels (WF-001 draagt
   nu subgeval 3). Een eigen nummer voor de sleutelbotsing is te verdedigen.

## Acceptance

- Rood-eerst: per gesloten gat één negatief dat onder de verifier van vóór de
  kaart PASS is en daarna één regel met de regel-ID en de positie.
- Arm-exact: alleen die clausule weg → alleen de nieuwe getuige(n) weer schoon,
  over de volle vier-zone-sweep.
- Nul over-afwijzing, direct gemeten over alle vier zones, met **gedateerde**
  tellingen en met baseline en landed-stand apart benoemd.
- ch. 2.9 vertelt na afloop de werkelijke dekking (nu doet hij dat ook al, maar
  als gat-beschrijving); de belofte "Closing it is WF-017's own card" wordt
  vervangen door de landing.
- Voor gat (b) een positieve getuige die de goede binding vastlegt, want een
  negatief pint een weigering en nooit een niet-weigering.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `corpus/golden/**` (een positieve getuige; de
verifier-only kast `jutterswet-vormenkabinet` is de goedkoopste plek — hij is
al `NOCASE` gepind in gate 12, zodat er geen pin in het beschermde `scripts/`
mee hoeft), `backlog/`.

## Out of scope

- De waardehelft van `dagsoortPerEenheid`: dicht sinds 2026-09-09, WF-034
  arm (s), ch. 4.6.
- De dagsoort-duurvormen zelf (§6.10/§6.11) — eigen semantiekkaart.
- `Constante.naam` — eigen kaartje,
  `20260909-constante-naam-gesloten-verzameling.md`.

---

## Result (2026-09-10)

**Geland: twee ARMEN onder WF-017**, geen nieuw nummer — dezelfde gepromoveerde
MUST, nu gecontroleerd waar élke geregistreerde spelling staat. Arm (a): geen
sleutel van `dagsoortPerEenheid` valt samen met een van de achttien ingebouwde
kalendereenheden. Arm (b): een gedeclareerde dagsoortnaam die als sleutel
voorkomt bindt aan ZICHZELF. Beide vergelijken onder de ch. 2.1-identiteit
(witruimte inklappen, kleine letters) — dezelfde identiteit waarmee een
consument een spelling in het register opzoekt.

### 1. Hermeting van beide gaten op deze HEAD (2026-09-10)

Beide gaten bestonden nog. Gemeten met de verifier van vóór de kaart, md5
`67becaba23c6d3364a9f1a080355541e`.

**Gat (a)**, handgeschreven wire-document (`jutterswet-schouwrooster` plus één
ingang, verder niets gewijzigd):

    $ verifier.exe corpus/negatief/wf017-eenheidsleutel-schaduwt-kalendereenheid/ir.json
    verifier: PASS

De ingang is `"dagen": "schouwdag"` naast de vier gewone ingangen; de sleutel
wordt daarmee door de dagsoort-eenheidslezing toegelaten als duureenheid naast
de kalendereenheden (WF-035/WF-038). Dezelfde spelling als GEDECLAREERDE naam
was al `WF-017 FAIL`. Ook de producentkant hermeten:

    $ make ir FILE=builtin.rgs        # Dagsoort de vaardag (mv: weken);
      WARNING (line 0): dagsoort-eenheid 'weken' valt samen met een ingebouwde
                        tijdseenheid en wordt niet als dagsoort-eenheid geregistreerd
      ERROR (line 1): syntaxfout: mismatched input 'weken' expecting {UPPER_ID, LOWER_ID}
    frontend exit != 0, geen ir geëmitteerd

Dus wire-only — maar de grond is de REGISTRATIEWACHT, niet de grammatica. In de
fixronde alle achttien ingebouwde namen als dagsoort-meervoud gemeten:
**veertien** geven een parse-fout (exit ≠ 0, geen IR), **vier** parsen
probleemloos — `uren`, `minuten`, `seconden`, `milliseconden` — en komen er met
exit 0 door, waarna de wacht in `registreerDagsoortEenheid` (die alle achttien
dekt) de ingang alsnog laat vallen. Voor die vier landt de WARNING niet op de
console maar in het `diagnostics`-veld van het geëmitteerde document:

    $ make ir FILE=bi-uren.rgs        # Dagsoort de vaardag (mv: uren);
    ir: … geschreven naar bi-uren.json        frontend exit=0
    dagsoortPerEenheid: {"vaardag": "vaardag"}    <- 'uren' niet geregistreerd
    diagnostics: [{"severity":"WARNING","message":"dagsoort-eenheid 'uren' valt
                   samen met een ingebouwde tijdseenheid en wordt niet als
                   dagsoort-eenheid geregistreerd","line":0}]

Geen enkel door deze frontend geëmitteerd document draagt de vorm dus, en dat
is wat wire-only draagt.

**Gat (b)**, end-to-end door de eigen frontend (jars in het gitignored `lib/`;
de bron van `frontends/` is niet aangeraakt):

    $ make ir FILE=collide.rgs OUT=collide-ir.json
    ir: objecttypen=0 feittypen=0 resolvedRegels=0 geschreven naar collide-ir.json
    frontend exit=0                      <- geen WARNING, geen ERROR

    dagsoorten:         ["schouwdag", "schouwdagen"]
    dagsoortPerEenheid: {"schouwdag":    "schouwdag",
                         "schouwdagen":  "schouwdag",
                         "schouwdagens": "schouwdagen"}

    $ verifier.exe collide-ir.json
    verifier: PASS

De stille first-wins-emissie (`putIfAbsent`) reproduceert onveranderd, en de
verifier van vóór de kaart liet hem door. Dat emitteerde bestand is, byte voor
byte, de negatief-getuige geworden.

### 2. Rood eerst, groen erna — per arm, met echte output

Beide getuigen waren PASS onder de verifier van vóór de kaart (hierboven). Erna,
md5 `5eca78cd1b651eb797da934e8f915abf`:

    $ verifier.exe corpus/negatief/wf017-eenheidsleutel-schaduwt-kalendereenheid/ir.json
    WF-017 FAIL /dagsoortPerEenheid/dagen: registered day-kind unit spelling
    'dagen' shadows a built-in calendar unit
    exit=1

    $ verifier.exe corpus/negatief/wf017-dagsoortnaam-bindt-vreemde-dagsoort/ir.json
    WF-017 FAIL /dagsoortPerEenheid/schouwdagen: declared day kind 'schouwdagen'
    is registered as a unit spelling of day kind 'schouwdag': its own spelling
    binds to another day kind
    exit=1

Eén regel per getuige, en de melding benoemt de arm: arm (a) noemt de
GEREGISTREERDE SPELLING tegenover de kalendereenheid, arm (b) de GEDECLAREERDE
naam die aan een ándere dagsoort bindt; de positie
(`/dagsoortPerEenheid/<sleutel>`) onderscheidt beide van de oude clausule over
`/dagsoorten`.

### 3. Arm-exactheid en over-afwijzing, over de volle sweep

Sweep = 585 documenten: `corpus/golden` 167 + `corpus/rood` 26 +
`corpus/negatief` 362 + de read-only pariteitszone `toka/` 30. Elk document
gedraaid met beide binaries, rapporten regel voor regel vergeleken.

| meting | uitkomst |
| --- | --- |
| vóór → na, hele kaart | **2** verdicten bewegen: de twee nieuwe getuigen. Geen andere verdict, geen andere rapportregel. Over-afwijzing: **nul** |
| arm (a) inert | alleen `wf017-eenheidsleutel-schaduwt-kalendereenheid` wordt schoon; verder niets beweegt |
| arm (b) inert | alleen `wf017-dagsoortnaam-bindt-vreemde-dagsoort` wordt schoon; verder niets beweegt |
| de positieve getuige toegevoegd | **0** verdicten, **0** rapportregels bewegen (onder beide binaries) |

Twee identiteitsmutanten, gemeten omdat een vergelijkingsvorm anders
onfalsifieerbaar is:

- arm (b) byte-exact in plaats van onder `norm` → meldt
  `/dagsoortPerEenheid/schouwdag` in `corpus/golden/jutterswet-vormenkabinet`
  (`"schouwdag": "SchouwDag"`) en haalt gate 3 onderuit;
- arm (b) vergelijkend met de EERSTE gedeclareerde dagsoort in plaats van met de
  EIGEN → meldt `jutterswet-schouwrooster` én `jutterswet-vormenkabinet`, plus
  drie extra regels in negatief-documenten.

### 4. De positieve getuige

In `corpus/golden/jutterswet-vormenkabinet` (verifier-only, geen case-bestand).
De kast declareerde één dagsoort, waardoor arm (b) er leeg draaide: met één
declaratie is elke oplosbare waarde de eigen. Hij declareert er nu twee, met
definitie, en registreert beide spellingen:

    "dagsoorten": ["schouwdag", "stormvloeddag"],
    "dagsoortPerEenheid": {
      "schouwdag":       "SchouwDag",
      "schouwdagen":     "  schouwdag  ",
      "stormvloeddag":   "stormvloeddag",
      "stormvloeddagen": "stormvloeddag"
    }

Drie positieve feiten: een gedeclareerde naam die byte-exact aan zichzelf bindt,
een gedeclareerde naam die alleen via de identiteit aan zichzelf bindt, en een
MEERVOUDSSLEUTEL die aan de eigen dagsoort bindt en met geen enkele andere
declaratie botst. Geen sleutel is een kalendereenheid, dus arm (a) zwijgt ook.
Eerlijk gemeld: de "eigen versus vreemde"-discriminatie wordt óók door
`jutterswet-schouwrooster` gepind — de kast is niet de enige pin en dat wordt
niet geclaimd. Wat de tweede declaratie oplevert is dat het document dat de
WF-index in WF-017's Golden-kolom noemt de positieve vorm zélf draagt in plaats
van een regel te getuigen die het niet kan uitoefenen. De NOTE legt dat vast,
inclusief de twee mutanten en de kostenmeting.

Gate 5 blijft hem melden als "NOT replayed, no case file yet" (geen nieuwe
verplichting, geen gefabriceerde case), gate 12 houdt hem `NOCASE`: geen enkel
beschermd script aangeraakt.

### 5. ch. 2.9 en de registerrij

ch. 2.9 is herschreven: de belofte "Closing both is WF-017's own card" is weg,
vervangen door de landing. De vier subgevallen staan er nu als **drie clausules
in twee posities**, met per clausule de regel die hem draagt: (1) gedeclareerde
naam × kalendereenheid → WF-017 over `/dagsoorten`; (2) registersleutel ×
kalendereenheid → **arm (a)**; (3) gedeclareerde naam × gedeclareerde naam →
WF-001; (4) geregistreerde spelling × spelling van een andere dagsoort →
**arm (b)**, in de enige vorm die de wire kan tonen. Daaronder: per arm de
meting, de identiteit met haar pin, en wat arm (b) BEWUST niet bereikt (een
sleutel die géén gedeclareerde naam is blijft vrij — de (s)-grond: de wire
draagt geen meervoudsVELD).

De registerrij noemt nu beide armen, de kast als golden ("twee dagsoorten, elk
aan zichzelf gebonden — één alleen via de ch. 2.1-identiteit — en een
meervoudssleutel die met niets botst") en de drie negatief-getuigen.

Meegetrokken omdat ze anders onwaar werden: ch. 4.6 (s) (drie passages over "de
sleutelkant is vrij" / "WF-017 leest deze map niet"), de WF-034 (s)-comment in
`checks.ml`, en de NOTE van `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort`.
De grens tussen (s) en WF-017 is scherper geworden in plaats van vager: (s)
leest de WAARDEN, de armen lezen de SLEUTELS, en arm (b) treedt terug waar ze
elkaar zouden raken — hij vuurt alleen als de waarde een WÉL gedeclareerde
dagsoort noemt, zodat een onoplosbare, lege of null-waarde de enkele diagnose
van (s) blijft. **Geen corpusdocument instantieert dat raakpunt** — de
bestaande WF-034-getuige niet: zijn foute sleutel `schouwdagen` is geen
gedeclareerde naam, dus arm (b) zwijgt daar via de gedeclareerde-naam-opzoeking
en komt aan de terugtred niet toe. Het terugtreden is daarom in de fixronde per
constructie gemeten — gedeclareerde sleutel `schouwdag` met waarde `havendag`,
met `null` en met `""`, drie voor de meting gebouwde documenten:

    WF-034 FAIL /dagsoortPerEenheid/schouwdag: … names day kind 'havendag', …
    WF-034 FAIL /dagsoortPerEenheid/schouwdag: … names no day kind at all: …
    WF-034 FAIL /dagsoortPerEenheid/schouwdag: … names day kind '', …

telkens precies één regel, en telkens WF-034's. De bestaande getuige is
ongewijzigd gebleven; zijn NOTE zegt nu wat hij wél en niet instantieert.

### 6. Conformance-kant: een frontend-diagnostiek, spec-first

Gat (b) verdient óók een producent-diagnostiek. `registreerDagsoortEenheid`
verliest stil (`putIfAbsent`) waar de spelling al bezet is; anders dan bij de
ingebouwde eenheid — waar de frontend wél waarschuwt en de ingang laat vallen —
zegt hij hier niets. Dat is werk aan de andere kant van de pin en dus
spec-first: de clausule landt hier, de gevendorde frontend
(`frontends/regelspraak`) krijgt de diagnostiek bij een latere kaart. **Deze
kaart raakt `frontends/` niet** — alleen read-only geëmitteerd voor de metingen
(de jars staan in het gitignored `lib/`; `git status --short` is leeg).

### 7. Gates

    scripts/check-self-contained.sh   check-self-contained: PASS
    scripts/check-ir-schema.sh        PASS (schema valid; 193 file(s) validate
                                      and round-trip, floor 192)
    scripts/check-verifier.sh         PASS (193 golden clean; 362 negatief
                                      rejected for their declared rule, floor 350)
    scripts/check-oracle.sh           PASS (ref suite 496 worked examples, floor
                                      489; 428 cases replayed exactly, floor 427;
                                      NOT replayed: jutterswet-seizoenvondst,
                                      jutterswet-vormenkabinet)

Gate 5 is meegedraaid omdat deze kaart een golden wijzigt; zijn
NOT-replayed-melding voor de kast is onveranderd. Geen tellingsvloer geraakt,
geen beschermd script aangepast.

De gates 9, 11, 12 en 13 kunnen op deze kaart structureel niet bewegen: de
gewijzigde golden krijgt geen case-bestand (dus geen kanaal in 12 — hij blijft
`NOCASE` — en geen twistpositie in 13, dat alleen goldens mét cases aanraakt),
hij draagt geen `bron.catala_en` (9) en geen fixture in de pariteitszone (11);
de negatief-getuigen liggen in een zone die alleen gate 3 leest. De fixronde-lens
draaide er negen groen.

### 8. Voorgestelde CONSTRUCTS-zin (rij 23, Day-kind definitions)

Aan te vullen achter de bestaande "**Landed (2026-09-09, WF-034 arm (s) …)**":

> **Landed (2026-09-10, WF-017 armen (a) en (b), IR-SPEC ch. 2.9):** de
> botsings-MUST loopt nu overal waar een spelling GEREGISTREERD staat, niet
> alleen over de gedeclareerde naam — arm (a): geen sleutel van
> `dagsoortPerEenheid` valt samen met een ingebouwde kalendereenheid (wire-only,
> gedragen door de registratiewacht die alle achttien namen dekt; de grammatica
> weigert er veertien, `uren`/`minuten`/`seconden`/`milliseconden` parsen wél en
> worden door de wacht gedropt met de waarschuwing in `diagnostics`); arm (b): een
> gedeclareerde dagsoortnaam die als sleutel voorkomt MOET aan zichzelf binden —
> de wire-zichtbare vorm van de stille first-wins-botsing die de map zelf
> verbergt (één waarde per sleutel). Beide onder de WF-001-identiteit; negatief
> `wf017-eenheidsleutel-schaduwt-kalendereenheid` en
> `wf017-dagsoortnaam-bindt-vreemde-dagsoort` (de eigen emissie van de
> frontend), goede binding positief gepind in `jutterswet-vormenkabinet`. De
> sleutelkant houdt geen RESOLUTIE-regel; de producent-diagnostiek op de stille
> `putIfAbsent` staat nog open aan de frontendkant.

De zinsnede "the two measured WF-017 key/spelling gaps are carded (backlog
20260909-wf017-sleutel-en-spellingsbotsing)" in dezelfde rij is daarmee
verouderd en kan vervallen.

### 9. Wat bewust niet bewoog

- **Geen nieuw WF-nummer.** De sleutelruimte-controle is dezelfde MUST op een
  tweede positie; het WF-059-armenpatroon is gevolgd (één functie, armen met
  eigen getuige en eigen meting).
- **De sleutelkant houdt geen resolutie-regel.** Een sleutel die geen
  gedeclareerde naam is blijft vrij (declaratief meervoud, afgeleid meervoud en
  afkorting zijn op de wire niet te onderscheiden) — de (s)-beslissing van
  2026-09-09 is ongewijzigd.
- **Arm (b) vuurt niet op een onoplosbare, lege of null-waarde**: dat blijft de
  enkele diagnose van WF-034 (s); geen document wordt tweemaal gemeld voor één
  defect.
- **`frontends/`, `spec/CONSTRUCTS.md`, `spec/PLAN.md` en alle `scripts/`**
  onaangeraakt. Geen case-bestand bij de kast, dus geen kanaalverschuiving in
  gate 12 en geen nieuwe verplichting in gate 5.
- **`corpus/negatief/wf017-daykind-shadows-builtin`** is onveranderd gebleven:
  hij pint clausule 1 en die is niet gewijzigd.

### 10. Fixronde (2026-09-10)

Een tweede lezer reproduceerde alles (beide sweeps, de mutanten, de
byte-identieke emissie inclusief canoniciteit, negen gates groen) en vond vier
tekstuele fouten. Wat daarop is gecorrigeerd, met de meting die het draagt:

1. **De grammatica-premisse was te sterk.** "De ingebouwde eenheidsnamen zijn
   grammaticasleutelwoorden, dus de brontaal kan de vorm niet uitdrukken" geldt
   voor **veertien** van de achttien namen. `uren`, `minuten`, `seconden` en
   `milliseconden` parsen als dagsoort-meervoud probleemloos (exit 0, IR
   geëmitteerd) en worden pas door de registratiewacht gedropt; hun WARNING
   staat in het `diagnostics`-veld van het geëmitteerde document, niet op de
   console. De wire-only-conclusie blijft staan maar rust nu op de wacht, die
   alle achttien dekt. Gecorrigeerd in ch. 2.9, in de arm-(a)-comment in
   `checks.ml` en in de NOTE van de arm-(a)-getuige.
2. **De terugtred-meting citeerde het verkeerde document.**
   `wf034-dagsoorteenheid-onbekende-dagsoort` instantieert het raakpunt niet
   (zijn foute sleutel is geen gedeclareerde naam), dus zijn schone
   één-regel-rapport bewijst de terugtred niet. Het terugtreden is per
   constructie gemeten (§5 hierboven). Gecorrigeerd in ch. 4.6 (s), in de
   (s)-comment in `checks.ml` en in de NOTE van die getuige — de getuige zelf
   is niet aangeraakt.
3. **De positieve-golden-motivering claimde te veel.** De twee mutanten worden
   elk óók elders gevangen (de byte-exacte al door de pre-kaart-toestand van de
   kast zelf, de eerste-declaratie-mutant door `jutterswet-schouwrooster`). De
   NOTE noemt nu de eerlijke en voldoende grond: index-hygiëne — het document
   dat de WF-index in WF-017's Golden-kolom noemt draagt de positieve vorm zelf.
4. **Eén telling week af** ("582 documents" in een comment) — nu 585, gelijk aan
   alle andere plekken.

Kleiner: het verouderde twee-ingangen-fragment in de kast-NOTE toont nu eenmalig
de vier ingangen, de weesregel in ch. 4.6 is herwikkeld, de kwalificatie bij
tabelrij 4 van ch. 2.9 staat nu in de cel zelf, en
`backlog/new/20260910-dagsoort-semantiek.md` is bijgewerkt (het pad wijst naar
`done/`, en de sleutelgaten heten daar niet langer open — wat van WF-017
openstaat is alleen de frontend-diagnostiek).

`spec/CONSTRUCTS.md` bleef ook in deze ronde onaangeraakt: de rij-23-achterblijver
en de §8-zin landen serieel bij de merge, zoals het registerkanaal voorschrijft.
