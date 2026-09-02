# Use-site ≡ declaratie: het keuzetype-gat dat WF-088 niet dicht

Goal: WF-088 (en WF-061 voor de somvariant) binden een use-site aan het
domein dat hij **noemt** — dezelfde waarden, dezelfde volgorde. Geen
regel bindt een use-site aan de declaratie waaruit hij **leest**. Een
`AttribuutRef` mag dus een `type` dragen dat een ánder, intern consistent
keuzetype-domein noemt dan het attribuut waarvan hij leest; het document
is verifier-schoon, WF-063 controleert de armen tegen het genoemde
domein, en ref fault bij evaluatie op een label dat geen arm heeft
(`ref/lib/eval.ml`, beide Match-armen). Sinds fixronde 2 van DEZE kaart
dragen die armen de onbereikbaarheidsclaim weer terecht: de lees-, de
schrijf- en de aggregatiehelft samen sluiten elke route die de kaart en
haar twee lensrondes gemeten hebben.

De som-kant is pre-existent en identiek: WF-061 loopt attribuut- en
parameterdatatypes plus de nominale vergelijkingscheck in `walk_cond` —
geen expressie-`type`, geen `FunctionDef`. Het enige bestaande
kruisverband is WF-027's leaf-ref-cross-check, en dat leeft alleen in de
objectcreatie-initialisatie (negatief `wf027-lying-attribuutref-type`).

Twee flanken, één vraag:
1. **De use-site-kant.** Moet een expressie-`type` (en een
   `FunctionDef`-parameter/resultaat) tegen de declaratie gekruist worden
   die hij leest, zoals WF-023/WF-027 dat voor de soort doen? Dan is de
   ref-fault echt onbereikbaar en mag de belofte terug in de melding.
2. **De declaratie-kant.** WF-088 slaat `domeinen` bewust over (de
   declaratie ís de waarheid), maar een geneste `Enumeratie` ín een
   SumType-payloaddeclaratie is óók een use-site van dat enum-domein en
   wordt nergens gecontroleerd. Genoteerd in
   `corpus/negatief/wf088-functiondef-drift/NOTE.md`.

Acceptance:
- Beide flanken als flankenpaar gereproduceerd (rood-eerst), met de
  gemeten uitkomst per executor (ref, clingo, Drools).
- Een besluit: regel erbij (proposal-diff op het beschermde pad, met
  negatief-geval per as) óf een gemotiveerde vastlegging dat de melding
  de waarheid is en het gat blijft.
- Als de regel landt: de melding in `ref/lib/eval.ml` (beide Match-armen)
  en de S14-prosa terug aanscherpen.
- Alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `spec/IR-SPEC.md` (proposal),
`corpus/negatief/**`, `ref/**`, `backlog/`.

Out of scope: de v0-wire-vorm van `Enumeratie`/`SumType`, WF-087.

## Bouwnotitie (bouwagent, 2026-09-02; nog geen Result)

Maintainerbesluit: nieuwe regel **WF-089 "domein-identiteit"** — een
referentie-expressie waarvan het `type` een keuzetype-domein noemt
(Enumeratie of SumType) noemt hetzelfde domein (ci) als de declaratie
waaruit hij leest; de domeinuitbreiding van de soort-kruiscontrole
WF-023/WF-027/WF-078. Geland in `spec/verifier/lib/checks.ml`
(`keuzetype_domain` + `wf089_domain_identity`, aangeroepen in de
`Attribuut_ref`-, `Dimensie_cel`-, `Parameter_ref`- en
`Binder_ref`-armen van `walk_expr`), clausule + indexrij in
`spec/IR-SPEC.md` ch. 2.4 (verwijzingen ch. 4.1, 4.3 DimensionCellRead,
4.7 WF-078), zeven negatiefs, `ref/lib/eval.ml` (beide Match-armen) en
één S14-alinea in `spec/ASP-TRANSLATION.md`.

### Meettabel (rood-eerst; /tmp/1a-flanken, generator gen.py)

Twee intern consistente domeinen per document: enum A `Vaarwater`
{Wad,Zeegat,Ruimzee} / B `Kust` {Noord,Zuid,West}; som A `beschikking`
{Toegekend(bedrag),Afgewezen,Aangehouden} / B `oordeel`
{Goedgekeurd(bedrag),Verworpen,Uitgesteld}. De referentie draagt type B,
de gelezen declaratie is A. Verifier-vóór = HEAD vóór deze kaart.

