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

## Result (2026-09-25, bouwer)

## Result — dialect-opgave-boven-berekening

### Ronde 5 (lensfix) — B3, B4, A1, A9 (2026-09-25)

**Kop: e748fc67** op branch `fix/lens-opgave` (worktree (worktree opgave)), vanaf `card/k7-unie` 36750511. Werkboom schoon; niets gepusht of gemerged.

| Commit | Vondst | Inhoud |
|---|---|---|
| 67c66181 | B3 | `RuleChecker.initialisatieFout`: een initialisatie met voorwaarde onder de markering is een ERROR op de declaratieregel; getuige in `OpgaveMarkeringTest`; DIVERGENCES §7 |
| da0a30a3 | B4 | `RuleChecker`: tweede kringpas `keurKringverwijzingenNaOpgave` over de gelowerde vormen; drie getuigen en een niet-kringgetuige in `ZelflezingKringTest`; DIVERGENCES §7 |
| a1ba8f10 | A1 | DIVERGENCES §7 en de javadoc van `keurKringverwijzingen`: "filter or test of a fold" en de kring over super- en subtype als niet-gespiegelde rand |
| e748fc67 | A9 | golden `corpus/golden/jutterswet-opgave-vormen` (bron.rgs, ir.json, 15 cases, NOTE), `scripts/check-drools.sh` (**voorstel**), DIVERGENCES §7 (goldenregel) |

Gewijzigde bestanden: `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/RuleChecker.java`, `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/OpgaveMarkeringTest.java` (+1 test), `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/ZelflezingKringTest.java` (+4 tests, één javadocalinea), `frontends/regelspraak/grammar/DIVERGENCES.md` (alleen §7), `corpus/golden/jutterswet-opgave-vormen/**` (nieuw), `scripts/check-drools.sh` (**voorstel**). Buiten de regio's van de emitter-fixer gebleven (niet aangeraakt: `Typering.java`, `ExpressieBouwer.java`, `IrEmitter.java`, RuleChecker r. ~3321, de DIVERGENCES-lijst "Producer refusals without a reader sentence"). `scripts/check-noop.sh` hoeft niet te bewegen (zie A9).

Proeven en metingen buiten de repo: (buiten de repo) (vóór-build van de unie in `voor/`, sweep in `sweep/`, tegencases in `golden/flip/`).

#### B3 — de voorwaarde van een initialisatie viel stil weg

**Vóór (unie, proef van de lens `w/i1.rgs`: bergloon gemarkeerd, `bergloon naar gewicht` = gewicht maal 2 indien gevuld, initialisatie 25,00 `indien zijn soort gelijk is aan "wrakhout"`; case soort "zeewier", geen gewicht, verwacht bergloon leeg):**
- frontend: 0 diagnosen, IR geschreven; verifier: `verifier: PASS`
- ref: `MISMATCH: V.bergloon = 25, expected leeg`
- clingo: `MISMATCH: V.bergloon = [2500], expected leeg`
- Drools-exec: `FAIL i1-case-zeewier.json: verwacht leeg, werkelijk 25`
- zonder markering (`w/i1-plain.rgs`): ref `oracle: PASS (2 expectations)`, Drools `PLAYED 1` (clingo: zie restpunt 3).

**Na:** `ERROR (line 4): attribuut 'bergloon' van Vondst: '(mag opgegeven worden)', en de initialisatie 'standaardbergloon' van 'bergloon' heeft een voorwaarde — de initialisatiedefault van de gegenereerde defaults-groep is alleen gerechtvaardigd waar 'bergloon' in de invoer leeg is, en draagt geen eigen voorwaarde (WF-050) (dialect §7)`; exit 1, geen IR geschreven. De ongemarkeerde vorm is byte-gelijk aan vóór.

**Definitieve weigerzin (patroon):** `attribuut '<X>' van <T>: '(mag opgegeven worden)', en de initialisatie '<naam>' van '<X>' heeft een voorwaarde — de initialisatiedefault van de gegenereerde defaults-groep is alleen gerechtvaardigd waar '<X>' in de invoer leeg is, en draagt geen eigen voorwaarde (WF-050) (dialect §7)` — ERROR op de regel van de gemarkeerde declaratie; volgorde in `initialisatieFout`: meer dan één initialisatie, dan venster/versies, dan voorwaarde. Frontend-eigen zin: de wire kent de markering niet, er verandert geen verifier-, lader- of Drools-zin.

**Keuze en reden:** ERROR, geen lowering. De voorwaarde kan niet zonder kunstgreep mee conform voorstel §5: (a) WF-050 laat geen geschreven rechtvaardiging op een initialisatiedefault toe (`the initialisation default carries no written justification (its justification is input-emptiness, ch. 7.3)`); (b) "X leeg in de invoer" is in een rechtvaardiging niet te spellen — een lezing van X leest de opgeloste waarde en sluit een kring door de groep (WF-051); (c) de initialisatie met voorwaarde naar `berekende X` verplaatsen verandert de betekenis — haar rechtvaardiging zou dan altijd gelden, zodat zij een invoerwaarde van X verdringt en als berekening telt waar een opgave van afwijkt (voorstel §5.3 punt 3 en de initialisatieproef van §5.4).

**Getuige (rood-eerst):** `OpgaveMarkeringTest.een_initialisatie_met_een_voorwaarde_weigert` — tegen de unie-code: `verwachtte precies één diagnose, kreeg: [] ==> expected: <1> but was: <0>`; na de fix groen. DIVERGENCES §7, lijst "Fail-fast refusals", kreeg het punt met de meting.

#### B4 — een kring door een gegenereerd lid kreeg geen melding

**Ontwerp:** de bestaande pas (`keurKringverwijzingen`, vóór de defaults-lowering, in de namen van de auteur) blijft ongewijzigd. Waar `opgaveLowering` een markering heeft gelowerd, loopt daarna `keurKringverwijzingenNaOpgave` over de gelowerde vormen: alle regels (de verlegde schrijvers op `berekende X`, de gegenereerde regel op `afwijkende opgave X`) en alle defaults-groepen (rechtvaardiging en consequentie van elke default, door geneste uitzonderingsgroepen heen, zoals de verifier ze vouwt; de initialisatiedefault leest niets). Ontdubbeling: de verzameling gemelde knopen loopt over beide passen door; in de tweede pas telt `berekende X` als X, zodat een kring die de eerste pas al meldde (X leest X, na lowering `berekende X` → X → `berekende X`) geen tweede melding krijgt. Een document zonder gelowerde markering bereikt de tweede pas niet: byte-identiek per constructie.

**Waarom niet de ene pas verplaatsen:** de bestaande pin `ZelflezingKringTest.een_zelflezing_op_een_gemarkeerd_attribuut_wordt_gemeld_in_de_namen_van_de_auteur` eist precies één melding `Vondst.waarde -> Vondst.waarde`; na de lowering zou dezelfde kring `Vondst.berekende waarde -> Vondst.waarde -> …` heten, en de §4-families zijn vóór de lowering regels en daarna groepen (andere volgorde van de knopen, dus mogelijk andere meldingstekst voor bestaande kringen).

**Metingen, vóór (unie) → na** (frontend; verifier op dezelfde emissie, die gelijk blijft):

| Proef (lens, `k/`) | Frontend vóór | Frontend na | Verifier |
|---|---|---|---|
| g1 (waarde = berekende waarde plus 1) | 0 diagnosen | `kringverwijzing tussen berekeningen: Vondst.berekende waarde -> Vondst.berekende waarde — herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)` | `WF-045 FAIL /rules/rules/0/versions/0: dependency cycle through no recursive creation: waardering -> waardering` |
| g2 (voorwaarde `hij geen afwijkende opgave waarde heeft`) | 0 diagnosen | `kringverwijzing tussen berekeningen: Vondst.berekende waarde -> Vondst.afwijkende opgave waarde -> Vondst.berekende waarde — …` | `WF-045 … waardering -> afwijking opgave waarde -> waardering` en twee keer WF-046 |
| g5 (waarde = premie plus 1; premie = berekende waarde) | 0 diagnosen | `kringverwijzing tussen berekeningen: Vondst.berekende waarde -> Vondst.premie -> Vondst.berekende waarde — …` | `WF-045 … waardering -> premieregel -> waardering` |
| g4 (waarde = waarde plus 1) | 1 WARNING `Vondst.waarde -> Vondst.waarde` | ongewijzigd, één melding | WF-045, WF-046, WF-051 |
| g3 (leest `opgegeven waarde`, geen kring) | 0 | 0 | `verifier: PASS` |
| eigen proef g2 met positieve lezing (`indien hij afwijkende opgave waarde heeft`) | — | dezelfde melding als g2 | WF-045 en één keer WF-046 (`afwijking opgave waarde reads what waardering produces …`) |

