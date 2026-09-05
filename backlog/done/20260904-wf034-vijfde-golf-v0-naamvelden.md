# WF-034, vijfde golf: de v0-naamvelden buiten de expressiewandeling

Goal: de vierde golf sloot de naamresolutie op de verdeling en bracht de
ch. 4.3-terugval terug binnen zijn scoping; de lenzen erop maten drie
naamvlakken die de wandeling nog steeds niet raakt, plus één gat in WF-028.
Alle vier zijn verifier-schoon gemeten en alle vier hebben een uitvoerder die
er anders mee omgaat dan de andere twee. Route als de vorige golven: een
WF-034-clausule per vlak (voor het WF-028-gat een arm op WF-028 zelf), elk
rood-eerst met eigen negatief, arm-exact mutant-bewezen, nul over-afwijzing
over de vier zones.

**R1 (zwaarst, voorrang) — de vier naamvelden van een `ResolvedFeitCreatie`.**
`zetVeld`, `joinVeld`, `tussenstappen[].type` en `tussenstappen[].veld` zijn
namen die nergens tegen een declaratie worden gehouden: **14 golden-posities**
(4 + 4 + 3 + 3, over `anchor/jutterswet-erflijn`,
`anchor/jutterswet-gemeenteberging` en `jutterswet-postwacht`). Gemeten, elk
veld één voor één op `bestaatniet`:

    verifier   14/14 PASS
    ref        14/14 CLEAN — replay tot de gepinde verwachtingen
    clingo     14/14 CLEAN — idem

Beide OCaml-uitvoerders zijn hier *blind*: zij lezen de symbolische velden
(`feittype`, `ankerRol`, `doelRol`, `joinRol`, `tussenstappen[].rolBinding`)
en raken de beanvelden niet aan, dus een onzinnige naam verandert hun antwoord
niet en er is geen verschil om te meten. De Drools-backend leest ze wél, en
reflectief; op `jutterswet-postwacht` (baseline: PLAYED) crasht hij op elk van
de vier:

    zetVeld              IllegalStateException: geen getter voor 'bestaatniet'
                         op ...facts.Sein   (via ConsequenceException)
    joinVeld             ConstraintEvaluationException: Error evaluating
                         constraint '' in  and in more rules
    tussenstappen[].veld ConstraintEvaluationException, idem
    tussenstappen[].type NullPointerException: Cannot read field "naam"
                         because "ot" is null

Dat is exact de vorm die deze regelfamilie het scherpst motiveert en die
`wf034-regeldoel-zonder-type` al eens droeg: **verifier-schone invoer die een
uitvoerder laat crashen**. Nu vier keer, op één knooppunt, en met een
golden die de gate speelt.

**Het WF-028-gat (voorrang, naast R1).** Een `ResolvedFeitCreatie` waarvan
*alle* extensiesleutels weg zijn — `feittype`, `ankerRol`, `doelRol`,
`joinRol` en `tussenstappen[].rolBinding` — is een kale v0-beanvorm. Gemeten
op `jutterswet-postwacht`:

    verifier   PASS
    ref        EVALUATION FAULT: fact creation "..." carries only bean fields;
               the symbolic form (IR-SPEC ch. 3.4) is what executes
    clingo     TRANSLATION FAULT: dezelfde zin
    Drools     PLAYED 1  — hij speelt hem gewoon, want hij leest juist die
               beanvelden

Twee uitvoerders weigeren het document, de derde speelt het, en de verifier
laat het door: WF-028 mist een geen-extensievelden-arm. Dit is bovendien de
enige plek in dit blok waar de drie uitvoerders het over het *bestaan* van het
document oneens zijn.

