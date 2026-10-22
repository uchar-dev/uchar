# jutterswet-filterwacht

*Since 2026-10-19 (card `zero-division-channel`) a filled dividend divided by
a numeric zero is leeg with a §9.5 fact of kind EF-005, no fault
(`RUNTIME-SEMANTICS.md` ch. 1.4 point 4). Both cases here pin `consistent`, so
they pin that the division does not happen where the reading form does not
reach the fold; the twin where it is reached is the golden
`fold-filter-divides-by-zero` (a red case until then). What follows about the
fault describes the state before that card.*

A fold whose subselection divides, read where the reading form does not
reach it. The reference interpreter evaluates a rule's condition first and
its value only where the rule fires, a composite condition only up to the
member after which its verdict stands (`RUNTIME-SEMANTICS.md` ch. 5.10),
and the `filter` of an instance fold per member of the subject's range,
only where it evaluates the fold (ch. 3.4a). A filter that divides by a
numeric zero there is a fault (ch. 1.4, point 4); nowhere else does the
division happen.

## What it pins

```
De zware vondsten van een Jutter moet berekend worden als het aantal
vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht
gedeeld door zijn deelnemers is groter dan 2
indien hij actief is.

Een Jutter is druk
indien hij aan alle volgende voorwaarden voldoet:
- hij is actief
- het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen:
  zijn gewicht gedeeld door zijn deelnemers is groter dan 2 is groter dan 0.

Het vondstental van een Jutter moet berekend worden als het aantal vondsten
van de Jutter.
```

In the first rule the fold is the value of a rule whose condition does not
hold for `J1`; in the second it is the member after the one that settles
the composite for `J1`. `J1`'s vondst `V2` has `deelnemers` 0, so a filter
evaluated there divides 9 by 0. The third rule reads the same range without
a filter, so `J1`'s leeg `zware vondsten` is the condition's doing, not an
empty range; it also gives the first case a derived value the no-op guard
(gate 7) can refute.

## Hand-computed absolutes

Case 1, `J1` not actief, with `V1` (gewicht 10, deelnemers 2) and `V2`
(gewicht 9, deelnemers 0):

| Jutter | actief | `vondstental` | `zware vondsten` | `druk` |
|--------|--------|---------------|------------------|--------|
| J1 | no | **2** | **leeg** — the rule does not fire, the fold is never evaluated | **no** — the composite stops after "hij is actief" |

and the session is consistent.

Case 2, two actieve Jutters whose vondsten divide cleanly:

| Jutter | vondsten (gewicht ÷ deelnemers) | `vondstental` | `zware vondsten` | `druk` |
|--------|----------------------------------|---------------|------------------|--------|
| J2 | V3 (10 ÷ 2 = 5 > 2), V4 (3 ÷ 3 = 1) | **2** | **1** | **yes** (1 > 0) |
| J3 | V5 (4 ÷ 4 = 1) | **1** | **0** | **no** (0 > 0 fails) |

and the session is consistent. Case 2 is the control: where the condition
holds the fold IS evaluated, its filter divides, and it decides both ways
(`J2` counts 1, `J3` counts 0, not leeg).

Where `J1` IS actief the fold is reached and `V2` divides by 0: that is the
red case `corpus/golden/fold-filter-divides-by-zero`, which pins the fault in
`ref`, clingo and the Drools exec-model branch.

## Why the two cases are separate

The ASP backend faults where the reference interpreter answers when a
Jutter that is not actief shares a session with one that is, or when a
vondst whose `deelnemers` is 0 is in no Jutter's range: its fault guard on
the division in the filter carries the rule's condition but does not join
the member to the fold's subject (measured 2026-10-09: `ref` passes, clingo
reports `viol("division by numeric zero","V2")`). So no case here holds a
Jutter that is not actief beside one that is, nor a vondst outside every
range; `backends/drools`' `FilterFaultWitnessTest` carries both shapes for
the Drools branches.

*No longer so (measured 2026-10-22):* since `zero-division-channel`
(2026-10-19) a division by a numeric zero is a channel kind, and a session
with a Jutter that is not actief beside one that is replays in clingo as in
`ref`; since `fix/clingo-filter-guard-binding` (2026-10-22) a guard inside a
fold member also joins the member to the subject of the rule. The two cases
stay as they are.

## The differential square

`ref` ≡ clingo over all eleven expectations (gates 5 and 6), anchored to the
hand-written absolutes above. Gate 12 replays both cases in the exec-model
branch and in the DRL-text branch; the truth-maintenance branch plays them
too (`TmsCorpusWitnessTest`). Gate 10 re-emits `source.rgs` to a
byte-identical `ir.json`, and gate 16 regenerates both case files from
`scenario.rgs`.

Before the fix, both Drools branches faulted on case 1, because they tested
the filter on the pattern of every Vondst as it was inserted (measured
2026-10-09):

```
exec-model: ERROR DelingDoorNul: deling door nul in de regel "zware vondsten":
            9 gedeeld door 0 — een numerieke nul als deler is een fout, geen
            leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)
DRL-text:   ERROR ConstraintEvaluationException: Error evaluating constraint
            '(rsAggVergelijk(rsGedeeld(gewicht, deelnemers),
            new java.math.BigDecimal("2"), ">"))' in [Rule "drukte",
            "zware vondsten" in dev/uchar/regelspraak/generated/regels.drl]
```
