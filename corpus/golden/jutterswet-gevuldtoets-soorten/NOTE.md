# jutterswet-gevuldtoets-soorten

The leeg and gevuld tests (`spec/RUNTIME-SEMANTICS.md` ch. 1.2) on attributes that are no number and no date. A
Vondst declares

    de categorie Categorie;            (an enumeration: schelp, hout, metaal)
    de omschrijving Tekst;
    de aangifte Boolean;
    de taxatie Numeriek (getal met 2 decimalen) (of onbekend of niet van toepassing);
    de herkeuring Categorie (of onbekend);

and every attribute carries two rules, one that makes a characteristic hold where it is gevuld and one where it is
leeg. A fifth pair reads the naam (Tekst) of the Jutter of the Vondst, over the to-one role of the fact type vangst.
The test asks after presence, not after the value: onwaar is a value, so the aangifte onwaar is gevuld; and an absence
marker is a value of the sum type the taxatie and the herkeuring declare (ch. 9.5), so a taxatie onbekend or niet van
toepassing and a herkeuring onbekend are gevuld as well. The herkeuring carries an enumeration as the payload of its
constructor `waarde`; a third rule reads that payload ("zijn herkeuring heeft een waarde" and "zijn herkeuring is gelijk
aan 'hout'", IR-SPEC WF-127) and makes houten hold.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), one scenario each:

| case | input | gevuld holds | leeg holds |
|---|---|---|---|
| 1 | V1: categorie schelp, omschrijving "roestig anker", aangifte onwaar, taxatie 120,00, herkeuring hout, jutter J1 with naam "Aaltje" | ingedeeld, beschreven, verantwoord, getaxeerd, herleidbaar, herkeurd (and houten) | none |
| 2 | V1 with nothing, its jutter J1 without naam; V2 with nothing and no jutter | none | onbepaald, onbeschreven, onverantwoord, ongetaxeerd, onherleidbaar, onherkeurd (V2: the absent link reads leeg) |
| 3 | V1: categorie hout, aangifte waar, taxatie onbekend, herkeuring onbekend; V2: omschrijving "drijvend wrakhout", taxatie niet van toepassing, herkeuring metaal | V1 ingedeeld, verantwoord, getaxeerd, herkeurd; V2 beschreven, getaxeerd, herkeurd (neither houten) | V1 onbeschreven; V2 onbepaald, onverantwoord |

ref, clingo and both branches of the Drools backend play the three cases.
