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

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Beide: (a) WF-026 keurt per concreet type door de typeketen — een toewijzing wordt nooit in een stapel gevouwen (ch. 7.10 vouwt alleen rekenregels), dus zonder poort: twee toewijzingen op één (type-of-voorouder, attribuut) zijn ill-formed; (b) de toewijzingslift van WF-050 (groep op het ene niveau, toewijzing op het andere) keurt dezelfde keten. De uitvoerders veranderen niet.
- U2 Zinnen (voorstel; de bouwer stelt vast en meldt letterlijk, zonder regelnummerstaart): `two allocations write target (Hoofdberger, bergloon) through its type chain — one declared on Berger, one on Hoofdberger — and an allocation is never folded into a stack, so both fire on a Hoofdberger instance; at most one allocation writes a target (ch. 3.2)`; voor WF-050 de bestaande liftzin met de ketenclausule. Pointer op de versie van het laagste type; ontdubbeling per (pointer, zin) zoals WF-079. Hergebruik de ketenpredicaten van WF-079 (`checks.ml`), dupliceer geen ketenlogica.
- U3 Getuigen: negatiefs `wf026-keten-twee-toewijzingen` en `wf050-keten-toewijzing-naast-groep`, rood-eerst (verifier PASS geciteerd), `expect.txt`, NOTE uit echte uitvoer, mutatieproef; de reproductie op de vier uitvoerders (proefdocumenten van de supertype-kaart, `/tmp/supertype/u7/` als die er nog staan, anders eigen) in het Result; elke golden met een supertype blijft schoon (gate 3); toka alleen tellen.
- U4 Spec: IR-SPEC WF-026 (ch. 3.2) en WF-050 krijgen de ketenclausule; ch. 6.1's zin blijft; RS ch. 7.10 één zin dat een toewijzing niet vouwt en WF-026 daarom zonder poort door de keten spreekt. Naamtabel rc 0; geen D-/U-nummers.
- U5 Gates 1, 2, 3.
