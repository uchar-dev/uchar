# jutterswet-late-vondst — a fold whose subselection reads through a role of the element

A Jutter has many vondsten (`JutterHeeftVondst`), and each Vondst one
jutter. Two rules on the Jutter fold over its vondsten with the same
subselection, which reads the sluitdag of the jutter of each Vondst:

| rule on the Jutter | value |
|---|---|
| eerste late vinddag | the earliest vinddag of the vondsten whose vinddag is later than the sluitdag of the jutter of the Vondst |
| aantal late vondsten | the number of those vondsten |
| sluitdag | its startdag |

The subselection is evaluated per vondst, with the vondst as its subject
(RUNTIME-SEMANTICS ch. 3.4a), so "de sluitdag van de jutter van de Vondst"
is a navigated read over the role `jutter`. The sluitdag is derived by a
rule, which `source.rgs` declares after the two folds.

## Hand computation

"Later dan" is strict, and a comparison with a leeg operand is onwaar
(RUNTIME-SEMANTICS ch. 1.2), so a vondst whose jutter has no sluitdag is
not in the subselection. An ordering fold over an empty source is leeg
(ch. 1.2e); a count is never leeg, and over an empty source it is 0.

`case1-drie-vondsten-rond-de-sluitdag.json`: J has startdag 01-03-2026,
so its sluitdag is **01-03-2026**. V1 (vinddag 15-02-2026) is not later,
V2 (10-03-2026) and V3 (20-03-2026) are. The eerste late vinddag of J is
**10-03-2026** and the aantal late vondsten **2**.

`case2-geen-late-vondst.json`:

- J2 has startdag 01-04-2026, sluitdag **01-04-2026**. W1 (15-03-2026) is
  not later, and W2 (01-04-2026) is equal, so not later: eerste late
  vinddag **leeg**, aantal late vondsten **0**.
- J3 has no startdag, so its sluitdag is **leeg** and W3 (10-03-2026) is
  not later than it: eerste late vinddag **leeg**, aantal **0**.
- J4 has startdag 01-03-2026 and no vondsten: sluitdag **01-03-2026**,
  eerste late vinddag **leeg**, aantal **0**.

## Executors

ref and clingo replay both cases. The Drools exec-model branch joins, in
the source of each accumulate, the object the role of the vondst reaches,
and tests the subselection on the vondst bound again after that join, so
the sluitdag written after the vondsten were inserted is seen; it plays
both cases. Before that (measured 2026-10-01) it tested the subselection
on the pattern of the vondst, which an update of the jutter does not run
again, and case 1 failed with the eerste late vinddag leeg and the count
0. The DRL-text branch refuses the document on its marker `TODO
aggregatie EERSTE`; behind it the count is refused by name, because that
branch writes such a subselection into the pattern of the vondst. Every
case refutes the do-nothing executor. The IR from `source.rgs` alone is
byte-identical to the one written beside the cases.
