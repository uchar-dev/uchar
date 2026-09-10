# De IR-wire-kant van de Drools-datumlezer: twee kale parses, en het schema dekt ze niet

Goal: beslissen of de twee kale `LocalDate.parse` op de **IR**-wire dezelfde
patroonklem plus getuige krijgen die de case-wire op 2026-09-10 kreeg, of dat
de kale lezing met een reden blijft staan. De kaart levert het besluit plus,
bij "klem", de klem en zijn getuige. Het gaat om twee aanroepen op één plek.

## De stand (gemeten 2026-09-10)

`loader/IrLoader.java:545-546`, in `geldigheid(JsonNode)`:

```java
return new Geldigheid(vanaf == null ? null : java.time.LocalDate.parse(vanaf),
        tot == null ? null : java.time.LocalDate.parse(tot));
```

Na de case-wire-kaart (`backlog/done/20260909-date-caselezer-klem.md`) zijn
dit de **enige** kale date-parses die nog in `backends/drools/src/main`
staan, naast de twee parses ín de kalendertakken van de klemmen zelf
(`CaseRunner` r. 948 en r. 977, waar het patroon al is afgedwongen).

**Het schema dekt deze twee niet.** Dat is het verschil met de case-wire en
de reden dat dit een eigen kaart is. In
`spec/schema/uchar-case-v0.schema.json` draagt elke datumpositie
`^[0-9]{4}-[0-9]{2}-[0-9]{2}$`; in `spec/schema/regelspraak-ir-v0.schema.json`
staat er bij `$defs/Geldigheid`:

```json
"vanaf": { "type": ["string", "null"] },
"tot":   { "type": ["string", "null"] }
```

Geen patroon. Gate 2 laat dus elke string door op deze twee posities, en de
lezer erachter is `LocalDate.parse`, die — gemeten in de case-wire-kaart —
de tekenjaarvormen `+012026-03-15` (jaar 12026) en `-0500-01-01` (jaar −500)
aanvaardt. De aanname "de IR-wire is schema-gedekt, dus de klem is daar
overbodig" is dus onjuist en moet niet als grond gebruikt worden.

**Wat het corpus draagt.** Over `corpus/golden/**` plus `corpus/rood/**`:
846 `vanaf`/`tot`-posities, waarvan 836 `null` en 10 met een waarde; de
distinct waarden zijn `2025-01-01`, `2025-12-31`, `2026-01-01`,
`2026-12-31`. Alle vier binnen het patroon. De klasse is dus smal — maar dat
was hij op de case-wire ook, en daar bleek hij niet leeg.

## De vraag die beslist moet worden

Drie dingen zijn ongemeten en de kaart moet ze meten voordat ze beslist:

