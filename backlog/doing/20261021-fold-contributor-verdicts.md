# fold-contributor-verdicts: de lezing van een kwantorvouw legt het oordeel van elk lid vast, met de stop als positie

*Vastgelegd door de hoofdsessie op 2026-10-21 uit Fable-advies 2
(`werk/defaults-decider/fable-advies-2.md` in `uchar-dev`, §3 weg A, §4). Besluit van de eigenaar 2026-10-21: weg A (de
lezing legt het oordeel van elk lid vast), niet B (de lezing blijft bij de stopregel en de uitleg beperkt zich daartoe);
het advies van Fable wordt meegenomen.*

## Aanleiding

RUNTIME-SEMANTICS ch. 5.12 legt de bijdragers van een kwantorvouw na de stop vast als `unreached`, zonder oordeel. De
Drools-backend evalueert de matrix per lid in een eigen regel en geeft elk lid een oordeel; waar de matrix falen noch
deponeren kan, houdt zijn sessie geen volgorde en schrijft hij `unordered` (211 van de 264 vouwen in de vergelijking van
gate 17, 80 %). De canonieke tekst van 5.12 is daardoor alleen canoniek via de afwijkende vergelijkingsregel van gate 17.
De toolchain-kaart `explanation-fold-members` (besluit van de eigenaar: ook leden na de stop) leest die oordelen; onder
de huidige 5.12 is dat informatie die alleen Drools levert.

Daarnaast verschillen ref en Drools al op twee plaatsen die gate 17 niet bereikt: na een lid met een fout (ref schrijft
niets, Drools `unreached`) en bij een filter met een fout (ref geen bijdragers, Drools alle `unreached`).

## Doel (weg A)

- **Spec, ch. 5.12** (voorstel; beschermd pad): bij "Folds over a set" per bijdrager het oordeel van de matrix of zijn
  fout, de tabel van ch. 3.9a's `test(m)` over de bijdragers, op de fixpuntwereld zoals een lezing evalueert, zonder
  deponering, plus of de evaluatie het lid bereikte; een oordeel of fout na de stop is van de lezing, niet van de
  sessie, en verandert de status niet; bij een filter met een fout geen bijdragers. De canonieke tekst
  `contributors [<object> <verdict|fault> reached|unreached; …]` (`- unreached` vervalt) en één zin over `unordered`
  als erkende "geen positie" (naar ch. 5.11). De definitiezin krijgt "and, for a fold over a set, the table its
  definition in ch. 3.9a fixes". Twee grenzen als zin in "The artifacts": `FAULTS` blijft buiten het differentieel (een
  golden faalt niet aan het fixpunt); een oordeel van een niet-bereikt lid kan rusten op een deponering die de lezing
  wegliet.
- **ref**: in de leesmodus (alleen met waarnemer; gate 5 bytegelijk) na de stop en vóór een fout elk overgebleven lid
  evalueren en met `reached = false` bijdragen; `Member_unreached` vervalt. Een ref-test `WV-5.12-<golden>` die de tabel
  uit een golden leest.
- **Drools**: het filter met een fout geeft geen bijdragers (één regel, een getuige in `ReadingsWitnessTest`); de
  javadoc van `Contributor.reached`: de positie, `null` waar de vouw er geen heeft.
- **Gate 17**: de vergelijkingsregel per bijdrager letterlijk op lid en oordeel of fout, de positie waar beide er een
  schrijven; de rij "unreached without a verdict agrees" keert om; een struikeldraad: schrijft Drools `unordered`, dan
  draagt geen bijdrager van ref een fout; een vloer `EXPECTED_MIN_UNREACHED_CONTRIBUTORS` (bij de landing gemeten).
- **Een getuige-golden**: een vouw `all` waarvan lid 1 niet geldt (stop) en waarvan de matrix op lid 2 de rekendatum
  leest in een sessie zonder datum (EF-011); het kanaal blijft `consistent`; de lezing toont `fault EF-011 unreached` op
  beide kanten.

## Acceptatie