Alle meldingen WARNING, regel 0, document wordt geschreven (zoals elke kring). In g1/g2/g5 is de IR op de diagnostiek na gelijk aan vóór. De overige kringproeven van de lens (k1–k5, n1, n3, n4, n6, n7, erf) zijn vóór en na byte-identiek; n2 en n5 weigeren vóór en na gelijk (exit 1).

**Getuigen (rood-eerst, tegen de unie-code gemeten: drie falen met `expected: <1> but was: <0>`, de niet-kringgetuige slaagt al):** `ZelflezingKringTest.een_berekening_die_haar_eigen_berekende_waarde_leest_wordt_gemeld`, `een_voorwaarde_op_het_gegenereerde_kenmerk_sluit_een_kring`, `een_kring_via_een_ander_attribuut_door_de_berekende_waarde_wordt_gemeld`, `een_lezing_van_de_opgave_of_van_het_doel_zonder_kring_meldt_niets`. De bestaande pin in de namen van de auteur blijft groen.

**Geen valse positieven, gemeten** (vóór = unie-build uit `git archive card/k7-unie`, na = e748fc67; IR, diagnostiek en uitvoer per document byte-vergeleken): 89 van 89 corpusbronnen van de unie identiek; 47 toka-bronnen en de casusdump zoals gate 11 haar samenstelt (45 bronnen) identiek, en de casusdump na == de gepinde `toka/casus/ir.json`; kringmeldingen vóór 0, na 0. Van de 89 draagt alleen `jutterswet-opgave-boven-berekening` de markering; die doorloopt de tweede pas zonder melding. De nieuwe golden (de 90e) krijgt `diagnostics: []`.

#### A1 — vouw en erflijn in DIVERGENCES en javadoc

- DIVERGENCES §7, vóór: `A navigated read, a read inside a fold, a role check and a channel test are no edges of it; a cycle only they close is refused by the verifier and not warned by the frontend.`
- Na: `A navigated read, a read in the filter or test of a fold, a role check and a channel test are no edges of it; a read in a fold's argument is one, as in the verifier. And a node belongs to the type the rule is written on, where the verifier lets a write reach every comparable type of the inheritance line, so a cycle over a supertype and a subtype is no cycle of the frontend's graph: measured 2026-09-25 on `x` of a Klok computed from its `y` and `y` of a Ding, its supertype, computed from its `x`, the verifier gives `WF-045 … zet x -> zet y -> zet x` and the frontend says nothing. A cycle only these close is refused by the verifier and not warned by the frontend.`
- Javadoc `keurKringverwijzingen`, de opsomming buiten de graaf eindigt nu op: `…, the forms that are no rule (an allocation, a creation, a consistency), and a cycle over a supertype and a subtype — a node belongs to the type the rule is written on, where the verifier lets a write reach every comparable type of the inheritance line (WF-045).`
- Gemeten (proeven van de lens): n6 (vouwargument over het eigen type) frontend `Ding.y -> Ding.x -> Ding.y`, verifier `WF-045 … som y -> zet x -> som y`; erf frontend 0 diagnosen, verifier `WF-045 FAIL /rules/rules/0/versions/0: dependency cycle through no recursive creation: zet x -> zet y -> zet x`.

#### A9 — golden `corpus/golden/jutterswet-opgave-vormen`

Eigen casus "Art. 3e (opgaven bij vondst, wacht en aanspoelsel)" (tekst in NOTE). Drie gemarkeerde attributen met verschillende namen (de gegenereerde vormnamen dragen geen typenaam): `waarde` van Vondst met een boom (`waardering`, `Uitzondering op waardering: lage taxatie`, en genest `Uitzondering op lage taxatie: museale vondst`) plus de lezer `premie` = waarde maal 2; `wachtgeld` van Jutter met `Uitzonderingen ongeordend:` (kustwacht 30, havenmeester 45); `stukprijs` van Aanspoelsel met een beslistabel (zeewier 2,50, wrakhout 7,00). `ir.json` = frontend-emissie, `diagnostics: []`, `verifier: PASS`. Op de wire: groep `waardering` op `berekende waarde` (keten lage taxatie met genest `ExceptionGroup` [museale vondst], dan de basis), twee `unordered: true`-groepen op `berekende wachtgeld`, de rijregels `stukprijs van een aanspoelsel 1`/`2` op `berekende stukprijs`, per X de gegenereerde groep, regel, kenmerk en consistentie.

| Case | Invoer | Verwacht (handgerekend, absoluut) |
|---|---|---|
| case1-boom-basis | taxatie 40.00 | berekende waarde 35.00; waarde 35.00; premie 70.00; afwijkende opgave waarde afwezig; consistent |
| case2-boom-uitzondering | taxatie 8.00 | berekende waarde 1.00; waarde 1.00; premie 2.00; consistent |
| case3-boom-geneste-uitzondering | taxatie 8.00, museaal | berekende waarde 100.00; waarde 100.00; premie 200.00; consistent |
| case4-boom-geneste-uitzondering-buiten-haar-drager | taxatie 40.00, museaal | berekende waarde 35.00; waarde 35.00; consistent |
| case5-boom-opgave-wijkt-af | taxatie 40.00, opgegeven waarde 50.00 | waarde 50.00; berekende waarde 35.00; kenmerk aanwezig; premie 100.00; inconsistent `controle opgave waarde` × V |
| case6-boom-opgave-gelijk-aan-uitzondering | taxatie 8.00, opgegeven waarde 1.00 | waarde 1.00; berekende waarde 1.00; kenmerk afwezig; premie 2.00; consistent |
| case7-blok-een-lid | kustwacht | berekende wachtgeld 30; wachtgeld 30; kenmerk afwezig; consistent |
| case8-blok-opgave-wijkt-af | kustwacht, opgegeven wachtgeld 50 | wachtgeld 50; berekende wachtgeld 30; kenmerk aanwezig; inconsistent `controle opgave wachtgeld` × J |
| case9-blok-conflict | kustwacht, havenmeester | berekende wachtgeld leeg; wachtgeld leeg; kenmerk afwezig; inconsistent `Jutter.berekende wachtgeld` × J |
| case10-blok-conflict-met-opgave | idem, opgegeven wachtgeld 40 | wachtgeld 40; berekende wachtgeld leeg; kenmerk afwezig; inconsistent `Jutter.berekende wachtgeld` × J |
| case11-blok-doel-verdrongen | havenmeester, wachtgeld 10 | wachtgeld 45; berekende wachtgeld 45; consistent |
| case12-tabel-rij | soort "zeewier" | berekende stukprijs 2.50; stukprijs 2.50; consistent |
| case13-tabel-opgave-gelijk | soort "wrakhout", opgegeven stukprijs 7.00 | stukprijs 7.00; berekende stukprijs 7.00; kenmerk afwezig; consistent |
| case14-tabel-opgave-wijkt-af | soort "wrakhout", opgegeven stukprijs 8.00 | stukprijs 8.00; berekende stukprijs 7.00; kenmerk aanwezig; inconsistent `controle opgave stukprijs` × A |
| case15-tabel-opgave-zonder-rij | soort "barnsteen", opgegeven stukprijs 3.00 | stukprijs 3.00; berekende stukprijs leeg; kenmerk afwezig; consistent |

**Replay (echte uitvoer):** ref 15 × `oracle: PASS (…)` (case1: `oracle: PASS (5 expectations)`); clingo 15 × `clingo: PASS (…)` (case1: `clingo: PASS (5 expectations)`); Drools-exec `PLAYED 15`; DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`. **Zestien tegencases** (buiten de repo, (buiten de repo), elk één verwachting omgedraaid) falen in alle drie, bv. case9 met het conflict onder `Jutter.wachtgeld`: ref `MISMATCH: expected inconsistent (a channel fact from rule "Jutter.wachtgeld" on "J"), but the channel carries: Jutter.berekende wachtgeld × J`, clingo `… but the model carries: jutter.berekende wachtgeld × J`, Drools `FAIL …: verwacht inconsistent (Jutter.wachtgeld @ J), niet gevonden onder Jutter.berekende wachtgeld @ J`. **Gate 7:** alle 15 cases geven onder `--noop` exit 1 (weerleggen een niets-doende uitvoerder): geen overlever, `scripts/check-noop.sh` ongewijzigd.

**Gewijzigde scriptregels `scripts/check-drools.sh` (beschermd: VOORSTEL):**
- `EXPECTED_PLAYED=204` → `EXPECTED_PLAYED=205`, met redenblok `# 204 -> 205 (2026-09-25, card dialect-opgave-boven-berekening, lens round): jutterswet-opgave-vormen, new, from bron.rgs and hand-computed — …` (the exec branch plays its fifteen cases on the first run, PLAYED 15; the DRL-text branch refuses it on its defaults sentence, so EXPECTED_DRL_PLAYED does not move). `EXPECTED_DRL_PLAYED=177` onveranderd.
- `DRL_CHANNEL` klasse (A), in naamvolgorde na `jutterswet-opgave-boven-berekening`: `"jutterswet-opgave-vormen|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"`.
- Kop klasse (A): `32 lines` → `33 lines`; `sitting 27 below` → `sitting 28 below`; `31 of the 32 lines` → `32 of the 33 lines`; `the 32nd, jutterswet-havenrooster` → `the 33rd, …`; `30 of those 31 ARE` → `31 of those 32 ARE`; `read as 31 exec-only players` → `read as 32 …`; `The 27 is 31 exec-only players — those 30 and` → `The 28 is 32 exec-only players — those 31 and`; hertelling erbij: `Recounted again the same day (card dialect-opgave-boven-berekening, lens round): jutterswet-opgave-vormen joins the class, 32 -> 33 lines and 27 -> 28 (204 -> 205 at EXPECTED_PLAYED).`
- `seventy-eight of the eighty-two` → `seventy-nine of the eighty-three` (gemeten over de lijst: 83 regels, waarvan 32 met de (A)-zin, 36 lader-weigeringen (B), 11 MARKERs; 32 + 36 + 11 = 79).

