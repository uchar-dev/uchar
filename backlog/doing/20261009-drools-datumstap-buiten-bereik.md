# drools-datumstap-buiten-bereik: de Drools-runtime geeft een datum waar een datumstap het bereik verlaat

*Gevonden op 2026-10-08 tijdens de kaart
`20261008-tms-aggregaties-creatie-en-fouten` (waarneming 3 van haar
`Result:`) en door de hoofdsessie vastgelegd volgens de afspraak dat een
bestaande bug een kaart krijgt. Gemeten op uchar main `54ba99f7` (2026-10-09),
met ref, clingo en de verifier uit `_build` van de hoofdcheckout, de shaded jar
van `backends/drools` uit een bevroren kopie van die commit, en de
RegelSpraak-frontend (`Resolver --ir`) voor het document. De proefbestanden
stonden onder `/tmp/w3-ioiB/`; het document staat hieronder.*

## Aanleiding

`spec/RUNTIME-SEMANTICS.md` ch. 3.8 (rond regel 2245–2278): "A step whose
result leaves the representable millisecond range is a fault, never a silent
wrap-around", met als uitgewerkt voorbeeld "2024-01-01T00:00:00 ± 2 000 000 000 000
uur = **fault**, and + 60 000 000 000 dagen = **fault**". Ref rekent de stap op
de waarde in milliseconden als OCaml-`int` (63 bits) en controleert de grenzen
vóór het product (`ref/lib/arith.ml`, `ms_range_fault` en `add_units_ms` rond
regel 614–631); een aantal buiten dat bereik is ook een fout (`whole_count_of`,
regel ~653–657). Clingo weigert zo'n getal al bij de vertaling, omdat het buiten
zijn gehele getallen valt.

De Drools-runtime, gedeeld door alle drie de takken, neemt in
`RegelSpraakRuntime.rsDatumReken`
(`backends/drools/src/main/java/dev/uchar/regelspraak/runtime/RegelSpraakRuntime.java`
regel 462) `aantal.longValue()`, dat boven het bereik van `long` stil omslaat,
en stapt met `java.time`, dat jaren tot ± 999 999 999 toelaat. Zo komt er een
datum uit waar de spec een fout vraagt. `rsDatumPlusDagsoort` (regel ~490)
neemt het aantal op dezelfde manier.

Document (RegelSpraak, door de frontend naar IR gebracht en verifier-schoon):

    Objecttype de Getijpost (mv: Getijposten) (bezield)
        het meldtijdstip Datum en tijd in millisecondes;
        de wachttijd Numeriek (geheel getal) met eenheid uur;
        de wachtdagen Numeriek (geheel getal) met eenheid dagen;
        het sluittijdstip Datum en tijd in millisecondes;
        het verre tijdstip Datum en tijd in millisecondes;

    Regel sluittijdstip van een getijpost
        geldig altijd
            Het sluittijdstip van een Getijpost moet berekend worden als zijn
            meldtijdstip plus zijn wachttijd.

    Regel verre tijdstip van een getijpost
        geldig altijd
            Het verre tijdstip van een Getijpost moet berekend worden als zijn
            meldtijdstip plus zijn wachtdagen.

Case: G1 met meldtijdstip 2024-01-01T00:00:00 en wisselende wachttijd en
wachtdagen.

## Gemeten (2026-10-09, main `54ba99f7`)

| Invoer | ref | clingo | Drools, alle drie de takken |
|---|---|---|---|
| wachttijd 26 (controle) | 2024-01-02T02:00 | idem | 2024-01-02T02:00 |
| wachttijd 2 000 000 000 000 | `EVALUATION FAULT: datum-tijd arithmetic leaves the representable millisecond range …` | `TRANSLATION FAULT: value 2000000000000 at 0 decimals exceeds clingo's integer range …` | sluittijdstip `+228160941-04-01T08:00` |
| wachttijd −2 000 000 000 000 | idem | idem | sluittijdstip `-228156894-10-02T16:00` |
| wachtdagen 60 000 000 000 | idem | idem | verre tijdstip `+164276444-06-02T00:00` |
| wachtdagen 18 446 744 073 709 551 617 (2⁶⁴ + 1) | `EVALUATION FAULT: duration count 18446744073709551617 is out of range` | idem | verre tijdstip `2024-01-02T00:00` (het aantal sloeg om naar 1) |

Geen golden of rood geval heeft een datumstap buiten het bereik; daarom zien
de gates het niet. Gezien, niet gemeten: een duur die uit een niet-afbrekende
deling komt, zou in `rsDatumReken` een `ClassCastException` geven.

