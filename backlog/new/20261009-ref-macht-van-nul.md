# ref-macht-van-nul: ref crasht op nul tot een negatieve macht in plaats van een benoemde fout te geven

*Gevonden op 2026-10-09 door de bouwer van de fixronde van
`20261009-drools-filterfout-buiten-de-lezende-regel` (bijvangst) en door de
hoofdsessie vastgelegd en nagemeten volgens de afspraak dat een bestaande bug een
kaart krijgt. Gemeten op uchar main (`_build` van de hoofdcheckout, ref
ongewijzigd sinds `54ba99f7`); het document staat in
`/tmp/lens-bugs/w2/f1/d/c3-macht/` (`source.rgs`, `ir.json`, `case2.json`).*

## Aanleiding

"Tot de macht" met een negatieve gehele exponent keert de basis om
(`ref/lib/arith.ml`, `pow_`, rond regel 275–300: `Q.inv (p lq (-e))`). Is de
basis 0, dan gooit Zarith `Division_by_zero`, die niemand opvangt: de runner
stopt met `Fatal error: exception Division_by_zero`, zonder benoemde fout, zonder
regelnaam en zonder het "EVALUATION FAULT"-formaat waaraan de rode gevallen en
de gates een fout herkennen. Nul tot een negatieve macht is een deling door een
numerieke nul (`spec/RUNTIME-SEMANTICS.md` ch. 1.4, punt 4) en hoort de fout
"division by numeric zero" te geven.

Document: een vouw "het aantal vondsten van de Jutter die aan de volgende
voorwaarde voldoen: zijn gewicht tot de macht zijn deelnemers naar beneden
afgerond op 0 decimalen is groter dan 2"; case 2: J1 actief met V1 (gewicht 0,
deelnemers −1).

| Uitvoerder | case 2 |
|---|---|
| verifier | `PASS` |
| ref | `Fatal error: exception Division_by_zero` |
| clingo | `TRANSLATION FAULT: a runtime exponent is not translated …` (weigert de vorm) |
| Drools, DRL-teksttak | `ERROR ConstraintEvaluationException … rsAfronding(rsMachtReeel …` |

## Acceptatie (bij oppakken aanscherpen)

- (a) Ref geeft `EVALUATION FAULT: division by numeric zero` (of de zin die
  ch. 1.4 voor deze vorm vraagt), rood eerst, met een voorbeeld in `ref/test/`.
- (b) Een rood geval in `corpus/red` met `expectedFault` voor ref en, waar ze
  de vorm dragen, Drools; clingo weigert de vorm al bij de vertaling.
- (c) Nagaan of andere plaatsen in `ref/lib/arith.ml` een Zarith-exceptie
  ongevangen laten (`Q.inv`, `Q.div`, `Z.div` op een nul).
- (d) Gates 5 en 8 groen; alle gates van `CLAUDE.md` groen.

## Omvang

Klein. May touch: `ref/lib/arith.ml`, `ref/test/`, `corpus/red/<nieuw geval>/`,
kaartverplaatsing.