#### Tests en gates (elk los na `eval $(opam env)`; kop e748fc67, gates 1 en 10 na de laatste tekstcorrectie opnieuw)

- `make test`: **726 tests groen** (unie 721, +5).
- 1: `check-self-contained: PASS`
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 302 file(s) validate and round-trip, floor 192)`
- 3: `check-verifier: PASS (302 golden clean; 462 negatief rejected for their declared rule, floor 350; 304 on their declared arm (expect.txt), floor 260; 564 cited pointers resolve in their documents, floor 547)`
- 4: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 735 case file(s) validate)`
- 5: `check-oracle: PASS (ref suite 1019 worked examples, floor 489; 692 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)`
- 6: `check-clingo: PASS (backend suite 1782 checks, floor 83; 692 cases replay exactly through clingo — ref = clingo over the corpus)`
- 7: `check-noop: PASS (650 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)`
- 10: `check-regelspraak: PASS — JUnit suite green (726 tests, floor 173); 90 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `check-toka-parity: PASS` — `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45`; `TOKA suite 27 tests, floor 27`; `29 replay slice(s)`; `28 replay slice(s) verifier-clean, floor 28`; ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED (jar `drools-jar: fresh (jar newer than 160 source files)`, gebouwd door gate 12)
- 12: `check-drools: PASS` — `played 205`; `channel floor 55 non-PLAYED goldens pinned by name; played matches the pinned 205`; `DRL-text branch played 177 (… pin 177)`; `DRL-text floor 83 non-PLAYED goldens pinned by name`; `JUnit 800 tests, floor 239` (de Drools-jar één keer gebouwd, door gate 12's eigen `mvn package`)
- Naamtabeltoets: `v1-naamtabel-toets: OK` (geen spec-zin geraakt).
- Niet gedraaid (niet in deze opdracht): 8, 9, 13, 14.

#### Registerzin voor `spec/CONSTRUCTS.md` rij 8 (vervangt die van het oorspronkelijke Result; de hoofdsessie landt hem)

> Landed (2026-09-25, card `dialect-opgave-boven-berekening`): form 3 of the dialect, sugar over the hardship pattern (`PHASE4-DEFAULTS.md` §4.1) — the dialect gets a surface for "given, not derived", with no wire delta. An attribute declaration may end in the marker `(mag opgegeven worden)` (one whole token; measured inert over 952 RegelSpraak documents, IR and diagnostics identical before and after). The RegelSpraak frontend lowers a marked attribute X to structure the wire already carries: the input attribute `opgegeven X`; the attribute `berekende X`, which every authored computing writer of X now targets (a rule, a table row, a tree or an unordered block, moved whole); one defaults group `opgave boven berekening X` on X with the opgave above the computation and an authored initialisation of X as the lowest default; and the possessive characteristic `afwijkende opgave X`, attributed by the rule `afwijking opgave X` where opgave and computation are both filled and differ, with the consistency `controle opgave X` asserting it absent, so a deviating opgave deposits one §9.5 fact. Case input on X itself keeps Reading A (ch. 7.8 unchanged). Refused at the declaration: the marker on a characteristic, on anything but a number without a unit, on a time-dependent or dimensioned attribute, beside an allocation or object creation that writes X, beside a writer of X elsewhere in the inheritance line, on a generated name already taken (a member, a writer, or a form name, WF-020), and with more than one initialisation or one with a window or a condition; without a computing writer it warns and generates nothing. A dependency cycle through a generated member gets the frontend's cycle warning like any other. Witnesses `jutterswet-opgave-boven-berekening` (two marked attributes, one with an initialisation, and a rule reading the generated characteristic; twelve cases) and `jutterswet-opgave-vormen` (a tree with a nested exception, an unordered block and a decision table under the marker; fifteen cases), both in ref ≡ clingo ≡ Drools exec; the DRL-text branch refuses both on its defaults sentence. No wire, spec, verifier or executor change.

#### Geregistreerde keuzes en afwijkingen

1. B3: ERROR gekozen, geen lowering (reden hierboven); de zin volgt de vorm van de bestaande venster-/versieweigering.
2. B4: tweede pas in plaats van de ene pas verplaatsen (reden hierboven); ontdubbeling per knoop met `berekende X` als X, zoals de bestaande ontdubbeling per knoop werkt.
3. Getuigen staan als nieuwe methoden in de twee testklassen die deze kaart zelf aanlegde (`OpgaveMarkeringTest`, `ZelflezingKringTest`); geen bestaande bewering gewijzigd.
4. A9 gaat verder dan de lensproeven l1–l3: de boom heeft een tweede niveau (geneste uitzondering, zodat "verhuist heel" ook `exceptions` pint), het blok kreeg de conflictcases (case9, case10: het conflict staat onder het verlegde doel), en de tabel een case zonder passende rij; 15 cases in plaats van 12. De proefwaarden van de lens (l1 case1–5, l2 case1–4, l3 case1–3) zitten er inhoudelijk in, op twee na: de Reading-A-case van de boom (l1 case4; case6 van `jutterswet-opgave-boven-berekening` pint Reading A al, en hier pint case11 hem voor het blok) en de lege case van het blok (l2 case3; niets in de invoer en niets berekend, dus onder gate 7 een overlever, terwijl case5-niets van `jutterswet-opgave-boven-berekening` de lege uitkomst al pint).
5. Geschiedenis: de B4-commit is vóór overdracht één keer aangevuld (de WF-046-bijzin in DIVERGENCES was te smal: ook een positieve lezing van het gegenereerde kenmerk geeft WF-046, gemeten); A1 en A9 zijn er opnieuw bovenop gezet. De hashes hierboven zijn de definitieve; alleen lokaal.

#### Restpunten

1. `scripts/check-drools.sh` is beschermd: de pins hierboven zijn een voorstel; de maintainer merget.
2. De registerzin voor CONSTRUCTS rij 8 hierboven: hoofdsessie.
3. Gemeten, buiten deze kaart: clingo weigert een vlakke initialisatie met voorwaarde luid met `TRANSLATION FAULT: rule kind not translated` (`backends/clingo/lib/translate.ml` r. 9235), op de ongemarkeerde lensproef `w/i1-plain.rgs` waar ref en de Drools-exectak spelen. Geen corpusgolden draagt een initialisatie met voorwaarde (gemeten: 0), dus geen gate ziet het.
4. De niet-gespiegelde randen van de kringgraaf blijven (genavigeerde lezing, filter en toets van een vouw, rolcheck, kanaaltest, vormen die geen regel zijn, kring over super- en subtype); gedocumenteerd, de verifier vangt ze onder WF-045/046.
5. De ontdubbeling van B4 is per knoop: een tweede, andere kring waarvan alle knopen (met `berekende X` als X) al op een gemelde kring lagen, krijgt geen eigen melding. In corpus en toka komt geen kring voor (0 meldingen).
6. Niet van deze opdracht, uit hetzelfde lensrapport: B1, B2, B5, A2–A8 en A10 (emitter-fixer, toka-fixer, hoofdsessie).

---

### Derde vervolgronde: RecursieTest-naam en de kringgraaf naar RUNTIME-SEMANTICS ch. 5.1/6.1 (2026-09-25)

**Nieuwe kop: 65d059d9** (branch `card/dialect-opgave-boven-berekening`, basis 3e72773b zoals overgedragen, worktree schoon, niets gepusht of gemerged).

| Commit | Inhoud |
|---|---|
| ee2d05d4 | punt 1 (mandaat): `RecursieTest` — methode `zelfde_instantie_zelfafleiding_clamp_idioom_resolvet_zonder_diagnose` → `zelfde_instantie_zelfafleiding_clamp_idioom_is_een_kring_met_warning`; de klassejavadoc-zin "is tolerated diagnostics-free as a documented divergence" → "is such a cycle too, of one node: the verifier refuses it under WF-045, and it gets the same WARNING" |
| a17f91cf | punt 2, rood: vijf kringtests in `ZelflezingKringTest` (kring door een voorwaarde over een ander attribuut; kenmerkregel die haar eigen kenmerk leest; twee kenmerken die elkaar lezen, gewoon en ontkend; kenmerk en attribuut die elkaar lezen) falen tegen 3e72773b; de pin "kenmerk in de voorwaarde zonder kring meldt niets" slaagt al |
| 65d059d9 | punt 2: `keurKringverwijzingen` spiegelt de lezingen van RUNTIME-SEMANTICS ch. 5.1 en 6.1 op het eigen onderwerp — knopen zijn eigen attributen én kenmerken (gescheiden soorten, namen onder de naamidentiteit van de verifier, `IrEmitter.norm`), randen lopen uit wat waarde én voorwaarde lezen (attribuutverwijzingen en kenmerkchecks in beide polariteiten); het zelf-rand-speciaalgeval van de vorige ronde gaat daarin op; DIVERGENCES §7 bijgewerkt |

**Vooraf gemeten (verifier, 3e72773b):** elk van de vijf vormen gaf `WF-045 FAIL … dependency cycle through no recursive creation` (`a -> b -> a`, `oud -> oud`, `oud -> versleten -> oud` gewoon en ontkend, `oud -> x -> oud`) terwijl het frontend zweeg; de controle zonder kring gaf `verifier: PASS`.

**De meldingen, letterlijk** (WARNING, regel 0, zonder regelnummerstaart; staart `— herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)`):
- `kringverwijzing tussen berekeningen: Ding.x -> Ding.y -> Ding.x — …` (kring door een voorwaarde)
- `kringverwijzing tussen berekeningen: Ding.oud -> Ding.oud — …` (kenmerkregel die haar eigen kenmerk leest)
- `kringverwijzing tussen berekeningen: Ding.oud -> Ding.versleten -> Ding.oud — …` (twee kenmerken, ook ontkend)
- `kringverwijzing tussen berekeningen: Ding.oud -> Ding.x -> Ding.oud — …` (kenmerk en attribuut)

**Byte-identiteit en geen nieuwe WARNING, gemeten** (vóór = 3e72773b uit `git archive`, na = 65d059d9, IR plus diagnostiek per document):
- 86 corpusbronnen + 47 toka-bronnen: 0 verschillen; kringmeldingen vóór 0, na 0 — de verifier-schone goldens krijgen er geen, dus geen spiegelfout zichtbaar.
- De casusdump zoals gate 11 haar samenstelt (45 bronnen, holdouts uit het script gelezen): vóór == na byte-identiek, en na == de gepinde `toka/casus/ir.json` (de herpin van de hoofdsessie zit in deze basis); 0 kringmeldingen.
- Ter informatie 824 documenten buiten de repo: 0 verschillen, 0 kringmeldingen vóór en na.

**Tests en gate 10:**
- `make test`: **685 tests groen**.
- Gate 10 (los na `eval $(opam env)`): `check-regelspraak: FAIL — 85 identical, 1 diverging, 0 emit failures`, met als enige `DIFF: jutterswet-strandmelding (frontend output diverges from the checked-in golden)` — het bekende unie-verschil dat de hoofdsessie fixt (op aanwijzing genegeerd; mijn wijziging laat die emissie byte-gelijk, zie de sweep). De JUnit-stap van gate 10 is groen.

**Wat de graaf bewust niet spiegelt** (in de javadoc van `keurKringverwijzingen` en DIVERGENCES §7): een genavigeerde lezing, een lezing in het filter of de toets van een vouw, een rolcheck, een kanaaltest (regelversie), en vormen die geen regel zijn (verdeling, creatie, consistentie). Een kring die alleen daardoor sluit, weigert de verifier onder WF-045/046 en meldt het frontend niet. Knopen zijn per onderwerpstype; een kring die over een supertype en een subtype loopt (een geërfd lid) valt er ook buiten.

**Open punten van deze ronde:**
1. De genoemde niet-gespiegelde randen (navigatie, vouw, rolcheck, kanaal, niet-regelvormen, erflijn): een eigen afweging; de verifier vangt ze al.

---

### Tweede en eerste vervolgronde en oorspronkelijk Result (hashes van vóór de latere rebases)

### Tweede vervolgronde: besluit hoofdsessie op open punten 1 en 2 (2026-09-25)

**Nieuwe kop: 34550931** (branch `card/dialect-opgave-boven-berekening`, basis b662e108 zoals overgedragen, worktree schoon, niets gepusht of gemerged).

| Commit | Inhoud |
|---|---|
| 7ae8f596 | punt 1, rood: `RecursieTest.zelfde_instantie_zelfafleiding_clamp_idioom_resolvet_zonder_diagnose` eist nu de kringmelding (de ene assertie omgedraaid, met de commentaarregel `// the verifier refuses this self-read under WF-045 like every dependency cycle, so the frontend warns here too`, onder het mandaat van de hoofdsessie); `ZelflezingKringTest.het_begrenzingsidioom_krijgt_de_melding` idem. Tegen b662e108 falen precies deze twee |
| a76bec7d | punt 1: de drie regels uitzondering voor het begrenzingsidioom weg uit `keurKringverwijzingen`; javadoc en DIVERGENCES §7 mee |
| 2e0703db | punt 2, rood: `een_zelflezing_in_de_voorwaarde_krijgt_de_melding`, `een_zelflezing_in_een_samengestelde_voorwaarde_krijgt_de_melding` (falen tegen a76bec7d) en de pin `een_voorwaarde_op_een_ander_attribuut_zonder_kring_meldt_niets` (slaagt al) |
| 34550931 | punt 2: een berekening (elke soort behalve kenmerktoekenning) waarvan de voorwaarde haar eigen doel leest, krijgt de zelf-rand; nieuwe `verzamelEigenReads(Conditie, …)` over vergelijking, predicaat, venster en samengestelde voorwaarden (het filter en de toets van een kwantorvouw lezen het element en blijven erbuiten); DIVERGENCES §7 mee |

