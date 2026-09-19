# synth3-armlichaam

The **closed lambda body with a branch node** witness — the sixth golden of the
ch.-10.2/10.6 family and the one form the card that built the other five
(`synth3-takhek`, `synth3-operatortak`, `synth3-armverdeling`,
`synth3-armwissel`, `synth3-armvangst`) measured and could not land, because
admitting it needed a channel pin outside that card's mandate.
`lichaamtotaal` folds

    (acc, x) -> Let m = de grondslag gedeeld door het kindertal in
                  (als x >= 0 dan acc plus 1,00 anders acc plus m)

over the list parameter `kindertallen`. Every other member of that family puts
the `Let`/`IfThenElse` chain at RULE level, where a binder can be leeg and the
barrier is the §6.5 channel. Here the same chain sits inside a CLOSED BODY: the
binders `acc`, `x` and `m` are never leeg, there is no subject-keyed helper and
no leeg barrier, so what the arm has to carry is not the channel but the
**fences** — the division's numeric-zero guard and its exactness guard.

## What this golden exists to falsify

**That a closed-body `Let` value may be placed eagerly.** `m` is
`grondslag / x` with `x` a bound list element — a RUNTIME divisor
(`jutterswet-kinderdeler`'s form) — and the body reads `m` in exactly ONE arm.
Case 1 feeds that fold a single `0`, whose arm is therefore the one the fold
does NOT take. An implementation that evaluates the `Let` value before the
branch divides 60 by 0 and must either fault or derive a `viol/2`; the answer
1,00 is only reachable if the value went into the arm that reads it. The other
three cases keep that from being the whole story: case 2 shows the anders-arm
really computes with `m` rather than skipping it, case 3 interleaves the two
arms over one accumulator, and case 4 pins the fold base.

**The falsifying twin is already in the corpus, and it faults.**
`corpus/rood/closed-runtime-division-by-zero` is the same fold over the same
`grondslag = 60` with a `0` among the kindertallen and NO branch node, and both
executors fault there with *"division by numeric zero"*. Same divisor, same
dividend, one structural difference — so case 1 answering 1,00 in both
executors is a measurement of the arm placement and not of an absent fence.

The element type is `teken: ONBEPAALD` on purpose. With the sign declaration of
`jutterswet-kinderdeler` (`NIET_NEGATIEF`) the condition `x >= 0` would be
vacuously true and the anders arm unreachable, which would make the document a
one-armed witness dressed as a two-armed one.

## What the ASP translation does with it, verbatim

The `Let` value stands syntactically OUTSIDE the branch node, and the ch.-10.6
rewrite moves it into the single arm that reads it. In the emitted program that
is visible as the arm guard `V < 0` — the complement of `x >= 0` — on the
value's three fences and on its recurrence, while the dan-arm's recurrence
carries `V >= 0` and no fence of the division at all:

    viol("division by numeric zero",…) :- …, lelem(…,I,V), V < 0,
        param("grondslag",P2), (V) == 0.
    fa1(…,I,(A+(((P2)*100)/(V)))) :- …, V < 0, param("grondslag",P2),
        (V) != 0, ((P2)*100) \ (V) == 0.
    fa1(…,I,(A+100))              :- …, lelem(…,I,V), V >= 0.

`V < 0, (V) == 0` is unsatisfiable, which is exactly why case 1 answers rather
than derives a `viol/2`. (clingo's grounder prints an `info: operation
undefined ((100*P2)\V)` while instantiating the exactness fence at `V = 0`; the
rule instance is discarded on `V < 0` regardless, and the same message appears
on the red twin above, so it is a pre-existing grounder remark and not a
property of this document.)

## Hand-computed absolutes (`grondslag = 60`, fold base `0.00`)

Per element, in list order; `>=0?` decides the arm, and `m = 60 / x` is
evaluated only where the anders arm is taken.

**case1-nuldeler — `kindertallen = [0]`**

| x | x >= 0 | arm | m | acc after |
|---|--------|-----|---|-----------|
| 0 | yes | dan: acc + 1,00 | *not evaluated* | **1.00** |

`lichaamtotaal = 1.00`. The eager reading answers nothing at all here: `60 / 0`.

**case2-maatdeel — `kindertallen = [-4]`**

| x | x >= 0 | arm | m | acc after |
|---|--------|-----|---|-----------|
| -4 | no | anders: acc + m | 60 / -4 = -15.00 | **-15.00** |

`lichaamtotaal = -15.00`.

**case3-gemengd — `kindertallen = [0, 3, -5, -2]`**

| # | x | x >= 0 | arm | m | acc after |
|---|---|--------|-----|---|-----------|
| 1 | 0 | yes | dan: acc + 1,00 | *not evaluated* | 0.00 + 1.00 = **1.00** |
| 2 | 3 | yes | dan: acc + 1,00 | *not evaluated* | 1.00 + 1.00 = **2.00** |
| 3 | -5 | no | anders: acc + m | 60 / -5 = -12.00 | 2.00 + (-12.00) = **-10.00** |
| 4 | -2 | no | anders: acc + m | 60 / -2 = -30.00 | -10.00 + (-30.00) = **-40.00** |

`lichaamtotaal = -40.00`. Both divisors divide 60 exactly at the 2-decimal
target scale (`6000 \ x == 0`), so the closed-body exactness guard
(`jutterswet-kinderdeler`'s `numt \ dent == 0`) is satisfied and the quotient is
a value rather than a `viol/2`.

**case4-leeg-lijst — `kindertallen = []`**

No element, so the fold base fires on `lpres` and nothing in the body is
evaluated: `lichaamtotaal = 0.00`.

All four cases match exactly in ref and clingo (gate 5 ≡ gate 6).

## The Drools square is two-thirds here, and says so

BOTH Drools branches — the exec-model one gate 12 counts and the DRL-text one it
counts beside it — refuse this golden at the SHARED loader, with the identical
line

    EXCLUDE loader: form not carried by the v0 Drools backend:
      Datatype:ListType (unknown datatype node)

A closed lambda body needs a list-typed fold operand (WF-069) or a function
section (`functionDefs`), and the v0 loader carries neither, so the refusal is
per form and is pinned by name in both of that gate's channel lists. The
absolute values above are therefore a ref = clingo agreement anchored to
hand-written numbers, not the full three-executor square — recorded here rather
than left to be inferred from a count. It is the one member of its family in
that position: the exec branch plays the other five ch.-10.2/10.6 goldens,
which put the same chain at rule level where no list type is involved.

NOOP-REFUTING: `lichaamtotaal` is a derived attribute on every case and no case
supplies it, so an identity transpiler answers leeg on all four.
