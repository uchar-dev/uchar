# writers-as-one-defaults-stack: alle schrijvers van een doel als één impliciete defaults-stapel

*Opgesteld door de hoofdsessie op 2026-11-04, op verzoek van de eigenaar. Na de kaart `defaults-per-period-on-timeline`.*

## Aanleiding

De ASP-backend weigert invoer uit de case op een berekend attribuut dat naast een rekenende regel ook een initialisatie, een verdeling of
een toekenning bij het maken van een object schrijft (`TRANSLATION FAULT`, het hek van `spec/ASP-TRANSLATION.md` S2 en S19). In ASP staat
een feit naast alles wat de regels afleiden; een invoer zonder weerlegbare route geeft dus twee waarden tegelijk, en het hek weigert liever
dan een fout antwoord te geven. Voor rekenende regels en defaults-groepen bestaat die route (`inputval/3` met `not wdecides`, de
defaults-keten met `not`); voor de drie andere schrijvers niet. ref en Drools beantwoorden de gevallen wel; geen golden draagt er een, dus
geen gate merkt het (kaart `empty-write-decides-nothing`, Result, VB-3).

RS ch. 7.8 beschrijft de invoer al als default, de virtual input default, tussen de laagste gewone default en de initialisatie.

## Goal

Elke schrijver van een doel staat in één impliciete defaults-stapel, per dag op een tijdlijndoel (kaart `defaults-per-period-on-timeline`):

1. rekenende regels, verdelingen en toekenningen bij het maken, onderling van gelijke rang (verschillen er twee op een dag: EF-016);
2. de invoer uit de case;
3. de initialisatie (§9.6: alleen waar er nog geen waarde is).

Een schrijver die met een lege waarde vuurt, beslist niets (besluit van de eigenaar, 2026-11-04). In clingo wordt de stapel één generieke
codering, de defaults-keten met `not`, en het hek verdwijnt. ref en Drools volgen die rangorde al; meet het en schrijf het als één regel
in RS ch. 7.8 (en ch. 3.11, 5.7 waar ze dezelfde rangorde in eigen woorden zeggen).

## Acceptance

- (a) Rood eerst: invoer op een doel met een rekenende regel en een (voorwaardelijke) initialisatie, een verdeling en een toekenning bij het
  maken, elk met een regel die beslist en een die leeg oplevert, scalair en, waar de vorm een tijdlijn toelaat, per dag: clingo weigert op
  main, en speelt daarna met de uitkomsten van ref en Drools. Goldens voor de vormen die het corpus nog niet draagt.
- (b) Elke bestaande verwachting blijft; de kanaal- en divergentielijsten van gate 6, 8 en 12 krimpen alleen waar het hek een regel had.
- (c) RS ch. 7.8 als huidige stand (voorstel), met de stapel als één regel en een uitgewerkt voorbeeld; ASP-TRANSLATION S2, S3, S10, S19
  zonder het hek; CONSTRUCTS rij 8.
- (d) `make gates` groen.

May touch: `backends/clingo/`, `ref/` en `backends/drools/**` alleen waar de meting een verschil toont, `spec/RUNTIME-SEMANTICS.md` en
`spec/ASP-TRANSLATION.md` (voorstel), `spec/verifier/` (het staande domein, voorstel), `corpus/golden/`, `scripts/` (alleen lijsten die
krimpen; voorstel), kaartverplaatsing.

Meenemen (gemeten in `flavours-of-empty`): clingo weigert een door de case gegeven enumeratiewaarde op een berekend doel zonder
registercode ("a case-supplied text/enum computed target … has no encoding here"); onder de stapel krijgt die invoer dezelfde route.

## Omvang

Klein tot middel.

## Result

*Bouwer, 2026-11-04. De kaart blijft in `doing/`; niet gemerged, niet gepusht. Logs onder
`werk/writers-as-one-defaults-stack/logs/` (buiten de repo), scripts onder `werk/writers-as-one-defaults-stack/scripts/`
(`probe.sh`, `scan_writers.py`, `write_golden_cases.py`, `write_fixture_cases.py`, `mutations.py`), proeven onder
`…/probes/`.*

### Commits

