# De acute flank van de geneste aggregatiebron: de frontend houdt op stil mis te binden

Afsplitsing van `backlog/new/20260910-geneste-aggregatie-rolketen.md`, sectie
"De acute flank kan en moet EERDER landen, als aparte kleine kaart". Die
moederkaart beslist de WIRE-vorm van een meerstaps aggregatiebereik
(`bronPad`, het volgende WF-nummer, spec + schema + verifier + corpus + drie
uitvoerders) en blijft liggen. Deze kaart doet alleen de frontend-helft: de
RegelSpraak-frontend beeldt een tweestaps-zin niet langer stil af op een
éénstaps-relatie, en de type-paar-afleiding van `bronRol` bindt niet langer
tegen de geschreven rolnaam in.

Goal: na deze kaart is het gemeten document B van de moederkaart niet meer
verifier-schoon-met-fout-antwoord.

May touch: `frontends/regelspraak/src/main/java/**`,
`frontends/regelspraak/src/test/java/**` (additief), `backlog/`.

Out of scope: de wire-verruiming zelf (`spec/**`, `corpus/**`) — die is de
moederkaart. De geneste TELVORM (het aantal over een keten), waarvan de
elementnaam in de attribuutpositie staat: een andere zinsvorm, eigen kaart.
De setlezing BUITEN een vouw (WF-096): beslecht door
`backlog/done/20260909-frontend-alle-zonder-vouw.md`.

## Result (2026-09-11)

**De gekozen weigeringsvorm: de bestaande `Onopgelost`-markering, op
WARNING.** De frontend kent precies twee manieren om iets niet te emitteren,
en ze zijn gemeten in plaats van aangenomen:

