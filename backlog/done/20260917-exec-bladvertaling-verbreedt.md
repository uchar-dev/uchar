# exec-bladvertaling-verbreedt: Samengesteld-matrix en parameter-grens in de exec-tak

Aanleiding: de exec-model-tak van `backends/drools` weigert twee
Kwantorvouw-matrixvormen die de DRL-tekst-tak WEL draagt en correct
speelt (gemeten door de asymmetrie-lens, uitgeschreven in rij 40 als
takongelijkheid "in BOTH directions"):

1. een SAMENGESTELDE matrix (`Samengesteld`, ook diep genest) — de
   per-subject-bladvertaling `leafPredicate` draagt de knoop niet;
2. een matrixvergelijking waarvan de grens een PARAMETER is — zelfde
   bladvertaling, en de weigeringstekst noemt sinds de asymmetrie-kaart
   de juiste as (de operandsoort).

Maintainersbesluit 2026-09-17: dit takverschil is ongewenst — de
exec-model-tak compileert de services en moet dus alles dragen wat de
DRL-tekst-tak (het snelle ontwikkel-/testkanaal) al aankan. De
DRL-tekst-uitkomsten zijn per probe gelijk aan ref en clingo gemeten,
dus de verwachtingen liggen vast.

Buiten deze kaart, bewust: de GENESTE VOUW als matrix — die weigeren
beide takken bij naam (diedat-kaart) en hij draagt een eigen
spec-vraag.

## Acceptatie (bij oppakken aanscherpen)

- (a) `leafPredicate` (of een verbrede opvolger) draagt `Samengesteld`
  recursief (de kwantor/drempel-samenstelling van §13.4.14 over de
  bladvormen die de tak al kent) en de parameter-operand als grens;
  wat overblijft weigert bij naam met de juiste as.
- (b) Corpus-goldens die beide vormen drieweg spelen (de DRL-tekst-tak
  speelde ze al — nu vierbeens incl. exec), handberekend; gate 12-pin
  met gedateerde stappen.
- (c) Rood-eerst: de huidige exec-weigeringsteksten gemeten vóór de
  bouw.
- (d) Rij 40's takongelijkheid-passage versmalt — registervoorstel in
  het Result.
- (e) Alle veertien gates groen.

## Result

**De twee matrixvormen spelen nu VIERBEENS.** Wat ze tegenhield was in
beide gevallen hetzelfde: de exec-tak vroeg ch. 3.9a's `toets` aan
`leafPredicate` — een conditie-naar-`Predicate1`-vertaling die per
definitie geen samenstelling kent en geen patroonvariabele kan binden.
Die aanvraag loopt nu door één recursieve `matrixVertaling`.

**De lowering** (`transpiler/model/ExecModelTranspiler.java`). Drie
vormen, in deze volgorde:

- een `Samengesteld` — §13.4.14's samenstelling, RECURSIEF en op elke
  diepte: elk niveau vouwt zijn bullets met zijn EIGEN kwantor en
  drempel, gelezen met ch. 5.10's stoptriggers (`vastgesteld`) en
  beslist door de gedeelde `telVerdict`. De kwantor van de
  samenstelling en die van de VOUW worden dus elk op hun eigen niveau
  beslist en kunnen niet in elkaar klappen;
- een §5.6-vergelijking tegen een numerieke parameter, aan WELKE KANT
  van de vergelijking dan ook — de grens komt binnen als
  patroonvariabele via `globalBullet`, dezelfde helper waarmee
  `filterSource` al een source-global bindt, en een parameter LINKS
  wordt omgekeerd zoals `leafPredicate` een literal links al omkeerde;
- elke andere vorm — de feitelijke `leafPredicate`, verbreed tot
  dezelfde ariteit door de grens te negeren.

De vouw-eval rijdt NA de `collectList`-accumulate en bindt de
verzamelde lijst plus hooguit één patroonvariabele. Daar past één
grens in; een matrix die er twee leest weigert bij naam. Dat is ook de
reden dat de FILTER-positie zijn eigen vouw (`leafCompound`) houdt in
plaats van deze aan te roepen: daar zijn de bullets constraints op het
SOURCE-patroon, waar de conjunctieve arm elke bullet zijn eigen
`.expr` kan geven. `DrlTranspiler` is niet aangeraakt — die tak is de
referentie van deze kaart en blijft byte-gelijk (gepind met de sweep).

### Rood-eerst: de gemeten weigeringen, letterlijk

