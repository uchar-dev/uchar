# jutterswet-gevuldesoorten

The typelessness of the presence test (`RUNTIME-SEMANTICS.md` ch. 3.4c;
maintainer decision G8 of 2026-09-19). One population of three Vondsten,
folded five times: once as an instance count and once per declared value
type. A Vondst declares `vindplaats` (tekst), `vondstdag` (datum),
`kustvak` (enumeratie noord/zuid) and `geborgen` (booleaan).

| fold | V1 | V2 | V3 | answer |
|---|---|---|---|---|
| aantal (instances) | member | member | member | **3** |
| `vindplaats` | "Noorderstrand" | "Zuiderhoofd" | leeg | **2** |
| `vondstdag` | 2026-05-01 | 2026-06-21 | leeg | **2** |
| `kustvak` | noord | zuid | leeg | **2** |
| `geborgen` | waar | **onwaar** | leeg | **2** |

The last row carries the sharpest half of the claim: `onwaar` is FILLED.
False is a value; leeg is the absence of one, and the test asks after
presence rather than after truth. An executor that counts booleans by
their truth answers 1 there, and an executor that requires a number
answers nothing at all — the fold neither compares nor adds, so the
orderability requirement the ranking folds carry (`IR-SPEC.md` ch. 2.4)
has no grip here.

The instance count stands beside them for the same reason it does in
`jutterswet-gevuldetelling`: without it the four 2's witness nothing,
since the reader cannot see that the population is 3.

No `source.rgs`: the kind is wire-first and no frontend spells it.
