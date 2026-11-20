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

### Commits

| hash | inhoud |
|---|---|
| `472be2315` | ontwerpnotitie |
| `a6bb6204e` | frontend: grammatica, het gedeelde domein (`ObjectType.attributeMarkerDomain`), de markers op `opgegeven X` en `berekende X`, doorgeven en vergelijken in `AbsenceMarkers`; `AbsenceMarkerTest` (+5, één omgedraaid); DIVERGENCES §7, "Afwezigheidsmarkeringen" en "Scenariotoetsen" |
| `5f6ccb2fc` | corpus (rood eerst): golden `jutterswet-opgave-afwezigheidsmarkers`; de verouderde zin in de NOTE van `jutterswet-opgave-per-periode` (bijvangst) |
| `e9e0a946f` | uitvoerders: clingo (schaal 0, datumliteral), Drools (gevolg per dag als kopie, schrijfregel van de wortel), README van Drools, ref-suite (+10 uitgewerkte voorbeelden), clingo-suite (de golden en de fixture `initialisation-under-the-opgave-marker`), DRL-pin van de golden |
| `066e3d48c` | gate 12 (beschermd, voorstel): `EXPECTED_PLAYED` 382 → 383 |
| `ffe36e464` | spec (beschermd, voorstel): RS ch. 9.5, ASP-TRANSLATION S14 |
| `bd4fd3ead` | Result |

De commits van de open punten staan onder "Result van de open punten".

### Wat per laag veranderde

- **Grammatica.** `attribuutSpecificatie` neemt `afwezigheidsmarkering? MAG_OPGEGEVEN_WORDEN?`: de afwezigheidsmarkering
  eerst. `(mag opgegeven worden) (of onbekend)` blijft een syntaxfout (RGS-091). Geen nieuw token.
- **Model en verlaging.** `ObjectType.attributeMarkerDomain` (lid → het attribuut wiens domein het deelt); `sumTypeOf`
  leest het, en de emitter declareert het domein één keer. `RuleChecker.inputDeclarations` geeft `opgegeven X` en
  `berekende X` de markers en het domein van X; zonder schrijver (RGS-153) gaan ze met hun markers weer weg.
  `AbsenceMarkers`: een verwijzing naar een ander lid van het sum type van het doel is een geschreven waarde zoals zij
  is (de twee gevolgen van de groep), en een (on)gelijkheid van twee zulke leden vergelijkt de verwijzingen (de
  afwijking). RGS-376 geldt ook voor een lezing van de waarde van een gegenereerd lid. Scenariolaag en semantic tokens:
  geen codewijziging; `markersOf` van een gegenereerd lid geeft de markers, dus een marker als opgave in een situatie en
  als verwachting op X en `berekende X` werkt (golden, gate 16).
- **ref.** Geen codewijziging; de suite speelt de tien cases als `WV-9.5-opgave-*` (2773 → 2793 voorbeelden). Na de
  open punten speelt ze de vijftien cases van de golden en houdt ref een getal in een andere eenheid als payload in de
  gedeclareerde eenheid (2806 voorbeelden).
- **clingo.** Een scalaire defaults-groep op een doel van een sum type schrijft op schaal 0
  (`translate_defaults_group`); een datumliteral als payload van een `Construct` is zijn dagnummer
  (`construct_ground_term`, bijvangst: was `malformed number lexeme`). Suite 2897 → 2919 checks. Na de open punten
  draagt clingo een berekende payload op regelniveau (zie "Result van de open punten").
- **Drools.** `copyOfOwnTimeline` (gedeeld door `producedTimeline` en `lowerDayOwnRule`): per dag is op een doel van een
  sum type de kopie van een eigen tijdlijn van dat sum type een gevolg, zonder eenheidstoets; `dayRootRule` schrijft op
  zo'n doel `PERIOD_WRITTEN_VALUES`. Scalair speelde al. Suite 1968 tests, 0 skipped (geen testmethode erbij; de
  corpusgetuigen lopen de golden mee); DRL-pin van de golden erbij, geen andere pin veranderde.
