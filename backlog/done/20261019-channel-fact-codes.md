# channel-fact-codes: een §9.5-kanaalfeit draagt de codes van zijn soorten

*Stap 7 van het programma "Engelse namen", kaart 7 van het ontwerp `spec/proposals/MESSAGE-CODES.md` (§4.2, §7).
Mandaat van de eigenaar voor stap 7: lokaal werken, de hoofdsessie merget, niet pushen. Raakt `spec/`, het case-schema
en `scripts/`: de branch blijft een voorstel tot de eigenaar merget. Serieel met kaart 5 van het ontwerp (beide raken
het case-schema en de omgeving van gate 8). Geen `CONSTRUCTS.md`-rij (Fable Q3): een codeset naast het kanaalfeit
verandert geen construct, geen draad en niet de identiteit van het feit (regel × subject).*

Goal: sinds de plak van `meldingscodes` draagt elke foutsoort met een rode getuige een `EF`-code (RUNTIME-SEMANTICS
ch. 5.7b), maar het §9.5-kanaal heeft er geen. Een §9.5-feit faalt niet, dus geen rode case kan het getuigen; ref gooit
de reden bij het inserten weg (`ref/lib/eval.ml`, `over_subjects`), clingo kent alleen `inconsistent/2`, Drools alleen
`Inconsistentie(regel, subject)`. Een golden die `inconsistent` pint, ziet daardoor niet welke soort het feit
neerlegde. Deze kaart:

1. legt in RUNTIME-SEMANTICS vast dat de identiteit van een kanaalfeit regel × subject blijft en dat de codes die voor
   dat paar worden neergelegd een verzameling naast het feit vormen, die de kanaallezing niet ziet (ch. 5.6); geeft
   elke §9.5-soort met een golden getuige een rij in de tabel van ch. 5.7b, kanaal `§9.5`; en noemt de soorten zonder
   getuige onder "Kinds without a code";
2. geeft de `inconsistent`-verwachting van het case-schema een optionele `code` (`^EF-[0-9]{3}$`): ten minste één
   passend kanaalfeit draagt die code;
3. laat ref, clingo en Drools de codeset op het kanaalfeit dragen en een gepinde code toetsen;
4. pint de code als golden getuige: in elke `inconsistent`-verwachting van een golden buiten `anchor/` en buiten een
   golden met `scenario.rgs`, en in een nieuwe golden case waar een soort geen getuige heeft;
5. voert de besloten verhuizing van EF-005 (deling door een numerieke nul) naar §9.5 uit (RUNTIME-SEMANTICS ch. 1.4
   punt 4), voor zover het mandaat dat toelaat (zie "Gemeten vóór de bouw").

## Gemeten vóór de bouw (2026-10-19, main 0ba90bff)

- De tien producenten van ch. 5.6, 4.4 en 7.2, in de volgorde van het ontwerp §4.2: een geschonden consistentie; een
  dubbele uniciteitssleutel; een declared-unordered defaultsgroep met verschillende beslissingen; vier
  verdelingsdegradaties (gewicht leeg bij naar rato; gewicht leeg bij de tie-break van de volgorde; negatief gewicht bij
  naar rato; maximum leeg of negatief); deling door een lege deler onder een gevuld deeltal; een negatieve radicand; een
  leeg anker van een variabel startpunt.
- Gemeten met ref over alle 823 golden cases: 64 cases dragen kanaalfeiten (106 feiten), met 103
  `inconsistent`-verwachtingen; elk feit hoort bij één soort. Getuigen zijn er voor de consistentie, de uniciteit (enkelvoudig en unie), de
  defaultsgroep, gewicht leeg bij naar rato (één case), gewicht leeg bij de tie-break (twee cases) en de deling door
  leeg. Geen golden legt een maximum leeg of negatief neer: die soort krijgt een nieuwe golden case.
