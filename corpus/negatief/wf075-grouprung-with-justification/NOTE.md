Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a group-rung — an exception sub-group with no own decision or
consequence — carries a justification (`voorwaarde`) of its own. A
group-rung's members decide; its own condition is never read, so a
non-null `voorwaarde` here would be silently ignored. This is the
fail-fast gap the rule closes: such a rung is neither a well-formed
form-a rung (which needs its own besluit/consequentie) nor a well-formed
group-rung (which carries no own content), so it is rejected.

A minimal hand-built document: one ladder on `Jutter.heffing` whose sole
rung holds a valid single-member sub-group (`grondY -> 10.00`), no own
besluit or consequentie, and a stray `voorwaarde` (`grondX`).
