# De dagsoort-semantiekkaart: §6.10/§6.11 en de twee gemelde kloven

Goal: het adres geven aan de dagsoortsemantiek waar inmiddels **vier** kaarten
naar doorverwijzen zonder dat het bestaat. De dagsoort-duurvormen —
`tijdsduur … in hele <dagsoort>` (§6.10) en `datum ± N <dagsoort>` (§6.11) —
krijgen hun normatieve semantiek in `spec/RUNTIME-SEMANTICS.md` met gewerkte
voorbeelden, en de uitvoerders dragen die of weigeren hem **met een reden die
de vorm noemt**. Deze kaart verzamelt daarnaast de twee kloven die de
contextwaarde-kaart van 2026-09-10 heeft gemeld maar niet gesloten.

## De vier doorverwijzingen die hier landen

- `backlog/done/20260905-dagsoort-per-eenheid-golden.md` (Afwijking 1): "Een
  eigen semantiekkaart is nodig om §6.10/§6.11 in ref en clingo te laten
  rekenen."
- `backlog/done/20260906-dagsoort-kruisingsarm.md` (Out of scope): de
  duurvormen zijn "een gemeten drie-uitvoerder-asymmetrie en een eigen
  semantiekkaart".
- `backlog/done/20260909-wf017-sleutel-en-spellingsbotsing.md` (Out of scope):
  "De dagsoort-duurvormen zelf (§6.10/§6.11) — eigen semantiekkaart."
- `backlog/done/20260909-constante-naam-gesloten-verzameling.md` (Out of scope
  én "Gemeld, niet gekaart"): dezelfde regel, plus de twee kloven hieronder.

## De gemeten asymmetrie die de kaart erft

`ref` en de ASP-backend **weigeren** beide duurvormen luid — `ref/lib/eval.ml`
zegt letterlijk "a day-kind duration is a recorded flank, not executed" — terwijl
de Drools-backend ze **wél** lowert (de gedeelde `Dagsoorten`-predikaatcompiler,
de §6.11-datumrekenkunde en de §6.10-tijdsduur). Twee weigeringen en één
uitvoering onder één wire-vorm: geen differentieel, want er is geen antwoord om
over te verschillen. Daarbovenop, `IR-SPEC.md` ch. 5.4: **geen enkele uitvoerder
evalueert vandaag een dagsoortpredicaat** — de predikaatsoort wordt geweigerd in
`ref/` én `backends/clingo`. Een dagsoort is dus vandaag hooguit een stratum-0
classificeerder van een invoerdatum, nooit een afgeleide lezing in het
regelfixpunt.

## De twee kloven die deze kaart erft (gemeld 2026-09-10, WF-098-kaart)

