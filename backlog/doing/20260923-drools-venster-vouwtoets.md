# drools-venster-vouwtoets: exec te streng onder ALLE, DRL leunt op de verifier

Gemeten door de tijdvenster-kaart (2026-09-23, kwantorvouwmeting): een
periodevenster in de toets van een `Kwantorvouw` speelt onder `ALLE`
in ref, clingo en de DRL-tak, maar de exec-tak weigert hem ook daar —
te streng, een kanaalasymmetrie zonder fout antwoord. Andersom
verlaagde de DRL-tak het venster onder ALLE vijf kwantoren en gaf
onder `GEEN` in een tijdvrije sessie het COMPLEMENT als antwoord;
sinds WF-112 houdt de verifier die documenten tegen, dus de flank is
dicht, maar de DRL-tak zelf draagt geen eigen weigering — wie de
verifier omzeilt (een oude wire, een handgemaakt document) krijgt
weer het complement. Huisbeginsel: elke uitvoerder weigert zelf, de
verifier is het eerste hek en niet het enige.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer beide flanken (exec-weigering onder ALLE; het
  DRL-complement op een WF-112-document met de verifier omzeild).
- (b) De exec-tak speelt het venster in een vouwtoets onder ALLE en
  DE (gelijk aan ref); de DRL-tak weigert zelf bij naam onder GEEN en
  de tellende kwantoren. Rood-eerst getuigen beide takken.
- (c) Corpussweep: kanaalwissels benoemd (een spelende exec-vorm kan
  pins bewegen — voorstel); alle veertien gates groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh`
(**voorstel**, pins/lijsten), kaartverplaatsing. NB: serieel met
andere Drools-kaarten.
