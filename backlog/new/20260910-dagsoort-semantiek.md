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
