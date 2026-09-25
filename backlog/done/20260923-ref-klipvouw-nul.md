# ref-klipvouw-nul: de arbiter antwoordt 0 waar niets geklipt is

Gemeten door de datumvouw-lens (2026-09-23): in de gedurende-klip-arm
van ref antwoordt de ARBITER zelf een betekenisloze 0 — een casus
zonder waardetijdlijn geeft `wachtsom = 0`, en een casus waarin de
hele klip wegvalt (kenmerk disjunct van de waarde) ook; met een
datumgetypeerd doel is die 0 zelfs 1970-01-01. Clingo volgde op de
oude basis (`[0]`); de Drools-exec CRASHT op de casus zonder waarde
met `ClassCastException BigDecimal→LocalDate`. De eigen-arm doet het
goed (leeg bij leeg). Sinds de datumvouw-kaart weigeren clingo en de
DRL-tak de datumgetypeerde vormen op documentniveau, maar refs
0-antwoord in de klip-arm en de exec-crash staan los van het
datumtype en blijven open.

Wortelvoorstel uit die kaart (beschermd pad, hier mee te nemen): een
WF-regel "de operand van TOTAAL/SOM/pro-rata is numeriek" — WF-091
vergelijkt vandaag alleen het aggregaattype met de operand, niet of
de operand een getal is.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide casussen op alle uitvoerders; leg vast wat de
  genormeerde uitkomst hoort te zijn (leeg, zoals de eigen-arm? — de
  spec beslist, RUNTIME-SEMANTICS ch. 3.5a/3.9 nalezen).
- (b) ref volgt de norm (vermoedelijk: leeg in plaats van 0);
  rood-eerst een ref-suitegetuige; clingo volgt; de exec-crash wordt
  een benoemde weigering of het juiste lege antwoord.
- (c) Weeg de WF-regel (spec/IR-SPEC + verifier, **voorstel**): als
  hij komt, worden de datumvouw-roodgetuigen negatiefs — benoem de
  verschuiving expliciet.
- (d) Corpussweep; alle veertien gates groen.

