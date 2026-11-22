# clingo-suite-shards: de suite van clingo in delen naast elkaar

*Opgesteld door de hoofdsessie op 2026-11-22, op verzoek van de eigenaar (de volledige gate-set duurde 1229 s; "veel te lang
om iets routinematig te doen").*

## Aanleiding, gemeten

`make gates` duurde 704 s en duurt nu 1229 s, doordat gate 6 (1221 s) het kritieke pad is. Alleen duurt gate 6 233 s: 177 s
suite (één proces, 168 s CPU, 4454 controles, elke controle start één of twee `clingo`-processen) en 47 s replays op acht
processen. De suite is één draad; onder de belasting van de andere gates is zij ongeveer zes keer zo traag. De suite naast de
replays laten lopen scheelt hoogstens 47 s.

## Wat er komt

`test_clingo` draait met `UCHAR_SUITE_SHARD=k/n` de k-de van n delen; gate 6 start n delen naast elkaar (`scripts/lib/parallel.sh`
of `xargs`) en telt. Zonder de variabele draait alles, zoals nu. Geen stille groene gate: een controle die in geen deel of in
twee delen draait, laat de gate falen; de floor van het aantal controles staat op de gemeten stand.

## May touch

`backends/clingo/test/test_clingo.ml`, `scripts/check-clingo.sh` (mandaat van de eigenaar voor deze kaart), `CLAUDE.md` gate 6
alleen als zin verandert (dan als voorstel in het Result).

## Acceptance

- Gate 6 in de volle set ≤ 400 s, en alleen ≤ 120 s; de volle `make gates` terug naar ongeveer 704 s.
- De gate faalt op een weggelaten deel, een dubbel deel en een controle die ontbreekt, elk aangetoond met een mutant.
- De suite geeft dezelfde controles en hetzelfde resultaat zonder de variabele.

## Result

Commits op `card/clingo-helper-scope`: `22895c725` (de suite in eenheden) en `e4c69f034` (`scripts/check-clingo.sh`, beschermd pad,
alleen dat bestand). Ervoor, uit open punt 3 van clingo-helper-scope: `3f724ffa0` (een feitcreatie deponeert onder haar eigen naam).

### Wat er is

- **De suite.** Elke controle staat in een eenheid van werk (`work`), genummerd vanaf 0 in de volgorde waarin het proces haar
  tegenkomt: 272 eenheden, 4797 controles. Met `UCHAR_SUITE_SHARD=k/n` en `UCHAR_SUITE_SHARD_LOG=<bestand>` doet een proces de
  eenheden ≡ k mod n en logt het `RUN <nummer>` per eenheid, en aan het eind `TOTAL` en `CHECKS`. Een controle buiten een eenheid,
  een eenheid in een eenheid, een andere vorm van de variabele en één van de twee variabelen zonder de ander eindigen met een
  melding en status 2 (`8/8`, `01/8`, `-1/8`, `1/8 ` en `1/0` onder meer). Zonder de variabelen is de uitvoer die van vroeger.
- **De gate** start 32 delen en de 1211 replays als taken van één `parallel_run` op 16 processen (één per kern; twee delen per
  proces). `judge_suite` eist: elk deel draaide en eindigde met 0, elk log heeft de vaste vorm, overal dezelfde `TOTAL`, de
  `RUN`-nummers samen precies 0..U−1 en elk eenmaal, en samen minstens `EXPECTED_MIN_SUITE_CHECKS` controles. Zij speelt eerst 12
  eigen rijen. Een fout in de suite en een fout in een replay staan nu in hetzelfde verslag; vroeger stopte de gate bij de suite.
- **Floor**: `EXPECTED_MIN_SUITE_CHECKS` van 83 naar 4797 (gemeten 2026-11-22).
- **Temp-bestanden**: geen met een vaste naam. De drie plaatsen (`write_temp`, de rij `clingo-infer-from-empty`, `Solve.run_clingo`)
  gebruiken `Filename.temp_file` (toevallige naam, exclusief aangemaakt); de rest leest. De delen loggen elk naar een eigen bestand.

### Gemeten

16 kernen (12 snelle), 2026-11-22, geen gate of build van een ander (tenzij vermeld), wandkloktijd.

| Wat | Tijd | Hoe |
|---|---|---|
| suite in één proces | 157 s | `test_clingo.exe` |
| langste deel, alle delen tegelijk, zonder replays | n=4 59 s, 6 72 s, 8 43 s, 12 39 s, 16 42 s, 32 43 s | `tools/measure_shards.py` |
| langste eenheid alleen | 35 s | `tools/check_times.py` |
| replays alleen, 8 processen | 40 s (samen 215 s, waarvan tien replays 198 s; de langste 37 s) | `tools/measure_replays.py` |
| gate 6 alleen, 8 processen, 32 delen (16 delen) | 64 s (63 s) | `tools/measure_gate.py` |
| gate 6 alleen, 16 processen, 32 delen (16 delen) | 47 s (47 s) | `sh scripts/check-clingo.sh` |

Zes van de 272 eenheden dragen vier vijfde van de tijd: de ketens van 50 + 1 + 50 en 100 + 1 + 100 mints (25 en 35 s) en vier
verdelingen in centen (13, 13, 21 en 33 s), elk één controle. Zij zaten in twee blokken (67 en 79 s); daarin is elk frame (19) en
elke controle (5) een eenheid, zonder wijziging aan de controles. De overige 248 blokken zijn elk één eenheid, de langste 4,2 s. In
de set is gate 6 niet gemeten; de suite was daar zes keer zo traag, en 47 s × 6 is 280 s (schatting).

### Acceptance

| Eis | Getuige | Rode stand |
|---|---|---|
| Gate 6 alleen ≤ 120 s | 47 s | op de basis 208 s |
| Gate 6 in de set ≤ 400 s, de set terug naar ±704 s | de landingsrun van de hoofdsessie | — |
| Dezelfde controles zonder de variabele | `tools/check_lines.py`, `tools/compare_lines.py`: de 4797 regels "uitkomst, naam" van `3f724ffa0`, van de nieuwe suite in één proces (zelfde volgorde, stdout gelijk, stderr als verzameling regels gelijk) en van 8 delen samen zijn gelijk, op de naam van een temp-bestand in één controle na; elk deel houdt de volgorde van het geheel | — |
| Een weggelaten deel | eigen rij; in het echt de lus vanaf 1 en tot n−1 | `shard 0/32 of the suite did not run` |
| Een dubbel deel | eigen rij; in het echt elk even deel tweemaal | `136 unit(s) of the suite ran more than once: 0 (0/32, 1/32), …` |
| Een ontbrekend nummer | eigen rij; in het echt een eenheid overgeslagen, en een eenheid zonder `RUN` | `1 unit(s) of the suite ran in no shard: 100` |
| Een afwijkend totaal | eigen rij; in het echt een deel dat een eenheid meer telt | `met different numbers of units: 272 (1/32, …); 273 (0/32)` |
| Een deel dat faalt | eigen rij; in het echt een rode controle, en een crash | `shard 0/32 of the suite ends with status 1: FAIL …`, `shard 8/32 … status 2` |
| Een controle die ontbreekt | eigen rij; in het echt één controle niet geteld | `ran 4796 checks, but EXPECTED_MIN_SUITE_CHECKS pins at least 4797` |

In het echt: `tools/gate_mutants.py`, een kopie van de gate met vier replays, 13 mutanten (ook: een ander n in de delen, één log voor
alle delen, een controle buiten een eenheid, een eenheid in een eenheid), alle 13 rood; zonder mutant groen. Elk van de zeven
regels van `judge_suite` uitgezet maakt een eigen rij rood; zonder de toets op de vorm van het log breekt het script af.

### Voorlopige besluiten

1. Een eenheid is een `let () =`-blok; een blok met rijen van seconden maakt elke rij een eenheid. Code tussen eenheden draait in
   elk deel mee en is goedkoop (een proces zonder eenheden kost 5 ms).
2. Eén proces per kern in plaats van één per twee, omdat gate 6 in de set naar rato van haar processen kerntijd krijgt. Dat kost
   geheugen: clingo houdt 10 GB vast op `jutterswet-opvolgerversies/case3`, 5 GB op een `wachtersketen`. Terug naar acht is één
   cijfer (`parallel_jobs 2`): 64 s alleen.
3. Meer delen (32) dan processen (16): gratis, en twee van de zes lange eenheden vallen zelden in één deel. Bij n = 6 gebeurt dat nu
   (72 s).
4. De aparte melding bij nul controles is opgegaan in de floor.

### Voorstel voor `CLAUDE.md` (niet gewijzigd)

- Gate 6: "It also runs the backend's unit suite (normalisation fault modes no corpus witness reaches) in shards beside the replays,
  and holds that every shard ended green and that the shards together ran every unit of the suite exactly once. Floor:
  `EXPECTED_MIN_SUITE_CHECKS`."
- "How the set runs": "Replays run through `scripts/lib/parallel.sh` (one process per two cores, in gate 6 one per core, with its
  replay flags on JVMs)"; de zin klopt anders voor gate 6 niet meer.