- **Corpus.** De golden: `teruggavedatum` (Datum in dagen, beide markers, berekend als de claimdatum plus een
  parameter), `afhaaldatum` (Datum in dagen, niet van toepassing, berekend als de claimdatum plus 7 dagen, geïnitialiseerd
  op 01-01-2026), `bergloon` (geheel getal, onbekend, een samengestelde berekening, geïnitialiseerd op onbekend) en
  `opslagkosten` (geheel getal per maand, onbekend), elk met de opgavemarkering; vijftien cases, met de hand verwacht in
  `scenario.rgs`. Geen negatief en geen rode case: de kaart voegt geen weigering en geen fout toe.

### Rood eerst (letterlijk)

Op de basis `8cd8e387d` (losse worktree, gebouwd; opgeruimd):

- **Frontend** (`logs/red-02-frontend-base.log`) op de bron van de golden: `ERROR (line 3): syntaxfout: extraneous
  input '(mag opgegeven worden)' expecting {<EOF>, UITZONDERING_OP, AFWIJKING_VAN, …}`, en daarna `ERROR (line 28):
  regeldoel 'De opslagkosten van een Vondst': 'opslagkosten' is geen gedeclareerd attribuut van Vondst (WF-022, IR-SPEC
  ch. 3.1)` en drie WARNINGs `onopgelost`.
- **De nieuwe frontendtests** op de basis (`logs/red-03-frontend-tests-base.log`, zonder de regel die het nieuwe veld
  leest): 4 van 21 rood — `the_opgave_and_the_computation_share_the_sum_type_of_the_marked_attribute() ✘ onverwachte
  diagnostiek: [ERROR (line 3): syntaxfout: extraneous input '(mag opgegeven worden)' …]`,
  `on_a_timeline_the_group_passes_the_members_on() ✘ geen groep 'opgave boven berekening opslagkosten'`,
  `without_a_computation_the_members_leave_with_their_markers() ✘ [ERROR (line 2): syntaxfout …]`,
  `a_read_of_the_value_of_the_opgave_needs_the_test() ✘ verwachtte één RGS-376, kreeg: [ERROR (line 3): syntaxfout …]`.
  Groen op de basis: `both_markings_are_keywords` (de tokens zijn ook naast de syntaxfout sleutelwoorden) en de
  omgekeerde volgorde, die al een syntaxfout was.
- **Uitvoerders** op de IR van de golden (`logs/red-01-executors-base.log`): verifier `PASS`; ref `oracle: PASS` op
  alle tien (niet rood: ref droeg het al); clingo op alle tien `TRANSLATION FAULT: unscalable datatype in the ASP
  translation`; Drools exec en DRL-tekst `EXCLUDE DROOLS-303: transpiler SKIP: defaults: defaults A:Vondst.opslagkosten
  — this operand of the period arithmetic of ch. 3.4/3.5 is not executed ('zijn opgegeven opslagkosten'): …`.
- **De datumliteral** (fixture, proef p6, vóór de fix): clingo `TRANSLATION FAULT: malformed number lexeme
  "2026-01-01"`; ref `PASS`, beide Drools-branches `PLAYED 2`.

### Mutaties (niet gecommit; losse worktree, opgeruimd; `logs/mut/mutations.log`, `scripts/mutations.py`)

