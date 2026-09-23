# drools-beannaamruimte-botsing: attribuut en kenmerk delen één getterruimte

Gemeten door de naamnorm-lens (2026-09-23, bestond al): een attribuut
`Gehesen` naast een kenmerk `gehesen` op één objecttype is
verifier-schoon en ref speelt het, maar de Drools-beanmodel-generatie
geeft beide dezelfde getter — exec faalt met `ERROR … setter
setGehesen(List)` en de DRL-tak weigert het hele bestand bij het
compileren. Luid, geen stil fout antwoord, maar een pakketbrede of
kale fout in plaats van een benoemde per-vorm-weigering. De
naamnorm-kaart maakte het geval bereikbaar via afwijkende spelling;
de botsing zelf is een naamruimtekwestie van het beanmodel.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer; meet welke declaratieparen botsen (attribuut ×
  kenmerk, en zijn er meer ruimtes — rollen?).
- (b) Benoemde weigering bij het laden (huispatroon: loader-Refusal
  die beide declaraties noemt) óf gescheiden naamruimtes in het
  beanmodel; kies de kleinste en motiveer; rood-eerst beide takken.
- (c) Corpussweep byte-identiek; alle veertien gates groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
