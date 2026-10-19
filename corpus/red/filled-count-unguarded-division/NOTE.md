# filled-count-unguarded-division

The cost maintainer decision G1 of 2026-09-19 accepted, made observable.
There is no average fold: `sum ÷ filledCount` is hand-spelled, and
over a source with no filled values the divisor is a real numeric zero.
This document is `corpus/golden/jutterswet-gevuldegemiddelde` with ONE
difference — the third rule's condition is gone — replayed on the jutter
who has no vondsten at all.

Both halves of that arithmetic are the filled count's own doing and
neither is an accident: the som falls back to 0 because it declares
`of 0 als die er niet zijn`, and the count is 0 because a count over an
empty collection is a cardinality, never leeg (ch. 1.2e, ch. 1.3). So the
division is 0 ÷ 0 and every executor refuses it. The same arithmetic
holds for a jutter whose only vondst carries no waardering, which is the
row the guarded twin pins at leeg.

This is the point of the decision rather than a defect of it: the wire
declines to invent an answer, and the rule author is given a guard that
is WRITABLE — a filled count is an ordinary expression, so it may stand
in a rule's condition, where `count` could not serve because it reads 1
for a member with no value. The golden twin pins that the guard works;
this case pins that without it the failure is LOUD. Stil verkeerd wordt
luid fout, and that trade is the whole of G1.

All THREE executors fault here and each carries its own pinned fragment —
ref and clingo in the arithmetic, the Drools runtime in the consequence.
A red case that three executors reach is rare: most red forms are refused
one phase earlier by at least one of them.