| # | referentiesoort | variant | verifier vóór | ref | clingo | Drools | verifier ná |
|---|---|---|---|---|---|---|---|
| F0 | controles (consistent) | enum/som | PASS | PASS | PASS | EXCLUDE Match / SumType | PASS |
| F1 | AttribuutRef, Match-scrutinee (kaartgeval) | enum | PASS | FAULT "no arm for enumeration value Wad" | MISMATCH: tarief underived (leeg) | EXCLUDE Expr:Match | WF-089 |
| F2 | AttribuutRef in gelijkheid met Literal B | enum | PASS | PASS (onwaar, stil) | PASS (onwaar, stil) | SKIP voorwaarde | WF-089 |
| F3 | AttribuutRef, Match-scrutinee | som | PASS | FAULT "no arm for constructor Toegekend" | MISMATCH: uitkering underived | EXCLUDE SumType | WF-089 |
| F4 | ParameterRef, Match-scrutinee | enum | PASS | FAULT (no arm) | TRANSLATION FAULT (symbolic_term-hek, S19) | EXCLUDE Match | WF-089 |
| F5 | ParameterRef, Match-scrutinee | som | PASS | FAULT (no arm) | TRANSLATION FAULT (unscalable datatype) | EXCLUDE SumType | WF-089 |
| F6 | BinderRef (Let-binder; waarde-type consistent A) | enum | PASS (WF-078 kruist alleen soort) | FAULT (no arm) | TRANSLATION FAULT unscalable (Let-over-enum, pre-existent hek, zie F6b) | EXCLUDE Match | WF-089 |
| F7 | BinderRef (Let-binder) | som | PASS | FAULT (no arm) | MISMATCH: uitkering underived | EXCLUDE SumType | WF-089 |
| F8 | DimensieCel (eigen type B, binnenref A) | enum | PASS | n.v.t. (case-formaat kent geen celinvoer) | n.v.t. | n.v.t. | WF-089 |
| F9 | TemporalRead (type B, tijdlijn A) | enum | **WF-049** | FAULT | TRANSLATION FAULT | EXCLUDE | WF-049 (geen arm nodig) |
| F10 | Invoke (type B, FunctionDef-result A) | som | **WF-068** | FAULT | TRANSLATION FAULT | EXCLUDE functionDefs | WF-068 (geen arm nodig) |
| F11 | Lambda-parameter B over bron-element A | som | **WF-070** ("sumtype, expected sumtype") | FAULT | MISMATCH som=0 (fout antwoord) | EXCLUDE ListType | WF-070 (geen arm nodig) |
| F12 | AttribuutRef `list of B` op attribuut `list of A` | som | PASS | FAULT (no arm) | **MISMATCH som = 0, verwacht 35.00 — fout antwoord** | EXCLUDE ListType | WF-089 (ListType-doorkijk) |
| F13b | Construct van B toegekend aan attribuut van A | som | PASS | schrijft `Verworpen` (alien constructor) | schrijft `verworpen` | EXCLUDE SumType | **WF-023** (schrijfzijde, geland in fixronde 1 als `wf023-schrijf-som-domein`) |
| F14 | AttribuutRef getypeerd Numeriek op enum-attribuut (eenzijdig) | enum | PASS | MISMATCH tarief = Wad (label in numeriek doel geschreven) | TRANSLATION FAULT unscalable | **ERROR ClassCastException** | WF-089 (eenzijdige arm) |
| F15 | AttribuutRef getypeerd enum B op numeriek attribuut (eenzijdig) | enum | PASS | FAULT "scrutinee is 4, not a constructor value" | TRANSLATION FAULT | EXCLUDE Match | WF-089 (eenzijdige arm) |
| F16 | AttribuutRef-type: som-KOPIE drift (extra constructor, zelfde domein) | som | PASS | PASS | PASS | EXCLUDE | PASS — WF-061-expressielaag, open punt (onschadelijk: beide executors lezen de declaratie) |
| F2b | consistente ref A = Literal B | enum | WF-036 | PASS | PASS | SKIP | WF-036 (WF-008-OPEN blijft zoals de index zegt) |
| F6b | controle: Let over enum, alles consistent | enum | PASS | PASS | TRANSLATION FAULT unscalable | EXCLUDE Match | PASS — pre-existent ASP-dragerhek, open punt |

Weggelaten als structureel onmogelijk: **Construct** leest geen
declaratie (hij noemt zijn eigen domein; WF-062) — de consumptie van een
Construct is de toekenning, F13b. **Lambda/FunctionDef-parameters** zijn
declaraties, geen referenties; hun lezers zijn BinderRefs (arm 4).

### Declaratiekant (geneste vormen in domeinen)

| | vorm | verifier | conclusie |
|---|---|---|---|
| D1 | Enumeratie als SumType-payload in `domeinen` | **WF-060** "constructor payload is enumeration, … not admissible" | afgewezen |
| D2 | Enumeratie als StructType-veld in `domeinen` | **WF-071** "field 'water' is enumeration, … not admissible" | afgewezen |
| D3 | Enumeratie als ListType-element (attribuut) | **WF-065** | afgewezen |
| D3b | ListType{Enumeratie} rechtstreeks in `domeinen` | **WF-065** `/domeinen/wateren` | afgewezen |
| D4 | gedeclareerde SumType als payload in `domeinen`, inline-kopie gedrift | PASS | open (WF-061 loopt alleen attributen/parameters; elke consument resolveert het genoemde domein) |
| D5 | idem als StructType-veld | PASS | open (idem) |

