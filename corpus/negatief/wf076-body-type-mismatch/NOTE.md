Violates WF-076 (`spec/IR-SPEC.md` ch. 4.10): the `Let` declares type
Number(2 dec) but its body is a Number(4-dec) literal — a `Let`'s type MUST
equal its body type. The verifier also has nothing else to flag.

A minimal hand-built document.
