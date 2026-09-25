# verifier-allocatie-keten: een toewijzing op super- én subtype voor één doel, zonder poort

Bijvangst van de kaart verifier-supertype-dubbele-schrijver (2026-09-25,
U7 en lensvondst A17.1): WF-026 (één toewijzing per (type, attribuut)) en
de toewijzingslift van WF-050 keyen op het gedeclareerde type, zoals WF-079
dat vóór die kaart deed. Gemeten op de L5-wire: een toewijzing op `Berger`
en een op `Hoofdberger is een Berger` voor hetzelfde doel is verifier-schoon
in beide bronvolgordes, terwijl `ref` en de Drools-exec-tak geen fixpunt
vinden, clingo twee waarden geeft (`[100;500]`) en de DRL-tak naar
bronvolgorde antwoordt. De principiële skip van de exec-transpiler ziet het
ketenpaar evenmin. Anders dan bij de rekenregels wordt een toewijzing nooit
in een defaults-stapel gevouwen (ch. 7.10 vouwt alleen rekenregels), dus
een ketenarm voor WF-026 heeft geen poort: het paar is altijd ill-formed.
IR-SPEC ch. 6.1 eist al dat de single-writer-hekken de effectieve stapel per
concreet type lezen.

## Te beslissen bij oppakken

- (a) WF-026 keurt per concreet type door de keten (de lijn van WF-079 sinds
  2026-09-25): twee toewijzingen op één (type-of-voorouder, attribuut) zijn
  ill-formed, met de uitdrukbare vorm in de zin (één toewijzing op het
  hoogste type dat het doel declareert, of een subtype-eigen doel).
- (b) De lift van WF-050 (groep op het subtype, toewijzing op het supertype)
  keurt dezelfde keten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproductie op de vier uitvoerders in de kaart vastgelegd (de
  proefdocumenten van 2026-09-25 opnieuw gebouwd, rechtenvrij).
- (b) Negatiefs `wf026-keten-…` en `wf050-keten-…` (**voorstel**),
  rood-eerst, mutatieproef; de spec-zinnen in IR-SPEC ch. 3.2 en 6.1
  (**voorstel**); één zin in de verifier zonder regelnummerstaart.
- (c) Alle bestaande goldens met een supertype schoon; toka alleen tellen.
- (d) Alle veertien gates groen.

May touch: `spec/verifier/**`, `spec/IR-SPEC.md`, `corpus/negatief/**`
(alle **voorstel**), kaartverplaatsing. Niet in deze kaart: de uitvoerders.

## U-besluiten (hoofdsessie, 2026-09-25, bij de claim)

- U1 Beide: (a) WF-026 keurt per concreet type door de typeketen — een toewijzing wordt nooit in een stapel gevouwen (ch. 7.10 vouwt alleen rekenregels), dus zonder poort: twee toewijzingen op één (type-of-voorouder, attribuut) zijn ill-formed; (b) de toewijzingslift van WF-050 (groep op het ene niveau, toewijzing op het andere) keurt dezelfde keten. De uitvoerders veranderen niet.
- U2 Zinnen (voorstel; de bouwer stelt vast en meldt letterlijk, zonder regelnummerstaart): `two allocations write target (Hoofdberger, bergloon) through its type chain — one declared on Berger, one on Hoofdberger — and an allocation is never folded into a stack, so both fire on a Hoofdberger instance; at most one allocation writes a target (ch. 3.2)`; voor WF-050 de bestaande liftzin met de ketenclausule. Pointer op de versie van het laagste type; ontdubbeling per (pointer, zin) zoals WF-079. Hergebruik de ketenpredicaten van WF-079 (`checks.ml`), dupliceer geen ketenlogica.
- U3 Getuigen: negatiefs `wf026-keten-twee-toewijzingen` en `wf050-keten-toewijzing-naast-groep`, rood-eerst (verifier PASS geciteerd), `expect.txt`, NOTE uit echte uitvoer, mutatieproef; de reproductie op de vier uitvoerders (proefdocumenten van de supertype-kaart, (buiten de repo) als die er nog staan, anders eigen) in het Result; elke golden met een supertype blijft schoon (gate 3); toka alleen tellen.
- U4 Spec: IR-SPEC WF-026 (ch. 3.2) en WF-050 krijgen de ketenclausule; ch. 6.1's zin blijft; RS ch. 7.10 één zin dat een toewijzing niet vouwt en WF-026 daarom zonder poort door de keten spreekt. Naamtabel rc 0; geen D-/U-nummers.
- U5 Gates 1, 2, 3.

## Result (2026-09-25, bouwer)

## Result — verifier-allocatie-keten

### Stand

- Branch `card/verifier-allocatie-keten`, worktree (worktree allocatie)
- Basis main 20e508d5, claim 7d541d11, kop **4cf35679**. De werkboom is schoon.
- Commits:
  - c9c61159 negatief wf026- en wf050-ketengetuigen: rood-eerst, vóór de ketenarmen
  - 6d7516ca verifier WF-026 en WF-050: ketenarmen voor allocaties, zonder poort
  - 40249b19 spec: WF-026 en de allocatielift van WF-050 lezen een typeketen per concreet type
  - fixronde (opdracht van de hoofdsessie: open punten 1 en 2 direct fixen):
    - 1c6128f7 negatief wf050: creatie door de keten rood-eerst, restanthelft vlak getuigd
    - ad042847 verifier WF-050: de creatieclausule leest de keten, alleen van onderen
    - 929cad43 IR-SPEC WF-050: de ketenclausule dekt aandelen, restant en creatie
  - golden (opdracht van de hoofdsessie, met mandaat voor corpus/golden en de pins van gate 12 als
    voorstel):
    - 4cf35679 golden jutterswet-strandmelding: de richtingshelft van de creatieketenarm
- Fase 1 (reproductie) heeft geen commit. De proefdocumenten staan in (buiten de repo) De
  metingen staan in de NOTE's van de getuigen en hieronder.