Alle zeven gemeten op de goldens zoals zij in commit `d15fc0e2`
landden, vóór enige codewijziging. Op datzelfde moment: verifier PASS,
`ref` PASS en clingo PASS op alle tien de cases, en de DRL-tekst-tak
`PLAYED 5` op allebei de goldens.

`jutterswet-redderkeur` (per regel gemeten op een document met alleen
die regel, omdat anders de eerste afketser het rapport vult):

```
EXCLUDE transpiler SKIP: regel: paraat bij ploegkeur — kwantorvouw ALLE over Redder (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Samengesteld): alle redders van de Reddingspost aan alle volgende voorwaarden voldoen; regel (voorwaarde buiten Phase-B scope): paraat bij ploegkeur
EXCLUDE transpiler SKIP: regel: geoefend bij ploegkeur — kwantorvouw ALLE over Redder (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Samengesteld): alle redders van de Reddingspost aan ten minste 1 van de volgende voorwaarden voldoen; regel (voorwaarde buiten Phase-B scope): geoefend bij ploegkeur
EXCLUDE transpiler SKIP: regel: stil bij ploegkeur — kwantorvouw GEEN over Redder (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Samengesteld): geen van de redders van de Reddingspost aan alle volgende voorwaarden voldoet; regel (voorwaarde buiten Phase-B scope): stil bij ploegkeur
EXCLUDE transpiler SKIP: regel: wisselend bij ploegkeur — kwantorvouw ALLE over Redder (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Samengesteld): alle redders van de Reddingspost aan ten hoogste 1 van de volgende voorwaarden voldoen; regel (voorwaarde buiten Phase-B scope): wisselend bij ploegkeur
```

`jutterswet-loodsgrens`:

```
EXCLUDE transpiler SKIP: regel: bekwaam bij keurgrens — kwantorvouw ALLE over Loods (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een vergelijking van het eigen attribuut 'ervaring' met de parameter 'keurgrens'): de ervaring van alle loodsen van de Loodspost groter is dan de keurgrens; regel (voorwaarde buiten Phase-B scope): bekwaam bij keurgrens
EXCLUDE transpiler SKIP: regel: onbekwaam bij keurgrens — kwantorvouw GEEN over Loods (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een vergelijking van het eigen attribuut 'ervaring' met de parameter 'keurgrens'): de ervaring van geen van de loodsen van de Loodspost groter is dan de keurgrens; regel (voorwaarde buiten Phase-B scope): onbekwaam bij keurgrens
EXCLUDE transpiler SKIP: regel: merendeels bij keurgrens — kwantorvouw TEN_MINSTE over Loods (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een vergelijking van het eigen attribuut 'ervaring' met de parameter 'keurgrens'): de ervaring van ten minste 2 van de loodsen van de Loodspost groter is dan de keurgrens; regel (voorwaarde buiten Phase-B scope): merendeels bij keurgrens
```

De grens LINKS geschreven (`de keurgrens kleiner is dan zijn
ervaring`, nu de regel `gangbaar`) gaf zijn eigen as:

```
EXCLUDE transpiler SKIP: regel: bekwaam bij keurgrens — kwantorvouw ALLE over Loods (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een vergelijking van de parameter 'keurgrens' met het eigen attribuut 'ervaring'): …
```

### De twee goldens, en wat de vier uitvoerders ervan maakten

Beide handgeschreven IR zonder `bron.rgs`, om dezelfde gemeten reden
als hun drie voorgangers (`RuleChecker.kwantorvouw` lowert alleen de
§8.2-GEKWALIFICEERDE conditie; de niet-temporele vouw uit bron is een
eigen kaart, die parallel loopt). Poort 10 ziet ze niet.

`corpus/golden/jutterswet-redderkeur` — vier regels met een
`Samengesteld` matrix: een ALLE-samenstelling onder een ALLE-vouw
(`paraat`) én onder een GEEN-vouw (`stil`), een
TEN HOOGSTE 1-samenstelling (`wisselend`), en een
TEN MINSTE 1-samenstelling waarvan de TWEEDE bullet zelf een
ALLE-samenstelling is (`geoefend`). Vijf cases, zes handberekende
verdictrijen. Cel 2 is de cel die de NESTING beslist: geen van beide
redders haalt de buitenste TEN MINSTE op de eerste bullet alleen — de
een wordt gedragen door de geneste samenstelling, de ander door de
bullet ernaast. Cel 5 is het lege lid: een ordeningsvergelijking over
leeg is onwaar, dus de telling van `wisselend` komt op 1 in plaats van
2 en de cel klapt juist daardoor om.

