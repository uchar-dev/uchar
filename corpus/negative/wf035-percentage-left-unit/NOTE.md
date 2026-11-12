Violates WF-035 (`spec/IR-SPEC.md` ch. 4.6): the left operand of
`percentageOf` is unitless or carries exactly the unit `%`. Here it is the
tarief of a Berging, declared in €: "het tarief van de Berging procent van de
opbrengst van de Berging".

    WF-035.percentage-left-unit FAIL /rules/rules/0/versions/0/value/left:
    the left operand of a percentage carries the unit EUR: it is unitless or
    carries exactly %, since the percentage takes its unit from the right
    operand

`l/100 × r` takes the unit of `r` (`spec/RUNTIME-SEMANTICS.md` ch. 2.2, ch.
2.6 rule 7), and no clause says what a unit of `l` does. Measured before the
arm (2026-11-12), with a tarief of 12 € and an opbrengst of 1000 €: the
verifier passed the document, ref answered a premie of 120 €, and the ASP
backend refused a unit-carrying multiplicative operand. `expect.txt` pins the
arm.
