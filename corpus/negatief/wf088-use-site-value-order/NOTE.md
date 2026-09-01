Violates WF-088 (`spec/IR-SPEC.md` ch. 2.4) on its ORDER clause: the
`keurstatus` attribute use site carries the declared values — but in the
other order (`Beschadigd`, `Gaaf` where the declaration has `Gaaf`,
`Beschadigd`). Enumeration value order is normative and preserved
(WF-006), so a reordered copy is drift like any other; set equality is
not agreement, exactly as it is not for a `SumType` use site (WF-061).

This case exists because its sibling `wf088-use-site-value-drift` differs
from the declaration by a value: a verifier weakened to a sorted
comparison still rejected that one, so the mutant survived gate 3.
Against this document it does not.

A minimal hand-built document.
