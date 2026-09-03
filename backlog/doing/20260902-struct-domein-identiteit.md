# Struct-domein-identiteit: de zustertweeling van WF-089/WF-023

Goal: Fixronde 2 van de use-site-declaratie-identiteit-kaart heeft de
struct-tweeling van het domein-identiteitsgat GEMETEN en bewust buiten
scope gelaten (de maintainerterm "keuzetype-familie" dekt Enumeratie en
SumType, geen structdomeinen): een `StructLiteral` van structdomein B
geschreven in een attribuut van structdomein A is verifier-schoon; ref
fault bij de veldlezing en clingo leidt niets af waar een waarde
verwacht werd — een fout antwoord, dus een verifier-schone
ref≢clingo-divergentie van precies de klasse die WF-089 voor keuzetypes
dichtte. De uitbreiding is al gebouwd en doorgemeten in die fixronde
(0 over-afwijzing over 197 documenten, waarvan 9 met 55
struct-getypeerde referenties; het E35-reproductiedocument vangt hij) en
is dus gemeten gratis; wat ontbreekt is het maintainerbesluit over de
term (de "keuzetype-familie" verbreden, of structdomeinen als eigen
zusterclausule onder WF-023/WF-089 brengen) en het volle regime
(negatief per as, rood-eerst, indexrijen). De meetstand staat in de
WF-023-clausule (ch. 3.1), de WF-023-indexrij en de WF-089-clausule.

Acceptance:
- De termkeuze als genummerde vraag aan de maintainer vóór de bouw.
- Rood-eerst: het struct-reproductiedocument (StructLiteral-drift, en de
  lees-tweeling als die bestaat) verifier-schoon getoond vóór de regel,
  afgewezen erna; per as een negatief met NOTE en mutant-bewijs.
- De "should preclude"-restzinnen in `ref/lib/eval.ml` en de
  spec-clausules die het gat nu documenteren, aangescherpt naar de
  nieuwe stand.
- Nul over-afwijzing over golden + rood + toka (read-only), gemeten;
  alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `spec/IR-SPEC.md` (proposal),
`corpus/negatief/**`, `ref/**` (meldteksten), `backlog/`, en — verruimd
in fixronde 1 — `corpus/golden/**` BUITEN `corpus/golden/anchor/` (dat
blijft beschermd): de ci-getuige van openstaand punt 1 kan alleen een
golden zijn, dezelfde vorm die `jutterswet-vaarwaterspelling` voor
WF-089 heeft. Zie §9.

Out of scope: keuzetype-armen (geland), de WF-061-expressielaag/
declaratiekant (eigen open punt), nieuwe struct-constructen.

## Bouwnotitie

**Maintainerbesluit (2026-09-03).** De regel landt als eigen nummer
**WF-090 "struct-domein-identiteit"**: eigen clausule, eigen indexrij,
eigen negatiefs; de verifier-machinerie gedeeld met WF-089/WF-023; de
ch.-2.4-term "keuzetype-familie" blijft ONgewijzigd (structdomeinen
vallen er niet onder).

### 1. Reproductie, rood-eerst, alle flanken

Tien documenten in `/tmp/sd-flanken/` (buiten de repo), gebouwd op de
`anchor/jutterswet-vondstdossier`-golden met een tweede, intern
consistent structdomein `logboek` (`bedrag`, `aantal`) naast `dossier`
(`waarde`, `telling`) — andere veldnamen, zodat een veldlezing de drift
zichtbaar maakt. VOOR-meting met de HEAD-verifier, NA-meting met de
gelande verifier.

