# frontend-breuk-en-navigatiekeuze: een breukliteral als rationaal op de wire, en een genavigeerde lezing kiest haar relatie of weigert in een zin

Twee producentgaten, gemeten op 2026-09-26 toen de testsuites van de oude
toolchain via uchars gate-pad (emitter → lader) liepen (rapport
`../research/toolchain-verhuizing/onderzoek-2026-09-26.md`, §2 G4).

1. **Breukliteral.** De grammatica leest RegelSpraaks rationeelgetal
   (§13.2.5, `RegelSpraakLexer.g4` r. 247: een gemengde breuk `1_1/2`), en
   de emitter schrijft dat lexeme door. De wire kent alleen een exact
   decimaal getal of een rationaal `a/b` (schema-beschrijving van Literal,
   IR-SPEC ch. 4.1); de verifier (`checks.ml`, `number_lexeme_refusal`) en
   de Drools-lader weigeren `1_1/2` en `-0_1/2` als misvormd document. De
   frontend schrijft dus een document dat haar eigen lezers weigeren.
2. **Genavigeerde lezing zonder unieke binding.** Een lezing "de prijs van
   de passagier" (rolnaam) of "de leeftijd van een Klant" (typenaam) vanaf
   een ander objecttype eindigt in de emitter met een
   `IllegalStateException` ("de lezing … navigeert, en de resolver vond
   daarvoor geen unieke binding") in plaats van een diagnose. De oude
   transpiler koos in die gevallen zelf op naam. (De proefdocumenten van
   het onderzoek dragen ©-domeinwoorden; de bouwer schrijft eigen,
   rechtenvrije Jutterswet-documenten.)

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Breuk: de emitter normaliseert een gemengde breuk naar het rationaal
  van de wire (`1_1/2` → `3/2`, `-0_1/2` → `-1/2`; gereduceerd, teken op de
  teller; de bouwer meet of `lexeme_bare_number` een teken op een rationaal
  toelaat en volgt de verifier); de auteursspelling blijft in de bron-
  snippet (`source`). Golden `jutterswet-breukliteral` (**voorstel**: een
  regel met een gemengde breuk en een met een zuivere breuk) met cases,
  gespeeld door ref en clingo; de Drools-tak speelt of weigert met reden
  in de lijst van gate 12 (de bouwer meet; een rationale literal die
  Drools niet draagt is een redendragende niet-speler, geen stille
  uitval). Eén IrEmitterTest die de normalisatie pint.
- U2 Navigatiekeuze: de resolver bindt een genavigeerde lezing op de
  geschreven rolnaam wanneer die precies één relatie tussen de twee typen
  aanwijst, ook als er meer relaties tussen die typen bestaan; een lezing
  op typenaam bindt alleen wanneer precies één relatie de typen verbindt.
  De bouwer toetst dit aan de RegelSpraak-specificatie (read-only, niet
  citeren: `/Users/yeb/Developer/uwv/regelspraak-werkbank/toolchain/
  specification/v2.3.0/`, §13.3 over rollen en relaties) en legt de
  gekozen lezing vast in `grammar/DIVERGENCES.md`.
- U3 Blijft de binding dubbelzinnig, dan een ERROR-diagnose in de
  lezerszin van de verifier voor de vorm (de bouwer zoekt de bestaande
  WF-053-/WF-109-zin die past; bestaat er geen, dan een negatief
  `wf053-…` met de nieuwe zin, rood-eerst) — nooit een exception uit de
  emitter. `LezerszinGrensTest` blijft groen (de zin staat letterlijk in
  de verifierliterals).
- U4 Getuigen, rechtenvrij: twee relaties tussen dezelfde typen met
  verschillende rolnamen (lezing op rolnaam bindt; lezing op typenaam
  weigert), één relatie (beide binden), geen relatie (weigert). Alle
  bestaande goldens met `bron.rgs` byte-identiek (gate 10); de
  pariteitszone alleen tellen (gate 11).
- U5 Klaar: alle veertien gates groen; gate 12's pins bijgewerkt voor de
  nieuwe golden; naamtabel-toets rc 0; geen regelnummerstaarten.

## Acceptatie

- (a) U1–U5; (b) een document met `1_1/2` is verifier-schoon en speelt in
  ref en clingo; (c) geen `IllegalStateException` meer voor een
  navigatie zonder binding: elke proef eindigt in een diagnose.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-breuk*`,
`corpus/negatief/wf053-*` (**voorstel**), `scripts/check-drools.sh` en
`scripts/check-noop.sh` (alleen de pins voor de nieuwe golden, **voorstel**),
`spec/IR-SPEC.md` alleen als U3 een nieuwe arm vraagt (**voorstel**). Niet in
deze kaart: de uitvoerders.

## Result (2026-09-26, bouwer)

### Ronde 1 — branch `card/frontend-breuk-en-navigatiekeuze`, kop: de commit die dit Result toevoegt, op `c972337c`

#### Stand
- Basis main `b88a8ccc`; claim `cf857c2e`; werkboom schoon (`git status --short` leeg;
  `lib/`, `gen/`, `out/`, `target/` en `_build/` zijn gitignored bouwproducten). Niets
  gepusht of gemerged, geen kaartjes gemaakt. Logs: `/tmp/golf/logs/breuk/`.
- Commits:
  1. `cf857c2e` claim.
  2. `b6c69eb7` frontend: een gemengde breuk gaat als rationaal de wire op.
  3. `7c9ed66e` frontend: een genavigeerde lezing kiest haar relatie of weigert.
  4. `a450ff6b` frontend: een lezing over een meervoudige rol weigert in een zin.
  5. `701710aa` frontend: een vouw die de wire niet spelt weigert in een zin.
  6. `c972337c` NavigatiekeuzeTest: de klassekop noemt ook de meervoudige rol en de vouw.
  7. de commit die dit Result toevoegt.
- Niet af (a): de golden `jutterswet-breukliteral` is NIET geland, omdat clingo geen
  rationale literal draagt en de uitvoerders buiten deze kaart vallen (open punt 1). De
  golden ligt klaar buiten de repo; acceptatie (b) geldt daarmee voor verifier, ref en beide
  Drools-takken, niet voor clingo.

#### U1 — de breuk
- `IrEmitter.getalLexeem` schrijft een gemengde breuk als het gereduceerde rationaal met het
  teken op de teller: `1_1/2` → `3/2`, `-0_1/2` → `-1/2`, `1_2/4` → `3/2`; een zuivere breuk
  (`3/4`) en elk ander getal gaan zoals voorheen (komma wordt punt). De bronsnippet houdt de
  auteursspelling. `lexeme_bare_number` (verifier) laat een `-` vóór de teller toe en eist een
  noemer zonder voorloopnul; dat volgt het lexeem. De tekenweigering van de resolver
  (`RuleChecker.keurTeken`) noemt hetzelfde lexeem: `teken-beperking: negative literal '-1/2'
  is written into a target under a non-negative sign restriction`.
- Gemeten vóór (het conceptdocument met de twee lexemen terug op `1_1/2` en `-0_1/2`): verifier
  `WF-081 FAIL /rules/rules/0/versions/0/value/right/value: numeric literal lexeme '1_1/2' is
  not a number of the case format (an optional '-', digits, an optional '.' and digits, or a
  rational a/b whose denominator has no leading zero — ch. 4.1)`; ref en clingo `IR LOAD
  ERROR at …` met dezelfde zin; Drools `malformed uchar-ir-v1 document at …`.
- Gemeten na, op het conceptgolden (drie regels: `maal 1_1/2`, `maal -0_1/2`, `maal 3/4`):
  verifier `PASS`; gate-2-schema, rondreis en sleutelvolgorde PASS (`check-ir-schema.sh
  <bestand>`); beide cases valide tegen `uchar-case-v0`; ref `oracle: PASS (7 expectations)`
  per case; Drools exec-model en DRL-tekst `PLAYED 2`; `--noop` exit 1 op beide cases (geen
  overlevende); een verwachting 10.50 → 10.51 faalt in ref, Drools (beide takken) en in clingo
  met de voorgestelde arm.
- Getuigen: `IrEmitterTest.gemengde_breuk_wordt_het_gereduceerde_rationaal` en
  `de_tekenweigering_noemt_het_lexeem_van_de_wire`; beide rood met de oude regel
  (`waarde.replace(',', '.')`) en groen met de nieuwe.

#### U2 — de navigatiekeuze
- Getoetst aan v2.3.0 (read-only, niet geciteerd): §13.3.9 (een rol krijgt per feittype een
  eigen naam naast de naam van het vullende type, dus een rol kan de naam van haar type dragen
  en één rolnaam kan in twee feittypen staan), §13.4.1 (een onderwerpverwijzing noemt een
  objecttype of een rol), §5.5.5 (een rol navigeert haar feittype) en §5.5.3 (een typenaam wijst
  terug naar een onderwerp van dat type). De spec zegt niet welke lezing wint bij een naam die
  beide is, en kent geen navigatie op typenaam. Vastgelegd in `grammar/DIVERGENCES.md`,
  sectie "§13.4.1 A navigated read binds its written role".
- Gebouwd: `IrEmitter.navigatieBindingen` valt, waar het typepaar niet beslist, terug op de rol
  die het geschreven zelfstandig naamwoord noemt (`RegelSpraakModel.benoemdeRolBinding`,
  gelezen op de kandidaat waarop het type werd opgelost). De rolnaam-, keten- en
  eenrelatiepaden zijn ongewijzigd; elke lezing die nu bindt, eindigde vóór deze kaart in de
  exception, dus geen bestaand document verandert (gate 10: 93 goldens, gate 11: 45 bronnen en
  de casusdump byte-gelijk).
- De vouw volgde U2 al (telbron: geschreven naam, dan het paar; setlezing: het paar, dan de
  geschreven naam met het elementtype zelf als vuller); alleen haar weigering veranderde (U3).

#### U3 — de weigering
- `RuleChecker.keurNavigatiebindingen` loopt na de lift over wat de emitter schrijft, vanaf het
  onderwerp waarvan hij elke positie schrijft, en weigert met een ERROR. De toetsen zijn die van
  de emitter: `IrEmitter.ongebondenLezing` en `IrEmitter.vouwweigering`; de emitter houdt zijn
  exception als invariant voor een met de hand gebouwd of achteraf gewijzigd model
  (`IrEmitterTest`, de mutatietests, ongewijzigd groen). Setlezingen en venstergrenzen slaat de
  naloop over: het setlezingshek en `periodeGrensFout` weigeren elk daarvan al.
- De lezerszinnen, gemeten door het document dat de emitter zou schrijven aan de verifier te
  geven:
  - lezing zonder pad van een ander type: `WF-053 FAIL …: an attribute reference without a path
    is the subject's own read, so its subjectType is the subject type 'Schuit' — 'Haven' names
    another type, which only a navigation reaches` (bestaande arm, getuigen
    `wf053-eigen-lezing-supertype` en `wf053-geleste-vouwoperand-onverbonden-hop`; dus geen
    nieuwe negatief en geen IR-SPEC-arm);
  - vouw zonder bereik: `LOAD ERROR at …: a single-argument fold with an elementType carries
    its range (sourceRole or sourcePath) under every kind but total: the range is written, never
    derived`;
  - bron met een geschreven stap: `WF-053 FAIL …: a path on a fold source is ill-formed (the
    fold's relation is its range, sourceRole or sourcePath)`;
  - totaal over een ander type: `WF-083 FAIL …: the elementType 'Haven' of a total without a
    range is not the fold subject 'Schuit': without a range the fold is the subject's own read,
    so its elementType must name the subject type`;
  - lezing over een meervoudige rol: `WF-053 FAIL …: hop 0 reaches the to-many role 'ligger' in
    a scalar slot — a to-many step needs an aggregation, never a silent first pick`.
- De frontendzinnen letterlijk (voorbeelden uit `NavigatiekeuzeTest`):
  - `genavigeerde lezing 'het kadegeld van de Haven' zonder binding (2 relaties verbinden Schuit
    met Haven, en 'de Haven' noemt geen rol die er precies één aanwijst): an attribute reference
    without a path is the subject's own read, so its subjectType is the subject type 'Schuit' —
    'Haven' names another type, which only a navigation reaches`
  - `genavigeerde lezing 'het kadegeld van de Haven' zonder binding (geen relatie verbindt
    Schuit met Haven): <dezelfde WF-053-zin>`
  - zonder lezer (de lezing noemt het eigen type): `genavigeerde lezing 'de romplengte van zijn
    sleper' zonder binding (de geschreven rol 'sleper' wijst vanaf Schuit niet precies één
    relatie aan): zonder pad zou de wire haar lezen als een eigen attribuut van het onderwerp,
    en dat is niet wat er staat`
  - `genavigeerde lezing 'de romplengte van de ligger' over een meervoudige rol: hop 0 reaches
    the to-many role 'ligger' in a scalar slot — a to-many step needs an aggregation, never a
    silent first pick`
  - `vouw 'het aantal Munten van de Jutter' zonder bereik (2 relaties verbinden Jutter met Munt,
    en 'Munten' noemt geen rol die er precies één aanwijst): a single-argument fold with an
    elementType carries its range (sourceRole or sourcePath) under every kind but total: the
    range is written, never derived`
  - `vouw 'het aantal liggers van zijn thuishaven' met een geschreven stap op haar bron: a path
    on a fold source is ill-formed (the fold's relation is its range, sourceRole or sourcePath)`
  - `vouw 'de som van zijn romplengte' over een eigen attribuut van het onderwerp, dat geen
    instanties telt: <de zin van de lader>`
  - `vouw 'het totaal van het kadegeld van de Haven' over 'kadegeld' op Haven, een ander type dan
    het onderwerp: <de WF-083-zin>`
  - ook zonder lezer: een totaal over het eigen type zonder bereik (`… : zonder bereik zou de
    wire haar lezen als het totaal van een eigen attribuut van het onderwerp`); de lijst
    "Producer refusals without a reader sentence" in DIVERGENCES is aangevuld.
- `LezerszinGrensTest` groen: elk Engels fragment staat in de verifierliterals.
- Twee uitbreidingen binnen het mandaat, omdat de motiverende proef van de kaart anders niet in
  een diagnose eindigde: (i) de feitcreatievoorwaarde op het doel ("… is een schuit van de
  Haven indien de romplengte van de schuit …", de Jutterswet-vorm van de proef uit het
  onderzoek) bindt na U2 de meervoudige rol en ging dan met `diagnostics: []` naar schijf, waar
  de verifier haar weigerde; nu weigert de naloop een lezing over een meervoudige rol met
  WF-053's zin. (ii) de vouwvormen die de emitter met een exception weigerde (geschreven stap op
  de bron, niet-totaal over het eigen attribuut) en het totaal over een ander type dat zonder
  die stap naar schijf ging, weigert de naloop nu ook; één classificatie
  (`IrEmitter.Vouwweigering`) dient emitter en resolver.

#### U4 — de getuigen (Jutterswet: Haven, Schuit, Vuurtoren)
- `NavigatiekeuzeTest` (11 tests): twee relaties, rolnaam bindt (`winterberging/winterhaven`,
  ook `de thuishaven van de Schuit`); twee relaties, typenaam weigert; twee relaties met een rol
  die de typenaam draagt (`havenligging/haven`, `de haven` en `de Haven`) bindt; één relatie,
  rolnaam en typenaam binden beide (`thuisligging/thuishaven`); geen relatie weigert; een
  geschreven rol die twee relaties van een zelfrelatie aanwijst weigert (zin zonder lezer);
  lezing over een meervoudige rol; feitcreatievoorwaarde op het doel; vouw over twee relaties
  (rolmeervoud draagt `sourceRole`, typemeervoud weigert); vier vouwvormen; en elke
  schrijfpositie (regelwaarde, regelvoorwaarde, consistentie, objectcreatiewaarde en
  -voorwaarde, feitcreatievoorwaarde, kenmerktoekenning, kwantorvouwtoets, uitzondering na de
  lift) eindigt in precies één ERROR.
- Mutatieproeven (telkens teruggezet): zonder de terugval op de rolnaam wordt
  `twee_relaties_een_rolnaam_die_de_typenaam_is_bindt` rood; zonder de naloop acht tests over
  Navigatiekeuze-, Telbereik- en SubtypeDialectTest; zonder de meervoudige-rolzin twee; zonder
  de vouwweigering drie.
- Bestaande tests bijgewerkt, omdat de weigering van de exception naar een ERROR verhuisde:
  `TelbereikTest` (twee), `SubtypeDialectTest` (één), `IrTest.aggregatie_structuur_wordt_vastgelegd`
  (de som over het eigen attribuut draagt nu haar ERROR; de structuur wordt nog steeds
  vastgelegd).
- Gate 10 en 11 byte-gelijk, zie U5.

#### U5 — gates (kop `c972337c`, elk als losse aanroep)
| gate | uitkomst |
|---|---|
| 1 | PASS |
| 2 | PASS — 307 bestanden valideren en rondreizen, 37935 objecten in schemavolgorde |
| 3 | PASS — 307 golden schoon; 481 negatief op hun regel (vloer 350), 324 op hun arm, 584 pointers |
| 4 | PASS — 752 casebestanden |
| 5 | PASS — ref-suite 1047, 709 cases exact |
| 6 | PASS — backendsuite 1878 checks, 709 cases |
| 7 | PASS — 665 cases weerleggen de no-op, 44 overlevenden |
| 8 | PASS — 43 rode cases; Drools 11 met fout, 32 met reden niet gedragen |
| 9 | PASS — 241 checks, 12 catala-goldens byte-gelijk |
| 10 | PASS — 777 tests (vloer 173), 93 rgs-goldens byte-gelijk (vloer 80) |
| 11 | PASS — de casusdump en 45 bronnen byte-gelijk (2 holdouts), 27 tests, 28 slices verifier-schoon (1 holdout), 29 cases in ref, clingo en Drools |
| 12 | PASS — played 209 = pin, DRL-tekst 179 = pin, JUnit 814 |
| 13 | PASS — 41 draaiingen over 23 goldens, 106 herhalingen, 22 in de DRL-tak |
| 14 | PASS — 150 checks, 2 flogic-goldens byte-gelijk |

- Naamtabel-toets: rc 0, `v1-naamtabel-toets: OK`.
- Gate 12's pins en `check-noop.sh` zijn niet aangeraakt: er kwam geen golden bij.
- Geen regelnummerstaarten in de zinnen; geen D- of U-nummers in code, commentaar of NOTE.

#### Voorgestelde registertekst
- `spec/CONSTRUCTS.md`, rij 4 (Fact types and role navigation), achteraan: "**Geland
  (2026-09-26, kaart `frontend-breuk-en-navigatiekeuze`):** de RegelSpraak-frontend bindt een
  genavigeerde lezing op de geschreven rolnaam wanneer die precies één relatie aanwijst, ook
  als meer relaties de twee typen verbinden; een naam die het andere type én een rol noemt telt
  als rolnaam wanneer het typepaar niet beslist; een typenaam bindt alleen bij precies één
  relatie. Wat niet bindt, een lezing over een meervoudige rol en een vouw die de wire niet
  spelt, weigert de resolver met een ERROR in de lezerszin van de verifier (WF-053, WF-083 of
  de lader), nooit meer met een exception uit de emitter (`grammar/DIVERGENCES.md` §13.4.1,
  `NavigatiekeuzeTest`). Geen wire-delta."
- De breuk is een producentfix zonder wire-delta; een CONSTRUCTS-rij stel ik niet voor.
- `spec/PLAN.md` §9, na "(`frontend-breuk-en-navigatiekeuze`)": "— in de frontend geland
  2026-09-26; de corpusgetuige `jutterswet-breukliteral` wacht op de schaal van een rationale
  literal in de ASP-backend".
- `CLAUDE.md`: geen regel (er kwam geen gate bij).

#### Open punten
1. **Golden `jutterswet-breukliteral` wacht op clingo.** De ASP-backend leest de schaal van een
   literal uit de cijfers na de punt (`translate.ml`, `lexeme_decimals`) en faalt op elke
   rationale literal met een niet-gehele waarde: `TRANSLATION FAULT: value 3/2 does not fit 0
   decimals`. Gate 6 kent geen uitzonderingslijst en de uitvoerders vallen buiten de kaart.
   Klaar buiten de repo: `/tmp/golf/results/breuk-golden/jutterswet-breukliteral/` (`bron.rgs`,
   `ir.json` via `make ir`, `case1-gevuld.json`, `case2-rand.json`, `NOTE.md`) en het
   armvoorstel `/tmp/golf/results/breuk-clingo-rationaal.patch` (lees een rationaal op de
   kleinste schaal die zijn waarde exact houdt). Met die arm, gemeten in een kladkopie: beide
   cases `clingo: PASS (7 expectations)`, backendsuite 1878 checks zonder fout, alle 709
   corpuscases door de gepatchte clingo zonder fout. Bij landing: `EXPECTED_PLAYED` 209 → 210 en
   `EXPECTED_DRL_PLAYED` 179 → 180 met een historieregel (beide takken `PLAYED 2`), geen
   kanaalvermelding, geen overlevende in `check-noop.sh`.
2. **Een rationaal zonder eindige decimaalvorm** (`1/3`, al vóór deze kaart zo geschreven):
   gemeten op "zijn taxatie maal 1/3 naar beneden afgerond op 2 decimalen" is het document
   verifier-schoon en rekent ref exact (`oracle: PASS`, 3.33), terwijl clingo weigert: zonder
   arm `value 1/3 does not fit 0 decimals`, met de arm uit punt 1 `rational literal 1/3 has no
   finite decimal expansion within 9 decimals`. Een beslissing voor de uitvoerderskaart
   (benoemde weigering in beide, of een WF-regel).
3. **Een dagsoortdefinitie die een attribuut leest** ("indien het peil van de Vuurtoren groter
   is dan 2") gaat met `diagnostics: []` naar schijf; de verifier weigert (`WF-031 … day-kind
   condition reads attribute 'peil'`). Bestond al; geen navigatievraag.
4. **"het maximum van de romplengte van de Schuit"** wordt een lezing van een attribuut
   `maximum van de romplengte` en gaat zonder diagnose naar schijf; de verifier weigert
   (`WF-034 … 'maximum van de romplengte' is neither an attribute nor a characteristic of
   'Schuit'`). Bestond al.
5. **Een tweestaps bereik vanaf een bezittelijk anker** ("de som van de romplengten van alle
   liggers van zijn thuishaven") wordt nu geweigerd met `(geen relatie verbindt Schuit met
   Schuit)`: waar voor de eenstapsafleiding die de frontend probeert, maar de zin noemt geen stap
   van wat de auteur schreef; een `sourcePath` vanaf een ander anker dan het onderwerp spelt de
   frontend niet (featurevraag). Vóór de kaart was dit de exception "geen unieke relatie van
   Schuit naar Schuit draagt haar bereik".
6. **Dekking van de naloop**: een vergelijking met een genavigeerde lezing in een
   `die/dat`-filter van een vouw zet de frontend niet om (marker met WARNING), dus de filtertak
   van de naloop heeft geen getuige uit bron; de kwantorvouwtoets wel.
7. **Normvraag, standaardantwoord toegepast**: een onbepaald onderwerp van een ander type ("de
   romplengte van een schuit" op een Haven) is in v2.3.0 een tweede universeel onderwerp; de
   frontend leest het (al vóór deze kaart) als navigatie op het typepaar, en na deze kaart bij
   een onbeslist paar ook op de rol die de naam noemt. Standaardantwoord: de geschreven naam
   beslist, zoals bij een bepaald onderwerp.
