# WF-089's eerlijkheidsnotitie is achterhaald: de `ParameterRef`-arm heeft een getuige

Goal: De WF-089-indexrij in `spec/IR-SPEC.md` (ch. 9, de regeltabel)
draagt een eerlijkheidsnotitie met TWEE meetclaims die de
meldplichtspoor-golden nu falsificeert. Letterlijk: "The `ParameterRef`
and `DimensieCel` arms have **no positive witness**: no golden, rood or
parity document carries a keuzetype-typed `ParameterReference` or
`DimensionCellRead`, so OVER-rejection in those two arms is unpinned
(measured mutants that make either arm fire unconditionally leave gate 3
green); a keuzetype-typed parameter cannot be added to a golden today
because the ASP carrier refuses the form ahead of translation."

Gemeten in fixronde 1 van `20260902-meldplichtspoor-niveau0` (in een
`git archive`-kopie, de werkkopie ongewijzigd):

- `corpus/golden/jutterswet-meldplichtspoor` draagt een lijstparameter
  `meldreeks` van `ListType`-van-`SumType`, en `keuzetype_domain` in
  `spec/verifier/lib/checks.ml` kijkt door een `List_type` heen naar het
  element. De `ParameterRef` van de foldbron valt dus BINNEN de arm.
- De arm onvoorwaardelijk laten vuren en alle 169 golden + rood
  ir.json-documenten door de verifier halen maakt **precies één** document
  rood: die golden. De arm heeft dus een positieve getuige, en
  over-afwijzing erin is niet langer ongepind.
- De ASP-weigeringsclaim is eveneens onwaar: clingo speelt alle acht
  casussen van die golden exact (gate 6 corpusbreed groen).

De `DimensieCel`-arm blijft wél zonder getuige — het casusformaat draagt
geen celinvoer — dus de notitie moet gesplitst, niet geschrapt.

`spec/IR-SPEC.md` is protected en viel buiten de scope van de
meldplichtspoor-kaart; deze kaart is de voorgestelde tekstwijziging plus
de hermeting die haar draagt.

Acceptance:
- De WF-089-indexrij herschreven als PROPOSAL-diff: de
  `ParameterRef`-helft van de eerlijkheidsnotitie vervangen door de
  getuige (`jutterswet-meldplichtspoor`, de lijstelement-doorkijk in
  `keuzetype_domain`), de `DimensieCel`-helft ongewijzigd als nog-open
  meetgat, en de weerlegde ASP-weigeringszin geschrapt.
- De getuigenkolom van de rij noemt de golden expliciet, in de vorm die
  de andere rijen gebruiken.
- De mutantmeting hermeten en in de bouwnotitie opgetekend met echte
  uitvoer: onvoorwaardelijke arm → exact één rood document, arm hersteld
  → gate 3 corpusbreed groen (169 golden clean, 206 negatief rejected).
- Controleer bij het schrijven of een tweede WF-rij dezelfde
  "geen-getuige"-claim voert over een vorm die deze golden inmiddels wél
  draagt (WF-065's lijstelement-`SumType`, WF-070's somtype-accumulator);
  zo ja, in dezelfde diff meenemen.
- Alle twaalf gates groen.

May touch: `spec/IR-SPEC.md` (proposal — maintainer merget),
`backlog/`. **Verruimd in de fixronde (2026-09-05), met reden in
R5:** `spec/RUNTIME-SEMANTICS.md` (eveneens beschermd, dus deel van
hetzelfde voorstel) en `spec/ASP-TRANSLATION.md` (geen beschermd pad).
Beide beschrijven de vorm die deze kaart als getuige pint — een
lijstparameter van `SumType`-elementen — als ongedragen; de getuige
weerlegt dat, dus de zinnen moeten mee.

Out of scope: elke verifier-, schema- of corpuswijziging (de meting
vraagt er geen: de arm is al correct, alleen haar notitie is verouderd);
de `DimensieCel`-getuige (die vraagt celinvoer in het casusformaat — een
eigen kaart); `scripts/**`.

