# jutterswet-melding-naar-aard — two writers of one target whose conditions exclude each other

The green twin of `rood/jutterswet-dubbele-melding`. Two attributes of a
Vondst are each written by two conditional rules, and on every instance at
most one of the two conditions holds, so every target has at most one writer
per instance and each rule's equation holds at the fixpoint
(RUNTIME-SEMANTICS ch. 5.1):

| target | rule | type | condition | value |
|---|---|---|---|---|
| meldingsdatum | melding na vinden | Vondst | zijn aanspoeldatum leeg is | zijn vinddatum |
| meldingsdatum | melding na aanspoelen | Strandvondst | zijn aanspoeldatum gevuld is | zijn aanspoeldatum |
| vindersloon | loon voor een zware vondst | Vondst | zijn gewicht groter is dan 5,00 | 25,00 |
| vindersloon | loon voor een lichte vondst | Vondst | zijn gewicht kleiner of gelijk is aan 5,00 | 10,00 |

and `afhaaltermijn` reads the meldingsdatum: the uiterste afhaaldatum is the
meldingsdatum plus 30 dagen. The meldingsdatum pair stands on a supertype and
its subtype: `melding na vinden` ranges over every Vondst, Strandvondsten
included (IR-SPEC ch. 2.2), and `melding na aanspoelen` over the
Strandvondsten alone. Both read the aanspoeldatum the Vondst declares. On a
plain Vondst with an aanspoeldatum neither rule writes. The vindersloon pair
splits on a threshold, and an empty gewicht satisfies neither comparison
(ch. 1.2d), so the vindersloon then stays leeg.

Where both conditions of such a pair hold on one instance and the values
differ, the two equations have no common solution and every executor faults
(ch. 5.7): that is the red twin. This golden pins that the form whose
conditions exclude each other keeps its answer in all four executors.

## Hand computation

`case1-vondsten-naar-aard.json`:

- V1, a Vondst: vinddatum 04-03-2026, no aanspoeldatum, gewicht 7,50. Its
  aanspoeldatum is leeg, so `melding na vinden` writes **04-03-2026**; V1 is
  no Strandvondst. 7,50 > 5,00, so the vindersloon is **25,00**. Uiterste
  afhaaldatum 04-03-2026 plus 30 dagen = **03-04-2026**.
- V2, a Vondst: vinddatum 05-03-2026, aanspoeldatum 02-03-2026, gewicht 5,00.
  Its aanspoeldatum is filled and it is no Strandvondst: no rule writes its
  meldingsdatum, which stays **leeg**, and so does its uiterste afhaaldatum
  (a leeg date plus a duration is leeg, ch. 1.2). 5,00 is not greater than
  5,00 and is smaller or equal: vindersloon **10,00**.
- S1, a Strandvondst: vinddatum 04-03-2026, aanspoeldatum 01-03-2026, gewicht
  2,25. `melding na aanspoelen` writes **01-03-2026**; `melding na vinden`
  does not hold. Vindersloon **10,00**; uiterste afhaaldatum 01-03-2026 plus
  30 dagen = **31-03-2026**.
- S2, a Strandvondst: vinddatum 06-03-2026 only. Its aanspoeldatum is leeg,
  so the supertype's rule writes **06-03-2026**. No gewicht: vindersloon
  **leeg**. Uiterste afhaaldatum **05-04-2026**.

`case2-grenzen-en-lege-gegevens.json`:

- V4, a Vondst: vinddatum 01-03-2026, gewicht 5,01. Meldingsdatum
  **01-03-2026**; 5,01 > 5,00, vindersloon **25,00**.
- S3, a Strandvondst: aanspoeldatum 28-02-2026 only. Meldingsdatum
  **28-02-2026**; uiterste afhaaldatum 28-02-2026 plus 30 dagen =
  **30-03-2026** (February 2026 has 28 days); vindersloon **leeg**.
- V5, a Vondst without values. `melding na vinden` holds (no aanspoeldatum)
  and evaluates leeg, so the meldingsdatum is **leeg**; vindersloon **leeg**;
  uiterste afhaaldatum **leeg**.

A no-op run fails both cases: it leaves every derived value leeg (case 1:
9 mismatches, case 2: 4).

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (12
expectations)`, `(8 expectations)`; clingo the same). Both Drools branches
play them, `PLAYED 2` in the exec-model branch and in the DRL-text branch,
on the jar of the card that named the divergence of the red twin and on the
jar before it alike: the form whose conditions exclude each other played in
all four executors before that card, and it is the form that card had to
leave as it was. The ASP backend gives each of the two writers of one target
an atom of its own and a pairwise `viol/2` for two different values on one
subject; with these conditions no subject carries two writers, so none
fires. In the DRL-text branch each of these writers watches its target; a
write by the one re-evaluates the other, whose condition does not hold on
that instance, so nothing fires again.

Not in this golden, with its reason: two writers whose conditions both hold
with the SAME value (the equations then hold together and every executor
plays, but whether an overlap that agrees is a modelling error a static rule
should refuse is open); two characteristic attributions on one
characteristic (monotone, outside the question); the opgave marker on a
target with two writers, which moves both to the generated `berekende`
attribute and keeps the same question there.