- (a) Rood eerst: de nieuwe ref-test en de getuige-golden rood op de stand vóór de kaart.
- (b) Mutant: ref die na de stop de leden als `reached` schrijft maakt gate 17 rood.
- (c) De zeventien gates groen; gate 5 bytegelijk.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 5.12 (voorstel), `ref/lib/eval.ml`, `ref/lib/reading.ml`, `ref/test/`,
`backends/drools` (het filter, de javadoc, een getuige), `scripts/check-readings.sh` (voorstel), één golden, deze kaart.

Out of scope: de toolchain (geen API-wijziging; de pin-bump van `fixpoint-inconsistency-order` brengt de spec mee);
deponeringen naast het oordeel in de tabel (pas als de vorm in een regelset voorkomt).

## Result (2026-10-21)

Gebouwd door een Opus-bouwer; de hoofdsessie zette de tak boven op main `b88df48e` (de kaarten
`fixpoint-inconsistency-order` en `reading-after-refusal`, zonder conflict), draaide de volledige set, landde de
voorstellen (`spec/RUNTIME-SEMANTICS.md` ch. 5.12, `scripts/check-readings.sh`, `scripts/check-drools.sh`) en de
`CLAUDE.md`-tekst, zette de clingo-vondst als kaart in `backlog/new/20261021-clingo-day-kind-walk-after-fold-stop.md`,
en schreef dit Result af. De commit-hashes hieronder zijn van vóór die herbasering.

Tak `card/fold-contributor-verdicts`, vanaf `0f45965f`. Geraakt: `ref/` (`lib/eval.ml`, `lib/reading.ml`, `test/`),
`backends/drools` (het filter in `TmsTranspiler.contributors`, de javadoc van `ConditionReading.Contributor`, een
getuige in `ReadingsWitnessTest`, en `SessionDump`, zie onder), `spec/RUNTIME-SEMANTICS.md` ch. 5.12 (voorstel),
`scripts/check-readings.sh` en `scripts/check-drools.sh` (voorstel), één nieuwe golden. Niet geraakt: `backends/clingo`,
`spec/verifier/`, `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`, `TmsTranspiler.meet`, `Fixpoint.inconsistencies`,
de rijen van `READINGS_ERROR_KNOWN`.

- `a5a9b872` de kaart naar `doing/` (hoofdsessie).
- `5bb592e1` de getuige-golden `jutterswet-keurgemiddelde`; gate 12 `EXPECTED_PLAYED` 261 → 262.
- `d779af42` ref: de lezing van een kwantorvouw legt de tabel van de matrix vast; twee ref-tests.
- `c52d04e0` Drools: een vouw waarvan het filter faalt, heeft geen bijdragers; javadoc; getuige.
- `8cb4afdd` RUNTIME-SEMANTICS 5.12 (voorstel).
- `39baf742` gate 17: de vergelijkingsregel, de struikeldraad, de vloer (voorstel).
- `b29fe750` `SessionDump`: een lijst lidtoetsen in de volgorde van de plaatsen.
- `cd4d9f32` gate 17: de vloermelding in woorden.

### Wat er is

**ref.** `Eval.contribution` is nu `{ found : member_found; reached : bool }`, met `member_found` = `Member_value` |
`Member_verdict` | `Member_fault`; `Member_unreached` vervalt. `contribute ?(reached = true)`. In de kwantorvouw
(`eval_cond_node`, arm `Quantifier_fold`) evalueert `unreached` met een waarnemer elk lid dat de evaluatie niet bereikt
— na de stop (`settled`) en, vóór de `raise`, na een lid met een fout — met `~err:ignore`, en meldt het met
`reached = false`, het oordeel of de fout (`Value.Fault` gevangen, met de EF-code). Zonder waarnemer doet `unreached`
niets. Een lid na de stop waarvan de matrix een binder van een omsluitende `Let` zou forceren, weigert de lezing bij
naam (`failwith` met `ref/lib/eval.ml` en de binder): forceren zou een waarde evalueren die de sessie niet evalueerde en
de knopen ervan als bereikt noteren. Geen document in het corpus heeft een binder in een kwantormatrix (gemeten). In
`reading.ml` schrijft `lines` de vondst en daarna `reached`/`unreached` uit de vlag.

