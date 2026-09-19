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

## Result (2026-09-19, herzien na twee lensrondes en twee maintainersbesluiten)

**Gedaan: gebouwd, niet genormeerd-geweigerd.** Beide duurvormen rekenen in
`ref` én de ASP-backend, met dezelfde absolute waarden die de Drools-backend
al speelde. Het vierkant sluit op twee goldens; de twee kloven zijn dicht; de
brontekst is per keuze getoetst en waar hij zwijgt staat er een genummerde
restvraag. Twee restvragen zijn na de eerste landing door de **maintainer
beslist**, beide anders dan mijn voorlopige voorstel; ze staan hieronder met
hun herkomst.

### De hermeting (main vóór de kaart)

Per uitvoerder en per vorm, met een minimaal wire-document verifier-schoon in
beide gevallen:

| vorm | verifier | `ref` | clingo | Drools (exec) | Drools (`--drl`) |
|---|---|---|---|---|---|
| §6.11 `datum ± N <dagsoort>` | PASS | EVALUATION FAULT `duration unit "schouwdagen" is not a recognised time unit (a day-kind duration is a recorded flank, not executed)` | TRANSLATION FAULT `date arithmetic unit "schouwdagen" …` | lowert | lowert |
| §6.10 `tijdsduur … in hele <dagsoort>` | PASS | EVALUATION FAULT `tijdsduur unit … a day-kind tijdsduur is a recorded flank` | TRANSLATION FAULT `tijdsduur unit …` | lowert | lowert |

Drools droeg **beide** vormen in **beide** takken via één paar statics in
`RegelSpraakRuntime`. Gemeten: vóór deze kaart noemde **niets** die statics
behalve de twee transpilers — geen test, geen script, geen document in enige
zone. Eén uitvoerder rekende, twee zwegen, en wat de ene rekende werd nergens
getoetst. Dat `RUNTIME-SEMANTICS.md` ch. 3.8 al twee dagsoort-alinea's droeg
was óók een vondst: ze staan er sinds de allereerste ch.-3-draft (`85bd768a`),
zonder uitvoerder, zonder reikwijdte-definitie, zonder `de dag` en zonder
resolutie.

### Het voorstel in vijf zinnen

Een dagsoort is een predicaat over een kalenderdag, dus de eenheidsspelling
wordt via `dagsoortPerEenheid` (getrimd, hoofdletterloos) tot een dagsoortnaam
herleid en die naam tot zijn ene `ResolvedDagsoort`; `P_k(d)` is diens
voorwaarde met **`de dag` gebonden aan d**, wat meteen de runtimebetekenis van
die contextwaarde is. §6.10 telt de dagen `A < d ≤ B` die `P_k` vervullen,
getekend naar operandvolgorde, leeg propagerend, zonder fence — een
onvervulbare dagsoort telt daar 0. §6.11 stapt naar de N-de vervullende dag;
de startdag telt in geen richting mee, `N = 0` en leeg-N geven de startdatum
ongewijzigd terug, negatieve N loopt achteruit en een breuk kapt naar nul af.
De wandeling termineert door een **normatieve reikwijdte van 40 000
kalenderdagen** — een grens op het gat, niet op het totaal, en een uitspraak
over hoe ver de wandeling kijkt, niet over vervulbaarheid. Op een
**datum-tijd**-operand draagt de §6.11-stap: de dagsoort wordt uitsluitend op
het kalenderdatumdeel beoordeeld en de oorspronkelijke kloktijd blijft staan,
terwijl de §6.10-tijdsduur tussen twee instants geweigerd blijft tot
gedeeltelijke dagen en grensmomenten apart bepaald zijn.

### De twee maintainersbesluiten