- Zonder getuige, dus zonder code (een code komt met haar getuige, ch. 5.7b): de negatieve radicand (de clingo-backend
  vertaalt de wortel niet, en gate 6 eist dat clingo elke golden speelt); het lege anker van een variabel startpunt
  (geen executor legt een raster per object); het negatieve gewicht bij naar rato (besloten voor v1 in ch. 4.4,
  alleen door de Drools-backend uitgevoerd: ref laat het ongeklemd, clingo faalt met een `viol/2`).
- De verhuizing van EF-005 verandert gedragsverwachtingen in testbestanden: `ref/test/test_ref.ml` (6 regels, onder
  meer `ch2.2-div-zero`, dat de fout eist), `backends/clingo/test/test_clingo.ml` (29 regels die de fout of haar
  `viol/2` pinnen) en 23 testbestanden van de Drools-backend (72 regels, onder meer `ZeroDivisorWitnessTest`). Het
  mandaat voor deze kaart laat in een testbestand alleen een mechanische vormwijziging toe. Verder kan clingo twee van
  de acht rode getuigen niet als golden spelen (`begrensd-reads-bounds-in-order`, `som-fallback-always-evaluated`:
  vormen die het weigert). Zonder toestemming voor die testwijzigingen voert deze kaart de verhuizing niet uit; het
  Result legt vast wat zij vraagt.

Acceptance:
- (a) RUNTIME-SEMANTICS ch. 5.6 met de codeset naast het feit, met een uitgewerkt voorbeeld; ch. 5.7b met een rij per
  nieuwe code, kanaal `§9.5`, en per rij ten minste één golden getuige die de code pint. Gate 8 houdt elke golden
  getuige aan de code van zijn rij (bestond al) en eist nieuw dat elke code die een golden `inconsistent`-verwachting
  pint een rij is met kanaal `§9.5`.
- (b) `spec/schema/uchar-case-v0.schema.json` met `inconsistent.code`; de patroonzelftest van gate 4 kent de nieuwe
  positie; gate 4 groen.
- (c) ref, clingo en Drools (beide takken) laten een golden met een gepinde code alleen slagen waar zij voor een passend
  feit die code neerleggen (gates 5, 6 en 12).
- (d) Elke nieuwe toets eerst rood tegen een mutatie, daarna groen, vastgelegd in het Result: ref legt voor één soort
  een andere code neer (gate 5 rood); clingo idem (gate 6 rood); Drools idem (gate 12 rood); een golden pint een code
  die geen §9.5-rij is (gate 8 rood); het patroon op de nieuwe positie verruimd (gate 4 rood).
- (e) Vloer `EXPECTED_MIN_GOLDEN_CODES` in gate 8: het aantal golden `inconsistent`-verwachtingen met een code,
  gemeten; `EXPECTED_MIN_EF_CODES` naar het gemeten aantal rijen.
- (f) Geen tekst van een bestaande melding verandert. Een testbestand verandert alleen waar een vorm mechanisch moet
  meegaan; geen verwachting over gedrag verandert; de aantallen staan in het Result.
- (g) Een nieuwe golden case haalt het volledige differentiaalvierkant (ref ≡ clingo ≡ Drools) met een handgeschreven
  absolute verwachting.
- (h) De verhuizing van EF-005: uitgevoerd, of, zonder toestemming voor de gedragswijzigingen in testbestanden, in het
  Result vastgelegd met wat zij vraagt.
- (i) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch.

May touch: `spec/RUNTIME-SEMANTICS.md` (ch. 5.6, ch. 5.7b; ch. 1.4 punt 4 alleen voor de verhuizing),
`spec/schema/uchar-case-v0.schema.json`, `ref/lib/`, `backends/clingo/lib/` en `bin/`, `backends/drools/src/main/`,
de testbestanden van ref, clingo en Drools alleen mechanisch, `scripts/check-case-schema.sh`, `scripts/check-rood.sh`,
`corpus/golden/` buiten `anchor/` en buiten goldens met `scenario.rgs` (`code` in `inconsistent`-verwachtingen, nieuwe
case files), `spec/proposals/MESSAGE-CODES.md` §7, deze kaart.

