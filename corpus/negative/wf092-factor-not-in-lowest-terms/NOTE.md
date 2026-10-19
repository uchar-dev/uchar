# wf092-factor-not-in-lowest-terms

Violates WF-092 (`spec/IR-SPEC.md` ch. 2.5), arm (c): a conversion factor that
is not in lowest terms. The unit `jaar` of the system `Termijn` carries
`factorToCanonical: {"num": 24, "den": 2}` — twelve months, the value of the
host golden, written with the common divisor 2 — at its declaration and at
both of its occurrences in rule 0.

    WF-092 FAIL /model/unitSystems/0/units/1/factorToCanonical: unit 'jaar'
    carries conversion factor 24/2, which is not in lowest terms: an exact
    factor has one spelling, here 12

The same line follows for `/rules/rules/0/versions/0/value/type/unit` and
`/rules/rules/0/versions/0/value/left/type/unit`.

Why the arm exists. Since L3 a factor is an exact rational `{num, den}` of two
integers of at least 1 (delta B17 (i) of `spec/proposals/V1-WIREMIGRATIE.md`,
CONSTRUCTS row 26). The schema and every loader hold both integers to at least
1, but a schema cannot state a greatest common divisor, so this arm does.
Lowest terms gives one value one spelling, and WF-091 depends on that: it
compares a unit's factor byte for byte, so `24/2` against `12/1` would be
reported as two different units although they convert identically. The three
copies here agree with each other, so WF-091 stays silent and only this arm
speaks.

The document is `corpus/golden/jutterswet-vergunningsduur` with the factor of
`jaar` respelled at its three positions. `expect.txt` pins the arm.
