# De acute flank van de geneste aggregatiebron: de frontend houdt op stil mis te binden

Afsplitsing van `backlog/new/20260910-geneste-aggregatie-rolketen.md`, sectie
"De acute flank kan en moet EERDER landen, als aparte kleine kaart". Die
moederkaart beslist de WIRE-vorm van een meerstaps aggregatiebereik
(`bronPad`, het volgende WF-nummer, spec + schema + verifier + corpus + drie
uitvoerders) en blijft liggen. Deze kaart doet alleen de frontend-helft: de
RegelSpraak-frontend beeldt een meerstaps-zin niet langer stil af op een
éénstaps-relatie — niet als de tweede stap een verzameling is en niet als ze
een to-één stap is — en de type-paar-afleiding van `bronRol` bindt niet langer
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

1. **De markering, per REGEL.** Een `Expr.Onopgelost` in plaats van de vouw.
   Dat is het idioom dat deze methode al gebruikt, twintig regels hoger, voor
   een vouwbron die ze niet kan binden (het "geen gedeclareerd attribuut van
   het elementtype"-geval, WARNING + markering). `RUNTIME-SEMANTICS.md` noemt
   het met zoveel woorden: "Honest inexpressibility stays with `Onopgelost`
   (WF-044)". De moederkaart beschrijft dezelfde keuze aan de kant van de
   toolchain: "de expressie houdt haar bestaande zichtbare 'niet
   geïmplementeerd'-markering". De markering van DEZE kaart draagt het eigen
   resultaattype van de aggregatie, niet het `Onbekend`-sentinel dat
   `onopgelost(ctx)` schrijft — zie "Het kanaal van de markering" hieronder.
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
met `Severity.ERROR` in de diagnoses is de suite **199 geslaagd / 6 gefaald**
(nagemeten op de eindkop door lens 2), en die zes zitten alle zes in
`GenesteVouwbronTest`: de `ir(...)`-routegetuigen, de
`assertTrue(Resolver.resolve(…).isOk(), …)` op regel 392 en de
niveau-assertie. **Anders dan bij de zusterkaart blokkeert
hier GEEN enkele bestaande test.** Onder ERROR schrijft `make ir` op document B
niets en eindigt non-zero — de letterlijke "frontend weigert de bron".
Terugdraaibaar in één token plus zes asserties: `Severity.WARNING` →
`Severity.ERROR` in `ExpressieBouwer.keurVouwbron` (driemaal), en in
`GenesteVouwbronTest` de assertie `assertEquals(Severity.WARNING, …)`, de
`assertTrue(m.isOk(), …)` in de `ir(...)`-helper én de losse
`assertTrue(Resolver.resolve(…).isOk(), …)` op regel 392.

### Wat er per route gebouwd is

Alle drie de checks zitten in één nieuwe methode,
`ExpressieBouwer.keurVouwbron`, aangeroepen vanuit `bouwFunctie` direct achter
de bestaande onbindbare-vouwbron-arm, en alleen voor een LEDENVOUW: een
aggregerende soort (`AANTAL, SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE`)
met precies één argument dat een `AttribuutRef` met `set: true` is.

**Het kanaal van de markering: WF-044, niet WF-034.** De markering draagt het
resultaattype dat `bouwFunctie` al berekend heeft (`Datatype type`), niet het
`Onbekend`-sentinel. `spec/IR-SPEC.md` ch. 2.4 definieert `Onopgelost` als "a
well-formed marker with a reason, not a type", en de WF-044-registerrij zegt
het met zoveel woorden: "the marker is legal wire content; the fence is
behavioural, not well-formedness". De enige corpusgetuige
(`corpus/rood/wf044-onopgelost-reached`) draagt dan ook een geldig
`Numeriek`-type en is verifier-schoon. Een geweigerde vouw hoort dus een
GELDIG document op te leveren dat luid faalt, niet een ill-formed document.
Gemeten na de omzetting: de emissie draagt `Numeriek(2 decimalen, kg)`,
`verifier: PASS`, en ref en clingo geven allebei "Onopgelost reached …
(WF-044)". De bestaande arm één blok hoger (onbekend attribuutlid) en de
acht andere `onopgelost(ctx)`-plekken in deze frontend schrijven het
sentinel nog wel; die zijn niet aangeraakt (pre-existent gedrag, andere
kaart), maar ch. 2.4 opent daar een bredere vraag — hier gemeld, niet
gekaart.

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

**Route 2 — een to-één tussenstap.** "… van alle vondsten van de boot van de
Jutter": één set-stap, daaronder een to-één stap. Dezelfde stille misbinding
langs een derde route, en de enige die noch route 1 noch route 3 ziet — de
geneste stap is geen verzameling, en de geschreven elementnaam kan de rol van
het directe feittype volkomen correct noemen. De vouwbron noemt één relatie
vanaf het REGELONDERWERP, dus een anker dat het onderwerp niet is laat zijn
stap even geruisloos vallen. De check: splits de geschreven naam op de eerste
" van ", lees de rest met dezelfde `naamKandidaten` en `objecttypeOpNaam`, en
is het eerste anker dat een OBJECTTYPE oplost niet het regelonderwerp, dan
volgt de markering. Dat is de letterlijke-ankerregel van de moederkaart, aan
de producentkant. Eén ontsnapping, met opzet: een anker dat GEEN objecttype
oplost (een rolnaam — "alle vondsten van de aanmelder", 2 van de 56
corpusvormen) blijft stil. Of een rolnaam voor het onderwerp mag staan is
precies de wire-vraag van de moederkaart, en die wordt hier niet vooruitgelopen.

**Route 3 — een type-paar-binding die de geschreven naam tegenspreekt.** De
moederkaart wijst `emit/IrEmitter.java` aan — de aanroep staat op r. 776 (767
is de Javadoc, 768 de methodekop van `bronRolBinding`)
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
- twee ontsnappingen, elk met een reden: geen type-paar-binding (dan is er
  niets stils om tegen te spreken — het document houdt zijn bestaande
  stroomafwaartse WF-048); en een geschreven naam die de OBJECTTYPENAAM zelf
  is (dan claimt de auteur geen rol en is de type-paar-afleiding de eerlijke
  terugval). Een genavigeerde bron (`navigatieRol`/`navigatiePad`) komt bij
  deze toets niet aan: die guard staat vóór routes (b) en (c) samen — de
  emitter leidt daar sowieso geen `bronRol` af.

**Eén grensgeval, gemeten.** `keurVouwbron` toetst `bron.objecttype()` (het
type van de vouwBRON), terwijl de emitter bindt op `f.elementObjecttype()`.
Bij `AANTAL` lopen die uiteen: "het aantal bakens van alle aangemelde vondsten
van de Jutter" geeft bronobjecttype `Vondst` en elementObjecttype `Baken`, en
de emitter bindt dan een andere relatie (`bebakening`/`baken`) dan de routes
beoordelen. Dat is geen stil gat: de verifier geeft er
`WF-083 FAIL … fold-source objecttype 'Vondst' is not the fold's anchor
'Baken'` op, luid en op de goede plek.

### Metingen, vóór en ná

De vier documenten zijn met de hand nagebouwd uit de moederkaart. Keten:
`Jutter` -(*bemanning*, rol *gevoerde boot*, to-many)→ `Boot` -(*berging*, rol
*geborgen vondst*, to-many)→ `Vondst.gewicht`. Casus: `J1` voert `B1` en `B2`;
`B1` bergt `V1` (3.00 kg) en `V2` (7.00 kg); `B2` bergt `V3` (5.00 kg).
Handverwachting UIT DE ZIN: **3.00**.

- **A** — de ketenzin, geen direct feittype `Jutter`↔`Vondst`.
- **B** — dezelfde zin, plus een direct feittype *vondstaanmelding* met rol
  *aangemelde vondst*, waarbij `J1` alleen `V3` heeft aangemeld. De acute
  flank.
- **C** — NIEUW in deze kaart, de rood-eerst-getuige voor route 3: hetzelfde
  model als B, maar de zin heeft maar ÉÉN stap ("… van alle geborgen vondsten
  van de Jutter"). Geen nesting; de geschreven naam en het gevonden feittype
  botsen, en de frontend bond vandaag stil.
- **F** — de rood-eerst-getuige voor route 2, op een eigen model: `Jutter`
  -(to-één *boot*)→ `Boot` -(*bootvangst*, rol *vondst*)→ `Vondst`, plus een
  DIRECT feittype *jutterbezit* met óók de rol *vondst*. De zin "… van alle
  vondsten van de boot van de Jutter" noemt de rol dus correct; alleen het
  anker verraadt de weggevallen stap. Vóór: `diagnostics: []`,
  `bronRol` = *jutterbezit*/*vondst*, `verifier: PASS`, en 5.00 waar de zin
  3.00 betekent.

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

**Ná** — A, B, C en F geven nu alle vier hetzelfde beeld:

| lens | uitkomst |
|---|---|
| frontend | precies ÉÉN WARNING; `waarde` is een `Onopgelost`-knoop met het EIGEN resultaattype (`Numeriek`, 2 decimalen, kg), geen `Functie`, geen `bronRol` |
| verifier | `verifier: PASS` |
| `ref` | `EVALUATION FAULT: Onopgelost reached in evaluation: this rule fails observably (WF-044)` |
| ASP-backend | `TRANSLATION FAULT: Onopgelost reached in translation: this rule fails observably (WF-044)` |
| Drools | `EXCLUDE transpiler SKIP: regel: laagste vondstgewicht — regel (waarde buiten scope)` |

Dat is precies de vorm van de rode zone: een WELGEVORMD document dat luid
faalt. Document B is daarmee niet meer verifier-schoon-met-fout-antwoord; er
is überhaupt geen antwoord meer, in geen van de drie uitvoerders, ongeacht
welke verwachting je erbij schrijft.

De eerste ronde van deze kaart zette de markering nog op het `Onbekend`-
sentinel, waardoor het document `WF-034 FAIL … unknown-type sentinel inside a
structured node` gaf. Ook luid, maar in het verkeerde kanaal: ch. 2.4 maakt
`Onopgelost` een markering mét reden en zonder type-claim, en WF-044's hek is
behavioraal. De omzetting was gratis — `bouwFunctie` had het type al berekend.

**De drie diagnoses, letterlijk** (severity WARNING, regelnummer = de regel
waar de aggregatie staat):

    geneste verzamelingsstap in een vouwbron: 'alle geborgen vondsten van alle
    gevoerde boten van de Jutter' stapelt de verzameling 'alle gevoerde boten
    van de Jutter' onder een tweede verzameling, en een vouwbron draagt precies
    één relatie (IR-SPEC ch. 4.3: één elementObjecttype plus één bronRol) — de
    buitenste stap zou spoorloos wegvallen, dus de aggregatie blijft onopgelost
    (§13.4.17)

    tussenstap in een vouwbron: 'alle vondsten van de boot van de Jutter'
    bereikt de elementen via Boot, maar een vouwbron draagt precies één relatie
    vanaf het regelonderwerp Jutter (IR-SPEC ch. 4.3: één elementObjecttype plus
    één bronRol) — de tussenstap zou spoorloos wegvallen, dus de aggregatie
    blijft onopgelost (§13.4.1/§13.4.17)

    vouwbron bindt niet op de geschreven naam: de zin noemt 'geborgen
    vondsten', maar het enige feittype dat Jutter met Vondst verbindt is
    'vondstaanmelding' met de rol 'aangemelde vondst' — de type-paar-afleiding
    zou een andere verzameling vouwen dan de zin noemt, dus de aggregatie
    blijft onopgelost (§13.4.1/§13.4.17)

**Rood eerst.** Alle achttien getuigen staan in één nieuw bestand,
`GenesteVouwbronTest`. Tegen de kop van vóór de kaart (de twee main-bestanden
teruggezet, de tests ongewijzigd): **7 geslaagd / 11 gefaald**. De elf die rood
staan zijn precies wat de ingreep beschrijft — drie nest-getuigen (A, B, de
gemengde keten), twee tussenstap-getuigen (F en de wire ervan), twee
naam-botsingsgetuigen (C en de wire ervan), de wire van B, het
markering-type, het niveau, en de diagnose-op-de-wire. De zeven die vóór én ná
slagen zijn de tegenproeven; die kunnen per constructie niet rood staan.

**Tegenproeven tegen over-afwijzing** (alle zeven groen vóór en ná):

| tegenproef | wat ze vasthoudt |
|---|---|
| een correcte éénstaps vouw ("alle aangemelde vondsten van de Jutter") | emitteert ongewijzigd, `kind: MINIMUM` mét `bronRol` — geen diagnose |
| dezelfde zin als F, ZONDER de tussenstap ("alle vondsten van de Jutter") | bindt en emitteert gewoon, `feittype: jutterbezit` — de ankerregel raakt de gewone éénstapsvorm niet |
| een ROLNAAM als anker ("alle vondsten van de aanmelder") | blijft buiten de weigering en houdt zijn bestaande binding — de letterlijke-ankervraag is wire-werk |
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
    check-regelspraak: PASS — JUnit suite green (205 tests, floor 173); 41 rgs golden(s) re-emit byte-identically (floor 38)
    check-toka-parity: PASS   (45 .rgs byte-identiek, vloer 45; 27 tests, vloer 27; 29 cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)

Suite 187 → 205 (+18, volledig additief; vloer 173 ongemoeid). Gate 11 is
ongewijzigd op elk getal. De overige gates zijn niet gedraaid en hoefden dat
niet: deze kaart voegt geen corpusbestand toe en wijzigt er geen, en de 41 +
45 byte-identieke re-emissies zijn het bewijs dat er geen document bewoog.
Geen gate-script is aangeraakt; geen vloer bewoog.

### Wat van de to-één-flank open blijft

De to-één tussenstap zelf is gedicht (route 2, document F). Wat OPEN blijft is
precies één deelvraag, en die is wire-werk: een anker dat geen objecttype
oplost maar een ROLNAAM is —

    … als de minimale waarde van het gewicht van alle vondsten van de aanmelder.

Daar noemt de zin het regelonderwerp niet letterlijk; of zo'n rolnaam voor het
onderwerp mag staan is de ankerregel die de moederkaart zelf wil beslechten
("het anker moet het objecttype van het onderwerp letterlijk noemen, nooit via
een rolnaam oplossen" — met het argument dat een rol wiens vuller toevallig het
eigen type is een ANDER exemplaar aanduidt). Zolang dat besluit niet valt houdt
deze frontend die vorm ongewijzigd: 2 van de 56 gemeten ledenvouwen in de vier
zones schrijven hem, en ze stil laten is de kleinste keuze — vooruitlopen zou
een besluit nemen dat aan de wire-kaart is. Gepind door de tegenproef
`een_rolnaam_als_anker_blijft_buiten_de_weigering`, zodat een latere wijziging
van die lijn zichtbaar is.

### Registerwerk voor de hoofdsessie (serieel, beschermde paden)

- **`spec/CONSTRUCTS.md` rij 4** ("Fact types and role navigation" — waar de
  `bronRol`-afleiding en de WF-047/WF-048-discipline wonen). De rij is ÉÉN
  fysieke regel: de zin hieronder gaat als ononderbroken tekst aan het eind
  van de kolom `v0→v1 delta` (de ZESDE kolom — de tabelkop is `| # |
  Construct | Origin | Verdict (v1) | Kernel or sugar? | v0→v1 delta |
  Notes |`), direct achter het huidige slot van die cel, "…is WITHDRAWN by
  this decision", zonder regeleindes, zonder blockquote.
  Hier alleen voor de leesbaarheid afgebroken:

  **Landed (2026-09-11):** de PRODUCENT van `bronRol` bindt niet langer tegen
  de geschreven rolnaam in — en dat is een ingreep in de RESOLUTIE, niet in de
  emitter: `IrEmitter.bronRolBinding` is geen letter gewijzigd, de
  RegelSpraak-frontend weigert de vouw nu STROOMOPWAARTS zodat er niets meer
  te binden valt. `verbindendeRolBinding` zoekt alleen op TYPES, dus bij
  precies één verbindend feittype won die altijd, welke rol de zin ook schreef,
  en de wire kan het niet zien: een ledenvouw heeft geen veld waarin de
  geschreven vorm van haar bron staat (de `fold_source`-herkenner kruist de
  geschreven naam alleen bij de Onbekend-getypeerde telwrat). Gemeten: een
  tweestaps-zin op een model met een toevallig direct feittype was
  `verifier: PASS` en ref, de ASP-backend en Drools antwoordden alle drie 5.00
  waar de zin 3.00 zegt — het differentiële vierkant eensluidend over de
  VERKEERDE verzameling, door geen enkele gate zichtbaar. Een emitter-fix helpt
  niet, ook gemeten: een BINDINGLOZE vouw over een uniek verbonden paar is
  verifier-schoon (WF-048 vraagt alleen naar afleidbaarheid) en elke uitvoerder
  leidt dezelfde verkeerde relatie opnieuw af — de binding weghalen maakt de
  fout onzichtbaar in plaats van weg. De frontend markeert de ledenvouw daarom
  als `Onopgelost` (WARNING, het idioom dat dezelfde methode al draagt voor een
  onbindbare vouwbron) langs DRIE routes: (a) het set-onderwerp nest zelf een
  set-stap — de v0-wire draagt één relatie, dus de buitenste stap zou spoorloos
  wegvallen; (b) er zit een TO-ÉÉN tussenstap onder de set-stap, dezelfde
  verdwijning langs een derde route en onzichtbaar voor (c) omdat de geschreven
  elementnaam de rol dan correct kan noemen — het anker moet het objecttype van
  het onderwerp letterlijk noemen, met als enige uitzondering een anker dat
  géén objecttype oplost (een rolnaam: dat is de ankerregel die de wire-kaart
  zelf beslecht, 2 van de 56 gemeten vormen); en (c) de geschreven elementnaam
  NOEMT de rol die het type-paar zou binden niet (gedeclareerd enkelvoud,
  gedeclareerd meervoud of het best-effort meervoud, exact `rolMatcht`), met
  twee ontsnappingen ernaast: geen type-paar-binding, en een geschreven naam die
  de objecttypenaam zelf is. De geschreven naam wordt in (b) en (c) beoordeeld
  onder exact de vormen waarmee het elementtype gebonden is (de
  kopnaam-splitsing van `resolveObjecttype`, nu gedeeld als `naamKandidaten`).
  De markering draagt het EIGEN resultaattype van de aggregatie, nooit het
  onbekend-sentinel: ch. 2.4 maakt `Onopgelost` een welgevormde markering met
  een reden en zonder type-claim en WF-044's hek is behavioraal, dus een
  geweigerde vouw levert een WELGEVORMD document op dat luid faalt — gemeten
  `verifier: PASS` plus "Onopgelost reached … (WF-044)" in ref én de
  ASP-backend, met een Drools-weigering ernaast: precies de rode-zonevorm.
  Gemeten over de vier zones: 56 ledenvouwen, NUL met een geschreven bron van
  twee set-stappen, dus geen wire bewoog (gate 10 41 en gate 11 45
  byte-identiek). Eén ding blijft bewust staan en is gemeten: het niveau is
  WARNING, waar ERROR — die het document helemaal niet naar schijf laat gaan —
  vrij ligt omdat GEEN bestaande test hem blokkeert, enkel zes asserties van de
  kaart zelf, gemeten 199/6 op de eindkop
  (`backlog/done/20260911-geneste-aggregatie-acute-flank.md`).

- **`spec/PLAN.md`**: geen voorgestelde wijziging. De scope van de moederkaart
  is niet veranderd — de wire-vraag staat er onverkort — en er is geen besluit
  bijgekomen dat het plan draagt.

### Wat bewust niet bewoog

- `emit/IrEmitter.java` — geen letter. De kaart wees die regel aan; de meting
  hierboven laat zien dat een fix dáár de fout onzichtbaar maakt in plaats van
  weg.
- De bestaande `onopgelost(ctx)`-plekken in deze frontend (de arm één blok
  hoger, plus acht andere) schrijven nog altijd het `Onbekend`-sentinel, en
  de documenten die zij produceren zijn dus ill-formed onder WF-034. Dat is
  pre-existent gedrag en niet van deze kaart; ch. 2.4's definitie van
  `Onopgelost` als "a well-formed marker with a reason, not a type" opent daar
  een bredere vraag. Hier gemeld, niet gekaart.
- `spec/**` in zijn geheel, `scripts/**`, `.github/**`, `corpus/**`: geen
  letter. De wire-vorm (`bronPad`, het volgende WF-nummer, de vlakke-unie-lezing,
  de diepte-vraag) is de moederkaart en is hier niet aangeraakt.
- Bestaande tests: geen enkele gewijzigd. Alle achttien nieuwe getuigen staan
  in één nieuw bestand.
- De geneste TELVORM en de setlezing buiten een vouw: allebei ongemoeid, en
  allebei met een tegenproef vastgezet zodat ze zichtbaar blijven waar ze zijn.
- Het ROLNAAM-anker ("alle vondsten van de aanmelder"): gemeten, benoemd, niet
  gedicht — dat is de ankerregel die de moederkaart zelf beslecht (zie
  hierboven). De to-één tussenstap zelf is wél gedicht, als route 2.

### Voor de hoofdsessie: re-vendor

Deze landing raakt `frontends/regelspraak`. Gewijzigde bestanden onder die
map, en alleen deze drie:

- `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java`
  (gewijzigd: één aanroep in `bouwFunctie` die het al berekende resultaattype
  meegeeft, plus `keurVouwbron` met haar drie routes, `vouwBronOnderwerp` en
  `genesteSetStap`)
- `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java`
  (gewijzigd: `naamKandidaten` uitgefactoriseerd uit `resolveObjecttype`, en
  `rolGenoemdAls` erbij)
- `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/GenesteVouwbronTest.java`
  (nieuw)
