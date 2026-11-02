# written-value-at-fixpoint: ref houdt een geschreven waarde vast als de voorwaarde later onwaar wordt

*Vastgelegd door de hoofdsessie op 2026-11-01, bij de landing van `20261008-fout-op-tussenstand`. Fable-advies van die
dag, vraag C: `/Users/yeb/Developer/uchar-dev/werk/fout-op-tussenstand/fable/advies.md`. Gemeten op die branch en op
`7c0220d11`, met dezelfde uitkomst.*

Goal: de uitkomst van ref hangt voor een scalair doel met één schrijver niet meer af van de regelvolgorde. Ch. 5.2 noemt
drie uitzonderingen op de confluentie en dit is er geen; ch. 6.1 legt het vast als residu dat op ch. 7 wachtte.

## Aanleiding

Document (`werk/fout-op-tussenstand/fable/t` en `t-rev`): "Het aandeel moet gesteld worden op 1 indien zijn teller kleiner
is dan 5" en "De teller moet gesteld worden op zijn score"; invoer teller 3, score 10.

| Uitvoerder | lezer eerst | schrijver eerst |
|---|---|---|
| ref | `MISMATCH: V1.aandeel = 1` | `PASS` (leeg) |
| clingo | leeg | leeg |
| Drools, exec-model en DRL-tekst | leeg | leeg |

Dezelfde klasse meten `TmsTimelineWitnessTest` B2 (1 van 2 volgordes fout), `TmsWholePeriodWitnessTest` A (8 van 24 volgordes
goed) en het deel `J1.gelijkpremie = 20` van A3 (3 van 6 volgordes fout). Het deel `J5.ongelijkpremie = 10` van A3 is de kaart
`20261009-ref-gehele-periode-lege-grens`.

## Geen normvraag voor een scalair doel met één schrijver

Ch. 7.8 (een regel die niet beslist, laat de waarde van de feitenbasis staan) en ch. 7.7 beslissen het. Open blijven doelen met
meerdere schrijvers (ASP afgeschermd, v0 agendavolgorde) en kenmerken (ch. 5.3 "only ever set true"; de truth-maintenance-tak trekt
ze al in): meet ze en leg ze aan Fable voor als ze meedoen.

## Besluiten Yeb (2026-11-02)

- **Kenmerk: intrekken.** Een kenmerk geldt aan het fixpunt alleen als een regel die het toekent daar beslist; ch. 5.3 "only ever set
  true" wordt aangepast.
- **Meerdere schrijvers: wat aan het fixpunt beslist.** Geen beslissende vorm: de invoerwaarde; één: diens waarde; twee met
  verschillende waarden: de fout van twee schrijvers (ch. 5.7). In elke regelvolgorde.

## Richting

Dezelfde constructie als `fout-op-tussenstand`: `World.outcome` draagt ook de schrijfactie (attribuut, waarde). Vuurt de
her-evaluatie van de vorm op het onderwerp niet, of leeg op een standing of ontsuikerd doel, en is de staande waarde van deze
vorm, dan gaat het doel terug naar de waarde van de invoerwereld (`world0`), met `changed = true`. `init_written` wordt het
algemene eigenaarschap.

Acceptance:
- (a) Rood eerst: een golden met het document hierboven, beide regelvolgordes, eigen Jutterswet-casus; ref ≡ clingo ≡ beide
  Drools-takken op absolute verwachtingen.
- (b) Elke volgorde van `TmsTimelineWitnessTest` B2 en `TmsWholePeriodWitnessTest` A haalt ref; het kenmerk en meerdere
  schrijvers gemeten, met de keuze in het Result.
- (c) De zin in ch. 6.1 over een geschreven waarde die blijft staan, en die in `spec/PLAN.md`, zeggen de nieuwe stand (voorstel).
- (d) `make gates` groen.

May touch: `ref/lib/world.ml`, `ref/lib/eval.ml`, `ref/test/`, `corpus/golden/<nieuw>/`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
`scripts/check-drools.sh` (pin, voorstel), kaartverplaatsing.

Out of scope: `reading.ml`, `backends/clingo` en `backends/drools`, tenzij de golden daar iets laat zien.

## Result:

