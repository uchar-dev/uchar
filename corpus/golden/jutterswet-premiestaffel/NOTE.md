# jutterswet-premiestaffel — defaults whose justification counts or reads further than one role

A Jutter earns a premie from his vondsten: 10, 20 when he has more than two
vondsten, and 30 when the gewicht of his zware vondsten together exceeds
the parameter zwaartegrens; a Vondst is zwaar when its gewicht is above 5.
A Jutter is ijverig unless his lightest vondst weighs less than 2 or his
heaviest less than 5. A Melding of a Vondst is worth a gemelde premie of 10,
or 20 when the premie of the jutter of its vondst exceeds 15, and it is
dringend unless that premie exceeds 25. A Controle of a Melding is worth a
controlepremie of 5, or 15 when the premie of the jutter of the vondst of its
melding exceeds 15. Every such "or" is an `Uitzondering op` above a base
(RUNTIME-SEMANTICS ch. 7), and every justification counts or reads further
than one role:

| group (target) | exception above the base | justification |
|---|---|---|
| premie (Jutter) | premie bij veel vondsten, 20 | het aantal vondsten van de Jutter groter is dan 2 |
| premie (Jutter) | premie bij een zware vangst, 30, written later so stacked higher | de som van het gewicht van alle vondsten van de Jutter die zwaar zijn groter is dan de zwaartegrens |
| ijverig (Jutter, characteristic) | lichte vondsten, niet ijverig | ten minste één van: de minimale waarde van het gewicht van alle vondsten van de Jutter kleiner dan 2; de maximale waarde kleiner dan 5 |
| gemelde premie (Melding) | melding over een rijke jutter, 20 | de premie van de jutter van de vondst van de Melding groter is dan 15 (two roles) |
| dringend (Melding, characteristic) | niet dringend | the same premie groter is dan 25 (two roles) |
| controlepremie (Controle) | controle van een rijke jutter, 15 | de premie van de jutter van de vondst van de melding van de Controle groter is dan 15 (three roles) |

The forms: a count, a sum with a filter over a derived characteristic
against a parameter, a minimum and a maximum in a composite, a comparison two
and three roles away, and characteristic targets beside attribute targets.
The premie that the Melding and the Controle read is itself written by a
defaults group, and `bron.rgs` declares the Melding and the Controle, and
their groups, before the Jutter: a group reads what a group declared after it
writes.

## Hand computation

`case1-staffel.json`, zwaartegrens 14.

Zwaar: V1 (6) and V3 (9) of J1, V7 (12) of J3; V2 (4), V4 (2), V5 (3), V6
(1), V8 (4) and V9 (3) are not.

Premie: J1 has three vondsten, and the gewicht of his zware vondsten is 6 +
9 = 15 > 14, so the highest exception decides **30**. J2 has three vondsten
and no zware one: the sum over no contributor is leeg, a comparison with
leeg does not hold (ch. 1.2), and the count 3 > 2 decides **20**. J3 has
one vondst, zwaar, 12 is not above 14 and 1 is not above 2: the base, **10**.
J4 has none: **10**. J5 has two, neither zwaar: **10**.

IJverig: J1 minimum 4, maximum 9, neither below its bound: the exception does
not hold and the base attributes, J1 **is ijverig**. J2 minimum 1 < 2:
**niet ijverig**. J3 12 and 12: **ijverig**. J4 has no vondst, so minimum and
maximum are leeg and neither comparison holds: **ijverig**. J5 minimum 3,
maximum 4 < 5: **niet ijverig**.

Meldingen: M1 reaches J1 through V1 (premie 30), M2 J2 through V4 (20), M3
J3 through V7 (10), M4 has no vondst, so its premie reads leeg (ch. 5.8,
rule 1). Gemelde premie: 30 > 15 and 20 > 15, so M1 and M2 **20**; M3 and M4
**10**. Dringend: only 30 > 25, so M1 is **niet dringend** and M2, M3 and M4
are **dringend**.

Controles: C1 reaches J1 through M1 and V1 (30 > 15): **15**. C2 reaches J3
(10): **5**. C3's melding has no vondst and C4 has no melding: **5** each.

`case2-hogere-zwaartegrens.json`, zwaartegrens 20, J1 with the same three
vondsten: 15 is not above 20, so the count decides **20**; J1 **is
ijverig**; M1 of V1 gets 20 > 15, **20**, and 20 is not above 25, so M1 **is
dringend**; C1 of M1 **15**. The parameter moves J1 from 30 to 20 and M1
from niet dringend to dringend.

A no-op run fails both cases: it leaves every derived value leeg and every
derived characteristic absent (case 1: 22 mismatches, among them `J1.premie =
leeg, expected 30`; case 2: 5).

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (26
expectations)`, `(5 expectations)`; clingo the same). The Drools exec-model
branch plays them: `PLAYED 2`. The DRL-text branch refuses the document on
its defaults groups:

    EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering

Measured on the exec-model branch before it lowered such a justification
(2026-09-29), the whole document was refused on its first group:
`defaults (Melding.gemelde premie): niet-reduceerbare justificatie
(far-side/aggregatie/global): <default>`. With the justifications lowered
but the groups built in declared order, each at the salience of its own-type
producers only (the order switched off in a probe of the branch, 2026-09-30),
case 1 answered `attribuut gemelde premie: verwacht 20, werkelijk 10` for M1
and M2, `kenmerk dringend: verwacht afwezig, werkelijk aanwezig` for M1 and
`attribuut controlepremie: verwacht 15, werkelijk 5` for C1, and case 2 the
gemelde premie and the controlepremie the same: the groups of the Melding and
the Controle decided on a leeg premie, before the group of the Jutter had
written it, and nothing made them decide again; a characteristic once
attributed would stay in any case (ch. 5.3). The branch now adds every group
after the groups that write what it reads and gives it their salience, and
plays both cases in the order of `bron.rgs`.

Not in this golden, with its reason: a quantifier fold and an aggregation
inside an arithmetic expression in a justification (refused by name in the
exec-model branch); a characteristic check two roles away (the RegelSpraak
frontend emits it as an unresolved condition, which `ref` and clingo refuse
too).
