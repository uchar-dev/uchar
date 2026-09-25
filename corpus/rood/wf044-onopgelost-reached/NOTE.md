# wf044-onopgelost-reached

The unsolved-state guard (WF-044): an `Unresolved` node is an honest
inexpressibility marker, and it must never vanish silently. This
document is verifier-clean — the marker itself is legal wire content —
but the rule that carries it is *reached* by the case: the reference
interpreter faults the evaluation, and the ASP backend faults the
translation, both citing WF-044. A harness in which this case stops
failing has started guessing where the spec says "cannot express".

**WF-113 (since M1, 2026-09-23).** The document carries one `warning` in
`diagnostics` whose message contains the marker's source snippet verbatim, in
the frontend's message form (`onopgelost: <reason> — '<bron>' blijft als marker
op de wire staan`): every marker on the wire is accounted for by a diagnostic,
and this document is one of the first corpus carriers of that clause (WF-113
(b)). `line` is 0 because no source file carries the marker's phrase; 0 is the
value the RegelSpraak frontend writes for a marker without a source position.
The case and its `expectedFault` are unchanged: the diagnostic carries no
semantics (WF-113 (c)).
