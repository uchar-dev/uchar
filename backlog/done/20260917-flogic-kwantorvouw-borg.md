# flogic-kwantorvouw-borg: een tweede brontaal voor de vouwknoop (optie)

Aanleiding: de Kwantorvouw-lijn (ALLE/GEEN/TEN MINSTE/TEN HOOGSTE/
PRECIES n, filter, heelperiode- en niet-temporele matrix) heeft drie
onafhankelijke UITVOERDERS maar één brontaal: alleen de
RegelSpraak-frontend produceert de knoop; Catala en F-logic dragen hem
niet en de niet-temporele getuigen zijn handgeschreven IR. De
onafhankelijkheidsborg die F-logic voor de subtype-kernel leverde (twee
talen, één betekenis — zie `frontends/flogic/README.md`, "Why F-logic")
ontbreekt voor deze knoop. Deze kaart is een OPTIE, geen noodzaak: de
uitvoerdersborg is de zwaardere van de twee en staat.

De natuurlijke ingang, gemeten aan de slice:

- F-logic-aggregaten zijn de native spelling voor tellingen:
  `?C = count{...}, ?C >= 2`. Alle vijf kwantoren zijn via tellingen
  uitdrukbaar ZONDER negatievorm — GEEN = `count(voldoeners) = 0`,
  ALLE = `count(voldoeners) = count(leden)` — dezelfde
  één-telregel-lezing waarmee de tellende-kwantorenkaart de vijf
  kwantoren dekte. De `\naf`/`\neg`-weigering van de slice hoeft dus
  niet te bewegen.
- De echte slice-uitbreiding is de GEFILTERDE comprehension: de slice
  draagt in `count{?V | ?V = ?X.role}` nu één kale rolhop; de matrix
  (en de die/dat-filter) vergen een conditie in de comprehension-body.

De kernbeslissing van de kaart, expliciet te nemen: lowert de frontend
zulke tellingen naar `Kwantorvouw` (dan borgt hij de VORM — het doel
van deze kaart, analoog aan RegelSpraaks WF-096-promotie), of blijven
zij `Functie AANTAL` + `Vergelijking` (semantisch gelijk, maar dan
borgt de kaart niets nieuws en kan hij beter niet gebouwd worden)?
Buiten bereik, bewust: de heelperiodematrix — de slice heeft geen
tijdlijnen, en die erbij trekken is groter dan de borg waard is.

## Acceptatie (bij oppakken aanscherpen)

- (a) De gefilterde comprehension in de slice, Flora-2-2.2-conform
  (elke vorm parseert in de reasoner), met de lowering naar
  `Kwantorvouw` voor de herkende telvormen en luide weigeringen voor
  de rest.
- (b) Een golden mét `bron.flogic` naast een bestaande
  RegelSpraak-vouwgolden, over één gedeelde set case-verwachtingen —
  het wrakrecht-patroon.
- (c) Alle veertien gates groen; gate 14-vloeren bewegen mee met
  gedateerde reden.

## Ontwerp (papier, vóór de bouw)

De kernbeslissing is genomen zoals de kaart hem stelde: de frontend
LOWERT de herkende telvormen naar `Kwantorvouw`. Het alternatief
(`Functie AANTAL` + `Vergelijking` laten staan) borgt niets nieuws en
maakt de kaart zinloos; de kaart staat in het programma, dus is hij
verworpen.

### De herkenningstabel

In een regellichaam is een doel van de vorm

```
count{?V | ?V = ?X.rol, G1, …, Gk} <op> <rechts>     (k ≥ 1)
```

een **tellende comprehension**. `?X` is de subjectvariabele van de
regel, `rol` één rolhop, `?V` het getelde lid. `G1 … G(k-1)` zijn de
**filter** (de die/dat-deelselectie), `Gk` is de **matrix** (de
gekwantificeerde toets). Beide worden gelezen met `?V` als onderwerp.
De positie is de conventie: F-logic heeft geen betrekkelijke bijzin om
de filter aan het bereik te hangen, dus draagt de LAATSTE conditie de
kwantificatie en is alles daarvóór bereikversmalling.

