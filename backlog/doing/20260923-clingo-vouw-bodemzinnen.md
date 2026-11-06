# clingo-vouw-bodemzinnen: niet-numerieke vouwen weigeren op een valdoorarm

Gemeten door de temporele-datumvouw-kaart (2026-09-23, zusterarm-
meting d): een DATUM-TIJD- of TEKST-getypeerde tijdlijn of setbron
onder een optellende vouw geeft in clingo geen fout getal — maar de
weigering komt uit de valdoorarmen `dt_term` en `symbolic_term`, en
dat zijn bodemzinnen: dezelfde zin voor elke vorm die de arm vangt,
geen benoeming van het construct. Gate 8 verbiedt zo'n zin als pin,
dus deze vormen kunnen geen rode getuige krijgen tot ze een eigen
benoemde arm hebben. De `Date_in_days`-vormen kregen die arm bij de
datumvouw-kaart (`refuse_date_timeline_total`); dit kaartje trekt de
zusters gelijk.

Aanvulling uit de datumvouw-lens (2026-09-23): ook MAXIMUM, MINIMUM,
EERSTE, LAATSTE en SOM over de eigen datumtijdlijn zijn
verifier-schoon en weigeren in ref én clingo uitsluitend met een
bodemzin (clingo: "expression kind not translated (no corpus case
exercises it)"). Geen fout getal, wel dezelfde onbenoemde klasse —
meenemen in de meting.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet per vorm (datum-tijd-tijdlijn, tekst-tijdlijn, de
  setbron-tegenhangers, en de vouwsoorten MAXIMUM/MINIMUM/EERSTE/
  LAATSTE/SOM over de eigen datumtijdlijn) de vijf uitvoerders; leg
  vast welke valdoorarm vandaag vangt.
- (b) Elke vorm krijgt een benoemde, fase-dragende weigerzin die het
  construct en de namen noemt (huispatroon van
  `refuse_date_timeline_total`); rood-eerst met mutatieproef.
- (c) Eén rode getuige per arm (`expectedFault {code, params}`;
  Drools-been met reden in de gate-8-lijst — voorstel).
- (d) Alle veertien gates groen; pins bewegen niet.

May touch: `backends/clingo/**`, `corpus/red/**`,
`scripts/check-red.sh` (**voorstel**, telling/lijst),
kaartverplaatsing. NB: serieel met andere translate.ml-kaarten.

## Result

Gebouwd op branch `card/creation-condition-and-clingo-cleanup`. Logs onder `uchar-dev/werk/clingo-cleanup/logs/`,
documenten van de meting onder `uchar-dev/werk/clingo-cleanup/k3/` (`gen.py`, `t-*`).

- (a) Opnieuw gemeten (`logs/k3-measure-1.log`, verifier, ref en clingo op de stand vóór de bouw):
  - som en totaal over een datum- of teksttijdlijn of -setbron: de verifier weigert ze (WF-091, negatieve gevallen
    `wf091-set-source-sum-text`, `wf091-set-source-sum-date`, `wf091-timeline-total-date`); niet meer bereikbaar;
  - een rangvouw (minimum, maximum, eerste, laatste) over de eigen tijdlijn: niet schrijfbaar (zonder `elementType`
    WF-037, met `elementType` en zonder bereik een laadfout, B12);
  - een rangvouw over het tijdsafhankelijke attribuut van de leden van een set naar een scalair doel: verifier PASS,
    ref `REF-005` (de bodemarm van zijn dispatch, soort `fallback`), clingo een fout zonder code;
  - dezelfde vouw, en een rangvouw over twee eigen tijdlijnen, naar een tijdlijndoel: verifier PASS, ref zijn
    algemene zin voor een tijdlijndoel ("temporal writes are not implemented", zonder code), clingo `CLINGO-024`
    (de bodemarm van zijn dispatch);
  - een opgesomde rangvouw over tijdlijnen naar een scalair doel: ref en clingo (eerste, laatste) weigeren met de
    zin van ch. 3.3 (een tijdlijn op een scalaire plaats), clingo minimum en maximum met `CLINGO-008`, omdat het een
    opgesomd minimum of maximum ook over scalairen niet vertaalt (geen tijdlijnvorm; buiten deze kaart).
- (b) Eén benoemde weigering, met de vouw en het attribuut als parameters (ch. 3.4c: de vouw noemen hoort bij de
  clausule), in beide uitvoerders: `REF-013` (`ref/codes.tsv`) en `CLINGO-040` (`backends/clingo/codes.tsv`), soort
  `refusal`, sleutel `ranking-fold-over-a-timeline`, parameters `fold`, `attribute`; gemaakt door
  `refuse_ranking_fold` in `ref/lib/eval.ml` en `backends/clingo/lib/translate.ml`, op drie plaatsen: de setvouw naar
  een scalair doel (ref vóór `REF-005`, dat de bodemarm blijft; clingo in plaats van de codeloze fout) en elke
  rangvouw over een tijdlijn als waarde van een tijdlijndoel (ref vóór zijn algemene zin, clingo vóór
  `CLINGO-024`, via `ranked_timeline`). Na de bouw weigeren ref en clingo alle twaalf vormen (vier vouwen, drie
  plaatsen) zo (`logs/k3-measure-after.log`); beide Drools-takken weigerden ze al met een code (DROOLS-210 en
  DROOLS-105, `logs/k3-drools.log`).
- Rood eerst: ref-suite op de oude `eval.ml` 13 FAILURES, clingo-suite op de oude `translate.ml` 15
  (`logs/k3-suite-ref-old.log`, `logs/k3-suite-clingo-old.log`); groen ref 2547 voorbeelden, clingo 2756 checks
  (`logs/k3-suite-ref.log`, `logs/k3-suite-clingo.log`). Frames: `WV-3.12-rangvouw-over-een-tijdlijn` en `ch3.12 a
  ranking fold over a timeline` (twaalf elk, code en parameters gepind). Vier bestaande frames pinden de oude zin
  (`WV-3.4c-temporeel-rangschikking-eigen-zin`; in clingo `minimum/maximum over a temporal set stays refused` en de
  twee `ch3.6 EERSTE/LAATSTE …`): hun naald is nu de nieuwe zin die de vouw noemt (gedrag dat de kaart uitdrukkelijk
  verandert).
- (c) Geen rood geval: gate 8 pint een EF-code, en ch. 5.7b geeft deze vorm geen foutsoort (alleen de gevulde
  telling heeft er een, EF-021). Een EF-code toevoegen is een normkeuze; zie het open punt.
- (d) Gerichte gates groen (`logs/k3-gates.log`): gate 1; gate 5 (ref-suite 2547, 1120 cases); gate 6 (suite 2756,
  1114 cases, het weigerkanaal ongewijzigd); gate 8 (85 rode gevallen, `check-exec-codes` 13 REF- en 40 CLINGO-codes).
  Geen pin of floor bewoog.

Open punt (normvraag): ch. 3.12 rij 4 noemt minimum, maximum, eerste en laatste over een tijdlijn "refused by
name by every executor" en geeft alleen de gevulde telling een foutsoort (EF-021, `declined`). Moet de rangvouw over
een tijdlijn een eigen foutsoort krijgen (of EF-021 verbreed worden), zodat een rood geval van gate 8 hem kan
vastleggen? Gemeten: ref en clingo weigeren alle twaalf vormen nu met een eigen registercode, beide Drools-takken met
DROOLS-210 of DROOLS-105.
