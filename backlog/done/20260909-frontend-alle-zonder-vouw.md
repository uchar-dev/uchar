# De frontend emitteert "alle <mv>" zonder vouw: welke diagnostiek of welke emissie?

Goal: WF-096 (`spec/IR-SPEC.md` ch. 4.3, maintainerbesluit 2026-09-09) maakt
een `AttribuutRef` met `set: true` buiten een vouwbron ill-formed op de wire.
Die regel snijdt een LEVENDE producentroute af: de RegelSpraak-frontend
emitteert die knoop vandaag zonder één diagnostiek, op twee posities. Deze
kaart beslist wat de frontend in plaats daarvan doet, spec-first — de
IR-regel staat er al, de frontend moet meebewegen. De emissie is bij het
WF-096-kaartje bewust ONGEWIJZIGD gelaten omdat ze daar bewijsmateriaal was;
ze staat nu byte voor byte in `corpus/negatief/wf096-setlezing-regelwaarde/`
en `corpus/negatief/wf096-setlezing-operand/`.

## Wat er gemeten is (2026-09-09, tijdens de WF-096-kaart)

Twee bronzinnen, beide grammaticaal, beide `diagnostics: []`:

    Het maatgevend gewicht van een Jutter moet berekend worden als
    het gewicht van alle vondsten van de Jutter.

    Het maatgevend gewicht van een Jutter moet berekend worden als
    het gewicht van alle vondsten van de Jutter plus het
    drempelgewicht van de Jutter.

De eerste levert de setlezing als HELE regelwaarde, de tweede dezelfde knoop
als LINKEROPERAND van een `Binair PLUS`. In beide gevallen: een echt
`Numeriek`-type (kg, twee decimalen), een `navigatieBindingen`-rolbinding,
`set: true`, geen diagnostiek. Beide documenten zijn sinds 2026-09-09
verifier-FAIL met één regel, WF-096, op precies die positie.