| hash | inhoud |
|---|---|
| `57cb00cd3` | kaart naar `doing/` |
| `db841a3d0` | ref: weigering bij naam van een initialisatie, verdeling en lidwaarde van een creatie op een tijdlijndoel; fixtures `ref/test/fixtures/writers-stack/` (zes documenten); ref-suite `WV-7.8-stapel*` |
| `fb56b6867` | clingo: de stapel als één codering; clingo-suite; `backends/clingo/README.md`; `spec/verifier/lib/desugar.ml` (beschermd, voorstel: `standing_targets` weg) |
| `3ef7deb8d` | Drools: een rekenende regel en een initialisatie naast een verdeling; DROOLS-142 → `RETIRED`; `TmsAllocationWitnessTest` omgedraaid |
| `cb9086496` | vier goldens; NOTE van `jutterswet-veilinginvoer`; `scripts/check-drools.sh` (beschermd, voorstel) `EXPECTED_PLAYED` 334 → 338 |
| `09657ae64` | RUNTIME-SEMANTICS ch. 3.11, 5.5, 5.7, 7.8 en ASP-TRANSLATION S1–S3, S10–S12, S16, S18, S19 (voorstel) |
| `3ecb3a9cf` | RUNTIME-SEMANTICS ch. 3.12 rij 10 (voorstel) |
| `44c1f24fa` | fixture `creation-conditional-initialisation` in beide suites en in RS ch. 7.8 (een mutatie bleef groen zonder) |
| `d67095174` | Drools: de verschilregel voor regel naast verdeling weer weg (geen getuige; het terugschrijven geeft de fout al); de golden met de verdeling krijgt een berging zonder ontvangers (een mutatie bleef groen zonder) |
| `821da699d` | Drools-test: de javadoc van `TmsCreationInputWitnessTest` noemt de huidige meting van clingo (99 en 7, waar main 106 en 12 geeft; proef `probes/tms-creation-*`) |
| (deze) | Result |

### De stapel

Eén regel, RS ch. 7.8 "The stack of a target": elke schrijver van een attribuutdoel staat in één impliciete stapel,
hoogste eerst: (1) de schrijvers, rekenende regels en verdelingen, van één rang (een schrijver beslist waar zijn waarde
gevuld is; twee met verschillende waarden: EF-016); (2) de invoer van het object, de case of, op een gemaakt object, de
waarde van het maken; (3) de initialisatie. Per dag op een tijdlijndoel voor schrijvers en invoer; een initialisatie
buiten een defaults-groep, een verdeling en een lidwaarde van een creatie op een tijdlijndoel weigert elke uitvoerder bij
naam (RS ch. 3.12 rij 10, open). Rang (2) wijkt af van de lijst van de kaart: zie VB-1 en vraag 1.

### Meting van ref en Drools tegen de rangorde (code van main, logs `01`, `11`)

| document | ref | clingo | Drools exec / DRL |
|---|---|---|---|
| twee regels + voorwaardelijke initialisatie, invoer (`jutterswet-invoer-naast-initialisatie` case1) | PASS | `TRANSLATION FAULT: the case supplies input for computed target "taxatie", which an initialisation, an allocation or an object creation writes as well …` | PLAYED / PLAYED |
| idem zonder invoer (case2) | PASS | `TRANSLATION FAULT: the initialisation of "taxatie" sits beside 2 computing rules …` | PLAYED / PLAYED |
| verdeling + regel, invoer op doel en rest (`-naast-verdeling` case1) | PASS | het hek | `EXCLUDE DROOLS-142` / idem |
| idem zonder invoer (case2) | PASS | PASS | `EXCLUDE DROOLS-142` / idem |
| verdeling + regel met 30 tegen 50 (`allocation-conflict`) | EF-016 | het hek | `EXCLUDE DROOLS-142` |
| creatie + regel, invoer (`-naast-objectcreatie`) | PASS (J2 **99**: de regel verdringt de waarde van het maken) | het hek | PLAYED / PLAYED |
| enumeratie en tekst, invoer (`-op-keurklasse`) | PASS | `TRANSLATION FAULT: a case-supplied text/enum computed target "staat" has no encoding here` | PLAYED / PLAYED |
| creatie + initialisatie (`creation-initialisation`) | PASS | **`MISMATCH: J1.terug = [12], expected 7`** (twee waarden, stil) | PLAYED / PLAYED |
| creatie + voorwaardelijke initialisatie (`creation-conditional-initialisation`) | PASS | `TRANSLATION FAULT: the initialisation of "waarde" shares its target with an allocation, an object creation …` | PLAYED / PLAYED |
| verdeling + voorwaardelijke initialisatie, invoer (`allocation-initialisation`) | PASS | idem | `EXCLUDE DROOLS-142` / idem |
| invoer op de rest van een verdeling (proef `allocation/rest-input`) | PASS | **`MISMATCH: G1.onverdeelde rest = [9;1]`** (stil) | — |
| initialisatie op een tijdlijndoel (`timeline-initialisation`) | PASS, maar de 5 komt in geen tijdlijn (stil) | `temporal attribute "strandloon" as a target …` | `EXCLUDE DROOLS-103` |
| verdeling, lidwaarde van een creatie op een tijdlijndoel | PASS, scalair in een tijdlijnattribuut (stil) | PASS, idem (stil) | `EXCLUDE DROOLS-271` |