Out of scope:
- `params` van een code, codes op de draad, catalogi, executorcodes, het bodemzinhek: kaarten 5 en 6 van het ontwerp.
- `corpus/golden/anchor/` (beschermd) en goldens met `scenario.rgs` (gate 16 genereert hun case files uit de
  scenariotaal, die geen code kent).
- Het negatieve gewicht bij naar rato uitvoeren, de wortel in clingo, het variabele startpunt: elk een eigen kaart.
- `CONSTRUCTS.md`, `PLAN.md` en `CLAUDE.md`: de hoofdsessie; het Result geeft de voorgestelde tekst.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-19)

Commits op `card/channel-fact-codes`:
- `b1239dd6` — de kaart; `6b96afac` — naar `doing/`.
- `b350098c` — ref en clingo dragen de codeset op het §9.5-kanaalfeit.
- `3544b202` — de Drools-backend draagt de codes op `Inconsistentie`.
- `993fe40f` — RUNTIME-SEMANTICS ch. 5.6 en 5.7b, `inconsistent.code` in het case-schema, gates 4 en 8, de golden
  pins en twee nieuwe cases.
- `ad23681d` — `spec/proposals/MESSAGE-CODES.md` §7: kaart 7 gedaan, met wat open blijft.
- de commit met dit Result.

Codes (acceptance (a)), in ch. 5.7b op kanaal `§9.5`, parameters `rule`, `label`; de getuigen in de tabel en het
aantal golden pins:
- EF-025 een consistentie houdt niet: `jutterswet-vondstnummer`, `jutterswet-keurcriteria`; 28 pins.
- EF-026 een uniciteitssleutel komt dubbel voor, in één bron of over de bronnen van een unie: `jutterswet-vondstnummer`,
  `jutterswet-uniciteit-drie-bronnen`; 6.
- EF-027 de defaultsgroepen van een declared-unordered groep beslissen verschillend, aan de wortel of genest:
  `jutterswet-wachtgeld`, `synth3-nestkorting`; 7.
- EF-028 leeg gewicht bij naar rato: `jutterswet-verdeling-dubbelbezet`; 1.
- EF-029 leeg tie-breakgewicht bij op volgorde: `jutterswet-rangmaximering`, `jutterswet-verdeling-dubbelbezet`; 3.
- EF-030 leeg of negatief maximum: `jutterswet-berging`, `jutterswet-rangmaximering`; 3.
- EF-031 een gevuld deeltal gedeeld door een lege deler: `jutterswet-bergloon`, `synth3-takhek`,
  `synth3-operandvolgorde`; 25.

Ch. 5.6 kreeg de clausule (identiteit regel × subject, de codes als verzameling ernaast, een soort zonder code legt
het feit neer zonder code, de kanaallezing ziet de set niet) met een uitgewerkt voorbeeld van één feit met twee codes;
ch. 5.7b noemt de drie §9.5-soorten zonder getuige onder "Kinds without a code": de negatieve radicand, het lege
anker van een variabel startpunt, het negatieve gewicht bij naar rato (alleen de Drools-backend voert het uit).

Golden getuigen (acceptance (g)): 73 pins in 40 case files. 69 bestaande `inconsistent`-verwachtingen in 38 case files
kregen de code die ref meet (elke verwachting treft feiten met precies één code; geen overgeslagen). Twee nieuwe,
handgeschreven cases: `jutterswet-berging/case2-empty-and-negative-cap.json` (W zonder maximum, D met maximum -5:
W 600, H 200, D 400, rest 0; W en D EF-030) en `jutterswet-rangmaximering/case3-empty-weight-and-cap.json` (W2 zonder
gewicht en zonder maximum: W1 300, W2 0, W3 700, rest 0; één feit op W2 met EF-029 én EF-030). Beide spelen in ref,
clingo en de twee Drools-takken; de no-op-runner faalt erop. Niet gepind: `anchor/` (beschermd) en de vijf goldens
met `scenario.rgs` die een `inconsistent` pinnen (gate 16 genereert hun case files uit de scenariotaal).

