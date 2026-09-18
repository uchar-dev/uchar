# drl-staande-waarden: classic DRL draagt de ch.-7.8-norm

Goal: De classic-DRL-tak implementeert dezelfde staande-waardedoelen als
ref, clingo en de exec-model-tak: een lege berekening op een staand
scalair of temporeel doel schrijft niets; een gevulde berekening
verdringt de bestaande waarde of hele tijdlijn. De
initialisatie-nevenvorm van ch. 7.7 volgt dezelfde domeinafbakening.
Dit is nodig voor volledige huidige RegelSpraak→uchar→beide-Drools
conformiteit, maar is nadrukkelijk geen herstel van oud
toolchaingedrag.

Evidence:

- RUNTIME-SEMANTICS ch. 7.8 is normatief en pinnt de scalaire
  doorval en de whole-timeline-doorval. De exec-model-implementatie
  heeft `staandeScalaireDoelen`, `ontsuikerdeDoelen` en
  `staandeTemporeleDoelen`; classic DRL kent die doelclassificatie niet.
- De bestaande exec-model-JUnitmatrices
  `StaandeScalaireNormWitnessTest` en
  `TemporeelFixpuntWitnessTest` pinnen leeg/gevuld en de domeinflanken,
  maar lopen via `CaseRunner.speelCase` alleen door exec-model.
- Huidige classic-DRL-uitkomsten:
  `anchor/jutterswet-waardestelling` wist case3 naar leeg;
  `anchor/jutterswet-waardering-v0vorm` laat in case6 de
  initialisatie 25 winnen van de staande input 40;
  `anchor/jutterswet-borgdeel` en
  `anchor/jutterswet-dagentelling` wissen de invoertijdlijn bij een
  lege fold.
- Een gerichte replay tegen de oorspronkelijke toolchain bewees het
  onderscheid: beide oude armen schreven bij een lege scalaire
  berekening `null` en bij een lege temporele berekening `[]`; een
  losse initialisatie liet invoer staan. Staande preservation is dus
  een bewuste uchar-delta, niet één van de oude-feature-regressies.

Acceptance:

- Rood-eerst: leg de vier bovenstaande classic-DRL-fouten vast, met
  minstens één scalaire, initialisatie- en tijdlijncase:

  ```sh
  mvn -q -o -f backends/drools/pom.xml package
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/anchor/jutterswet-waardestelling/ir.json \
    corpus/golden/anchor/jutterswet-waardestelling/case*.json
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/anchor/jutterswet-borgdeel/ir.json \
    corpus/golden/anchor/jutterswet-borgdeel/case*.json
  ```

- Trek de doelclassificatie uit naar één gedeelde, modelgebaseerde
  helper of bewijs met één gedeelde testmatrix dat beide branches exact
  dezelfde sets afleiden. De clauses zijn ref-getrouw:
  versie-vrij, één rekenregelschrijver, scalaire initialisatiepaarvorm
  apart via ch. 7.7, geen defaults/allocatie/creatiewrite en dezelfde
  other-writergrens. Classic DRL mag niet een ruimer
  "skip iedere null-write"-beleid krijgen.
- Scalar:
  leeg laat bestaande input staan; zonder input blijft het doel leeg;
  gevuld verdringt input. Naast een initialisatie laat leeg de
  initialisatie beslissen, laat bestaande input de initialisatie leeg
  en verdringt gevuld beide.
- Tijdlijn:
  een lege pro-rata- of dagtellingopbrengst laat de invoertijdlijn
  geheel staan; een niet-lege opbrengst vervangt de hele tijdlijn.
  Invoer- en berekende segmenten worden nooit per bucket gemengd.
  De bestaande idempotentie/fixpuntgarde blijft nodig en actief.
- De niet-staande flanken blijven gepind: twee rekenregels op één doel
  behouden de v0-wissemantiek; een versie-doel komt niet in het domein;
  een werkelijk beslissende write wordt nooit onderdrukt. Een test die
  alleen de vier groene doorvallen controleert is onvoldoende.
- Maak de twee bestaande witnessklassen dual-branch of voeg één
  branchespiegel toe die hun volledige geldige matrix met
  `viaExecModel` en `viaDrlTekst` draait. De WF-050-ongeldige
  defaults-mechaniekwitness hoeft niet door classic DRL, omdat die tak
  prioritized defaults als geheel luid weigert.