| mutatie | rood |
|---|---|
| F1 de groep leest de waarde van opgave en berekening (geen doorgeven) | `the_opgave_and_the_computation_share…` (`ERROR (line 3): 'zijn opgegeven teruggavedatum' leest de waarde van 'opgegeven teruggavedatum' … zonder eerst te toetsen`, RGS-376), `on_a_timeline_the_group_passes_the_members_on` (`expected: <…AttributeReference> but was: <…Construct>`), `a_read_of_the_value_of_the_opgave_needs_the_test`; de golden emitteert niet meer |
| F2 de afwijking vergelijkt de payloads (de vergelijking van de melding) | `the_opgave_and_the_computation_share…` (RGS-376 op de lezingen in de voorwaarde van de afwijking), `a_read_of_the_value_of_the_opgave_needs_the_test`; de golden emitteert niet meer |
| F3 `opgegeven X` en `berekende X` krijgen de markers niet | `the_opgave_and_the_computation_share…` (`expected: <SumType[domain=teruggavedatum van een Vondst, …]> but was: …`), `on_a_timeline…`, `a_read_of_the_value_of_the_opgave_needs_the_test` (`verwachtte één RGS-376, kreeg: []`) |
| F4 de teruggetrokken leden houden hun markers | `without_a_computation_the_members_leave_with_their_markers` (`Cannot invoke "java.util.List.iterator()" …`) |
| C1 clingo: de scalaire groep op een sum type op de schaal van een getal | golden case1, case5, case10: `TRANSLATION FAULT: unscalable datatype in the ASP translation` |
| C2 clingo: een datumliteral als getal | fixture: `TRANSLATION FAULT: malformed number lexeme "2026-01-01"` |
| D1 Drools: geen kopie als gevolg per dag | golden op beide branches `EXCLUDE DROOLS-303: … ('zijn opgegeven opslagkosten') …` |
| D2 Drools: de wortel schrijft getallen | golden op beide branches `ERROR ConsequenceException: … "defaults A:Vondst.opslagkosten: eens wortel" … kan geen Breuk maken van …` |

### Codes, pins en vloeren (oud → nieuw)

- Geen RGS-, CLINGO-, DROOLS- of EF-code nieuw, gewijzigd of gepensioneerd; geen catalogusregel veranderde.
- Gate 12 `EXPECTED_PLAYED` 382 → 383 (de golden, nu `PLAYED 15` op beide branches). DRL-pin: één bestand erbij. Geen
  vloer verschoof.
- Test die de kaart omdraait: `AbsenceMarkerTest.a_marker_on_a_characteristic_or_beside_the_opgave_marker_is_a_syntax_error`
  → `…_or_after_the_opgave_marker_is_a_syntax_error`. Oud: `(of onbekend) (mag opgegeven worden)` geeft RGS-091. Nieuw:
  `(mag opgegeven worden) (of onbekend)` geeft RGS-091; de besloten volgorde verlaagt (de nieuwe tests).

### Beschermde paden

- `scripts/check-drools.sh` (`066e3d48c`): `EXPECTED_PLAYED` 383 met de regel die de golden noemt; de golden speelt
  op beide branches.
- `spec/RUNTIME-SEMANTICS.md` (`ffe36e464`): ch. 9.5, de alinea "An opgave above a computation", de zin "Executed" en
  de uitgewerkte voorbeelden `WV-9.5-opgave-*`, die `ref/test` letterlijk speelt.
- `spec/ASP-TRANSLATION.md` (`ffe36e464`): S14, de groep op een doel van een sum type en de datumliteral als payload.

### Voorgestelde tekst (huidige stand)

- **RUNTIME-SEMANTICS** en **ASP-TRANSLATION**: in `ffe36e464`.
- **IR-SPEC**: geen wijziging. WF-061 eist al dat twee sum-typed operanden van één vergelijking één domein noemen, wat
  het gedeelde domein geeft; WF-023 houdt een geschreven verwijzing aan het domein van het doel; ch. 4.7 noemt
  `Construct` en `PayloadAccess` al als de spelling van de frontend.
