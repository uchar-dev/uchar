Violates WF-050 (`spec/IR-SPEC.md` ch. 6.1): a rung inside an
attribute-ladder exception sub-group carries a `besluit` — the rung-shape
checks recurse into sub-rungs (a sub-ladder inherits the root target
kind), so a boolean decision belongs only on a characteristic ladder,
never on an attribute one.

A minimal hand-built document: a form-a rung (`grondX` → 40.00) on an
attribute ladder whose sub-group holds a member chain with a
characteristic-style `besluit` rung.