**(a) De `de dag`-runtimekloof.** WF-098 (`IR-SPEC.md` ch. 4.1) laat `de dag`
toe als contextwaarde — de wire draagt er 11 van de 14 — en de ch. 4.1-rij zegt
dat zijn runtime-betekenis de dagsoortevaluatie van ch. 3.7 is: de gebonden dag
waarop het predicaat van de definitie wordt toegepast. `RUNTIME-SEMANTICS.md`
**ch. 8 draagt die betekenis niet**: dat hoofdstuk noemt alleen `Rekendatum` en
`Rekenjaar` ("the evaluation date is available to expressions solely as the v0
execution-date constant"). Dat is een **bestaande** kloof — niet door WF-098
geopend, wel door hem zichtbaar gemaakt, omdat de rij `de dag` nu benoemt. Er is
geen gewerkt voorbeeld en geen uitvoerder die hem vandaag zou kunnen dragen (zie
ch. 5.4 hierboven), dus hij hoort bij deze kaart en niet bij een lexeem- of
naamregel.

**(b) De `Rekendatum`-in-dagsoortvoorwaarde-flank.** WF-098's verzameling is
**plat**: elk van de drie namen op elke expressiepositie die de wandeling
bereikt, zonder positieregel ernaast. Gemeten 2026-09-10, één veld van
`corpus/golden/jutterswet-schouwrooster` omgezet (`de dag` → `Rekendatum`,
binnen `resolvedDagsoorten[0]/voorwaarde`):

    verifier: PASS
    ref:      oracle: PASS (7 expectations)

De sessie-evaluatiedatum staat dus in een **stratum-0**-predicaat en niets
weigert het. Twee redenen, allebei gecontroleerd: WF-031's eigen
toelatingslijst (`IR-SPEC.md` ch. 3.7) noemt "dates, date-components,
eerste-paasdag, literals, parameters and other day-kind names" en **geen enkele
contextwaarde**; en de zuiverheidswandeling in `spec/verifier/lib/checks.ml`
(±r. 2581) matcht `Constante _` **naamblind** en telt hem als puur. Niet door
WF-098 veroorzaakt — de arm die hij verving gaf daar ook `[]` — wel door hem
zichtbaar. De vraag die eronder ligt is een **semantiekvraag**, geen
spellingsvraag: mag een sessiewaarde een stratum-0-predicaat binnen, en wat
betekent dat voor de ch. 5.4-framing dat een dagsoort vóór de regelgraaf wordt
geëvalueerd? Vandaar hier en niet daar.

## Acceptance

- `spec/RUNTIME-SEMANTICS.md` (**voorstel** — beschermd pad) krijgt de
  semantiek van beide duurvormen, met **gewerkte voorbeelden met absolute
  verwachte waarden** in de vorm die de andere hoofdstukken hanteren
  (`ch. 3.8` is de buur), inclusief de randgevallen die de bestaande tekst voor
  de kalenderduren al pint: de "A telt zelf niet mee"-regel, de richting
  achteruit, een `leeg` datum, en een **onvervulbare** dagsoortdefinitie.
- Kloof (a) gesloten of expliciet niet-gedaan: `de dag` krijgt zijn
  runtime-betekenis in `RUNTIME-SEMANTICS.md` (waar hoort hij — ch. 8 bij de
  contextwaarden, of bij de dagsoortevaluatie zelf?), óf de kaart legt vast
  waarom hij daar niet hoort, met de meting erbij.
- Kloof (b) gesloten of expliciet niet-gedaan: als een sessiewaarde in een
  stratum-0-predicaat ongewenst is, dan is dat een **WF-clausule** (kandidaat:
  een arm van WF-031, die de toelatingslijst al draagt) met rood-eerst-bewijs,
  arm-exactheid en nul over-afwijzing over alle zones; als het toelaatbaar is,
  dan staat dat met zijn motivering in ch. 3.7 en ch. 5.4 en verdwijnt de post
  uit de meldlijst. Niet stil laten staan.
- **Uitvoerderdracht of gemotiveerde weigering.** Óf `ref` en de ASP-backend
  rekenen beide duurvormen — met een golden die alle drie uitvoerders spelen,
  zodat het vierkant sluit (ref ≡ clingo ≡ Drools) — óf de weigering blijft en
  wordt genormeerd: dan noemt de spec de vorm als een **erkende, niet-uitgevoerde
  flank** en zeggen beide weigeringen welke vorm ze weigeren, in plaats van door
  te vallen naar een generieke boodschap. Een asymmetrie waarbij één uitvoerder
  rekent en twee zwijgen is geen van beide.
- Gate 5 en 6 draaien de nieuwe gewerkte voorbeelden; gates 3 en 12 bewegen mee
  waar de kaart een clausule of een golden toevoegt; alle dertien gates groen.
- Nul over-afwijzing bij elke nieuwe clausule, direct gemeten over alle zones.

May touch: `spec/RUNTIME-SEMANTICS.md` (**voorstel** — beschermd pad),
`spec/IR-SPEC.md` (**voorstel** — beschermd pad), `spec/verifier/**`
(**voorstel** — beschermd pad), `ref/**`, `backends/**`, `corpus/**`,
`backlog/`.

## Out of scope

- **De sleutelkant van `dagsoortPerEenheid`.** Bewust vrij gelaten met de
  motivering in `IR-SPEC.md` ch. 2.9 en ch. 4.6 (s); de waardehelft is dicht
  sinds 2026-09-09.
- **WF-017's sleutelgaten** (een registersleutel die een ingebouwde
  kalendereenheid schaduwt; twee dagsoortspellingen die botsen) — **gesloten
  2026-09-10**, WF-017 armen (a) en (b), ch. 2.9, met twee negatief-getuigen en
  de positieve binding in `jutterswet-vormenkabinet`
  (`backlog/done/20260909-wf017-sleutel-en-spellingsbotsing.md`). Wat van
  WF-017 nog openstaat is alleen de PRODUCENTKANT: een frontend-diagnostiek op
  de stille `putIfAbsent` in `registreerDagsoortEenheid` — zie de
  conformance-notitie in die done-kaart. Deze kaart draagt hem niet.