ref en Drools volgen de rangorde, met één afwijking van de lijst van de kaart: beide lezen de lidwaarde van een creatie
als de invoer van het gemaakte object (rang 2), niet als schrijver (rang 1); met een regel die anders beslist geven ze de
waarde van de regel, geen EF-016 (`TmsCreationInputWitnessTest`, sinds tms-expressievormen-en-rekendatum). Drools
weigerde een regel of initialisatie naast een verdeling (DROOLS-142); ref schreef op een tijdlijndoel stil een scalair.

### Wat per uitvoerder veranderde

- **clingo** (`translate.ml`): elke schrijver van een scalair doel leidt `wval/4` af onder een eigen schrijver (een regel
  onder zijn index, een verdeling als `vdN` voor aandeel en rest), met het lichaam van zijn `val`-kop, en `wdecides/2`;
  de invoer staat met `val :- inputval, base, not wdecides` (`defeasible_input`, `input_on`); de initialisatie heeft
  `not inputfilled, not wdecides`; `writer_conflicts` geeft EF-016 ook voor regel tegen verdeling. De lidwaarde van een
  creatie is `inputval/3` (en `inputfilled/2`) op het gemaakte object waar een schrijver, een defaults-groep of een
  initialisatie het lid schrijft. Weg: het hek (geen registercode), de hekken van een berekend tekst/enum-, som-,
  struct- en datum-tijd-doel (geen code), de weigering van een initialisatie naast twee rekenende regels en naast een
  verdeling of creatie (geen code), beide hekken van de verdeling (gelezen elders, tweede schrijver; geen code),
  `comp_decides/2`, `vdN_eff/1`. Erbij: een verdeling of lidwaarde van een creatie op een tijdlijndoel weigert in ref's
  woorden. Opgelost als bijvangst: de twee stille verkeerde antwoorden van de tabel (creatie + initialisatie 12, rest
  [9;1]) en de twee waarden die `TmsCreationInputWitnessTest` voor clingo noemde (gemeten op dezelfde vormen in
  RegelSpraak: main 106 naast de staande regel en 12 naast de initialisatie, de branch 99 en 7, als ref).
- **ref** (`eval.ml`): de rangorde ongewijzigd; een initialisatie, een verdeling en een lidwaarde van een creatie op een
  tijdlijndoel weigeren bij naam (`temporal attribute %S as a target: temporal writes are not implemented`).
- **Drools** (`TmsTranspiler.allocationWriters`): een rekenende regel en een initialisatie naast een verdeling worden
  gedragen; een verdeling schrijft haar aandelen als `Value` met haar naam, dus het terugschrijven geeft EF-016 waar ze
  van een regel verschilt, en de wacht van een initialisatie ziet haar aandeel. DROOLS-142 → `RETIRED`. Geen nieuwe
  code; beide takken renderen dezelfde analyse.
- **verifier** (beschermd, voorstel): `Desugar.standing_targets` weg, geen lezer meer.

### Goldens en fixtures

Goldens (alle in ref, clingo en beide Drools-takken; geen overlever van gate 7): `jutterswet-invoer-naast-initialisatie`
(2 cases), `jutterswet-invoer-naast-verdeling` (2), `jutterswet-invoer-naast-objectcreatie` (1),
`jutterswet-invoer-op-keurklasse` (1). Fixtures `ref/test/fixtures/writers-stack/`: `allocation-initialisation`,
`allocation-conflict`, `creation-initialisation`, `creation-conditional-initialisation`, `timeline-initialisation`,
`timeline-allocation`, `timeline-creation`. Een tijdlijnarm per dag voor de nieuwe vormen bestaat niet (geen van de drie
heeft een lezing per dag); de bestaande per-dag-getuigen van regels en defaults (`writers-per-day`, `defaults-per-day`,
`jutterswet-borgdeel`, `jutterswet-opgave-per-periode`) zijn ongewijzigd groen.