## Een gat in de spec

"The representable millisecond range" heeft in de spec geen getal. Ref haalt
het uit de 63 bits van OCaml (± 4 611 686 018 427 387 903 ms, ongeveer
± 146 miljoen jaar rond 1970), een eigenschap van één executor. De voorbeelden
van ch. 3.8 (ongeveer 228 en 164 miljoen jaar) liggen buiten dat bereik en
binnen dat van `java.time`, zodat ze de fout van deze kaart wel vastleggen,
maar de grens zelf niet. Wil de
Drools-runtime dezelfde grens als ref, dan moet de bouwer die van ref overnemen,
en hoort er een getal in de spec (beschermd pad: voorstel in het `Result:`).

## Acceptatie (bij oppakken aanscherpen)

- (a) Rode gevallen in `corpus/rood` voor de voorbeelden van ch. 3.8 (+ en −
  2 000 000 000 000 uur, + 60 000 000 000 dagen) met
  `expectedFault {ref, clingo, drools}`, en een getuige onder
  `backends/drools/src/test/java` voor het omslaan van het aantal (clingo
  weigert die invoer al bij de vertaling, dus een rood geval is daar ook goed
  als gate 8 dat toelaat).
- (b) `rsDatumReken` geeft een fout met een eigen zin in plaats van een verre of
  omgeslagen datum, rood eerst, met dezelfde grens als ref; de drie takken
  delen de runtime, dus de fout geldt in alle drie.
- (c) Nagaan, en zo nodig in dezelfde fix meenemen: de dagprecisie (een Datum
  zonder tijd), `rsDatumPlusDagsoort`, het aftrekken, en de `ClassCastException`
  bij een duur uit een niet-afbrekende deling (eerst meten).
- (d) Gates 5, 6, 8 en 12 groen, en alle gates van `CLAUDE.md`; het `Result:`
  geeft het voorstel voor een getal in ch. 3.8.

## Omvang

Klein. Eén functie in de runtime, misschien twee, en de rode gevallen.

May touch:
`backends/drools/src/main/java/dev/uchar/regelspraak/runtime/RegelSpraakRuntime.java`,
getuigen onder `backends/drools/src/test/java`, `corpus/rood/<nieuwe gevallen>/`,
`scripts/check-rood.sh` alleen als een lijst daar het vraagt (beschermd pad:
voorstel), kaartverplaatsing. Niet in deze kaart: ref en clingo (geven al een
fout), de spec (alleen een voorstel), de transpilers.

## Raakvlakken

De runtime blijft na `20261008-tms-omschakeling` en
`20261008-drl-teksttak-op-truth-maintenance` dezelfde; deze fix vervalt dus
niet. Serieel met andere kaarten in `backends/drools`;
`20261009-drools-filterfout-buiten-de-lezende-regel` raakt andere bestanden,
maar dezelfde gates.

## Result (2026-10-09)

*Geschreven door de hoofdsessie. De eigenaar was offline; elke kaart van deze ronde stopt vóór de merge.*

| Commit | Wat |
|---|---|
| `2ce8b9a5` | claim |
| `cf54b8b9` | de runtime geeft een fout waar een datumstap het bereik verlaat, met de grens van ref; rood geval `datumtijdstap-buiten-bereik` (vier cases) en twee JUnit-getuigen |
| `9244d361` | een datumstap met een niet-afbrekend aantal (`Breuk`) geeft de datum van ref in plaats van een `ClassCastException`; drie casts in de exec-modeltak, één in de tak met truth maintenance |
| `16905a6d` | alleen de toelichting van een getuige |
| `6ac5665d` | lensfix: ook de dagsoortstap op een datum-tijd toetst het bereik; rood geval `dagsoortstap-buiten-bereik` |

### Wat er staat

De runtime (`RegelSpraakRuntime`, gedeeld door de drie takken) neemt de grens van ref over: een datum-tijd is een telling
in milliseconden in [−2⁶², 2⁶² − 1], en het hele aantal van een duur ligt in hetzelfde interval. De controle valt vóór
de stap, zoals in ref (`ref/lib/arith.ml`, `add_units_ms`, `whole_count_of`): eerst het aantal (`duration count … is
out of range`), dan in een klokeenheid de stap en het bereikte tijdstip, in een kalendereenheid het tijdstip en de
middernacht van de landingsdag; ook ref's twee conservatieve hoeken, zodat dezelfde gevallen falen. Buiten het bereik
is de zin die van ref zonder "(v0 raises DateTimeException)": `datum-tijd arithmetic leaves the representable
millisecond range`; ref en Drools pinnen in de rode gevallen dus hetzelfde fragment. Binnen het bereik rekent
`java.time` als voorheen; geen golden veranderde (zie onder). Op dagprecisie heeft ref geen bereik; de runtime volgt
ref zolang `java.time` de datum kan houden (jaren ± 999 999 999) en geeft daarbuiten een eigen fout. Ook
`rsDatumPlusDagsoort` en (na de lens) `rsDatumTijdPlusDagsoort` toetsen het aantal en de landingsdag.