Conclusie: voor de Enumeratie is geen declaratie-kant-regel nodig —
WF-060/065/071 wijzen elke geneste vorm af; vastgelegd in de
WF-089-clausule en in `wf088-functiondef-drift/NOTE.md`. D4/D5 zijn
WF-061's declaratie-kant (niet deze regel), gemeten en als open punt
genoteerd.

### Regelvorm per referentiesoort (declaratiebron)

- `AttribuutRef` → `resolved_attr_datatype` (attribuut op het
  eindpunt-objecttype; genavigeerd kruist tegen het bereikte attribuut).
- `DimensieCel` → de binnen-`AttribuutRef` via `resolved_attr_datatype`;
  het celtype is het attribuutdatatype.
- `ParameterRef` → `par_datatype`.
- `BinderRef` → `bdt` uit de WF-078-omgeving (arm-payload uit
  `sum_decl`, lambda/FunctionDef-parametertype, Let-waardetype,
  filter-elementtype).
- `TemporalRead`/`Invoke`/`Lambda`-parameters: al structureel gekruist
  (WF-049/068/070) — geen arm; `Construct`: leest geen declaratie.
- `keuzetype_domain` kijkt door `List_type` heen (F12). Defer: onbekend-
  sentinel aan een van beide kanten, onoplosbare referentie (rolnaam-
  foldbron, kenmerk), ontbrekend type. Eenzijdig (slechts één kant noemt
  een domein) vuurt (F14/F15).

### Rood-eerst, mutanten, over-afwijzing

- Gate 3 op de pre-change verifier (/tmp/K0, git archive van HEAD vóór
  de wijziging, alleen de zeven negatiefs erbij): zeven maal `FAIL:
  negatief case … passed the verifier (must violate WF-089)`.
- Gate 3 ná: `PASS (167 golden clean; 185 negatief rejected)`.
- Mutanten (/tmp/K, git archive HEAD met de regel): m1 AttribuutRef-arm
  → de vier attribuutref-negatiefs schoon, rest rood; m2 ParameterRef-arm
  → alleen parameterref-enum schoon; m3 BinderRef-arm → alleen
  binderref-sum; m4 DimensieCel-arm → alleen dimensiecel-enum; m5
  ListType-doorkijk weg → alleen attribuutref-lijst-sum; m6 eenzijdige
  takken weg → alleen attribuutref-geen-domein; m7 Sum_type-regel van
  `keuzetype_domain` weg → de drie som-negatiefs schoon, de enum-negatiefs
  rood. Ongemuteerde kopie: gate 3 groen.
- Over-afwijzing: 197 documenten (145 golden + 22 rood + 30 parity,
  read-only) door de nieuwe verifier: **0 WF-089-treffers**.

### S14 (May-touch-uitbreiding, kaartacceptatie)

Eén alinea in `spec/ASP-TRANSLATION.md` S14 (de gedeelde-garanties-
alinea van de enum-scrutinee): een onafgeleide `matchN` betekent op een
verifier-schoon document alleen nog een lege scrutinee; de gemeten
"leeg om de verkeerde reden" en de gevouwen 0 (F12) zijn nu vóór de
vertaling afgewezen. Niets anders in dat bestand geraakt. **De alinea is
in fixronde 1 en nogmaals in fixronde 2 herschreven** — de geldende
tekst noemt alle drie de helften en zegt er expliciet bij dat de garantie
op de verifier rust en niet op het ASP-dragerhek; zie de sectie
"Fixronde 2" hieronder, niet deze alinea.

### Open punten (gemeten, niet geland)

1. **WF-023 domein-identiteit van de toekenning** (F13b): een Construct
   van domein B toegekend aan een attribuut van domein A is verifier-
   schoon (WF-023 vergelijkt alleen soort: sumtype = sumtype); ref én
   clingo schrijven de vreemde constructor. Zelfde regelvorm als WF-089,
   maar in `wf023` (doel-type vs. waarde-type).
2. **WF-061 expressielaag/declaratiekant** (F16, D4, D5): de spec zegt
   "elke use site", de verifier loopt alleen attribuut- en
   parameterdatatypes; een gedrifte som-KOPIE in een expressie-`type` of
   in een geneste `domeinen`-payload/-veld is verifier-schoon. Gemeten
   onschadelijk voor F16 (beide executors lezen de declaratie); D4/D5 niet
   executor-gemeten (geen case-vorm).
3. **ASP-dragerhek Let-over-enum** (F6b): een `Let` dat een enumeratie-
   waarde bindt faalt de vertaling met `unscalable datatype`, ook volledig
   consistent (ref PASS) — pre-existent, hoort in de S19-inventaris als
   het er nog niet staat.
4. **Drools crasht op een soort-leugen** (F14: `ClassCastException` bij
   fire) in plaats van luid te weigeren — na WF-089 onbereikbaar op een
   verifier-schoon document; blijft een robuustheidsnotitie voor de
   backend.
5. **WF-070-melding** "lambda parameter 'a' is sumtype, expected sumtype"
   verbergt het domein (datatype_kind); cosmetisch.
