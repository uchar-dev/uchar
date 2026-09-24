# L3 — het typesysteem (M4: B14, B15, B17 (i), B18, plus D54 en D55): de derde klapkaart van v1

Derde kaart van fase 2 van `spec/proposals/V1-WIREMIGRATIE.md` (§7, de
landingstabel: L3 = M4), na L1 (`backlog/done/20260923-m1-schema-en-container.md`)
en L2 (`backlog/done/20260923-l2-referentielaag.md`, main `fd3f0a73`,
registerwerk `e5c728b9`). L3 zet het TYPESYSTEEM van de wire om en niets
anders: `Percentage` lost op in `Number` met de eenheid `%` (B14),
`DatumInDagen` en `DatumTijd` worden één `Date` met een precisie (B15), de
conversiefactor van een eenheid wordt een exacte rationaal (B17 (i)), een
parameter krijgt dezelfde tijdlijnfacet als een attribuut (B18), het
eenheidstelsel wordt een lijst met spellingen per eenheid (D54) en de
dagsoorten worden knopen met naam en meervoud (D55). Alle andere knopen,
velden en waardespellingen blijven byte-voor-byte L2 tot hun eigen landing
(L4, L5). Dit is de landing die het meest waarschijnlijk waarden van gates
5, 6 en 12 beweegt (V1-WIREMIGRATIE §7) — daarom staat hij los.

Goal: één ff-landing waarna alle veertien gates groen zijn op de L3-vorm,
alle documenten in de vier corpuszones en de pariteitszone in die vorm
staan (stand main `e5c728b9`: golden 240, negatief 400, rood 46, toka 30 =
716), de drie emitters hem schrijven, de vier consumenten hem lezen met
EXACTE rationale factoren, en de toka-zone meeverhuist (gate 11
regressievorm; README-divergentieregel L3; de 540 niet-gehele factoren van
de dump worden exact gereconstrueerd en tegen de her-emissie gemeten).

## Besluiten die deze kaart uitvoert (niet heropenen)

Uit `research/v1-klap/BESLUITENREGISTER-L2-L5.md`: de kruisende besluiten
D1–D6 (zie de L2-kaart: herstructureren = IR-SPEC-namen voor knoop en
sleutels; `bron` blijft `bron`; elke knoop draagt `node` behalve de exacte
factor; enumwaarden houden hun v0-spelling, een nieuwe enum zonder v0-
spelling krijgt de IR-SPEC-spelling; null → afwezig waar IR-SPEC optioneel
zegt; de Java-records van de frontend blijven v0), en voor L3:

- **D22** De percentagetag is `presentation: "percentage"`, alleen op
  declaratieposities (het `datatype` van attribuut, parameter en domein,
  `payload`, `element`, structveld, `FunctionDef`-parameter en -resultaat,
  lambdaparameter), nooit op een `type`-annotatie; sleutel en waarde komen
  eerst in IR-SPEC 2.4; het OPEN-punt over percentage-van blijft open.
- **D23** Een `Percentage` zonder eenheid migreert naar een `Number` met de
  ingebouwde eenheid `{abbreviation: "%", canonical: "%", factorToCanonical:
  1}` zonder `system` (de vorm die de groene dragers al hebben).
- **D24** v0-`teken: null` migreert naar `sign: "ONBEPAALD"` (v0-spelling
  tot L5); gates 5, 6 en 12 mogen daarbij niet bewegen — bewegen ze toch,
  dan stopt de landing op dat punt met de meting.
- **D25** `factorToCanonical` wordt een exacte positieve rationaal in
  laagste termen: een geheel v0-getal migreert verbatim, een niet-geheel
  v0-getal naar de rationaal met de kleinste noemer waarvan de double exact
  gelijk is aan de v0-double, anders faalt het script luid; voor de
  casus-dump wordt de uitkomst vergeleken met de her-emissie door de
  frontend. De JSON-vorm: N1, standaardantwoord `{num, den}` (U1).
- **D54** `Eenheidstelsel` → `UnitSystem {node, name, units}`: `eenheden`
  wordt een lijst in kaartvolgorde; elke alias wordt een `spelling` van de
  eenheid met dezelfde `afkorting`; een kaartsleutel die van de `afkorting`
  afwijkt wordt de eerste `spelling` van haar eenheid.
- **D55** `dayKinds` en `dagsoortPerEenheid` → één lijst `{node: "DayKind",
  name, plural?}`: de tweede registrysleutel naar dezelfde dagsoort wordt
  `plural`; de drie getuigen met een derde spelling zijn onschrijfbaar en
  worden verwijderd-met-reden; WF-017 blijft op `name` en `plural`.
- **D56/D19** Geen canonicalisatiepas (V7-terugval): gate 4 en de
  casusbestanden bewegen niet; de vijf `t`-tegen-`kg`-getuigen blijven zoals
  ze zijn.
- **D4 (b)** `Date.precision` heeft geen v0-spelling en krijgt de
  IR-SPEC-spelling `day` | `millisecond`.
- **N1** (normvraag, standaardantwoord geldt): de factor is het object
  `{num, den}` met positieve gehele getallen in laagste termen.

**Uitvoeringsbesluiten van de hoofdsessie (technisch, 2026-09-24):**

- **U1 — de exacte factor op de wire.** `factorToCanonical` is
  `{"num": integer ≥ 1, "den": integer ≥ 1}`, beide verplicht, zonder
  `node` (D3); laagste termen en positiviteit bewaakt de verifier als
  arm (c) van WF-092 (getuige `wf092-factor-niet-in-laagste-termen`) —
  het schema kan ggd niet toetsen; gate 2 krijgt zelftestrijen voor een
  factor als JSON-getal, als string en zonder `den`. Serialisatie: het
  object inline, sleutelvolgorde `num`, `den`.
- **U2 — de presentatietag alleen op declaraties.** Het schema laat
  `presentation` optioneel toe op `Number`; de verifier weigert de tag op
  elke `type`-annotatie (afgeleide positie, `Literal.type`, `Let`/`Fold`)
  als nieuwe arm van WF-007 (getuige `wf007-presentatietag-op-annotatie`).
  De migratie schrijft de tag alleen waar een v0-`Percentage` op een
  declaratiepositie stond; op annotatieposities wordt het een `Number` met
  eenheid `%` zonder tag. De emitters doen precies hetzelfde.