Gemeten over het corpus: de replay (`runner` zonder `--trace`) van alle 839 golden-case-bestanden is bytegelijk aan die
van vóór de wijziging (een runner uit `git archive 5bb592e1`, uitvoer vergeleken met `cmp`); in de trace veranderen
precies de 106 regels die `- unreached` droegen: 118 bijdragers in 17 goldens krijgen hun oordeel (65 `false`, 53
`true`; 116 in de 16 goldens van vóór de kaart, 2 in de nieuwe), geen enkele een fout.

Twee ref-tests: `WV-5.12-keurgemiddelde` leest de tabel uit de golden (de vier cases) en
`WV-5.12-fout-in-een-kwantorvouw` toetst de foutarmen op de invoerwereld van een eigen document (zoals
`WV-5.12-fout-in-een-lezing`): een matrix `ALLE [gekeurd ; Unresolved]` en een filter `TEN MINSTE 1 [gekeurd ;
Unresolved]`. Verwacht en gekregen:

```
/rules/rules/0/versions/0 "P1" 0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]
/rules/rules/0/versions/0 "P1" FAILS
/rules/rules/0/versions/0 "P2" 0 QuantifierFold fault EF-019 contributors ["W3" fault EF-019 reached; "W4" false unreached]
/rules/rules/0/versions/0 "P2" FAULTS
/rules/rules/1/versions/0 "P1" 0 QuantifierFold fault EF-019
/rules/rules/1/versions/0 "P1" FAULTS
```

(en P2 op regel 1 als P1). Suite 1306 → 1311.

**Drools.** `TmsTranspiler.contributors`: `if (s.term() instanceof Term.FoldRead f && rec.outcome(f.outcome()) == null)
return out;` — viel de evaluatie van de vouw vóór haar waarde (een fout van het filter, of van de waarde van een lid
van een waardevouw over een tijdlijn), dan zijn de bijdragers niet bepaald. Tot nu toe gaf de lezing dan de leden
waarvan het filter gold: geordend allemaal `unreached`, ongeordend met `unordered`. De begintoestand `stopped =
rec.outcome(read.contributions()) == null` is daarmee altijd `false` en is zo geschreven. De javadoc van
`ConditionReading.Contributor`: de tabel van ch. 3.9a, `reached` de positie, null waar de vouw er geen heeft, geen
bijdragers na een filter met een fout. Getuige `ReadingsWitnessTest.the_table_of_a_quantifier_fold_with_its_faults`,
hetzelfde document als de ref-test, via `--readings` op beide takken; de canonieke tekst is die van ref hierboven.

`SessionDump` (niet in het advies; nodig): de nieuwe golden liet `BranchDumpsCorpusWitnessTest` vallen op alle vier
cases van de build met lezingen. Het `Reading`-feit van een geordende kwantorvouw draagt de lijst `MemberTest` die een
accumulate verzamelde, in de volgorde van de agenda, en die verschilt tussen de takken (exec-model `[plaats 0, plaats
1]`, DRL-tekst `[plaats 1, plaats 0]`); `quantifierRead` sorteert de lijst vóór het lezen. `render` schrijft een lijst
van alleen `MemberTest` nu in hun natuurlijke volgorde (op plaats). De bestaande goldens met een geordende vouw over
meer leden (`keurploeg`, `ploegpremie`, `reddingsvloot`, `sleepvloot`) vielen niet; daaruit volgt dat de accumulate daar
op beide takken in dezelfde volgorde verzamelde (gevolgtrekking uit de groene test, niet apart gemeten).

