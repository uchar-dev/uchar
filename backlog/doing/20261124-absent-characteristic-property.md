# absent-characteristic-property: een kenmerk dat vast niet geldt, ook als zijn naam geen eigenschapsnaam is

*Gemeld vanuit de werkbank, 2026-11-24, bij het strikt maken van een regelset buiten deze repository (op toolchain
`5a14ba4f4`, uchar `e25f201ef`). Vóór de bouw gelezen door een extern model (xhigh) in de hoofdsessie; de punten
onderaan zijn eisen. Nog niet opgepakt.*

## Aanleiding

In een document met `Afleiden uit leeg: nee` geeft een casus van een kenmerk dat het vast niet geldt
(`characteristicsAbsent`; in de toolchain een strikt verzoek met `{"state": "holdsNot"}`, in een scenariotoets
"- heeft geen X" of "- is niet X"). Heeft het kenmerk een naam die geen Java-eigenschapsnaam is, met een spatie, een
streepje, een hoofdletter of een diakriet, dan faalt de sessie:

```
IllegalStateException: geen getter voor de eigenschap '<gedeclareerde naam>' op dev.uchar.regelspraak.facts.<Type>:
get<gedeclareerde naam> noch is<gedeclareerde naam>
```

In die regelset ging het om drie kenmerken met een spatie en streepjes in de naam. 50 van de 233 scenario's en de
smoke van de dienst vielen erdoor om; met het herstel hieronder slagen ze alle.

## Plek

Twee aanroepen van `Accessors.leesEigenschap` geven de gedeclareerde naam van het kenmerk door, waar die methode de
eigenschapsnaam van de bean verwacht:

- `backends/drools/src/main/java/dev/uchar/regelspraak/cases/CaseInput.java` r. 481:
  `Accessors.leesEigenschap(bean, k)`;
- `backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java` r. 467:
  `Accessors.leesEigenschap(a.bean(), a.characteristic())`.

Daarnaast lezen `CaseInput.java` r. 467 (de kenmerken die gelden) en `transpiler/tms/SessionReaders.java` de bean wel
met `Namen.property(…)`.

## Herstel

`Namen.property(k)` en `Namen.property(a.characteristic())` op die twee plekken. In een kopie van de toolchain met
precies die twee regels gewijzigd verdween de fout; geen andere uitkomst veranderde.

Een tweede lezer (Neon, xhigh) wees op `DroolsBackend.java` r. 463: daar staat een exacte `contains` op de gedeclareerde
naam zonder `Namen.norm`. Via `CaseInput` gaat dat goed, omdat die eerst `declaredCharacteristic` aanroept, maar een
directe aanroeper van `Input.characteristicAbsent` met een andere schrijfwijze zou het kenmerk niet vinden. Kijk na of dat
een fout is of een contract.

## Acceptatie

- Rood eerst: een golden of fixture van de Jutterswet met een kenmerk met een spatie of streepje in de naam (en een met
  een hoofdletter of diakriet), dat de casus in een strikt document als vast niet geldend geeft; de casus faalt vóór het
  herstel met de zin hierboven en geeft erna het kenmerk als `holdsNot`.
- Dezelfde casus in Drools en in `ref` (en clingo waar die het draagt) met dezelfde uitkomst.
- Daarna een toolchain-pin met het herstel; de werkbank wacht daarop met haar eigen pin.

## Uit de lezing vóór de bouw (extern model, nagerekend) — eisen

- **De diagnose klopt, het herstel is volledig:** geen andere plek in de backend gebruikt een gedeclareerde naam als
  eigenschapsnaam (invoer, vergelijking, eindstand, lezingen en beide takken gaan via `Namen.getter`/`setter`/`property`).
  Kleine correctie: `CaseInput` r. 467 schrijft de geldende kenmerken via `Namen.setter`, zij lezen niet.
- **`DroolsBackend` r. 463 is een inconsistentie, geen contract:** elders matcht `insert` onder `Namen.norm`. Los
  `a.characteristic()` eerst op tot de gedeclareerde naam (zoals `CaseRunner.declaredCharacteristic`) en geef die door aan
  r. 465 (`characteristicGranularity`), r. 467 (`Accessors.leesEigenschap(a.bean(), Namen.property(declared))`) en r. 470
  (`ProducerConsumer.elementOf`).
- **Welke namen echt falen:** een spatie (`"op zee"`), een streepje (`"niet-erkend"`), een apostrof, een woord in
  hoofdletters van twee of meer tekens (`"EU"`: de bean heet `isEu`), een begincijfer of superscript (`"3e lid"`, `"m²"`),
  en `İş`. Een beginhoofdletter of een gewone diakriet (`"reëel"`) faalt niet: neem in de rode test namen die echt falen.
