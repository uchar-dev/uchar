# jutterswet-vroegste-vondst — the earliest and the latest date over a role, and folds as operands

A Jutter holds vondsten, and a Strandvoogd beheert jutters. The rules of the
Jutter fold over the vondsten of the Jutter, the rule of the Strandvoogd over
the vondsten of its beheerde jutters, two roles deep. A Vondst is zwaar when
its gewicht is above 5, by a rule declared after every fold that reads it.

| rule | value | form |
|---|---|---|
| eerste vinddag van een jutter | de eerste van de vinddag van alle vondsten van de Jutter | the earliest date over a to-many role |
| laatste vinddag van een jutter | de laatste van de vinddag van alle vondsten van de Jutter | the latest date |
| eerste zware vinddag | de eerste van de vinddag van alle vondsten van de Jutter die zwaar zijn | the same fold with a condition on the elements, derived later |
| meldgrens | (de eerste van de vinddag …) plus 30 dagen | a date fold as operand, plus a duration |
| zoekgrens | zijn startdag plus de maximale waarde van de zoekduur van alle vondsten van de Jutter | a number fold (in dagen) as the duration of an own date |
| vondstspanne | de tijdsduur van de eerste van … tot de laatste van … in hele dagen | two date folds as the operands of a duration |
| weeggrens | (de minimale waarde van het gewicht …) plus 1 | a number fold as operand, over an empty source too |
| heffing | 10% van de som van het gewicht … | a percentage of a sum |
| eerste vinddag van een strandvoogd | de eerste van de vinddag van alle vondsten van alle beheerde jutters van de Strandvoogd | the earliest date two roles deep |

RUNTIME-SEMANTICS ch. 1.2e: eerste and laatste are ordering folds over the
contributed values, never a positional pick; an empty contributor is
skipped, and an empty or all-empty source folds to leeg, as the minimum and
the maximum do. The operators around a fold take its value as they take any
operand (ch. 1.2): a date plus a duration propagates an empty date and
coalesces an empty duration to 0, a tijdsduur propagates an empty operand,
plus and percentage van coalesce an empty operand to 0.

## Hand computation

`case1-vier-bronvormen.json` (42 expectations). J1 has V1 (vinddag
12-03-2026, gewicht 4, zoekduur 10 dagen), V2 (20-03-2026, 7, 3 dagen), V3
(no vinddag, 6, 20 dagen) and V4 (05-03-2026, 2, no zoekduur), in that order;
J2 has no vondst; J3 has V5, without vinddag and gewicht; J4 has V6
(31-12-2025, 9, 5 dagen). Startdag J1 and J2 01-03-2026, J4 01-01-2026, J3
none. S1 beheert J1 and J3, S2 J2, S3 J4, S4 no jutter.

- zwaar: V2 (7), V3 (6) and V6 (9); not V1, V4 and V5 (no gewicht).
- eerste and laatste vinddag: J1 over {12-03, 20-03, 05-03} (V3 is skipped)
  **05-03-2026** and **20-03-2026**; J2 no contributor and J3 only an empty
  one: **leeg** and **leeg**; J4 **31-12-2025** twice. J1's first member is
  neither the earliest nor the latest date and its last filled member is the
  earliest, so a positional reading fails both.
- eerste zware vinddag: J1 over {20-03, leeg} **20-03-2026**; J2 and J3
  **leeg** (V5 is not zwaar); J4 **31-12-2025**.
- meldgrens: 05-03-2026 + 30 = **04-04-2026**; J2, J3 **leeg**; 31-12-2025 +
  30 = **30-01-2026**.
- zoekgrens: J1 01-03-2026 + max{10, 3, 20} = **21-03-2026**; J2 01-03-2026
  plus the maximum over no contributor, an empty duration that counts as 0 =
  **01-03-2026**; J3 no startdag: **leeg**; J4 01-01-2026 + 5 =
  **06-01-2026**.
- vondstspanne: J1 05-03 to 20-03 = **15 dagen**; J2, J3 **leeg**; J4 **0
  dagen**.
- weeggrens: J1 min{4, 7, 6, 2} + 1 = **3**; J2 leeg + 1 = **1**; J3 (V5 has
  no gewicht) leeg + 1 = **1**; J4 9 + 1 = **10**.
- heffing: J1 10% of 19 = **1,90**; J2 and J3 10% of leeg = **0,00**; J4 10% of
  9 = **0,90**.
- eerste vinddag of the Strandvoogd: S1 over the vondsten of J1 and J3
  **05-03-2026**; S2 **leeg**; S3 **31-12-2025**; S4 **leeg**.

`case2-volgorde-en-gelijkspel.json` (22 expectations). J5 has V7 (30-06-2025,
8, 12 dagen), V8 (14-02-2025, 3), V9 (01-11-2025, 6, 40 dagen) and V10
(14-02-2025, 1), in that order; J6 has V11 (10-01-2025, 2, no zoekduur).
Startdag J5 01-01-2025, J6 none. S5 beheert J5 and J6.

- zwaar: V7 and V9; not V8, V10 and V11.
- eerste vinddag J5 **14-02-2025**, reached by V8 and V10 (a tie: the fold
  yields a value, so which member wins is not observable); laatste
  **01-11-2025**. The first member (30-06) is neither end and the last
  (14-02) is the earliest. J6 **10-01-2025** twice.
- eerste zware vinddag: J5 over {30-06, 01-11} **30-06-2025**; J6 **leeg**.
- meldgrens: 14-02-2025 + 30 = **16-03-2025**; 10-01-2025 + 30 =
  **09-02-2025**.
- zoekgrens: J5 01-01-2025 + max{12, 40} = **10-02-2025**; J6 no startdag:
  **leeg**.
- vondstspanne: J5 14-02 to 01-11-2025 = **260 dagen**; J6 **0 dagen**.
- weeggrens: J5 min{8, 3, 6, 1} + 1 = **2**; J6 2 + 1 = **3**.
- heffing: J5 10% of 18 = **1,80**; J6 10% of 2 = **0,20**.
- eerste vinddag of S5: over the vondsten of J5 and J6 **10-01-2025**, a
  member of the second jutter.

A no-op run fails both cases: it leaves every derived value leeg and every
characteristic absent (case 1: 26 mismatches, case 2: 17).

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (42
expectations)`, `(22 expectations)`; clingo the same). The exec-model branch
plays them, `PLAYED 2`; the DRL-text branch refuses the document by name
(`drl-transpiler MARKER: regel: eerste vinddag van een jutter — TODO
aggregatie EERSTE`).

Measured on the exec-model branch before the card
drools-exec-datumvouw-over-rol (2026-09-30):

    EXCLUDE transpiler SKIP: regel: eerste vinddag van een jutter — regel (waarde buiten scope): eerste vinddag van een jutter

and, on the same forms in separate documents, the zoekgrens refused as
`aggregatie-in-arithmetiek buiten scope`, the weeggrens answered leeg where
the source is empty (`verwacht 1, werkelijk leeg`) and the heffing answered
leeg for every Jutter. The branch now lowers a fold over a set source inside
a rule's value to an accumulate beside the subject pattern, and the value
reads its result as an operand.

Not in this golden, with its reason: the earliest date-and-time over a role
(the ASP backend does not fold a date-and-time over a set), a date fold in a
condition (the exec-model branch refuses it by name), and the minimum or
maximum over a date set (the RegelSpraak frontend types that spelling out;
`jutterswet-vondstbereik` carries it by hand, and the exec-model branch plays
it since the same day). The first two are rows of
`DateFoldOverRoleWitnessTest` in the Drools backend.
