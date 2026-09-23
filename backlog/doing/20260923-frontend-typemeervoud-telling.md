# frontend-typemeervoud-telling: kent de brontaal "het aantal Vondsten"?

Openstaande normvraag uit de aantal-zonder-bron-kaart (2026-09-23,
open punt 2): "het aantal Vondsten" — een los typemeervoud als
telbron, zonder rol- of relatiebinding — wordt sinds de
`tellingBronFout`-ERROR door de frontend geweigerd, waar hij eerst
stil bronloos emitteerde (antwoord: het operand, niet de instanties).
Of de brontaal een telling over ALLE instanties van een type kent
(§13.4.16-lezing), en zo ja langs welke wire-vorm (een
instantiebron met elementtype en documentwijde scope?), is nooit
beslist. Vandaag is weigeren de veilige stand; deze kaart beslist of
er een gedragen vorm moet komen.

## Acceptatie (bij oppakken aanscherpen)

- (a) De brontaalvraag beantwoorden met sectieverwijzingen (v2.3.0
  read-only, niet citeren in artifactzones) en de toolchain-praktijk
  meten (emitteert de toolchain er iets voor in toka/?).
- (b) Bij "ja": de wire-vorm ontwerpen als voorstel (spec-first) en
  de frontend-route bouwen met getuigen; bij "nee": de weigering
  normatief vastleggen met een frontend-getuige die de zin pint.
- (c) Alle veertien gates groen; herpin bij frontend-beweging.

May touch: `frontends/regelspraak/**`, `spec/**` (**voorstel**),
`corpus/**`, kaartverplaatsing.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/frontend-trio` (derde van drie), na
`frontend-bron-annotatie-in-defaults`. Deze kaart raakt `spec/**` en
`corpus/**` niet: bij "ja" komt de wire-vorm als voorstel in het Result,
bij "nee" blijft de weigering staan.

1. De brontaalvraag beantwoorden met sectieverwijzingen naar RegelSpraak
   v2.3.0 (hoofddocument en typeringen, read-only, niet geciteerd): kent
   de taal een telling over ALLE instanties van een type?
2. De toolchain-praktijk meten: de toolchain-IR in `toka/` (read-only,
   geen citaten) op de wire-vormen van de telling, en de toolchain-resolver
   op dezelfde zin.
3. Bij "ja": de wire-vorm als voorstel (instantiebron, elementtype,
   documentwijde scope; wat IR-SPEC, schema, verifier, ref, clingo en
   Drools moeten dragen), niet gebouwd, en de weigering gepind tot hij
   landt. Bij "nee": de weigering normatief vastleggen met een
   JUnit-getuige die de zin pint en een `DIVERGENCES.md`-notitie.
4. Rood-eerst, mutatieproef per voorwaarde met cmp-herstel.
5. Gate 10 byte-identiek (70 rgs-goldens), alle veertien gates groen, elk
   als losse aanroep.