- **Acceptatie:** ook een gewoon document (`characteristicsAbsent` crasht daar net zo, terwijl het daar niets mag
  veranderen); een Jutterswet-casus die ref, clingo en Drools (exec-model en DRL-tekst) gelijk spelen, met de verwachting
  `characteristicAbsent`; een directe test op `DroolsBackend.insert` met `Input.characteristicAbsent` (slaagt als de bean het
  kenmerk niet heeft, weigert met DROOLS-371 als de bean het wel heeft, ook bij een andere schrijfwijze); EF-047 voor zo'n
  naam in zowel `characteristics` als `characteristicsAbsent` (nu een `IllegalStateException` in plaats van `NameFault`).
  De toolchain- en werkbank-pin zijn een vervolgstap buiten deze repository.

## Result

*Bouwer, 2026-11-24. Branch `card/absent-characteristic-property`, vier commits na de claim (`bc41795d5`) en dit
Result; niet gemerged. Beschermd pad met mandaat uit de opdracht: `scripts/check-drools.sh` (`EXPECTED_PLAYED`).*

**Wat landde.**

- `9212854ef` (rood) `AbsentCharacteristicNameWitnessTest` (Drools, package `cases`), op een document zonder regels
  met een kenmerk per naam `op zee`, `niet-erkend`, `jutter's`, `EU`, `3e lid`, `m²`, `İş`, elk op beide takken: in een
  strikt document geldt een kenmerk uit `characteristicsAbsent` vast niet en is een ongenoemd kenmerk open; in een gewoon
  document verandert de opgave niets; een naam in beide lijsten is EF-047 met `label` en `characteristic`; en
  `DroolsBackend.insert` met `Input.characteristicAbsent` in de gedeclareerde schrijfwijze en in een andere (spatie
  vooraan, spaties verdubbeld, A-Z omgekeerd) haalt het kenmerk uit wat open is, en weigert met DROOLS-371 en reden
  `holds` als de bean het heeft. Rood op de code van `d655416e0`: alle vier de tests, alle zeven namen, beide takken,
  met `IllegalStateException: geen getter voor de eigenschap 'op zee' op dev.uchar.regelspraak.facts.Kenmerkproef:
  getOp zee noch isOp zee` (enzovoort per naam: `getEU noch isEU`, `get3e lid noch is3e lid`, `getM² noch isM²`,
  `getİş noch isİş`, ...); in een andere schrijfwijze weigerde `insert` met "zijn type Kenmerkproef declareert geen
  kenmerk met die naam".
- `dddc2ed50` het herstel: `CaseInput` leest of de bean het kenmerk heeft via `Namen.property(k)`; `DroolsBackend.openInput`
  brengt `a.characteristic()` eerst terug tot de gedeclareerde naam (`CaseRunner.declaredCharacteristic`, onder
  `Namen.norm`) en geeft die door aan de granulariteit, aan `Accessors.leesEigenschap(…, Namen.property(declared))` en
  aan `ProducerConsumer.elementOf`. EF-047 volgt daaruit: `NameFault.bothStated` wordt nu bereikt. De javadoc van
  `Input.characteristicAbsent` en `Input.Absent` zegt dat de naam in elke schrijfwijze onder `Namen.norm` mag. Groen:
  `Tests run: 4, Failures: 0`.
- `43b52902d` corpus. Keuze: twee nieuwe goldens en een rode casus.
  - `corpus/golden/jutterswet-kenmerknamen-strikt` (`Afleiden uit leeg: nee`): een Vondst met de invoerkenmerken
    `op zee geborgen`, `half-vergaan`, `ex-VOC`, `met foto's vastgelegd` en de regel "afhaalbaar indien hij aan alle
    volgende voorwaarden voldoet" (drie ontkende toetsen en één gewone). Drie casussen met handberekende verwachtingen,
    elk met `characteristicAbsent` op de invoerkenmerken die vast niet gelden: alles gegeven (afhaalbaar geldt), ex-VOC
    open (afhaalbaar open), op zee geborgen en geen foto's (afhaalbaar geldt niet).
  - `corpus/golden/jutterswet-kenmerknamen`: dezelfde regels in een gewoon document; casus 1 noemt de drie ontkende
    kenmerken niet, casus 2 geeft ze in `characteristicsAbsent`; dezelfde verwachtingen. De frontend waarschuwt bij de
    drie regels "- is niet …" dat ze hier niets veranderen (bedoeld; geen gate faalt erop).
  - `corpus/red/jutterswet-kenmerknaam-wel-en-niet`: `op zee geborgen` in beide lijsten, EF-047 (document van de
    strikte golden).
  - Beide goldens hebben `source.rgs` en `scenario.rgs` (gates 10 en 16), DRL-tekstpins
    (`backends/drools/src/test/resources/drl-text/jutterswet-kenmerknamen{,-strikt}.drl`, alleen die twee nieuw) en
    `EXPECTED_PLAYED` 386 → 388 met reden. Geen nieuwe noop-survivor. Rood vóór het herstel (jar van de code van
    `9212854ef`): beide goldens op beide takken `ERROR IllegalStateException: geen getter voor de eigenschap 'op zee
    geborgen' op dev.uchar.regelspraak.facts.Vondst: getOp zee geborgen noch isOp zee geborgen`; erna `PLAYED 3` en
    `PLAYED 2` op beide takken.
