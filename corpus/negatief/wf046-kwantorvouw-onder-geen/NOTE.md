# wf046-kwantorvouw-onder-geen

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3) through the context clause of the
quantifier-fold decision of 2026-09-23: a fold under an enclosing
negation-like composite is negative in both parts, whatever its own
quantifier.

The model is `corpus/golden/jutterswet-opvolgerquorum` with another creation
condition: "werf een opvolger" mints an opvolger while "de rang van de
Wachter is kleiner dan 5 en geen van de volgende voorwaarden: ten minste 1
opvolger van de Wachter heeft een rang groter dan 0". On its own a
TEN_MINSTE fold reads positively in both parts. Under the propositional
GEEN it is negated, and a minted opvolger (rang of at least 2) falsifies the
condition that minted it.

    WF-046 FAIL /rules/objectCreaties/0/versions/0: negative dependency edge on
    a cycle: werf een opvolger reads what werf een opvolger produces while werf
    een opvolger is reachable from it (no strata exist)

The case isolates that clause: ignore the enclosing context in the fold's
arm and the document is clean (mutation Kh, measured 2026-09-23).

Measured before the quantifier-fold arm (2026-09-23), with one input
Wachter W1 of rang 1: `verifier: PASS`; the reference interpreter minted a
chain of five, each creation justified by a GEEN that the next generation
falsifies; the ASP backend found no stable model.
