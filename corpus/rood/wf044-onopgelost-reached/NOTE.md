# wf044-onopgelost-reached

The unsolved-state guard (WF-044): an `Onopgelost` node is an honest
inexpressibility marker, and it must never vanish silently. This
document is verifier-clean — the marker itself is legal wire content —
but the rule that carries it is *reached* by the case: the reference
interpreter faults the evaluation, and the ASP backend faults the
translation, both citing WF-044. A harness in which this case stops
failing has started guessing where the spec says "cannot express".
