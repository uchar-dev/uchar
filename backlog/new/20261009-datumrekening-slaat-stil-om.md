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
  datum-tijd raken, met `expectedFault {ref, clingo, drools}`; voor wat clingo
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