**De melding, letterlijk** (WARNING, regel 0, zonder regelnummerstaart), voor de kale zelflezing, het begrenzingsidioom en de zelflezing in de voorwaarde gelijk:

`kringverwijzing tussen berekeningen: Ding.x -> Ding.x — herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)`

**Byte-identiteit gemeten**, vóór = b662e108 (uit `git archive` gebouwd), na = 34550931, IR plus diagnostiek per document:
- 86 corpusbronnen (het corpus telt er inmiddels 86) en 47 toka-bronnen: 0 verschillen; geen enkele draagt een kringmelding.
- De casusdump zoals gate 11 haar samenstelt (45 niet-uitgezonderde bronnen samen): vóór == na byte-identiek, geen kringmelding. Dat zij van de gepinde `toka/casus/ir.json` afwijkt, is de bekende herpin van de hoofdsessie en de emitter-bouwer.
- Ter informatie: 824 documenten buiten de repo (660 + 164; eerder schreef ik abusievelijk 820): 0 verschillen.

**Tests en gates:**
- `make test`: **673 tests groen**.
- Gate 10 (`eval $(opam env)` los): **FAIL**, overgeërfd, niet van deze kaart: `check-regelspraak: FAIL — 85 identical, 1 diverging, 0 emit failures`; `DIFF: jutterswet-strandmelding (frontend output diverges from the checked-in golden)`. De golden kwam met unie-commit e70df7f1 (kaart verifier-allocatie-keten); het frontend schrijft op haar objectcreatie een `connectingRole` (`factType: strandmelding`, `role: gemeld strandrecht`, de emitterkaart, commit f13b4397) die de gecommitte `ir.json` niet heeft. Ook de overgedragen kop b662e108 emitteert dat al (gemeten met de vóór-build), en mijn build is daaraan byte-gelijk. JUnit en de overige 85 goldens van gate 10 zijn in orde. Gate 1 en 11 niet gedraaid (bekend, op aanwijzing).

**Open punten van deze ronde:**
1. `RecursieTest`: de methodenaam `…_resolvet_zonder_diagnose` en de klassejavadoc ("tolerated diagnostics-free as a documented divergence") beschrijven nog de oude houding. Het mandaat dekte één assertie en een commentaarregel; hernoemen plus één javadoczin vraagt hetzelfde mandaat.
2. Een kring die door een voorwaarde over een ander attribuut loopt (x = 5 indien y > 3; y = x plus 1), en een kenmerkregel die haar eigen kenmerk in de voorwaarde leest, meldt de frontend nog niet; de verifier weigert zulke kringen onder WF-045. Buiten de opdracht; DIVERGENCES §7 noemt het eerste.
3. Gate 10: `corpus/golden/jutterswet-strandmelding/ir.json` opnieuw emitteren (met de `connectingRole`) en de replay in ref, clingo en Drools daarna nameten; niet van deze kaart.