### Rood eerst (letterlijk)

- Goldens en fixtures op de code van main: de tabel hierboven (logs `01`, `11`).
- De nieuwe ref-suite tegen de bibliotheek van main (log `14`): `FAIL WV-7.8-stapel-tijdlijn-timeline-initialisation`,
  `…-timeline-allocation`, `…-timeline-creation`, `3 failure(s)`.
- De nieuwe clingo-suite tegen de bibliotheek van main (log `15`): `test_clingo: 2605 checks, 17 FAILURES`, o.a.
  `initialisation beside a computation, input supplied: the input stands: clingo pins the values (fault: the case
  supplies input for computed target "bergloon", …)`, `an initialisation beside two computing rules: two values:
  expected a fault at solving, got one in translation (the initialisation of "bergloon" sits beside 2 computing rules
  …)`, `the stack of a target: creation-initialisation: clingo pins the values (J1.terug = [12], expected 7)`, `… on a
  timeline target, timeline-allocation: expected a loud fault, got a model`, `… case input on a computed sum target:
  clingo pins the values (fault: a case-supplied sum-typed computed target "herbeschikking" has no encoding here …)`,
  idem struct en datum-tijd.
- `TmsAllocationWitnessTest` tegen main (log `20`): `Tests run: 13, Failures: 1`,
  `h_a_second_writer_of_a_target_stands_in_its_stack … expected: <{regel=WritersConflict: … schrijft 5 en de regel
  "verdeel de opbrengst naar rato" schrijft 100 …, initialisatie=gespeeld, …}> but was: <{regel=Excluded: … DROOLS-142
  …}>`.

### Groen

ref-suite 2409 worked examples (was 2391); clingo-suite 2607 checks, 0 failures (was 2578); Drools-suite 1847 tests, 0
failures, 0 errors, 0 skipped (log `16`, en in gate 12).

### Mutaties (niet gecommit; `git worktree add --detach`, opgeruimd; logs `17-mutations/`, `19-mutations/`)

| mutatie | rood |
|---|---|
| C1 invoer staat zonder `not wdecides` | 8 frames (o.a. `… input supplied: the computed value displaces it`, de drie `empty-write`-documenten, som/struct/datum-tijd) en alle vier goldens (`V1.taxatie = [40;50]`, `J2.terug = [106]`, `V1.staat = ["gaaf";"beschadigd"]`) |
| C2 initialisatie zonder `not wdecides` | 6 frames (`initialisation beside a computation: the computed value wins: … [2500;3000]`) |
| C3 lidwaarde van een creatie als `val/3` | `creation-initialisation` (`J1.terug = [12]`), golden `-naast-objectcreatie` |
| C4 creatie zonder `inputfilled/2` | eerst groen; na de fixture `creation-conditional-initialisation`: `J1.terug = [12], expected 7` |
| C5 aandeel zonder `wval/4` | `allocation-initialisation` (`B1 = [50;5]`), golden `-naast-verdeling` |
| C6 geen conflict regel tegen verdeling | `… a computing rule and an allocation with different values: expected a fault at solving, got a model` |
| C7 geen doorgang voor de rest | eerst groen; na G2 in de golden: `G2.onverdeelde rest is underived (leeg), expected 9` |
| C8 tekst/enum-invoer als `val/3` | golden `-op-keurklasse` (`V1.staat = ["gaaf";"beschadigd"]`) |
| C9, C10 verdeling, creatie op een tijdlijn niet geweigerd | `… on a timeline target, timeline-allocation` / `-creation: expected a loud fault, got a model` |
| R1–R3 ref: initialisatie, verdeling, creatie op een tijdlijn niet geweigerd | `FAIL WV-7.8-stapel-tijdlijn-…` (elk één) |
| D1 Drools: geen verschilregel voor regel naast verdeling | groen: het terugschrijven geeft de fout al; de regel is weer weg (`d67095174`) |
| D2 Drools: de weigering terug | `TmsAllocationWitnessTest` `Failures: 1`, golden `-naast-verdeling` `EXCLUDE` |

### Tests die de kaart omdraait (oud → nieuw)

1. clingo-suite `initialisation beside a computation, input supplied` (`faults_with`, het hek) → drie `both_answer`-frames:
   de invoer staat (regel vuurt niet; regel vuurt leeg), de berekende waarde verdringt haar.