| flank | verifier vóór | ref | clingo | verifier ná |
|---|---|---|---|---|
| W1 regeldoel (StructLiteral B → attribuut A) | PASS | `EVALUATION FAULT: field "waarde" is not present on the record value` | `MISMATCH: J1.waardekopie is underived (leeg), expected 12550` | WF-090 FAIL |
| W2 objectcreatie-toekenning | PASS | zelfde fout | **`clingo: PASS (8 expectations)`** — groen op een document dat ref niet kan evalueren | WF-090 FAIL |
| W3 defaults-consequentie | PASS | zelfde fout | `TRANSLATION FAULT: unscalable datatype` (ook op de TROUWE tweeling — het dragerhek, geen bevinding) | WF-090 FAIL |
| R1 AttribuutRef (scalair) | PASS | `field "bedrag" is not present…` | `MISMATCH: J1.waardekopie is underived (leeg), expected 12050` | WF-090 FAIL |
| R4 AttribuutRef door `ListType` | PASS | zelfde fout | **`MISMATCH: J1.totaalwaarde = [0], expected 1250`** — fout antwoord | WF-090 FAIL |
| R2 BinderRef (lambdaparameter) | PASS | zelfde fout | **`MISMATCH: J1.totaalwaarde = [0], expected 1250`** — fout antwoord | WF-090 FAIL |
| R3 ParameterRef | PASS | zelfde fout | `TRANSLATION FAULT: a struct field-access base here is …` | WF-090 FAIL |
| R7 DimensieCel | PASS | geen route (het caseformaat draagt geen celinvoer) | idem | WF-090 FAIL |
| R5 foldbron (`elementObjecttype`) | PASS | zelfde fout | `TRANSLATION FAULT: expression kind not translated` (ook op de trouwe tweeling) | WF-090 FAIL |
| R6 aggregatie-doorgeefluik (`EERSTE`) | PASS | zelfde fout | idem | WF-090 FAIL |

Trouwe tweelingen blijven schoon: W1t `oracle: PASS` + `clingo: PASS`,
W2t `oracle: PASS (8)` + `clingo: PASS (8)`, R5t `verifier: PASS` +
`oracle: PASS`.

**Al gedekt, geen dubbele regel** (gemeten, niet aangenomen):

- struct-in-struct — een veldwaarde van domein B waar het veld A
  declareert: `WF-073 FAIL … field 'kern' value is struct 'logboek', but
  the declared field datatype is struct 'dossier'` (volle
  datatype-gelijkheid);
- het `FieldAccess`-resultaattype: `WF-074 FAIL … field access type
  struct 'logboek' differs from the declared field datatype struct
  'dossier'`;
- een functielichaam tegen zijn gedeclareerde resultaat: WF-067 kruist
  het volle datatype;
- `TemporalRead`/`Invoke`/`Lambda`-parameters: WF-049/068/070,
  structureel.

Bijvangst: die twee meldingen luidden vóór deze kaart "struct differs
from struct" — `datatype_detail` had geen `Struct_type`-arm. Toegevoegd
(de L2-F8-fix, nu voor structs).

### 2. Wat er landde

`spec/verifier/lib/checks.ml`: `struct_domain` (spiegelt
`keuzetype_domain`, mét de `ListType`-doorkijk), `wf090_struct_identity`
(spiegelt `wf089_domain_identity`, met de twee eenzijdige armen), en één
gedeeld ingangspunt `use_site_identity` waar de zes leesposities doorheen
lopen — de twee regels blijven mechanisch in de pas, maar zijn apart
falsifieerbaar. De schrijfhelft rijdt mee op de drie posities van
`wf023_assignment` onder eigen regelnummer; eenzijdig alleen binnen één
familie, net als de keuzetype-clausule. `Datatype_onbekend`, een
onoplosbare bron en een ontbrekend `type` defer-en zoals bij WF-023/089.

### 3. Negatiefs en mutantbewijs (gate 3: 206 → 216)

Elf mutanten in de git-archive-kopie `/tmp/K`, elk arm-exact:

| mutant | schoon geworden negatiefs |
|---|---|
| M1 WF-090 onderdrukt op schrijfpositie 1 | alleen `wf090-schrijf-regeldoel` |
| M2 idem positie 2 | alleen `wf090-schrijf-objectcreatie` |
| M3 idem positie 3 | alleen `wf090-schrijf-defaults` |
| M11 de hele structclausule uit `wf023_assignment` | precies de drie schrijfgevallen |
| M4 `Attribuut_ref`-arm terug naar WF-089-only | `wf090-attribuutref` + `wf090-attribuutref-lijst` |
| M5 `List_type`-recursie uit `struct_domain` | alleen `wf090-attribuutref-lijst` |
| M6 `Binder_ref`-arm | alleen `wf090-binderref` |
| M7 `Parameter_ref`-arm | alleen `wf090-parameterref` |
| M8 `Dimensie_cel`-arm | alleen `wf090-dimensiecel` |
| M9 foldbron-tak | alleen `wf090-foldbron` |
| M10 aggregatietak | alleen `wf090-doorgeefluik` |

