# absence-markers-with-opgave: een afwezigheidsmarkering naast de opgavemarkering op één attribuut

*Opgesteld door een werkbanksessie op 2026-11-19, op verzoek van de eigenaar, na de evalronde van s04 (herleving-ww). Volgt
op de kaart `absence-markers` (2026-11-04).*

## Aanleiding

De regelset v2/herleving-ww kan "het recht herleeft niet" alleen als afwezigheid uitdrukken: het kenmerk `indicatie
herleving recht op uitkering` ontbreekt en `de herlevingsdatum` van de RechtOpUitkeringPeriode blijft leeg. Een toetsdocument
ziet dan geen verschil met een herleving waarover niets beslist kon worden omdat invoer ontbrak. Gemeten op 2026-11-19 met
`bin/regelspraak scenario` op het scenariodocument van eval-scenario s04: in de stel-dat van s04-v03 (bedrijf beëindigd na
de herkrijgingstermijn) slagen `R1 heeft geen indicatie herleving recht op uitkering` en `de herlevingsdatum van P1 is
leeg`, en zonder de einddatum van de eindigingsgrond (de omzetting vergat de bedrijfsbeëindiging) slagen ze allebei ook.
Een vergeten invoer blijft zo onopgemerkt.

De markering `(of niet van toepassing)` van `absence-markers` maakt "geen herlevingsdatum, en dat staat vast" schrijfbaar:
een regel schrijft de marker als de herleving is uitgesloten, en een verwachting `de herlevingsdatum van P1 is niet van
toepassing` faalt dan terecht als de invoer ontbreekt (leeg). Een kenmerk kan geen markering dragen, dus het moet de datum
zijn. Die datum draagt al de opgavemarkering:

    de herlevingsdatum (cgm: Datum herleving recht op uitkering) (handboek: dag van herleving) Datum in dagen (mag opgegeven worden);

(regelspraak-rules `v2/fundament/rules/fnd-03-persoon-en-recht.rgs` r. 52; een medewerker kan een herlevingsdatum
opgeven, die boven de berekening gaat). `frontends/regelspraak/grammar/DIVERGENCES.md`, "Afwezigheidsmarkeringen": "an
attribute carries one of the two or neither, so `(of onbekend) (mag opgegeven worden)` is a syntax error".

## Goal

Een attribuut kan de opgavemarkering en een afwezigheidsmarkering samen dragen, met elk van de drie vormen van de marker,
voor elk datatype waarop beide nu afzonderlijk mogen.

- **Bron (dialect):** beide markeringen in één declaratie, in een vaste volgorde, bijvoorbeeld `(mag opgegeven worden) (of
  niet van toepassing)`. De precieze spelling en volgorde stelt de bouwer voor; de hoofdsessie besluit. DIVERGENCES §7 en
  het hoofdstuk Afwezigheidsmarkeringen vullen elkaar aan.
- **Verlaging:** de opgave verlaagt naar een opgegeven en een berekende waarde met de opgave erboven, en een melding als de
  opgave afwijkt (DIVERGENCES §7); met markers dragen die drie het somtype van het attribuut, zodat de berekening een
  marker kan schrijven en de uitkomst haar doorgeeft waar geen opgave is. De bouwer meet en stelt voor, zonder een tweede
  afwezigheid in te voeren.
- **Semantiek (RS als voorstel):** een marker is een gevulde waarde (RS ch. 9.5). Een opgave gaat boven de berekening,
  ook boven een berekende marker.

Normvragen voor de eigenaar, elk met een aanbeveling van de bouwer:

1. Mag een opgave zelf een marker zijn ("de opgegeven herlevingsdatum is niet van toepassing")?
2. Is een opgave met een datum tegenover een berekende marker een afwijkende opgave (de melding van §7), en omgekeerd?

## Besluiten vóór de bouw (2026-11-20)

Van de eigenaar, op de twee normvragen hierboven:

- Een opgave mag zelf een marker zijn, voor elke marker die de declaratie van het attribuut toelaat: `opgegeven X` draagt
  dezelfde markers als X.
- Een opgave die verschilt van een gevulde berekening is een afwijkende opgave, ook waar een van de twee een marker is:
  een opgegeven datum tegenover een berekende marker, een opgegeven marker tegenover een berekende datum, en twee
  verschillende markers. Twee gelijke markers zijn gelijk. De reden: een marker is een gevulde waarde (RS ch. 9.5), dus
  de voorwaarde van §7 (beide gevuld en ongelijk) beslist het zonder extra regel. Een opgave naast een lege berekening
  geeft geen melding, zoals nu.

Van de hoofdsessie, voorlopig, over de spelling in de bron:

- De afwezigheidsmarkering staat vóór de opgavemarkering, in die ene volgorde: `Datum in dagen (of niet van toepassing)
  (mag opgegeven worden)`, en na een tijdlijn `… voor elke maand (of onbekend) (mag opgegeven worden)`. De marker hoort
  bij de waarden die het attribuut kan dragen; de opgavemarkering zegt daarna dat die waarde, een marker inbegrepen, mag
  worden opgegeven. De omgekeerde volgorde blijft een syntaxfout.

## Acceptance

- (a) Rood eerst: een golden met `source.rgs` en `scenario.rgs` en een attribuut met beide markeringen, scalair en per
  periode, met cases waarin de berekening een marker schrijft en de uitkomst haar draagt, een opgave een berekende marker
  verdringt, een opgave een berekend getal verdringt zoals nu, en zonder opgave en zonder berekening het attribuut leeg
  blijft; en een negatief voor elke weigering die erbij komt.
- (b) ref, clingo en Drools (beide branches) spelen de golden exact; een uitvoerder die een vorm weigert, weigert haar bij
  naam.
