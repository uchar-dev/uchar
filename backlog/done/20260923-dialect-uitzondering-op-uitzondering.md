# dialect-uitzondering-op-uitzondering: nesting, de dragervoorwaarde, `Afwijking van` en ongeordende broers onder een uitzondering (vorm 2)

Bouwkaart uit de onderzoekskaart dialect-defaults-niet-en-nesting; na de
kaarten anker-ww-herleving-dragervoorwaarde en dialect-niet-in-uitzondering
(zelfde bestanden in de resolver, dus serieel). Bronnen: het rapport buiten
de repo (`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§8.2, §8.3, §9) en het gelande voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`. Deze kaart draagt de
maintainersbesluiten van 2026-09-23.

Vandaag weigert de resolver `Uitzondering op <naam van een uitzondering>`:
`ordeningsDefaults` (`RuleChecker.java:206–283`) zoekt de drager alleen
onder gewone regels en haalt elke gelifte uitzondering uit
`resolvedRegels`. De wire-vorm `Default.excepties` (IR-SPEC ch. 6.1,
WF-075, RUNTIME-SEMANTICS ch. 7.9) en de uitvoerders dragen nesting al; het
Catala-frontend vertaalt vertakking onder de wortel al
(`frontends/catala/lib/translate.ml:28–31`). De WW vraagt vijf niveaus
(art. 15 → 20 → 21-1 → 21-2/21-3 → RHT art. 1).

## Ontwerp (maintainersbesluiten 4, 5, 6, 2026-09-23)

- 4a Vorm: `Uitzondering op <uitzonderingsnaam>: <naam>`, dezelfde kop als
  aan de wortel; de resolver zoekt de drager ook onder de al gelifte
  uitzonderingen (en onder `Afwijking van`-blokken). Lowering: een
  `ExceptionGroup` in `Default.excepties` van de drager-default,
  `ongeordend: false`, één lid met een keten van één default.
- 4b Volgorde: meerdere `Uitzondering op X` op één drager vormen één keten,
  later geschreven is hoger (zoals aan de wortel). De volgorde is
  inhoudelijk: in art. 21 moet lid 2 boven de lid 3-keten staan, anders
  herstelt de RHT-vrijstelling de herleving ook bij een nieuw recht
  (rapport §8.2, tabel).
- 4c Diepte: onbegrensd; WF-075 en de recursie van WF-050 dekken elke
  diepte. Hoogstens een linter-aanwijzing bij grote diepte.
- 4d Dragervoorwaarde: **`Uitzondering op X` is beperkend** — het frontend
  conjungeert de (zelf al geconjungeerde) rechtvaardiging van X met de
  eigen voorwaarde van de uitzondering, ook aan de wortel. De conjunctie
  neemt dus de hele keten onder X mee: in het WW-anker draagt de blokkade
  van art. 21 lid 2 ook `werkloos` van art. 15 (maintainersbesluit
  2026-09-23, besluitvraag; zonder die conjunct beslist de herleving `waar`
  voor iemand bij wie nooit een recht ontstond). De wire toont de
  conjunctie (zelfstandige rechtvaardigingen, ch. 7.1; de IR verandert
  niet). Een uitzondering die de basis *verruimt* (hardheid: "als ware
  twaalf vervangen door zes") is geen uitzondering maar een afwijking en
  krijgt de kop `Afwijking van <naam>: <naam>` met onafhankelijke
  voorwaarde. Gevolg: `jutterswet-bergingsdrempel/bron.rgs` (noodweer met
  "≥ 8" boven "≥ 20") wordt `Afwijking van`; de `ir.json` blijft
  byte-identiek. Motivering: ch. 7.1 schrijft de conjunctie voor; geen
  formalisme laat de uitzondering de voorwaarde erven (Catala: "Exceptions
  do not inherit conditionals from their base case"; Lawsky schrijft de
  insluiting in W), dus zij moet ergens staan; de juridische betekenis van
  een uitzondering is beperkend; hoek B van het anker is de stille fout van
  de andere keuze. Maintainersbesluit 2026-09-23 (besluitvraag, bevestigd):
  **beperkend met erven, plus `Afwijking van`**. Tweede besluitvraag, zelfde
  dag: **`Afwijking van` alleen met een basisregel als drager**; een
  `Afwijking van` die een uitzondering als drager noemt wordt luid
  geweigerd (een niet-ervend blok midden in de boom kan `waar` beslissen
  waar nooit een recht bestond, de fout die het WW-anker liet zien). Komt
  een wettekst die het echt nodig heeft, dan opnieuw met die tekst als
  getuige.
- 5 Overerving: `Uitzondering op` met een geërfde uitzondering als drager
  wordt luid geweigerd (SUBTYPING §4.4: een geërfde `excepties`-subboom hoort
  bij de geërfde default, het subtype adresseert er niet in). De
  uitdrukbare vorm is een eigen wortel-uitzondering van het subtype met de
  volledige voorwaarde; de melding zegt dat.
- 6 `Uitzonderingen ongeordend op <naam>:` gevolgd door `Regel`-blokken:
  één `Default.excepties` op de drager met `ongeordend: true` en één
  `ExceptionGroup` per lid (de `nestkenmerk`-vorm); onder een basis een
  group-default direct boven de basis (voorstel §10). Gelijke besluiten
  reconciliëren, verschillende geven het padgesleutelde §9.5-feit. Met
  `niet` toegelaten wordt het kenmerkconflict vanuit het dialect
  schrijfbaar; de zin in `DIVERGENCES.md` §4 dat een §9.5-conflict alleen
  op een attribuutdoel uitdrukbaar is, vervalt. **De leden erven de
  (geconjungeerde) rechtvaardiging van X**, net als een enkele
  `Uitzondering op X` (maintainersbesluit 2026-09-23, besluitvraag): één
  briefje of drie tegelijk, het blijven uitzonderingen óp X. Gevolg voor
  de nestkenmerk-tweeling: het anker `jutterswet-nestkenmerk` draagt zijn
  broers `ny`/`nz` vandaag zonder `nx`; de kaart
  anker-nestkenmerk-dragervoorwaarde voegt `nx` toe (de drie bestaande
  cases hebben `nx` en veranderen niet) en gaat aan de tweeling vooraf.
- Het gelande voorstel kent deze twee besluiten (erven onder
  `Uitzonderingen ongeordend op`, `Afwijking van` alleen op een basis) nog
  niet: de kaart neemt de proposaltekst mee als **voorstel** in dezelfde
  branch.
- Nieuwe koptokens `AFWIJKING_VAN` en `UITZONDERINGEN ONGEORDEND OP` naar
  het patroon van `UITZONDERING_OP`: whole-phrase, inert op officiële
  v2.3.0-documenten; de gereserveerde naamwoorden in `DIVERGENCES.md`
  bijwerken.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst JUnit-getuigen: nesting op twee en op vier niveaus; de
  broervolgorde; de conjunctie (de wire-conditie van een geneste
  uitzondering bevat de dragervoorwaarde); `Afwijking van` zonder
  conjunctie; `Uitzonderingen ongeordend op` met gelijke en met
  verschillende besluiten (de leden dragen de voorwaarde van X op de
  wire); de geweigerde geërfde drager; een onbekende drager; een
  `Afwijking van` op een onbekende drager; een `Afwijking van` met een
  uitzondering als drager (geweigerd).
- (b) Goldens, elk gebouwd met de penningrechtmethode
  (`spec/proposals/PHASE5D-DIFF.md` §2–3): een `bron.rgs`, de door het
  frontend uitgezonden en door de serializer van gate 2 gecanoniseerde
  `ir.json`, dezelfde cases met dezelfde absolute verwachtingen als het
  handanker, en een NOTE met de structurele diff tegen het anker (elke
  `bron` gestript, sleutels gesorteerd) en per verschil de triage:
  (i) dialectverschil, (ii) IR-ontwerpfout, (iii) frontendfout — een (ii)
  of (iii) stopt de kaart en gaat terug naar de maintainer. Groen in
  ref ≡ clingo ≡ Drools-exec: de tweeling van `anchor/ww-herleving-inkomen`
  (na de ankerkaart, die het kenmerk `recht op uitkering` bezittelijk
  maakt; tien cases (de ankerkaart voegde case5 t/m case10 toe)), van `anchor/jutterswet-uitsluiting` (drie lagen,
  ontkende middenlaag) en van `anchor/jutterswet-nestkenmerk` (ongeordende
  broers met een ontkend lid en het kenmerkconflict
  `Jutter.vrijgesteld/0`; diens `case1-y-wint` hoort met de reden van het
  anker op de overleverslijst van gate 7). Werknamen
  `ww-herleving-inkomen-rgs`, `jutterswet-uitsluiting-rgs`,
  `jutterswet-nestkenmerk-rgs`; de vorm (eigen golden, of `bron.rgs` in de
  ankermap) kiest de kaart zo dat gate 10 en de ankerregels haar dragen.
  Gate 12: `EXPECTED_PLAYED` +3 en de drie namen in klasse (A) van
  `DRL_CHANNEL` (de DRL-teksttak heeft geen defaults-lowering); beide
  scripts beschermd, dus **voorstel** op de branch.
- (b2) De registerzin voor `spec/CONSTRUCTS.md` rij 8 (de dialecttweeling
  van het WW-anker als getuige) in het Result; de hoofdsessie landt haar.
- (c) `jutterswet-bergingsdrempel/bron.rgs` naar `Afwijking van`,
  `ir.json` byte-identiek (gate 10).
- (d) `DIVERGENCES.md` §4: nesting, de conjunctie, `Afwijking van`,
  `Uitzonderingen ongeordend op`, de geërfde-dragerweigering, de
  tokenreservering; de attribuut-alleen-zin over §9.5 vervalt.
- (e) Alle veertien gates groen; gate-12-pins (played, DRL-lijst) als
  **voorstel** bijgewerkt voor de nieuwe defaults-goldens.

May touch: `frontends/regelspraak/**`, `corpus/golden/jutterswet-bergingsdrempel/bron.rgs`,
`corpus/golden/<tweelingen>/**` (of `bron.rgs` in de ankermappen —
**voorstel**), `scripts/check-drools.sh` en `scripts/check-noop.sh`
(**voorstel**: pins en overleverslijst), kaartverplaatsing. Bestaande
testbestanden alleen met toestemming van de maintainer:
`DefaultsExtensieTest.onbekende_basisregel_weigert_luid` pint de melding
"geen basisregel met die naam gevonden", die dit werk vervangt.

May touch, aanvulling: `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(**voorstel**: de twee besluiten van de tweede besluitvraag).

Niet in deze kaart: de ankerherstellen (eigen kaarten, gaan vooraf: het
WW-anker is geland, het nestkenmerk-anker volgt); de spec, de verifier,
de uitvoerders; `Regelgroep` rond een uitzondering en de
`// Bron:`-annotaties (eigen kaarten).

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Voorwaarden geland: `anker-ww-herleving-dragervoorwaarde` (tien cases,
  `recht op uitkering` bezittelijk), `anker-nestkenmerk-dragervoorwaarde`
  (VIER cases: case4-nz-zonder-nx met de instanties M/N/O; `nx` in beide
  broers; aanwezig ⇔ nx ∧ ¬ny, conflict ⇔ nx ∧ ny ∧ nz), en
  `dialect-niet-in-uitzondering` (de vier spellingen van `niet`,
  `NietInUitzonderingTest`, DIVERGENCES §4 "Form 1"; de zin dat een
  §9.5-conflict alleen op een attribuutdoel schrijfbaar is, is DAAR al
  vervangen — acceptatie (d) hoeft dat niet meer).
- Het voorstel DIALECT-DEFAULTS-NIET-EN-NESTING.md is geland op main (§5,
  §10 besluiten 4–6). §5.2 laat "Whether `Afwijking van` may also name an
  exception" nog aan deze kaart en §5.6 kent het erven onder
  `Uitzonderingen ongeordend op` nog niet: beide besluiten van de tweede
  besluitvraag komen als **voorstel** in dat bestand (alleen die passages).
- Wire-namen: `Default.excepties {ongeordend, leden}` → `Default.exceptions
  {unordered, members: [ExceptionGroup {defaults: [Default …]}]}`; `besluit`
  → `decision`; `ALLE` → `Compound {quantifier: "all", threshold: 0, parts}`;
  `drempel` → `threshold`; `bron` → `source {snippet}`;
  `PrioritizedDefaults.ongeordend` → `unordered`. De conflictsleutel
  `Jutter.vrijgesteld/0` blijft. `resolvedRegels` is frontend-intern.
- Frontend: `RuleChecker.ordeningsDefaults` r. 684–763, `groepsDefaults`
  r. 769 e.v.; `DefaultsExtensieTest.onbekende_basisregel_weigert_luid`
  r. 40–52 pint "geen basisregel met die naam gevonden";
  `frontends/regelspraak/grammar/DIVERGENCES.md` §4 (de te vervangen zin
  "Exception-on-exception (nested `Default.excepties`) is not authored from
  source in this card" r. 430–431; de gereserveerde woorden r. 435–439).
  `Uitzonderingen` en `ongeordend` zijn al tokens; nieuw is alleen de
  whole-phrase kop `AFWIJKING_VAN` en de `op <naam>`-staart van
  `Uitzonderingen ongeordend`.
- Gate-12-pins vandaag: `EXPECTED_PLAYED=188`, `EXPECTED_DRL_PLAYED=167`;
  `DRL_CHANNEL` klasse (A) luidt "ch. 7 prioritizedDefaults — the DRL-text
  branch has no defaults lowering". Gate 7: `EXPECTED_CHANNEL`-lijst in
  `scripts/check-noop.sh`. 73 rgs-goldens.
- De tweelingen komen in `corpus/golden/<naam>-rgs/` (de ankermap is
  handgeschreven en beschermd; gate 10 leest `corpus/golden/**`).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Tweelingen** `corpus/golden/ww-herleving-inkomen-rgs`,
  `jutterswet-uitsluiting-rgs`, `jutterswet-nestkenmerk-rgs`: `bron.rgs`, de
  frontend-emissie als `ir.json` (gate 10 byte-identiek, gate 2 round-trip),
  de cases van het anker byte-gelijk gekopieerd, NOTE met de structurele
  diff (elke `source` gestript, sleutels gesorteerd) en per verschil de
  triage (i)/(ii)/(iii) — (ii) of (iii) stopt de tweeling en gaat als
  genummerd open punt naar de hoofdsessie. `case1-y-wint` van de
  nestkenmerk-tweeling op de gate-7-overleverslijst met de reden van het
  anker (**voorstel**).
- U2 **Bestaande testpin.** De melding van `onbekende_basisregel_weigert_luid`
  verandert (de drager wordt ook onder uitzonderingen en afwijkingen
  gezocht); die ene assertie mag mee naar de nieuwe zin (toestemming van de
  hoofdsessie, aan de maintainer gemeld). Verder geen bestaande tests
  wijzigen: nieuwe klasse `UitzonderingOpUitzonderingTest` (en zo nodig
  `AfwijkingVanTest`).
- U3 **Conjunctie op de wire.** De rechtvaardiging van elke `Uitzondering
  op X` — ook aan de wortel — is één platte `Compound {quantifier: "all",
  threshold: 0}` met de conjuncten van X (zelf al geconjungeerd, dus de hele
  keten tot de basis) eerst en dan de eigen; een eigen voorwaarde die zelf
  een `all`-Compound is wordt platgeslagen, een andere kwantor blijft één
  conjunct; een onvoorwaardelijke uitzondering draagt alleen de
  dragerconjuncten; `source.snippet` van de Compound = de conjunctbronnen met
  " en " ertussen (zoals de ankers). `Afwijking van <basis>: <naam>` draagt
  alleen de eigen voorwaarde (de lege `all` als zij onvoorwaardelijk is) en
  lowert als vandaag een eerste-niveau `Uitzondering op`.
- U4 **Bestaande rgs-goldens met `Uitzondering op`.** Meet per golden de
  her-emissie: verandert `ir.json` niet (onvoorwaardelijke basis), dan
  niets; verandert zij door de conjunctie, dan beslist de betekenis: een
  uitzondering die de basis verruimt (bergingsdrempel) wordt `Afwijking van`
  met byte-identieke `ir.json`; een beperkende uitzondering houdt haar kop,
  krijgt de geconjungeerde `ir.json` (her-emissie), haar cases moeten
  ongewijzigd groen blijven en haar NOTE meldt het. Elke keuze per golden in
  het Result.
- U5 **Weigeringen** (elk één ERROR, bij naam, met de uitdrukbare vorm in de
  melding): onbekende drager; cyclus (met pad); `Afwijking van` met een
  uitzondering of afwijking als drager; `Uitzondering op` met een geërfde
  uitzondering als drager (SUBTYPING §4.4); drager met zowel geordende
  kinderen als een `Uitzonderingen ongeordend op`-blok; ander doel, ander
  venster, meer dan één versie (de bestaande hekken, nu per boomkant).
- U6 **Lowering** naar §5.2 van het voorstel in L5-namen: eerste niveau een
  ketenpositie; dieper `Default.exceptions {unordered: false, members:
  [ExceptionGroup [F2, F1]]}`; `Uitzonderingen ongeordend op E1` →
  `exceptions {unordered: true, members: [[U1], [U2]]}`; onder een basis een
  group-default direct boven de basis; de leden erven X's geconjungeerde
  rechtvaardiging (U3).
- U7 **Inertheid.** `Afwijking` (kop) is na deze kaart gereserveerd: sweep
  over elke bron in corpus en toka (toka: alleen tellen) dat het woord
  nergens als naamwoord voorkomt; gate 11 byte-identiek.
- U8 **Gate-pins** (**voorstel**): `EXPECTED_PLAYED` en `DRL_CHANNEL` +3
  als de tweelingen in exec spelen (meten), `EXPECTED_DRL_PLAYED`
  ongewijzigd; de gate-7-overlever (U1). De registerzin voor CONSTRUCTS rij
  8 (acceptatie (b2)) in het Result.
- U9 **Gates** voor deze bouwer: 1–8, 10, 11, 12, 13; de unie draait alle
  veertien.

## Result (2026-09-25, bouwer)

## Result — dialect-uitzondering-op-uitzondering (2026-09-25)

### Stand

Branch `card/dialect-uitzondering-op-uitzondering`, worktree `wt/dialect2`, basis main 2f47d80e, claim 99e314ff.
Kop **f054eb25**. Niet gepusht, niet gemerged. De tak raakt beschermde paden (`scripts/check-drools.sh`,
`scripts/check-noop.sh`) en is daarom als geheel een voorstel.

| commit | inhoud |
|---|---|
| d358516f | rood-eerst: `UitzonderingOpUitzonderingTest` (33 tests; 32 rood tegen de ongewijzigde frontend) |
| 20c2d324 | grammatica (`AFWIJKING_VAN`, `op <naam>`-staart), resolver als boombouw, conjunctie, weigeringen, eenmalige beoordeling van gedeelde conjuncten (+2 getuigen), javadoc `ExceptionGroup`/`PrioritizedDefaults` |
| 81046eab | bestaande goldens: bergingsdrempel -> `Afwijking van` (ir.json byte-identiek); schorsing her-emitteerd + NOTE |
| 3a01e293 | tweelingen `jutterswet-uitsluiting-rgs`, `jutterswet-nestkenmerk-rgs` + pins gate 12 en gate 7 (**voorstel**) |
| cd195c49 | `DIVERGENCES.md` §4 vorm 2; voorstel §5.2 laatste zin en §5.6 (**voorstel**) |
| f054eb25 | DIVERGENCES-tabel: plaatshouders P, Q in plaats van U1, U2 |

Tellingen: JUnit 612 (577 + 35 nieuw), geen bestaand testbestand gewijzigd. rgs-goldens 73 -> 75. Tweelingen: 2 van 3 geland
(de WW-tweeling is een nulmeting, open punt 1). Gate 12 played 188 -> 190, DRL 167 ongewijzigd; gate 7 overlevers 37 -> 38.

### Wat er gebouwd is

- **Grammatica.** `AFWIJKING_VAN : 'Afwijking' WSP 'van'` (whole-phrase, naast `UITZONDERING_OP`);
  `uitzonderingRegel : ( UITZONDERING_OP | AFWIJKING_VAN ) uitzonderingBasisNaam COLON regelNaam regelVersie+`;
  `uitzonderingGroep : UITZONDERINGEN ONGEORDEND ( OP uitzonderingBasisNaam )? COLON regel+` (bestaande tokens; geen nieuw token voor de staart).
- **Resolver** (`RuleChecker.boomDefaults`, vervangt `ordeningsDefaults`): knopen = elk `Uitzondering op`-/`Afwijking van`-blok en elk lid van een
  `Uitzonderingen ongeordend op`-blok; drager eerst onder de blokken gezocht, dan onder de gewone regels; alles opgelost vóór er iets gelift wordt;
  wortels (basisregels) met hun boom in de volgorde waarin het eerste blok van de boom staat; per kant doel/venster/versie; lift aan het eind
  (`resolvedRegels.removeIf` op identiteit). Lowering: eerste niveau ketenposities (later geschreven hoger); dieper
  `Default.exceptions {unordered: false, members: [ExceptionGroup [F2, F1]]}`; ongeordend op een blok `{unordered: true, members: [[P],[Q]]}`;
  ongeordend op een basis een groepsdefault (geen besluit, geen rechtvaardiging, geen bron) direct boven de basis.
- **Conjunctie (U3).** `Uitzondering op X` en elk lid van `Uitzonderingen ongeordend op X`: één `Compound {all, 0}` met de conjuncten van X
  (zelf geconjungeerd) voorop, dan de eigen; eigen `all` platgeslagen (op elke diepte), andere kwantor één conjunct; onvoorwaardelijk blok = alleen
  dragerconjuncten; `source.snippet` = conjunctbronnen met " en ". Draagt de drager geen conjunct (onvoorwaardelijke basis, lege `all`), dan houdt het
  blok zijn geschreven voorwaarde (daardoor blijft wrakrecht byte-identiek, U4). `Afwijking van`: alleen de eigen voorwaarde (lege `all` als
  onvoorwaardelijk), alleen onder een basis; een `Uitzondering op` onder een afwijking erft diens eigen voorwaarde.
- **Gedeelde conjuncten.** De dragerconjuncten zijn dezelfde knopen in elke rechtvaardiging die ze herhaalt. De markerveeg
  (`Resolver.meldDefaults`) en het WF-112-hek (`RuleChecker.vensterPositieDefaults`) beoordelen elke knoop nu eenmaal. Twee getuigen
  (`een_marker_in_de_basisvoorwaarde_wordt_eenmaal_gemeld`, `een_venster_onder_geen_in_de_basisvoorwaarde_weigert_eenmaal`), apart rood
  gemeten: met de ontdubbeling uit falen precies die twee (twee WF-112-ERRORs, twee onopgelost-WARNINGs voor één knoop).

### Rood-eerst (acceptatie (a))

Tegen de frontend van 99e314ff: 610 tests, 578 groen, **32 rood, alle in de nieuwe klasse**; de ene groene,
`een_onvoorwaardelijke_basis_voegt_geen_conjunct_toe`, pint gedrag dat moet blijven. Gemeten foutbeelden vóór de bouw:
`Uitzondering op <uitzondering>` -> "geen basisregel met die naam gevonden"; `Afwijking van` -> "syntaxfout: extraneous input 'Afwijking' …";
`Uitzonderingen ongeordend op X:` -> "syntaxfout: mismatched input 'op' expecting ':'"; `Uitzondering op zelf: zelf` -> de WF-050-tweedeschrijvermelding;
lid van een dragerloos ongeordend blok als drager -> "groepslid '…' leverde geen kenmerktoekenning/gelijkstelling op (§4)"; geërfde uitzondering als
drager -> "geen basisregel met die naam gevonden". Getuigen (35): nesting 2 en 4 niveaus, drager later in het document, broervolgorde op twee
niveaus, conjunctie aan de wortel, onvoorwaardelijke basis/uitzondering, andere kwantor, de platte `all` op de wire (JSON-fragment, bron met " en ",
precies twee `all`-knopen), afwijking zonder conjunct, lege `all`, gemengde keten afwijking/uitzondering, uitzondering op afwijking, ongeordend op
basis (gelijke en verschillende besluiten, groepsdefault op de wire), ongeordend op uitzondering, uitzondering op een lid, alle weigeringen hieronder,
annotatie op de geneste default (ExceptionGroup zonder bron), `testDocument` met beide nieuwe koppen, de twee ontdubbelingsgetuigen.

### De weigerzinnen, letterlijk (uit de frontenduitvoer)

Alleen frontend (geen verifier- of laderzin; geen enumwaarde of type in de zinnen). Elk één ERROR op de kop van het blok.

1. Onbekende drager: `Uitzondering op 'onbekend': geen basisregel met die naam gevonden, en ook geen uitzondering of afwijking — noem als drager een gewone platte regel (kenmerktoekenning of gelijkstelling), een 'Uitzondering op'- of 'Afwijking van'-blok, of een lid van een 'Uitzonderingen ongeordend op'-blok (dialect §4)` (bij een beslistabel of verdeling/… volgt `basisReden` direct na "afwijking", zoals voorheen). Voor een ongeordend blok dezelfde zin met kop `Uitzonderingen ongeordend op 'onbekend'`.
2. Afwijking op onbekende drager: `Afwijking van 'onbekend': geen basisregel met die naam gevonden — de drager van een afwijking is een gewone platte regel (kenmerktoekenning of gelijkstelling) (dialect §4)`
3. Kring: `Uitzondering op 'tweede': de dragers vormen een kring ('eerste' -> 'tweede' -> 'eerste') — een uitzondering hangt aan één drager, en de keten van dragers eindigt bij de basisregel die de defaults-groep draagt; een kring heeft geen basisregel. Laat één blok van de kring een gewone platte regel of een blok buiten de kring als drager noemen (dialect §4)` (zelfverwijzing: `('zelf' -> 'zelf')`).
4. Afwijking onder een niet-basis: `Afwijking van 'herroeping': 'herroeping' is een uitzondering, geen basisregel — een afwijking erft geen voorwaarde en staat daarom alleen direct boven een basisregel; midden in de boom zou zij beslissen waar de voorwaarde van haar drager niet geldt. Schrijf 'Uitzondering op herroeping: verval' als zij de voorwaarde van 'herroeping' erft, of 'Afwijking van uitsluiting: verval' met de volledige voorwaarde (dialect §4)` ("is een afwijking" / "is een lid van een 'Uitzonderingen ongeordend op'-blok" naar de soort van de drager).
5. Geërfde drager: `Uitzondering op 'uitkering van een betwist strandrecht': 'uitkering van een betwist strandrecht' is een uitzondering van Strandrecht, en uitzondering 'uitkering van een diep wrakrecht' schrijft voor het subtype Wrakrecht — een subtype erft de uitzonderingen van zijn supertype met wat eronder genest is, en adresseert er geen (RUNTIME-SEMANTICS ch. 7.10). Schrijf een eigen uitzondering van Wrakrecht op een basisregel van Wrakrecht, met de volledige voorwaarde van 'uitkering van een betwist strandrecht' en de eigen voorwaarde: de eigen uitzonderingen van Wrakrecht gaan vóór de geërfde, dus zij beslist precies waar beide gelden (dialect §4)`
6. Gemengde kinderen: `Uitzonderingen ongeordend op 'uitsluiting': de drager 'uitsluiting' heeft ook geordende uitzonderingen ('herroeping') — de kinderen van één drager vormen óf één geordende keten ('Uitzondering op', 'Afwijking van') óf de leden van één ongeordend blok, want de voorrang daartussen ligt nergens vast. Schrijf ze allemaal geordend, of neem ze allemaal als lid op in één blok 'Uitzonderingen ongeordend op uitsluiting:' (dialect §4)`; bij twee ongeordende blokken "heeft ook een ander 'Uitzonderingen ongeordend op'-blok", op de kop van het tweede.
7. Lid van een dragerloos blok als drager (eigen besluit, open punt 5a): `Uitzondering op 'uitsluiting bij schorsing': 'uitsluiting bij schorsing' is een lid van een 'Uitzonderingen ongeordend:'-blok zonder drager — zo'n lid is een eigen defaults-groep naast de andere leden en geen drager. Schrijf de leden onder een basisregel ('Uitzonderingen ongeordend op <basisregel>:'): een lid van zo'n blok kan wel drager zijn (dialect §4)`
8. Per kant, de bestaande hekken: doel `Uitzondering op 'herroeping': uitzondering 'bevoegdheid' schrijft een ander doel (Jutter.bergingsbevoegd (kenmerk)) dan haar drager 'herroeping' (Jutter.uitgesloten (kenmerk)) — een uitzondering moet hetzelfde onderwerp en kenmerk/attribuut betreffen (§4, WF-050)` (op het eerste niveau ongewijzigd "dan de basisregel (…)"); venster en versies met de teksten van de eerste kaart ("draagt een ander geldigheidsvenster dan de basisregel — …", "Uitzondering op: de uitzondering 'verval' heeft meer dan één regelversie — …"). Een geweigerde knoop houdt zijn hele boom ongelift; onder een geweigerde knoop wordt niets meer beoordeeld.

### Bestaande rgs-goldens (U4)

| golden | kop | her-emissie | beslissing |
|---|---|---|---|
| jutterswet-bergingsdrempel | Uitzondering op -> **Afwijking van** | met Uitzondering op: anders (`>= 20` vooraan); met Afwijking van: byte-identiek | verruimt (noodweer, 8 tegen 20); ir.json onaangeroerd (acceptatie (c)) |
| jutterswet-schorsing | Uitzondering op (blijft) | anders: rechtvaardiging = all[vondsten >= 12, geschorst] | beperkt (art. 4bis); ir.json her-emitteerd, NOTE-sectie "The carrier's condition"; 4/4 PASS in ref en clingo, exec PLAYED 4, DRL EXCLUDE (defaultszin) |
| jutterswet-wrakrecht | Uitzondering op (2x) | byte-identiek (onvoorwaardelijke bases) | niets |
| jutterswet-wachtgeld | Uitzonderingen ongeordend: | byte-identiek | niets |
| overige 69 | — | byte-identiek | niets |

### Tweelingen (U1) — structurele diff (elke `source` gestript, sleutels gesorteerd) en triage

- **`jutterswet-uitsluiting-rgs`** (geland). Bron = voorstel §8.3 G3 plus `// Bron:` met de ankerteksten. Buiten de groep identiek. Eén verschil (56 regels):
  anker `[waar als all[geweigerd, herroepen, vervallen]; onwaar als all[geweigerd, herroepen]; waar als geweigerd]`, tweeling
  `[onwaar als all[geweigerd, herroepen] met exceptions {unordered: false, [[waar als all[geweigerd, herroepen, vervallen]]]}; waar als geweigerd]`.
  **(i)**: derde laag genest (§5.2), zelfde beslisvolgorde, rechtvaardigingen gelijk. Verifier PASS; ref en clingo 4/4 PASS; exec PLAYED 4; DRL EXCLUDE; geen no-op-overlever.
- **`jutterswet-nestkenmerk-rgs`** (geland). Bron = voorstel §8.3 G4 plus annotaties. Eén verschil (5 regels): anker default 0 = form-a (waar als nx +
  ongeordende groepen), tweeling default 0 = groepsdefault met dezelfde groepen (onwaar als all[nx, ny]; waar als all[nx, nz]) en default 1 = waar als nx.
  **(i)**: eerste niveau buiten de basis (§5.2, ch. 7.10); conflictsleutel blijft `Jutter.vrijgesteld/0`. Verifier PASS; ref en clingo 4/4 PASS
  (case2 het feit `/0`); exec PLAYED 4; DRL EXCLUDE; `case1-y-wint` overleeft no-op (gate-7-overlever, voorstel).
- **`ww-herleving-inkomen-rgs`** (**NULMETING, niet toegelaten**; bron, emissie en tien cases in (buiten de repo)). Bron = voorstel §8.2.
  De boom is die van het anker op de vorm na (einde als ketenpositie boven de toekenning): per default dezelfde besluiten en platte all-conjuncties
  (einde all[werkloos, T]; herleving + V; blokkade + nieuw recht + nieuw maandloon > grens) — (i). Overige verschillen: eenheid `dag` met
  `canonical: "dag"`, `factorToCanonical: 1/1` (anker: alleen de afkorting) — (i); literal `0,8750` getypeerd met 3 decimalen, het product met 5
  (anker 4 en 6) — (i) naar de diff, maar met gevolg: verifier PASS, ref 10/10 PASS, exec `PLAYED 10`, DRL EXCLUDE, **clingo 10/10**
  `clingo: UNSATISFIABLE — a fail-fast guard fired: viol("arithmetic range exceeded (clingo grounds in machine integers)","W1")` (W1..W10).
  Oorzaak gemeten: de frontend typeert een literal met zijn kleinste schaal (`ExpressieBouwer.decimalenVanLiteral`, "§6.1.2 treats 0,5 and 0,50
  as the same precision"; zo ook in meldloon, vangstklem, wrakrecht en de toka-zone); clingo's gesplitste geld-maal-tariefcodering
  (`backends/clingo/lib/translate.ml`, `money_mult_shape`) herkent alleen een tarief met precies 4 decimalen, en op het gewone productpad vuurt de
  bereikshek bij maandloon 2400,00. Tegenproef: dezelfde emissie met alleen die twee annotaties op 4/6 -> clingo 10/10 PASS; met alleen de eenheid
  zonder canonical/factor -> nog steeds UNSAT. Geen (ii) of (iii) in de dialectlowering; de blokkade zit in de uitvoerder.

### Pins (beschermde paden, **voorstel**)

- `scripts/check-drools.sh`: r. 849–858 historieblok "188 -> 190 … jutterswet-uitsluiting-rgs and jutterswet-nestkenmerk-rgs …" en
  `EXPECTED_PLAYED=190` (was 188); r. 871 "seventy-four of the seventy-seven" (was seventy-two/seventy-five); r. 876–890 klasse-(A)-kop:
  28 regels, 27/26, "sitting 23 below", "The 23 is 27 exec-only players … minus 4 goldens" met jutterswet-afdrachtstapel erbij (was 26/25/24,
  22 en 3 — al onjuist sinds afdrachtstapel, gemeten: 25 exec-only min 4 DRL-only = 21 = 188 − 167); r. 904 en 910 twee klasse-(A)-regels
  `jutterswet-nestkenmerk-rgs|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch` en idem `jutterswet-uitsluiting-rgs`.
  `EXPECTED_DRL_PLAYED` (167) en `EXPECTED_CHANNEL` ongewijzigd.
- `scripts/check-noop.sh`: r. 143–150 redentekst van de twee bergingsdrempel-overlevers noemt 'Afwijking van'; r. 170–178 reden van de nieuwe
  overlever (die van het anker); r. 216 `corpus/golden/jutterswet-nestkenmerk-rgs/case1-y-wint.json` in `SURVIVORS`.

### Inertheid (U7)

`Afwijking`: corpus 75 rgs-bronnen — alleen de kop van jutterswet-bergingsdrempel; toka 47 rgs-bronnen — 0 (`Afwijking`, `Afwijking van`, `afwijking`;
ook buiten .rgs 0), alleen geteld; RegelSpraak-spec v2.3.0 (9 bestanden) — 0, alleen geteld. Gate 11 byte-identiek (45 + 2 holdouts).

### Gates (elk los na `eval $(opam env)`, op cd195c49; f054eb25 raakt alleen twee tabelcellen van DIVERGENCES, gate 1 daarop opnieuw PASS)

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 291 file(s) validate and round-trip, floor 192)
check-verifier: PASS (291 golden clean; 418 negatief rejected for their declared rule, floor 350; 260 on their declared arm (expect.txt), floor 260)
check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 675 case file(s) validate)
check-oracle: PASS (ref suite 977 worked examples, floor 489; 627 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 1726 checks, floor 83; 627 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (589 cases refute a do-nothing executor; 38 pinned survivors pin defeasible standing)
check-rood: Drools leg PASS (11 fault-pinned, 37 not-carried with reason)
check-rood: PASS (48 red cases fault observably in ref and clingo, floor 26; 96 pinned fragments cite none of the 36 listed bottom sentences)
check-regelspraak: PASS — JUnit suite green (612 tests, floor 173); 75 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS — re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-…, toka-45-…); TOKA suite 27 tests, floor 27; 29 toka cases: ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED
check-drools: PASS — played 190; channel floor 54 non-PLAYED goldens pinned by name; played matches the pinned 190; DRL-text branch played 167 (pin 167); DRL-text floor 77 non-PLAYED goldens pinned by name; JUnit 773 tests, floor 239
check-reftype-derivation: PASS (40 unit annotations twisted over 22 goldens, floor 38; 100 executor replays reproduce the untouched outcome, 21 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
```
Naamtabel: `python3 spec/proposals/v1-naamtabel-toets.py` rc 0.

### Registerzin voor `spec/CONSTRUCTS.md` rij 8 (acceptatie (b2); de hoofdsessie landt haar)

> **Landed (2026-09-25, card `dialect-uitzondering-op-uitzondering`):** form 2 of the dialect is written from source. The RegelSpraak frontend builds the `Uitzondering op` blocks as a tree under a base rule of any depth — a block may name another block as its carrier, the later-written sibling ranks higher at every level, first-level exceptions stay chain positions above the base and deeper ones lower to `Default.exceptions` — and reads `Uitzondering op X` restrictively: the justification is one flat `all` of X's already conjoined conjuncts followed by the block's own, at every level including the first (a carrier without conjuncts leaves the written condition as it is). `Afwijking van <basis>` (a new whole-phrase header) carries its own condition only and names a base rule only; `Uitzonderingen ongeordend op X:` lowers to one exception group per member, unordered, inside X or as a group-default directly above a base, each member inheriting X's justification. Refused, each with the form to write instead: an unknown carrier, a cycle (with its path), an `Afwijking van` under a non-base, a subtype's block under a node of its supertype's tree (ch. 7.10), a carrier with ordered and unordered children or two unordered blocks, and a member of a carrier-less unordered block named as a carrier. `jutterswet-bergingsdrempel` is an `Afwijking van` now, its `ir.json` byte-identical; `jutterswet-schorsing` keeps its `Uitzondering op` and was re-emitted with the base's condition conjoined, its four cases unchanged. The witnesses are the RegelSpraak twins `jutterswet-uitsluiting-rgs` (an exception on an exception) and `jutterswet-nestkenmerk-rgs` (an unordered block under a base, conflict key `Jutter.vrijgesteld/0`): each differs from its anchor only in the group's shape, in no justification or decision, and plays its anchor's cases in ref, clingo and the Drools exec branch; the DRL-text branch refuses both on its defaults sentence. The twin of `anchor/ww-herleving-inkomen` is not admitted: its tree and justifications equal the anchor's and ref and the exec branch play all ten cases, but clingo refuses the frontend's emission on a range fence — the frontend types the literal `0,8750` at its minimal scale (three decimals), and the ASP backend's split money × rate encoding recognises a four-decimal rate only. No wire, verifier or executor change.

### Wat aan de kaarttekst niet klopte (gemeten)

1. Acceptatie (b), U1, U8 en voorstel §5.2/§8.2 ([M12] "passes the anchor's cases in all three executors"): de WW-tweeling is op de échte frontend-emissie niet groen in clingo (hierboven). Gevolg: twee tweelingen, `EXPECTED_PLAYED` +2 in plaats van +3.
2. U2: de assertie van `DefaultsExtensieTest.onbekende_basisregel_weigert_luid` hoefde niet mee: dezelfde zin "geen basisregel met die naam gevonden" pinnen ook `RegelgroepUitzonderingTest.de_tabelnaam_als_basis_krijgt_de_juistere_melding` en `…een_objectcreatie_in_de_groep_als_basis_krijgt_de_juistere_melding`, die U2 niet opent. De nieuwe zin houdt die frase ("…gevonden, en ook geen uitzondering of afwijking"); geen bestaand testbestand gewijzigd.
3. U1 noemt alleen `case1-y-wint` voor gate 7; een WW-tweeling had ook de overlever `case7-invoer-staat-tegen-blokkade` van het anker nodig gehad (vervalt nu).
4. De klasse-(A)-kop van `DRL_CHANNEL` klopte al vóór deze kaart niet (22 en 3 in plaats van 21 en 4 sinds jutterswet-afdrachtstapel); in de voorstelregels hersteld.
5. Ontwerp "nieuwe koptokens … `UITZONDERINGEN ONGEORDEND OP`": zoals de correcties zeggen is alleen `AFWIJKING_VAN` nieuw; de staart gebruikt `OP`.

### Open punten

1. **WW-tweeling (nulmeting).** Artefacten in (buiten de repo) (bron.rgs, ir.json, tien cases). Toelaten vergt een uitvoerderskaart: clingo's `money_mult_shape` een literal-tarief met minder dan 4 decimalen laten herkennen (de exacte waarde 0,875 = 0,8750 op schaal 4 lezen), of het gewone productpad verbreden. Het literaltypen in de frontend veranderen raakt de toka-pariteit en meldloon/vangstklem/wrakrecht en is af te raden. Daarna: tweeling toelaten, gate-7-overlever voor haar case7, `EXPECTED_PLAYED` +1, klasse (A) +1, en de registerzin bijwerken.
2. **`corpus/golden/jutterswet-bergingsdrempel/NOTE.md`** (buiten de may-touch-set) beschrijft nog een `Uitzondering op`-blok. Voorstel: titel "ordered kenmerk deviation (Afwijking van)"; eerste alinea: het blok verruimt zijn basis (8 wrakken onder noodweer tegen 20) en draagt daarom alleen zijn eigen voorwaarde; tot de kaart dialect-uitzondering-op-uitzondering heette het `Uitzondering op`, dat sindsdien de basisvoorwaarde conjungeert; `ir.json` is onder beide koppen byte-identiek; in "Source", "Lowering" en de casetabel "exception" -> "deviation".
3. De registerzin voor CONSTRUCTS rij 8 (hierboven) landt de hoofdsessie.
4. Voorstel §5.2 ("[M12] The twin's wire of §8.2 … passes … in all three executors"), §8.2 en §8.4 ("plays 10/10") beweren het groen van de WW-tweeling; gemeten onjuist voor clingo op de frontend-emissie. Buiten mijn twee passages; voor de hoofdsessie.
5. Eigen technische keuzes, ter bevestiging: (a) een lid van een dragerloos `Uitzonderingen ongeordend:`-blok is geen drager (geweigerd; toelaten zou zijn zustergroep een keten [F, lid] maken); (b) twee `Uitzonderingen ongeordend op`-blokken op één drager geweigerd (samenvoegen kan ook); (c) draagt de drager geen conjunct, dan blijft de geschreven voorwaarde staan (geen `all` van één deel) — volgt uit U4 "onvoorwaardelijke basis: niets"; (d) `ExceptionGroup` en groepsdefault zonder `source`; (e) platslaan op elke diepte; (f) de weigering van een geweigerde knoop houdt de hele boom ongelift, zoals een familie voorheen.
6. regelspraak-java vendort de grammatica: `AFWIJKING_VAN` en de `op`-staart gaan mee met de pin-bump-kaart.
7. De positionele Regelgroep-arm zegt "Plaats het blok en zijn basisregel …" ook bij een genest blok, waar de drager een uitzondering is: onnauwkeurig, ongewijzigd gelaten.
8. De inertheid van `Afwijking van` is gemeten over de repo-bronnen en de v2.3.0-spec; de externe collecties van DIVERGENCES §6 (876 documenten) niet opnieuw.
9. Besluit 4c noemt "hoogstens een linter-aanwijzing bij grote diepte": niet gebouwd.

## Lens en fixronde (2026-09-25, hoofdsessie)

**Lens C (export van de unie 1aa057e8; oordeel EERST FIXEN).** B1 — de NOTE van `jutterswet-bergingsdrempel` beschreef nog een `Uitzondering op`-blok; B2 — het voorstel beweerde in §5.2 [M12], §8.2 en §8.4 dat de WW-tweeling in alle drie uitvoerders slaagt, wat voor de frontend-emissie in clingo onjuist is; A1 — de Regelgroep-melding zegt "zijn basisregel" bij een geneste drager; A2 — de geërfde-dragermelding noemt de uitweg niet voor een subtype zonder eigen basis; A3 — twee ERRORs op één kopregel bij weigering per lid; A4 — `ExceptionGroup` en groepsdefault zonder `source`, anders dan de ankers; A5 — normvraag: een geërfde beperkende uitzondering draagt de voorwaarde van de VERVANGEN supertypebasis (gemeten 20,00 waar het zonder conjunctie 99,00 was); A6/A7 — achterhaalde feiten en een te ruime zin over de tokenreservering; A8 — de WW-tweeling: een clingo-beperking (`money_mult_shape` eist vier decimalen; de frontend typeert `0,8750` op drie, zoals de toka-pariteit vereist), geen frontendfout; A9 — een verruimende `Uitzondering op` in bestaande documenten verandert stil van betekenis; A10–A14 — snippet met punt midden in de zin, javadoc, twee bijna gelijke wandelaars, het niet-gebouwde alternatief van keuze (a), opmaak. Bevestigd op eigen meting: de lowering tegen §5.2 en ch. 7.9 (zes proefdocumenten, alle uitvoerders gelijk), de acht weigeringen, de her-emissie van alle bestaande goldens, de tweelingen op alle acht combinaties, de pins en de inertheid (ook 513 werkbank- en 165 regelspraak-java-bronnen zonder `Afwijking`).

**Besluiten van de hoofdsessie.** B1, B2, A1–A4, A6, A7, A10–A14 gefixt; A4: `ExceptionGroup` en groepsdefault krijgen het kopsnippet als `source`, de tweelingen her-geëmitteerd; A5: standaardantwoord zo laten, één zin in DIVERGENCES, getuige zodra een golden conditionele bases op twee niveaus heeft; A8: kaart `clingo-tarief-decimalen` (backlog/new); A9: geen waarschuwing, open punt; de linter bij grote diepte (besluit 4c "hoogstens") niet gebouwd.

**Fixronde (fixer C, card/k6-fix-c, gerebaset in de unie).** B1: de NOTE van bergingsdrempel beschrijft de afwijking (het blok verruimt zijn basis; onder `Uitzondering op` zou het nooit beneden twintig wrakken beslissen — gemeten: de oude kop door de nieuwe frontend laat case2 falen); B2: het voorstel draagt in §5.2 een gedateerde [M]-noot over de clingo-weigering van de frontend-emissie (ref 10/10, exec 10/10), §5.7 noemt alleen `AFWIJKING_VAN` als nieuw token, §8.2 zet de literalrij als blokkade en noemt het naamverschil van de groep, §8.4/§8.5 "+2 nu, +1 later". A1: de Regelgroep-melding zegt "zijn drager" waar de drager een blok is; A2: de geërfde-dragermelding noemt voor een subtype zonder eigen basis welke basisregel te schrijven is; A3: één ERROR per geweigerd blok; A4: uitzonderingsgroepen en groepsdefaults dragen de koptekst als `source` ("Uitzondering op <drager>" / "Uitzonderingen ongeordend op <drager>") — de twee tweelingen her-geëmitteerd (vier source-records erbij; de gestripte diff blijft 56 en 5 regels, want het anker uitsluiting heeft geen uitzonderingsgroep en het anker nestkenmerk draagt beschrijvingen die niet uit een bron af te leiden zijn), één assertie in de nieuwe testklasse mee; A5 (standaard: zo laten): DIVERGENCES legt vast dat een geërfde beperkende uitzondering de voorwaarde van de vervangen supertypebasis houdt (gemeten 20,00; zonder conjunctie 99), getuige zodra een golden conditionele bases op twee niveaus heeft; A6, A7, A10 (geen punt midden in de samengevoegde bron), A11, A12 (`Conditie.elkConjunctEenmaal` als enige wandelaar), A13, A14 gefixt. JUnit 618. Open: A8 (kaart `clingo-tarief-decimalen`; de WW-emissie draagt sinds A4 twee source-records meer), A9 (geen waarschuwing; naar de pin-bump-kaart), een randgeval van A2 (een gewone kenmerkregel van het subtype als eigen basis), en of een frontend in regelspraak-java de koptekst-`source` ook moet emitteren (pin-bump-kaart).