Gebouwd op main `e284bab9a` in de worktree `wt/written-value-at-fixpoint`. De kaart blijft in `doing/`; de hoofdsessie landt.
Proefdocumenten, scripts en logs onder `/Users/yeb/Developer/uchar-dev/werk/written-value-at-fixpoint/` (`probe/`, `logs/`).

### Besluiten van Yeb (2026-11-02), meegebouwd

1. **Kenmerk: intrekken.** Een kenmerk geldt aan het fixpunt alleen waar een toekenning die het geeft daar beslist, of waar
   de invoer het zet. Ch. 5.3 "only ever set true" is aangepast (voorstel), ref trekt in, clingo en beide Drools-branches
   gemeten.
2. **Meerdere schrijvers: wat aan het fixpunt beslist.** Geen schrijver beslist: de invoerwaarde (of leeg); één: diens
   waarde (ook een lege, waar de schrijver niet staand is, ch. 1.2g); twee met verschillende waarden: de bestaande fout van
   ch. 5.7. In elke regelvolgorde. Spec (voorstel) ch. 5.7, ref, clingo (dat de invoer op zo'n doel weigerde en haar nu
   draagt), Drools gemeten.

### Commits

- `8e044cc8f` kaart naar doing
- `19981d08c` ref: een schrijfactie staat alleen waar haar vorm aan het fixpunt beslist (eigenaarschap, `release`,
  `release_char`; WV-6.1-written in `ref/test/`, de B2-documenten als fixture)
- `c7f031b4f` clingo: de invoer naast twee schrijvers staat waar geen van hen vuurt (`translate.ml`, README,
  `ASP-TRANSLATION.md` voorstel)
- `5177ac470` goldens: geschreven waarde, kenmerk en twee schrijvers aan het fixpunt; `EXPECTED_PLAYED` 309 → 315 (voorstel)
- `cbc1a7e87` ref: de vormen geven terug op een tussenstand die geen case schrijft (test `WV-6.1-written-between`; commentaar
  bij de strata)
- `14327882e` spec (voorstel): ch. 5.3, 5.7, 6.1, 6.3, 7.1, 7.4
- `310fa3450` ref: een verdeling geeft haar delen terug waar ze niet meer beslist (test `WV-6.1-written-allocation`)
- `0dc3643a6` spec (voorstel): ook een verdeling wordt aan het fixpunt gelezen
- de commit met dit Result

### Wat er in ref veranderde

`World.t` draagt in plaats van `init_written` het algemene eigenaarschap: `owners` (per (label, attribuut) de vorm wier
schrijfactie de staande waarde of tijdlijn is, of die vorm een initialisatie is, en wat het doel had vóór de eerste
schrijfactie van een vorm: `Held_value` of `Held_timeline`) en `holders` (per (label, kenmerk) de toekenningen die het
geven, en of het al gold vóór de eerste). Nieuwe functies: `write_attr_by`, `write_timeline_by`, `release`, `set_char_by`,
`release_char`; `write_attr_init` krijgt `~form`. `release ~form` geeft het doel zijn basis terug als `form` de staande
waarde schreef; anders doet het niets. `init_may_write` leest de vlag `initialisation` en is verder gelijk.

`Eval`: `run_rule` geeft terug (`release`, `release_char`) waar de voorwaarde onwaar is, de initialisatie geblokkeerd is, of
een staand of ontsuikerd doel leeg uitrekent; schrijft met eigenaar. `run_pro_rata` krijgt `~form` en geeft terug op een
leeg resultaat van een staand temporeel doel. `run_defaults_group` geeft terug bij `Undecided`, `Conflict` en
`Char_dec false`. `run_allocation` schrijft delen en rest als eigenaar `<pointer> from <bron>` en geeft terug waar de
voorwaarde onwaar is, `Alloc.Nothing`, of een ontvanger `Excluded` is. Een creatie schrijft zonder eigenaar (`write_attr`).
`reading.ml`: één argument (`~form`) door de nieuwe signatuur van `run_pro_rata`; de lezing verandert niet.

### Goldens (elk in de volgorde van de casus en `-omgekeerd`)

| golden | cases per volgorde | inhoud |
|---|---|---|
| `jutterswet-geschreven-waarde-aan-fixpunt` (a) | 4 | het document van de kaart: voorwaarde vervalt, blijft, invoer staat, lege waarde schrijft niet |
| `jutterswet-kenmerk-aan-fixpunt` | 4 | twee toekenningen van `spoedeisend` en een lezer: vervalt, blijft, andere toekenning beslist, invoer staat |
| `jutterswet-twee-schrijvers-aan-fixpunt` | 6 | geen beslist, invoer staat, één beslist (twee kanten), een vervalt, een beslist met een lege waarde |