**RUNTIME-SEMANTICS 5.12** (voorstel). (1) De definitiezin: "…once, at the end, and, for a quantifier fold, the table
its definition in ch. 3.9a fixes." (2) "Folds over a set": per bijdrager het oordeel of de fout — "the table of
ch. 3.9a's `test(m)` over the contributors, evaluated on the fixpoint world as a reading evaluates, depositing nothing"
— en of de evaluatie hem bereikte; na de stop en na een fout niet bereikt; "Their verdict or fault is the reading's, not
the session's … and the status of the reading does not change by it"; "Where the evaluation of a fold faults before its
contributors are settled — its filter faults, or the value of one of its members — the node carries no contributors";
`unordered` als erkende geen-positie ("ch. 5.11: an invisible order"). (3) De canonieke tekst:
`contributors [<object> <value|verdict|fault> reached|unreached; …]`, `unordered` in plaats van de positie; `- unreached`
vervalt. (4) Een tweede uitgewerkt voorbeeld, `jutterswet-keurgemiddelde`, met de tabel van de vier cases en de
canonieke tekst van case 3. (5) "The artifacts": de vier ref-tests, de Drools-getuige, de regel van gate 17 en de
struikeldraad; drie grenzen: `FAULTS` en een fout aan een knoop buiten het differentieel; een fout van een bijdrager na
de stop eveneens, gemeten (onder); een oordeel van een niet-bereikt lid kan op een weggevallen deponering rusten (W2 in
case 1, W3 in case 3). De twee "consequences" bleven ongewijzigd en blijven waar.

**Gate 17** (voorstel). De vergelijkingsregel (`contributors_agree`): de leden gelijk, per lid de vondst letterlijk
(`rv != dv` faalt) en de positie waar beide er een schrijven (Drools `unordered` slaat haar over). Vóór:

```python
        if dr != 'unordered' and rr != dr: return False
        if rr == 'reached' and rv != dv: return False
```

na:

```python
        if rv != dv: return False
        if dr != 'unordered' and rr != dr: return False
```

De struikeldraad (`unordered_faults`): schrijft Drools voor een knoop `unordered`, dan draagt geen bijdrager van ref op
die knoop een fout; hij draait op elk vergeleken case-bestand, ook een in `DEVIATIONS`, met een eigen melding. Rijen:
de rij "the backend verdict after the stop is not read" (agree) is omgekeerd tot "a verdict after the stop apart
differs" (differ); erbij "a fault after the stop against a verdict differs" en "an equal fault after the stop agrees";
de twee `unordered`-rijen lezen nu ook een oordeel na de stop: twaalf rijen (waren tien), plus drie rijen van de
struikeldraad. De vloer `EXPECTED_MIN_UNREACHED_CONTRIBUTORS=106`: de bijdragers van ref met positie `unreached` in de
vergeleken case-bestanden (exec-tak), gemeten 2026-10-21 (Fable schatte ~110); de 12 andere van de 118 staan in goldens
die Drools uitsluit of niet leest.

**De getuige-golden `jutterswet-keurgemiddelde`** (handgeschreven IR, vier cases, `NOTE.md`): `Een Ploeg is inzetbaar
indien alle wachten van de Ploeg aan alle volgende voorwaarden voldoen: hij is gekeurd; ⌊keurpunten ÷ keuringen⌋ groter
dan 6`, en `wachttal` als telling (zodat elke case gate 7 weerlegt). Case 1: W1 niet gekeurd (stop), W2 met 0 keuringen
→ consistent; case 2: beide gelezen, `inzetbaar`; case 3: W2 beslist (stop), W3 met 0 keuringen → consistent; case 4: W2
met 0 keuringen bereikt → `inconsistent` EF-005 op `inzetbaar bij keurgemiddelde` × P1 (de controle). Het vierkant is
het eens (ref, clingo en Drools beide takken `PLAYED 4`), de lezingen van ref en Drools zijn gelijk.

### Anders dan kaart en advies, met reden