May touch: `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**), `corpus/**`,
`scripts/check-rood.sh` (**voorstel**), kaartverplaatsing. NB: raakt
de arbiter — extra zware lens.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- **De norm bestaat al en `ref` volgt haar.** RUNTIME-SEMANTICS ch. 3.6
  (r. 1522–1529): "A fully clipped-away — or absent — value timeline totals
  0, never leeg: the clip's presence turns absence into 0 (the mirrored v0
  nuance; the unfiltered total of an absent timeline stays leeg)", gepind
  door `corpus/golden/jutterswet-getijdenwacht` ("a carrier without periods,
  and one without a timeline, both total 0") en `anchor/jutterswet-dubbelwacht`
  (disjuncte leden → 0). De titel van de kaart en acceptatie (b)
  ("vermoedelijk: leeg in plaats van 0") vervallen: `ref` en clingo
  veranderen niet. Wat de lens zag was de 0 in een DATUMslot (1970-01-01):
  dat is het gevolg van een datumgetypeerde operand onder een temporele
  vouw, en daarover gaat de WF-arm. Wat overblijft van (b) is de exec-crash.
- De rode getuigen van vandaag: `corpus/rood/datumtijdlijn-totaalvouw`,
  `datumtijdlijn-klipvouw`, `datumbasis-prorata` (kaart temporele-datumvouw,
  Result §5, §8, §11) en `datumsetbron-totaalvouw`, `datumsetbron-somvouw`
  (kaart datum-setbron-breuk); hun regels in `scripts/check-rood.sh`
  r. 418–422 (de Drools-uitsluitingslijst) en de NOTE's die naar deze kaart
  verwijzen.
- Wire-namen: de vouw is een `Aggregation {kind: "total" | "sum", arguments,
  elementType?, filter?, sourceRole?, sourcePath?}`; de klip is haar
  `filter`; het tijdsevenredig deel is de ch. 3.7-vorm (lees IR-SPEC ch. 4.3
  voor de L5-spelling); typen zijn `Number`, `Date {precision}`, `Text`,
  `Boolean`, `Enumeration`; een melding die een type of enumwaarde noemt
  gebruikt de L5-spelling.
- `ref`: de klip-arm staat in `ref/lib/eval.ml` r. 1506–1584; de zin `maal:
  expected a number, got 2024-01-15` is een rekenfout, geen bodemzin.
- Het Drools-getuigenbestand van de datumvouw-kaart:
  `backends/drools/src/test/.../cases/TijdlijnDatumvouwWitnessTest.java`
  (vier rijen; de rij "de DRL-marker op een lege casus (exec speelt leeg,
  DRL weigert)" pint de fase die U4 verandert).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Norm: de 0 blijft.** Geen delta, geen wijziging in `ref` of clingo
  voor een numeriek doel. Meet (acceptatie (a)) beide klip-casussen (zonder
  waardetijdlijn; waarde geheel weggeklipt) op alle uitvoerders met een
  NUMERIEK doel én met het datumdoel; de tabel in het Result.
- U2 **WF-arm: nieuwe arm van WF-091, geen nieuw nummer** (WF-091 is de
  facettenregel; een som, totaal of tijdsevenredig deel over een
  niet-numeriek operandtype heeft geen afgeleid type — de lijn van
  WF-037's uitbreidingen). De operand van een temporele `total` of `sum`
  (eigen tijdlijn, geklipt, §7.1 per-element setbron) en de basis van het
  tijdsevenredig deel (ch. 3.7) MOET een `Number` zijn; elke andere
  declaratie is ill-formed. Zin (voorstel; de bouwer stelt vast en meldt
  letterlijk): `the temporal total of "bewaarvergoeding per maand" on
  "Jutter" folds a Date timeline: a temporal total, sum or pro-rata part is
  arithmetic over a Number, and a Date is a calendar position (WF-091)` —
  één zin per vouwsoort (total/sum/pro-rata), zonder regelnummerstaart. De
  uitvoerdersnetten (ref `maal`, clingo "Declined in translation", Drools
  `tijdlijnWeigering`/setbron-zin) blijven staan als fail-fast; hun zinnen
  veranderen niet.
- U3 **Getuigen verhuizen.** De vijf rode getuigen worden negatiefs:
  `wf091-tijdlijntotaal-datum`, `wf091-klipvouw-datum`,
  `wf091-prorata-datumbasis`, `wf091-setbron-totaal-datum`,
  `wf091-setbron-som-datum` (`ir.json` + `NOTE.md` + `expect.txt`; de
  case-bestanden vervallen; NOTE noemt de herkomst en citeert de echte
  verifieruitvoer). Rood-eerst per negatief (verifier PASS vóór de arm,
  geciteerd); mutatieproef. `scripts/check-rood.sh`: de vijf
  uitsluitingsregels vervallen (**voorstel**); de commentaarregel r. 355
  meebewegen. Het Drools-getuigenbestand blijft (het bouwt eigen documenten).
  Elke NOTE of README die naar `20260923-ref-klipvouw-nul` of naar de rode
  getuigen verwijst (datumvouw-NOTE's, `corpus/rood/README.md`, de
  klip-NOTE's) wordt bijgewerkt.
- U4 **Drools exec: de crash wordt een benoemde weigering.** De
  exec-transpiler vraagt dezelfde `Vouwwaarde.tijdlijnWeigering` (de guard
  van de DRL-tak) op documentniveau, zodat de casus zonder waarde niet meer
  met `ClassCastException` valt maar met de bestaande zin weigert (open punt
  2 van de datumvouw-kaart). Getuige in een NIEUW testbestand; de ene rij van
  `TijdlijnDatumvouwWitnessTest` die de oude fase pint mag mee naar de
  nieuwe fase (toestemming van de hoofdsessie, aan de maintainer gemeld) —
  verder geen bestaande tests wijzigen. Meet ook dat een NUMERIEK klipdoel
  zonder waarde in exec 0 speelt (≡ `ref`; `jutterswet-getijdenwacht` is de
  drager als hij PLAYED is).
- U5 **Spec.** IR-SPEC WF-091 krijgt de armtekst; RS ch. 3.5, 3.6 en 3.7
  krijgen elk één kruisverwijzing ("the operand is a Number — WF-091");
  geen D-/U-nummers, geen procestaal. Registerzin voor CONSTRUCTS rij 1 (de
  verhuizing van de vijf getuigen, de arm) in het Result; de hoofdsessie
  landt haar. Naamtabel-toets rc 0.
- U6 **Corpus-sweep.** De verifier met de nieuwe arm over golden, rood,
  negatief en toka (toka: alleen tellen): precies de vijf verhuizen, geen
  golden wordt rood.
- U7 **Gates** voor deze bouwer: 1, 2, 3, 5, 6, 8, 12, 13; de unie draait
  alle veertien.

## Result (2026-09-25, bouwer)

## Result ref-klipvouw-nul (2026-09-25)

### Stand

Branch `card/ref-klipvouw-nul`, worktree `/Users/yeb/Developer/uchar-dev/wt/klipvouw`, basis main
2f47d80e + claim df44a7c5, kop **0e8f866f**, werkboom schoon. Niet gepusht, niet gemerged. Zeven commits:

| commit | inhoud |
|---|---|
| b0dac336 | rood eerst: de vijf `ir.json` byte-identiek naar `corpus/negatief/wf091-*` |
| 7d699d68 | verifier: de numerieke-operandarm van WF-091, `expect.txt`, NOTE's (voorstel) |
| 8e353cc5 | de vijf mappen uit `corpus/rood/`, `scripts/check-rood.sh` (voorstel), golden-NOTE, fixtures van de Drools-getuige |
| fe928281 | Drools exec vraagt `Vouwwaarde.tijdlijnWeigering` op documentniveau; nieuwe getuige |
| 02b333ee | arm strikt op `ListType`; zesde negatief `wf091-setbron-som-tekst` (voorstel) |
| 48b93194 | IR-SPEC WF-091 + RS ch. 3.5/3.6/3.7 (voorstel) |
| 0e8f866f | één Javadoc-zin eenduidig |

Kern: de 0 blijft de norm (RS ch. 3.6); `ref`, clingo, `frontends/` en de uitvoerderszinnen zijn niet
aangeraakt. `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md`, `.github`, de kaart zelf: niet aangeraakt.

### 1. Meting (U1)

Documenten: NUMERIEK = `corpus/golden/jutterswet-erfwacht/ir.json`; DATUM = de klipgetuige (toen
`corpus/rood/datumtijdlijn-klipvouw`, nu byte-identiek `corpus/negatief/wf091-klipvouw-datum`).
Casussen: (a) K1 zonder waardetijdlijn, wachtplichtig 2023-07-01..2023-08-01; (b) waarde
2023-07-01..2023-09-01, wachtplichtig 2024-01-01..2024-02-01 (disjunct, geheel weggeklipt);
(c) één geklipte maand. Verwachting telkens `attributeEmpty`, zodat elke uitvoerder zijn uitkomst toont.

| doel | casus | verifier | `ref` | clingo | exec vóór | exec ná | DRL |
|---|---|---|---|---|---|---|---|
| numeriek | (a) zonder waarde | PASS | 0 | [0] | 0 | 0 | 0 |
| numeriek | (b) weggeklipt | PASS | 0 | [0] | 0 | 0 | 0 |
| numeriek | (c) één maand | PASS | 10 | [10] | 10 | 10 | 10 |
| datum | (a) zonder waarde | vóór PASS, ná WF-091 | **0** (getal in datumslot) | TRANSLATION FAULT (clip-zin) | **ERROR ClassCastException BigDecimal→LocalDate** | EXCLUDE transpiler SKIP (tijdlijnWeigering) | EXCLUDE MARKER |
| datum | (b) weggeklipt | idem | **0** | TRANSLATION FAULT | EXCLUDE caselader | EXCLUDE transpiler SKIP | EXCLUDE MARKER |
| datum | (c) één maand | idem | FOUT `maal: expected a number, got 2024-01-15` | TRANSLATION FAULT | EXCLUDE caselader | EXCLUDE transpiler SKIP | EXCLUDE MARKER |
| datum, eigen totaal | zonder waarde | idem | leeg | TRANSLATION FAULT | PLAYED (leeg) | EXCLUDE transpiler SKIP | EXCLUDE MARKER |

`jutterswet-getijdenwacht` (J2 zonder tijdlijn → 0, J3 zonder kenmerkperioden → 0): exec PLAYED 1 en
DRL PLAYED 1, vóór én ná; `ref` en clingo PASS (10). De numerieke 0 is het volle vierkant.

### 2. De WF-091-arm (verifier, voorstel)

`spec/verifier/lib/checks.ml`, `numeric_operand_arm`, aangeroepen als eerste in `composite_identity`.
Reikwijdte (eigen besluit, zie §6 punt d): elke operand van `sum` en `total` in elke lezing (lijstsom,
setvouw, §7.1 per-element, eigen temporele total, gedurende-klip) en de basis van `timeProportionalPart`.
Toegestaan: `Number` (ook met eenheid `%`). Elke andere declaratie (`Date`, `Text`, `Boolean`,
`Enumeration`, `SumType`, `ListType`, `StructType`) meldt op `…/arguments/<i>`, één melding per
niet-numerieke operand; waar de arm meldt, loopt de identiteitskruising niet. Onbekend type: uitstel.

#### Definitieve zinnen, letterlijk (zonder regelnummerstaart)

Sjabloon: `<kop> of <operand> <werkwoord> <T> values: a total, sum or pro-rata part is arithmetic over a
Number, and <staart>` met kop `the total` | `the sum` | `the pro-rata part (timeProportionalPart)`,
werkwoord `folds` (total/sum) | `scales` (pro-rata), operand `'<attribuut>' on '<objecttype>'` |
`the parameter '<naam>'` | `its operand <i>`, staart `Date values are calendar positions` (Date) |
`<T> values are not amounts` (elk ander type). Uit de echte verifieruitvoer:

- `WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the total of 'bewaarvergoeding per maand' on 'Jutter' folds Date values: a total, sum or pro-rata part is arithmetic over a Number, and Date values are calendar positions`
- `WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the total of 'wachtvergoeding per maand' on 'Klipjutter' folds Date values: a total, sum or pro-rata part is arithmetic over a Number, and Date values are calendar positions`
- `WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the pro-rata part (timeProportionalPart) of 'borgtarief' on 'Jutter' scales Date values: a total, sum or pro-rata part is arithmetic over a Number, and Date values are calendar positions`
- `WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the total of 'vondstdatum' on 'Vondst' folds Date values: a total, sum or pro-rata part is arithmetic over a Number, and Date values are calendar positions`
- `WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of 'vondstdatum' on 'Vondst' folds Date values: a total, sum or pro-rata part is arithmetic over a Number, and Date values are calendar positions`
- `WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of 'vindplaats' on 'Vondst' folds Text values: a total, sum or pro-rata part is arithmetic over a Number, and Text values are not amounts`

Afwijkingen van de U2-voorstelzin, met reden: (1) geen `(WF-091)`-staart (regime: zonder
regelnummerstaart; gate 3 verbiedt het regelnummer ook in `expect.txt`); (2) geen "temporal" en geen
"timeline": twee van de vijf getuigen zijn niet-temporele setvouwen (gemeten, §6 d), dus die woorden
waren daar onwaar; (3) enkele aanhalingstekens rond namen, de verifierconventie (75× `'%s'` tegen
3× `%S` in `checks.ml`); (4) een typeafhankelijke staart, omdat "calendar position" alleen voor een
`Date` waar is.

Het verifierbericht is alleen verifier-zijde: de gedeelde OCaml-lader en de Drools-lader dragen deze vorm
niet (de uitvoerdersnetten bleven, met ongewijzigde zinnen: `ref` `maal`/`plus`/`pro-rata base is not
numeric`, clingo "… Declined in translation", Drools `Vouwwaarde.tijdlijnWeigering`/`weigering`).

#### Rood eerst en mutatieproeven (letterlijk gemeten)

- Vóór de arm (verifier md5 `e0bafac4…`, main 2f47d80e): alle vijf `verifier: PASS`; gate 3 rc=1 met
  vijf regels `FAIL: negatief case corpus/negatief/wf091-…/ir.json passed the verifier (must violate WF-091):`.
- Na de arm (definitieve md5 `d2b5ce9992759f07860e95fbecec733e`): elk één WF-091-regel (boven).
- Mutant 0, typetoets uit (`when false && …`): alle zes PASS; gate 3 noemt elk
  (`passed the verifier` + `not on the arm … expect.txt names`).
- Mutant `total` uit de vouwtak: precies de drie total-getuigen PASS. `sum` uit: precies de twee
  sum-getuigen PASS. Pro-rata-tak uit: alleen `wf091-prorata-datumbasis` PASS.
- Mutant niet-Date-staart = Date-staart: document blijft afgewezen, gate 3 faalt alleen op
  `wf091-setbron-som-tekst` (`rejected for WF-091, but not on the arm … expect.txt names`).
- Elke keer hersteld met `cp` + `cmp`, binary weer md5 `d2b5ce99…`.

### 3. De getuigen

Verhuisd (U3), `ir.json` byte-identiek (git ziet renames), case-bestanden vervallen, `expect.txt` = de
volle melding, NOTE met herkomst, echte verifieruitvoer, rood eerst en mutatie:
`corpus/negatief/wf091-tijdlijntotaal-datum` (was `rood/datumtijdlijn-totaalvouw`),
`wf091-klipvouw-datum` (was `rood/datumtijdlijn-klipvouw`), `wf091-prorata-datumbasis`
(was `rood/datumbasis-prorata`), `wf091-setbron-totaal-datum` (was `rood/datumsetbron-totaalvouw`),
`wf091-setbron-som-datum` (was `rood/datumsetbron-somvouw`).

Toegevoegd (eigen besluit): `corpus/negatief/wf091-setbron-som-tekst` — `wf091-setbron-som-datum` met lid
en doel naar `Text` en hernoemd; pint de staart van elk type behalve `Date`. Oud PASS, nieuw één regel.
Gemeten op dit document: `ref` `EVALUATION FAULT: plus: expected a number, got "strand"`, clingo
`TRANSLATION FAULT: a text/enum/boolean operand here is an own-subject attribute read or a literal (no
corpus case for more)`, beide Drools-takken `… vouw SOM over de setbron 'vindplaats' van Vondst: die leden
zijn van type Tekst en niet getalachtig …`.

### 4. Corpus-sweep (U6), verifier oud (e0bafac4) tegen nieuw (d2b5ce99)

| zone | documenten | afgewezen oud | afgewezen nieuw | rapport veranderd |
|---|---|---|---|---|
| golden | 242 | 0 | 0 | 0 |
| rood | 42 (43 casussen) | 0 | 0 | 0 |
| negatief | 424 | 418 | 424 | 6 (de vijf + de tekstgetuige) |
| toka (alleen geteld) | 30 | 28 | 28 | 0 |

Dekking (tijdelijke sonde, hersteld met cmp): de arm leest 46 verschillende operandposities over 34
goldens (19 `sum`, 17 `total`, 10 pro-rata-bases), alle `Number`; rood 5 over 4; toka 2 over 2.
Geen golden wordt rood.

### 5. Drools exec (U4)

`ExecModelTranspiler.tijdreeksTotaal` vraagt na de granulariteitscheck (volgorde van
`DrlTranspiler.tijdreeksAggregatie`) `Vouwwaarde.tijdlijnWeigering`; nieuw veld `tijdlijnReden`
(gereset in `meet()`), gelezen in `regel()` → `skip("regel (" + reden + "): " + naam)`. Exec-melding
(zelfde zin als de DRL-marker): `transpiler SKIP: regel: wachtsom van een klipjutter — regel (vouw TOTAAL
over de tijdlijn 'wachtvergoeding per maand' van Klipjutter: de segmentwaarden zijn van type Datum in dagen
en niet getalachtig, terwijl de tijdlijnvouw elke segmentwaarde met haar aantal roosterperioden
vermenigvuldigt. Vermenigvuldigen is rekenkunde en alleen bedragen schalen; ref werpt op de
vermenigvuldiging zelf een typefout. Geen enkele uitvoerder draagt deze vorm (§7.1/ch. 3.6)): wachtsom van
een klipjutter`.

Nieuw testbestand `backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/TijdlijnKlipvouwWitnessTest.java`
(3 rijen, `BeideTakken`): klip zonder waarde weigert in beide takken; weggeklipte datumwaarde idem;
numeriek klipdoel zonder waarde en met weggeklipte waarde speelt 0 in beide takken. Rood eerst op de
exec-code van de basis: `Tests run: 3, Failures: 2` — `Unexpected exception type thrown, expected:
<…CaseRunner.Excluded> but was: <org.kie.api.runtime.rule.ConsequenceException>` met
`ClassCastException: Cannot cast java.math.BigDecimal to java.time.LocalDate`, en `exec-model names 'vouw
TOTAAL over de tijdlijn …': non-numeric timeline value on 'wachtvergoeding per maand' …`; hersteld 3/3.
Mutant zonder de skip-regel in `regel()`: 2 failures, exec valt door naar de instantie-accumulate en
weigert met de SETBRON-zin (`vouw TOTAAL over de setbron 'wachtvergoeding per maand' …`) — de reden voor
het veld; hersteld met cmp.

`TijdlijnDatumvouwWitnessTest` (bestaand): fixtures verlegd naar `corpus/negatief` met de casusinvoer in
de test (de case1.json-bestanden vervielen); drie rijen naar de nieuwe fase (§6 b/c); Javadoc bijgewerkt.

### 6. Wat aan de kaarttekst niet klopte (gemeten)

a. `corpus/rood/README.md` bestaat niet; `corpus/README.md` noemt geen getuige.
b. U3 "Het Drools-getuigenbestand blijft (het bouwt eigen documenten)": onwaar. `TijdlijnDatumvouwWitnessTest`
   las `corpus/rood/datumtijdlijn-{totaalvouw,klipvouw}/ir.json` én `case1.json`; de verhuizing brak drie
   van de vier rijen. Fixturebron verlegd (commit 8e353cc5), beweringen daar ongewijzigd, 4/4 groen.
c. U4 "die ene rij": DRIE rijen pinden de oude exec-fase — rij 1 en 2 in hun exec-bewering (de
   caselader-zin `non-numeric timeline value on …`), rij 3 geheel (exec speelde leeg). Alle drie naar de
   nieuwe fase (zelfde zin als DRL). Dit gaat verder dan de toestemming voor één rij; zonder deze
   wijziging is gate 12 rood. Aan de maintainer te melden.
d. U2-reikwijdte "(eigen tijdlijn, geklipt, §7.1 per-element setbron)" en de zin "the temporal total …
   folds a Date timeline": `datumsetbron-totaalvouw`/`-somvouw` zijn niet-temporele setvouwen
   (`vondstdatum` heeft geen `timeline`), dus geen §7.1 per-element vouw. Een temporele arm had U3/U6
   (vijf verhuizen) niet gehaald; de arm dekt daarom elke lezing van `sum`/`total`, en de zinnen noemen
   geen "temporal"/"timeline". Het oorspronkelijke WF-voorstel (kaart temporele-datumvouw §8) was al
   algemeen ("de operand van TOTAAL, SOM en het tijdsevenredig deel is numeriek").
e. U2-voorstelzin draagt zelf een regelnummerstaart `(WF-091)`; weggelaten (regime).
f. De opdracht noemde de commentaarregel r. 355; het getal "six" stond op r. 352, dus r. 352–355 bewogen.

### 7. Gewijzigde scriptregels (beschermd pad, voorstel)

`scripts/check-rood.sh`:

```
-# of the case, and a case defect lands on either side: measured 2026-09-23, six listed cases get
+# of the case, and a case defect lands on either side: measured 2026-09-23, four listed cases get
-# unknown-role-target-label, uni-temporal-key-scalar, uu-temporal-key-scalar,
-# datumtijdlijn-totaalvouw, datumtijdlijn-klipvouw), and on the vensterheffing document an
+# unknown-role-target-label, uni-temporal-key-scalar, uu-temporal-key-scalar — six until the card
+# ref-klipvouw-nul moved two to corpus/negatief), and on the vensterheffing document an
```

en de vijf regels `datumbasis-prorata|…`, `datumsetbron-somvouw|…`, `datumsetbron-totaalvouw|…`,
`datumtijdlijn-klipvouw|…`, `datumtijdlijn-totaalvouw|…` van de Drools-uitsluitingslijst vervallen (oud
r. 418–422). De vier overgebleven caselader-gevallen opnieuw gemeten: alle vier EXCLUDE uit de caselader.
Geen telvloer aangeraakt (rood 43 ≥ 26; negatief 424 ≥ 350; expect 266 ≥ 260).

Overige beschermde paden (voorstel): `spec/verifier/lib/checks.ml`, `spec/IR-SPEC.md` (nieuwe alinea
"The operand of an arithmetic fold is a Number" na de tabel in ch. 4.1; één zin in de pro-rata-alinea
"Measured, and open"; indexrij WF-091 in alle vier kolommen), `spec/RUNTIME-SEMANTICS.md` (ch. 3.5 bullet
"The operand is a Number — WF-091 …", ch. 3.6 na de klip-0-zin "The operand is a Number — WF-091
(`IR-SPEC.md` ch. 4.1) — so this 0 always lands in a numeric slot.", ch. 3.7 "The base is a Number —
WF-091 (`IR-SPEC.md` ch. 4.1)."). Naamtabel-toets: `v1-naamtabel-toets: OK`, rc 0.

### 8. Gates (U7), elk los na `eval $(opam env)`, op de kop vóór de Javadoc-commit 0e8f866f

1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 284 file(s) validate and round-trip, floor 192)` — was 289 (vijf rode documenten weg)
3. `check-verifier: PASS (284 golden clean; 424 negatief rejected for their declared rule, floor 350; 266 on their declared arm (expect.txt), floor 260)` — was 289 / 418 / 260
5. `check-oracle: PASS (ref suite 977 worked examples, floor 489; 619 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)`
6. `check-clingo: PASS (backend suite 1726 checks, floor 83; 619 cases replay exactly through clingo — ref = clingo over the corpus)`
8. `check-rood: Drools leg PASS (11 fault-pinned, 32 not-carried with reason)` en `check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences)` — was 48 casussen, 96 fragmenten, Drools 11 / 37
12. `check-drools: PASS` — `played 188` = pin 188; `DRL-text branch played 167` = pin 167 (ongewijzigd, zoals verwacht); kanaalvloer 54, DRL-vloer 75; `JUnit 776 tests, floor 239` (inclusief de 3 nieuwe rijen)
13. `check-reftype-derivation: PASS (40 unit annotations twisted over 22 goldens, floor 38; 100 executor replays reproduce the untouched outcome, 21 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`

Commit 0e8f866f wijzigt alleen een Javadoc-zin; daarna `TijdlijnDatumvouwWitnessTest` 4/4 en
`TijdlijnKlipvouwWitnessTest` 3/3 opnieuw groen. `dune build frontends/flogic` en `frontends/catala` rc 0.

### 9. Registerzin voor `spec/CONSTRUCTS.md` rij 1 (de hoofdsessie landt haar)

> **Landed (2026-09-25, card ref-klipvouw-nul):** WF-091 gains the numeric-operand arm. Every operand of
> a `sum` or `total` — in each reading: the listed sum, the set fold, the §7.1 per-element fold, the
> subject's own temporal total and its gedurende clip — and the base of the pro-rata part
> (`timeProportionalPart`) MUST be a `Number` (a `%` number included); any other declaration is
> ill-formed, reported at the operand, and there the derived-type cross does not run (IR-SPEC ch. 4.1,
> "The operand of an arithmetic fold is a Number"; RUNTIME-SEMANTICS ch. 3.5–3.7 point to it). The five
> red witnesses of the cards temporele-datumvouw and datum-setbron-breuk were verifier-clean only because
> the `sum`/`total` derivation passed a `Date` operand through; they moved to `corpus/negatief` as
> `wf091-tijdlijntotaal-datum`, `wf091-klipvouw-datum`, `wf091-prorata-datumbasis`,
> `wf091-setbron-totaal-datum` and `wf091-setbron-som-datum`, beside the new `wf091-setbron-som-tekst`
> (the message tail of every type but a `Date`); gate 8 lost their five cases and their five Drools
> exclusion lines. The norm of ch. 3.6 is unchanged — a fully clipped-away or absent value timeline under
> a clip totals 0 — and `ref` and clingo did not change: the 0 in a `Date` slot that `ref` answered on
> the clip witness is no longer reachable from a verifier-clean document. The Drools exec branch now asks
> `Vouwwaarde.tijdlijnWeigering` of the declaration, as the DRL-text branch did, so a date-valued clip
> without a value refuses by name instead of crashing with a `ClassCastException` at fire. Measured over
> golden (242), rood (42), negatief (424) and the parity zone (30): only the six witnesses change their
> verifier report. Gate 12 pins unchanged (188 / 167).

Daarnaast verwijzen de eerdere "Landed"-blokken van datum-setbron-breuk en temporele-datumvouw in rij 1
naar `corpus/rood/…`-paden die niet meer bestaan (zie open punt 3).

### 10. Open punten

1. `spec/ASP-TRANSLATION.md` r. 1012–1013 noemt `corpus/rood/datumsetbron-totaalvouw` en
   `corpus/rood/datumsetbron-somvouw`; buiten de may-touch-set. Nieuwe paden:
   `corpus/negatief/wf091-setbron-totaal-datum`, `…/wf091-setbron-som-datum`.
2. `backends/clingo/test/test_clingo.ml` r. 6106 en 6154: commentaar noemt `rood/datumsetbron-somvouw`
   en `corpus/rood/datumsetbron-totaalvouw/NOTE.md`. Bestaand testbestand, niet gewijzigd.
3. `spec/CONSTRUCTS.md` rij 1: de oudere blokken noemen de rode paden; de hoofdsessie landt de
   registerzin (§9) en kan die paden bijwerken.
4. Twee clausules van de arm hebben in geen zone een getuige (gemeten door elk te verwijderen): een
   `%`-getal als operand (mutant die alleen `Numeriek` toelaat: 0 documenten veranderd) en de
   `ListType`-weigering. Een golden met een `sum` over een `%`-attribuut zou de niet-overweigering pinnen;
   een negatief met een lijstoperand de weigering. Ook de operandbenamingen `the parameter '<naam>'` en
   `its operand <i>` hebben geen corpusdocument.
5. Afwijking van U4 (§6 c): drie bestaande rijen van `TijdlijnDatumvouwWitnessTest` gewijzigd, plus de
   fixturebron (§6 b). Maintainer-bevestiging nodig.
6. De Drools-zinnen `tijdlijnWeigering`/`weigering` dragen nog v0-spelling (`TOTAAL`, `SOM`,
   `Datum in dagen`); U2 hield ze ongewijzigd. Exec gebruikt nu dezelfde zin als DRL.
7. `corpus/negatief/README.md` heeft geen lijst voor getuigen die buiten een migratiestap binnenkomen; de
   zes staan alleen in hun NOTE en in de WF-091-indexrij. De hoofdsessie beslist of de README een regel krijgt.
8. Gates 4, 7, 9, 10, 11 en 14 heb ik niet gedraaid (U7). Gate 9 en 14 zetten door frontends uitgegeven
   programma's door de verifier; de arm kan daar alleen iets raken als een frontend een `sum`/`total`/
   pro-rata over een niet-`Number` uitgeeft. Gate 11: de sonde vond in toka alleen twee `sum`-operanden
   van type `Number` en geen `total`, dus de exec-wijziging raakt daar niets (eigen gevolgtrekking).
9. De onderwerpregel van commit 7d699d68 is langer dan 72 tekens (cosmetisch; herschrijven vergt een rebase).

## Lens en fixronde (2026-09-25, hoofdsessie)

**Lens A (spec/verifier-kwartet, export van de unie d43eb002; oordeel EERST FIXEN).** Voor deze kaart: B2 — na de verhuizing was de tweede lijn van ref en clingo (de datumweigeringen) voor drie van de vijf vormen nergens meer gepind; B4 — de `ListType`-clausule en de operandnamen "the parameter"/"its operand" zijn bereikbaar zonder negatief, en de benamingszin in de spec was onnauwkeurig; B5 — verwijzingen naar de verdwenen rode paden in ASP-TRANSLATION, test_clingo.ml en CONSTRUCTS rij 1; B6 — onware javadoc in `Vouwwaarde.java`; B8 — dezelfde eis onder WF-038 en WF-091; A7 — te lange regels en gedateerde tellingen; A9 — dode testregel en de meetdatum in check-rood.sh; A10 — javadoc van `tijdreeksTotaal`; A11 — de `%`-clausule zonder getuige; A12 — drie herschreven rijen van het bestaande Drools-getuigenbestand, bevestigd. Bevestigd: de arm leest 46 posities over 34 goldens, alle `Number`; de norm van ch. 3.6 ongewijzigd; de oude crashfase onbereikbaar.

**Besluiten van de hoofdsessie.** B2/B3: suitegetuigen in de clingo- en ref-suites (mandaat); B4: drie negatiefs en de spectekst; B5: paden bijgewerkt (ASP-TRANSLATION met mandaat; CONSTRUCTS bij het registerwerk); B8: kruisverwijzing, de arm blijft onder WF-091; A11: een `%`-golden; A12: bevestigd. Laag gewogen en open: exec-consistentie met een ander compound dan `all`; de Drools-zinnen in v0-spelling (geheugennamen).

**Fixronde (fixer A-exec, card/k6-fix-a-exec, gerebaset in de unie).** B2: de tweede lijn is weer gepind — drie `faults_with`-checks in `backends/clingo/test/test_clingo.ml` (eigen tijdlijntotaal, klip en pro-rata-basis over een datum, met de negatiefdocumenten en de invoer van de vroegere case1.json) en vijf rijen `WF-091-tweede-linie-*` in `ref/test/test_ref.ml` met de oude rode pins als naald (rood-eerst met de lensmutant: 3 clingo-failures en 1 ref-failure; een eigen ref-mutant laat precies de vier maal/plus-rijen omvallen); B5: ASP-TRANSLATION en de clingo-suite wijzen naar de negatiefs en noemen de suite als getuige van de vertaling; B6 en A10: de javadoc van `Vouwwaarde` en `tijdreeksTotaal` naar de waarheid; A9: de dode testregel weg. Suites: ref 994, clingo 1733.

**Fixronde (fixer A-spec).** B4: drie negatiefs, rood-eerst (de basisverifier liet alle drie door): `wf091-setbron-som-lijsttype` ("and ListType values are not amounts"), `wf091-som-datumparameters` ("the sum of the parameter 'peildatum' folds Date values"), `wf091-som-datumliteralen` ("the sum of its operand 0 folds Date values"); per clausule maakt één mutant gate 3 rood op precies zijn eigen map; de benamingszin in ch. 4.1 luidt nu "an operand that is neither an attribute nor a parameter reference is named `its operand <i>`, a parameter operand `the parameter '<name>'`", en "Every clause has a witness" vervangt de alinea over de twee ongetuigde clausules. B8: WF-038 verwijst naar de numerieke-operandarm van WF-091 en ch. 4.1 terug; de arm blijft onder WF-091. A7: hermeten op de unie — precies negen negatiefs veranderen, de arm leest 47 posities in 35 goldens (20 sum, 17 total, 10 pro-rata, één %-getal); regels herwrapt. A9: de teldatum in check-rood.sh op 2026-09-25. A11: de golden `corpus/golden/jutterswet-aandelensom` (setsom over het %-attribuut `gewichtsaandeel`; case1 B1 75, case2 B1 100 / B2 60; alle vier uitvoerders, gate 13 draait hem mee) pint de %-clausule; pins +1/+1. Open: de WF-038-indexrij heeft geen kruisverwijzing; de poortgetuige staat alleen in de WF-079-rij.
