# datumrekening-slaat-stil-om: ref, clingo en de Drools-runtime slaan in een paar hoeken van de datumrekening stil om

*Gevonden op 2026-10-09 door de bouwer van
`20261009-drools-datumstap-buiten-bereik` (bijvangst, gemeten, niet
gerepareerd) en door de hoofdsessie vastgelegd volgens de afspraak dat een
bestaande bug een kaart krijgt. Gemeten op die branch (`9244d361`, op main
`9b9a42ad`), met ref en clingo uit `_build` van die worktree en de shaded jar van
die commit; de meetscripts staan in `/tmp/lens-bugs/w3/meting/`.*

## Aanleiding

`spec/RUNTIME-SEMANTICS.md` ch. 3.8: een datumstap die het representeerbare
bereik verlaat, is een fout, "never a silent wrap-around". Op vier plaatsen
gebeurt dat toch:

1. **Ref** rekent op een OCaml-`int` en slaat in hoeken om die zijn
   grenscontrole niet dekt:
   - 2024-01-01T00:00:00 plus 3 074 457 345 618 258 603 kwartalen geeft
     `@1706745600000ms` (2024-02-01T00:00:00): het aantal maal 3 slaat om;
   - `min −4 611 686 018 427 387 904 millisecondes` geeft
     `@-4611684314360187904ms`: het omkeren van het teken slaat om;
   - op een datum (dagprecisie, waar ref geen bereik heeft): plus
     4 611 686 018 427 387 903 dagen geeft −12626367463881254-09-18, plus
     7·10¹⁷ weken geeft −11836990593520844-11-04;
   - `floor_div` in `split_ms` loopt over voor een datum-tijd binnen ongeveer
     een dag van −2⁶² (onder −4 611 686 018 340 987 904 ms): vanaf
     −146136543-09-09T00:00 min 1 springtijdag geeft ref
     `@4611686017705975808ms` in plaats van de bereikfout (gemeten in de
     fixronde van `20261009-drools-datumstap-buiten-bereik`, `6ac5665d`); het
     raakt elke bewerking die `split_ms` gebruikt.
2. **Clingo** slaat om bij 2024-01-01 plus 2 000 000 000 maanden op een datum:
   epochdag `744227579` (2039595-11-27), waar ref en Drools 166668690-09-01
   geven. En het geeft bij een vooruitstap een misleidende zin, "date arithmetic
   reaches a year before 0000-03" (bij + 999 997 975 jaren op een datum en bij
   + 1 753 661 499 maanden op een datum-tijd).
3. **De Drools-runtime**, `RegelSpraakRuntime.rsEerstePaasdag`, neemt het jaar
   met `intValue()`: paasjaar 4 294 969 320 geeft in de exec-modeltak en de
   DRL-teksttak 2024-03-31; ref geeft 4294969320-04-21, clingo weigert. Gezien,
   niet gemeten: `rsDatumMet` en `rsDatumtijdMet` nemen hun delen ook met
   `intValue()`.

## Een normvraag: het bereik op dagprecisie

Ch. 3.8 geeft een bereik voor een datum-tijd (in milliseconden) en zegt niets
over een datum zonder tijd. Ref rekent daar zonder grens (en slaat om, punt 1);
de Drools-runtime volgt sinds `20261009-drools-datumstap-buiten-bereik` ref
zolang `java.time` de datum kan houden (jaren ± 999 999 999) en geeft daarbuiten
een eigen fout. De bouwer van die kaart raadt aan: hetzelfde interval als voor
een datum-tijd, gelezen op de middernacht van de datum (epochdagen
−53 375 995 583 … 53 375 995 583); dan veranderen ref en Drools (nu geven beide
164276444-06-02 voor 2024-01-01 + 6·10¹⁰ dagen). Het alternatief: op dagprecisie
geen bereik, maar wel "never a silent wrap-around".

