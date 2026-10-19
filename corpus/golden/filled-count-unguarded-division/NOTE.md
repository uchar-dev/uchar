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
division is 0 ÷ 0: a filled dividend over a numeric zero. Since 2026-10-19
(card `zero-division-channel`, `RUNTIME-SEMANTICS.md` ch. 1.4 point 4) its
quotient is leeg and the rule `gemiddelde vondstwaarde` deposits a §9.5 fact
of kind EF-005 on J2; the case pins `aantal gewaardeerde vondsten` 0,
`vondstwaarde` 0, `gemiddelde vondstwaarde` leeg and that fact.

This is the point of the decision rather than a defect of it: the wire
declines to invent an answer, and the rule author is given a guard that
is WRITABLE — a filled count is an ordinary expression, so it may stand
in a rule's condition, where `count` could not serve because it reads 1
for a member with no value. The golden twin pins that the guard works;
this case pins that without it the failure is observable. Until 2026-10-19
it was a red case and the failure was a fault in all three executors; it is
a channel fact since, still never a silent value.