Result (2026-09-05): geland als **voorstel** op `spec/IR-SPEC.md`
(beschermd pad — de maintainer merget). De WF-089-eerlijkheidsnotitie is
gesplitst: de `ParameterRef`-helft is vervangen door haar getuige
(`jutterswet-meldplichtspoor`, de `ListType`-doorkijk, de hermeten
één-document-mutant met de letterlijke regel), de `DimensieCel`-helft
blijft staan als open meetgat — nu zelf hermeten en gedateerd — en de
weerlegde ASP-weigeringszin is geschrapt. De getuigenkolom noemt de golden
in de vorm van de andere rijen. **Er is wél een tweede rij, en die is in
de fixronde meegegaan in dezelfde diff**: WF-091's eerlijkheidsnotitie 3
sloot af met "no golden carries a keuzetype-typed or struct-typed
`ParameterReference` or `DimensionCellRead`, so the corresponding notes at
WF-089 and WF-090 stand unchanged" — vier conjuncten waarvan precies één
onwaar (de keuzetype-`ParameterRef`), en een slotclausule die door deze
eigen diff werd tegengesproken. Zie de fixronde-paragraaf hieronder.
WF-065 en WF-070 voeren inderdaad geen geen-getuige-claim, en WF-090's
claim over STRUCT-getypeerde gebruiksplekken blijft waar (hermeten: nul in
golden+rood én in de pariteitzone). Verifier, schema, corpus en scripts
ongewijzigd — de mutanten waren tijdelijk, draaiden in een `git
archive`-kopie buiten de werkkopie, en `checks.ml` is byte-identiek aan
main gebleven. Alle dertien gates groen, alle tellingen gelijk aan main
(176/297, 405/405).

## Bouwnotitie — de hermeting (2026-09-05)

De kaartcijfers hierboven zijn een gedateerde meting (169 golden / 206
negatief / twaalf gates). De stand bij het werken van de kaart: **176
golden+rood-documenten** (154 golden + 22 rood), **297 negatief**,
**dertien gates**.

### Nulmeting

```
$ sh scripts/check-verifier.sh
check-verifier: PASS (176 golden clean; 297 negatief rejected for their declared rule)
```

### De mutant

De `ParameterRef`-arm van WF-089 (`spec/verifier/lib/checks.ml`) tijdelijk
laten OVER-afwijzen: vuren zodra de kruising LEEFT (één van beide zijden
noemt een keuzetype), in plaats van alleen wanneer de twee verschillen. De
verifier daarna over alle golden- en rood-documenten:

```
== MUTANT BUILD OK ==
RED: corpus/golden/jutterswet-meldplichtspoor/ir.json
WF-089 FAIL /resolvedRegels/0/waarde/list/type: MUTANT over-rejection: parameter reference 'meldreeks' is refused for naming a keuzetype domain at all
== zwaai: 176 golden+rood documenten, 1 rood ==
```

**Precies één document rood, en dat is de getuige.** De `ParameterRef`-arm
van WF-089 heeft daarmee een positieve getuige: over-afwijzing erin is niet
langer ongepind. Het knooppunt zit op `/resolvedRegels/0/waarde/list` — de
foldbron, een `ParameterRef` naar `meldreeks` met type `ListType`-van-
`SumType 'gebeurtenis'`; `keuzetype_domain` kijkt door de `List_type` heen
naar het element, dus beide zijden noemen hetzelfde domein en de arm
zwijgt terecht.

### De `DimensieCel`-helft, in dezelfde ronde hermeten

Dezelfde over-afwijzende mutant op de `DimensieCel`-arm gelegd: **nul**
MUTANT-regels over de 176 golden+rood-documenten en nul over de 30
documenten van de pariteitzone. Golden en rood dragen twee celleesknopen
(`jutterswet-seizoenvondst`, `jutterswet-vormenkabinet`) en geen van beide
noemt een keuzetype; de pariteitzone draagt er geen enkele. Die helft van
de notitie blijft dus staan — nu gedateerd op de hermeting.