6. **WF-089 en `Enumeratie` zonder `domein`** (`domein: null`): telt als
   "noemt geen domein" en vuurt eenzijdig tegen een domein-dragende
   declaratie; geen corpus- of parity-document draagt die vorm (0
   treffers), dus onfalsifieerbaar gelaten.

## Fixronde 1 (2026-09-02, lensbevindingen L1-F1..F9)

Kern van de ronde: het maintainerbesluit is één principe —
keuzetype-**domein-identiteit** — en dat geldt aan de lees- én de
schrijfzijde. WF-089 dekte alleen de leeszijde; de lens bouwde zeven
verifier-schone documenten die een waarde van domein B in een declaratie
van domein A schrijven en daarmee precies de ref-fout bereikten wier
melding "unreachable" beloofde. Geland: de schrijfhelft.

### Wat er landde

- **WF-023 draagt de schrijfhelft**, op drie posities: regeldoel,
  objectcreatie-toekenning (attribuut; het kenmerk blijft WF-027) en
  prioritizedDefaults-consequentie (recursief door exceptiegroepen, die
  het worteldoel erven). De kruising gaat tegen het **gedeclareerde**
  doeldatatype, niet tegen het geserialiseerde `type` — een passend paar
  leugens (doel én waarde getypeerd met hetzelfde vreemde domein) is
  precies de vorm die de oude slice doorliet. Posities 2 en 3 hadden
  daarvóór *geen enkele* waarde-typering (ook geen soort): de familie- en
  eenheden-/tekenclausule geldt daar nu ook.
- **WF-067 kreeg zijn lichaamskruising** (`body`-type = gedeclareerd
  `result`, volledige datatypegelijkheid). Gevonden door ná de
  schrijfarmen opnieuw naar een route naar de "no arm"-fout te zoeken:
  WF-068 kruist de *lezing* van een functie, niets kruiste de
  *schrijving* in het resultaat.
- **WF-089 vergelijkt variant én naam** (`keuzetype = Kt_enum | Kt_sum`);
  `domeinen` is één naamruimte, dus dezelfde naam onder de andere variant
  is altijd een leugen. Eigen melding.
- **`datatype_detail` toont het keuzetype-domein en het lijstelement**;
  toegepast op de structurele gelijkheidsmeldingen WF-064/066/068/069/
  070/073/074 ("enumeration, but ... enumeration" is weg).
- **Zestien negatiefs erbij** (185 → 201 in gate 3), elk met NOTE en een
  falsificatie-recept.

### Meting per lensbevinding

| bevinding | actie | meting |
|---|---|---|
| F1 schrijfzijde | gefixt | de zeven lensdocumenten (`/tmp/lens1a-1/exp`) zijn nu alle zeven WF-023 FAIL; de trouwe tweelingen E0/E2/E5c/E24c blijven verifier-schoon |
| F2 onwaar ref-commentaar | gefixt | beide Match-armen noemen nu lees- én schrijfhelft; sondes P1-P8 vonden geen verifier-schone route meer naar de fout |
| F3 onwaar spec | gefixt | S14, WF-089-motivatie, WF-023-clausule + indexrij, WF-088's bungelende kaartverwijzing vervangen door de gemeten reststand |
| F4 variant | gefixt | E3 en E3r nu WF-089 FAIL met de variantmelding; negatiefs `wf089-enum-over-som`, `wf089-som-over-enum` |
| F5 eenzijdige tak | gefixt | negatief `wf089-attribuutref-geen-keuzetype-declaratie` (E27) |
| F6 genavigeerde lezing | gefixt | negatief `wf089-attribuutref-navigatie-enum` (E5) |
| F7 ci-clausule ongepind | **bewust gelaten** | mutant H (ci → hoofdlettergevoelig) laat gate 3 groen; E2-ci-domein passeert in alle drie. Een getuige vergt een synth-**golden** en `corpus/golden` valt buiten de may-touch van deze kaart; de WF-089-indexrij noteert de clausule nu expliciet als ongepind, zoals WF-088 dat voor zijn doel-type-uitzondering doet |
| F8 soort op positie 2/3 | gefixt | negatiefs `wf023-objectcreatie-soort`, `wf023-defaults-soort` |
| F9 melding zonder domein | gefixt | `datatype_detail` (zie boven) |

### Mutanten (gate 3 in /tmp/M, git-archive-kopie + de werkboomwijziging)

| mutant | gate 3 | gevangen door |
|---|---|---|
| A domeinclausule van `wf023_assignment` uit | rood | de negen `wf023-*-domein`/`-lijst-*`/`-doeltype-leugen`/`-spookdomein` |
| B schrijfpositie 2 uit | rood | `wf023-objectcreatie-domein` + `-soort` |
| C schrijfpositie 3 uit | rood | `wf023-defaults-domein` + `-soort` |
| D WF-067-lichaamsarm uit | rood | `wf067-body-result-drift` |
| E variantvergelijking uit | rood | `wf089-enum-over-som` + `wf089-som-over-enum` |
| F eenzijdige arm `Some _, None` uit | rood | `wf089-attribuutref-geen-keuzetype-declaratie` |
| G genavigeerde referenties overslaan | rood | `wf089-attribuutref-navigatie-enum` |
| H ci → hoofdlettergevoelig | **groen** | niets (F7) |