- Alles op beschermde paden is **voorstel**:
  - spec/verifier/lib/checks.ml
  - spec/IR-SPEC.md
  - spec/RUNTIME-SEMANTICS.md (door U4 gevraagd; staat niet in de may-touch-regel van de kaart)
  - vijf nieuwe mappen onder corpus/negatief/
  - de golden corpus/golden/jutterswet-strandmelding/
  - scripts/check-drools.sh (de pins van gate 12)
- Scripts: alleen de pins van gate 12 in scripts/check-drools.sh (zie "De golden"). Geen floor
  verhoogd. De uitvoerders zijn niet veranderd (U1). De kaart zelf is niet bijgewerkt.
- Tellingen gate 3: negatief 433 → 438, expect.txt 275 → 280.

### Ontwerpbesluiten (technisch, zelf genomen)

1. **Gedeelde ketenpredicaten (U2).** WF-079 had een lokale `chain_pairs`, `distinct_form` en een
   ontdubbeling. Die zijn getild naar vier helpers op topniveau, na `chain_group_keys`:
   - `chain_lower`: van twee schrijvers, via hun typen, de lagere eerst, of None;
   - `chain_pairs`: alle ketenparen van één schrijverslijst;
   - `distinct_form`;
   - `dedup_reports`: één melding per (pointer, zin).

   WF-079, WF-026 en de WF-050-lift lezen alle drie deze helpers. Er is geen gedupliceerde
   ketenlogica. WF-079 is gedragsgelijk: de sweep over 759 documenten, en na de fixronde over 761, toont 0 verschillen in
   WF-079-uitvoer.
2. **WF-026: vlakke arm plus ketenarm, zonder poort.** De ketenarm vergelijkt alleen doelen,
   zoals de vlakke arm; restantschrijvingen tellen niet mee. De pointer staat op de versie van het
   lagere type. De ontdubbeling per (pointer, zin) geldt voor WF-026 als geheel, zoals bij WF-079
   (U2).
3. **WF-050: de ketenarm van de allocatielift dekt beide helften van de vlakke lift.** Dat zijn de
   aandelen op `targetType` en het restant op `sourceType`. Gemeten: beide helften hebben door de
   keten dezelfde kloof (zie de reproductie).
   - De melding staat op de groep (`/rules/prioritizedDefaults/i`), net als bij de vlakke lift.
   - U2's "pointer op de versie van het laagste type" heb ik daarom alleen op WF-026 toegepast.
     WF-050 is een regel over de groep, en een groep heeft geen versie.
   - De ontdubbeling geldt binnen de ketenarm.
4. **Een derde getuige.** U3 noemt er twee. `wf050-keten-restant-naast-groep` maakt de restanthelft
   falsifieerbaar (mutant m3). Zonder deze getuige zou die helft ongetuigd zijn. Dat was de
   lensvondst B1 bij de supertype-kaart.
5. **Fixronde: de creatieclausule van WF-050 krijgt een ketenarm, in één richting.** Een creatie
   maakt een instantie van precies haar `createdType`; zij is niet polymorf.
   - Is `createdType` een strikt subtype van het subject van de groep, dan erft die instantie de
     stapel van de groep, en dan zijn er twee schrijvers. Die richting weigert de ketenarm.
   - Is `createdType` een voorouder van het subject, dan maakt de creatie geen instantie waar de
     groep op draait. Gemeten: schoon in `ref`, clingo en exec. Die richting laat de ketenarm door.
   - Dat is het verschil met de allocatiearmen: een allocatie schrijft elke instantie van haar type
     en ontmoet de groep daarom in beide richtingen.
   - Een lokaal predicaat `initialises` wordt gelezen door de vlakke creatieclausule en door haar
     ketenarm, zodat die twee niet uiteen kunnen lopen. De vlakke clausule is gedragsgelijk: de
     sweep toont geen verschil buiten de nieuwe getuige.
   - De ketenarm gebruikt `chain_lower` en `dedup_reports`. Proef c-twee-creaties (twee creaties
     van Wrakrecht) geeft één regel.
6. **Fixronde: de restanthelft van de vlakke allocatieclausule krijgt haar getuige**
   `wf050-restant-naast-groep`, het document (buiten de repo)
   ongewijzigd. Het fragment in `expect.txt` is een prefix van de zin van de ketenarm met hetzelfde
   subject en attribuut. Op dit document kan de ketenarm niet vuren, want het subject van de groep
   en het brontype van de allocatie zijn één type. De mutant m8 onderscheidt de armen.

### De definitieve weigerzinnen (letterlijk, uit checks.ml en de echte uitvoer)

Deze zinnen staan alleen in de verifier:
- De gedeelde OCaml-lader en de Drools-lader weigeren deze vormen niet.
- Buiten de verifier komen ook de vlakke zinnen nergens voor (git grep).
- `ref` faalt pas bij evaluatie, met "no fixpoint".
- Er zit geen regelnummerstaart in en er staat geen enumwaarde in de zinnen.

**WF-026, ketenarm.** Sjabloon, met (lager, attribuut, hoger, lager, lager, lager):

    two allocations write target (%s, %s) through its type chain — one ranging over %s, one over %s — and an allocation is never folded into a stack, so an instance of %s receives both shares; at most one allocation writes a target (ch. 3.2), and a second share needs an attribute %s declares itself

Op de getuige, echte uitvoer:

    WF-026 FAIL /rules/allocations/1/versions/0: two allocations write target (Hoofdberger, bergingsaandeel) through its type chain — one ranging over Berger, one over Hoofdberger — and an allocation is never folded into a stack, so an instance of Hoofdberger receives both shares; at most one allocation writes a target (ch. 3.2), and a second share needs an attribute Hoofdberger declares itself

Afwijkingen van het U2-voorstel, met reden:
1. "one ranging over Berger, one over Hoofdberger" in plaats van "one declared on …". Een allocatie
   wordt niet op een type gedeclareerd. IR-SPEC ch. 3.2 noemt `targetType` "the population the
   allocation ranges over".
