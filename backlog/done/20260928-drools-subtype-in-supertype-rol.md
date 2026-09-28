# drools-subtype-in-supertype-rol: een subtype-instantie past niet in een rol die het supertype vult

*Vondst van een regelsetbouwer, 2026-09-28, bevestigd door de hoofdsessie op
uchar `056df376` via de distributie; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/a8/` (alleen lezen). Luid (MARKER, exit 3), maar een echt
gat met een misleidende melding: ref en clingo spelen beide vormen.*

## Aanleiding

Het beanmodel geeft een rol een referentieveld met precies het type van de
gedeclareerde vuller (`FactModelGenerator.heeftReferentieVeld`). Een
instantie van een SUBTYPE van die vuller (`is een`, DIVERGENCES §5) komt
daardoor niet in de rol: de casus met een subtype-instantie in de rol geeft
een MARKER met een oorzaak die er niet is ("a same-named role toward another
type already holds"), en een regel op het subtype die over de rol telt of
vouwt krijgt exit 3 ("de aggregatie-join heeft geen beanveld"). In een
CGM-gegrond model is dit de gewone vorm: een specifiek recht (`is een` het
algemene recht) heeft de gronden en perioden die het feittype aan het
algemene recht hangt. Ref en clingo spelen het.

## Minimale reproductie

`056df376`, distributie.

    R1  Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
        Objecttype de Wrakvondst is een Vondst (mv: Wrakvondsten)
            het aantal meldingen Numeriek (geheel getal);
        Objecttype de Melding (mv: Meldingen)
            de gemelde waarde Numeriek (geheel getal);
        Feittype VondstHeeftMelding
            de vondst Vondst
            de melding (mv: meldingen) Melding
        Eén vondst heeft meerdere meldingen
        Regel gemelde waarde / geldig altijd /
        De gemelde waarde van een Melding moet gesteld worden op de waarde van de vondst van de Melding.
        Scenario: een Wrakvondst W met waarde 5, een Melding M met "de vondst is W"; verwacht: de gemelde waarde van M is 5.
        nu: check exit 0; scenario exit 3, MARKER "role 'vondst' resolves to the to-one role 'vondst'
        on Melding, whose field a same-named role toward another type already holds — the bean cannot
        carry the reference field (name-sharing bean limit: attribute > characteristic > reference)";
        cases + ref `oracle: PASS (1 expectations)`, clingo `clingo: PASS (1 expectations)`.
    R2  R1 zonder de regel, met:
        Regel telling / geldig altijd /
        Het aantal meldingen van een Wrakvondst moet berekend worden als het aantal meldingen van de Wrakvondst.
        nu: check exit 3, "transpiler SKIP: regel: telling — regel (de aggregatie-join heeft geen
        beanveld — het beanmodel leidt voor de rol 'vondst' op Melding geen referentieveld naar
        Wrakvondst af (§13.3.9 opslagrichting)): telling".

## Acceptatie

- (a) Een rol waarvan de vuller een supertype is, neemt in beide
  Drools-takken een instantie van elk subtype van die vuller (de beans erven
  al via `is een`: het referentieveld van het vullertype is toewijsbaar);
  de case-lezer, de navigatie, de kwantoren en vouwen vanaf het subtype
  (R2) en de verdeling volgen dezelfde regel. Meet de vormen: to-one en
  to-many, lezing vanaf beide kanten, een regel op het subtype en op het
  supertype, twee niveaus `is een`.
- (b) Wat na deze kaart nog niet kan, weigert met een zin die de echte
  oorzaak noemt; de zin "a same-named role toward another type" verschijnt
  niet meer waar geen tweede rol is.
- (c) Goldens (werknaam `jutterswet-wrakmelding`, R1 en R2 en de gemeten
  vormen), handgerekend, in ref ≡ clingo ≡ Drools-exec; `EXPECTED_PLAYED`
  in `scripts/check-drools.sh` als **voorstel**; rood-eerst JUnit-getuigen.
- (d) Inertie: de beans van alle bestaande goldens byte-gelijk, gate 11
  groen, alle zestien gates groen (bash waar de shebang dat zegt).

May touch: `backends/drools/**`, `corpus/golden/<nieuwe goldens>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: parallel met de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (zelfde backend, andere
bestanden); de hoofdsessie landt serieel en rebaset.

## Result (2026-09-28, bouwer)

Branch `card/drools-subtype-in-supertype-rol`, kop van de code `5caf206b`
(daarna alleen de commit van dit Result), gebouwd op `396097de`. De kaart
blijft in `backlog/doing/`; de hoofdsessie landt.

### Wat landde

- `backends/drools/.../schema/FactModelGenerator.java`
  - Het type van een referentieveld is de gedeclareerde vuller van de rol
    (`vullertype`), niet het eerste type dat de afleidingslus tegenkomt. De
    lus bereikt een rol eenmaal per subtype van de vuller; getypeerd naar dat
    type besliste de declaratievolgorde de klasse van het veld.
  - `heeftReferentieVeld` vraagt toewijsbaarheid in plaats van gelijkheid: de
    klasse van het doel zelf (als voorheen), of die van een strikte voorouder
    van het doel, maar dat laatste alleen voor het veld van dezelfde
    roldeclaratie (`Herkomst` van het veld gelijk aan `rolHerkomst(ot, doel,
    rol)`, met een feittype). Een gelijknamig veld van een andere rol blijft
    weigeren, ook als het doel in zijn klasse past.
  - Nieuw: `veld` (het lid dat een bean onder een eigenschap draagt, eigen of
    geërfd, met herkomst), `veldHerkomst` (publiek), `rollenNaar` (de ene
    zoektocht achter `rolHerkomst` en `vullertype`); `rolHerkomst` is publiek.
    `leden()`, `verdringingen()` en `klassebotsing()` zijn ongewijzigd in
    gedrag (`alleLeden` loopt nu via `gededupliceerd`).
- `backends/drools/.../cases/CaseRunner.java`
  - De setter van een to-one-rol wordt gezocht op de klasse van het veld
    (`veldklasse`, de returntype van de getter), niet op die van het doel.
  - De spiegelpas voor een wederkerige rol zoekt de rol langs de keten van de
    bean, zodat de poot van een subtype-instantie gespiegeld wordt.
  - `naamdelingsPoort` leest de oorzaak af aan wat de eigenschap houdt en
    noemt die (zie Weigerzinnen).
- `FactModelGenerator.geenReferentieveldReden` krijgt het model en noemt,
  waar een andere declaratie de beaneigenschap houdt (een rol van een ander
  feittype, of een lid onder een andere spelling), die houder;
  `geenJoinveldReden` zegt "(beide zijden meervoudig)" alleen nog waar de
  zusterrol meervoudig is en geeft anders de reden van het ontbrekende veld.
  In `ExecModelTranspiler` verandert daarvoor alleen de private doorgever
  `geenReferentieveldReden` (nu met `model`, één regel plus de signatuur); in
  `DrlTranspiler` krijgen drie aanroepen `model` mee.
- `corpus/golden/jutterswet-wrakmelding/` (bron.rgs, scenario.rgs, ir.json,
  drie cases, NOTE.md), toegelaten via `scripts/admit.sh`.
- `backends/drools/src/test/.../cases/SubtypeRolvullerWitnessTest.java`, elf
  rijen, beide takken waar het om lowering of een weigerzin gaat.
- `scripts/check-drools.sh` (voorstel): `EXPECTED_PLAYED` 219 -> 220 en
  `EXPECTED_DRL_PLAYED` 188 -> 189.

Niets buiten "May touch" gewijzigd: `backends/drools/**`, de nieuwe golden,
`scripts/check-drools.sh` (voorstel) en de kaartverplaatsing.

### Vormenmeting

Elke vorm is een eigen document (bron.rgs + scenario.rgs, via de frontend van
deze repo naar ir.json en cases), zodat een weigering van de ene vorm de andere
niet verbergt. Uitvoerders van deze repo: `verifier.exe`, `runner.exe` (ref),
`clingo_runner.exe`, de Drools-jar (`java -jar …-shaded.jar [--drl]`). "Voor" is
de jar van `396097de`, "na" die van de branch. Waar niets anders staat: verifier
clean, ref PASS, clingo PASS.

| # | vorm | voor, exec | voor, DRL | na, exec | na, DRL |
|---|---|---|---|---|---|
| v01 | to-one, lezing aan de opslagkant (R1): Wrakvondst in rol `vondst` (vuller Vondst), regel op Melding | EXCLUDE case-lezer, "a same-named role toward another type" | idem | PLAYED 1 | PLAYED 1 |
| v02 | to-many, telling vanaf het subtype (R2) | SKIP aggregatie-join "leidt … geen referentieveld naar Wrakvondst af" | MARKER aggregatie-join, idem | PLAYED 1 | PLAYED 1 |
| v03 | to-many, telling vanaf het supertype, Vondst en Wrakvondst | EXCLUDE case-lezer, idem v01 | idem | PLAYED 1 | PLAYED 1 |
| v04 | to-many, som vanaf het subtype | SKIP aggregatie-join | MARKER aggregatie-join | PLAYED 1 | PLAYED 1 |
| v05 | een-op-een, lezing van beide kanten, regel op subtype en supertype, case van elke kant geschreven | EXCLUDE case-lezer, idem v01 | idem | PLAYED 2 | PLAYED 2 |
| v06 | twee niveaus (Scheepswrak ⊑ Wrakvondst ⊑ Vondst): lezing, telling op subtype en supertype | SKIP aggregatie-join | MARKER aggregatie-join | PLAYED 1 | PLAYED 1 |
| v07 | kwantor (ten minste 2) vanaf het subtype | SKIP "kwantorvouw … de bereik-join heeft geen beanveld" | MARKER kwantorvouw-bereik | PLAYED 1 | PLAYED 1 |
| v08 | verdeling met het subtype als bron | SKIP "verdeling (het beanmodel leidt …)" | MARKER verdeling | PLAYED 1 | PLAYED 1 |
| v09 | verre rolcheck vanaf het subtype ("indien hij een vondst is") | SKIP verre rollidmaatschapscheck | MARKER idem | PLAYED 1 | PLAYED 1 |
| v10 | objectcreatie op het subtype, met connectingRole | SKIP (de meldsom-join) | MARKER | PLAYED 1 | PLAYED 1 |
| v10c | idem zonder connectingRole (typepaarroute) | SKIP | MARKER | PLAYED 1 | PLAYED 1 |
| v11 | feitcreatie met het subtype als anker | SKIP "feitcreatie (het beanmodel leidt …)" | MARKER feitcreatie | PLAYED 1 | PLAYED 1 |
| v12 | subtype vóór zijn supertype gedeclareerd, gewone Vondst in de rol | SKIP genavigeerde lezing "leidt … geen referentieveld naar Vondst af" (veld getypeerd Wrakvondst) | MARKER idem | PLAYED 1 | PLAYED 1 |
| v13 | wederkerige rol `maat` (vuller Vondst), poot op de Wrakvondst | EXCLUDE "wederkerig self-pair role 'maat' on Vondst shares its name with a declared member" (veld getypeerd Wrakvondst) | idem | PLAYED 1 | PLAYED 1 |
| v14 | WF-102-pad (som over twee stappen) vanaf het subtype | SKIP "sourcePath-stap 0 … (beide zijden meervoudig)" | MARKER idem | PLAYED 1 | PLAYED 1 |
| v15 | instantie-unie-telling vanaf het subtype | SKIP "de join van een instantie-unie-selectie heeft geen beanveld" | MARKER instantie-unie-join | PLAYED 1 | PLAYED 1 |
| x1 | bindingloze objectcreatie op het subtype, twee feittypen (ref en clingo: FAULT "more than one fact type relates \"Wrakvondst\" to \"Melding\"") | SKIP (de meldsom-join) | MARKER | SKIP "geen uniek verbindend feittype: de terugkoppeling is niet af te leiden en ch. 5.5 gist niet" | MARKER idem |
| x2 | rol `half stok` op de beaneigenschap van attribuut `half-stok` (Sein) | EXCLUDE case-lezer, "a same-named role toward another type" | idem | EXCLUDE case-lezer, "whose bean property Sein.halfStok belongs to the attribute 'half-stok' of Sein" | idem |
| x3 | hek: twee rollen `ligger`, Vlot ⊑ Ponton, lezing over `vlotligging` | SKIP genavigeerde lezing "het beanmodel leidt voor de rol 'ligger' op Schuit geen referentieveld naar Vlot af" | MARKER idem | SKIP, zin noemt de houder (zie Weigerzinnen) | MARKER idem |
| x4 | WF-102-pad zonder subtypen; het veld `Melding.vondst` van de enkelvoudige zusterrol is genomen door het attribuut `vondst` | SKIP "… (beide zijden meervoudig)" | MARKER idem | SKIP "… en dat veld ontbreekt: de rol 'vondst' op Melding deelt haar naam met een gedeclareerd lid …" | MARKER idem |

Twee tussenmetingen tonen dat elk deel van de case-lezer nodig is: zonder de
setteropzoeking op de veldklasse gaf v01 in beide takken `ERROR
RuntimeException: kon setter setVondst(Wrakvondst) niet aanroepen op Melding`;
zonder de ketenloop in de spiegelpas gaf v13 in beide takken `FAIL …
attribuut maatwaarde: verwacht 5, werkelijk leeg`.

### Weigerzinnen, letterlijk

De case-lezer (`CaseRunner.naamdelingsPoort`) heeft nu vijf oorzaken; de zin
"a same-named role toward another type" bestaat niet meer. Vorm:
`role '<geschreven>' resolves to the to-one role '<rol>' on <Type>, <oorzaak> — the
bean cannot carry the reference field (name-sharing bean limit: attribute >
characteristic > reference)`, met als `<oorzaak>`:

1. `which shares its name with a declared member` (ongewijzigd)
2. `whose bean property <Klasse>.<eigenschap> belongs to the attribute '<a>' of <T>`
   (of `the characteristic '<k>' of <T>`)
3. `whose field <Klasse>.<eigenschap> another role already holds, the role '<r>' of fact type '<F>'`
4. `for which the bean model derives no field`
5. `whose field <Klasse>.<eigenschap> cannot hold a <T>` (verdedigend; geen gemeten vorm bereikt hem)

Gemeten:

    corpus/rood/ambiguous-case-role-key (gate 8, EXCLUDE ongewijzigd):
    EXCLUDE role 'ligger' resolves to the to-one role 'ligger' on Schuit, whose field Schuit.ligger another role already holds, the role 'ligger' of fact type 'pontonligging' — the bean cannot carry the reference field (name-sharing bean limit: attribute > characteristic > reference)
    x2 (beide takken):
    EXCLUDE role 'half stok' resolves to the to-one role 'half stok' on Sein, whose bean property Sein.halfStok belongs to the attribute 'half-stok' of Sein — the bean cannot carry the reference field (name-sharing bean limit: attribute > characteristic > reference)

De transpilers (`FactModelGenerator.geenReferentieveldReden`, nu met het
model) hebben een derde oorzaak: `de rol '<rol>' op <T> heeft geen eigen
referentieveld naar <D>: de beaneigenschap <Klasse>.<eigenschap> draagt
<beschrijving van de houder>`. De twee bestaande zinnen (`… deelt haar naam
met een gedeclareerd lid …` en `het beanmodel leidt voor de rol … geen
referentieveld naar … af`) zijn ongewijzigd. De drie goldens die "(beide
zijden meervoudig)" tonen (`anchor/jutterswet-erflijn`,
`anchor/jutterswet-bootberging`, `jutterswet-muntketen`) zijn beide kanten
meervoudig en houden hun statusregel byte-gelijk.

Wat na deze kaart nog niet kan in deze familie, gemeten, na:

    x3, exec: EXCLUDE transpiler SKIP: regel: ligdiepte van een schuit — regel (genavigeerde lezing: de rol 'ligger' op Schuit heeft geen eigen referentieveld naar Vlot: de beaneigenschap Schuit.ligger draagt de rol 'ligger' (feittype 'pontonligging') (§13.3.9 opslagrichting)): ligdiepte van een schuit
    x3, DRL:  EXCLUDE drl-transpiler MARKER: regel: ligdiepte van een schuit — genavigeerde lezing: de rol 'ligger' op Schuit heeft geen eigen referentieveld naar Vlot: de beaneigenschap Schuit.ligger draagt de rol 'ligger' (feittype 'pontonligging') (§13.3.9 opslagrichting)
    x4, exec: EXCLUDE transpiler SKIP: regel: bijlagengewicht van een vondst — regel (sourcePath-stap 0 (Vondst -> Melding) heeft geen beanveld — de rol 'melding' op Vondst is meervoudig, dus haar opslag is het veld aan de overzijde, en dat veld ontbreekt: de rol 'vondst' op Melding deelt haar naam met een gedeclareerd lid — het beanveld draagt dat attribuut/kenmerk en niet de referentie (leden(): attribuut > kenmerk > referentie), dus de koppeling naar Vondst heeft geen veld (§13.3.9 opslagrichting)): bijlagengewicht van een vondst
    x1, exec: EXCLUDE transpiler SKIP: objectcreatie: meld elke wrakvondst — objectcreatie (geen uniek verbindend feittype: de terugkoppeling is niet af te leiden en ch. 5.5 gist niet, dus het gecreëerde object zou onbereikbaar zijn): meld elke wrakvondst

x3 vóór deze kaart zei "het beanmodel leidt voor de rol 'ligger' op Schuit
geen referentieveld naar Vlot af" (waar, zonder oorzaak); x4 zei "(beide
zijden meervoudig)" terwijl de zusterrol enkelvoudig is. x1 weigert waar ref
en clingo een fout geven, om dezelfde reden. Buiten x1 tot x4 speelt elke
gemeten vorm in beide takken.

### Golden `jutterswet-wrakmelding`, replay per uitvoerder

Tien regels (negen plus een verdeling) over `vondst heeft melding` (to-many) en
`vondst heeft rapport` (een-op-een), drie scenario's in `scenario.rgs`,
handgerekend in NOTE.md; `ir.json` is gelijk aan de emissie van `bron.rgs`
alleen (gate 10) en aan die met `scenario.rgs` (gate 16).

    verifier: PASS
    case1-een-wrakvondst-in-de-rol-van-de-vondst:        oracle: PASS (21 expectations)  clingo: PASS (21 expectations)
    case2-een-scheepswrak-twee-niveaus-onder-de-vuller:  oracle: PASS (10 expectations)  clingo: PASS (10 expectations)
    case3-een-rapport-van-beide-kanten:                  oracle: PASS (8 expectations)   clingo: PASS (8 expectations)
    Drools exec-model: PLAYED 3        Drools DRL-tekst: PLAYED 3
    voor de wijziging, beide takken: EXCLUDE (transpiler SKIP / drl-transpiler MARKER: regel: aantal meldingen van een wrakvondst — … aggregatie-join … geen referentieveld naar Wrakvondst af)
    admit: ADMITTED corpus/golden/jutterswet-wrakmelding (3 case(s); verifier-clean, ref = clingo, no-op refuted, gates green)

De objectcreatie, de feitcreatie, de wederkerige rol en de
declaratievolgorde staan niet in de golden maar als getuigen op varianten
ervan: een creatie voegt meldingen toe die de tellingen van de golden
veranderen, en de volgorde en de wederkerige rol zijn beanmodelvragen. Die
varianten zijn gemeten in alle uitvoerders: verifier clean, ref PASS, clingo
PASS, voor EXCLUDE in beide takken, na PLAYED in beide takken (volgorde:
PLAYED 3).

### Getuigen (rood-eerst)

`SubtypeRolvullerWitnessTest`, elf rijen: (1) wat de case-lezer van de invoer
maakt (veldklasse Vondst, de bean van W zelf in `M1.vondst`; de tweede
schrijf van ch. 5.7a op de een-op-een, van beide kanten en twee niveaus
diep), (2) de drie cases in beide takken, (3) de veldklasse in elke
declaratievolgorde, (4) toewijsbaarheid voor dezelfde rol, (5) het hek tegen
het veld van een andere rol, met de zin die de houder noemt, (6) de
wederkerige rol, (7) objectcreatie en (8) feitcreatie op het subtype, (9) en
(10) de weigerzin van de case-lezer die de houder noemt (een andere rol, een
lid onder een andere spelling), (11) de kardinaliteitszin van een pad (x4).

Rood-eerst, in twee stappen gemeten:

    tegen de hoofdbestanden van bd56bb19 (claim, code = 396097de), rij 1-10:
    [ERROR] Tests run: 10, Failures: 4, Errors: 5, Skipped: 0 … in dev.uchar.regelspraak.cases.SubtypeRolvullerWitnessTest
    (rij 5 groen: haar weigering stond er al)
    tegen de hoofdbestanden van 1f0a825e (typefix, zinnen nog oud), rij 1-11:
    [ERROR] Tests run: 11, Failures: 2, Errors: 0, Skipped: 0 … (rij 5 en rij 11)

Op de kop: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`; de hele suite
862 -> 873, groen.

### Gewijzigde scriptregels (voorstel, commit `9c5b072c`)

`scripts/check-drools.sh`: `EXPECTED_PLAYED=219` -> `EXPECTED_PLAYED=220` met
elf regels reden erboven, en `EXPECTED_DRL_PLAYED=188` ->
`EXPECTED_DRL_PLAYED=189` met twee regels reden. Geen andere regel. De kaart
drools-exec-voorwaarde-genavigeerde-afleiding raakt dezelfde tellers; bij het
landen tellen beide op.

### Inertiemeting

- Beans: `FactModelGenerator.genereer` over alle 346 documenten onder
  `corpus/golden`, `corpus/rood` en `toka` (303 met beans, 43 weigert de
  lader al) voor en na: `diff -r` leeg, byte-gelijk. Op de meetvormen
  verandert alleen wat moest: v12 `private Wrakvondst vondst;` -> `private
  Vondst vondst;`, v13 `private Wrakvondst maat;` -> `private Vondst maat;`.
- Replay: elke bestaande golden (277) door beide takken met de jar van voor
  en die van de eindcode: statusregels identiek, exec 219 PLAYED / 54 EXCLUDE /
  1 FAIL / 3 NOCASE, DRL 188 PLAYED / 84 EXCLUDE / 2 FAIL / 3 NOCASE; de enige
  nieuwe regel is `jutterswet-wrakmelding PLAYED 3`, in beide takken. De beans
  zijn op de eindcode opnieuw gemeten: byte-gelijk over de 346 documenten.
- Gate 11: zie Gates.

### Afwijkingen van de kaart

1. Ook `EXPECTED_DRL_PLAYED` gaat omhoog (188 -> 189): de DRL-tak speelt de
   golden en de gelijkheidspin eist het.
2. Het veldtype naar de vuller (`vullertype`) staat niet letterlijk in de
   kaart maar valt onder (a), "het referentieveld van het vullertype":
   gemeten nodig voor v12 en v13, beans van bestaande documenten byte-gelijk.
3. Gemeten vormen buiten de lijst van (a): objectcreatie, feitcreatie, verre
   rolcheck, WF-102-pad, instantie-unie, declaratievolgorde, wederkerig. Reden:
   `heeftReferentieVeld` is de gedeelde poort van al die lezers, dus de
   versoepeling raakt ze allemaal; alle spelen, zie de tabel.
4. De spiegelpas voor een wederkerige rol (case-lezer) is aangepast; zonder
   die aanpassing gaf de typecorrectie een stil verkeerde waarde (v13, "verwacht
   5, werkelijk leeg").
5. De zinnen van de transpilers (via `FactModelGenerator`) zijn aangescherpt,
   ook voor x4, een vorm zonder subtypen; reden: (b), de zin moet de echte
   oorzaak noemen, en v14 gaf vóór de typecorrectie diezelfde onjuiste
   "(beide zijden meervoudig)". Gemeten: geen golden verandert van
   statusregel in een van beide takken; de bestaande getuigen die de oude
   zinnen vastpinnen blijven groen (hun vormen hebben geen houder). In
   `ExecModelTranspiler`, dat de parallelle kaart bewerkt, verandert alleen
   de private doorgever.

### Gates

Alle zestien, in de volgorde van `.github/workflows/gates.yml`, op `5caf206b`,
elk onder de interpreter van zijn shebang (bash: catala, regelspraak,
toka-parity, drools, flogic, graaf, scenario; sh: de rest), na
`eval $(opam env)`; alle zestien rc=0. De verdictregels letterlijk:

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 317 file(s) validate and round-trip, floor 192; 41574 object(s) in 818 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 788 case file(s) validate)
    check-verifier: PASS (317 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 748 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    check-clingo: PASS (backend suite 1988 checks, floor 83; 748 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (704 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: Drools leg PASS (12 fault-pinned, 28 not-carried with reason)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (969 tests, floor 173); 105 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity: PASS
        Drools : 29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR
    check-drools: PASS
      played 220  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
      auto-excluded 54  (per-form refusals: loader / transpiler marker / contract / unsupported input)
      value-divergence 1  (listed with reason — the exec-model differs from ref/clingo)
      no-case 3  (goldens without a case file)
      channel floor 58 non-PLAYED goldens pinned by name; played matches the pinned 220
      DRL-text branch played 189  (its OWN count — the narrower branch, not the square; pin 189)
      DRL-text floor 89 non-PLAYED goldens pinned by name; sweep cost 153s wall clock
      JUnit 873 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: spec leg — 241 of 278 goldens compared, 131 dependencies equal to the verifier's ch. 5 edges (floors 234 / 116), 3 listed difference(s)
    check-graaf: route leg — the graph of the wire equals the graph of the source on all 105 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)
    check-graaf: PASS
    check-scenario: PASS — 12 scenario golden(s) (floor 7) regenerate 47 case file(s) (floor 31) and their ir.json byte-identically

Basislijn op `bd56bb19` (de claim, code gelijk aan `396097de`), vooraf in een
aparte kloon gedraaid: ook alle zestien rc=0 (played 219, DRL 188, JUnit
862). Verschil met de eindrun, alleen door de nieuwe golden en de getuigen:
gates 2 tot en met 7 tellen één document en drie cases meer (317 documenten,
788 casebestanden, 748 cases in ref en clingo, 704 no-op-weerleggingen);
gates 10 en 15 één rgs-golden meer (105); gate 15 vergelijkt 241 van 278
goldens (240 van 277), met 131 afhankelijkheden (de golden heeft geen regel die
de uitvoer van een andere regel leest); gate 16 twaalf scenario-goldens met 47
cases (11 en 44); gate 12 op 220 / 189 / 873.

### Open punten

1. Landen: `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` in
   `scripts/check-drools.sh` worden ook door de kaart
   drools-exec-voorwaarde-genavigeerde-afleiding verzet; de hoofdsessie telt
   beide stappen op. In `ExecModelTranspiler` raakt deze branch alleen de
   doorgever `geenReferentieveldReden` (rond regel 944).
2. De distributie draait op de oude pin: R1 en R2 via de CLI blijven weigeren
   tot de submodulepin voorbij deze branch schuift.
3. x3 (twee gelijknamige rollen in twee feittypen op één houder) blijft een
   grens van het beanmodel: één veld per eigenschap. De zin noemt nu de
   houder; dragen vraagt een beanmodel met een veld per roldeclaratie.
4. Geen CONSTRUCTS- of PLAN-rij voorgesteld: geen IR-delta, alleen het
   beanmodel en de weigerzinnen van deze backend.

### Commits

    bd56bb19 claim: kaart drools-subtype-in-supertype-rol naar doing
    4c353c74 beanmodel: een subtype-instantie past in de rol van haar vuller
    9716c768 golden jutterswet-wrakmelding: een subtype in de rol van de vuller
    9c5b072c voorstel gate 12: jutterswet-wrakmelding speelt in beide takken
    d0cedd0d getuigen: een subtype in de rol van zijn vuller, beide takken
    1f0a825e getuige: de weigerzin noemt het lid dat de eigenschap houdt
    5caf206b weigerzin van het beanveld noemt wie de eigenschap houdt

en daarna de commit die dit Result in de kaart zet.

## Keuring (hoofdsessie, 2026-09-29)

Geland op main na een rebase op de landing van de kaart
frontend-extensie-herdeclaratie (geen conflicten) en een eigen run van
alle zestien gates op de rebased kop `95d2c42d` (logs
`/tmp/v2/uchar-gates4/`): groen. Gemeten daar: gate 3 317 goldens schoon;
gate 5 en 6 748 cases exact; gate 7 704 weerleggingen, 44 overlevers
(ongewijzigd); gate 10 990 toetsen; gate 12 speelt 220 en 189 (beide
pinnen één omhoog, voorstel van de bouwer, hier geland); gate 16 12
scenario-goldens, 47 casebestanden.

Gelezen: het referentieveld van een rol krijgt het type van de
gedeclareerde vuller in plaats van het eerste type in declaratievolgorde,
en `heeftReferentieVeld` laat een strikt subtype toe in het veld van
dezelfde roldeclaratie; de case-lezer, de transpilers en de spiegelpas
van de wederkerige rol volgen; de zestien gemeten vormen (to-one en
to-many, beide kanten, regel op subtype en supertype, twee niveaus,
verdeling, objectcreatie, feitcreatie) met hun vóór-meting (alle zestien
weigerden, ref en clingo speelden); de elf getuigenrijen; de golden met
handberekening en `scenario.rgs`; de beans van 346 documenten byte-gelijk.
De afwijkingen van de kaart aanvaard met hun meting: `EXPECTED_DRL_PLAYED`
ook één omhoog (de teksttak speelt de golden), het veldtype volgt de
vuller (nodig voor de declaratievolgorde en de wederkerige rol), de
spiegelpas aangepast (anders een stil verkeerde waarde), de redenzinnen
aangescherpt. De misleidende zin "a same-named role toward another type"
is weg; twee gelijknamige rollen op één houder blijven een benoemde grens
van het beanmodel.

Gevolg voor de regelset op de werkbank: brug A8 (drie feittypen versmald
tot het subtype) kan terug naar de modelkaart zodra de pin verschuift.
Pin-bump samen met de kaart drools-exec-voorwaarde-genavigeerde-afleiding.
