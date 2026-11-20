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
| (deze) | Result |

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
- **ref.** Geen codewijziging; de suite speelt de tien cases als `WV-9.5-opgave-*` (2773 → 2793 voorbeelden).
- **clingo.** Een scalaire defaults-groep op een doel van een sum type schrijft op schaal 0
  (`translate_defaults_group`); een datumliteral als payload van een `Construct` is zijn dagnummer
  (`construct_ground_term`, bijvangst: was `malformed number lexeme`). Suite 2897 → 2919 checks.
- **Drools.** `copyOfOwnTimeline` (gedeeld door `producedTimeline` en `lowerDayOwnRule`): per dag is op een doel van een
  sum type de kopie van een eigen tijdlijn van dat sum type een gevolg, zonder eenheidstoets; `dayRootRule` schrijft op
  zo'n doel `PERIOD_WRITTEN_VALUES`. Scalair speelde al. Suite 1968 tests, 0 skipped (geen testmethode erbij; de
  corpusgetuigen lopen de golden mee); DRL-pin van de golden erbij, geen andere pin veranderde.
- **Corpus.** De golden: `teruggavedatum` (Datum in dagen, beide markers) en `opslagkosten` (geheel getal per maand,
  onbekend), elk met de opgavemarkering; tien cases, met de hand verwacht in `scenario.rgs`. Geen negatief en geen rode
  case: de kaart voegt geen weigering en geen fout toe.

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
- Gate 12 `EXPECTED_PLAYED` 382 → 383 (de golden, `PLAYED 10` op beide branches). DRL-pin: één bestand erbij. Geen
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
- VB-5 (golden): X wordt berekend uit een eigen attribuut ("zijn claimdatum"); een samengestelde payload weigert clingo
  ook zonder de opgavemarkering (open punt 1). Geen initialisatie in de golden, omdat de case "zonder opgave en zonder
  berekening leeg" anders niet kan; de initialisatie met een marker en met een datum speelt in de fixture
  `initialisation-under-the-opgave-marker` (ref en clingo in de clingo-suite; Drools gemeten op p6, `PLAYED 2`).
- VB-6 (bijvangst): de datumliteral als payload in clingo opgelost (een crash met de tekst van de getallezer), en de
  verouderde zin over DROOLS-271 in DIVERGENCES §7 en de NOTE van `jutterswet-opgave-per-periode` naar de huidige stand.
- VB-7 (namen): golden `jutterswet-opgave-afwezigheidsmarkers`, voorbeelden `WV-9.5-opgave-*`, fixture
  `initialisation-under-the-opgave-marker`; identifiers Engels, domeinnamen Nederlands.

### Open punten en vragen

1. **clingo, een samengestelde payload op regelniveau** (CLINGO-006, een `fallback`-code): "De teruggavedatum … moet
   berekend worden als de claimdatum plus 3 dagen" op een datum met markers, met of zonder opgavemarkering. Ref en beide
   Drools-branches spelen het (proef p2: `PLAYED 8`). De herlevingsdatum van de werkbank wordt waarschijnlijk berekend;
   een golden die dat vastlegt, vraagt deze vorm in clingo. Een eigen kaart, of meenemen bij de werkbankkaart?
2. **De distributie.** `opgegeven X` is op een attribuut met markers nu sum-typed: de dienst moet een marker als opgave
   kunnen ontvangen en een constructorwaarde van `berekende X` kunnen weergeven (toolchain-kaart bij de pin-bump).
3. **Tellingen in DIVERGENCES "Scenariotoetsen"** ("41 goldens … counted 2026-11-05") zijn ouder dan deze kaart en niet
   bijgewerkt; de golden staat in de lijst van getuigen.

### Inertie, gemeten

`git diff 8cd8e387d -- corpus` raakt alleen de nieuwe golden en de NOTE van `jutterswet-opgave-per-periode`: geen
bestaande verwachting veranderde. In de run hieronder: gate 10 `220 rgs golden(s) re-emit byte-identically` (219 zonder
de combinatie en de nieuwe), gate 11 `1 casus dump(s), 41 .rgs re-emitted byte-identical`, gate 16 `47 scenario
golden(s) … regenerate 168 case file(s) … and their ir.json byte-identically`. Gate 12 `played matches the pinned 383`
en `DRL-text branch played 383`; gate 17 `384 goldens and 1065 case files compared`; gate 15 route leg op alle 220
goldens met een `source.rgs`.

### make gates

Eén run aan het eind in de worktree op `ffe36e464` (`out/gates/20261120-115024/`, log `logs/gates-1.log`); de commit
daarna raakt alleen deze kaart.

```
start 2026-11-20 11:50:24 ffe36e464 card/absence-markers-with-opgave
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 510 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 2s drools-jar: rebuilt (389 source path(s) newer than the jar, e.g. backends/drools/src/test/resources/drl-text; 2s)
01-self-contained rc=0 2s     02-ir-schema rc=0 148s    04-case-schema rc=0 9s    03-verifier rc=0 47s
05-oracle rc=0 28s            06-clingo rc=0 604s       07-noop rc=0 23s          08-red rc=0 264s
09-catala rc=0 6s             10-regelspraak rc=0 362s  11-toka-parity rc=0 284s  12-drools rc=0 538s
13-reftype rc=0 570s          14-flogic rc=0 2s         15-graph rc=0 59s         16-scenario rc=0 51s
17-readings rc=0 354s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-20 12:00:31 607s red=0
```

Suites in die run: frontend `1694 tests` (0 skipped), ref `2793 worked examples`, clingo `2919 checks`, Drools
`JUnit 1968 tests`.
