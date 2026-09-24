Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) at its second write position
on the FAMILY clause: the object creation initialises `vaarwater`
(declared `Enumeratie` `Vaarwater`) with the numeric `Literal` `4`.

    WF-023 FAIL /rules/objectCreations/0/versions/0/initialisations/0: a number
    value is assigned to a enumeration target

Why it has to exist: the object-creation member had NO value typing at all
— WF-027 checks existence (and the characteristic value's boolean-ness),
WF-023 lived on the rule target only. Verifier-clean before; ref faulted
`match scrutinee is 4, not a constructor value` at the faithful reader;
clingo faulted the translation (`unscalable datatype`, the pre-existing
enumeration-member fence). The soort layer of the write side, pinned
separately from the domain layer so the family check at this position has
its own witness.

Falsifiable: drop the object-creation position from `wf023` and this case
passes.
