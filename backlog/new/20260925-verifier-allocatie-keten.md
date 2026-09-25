# verifier-allocatie-keten: een toewijzing op super- én subtype voor één doel, zonder poort

Bijvangst van de kaart verifier-supertype-dubbele-schrijver (2026-09-25,
U7 en lensvondst A17.1): WF-026 (één toewijzing per (type, attribuut)) en
de toewijzingslift van WF-050 keyen op het gedeclareerde type, zoals WF-079
dat vóór die kaart deed. Gemeten op de L5-wire: een toewijzing op `Berger`
en een op `Hoofdberger is een Berger` voor hetzelfde doel is verifier-schoon
in beide bronvolgordes, terwijl `ref` en de Drools-exec-tak geen fixpunt
vinden, clingo twee waarden geeft (`[100;500]`) en de DRL-tak naar
bronvolgorde antwoordt. De principiële skip van de exec-transpiler ziet het
ketenpaar evenmin. Anders dan bij de rekenregels wordt een toewijzing nooit
in een defaults-stapel gevouwen (ch. 7.10 vouwt alleen rekenregels), dus
een ketenarm voor WF-026 heeft geen poort: het paar is altijd ill-formed.
IR-SPEC ch. 6.1 eist al dat de single-writer-hekken de effectieve stapel per
concreet type lezen.

## Te beslissen bij oppakken

- (a) WF-026 keurt per concreet type door de keten (de lijn van WF-079 sinds
  2026-09-25): twee toewijzingen op één (type-of-voorouder, attribuut) zijn
  ill-formed, met de uitdrukbare vorm in de zin (één toewijzing op het
  hoogste type dat het doel declareert, of een subtype-eigen doel).
- (b) De lift van WF-050 (groep op het subtype, toewijzing op het supertype)
  keurt dezelfde keten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproductie op de vier uitvoerders in de kaart vastgelegd (de
  proefdocumenten van 2026-09-25 opnieuw gebouwd, rechtenvrij).
- (b) Negatiefs `wf026-keten-…` en `wf050-keten-…` (**voorstel**),
  rood-eerst, mutatieproef; de spec-zinnen in IR-SPEC ch. 3.2 en 6.1
  (**voorstel**); één zin in de verifier zonder regelnummerstaart.
- (c) Alle bestaande goldens met een supertype schoon; toka alleen tellen.
- (d) Alle veertien gates groen.

May touch: `spec/verifier/**`, `spec/IR-SPEC.md`, `corpus/negatief/**`
(alle **voorstel**), kaartverplaatsing. Niet in deze kaart: de uitvoerders.