**BESLUIT 1 (D4) — de reikwijdte is 40 000 dagen, niet 3 660.** Mijn
voorlopige voorstel was tien jaar, afgeleid van de langste *gregoriaanse*
terugkeer (de eeuwoverslaande schrikkeldag, 2 921 dagen). De lens bouwde het
tegenvoorbeeld dat die afleiding breekt: **"de 1e of de 15e van 2040"** —
verifier-schoon, zuiver, vervulbaar, en vanaf een meldingsdatum in 2026 meer
dan een decennium weg. Een dagsoort kan jaar- of parametergebonden zijn, en
dan zegt de gregoriaanse terugkeer niets. Doorgevoerd in `ref`
(`day_kind_reach`), clingo, `RegelSpraakRuntime` (`DAGSOORT_REIKWIJDTE`), de
spec en de rode case. Gemeten na de wijziging: alle drie de uitvoerders
beantwoorden het 2040-voorbeeld (`2026-03-10 + 1 → 2040-01-01`).
De **motivering is herschreven** zoals gevraagd: de grens is een
reikwijdtebeperking, geen onvervulbaarheidsdetector. `an unsatisfiable
day-kind definition?` is uit alle drie de foutzinnen weg; er staat nu *the
walk reaches no further; a day kind whose next occurrence lies beyond the
reach is refused, not diagnosed*. Ch. 3.8 benoemt de **geaccepteerde
restkosten** in een eigen alinea: een vervulbare dagsoort voorbij de
reikwijdte wordt geweigerd hoewel hij bestaat, en wat de afspraak draagbaar
maakt is dat het een *fout* is en geen waarde, dat hij in alle drie de
uitvoerders identiek is, en dat het getal in de spec staat en geen knop is.

