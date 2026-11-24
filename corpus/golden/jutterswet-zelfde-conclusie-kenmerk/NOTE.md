# jutterswet-zelfde-conclusie-kenmerk — a stack on a characteristic with an open default that can decide only the same

Four stacks on a characteristic in a document with `Afleiden uit leeg: nee` (`inferFromEmpty: false`), read as
RUNTIME-SEMANTICS ch. 1.5 lays down ("A stack", "An open default with a constant", "A stack on a characteristic",
"Exception groups and a declared-unordered group"). Own Jutterswet-flavoured casus, rights-free.

- `beloonbaar`: the base `beloonbaarheid`, "beloonbaar indien zijn gewicht groter is dan 2", with above it the deviation
  `drijfhout niet beloonbaar`, "niet beloonbaar indien hij drijfhout is", and above that `betwist niet beloonbaar`,
  "niet beloonbaar indien hij betwist is" (`Afwijking van`: the later-written block ranks higher, and each keeps its own
  condition);
- `vindbaar`: the base `vindbaarheid`, "vindbaar indien zijn gewicht groter is dan 2", with above it `gemeld vindbaar`,
  "vindbaar indien hij gemeld is";
- `wachtpremie`: "op 3 indien hij niet beloonbaar is", which reads the characteristic;
- `bergbaar`: the base `bergbaarheid`, "bergbaar indien zijn gewicht groter is dan 2", with `Uitzonderingen ongeordend
  op bergbaarheid:` the two exception groups "niet bergbaar indien hij betwist is" and "niet bergbaar indien hij
  drijfhout is" — a group-default directly above the base; the exceptions are restrictive, so their justifications are
  "zijn gewicht groter is dan 2 en hij betwist is" and "… en hij drijfhout is";
- `keurbaar`: a declared-unordered group (`Uitzonderingen ongeordend:`) of the two groups "keurbaar indien zijn gewicht
  groter is dan 2" and "niet keurbaar indien hij betwist is".

`betwist`, `drijfhout` and `gemeld` are input characteristics, none declared complete: where the case names one in
`characteristics` it holds, in `characteristicsAbsent` it stands not to hold, and elsewhere it is open.

The reading. A default on a characteristic decides a constant, to attribute or not, so a default whose justification is
undetermined can decide only that: it is *open with* its decision. Below it the stack is read on, provisionally: a lower
default that decides the same decides the characteristic, one that decides the other makes the stack open. At the root
of a characteristic a stack open with not attributing stands not to hold (Kleene: ¬betwist ∧ R is false where R is
false); a stack open with attributing stays open (betwist ∨ false is betwist). That closing is the root's alone: in an
exception group and in a member of a declared-unordered group, open with not attributing stays so, because there giving
way and not attributing differ (`bergbaar` and `keurbaar` pin it).

## Cases (hand-computed, absolute)

"not" is a `characteristicAbsent` expectation, "open" a `characteristicOpen` or `attributeOpen` (the scenario's "staat
niet vast").

| case | gewicht | drijfhout | betwist | gemeld | beloonbaar | wachtpremie | vindbaar | bergbaar | keurbaar | §9.5 |
|---|---|---|---|---|---|---|---|---|---|---|
| case1-betwist-open | 5 | holds | open | open | not | 3 | holds | not | open | none |
| case2-betwist-waar | 5 | holds | holds | open | not | 3 | holds | not | not | `Vondst.keurbaar` V1 |
| case3-betwist-niet | 5 | holds | not | open | not | 3 | holds | not | holds | none |
| case4-drijfhout-niet-betwist-open | 5 | not | open | open | open | open | holds | open | open | none |
| case5-wortel-betwist-open | 1 | not | open | open | not | 3 | open | not | not | none |
| case6-wortel-betwist-waar | 1 | not | holds | open | not | 3 | open | not | not | none |
| case7-wortel-betwist-niet | 1 | not | not | open | not | 3 | open | not | not | none |
| case8-gemeld-waar | 1 | not | not | holds | not | 3 | holds | not | not | none |
| case9-gemeld-niet | 1 | not | not | not | not | 3 | not | not | not | none |

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
open.

bergbaar: in case1 the group for betwist is undetermined, open with not attributing, and the group for drijfhout decides
not to attribute: the groups decide it, and bergbaar stands not to hold; case2 and case3 give the same. Case4: the group
for drijfhout gives way (drijfhout stands not), so the groups are open with not attributing, above the base, which
attributes at gewicht 5: another decision, so the stack is open. Had the exception group closed to giving way as the
root does, the base would decide and bergbaar would hold. Case5 to case9: at gewicht 1 both exceptions are established
false ("zijn gewicht groter is dan 2" is false), and so is the base: bergbaar stands not to hold.

keurbaar: in case1 and case4 the group "niet keurbaar indien hij betwist is" is open with not attributing, beside the
group that attributes at gewicht 5: the member that attributes decides only where every other member decides the same
or can decide only the same, which this one cannot, so keurbaar is open. Had the member closed to giving way, keurbaar
would hold. Case2: the two groups decide differently, the conflict of ch. 7.2, keyed by the target form,
`Vondst.keurbaar`; no write, so keurbaar stands not to hold. Case3: only the first decides: it holds. Case5: at gewicht
1 the first group gives way, the second is open with not attributing: the group is open with not attributing, and at
the root keurbaar stands not to hold; case6 and case7 give the same with betwist given.

Gate 5 holds every case against the run with each given input left out (monotonicity): case2, case3, case6 and case7
without betwist are case1 and case5.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.
