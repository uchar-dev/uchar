# jutterswet-gevuldegemiddelde

The hand-spelled average, and the guard that makes it safe
(`RUNTIME-SEMANTICS.md` ch. 3.4c, "What this does NOT give"; maintainer
decision G1 of 2026-09-19). There is no average fold on the wire: the
author writes `SOM ÷ AANTAL_GEVULDE` and owns the division by zero on a
wholly empty source. This golden pins that the escape from that division
is WRITABLE and that it actually works.

| jutter | waarderingen | aantal gevulde | vondstwaarde | gemiddelde |
|---|---|---|---|---|
| J1 | 4, 6, leeg | **2** | 10 | **5** |
| J2 | (no vondsten) | **0** | 0 | **leeg** |
| J3 | leeg | **0** | 0 | **leeg** |

The third rule carries the condition "het aantal gewaardeerde vondsten van
de Jutter groter is dan 0". A filled count is an ordinary expression, so
it may stand there — and it is the ONLY count that can. `AANTAL` is 1 for
J3, a jutter with one vondst and no value, so a guard written with the
instance count would let the division through and fault. That is the
difference the two counts make, in the position where it costs something.

Row J1 is the arithmetic the wire could not get right before this kind
existed: with `AANTAL` as the divisor it is 10 ÷ 3, a silently wrong
answer rather than a loud one.

The `vondstwaarde` column is pinned on ALL THREE jutters, not just on the
one where it is interesting. J2 and J3 are there because the som declares
`of 0 als die er niet zijn`, and that fallback is half of the arithmetic
the guard exists for: it is what makes the unguarded division 0 ÷ 0
rather than leeg ÷ 0. Pinning the numerator only where it is non-zero
would leave the reader to take the fallback on trust and would let an
executor that answered leeg there pass.

The red twin `corpus/rood/aantalgevulde-ongewaakte-deling` is this same
document with the condition removed. It faults in all three executors, so
what the guard prevents is pinned as well as the guard itself — and what
decision G1 leaves to the rule author is a LOUD failure, not a quiet
wrong number.

No `bron.rgs`: the kind is wire-first and no frontend spells it.