1. **De getuige-golden draagt een deponering, geen fout.** De kaart vroeg een lid na de stop dat de rekendatum leest
   in een sessie zonder datum (EF-011), met `fault EF-011 unreached` op beide kanten. Gemeten 2026-10-21, elk met de
   getuige-vorm (`ALLE [gekeurd ; <apparaat>]`, lid 1 niet gekeurd): clingo geeft `TRANSLATION FAULT: EF-011` voor elk
   document dat het rekenjaar leest zonder `asOfDate`, ook als het lid nooit gelezen wordt, dus gate 6 valt; `Unresolved`
   (EF-019) idem, `Unresolved condition reached in translation`; de dagsoortwandeling (EF-009) vuurt in clingo voor elk
   lid van het bereik, ook na de stop (`viol("day-kind walk: …","W2")`); een ongebonden parameter (EF-014) bewaakt clingo
   lui, maar Drools leest hem als leeg (gate 8, `aanvoergeld-parameter-unbound|NOFAULT`), zodat de lezing `"W2" false
   unreached` tegenover ref's `"W2" fault EF-014 unreached` zou geven en gate 17 een `DEVIATIONS`-regel nodig had. Geen
   fout haalt dus het vierkant én gelijke lezingen. De deling door nul (EF-005, §9.5) wel: zij pint voor het eerst als
   golden dat een kwantorlid na de stop niets deponeert (de getuigen van ch. 5.10 zijn alleen `Compound`), en zij maakt
   de derde grens van 5.12 (een oordeel op een weggevallen deponering) zichtbaar in gate 17. De foutarmen dragen de
   eigen tests van beide uitvoerders; de grens staat in 5.12. Bewijsdocumenten in `u5/probes/`.
2. **"for a quantifier fold"** in de definitiezin, niet "for a fold over a set": de tabel van ch. 3.9a is die van de
   kwantorvouw; een waardevouw heeft haar definitie in ch. 3.4.
3. **De Drools-regel is algemeen**: geen bijdragers zodra de `FoldRead` vóór haar waarde faalde, dus ook bij een fout van
   de waarde van een lid van een waardevouw over een tijdlijn. ref schrijft daar al niets (`contributing_values` na het
   evalueren van alle waarden); de zin in 5.12 noemt beide.
4. **ref weigert een buitenste Let-binder** in de matrix van een niet-bereikt lid luid (punt hierboven); het advies
   zegt hier niets over.
5. **`SessionDump`** (punt hierboven); niet in het advies, nodig voor de Drools-suite met de nieuwe golden.
6. **De struikeldraad** draait ook op een case in `DEVIATIONS` en heeft eigen rijen; de vloer telt de kant van ref.
7. **Gate 12**: `EXPECTED_PLAYED` 261 → 262 (de nieuwe golden speelt).

### (a) Rood eerst

ref, de suite met de twee nieuwe tests op de code van vóór de wijziging (`u5/logs/red-ref-1.log`), 3 failures:

```
FAIL WV-5.12-keurgemiddelde case1-stop-voor-de-deling.json
  got: /rules/rules/0/versions/0 "P1" 0 QuantifierFold false contributors ["W1" false reached; "W2" - unreached]
FAIL WV-5.12-keurgemiddelde case3-het-tweede-lid-beslist.json
  got: … contributors ["W1" true reached; "W2" false reached; "W3" - unreached]
FAIL WV-5.12-fout-in-een-kwantorvouw
  got: /rules/rules/0/versions/0 "P1" 0 QuantifierFold false contributors ["W1" false reached; "W2" - unreached]
  got: /rules/rules/0/versions/0 "P2" 0 QuantifierFold fault EF-019 contributors ["W3" fault EF-019 reached]
```

Drools, `ReadingsWitnessTest` met de nieuwe getuige op de code van vóór de wijziging (`TmsTranspiler` gestasht,
`u5/logs/red-drools-2.log`), 1 failure: `reading /rules/rules/1/versions/0 "P1" 0 QuantifierFold fault EF-019
contributors ["W2" true unordered]` waar geen bijdragers verwacht zijn. (Een eerste versie van de getuige, met een
filter dat op elk lid faalt, was al groen vóór de wijziging: zonder lid dat het filter doorkomt is er geen
`Contribution`; `red-drools-1.log`. Het filter faalt nu alleen op een lid dat niet gekeurd is.)

