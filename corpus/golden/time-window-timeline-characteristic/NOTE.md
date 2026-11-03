# time-window-timeline-characteristic

`RUNTIME-SEMANTICS.md` ch. 3.10 decides on the rule's **target**. A scalar
target gets the evaluation-date gate. A time-dependent target gets the
clip: the window says which days of the timeline are written. The target
here is a time-dependent **characteristic**, `geborgen` for every year, and
the rule "Een Vondst is geborgen van 1 januari 2024 tot 1 januari 2025"
writes it: the characteristic holds on [2024-01-01, 2025-01-01) and nowhere
else (ch. 3.11), whatever the evaluation date.

A characteristic declares its timeline on its own `Characteristic` node
(`timeline`), not on an `Attribute`, and an executor that asks only the
attribute table sees a scalar target where the document has a timeline.
That happened: on 2026-09-20 the ASP backend played the scalar gate on this
document, present at an evaluation date inside the window and absent
outside it. The two cases pin that it is the timeline: one evaluation date
inside the window and one after it, with the same expectation.

The characteristic is observed through `bergdagen`, the day count of
`geborgen` for every year (ch. 3.8): 366 in 2024, a leap year, and 0 in
every other year, before 2024 and from 2025 on as one open segment each.

Until card `conditions-per-period` this document stood in `corpus/red`,
with the one case `case1`: no executor wrote a time-dependent
characteristic, and `ref` and the ASP backend refused it with EF-023, both
Drools branches with DROOLS-102. Since then `ref` and the ASP backend write
it; both Drools branches write it since step B part ii.
