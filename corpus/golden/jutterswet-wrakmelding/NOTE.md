# jutterswet-wrakmelding — a subtype instance in a role its supertype fills

`Wrakvondst` is a `Vondst`, and `Scheepswrak` is a `Wrakvondst`. The relation
`vondst heeft melding` declares its role `vondst` with the filler `Vondst`, and
`vondst heeft rapport` its role `gerapporteerde vondst` with the same filler.
A role's declared filler is an upper bound (`IR-SPEC.md` ch. 2.6): the role
takes an instance of every subtype of it, one level down and two. Every case of
this golden puts such an instance in one of the two roles and reads the link
from both ends, with rules on the subtype and on the supertype.

## The rules

    De gemelde waarde van een Melding moet gesteld worden op de waarde van de
    vondst van de Melding.

reads over the to-one role from the side that stores the link (`Melding`), on
whatever instance fills it. The next six rules read the to-many side, from the
subtype `Wrakvondst` unless stated otherwise:

    Het aantal meldingen van een Wrakvondst moet berekend worden als het aantal
    meldingen van de Wrakvondst.

    Het meldingental van een Vondst moet berekend worden als het aantal
    meldingen van de Vondst.                                  (the supertype)

    De meldsom van een Wrakvondst moet berekend worden als de som van de ernst
    van alle meldingen van de Wrakvondst.

    Een Wrakvondst is veelgemeld indien de ernst van ten minste 2 van de
    meldingen van de Wrakvondst groter is dan 3.

    Een Wrakvondst is gemeld indien hij een vondst is.

    Het meldgeld van een Wrakvondst wordt verdeeld over de meldaandelen van
    alle meldingen van de Wrakvondst, waarbij wordt verdeeld:
    - in gelijke delen,
    - afgerond op 0 decimalen naar beneden.
    Als onverdeelde rest blijft de meldrest van de Wrakvondst over.

a count, a count on the supertype, a sum, a quantifier, a role check whose link
is stored at the other end, and an allocation whose source is the subtype. The
last three rules read the one-to-one relation from both ends:

    De rapportwaarde van een Rapport moet gesteld worden op de waarde van de
    gerapporteerde vondst van het Rapport.

    Het wrakcijfer van een Wrakvondst moet gesteld worden op het cijfer van het
    rapport van de Wrakvondst.

    Het rapportcijfer van een Vondst moet gesteld worden op het cijfer van het
    rapport van de Vondst.                                    (the supertype)

`scenario.rgs` carries every expectation; the frontend's `--cases` mode lowers
its three scenarios to the three case files, and gate 16 regenerates them from
it byte for byte.

## Hand computation (absolute)

`case1-een-wrakvondst-in-de-rol-van-de-vondst.json`. V is a `Vondst` with
waarde 2; W and X are `Wrakvondst`en with waarde 5 and 7 and meldgeld 10 and 4.
M1, M2 and M3 name W as their vondst, with ernst 4, 5 and 1; M4 names V, with
ernst 6. X has no melding.

| reading | W | X | V | why |
|---|---|---|---|---|
| gemelde waarde of its meldingen | **5** (M1, M2, M3) | — | **2** (M4) | the waarde of the instance in the role |
| aantal meldingen | **3** | **0** | — | the rule is on `Wrakvondst`; V is none |
| meldingental | **3** | **0** | **1** | the rule is on `Vondst` and covers the subtype |
| meldsom | 4 + 5 + 1 = **10** | **leeg** | — | a sum over the empty set without a fallback |
| veelgemeld | **yes** | **no** | — | ernst above 3: M1 and M2 for W, two of them; none for X |
| gemeld | **yes** | **no** | — | W plays the role `vondst` in three links, X in none |
| meldaandeel of its meldingen | **3** each | — | **leeg** (M4) | 10 in three equal parts is 3,33, rounded down to 3; M4 belongs to V, which is no `Wrakvondst` |
| meldrest | 10 − 9 = **1** | **leeg** | — | X has no recipient, so nothing is distributed and the rest stays leeg |

`case2-een-scheepswrak-twee-niveaus-onder-de-vuller.json`. S is a
`Scheepswrak` (a `Wrakvondst`, so a `Vondst`) with waarde 9 and meldgeld 7; N1
and N2 name S as their vondst, with ernst 2 and 8. The gemelde waarde of N1 and
N2 is **9**; the aantal meldingen and the meldingental of S are **2** each (both
rules cover an instance two levels below their subject); the meldsom is
2 + 8 = **10**; S is **not** veelgemeld (only N2 has an ernst above 3, one of
the two required); S is **gemeld**; 7 in two equal parts is 3,5, so each
meldaandeel is **3** and the meldrest 7 − 6 = **1**.

`case3-een-rapport-van-beide-kanten.json`. Each one-to-one link is written
from one end only, and each is read from both (`RUNTIME-SEMANTICS.md` ch. 5.7a:
a supplied link establishes the relation in both directions):

| link, written as | rapportwaarde | wrakcijfer | rapportcijfer |
|---|---|---|---|
| R (cijfer 8): gerapporteerde vondst is W (a `Wrakvondst`, waarde 6) | R: **6** | W: **8** | W: **8** |
| V (a `Vondst`, waarde 3): rapport is Q (cijfer 4) | Q: **3** | — | V: **4** |
| S (a `Scheepswrak`, waarde 1): rapport is P (cijfer 2) | P: **1** | S: **2** | S: **2** |

The inputs carry no value any rule writes, so a no-op run fails all three
cases.

## Before

On the Drools backend as it stood at `396097de` both branches refused the
document at the first rule that reads the link from the subtype:

    EXCLUDE transpiler SKIP: regel: aantal meldingen van een wrakvondst — regel
    (de aggregatie-join heeft geen beanveld — het beanmodel leidt voor de rol
    'vondst' op Melding geen referentieveld naar Wrakvondst af (§13.3.9
    opslagrichting)): aantal meldingen van een wrakvondst

and, with the four rules and the allocation that join the link on the subtype
removed, the case input refused the link itself:

    EXCLUDE role 'vondst' resolves to the to-one role 'vondst' on Melding, whose
    field a same-named role toward another type already holds — the bean cannot
    carry the reference field (name-sharing bean limit: attribute > characteristic
    > reference)

a cause the document does not have: no other role is named `vondst`. The bean
field `Melding.vondst` is of the class `Vondst`, which the class `Wrakvondst`
extends; the backend asked for the field's type by equality.

## The square

`ref` and the ASP backend replay the three cases (`oracle: PASS (21
expectations)`, `(10 expectations)`, `(8 expectations)`, and the same three
lines from clingo), and both Drools branches play them (`PLAYED 3`, exec-model
and DRL text).