Samen 28 cases; ref, clingo en beide Drools-branches `PASS` / `PLAYED` in beide volgordes (`logs/step6-goldens-new.log`;
gate 12 speelt 315 op beide branches).

### Rood eerst (binaries van `e284bab9a`, `logs/step6-goldens-base.log`, `logs/step7-reftest-base.log`)

| golden | ref | clingo | Drools |
|---|---|---|---|
| geschreven-waarde | 2 van 4 (`V1.spoedpremie = 1, expected leeg`; `V3.spoedpremie = 1, expected 7`) | 0 | `PLAYED 4` |
| -omgekeerd | 0 | 0 | `PLAYED 4` |
| kenmerk, beide | 0 | 0 | `PLAYED 4` |
| twee-schrijvers | 3 van 6 (case 1, 2, 5) | 3 van 6 `TRANSLATION FAULT: the case supplies input for computed target "spoedpremie"` | `PLAYED 6` |
| -omgekeerd | 0 | dezelfde 3 | `PLAYED 6` |

De kenmerk-golden is op de basis op geen uitvoerder rood: beide toekenningen lezen hun attributen stratum-strikt, dus ze
beslissen nooit op een tussenstand. Dat is gemeten, niet aangenomen; zie voorlopig besluit 4.

`ref/test` op de basis met de nieuwe rijen (zonder de rijen op `World`-niveau, die de basis niet compileert): 14 keer FAIL,
`WV-6.1-written-a`, `-a-input`, `-leeg` (3), `-group`, `-group-high`, `-two` (3), `-two-input` (3) en
`-timeline jaardeel-first`. De rijen `-allocation` kwamen later; op de basis is de proef `v` rood
(`J1.bergdeel = 25, expected leeg; J2.bergdeel = 75, expected leeg`, `logs/step11-allocation-2.log`).

### Mutaties (niet gecommit; `probe/mutate.py` op een losse worktree, `logs/step9-mutations*.log`, `step11-mutation-m8.log`)

| | recept | rood |
|---|---|---|
| M1 | `release` geeft niets terug | 14 FAIL, onder meer `WV-6.1-written-a`, `-leeg`, `-group`, `-two` |
| M2 | `release_char` trekt niets in | `WV-6.1-written-holders-none-left`, `WV-6.1-written-between` |
| M3 | defaultsgroep `Undecided -> (w, false)` | `WV-6.1-written-between` (`V1.wachtpremie = 1, expected 4`) |
| M4 | tijdlijn niet terug | `WV-6.1-written-timeline jaardeel-first` |
| M5 | lege waarde op staand doel geeft niet terug | `WV-6.1-written-leeg` in 3 volgordes (`V1.wachtpremie = 1, expected 9`) |
| M6 | `if not fire then (w, false)` | 14 FAIL |
| M7 | toekenning met onware voorwaarde trekt niet in | `WV-6.1-written-between` |
| M8 | verdeling met onware voorwaarde geeft niet terug | `WV-6.1-written-allocation vrijgavescore-first case1` |
| C1 | clingo: de regel `val :- inputval, not wfired` weg | twee-schrijvers case 2 en 5 in beide volgordes (`underived (leeg), expected 7`) |

M3 en M7 waren eerst niet rood (de groep en de toekenning staan in een hoger stratum dan wat ze lezen); daarvoor kwam
`WV-6.1-written-between`, een invoerwereld met de toestand die die vormen op een tussenstand zouden achterlaten
(`with_files ~world_state`).

### (b) De getuigen in ref, per volgorde (`logs/step4-witness-1.log`, `step4-witness-2.log`)

Documenten uitgeschreven met de `main` van de testklassen op deze worktree.

