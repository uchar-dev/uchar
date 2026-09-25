# dialect-opgave-boven-berekening: de markering `(mag opgegeven worden)` op een attribuut (vorm 3, suiker)

Bouwkaart uit de kaart kenmerkinvoer-en-invoer-boven-berekening (optie
7-iii: voorstel zonder bouw, hoofdsessie 2026-09-25). Bron:
`spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §5. Na de
kaart dialect-uitzondering-op-uitzondering (zelfde resolverbestanden, dus
serieel). Of zij vóór of ná de pin-bump van regelspraak-java komt, is het
sein van de maintainer.

Een werkbankgebruiker wil een afgeleid attribuut als invoer kunnen opgeven
en de afleiding laten wijken ("opgegeven, niet afgeleid"; Reading B),
zonder dat casusinvoer als geheel van Reading A afwijkt (RUNTIME-SEMANTICS
ch. 7.8; PHASE4-DEFAULTS §10 besluit 1). Het hardheidspatroon
(PHASE4-DEFAULTS §4.1) doet dat vandaag als programmastructuur, het
Catala-frontend genereert het voor `context` (PHASE5-CATALA besluit 6).
Deze kaart geeft het RegelSpraak-dialect er een declaratie voor. Geen
wire-, spec-, verifier- of uitvoerderswijziging: de lowering hieronder is
met de hand op de L5-wire gemeten en speelt in ref, clingo en de
Drools-exectak (voorstel §5.4, de lowering- en de initialisatieproef:
dertien cases, elk met een omgekeerde tegencase).

## Ontwerp (voorstel §5.1–5.6)

- Spelling: `(mag opgegeven worden)` als laatste element van een
  attribuutdeclaratie, vóór de `;`:

      de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

  Eén nieuw lexertoken `MAG_OPGEGEVEN_WORDEN : '(mag' WSP 'opgegeven' WSP
  'worden)'`, geheel met haakjes zoals `BEZIELD`, `BEZITTELIJK` en
  `BIJVOEGLIJK`; de grammatica laat het ook toe aan het eind van een
  kenmerkdeclaratie, zodat de resolver daar gericht weigert. Het token
  reserveert geen naamwoord.
- Lowering voor een gemarkeerd attribuut X van objecttype T:
  1. attribuut `opgegeven X` op T, datatype van X; geen schrijver, de
     casus levert het;
  2. attribuut `berekende X` op T, datatype van X; elke rekenende
     schrijver die de auteur voor X schreef (gelijkstelling, berekening,
     beslistabelrij, en de defaults-groepen van `Uitzondering op` en
     `Uitzonderingen ongeordend`) krijgt `berekende X` als doel; naam en
     vorm blijven, een ongeordende set verhuist als set;
  3. één `PrioritizedDefaults` op X met naam `opgave boven berekening X`,
     van hoog naar laag: de opgave-default (rechtvaardiging de lege
     `Compound` met `quantifier: "all"` en `source.snippet` "altijd",
     consequentie `opgegeven X`, `source.snippet` `opgegeven X`), de
     rekendefault (idem met `berekende X`) en, als de auteur een
     initialisatie voor X schreef, die initialisatie als
     `initialisation`-default onderaan: zij blijft op X;
  4. bezittelijk kenmerk `afwijkende opgave X` op T, toegekend door de
     regel `afwijking opgave X` onder `Compound` `all` van: `opgegeven X`
     gevuld, `berekende X` gevuld, `opgegeven X` ongelijk aan
     `berekende X`;
  5. consistentie `controle opgave X` met als assertie het kenmerk
     `afwijkende opgave X`, ontkend.
  Lezers van X elders blijven X lezen.
- Uitleg: de beslissende default draagt zijn bloknaam in `source.snippet`
  (`opgegeven X` of `berekende X`); `berekende X` houdt de berekende
  waarde vast, ook als de opgave beslist.
- Tegenspraak: opgave en berekening beide gevuld en ongelijk geeft één
  §9.5-feit `controle opgave X` × onderwerp plus het kenmerk; een opgave
  zonder berekenbare waarde is geen tegenspraak.
- Casusinvoer op X zelf blijft Reading A (ch. 7.8 ongewijzigd; voorstel
  §5.4 cases 6 en 7).

## Weigeringen (ERROR op de regel van de declaratie) en één waarschuwing (voorstel §5.7)

- de markering op een kenmerk (de positieve richting geldt zonder
  markering, ch. 7.8; de negatieve vraagt de driewaardige opgave van
  voorstel §5.8, eigen kaart);
- de markering op een attribuut dat geen Numeriek zonder eenheid is (de
  corpus draagt als attribuut-defaultsdoel alleen Numeriek, 28 van 28,
  geen met eenheid); een ander datatype of een eenheid pas met een eigen
  golden die in de drie uitvoerders speelt;
- de markering op een tijdsafhankelijk attribuut (WF-086) of op een
  gedimensioneerd attribuut;
- een verdeling of objectcreatie-initialisatie die X schrijft;
- een schrijver van X op een super- of subtype (ch. 7.10);
- een gegenereerde naam die het model al declareert of gebruikt, met beide
  plekken in de melding.
- WARNING: de markering op een attribuut dat geen regel schrijft (al
  invoer; er wordt niets gegenereerd).

## Acceptatie (bij oppakken aanscherpen)

- (a) Inertheid gemeten, niet aangenomen (de methode van `DIVERGENCES.md`
  §6): elk vlak RegelSpraak-document dat de werkbank bereikt, door het
  frontend vóór en na het token, IR plus diagnostics vergeleken; alle
  `bron.rgs`-goldens byte-identiek via gate 10.
- (b) Rood-eerst JUnit-getuigen: de vijf stukken van de lowering (namen,
  volgorde, `source.snippet`), elke weigering en de waarschuwing, met de
  weigerzinnen letterlijk.
- (c) Een golden met `bron.rgs` (werknaam
  `jutterswet-opgave-boven-berekening`, eigen Jutterswet-casus) met de
  cases van voorstel §5.4 (geen opgave, gelijk, afwijkend met het
  §9.5-feit, opgave zonder feiten, niets, X zelf in de invoer met en zonder
  feiten, initialisatie), groen in ref ≡ clingo ≡ Drools-exec. Gate 12:
  `EXPECTED_PLAYED` +1 en de golden in klasse (A) van `DRL_CHANNEL` (de
  DRL-teksttak heeft geen defaults-lowering); gate 7: de cases die een
  niets-doende uitvoerder haalt met reden op de overleverslijst. Beide
  scripts beschermd: **voorstel** op de branch.
- (d) `DIVERGENCES.md`: een eigen sectie voor de markering (spelling,
  lowering, weigeringen, de inertheidsmeting).
- (e) De registerzin voor `spec/CONSTRUCTS.md` in het Result (het dialect
  krijgt een oppervlak voor het hardheidspatroon; geen wire-delta); de
  hoofdsessie landt hem.
- (f) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/**`,
`scripts/check-drools.sh` en `scripts/check-noop.sh` (**voorstel**),
kaartverplaatsing.