2. "so an instance of Hoofdberger receives both shares" in plaats van "so both fire on a Hoofdberger
   instance", om twee redenen:
   - een allocatie vuurt per bron (de Berging), niet op de ontvanger;
   - het sjabloon "a %s instance" gaf in proef p1 "a Opperberger instance".
3. Toegevoegd: ", and a second share needs an attribute Hoofdberger declares itself". Kaartpunt (a)
   vraagt de uitdrukbare vorm in de zin. Gemeten: die vorm is schoon en speelt in alle vier de
   uitvoerders.
4. Het attribuut is `bergingsaandeel`. Het U2-voorbeeld noemde `bergloon`.

**WF-050, ketenarm van de allocatielift.** Sjabloon, met (subject, attribuut, subject,
"shares"|"remainder", type van de schrijving, lager):

    an allocation also writes defaults target '%s.%s' through its type chain — the group declared on %s, the allocation's %s on %s — and an allocation is never folded into a stack, so an instance of %s has both writers

Op de getuigen, echte uitvoer:

    WF-050 FAIL /rules/prioritizedDefaults/0: an allocation also writes defaults target 'Hoofdberger.bergingsaandeel' through its type chain — the group declared on Hoofdberger, the allocation's shares on Berger — and an allocation is never folded into a stack, so an instance of Hoofdberger has both writers
    WF-050 FAIL /rules/prioritizedDefaults/0: an allocation also writes defaults target 'Wrakberging.onverdeelde rest' through its type chain — the group declared on Wrakberging, the allocation's remainder on Berging — and an allocation is never folded into a stack, so an instance of Wrakberging has both writers

**WF-050, ketenarm van de creatieclausule (fixronde).** Sjabloon, met (subject, doelnaam, subject,
createdType, createdType):

    an object creation also initialises defaults target '%s.%s' through its type chain — the group declared on %s, the created type %s — and an object creation is never folded into a stack, so an instance of %s it creates has both writers

Op de getuige, echte uitvoer:

    WF-050 FAIL /rules/prioritizedDefaults/0: an object creation also initialises defaults target 'Strandrecht.uitkering' through its type chain — the group declared on Strandrecht, the created type Wrakrecht — and an object creation is never folded into a stack, so an instance of Wrakrecht it creates has both writers

De restanthelft van de vlakke clausule (getuige `wf050-restant-naast-groep`) houdt haar
bestaande zin:

    WF-050 FAIL /rules/prioritizedDefaults/0: an allocation also writes defaults target 'Berging.onverdeelde rest'

`expect.txt` pint:
- tot en met "receives both shares" (WF-026);
- tot en met "has both writers" (de ketenarmen van WF-050, ook die van de creatie);
- de hele vlakke zin (restanthelft).

De vlakke zinnen van WF-026 en WF-050 zijn ongewijzigd.

### Reproductie (U3), gemeten 2026-09-25

Bronnen uit (buiten de repo), opnieuw geëmitteerd met de frontend van deze worktree:
- de uitvoer is byte-gelijk aan de u7-emissie (L5-wire, `uchar-ir-v1`);
- verifier vóór de arm: md5 7816a181b61c7e337e9f2b045877f7b6 (main);
- case: B1 Berging (opbrengst 1000, premie 100), bergers H1 (Hoofdberger, rang 1) en G1 (Berger).
  De verwachtingen in de case zijn plaatshouders; het gaat om de uitvoer.

| proef | verifier vóór | verifier met arm | ref | clingo | Drools exec | DRL-tak |
|---|---|---|---|---|---|---|
| allocatie over Berger en over Hoofdberger, Berger eerst | PASS | WF-026 /rules/allocations/1/versions/0 | no fixpoint | H1 `[100;500]` | geen fixpunt | FAIL, werkelijk 100 |
| idem, Hoofdberger eerst | PASS | WF-026 /rules/allocations/0/versions/0 | no fixpoint | H1 `[500;100]` | geen fixpunt | PLAYED 1 (500) |
| vlakke controle: beide over Hoofdberger | WF-026 (vlak) | ongewijzigd | no fixpoint | H1 `[100;1000]`, G1 leeg | EXCLUDE transpiler SKIP (>1 verdeling) | FAIL |
| groep op Hoofdberger, allocatie over Berger | PASS | WF-050 ketenarm (shares on Berger) | no fixpoint | H1 `[7;500]` | geen fixpunt | EXCLUDE (geen defaults-lowering) |
| groep op Berger, allocatie over Hoofdberger | PASS | WF-050 ketenarm (shares on Hoofdberger) | no fixpoint | H1 `[7;1000]`, G1 `[7]` | geen fixpunt | EXCLUDE |
| vlakke controle: groep en allocatie op Berger | WF-050 (vlak) | ongewijzigd | no fixpoint | H1 `[7;500]`, G1 `[7;500]` | geen fixpunt | EXCLUDE |
| restant: groep op Wrakberging.onverdeelde rest, allocatie uit Berging met die rest | PASS | WF-050 ketenarm (remainder on Berging) | no fixpoint | B1 `[7;1]` | EXCLUDE (beanlimiet)¹ | EXCLUDE |
| restant vlak: groep op Berging | WF-050 (vlak) | ongewijzigd | no fixpoint | B1 `[7;1]` | EXCLUDE (beanlimiet)¹ | EXCLUDE |
| uitdrukbare vorm WF-026: tweede aandeel naar `premieaandeel` op Hoofdberger | PASS | PASS | PASS (4) | PASS (4) | PLAYED 1 | PLAYED 1 |
| uitdrukbare vorm WF-050: groep op `toeslag` van Hoofdberger | PASS | PASS | PASS (4) | PASS (4) | PLAYED 1 | EXCLUDE |

¹ Exec: "name-sharing bean limit". Met een gewone Berging als B1 speelt exec wel. De vlakke
controle geeft dan "verwacht 1, werkelijk 7".

**Creatieclausule (fixronde)**, proeven in (buiten de repo) M1 is een Melding met
omvang 50.00. De creatie maakt een instantie en initialiseert `uitkering`. Een leesregel
schrijft de `uitkering` van die instantie terug in `gemelde uitkering` van M1.

