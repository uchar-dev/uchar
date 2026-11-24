# jutterswet-zelfde-conclusie-kenmerk — a stack on a characteristic with an open default that can decide only the same

Two stacks on a characteristic in a document with `Afleiden uit leeg: nee` (`inferFromEmpty: false`), read as
RUNTIME-SEMANTICS ch. 1.5 lays down ("A stack", "An open default with a constant", "A stack on a characteristic"). Own
Jutterswet-flavoured casus, rights-free.

- `beloonbaar`: the base `beloonbaarheid`, "beloonbaar indien zijn gewicht groter is dan 2", with above it the deviation
  `drijfhout niet beloonbaar`, "niet beloonbaar indien hij drijfhout is", and above that `betwist niet beloonbaar`,
  "niet beloonbaar indien hij betwist is" (`Afwijking van`: the later-written block ranks higher, and each keeps its own
  condition);
- `vindbaar`: the base `vindbaarheid`, "vindbaar indien zijn gewicht groter is dan 2", with above it `gemeld vindbaar`,
  "vindbaar indien hij gemeld is";
- `wachtpremie`: "op 3 indien hij niet beloonbaar is", which reads the characteristic.

`betwist`, `drijfhout` and `gemeld` are input characteristics, none declared complete: where the case names one in
`characteristics` it holds, in `characteristicsAbsent` it stands not to hold, and elsewhere it is open.

The reading. A default on a characteristic decides a constant, to attribute or not, so a default whose justification is
undetermined can decide only that: it is *open with* its decision. Below it the stack is read on, provisionally: a lower
default that decides the same decides the characteristic, one that decides the other makes the stack open. At the root
of a characteristic a stack open with not attributing stands not to hold (Kleene: ¬betwist ∧ R is false where R is
false); a stack open with attributing stays open (betwist ∨ false is betwist).

## Cases (hand-computed, absolute)

"not" is a `characteristicAbsent` expectation, "open" a `characteristicOpen` or `attributeOpen` (the scenario's "staat
niet vast").

| case | gewicht | drijfhout | betwist | gemeld | beloonbaar | wachtpremie | vindbaar |
|---|---|---|---|---|---|---|---|
| case1-betwist-open | 5 | holds | open | open | not | 3 | holds |
| case2-betwist-waar | 5 | holds | holds | open | not | 3 | holds |
| case3-betwist-niet | 5 | holds | not | open | not | 3 | holds |
| case4-drijfhout-niet-betwist-open | 5 | not | open | open | open | open | holds |
| case5-wortel-betwist-open | 1 | not | open | open | not | 3 | open |
| case6-wortel-betwist-waar | 1 | not | holds | open | not | 3 | open |
| case7-wortel-betwist-niet | 1 | not | not | open | not | 3 | open |
| case8-gemeld-waar | 1 | not | not | holds | not | 3 | holds |
| case9-gemeld-niet | 1 | not | not | not | not | 3 | not |

beloonbaar, case1: `betwist niet beloonbaar` is undetermined, open with not attributing; `drijfhout niet beloonbaar`
decides not to attribute: the same, so the stack decides it, and beloonbaar stands not to hold. Case2 and case3 give the
same: where betwist holds the highest default decides not to attribute, where it stands not to hold the second does.
The old reading, which stopped at the first open default, left beloonbaar and the wachtpremie open in case1.

Case4: the second default gives way (drijfhout stands not), and the base decides to attribute at gewicht 5: another
decision than the open default's, so the stack is open, and so is the wachtpremie, which reads it.

Case5: the second default and the base give way (gewicht 1), so the stack is open with not attributing, and at the
root beloonbaar stands not to hold; case6 and case7 give the same with betwist given. The old reading left it open.

vindbaar: in case1 to case4 `gemeld vindbaar` is open with attributing and the base attributes at gewicht 5: vindbaar
holds. In case5 to case7 the base gives way at gewicht 1, the stack is open with attributing, and vindbaar is open; case8
and case9 give gemeld, and vindbaar holds and stands not to hold.

The wachtpremie is 3 where beloonbaar stands not to hold ("niet beloonbaar" established true) and open where it is
open. No case deposits a §9.5 fact. Gate 5 holds every case against the run with each given input left out
(monotonicity): case2, case3, case6 and case7 without betwist are case1 and case5.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.
