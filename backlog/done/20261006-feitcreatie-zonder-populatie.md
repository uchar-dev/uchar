# feitcreatie-zonder-populatie: een feitcreatie schrijft geen populatie, een rolcheck leest zijn relatie, check weigert ook een kring door een objectcreatie, en wat het frontend schrijft hangt niet aan de bronvolgorde

*Ronde zonder kaart, opdracht van de hoofdsessie van 2026-10-06; de eigenaar
koos de volledige ronde en gaf akkoord voor spec, verifier, frontend, tests en
de beschermde paden die dit raakt. Tijdens de ronde scherpte de eigenaar het
bronvolgordepunt aan: dat de uitkomst van de volgorde van regels of bestanden
afhangt, is niet aanvaardbaar, en het hoort in deze ronde. Gebouwd vanaf uchar
`d07b9f93` op de tak `fix/feitcreatie-populatie`. Sluit open punt 3(b) van
`20261002-ronde-klein-2.md`. Commits: `19b57ae5` (getuigen, rood), `e70bd58b`
(verifier), `b6667aad` (frontend: graaf en de grens voor creatiekringen),
`22835197` (frontend: bronvolgorde), `794a5c12` (getuige WF-001, rood),
`2e328d4e` (frontend: WF-001), `224b7ebe` (spec), `18a52dd8` (DIVERGENCES en
parity-zone), `40b408e7` (poort 12). Het voorafgaande onderzoek staat onder
`/tmp/kringonderzoek/`, de metingen van deze ronde onder `/tmp/feitmeting/`
(`units.txt`, `doclist.txt`, `caselist.txt`, de varianten `v0`, `v1`, `v2` met
`tool/strata.ml`, `out-v*/`, `replay-v*/`, `docs/` en `docs-na/`,
`perm/perm.py` met `perm-main*.txt` en `perm-na.txt`, `randen-fe/` en
`randen-v/`, `gates/summary.txt`). Geen Keuring in dit record; die is van de
eigenaar.*

## Aanleiding

RUNTIME-SEMANTICS ch. 6.1 liet een relatiecreatie (feitcreatie) de populatie
van het type in haar doelrol schrijven, naast de relatie die zij maakt. IR-SPEC
ch. 5.1 noemde dat "the conservative proxy": een feitcreatie maakt geen
instantie. Sinds L2 noemt elke telling, vouw en verdeling haar relatie (B12), dus
de proxy ordende niets meer wat de relatierand niet ordende. Hij tekende wel een
kring die niet bestaat. In de TOKA-casus telt de objectcreatie `vastgestelde
contingent treinmiles` de passagiers van een vlucht (elementtype Natuurlijk
persoon) en koppelt de feitcreatie `passagier met recht op treinmiles` personen
aan het gemaakte contingent: proxyrand toka-29 → toka-28 over `pop:natuurlijk
persoon`, naast de echte rand toka-28 → toka-29 over `rel:reis met contingent
treinmiles`. De verifier en ref weigerden de casus onder WF-045; de ASP-backend
vond één model, en Drools (beide takken) en ref zonder proxy gaven hetzelfde. Om
die casus liet `check` sinds ronde klein 2 elke kring door een objectcreatie
buiten een recursieve groep door (open punt 3(b)).

## Stap 1: het risico van de rolcheck, gemeten vóór de wijziging

Een rolcheck (`RoleMembership`) las in de analyse niets: zijn relatie gold als
invoer (RS ch. 6.1, "Scope narrowing, recorded"). Het risico: een telling,
verdeling of objectcreatie die de populatie van een type leest en een rolcheck
draagt op de relatie die een feitcreatie maakt, stond misschien alleen dankzij
de proxy na die feitcreatie.

Gemeten met drie bouwsels van verifier, ref en clingo (`v0` = main, `v1` = zonder
proxy, `v2` = zonder proxy en met de rolcheck die zijn relatie leest) en een
meettool die de strata rekent zoals ref:

- 1171 IR-documenten: alle `ir.json` van `corpus/` en `toka/`, de 462 eenheden
  van de vorige meting door het frontend van main (goldens met en zonder
  `scenario.rgs`, de TOKA-casus en haar fragmenten, `examples/` en het
  toka-manifest van de distributie, de werkbank v1 `basis`, `dagloon-ww`,
  `herleving-ww`, `verrekening-ww`, v2 `fundament`, `herleving-ww` met hun
  scenario's, `presentaties/demo-govtech4all`), en de bronselectie van
  `TokaBron`.
- Geen enkel document heeft een rolcheck op een relatie die een feitcreatie
  schrijft; ook geen op een relatie die een objectcreatie (terugkoppeling)
  schrijft. In de bronnen van de werkbank en de distributie staat buiten de
  TOKA-casus geen enkele feitcreatie (gezocht op de zin van §13.4.7, ook over
  regelgrenzen). De werkbank v1 `dagloon-ww` schrijft geen IR (twee ERRORs
  `1/dg`); zijn bronnen hebben geen feitcreatie.
- Verifier: `v1` en `v2` laten alleen de WF-045 van de TOKA-casus vallen
  (`toka/casus/ir.json` en de `TokaBron`-selectie). 24 documenten verliezen
  proxyranden. `v2` is op alle 1171 documenten gelijk aan `v1`, in randen,
  strata en oordeel.
- Strata: alleen `jutterswet-dubbelverbinding` (twee regels van stratum 1 naar
  0) en de TOKA-casus (de verdelingen van 0 naar 1, omdat de kring weg is)
  verschuiven.
- Replay: 1186 cases (corpus, `toka/`, de scenario's van de eenheden) door ref
  en clingo in `v0`, `v1` en `v2`: byte-gelijk op de naam van een tijdelijk
  bestand na. Drools leest de graaf van dep_forms niet (zijn stratificatie leest
  alleen kenmerken, `transpiler/Stratificatie`), dus geen uitkomst daar
  verandert.

Keuze: het raakt niets, en de precieze lezing is klein (één arm in de verifier,
één in het frontend), dus zij is opgenomen. De rolcheck leest `rel:<factType>`,
positief; ontkend of onder een ontkennende kwantor negatief, zoals een
kenmerkcheck, omdat "hij is geen …" op afwezigheid houdt en een latere koppeling
haar onwaar maakt. Dat volgt uit de definitie van een negatieve rand in RS
ch. 6.1. Een ontkende rolcheck op de relatie die een recursieve creatie zelf
legt, wordt daarmee een WF-046-schending (negatief hieronder).

## Wat landde

- **Verifier** (`spec/verifier/lib/checks.ml`, `dep_forms` en `dep_cond`): een
  feitcreatie schrijft alleen `rel:<factType>`; de arm `Role_membership` leest
  `rel:<fact_type>`. ref volgt via dep_forms.
- **Frontend** (`resolver/Afhankelijkheden`): dezelfde twee wijzigingen;
  `doelvuller` gaat mee weg. `RuleChecker.keurAfhankelijkheidskringen`: de
  uitzondering voor een kring door een objectcreatie is weg (3(b)). De randen
  van `Afhankelijkheden` zijn gelijk aan `verifier --dependency-edges` op 1173
  documenten (2920 randen, 144 over een relatie).
- **Bronvolgorde** (`resolver/RuleChecker`, `resolver/DefinitionResolver`): zie
  de volgende sectie.
- **Spec**: RS ch. 6.1 (de relatieknoop, de rij van de relatiecreatie, de
  negatieve rand, de alinea "Relation reads" met de geschiedenis en de meting),
  ch. 6.4 (WV-6.4-h en het negatief); IR-SPEC ch. 5.1 (de relatieknoop, de
  populatieschrijving), 5.2 en WF-046 (de ontkende rolcheck, met golden en
  negatief in de tabel) en de Recorded-alinea van 5.5 (de grens is weg).
- **Corpus**: golden `jutterswet-premiepot` (`bron.rgs`, `scenario.rgs`, één
  case, met de hand gerekend in `NOTE.md`: een objectcreatie telt de
  havenjutters, een feitcreatie koppelt ze aan de gemaakte premiepot, een
  rolcheck leest de koppeling); negatief
  `wf046-ontkende-rolcheck-recursieve-creatie` (`expect.txt` pint de arm van de
  WF-046-regel). ref: WV-6.4-h.
- **Tekst**: `grammar/DIVERGENCES.md` ("Kringen: WF-045 en WF-051 in check" en de
  nieuwe sectie "Bronvolgorde"), `scripts/check-toka-parity.sh` en
  `toka/README.md` (14 meldingen in plaats van 15), `scripts/check-drools.sh`
  (pins 240 en 194, met de reden).
- **Tests**: `resolver/FeitcreatieRandenTest` (nieuw), `BronvolgordeTest`
  (nieuw), een rij in `AfhankelijkheidskringTest`; de rijen van `CreatieRolTest`
  ("werf een wachter", "werf een maat") en `SetlezingZonderVouwTest` ("nieuwe
  vondst") leggen nu de weigering vast, letterlijk:

      objectcreatie 'werf een wachter' hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een recursieve creatie (WF-045): dependency cycle through no recursive creation: werf een wachter -> werf een wachter

  `AantalGevuldeDialectTest` gaf het model van een andere test een kenmerk via
  een tweede kop van het objecttype; dat kenmerk staat nu in een extensie.

## Bronvolgorde

De regels worden een voor een opgelost, in bronvolgorde. Gezocht naar plekken
waar een regel leest wat een eerdere regel in het model zette: een leesronde
door Copilot (gpt-6-sol, alleen lezen) over `RuleChecker`, `ExpressieBouwer`,
`DefinitionResolver`, `ScenarioResolver` en `Resolver`, en een meting
(`perm/perm.py`): 368 bronnen en bronketens uit de lijst van eenheden, elk in de
oorspronkelijke volgorde, met de bestanden omgekeerd, en met de blokken van elk
niet-scenariobestand herschikt (P: declaraties, regels omgekeerd, dan de
uitzonderings- en groepsblokken; Q: die blokken, regels omgekeerd, dan de
declaraties); vergeleken op exitcode, diagnoses (zonder regelnummers), IR
(zonder regelnummers, elke lijst gesorteerd, en apart "alleen volgorde" als de
IR gelijk is met elke lijst gesorteerd) en cases.

Opgelost, elk met een getuige die eerst rood was (`BronvolgordeTest`):

1. **`aangemaaktNaar`** (`RuleChecker.tussenstap`): een feitcreatie die door de
   terugkoppeling van een objectcreatie loopt, telde alleen een objectcreatie
   die eerder in de bron stond; erboven viel zij weg met de WARNING
   "feitcreatie wordt nog niet naar IR/DRL omgezet … referentieveld-zijde voor
   tussenstap … niet af te leiden". Gemeten op de TOKA-casus: de bestanden
   omgekeerd gaven die WARNING (uchar `toka/casus`, `tc-toka-alle`,
   `tc-toka-fundament+alle`). Nu leest een pas vóór de regels de objectcreaties
   van het hele document af (`aangemaakteObjecten`). De Jutterswet-getuige:
   `jutterswet-premiepot` in twee volgordes geeft hetzelfde document byte voor
   byte, en langs de scenarioroute dezelfde cases.
2. **Het startpuntanker** (`RuleChecker.startpunt`): het lid `startpunt
   <tijdlijn>` werd pas gedeclareerd als de startpuntbepaling aan de beurt was;
   een regel erboven die het anker leest, werd een marker. Nu declareert een pas
   vóór de regels het (`startpuntAnkers`).
3. **Twee declaraties van één naam** (`DefinitionResolver`): twee koppen van één
   objecttype, of twee parameters, domeinen, eenheidsystemen, feittypen of
   dimensies van één naam, nam het frontend stil samen, de laatste won, en de IR
   droeg één declaratie, zodat de verifier WF-001 niet zag; twee dagsoorten van
   één naam lieten de emitter vastlopen. Nu weigert `weigerDubbeleNamen` de
   tweede in de zin van de verifier (WF-001). Tijdlijnen ziet de verifier al
   (WF-015).

Na de fix: de 462 eenheden door het frontend van main en van de tak geven
dezelfde exitcode, uitvoer, IR en cases; de `TokaBron`-selectie geeft dezelfde
IR, en met de bestanden omgekeerd dezelfde IR op de volgorde van de lijsten na.
De herschikking (`perm-na.txt`): geen enkel document verschilt meer behalve
in de volgorde van een lijst, op twee soorten na die geen inhoud zijn:

- Blokken die de regels erna groeperen (`Regelgroep`, `Uitzonderingen
  ongeordend`) uit elkaar halen, is syntaxis: de parser weigert of de groep
  verliest leden (`jutterswet-nestkenmerk-rgs`, `-opgave-vormen`,
  `-opvolgerversies`, `-vocabulaires`, `-wachtgeld`, `-wachtersketen-rgs`).
- Welke vorm een weigering eerst noemt: de WF-045-wandeling noemt de eerste
  kring die zij tegenkomt (`jutterkring`: "Jutter.uitgesloten -> …" of
  "Jutter.penninggerechtigd -> …"); de verifier doet hetzelfde.

Gevonden en niet veranderd (zie Open punten): de volgorde binnen een lijst; welke
declaratie of regel een melding over een botsing als eerste noemt; de brontekst
van de regel van een gegenereerd lid bij twee vouwen over dezelfde stappen; de
volgorde van `Uitzondering op`-blokken, die bewust de rangorde bepaalt (§4); en,
vermoedelijk, `subjectVan` bij een enkelvoudige consistentieregel, dat de
uitdrukking bouwt vóór `zetVariabelen` de variabelen van de vorige regel wist.

## Metingen na

- **TOKA-casus**: `toka/casus/ir.json` 15 → 14 meldingen (weg: `WF-045 FAIL
  /rules/objectCreations/0/versions/0: dependency cycle through no recursive
  creation: vastgestelde contingent treinmiles -> passagier met recht op
  treinmiles -> vastgestelde contingent treinmiles`; blijven: 3 × WF-026,
  8 × WF-034, 1 × WF-022, 1 × WF-079, 1 × WF-099). De `TokaBron`-selectie 8 → 7.
  `check` op de `TokaBron`-selectie: exit 0 zonder de grens, en met de
  bestanden in elke volgorde.
- **Rood vóór**: op `d07b9f93` falen 8 frontendtests en WV-6.4-h, weigert de
  verifier de golden (WF-045) en laat hij het negatief door; op `22835197` faalt
  de WF-001-rij van `BronvolgordeTest`.
- **Suites**: frontend 1165 tests, 0 failures (was 1158); ref 1154 uitgewerkte
  voorbeelden (was 1152); backend 1094, 0 failures.

## Poorten

De zestien in de volgorde van `gates.yml`, `dune build` vooraf, op `40b408e7`
(de laatste commit buiten `backlog/`; de commit met dit record raakt alleen
`backlog/`), zeventien keer `exit 0` en `DONE 15:32:18`:

     1. check-self-contained: PASS
     2. check-ir-schema: PASS (… 335 file(s) validate and round-trip, floor 192; 44085 object(s) in 840 document(s) in the schema's key order …)
     3. check-verifier: PASS (335 golden clean; 505 negatief rejected for their declared rule, floor 350; 348 on their declared arm (expect.txt), floor 260; 611 cited pointers resolve …)
     4. check-case-schema: PASS (… 843 case file(s) validate)
     5. check-oracle: PASS (ref suite 1154 worked examples, floor 489; 801 cases replayed exactly, floor 427 …)
     6. check-clingo: PASS (backend suite 1988 checks, floor 83; 801 cases replay exactly through clingo …)
     7. check-noop: PASS (757 cases refute a do-nothing executor; 44 pinned survivors …)
     8. check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26 …)
     9. check-catala: PASS (241 checks, floor 201; 12 catala golden(s) …)
    10. check-regelspraak: PASS (JUnit suite green (1165 tests, floor 173); 122 rgs golden(s) re-emit byte-identically, floor 80)
    11. check-toka-parity: PASS (ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)
    12. check-drools: PASS (played 240, pin 240; DRL-text branch played 194, pin 194; JUnit 1094 tests, floor 239)
    13. check-reftype-derivation: PASS (43 unit annotations twisted over 24 goldens, floor 38; 122 executor replays …)
    14. check-flogic: PASS (150 checks, floor 81; 2 flogic golden(s) …)
    15. check-graaf: PASS (spec leg 258 of 295 goldens compared, 196 dependencies equal …; route leg 122 goldens, 68 unit tests)
    16. check-scenario: PASS (29 scenario golden(s) regenerate 100 case file(s) …)

## Voorgestelde registertekst (CONSTRUCTS rij 6)

In de Landed-alinea van 2026-10-02 (ronde klein 2) de bijzin "except a cycle its
own warning names on a rule's own subject and a cycle through an object creation
outside a recursive group (both pinned by existing frontend tests)" vervangen
door: "since 2026-10-06 without exception: a cycle on a rule's own subject is
refused as well, naming WF-046 too where a negative edge lies between two forms
of the cycle (negatief `wf046-kenmerken-lezen-elkaars-ontkenning`), and so is a
cycle through an object creation outside a recursive group, once the fact
creation's population proxy, which drew a false cycle in the casus of the parity
zone, left the graph (golden `jutterswet-premiepot`); since the same day a role
check reads the relation it names, negated as a negative edge (negatief
`wf046-ontkende-rolcheck-recursieve-creatie`)". Dit vervangt ook het voorstel van
`20261006-kringtoets-volgt-wf045.md`. Besluit gevraagd in kaart
`20261006-besluit-registertekst-constructs-rij-6.md`.

## Open punten

1. Open punt 4 van ronde klein 2 blijft en wordt groter: een negatieve rand op
   een kring door een recursieve creatie gaat door `check`, nu ook een ontkende
   rolcheck op de relatie die de creatie legt (het nieuwe negatief schrijft het
   frontend zonder melding; de verifier en ref weigeren het onder WF-046).
   Kaart: `20261006-check-wf046-door-recursieve-creatie.md`.
2. Bronvolgorde, niet veranderd: (a) welke vorm of declaratie een weigering als
   eerste noemt (de WF-045-wandeling, de botsing van twee dagsoortdefinities of
   regelnamen, de keuze van de eigen basis in de hersteladviezen van de
   opgavelowering); de weigering zelf staat in elke volgorde. (b) De regel van
   een gegenereerd lid neemt de brontekst van de eerste vouw die het lid
   genereert; bij twee vouwen over dezelfde stappen met een anders geschreven
   lezing verschilt alleen die `source`. (c) De rangorde van `Uitzondering
   op`-blokken volgt de bron, bewust (§4). (d) `subjectVan` bouwt bij een
   enkelvoudige consistentieregel haar uitdrukking vóór `zetVariabelen`, dus met
   de variabelen en het onderwerp van de vorige regel; geen geaccepteerde zin
   gevonden die daardoor een ander document krijgt, niet gemeten.
   Kaart: `20261006-bronvolgorde-restpunten.md`, punten (a)–(d).
3. De distributie noemt in `RegelSpraakEngine.Session.fire` en `Uitvoerder` nog
   "de resolver-WARNING" als bron van een kring zonder fixpunt (open punt 3 van
   `20261006-kringtoets-volgt-wf045.md`); ongewijzigd. Kaart:
   `20261006-bronvolgorde-restpunten.md`, punt (e).
4. De pin volgt in de distributie en de werkbank. **Gebeurd op 2026-10-06**:
   regelspraak-toolchain `f9fc44c` pint uchar `0a378588`, de werkbank `e504fda`
   pint de toolchain op `f9fc44c`.