- Namen die de RegelSpraak-lexer in een kenmerk niet schrijft (een woord met een beginhoofdletter, een begincijfer,
  `²`, `İ`) staan alleen in de Drools-test. ref en clingo heb ik op hetzelfde testdocument met de hand gemeten: de 28
  casussen (7 namen × vast niet / ongenoemd × strikt / gewoon) PASS in beide, en de 14 rode casussen geven EF-047 in
  beide.

**Gates** (elk apart, logs in `werk/absent-characteristic-property/gate*.log`; 2, 4–7 en 16 opnieuw na de laatste
wijziging van casus 3, 12 ook):

- 1 `check-self-contained: PASS`
- 2 `check-ir-schema: PASS (… 512 file(s) validate and round-trip, floor 192; 65560 object(s) in 1158 document(s) in
  the schema's key order, 2 in no subschema)`
- 3 `check-verifier: PASS (… 512 golden clean; 646 negative rejected for their declared rule, floor 568; …)`
- 4 `check-case-schema: PASS (schema valid; 146 self-test row(s); 1298 case file(s) validate)`
- 5 `check-oracle: PASS (ref suite 4232 worked examples, floor 489; 1197 cases replayed exactly, floor 427;
  preservation …: 281 goldens and 787 cases play; … 6 carrying the property already … jutterswet-kenmerknamen-strikt …;
  monotonicity: 812 cases, 2680 runs without one input compared, floor 2652 …)`
- 6 `check-clingo: PASS (backend suite 4879 checks, floor 4797 …; 1167 cases replay exactly through clingo …)`
- 7 `check-noop: PASS (1152 cases refute a do-nothing executor; 45 pinned survivors pin defeasible standing)`
- 8 `check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; …)`; Drools leg
  `PASS (82 fault-pinned, 82 of them reporting the case's code, 134 pinned parameter checks confirmed, floor 28; …)`
- 10 `check-regelspraak: PASS — JUnit suite green (1716 tests, floor 173); 226 rgs golden(s) re-emit byte-identically`
- 12 `check-drools: PASS` — `played 388`, `DRL-text branch played 388`, `JUnit 2066 tests, floor 1573`
- 13 `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays …)`
- 15 `check-graph: PASS` (route leg 226 goldens, 86 unit tests)
- 16 `check-scenario: PASS — 53 scenario golden(s) (floor 7) regenerate 174 case file(s) (floor 31) and their ir.json
  byte-identically`
- 17 `check-readings: PASS (ref traced 1197 golden case files …; on both branches 389 goldens and 1071 case files
  compared, 8081 readings …; deviations 0, end-state deviations 0 …)`

Niet gedraaid: 9 (catala), 11 (toka), 14 (flogic): hun bronnen veranderden niet; `make gates` doet de hoofdsessie.

**Open punten.**

1. **Een gat in `ReadingsCorpusWitnessTest.stop` (gate 12), buiten deze kaart.** De eerste versie van casus 3 van de
   strikte golden (op zee geborgen, half-vergaan en ex-VOC open, geen foto's) speelt in ref, clingo en beide takken, en
   de lezingen van de backend zijn gelijk aan de trace van ref (drie leden onbepaald, het vierde onwaar, de
   samenstelling onwaar). De getuige faalt toch: `samenstelling 0 bereikte lid 2 na de stop`, omdat `stop()` met
   `ComputeHelpers.established(kw, k, touch, i, m - i)` een onbepaald lid als onwaar telt, waar Kleene doorleest
   (RUNTIME-SEMANTICS ch. 1.5). Ik heb de test niet veranderd (CLAUDE.md: eerst vragen) en casus 3 vervangen door een
   casus die vóór een onbepaald lid stopt (op zee geborgen geldt). Meting, de oorspronkelijke casus en een voorstel van
   drie regels (onbepaald telt als geen van beide; `established(kw, k, touch, touch + fails, m - touch - fails)` is
   `Composite.truth` met `n = m`) staan in `werk/absent-characteristic-property/witness-gap/README.txt`. Na goedkeuring
   kan de oorspronkelijke casus terug in de golden.
2. Toolchain-pin en werkbank-pin: vervolgstap buiten deze repository (kaart).
3. Geen spec-, CONSTRUCTS- of PLAN-wijziging: het gedrag stond al in RUNTIME-SEMANTICS ch. 1.5; geen rij voor de
   hoofdsessie.