### Bereikbaarheidssondes ná de fix (/tmp/fr1a/probe)

Elke poging om opnieuw een verifier-schoon document te bouwen dat ref's
"no arm" bereikt, wordt bij naam afgewezen: P1 onbekend-sentinel als
regelwaarde → **WF-034**; P7/P8 dezelfde sentinel op de objectcreatie- en
defaults-positie → **WF-034** (de `Datatype_onbekend`-deferral van WF-023
is dus een verdedigingslaag, geen open route); P2 referentie die over een
ander attribuut liegt → **WF-089**; P4 onopgelost doel → **WF-022** (+
WF-023); P5 `Match`-waarde en P6b `Let`/`IfThenElse`-waarde van het
vreemde domein → **WF-023** (samengestelde waarden dragen hun eigen type,
dat de schrijfarm kruist); P3 waarde zonder `type` → LOAD ERROR. De enige
route die buiten de WF-laag blijft is de **case-invoer**, en die sluit de
gedeelde loader (`ref/lib/case.ml` weigert een label of constructor die de
declaratie niet draagt) — zo staat het nu ook in de melding en in de
WF-089-clausule, als benoemde route, niet als stilzwijgende belofte.

### Over-afwijzing

197 documenten (145 golden + 22 rood + 30 parity, read-only door de
verifier): **0 treffers** van WF-023, WF-067 of WF-089. De 28 niet-schone
parity-documenten die de meting passeert dragen WF-016/WF-022/WF-024 —
pre-existent en buiten deze kaart (die regels zijn niet aangeraakt).

### Open punten na deze ronde

1. ~~WF-023 domein-identiteit van de toekenning (F13b)~~ — geland; F13b is
   nu `wf023-schrijf-som-domein`.
2. WF-061 expressielaag/declaratiekant (F16, D4, D5) — ongewijzigd open.
3. ASP-dragerhek Let-over-enum (F6b) — ongewijzigd open (S19-inventaris).
4. Drools' `ClassCastException` op een soort-leugen — ongewijzigd
   robuustheidsnotitie.
5. ~~WF-070-melding verbergt het domein~~ — geland (`datatype_detail`).
6. WF-089 en `Enumeratie` zonder `domein` — ongewijzigd onfalsifieerbaar.
7. **Nieuw:** de ci-clausule van WF-089/WF-023 is ongepind (F7); een
   synth-golden met een ci-gespeld use-site-domein zou hem pinnen.
8. **Nieuw:** `RUNTIME-SEMANTICS.md` ch. 9.2 noemt voor de
   "no arm"-onbereikbaarheid alleen WF-063; de zin is ná deze ronde waar,
   maar de verwijzing is smal (WF-063/088/089 + WF-023/067). Buiten de
   may-touch van deze kaart.

Gates in de werkboom: 1 PASS, 2 PASS (167), 3 PASS (167 golden clean; 201
negatief rejected), 4 PASS (405 cases), 5 PASS (383), 6 PASS (383), 7 PASS
(347 + 36 survivors), 8 PASS (22), 9 PASS (201 checks; 12 catala-goldens).

## Fixronde 2 (2026-09-02, lensbevindingen L2-F1..F11)

Kern van de ronde: het principe was compleet aan lees- én schrijfzijde,
maar niet aan de **doorgeef**zijde. Een `Functie` van soort EERSTE /
LAATSTE / MINIMUM / MAXIMUM geeft een bijdrager ONGEWIJZIGD door — bij
één bijdrager doet `agg_extreme` geen enkele vergelijking — terwijl zijn
eigen `type` door geen enkele regel gekruist werd. Drie verifier-schone
documenten bereikten daarmee opnieuw precies de ref-fout wier melding
onbereikbaarheid beloofde. Bij het nameten kwam er een vierde bij die de
lens niet had: de **foldbron** zelf, de enige referentiepositie die de
expressiewandeling nooit binnengaat (hij wordt afgetakt naar de
foldbron-vormcontroles), mag dus liegen over het attribuut dat hij leest.

### Wat er landde

- **WF-089 krijgt de aggregatiepositie**: het knooptype van de
  element-doorgevende folds wordt tegen ELK operandtype gekruist met
  dezelfde helper. Het operandtype is zelf verankerd (door deze regel of
  door een structurele gelijkheid), dus de fold ketent door naar een
  declaratie in plaats van een domein wit te wassen. `SOM`/`TOTAAL`/
  `AANTAL` rekenen — daar reist geen keuzetype doorheen (nagemeten in
  `ref/lib/arith.ml`: alleen `agg_extreme` geeft een waarde door).
- **WF-089 krijgt de foldbron-arm**: dezelfde `AttribuutRef`-kruising,
  aangebracht op de plek waar de wandeling aftakt.
- **De WF-089-melding noemt zijn bron parametrisch** (`de declaratie` of
  `operand N`) in plaats van altijd "the declaration it reads from".
