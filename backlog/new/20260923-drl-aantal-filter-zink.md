# drl-aantal-filter-zink: de DRL-tekst-tak zinkt op een gekwalificeerd AANTAL-filter

Gemeten door de periode-zink-lens (2026-09-23, proef-IR's
OWN-D-filter-in-aantal en OWN-D2-regel): een heelperiode-kwalificatie
in het FILTER van een `AANTAL` ("het aantal posten van het kustvak die
gedurende het gehele jaar bemand zijn > 0") is verifier-schoon, ref en
clingo geven PASS 0 en exec speelt correct — maar de DRL-tekst-tak
zinkt het hele pakket met een ongelezen compilerfout (`Unable to
Analyse Expression rsGeheelPeriode($subject.getBemand(), "JAAR", 7,
null)`), zowel als creatie-guard als in een gewone rekenregel. Het zit
in de accumulate van de ANKERpoging — een andere positie dan de
doelpoging die de kaart drl-periode-zink dichtte, dezelfde verboden
klasse: een pakketbrede compilerfout in plaats van een per-vorm-
weigering met reden. Geen corpusdrager; gate 12 telt de EXCLUDE
zonder bruikbare reden.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide vormen (creatie-guard en rekenregel) met de
  vijf uitkomsten; leg vast welke documentvormen de accumulate-zink
  bereiken (draagtGeheelPeriode daalt niet af in een Expr — meet of
  dat het hele gat is).
- (b) Bedraden of per construct weigeren met een benoemde reden, langs
  de afweging van de zink-kaart: bedraden alleen als de lowering op
  deze positie werkelijk het juiste subject leest (exec speelt hier
  correct — meet wat exec doet en of de DRL-tak dat kan volgen);
  anders de redendragende weigering.
- (c) Rood-eerst getuigen; bij een fix een golden (vierkant), bij een
  weigering de kanaallijstvermelding — pins/lijsten als benoemd
  voorstel.
- (d) Corpussweep byte-identiek; alle veertien gates groen.

May touch: `backends/drools/**`, `corpus/**`, `scripts/check-drools.sh`
(**voorstel**, pins/kanaallijsten), kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
