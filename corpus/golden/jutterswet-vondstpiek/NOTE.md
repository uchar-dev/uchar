# jutterswet-vondstpiek

The **MAXIMUM/MINIMUM over a set** witness (ASP-TRANSLATION S8/S19). A `Jutter`'s
`zwaarste`/`lichtste` take the max/min of the `gewicht` of its bergings-
`vondsten` — the set-typed `Aggregation MAXIMUM`/`MINIMUM` arm, mirroring the SOM
contributor gathering but aggregated with `#max`/`#min`. Empty contributors are
skipped (an instance without `gewicht` contributes no tuple); an all-empty or
empty set derives nothing (ref `agg_max`/`agg_min` → Empty) through the
mandatory presence guard — `#sup`/`#inf` never escape into a value.

## Hand-computed absolutes (2 decimals)

| Jutter | vondsten (gewicht)        | zwaarste (max)       | lichtste (min)       |
|--------|---------------------------|----------------------|----------------------|
| J1     | V1=3.00, V2=7.00, V3=12.00| **12.00**            | **3.00**             |
| J2     | (none)                    | **leeg**             | **leeg**             |
| J3     | V4 (gewicht leeg)         | **leeg** (skip → all-empty) | **leeg**      |
| J4     | V5=8.00, V6 (gewicht leeg)| skip V6 → **8.00**   | skip V6 → **8.00**   |

J2 pins no-contributors → leeg; J3 pins the empty-skip collapsing to leeg (never
`#sup`/`#inf`); J4 pins the skip leaving a real extreme. ref = clingo over every
expectation (gate 5 ≡ gate 6). NOOP-REFUTING: J1/J4 extremes are computed,
absent from the raw input.