| proef | verifier vóór (d4752610) | met de arm (8f0bca49) | ref | clingo | exec | DRL |
|---|---|---|---|---|---|---|
| creatie van Wrakrecht, groep op Strandrecht (= getuige) | PASS | WF-050 keten | 5 (de groep wint) | `[500;5000]` | 5 | EXCLUDE |
| omgekeerd: creatie van Strandrecht, groep op Wrakrecht, plus W1 (Wrakrecht, grondslag 10.00) | PASS | PASS | PASS (M1 50.00, W1 5.00) | PASS | PLAYED 1 | EXCLUDE |
| vlakke controle: creatie van Strandrecht, groep op Strandrecht | WF-050 (vlak) | ongewijzigd | 5 | `[500;5000]` | 5 | EXCLUDE |
| uitdrukbare vorm: de creatie zet `grondslag`, de groep beslist `uitkering` (omvang 50 en 150) | PASS | PASS | PASS (5.00 en 9.00) | PASS | PLAYED 1 | EXCLUDE |
| drie niveaus: creatie van Diepwrak ⊑ Wrakrecht ⊑ Strandrecht, groep op Strandrecht | PASS | WF-050 keten (created type Diepwrak) | — | — | — | — |
| twee creaties van Wrakrecht | PASS | één regel (ontdubbeld) | — | — | — | — |
| zustertype: groep op Kustrecht, creatie van Wrakrecht | PASS | PASS | — | — | — | — |
| kenmerkdoel: groep op het kenmerk `recht op uitkering`, creatie van Wrakrecht die het kenmerk zet | PASS | WF-050 keten | — | — | — | — |

**Restanthelft vlak (fixronde).** B1 is een gewone Berging (opbrengst 1001) met twee Bergers.
- `ref`: no fixpoint;
- clingo: `B1.onverdeelde rest = [7;1]`;
- exec: "verwacht 1, werkelijk 7";
- DRL: EXCLUDE;
- verifier: WF-050 (vlak), zowel vóór als na deze kaart; met m8 `verifier: PASS`.

Samenvatting:
- Het kaarttekstgeval is exact gereproduceerd: verifier PASS in beide bronvolgordes, ref en exec
  geen fixpunt, clingo twee waarden, de DRL-tak naar bronvolgorde.
- De principiële skip van de exec-transpiler keyt op het gedeclareerde type. Hij vuurt alleen in
  de vlakke controle.

### Rood-eerst en mutatieproef (U3)

**Rood.** Op de drie getuigen, met verifier md5 7816a181… (commit c9c61159): `verifier: PASS`.
Gate 3 gaf rc=1:

    FAIL: negatief case corpus/negatief/wf026-keten-twee-toewijzingen/ir.json passed the verifier (must violate WF-026):
    verifier: PASS
    FAIL: negatief case corpus/negatief/wf050-keten-restant-naast-groep/ir.json passed the verifier (must violate WF-050):
    verifier: PASS
    FAIL: negatief case corpus/negatief/wf050-keten-toewijzing-naast-groep/ir.json passed the verifier (must violate WF-050):
    verifier: PASS

**Met de arm.** Binary md5 d4752610c43cb4f15a9e1c9a8fb35a7c, bron checks.ml md5
9edf47e5655917192a08f3597a30f1de. Gate 3 rc=0.

**Rood in de fixronde (1c6128f7).**
- De creatiegetuige gaf met verifier d4752610 `verifier: PASS`. Gate 3 rc=1:

      FAIL: negatief case corpus/negatief/wf050-keten-creatie-naast-groep/ir.json passed the verifier (must violate WF-050):
      verifier: PASS

- De flat-restantgetuige wordt door d4752610 al geweigerd (`WF-050 FAIL /rules/prioritizedDefaults/0:
  an allocation also writes defaults target 'Berging.onverdeelde rest'`). Tegen mutant m8 (b7ac9b8e…)
  gaf zij `verifier: PASS`, en gate 3 met m8 had rc=1:

      FAIL: negatief case corpus/negatief/wf050-restant-naast-groep/ir.json passed the verifier (must violate WF-050):
      verifier: PASS
      FAIL: negatief case corpus/negatief/wf050-restant-naast-groep/ir.json rejected for WF-050, but not on the arm corpus/negatief/wf050-restant-naast-groep/expect.txt names (sought in the messages of the WF-050 lines):

**Definitieve arm (ad042847).** Bron checks.ml md5 9f9e0bcfece1ae7be0d35582d12e6f46, binary md5
8f0bca496027ae2f15dce4950399ac8f. Gate 3 rc=0.

**Mutanten, op de definitieve bron (9f9e0bcf…).** Recept = de diff tegen die bron
((buiten de repo); de snippets staan in (buiten de repo)). Voor elke
mutant zijn bouw, sweep en gate 3 samen gedraaid, met de md5 van de gebouwde binary.
- Eerdere rondes staan in (buiten de repo) en (buiten de repo), met hetzelfde gedrag.
- In een allereerste ronde maten de sweeps per ongeluk de binaries van de vorige ronde, omdat een
  `cp` op een alleen-lezen kopie faalde. Dat is hersteld en opnieuw gemeten.

Kolom "verschuift" telt de documenten over 761 (golden 251, rood 42, negatief 438 en toka 30) die
van verdict of rapport veranderen ten opzichte van de definitieve arm.