- **U3 — de in-memory modellen blijven v0 (D6), ook in OCaml.** Anders dan
  bij L2 verliest de OCaml-AST hier GEEN constructoren: `Numeriek`,
  `Percentage`, `DatumInDagen`, `DatumTijd` blijven in het geheugen; de
  gedeelde lader legt de wire bijectief terug (`Number` met eenheid `%` ↔
  `Percentage`, ongeacht de tag; `Date` met `day` ↔ `DatumInDagen`, met
  `millisecond` ↔ `DatumTijd`) en de emitters schrijven de L3-vorm. Reden:
  de semantiek van percentages en datums (PERCENTAGE_VAN, WF-036, de
  kalenderrekening) hangt aan die constructoren en L3 verandert geen
  betekenis. De ENIGE in-memory verandering: de factor van een eenheid wordt
  in de OCaml-AST een exacte rationaal (num, den) in plaats van `float
  option`, en elke rekenlezer (`ref/lib/arith.ml`, `ref/lib/timeline.ml`,
  `backends/clingo/lib/{normalise,translate}.ml`, `checks.ml`) rekent er
  exact mee (IR-SPEC 2.5 restatement: `factor(u)/factor(v)` als rationaal,
  één afronding aan het eind). In Java houden de records hun `Eenheid`-vorm;
  de Drools-lader zet `{num, den}` om naar de factor van het record en de
  frontend-emitter schrijft de rationaal uit de gedeclareerde vorm — de
  pre-afgeronde Drools-ratio is een gevolgde delta (CONSTRUCTS rij 26), en
  de pins van gate 12 mogen niet bewegen (alle groene factoren zijn gehele
  getallen).
- **U4 — `Unit` overal hernoemd.** `Unit` is geherstructureerd (D1: de
  factor verandert van type, `spellings` komt erbij), dus elke `Eenheid`-
  knoop — in het stelsel én op elke voorkomenspositie (`Number.unit`,
  `Literal.type`, `denominator`) — krijgt de IR-SPEC-sleutels
  `abbreviation`, `system?`, `canonical?`, `factorToCanonical?`,
  `denominator?`, `spellings?`; `spellings` staat alleen in de declaratie
  in `UnitSystem.units`; `null` wordt afwezig (D5 (a)). WF-091 vergelijkt de
  eenheid op de nieuwe sleutels byte-voor-byte, en de rationaal op `num`/
  `den`; WF-034 (r) kruist `system` tegen de gedeclareerde `UnitSystem`.
- **U5 — `Number` en `Date`.** `Number {node, whole, decimals, sign, unit?,
  presentation?}` (sleutelvolgorde = IR-SPEC-tabel; `sign` in v0-spelling
  `ONBEPAALD`/`NEGATIEF`/`NIET_NEGATIEF`/`POSITIEF`; `unit` afwezig bij
  v0-null); `Date {node, precision}`. WF-036's datumnaad zit op `precision`;
  WF-007 blijft op `whole`/`decimals`.
- **U6 — `Parameter`.** `{node, name, datatype, timeline?}` met `timeline`
  = `TimelineFacet {node, granularity, count: 1}` zonder `timelineName`
  (IR-SPEC 2.10: geen v0-parameter draagt een tijdlijnnaam); v0
  `granulariteit: null` → afwezig; `naam` → `name` (D1: de knoop is
  geherstructureerd); WF-018 kruist de facet onder WF-005.
- **U7 — `DayKind`.** `model.dayKinds` wordt `[ {node: "DayKind", name,
  plural?} ]` in de volgorde van de v0-lijst; `dagsoortPerEenheid` verdwijnt
  als sleutel; een registrysleutel gelijk aan de naam vervalt (het is de
  naam), een tweede sleutel wordt `plural`, een derde laat het script luid
  falen; WF-017 krijgt zijn armen over `/model/dayKinds/<i>/name` en
  `/plural`.
- **U8 — `UnitSystem`.** `{node: "UnitSystem", name, units: [Unit]}`; een
  `null` in `eenheden` of `aliassen` laat het script luid falen; een alias
  waarvan de eenheid niet in het stelsel staat idem.
- **U9 — de migratie `m3.py`** naar het model van `m2.py` (`migrate_bytes`,
  `--all`, `--check`, idempotent, luid met JSON-pointer), met per document
  een stderr-regel (percentages gesplitst naar declaratie/annotatie, datums
  per precisie, factoren geheel/gereconstrueerd, tekens aangevuld,
  aliassen → spellingen, dagsoortmeervouden). De rationale reconstructie:
  zoek de kleinste noemer `d ≤ 10^6` waarvoor `float(round(x·d)) / d == x`
  exact; anders luid falen. De dump: byte-identiek aan de her-emissie of
  een bevinding (C).
- **U10 — lessen van L2, bindend voor deze kaart.** (a) Tijdens de bouw
  landt de hoofdsessie GEEN andere kaart op main (de clingo-kaart midden in
  L2 kostte een bouwer zijn context aan een conflict); (b) A2 schrijft de
  spec-tekst ZELF, zonder fork — de lens leest elke gewijzigde spec-regel;
  (c) elke bouwer krijgt bij een fixronde het vondstenbestand van de lens
  als werklijst en werkt op een eigen branch vanaf de unie (disjuncte
  bestanden; de hoofdsessie rebaset serieel); (d) een bouwer die een
  betekenisverschil meet STOPT op dat punt en meldt het (de nulmeting);
  (e) elke nieuwe zin die een uitvoerder weigert, staat letterlijk gelijk in
  verifier, ref, clingo en Drools (L2 kostte twee rondes aan ongelijke
  zinnen); (f) geen besluitnummers (D/U) in IR-SPEC, RS, schema of golden-
  NOTE's — verwijs naar B-delta's, CONSTRUCTS-rijen en deze kaart.
- **U11 (L3) — de reconstructie van de factor, verfijning van D25:** de
  v0-doubles van de pariteitszone zijn producten van per stap op 16 cijfers
  afgeronde verhoudingen, dus exacte double-gelijkheid vindt de bedoelde
  breuk niet. `m3.py` neemt daarom de breuk met de KLEINSTE noemer waarvan de
  waarde binnen 10^-14 relatief van de v0-double ligt, zonder noemergrens
  (jouw gemeten alternatief: noemers 24/100/1000/1440/86400/86400000; gelijk
  aan de exacte regel overal waar die slaagt); vindt hij binnen die
  tolerantie geen breuk met noemer ≤ 10^9, dan faalt hij luid. De toets op de
  waarheid is de her-emissie van de casus-dump door de frontend, die de
  gedeclareerde `1/N`-vormen kent (bouwer C meet byte-identiteit; elk verschil
  is een bevinding). Zet die regel in de docstring en de README-zin.
