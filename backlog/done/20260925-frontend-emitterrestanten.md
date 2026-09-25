# frontend-emitterrestanten: de laatste emissies die de wire niet draagt, vóór de pin-bump

Restpunten uit het Result van de L5-kaart (open punt 6) en van de kaart
rgs-getuigen-bronloze-constructen (open punt O1), gebundeld omdat elke fix
de emissie van de frontend verandert en dus goldens raakt: na de pin-bump
zou dat een tweede bump betekenen. Maintainersbesluit 2026-09-25: deze
kaart hoort bij de laatste vóór de bump, samen met
dialect-opgave-boven-berekening en verifier-allocatie-keten.

Gemeten (2026-09-25):

- De frontend schrijft `type: null` op de builtin-aanroepen `dayCount` en
  `concatenation`; WF-034 weigert zo'n document (pre-L5 gedrag, nooit in
  een golden omdat de vorm er niet voorkomt).
- De emitter kan geen `DimensionSelection` schrijven: hij weigert de vouw
  met een dimensieselectie.
- De frontend schrijft nooit `connectingRole` op een objectcreatie, ook
  niet als de zin de rol noemt (IR-SPEC ch. 3.3 laat het veld weg en RS
  ch. 5.5 leidt de relatie af; bij een zelfgetypeerde creatie is de
  terugverwijzing niet afleidbaar, dus die vorm heeft geen
  RegelSpraak-route: de ankers wachtersketen en budgetgrens-onder en de
  golden opvolgerquorum zijn handgeschreven).
- De Java-emitter weigert een lege brontekst met een Nederlandse melding
  waar de OCaml-lader Engels spreekt (L5 open punt 2); geen wire-gevolg.

## Acceptatie (bij oppakken aanscherpen)

- (a) Per restant een rood-eerst JUnit-getuige; `type` op `dayCount` en
  `concatenation` afgeleid volgens RS ch. 2.10 (een geheel getal dagen,
  een tekst); een `DimensionSelection` geëmitteerd waar de zin er een
  schrijft, met een golden (handgerekend, ref ≡ clingo ≡ Drools waar
  gedragen); `connectingRole` geschreven waar de zin de rol noemt, met een
  golden die de zelfgetypeerde vorm uit de bron haalt — meet welke
  bestaande goldens daardoor anders emitteren (`jutterswet-wrakboeking-rgs`,
  `jutterswet-wrakboekwaarde`) en her-emitteer ze met byte-identieke cases.
- (b) De meldingstaal van de lege brontekst gelijk aan de OCaml-lader
  (Engels; gelijke zin, U10 van L5).
- (c) Gate 10 byte-identiek over alle rgs-goldens; gate 11 byte-identiek
  (de toka-casus schrijft geen van deze vormen: meten, alleen tellen).