De golden zelf is groen op de oude code in gates 5, 6 en 12 (zijn antwoorden veranderen niet); rood zijn op de oude ref
de tabel van `WV-5.12-keurgemiddelde` en, onder de nieuwe vergelijkingsregel, gate 17 op case 1 en 3 (de mutant
hieronder schrijft dezelfde posities fout).

### (b) Mutant (niet gecommit, één keer)

ref schrijft de leden na de stop als bereikt: in `unreached` `contribute ~reached:false m.label found` →
`contribute m.label found`. Gate 17 (`u5/logs/gate17-mutant.log`, 432 s): `rc=1`, 20 (case, tak)-paren verschillen —
10 case-bestanden op beide takken, in `jutterswet-keurgemiddelde` (2), `keurploeg` (2), `ploegpremie` (4), `reddingsvloot`
(1) en `sleepvloot` (1) — en de vloer valt:

```
check-readings: FAIL — jutterswet-keurgemiddelde/case1-stop-voor-de-deling.json (exec): the trace and the readings differ:
      ref     /rules/rules/0/versions/0 "P1" 0 QuantifierFold false contributors ["W1" false reached; "W2" false reached]
      backend /rules/rules/0/versions/0 "P1" 0 QuantifierFold false contributors ["W1" false reached; "W2" false unreached]
check-readings: FAIL — 0 unreached_contributors compared, but EXPECTED_MIN_UNREACHED_CONTRIBUTORS pins at least 106 — …
```

De vloer vangt ook de 211 vouwen waar Drools `unordered` schrijft en de positie niet vergeleken wordt. Teruggezet met
`git checkout ref/lib/eval.ml`; de melding van de vloer noemt sindsdien "unreached contributors" (`cd4d9f32`).

### Gates, zelf gedraaid

- ref-suite: 1311 worked examples (`u5/logs/green-ref-1.log`).
- gate 1 PASS; gates 2, 3, 4 en 7 PASS met de golden (`u5/logs/gates-234-1.log`: 351 documenten, 884 case-bestanden,
  794 cases weerleggen de no-op).
- gate 5 PASS (suite 1311, 839 cases; `u5/logs/gate5-1.log`).
- gate 6 PASS (839 cases, suite 2198 checks; `u5/logs/gate6-1.log`, 223 s).
- gate 12 `--fast` PASS, played 262 (`u5/logs/drools-12-suite-1.log`).
- gate 17 PASS op `39baf742` (`u5/logs/gate17-1.log`, 458 s; vloer toen op 0 gezet om te meten): ref 839
  golden-case-bestanden; per tak 262 goldens, 710 case-bestanden, 3830 lezingen, 13975 knoopregels waarvan 3118
  `unreached`, 318 met bijdragers, 76 met een tijdlijn, 106 bijdragers van ref niet bereikt; `DEVIATIONS` leeg.
- Drools: `ReadingsWitnessTest` groen (6), `BranchDumpsCorpusWitnessTest` en `SessionDumpWitnessTest` groen na de
  dumpfix (`u5/logs/dumps-1.log`); de hele suite (`mvn -o test`) op `cd4d9f32`: 1647 tests, 0 failures, 0 skipped
  (`u5/logs/drools-12-suite-1.log`, 327 s). De eerste suiterun, vóór de dumpfix, gaf 1 failure in
  `BranchDumpsCorpusWitnessTest` (`u5/logs/drools-suite-1.log`).
- gate 13 PASS (205 s) en gate 15 PASS (14 s) (`u5/logs/gates-13-15-1.log`).
- Niet gedraaid: gates 8–11, 14, 16 en de volledige gate 12; de hoofdsessie draait de set.

### Volledige set, door de hoofdsessie

Op `b7cb1697` (na de herbasering; `werk/defaults-decider/u5/gates-1`): alle zeventien `rc=0`, `EXIT 0`. Gate 12 698 s,
played 262 op beide takken, JUnit 1649; gate 17 382 s: 839 case-bestanden getraceerd, 262 goldens en 710 case-bestanden
vergeleken, 3830 lezingen, 106 niet-bereikte bijdragers van ref, afwijkingen 0, 12 vergelijkingsrijen en 3 rijen van de
struikeldraad.