- **U12 (L3) — `spellings` bevatten nooit de `abbreviation` zelf:** een alias
  die gelijk is aan de afkorting van zijn eenheid (jouw open punt 5: 30 in
  toka) wordt geen spelling; `spellings` blijft dan afwezig als er niets
  overblijft. De emitter van C past dezelfde regel toe.

## Lens-checklist — vooraf, per bouwer (de twaalf punten van L2, plus L3)

1. Elke nieuwe of veranderde wirepositie door alle vier de lezers gelezen én
   gebruikt: `Number.presentation` (alleen gelezen om te weigeren op een
   annotatie), `Date.precision`, `Unit.spellings`, de rationale factor,
   `Parameter.timeline`, `DayKind.plural`, `UnitSystem.units`.
2. Onbekende en vervallen velden bij naam geweigerd in elke lader:
   `Numeriek`, `Percentage`, `DatumInDagen`, `DatumTijd`, `Eenheid`,
   `Eenheidstelsel` als knoop; `geheel`/`decimalen`/`teken`/`eenheid`,
   `afkorting`/`stelsel`/`canoniek`/`factorNaarCanoniek`/`noemer`,
   `granulariteit` op een parameter, `dagsoortPerEenheid`, `aliassen`,
   `naam` op `Parameter`; een factor als JSON-getal of string; een
   `presentation` met een andere waarde dan `"percentage"`.
3. Geen v0-namen in meldingen, commentaren, NOTE-citaten, javadoc, en geen
   D-/U-nummers in spec, schema of NOTE's.
4. Tests die een verboden vorm pinnen herschrijven, niet verwijderen.
5. Sleutelvolgorde = schema-`properties`-volgorde voor de zes nieuwe knopen;
   v0-volgorde voor de rest; alle drie de emitters en het script schrijven
   die volgorde.
6. Elke lezer dezelfde defaults: `sign` afwezig bestaat niet (verplicht);
   `unit` afwezig = eenheidloos; `presentation` heeft geen betekenis;
   `factor` afwezig = bare unit met factor 1 (IR-SPEC 2.5); `Date` zonder
   `precision` bestaat niet.
7. Per getuige de verwachte arm in `expect.txt` (U1 van L2 geldt: elke
   herschreven of vervangen getuige).
8. Vloeren gemotiveerd; `EXPECTED_MIN_NEGATIEF` 350 blijft; de nieuwe stand
   gemeld.
9. Gate-12-pins hermeten (verwacht 188/166, geen kanaalwissel); elke
   afwijking is een nulmeting (D24, U3).
10. Gate 13: de twister sleutelt op de L3-namen (`Number`, `Date`, `unit`,
    `factorToCanonical`) en verdraait de zustereenheid als rationaal
    (`num × 10^k`); `EXPECTED_MIN_TWISTS` 38 blijft.
11. Geen stille terugval: geen lezer rekent nog met een double-factor waar
    de wire een rationaal geeft; geen lezer leidt een precisie af uit een
    datumlexeem.
12. Diagnostics en `bron` ongemoeid.

## De L3-vorm van de wire (normatief voor deze kaart)

```
Number      { "node": "Number", "whole": boolean, "decimals": integer ≥ 0,
              "sign": "ONBEPAALD"|"NEGATIEF"|"NIET_NEGATIEF"|"POSITIEF",   // v0-spelling tot L5; null → ONBEPAALD (D24)
              "unit"?: Unit,                                               // v0 eenheid; null → afwezig; Percentage zonder eenheid → de ingebouwde %
              "presentation"?: "percentage" }                              // alleen uit een v0-Percentage op een declaratiepositie (D22, U2)
Date        { "node": "Date", "precision": "day"|"millisecond" }           // DatumInDagen → day; DatumTijd → millisecond
Unit        { "node": "Unit", "abbreviation": string, "system"?: string, "canonical"?: string,
              "factorToCanonical"?: { "num": integer ≥ 1, "den": integer ≥ 1 },  // U1; laagste termen (WF-092 (c))
              "denominator"?: Unit, "spellings"?: [ string, ... ] }        // spellings alleen in de declaratie (UnitSystem.units); minItems 1
UnitSystem  { "node": "UnitSystem", "name": string, "units": [ Unit ] }   // v0 eenheden (kaart) → lijst in kaartvolgorde; aliassen → spellings (D54)
Parameter   { "node": "Parameter", "name": string, "datatype": Datatype,
              "timeline"?: { "node": "TimelineFacet", "granularity": "DAG"|…|"JAAR", "count": 1 } }  // v0 granulariteit; null → afwezig (B18)
DayKind     { "node": "DayKind", "name": string, "plural"?: string }       // model.dayKinds; dagsoortPerEenheid verdwijnt (D55)
```

`Tekst`, `Booleaan`, `Enumeratie`, `SumType`, `ListType`, `StructType`,
`Tijdlijn`, `Dimensie`, `Feittype`, `Domain`, alle regel- en
expressieknopen: byte-voor-byte L2. `Literal.type` en de afgeleide
annotaties dragen `Number`/`Date` in dezelfde vorm (zonder tag). De
datatype-unie verliest `Numeriek`, `Percentage`, `DatumInDagen`, `DatumTijd`
en krijgt `Number`, `Date`; `domainDatatype` idem. Een v0-waarde die de
L3-vorm niet kan dragen — een factor ≤ 0, een niet-reconstrueerbare double,
een derde dagsoortspelling, een `null` in een stelselkaart, een alias zonder
eenheid, een `Percentage` met een eenheid die niet `%` is — laat `m3.py`
LUID falen met bestandsnaam en JSON-pointer.

## Regels die veranderen (IR-SPEC + verifier, bouwer A2)

- **WF-092**: arm (c) erbij — `factorToCanonical` is een positieve rationaal
  in laagste termen (`den ≥ 1`, ggd(num, den) = 1); armen (a)/(b) op de
  nieuwe sleutels (`canonical` en `factorToCanonical` samen of geen van
  beide). **WF-009**: «positive rationals» wordt nu de wire-vorm zelf.
- **WF-007**: arm erbij — `presentation` alleen op een declaratiepositie
  (U2); de precisievormen ongewijzigd op `whole`/`decimals`.
