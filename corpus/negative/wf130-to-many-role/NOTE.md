Violates WF-130 (`spec/IR-SPEC.md` ch. 2.6), the arm of a to-many role (`WF-130.to-many-role`). In the non-reciprocal
fact type `vangst` between `Jutter` and `Vondst`, the to-many role `vondst` (`many: true`) carries `complete: true`:
only a to-one role of a non-reciprocal fact type may declare its input complete (`RUNTIME-SEMANTICS.md` ch. 1.5).
