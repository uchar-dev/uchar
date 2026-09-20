# periodevenster-tellende-kwantor

The **second** bound `RUNTIME-SEMANTICS.md` ch. 3.10's v0 mirror note puts
on the period condition:

> v0's backends lower only literal or own-scalar-attribute bounds and
> refuse the rest, **and refuse the condition under counting quantifiers**.

The window here is the second member of a `GEEN` composite. What the
refused position would otherwise do is not a near miss but a reading
nothing norms: the quantifier COMPLEMENTS the window's verdict, so a
time-free session — where ch. 3.10 says the window never matches — makes
the composite **true** and fires the rule. The chapter norms the window as
a gate on the RULE; a quantifier that counts or complements its verdict is
using it as something else. This case carries `asOfDate: null` for exactly
that reason: it is the input on which the unnormed reading is loudest.

## Where the boundary comes from — measured, not invented

One verifier-clean document per quantifier, 2026-09-20, the window always
the second composite member:

| quantifier | Drools exec-model | Drools DRL-tekst |
|---|---|---|
| `ALLE` | PLAYED | PLAYED |
| `DE` | PLAYED | PLAYED |
| `GEEN` | transpiler SKIP | MARKER: periodevergelijking onder een tellende kwantor (§8.4.3) |
| `TEN_MINSTE` | transpiler SKIP | the same marker |
| `TEN_HOOGSTE` | transpiler SKIP | the same marker |
| `PRECIES` | transpiler SKIP | the same marker |

So the carried pair is `ALLE` and `DE` — and `ALLE` is exactly what the
green witness `corpus/golden/jutterswet-premievenster` plays through all
four executors. The refused four are refused because the DRL-text branch
names this very limit, not because the two OCaml executors chose a line of
their own: the no-superset rule this card applies to the bound shape,
applied to the position.

The quantifier FOLD (ch. 3.9a, `Kwantorvouw`) is refused by the same rule
and the same sentence, without a Drools measurement of its own: its
quantifiers are the composite's, the quantification is the same one over
instances instead of parts, and the safe direction of an unmeasured
boundary is the refusal. Recorded rather than glossed.

## Four executors, and the pins carry their phase

    verifier            PASS
    ref                 EVALUATION FAULT: a period condition under GEEN or a
                        counting quantifier … Declined at evaluation
    clingo              TRANSLATION FAULT: the same sentence … Declined in
                        translation
    Drools exec-model   EXCLUDE transpiler SKIP: voorwaarde buiten Phase-B scope
    Drools DRL-tekst    EXCLUDE drl-transpiler MARKER: TODO
                        periodevergelijking onder een tellende kwantor (§8.4.3)

The day a card norms the position, this directory stops being red and the
gate's exclusion list goes stale with it.