- **Case-schema**: geen wijziging; een marker als opgave is een constructorwaarde van de bestaande variant.
- **DIVERGENCES**: in `a6bb6204e`.
- **CONSTRUCTS rij 47**, in de kolom v0→v1 delta, na "… an object creation writes a marker into a member as a rule
  writes it.": "Beside the opgave marker (row 8, `frontends/regelspraak/grammar/DIVERGENCES.md` §7) the marking stands
  first, `(of onbekend) (mag opgegeven worden)`: `opgegeven X` and `berekende X` carry X's markers over X's domain, the
  generated group passes the member that decides on whole, a marker included, and the deviation compares the two as
  constructor values (RUNTIME-SEMANTICS ch. 9.3), so a supplied value beside a computed marker, a supplied marker beside
  a computed value and two different markers deviate (decided 2026-11-20)." In de laatste kolom: "goldens
  `jutterswet-afwezigheidsmarkers`, `-cel` and `-creatie` (each with its `scenario.rgs`, gate 16) and `-kopie`" →
  "goldens `jutterswet-afwezigheidsmarkers`, `-cel` and `-creatie` (each with its `scenario.rgs`, gate 16), `-kopie`
  and `jutterswet-opgave-afwezigheidsmarkers` (with its `scenario.rgs`)", en "2026-11-05, card
  `absence-markers-residue`." → "2026-11-05, card `absence-markers-residue`; 2026-11-20, card
  `absence-markers-with-opgave`."
- **CONSTRUCTS rij 8** (de opgavemarkering): geen wijziging nodig; rij 47 noemt de combinatie.
- **CLAUDE.md**: geen wijziging.

### Voorlopige besluiten

- VB-1 (domein): de drie leden delen het ene domein `X van een T`, via `ObjectType.attributeMarkerDomain`; geen domein
  per lid, zodat de groep zonder omzetting doorgeeft en WF-061 de vergelijking toelaat.
- VB-2 (doorgeven): alleen een verwijzing naar een *ander* lid van hetzelfde sum type wordt als verwijzing geschreven;
  binnen het dialect delen alleen de leden van één opgavemarkering een domein, dus elk ander document emitteert wat het
  emitteerde. Een kopie van hetzelfde attribuut van een ander object blijft de payload lezen, zoals vóór de kaart.
- VB-3 (vergelijken): alleen een gelijkheid of ongelijkheid van twee leden van één sum type vergelijkt de waarden met
  marker; een ordening leest de payloads onder RGS-376, zoals elke andere lezing.
- VB-4 (codes): geen nieuwe RGS-code; de omgekeerde volgorde is de gewone syntaxfout.
- VB-5 (golden): de teruggavedatum wordt berekend als de claimdatum plus een parameter (sinds de open punten; clingo
  draagt de berekende payload). De initialisatie staat op twee eigen attributen, de afhaaldatum (een datum) en het
  bergloon (de marker onbekend), zodat de teruggavedatum zonder opgave en zonder berekening leeg blijft. De fixture
  `initialisation-under-the-opgave-marker` is weg; de golden dekt haar.
- VB-6 (bijvangst): de datumliteral als payload in clingo opgelost (een crash met de tekst van de getallezer), en de
  verouderde zin over DROOLS-271 in DIVERGENCES §7 en de NOTE van `jutterswet-opgave-per-periode` naar de huidige stand.
- VB-7 (namen): golden `jutterswet-opgave-afwezigheidsmarkers`, voorbeelden `WV-9.5-opgave-*`, fixture
  `initialisation-under-the-opgave-marker`; identifiers Engels, domeinnamen Nederlands.

### Open punten en vragen

1. **clingo, een samengestelde payload op regelniveau.** Opgelost in deze kaart: zie "Result van de open punten".
2. **De distributie.** `opgegeven X` is op een attribuut met markers nu sum-typed: de dienst moet een marker als opgave
   kunnen ontvangen en een constructorwaarde van `berekende X` kunnen weergeven (toolchain-kaart bij de pin-bump).
3. **Tellingen in DIVERGENCES "Scenariotoetsen".** Opgelost (`4b5f6fe1f`): 47 goldens, geteld 2026-11-20.

### Inertie, gemeten

`git diff 8cd8e387d -- corpus` raakt alleen de nieuwe golden en de NOTE van `jutterswet-opgave-per-periode`, en
`git diff bd4fd3ead -- corpus` alleen de golden zelf: geen bestaande verwachting van een andere golden veranderde, en
van de DRL-pins alleen die van de golden. In de run hieronder (op `85af811e0`): gate 10 `220 rgs golden(s) re-emit
byte-identically` (219 zonder de combinatie en de nieuwe), gate 11 `1 casus dump(s), 41 .rgs re-emitted
byte-identical`, gate 16 `47 scenario golden(s) … regenerate 173 case file(s) … and their ir.json byte-identically`.
Gate 12 `played matches the pinned 383` en `DRL-text branch played 383`; gate 17 `384 goldens and 1070 case files
compared`; gate 15 route leg op alle 220 goldens met een `source.rgs`.

