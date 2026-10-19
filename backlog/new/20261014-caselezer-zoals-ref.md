# caselezer-zoals-ref: de Drools-caselezer leest sleutels en labels anders dan ref

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit het
verslag van stap 2b en de lens op ronde 4 van die kaart. Bestaand gedrag van de caselezer in `cases/CaseRunner`, op
beide takken; geen golden raakt het.*

## Aanleiding

Gemeten in stap 2b van die kaart, met een aangepaste case:

- **Een sleutel met een dubbele spatie.** "geschatte  waarde" (twee spaties) als attribuut in case1 van
  `anchor/jutterswet-waardestelling`: ref en clingo geven PASS; de Drools-takken weigeren met `EXCLUDE case sets
  undeclared attribute 'geschatte  waarde' on Vondst`. `Objecttype.attribuut` vergelijkt met `equalsIgnoreCase` en
  vouwt geen witruimte.
- **Een sleutel die alleen in een niet-ASCII-hoofdletter verschilt.** "İŞ" voor het attribuut "İş" (eigen document
  `eigenschap/turks-lid`, case-invoer): ref en clingo geven `CASE ERROR: attribute … is not declared on "Jutter"`; de
  Drools-takken spelen (`PLAYED 1`). De caselezer aanvaardt hier ruimer dan ref.

Gemeld door de lens op stap 6c (2026-10-14): de caselezer zoekt een lid via de afgeleide Java-naam en toetst niet of
de gespelde naam gedeclareerd is. Een verwachting op "premie-som" voor het lid "premie som" geeft dezelfde getter
(`getPremieSom`), dus replay en scenariorapport slagen, waar ref de verwachting als niet-gedeclareerd weigert
(`ref/lib/case.ml`, r. 695–701 op 2026-10-14). Een tikfout in een scenario blijft zo onopgemerkt.

Gemeld door de lens op ronde 4: `CaseRunner` (r. 620–634 op `605fc574`) geeft bij een tweede case-instantie met
hetzelfde label de eerste bean terug. Twee instanties met één label worden zo stil samengevoegd in plaats van als
ongeldige invoer geweigerd. Wat ref en clingo daar doen, is niet gemeten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Leg vast welke regel de spec of het caseschema geeft voor het vinden van een sleutel (witruimte, hoofdletters) en
  voor een label dat twee keer voorkomt, en meet ref en clingo op elk van de drie gevallen.
- (b) De Drools-caselezer volgt die regel op beide takken; waar ref weigert, weigert hij met een zin die gate 8 kan
  pinnen.
- (c) Rode cases onder `corpus/red/` waar de invoer ongeldig is (`expectedFault` voor ref, clingo en drools), en een
  golden waar ref de invoer aanvaardt; rood eerst.

May touch: `backends/drools/**` (`cases/`, `schema/`), `corpus/red/**`, `corpus/golden/**`, `scripts/check-red.sh`
(**voorstel**, als de Drools-poot een regel nodig heeft).

Out of scope: `ref/`, `backends/clingo` (tenzij (a) daar een afwijking van de regel vindt; dan een eigen kaart).
