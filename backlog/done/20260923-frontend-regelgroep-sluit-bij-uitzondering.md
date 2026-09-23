# frontend-regelgroep-sluit-bij-uitzondering: een `Regelgroep` eindigt stil bij de eerste `Uitzondering op`

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting, gemeten
in het voorstel-concept `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(M5, kaartbranch): een `Regelgroep berging` over een basisregel, haar
`Uitzondering op` en een derde regel zendt `leden: ["bergingsbevoegd bij
quotum"]` uit met `diagnostics: []`. De uitzondering en de regel erna
vallen buiten de groep, want `regelGroep` omvat alleen opeenvolgende
`regel`-constructen (`RegelSpraak.g4`, het `Regelgroep`-patroon), en omdat
de basis in een defaults-groep wordt gelift weigert de verifier het document
daarna met `WF-033 FAIL /regelGroepen/0: group member 'bergingsbevoegd bij
quotum' does not resolve to a rule form`. De auteur krijgt de fout op de
verkeerde plaats en zonder oorzaak.

## Te beslissen bij oppakken

- (a) Weigeren: een `Uitzondering op` (of `Uitzonderingen ongeordend`,
  `Afwijking van`) binnen de reikwijdte van een `Regelgroep` is in het
  dialect niet toegestaan; luide melding op de kop van het blok, met de
  verwijzing naar §9.10 (de groep bestaat alleen voor recursie).
- (b) Opnemen: de defaults-groep (WF-107 geeft haar een naam) wordt lid van
  de regelgroep. Dat vergt dat een groepslid naar een defaults-groep mag
  verwijzen — IR-SPEC (WF-033) en verifier (**voorstel**) — en een
  semantiek voor een defaults-groep in een recursieve groep
  (RUNTIME-SEMANTICS ch. 5.5/9.10, **voorstel**).

Aanbeveling: (a), tenzij een werkbank-regelset de combinatie nodig heeft;
(b) is een spec-vraag met drie uitvoerders erachter.

## Acceptatie (bij oppakken aanscherpen)

- Rood-eerst JUnit-getuige met het M5-document; de gekozen uitkomst
  (melding op de juiste regel, of opname met verifier-dekking).
- `DIVERGENCES.md` §4 en het `Regelgroep`-deel beschrijven de interactie.
- Alle veertien gates groen.

May touch: `frontends/regelspraak/**`; bij (b) `spec/IR-SPEC.md`,
`spec/verifier/**`, `corpus/negatief/**` (**voorstel**); kaartverplaatsing.

Niet in deze kaart: de dialectvormen zelf (eigen kaarten).

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/frontend-trio` (drie frontendkaarten serieel, deze eerst),
basis `main` `3a140fe4`.

Maintainerkeuze, door de hoofdsessie genomen langs de aanbeveling:
**optie (a), weigeren.** Een `Uitzondering op` (en de zusterkoppen
`Uitzonderingen ongeordend`, straks `Afwijking van`) binnen de reikwijdte
van een `Regelgroep` is in het dialect niet toegestaan; luide melding op
de kop van het blok, met verwijzing naar §9.10 (de groep bestaat alleen
voor recursie) en dialect §4.

1. M5 reproduceren met `make ir` en de verifier: `leden` alleen de basis,
   `diagnostics: []`, daarna WF-033.
2. De reikwijdte meten, niet aannemen: welke plaatsingen van een
   extensieblok rond een `Regelgroep` geven vandaag een stil
   WF-033-document, en welke zijn welgevormd?
3. Rood-eerst JUnit-getuige met het M5-document: precies één ERROR, op de
   kopregel van het blok, met §9.10 en §4 erin; het document bereikt de
   schijf niet. Tegenproeven voor de plaatsingen die schoon blijven.
   Mutatieproef per weigerarm, met cmp-herstel.
4. `DIVERGENCES.md` §4 en het `Regelgroep`-deel beschrijven de interactie.
5. De werkbank-regelsets gemeten op de combinatie (open punt als zij haar
   vragen).
6. Gate 10 byte-identiek (70 rgs-goldens), alle veertien gates groen, elk
   als losse aanroep.

## Result (2026-09-23)

Optie (a) uitgevoerd. Een extensieblok binnen de reikwijdte van een
`Regelgroep`, of over een basisregel die groepslid is, krijgt één ERROR
op zijn eigen kop. Twee plaatsingen die verifier-schoon waren, worden
daardoor nu geweigerd (§1, rij 3 en 4: een blok direct na de groep met
de basis erbuiten, en een ongeordend blok direct na de groep); de twee
andere plaatsingen van §1 gingen al als WF-033-document de schijf op.
Geen corpusbron beweegt (geen `.rgs` in de repository draagt een
`Regelgroep`), alle veertien gates groen. Dit is een HERPIN-kaart: het
vendored frontend-slice beweegt.

### 1. Reproductie (basis `3a140fe4`)

M5 via `Resolver --ir` en de verifier, woordelijk:

    regelGroepen: leden ["bergingsbevoegd bij quotum"]   diagnostics: []
    resolvedRegels: ["bergingspremie"]   prioritizedDefaults: ["bergingsbevoegd bij quotum"]
    WF-033 FAIL /regelGroepen/0: group member 'bergingsbevoegd bij quotum' does not resolve to a rule form

De reikwijdte gemeten over vier plaatsingen, niet aangenomen:

    plaatsing                                                  frontend          verifier
    M5: groep, basis, Uitzondering op, derde regel             [] geschreven     WF-033
    basis in de groep, Parameter sluit de groep, dan het blok  [] geschreven     WF-033
    basis vóór de groep, blok direct na het groepslid          [] geschreven     PASS
    Uitzonderingen ongeordend direct na het groepslid          [] geschreven     PASS

De tweede rij is dezelfde WF-033-fout langs een andere plaatsing: het
blok staat buiten de reikwijdte, maar zijn basis is groepslid en het
liften haalt dat lid uit de regelvormen. Een weigering die alleen op de
plaats sleutelde, had die laten staan.

### 2. De keuze en haar lezing

Maintainerkeuze (hoofdsessie, langs de aanbeveling van de kaart),
verbatim: optie (a) WEIGEREN — een `Uitzondering op` (en de zusterkoppen
`Uitzonderingen ongeordend`, straks `Afwijking van`) binnen de reikwijdte
van een `Regelgroep` is in het dialect niet toegestaan; luide melding op
de kop van het blok, met verwijzing naar §9.10 (de groep bestaat alleen
voor recursie) en dialect §4.

Technische lezing, één ERROR per blok met elke reden die geldt:

- **plaatsingsarm** — het blok staat direct na het laatste lid van een
  groep: daar zou de groep doorlopen als het blok een regel was, en daar
  eindigde zij tot nu toe stil. De arm sleutelt op "geen definitie en geen
  groep" direct na de groep, niet op de twee koppen bij naam, zodat
  `Afwijking van` er vanzelf onder valt wanneer de kaart
  dialect-uitzondering-op-uitzondering haar toevoegt;
- **lidmaatschapsarm** — de basisregel van het blok is groepslid, waar het
  blok ook staat. Een latere kop met een eigen basis moet zich hier bij
  naam aansluiten; dat staat in de code en in `DIVERGENCES.md` §4.

Een geweigerd blok blijft ongelift met zijn hele basisfamilie, zoals de
andere weigeringen van de pass het laten. Rij 3 en 4 hierboven waren
verifier-schoon en worden nu geweigerd: op die plek vallen de regels
achter het blok stil buiten de groep. Geen bron draagt ze.

De melding op M5 (beide armen):

    ERROR (line 13): Uitzondering op 'bergingsbevoegd bij quotum': een
    uitzonderingsblok is in het dialect niet toegestaan binnen de reikwijdte
    van een regelgroep, noch met een groepslid als basisregel — het staat
    direct na het laatste lid van regelgroep 'berging', waar de groep stil
    eindigt, zodat het blok en elke regel erna buiten de groep zouden vallen;
    zijn basisregel 'bergingsbevoegd bij quotum' is lid van regelgroep
    'berging', en het liften naar een defaults-groep zou dat lid uit de
    regelvormen halen, zodat de groep naar niets meer verwijst (WF-033). Een
    regelgroep bestaat alleen om recursie te markeren (§9.10) en omvat alleen
    regels en beslistabellen; een defaults-blok (dialect §4) hoort er niet
    in. Plaats het blok en zijn basisregel buiten de groep, bijvoorbeeld vóór
    de kop 'Regelgroep berging'

Code: `RuleChecker.regelgroepReikwijdte`, vooraan in `defaults`. De
grammatica is niet gewijzigd; de toelichting bij `regelGroep` noemt de
weigering. `DIVERGENCES.md` heeft geen eigen `Regelgroep`-sectie; het
`Regelgroep`-deel van de documentatie is die toelichting in
`RegelSpraak.g4`, en §4 kreeg de subsectie "Interaction with
`Regelgroep` (§9.10) — refused".

### 3. Rood-eerst en mutatie

Getuige `RegelgroepUitzonderingTest` (nieuw bestand, bestaande tests niet
aangeraakt): vier weigeringen (M5 met beide armen, elke arm alleen, het
ongeordende blok) en twee tegenproeven (blok vóór de groepskop, blok
achter een definitie die de groep sluit).

    tegen de oude RuleChecker (md5 f75c962c9883ced82540063b822d17ef):
      6 tests found, 2 successful, 4 failed; elk van de vier:
      "... hoort geweigerd te worden: [] ==> expected: <false> but was: <true>"
    na de fix:                               6 successful, 0 failed
    mutant P (plaatsingsarm uit):            3 successful, 3 failed (M5, direct na de groep, ongeordend)
    mutant L (lidmaatschapsarm uit):         4 successful, 2 failed (M5, basis als groepslid)
    mutant S (geweigerd blok toch gelift):   4 successful, 2 failed (M5, ongeordend: de pin "blijft ongelift")
    elke mutant teruggezet met cp, cmp gelijk (md5 a4f9be2e09df6c123b0badce7080b569)

### 4. Werkbank

Geen werkbank-regelset vraagt de combinatie, dus geen open punt. De 198
`.rgs` onder `rulesets/regelspraak-rules` (basis, dagloon-ww,
herleving-ww, verrekening-ww) en de overige werkbankbronnen (513 `.rgs`
in totaal) dragen nul `Regelgroep`-koppen en nul extensiekoppen.

### 5. Gates, elk als losse aanroep na `eval $(opam env)`

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  278 documenten, vloer 192
     3 check-verifier           PASS  278 golden schoon; 440 negatief, vloer 350
     4 check-case-schema        PASS  638 casusbestanden
     5 check-oracle             PASS  ref-suite 806, vloer 489; 595 cases, vloer 427
     6 check-clingo             PASS  suite 523, vloer 83; 595 cases ref = clingo
     7 check-noop               PASS  559 weerleggen; 36 overlevers
     8 check-rood               PASS  43 rood, vloer 26; 86 fragmenten; Drools 9 gepind, 34 niet-gedragen
     9 check-catala             PASS  201 checks, 12 goldens
    10 check-regelspraak        PASS  415 JUnit (409 + 6), vloer 173; 70 rgs-goldens byte-identiek
    11 check-toka-parity        PASS  45 .rgs byte-identiek, 27 tests, 29 cases nul divergenties
    12 check-drools             PASS  gespeeld 182 (pin), DRL-tekst 161 (pin), JUnit 620
    13 check-reftype-derivation PASS  131 twists over 27 goldens, 128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks, 2 goldens

Gates 1 en 10 opnieuw na de laatste documentatiecommit: PASS, 415 / 70.
Geen vloer of pin aangeraakt.

### 6. Herpin

Het vendored frontend-slice beweegt: `RuleChecker.java`, `RegelSpraak.g4`
(alleen commentaar), `DIVERGENCES.md` en de nieuwe test. De herpin in
regelspraak-java doet de hoofdsessie na de merge.

### 7. Registervoorstel — `spec/CONSTRUCTS.md` rij 14

Aan het eind van de IR-deltakolom, na "… witnessed by
`anchor/jutterswet-erflijn` (2026-08-18)":

> **Follow-up (2026-09-23, card `frontend-regelgroep-sluit-bij-uitzondering`):
> a group takes up no defaults block, and the frontend says so.**
> `groepLid` takes rules and decision tables only, so a `Regelgroep` ended
> silently at the first `Uitzondering op` block: the rules behind it fell
> outside the group without a word, and when the block's base was a
> member, lifting it into a defaults group left the group naming no rule
> form, which the verifier refused under WF-033 on the group instead of on
> the block. Maintainer decision: refuse (option a). A defaults group as a
> group member (option b) would need WF-033 to accept one and a semantics
> for defaults inside a recursive group; it is recorded, not taken. One
> ERROR at the block's head citing §9.10 and dialect §4, with two arms: the
> block stands directly after a group's last member (keyed on "neither a
> definition nor a group", so a later extension head is refused by
> construction), or its base rule is a group member wherever the block
> stands. No `.rgs` source in any zone carries a `Regelgroep`, so no
> document moves. A HERPIN card.

### 8. Open punten

1. De plaatsingsarm rekent elk top-level alternatief dat geen definitie,
   groep, regel of beslistabel is tot de extensieblokken. Voegt een latere
   kaart een top-level constructie toe die wél direct na een groep mag
   staan, dan moet zij haar daar uitzonderen; de test van die kaart ziet
   het meteen.
2. De grammatica- en resolvercommentaren verwijzen voor §9.10 en andere
   onderwerpen naar `docs/grammar-divergences.md`, een bestand dat in deze
   repository niet bestaat (geërfd van de toolchain). Niet aangeraakt: het
   raakt een tiental commentaren buiten deze kaart.

### Fixronde 1 (2026-09-23, hoofdsessie)

Open punt 2 gesloten in `0a023e17`: alleen commentaar en registertekst.
De 24 commentaren in de grammatica en de hoofdbronnen (`grammar/*.g4`,
`resolver/`, `ir/`, `model/`) die naar `docs/grammar-divergences.md`
wezen, wijzen nu naar `grammar/DIVERGENCES.md`. Dat pad is relatief aan
de frontend, zoals de bestaande verwijzingen, en klopt dus ook in het
vendored slice. De tekst die ze citeerden kwam bij de overname niet mee.
Daarom zegt het register bovenaan welke secties het nog niet draagt
('§9.10 recursie / regelgroep', '§Scenariotoetsen', 'negated
membership', "concatenatie as a standalone value", plus de ongetitelde
onderwerpen). Zo wijst geen verwijzing naar een sectie die er niet is.
Twee verwijzingen landen op tekst: §13.4.17.47 (de operandenlijst van de
telling) en §4 (Regelgroep bij de defaults-extensie).

Bewust blijven staan:

- de twee verwijzingen in bestaande tests (`RecursieTest`,
  `TyperingTest`): bestaande tests alleen onder mandaat, en de opdracht
  noemde grammatica en resolver;
- die in de pariteitszone (`toka/`, read-only);
- die in de werkproza (`spec/IR-REVIEW.md`, backlog): historische
  citaten van het toolchainbestand;
- de verwijzingen naar `docs/SCENARIOTOETSEN.md`, het scenariocontract
  van regelspraak-java (niet gevraagd).

Gate 10 PASS (450 tests, 70 rgs-goldens byte-identiek), gate 1 PASS. Het
vendored slice beweegt verder (commentaar in grammatica en bronnen,
`DIVERGENCES.md`).

### Fixronde 2 (2026-09-23, hoofdsessie; lensadviezen 1, 4 en 7)

**Advies 1, `a8ab1639`: de lidmaatschapsarm kent de rijen van een
gegroepeerde beslistabel.** Een beslistabel in de groep staat voor de
regels van haar rijen ("Bergingspremie 1", …), en die noemt een blok als
basis. De arm sleutelde op de tabelnaam. Gemeten op `57114772`: een blok
op een rij, achter een Parameter die de groep sluit, ging met
`diagnostics: []` de schijf op, waarna de verifier WF-033 en WF-050 gaf.
De arm sleutelt nu op regelnamen (de desugaring legt de rijen vast) en
spreekt alleen waar het liften echt zou gebeuren. Een basis die naar geen
platte regel oplost (de tabelnaam zelf, een objectcreatie in de groep)
krijgt de juistere weigering "geen basisregel met die naam gevonden",
die de constructie noemt.

    rood-eerst tegen RuleChecker md5 f42fbaf463d3bd58b9d41548bdbfa2b4:
      9 tests, 6 successful, 3 failed (rij als basis: "[]"; tabelnaam en
      objectcreatie: de regelgroepweigering in plaats van "geen basisregel")
    mutant R1 (rijen niet gesleuteld):        1 failed
    mutant R2 (liftwacht resolvedVan weg):    1 failed (objectcreatie)
    teruggezet met cp, cmp gelijk (md5 bd954129327a7b234b3071bd0e4dd7f3)

**Bijvangst, `78edd45d`: een Regelgroep noemt de rijen van haar
beslistabel als leden.** Gemeten bij advies 1: elke groep met een
beslistabel, ook zonder extensieblok, werd met de tabelnaam als lid
uitgezonden en door de verifier geweigerd (`WF-033 FAIL /regelGroepen/0:
group member 'Bergingspremie' does not resolve to a rule form`). Leden
zijn regelnamen (IR-SPEC ch. 3.9), dus de groep noemt nu de rijregels;
het document is daarna `verifier: PASS`. Getuige `RegelgroepLedenTest`:
rood-eerst 1 failed ("but was: <[bergingsbonus, Bergingspremie]>"),
mutant L (tabelnaam terug) 1 failed, cmp-herstel. Geen bron in welke
zone ook draagt een Regelgroep.

**Advies 4, `d555c43f`: de uitweg klopt overal.** De melding raadde "vóór
de kop 'Regelgroep <deze groep>'" aan. Bij twee groepen direct na elkaar
komt wie dat volgt achter het laatste lid van de andere groep terecht en
wordt opnieuw geweigerd. De melding noemt nu: vóór de eerste
regelgroepkop van het document (daar reikt geen groep), of achter een
definitie die de groep afsluit. De getuige volgt de raad en eist een
schoon document. Een groepskop zonder leden (al een syntaxfout) kreeg
daarnaast een plaatsingsmelding over een "laatste lid" dat er niet is;
die vervalt.

    rood-eerst tegen RuleChecker md5 c7ec34fa545de39d0d517688d9e0ed99:
      11 tests, 8 successful, 3 failed (M5- en tweegroepen-uitweg, lege kop)
    mutant M1 (lege kop weer meegeteld):       1 failed
    mutant M2 (uitweg naar de laatste groep):  1 failed
    teruggezet met cp, cmp gelijk (md5 d08cb8fed53f71a447411d028bf6e642)

**Advies 7, `41e47795`:** de openingszin van dit Result gecorrigeerd. Twee
verifier-schone plaatsingen worden nu geweigerd (§1, rij 3 en 4).

Aanvulling op het registervoorstel voor rij 14, als laatste zin:

> A decision table in a group stands for the rules of its rows: the group
> lists those rows as members — it listed the table's name, and the
> verifier refused every group holding a table under WF-033 — and the
> refusal's membership arm keys on them, speaking only where a lift would
> happen. The way out the refusal names now holds wherever the author
> follows it: before the document's first group header, or behind a
> definition that ends the group.

### Fixronde 3 (2026-09-23, hoofdsessie; lensadviezen 2, 3 en 5)

**Advies 2, `402dc130`: "geen basisregel" noemt de beslistabel.** Het
register en dit Result zeiden dat die weigering de constructie noemt; voor
een tabelnaam als basis klopte dat niet (`basisReden` kende geen
beslistabel). De toelichting zegt nu dat de naam een beslistabel is, noemt
haar rijen, en zegt waarom geen rij alleen de basis kan zijn: de rijen
schrijven samen één doel. Rood-eerst tegen RuleChecker md5
c534c7d2ad22fe21bdf9fdc41d0580de: 11 tests, 10 successful, 1 failed;
mutant B (tabel niet genoemd) 1 failed; cmp-herstel (md5
d0cd62b168c3443328bfc03788592618).

**Advies 3, `d4a25506`: een tweede schrijver op het doel van een
defaults-groep (WF-050).** Gemeten op de tip van fixronde 2 (`c8f32cd5`),
zes plaatsingen:

    plaatsing                                          frontend        verifier
    Uitzondering op X, andere gewone regel op het doel [] geschreven   WF-050 (a rule also writes)
    Uitzondering op een tabelrij, buiten een groep      [] geschreven   WF-050 (de andere rijen)
    twee geordende families op één doel                [] geschreven   WF-050 (geen ongeordend-markering)
    een familie naast een ongeordend blok              [] geschreven   WF-050 (idem)
    een ongeordend blok naast een gewone regel         [] geschreven   WF-050 (a rule also writes)
    twee ongeordende blokken op één doel               [] geschreven   PASS

De lens vroeg om de eerste twee; de andere drie zijn dezelfde zin van
WF-050 en gingen even stil de schijf op, dus het hek dekt ze mee.
`RuleChecker.keurEnigeSchrijver` loopt na het liften per doel (sleutel:
het gedeclareerde objecttype, zoals WF-050) en geeft per doel en clausule
één ERROR die beide schrijvers noemt, op de kop van het blok dat de groep
maakte. Een tabelrij wordt als rij van haar tabel genoemd. De laatste rij
blijft schoon, want elke groep is ongeordend. Niet gemeten en niet
gedekt: een verdeling of een objectcreatie-initialisatie als andere
schrijver (WF-050 noemt ze ook).

    getuige EnigeSchrijverTest, rood-eerst tegen RuleChecker md5
      d0cd62b168c3443328bfc03788592618: 7 tests, 2 successful, 5 failed (elk "[]")
    mutant U1 (clausule gewone regel uit):         3 failed
    mutant U2 (clausule meerdere groepen uit):     2 failed
    mutant U3 (ook ongeordende groepen geweigerd): 3 failed, waaronder
                                                   DefaultsExtensieTest.ongeordende_groep_bouwt_zusterdefaults
    teruggezet met cp, cmp gelijk (md5 35bd822b616e7e0bfdb64283a3c3d3c6)

**Advies 5, `b488154b`: tabelrijen met één regelnaam.** Een rij heet naar
de tabel en het nummer in haar eerste cel. Rijen zonder nummer (twee keer
"Bonustabel") of met hetzelfde nummer (twee keer "Bonustabel 1") gingen
stil de schijf op, waarna de verifier WF-020 en WF-021 gaf. Nu is het een
ERROR die de dubbele naam noemt, en een groep met die tabel noemt elke naam
één keer. Getuige `BeslistabelRijnaamTest`: rood-eerst tegen RuleChecker md5
cf62a371afc2228c90825b2b9ca9b10c: 4 tests, 1 successful, 3 failed; mutant
N1 (ERROR uit) 3 failed, mutant N2 (dubbel in de groep) 1 failed;
cmp-herstel (md5 465e70c6186d2f99a71f856357f49077).

Registervoorstel erbij, rij 8, als follow-up:

> **The single writer, producer side (2026-09-23, card
> `frontend-regelgroep-sluit-bij-uitzondering`, fix round 3).** The
> RegelSpraak lowering built each defaults group from its own block and
> never looked beside it, so a plain rule on the group's target — another
> rule, or the other rows of a decision table when one row was the base —
> and a second group on the target that was not declared unordered all
> reached disk silently for WF-050 to refuse. The frontend now refuses each
> by name, keyed on the declared object type as WF-050 is; two unordered
> blocks on one target stay admitted. An allocation or an object-creation
> initialisation as the other writer is not fenced.
