# drools-temporele-naamnorm: temporele lookups vergelijken op exacte spelling

Gemeten door de periode-zink-kaart (2026-09-23, route R9): een
heelperiode-guard met `Gehesen` tegen een declaratie `gehesen` is voor
de verifier, ref en clingo hetzelfde kenmerk (naamnorm: trim +
witruimtevouw + lowercase, RUNTIME-SEMANTICS ch. 5.6), maar de
Drools-backend zoekt temporele namen op EXACTE spelling: 22×
`kenmerkGranulariteitLeden().containsKey` en 50×
`granulariteitLeden().containsKey`. Gevolg vóór de zink-kaart: de
ankerpoging miste het kenmerk en exec las stil het DOEL (antwoord 1
waar ref/clingo 0 geven); sinds die kaart wordt de doelpoging benoemd
geweigerd, dus de flank is dicht — maar de lookup-asymmetrie zelf
blijft en kan elke temporele vorm met afwijkende spelling naar een
weigering sturen waar de andere uitvoerders spelen. De golden
`jutterswet-seinjaar` pint de huidige stand bij naam en moet bij het
oppakken van kanaal wisselen (de NOTE zegt dat expliciet).

Zelfde familie, erbij belegd (consistentie-label-kaart, 2026-09-23):
`CaseRunner` vergelijkt de REGELNAAM van een inconsistent-sleutel
byte-exact waar ref `norm` plus versiestrip toepast. Geen
corpusdrager vandaag; hier meenemen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Inventariseer de 72 lookup-plekken en leid ze door één
  genormeerde vergelijking (het huispatroon van de
  enumnaam-identiteit: `Namen.norm`), of motiveer per klasse waarom
  exacte spelling daar juist is.
- (b) Rood-eerst getuigen beide takken voor de R9-vorm (guard-spelling
  wijkt af van de declaratie): na de fix speelt hij met refs antwoord.
- (c) `jutterswet-seinjaar` wisselt van kanaal (weigering → PLAYED in
  de ankerpoging): pins en kanaallijsten bewegen — benoemd
  beschermd-pad-voorstel; corpussweep verder byte-identiek.
- (d) Alle veertien gates groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh`
(**voorstel**, pins/kanaallijsten), kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