Niet in deze kaart: de markering op een kenmerk (voorstel §5.8); de
semantische variant (voorstel §5.9); de uitleg in regelspraak-java en de
werkbank (kaart regelspraak-java-defaults-in-scenario-en-werkbank, gat 3);
het Catala-frontend (voorstel §5.10); de `atLeast`-arm in de
consistentielowering van de Drools-exectak (voorstel §5.6, alternatief d).

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Bouw naar `spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §5.1–5.7 en deze kaart (de namen `opgegeven X`, `berekende X`, `afwijkende opgave X`, groep `opgave boven berekening X`, regel `afwijking opgave X`, consistentie `controle opgave X`; de initialisatie blijft op X; alleen Numeriek zonder eenheid). Elke afwijking meld je met reden in het Result.
- U2 Parallel bouwt de kaart frontend-emitterrestanten aan `IrEmitter.java` (builtin-typen, `DimensionSelection`, `connectingRole`, de meldingstaal). Jij raakt de grammatica (het token), de resolver (de lowering) en gebruikt de bestaande emissiepaden; nieuwe emitterlogica alleen als de lowering die nodig heeft, en dan gemeld. De hoofdsessie rebaset serieel.
- U3 Golden `corpus/golden/jutterswet-opgave-boven-berekening/` met `bron.rgs`, de emissie als `ir.json` (gate 10 byte-identiek, gate 2 round-trip), de cases van voorstel §5.4 handgerekend (ref ≡ clingo ≡ Drools-exec), NOTE; gate-12-pins `EXPECTED_PLAYED` +1 en klasse (A) van `DRL_CHANNEL` (**voorstel**); gate-7-overlevers met reden (**voorstel**).
- U4 Inertheid gemeten: alle rgs-goldens byte-identiek (gate 10), toka byte-identiek (gate 11), en de sweep van DIVERGENCES §6 over de repo-bronnen (toka alleen tellen).
- U5 DIVERGENCES.md: eigen sectie; de registerzin voor CONSTRUCTS (het dialect krijgt een oppervlak voor het hardheidspatroon, geen wire-delta) in het Result; geen D-/U-nummers in DIVERGENCES, NOTE's, javadoc en scripts.
- U6 Gates 1–8, 10, 11, 12, 13.
