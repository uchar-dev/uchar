# wf046-kwantorvouw-tenhoogste-bereik

Violates **WF-046** (`spec/IR-SPEC.md` ch. 4.6 and 5.2,
`RUNTIME-SEMANTICS.md` ch. 6.3) through one clause of the quantifier-fold
decision of 2026-09-23: the RANGE of a TEN_HOOGSTE fold is a negative edge,
because a growing range can push the count over the threshold.

The model is `corpus/golden/jutterswet-opvolgerquorum` with another creation
condition: "werf een opvolger" mints an opvolger while "de rang van de
Wachter is kleiner dan 5 en ten hoogste 1 opvolger van de Wachter heeft een
kleur ongelijk aan 7". `kleur` is input only, so the matrix reads no written
node; the range is the `opvolging` relation that the creation itself fills.
An opvolger the creation mints has no kleur, and a leeg kleur is ongelijk
aan 7 (ch. 1.2), so every minted opvolger counts.

    WF-046 FAIL /objectCreaties/0: negative dependency edge on a cycle: werf een opvolger reads what werf een opvolger produces while werf een opvolger is reachable from it (no strata exist)

The case isolates that clause: read the TEN_HOOGSTE range positively and the
document is clean (mutation Kj, measured 2026-09-23).

Measured before the quantifier-fold arm (2026-09-23), with W1 (rang 1)
carrying an input opvolger W2 (rang 3): `verifier: PASS`. The reference
interpreter let W1 mint (one opvolger counted, 1 ≤ 1), after which W1 has
two counted opvolgers and merk(W1) = 2; the ASP backend found no stable
model.