| mutant | recept | md5 | verschuift | gate 3 |
|---|---|---|---|---|
| m1 | WF-026-ketenarm uit: `dedup_reports (pairs named @ (if false then chain else []))` | fa863d8e… | alleen wf026-keten-twee-toewijzingen | rc=1 |
| m2 | allocatie-ketenarm van WF-050 uit: de lijst `[ ("shares", …); ("remainder", …) ]` achter `if false` | 743a7d5d… | wf050-keten-toewijzing en wf050-keten-restant | rc=1 |
| m3 | alleen de remainder-ingang weg | 22b1b635… | alleen wf050-keten-restant-naast-groep | rc=1 |
| m4 | alleen de shares-ingang weg | 3b8d8553… | alleen wf050-keten-toewijzing-naast-groep | rc=1 |
| m5 | eerste tak van `chain_lower` (`false &&`) | 3e17465b… | wf050-keten-toewijzing, -restant en -creatie | rc=1 |
| m6 | tweede tak van `chain_lower` | 42cc6108… | wf026-keten-twee-toewijzingen en wf079-keten-kale-schrijvers (gedeelde helper) | rc=1 |
| m7 | ontdubbeling WF-026 uit | 4236b05f… | geen verdict; alleen het rapport van toka/casus/ir.json | rc=0 |
| m8 | restanthelft van de VLAKKE allocatieclausule uit (`false && match …`) | 4f2564a6… | alleen wf050-restant-naast-groep (vóór die getuige: 0 documenten) | rc=1 |
| m9 | creatie-ketenarm uit: `when false && norm low = norm nt` | fe06f889… | alleen wf050-keten-creatie-naast-groep | rc=1 |
| m10 | richtingsvoorwaarde van de creatie-ketenarm uit: `when true \|\| norm low = norm nt` | ccc13603… | 0 documenten; weigert wel de omgekeerde proef c-omgekeerd-lees ten onrechte (zie open punt 1) | rc=0 |

Na elke mutant is de bron hersteld: checks.ml md5 9f9e0bcf…, binary 8f0bca49….

### De golden (4cf35679): corpus/golden/jutterswet-strandmelding

Mandaat van de hoofdsessie: corpus/golden en de pins van gate 12, als voorstel. De golden
getuigt van de richtingsvoorwaarde van de creatie-ketenarm, die in open punt 1 van de vorige
ronde geen getuige had.

- **Bron.** `bron.rgs` is de proef c-omgekeerd-lees ongewijzigd:
  - een groep op Wrakrecht beslist `uitkering` (9,00 bij grondslag > 100, anders 5,00);
  - een objectcreatie maakt per Melding een Strandrecht (feittype `strandmelding`) met
    `uitkering` = omvang;
  - een leesregel zet de uitkering van die instantie terug in `gemelde uitkering`.
- **ir.json.** De emissie, vanuit de repo gemaakt zoals gate 10 dat doet (byte-gelijk).
- **case.json.** Absolute verwachtingen, met de hand berekend:

  | instantie | invoer | verwacht | wat het toont |
  |---|---|---|---|
  | M1 Melding | omvang 50,00 | gemelde uitkering 50,00 | de gemaakte Strandrecht houdt de creatiewaarde |
  | W1 Wrakrecht | grondslag 10,00 | uitkering 5,00 | de basis van de groep |
  | W2 Wrakrecht | grondslag 150,00 | uitkering 9,00 | de uitzondering van de groep |
  | S1 Strandrecht | grondslag 150,00 | uitkering leeg | een gewone Strandrecht valt buiten de groep |

  De case eindigt `consistent` en valideert tegen het case-schema (rechtstreeks gecontroleerd:
  0 fouten).
- **NOTE (Engels).** Zegt wat de golden pint en bevat de weigering door de mutant, de metingen
  en de no-op-weerlegging. Er staan geen lokale paden en geen procestaal in.
- **Gemeten 2026-09-25:**
  - verifier main PASS, verifier met de arm PASS (8f0bca49…);
  - m10 (arm in beide richtingen, ccc13603…) weigert de golden met:

        WF-050 FAIL /rules/prioritizedDefaults/0: an object creation also initialises defaults target 'Wrakrecht.uitkering' through its type chain — the group declared on Wrakrecht, the created type Strandrecht — and an object creation is never folded into a stack, so an instance of Strandrecht it creates has both writers

    Van de 762 documenten (golden 252, rood 42, negatief 438, toka 30) verandert alleen deze golden,
    en gate 3 met m10 is rood (`FAIL: document corpus/golden/jutterswet-strandmelding/ir.json is
    not verifier-clean`);
  - `ref`: `oracle: PASS (5 expectations)`;
  - clingo: `clingo: PASS (5 expectations)`;
  - exec: `PLAYED 1`;
  - DRL: `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`;
  - no-op (`ref --noop`): rc 1, met `MISMATCH: M1.gemelde uitkering = leeg, expected 50` en
    hetzelfde voor W1 en W2. De golden is dus geen survivor.
- **IR-SPEC WF-050.** De ketenclausule noemt de golden bij de schone richting. De indexrij van
  WF-050 noemt hem in de goldenkolom.
- **Pins in scripts/check-drools.sh (voorstel):**
  - `EXPECTED_PLAYED` 197 → 198, met een redenblok "197 -> 198 (2026-09-25, card
    verifier-allocatie-keten): jutterswet-strandmelding, new and hand-computed — …";
  - `DRL_CHANNEL` klasse (A): regel `"jutterswet-strandmelding|EXCLUDE|ch. 7 prioritizedDefaults: no
    defaults lowering in this branch"`, op naamvolgorde na jutterswet-schorsing;
  - koptelling van klasse (A):

    | | was | wordt |
    |---|---|---|
    | regels in de klasse | 29 | 30 |
    | regels die op de defaults-zin weigeren | 28 | 29 |
    | daarvan alleen door exec gespeeld | 27 | 28 |
    | afstand DRL-telling tot exec-telling | 24 | 25 |

    De zin "The 25 is 29 exec-only players — those 28 …" is mee aangepast, en er staat een
    Recounted-zin bij voor deze kaart;
  - de zin boven de lijst: "seventy-six of the seventy-nine entries" (was 75 van 78, nageteld:
    A 29 + B 36 + C 10 = 75 van 78);
  - `EXPECTED_DRL_PLAYED` blijft 173.
  - Andere kaarten verhogen dezelfde pins; de hoofdsessie telt op.

### Sweep oud/nieuw (761 documenten)

