# dialect-niet-in-uitzondering: een ontkende kenmerkbeslissing in een defaults-blok (vorm 1)

Bouwkaart uit de onderzoekskaart dialect-defaults-niet-en-nesting. Bronnen:
het rapport buiten de repo
(`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§8.1 en §9) en het gelande voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`. Deze kaart draagt de
maintainersbesluiten van 2026-09-23.

Vandaag leest het frontend `Een Recht is niet herleefd` als toekenning van
een kenmerk met de naam "niet herleefd": `NIET` is een naamwoord in
`kaleNaam` (`RegelSpraak.g4:559–566`), de kenmerktoekenning is
`onderwerpKeten (IS | HEEFT) EEN? kaleNaam` (r. 671), en
`RuleChecker.kenmerk` (r. 1533–1540) neemt de geschreven naam als doel zonder
haar op te zoeken. In een `Uitzondering op` geeft dat de misleidende
WF-050-melding "schrijft een ander doel"; in een vlakke `Regel` wordt het
document zonder melding uitgezonden en pas door de verifier geweigerd
(WF-022, "target 'niet herleefd' is neither an attribute nor a
characteristic"). De defaults-lowering zet `besluit` altijd op `TRUE`
(`RuleChecker.java:347`). De IR, de verifier en ref ≡ clingo ≡ Drools-exec
dragen `besluit: false` al (ankers `jutterswet-uitsluiting`,
`jutterswet-nestkenmerk`, `ww-herleving-inkomen`); alleen de spelling
ontbreekt.

## Ontwerp (maintainersbesluiten 1–3, 2026-09-23)

- Besluit 1: `niet` in een uitzondering is legitiem. Onderbouwing in het
  rapport §5 en §7: binnen een defaults-groep (één schrijver, totale
  voorrang, evaluatie op het fixpunt van de rechtvaardigingsstrata) is de
  ontkende conclusie de exception van Kowalski–Sadri, de superieure
  negatieve defeasibele regel van Antoniou e.a., Lawsky's negatieve default
  en Catala's `consequence not fulfilled`; de vertaling is gestratificeerd
  met één stabiel model. Ch. 5.3 blijft onaangeraakt: een `false`-besluit
  schrijft niets.
- Besluit 2 (reikwijdte): toegestaan in elke regel die een default wordt —
  het lichaam van een `Uitzondering op`, een lid van `Uitzonderingen
  ongeordend`, en de basisregel van een `Uitzondering op`-keten (Catala's
  stiltebasis; semantisch leeg onder de gesloten wereld, wel leesbaar en
  een naam om aan te hangen). Nooit in een vlakke `Regel` die geen default
  wordt: dat vraagt een tweede schrijver op één doel en een
  botsingssemantiek (rapport §5.7). Maintainersbesluit 2026-09-23
  (besluitvraag, bevestigd): de ontkende basisregel is **toegestaan**.
- Besluit 3 (spelling): bijvoeglijk `is niet X`, bezittelijk `heeft geen
  X`, zelfstandig `is geen X` en `is niet een X` — de vormen die voorwaarden
  al kennen (§8.1.8 van de spec, tabel 16), gelezen met hetzelfde
  verbatim-first-pad als de voorwaarden (`RuleChecker.java:2790–2807`): eerst
  de hele naam binden, pas dan `niet`/`geen` strippen. `is niet een X`
  parseert vandaag niet als resultaat (`EEN` is geen naamwoord): één extra
  alternatief in `kenmerkToekenning`.
- Lowering: `besluit: false` op hetzelfde doel als de drager; verder de
  bestaande `Uitzondering op`-lowering (`DIVERGENCES.md` §4).
- Weigeringen (luid, elk met een JUnit-getuige): een ontkende
  kenmerktoekenning buiten een defaults-blok, met een gerichte melding die
  §9.2 en het dialect §4 noemt (vervangt de WF-050-melding uit meting 1 én
  de stille uitzending van meting M3 van het voorstel); een ontkend kenmerk
  dat het onderwerp na het strippen niet declareert; een ontkende
  gelijkstelling bestaat niet (`moet gesteld worden op onwaar` blijft een
  Boolean-waarde, met de bekende clingo-weigering als grens).
- Een groep waarvan elke default `onwaar` beslist leidt nooit iets af:
  geen fout (welgevormd, betekent "nooit"), wel een waarschuwing van de
  RuleChecker.
- Meegenomen uit het voorstel: `testDocument` (de scenariotoetsen)
  accepteert `uitzonderingRegel` en `uitzonderingGroep` als
  documentonderdelen — het grammaticadeel van bijvangst 3; het
  scenario-harnas zelf hoort bij de regelspraak-java-kaart. En de javadoc
  van `ir/PrioritizedDefaults.java` ("the RegelSpraak resolver never builds
  it", onjuist sinds 2026-08-29) wordt gecorrigeerd.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: JUnit-getuigen voor de drie ontkenningsvormen in een
  `Uitzondering op`, voor `niet` in een vlakke `Regel` (gerichte melding),
  voor de basisregel met `niet` (toegelaten, besluit 2), voor het
  ongedeclareerde kenmerk na strippen, en voor
  de nooit-afleidende groep (waarschuwing).
- (b) Eén nieuw golden met `bron.rgs`, eigen casusmateriaal (bijvoorbeeld
  art. 15 en art. 19 lid 1 WW: `heeft geen recht op uitkering` als
  uitzondering op `heeft recht op uitkering`, of een Jutterswet-vorm),
  cases die op de basis én op de ontkende uitzondering beslissen; groen in
  gate 10 (byte-identieke heruitzending), 5, 6 en 12 (exec-tak gespeeld;
  DRL-teksttak `EXCLUDE` met reden; de played-pin en de DRL-lijst van
  `scripts/check-drools.sh` als **voorstel**).
- (c) `frontends/regelspraak/grammar/DIVERGENCES.md` §4 beschrijft de
  ontkende vorm, de reikwijdte en de weigeringen; de zin dat het frontend
  nooit `besluit: false` uitzendt vervalt.
- (d) Alle veertien gates groen; de bestaande drie extensie-goldens
  byte-identiek.

May touch: `frontends/regelspraak/**` (grammatica alleen voor het
`is niet een`-alternatief, resolver, tests), `corpus/golden/<nieuw>/**`,
`scripts/check-drools.sh` (**voorstel**: pins), kaartverplaatsing.

Niet in deze kaart: uitzonderingen op uitzonderingen, `Afwijking van`,
`Uitzonderingen ongeordend op` (kaart dialect-uitzondering-op-uitzondering);
de spec, de verifier, de uitvoerders.

## Result (2026-09-23)

**Gebouwd in `card/dialect-niet-in-uitzondering` (worktree `wt/dialect1`,
basis main 98d888ee), alle veertien gates groen op e6704bd5. Niet gepusht en
niet gemerged: `scripts/check-drools.sh` is een beschermd pad, dus de tak is
als geheel een voorstel.** Commits: `b5e0619e` (claim), `8868d701`
(rood-eerst getuigen), `b2fa5abe` (grammatica en resolver), `6c2af050`
(golden), `c706459e` (DIVERGENCES §4, javadoc), `23498818` (meldingen),
`83b021a0` (gate-12-pins, **voorstel**), `4cbd373e`, `62905d78`,
`ab0e64a2`, `bcef3dbd` en `e6704bd5` (reviewronde), plus de commit die
deze kaart naar `done/` zet.

### Wat er veranderde

- **Grammatica** (`RegelSpraak.g4`): `kenmerkToekenning` kreeg één
  alternatief, `onderwerpKeten IS NIET EEN kaleNaam` (M4: `is niet een X`
  parseerde niet als resultaat); `testDocument` kreeg `uitzonderingGroep`
  en `uitzonderingRegel` (bijvangst 3, alleen het grammaticadeel).
- **Resolver** (`RuleChecker.java`): `kenmerk` leest de naam via
  `kenmerkConditie`, het verbatim-first-pad van de voorwaarden. Bindt de
  hele naam, dan blijft de geschreven spelling het doel, zoals voorheen.
  Bindt pas de rest na `niet`/`geen`, dan krijgt de regel het
  gedeclareerde kenmerk als doel en intern de waarde `onwaar`.
  `defaultVan` maakt daarvan `besluit: false`. `keurVlakkeOntkenningen`
  weigert een ontkenning die na de lift nog in een gewone regel staat.
  `keurNooitAfleidend` waarschuwt eenmaal per kenmerkdoel waarop elke
  default `onwaar` beslist (WARNING, regel = kop van het blok, dus
  WF-113 (a)). `keurEnigeSchrijver` telt een regel uit een geweigerd
  uitzonderingsblok niet meer als tweede schrijver.
- **Javadoc** van `ir/PrioritizedDefaults.java` gecorrigeerd: de resolver
  bouwt de groep uit de eigen blokken van het dialect, met `false` bij een
  ontkenning. De javadoc van `keurRegeldoelen` zegt nu wat er met een
  ongedeclareerd kenmerk gebeurt.
- **`DIVERGENCES.md` §4**: de zin "the frontend never emits `besluit:
  false`" is vervangen. Ook de zin dat een §9.5-conflict alleen op een
  attribuut schrijfbaar is, is vervangen, want vorm 1 maakt die onwaar.
  Nieuw is de subsectie "Form 1": spellingstabel, verbatim-first,
  grammatica, reikwijdte, lowering, wat `false` wel en niet zegt,
  weigeringen, de waarschuwing en welke documenten kunnen veranderen. De
  lijst "Fail-fast refusals" noemt de nieuwe weigeringen, en de alinea over
  de tweede schrijver noemt de uitzondering voor blokleden.

### Getuigen (`NietInUitzonderingTest`, 29 JUnit-tests)

Rood-eerst op commit `8868d701`, tegen de ongewijzigde frontend: 18 van
de 21 toen geschreven tests faalden. Daarbij waren meting 1 ("schrijft een
ander doel (Jutter.niet uitgesloten (kenmerk))"), M3 (vlak: geen
diagnostiek) en M4 ("extraneous input 'een'"). Ook `testDocument` gaf
"extraneous input 'Uitzondering op'". De drie groene tests pinnen gedrag
dat moet blijven:
- een gedeclareerd `niet erkend` bindt positief;
- `moet gesteld worden op onwaar` blijft een waarde;
- `niet` in een attribuutnaam is geen ontkenning.

De getuigen uit de reviewronde zijn apart rood gemeten. Met de huidige
testklasse tegen de RuleChecker van `62905d78` (kopie buiten de repo)
falen 12 van de 29.

Wat de tests pinnen:

- **Toegelaten.**
  - De vier spellingen in een `Uitzondering op` geven `besluit`
    [false, true] op het doel van de basis.
  - Een ontkende basis is toegelaten (besluit 2): [true, false], met de
    lege ALLE-rechtvaardiging.
  - Een ontkend lid naast een positief lid in `Uitzonderingen
    ongeordend` geeft twee zustergroepen, [true] en [false].
- **Weigeringen, elk één ERROR.**
  - Een ontkenning in een vlakke `Regel` (vier spellingen), met §9.2,
    §3.5, WF-022 en "dialect §4" in de melding.
  - Dezelfde naast een defaults-groep: alleen deze melding, geen
    WF-050-melding.
  - Een regel met twee versies: één melding.
  - Een tabelrij: de melding staat op de regel van de tabel (9) en noemt
    de tabel.
  - Een ongedeclareerd kenmerk na het strippen (drie spellingen): "… is
    geen kenmerk van Jutter".
  - Een ontkend attribuut: de melding wijst naar `moet gesteld worden op
    onwaar`.
  - `is niet een` op een bijvoeglijk of bezittelijk kenmerk (Tabel 16).
  - Een lid van een geweigerd ongeordend blok telt niet als tweede
    schrijver.
- **Waarschuwing.**
  - Een keten die alleen `onwaar` beslist.
  - Een ongeordend blok dat alleen `onwaar` beslist: één waarschuwing per
    doel.
  - Geen waarschuwing als een tweede blok op hetzelfde doel `waar`
    beslist.
  - Geen waarschuwing naast een vlakke positieve schrijver; daar spreekt
    alleen WF-050.
- **Overig.**
  - De Tabel 16-registerwaarschuwing (F8) bij `is geen` op een
    bezittelijk kenmerk.
  - `testDocument` aanvaardt beide blokken.

### Golden `corpus/golden/jutterswet-schorsing`

Het bronmateriaal is de eigen Jutterswet (`wet-v4.md`). Art. 3
(penningrecht, drempel 12 voor penningjaar 2025) is de basis:
`Een Jutter heeft recht op de penning indien zijn aantal aangemelde
vondsten groter of gelijk is aan 12.` Art. 4bis is de ontkende
uitzondering: `Een Jutter heeft geen recht op de penning indien hij
vergunning geschorst is.` Beide blokken dragen een `// Bron:`-annotatie.
De `ir.json` is door de frontend uitgezonden en rondt gate 2 byte-identiek
af. Het is de eerste golden waarin de frontend `besluit: false`
uitzendt: `[{besluit false, vergunning geschorst}, {besluit true, vondsten
>= 12}]`, met lege `diagnostics`.

Er zijn vier handberekende cases. Elke case heeft een exemplaar dat de
basis toekent, zodat geen case een no-op-run overleeft:
- case1: 12 vondsten, niet geschorst → aanwezig;
- case2: 14 en geschorst → afwezig, naast 14 en niet geschorst → aanwezig;
- case3: 5 en geschorst → afwezig, naast 12 → aanwezig;
- case4: 11 → afwezig, naast 12 → aanwezig.

Metingen:
- ref: 4× `oracle: PASS`; clingo: 4× `clingo: PASS`;
- Drools exec-tak: `PLAYED 4`, zonder backendwijziging;
- DRL-teksttak: `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text
  branch has no defaults lowering`.

De NOTE legt uit waarom art. 4bis hier een uitzondering op het doel van
art. 3 is. RUNTIME-SEMANTICS ch. 7.1 modelleert "evenmin" als een eigen
doel; met één grond aan elke kant geven beide modellen hetzelfde
antwoord. De NOTE zegt ook wat besluit 4(d) aan de wire zal veranderen.

### Gates op e6704bd5 (elk los, na `eval $(opam env)`)

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 26 pattern self-test row(s); 22 shape self-test row(s); 287 file(s) validate and round-trip, floor 192)
check-verifier: PASS (287 golden clean; 444 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 662 case file(s) validate)
check-oracle: PASS (ref suite 932 worked examples, floor 489; 614 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 523 checks, floor 83; 614 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (577 cases refute a do-nothing executor; 37 pinned survivors pin defeasible standing)
check-rood: PASS (48 red cases fault observably in ref and clingo, floor 26; 96 pinned fragments cite none of the 36 listed bottom sentences)
check-catala: PASS — frontend suite green (207 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (536 tests, floor 173); 72 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS (re-emission: 45 .rgs re-emitted byte-identical, floor 45)
check-drools: PASS (played 186, pin 186; DRL-text branch played 164, pin 164; DRL-text floor 75 non-PLAYED goldens pinned by name; JUnit 679 tests, floor 239)
check-reftype-derivation: PASS (131 unit annotations twisted over 27 goldens, floor 74; 128 executor replays reproduce the untouched outcome, 25 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
check-flogic: PASS — frontend suite green (135 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
```

De drie bestaande extensie-goldens (`jutterswet-bergingsdrempel`,
`-wrakrecht`, `-wachtgeld`) zijn onaangeroerd: `git diff 98d888ee` op hun
mappen is leeg, en gate 10 zendt ze byte-identiek opnieuw uit.

### Pinwijzigingen in `scripts/check-drools.sh` (voorstel)

- r. 833–839: een historieblok "185 -> 186 … jutterswet-schorsing"
  erbij.
- r. 840: `EXPECTED_PLAYED` 185 → 186.
- r. 853: "seventy-one of the seventy-four" → "seventy-two of the
  seventy-five".
- r. 858: "(A) … — 25" → "— 26".
- r. 859: "sitting 21 below" → "22".
- r. 862: "23 of the 24" → "24 of the 25".
- r. 863: "not read as 24" → "25".
- r. 865: "The 21 is 24 exec-only players" → "The 22 is 25".
- r. 884: nieuwe regel
  `"jutterswet-schorsing|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"`
  in klasse (A) van `DRL_CHANNEL`, alfabetisch na `jutterswet-samenloop`.
- Ongewijzigd:
  - `EXPECTED_DRL_PLAYED` (r. 1071, 164);
  - `EXPECTED_CHANNEL`, want de exec-tak speelt de golden;
  - `scripts/check-noop.sh`: geen overlever.

### Registerzin voor `spec/CONSTRUCTS.md` rij 8 (voorstel, voor de hoofdsessie)

> **Landed (2026-09-23, card `dialect-niet-in-uitzondering`):** form 1 of
> the dialect is written from source. The RegelSpraak frontend reads `is
> niet X`, `heeft geen X`, `is geen X` and `is niet een X` (one new
> `kenmerkToekenning` alternative) verbatim-first, as the condition side
> does, admits the negation in every rule that becomes a default (an
> `Uitzondering op` body, an `Uitzonderingen ongeordend` member, the base of
> a chain) and lowers it to `besluit: false` on the group's target. It
> refuses a negation in a plain `Regel`, a negation whose rest is no
> characteristic of the subject, and `is niet een` on a characteristic that
> is not zelfstandig, and warns once per characteristic target on which
> every default decides `onwaar`; `testDocument` parses both extension
> blocks. Golden `jutterswet-schorsing` (Jutterswet art. 3 and 4bis) is the
> first frontend-emitted `besluit: false`: ref ≡ clingo ≡ Drools exec over
> its four cases, the DRL-text branch refusing it on its defaults sentence.
> No wire, verifier or executor change.

### Tweede lezer

Een opus55-lezer (read-only) heeft de diff tot `62905d78` beoordeeld.
Geen fout gevonden op drie punten:
- de interne `onwaar`-waarde bereikt nooit zonder ERROR de wire;
- een document zonder ontkenning verandert niet: de 71 goldens op 98d888ee
  en na de wijziging zijn byte-identiek;
- het nieuwe grammatica-alternatief laat geen bestaand document anders
  parsen.

Zijn bevindingen en wat ermee gebeurde:
- A. De waarschuwing beweerde "komt alleen uit de invoer", ook naast een
  subtyperegel of objectcreatie. Verwerkt: zij zegt nu alleen wat de
  groepen doen, en zwijgt naast een geweigerde schrijver.
- B. Een zin in een andere golden-NOTE klopt niet meer. Open punt 2.
- C. `is niet een` bond elk soort kenmerk. Verwerkt: de vorm eist nu een
  zelfstandig kenmerk.
- D. Een ontkend attribuut heette "niet gedeclareerd". Verwerkt.
- E. Bij een tabelrij stond het regelnummer uit de apart geparste rij.
  Verwerkt voor de twee nieuwe weigeringen.
- F. Dubbele meldingen bij een regel met versies en bij een lid van een
  geweigerd blok. Verwerkt.
- De tekstpunten zijn verwerkt: §3.5 tegenover §9.2 in de melding, "71
  bestaande", "zonder lookup", en de NOTE over art. 4bis.

### Open punten

1. `DefaultsExtensieTest.onbekende_basisregel_weigert_luid` is niet
   aangeraakt. De test pint "geen basisregel met die naam gevonden", de
   melding voor een onbekende basis. Die melding hoort bij vorm 2 (kaart
   `dialect-uitzondering-op-uitzondering`, die de test noemt); dit werk
   vervangt haar niet.
2. `corpus/golden/jutterswet-wachtgeld/NOTE.md:26-28` zegt nog dat een
   §9.5-conflict alleen op een attribuutdoel schrijfbaar is. Dat bestand
   valt buiten de may-touch-set van deze kaart. Voorstel: de zin
   dateren ("tot vorm 1 van het dialect, 2026-09-23") of verwijzen naar
   `DIVERGENCES.md` §4.
3. Een **positieve** kenmerktoekenning op een ongedeclareerd kenmerk
   weigert de frontend nog steeds niet; de verifier weigert haar (WF-022).
   De javadoc van `RegeldoelTest` (een testbestand, niet aangeraakt) zegt
   nog dat deze kaart de hele grond dekt. Kandidaat voor een kleine
   kaart, samen met punt 4.
4. `keurRegeldoelen` neemt bij een tabelrij het regelnummer uit de apart
   geparste rij. Dat gebeurde al vóór deze kaart (de tweede lezer heeft
   het nagemeten). De helper `tabelregel` die deze kaart toevoegt, lost
   dat op dezelfde manier op.
5. Inertheid is gemeten over alle RegelSpraak-bronnen in de repo: 71
   bestaande goldens (gate 10) en 45 bronnen van de pariteitszone (gate
   11), alle byte-identiek. De meting over externe collecties uit
   `DIVERGENCES.md` §6 (876 documenten) is niet herhaald.
6. De kaart `dialect-uitzondering-op-uitzondering` (besluit 4(d)) zal de
   `ir.json` van `jutterswet-schorsing` veranderen: de basisvoorwaarde
   wordt geconjungeerd in de rechtvaardiging van de uitzondering. De
   verwachte waarden blijven (zie de NOTE). Die kaart moet de golden dus
   opnieuw uitzenden. De attribuut-alleen-zin over §9.5, die zij in (d)
   noemt, is hier al vervangen.
7. regelspraak-java vendort deze grammatica. Het nieuwe alternatief en de
   uitbreiding van `testDocument` gaan mee met de pinbump-kaart.