**BESLUIT 2 (D6) — de datum-tijd-kruising is GESPLITST.**
- **(a) `datum-tijd ± N <dagsoort>` draagt** in `ref` en clingo (Drools droeg
  hem al). Geverifieerd dat Drools precies dít doet — dagdeel beoordeeld,
  kloktijd behouden — via de gepinde waarden van de nieuwe golden én een
  unittest op `rsDatumTijdPlusDagsoort` zelf. De **Empty-arm-vondst van de
  lens valt hiermee goed**: ref's "leeg N → datumtijd ongewijzigd" is onder
  dit besluit *correct gedrag*, en staat zo vastgelegd in ch. 3.8 ("including
  the leeg-count arm, which returns the datetime with its clock time, exactly
  as it returns the date") en in de NOTE van de nieuwe golden.
- **(b) De §6.10-tijdsduur over datumtijden blijft geweigerd**, en de
  lens-fix is onverkort doorgevoerd: de **precisietoets staat vóór de
  Empty-armen** in `ref/lib/arith.ml`, zodat óók een lege operand de
  vorm-noemende weigering krijgt in plaats van de leeg-regel van een vorm die
  niet gedragen wordt. Drie ref-checks erbij
  (`WV-3.8-dk-dt-telling`, `…-leeg-links`, `…-leeg-rechts`).
- **(c) De differentiële golden: `corpus/golden/jutterswet-getijklok`**, nieuw
  en niet extra cases op `bergingsklok`. Motivering: `bergingsklok` declareert
  dag-precieze attributen en zijn NOTE is om dat verhaal geschreven;
  datum-tijd erbij zou hertypering en herpinning van alle 22 handberekende
  waarden betekenen, en de kanaalboekhouding wordt er eerlijker van — de
  nieuwe golden beweegt beide gate-12-pins op zijn eigen reden in plaats van
  in andermans rij te verdwijnen. Zes handberekende waarden in case1, vier in
  case2; **G2 is de rij die een herafgeleide klok vangt** (melding op een
  springtijdag om **23:59:59**, stap naar 1 april om 23:59:59 — niet naar
  middernacht en niet een dag verder), **G3** is de nulrij op middernacht, en
  case2 is leeg/nul. Speelt in `ref`, clingo en **beide** Drools-takken.

### De lens-fix (3): de geneste verwijzing op een andere dag

Gemeten: `ref` en Drools rekenden, clingo weigerde. **Genormeerd langs de
smalle route** — alle drie weigeren nu:
- `ref` weigert vorm-noemend in clingo's woorden (`a nested day-kind test on
  a date other than the bound day is a recognised, unexecuted form (the §9.8
  composition is carried on the bound day only)`);
- clingo ongewijzigd, zelfde zin;
- de Drools-dagsoortcompiler (`transpiler/kern/Dagsoorten`) laat de
  compositie **ongecompileerd** wanneer de operand niet de gebonden dag is,
  zodat de bereikende regel een transpiler-SKIP wordt — **benoemd** in gate
  8's Drools-uitsluitingslijst;
- rode getuige `corpus/rood/dagsoortverwijzing-andere-dag`.

### De genummerde restvragen

De brontekst (RegelSpraak v2.3.0, gelezen als toets) noemt dagsoorten in
§6.11 expliciet en in **§6.10 geen enkele keer**; §9.8 geeft het predicaat,
§3.12 de declaratie, §13.4.17.44 `de dag`. Waar hij zwijgt:

- **D1 — telt A mee in §6.10?** Bron zwijgt. Opties: (a) `(A, B]`, (b)
  `[A, B]`, (c) `[A, B)`. *Gekozen: (a)* — Drools' lezing, en de enige die met
  §6.11's expliciete "X telt niet mee" strookt.
- **D2 — achteruit stappen.** Bron beschrijft alleen optellen. Opties: (a)
  spiegelbeeld, (b) weigeren. *Gekozen: (a)*; `min` is grammaticaal toegelaten,
  dus (b) snijdt een bestaande frontendvorm af.
- **D3 — `N = 0` bij een dagsoort.** Bron spreekt zichzelf tegen. *Gekozen:
  de 0-regel wint* van "het resultaat is altijd van die soort".
- **D4 — de grens. BESLIST door de maintainer: een gapgrens van 40 000
  kalenderdagen**, als reikwijdtebeperking geformuleerd. (Mijn voorstel was
  3 660; het tegenvoorbeeld staat hierboven.) De bron heeft geen enkele
  terminatieclausule voor dagsoorten; §9.10's iteratielimiet-eis gaat over
  recursieve regelgroepen.
- **D5 — onvervulbaar in §6.10.** Bron zwijgt. *Gekozen: 0*, geen fout — een
  telling over een lege verzameling ís een antwoord.
- **D6 — datum-tijd. BESLIST door de maintainer: gesplitst** — de plus-vorm
  draagt op het dagdeel met behoud van kloktijd, de tijdsduur tussen instants
  blijft geweigerd. (Mijn voorstel was: beide weigeren.)
- **D7 — het dagsoortpredicaat als regelvoorwaarde** (§8.1.5). Bron laat het
  toe; uchar weigert het bij naam in beide OCaml-uitvoerders. *Aanbeveling:
  zo laten, eigen kaart* — het is een aparte semantische stap (ch. 5.4) en
  niet nodig voor de duurvormen. Binnen een dagsoortvoorwaarde is de geneste
  verwijzing (§9.8) wél uitgevoerd, op de gebonden dag.
- **D8 — `de dag` buiten een dagsoortdefinitie: ook statisch weigeren?**
  Vandaag runtimefout bij naam in beide uitvoerders, rood gepind.
  *Aanbeveling: ja, een positie-arm op WF-098, eigen kaartje* — het is een
  tweede normbesluit op een regel die WF-098 bewust plat heeft gelaten.
- **D9 (nieuw) — de §9.8-compositie op een ANDERE dag dan de gebonden dag.**
  Nu in alle drie de uitvoerders geweigerd (de smalle route hierboven). De
  **dragende lezing staat open** voor wie er een bron voor aandraagt: wat zo'n
  toets op een vaste datum *betekent* binnen een definitie die over een
  willekeurige dag gaat — een constante conjunct, een scopingfout, of een
  afkorting voor iets anders — staat nergens. Opties: (a) zo laten, (b) dragen
  zodra de bron het zegt. *Aanbeveling: (a), met (b) expliciet open.*

### De gerichte nalens op de ASP-lowering (F1–F5)

De herbouwde extensietabel droeg vijf bevindingen. Alle vijf zijn gesloten,
elk met zijn eigen machinegetoetste getuige.

- **F4 (blokkerend) — het reikwijdtehek had een gat.** Viel de doelrang buiten
  de tabel terwijl die aan die zijde **niet** afgekapt was, dan vuurde geen
  enkele viol: het resultaatatoom werd stil niet afgeleid en een stap voorbij
  de reikwijdte las **leeg**. Gemeten (anker 2026-03-15, tal 3, casusgrenzen
  1900/2200): verifier PASS, `ref` EVALUATION FAULT, Drools
  IllegalStateException — en clingo PASS met leeg. Precies wat ch. 3.8's "the
  refusal is a FAULT and never a value" verbiedt. Gedicht met de zusterarmen
  die de lens aanwees: een **ontbrekende rang in een niet-afgekapte tabel ís
  de reikwijdtefout** — de tabel bevat dan elke treffer die een wandeling ooit
  kon bereiken, dus een rang die zij niet heeft bereikt geen enkele wandeling.
  Rode getuige `corpus/rood/dagsoort-reikwijdte-buiten-tabel`, met alle drie
  de `expectedFault`-velden gepind.
- **F1 — een ongebonden parameter werd stil `Empty`.** `ref` is daar
  fail-fast (`parameter … is unbound in this case`, ch. 5.7); de tabelbouw las
  hem als leeg en verscheepte een stellige tabel op een vergelijking die
  stilletjes onwaar was geworden. Nu faalt clingo in refs woorden. Dekking:
  er droeg **nul** document in enige zone een `ParameterRef` in een
  `resolvedDagsoort` — een clausule zonder getuige. Daarom de golden
  `corpus/golden/jutterswet-havenrooster` (twee cases die de parameter
  **verschillend** binden, zodat elke gepinde waarde bewijst dat hij gelezen
  wordt) plus de rode `corpus/rood/dagsoortparameter-ongebonden` en een
  vertaaltijd-tweeling in de clingo-suite.
- **F5 — dubbele, deels beschuldigende diagnose.** Een startdatum buiten het
  backend-venster trok zowel de venstergrens als een **onterechte**
  reikwijdte-viol. Elke reikwijdteregel draagt nu de in-venster-voorwaarde, en
  de venster-stap-regel ook, zodat er één oorzaak en één zin overblijft;
  gemeten op een dagsoort die elke maand voorkomt. Drie suite-checks pinnen de
  structuur (alle reikwijdteregels dragen de venstervoorwaarde; de arm voor de
  ontbrekende rang bestaat).
- **F2 — `dk_value` draagt een DEELVERZAMELING van WF-031.** `Binair`,
  `Afgerond`, `IfThenElse`, `Let` en `Match` zijn WF-031-schoon en `ref`
  rekent ze; de ASP-backend weigert ze vorm-noemend. De weigering is luid en
  aanvaardbaar als benoemde backend-grens, maar de tekst klopte niet:
  `ASP-TRANSLATION.md` presenteerde WF-031's lijst als de evalueerbare
  verzameling. Herschreven naar de werkelijke deelverzameling, met de
  weigerzin erbij en de opmerking dat elke ontbrekende vorm één arm in
  dezelfde evaluator is. In ch. 3.8 zijn de twee dragende zinnen zo
  geformuleerd dat ze over het **hek** gaan en de F2-grens niet
  mee-overclaimen.
- **F3 en de kan-laters.** `dk_compare`'s lege-operandgedrag is niet langer
  een lokale uitvinding maar ch. 1.2 arm voor arm (ordening onwaar; numerieke
  GELIJK alleen waar bij twee legen, op de datumrij ook dan onwaar; ONGELIJK
  waar bij precies één lege) — en na F1 aantoonbaar onbereikbaar, dus bewust
  als spiegel bewaard. De "in the same words ref uses"-claim is waargemaakt in
  plaats van afgezwakt: clingo's §6.10-datumtijdweigering is nu **woord voor
  woord** refs zin (gemeten, beide regels naast elkaar). De NOTE schrijft de
  klok-vangst nu aan de **min**-rij toe — één seconde kloktijd scheidt daar
  1 maart van 15 maart. De twee nieuwe regels in check-roods
  uitsluitingslijst staan alfabetisch (de hele lijst is opnieuw gesorteerd).
  De "author error"-commentregel in de Drools-runtime claimt geen diagnose
  meer.

**De F2-asymmetrie, eerlijk genoteerd.** Er blijft één plek waar `ref` rekent
en clingo vorm-noemend weigert: een dagsoortvoorwaarde die rekenkunde,
afronding, begrenzing, `Let`, `IfThenElse` of `Match` gebruikt. Gemeten:
`de dag later is dan 10 plus 5` → `ref` PASS, clingo TRANSLATION FAULT (`a
day-kind voorwaarde carries an expression this backend does not evaluate`).
Dat is geen semantisch verschil maar een smalle, per vorm benoemde en
uitbreidbare backend-grens — geen document kan hem stil passeren, en geen
golden kan hem dragen.

### De twee kloven

**(a) `de dag` — GESLOTEN.** Zijn runtimebetekenis staat in
`RUNTIME-SEMANTICS.md` **ch. 3.8**, bij de dagsoortevaluatie — niet in ch. 8.
Motivering ter plaatse: ch. 8 is tijd*selectie* en gaat over de sessiewaarden;
`de dag` is de gebonden dag, dus zijn betekenis hoort waar de binding wordt
gelegd. Ch. 8.2 kreeg een alinea die dat zegt en doorverwijst. `ref` bindt hem
via `env.locals` onder een NUL-sleutel die geen bronnaam kan botsen; in clingo
bestaat er sinds de extensie-tabel géén ASP-zijde meer voor, dus daar is elke
`de dag` per constructie er een buiten zijn binding — en die weigert bij naam.

**(b) Rekendatum in stratum 0 — GESLOTEN met een WF-clausule.** Besluit: niet
toelaatbaar. WF-031's zuiverheidsclausule krijgt een positie-arm: binnen een
dagsoortvoorwaarde is `de dag` de enige toegelaten contextwaarde. Grond: de
uitvoerders evalueren de voorwaarde nu per kalenderdag, dus een
sessieafhankelijk `P_k` zou hetzelfde document dezelfde dag in twee sessies
verschillend laten classificeren — precies wat ch. 5.4 ontkent.
- **Rood-eerst:** de meting van 2026-09-10 ís de getuige geworden
  (`corpus/negatief/wf031-sessiewaarde-in-dagsoort`), die vóór de arm
  `verifier: PASS` en `ref: oracle: PASS (7 expectations)` gaf.
- **Arm-exactheid:** de arm noemt de twee **genormaliseerde** sessienamen —
  `norm`-vergelijking, waar WF-098 byte-exact vergelijkt. Die overlap is
  bedoeld en gemeten: een kleine-letter `rekendatum` op deze positie meldt
  **beide** regels, WF-098 omdat de spelling niet in de gesloten verzameling
  zit en WF-031 omdat de naam waarnaar hij normaliseert hier niet mag staan.
  Een naam buiten de verzameling die géén sessiewaarde is blijft van WF-098
  alleen: `wf098-onbekende-contextwaarde-naam` draagt `de dagX` op exact deze
  positie en meldt nog steeds alleen WF-098. De lege helft (`naam: null`)
  blijft ongemoeid.
- **Nul over-afwijzing, direct gemeten over alle zones:** 676 documenten
  (golden, negatief, rood, pariteitszone) dragen samen **38** `Constante`-
  knopen binnen een `resolvedDagsoorten`-voorwaarde; **36** daarvan zijn
  `de dag`, de andere twee zijn de hier genoemde getuigen.

### Uitvoerderdracht

`ref` loopt de wandeling. De ASP-backend kán dat niet meer: met een
reikwijdte van 40 000 dagen zou een gegronde dag-voor-dag-wandeling per stap
en per subject zoveel dagen materialiseren. Hij gebruikt daarom WF-031's
zuiverheidsclausule: `P_k` is een functie van de dag, van literalen en van
parameters, en de ASP-vertaling is per case, dus de backend **evalueert het
predicaat op vertaaltijd** over een venster en verscheept de **extensie** van
de dagsoort — zijn treffers, elk met hun rang. §6.10 is dan één aggregaat en
§6.11 een rangopzoeking, want "de N-de dag van die soort ná A" ís "de dag met
rang rang(A) + N"; de reikwijdte wordt van diezelfde tabel afgelezen. Eén
grens is van deze backend alleen en luid (het S6a-patroon): het venster dekt
de datums van de case plus de reikwijdte aan beide kanten, en een vraag die
daarbuiten valt wordt hier geweigerd waar `ref` hem rekent — nooit beantwoord
uit een tabel die niet reikt.

Differentieel gemeten buiten de goldens om, ref ≡ clingo op handberekende
data: alle vijf kwantoren in een dagsoortvoorwaarde, een kale vergelijking,
en de geneste §9.8-verwijzing op de gebonden dag positief én ontkend (8 van 8),
plus het jaargebonden 2040-voorbeeld in alle drie de uitvoerders.

### Artefacten

- **golden `jutterswet-bergingsklok`** (met `bron.rgs`): dagsoort
  `springtijdag` = de 1e of de 15e; vier regels (stap vooruit, stap achteruit,
  getekende telling, absolute telling); twee cases, 22 handberekende
  verwachtingen. Speelt in `ref`, clingo én beide Drools-takken.
- **golden `jutterswet-getijklok`** (met `bron.rgs`): de §6.11-stap op
  datum-tijd, tien handberekende verwachtingen, de kloktijd-rand (de min-helft
  van 23:59:59) en leeg/nul. Speelt in alle vier.
- **golden `jutterswet-havenrooster`** (met `bron.rgs`, F1): de eerste
  `ParameterRef` in een dagsoortvoorwaarde in enige zone; twee cases die de
  parameter verschillend binden, acht handberekende verwachtingen. `ref` en
  clingo spelen; beide Drools-takken weigeren, per naam gelijst.
- **rood `dagsoort-onvervulbare-wandeling`**: de reikwijdte, gepind in alle
  drie de uitvoerders.
- **rood `dagsoort-reikwijdte-buiten-tabel`** (F4, nieuw): de ontbrekende rang
  in een niet-afgekapte tabel, alle drie gepind.
- **rood `contextwaarde-de-dag-buiten-dagsoort`**: `de dag` zonder binding.
- **rood `dagsoortverwijzing-andere-dag`**: de §9.8-compositie op een andere
  dag.
- **rood `dagsoortparameter-ongebonden`** (F1, nieuw): de ongebonden
  parameter, in beide uitvoerders in dezelfde zin.
- **negatief `wf031-sessiewaarde-in-dagsoort`**.
- **`ref/test`**: 37 nieuwe gewerkte voorbeelden (643 → 680).
- **`backends/clingo/test`**: 19 nieuwe checks (423 → 442), waaronder de drie
  structuurchecks op de twee hekken en de fail-fast-parametercheck.
- **`backends/drools`**: `DagsoortDuurTest` (JUnit 478 → 496).

### Gates (alle veertien, elk los, op de geland boom)

1 PASS · 2 PASS (247 bestanden) · 3 PASS (247 golden, 399 negatief) ·
4 PASS (583 cases) · 5 PASS (680 voorbeelden, 549 cases) ·
6 PASS (442 checks, 549 cases) · 7 PASS · 8 PASS (34 rode cases, Drools-leg
8 gepind / 26 met reden; 68 fragmenten citeren geen bodemzin) ·
9 PASS (201 checks, 12 goldens) · 10 PASS (322 tests, 61 rgs-goldens) ·
11 PASS · 12 PASS (`EXPECTED_PLAYED` 159 → **161**, `EXPECTED_DRL_PLAYED`
136 → **138**, 52 niet-spelers per naam, JUnit 496) · 13 PASS · 14 PASS.

### Conformance-notitie (spec-first, het WF-017-precedent)

`backends/drools/runtime/RegelSpraakRuntime` is de **productiesemantiek**, hier
geport. Deze kaart wijzigt hem op twee punten, en dat zijn **runtime-
divergenties ten opzichte van upstream `regelspraak-java`** die de
pin-bump-conformance-PR daar moet melden — de spec-first-volgorde van
`CLAUDE.md`, eerst de clausule hier, dan de PR daar:

1. **De wandelgrens.** `rsDatumPlusDagsoort` bond de wandeling aan
   **4 000 000 TOTALE stappen** onder de boodschap
   `onvervulbare dagsoortdefinitie?`. Hier is dat de normatieve **reikwijdte**
   geworden: een **gat** van `DAGSOORT_REIKWIJDTE` (40 000) dagen, met een
   foutzin die geen diagnose claimt. Gevolg voor een conform programma: een
   legitieme grote N (`datum plus 5 000 werkdagen`) die upstream faalde, slaagt
   hier; een dagsoort wiens eerstvolgende voorkomen voorbij de reikwijdte ligt
   faalt hier waar upstream nog 4 000 000 dagen doorliep.
2. **De §9.8-compositie op een andere dag.** `transpiler/kern/Dagsoorten`
   compileerde een geneste dagsoorttoets op elke datum-operand; hier
   compileert hij hem alleen op de gebonden dag en laat de rest ongecompileerd
   (een gewone weigering van die compiler). Gevolg: een document met die vorm
   lowert hier niet waar het upstream wél lowerde.

Beide zijn bewust en gemeten; de derde plek waar dit backend de spec vóór de
geporte semantiek uit implementeert (ch. 7 prioritizedDefaults) staat al in
`CLAUDE.md`'s gate-12-beschrijving. **Deze kaart raakt `frontends/` niet** —
alleen read-only geëmitteerd voor de metingen.

### Beschermde paden — voorstellen, niet gemerged

Gemandateerd door de kaart: `spec/RUNTIME-SEMANTICS.md` (ch. 3.8 de
dagsoortsectie inclusief reikwijdte, restkosten en de datum-tijd-splitsing;
ch. 8.2 de contextwaarde-alinea; ch. 2.10 de uitzonderingsnoot; ch. 3.12 vijf
rijen), `spec/IR-SPEC.md` (ch. 3.7 WF-031's positie-arm, ch. 4.1 de
ContextValue-rij, ch. 5.4, WF-098's vlakheidsalinea, de WF-031-regeltabelrij),
`spec/verifier/**`.
**Niet gemandateerd, wel voorgesteld en hier gemeld:**
`spec/ASP-TRANSLATION.md` (drie plekken die anders onwaar zouden zijn) en de
twee gatescripts `scripts/check-drools.sh` (twee pins met redenblok) en
`scripts/check-rood.sh` (twee regels in de Drools-uitsluitingslijst).

### Registervoorstel voor `spec/CONSTRUCTS.md` (niet aangeraakt)

De dagsoortrij (de rij die §3.12/§9.8 draagt) krijgt er als delta-tekst bij:

> **Duurvormen uitgevoerd (2026-09-19).** §6.10 en §6.11 met een dagsoort als
> eenheid draaien in `ref`, de ASP-backend en beide Drools-takken (goldens
> `jutterswet-bergingsklok` dag-precies, `jutterswet-getijklok` op
> datum-tijd; `RUNTIME-SEMANTICS.md` ch. 3.8). **Vijf diverge-from-v0-delta's**,
> elk omdat de bron zwijgt of zichzelf tegenspreekt: de halfopen telling
> `(A, B]` van §6.10 (de bron noemt dagsoorten daar niet); de achterwaartse
> stap van §6.11 (de bron beschrijft alleen optellen); de 0-regel die het wint
> van "het resultaat is altijd van die soort"; een **reikwijdte van 40 000
> kalenderdagen** waar de bron geen terminatieclausule heeft (de
> Drools-backend gaf die grens als een totaal van 4 000 000 stappen en volgt
> nu de genormeerde reikwijdte); en de **datum-tijd-splitsing** — de stap
> draagt op het kalenderdatumdeel met behoud van kloktijd, de tijdsduur tussen
> twee instants blijft geweigerd tot gedeeltelijke dagen en grensmomenten
> apart bepaald zijn (v0 telt daar over de bare datumdelen). Daarnaast is de
> §9.8-compositie **op de gebonden dag** beperkt: v0 rekende hem op elke
> datum-operand, uchar weigert dat in alle drie de uitvoerders (rood
> `dagsoortverwijzing-andere-dag`).

### Niet gedaan, bewust

D7 (het dagsoortpredicaat als regelvoorwaarde), D8 (een statische arm op
`de dag` buiten zijn binding) en D9 (de dragende lezing van de compositie op
een andere dag) zijn genummerd en gemotiveerd, niet gebouwd. De sleutelkant
van `dagsoortPerEenheid`, WF-017's producentkant en de lege helft
`Constante.naam: null` blijven zoals de kaart ze buiten scope plaatste.