- **WF-036**: de datumnaad leest `precision` («`day` and `millisecond` share
  no order»); de zes `wf095-*` en de datumgetuigen migreren mechanisch.
- **WF-034 (r)**: `system` moet een gedeclareerde `UnitSystem` noemen
  (was `stelsel`); `wf034-eenheid-onbekend-stelsel` mechanisch.
- **WF-091**: de eenheidsidentiteit op `abbreviation`/`system`/`canonical`/
  `factorToCanonical` (als rationaal) en `denominator` recursief;
  `wf091-plus-eenheid-kast` en de vijf `t`-tegen-`kg`-getuigen mechanisch.
- **WF-017**: de armen over `/model/dayKinds/<i>/name` en `/plural`; de
  registry-arm (a) vervalt met `dagsoortPerEenheid` (geschiedenisregel); de
  drie getuigen met een derde spelling verwijderd-met-reden; de andere
  `wf017-*` mechanisch op `name`/`plural`.
- **WF-018/WF-005**: de parameterfacet onder WF-005.
- **Spec-tekst**: IR-SPEC 2.4 (de deltas B14/B15 → geland; de tag; het
  OPEN-punt blijft), 2.5 (de wirevorm van `Unit`/`UnitSystem` met
  `spellings`; de rationaal; de v0-mirror-notes → geschiedenis), 2.9
  (`DayKind` geland; registry → geschiedenis), 2.10 (`timeline` geland),
  de WF-index; RUNTIME-SEMANTICS waar het `Percentage`, `DatumInDagen`/
  `DatumTijd`, `factorNaarCanoniek` of de registry als levende wirevorm
  noemt (grep; de lens leest elke regel).

## Stempellijst (V14 (a), D17, D18)

Uit `research/v1-klap/stempelmeting-negatief-l2-l5.md` §3 en §9, gemeten
op `16234880`, plus de meting van de hoofdsessie op `e5c728b9`:

| map | regel | delta | stempel | v1-vorm / reden |
|---|---|---|---|---|
| `wf092-canoniek-zonder-factor` | WF-092 | B17 (i) | herschreven (m) | `canonical: "m"` zonder `factorToCanonical` |
| `wf092-factor-zonder-canoniek` | WF-092 | B17 (i) | herschreven (m) | `factorToCanonical: {num: 1, den: 1}` zonder `canonical` |
| `wf092-noemer-zonder-factor` | WF-092 | B17 (i) | herschreven (m) | (de binding kwam in L2) |
| `wf017-eenheidsleutel-schaduwt-kalenderafkorting` | WF-017 (a) | D55 | verwijderd-met-reden | derde registryspelling: onschrijfbaar |
| `wf017-eenheidsleutel-schaduwt-kalendereenheid` | WF-017 (a) | D55 | verwijderd-met-reden | idem |
| `wf017-eenheidsleutel-schaduwt-tweede-meervoudsvorm` | WF-017 (a) | D55 | verwijderd-met-reden | idem |
| overige `wf017-*` | WF-017 | D55 | herschreven (m) | op `name`/`plural` (A2 meet per stuk de arm; `expect.txt`) |
| de zes `wf095-*` | WF-095 | B15 | herschreven (m) | `Date` met `millisecond` |
| `wf034-eenheid-onbekend-stelsel` | WF-034 (r) | U4 | herschreven (m) | `system: "Afstandt"` |
| de vijf `t`-tegen-`kg`-getuigen en `wf091-plus-eenheid-kast` | WF-091 | U4 | herschreven (m) | eenheid op de nieuwe sleutels |
| `wf091-binair-soort` | WF-091 | B14 | herschreven (m) | annotatie `Booleaan` sinds L2; geen facetverschuiving meer |
| de 14 negatieve `Percentage`-dragers zonder eenheid | divers | B14, D23 | herschreven (m) | `Number` + ingebouwde `%` (+ tag op declaraties) |
| de 7 negatief-documenten met `teken: null` (10 knopen) | divers | D24 | herschreven (m) | `sign: "ONBEPAALD"` |
| de 4 negatief-documenten met een parametergranulariteit | divers | B18 | herschreven (m) | `timeline` |
| de 53 negatief-documenten met aliassen, 15 met een afwijkende kaartsleutel | divers | D54 | herschreven (m) | `spellings` |
| het ene negatief-document met factoren 2.0 en 0.5 | divers | D25 | herschreven (m) | `{2, 1}` en `{1, 2}` |
| nieuw: `wf092-factor-niet-in-laagste-termen`, `wf007-presentatietag-op-annotatie`, `wf017-meervoud-schaduwt-kalendereenheid` (als die arm geen getuige heeft) | | U1, U2, U7 | nieuw | met NOTE en `expect.txt` |

Verwachte negatief-stand: 400 − 3 + 2 à 3 = 399 à 400; `expect.txt` mee
voor elke herschreven getuige met een L3-delta (U1 van L2). Verwijderd-
met-reden vraagt de meting dat `m3.py` erop faalt.

## Acceptatie

- (a) `python3 spec/proposals/v1-migratie/m3.py --check` meldt 0
  L2-documenten; idempotent; tellingen per zone in het Result; de dump
  byte-identiek aan de her-emissie (of een bevinding).
- (b) Gate 2: metaschema-valide; alle golden- en rood-documenten valideren
  en round-trippen; zelftestrijen voor elk vervallen veld en elke nieuwe
  vorm (checklist 2), en voor de factor als getal/string/zonder `den`;
  `EXPECTED_MIN_DOCS` 192 blijft.
- (c) Gate 3: alle golden- en rood-documenten verifier-schoon; negatief
  ≈ 399–400, elk op zijn regel én arm; vloeren 350/91 blijven tenzij de
  stand eronder komt.
- (d) Gate 4 ongewijzigd (D56).
- (e) Gates 5, 6, 7, 8: GEEN uitkomst beweegt (D24, U3); ref- en
  clingo-suites herschreven op de rationale factor met hun tellingen; de
  IR-SPEC 2.5-voorbeelden (`18 mnd` = `3/2 jr` exact) als suitegetuigen;
  geen rood-fragment noemt een L3-naam (gemeten) — een fragment dat toch
  beweegt wordt opnieuw gepind, per regel in het Result.
- (f) Gates 9, 10, 14: alle emittergoldens byte-identiek na her-emissie
  (12 catala; 72 rgs; 2 flogic) én aan de mechanische migratie; vloeren
  201/12, 173/38, 81/1 blijven.