### make gates

Eén run aan het eind in de worktree op `85af811e0`, na de open punten (`out/gates/20261120-124919/`, log
`logs/gates-2.log`); de commit daarna raakt alleen deze kaart. De run van de eerste bouwer op `ffe36e464` staat in
`logs/gates-1.log` (607 s, red=0).

```
start 2026-11-20 12:49:19 85af811e0 card/absence-markers-with-opgave
ocaml rc=0 1s ocaml-build: rebuilt (21 source path(s) newer than the stamp, e.g. backends/clingo/lib/translate.ml; 1s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 3s drools-jar: rebuilt (1 source path(s) newer than the jar, e.g. backends/drools/src/test/java/dev/uchar/regelspraak/cases/AbsenceMarkerWitnessTest.java; 3s)
01-self-contained rc=0 2s     02-ir-schema rc=0 156s    04-case-schema rc=0 9s    03-verifier rc=0 52s
05-oracle rc=0 29s            06-clingo rc=0 583s       07-noop rc=0 25s          08-red rc=0 262s
09-catala rc=0 6s             10-regelspraak rc=0 356s  11-toka-parity rc=0 272s  12-drools rc=0 519s
13-reftype rc=0 553s          14-flogic rc=0 2s         15-graph rc=0 63s         16-scenario rc=0 49s
17-readings rc=0 351s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-20 12:59:05 586s red=0
```

Suites in die run: frontend `1694 tests` (0 skipped), ref `2806 worked examples`, clingo `2945 checks`, Drools
`JUnit 1969 tests` (0 skipped).

## Result van de open punten

*Tweede bouwer, 2026-11-20, vanaf `bd4fd3ead` (opdracht `werk/absence-markers-with-opgave/prompt-fix.md`). Niet gemerged,
niet gepusht. Logs `logs/fix-*` en `logs/mut2/`, proeven `probes/p7` tot en met `p11`, scripts
`scripts/mutations-fix.py` en `scripts/mutations-unit.py` (buiten de repo).*

### Commits

| hash | inhoud |
|---|---|
| `d11f99505` | clingo: een berekende payload van een `Construct` op regelniveau; suite: de lege operanden per operator, de parameter als payload (S19 deur 2 omgedraaid), de weigeringen van een andere schaal en een `Match` |
| `7e4df98bd` | corpus: de golden berekent en initialiseert (vijftien cases); ref-suite (+5 `WV-9.5-opgave-*`), clingo-suite (de golden, de fixture `initialisation-under-the-opgave-marker` weg), DRL-pin, NOTE, DIVERGENCES §7 en "Afwezigheidsmarkeringen", README van Drools |
| `4b5f6fe1f` | DIVERGENCES "Scenariotoetsen": 47 goldens, geteld 2026-11-20 |
| `204d0dfe2` | spec (beschermd, voorstel): RS ch. 9.5 "Executed" en de uitgewerkte voorbeelden; ASP-TRANSLATION S14 en S19 |
| `b5638ffbe` | bijvangst: een payload in een andere eenheid; ref en Drools zetten om naar de gedeclareerde eenheid, clingo weigert bij naam; tests in de drie suites, fixture `payload-in-another-unit` |
| `85af811e0` | spec (beschermd, voorstel): de payload in een andere eenheid in RS ch. 9.5 en ASP-TRANSLATION |
| (deze) | Result van de open punten |

### Wat in clingo veranderde