### 4. Twee eerlijkheidsnotities (gemeten)

- ~~**De ci-clausule is ONgepind.**~~ **Gesloten in fixronde 1** — zie
  §8. Stand vóór die ronde: geen enkel golden-, rood- of
  pariteitsdocument spelde een struct-use-site anders dan zijn
  declaratie; de mutant die de vergelijking hoofdlettergevoelig maakt
  liet gate 3 groen (`check-verifier: PASS (170 golden clean; 216
  negatief …)`). Anders dan bij WF-089, waar
  `jutterswet-vaarwaterspelling` die clausule pint.
- **Vier van de zes leesarmen hebben geen positieve getuige.** Gemeten
  telling over golden + rood + toka (EINDstand, na de golden van §9):
  `AttribuutRef` 50 struct-getypeerde
  referenties, `BinderRef` 9, `ParameterRef` 0, `DimensieCel` 0,
  foldbron 0, doorgeeffold 0. Mutanten die op elke struct-getypeerde use
  site vuren maken TIEN documenten rood bij de `AttribuutRef`-arm en VIJF
  bij de `BinderRef`-arm (fixronde 2 — de getuigenis is dus breed, niet
  één document), en laten gate 3 groen bij de vier andere. Een golden is
  daar vandaag niet toe te voegen: het ASP-dragerhek weigert de vormen
  vóór de vertaling (gemeten op de trouwe tweelingen).

### 5. Over-afwijzing

Nul, met de gelande verifier (EINDstand, hermeten in fixronde 2):

    golden+rood: 171 documenten, WF-090-treffers=0
    toka:         30 documenten, WF-090-treffers=0

De struct-lading die daar doorheen ging: 10 documenten, 59
struct-getypeerde referenties (`AttribuutRef` 50 + `BinderRef` 9), 16
`StructLiteral`s
(`anchor/jutterswet-vondstdossier`, `jutterswet-dossierspelling` — de
golden van §9 —, `jutterswet-vangstwaardering`,
`jutterswet-waarderingsinvoer`, `jutterswet-dossierrecht-catala`,
`jutterswet-kamerhuishouden-catala`, `synth3-nestwaarde`,
`synth3-vondststaat`, `rood/list-element-overprecision`,
`rood/struct-field-overprecision`). De vorige ronde mat 9 / 55 / 15 —
dat was de stand VÓÓR de eigen ci-golden; op main reproduceert dat getal
nog exact.

De toka-poot draagt hier NIETS bij: gemeten draagt geen van de 30
pariteitsdocumenten één `StructType`, dus "nul treffers" is daar leeg,
geen bevestiging.