- Alle cases van `waardestelling`, `waardering-v0vorm`, `borgdeel` en
  `dagentelling` eindigen zonder vlag én met `--drl` in `PLAYED`, met
  ongewijzigde verwachtingen. Gate 12's exec-modelkanaaltelling
  verandert niet.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`
uitsluitend om de doelclassificatie werkelijk te delen,
een nieuwe gedeelde helper onder
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/runtime/`
alleen wanneer een gedeelde writegarde nodig is,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`.

Out of scope: het terugbrengen van de oude destructieve leeg-write,
prioritized-defaultslowering in classic DRL, wijziging van de
ch.-7.8-norm, andere multi-writersemantiek, object-/feitcreatie,
nieuwe temporele functies en corpusverwachtingen. Deze kaart is een
uchar-conformiteitskaart en moet in `Result:` ook zo worden genoemd.
Zij deelt `DrlTranspiler.java` met de andere pariteitskaarten van
2026-09-18 en wordt serieel gepland.

---

Result: **Geland (2026-09-18). Dit is een uchar-CONFORMITEITSKAART, geen
herstel van oud toolchaingedrag.** De oorspronkelijke toolchain schreef bij
een lege scalaire berekening `null` en bij een lege temporele berekening `[]`;
de staande preservation is een bewuste uchar-delta (RS ch. 7.8, `CONSTRUCTS`
rijen 8/18). De classic-DRL-tak deed tot deze kaart precies wat de oude
toolchain deed, en dat was het defect: zij droeg de uchar-norm niet.

**Rood-eerst, letterlijk gemeten** (`mvn -q -o -f backends/drools/pom.xml
package`, daarna de shaded jar met `--drl`; zonder vlag speelden alle vier al):

```
FAIL case3-leeg-valt-door.json: attribuut geschatte waarde: verwacht 40.00, werkelijk leeg
FAIL case6-leeg-doorval-input.json: attribuut waardering: verwacht 40, werkelijk 25
FAIL case5-input-blijft.json: attribuut borgdeel: verwacht [2024-03-01,2024-05-01)=7, werkelijk (leeg)
FAIL case4-input-blijft.json: attribuut borgdagen: verwacht [2024-03-01,2024-04-01)=99, werkelijk (leeg)
```

(respectievelijk `anchor/jutterswet-waardestelling`, `-waardering-v0vorm`,
`-borgdeel`, `-dagentelling`; exec-model gaf op dezelfde vier PLAYED 4 / 6 / 6
/ 4.)

**Groen ná:** alle vier goldens PLAYED in BEIDE takken, verwachtingen
ongewijzigd — `waardestelling` 4, `waardering-v0vorm` 6, `borgdeel` 6,
`dagentelling` 4, zowel zonder vlag als met `--drl`.

**Wat er is gebouwd.** Niet een tweede kopie van de arm, maar het uittrekken
van de AFBAKENING: `transpiler/kern/StaandeDoelen.java` draagt de
ch.-7.8-domeinen (scalair en tijdlijn) en het ch.-7.7-ontsuikerdomein
ref-getrouw (`Desugar.standing_targets_gen` / `standing_temporal_targets` /
`desugared_targets`, clausule voor clausule), en BEIDE takken lezen die ene
afleiding — `ExecModelTranspiler` verloor zijn privé-kopie volledig (149
regels verwijderd, 23 toegevoegd, netto −126; lensmeting), `DrlTranspiler`
kreeg er geen eigen. Wat de norm zichtbaar doet is
beslissen WAAR een lege write wordt onderdrukt, dus de grens is wat gedeeld
moet zijn, niet de garde. De tijdlijn-leegtest verhuisde mee naar de runtime
(`RegelSpraakRuntime.rsTijdreeksGevuld`): het exec-model roept hem aan en de
geëmitteerde DRL-tekst importeert hem, zodat "gevuld" één definitie heeft.
De DRL-tak krijgt de defaultslijst doorgegeven in plaats van een impliciete
lege (haar aanroeper weigert zo'n document al aan de deur) — de
`other_writer`-clausule leest die lijst, en een stil doorgegeven lege lijst
zou het staande domein verbreden zodra die tak ooit wél defaults zou lageren.

Twee emissieplekken in `DrlTranspiler`: de scalaire write krijgt
`<waarde> != null &&` vóór de bestaande idempotentiegarde, en de temporele
write `rsTijdreeksGevuld(<tijdlijn>)` — bij de geankerde vorm vóór
`rsTijdreeksGelijk`, bij de kalendervorm als enige garde, want die vorm
droeg er nooit een en heeft er geen nodig (zij leest geen
startpuntattribuut). Lensmeting bij die geankerde plek: géén corpusgolden
bereikt de geankerde temporele arm van de DRL-tekst-tak (0 emissies van
`rsTijdreeksGelijk`, op main net zo — pre-existent onbeoefend); de
fixpuntrijen bewijzen de `rsTijdreeksGelijk`-garde dus voor de exec-tak,
waar `werkBijTijdlijn` hem onvoorwaardelijk aanroept, en beide
fixpuntmodellen lopen langs de kalenderarm.

**Sweep-beweging** (corpusbreed `--drl`, elke golden met case-bestanden, vóór
en ná op deze tak): **117 → 121 PLAYED van 196**. De per-golden-diff is
EXACT de vier genoemde goldens, van FAIL naar PLAYED; geen enkele andere
golden veranderde van regel, status of kanaal. De drie overige
`--drl`-FAILs zijn onaangeroerd en ongewijzigd — `anchor/jutterswet-kustwacht`
(negatiestratificatie, een echte v0-divergentie) en
`anchor/jutterswet-rekenkamer` + `jutterswet-rekenkamerheffing-catala`
(`verwacht 70, werkelijk leeg`) — geen staande-normdefecten, en niet van deze
kaart. Twee `ERROR ConsequenceException` stonden er vóór en staan er ná.

**Getuigen, bij naam.** `BeideTakken` (nieuw, `src/test/.../cases/`) is de
branchespiegel: één geauteurde case door beide transpilertakken, met hetzelfde
oordeel geëist van elk, en de takken benoemd in de assertieboodschap. Beide
bestaande matrices draaien er nu volledig doorheen:

- `StaandeScalaireNormWitnessTest` — 9 tests (was 8). Alle zes normrijen
  dual-branch (leeg laat invoer staan; zonder invoer blijft leeg; gevuld
  verdringt; naast een initialisatie laat leeg de initialisatie beslissen,
  laat invoer de initialisatie leeg, verdringt gevuld beide), plus de
  tweeschrijversflank dual-branch, plus NIEUW
  `een_versiedoel_komt_niet_in_het_staande_domein` — de waardestelling met een
  geldigheidsvenster op haar enige regel: versie-vrijheid ontbreekt, dus geen
  domein, dus de v0-wis staat. Vooraf tegen `ref` geverifieerd
  (`oracle: PASS (2 expectations)`), en exec ≡ DRL ≡ ref.
- `TemporeelFixpuntWitnessTest` — 7 tests, alle replay-rijen dual-branch: de
  twee fixpuntrijen (twee en drie temporele schrijvers) én de vier
  ch.-7.8-tijdlijnrijen (lege pro-rata / lege dagtelling laten de invoerreeks
  HEEL staan; gevulde vervangen haar geheel, nooit per bucket). De laatste rij
  (`Segment` draagt geen `equals`) blijft taakonafhankelijk — zij stopt bij de
  invoerlaag, vóór enige regel vuurt — en zegt dat nu ter plekke.
- **De ene uitzondering, benoemd in plaats van geschrapt:** de
  WF-050-ongeldige defaults-mechaniekrij. De DRL-tekst-tak heeft geen
  ch.-7-defaultslagering en weigert zo'n document als geheel; die rij assert
  nu de WEIGERING in die tak (`CaseRunner.Excluded`, boodschap bevat
  `prioritizedDefaults`) naast de exec-uitkomst — zodat een tak die zo'n
  document stilletjes zou gaan spelen hier omvalt.

**Mutatieproeven, feitelijk uitgevoerd en hersteld** (vier, elk aantoonbaar
rood, telkens met de tak in de faalregel):

1. *altijd-schrijvend* (de DRL-garde weg): 4 rood, alle vier `drl-tekst` — de
   twee scalaire doorvallen en de twee tijdlijndoorvallen.
2. *nooit-schrijvend* (garde altijd waar): 4 rood, alle vier `drl-tekst` — de
   twee scalaire verdringingen en de twee tijdlijnverdringingen.
3. *afbakeningsmutant* (classificatie weg, elke null-write geskipt): 2 rood,
   `drl-tekst`, en precies de twee niet-staande flanken —
   `twee_rekenregels_op_een_doel_wissen_de_invoerwaarde_wel` en
   `een_versiedoel_komt_niet_in_het_staande_domein`. Dit is de rij die de
   kaart eist: de grens, niet de garde.
4. *gedeelde afleiding leeggemaakt* (in `StaandeDoelen` zelf): 5 rood, met
   `exec-model` in de faalregels — het bewijs dat ook de exec-tak werkelijk
   díe ene afleiding leest en geen eigen kopie meer heeft.

**Gerichte checks.** `mvn -o -f backends/drools/pom.xml package` mét tests:
`Tests run: 409, Failures: 0, Errors: 0, Skipped: 0` (was 408; +1 = de
versieflank). Gate 12 `check-drools`: PASS, `played 148` — `EXPECTED_PLAYED`
ONGEWIJZIGD op 148, auto-excluded 47, value-divergence 1, no-case 2,
kanaalvloer 50 goldens bij naam, `JUnit 409 tests, floor 239`. Gate 13
`check-reftype-derivation`: PASS, 108 annotaties over 22 goldens (vloer 74),
100 executor-replays, **21 daarvan PLAYED door de DRL-tekst-tak vóór én ná**
(vloer 13) — zelf gemeten op de basis-broncode en op de nieuwe, 0 gepinde
survivors.

**Alle veertien gates, exitcodes:** self-contained 0, ir-schema 0, verifier 0,
case-schema 0, oracle 0, clingo 0, noop 0, rood 0, catala 0, regelspraak 0,
toka-parity 0, drools 0, reftype-derivation 0, flogic 0.

**May touch.** `git diff --name-only main...HEAD` geeft negen paden, alle
binnen de kaart: `transpiler/DrlTranspiler.java`,
`transpiler/model/ExecModelTranspiler.java` (uitsluitend het delen van de
classificatie), de nieuwe `transpiler/kern/StaandeDoelen.java`,
`runtime/RegelSpraakRuntime.java` (de gedeelde writegarde),
`cases/CaseRunner.java`, drie bestanden onder `src/test/`, en deze kaart.
Geen `scripts/`, geen `spec/`, geen corpus.

**Bewuste restscope.** De DRL-tekst-tak krijgt géén
prioritized-defaultslagering (out of scope; de weigering is nu geassert). De
`verdeling`- en `objectcreatie`-takken van `other_writer` blijven zonder
corpusmodel — legaal, maar geen enkele golden draagt de vorm; dat stond al zo
in de oorspronkelijke getuige en is niet verbreed. De drie overige
`--drl`-FAILs blijven staan, met reden, en horen bij andere kaarten.

**Registervoorstel — NIET geland (beschermd pad `scripts/`).** Er is één stuk
werkelijk register-stale proza en het zit in gate 12 zelf,
`scripts/check-drools.sh` clausule (d'), regel ~148-155. Daar staat nu dat het
ch.-7.8-scalaire domein plus het ch.-7.7-ontsuikerdomein zijn "ported into
ExecModelTranspiler and read at the scalar write boundary (werkBij)". Sinds
deze kaart is dat de halve waarheid: de domeinen staan in
`transpiler/kern/StaandeDoelen.java` en worden door BEIDE Drools-takken
gelezen. Voorgestelde exacte vervanging van die deelzin:

> ported into the SHARED `transpiler/kern/StaandeDoelen` — one derivation,
> read by BOTH Drools branches: the exec-model branch at its scalar write
> boundary (`werkBij`) and the DRL-text branch at its emitted `modify` guard
> (card drl-staande-waarden, 2026-09-18, which built the DRL-text arm; the
> timeline emptiness test is `RegelSpraakRuntime.rsTijdreeksGevuld`, likewise
> shared)

Aanvullend, in dezelfde clausule, achter de slotzin over de domeinpoort:
"— pinned by `StaandeScalaireNormWitnessTest`, sinds 2026-09-18 dual-branch
(`BeideTakken`) en met de versieflank ernaast."

`spec/CONSTRUCTS.md` rij 8 en rij 18 zijn NIET stale: geen van beide beweert
dat de classic-DRL-tak de norm niet draagt ("both executors" daar slaat op
`ref` en clingo). Een optionele `**Landed**`-clausule voor rij 8 zou luiden:
"**Landed (2026-09-18, kaart drl-staande-waarden):** de classic-DRL-tak van
`backends/drools` draagt de ch.-7.8-norm nu ook — scalair én tijdlijn — en
beide Drools-takken lezen ÉÉN doelclassificatie (`StaandeDoelen`), zodat de
afbakening niet per tak kan verschuiven; vier goldens van FAIL naar PLAYED in
die tak (117 → 121 corpusbreed), gate 12's `EXPECTED_PLAYED` onveranderd 148."
