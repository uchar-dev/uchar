# De AANTAL-setbron: de wire weigert de vorm (besluit genomen)

**Maintainersbesluit 2026-09-19: optie (ii), met een expliciete
scheiding van betekenissen.** De huidige AANTAL-setbron-vorm wordt
geweigerd; `AANTAL` blijft conform de normatieve RegelSpraak-spec
§5.8.1 en `RUNTIME-SEMANTICS.md` ch. 3.4a/3.4b INSTANTIES tellen —
drie vondsten blijven drie, ook als één gewicht leeg is. De
wire-vorm mag de twee betekenissen (ledentelling en waardentelling)
niet vermengen. Voor de waardentelling en het gemiddelde komt een
afzonderlijke, normatief gedefinieerde operatie: eigen kaart
`20260919-aantal-gevulde-gemiddelde.md`.

De meting die het besluit droeg (kaart eerste-laatste-setvouw-kloof
§3/§6, door de lens onafhankelijk gereproduceerd): `ref` telt gevulde
waarden, Drools telt leden — 2 tegen 3 zodra één lid het attribuut
mist — de ASP-backend weigert, en er is geen producentroute (de
frontend weigert de natuurlijke zin met haar eigen WF-096-melding).

Uitvoering (route ii): de WF-093/WF-096-exemptie versmalt tot
"arity 1 ∧ een WAARDE-vouwende soort" (AANTAL eruit); `ref`'s
setbron-arm en de Drools-lowering worden onbereikbare code voor
welgevormde documenten; ch. 3.4a/3.4b blijven staan zoals ze staan.

Acceptance: rood-eerst — het negatief per spelling met de
verifier-md5 vóór en ná, plus een arm-exacte mutant; de corpussweep
vóór/ná (verwacht nul beweging: geen golden draagt de vorm); de
"awaiting the maintainer decision"-passages in `spec/IR-SPEC.md`
(WF-096-exemptie-alinea, WF-102-COUNT-bullet),
`spec/RUNTIME-SEMANTICS.md` (ch. 3.4b gevolg 2), `spec/CONSTRUCTS.md`
rij 1 en `spec/ASP-TRANSLATION.md` (catch-all) worden in het
registervoorstel bijgewerkt naar het genomen besluit; alle veertien
poorten groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL
(beschermde paden), `corpus/negatief/**`, kaartverplaatsing.

Out of scope: EERSTE/LAATSTE — beslecht 2026-09-19, route (i)
gebouwd. De nieuwe waardentelling/GEMIDDELDE-vouw — eigen kaart.
`frontends/**` (de WF-096-meldingstekst belooft `aantal` als
vouwsoort en verdient een herformulering zodra deze kaart landt —
dat is een frontend-klusje bij de landing, zoals bij subselectie-4a).

## Result (2026-09-19)

Route (ii) uitgevoerd. De setfence van WF-096 versneed met precies één
soort; de sentinelfence van WF-034 bleef staan zoals zij stond, want v0
spelt een telbron nu eenmaal met de sentinel. Drie negatiefs, alle veertien
poorten groen, nul beweging in het corpus.

### 1. De versmalling, en waarom zij NIET één grens is

De exemptie stond in `spec/verifier/lib/checks.ml` als één gehoiste
booleaan (`source_exempt`, arity 1 ∧ (waardevouw ∨ AANTAL)), gelezen door
twee regels: de sentinelfence van WF-034 en de setfence van WF-096. Hem als
geheel versmallen zou 81 kale wratten in golden + rood + negatief + pariteit
op WF-034 laten vallen — de gewone telbron. Er staan nu dus twee grenzen:

    source_exempt      arity 1 ∧ (waardevouw ∨ AANTAL)   ongewijzigd (WF-034)
    set_source_exempt  arity 1 ∧ waardevouw              nieuw      (WF-096)

Het commentaar bij beide zegt waarom ze in precies één soort verschillen.
De meldingstekst van WF-096 is op BEIDE plaatsen herschreven (de
vouwargument-arm en de gewone-leesarm in `walk_expr`), zodat er één zin
staat over waar een setlezing thuishoort.

### 2. Rood eerst, in volgorde, met de meting erbij

**(1) De negatiefs.** Eén per wire-spelling waarin de vorm zich voordeed:

- `corpus/negatief/wf096-aantal-setbron` — de gemeten vorm: een echt
  getypeerde elementattribuutlezing met `set: true` over een `bronRol`.
  Gebouwd uit `corpus/golden/jutterswet-vondsttelling` door de bron van de
  telling te vervangen door de setbron die de SOM ernaast al droeg.
- `corpus/negatief/wf096-aantal-setbron-wrat` — de SETSPELLING van de rij-1
  wrat: sentinel op het eigen `type`, `set: true`, anker op het elementtype.
- `corpus/negatief/wf096-aantal-setbron-bronpad` — het MEERSTAPSBEREIK van
  ch. 3.4b, gebouwd uit `corpus/golden/anchor/jutterswet-bootberging`.

Twee verdere handgebouwde spellingen zijn gemeten en bewust NIET gepind
omdat ze op dezelfde arm vallen: een bindingloze setbron (WF-048 leidt de
relatie af) en een setbron met een zelfdenoterende `navigatieRol`. Eén arm,
één getuige per spelling, niet per veldcombinatie.

**(2) De huidige verifier accepteert ze.** Binary md5
`441c25d84dc4cc54931ed8a5504b00f5` (bron `spec/verifier/lib/checks.ml` md5
`2f1fdd5dcfdf5b6b7ff16086e0cbf4a8`): alle drie `verifier: PASS`, exit 0. De
md5 is reproduceerbaar — de bouw van HEAD's `checks.ml` in een aparte
werkmap gaf dezelfde binary-md5 terug.

**(3) De versmalling.** Nieuwe binary md5
`3549d7ea72a43cd3164585e7f2955ae2` (bron md5
`eb64e8a643b73731b6da03f009d25963`).