2. clingo-suite `an initialisation beside two computing rules` (`faults_with`, "sits beside 2 computing rules") → vier
   `both_answer`-frames (leeg; één beslist; beide leeg, de invoer; beide leeg, de initialisatie) en één
   `faults_at_solving` EF-016.
3. clingo-suite `an initialisation beside a defaults group of its own level`: zelfde bewering, naald nu "shares its target
   with a defaults group of its own level" (het bericht noemt verdeling en creatie niet meer).
4. Drools `TmsAllocationWitnessTest.h_a_second_writer_of_a_target_is_refused_loudly` (regel en initialisatie:
   `Excluded … DROOLS-142`) → `h_a_second_writer_of_a_target_stands_in_its_stack` (regel: `WritersConflict … schrijft 5
   en … schrijft 100`, initialisatie: `gespeeld`; twee verdelingen ongewijzigd WF-026).

### Codes en lijsten (oud → nieuw)

- DROOLS-142 `REFUSAL` → `RETIRED` (nummer blijft, catalogusregels blijven, zoals DROOLS-222); `check-drools-codes`: 21
  → 22 retired, PASS.
- clingo: de weggehaalde fouten hadden geen registercode; geen code naar een andere soort, geen nieuwe code
  (`codes.tsv` kent geen soort `RETIRED`: `check-exec-codes.sh` laat alleen refusal, limit en fallback toe). ref: geen code.
- Gate 12 `EXPECTED_PLAYED` 334 → 338 (beschermd, voorstel, met regel in de commentaargeschiedenis); `EXPECTED_CHANNEL`
  ongewijzigd. Gate 6 `EXPECTED_CHANNEL` ongewijzigd (2). Gate 8 divergentie- en uitsluitingslijst ongewijzigd. Gate 7
  overleverslijst ongewijzigd. Het hek had in geen lijst een regel (geen golden droeg het), dus er krimpt niets (b).

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS** en **ASP-TRANSLATION**: in `09657ae64`, `3ecb3a9cf` en `44c1f24fa` (RS ch. 7.8 "The stack of a
target" met `WV-7.8-stapel`, de standing norm als rang 1; ch. 3.11, 5.5, 5.7 verwijzen; ch. 3.12 rij 10; ASP S1–S3,
S10–S12, S16, S18, S19 zonder het hek). **clingo-README**: in `fb56b6867`.

**CONSTRUCTS rij 8**, achteraan:

> **The stack of a target (2026-11-04, card `writers-as-one-defaults-stack`, RUNTIME-SEMANTICS ch. 7.8):** every writer
> of an attribute target stands in one implicit stack of defaults: the computing rules and the allocation that write
> it, of one rank (two with different values on one object are the fault of ch. 5.7); then the input of the object,
> the case's or, on an object a creation made, the value of its making; then the initialisation. On a timeline target
> the writers and the input are read per day; an initialisation outside a defaults group, an allocation and a
> creation's member value on a timeline target are refused by name in every executor (ch. 3.12 row 10). In ref, clingo
> and both Drools branches (goldens `jutterswet-invoer-naast-initialisatie`, `jutterswet-invoer-naast-verdeling`,
> `jutterswet-invoer-naast-objectcreatie`, `jutterswet-invoer-op-keurklasse`; documents
> `ref/test/fixtures/writers-stack`). The ASP backend encodes the stack as one chain with negation as failure and
> carries case input beside an initialisation, an allocation, a creation and on every scalar datatype; the Drools
> backend carries a computing rule and an initialisation beside an allocation. Delta against v0, which lets the last
> writer of a target win.

**CONSTRUCTS rij 11** (landingen), achteraan: "2026-11-04, card `writers-as-one-defaults-stack`: an initialisation, an
allocation and a creation's member value on a timeline target are refused by name in ref and clingo (ref wrote a scalar
there that no reader of the timeline saw)."

**CLAUDE.md gate 12**, in de opsomming van de `RETIRED`-codes na "… DROOLS-302, DROOLS-104 and DROOLS-311": "…, since the
card `writers-as-one-defaults-stack` (2026-11-04) DROOLS-142". Verder geen wijziging (geen gate-tekst noemt het hek; de
vloeren blijven ondergrenzen).

### Voorlopige besluiten