- **De berekende payload.** De arm van `expr_term` voor een `Construct` met een argument dat geen literal is, vertaalt
  het argument met `expr_term` en vouwt de term in de functor; de atomen van de payload gaan mee in de regel. Zo draagt
  hij elke vorm die `expr_term` al vertaalt: een eigen attribuut (zoals vóór de kaart), een parameter, een datum plus
  een duur (literal of parameter) en een samengestelde getalberekening. De vorige arm voor alleen een eigen attribuut en
  de onderste arm met CLINGO-006 gingen weg. Programma, uit de golden:
  `val(S,"berekende afhaaldatum",waarde(((A5)+(7)))) :- isa(S,"vondst"), val(S,"claimdatum",A5).`
- **Leeg per operator, gemeten** (proef `p8`, nu de fixture `empty-operands-of-a-computed-payload`, vier cases): ref
  telt bij plus, min en maal een lege operand als 0, propageert bij verminderd met de linker en telt de rechter als 0,
  propageert bij een datum plus of min een duur de lege datum en telt de lege duur als 0. Clingo's `expr_term` doet per
  operator hetzelfde (`coalesce0`), dus de `Construct` is precies leeg waar ref de payload leeg leest; geen extra code.
  De vier cases spelen in ref, clingo en beide Drools-branches (`PLAYED 4`).
- **Bij naam geweigerd, CLINGO-006** (`fallback`, sleutel `construct-payload-rule-level`, geen parameters, functie
  `expr_term`, alle drie ongewijzigd in `codes.tsv`): een `Match` als payload; een payload waarvan de term op een andere
  schaal staat dan de gedeclareerde (een som van gehele getallen in een payload van 2 decimalen: ref en beide
  Drools-branches spelen haar, `PLAYED 1`); een payload in een andere eenheid dan de gedeclareerde (bijvangst
  hieronder). `check-exec-codes.sh`: `PASS — 13 REF codes and 48 CLINGO codes`.
- **Deling.** Een deling als payload valt onder de bestaande precisiepoort van WF-062: op een schrijfplaats laat de
  verifier alleen een argument toe waarvan de precisiegrens in de decimalen van de payload past, dus clingo's quotiënt is
  daar exact op de schaal; een term op een andere schaal weigert de schaaltoets.

### Bijvangst: een payload in een andere eenheid (een fout, opgelost)

Gemeten op proef `p11` (een gewicht in kg met de marker onbekend, berekend als "zijn lading", een geheel getal in t;
lading 2 t, verwacht waarde(2000)), vóór de fix in deze worktree: ref `oracle: PASS` maar trace en eindtoestand
`"waarde"(2)`; clingo `MISMATCH: V.gewicht = [waarde(2)], expected waarde(2000)`; Drools `FAIL case1-twee-ton.json:
attribute gewicht: expected waarde(2000), actual waarde(2)`. Dat bestond al vóór de kaart voor een eigen attribuut als
payload. WF-062 leest het argument van een `Construct` als een schrijfactie in het gedeclareerde payload-datatype,
dus: ref (`in_payload_unit`) en Drools (`construct`, met de bestaande `restated` en het deel `write-boundary-target`)
zetten een getal in een andere eenheid om naar de eenheid van de payload in de declaratie van het domein; clingo zet
niet om en weigert bij naam. Daarna: ref en beide branches `"waarde"(2000)` in lezing en eindtoestand, `PLAYED 1`;
clingo `TRANSLATION FAULT: CLINGO-006: a Construct payload in another unit than the declared payload's is not restated
(ref restates it into the declared unit; no corpus case)`. Een getal zonder eenheid laat ref ongemoeid (geen verschil
in een bestaande golden).

### Rood eerst (letterlijk)

- **Op `bd4fd3ead`**, de golden in wording (proef `p7`, vijftien cases; `logs/fix-01-red-p7-base.log`): ref `oracle:
  PASS` op alle vijftien (de handverwachtingen kloppen); clingo op alle vijftien `TRANSLATION FAULT: CLINGO-006: a
  Construct payload other than a literal or an own attribute read is not translated at rule level (a parameter read, a
  Match or a composite payload has no corpus case)`; Drools exec en DRL-tekst `PLAYED 15`.