### 6. Gates (stand na fixronde 1)

    check-self-contained: PASS
    check-ir-schema:      PASS (schema valid; 171 file(s) validate and round-trip)
    check-verifier:       PASS (171 golden clean; 220 negatief rejected for their declared rule)
    check-case-schema:    PASS (schema valid; 419 case file(s) validate)
    check-oracle:         PASS (397 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)
    check-clingo:         PASS (397 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop:           PASS (361 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)
    check-rood:           PASS (22 red cases fault observably in ref and clingo); Drools leg PASS (5 fault-pinned, 17 not-carried with reason)
    check-catala:         PASS (frontend suite green, 201 checks; 12 catala goldens regenerate byte-identically)
    check-regelspraak:    PASS (JUnit 159 tests; 26 rgs goldens re-emit byte-identically)
    check-toka-parity:    PASS (29 toka cases through ref, clingo en Drools; nul stille divergenties)
    check-drools:         PASS (105 played, 42 auto-excluded met reden, 1 gelijste waardedivergentie)

`dune runtest`: 201 checks, 0 failures (waarvan `test_ref` 376 worked
examples).

### 7. Open gebleven

1. ~~**De ci-clausule van WF-090 is ongepind**~~ — **gesloten in
   fixronde 1**: `corpus/golden/jutterswet-dossierspelling`, zie §8.
2. **Vier leesarmen zonder positieve getuige** (ParameterRef,
   DimensieCel, foldbron, doorgeeffold) — geblokkeerd door het
   ASP-dragerhek, dat drie van die vormen vóór de vertaling weigert.
   Dezelfde blokkade als bij WF-089; hoort bij een dragerkaart, niet bij
   deze.
3. **Orderbaarheid van de doorgeeffolds voor structs.** `EERSTE` is in v0
   een ORDENENDE fold en `StructType` is equality-only (ch. 2.4), dus een
   volstrekt trouwe struct-`EERSTE` met twee bijdragers is een
   WF-038-operandvraag — kaart `20260902-doorgeeffold-orderbaarheid`,
   niet deze regel. Dezelfde grens aan de andere kant, gemeten in
   fixronde 1: WF-090's aggregatie-arm hangt bewust aan de
   DOORGEEFfolds, dus een REKENENDE fold (`SOM`, `TOTAAL`) met
   structoperanden van twee domeinen is verifier-schoon. Ook dat is een
   WF-038-operandvraag en geen fout antwoord — ref faalt fail-fast
   (`plus: expected a number, got {waarde: 120.5, telling: 3}`) en het
   ASP-dragerhek weigert de vorm (`TRANSLATION FAULT: expression kind
   not translated`). Genoteerd in de WF-090-clausule en -indexrij, geen
   eigen arm.
4. **Het clingo-dragerhek op struct-defaults, struct-parameters en
   struct-folds** — `TRANSLATION FAULT` op de trouwe tweelingen, dus geen
   divergentie maar wel een lege plek in het differentiaalvierkant.

## Fixronde 1 (2026-09-03)

De lens bevestigde de REGEL — elf mutanten reproduceren arm-exact, tien
negatiefs falen elk op precies één WF-090-melding, nul over-afwijzing
over 170 golden+rood- en 30 toka-documenten, alle twaalf gates groen —
en falsifieerde drie afsluitende CLAIMS. Alle vijf bevindingen verwerkt.

### 8. S1/S2/S3 — de inline structkopie, gesloten met een regel

De drie zwaarste bevindingen waren één route: het `StructType`-knooppunt
draagt een INLINE kopie van de domeindeclaratie, en de consumenten lezen
die kopie in plaats van de declaratie — refs gedeelde case-loader kruist
een record veldsgewijs tegen `velden` van de use site (`ref/lib/case.ml`)
en de ASP-backend codeert structtermen POSITIONEEL uit dezelfde lijst.
WF-072 kruiste die kopie alleen op een attribuut- of parameterdatatype
op TOP-niveau. Drie gemeten, schema-geldige tegenvoorbeelden:

| document | verifier vóór | ref | clingo |
|---|---|---|---|
| gedrifte `ListType`-elementkopie (`dossiers`) | PASS | `EVALUATION FAULT: field "waarde" is not present…` | **`PASS (3 expectations)`** |
| één expressie-`type`, veldnamen hernoemd | PASS | `oracle: PASS (8)` | `TRANSLATION FAULT: field "waarde" is not a declared field of struct "dossier" (WF-074)` |
| één expressie-`type`, alleen VOLGORDE gedraaid | PASS | `oracle: PASS (8)` | **`MISMATCH: J1.samenvatting = [dossier(503,2)], expected dossier(12550,2)`** |

De derde is een fout antwoord op een verifier-schoon document: de
backend leest positioneel `telling`=3 waar `waarde`=120.50 bedoeld was.
Voorbestaand op main — maar dat is precies waarom een claim van
onbereikbaarheid er niet overheen mocht.

Gekozen uitweg: de flank sluiten, niet de zin herschrijven.
`struct_use_site` is nu een datatypewandeling (spiegel van
`enum_use_site`) die door een `ListType`-element heen kijkt, aangeroepen
op de vier posities die WF-088 al liep — attribuut, parameter,
`FunctionDef` — plus twee die WF-088 niet nodig heeft: een struct-veld
binnen `domeinen` (WF-071 laat daar een ánder domein toe, terwijl
WF-060/065/071 enumeraties uit elke declaratiepositie hekken) en elke
expressie-`type`, via het typeprefix van `walk_expr`.

Twee posities bewust NIET gelopen, elk omdat geen negatief ze kan
isoleren: de eigen `velden` van een gematchte struct (`fields_agree` is
een gelijkheid op het HELE velddatatype, dus een gedrifte geneste kopie
laat de omhullende kopie al afwijken — het argument dat WF-073/074 voor
hun geneste vormen maken) en een sompayload (WF-060 laat een
structpayload in een declaratie helemáál niet toe — gemeten op een
handgebouwd document, `WF-060 FAIL … constructor payload is struct` —
dus zo'n payload kan alleen in een gedrifte inline somkopie zitten, en
dat is WF-061's al opgetekende open flank).

Vier negatiefs erbij, elk op precies één WF-072-melding en niets anders,
plus vijf mutanten in `/tmp/K`, elk arm-exact en met nul rode goldens:

| mutant | schoon geworden negatiefs |
|---|---|
| N1 `List_type`-recursie uit `struct_use_site` | alleen `wf072-lijstelement-drift` |
| N2 de `domeinen`-aanroep weg | alleen `wf072-domeinveld-drift` |
| N3 de `functionDefs`-aanroep weg | alleen `wf072-functiondef-drift` |
| N4 de expressielaag-aanroep weg | alleen `wf072-expressietype-volgorde` |
| N5 `struct_use_site` levert altijd `[]` | alle vijf (incl. het bestaande `wf072-use-site-drift`) |

Over-afwijzing na de verbreding, gemeten: 170 golden+rood-documenten
schoon (nu 171 met de nieuwe golden) en 30 toka-documenten zonder één
WF-072-treffer. Gate 3: 216 -> 220 negatiefs.

Daarmee vervalt de gefalsifieerde zin in de WF-090-clausule ("WF-072
pins every declared struct use site against its declaration") niet door
hem te schrappen maar door hem WAAR te maken; de WF-072-clausule zelf
draagt nu de gemeten motivering, en de ch.-4.9-alinea zegt erbij dat
WF-073/074 tegen de DECLARATIE kruisen en de backend tegen de kopie —
de reden dat de wandeling tot de expressielaag moest reiken.

De melding + het commentaar in `ref/lib/eval.ml` noemen nu beide regels
(WF-090 voor het DOMEIN, WF-072 voor de VELDLIJST van dat domein op de
draad) en beschrijven de loader-route de goede kant op: de loader
consulteert de declaratie niet, hij kruist tegen de inline kopie — en
die is nu aan de declaratie gepind.

### 9. S4 — de ci-getuige, openstaand punt 1 gesloten

`corpus/golden/jutterswet-dossierspelling`: klein en handgeschreven in
het Jutterswet-vocabulaire (geen kopie van de anker-golden, de vorm die
`jutterswet-vaarwaterspelling` voor WF-089 heeft). Twee use-sites
spellen hun domein `DOSSIER` waar de declaratie `dossier` zegt, met de
veldlijst exact herhaald — de leeshelft (`type` van de `AttribuutRef`
onder een veldlezing) en de schrijfhelft (`type` van de `StructLiteral`
die in een struct-getypeerd attribuut wordt geschreven).

    verifier: PASS   ref: oracle: PASS (3)   clingo: PASS (3)

Onder de hoofdlettergevoelige mutant (`norm a = norm b` -> `a = b` in
`wf090_struct_identity` én in de structklem van `wf023_assignment`)
wordt precies dit document rood, in beide helften, en géén ander:

    FAIL: document corpus/golden/jutterswet-dossierspelling/ir.json is not verifier-clean:
    WF-090 FAIL /resolvedRegels/1: a value of the struct domain 'DOSSIER' is assigned to a target of the struct domain 'dossier'
    WF-090 FAIL /resolvedRegels/0/waarde/base/type: attribute reference 'dossier' is typed with struct domain 'DOSSIER', but the declaration it reads from names struct domain 'dossier'

De val die de lens onderweg mat en die de kaart vermijdt, in fixronde 2
scherper gemeten: alleen een UNIFORME herspelling (élke `StructType` op
de draad op `DOSSIER`, de sleutel in `domeinen` nog `dossier`) blijft
onder de mutant groen — die pint WF-072's ci-opzoeking in `domeinen`,
niet WF-090's kruising. Een PARTIËLE herschrijving van alleen de twee
attribuutdeclaraties doet iets ánders: dan worden de twee
`FieldAccess`-bases binnen de `StructLiteral` zelf de gedrifte use
sites en gaat het document juist ROOD op
`/resolvedRegels/1/waarde/fields/{0,1}/value/base/type`. De NOTE draagt
beide varianten met hun uitvoer.

### 10. S5 — bewust gelaten, met motivering

De aggregatie-arm van WF-090 hangt aan `passthrough_folds`, dus een
REKENENDE fold met structoperanden van twee domeinen is verifier-schoon.
Gemeten: `SOM` met operanden `dossier` en `logboek` geeft `verifier:
PASS`, ref `EVALUATION FAULT: plus: expected a number, got {waarde:
120.5, telling: 3}` en clingo `TRANSLATION FAULT: expression kind not
translated`; dezelfde vorm met `MINIMUM` geeft wél `WF-090 FAIL … but
operand 1 names struct domain 'logboek'`. Geen fout antwoord, beide
executors falen waarneembaar, en de vraag is WF-038's operandtypering
(`SOM` eist numerieke operanden) — dezelfde as als openstaand punt 3.
Geen eigen WF-090-arm; wel expliciet genoteerd in openstaand punt 3, in
de WF-090-clausule (ch. 2.4) en in de WF-090-indexrij, want "de
aggregatie-positie" klonk zonder die zin volledig.

## Fixronde 2 (2026-09-03)

De lens bevestigde opnieuw de REGEL en de vorige fixronde — geen fout
antwoord, geen verifier-schone ref≢clingo-splitsing, alle twaalf gates
groen — en falsifieerde zes afsluitende CLAIMS in de PROZA. Alle zes
verwerkt; alles hieronder zelf hermeten.

### 11. S6 — de derde ongelopen positie: het `type` van een regel-DOEL

De kernzin "every `StructType` on the wire, at every depth and in every
section" was te sterk. `wf072` loopt de modellaag plus, via het
typeprefix van `walk_expr`, de expressielaag — en een regel-DOEL is in
die wandeling geen expressieknoop maar WF-022's lvalue. Gemeten:

    struct-getypeerde `doel/type` in het corpus: 5 goldens + 2 rood
      anchor/jutterswet-vondstdossier   /resolvedRegels/2/doel/type
      jutterswet-dossierspelling        /resolvedRegels/1/doel/type
      jutterswet-vangstwaardering       /resolvedRegels/0/doel/type (+ velddatatype)
      synth3-nestwaarde                 /resolvedRegels/0/doel/type (+ velddatatype)
      synth3-vondststaat                /resolvedRegels/0/doel/type
      rood/list-element-overprecision, rood/struct-field-overprecision

Twee sondes op `jutterswet-dossierspelling`, beide schema-geldig
(gate 2's schema):

| sonde | verifier | ref | clingo |
|---|---|---|---|
| A1 `/resolvedRegels/1/doel/type.fields` omgedraaid | PASS | `oracle: PASS (3 expectations)` | `clingo: PASS (3 expectations)` |
| A2 dezelfde velden hernoemd naar `bedrag`/`aantal` | PASS | `oracle: PASS (3 expectations)` | `clingo: PASS (3 expectations)` |

Dus: verifier-schoon en GEMETEN onschadelijk — precies de stand die de
zusterrij WF-088 voor enums al eerlijk opschrijft, want beide executors
schrijven door het GEDECLAREERDE attribuutdatatype. En de leugen laat
zich niet meesmokkelen: sonde B (`wf090-schrijf-regeldoel` met een
`doel/type` dat de leugen van de waarde DEELT, `logboek` op beide) blijft
rood —

    WF-090 FAIL /resolvedRegels/2: a value of the struct domain 'logboek'
    is assigned to a target of the struct domain 'dossier'

— want `wf023_assignment` kruist tegen het gedeclareerde datatype. De
positie is nu op alle vier de plaatsen genoemd met deze meting: de
WF-072-clausule, de WF-072-indexrij, de WF-090-clausule en de meldtekst
in `ref/lib/eval.ml`. De zin zegt nu "model- en expressielaag" in plaats
van "the wire".

### 12. S7 — de getallen hermeten op de EINDstand

De geciteerde getallen beschreven de corpusstand VÓÓR de eigen
ci-golden. Zelfde teller op beide checkouts:

    main (170 docs):  9 documenten,  55 referenties, 15 StructLiterals  (AttribuutRef 46, BinderRef 9)
    HEAD (171 docs): 10 documenten,  59 referenties, 16 StructLiterals  (AttribuutRef 50, BinderRef 9)

Bijgezet in de WF-090-indexrij en in §4/§5 hierboven. En "the only
positive witness" is met een echte over-vuurmutant weerlegd (extra
WF-090-melding zodra het use-site-`type` een structdomein noemt,
`dune build spec/verifier`):

    AttribuutRef-arm: 10 documenten rood (8 goldens + 2 rood)
    BinderRef-arm:     5 documenten rood

`anchor/jutterswet-vondstdossier` is dus de RIJKSTE getuige, niet de
enige. Ten slotte: de pariteitszone draagt nul `StructType`-knopen
(0 van 30 documenten), dus "read-only, the parity zone" bevestigt voor
WF-090/WF-072 niets — dat staat er nu bij.

### 13. S8 — het valrecept in de golden-NOTE stond omgekeerd

Zie §9. Gemeten onder de ci-mutant: het hoofdrecept reproduceert
letterlijk (`totaal rood onder ci-mutant: 1`, beide meldingen, alle 220
negatiefs blijven op hun eigen regel falen), maar de PARTIËLE
declaratieherschrijving maakt het document juist rood en alleen de
UNIFORME herspelling is mutant-schoon (`verifier: PASS`). De NOTE draagt
nu beide varianten met uitvoer.

### 14. S9 — de tweede buurflank: orderbaarheid

De WF-090-clausule zei "One neighbouring gap" waar er twee zijn; de
kaart (openstaand punt 3) was hier al eerlijker dan de spec. De trouwe
tweeling van `wf090-doorgeefluik` — `EERSTE` over `kern` van alle
Schuiten, alles domein `dossier`, met twee bijdragers — gemeten:

    verifier: PASS
    ref:      EVALUATION FAULT: unordered comparison: {waarde: 20, telling: 2} vs {waarde: 10, telling: 1}
    clingo:   TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)

Verifier-schoon, geen fout antwoord, beide executors falen waarneembaar:
een WF-038-operandvraag, exact de tweeling van de flank die WF-089 wél
opschrijft. Nu "Two neighbouring gaps" in de clausule, met een halve zin
in de indexrij.

### 15. S10/S11 — twee eenregelige rechtzettingen

- De docstring van `wf072` noemde een sompayload-arm die
  `struct_use_site` niet heeft en die het commentaar tien regels hoger
  uitdrukkelijk wegmotiveert. "or sum payload" geschrapt; de docstring
  verwijst nu naar dat commentaar en noemt de doel-`type` als derde
  ongelopen positie.
- De "May touch" van deze kaart is bijgewerkt naar `corpus/golden/**`
  buiten `corpus/golden/anchor/`, met de reden erbij: de ci-getuige van
  openstaand punt 1 kan alleen een golden zijn. De scopeverruiming stond
  in de diff maar nergens opgeschreven.
