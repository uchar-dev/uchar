# jutterswet-sluitdag — computing rules under a quantifier condition whose operands are derived later

A Berging holds vondsten. The bergdag of a Vondst is the day after its
vinddag, when it has one; the laatste bergdag of the Berging is the latest
bergdag of its vondsten, its uiterste meldingsdag five days after that, and
its wachttijd its openingsdag minus 10. Three rules decide a day of the
Berging under a quantifier fold over its vondsten (IR-SPEC ch. 4.5,
RUNTIME-SEMANTICS ch. 3.9a), and each reads, in its value, attributes of
the Berging that other rules derive:

| rule | condition | value | its operands come from |
|---|---|---|---|
| sluitdag | de bergdag van alle vondsten van de Berging gevuld is | zijn laatste bergdag plus 1 | the fold over the derived bergdag |
| afrekendag | de vinddag van geen van de vondsten van de Berging leeg is | gesteld op zijn uiterste meldingsdag | a rule on the laatste bergdag, two steps from the bergdag |
| controledag | de bergdag van ten minste één van de vondsten van de Berging gevuld is | zijn laatste bergdag plus zijn wachttijd | the fold, and a rule on the openingsdag |

`bron.rgs` declares the three readers before the rules that derive their
operands. RUNTIME-SEMANTICS ch. 5.1 re-evaluates a rule whenever a value it
reads changes, so the order does not matter to the answer; the sluitdag is
the plus form of the reproduction of the card
drools-exec-kwantorvoorwaarde-eigen-operand.

## Hand computation

`case1-twee-gevonden-vondsten.json`: B, openingsdag 25; V1, vinddag 30; V2,
vinddag 20. Bergdag V1 **31**, V2 **21**; laatste bergdag max(31, 21) =
**31**; uiterste meldingsdag 31 + 5 = **36**; wachttijd 25 − 10 = **15**.
Both bergdagen are filled, so the sluitdag is 31 + 1 = **32**; neither
vinddag is empty, so the afrekendag is the uiterste meldingsdag, **36**; a
bergdag is filled, so the controledag is 31 + 15 = **46**.

`case2-een-vondst-zonder-vinddag.json`: B, openingsdag 15; V1, vinddag 30;
V2 without vinddag. Bergdag V1 **31**, V2 **leeg** (the rule's condition
does not hold); laatste bergdag **31** (an empty contributor is skipped,
ch. 1.2); uiterste meldingsdag **36**; wachttijd **5**. V2's bergdag is
empty, so "alle" does not hold and the sluitdag stays **leeg**; V2's vinddag
is empty, so "geen" does not hold and the afrekendag stays **leeg**; V1's
bergdag is filled, so the controledag is 31 + 5 = **36**.

`case3-geen-vondsten.json`: B, openingsdag 12, no vondst. The laatste
bergdag is the maximum over no contributor, **leeg**, and stays so: the
uiterste meldingsdag is leeg plus 5 = **5** (plus coalesces an empty
operand, ch. 1.2); wachttijd **2**. Over an empty set "alle" and "geen" hold
and "ten minste één" does not (ch. 3.9a): the sluitdag is leeg plus 1 =
**1**, the afrekendag **5**, the controledag **leeg**. Here the empty
operand is the fixpoint value and not a transient one, so ch. 1.2 decides.

A no-op run fails all three cases: it leaves every derived value leeg
(case 1: 8 mismatches, case 2: 5, case 3: 4).

## The square

`ref` and the ASP backend replay the three cases (`oracle: PASS (8
expectations)`, `(8 expectations)`, `(6 expectations)`; clingo the same).
Both Drools branches play them: `PLAYED 3` in the exec-model branch and in
the DRL-text branch.

Measured on the exec-model branch before the card (2026-09-30): each
condition lowers wholly into separate rule items (the fold's accumulate and
its test), which left the Berging's pattern without a test, and a pattern
without a test reacts to no update of the Berging. Each rule fired once,
when its fold first held, on operands still empty, and did not fire again:

    FAIL case1-twee-gevonden-vondsten.json: attribuut sluitdag: verwacht 32, werkelijk 1; attribuut afrekendag: verwacht 36, werkelijk leeg; attribuut controledag: verwacht 46, werkelijk 0 | case2-een-vondst-zonder-vinddag.json: attribuut controledag: verwacht 36, werkelijk 0 | case3-geen-vondsten.json: attribuut afrekendag: verwacht 5, werkelijk leeg

The branch now gives such a pattern the opaque always-true test that keeps
it reactive to the Berging, as a test on the subject would have.

Not in this golden, with its reason: the maximum or minimum of two scalar
operands (the card's R1; the ASP backend does not translate that value), an
operand written by a defaults group (the DRL-text branch refuses a document
with a group), an aggregation comparison whose own threshold is derived
later and a role check stored on the other end (the same fix; the DRL-text
branch answers the first wrong). Each is a row of
`KwantorvoorwaardeOperandWitnessTest` in the Drools backend.
