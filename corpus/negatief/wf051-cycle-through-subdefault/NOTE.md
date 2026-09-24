Violates WF-051 (`spec/IR-SPEC.md` ch. 6.1): a default inside the
exception group reads the defaults' own target `Jutter.heffing` in its
justification — `dep_forms` folds every descendant read (justifications
and consequences, recursively through nested defaults) into the root
group's negative edges, so the read closes a dependency cycle through the
defaults: no strata exist.

A minimal hand-built document: a group default whose single member chain
holds a default whose justification is `filled(zijn heffing)` and whose
consequence is 10.00.
