Violates WF-050 (`spec/IR-SPEC.md` ch. 6.1): a default inside an
exception group of attribute defaults carries a `besluit` — the
default-shape checks recurse into nested defaults (an exception group
inherits the root target kind), so a boolean decision belongs only on
characteristic defaults, never on attribute ones.

A minimal hand-built document: a form-a default (`grondX` → 40.00) on
attribute defaults whose exception group holds a member chain with a
characteristic-style `besluit` default.
