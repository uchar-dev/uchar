# Use-site ≡ declaratie: het keuzetype-gat dat WF-088 niet dicht

Goal: WF-088 (en WF-061 voor de somvariant) binden een use-site aan het
domein dat hij **noemt** — dezelfde waarden, dezelfde volgorde. Geen
regel bindt een use-site aan de declaratie waaruit hij **leest**. Een
`AttribuutRef` mag dus een `type` dragen dat een ánder, intern consistent
keuzetype-domein noemt dan het attribuut waarvan hij leest; het document
is verifier-schoon, WF-063 controleert de armen tegen het genoemde
domein, en ref fault bij evaluatie op een label dat geen arm heeft
(`ref/lib/eval.ml`, beide Match-armen dragen sinds fixronde 2 de eerlijke
melding in plaats van "unreachable on a verifier-clean document").

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
| F13b | Construct van B toegekend aan attribuut van A | som | PASS | schrijft `Verworpen` (alien constructor) | schrijft `verworpen` | EXCLUDE SumType | PASS — **WF-023-terrein, open punt** |
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
vertaling afgewezen. Niets anders in dat bestand geraakt.

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
