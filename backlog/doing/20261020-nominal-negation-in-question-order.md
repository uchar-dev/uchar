# nominal-negation-in-question-order: "niet een <kenmerk>" in de vragende volgorde

*Gevonden in de fixronde na de review van frontend-kwantor-over-een-pad (2026-10-20), gemeten op `bed92c3f`;
proeven `uchar-dev/werk/kwantor-pad/fixronde/n1-subject-niet-een` en `n6-subject-de-niet-een` (alleen lezen).
Luid, geen fout antwoord.*

Tabel 16 (§8.1.8.3) ontkent een zelfstandig kenmerk met "is niet een <k>". In de stellende volgorde ("hij is niet
een wrakstuk") heeft de grammatica daar een tak voor (`nonToplevelOperator`, `NIET EEN kaleNaam`), en die speelt
(proef `n9-subject-stellend-niet-een`: PASS in ref, clingo en beide Drools-takken). In de vragende
volgorde van een voorwaarde niet: in "indien de Vondst niet een wrakstuk is" komt `niet` in het onderwerp, de
EEN-vorm zoekt de naam "niet wrakstuk", die bindt niets, en de voorwaarde blijft een marker met de WARNING
"onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp" (EF-019 in ref en clingo, een
evaluatiefout in beide Drools-takken). "indien hij niet een wrakstuk is" is een syntaxfout ("no viable alternative
at input 'nieteen'"). Onder een kwantor leest de frontend de vorm sinds de fixronde van 2026-10-20 wel
(`RuleChecker.elementCharacteristicCheck`); "geen wrakstuk" speelt in elke gemeten vorm (proeven n2, n7–n9,
f2-pad-geen).

Acceptatie:
- (a) Rood eerst: een JUnit-getuige met beide zinnen.
- (b) "indien <onderwerp> niet een <zelfstandig kenmerk> is" lowert naar de ontkende kenmerktoets, gelijk aan
  "geen <k>", voor het onderwerp zelf, "hij" en een navigatie naar één; na "niet een" blijft een ander soort
  kenmerk of een naam die niets bindt luid geweigerd.
- (c) DIVERGENCES (§13.4.15.45 of de tabel van vorm 1 in §4); alle zestien gates groen.

May touch: `frontends/regelspraak/**`.
