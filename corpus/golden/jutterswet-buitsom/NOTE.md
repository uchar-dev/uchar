# jutterswet-buitsom

The **SOM-without-a-fallback** witness (ASP-TRANSLATION S8/S19). A `Jutter`'s
`buitsom` sums the `waarde` of its bergings-`vondsten` with **no** `leegwaarde`,
so an empty contributor set is ref's Empty (`agg_som None → Empty`), NOT the
0 a bare `#sum` would give. The clingo arm adds a presence guard (a helper that
holds iff at least one contributor exists) so the reading rule is underivable on
the empty set. Contrast `jutterswet-strandgoed`, whose SOM carries a
`leegwaarde` 0 and correctly derives 0 there.

## Hand-computed absolutes (2 decimals)

| Jutter | vondsten (waarde)        | buitsom               |
|--------|--------------------------|-----------------------|
| J1     | V1=3.00, V2=7.00         | 3.00 + 7.00 = **10.00** |
| J2     | (none)                   | **leeg** (no fallback) |
| J3     | V4 (waarde leeg)         | **leeg** (skip → all-empty) |
| J4     | V5=5.00, V6 (waarde leeg)| skip V6 → **5.00**    |

J2 pins no-contributors → leeg; J3 pins the empty-contributor skip collapsing to
leeg; J4 pins the skip leaving a real sum. ref = clingo over every expectation
(gate 5 ≡ gate 6). NOOP-REFUTING: J1/J4 buitsom are computed, absent from the
raw input.

**Drools note:** the v0 Drools backend has no leegwaarde-less-som value distinct
from 0; where it plays the form it would fire 0 on J2/J3 where ref/clingo derive
leeg — a value divergence recorded in `scripts/check-drools.sh` (never a silent
pass).
