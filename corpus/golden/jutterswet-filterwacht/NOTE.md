# jutterswet-filterwacht

*Since 2026-10-19 (card `zero-division-channel`) a filled dividend divided by
a numeric zero is leeg with a §9.5 fact of kind EF-005, no fault
(`RUNTIME-SEMANTICS.md` ch. 1.4 point 4). Every case here pins `consistent`, so
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

Case 3, a Jutter that is not actief beside one that is, and a vondst that is
in no Jutter's range: `J1` as in case 1 (`V1` 10 ÷ 2, `V2` 9 ÷ 0), `J2` as in
case 2 (`V3` 10 ÷ 2, `V4` 3 ÷ 3), and `V9` (9 ÷ 0), which no Jutter has.

| Jutter | actief | `vondstental` | `zware vondsten` | `druk` |
|--------|--------|---------------|------------------|--------|
| J1 | no | **2** | **leeg** | **no** |
| J2 | yes | **2** | **1** | **yes** |

and the session is consistent: the fold is evaluated for `J2` only, over `V3`
and `V4`, so neither `V2` nor `V9` is divided. An executor that guards the
division in the filter by the reading form's condition alone, without joining
the member to the fold's subject, divides `V2` and `V9` here.

## The differential square

`ref` ≡ clingo over all eighteen expectations (gates 5 and 6), anchored to the
hand-written absolutes above. Gate 12 replays the three cases in the exec-model
branch and in the DRL-text branch; the truth-maintenance branch plays them
too (`TmsCorpusWitnessTest`). Gate 10 re-emits `source.rgs` to a
byte-identical `ir.json`, and gate 16 regenerates the three case files from
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