- (c) Geen bestaande verwachting verandert; een document zonder de combinatie emitteert wat het nu emitteert (gates 10
  en 11). Meet het.
- (d) Voorgestelde tekst (huidige stand) voor RS, IR-SPEC, het case-schema en DIVERGENCES.
- (e) `make gates` groen.

May touch: `frontends/regelspraak/`, `ref/`, `backends/clingo/`, `backends/drools/**`, `corpus/`, kaartverplaatsing.
Protected (de hoofdsessie landt ze): `spec/`, `scripts/`.

Out of scope: de dienst van de distributie (een toolchain-kaart, daarna een pin-bump in de werkbank); de wijziging in
regelspraak-rules (een regel die `niet van toepassing` schrijft als een uitzondering de herleving uitsluit) en de
verwachtingen van de eval-dataset (de musts "kan niet herleven" in s02-v02, s03-v03 en s04-v03); die volgen na de pin.

## Omvang

Middel.

## Result

*Bouwer, 2026-11-20. Niet gemerged, niet gepusht. Logs onder `werk/absence-markers-with-opgave/logs/`, proeven onder
`werk/absence-markers-with-opgave/probes/`, scripts onder `werk/absence-markers-with-opgave/scripts/` (buiten de repo).*

### Ontwerpnotitie (gemeten vóór de bouw)

Gemeten met een proef in de frontend (de grammatica in de besloten volgorde en de verlaging hieronder, nog niet
gecommit) op `probes/p1` tot en met `p6`: de IR door de verifier, ref, clingo en beide Drools-branches, en de lezingen
van ref tegen die van beide branches (`scripts/readings.py`).

1. **Eén sum type voor de drie leden.** X, `opgegeven X` en `berekende X` dragen één sum type over het domein
   `X van een T`: de constructoren `waarde` (X's datatype als payload) en de markers van X. Het domein staat één keer
   in `domains`. `opgegeven X` draagt zo dezelfde markers als X (besluit van de eigenaar). WF-061 laat het toe: één
   domein, dus een vergelijking van twee leden is nominaal goed. Verifier `PASS` op p1 tot en met p6.
2. **Hoe er geschreven wordt.** Een verplaatste schrijver schrijft `berekende X` zoals hij X schreef: een waarde v als
   `Construct waarde(v)`, een marker als de `Construct` van die marker (hetzelfde sum type). De twee gegenereerde
   defaults hebben als gevolg de verwijzing naar `opgegeven X` en `berekende X` zelf, met het sum type: geen
   `Construct` en geen `PayloadAccess`, zodat de groep een waarde of een marker ongewijzigd doorgeeft en er geen tweede
   afwezigheid ontstaat (een `PayloadAccess` zou een marker leeg maken en de groep laten doorvallen). Een
   initialisatie van X is de laagste default met `Construct waarde(v)` of de `Construct` van de marker (p6). In
   `AbsenceMarkers`: een verwijzing naar een ander lid van hetzelfde sum type is een geschreven waarde zoals zij is;
   alleen de leden van één opgavemarkering delen een domein, dus elk ander document blijft gelijk.
3. **De voorwaarde van `afwijkende opgave X`.** `opgegeven X` gevuld en `berekende X` gevuld, op de verwijzingen (een
   marker is gevuld, RS ch. 9.5), en `opgegeven X` ongelijk aan `berekende X`, op de verwijzingen, dus op de waarden
   met marker. RS ch. 9.3 definieert de ongelijkheid van twee constructorwaarden van één domein (gelijk alleen bij
   gelijke tag en gelijke payload; één lege kant geeft waar, twee lege onwaar), dus de vergelijking vraagt geen
   spec-delta. Ref, clingo (`sum_term`, `neq_helper`) en beide Drools-branches dragen haar zonder wijziging.
4. **Per periode.** De stukken 1 tot en met 3 zoals nu: de groep per dag, met als gevolgen de kopie van een eigen
   tijdlijn van het sum type (RS ch. 9.4); geen kenmerk, geen consistentie, geen melding.

Gemeten per uitvoerder, op de IR van de proef:

- ref: alles speelt zonder wijziging (p5: tien cases `oracle: PASS`).
- clingo: (a) een defaults-groep op een scalair doel van een sum type geeft `TRANSLATION FAULT: unscalable datatype
  in the ASP translation` (de doelschaal in `translate_defaults_group`); per periode speelt het zonder wijziging (p4).
  (b) `Construct waarde(v)` met een samengestelde payload op regelniveau ("de vinddag van de Vondst plus 3 dagen")
  geeft CLINGO-006, ook zonder opgavemarkering. (c) Een datumliteral als payload ("… geïnitialiseerd worden op
  01-01-2026" op een datum met markers) geeft `TRANSLATION FAULT: malformed number lexeme "2026-01-01"`, ook zonder
  opgavemarkering.
- Drools: scalair `PLAYED 8` op beide branches zonder wijziging (p2); per periode `EXCLUDE DROOLS-303` (het gevolg
  van een default per dag moet een getal zijn), en met die weigering open een `ConsequenceException` in de
  schrijfregel van de wortel (`PeriodArithmetic.written` op een `ConstructorValue`).
- Lezingen (gate 17): op p3 tot en met p6 per case gelijk aan ref, op beide branches.

Wat de bouw dus vraagt: in de frontend de grammatica, het gedeelde domein, het doorgeven en het vergelijken; in clingo
schaal 0 voor een doel van een sum type in een scalaire groep, en (c) als bijvangst; in Drools het gevolg per dag als
kopie op een doel van een sum type en de schrijfregel van de wortel met `PERIOD_WRITTEN_VALUES`. De golden berekent X
uit een eigen attribuut ("zijn claimdatum"); (b) blijft een open punt, want het geldt ook zonder de opgavemarkering.