- **De contextwaarde-naam zelf.** WF-098 is geland (2026-09-10, byte-exact,
  gesloten verzameling van drie); deze kaart raakt de spelling niet, alleen de
  betekenis en de positie.
- **De lege helft `Constante.naam: null`** — de (n)/(o)-precedent, vastgelegd
  met zijn meting bij WF-098 in ch. 4.1; een eigen clausule met een eigen
  beslissing.

## Maintainersbesluit (2026-09-19): oppakken

De kaart wordt gebouwd: het semantiekvoorstel in RUNTIME-SEMANTICS met
gewerkte voorbeelden, met Drools' bestaande gedrag als vertrekpunt en
de brontekst als toets, en genummerde restvragen waar de bron zwijgt.

---

## Result (2026-09-19)

**Gedaan: gebouwd, niet genormeerd-geweigerd.** Beide duurvormen rekenen in
`ref` én de ASP-backend, met dezelfde absolute waarden die de Drools-backend
al speelde. Het vierkant sluit op één golden; de twee kloven zijn allebei
dicht; de brontekst is per keuze getoetst en waar hij zwijgt staat er een
genummerde restvraag.

### De hermeting (huidige main, vóór deze kaart)

Per uitvoerder en per vorm, met een minimaal wire-document
(`Baken.peildatum plus Baken.termijn` in `schouwdagen`, en
`tijdsduur … in hele schouwdagen`), verifier-schoon in beide gevallen:

| vorm | verifier | `ref` | clingo | Drools (exec) | Drools (`--drl`) |
|---|---|---|---|---|---|
| §6.11 `datum ± N <dagsoort>` | PASS | EVALUATION FAULT `duration unit "schouwdagen" is not a recognised time unit (a day-kind duration is a recorded flank, not executed)` | TRANSLATION FAULT `date arithmetic unit "schouwdagen" …` | lowert | lowert |
| §6.10 `tijdsduur … in hele <dagsoort>` | PASS | EVALUATION FAULT `tijdsduur unit … a day-kind tijdsduur is a recorded flank` | TRANSLATION FAULT `tijdsduur unit …` | lowert | lowert |

Drools droeg **beide** vormen in **beide** takken, via één paar statics in
`RegelSpraakRuntime` (`rsDatumPlusDagsoort`, `rsTijdsduurDagsoort`, plus hun
datum-tijd-wikkels). Gemeten: vóór deze kaart noemde **niets** die vier
statics behalve de twee transpilers — geen test, geen script, geen document
in enige zone. Eén uitvoerder rekende dus, twee zwegen, en wat de ene rekende
werd nergens getoetst.

Dat `RUNTIME-SEMANTICS.md` ch. 3.8 al twee alinea's over de duurvormen droeg
was óók een vondst: ze staan er sinds de allereerste ch.-3-draft
(`85bd768a`), zonder uitvoerder, zonder gewerkt voorbeeld als test, zonder
fence-definitie, zonder een woord over `de dag` of over resolutie.

