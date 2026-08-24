# De vormenkabinet-case

Goal: corpus/golden/jutterswet-vormenkabinet draagt geen enkel
case-bestand; gate 5 meldt hem al meerdere kaarten als "NOT
replayed". Zoek eerst uit wat de golden pint en of het ontbreken een
reden had (git log, README's); is het gewoon een gat, schrijf dan
cases met absolute hand-berekende verwachtingen die de golden
volwaardig laten meedraaien in gates 4-7 (case-schema, oracle,
clingo, noop); had het een reden, documenteer die dan expliciet in
de golden zelf en laat gate 5 hem gemotiveerd blijven melden.

Acceptance: alle negen gates groen; gate 5 meldt de golden niet meer
als case-loos (of draagt de gedocumenteerde reden); rood-eerst
(noop-refutatie of reden-dragende survivor-pin); twee opus-lenzen
verwerkt vóór de merge.

May touch: corpus/golden/jutterswet-vormenkabinet/,
scripts/check-noop.sh (alleen indien nodig), backlog/. Out of scope:
al het overige.

Result: bewuste keuze, nog steeds geldig — gedocumenteerd, geen case
verzonnen. Onderzoek (git log f903b89, corpus/golden/README.md
"witness cabinet", kaart backlog/done/20260818-uniciteit-en-wf-opruim.md)
toont dat jutterswet-vormenkabinet het verifier-only getuigedocument is
dat de Golden-kolom-OPENs van WF-004/006/014/016/017/021/031/032/039/
040/041/042/043 sluit. De case-loosheid is geen gat: ref én clingo
replayen een case door het HELE document te evalueren, en drie regels
oefenen constructen die beide executors botweg weigeren. Empirisch
aangetoond met de ref-runner op deze ir.json:
  - `vakcijfer uit voorjaarsopbrengst` (DimensieCel-lezing van de
    gedimensioneerde opbrengst, WF-004/014/040) →
    "expression kind not implemented in this card";
  - `peiling bij dubbele borging` (telkwantor met een Periode-tijdvenster-
    lid, WF-041/042) → "condition kind not implemented in this card";
  - `doorwaadbaar bij lage stand #1/#2` (geheel-periode-vergelijking over
    de temporele waterstand, WF-043) → "whole-period comparison is ch. 3
    scope".
De twee wél-uitvoerbare regels (staat=gaaf, aanwas=3) zijn niet los
replaybaar (de evaluator draait elke resolvedRegel). Een case met nul
Schouwveld-instanties ontwijkt de faults maar pint niets — precies de
"melding wegpoetsen"-case die de kaart verbiedt.

Actie: NOTE.md toegevoegd ín de golden die de reden empirisch vastlegt
(de drie letterlijke faults + reproductie + de krimp-conditie); gate 5
blijft de golden gemotiveerd als "NOT replayed" melden. Geen case, dus
geen rood-eerst/noop-refutatie van toepassing; scripts/check-noop.sh
ONGEWIJZIGD (de survivor-lijst iterateert over case*.json — deze golden
komt daar niet in voor). Alle negen gates groen (slotregels geciteerd
in de sessie-uitvoer).
