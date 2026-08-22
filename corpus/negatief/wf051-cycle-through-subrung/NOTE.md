Violates WF-051 (`spec/IR-SPEC.md` ch. 6.1): a rung inside the exception
sub-group reads the ladder's own target `Jutter.heffing` in its
justification — `dep_forms` folds every descendant read (justifications
and consequences, recursively through sub-rungs) into the root ladder's
negative edges, so the read closes a dependency cycle through the ladder:
no strata exist.

A minimal hand-built document: a group-rung whose single member chain
holds a rung whose justification is `GEVULD(zijn heffing)` and whose
consequence is 10.00.
