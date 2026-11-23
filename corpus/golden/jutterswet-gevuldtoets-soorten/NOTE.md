# jutterswet-gevuldtoets-soorten

The leeg and gevuld tests (`spec/RUNTIME-SEMANTICS.md` ch. 1.2) on attributes that are no number and no date. A
Vondst declares

    de categorie Categorie;            (an enumeration: schelp, hout, metaal)
    de omschrijving Tekst;
    de aangifte Boolean;

and every attribute carries two rules, one that makes a characteristic hold where it is gevuld and one where it is
leeg. A fourth pair reads the naam (Tekst) of the Jutter of the Vondst, over the to-one role of the fact type vangst.
The test asks after presence, not after the value: onwaar is a value, so the aangifte onwaar is gevuld.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), one scenario each:

| case | input | gevuld holds | leeg holds |
|---|---|---|---|
| 1 | V1: categorie schelp, omschrijving "roestig anker", aangifte onwaar, jutter J1 with naam "Aaltje" | ingedeeld, beschreven, verantwoord, herleidbaar | none |
| 2 | V1 with nothing, its jutter J1 without naam; V2 with nothing and no jutter | none | onbepaald, onbeschreven, onverantwoord, onherleidbaar (V2: the absent link reads leeg) |
| 3 | V1: categorie hout, aangifte waar; V2: omschrijving "drijvend wrakhout" | V1 ingedeeld, verantwoord; V2 beschreven | V1 onbeschreven; V2 onbepaald, onverantwoord |

ref, clingo and both branches of the Drools backend play the three cases.