**Besluit van de eigenaar (2026-10-09):** het advies van de bouwer. Een datum
op dagprecisie heeft hetzelfde bereik als een datum-tijd, gelezen op zijn
middernacht: epochdagen −53 375 995 583 tot en met 53 375 995 583, de dagen
waarvan de middernacht in [−2⁶², 2⁶² − 1] ms ligt. Een stap die daarbuiten
uitkomt, is een fout, nooit een omslag; een executor die zo ver niet kan
rekenen, weigert luid. Het bereik van een datum-tijd staat sinds de landing van
`20261009-drools-datumstap-buiten-bereik` normatief in ch. 3.8; de zin voor de
datum komt met deze kaart.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rode gevallen in `corpus/red` voor de hoeken van punt 1 en 2 die een
  datum-tijd raken, met `expectedFault {code, params}`; voor wat clingo
  bij de vertaling al weigert, telt die weigering.
- (b) Ref controleert het aantal maal de eenheid en het omkeren van het teken
  vóór de stap; clingo controleert zijn epochdag en noemt bij een vooruitstap
  de goede grens; de Drools-runtime neemt jaar, maand, dag en de delen van
  `rsDatumMet`/`rsDatumtijdMet` exact (geen `intValue()` dat omslaat).
- (c) Het besluit over dagprecisie in ref, clingo en Drools, met rode
  gevallen, en de zin in ch. 3.8 (voorstel).
- (d) Gates 5, 6, 8 en 12 groen; alle gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel: vier kleine plaatsen in drie executors, plus een normvraag.

May touch: `ref/lib/arith.ml`, `backends/clingo/**`,
`backends/drools/src/main/java/dev/uchar/regelspraak/runtime/RegelSpraakRuntime.java`,
getuigen in de drie, `corpus/red/<nieuwe gevallen>/`, `spec/RUNTIME-SEMANTICS.md`
(voorstel, na het besluit), kaartverplaatsing.

## Raakvlakken

Bouwt op `20261009-drools-datumstap-buiten-bereik` (geland 2026-10-09: de
grens van ref in de runtime en het getal in ch. 3.8).

## Result

Gebouwd 2026-11-06 op branch `card/allocation-letbinder-dates`, in één commit. Logs onder
`werk/allocation-letbinder-dates/logs/`. Elk punt eerst opnieuw gemeten op main `17f956d92`
(`k4-red-probe-ref-clingo.log`, `k4-red-probe-drools.log`).

- Nagemeten. Punt 1 (ref): kwartalen, het omkeren van −2⁶² en de dagen en weken op een datum sloegen nog om; de
  datum op dagprecisie had geen bereik (2024-01-01 + 6·10¹⁰ dagen gaf 164276444-06-02); `floor_div` liep over bij
  een datum-tijd binnen een dag van −2⁶². Punt 2 (clingo): + 2 000 000 000 maanden op een datum vond een model met
  dag 744227579 (omslag), + 999 997 975 jaren noemde "a year before 0000-03", + 1 753 661 499 maanden op een
  datum-tijd vond een model met dag 1836388008 (omslag). Punt 3 (Drools): `rsEerstePaasdag` had nog `intValue()`,
  maar de aanroeper `ComputeHelpers.easterSunday` weigerde jaren buiten ±999 999 999 al met een eigen zin; de
  constructies namen hun delen al met `intValueExact()`; een stap op een datum faalde alleen buiten de jaren van
  java.time met een eigen zin zonder code.
- (a) Rode gevallen, alle met `expectedFault {code: EF-007, params: {}}`: `corpus/red/date-time-step-wrap` (kwartalen
  3 074 457 345 618 258 603, min −2⁶² milliseconden, en één dag terug vanaf 36 minuten boven −2⁶²: de
  `floor_div`-hoek), `corpus/red/date-step-outside-range` (dagen één voorbij beide grenzen, 7·10¹⁷ weken,
  2·10⁹ maanden, 999 997 975 jaren) en `corpus/red/date-with-outside-date-range` (datum met jaar 200 000 000 en met
  146140482-04-25, eerste paasdag van 4 294 969 320). Ref van main gaf op alle elf een waarde
  (`k4-red-old-ref-values.log`); nu ref en Drools EF-007 (`k4-red-cases.log`, `k4-red-drools.log`). Clingo weigert
  elk als eigen limiet; gate 8 heeft elf regels in de divergentielijst (CLINGO-001, -029, -034).
