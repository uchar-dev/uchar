# jutterswet-zelfde-conclusie-ongeordend — a declared-unordered group with a member that can decide only the same

Three declared-unordered groups (`Uitzonderingen ongeordend:`, ch. 7.2) in a document with `Afleiden uit leeg: nee`
(`inferFromEmpty: false`), read as RUNTIME-SEMANTICS ch. 1.5 lays down ("Exception groups and a declared-unordered
group", "An open default with a constant"). Own Jutterswet-flavoured casus, rights-free.

- `bergloon`: "op 0 indien hij betwist is" and "op 0 indien hij beschadigd is";
- `meldloon`: "op 0 indien hij betwist is" and "op 5 indien hij beschadigd is";
- `havenloon`: "op 0 indien hij betwist is", "op 0 indien hij beschadigd is" and "op 5 indien hij gemeld is".

`betwist`, `beschadigd` and `gemeld` are input characteristics, none declared complete: named in `characteristics` a
characteristic holds, in `characteristicsAbsent` it stands not to hold, elsewhere it is open.

The reading. Each member is a chain of one default; one whose justification is undetermined is *open with* its
`Literal`. Two members that decide differently conflict (ch. 7.2: no write, the §9.5 fact under the bare target form).
Otherwise, beside a member that decides v, the group decides v where every open member is open with v — such a member
cannot make a conflict — and is open where one is open with another value. Where no member decides and every open
member is open with v, the group is open with v, and on an attribute the target is open.

## Cases (hand-computed, absolute)

"open" is an `attributeOpen` expectation (the scenario's "staat niet vast"), "leeg" an `attributeEmpty`; "—" is not
expected in that case.

| case | beschadigd | gemeld | betwist | bergloon | meldloon | havenloon | §9.5 |
|---|---|---|---|---|---|---|---|
| case1-betwist-open | holds | not | open | 0 | open | 0 | none |
| case2-betwist-waar | holds | not | holds | 0 | leeg | 0 | `Vondst.meldloon` V1 |
| case3-betwist-niet | holds | not | not | 0 | 5 | 0 | none |
| case4-beschadigd-niet | not | not | open | open | open | open | none |
| case5-gemeld-open | holds | open | open | 0 | — | open | none |
| case6-gemeld-waar | holds | holds | open | 0 | — | leeg | `Vondst.havenloon` V1 |

bergloon, case1: the member for betwist is open with 0 and the member for beschadigd decides 0: the bergloon is 0, as in
case2 (both decide 0) and case3 (only the second). The old reading, where a member that is open kept the group open,
left it open. Case4: no member decides, the first is open with 0: open, 0 or settled leeg.

meldloon, case1: the member for betwist is open with 0 beside a member that decides 5, and could make the conflict of
case2: open.

havenloon, case1: the member for gemeld gives way, and the group is the bergloon's: 0. Case5: the member for gemeld is
open with 5, another value than the 0 of the member for beschadigd: open. Case6: the member for gemeld decides 5 beside
the 0 of the member for beschadigd, a conflict whatever betwist: no write, the fact `Vondst.havenloon`.

Gate 5 holds every case against the run with each given input left out (monotonicity): case2 and case3 without
betwist are case1, case6 without gemeld is case5. A case that leaves every characteristic not holding is left out: its
expectations, all settled leeg, would hold on the raw input as well (gate 7).

`ir.json` is the frontend's emission of `source.rgs` (gate 10); the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.