- **WF-062's payloadmelding** gebruikt nu `datatype_detail` zoals de
  zeven andere gelijkheidsmeldingen — "sumtype does not match the
  declared payload sumtype" toont voortaan beide domeinen.
- **Vijf negatiefs erbij** (201 → 206) en **één golden** (167 → 168).

### Meting per lensbevinding

| bevinding | actie | meting |
|---|---|---|
| L2-F1 aggregatie-doorgeefluik | gefixt | E30/E31/E32 waren `verifier: PASS`, zijn nu WF-089 FAIL; de trouwe tweeling E30c blijft schoon en replayt `oracle: PASS`. Nagemeten uitbreiding die de lens niet had: E38, de liegende **foldbron** — ook `verifier: PASS` vóór, nu WF-089 FAIL |
| L2-F2 onwaar ref-commentaar | gefixt | beide Match-armen noemen nu lees-, schrijf- én aggregatiehelft plus de foldbron; "unreachable" is daarmee weer waargemaakt |
| L2-F3 onwaar spec | gefixt | WF-089-clausule (onderwerp verbreed naar "use site that takes a keuzetype value"), WF-089-indexrij, ch. 4.1 domein-identiteitsalinea en de S14-alinea. S14 zegt er nu expliciet bij dat de garantie op de verifier rust en NIET op het ASP-dragerhek. De navigatieclausule staat nu alleen nog bij de attribuut- en dimensiecelarm |
| L2-F4 exceptiegroep-recursie ongepind | gefixt | negatief `wf023-defaults-exceptiegroep-domein`; mutant N3 (recursie uit) → alleen dit geval schoon |
| L2-F5 INITIALISATIE-positie ongepind | gefixt | negatief `wf023-initialisatie-domein`; mutant N4 (soort-tupel zonder `Initialisatie`) → alleen dit geval schoon |
| L2-F6 twee armen zonder positieve getuige | deels gefixt, deels vastgelegd | de nieuwe golden pint de ci-clausule (zie F7 hieronder); de ParameterRef- en DimensieCel-armen blijven zonder positieve getuige en dát staat nu in de indexrij, mét de reden dat het niet op te lossen is: het ASP-dragerhek weigert een keuzetype-getypeerde parameter vóór de vertaling (nagemeten — de variant met parameter geeft `TRANSLATION FAULT`, dus gate 6 zou breken) |
| L2-F7 structdomein-tweeling | **bewust gelaten**, belofte wél verwijderd | E35 blijft `verifier: PASS`; de uitbreiding is gemeten GRATIS (0 over-afwijzing over 197 documenten met 55 struct-getypeerde referenties) maar verbreedt de door de maintainer vastgelegde term "keuzetype-familie" — dus voorstel, geen landing. De onware belofte "(WF-074 should preclude this)" is uit `ref/lib/eval.ml` gehaald en vervangen door de gemeten reststand; de clausule (ch. 3.1) en de WF-023-indexrij leggen het gat vast |
| L2-F8 WF-062-melding | gefixt | `argument type sumtype 'advies' does not match the declared payload sumtype 'oordeel'` |
| L2-F9 kaart-Goal | gefixt | de alinea beschrijft nu de toestand ná fixronde 2, met het aggregatiegat als reden |
| L2-F10 achterhaalde meettabel | gefixt | rij F13b wijst naar `wf023-schrijf-som-domein`; de S14-paragraaf verwijst door in plaats van de oude tekst te herhalen |
| L2-F11 kenmerkdoel-terugval | gefixt | de WF-023-indexrij noemt nu beide gevallen waarin de geserialiseerde terugval leeft: een onoplosbaar doel (WF-022 verbiedt het) én een KENMERK-doel (WF-022 staat het toe en pint de waarde al op de booleaanse constante) |

### Mutanten (gate 3 in /tmp/K2, git-archive-kopie + de werkboomwijziging)

| mutant | gate 3 | gevangen door |
|---|---|---|
| N1 aggregatie-doorgeefkruising uit | rood | `wf089-aggregatie-doorgeefluik-enum` + `wf089-minimum-doorgeefluik` |
| N2 foldbron-arm uit | rood | `wf089-foldbron-enum` |
| N3 WF-023-exceptiegroep-recursie uit | rood | `wf023-defaults-exceptiegroep-domein` |
| N4 WF-023 zonder `INITIALISATIE` | rood | `wf023-initialisatie-domein` |
| N5 doorgeeffamilie tot EERSTE/LAATSTE beperkt | rood | `wf089-minimum-doorgeefluik` |
| H ci → hoofdlettergevoelig | **rood** (was groen) | `jutterswet-vaarwaterspelling` — de golden, en alleen die |

Elke mutant wordt door precies de bedoelde getuige(n) gevangen en door
geen andere. Ongemuteerde kopie: `check-verifier: PASS (168 golden
clean; 206 negatief rejected for their declared rule)`.

### De nieuwe golden (May-touch-uitbreiding van de hoofdsessie)