| deel | basis | nu |
|---|---|---|
| `TmsTimelineWitnessTest` B2 | 1 van 2 volgordes (`J1.jaardeel = … 306 … 59, expected … 4` met jaardeel eerst) | 2 van 2 |
| `TmsWholePeriodWitnessTest` A | 8 van 24 (beide cases) | 24 van 24 |
| `TmsWholePeriodWitnessTest` A3 | 0 van 6: `J5.ongelijkpremie = 10` overal, `J1.gelijkpremie = 20` en `J5.gelijkpremie = 20` in 3 | 0 van 6: alleen `J5.ongelijkpremie = 10`, in elke volgorde (kaart `20261009-ref-gehele-periode-lege-grens`) |

De andere delen van beide klassen veranderen niet (a-keten 120/120, b, c, d, e, b-stap, c-tellend, de emmer-delen; r2 en r3
blijven de weigeringen die de klasse beschrijft).

### Kenmerk en meerdere schrijvers gemeten (`logs/step2-measure-base.log`, `step5-m2*.log`, `step6-*`)

- Kenmerk (`probe/k`, nu de golden): op de basis en nu `PASS`/`PLAYED` op alle vier in beide volgordes. Ref trekt nu in
  (`set_char_by`/`release_char`), maar geen document laat het zien: de strata (ch. 6.3) en de niveaus van een
  instance-ordered deel (ch. 6.2) zetten elke toekenning boven wat ze leest. `WV-6.1-written-between` houdt het
  mechanisme vast.
- Meerdere schrijvers (`probe/m`, `m2`, nu de golden): basis-ref 3 van 6 fout in de volgorde van de casus, clingo
  weigerde elke case met invoer op het doel, Drools speelde alles. Nu alle vier gelijk.
- Defaultsgroep (`probe/g`): de lezing van de groep is een negatieve kant (`verifier --dependency-edges`), dus de groep
  rekent na de regel; de basis was alleen fout doordat de regel eronder haar schrijfactie hield.
- Het residu van ch. 6.1 "off a cycle" (`probe/h`, Houder met één Ding, a = 5 door een regel): basis-ref `H1.b = 1` en
  `H1.c = 1` met de lezer eerst; nu leeg in beide volgordes op alle vier.
- Verdeling (`probe/v`): basis-ref 25 en 75 met de vrijgavescore eerst, clingo en Drools leeg; nu overal leeg.

### Pins en tellingen

`EXPECTED_PLAYED` van gate 12: 309 → 315 (voorstel; de zes nieuwe goldens). Geen vloer verlaagd, geen bestaande test of
verwachting veranderd. Ref-suite 1862 → 2103 voorbeelden; gate 5 en 6 vervangen 987 → 1015 cases.

### Voorgestelde tekst `spec/CONSTRUCTS.md`

Rij 8, achteraan: "**A write is read at the fixpoint (2026-11-02, card `written-value-at-fixpoint`, RUNTIME-SEMANTICS
ch. 5.3, 5.7, 6.1):** a value, a timeline or a characteristic that a form wrote stands only where that form decides at the
fixpoint; a rule, a defaults group or an allocation that does not decide gives the target back what it held before, and a
characteristic holds while an attribution that grants it decides or where the input sets it. A target of two or more
writers takes the value of the writers that decide at the fixpoint: none, its input; one, its value; two that differ, the
fault of ch. 5.7 (maintainer decisions 2026-11-02). Tracked delta against v0's agenda semantics, which keeps what a rule
wrote, sets characteristics monotonically and lets the last writer of a target win. Goldens
`jutterswet-geschreven-waarde-aan-fixpunt`, `jutterswet-kenmerk-aan-fixpunt`, `jutterswet-twee-schrijvers-aan-fixpunt`,
each in both rule orders."

### Voorgestelde tekst `spec/PLAN.md`

