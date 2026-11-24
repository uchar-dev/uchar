# jutterswet-zelfde-conclusie-waarde — a stack on an attribute with an open default that can decide only the same

Four stacks on an attribute in a document with `Afleiden uit leeg: nee` (`inferFromEmpty: false`), read as
RUNTIME-SEMANTICS ch. 1.5 lays down ("A stack", "An open default with a constant"). Each has the base "zijn gewicht
maal 2" and deviations above it (`Afwijking van`: the later-written block ranks higher, and each keeps its own
condition). Own Jutterswet-flavoured casus, rights-free.

| target | highest | then | then | base |
|---|---|---|---|---|
| vindloon | "op 0 indien hij betwist is" | "op 0 indien hij beschadigd is" | — | "zijn gewicht maal 2" |
| strandloon | "op 0 indien hij betwist is" | "op 5 indien hij beschadigd is" | — | "zijn gewicht maal 2" |
| wrakloon | "zijn gewicht maal 0 indien hij betwist is" | "op 0 indien hij beschadigd is" | — | "zijn gewicht maal 2" |
| kustloon | "op 0 indien hij betwist is" | "op 7 indien hij gemeld is" | "op 0 indien hij beschadigd is" | "zijn gewicht maal 2" |

`betwist`, `beschadigd` and `gemeld` are input characteristics, none declared complete: named in `characteristics` a
characteristic holds, in `characteristicsAbsent` it stands not to hold, elsewhere it is open.

The reading. An open default whose consequence is a `Literal` can decide that value or nothing: it is *open with* the
value. Below it the stack is read on, provisionally: a lower default that decides the same value decides the target; one
that decides another value, or is open with another, makes the stack open; one that gives way passes on to the next.
A default whose value is computed is open with any value, also where its value would be 0 (the wrakloon): the reading
takes the value from a constant alone. On an attribute a stack open with a value leaves the target open.

## Cases (hand-computed, absolute)

Every case has gewicht 50, so the base decides 100. "open" is an `attributeOpen` expectation (the scenario's "staat
niet vast"); "—" is not expected in that case.

| case | beschadigd | gemeld | betwist | vindloon | strandloon | wrakloon | kustloon |
|---|---|---|---|---|---|---|---|
| case1-betwist-open | holds | not | open | 0 | open | open | 0 |
| case2-betwist-waar | holds | not | holds | 0 | 0 | 0 | 0 |
| case3-betwist-niet | holds | not | not | 0 | 5 | 0 | 0 |
| case4-beschadigd-niet | not | not | open | open | open | open | open |
| case5-beschadigd-niet-betwist-niet | not | not | not | 100 | 100 | 100 | 100 |
| case6-gemeld-open | holds | open | open | 0 | — | — | open |
| case7-gemeld-waar | holds | holds | open | 0 | — | — | open |
| case8-gemeld-waar-betwist-niet | holds | holds | not | 0 | — | — | 7 |

vindloon, case1: the highest default is open with 0; the next decides 0, the same: the vindloon is 0, as in case2
(the highest decides 0) and case3 (the next decides 0). The old reading, which stopped at the first open default, left
it open. Case4: the next gives way, the base decides 100, another value: open, 0 or 100.

strandloon, case1: the next default decides 5, another value than the open 0: open, 0 or 5 (case2 and case3).

wrakloon, case1: the highest default computes "zijn gewicht maal 0"; open, it can decide any value, so the stack is
open, though case2 and case3 both give 0.

kustloon: "op 7 indien hij gemeld is" stands between the two defaults of 0. In case1 it gives way (gemeld stands not)
and does not stand in the way: 0. In case6 it is open with 7 and in case7 it decides 7, another value than the open 0:
open. In case8 betwist stands not to hold and it decides 7.

No case deposits a §9.5 fact. Gate 5 holds every case against the run with each given input left out
(monotonicity): case2 and case3 without betwist are case1, case8 without betwist is case7.

The readings of RUNTIME-SEMANTICS ch. 5.12 on case1 (`WV-5.12-same-conclusion`): the highest default of the vindloon
`OPEN`, the default for beschadigd `HOLDS` (it decides the vindloon), the base `DISPLACED`; on case4 the default for
beschadigd `FAILS` and the base `HOLDS`, which leaves the stack open.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.