### Acceptatie

- (a) Rode gevallen `corpus/rood/datumtijdstap-buiten-bereik` (+ en − 2 000 000 000 000 uur, + 60 000 000 000 dagen,
  2⁶⁴ + 1 dagen) en `corpus/rood/dagsoortstap-buiten-bereik` (eerst + 53 375 975 860 dagen, dan + 1 springtijdag),
  elk met `expectedFault {ref, clingo, drools}`; clingo weigert de grote aantallen al bij de vertaling (`value …
  exceeds clingo's integer range`), en de `NOTE.md` zegt dat dat niets over het bereik in milliseconden bewijst.
  JUnit: `DatumstapBereikWitnessTest` (rijen naar de gemeten antwoorden van ref, met de dagsoortstap),
  `DatumstapBereikTakkenWitnessTest` (de rode gevallen door alle drie de takken; gate 8 speelt alleen de
  exec-modeltak), `DatumstapDelingWitnessTest` (een duur uit een niet-afbrekende deling). Alle rood gemeten op de
  code van vóór de fix (gate 8: `Drools did not FAULT … observed NOFAULT`; de getuigen: geen exception, of een
  `ClassCastException`).
- (b) Gedaan, met dezelfde grens als ref; de drie takken delen de runtime.
- (c) Dagprecisie: zie boven en open punt 2. `rsDatumPlusDagsoort`: dezelfde controle op het aantal. Aftrekken: het
  aantal wordt pas na de controle omgekeerd. De `ClassCastException` was er (gemeten) in de exec-modeltak en de tak
  met truth maintenance, en is gerepareerd in `9244d361`; dat raakt twee transpilers, buiten de May-touch van de kaart,
  maar binnen haar acceptatie (c), en de hoofdsessie nam het over. De DRL-teksttak weigert zo'n stap luid (`EXCLUDE drl
  compile: … Unable to Analyse Expression … rsDatumReken(…, rsGedeeld(…), …)`), ook bij een deling die afbreekt;
  open punt 4.
- (d) Gates: zie onder. Het voorstel voor een getal in ch. 3.8 staat bij de open punten.

Meting van de bouwer over 57 grensgevallen (alle eenheden, plus en min, beide precisies): Drools geeft in alle drie de
takken hetzelfde als ref, op 9 na: 6 waar ref zelf omslaat (kaart `20261009-datumrekening-slaat-stil-om`), en 3 waar
een datum voorbij de jaren van `java.time` gaat (ref een datum; Drools en clingo een fout).

Corpusdoorloop van de hoofdsessie (elke golden door de drie takken, jar uit een bevroren kopie van `6ac5665d`, tegen
dezelfde doorloop op main `54ba99f7`): geen enkele statusregel veranderde (exec 240, DRL 194 plus de 2 bekende FAIL,
truth maintenance 120).

Volledige gateset van de hoofdsessie op `6ac5665d` (lokaal, `CLAUDE.md` "Gate set"): alle 16 groen. Gate 8: `47 red
cases fault observably in ref and clingo, floor 26; 94 pinned fragments …`, Drools-poot `19 fault-pinned, 28
not-carried`; gate 12: `check-drools: PASS` (played 240, DRL 194, JUnit 1241); gate 5: `ref suite 1154 worked
examples; 801 cases replayed exactly`; gate 6: `backend suite 1988 checks; 801 cases`; gate 4: `848 case file(s)
validate`. Beschermde paden zijn niet geraakt (`scripts/`, `spec/`).

### Lensvondsten

Copilot (sol6, effort xhigh) las na de bouw en na de fixronde (twee runs, elk één premium request). De eindlens met
Opus 5.5 is niet gedraaid: die stemt de hoofdsessie eerst met de eigenaar af.

Aanvaard en gefixt:
- lens 1, BLOKKEREND: `rsDatumTijdPlusDagsoort` toetste het resultaat niet; eerst + 53 375 975 860 dagen en dan
  + 1 springtijdag gaf in exec en DRL `+146140482-05-01T00:00` waar ref de bereikfout geeft (`6ac5665d`).
- lens 1, ADVIES: niet alle grensgetuigen pinnen een antwoord van ref (een rij verwacht bewust een fout waar ref
  omslaat); daarom staat hierboven 9 van 57, en de omslag in ref staat op een eigen kaart.

Lens 2 (de hele diff, na de fixronde): geen nieuwe BLOKKEREND-vondst; eindoordeel "klaar voor het Result". Aanvaard
als tekst, ADVIES: twee bekende verschillen met ref horen hier als verschil, niet als pariteit:
- vanaf −146136543-09-09T00:00 min 1 springtijdag geeft Drools terecht de bereikfout, terwijl ref door een overloop in
  `split_ms` een positief millisecondengetal geeft (`@4611686017705975808ms`; open punt 2);
- op dagprecisie geeft 2024-01-01 plus 365 241 760 749 dagen in ref 1000000000-01-01 en in Drools een fout, door de
  grens van `LocalDate` (jaren ± 999 999 999); de getuige pint de fout van Drools (open punt 2, de normvraag).

Afgewezen: geen.

### Open punten (voor de eigenaar)

1. **Voorstel voor ch. 3.8** (beschermd pad, niet op de branch): de zin "A step whose result leaves the representable
   millisecond range is a fault … (v0's `LocalDateTime.plus` raises `DateTimeException` past its range)" klopt niet
   (`java.time` reikt tot ± 999 999 999 jaar, dus v0 faalt niet op de voorbeelden) en geeft geen getal. Voorgestelde
   tekst van de bouwer:

   > **The representable millisecond range (normative).** A datum-tijd value is a whole number of milliseconds since
   > 1970-01-01T00:00:00 in the closed interval [−2⁶², 2⁶² − 1], that is −4 611 686 018 427 387 904 to
   > 4 611 686 018 427 387 903 ms: from −146136543-09-09T08:23:32.096 to 146140482-04-24T15:36:27.903 in the proleptic
   > Gregorian calendar, about 146 million years either side of the epoch. The bound belongs to the language, not to
   > an engine: an executor with wider integers still faults outside it, and one with narrower integers refuses loudly
   > where it cannot follow (the ASP backend refuses a count beyond its 32-bit integers at translation, S6a). A
   > datum-tijd step is checked before it is taken and faults, never wraps around, when the duration's whole count
   > (after truncation toward zero) lies outside the interval; in a sub-day unit, when the step itself (the count times
   > the unit's length in milliseconds) or the instant it reaches lies outside it; in a calendar unit, when the instant
   > it reaches lies above the interval or the midnight of the civil day it lands on lies below it.
   >
   > Worked example: 2024-01-01T00:00:00 + 1 281 023 420 655 uur = **146140482-04-24T15:00:00**, one hour more =
   > **fault**; + 18 446 744 073 709 551 617 dagen (2⁶⁴ + 1) = **fault**: the count itself is out of range and never
   > wraps to 1.

   Het alternatief, "alleen het resultaat telt", vraagt dat ref en Drools allebei in de twee conservatieve hoeken
   veranderen.
2. **Normvraag dagprecisie**, met de omslagen in ref en clingo en `rsEerstePaasdag` in Drools: kaart
   `20261009-datumrekening-slaat-stil-om` (main `84932950`). Nieuw sinds die kaart (fixronde): `floor_div` in
   `split_ms` van ref loopt over binnen ongeveer een dag van −2⁶²; vanaf −146136543-09-09T00:00 min 1 springtijdag geeft
   ref `@4611686017705975808ms` in plaats van de bereikfout (toegevoegd aan die kaart).
3. **De tak met truth maintenance** laat de nieuwe fouten als exception uit de fire komen; alleen een deling door nul
   is daar een `Fout`-feit dat aan het fixpunt telt: kaart 2 (`20261008-tms-expressievormen-en-rekendatum`, punt 6).
4. **De DRL-teksttak** weigert een datumstap waarvan het aantal een deling is (`Unable to Analyse Expression`), ook
   als die afbreekt: luid, geen golden; voor `20261008-drl-teksttak-op-truth-maintenance`.
5. **Landing**: de branch staat op main `9b9a42ad`. Main is sindsdien verder gegaan (alleen kaarten); en
   `20261009-drools-filterfout-buiten-de-lezende-regel` raakt `ExecModelTranspiler` op een andere plaats. Vóór de
   ff-merge dus `git rebase main` in de worktree en de gates opnieuw (zie het overzicht van de hoofdsessie).
6. Geen registertekst voor `spec/CONSTRUCTS.md`; voor PLAN §9 niets.
