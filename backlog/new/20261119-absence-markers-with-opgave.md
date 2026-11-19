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
