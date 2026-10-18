# jutterswet-afrekening — folds in the toekenningen of an object creation

A Jutter holds vondsten. Two object creations mint objects for him: an
Afrekening, one per Jutter, whose four toekenningen each fold over his
vondsten, and a Zeebrief, of which a Jutter may hold many, whose one
toekenning is a date fold over the zware vondsten plus a duration, under a
condition that counts. Rules on the Jutter read the minted values back,
because a case names no minted instance. A Vondst is zwaar when its gewicht
is above 5, by a rule declared after both creations.

| creation | toekenning | form |
|---|---|---|
| maak een afrekening | heffing gelijk aan 10% van de som van het gewicht van alle vondsten van de Jutter | a percentage of a sum |
| | weeggrens gelijk aan (de minimale waarde van het gewicht van alle vondsten van de Jutter) plus 1 | a number fold as an operand, over an empty source too |
| | afrekendag gelijk aan de eerste van de vinddag van alle vondsten van de Jutter | the earliest date over a role |
| | telling gelijk aan het aantal vondsten van de Jutter | a count |
| geef een zeebrief | slotdag gelijk aan (de laatste van de vinddag van alle vondsten van de Jutter die zwaar zijn) plus 30 dagen, indien het aantal vondsten van de Jutter groter is dan 0 | the latest date over a subselection on a characteristic derived later, plus a duration, under a count |

RUNTIME-SEMANTICS ch. 5.5: a creation evaluates its member initialisations
on its subject, mints one object per subject whose condition holds, and on
the one-to-one shape mints nothing where the case already links an instance
to the subject. Ch. 1.2 and 1.2e give the empty source of each fold and the
empty operand of each operator: a sum, a minimum and a first or last date
over no contributor are leeg, a count is 0; 10% of leeg is 0, leeg plus 1 is
1, and an empty date plus a duration stays leeg. The fold empty does not
stop the creation: its object is minted, with the value its toekenning
computes.

## Hand computation

`case1-vier-bronvormen.json` (24 expectations). J1 has V1 (vinddag
12-03-2026, gewicht 4), V2 (05-03-2026, 7) and V3 (no values), in that
order; J2 has no vondst; J3 has V4 (no values); J4 has V5 (20-03-2026, 9).
No Jutter holds an Afrekening or a Zeebrief in the input.

- zwaar: V2 (7) and V5 (9); not V1 (4), and not V3 and V4 (no gewicht).
- heffing: J1 10% of 4 + 7 = **1,10** (V3 is skipped); J2 no contributor and
  J3 only an empty one, 10% of leeg = **0,00** both; J4 10% of 9 = **0,90**.
- weeggrens: J1 min{4, 7} + 1 = **5**; J2 and J3 leeg + 1 = **1**; J4 9 + 1 =
  **10**.
- afrekendag: J1 the earliest of {12-03, 05-03} = **05-03-2026** (its first
  member is the later one); J2 and J3 **leeg**; J4 **20-03-2026**.
- telling: J1 **3** (V3 counts: a count is of instances), J2 **0**, J3 **1**,
  J4 **1**.
- zeebrief: J1 (3 vondsten), J3 (1) and J4 (1) get one, J2 (0) none. slotdag
  J1 05-03-2026 (V2, its one zware vondst) + 30 = **04-04-2026**; J3 has no
  zware vondst, leeg + 30 dagen = **leeg**; J4 20-03-2026 + 30 =
  **19-04-2026**. Read back as the latest slotdag of his zeebrieven: J2,
  without one, **leeg**.
- aantal brieven: **1**, **0**, **1** and **1**: J3's zeebrief exists with a
  leeg slotdag.

`case2-volgorde-gelijkspel-en-een-ingevoerde-afrekening.json` (12
expectations). J5 has V6 (01-04-2026, 6), V7 (01-04-2026, 6) and V8
(15-03-2026, 12), in that order; J6 has V9 (10-03-2026, 3), and the case
links J6 to the Afrekening A1 (heffing 9,99, weeggrens 99, afrekendag
01-01-2026, telling 42).

- zwaar: V6, V7 and V8; not V9.
- J5: heffing 10% of 24 = **2,40**; weeggrens min{6, 6, 12} + 1 = **7**;
  afrekendag **15-03-2026**, the last member, while the first two tie;
  telling **3**; slotdag the latest zware vinddag, 01-04-2026 (a tie between
  V6 and V7: the fold yields a value, so which member wins is not
  observable), + 30 = **01-05-2026**; aantal brieven **1**.
- J6: A1 fills the afrekening role toward J6, so "maak een afrekening" mints
  nothing for J6 and the rules read A1: heffing **9,99**, weeggrens **99**,
  afrekendag **01-01-2026**, telling **42** (a mint would have read 0,30, 4,
  10-03-2026 and 1). The zeebrief is minted (1 vondst), over no zware vondst:
  slotdag **leeg**, aantal brieven **1**.

A no-op run fails both cases: it leaves every derived value leeg (case 1:
20 mismatches, case 2: 11).

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (24
expectations)`, `(12 expectations)`; clingo the same). The exec-model branch
of the Drools backend plays them, `PLAYED 2`; the DRL-text branch refuses
the document by name (`drl-transpiler MARKER: regel: slotdag — TODO
aggregatie LAATSTE: de laatste van de slotdag van alle zeebrieven van de
Jutter`). Until 2026-10-01 the exec-model branch refused it too
(`objectcreatie (toekenning-waarde buiten scope): maak een afrekening`), and
on the same forms in separate documents answered the heffing leeg for every
Jutter and minted no object over an empty minimum. A fold in a toekenning
now lowers as one in a rule's value: an accumulate beside the subject, whose
result the toekenning reads as an operand.

Not in this golden, with its reason: a toekenning in another unit than its
member (the ASP backend does not translate a restatement at an
initialisation), a subselection that compares a member (the ASP backend does
not ground it in a creation's toekenning), a division in a toekenning (the
exec-model branch refuses it by name, as a creation has no companion that
raises the Fout), a date fold in the creation's condition (refused by name),
and a fold over a value that a defaults group, a later creation or a fact
creation writes (both Drools branches fire the creation before that writer).
The first, third and fourth are rows of `ObjectCreationFoldWitnessTest` in the
Drools backend.