- (g) Gate 11: casus-dump opnieuw geëmitteerd en byte-identiek aan de
  migratie (540 gereconstrueerde factoren: elk verschil is een bevinding
  over de reconstructie of de emitter); 29 slices via `m3.py`; replay
  29/29/29; README-divergentieregel L3.
- (h) Gate 12: pins 188/166 opnieuw gemeten en gelijk (geen groene factor is
  niet-geheel); JUnit zonder skips; de Drools-lader leest `{num, den}`,
  `Date.precision`, `presentation`, `spellings`, `DayKind`, `timeline` en
  weigert elk vervallen veld bij naam (`OnbekendVeldWitnessTest`-model).
- (i) Gate 13: twister op de L3-namen; 38 twists blijven; alle legs op hun
  ongetwiste regel.
- (j) Naamtabeltoets rc 0: `Number`, `Date`, `Unit`, `UnitSystem`,
  `DayKind`, `Parameter` en hun sleutels als identiteit/geland M4; de
  vervallen v0-namen geland-vervallen; `geland_telling` bijgewerkt.
- (k) Herpin `regelspraak-java` (hoofdsessie, na de landing): `IrEmitterTest`
  kantelt op `percentage_literal_lexeme_is_kaal`, twee datum-literaaltests
  en `unit_literal_lexeme_is_kaal_eenheid_in_type` (TC A.6); de records
  bewegen niet (D6, U3).