`corpus/golden/jutterswet-loodsgrens` — vier regels tegen één
parameter: `bekwaam` (ALLE), `onbekwaam` (GEEN), `merendeels`
(TEN MINSTE 2) en `gangbaar`, dat dezelfde vergelijking is met de
grens aan de ANDERE kant. Vijf cases, zes handberekende verdictrijen.
Cases 1–3 dragen dezelfde drie loodsen onder keurgrens 4, 6 en 9: een
tak die de grens negeert antwoordt ze alle drie hetzelfde, en zij zijn
het op geen kolom eens. Cel 4 is het vacuüm waarop de drie kwantoren
uit elkaar lopen; cel 5 pint het bereik (twee posten die tegengesteld
lezen) en het lege lid.

Uitkomsten, alle vier de uitvoerders, alle tien de cases:

```
verifier  : PASS           PASS
ref       : 5/5 PASS       5/5 PASS      (6+6+6+11+6 resp. 6+6+6+6+11 verwachtingen)
clingo    : 5/5 PASS       5/5 PASS      (dezelfde aantallen)
Drools exec-model : PLAYED 5   PLAYED 5
Drools DRL-tekst  : PLAYED 5   PLAYED 5
```

### De sweep: wat er bewoog, en wat niet

Corpusbreed, beide takken, vóór en ná de codewijziging, 202 goldens ×
2 takken = 404 statusregels. De diff is TWEE regels:

```
< jutterswet-loodsgrens|exec|EXCLUDE transpiler SKIP: … 'ervaring' met de parameter 'keurgrens' …
> jutterswet-loodsgrens|exec|PLAYED 5
< jutterswet-redderkeur|exec|EXCLUDE transpiler SKIP: … een conditie van soort Samengesteld …
> jutterswet-redderkeur|exec|PLAYED 5
```

Tellingen: exec 151 → 153, DRL-tekst 126 → 126 met een BYTE-GELIJK
rapport over het hele corpus — de pin dat `DrlTranspiler` niet
aangeraakt is. Ten opzichte van `main` (zonder deze twee goldens) is
de DRL-tekst-beweging 124 → 126: de twee goldens speelden daar vanaf
hun eerste commit.

### Getuigen en mutaties

`backends/drools/.../cases/VouwMatrixVormWitnessTest.java`, acht rijen,
beide takken (poort 12 speelt alleen de exec-tak; poort 13 raakt deze
documenten niet, want zij dragen geen eenheidsdragende annotatie):
beide goldens PLAYED in beide takken met elke cel gespeeld, drie
mutanten, de vier-lagen-matrix, en de twee restvormen.

De vier-lagen-matrix is
`ALLE[ TEN MINSTE 1[ ALLE[ GEEN[ dj>9, c>20 ], c>7 ], gediplomeerd ], dj>2 ]`
over drie posten, elk beslist op een ander niveau. Gemeten vóór hij
gepind werd: verifier PASS, `ref` PASS (4 verwachtingen), clingo PASS
(4 verwachtingen), beide Drools-takken PLAYED — dus de twee
Java-takken worden aan het antwoord van de twee OCaml-uitvoerders
gehouden en niet alleen aan elkaar.

De mutaties zijn FEITELIJK gedraaid: bron gewijzigd, herbouwd,
gemeten, teruggedraaid.

| mutatie in `matrixVertaling` | redderkeur | loodsgrens | JUnit |
|---|---|---|---|
| samenstellingskwantor verwisseld (ALLE→GEEN, ten minste↔ten hoogste) | FAIL (paraat, stil, wisselend, geoefend) | PLAYED 5 | 2 rijen rood |
| samenstellingsdrempel +1 | FAIL (geoefend, cases 1 en 2) | PLAYED 5 | 3 rijen rood |
| parametergrens genegeerd (null i.p.v. de gebonden global) | PLAYED 5 | FAIL (bekwaam, onbekwaam, merendeels, gangbaar) | 1 rij rood |
| geneste samenstelling geweigerd (terug naar één niveau) | EXCLUDE | PLAYED 5 | 4 rijen rood |

Elke mutatie is rood op precies de golden die haar pint en groen op de
andere — de scheiding tussen de twee vormen is echt, geen toeval.

### Wat er OVERBLIJFT, gemeten met eigen probes

| vorm | verifier | ref | clingo | DRL-tekst | exec |
|---|---|---|---|---|---|
| matrix leest TWEE verschillende parameters | PASS | PASS | PASS | PLAYED | EXCLUDE bij naam |
| NIET-numerieke (Tekst) parametergrens | PASS | PASS | TRANSLATION FAULT | PLAYED | EXCLUDE bij naam |