Van de negen pariteitdocumenten mét een `ParameterRef` vuurde de
`ParameterRef`-mutant er ook geen; de golden is de enige getuige in alle
zones. (De pariteitzone is buiten deze meting sowieso niet
verifier-schoon — die documenten dragen gewone WF-meldingen — maar de
loop bereikt de armen, wat de nulmeting daar betekenis geeft.)

### De weerlegde ASP-claim

De geschrapte zin beweerde dat een keuzetype-getypeerde parameter vandaag
niet aan een golden kan worden toegevoegd omdat de ASP-drager de vorm vóór
vertaling weigert. Gemeten, gericht:

```
== corpus/golden/jutterswet-meldplichtspoor/case1-op-tijd-gemeld.json
clingo: PASS (2 expectations)
… (case2 t/m case8 idem)
```

Alle acht casussen spelen exact; er is geen `TRANSLATION FAULT`. De zin is
onwaar en verdwijnt.

### Herstel

```
$ git checkout -- spec/verifier/lib/checks.ml && dune build --root spec/verifier
$ sh scripts/check-verifier.sh
check-verifier: PASS (176 golden clean; 297 negatief rejected for their declared rule)
```

### Andere rijen met dezelfde claim — de leesmiss van deze ronde

*Deze paragraaf stond hier oorspronkelijk als "Andere rijen met dezelfde
claim: geen". Dat was onwaar en de fixronde heeft het rechtgezet; de
oorspronkelijke redenering over WF-065/WF-070/WF-090 klopt en blijft
staan, de conclusie niet.*

Gegrepen op `no positive witness` / `no golden` / `cannot be added` /
`unpinned` in `spec/IR-SPEC.md`. De enige `no positive witness`-claim in het
hele document is inderdaad die van WF-089 — maar de grepterm `no golden`
**trof regel 3868 wel degelijk**, de WF-091-rij. Dat is niet gezien: die
rij is één regel van ruim 25 000 tekens, de treffer stond middenin
eerlijkheidsnotitie 3, en er is op de trefferlijst afgegaan in plaats van
op de treffer zelf. De rij voert dezelfde geen-getuige-claim over dezelfde
twee gebruiksplekken, en is in de fixronde alsnog meegegaan (zie onder).

De twee rijen die de kaart bij naam noemt voeren zo'n claim inderdaad
niet:

- **WF-065** (lijstelement-`SumType`) draagt geen geen-getuige-zin; haar
  getuigenkolom noemt `anchor/jutterswet-staffelvergoeding` +
  `anchor/jutterswet-vondstdossier`.
- **WF-070** (somtype-accumulator) evenmin; getuigenkolom
  `anchor/jutterswet-staffelvergoeding` + `anchor/jutterswet-aanspraaktelling`.
  Ch. 4.8 zegt zelfs uitdrukkelijk dat een `SumType`-accumulator toelaatbaar
  is.

De naastgelegen rij die wél een gelijkvormige claim voert is **WF-090**
(structdomeinen): "the `ParameterRef`, `DimensieCel`, fold-source and
aggregation arms carry no struct-typed use site anywhere in the corpus".
Die claim gaat over STRUCT-getypeerde gebruiksplekken, en deze golden
draagt een som-getypeerde lijstparameter, geen struct. Nagemeten over
golden+rood: nul struct-getypeerde `ParameterRef`-knopen. De claim blijft
waar en blijft ongewijzigd.

## Fixronde (2026-09-05)