De zones: golden 251, rood 42, negatief 438, toka 30. Er zijn drie binaries vergeleken:
- main: 7816a181…
- oud: d4752610…, de arm vóór de fixronde
- nieuw: 8f0bca49…, de definitieve arm

**Oud → nieuw (de fixronde).**
- Alleen `wf050-keten-creatie-naast-groep` verschuift van verdict (0 → 1).
- Geen ander rapport verandert, ook geen toka-rapport.
- De flat-restantgetuige wordt door beide geweigerd. Haar rood-eerst staat tegen m8, zie boven.

**Main → nieuw (de hele kaart).**
- Er verschuiven vier verdicts, precies de vier ketengetuigen: `wf026-keten-twee-toewijzingen`,
  `wf050-keten-toewijzing-naast-groep`, `wf050-keten-restant-naast-groep` en
  `wf050-keten-creatie-naast-groep`.
- Daarnaast verandert alleen het rapport van toka/casus/ir.json, door de ontdubbeling van WF-026:
  de WF-026-regels gaan van 6 naar 3, met dezelfde verzameling regels, en er is geen verdictwissel.
- Golden 251/251 schoon, rood 42/42 schoon.

**Toka-tellingen** (alleen tellingen, geen citaten):

| | main | oud | nieuw |
|---|---|---|---|
| schone documenten | 2 | 2 | 2 |
| FAIL-regels | 46 | 43 | 43 |
| WF-026-regels | 6 | 3 | 3 |
| WF-050-regels | 0 | 0 | 0 |
| WF-079-regels | 1 | 1 | 1 |

Geen gate leest verifieruitvoer op toka.

**Gedragsgelijkheid.**
- WF-079 is identiek over alle documenten.
- Het commentaar-only verschil tussen f3f614bc en 8f0bca49 geeft een identieke sweep over 761
  documenten.

### Spec (U4), commit 40249b19

- **IR-SPEC WF-026 (ch. 3.2).**
  - Nieuwe clausule "**Through a type chain the rule is read per concrete type**": `targetType`
    ranget extensioneel, geen stapel neemt een allocatie op (RS ch. 7.10), twee allocaties op één
    keten paren als twee op één type, geen poort (anders dan WF-079). De melding staat op de versie
    van het lagere type. Ook staan er de uitdrukbare vormen en de getuige.
  - De v0-noot zegt nu dat de geporteerde skip op het gedeclareerde type keyt.
- **IR-SPEC WF-050 (ch. 6.1).** Na "(the ch. 7.1 single-writer lift)." staat nu "**Through a type
  chain the allocation clause is read per concrete type**, as WF-026's (ch. 3.2): …". Daarin staan
  de aandelen en het restant, geen poort, de melding op de groep en beide getuigen.
- **Blockquote in ch. 6.1.** Ongewijzigd.
- **WF-index.**
  - De WF-026-rij noemt de ketenclausule.
  - De schone goldens `jutterswet-hoofdberging`, `jutterswet-bergingsdeling` en
    `jutterswet-bergersdeling` hebben elk één allocatie over Hoofdberger op het geërfde
    `bergingsaandeel`, dus één schrijver in de keten.
  - De WF-026-rij noemt de nieuwe negatief; de WF-050-rij noemt de clausule en beide nieuwe
    negatiefs.
- **RS ch. 7.10.** Eén zin, aan het eind van de alinea "On an absorbed target …":

      An allocation is never folded — this section folds ordinary rules only — so WF-026 speaks through the chain without a gate: an allocation over a type and one over an ancestor both write the type's instances whatever defaults groups the chain declares, and so do an allocation and a defaults group on two levels of one chain, which WF-050's lift refuses the same way (`IR-SPEC.md` WF-026 and WF-050; witnesses `corpus/negatief/wf026-keten-twee-toewijzingen` and `corpus/negatief/wf050-keten-toewijzing-naast-groep`).

- **Fixronde (929cad43), IR-SPEC WF-050.**
  - De ketenclausule heet nu "**Through a type chain the allocation and creation clauses are read
    per concrete type**". Zij dekt de aandelen en het restant (beide richtingen) en de creatie
    (alleen `createdType` strikt onder het subject; een creatie van een voorouder blijft schoon),
    zonder poort. De melding staat op de groep, met drie getuigen.
  - De WF-050-rij in de index noemt de creatieclausule en de getuigen `wf050-restant-naast-groep`
    en `wf050-keten-creatie-naast-groep`.
  - RS ch. 7.10 is niet aangeraakt: de zin daar is waar zonder de creatie.
- **Naamtabel-toets.** `v1-naamtabel-toets: OK`, rc 0 na elke spec-edit, ook in de fixronde. De
  naamtabel is niet gewijzigd.
- **Nummers.** Geen D-/U-nummers in spec, comment of NOTE.

### Gates (elk als losse aanroep na `eval $(opam env)`, op 4cf35679, werkboom schoon)