`corpus/golden/jutterswet-vaarwaterspelling` — een `Kadeplaats` met een
`vaarwater` van domein `Vaarwater`; het `kadegeld` volgt uit een `Match`
waarvan de scrutinee zijn domein **`VAARWATER`** spelt. Alle labels en
hun volgorde zijn exact herhaald, dus WF-088 zwijgt terecht; het enige
verschil is de kast van de domeinNAAM. Geen `bron.rgs`, geen
`bron.catala_en` (beide emitters leiden een referentietype uit de
declaratie af, dus de drift zou wegronddraaien). Pins EERST met ref
gemeten (`kadegeld` 5 resp. 9) en door clingo bevestigd; gate 7 faalt er
vanzelf op (afgeleide uitkomst, geen invoer — geen nieuwe survivor).

### Bereikbaarheidssonde ná de fix

De doorgeefkruising vuurt per operand en op elke operandvorm, niet
alleen op referenties: `EERSTE(Literal Noord:Kust, Literal Zuid:Kust)`
met knooptype `Vaarwater` levert twee WF-089-meldingen, één per operand.
De één-argumentsvorm zonder `elementObjecttype` en zonder
`dimensieSelectie` wordt al door WF-037 geweigerd ("value fold with fewer
than two operands"), dus die route bestaat niet.

### Over-afwijzing

197 documenten (145 golden + 22 rood + 30 parity, read-only) door de
nieuwe verifier: **0 treffers** van WF-023, WF-062, WF-067 of WF-089. De
meting is niet vacuüm: dezelfde 197 documenten dragen 20 operanden van
element-doorgevende folds in 6 documenten.

### Open punten na deze ronde

1. ~~WF-023 domein-identiteit van de toekenning (F13b)~~ — geland
   (fixronde 1).
2. WF-061 expressielaag/declaratiekant (F16, D4, D5) — ongewijzigd open.
3. ASP-dragerhek Let-over-enum (F6b) — ongewijzigd open (S19-inventaris);
   fixronde 2 voegt er twee hekken aan toe die dezelfde inventaris
   verdienen: een enum-operand in een fold en een keuzetype-getypeerde
   parameter, beide `TRANSLATION FAULT` vóór de vertaling.
4. Drools' `ClassCastException` op een soort-leugen — ongewijzigd
   robuustheidsnotitie.
5. ~~WF-070-melding verbergt het domein~~ — geland (fixronde 1).
6. WF-089 en `Enumeratie` zonder `domein` — ongewijzigd onfalsifieerbaar.
7. ~~ci-clausule ongepind~~ — geland: `jutterswet-vaarwaterspelling`.
8. `RUNTIME-SEMANTICS.md` ch. 9.2 noemt voor de "no arm"-onbereikbaarheid
   alleen WF-063; buiten de may-touch van deze kaart. Ná fixronde 2 is de
   volledige verwijzing WF-063/088/089 + WF-023/067.
9. **Nieuw: de structdomein-tweeling.** Een `StructLiteral` van
   structdomein B geschreven in een attribuut van structdomein A is
   verifier-schoon; ref fault bij de veldlezing en clingo leidt niets af
   waar een waarde verwacht werd — een fout antwoord. De uitbreiding is
   gemeten gratis (0 over-afwijzing, 55 struct-getypeerde referenties in
   9 documenten) maar verbreedt de maintainerterm "keuzetype-familie";
   vastgelegd in de WF-023-clausule, de WF-023-indexrij en de
   WF-089-clausule. **Eigen kaart / maintainerbesluit.**
10. **Nieuw: orderbaarheid van de doorgeeffamilie.** EERSTE / LAATSTE /
    MINIMUM / MAXIMUM zijn in v0 ORDENENDE folds, en ch. 2.4 noemt
    Enumeratie en SumType equality-only. Een volstrekt TROUWE `EERSTE`
    over een enum-attribuut is verifier-schoon en fault in ref zodra er
    een tweede bijdrager is (`unordered comparison: Zeegat vs Wad`),
    terwijl één bijdrager stil slaagt — gemeten. Dat is een
    WF-038-operandvraag, geen domein-identiteitsvraag; vastgelegd in de
    WF-089-clausule. **Eigen kaart.**
11. **Nieuw: twee WF-089-armen zonder positieve getuige.** Geen golden,
    rood- of parity-document draagt een keuzetype-getypeerde
    `ParameterReference` of `DimensionCellRead`, dus over-afwijzing in die
    twee armen is ongepind (mutanten die de arm onvoorwaardelijk laten
    vuren laten gate 3 groen). Onoplosbaar zolang het ASP-dragerhek een
    keuzetype-getypeerde parameter weigert; genoteerd in de indexrij.

Gates in de werkboom ná fixronde 2: 1 PASS, 2 PASS (168), 3 PASS (168
golden clean; 206 negatief rejected), 4 PASS (407 cases), 5 PASS (385),
6 PASS (385), 7 PASS (349 + 36 survivors), 8 PASS (22), 9 PASS (201
checks; 12 catala-goldens), 10 PASS (159 tests; 26 rgs-goldens), 11 PASS
(clingo 29 PASS / 0 divergentie, Drools 29 PLAYED / 0 EXCLUDE), 12 PASS
(played 104, auto-excluded 40 — de nieuwe golden draagt een `Match` en
zit in die 40; JUnit 125 tests). Verschoven tellingen t.o.v. fixronde 1:
gate 2 167→168, gate 3 167→168 golden en 201→206 negatief, gate 4
405→407, gate 5 383→385, gate 6 383→385, gate 7 347→349 (survivors
onveranderd 36), gate 12 auto-excluded 40 met de nieuwe golden erin
(played onveranderd 104, dus de golden wordt geweigerd en niet gespeeld;
de stand vóór deze ronde is niet apart nagemeten).
Gates 8, 9, 10 en 11 ongewijzigd.

## Result (hoofdsessie, 2026-09-02)

Geland: **WF-089, domein-identiteit voor de keuzetype-familie, over alle
drie de zijden.** De LEESzijde (bouwgolf): elke referentie-expressie —
AttribuutRef incl. navigatie-eindpunt, ParameterRef, BinderRef,
DimensieCel, met doorkijk door ListType — noemt het domein van de
declaratie waaruit hij leest, ci én per variant (Enumeratie ≠ SumType,
één naamruimte), met eenzijdige armen voor
referentie-zonder-domein-op-domein-declaratie en omgekeerd. De
SCHRIJFzijde (fixronde 1, lens-1-vangst): WF-023 kruist op drie
schrijfposities — regeldoel, objectcreatie-toekenning en
prioritizedDefaults-consequentie incl. exceptiegroepen en initialisatie —
tegen het GEDECLAREERDE doeldatatype; twee van die posities hadden
voorheen géén enkele waarde-typering (ook de soortlaag is daar nieuw), en
WF-067 pint het functielichaam op het gedeclareerde resultaat. De
DOORGEEFzijde (fixronde 2, lens-2-vangst): de element-doorgevende
aggregaties (EERSTE/LAATSTE/MINIMUM/MAXIMUM) en de foldbron kruisen hun
knooptype tegen de operanden. Elke gedichte route is vóóraf als
verifier-schone ref≢clingo-flank gemeten (meettabel F0–F16: ref fault of
schrijft het vreemde label, clingo leidt niets af, vouwt 0 of crasht de
vertaling; Drools tot ClassCastException aan toe); de
onbereikbaarheidsclaims in `ref/lib/eval.ml` (beide Match-armen), de
IR-SPEC-motivatie en S14 zijn pas ná hermeting met sondes teruggezet, en
RS ch. 9.2 noemt nu de volledige keten (hoofdsessie-commit).

Omvang: 28 nieuwe negatiefs (13× wf089, 14× wf023-schrijfposities, 1×
wf067) elk mutant-bewezen arm-exact; één nieuwe golden
`jutterswet-vaarwaterspelling` (May-touch-uitbreiding hoofdsessie) die
de ci-clausule falsifieerbaar maakt — pins eerst door ref, bevestigd
door clingo; de declaratiekant gemeten al dicht (WF-060/065/071);
`datatype_detail` toont domeinen in negen gelijkheidsmeldingen;
over-afwijzing driemaal gemeten nul over 197 documenten (golden + rood +
toka read-only). CONSTRUCTS rij 36 draagt de amendering
(hoofdsessie-commit; de kaart-May-touch is door de hoofdsessie verbreed
met de golden, CONSTRUCTS en de RS-regel).

Eindstand na bouw + 2 lensronden + 2 fixronden: **alle twaalf gates
groen** (eindgate-run vóór de rebase op 284508c; na de rebase op main
met de companion-kaart erin opnieuw volledig gedraaid — zie de
merge-commit). Tellingen: gate 2/3 = 168 goldens, 206 negatiefs (was
167/178); gates 5/6 = 385 (was 383); gate 7 = 349/36; gate 12 = played
104 / auto-excluded 40 (de nieuwe golden draagt een Match en wordt
reden-dragend geweigerd) / value-divergence 1 / no-case 1.

Open gebleven, elk gemeten en genoteerd: de structdomein-tweeling
(gemeten gratis maar buiten de maintainerterm — **eigen kaart**
`20260902-struct-domein-identiteit`); de orderbaarheid van de
doorgeeffamilie over equality-only-typen (**eigen kaart**
`20260902-doorgeeffold-orderbaarheid`); WF-061's expressielaag/
declaratiekant (F16/D4/D5, gemeten onschadelijk); het pre-existente
ASP-dragerhek Let-over-enum plus de twee nieuwe hekken
(S19-inventariswerk); Drools' ClassCastException op een soort-leugen (na
WF-089 onbereikbaar op verifier-schone documenten; robuustheidsnotitie);
`Enumeratie` zonder `domein` (onfalsifieerbaar, 0 dragers); twee
WF-089-armen zonder positieve getuige (ParameterRef/DimensieCel —
onoplosbaar zolang het ASP-hek een keuzetype-parameter weigert; in de
indexrij genoteerd).