Twee lenzen lazen de gelande diff na en bevestigden de kern (de getuige
klopt, de mutant klopt, de geschrapte ASP-zin was terecht geschrapt). Wat
ze vonden zit in de tekst eromheen. Vijf punten, elk met een eigen
hermeting; **elke meting is hier zelf opnieuw gedaan vóór het schrijven**,
in twee `git archive`-kopieën onder `/tmp`, zodat de werkkopie geen moment
een mutant droeg (`git diff main -- spec/verifier/lib/checks.ml` is leeg).

### R1 — het blokkerende punt: de slotzin van WF-091's notitie 3

De zin "no golden carries a keuzetype-typed or struct-typed
`ParameterReference` or `DimensionCellRead`, so the corresponding notes at
WF-089 and WF-090 stand unchanged" draagt **vier** conjuncten. Precies één
is onwaar, en de slotclausule ("stand unchanged") werd door deze eigen
diff tegengesproken — WF-089's notitie veránderde er immers in.

Hermeten met een arm-en-klasse-beperkte over-afwijzende mutant (vuur zodra
de kruising van díé arm voor díé klasse LEEFT), over alle 176
golden+rood-documenten en over de 30 pariteitdocumenten:

```
MUT=pk (ParameterRef × keuzetype) : 176 documenten, 1 rood
  corpus/golden/jutterswet-meldplichtspoor/ir.json
  /resolvedRegels/0/waarde/list/type: parameter reference 'meldreeks'
MUT=ps (ParameterRef × struct)    : 176 documenten, 0 rood
MUT=ck (DimensieCel  × keuzetype) : 176 documenten, 0 rood
MUT=cs (DimensieCel  × struct)    : 176 documenten, 0 rood
pariteitzone (30 documenten): 0 rood bij alle vier
```

De zin is vier-weg gesplitst: drie paringen leeg (dus WF-090's notitie
ongewijzigd op beide armen, en WF-089's `DimensieCel`-helft ook), de
vierde niet — en dát is precies de helft die deze diff veranderde.

### R2 — vijf mini-ingrepen in de WF-089-rij

- **(a) terminologiebotsing.** De getuige-zin zei "whose fold source reads
  a list PARAMETER". De fold-bron-arm is in deze rijen attribuut-only: ze
  vuurt alleen voor een `AttribuutRef`-foldargument, elk ander argument
  valt door naar de gewone expressiewandeling. De getuige zit dus op de
  **gewone `ParameterRef`-arm**, bereikt via de lijstoperand van de
  `Fold`. Herschreven, met "an ordinary walked reference on THIS arm, NOT
  the fold-source arm" erbij. En "carried end to end" is teruggebracht tot
  wat waar is: **twee benen van het vierkant** — `ref` en clingo spelen de
  acht casussen exact (poorten 5 en 6), de golden heeft géén frontendbron
  (`bron` is `null`, dus poorten 9 en 10 zien haar nooit), en de
  Drools-leg weigert haar. **Afwijking van het recept, na meting:** het
  recept sprak van een weigering van de lijst-*invoervorm*; de gemeten
  weigering zit een stap eerder, bij de IR-loader en vóór enige casus —
  `EXCLUDE loader: form not carried by the v0 Drools backend:
  Datatype:SumType (unknown datatype node)` (`java -jar …-shaded.jar` op
  die golden, 2026-09-05). De gelande zin noemt die reden letterlijk. De
  golden staat in poort 12 bij de gegroepeerde auto-weigeringen en nooit
  in de gespeelde verzameling (poort 12: played 108).
- **(b) pariteitbijzin.** Gemeten met dezelfde probe: de 30
  pariteitdocumenten betreden de `ParameterRef`-arm **18 keer over 9
  documenten**, en geen van die referenties noemt een keuzetype (de
  `pk`-mutant zwijgt er). Dat stond nergens; nu wel.
- **(c) bereikbaarheid van de DimensieCel-nul.** Het is een BEREIKTE nul,
  geen onbezochte arm: de celarm wordt betreden en vergelijkt bij precies
  de twee golden-cellezingen — **2 vergelijkingen over 176 documenten**,
  tegenover **46 vergelijkingen over 31 documenten** op de
  `ParameterRef`-arm. De mutant kreeg daar dus zijn twee kansen.
- **(d) kolomgroepering.** In de getuigenkolom stond
  `jutterswet-meldplichtspoor` met een `+` áchter de somreferentiegroep,
  wat haar in die groep trekt. Nu een komma tussen de groepen en `+`
  alleen binnen een groep, met de eigen omschrijving ("the `ParameterRef`
  arm … standing as a `Fold`'s list operand — not the fold-source arm").
- **(e)** "of all 257 negatiefs" → **297**, de stand van vandaag (gemeten:
  `find corpus/negatief -name ir.json | wc -l` = 297).

### R3 — twee WF-091-punten in dezelfde diff

- **(a) negatiefkolom.** "`wf091-parameterref-eenheid` (the arm without a
  golden witness)" werd tegengesproken door de goldenkolom én door
  notitie 3 van diezelfde rij, die beide `jutterswet-aanvoergeld` noemen.
  Nu: "(the arm whose golden witness, `jutterswet-aanvoergeld`, arrived
  only on 2026-09-04 — see honesty note 3)".