### Vondsten buiten de kaart

- **clingo vuurt het hek van de dagsoortwandeling (EF-009) op een kwantorlid na de stop.** In een samenstelling
  (ch. 5.10) bewaakt de prefix-wacht het lid (`u5/probes/samenstelling-dagsoortwandeling`: clingo PASS), in de matrix
  van een kwantorvouw niet: het hulpatoom van de wandeling wordt voor elk lid van het bereik afgeleid
  (`u5/probes/kwantor-dagsoortwandeling/case1.json`: `UNSATISFIABLE — … viol("day-kind walk: no day of the kind within
  40000 calendar days …","W2")`, waar ref PASS geeft). Afwijking van ch. 3.9a ("a member never reached produces no
  fault"). Voorstel: een kaart in `backends/clingo`; de golden uit de probe is dan de getuige.
- EF-011 en EF-019 in clingo zijn vertaalfouten ("TRANSLATION FAULT", ontwerp), en Drools leest een ongebonden parameter
  als leeg (bekend, gate 8): samen de reden dat geen golden een fout na de stop kan dragen (5.12, de tweede grens).

### Voor de toolchain (T3)

Geen API-wijziging: `ConditionReading.Contributor(member, value, fault, reached)` blijft. Twee gedragsveranderingen die
T3 leest: een vouw waarvan het filter (of de waarde van een lid) faalt, heeft nu geen bijdragers; en de normatieve bron
voor een oordeel na de stop is 5.12, met ref als tweede uitvoerder en gate 17 als getuige.

### Voorgestelde tekst voor `CLAUDE.md` (gate 17)

Vervang in gate 17 de zinnen vanaf "Every line is compared verbatim" tot en met "passes ten rows of its own." en de
vloerenlijst door:

```
    Every line is compared verbatim except the position of a
    contributor of a quantifier fold: since the card
    `fold-contributor-verdicts` (2026-10-21) ch. 5.12 records the table
    of ch. 3.9a at that node, so per contributor the member and its
    verdict, value or fault are compared verbatim, and its position
    (`reached`/`unreached`) where both write one (`unordered` where the
    backend's session keeps no order). Where the backend writes
    `unordered`, no contributor of the trace may carry a fault (a
    tripwire, held on a listed case too). A difference fails unless the
    reason-carrying `DEVIATIONS` names the case and the branch (empty
    since the gate began); both lists fail when stale, in both
    directions, and the comparison rule passes twelve rows of its own and
    the tripwire three. Floors `EXPECTED_MIN_TRACED_CASES=835`,
    `EXPECTED_MIN_COMPARED_GOLDENS=261`, `EXPECTED_MIN_COMPARED_CASES=706`,
    `EXPECTED_MIN_READINGS=3826`, `EXPECTED_MIN_NODE_LINES=13971`,
    `EXPECTED_MIN_UNREACHED=3118`, `EXPECTED_MIN_CONTRIBUTOR_LINES=314`
    and `EXPECTED_MIN_TIMELINE_LINES=76` (measured 2026-10-21), and
    `EXPECTED_MIN_UNREACHED_CONTRIBUTORS=106` (contributors of the trace
    not reached, measured 2026-10-21, card `fold-contributor-verdicts`).
```

Gate 12 noemt het getal van `EXPECTED_PLAYED` niet in `CLAUDE.md`; geen tekst nodig. Geen andere gate telt de golden
in een tekst van `CLAUDE.md`; de vloeren van gates 2–7 en 15 zijn ondergrenzen en blijven.

Voor `spec/CONSTRUCTS.md`: geen rij (5.12 is een clausule over wat uitvoerders lezen, geen vorm en geen afwijking van
v0). Voor `spec/PLAN.md`: geen scopewijziging; de clingo-vondst hierboven is een kaart voor `backlog/new/`.
