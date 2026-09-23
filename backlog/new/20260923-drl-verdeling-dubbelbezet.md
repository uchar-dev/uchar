# drl-verdeling-dubbelbezet: twee verdelingen op één ontvanger breken de DRL-tak

Gemeten door de consistentie-label-lens (2026-09-23, bestond al op de
basis): twee verdelingen over dezelfde ontvangers, met een leeg
gewicht, laten de DRL-tekst-tak falen met `ConsequenceException …
cannot invoke method: setAandeel` — een runtime-worp in plaats van
een benoemde weigering of het juiste antwoord. ref, clingo en de
exec-tak dragen de dubbelbezetting (de lens mat een ontvanger in een
naar-rato- én een op-volgorde-verdeling met beide regels correct op
D1 en D2 in ref, clingo en exec). Luid, dus geen stille divergentie,
maar wel de verboden vorm: een ongelezen exceptie die niet zegt welk
construct hem veroorzaakte. Geen corpusdrager.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer met een proef-IR (twee verdelingen, gedeelde
  ontvanger, leeg gewicht); meet alle vier de uitvoerders.
- (b) De DRL-tak draagt de vorm zoals exec, of weigert per construct
  benoemd; rood-eerst getuige beide takken.
- (c) Corpussweep byte-identiek; alle veertien gates groen; pins
  bewegen niet.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
