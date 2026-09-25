# aantal-bronarm-en-drools-lader: een telling met bereik en een argument dat geen bron is

Bijvangst van de kaart aantal-bronargument-welgevormd (2026-09-25, open
punten 1 en 2 en lensvondsten A15/A17.2). Sinds die kaart weigert WF-037 een
telling met `elementType` zonder argument en met een `ListLiteral`; de
tussenvormen niet: een `count`/`filledCount` met `elementType`, een bereik
(`sourceRole` of `sourcePath`) én een argument dat geen instantiebron is (een
`Literal`, een `Binary`, een `ParameterReference`, …) krijgt vandaag alleen
de bindingszin van WF-047, `ref` de bodemzin `unsupported instance-fold
source shape` en clingo een bodemzin uit de lijst van 36. Gemeten: de
Drools-lader voert zulke documenten uit — `count` + `elementType` +
`sourceRole` zonder argument, met een `Literal` of met een `Binary` speelt
in beide takken (PLAYED 1; hij telt het bereik en negeert het argument), en
`filledCount` zonder argument speelt in de DRL-tak. Geen gate speelt
ongeldige documenten, dus het is geen kloof in het vierkant; het is wel een
uitvoerder die een ill-formed document stil uitvoert.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een bronarm onder WF-037 (**voorstel**): een telling met `elementType`
  draagt als enig argument een instantiebron (de vormen van WF-096/WF-102);
  elke andere vorm weigert bij naam, WF-047 treedt terug (één melding per
  gebrek), gespiegeld aan de setbronarm van de gevulde telling. Negatiefs
  per vorm, rood-eerst, mutatieproef.
- (b) `ref` en clingo weigeren in dezelfde zin vanuit benoemde armen (de
  bodemzinnen vervallen voor deze vormen); suitegetuigen in beide suites.
- (c) De Drools-lader weigert de vormen die WF-037 weigert (deze kaart én
  de vorige) in de zin van de verifier, met een getuige in de eigen JUnit.
- (d) Alle veertien gates groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` (**voorstel**),
`corpus/negatief/**`, `ref/**`, `backends/clingo/**`, `backends/drools/**`,
kaartverplaatsing.