- **(b) breedtecijfers van notitie 3.** Die stonden op de 174-stand.
  Hermeten met de notitie-eigen mutant (vuurt op elke eenheiddragende
  gebruiksplek op één arm), over de 176:

```
ARM=target : 17  (was 15; erbij: jutterswet-tempototaal, jutterswet-tempovouw)
ARM=attr   : 13  (ongewijzigd)
ARM=binder :  2  (ongewijzigd)
ARM=fold   :  3  (was 1; jutterswet-vondsttelling + tempototaal + tempovouw)
ARM=param  :  1  (jutterswet-aanvoergeld)
ARM=cel    :  1  (jutterswet-seizoenvondst)
```

  Alleen `target` en `fold` bewogen, en de rij zegt nu ook waaróm: niet de
  regel maar het corpus groeide — beide temporele goldens dragen zowel een
  eenheiddragend regeldoel als een eenheiddragende foldbron. Herdateerd op
  "re-measured 2026-09-05 over the 176".

### R4 — deze kaart

De Results-alinea en de bouwnotitie beweerden "geen tweede rij". Dat was
onwaar: de eigen grepterm `no golden` trof regel 3868 letterlijk. Beide
plekken zijn hierboven eerlijk herschreven, mét de reden van de miss (een
leesmiss in een regel van ruim 25 000 tekens), en met de vermelding dat de
tweede rij in dezelfde diff is meegegaan.

### R5 — twee verruimingen van *may touch*, met reden

Beide zijn gemeld en beide zijn nodig omdat de nieuw gepinde vorm — een
lijstparameter van `SumType`-elementen — elders als *ongedragen* wordt
beschreven, wat de getuige van deze kaart weerlegt.

- **`spec/ASP-TRANSLATION.md` (~S19, r. 2211)** — géén beschermd pad, dus
  een gewone wijziging; hier gemeld omdat de kaart hem oorspronkelijk niet
  in *may touch* had. De zin "the ASP case emission scales every parameter
  through `decimals_of_datatype`" is als **mechanisme** onwaar: de
  parameterlus neemt haar `ListType`-tak eerst (S15,
  `element_scale_of`/`reify_list`), die een `SumType`-element als
  grondconstructorterm op schaal 0 draagt, en pas de doorval schaalt
  scalair. Nu "every *scalar* parameter" — hetzelfde woord dat de
  structzusterzin twintig regels verderop al gebruikte — plus één zin over
  de voorafgaande lijsttak met `jutterswet-meldplichtspoor` als getuige
  (gemeten: 8 van 8 casussen `clingo: PASS`).