### Het voorstel in vijf zinnen

Een dagsoort is geen eenheid maar een predicaat over een kalenderdag, dus de
eenheidsspelling wordt via `dagsoortPerEenheid` (getrimd, hoofdletterloos)
tot een dagsoortnaam herleid en die naam tot zijn ene `ResolvedDagsoort`;
`P_k(d)` is diens voorwaarde met **`de dag` gebonden aan d** en verder niets
in scope, wat meteen de runtimebetekenis van die contextwaarde is. §6.10 telt
de dagen `A < d ≤ B` die `P_k` vervullen — start exclusief, eind inclusief —
getekend naar operandvolgorde, leeg propagerend, zonder fence, en een
onvervulbare dagsoort telt daar gewoon 0. §6.11 stapt dag voor dag en levert
de N-de vervullende dag; de startdag telt in geen van beide richtingen mee,
`N = 0` en leeg-N geven de startdatum ongewijzigd terug (ook als die zelf
geen dag van die soort is), een negatieve N loopt achteruit en een breuk kapt
naar nul af. De wandeling termineert door een **normatieve gapgrens van
3 660 kalenderdagen**: 3 660 opeenvolgende dagen zonder treffer is een fout,
geen eindeloze lus — een grens op het gat en niet op het totaal, zodat
`datum plus 5 000 werkdagen` beantwoordbaar blijft. De datum-tijd-kruising
blijft een erkende, niet-uitgevoerde vorm die beide OCaml-uitvoerders in
dezelfde woorden weigeren.

### De genummerde restvragen

De brontekst (RegelSpraak v2.3.0, gelezen als toets) noemt dagsoorten in
§6.11 wél expliciet en in **§6.10 geen enkele keer**; §9.8 geeft het
predicaat, §3.12 de declaratie, §13.4.17.44 `de dag`. Waar hij zwijgt:

- **D1 — telt A mee in §6.10?** De bron zegt over §6.10 niets over
  intervalinclusie en noemt dagsoorten daar niet. Opties: (a) `(A, B]`,
  (b) `[A, B]`, (c) `[A, B)`. *Aanbeveling en gekozen (voorlopig): (a)* —
  het is wat Drools doet en het is de enige lezing die met §6.11's expliciete
  "X telt niet mee" strookt; `25-12 tot 26-12 = 1` is de getuige die (b) en
  (a) scheidt.
- **D2 — achteruit stappen met een dagsoort.** De bron beschrijft alleen
  optellen ("de 5e werkdag ná X"). Opties: (a) spiegelbeeld — de N-de dag
  strikt vóór X, (b) weigeren. *Aanbeveling en gekozen (voorlopig): (a)*;
  Drools doet het al en `min` is grammaticaal toegelaten, dus (b) zou een
  bestaande frontendvorm afsnijden.
- **D3 — `N = 0` bij een dagsoort.** De bron spreekt zichzelf tegen: de
  algemene 0-regel zegt "de oorspronkelijke datum", de dagsoortregel zegt
  "het resultaat is altijd van die soort". Opties: (a) 0-regel wint,
  (b) dagsoortregel wint (dus de eerstvolgende dag van die soort, óók bij 0),
  (c) weigeren. *Aanbeveling en gekozen (voorlopig): (a)* — (b) maakt 0 en 1
  gelijk voor een niet-vervullende startdag en breekt `leeg ≡ 0`.
