# datumbasis-prorata

The ch. 3.7 pro-rata part (`tijdsevenredig deel`) over a DATE-valued
base, as a red case. The document is `golden/anchor/jutterswet-borgdeel`
reduced to its `borgdeel` rule, with the base `borgtarief` (granularity
`year`) retyped to `Datum in dagen`; the target stays numeric. The
verifier passes it.

A pro-rata part is the base scaled by true days over bucket days, and a
date is a calendar POSITION: scaling one has no meaning. `ref` refuses
the base itself — `pro-rata base is not numeric: 2024-01-18`.

What the case guards against is a DEFINITE WRONG NUMBER, and the leak
was data-dependent. Measured 2026-09-23: over one whole characteristic
month the ASP backend answered `borgdeel` = **19740.00** for January 2024
— the day number of 2024-01-18 — and 19737.00 for 2024-01-15. Where the
part is NOT a whole bucket, the exactness guard ("non-exact pro-rata
part") fires only when the scaled day number does not divide evenly
(measured by the review on the anchor's own case: 2024-01-15 faulted,
2024-01-18 answered), so whether a date leaked or faulted was an
accident of its day number. The translation now declines the form by name, asked of the
base's DECLARATION, before it emits anything. The case uses 2024-01-18
over a whole month so that it pins the leak itself and not the
accidental exactness guard.

Drools: neither branch lowers this temporal assignment at all (exec
"temporele toekenning buiten Phase-F scope", DRL-text marker "TODO
temporele toekenning"), whatever the base type; listed in gate 8's
exclusion list with that reason.