- Gate 1: `check-self-contained: PASS`
- Gate 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 294 file(s) validate and round-trip, floor 192)`
- Gate 3: `check-verifier: PASS (294 golden clean; 438 negatief rejected for their declared rule, floor 350; 280 on their declared arm (expect.txt), floor 260)`
- Gate 5: `check-oracle: PASS (ref suite 994 worked examples, floor 489; 644 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)`
- Gate 6: `check-clingo: PASS (backend suite 1733 checks, floor 83; 644 cases replay exactly through clingo — ref = clingo over the corpus)`
- Gate 7: `check-noop: PASS (606 cases refute a do-nothing executor; 38 pinned survivors pin defeasible standing)`
- Gate 10: `check-regelspraak: PASS — JUnit suite green (618 tests, floor 173); 82 rgs golden(s) re-emit byte-identically (floor 80)`
- Gate 12: `check-drools: PASS`. Uit het rapport:
  - `played 198  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)`
  - `channel floor 54 non-PLAYED goldens pinned by name; played matches the pinned 198`
  - `DRL-text branch played 173  (its OWN count — the narrower branch, not the square; pin 173)`
  - `DRL-text floor 79 non-PLAYED goldens pinned by name; sweep cost 126s wall clock`
  - `JUnit 776 tests, floor 239  (the backend's own suite, green with a non-zero count)`

  De `mvn package` van de gate zelf was de enige jarbouw van deze ronde; de looptijd was 259 s.
- Gate 13: `check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`. Hij gebruikte de verse jar van gate 12 (`drools-jar: fresh (jar newer than 158 source files)`).
- Naamtabel-toets: `v1-naamtabel-toets: OK` (rc 0)

Gewijzigde scriptregels: alleen scripts/check-drools.sh, de pins van gate 12 hierboven (voorstel).

### Aanvullende metingen (geen gates)

**Suites die de verifierbibliotheek gebruiken.** Rechtstreeks gedraaid na `dune build ref backends
frontends/flogic`, met de nieuwe verifier; na de fixronde opnieuw gedraaid, met gelijke tellingen:
- `test_clingo: 1733 checks, 0 failures`
- `test_ref: all 994 worked examples pass`
- F-logic runtest `150 checks, 0 failures`
- De catala-suite heb ik niet gedraaid. De frontend kent geen typehiërarchie (`ot_supertype =
  None`) en geen allocaties (statisch nagelopen).

**Randproeven** ((buiten de repo), geëmitteerd uit RegelSpraak):

| proef | uitkomst |
|---|---|
| drie niveaus (allocaties over Berger, Hoofdberger en Opperberger) | 3 WF-026-ketenmeldingen: 1 op Hoofdberger, 2 verschillende zinnen op Opperberger |
| twee allocaties over Berger plus één over Hoofdberger | vlak WF-026 en precies 1 ketenmelding (ontdubbeld) |
| zustertypen (Hoofdberger en Hulpberger) | PASS |
| ander attribuut | PASS |
| groep op Hoofdberger plus twee allocaties over Berger | vlak WF-026 en 1 WF-050-ketenmelding (ontdubbeld) |
| keten met groep plus allocaties op twee niveaus | WF-026-ketenmelding (geen poort), WF-050 vlak en WF-050 keten |
| groepen op Berger en op Hoofdberger plus een allocatie over Hoofdberger | WF-050 vlak op de Hoofdberger-groep, keten op de Berger-groep |
| groep op het middelste type, allocatie over het laagste | WF-050-keten, lager type Opperberger |

### Wat aan de kaarttekst niet klopte (gemeten)

1. **"(b) De lift van WF-050 (groep op het subtype, toewijzing op het supertype)".** Die lift heeft
   de kloof in beide richtingen. Ook de omgekeerde plaatsing (groep op Berger, allocatie over
   Hoofdberger) is verifier-PASS, met ref geen fixpunt en clingo H1 `[7;1000]`. En de lift heeft de
   kloof in beide helften: de restantschrijving op `sourceType` gedraagt zich net zo (B1 `[7;1]`).
2. **U3 "twee getuigen".** In de eerste ronde werden het er drie (zie ontwerpbesluit 4). Zonder
   de derde is de restanthelft van de ketenarm niet falsifieerbaar. Na de fixronde zijn het er vijf:
   vier ketengetuigen en de vlakke restantgetuige.
3. **"alleen de twee getuigen verschuiven".** Twee afwijkingen:
   - tegen main verschuiven vier verdicts, precies de vier ketengetuigen. De vijfde getuige
     (restanthelft, vlak) weigerde main al; haar rood-eerst staat tegen m8;
   - door de U2-ontdubbeling, die WF-026 als geheel raakt, verandert ook het rapport van één
     toka-document, zonder verdictwissel (zie de sweep).
4. **U2 "Pointer op de versie van het laagste type".** Alleen op WF-026 toegepast. WF-050 meldt op de
   groep, zoals zijn vlakke lift (zie ontwerpbesluit 3).
5. **U2-zin.** Vier gemeten afwijkingen; zie "De definitieve weigerzinnen". Het voorbeeldattribuut
   `bergloon` bestaat in het meetdocument niet; het is `bergingsaandeel`.
6. **"ch. 7.10 vouwt alleen rekenregels".** De vouw neemt gewone regels op: rekenregels én
   initialisaties (`level_rules` in desugar.ml). De spec-zinnen zeggen daarom "ordinary rules".
7. **May-touch.** De may-touch-regel noemt RUNTIME-SEMANTICS.md niet, maar U4 vraagt er één zin in.
   Die zin is geschreven, als voorstel.
8. **Basis.** Het regime noemt basis main 2f47d80e. De worktree staat op 20e508d5, zoals de opdracht
   zegt.

### Open punten

De punten 1 en 2 van de eerste ronde zijn in de fixronde gefixt:
- de creatieketenarm (ad042847, 929cad43);
- de getuige van de restanthelft van de vlakke clausule (1c6128f7).

De nummering hieronder is nieuw.

1. **Gefixt in 4cf35679:** de richtingsvoorwaarde heeft nu haar getuige,
   `corpus/golden/jutterswet-strandmelding` (zie "De golden"). Oorspronkelijke tekst:
   **De richtingsvoorwaarde van de creatie-ketenarm heeft geen corpusgetuige.** Mutant m10, die de
   arm in beide richtingen laat vuren, verandert 0 van de 761 documenten, en gate 3 blijft groen.
   Toch weigert m10 de omgekeerde proef ten onrechte, terwijl `ref`, clingo en exec het over die
   proef eens zijn.
   - Een negatief kan dat niet vastleggen, want een negatief laat alleen zien dat een document
     geweigerd wordt, niet dat een schoon document schoon blijft. Alleen een golden kan het.
   - Kant-en-klare kandidaat: (buiten de repo) met case-c-omgekeerd.json.
     Gemeten: ref PASS (3), clingo PASS (3), exec PLAYED 1, DRL EXCLUDE (defaults).
   - Een golden valt buiten de may-touch (corpus/golden). Hij verschuift ook de pins van gate 12
     (`EXPECTED_PLAYED` +1, een DRL_CHANNEL-rij), en dat zijn beschermde scripts. Dit beslist de
     hoofdsessie.
2. **De exec-transpiler van de Drools-backend keyt zijn principiële skip op het gedeclareerde type.**
   Ook de DRL-tak weigert de ketenparen niet. Onder U1 veranderen de uitvoerders niet. De spec
   noemt de vormen nu ill-formed, en een uitvoerderskaart kan de skip op de keten laten keyen als de
   hoofdsessie dat wil.
3. **De meldingsfout van gate 3 bestaat nog** (open punt 2 van de supertype-kaart). Een getuige die
   de verifier passeert en een expect.txt draagt, krijgt ook de regel "rejected for WF-0xx, but not
   on the arm …". Die regel is onwaar. Opnieuw gezien bij de mutanten, en in de rood-eerstmeting
   van `wf050-restant-naast-groep` met m8.
4. **De negatief-README noemt de nieuwe getuigen niet.** Zoals bij de supertype-kaart staan
   getuigen daar alleen per migratiestap. Rijteksten, als de hoofdsessie ze wil:
   - `| wf026-keten-twee-toewijzingen | WF-026 | — | toegevoegd | two allocations write target (Hoofdberger, bergingsaandeel) through its type chain … | een allocatie over Berger en een over Hoofdberger op één doel |`
   - `| wf050-keten-toewijzing-naast-groep | WF-050 | — | toegevoegd | an allocation also writes defaults target 'Hoofdberger.bergingsaandeel' through its type chain … | groep op Hoofdberger, allocatie over Berger |`
   - `| wf050-keten-restant-naast-groep | WF-050 | — | toegevoegd | an allocation also writes defaults target 'Wrakberging.onverdeelde rest' through its type chain … | groep op Wrakberging, restant van een allocatie uit Berging |`
   - `| wf050-keten-creatie-naast-groep | WF-050 | — | toegevoegd | an object creation also initialises defaults target 'Strandrecht.uitkering' through its type chain … | groep op Strandrecht, creatie van een Wrakrecht die uitkering initialiseert |`
   - `| wf050-restant-naast-groep | WF-050 | — | toegevoegd | an allocation also writes defaults target 'Berging.onverdeelde rest' | groep op Berging, restant van een allocatie uit Berging (vlak) |`
5. **De toelichting in IR-SPEC ch. 6.1 noemt alleen de ketenclausule van WF-079** ("WF-079's chain
   clause (ch. 3.1) asks exactly that …"). De clausules van WF-026 en WF-050 staan in hun eigen
   bullets. Ch. 6.1 is niet aangepast, omdat U4 zegt "ch. 6.1's zin blijft". Een verwijzing daar
   beslist de hoofdsessie.
6. **Niet gedraaid: gates 4, 8, 9, 11 en 14.** Gates 1–3, 5, 6, 7, 10, 12 en 13 zijn wel gedraaid, zie
   "Gates". Voor de overige gates is wel het volgende rechtstreeks gemeten:
   - de nieuwe case valideert tegen het case-schema (0 fouten), wat gate 4 ook toetst;
   - de F-logic-suite (150 checks, groen), die gate 14 draait;
   - de catala-suite heb ik statisch nagelopen.

   Gate 8 (rood) en gate 11 (de toka-pariteitszone) raken deze wijziging niet: er is geen nieuw
   rood-document, en voor toka zijn alleen tellingen gemeten. De uniedraai is de bevestiging.
7. **CONSTRUCTS.** De kaart vraagt geen registerzin. WF-026 verwijst naar "Delta (promotion, row
   17)". Of rij 17 de ketenclausule moet noemen, beslist de hoofdsessie.
8. **In de kop van klasse (A) klopt een oudere telling niet** (gemeten, niet aangepast). De zin
   "jutterswet-diepwrakbasis joins the class, 28 -> 29 lines and 23 -> 24 (195 -> 196 at
   EXPECTED_PLAYED)" noemt 195 -> 196. Het redenblok van `EXPECTED_PLAYED` zet diepwrakbasis op
   196 -> 197. Andere kaarten wijzigen dezelfde kop tegelijk, dus de hoofdsessie telt op.