- **`spec/RUNTIME-SEMANTICS.md` (~r. 3754)** — **wél een beschermd pad**,
  dus deze wijziging is onderdeel van hetzelfde voorstel als de
  IR-SPEC-diff; de maintainer merget. De parenthese "(the
  sum-typed-parameter sibling, ASP-TRANSLATION S19)" suggereerde dat de
  somkant even ongedragen is als de structkant. De structhelft blíjft waar
  — gemeten over elke corpuscasus die een parameter aanlevert: de
  aangeleverde vormen zijn scalair `Numeriek` (65), lijst-van-`Numeriek`
  (19) en lijst-van-`SumType` (8), en er is **geen enkele** struct-vorm,
  scalair noch lijst. De somkant niet: de zusterschap geldt alleen voor de
  **scalaire** somparameter; de lijstvorm ervan wordt door een golden
  gedragen.

### De dertien poorten (na de wijziging, alle tellingen gelijk aan main)

| # | uitslag |
|---|---|
| 1 | `check-self-contained: PASS` |
| 2 | `PASS (schema valid; 176 file(s) validate and round-trip)` |
| 3 | `PASS (176 golden clean; 297 negatief rejected for their declared rule)` |
| 4 | `PASS (schema valid; 427 case file(s) validate)` |
| 5 | `PASS (405 cases replayed exactly; …)` |
| 6 | `PASS (405 cases replay exactly through clingo — ref = clingo over the corpus)` |
| 7 | `PASS (369 cases refute a do-nothing executor; 36 pinned survivors)` |
| 8 | `PASS (22 red cases fault observably in ref and clingo)`; Drools-been `PASS (5 fault-pinned, 17 not-carried with reason)` |
| 9 | `PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically` |
| 10 | `PASS — JUnit suite green (171 tests); 30 rgs golden(s) re-emit byte-identically` |
| 11 | `PASS` — ref 29/29, clingo 29/29, Drools 29 gespeeld |
| 12 | `PASS` — played 108, value-divergence 1 (met reden), no-case 2, JUnit 199 |
| 13 | `PASS (64 unit annotations twisted over 13 goldens; 58 executor replays reproduce the untouched outcome, 11 PLAYED by the DRL-text branch; 0 pinned survivors)` |

De kaart raakt geen uitvoerbaar artefact, dus geen enkele telling beweegt.

### De dertien poorten, opnieuw na de fixronde (2026-09-05)

Alle dertien opnieuw gedraaid ná de R1–R5-wijzigingen; alle tellingen
gelijk aan main.

| # | uitslag |
|---|---|
| 1 | `check-self-contained: PASS` |
| 2 | `PASS (schema valid; 176 file(s) validate and round-trip)` |
| 3 | `PASS (176 golden clean; 297 negatief rejected for their declared rule)` |
| 4 | `PASS (schema valid; 427 case file(s) validate)` |
| 5 | `PASS (405 cases replayed exactly; …)` |
| 6 | `PASS (405 cases replay exactly through clingo — ref = clingo over the corpus)` |
| 7 | `PASS (369 cases refute a do-nothing executor; 36 pinned survivors)` |
| 8 | `PASS (22 red cases fault observably in ref and clingo)`; Drools-been `PASS (5 fault-pinned, 17 not-carried with reason)` |
| 9 | `PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically` |
| 10 | `PASS — JUnit suite green (171 tests); 30 rgs golden(s) re-emit byte-identically` |
| 11 | `PASS` — ref 29/29, clingo 29/29, Drools 29 gespeeld |
| 12 | `PASS` — played 108, auto-excluded 43, value-divergence 1 (met reden), no-case 2, JUnit 199 |
| 13 | `PASS (64 unit annotations twisted over 13 goldens; 58 executor replays reproduce the untouched outcome, 11 PLAYED by de DRL-tekstarm; 0 pinned survivors)` |

`git diff main -- spec/verifier/lib/checks.ml` is leeg: geen mutant heeft
de werkkopie ooit geraakt, laat staan een commit.