De stempel zit op één plek: `isSetOnderwerp(onderwerp)` bij
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java:1664`
— het "alle `<mv>`"-oppervlak zet `set`, en niets vraagt daarna of er een
vouw omheen staat.

## De te wegen antwoorden

**(a) Een diagnostiek.** De zin noemt een verzameling waar de regel één
waarde schrijft; de frontend weigert hem met een gerichte melding en
emitteert de regel niet (of emitteert met niet-lege `diagnostics`). Vraag om
te beslechten: WAAR staat de test — bij het bouwen van de referentie (weet
daar iets van de omhullende positie?) of als een naloop over de gebouwde
regelboom, waar "staat er een vouw omheen" een lokale vraag is. En: is dit
een fout of een waarschuwing-met-drop, gegeven dat v0's resolver
fail-soft is en `CLAUDE.md`'s fail-fast-regel het tegenovergestelde vraagt.

**(b) Een andere emissie.** De zin krijgt een impliciete vouw — maar WELKE?
Er is geen aggregatiewoord in de bron, dus elke keuze (som? eerste?) is een
gok die de frontend nergens anders maakt. Als dit het antwoord wordt, moet
het een RegelSpraak-argument hebben (spreekt de taalspecificatie een
default-aggregatie uit?) en niet een IR-argument.

Het vermoeden bij het schrijven van deze kaart is (a); de kaart moet het
argument leveren, niet de aanname.

## Acceptance

- Eén beslissing voor BEIDE posities (hele regelwaarde, operand van een
  samengestelde knoop), expliciet gemotiveerd — WF-096 is één regel voor
  beide omdat ze identiek meten, en de frontend heeft geen reden ze te
  splitsen die de IR niet heeft.
- De twee bronzinnen hierboven zijn de testgetuigen: additieve tests in
  `frontends/regelspraak/src/test/java/**` die de nieuwe uitkomst pinnen
  (bij (a): de diagnostiek, letterlijk; bij (b): het geëmitteerde IR).
- De negatiefs `wf096-setlezing-regelwaarde` en `wf096-setlezing-operand`
  blijven staan en blijven WF-096-FAIL: ze pinnen de WIRE-regel, niet de
  frontend. Als (b) wint verandert dat niet — de oude vorm blijft
  ill-formed, de frontend produceert hem alleen niet meer.
- Gate 10 groen met een niet-nul testtelling; gate 9 en gate 11 ongemoeid en
  gemeten (de `toka/`-pariteitszone draait dezelfde emitter — meet of enige
  pariteitsdump van vorm verandert VOORDAT je iets wijzigt, en zo ja, dan is
  dat de eerste bevinding van de kaart, niet een verrassing achteraf).
- Alle dertien gates groen.

May touch: `frontends/regelspraak/src/main/java/**`,
`frontends/regelspraak/src/test/java/**` (additief), `backlog/`.

Out of scope: `spec/IR-SPEC.md` en `spec/verifier/**` — WF-096 is beslecht
en wordt hier niet heropend. `corpus/negatief/wf096-*` — die documenten zijn
de wire-getuigen en blijven ongewijzigd. Het `set`-veld op een regel-DOEL:
gemeten verifier-schoon, door alle drie de uitvoerders genegeerd, en een
eigen vraag (`spec/IR-SPEC.md` ch. 4.3, de "niet bereikt"-alinea bij
WF-096).

## Result (2026-09-11)

**Het besluit: (a), een diagnose. Geen impliciete vouw, geen nieuwe
lowering.** Twee onafhankelijke redenen, en de kaart vroeg om de tweede.

1. Het besluitkader van vandaag (maintainerlijn 2026-09-11, gedocumenteerd
   terugdraaibaar): alleen echte bugs, geen nieuwe features. Antwoord (b) —
   de zin een impliciete vouw geven — is per definitie een nieuwe lowering en
   valt daarmee buiten het kader. Terugdraaibaar: het kader kan herzien
   worden, en dan ligt (b) weer open; deze kaart heeft er niets voor
   afgebroken.
2. Los daarvan haalt (b) de eigen voorwaarde van de kaart niet. De kaart
   eist voor (b) een RegelSpraak-argument, en dat is er niet: in de bronzin
   staat geen aggregatiewoord, dus elke keuze (som? eerste?) is een gok die
   de frontend nergens anders maakt, en de taalspecificatie spreekt geen
   default-aggregatie uit. De vorm MET vouw bestaat al en werkt — "de som
   van …" en "de eerste van …" emitteren ongewijzigd en verifier-schoon — en
   welke vouwsoorten de uitvoerders dragen is de vraag van
   `backlog/new/20260909-eerste-laatste-setvouw-kloof.md`, die blijft liggen.

**Eén beslissing, acht posities.** De kaart vroeg één beslissing voor twee
posities. De meting vond er nog zes van dezelfde vorm, uit dezelfde
zinsnede, alle met `diagnostics: []` en alle verifier-FAIL onder WF-096:

| route | soort | verifier-aanwijzer |
|---|---|---|
| hele regelwaarde ("moet berekend worden als") | BEREKENING | `/resolvedRegels/0/waarde` |
| linkeroperand van een `Binair PLUS` | BEREKENING | `/resolvedRegels/0/waarde/links` |
| §13.4.4 gelijkstelling ("moet gesteld worden op") | GELIJKSTELLING | `/resolvedRegels/0/waarde` |
| §13.4.9 initialisatie ("moet geïnitialiseerd worden op") | INITIALISATIE | `/resolvedRegels/0/waarde` |
| voorwaarde van de regel | — | `/resolvedRegels/0/voorwaarde/links` |
| §13.4.8 consistentieregel | — | `/consistenties/0/assertie/links` |
| voorwaarde van een §13.4.6 objectcreatie | — | `/objectCreaties/0/voorwaarde/links` |
| §13.4.6.3 objectcreatie-toekenning (generieke expressie-RHS) | — | `/objectCreaties/0/toekenningen/0/waarde` |

De telling, zodat ze naspeelbaar is: de eerste ronde mat er zes, waarbij rij 3
en 4 nog als één "initialisatiewaarde" geteld werden (de zin was de
GELIJKSTELLING-vorm, niet de §13.4.9-vorm); de fixronde splitste die twee en
voegde de achtste toe — de objectcreatie-toekenning, een generieke `Expr` op
`ResolvedObjectCreatie.Toekenning.waarde` en een eigen levende producentroute,
waar de verifier WF-096 als ENIGE regel gaf.

Ze meten identiek, dus ze krijgen één regel — hetzelfde argument waarmee
WF-096 de twee leesposities niet splitst. De zes extra zijn geen
scope-uitbreiding maar dezelfde bug op een buurroute; ze los laten staan
zou de "een groen deel is niet groen"-regel schenden.

**Waar de test staat: een naloop, niet het bouwen van de referentie.** De
WAARDEN lopen door `RuleChecker.keurSetlezingen()`, aangeroepen in `check()`
ná `keurKringverwijzingen()` en vóór `defaults(doc)`: alle DRIE de
Expr-dragende waarde-sleuven die deze IR heeft — de `waarde` van een
`ResolvedRegel`, de `assertie` van een `ResolvedConsistentie` en de `waarde`
van een `ResolvedObjectCreatie.Toekenning`. De VOORWAARDEN lopen
één regel na hun bouw in `regelVersies`, want dát is de ene plek waar de
voorwaarde van ELK resultaatdeelsoort langskomt: de constructies die géén
`ResolvedRegel` worden (verdeling, objectcreatie, feitcreatie,
dagsoortdefinitie, startpuntbepaling) dragen hun voorwaarde in een eigen
carrier, en zonder die plaatsing zou de naloop ze niet zien. Arm-exact
gemeten op de eindkop: die ene aanroep uitzetten maakt precies twee getuigen
rood — `setlezing_in_een_voorwaarde_wordt_benoemd` en
`setlezing_in_de_voorwaarde_van_een_objectcreatie_wordt_benoemd` — en verder
niets (185 geslaagd / 2 gefaald). Drie redenen voor de naloop zelf:

- `ExpressieBouwer.attribuutRef` bouwt de referentie vóórdat de knoop die
  haar zou consumeren bestaat; "staat er een vouw omheen" zou daar door de
  hele bouwer heen geregen moeten worden. In de naloop is het een lokale
  vraag, precies zoals de kaart voorzag.
- Vóór de defaults-lift, omdat die pass regels UIT `resolvedRegels` haalt en
  de `consequentie` van een `Default` een kopie van de regelwaarde is: daar
  ervóór ziet de wandeling elke regel precies één keer.
- De exemptie is letterlijk de grens die de verifier trekt: één argument,
  een vouwende soort (de zes waardevouwen plus `AANTAL`) én een opgelost
  `elementObjecttype`. Die derde conjunct is geen versiering: de fold-tak
  van de verifier is gescoped op `element_objecttype = Some`, en zonder die
  valt het argument terug op `walk_expr`, waar de WF-096-arm alsnog vuurt.
  Een `DimensieCel` om de bron heen is ook niet de vrijgestelde positie —
  de fold-tak matcht op de blote referentie — en de wandeling stopt de
  vrijstelling daar op dezelfde manier.

De `Expr`-wandeling is uitputtend over de sealed union (geen `default`-arm,
zoals `Expr.bevat`, zodat de volgende knoop een compilerfout is en geen
stille bladpositie); de `Conditie`-helft reikt precies zo ver als
`Conditie.bevatExpr`. Het regel-DOEL wordt bewust niet gelopen: dat is een
schrijfpositie, WF-096's eigen scope-noot reikt er niet, en het is een
eigen vraag. En de twee overgebleven constructies zijn niet overgeslagen maar
LEEG: `ResolvedVerdeling` en `ResolvedFeitCreatie` dragen structureel geen
enkele `Expr` — alleen kale attribuut- en veldNAMEN plus een `Conditie` — dus
daar valt geen waarde te lopen, en hun voorwaarde is gedekt door de aanroep in
`regelVersies`. De drie waarde-sleuven hierboven zijn daarmee de volledige
lijst, geen steekproef.

**Het niveau: WARNING, met de ERROR-vork benoemd — het open punt van deze
kaart.** De melding bereikt de wire (de emitter schrijft elke diagnostic),
dus het document vertelt nu zelf wat eraan mankeert waar het eerst
`diagnostics: []` schreef. Wat WARNING NIET koopt: het document wordt nog
steeds geschreven, dus de frontend produceert nog altijd een ill-formed
document — gediagnosticeerd, niet voorkomen. Gate 3 blijft de tweede
verdedigingslinie.

ERROR is het fail-fast-niveau en de huisregel van `CLAUDE.md` wijst die
kant op: `Resolver.irModus` schrijft niets zodra het model een fout draagt,
dus de ongeldige emissie zou de schijf niet meer raken. Waarom hij hier
niet genomen is, gemeten in plaats van vermoed: met `Severity.ERROR` is de
suite **184 geslaagd / 3 gefaald**, en die drie zijn precies deze —

- `IrTest.referenties_lossen_op_via_feittype_rollen`
  (`src/test/java/io/github/yhavinga/regelspraak/IrTest.java:351`), die met
  `assertTrue(m.errors().isEmpty(), "valse fout via rolnavigatie")` pint dat

      De prijs van een groep moet gesteld worden op de leeftijd van alle deelnemers.

  foutloos resolvet — een in-repo getuige van dezelfde producentroute,
  vastgelegd als een slagende verwachting van vóór WF-096;
- `SetlezingZonderVouwTest.de_diagnose_is_een_waarschuwing_en_geen_fout`, de
  assertie `assertEquals(Severity.WARNING, ds.get(0).severity())`;
- `SetlezingZonderVouwTest.de_emissie_draagt_de_diagnose_op_de_wire`, de
  assertie `assertTrue(json.contains("\"severity\": \"WARNING\""))`.

De laatste twee zijn van mij en vallen onder ERROR mee om: ze pinnen het
niveau juist zodat een verhoging zichtbaar is. De eerste is de blokkade: een
kaartsessie verzet geen bestaande test, dus het niveau stopt bij WARNING en de
vork gaat omhoog. **Terugdraaibaar in één token plus drie asserties, in twee
bestanden:** `Severity.WARNING` → `Severity.ERROR` in
`RuleChecker.keurSetlezing`, en de drie asserties hierboven
(`IrTest.java` één, `SetlezingZonderVouwTest.java` twee).

**De melding, letterlijk** (severity WARNING, regelnummer = de regel waar de
setlezing staat):

    setlezing buiten een vouwbron: 'het gewicht van alle vondsten van de
    Jutter' noemt de waarde van elk lid van de verzameling — alleen
    toegestaan als bron van één vouw (som, totaal, minimum, maximum, eerste,
    laatste, aantal); hier wordt één waarde verwacht (WF-096, IR-SPEC ch. 4.3)

### Metingen, vóór en ná

**Vóór er iets bewoog — de vormsweep over vier zones.** 591 IR-documenten in
`corpus/golden`, `corpus/rood`, `corpus/negatief` en `toka`, gescand op een
`AttribuutRef` met `set: true` buiten de exemptie. Precies **vier** dragen de
vorm, alle vier in `corpus/negatief`: de drie `wf096-*` en
`wf034-sentinel-geleste-setlezing`. **Geen golden, geen rode casus en geen
pariteitsdump draagt hem** — de aanname van de kaart is daarmee geverifieerd,
en de pariteitszone kon per constructie niet van vorm veranderen. Nagemeten
ná de ingreep: gate 11 PASS, `45 .rgs re-emitted byte-identical`.

**Rood eerst, herbevestigd op de eindkop.** De hele check uitzetten (beide
aanroepen) geeft **177 geslaagd / 10 gefaald**: de acht routegetuigen plus de
twee die het niveau en de wire pinnen
(`de_diagnose_is_een_waarschuwing_en_geen_fout`,
`de_emissie_draagt_de_diagnose_op_de_wire`). De drie tegenproeven slagen vóór
én ná — ze kunnen per constructie niet rood staan. Ná de fix: **187 geslaagd /
0 gefaald**. Suite 174 → 187 (+13, volledig additief; vloer 173 ongemoeid).

Arm-exact per aanroep, zodat geen van beide overbodig is: alléén de
conditie-aanroep in `regelVersies` uitzetten geeft **185 geslaagd / 2
gefaald** — de voorwaarde- en de objectcreatie-voorwaarde-getuige, en verder
niets.

**De emissie zelf, vóór en ná, op elf probe-documenten** (één model, elf
regelbestanden, `make ir`, met de check uit en weer aan): de **acht**
ill-formed routes krijgen elk exact één WARNING, de **drie** tegenproeven
(`som`, `eerste`, `som` in een objectcreatie-toekenning) krijgen er nul, en in
ALLE elf is de rest van het document byte-identiek — alleen de
`diagnostics`-array beweegt. Dat is waarom gate 10's byte-identiteit niet kon
breken: een golden zonder de vorm krijgt geen diagnostic, en dus geen ander
document. Expliciet nagemeten in plaats van afgeleid: van de **41** goldens
met een `bron.rgs` heeft er **nul** een niet-lege `diagnostics`-array, dus ook
nul die de setlezingmelding zou kunnen oppikken.

**Gates — alle dertien groen op deze kop, slotregels letterlijk:**

    1  check-self-contained: PASS
    2  check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 197 file(s) validate and round-trip, floor 192)
    3  check-verifier: PASS (197 golden clean; 364 negatief rejected for their declared rule, floor 350)
    4  check-case-schema: PASS (schema valid; 467 case file(s) validate)
    5  check-oracle: PASS (ref suite 535 worked examples, floor 489; 441 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
    6  check-clingo: PASS (backend suite 120 checks, floor 83; 441 cases replay exactly through clingo — ref = clingo over the corpus)
    7  check-noop: PASS (405 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
    8  check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)
    9  check-catala: PASS — frontend suite green (201 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10 check-regelspraak: PASS — JUnit suite green (187 tests, floor 173); 41 rgs golden(s) re-emit byte-identically (floor 38)
    11 check-toka-parity: PASS  (45 .rgs re-emitted byte-identical, floor 45; TOKA suite 27 tests; 29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)
    12 check-drools: PASS (channel floor 47 non-PLAYED goldens pinned by name; played matches the pinned 124; JUnit 310 tests, floor 239)
    13 check-reftype-derivation: PASS (83 unit annotations twisted over 16 goldens, floor 74; 74 executor replays reproduce the untouched outcome, 14 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)

Gate 3's `364 negatief rejected for their declared rule` is de plek waar de
twee wf096-getuigen gewoon WF-096-FAIL blijven; geen vloer bewoog, geen enkel
gate-script is aangeraakt.

### Registerwerk voor de hoofdsessie (serieel, beschermde paden)

- **`spec/CONSTRUCTS.md` rij 1**, de WF-096-zin van 2026-09-09, zegt nu nog
  "both live producer routes, follow-up card for the frontend". Voorstelzin
  om daarachter te zetten:

  > **Frontend follow-up landed (2026-09-11):** the RegelSpraak frontend
  > names the shape instead of emitting it in silence — one resolver walk
  > over the built rule tree (after the cycle check, before the defaults
  > lift), exempting exactly the verifier's fold-source boundary (single
  > argument ∧ folding kind ∧ a resolved `elementObjecttype`), so the
  > producer's boundary and the wire's are one boundary. All THREE
  > Expr-carrying value slots this IR has are walked — a rule's waarde, a
  > consistentieregel's assertie and a §13.4.6.3 objectcreatie toekenning —
  > which is the complete list and not a sample: `ResolvedVerdeling` and
  > `ResolvedFeitCreatie` carry no `Expr` at all. Conditions are walked one
  > line after they are built, where every resultaatdeel kind's voorwaarde
  > passes, so the constructs that never become a rule carrier are covered
  > too. Measured EIGHT routes from one sentence fragment, all previously
  > `diagnostics: []` and all verifier-FAIL: the berekening value, an
  > operand of a composite, the §13.4.4 gelijkstelling and §13.4.9
  > initialisatie values, the rule's voorwaarde, a §13.4.8
  > consistentieregel, and an objectcreatie's voorwaarde and its
  > toekenning. The diagnostic rides the wire at severity WARNING, so the
  > document now states its own defect; ERROR — which would stop the
  > ill-formed document reaching disk at all — is deliberately NOT taken
  > and is recorded as the maintainer's call, because one committed
  > frontend expectation (`IrTest.referenties_lossen_op_via_feittype_rollen`,
  > a third witness of the same producer route) asserts that a rule value
  > of exactly this shape resolves error-free; promotion costs one token
  > plus three assertions in two files. The rule TARGET stays out of scope,
  > as WF-096's own scope note has it. No golden, red case or parity
  > document carries the shape (measured over 591 documents), so no wire
  > moved (`backlog/done/20260909-frontend-alle-zonder-vouw.md`).

- **`spec/PLAN.md`**: geen voorgestelde wijziging. De scope van de kaart is
  niet veranderd en er is geen besluit bijgekomen dat het plan draagt; het
  enige open besluit (het niveau) staat hierboven en hoort bij de
  maintainer, niet in het plan.

### Wat bewust niet bewoog

- `corpus/negatief/wf096-setlezing-regelwaarde` en
  `wf096-setlezing-operand`: niet aangeraakt, niet geregenereerd. Ze pinnen
  de WIRE-regel, niet de frontend, en blijven WF-096-FAIL.
- `backlog/new/20260909-eerste-laatste-setvouw-kloof.md` blijft liggen. De
  vorm MET vouw is daar, niet hier: deze kaart raakt geen vouwsoort aan en
  voegt er geen toe. De tweede tegenproef ("de eerste van …") pint alleen dat
  de frontend hem ongewijzigd blijft emitteren, zodat die kaart zijn eigen
  meting nog kan doen.
- `spec/**` (heel), `scripts/**`, `.github/**`, `corpus/**`: geen letter.
  WF-096 is niet heropend.
- Bestaande tests: geen enkele gewijzigd. Alle dertien nieuwe tests staan in
  één nieuw bestand.
- Het regel-DOEL (`set: true` op een `doel`): buiten WF-096's bereik, niet
  gelopen, eigen vraag.
- Geen nieuwe emissie, geen impliciete vouw, geen drop van de regel. De
  regel blijft in het model staan; alleen het document zegt er nu iets bij.

### Voor de hoofdsessie: re-vendor

Deze landing raakt `frontends/regelspraak`, dus de toolchain-re-vendor hoort
erbij. Gewijzigde bestanden onder die map, en alleen deze twee:

- `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/RuleChecker.java`
  (gewijzigd: twee aanroepen — één in `check()` voor de drie waarde-sleuven,
  één in `regelVersies` voor de voorwaarden — plus `VOUWSOORTEN`,
  `keurSetlezingen()` en de twee `keurSetlezing`-overloads)
- `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/SetlezingZonderVouwTest.java`
  (nieuw)