`spec/CONSTRUCTS.md` en `spec/PLAN.md`: geen rij.

### Open punten

1. De kop van `scripts/lib/parallel.sh` (buiten "May touch") noemt voor gate 6 alleen "the clingo runner"; voorstel: "gate 6 the
   clingo runner and the shards of the clingo suite".
2. De ondergrens van het kritieke pad van gate 6 is één clingo-aanroep: 35 s (suite, 100 + 1 + 100 mints) en 37 s (de replay van
   `jutterswet-opvolgerversies/case3`). Onder de belasting van de set is dat ±220 s.

### Gates (los)

- 1 `check-self-contained: PASS`
- 5 `check-oracle: PASS` (4239 voorbeelden, 1211 cases)
- 6 `check-clingo: PASS` (suite 4797 controles, 272 eenheden elk eenmaal in 32 delen; 1181 cases spelen, 10 goldens en 30 cases in de
  weigerlijst; de regel van de lijst 22 eigen rijen, die van de delen 12)
- 7 `check-noop: PASS` (1169 cases)
- 8 `check-exec-codes: PASS` (14 REF- en 51 CLINGO-codes)

Niet gedraaid: de andere gates en `make gates`. De programma's van het corpus zijn na `3f724ffa0` byte-gelijk (1313/0,
`dump_programs.py`).