## Lens en fixronde (2026-09-25, hoofdsessie)

Lens (uitvoerders-trio, unie 36750511, eigen meting in kopieën): voor deze kaart GOEDGEKEURD, geen vondst. Gemeten: de ketenhelpers (`chain_lower`, `chain_pairs`, `distinct_form`, `dedup_reports`) worden door WF-026, de aandelen-, restant- en creatie-armen van WF-050 en WF-079 gedeeld, zonder gedupliceerde ketenlogica; eigen proefdocumenten voor elke richting en elk niveau (drie niveaus, zustertypen, kenmerkdoel, de uitdrukbare vormen) geven precies de spec-uitkomst; de uitvoerders gedragen zich zoals IR-SPEC zegt (ref en de exec-tak geen fixpunt, clingo twee modellen, de DRL-tak een stil antwoord op het ketenpaar — het laatste valt onder U1: de verifier is de poort); de mutanten m1/m3/m4/m9/m10 maken elk precies één getuige rood (m10, de creatiearm in beide richtingen, alleen de golden `jutterswet-strandmelding`); de sweep basis→unie laat alleen de vier ketengetuigen van verdict wisselen; in de pariteitszone gaan de WF-026-regels van zes naar drie door de ontdubbeling per (pointer, zin), met dezelfde unieke regels. De pins van gate 12 en 7 op de unie zijn nagerekend tegen de lijsten (204 = 197+2+1+1+1+1+1; 177 = 173+2+1+1; kloof 27 = 31 alleen-exec − 4 alleen-DRL; 635 + 42 = 677).

Fixronde: niets voor deze kaart. Wel een gevolg elders: de ontdubbeling verlaagde het aantal meldingen op de casusdump van de pariteitszone van 18 naar 15; de toka-README en gate 11 noemden nog 18 (gefixt in de toka-fixronde). De registertekst voor rij 17 staat in CONSTRUCTS.