| bron (in een regellichaam) | lowering |
|---|---|
| `count{?V \| ?V = ?X.rol} <op> e` (k = 0) | ONGEWIJZIGD: `Vergelijking` over `Functie AANTAL` — zonder matrix is er geen vouw (WF-108 (d)) |
| `count{?V \| ?V = ?X.rol, …, M} >= n`, n ≥ 1 | `Kwantorvouw TEN_MINSTE`, `drempel` n |
| `… <= n`, n ≥ 1 | `Kwantorvouw TEN_HOOGSTE`, `drempel` n |
| `… = n`, n ≥ 1 | `Kwantorvouw PRECIES`, `drempel` n |
| `… = 0` | `Kwantorvouw GEEN`, géén `drempel` |
| `… = count{?V \| ?V = ?X.rol, …}` (dezelfde hop, dezelfde filter, matrix weggelaten) | `Kwantorvouw ALLE`, géén `drempel` |

De twee laatste rijen zijn de kaartregel `GEEN = count(voldoeners) = 0`
en `ALLE = count(voldoeners) = count(leden)`, waarbij "leden" precies de
bijdragers zijn die de filter overleven — RUNTIME-SEMANTICS ch. 3.9a
schrijft `ALLE = count = |contributors|`. Geen negatievorm; `\naf` en
`\neg` blijven geweigerd.