**(4) De weigering, met de juiste citatie.** Alle drie, met ÉÉN fout:

    WF-096 FAIL /resolvedRegels/0/waarde/argumenten/0: set reading on a
    fold operand that is not a VALUE fold's SOURCE: a set reading is
    admissible on the single argument of a value AGGREGATION alone
    (ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads; at a non-aggregation kind
    there is no source at all; and under AANTAL the fold reads the
    CONTRIBUTORS, never a value per contributor, so a set reading there
    names a collection the count does not consume

De twee ongepinde spellingen vallen op exact dezelfde zin.

**(5) Arm-exacte mutant.** `AANTAL` teruggezet in `set_source_exempt` — één
disjunct, verder niets — geeft binary md5
`25aefe6ea348721203a7dd42fe9d30be`. Sweep over golden + rood + negatief +
pariteit (666 documenten), drie binaries, per document vergeleken:

    OUD  → NIEUW   4 verdicts bewegen: de drie negatiefs van PASS naar FAIL,
                   plus `wf083-vouwbron-anker-op-operand` dat één meelopende
                   WF-096-regel krijgt naast het WF-083-verdict dat het houdt.
                   4 documenten veranderen alleen MELDINGSTEKST, geen verdict.
    NIEUW → MUTANT exact diezelfde 4 verdicts terug, verder niets.
    OUD  → MUTANT  NUL verdictverschillen; alleen de 4 herschreven meldingen.

De mutant is dus gedragsmatig de oude verifier: de versmalling raakt precies
de AANTAL-arm en niets anders.

**(6) Corpussweep vóór/ná over ref, clingo en beide Drools-takken.** Nul
beweging, zoals verwacht — geen golden draagt de vorm (gemeten: 0 AANTAL-
setbronnen in golden en rood; de enige in de vier zones zat al in negatief).
Poort 12 en poort 13 leveren vóór en ná een BYTE-IDENTIEKE uitvoer; poort 5
en 6 dezelfde statusregels (539 cases, 643 voorbeelden, 419 checks).

### 3. Wat de uitvoerders op deze drie documenten doen (live nagemeten)

`wf096-aantal-setbron`, met de casus van de golden zelf (J1 heeft drie
vondsten waarvan V3 geen `gewicht` draagt; J3 heeft één zo'n vondst), tegen
de instantietellingen 3 en 1 die de golden pint:

    ref         : MISMATCH J1 = 2, J3 = 0   (telt GEVULDE WAARDEN)
    ASP-backend : TRANSLATION FAULT: expression kind not translated
    Drools      : PLAYED — 3 en 1 (telt LEDEN); tegen ref's antwoorden
                  zegt hij "verwacht 2, werkelijk 3"

De 2-tegen-3 van de vorige kaart is daarmee op één document, in één beurt,
opnieuw gereproduceerd.

`wf096-aantal-setbron-bronpad`, met de ketencasus van de anker-golden (J1
bereikt drie gevulde vondsten, J2 bereikt er twee waarvan één zonder
`gewicht`), tegen de handberekende elementtellingen 3, 2, 0, 0:

    ref         : MISMATCH J2 = 1, verwacht 2
    ASP-backend : TRANSLATION FAULT
    Drools      : EXCLUDE transpiler SKIP (bronPad-stap zonder beanveld)

Over een meerstapsbereik antwoordt dus nog maar ÉÉN uitvoerder, en hij
antwoordt tegen ch. 3.4b gevolg 2 in.

`wf096-aantal-setbron-wrat` is een eerlijke uitzondering en staat zo in zijn
NOTE: alle drie de uitvoerders antwoorden hier, en ze zijn het EENS (3 en 1)
— `ref` matcht de sentinel op zijn eerste vouwbron-arm, vóór en ongeacht
`set`, en de andere twee sleutelen hun telarm net zo. Het veld is daar inert.
Wat deze spelling pint is dus niet een uitvoerderskloof maar de wire: de
sentinel zegt "instanties", `set` zegt "de waarden van de leden". Eén knoop,
twee tegenstrijdige claims — de WF-083 (c)-vraag, en het huis-antwoord is
één betekenis, één spelling (WF-102 (a) trekt dezelfde lijn).

### 4. Wat bewust NIET is gedaan

- `ref`'s setbron-telarm en de Drools-lowering staan ongewijzigd. Ze zijn nu
  onbereikbaar voor een welgevormd document en blijven als tweede
  verdedigingslijn staan — het huispatroon, geen dode tak.
- `frontends/**` niet aangeraakt, en het vervolgklusje is BREDER dan een
  meldingstekst. Twee plekken in `RuleChecker.java` lopen achter op de
  wire: de melding belooft `aantal` als vouwsoort, en de verzameling
  `VOUWSOORTEN` (r. 471–476) houdt `AANTAL` in de frontend-exemptie, met
  een commentaar dat zegt dat "the frontend's exemption and the verifier's
  are the same boundary" — sinds dit besluit onwaar: de frontend is RUIMER
  dan de wire. Gemeten is er geen lek — vier natuurlijke producentzinnen
  weigeren met de eigen WF-096-ERROR vóór emissie, dus de frontend schrijft
  de vorm niet — maar de klus is de verzameling, het commentaar én de
  tekst, niet de tekst alleen.
- `spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt (§5 draagt de
  voorgestelde rijtekst letterlijk).
- Geen nieuw WF-nummer. Het besluit is een versmalling van WF-096's
  exemptie, precies zoals de kaart hem formuleert; een tweede nummer zou
  één grens in twee regels splitsen.

### 5. Registervoorstellen

**Geland in deze tak (beschermd pad, dus VOORSTEL; de kaart geeft het
mandaat, de maintainer merget):**

- `spec/IR-SPEC.md` — de normatieve WF-096-zin (setlezing alleen bij de
  bron van een WAARDEvouw), de alinea "The set reading is the SOURCE's
  field", de EXEMPTION-alinea (AANTAL van OPEN naar GESLOTEN, met de
  kosten en de drie negatiefs), de WF-102-COUNT-bullet (de telling over een
  meerstapsbereik is onuitdrukbaar ALS AANTAL-VOUW — de claim is op de
  KNOOP geschaald, niet op de IR; of een andere knoop over een
  meerstapsbereik telt is die knoop zijn eigen vraag onder zijn eigen
  regel), de flank-(5)-telling (een tweede meelopend document, en het
  onderscheid verdict/zin), de WF-093-clausule (één kruisverwijzing: de
  SETSPELLING van de wrat onder een telling is sinds dit besluit WF-096's
  zaak, de kale blijft WF-093's) en de WF-096-registerrij met de drie
  nieuwe negatiefs.
- `spec/RUNTIME-SEMANTICS.md` — ch. 3.4b gevolg 2: de drie-optiesalinea
  wordt het besluit; het woord ELEMENTS blijft staan en wacht op een drager.
- `spec/ASP-TRANSLATION.md` — de catch-all-inventaris: de AANTAL-setbron is
  het enige item dat de catch-all nooit meer door vertaling verlaat.

**Niet zelf geland — `spec/CONSTRUCTS.md` rij 1.** De zin die vandaag luidt

> **AANTAL** blijft open en is nu scherper gemeten dan "een kloof": `ref`
> telt gevulde waarden waar Drools LEDEN telt (2 tegen 3 zodra één lid het
> attribuut mist), de ASP-backend weigert, er is geen producentroute — de
> frontend weigert de natuurlijke zin met haar eigen WF-096-melding — en
> ch. 3.4a/3.4b geven Drools gelijk. Drie genummerde opties liggen bij de
> maintainer (`backlog/done/20260909-eerste-laatste-setvouw-kloof.md` §6).

wordt voorgesteld te vervangen door

> **AANTAL is beslecht (2026-09-19, optie (ii) van
> `backlog/done/20260909-eerste-laatste-setvouw-kloof.md` §6): de wire
> WEIGERT de setbron onder een telling.** De exemptie van WF-096 is
> versmald van het paar waarop WF-093 sleutelt naar de zes WAARDEvouwen
> alleen; de sentinelfence van WF-034 houdt het bredere paar, want v0 spelt
> een telbron met de sentinel en 81 kale wratten in de vier zones leunen
> daarop. De grond is gemeten, niet beredeneerd: `ref` telde GEVULDE
> WAARDEN waar Drools LEDEN telde (2 tegen 3 zodra één lid het attribuut
> mist), de ASP-backend weigerde de soort, beide lezingen waren
> verifier-schoon, en de normtekst geeft één van de twee lezingen ongelijk:
> `RUNTIME-SEMANTICS.md` ch. 3.4a zegt met zoveel woorden dat een telling
> INSTANTIES vouwt en geen waarden, ch. 3.4b herhaalt het voor de unie
> (ELEMENTEN), en de normatieve RegelSpraak §5.8.1 definieert de telling
> over instanties; ch. 1.2e voegt daar alleen de buurclausule aan toe dat
> een telling nooit leeg is. Er is
> geen producentroute: de frontend weigert de natuurlijke zin met haar
> eigen WF-096-melding. Drie negatiefs, één per wire-spelling
> (`wf096-aantal-setbron`, de gemeten vorm over een `bronRol`;
> `wf096-aantal-setbron-wrat`, de setspelling van de wrat, waar alle drie
> de uitvoerders het EENS zijn en het veld inert is — daar pint de wire de
> tegenstrijdigheid, niet een kloof; `wf096-aantal-setbron-bronpad`, het
> meerstapsbereik, waar nog maar één uitvoerder antwoordt en tegen ch. 3.4b
> in — onuitdrukbaar als AANTAL-VOUW, een claim op de knoop en niet op de
> IR). `ref`'s setbron-arm en de Drools-lowering blijven staan als tweede
> verdedigingslijn, onbereikbaar voor een welgevormd document. Arm-exact
> gemeten: de mutant die `AANTAL` terugzet in `set_source_exempt` beweegt
> exact vier documenten in 666 en geen enkel verdict buiten de negatiefzone
> (poort 12 en 13 byte-identiek vóór en ná). De waardentelling en het
> gemiddelde krijgen een eigen, normatief gedefinieerde operatie:
> `backlog/new/20260919-aantal-gevulde-gemiddelde.md`.

### 6. Gewijzigde bestanden

    spec/verifier/lib/checks.ml                          (versmalling + 2 meldingen)
    corpus/negatief/wf096-aantal-setbron/{ir.json,NOTE.md}          nieuw
    corpus/negatief/wf096-aantal-setbron-wrat/{ir.json,NOTE.md}     nieuw
    corpus/negatief/wf096-aantal-setbron-bronpad/{ir.json,NOTE.md}  nieuw
    corpus/negatief/wf096-setlezing-operand/NOTE.md         citaatblok bijgewerkt
    corpus/negatief/wf096-setlezing-regelwaarde/NOTE.md     citaatblok bijgewerkt
    corpus/negatief/wf096-setlezing-geleste-operand/NOTE.md citaatblok bijgewerkt
    corpus/negatief/wf083-vouwbron-anker-op-operand/NOTE.md  falsificatiealinea herzien
    spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/ASP-TRANSLATION.md

De VIER bijgewerkte NOTEs zijn bestaande artefacten. Bij drie was het
woordelijke verifier-citaat door de meldingsherschrijving verouderd; bij
`wf083-vouwbron-anker-op-operand` was het de falsificatiealinea (§9, fix 1).
Geen document en geen verdict daar bewoog; alleen proza. Geen bestaande
test of check is gewijzigd.

### 7. Poorten (alle veertien, elk als losse aanroep)

    1  check-self-contained      PASS
    2  check-ir-schema           PASS (238 bestanden valideren en round-trippen, vloer 192)
    3  check-verifier            PASS (238 golden schoon; 398 negatief afgewezen op hun eigen WF, vloer 350)
    4  check-case-schema         PASS (568 casusbestanden)
    5  check-oracle              PASS (ref-suite 643 voorbeelden, vloer 489; 539 cases exact, vloer 427)
    6  check-clingo              PASS (backend-suite 419 checks, vloer 83; 539 cases — ref = clingo)
    7  check-noop                PASS (503 cases weerleggen een niets-doener; 36 gepinde overlevers)
    8  check-rood                PASS (29 rode cases falen zichtbaar, vloer 26; Drools-been 6 gepind, 23 niet-gedragen)
    9  check-catala              PASS (201 checks, vloer 201; 12 catala-goldens byte-identiek, vloer 12)
    10 check-regelspraak         PASS (298 JUnit-tests, vloer 173; 58 rgs-goldens byte-identiek, vloer 38)
    11 check-toka-parity         PASS
    12 check-drools              PASS (played 159; kanaalvloer 50 niet-spelers bij naam; JUnit 478, vloer 239)
    13 check-reftype-derivation  PASS (126 annotaties gedraaid over 23 goldens, vloer 74; 104 replays, 22 DRL-tekst, vloer 13)
    14 check-flogic              PASS (130 checks, vloer 81; 2 flogic-goldens, vloer 1)

De negatiefteller staat op 398 (was 395); `EXPECTED_MIN_NEGATIEF` blijft 350
— groei hoeft de vloer niet op te tillen. Geen enkele vloer is aangeraakt.

### 8. Open twijfel

De setspelling van de wrat (`wf096-aantal-setbron-wrat`) is de enige van de
drie die geen uitvoerderskloof afsluit: drie uitvoerders, één antwoord, een
inert veld. Zij valt omdat de kaart één clausule vraagt en niet twee, en
omdat de wire twee tegenstrijdige dingen niet hoort te zeggen. Wie die
afweging anders maakt, weegt "geen enkele consument leest dat veld daar"
tegen "één betekenis, één spelling" — en die weging staat hier genoteerd in
plaats van weggeschreven.

### 9. Lensronde (2026-09-19): zes correcties, alle tekstueel

De lens draaide dertien poorten zelf groen (gate 9 kon daar niet draaien,
catala ontbrak in die omgeving), reproduceerde de mutantsweep en bevestigde
de splitsing van de twee grenzen — één gezamenlijke grens zou dertig
golden/rood-documenten vuil maken, waar ik 81 wratposities telde: dezelfde
bevinding, een andere teller. Geen van de zes punten raakt de verifier of
een document; `spec/verifier/lib/checks.ml` is sinds §2 geen letter
gewijzigd en de md5's in §2 staan.

**Fix 1 — de falsificatiealinea van `wf083-vouwbron-anker-op-operand`.** Die
beloofde dat het document onder de WF-083-armmutant "alone, turns clean".
Dat kan niet meer: het is een `AANTAL` met een setbron, dus het draagt sinds
deze kaart een meelopende WF-096-regel en blijft afgewezen onder elke
WF-083-mutant. De alinea stelt de proef nu op de REGEL in plaats van op het
verdict, en is opnieuw gemeten op de versmalde kop: de re-ankering
(`self_denoting_role ctx ~otype:otn nr` in plaats van `~otype:st`)
verplaatst over 666 documenten exact DRIE rapportregels en GEEN enkel
verdict — dit document verliest zijn WF-083-regel en houdt zijn
WF-096-regel, en de twee `wf093-wratvouw-set-*`-zusters KRIJGEN er elk één,
omdat de re-ankering ook de wrat bereikt. Dat laatste was onzichtbaar toen
de alinea werd geschreven (508 documenten, geen van beide zusters bestond)
en staat er nu bij.

**Fix 2 — de overclaim over uitdrukbaarheid.** "a count over a multi-step
range is not expressible on the wire at all" was te breed: de lens bouwde
een verifier-schoon document met een tellende kwantor over een tweestaps
`bronPad` dat ref én clingo beantwoorden. De claim is versmald tot "niet
uitdrukbaar als AANTAL-VOUW", met de schaal er expliciet bij (de KNOOP, niet
de IR) op alle vier de plekken waar hij stond: de WF-102-COUNT-bullet, de
WF-096-registerrij, de NOTE van `wf096-aantal-setbron-bronpad` en ch. 3.4b
gevolg 2. De positieve claim over de kwantorvouw-route is bewust NIET
opgenomen — een specszin zonder machine-controleerbaar artefact hoort hier
niet, en de getuige-golden raakt de gate-12-kanaallijsten die een parallelle
kaart herstructureert. Dat is werk voor een eigen kaartje.

**Fix 3 — verdict versus zin.** De flank-(5)-telling zei "no golden, red
case or other negatief moves a sentence". Vier negatiefs bewegen wél een
zin: de meldingsherschrijving, precies zoals §2(5) al rapporteerde. De
telling zegt nu VERDICT en noemt de zinsbeweging er apart bij.

**Fix 4 — de bronvermelding klopte niet.** "ch. 1.2e/3.4a/3.4b plus §5.8.1
zeggen alle drie" was op twee manieren onzuiver: het zijn vier bronnen, en
ch. 1.2e draagt de instantieclaim niet — die staat expliciet in ch. 3.4a
("a count folds instances, not values"), ch. 3.4b herhaalt hem voor de unie
(ELEMENTEN), en §5.8.1 definieert de telling zo; ch. 1.2e voegt alleen toe
dat een telling nooit leeg is. Herschreven op de drie plekken in
`spec/IR-SPEC.md` én in de voorgestelde CONSTRUCTS-rijtekst in §5, want die
landt de hoofdsessie letterlijk.

**Fix 5 — de taakverdeling WF-093/WF-096 staat er nu.** De WF-093-clausule
draagt één kruisverwijzing: de SETSPELLING van de wrat onder een telling is
sinds dit besluit WF-096's zaak, de kale spelling blijft WF-093's, en onder
een niet-tellende soort zijn beide spellingen nog steeds WF-093's — wat de
zin "`set` does not enter the rule" betekent en waarom die onveranderd
blijft staan.

**Fix 6 — het frontend-klusje is breder.** Zie §4: niet alleen de
meldingstekst maar ook de `VOUWSOORTEN`-verzameling en het commentaar dat
frontend- en wire-grens gelijk noemt. Gemeten geen lek, wel een onware zin.

**Poorten na deze ronde.** Gate 2 en gate 3 opnieuw gedraaid, want zij zijn
de enige die de gewijzigde bestanden lezen: gate 2 leest golden- en
rood-`ir.json` (niet bewogen, maar hij bewaakt ook de serialisatievorm die
de NOTE-wijzigingen hadden kunnen raken) en gate 3 leest de negatiefzone
plus de verifier. De overige twaalf lezen geen van de aangeraakte bestanden
— de fixes raken alleen `.md`-proza en geen `ir.json`, geen casus, geen
script, geen broncode — en hun uitvoer van §7 staat dus. Gate 1 is er voor
de zekerheid bij gedraaid omdat hij proza in de artefactzones leest.

Daarna is de tak herbased op de nieuwe `main` (`9f41f21a`, de kaart
frontend-stille-markers). De twee verzamelingen bestanden zijn DISJUNCT —
die kaart raakt `frontends/regelspraak/**` en haar eigen kaartbestand, deze
kaart `corpus/negatief/**`, `spec/**` en de verifier — dus er viel niets te
verzoenen. Gate 1, 2 en 3 opnieuw groen op de gecombineerde boom, en gate 10
er ter controle bij omdat de andere kaart de frontend-suite veranderde: PASS
met 317 tests (was 298; de negentien erbij zijn de hare). De overige tien
lezen niets wat een van beide kaarten daar bewoog.
