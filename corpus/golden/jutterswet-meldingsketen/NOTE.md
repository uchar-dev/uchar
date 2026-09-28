# jutterswet-meldingsketen — a condition on a derived value of the vinder

A Vondst is reported by its vinder, a Jutter (art. 3a, the aanmelding).
The Jutter carries a meldingsdatum; the strandvoogd recognises it as the
erkende meldingsdatum, unless a herstelde meldingsdatum was set, which
then takes its place. A Jutter with an erkende meldingsdatum is erkend.
Five rules on the Vondst read these DERIVED values of their vinder, one
role step away (`vondstmelding`, one vinder, many vondsten), in their
condition:

| rule on the Vondst | condition over the vinder |
|---|---|
| aanmelddatum := de vinddag | the erkende meldingsdatum of the vinder is gevuld |
| controledatum := de vinddag plus 14 dagen | all of: het gewicht is groter dan 0 (own); the erkende meldingsdatum of the vinder is later dan de vinddag |
| is tijdig aangemeld | the erkende meldingsdatum of the vinder is eerder dan the parameter meldtermijngrens |
| meldingsgewicht := het gewicht gedeeld door de weegfactor | zijn vinder is erkend |
| is ongemeld | zijn vinder is niet erkend |

and three rules on the Jutter derive what they read: the erkende
meldingsdatum through a defaults group (`Uitzondering op erkende
meldingsdatum`: the herstelde meldingsdatum where it is gevuld, else the
meldingsdatum), and the characteristic erkend from it. In `bron.rgs` the
five readers come first and the derivations last.

The forms, one per rule: a gevuld test, a compound with an own and a
navigated part, a comparison with a parameter, a navigated
characteristic check and its negation; the value read is written by a
defaults group and by a characteristic assignment; and the §6.5 division
under the condition, whose fault is reported only where the condition
holds. V2 has no vinder, so every condition reads through an absent link.

## Hand computation

Parameter meldtermijngrens is 01-04-2026 in all three cases.

`case1-erkende-melding.json`: J1 has meldingsdatum 10-03-2026 and no
herstelde meldingsdatum, so the exception is not justified and the base
gives the erkende meldingsdatum **10-03-2026**; J1 **is erkend**.
V1 (vinder J1, vinddag 01-03-2026, gewicht 12, weegfactor 4): the erkende
meldingsdatum is gevuld, so the aanmelddatum is the vinddag
**01-03-2026**; gewicht 12 > 0 and 10-03-2026 is later than 01-03-2026,
so the controledatum is 01-03-2026 + 14 dagen = **15-03-2026**;
10-03-2026 is earlier than 01-04-2026, so V1 **is tijdig aangemeld**;
J1 is erkend, so the meldingsgewicht is 12 / 4 = **3,00**; J1 is erkend,
so V1 is **not ongemeld**.
V2 (no vinder, vinddag 05-03-2026, gewicht 8, weegfactor 2): the link is
absent, so every read of the vinder is leeg (RUNTIME-SEMANTICS ch. 5.8,
rule 1). Gevuld does not hold, a comparison with leeg does not hold, and
a characteristic check over an absent link does not hold in either
polarity (negation does not resurrect an absent relation): aanmelddatum,
controledatum and meldingsgewicht stay **leeg**, V2 is **not tijdig
aangemeld** and **not ongemeld**. No division runs, so the case is
**consistent**.

`case2-herstelde-melding.json`: J2 has meldingsdatum 10-03-2026 and
herstelde meldingsdatum 15-04-2026; the exception is justified and takes
priority over the base, so the erkende meldingsdatum is **15-04-2026**;
J2 **is erkend**.
V3 (vinder J2, vinddag 01-04-2026, gewicht 9, no weegfactor):
aanmelddatum **01-04-2026**; 9 > 0 and 15-04-2026 is later than
01-04-2026, so the controledatum is **15-04-2026**; 15-04-2026 is not
earlier than 01-04-2026, so V3 is **not tijdig aangemeld**; J2 is
erkend, so the division 9 / leeg runs: the meldingsgewicht is **leeg**
and the §6.5 fault makes the case **inconsistent**; V3 is **not
ongemeld**.
V4 (vinder J2, vinddag 20-04-2026, gewicht 6, weegfactor 3): aanmelddatum
**20-04-2026**; 15-04-2026 is not later than 20-04-2026, so the
controledatum stays **leeg**; V4 is **not tijdig aangemeld**; the
meldingsgewicht is 6 / 3 = **2,00**; V4 is **not ongemeld**.

`case3-geen-melding.json`: J3 has no meldingsdatum and no herstelde
meldingsdatum; neither default writes a value, so the erkende
meldingsdatum stays **leeg** and J3 is **not erkend**.
V5 (vinder J3, vinddag 01-03-2026, gewicht 5, weegfactor 5): aanmelddatum
and controledatum stay **leeg**, V5 is **not tijdig aangemeld**, the
meldingsgewicht stays **leeg** (the condition does not hold, so no
division runs and the case is **consistent**), and J3 is linked and not
erkend, so V5 **is ongemeld**.

A no-op run fails every case: case 1 and 2 expect derived dates and
weights the input does not carry, case 3 expects V5 ongemeld.

## The square

`ref` and the ASP backend replay all three cases (`oracle: PASS (13
expectations)`, `(13 expectations)`, `(8 expectations)`; clingo the same).
The Drools exec-model branch plays them: `PLAYED 3`. The DRL-text branch
refuses the document on its defaults group:

    EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering

Measured on the exec-model branch before it re-tested such a condition
after the far-side join (2026-09-28): case 1 `kenmerk ongemeld: verwacht
afwezig, werkelijk aanwezig`, case 2 the same for V3 and V4. The
condition was tested on the Vondst when it was inserted, when the
vinder's erkend characteristic was still absent, and not again when the
vinder became erkend. The other four readers passed there only by
accident: the wrong ongemeld write updated the Vondst after the
derivations, which ran their tests once more. The same document without
the ongemeld rule answered, on that branch, in case 1 the aanmelddatum,
controledatum and meldingsgewicht of V1 leeg and tijdig aangemeld
absent, and in case 2 both aanmelddatums, the controledatum of V3 and the
meldingsgewicht of V4 leeg and the case consistent.
The branch now plays all three cases in the order of `bron.rgs`, and
also with the three derivations declared first.
