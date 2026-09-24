Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a group default — `exceptions` present, no own decision or
consequence — carries a `justification` of its own. A group default's
exception groups decide; its own condition is never read, so a
`justification` here would be silently ignored. This is the
fail-fast gap the rule closes: such a default is neither a well-formed
form-a default (which needs its own decision/consequence) nor a
well-formed group default (which carries no own content), so it is
rejected.

A minimal hand-built document: one set of defaults on `Jutter.heffing`
whose sole default's `exceptions` holds one valid exception group
(`grondY -> 10.00`), no own decision or consequence, and a stray
`justification` (`grondX`).
