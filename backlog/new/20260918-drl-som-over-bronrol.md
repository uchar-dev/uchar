# drl-som-over-bronrol: de tellende vouw speelt, de sommerende nog niet

Rest van de rolvelden-kaart (2026-09-18), door de lens precies
afgebakend: sinds de per-rol-afleiding speelt een AANTAL over een
geschreven rol van een herhaald typepaar in beide Drools-takken, maar
een SOM/MIN/MAX over dezelfde symbolische `bronRol` speelt alleen in
exec-model; de DRL-tekst-tak geeft `MARKER: TODO aggregatie SOM`. Op
main bestond deze takongelijkheid niet — beide takken weigerden de vorm
— dus de split is door de rolvelden-kaart nieuw bereikbaar geworden en
bewust buiten het corpus gehouden (de nieuwe golden vouwt uitsluitend
AANTAL).

Meettabel van de lens: herhaald paar — verifier PASS, ref PASS, clingo
PASS, exec PLAYED, drl MARKER; uniek paar — alle vijf groen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de marker letterlijk op een eigen invoer met
  `bronRol` over een herhaald paar.
- (b) De DRL-tekst-tak lowert SOM/MIN/MAX over een symbolische
  `bronRol` langs dezelfde per-rol-afleiding als AANTAL; gedeelde
  vormen weigeren blijft per construct met reden.
- (c) Dual-branchgetuigen + mutatieproef; daarna kan een corpusgetuige
  de vorm pinnen (kanaaltellings-consequentie als
  beschermd-padvoorstel).
- (d) Alle veertien gates groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/test/**`, eventueel één corpusgolden +
`scripts/check-drools.sh`-telling (voorstel), deze kaart en haar
verplaatsing binnen `backlog/`.
