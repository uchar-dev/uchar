# synth3-takhek

The **arm-fence witness** on the rule-head route: a hand-written `ir.json`
with no `source.rgs`, like its `synth3-*` siblings — the RegelSpraak emitter
produces no `IfThenElse` at all (IR-SPEC ch. 4.10), so this family has no
source language and is authored on the wire.

Its job is to pin, at corpus level, the half of `RUNTIME-SEMANTICS.md`
ch. 10.2 that says **exactly the chosen branch is evaluated** — and to pin
it for what the non-chosen arm *produces* as well as for what it computes:
a §6.5 divisor place there deposits no §9.5 channel reason, and a numeric
zero there raises no fault (ch. 5.11: an operand never reached raises no
fault and deposits no channel fact).

## The shape

```
heffing := let k = zijn inkomen in
             if k > drempel then basisheffing
                            else k / zijn restpost
```

`k` routes the attribute read through a binder, which is what WF-077 asks
of a condition operand. The division — the §6.5 place and the fail-fast
place at once — sits in the `else` arm only.

## Why this discriminates

An executor that evaluates the whole value and then selects (eager
placement) reaches the division on **every** case. On J1 it would deposit
a §6.5 channel reason the case forbids; on J2 it would break the run off
with *division by numeric zero*. Both are observable, and both are the
opposite of a missing answer: they are a **fabricated** one. J4 is the
mirror that keeps this honest — there the arm *is* entered, the same two
places *must* speak, and an executor that simply suppressed them fails.

## Hand-computed absolutes

`drempel = 10,00`, `basisheffing = 5,00`.

| case | Jutter | inkomen | restpost | arm  | heffing                               |
|------|--------|---------|----------|------|---------------------------------------|
| 1    | J1     | 40,00   | *absent* | then | **5,00**, no channel reason           |
| 1    | J2     | 40,00   | 0,00     | then | **5,00**, no fault                    |
| 1    | J3     | 5,00    | 2,00     | else | 5,00 / 2,00 = **2,50**                |
| 2    | J4     | 5,00    | *absent* | else | **leeg**, *with* the channel reason   |

Case 1 also carries `consistent`, so the three rows together assert that
**no** inconsistency fact exists; case 2 asserts that this rule × this
subject carries one. The two files are separate because those two claims
are each global to a case.

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6), anchored to
these hand-written absolutes; the Drools exec-model branch PLAYS it
(gate 12), so ref ≡ clingo ≡ Drools. NOOP-REFUTING: J1, J2 and J3 are
computed and absent from the raw input, so a do-nothing run mismatches
them (gate 7).