De eerste is een structurele grens van DEZE lowering en niet van de
IR: de eval na de accumulate bindt de verzamelde lijst plus hooguit
één patroonvariabele. De DRL-tekst-tak heeft dat probleem niet, want
daar is een global een vrije naam in de constraint-tekst. De tweede
draagt de ASP-backend evenmin, op zijn eigen as, dus daarvoor valt in
geen enkele richting een corpusgetuige te maken. Beide staan als rij
in de getuigentest, met de weigeringstekst erin.

Onaangeroerd, en dus ongewijzigd in de rij: de RANGE-helft van de
takongelijkheid (de `bronRol`-terugval en de
§13.3.9-opslagrichtingssplitsing die het bereik van de DRL-tekst-tak
niet heeft), en de GENESTE VOUW als matrix, die beide takken bij naam
blijven weigeren.

### De scriptwijziging (beschermd pad, gemandateerd)

`scripts/check-drools.sh`, één constante en de gedateerde reden
ernaast; niets anders in het script:

```
-EXPECTED_PLAYED=151
+# 2026-09-19 (card exec-bladvertaling-verbreedt), 151 -> 153: … (13 regels reden)
+EXPECTED_PLAYED=153
```

De kanaallijst blijft 49 en is niet aangeraakt: die pint alleen
NIET-spelers, en de twee nieuwe goldens spelen.

### De veertien poorten (eindstand, alle exitcode 0)

```
1  check-self-contained: PASS                                                    rc=0
2  check-ir-schema: PASS (schema valid; 24 pattern self-test rows; 228 files validate and round-trip, floor 192)   rc=0
3  check-verifier: PASS (228 golden clean; 395 negatief rejected for their declared rule, floor 350)               rc=0
4  check-case-schema: PASS (schema valid; 552 case files validate)               rc=0
5  check-oracle: PASS (ref suite 643 worked examples, floor 489; 526 cases replayed exactly, floor 427)            rc=0
6  check-clingo: PASS (backend suite 362 checks, floor 83; 526 cases replay exactly through clingo)                rc=0
7  check-noop: PASS (490 cases refute a do-nothing executor; 36 pinned survivors)                                  rc=0
8  check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26)  rc=0
9  check-catala: PASS (frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically)        rc=0
10 check-regelspraak: PASS (JUnit 283 tests, floor 173; 54 rgs goldens re-emit byte-identically, floor 38)         rc=0
11 check-toka-parity: PASS (45 .rgs re-emitted byte-identical, floor 45; 29 cases through ref, clingo, Drools; 0 divergences)  rc=0
12 check-drools: PASS (played 153 = pinned 153; channel floor 49; value-divergence 1; JUnit 455 tests, floor 239)  rc=0
13 check-reftype-derivation: PASS (108 annotations twisted over 22 goldens, floor 74; 100 replays, 21 DRL-text PLAYED, floor 13)  rc=0
14 check-flogic: PASS (frontend suite 130 checks, floor 81; 2 flogic goldens re-emit byte-identically, floor 1)    rc=0
```

Bewegingen: poort 2 en 3 van 226 naar 228 documenten, poort 4 van 542
naar 552 casebestanden, poort 5 en 6 van 516 naar 526 cases, poort 7
van 480 naar 490, poort 12's JUnit van 447 naar 455 tests en
`EXPECTED_PLAYED` van 151 naar 153. Geen enkele vloer verlaagd.

### Acceptatie

- (a) gehaald — `matrixVertaling` draagt `Samengesteld` recursief op
  elke diepte en de parameter-grens aan beide kanten; wat overblijft
  weigert bij naam met de as erin (de twee tabelrijen hierboven).
- (b) gehaald — twee goldens, handberekend vóór enige uitvoerder
  draaide, vierbeens gespeeld; gate-12-pin met gedateerde stap.
- (c) gehaald — zeven weigeringsteksten letterlijk hierboven, gemeten
  op de commit vóór de bouw.
- (d) registervoorstel hieronder.
- (e) alle veertien groen, exitcodes hierboven.

