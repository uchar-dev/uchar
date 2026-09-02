# Ongepinde executor-asymmetrie: een niet-literale `Construct`-lading speelt in `ref` en wordt door de ASP-drager geweigerd

Goal: Er is vandaag een vorm die `ref` exact speelt en die de
ASP-drager luid weigert, en GEEN corpusdocument pint dat. Gemeten in
fixronde 2 van `20260902-meldplichtspoor-niveau0` (in een `git
archive`-kopie, de werkkopie ongemoeid): een `Fold` waarvan de `init`
een `Construct` is met een `AttribuutRef` als argument in plaats van een
`Literal` — de gereïficeerde meldplicht met haar EIGEN termijn,
`Openstaand(<de termijn van deze meldplicht>)` — is verifier-clean
(`verifier: PASS`), speelt door `ref` (`oracle: PASS (3
expectations)`), en wordt door de ASP-drager vóór vertaling geweigerd,
letterlijk:

```
TRANSLATION FAULT: a Construct payload other than a literal is not
translated (a bound payload would need underivability propagation; no
corpus case)
```

De weigeringstekst zegt zelf dat er geen corpuscasus is — dat is precies
het gat. Een asymmetrie die geen enkele gate ziet, is een asymmetrie die
stil kan wegdrijven: hij kan verdwijnen (dan merkt niemand dat hij weg
is) of verergeren (dan merkt niemand dat ook niet). Een van de twee moet
gebeuren: de drager gaat de vorm dragen (dan wordt het een golden die
ref ≡ clingo pint), of de weigering wordt als weigering vastgelegd. Bij
`corpus/rood/` hoort een fout die in BEIDE executors observeerbaar
faalt, en die eis haalt deze vorm niet — `ref` speelt hem gewoon — dus
rood is NIET de plek en de kaart moet eerst bepalen wat de plek dan is.

Dit is ook de smalle rest van de identiteitsgrens die de
meldplichtspoor-NOTE post (b) noemt: een relationele positie is
uitdrukbaar door reificatie, maar "elk met een eigen termijn" strandt —
niet op identiteit, op dit hek.

Acceptance:
- De vorm hermeten en met echte uitvoer opgetekend: verifier, `ref` en
  de ASP-drager, elk met de letterlijke regel.
- Bepaal en beargumenteer WAAR de asymmetrie thuishoort. Drie routes,
  expliciet afwegen, niet stilzwijgend één kiezen: (1) de drager draagt
  de vorm — dan landt een golden en verdwijnt de asymmetrie; (2) de
  weigering is normatief bedoeld — dan hoort er een WF-nummer bij, met
  negatief-casus, en wordt zij door gate 3 gedragen in plaats van door
  een vertaalfout; (3) de weigering is een gedocumenteerde
  dragersbeperking — dan hoort zij in de weigeringsinventaris die de
  spec al voert, met de reden erbij.
- Wat route (1) zou kosten is de eerste vraag die beantwoord moet
  worden: de weigeringstekst noemt "underivability propagation" als de
  ontbrekende machinerie. Meet of dat klopt.
- Geen pin wordt aangepast om een executor te laten slagen; als de vorm
  maar door één executor speelt, landt er geen golden.
- Alle twaalf gates groen.

May touch: hangt van de gekozen route af; `backends/clingo/**` bij (1),
`spec/**` als proposal bij (2) en (3), `corpus/**` bij (1).

Out of scope: de identiteitsvraag zelf (een objectverwijzing als
datatypesoort — dat is de andere helft, post (a) in de
meldplichtspoor-NOTE, en een eigen fase); `scripts/**`.