- **D4 — de fence.** De bron heeft **geen** bovengrens en **geen**
  terminatieclausule voor dagsoorten (de enige iteratielimiet-eis, §9.10,
  gaat over recursieve regelgroepen). Opties: (a) een totaalgrens (Drools'
  4 000 000 stappen), (b) een **gapgrens**, (c) geen grens. *Aanbeveling en
  gekozen (voorlopig): (b) met 3 660 dagen* — (a) weigert een legitieme grote
  N onder een boodschap die de definitie beschuldigt, (c) is geen semantiek.
  3 660 ligt boven de langste gregoriaanse terugkeer (de eeuwoverslaande
  schrikkeldag, 2096-02-29 → 2104-02-29 = **2 921** dagen) en laag genoeg om
  te gronden. **Gevolg, gedaan in deze kaart:** Drools' totaalgrens is de
  gapgrens geworden, zodat de drie uitvoerders op dezelfde invoer in dezelfde
  woorden falen.
- **D5 — onvervulbaar in §6.10.** De bron zwijgt. Opties: (a) 0,
  (b) fout. *Aanbeveling en gekozen (voorlopig): (a)* — een telling over een
  lege verzameling ís een antwoord; alleen de stap heeft er geen.
- **D6 — de datum-tijd-kruising.** De bron laat datum-tijd-operanden in §6.10
  toe (typeringen §4.16) en zwijgt over de borrow-regel bij een dagsoort.
  Opties: (a) tellen over de datumdelen zonder borrow (wat Drools doet),
  (b) mét borrow, zoals de kalendereenheden, (c) weigeren. *Aanbeveling en
  gekozen (voorlopig): (c)* — er is geen enkele getuige in enige zone, en een
  clausule zonder getuige is een groene deelverzameling. Beide OCaml-
  uitvoerders weigeren in dezelfde woorden; Drools' (a) is daarmee een
  gedrag dat de spec niet draagt en dat geen document kan bereiken.
