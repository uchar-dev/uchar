Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a group default — an exception group with no own decision or
consequence — carries a justification (`voorwaarde`) of its own. A group
default's members decide; its own condition is never read, so a
non-null `voorwaarde` here would be silently ignored. This is the
fail-fast gap the rule closes: such a default is neither a well-formed
form-a default (which needs its own besluit/consequentie) nor a
well-formed group default (which carries no own content), so it is
rejected.

A minimal hand-built document: one set of defaults on `Jutter.heffing`
whose sole default holds a valid single-member exception group
(`grondY -> 10.00`), no own besluit or consequentie, and a stray
`voorwaarde` (`grondX`).