---

### Eerste vervolgronde en oorspronkelijk Result (hashes van vóór de tweede rebase)

### Vervolgronde na de rebase op card/k7-unie (2026-09-25)

**Nieuwe kop: bdd1a0cb** (branch `card/dialect-opgave-boven-berekening`, worktree schoon, niets gepusht of gemerged). De rebase gaf de eerdere commits nieuwe hashes: claim 1bf50b6c, rood-eerst f5139658, markering 511ccdc3, golden 76dc28ce, DIVERGENCES §7 cfc9fa3e, identiteit 435a9a19, §4/§7 9c941b85, 952-meting 4056a168, lexercommentaar 3c878373. Nieuw in deze ronde:

| Commit | Inhoud |
|---|---|
| 6379d368 | punt 1: `IrEmitter.java`, het commentaar bij de emissie van `prioritizedDefaults` noemt ook de groep `opgave boven berekening X` van de markering (één regel erbij) |
| 755c3697 | punt 2, rood-eerst: `ZelflezingKringTest` (7 tests; tegen de voorgaande kop 5 rood, 2 groen: de kring over twee attributen en het begrenzingsidioom, die al moeten gelden) |
| bdd1a0cb | punt 2: `RuleChecker.keurKringverwijzingen` telt de directe zelflezing als kring van één knoop; `DIVERGENCES.md` §7 bijgewerkt |

**De melding, letterlijk** (WARNING, regel 0, zonder regelnummerstaart; dezelfde vorm als de transitieve kring):

`kringverwijzing tussen berekeningen: Ding.x -> Ding.x — herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)`

Op een gemarkeerd attribuut in de namen van de auteur (de controle draait vóór de lowering): `kringverwijzing tussen berekeningen: Vondst.waarde -> Vondst.waarde — herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)`; het document gaat, zoals bij elke kring, nog steeds naar schijf.

**Gemeten vóór de wijziging:** `De x van een Ding moet berekend worden als de x van de Ding plus 1` ging zonder melding de wire op; de verifier: `WF-045 FAIL /rules/rules/0/versions/0: dependency cycle through no recursive creation: ophoog x -> ophoog x`. Hetzelfde voor het begrenzingsidioom (`… begrens x -> begrens x`) en voor een zelflezing in de voorwaarde (`… zet x -> zet x`).

**Afwijking van de opdracht, met reden (besluitpunt):** het begrenzingsidioom `De x … moet gesteld worden op de x van de Ding, met een maximum van de y van de Ding` blijft zonder melding. Het bestaande testbestand `RecursieTest.java` (`zelfde_instantie_zelfafleiding_clamp_idioom_resolvet_zonder_diagnose`, meegekomen met de kopieerroute) pint die vorm als "tolerated diagnostics-free as a documented divergence", en CLAUDE.md verbiedt een testbestand te wijzigen zonder eerst te vragen. De verifier weigert die vorm wél (WF-045, gemeten). De uitzondering is exact: alleen een waarde die een begrenzing is met als basis de kale lezing van het doel; een begrenzing van iets anders (`x plus 1, met een maximum …`) en een grens die het doel leest krijgen de melding (getuigen in `ZelflezingKringTest`). Opheffen = één assertie in `RecursieTest` omdraaien en de drie regels in `keurKringverwijzingen` weghalen; dat vraagt het akkoord van de maintainer.

**Byte-identiteit gemeten:**
- Sweep-harnas vóór (6379d368) en na (bdd1a0cb) over 952 documenten (81 + 47 + 660 + 164) en over de huidige 85 corpusbronnen + 47 toka-bronnen per bestand: 0 verschillen in IR plus diagnostiek; geen enkel document draagt een kringmelding.
- De toka-casusdump zoals gate 11 haar bouwt (de 45 niet-uitgezonderde bronnen samen): vóór en na byte-identiek aan elkaar, én byte-identiek aan de emissie van de unie-basis 79c22c1e (gebouwd uit `git archive`, zonder één commit van deze kaart).
- `make test`: **670 tests groen**.

