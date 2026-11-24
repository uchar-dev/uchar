# jutterswet-zelfde-conclusie-genest — exception groups with a member that can decide only the same

Four stacks with exception groups (ch. 7.9) in a document with `Afleiden uit leeg: nee` (`inferFromEmpty: false`),
read as RUNTIME-SEMANTICS ch. 1.5 lays down ("Exception groups and a declared-unordered group", "An open default with
a constant"). Each has the base "zijn gewicht maal 2". Own Jutterswet-flavoured casus, rights-free.

- `vindloon`: `Uitzonderingen ongeordend op vindloon:` with the members "op 0 indien hij betwist is" and "op 0 indien hij
  beschadigd is" — a group-default with two exception groups, directly above the base;
- `strandloon`: `Afwijking van strandloon:` "op 0 indien hij beschadigd is", with `Uitzondering op` it "op 0 indien hij
  betwist is" — a default whose one exception group decides above its own content, the fall-back; the exception is
  restrictive, so its justification is "hij is beschadigd en hij is betwist";
- `bergloon`: `Uitzonderingen ongeordend op bergloon:` with the members "op 5" and "op 0 indien hij beschadigd is", and
  `Uitzondering op` the first "op 0 indien hij betwist is" — the first member is the chain "0 indien hij betwist is"
  above "5", whose outcome counts whole;
- `havenloon`: the chain of `Afwijking van havenloon:` "op 0 indien hij betwist is", above `Afwijking van havenloon:`
  "op 7 indien hij beschadigd is" with `Uitzonderingen ongeordend op` it the members "op 0 indien zijn gewicht groter is
  dan 2" and "op 5 indien zijn gewicht groter is dan 10" (restrictive: "hij is beschadigd en zijn gewicht …"), above
  the base. Where beschadigd holds, the two exception groups decide 0 and 5: a nested conflict, keyed
  `Vondst.havenloon/1` (the default at index 1 of the chain).

`betwist` and `beschadigd` are input characteristics, none declared complete: named in `characteristics` a
characteristic holds, in `characteristicsAbsent` it stands not to hold, elsewhere it is open.

The reading. A default whose justification is undetermined and whose consequence is a `Literal` is *open with* that
value. Exception groups reconcile as a declared-unordered group: beside a member that decides v, every member open with v
lets the groups decide v; the outcome stands above the fall-back, or above the rest of the chain, as a default does:
open with v above a fall-back that decides v decides v. A member counts with its whole chain: the chain "0 indien hij
betwist is" above "5" is open (it can decide 0 or 5), not open with 0. Below a default open with a value the rest of
the chain is read provisionally (ch. 1.5): a conflict there is not deposited as a §9.5 fact; it can decide another value
than the open default, so the chain is open.

## Cases (hand-computed, absolute)

Every case has gewicht 50, so the base decides 100. "open" is an `attributeOpen` expectation (the scenario's "staat
niet vast"), "leeg" an `attributeEmpty`.

| case | beschadigd | betwist | vindloon | strandloon | bergloon | havenloon | §9.5 |
|---|---|---|---|---|---|---|---|
| case1-betwist-open | holds | open | 0 | 0 | open | open | none |
| case2-betwist-waar | holds | holds | 0 | 0 | 0 | 0 | none |
| case3-betwist-niet | holds | not | 0 | 0 | leeg | leeg | `Vondst.bergloon/0` V1, `Vondst.havenloon/1` V1 |
| case4-beschadigd-niet | not | open | open | 100 | open | open | none |
| case5-beschadigd-niet-betwist-niet | not | not | 100 | 100 | 5 | 100 | none |

vindloon, case1: the exception group for betwist is open with 0, the one for beschadigd decides 0: the groups decide
0, and the vindloon is 0. Case4: the second gives way, so the groups are open with 0; the base below decides 100,
another value: open. The old reading left case1 open.

strandloon, case1: the exception "beschadigd en betwist" is undetermined, open with 0; the fall-back "op 0 indien hij
beschadigd is" decides 0: the strandloon is 0. Case4: the exception is established false (beschadigd stands not), the
fall-back gives way, the base decides 100. The old reading left case1 open.

bergloon, case1: the first member is the chain "0 indien hij betwist is" (open with 0) above "5", which decides 5:
another value, so the member is open, and the groups with it, beside the 0 of the second member. Case2: the first member
decides 0, the second 0: 0. Case3: the first member decides 5, the second 0: the conflict of its exception groups, keyed
by the path of the group-default, `Vondst.bergloon/0`; no write. Case4: the first member is open, the second gives way:
open. Case5: the first member decides 5, the second gives way: 5.

havenloon, case1: "op 0 indien hij betwist is" is undetermined, open with 0; below it the default for beschadigd is
read provisionally: its exception groups decide 0 and 5, a conflict, which is not deposited, and which makes the chain
open (the conflict is another outcome than 0). So the havenloon is open and no §9.5 fact is written. Case2: the highest
default decides 0, and nothing below it is read. Case3: the highest gives way, the conflict of the exception groups
stands: no write, the fact `Vondst.havenloon/1`. Case4: the highest is open with 0; the exception groups give way
(beschadigd stands not), the fall-back "op 7 indien hij beschadigd is" gives way, the base decides 100: another value,
open. Case5: the base decides 100.

Gate 5 holds every case against the run with each given input left out (monotonicity): case2 and case3 without betwist
are case1, case5 without betwist is case4.

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.