1. **De markering, per REGEL.** `ExpressieBouwer.onopgelost(ctx)` bouwt een
   `Expr.Onopgelost` met het `Onbekend`-sentinel als type. Dat is het idioom
   dat deze methode al gebruikt, twintig regels hoger, voor een vouwbron die
   ze niet kan binden (het "geen gedeclareerd attribuut van het
   elementtype"-geval, WARNING + markering). `RUNTIME-SEMANTICS.md` noemt het
   met zoveel woorden: "Honest inexpressibility stays with `Onopgelost`
   (WF-044)". De moederkaart beschrijft dezelfde keuze aan de kant van de
   toolchain: "de expressie houdt haar bestaande zichtbare 'niet
   geïmplementeerd'-markering".
2. **De document-brede weigering.** `Severity.ERROR`, waarna
   `Resolver.irModus` niets naar schijf schrijft en met 1 eindigt. Er is GEEN
   derde mechanisme: één regel uit een verder geldig document laten vallen
   kan deze frontend niet.

Gekozen is (1), om drie redenen. Het is het idioom dat er al is, in dezelfde
methode, voor dezelfde soort onbindbaarheid. Het is per regel: één onbindbare
zin doodt niet de overige, wel correcte regels van hetzelfde document. En het
haalt de acceptance volledig — de markering is geen vouw, dus er blijft geen
fout antwoord over; alle vier de lenzen zijn luid (zie de metingen).

(2) is gemeten en niet genomen, en dat is een maintainerkeuze, geen blokkade:
met `Severity.ERROR` in beide diagnoses is de suite **196 geslaagd / 4
gefaald**, en die vier zijn alle vier van mij (drie `ir(...)`-aanroepen in de
nieuwe tests plus de niveau-assertie). **Anders dan bij de zusterkaart blokkeert
hier GEEN enkele bestaande test.** Onder ERROR schrijft `make ir` op document B
niets en eindigt non-zero — de letterlijke "frontend weigert de bron".
Terugdraaibaar in één token plus vier asserties, in twee bestanden:
`Severity.WARNING` → `Severity.ERROR` in `ExpressieBouwer.keurVouwbron`
(tweemaal), en in `GenesteVouwbronTest` de assertie
`assertEquals(Severity.WARNING, …)` plus de `assertTrue(m.isOk(), …)` in de
`ir(...)`-helper.

### Wat er per route gebouwd is

Beide checks zitten in één nieuwe methode, `ExpressieBouwer.keurVouwbron`,
aangeroepen vanuit `bouwFunctie` direct achter de bestaande
onbindbare-vouwbron-arm, en alleen voor een LEDENVOUW: een aggregerende soort
(`AANTAL, SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE`) met precies één
argument dat een `AttribuutRef` met `set: true` is.

**Route 1 — de geneste verzamelingsstap.** `vouwBronOnderwerp(f)` daalt af in
de parse-boom van de aggregatie zelf en levert het set-gekwantificeerde
`onderwerp` van haar EIGEN operand; ze stopt bij een geneste `functieAanroep`
(die beoordeelt haar eigen operand) en bij een §5.8.2 `leegwaardeAlternatief`
(de terugvalwaarde is de vouwbron niet). `genesteSetStap(o)` loopt vervolgens
de hele `van`-keten van dat onderwerp af en levert de eerste geneste
set-stap. Is die er, dan volgt de markering met een diagnose die de KETEN
noemt — beide verzamelingen letterlijk, en waarom er maar één past.

De ketenwandeling is een lus en niet één stap, omdat de gulzige `kaleNaam`
bepaalt waar de keten splitst: gemeten valt "alle geborgen vondsten van de
bergende boot van alle gevoerde boten van de Jutter" (set → to-één → set) ook
in de weigering, terwijl "alle geborgen vondsten van de Jutter" met de
volledige rest in één `kaleNaam` eindigt.

**Route 2 — een type-paar-binding die de geschreven naam tegenspreekt.** De
moederkaart wijst `emit/IrEmitter.java:767` aan
(`verbindendeRolBinding(huidigSubject, elementObjecttype)`, een zuivere
type-paar-zoektocht). **Die regel is bewust NIET gewijzigd**, op grond van
een meting die de hele routekeuze draagt:

> Neem het geëmitteerde document van de éénstaps-getuige en verwijder met de
> hand de `bronRol` — precies wat een emitter-fix zou opleveren. Uitkomst:
> `verifier: PASS`, `oracle: PASS (1 expectations)` en `clingo: PASS (1
> expectations)` op de verwachting 5.00. Een BINDINGLOZE vouw over een uniek
> verbonden paar is welgevormd (WF-048 vraagt alleen of het paar afleidbaar
> is) en elke uitvoerder leidt dezelfde verkeerde relatie opnieuw af.

De binding weghalen verplaatst het probleem dus niet — het maakt het alleen
onzichtbaar. Daarom weigert de RESOLUTIE. `keurVouwbron` vergelijkt de
geschreven elementnaam met de rol die het type-paar zou binden:

- de geschreven naam komt uit het onderwerp van de parse-boom
  (`primaireNaam`), onder **alle** vormen waarmee het elementtype zelf
  gebonden is — daarvoor is de kopnaam-splitsing van `resolveObjecttype`
  uitgefactoriseerd naar `RegelSpraakModel.naamKandidaten` (één bron van
  waarheid, geen tekstscan ernaast);
- `RegelSpraakModel.rolGenoemdAls(binding, naam)` is de geschreven-naam-kant:
  noemt `naam` de rol die de binding bindt — gedeclareerd enkelvoud,
  gedeclareerd meervoud, of (alleen als geen meervoud gedeclareerd is) het
  best-effort meervoud, exact `rolMatcht`;
- drie ontsnappingen, elk met een reden: geen type-paar-binding (dan is er
  niets stils om tegen te spreken — het document houdt zijn bestaande
  stroomafwaartse WF-048); een geschreven naam die de OBJECTTYPENAAM zelf is
  (dan claimt de auteur geen rol en is de type-paar-afleiding de eerlijke
  terugval); en een genavigeerde bron (`navigatieRol`/`navigatiePad`), waar de
  emitter sowieso geen `bronRol` afleidt.

### Metingen, vóór en ná

De drie documenten zijn met de hand nagebouwd uit de moederkaart. Keten:
`Jutter` -(*bemanning*, rol *gevoerde boot*, to-many)→ `Boot` -(*berging*, rol
*geborgen vondst*, to-many)→ `Vondst.gewicht`. Casus: `J1` voert `B1` en `B2`;
`B1` bergt `V1` (3.00 kg) en `V2` (7.00 kg); `B2` bergt `V3` (5.00 kg).
Handverwachting UIT DE ZIN: **3.00**.

- **A** — de ketenzin, geen direct feittype `Jutter`↔`Vondst`.
- **B** — dezelfde zin, plus een direct feittype *vondstaanmelding* met rol
  *aangemelde vondst*, waarbij `J1` alleen `V3` heeft aangemeld. De acute
  flank.
- **C** — NIEUW in deze kaart, de rood-eerst-getuige voor route 2: hetzelfde
  model als B, maar de zin heeft maar ÉÉN stap ("… van alle geborgen vondsten
  van de Jutter"). Geen nesting; de geschreven naam en het gevonden feittype
  botsen, en de frontend bond vandaag stil.

**Vóór** (gereproduceerd op deze kop; A en B reproduceren de moederkaart
letterlijk):

| lens | A | B | C |
|---|---|---|---|
| frontend | `diagnostics: []`, geen `bronRol` | `diagnostics: []`, `bronRol` = *vondstaanmelding*/*aangemelde vondst* | `diagnostics: []`, `bronRol` = *vondstaanmelding*/*aangemelde vondst* |
| verifier | `WF-048 FAIL /resolvedRegels/0/waarde: bindingless instance fold: no fact type relates 'Jutter' to 'Vondst'` | `verifier: PASS` | `verifier: PASS` |
| `ref` (verwachting 3.00) | `EVALUATION FAULT: no fact type relates "Jutter" to "Vondst"` | `MISMATCH: J1.laagste vondstgewicht = 5, expected 3` | `MISMATCH: J1.laagste vondstgewicht = 5, expected 3` |
| ASP-backend (3.00) | `TRANSLATION FAULT: no fact type relates "Jutter" to "Vondst"` | `MISMATCH: J1.laagste vondstgewicht = [500], expected 300` | `MISMATCH: J1.laagste vondstgewicht = [500], expected 300` |
| Drools (3.00) | `EXCLUDE transpiler SKIP: regel: laagste vondstgewicht — regel (aggregatie buiten Phase-C scope)` | `FAIL … attribuut laagste vondstgewicht: verwacht 3.00, werkelijk 5.00` | idem |
| `ref` / ASP / Drools (verwachting 5.00) | — | `oracle: PASS` / `clingo: PASS` / `PLAYED 1` | `oracle: PASS` / `clingo: PASS` / `PLAYED 1` |

**Ná** — A, B en C geven nu alle drie hetzelfde beeld:

| lens | uitkomst |
|---|---|
| frontend | precies ÉÉN WARNING; `waarde` is een `Onopgelost`-knoop, geen `Functie`, geen `bronRol` |
| verifier | `WF-034 FAIL /resolvedRegels/0/waarde/type: unknown-type sentinel inside a structured node` |
| `ref` | `EVALUATION FAULT: Onopgelost reached in evaluation: this rule fails observably (WF-044)` |
| ASP-backend | `TRANSLATION FAULT: Onopgelost reached in translation: this rule fails observably (WF-044)` |
| Drools | `EXCLUDE transpiler SKIP: regel: laagste vondstgewicht — regel (waarde buiten scope)` |

Document B is daarmee niet alleen niet meer verifier-schoon-met-fout-antwoord:
het is **niet meer verifier-schoon**, ongeacht welke verwachting je erbij
schrijft. Dat de markering WF-034 raakt is geen nieuwe klasse uitvoer — het
`Onbekend`-sentinel is wat `onopgelost(ctx)` overal in deze frontend al
schrijft; hier is het alleen voor het eerst op een vouwbron gemeten.

**De twee diagnoses, letterlijk** (severity WARNING, regelnummer = de regel
waar de aggregatie staat):

    geneste verzamelingsstap in een vouwbron: 'alle geborgen vondsten van alle
    gevoerde boten van de Jutter' stapelt de verzameling 'alle gevoerde boten
    van de Jutter' onder een tweede verzameling, en een vouwbron draagt precies
    één relatie (IR-SPEC ch. 4.3: één elementObjecttype plus één bronRol) — de
    buitenste stap zou spoorloos wegvallen, dus de aggregatie blijft onopgelost
    (§13.4.17)

    vouwbron bindt niet op de geschreven naam: de zin noemt 'geborgen
    vondsten', maar het enige feittype dat Jutter met Vondst verbindt is
    'vondstaanmelding' met de rol 'aangemelde vondst' — de type-paar-afleiding
    zou een andere verzameling vouwen dan de zin noemt, dus de aggregatie
    blijft onopgelost (§13.4.1/§13.4.17)

**Rood eerst.** Alle dertien getuigen staan in één nieuw bestand,
`GenesteVouwbronTest`. Tegen de kop van vóór de ingreep (de twee
main-bestanden gestasht, de tests ongewijzigd): **5 geslaagd / 8 gefaald**. De
acht die rood staan zijn precies de acht die de ingreep beschrijft — de drie
nest-getuigen (A, B, de gemengde keten), de twee naam-botsingsgetuigen (C, en
de wire ervan), de wire van B, het niveau, en de diagnose-op-de-wire. De vijf
die vóór én ná slagen zijn de tegenproeven; die kunnen per constructie niet
rood staan.

**Tegenproeven tegen over-afwijzing** (alle vijf groen vóór en ná):

| tegenproef | wat ze vasthoudt |
|---|---|
| een correcte éénstaps vouw ("alle aangemelde vondsten van de Jutter") | emitteert ongewijzigd, `kind: MINIMUM` mét `bronRol` — geen diagnose |
| een geschreven OBJECTTYPENAAM als elementwoord | houdt de type-paar-afleiding; de auteur claimde geen rol |
| de §13.4.17.47 telvorm ("het aantal aangemelde vondsten van de Jutter") | bindt nog altijd via de geschreven elementnaam (`elementRolBinding`), een andere afleiding op een andere zinsvorm |
| dezelfde ketenzin ZONDER vouw eromheen | houdt haar eigen WF-096-diagnose (zusterkaart) en pikt de mijne niet op — precies één diagnose |
| de éénstaps botsing ZONDER enig verbindend feittype | geen diagnose van mij; het document houdt zijn bestaande stroomafwaartse WF-048 |

**De vormsweep over vier zones** (`corpus/golden`, `corpus/rood`,
`corpus/negatief`, `toka`): **56 ledenvouwen**. Daarvan draagt er **nul** een
geschreven bron met twee set-stappen — de aanname van de moederkaart,
onafhankelijk nagemeten. Veertig noemen met hun geschreven naam de gebonden
rol, acht dragen geen `bronRol`, en acht zijn documenten waarvan de
`onderwerpBron`-STRING het hele attribuutlezen draagt in plaats van het kale
onderwerp, zodat de sweep ze niet kan splitsen; die acht zijn alle acht
handgebouwde negatiefs (`NOTE.md` + `ir.json`, geen `bron.rgs`), dus geen
frontend-route regenereert ze. De harde nulmeting is hoe dan ook gate 10 en
gate 11: 41 respectievelijk 45 bronbestanden re-emitteren byte-identiek.

### Gates

Vooraf gemeten, vóór er iets bewoog:

    check-regelspraak: PASS — JUnit suite green (187 tests, floor 173); 41 rgs golden(s) re-emit byte-identically (floor 38)
    check-toka-parity: PASS   (45 .rgs byte-identiek, vloer 45; 27 tests, vloer 27; 29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)

Ná, op deze kop:

    check-self-contained: PASS
    check-regelspraak: PASS — JUnit suite green (200 tests, floor 173); 41 rgs golden(s) re-emit byte-identically (floor 38)
    check-toka-parity: PASS   (45 .rgs byte-identiek, vloer 45; 27 tests, vloer 27; 29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)

Suite 187 → 200 (+13, volledig additief; vloer 173 ongemoeid). Gate 11 is
ongewijzigd op elk getal. De overige gates zijn niet gedraaid en hoefden dat
niet: deze kaart voegt geen corpusbestand toe en wijzigt er geen, en de 41 +
45 byte-identieke re-emissies zijn het bewijs dat er geen document bewoog.
Geen gate-script is aangeraakt; geen vloer bewoog.

### Een gemeten open flank, voor de moederkaart

De moederkaart zet "gemengde ketens waarin een to-one stap voorkomt" onder
*Out of scope*, "benoemen als open vraag". Hier is de meting erbij. Een
vouwbron met ÉÉN set-stap en daaronder een TO-ÉÉN stap —

    … als de minimale waarde van het gewicht van alle vondsten van de boot van de Jutter.

— waarbij *vondst* wél de rol is die het enige `Jutter`↔`Vondst`-feittype
bindt, blijft vandaag stil: geen diagnose, `verifier: PASS`, en "van de boot"
valt spoorloos weg; de vouw loopt vanaf de Jutter. Route 1 vangt hem niet (de
geneste stap is geen verzameling) en route 2 ook niet (de geschreven naam
noemt de gebonden rol correct). Dat is dezelfde klasse fout als document B,
langs een derde route, en hij hoort bij de wire-vraag: zodra de bron een PAD
draagt is een to-één tussenstap gewoon een stap in dat pad.

### Registerwerk voor de hoofdsessie (serieel, beschermde paden)

- **`spec/CONSTRUCTS.md` rij 4** ("Fact types and role navigation" — waar de
  `bronRol`-afleiding en de WF-047/WF-048-discipline wonen). Voorstelzin om
  achteraan toe te voegen:

  > **Landed (2026-09-11):** de PRODUCENT van `bronRol` raadpleegt de
  > geschreven rolnaam. `verbindendeRolBinding` zoekt alleen op TYPES, dus bij
  > precies één verbindend feittype won die altijd, welke rol de zin ook
  > schreef — en dat is niet zichtbaar op de wire, want een ledenvouw heeft
  > geen veld waarin de geschreven vorm van haar bron staat (de
  > `fold_source`-herkenner kruist de geschreven naam alleen bij de
  > Onbekend-getypeerde telwrat). Gemeten: een tweestaps-zin op een model met
  > een toevallig direct feittype was `verifier: PASS` en ref, de ASP-backend
  > en Drools antwoordden alle drie 5.00 waar de zin 3.00 zegt — het
  > differentiële vierkant eensluidend over de VERKEERDE verzameling, door
  > geen enkele gate zichtbaar. De binding weglaten helpt niet, ook gemeten:
  > een BINDINGLOZE vouw over een uniek verbonden paar is verifier-schoon
  > (WF-048 vraagt alleen naar afleidbaarheid) en elke uitvoerder leidt
  > dezelfde verkeerde relatie opnieuw af. Daarom weigert de RESOLUTIE, niet
  > de emissie: de RegelSpraak-frontend markeert een ledenvouw als
  > `Onopgelost` (WARNING, het idioom dat dezelfde methode al draagt voor een
  > onbindbare vouwbron) zodra (a) het set-onderwerp zelf een set-stap nest —
  > de v0-wire draagt één relatie, dus de buitenste stap zou spoorloos
  > wegvallen — of (b) de geschreven elementnaam de rol die het type-paar zou
  > binden niet NOEMT (gedeclareerd enkelvoud, gedeclareerd meervoud of het
  > best-effort meervoud, exact `rolMatcht`), met drie ontsnappingen: geen
  > type-paar-binding, een geschreven naam die de objecttypenaam zelf is, en
  > een genavigeerde bron. De geschreven naam wordt beoordeeld onder exact de
  > vormen waarmee het elementtype gebonden is (de kopnaam-splitsing van
  > `resolveObjecttype`, nu gedeeld als `naamKandidaten`). Gemeten over de
  > vier zones: 56 ledenvouwen, NUL met een geschreven bron van twee
  > set-stappen, dus geen wire bewoog (gate 10 41 en gate 11 45 byte-identiek).
  > Twee dingen blijven bewust staan en zijn gemeten: het niveau is WARNING,
  > waar ERROR — die het document helemaal niet naar schijf laat gaan — vrij
  > ligt omdat GEEN bestaande test hem blokkeert (enkel vier asserties van de
  > kaart zelf), en de gemengde keten met alleen een TO-ÉÉN tussenstap blijft
  > stil: die hoort bij de wire-vraag, want met een bereikPAD is zo'n stap
  > gewoon een stap (`backlog/done/20260911-geneste-aggregatie-acute-flank.md`).

- **`spec/PLAN.md`**: geen voorgestelde wijziging. De scope van de moederkaart
  is niet veranderd — de wire-vraag staat er onverkort — en er is geen besluit
  bijgekomen dat het plan draagt.

### Wat bewust niet bewoog

- `emit/IrEmitter.java` — geen letter. De kaart wees die regel aan; de meting
  hierboven laat zien dat een fix dáár de fout onzichtbaar maakt in plaats van
  weg.
- `spec/**` in zijn geheel, `scripts/**`, `.github/**`, `corpus/**`: geen
  letter. De wire-vorm (`bronPad`, het volgende WF-nummer, de vlakke-unie-lezing,
  de diepte-vraag) is de moederkaart en is hier niet aangeraakt.
- Bestaande tests: geen enkele gewijzigd. Alle dertien nieuwe getuigen staan
  in één nieuw bestand.
- De geneste TELVORM en de setlezing buiten een vouw: allebei ongemoeid, en
  allebei met een tegenproef vastgezet zodat ze zichtbaar blijven waar ze zijn.
- De gemengde keten met alleen een to-één tussenstap: gemeten, benoemd, niet
  gedicht (zie hierboven).

### Voor de hoofdsessie: re-vendor

Deze landing raakt `frontends/regelspraak`. Gewijzigde bestanden onder die
map, en alleen deze drie:

- `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java`
  (gewijzigd: één aanroep in `bouwFunctie`, plus `keurVouwbron`,
  `vouwBronOnderwerp` en `genesteSetStap`)
- `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java`
  (gewijzigd: `naamKandidaten` uitgefactoriseerd uit `resolveObjecttype`, en
  `rolGenoemdAls` erbij)
- `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/GenesteVouwbronTest.java`
  (nieuw)