De kop `?X : kenmerk :- lichaam.` komt erbij: een vouw is een CONDITIE
en de bestaande slice kan alleen attribuutwaarden schrijven, dus zonder
kenmerkkop is er geen niet-temporele vouwgolden te spiegelen. Hij is de
symmetrische tegenhanger van de kenmerkcheck `?V : k` die de slice in
een lichaam al leest, en hij is Flora-2's eigen `isa`-kop. Een
kenmerkdoel lowert naar een `ResolvedRegel KENMERKTOEKENNING` — NIET
naar een defaultsgroep — omdat het prioriteitsapparaat twee definities
van één doel ordent en een kenmerk maar één waarde kan krijgen: er is
niets te ordenen. Een tweede definitie van hetzelfde kenmerkdoel
weigert, zoals een tweede onvoorwaardelijke attribuutdefinitie weigert.
*(Die motivering is in de fixronde gecorrigeerd: op de draad KAN een
kenmerk overbepaald worden. Zie het Result, "De herkenningstabel zoals
gebouwd", voor de smallere en juiste reden.)*

### Wat weigert

Elke weigering noemt reden én mandaatdocument, zoals elke bestaande.

1. `>` / `<` tegen een tellende comprehension — de drempel op de draad is
   het getal dat de bron schreef; `>= n+1` / `<= n-1` is de directe
   spelling. `!=` / `\=` krijgt een EIGEN arm: daar bestaat geen directe
   spelling om naar te herschrijven, en dat zegt het bericht.
2. `<= 0` — WF-041: een teldrempel is minstens 1; `= 0` is de
   GEEN-spelling. `>= 0` kreeg in de fixronde een eigen arm: dat is geen
   drempelkwestie maar een tautologie (een telling is nooit negatief).
3. een tellende comprehension vergeleken met iets anders dan een geheel
   niet-negatief literal of de matrixloze tweeling (een pad, een som,
   een breuk, een rekenuitdrukking).
4. de ALLE-tweeling die niet klopt — andere rolhop of andere filter;
   het bericht zet beide spellingen naast elkaar.
5. de telling RECHTS van de vergelijking (`2 <= count{…}`).
6. een gefilterde comprehension in een WAARDEpositie (regelkop,
   `?V = …`-binding, rekenoperand): een geconditioneerde comprehension
   is een kwantificatie en heeft een drempel nodig om mee te
   vergelijken — de F-logic-tegenhanger van de WF-096-weigering die de
   RegelSpraak-frontend op een setlezing buiten een vouwbron geeft.
7. een gefilterde `sum{…}` — de som van de slice draagt geen
   deelselectie.
8. een comprehension die niet op één rolhop staat
   (`?V = ?X.rol.attribuut, M`), of waarvan de hop niet het eerste doel
   is.
9. een doel in de comprehension dat niet over het getelde lid `?V`
   gaat.
10. een kenmerkkop die geen gedeclareerd kenmerk van het subject noemt.
11. een tweede definitie van één kenmerkdoel.

### Flora-2-2.2-conformiteit

De gefilterde comprehension is Flora-2's eigen aggregaatvorm:
`count{?V [?Groep] | Doel}` waarbij `Doel` een met komma's gescheiden
conjunctie van literals is. De slice beperkt die conjunctie tot
"hop, filter…, matrix" en voegt niets aan de grammatica toe. De
kenmerkkop `?X : k :- Lichaam.` is een gewone Flora-2-`isa`-regel.
Beide vormen zijn dus subset-conform per constructie.

## Result

**De kernbeslissing is genomen en gebouwd: de frontend lowert de
herkende telvormen naar `Kwantorvouw`.** Daarmee heeft de vouwknoop voor
het eerst een BRONTAAL — tot nu toe had hij drie onafhankelijke
uitvoerders en nul talen, want de RegelSpraak-frontend weigert de
niet-temporele vorm (WF-096, `setlezing buiten een vouwbron`) en de
handgeschreven getuigen zijn IR. Vier bestanden in `frontends/flogic/lib`
bewogen, één goldenmap kwam erbij, en in `scripts/check-drools.sh`
bewoog uitsluitend de kanaaltelling.

### De herkenningstabel zoals gebouwd

Onveranderd ten opzichte van het ontwerp hierboven; één ding is er
tijdens de bouw bij gekomen dat het ontwerp niet had voorzien: **de
kenmerkkop**. Een `Kwantorvouw` is een CONDITIE, en de slice kon tot nu
toe alleen attribuutwaarden schrijven, dus was er niets waar de vouw de
voorwaarde VAN kon zijn — en geen bestaande niet-temporele vouwgolden om
te spiegelen (alle vier kennen kenmerken toe). `?X : k :- Lichaam.` is
Flora-2's eigen `isa`-kop en de symmetrische tegenhanger van de
kenmerkcheck `?V : k` die de slice in een lichaam al las.

Een kenmerkdoel lowert naar `ResolvedRegel KENMERKTOEKENNING` met de
conditie als `voorwaarde`, dus NIET naar een defaultsgroep. De reden is
NIET dat een kenmerk niet overbepaald kan worden — op de draad kan dat
wel: `PrioritizedDefaults` draagt een `kenmerk`-doel en `Default` draagt
`besluit`, dus toekennen en niet-toekennen zijn twee besluiten die een
stapel wél ordent (RUNTIME-SEMANTICS ch. 7). De juiste, smallere reden
is tweeledig, en staat zo in de README en in de NOTE van de golden:
**deze slice heeft geen niet-toekennen-vorm** (de negatievormen
weigeren), dus binnen F-logic kunnen twee definities van één kenmerk
niet botsen; en het is **de vorm die de RegelSpraak-frontend dezelfde
zin geeft** — een voorwaardelijke kenmerktoekenning lowert daar ook naar
een `ResolvedRegel` en niet naar een groep, dus zou een groep hier twee
frontends laten twisten over de vorm van één betekenis. Een tweede
definitie van één kenmerkdoel weigert.

De dertien weigeringen staan er alle dertien, elk met een eigen fixture
die reden én mandaatcitaat controleert (elf uit het ontwerp, plus de
vreemde hopvariabele en de ongelijkheid, beide uit de fixronde).

### Flora-2-2.2-conformiteit: BEARGUMENTEERD, NIET GEMETEN

Er is op deze machine geen werkende Flora-2/ErgoLite-installatie meer:
waar de frontend-kaart van 2026-09-12 op 2.2 beta rev. 1203 over XSB 5.0
mat, resteert per 2026-09-19 een lege mappenstructuur en een verweesde
XSB-cache. **De nieuwe vormen zijn dus niet in de reasoner gedraaid**, en
de README zegt dat nu op beide plekken waar hij de conformiteit claimde,
met een genummerde MEETLIJST van precies drie vormen (de conjunctieve
comprehension-body, de `isa`-kop, en `<=`) zodat een volgende sessie mét
reasoner weet wat er af te vinken valt. Wat wel staat:

- `count{?V | ?V = ?X.rol, G, …}` gebruikt Flora-2's eigen
  aggregaatlichaam — een met komma's gescheiden conjunctie van
  literals — en de hopspelling `?V = ?X.rol` is de vorm die de slice al
  droeg en die de vorige kaart wél mat. De uitbreiding voegt geen
  grammatica toe, ze beperkt: de eerste conjunct moet de hop zijn.
- `?X : k :- Lichaam.` is een `isa`-kop, Flora-2's manier om
  klasselidmaatschap af te leiden.
- `?V : k` en `?V.attribuut > 5` binnen het aggregaatlichaam zijn
  gewone Flora-2-literals.

De open twijfel staat onderaan.

### Rood eerst

De elf fixtures en het vijfkwantorenprogramma zijn vóór de lowering
geschreven en gemeten (commit `3bf1169d`): **115 checks, 12 failures**.
Elke nieuwe vorm stierf op een naamloze syntaxfout — `expected "[",
found :` voor de kenmerkkop, `expected "}", found ,` voor de gefilterde
comprehension. Dat is precies wat een handgeschreven parser hoort te
vermijden, en het is de nulmeting waartegen de weigeringen nu bij naam
spreken. Ná de bouw: **124 checks, 0 failures**; ná de fixronde **130
checks, 0 failures** (vloer 81, de meting van 2026-09-12, die exact de
stand vóór deze kaart was).

### Fixronde na de lens

Zes punten, alle zes geland op deze branch.

**F1, blokkerend — het hop-variabelegat.** `count{?V | ?V = ?Q.rol, M}
>= n` met een vreemde of ongebonden `?Q` werd op het VOUWpad stil
geaccepteerd en als het regelonderwerp gelezen, terwijl het identieke
construct op het WAARDEpad al luid weigerde. Oorzaak: `as_quantification`
matchte `E_path (_, [r])` en gooide de variabele weg, en de
variabelecontrole liep over `q_matrix :: q_filter` maar niet over de hop.
De comprehension draagt haar subjectvariabele nu mee en `low_kwantorvouw`
weigert met exact de boodschap van `low_agg`, vóór `find_hop`. Fixture
`refuse_telling_vreemde_hop.flogic`. Gemeten vóór de fix: geen enkele
uitvoer, exitcode 0, een geëmitteerd document; erna:

```
the aggregate path ?Q.boten does not start at the rule's subject ?S (line 15)
```

**F2 — de ongelijkheid krijgt een eigen arm.** `!=` / `\=` viel door naar
de generieke "vergeleken met"-weigering, die een drempeladvies gaf dat
voor een ongelijkheid niet bestaat. Nu:

```
the inequality '!=' against a counting comprehension: the five quantifiers of
the fold are the comparisons >=, <=, = and the matrix-free twin, and a count
DIFFERENT from a number is none of them — there is no direct spelling to
rewrite this into, so the sentence has to be stated as a quantification the
node carries (line 14)
```

Fixture `refuse_telling_ongelijk.flogic`.

**F3 — het advies is doodlopend-vrij.** `< 1` wees naar `<= 0` en `< 0`
naar `<= -1`, en beide weigeren zelf. Nu wijst `< 1` naar `= 0` (de
GEEN-spelling), krijgt `< 0` een eigen weigering (een telling is nooit
negatief, dus de vergelijking geldt van geen enkel subject), en is de
drempel-0-arm gesplitst: `>= 0` is geen drempelkwestie maar een
tautologie en zegt dat, `<= 0` blijft naar `= 0` wijzen.

**F4 — een weigering citeert de bron.** De ALLE-tweelingweigering
renderde beide kanten met de genormaliseerde `?V` van de draad, ook als
de auteur `?L` schreef. De vergelijking blijft op de genormaliseerde vorm
(dat is alfa-equivalentie en dat is de juiste toets), de BOODSCHAP citeert
nu de letters van de bron.

**F5 — de motivering van de kenmerkkop-uitzondering.** Hierboven
gecorrigeerd, in README en NOTE.

**Positieregel-waarschuwing.** De README-vouwsectie zegt nu dat een
verwisselde filter en matrix geen weigering maar een ándere zin zijn, met
de eigen meting erbij (niet overgenomen van de lens): op een case met
`count(F) ≠ count(M)` — B1 lengte 10 en zeewaardig, B2 lengte 10 en niet
zeewaardig — klapt ALLE om in ref én clingo, terwijl TEN MINSTE 1 en GEEN
op dezelfde case invariant blijven. Dat is ook precies wat de rekenkunde
voorspelt: de tellende kwantoren en GEEN lezen `|{m : F(m) ∧ M(m)}|`, wat
symmetrisch is in de twee, en ALLE niet.

### De golden, door vier uitvoerders

`corpus/golden/jutterswet-vuurtoren-flogic` — de tweede brontweeling van
de handgeschreven `jutterswet-vuurtoren`, over dezelfde vijf
casebestanden **byte voor byte**. Niets herpind.

| uitvoerder | uitkomst |
|---|---|
| verifier | `PASS` |
| `ref` | 5/5 cases exact, 35 verwachtingen (6 per case, case 4 draagt er 11 — twee torens) |
| clingo | 5/5 cases exact |
| Drools exec-model | `PLAYED 5` |
| Drools DRL-tekst (`--drl`) | `PLAYED 5` |

89 verschilregels tegen de tweeling, in drie categorieën en geen vierde:
herkomst (`bron` + `onderwerpBron`, 68 regels, waarvan 8 het impliciete
`waar` van een kenmerktoekenning dat F-logic niet spelt en dus `null`
laat), gedeclareerde oppervlaktenamen waar F-logic geen grammatica voor
heeft (18: `meervoud` ×6, de rolnaam ×2, `bronRol.rol` ×10), en de lege
`prioritizedDefaults`-array die deze serialiser altijd schrijft (3).
Geen semantisch verschil — de volledige verantwoording staat in de
NOTE.md van de golden.

### Sweep vóór/ná, beide takken

Corpusbreed gemeten met de gate-12-jar, de nieuwe golden eerst buiten de
boom en daarna erin:

```
exec-model:  199 -> 200 goldens,  played 150 -> 151
DRL-tekst:   199 -> 200 goldens,  played 123 -> 124
diff (beide takken, identiek):
  160a161
  > jutterswet-vuurtoren-flogic :: PLAYED 5
```

Precies één rapportregel beweegt, in elke tak; geen enkele andere golden
wisselt van kanaal of van reden.

### Getuigen en mutatieproeven

Getuigen: het programma `frontends/flogic/test/kwantorvouw.flogic` draagt
alle vijf de kwantoren in één document, met vier cases — de lege
bijdragersverzameling (het vacuümgeval: ALLE én GEEN waar), een gemengde
verzameling, en twee cellen die alleen door de filter worden beslist.
`paraat` zet een vouw náást een gewone conditie van het subject, zodat de
`Samengesteld`-met-vouw-vorm een getuige heeft in ref én clingo.

Twee mutaties, elk gedraaid en teruggedraaid:

| mutatie | uitkomst |
|---|---|
| kwantorherkenning verwisseld (`= 0` → ALLE, `= tweeling` → GEEN) | **130 checks, 4 failures** — `slagvaardig` (case twee) en `dubbelgekeurd` (case filterbeslist) vallen om, in ref én clingo |
| de filterconditie genegeerd (`kv_filter = None`) | **130 checks, 4 failures** — `beproefd` (case gemengd) en `dubbelgekeurd` (case filterbeslist) vallen om; en de golden zelf valt om: case 4 meldt `T1 carries characteristic "keurig", expected absent` |

De tweede mutatie is ook de reden dat de golden de filter écht pint en
niet toevallig meeloopt.

### De veertien gates

Alle veertien groen, exitcode 0:

```
check-self-contained        exit 0
check-ir-schema             exit 0   226 file(s) validate and round-trip, floor 192
check-verifier              exit 0   226 golden clean; 395 negatief rejected, floor 350
check-case-schema           exit 0   542 case file(s) validate
check-oracle                exit 0   ref suite 643, floor 489; 516 cases replayed, floor 427
check-clingo                exit 0   backend suite 300, floor 83; 516 cases replay exactly
check-noop                  exit 0   480 cases refute a do-nothing executor; 36 survivors
check-rood                  exit 0   26 red cases, floor 26
check-catala                exit 0   201 checks, floor 201; 12 goldens byte-identical
check-regelspraak           exit 0   283 tests, floor 173; 54 rgs goldens byte-identical
check-toka-parity           exit 0   29 toka cases through ref, clingo, Drools
check-drools                exit 0   played 151; channel floor 49 pinned; JUnit 437, floor 239
check-reftype-derivation    exit 0   108 annotations twisted over 22 goldens, floor 74
check-flogic                exit 0   130 checks, floor 81; 2 goldens byte-identical, floor 1
```

Gate 14 groeit boven zijn vloeren zonder scriptwijziging: 81 → 130 checks
en 1 → 2 goldens. De vloeren zijn ondergrenzen, dus groei hoeft niet
gebumpt; het verhogen is een bewuste daad bij gelegenheid en deze kaart
raakt `scripts/check-flogic.sh` niet aan. Gate 13 ziet de nieuwe golden
niet: hij draagt geen enkele eenheid, dus staat hij buiten de
verdraaiverzameling.

### De scriptdiff, verantwoord

`scripts/check-drools.sh` is beschermd; deze kaart draagt het mandaat
voor uitsluitend de kanaaltelling. Tien regels erbij, één eraf:

```
-EXPECTED_PLAYED=150
+# 2026-09-19 (card flogic-kwantorvouw-borg), 150 -> 151: `jutterswet-vuurtoren-flogic` joins as a NEW
+# PLAYER — the counting quantifiers over a NON-temporal matrix, emitted from a `bron.flogic` instead of
+# hand-written. Nothing in this backend moved for it: the node, the filter and the threshold are the ones
+# `jutterswet-vuurtoren` already plays, and the twin shares that golden's five case files byte for byte,
+# so a played pass here says the F-logic frontend reached exactly the wire the hand-written document
+# states. What it buys is the direction the executors cannot cover: until now the fold had THREE
+# independent executors and no source language at all, the RegelSpraak frontend declining the
+# non-temporal form. Measured before and after over ALL goldens in BOTH branches: exec 150 -> 151,
+# DRL-text 123 -> 124, and exactly this one report line moves — no other golden changes channel or reason.
+EXPECTED_PLAYED=151
```

Niets anders in dat script bewoog: geen kanaalregel, geen
divergentieregel, geen vloer. Dat de golden speelt is gemeten vóór de
bump, niet eromheen geredeneerd.

### May touch — geverifieerd

Gewijzigd t.o.v. `main` (245f7152), 35 bestanden, en niets anders:
`frontends/flogic/{README.md,lib/{syntax,parse,lower,emit}.ml,test/**}`,
de nieuwe map `corpus/golden/jutterswet-vuurtoren-flogic/`,
`scripts/check-drools.sh` (alleen `EXPECTED_PLAYED`), en de
kaartverplaatsing. Geen andere corpusbestanden, geen `spec/`, geen
`backends/`, geen `ref/`.

### Registervoorstellen (NIET geland — voor de hoofdsessie)

**`spec/CONSTRUCTS.md` rij 40 (Quantifier fold) — aanvulling, achteraan
de laatste kolom:**

> **Landed (2026-09-19, kaart flogic-kwantorvouw-borg) — de knoop krijgt
> zijn eerste BRONTAAL.** Tot nu toe had deze rij drie onafhankelijke
> uitvoerders en nul talen: de RegelSpraak-frontend weigert de
> niet-temporele matrix (WF-096, `setlezing buiten een vouwbron`), Catala
> draagt de knoop niet, en beide niet-temporele getuigen waren
> handgeschreven IR. De F-logic-frontend (`frontends/flogic`) lowert nu
> de GEFILTERDE comprehension naar deze knoop: `count{?V | ?V = ?X.rol,
> F…, M}` vergeleken met een drempel, waarbij de eerste conjunct de hop
> is, de LAATSTE de matrix en alles ertussen de die/dat-deelselectie —
> Flora-2 heeft geen betrekkelijke bijzin om een filter aan een bereik te
> hangen, dus draagt de positie het onderscheid. Alle vijf kwantoren
> volgen uit één vergelijking van een telling met een getal: `>= n`,
> `<= n`, `= n` zijn de drie tellende, `= 0` is GEEN en `= count{…}`
> zonder de laatste conjunct is ALLE — exact `ALLE = count =
> |contributors|` van ch. 3.9a. Daarom heeft deze brontaal GEEN
> negatievorm nodig en bleven de `\naf`/`\neg`-weigeringen van de slice
> onaangeroerd. Getuige `corpus/golden/jutterswet-vuurtoren-flogic`, de
> brontweeling van de handgeschreven `jutterswet-vuurtoren` over
> dezelfde vijf casebestanden byte voor byte, gespeeld door alle vier de
> uitvoerders (gate 12 `EXPECTED_PLAYED` 150 → 151, beide takken).
> Buiten bereik en bewust: de heelperiodematrix — de slice draagt geen
> tijdlijnen.

**`spec/CONSTRUCTS.md` rij 39 (Object-type inheritance) — aanvulling,
achteraan de laatste kolom:**

> De F-logic-frontend die deze rij als getuige aandroeg is sinds
> 2026-09-19 niet langer alleen de subtypeborg: dezelfde slice draagt nu
> ook de kwantorvouw van rij 40 (de gefilterde comprehension en de
> `isa`-kop `?X : k :- …`, die een kenmerkdoel als staande norm schrijft
> en niet als defaultsstapel, omdat een kenmerk één waarde heeft en er
> dus niets te ordenen valt). De borgredenering is dezelfde en de
> frontend is niet langer een eenconstructsgetuige.

**Optioneel, ter beoordeling:** `spec/RUNTIME-SEMANTICS.md` ch. 3.12's
statusrij voor de vouw noemt de getuigen bij naam; daar zou
`corpus/golden/jutterswet-vuurtoren-flogic` bij kunnen als de tweede
brontaal. Dat is een beschermd pad en deze kaart draagt er geen mandaat
voor.

### Open twijfel

1. **`<=` in Flora-2 2.2, ongemeten.** De slice declareerde de
   vergelijkingen `> >= < <= = !=` al vóór deze kaart, maar geen enkele
   bestaande fixture gebruikte `<=`; de regels `zuinig` (golden) en
   `karig` (suite) zijn de eerste gebruikers. XSB spelt de niet-strikte
   kleiner-vergelijking `=<`, en of Flora-2 daarnaast `<=` accepteert heb
   ik niet kunnen meten. Als het niet zo is, is de reparatie klein en
   mechanisch: `=<` in de punctuatietabel van de lexer, in de
   operatorregel van de README en in de drie bronnen die het gebruiken.
   Géén synoniem toegevoegd — dat zou net zo goed een gok zijn, en de
   slice-regel is "gebruik Flora-2's spelling", niet "accepteer er twee".
   TEN HOOGSTE heeft in de slice geen andere spelling, dus de VORM staat
   niet ter discussie, alleen het lexeem. Het punt staat nu als nummer 3
   op de meetlijst in de README.
2. **De positieregel is een conventie, geen ontdekking.** "De laatste
   conditie is de matrix" is verdedigbaar en staat overal opgeschreven,
   maar een auteur die zijn filter achteraan zet krijgt een geldig
   document met een ándere betekenis in plaats van een weigering. Gemeten
   op een case met `count(F) ≠ count(M)`: onder ALLE klapt het oordeel om
   in ref én clingo; onder de drie tellende kwantoren en GEEN is de
   verwisseling invariant, want die lezen `|{m : F(m) ∧ M(m)}|` en dat is
   symmetrisch. De fout is dus stil op vier van de vijf posities. Alleen
   als filter en matrix van type verschillen (kenmerkcheck tegen
   vergelijking) zou een heuristiek dat kunnen vangen, en zo'n heuristiek
   zou op zijn beurt legitieme programma's weigeren. De conventie is
   gekozen, gedocumenteerd en nu ook in de README gewaarschuwd; een
   expliciete bronmarkering (iets als een aparte `filter{…}`-haak) zou
   het dichttimmeren maar zou de slice uit Flora-2's grammatica trekken.
3. **De gefilterde `Functie AANTAL` blijft ongebouwd.** Een gefilterde
   comprehension in een WAARDEpositie weigert nu luid; ze zou lowerbaar
   zijn naar `Functie AANTAL` met `filter`, wat een bestaand wireveld
   is. Dat is een aparte kaart waard, geen stiekeme uitbreiding hier.