**Gates (elk los na `eval $(opam env)`, op bdd1a0cb):**
- 1: **FAIL**, overgeërfd, niet van deze kaart: `FAIL: TOKA material in artifact zone 'spec/IR-SPEC.md' (rights policy, corpus/README.md; the sole exception is the isolated toka/ zone)`. Elf treffers van het tripwirepatroon op `spec/IR-SPEC.md` r. 3282–3283, 4680–4681, 5016–5017, 6071–6072, 7340–7341: de woorden "toka-24" en "the card toka-welgevormd", uit commit 1903c4b7 ("IR-SPEC: gate 11 haalt de pariteitsslices nu door de verifier"). Hetzelfde aantal (11) op de unie-basis 79c22c1e; deze kaart raakt `spec/` niet. Alle andere controles van gate 1 (privépaden in alle zones, no-share, de hernoemtripwire, toka zonder machinepaden) slaagden; het TOKA-patroon treft in geen enkele andere zone.
- 10: `check-regelspraak: PASS — JUnit suite green (670 tests, floor 173); 85 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: **FAIL**, overgeërfd, niet van deze kaart: `DIFF: casus (re-emitted IR diverges from the pinned toolchain dump)` / `check-toka-parity: FAIL — casus: the frontend output diverges from the pinned dump`. Bouw en TOKA-suite slaagden; de gate stopt bij stap 3, de latere stappen (replay, welgevormdheid) draaiden niet. Het verschil, in sleutels en tellingen: de ene objectcreatie van de casus krijgt `connectingRole` en er komt één WARNING bij (4 → 5 diagnosen). Dat komt uit de unie (kaart frontend-emitterrestanten; de rol op de objectcreatie is commit f13b4397), en de gepinde `toka/casus/ir.json` is daar niet meebewogen. De emissie van de unie-basis 79c22c1e geeft hetzelfde verschil; deze kaart verandert de casus niet (zie boven). `toka/` valt buiten deze kaart.

**Open punten van deze ronde** (de eerdere lijst hieronder: punt 1 en 2 zijn hiermee afgehandeld, op het begrenzingsidioom na):
1. Besluit maintainer: het begrenzingsidioom ook melden (de `RecursieTest`-pin omdraaien) of de uitzondering houden.
2. Een zelflezing in de VOORWAARDE van een berekening weigert de verifier ook (WF-045, gemeten); de kringcontrole kijkt, net als voor de transitieve kring, alleen naar de waarde. Buiten de opdracht gelaten.
3. Gate 1: de elf "toka"-treffers in `spec/IR-SPEC.md` (commit 1903c4b7, kaart toka-welgevormd) moeten anders gespeld worden; beschermd pad, niet van deze kaart.
4. Gate 11: `toka/casus/ir.json` opnieuw pinnen op de emissie van de unie (`connectingRole` op de objectcreatie, de extra WARNING), of het verschil anders verantwoorden; toka-zone, niet van deze kaart.

---

### Oorspronkelijk Result (vóór de rebase; hashes zijn die van vóór de rebase)

### Stand

Branch `card/dialect-opgave-boven-berekening` (worktree (worktree opgave)), basis main 20e508d5, claim 32b1bbb5, kop **225142a8**. Werkboom schoon; niets gepusht of gemerged.

| Commit | Inhoud |
|---|---|
| 828920f3 | rood-eerst: `OpgaveMarkeringTest` (30 tests; op de basis 28 rood, 2 groen) |
| 886a4905 | lexertoken, twee parserproducties, lowering in `RuleChecker` |
| 7ae02a38 | golden `jutterswet-opgave-boven-berekening` + voorstel gate 12 en gate 7 |
| ed6e3964 | `DIVERGENCES.md` §7 + javadoc `PrioritizedDefaults`, `Default` |
| 36ec7959 | `RuleChecker`: de opgenomen initialisatie verlaat de regellijst op identiteit |
| fea8d270 | `DIVERGENCES.md` §4 (emissie noemt de markering) en §7 (telling emitterweigeringen) |
| cb784451 | `DIVERGENCES.md` §7: inertheid over 952 documenten, collecties neutraal benoemd |
| 225142a8 | lexercommentaar noemt de gemeten bronnen neutraal |

Gewijzigde bestanden: `frontends/regelspraak/grammar/{RegelSpraakLexer.g4,RegelSpraak.g4,DIVERGENCES.md}`, `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/RuleChecker.java`, `…/ir/{PrioritizedDefaults.java,Default.java}` (alleen javadoc), `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/OpgaveMarkeringTest.java` (nieuw), `corpus/golden/jutterswet-opgave-boven-berekening/**` (nieuw), `scripts/check-drools.sh` en `scripts/check-noop.sh` (**voorstel**). `IrEmitter.java` niet aangeraakt (U2): de bestaande emissiepaden volstonden.

### Tellingen

- Frontend JUnit: 618 → 648 (+30, `OpgaveMarkeringTest`), 0 rood.
- `bron.rgs`-goldens: 81 → 82; golden cases 643 → 655; gate 12 `played` 197 → 198; DRL-tak 173 (onveranderd); gate 7 overlevers 38 → 39.
- Drools-backendsuite: 776 tests, 0 skipped.

### Wat gebouwd is

- Token `MAG_OPGEGEVEN_WORDEN : '(mag' WSP 'opgegeven' WSP 'worden)' ;` naast `BEZIELD`/`BEZITTELIJK`/`BIJVOEGLIJK`; `MAG_OPGEGEVEN_WORDEN?` als laatste element vóór `SEMICOLON?` in `attribuutSpecificatie` en in beide alternatieven van `kenmerkSpecificatie`.
- `RuleChecker.opgaveDeclaraties` (vóór de regelresolutie): declaratieweigeringen; daarna `opgegeven X`, `berekende X` direct na X en `afwijkende opgave X` (bezittelijk) achteraan de kenmerken gedeclareerd, zodat regels ze kunnen lezen.
- `RuleChecker.opgaveLowering` (na de defaults-lowering van §4): schrijversweigeringen; rekenende schrijvers (regel, beslistabelrij, boom, ongeordend blok) verlegd naar `berekende X` met behoud van naam, vorm, voorwaarde, venster en statuutannotatie; groep `opgave boven berekening X` (opgave-default, rekendefault, initialisatie onderaan met haar statuutannotatie als source), regel `afwijking opgave X`, consistentie `controle opgave X` (source = de gemarkeerde declaratie tot en met de markering, met regelnummer).
- Brontekst van de gegenereerde regel en lezingen = wat het frontend emitteert voor dezelfde regel met de hand geschreven (`Een Vondst heeft afwijkende opgave waarde`, `zijn opgegeven waarde is gevuld`, …; getuige in de JUnit-wiretest). De twee gegenereerde defaults dragen `opgegeven X` / `berekende X` als source.

### Definitieve weigerzinnen (letterlijk; elk ERROR op de regel van de declaratie) en de waarschuwing

Uit `OpgaveMarkeringTest`, die ze woordelijk pint:

1. kenmerk: `kenmerk 'getaxeerd' van Vondst: '(mag opgegeven worden)' staat alleen op een attribuut — casusinvoer op een kenmerk staat al tegen elk besluit van een defaults-groep (RUNTIME-SEMANTICS ch. 7.8), en een opgave 'niet aanwezig' boven een berekende toekenning vraagt een opgave met drie waarden, die de casus voor een kenmerk niet kent (dialect §7)` (idem `'keurmerk'`, bezittelijk)
2. datatype: `attribuut 'opgave' van Vondst: '(mag opgegeven worden)' staat alleen op een Numeriek attribuut zonder eenheid, en 'opgave' is Tekst — een defaults-groep op een attribuut van een ander datatype of met een eenheid is nog niet in alle drie de uitvoerders gemeten (dialect §7)`; op de plaats van `Tekst` gemeten: `Numeriek met eenheid €`, `Percentage`, `Boolean`, `Datum in dagen`
3. tijd: `attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat niet op een tijdsafhankelijk attribuut — een defaults-groep beslist één keer en heeft geen tijdlijn om op te schrijven (WF-086) (dialect §7)`
4. dimensie: `attribuut 'premie' van Jutter: '(mag opgegeven worden)' staat niet op een gedimensioneerd attribuut, en 'premie' is gedimensioneerd met seizoen — een defaults-groep op een gedimensioneerd attribuut speelt nog niet in alle drie de uitvoerders (dialect §7)`
5. verdeling: `attribuut 'sluitdeel' van Jutter: '(mag opgegeven worden)', maar de verdeling 'verdeel de sluitpremie' schrijft 'sluitdeel' — de markering verlegt een verdeling niet naar 'berekende sluitdeel', en naast de defaults-groep op 'sluitdeel' is zij een tweede schrijver (WF-050) (dialect §7)`
6. objectcreatie: `attribuut 'grondslag' van Strandrecht: '(mag opgegeven worden)', maar de objectcreatie 'boek een strandrecht' kent 'grondslag' toe bij het aanmaken — de markering verlegt die toekenning niet naar 'berekende grondslag', en naast de defaults-groep op 'grondslag' is zij een tweede schrijver (WF-050) (dialect §7)`
7. erflijn: `attribuut 'waarde' van Vondst: '(mag opgegeven worden)', maar regel 'wrakwaardering' schrijft 'waarde' voor Wrakvondst, een subtype van Vondst — de gegenereerde defaults-groep op Vondst zou met de schrijvers van Wrakvondst samengaan tot één stapel per type (RUNTIME-SEMANTICS ch. 7.10), en die samenstelling bouwt de markering niet (dialect §7)`
8. naam al gedeclareerd: `attribuut 'waarde' van Vondst (bronregel 3): '(mag opgegeven worden)' genereert het attribuut 'opgegeven waarde', en Vondst declareert die naam al als attribuut (bronregel 4) — een naam die de markering genereert, bestaat nog niet in de erflijn van Vondst (dialect §7)`
9. vormnaam al gedragen: `attribuut 'waarde' van Vondst (bronregel 4): '(mag opgegeven worden)' genereert de regel 'afwijking opgave waarde', en regel 'Afwijking opgave waarde' heet al zo (bronregel 10) — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld (WF-020) (dialect §7)`
10. vormnaam van een andere markering: `attribuut 'waarde' van Wrak (bronregel 7): '(mag opgegeven worden)' genereert de defaults-groep 'opgave boven berekening waarde', en de markering op attribuut 'waarde' van Vondst genereert die naam al (bronregel 3) — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld (WF-020) (dialect §7)`
11. schrijver van een gegenereerd lid: `attribuut 'waarde' van Vondst (bronregel 3): regel 'directe berekening' schrijft 'berekende waarde' (bronregel 11), een lid dat '(mag opgegeven worden)' genereert — 'opgegeven waarde' levert de casus, 'berekende waarde' krijgt de schrijvers van 'waarde', en 'afwijkende opgave waarde' kent de regel 'afwijking opgave waarde' toe; een andere schrijver verdraagt zo'n lid niet (dialect §7)`
12. twee initialisaties: `attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en 'waarde' heeft meer dan één initialisatie ('standaardwaarde' en 'tweede standaardwaarde') — de gegenereerde defaults-groep draagt één initialisatiedefault, de laagste (WF-050) (dialect §7)`
13. initialisatie met venster (of versies): `attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en de initialisatie 'standaardwaarde' van 'waarde' heeft een ander geldigheidsvenster dan altijd — de gegenereerde defaults-groep geldt altijd, en een default draagt geen eigen venster (dialect §7)` (bij versies: `heeft meer dan één versie`)
14. WARNING, geen rekenende schrijver (ook: alleen een initialisatie): `attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat op een attribuut dat geen regel berekent of vaststelt — 'waarde' is al invoer, en de markering genereert niets (dialect §7)`
15. idem, maar een regel leest een gegenereerde naam (ERROR in plaats van de WARNING): `attribuut 'waarde' van Vondst: '(mag opgegeven worden)' genereert niets, omdat geen regel 'waarde' berekent of vaststelt, maar het model leest 'berekende waarde' — die naam bestaat alleen waar de markering een berekening van 'waarde' verlegt (dialect §7)`

Geen verifier-, lader- of Drools-zin veranderd: de weigeringen zijn frontend-eigen (de wire kent de markering niet).

### Golden `corpus/golden/jutterswet-opgave-boven-berekening`

Eigen casus "Art. 3d (vondstwaarde en bergloon)" (tekst in NOTE). `waarde` (taxatie verminderd met 5,00) en `bergloon` (gewicht maal 2 indien het gewicht gevuld is; initialisatie 25,00) dragen de markering; `ter controle` leest `afwijkende opgave waarde`. `ir.json` = frontend-emissie, `diagnostics: []`, verifier PASS.

| Case | Invoer | Verwacht (absoluut) |
|---|---|---|
| case1-geen-opgave | taxatie 40.00 | waarde 35.00; berekende waarde 35.00; afwijkende opgave waarde afwezig; ter controle afwezig; bergloon 25.00; berekende bergloon leeg; consistent |
| case2-opgave-gelijk | taxatie 40.00, opgegeven waarde 35.00 | waarde 35.00; berekende waarde 35.00; kenmerk afwezig; ter controle afwezig; bergloon 25.00; consistent |
| case3-opgave-wijkt-af | taxatie 40.00, opgegeven waarde 50.00 | waarde 50.00; berekende waarde 35.00; afwijkende opgave waarde aanwezig; ter controle aanwezig; bergloon 25.00; inconsistent `controle opgave waarde` × V |
| case4-opgave-zonder-feiten | opgegeven waarde 50.00 | waarde 50.00; berekende waarde leeg; kenmerk afwezig; ter controle afwezig; bergloon 25.00; consistent |
| case5-niets | — | waarde leeg; berekende waarde leeg; kenmerk afwezig; bergloon 25.00; berekende bergloon leeg; afwijkende opgave bergloon afwezig; consistent |
| case6-doel-zelf-in-invoer | taxatie 40.00, waarde 60.00 | waarde 35.00; berekende waarde 35.00; kenmerk afwezig; bergloon 25.00; consistent |
| case7-doel-zelf-zonder-feiten | waarde 60.00 | waarde 60.00; berekende waarde leeg; kenmerk afwezig; bergloon 25.00; consistent |
| case8-bergloon-berekend | gewicht 20.00 | bergloon 40.00; berekende bergloon 40.00; kenmerk afwezig; waarde leeg; consistent |
| case9-bergloon-opgave-wijkt-af | gewicht 20.00, opgegeven bergloon 50.00 | bergloon 50.00; berekende bergloon 40.00; afwijkende opgave bergloon aanwezig; ter controle afwezig; waarde leeg; inconsistent `controle opgave bergloon` × V |
| case10-bergloon-opgave-zonder-feiten | opgegeven bergloon 50.00 | bergloon 50.00; berekende bergloon leeg; kenmerk afwezig; waarde leeg; consistent |
| case11-bergloon-zelf-staat | bergloon 60.00 | bergloon 60.00; berekende bergloon leeg; kenmerk afwezig; waarde leeg; consistent |
| case12-beide-wijken-af | taxatie 40.00, opgegeven waarde 50.00, gewicht 20.00, opgegeven bergloon 30.00 | waarde 50.00; berekende waarde 35.00; beide kenmerken aanwezig; ter controle aanwezig; bergloon 30.00; berekende bergloon 40.00; inconsistent `controle opgave waarde` × V en `controle opgave bergloon` × V |

Replay: ref 12/12 en clingo 12/12 exact; Drools exec `PLAYED 12`; DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`. Dertien tegencases (buiten de repo, (buiten de repo), elk één verwachting omgedraaid) falen in alle drie. No-op: alleen case11 overleeft.

### Gate-uitvoerregels (na `eval $(opam env)`, elk los; 1, 10 en 11 nogmaals op kop 225142a8)

- 1: `check-self-contained: PASS`
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 294 file(s) validate and round-trip, floor 192)`
- 3: `check-verifier: PASS (294 golden clean; 433 negatief rejected for their declared rule, floor 350; 275 on their declared arm (expect.txt), floor 260)`
- 4: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 698 case file(s) validate)`
- 5: `check-oracle: PASS (ref suite 994 worked examples, floor 489; 655 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)`
- 6: `check-clingo: PASS (backend suite 1733 checks, floor 83; 655 cases replay exactly through clingo — ref = clingo over the corpus)`
- 7: `check-noop: PASS (616 cases refute a do-nothing executor; 39 pinned survivors pin defeasible standing)`
- 8: `check-rood: Drools leg PASS (11 fault-pinned, 32 not-carried with reason)` / `check-rood: PASS (43 red cases fault observably in ref and clingo, floor 26; 86 pinned fragments cite none of the 36 listed bottom sentences)`
- 10: `check-regelspraak: PASS — JUnit suite green (648 tests, floor 173); 82 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `check-toka-parity: PASS` (45 .rgs byte-identiek, floor 45; 29 toka-cases door ref, clingo, Drools; nul stille divergenties)
- 12: `check-drools: PASS` — `played 198`; `channel floor 54 non-PLAYED goldens pinned by name; played matches the pinned 198`; `DRL-text branch played 173 (… pin 173)`; `JUnit 776 tests, floor 239`
- 13: `check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`
- Drools-jar: één keer met `mvn -o -B -f backends/drools/pom.xml package` (776 tests), daarna bouwt gate 12 hem zoals ontworpen; gate 8 herbouwde hem nog eens met `-DskipTests` (2 s) omdat `RuleChecker.java` in de bronverzameling van `drools-jar.sh` nieuwer was. Naamtabeltoets `v1-naamtabel-toets: OK` (geen spec-zin geraakt).

### Gewijzigde scriptregels (beschermd: VOORSTEL)

`scripts/check-drools.sh`:
- `EXPECTED_PLAYED=197` → `EXPECTED_PLAYED=198`, met redenblok "197 -> 198 (2026-09-25, card dialect-opgave-boven-berekening) …".
- `DRL_CHANNEL` klasse (A), in naamvolgorde na `jutterswet-nestkenmerk-rgs`: `"jutterswet-opgave-boven-berekening|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in this branch"`.
- Kop klasse (A): 29 → 30 regels, kloof 24 → 25, "28 of the 29" → "29 of the 30", "the 29th" → "the 30th", "27 of those 28" → "28 of those 29", "28 exec-only players" → "29" (twee plaatsen), "The 24 is 28 … those 27" → "The 25 is 29 … those 28", en de hertelling "And again the same day (card dialect-opgave-boven-berekening): … 29 -> 30 lines and 24 -> 25 (197 -> 198 at EXPECTED_PLAYED)". `EXPECTED_DRL_PLAYED=173` onveranderd.

`scripts/check-noop.sh`: overlever `corpus/golden/jutterswet-opgave-boven-berekening/case11-bergloon-zelf-staat.json`, met reden in de kop (de initialisatiedefault onderaan de gegenereerde groep op een invoerdoel: geen opgave en geen gewicht, haar rechtvaardiging faalt op de invoer 60.00, de invoer blijft staan; ch. 7.3, ch. 7.8).

### Registerzin voor `spec/CONSTRUCTS.md` (rij 8, als Landed-alinea; de hoofdsessie landt hem)

> Landed (2026-09-25, card `dialect-opgave-boven-berekening`): form 3 of the dialect, sugar over the hardship pattern (`PHASE4-DEFAULTS.md` §4.1) — the dialect gets a surface for "given, not derived", with no wire delta. An attribute declaration may end in the marker `(mag opgegeven worden)` (one whole token; measured inert over 952 RegelSpraak documents, IR and diagnostics identical before and after). The RegelSpraak frontend lowers a marked attribute X to structure the wire already carries: the input attribute `opgegeven X`; the attribute `berekende X`, which every authored computing writer of X now targets (a rule, a table row, a tree or an unordered block, moved whole); one defaults group `opgave boven berekening X` on X with the opgave above the computation and an authored initialisation of X as the lowest default; and the possessive characteristic `afwijkende opgave X`, attributed by the rule `afwijking opgave X` where opgave and computation are both filled and differ, with the consistency `controle opgave X` asserting it absent, so a deviating opgave deposits one §9.5 fact. Case input on X itself keeps Reading A (ch. 7.8 unchanged). Refused at the declaration: the marker on a characteristic, on anything but a number without a unit, on a time-dependent or dimensioned attribute, beside an allocation or object creation that writes X, beside a writer of X elsewhere in the inheritance line, on a generated name already taken (a member, a writer, or a form name, WF-020), and with more than one initialisation or one with a window; without a computing writer it warns and generates nothing. Witness `jutterswet-opgave-boven-berekening` (two marked attributes, one with an initialisation, and a rule reading the generated characteristic): twelve cases in ref ≡ clingo ≡ Drools exec; the DRL-text branch refuses it on its defaults sentence. No wire, spec, verifier or executor change.

### Wat aan de kaart of het voorstel niet klopte (gemeten) en afwijkingen van U1

1. **Initialisatie onder een coalescerende berekening.** Een berekening die leeg als 0 telt (`maal`, `plus`, `min`; RUNTIME-SEMANTICS ch. 1.2) beslist altijd, zodat de initialisatie eronder nooit beslist. Gemeten op mijn eerste golden-ontwerp (`gewicht maal 2` zonder voorwaarde): ref `MISMATCH: V.bergloon = 0, expected 25`, clingo `MISMATCH: V.bergloon = [0], expected 2500` (ref en clingo eens; mijn handberekening was fout). De lowering volgt hier de vlakke ch. 7.7-volgorde (berekening boven initialisatie); de golden kreeg `indien zijn gewicht gevuld is`. Vastgelegd in NOTE en DIVERGENCES §7. De proef van het voorstel (§5.4) gebruikte `verminderd met`, dat een lege linkeroperand doorgeeft, en zag het daarom niet.
2. **"Een attribuut dat geen regel schrijft"** gelezen als "geen rekenende schrijver": met alleen een initialisatie zou `berekende X` geen schrijver hebben en de consistentie nooit kunnen afgaan. Die gevallen krijgen de WARNING; de initialisatie blijft een gewone regel.
3. **Drie weigeringen die de kaart niet noemt:** (a) meer dan één initialisatie van X, of één met een venster of versies (de groep draagt één venster en één initialisatiedefault; WF-050); (b) een schrijver van een gegenereerd lid — ik lees "gebruikt" in de botsingsregel als "declareert of schrijft"; lezen mag, zoals §5.6 van het voorstel het kenmerk leesbaar noemt; (c) zonder rekenende schrijver een lezer van een gegenereerde naam: ERROR in plaats van de WARNING, want de naam zou na het intrekken niets lezen.
4. **Gegenereerde leden vóór de regelresolutie.** Het voorstel noemt het kenmerk "readable by rules" (§5.6); dat vraagt dat de leden gedeclareerd zijn voordat regels resolven. Gemeten op de basis: een regel die `afwijkende opgave waarde` of `zijn berekende waarde` las, bleef een marker met WARNING. Daarom twee passen; getuige `het_gegenereerde_kenmerk_en_de_berekende_waarde_zijn_leesbaar` en de golden (`ter controle`).
5. **WF-020 en de vaste namen.** De zes gegenereerde namen dragen geen typenaam; twee gemarkeerde attributen met één naam op twee objecttypen genereren dezelfde vormnamen en botsen onder WF-020. Geweigerd met beide plekken (zin 10).
6. **Proefvormen van §5.4 versus de kaart:** de proeven hadden groepnaam `waarde` en een bijvoeglijk kenmerk; de bouw volgt de kaart (`opgave boven berekening X`, bezittelijk). Gemeten: speelt in alle drie. De twee proeftabellen (8 + 5 cases) zijn in één golden 12 cases geworden (de "afwijking leesbaar"-case zit in case3, de lege initialisatiecase in case5), met 13 tegencases buiten de repo.
7. **Inertheid:** anders dan in DIVERGENCES §6 veranderde geen "expecting {…}"-tekst; het token staat alleen aan het eind van een liddeclaratie en geen gemeten document heeft daar een syntaxfout. De sweep liep over 952 documenten (81 + 47 + 660 + 164) i.p.v. de "876 in zeven collecties" van §6; welke van die zeven collecties de 660 + 164 samen dekken, heb ik niet hervastgesteld.
8. **Regelverwijzingen van het voorstel:** `RegelSpraak.g4:368-373` voor `attribuutSpecificatie` stond op 20e508d5 op 374-379.
9. **L5-spelling in meldingen:** zin 2 noemt het brontype zoals RegelSpraak het schrijft (`Tekst`, `Numeriek met eenheid €`, via `Datatype.naam()`), zoals alle bronmeldingen van het frontend; ik lees de L5-regel van het regime als geldend voor wire-meldingen (verifier, laders).

### Open punten

1. `IrEmitter.java` (kaart van U2): het commentaar bij de emissie van `prioritizedDefaults` zegt dat de lijst alleen verschijnt voor `Uitzondering op`/`Uitzonderingen ongeordend`; de opgavemarkering maakt nu ook groepen. Eén regel commentaar bij de seriële rebase; DIVERGENCES §4 is al bijgewerkt.
2. Bestaand gat, gemeten, buiten deze kaart: een berekening van X die X leest (direct of via een ander attribuut) is een afhankelijkheidskring die de verifier weigert onder WF-045, met en zonder markering (met markering ook WF-046 en WF-051). Het frontend zegt bij een directe zelflezing niets en geeft bij een transitieve kring alleen een WARNING (`keurKringverwijzingen` sluit zelflezing uit met "no-loop"). Een frontendweigering zou beide sluiten.
3. De typeloze vormnamen (punt 5 hierboven): als twee gemarkeerde attributen met één naam op twee typen naast elkaar moeten kunnen, is een naamgevingsbesluit nodig (typenaam in de gegenereerde namen); vandaag geweigerd.
4. Andere datatypefamilies (datum, tekst, enumeratie, getal met eenheid), de kenmerkmarkering (voorstel §5.8), verlegging van verdeling en objectcreatie, en samenstelling in de erflijn (ch. 7.10): elk een eigen kaart met golden in de drie uitvoerders.
5. De uitleg (voorstel §5.5) hoort bij de regelspraak-java/werkbank-kaart; de source-snippets `opgegeven X`/`berekende X` van de gegenereerde defaults zijn het aanknopingspunt.
6. De registerzin hierboven voor CONSTRUCTS rij 8, en het verplaatsen van de kaart: hoofdsessie.
7. Als de hoofdsessie de L5-regel ook op bronmeldingen van het frontend van toepassing acht: zin 2 wijzigt op één plek (`RuleChecker.opgaveDeclaratieFout`) en in `OpgaveMarkeringTest`.

## Lens en fixronde (2026-09-25, hoofdsessie)

Lens (frontend-duo, unie 36750511, eigen meting in kopieën): EERST FIXEN. Goedgekeurd op eigen meting: de lowering in vijf stukken (proeven boom, ongeordend blok, beslistabel, initialisatie zonder bron: verifier PASS, ref/clingo/Drools-exec spelen, de gegenereerde regel gelijk aan de met de hand geschreven regel), de vijftien weigeringen (elk precies één melding op de declaratieregel, geen document), de golden 12/12 in drie uitvoerders met alleen case11 als no-op-overlever, de inertheid (138 emissies basis tegen unie, 122 identiek, 16 verklaard), de kringgraaf (vijf kringvormen gemeld, niet-kringen stil, geen valse positieven over 89 corpus- en 47 toka-emissies).

Blokkerend voor deze kaart: B3 — de markering liet de voorwaarde van een initialisatie stil vallen (gemeten: met markering schreven alle drie de uitvoerders 25 waar de voorwaarde leeg eiste); B4 — een kring door een gegenereerd lid kreeg geen melding (de kringcontrole liep vóór de lowering). Advies: A1 (DIVERGENCES-zin "filter or test of a fold"; de kring over super- en subtype vastleggen), A9 (de drie vormen onder de markering als golden), A10 (kortere commitonderwerpen — voor de toekomst, geen herschrijving).

Besluiten van de hoofdsessie: B3 wordt een ERROR op de declaratieregel (geen stille lowering, geen nieuwe semantiek) tenzij de voorwaarde zonder kunstgreep conform voorstel §5 in het gelowerde stuk meegaat; B4: de kringcontrole loopt ook over de gelowerde vormen; A9: nieuwe golden `jutterswet-opgave-vormen` (pins van gate 12 opgeteld). A8 geregistreerd: de uitspraak van emitter-ronde 6 dat de frontend bij "versie 2 onbewaakt" strenger is dan de verifier, is op de unie achterhaald — WF-033 leest sinds a41f6d2d elke versie; open punt 8 van die ronde (twee versies van één creatie over een naar-één-relatie) is door de kaartloze WF-052-beslissing gesloten (de versies van één creatie zijn één schrijver; golden `jutterswet-opvolgerversies`).

Fixronde (branch fix/lens-opgave, verse fixer): vier commits op de unie. B3: een initialisatie met voorwaarde onder de markering is een ERROR op de declaratieregel (`… heeft een voorwaarde — de initialisatiedefault van de gegenereerde defaults-groep is alleen gerechtvaardigd waar 'X' in de invoer leeg is, en draagt geen eigen voorwaarde (WF-050) (dialect §7)`); de voorwaarde laten meegaan kan niet zonder kunstgreep (WF-050 staat geen geschreven rechtvaardiging op een initialisatiedefault toe, "leeg in de invoer" is geen schrijfbare voorwaarde, en verplaatsen naar `berekende X` verandert de betekenis); getuige rood-eerst. B4: na de lowering loopt een tweede kringpas over de regels en alle defaults-groepen, met `berekende X` als X zodat een al gemelde kring geen tweede melding krijgt; g1, g2 en g5 krijgen elk hun WARNING, een getuige zonder kring zwijgt; 89 corpusbronnen en de toka-bronnen byte-identiek, 0 kringmeldingen vóór en na. A1: DIVERGENCES zegt "a read in the filter or test of a fold"; de kring over super- en subtype staat er en in de javadoc als niet-gespiegelde rand. A9: golden `jutterswet-opgave-vormen` (boom met geneste uitzondering, ongeordend blok met twee conflictcases, beslistabel met een case zonder passende rij; vijftien handgerekende cases, zestien omgedraaide tegencases falen in alle drie de uitvoerders; ref en clingo 15/15, exec PLAYED 15, DRL-tak op de ch. 7-zin; geen no-op-overlever); pins van gate 12 opgeteld (played 204 → 205, klasse (A) 32 → 33). JUnit 721 → 726; gates 1–7, 10, 11, 12 groen op de fixbranch. Restpunten: de ontdubbeling in de tweede pas werkt per knoop (een tweede, andere kring over al gemelde knopen krijgt geen eigen melding; corpus en toka kennen geen kring); buiten de kaart gemeten: clingo weigerde een gewone initialisatie met voorwaarde luid — apart gefixt in dezelfde ronde (golden `jutterswet-voorwaardelijke-initialisatie`).