- (d) Gate-12-pins (**voorstel**) voor nieuwe goldens; alle veertien gates
  groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/**` (nieuwe goldens
en her-emissies met byte-gelijke cases), `scripts/check-drools.sh` en
`scripts/check-noop.sh` (**voorstel**), kaartverplaatsing. Niet in deze
kaart: de spec en de verifier.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Typen: lees IR-SPEC ch. 4.2 (de `BuiltinCall`-tabel: het resultaattype van `dayCount` en `concatenation`) en RUNTIME-SEMANTICS ch. 2.10; meet eerst op een proefdocument wat de frontend nu schrijft (`type: null`) en wat de verifier zegt (WF-034); de emitter schrijft daarna precies het type dat de spec afleidt; de verifier is de arbiter (schoon). Rood-eerst JUnit per vorm.
- U2 `DimensionSelection`: `IrEmitter.java` r. ~805 weigert een dimensieselectie op een vouw (`facetWeigering`); emitteer haar als `Aggregation.dimensionSelection` (`DimensionSelection {dimension, labels}`, schema r. ~2017); golden met `bron.rgs` en handgerekende cases (ref ≡ clingo); Drools weigert gedimensioneerde invoer per vorm (bekende auto-refusal) → `EXPECTED_CHANNEL`-regel met reden (**voorstel**), meten.
- U3 `connectingRole`: de allocatie schrijft hem al (r. ~1391–1410, `verbindendeRolBinding`); de objectcreatie schrijft hem waar de zin de rol noemt (lees hoe de resolver de rol van een objectcreatie kent; is die binding afleidbaar uit het typenpaar, schrijf hem dan ook — de wire laat het veld toe en een lezer die hem heeft hoeft niets af te leiden). Her-emitteer `jutterswet-wrakboeking-rgs` en `jutterswet-wrakboekwaarde` (cases byte-gelijk; hun NOTE-diff tegen de drager krimpt — NOTE bijwerken); een golden voor de zelfgetypeerde creatie uit de bron: een tweeling van `jutterswet-opvolgerquorum` (of het kleinste anker `jutterswet-wachtersketen`/`synth2-budgetgrens-onder`; ankers zijn beschermd, de tweeling komt in `corpus/golden/`), cases byte-gelijk, ref ≡ clingo; Drools meten (de exec-tak speelt een creatie met binding).
- U4 Meldingstaal: `IrEmitter.java` r. ~1150 (lege brontekst): Engels, letterlijk de zin van de OCaml-lader (zoek haar in `spec/verifier/lib/` op "snippet"); de WF-113 (b)-melding r. ~1132 idem als die afwijkt.
- U5 Parallel bouwt de kaart dialect-opgave-boven-berekening aan de grammatica en de resolver (de lowering van de opgave-markering); jij raakt `IrEmitter.java` en alleen de resolverplek van de objectcreatierol. De hoofdsessie rebaset serieel.
- U6 Gates 1–8, 10, 11, 12, 13; pins als voorstel.

## Result (2026-09-25, bouwer)

## Result — frontend-emitterrestanten

### Ronde 8 (resterende lezerszinnen) — branch `fix/lens-zinnen`, kop **`6aff9ba0`** (na de slotronde; daarvóór `9d907639`)

#### Stand
- Nieuwe branch `fix/lens-zinnen` bovenop `fix/lens-emitter` (`147eabd3`), worktree
  (worktree emitter). Elf commits, werkboom schoon, niets gepusht of gemerged, geen
  beschermd pad en niets onder `toka/`, `spec/`, `scripts/`, `corpus/`, `ref/`, `backends/` geraakt (gemeten:
  `git diff --stat 147eabd3..HEAD` over die paden is leeg). Uit de regio's van de parallelle fixer gebleven: de hunks
  in `RuleChecker` liggen op r. 186–194, 281–292, 396–417, 441–448, 467–496, 701–711, 1975–1984, 1993–2048,
  2059–2071, 2094–2100, 2110–2116, 2161–2173, 2250–2267, 2873–2880, 3184–3192, 3196–3211, 3214–3222, 3446–3493
  (basisnummering, met drie regels context), niet op 101–110 of rond 1506/1692; in DIVERGENCES alleen §4,
  §8.4.3, §13.4.17.47 en de lijst "Producer refusals without a reader sentence", niet §7.
- Commits (onderwerp ≤ 72, één per familie):
  - `f3f42f68` resolver: de ontkende kenmerkregel spreekt de zin van WF-022
  - `66b22dba` resolver: de tweede schrijver naast een defaults-groep in WF-050
  - `e3b0c0f5` resolver: recursieve groep en blok op een groepslid in WF-033
  - `d654d923` resolver: versieoverlap en dubbele rijnamen in de zinnen van de verifier
  - `d686a767` resolver: de weigeringen van de startpuntbepaling in WF-032
  - `dad268ab` resolver: de setlezing buiten een vouwbron in WF-053 en WF-093
  - `48cecf4c` resolver: venstergrens en vensterpositie in WF-111 en WF-112
  - `2ad55f12` resolver: een onbekend lid bij een objectcreatie in WF-027
  - `92de72c3` resolver: dubbele afkorting en kalenderdagsoort in WF-009 en WF-017
  - `3c25a2ca` resolver: de vorm van een telling in de zinnen van WF-037
  - `9d907639` DIVERGENCES en guard: de structurele weigeringen zonder lezerszin
  Elke commit is gebouwd en de volledige suite groen gedraaid (729, 731, 734, 738, 742, 744, 749, 750, 752, 755, 755).
- JUnit 728 → 755: nieuwe getuige `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/StructuurZinTest.java`
  (27 tests). Productiecode: `resolver/RuleChecker.java`, `resolver/ExpressieBouwer.java`,
  `resolver/DefinitionResolver.java`, `emit/IrEmitter.java` (alleen: `navigatieBindingen` ook als publieke statische
  methode, de instantiemethode delegeert), `grammar/DIVERGENCES.md`.

#### Meetmethode
- Gepatchte kopie van de frontend ((buiten de repo) → (buiten de repo)): elke onderzochte weigering is
  daar uit te schakelen met `-Dzinnen.uit=<sleutel>`, zodat de frontend het document schrijft dat de weigering tegenhield.
  Proefbronnen (buiten de repo) (sleutel op regel 1), vergelijking (buiten de repo): elke ERROR van
  de nieuwe frontend moet eindigen op `": " + <zin>` met `<zin>` de tekst na `WF-### FAIL <pointer>: ` van de verifier
  (`dune build --root spec/verifier`, `spec/verifier/_build/default/bin/verifier.exe`) op dat document. Waar de
  frontend geen document kan schrijven heb ik het met de hand gebouwd uit een geldige emissie
  ((buiten de repo): WF-009 ×2, het ongedeclareerde startpuntonderwerp, het gevulde aantal over een
  gewone attribuutlezing ×2).
- Uitkomst over alle 65 proeven: 54 proeven GELIJK (55 meldingen; één proef draagt twee weigeringen), 11 houden
  Nederlands met hun gemeten reden (hieronder; één daarvan is een syntaxfout, `Dagsoort de Uren`, geen weigering van
  deze opdracht).

#### Per plek: oud → nieuw, met de meting (regels op `147eabd3`)
1. **WF-022**, `RuleChecker.java:285`. Oud: `regel 'vlak': een gewone regel kan een kenmerk niet ontkennen (§9.2) — zonder
   toekenning is 'uitgesloten' al onwaar (§3.5), en een gewone regel kent een kenmerk op de wire alleen 'waar' toe
   (WF-022). Een ontkenning (…) staat alleen in een regel die een default wordt: … (dialect §4)`. Nieuw: `regel 'vlak':
   characteristic target's value must be the boolean constant true` (tabelrij: `regel 'Uitsluitingstabel 1' (een rij van
   beslistabel 'Uitsluitingstabel'): …`). Verifier: `WF-022 FAIL /rules/rules/0/versions/0/value: characteristic target's
   value must be the boolean constant true`.
2. **WF-050, andere schrijver**, `RuleChecker.java:402`. Oud: `Uitzondering op 'basispremie': het doel Jutter.premie heeft
   naast de defaults-groep 'basispremie' nog een schrijver: regel 'andere premie' — … (WF-050). Neem de regel op in de
   groep, of geef hem een ander doel`. Nieuw: `Uitzondering op 'basispremie' (defaults-groep 'basispremie', doel ook
   geschreven door regel 'andere premie'): a rule also writes defaults target 'Jutter.premie' — an unordered plain writer
   would silently defeat the group's decisions`. Verifier: `WF-050 FAIL /rules/prioritizedDefaults/0: a rule also writes
   defaults target 'Jutter.premie' — an unordered plain writer would silently defeat the group's decisions`.
3. **WF-050, meer groepen**, `RuleChecker.java:411`. Oud: `Uitzondering op 'tweede basis': het doel Jutter.premie heeft
   meer dan één defaults-groep ('basispremie' en 'tweede basis') en niet elke groep is ongeordend — … (WF-050). Zet de
   uitzonderingen onder één basisregel, …`. Nieuw: `Uitzondering op 'tweede basis' (defaults-groepen 'basispremie' en
   'tweede basis'): multiple defaults groups on target 'Jutter.premie' without the declared-unordered marking — priority
   is explicit (defaults) or explicitly absent ('unordered'), never implicit (ch. 7.2)`. Verifier: dezelfde zin op
   `/rules/prioritizedDefaults/0` en `/1` (per groep; de frontend meldt één keer per doel).
4. **WF-033, recursieve groep**, `RuleChecker.java:190`. Oud: `recursieve regelgroep 'estafette' zonder objectcreatieregel —
   de 'is recursief'-markering kan de keten niet laten voortschrijden (§9.10, gedocumenteerde extensie)`. Nieuw:
   `recursieve regelgroep 'estafette': recursive group without a creation member`. Verifier: `WF-033 FAIL
   /rules/ruleGroups/0: recursive group without a creation member`. **Blijft Nederlands**: een groep met alleen een
   feitcreatie als lid; verifier PASS (proef `wf033feit`), want hij aanvaardt een relatiecreatie als drager (ch. 5.5).
5. **WF-021**, `RuleChecker.java:2260`. Oud: `geldigheidsperioden van regelversies overlappen (regel 'premie')`. Nieuw:
   `regel 'premie': validity windows of versions 'premie #1' and 'premie #2' overlap (rules)`; objectcreatie: `…
   'werf de opvolger #1' and 'werf de opvolger #2' overlap (objectCreations)`. Verifier: `WF-021 FAIL
   /rules/rules/0/versions/0: validity windows of versions 'premie #1' and 'premie #2' overlap (rules)` (hij meldt elk
   overlappend paar, bij vier versies vier; de frontend noemt het eerste). De rij volgt de soort van de versies
   (`rules`, `objectCreations`, `allocations`, `consistencies`, `uniquenesses`, `factCreations`), het nummer de plaats
   onder de geschreven versies. **Blijft Nederlands**: een dagsoortdefinitie (verifier: `WF-031 … more than one
   definition for day kind 'springtijdag'`, WF-021 vergelijkt haar niet) en versies van twee soorten (twee rijen).
6. **WF-032, tijdlijn**, `RuleChecker.java:3189`. Oud: `startpuntbepaling: tijdlijn 'penningjaar' is niet gedeclareerd (regel
   'startpunt penningjaar')`. Nieuw: `startpuntbepaling (regel 'startpunt penningjaar'): start point for undeclared
   timeline 'penningjaar'`. Verifier: `WF-032 FAIL /rules/startPointDeterminations/0: start point for undeclared timeline
   'penningjaar'`.
7. **WF-112**, `RuleChecker.java:704`. Oud: `periodevergelijking onder 'ten minste 1' (als lid van een samengestelde
   voorwaarde): een venster (§8.4.3) maakt de REGEL tijdsafhankelijk, … (WF-112, IR-SPEC ch. 4.6). Zet het venster buiten
   die kwantor: …`. Nieuw: `periodevergelijking onder 'ten minste 1' (als lid van een samengestelde voorwaarde): a time
   window under none or a counting quantifier: the window gates the RULE, and no reading is specified for a verdict that
   is counted or complemented`. Verifier: die zin op `…/condition` en, onder geen-van-de binnen alle, op
   `…/condition/parts/1`.
8. **WF-111**, `RuleChecker.java:3453` (`periodeGrensFout`). Oud: `periodevergelijking (§8.4.3): de vanaf-grens 'de
   peilparameter' is de parameter 'peilparameter' — een venstergrens is een datumliteraal of … (WF-111, IR-SPEC ch.
   4.6)`. Nieuw: `periodevergelijking (de vanaf-grens 'de peilparameter'): the from bound of a time window is the
   parameter 'peilparameter' — a bound is a date literal or an own scalar date attribute of the subject; the general
   date expression is an OPEN wire decision`, met als middendeel ook `a navigated read of '<a>'`, `a computed date
   expression`, `the time-dependent attribute '<a>'`, `the attribute '<a>' at a position with no subject to own it`;
   millisecondeprecisie (attribuut of literal): `periodevergelijking (de vanaf-grens '…'): the from bound of a time window
   is a millisecond-precision date: the window is a range of civil days and no clause reads an instant bound`; `tot` →
   `to`. Verifier: 11 proeven letterlijk gelijk op `…/condition/from|to`. **Blijft Nederlands**: een lezing op een ander
   objecttype (ook een sub- of supertype) die geen feittype bereikt, en een geschreven navigatie zonder afleidbaar pad:
   de emitter weigert dan het model (`IllegalStateException … navigeert, en de resolver vond daarvoor geen unieke
   binding`), dus geen document (proeven `wf111_09`, `wf111_10`); mét relatie is het de navigatiezin (`wf111_12/13`). De
   beslissing wát geweigerd wordt is ongewijzigd; alleen de zin volgt het document.
9. **WF-027**, `RuleChecker.java:2877`. Oud: `objectcreatie: 'graad' is geen attribuut of kenmerk van Berger (§13.4.6.2)`.
   Nieuw: `objectcreatie: 'graad' is not a declared attribute of 'Berger'`. Verifier: `WF-027 FAIL
   /rules/objectCreations/0/versions/0/initialisations/0: 'graad' is not a declared attribute of 'Berger'`.
10. **WF-009**, `DefinitionResolver.java:527`. Oud: `eenheidstelsel 'Gewicht': de afkorting 't' is al gedeclareerd voor de
    eenheid 't'; één eenheid per afkorting (WF-009), dus deze tweede declaratie wordt geweigerd in plaats van de eerste
    stil te vervangen`. Nieuw: `eenheidstelsel 'Gewicht', al gedeclareerd voor de eenheid 't': the unit 't' is declared
    twice in the unit system 'Gewicht': one Unit per abbreviation (WF-009)`; hoofdlettervariant: `eenheidstelsel
    'Gewicht', als 'kg' al gedeclareerd voor de eenheid 'kg': the unit 'KG' is declared twice in the unit system
    'Gewicht': one Unit per abbreviation (WF-009)`. Verifier (handgebouwd, beide eenheden naast elkaar): `WF-009 FAIL
    /model/unitSystems/0/units/2/abbreviation: the unit 't' is declared twice in the unit system 'Gewicht': one Unit per
    abbreviation (WF-009)`, idem voor `'KG'`. Met de weigering uit schrijft de frontend zelf een document waarin de tweede
    declaratie de eerste stil vervangt (verifier PASS), of gooit de emitter (hoofdlettervariant): vandaar handgebouwd.
11. **WF-017**, `DefinitionResolver.java:893`. Oud: `dagsoort 'wk' heet naar een kalendereenheid uit de tijdseenheidtabel en
    kan geen dagsoort zijn`. Nieuw: `dagsoort 'wk': day-kind 'wk' shadows a built-in calendar unit`. Verifier: `WF-017 FAIL
    /model/dayKinds/0/name: day-kind 'wk' shadows a built-in calendar unit` (ook `'secondes'`).
12. **WF-037, opgesomde telling**, `ExpressieBouwer.java:1747`. Oud: `opgesomde telling: '<zin>' noemt 2 operanden — een
    telling vouwt de INSTANTIES … (WF-037, IR-SPEC ch. 4.3). Wat de kop over de LIJST zegt is waar; over de ZIN niet — …
    Die route opent hier niet — <reden>`. Nieuw: `opgesomde telling '<zin>': a count folds INSTANCES and has no LISTED
    form, and this node carries 2 operands: the source language spells a count over a subject expression alone (ch. 4.3),
    so there is no articulated operand list to admit — the only reading an operand list leaves a count is the number of
    FILLED operands, and that meaning belongs to the filled count over its set source, never to this kind`; opende de
    unieroute en weigerde `bouwInstantieUnie` haar, dan staat die reden in de context: `opgesomde telling '<zin>', geen
    unie van selecties: <reden>: a count folds …`. Verifier: `WF-037 FAIL …/value: a count folds INSTANCES …` (2 en 3
    operanden; de elementspelling geeft daarnaast 2× WF-034).
13. **WF-037, telling zonder bron**, `ExpressieBouwer.java:1797`. Oud: `telling zonder bron: '<zin>' noemt geen instanties om
    te tellen — … (WF-037, IR-SPEC ch. 4.3, besluit 2026-09-23)`, met de varianten `telling zonder bron, over een
    objecttype: … Tel via een rol vanaf het onderwerp …` en `telling zonder bron, of een attribuut: … schrijf dan
    'aantal …'`. Nieuw: `telling zonder bron '<zin>': the count is an INSTANCE fold and MUST carry an element type:
    without one its argument is no fold source at all — and with no argument there is none — so the count has no
    contributors to count, only its operand, whatever that operand holds (ch. 4.3)`, met context `telling zonder bron
    '<zin>', over het objecttype Vondst: …` en `telling zonder bron '<zin>', waar 'aantal gewogen posten' een
    gedeclareerd attribuut van Jutter is: …`. Verifier: alle tien proeven `WF-037 FAIL …/value: the count is an INSTANCE
    fold …`.
14. **WF-037, gevuld aantal**, `ExpressieBouwer.java:647`. Oud: `het aantal gevulde waarden van vereist een setlezing als
    bron: … — '<zin>' noemt er geen, en het gevulde aantal van één eigen waarde is 0 of 1 (WF-037, IR-SPEC ch. 4.3)`.
    Nieuw: `gevulde telling '<zin>': the filled count is an INSTANCE fold and MUST carry an element type: without one its
    argument is no fold source at all, and the count has no contributors whose values it could test for presence (ch.
    4.3)`. Verifier: die zin op `…/value` (gepatchte emissie bij een onopgeloste operand; handgebouwd waar de bron een
    gewone attribuutlezing is, want daar gooit de emitter).

De zes plekken van (2):
- **Setlezing buiten een vouwbron** (`RuleChecker.java:2010`): arm, omgezet. Nieuw: `setlezing buiten een vouwbron
  '<lezing>': hop 0 reaches the to-many role 'vondst' in a scalar slot — a to-many step needs an aggregation, never a
  silent first pick` (één zin per meervoudige stap, met `; `), en als bron van een telling: `setlezing buiten een
  vouwbron '<lezing>': the source of this count aggregation reads 'gewicht', a member of the element type 'Vondst': a
  count folds INSTANCES, so its source is a count source naming the relation's role, never a value read per contributor`.
  Verifier: WF-053 op `…/value` en `…/value/left`, WF-093 op `…/value/arguments/0`; een setlezing als argument van een
  bronloze telling geeft beide frontendzinnen en beide verifierregels. De stap en de rol zijn die van het pad dat de
  emitter zou schrijven (`IrEmitter.navigatieBindingen`, nu ook statisch aanroepbaar), vanaf het onderwerp waar de emitter
  vanaf schrijft (daarvoor draagt de walk nu het onderwerp mee). **Blijft Nederlands**: geen afleidbaar pad (de emitter
  gooit; model A van `GenesteVouwbronTest`), geen meervoudige stap, of een telbron die geen lid van het elementtype noemt.
- **Beslistabelrijen met één regelnaam** (`RuleChecker.java:2167`): arm WF-020, omgezet. Oud: `beslistabel 'Premietabel':
  rijen krijgen dezelfde regelnaam ('Premietabel 1') — … (WF-020, WF-021). Geef elke rij een eigen nummer`. Nieuw:
  `beslistabel 'Premietabel': duplicate name 'Premietabel 1' in rules` (per naam, met `; `, gebouwd met het
  Printf-sjabloon van de verifier). Verifier: `WF-020 FAIL /rules/rules/1: duplicate name 'Premietabel 1' in rules`
  (geen WF-021, ook niet met een begrensde tabelversie).
- **"ongedefinieerd …"** (`RuleChecker.java:2363`): geen arm → DIVERGENCES. De emitter gooit (`een onbekend datatype
  ('domein Bedrag') heeft op deze positie geen spelling in uchar-ir-v1`); de wire schrijft het datatype van een domein
  waar het gebruikt wordt, dus een ongedeclareerd domein heeft geen wirevorm (handgebouwd zonder het domein: PASS).
- **Leden van een ongeordende groep** (`RuleChecker.java:1119–1136`): geen arm → DIVERGENCES. Een lid met een ander doel:
  met de weigering uit schrijft de verlaging beide leden op het doel van het eerste lid, en de verifier aanvaardt dat
  (PASS); een lid dat geen kenmerktoekenning of gelijkstelling is, of geen regel oplevert: geen document (dialecthek).
- **Uitzonderingsblok in een regelgroep** (`RuleChecker.java:490`): lidmaatschapsarm WF-033, omgezet; plaatsing alleen:
  verifier PASS → Nederlands, DIVERGENCES. Nieuw: `Uitzondering op 'bergingsbevoegd bij quotum' (zijn basisregel is lid
  van regelgroep 'berging'): group member 'bergingsbevoegd bij quotum' does not resolve to a rule form`; gelden beide
  armen: `… (zijn basisregel is lid van regelgroep 'berging', en het blok staat direct na het laatste lid van regelgroep
  'berging'): group member …`. Verifier: `WF-033 FAIL /rules/ruleGroups/0: group member 'bergingsbevoegd bij quotum' does
  not resolve to a rule form`. Nog steeds één melding per blok.
- **Startpunt: onderwerp, uitdrukkingstype, ankernaam** (`RuleChecker.java:3201/3207/3218`): alle drie een arm, omgezet.
  `startpuntbepaling (regel 'startpunt penningjaar'): object type 'Strandjutter' is not declared` (WF-032, handgebouwd;
  de verifier meldt daarnaast WF-022 en WF-034 op de synthetische regel); `startpuntbepaling (regel '…'): toekenning: a
  number value is assigned to a date-day target` en `… a date-ms value …` (WF-023 op de regel die het anker schrijft; de
  zin komt uit `Typering.toekenning`); `startpuntbepaling (regel '…'): anchor member 'startpunt penningjaar' is number,
  must be a day-precision date` (WF-032; de verifier meldt daarnaast WF-023). Een anker met een ongedeclareerd domein
  houdt de eigen zin (geen soortwoord).

#### Tests onder mandaat (per test: welke zin haar dwong; ze pint nu de nieuwe zin)
Rood-eerst gemeten: de testbronnen van de kop tegen de hoofdcode van `147eabd3` geven 95 fouten, precies de 71
aangepaste testinstanties hieronder plus 24 van de 27 getuigen; vóór de aanpassing waren dezelfde 71 instanties rood
tegen de nieuwe code. De drie getuigen die tegen de basis groen zijn pinnen de Nederlandse zinnen die blijven.
- `NietInUitzonderingTest` (WF-022): `een_ontkenning_in_een_vlakke_regel_weigert_gericht` (4), `…naast_een_defaults_groep_
  spreekt_alleen`, `een_ontkende_tabelrij_…` pinden `§9.2`, `dialect §4` en `staat alleen in een regel die een default
  wordt`; nu de volledige WF-022-zin (assertEquals of contains). WF-050: `…_krijgt_alleen_wf050` pinde `WF-050`; nu
  `a rule also writes defaults target '`.
- `EnigeSchrijverTest` (WF-050, 5): de helper pinde `WF-050`, nu een van beide WF-050-zinnen; `twee_geordende_families`
  pinde `ongeordend`, nu `without the declared-unordered marking`.
- `RegelgroepUitzonderingTest` (WF-033, 4): pinden `'…' is lid van regelgroep 'berging'`, `WF-033`, `§9.10`/`§4` en de
  uitweg `vóór de eerste regelgroepkop …`; nu `zijn basisregel is lid van regelgroep '…'` en `group member '…' does not
  resolve to a rule form`, bij twee groepen de volledige context (de rest van die test, de gevolgde uitweg geeft een
  schoon document, blijft). De uitweg staat alleen nog in de plaatsingsmelding.
- `BeslistabelRijnaamTest` (WF-020, 2): pinden `WF-020`; nu `duplicate name '…' in rules`.
- `SetlezingZonderVouwTest` (5, via `MELDING`), `AantalSetbronTest` (1, `MELDING`), `GenesteVouwbronTest` (1): pinden de
  Nederlandse setlezingzin met `(WF-053, IR-SPEC ch. 4.1a)` of `(WF-093 (b), …)`; nu de WF-053- en WF-093-zin.
- `VensterKwantorTest` (5), `UitzonderingOpUitzonderingTest` (1): pinden `WF-112`; nu `a time window under none or a
  counting quantifier`.
- `PeriodegrensTest` (10 gevallen, 8 rood): pinde `WF-111` en `§8.4.3` met Nederlandse fragmenten; nu de verifierfragmenten,
  en voor de twee gevallen zonder relatie (sub- en supertype) blijven de Nederlandse fragmenten gepind.
- `IrEmitterTest` (WF-009, 1): twee volledige Nederlandse meldingen → de nieuwe.
- `AantalOpgesomdTest` (5), `AantalUnieTest` (1): pinden `WF-037`, `noemt 2|3 operanden`, `geen opgesomde vorm` en de
  staart `niet elke operand is een rolselectie`; nu `has no LISTED form`, `this node carries 2|3 operands`, de rest van
  de verifierzin, en dat bij waarde-operanden geen unie-reden in de context staat.
- `AantalZonderBronTest` (9), `AantalAttribuutnaamTest` (4), `TypemeervoudTellingTest` (10): pinden `WF-037`, de normzin
  met §-verwijzingen, de uitweg `vanaf Jutter is dat de rol 'vondsten' …`, `schrijf dan '…'` en het voorvoegsel `telling
  zonder bron: `; nu de WF-037-zin met de context (`, over het objecttype Vondst`, `, waar 'aantal gewogen posten' …`,
  `telling zonder bron '<zin>': `).
- `AantalGevuldeDialectTest` (2): `NIET_SET` (volledige Nederlandse zin) en het filter op `WF-037`; nu de zin van het
  gevulde aantal.

Buiten het mandaat voor Nederlandse zinnen, maar nodig voor de guard, en daarna door de hoofdsessie gemandateerd
(bericht na `9d907639`: de guard-uitbreiding, tien woorden en vloer 149, heeft haar mandaat):
`resolver/LezerszinGrensTest.java`. Tien van mijn
Engelse fragmenten droegen geen woord uit `ENGELS` en ontsnapten; de lijst kreeg `priority complemented element slot
aggregation duplicate rules validity anchor navigated` (geen van die woorden staat in een Nederlandse meerwoordsliteral
van resolver of emitter, gemeten), de ondergrens ging van 95 naar 149 (gemeten 153, dezelfde marge van 4). Mutatietoets
in een spiegel onder `/tmp`: herformuleert de verifier `duplicate name`, `anchor member` of `a navigated read of`, dan
wordt de guard rood op precies die drie fragmenten; vóór de uitbreiding ontsnapten ze. Een afwijking aan de
frontendkant in de WF-009-zin (`one unit per abbreviation`) maakt hem ook rood; een afwijking met een woord buiten de
lijst (`duplicated name`) ontsnapt, de bekende rand (restpunt 5).

#### Inertheid (frontend `147eabd3` tegen `9d907639`, basis schoon uit `git archive`; (buiten de repo))
- Corpus: 89 `bron.rgs`; document, rc en uitvoer basis == kop 89/89; kop == vastgelegde `ir.json` 89/89.
- toka (alleen tellingen): casusdump uit 45 fragmenten: rc 0 beide, basis == kop == pin, uitvoer gelijk; volledige casus
  (47 bronnen): rc 1 beide, document gelijk, uitvoer gelijk (7 regels; 3 ERROR en 4 WARNING beide); elke bron los:
  document 47/47, rc 47/47, uitvoer 43/47 gelijk. De vier verschillen zijn ERROR-teksten van omgezette weigeringen die
  alleen vuren als een fragment buiten de casus staat (2× de bronloze telling, 2× de ongedeclareerde tijdlijn). Geen
  toka-test pint een van de veranderde zinnen (gegrept, alleen tellingen). Slices: niet opnieuw gemeten; geen van de
  betrokken bestanden veranderde (`toka/`, `spec/`, `ref/`, `backends/` ongewijzigd).

#### Gate-regels (elk los na `eval $(opam env)`, op de kop `9d907639`)
- `make test`: `755 tests found`, `755 tests successful`, `0 tests failed`
- 1: `check-self-contained: PASS`
- 10: `check-regelspraak: PASS — JUnit suite green (755 tests, floor 173); 89 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: niet gedraaid, zoals de opdracht toelaat: geen WARNING-tekst veranderd; de casusdump her-emitteert byte-gelijk aan
  de pin (gemeten) en de volledige casus geeft dezelfde uitvoer.

#### Wat aan de opdracht niet klopte (gemeten)
1. `checks.ml:2006` is een WF-034-arm van de verdeling; de WF-027-zin staat op `checks.ml:2174`.
2. "De lijst wordt korter": de 14 stonden niet in de DIVERGENCES-lijst (ze waren er nooit geregistreerd); de lijst werd
   langer, met de gemeten deelgevallen zonder lezerszin (feitcreatie als drager, dagsoortdefinitie, navigerende grens
   zonder pad, setlezing zonder pad of meervoudige stap, ongedeclareerd domein, blok alleen na een groep, lid met een
   ander doel).
3. De WF-021-zin heeft twee variabele delen die de verifier uit de wire haalt: het versienummer volgens de lader en de
   rij volgens de soort; de frontend rekent beide nu uit.
4. Van de zes niet-geplaatste plekken hebben er drie een arm (setlezing, rijnamen, startpunt), één voor de helft
   (blok in een regelgroep: lidmaatschap wel, plaatsing niet) en twee geen (domein, ongeordende groep).

#### Definitieve weigerzinnen (letterlijk; `<…>` is variabel)
- `regel '<r>'<tabelrij>: characteristic target's value must be the boolean constant true`
- `<kop> (defaults-groep|defaults-groepen <namen>, doel ook geschreven door regel|de regels <regels>): a rule also writes defaults target '<T.a>' — an unordered plain writer would silently defeat the group's decisions`
- `<kop> (defaults-groepen <namen>): multiple defaults groups on target '<T.a>' without the declared-unordered marking — priority is explicit (defaults) or explicitly absent ('unordered'), never implicit (ch. 7.2)`
- `recursieve regelgroep '<g>': recursive group without a creation member`
- `<kop> (zijn basisregel is lid|een rij van beslistabel '<t>', lid van regelgroep '<g>'[, en het blok staat direct na het laatste lid van regelgroep '<g2>']): group member '<b>' does not resolve to a rule form`
- `regel '<r>': validity windows of versions '<r> #<i>' and '<r> #<j>' overlap (<rij>)`
- `beslistabel '<t>': duplicate name '<n>' in rules`
- `startpuntbepaling (regel '<r>'): start point for undeclared timeline '<t>'`
- `startpuntbepaling (regel '<r>'): object type '<T>' is not declared`
- `startpuntbepaling (regel '<r>'): toekenning: a <kind> value is assigned to a date-day target`
- `startpuntbepaling (regel '<r>'): anchor member '<lid>' is <kind>, must be a day-precision date`
- `setlezing buiten een vouwbron '<lezing>': hop <i> reaches the to-many role '<rol>' in a scalar slot — a to-many step needs an aggregation, never a silent first pick`
- `setlezing buiten een vouwbron '<lezing>': the source of this count aggregation reads '<a>', a member of the element type '<T>': a count folds INSTANCES, so its source is a count source naming the relation's role, never a value read per contributor`
- `periodevergelijking onder '<kwantor>' (<plaats>): a time window under none or a counting quantifier: the window gates the RULE, and no reading is specified for a verdict that is counted or complemented`
- `periodevergelijking (de vanaf|tot-grens '<grens>'): the from|to bound of a time window is <wat> — a bound is a date literal or an own scalar date attribute of the subject; the general date expression is an OPEN wire decision`
- `periodevergelijking (de vanaf|tot-grens '<grens>'): the from|to bound of a time window is a millisecond-precision date: the window is a range of civil days and no clause reads an instant bound`
- `objectcreatie: '<m>' is not a declared attribute of '<T>'`
- `eenheidstelsel '<s>', [als '<eerder>' ]al gedeclareerd voor de eenheid '<e>': the unit '<a>' is declared twice in the unit system '<s>': one Unit per abbreviation (WF-009)`
- `dagsoort '<d>': day-kind '<d>' shadows a built-in calendar unit`
- slotronde: `dagsoortdefinitie '<d>' (regel '<r>'): day-kind definition with a bounded validity window; must be always (from and to both absent)`
- slotronde: `dagsoortdefinitie '<d>' (regel '<r>'): more than one definition for day kind '<eerste d>'`
- `opgesomde telling '<zin>'[, geen unie van selecties: <reden>]: a count folds INSTANCES and has no LISTED form, and this node carries <n> operands: the source language spells a count over a subject expression alone (ch. 4.3), so there is no articulated operand list to admit — the only reading an operand list leaves a count is the number of FILLED operands, and that meaning belongs to the filled count over its set source, never to this kind`
- `telling zonder bron['<zin>'][, over het objecttype <T>|, waar '<naam>' een gedeclareerd attribuut van <T> is]: the count is an INSTANCE fold and MUST carry an element type: without one its argument is no fold source at all — and with no argument there is none — so the count has no contributors to count, only its operand, whatever that operand holds (ch. 4.3)`
- `gevulde telling '<zin>': the filled count is an INSTANCE fold and MUST carry an element type: without one its argument is no fold source at all, and the count has no contributors whose values it could test for presence (ch. 4.3)`
- `<kind>` is het soortwoord van de verifier (`Typering.soortWoord`); `<rij>` de wirenaam van de rij (`rules`, `objectCreations`, …). Geen CONSTRUCTS-registerzin gevraagd.

#### Slotronde (kop `6aff9ba0`)
- Opdracht van de hoofdsessie na `9d907639`: restpunt 1 (dagsoortdefinitie) en restpunt 6 (javadoc van
  `PeriodegrensTest`) afmaken; restpunt 2 (schrijfaanwijzingen naast de verifierzin) blijft staan als patroonbesluit
  voor de maintainer. Twee commits, werkboom schoon, niets gepusht of gemerged:
  - `7a5b1c87` resolver: begrensde en dubbele dagsoortdefinities in WF-031
  - `6aff9ba0` test: de javadoc van PeriodegrensTest volgt de gemeten emitter
- **Dagsoortdefinitie** (`RuleChecker.resultaat`, tak `dagsoortResultaat`, r. 2472–2488 op `9d907639`; buiten de regio's
  van de parallelle fixer). Vóór: een tweede definitie van één dagsoort gaf alleen de WARNING `dagsoort 'springtijdag'
  heeft meerdere definities; alleen de eerste wordt gebruikt (versievensters worden nog niet toegepast)` en het
  document ging de schijf op; een begrensd venster gaf geen enkele melding. Na, beide ERROR (de WARNING is weg):
  `dagsoortdefinitie 'springtijdag' (regel 'Dagsoortdefinitie springtijdag'): day-kind definition with a bounded
  validity window; must be always (from and to both absent)` (per versie met een grens) en `dagsoortdefinitie
  'springtijdag' (regel '<regel>'): more than one definition for day kind 'springtijdag'` (bij de tweede definitie; de
  naam in de zin is die van de eerste, zoals de verifier hem noemt). De overlapweigering van versies blijft daarnaast
  staan (§4.2; WF-021 vergelijkt dagsoortdefinities niet).
- Meting frontend-ERROR ⇔ verifier-FAIL, op het document dat de frontend van `147eabd3` schrijft (die weigert niet):
  twee begrensde versies (`dag_twee`): 3 ERROR ⇔ 3 FAIL (`WF-031 FAIL /rules/dayKindDefinitions/1: more than one
  definition for day kind 'springtijdag'`, `WF-031 FAIL /rules/dayKindDefinitions/0: day-kind definition with a bounded
  validity window; must be always (from and to both absent)`, idem `/1`); één begrensde versie (`dag_venster`): 1 ⇔ 1;
  twee regels die één dagsoort definiëren (`dag_tweeregels`): 1 ⇔ 1. Twee overlappende versies die altijd gelden
  (`wf021dag`, document met de overlapweigering uit): de Engelse ERROR ⇔ de enige FAIL (`more than one definition …`);
  de Nederlandse overlapweigering heeft geen lezerszin en staat in de DIVERGENCES-lijst.
- Getuigen in `StructuurZinTest`, rood-eerst tegen `9d907639`: `een_dagsoortdefinitie_met_twee_begrensde_versies`,
  `een_dagsoortdefinitie_met_een_begrensd_venster`, `twee_regels_die_een_dagsoort_definieren` rood (de WARNING of
  niets), en de bestaande `overlappende_versies_van_een_dagsoortdefinitie_houden_de_eigen_zin` (deze ronde geschreven)
  rood omdat ze nu ook de WF-031-ERROR pint; `een_dagsoortdefinitie_die_altijd_geldt_blijft_schoon` groen (pint dat de
  gewone definitie schoon blijft). Geen bestaande test pinde de WARNING (gegrept). JUnit 755 → 759.
- **Javadoc `PeriodegrensTest`**: de zin "goes out as a navigation (by-pair bindings) or as an off-subject read without
  one, and the verifier refuses both" is vervangen door het gemeten gedrag: navigatie bij een relatie (WF-111), en
  zonder relatie weigert de emitter het model, dus geen document en de Nederlandse weigering (de sub- en
  supertyperijen van de test). De gedateerde meting van vóór het hek (1c71ecf3) in de eerste alinea is ongewijzigd.
- DIVERGENCES, lijst "Producer refusals without a reader sentence": de opsomming van de structurele weigeringen noemt
  nu ook de dagsoortdefinitie, en de vermelding van de overlappende dagsoortversies zegt dat de tweede definitie en het
  begrensde venster WF-031's zijn, geweigerd in die zinnen naast de overlap. De alinea is opnieuw op 80 kolommen gezet
  (de woordvolgorde gecontroleerd gelijk op de wijzigingen na).
- Inertheid (`147eabd3` tegen de kop): corpus 89 `bron.rgs`, document, rc en uitvoer 89/89 gelijk, kop == vastgelegde
  `ir.json` 89/89; toka (alleen tellingen): casusdump uit 45 fragmenten basis == kop == pin, uitvoer gelijk; volledige
  casus (47 bronnen) rc 1 beide, document gelijk, uitvoer gelijk (7 regels, 3 ERROR, 4 WARNING); elke bron los document
  47/47, rc 47/47, uitvoer 43/47 (dezelfde vier ERROR-teksten als hierboven). Geen toka-uitvoer droeg de WARNING die
  een ERROR werd. Slices: geen van de betrokken bestanden veranderde.
- Gate-regels (elk los na `eval $(opam env)`, op `6aff9ba0`): `make test`: `759 tests found`, `759 tests successful`,
  `0 tests failed`; 1: `check-self-contained: PASS`; 10: `check-regelspraak: PASS — JUnit suite green (759 tests, floor
  173); 89 rgs golden(s) re-emit byte-identically (floor 80)`. Gate 11 niet gevraagd en niet gedraaid; de enige
  WARNING-tekst die verdween komt in geen toka-uitvoer voor (gemeten hierboven).
- Restpunten na de slotronde (vervangen de lijst hieronder): 1 en 6 zijn gesloten. Open blijven: (a) restpunt 2, de
  schrijfaanwijzingen naast de verifierzin, een patroonbesluit voor de maintainer; (b) restpunt 3, het WF-053-pad bij
  een geschreven meerstapsbereik; (c) restpunt 4, per paar of per groep tegen één melding; (d) restpunt 5, de randen
  van de guard; (e) restpunt 7, de datumliteral die geen kalenderdatum is. Nieuw, uit deze slotronde: (f) een
  dagsoortdefinitie zonder voorwaarde valt met een WARNING weg en het document gaat zonder haar de schijf op; de
  verifier kent daarvoor `day-kind definition without a condition`, maar het geschreven document draagt de definitie
  niet, dus niet gemeten als gat.

#### Restpunten
1. Gevonden, niet in de opdracht, gemeten: een dagsoortdefinitie met twee versies geeft alleen de WARNING `dagsoort
   'springtijdag' heeft meerdere definities; …` en schrijft het document, dat de verifier drie keer weigert (WF-031:
   `more than one definition for day kind 'springtijdag'` en twee keer `day-kind definition with a bounded validity
   window; must be always (from and to both absent)`); één versie met een begrensd venster schrijft de frontend zonder
   enige melding, en de verifier weigert haar met die tweede zin. Voorstel: een ERROR in de WF-031-zinnen; dat maakt van
   de WARNING een ERROR, dus eerst meten of de pariteitszone een dagsoortdefinitie met een venster draagt.
2. Met de Nederlandse zinnen vielen de schrijfaanwijzingen weg (patroon van ronde 4: de melding ís voorvoegsel plus
   verifierzin): waar een ontkenning mag staan (WF-022), "neem de regel op in de groep" en "zet de uitzonderingen onder
   één basisregel" (WF-050), "zet het venster buiten die kwantor" (WF-112), de uitweg bij het blok op een groepslid (staat
   nog in de plaatsingsmelding), de rol als uitweg bij een telling over een objecttype en "schrijf dan 'aantal …'" bij de
   attribuutbotsing (WF-037), en de staart over de §5.7-route bij waarde-operanden. Wil de maintainer een aanwijzing naast
   de verifierzin, dan vraagt het patroon een besluit (een staart na de zin weerspreekt de DIVERGENCES-tekst).
3. Voor een setlezing met een geschreven meerstapsbereik noemt de WF-053-zin het pad dat de emitter zou schrijven: waar
   een directe relatie bestaat is dat die relatie (model B van `GenesteVouwbronTest`: `'aangemelde vondst'`), niet de
   geschreven keten. Letterlijk gelijk aan de verifier op dat document, maar niet wat de auteur schreef; de scalaire
   padafleiding van de emitter leest `bereikPad` niet. De setlezing blijft geweigerd.
4. WF-021 en WF-050 (meer groepen): de verifier meldt per paar of per groep, de frontend één keer per regel of doel,
   met dezelfde zin.
5. De guard ziet een Engels fragment van één woord niet (`day-kind '` in de WF-017-zin) en een fragment zonder woord uit
   zijn lijst; beide staan in zijn javadoc.
6. De klasse-javadoc van `PeriodegrensTest` zegt nog dat een lezing zonder relatie "as an off-subject read" uitgaat en
   onder WF-053 geweigerd wordt; gemeten gooit de emitter dan. Niet aangepast (het mandaat gold de beweringen);
   DIVERGENCES §8.4.3 is wel gecorrigeerd. Idem het commentaar "the advice must be true" in
   `AantalAttribuutnaamTest#het_aangeraden_lidwoordloze_spelling_leest_het_attribuut`: de eigenschap klopt, de aanwijzing
   staat niet meer in de melding.
7. Uit ronde 4/7 nog open: de datum(-tijd)literal die geen kalenderdatum is (Nederlands, in de DIVERGENCES-lijst).

---

### Ronde 7 (lensfix) — branch `fix/lens-emitter`, kop **`147eabd3`** (na de slotronde; daarvóór `be7175f7`)

#### Stand
- Nieuwe branch `fix/lens-emitter` vanaf de unie `card/k7-unie` (`36750511`), in (worktree emitter).
  Acht commits, werkboom schoon, niets gepusht of gemerged, geen beschermd pad geraakt. Uit de regio's van de
  parallelle fixer gebleven (`RuleChecker` r. 101/1506/1692, DIVERGENCES §7, de kringgraaf): mijn wijzigingen in
  `RuleChecker` zitten in `keurTeken` en `keurGeheelPeriode` en die in DIVERGENCES achteraan.
- Commits:
  - `52a37c9b` resolver: de datumrekenzinnen volgen de verifier van de unie — B1
  - `d4cc323e` resolver: de nieuwe operandarmen van de verifier in zijn zinnen — B2
  - `6a63f2ab` resolver: tijdseenheden die de wire niet draagt worden geweigerd — A4
  - `f815ee2e` IrEmitter: een negatieve bronregel in de zin van de lezers — A7
  - `5492db3e` DIVERGENCES: de zelfgetypeerde creatie zonder lezerszin — A6
  - `0cb9b4ca` DIVERGENCES: de eenzijdige clamp-eenheid — A5
  - `f663cadb` golden jutterswet-strandmelding: de dubbele slotalinea weg — A3
  - `be7175f7` test: de Engelse zinnen van de frontend staan in de verifier — de guard bij B1
  Drie onderwerpen heb ik vóór het afronden herschreven (te lang, of een opsomming): de commits opnieuw toegepast,
  de boom byte-gelijk aan de vorige kop (gemeten, 0 regels verschil).
- JUnit 721 (unie) → 728: `TyperingZinTest` +2 (`tekenbeperking_van_het_doel`, `subdag_eenheid_op_een_dagdatum`;
  de bestaande datumrekentest kreeg daarnaast een bewering), `VouwOperandZinTest` +2 (`wortel_en_absolute_waarde`,
  `de_eenheid_van_een_tijdsduur`), `LezerszinGrensTest` +3 (nieuw, `…/resolver/`).
- Metingen met de unieverifier (`dune build`, bron `36750511`), proeven in (buiten de repo).

#### Per vondst: vóór en na, de zinnen letterlijk
- **B1.** Vóór: frontend `plus: 'EUR' is not a recognised time unit or declared day kind` en `duration + date is
  ill-formed (the date stands on the left; v0 does not commute)`; unieverifier `WF-035 … date ± duration: the duration
  unit 'EUR' is neither a recognised time unit nor a declared day kind` en `… (the date stands on the left; date
  arithmetic does not commute)`. Na (`Typering`, één hulpzin voor r. 65 en 484): `plus: date ± duration: the duration
  unit 'EUR' is neither a recognised time unit nor a declared day kind`, idem met `'%'` voor een percentage, en `plus:
  duration + date is ill-formed (the date stands on the left; date arithmetic does not commute)`; letterlijk gelijk aan
  de verifier op de gemuteerde proeven `a-datum-plus-eur`, `a-datum-plus-pct`, `a-getal-plus-datum`. Bestaande
  beweringen met mandaat: `TyperingZinTest` r. 174 (de EUR-zin) en `TyperingTest` r. 41 (`contains("is neither a
  recognised time unit nor a declared day kind")`). De zin voor duur plus datum kreeg een getuige in
  `TyperingZinTest` (geen test pinde haar).
- **B1, de guard.** `LezerszinGrensTest` leest `resolver/*.java` en `emit/IrEmitter.java` en alle literals van
  `spec/verifier/lib/*.ml` (escapes ontsleuteld, gesplitst op Printf-specifiers). Elk Engels fragment (voorvoegsels
  `IrEmitter: `, een WF-armtag, een pointer, Nederlandse contextsegmenten en een slot-` — ` eraf; Engels = minstens
  twee woorden, een woord uit de Engelse lijst en geen uit de Nederlandse) moet letterlijk in een verifierstuk staan.
  Gemeten: 99 fragmenten gelezen, ondergrens 95; één uitzondering met reden (de `IfThenElse`-weigering van de emitter,
  twee regels, een invariant zonder lezer); `ir/Spelling` bewust buiten de scan (zijn Engels is de interne controle van
  de tabellen). Zelftoets: de twee oude zinnen staan niet in de verifier, de twee nieuwe wel; en een derde test eist dat
  de bronnen geen Java-tekstblok en geen OCaml-quoted string dragen, die de scanner niet leest. Mutatietoets: met de
  oude zin terug in een kopie van `Typering.java` wordt de guard rood met `Typering.java:489 'duration + date is
  ill-formed (the date stands on the left; v0 does not commute)'`. Twee zinnen die de verifier rond een variabele bouwt,
  schrijft de frontend nu met zijn sjabloon: de clamp (`"clamp " + positie + " is " + … + ", must be numeric"`, één hulp
  voor basis, minimum en maximum) en het keuzetypedomein (`String.format("a value of the %s '%s' is assigned to a target
  of the %s '%s'", "enumeration domain", …)`); de zinnen zelf zijn ongewijzigd.
- **B2.** Vóór: `de wortel van vereist numerieke operanden, kreeg Tekst` (idem absolute waarde) en `waarde -5 voldoet niet
  aan de teken-beperking 'niet-negatief' van het doel-datatype (§3.3.1)`; unieverifier `WF-038 … squareRoot operand is
  text, must be a number`, `absoluteValue operand is text, must be a number`, `WF-039 …/value/value: negative literal
  '-5' is written into a target under a non-negative sign restriction`. Na: `de wortel van: squareRoot operand is
  <kind>, must be a number`, `de absolute waarde van: absoluteValue operand is <kind>, must be a number` (percentage houdt
  `… vereist numerieke operanden, kreeg Percentage`); `teken-beperking: negative literal '<w>' is written into a target
  under a non-negative sign restriction`, `teken-beperking: literal '<w>' is written into a target under a positive sign
  restriction`, `teken-beperking: literal '<w>' is written into a target under a negative sign restriction`, met `<w>` het
  lexeem dat de emitter schrijft (komma → punt). Het teken leest de frontend nu zoals de verifier van het lexeem af
  (voorloopminteken = negatief, alleen nullen = nul): gemeten `-5`, `-0` (niet-negatief), `0` (positief), `5` (negatief)
  letterlijk gelijk; `-0` weigerde de frontend eerst niet, de verifier wel. Het opzijstaan van de verifier waar het eigen
  teken van de literal al weigert is in de frontend onbereikbaar (elke getalliteral krijgt het teken `ONBEPAALD`), dus
  steeds één melding; dat staat in de javadoc van `keurTeken`. De ongebruikte `tekenNaam` is weg. DIVERGENCES: de twee
  zinsdelen uit de lijst, het percentage bij wortel en absolute waarde erin.
- **A4.** Drie vormen, alle drie vóór: geen melding, document geschreven; na: ERROR, geen document, letterlijk gelijk aan
  de verifier op het oude document:
  `plus: sub-day time unit on a day-precision date (millisecond precision required)` (en `min:`; WF-035; `zijn vondstdag
  plus 3 uur`, `… min 90 minuten`);
  `de (absolute) tijdsduur van … tot …: duration: the counted unit '<abbr>' is neither a recognised time unit nor a
  declared day kind` (WF-038; gemeten met `euros` → `'EUR'` en de ongedeclareerde `kopjes` → `'kopjes'`, ook bij de
  absolute tijdsduur);
  `de (absolute) tijdsduur van … tot …: sub-day duration unit on day-precision operands (millisecond precision
  required)` (WF-038; `in hele uren` tussen twee dagdatums). Deze derde vorm noemde de lens niet: ik vond haar bij het
  lezen van dezelfde verifierarm en heb haar meegenomen. Controles blijven schoon: dagdatum plus 3 dagen, een
  millisecondedatum plus 3 uur, een tijdsduur in dagen, een tijdsduur in uren tussen millisecondedatums.
- **A7.** Vóór: `IrEmitter: de bronregel -1 van 'weging' is negatief; een regel in het bronrecord is minstens 1, en 0
  betekent geen regel`. Na: `IrEmitter: a source line is at least 1` (de zin van `loader.ml` r. 403 en
  `IrLoader.BRONREGEL_ONDER_1`).
- **A6.** De weigering van de zelfgetypeerde creatie die geen rol noemt staat nu in de lijst "Producer refusals without
  a reader sentence" (de verifier aanvaardt het document zonder koppeling; gemeten in ronde 2).
- **A5.** Nieuwe DIVERGENCES-sectie "§6.1.4 A one-sided clamp unit — written with a warning": één alinea met de reden
  (de casus van de pariteitszone draagt één zo'n clamp), de WF-099-zin, wat een ERROR wordt, en wanneer de
  waarschuwing een ERROR kan worden.
- **A3.** De laatste, ongewrapte regel van `corpus/golden/jutterswet-strandmelding/NOTE.md` (een herhaling van de sectie
  over de her-emissie met de genoemde rol) is weg; die sectie is nu de enige alinea hierover.

#### B5 (gemeten, niets gewijzigd buiten wat hieronder staat)
Per bewering: de huidige testbron tegen de frontend van vóór de wijziging en tegen de kop.
- `TyperingTest` r. 41 (`b0e3ada9`, nu B1): was `contains("tijdseenheid")`, werd de Engelse WF-035-zin toen de frontend
  die zin kreeg. Tegen de frontend van `b096bdc2`: rood (`datum plus vereist een tijdseenheid, kreeg 'km'`); tegen de
  kop: groen. `assertFalse(r.ok())` gaat eraan vooraf: de weigering blijft gevergd.
- `TyperingTest` r. 56 (`3ec171c4`): was `contains("tijd-precisie")`, werd `equality across date precisions` toen de
  datumprecisie de WF-036-zin kreeg. `b096bdc2`: rood (`gelijk/ongelijk vereist dezelfde tijd-precisie, …`); kop:
  groen; `assertFalse(r.ok())` blijft.
- `VariabeledeelLoweringTest` r. 363 (`9110543b`): was `ERROR: 'paars' is geen waarde van Enumeratie Kleur`, werd
  `ERROR: enumeration label 'paars' is not in the declared domain` toen het label de WF-039-zin kreeg. `b096bdc2`: rood
  (`[ERROR: 'paars' is geen waarde van Enumeratie Kleur [rood, groen] (§13.3.4.2)]`); kop: groen; het fragment begint met
  `ERROR: `, dus de weigering blijft gevergd, naast de gelijkheid met de kale literal.
- `toka/…/CasusTest` r. 55–58 (`b60264f9`, `b0e3ada9`): de twee herkenningspredicaten van de bekende quirks volgden de
  eenheidszin en de datumrekenzin. De test eist nog steeds beide quirks (`anyMatch`) en geen enkele andere ERROR
  (filter); op de kop matchen de drie ERROR-regels van de volledige casus precies de twee predicaten (gemeten in ronde 4
  en nu: 3 ERROR-regels, geen andere). Op de frontend van vóór die commits droeg de datumquirk een Nederlandse zin die het
  huidige predicaat niet herkent. Niets in `toka/` gewijzigd.
- Deze ronde buiten de mandaatlijst, afgedwongen door de gemandateerde wijziging (zelfde meting, tegen de unie en de
  kop): `VariabeledeelLoweringTest` r. 331–332 (B2): het fragment werd `ERROR: teken-beperking: negative literal '-49' is
  written into a target under a non-negative sign restriction`, en de commentaarzin "the emitted wire is
  verifier-clean" werd onwaar (de unieverifier weigert het document) en is vervangen; unie rood, kop groen.
  `IrEmitterTest` r. ~1505 (A7): `contains("de bronregel -1 van 'weging' is negatief")` werd `assertEquals("IrEmitter: a
  source line is at least 1", …)` met een commentaarregel; unie rood, kop groen. In `VouwOperandZinTest` (een test van
  deze kaart) verhuisde de wortel over tekst van "houdt de eigen zin" naar de WF-038-zin; het percentage bleef daar.

#### Inertheid (frontend `36750511` tegen `be7175f7`, beide schoon uit `git archive`)
- Corpus: 89 `bron.rgs`, document en rc 89/89 gelijk, uitvoer 89/89 gelijk; geen getuige eiste een verschil.
- toka (alleen tellingen): casusdump uit 45 fragmenten: unie == kop == pin, uitvoer gelijk; volledige casus (47
  bronnen): uitvoer gelijk (7 regels); elke bron los: document en rc 47/47 gelijk, uitvoer 44/47. De drie verschillen
  zijn alleen regelnummers in een Java-stacktrace (`IrEmitter.allocationVersion(IrEmitter.java:…)`): die drie fragmenten
  laten los van de casus de emitter in beide bouwen dezelfde uitzondering gooien, en A7 maakte `IrEmitter.java` één regel
  korter. Slices: gate 11 hieronder (29 schema, 28 welgevormd, 29 in drie uitvoerders).

#### Gate-regels (elk los na `eval $(opam env)`, op de kop `be7175f7`; de jar één keer, door gate 11)
- `make test`: `728 tests found`, `728 tests successful`, `0 tests failed`
- 1: `check-self-contained: PASS`
- 10: `check-regelspraak: PASS — JUnit suite green (728 tests, floor 173); 89 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `drools-jar: rebuilt (12 source path(s) newer than the jar, e.g. backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader; 5s)`;
  `check-toka-parity: PASS`; `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-… toka-45-…); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)`;
  `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum); …`;
  `differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences`
  (`ref : 29 PASS / 0 diverge`, `clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge`, `Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`)

#### Registratie bij de landing (bij A8 van de lens)
- Ronde 6 schreef "hier is de frontend strenger dan de verifier" (versie 2 onbewaakt): in de unie niet meer waar,
  `a41f6d2d` laat WF-033 elke versie toetsen. Open punt 8 van ronde 6 (twee versies naar-één, WF-052) is met
  `36750511` gesloten. Open punt 4 van ronde 4 (verifierarmen voor wortel, absolute waarde en teken) is met de
  unieverifier gesloten en leidde tot B2.

#### Slotronde (kop `147eabd3`)
- `91cfeec5` resolver: het teken van een literal uit haar waarde. De verifierfixer laat WF-039 het teken van de
  WAARDE lezen (nul negatief noch positief); de frontend doet nu hetzelfde (`Literalen.getal`, `signum`), de zin noemt
  het geschreven lexeem. Getuige `TyperingZinTest.tekenbeperking_van_het_doel` (rood-eerst): `-0` in een niet-negatief
  doel geeft geen melding meer; `teken-beperking: literal '-0' is written into a target under a negative sign
  restriction` en `teken-beperking: literal '0/5' is written into a target under a positive sign restriction` zijn nieuw;
  `-5`, `0` en `5` ongewijzigd. Let op: tegen de unieverifier (die het lexeem nog leest) verschillen `-0` en `0/5` tot
  `fix/lens-verifier` landt; tegen die fix vallen frontend-ERROR en verifier-FAIL samen (niet door mij gemeten: die
  branch staat niet in deze worktree).
- `147eabd3` resolver: de weigering van de volledige periode draagt haar regel (`bron.line()` in plaats van 0, beide
  takken van `keurGeheelPeriode`). Getuige: de drie beweringen in `TyperingZinTest` verwachten nu `ERROR 30 …`
  (rood-eerst gemeten). Restpunt 1 van ronde 7 (regel 0) is daarmee gesloten.
- `make test`: `728 tests found`, `728 tests successful`, `0 tests failed`; 1: `check-self-contained: PASS`; 10:
  `check-regelspraak: PASS — JUnit suite green (728 tests, floor 173); 89 rgs golden(s) re-emit byte-identically (floor
  80)`. Gate 11 niet gedraaid: deze twee wijzigingen raken alleen ERROR-meldingen, en ERROR-meldingen bereiken de
  toka-emissie niet.
- **(3) Structurele weigeringen die de verifier in eigen woorden deelt en die in de frontend nog Nederlands zijn: 14.**
  Dat is meer dan een handvol, dus niet omgezet; hier precies, voor een verse fixer (regels op `147eabd3`; patroon van
  ronde 4: `<Nederlands voorvoegsel>: <verifierzin>`, gemeten door de geldige emissie tot precies het gebrek te
  veranderen; de guard `LezerszinGrensTest` vangt ze daarna). Bestaande tests pinnen enkele Nederlandse zinnen
  (telling 4 bestanden, defaults-groep 6, zie ronde 4): die volgen met mandaat.
  1. `RuleChecker.java:285` `regel '<r>'…: een gewone regel kan een kenmerk niet ontkennen (§9.2) — …` ↔ WF-022
     (`checks.ml:1687`) `characteristic target's value must be the boolean constant true`.
  2. `RuleChecker.java:402` `… heeft naast de defaults-groep <g> nog een schrijver …` ↔ WF-050 (`checks.ml:7866`)
     `a rule also writes defaults target '<T.a>' — an unordered plain writer would silently defeat the group's
     decisions`.
  3. `RuleChecker.java:411` `… heeft meer dan één defaults-groep (<g>) en niet elke groep is ongeordend …` ↔ WF-050
     (`checks.ml:7842`) `multiple defaults groups on target '<T.a>' without the declared-unordered marking — …`.
  4. `RuleChecker.java:190` `recursieve regelgroep '<g>' zonder objectcreatieregel — …` ↔ WF-033 (`checks.ml:3352`)
     `recursive group without a creation member` (de verifier aanvaardt ook een feitcreatie als drager; de frontend
     niet).
  5. `RuleChecker.java:2260` `geldigheidsperioden van regelversies overlappen (regel '<r>')` ↔ WF-021 (`checks.ml:1642`)
     `validity windows of versions '<a>' and '<b>' overlap (<array>)`.
  6. `RuleChecker.java:3189` `startpuntbepaling: tijdlijn '<t>' is niet gedeclareerd (regel '<r>')` ↔ WF-032
     (`checks.ml:3273`) `start point for undeclared timeline '<t>'`.
  7. `RuleChecker.java:704` `periodevergelijking onder '<w>' (<plaats>): een venster (§8.4.3) maakt de REGEL
     tijdsafhankelijk, …` ↔ WF-112 (`checks.ml:4248`) `a time window under none or a counting quantifier: …`.
  8. `RuleChecker.java:3453` `periodeGrensFout` (redenen vanaf r. 3457: `de parameter '<p>'`, `een berekende
     datumexpressie`, …) ↔ WF-111 (`checks.ml:4192`) `the <kant> bound of a time window is <wat> — a bound is a date
     literal or an own scalar date attribute of the subject; the general date expression is an OPEN wire decision`
     (en de millisecondezin daarna).
  9. `RuleChecker.java:2877` `objectcreatie: '<m>' is geen attribuut of kenmerk van <T> (§13.4.6.2)` ↔ WF-027
     (`checks.ml:2006`) `'<m>' is not a declared attribute of '<T>'`.
  10. `DefinitionResolver.java:527` `eenheidstelsel '<s>': de afkorting '<a>' is al gedeclareerd …` ↔ WF-009
      (`checks.ml:1261`) `the unit '<a>' is declared twice in the unit system '<s>': one Unit per abbreviation (WF-009)`.
  11. `DefinitionResolver.java:893` `dagsoort '<d>' heet naar een kalendereenheid uit de tijdseenheidtabel en kan geen
      dagsoort zijn` ↔ WF-017 (`checks.ml:1432`) `day-kind '<d>' shadows a built-in calendar unit`.
  12. `ExpressieBouwer.java:1747` `opgesomde telling: '<snippet>' noemt <n> operanden — …` ↔ WF-037 (`checks.ml:4544`)
      `a count folds INSTANCES and has no LISTED form, and this node carries <n> operands: …`.
  13. `ExpressieBouwer.java:1797` `tellingBronFout` ↔ WF-037 (`checks.ml:4570`) `the count is an INSTANCE fold and MUST
      carry an element type: …`.
  14. `ExpressieBouwer.java:647` `het aantal gevulde waarden van vereist een setlezing als bron: …` ↔ WF-037
      (`checks.ml:4654`) `the filled count is an INSTANCE fold and MUST carry an element type: …`.
  Niet tegen een verifierarm gelegd (voor dezelfde fixer): de setlezing buiten een vouwbron, beslistabelrijen met één
  regelnaam, "ongedefinieerd …", de leden van een ongeordende groep, een uitzonderingsblok in een regelgroep, en de
  startpuntmeldingen over onderwerp, uitdrukkingstype en ankernaam. De onmogelijke datumliteral blijft in de
  DIVERGENCES-lijst.

#### Restpunten
1. Uit ronde 4, nog open: punt 1 (de 14 structurele weigeringen hierboven, nog Nederlands) en punt 5 (de datumliteral
   die geen kalenderdatum is, Nederlands; staat in de DIVERGENCES-lijst). Punt 3 (regel 0) is in de slotronde gesloten. Punt 6 (de clamp-WARNING) staat nu in DIVERGENCES (A5) en blijft zolang de
   pariteitszone de vorm draagt.
2. De guard ziet een Engels fragment zonder een woord uit zijn lijst niet, en leest `ir/Spelling` bewust niet; beide
   staan in zijn javadoc.
3. A2 van de lens (het getal 18 in `toka/README.md` en `scripts/check-toka-parity.sh`) hoort bij de toka-fixer; niet
   aangeraakt.

---

### Ronde 6 — elke versie van een objectcreatie is lid van haar recursieve groep (kop **`2be2ec13`**)

#### Stand
- Direct op de branch; de unie stond op `ad1f64e6`, dus geen rebase. Eén commit:
  `2be2ec13` resolver: elke versie van een objectcreatie is lid van haar recursieve groep.
  Werkboom schoon, niets gepusht of gemerged, geen beschermd pad geraakt. JUnit 720 → 721.

#### Meting vooraf (frontend `ad1f64e6`; proeven (buiten de repo))
Een recursieve groep met één creatieregel met twee versies (`geldig vanaf 2025 t/m 2025` en `geldig vanaf 2026`),
een naar-veel-relatie (`Eén voorganger heeft meerdere opvolgers`). Voor de verifierkant dezelfde bron zonder
`is recursief` geëmitteerd en in de IR `recursive: true` gezet (de groep draagt op de wire één lid, `werf de
opvolger`, en de creatie is één vorm met twee versies).
- Frontend, alle vier de varianten (beide bewaakt, alleen versie 1 onbewaakt, alleen versie 2 onbewaakt, geen
  bewaakt): `ERROR (line 9): recursieve regelgroep 'estafette' zonder objectcreatieregel — de 'is
  recursief'-markering kan de keten niet laten voortschrijden (§9.10, gedocumenteerde extensie)`, geen document. De
  oorzaak: `RuleChecker` koppelde lid en creatie op de exacte naam (`l.equals(oc.naam())`), en het model noemt de
  versies `werf de opvolger #1` en `#2`. Ook de variant met beide versies bewaakt werd dus geweigerd, terwijl de
  verifier dat document goedkeurt (`verifier: PASS`).
- **Verifier, gemeten feit: WF-033 toetst alleen de eerste versie.** Beide bewaakt: PASS. Alleen versie 2 onbewaakt:
  PASS. Alleen versie 1 onbewaakt: `WF-033 FAIL /rules/ruleGroups/0: unguarded object creation 'werf de opvolger #1'
  in a recursive group`. Geen van beide bewaakt: alleen die ene regel voor `#1`, geen voor `#2`. In de code:
  `oc_by_name` in `checks.ml` koppelt op `strip_version` en neemt met `List.find_opt` de eerste versie. Voor de
  fixer van de verifier.

#### De fix en de meting erna (frontend `2be2ec13`)
- `RuleChecker`: lid en creatie koppelen op de naam zonder versieachtervoegsel aan beide kanten
  (`zonderVersie(l).equals(zonderVersie(oc.naam()))`, de sleutel van de verifier), zodat elke versie een lid is; de
  bestaande WF-033-lus meldt daarna per versie. De zin (ongewijzigd sinds ronde 5):
  `recursieve regelgroep '<groep>': unguarded object creation '<creatie> #<i>' in a recursive group`.
- Gemeten: beide bewaakt → document, verifier PASS. Alleen versie 1 onbewaakt → `ERROR (line 9): recursieve
  regelgroep 'estafette': unguarded object creation 'werf de opvolger #1' in a recursive group`, gelijk aan de
  verifier. Alleen versie 2 onbewaakt → dezelfde zin voor `#2`; hier is de frontend strenger dan de verifier (die geeft
  PASS). Geen bewaakt → twee ERRORs (`#1`, `#2`); de verifier meldt alleen `#1`.
- Getuige (rood-eerst: rood op de ERROR "zonder objectcreatieregel", daarna groen): `RegelgroepLedenTest` —
  beide bewaakt geen melding, versie 2 onbewaakt één ERROR voor `#2`, geen bewaakt twee ERRORs.

#### Inertheid (frontend `ad1f64e6` tegen `2be2ec13`, beide schoon uit `git archive`)
- Corpus: 88 `bron.rgs`, document en rc 88/88 gelijk, uitvoer 88/88 gelijk.
- toka (alleen tellingen): casusdump uit 45 fragmenten: basis == kop == pin, uitvoer gelijk; volledige casus (47
  bronnen): uitvoer gelijk (7 regels, 0 verschillend); elke bron los: document, rc en uitvoer 47/47 gelijk.

#### Gate-regels (elk los na `eval $(opam env)`, op de kop `2be2ec13`)
- `make test`: `721 tests found`, `721 tests successful`, `0 tests failed`
- 1: `check-self-contained: PASS`
- 10: `check-regelspraak: PASS — JUnit suite green (721 tests, floor 173); 88 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/RuleChecker.java; 2s)`;
  `check-toka-parity: PASS`; `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-… toka-45-…); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)`;
  `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum); …`;
  `differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences`
  (`ref : 29 PASS / 0 diverge`, `clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge`, `Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`)

#### Open punten na ronde 6
Open punt 7 van ronde 5 is aan de frontendkant gesloten; de verifierkant staat hierboven als gemeten feit. De
punten 1, 3, 4, 5 en 6 van ronde 4 staan nog. Nieuw:
8. Twee versies van één objectcreatie over een naar-één-relatie: de frontend schrijft het document, de verifier
   weigert het met `WF-052 FAIL /rules/objectCreations/0/versions/1: connecting relation 'opvolging' has a to-one
   created-side role and 2 object-creation writers; at most one may write it`. De versies hebben geen overlappende
   geldigheidsvensters. Buiten een groep was dat al zo in ronde 5 (gemeten, ronde 5 en 6 gelijk; zonder recursieve
   groep geeft de verifier daar ook WF-045). In een recursieve groep hield de onterechte weigering "zonder
   objectcreatieregel" deze vorm tot deze ronde tegen; nu schrijft de frontend hem net als buiten een groep. Nog te
   beslissen: telt WF-052 twee versies van één creatie als één schrijver, of weigert de frontend deze vorm.

---

### Ronde 5 — de vier documenten die de verifier weigerde (kop **`ad1f64e6`**)

#### Stand
- De hoofdsessie rebasete de branch op `card/k7-unie`; basis van deze ronde `14f22054` (716 JUnit-tests groen).
  Werkboom schoon, niets gepusht of gemerged, geen beschermd pad geraakt.
- Hashes van ronde 4 na de rebase (oud → nieuw): `7f9e319e`→`a15c0bc7`, `078563d7`→`3ec171c4`, `764f871e`→`95e1e2fa`,
  `57d9604e`→`7965ceb9`, `a09fc34c`→`b0e3ada9`, `9ce11b5c`→`ddfd18c7`, `351df1e3`→`9110543b`, `bc3e6346`→`5240da53`,
  `5550d20b`→`1bee5078`, `fee5eb7f`→`0f4f605e`, `028cb0f2`→`14f22054`.
- Vóór het werk alle zinnen van ronde 4 opnieuw gemeten tegen de verifier van de unie (die bracht onder meer
  `7f9fedfc` en twee WF-037-voorstellen mee): alle nog letterlijk gelijk, geen enkele operandzin week af.
- Commits, één per punt:
  - `250058e0` resolver: een onbewaakte objectcreatie in een recursieve groep wordt geweigerd — (a)
  - `ca6e9c80` resolver: een startpuntbepaling voor een tijdlijn met vast startpunt wordt geweigerd — (b)
  - `c8178627` resolver: een waardevouw met één operand zonder bereik wordt geweigerd — (c)
  - `ad1f64e6` resolver: de volledige periode over een samengestelde operand wordt geweigerd — (d)
- JUnit 716 → 720, +4, elk rood-eerst gemeten (rood om de juiste reden, dan groen): `RegelgroepLedenTest`
  (a), `VastStartpuntTest` (b, met als controle dezelfde bepaling op een tijdlijn met variabel startpunt: geen
  fout), `VouwOperandZinTest` (c, met als controle een setvouw en `het totaal van zijn telling`: geen melding),
  `TyperingZinTest` (d, met als controle de directe lezing: geen melding). Geen bestaande test pinde een van de
  twee WARNINGs of het oude gedrag (gemeten met grep); geen bestaande bewering aangepast. Twee testmodellen kregen
  declaraties ná de regel (twee parameters; een tijdsafhankelijk attribuut op Vondst), zodat de regelnummers
  van de bestaande beweringen gelijk bleven.

#### De zinnen letterlijk, met de meting
Meetmethode als in ronde 4: de frontend op de gebrekkige bron, en de verifier op het document dat de frontend van
vóór deze ronde schreef (bij (a) en (b): de geldige emissie met alleen de voorwaarde verwijderd, resp. het
variabele startpunt op `false`). Proeven in (buiten de repo).
- (a) `recursieve regelgroep '<groep>': unguarded object creation '<creatie>' in a recursive group` (ERROR, was een
  WARNING). Gemeten: frontend `ERROR (line 9): recursieve regelgroep 'estafette': unguarded object creation 'werf de
  opvolger' in a recursive group`, geen document; verifier `WF-033 FAIL /rules/ruleGroups/0: unguarded object creation
  'werf de opvolger' in a recursive group`. De naam: de verifier bouwt voor een vorm met meerdere versies `naam #i`,
  zoals de frontend intern (`loader.ml` `named_form`).
- (b) `startpuntbepaling (regel '<regel>'): start point for fixed-start timeline '<tijdlijn>'` (ERROR, was een
  WARNING; de lowering daarna ongewijzigd). Gemeten: frontend `ERROR (line 8): startpuntbepaling (regel 'startpunt
  penningjaar'): start point for fixed-start timeline 'penningjaar'`, geen document; verifier `WF-032 FAIL
  /rules/startPointDeterminations/0: start point for fixed-start timeline 'penningjaar'`.
- (c) `<som van|totaal van|minimale waarde van|maximale waarde van|eerste van|laatste van>: value fold with fewer
  than two operands`, bij een vouw zonder elementtype en zonder dimensieselectie met minder dan twee operanden: de
  voorwaarde van de WF-037-arm, en precies de knopen waarop de emitter geen `elementType` schrijft
  (`ExpressieBouwer.bouwFunctie`, vlak vóór de `Functie` gebouwd wordt; de dimensievouw keert eerder terug).
  Gemeten, frontend (nieuw, geen document) tegen verifier op het oude document (`WF-037 FAIL
  /rules/rules/0/versions/0/value: value fold with fewer than two operands`): `de som van de havenprijs`, `het totaal
  van de havenprijs`, `de maximale waarde van de havenprijs`, `de laatste van de havendag`, `de som van 5`; en `de som
  van zijn raadsel` (onopgeloste operand), waar het oude document ook `WF-034 … value node without a static type`
  kreeg. Ongemoeid en schoon: `het totaal van zijn telling`, `het totaal van zijn saldo` (elementtype Jutter, verifier
  PASS). Een som of extremum over één eigen attribuut weigerde de emitter al (IrEmitter, "haar bron is het eigen
  attribuut van het onderwerp en geen instantiebron").
- (d) `de check op volledige periode (gedurende de/het gehele jaar/maand): wholePeriod qualifier on a
  non-time-dependent operand` (de zin van ronde 4), nu ook voor een operand die een tijdsafhankelijke waarde leest
  door rekenen, afronden, begrenzen of een dimensiecel heen: `RuleChecker.exprTemporeel` volgt nu `expr_temporal` van
  de verifier (alleen een directe attribuut- of parameterlezing). Gemeten: `zijn vondstsaldo plus 1 gedurende het
  gehele jaar groter is dan 5` en `zijn vondstsaldo naar beneden afgerond op 0 decimalen gedurende …`: frontend `ERROR
  (line 0): …`, geen document; verifier op het oude document `WF-043 FAIL /rules/rules/0/versions/0/condition:
  wholePeriod qualifier on a non-time-dependent operand`. De directe lezing: document, verifier PASS.

#### Inertheid (frontend `14f22054` tegen `ad1f64e6`, beide schoon uit `git archive`; (buiten de repo))
- Corpus: 88 `bron.rgs` (de unie bracht er één bij): document en rc 88/88 gelijk, uitvoer 88/88 gelijk.
- toka (alleen tellingen): casusdump uit 45 fragmenten: basis == kop == pin, uitvoer gelijk. Volledige casus (47
  bronnen): rc 1 beide, uitvoer gelijk (7 regels, 0 verschillend). Elke bron los: document en rc 47/47 gelijk,
  uitvoer 45/47: de twee fragmenten `toka-20-…` en `toka-21-…` gaven los al rc 1 zonder document (hun vouwoperand
  lost alleen in de casus op), en krijgen nu elk één ERROR meer, de zin van (c). Dat die weigering daar niet strenger
  is dan de verifier, is gemeten op een eigen bron met een onopgeloste operand (zie (c)).
- De twee WARNINGs stonden in geen vastgelegd document (gemeten in ronde 4); geen golden emitteert anders: geen
  nulmeting.

#### Gate-regels (elk los na `eval $(opam env)`, op de kop `ad1f64e6`)
- `make test`: `720 tests found`, `720 tests successful`, `0 tests failed`
- 1: `check-self-contained: PASS`
- 10: `check-regelspraak: PASS — JUnit suite green (720 tests, floor 173); 88 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `drools-jar: rebuilt (16 source path(s) newer than the jar, e.g. backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader; 4s)`;
  `check-toka-parity: PASS`; `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-… toka-45-…); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)`;
  `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum); …`;
  `differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences`
  (`ref : 29 PASS / 0 diverge`, `clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge`, `Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`)

#### Open punten na ronde 5
Open punt 2 van ronde 4 is gesloten door (a)–(d). Punten 1, 3, 4, 5 en 6 van ronde 4 staan nog. Nieuw:
7. Een objectcreatie met meerdere versies in een recursieve groep (gelezen, niet gemeten): de frontend koppelt
   groepsleden aan creaties op de exacte naam, en een creatie met versies heet intern `naam #i`, dus zij wordt nooit
   als lid herkend. De verifier koppelt op de naam zonder versieachtervoegsel en toetst dan alleen de eerste versie
   (`List.find_opt` in `oc_by_name`).

---

### Ronde 4 (vervolgbouwer) — kop **`028cb0f2`**

#### Stand
- Branch `card/frontend-emitterrestanten`, worktree (worktree emitter), basis `b096bdc2`,
  kop `028cb0f2`. Niets gepusht of gemerged; werkboom schoon. Geen beschermd pad geraakt (geen scriptregels,
  geen spec, geen corpus, geen `toka/`).
- Elf commits in ronde 4. Zeven van mijn voorganger (in zijn Result nog niet beschreven, hieronder wel):
  `7f9e319e` tot de macht met een eenheid, `078563d7` datumprecisie, `764f871e` keuzetypedomein, `57d9604e` familie
  bij toekennen en gelijkheid, `a09fc34c` niet-numerieke operanden bij rekenen en ordenen, `9ce11b5c`
  voorwaardepredicaten, `351df1e3` enumlabel buiten het domein. Vier van mij:
  - `bc3e6346` resolver: de operanden van vouwen, paasdag en tijdsduur in de zinnen van de verifier (stap 1)
  - `5550d20b` resolver: een gebrek in een voorwaarde één keer gemeld (gevonden bij stap 1, direct opgelost)
  - `fee5eb7f` resolver: leegwaarde, venstergrens, telwoord, volledige periode en kenmerkwaarde in de zinnen van de
    verifier (stap 2)
  - `028cb0f2` DIVERGENCES: de producentweigeringen zonder lezerszin (stap 2)
- Tellingen: JUnit 692 (basis) → 709 (kop), +17: `TyperingZinTest` 12, `VouwOperandZinTest` 5. Rgs-goldens 87.
  Aangeraakte bestanden in mijn commits: `resolver/ExpressieBouwer.java`, `resolver/Typering.java`,
  `resolver/RuleChecker.java`, `grammar/DIVERGENCES.md`, tests `VouwOperandZinTest.java` (nieuw),
  `TyperingZinTest.java` (model: een Vondst en een feittype ná de regel, zodat de regel op regel 30 blijft).

#### Meetmethode (zoals ronde 3 (a))
Per geval twee kanten met dezelfde namen: de frontend op de gebrekkige bron, en de echte verifier
(`spec/verifier/_build/default/bin/verifier.exe`, actueel na `dune build`) op de geldige emissie waarin alleen de
declaratie (of de literal, of de drempel) tot precies het gebrek is veranderd. Scripts en proeven:
(buiten de repo) (+ `vouw.py`, `param.py`, `prec.py`), (buiten de repo) (+ `zes.py`, `drie.py`,
`drie2.py`, `drie3.py`), gaten in (buiten de repo); de proeven van mijn voorganger in (buiten de repo)
(opnieuw door de verifier gehaald). Waar de gemuteerde IR ook een `WF-091 … mismatch` op de type-annotatie van de
vouw geeft, is dat een bijproduct van de mutatie (de annotatie volgt de oude declaratie), niet van het gebrek.

#### Per familie: de zin letterlijk, met de meting
Vorm: de frontendmelding is `<Nederlands voorvoegsel>: <verifierzin>`; meerdere gebreken op één plek gescheiden
door `; `. Verifierregel = `WF-### FAIL <pointer>: <zin>` op de gemuteerde proef.

Eerdere commits van ronde 4 (zinnen gepind in `TyperingZinTest`; verifierregels op (buiten de repo)):
1. Tot de macht (`7f9e319e`): `tot de macht: power operands must be unitless` — `WF-035 … power operands must be
   unitless` (ook voor een percentage: `a-macht-pct`).
2. Datumprecisie (`078563d7`): `voorwaarde: gelijk: equality across date precisions` (WF-036);
   `gelijkstelling: toekenning: a date-ms value is assigned to a date-day target` (WF-023);
   `eerste van: first orders dates of two precisions (day and millisecond), which share no order` (WF-038);
   `de (absolute) tijdsduur van … tot …: duration operands mix date precisions (both must share one precision)`
   (WF-038); `een tijd-component (uur/minuut/seconde/milliseconde uit): sub-day extractor operand is date-day, must
   be a millisecond-precision date` (WF-038).
3. Keuzetypedomein (`764f871e`): `voorwaarde: gelijk: equality across different enumeration domains` (WF-036);
   `gelijkstelling: toekenning: a value of the enumeration domain 'Weerklasse' is assigned to a target of the
   enumeration domain 'Keurklasse'` (WF-023).
4. Familie (`57d9604e`): `gelijkstelling: toekenning: a text value is assigned to a number target` (WF-023),
   `voorwaarde: gelijk: equality across families: text vs number` (WF-036); getal naast percentage zonder literal:
   `…: toekenning: assignment across incompatible units` / `voorwaarde: gelijk: equality operands have incompatible
   units`; met een eenheidloze literal PASS in de verifier, dus eigen zin (`f-*-pct-lit`).
5. Niet-numerieke operanden (`a09fc34c`): `verminderd met|maal|gedeeld door|tot de macht: operand of a numeric
   operator is text` (WF-035); `plus: date ± date is ill-formed (date arithmetic takes a date and a duration)`,
   `min: duration − date is ill-formed (subtraction admits the date on the left only)`, `plus: duration + date is
   ill-formed (the date stands on the left; v0 does not commute)`, `plus: date ± duration: the duration operand is
   text, must be a number carrying a time unit`, `plus: date ± duration: the duration operand carries no time unit`,
   `plus: 'EUR' is not a recognised time unit or declared day kind` (WF-035); `voorwaarde: vergelijking: ordering
   operand is text`, `voorwaarde: vergelijking: date-ordering operand is number` (WF-036). Verifier PASS, dus eigen
   zin: tekst plus getal (`a-tekst-plus`), maal met een percentage (`a-maal-pct`, alleen het annotatiebijproduct).
6. Voorwaardepredicaten (`9ce11b5c`): `voorwaarde: elfproef: elevenTest operand is date-day, must be text or a whole
   unitless number`, `voorwaarde: numeriek met exact: exactDigits operand is number, must be text`, `voorwaarde:
   dagsoort: day-kind predicate operand is number, must be a date` (WF-036).
7. Enumlabel (`351df1e3`): `enumeration label 'zwart' is not in the declared domain` (WF-039), bij toekenning en
   vergelijking.

Stap 1 (`bc3e6346`; de ongecommitte diff was af en groen; ik heb twee dode `null`-controles en de ongebruikte
`sectie`/`kind` in `tijdsduurPrecisieFout` weggehaald, de javadoc bijgewerkt en drie takken aan de test toegevoegd):
8. Ordeningsvouw, operand: `maximale waarde van: operand of the maximum ordering fold is text, must be a number or
   a date` — `WF-038 FAIL /rules/rules/0/versions/0/value/arguments/1: operand of the maximum ordering fold is text,
   must be a number or a date`; idem `minimale waarde van:`/`minimum` (arguments/0), `eerste van:`/`first`,
   `laatste van:`/`last` (arguments/0).
9. Ordeningsvouw, families: `maximale waarde van: maximum orders operands of two comparable families, numbers and
   dates, which share no ordering` — `WF-038 FAIL …/value: …` gelijk; idem `minimum`, en `laatste van: last orders
   operands of two comparable families, …`.
10. Ordeningsvouw, precisies bij minimum/maximum: `maximale waarde van: maximum orders dates of two precisions (day
    and millisecond), which share no order` — `WF-038 FAIL …/value: …` gelijk (geldige getallenvouw, beide
    declaraties tot datums van twee precisies veranderd).
11. Rekenvouw (WF-091, numeric-operand-arm): `som van: the sum of 'vondstproef' on 'Vondst' folds Text values: a
    total, sum or pro-rata part is arithmetic over a Number, and Text values are not amounts` — `WF-091 FAIL
    …/value/arguments/0: …` gelijk; Date-variant `… folds Date values: … and Date values are calendar positions`
    (dag- en millisecondeprecisie); `totaal van: the total of …` (tekst en datum); parameter: `som van: the sum of
    the parameter 'proefparameter' folds Text values: …` en `totaal van: the total of the parameter 'proefparameter'
    folds Date values: …` gelijk; andere operand: `som van: the sum of its operand 0 folds Date values: …` — gelijk
    aan de verifier op het parameterdocument met een `ContextValue` (`evaluationDate`) als operand. Een som over tekst
    krijgt terecht ook de toekenningszin (`berekening: toekenning: a text value is assigned to a number target`).
12. Paasdag: `eerste paasdag van: easterSunday operand is text|number|percentage, must be a whole unitless number` —
    `WF-038 FAIL …/value: …` gelijk voor tekst, 2 decimalen, EUR en percentage.
13. Tijdsduur: `de (absolute) tijdsduur van … tot …: duration operand is number, must be a date` — `WF-038 FAIL
    …/condition/left/arguments/1: …` gelijk; tekst links: `…/arguments/0`.
14. Blijft eigen zin (verifier PASS, gemeten): percentage in minimum/maximum (`v-max-pct`), getallen in eerste/laatste,
    percentage in een som (`w-som-pct`: alleen het annotatiebijproduct), wortel en absolute waarde over tekst
    (`v-wortel-tekst`, `v-abs-tekst`: PASS; de verifier kent voor deze twee alleen de ariteit, `scalar_one_arg_kinds`).

Gevonden bij stap 1 (`5550d20b`): elke melding die bij het bouwen van een voorwaarde-operand ontstaat (vouw, paasdag,
tijdsduur, tijd-component) stond twee keer in de lijst, ook op de basis (gemeten met de frontend van `b096bdc2`):
`RuleChecker.voorwaarden()` bouwt de operand via `infer` een tweede keer. Nu valt een melding die die tweede bouw
letterlijk herhaalt weg (het patroon van `zetVariabelen`); een melding die alleen die bouw geeft blijft. Getuige:
één gebrek → één melding; een tijdsduur met één niet-datum aan beide kanten van één vergelijking → twee meldingen,
zoals de verifier daar twee rapporten geeft (`…/left/arguments/1`, `…/right/arguments/1`). De frontend meldt per
knoop en voegt de operandzinnen samen met `; `; de verifier meldt per operand (gemeten: twee knopen met elk twee
niet-datums geven 2 frontendmeldingen en 4 verifierrapporten). Geen document droeg een dubbele diagnose (0 van 767 vastgelegde `ir.json`); de WARNING
`onopgelost` komt uit de eindronde over het model en was niet dubbel.

Stap 2 (`fee5eb7f`), resterende type- en waardeweigeringen met een eigen verifierzin:
15. `leegwaarde-alternatief van een sommatie: sum fallback is text, must be numeric` — `WF-038 FAIL
    …/value/emptyFallback: sum fallback is text, must be numeric`. Percentage blijft eigen zin (de verifier telt het
    als getal); die zin drukte het record af (`kreeg Percentage[...]`), nu `kreeg Percentage (§5.8.2)`.
16. `periodevergelijking: window bound is number, must be a date` — `WF-042 FAIL …/condition/from: window bound is
    number, must be a date`.
17. `telwoord van een kwantificatie: counting quantifier with threshold 0 < 1` — `WF-041 FAIL …/condition: counting
    quantifier with threshold 0 < 1`. Alleen als het geschreven telwoord een geheel getal is (`keurDrempel` krijgt nu de
    kwantificatiecontext); een decimaal of breuk komt als 0 op de wire, dus die houdt de eigen zin.
18. `de check op volledige periode (gedurende de/het gehele jaar/maand): wholePeriod qualifier on a non-time-dependent
    operand` (vergelijking, predicaat) — `WF-043 FAIL …/condition: …` gelijk; `… non-time-dependent characteristic`
    (eigen kenmerk, tijdlijn uit de declaratie gehaald) — gelijk. Genavigeerd kenmerk en andere voorwaardesoorten:
    eigen zin (de kenmerkarm van WF-043 vraagt `navigatie_binding = None`).
19. `kenmerkwaardetoekenning 'gewogen': characteristic initialisation's value must be a boolean-typed expression` —
    `WF-027 FAIL /rules/objectCreations/0/versions/0/initialisations/0: …` gelijk. Een booleaans getypeerde expressie
    die geen literal is laat de verifier toe (`Some Booleaan | None -> []`): eigen zin.
20. Niet omgezet, gemeten: de tekenbeperking van een literal (`waarde -5 voldoet niet aan de teken-beperking
    'niet-negatief' …`): verifier PASS op de gemuteerde proef, want de frontend schrijft de literal met zijn eigen
    type (teken `unspecified`); alleen een literal die zelf een teken draagt weigert de verifier (WF-039, gemeten met
    een handmatig getekende literal). Meer decimalen dan het doel: PASS. Maximum over EUR en kg: PASS.

`DIVERGENCES.md` (`028cb0f2`): nieuwe sectie "Producer refusals without a reader sentence", één alinea met de lijst
(eenheid naast eenheidloze literal bij plus/min/toekenning, plus/min zonder datum over een niet-getal, percentage in
product, quotiënt, minimum, maximum, som en leegwaarde, getal naast percentage met literal, datum in
minimum/maximum, getal in eerste/laatste, wortel en absolute waarde, eenheden in minimum/maximum, totaal zonder
tijdsnoemer, decimalen bij toekennen en verdelen, familie en eenheid bij verdelen, tekenbeperking van een literal,
eenheden die het eenheidsmodel niet kan spellen, een telwoord of cijferaantal dat geen geheel getal is, een
onmogelijke datum(-tijd), een booleaanse kenmerkwaarde die geen literal is, de volledige periode op een genavigeerd
kenmerk of een andere voorwaardesoort) en de weigeringen van de brontekst zelf. De testklassen verwijzen ernaar.

#### Inertheid (stap 3), frontend van `b096bdc2` en van de kop, beide schoon uit `git archive`
- Corpus: 87 `bron.rgs` (de opdracht noemde 86; gate 10 telt er ook 87): document en rc byte-gelijk 87/87, uitvoer
  (stdout+stderr) gelijk 87/87, alle 87 schrijven een document ((buiten de repo) corpus`).
- toka (alleen tellingen): 47 bronnen naast de casus, 45 in de casusdump zoals gate 11 die samenstelt (gesorteerd,
  zonder de holdouts `toka-23-…`, `toka-45-…`). Casusdump: rc 0 beide; basis == pin, kop == pin, basis == kop;
  uitvoer gelijk. Elke bron los: 47/47 document en rc gelijk, 47/47 uitvoer gelijk. Volledige casus (47 bronnen):
  rc 1 beide, geen document beide; uitvoer 8 → 7 regels, ERROR 4 → 3, WARNING 4 → 4: één ERROR kreeg in deze ronde
  de verifierzin (de datumrekenweigering, `a09fc34c`), en één ERROR die de basis op één regel twee keer meldde staat
  er nu één keer (`5550d20b`). Geen wire-verschil; `CasusTest` (anyMatch en filter) blijft groen.
- Geen golden emitteert anders: geen nulmeting.

#### Gate-regels (stap 4; elk los na `eval $(opam env)`, op de kop `028cb0f2`)
- `make test`: `709 tests found`, `709 tests successful`, `0 tests failed`
- 1: `check-self-contained: PASS`
- 10: `check-regelspraak: PASS — JUnit suite green (709 tests, floor 173); 87 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `drools-jar: rebuilt (4 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver; 3s)`;
  `check-toka-parity: PASS`; `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-… toka-45-…); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) metaschema-valid + byte-round-trip (gate-2 discipline, explicit mode)`;
  `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum); …`;
  `differential replay: 29 toka cases (floor 29) through ref, clingo, Drools (slices under toka/cases/), ZERO silent divergences`
  (`ref : 29 PASS / 0 diverge`, `clingo : 29 PASS / 0 loud translation-fault / 0 silent diverge`, `Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`)

#### Wat aan de opdracht niet klopte (gemeten)
1. "86 corpusbronnen": er zijn 87 `bron.rgs` onder `corpus/golden`, en gate 10 re-emitteert er 87.
2. De ongecommitte stap-1-diff was al af (de test groen); ik heb alleen opgeruimd en drie takken toegevoegd.

#### Definitieve weigerzinnen van deze ronde (letterlijk; `<…>` is variabel)
- `<minimale waarde van|maximale waarde van|eerste van|laatste van>: operand of the <minimum|maximum|first|last> ordering fold is <kind>, must be a number or a date`
- `<…>: <minimum|maximum|first|last> orders operands of two comparable families, numbers and dates, which share no ordering`
- `<…>: <minimum|maximum|first|last> orders dates of two precisions (day and millisecond), which share no order`
- `<som van|totaal van>: the <sum|total> of <'<attr>' on '<type>'|the parameter '<p>'|its operand <i>> folds <Text|Date|Boolean|Enumeration> values: a total, sum or pro-rata part is arithmetic over a Number, and <Text|Boolean|Enumeration> values are not amounts` (Date: `and Date values are calendar positions`)
- `eerste paasdag van: easterSunday operand is <kind>, must be a whole unitless number`
- `de (absolute) tijdsduur van … tot …: duration operand is <kind>, must be a date`
- `leegwaarde-alternatief van een sommatie: sum fallback is <kind>, must be numeric`
- `periodevergelijking: window bound is <kind>, must be a date`
- `telwoord van een kwantificatie: counting quantifier with threshold <n> < 1`
- `de check op volledige periode (gedurende de/het gehele jaar/maand): wholePeriod qualifier on a non-time-dependent <operand|characteristic>`
- `kenmerkwaardetoekenning '<kenmerk>': characteristic initialisation's value must be a boolean-typed expression`
- De zinnen van de zeven eerdere commits staan hierboven onder 1–7. `<kind>` is het soortwoord van de verifier
  (`number`, `percentage`, `text`, `boolean`, `date-day`, `date-ms`, `enumeration`); de lezers (ref, clingo,
  Drools) dragen deze typeringsregels niet, dus alleen de verifier is de maat. Geen CONSTRUCTS-registerzin gevraagd.

#### Open punten na ronde 4 (het open punt van ronde 3 over de resterende Nederlandse resolvermeldingen is voor de
typerings-, eenheids-, vouw- en waardefamilies gesloten; C van ronde 2 en de open punten van ronde 1 staan
hieronder in hun eigen secties)
1. Structurele weigeringen die de verifier in eigen woorden deelt, nog Nederlands (kop `028cb0f2`, arm gelezen in
   `checks.ml`, niet op de wire gemeten): `RuleChecker` r. 282 (een gewone regel ontkent een kenmerk) ↔ WF-022
   `characteristic target's value must be the boolean constant true`; r. 399 (nog een schrijver naast een
   defaults-groep) ↔ WF-050 `a rule also writes defaults target '<T.a>' — an unordered plain writer would silently
   defeat the group's decisions`; r. 408 (meer groepen, niet alle ongeordend) ↔ WF-050 `multiple defaults groups on
   target '<T.a>' without the declared-unordered marking — …`; r. 187 (recursieve groep zonder objectcreatie) ↔
   WF-033 `recursive group without a creation member` (de verifier aanvaardt ook een feitcreatie als drager, de
   frontend niet); r. 2257 (versievensters overlappen) ↔ WF-021 `validity windows of versions '<a>' and '<b>' overlap
   (<array>)`; r. 3180 (startpunt voor een onbekende tijdlijn) ↔ WF-032 `start point for undeclared timeline '<t>'`;
   r. 701 (venster onder geen of een tellende kwantor) ↔ WF-112; `periodeGrensFout` ↔ de WF-111-zinnen; r. 2868
   (creatielid dat geen attribuut of kenmerk is) ↔ WF-027 `'<m>' is not a declared attribute of '<T>'`;
   `DefinitionResolver` r. 526 (afkorting twee keer) ↔ WF-009 `the unit '<a>' is declared twice in the unit system
   '<s>': one Unit per abbreviation (WF-009)`; r. 892 (dagsoort heet naar een kalendereenheid) ↔ WF-017 `day-kind
   '<d>' shadows a built-in calendar unit`; `ExpressieBouwer.tellingAriteitFout` ↔ WF-037 `a count folds INSTANCES
   and has no LISTED form, and this node carries <n> operands: …`; `tellingBronFout` ↔ WF-037 `the count is an
   INSTANCE fold and MUST carry an element type: …`; het gevulde aantal zonder setbron (r. 635) ↔ WF-037 `the filled
   count is an INSTANCE fold and MUST carry an element type: …`. Niet omgezet omdat deze meldingen onder
   maintainerbesluiten schrijfaanwijzingen dragen, bestaande tests ze pinnen (telling 4 testbestanden, defaults-groep
   6, setlezing 8) en enkele verifierzinnen wire-namen noemen (versielabels, arraynamen) die de resolver zou moeten
   nabouwen. Niet tegen een arm gelegd: setlezing buiten een vouwbron (r. 2007), beslistabelrijen met één regelnaam
   (r. 2164), "ongedefinieerd" (r. 2360), leden van een ongeordende groep (r. 1116–1132), een uitzonderingsblok in
   een regelgroep (r. 488), startpunt r. 3191/3198/3209.
2. De frontend schrijft een document dat de verifier weigert (gemeten, (buiten de repo)):
   (a) een onbewaakte objectcreatie in een recursieve groep: WARNING `objectcreatieregel '<n>' in recursieve
   regelgroep '<g>' heeft geen voorwaarde — …`, document geschreven; verifier `WF-033 FAIL /rules/ruleGroups/0:
   unguarded object creation 'werf de opvolger' in a recursive group`. (b) een startpuntbepaling voor een tijdlijn met
   vast startpunt: WARNING, document geschreven; verifier `WF-032 FAIL /rules/startPointDeterminations/0: start point
   for fixed-start timeline 'penningjaar'`. Voorstel voor beide: ERROR in de verifierzin; inert gemeten in de zin dat
   geen vastgelegd document die WARNING draagt, 0 toka-bronnen een recursieve groep hebben en het verifierrapport van
   de gepinde casusdump (15 regels: 1 × WF-022, 3 × WF-026, 8 × WF-034, 1 × WF-045, 1 × WF-079, 1 × WF-099) geen
   WF-032 of WF-033 bevat. (c) `de som van <scalaire parameter>` (ook `het totaal van …`): geen
   melding; verifier `WF-037 FAIL …/value: value fold with fewer than two operands`. Beslissing nodig: weigeren of
   anders lageren. (d) volledige periode over een samengestelde tijdsafhankelijke operand (`zijn saldo plus 1
   gedurende het gehele jaar groter is dan 5`): geen melding; verifier `WF-043 FAIL …/condition: wholePeriod qualifier
   on a non-time-dependent operand` (de verifier noemt alleen een directe attribuut- of parameterlezing
   tijdsafhankelijk; `RuleChecker.exprTemporeel` leest ook door rekenen, afronden en begrenzen heen).
3. De weigering van de volledige periode draagt regel 0 (`keurGeheelPeriode`, bestond al); `bron.line()` zou de
   bronregel geven.
4. Spec-kant: de verifier heeft voor `squareRoot` en `absoluteValue` geen operandtype-arm (alleen de ariteit); een
   tekstoperand passeert (gemeten). En de tekenbeperking van het doel wordt niet aan een literal getoetst die zijn
   eigen type draagt (WF-023 vergelijkt twee gedeclareerde tekens, WF-039 de literal met zijn eigen annotatie).
5. De datum(-tijd)-literal die geen kalenderdatum is (`ExpressieBouwer` r. 462/482) houdt de eigen zin; de verifier
   heeft lexeemzinnen (WF-084, WF-095) voor een geschreven lexeem, dat de frontend alleen uit een geldige datum vormt.
6. C van ronde 2 blijft: de clamp-melding voor het eenzijdige paar is een WARNING zolang de toka-casus de vorm draagt.

---

### Ronde 3 — na de tweede rebase op de unie (nieuwe kop **`e87d6064`**)

Basis `cb652adb` (gate 1 daar groen na de herspelling in IR-SPEC). Commits deze ronde:

- `9e3aa475` golden jutterswet-strandmelding: her-emissie met de rol die de zin noemt
- `a58134cc` resolver: eenheids- en numeriekweigeringen in de zinnen van de verifier — punt (a)
- `aa4feb87` resolver: een verzameling van twee labels zonder komma leest twee labels — punt (b)
- `1ef6ce3f` golden jutterswet-strandmelding: de Drools-meting na de her-emissie
- `e87d6064` resolver: een niet-numerieke clampbasis één keer gemeld (gevonden bij (a), direct opgelost)

#### (0) Gevonden op de basis: gate 10 zag een verschil bij `jutterswet-strandmelding`
De golden kwam uit de kaart verifier-allocatie-keten, geëmitteerd zonder mijn U3; op de unie schrijft de
frontend er `connectingRole {strandmelding, gemeld strandrecht}` bij (+5 regels). Dat is dezelfde relatie die het
typenpaar afleidt (één feittype tussen Melding en Strandrecht), dus geen betekenisverschil. Her-emitteerd; case
ongewijzigd. Opnieuw gemeten: verifier PASS; ref en clingo 5 verwachtingen PASS; de WF-050-mutatie van haar NOTE
(`when norm low = norm nt` → `when true || …`, op een kopie van de verifier in /tmp) weigert haar nog met hetzelfde
rapport; Drools exec `PLAYED 1`, DRL-tekst `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no
defaults lowering`. NOTE aangevuld.

#### (a) Eenheids- en numeriekweigeringen in de verifierzin
- Vóór (proeven (buiten de repo)): `plus: als één operand een eenheid heeft, moeten beide er een hebben`,
  `plus: eenheden 'EUR' en 'kg' zijn niet in elkaar om te rekenen`, idem onder `voorwaarde: vergelijking: …`,
  `voorwaarde: gelijk: …`, `gelijkstelling: toekenning: …`; `begrenzing is alleen toegestaan op een numerieke waarde,
  kreeg Tekst`, `begrenzing vereist een numerieke grens, kreeg Tekst`.
- Gemeten: de frontend adopteert een eenheidloze literal alleen bij een vergelijking (`RuleChecker.meldVergelijk`),
  niet bij rekenen en toekennen; de verifier adopteert op alle drie de posities. `€ plus 5` en `€ := 5` weigert
  alleen RegelSpraak — daarvoor bestaat geen verifierzin, dus die twee houden de Nederlandse zin.
- Na (definitieve zinnen, na het bestaande contextvoorvoegsel):
  - rekenen: `plus: additive operands have incompatible units` (ook `min:`, `verminderd met:`) — WF-035
  - ordening: `voorwaarde: vergelijking: ordering operands have incompatible units` — WF-036
  - gelijkheid: `voorwaarde: gelijk: equality operands have incompatible units` — WF-036
  - toekennen: `<soort>: toekenning: assignment across incompatible units` (regel, objectcreatielid) — WF-023
  - clamp: `clamp base is <soort>, must be numeric` en `clamp minimum|maximum is <soort>, must be numeric` — WF-035,
    met de soortwoorden van de verifier (`number`, `percentage`, `text`, `boolean`, `date-day`, `date-ms`,
    `enumeration`)
  - blijft Nederlands (geen verifierzin): de eenzijdige eenheid naast een eenheidloze getalliteral bij rekenen en
    toekennen; de verdeling (de verifier kruist geen eenheden bij een allocatie); minimum/maximum over gelijste
    operanden (geen eenheidsarm in de verifier).
- Pariteit gemeten op de wire (geldige emissie, in de IR tot precies het gebrek veranderd, door de echte verifier):
  `WF-035 FAIL …/value: additive operands have incompatible units`; `WF-036 FAIL …/condition: ordering operands have
  incompatible units`; `WF-036 FAIL …/condition: equality operands have incompatible units`; `WF-023 FAIL
  /rules/rules/0/versions/0: assignment across incompatible units`; `WF-035 FAIL …/value/base: clamp base is text,
  must be numeric`.
- Tests: nieuw `EenheidZinTest` (5, rood-eerst). Bestaande test aangepast met commentaarregel:
  `toka/src/test/java/io/github/yhavinga/regelspraak/CasusTest.java` r. 56 (de herkenning van de toka-45-quirk,
  twee meldingen, nu in de WF-035-zin); `TyperingTest` toetst geen tekst en bleef ongemoeid. De gedateerde meting in
  `corpus/golden/jutterswet-tempototaal/NOTE.md` kreeg één alinea met de huidige zin
  (`ERROR (line 23): berekening: toekenning: assignment across incompatible units`, opnieuw gemeten).

#### (b) `{a en b}`: productie, fix, inertheid
- Productie: `dimensieWaardeWoord` bevat `EN` (`RegelSpraak.g4`), en `dimensieWaarde : dimensieWaardeWoord+`
  staat in `aggregerenOverBereik : DE dimensieNaamMeervoud IN LBRACE dimensieWaarde ( ( COMMA dimensieWaarde )* EN
  dimensieWaarde )? RBRACE`. Bij `{voorjaar en najaar}` zijn twee ontledingen geldig (één lid, of lid + `EN` + lid)
  en kiest ANTLR de gulzige lus: één lid `voorjaar en najaar`, geen gedeclareerd label, selectie onopgelost. Met een
  komma (`{a, b en c}`) eist de staart een `EN`, dus daar kiest de parser goed.
- Fix (klein, resolver, grammatica ongemoeid): `ExpressieBouwer.bereik` leest een lid dat geen gedeclareerd label is
  als labels verbonden door "en", als precies één indeling van de woorden alleen gedeclareerde labels geeft; een
  gedeclareerd label met "en" wordt eerst heel gelezen. Reden voor de resolver en niet de grammatica: een label met
  "en" blijft schrijfbaar, en alleen wat nu niet oplost verandert.
- Getuigen (rood-eerst, `DimensievouwTest`): `{voorjaar en najaar}` → `[voorjaar, najaar]`; een label `eb en vloed`
  blijft heel in `{eb en vloed en doodtij}` → `[eb en vloed, doodtij]` en in `{eb en vloed, doodtij en springtij}`.
- Inertheid, gemeten tegen de frontend van vóór de fix (gebouwd uit `a58134cc` in /tmp): 86 corpusbronnen byte-gelijk
  (document en uitvoer); de casusdump uit 45 toka-fragmenten byte-gelijk vóór, na en aan de pin; de volledige casus
  (47 bronnen) geeft byte-gelijke uitvoer (rc 1 door de twee holdouts). In de bronnen: 1 corpusbron met `{` (mijn
  `seizoenvouw`, drie labels), 0 toka-bronnen met een dimensie. De oude proef: `maximum ['voorjaar', 'najaar']`,
  verifier PASS. De NOTE van `jutterswet-seizoenvouw` beweerde dat die vorm niet oplost; bijgewerkt.

#### Gate-regels ronde 3 (elk los na `eval $(opam env)`, op de eindkop `e87d6064`)
- `make test`: 683 tests, 683 geslaagd (was 676 op de basis); toka-suite 27 groen.
- 1: `check-self-contained: PASS`
- 10: `check-regelspraak: PASS — JUnit suite green (683 tests, floor 173); 86 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `drools-jar: rebuilt (2 source path(s) newer than the jar, …; 2s)`; `check-toka-parity: PASS`;
  `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: …); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) …`; `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum)`;
  `differential replay: 29 toka cases …` (ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED).

#### Open punten na ronde 3 (vervangen A en B van ronde 2; C blijft)
- D. Nog Nederlandse resolvermeldingen met een verifier-tegenhanger, buiten deze opdracht (die noemde eenheden en de
  niet-numerieke clamp): familie bij toekennen (verifier `a <soort> value is assigned to a <soort> target`),
  niet-numerieke operanden bij rekenen en ordenen (`operand of a numeric operator is …`, `ordering operand is …`),
  `tot de macht` met eenheid (`power operands must be unitless`), datumprecisie (`equality across date precisions`)
  en het keuzetypedomein (de WF-023-domeinzinnen).
- E. (gesloten in `e87d6064`) Een niet-numerieke basis in een begrenzing met twee grenzen kreeg de clampzin twee
  keer, één keer per grens (gemeten); nu één keer, zoals de verifier (getuige in `EenheidZinTest`).

---

### Ronde 2 — na de rebase op `card/k7-unie` (nieuwe kop **`aaeef0ac`**)

De hoofdsessie rebasete de branch op de unie (basis met toka-welgevormd en een IR-SPEC-update); daarop drie
nieuwe commits, één per punt:

- `6a5e6f34` toka: herpin na de rol op de objectcreatie en de clamp-waarschuwing (mandaat voor `toka/` verleend)
- `ce8347dc` resolver: een zelfgetypeerde creatie zonder rolnaam wordt geweigerd (open punt 6)
- `aaeef0ac` frontend: WF-113 (a) en de clamp-eenheid in de zinnen van de verifier (open punt 8)

#### (1) De toka-herpin, opnieuw gemeten
- `toka/casus/ir.json`: her-emissie door de frontend zoals gate 11 (45 fragmenten, holdouts toka-23 en toka-45
  buiten), +11 regels, −0: 5 voor de `connectingRole` van de ene objectcreatie, 6 voor één WARNING-diagnostic
  (`diagnostics` 4 → 5). Byte-gelijk aan de patch van ronde 1 (de casus was in de basis niet veranderd).
- Slices: alleen `toka/cases/scenario-toka-28-vastgestelde-contingent/ir.json` draagt de creatie; die slice
  draagt in de nieuwe basis een startpuntbepaling (+16/−1 t.o.v. ronde 1), dus de binding is opnieuw gezet via de
  gate-2-serialisator (rondgang vóór en na gecontroleerd): +5 regels. Geen andere slice geraakt.
- Welgevormdheidsbeen: 28 slices verifier-schoon, slice 28 `verifier: PASS`, toka-24 precies op zijn pin
  `WF-099 /rules/rules/0/versions/0/value/base/minimum`. De WARNING staat alleen in de casusdump, die buiten dat
  been valt; de 18 verifierrapporten van de casusdump hebben na de herpin dezelfde regels en pointers
  (1 × WF-022, 6 × WF-026, 8 × WF-034, 1 × WF-045, 1 × WF-079, 1 × WF-099).
- `toka/README.md`, "Deliberate divergences": een regel "2026-09-25 — a creation writes the role its sentence
  names, and a one-sided clamp unit draws a WARNING", met alleen paden, pointers en tellingen.

#### (2) Zelfgetypeerde creatie die het type noemt: ERROR
- Vóór: `Een Wachter heeft een Wachter met …` over `opvolging` ging zonder binding, verifier PASS, ref/clingo
  `MISMATCH: W1.opvolgerrang = 0, expected 2`.
- Na: de resolver weigert (`RuleChecker.objectcreatie`, helper `zelfgetypeerdeKoppelBlokkade`) als de zin geen rol
  bindt, het subject het eigen type maakt, en de typenpaar-afleiding niet koppelt: een tweerollig feittype waarvan
  het type beide rollen vult, of meer dan één kandidaat (de afleiding van `ref`'s `connecting_pair`). Eén wederkerig
  feittype koppelt wel, en zonder feittype is de creatie bewust koppelloos: daar geen melding (JUnit).
- Gemeten: de proef rc 1, geen document; 84 rgs-goldens byte-gelijk; toka-casus byte-gelijk aan de nieuwe pin.
- Weigerzin (ERROR, Nederlands producentproza: de lezers hebben voor deze vorm geen zin, want zij weigeren niet):
  `objectcreatie '<naam>': een <T> maakt een <T>, en de zin noemt geen rol die het nieuwe object bindt; dan komt de koppeling uit het typenpaar, en die afleiding koppelt bij een zelfgetypeerd paar niets (RUNTIME-SEMANTICS ch. 5.5). Noem de rol: 'Een <T> heeft een <rol> met …', met als <rol> '<rol>' of '<rol>' (feittype '<feittype>')`
  (meerdere feittypen gescheiden door ", "). Gemeten op de ketenproef:
  `objectcreatie 'werf de opvolger': een Wachter maakt een Wachter, en de zin noemt geen rol die het nieuwe object bindt; dan komt de koppeling uit het typenpaar, en die afleiding koppelt bij een zelfgetypeerd paar niets (RUNTIME-SEMANTICS ch. 5.5). Noem de rol: 'Een Wachter heeft een <rol> met …', met als <rol> 'voorganger' of 'opvolger' (feittype 'opvolging')`

#### (3) De twee resterende Nederlandse producentweigeringen, nu in de lezerszinnen
- WF-113 (a) (`IrEmitter.emit`): per arm de zin van de verifier (`severity_refusal` en de message- en line-arm van
  `wf113` in `spec/verifier/lib/checks.ml`), op de pointer waar de verifier haar meldt, met de geweigerde diagnose
  erachter (twee bestaande tests eisen dat de ERROR-tekst zelf in de weigering staat):
  `IrEmitter: WF-113 (a) at /diagnostics/<i>/severity: severity error is not a wire severity: an error means the producer refused to write the document, so a document that was written carries none — <diagnose>`
  en idem met `severity info is not a wire severity: the list is the account of what stayed unresolved, and info is tooling talk`,
  `diagnostic without a severity: the one wire severity is warning`,
  `…/message: diagnostic with an empty or blank message: an entry that says nothing accounts for nothing`,
  `…/line: diagnostic with line <n>: a line is a source position, and 0 is the value for none (a hand-built document, or a producer warning without a token)`;
  meerdere gebreken gescheiden door "; ".
- Tweezijdig onverenigbare clamp-eenheid (`Typering.begrenzing`, ERROR): `clamp minimum and base have incompatible units` /
  `clamp maximum and base have incompatible units` — dezelfde WF-099-zin als de eenzijdige WARNING; was
  `begrenzing: eenheden 'EUR' en 'kg' zijn niet in elkaar om te rekenen`.

#### Gate-regels ronde 2 (elk los na `eval $(opam env)`; jar één keer, door gate 12)
- 1: **ROOD, niet door deze kaart**: `FAIL: TOKA material in artifact zone 'spec/IR-SPEC.md' (rights policy, corpus/README.md; the sole exception is the isolated toka/ zone)`
  op 11 regels (3282, 3283, 4680, 4681, 5016, 5017, 6071, 6072, 7340, 7341, 8716). Gemeten: 0 treffers op `20e508d5`,
  11 vanaf basiscommit `1903c4b7` ("IR-SPEC: gate 11 haalt de pariteitsslices nu door de verifier"); deze branch
  raakt `spec/` niet.
- 10: `check-regelspraak: PASS — JUnit suite green (639 tests, floor 173); 84 rgs golden(s) re-emit byte-identically (floor 80)`
- 12: `check-drools: PASS`; `played 199`; `no-case 3`; `channel floor 55 …; played matches the pinned 199`;
  `DRL-text branch played 175 (… pin 175)`; `DRL-text floor 79 …`; `JUnit 776 tests, floor 239`
- 11: `check-toka-parity: PASS`; `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-… toka-45-…); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) metaschema-valid + byte-round-trip`; `well-formedness: 28 replay slice(s) verifier-clean, floor 28 (verifier holdouts: toka-24 WF-099 /rules/rules/0/versions/0/value/base/minimum)`;
  `differential replay: 29 toka cases …, ZERO silent divergences` (ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)
- JUnit 633 → 639 (+3 CreatieRolTest, +2 DiagnoseWeigeringTest, +1 KlemEenheidTest).

#### Open punten na ronde 2 (vervangen punten 1, 6 en 8 van ronde 1; de rest blijft staan)
- A. Gate 1 is rood op de basis: 11 regels in `spec/IR-SPEC.md` bevatten het woord dat de TOKA-struikeldraad zoekt
  (o.a. "toka-24" in de WF-099-rij). Beschermd pad en buiten deze kaart; de hoofdsessie moet die regels herformuleren
  zonder dat token (bv. verwijzen naar `toka/README.md` is ook een treffer; alleen een omschrijving zonder het woord
  gaat door de struikeldraad).
- B. Nog Nederlandse resolvermeldingen met een verifier-tegenhanger, niet in deze kaart: de niet-numerieke
  begrenzing (`begrenzing is alleen toegestaan op een numerieke waarde, kreeg …`, `begrenzing vereist een numerieke
  grens, kreeg …`; verifier `clamp <n> is <kind>, must be numeric`) en de eenheidsmeldingen van rekenen, vergelijken
  en toekennen (`… eenheden '…' en '…' zijn niet in elkaar om te rekenen`; verifier WF-035, WF-036, WF-023).
- C. De clamp-melding voor het eenzijdige paar blijft een WARNING zolang de toka-casus de vorm draagt.

---

### Ronde 1 (hashes na de rebase)

### Stand

- Branch `card/frontend-emitterrestanten`, worktree (worktree emitter), basis main `20e508d5`,
  claim `5627810f`, kop **`79c22c1e`**. Niets gepusht of gemerged; worktree schoon.
- Commits (oud naar nieuw):
  - `d682f11a` emitter: de dagentelling en de concatenatie dragen hun vaste type (U1)
  - `ebc0c6e2` emitter: de dimensievouw gaat de wire op, zonder elementtype (U2)
  - `f13b4397` objectcreatie: de rol die de zin noemt wordt de connectingRole (U3)
  - `db6cd3df` emitter: een brontekst die geen lezer aanneemt, in de zin van de lezers (U4)
  - `9b4a2e55` resolver: een eenzijdig eenheidspaar in een begrenzing krijgt de verifierzin (clamp-restant)
  - `4a3ca392` golden: de dagentelling uit de RegelSpraak-bron (U1)
  - `2b24e148` golden: de dimensievouw uit de RegelSpraak-bron, alleen de verifier (U2)
  - `6a2caed4` goldens: de connectingRole uit de bron, en de zelfgetypeerde creatie (U3)
  - `e5d3b6ef` gates 7 en 12: pins voor de nieuwe goldens (**voorstel**, beschermd pad)
  - `79c22c1e` emitter: de klassebeschrijving noemt de rol op de objectcreatie (alleen javadoc)
- Tellingen: JUnit 618 → 633 (+15: BuiltinTypeTest 2, DimensievouwTest 3, CreatieRolTest 4, BrontekstWeigeringTest 2,
  KlemEenheidTest 4); rgs-goldens 81 → 84; gate 5: 649 cases; gate 12: played 197 → 199, DRL-tekst 173 → 175, no-case 2 → 3;
  Drools-backend-suite 776 groen.
- Aangeraakte bestanden buiten tests/corpus: `emit/IrEmitter.java`, `ir/ResolvedObjectCreatie.java`,
  `model/RegelSpraakModel.java` (één methode `benoemdeRolBinding`), `resolver/RuleChecker.java` (alleen `objectcreatie`),
  `resolver/ExpressieBouwer.java` (alleen de begrensde tak en `meldBegrenzing`), `resolver/Typering.java` (alleen javadoc).
- Drools-jar: één keer gebouwd met `mvn -o -B -f backends/drools/pom.xml package` (776 tests) voor de metingen; gate 12
  bouwde daarna zelf (zijn eigen ontwerp); gates 8 en 13 namen hem vers.

### Per restant: meting vóór en na

#### U1 — `type` op `dayCount` en `concatenation`
- Vóór (proef (buiten de repo),concat}.rgs`): beide `"type": null`; verifier:
  `WF-034 FAIL /rules/rules/0/versions/0/value/type: value node without a static type`.
- Oorzaak: `ExpressieBouwer.functieType` geeft `CONCATENATIE` en (via `default`) `AANTAL_DAGEN_IN` Onbekend.
- Na: de emitter schrijft het vaste type (`IrEmitter.functieType`): `dayCount` → `{Number, whole true, decimals 0,
  sign nonNegative}` zonder eenheid (RS ch. 2.3 rij "aantal" en ch. 3.8; alle 13 dayCount-knopen in het corpus dragen
  precies dit type), `concatenation` → `{Text}` (IR-SPEC ch. 4.3 "text fold"; `ref` rekent `Arith.concat` → Text).
  Een afwijkende resolverclaim weigert de emitter (onbereikbaar met de huidige resolver). Verifier op beide proeven: PASS.
- Gate 10: geen rgs-golden emitteerde anders (0 concatenation-knopen in het corpus, 0 dayCount in een rgs-golden).
- Extra golden `jutterswet-dagentelling-rgs` (brontweeling van het anker; zonder `source` structureel gelijk).
- Concatenatie-uitvoering gemeten (case J1 "Jan"+"Duin"): ref `oracle: PASS (2 expectations)`; clingo
  `TRANSLATION FAULT: a text/enum/boolean operand here is an own-subject attribute read or a literal (no corpus case for more)`;
  Drools exec `EXCLUDE transpiler SKIP: regel: volledige naam van een jutter — regel (waarde buiten scope): …`, DRL
  `EXCLUDE drl-transpiler MARKER: … TODO aggregatie CONCATENATIE: …` → geen concatenatie-golden mogelijk.

#### U2 — `DimensionSelection`
- Vóór: de weigering kwam uit `IrEmitter.vouwbereik` (toen r. 1537), niet uit `facetWeigering` (r. 805):
  `IrEmitter: de sum-vouw 'de som van zijn vondstgewicht over alle seizoenen' heeft geen spelling in uchar-ir-v1: haar bron is het eigen attribuut van het onderwerp en geen instantiebron`.
  Oorzaak: de resolver noteert het onderwerp als elementtype van de vouw; een geschreven `elementType` op een vouw met
  één argument eist een bereik (schema, IR-SPEC ch. 4.3).
- Na: een aggregatie met een dimensieselectie gaat zonder `elementType` en zonder bereik, met één eigen lezing als
  argument en `dimensionSelection {dimension, labels}` in gedeclareerde volgorde. Verifier PASS op som, minimum en
  maximum en op alle vier schrijfwijzen (`over alle <mv>`, `vanaf a t/m b`, `over de <mv> vanaf a t/m b`,
  `over de <mv> in {a, b en c}`).
- Een bestaande test (`SubtypeDialectTest.een_geerfd_gedimensioneerd_attribuut_draagt_de_gedeclareerde_spelling`)
  pinde de oude weigering; de bewering pint nu de gedeclareerde spelling `betaalde belasting` op de wire (verifier PASS).
- Golden `jutterswet-seizoenvouw`: alleen verifier (NOCASE), vier regels. Gemeten per uitvoerder (twee kladcases):
  ref `EVALUATION FAULT: dimension selection not implemented in this card`; clingo
  `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`; Drools zonder invoer `PLAYED 1`
  in beide takken (alle vier vouwen leeg), met scalaire invoer
  `EXCLUDE dimensioned attribute input 'vondstgewicht' (§3.6, a Map bean member) is not carried by the v0 backend`
  in beide takken. `uchar-case-v0` heeft geen invoervariant voor cellen.

#### U3 — `connectingRole` op de objectcreatie
- Vóór: geen enkele creatie droeg het veld. Tweeling van `anchor/jutterswet-wachtersketen` uit de bron: verifier PASS,
  en ref/clingo `MISMATCH: W1.opvolgerrang = 0, expected 2` / `= [0]`; case2 `W2.opvolgerrang = 0, expected 5`.
- Na: `ResolvedObjectCreatie` kreeg de component `verbindendeRol` (plus de oude 6-argumentenconstructor, die de
  Drools-lader gebruikt); `RegelSpraakModel.benoemdeRolBinding` leest de rol op de kandidaat waarop
  `resolveObjecttype` het type vond, alleen als gedeclareerde rolnaam of -meervoud, uniek over het paar;
  `RuleChecker.objectcreatie` bewaart haar; de emitter schrijft haar na `validity`. Een zin die het type noemt schrijft
  geen rol. De twee-feittypen-vorm kiest de genoemde rol (JUnit).
- `jutterswet-wrakboeking-rgs` en `jutterswet-wrakboekwaarde`: +5 regels elk (alleen de binding), cases byte-gelijk,
  NOTE's bijgewerkt; drager en tweeling zijn zonder `source` nu gelijk (de diff-tabel van één rij is leeg); ook de NOTE
  van de drager `jutterswet-wrakboeking` (die zin werd onwaar). Drools: `PLAYED 1` in beide takken, met binding.
- Tweeling `jutterswet-wachtersketen-rgs`: zonder `source` gelijk aan het anker, `connectingRole {opvolging, opvolger}`;
  ref en clingo PASS op beide ankercases; Drools `PLAYED 2` in beide takken.

#### U4 — meldingstaal
- Vóór: `IrEmitter: een knoop met een lege brontekst heeft geen spelling in uchar-ir-v1: een snippet is nooit leeg, en een knoop zonder brontekst draagt geen source`
  en `IrEmitter: WF-113 (b) — een Unresolved-marker zonder brontekst ('<s>') kan door geen warning verantwoord worden`
  (één zin voor null, leeg en blanco).
- Na: zie "Definitieve weigerzinnen". Een lege snippet krijgt ook op een marker de laadzin (zoals elke lezer, U10 van L5);
  twee bestaande beweringen in `IrEmitterTest` (de Nederlandse zin; de lege markersnippet onder "WF-113 (b)") volgen.

#### Clamp-restant van de hoofdsessie — een eenzijdig eenheidspaar in een begrenzing
- Vóór (eigen proeven (buiten de repo)): `met een minimum van 0 €`, `maximum van 100 €`, beide, en een
  eenheidloze attribuutgrens over een €-basis: frontend zonder diagnostic; verifier
  `WF-099 FAIL …/value/minimum: clamp minimum and base have incompatible units` (resp. `maximum`). Eenheidloze
  literal, adoptie (`0` over €) en gelijke eenheid: PASS. Tweezijdig onverenigbaar (€/kg): de frontend weigerde al
  (`begrenzing: eenheden 'EUR' en 'kg' zijn niet in elkaar om te rekenen`).
- Na: `ExpressieBouwer.meldBegrenzing` meldt de verifierregel: precies één kant draagt een eenheid en geen van beide
  kanten is een eenheidloze getalliteral → WARNING in de verifierzin. Ook de percentagebasis met eenheidloze
  attribuutgrens: WARNING en WF-099; met literal 0: geen melding, PASS.
- Ernst: als **ERROR** schreef de frontend de toka-casus niet meer (gemeten: `--ir` rc 1, geen document, 1 clamp-zin),
  en `CasusTest` eist geen andere ERRORs dan de twee quirks → gebouwd als **WARNING**, zoals voorgeschreven.
- Proefdocumenten van de toka-bouwer: `klem-eenzijdig` → WARNING en WF-099; `-eenheidloos`, `-adoptie` → geen melding,
  verifier PASS.
- Gate 10: geen rgs-golden verandert. Gate 11: **niet** byte-gelijk — de casusdump krijgt één WARNING-diagnostic
  (6 regels, `diagnostics` 4 → 5; het bericht is de vaste verifierzin).

### Goldens en hun verwachtingen

- `jutterswet-dagentelling-rgs` (nieuw; cases byte-gelijk aan het anker): case1-voorbeeld J1.borgdagen 2024-01 = 23,
  2024-02 = 13, jaardagen 2024 = 36, consistent; case2-jaargrens borgdagen 2024-12 = 2, 2025-01 = 2, jaardagen 2024 = 2,
  2025 = 2; case3-leeg beide tijdlijnen leeg; case4-input-blijft borgdagen 2024-03 = 99 blijft. ref, clingo, Drools
  (beide takken, PLAYED 4). case3 en case4 overleven een no-op (redenen van het anker).
- `jutterswet-wachtersketen-rgs` (nieuw; cases byte-gelijk): case1 W1.opvolgerrang 2, inconsistent `rangdetector`;
  case2 W2 5, W3 0, inconsistent `rangdetector` bij W3. ref, clingo, Drools (PLAYED 2 beide takken); geen no-op-overlever.
- `jutterswet-seizoenvouw` (nieuw; geen case; gate 5 meldt hem als niet afgespeeld).
- `jutterswet-wrakboeking-rgs` (her-emissie): W1/W2 bergingsvergoeding 10.00/2.50, consistent — ongewijzigd.
- `jutterswet-wrakboekwaarde` (her-emissie): W1/W2 bergingsvergoeding 10.00/2.50, boekwaarde 11.00/3.50 — ongewijzigd.

### Gate-uitvoerregels (worktree, gedraaid op `e5d3b6ef`; na `79c22c1e` gates 10 en 1 opnieuw: PASS)

- 1: `check-self-contained: PASS`
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 296 file(s) validate and round-trip, floor 192)`
- 3: `check-verifier: PASS (296 golden clean; 433 negatief rejected for their declared rule, floor 350; 275 on their declared arm (expect.txt), floor 260)`
- 4: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 692 case file(s) validate)`
- 5: `check-oracle: PASS (ref suite 994 worked examples, floor 489; 649 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)`
- 6: `check-clingo: PASS (backend suite 1733 checks, floor 83; 649 cases replay exactly through clingo — ref = clingo over the corpus)`
- 7: `check-noop: PASS (609 cases refute a do-nothing executor; 40 pinned survivors pin defeasible standing)`
- 8: `check-rood: Drools leg PASS (11 fault-pinned, 32 not-carried with reason)` en `check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences)`
- 10: `check-regelspraak: PASS — JUnit suite green (633 tests, floor 173); 84 rgs golden(s) re-emit byte-identically (floor 80)`
- 11 (worktree): **ROOD** — `DIFF: casus (re-emitted IR diverges from the pinned toolchain dump)` /
  `check-toka-parity: FAIL — casus: the frontend output diverges from the pinned dump`
- 11 (wegwerpkloon in /tmp met de herpin-patch toegepast): `check-toka-parity: PASS`; `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`;
  `schema: 29 replay slice(s) metaschema-valid + byte-round-trip`; ref `29 PASS / 0 diverge`, clingo `29 PASS / 0 loud translation-fault / 0 silent diverge`, Drools `29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR`
- 12: `check-drools: PASS`; `played 199`; `no-case 3`; `channel floor 55 non-PLAYED goldens pinned by name; played matches the pinned 199`;
  `DRL-text branch played 175 (… pin 175)`; `DRL-text floor 79 non-PLAYED goldens pinned by name`; `JUnit 776 tests, floor 239`
- 13: `check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`
- Naamtabel: `python3 spec/proposals/v1-naamtabel-toets.py` rc 0 (`v1-naamtabel-toets: OK`). Gates 9 en 14 niet gedraaid (U6).

### Gewijzigde scriptregels (beschermde paden — voorstel, commit `e5d3b6ef`)

`scripts/check-drools.sh`:
- `EXPECTED_CHANNEL` + `"jutterswet-seizoenvouw|NOCASE|a golden with no case file — nothing to replay"`
- `DRL_CHANNEL` klasse (E) + `"jutterswet-seizoenvouw|NOCASE|a golden with no case file"`; kop "seventy-eight" → "seventy-nine"
- `EXPECTED_PLAYED=197` → `199`, met redenblok "197 -> 199 (2026-09-25, card frontend-emitterrestanten)"
- `EXPECTED_DRL_PLAYED=173` → `175`, met redenblok "173 -> 175"
- twee zinnen in de blokken 188 -> 192 en 194 -> 195 van tegenwoordige naar verleden tijd ("carried no connectingRole then …",
  "as they did then for the twin …"), omdat ze met U3 onwaar werden.

`scripts/check-noop.sh`: redenblok voor `jutterswet-dagentelling-rgs` en twee `SURVIVORS`-regels
(`…/jutterswet-dagentelling-rgs/case3-leeg.json`, `…/case4-input-blijft.json`).

### Definitieve weigerzinnen (letterlijk)

- Lege snippet (`IrEmitter.bronrecord`, ook op een marker):
  `IrEmitter: a source snippet is never empty: a node without source text carries no source`
  — de zin na "IrEmitter: " is gelijk aan `Loader.empty_snippet_refusal` (verifier, ref, clingo) en `IrLoader.SNIPPET_LEEG` (Drools).
- Marker zonder bron: `IrEmitter: WF-113 (b) — Unresolved marker without a source: no diagnostic message can contain it, so it cannot be accounted for`
  — gelijk aan `marker_without_source` (verifier) en `IrLoader.MARKER_ZONDER_BRON`.
- Marker met blanco snippet: `IrEmitter: WF-113 (b) — Unresolved marker whose source snippet is blank: blank counts as absent, since nearly every message contains it and a message about another marker would account for this one`
  — de blanco-arm van WF-113 in de verifier (geen lader draagt die arm).
- Clamp (WARNING, resolver): `clamp minimum and base have incompatible units` / `clamp maximum and base have incompatible units`
  — de WF-099-zin van de verifier, zonder regelnummerstaart.
- Nieuwe producentzinnen zonder lezer-tegenhanger (in-memory velden die de wire niet draagt; Nederlands zoals de
  andere emitterweigeringen; met de huidige resolver onbereikbaar):
  `IrEmitter: de <soort>-aanroep '<snippet>' draagt het type <X>, en de spec legt het type <Y> vast` en
  `IrEmitter: de <soort>-vouw '<snippet>' heeft geen spelling in uchar-ir-v1: haar dimensieselectie loopt over een attribuut van <X>, en een dimensievouw leest het eigen attribuut van <Y>`.

### Wat aan de kaart niet klopte (gemeten)

1. U1 verwijst naar "IR-SPEC ch. 4.2 (de BuiltinCall-tabel met de resultaattypen)": de BuiltinCall-tabel staat in
   ch. 4.3 en noemt geen resultaattypen; RS ch. 2.10 ook niet. De afleiding komt uit RS ch. 2.3 (rij "aantal") en
   ch. 3.8, IR-SPEC ch. 4.3 ("concatenation (text fold)") en het corpus (13 dayCount-knopen, één type).
2. U2 "r. ~805 weigert (`facetWeigering`)": de weigering kwam uit `vouwbereik` (elementtype = onderwerp zonder bereik).
3. U2 "golden met handgerekende cases (ref ≡ clingo)" en "Drools … `EXPECTED_CHANNEL`-regel": onmogelijk — ref en
   clingo voeren de dimensieselectie niet uit en `uchar-case-v0` kan geen cellen leveren; de golden is alleen-verifier
   en staat als NOCASE (niet EXCLUDE) in beide lijsten.
4. (c) "de toka-casus schrijft geen van deze vormen": de casus heeft 1 objectcreatie (1 versie) waarvan de zin de rol
   noemt, en de slice `toka/cases/scenario-toka-28-vastgestelde-contingent/ir.json` dezelfde; met U3 krijgt zij
   `connectingRole` (+5 regels). Gemeten: de geschreven rol is gelijk aan de unieke typenpaar-afleiding (niet
   zelfgetypeerd, 1 verbindend feittype), dus geen betekenisverschil — wel een bytediff in gate 11. `toka/` valt
   buiten de may-touch-set; de precedenten in `toka/README.md` hernemen de pin in dezelfde kaart.
5. (d) "alle veertien gates": U6 noemt 1–8 en 10–13; 11 is rood om punt 4 en de clamp-WARNING.

### Open punten

1. **toka-herpin (nodig voor gate 11)**: patch (buiten de repo) (16 toegevoegde regels,
   0 weggehaald; `git apply --check` past op de branch): `toka/casus/ir.json` = her-emissie door de frontend
   (+11: 5 `connectingRole`, 6 één WARNING-diagnostic) en de slice van scenario 28 (+5, dezelfde binding, met de
   gate-2-serialisator). Met de patch: gate 11 PASS (kloon). Ontwerptekst voor `toka/README.md`, "Deliberate divergences":
   "**2026-09-25 — a creation's named role, and a warning on a one-sided clamp unit.** Executed by the card
   frontend-emitterrestanten. The casus's one object creation names its created object by a role, so its version
   gained `connectingRole` (five lines) — the binding the type pair derives on this document too (one connecting fact
   type, not self-typed), so nothing it links changes; the slice `cases/scenario-toka-28-vastgestelde-contingent`
   carries the same creation and gained the same five lines by hand. The casus's one clamp bounds a unitless value by
   a unit-bearing literal, which the verifier refuses (WF-099); the frontend now appends a WARNING in the verifier's
   sentence (six lines, `diagnostics` 4 → 5), and no slice moved for it. The original toolchain writes neither, so for
   these two nodes the comparison proves "frontend == the recorded state"."
2. De dimensievouw uitvoerbaar maken (caseformaat met cellen, arm in `ref` en in de ASP-backend); dan verhuist
   `jutterswet-seizoenvouw` naar een uitvoerend document met handgerekende cases.
3. `{a en b}` (twee labels, geen komma) parseert als één label, omdat `dimensieWaardeWoord` EN toelaat; de selectie
   blijft onopgelost (WARNING) en de emitter weigert de vouw. Grammatica/resolver, niet deze kaart.
4. IR-SPEC WF-040-indexrij (beschermd): de selectie-arm heeft nu een positieve getuige, `jutterswet-seizoenvouw`.
   Voorstel: bij "positive" toevoegen "and `jutterswet-seizoenvouw` (the selection arm, verifier-only)".
5. CONSTRUCTS rij 28 (registerzin, voorstel voor de hoofdsessie): "Since the card frontend-emitterrestanten the
   RegelSpraak frontend emits the dimension fold (sum, minimum, maximum, no element type); golden
   `jutterswet-seizoenvouw`, verifier-only — no executor and no case format carry it yet."
6. Een zelfgetypeerde creatie die het TYPE noemt (`Een Wachter heeft een Wachter met …`) gaat zonder binding,
   verifier PASS, en ref/clingo leggen stil geen koppeling (gemeten opvolgerrang 0 i.p.v. 2). Kandidaat: een
   producentwaarschuwing of een verifierregel voor het zelfgetypeerde paar zonder binding.
7. De losse concatenatie: alleen `ref` voert haar uit; clingo en beide Drools-takken weigeren (zinnen onder U1). De
   typefix is alleen door JUnit gepind.
8. Nog Nederlandse producentweigeringen met een lezerzin: WF-113 (a) in `IrEmitter.emit`, en de tweezijdige
   clamp-eenheid in `Typering.begrenzing` (`begrenzing: eenheden '…' en '…' zijn niet in elkaar om te rekenen`, waar de
   verifier `clamp minimum and base have incompatible units` zegt). Niet gevraagd in deze kaart.
9. De clamp-melding is een WARNING om de toka-casus; verdwijnt de zone (of wordt toka-24 anders behandeld), dan kan zij
   een ERROR worden zoals andere WF-weigeringen.
10. Gelijktijdige bouwer (dialect-opgave-boven-berekening): mijn resolverwijzigingen staan in `RuleChecker.objectcreatie`,
    `RegelSpraakModel` (nieuwe methode direct na `resolveObjecttype`), `ExpressieBouwer` (begrensde tak en
    `meldBegrenzing`) en de javadoc van `Typering.begrenzing`; de hoofdsessie rebaset serieel.
11. Aangepaste bestaande tests (de kaart vroeg om de gedragswijziging die zij pinden): `IrEmitterTest` twee beweringen
    (Nederlandse lege-snippetzin; lege markersnippet onder "WF-113 (b)"), `SubtypeDialectTest` één bewering (de oude
    weigering van de intervalvouw → de gedeclareerde spelling op de wire).

## Lens en fixronde (2026-09-25, hoofdsessie)

Lens (frontend-duo, unie 36750511, eigen meting in kopieën): EERST FIXEN. Goedgekeurd op eigen meting: de typen op `dayCount` (`Number, whole, 0, nonNegative`) en `concatenation` (`Text`) tegen RS ch. 2.3 en de verifier; de `DimensionSelection`-emissie conform IR-SPEC ch. 4.3, eerlijk als NOCASE vastgelegd; `connectingRole` (wrakboeking-rgs en wrakboekwaarde elk +5 regels, cases byte-gelijk, tweelingen zonder `source` gelijk aan de dragers, de cases byte-gelijk aan de ankers); de zelfgetypeerde creatie die het type noemt: precies één ERROR; de clamp (eenzijdig WARNING met document, tweezijdig ERROR, dezelfde WF-099-zin; eenheidloze literal PASS); de toka-herpin (unie-emissie byte-gelijk aan de casusdump, +11/−0; slice 28 +5; gate 11 28/29); de rondes 5 en 6 (frontend-ERROR precies waar de unieverifier FAIL geeft, dezelfde zin: WF-033 alle versies, WF-032, WF-037, WF-043); 62 Engelse fragmenten teruggezocht in spec/verifier/lib, twee ontbraken (B1).

Blokkerend voor deze kaart: B1 — twee frontendzinnen weken af van de unieverifier (de datumrekenzinnen, verschoven door de kaartloze restpunten-rondes ná de laatste emitterronde; geen gate vergelijkt frontendzinnen met de verifier); B2 — de DIVERGENCES-lijst "Producer refusals without a reader sentence" was op drie punten onwaar geworden (de unieverifier kreeg zinnen voor wortel, absolute waarde en het literalteken tegen het doel); B5 — vijf bestaande testbeweringen gewijzigd buiten de gemandateerde lijst (TyperingTest r. 41 en 56, VariabeledeelLoweringTest r. 363, CasusTest in de pariteitszone r. 55–58). Advies: A2 (het getal 18 in toka/README en check-toka-parity.sh: onder de unieverifier 15 — bij de toka-fixronde), A3 (dubbele slotalinea in de NOTE van jutterswet-strandmelding), A4 (twee vormen van main die de frontend schreef en de verifier weigert: een tijdsduur in een niet-tijdseenheid, WF-038, en een dagdatum plus een eenheid kleiner dan een dag, WF-035), A5 (de eenzijdige clamp-WARNING in DIVERGENCES), A6 (de zelfgetypeerde creatie in de lijst), A7 (IrEmitter r. 1212 de lezerszin), A10 (kortere commitonderwerpen — voor de toekomst).

Besluiten van de hoofdsessie: B5 — mandaat achteraf voor de vijf beweringen, mits elk het gemandateerde nieuwe gedrag pint (de Engelse lezerszinnen van U4, de clamp-WARNING op de casusdump) en niets uitschakelt; de fixer meet dat per bewering. B1 krijgt naast de fix een JUnit-guard die elke Engelse lezerszin van de frontend terugzoekt in spec/verifier/lib, zodat drift zoals deze voortaan rood wordt. A4 wordt gefixt (weigeren in de verifierzin).

Fixronde (branch fix/lens-emitter, de emitter-bouwer): tien commits van de emitter-bouwer en dertien van een verse fixer voor de resterende zinnen, alle op de unie. B1: de twee datumrekenzinnen volgen de verifier (`date ± duration: the duration unit '<abbr>' is neither a recognised time unit nor a declared day kind`, `… (the date stands on the left; date arithmetic does not commute)`), TyperingZinTest en TyperingTest onder mandaat mee; de guard `LezerszinGrensTest` eist dat elk Engels fragment van een weigering in resolver/ of IrEmitter letterlijk in de literals van spec/verifier/lib staat (één uitzondering met reden: de IfThenElse-weigering van de emitter, waar geen lezer bij hoort; na de zinnenronde 153 fragmenten, vloer 149; een mutatietoets maakt hem rood als de verifier een zin herformuleert). B2: de WF-038-zin voor wortel en absolute waarde (percentage houdt de eigen zin) en de WF-039-zinnen tegen het doel; het teken wordt, gelijk aan de nieuwe verifierlezing, van de waarde gelezen (nul is negatief noch positief). A4: drie vormen die de frontend schreef en de verifier weigerde zijn nu een ERROR in de verifierzin (sub-day eenheid op een dagdatum, de duur in een niet-tijdseenheid, een sub-day duur-eenheid op dagoperanden — de derde vond de bouwer zelf). A3, A5, A6, A7 zoals gevraagd; het regelnummer 0 in de weigering van de volledige periode is de echte regel geworden. B5 gemeten: de drie beweringen buiten de mandaatlijst falen op de frontend van vóór de zinswijziging en pinnen het nieuwe gedrag; CasusTest in de pariteitszone eist nog beide quirks en geen andere ERROR. Zinnenronde: alle veertien structurele weigeringen met een verifierzin spreken die zin na het Nederlandse voorvoegsel (van 65 proeven eindigen 54 letterlijk op de FAIL-zin van de verifier; de elf die Nederlands blijven hebben elk een gemeten reden — de verifier aanvaardt het document, of er ontstaat geen document), `StructuurZinTest` met 31 getuigen, 71 testinstanties in 17 klassen onder mandaat (rood-eerst gemeten), en de dagsoortdefinitie met een begrensd venster of een tweede definitie is een ERROR in de WF-031-zin waar de frontend eerst een WARNING gaf of niets. Op de unie werd de guard vervolgens terecht rood: de kaartloze fixronde liet de verifier de WF-038-duurzinnen de eigen bouwsteen noemen (duration of absoluteDuration) ná de emitterronde; de hoofdsessie spelde de drie zinnen uit de naamtabel, met een getuige voor de absolute tijdsduur. Inertheid: 89 bron.rgs byte-gelijk, de toka-casusdump gelijk aan de pin. JUnit 721 → 759; gates 1, 10, 11 groen op de branches. Bewust open: de schrijfaanwijzingen naast de verifierzin ("neem de regel op in de groep") zijn met de Nederlandse zinnen weggevallen — een patroonbesluit voor de maintainer; de WF-053-zin noemt bij een geschreven keten de directe relatie die de emitter kiest; de verifier meldt bij WF-021 en de meergroepenclausule van WF-050 per paar of groep, de frontend één keer; de randen van de guard (fragmenten van één woord); de datumliteral die geen kalenderdatum is blijft Nederlands; de dimensievouw zonder caseformaat; concatenatie alleen in ref.
