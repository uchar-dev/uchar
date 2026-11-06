# jutterswet-loopgrens — a Let binder against a value in another unit

A Strook has an `aanlooplengte` in metres. Two rules read it through a variable ("Daarbij geldt: de loop is de
aanlooplengte van de Strook"), which the frontend lowers to a `Let` with a `BinderRef` (IR-SPEC ch. 4.10), and set it
against a literal in kilometres:

- `schouwlengte` = de loop, met een minimum van 1 km (a `Clamp`: the minimum compares the binder with the bound);
- `looplengte` = de loop plus 1 km (an additive join).

A binder carries the unit of its value (RUNTIME-SEMANTICS ch. 2.6, 10.6), so the bound and the summand are restated
into metres: 1 km is 1000 m.

## The hand calculation

| case | aanlooplengte | schouwlengte | looplengte |
|---|---|---|---|
| case1-korte-aanloop | 500 m | max(500, 1000) = **1000** | 500 + 1000 = **1500** |
| case2-lange-aanloop | 1500 m | max(1500, 1000) = **1500** | 1500 + 1000 = **2500** |

Read without the unit, the clamp would take max(500, 1) = 500 in case 1 (the other bound) and max(1500, 1) = 1500 in
case 2 (the same one), and the sum would be 501 and 1501.

## Provenance

Own work (card `clingo-let-binder-vergelijking-over-eenheden`, 2026-11-06), written as a RegelSpraak source; gate 10
re-emits it. Measured at admission: verifier PASS; ref and both branches of the Drools backend play both cases; the
ASP backend of main `17f956d92` answered 500 and 501 in case 1 and 1501 in case 2 (the binder read as a bare number;
the same forms over the attribute itself translated), and plays both cases since that card.
