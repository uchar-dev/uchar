# jutterswet-vergunningketen-eigenaarsrol — de eigenaarsrol draagt de naam van de zusterrol

Dezelfde regels als `jutterswet-vergunningketen`: een Vergunning kan een
vorige vergunning hebben, haar einddag wordt verlengd met het restant van de
vorige vergunning, en een Termijn is een satelliet van haar vergunning. Het
verschil zit in één naam. De eigenaarsrol van de satellietrelatie
`vergunningstermijn` heet `vergunning`, net als de rol van de schakelrelatie
`vorige vergunning` die een opvolger bereikt.

Een geval legt een relatie op rolnaam vast, en de teruglink van "V2 vorige
vergunning V1" staat op V1 onder de zusterrol `vergunning`. Een Termijn vindt
haar vergunning onder dezelfde naam. Het document is de getuige dat de diepte
van ch. 6.2 (`spec/RUNTIME-SEMANTICS.md`) alleen de links van de eigen
relatie telt. De eerste vergunning van een keten heeft geen voorganger en
staat op diepte 0; een termijn staat op de diepte van haar vergunning. Een
stap naar de eigenaar vanaf de eerste vergunning zou haar opvolger vinden,
en via diens voorganger weer haarzelf.

## De handberekening

De duur in hele dagen tussen twee datums is hun verschil in kalenderdagen;
een datum plus een aantal dagen is een kalenderstap (ch. 3.8). Een leeg
overschot telt in de som als 0 dagen (ch. 1.2). Het restant van een
vergunning zonder voorganger is leeg, dus haar einddag is de basiseinddag
(ch. 5.8).

`case1-keten-van-twee`: V1 (stopdag 2026-02-10, basiseinddag 2026-05-20,
termijn T1) en V2 (stopdag 2026-10-01, basiseinddag 2026-11-15, vorige
vergunning V1, termijn T2). Diepte: V1 en T1 0, V2 en T2 1.

- restant V1: 2026-02-10 tot 2026-05-20 is 19 (tot 1 maart) + 31 + 30 + 19
  = **99**.
- verlengde einddag V1: geen voorganger, dus 2026-05-20 + 0 = **2026-05-20**;
  overschot T1: 2026-02-10 tot 2026-05-20 = **99**.
- einddag V2: 2026-11-15 + 99 dagen: 30 (tot 15 december) + 31 (tot 15
  januari) + 31 (tot 15 februari) + 7 = **2027-02-22**.
- restant V2: 2026-10-01 tot 2027-02-22 is 31 + 30 + 31 + 31 + 21 = **144**.
- verlengde einddag V2: 2026-11-15 + het overschot van T1 (99) =
  **2027-02-22**; overschot T2: 2026-10-01 tot 2027-02-22 = **144**.

`case2-keten-van-drie`: oudste (stopdag 2026-01-05, basiseinddag 2026-04-01,
termijn "termijn oudste"), middelste (stopdag 2026-07-01, basiseinddag
2026-09-30, vorige vergunning oudste, geen termijn) en jongste (stopdag
2027-01-15, basiseinddag 2027-03-31, vorige vergunning middelste, termijn
"termijn jongste"). Diepte: oudste en haar termijn 0, middelste 1, jongste en
haar termijn 2.

- restant oudste: 2026-01-05 tot 2026-04-01 is 27 (tot 1 februari) + 28 + 31
  = **86**;
  einddag en verlengde einddag oudste **2026-04-01**; overschot "termijn
  oudste" **86**.
- einddag middelste: 2026-09-30 + 86 dagen: 31 (tot 31 oktober) + 30 (tot 30
  november) + 25 = **2026-12-25**; verlengde einddag middelste: 2026-09-30 +
  het overschot van "termijn oudste" (86) = **2026-12-25**.
- restant middelste: 2026-07-01 tot 2026-12-25 is 31 + 31 + 30 + 31 + 30 + 24
  = **177**.
- einddag jongste: 2027-03-31 + 177 dagen: 30 + 31 + 30 + 31 + 31 (tot 31
  augustus, 153) + 24 = **2027-09-24**.
- restant jongste: 2027-01-15 (dag 15) tot 2027-09-24 (dag 267) = **252**.
- verlengde einddag jongste: middelste heeft geen termijn, dus het overschot
  is leeg en telt als 0: **2027-03-31**; overschot "termijn jongste":
  2027-01-15 tot 2027-03-31 (dag 90) = **75**.

| case | einddag | restant | verlengde einddag | overschot |
|---|---|---|---|---|
| `case1-keten-van-twee` | V1 **2026-05-20**, V2 **2027-02-22** | V1 **99**, V2 **144** | V1 **2026-05-20**, V2 **2027-02-22** | T1 **99**, T2 **144** |
| `case2-keten-van-drie` | oudste **2026-04-01**, middelste **2026-12-25**, jongste **2027-09-24** | oudste **86**, middelste **177**, jongste **252** | oudste **2026-04-01**, middelste **2026-12-25**, jongste **2027-03-31** | termijn oudste **86**, termijn jongste **75** |

In case 2 verschillen einddag en verlengde einddag van jongste, omdat
middelste geen termijn heeft. De volgorde in het bestand (middelste, jongste,
oudste) en de alfabetische (jongste, middelste, oudste) zijn allebei niet de
volgorde van de keten. Elke case pint ook dat het kanaal leeg is.

## De bron

De bron staat in `source.rgs`; de frontend geeft dit `ir.json` byte-identiek
uit (gate 10).

## Herkomst

Eigen werk, naar de vorm van art. 42b WW (de duur van een nieuw recht wordt
verlengd met het restant van het eerdere recht), zoals
`jutterswet-vergunningketen`.