**R2 — `objectCreaties[].subject` op een toekenningsloze creatie.** Van de 11
golden-objectcreaties heeft er één geen `toekenningen`
(`anchor/jutterswet-penninguitgifte`, creatie 1). Daar is `subject` het enige
wat de creatie nog aan een objecttype bindt, en niemand kruist het. Gemeten
met `subject: "Bestaatniet"`:

    verifier   PASS
    ref        MISMATCH: J1.aantal dossiers = 0, expected 1
    clingo     MISMATCH: J1.aantal dossiers = [0], expected 1

Geen van beide faalt; ze zijn het eens over hetzelfde **foute** antwoord, op
een **anker**-golden. Dat is de rij waar het differentiële vierkant blind is
en alleen de handgepinde verwachting nog vangt. Gemeten over alle elf:
**1 van de 11** blijft verifier-schoon, precies de toekenningsloze. De tien
andere worden alsnog afgewezen — maar door **WF-053**, die de navigatie in de
toekenningswaarde niet meer kan binden zodra het subject een onbekend type
noemt. Zij zijn dus incidenteel gedekt, niet door een regel die over dit veld
gaat; die dekking valt weg zodra een creatie geen navigerende toekenning
draagt.

**R3 — `Rollidmaatschap.anderTeFiller`.** Vier golden-posities
(`jutterswet-wachtindeling` 2, `jutterswet-wachtploeg` 2). Gemeten met
`Bestaatniet`: **4/4 verifier-schoon**, en beide uitvoerders luid —
`ref` "EVALUATION FAULT: no fact type relates \"Jutter\" to \"Bestaatniet\"",
clingo dezelfde zin als TRANSLATION FAULT. De goedkoopste van de drie: één
kruising tegen de rolvullers, en de vorm is dicht.

Acceptance:
- Per vlak minstens één negatief, rood-eerst zichtbaar (PASS onder de
  verifier van vóór de kaart, één regel erna), met de arm-exacte mutant
  erbij: alleen die arm weg → alleen het eigen document schoon, over de
  volle vier-zone-sweep.
- R1 krijgt vier getuigen (één per veld) of een gemeten argument waarom
  minder volstaat; de crashende vorm wordt met de exception-naam vastgelegd,
  zoals `wf034-regeldoel-zonder-type` dat doet.
- Het WF-028-gat krijgt zijn eigen negatief onder `wf028-`, want de regel die
  hem afwijst is WF-028 en niet WF-034.
- Sweeps vóór/na met nul over-afwijzing; de tellingen in IR-SPEC (vier zones,
  negatiefzone) en de open-lijst van de WF-034-indexrij worden in dezelfde
  ronde bijgewerkt — een golf die zijn eigen tellingen laat verlopen is de
  fout die de fixronde van de vierde golf moest herstellen.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: uitvoerders, frontends, goldens; de rij-1-wrat en zijn
restwijdte (acht posities, twee golden — geteld in de vierde golf); de
sentinel op de 9 fold-/aggregatie-argumenttypes; WF-007's
payload-blindevlek; het herstellen van de Drools-crashes zelf (de kaart maakt
de invoer ill-formed, zij repareert geen backend).

---

## Result (2026-09-05)

Geland: vier vlakken, vier ingrepen, **zeven** getuigen. Alle dertien
poorten groen; negatiefzone **287 → 294**, vier zones **493 → 500**.

**Rood-eerst, gereproduceerd** (verifier van 0879e0b, uitvoerders van
vandaag).

*R1 — de vier v0-beanvelden.* Alle 14 golden-posities (4 `zetVeld`,
4 `joinVeld`, 3 `tussenstappen[].type`, 3 `tussenstappen[].veld`, over
`anchor/jutterswet-erflijn`, `anchor/jutterswet-gemeenteberging` en
`jutterswet-postwacht`), elk veld een voor een op `bestaatniet`:

    verifier   14/14 PASS
    ref        14/14 schoon — replay tot de gepinde verwachtingen
    clingo     14/14 schoon — idem

