# jutterswet-uniciteit-drie-bronnen — een uniciteit over de unie van drie bronnen

"De reekscode met het volgnummer van Strandvondst verenigd met Wrakvondst
verenigd met Drijfvondst moeten uniek zijn." De sleutel heeft twee delen,
reekscode en volgnummer, en de drie objecttypen vormen één
sleuteldomein (`RUNTIME-SEMANTICS.md` ch. 5.9, regel 1): een dubbele
sleutel kan over twee verschillende brontypen ontstaan, en de delen worden
op hun plaats vergeleken, dus de volgorde van de delen telt. Elk object
waarvan de sleutel vaker dan eens voorkomt, krijgt de fout onder de naam
van de uniciteit, uit welke bron het ook komt.

De andere goldens van een unie hebben twee bronnen. Dit document heeft er
drie, zodat de sleutels van een derde bron meedoen.

## De handberekening

| case | S1 (Strandvondst) | W1 (Wrakvondst) | D1 (Drijfvondst) | uitkomst |
|---|---|---|---|---|
| `case1-botsing` | (7, 1) | (1, 7) | (7, 1) | S1 en D1 **inconsistent**; W1 niet |
| `case2-zonder-botsing` | (7, 1) | (8, 2) | (1, 7) | **consistent** |

In case 1 botsen bron 0 en bron 2 op (7, 1). W1 heeft dezelfde getallen in
de omgekeerde volgorde en botst niet. In case 2 heeft D1 de omgekeerde
sleutel van S1; omdat de delen op hun plaats vergeleken worden, botst ook
dat niet.

Case 2 doorstaat de no-op-uitvoerder: "consistent" is waar van een leeg
kanaal. Wat de case toetst, is precies dat het kanaal leeg blijft: een
uitvoerder die de delen van een sleutel als verzameling vergelijkt, of de
bronnen door elkaar haalt, meldt hier een botsing. Gate 7 noemt de case
daarom in zijn lijst van overlevers, zoals de case `case-consistent` van
`jutterswet-vondstnummer`.

## Herkomst

Eigen werk (kaart `drl-teksttak-op-truth-maintenance`, fixronde 3b), met de
hand opgesteld als IR; er is geen bron in RegelSpraak. Gemeten 2026-10-14
bij de toelating: verifier PASS; ref en clingo spelen beide cases; beide
takken van de Drools-backend PLAYED 2. Met de verwachting "consistent" op
case 1 noemt ref precies S1 en D1, en telt clingo twee inconsistente
feiten.