- VB-1: rang 2 van de stapel is "de invoer van het object": de case of, op een gemaakt object, de waarde van het maken.
  De kaart zet "toekenningen bij het maken" op rang 1. Gemeten lezen ref en Drools de lidwaarde als invoer van het
  gemaakte object (een regel die anders beslist, wint zonder fout; Drools-javadoc en -test sinds
  tms-expressievormen-en-rekendatum), RS ch. 5.5 noemt ze "member initialisations", en rang 1 zou die bestaande test
  omdraaien; daarom geen wijziging in ref en Drools, en clingo volgt hen. Vraag 1.
- VB-2: één codering voor elk scalair datatype: ook een berekend som-, struct- en datum-tijd-doel neemt invoer in de
  stapel (de hekken waren de zusters van het tekst/enum-hek). Getuigen in de clingo-suite tegen ref op de bestaande
  goldens `anchor/jutterswet-beschikkingsom`, `jutterswet-vangstwaardering` en `jutterswet-getijdenpaar`; geen golden,
  Drools niet gemeten voor die drie.
- VB-3: de initialisatie naast twee rekenende regels en naast een verdeling of creatie valt onder dezelfde codering
  (`not wdecides`, `inputfilled/2`) en weigert niet meer; alleen naast een defaults-groep van het eigen niveau (WF-050).
- VB-4: de verdeling verliest ook het hek "gelezen elders": de negatie is gestratificeerd zoals die van een rekenende
  regel (een lezer van het doel die pot, gewicht, criterium of voorwaarde voedt, is een cyclus die WF-045 weigert). De
  rest is een schrijver als het aandeel.
- VB-5: geen eigen verschilregel in Drools voor regel naast verdeling (D1: geen getuige; het terugschrijven geeft EF-016).
- VB-6: een initialisatie, verdeling of lidwaarde van een creatie op een tijdlijndoel weigert bij naam in ref en clingo
  (Drools weigerde al), geen lezing per dag; RS ch. 3.12 rij 10 open.
- VB-7: namen `jutterswet-invoer-naast-*`, `jutterswet-invoer-op-keurklasse`, fixtures Engels.

### Vragen aan de hoofdsessie

1. VB-1: de lidwaarde van een creatie op rang 2 (invoer van het gemaakte object, zoals ref en Drools al lezen) of op rang 1
   naast de regels (de lijst van de kaart: dan wordt "regel 99 tegen creatie 7" EF-016 in alle drie, en draait
   `TmsCreationInputWitnessTest.a_standing_rule_displaces_the_value_of_the_creation` om)?
2. Drools noemt in het EF-016-bericht de verdeling "de regel \"verdeel …\"", ref "allocation \"verdeel …\""; de parameter
   `rules` verschilt dus (geen rode case pint het). Gelijktrekken, en zo ja in welke woorden?
3. RS ch. 3.12 rij 10 (de drie vormen op een tijdlijndoel): een WF-regel in de verifier in plaats van een weigering per
   uitvoerder, of een lezing per dag?

### make gates

Eén run aan het eind in de worktree op `821da699d` (`out/gates/20261104-211827/`, log `gates-2.log`); de commit daarna
raakt alleen deze kaart. Een run ervoor op `d67095174` (`gates-1.log`) had alle zeventien gates groen en `after-run`
rood, omdat ik tijdens die run een testbestand van Drools aanraakte; daarom de tweede run.

```
start 2026-11-04 21:18:27 821da699d card/writers-as-one-defaults-stack
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 395 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 5s drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. backends/drools/src/test/java/dev/uchar/regelspraak/cases/TmsCreationInputWitnessTest.java; 5s)
01-self-contained rc=0 2s
02-ir-schema rc=0 132s
04-case-schema rc=0 7s
03-verifier rc=0 39s
05-oracle rc=0 23s
06-clingo rc=0 283s
07-noop rc=0 14s
08-red rc=0 172s
09-catala rc=0 5s
10-regelspraak rc=0 255s
11-toka-parity rc=0 238s
12-drools rc=0 356s
13-reftype rc=0 331s
14-flogic rc=0 2s
15-graph rc=0 49s
16-scenario rc=0 40s
17-readings rc=0 268s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 21:24:28 361s red=0
```

Gate 5: ref-suite 2409, 1078 cases (was 1072). Gate 6: clingo-suite 2607, 1074 cases (was 1068), 2 goldens in het
weigeringskanaal. Gate 7: 41 overlevers. Gate 8: 71 rode cases, 37 foutsoorten. Gate 12: JUnit 1847 tests, `played
338`, DRL-tekst `played 338`. Gate 17: 339 goldens, 949 case files, 7453 lezingen, 24211 knoopregels, deviations 0.