Beide OCaml-uitvoerders zijn hier blind per constructie. De
beanmodel-uitvoerder leest ze reflectief; op `jutterswet-postwacht`
(baseline `PLAYED 1`) crasht hij op elk van de vier, letterlijk:

    zetVeld    ERROR ConsequenceException: Exception executing consequence
               for rule "vakbeseining door het kustvak" in defaultpkg:
               java.lang.IllegalStateException: geen getter voor
               'bestaatniet' op io.github.yhavinga.regelspraak.facts.Sein
    joinVeld   ERROR ConstraintEvaluationException: Error evaluating
               constraint '' in  and in more rules
    ts[].veld  ERROR ConstraintEvaluationException: Error evaluating
               constraint '' in  and in more rules
    ts[].type  ERROR NullPointerException: Cannot read field "naam"
               because "ot" is null

*WF-028-gat — de kale v0-beanvorm* (alle vijf extensiesleutels weg op
`jutterswet-postwacht`):

    verifier   PASS
    ref        EVALUATION FAULT: fact creation "vakbeseining door het
               kustvak" carries only bean fields; the symbolic form
               (IR-SPEC ch. 3.4) is what executes
    clingo     TRANSLATION FAULT: dezelfde zin
    beanmodel  PLAYED 1

*R2 — `objectCreaties[].subject`.* Over alle 11 golden-objectcreaties op
`Bestaatniet`: **1 van de 11** blijft verifier-schoon, precies de
toekenningsloze (`anchor/jutterswet-penninguitgifte`, creatie 1). De tien
andere worden alsnog afgewezen — **alle tien door WF-053, negen ook door
WF-047** (dat laatste is de enige verfijning t.o.v. de kaart, die alleen
WF-053 noemde). Op de elfde:

    verifier   PASS
    ref        MISMATCH: J1.aantal dossiers = 0, expected 1
    clingo     MISMATCH: J1.aantal dossiers = [0], expected 1
    beanmodel  SKIP (om een andere reden — een gerichte telling die het
               v0-beanmodel niet uitdrukt)

*R3 — `Rollidmaatschap.anderTeFiller`.* 4/4 verifier-schoon; ref
"EVALUATION FAULT: no fact type relates \"Jutter\" to \"Bestaatniet\"",
clingo dezelfde zin als TRANSLATION FAULT. **Extra bevinding, niet op de
kaart:** de beanmodel-uitvoerder neemt een derde weg — van `PLAYED 1`
naar een transpiler-SKIP ("voorwaarde buiten Phase-B scope"), dus het
document faalt niet én antwoordt niet. Drie uitvoerders, drie
behandelingen.

