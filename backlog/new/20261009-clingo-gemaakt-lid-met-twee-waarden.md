# clingo-gemaakt-lid-met-twee-waarden: een objectcreatie naast een staande regel op hetzelfde lid geeft het gemaakte object in clingo twee waarden

*Gevonden op 2026-10-09 door de bouwer van stap 3 van `20261008-tms-expressievormen-en-rekendatum` (de schrijvers) en
door de hoofdsessie nagemeten en vastgelegd volgens de afspraak dat een bestaande bug in ref, clingo of de spec een
kaart krijgt. Gemeten op die kaartbranch (`1738bf61`) met ref, clingo en de verifier uit `_build` van de worktree en de
shaded jar van `backends/drools`; de documenten staan onder `/tmp/lens-tms-k2/proef/creatierand/` en hieronder
beschreven.*

## Aanleiding

Een objectcreatie zet de leden van het nieuwe object bij het maken (RUNTIME-SEMANTICS ch. 5.5); ref leest die waarden
als de invoer van het nieuwe object, zodat een staande rekenende regel op hetzelfde lid ze verdringt (ch. 7.8).

Document `A-staande-regel`: objecttypen Jutter (`vangst`, `terug`), Melding (`waarde`) en Jutmelding (subtype van
Melding); feittype `jutmelding` (melder: Jutter, één; melding: Jutmelding, meer). Objectcreatie "meld de vangst": een
Jutter maakt een Jutmelding met waarde = zijn vangst. Regel "waarde van een melding": "De waarde van een Melding moet
gesteld worden op 99" (op het supertype, dus ook op de gemaakte Jutmelding). Regel "terug van een jutter": de som van de
waarde van zijn meldingen, of 0. Case: J1 met vangst 7 (en een Melding M1 zonder waarde).

| Uitvoerder | J1.terug |
|---|---|
| ref | 99 |
| clingo | 106: het gemaakte object draagt zowel 7 als 99 |
| Drools, oude exec-model-tak en oude DRL-teksttak | 99 |
| Drools, tak met truth maintenance | 99 (sinds `8185e0d6`, fixronde 1 van stap 3 van die kaart; daarvoor een schrijversfout) |

Zonder de regel "waarde van een melding" (`B-alleen-creatie`) geven alle vijf 7. Clingo houdt dus de waarde van de creatie naast die van de regel,
waar het model één waarde per lid hoort te hebben; er volgt geen `viol` en geen fout. Volgens de meting van die bouwer
(in elke regelvolgorde) geeft clingo ook elders een andere waarde dan ref zodra een schrijver de waarde van de creatie
ontmoet: met een initialisatie op het gemaakte lid 12 waar ref 7 geeft (de creatie vult het doel, de initialisatie
schrijft niet), en met een niet-staande regel die leeg berekent 7 waar ref 0 geeft (de lege berekening wist de
waarde).
Geen golden heeft de combinatie. (Een variant met een defaults-groep op hetzelfde lid is niet welgevormd: WF-050.)

## Aanvulling (2026-10-09, avond): een verdeling naast een andere schrijver

Dezelfde klasse, gemeten tijdens `20261008-tms-verdeling-en-tijdlijnen` (documenten verifier PASS): een verdeling naar
rato (pot 100, één ontvanger met gewicht 1) naast een andere schrijver van het aandeel.
- Naast een initialisatie 5: ref 100 (de verdeling schrijft, de initialisatie beslist niet); clingo `J1.bergingsaandeel
  = [5;100]`.
- Naast een rekenende regel die 5 schrijft: ref faalt ("two writers of one target disagree: rule … writes 5 and
  allocation … writes 100", ch. 5.7); clingo `[100;5]`.
De tak met truth maintenance weigert beide paren luid (getuige `TmsVerdelingWitnessTest` deel H); de oude Drools-takken
spelen 100 bij het paar met de regel.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, eigen Jutterswet-casus, rechtenvrij, met cases met en zonder de staande regel, en een
  case met een niet-staande regel die leeg berekent; de verwachtingen zoals ref.
- (b) Clingo gerepareerd, rood eerst: ref ≡ clingo ≡ Drools op de nieuwe golden. Nagaan of hetzelfde geldt voor een
  feitcreatie en voor een initialisatie op een gemaakt lid.
- (c) Als de spec niet vastlegt dat de ledenwaarden van een creatie de invoer van het nieuwe object zijn: een zin met
  een voorbeeld in ch. 5.5 of 7.8 (beschermd pad: voorstel).
- (d) Gate 12: de nieuwe golden speelt in de takken die de vorm dragen (de pins als voorstel); alle gates groen.

## Omvang

Klein tot middel.

May touch: `backends/clingo/`, `corpus/golden/<nieuwe golden>/`, `ref/test/`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
`scripts/check-drools.sh` (pins, voorstel), kaartverplaatsing.