- **De clingo-suite** na de wijziging, vóór de aanpassing van de test (`logs/fix-02-clingo-suite.log`): `FAIL S19 via 2
  (rule-level Construct with a parameter payload): expected a loud fault, got a model` — de test die de kaart omdraait
  (hieronder).
- **De eenheid**: zie de bijvangst hierboven.

### Mutaties (niet gecommit; losse worktree `mut2`, opgeruimd; `logs/mut2/mutations.log`)

| mutatie | rood |
|---|---|
| K1 de arm draagt alleen een eigen attribuut, de rest CLINGO-006 (de stand vóór de kaart) | clingo-suite 20 FAIL, o.a. alle vijftien cases van de golden (`clingo pins the values (fault: mutation K1)`), deur 2 en de vier lege-operandcases; runner op de golden `TRANSLATION FAULT: CLINGO-006` |
| K2 geen schaaltoets | `FAIL absence markers: a computed payload at another scale than the declared one is refused: expected a refusal, got a program` |
| K3 geen weigering van een `Match` als payload | `FAIL absence markers: a Match as payload is refused: expected a refusal, got a program` |
| K4 de term zonder de atomen van de payload | clingo-suite 32 FAIL (`clingo exited 65`, onveilige variabelen), o.a. de golden en `jutterswet-afwezigheidsmarkers` |
| K5 een datumliteral als payload als getal gelezen | alle vijftien cases van de golden `fault: malformed number lexeme "2026-01-01"`: de golden dekt de initialisatie met een datum van de verwijderde fixture |
| K6 een lege payload telt als 0 | clingo-suite 18 FAIL, o.a. `jutterswet-afwezigheidsmarkers` case4 (`V1.taxatie = [waarde(0)], expected leeg`) en de golden; runner op de golden: case12 en case15 `MISMATCH: V.berekende … = [waarde(0)], expected leeg`, case1 en case2 EF-016 (de lege berekening schrijft naast de marker) |
| U1 ref zet een payload niet om | ref-suite `FAIL WV-9.5-waarde-in-de-gedeclareerde-eenheid: the payload restated into kg` |
| U2 Drools zet een payload niet om | `AbsenceMarkerWitnessTest.a_payload_in_another_unit_is_restated_into_the_declared_one`: `exec-model — case.json ==> expected: <null> but was: <case: attribuut gewicht: verwacht waarde(2000), werkelijk waarde(2)>` |
| U3 clingo weigert een payload in een andere eenheid niet | runner op de fixture die de suite weigert: `MISMATCH: V.gewicht = [waarde(2)], expected waarde(2000)` |

### Codes, pins en vloeren (oud → nieuw)

- Geen code nieuw, gewijzigd of gepensioneerd. CLINGO-006 blijft, met twee plaatsen in `expr_term` in plaats van één
  en drie zinnen (`Match`, schaal, eenheid) in plaats van de ene over "a parameter read, a Match or a composite
  payload"; de regel in `codes.tsv` is gelijk.
- Test die de kaart omdraait: clingo-suite "S19 via 2 (rule-level Construct with a parameter payload)". Oud: een
  `Construct` met een parameter als payload faalt luid met de CLINGO-006-zin. Nieuw: "S19 door 2: a rule-level
  Construct with a parameter payload is carried", ref en clingo spelen `meldpositie` = Openstaand(30); de case pint die
  waarde nu (eerst alleen `consistent`).
- Weg: de fixture `ref/test/fixtures/absence-markers/initialisation-under-the-opgave-marker` en haar test in de
  clingo-suite; de golden dekt beide vormen (case12 de datum, case15 de marker; mutatie K5).
- Erbij: de fixtures `empty-operands-of-a-computed-payload`, `payload-at-another-scale`, `match-as-payload` en
  `payload-in-another-unit` onder `ref/test/fixtures/absence-markers/`.
- DRL-pin: alleen `jutterswet-opgave-afwezigheidsmarkers.drl` opnieuw geschreven. `EXPECTED_PLAYED` blijft 383 (de
  golden speelt `PLAYED 15`). Geen vloer verschoof; `scripts/` niet aangeraakt.
