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
