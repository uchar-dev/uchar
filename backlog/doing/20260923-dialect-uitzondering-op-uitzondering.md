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
