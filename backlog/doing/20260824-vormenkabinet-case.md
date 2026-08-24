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