- **D7 — het dagsoortpredicaat als regelvoorwaarde** (§8.1.5, "… is een
  werkdag" in een `indien`). De bron laat het toe; uchar weigert het nog
  steeds bij naam in beide OCaml-uitvoerders. Opties: (a) zo laten,
  (b) uitvoeren (dan moet de ASP-kant een dag-domein rond een afgeleide datum
  gronden). *Aanbeveling: (a) voor nu, eigen kaart voor (b)* — het is een
  aparte semantische stap (ch. 5.4's framing) en niet nodig voor de
  duurvormen. Binnen een dagsoortvoorwaarde is de geneste verwijzing (§9.8)
  wél uitgevoerd, in beide uitvoerders.
- **D8 — `de dag` buiten een dagsoortdefinitie: ook statisch weigeren?** De
  bron plaatst `<dagsoortreferentie>` in de algemene `<referentie>`-alternatie
  zonder contextconditie. Vandaag: runtimefout bij naam in beide
  uitvoerders, gepind door `corpus/rood/contextwaarde-de-dag-buiten-dagsoort`.
  Opties: (a) zo laten, (b) een positie-arm op WF-098. *Aanbeveling: (b),
  eigen kaartje* — het is een tweede normbesluit op een beschermde regel die
  WF-098 bewust plat heeft gelaten, en de spiegel ervan (WF-031's arm) is in
  deze kaart wél genomen omdat de kaart daarom vroeg.

### De twee kloven

**(a) `de dag` — GESLOTEN.** Zijn runtimebetekenis staat in
`RUNTIME-SEMANTICS.md` **ch. 3.8**, bij de dagsoortevaluatie — niet in ch. 8.
Motivering, en die staat er ook: ch. 8 is tijd*selectie* en gaat over de
sessiewaarden; `de dag` is geen sessiewaarde maar de gebonden dag, dus zijn
betekenis hoort waar de binding wordt gelegd. Ch. 8.2 kreeg een alinea die
dat zegt en doorverwijst. Beide uitvoerders binden hem nu (`ref` via
`env.locals` onder een NUL-sleutel die geen bronnaam kan botsen; clingo via
`ctx.binders`, zodat elke operandvorm die `expr_term` al draagt ongewijzigd
in een dagsoortvoorwaarde landt) en falen bij naam buiten die binding.

**(b) Rekendatum in stratum 0 — GESLOTEN met een WF-clausule.** Besluit:
**niet toelaatbaar**. WF-031's zuiverheidsclausule krijgt een positie-arm:
binnen een dagsoortvoorwaarde is `de dag` de enige toegelaten contextwaarde,
en `Rekendatum`/`Rekenjaar` worden geweigerd. Grond: de uitvoerders
evalueren de voorwaarde nu per kalenderdag, dus een sessieafhankelijk `P_k`
zou hetzelfde document dezelfde dag in twee sessies verschillend laten
classificeren — precies wat ch. 5.4 ontkent.
- **Rood-eerst:** de meting van 2026-09-10 ís de getuige geworden —
  `corpus/negatief/wf031-sessiewaarde-in-dagsoort`, één veld van
  `jutterswet-schouwrooster` omgezet, dat vóór de arm `verifier: PASS` en
  `ref: oracle: PASS (7 expectations)` gaf.
- **Arm-exactheid:** de arm noemt de twee sessienamen en niets anders, dus
  `wf098-onbekende-contextwaarde-naam` — wiens `de dagX` op exact deze
  positie staat — meldt nog steeds alleen WF-098, en de lege helft
  (`naam: null`) blijft ongemoeid.
- **Nul over-afwijzing, direct gemeten over alle zones:** 671 documenten
  (golden, negatief, rood, pariteitszone) bevatten samen 26 `Constante`-knopen
  binnen een `resolvedDagsoorten`-voorwaarde; alle 26 zijn `de dag` op de twee
  hier genoemde getuigen na. Gate 3 groen: 242 golden schoon, 399 negatief
  afgewezen op hun eigen regel.

### Uitvoerderdracht

`ref` loopt de wandeling; de ASP-backend **grondt** hem: de dagen die bezocht
mogen worden worden een domeinpredicaat dat alleen uit de operanden volgt
(voor §6.10 het operandinterval, voor §6.11 `N × 3 660`), de voorwaarde wordt
een regel over dat domein met de gebonden dag als variabele, §6.10 leest een
`#count` en §6.11 een recursieve (gevonden, gat)-rang. Dat het domein niet van
de voorwaarde afhangt is precies wat het geheel gestratificeerd houdt. Eén
venster is van deze backend alleen, luid en gedocumenteerd naar het
S6a-precedent: een stap die meer dan 64 dagen van die soort tegelijk zoekt
wordt hier geweigerd waar `ref` hem rekent.

Differentieel gemeten buiten de golden om, ref ≡ clingo op handberekende
data: alle vijf kwantoren in een dagsoortvoorwaarde, een kale vergelijking,
en de geneste §9.8-verwijzing positief én ontkend (8 van 8).

### Artefacten

- **golden `corpus/golden/jutterswet-bergingsklok`** (met `bron.rgs`, dus
  gate 10 emitteert hem byte-identiek terug): dagsoort `springtijdag` = de
  1e of de 15e; vier regels (stap vooruit, stap achteruit, getekende telling,
  absolute telling); twee cases, 22 handberekende verwachtingen. Speelt in
  `ref`, clingo én **beide** Drools-takken — het vierkant.
- **rood `dagsoort-onvervulbare-wandeling`**: de gapgrens, gepind in alle
  drie de uitvoerders (`expectedFault.drools` erbij).
- **rood `contextwaarde-de-dag-buiten-dagsoort`**: `de dag` zonder binding,
  in beide OCaml-uitvoerders in dezelfde woorden.
- **negatief `wf031-sessiewaarde-in-dagsoort`**.
- **`ref/test`**: 28 nieuwe gewerkte voorbeelden (643 → 671), de ch.-3.8-tekst
  verbatim plus de resolutie- en bindingshelft door `Eval`.
- **`backends/clingo/test`**: 13 nieuwe checks (423 → 436) — de vijf
  kwantoren, de geneste verwijzing positief/ontkend, en vijf weigeringen.
- **`backends/drools`**: `DagsoortDuurTest`, de eerste test die de twee
  runtime-statics aanraakt (JUnit 478, vloer 239).

### Gates (alle veertien, elk los)

1 PASS · 2 PASS (242 bestanden) · 3 PASS (242 golden, 399 negatief) ·
4 PASS (576 cases) · 5 PASS (671 voorbeelden, 545 cases) ·
6 PASS (436 checks, 545 cases) · 7 PASS (509 cases, 36 survivors) ·
8 PASS (31 rode cases, Drools-leg 7 gepind / 24 met reden) ·
9 PASS (201 checks, 12 goldens) · 10 PASS (322 tests, 59 rgs-goldens) ·
11 PASS (29 PLAYED) · 12 PASS (`EXPECTED_PLAYED` 159 → **160**,
`EXPECTED_DRL_PLAYED` 132 → **133**, JUnit 478) ·
13 PASS (128 twists over 24 goldens, 0 survivors, 0 buiten de tabel) ·
14 PASS (130 checks, 2 goldens).

### Beschermde paden — voorstellen, niet gemerged

Gemandateerd door de kaart: `spec/RUNTIME-SEMANTICS.md` (ch. 3.8 de
dagsoortsectie, ch. 8.2 de contextwaarde-alinea, ch. 2.10 de
uitzonderingsnoot, ch. 3.12 drie rijen), `spec/IR-SPEC.md` (ch. 3.7 WF-031's
positie-arm, ch. 4.1 de ContextValue-rij, ch. 5.4, WF-098's vlakheidsalinea,
de WF-031-regeltabelrij), `spec/verifier/**`.
**Niet gemandateerd, wel voorgesteld en hier gemeld:**
`spec/ASP-TRANSLATION.md` (drie plekken die vanaf nu onwaar zouden zijn: de
"loud flanks"-lijst, de datumfamilie-samenvatting, en de DAGSOORT-predicaat-
alinea) en de twee gatescripts `scripts/check-drools.sh` (de twee pins met
hun redenblok) en `scripts/check-rood.sh` (één regel in de
Drools-uitsluitingslijst).

### Registervoorstel voor `spec/CONSTRUCTS.md` (niet aangeraakt)

De dagsoortrij (de rij die §3.12/§9.8 draagt) krijgt er als delta-tekst bij:

> **Duurvormen uitgevoerd (2026-09-19).** §6.10 en §6.11 met een dagsoort
> als eenheid draaien in `ref`, de ASP-backend en beide Drools-takken over
> dag-precieze operanden (`RUNTIME-SEMANTICS.md` ch. 3.8; golden
> `jutterswet-bergingsklok`). Vier **diverge-from-v0**-delta's, elk omdat de
> bron zwijgt of zichzelf tegenspreekt: de halfopen telling `(A, B]` van
> §6.10 (de bron noemt dagsoorten daar niet), de achterwaartse stap van
> §6.11 (de bron beschrijft alleen optellen), de 0-regel die het wint van
> "het resultaat is altijd van die soort", en een **gapgrens van 3 660
> kalenderdagen** waar de bron geen enkele terminatieclausule heeft — de
> Drools-backend gaf die grens als een totaal van 4 000 000 stappen en
> volgt nu de genormeerde gapgrens. De datum-tijd-kruising blijft een
> erkende, niet-uitgevoerde vorm (v0 telt daar over de datumdelen; uchar
> weigert bij naam).

### Niet gedaan, bewust

D6 (datum-tijd × dagsoort), D7 (het dagsoortpredicaat als regelvoorwaarde) en
D8 (een statische arm op `de dag` buiten zijn binding) zijn genummerd en
gemotiveerd, niet gebouwd. De sleutelkant van `dagsoortPerEenheid`, WF-017's
producentkant en de lege helft `Constante.naam: null` blijven zoals de kaart
ze buiten scope plaatste.