May touch (per bouwer disjunct): `spec/schema/**`, `spec/proposals/v1-migratie/**`,
`spec/proposals/v1-naamtabel.json` (+ toets), `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md` (alleen de genoemde plekken), `spec/verifier/**`,
`corpus/**`, `toka/**`, `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`frontends/**`, en van `scripts/`: `check-ir-schema.sh` (zelftestrijen),
`check-reftype-derivation.sh` (twister), `check-drools.sh` (pins, alleen bij
een gemeten wissel), `check-rood.sh` (alleen fragmenten die bewegen). NIET:
`spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `WAY-OF-WORKING.md`,
`.github/**`, `spec/proposals/V1-WIREMIGRATIE.md` (behalve de L3-rij).

Out of scope: B17 (ii) (D56), B19–B28, B6, B10, B26 (L4, L5); de
enumwaardespellingen; de `rules`-sleutels; de andere datatypes; het
OPEN-punt percentage-van; alles wat betekenis wijzigt.

## Werkverdeling

Vier bouwers, kaartbranch `card/l3-typesysteem` vanaf main (worktree
`wt/l3`); A1 eerst; daarna A2, B en C parallel op `card/l3-a2`, `card/l3-b`,
`card/l3-c` vanaf A1's kop (worktrees `wt/l3-a2`, `wt/l3-b`, `wt/l3-c`); de
hoofdsessie voegt serieel samen (ff/rebase), draait de veertien gates, zet
één lens op de unie (checklist + kaart + elke gewijzigde spec-regel),
fixronde op eigen branches vanaf de unie, herkeuring strikt op de gemelde
punten, adviesronde in dezelfde kaart zonder derde lens.

- **A1 — de wire.** Schema (de zes knopen; de unies; `model.dayKinds` als
  knooplijst; `dagsoortPerEenheid` weg; `UnitSystem`), README, gate-2-rijen,
  `m3.py` (U9), alle documenten (corpus + toka mechanisch), naamtabel.
  Sluit met gate 2 en 4 groen; rapporteert per zone de tellingen en elke
  weigering.
- **A2 — verifier en spec.** Lader/AST (U3: bijectie, rationale factor in
  de AST), de regels hierboven, IR-SPEC/RS-tekst ZELF geschreven, de
  stempels met `expect.txt`, README-sectie "L3", `check-verifier.sh` alleen
  als een vloer beweegt. Sluit met gate 3 groen.
- **B — de OCaml-kant.** `ref/lib/{arith,timeline,eval,case}.ml` en
  `ref/test`, `backends/clingo/lib/{normalise,translate}.ml` en de suite
  (exacte rationale factoren; de integer-codering van de grounder),
  catala/flogic-emitters en -suites, de twister van gate 13. Sluit met
  gates 5, 6, 7, 8, 9, 14 groen; gate 13 op de unie.
- **C — de Java-kant.** `IrEmitter` (Number/Date/Unit/UnitSystem/Parameter/
  DayKind in canonieke volgorde; de rationaal uit de gedeclareerde vorm; de
  tag alleen op declaraties), `IrLoader` (strikt; `{num, den}` → recordfactor;
  bijectie naar de records), transpilers waar zij `Eenheid`-velden of
  `Percentage`/datumtypen van de wire lezen, de getuigentests, de
  rgs-goldens en de toka-dump byte-identiek, `toka/README.md`,
  `check-drools.sh` alleen bij een gemeten wissel. Sluit met gates 10, 12,
  11 groen.

Elke bouwer: commit klein en vaak met een bouwbare branch; geen
`git add -A`; geen kaartjes; nooit pushen of mergen; CONSTRUCTS/PLAN/
CLAUDE.md nooit; loopt de lens-checklist na; eindigt met een Result-blok
(stand, tellingen, gate-uitvoer, scriptregels, open punten) — ook op zijn
contextgrens, na een commit van wat staat.

## Registerwerk bij de landing (hoofdsessie)

`spec/CONSTRUCTS.md`: rij 26 (`factorToCanonical` exact `{num,den}` geland),
rij 27 (`Percentage` → `Number` + `%`; `Date(precision)`; `Onbekend` al L2),
de knooprijen `Numeriek`/`Percentage`/`DatumInDagen`/`DatumTijd`/`Eenheid`/
`Eenheidstelsel`/`Parameter`/dagsoorten, rij 11 (parameterfacet), rij 26 de
V7-terugval (D56). `spec/PLAN.md` §9 (11): L3 geland. `V1-WIREMIGRATIE.md`:
de L3-rij (A2 zet "loopt"; de hoofdsessie GELAND) en de V7-terugval
genoteerd. `CLAUDE.md`: geen gatetekst verandert (gate 13 blijft
"afgeleide posities"). Daarna herpin en memory.

## Correcties op de kaarttekst (A1, gemeten)

Gemeten op de kaartbranch (kop `87fc726b`, na de migratie van alle zones).

- Stand: golden 241 en rood 47; met negatief 400 en toka 30 zijn dat 718
  documenten (de kaart noemde golden 240 en rood 46, samen 716).
- "de 540 niet-gehele factoren van de dump": de 540 zijn verspreid over de
  hele toka-zone, 18 per document; de casus-dump zelf draagt er 18.
- `wf034-dagsoorteenheid-onbekende-dagsoort` is een vierde onschrijfbare
  getuige: zijn registrywaarde noemt een niet-gedeclareerde dagsoort, en arm
  (s) van WF-034 verliest met het register zijn object. A2 stempelt hem
  verwijderd-met-reden.
- Nulmeting D24: 45 knopen kregen `sign: "ONBEPAALD"` uit een v0-null (golden
  35 in 6 documenten, negatief 10 in 7, rood 0, toka 0); 21 knopen kregen
  `ONBEPAALD` uit een `Percentage` (golden 6, negatief 14, rood 1), en sinds
  de toka-zone migreert komen daar 90 in toka bij.

## Result

**Geland (L3 van de v1-klap, 2026-09-24; basis main `6166f382` — L1 en L2
eronder; de landingscommit is deze kaartverplaatsing zelf, unie `c1a7a87b`).**
Vier bouwers (A1 wire, A2 verifier/spec, B OCaml, C Java), twaalf
uitvoeringsbesluiten van de hoofdsessie (U1–U12) bovenop de besluiten D22–D25,
D54–D56 en het standaardantwoord op N1; één lensronde op unie 1
(`c140ccb0`: EERST FIXEN — B1–B5 en A1–A15, alle in dezelfde kaart gefixt
op vier branches), de herkeuring door een verse lens op unie 2
(`f12b45cc`): MERGEN — zes restvondsten als advies, in de adviesronde afgehandeld. Alle veertien gates groen op de unie, die op de
main-tip stond.

**De wire.** `Number {node, whole, decimals, sign, unit?, presentation?}`
(B14: `Percentage` weg — een percentage is een `Number` met de eenheid `%`,
de ingebouwde `%` voor een v0-percentage zonder eenheid, de tag
`presentation: "percentage"` alleen op declaraties; `sign` verplicht, een
v0-`teken: null` werd `ONBEPAALD`); `Date {node, precision: day |
millisecond}` (B15); `Unit {node, abbreviation, system?, canonical?,
factorToCanonical?: {num, den}, denominator?, spellings?}` met de exacte
rationaal in laagste termen (B17 (i), N1; `num` en `den` ten hoogste
2^62−1, schemagrens) en `spellings` alleen op de declaratie; `UnitSystem
{node, name, units}` (D54: de kaarten werden een lijst, aliassen en
afwijkende kaartsleutels werden spellingen, nooit de afkorting zelf);
`Parameter {node, name, datatype, timeline?}` (B18); `DayKind {node, name,
plural?}` in `model.dayKinds`, `dagsoortPerEenheid` weg (D55). De
datatype-unie zonder `Numeriek`, `Percentage`, `DatumInDagen`, `DatumTijd`.
Alle andere knopen en de enumwaarden byte-voor-byte L2. In-memory bleven
de v0-constructoren in elke lezer (U3: bijectie); de in-memory
veranderingen zijn de exacte rationale factor in de OCaml-rekenlezers en de
lezing van een afgeleid `%`-resultaat als percentage (B1, hieronder).

**Migratie.** `spec/proposals/v1-migratie/m3.py` (`migrate_bytes`,
`--all`, `--check`, idempotent, luid met JSON-pointer): 714 van 718
documenten mechanisch (golden 241, negatief 396, rood 47, toka 30); de vier
weigeringen zijn de onschrijfbare getuigen (drie `wf017-eenheidsleutel-
schaduwt-*` met een derde dagsoortspelling; `wf034-dagsoorteenheid-
onbekende-dagsoort`); `--check` op de unie: 0 over 725 documenten. U11: de
niet-gehele v0-factoren van de pariteitszone zijn producten van per stap
afgeronde verhoudingen, zodat de exacte-double-regel van D25 op 360 van 540
faalde; het script neemt de breuk met de kleinste noemer binnen 10^-14
relatief (noemers 24: 60, 100: 90, 1000: 90, 1440: 180, 86400: 60,
86400000: 60; de docstring noemt de garantiegrens p·q < 5·10^13, alle 542
niet-gehele factoren liggen eronder, de grootste p·q is 86 400 000), en de
her-emissie van de casus-dump door de frontend uit de gedeclareerde
`1/N`-vormen is BYTE-IDENTIEK aan die reconstructie — de toets van U11.
Tellingen: `Number` 1924/1787/305/1134 (golden/negatief/rood/toka); `sign`
uit null 35 (6 documenten)/10 (7)/0/0 (D24, nulmeting: geen uitkomst
bewoog); percentages op een declaratie met tag 2/14/0/90, op een annotatie
zonder tag 4/0/1/0; datums day/millisecond 99/25, 101/22, 54/0, 128/213;
factoren geheel/gereconstrueerd 294/0, 720/2, 38/0, 940/540; aliassen →
spellingen 70/162/9/540 (30 toka-aliassen gelijk aan de afkorting
vervielen, U12); kaartsleutels → spelling 16/15/2/30; parameterfacetten
3/4/1/0; dagsoortmeervouden 7/8/5/30.

**Regels.** WF-092: armen (a)/(b) benoemd, arm (c) laagste termen en
positiviteit; WF-009 leest de factor niet meer, lost `canonical` op
`abbreviation` op en kreeg de arm "one Unit per abbreviation" (de zin
`the unit '<a>' is declared twice in the unit system '<s>': one Unit per
abbreviation (WF-009)`, letterlijk gelijk in Drools onder `Namen.norm`; de
gedeelde OCaml-lader blijft laden — een verifier-vuil document bereikt ref
en clingo niet); WF-007: de tag alleen op een declaratie; WF-017:
naamclausule, arm (a) meervoud tegen kalender (ook tegen een
kalenderafkorting), arm (b) meervoud tegen naam én meervoud, de
registerarmen → geschiedenis; WF-034 (r) op `system`, (s) → geschiedenis;
WF-005 eist `count = 1` op een parameterfacet zolang RS 3.12 multi-period
open laat (ref en clingo lezen het periodetal nergens) en WF-018 de
parameterfacet; WF-036 op `precision`; WF-091 op `num`/`den` en leidt voor
een afgeleid `%`-resultaat een percentage af (B1: `% ± %`, `% × n`, TOTAAL
en MIN/MAX over percentages — de lezing van de lader, zodat de verifier de
eigen emissie van de frontend aanvaardt; op het corpus verandert geen
rapport); WF-001 met de labels `'unitSystems'`/`'dayKinds'`; WF-081/084/
095/111 en de index mee; IR-SPEC 2.1 (naam op `Parameter`/`UnitSystem`, de
€-kaartsleutels → geschiedenis), 2.4 (B14/B15 geland; de
betekenisverfijning op de afgeleide positie), 2.5 (de wirevorm van `Unit`/
`UnitSystem`, de rationaal, de v0-mirror-notes → geschiedenis; `spellings`
als OPEN-punt), 2.9 (`DayKind`, de botsingstabel gesloten), 2.10
(`timeline`); RUNTIME-SEMANTICS op de genoemde regels (3.12 één zin);
V7-terugval in het voorstel; het OPEN-punt percentage-van blijft open.

**Stempels (V14 (a)), lijst in `corpus/negatief/README.md` sectie "L3":**
4 verwijderd-met-reden (de m3-weigering aangehaald), 101 herschreven (m),
11 nieuw (`wf092-factor-niet-in-laagste-termen`, `wf007-presentatietag-op-
annotatie`, `wf017-meervoud-schaduwt-kalendereenheid`, `wf017-meervoud-
schaduwt-tweede-meervoudsvorm`, `wf017-meervoud-botst-met-vreemd-meervoud`
— de nieuwe pin van de hoofdletteridentiteit die met het register uit
`jutterswet-vormenkabinet` verdween —, `wf005-parameterfacet-telling-nul`,
en na de lens `wf009-dubbele-afkorting`, `wf017-meervoud-schaduwt-
kalenderafkorting`, `wf001-dubbele-dagsoort`, `wf001-dubbel-eenheidstelsel`,
`wf005-parameterfacet-telling-twee`); negatief 400 → 407; `expect.txt` 91
→ 165 (`EXPECTED_MIN_EXPECT` mee); vloer 350 blijft.

**Consumenten en emitters.** Gedeelde OCaml-lader op de L3-vorm met de
bijectie en de rationale factor (AST: `rationaal`, `gedeclareerde_eenheid`,
`dagsoort`, `presentatie_annotaties` — de tag staat niet in het datatype
zodat de structurele typevergelijking blijft werken; `par_tijdlijn_aantal`);
elk vervallen veld en elke vervallen knoop bij naam geweigerd (35
laadweigeringsrijen in de clingo-suite; `TypesysteemWeigeringWitnessTest`
in Drools; de enumzinnen van Drools zijn die van de OCaml-lader: `unknown
precision value "<v>"`, `unknown presentation value "<v>"`, `unknown sign
value "<v>"`, `unknown datatype node "<k>"`); ref en clingo rekenen exact
(IR-SPEC 2.5: `3 jr` = `36 mnd`, `18 mnd` = `3/2 jr`, `100 €/mnd` = `1200
€/jr` als getuigen, plus de niet-gehele `{1, 24}`-getuige `36 uur` = `3/2
dg`, die op de oude double-route faalt); catala en flogic schrijven de
L3-vorm; `IrEmitter` schrijft de rationaal uit de gedeclareerde vorm (het
frontendmodel kreeg `exacteFactoren`/`Factor`; `DefinitionResolver` rekent
de breuk langs dezelfde keten als de decimale factor), de tag alleen op
declaraties; `DefinitionResolver` meldt een tweede declaratie van dezelfde
afkorting in één stelsel (ook `kg`/`KG`, dezelfde naamidentiteit als de
emitter) als benoemde ERROR op de regel van die declaratie — tot de
adviesronde verving zij de eerste stil, met lege `diagnostics` en een
verifier-schoon document (restvondst 6, bestond op de basis); de
emitterbewaking blijft; de Drools-lader zet `{num, den}` om naar de
DECIMAL64-recordfactor (gevolgde delta, rij 26; `VoorafgerondeFactor
WitnessTest` pint `{1, 3}` en `{1, 24}`: `216.00 sl` → `9.000000000000001`
in beide takken waar ref `9.00` geeft) en leest de wire bijectief terug; de
weigerzinnen letterlijk gelijk in verifier, ref, clingo en Drools, sinds de
adviesronde ook de factorzin met de bovengrens 2^62 − 1, de WF-005-zin bij
een parameterfacet met `count ≠ 1` (Drools: documentfout, geen
vormweigering meer) en de `Onbekend`-arm uit L2 op `<datatype>/node`; 73
rgs-goldens en de toka-dump byte-identiek (README-divergentieregel L3).
Nieuw golden `jutterswet-afdrachtstapel` (B1: `% ± %` in een voorwaarde,
`bron.rgs` + case, vier instanties op beide grenzen; ref, clingo en de
DRL-tekst-tak spelen het, de exec-tak sluit een percentagevergelijking bij
naam uit — `EXPECTED_CHANNEL`-regel met reden).

**Gate-scripts (per regel):** `check-ir-schema.sh` (`_l3_base`,
`L3_SHAPE_ROWS`, 123 vormrijen, waaronder de grens 2^62−1 op `num`/`den`;
de M1/L2-bases op de L3-vorm); `check-verifier.sh` (`EXPECTED_MIN_EXPECT`
91 → 165); `check-reftype-derivation.sh` (twister op `Number`/`Date`/
`unit`/`factorToCanonical`, de zustereenheid als rationaal `num × 10^k`,
`Fraction`; `EXPECTED_MIN_TWISTS` 38 blijft); `check-clingo.sh` (r. 58:
geen fijnste-eenheidfout meer, `1/24` blijft exact); `check-drools.sh`
(`EXPECTED_DRL_PLAYED` 166 → 167 met datum en reden, `EXPECTED_PLAYED` 188,
de kanaalregel voor `jutterswet-afdrachtstapel`); `check-rood.sh`
ongewijzigd (geen fragment bewoog).

**Lens:** eerste ronde op unie 1 (`c140ccb0`) EERST FIXEN met vijf blokkerende
vondsten en vijftien adviespunten: B1 de verifier weigerde de eigen emissie
van de frontend voor `% ± %` (L3 liet `Numeriek(%)` samenvallen met een
percentage terwijl de afgeleide positie een getal eiste), B2 de naamtabeltoets
rc 1, B3 een dubbele afkorting in één stelsel alleen door Drools geweigerd,
B4 andere enumzinnen in Drools, B5 vervallen wirevormen als geldend
beschreven; de fixronde op vier branches (`card/l3-fix-wire`,
`card/l3-fix-spec`, `card/l3-b`, `card/l3-c`) sloot B1–B5 en A1–A9,
A11–A15 (A12 deels); de herkeuring door een verse lens op unie 2
(`f12b45cc`): MERGEN, alle vijf blokkerende vondsten en dertien adviespunten
gefixt en nagemeten (zeven B1-probes en 22 eigen probes byte-gelijk
opnieuw geëmitteerd en verifier-schoon; de golden handmatig nagerekend;
de nulmeting basis → unie 2 over alle goldens en vier uitvoerders: het
enige verschil is de regel van het nieuwe golden; de weigerzinnen van B3
en B4 in vier lezers op dezelfde pointer; `{1, 24}` faalt op de basis en
slaagt op de unie), A10 bij de landing (registerwerk), zes restvondsten
als advies — R2 de Drools-zin bij `count ≠ 1` (gedaan vóór de uitspraak),
R3 de bovengrens 2^62 − 1 in de factorzin van beide laders, R4 de
`Onbekend`-arm van Drools op de pointer en zin van de OCaml-lader, R5 de
vier v0-labels in WF-001, R6 de stille vervanging van een dubbele afkorting
in `DefinitionResolver` (bestond op de basis; nu een benoemde ERROR met
test), R1 de exec-tak (vastgelegd, open punt 7) — alle in de adviesronde
in deze kaart afgehandeld zonder derde lens; proeven in
`/tmp/lens-l3-meet/herkeuring/`.

**Gates op de landing (alle veertien, elk los):** 1 PASS; 2 PASS 289 bestanden, 26 + 123 zelftestrijen; 3 PASS 289 schoon / 407 negatiefs / 165 armfragmenten; 4 PASS 667 case-files; 5 PASS ref-suite 955 / 619 replays; 6 PASS suite 1129 / 619; 7 PASS 582 + 37; 8 PASS 48 / 96 (Drools-poot 11 / 37); 9 PASS 207 / 12; 10 PASS 559 tests / 73 goldens; 11 PASS 45 fragmenten, 27 tests, 29 slices, replay 29/29/29; 12 PASS played 188, DRL 167, JUnit 717; 13 PASS 40 twists / 22 goldens, 100 replays, DRL 21; 14 PASS 135 / 2.

**Open punten (genummerd, hoofdsessie weegt ze bij L4–L5):**
1. Normvraag N1 uitgevoerd met het standaardantwoord `{num, den}`; U11
   (tolerante reconstructie) en U12 (spelling ≠ afkorting) als verfijningen
   van D25/D54 — U12 dwingt geen lezer af (schrijfregel in IR-SPEC).
2. De inhoud van `spellings` heeft geen regel (geen uniciteit binnen een
   stelsel, geen botsingsregel tegenover andere afkortingen; gemeten
   verifier-schoon: `kg` met spelling `t` naast eenheid `t`) en geen lezer
   gebruikt ze — OPEN-punt in IR-SPEC 2.5.
3. Multi-period op een parametertijdlijn blijft OPEN in RS 3.12; WF-005
   houdt `count = 1` tot die betekenis vastligt.
4. Een eenheid zonder afkorting: de grammatica staat hem toe, de emitter
   weigert met een `IllegalStateException` (geen golden of test draagt er
   een); een `Number` met `%` en een teken schrijft de emitter, elke lader
   weigert (0 in het corpus).
5. WF-057 leest meervoud en spellings niet (was zo); de zeven
   WF-001-namespacelabels noemen sinds de adviesronde de wirelijsten
   (`objectTypes`, `domains`, `parameters`, `unitSystems`, `factTypes`,
   `dimensions`, `dayKinds`; geen rapport of `expect.txt` droeg een oud
   label), terwijl de pointers in verifierrapporten hun v0-paden houden
   tot de hernoemingslanding.
6. Drools rekent met de vooraf afgeronde DECIMAL64-factor (rij 26): op een
   verifier-schoon frontenddocument waarneembaar (`1.9999999999999999` in
   een attribuut met twee decimalen, beneden-afronding 1 waar 2 hoort;
   gepind in `VoorafgerondeFactorWitnessTest` en de `IrLoader`-klassekop);
   de sluiting via `exacteFactoren` is een eigen kaart.
7. De exec-tak rekent niet met percentages in een vergelijking
   (`jutterswet-afdrachtstapel` EXCLUDE bij naam; dezelfde familie als
   "waarde buiten scope" bij `vondsttarief`), en die uitsluiting is sinds
   L3 breder dan op v0: een vergelijking tussen twee afgeleide
   %-bewerkingen (`A plus B > C plus D`), die de tak speelde zolang hij de
   operanden als getallen las, sluit hij nu ook uit (0 corpusdocumenten;
   gemeten door de lens op frontendproeven, PLAYED → EXCLUDE); dichten
   verschuift gate-12-kanalen — eigen kaart (restvondst 1 van de
   herkeuring; de javadoc van `getalachtigVergelijkbaar` zegt nu wat de
   code doet).
8. De frontend weigert TOTAAL/MIN/MAX over een gedeclareerd percentage,
   zodat zijn laagste-precisieregel voor `Numeriek` op die posities
   onbereikbaar is; de verifier-lezing van B1 dekt de vorm wel.
9. Testwijzigingen (gemeld, onder kaartmandaat): ref 948 → 955, clingo
   1078 → 1129, catala 207, flogic 135, frontend JUnit 556 → 559 (9
   herschreven, één nieuw voor de resolver), Drools JUnit 705 → 717
   (`OnbekendVeldWitnessTest` vloer 60 → 59 knoopsoorten).
10. Stap (k), de herpin van `regelspraak-java`: UITGESTELD tot de nieuwe
    toolchain onder uchar (besluit maintainer 2026-09-24: geen tussentijdse
    herpins voor L3–L5, één pin-bump als v1 klaar is). Wat dan kantelt:
    minstens 9 frontendtests pinnen de L2-spelling (o.a. `tijdsevenredig_
    deel_draagt_basistype_geen_sentinel`, 3 buiten `IrEmitterTest`),
    `IrEmitterTest` op `percentage_literal_lexeme_is_kaal`, twee
    datum-literaaltests en `unit_literal_lexeme_is_kaal_eenheid_in_type`
    (TC A.6); het frontendmodel draagt `exacteFactoren`/`Factor` — een
    `Eenheidstelsel` dat de toolchain zonder `DefinitionResolver` bouwt,
    weigert een niet-gehele factor.
