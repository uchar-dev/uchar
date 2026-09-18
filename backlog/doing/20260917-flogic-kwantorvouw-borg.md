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

### Wat weigert

Elke weigering noemt reden én mandaatdocument, zoals elke bestaande.

1. `>` / `<` / `!=` tegen een tellende comprehension — de drempel op de
   draad is het getal dat de bron schreef; `>= n+1` / `<= n-1` is de
   directe spelling.
2. `>= 0` / `<= 0` — WF-041: een teldrempel is minstens 1; `= 0` is de
   GEEN-spelling.
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
