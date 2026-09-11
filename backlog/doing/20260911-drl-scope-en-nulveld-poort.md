# De twee ongelijke plekken tussen de Drools-takken

Goal: de twee plaatsen waar de DRL-tekst-tak een vraag NIET stelt die de
exec-model-tak wél stelt, gelijktrekken — met dezelfde gedeelde afleiding en
dezelfde redenzin, en met een rood-eerst-getuige per plek. Beide zijn gemeten en
geen van beide wordt door het corpus geraakt, dus dit is geen bugfix onder druk
maar het sluiten van twee gaten voordat een golden erin valt.

## Waar dit vandaan komt

De kaart `20260910-drools-ongepoorte-rollezers.md` poortte zes lezers van het
§13.3.9 referentieveld in beide takken. Bij het meten daarvan kwamen twee
plekken boven die op geen enkele lijst stonden.

### A. `DrlTranspiler.scope(...)` stelt de gedeelde vraag niet

`private Frag scope(Objecttype elem, Objecttype subject, Expr.Functie f)`
(~r. 3071, eindigend op
`Frag.todo("aggregatie-bereik", arg != null ? arg.onderwerpBron() : f.bron().snippet())`)
leidt de aggregatie-join af — eerst via de geschreven elementnaam
(`verbindendeRolViaElementnaam`), dan via het paar (`verbindendeRol`) — en emit
`<rol> == $subject` zodra één van beide een rol oplevert. Een rol is nog geen
veld: `FactModelGenerator.leden` leidt er alleen één af als de rol enkelvoudig
is, uniek bereikbaar, en haar naam niet door een gedeclareerd lid is ingenomen.
De exec-tegenhanger (`ExecModelTranspiler.aggregatie`) vraagt dat sinds
2026-08-30 (`heeftReferentieVeld` + `aggregatieReden`); deze tak vraagt niets.

**Kanaalrisico: gemeten nul.** De hele golden-verzameling door de DRL-tak
gehaald (167 goldens met case-bestanden, `--drl`): **0 kale
`EXCLUDE drl compile`-uitkomsten**. De twee goldens die dit gat vóór die kaart
lieten zien — `anchor/jutterswet-erflijn` en `anchor/jutterswet-gemeenteberging`
— worden sindsdien door de nieuwe DRL-feitcreatiepoort eerder geweigerd, mét de
§13.3.9-zin, dus die getuige reproduceert **niet meer**. De derde kandidaat,
`anchor/jutterswet-bergingstoezicht`, markert in deze tak op een andere grond
(`TODO aggregatie SOM`) en bereikt `scope` niet.

Gevolg voor het kaartwerk: **de rood-eerst-getuige moet een synthetisch
JUnit-model zijn**, gebouwd zoals de zeven in
`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/RolveldlezerWitnessTest.java`
— een golden-`ir.json` van de wire, in het geheugen gevarieerd, naar een
tijdelijk bestand, en door `CaseRunner.viaDrlTekst` gehaald. Dát is het werk;
de poort zelf is één regel.

### B. De DRL-feitcreatiepoort toetst voorwaardelijk waar de exec-poort dat niet doet

`ExecModelTranspiler.feitCreatie` weigert onvoorwaardelijk:

    if (anker == null || doel == null || fc.zetVeld() == null || fc.joinVeld() == null)
        return skip("feitcreatie (onopgelost): " + fc.naam());

`DrlTranspiler.feitCreatie` zet zijn §13.3.9-poort achter

    if (anker != null && doel != null && fc.zetVeld() != null && fc.joinVeld() != null) { … }

en laat een feitcreatie met een null anker/doel/zet/join daarna gewoon door naar
de emissie. Eén constructie, twee antwoorden: in exec een reden-dragende
weigering, in DRL niets. Hetzelfde beginsel als hierboven — één vraag in beide
takken.

**Kanaalrisico: gemeten nul.** `corpus/golden` draagt 4 feitcreaties; geen
enkele heeft een null anker, doel, `zetVeld` of `joinVeld`. Ook hier moet de
getuige dus synthetisch zijn.

## Aanpak

1. **A**: één `navVeld("aggregatie-join", elem, rol, subject)` in `scope`, vóór
   `Frag.ok(...)`, in de vorm van de zes poorten van de vorige kaart — geen
   nieuwe afleiding, geen nieuwe redenzin. Bij een weigering de bestaande
   marker-route (`Frag.todo`), zodat `navReden` de generieke tekst aanscherpt.
   Let op de VOLGORDE: de exec-tak kent bovendien een aparte,
   CONSERVATIEVE weigering voor de enkelvoudige elementrol met meervoudige
   zusterrol; die niet overnemen zonder eigen meting — hij verschuift kanalen
   (zie de waarschuwing in `aggregatieReden`).
2. **B**: de voorwaarde om de DRL-poort laten vallen en de null-gevallen even
   luid weigeren als exec dat doet, met de bestaande marker-vorm
   (`// onopgelost feitcreatie (onopgelost)` + `eval(true)`).
3. Per plek een rood-eerst-getuige in `RolveldlezerWitnessTest` of een
   zusterklasse: eerst rood draaien met de poort weg, dan groen.

## Acceptance

- Beide plekken gepoort, elk met een getuige die zonder de poort rood is.
- Gate 12 groen met `EXPECTED_PLAYED` **ongewijzigd** (de DRL-tak is geen
  gate-12-pad, dus dit moet triviaal gelden — bevestig het, neem het niet aan).
- Gate 13 groen met de DRL-vloer ongewijzigd (nu: 13 PLAYED door de tekst-tak).
- Gate 11 groen (29/29 PLAYED).
- De JUnit-teller stijgt; vloer `EXPECTED_MIN_TESTS` ongemoeid.

May touch: `backends/drools/**`, `backlog/`.

## Out of scope

- De CONSERVATIEVE zusterrol-weigering in `aggregatie` / `toOneAantalReader`
  opheffen: die verschuift gate-12-kanalen en heeft een eigen meting nodig.
- De exec-tak: die stelt beide vragen al.
- `ref/`, `backends/clingo`, `spec/verifier/**`.