**De clausules.** Vier ingrepen in `spec/verifier/lib/checks.ml`:
`wf034_feitcreatie_beanvelden` (nieuw, vier armen), de
geen-extensievelden-arm van `wf028`, `wf034_objectcreatie_subject`
(nieuw) en de `Rollidmaatschap`-arm van `walk_cond`. Het toetsingskader
voor R1 is afgeleid uit de bestaande WF-028-machinerie: een binaire
relatie leeft in het v0-beanmodel als ÉÉN referentieveld aan de
opgeslagen kant, genoemd naar een rol van die relatie, dus elk beanveld
spelt een **gedeclareerde rol** van de relatie die zijn symbolische
zuster noemt (`zetVeld` → `feittype`, `joinVeld` → `joinRol`'s,
`tussenstappen[j].veld` → die `rolBinding`'s); `tussenstappen[j].type`
kruist tegen de hop zelf (de vuller van de gebonden rol, via dezelfde
`resolve_binding`). Beide rollen worden toegelaten en geen aangenomen.
*(Beide helften van die laatste zin zijn in de fixronde herzien — zie
daar. De motivering is weerlegd, en de meting die hem droeg was fout: per
CREATIE is het **twee en twee**, niet drie en een. `anchor/jutterswet-
erflijn` draagt twee creaties, beide met de doelrol; `jutterswet-postwacht`
en `anchor/jutterswet-gemeenteberging` spellen de ankerrol. De zin telde
documenten voor `zetVeld` en creaties voor `joinVeld`.)* `joinVeld` is
drie keer de gebonden en een keer de tegenoverliggende rol — dat deel
klopt. Alleen de gedeclareerde naam bindt (het meervoud is presentatie,
als bij WF-047).

**Vier getuigen voor R1**, zoals de kaart vroeg, alle vier op
`jutterswet-postwacht` — de enige van de drie die de beanmodel-poort
speelt, zodat elke crash een feit over een corpusdocument is.

**Sweep, vier zones, 500 documenten** (154 golden + 22 rood + 294
negatief + 30 pariteit; de kaart schreef 176 golden, dat is
golden+rood — zelf gemeten en zo gerapporteerd). Over de **493**
documenten die de kaart voorgingen beweegt **geen enkel oordeel en geen
enkele melding** — strenger dan golf 3 en 4, die elk nog incidentele
regels verplaatsten.

**Mutantbewijs, arm-exact** (elk: eigen build + volledige vier-zone-sweep
van 500 documenten). Zeven mutanten, zeven keer precies één bewogen
document — het eigen — en nergens anders een oordeel of een melding:

    A  zetVeld-regel weg                 -> 1 (het eigen)
    B  joinVeld-regel weg                -> 1 (het eigen)
    C  tussenstap-type-arm uit           -> 1 (het eigen)
    D  tussenstap-veld-regel weg         -> 1 (het eigen)
    E  WF-028 kale-vorm-arm weg          -> 1 (het eigen)
    F  wf034_objectcreatie_subject uit   -> 1 (het eigen)
    G  Rollidmaatschap-arm uit           -> 1 (het eigen)

Clausule-exact: beide nieuwe top-level clausules inert → precies hun vijf
getuigen schoon, verder niets.

**IR-SPEC** (voorstel, beschermd pad): WF-028's clausule verliest zijn
"blijft ontoetsbaar"-ontsnapping en krijgt de presence-eis plus de
meting; WF-034 ch. 4.6 krijgt **vijfde golf (m)–(o)**; de ch. 3.4
migratiealinea zegt waar het hek van de superseded beanvelden staat; de
indexrijen WF-028 en WF-034 zijn bijgewerkt met de zeven getuigen; de
open-lijst krijgt de nullhelft van (n) erbij; twee meetnoemers zijn
gedateerd (de 174 golden+rood van 2026-09-04, nu 176; de 492 vier-zone-
documenten van 2026-09-05, nu 500).

**Bewust opengelaten en benoemd** (niet stil): `objectCreaties[].subject`
op `null` — gemeten verifier-schoon met beide uitvoerders luid ("missing
ObjectCreatie.subject") — is de lege-referentieklasse van de eerdere
golven, een andere vraag dan resolutie, en heeft nog geen getuige. Idem
niet aangeraakt: een `anderTeFiller` waarvan de kandidatenlijst
MEERVOUDIG is (ch. 4.1-ambiguïteit, WF-048's zaak) en een `rol`-naam die
nergens tegen te houden is (het veld is presentatie; de uitvoerders lezen
het niet).

**Dertien poorten, uitslagen:**

     1 check-self-contained        PASS
     2 check-ir-schema             PASS (176 bestanden valideren + round-trippen)
     3 check-verifier              PASS (176 golden schoon; 294 negatief afgewezen
                                        voor hun eigen regel) — was 287
     4 check-case-schema           PASS (427 casebestanden)
     5 check-oracle                PASS (405 cases exact; 2 goldens zonder case)
     6 check-clingo                PASS (405 cases; ref = clingo)
     7 check-noop                  PASS (369 weerleggen de nietsdoener; 36 gepinde
                                        overlevers)
     8 check-rood                  PASS (22 rood; Drools-been 5 fault-pinned,
                                        17 not-carried met reden)
     9 check-catala                PASS (suite 201 checks; 12 catala-goldens
                                        byte-identiek)
    10 check-regelspraak           PASS (JUnit 171 tests; 30 rgs-goldens
                                        byte-identiek)
    11 check-toka-parity           PASS (29 cases: ref 29, clingo 29, Drools 29
                                        PLAYED, nul stille divergenties)
    12 check-drools                PASS (played 108; 1 value-divergence met reden;
                                        2 no-case; JUnit 199 tests)
    13 check-reftype-derivation    PASS (64 annotaties gedraaid over 13 goldens;
                                        58 replays; 0 gepinde overlevers)

**Afwijkingen van de kaart:** (1) de vier-zone-teller is **500**, niet
515 — de kaart telde rood dubbel (176 = golden+rood, niet golden alleen);
zelf gemeten, zoals de kaart opdroeg. (2) De negatiefzone gaat naar
**294**, binnen de verwachte "287→293±". (3) R2's incidentele dekking is
WF-053 **plus WF-047** (negen van de tien), niet WF-053 alleen. (4) R3
levert een derde uitvoerdersgedrag op dat de kaart niet had: de
beanmodel-transpiler laat de regel vallen.

---

## Fixronde (2026-09-05)

Twee lenzen bevestigden de landing — 7/7 mutanten arm-exact
gereproduceerd, alle metingen letterlijk — en legden daarnaast één
concessie, één stil gat en zeven tekstfouten bloot. Alles hieronder is
zelf hermeten; de negatiefzone gaat **294 → 297**, de vier zones **500 →
503**.

### 1. De aanscherping: de opslagrichting is wél afleidbaar

De vijfde golf liet *beide* rollen toe met de reden "welke van de twee
het beanmodel opslaat is een beanmodelbeslissing die de wire niet
draagt". Die reden is **weerlegd door de backend in deze repo**: hij
leidt de kant symbolisch af. Een binaire relatie leeft in het
v0-beanmodel als **één scalair referentieveld**, aan het objecttype dat
de **to-many** rol vult, genoemd naar de **to-one** rol — §13.3.9
opslagrichting, wat `FactModelGenerator.leden` rendert en wat de
transpiler in zijn eigen skip-redenen bij naam citeert.

Drie zij-aan-zij armen, in cascade zodat elk apart falsifieerbaar is:

    A  een GEDECLAREERDE rol van die relatie          (bestaand)
    B  waar de relatie precies één to-one rol draagt,
       IS het die rol                                  (nieuw)
    C  in de GEDECLAREERDE spelling, byte voor byte    (nieuw)

**De ontsnappingstabel, vóór en ná.** Alle beanveld-naamposities over de
vier zones, geclassificeerd naar de arm die ze zou raken:

    vóór (500 documenten, 36 naamposities)
      to-one rol genoemd, byte-exact ........ 21
      gelijke cardinaliteit (arm B staat af) .. 8
      feittype ontbreekt (WF-028's melding) ... 4
      arm A (de drie golf-5-getuigen) ......... 3
      arm B .................................... 0   <- nul kosten
      arm C .................................... 0   <- nul kosten
      afwezig .................................. 0   <- nul kosten

    ná (503 documenten, 45 naamposities, 13 type-posities)
      to-one rol genoemd, byte-exact ........ 27
      gelijke cardinaliteit ................... 8
      feittype ontbreekt ...................... 4
      arm A ................................... 3
      arm B ................................... 1   <- de eigen getuige
      arm C ................................... 1   <- de eigen getuige
      afwezig ................................. 1   <- de eigen getuige

**Bewust niet verder.** De sterkere regel — altijd één kant — zou
`anchor/jutterswet-erflijn` (feittype `erflijn`, beide rollen to-many) en
`anchor/jutterswet-gemeenteberging` (join over `aanmelding`, idem)
**ill-formed** maken. Bij gelijke cardinaliteit leidt het model geen kant
af (twee to-many rollen: helemaal geen scalair veld; twee to-one rollen:
één aan elke kant), en daar zijn beide namen toegelaten. De aanscherping
stopt exact waar de afleiding stopt.

Op `tussenstappen[].type` **geen** byte-arm: die consument vergelijkt per
gedocumenteerd contract `equalsIgnoreCase`. Een byte-arm daar zou
strenger zijn dan alle drie uitvoerders en heeft geen getuige. De
asymmetrie staat in (m) benoemd.

### 2. De nullflank

De golf kruiste de vier beanvelden alleen als ze een naam dróegen. De
lege helft is nu **één lijstgedreven present-arm** over de vier (pad,
veld, waarde)-tripels, niet vier losse armen: met één getuige beweegt een
mutant die één van de vier sub-armen weghaalt **0 van de 503** documenten
(gemeten, mutant D2) — een clausule zonder tegenvoorbeeld is geen
clausule. Kosten over het corpus: **0**; alle 36 naamposities en alle 10
`tussenstappen[].type`-posities van vóór de fixronde zijn aanwezig.

### 3. Drie negatiefs, rood-eerst en arm-exact

Alle drie op `corpus/golden/jutterswet-postwacht` (de enige van de drie
feitcreatie-goldens die de beanmodel-poort speelt), elk een
één-regel-diff, elk PASS onder de verifier van de kaart (416b6c4) vóór de
armen:

| negatief | vorm | verifier vóór | ref | clingo | beanmodel |
| --- | --- | --- | --- | --- | --- |
| `wf034-feitcreatie-zusterrol` | `zetVeld: vaksein` (de to-many zusterrol) | PASS | PASS (10) | PASS (10) | ERROR `IllegalStateException: geen getter voor 'vaksein' op …facts.Sein` |
| `wf034-feitcreatie-hoofdletterveld` | `zetVeld: seinVak` (INTERNE hoofdletter) | PASS | PASS (10) | PASS (10) | ERROR `geen getter voor 'seinVak'` |
| `wf034-feitcreatie-loos-tussenstapveld` | `tussenstappen[0].veld: null` | PASS | PASS (10) | PASS (10) | ERROR `ConstraintEvaluationException` |

De hoofdletterkeuze is gemeten, niet aangenomen: een **eerste**-letterwissel
(`Seinvak`) speelt gewoon door (`PLAYED 1`) — de identifier-mapping
verlaagt de beginletter — en zou de crashclaim vals hebben gemaakt. Dat
staat in de NOTE, mét de eerlijke keerzijde: de byte-arm wijst `Seinvak`
wél af en is dus op precies die ene vorm strenger dan de uitvoerder.

Alle vier de null-gedragingen zijn gemeten en staan in de NOTE:
`tussenstappen[].veld` en `[].type` crashen met **exact** de excepties die
de twee gevulde-naam-getuigen al pinnen, `zetVeld` en `joinVeld` laten de
hele creatie vallen als luide transpiler-SKIP — de derde uitvoerdersweg
opnieuw.

**Mutanten, arm-exact** (elk: eigen build + volledige vier-zone-sweep van
503 documenten):

    B   de to-one-arm ([ one ]-tak) uit   -> 1 (het eigen)
    C   de byte-exacte tak uit            -> 1 (het eigen)
    D   de present-arm uit                -> 1 (het eigen)
    D2  één van de vier present-tripels   -> 0   (waarom D één arm is)

De zeven golf-5-mutanten zijn **hermeten** op 503 documenten, omdat arm A
van vorm veranderde. Zes van de zeven blijven één-document-exact. De
zevende niet meer, en dat staat nu eerlijk in zijn NOTE:
`wf034-feitcreatie-onbekend-zetveld` is sinds de fixronde de **doorsnede
van twee mutanten** — de `zetVeld`-positie (die nu drie getuigen draagt)
en de niet-gedeclareerd-arm (die er ook drie draagt) — en is het enige
document in beide verzamelingen.

**Byte-sweep.** Over alle **500** documenten die de fixronde voorgingen
beweegt **geen enkel oordeel en geen enkele melding**; de drie nieuwe
negatiefs zijn de enige toevoegingen.

### 4. De tekstreparaties

- **Vijf verlopen tellingen gedateerd** in plaats van stil verlopen
  ("measured at the pre-card state (8 / 28 / 8), N since"), en de inhoud
  gerepareerd waar die onwaar was geworden: **16** feitcreaties, waarvan
  **15** de symbolische vorm dragen (de zestiende is de eigen getuige);
  **30** objectcreaties, waarvan **29** een gedeclareerd subject noemen;
  **10** `Rollidmaatschap`-knopen — **8** resolveren, **1** is null,
  **1** resolveert nergens tegen. Plaatsen: IR-SPEC (ch. 3.4 WF-028), de
  twee commentaarblokken in `checks.ml`, en vier NOTE's.
- **"Drie van de vier"** was gemeten onwaar. Per **creatie** is het twee
  en twee: `anchor/jutterswet-erflijn` draagt er twee, beide met de
  doelrol. De zin telde documenten voor `zetVeld` en creaties voor
  `joinVeld`. Gecorrigeerd op alle vijf plaatsen (IR-SPEC ch. 4.6 (m), de
  WF-034-indexrij, het `checks.ml`-commentaar, de zetVeld-NOTE en dit
  Result). De fout staat ook in twee commit-messages; die geschiedenis
  blijft staan, en deze kaart zegt het.
- **Ch. 3.4 had de geciteerde autoriteit niet.** `ref` en clingo weigeren
  een kale beanvorm met "the symbolic form (IR-SPEC ch. 3.4) is what
  executes", maar die zin stond er vóór de kaart nergens. Toegevoegd, in
  één zin: de symbolische vorm is **normatief en verplicht**; de
  beanvelden zijn een **gesuperseerde spiegel** die een consument
  bovendien mag lezen, nooit in plaats van. Daarmee is de citatie waar en
  verdwijnt de spanning met het oude "a consumer may read either".
- Het **"none, some, all"-drieluik** genuanceerd: de *all*-hoek is de
  **welgevormde** vorm en krijgt dus nooit een getuige;
  `wf028-create-along-final-hop` pint een andere clausule (de
  ketenclausule), niet die derde hoek.
- **Indexrijkop:** "FOUR waves plus a fix round, eighteen arms" →
  "FIVE waves plus two fix rounds, twenty-seven arms" (18 + 6 van golf 5
  + 3 van deze ronde). En de nul-voorkomens-claim is **gesplitst**: hij
  geldt voor de achttien vormen van golf 1-4, die nul voorkomens in
  golden/rood dragen; de negen armen van golf 5 + fixronde kruisen
  posities die het corpus wél draagt (14 beannamen + 4 hop-types, 14
  objectcreaties, 5 rolchecks over golden+rood) en die alle resolveren —
  daar is de sweep het bewijs, niet afwezigheid.
- **Twee exceptiecitaten** zijn Drools-eigen en dragen nu de versie
  waarop ze zijn waargenomen (`drools.version` 8.44.0.Final); de
  `IllegalStateException` komt uit de geporte runtime zelf en is van ons.
- **De open-lijst van ch. 4.6** is gedateerd en uitgebreid met de
  gemeten restflanken: `VerreRollidmaatschap` (0 dracht — de tweelingzus
  van (o); een clausule zonder getuige mag niet, dus open-lijstregel),
  `Kenmerk.eigenaar` (2 van 190 checks, 0 golden), de **916**
  zijtabelsleutels (0 wezen vandaag), `Eenheid.stelsel` (1292/1292),
  `Eenheidstelsel.naam` (195/195), de **658** `regels`-stubs die geen
  enkele check leest, `dagsoortPerEenheid` (60, alle in de
  pariteitszone) en `anderTeFiller: null`.

### 5. De zesde golf

`backlog/new/20260905-wf034-zesde-golf-restvelden.md`: de restlijst als
kaart, met per veld de meting, de zijtabelsleutels als voorrangspost (916
posities, en de enige rest waar de schade een *stille
semantiekwijziging* is), en het nul-dracht-dilemma expliciet in de
acceptance — clausule-zonder-getuige tegenover de open-lijstregel, per
veld te beslissen en op te schrijven. Buiten scope daar: de
Drools-namencontract-bug (eigen backendkaart: de schrijfkant van de
feitcreatie mist de luide poort die de case-invoerkant en de vouwkant
wél hebben), `Rollidmaatschap.rol`, `Eenheid.afkorting`/WF-009, de wrat,
de 9 argumenttypes en WF-007.

### 6. Dertien poorten, uitslagen (fixronde)

     1 check-self-contained        PASS
     2 check-ir-schema             PASS (176 bestanden valideren + round-trippen)
     3 check-verifier              PASS (176 golden schoon; 297 negatief afgewezen
                                        voor hun eigen regel) — was 294
     4 check-case-schema           PASS (427 casebestanden)
     5 check-oracle                PASS (405 cases exact; 2 goldens zonder case)
     6 check-clingo                PASS (405 cases; ref = clingo)
     7 check-noop                  PASS (369 weerleggen de nietsdoener; 36 gepinde
                                        overlevers)
     8 check-rood                  PASS (22 rood; Drools-been 5 fault-pinned,
                                        17 not-carried met reden)
     9 check-catala                PASS (suite 201 checks; 12 catala-goldens
                                        byte-identiek)
    10 check-regelspraak           PASS (JUnit 171 tests; 30 rgs-goldens
                                        byte-identiek)
    11 check-toka-parity           PASS (29 cases: ref 29, clingo 29, Drools 29;
                                        45 .rgs byte-identiek, TOKA-suite 27 tests)
    12 check-drools                PASS (played 108; 43 auto-excluded met reden;
                                        1 value-divergence; 2 no-case;
                                        JUnit 199 tests)
    13 check-reftype-derivation    PASS (64 annotaties gedraaid over 13 goldens;
                                        58 replays, 11 door de DRL-tekst-tak;
                                        0 gepinde overlevers)

Elke uitslag is identiek aan die van de kaart zelf, op poort 3 na: de
negatiefzone gaat van 294 naar 297. Geen enkele andere teller beweegt —
wat de byte-sweep hierboven al voorspelde.

### 7. Afwijkingen van het fixronderecept

1. **Armentelling.** Het recept schreef "twenty-four arms"; dat telt de
   drie armen van deze ronde niet mee en zou dus meteen weer verlopen —
   precies de fout die F4 repareert. Geland als **twenty-seven arms,
   FIVE waves plus TWO fix rounds**.
2. **De Rollidmaatschap-telling.** Het recept gaf "7 + 1 null + 2
   gebroken"; per knoop gemeten (met een tijdelijk geïnstrumenteerde
   arm) is het **8 resolverend + 1 null + 1 onresolveerbaar** — het
   tweede, intacte rolcheck-document van de getuige zelf resolveert
   gewoon. Zo opgeschreven.
3. **De zijtabelsleutels** tellen **916**, niet 910 (acht per-lid-maps,
   zelf geteld en in de kaart benoemd zodat een hertelling reproduceerbaar
   is).
4. **De zeven golf-5-mutanten zijn hermeten** (niet in het recept
   gevraagd): arm A veranderde van vorm, en dat bleek de
   zetVeld-getuige's falsifieerbaarheidsclaim te raken — nu een
   doorsnede van twee mutanten in plaats van één.
