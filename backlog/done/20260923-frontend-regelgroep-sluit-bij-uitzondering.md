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
op zijn eigen kop. Geen welgevormd document verandert van emissie, geen
corpusbron beweegt (geen `.rgs` in de repository draagt een
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

Open punt 2 gesloten in `1f3ae38d`: alleen commentaar en registertekst.
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
