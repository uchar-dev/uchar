Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) at its third write position on
the FAMILY clause: the defaults consequence for `Berging.vaarwater`
(declared `Enumeratie` `Vaarwater`) is the numeric `Literal` `4`.

    WF-023 FAIL /prioritizedDefaults/0/defaults/0/consequentie: a number
    value is assigned to a enumeration target

Why it has to exist: the soort layer of the defaults position, pinned
separately from the domain layer. Verifier-clean before; ref faulted
`match scrutinee is 4, not a constructor value` at the faithful reader;
clingo faulted the translation (`unscalable datatype`).

Falsifiable: drop the defaults position from `wf023` and this case passes.