Executors (acceptance (c)):
- ref: elke deponering geeft een `Value.deposit = { code; text }` aan de `err` van haar regel × subject;
  `World.error_fact` kreeg `codes` (oplopend, elk één keer) en `insert_error ~code` voegt een code toe aan het ene
  feit. `Case.Inconsistent` kreeg `code`.
- clingo: de helper `deposit` schrijft naast elke `inconsistent/2`-regel een `inconsistent_code/3` met dezelfde body
  (negen plekken), met `#defined`/`#show`; `pre_clone` schrijft het predicaat niet opnieuw. `Model.check` zoekt een
  gepinde code in `inconsistent_code/3` over hetzelfde paar.
- Drools: de sonde (`DepositFlag`) verzamelt de codes die zij hoort (deling EF-031, wortel zonder code);
  `MemberTest` en `Term.FoldRead` dragen codes; `Allocation.Result.depositors` is een map naar codes;
  `Consequence.Inconsistency` kreeg `codes`; `StackFact.Inconsistency.CODES` is EF-027; de route `DEPOSIT_CODES` is
  nieuw, `REPORT` en `INCONSISTENCY` kregen een lijst; de gegenereerde bean `Inconsistentie` kreeg `codes`.
  Een gevolg in de rechterkant leest de codes uit zijn sonde; een toets in de linkerkant hoort zijn plaatsen samen,
  dus daar geeft het plan de code, en een voorwaarde of vouwfilter met een deponerende deling én wortel wordt
  geweigerd: DROOLS-290 `deposit-kinds-mixed` (geen golden en geen test heeft zo'n vorm). `CaseRunner` toetst een
  gepinde code over de feiten van het paar.
- Geen tekst van een bestaande melding veranderd (acceptance (f)): zonder gepinde code zijn de mismatchzinnen van ref,
  clingo en `CaseRunner` letterlijk dezelfde; met een code is er een nieuwe zin met de codes per feit. De PASS-regel van
  gate 8 is ongewijzigd; de nieuwe telling staat op een eigen regel.

Testaanpassingen, alle mechanisch, geen gedragsverwachting veranderd: `ref/test/test_ref.ml` 2 regels (de annotatie
van `no_err` en `no_alloc_err`, `string` → `Value.deposit`); `backends/clingo/test` 0; Drools 4 bestanden, 19 regels:
`RouteWitnessTest` 5 (drie vastgepinde signaturen, twee aanroepen van `report`), `FormsPlan` 6 (twee constructies van
`FoldRead`, één van `Consequence.Inconsistency`, drie vastgepinde DRL-fragmenten), `ChannelPlanWitnessTest` 7 en
`RulePlanWitnessTest` 1 (het planveld `codes=[…]`).

Gates 4 en 8 (acceptance (b), (e)): gate 4 kent de positie `/$defs/expectation/oneOf/6/properties/code` (9
zelftestrijen, 110 in totaal). Gate 8 eist dat elke code die een golden `inconsistent`-verwachting pint een rij op
kanaal §9.5 is; vloeren `EXPECTED_MIN_GOLDEN_CODES=73` (nieuw, gemeten) en `EXPECTED_MIN_EF_CODES` van 24 naar 31.

Mutaties (acceptance (d)), elk op de gecommitte stand van `993fe40f`, daarna met `git checkout` teruggezet; logs in
de werkmap van de kaart (`mutations/`):
1. ref: consistentie EF-025 → EF-026 (`ref/lib/eval.ml`). Gate 5 rood: 14 case files, 28 MISMATCH-regels, alle
   EF-025-pins.
2. clingo: deling door leeg EF-031 → EF-030 (`translate.ml`). Gate 6 rood: 13 case files, 25 MISMATCH-regels, alle
   EF-031-pins.
3. Drools: maximum bij naar rato EF-030 → EF-031 (`Allocation.java`). Gate 12 (`--fast`: exec-model met alle pins)
   rood: `jutterswet-berging` speelt niet, "verwacht inconsistent (verdeel de opbrengst van een berging @ W) code
   EF-030, niet gevonden onder … [EF-031]". Los daarvan faalt een gemuteerde kopie van case3 (EF-031 gepind) in beide
   Drools-takken.
4. Gate 8: `synth3-takhek/case2` pint EF-005. Rood met twee regels: de rij EF-031 mist daar haar getuige, en EF-005
   staat op kanaal 'fault'.
5. Gate 4: het patroon op de nieuwe positie verruimd tot `^EF-[0-9]+$`. Rood: 'EF-05' en 'EF-0005' aanvaard.

Gates (acceptance (i)), één volledige run op `ad23681d` (2026-10-19 03:11–03:29): alle 16 groen. Gate 5 825 cases en
1286 voorbeelden; gate 6 825 cases en 2198 checks; gate 8 50 rode cases, 93 codetoetsen, 31 foutsoorten, 73 golden
codepins; gate 12 255 gespeeld in beide takken, JUnit 1619; gate 4 875 case files; gate 7 780 cases.

Open:
- De verhuizing van EF-005 (acceptance (h)) is niet uitgevoerd. Zij verandert gedragsverwachtingen in
  `ref/test/test_ref.ml` (6 regels, onder meer `ch2.2-div-zero`), `backends/clingo/test/test_clingo.ml` (29 regels) en
  23 Drools-testbestanden (72 regels, onder meer `ZeroDivisorWitnessTest`), en de uitgewerkte voorbeelden van
  RUNTIME-SEMANTICS ch. 3.4c, 5.10, 5.11 en 10.6 die de fout als uitkomst pinnen. Het mandaat liet alleen mechanische
  testwijzigingen toe. Twee van de acht rode getuigen kan clingo niet als golden spelen
  (`begrensd-reads-bounds-in-order`, `som-fallback-always-evaluated`). Nodig: toestemming van de eigenaar voor die
  wijzigingen en een besluit over die twee getuigen; dan een eigen kaart.
- Drie §9.5-soorten zonder code (zie boven), elk tot er een getuige is.
- `params` op de §9.5-codes: kaart 5 van het ontwerp, zoals voor de foutcodes.
- Codes in `anchor/` en in de scenario-goldens: de eigenaar (anchor) en een kaart voor de scenariotaal.

Voor de hoofdsessie:
- Geen `CONSTRUCTS.md`-rij (Fable Q3).
- Voorstel voor de alinea van gate 8 in `CLAUDE.md` (beschermd): vervang "floors `EXPECTED_MIN_RED_CODES=93` and
  `EXPECTED_MIN_EF_CODES=24` (measured 2026-10-18)" door "floors `EXPECTED_MIN_RED_CODES=93` (measured 2026-10-18) and
  `EXPECTED_MIN_EF_CODES=31` (24 measured 2026-10-18, raised 2026-10-19 with the seven §9.5 codes)", en voeg aan het
  eind toe: "Since 2026-10-19 (card `channel-fact-codes`) a §9.5 channel fact carries the EF codes of its kinds beside
  its identity rule × subject (RUNTIME-SEMANTICS ch. 5.6), and a golden `inconsistent` expectation may pin one
  (`code`), which gates 5, 6 and 12 hold ref, clingo and both Drools branches to; this gate holds every code a golden
  pins to a row of the ch. 5.7b table on the §9.5 channel, floor `EXPECTED_MIN_GOLDEN_CODES=73` (measured
  2026-10-19)."