1. **De vormklasse.** Wat aanvaardt `LocalDate.parse` hier dat de andere
   lezers van hetzelfde IR-document weigeren? Vermoedelijk dezelfde twee
   tekenjaarvormen, maar dat is een vermoeden tot het gedraaid is — en de
   tegenlezer is hier een andere (`ref`'s IR-loader, `spec/verifier/`), niet
   `ref`'s case-loader.
2. **De producent.** Wie schrijft `Geldigheid.vanaf`/`tot` op de wire? Als
   dat uitsluitend de eigen emitter is, is de producentroute smaller dan die
   van de case-wire (waar een generator of een handmatige run legitiem is) en
   weegt het consistentieargument zwaarder dan het lekargument. Meet het,
   beredeneer het niet.
3. **Wat de klem zou moeten volgen.** Op de case-wire volgde de klem `ref`,
   en dat viel samen met het schemapatroon. Hier is er geen schemapatroon om
   mee samen te vallen, dus de kaart moet expliciet kiezen: `ref`'s
   IR-lezer, de verifier, of een patroon dat als schemavoorstel meegaat.

Een vierde uitkomst is toegestaan en misschien de beste: **het schema
repareren** in plaats van (of naast) de lezer, want een IR-wire zonder
patroon op een datumveld is zelf de afwijking. `spec/schema/` is protected —
dat wordt dan een voorstel, geen merge.

## Acceptance

- Het besluit staat in het Result, met de drie metingen hierboven erbij.
- Bij "klem": de klem staat er, weigert de gemeten vormen hard (nooit een
  `Excluded`), noemt waarde + veld + verwachte vorm, en krijgt een getuige
  die rood-eerst is getoond, met de spiegelassertie en de randbreedtes van
  beide kanten gepind — het patroon van `CaseWireDatumWitnessTest`.
- Bij "geen klem": de reden staat als commentaar op de regel, en de kaart
  legt vast dat de vormen bekend en aanvaard zijn.
- Bij een schemavoorstel: de diff staat op de tak als voorstel, niet gemerged.
- Gate 12 groen met stijgende JUnit-teller bij "klem"; gate 2 groen; geen
  andere telling beweegt. Gate 1 groen.

May touch: `backends/drools/**`, `backlog/`, en — als voorstel, niet
mergen — `spec/schema/regelspraak-ir-v0.schema.json`.

## Out of scope

- De case-wire-routes en hun klem: die landden 2026-09-10 en zijn klaar.
- De datum-LITERAL-lezer (WF-084) — andere lezer, eigen regel, eigen getuige.
- Andere lezers van hetzelfde IR-document (`ref`, `spec/verifier/`,
  `frontends/`): die mogen als TEGENMETING gedraaid worden, maar deze kaart
  wijzigt ze niet.

## Result (2026-09-11)

**Besluit: BEIDE kanten dicht, wire-first.** Een schemapatroon op
`Geldigheid.vanaf`/`tot` (voorstel, beschermd pad) én de klem in
`IrLoader.geldigheid`. De vierde uitkomst die de kaart openhield — het schema
repareren — is dus genomen, en niet in plaats van maar naast de lezer.

**Terugdraaibaarheid, voor het record.** Dit besluit is in de hoofdsessie
genomen terwijl de maintainer offline was, langs de gevestigde lijnen (het
case-schema draagt het patroon al overal; de case-wire-klem van 2026-09-10 is
de vorm die hier is nagevolgd). De twee helften staan in TWEE aparte commits,
zodat elke helft los kan worden teruggedraaid of geweigerd: `104d1d4` is de
klem plus getuige (`backends/drools`, gewoon pad), `608202d` is het
schemavoorstel (`spec/schema`, beschermd — niet mergen zonder review). De klem
staat op eigen benen: hij is niet afhankelijk van het patroon en blijft
correct als het voorstel wordt afgewezen. Omgekeerd geldt hetzelfde.

### De drie metingen die de kaart eiste

**1. De vormklasse.** Anders dan de kaart vermoedde is dit géén herhaling van
de case-wire. Daar weigerden de tegenlezers hard en aanvaardde Drools stil;
hier weigert op de IR-wire NIEMAND. `ref` (`eval.ml:83`), clingo
(`translate.ml:311`) en de verifier (`loader.ml:862`) houden een grens als
ONDOORZICHTIGE STRING en ordenen hem lexicografisch — de verifier schrijft die
aanname letterlijk op naast zijn overlapcontrole (`checks.ml:80-81`, "ISO
dates compare lexicographically"), en WF-021 steunt erop. Drools was de enige
die het lexeem parseerde. Gemeten op een met de hand bewerkte kopie van
`corpus/golden/jutterswet-vensterheffing` (twee regelversies, een case binnen
en een case vóór het venster), de `vanaf` van versie 1 vervangen; alle
mutaties in `/tmp`, geen corpusbestand aangeraakt:

| lexeem | verifier | ref | clingo | Drools (vóór de klem) |
| --- | --- | --- | --- | --- |
| `2026-01-01` (schoon) | PASS | PASS / PASS | PASS / PASS | PLAYED 1 / PLAYED 1 |
| `+012026-01-01` | PASS | PASS / MISMATCH | PASS / MISMATCH | **FAIL / PLAYED 1** |
| `-0500-01-01` | PASS | PASS / MISMATCH | PASS / MISMATCH | PLAYED 1 / FAIL |
| `2026-02-30` | PASS | PASS / PASS | PASS / PASS | ERROR / ERROR (kale parse-fout) |
| `niet-een-datum` | PASS | MISMATCH / PASS | MISMATCH / PASS | ERROR / ERROR (kale parse-fout) |

(twee uitslagen per rij = `case1-binnen-venster` / `case2-voor-venster`.)

Drie klassen, en ze wijzen niet dezelfde kant op:

- **Rij 2 is de divergentie.** `+012026-01-01` sorteert vóór elk viercijferig
  jaar, dus ref en clingo passen de regelversie toe; Drools maakt er jaar
  12026 van en past hem niet toe. Eén document, twee antwoorden — en op
  `case2` is het precies de gemeten divergentieklasse van de case-klem-kaart:
  Drools meldt `PLAYED 1` waar ref en clingo `MISMATCH` melden.
- **Rij 3 is de stille helft.** Jaar −500 en de string `-0500-01-01` ordenen
  allebei vóór de rekendatum, dus alle lezers zeggen hetzelfde en er is geen
  DIVERGENTIE om te zien: `case2` wordt in alle drie de lezers tegelijk rood,
  wat er voor de differentiaal uitziet als een gewone verkeerde verwachting.
  Een corpus met alleen `case1` had helemaal niets gezien — daar melden alle
  drie gewoon PASS respectievelijk PLAYED.
- **Rij 4 en 5 zijn de omgekeerde asymmetrie.** Daar is Drools de enige die
  iets merkt, en hij zegt het met een kale `DateTimeParseException` die noch
  het veld noch de verwachte vorm noemt. Rij 5 is het wijdste gat: voor de
  verifier is een venster met de grens `niet-een-datum` een welgevormd
  document.

**2. De producent.** Smaller dan op de case-wire, maar niet leeg, en op één
punt breder dan verwacht.

- De **RegelSpraak-frontend** schrijft `LocalDate.toString()`
  (`IrEmitter.java:897`). Dat is de exacte inverse van `LocalDate.parse` en
  schrijft dus ZELF de getekende, uitgebreide vorm voor een jaar buiten
  0000–9999. De formatter is dus geen hek. Wat het wél is, is de grammatica
  ervóór: de `DATE`-token pint het jaar op exact vier cijfers
  (`RegelSpraakLexer.g4:222`) en `datumOfJaar` laat verder alleen een
  vierjarig `NUMBER` toe, dus uit een conforme bron kan deze producent geen
  buiten-patroon-lexeem halen.
- De **Catala-frontend** schrijft altijd `{vanaf: null, tot: null}`
  (`frontends/catala/lib/translate.ml:118`) — nooit een waarde.
- **Handgeschreven documenten** zijn de derde route en de enige onbegrensde:
  de drie corpusdocumenten die een grens dragen zijn met de hand geschreven,
  waaronder een ankergolden. Daar is geen grammatica die de vorm bewaakt, en
  tot dit voorstel ook geen schema.

Dat maakt het consistentieargument zwaarder dan het lekargument — precies de
weging die de kaart voorzag — maar het lekargument valt niet weg: de IR is een
gepubliceerde, engine-neutrale wire, en de hand-route is er nu al.

**3. Wat de klem volgt.** Hier moest de kaart kiezen, en de eerlijke uitkomst
is dat de derde optie de enige verdedigbare was. `ref` volgen kan niet: `ref`
heeft op deze positie geen vorm om te volgen — het parseert de grens niet en
vergelijkt hem als string. De verifier evenmin. De klem volgt dus **het
voorgestelde schemapatroon plus de kalender**, en dat patroon is niet nieuw
bedacht: het is letterlijk WF-084's datum-literalgrammatica
(`^[0-9]{4}-[0-9]{2}-[0-9]{2}$` plus een echte kalenderdag, IR-SPEC ch. 4.1)
toegepast op de andere datumpositie van dezelfde wire. Dat de Drools-lezer
daarmee de enige is die weigert, is een bewuste asymmetrie met de bekende
grond: elke lezer houdt zijn eigen hek. De kant die daarmee open blijft — de
verifier heeft er nog geen — is gekaart:
`backlog/new/20260911-geldigheidsgrens-verifierhek.md`.

### De schema-sweep

Het IR-schema droeg **nul** `pattern`-sleutels; het case-schema draagt er tien.
Dat is de asymmetrie waar de kaart naar wees, en ze is breder dan de twee
posities: het is de eerste `pattern` in dit schema überhaupt.

Alle string-toelatende posities van het schema zijn langsgelopen: **146
benoemde** (62 distincte eigenschapsnamen; 144 die hun `string` rechtstreeks in
hun eigen `type` dragen, plus `Fold.function` en `MapList.function`, die het
via een `oneOf`-tak naast een `Lambda` toelaten — die twee ontbraken in de
eerste telling van 144 en zijn hier nagemeten) **plus 15 anonieme**
(map-waarden en array-items: dagsoortnamen, enumeratiewaarden, navigatiepad-
stappen, dimensielabels, sleutel- en ledenlijsten, bronannotaties). Geen van
die 15 is datumdragend, en de twee `oneOf`-posities evenmin — het zijn
functienamen. Datumdragend zijn er precies twee soorten:

| positie | vorm | besluit |
| --- | --- | --- |
| `Geldigheid.vanaf` / `.tot` | onvoorwaardelijk een datum of `null` | **gepatroneerd** |
| `Literal.waarde` | polymorf: hangt af van `type` | ongemoeid |

`Literal.waarde` draagt in het corpus 284 getallen, 61 booleans, 16 teksten, 5
percentages, 5 enumeratiewaarden, 18 datums en 2 datumtijden. Eén patroon per
positie kan dat niet dekken, en het hoeft ook niet: WF-084 en WF-095 normeren
die twee lexemen al, met kalendercontrole, in de verifier. `TemporalRead.datum`
klinkt als een datumpositie maar is een `expr`, geen string. Over alle 161
posities samen draagt verder geen enkele een datum; de sweep is daarmee
volledig en geen steekproef.

**Hertelling vóór de wijziging** (de kaart telde 846/836/10; main is sinds die
meting doorgelopen): **848** `vanaf`/`tot`-posities over 194 documenten in
`corpus/golden` plus `corpus/rood`, **839 null**, **9 met waarde**, distinct
`2025-01-01`, `2025-12-31`, `2026-01-01`, `2026-12-31` — alle vier binnen het
patroon, verdeeld over drie documenten (`anchor/jutterswet-jutheffing`,
`jutterswet-vensterheffing`, `jutterswet-vormenkabinet`). Gate 2 daarna: PASS,
194 documenten valideren en round-trippen.

Het patroon bijt ook echt, gemeten met dezelfde vier mutanten tegen het
gewijzigde schema:

```
+012026-01-01   REJECTED  /resolvedRegels/0/geldigheid/vanaf: does not match '^[0-9]{4}-…$'
-0500-01-01     REJECTED  (idem)
niet-een-datum  REJECTED  (idem)
2026-02-30      accepted by the schema
2026-01-01      accepted by the schema
```

De meldingstekst hierboven is die van `best_match`, wat gate 2 zelf gebruikt:
pad en patroon staan er precies in.

**Maar: het patroon zelf wordt door niets bewaakt.** Gemeten door de
gebundelde lens en hier gereproduceerd — beide `pattern`-sleutels uit
`$defs/Geldigheid` verwijderd, het schema schoon geherserialiseerd: gate 2
meldt `PASS (schema valid; 194 file(s) validate and round-trip, floor 192)` en
de getuige van deze kaart blijft 18/18 groen. **Het patroon verwijderen is dus
een stille groen**, en zijn beet staat alleen in kaartproza — in de vijf regels
hierboven en nergens in een gate. Dat is niet uniek voor deze twee sleutels: de
tien patronen van `uchar-case-v0` staan in precies dezelfde positie, dus dit is
de bestaande huistoestand en geen regressie die deze kaart introduceert. De
bewakingsvraag is meegegeven aan
`backlog/new/20260911-geldigheidsgrens-verifierhek.md`, waar hij kan meeliften
op de negatief-getuige die de verifier-arm daar wél oplevert.

### Rood eerst

De getuige is
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/loader/IrWireDatumWitnessTest.java`,
een nieuwe klasse in de loader-testmap, in de vorm van `CaseWireDatumWitnessTest`
(zelfde helperopzet, zelfde twee-takken-assertie, zelfde uitsluiting van het
"niet gedragen"-kanaal — daar `Excluded`, hier `IrLoader.Refusal`). Een eigen
bestand omdat de case-getuige in naam en javadoc over de case-wire gaat en dit
een andere wire, een andere lezer en een andere producent is.

Tegen de kale `LocalDate.parse`, vóór de klem:

```
[ERROR] Tests run: 18, Failures: 14, Errors: 0, Skipped: 0
```

Vier daarvan meldden **`ACCEPTED it`** — de eigenlijke gaten:

| getuige | wat de lezer ervan maakte |
| --- | --- |
| `een_getekend_uitgebreid_jaar_wordt_geweigerd` | `+012026-01-01` gelezen als `+12026-01-01` |
| `een_negatief_jaar_wordt_geweigerd` | `-0500-01-01` gelezen als jaar −500 |
| `beide_grenzen_noemen_hun_eigen_veld_en_hun_eigen_drager` | `+012026-01-01` aanvaard, dus er was geen melding waaruit veld of drager viel af te lezen |
| `elke_geldigheidsdrager_wordt_langs_dezelfde_klem_gelezen` | het hele document geladen met een getekend jaar op een `ResolvedRegel` |

De tien andere weigerden al, maar met een kale `DateTimeParseException` die
noch het veld noch de verwachte vorm noemt — de tweede helft van wat de klem
repareert. De vier die vóór de klem al groen waren zijn de tegenmetingen: het
ongetwiste venster, **beide jaarranden** (`0000-01-01` en `9999-12-31` — geen
corpusdocument zit in de buurt, dus zonder die assertie passeert een van
onderen vernauwde klem elke andere test én elke gate), de schrikkeldag en de
open grens (`null` moet `null` blijven en niet als lexeem worden geparseerd).

Na de klem:

```
tests="18" errors="0" skipped="0" failures="0"
```

De rood-commit is niet apart bewaard: elke commit op de tak draagt een groene
suite, en de rood-bewijsvoering staat hier.

**Mutantproef.** Vier mutanten op de klem, elk met de volle getuige erop. De
twee valkuilen die de vorige kaart optekende zijn hier vermeden: de klem stond
GECOMMIT toen de mutanten draaiden, zodat de `git checkout --` tussen twee
mutanten alleen de mutatie terugnam en niet de klem eronder; en de vallende
gevallen zijn per mutant uit de surefire-XML gelezen, niet uit de consoleregels
of uit proza.

| mutant | uitslag | wie valt |
| --- | --- | --- |
| A: de VORM-tak draagt óók de kalenderformulering | 9 failures | alle negen vormgetuigen |
| B: patroon verruimd tot `[+-]?\d{4,}-\d{2}-\d{2}` | 5 failures | de twee tekenjaren, het vijfcijferige jaar, `beide_grenzen…`, `elke_geldigheidsdrager…` |
| C: patroon vernauwd tot `[1-9]\d{3}-\d{2}-\d{2}` (geen jaar 0000) | 1 error | `de_jaarranden_die_het_patroon_toelaat_worden_gelezen` |
| D: `find()` in plaats van `matches()` | 8 failures | de twee tekenjaren, het vijfcijferige jaar, zonesuffix, datumtijd-lexeem, omringende tekst, `beide_grenzen…`, `elke_geldigheidsdrager…` |

A is de spiegelassertie die de kaart eist, en hij valt precies op de negen
vormgetuigen — de twee routegetuigen asserteren de kalenderformulering niet en
vallen dus niet mee; dat is de reikwijdte van A, opgeschreven zoals gemeten. B
en D vallen allebei óók op de twee routegetuigen, omdat die met
`+012026-01-01` twisten en dat lexeem door beide mutanten wordt doorgelaten. C
is de kant die een van bovenaf vastgelegde klem niet ziet: de negen
corpusgrenzen zijn allemaal 2025 of 2026, dus zonder die ene assertie passeert
een vernauwing tot "geen jaar 0000" elke andere test in dit bestand én elke
gate. Eén getuige is daar de hele dekking, wat een dunne marge is en daarom
hier staat.

### De laagverhouding, expliciet

Drie lagen, elk met een eigen bereik, en geen ervan maakt de andere overbodig:

1. **Het schemapatroon (gate 2)** is de VORM en dekt elk document dat langs de
   gate komt, voor alle lezers tegelijk. Het kan de kalender niet kennen:
   `2026-02-30` matcht en passeert. En het is één lexeemklasse ruimer dan de
   klem: Python's `re`, waarmee `jsonschema` en dus gate 2 draait, laat `$` ook
   matchen vóór één afsluitende newline, dus `"2026-01-01\n"` passeert het
   patroon terwijl `matches()` in Java het hele lexeem eist en hem weigert
   (beide gemeten; `"\n2026-01-01"` en `"2026-01-01\n\n"` vallen in allebei af).
   Die klasse faalt dus luid bij de eerstvolgende lezer in plaats van stil door
   te lopen — en de acht datumpatronen van `uchar-case-v0` delen hem.
2. **De klem in de Drools-lezer** is de vorm PLUS de kalender, op de drempel
   van één executor. Zijn bereik is precies wat het patroon niet ziet: het
   met de hand gebouwde of gegenereerde document dat rechtstreeks een executor
   in gaat, en de kalenderdag die geen reguliere expressie kan controleren.
   Dat is dezelfde verhouding als op de case-wire, waar het patroon er al was
   en de klem er toch bij kwam — met dit verschil dat het patroon daar de
   klem VOORGING en hier tegelijk met de klem wordt voorgesteld.
3. **De verifier**, die deze positie nog niet bewaakt en waar een WF-regel
   thuishoort omdat hij vóór elke executor weigert. Gekaart, niet gedaan.

### Geen `corpus/negatief`-getuige, en waarom niet

**Expliciet: er is er geen toegevoegd, en dat is het besluit, niet een
vergetelheid.** Een `corpus/negatief/<wf###-slug>/` bestaat om te bewijzen dat
de VERIFIER een document weigert onder een genoemde WF-regel (gate 3), en die
regel is er niet: het patroon is gate-2-handhaving en gate 3 ziet het nooit.
Een map aanmaken zonder WF-regel om te citeren zou de gate niet eens kunnen
lezen. Zodra de verifier-kaart hierboven een regel oplevert, hoort de
negatief-getuige daarbij en niet hier.

### Gates

| gate | uitslag | tellingen |
| --- | --- | --- |
| 1 `check-self-contained.sh` | PASS | — |
| 2 `check-ir-schema.sh` | PASS | 194 documenten valideren en round-trippen, vloer 192 |
| 3 `check-verifier.sh` | PASS | 194 golden clean; 362 negatief geweigerd, vloer 350 |
| 8 `check-rood.sh` | PASS | 26 rode cases falen zichtbaar in ref én clingo, vloer 26; Drools-poot 6 fault-pinned, 20 not-carried |
| 12 `check-drools.sh` | PASS | **JUnit 294, vloer 239**; de getuige draagt er 18 blijkens zijn surefire-XML, dus de suite stond vóór deze kaart op 276 (afgeleid uit die twee metingen, niet apart gedraaid). played **121** = `EXPECTED_PLAYED`, ongewijzigd. auto-excluded 44, channel floor 47 op naam gepinde niet-gespeelde goldens, value-divergence 1, no-case 2 — en die vier bewegen niet ongezien: het kanaalpin faalt op naam zodra een golden van kanaal wisselt, en de gelijkheid op played faalt zodra er een bijkomt |
| 13 `check-reftype-derivation.sh` | PASS | 74 annotaties getwist over 15 goldens (vloer 74), 70 replays reproduceren de ongetwiste uitkomst, 13 PLAYED via de DRL-tekst-tak (vloer 13), 0 survivors |

Geen vloer verhoogd, geen `EXPECTED_PLAYED` aangeraakt, geen gate-script
aangeraakt. Gates 4, 5, 6, 7, 9, 10 en 11 zijn hier niet gedraaid: 4/5/6/7
raken corpus- en OCaml-kant die deze tak niet aanraakt, 9/10 zijn frontends
zonder backend-jar. Gate 11 draait de jar wél — de klem zit daar dus op het
pad — en hoort bij de volle set die vóór de merge draait.

### Wat bewust niet bewoog

- **`ref/lib/case.ml`, `backends/clingo/test`, `corpus/golden`**: niet
  aangeraakt (parallelle sporen). De vier mutanten waren `/tmp`-kopieën.
- **`ref`, `backends/clingo`, `spec/verifier`**: alleen als tegenmeting
  gedraaid, zoals de kaart toestaat. Hun ontbrekende hek is gekaart, niet
  gerepareerd — dat raakt twee beschermde paden en een normatieve ID en is
  een eigen kaart waard.
- **`Literal.waarde`**: niet gepatroneerd. Polymorf per `type`; WF-084/WF-095
  dekken de twee datumlexemen al, met kalender. Gemeten in de sweep, niet
  aangenomen.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`**: niet aangeraakt, en er is naar
  mijn oordeel **geen registerrij nodig**. Het schemapatroon vernauwt de
  toegestane vorm van een bestaande wire-positie tot wat elk corpusdocument al
  schrijft en wat WF-084 elders al normeert; er verandert geen constructie,
  geen IR-vorm en geen gepinde verwachting — gate 12 speelt dezelfde 121
  goldens met dezelfde uitkomsten. Zou de maintainer het tóch willen
  registreren, dan is dit de zin: *"`Geldigheid.vanaf`/`tot` dragen op de wire
  het strikte ISO-datumlexeem van WF-084 (`^[0-9]{4}-[0-9]{2}-[0-9]{2}$`),
  afgedwongen door het schema (gate 2); de kalendercontrole erbovenop houdt
  elke lezer zelf."*
- **De reikwijdte van de getuige**, expliciet: geasserteerd is één helper met
  tien aanroepplaatsen over negen knoopsoorten, en alle negen soorten worden
  door `elke_geldigheidsdrager_wordt_langs_dezelfde_klem_gelezen` getwist en
  op naam geëist. Het ene gat dat overblijft en hier genoemd staat in plaats
  van verzwegen: `ResolvedFeitCreatie` heeft TWEE aanroepplaatsen (met en
  zonder `feittype`) en alle vier de corpusgetuigen dragen een `feittype`, dus
  de tweede tak is gedekt door constructie — één gedeelde helper — en niet
  door een document.