In de alinea "Faults and deposits count at the fixpoint (2026-10-09 …)" vervangt dit de laatste zin ("A write whose
condition later turns false keeps its value in the reference interpreter: …"): "Writes count at the fixpoint as well
(2026-11-02, card `written-value-at-fixpoint`): a value, a timeline or a characteristic stands only where the form that wrote
it decides there, and a target of two or more writers takes the value of those that decide there — none: its input; one:
its value; two that differ: the fault of ch. 5.7 (maintainer decisions of that day on characteristics and on writers). The
residual of ch. 6.1 is gone; in the reference interpreter `TmsTimelineWitnessTest` B2 and `TmsWholePeriodWitnessTest` A
pass in every order, and of A3 only `J5.ongelijkpremie` remains (card `20261009-ref-gehele-periode-lege-grens`). The ASP
backend carries a case input beside two writers; both Drools branches already read so."

### Voorlopige besluiten

1. **De basis is wat het doel had vóór de eerste schrijfactie van een vorm**, niet een opzoeking in `world0`: voor een object
   uit de invoer is dat de invoerwaarde, voor een object dat een creatie maakte de waarde die de creatie het gaf (dat
   object staat niet in `world0`). Neemt een tweede vorm het doel over, dan blijft de basis.
2. **Eigenaarschap gaat over bij elke schrijfactie met eigenaar, ook van een gelijke waarde**, en telt alleen als
   verandering waar de vorige eigenaar een initialisatie was (haar blokkade leest het). Telde elke overdracht, dan zouden
   twee schrijvers met gelijke waarde nooit tot rust komen.
3. **Een creatie schrijft zonder eigenaar**: haar schrijfactie bij het maken is onherroepelijk (ch. 5.5).
4. **Toekenningen en defaultsgroepen geven ook terug**, al laat geen document dat zien (strata); zo leest ref het besluit
   letterlijk en hangt de juistheid niet alleen aan de strata. Getuige op `World`-niveau en met een invoerwereld die geen
   case schrijft (`WV-6.1-written-holders-*`, `WV-6.1-written-between`).
5. **De verdeling is meegenomen** (niet in de kaart): dezelfde klasse, gemeten rood, kleine wijziging. Eigenaar per bron
   (`<pointer> from <label>`), zodat de ene bron niet teruggeeft wat de andere schreef.
6. **Clingo**: naast twee of meer niet-staande co-schrijvers komt de invoer als `inputval/3` binnen en staat ze waar geen
   `wfires` hoort; een schrijver die met een lege waarde vuurt, blokkeert de invoer en schrijft leeg (de bestaande zin van
   ch. 5.7). Een tekst-, enum-, som- of struct-doel met invoer blijft geweigerd zoals het was.
7. **Fixtures**: de B2-documenten en de verdelingsvariant staan onder `ref/test/fixtures/written-timeline/` en
   `written-allocation/`, de B2-documenten letterlijk zoals `TmsTimelineWitnessTest.main` ze schrijft.

### Wat bleef liggen

- `IR-SPEC.md` ch. 5 (de randsoorten, regels rond 9066 en 9073) noemt een kenmerktoekenning nog onherroepelijk en
  "monotone"; RUNTIME-SEMANTICS ch. 6.1 zegt nu dat haar lezingen stratum-strikt blijven. IR-SPEC viel buiten de opdracht.
- De Javadoc-alinea's "Measured" van `TmsTimelineWitnessTest` en `TmsWholePeriodWitnessTest` (2026-10-09) beschrijven de
  volgordeafhankelijkheid van ref die nu weg is; `backends/drools` viel buiten de opdracht.
- A3 `J5.ongelijkpremie = 10`: kaart `20261009-ref-gehele-periode-lege-grens`.
- De residuen op een toelaatbare cyclus (ch. 6.1, "conditions that hold on absence", "a value that grows") zijn niet
  aangeraakt.
- Geen golden voor de verdeling of de tijdlijn; beide zitten in `ref/test/` (fixtures).

### Gates

`make gates` op `0dc3643a6` (de code van dit Result; daarna verandert alleen de kaart), `out/gates/20261102-222933/`:

```
start 2026-11-02 22:29:33 0dc3643a6 card/written-value-at-fixpoint
ocaml rc=0 2s ocaml-build: rebuilt (14 source path(s) newer than the stamp, e.g. ref/lib; 2s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 537 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 136s
04-case-schema rc=0 7s
03-verifier rc=0 41s
05-oracle rc=0 22s
06-clingo rc=0 279s
07-noop rc=0 13s
08-red rc=0 173s
09-catala rc=0 5s
10-regelspraak rc=0 241s
11-toka-parity rc=0 231s
12-drools rc=0 375s
13-reftype rc=0 294s
14-flogic rc=0 2s
15-graph rc=0 50s
16-scenario rc=0 41s
17-readings rc=0 254s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 22:35:50 377s red=0
```

Gate 12: played 315 op beide branches; gate 17: 315 goldens en 885 case files vergeleken, deviations 0. Een eerdere run op
`14327882e` (vóór de verdeling) was ook groen, 322 s, `red=0`.
