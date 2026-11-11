# jutterswet-werfkeur — a quantifier fold over a to-many role that creations fill, stopping before a fault

The witness of the member order of a to-many role (`RUNTIME-SEMANTICS.md` ch. 5.7a): the links the case wrote, in
written order, then every other link — a reverse leg the loader materialised, a link a creation or a fact creation
laid — in instance order: the case's instances ascending by label, then the made objects by the index of their
creation and then by their subject. The quantifier fold reads its contributors in that order and stops at the first
one after which the outcome stands (ch. 3.9a, 5.10), so the order decides whether a fault is reached (ch. 5.11). The
red case `corpus/red/jutterswet-werfkeur-bereikt` is the same document with a case in which the order reaches the
fault.

```
De keur van een Werf moet gesteld worden op 1 indien ten minste drie schuiten van de Werf
aan ten minste een van de volgende voorwaarden voldoen:
  - zijn lengte is groter dan 5;
  - zijn keurdatum ligt na de rekendatum.
Het schuittal van een Werf moet berekend worden als het aantal schuiten van de Werf.

Objectcreatie 0, bouw een kotter: een Werf krijgt een Schuit met lengte 9 (ligplaats)
  indien het aantal werfvlaggen van de Werf groter is dan of gelijk aan 1.
Objectcreatie 1, bouw een sloep: een Werf krijgt een Schuit met lengte 2 (ligplaats).
Objectcreatie 2, hijs de werfvlag: een Werf krijgt een Werfvlag (vlagvoering)
  indien de Werf vlaggerechtigd is.
Feitcreatie, ligplaats van een bouwschuit: een Schuit krijgt als werf zijn bouwwerf (ligplaats).
```

No case carries an evaluation date, so a schuit that the fold reaches and whose lengte is 5 or less reads the
rekendatum and faults (EF-011, ch. 8.2): the matrix reads its parts in wire order and stops at the first that holds
(ch. 5.10), and `zijn keurdatum` is read before `de rekendatum` (ch. 5.11). `schuittal` states the range per case.

## The cells, computed by hand before any executor ran

| case | W1 | the members of W1's schuiten, in order (lengte) | per member | keur | schuittal |
|---|---|---|---|---|---|
| 1 | vlaggerechtigd; the case writes schuiten S2, S1 | S2 (9), S1 (7), kotter (9), sloep (2) | true, true, true — stop | **1** | **4** |
| 2 | not vlaggerechtigd; S1, S3, S4 name W1 as their werf, S2 as its bouwwerf | S1 (8), S2 (9), S3 (6), S4 (2), sloep (2) | true, true, true — stop | **1** | **5** |

Case 1: written first, in written order, so S2 before S1 although S1 sorts first. Then the made objects by the index
of their creation: the kotter (creation 0) before the sloep (creation 1). The kotter waits for the werfvlag that
creation 2 makes, so the session lays its link after the sloep's; the order does not follow that. Read in the order
the links were laid, the sloep would come third, its lengte 2 would send the matrix to the rekendatum, and the rule
would fault.

Case 2: the case writes no link on W1's side. The loader materialises S1, S3 and S4 (ascending label), the fact
creation links S2 during the evaluation, and the sloep follows. In instance order S2 stands between S1 and S3. Read in
the order the links were laid, S4 (lengte 2) would come third and the rule would fault.

## The reading (ch. 5.12)

| case | contributors of the fold |
|---|---|
| 1 | `"S1" true reached; "S2" true reached; «"bouw een kotter"»("W1")#1 true reached; «"bouw een sloep"»("W1")#1 fault EF-011 unreached` |
| 2 | `"S1" true reached; "S2" true reached; "S3" true reached; "S4" fault EF-011 unreached; «"bouw een sloep"»("W1")#1 fault EF-011 unreached` |

## Executors

`ref/` replays both cases, and its test `WV-5.7a-ledenvolgorde` pins the member lists above. The ASP backend refuses
the fold with CLINGO-035 and the Drools backend with DROOLS-259, on both branches: neither carries the member order of
a relation a creation writes (gate 6 and gate 12 list the golden in their refusal channel).
