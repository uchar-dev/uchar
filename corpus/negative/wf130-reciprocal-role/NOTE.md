Violates WF-130 (`spec/IR-SPEC.md` ch. 2.6), the arm of a reciprocal fact type (`WF-130.reciprocal-role`). The
reciprocal fact type `maatschap` (`reciprocal: true`) declares its role `maat` (`many: false`) with `complete: true`:
only a to-one role of a non-reciprocal fact type may declare its input complete (`RUNTIME-SEMANTICS.md` ch. 1.5).