- (b) Ref (`ref/lib/arith.ml`): de stap rekent in Zarith (aantal maal eenheid, maandtelling), het omkeren van
  `min_int` is een bereikfout, `floor_div` vormt geen som die overloopt. Clingo (`translate.ml`): het aantal van een
  datumstap is begrensd vóór de vermenigvuldiging (CLINGO-034), en de dag erna (nieuwe limietcode CLINGO-037, "date
  arithmetic reaches a year after 5000000"; ook de bestaande "year before 0000-03" heeft nu die code); die hekken
  dragen de grens van het aantal, zodat een te groot aantal alleen door zijn eigen hek wordt geweigerd. Drools
  (`RegelSpraakRuntime.java`): `rsEerstePaasdag` toetst het jaar vóór `intValueExact()`, `rsDatumMet` en
  `rsDatumtijdMet` toetsen het jaar vóór de kalender, en elke datum op dagprecisie (stap, constructie, paasdag,
  dagsoortstap) landt in het bereik of geeft EF-007 (`DATUM_BUITEN_BEREIK`, catalogusregel `EF-007.date`); de
  eigen zin over de jaren van java.time is weg.
- (c) Het bereik op dagprecisie (besluit 2026-10-09) staat in ch. 3.8 als normatieve alinea met een uitgewerkt
  voorbeeld (de twee grenzen op de dag, de maandstap tot de grens, de constructie en de paasdag), en de zinnen over
  het paasjaar en de datumconstructie noemen EF-007 in plaats van "the executor's machine-integer range". De rij van
  EF-007 in ch. 5.7b noemt nu ook de datum, met de drie nieuwe getuigen. Ref worked examples `WV-3.8-d-bereik-*`
  en `WV-3.8-dt-bereik-{kwartalen,min-min-int,bodem*}`, `WV-3.8-floor-div-bodem`; clingo-checks voor de hekken.
  `ASP-TRANSLATION.md` en de README van clingo beschrijven de hekken.
- Gewijzigde tests omdat het gedrag bewust verandert (besluit van de eigenaar): ref `WV-3.8-paasdag-groot-jaar`
  (paasdag van 10¹⁵ was een datum, is nu EF-007); Drools `DateStepRangeWitnessTest`: de test
  `a_date_without_time_follows_ref_to_the_years_of_java_time` (pinde 164276444-06-02 en 166668690-09-01 als waarde)
  is vervangen door `a_date_without_time_has_the_range_of_its_midnight`, plus
  `a_constructed_date_has_the_range_of_its_midnight`; `FilterFaultWitnessTest` pint nu ook de fout op de stap van
  2·10⁹ jaren en de paasdag van 2·10⁹.
- (d) Gerichte gates groen (`logs/k4/summary.txt`): 1, 2, 3, 4, 5 (2530 worked examples), 6 (2675 checks), 8 (85
  rode gevallen, 20 in de divergentielijst), 12.

Voorgestelde rijtekst voor CONSTRUCTS (datumrekening), door de hoofdsessie te landen: "Een datum op dagprecisie heeft
het bereik van zijn middernacht, epochdagen −53 375 995 583 tot en met 53 375 995 583 (ch. 3.8, besluit
2026-10-09); daarbuiten EF-007 in ref en Drools; clingo houdt de dagnummers tot 5000000-12-31 en weigert daarbuiten
als limiet (CLINGO-001, -029, -034, -037)."

### Landing (hoofdsessie, 2026-11-06)

Geland op main na `make gates` op de bouwerstand (`319628d89`, 17 van 17 groen, 433 s; na de rebase op main alleen
backlog en CONSTRUCTS anders). De voorgestelde CONSTRUCTS-tekst staat in de landingscommit.
