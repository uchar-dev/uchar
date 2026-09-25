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

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Bronarm onder WF-037 (geen nieuw nummer): een telling (`count`/`filledCount`) met `elementType` draagt als enig argument een instantiebron (de vormen van WF-096/WF-102: een `AttributeReference` langs een to-many rol of pad, met of zonder `sourceRole`/`sourcePath`); elke andere vorm (Literal, Binary, ParameterReference, VariableReference, BuiltinCall, …) is ill-formed bij naam; WF-047 treedt terug (één melding per gebrek), gespiegeld aan de setbronarm van de gevulde telling en aan de omgekeerde arm van 2026-09-25. Negatiefs per vorm (`wf037-aantal-elementtype-literaal`, `-binair`, `-parameter`, …), rood-eerst, `expect.txt`, NOTE uit echte uitvoer, mutatieproef.
- U2 `ref` en clingo weigeren in dezelfde zin vanuit benoemde armen (de bodemzin `unsupported instance-fold source shape` en de clingo-bodemzin vervallen voor deze vormen); suitegetuigen in `ref/test/test_ref.ml` en `backends/clingo/test/test_clingo.ml` (mandaat); rood-eerst met een mutant.
- U3 De Drools-lader (`IrLoader.java`) weigert de vormen die WF-037 weigert — elementType zonder argument, een `ListLiteral`, en de niet-bronargumenten van U1 — met de zin van de verifier (`Refusal`), zodat geen ill-formed telling meer stil speelt; JUnit-getuige (nieuwe klasse); de zinnen letterlijk gelijk in vier lezers.
- U4 Tweede taak, direct fixen waar het kan: de exec-tak sluit een consistentie uit waarvan de assertie een `Compound` met een andere kwantor dan `all` is (`ExecModelTranspiler.java` r. ~5577, "schending buiten Phase-D scope"). Meet eerst; hergebruikt de lowering de bestaande conditie-evaluatie van regels (die `atLeast`/`atMost`/`exactly`/`none` al draagt), bouw hem dan, met een golden (handgerekend, ref ≡ clingo ≡ exec, `bron.rgs` als de frontend de vorm schrijft) en gate-12-pins (**voorstel**); anders meld je precies wat ontbreekt.
- U5 Gates 1, 2, 3, 5, 6, 8, 12 (13 als een golden een type-annotatie op een afgeleide positie draagt).
