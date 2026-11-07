# wf009-undeclared-abbreviation

Violates WF-009 (`spec/IR-SPEC.md` ch. 2.5): a unit where it occurs names a
declared unit system that does not declare its abbreviation. The abbreviation
is compared with the units of that system under the ch. 2.1 identity.

    WF-009.undeclared-abbreviation FAIL /model/objectTypes/0/attributes/22/datatype/unit/0:
    the unit 'kg' names the unit system 'Munt', which declares no unit 'kg': the
    unit resolves against no declaration

(twice: the factor `kg` of the attribute `gewicht` and the factor `kg` of the
attribute `prijs`, `€/kg`.)

The document is `corpus/golden/jutterswet-tekst-van-een-waarde` with the
`system` of both factors `kg` changed from `Gewicht` to `Munt`, which declares
only `EUR`. The system resolves, so WF-034 (ch. 4.6 (r)) passes the factor; the
unit `kg` is declared, but by `Gewicht`. The two factors carry the same fields,
so the units still agree where they meet (WF-091), and the verifier reports
only this arm.

**What reads the declaration.** A text part that writes its unit
(`«zijn gewicht (eenheid)»`) writes each factor in the symbol of its
declaration, the declaration with the factor's system and abbreviation
(`RUNTIME-SEMANTICS.md` ch. 1.2). This factor has none. Measured on the
golden's two case files, against the verifier without this arm, which passed
the document:

    ref     case1-gevuld  EVALUATION FAULT: the text part writes the unit kg,
                          whose factor 'kg' the unit system 'Munt' does not declare
            case2-leeg    oracle: PASS (31 expectations)   (no weight, so the part
                          writes no unit)
    clingo  both          TRANSLATION FAULT: CLINGO-042   (the golden's own limit:
                          a text composition)
    drools  both branches EXCLUDE DROOLS-369: … no unit system of the document
                          declares its factor 'kg' of the unit system 'Munt' …

The conversion of a value does not read the declaration: the factor carries
its `canonical` and `factorToCanonical` itself. `expect.txt` pins the arm.