### Geraakte bestanden (`git diff --name-only main...HEAD`)

    backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java
    backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/VouwMatrixVormWitnessTest.java
    corpus/golden/jutterswet-redderkeur/**  (nieuw: ir.json, NOTE.md, case1-5)
    corpus/golden/jutterswet-loodsgrens/**  (nieuw: ir.json, NOTE.md, case1-5)
    scripts/check-drools.sh                 (uitsluitend de kanaaltelling)
    backlog/new -> backlog/done (deze kaart)

De oorspronkelijke kaart draagt geen May-touch-sectie; de beschermde
scriptwijziging rust op acceptatiepunt (b) ("gate 12-pin met gedateerde
stappen") en is minimaal en gedateerd gehouden.

### Eerlijke afwijking

**De grens LINKS van de vergelijking is meegenomen, buiten de letter
van de kaart om.** De kaart noemt "een matrixvergelijking waarvan de
grens een PARAMETER is"; de asymmetrie-lens mat die met de parameter
RECHTS. Gemeten bleek dezelfde zin met de operanden verwisseld een
eigen weigering te geven (`… van de parameter 'keurgrens' met het
eigen attribuut 'ervaring'`) terwijl `ref`, clingo én de DRL-tekst-tak
hem gewoon antwoorden. Dat als rest laten staan zou de rij dwingen tot
"behalve als de grens links staat" — een residu dat niets zegt over de
vorm. Het is dezelfde omkering die `leafPredicate` al toepast op een
literal links, drie regels, en hij heeft een corpuscel (`gangbaar`),
wiens kolom in elke cel gelijk is aan die van `bekwaam` — en dát is de
bewering.

### Registervoorstel (letterlijk, voor de hoofdsessie)

Rij 40, aan te vullen achter de bestaande tekst. De aanvulling
VERSMALT de takongelijkheid-passage van de kwantor-asymmetrie-kaart —
"the exec branch declines a COMPOUND (`Samengesteld`) matrix and one
whose bound is a PARAMETER, which the DRL-text branch both carry" —
die per vandaag niet meer waar is:

**Landed (2026-09-19, kaart exec-bladvertaling-verbreedt) — the two Drools branches carry the SAME matrix FORMS now, and this NARROWS the branch-inequality passage above:** where that passage says the exec branch declines a COMPOUND (`Samengesteld`) matrix and one whose bound is a PARAMETER, which the DRL-text branch both carry, the exec branch carries BOTH — the composite at ANY DEPTH, and the bound written on EITHER SIDE of the comparison. ch. 3.9a's `test` is an ordinary condition with the element as subject and §13.4.14's composite IS one, so the branch asks it through a single recursive lowering in which every level folds its own bullets with its OWN quantifier and threshold over the leaf translation that was already there — read with ch. 5.10's stop triggers and settled by the same count-and-compare the fold itself uses, which is why the composite's quantifier and the fold's are decided at their own levels and cannot collapse into one another. A §5.6 numeric Parameter binds as the ONE pattern variable the post-accumulate eval has room for, through the very helper the die/dat filter already binds a source-global with; a bound written on the LEFT is swapped and its operator reversed, the normalisation a literal-LHS comparison already got. Witnesses `corpus/golden/jutterswet-redderkeur` (hand-written IR, five cases carrying six hand-computed verdict rows: an ALLE composite under an ALLE fold and under a GEEN fold, a TEN HOOGSTE composite, and a TEN MINSTE composite whose SECOND bullet is itself a composite — and the cell that decides is the one where neither member clears the outer threshold on the first bullet alone, one carried by the nested composite and the other by the bullet beside it) and `corpus/golden/jutterswet-loodsgrens` (hand-written IR, five cases carrying six hand-computed verdict rows: the three fold quantifiers over ONE matrix whose bound is a parameter, with cases 1–3 sharing one instance set under keurgrens 4, 6 and 9 — a branch that ignores the bound answers all three alike and they agree on no column — plus a fourth rule writing that same bound on the LEFT, whose column is therefore the first rule's in every cell, which IS the assertion that operand order is not a form of its own). Both play in `ref`, in clingo and in BOTH Drools branches; gate 12's `EXPECTED_PLAYED` 151 → 153 (DRL-text 124 → 126 as the goldens arrive), with that branch's whole corpus report BYTE-IDENTICAL before and after the code change — it is this card's reference and was not touched. **What is LEFT of the matrix half, measured per form rather than claimed:** the exec branch still declines a matrix reading TWO DIFFERENT Parameters — the eval that asks a non-temporal matrix rides after the `collectList` accumulate and binds the collected list plus at most ONE further pattern variable, a limit of that lowering and not of the IR, since the document is verifier-clean and `ref`, clingo and the DRL-text branch all answer it — and a NON-NUMERIC Parameter bound, which the DRL-text branch and `ref` carry while the ASP backend refuses it on its own axis, so that one form has no corpus witness to be had in any direction. Both are pinned as rows of the backend's own dual-branch suite, with the refusal text in them. The RANGE half of the passage stands unchanged (the `bronRol` fallback and the §13.3.9 storage-direction split the DRL-text branch's range does not have), and a NESTED FOLD as matrix is still declined by both, by name