- Suites: ref 2793 → 2806 voorbeelden, clingo 2919 → 2945 checks, Drools 1968 → 1969 tests (0 skipped), frontend
  ongewijzigd (1694 tests, 0 skipped).

### Beschermde paden

- `spec/RUNTIME-SEMANTICS.md` (`204d0dfe2`, `85af811e0`): ch. 9.5 de zin "Executed" (een berekende waarde, de
  initialisatie als laagste default, de waarde in de gedeclareerde eenheid, wat de ASP-backend bij naam weigert) en de
  uitgewerkte voorbeelden van de opgave (`-marker-boven-datum` en `-gelijke-datum` naar de termijn van 14 dagen, vijf
  nieuwe: `WV-9.5-opgave-berekende-afhaaldatum`, `-afhaaldatum-zonder-claim`, `-marker-boven-initialisatie`,
  `-berekend-bergloon`, `-bergloon-zonder-gewicht`) en `WV-9.5-waarde-in-de-gedeclareerde-eenheid`; `ref/test` speelt
  ze letterlijk. De alinea "An opgave above a computation" is niet aangeraakt.
- `spec/ASP-TRANSLATION.md` (`204d0dfe2`, `85af811e0`): S14 "A computed payload at rule level" (huidige stand) en in
  S19 punt (i) en de eerste weigering naar de huidige stand.

### Voorgestelde rijtekst

- **CONSTRUCTS rij 47**, laatste kolom, na "the readings of ch. 5.12)": "; a value computed at rule level (a date plus
  a duration, an arithmetic of numbers) and an initialisation below the opgave, a number in another unit held in the
  declared one; the ASP backend refuses by name a computed value at another scale or in another unit than the declared
  payload and a `Match` as the value (CLINGO-006)". En "2026-11-20, card `absence-markers-with-opgave`." blijft zoals in
  het voorstel hierboven.

### Voorlopige besluiten

- VB-8 (golden): één golden, geen tweede: de afhaaldatum en het bergloon zijn eigen attributen, dus de cases van de
  teruggavedatum en de opslagkosten veranderen alleen in de berekende datum (de termijn van 14 dagen). Elk nieuw case
  legt één ding vast; de parameter staat in een parameterset die elk scenario noemt.
- VB-9 (lege payload): het bergloon is "zijn gewicht verminderd met 2 maal zijn schade", zodat een leeg gewicht de
  berekening leeg laat en de initialisatie onbekend beslist (case15); met plus of maal bovenaan zou de berekening 0
  geven en de initialisatie nooit bereiken.
- VB-10 (eenheid): ref en Drools zetten een payload om naar de gedeclareerde eenheid, gelezen uit de declaratie van het
  domein (ch. 2.10), alleen bij een getal met een eenheid; clingo weigert. Omzetten in clingo (een exacte factor, zoals de
  vergelijking doet) kan later; geen getuige vraagt het nu.
- VB-11 (weigeringen): een `Match` als payload blijft geweigerd, zoals de opdracht zegt; met de arm weg (K3) maakt
  clingo er een programma van, ongemeten of het juist antwoordt.

### Open punten

1. **De distributie** (open punt 2 hierboven) blijft.
2. **De schaal van een berekende payload in clingo.** Een som van gehele getallen in een payload van 2 decimalen speelt
   in ref en Drools en weigert clingo bij naam (DIVERGENCES "Afwezigheidsmarkeringen" noemt het). Opschalen met een
   exacte factor is een kleine uitbreiding als een regelset het vraagt.
3. **Ref houdt een payload als berekend, niet ingepast in de decimalen van de payload.** Op een schrijfplaats houdt
   WF-062 dat binnen de decimalen; in een voorwaarde niet. Gemeten is het niet.

### make gates

Eén run op `85af811e0`, groen: `end 2026-11-20 12:59:05 586s red=0` (zie "make gates" hierboven, `logs/gates-2.log`).
